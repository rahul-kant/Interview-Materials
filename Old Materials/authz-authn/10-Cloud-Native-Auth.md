# 10. Cloud Native Auth & Migrations (Deep Dive)

> **Level:** Principal Architect  
> **Topic:** Scale, Resilience, Costs & Customization  
> **Goal:** Deploying Auth for 10M+ Users without outages or bankruptcy.

---

## 📖 Table of Contents
1.  [ELI5: Rent vs Buy](#1-eli5-rent-vs-buy)
2.  [Cloud Providers (AWS, Azure, GCP)](#2-cloud-providers)
3.  [Deep Dive: Multi-Region Resilience](#3-deep-dive-multi-region-resilience)
4.  [Deep Dive: The Pricing Trap (Cost Analysis)](#4-deep-dive-the-pricing-trap)
5.  [Deep Dive: Advanced Customization (Lambda Triggers)](#5-deep-dive-advanced-customization)
6.  [Deep Dive: B2B Multi-Tenancy](#6-deep-dive-b2b-multi-tenancy)
7.  [Migration Strategies](#7-migration-strategies)
8.  [Spring Boot Integration](#8-spring-boot-integration)
9.  [Interview Questions](#9-interview-questions)
10. [Architect Decision Notes](#10-architect-decision-notes)

---

<a name="1-eli5-rent-vs-buy"></a>
## 1️⃣ ELI5: Rent vs Buy

-   **On-Prem (Build):** You build your own power plant. You buy coal, run the turbines, and fix it when it breaks. (Example: Writing your own User Table + BCrypt logic).
-   **Cloud Native (Rent):** You plug into the wall socket. You pay a monthly bill to the electric company. (Example: Using AWS Cognito).

**Why Rent?**
Security is hard. If you build it, you must patch it. If AWS builds it, their 5,000 security engineers patch it.

---

<a name="2-cloud-providers"></a>
## 2️⃣ Cloud Providers

### AWS Cognito
-   **User Pools:** The Directory. Sign-up, Sign-in, MFA, Forgot Password.
-   **Identity Pools:** Exchange User Pool tokens for **AWS Credentials** (e.g., to upload directly to S3).
-   **Pros:** Very cheap (50k MAU free).
-   **Cons:** UI customization is limited. Developer Experience (DX) is verbose.

### Azure AD (Entra ID)
-   **Focus:** Enterprise B2B. Default for O365.
-   **Pros:** Unbeatable integration with Corporate Directories.

### Auth0 / Okta
-   **Focus:** Developer Experience.
-   **Pros:** amazing docs, SDKs, and "Rules" engine.
-   **Cons:** Very expensive at scale (B2C).

---

<a name="3-deep-dive-multi-region-resilience"></a>
## 3️⃣ Deep Dive: Multi-Region Resilience

**The Scenario:** `us-east-1` goes down. Your users in New York cannot login.
**The Problem:** Most IDaaS (Identity as a Service) providers are **Region-Locked**. User data in `us-east-1` does NOT exist in `us-west-2` by default.

### Solution A: Active-Passive (Backup)
1.  **Export:** Nightly dump of all users (JSON).
2.  **Replicate:** Copy dump to Backup Region.
3.  **Failover:** If Primary down, run import script in Backup.
4.  **RTO (Recovery Time Objective):** Hours. (Users cannot login for hours).
5.  **Passwords:** **CRITICAL:** Most providers do NOT allow exporting password hashes. Users must reset passwords after failover.

### Solution B: Active-Active (Global Tables)
*Advanced Pattern*
1.  Use a custom DynamoDB Global Table as the user store.
2.  Use Cognito **Lambda Triggers** (Pre-Auth) to check this Global Table instead of the local User Pool.
3.  **Result:** Users can login to ANY region instantly.
4.  **Complexity:** Extreme. You are effectively building your own IDP on top of AWS primitives.

---

<a name="4-deep-dive-the-pricing-trap"></a>
## 4️⃣ Deep Dive: The Pricing Trap

**Scenario:** You launch a B2C app. You get 1 Million Monthly Active Users (MAU).

### The Bill:
| Provider | 50k Users | 100k Users | 1M Users (MAU) |
| :--- | :--- | :--- | :--- |
| **Keycloak (Self-Hosted)** | $50 (EC2) | $100 (EC2) | $500 (Cluster) |
| **AWS Cognito** | Free | $275 | $3,500 |
| **Auth0 (B2C)** | $1,500+ | $3,000+ | **$20,000+ / month** |

**The SAML Trap:**
Many providers charge extra for "Enterprise Connections" (SAML/SSO).
*   Cognito: +$0.015 per active user.
*   Auth0: Often requires "Enterprise" tier ($$$).

**Architect Decision:**
Start with IDaaS. If you hit scale (1M+ users), the bill might justify migrating to a self-hosted solution (Keycloak) or negotiating an Enterprise contract.

---

<a name="5-deep-dive-advanced-customization"></a>
## 5️⃣ Deep Dive: Advanced Customization (Lambda Triggers)

Cognito (and Auth0 Actions) allows you to inject running code into the auth lifecycle.

### Use Case 1: Migration (Lazy Loading)
**Trigger:** `UserMigration_Authentication`
-   Old User tries to login. Not found in Cognito.
-   Lambda fires. Connects to Legacy SQL DB. Validates password.
-   Lambda returns "Success".
-   Cognito silently migrates user profile + password to Cloud.

### Use Case 2: Custom Claims (The "Hasura" Pattern)
**Trigger:** `Pre Token Generation`
-   User logs in.
-   Lambda fires. Queries internal DynamoDB for "Subscription Tier" or "Team ID".
-   Lambda injects `https://myapp.com/claims/tier = "GOLD"` into the JWT.
-   **Result:** Frontend gets the data in the token without an extra API call.

```javascript
exports.handler = async (event) => {
    event.response = {
        claimsOverrideDetails: {
            claimsToAddOrOverride: {
                "my_custom_role": "super-admin"
            }
        }
    };
    return event;
};
```

---

<a name="6-deep-dive-b2b-multi-tenancy"></a>
## 6️⃣ Deep Dive: B2B Multi-Tenancy

**The Challenge:**
You sell software to Companies (Tenants).
ColaCo users should only see ColaCo data.
PepsiCo users should only see PepsiCo data.

### Pattern A: One Pool per Tenant
-   Create a separate User Pool for every customer.
-   **Pros:** Perfect isolation. Different password policies per tenant.
-   **Cons:** Management nightmare. 1000 tenants = 1000 Pools. AWS Limits (Soft limit 50 pools).

### Pattern B: Shared Pool + Groups
-   One giant User Pool.
-   Use `groups` to assign `TenantID`.
-   **Pros:** Easy to manage.
-   **Cons:** "Leaky" isolation. If your code forgets `WHERE tenant_id = X`, data leaks.

### Pattern C: The "Organization" Feature (Auth0/Clerk)
-   Modern IDaaS has first-class support for "Organizations".
-   Users belong to an Org. Tokens contain `org_id`.
-   **Verdict:** If doing B2B, pay for a provider that supports this native construct.

---

<a name="7-migration-strategies"></a>
## 7️⃣ Migration Strategies

### Strategy 1: The Strangler Fig
1.  **Gateway:** Put a Gateway in front of Monolith.
2.  **New Users:** All *new* sign-ups go to new IDP.
3.  **Route:** Gateway checks: "Is this a legacy session?" -> Monolith. "Is this a JWT?" -> Microservices.
4.  **Slowly:** Move functionality. Eventually, Monolith handles 0 traffic.

### Strategy 2: Just-In-Time (JIT) Migration (Best)
1.  User 'Alice' tries to login to New IDP. **Fail** (User doesn't exist yet).
2.  IDP calls "Legacy Auth Hook" (Lambda).
3.  Hook calls Legacy DB: "Check username/password".
4.  Legacy DB says "Valid".
5.  Hook creates Alice in New IDP *right now* and sets her password to what she just typed.
6.  Alice logs in.

---

<a name="8-spring-boot-integration"></a>
## 8️⃣ Spring Boot Integration (Cognito)

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://cognito-idp.us-east-1.amazonaws.com/us-east-1_xxxxxx
```

**Roles Mapping (Groups to Roles):**
Cognito uses `cognito:groups`. Spring uses `SCOPE_`.
You need a `JwtAuthenticationConverter`:

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter groupsConverter = new JwtGrantedAuthoritiesConverter();
    groupsConverter.setAuthorityPrefix("ROLE_");
    groupsConverter.setAuthoritiesClaimName("cognito:groups");
    
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(groupsConverter);
    return converter;
}
```

---

<a name="9-interview-questions"></a>
## 9️⃣ Interview Questions

### Architect
**Q: How do you handle Data Residency (GDPR) in a global auth system?**
**A:** You cannot have one global User Pool. You must use **Regional Sharding**.
-   EU Users -> EU-Frankfurt User Pool.
-   US Users -> US-East-1 User Pool.
-   **Routing:** The Frontend asks the user "Select your Region" (or detects IP), and directs login to the correct pool.
-   **Apps:** The Backend API must verify tokens from *multiple* issuers (EU issuer and US issuer).

**Q: Why use Cognito Identity Pools vs User Pools?**
**A:**
-   **User Pool:** "Authentication". Returns JWT. Who are you?
-   **Identity Pool:** "Authorization". Returns AWS IAM temporary credentials. What AWS resources (S3, DynamoDB) can you touch directly?

---

<a name="10-architect-decision-notes"></a>
## 🔟 Architect Decision Notes

> **Decision:** **Start with Managed (Cognito/Auth0).**
>
> 1.  **Do not build your own IDP** unless you are a bank or have >10M users.
> 2.  **Watch the Costs:** Calculate your MAU growth. If you are B2C freemium, Auth0 will bankrupt you. Use Cognito or Firebase.
> 3.  **Plan for downtime:** Cloud Providers go down. Decide your RTO (Recovery Time). If you need 99.999%, you need Active-Active Multi-Region, which implies extreme complexity.
> 4.  **Own your data:** Ensure you have a way to export your users (CSV/JSON) if the provider raises prices 10x next year.

---

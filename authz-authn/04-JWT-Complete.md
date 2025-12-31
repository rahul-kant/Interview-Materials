# JSON Web Tokens (JWT) - Complete Guide & MAANG Architecture

> **Level:** Principal Engineer  
> **Time to Master:** 3-4 weeks  
> **Interview Focus:** Essential for microservices roles  
> **Case Studies:** Netflix, Google, Meta

---

## 📖 Table of Contents
1.  [ELI5: The Notarized Letter](#1-eli5)
2.  [Real-Life Analogy](#2-analogy)
3.  [Why This Exists](#3-why)
4.  [MAANG Case Study: Netflix "Passport"](#4-netflix)
5.  [Core Concepts](#5-concepts)
6.  [Visual Diagrams](#6-visuals)
7.  [Simple Example](#7-example)
8.  [Deep Dive (Architectural Patterns)](#8-deep-dive)
    *   [JWT vs Opaque](#jwt-vs-opaque)
    *   [JWS vs JWE](#jws-vs-jwe)
    *   [Sender-Constrained Tokens (DPoP)](#dpop)
    *   [Revocation at Scale (Bloom Filters)](#revocation)
9.  [Spring & Java Implementation](#9-impl)
10. [Hands-On Labs (1-6)](#10-labs)
11. [Interview Questions](#11-questions)
12. [When to Use / Not Use](#12-usage)
13. [Security Attacks (STRIDE)](#13-security)
14. [Mind Map](#14-mindmap)

---

<a name="1-eli5"></a>
## 1️⃣ ELI5 (Explain Like I’m 5)
**The Notarized Document:**
Imagine you have a letter that says "I am allowed to enter the vault".
If you just write it on a napkin, no one believes you.
But if a **Notary Public** stamps it with an un-forgeable wax seal, then the guard believes the letter *without calling the Notary*.
The guard checks:
1.  Is the wax seal broken? (Signature)
2.  Is the date expired? (Expiration)
3.  Does it say "Allowed"? (Claims)

A JWT is just a digital letter with a wax seal from the server.

---

<a name="2-analogy"></a>
## 2️⃣ REAL-LIFE ANALOGY
**A Passport or Driver's License:**
-   It contains your data (Name, DOB, Class of license).
-   It is issued by an Authority.
-   It has a holographic strip (Signature) to prove it's real.
-   A policeman can verify it *offline* just by looking at the hologram. They don't need to call the DMV every time (Stateless).

---

<a name="3-why"></a>
## 3️⃣ WHY THIS EXISTS
**Ideally:** We have 100 microservices. We don't want every service calling the Database to check if a user is logged in. That kills the database.
**The Fix:** We give the user a JWT. The JWT contains `role: admin`.
**The Benefit:** Service A, B, and C can all trust the token just by doing a math check (Cpu is cheap; Database is expensive).

---

<a name="4-netflix"></a>
## 4️⃣ MAANG Case Study: Netflix "Passport"

**The Architecture:**
Netflix has thousands of microservices. How does "Service A" tell "Service Z" who the user is without hitting a central bottleneck?

1.  **Edge Gateway (Zuul/Mantis):**
    *   The **ONLY** place that handles external tokens (Cookies, Opaque Tokens).
    *   Validates the user against the Auth Service.
    *   Generates an internal **"Passport"** (A highly specialized, short-lived JWT).
2.  **Internal Propagation:**
    *   The Passport is passed in the header `X-Netflix-Passport`.
    *   It contains: User ID, Device ID, Cell ID (Routing), and Integrity Signature.
3.  **MS Authorization:**
    *   Downstream services (Recommendation, Video API) blindly trust the Passport integrity.
    *   They do **NOT** call the Auth Service. They just verify the HMAC/RSA signature.

**Why this wins:**
*   **Latency:** Zero network hops to check identity inside the mesh.
*   **Decoupling:** Auth logic is concentrated at the Edge.

```mermaid
sequenceDiagram
    participant Device
    participant Edge as Edge Gateway
    participant Auth as Auth Service
    participant Movie as Movie Service
    participant Recs as Recommendations

    Device->>Edge: Login / Stream (Cookie)
    Edge->>Auth: Validate Cookie
    Auth-->>Edge: Returns "Passport" (JWT)
    
    Edge->>Movie: GET /movie/123 (Header: Passport)
    Note over Movie: Verify Passport Sig (Local CPU)
    
    Movie->>Recs: GET /similar (Header: Passport)
    Note over Recs: Verify Passport Sig (Local CPU)
    
    Recs-->>Movie: 200 OK
    Movie-->>Device: Stream Data
```

---

<a name="5-concepts"></a>
## 5️⃣ CORE CONCEPTS
-   **Stateless:** The server does not store the token.
-   **Compact:** Small enough to fit in a header.
-   **Structure:** `Header` . `Payload` . `Signature`
-   **Claims:** The key-value pairs inside (e.g., `sub: user123`, `exp: 17000000`).

---

<a name="6-visuals"></a>
## 6️⃣ VISUAL DIAGRAMS

**JWT Structure:**
```mermaid
graph LR
    H[Header - Red] -->|Base64| JWT
    P[Payload - Purple] -->|Base64| JWT
    S[Signature - Blue] -->|Hash(H+P+Secret)| JWT
    JWT[JWT String: aaaaa.bbbbb.ccccc]
```

**Microservices Trust Flow:**
```mermaid
sequenceDiagram
    participant Client
    participant Auth as Auth Server
    participant ServiceA
    participant ServiceB

    Client->>Auth: Login
    Auth-->>Client: Return JWT (Signed by Private Key)
    
    Client->>ServiceA: Request + JWT
    ServiceA->>ServiceA: Verify Signature (Public Key)
    ServiceA-->>Client: 200 OK (No DB Call!)
```

---

<a name="7-example"></a>
## 7️⃣ SIMPLE EXAMPLE (BEGINNER)

**The Token string looks like this:**
`eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ`

**Decoded (What's inside):**
**Header:** `{ "alg": "HS256", "typ": "JWT" }`
**Payload:** `{ "sub": "john", "admin": true, "iat": 1516239022 }`
**Signature:** `HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)`

---

<a name="8-deep-dive"></a>
## 8️⃣ DEEP DIVE (ADVANCED)

<a name="jwt-vs-opaque"></a>
### 8.1 JWT vs Opaque Tokens (The Architect's Dilemma)

| Feature | JWT (Stateless) | Opaque (Reference) |
| :--- | :--- | :--- |
| **Format** | JSON Object (Readable) | Random String (UUID) |
| **Size** | Large (contains data) | Tiny (32 chars) |
| **Validation** | Local CPU check (Fast) | DB/Redis Lookup (Slower) |
| **Revocation** | **Hard.** (Must wait for expiry) | **Instant.** (Delete from Redis) |
| **Pell-Mell** | Good for Microservices | Good for Monoliths/High-Security |

---

<a name="jws-vs-jwe"></a>
### 8.2 JWS vs JWE (Encryption vs Signing)

Most people think JWT = JWS. There are two types:

1.  **JWS (Signed) - The Standard**
    *   **Structure:** `Header . Payload . Signature`
    *   **Visibility:** **Public.** Anyone can base64-decode the payload.
    *   **Use Case:** Passing non-sensitive data (User ID, Roles, Scopes).

2.  **JWE (Encrypted) - The Vault**
    *   **Structure:** `Header . EncryptedKey . IV . CipherText . Tag`
    *   **Visibility:** **Hidden.** The payload looks like garbage noise.
    *   **Use Case:** Passing PII (SSN, Email, Address).
    *   **Cost:** Slower. Requires Double Crypto (RSA to encrypt key + AES to encrypt payload).

**Senior Tip:** Do not use JWE unless you *really* need to hide the data. JWS is faster and easier to debug.

---

<a name="dpop"></a>
### 8.3 Sender-Constrained Tokens (DPoP)
*Theft Proofing: "Making a stolen token useless."*

**The Problem:** Conventional Bearer tokens are like cash. If you find $20 on the ground, you can spend it.
**The Fix (DPoP):**
*   Client generates a public/private key pair.
*   Client sends JWT + `DPoP` header (signature of the request).
*   Server Checks: "Does the key in the DPoP header match the one bound to the access token?"
*   Result: If hacker steals the token, they still fail because they don't have the private key.

---

<a name="revocation"></a>
### 8.4 Revocation at Scale (Bloom Filters)

**The Myth:** "You can't revoke JWTs."
**The Reality:** You can, but it costs money/latency.

**Strategy: The Distributed Blacklist**
1.  **Event:** User clicks "Logout".
2.  **Action:** Add `jti` (Token ID) to Redis.
3.  **Optimization (Google Scale):** Use a **Bloom Filter**.
    *   A probabilistic structure. "Is this token revoked?" -> "Definite NO" or "Maybe YES".
    *   If "Maybe YES", check the slow DB.
    *   Reduces DB hits by 99% for valid tokens.

---

<a name="9-impl"></a>
## 9️⃣ SPRING & JAVA IMPLEMENTATION

**Dependencies:** `spring-boot-starter-oauth2-resource-server`

**Code (RS256 Verification):**
```java
@Configuration
@EnableWebSecurity
public class JwtConfig {
    
    @Value("${spring.security.oauth2.resourceserver.jwt.public-key-location}")
    RSAPublicKey key;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> 
                jwt.decoder(NimbusJwtDecoder.withPublicKey(key).build())
            ));
        return http.build();
    }
}
```

---

<a name="10-labs"></a>
## 🔟 HANDS-ON LABS (NO COST)

### Lab 1: RSA Key Generation & Rotation
**Goal:** Generate and rotate RSA keys for JWT signing.
*   Use `openssl genrsa` to create 2048-bit keys.
*   Implement a Java Scheduler to rotate keys monthly.

### Lab 2: JWKS Endpoint Implementation
**Goal:** Expose public keys via `/.well-known/jwks.json`.
*   Use `nimbus-jose-jwt` to convert `RSAPublicKey` to JSON Web Key (JWK) format.

### Lab 3: JWT Blacklisting with Redis
**Goal:** Implement immediate token revocation.
*   On Logout: `redis.set("blacklist:" + jti, "revoked", ttl);`
*   On Request: `Filter` checks Redis. If found -> 401.

### Lab 4: Custom Claims Validation
**Goal:** Enforce business rules (Tenant ID).
*   Implement `OAuth2TokenValidator<Jwt>`.
*   Check if `jwt.getClaim("tenant_id")` matches the current context.

### Lab 5: Performance Benchmarking
**Goal:** Measure HS256 vs RS256.
*   Result: HS256 is ~10x faster but requires shared secrets.

### Lab 6: Cracking a Weak JWT (Security Lab)
**Goal:** Understand why `HS256` is dangerous with weak secrets.
**Tools:** `hashcat`.
**Steps:**
1.  Get a JWT signed with HS256 and secret "secret".
2.  Run `hashcat -m 16500 jwt.txt wordlist.txt`.
3.  **Result:** It finds the key in milliseconds.

---

<a name="11-questions"></a>
## 1️⃣1️⃣ INTERVIEW QUESTIONS

### Principal Engineer
**Q: How does Netflix handle Key Rotation without downtime?**
**A:**
1.  Auth Service generates **New Key (B)**.
2.  Publishes Key (B) to JWKS, but signs with **Old Key (A)** for 5 mins (Propagation delay).
3.  All Microservices refresh cache.
4.  Auth Service starts signing with Key (B).

**Q: Design a system where a user can grant "One-Time Access" to a document.**
**A:** Use a JWT with `max_uses: 1` claim (requires state) OR a JWT with a very short expiry (1 min) and a specific scope `doc:read:123`.

### Senior Engineer
**Q: Why use RS256 over HS256 in Microservices?**
**A:** Key Management. With HS256, every microservice needs the "Private Secret". If one service is compromised, the attacker can forge tokens for ANY service. RS256 allows services to verify using a Public Key (safe to share).

---

<a name="12-usage"></a>
## 1️⃣2️⃣ WHEN TO USE / WHEN NOT TO USE

| Scenario | Decision | Reason |
| :--- | :--- | :--- |
| **Session Mgmt** | ❌ NO | Use server-side sessions for simple web apps. |
| **Microservices** | ✅ YES | Avoids "Chatty" calls to Auth Server. |
| **One-time Links** | ✅ YES | "Password Reset" links are often JWTs. |
| **High Security** | ⚠️ NO | If you need instant ban buttons, Opaque (Ref) is better. |

---

<a name="13-security"></a>
## 1️⃣3️⃣ SECURITY ATTACKS & DEFENSE (STRIDE)

| Threat | Attack Vector | Impact | Mitigation |
| :--- | :--- | :--- | :--- |
| **Tampering** | **Signature Stripping.** Attacker changes `alg` headers to `none`. | Signature Bypass. | **Disable `none` alg** in library. Enforce `RS256`. |
| **Information Disclosure** | PII in Payload. | Data Leakage. | **Minimal Claims.** Only put `sub` and `scope`. Use JWE if PII needed. |
| **Spoofing** | **Key Confusion.** Attacker signs with Public Key as HMAC. | Forgery. | Explicitly check `alg` header matches options. |
| **Repudiation** | Token theft without logs. | Impossible to trace. | Log `jti` (Token ID) for key transactions. |

---

<a name="14-mindmap"></a>
## 1️⃣4️⃣ MIND MAP

*   **JWT**
    *   **Structure**: Header.Payload.Signature
    *   **Benefit**: Stateless Trust
    *   **Architecture**:
        *   **Netflix Passport**: Internal Identity
        *   **JWS vs JWE**: Sign vs Encrypt
    *   **Security**:
        *   **DPoP**: Theft Proofing
        *   **Revocation**: Blacklists / Bloom Filters

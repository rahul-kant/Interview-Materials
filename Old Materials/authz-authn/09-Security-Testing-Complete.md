# 09. Security Testing: MockMvc, MockUser, & Scans

> **Level:** Senior  
> **Topic:** Automated Security Verification  
> **Goal:** "Shift Left" - Prevent bugs before commit.

---

## 📖 Table of Contents
1.  [ELI5: Testing the Guard](#1-eli5-testing-the-guard)
2.  [Why This Exists](#2-why-this-exists)
3.  [Dependency Setup](#3-dependency-setup)
4.  [Level 1: Unit Testing (@WithMockUser)](#4-level-1-unit-testing)
5.  [Level 2: Custom Users (@WithUserDetails)](#5-level-2-custom-users)
6.  [Level 3: Integration Testing (MockMvc)](#6-level-3-integration-testing)
7.  [Testing JWTs](#7-testing-jwts)
8.  [Testing CSRF](#8-testing-csrf)
9.  [Fuzzing & ZAP Scans](#9-fuzzing--zap)
10. [Interview Questions](#10-interview-questions)

---

<a name="1-eli5-testing-the-guard"></a>
## 1️⃣ ELI5: Testing the Guard

Imagine you hire a security guard for your vault.
How do you know they are doing their job?
-   **Method 1 (Manual):** You dress up as a robber and try to break in. (Manual Penetration Testing).
-   **Method 2 (Automated):** You build a robot that tries to break in every morning at 9am. (Automated Security Tests).

Security Testing is simply asking: "**Does this endpoint return 403 Forbidden when it should?**"

---

<a name="2-why-this-exists"></a>
## 2️⃣ Why This Exists

Developers often write tests for the "Happy Path" (User logs in successfully).
They rarely write tests for the "Unhappy Path" (Hacker tries injection).
**Security Unit Tests** prove that your `@PreAuthorize` annotations strictly enforce the rules you designed.

---

<a name="3-dependency-setup"></a>
## 3️⃣ Dependency Setup

In `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

<a name="4-level-1-unit-testing"></a>
## 4️⃣ Level 1: Unit Testing (@WithMockUser)

The simplest way to simulate a logged-in user.
It bypasses authentication logic and directly populates the `SecurityContext`.

```java
@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void admin_CanAccessAdminPage() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void user_CannotAccessAdminPage() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
               .andExpect(status().isForbidden());
    }
    
    @Test
    void anonymous_CannotAccess() throws Exception {
         mockMvc.perform(get("/admin/dashboard"))
               .andExpect(status().isUnauthorized()); // or 302 login
    }
}
```

---

<a name="5-level-2-custom-users"></a>
## 5️⃣ Level 2: Custom Users (@WithUserDetails)

Use this when your app depends on custom properties in your `UserDetails` object (e.g., `user.getTenantId()`).

**Step 1: Define a Test User Factory**
```java
@TestConfiguration
public class TestConfig {
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = CustomUser.builder()
            .username("admin")
            .tenantId("tenant-1") // Custom field
            .roles("ADMIN")
            .build();
        return new InMemoryUserDetailsManager(admin);
    }
}
```

**Step 2: Use in Test**
```java
@Test
@WithUserDetails("admin") // Looks up 'admin' in your UserDetailsService
void admin_AccessMultiTenantData() {
    // ...
}
```

---

<a name="6-level-3-integration-testing"></a>
## 6️⃣ Level 3: Integration Testing (MockMvc & RequestPostProcessors)

Use `SecurityMockMvcRequestPostProcessors` to inject credentials per-request.

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@Test
void manualAuthTest() throws Exception {
    mockMvc.perform(get("/api/data")
            .with(httpBasic("user", "password"))) // Simulates Basic Auth
            .andExpect(status().isOk());
}

@Test
void csrfTest() throws Exception {
    // POST without CSRF token -> 403
    mockMvc.perform(post("/api/transfer"))
            .andExpect(status().isForbidden());

    // POST with CSRF token -> 200
    mockMvc.perform(post("/api/transfer").with(csrf()))
            .andExpect(status().isOk());
}
```

---

<a name="7-testing-jwts"></a>
## 7️⃣ Testing JWTs

With `oauth2-resource-server`, you can mock the JWT directly.

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@Test
void jwtTest() throws Exception {
    // Simulates a valid JWT with 'SCOPE_read'
    mockMvc.perform(get("/api/orders")
            .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_read")))) 
            .andExpect(status().isOk());
}

@Test
void jwtWithClaims() throws Exception {
    mockMvc.perform(get("/api/profile")
            .with(jwt().jwt(jwt -> jwt.claim("sub", "user-123"))))
            .andExpect(status().isOk());
}
```

---

<a name="8-testing-csrf"></a>
## 8️⃣ Testing CSRF

If your API is stateless (JWT), **CSRF should be disabled**.
Test to ensure it IS disabled!

```java
@Test
void csrfShouldBeDisabled_ForApi() throws Exception {
    // Perform POST without CSRF token
    // If CSRF is enabled, this returns 403.
    // If disabled (correct for APIs), this passes (or fails logic validation).
    
    mockMvc.perform(post("/api/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isBadRequest()); // 400 means it hit the controller! 
            // If 403, CSRF might be ON inadvertently.
}
```

---

<a name="9-fuzzing--zap"></a>
## 9️⃣ Fuzzing & ZAP Scans

**OWASP ZAP (Zed Attack Proxy):** 
Automated tool to scan your API for simple vulns.

**How to run (Docker):**
```bash
docker run -t owasp/zap2docker-stable zap-baseline.py -t http://host.docker.internal:8080
```
**Common Findings:**
1.  Missing `X-Content-Type-Options: nosniff`.
2.  Missing `Content-Security-Policy`.
3.  Cookies without `HttpOnly`.

---

<a name="10-interview-questions"></a>
## 🔟 Interview Questions

### Senior Engineer
**Q: How do you separate Unit vs Integration tests for Security?**
**A:**
-   **Unit (@WithMockUser):** Tests the ACLs (`@PreAuthorize`). Verifies "Does the code allow Role A?". Fast. Mocked database.
-   **Integration (TestContainers):** Spins up real DB and Keycloak. Tests the full flow: Login -> Get Token -> Call API. Slow. Verifies configuration.

**Q: Why use @WithMockUser instead of passing a real token?**
**A:** Speed. Generating a real RSA-signed JWT takes milliseconds. Doing it 1000 times adds up. @WithMockUser skips the cryptographic signature check and injects the Principal directly, isolating the authorization logic.

---

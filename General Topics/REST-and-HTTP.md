# 🌐 REST & HTTP — A Complete Study Guide

> From the raw mechanics of an HTTP request to designing idempotent, versioned, secure APIs at FAANG scale. Written to be read top-to-bottom: every section builds on the one before it, and the interviewer's follow-up questions are baked in rather than left for you to guess.

---

## 📋 Table of Contents

**Part I — HTTP Foundations**

1. [Why REST & HTTP Matter](#1--why-rest--http-matter)
2. [The Anatomy of an HTTP Request/Response](#2--the-anatomy-of-an-http-requestresponse)
3. [HTTP Methods (Verbs)](#3--http-methods-verbs)
4. [HTTP Status Codes](#4--http-status-codes)
5. [HTTP Headers](#5--http-headers)
6. [Statelessness](#6--statelessness)
7. [The Request Lifecycle](#7--the-request-lifecycle)

**Part II — Designing RESTful APIs**

8. [What REST Actually Is](#8--what-rest-actually-is)
9. [Resource Modeling: Nouns vs Verbs & URI Design](#9--resource-modeling-nouns-vs-verbs--uri-design)
10. [HATEOAS](#10--hateoas)
11. [The Richardson Maturity Model](#11--the-richardson-maturity-model)
12. [Request & Response Design](#12--request--response-design)
13. [Pagination, Filtering & Sorting](#13--pagination-filtering--sorting)
14. [API Versioning](#14--api-versioning)

**Part III — Correctness & Reliability**

15. [Idempotency](#15--idempotency)
16. [Caching: ETags, Cache-Control & Conditional Requests](#16--caching-etags-cache-control--conditional-requests)
17. [Error Handling & RFC 7807 Problem Details](#17--error-handling--rfc-7807-problem-details)

**Part IV — Security, Performance & Protocol Choice**

18. [Security: AuthN, AuthZ, Rate Limiting & CORS](#18--security-authn-authz-rate-limiting--cors)
19. [Performance: Compression, CDN, Keep-Alive, HTTP/2 & HTTP/3](#19--performance-compression-cdn-keep-alive-http2--http3)
20. [REST vs RPC vs GraphQL vs gRPC](#20--rest-vs-rpc-vs-graphql-vs-grpc)
21. [API Documentation & Contract Testing](#21--api-documentation--contract-testing)

**Part V — Staff-Level Design Layer**

22. [Backward Compatibility & Deprecation at Scale](#22--backward-compatibility--deprecation-at-scale)
23. [Idempotency Keys in Distributed Systems](#23--idempotency-keys-in-distributed-systems)
24. [Rate-Limiting Algorithms & Where They Live](#24--rate-limiting-algorithms--where-they-live)
25. [API Gateway & Backend-for-Frontend Patterns](#25--api-gateway--backend-for-frontend-patterns)

**Part VI — FAANG Case Studies**

26. [6 FAANG-Favorite REST API Design Case Studies](#26--6-faang-favorite-rest-api-design-case-studies)

**Part VII — Revision & Interview Prep**

27. [⚡ Quick Revision](#27--quick-revision)
28. [🎓 FAANG Interview Q&A (20 Questions)](#28--faang-interview-qa-20-questions)
29. [📝 STAR Behavioral Questions](#29--star-behavioral-questions)
30. [🔗 Further Reading](#30--further-reading)

---

# Part I — HTTP Foundations

## 1. 🎯 Why REST & HTTP Matter

Almost every product you have ever used talks to a server over HTTP. When you open Instagram, refresh Gmail, tap "Pay" in a banking app, or ask a ride-sharing app to find a driver, a client sends an HTTP request and a server sends back an HTTP response. **REST is the dominant architectural style for designing those request/response conversations**, and HTTP is the protocol that carries them.

Understanding this stack matters for three reasons. First, it is the *lingua franca* of backend engineering — nearly every microservice, public API, and mobile backend speaks it. Second, REST design decisions (how you name resources, how you handle retries, how you version) have consequences that are extremely expensive to reverse once external clients depend on them. Third, and most relevant to interviews, REST sits at the intersection of networking, distributed systems, security, and API design, which is exactly why FAANG interviewers reach for it — a single "design this API" prompt lets them probe idempotency, caching, concurrency, and backward compatibility all at once.

This guide walks the full ladder. We begin with the wire-level mechanics of HTTP, climb into REST design principles, then into the correctness and performance concerns that separate a working API from a production-grade one, and finish with the staff-level tradeoffs and six case studies interviewers use repeatedly.

<details>
<summary>📖 In plain terms</summary>

HTTP is the set of rules for how a client (your browser, phone app) and a server exchange messages. REST is a popular *style* of using HTTP where you treat everything as a "resource" (a user, an order, a photo) with a clear address, and you use standard HTTP verbs (GET to read, POST to create, DELETE to remove) to act on them. Get these fundamentals right and most backend systems suddenly look familiar, because they almost all follow the same shape.

</details>

---

## 2. 🎨 The Anatomy of an HTTP Request/Response

HTTP is a **request/response protocol built on top of TCP** (and, in HTTP/3, on QUIC over UDP). The client always initiates: it opens a connection, sends a request message, and the server replies with a response message. Both messages share the same basic structure — a start line, a set of headers, a blank line, and an optional body.

Here is a concrete request and its response. The comments mark the four parts of each message:

```http
GET /v1/users/42 HTTP/1.1        ◄── REQUEST LINE  (method + path + version)
Host: api.example.com            ┐
Accept: application/json         │ headers
Authorization: Bearer eyJhbGc... │
User-Agent: MyApp/2.1            ┘
                                 ◄── blank line (separates headers from body)
                                     (no body — GET has none)
```

```http
HTTP/1.1 200 OK                  ◄── STATUS LINE  (version + code + reason)
Content-Type: application/json   ┐
Cache-Control: max-age=60        │ headers
ETag: "a1b2c3"                   │
Content-Length: 82               ┘
                                 ◄── blank line (separates headers from body)
{"id":42,"name":"Ada Lovelace","email":"ada@example.com"}   ◄── BODY
```

The **request line** carries the method (`GET`), the target path (`/v1/users/42`), and the protocol version. The **status line** in the response carries the version, a numeric status code (`200`), and a human-readable reason phrase (`OK`). Headers are key-value metadata: they describe the body, control caching, carry authentication, and negotiate content formats. The **body** carries the actual payload — JSON, HTML, an image, anything.

A crucial point that the diagram below is drawn to make clear: **each message is sent as one continuous byte stream, not as four separate transmissions.** The start line, headers, blank line, and body are simply *regions within a single message*, laid out one after another and delimited by newlines (`CRLF`). The blank line is the marker that tells the receiver "headers are done — everything after this is the body." So the client sends *one* request message and receives *one* response message; the four parts are structure *inside* those two messages, not four round trips.

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    Note over C,S: ONE request message (a single byte stream)
    C->>S: request line + headers + blank line + body
    Note over S: Server parses the message,<br/>runs business logic
    Note over C,S: ONE response message (a single byte stream)
    S->>C: status line + headers + blank line + body
```

```mermaid
flowchart TB
    subgraph MSG["A single HTTP message (one byte stream)"]
        direction TB
        L["Start line — request line OR status line"]
        H["Headers (key: value, one per line)"]
        B0["◄ blank line — the delimiter"]
        BODY["Body (optional payload: JSON, HTML, image…)"]
        L --> H --> B0 --> BODY
    end
    style L fill:#e6f0ff
    style B0 fill:#ffe6e6
    style BODY fill:#e6ffe6
```

The elegance of REST is that it reuses this generic message structure and assigns *meaning* to its parts: the path identifies a resource, the method declares intent, the status code communicates outcome, and headers carry cross-cutting concerns like caching and auth.

---

## 3. 💻 HTTP Methods (Verbs)

The method declares **what you intend to do** with a resource. Choosing the right one is the single most common REST design decision, and interviewers probe it constantly because the choice carries semantic guarantees — safety and idempotency — that clients, proxies, and CDNs rely on.

| Method | Purpose | Safe? | Idempotent? | Has body? |
|--------|---------|-------|-------------|-----------|
| `GET` | Retrieve a resource | ✅ Yes | ✅ Yes | No |
| `HEAD` | Like GET but headers only | ✅ Yes | ✅ Yes | No |
| `POST` | Create a resource / trigger a process | ❌ No | ❌ No | Yes |
| `PUT` | Replace a resource entirely | ❌ No | ✅ Yes | Yes |
| `PATCH` | Partially update a resource | ❌ No | ⚠️ Not required | Yes |
| `DELETE` | Remove a resource | ❌ No | ✅ Yes | Rarely |
| `OPTIONS` | Discover allowed methods / CORS preflight | ✅ Yes | ✅ Yes | No |

Two properties matter enormously:

**Safe** means the method has no side effects — it only reads. `GET`, `HEAD`, and `OPTIONS` are safe. This is why a browser can pre-fetch a `GET` URL or a crawler can hit it freely, but no framework should ever mutate data on a `GET`.

**Idempotent** means calling it *N* times has the same effect as calling it once. `PUT /users/42 {full object}` is idempotent: replaying it just re-writes the same state. `DELETE /users/42` is idempotent: the resource ends up deleted whether you call it once or five times. But `POST /orders` is **not** idempotent by default — each call creates a new order. This distinction is the bedrock of retry-safe distributed systems, which we return to in depth in [Idempotency Keys](#23--idempotency-keys-in-distributed-systems).

**PUT vs PATCH** is a favorite interview trap. `PUT` replaces the *entire* resource — omit a field and you have effectively cleared it. `PATCH` applies a *partial* modification — you send only the fields you want changed. PATCH is not inherently idempotent: a PATCH that says "add 10 to balance" produces different results each time, whereas a PATCH that says "set status to SHIPPED" is idempotent in practice. Design PATCH bodies to be idempotent (absolute values, not deltas) when you can.

<details>
<summary>📖 In plain terms</summary>

Think of the verb as telling the server your intent. GET is "just let me look" — it never changes anything. POST is "make me a new thing" — run it twice and you get two things. PUT is "here's the complete replacement." PATCH is "just tweak these couple of fields." The important habit: never change data on a GET, and remember that POST is the one verb where retrying can accidentally create duplicates.

</details>

<details>
<summary>💻 Java (Spring): mapping the verbs</summary>

```java
@RestController
@RequestMapping("/v1/users")
public class UserController {

    @GetMapping("/{id}")            // safe + idempotent read
    public User get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping                    // NOT idempotent — creates new
    public ResponseEntity<User> create(@RequestBody CreateUserRequest req) {
        User created = service.create(req);
        return ResponseEntity
                .created(URI.create("/v1/users/" + created.getId())) // 201 + Location
                .body(created);
    }

    @PutMapping("/{id}")            // idempotent — full replace
    public User replace(@PathVariable Long id, @RequestBody User full) {
        return service.replace(id, full);
    }

    @PatchMapping("/{id}")          // partial update
    public User patch(@PathVariable Long id, @RequestBody Map<String,Object> fields) {
        return service.applyPatch(id, fields);
    }

    @DeleteMapping("/{id}")         // idempotent — delete
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build(); // 204
    }
}
```

</details>

---

## 4. 📊 HTTP Status Codes

The status code is a three-digit number that tells the client **the outcome** of its request. The first digit defines a class of response, and getting these classes right is what makes an API predictable for clients, proxies, and monitoring systems.

If you memorize only seven codes, memorize these — they cover the overwhelming majority of real-world responses and are the ones interviewers expect you to reach for instinctively:

| Code | Meaning | When you return it |
|------|---------|--------------------|
| **`200 OK`** | Success | A `GET`, `PUT`, or `PATCH` succeeded and there's a body to return. |
| **`201 Created`** | Resource created | A `POST` created a new resource. Include a `Location` header pointing to it. |
| **`400 Bad Request`** | Malformed request | The client sent something the server can't parse (bad JSON, missing required param). |
| **`401 Unauthorized`** | Not authenticated | No valid credentials — "I don't know who you are." (Misnomer: it's about *authentication*.) |
| **`403 Forbidden`** | Not authorized | Authenticated, but not permitted — "I know who you are, and you may not do this." |
| **`404 Not Found`** | No such resource | The requested URI doesn't map to anything (or is hidden from this caller on purpose). |
| **`500 Internal Server Error`** | Server failure | The server hit an unhandled error. The client's request was fine; the fault is ours. |

With those seven as your foundation, here is the full picture organized by class:

| Class | Meaning | Common members |
|-------|---------|----------------|
| **1xx** | Informational | `100 Continue`, `101 Switching Protocols` |
| **2xx** | Success | `200 OK`, `201 Created`, `202 Accepted`, `204 No Content`, `206 Partial Content` |
| **3xx** | Redirection | `301 Moved Permanently`, `304 Not Modified`, `307 Temporary Redirect` |
| **4xx** | Client error | `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict`, `422 Unprocessable Entity`, `429 Too Many Requests` |
| **5xx** | Server error | `500 Internal Server Error`, `502 Bad Gateway`, `503 Service Unavailable`, `504 Gateway Timeout` |

The distinction that trips people up most is **4xx vs 5xx**: 4xx means *"you (the client) made a mistake — don't retry the same thing and expect a different result."* 5xx means *"I (the server) failed — retrying may succeed."* This single boundary drives client retry logic, alerting thresholds, and SLA calculations, so misclassifying an error (returning 500 for a validation failure, or 200 with an error body) is a genuine production bug.

A few codes deserve specific attention because interviewers ask about them by name:

- **`201 Created`** — used after a successful `POST` that created a resource. Pair it with a `Location` header pointing at the new resource.
- **`202 Accepted`** — the request is valid and accepted, but processing happens *asynchronously*. This is the signature of async APIs (send an email, kick off a video encode). We use it heavily in the [Notification case study](#case-notification).
- **`204 No Content`** — success, but there is nothing to return (typical for `DELETE` or a `PUT` that returns no body).
- **`304 Not Modified`** — the conditional-request response that powers ETag caching (see [Caching](#16--caching-etags-cache-control--conditional-requests)).
- **`409 Conflict`** — the request conflicts with current state, e.g. an optimistic-locking version mismatch or a duplicate creation.
- **`422 Unprocessable Entity`** — the syntax is fine but the semantics are invalid (validation failed). Many teams prefer 422 over 400 for validation errors to distinguish "malformed JSON" (400) from "well-formed but invalid data" (422).
- **`429 Too Many Requests`** — rate limit exceeded; pair with `Retry-After`.
- **`503 Service Unavailable`** — temporary overload/maintenance; also pairs with `Retry-After`.

<details>
<summary>📖 In plain terms</summary>

The number is the server's one-glance verdict. 2xx = worked. 3xx = look somewhere else / you already have it. 4xx = you sent something wrong, fix your request. 5xx = the server broke, maybe try again later. The golden rule engineers live by: a 4xx is the client's fault and retrying won't help; a 5xx is the server's fault and a retry might. Clients build their whole retry strategy on that line.

</details>

---

## 5. 🎨 HTTP Headers

Headers are the **metadata layer** of HTTP. The body carries *what* you are sending; headers carry *how to interpret it, who is asking, and how it may be cached, secured, and negotiated.* Understanding the important ones is essential because so much of REST's power — caching, content negotiation, auth, compression — lives entirely in headers.

They group naturally by purpose:

**Content & negotiation.** `Content-Type` declares the body's media type (`application/json`). `Content-Length` gives its size. `Accept` lets the client say what it *wants* back (`Accept: application/json`), and `Accept-Encoding: gzip, br` advertises which compressions it understands. The server responds with `Content-Encoding: gzip`. This back-and-forth is **content negotiation**, and it even underpins one style of API versioning.

**Caching & concurrency.** `Cache-Control`, `ETag`, `Last-Modified`, `If-None-Match`, and `If-Match` together implement HTTP caching and optimistic concurrency — covered in full in [Caching](#16--caching-etags-cache-control--conditional-requests).

**Authentication & security.** `Authorization: Bearer <token>` carries credentials. `WWW-Authenticate` is how a server challenges. Security headers like `Strict-Transport-Security`, `Content-Security-Policy`, and the CORS family (`Access-Control-Allow-Origin`, etc.) live here too.

**Tracing & control.** `X-Request-Id` / `traceparent` correlate a request across services. `Retry-After` tells a throttled client when to come back. Custom `X-*` headers historically carried app-specific data, though the `X-` convention is now discouraged by the IETF in favor of registered names.

<details>
<summary>📖 In plain terms</summary>

If the body is the letter, headers are everything written on the envelope: what language the letter is in (`Content-Type`), who it's from and whether they're allowed in (`Authorization`), whether the recipient can keep a copy for later (`Cache-Control`), and how big it is. Most of HTTP's cleverness — caching, compression, auth, versioning — is negotiated purely through these envelope notes, without touching the letter itself.

</details>

---

## 6. 🎯 Statelessness

One of REST's defining constraints is that **every request must be self-contained**: the server does not rely on any client-specific context stored from previous requests. If a request needs authentication, it carries its token *every time*. If it needs to know which page of results you want, it carries the cursor *every time*. The server keeps no per-client session in memory between requests.

This sounds like a limitation, but it is the property that lets REST scale horizontally. Because no request depends on server-side session memory, **any server in a fleet can handle any request**. You can put a hundred identical stateless servers behind a load balancer, and it does not matter which one a given request lands on.

### A concrete example: the stateful trap

Let's make this concrete with a shopping cart, because it's the classic case where the difference bites. Imagine a **stateful** design that stores the cart in the server's memory:

```
Request 1  →  Server A:  "Add item BOOK-42 to cart"
              Server A remembers in local memory: cart = [BOOK-42]

Request 2  →  Server B:  "Add item PEN-7 to cart"
              Server B has NEVER heard of this user's cart.
              Its local memory says: cart = [PEN-7]      ❌ BOOK-42 is gone!
```

The user's second request was load-balanced to a *different* server, and because the cart lived only in Server A's memory, Server B has no idea it exists. The cart appears to lose items at random. The usual patch for this is **sticky sessions** — configuring the load balancer to always send this user back to Server A — but that creates new problems: Server A becomes a single point of failure (if it crashes, the cart is gone and the user is logged out), load can't be balanced evenly (a "hot" server stays hot), and you can't freely add or drain servers during a deploy.

Now the **stateless** version. The server keeps *nothing* in local memory; the cart lives in a shared store (a database or Redis) keyed by the user, and every request carries the user's identity (via a token):

```
Request 1  →  Server A:  "Add BOOK-42" + token(user=42)
              Server A writes to shared Redis:  cart:42 = [BOOK-42]

Request 2  →  Server B:  "Add PEN-7"   + token(user=42)
              Server B reads cart:42 from Redis → [BOOK-42], appends → [BOOK-42, PEN-7]   ✅
```

Now it *doesn't matter* which server handles the request — both read and write the same shared cart. Server B produced the correct result even though it had never seen this user before, because the request was self-contained (it carried the user's token) and the state lived in a shared store, not in any one server's memory.

```mermaid
flowchart LR
    C1[Client A] --> LB{Load Balancer}
    C2[Client B] --> LB
    C3[Client C] --> LB
    LB -->|any request<br/>to any server| S1[Server 1]
    LB --> S2[Server 2]
    LB --> S3[Server 3]
    S1 -.read/write.-> DB[(Shared State<br/>Redis / DB)]
    S2 -.read/write.-> DB
    S3 -.read/write.-> DB
    style DB fill:#e8f0fe
```

The payoff, restated: add capacity by adding servers; lose a server and *no* user sessions are destroyed (the state was never in that server); deploy by draining and replacing servers one at a time with zero user impact. That elasticity is the entire reason cloud auto-scaling works, and it is only possible because the request-handling tier is stateless.

The nuance interviewers push on: **statelessness does not mean the system has no state** — it means the *application server* holds no *session* state. State still lives somewhere: in the database, in a distributed cache like Redis, or in the token itself (a JWT encodes the user's identity so the server needn't even do a lookup to know who's calling). The art is pushing state to those shared, purpose-built stores so the request-handling tier stays stateless and elastic. The one real cost is a network hop to fetch shared state on each request — which is exactly why fast in-memory stores like Redis exist and why self-contained JWTs are attractive (they let the server skip the session lookup entirely).

<details>
<summary>📖 In plain terms</summary>

Statelessness means each request stands on its own — it carries everything the server needs to handle it (like your auth token and which page you want), so the server doesn't have to "remember" you from last time. The payoff is huge: since no server is holding your session, any server can serve you, so you can add or remove servers freely. The state doesn't vanish; it just moves to a shared database or cache that all servers read from.

</details>

---

## 7. 🔗 The Request Lifecycle

Before we leave HTTP fundamentals, it helps to trace a single request end-to-end. When you type `https://api.example.com/v1/users/42` and hit enter, a surprising amount happens:

```mermaid
sequenceDiagram
    participant App as Client App
    participant DNS as DNS Resolver
    participant LB as Load Balancer / CDN
    participant GW as API Gateway
    participant Svc as Service
    participant DB as Database

    App->>DNS: Resolve api.example.com
    DNS-->>App: IP address
    App->>LB: TCP + TLS handshake
    Note over App,LB: TLS negotiates encryption (HTTPS)
    App->>LB: GET /v1/users/42 + headers
    LB->>GW: Route request
    GW->>GW: AuthN/AuthZ, rate limit, validate
    GW->>Svc: Forward to service
    Svc->>DB: Query user 42
    DB-->>Svc: Row
    Svc-->>GW: 200 + JSON
    GW-->>LB: 200 + JSON
    LB-->>App: 200 + JSON (maybe cached)
```

The stages, in order: **DNS resolution** turns the hostname into an IP. A **TCP handshake** (and, for HTTPS, a **TLS handshake**) establishes a secure connection. The request travels to a **load balancer or CDN**, which may serve a cached response outright. If not, it reaches the **API gateway**, which handles cross-cutting concerns — authentication, authorization, rate limiting, request validation — before routing to the correct **service**. The service executes business logic, reads or writes the **database or cache**, and produces a response that flows back along the same path, potentially being cached on the way out.

Notice how many concerns are handled *before* your business logic runs — TLS, routing, auth, rate limiting. This layered pipeline is precisely why [API gateways](#25--api-gateway--backend-for-frontend-patterns) exist, and why understanding the lifecycle helps you reason about where latency and failures come from.

---

# Part II — Designing RESTful APIs

## 8. 🎯 What REST Actually Is

**REST — Representational State Transfer — is an architectural style, not a protocol or a standard.** It was defined by Roy Fielding in his 2000 doctoral dissertation as a set of *constraints* that, when applied to a networked system, yield desirable properties like scalability, evolvability, and simplicity. When people say "REST API" they usually mean "a JSON-over-HTTP API," but strictly speaking REST is the collection of constraints below, and most real APIs satisfy only some of them.

Fielding's constraints are: a **client-server** separation of concerns; **statelessness** (each request self-contained); **cacheability** (responses declare whether they can be cached); a **uniform interface** (the same small set of verbs and resource-addressing rules everywhere); a **layered system** (gateways, proxies, and caches can sit between client and server transparently); and optionally **code-on-demand** (the server can ship executable code, e.g. JavaScript).

The **uniform interface** is the heart of REST and the source of its power. It says: identify resources with URIs, manipulate them through representations (JSON, XML), make messages self-descriptive (the method and headers tell you everything), and use hypermedia to drive state (the controversial HATEOAS constraint). The practical payoff is that once you learn how to talk to *one* REST API, you largely know how to talk to *all* of them — the same `GET`/`POST`/`PUT`/`DELETE`, the same status codes, the same header semantics.

It is worth stating plainly: most "REST" APIs in the wild are really **REST-ish**. They nail resources, verbs, and status codes but skip HATEOAS. That is a legitimate engineering choice, and the [Richardson Maturity Model](#11--the-richardson-maturity-model) gives us vocabulary to describe exactly *how* RESTful a given API is.

<details>
<summary>📖 In plain terms</summary>

REST is a set of design rules for web APIs, not a piece of software you install. The rules boil down to: treat data as "resources" with clear addresses, use the standard HTTP verbs to act on them, keep each request self-contained, and let responses say whether they can be cached. Follow these and your API becomes predictable — anyone who has used one REST API can guess how yours works. In practice most teams follow the useful 80% and skip the strictest rule (HATEOAS).

</details>

---

## 9. 🎨 Resource Modeling: Nouns vs Verbs & URI Design

The first real design decision in any REST API is **what your resources are and how you name them**. A resource is any concept worth addressing — a user, an order, a photo, a bank account. The cardinal rule: **URIs should be nouns, not verbs.** The verb is already carried by the HTTP method, so encoding actions into the path is redundant and un-RESTful.

| ❌ Verb-in-URI (RPC-style) | ✅ Noun + HTTP method (REST) |
|----------------------------|------------------------------|
| `POST /createUser` | `POST /users` |
| `GET /getUserById?id=42` | `GET /users/42` |
| `POST /users/42/delete` | `DELETE /users/42` |
| `POST /updateOrderStatus` | `PATCH /orders/42` |
| `GET /searchProducts?q=x` | `GET /products?q=x` |

Beyond the noun rule, a set of conventions makes URIs predictable and pleasant:

**Use plural nouns for collections.** `GET /users` returns many; `GET /users/42` returns one. Consistency here (never mixing `/user/42` and `/users`) matters more than which convention you pick.

**Model relationships through nesting — but shallowly.** `GET /users/42/orders` reads naturally as "orders belonging to user 42." But avoid deep nesting like `/users/42/orders/7/items/3/reviews` — it is brittle and hard to link to. A common rule of thumb is to nest at most one level deep and otherwise expose resources at the top level (`GET /order-items/3`). We debate exactly this tradeoff in the [Social Feed case study](#case-social).

**Keep URIs lowercase, hyphenated, and stable.** `/order-items`, not `/orderItems` or `/order_items`. Once a URI is public, changing it breaks clients, so treat URI design as a near-permanent commitment.

**Query parameters are for filtering, sorting, and pagination — not for identifying resources.** `/products/42` identifies; `/products?category=books&sort=price` refines a collection.

<details>
<summary>📖 In plain terms</summary>

Design your API around "things" (nouns) rather than "actions" (verbs), because the action is already in the HTTP method. So instead of a `/createOrder` endpoint, you have an `/orders` endpoint and you POST to it. Use plural names for lists (`/orders`), put the ID after for a single item (`/orders/42`), keep names lowercase-with-hyphens, and once a URL is public treat it as permanent — clients depend on it.

</details>

---

## 10. 🔗 HATEOAS

**HATEOAS — Hypermedia As The Engine Of Application State** — is REST's most sophisticated and least-adopted constraint. The idea: a response should not just contain data, but also **links describing what you can do next**. The client discovers available actions dynamically from the response rather than hard-coding URL patterns.

Compare a plain response with a HATEOAS one:

```json
// Without HATEOAS — client must know URL structure
{ "id": 42, "status": "PENDING", "total": 99.90 }
```

```json
// With HATEOAS — response advertises next actions
{
  "id": 42,
  "status": "PENDING",
  "total": 99.90,
  "_links": {
    "self":   { "href": "/orders/42" },
    "cancel": { "href": "/orders/42/cancel", "method": "POST" },
    "pay":    { "href": "/orders/42/payment", "method": "POST" }
  }
}
```

The theoretical appeal is **decoupling and evolvability**: clients follow links rather than constructing URLs, so the server can restructure its URIs without breaking clients, and the set of valid next actions (can I cancel this order?) is driven by server-side state rather than duplicated client-side logic. If the order is already shipped, the server simply omits the `cancel` link, and the client's UI naturally reflects that.

In practice, **HATEOAS is rarely fully adopted.** Real clients — especially mobile apps with baked-in navigation — still hard-code URLs for performance and simplicity, the tooling ecosystem is thin, and the extra payload has a cost. It shines in specific domains: **Open Banking / PSD2 APIs** use hypermedia links heavily, and the **GitHub API** returns `_links`-style relations. For most internal microservices, teams consciously skip it. Knowing *why* it exists and *why* it is usually skipped is exactly the nuanced answer interviewers want.

<details>
<summary>📖 In plain terms</summary>

HATEOAS means the API response includes not just the data but a little menu of links telling the client what it can do next — "here's how to cancel this order, here's how to pay for it." If an action isn't available (the order already shipped), the server just leaves that link out. In theory this lets the server change its URLs freely. In reality most apps ignore it and hard-code URLs, but banking and GitHub-style APIs do use it.

</details>

---

## 11. 📊 The Richardson Maturity Model

Leonard Richardson proposed a four-level model that grades **how RESTful an API is**, and it gives interviews a shared vocabulary. Martin Fowler popularized it. Each level adds one of REST's ingredients.

```mermaid
flowchart TB
    L0["**Level 0 — The Swamp of POX**<br/>Single URI, single verb (usually POST).<br/>e.g. one /api endpoint, action in the body"]
    L1["**Level 1 — Resources**<br/>Many URIs, still one verb.<br/>e.g. POST /orders, POST /users"]
    L2["**Level 2 — HTTP Verbs**<br/>Proper methods + status codes.<br/>GET/POST/PUT/DELETE, 200/201/404"]
    L3["**Level 3 — Hypermedia (HATEOAS)**<br/>Responses carry links to next actions"]
    L0 --> L1 --> L2 --> L3
    style L2 fill:#e6ffe6
    style L3 fill:#fff3cd
```

To make the levels click, we'll build **one running example** — a doctor's-appointment booking API — and climb it level by level, watching the *same* feature get more RESTful at each step. The goal each time: *let a patient see open slots and book an appointment.*

#### Level 0 — "The Swamp of POX" (Plain Old XML)

One endpoint, one verb. Everything is tunneled through a single URL, with the actual operation named *inside the body*. The URL and the HTTP method tell you nothing:

```http
POST /appointmentService HTTP/1.1
{ "action": "getOpenSlots", "doctorId": 17, "date": "2026-08-10" }

POST /appointmentService HTTP/1.1
{ "action": "bookAppointment", "doctorId": 17, "slotId": 5, "patientId": 42 }
```

Both calls hit `/appointmentService` with `POST`. The server dispatches on the `action` field in the body. This is essentially RPC (remote procedure call) wearing an HTTP costume — SOAP and old XML-RPC services live here. HTTP is just a dumb tunnel; none of its features (caching, status semantics, verb meaning) are used.

#### Level 1 — Resources

You break the single endpoint into **many resource URIs**, one per "thing." Now the URL identifies *what* you're talking about — but you still use one verb (usually `POST`) for everything, and the action still lives in the body:

```http
POST /doctors/17 HTTP/1.1
{ "action": "getOpenSlots", "date": "2026-08-10" }

POST /appointments HTTP/1.1
{ "action": "book", "doctorId": 17, "slotId": 5 }
```

Progress: there's now a `/doctors/17` resource and an `/appointments` resource, so requests are addressed to specific nouns instead of one god-endpoint. But you still can't tell a *read* from a *write* by looking at the request, and a cache still can't safely cache `/doctors/17` because it's a `POST`. You have nouns, but not the uniform verb interface.

#### Level 2 — HTTP Verbs (and status codes)

Now you use the **methods for their real meaning** and return **meaningful status codes**. The verb declares intent; the body only carries data, not actions:

```http
GET /doctors/17/slots?date=2026-08-10 HTTP/1.1     → 200 OK  (a safe, cacheable read)

POST /appointments HTTP/1.1                          → 201 Created
{ "doctorId": 17, "slotId": 5 }                        Location: /appointments/990
```

The leap is huge. `GET` is now a safe, idempotent, *cacheable* read — a CDN or browser can cache the slot list. `POST` clearly means "create," and it returns `201 Created` with a `Location` header pointing to the new appointment. If slot 5 was already taken, the server returns `409 Conflict`; if the doctor doesn't exist, `404`. **This is where the vast majority of production "REST" APIs sit, and it captures most of REST's practical value** — caching works, verbs are predictable, errors are standardized, and any developer can guess how the API behaves.

#### Level 3 — Hypermedia Controls (HATEOAS)

The final step: responses carry **links telling the client what it can do next**, so the client discovers actions rather than hard-coding URLs. The booked appointment advertises its own next steps:

```http
GET /appointments/990 HTTP/1.1
→ 200 OK
{
  "id": 990,
  "status": "CONFIRMED",
  "doctorId": 17,
  "_links": {
    "self":        { "href": "/appointments/990" },
    "cancel":      { "href": "/appointments/990/cancellation", "method": "POST" },
    "reschedule":  { "href": "/appointments/990/slot", "method": "PUT" }
  }
}
```

If the appointment is in the past, the server simply *omits* the `cancel` and `reschedule` links, so the client's UI naturally reflects what's allowed *right now* — driven by server state, not duplicated client logic. Full REST, per Fielding, technically requires this level, but as discussed few APIs reach it.

The practical takeaway for interviews: **aim for Level 2 as your default, and reach for Level 3 only when evolvability genuinely demands it** (public banking APIs, long-lived clients you can't force to upgrade). Saying "we're a solid Level 2, and here's why Level 3 wasn't worth the cost for our internal services" is a mature, senior answer.

| Level | `/appointmentService` example | What's gained |
|-------|-------------------------------|---------------|
| **0** | `POST /appointmentService` + `action` in body | Nothing — HTTP is a tunnel |
| **1** | `POST /doctors/17`, `POST /appointments` | Resources have addresses (nouns) |
| **2** | `GET /doctors/17/slots`, `POST /appointments` → 201 | Real verbs + status codes + caching |
| **3** | Response carries `cancel`/`reschedule` links | Server drives valid next actions |

<details>
<summary>📖 In plain terms</summary>

Richardson's model is a 0-to-3 scorecard for how "properly REST" an API is. Level 0: one URL, everything is POST (basically not REST). Level 1: many URLs but still all POST. Level 2: real verbs (GET/POST/PUT/DELETE) and real status codes — this is where almost every good API lives. Level 3: adds the "what can I do next" links (HATEOAS). Most teams aim for Level 2 on purpose and treat Level 3 as optional.

</details>

---

## 12. 💻 Request & Response Design

A well-designed API is consistent about *where information goes* and *what a response looks like.* There are four channels for carrying information into a request, and choosing correctly is a frequent interview probe.

**Path variables** identify a specific resource: `/orders/{orderId}`. Use them for the resource's identity — the thing is not optional and not a filter.

**Query parameters** refine or shape a collection: `/orders?status=shipped&sort=-createdAt&page=2`. Use them for filtering, sorting, pagination, and optional modifiers. They should never change *which* resource you address, only how you view a collection.

**Headers** carry metadata and cross-cutting concerns: authentication (`Authorization`), content negotiation (`Accept`), caching (`If-None-Match`), and idempotency (`Idempotency-Key`). Anything that is *about* the request rather than *part of* the resource belongs here.

**Body** carries the resource representation for writes (`POST`, `PUT`, `PATCH`). `GET` and `DELETE` conventionally have no body.

On the response side, consistency is everything. Pick an **envelope strategy and stick to it.** Two common patterns:

```json
// Pattern A — data at top level, metadata alongside (common, clean)
{
  "data": [ {"id": 1}, {"id": 2} ],
  "pagination": { "next_cursor": "abc", "limit": 20 }
}
```

```json
// Pattern B — bare resource for single items
{ "id": 42, "name": "Ada", "email": "ada@example.com" }
```

Whatever you choose, apply it uniformly, use consistent field naming (`snake_case` *or* `camelCase`, never both), always return the same shape for the same endpoint, and use a **standard error format** for all failures — which brings us to [RFC 7807](#17--error-handling--rfc-7807-problem-details).

<details>
<summary>📖 In plain terms</summary>

There are four places to put information in a request, and each has a job: the path says *which* resource (`/orders/42`), query parameters *filter or sort* a list (`?status=shipped`), headers carry *behind-the-scenes* stuff like auth and caching, and the body carries the actual data you're saving. On the way back, keep every response in the same predictable shape and use one consistent error format — surprises are what make APIs painful to use.

</details>

---

## 13. 📊 Pagination, Filtering & Sorting

Any collection that can grow — orders, posts, transactions — **must** be paginated. Returning "all users" is a latent outage waiting for the day the table hits ten million rows. There are two dominant pagination strategies, and the choice between them is a classic scaling question.

**Offset / limit pagination** is the intuitive one: `GET /posts?offset=40&limit=20` means "skip 40, give me the next 20." It is trivial to implement (`OFFSET 40 LIMIT 20` in SQL) and supports jumping to arbitrary pages ("go to page 5"). But it has two serious flaws at scale. First, **it gets slower the deeper you go** — the database must scan and discard all `offset` rows before returning yours, so page 10,000 is expensive. Second, it suffers from **drift**: if items are inserted or deleted while a user pages through, rows shift and you can see duplicates or skip items.

**Cursor (keyset) pagination** solves both: `GET /posts?cursor=eyJpZCI6MTA0fQ&limit=20`. The cursor is an opaque token encoding "where you left off" (typically the last item's sort key, like `created_at + id`). The query becomes `WHERE (created_at, id) < (?, ?) ORDER BY created_at DESC, id DESC LIMIT 20` — an indexed range scan that is **fast at any depth** and **stable** against inserts, because it anchors on a value rather than a position. The tradeoff: you can only go next/previous, not jump to an arbitrary page, and the cursor must be opaque so clients don't build fragile logic on its contents.

```mermaid
flowchart LR
    subgraph Offset["Offset pagination"]
        direction TB
        O1["page 2 = OFFSET 20"]
        O2["DB scans + discards 20 rows"]
        O3["❌ slow when deep<br/>❌ drifts on insert"]
    end
    subgraph Cursor["Cursor pagination"]
        direction TB
        C1["cursor = last seen (created_at,id)"]
        C2["WHERE (created_at,id) < cursor<br/>indexed range scan"]
        C3["✅ fast at any depth<br/>✅ stable on insert"]
    end
```

<details>
<summary>📖 Worked example: why the cursor wins(expand to read)</summary>

Say we're paging through a feed of posts, newest first, 3 per page. The table right now:

```
id=105  "Good morning"     10:05
id=104  "Coffee time"      10:04
id=103  "Deploying now"    10:03
id=102  "Lunch break"      10:02
id=101  "Heading home"     10:01
id=100  "Good night"       10:00
```

**With OFFSET.** Page 1 is `?offset=0&limit=3` → you get posts 105, 104, 103. Now, *before* you ask for page 2, a new post arrives:

```
id=106  "Just woke up"     10:06   ← brand-new, jumps to the top
id=105  "Good morning"     10:05
id=104  "Coffee time"      10:04
...
```

You request page 2 = `?offset=3&limit=3`. The database skips the first 3 rows — which are now **106, 105, 104** — and returns rows 4-6: **103, 102, 101**. Wait — but you already *saw* post 103 on page 1, and now it has been pushed down to position 4 and served again. Post **103 appears on both pages**. That's **drift**: because offset counts *positions*, and inserting 106 shifted every existing post down by one position, your page-2 window slid back over a post you'd already seen. (A deletion causes the mirror problem — a post shifts *up* past the boundary and you *skip* it entirely.)

**With a cursor.** Page 1 returns posts 105, 104, 103, and the response hands back a `next_cursor` that encodes *the last item you actually saw* — post 103, i.e. `(10:03, id=103)`. When you ask for page 2, you send that cursor, and the query is literally "give me posts *older than* 103":

```sql
SELECT * FROM posts
WHERE (created_at, id) < ('10:03', 103)   -- everything strictly after what I saw
ORDER BY created_at DESC, id DESC
LIMIT 3;
```

Even though post 106 was inserted at the top, it doesn't matter — the cursor anchors on the *value* `(10:03, 103)`, not on a position count. Page 2 correctly returns **102, 101, 100** with no duplicate and no skip. And because `(created_at, id)` is indexed, the database jumps straight to that point instead of scanning and discarding rows, so page 2 is just as fast as page 2,000.

```
Page 1:  [105, 104, 103]   next_cursor = encode(10:03, 103)
   ↓ (post 106 inserted at top — irrelevant to the cursor)
Page 2:  GET /posts?cursor=<10:03,103>  →  [102, 101, 100]   ✅ no dup, no skip, fast
```

The `next_cursor` is deliberately **opaque** — usually the sort key base64-encoded, like `eyJ0cyI6IjEwOjAzIiwiaWQiOjEwM30`. Clients treat it as a black box they echo back, so you can change what's inside it later without breaking them.

</details>

**The rule of thumb:** use cursor pagination for large, frequently-changing, or infinite-scroll feeds (this is why Twitter, Instagram, Slack, and Stripe all use cursors); use offset only for small, admin-style tables where "jump to page N" genuinely matters and the dataset is bounded.

**Filtering** goes in query params: `?status=active&min_price=10`. **Sorting** too, commonly with a `-` prefix for descending: `?sort=-created_at,name`. Keep the filter/sort grammar consistent and documented, and be careful to only allow filtering/sorting on **indexed columns** — an unindexed `sort=` on a huge table is an easy way to melt your database.

<details>
<summary>📖 In plain terms</summary>

Never return a whole giant list at once — hand it out in pages. The simple way, offset ("skip 40, give me 20"), is easy and lets you jump to any page, but it gets slow deep in the list and can show duplicates if new items arrive. The scalable way, cursors, remembers "the last item you saw" and grabs the next batch from there — fast no matter how deep, and stable when new items appear. That's why feeds like Twitter and Stripe use cursors; you just can't jump straight to "page 500."

</details>

<details>
<summary>💻 Java (Spring): cursor pagination</summary>

```java
public record Page<T>(List<T> data, String nextCursor) {}

@GetMapping("/posts")
public Page<Post> feed(
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") int limit) {

    // Decode opaque cursor -> (createdAt, id) of last seen row
    CursorPos pos = (cursor == null) ? null : CursorCodec.decode(cursor);

    // Fetch limit+1 to know if another page exists
    List<Post> rows = repo.fetchAfter(pos, limit + 1);

    boolean hasMore = rows.size() > limit;
    List<Post> page = hasMore ? rows.subList(0, limit) : rows;

    String next = hasMore
        ? CursorCodec.encode(new CursorPos(
              page.get(page.size()-1).getCreatedAt(),
              page.get(page.size()-1).getId()))
        : null;

    return new Page<>(page, next);
}
// SQL behind fetchAfter:
// SELECT * FROM posts
// WHERE (created_at, id) < (:ts, :id)   -- omit WHERE for first page
// ORDER BY created_at DESC, id DESC
// LIMIT :limitPlusOne
```

</details>

---

## 14. 🎯 API Versioning

APIs evolve, but the clients calling them do not upgrade in lockstep — a mobile app installed on millions of phones may keep calling your API for *years*. Versioning is how you make breaking changes without breaking those clients. First, the crucial distinction:

A **non-breaking change** is safe to ship without a new version: adding a new optional field, adding a new endpoint, adding a new optional query parameter. Clients that don't know about the addition simply ignore it. A **breaking change** requires versioning: removing or renaming a field, changing a field's type, making an optional field required, changing status-code semantics, or restructuring a response. The engineering discipline is **"be conservative in what you send, liberal in what you accept"** (Postel's Law) — additive evolution avoids most version bumps entirely.

When a breaking change is truly unavoidable, there are three main strategies:

| Strategy | Example | Pros | Cons |
|----------|---------|------|------|
| **URI versioning** | `GET /v1/users/42` | Dead simple, visible, cache-friendly, easy to route | "Version" arguably shouldn't be in the resource identity; proliferates URLs |
| **Header versioning** | `Accept-Version: 2` (custom header) | Keeps URIs clean & stable | Invisible, harder to test in a browser, easy to forget |
| **Content negotiation** | `Accept: application/vnd.example.v2+json` | Purest REST; versions the *representation* not the resource | Verbose, unfamiliar to many, tooling friction |

**In practice, URI versioning (`/v1/`) dominates** — Stripe (partly), Twitter, GitHub's older API, and most public APIs use it because it is unambiguous, trivially routable at the gateway, and easy for developers to see and test. Its purist critics are right that a resource's identity shouldn't include a version, but pragmatism wins.

The most sophisticated approach in the wild is **Stripe's date-based versioning**: each account is pinned to the API version in effect when it first integrated (e.g. `Stripe-Version: 2024-06-20`), and Stripe maintains internal compatibility shims that transform new responses back into old shapes. Clients never break, and they upgrade on their own schedule. This is the gold-standard answer for "how do you version at scale," and it connects directly to [backward compatibility](#22--backward-compatibility--deprecation-at-scale).

<details>
<summary>📖 In plain terms</summary>

Old versions of your app keep calling your API long after you've moved on, so you can't just change things freely. Adding a new field is safe (old clients ignore it); removing or renaming one breaks them — that needs a new version. The most common approach is to put the version in the URL (`/v1/`, `/v2/`). Stripe does something fancier: it pins each customer to the version they signed up with and quietly translates new responses into the old shape, so nothing ever breaks.

</details>

---

# Part III — Correctness & Reliability

## 15. 🎯 Idempotency

**An operation is idempotent if performing it multiple times has the same effect as performing it once.** This is not academic — it is the property that makes distributed systems survivable. Networks are unreliable: a client sends `POST /payments`, the server charges the card successfully, but the response is lost to a network blip. The client sees a timeout and retries. Without idempotency, the customer is charged twice.

As we saw, `GET`, `PUT`, and `DELETE` are idempotent by their HTTP contract, while **`POST` is the dangerous one** — it is defined to create something new on every call. Yet `POST` is exactly what we use for the most critical, must-not-duplicate operations: creating orders, charging payments, sending notifications. The reconciliation is the **idempotency key**.

The client generates a unique key (a UUID) for each *logical* operation and sends it in a header:

```http
POST /v1/charges HTTP/1.1
Idempotency-Key: 4f3c9a2e-8b1d-4e77-9c1a-2f9e6d5b4a3c
Content-Type: application/json

{ "amount": 5000, "currency": "usd", "source": "tok_visa" }
```

The server stores the key alongside the result of the first successful execution. If the *same* key arrives again — because the client retried — the server does **not** re-execute; it returns the *stored* original response. One logical charge, no matter how many times the request is delivered.

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant K as Idempotency Store

    C->>S: POST /charges (Key: 4f3c…)
    S->>K: Seen 4f3c… before?
    K-->>S: No
    S->>S: Execute charge, get charge_789
    S->>K: Store 4f3c…, value {201, charge_789}
    S-->>C: 201 Created (charge_789)
    Note over C,S: ⚡ Response lost / timeout — client retries
    C->>S: POST /charges (Key: 4f3c…) [retry]
    S->>K: Seen 4f3c… before?
    K-->>S: Yes, return {201, charge_789}
    S-->>C: 201 Created (charge_789)  ✅ same result, no double charge
```

This is the mechanism Stripe made famous with its `Idempotency-Key` header, and it is now standard for any write that costs money or has irreversible effects. We cover the distributed-systems subtleties — key scope, storage TTL, race conditions between concurrent retries — at staff level in [Idempotency Keys in Distributed Systems](#23--idempotency-keys-in-distributed-systems).

### Where does the key come from, and when does it change?

The single most important rule: **the key identifies one *logical user action*, not one network request.** It is generated **once, on the client, at the moment the user commits to the action** — the instant they tap "Pay" — and then *reused* on every automatic retry of that same action. Generate a version-4 UUID (a random 128-bit value with negligible collision odds):

```java
// Client side — generate ONCE when the user taps "Pay"
String idempotencyKey = UUID.randomUUID().toString();
// e.g. "4f3c9a2e-8b1d-4e77-9c1a-2f9e6d5b4a3c"

// Attach the SAME key to the first attempt AND every retry of THIS payment
for (int attempt = 0; attempt < maxRetries; attempt++) {
    Response r = http.post("/v1/charges")
                     .header("Idempotency-Key", idempotencyKey)   // ← unchanged across retries
                     .body(chargeJson)
                     .send();
    if (r.isSuccessOrClientError()) break;   // stop retrying on 2xx/4xx
    // on timeout / 5xx: loop and retry with the SAME key
}
```

Now the two cases the interviewer will press you on:

**Same action, retried by the same user → SAME key.** The user tapped "Pay" once. Their phone lost signal and the app auto-retried three times. All four HTTP requests carry the *same* `4f3c9a2e-…` key, because they are all the *same logical charge*. The server executes once and replays the stored result for the other three. **One tap → one charge.** This is the whole point: the key must survive the retry, so it is generated *before* the first attempt and held constant, never regenerated per request.

**Different action, or a different user → DIFFERENT key.** The moment the user taps "Pay" *again* for a genuinely new purchase, the client generates a *fresh* UUID — because this is a different logical action that *should* result in a new charge. Likewise, a different user tapping "Pay" on their own device generates their own independent UUID. Keys are unique per logical operation, so two distinct intended charges never collide.

```
User A taps "Pay" for order #1  → key = 4f3c…aaa   → 1 charge
   ├─ retry (timeout)           → key = 4f3c…aaa   → replays, no new charge ✅
   └─ retry (timeout)           → key = 4f3c…aaa   → replays, no new charge ✅
User A taps "Pay" for order #2  → key = 9b2e…bbb   → NEW charge ✅ (different action)
User B taps "Pay" for order #3  → key = c7d1…ccc   → NEW charge ✅ (different user)
```

Two hard-won details that make this robust in practice. First, the key must be **client-generated, never server-generated** — if the server minted the key, a lost response would leave the client with no key to retry with, defeating the purpose. Second, servers **scope the key** (typically per authenticated account + endpoint) and often store a **hash of the request body** alongside it, so if the *same* key ever arrives with a *different* payload — a client bug where a UUID got accidentally reused — the server rejects it rather than silently returning the wrong stored result.

<details>
<summary>📖 In plain terms</summary>

Idempotent means "doing it again changes nothing new." Reading a page (GET) or deleting an item (DELETE) is naturally like this. But creating things with POST is risky: if the network hiccups and your app retries a payment, you could get charged twice. The fix is an idempotency key — a unique ID the client attaches to the request. The server remembers "I already did this one" and just replays the original result instead of charging again.

</details>

---

## 16. 📊 Caching: ETags, Cache-Control & Conditional Requests

Caching is where HTTP quietly delivers enormous performance wins, and it is built entirely into headers. There are two complementary questions: *"can I avoid re-fetching this at all?"* (freshness) and *"has this changed since I last saw it?"* (validation).

**Freshness — `Cache-Control`.** The server declares how long a response may be reused without asking again:

```http
Cache-Control: max-age=3600, public        # cacheable by anyone for 1 hour
Cache-Control: private, max-age=60          # only the browser, not shared caches
Cache-Control: no-cache                     # store, but always revalidate before use
Cache-Control: no-store                     # never cache (sensitive data)
```

While a response is *fresh*, the client (or CDN) serves it straight from cache with **zero network round trips** — the fastest possible request is the one you never make. `public` responses can be cached by shared intermediaries like CDNs; `private` restricts caching to the end user's browser.

**Validation — `ETag` and conditional requests.** When a cached copy goes stale, the client shouldn't blindly re-download it — it might be unchanged. An **ETag** is a version fingerprint (often a hash) the server attaches to a response:

```http
HTTP/1.1 200 OK
ETag: "a1b2c3"
Cache-Control: max-age=60
```

On the next fetch after expiry, the client asks *conditionally* using `If-None-Match`:

```http
GET /v1/users/42
If-None-Match: "a1b2c3"
```

If the resource is unchanged, the server replies **`304 Not Modified`** with an empty body — the client reuses its cached copy, saving the bandwidth of re-sending the payload. If it changed, the server sends `200` with fresh data and a new ETag. `Last-Modified` / `If-Modified-Since` provide the same mechanism using timestamps rather than hashes.

```mermaid
sequenceDiagram
    participant C as Client (has ETag "a1b2c3")
    participant S as Server
    C->>S: GET /users/42<br/>If-None-Match: "a1b2c3"
    alt Resource unchanged
        S-->>C: 304 Not Modified (empty body) ✅ bandwidth saved
    else Resource changed
        S-->>C: 200 OK + new body + ETag "d4e5f6"
    end
```

**ETags do double duty: optimistic concurrency control.** The *same* ETag, sent as `If-Match` on a write, prevents lost updates. Two users both fetch order 42 (ETag `"a1b2c3"`). Both edit it. The first `PUT` with `If-Match: "a1b2c3"` succeeds and the ETag becomes `"d4e5f6"`. The second user's `PUT` still carries `If-Match: "a1b2c3"`, which no longer matches, so the server rejects it with **`412 Precondition Failed`** — no silent overwrite of the first user's change. This "compare-and-swap over HTTP" is the standard REST answer to concurrent edits, and it shows up in the [E-commerce inventory case study](#case-ecommerce).

<details>
<summary>📖 In plain terms</summary>

Caching has two tricks. First, `Cache-Control: max-age=3600` lets the browser or CDN reuse a response for an hour without asking again — the fastest request is one you never send. Second, when that expires, instead of re-downloading, the client asks "has this changed?" by sending the ETag (a version fingerprint). If nothing changed, the server says `304 Not Modified` with no body — saving bandwidth. Bonus: the same ETag on a write blocks two people from silently overwriting each other's edits.

</details>

---

## 17. ❌ Error Handling & RFC 7807 Problem Details

Nothing frustrates an API consumer faster than inconsistent, uninformative errors — a `500` here, a `{"error":"bad"}` there, an HTML page somewhere else. A production API needs **one consistent, machine-readable error format**, and the standard for that is **RFC 7807 — "Problem Details for HTTP APIs"** (updated by RFC 9457).

The idea is a standard JSON body with the media type `application/problem+json`:

```http
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{
  "type": "https://api.example.com/problems/insufficient-funds",
  "title": "Insufficient funds",
  "status": 422,
  "detail": "Your account balance of $30.00 is less than the transfer amount of $50.00.",
  "instance": "/accounts/12345/transfers/98765",
  "balance": 3000,
  "requested": 5000
}
```

The fields have precise roles. **`type`** is a URI identifying the *category* of problem — the stable, machine-readable key clients should branch on (and ideally a link to human docs). **`title`** is a short human summary of the type. **`status`** mirrors the HTTP status code. **`detail`** is a human-readable explanation *specific to this occurrence*. **`instance`** identifies the specific request that failed. Crucially, you can **extend** the object with domain-specific fields (`balance`, `requested`) to give clients actionable data.

Good error design goes beyond the format. Return the **right status code** (validation → 422 or 400, not 500). Give a **stable error code** clients can switch on programmatically, rather than forcing them to string-match human messages. **Never leak internals** — no stack traces, SQL, or internal hostnames in a public error (they are security disclosures). Always include a **correlation/request ID** so a developer can hand you "request `abc-123` failed" and you can find it in your logs instantly. And be consistent: the *same* problem should always produce the *same* shape and code across every endpoint.

<details>
<summary>📖 In plain terms</summary>

Every error your API returns should look the same and be easy for code to understand. RFC 7807 defines a standard JSON error body with a `type` (a stable ID for the kind of error), a `title` and `detail` (human-readable), the status code, and room for extra fields like the actual account balance. Rules of thumb: use the correct status code, give a stable machine-readable error code (not just a message), never leak stack traces, and always include a request ID so you can trace what went wrong.

</details>

<details>
<summary>💻 Java (Spring): RFC 7807 with ProblemDetail</summary>

```java
// Spring 6 / Boot 3 has built-in ProblemDetail support
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handleFunds(InsufficientFundsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("https://api.example.com/problems/insufficient-funds"));
        pd.setTitle("Insufficient funds");
        pd.setProperty("balance", ex.getBalance());
        pd.setProperty("requested", ex.getRequested());
        pd.setProperty("requestId", MDC.get("requestId"));  // correlation id
        return pd;   // serialized as application/problem+json
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
            .map(f -> Map.of("field", f.getField(), "message", f.getDefaultMessage()))
            .toList());
        return pd;
    }
}
```

</details>

---

# Part IV — Security, Performance & Protocol Choice

## 18. 🔒 Security: AuthN, AuthZ, Rate Limiting & CORS

Security in REST rests on distinguishing two concepts that beginners conflate. **Authentication (AuthN)** answers *"who are you?"* — verifying identity. **Authorization (AuthZ)** answers *"what are you allowed to do?"* — enforcing permissions. A request can be authenticated (we know it's Ada) but unauthorized (Ada can't delete other users). This is exactly why HTTP has two different codes: **`401 Unauthorized`** means "authenticate first" (a misnomer — it's really about authentication), while **`403 Forbidden`** means "we know who you are, and you may not do this."

### The three common credential mechanisms

**API keys** are the simplest: a long random string identifying the *caller* (usually an application, not a user), sent in a header (`Authorization: Bearer sk_live_...` or `X-API-Key`). They are easy to issue and revoke, ideal for server-to-server and public developer APIs (Stripe, Google Maps, OpenAI all use them). Their weakness: a leaked key grants full access until revoked, and they carry no built-in expiry or fine-grained scope unless you add it.

**OAuth 2.0** is the framework for **delegated authorization** — letting a third party act on a user's behalf *without* handing over the password. When you "Sign in with Google," OAuth is what lets the app get a scoped access token instead of your Google password. The core flow (Authorization Code with PKCE, the modern default) issues short-lived **access tokens** and longer-lived **refresh tokens**, each scoped to specific permissions.

<details>
<summary>🔓 Example: the OAuth2 Authorization Code flow ("Sign in with Google")</summary>

OAuth2 lets a third-party app act on your behalf *without ever seeing your password*. Concretely, when a photo-printing app wants to read your Google Photos:

```mermaid
sequenceDiagram
    participant U as User
    participant App as PrintApp (client)
    participant Auth as Google Auth Server
    participant API as Google Photos API

    U->>App: "Import my Google Photos"
    App->>Auth: Redirect to Google consent<br/>(client_id, scope=photos.readonly)
    U->>Auth: Log in + click "Allow"
    Auth-->>App: Redirect back with a one-time auth CODE
    App->>Auth: Exchange CODE (+ client_secret) for tokens
    Auth-->>App: access_token (short-lived) + refresh_token
    App->>API: GET /photos  Authorization: Bearer access_token
    API-->>App: Your photos ✅
```

The key ideas: the user authenticates with **Google, not the app**, so the app never touches the password. The app receives a short-lived, **scoped** `access_token` (here limited to `photos.readonly` — it *cannot* delete your photos or read your email), plus a `refresh_token` to get new access tokens when they expire. The exchange of the one-time code for tokens happens server-to-server using a `client_secret`, and the modern public/mobile-client variant adds **PKCE** to prevent code interception. This is exactly the flow behind every "Sign in with Google/GitHub/Apple" button, and the `access_token` it issues is very often a JWT like the one below.

</details>

<br/>

**JWT (JSON Web Token)** is a *token format* often used as the access token in OAuth. A JWT is a signed, self-contained token with three parts — header, payload (claims like `sub`, `exp`, `scope`), and signature. Its superpower aligns perfectly with REST's statelessness: because the token is **signed and self-contained**, any server can verify it with the public key *without a database lookup or shared session store*. The tradeoff is revocation — a stateless JWT is valid until it expires, so you can't easily "log someone out" server-side without adding a denylist (which reintroduces state). The standard mitigation is short-lived access tokens (minutes) plus refresh tokens.

<details>
<summary>🔑 Example: anatomy of a JWT</summary>

A JWT is just three Base64URL-encoded parts joined by dots: **`header.payload.signature`**. It looks like one opaque string on the wire:

```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MiIsInNjb3BlIjoi
dXNlcnM6cmVhZCIsImV4cCI6MTcyMDAwMDAwMH0.SflKxwRJSMeKKF2QT4fwpMeJf...
   └──── header ────┘ └──────── payload (claims) ────────┘ └─ signature ─┘
```

Decoded, the first two parts are plain JSON:

```json
// Header — how the token is signed
{ "alg": "RS256", "typ": "JWT" }

// Payload — the "claims" (facts about the caller)
{
  "sub": "42",                 // subject: the user id
  "scope": "users:read",       // what they're allowed to do (drives AuthZ)
  "iss": "https://auth.example.com",
  "iat": 1719996400,           // issued-at
  "exp": 1720000000            // expiry (short-lived: minutes)
}
```

The **signature** is the security: the auth server signs `header.payload` with its private key, and any API server verifies it with the matching public key. If someone tampers with a single character of the payload (say, bumps `scope` to `users:delete`), the signature no longer matches and the token is rejected. This is why a JWT fits REST's statelessness perfectly — **any server can verify identity and permissions from the token alone, with no database or session lookup.** The tradeoff: the token is valid until `exp`, so you can't instantly revoke it — hence short lifetimes plus refresh tokens. Note the payload is only *encoded*, not *encrypted*: never put secrets in it.

</details>

<details>
<summary>🎨 Diagram: how AuthN and AuthZ gate a request</summary>

```mermaid
flowchart TB
    subgraph AuthN["Authentication — who are you?"]
        A1[API Key] 
        A2[OAuth2 Access Token]
        A3[JWT]
    end
    subgraph AuthZ["Authorization — what may you do?"]
        Z1[Scopes / Roles / Claims]
    end
    Req[Incoming Request] --> AuthN --> AuthZ --> Allow{Allowed?}
    Allow -->|yes| Svc[Service logic]
    Allow -->|no| F["403 Forbidden"]
    AuthN -->|missing/invalid| U["401 Unauthorized"]
```

</details>

### Rate limiting

Rate limiting protects an API from abuse, runaway clients, and cost blowouts by capping how many requests a caller may make in a window. When a client exceeds its budget, the API returns **`429 Too Many Requests`** with a **`Retry-After`** header (and often `X-RateLimit-Remaining` / `X-RateLimit-Reset`) telling the client when to try again. The *algorithms* behind the cap — token bucket, sliding window — are a staff-level topic covered in [Rate-Limiting Algorithms](#24--rate-limiting-algorithms--where-they-live).

### CORS

**CORS (Cross-Origin Resource Sharing)** is a *browser* security mechanism, and it confuses a lot of engineers because it fails in ways that look like server bugs. Browsers enforce the **same-origin policy**: JavaScript on `https://app.example.com` cannot, by default, read responses from `https://api.other.com`. CORS is the server's way of *opting in* to cross-origin access via response headers like `Access-Control-Allow-Origin: https://app.example.com`. For "non-simple" requests (custom headers, `PUT`/`DELETE`, JSON bodies), the browser first sends a **preflight `OPTIONS`** request asking "am I allowed to make this call?", and only proceeds if the server's CORS headers approve. Two things to internalize: CORS is enforced by the *browser*, not the server (curl and mobile apps ignore it entirely), and it is *not* an authentication mechanism — it only governs which web origins may read your responses.

<details>
<summary>📖 In plain terms</summary>

Authentication is "who are you" (401 if you can't prove it); authorization is "are you allowed" (403 if not). Three ways to prove identity: an API key (a long secret string, great for server-to-server), OAuth2 (lets an app act on your behalf without your password — that's "Sign in with Google"), and JWT (a signed token any server can check without a database lookup, which fits REST's statelessness). Rate limiting caps how often you can call and returns 429 when you're over. CORS is just the browser rule that decides whether a webpage on one domain may read data from another.

</details>

<details>
<summary>💻 Java (Spring Security): JWT resource server + method authz</summary>

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http
          .authorizeHttpRequests(auth -> auth
              .requestMatchers("/v1/public/**").permitAll()
              .anyRequest().authenticated())
          .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults())) // verifies JWT signature
          .cors(Customizer.withDefaults())
          .csrf(csrf -> csrf.disable()); // stateless API, no cookies -> CSRF N/A
        return http.build();
    }
}

@RestController
class AdminController {
    @DeleteMapping("/v1/users/{id}")
    @PreAuthorize("hasAuthority('SCOPE_users:delete')")  // AuthZ: scope check -> 403 if missing
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

</details>

---

## 19. ⚡ Performance: Compression, CDN, Keep-Alive, HTTP/2 & HTTP/3

REST performance is won at several layers, from the bytes on the wire to the connections underneath. A staff engineer should be able to reason about all of them.

**Compression.** Text payloads (JSON, HTML) compress dramatically. The client advertises `Accept-Encoding: gzip, br`, the server compresses and responds with `Content-Encoding: gzip` (or Brotli, which typically beats gzip on text). Compression can cut a JSON response by 70–90%, trading a little CPU for a large bandwidth and latency win. (One caveat: compressing responses that reflect secret data alongside attacker-controlled input can enable BREACH-style attacks — a niche but real concern for sensitive endpoints.)

**Caching & CDN.** We covered HTTP caching in [Caching](#16--caching-etags-cache-control--conditional-requests); the *scaled-up* form is a **CDN** (CloudFront, Cloudflare, Fastly). A CDN is a globally distributed cache that sits close to users. For cacheable `GET`s, the CDN serves the response from an edge location physically near the user, cutting latency from hundreds of milliseconds to single digits and shielding your origin from traffic. Respecting `Cache-Control` correctly is what makes your origin CDN-friendly.

**Connection reuse — Keep-Alive.** Opening a TCP connection (and a TLS handshake on top) costs round trips. **HTTP Keep-Alive** (`Connection: keep-alive`, the default in HTTP/1.1) reuses one connection for many requests instead of opening a fresh one each time, eliminating repeated handshakes. Connection *pooling* on the client extends this idea.

**HTTP/2.** The big leap. Where HTTP/1.1 forces requests on a connection to be answered roughly in order (causing **head-of-line blocking** — one slow response stalls the rest), HTTP/2 introduces **multiplexing**: many concurrent request/response streams over a *single* TCP connection, in any order. It also adds **header compression (HPACK)** and binary framing. The result is far better use of a single connection, especially for pages/apps that make many small requests.

**HTTP/3.** HTTP/2 still runs on TCP, so a single lost packet stalls *all* streams (TCP-level head-of-line blocking). **HTTP/3 replaces TCP with QUIC, a protocol built on UDP.** QUIC gives each stream independent delivery (a lost packet only stalls its own stream), combines the transport and TLS handshakes into fewer round trips (often 0-RTT on reconnect), and — crucially for mobile — supports **connection migration**, so switching from Wi-Fi to cellular doesn't drop the connection. This is why it matters for the mobile-heavy clients in ride-sharing and social apps.

| Feature | HTTP/1.1 | HTTP/2 | HTTP/3 |
|---------|----------|--------|--------|
| Transport | TCP | TCP | **QUIC (UDP)** |
| Multiplexing | ❌ (HOL blocking) | ✅ (per stream, but TCP-level HOL) | ✅ (no transport HOL) |
| Header compression | ❌ | ✅ HPACK | ✅ QPACK |
| Handshake round trips | TCP + TLS separate | TCP + TLS separate | Combined, often 0-RTT |
| Connection migration | ❌ | ❌ | ✅ (Wi-Fi ↔ cellular) |

<details>
<summary>📖 In plain terms</summary>

Speeding up a REST API happens in layers. Compress text responses (gzip/Brotli) to shrink them 70–90%. Cache aggressively and put a CDN near users so common reads never travel far. Reuse TCP connections (Keep-Alive) so you're not re-shaking hands every request. HTTP/2 lets many requests share one connection at once instead of waiting in line. HTTP/3 goes further by running on QUIC/UDP, so one lost packet doesn't stall everything and switching from Wi-Fi to cellular doesn't drop your connection — a big deal on phones.

</details>

---

## 20. 🎨 REST vs RPC vs GraphQL vs gRPC

"Should this be REST?" is one of the most common senior design questions, and the honest answer is *"it depends on who the client is and what the traffic looks like."* Here are the four dominant styles and where each wins.

**REST** models resources and uses HTTP verbs. Its strengths are ubiquity, human-readability, excellent caching (it rides HTTP's cache layer natively), and a massive tooling ecosystem. Its weaknesses are **over-fetching** (you get the whole resource even if you need two fields) and **under-fetching / N+1** (you must call several endpoints to assemble a screen). It is the default for **public APIs and resource-oriented CRUD**.

**RPC (Remote Procedure Call)** — including JSON-RPC — models *actions/functions* rather than resources: `POST /calculateShippingCost`. It is simple and natural when your API is fundamentally a set of operations rather than resources, but it loses REST's uniform interface and caching. Plain RPC is less common today; its spiritual successor for internal services is gRPC.

**GraphQL** (Facebook) exposes a single endpoint and a **query language** where the *client* specifies exactly which fields it wants across which related objects, in one request. This directly cures over- and under-fetching, which is why it shines for **complex, nested data feeding rich UIs with many client variants** (a mobile app and a web app fetching different shapes from the same graph). Its costs: HTTP caching no longer works out of the box (everything is a `POST` to `/graphql`), you must guard against expensive/deeply-nested queries (query cost analysis), and server complexity rises.

**gRPC** (Google) uses **Protocol Buffers** (a compact binary format) over **HTTP/2**, with strongly-typed contracts defined in `.proto` files and code generation for many languages. It is fast, compact, supports **streaming** (client, server, and bidirectional), and is the go-to for **internal microservice-to-microservice** communication where performance and strict contracts matter. Its weakness: it is not human-readable, browser support requires a proxy (gRPC-Web), and it is awkward as a public-facing API.

```mermaid
flowchart TB
    Q{What are you building?} 
    Q -->|Public API,<br/>resource CRUD,<br/>need caching| REST[REST]
    Q -->|Rich UI, nested data,<br/>many client shapes,<br/>avoid over-fetch| GQL[GraphQL]
    Q -->|Internal microservices,<br/>low latency, streaming,<br/>strict contracts| GRPC[gRPC]
    Q -->|Simple action-oriented<br/>internal calls| RPC[RPC / JSON-RPC]
```

| | REST | RPC | GraphQL | gRPC |
|---|------|-----|---------|------|
| **Model** | Resources + verbs | Functions | Query graph | Functions (typed) |
| **Payload** | JSON (text) | JSON (text) | JSON (text) | Protobuf (binary) |
| **Transport** | HTTP/1.1+ | HTTP | HTTP (usually POST) | HTTP/2 |
| **Caching** | ✅ Native (HTTP) | ⚠️ Manual | ❌ Hard | ❌ Manual |
| **Over/under-fetch** | ❌ Prone | ⚠️ | ✅ Solved | ⚠️ |
| **Streaming** | ⚠️ (SSE/WS) | ❌ | ⚠️ (subscriptions) | ✅ First-class |
| **Human-readable** | ✅ | ✅ | ✅ | ❌ |
| **Best for** | Public/CRUD APIs | Simple internal ops | Rich, varied clients | Internal microservices |

**The staff-level synthesis:** use **REST for public APIs** (ubiquity, caching, developer familiarity), **gRPC for internal service-to-service** (performance, typed contracts, streaming), and **GraphQL when a single client needs to compose varied, nested data** from many sources and over-fetching is a real problem. These are not mutually exclusive — a mature company often runs REST at the public edge, GraphQL as a BFF aggregation layer, and gRPC between backend services. The wrong answer in an interview is dogma; the right answer is matching the style to the client and traffic.

<details>
<summary>📖 In plain terms</summary>

Four ways for programs to talk. REST: everything is a resource with a URL, great for public APIs and caching, but you sometimes fetch too much or too little. RPC: you call functions like `calculateCost` — simple but loses REST's niceties. GraphQL: the client asks for exactly the fields it wants in one request — perfect for rich apps with different screens, but caching gets hard. gRPC: fast binary format over HTTP/2, ideal *between* your backend services. Rule of thumb: REST outward to the public, gRPC between internal services, GraphQL when one screen needs data from many places.

</details>

---

## 21. 📚 API Documentation & Contract Testing

An API that isn't documented well is, from a consumer's perspective, barely an API at all. The industry standard is the **OpenAPI Specification** (formerly Swagger) — a machine-readable (YAML/JSON) description of every endpoint, its parameters, request/response schemas, auth, and error shapes. Because it is machine-readable, OpenAPI unlocks a whole toolchain: **Swagger UI** renders interactive, try-it-in-the-browser docs; **client SDKs** can be code-generated in a dozen languages; server stubs, mock servers, and request validators can all be produced from the same spec.

There are two philosophies for producing it. **Code-first** generates the spec from annotations in your implementation (e.g. springdoc-openapi scanning Spring controllers) — convenient, but the spec follows the code. **Design-first** writes the OpenAPI spec *before* any code, treating it as the contract that both server and clients build against — better for parallel team development and for keeping the API a deliberate design artifact rather than an accident of implementation.

A good spec pins down far more than URLs: for each endpoint it declares the exact **parameters** (and where they live — path, query, header), the **request body schema**, every possible **response status** with its **body schema**, the **auth** required, and reusable **component schemas** so types are defined once and referenced everywhere. Here is a fuller excerpt for retrieving an order, including the schema definitions the responses point at, correlated with the real request and response it describes:

<details>
<summary>📄 Example: OpenAPI spec, correlated request & response</summary>

```yaml
# openapi.yaml (excerpt) — design-first contract
paths:
  /v1/orders/{orderId}:
    get:
      summary: Retrieve an order
      operationId: getOrder
      security:
        - bearerAuth: []                 # requires a valid OAuth2/JWT bearer token
      parameters:
        - name: orderId
          in: path
          required: true
          description: Unique order identifier
          schema: { type: string, example: "ord_990" }
      responses:
        '200':
          description: The order
          headers:
            ETag:
              description: Version for optimistic concurrency
              schema: { type: string }
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Order' }
        '404':
          description: No order with that id
          content:
            application/problem+json:
              schema: { $ref: '#/components/schemas/Problem' }

components:
  securitySchemes:
    bearerAuth: { type: http, scheme: bearer, bearerFormat: JWT }
  schemas:
    Order:
      type: object
      required: [id, status, total, currency, items]
      properties:
        id:       { type: string, example: "ord_990" }
        status:   { type: string, enum: [CREATED, PAID, SHIPPED, DELIVERED], example: "PAID" }
        total:    { type: integer, description: "Amount in minor units (cents)", example: 4999 }
        currency: { type: string, example: "USD" }
        items:
          type: array
          items:
            type: object
            properties:
              sku: { type: string, example: "BOOK-42" }
              qty: { type: integer, example: 1 }
    Problem:                              # RFC 7807 error shape, reused everywhere
      type: object
      properties:
        type:   { type: string }
        title:  { type: string }
        status: { type: integer }
        detail: { type: string }
```

**Correlating the spec with a real call.** The whole value of the spec is that it *exactly* predicts the wire traffic. Here is the actual HTTP request and response that the definition above describes — read them side by side with the YAML and every field lines up (the path parameter, the bearer auth, the `ETag` header, the `Order` schema, and the enum `status`):

```http
GET /v1/orders/ord_990 HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJhbGciOiJSUzI1Ni...        # ← security: bearerAuth
Accept: application/json
```

```http
HTTP/1.1 200 OK                                     # ← responses: '200'
Content-Type: application/json
ETag: "v7"                                          # ← declared response header
Cache-Control: private, max-age=30

{
  "id": "ord_990",                                  # ┐
  "status": "PAID",                                 # │ matches
  "total": 4999,                                    # │ Order
  "currency": "USD",                                # │ schema
  "items": [ { "sku": "BOOK-42", "qty": 1 } ]       # ┘
}
```

And the error path, exactly as the `'404'` branch promises — an RFC 7807 `Problem` body:

```http
GET /v1/orders/ord_DOES_NOT_EXIST HTTP/1.1
Authorization: Bearer eyJhbGciOiJSUzI1Ni...
```

```http
HTTP/1.1 404 Not Found                              # ← responses: '404'
Content-Type: application/problem+json

{
  "type": "https://api.example.com/problems/order-not-found",
  "title": "Order not found",
  "status": 404,
  "detail": "No order exists with id ord_DOES_NOT_EXIST."
}
```

</details>

Because the spec and the real traffic are guaranteed to match, tooling can generate a typed client, a mock server that returns exactly this shape, and request/response validators that reject any drift — which is precisely what contract testing enforces automatically.

**Contract testing** closes the loop. In a microservices world, the danger is *drift*: a provider changes its response shape and silently breaks consumers. Contract testing (e.g. **Pact**, or **Spring Cloud Contract**) verifies — in CI, on both sides — that the provider still honors exactly what consumers depend on. In **consumer-driven contract testing**, each consumer publishes the subset of the API it actually uses, and the provider's build fails if it would break any of them. This catches integration breakages *before* deploy, without the cost and flakiness of full end-to-end environments, and it is the mature answer to "how do you stop microservices from breaking each other."

<details>
<summary>📖 In plain terms</summary>

OpenAPI (Swagger) is a standard file that describes your whole API — every endpoint, input, output, and error — in a machine-readable way. From it you get interactive docs, auto-generated client libraries, and mock servers for free. You can write it before coding (design-first, good for teams working in parallel) or generate it from code. Contract testing tools like Pact then check, automatically in CI, that a service hasn't changed its responses in a way that would break the apps depending on it — catching breakage before it ships.

</details>

---

# Part V — Staff-Level Design Layer

## 22. 🎓 Backward Compatibility & Deprecation at Scale

At FAANG scale the hardest API problem is not designing v1 — it is evolving to v2 without breaking millions of clients you do not control. The guiding principle is **additive, backward-compatible evolution**: prefer changes that old clients can ignore. Adding an optional field, a new endpoint, or a new enum value your clients tolerate — all safe. Removing a field, tightening validation, changing a type, or repurposing a field — all breaking.

When breakage is unavoidable, you run a **deprecation lifecycle** rather than a hard cutover: announce the deprecation, mark the old version/field (the HTTP `Deprecation` and `Sunset` headers exist for exactly this, signaling to clients when an endpoint will disappear), instrument to *measure who is still calling the deprecated path*, reach out to the heavy users, and only remove after usage drops below a threshold and the sunset date passes. You cannot delete what you cannot measure, so **per-client usage analytics on deprecated endpoints are a prerequisite**, not an afterthought.

The subtle traps interviewers love: an enum is a compatibility hazard — adding a new value can break clients that exhaustively switch on it, so document that clients must tolerate unknown values. Making an optional field required is breaking even though it "feels" additive. And **defaults must never change silently**, because a client relying on the old default will behave differently overnight. The gold-standard implementation remains Stripe's versioning: pin each integration to its original version and run internal transformation layers so new code serves old shapes — the API evolves freely while every existing client sees a frozen contract.

---

## 23. 🎓 Idempotency Keys in Distributed Systems

We introduced idempotency keys earlier; at staff level the interesting parts are the *edge cases*. The core is a server-side store mapping `idempotency_key → stored_response`, but making it correct under concurrency and failure requires care.

**Concurrent retries (the race).** A client times out and fires a retry *while the original is still running*. Now two requests with the same key hit the server simultaneously. A naive "check-then-act" has a window where both see "not seen before" and both execute — a double charge. The fix is to make the key insert **atomic**: insert the key into a unique-constrained table (or `SET NX` in Redis) *before* doing the work. The first request wins the insert and proceeds; the second gets a uniqueness violation and must either wait for the first to finish and return its result, or respond `409 Conflict` telling the client "a request with this key is in progress."

```mermaid
sequenceDiagram
    participant R1 as Request 1 (key K)
    participant R2 as Request 2 (key K, retry)
    participant DB as Idempotency Store (unique on K)
    R1->>DB: INSERT K (status=IN_PROGRESS)
    R2->>DB: INSERT K (status=IN_PROGRESS)
    DB-->>R1: OK (won the row)
    DB-->>R2: unique violation (rejected)
    R1->>R1: Execute, store result
    R2->>DB: read K, wait or return in-progress (409)
    R1->>DB: UPDATE K (status=DONE, response)
    Note over R2: retry later, gets stored response
```

**Key scope and request binding.** An idempotency key must be scoped correctly — usually per endpoint and per authenticated account — so two unrelated operations can't collide. Robust implementations (Stripe) also store a **fingerprint of the request payload** with the key and return an error if the *same key* arrives with a *different body*, catching client bugs where a key is accidentally reused for a different operation.

**Storage & TTL.** Keys can't live forever. A common design keeps them for 24 hours to a few days — long enough to cover any realistic retry window — then expires them. **Where** you store them matters: Redis is fast but you must reason about its durability, while a database row is durable but slower. **Who generates the key** also matters: the *client* must generate it (a UUID at the moment of user action), because a server-generated key can't survive a lost response — the client wouldn't know the key to retry with.

### Different approaches to achieving idempotency

An idempotency key is the most general mechanism, but it is not the only one, and a strong answer names the full toolbox and picks the cheapest tool that fits. There are five approaches worth knowing, ordered from "free" to "most machinery."

**1. Rely on naturally idempotent methods.** The cheapest idempotency is the kind HTTP gives you for free. `GET`, `PUT`, and `DELETE` are defined as idempotent: `PUT /users/42 {name: "Ada"}` sets the resource to an absolute state, so running it once or five times leaves the same result, and `DELETE /users/42` leaves the user gone whether called once or twice (the second call returns `404` or `204`, but the *state* is identical). The design lesson is to prefer full-replacement `PUT` over relative mutations when you can, because a relative operation like "add 10 to the balance" is inherently non-idempotent and forces you to add machinery. The method that is never idempotent is `POST` ("create a new thing"), which is exactly why `POST` is the case that needs the remaining approaches.

**2. Idempotency key (the general mechanism).** For non-idempotent `POST` operations — charging a card, placing an order, sending a message — the client sends a unique `Idempotency-Key` header (a UUID it generates once per user action) and the server stores `key → response`. This is the Stripe model covered above: it works for *any* operation, which is why it is the default answer for payments, and it is the approach the concurrency and TTL discussion applies to.

**3. Business-level deduplication via a natural unique key.** Sometimes the domain already contains a value that must be unique, so you can enforce idempotency with a database unique constraint instead of a separate key store. A signup endpoint can put a unique index on `email`; a "create invoice for order X" endpoint can make `order_id` unique on the invoices table. A duplicate request then fails the insert with a uniqueness violation, and you translate that into returning the *existing* resource (`200`) rather than creating a second one. This is cheaper than a key store when a natural unique field exists, but it only dedupes on that field's meaning, not on "the same user action."

**4. Optimistic concurrency (conditional writes).** When the risk is not "the same request twice" but "two *different* updates racing," idempotency-of-effect comes from `ETag` + `If-Match`, covered in [Caching](#16--caching-etags-cache-control--conditional-requests). The client sends `If-Match: "v7"`; the server applies the write only if the version still matches and otherwise returns `412 Precondition Failed`. A blindly retried update can't silently clobber an intervening change, so the operation is safe to repeat.

**5. State-machine / transition guards.** For workflow resources, idempotency can be enforced by the allowed state transitions themselves. An order that is already `SHIPPED` simply ignores a second `POST /orders/{id}/ship` (returning the current state), because the transition `SHIPPED → SHIPPED` is a no-op. This makes an operation naturally safe to retry without any key at all, as long as every transition checks the current state before acting.

<details>
<summary>📊 Which approach when — a comparison</summary>

| Approach | Best for | Mechanism | Cost |
|---|---|---|---|
| Idempotent methods (`PUT`/`DELETE`) | Absolute state changes | HTTP semantics; full replacement | Free — just design choice |
| Idempotency key | Any `POST` (payments, orders, messages) | Client UUID + server `key → response` store | Key store + TTL + concurrency handling |
| Natural unique key | Creates with a naturally-unique field (email, order_id) | DB unique constraint, return existing on conflict | One index; no separate store |
| Optimistic concurrency | Racing *updates* to the same resource | `ETag` + `If-Match` → `412` on mismatch | Version tracking per resource |
| State-machine guard | Workflow transitions (ship, cancel, approve) | Reject/no-op if already in target state | State checks in domain logic |

</details>

### Use cases and concrete examples

The approaches map onto recognizable real systems. In **payments** (Stripe, PayPal), the client generates an idempotency key per "Pay" click, so a network timeout and retry can never double-charge the customer — the canonical use case, and approach 2. In **e-commerce checkout**, "Place Order" carries a key so a double-click or a mobile app retry produces one order, not two; the same system often *also* uses approach 3, a unique constraint on `(cart_id, checkout_attempt)`, as a second line of defense. In **messaging and notifications** (SQS, Kafka consumers, webhook receivers), delivery is "at-least-once," so consumers must dedupe by a `message_id` they've already processed — approach 3 applied to event streams. In **infrastructure and provisioning** (Terraform, Kubernetes), the entire model is *declarative*: you `PUT` a desired state and the system converges to it, so re-applying the same manifest is a no-op — approach 1 at the level of whole systems. And in **document or profile edits**, concurrent saves are guarded by `ETag`/`If-Match` so a retried or stale save returns `412` instead of overwriting a newer version — approach 4.

<details>
<summary>💻 Java: three idempotency approaches side by side</summary>

```java
// Approach 2 — Idempotency key for a POST (payments / orders)
@PostMapping("/v1/charges")
public ResponseEntity<Charge> charge(
        @RequestHeader("Idempotency-Key") String key,
        @RequestBody ChargeRequest req) {
    // Atomic claim: first request wins the row, retries collide.
    Optional<StoredResponse> existing = idempotencyStore.putIfAbsent(key, req.fingerprint());
    if (existing.isPresent()) {
        return existing.get().toResponse();     // replay the stored result, no re-charge
    }
    Charge c = paymentGateway.execute(req);      // do the real work once
    idempotencyStore.complete(key, c);           // persist result for future retries
    return ResponseEntity.status(HttpStatus.CREATED).body(c);
}

// Approach 3 — Natural unique key (signup dedupe, no key store)
@PostMapping("/v1/users")
public ResponseEntity<User> signup(@RequestBody SignupRequest req) {
    try {
        User u = users.save(new User(req.email()));          // UNIQUE index on email
        return ResponseEntity.status(HttpStatus.CREATED).body(u);
    } catch (DataIntegrityViolationException dup) {
        User existing = users.findByEmail(req.email());       // idempotent: return what exists
        return ResponseEntity.ok(existing);
    }
}

// Approach 4 — Optimistic concurrency for a racing update
@PutMapping("/v1/orders/{id}")
public ResponseEntity<Order> update(
        @PathVariable String id,
        @RequestHeader("If-Match") String version,
        @RequestBody OrderPatch patch) {
    Order o = orders.findById(id).orElseThrow();
    if (!o.version().equals(version)) {
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build(); // 412
    }
    return ResponseEntity.ok(orders.applyAndBump(o, patch));  // safe to retry
}
```

</details>

<details>
<summary>📖 In plain terms</summary>

Idempotency means doing the same operation more than once has the same effect as doing it once — which matters because networks drop responses and clients retry. There is a spectrum of ways to get it. The cheapest is to use HTTP methods that are already idempotent: `PUT` (set to an exact state) and `DELETE`. When you must use `POST` (like charging a card), the client sends a unique idempotency key and the server remembers the result so a retry replays it instead of charging twice. If your data already has something unique (an email, an order id), a database unique constraint dedupes for free. If two *different* edits might race, `ETag`/`If-Match` lets the server reject the stale one with `412`. And for workflows, you can just ignore an action if the resource is already in the target state. The skill is picking the lightest of these that solves the actual problem.

</details>

Getting this right is what separates "I know idempotency keys exist" from "I've built a payment system that survives retries," which is precisely the depth interviewers probe on the [payments](#case-payments) and [e-commerce](#case-ecommerce) case studies.

---

## 24. 🎓 Rate-Limiting Algorithms & Where They Live

When an interviewer asks "how would you rate-limit this API," they want the algorithm *and* its placement in the stack. Four algorithms, from crude to refined:

**Fixed window** counts requests per calendar window (e.g. 100 per minute, resetting at each minute boundary). Simple, but suffers **boundary bursts**: a client can send 100 at 11:00:59 and another 100 at 11:01:00 — 200 requests in two seconds — because the counter resets.

**Sliding window log** stores a timestamp for every request and counts those within the trailing 60 seconds. Accurate, but memory-heavy at scale (one entry per request).

**Sliding window counter** approximates the log cheaply by weighting the previous window's count into the current one — smooths out the boundary-burst problem with far less memory. This is what many production limiters (Cloudflare has described this approach) actually use.

**Token bucket** is the most popular for APIs. A bucket holds up to *N* tokens and refills at a steady rate (say 10/second); each request consumes a token, and requests are rejected when the bucket is empty. Its virtue is that it **allows controlled bursts** (you can spend the whole bucket at once) while enforcing a long-run average rate — which matches real traffic better than a rigid window. Its cousin, **leaky bucket**, enforces a strictly smooth output rate (good for shaping traffic to a downstream that needs even load).

```mermaid
flowchart LR
    subgraph TB["Token Bucket"]
        direction TB
        Refill["Refill 10 tokens/sec"] --> Bucket[("Bucket<br/>capacity N")]
        Bucket -->|take 1| ReqA[Request] 
        Bucket -->|empty, so 429| Rej[Reject]
    end
```

**Where it lives** is the other half of the answer. Rate limiting usually sits at the **API gateway or a dedicated edge layer** (so bad traffic is rejected before reaching your services), backed by a **centralized, fast store like Redis** so the limit is enforced *globally* across all server instances — a per-instance in-memory counter would let a client multiply its limit by the number of servers. At the very edge, CDNs and WAFs (Cloudflare, AWS WAF) provide a coarse first line of defense against volumetric abuse before requests even reach your gateway. The full picture is a layered defense: CDN/WAF for gross abuse, gateway + Redis for per-client fairness, and sometimes per-endpoint limits inside services for expensive operations.

---

## 25. 🎓 API Gateway & Backend-for-Frontend Patterns

As a system grows from one service to dozens, you don't want every service re-implementing authentication, rate limiting, TLS termination, and logging — and you don't want clients to know the internal topology. The **API Gateway** is the single entry point that centralizes these cross-cutting concerns: TLS termination, authN/authZ, rate limiting, request routing, response caching, and observability all live in one layer in front of the services (Kong, AWS API Gateway, Apigee, Spring Cloud Gateway). The service mesh handles service-to-service concerns; the gateway handles north-south (client-to-system) traffic.

```mermaid
flowchart TB
    Web[Web App] --> BFFW[Web BFF]
    Mobile[Mobile App] --> BFFM[Mobile BFF]
    Partner[Partner] --> GW[API Gateway]
    BFFW --> GW
    BFFM --> GW
    GW --> S1[Orders svc]
    GW --> S2[Users svc]
    GW --> S3[Inventory svc]
    style GW fill:#e6f0ff
```

The **Backend-for-Frontend (BFF)** pattern goes one step further. Different clients have genuinely different needs: a mobile app wants small, aggregated payloads over few round trips (battery and bandwidth matter), while a web app or a partner integration wants richer data. Rather than bloating one generic API with every client's needs — or forcing the mobile app to make ten calls to build one screen — you build a **thin, client-specific aggregation layer per frontend**. The mobile BFF calls the orders, users, and inventory services and composes exactly the compact response the mobile screen needs; the web BFF composes a different shape. This keeps the downstream services clean and general while each client gets an API tailored to it. It is also, notably, one of the strongest places to introduce **GraphQL** — a BFF is a natural home for a query layer that composes many backend services into client-shaped responses.

---

# Part VI — FAANG Case Studies

## 26. 💡 6 FAANG-Favorite REST API Design Case Studies

These six problems appear again and again in interviews because each one forces a *different* set of REST design tradeoffs — not just CRUD. Banking forces security and consistency; e-commerce forces idempotency and concurrency; ride-sharing forces real-time state and geospatial queries; social feeds force pagination and fan-out; notifications force async design and delivery guarantees; payments force the whole distributed-systems toolkit at once. Work through all six and you have rehearsed nearly every REST design decision an interviewer can throw at you.

### The common REST operations — and how to choose method, URL, headers, and body

Before the case studies, it helps to have the standard operations in one place, because almost every REST API — regardless of domain — is assembled from the same small set of building blocks. An interviewer isn't testing whether you can invent new HTTP verbs; they're testing whether you reach for the *right* one and shape the URL, headers, and body correctly around it. The recurring operations are: list a collection, read one item, create, full-replace, partial-update, delete, and trigger an action that doesn't fit CRUD.

<details>
<summary>📋 REST operations cheat sheet — method, URL, body, and status</summary>

| Operation | Method + URL | Idempotent? | Request body | Typical success | Key headers |
|---|---|---|---|---|---|
| List collection | `GET /v1/orders?status=paid&limit=20&cursor=…` | Yes (safe) | none | `200 OK` | `Accept`, cache: `ETag`/`Cache-Control` |
| Read one | `GET /v1/orders/{id}` | Yes (safe) | none | `200 OK` (`404` if absent) | `ETag` out; `If-None-Match` in |
| Create | `POST /v1/orders` | No → add `Idempotency-Key` | full new representation | `201 Created` + `Location` | `Content-Type`, `Idempotency-Key` |
| Full replace | `PUT /v1/orders/{id}` | Yes | the entire resource | `200 OK` / `204 No Content` | `If-Match` for concurrency |
| Partial update | `PATCH /v1/orders/{id}` | Not inherently | only the changed fields | `200 OK` | `If-Match`, `Content-Type` |
| Delete | `DELETE /v1/orders/{id}` | Yes | none | `204 No Content` | `If-Match` (optional) |
| Async accept | `POST /v1/notifications` | No → `Idempotency-Key` | the job to enqueue | `202 Accepted` + `Location` | `Idempotency-Key` |
| Action (non-CRUD) | `POST /v1/orders/{id}/cancel` | model to be safe | action params (often none) | `200 OK` | `Idempotency-Key` if it moves money |

</details>

The decision procedure is quick once internalized. Choose the **method** by intent: reading is `GET`, creating a server-named resource is `POST`, setting a client-known resource to an exact state is `PUT`, changing a few fields is `PATCH`, removing is `DELETE`; if the operation is a verb that isn't CRUD (cancel, ship, confirm, refund), model it as a `POST` to an action sub-resource (`/orders/{id}/cancel`) rather than inventing a method. Shape the **URL** as plural nouns with the resource identity in the path (`/v1/accounts/{id}/transactions`) and everything that *filters or shapes a list* — pagination, sorting, search, field selection — in the **query string** (`?status=paid&sort=-created&limit=20`), never in the path. Put **identity and representation metadata in headers**: `Authorization` for who you are, `Content-Type`/`Accept` for the media type, `Idempotency-Key` for retry-safety, and `If-Match`/`If-None-Match` for concurrency and caching. Reserve the **body** for the resource representation on writes (and never on `GET`/`DELETE`). Finally, let the **status code** carry the outcome honestly: `201` with a `Location` for a create, `202` for accepted-but-not-done async work, `204` for a successful delete with no body, `400`/`422` for bad input, `409` for a state conflict, `412` for a failed precondition.

<details>
<summary>💻 One resource, all its operations — a worked example</summary>

Taking a single `orders` resource end to end shows how the same entity is manipulated through the standard verbs, with the right headers and codes on each:

```http
# CREATE — POST to the collection, retry-safe, returns Location
POST /v1/orders HTTP/1.1
Authorization: Bearer <token>
Idempotency-Key: 3d5a7c1e-8b2f-4a11-9c3d-77e0a1b2c3d4
Content-Type: application/json

{ "items": [{ "sku": "BOOK-42", "qty": 1 }], "payment_token": "tok_x" }

HTTP/1.1 201 Created
Location: /v1/orders/ord_990
ETag: "v1"
```

```http
# READ ONE — safe GET, cache-validated with If-None-Match
GET /v1/orders/ord_990 HTTP/1.1
If-None-Match: "v1"

HTTP/1.1 304 Not Modified          # nothing changed; no body resent
```

```http
# PARTIAL UPDATE — PATCH one field, guarded by the version
PATCH /v1/orders/ord_990 HTTP/1.1
If-Match: "v1"
Content-Type: application/json

{ "status": "PAID" }

HTTP/1.1 200 OK
ETag: "v2"
```

```http
# ACTION — non-CRUD verb modeled as a POST sub-resource
POST /v1/orders/ord_990/cancel HTTP/1.1
Idempotency-Key: 9e1c…

HTTP/1.1 200 OK
{ "id": "ord_990", "status": "CANCELLED" }
```

```http
# LIST — filtering, sorting, and pagination live in the query string
GET /v1/orders?status=paid&sort=-created_at&limit=20&cursor=eyJ0cyI6… HTTP/1.1

HTTP/1.1 200 OK
{ "data": [ /* 20 orders */ ], "pagination": { "next_cursor": "…", "has_more": true } }
```

</details>

With that shared vocabulary in place, each case study below shows how a specific domain maps its **entities onto these same REST components** — which nouns become resources, how they nest, which headers matter most, and where the standard operations need reinforcement (idempotency keys, ETags, async codes).

<a name="case-banking"></a>

<details>
<summary><b>🏦 Case Study 1 — Banking / Open Banking API Platform</b></summary>

### Problem statement

Design secure APIs for **accounts, balances, fund transfers, beneficiaries, transaction history, statements, and regulatory integrations**. The dominant forces here are **security, correctness/consistency, auditability, and regulatory compliance** (PSD2/Open Banking in the EU, similar regimes elsewhere). A wrong balance or a double transfer is not a bug — it is a financial and legal incident.

### Resource model

```http
GET    /v1/accounts                          # list the customer's accounts
GET    /v1/accounts/{accountId}              # account details
GET    /v1/accounts/{accountId}/balances     # current + available balance
GET    /v1/accounts/{accountId}/transactions # transaction history (paginated)
GET    /v1/accounts/{accountId}/statements/{statementId}   # PDF/JSON statement
GET    /v1/beneficiaries
POST   /v1/beneficiaries                      # add a payee
POST   /v1/payments                           # initiate a transfer
GET    /v1/payments/{paymentId}               # payment status
```

Note the modeling choices: **balances and transactions are sub-resources of an account** (they only exist in that context), while **payments are a top-level resource** because a payment has its own lifecycle, its own status, and is referenced independently — nesting it under an account would be awkward. This is the nested-vs-flat judgment applied deliberately.

### Mapping the domain onto REST components

The design work is translating banking *entities* into REST *components* — deciding what becomes a resource, what lives in the path vs the query string, which headers carry security and consistency, and what the body holds. For banking the mapping is dominated by security headers and by treating money movement as its own resource:

| Domain entity | Resource / URL | Method(s) | Path vs query | Key headers | Body |
|---|---|---|---|---|---|
| Account | `/v1/accounts/{accountId}` | `GET` | id in path | `Authorization: Bearer` (scope `accounts:read`) | none |
| Balance | `/v1/accounts/{accountId}/balances` | `GET` | nested sub-resource | strong-consistency read | none |
| Transaction history | `/v1/accounts/{id}/transactions?from=…&to=…&cursor=…` | `GET` | date filters + cursor in query | `ETag` for caching | none |
| Beneficiary (payee) | `/v1/beneficiaries`, `/v1/beneficiaries/{id}` | `GET`/`POST` | flat top-level | `Authorization` (scope `payments:write`) | payee details on create |
| Payment (transfer) | `/v1/payments`, `/v1/payments/{id}` | `POST`/`GET` | flat top-level (own lifecycle) | `Idempotency-Key` (mandatory) | from/to/amount/reference |
| Consent | `/v1/account-access-consents/{id}` | `POST`/`GET` | flat, HATEOAS-driven | `Authorization`; links in body | requested scopes |

The two entries that separate a senior answer: **transaction history filters (`from`, `to`, `cursor`) belong in the query string**, never the path, because they shape a list rather than identify a resource; and the **`Idempotency-Key` header is mandatory on `POST /payments`**, because that is the one entity where a retry must not duplicate the effect.

### Fund transfer — the critical path

A transfer is a `POST /v1/payments` and it must be **idempotent**, because a client that times out and retries must never move money twice:

```http
POST /v1/payments HTTP/1.1
Authorization: Bearer <oauth2-access-token>
Idempotency-Key: 8f14e45f-ceea-467d-9f3a-1c2b3d4e5f60
Content-Type: application/json

{
  "from_account": "acc_123",
  "to_beneficiary": "ben_789",
  "amount": { "value": 5000, "currency": "GBP" },
  "reference": "Invoice 4471"
}
```

```http
HTTP/1.1 201 Created
Location: /v1/payments/pay_abcdef
Content-Type: application/json

{ "id": "pay_abcdef", "status": "PENDING", "amount": {"value":5000,"currency":"GBP"} }
```

The transfer status then transitions `PENDING → PROCESSING → COMPLETED` (or `FAILED`/`RETURNED`), exposed via `GET /v1/payments/{id}`. Note the money amount is an **integer in minor units** (5000 pence, not 50.00) — floating-point currency is a classic correctness bug interviewers watch for.

### Reading transaction history — filtering and pagination in practice

Statements and history are the read-heavy side of the API, and they show every list-shaping component working together: date filters and pagination in the query string, a bearer scope in the header, and a cacheable, cursor-paginated response body.

```http
GET /v1/accounts/acc_123/transactions?from=2026-07-01&to=2026-07-31&limit=2&cursor=eyJpZCI6InR4bl81NSJ9 HTTP/1.1
Host: api.bank.example.com
Authorization: Bearer <oauth2-access-token>   # scope: accounts:read
Accept: application/json
```

```http
HTTP/1.1 200 OK
Content-Type: application/json
ETag: "txlist-9f2a"
Cache-Control: private, max-age=30

{
  "data": [
    { "id": "txn_57", "amount": { "value": -1299, "currency": "GBP" },
      "type": "DEBIT", "booked_at": "2026-07-19T10:04:00Z", "description": "Coffee Co" },
    { "id": "txn_56", "amount": { "value": 250000, "currency": "GBP" },
      "type": "CREDIT", "booked_at": "2026-07-18T08:00:00Z", "description": "Salary" }
  ],
  "pagination": { "next_cursor": "eyJpZCI6InR4bl81NiJ9", "has_more": true }
}
```

Every design element is deliberate: the account id is in the **path** (identity), the date window and cursor are in the **query string** (they filter a list), `Authorization` in the **header** carries the scoped token, and the **body** returns cursor pagination (not offset — a ledger grows constantly, so offset would drift). Debits are negative integers in minor units, keeping the arithmetic exact.

### Design tradeoffs interviewers probe

**Security is layered and non-negotiable.** TLS everywhere; **OAuth2** with strong customer authentication (SCA) and fine-grained **scopes** (`accounts:read`, `payments:write`); short-lived tokens; and often **HATEOAS**, because Open Banking specs genuinely use hypermedia links to drive multi-step consent + payment flows. Every state-changing call is written to an **immutable audit log** for regulators.

**Consistency over availability.** Unlike a social feed, a bank cannot serve a stale balance and shrug. Reads of balances favor strong consistency, and the transfer itself must be atomic — internally this is a transaction (or a saga with compensations across ledger services), but the *API* surfaces it as a single idempotent `POST` whose status you can poll.

**Regulatory & consent flows.** Open Banking adds a consent resource (`POST /v1/account-access-consents`) that the user authorizes before a third party may read accounts — modeled as its own resource with its own status, exactly the kind of HATEOAS-driven flow REST Level 3 was designed for.

**Real-world reference:** the UK **Open Banking Standard** and **Plaid**'s API are the canonical examples — resource-oriented, OAuth2-secured, consent-driven, and hypermedia-aware.

</details>

<a name="case-ecommerce"></a>

<details>
<summary><b>🛒 Case Study 2 — E-commerce Order & Inventory API</b></summary>

### Why it's loved

This is the go-to problem for testing **idempotency and concurrency** under REST. Orders involve money (retry-safety) and inventory involves shared mutable state (concurrent decrements) — both classic distributed-systems hazards.

### Mapping the domain onto REST components

The e-commerce entities map onto REST components with two components doing the heavy lifting: the `Idempotency-Key` header on order creation and the `If-Match`/`ETag` pair on inventory. Notice how the *same* entity (inventory) uses a query-string-free path for identity but relies entirely on headers for correctness.

| Domain entity | Resource / URL | Method(s) | Path vs query | Key headers | Body |
|---|---|---|---|---|---|
| Product catalog | `/v1/products?category=books&sort=-rating&limit=20` | `GET` | facets/sort/paging in query | `ETag`, `Cache-Control` | none |
| Cart | `/v1/carts/{cartId}`, `/v1/carts/{id}/items` | `GET`/`POST`/`PATCH`/`DELETE` | cart id in path; items nested | `Authorization` | line items on write |
| Order | `/v1/orders`, `/v1/orders/{id}` | `POST`/`GET`/`PATCH` | flat top-level | `Idempotency-Key` (create) | items + payment token |
| Order status transition | `/v1/orders/{id}` (`PATCH`) or `/orders/{id}/ship` | `PATCH`/`POST` | id in path | `If-Match` (optional) | `{ "status": "SHIPPED" }` |
| Inventory | `/v1/inventory/{sku}` | `GET`/`PATCH` | sku in path | `ETag` out, `If-Match` in | `{ "op": "decrement", "qty": 1 }` |

The senior distinctions: **filtering the catalog goes in the query string** while the sku identifies inventory in the path; **order creation carries an idempotency key** (money) whereas catalog reads carry cache validators; and **inventory mutation is a conditional `PATCH`** guarded by `If-Match`, not a blind write.

### Idempotent order creation

`POST /orders` must be retry-safe so a network timeout never creates two orders or two charges:

```http
POST /v1/orders HTTP/1.1
Idempotency-Key: 3d5a7c1e-...            # client-generated per checkout attempt
Content-Type: application/json

{ "items": [{"sku": "BOOK-42", "qty": 1}], "payment_token": "tok_x" }
```

```http
HTTP/1.1 201 Created
Location: /v1/orders/ord_990
ETag: "v1"
Content-Type: application/json

{ "id": "ord_990", "status": "CREATED", "total": 4999, "currency": "USD",
  "items": [ { "sku": "BOOK-42", "qty": 1 } ] }
```

The server stores the key → result, so the retry returns the *same* order (identical `201` and `ord_990`) rather than creating a new one (see [Idempotency Keys](#23--idempotency-keys-in-distributed-systems)).

### Optimistic concurrency on stock decrement

Two customers race to buy the last unit. You must not oversell. The REST-native answer is **optimistic concurrency with ETags**. The inventory resource carries a version:

```http
GET /v1/inventory/BOOK-42
→ 200 OK
  ETag: "v7"
  { "sku": "BOOK-42", "available": 1 }
```

Decrementing is a conditional write:

```http
PATCH /v1/inventory/BOOK-42
If-Match: "v7"
{ "op": "decrement", "qty": 1 }
```

The first request matches version `v7`, succeeds, and bumps the version to `v8`. The second customer's request still carries `If-Match: "v7"`, which no longer matches, so it fails:

```http
HTTP/1.1 412 Precondition Failed
Content-Type: application/problem+json

{ "type": "https://api.shop.example.com/problems/version-conflict",
  "title": "Inventory changed since you last read it",
  "status": 412, "detail": "Expected version v7 but current is v8. Re-read and retry." }
```

No oversell, no locks held across the network — the losing client simply re-reads the current `available` count and decides whether to retry. This "compare-and-swap over HTTP" is the textbook REST concurrency answer.

### Order state machine — PUT vs PATCH

An order moves through `CREATED → PAID → SHIPPED → DELIVERED`. You expose transitions with **`PATCH /orders/{id}`** carrying the target status, *not* `PUT`, because `PUT` means "replace the whole order" — you don't want the client resending the entire order (with items, addresses, totals) just to advance its status, and you certainly don't want them able to overwrite the total while changing status. `PATCH` with a validated transition (`{"status": "SHIPPED"}`) is both smaller and safer, and the server enforces legal transitions (rejecting `DELIVERED → CREATED` with `409`).

### Nested vs flat resources

`GET /orders/{id}/items` reads naturally for "items in this order." But `order-items` may also warrant a top-level `/order-items/{id}` if they're referenced independently (e.g. for returns or fulfillment). The rule: nest when the child only exists in the parent's context and you never address it globally; flatten when it has an independent lifecycle.

### Partial failures — saga at the API layer

The nightmare: **payment succeeds but inventory reservation fails.** Since you can't hold a distributed ACID transaction across payment and inventory services, you use a **saga** with compensating actions — and the API must surface this honestly. The order enters a `PAYMENT_CONFIRMED` state, an inventory reservation is attempted, and on failure a **compensating refund** is triggered, moving the order to `CANCELLED_REFUNDED`. The client sees an order whose status tells the true story rather than a lie.

### Async payment confirmation — webhooks vs polling

Payment processors confirm asynchronously. Two options: the client **polls** `GET /orders/{id}` until status settles (simple, but wasteful), or you register a **webhook** so the processor calls *you* when the charge settles (efficient, but requires you to handle out-of-order and duplicate deliveries idempotently). Mature systems offer both — webhooks as the primary path, polling as the fallback.

**Real-world reference:** **Shopify** and **Amazon**'s order APIs, and **Stripe**'s idempotency keys, embody exactly these patterns.

</details>

<a name="case-rideshare"></a>

<details>
<summary><b>🚗 Case Study 3 — Ride-Sharing API (Uber/Lyft-style)</b></summary>

### Why it's loved

It tests **real-time state, geospatial queries, and the limits of REST** — the point where a naive REST design breaks and you must justify reaching for something else.

### Mapping the domain onto REST components

Ride-sharing is the case where the mapping table also reveals what REST *shouldn't* own: the high-frequency location stream is deliberately *not* a REST resource. Note how geospatial parameters live in the query string while the ride's identity and lifecycle are pure REST.

| Domain entity | Resource / URL | Method(s) | Path vs query | Key headers | Body / transport |
|---|---|---|---|---|---|
| Nearby drivers | `/v1/drivers/nearby?lat=…&lng=…&radius=2000&vehicle=xl` | `GET` | all geo params in query | `Authorization` | none |
| Ride | `/v1/rides`, `/v1/rides/{id}` | `POST`/`GET`/`PATCH` | flat top-level | `Idempotency-Key` (request) | pickup/dropoff/type |
| Ride state transition | `/v1/rides/{id}` (`PATCH`) | `PATCH` | id in path | server-validated transition | `{ "status": "IN_PROGRESS" }` |
| Live driver location | **not REST** — WebSocket/SSE channel | push | n/a | persistent connection | streamed coordinates |
| Receipt / rating | `/v1/rides/{id}/receipt`, `/v1/rides/{id}/rating` | `GET`/`PUT` | nested sub-resource | `Authorization` | rating value |

The insight interviewers reward: **geospatial search parameters (`lat`, `lng`, `radius`) go in the query string** of a collection `GET`, the ride is a normal REST resource with an idempotent create, but **live location is intentionally excluded from the REST surface** because request/response is the wrong transport for a per-second push stream.

### Geospatial driver search

```http
GET /v1/drivers/nearby?lat=37.7749&lng=-122.4194&radius=2000&vehicle=xl
→ 200 OK
{ "data": [ {"driver_id":"d_1","eta_seconds":180,"lat":...,"lng":...}, ... ] }
```

Behind this is a **geospatial index** — a geohash, an S2/H3 cell scheme (Uber built H3 for exactly this), or Redis `GEORADIUS`. The REST surface is a simple filtered collection; the interesting engineering is the index that makes "who's within 2km" fast at city scale.

### The ride as a long-lived resource

A ride is a resource that transitions `REQUESTED → MATCHED → DRIVER_ARRIVING → IN_PROGRESS → COMPLETED` (or `CANCELLED`). You model the ride with `POST /v1/rides` to request, and **`PATCH /v1/rides/{id}`** to advance state (accept, start, complete), each transition validated server-side.

```http
POST /v1/rides HTTP/1.1
Authorization: Bearer <token>
Idempotency-Key: 7a2b…          # double-tap protection
Content-Type: application/json

{ "pickup": { "lat": 37.7749, "lng": -122.4194 },
  "dropoff": { "lat": 37.7849, "lng": -122.4094 },
  "vehicle_type": "xl" }
```

```http
HTTP/1.1 201 Created
Location: /v1/rides/ride_551
Content-Type: application/json

{ "id": "ride_551", "status": "REQUESTED", "vehicle_type": "xl",
  "estimated_fare": { "value": 1450, "currency": "USD" } }
```

The pickup and dropoff coordinates travel in the **body** (they are the ride's data, not a filter), the idempotency key in the **header**, and the response returns `201` with a `Location` pointing at the new ride whose `status` the client then tracks.

### Where REST breaks down — location updates

Here's the crux interviewers push toward. A driver's location changes every second, and the rider's app must see it move on the map. Modeling that as REST **polling** (`GET /rides/{id}/driver-location` every second) is wasteful — thousands of requests, most returning tiny changes, hammering your servers. So you justify a different tool:

- **Polling:** simplest, works everywhere, but high overhead and stale between polls. Fine for low-frequency status.
- **Webhooks:** great for server-to-server events (driver accepted), useless for pushing to a mobile client behind NAT.
- **WebSockets (or Server-Sent Events):** a persistent connection over which the server *pushes* location updates in real time. **This is the right tool for live driver tracking** — and articulating *why* (high-frequency, server-initiated, low-latency push that REST's request/response model handles poorly) is exactly the senior insight being tested.

The mature answer: **REST for the CRUD lifecycle** (request ride, get receipt, rate driver) **plus WebSockets/SSE for the real-time location stream.** REST isn't wrong; it's just the wrong tool for one specific sub-problem, and knowing where the boundary lies is the point.

### Idempotency — double-tap protection

A rider double-taps "Request Ride." Without protection, two rides and two matches. `POST /v1/rides` carries an **`Idempotency-Key`** so the second tap returns the *same* ride, not a new one.

### Versioning when clients can't be forced to upgrade

Millions of installed mobile apps won't upgrade on your schedule. You **must** version defensively (URI `/v1/`), keep old versions alive for a long deprecation window, make changes additive, and use `Deprecation`/`Sunset` signaling — because you literally cannot force the client to update. This constraint dominates the API-evolution strategy for any mobile-first product.

**Real-world reference:** **Uber**'s H3 geospatial library and their rider/driver APIs; **Lyft**'s public API.

</details>

<a name="case-social"></a>

<details>
<summary><b>📱 Case Study 4 — Social Media Feed API (Twitter/Instagram-style)</b></summary>

### Why it's loved

Classic **pagination + fan-out** design under a REST contract, at a scale where naive choices fall apart.

### Mapping the domain onto REST components

The feed's mapping is dominated by the query string (pagination, field selection) and by the choice to model likes as an idempotent state-set rather than a create. Note how comments nest for listing but flatten for addressing.

| Domain entity | Resource / URL | Method(s) | Path vs query | Key headers | Body |
|---|---|---|---|---|---|
| Feed | `/v1/feed?cursor=…&limit=20` | `GET` | cursor + limit in query | `Authorization` | none |
| Post | `/v1/posts`, `/v1/posts/{id}?fields=id,text,author` | `POST`/`GET` | field selection in query | `Idempotency-Key` (create) | text/media on create |
| Comment | list: `/v1/posts/{id}/comments`; item: `/v1/comments/{id}` | `GET`/`POST`/`DELETE` | nested to list, flat to address | `Authorization` | comment text |
| Like | `/v1/posts/{id}/like` | `PUT`/`DELETE` | id in path | `Authorization` | none (state-set) |
| Follow | `/v1/users/{id}/following/{targetId}` | `PUT`/`DELETE` | ids in path | rate-limited | none |

The senior distinctions: **pagination and sparse-fieldset selection live in the query string** (`?cursor=…`, `?fields=…`), a like is modeled as an **idempotent `PUT`/`DELETE` on a flat sub-resource** rather than a `POST` toggle (so a retried double-tap can't create duplicate likes), and comments **nest for listing but flatten to `/comments/{id}`** for edit/delete because they're addressed independently.

### Cursor pagination — why it wins at scale

A feed is effectively infinite and constantly changing, which is the exact scenario where **offset pagination fails** (slow deep pages, drift showing duplicates as new posts arrive). So the feed uses **cursor pagination**:

```http
GET /v1/feed?cursor=eyJ0cyI6MTY4...&limit=20
→ 200 OK
{
  "data": [ /* 20 posts */ ],
  "pagination": { "next_cursor": "eyJ0cyI6MTY3...", "has_more": true }
}
```

The cursor encodes the last item's `(timestamp, id)`, giving an indexed range scan that stays fast at any depth and stable as new posts appear at the top. (Twitter, Instagram, and Slack all use cursors for exactly this reason — see [Pagination](#13--pagination-filtering--sorting).)

### Fan-out — the real design decision behind POST /posts

When a user posts, how does it reach followers' feeds? Two models, and the API design exposes the choice:

- **Fan-out-on-write (push):** when you `POST /posts`, the system *immediately* writes that post into every follower's precomputed feed. Reads are then trivially fast (feed is ready), but a celebrity with 100M followers triggers 100M writes — the "thundering herd." So `POST /posts` **returns `201` immediately** while fan-out happens **asynchronously** in the background; the response does not wait for feed generation.
- **Fan-out-on-read (pull):** feeds are assembled *at read time* by merging the posts of everyone you follow. Cheap writes, expensive reads.
- **Hybrid (what Twitter/Instagram actually do):** fan-out-on-write for normal users, fan-out-on-read for celebrities — the best of both.

The API insight: `POST /posts` is **accepted and acknowledged fast**, with feed propagation decoupled and async. An interviewer wants you to say the write returns before the fan-out completes, and to explain why.

### Nested resources — posts → comments → likes

`GET /posts/{id}/comments` is natural one level deep. Likes, though, are better as a **toggle on a flat sub-resource** than a growing nested list you paginate. And you *flatten* when a resource is addressed independently — a specific comment is `/comments/{id}` for editing/deleting, even though you list them under a post.

### Idempotency on like/unlike

Is "like" a `POST /posts/{id}/likes` (create) or a `PUT /posts/{id}/like` (set state)? **Modeling it as an idempotent state-set is cleaner:** `PUT` "liked" is idempotent (double-tap = still liked), `DELETE` removes it. This avoids the duplicate-like bug that a naive `POST` toggle creates when the network retries.

### Field selection — the "why not GraphQL?" segue

To reduce over-fetching on mobile, REST offers **sparse fieldsets**: `GET /posts?fields=id,text,author`. This works, but as soon as clients want *deeply nested, per-screen-varying* shapes ("give me the post, its author's avatar, and the first 3 comments with their authors"), you're reinventing a query language badly — which is the natural moment to say **"this is where GraphQL earns its place,"** and Facebook built GraphQL for precisely this feed-composition problem.

### Rate limiting write endpoints

Posting, liking, and following are abused by bots, so write endpoints are rate-limited per user, typically with a **sliding window** (smoother than fixed window's boundary bursts) enforced at the gateway via Redis, returning `429 + Retry-After` when exceeded (see [Rate-Limiting Algorithms](#24--rate-limiting-algorithms--where-they-live)).

**Real-world reference:** **Twitter/X API v2** (cursor pagination, field selection), **Instagram Graph API**.

</details>

<a name="case-notification"></a>

<details>
<summary><b>🔔 Case Study 5 — Notification / Messaging Service API</b></summary>

### Why it's loved

The most "senior" of the set — it tests **async API design and delivery guarantees**, concepts that separate mid-level from staff.

### Mapping the domain onto REST components

This mapping is unusual because the *status code* is the star: `202 Accepted` for async work and `207 Multi-Status` for partial batch success. The entities are simple; the semantics in headers and codes are where the design lives.

| Domain entity | Resource / URL | Method(s) | Path vs query | Key headers | Success code |
|---|---|---|---|---|---|
| Notification | `/v1/notifications`, `/v1/notifications/{id}` | `POST`/`GET` | flat top-level | `Idempotency-Key` | `202 Accepted` + `Location` |
| Batch send | `/v1/notifications/batch` | `POST` | flat | `Idempotency-Key` | `207 Multi-Status` |
| Delivery status | `/v1/notifications/{id}` | `GET` | id in path | poll or webhook | `200 OK` |
| Webhook subscription | `/v1/webhook_endpoints` | `POST` | flat | signed callbacks | `201 Created` |
| Template | `/v1/templates/{id}` | `GET`/`PUT` | id in path | `Authorization` | `200 OK` |

The distinctions interviewers reward: **`POST /notifications` returns `202`, not `201`**, because delivery hasn't happened yet — only acceptance; the **batch endpoint returns `207`** to report per-item success/failure honestly; and the **`Idempotency-Key` header prevents duplicate sends**, since a duplicate notification is user-visible spam.

### Sync request, async delivery — 202 Accepted

Sending a notification (email, SMS, push) is inherently asynchronous: you accept the request, queue it, and deliver later through providers you don't control. So `POST /notifications` returns **`202 Accepted`, not `200`/`201`** — the semantically correct code for "I've accepted this for processing but it isn't done yet":

```http
POST /v1/notifications HTTP/1.1
Idempotency-Key: 9c1a...
Content-Type: application/json

{ "to": "user_42", "channel": "email", "template": "welcome", "data": {...} }
```

```http
HTTP/1.1 202 Accepted
Location: /v1/notifications/ntf_123
{ "id": "ntf_123", "status": "QUEUED" }
```

The `Location` header points at a resource whose status the client can track.

### Status polling vs webhook callback

How does the sender learn the outcome (`DELIVERED`, `BOUNCED`, `FAILED`)? Two mechanisms, and a staff answer offers **both**:

- **Polling** `GET /v1/notifications/ntf_123` — simple, client-controlled, but wasteful and laggy.
- **Webhooks** — you register a callback URL and the service `POST`s delivery events to *you*. Efficient and timely, but requires the receiver to be publicly reachable, to verify signatures, and to handle duplicate/out-of-order events idempotently.

Offer polling for simplicity and webhooks for scale; document that webhooks may retry, so consumers must be idempotent.

### Idempotency keys prevent duplicate sends

A retried `POST /notifications` must not send two emails. The **`Idempotency-Key`** ensures one logical send regardless of retries — critical because a duplicate notification is user-visible spam.

### Bulk operations & partial success — 207 Multi-Status

Sending to 10,000 users in one call: some succeed, some fail (invalid addresses). A single `200`/`400` can't express "mostly worked." Use **`POST /v1/notifications/batch`** returning **`207 Multi-Status`** with a per-item breakdown:

```http
HTTP/1.1 207 Multi-Status
{
  "results": [
    { "to": "user_1", "status": "QUEUED",  "id": "ntf_a" },
    { "to": "bad@",    "status": "REJECTED","error": "invalid_address" }
  ]
}
```

This honest per-item modeling of partial success is a hallmark of senior API design.

### Delivery guarantees in the contract

The API must be explicit about semantics. **At-least-once** delivery (the common choice) means a message may be delivered *more than once* on retry — which pushes idempotency onto the *consumer*: the client must dedupe on a message ID. **At-most-once** never duplicates but may drop. **Exactly-once** is famously hard and usually approximated by at-least-once + consumer-side dedupe. Surfacing this in the contract — "we guarantee at-least-once; dedupe on `message_id`" — is exactly the delivery-semantics reasoning interviewers want.

### Backpressure — 429 + Retry-After

When the system is saturated, it signals backpressure with **`429 Too Many Requests`** (or `503`) plus **`Retry-After`**, telling clients to slow down rather than collapsing. Well-behaved clients honor it with exponential backoff.

**Real-world reference:** **Twilio** (SMS, status callbacks), **AWS SNS/SES**, **Firebase Cloud Messaging** — all use 202-style async acceptance, webhooks, and batch APIs.

</details>

<a name="case-payments"></a>

<details>
<summary><b>💳 Case Study 6 — Digital Payment Platform (Stripe/PayPal-style)</b></summary>

### Problem statement

Design APIs for merchants to **accept payments, issue refunds, manage subscriptions, generate invoices, and receive webhook notifications.** This case study is the capstone — it combines idempotency, state machines, security, async delivery, and consistency all at once, which is why it's the hardest and most revealing.

### Resource model

```http
POST /v1/payment_intents            # start a payment (idempotent)
GET  /v1/payment_intents/{id}       # payment status
POST /v1/payment_intents/{id}/confirm
POST /v1/refunds                    # refund a charge (idempotent)
GET  /v1/charges/{id}
POST /v1/subscriptions              # recurring billing
GET  /v1/invoices/{id}
POST /v1/webhook_endpoints          # register for events
```

Stripe's **PaymentIntent** model is the reference: a payment is not a single call but a *resource with a lifecycle* (`requires_payment_method → requires_confirmation → processing → succeeded`/`requires_action` for 3D-Secure), which cleanly handles multi-step authentication (SCA/3DS) that a one-shot "charge" endpoint cannot.

### Mapping the domain onto REST components

The payments mapping combines every component seen in the other cases: idempotency keys on money moves, action sub-resources for lifecycle steps, signed webhooks for async truth, and a strict split between publishable and secret credentials in the `Authorization` header.

| Domain entity | Resource / URL | Method(s) | Path vs query | Key headers | Body |
|---|---|---|---|---|---|
| PaymentIntent | `/v1/payment_intents`, `/{id}` | `POST`/`GET` | flat top-level | `Idempotency-Key`, secret key | amount/currency/method |
| Confirm action | `/v1/payment_intents/{id}/confirm` | `POST` | action sub-resource | `Idempotency-Key` | confirmation params |
| Refund | `/v1/refunds` | `POST`/`GET` | flat, references a charge | `Idempotency-Key` | `charge` id + amount |
| Subscription | `/v1/subscriptions`, `/{id}/cancel` | `POST`/`PATCH` | action sub-resource for cancel | secret key | plan/customer |
| Invoice | `/v1/invoices/{id}` | `GET`/`PATCH` | id in path | secret key | line items |
| Webhook endpoint | `/v1/webhook_endpoints` | `POST` | flat | verifies `Stripe-Signature` | callback URL + events |

The senior distinctions: **every money-moving `POST` carries an `Idempotency-Key`**; **lifecycle steps that aren't CRUD (`confirm`, `cancel`) are modeled as `POST` action sub-resources**, not overloaded `PATCH`es; and the **`Authorization` header distinguishes publishable keys** (browser-safe, can only tokenize) **from secret keys** (server-only), so raw card data never reaches the merchant server for PCI-DSS.

### Idempotency — the non-negotiable core

Every money-moving `POST` (payments, refunds) requires an **`Idempotency-Key`**:

```http
POST /v1/payment_intents HTTP/1.1
Authorization: Bearer sk_live_...
Idempotency-Key: 6b3f2a1c-9e8d-...
Content-Type: application/x-www-form-urlencoded

amount=5000&currency=usd&payment_method=pm_card_visa
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{ "id": "pi_3Nx...", "status": "succeeded", "amount": 5000, "currency": "usd",
  "charges": { "data": [ { "id": "ch_3Nx...", "paid": true } ] } }
```

If the client times out and retries with the same key, Stripe returns the *original* PaymentIntent (same `pi_3Nx…`, same `succeeded`) — never a second charge. Stripe additionally stores a **request fingerprint** and errors if the same key arrives with a different body (catching client bugs). This is the canonical real-world implementation of everything in [Idempotency Keys](#23--idempotency-keys-in-distributed-systems): atomic key insert, stored response, 24-hour retention.

### Webhooks — asynchronous truth

Payment outcomes settle asynchronously (bank authorization, 3DS challenges, ACH clearing that can take days). The platform **`POST`s events to the merchant's registered webhook** (`payment_intent.succeeded`, `charge.refunded`). The critical design details interviewers probe:

- **Signature verification:** webhooks carry a signature header (`Stripe-Signature`) so the merchant can verify the call genuinely came from Stripe and wasn't spoofed.
- **Idempotent handling:** webhooks are delivered **at-least-once** and may arrive out of order or duplicated, so the receiver must dedupe on the event ID.
- **Retries with backoff:** if the merchant's endpoint is down, the platform retries with exponential backoff for hours — so the endpoint must be idempotent and fast (acknowledge with `200` quickly, process async).

A delivered webhook is itself just a `POST` to the merchant's URL:

```http
POST /my/webhook-handler HTTP/1.1
Stripe-Signature: t=1720000000,v1=5257a8...   # verify before trusting
Content-Type: application/json

{ "id": "evt_1Nx...", "type": "payment_intent.succeeded",
  "data": { "object": { "id": "pi_3Nx...", "status": "succeeded" } } }
```

The merchant verifies the `Stripe-Signature` header, dedupes on the event `id` (`evt_1Nx…`) in case of redelivery, and returns `200` fast — doing the real work asynchronously so a slow handler doesn't trigger a retry storm.

### State machines & PATCH

Subscriptions (`active → past_due → canceled`) and invoices (`draft → open → paid → void`) are **state machines exposed as resources**, advanced via `POST` action sub-resources (`/subscriptions/{id}/cancel`) or `PATCH`, with server-enforced legal transitions.

### Security & consistency

TLS everywhere; secret vs publishable API keys (publishable keys are safe in the browser and can only create tokens, secret keys stay server-side); **PCI-DSS** compliance means raw card numbers never touch the merchant's server — they're exchanged for a **token** client-side, and only the token flows through the API. On consistency: a ledger must never lose or double-count money, so writes are transactional/idempotent and the API exposes an authoritative, strongly-consistent view of each charge's status.

### Refunds — the reverse idempotent flow

`POST /v1/refunds` with an idempotency key, referencing the original charge, is itself a state machine (`pending → succeeded`/`failed`) and must be retry-safe so a network blip never issues two refunds.

**Real-world reference:** **Stripe** (PaymentIntents, idempotency keys, signed webhooks) and **PayPal**/**Braintree** are the definitive examples — this case study is essentially "rebuild Stripe's core API," which is why it's a favorite closing question.

</details>

---

# Part VII — Revision & Interview Prep

## 27. ⚡ Quick Revision

**HTTP is a request/response protocol over TCP** (QUIC/UDP for HTTP/3). Every message is a start line, headers, a blank line, and an optional body. REST assigns *meaning* to those parts: the path names a resource, the method declares intent, the status code reports the outcome, and headers carry cross-cutting concerns. Get this mental model and most backends look the same.

The **methods** carry two guarantees engineers build on. *Safe* (`GET`, `HEAD`, `OPTIONS`) means no side effects. *Idempotent* (`GET`, `PUT`, `DELETE`) means N calls equal one call. `POST` is neither — it creates something new every time, which is why retries need idempotency keys. `PUT` replaces the whole resource; `PATCH` changes part of it; prefer absolute-value PATCH bodies so they stay idempotent. **Status codes** split at 4xx vs 5xx: 4xx is the client's fault (don't retry blindly), 5xx is the server's (retry may help). Know `201`+`Location`, `202` (async accepted), `204` (no body), `304` (not modified), `409` (conflict), `422` (validation), `429` (rate limited). **Statelessness** means each request is self-contained so any server can handle it — state moves to a shared DB/cache or into the token, enabling horizontal scale.

**REST** is Fielding's set of constraints (client-server, stateless, cacheable, uniform interface, layered). Model **nouns not verbs** (`POST /orders`, not `/createOrder`), plural collections, shallow nesting, lowercase-hyphenated stable URIs, query params for filter/sort/paginate. **HATEOAS** (responses carry next-action links) is REST Level 3 — powerful for evolvability, used by Open Banking and GitHub, skipped by most. The **Richardson Maturity Model** grades APIs 0 (one URI/verb) → 1 (resources) → 2 (real verbs + status codes, where most good APIs live) → 3 (hypermedia). Aim for Level 2 by default.

**Request design** has four channels: path (identity), query (filter/sort/paginate), headers (auth, caching, idempotency), body (the representation). **Pagination**: offset is simple and allows page-jumps but is slow-when-deep and drifts on insert; **cursor (keyset)** encodes the last-seen key for a fast, stable indexed range scan — the choice for large/infinite feeds (Twitter, Stripe, Slack). **Versioning**: additive changes are safe; breaking changes need a version. URI versioning (`/v1/`) dominates for its simplicity; Stripe's date-pinned versioning with internal shims is the gold standard.

**Idempotency** is what makes distributed retries survivable, and there is a *spectrum* of approaches — pick the lightest that fits. Free: lean on naturally idempotent methods (`PUT` sets an absolute state, `DELETE` removes) and prefer full-replacement over relative mutations. For non-idempotent `POST` (payments, orders), the client sends a unique `Idempotency-Key` and the server executes once, stores the result, and replays it on retry — no double charge. When the domain has a naturally unique field (email, order id), a DB unique constraint dedupes for free. For racing *updates*, `ETag`+`If-Match`→`412` gives optimistic concurrency; for workflows, a state-machine guard makes a repeated transition a no-op. At scale, insert the key **atomically** (unique constraint / `SET NX`) to beat concurrent-retry races, scope it per account+endpoint, store a request fingerprint, and expire after ~24h. **Caching** has two levers: freshness (`Cache-Control: max-age`) lets clients/CDNs skip the network entirely; validation (`ETag` + `If-None-Match` → `304`) avoids re-downloading unchanged data. The same ETag as `If-Match` on a write gives optimistic concurrency (`412` on mismatch), preventing lost updates. **Errors** should follow **RFC 7807** (`application/problem+json` with `type`/`title`/`status`/`detail`/`instance` + custom fields), use the right code, expose a stable machine-readable error code, never leak internals, and always carry a request ID.

**Security**: authentication (who — 401 if unproven) vs authorization (allowed — 403 if not). API keys for server-to-server; OAuth2 for delegated access; JWT as a signed, self-contained token any server verifies without a lookup (fits statelessness; revocation needs short TTLs + refresh tokens). **CORS** is a *browser* rule (preflight `OPTIONS`) governing cross-origin reads — not auth, and invisible to curl. **Rate limiting** returns `429 + Retry-After`; token bucket (allows bursts, enforces average) is the popular algorithm, enforced at the gateway backed by Redis so the limit is global.

**Performance** stacks up: compress text (gzip/Brotli, 70–90% smaller); cache + CDN to serve reads near users; Keep-Alive to reuse connections; **HTTP/2** multiplexes many streams over one connection (kills HTTP/1.1 head-of-line blocking); **HTTP/3** runs on QUIC/UDP for per-stream independence, faster handshakes, and connection migration (Wi-Fi ↔ cellular) — big for mobile. **Protocol choice**: REST for public/CRUD APIs (caching, ubiquity), gRPC for internal microservices (binary Protobuf over HTTP/2, streaming, typed), GraphQL when one client composes varied nested data and over-fetching hurts. **OpenAPI/Swagger** documents the contract and generates SDKs/mocks; **contract testing** (Pact) stops microservices from silently breaking each other.

**Mapping any domain onto REST** reduces to a repeatable drill: pick the **method** by intent (`GET` read, `POST` create, `PUT` full-replace, `PATCH` partial, `DELETE` remove, and a `POST` to an action sub-resource like `/orders/{id}/cancel` for non-CRUD verbs); shape the **URL** as plural nouns with identity in the path and everything that filters or shapes a list — pagination, sorting, search, field selection — in the query string; put identity and metadata in **headers** (`Authorization`, `Content-Type`/`Accept`, `Idempotency-Key`, `If-Match`/`If-None-Match`); reserve the **body** for the representation on writes only; and let the **status code** carry the outcome (`201`+`Location`, `202` async, `204` empty, `400`/`422` bad input, `409` conflict, `412` precondition). Every case study is just this drill applied to a domain — deciding which nouns become resources, how they nest, and where the standard operations need reinforcement.

**Case-study reflexes**: banking → OAuth2 scopes + strong consistency + audit + HATEOAS consent flows; e-commerce → idempotency keys on orders + ETag/`If-Match` optimistic concurrency on stock + saga for partial failures; ride-sharing → geospatial query + ride state machine via PATCH + **WebSockets for live location** (where REST breaks) + defensive versioning for un-upgradable mobile clients; social feed → cursor pagination + async fan-out (write returns before propagation) + idempotent like via PUT + sparse fields segueing to GraphQL; notifications → `202 Accepted` + webhooks-or-polling + `207 Multi-Status` batch + at-least-once semantics pushing dedupe to consumers; payments → PaymentIntent lifecycle + idempotency keys + signed, retried, idempotent webhooks + PCI tokenization.

---

## 28. 🎓 FAANG Interview Q&A (20 Questions)

<details>
<summary><b>Q1. What's the difference between PUT and PATCH, and when does it actually matter?</b></summary>

`PUT` replaces the *entire* resource — any field you omit is effectively cleared — which makes it idempotent by definition (replaying the same full representation yields the same state). `PATCH` applies a *partial* update, sending only the fields to change. The distinction bites in real systems: for an order state transition you use `PATCH /orders/42 {"status":"SHIPPED"}` because a `PUT` would force the client to resend the whole order and risk clobbering the total or address. The subtle trap is that PATCH is *not guaranteed* idempotent — `{"op":"increment","amount":10}` differs each call — so design PATCH bodies with absolute values (`{"status":"SHIPPED"}`) rather than deltas when you want retry-safety. Stripe and GitHub both use PATCH for partial updates precisely to avoid the full-replacement footgun.

</details>

<details>
<summary><b>Q2. Explain safe vs idempotent. Why does POST break the pattern?</b></summary>

*Safe* means the method has no side effects — it only reads — so `GET`, `HEAD`, and `OPTIONS` can be freely retried, prefetched, and cached. *Idempotent* means N identical calls have the same effect as one: `GET`, `PUT`, and `DELETE` qualify (deleting twice still leaves the resource deleted). `POST` is neither safe nor idempotent because it is defined to *create a new resource on every call* — retry a `POST /orders` after a lost response and you get two orders. This matters enormously in distributed systems where timeouts and retries are routine: the standard fix is an `Idempotency-Key` header that lets the server recognize a retried POST and replay the original result. The whole reliability of payment and order systems hinges on understanding this gap.

</details>

<details>
<summary><b>Q3. When should you return 400 vs 401 vs 403 vs 404 vs 409 vs 422?</b></summary>

`400` is a malformed request (bad JSON, missing required param). `401` means "not authenticated — I don't know who you are" (a misnomer, it's about authentication). `403` means "authenticated but not permitted" — we know you and you still can't do this. `404` is "resource doesn't exist" — and note some APIs deliberately return 404 instead of 403 to avoid leaking that a resource exists to unauthorized users. `409` is a conflict with current state (optimistic-lock version mismatch, duplicate creation). `422` (Unprocessable Entity) is for well-formed but semantically invalid data (validation failures) — many teams prefer it over 400 to distinguish "your JSON is broken" (400) from "your JSON is valid but the email is taken" (422). Getting these right drives client retry logic and monitoring.

</details>

<details>
<summary><b>Q4. What is HATEOAS and why do most APIs skip it?</b></summary>

HATEOAS (Hypermedia As The Engine Of Application State) means responses include links describing valid next actions — an order response carries a `cancel` link only if cancellation is currently allowed. The theoretical payoff is decoupling: clients follow server-provided links instead of hard-coding URLs, so the server can restructure URIs freely, and the set of legal actions is driven by server state rather than duplicated client logic. It's the top level (3) of the Richardson Maturity Model. Most APIs skip it because real clients — especially mobile apps with baked-in navigation — hard-code URLs anyway for performance and simplicity, tooling support is thin, and payloads grow. It genuinely shines in Open Banking/PSD2 (multi-step consent flows) and GitHub's API. The senior answer isn't "HATEOAS is good/bad" but "here's the tradeoff and why we chose Level 2."

</details>

<details>
<summary><b>Q5. How does ETag-based caching work, and how is it different from Cache-Control?</b></summary>

They answer two different questions. `Cache-Control: max-age=3600` handles *freshness* — for an hour the client or CDN serves the cached copy with **zero network calls**, the fastest possible request. `ETag` handles *validation* — when the cached copy expires, instead of blindly re-downloading, the client sends `If-None-Match: "<etag>"`; if unchanged, the server replies `304 Not Modified` with an empty body, saving the payload bandwidth while confirming freshness. So Cache-Control avoids the request entirely while fresh; ETag avoids the *payload* when stale-but-unchanged. A concrete example: a CDN serves a product image for an hour via max-age, then revalidates with the ETag and gets a 304 if the image hasn't changed. Bonus: the same ETag sent as `If-Match` on a write gives you optimistic concurrency control.

</details>

<details>
<summary><b>Q6. Offset vs cursor pagination — which and why?</b></summary>

Offset (`?offset=40&limit=20`) is intuitive and lets you jump to any page, but it degrades two ways at scale: the database must scan and discard all `offset` rows (page 10,000 is slow), and it *drifts* — if rows are inserted while paging, you see duplicates or skips. Cursor/keyset pagination (`?cursor=<opaque>&limit=20`) encodes the last-seen sort key (e.g. `created_at,id`) and issues `WHERE (created_at,id) < (?,?) ORDER BY ... LIMIT 20` — an indexed range scan that's fast at *any* depth and stable against inserts. The tradeoff: no arbitrary page jumps, only next/previous, and the cursor must be opaque. Use cursor for large, changing, infinite-scroll feeds (Twitter, Stripe, Slack all do); use offset only for small, bounded, admin tables where "go to page N" matters.

</details>

<details>
<summary><b>Q7. Walk me through designing an idempotent payment endpoint.</b></summary>

The client generates a unique `Idempotency-Key` (a UUID) per logical payment attempt and sends it as a header on `POST /payments`. Server-side, before doing any work, I **atomically insert** the key into a unique-constrained store (DB row or Redis `SET NX`) — this is what beats the concurrent-retry race where two copies of the same request arrive simultaneously. The winner proceeds to charge and stores `key → {status, response}`; the loser gets a uniqueness violation and either waits for the winner's result or returns `409 in-progress`. On a later retry with the same key, the server returns the *stored* original response — one charge, no matter how many deliveries. I'd also store a fingerprint of the request body to reject the same key used with different parameters (a client bug), scope the key per account+endpoint, and expire keys after ~24 hours. This is exactly Stripe's design.

</details>

<details>
<summary><b>Q8. REST vs gRPC vs GraphQL — how do you choose?</b></summary>

I match the style to the client and traffic. **REST** for public-facing and resource-CRUD APIs — it rides HTTP caching natively, is universally understood, and has huge tooling; its weakness is over/under-fetching. **gRPC** for internal microservice-to-microservice calls — Protobuf over HTTP/2 gives compact binary payloads, first-class streaming, and strict typed contracts with codegen; its weakness is poor browser support and human-unreadable payloads. **GraphQL** when a single client needs to compose varied, nested data from many sources and over-fetching is a real cost (rich mobile + web clients from one graph); its weakness is that HTTP caching breaks and you must guard against expensive queries. A mature company often runs all three: REST at the public edge, gRPC between services, GraphQL in a BFF aggregation layer. The wrong answer is dogma; the right one is "it depends on who's calling and what the data shape is."

</details>

<details>
<summary><b>Q9. How do you version an API without breaking existing clients?</b></summary>

First distinguish non-breaking from breaking: adding optional fields, endpoints, or enum values is safe (old clients ignore them); removing/renaming fields, changing types, or tightening validation is breaking. I make evolution *additive* by default (Postel's Law) to avoid version bumps entirely. When a break is unavoidable, URI versioning (`/v1/`, `/v2/`) is the pragmatic default — visible, trivially routable at the gateway, easy to test. But the gold standard for scale is **Stripe's date-pinned versioning**: each integration is pinned to the API version it onboarded with, and internal transformation shims translate new responses back into old shapes, so every client sees a frozen contract while the code evolves freely. Alongside, I run a deprecation lifecycle with `Deprecation`/`Sunset` headers and *per-client usage analytics* — you can't remove what you can't measure who's still calling.

</details>

<details>
<summary><b>Q10. What's the difference between authentication and authorization in a REST API?</b></summary>

Authentication answers "who are you?" and authorization answers "what may you do?" A request can pass one and fail the other — we can authenticate that you're Ada (valid token) yet forbid you from deleting another user's account. HTTP encodes both: `401 Unauthorized` really means "authenticate first" (a naming misnomer), while `403 Forbidden` means "we know who you are and you're not permitted." Concretely: a JWT or OAuth2 access token establishes *identity* (authN); the *scopes/roles/claims* inside it, checked against the operation, enforce *permission* (authZ) — e.g. Spring Security's `@PreAuthorize("hasAuthority('SCOPE_users:delete')")` returns 403 if the token lacks that scope. Keeping the two concerns separate (a gateway does authN, services do fine-grained authZ) is a common and clean split.

</details>

<details>
<summary><b>Q11. [Staff] How do you guarantee no lost updates when two clients edit the same resource concurrently?</b></summary>

The REST-native answer is **optimistic concurrency control via ETags**. Each `GET` returns an ETag (a version/hash). To write, the client sends `If-Match: "<etag>"`. The server compares it to the current version: if they match, the write proceeds and the version bumps; if the resource changed in between, the ETags differ and the server rejects with `412 Precondition Failed`, forcing the client to re-fetch and reconcile. This is compare-and-swap over HTTP — no locks held across the network, which is critical for availability. I'd contrast it with pessimistic locking (holding a DB lock across a user's think-time is a scalability disaster) and note the e-commerce use case: two buyers racing for the last unit both send `If-Match: "v7"`; the first wins and bumps to `v8`, the second gets `412` and no oversell occurs. For high-contention counters, I might instead use an atomic server-side decrement rather than round-tripping ETags.

</details>

<details>
<summary><b>Q12. [Staff] Design the rate-limiting layer for a public API. Algorithm and placement?</b></summary>

I'd use a **token bucket** for most endpoints: a bucket of N tokens refilling at a steady rate, each request spending one, rejected with `429 + Retry-After` when empty. It allows controlled bursts while enforcing a long-run average — closer to real traffic than a rigid fixed window, which suffers boundary bursts (100 requests at 11:00:59 plus 100 at 11:01:00). For placement, the limiter sits at the **API gateway / edge**, not inside each service, so abusive traffic is rejected early, and it's backed by a **centralized Redis** so the limit is enforced *globally* — a per-instance in-memory counter would let a client multiply its limit by the fleet size. I'd layer it: CDN/WAF (Cloudflare, AWS WAF) for volumetric abuse at the very edge, gateway + Redis for per-client/per-key fairness, and per-endpoint limits inside services for especially expensive operations. I'd also return `X-RateLimit-Remaining`/`Reset` headers so well-behaved clients self-throttle.

</details>

<details>
<summary><b>Q13. [Staff] A payment succeeds but the inventory reservation fails. How does your API surface this?</b></summary>

You can't hold a distributed ACID transaction across independent payment and inventory services, so I'd use a **saga with compensating actions**, and — crucially — surface the truth in the API rather than lying with a 200. The order moves to a `PAYMENT_CONFIRMED` state, then attempts the inventory reservation; on failure it triggers a **compensating refund** and transitions to `CANCELLED_REFUNDED`. The client polling `GET /orders/{id}` (or receiving a webhook) sees a status that reflects reality. The alternative — trying to make it look atomic — hides a money-vs-stock inconsistency that becomes a support nightmare. I'd make each step idempotent (so retries during the saga don't double-charge or double-reserve) and emit events at each transition for observability. This is the classic "eventual consistency exposed honestly through resource state" answer, and it's why the order is modeled as a state machine rather than a boolean success flag.

</details>

<details>
<summary><b>Q14. [Staff] Why does `POST /posts` return before the feed fan-out completes?</b></summary>

Because synchronous fan-out doesn't scale. In fan-out-on-write, a new post is copied into every follower's precomputed feed; for a celebrity with 100M followers that's 100M writes, and making the author's request block on them would give a multi-minute (or failing) `POST`. So `POST /posts` validates, persists the post, returns `201` **immediately**, and the fan-out runs **asynchronously** via a queue. Reads stay fast because feeds are precomputed, and the write stays fast because it's decoupled from propagation. The eventual-consistency cost — a follower might not see the post for a second or two — is acceptable for a social feed (unlike a bank balance). Real systems go hybrid: fan-out-on-write for normal users, fan-out-on-read for celebrities to avoid the write storm. The API insight the interviewer wants: acknowledge fast, propagate async, and be explicit that the feed is eventually consistent.

</details>

<details>
<summary><b>Q15. [Staff] When does REST stop being the right tool, and what do you reach for?</b></summary>

REST's request/response model is a poor fit for **high-frequency, server-initiated, low-latency push**. The canonical example is live driver location in a ride-sharing app: polling `GET /rides/{id}/driver-location` every second is enormously wasteful and still stale between polls. There I'd keep REST for the CRUD lifecycle (request ride, fetch receipt, rate driver) but use **WebSockets or Server-Sent Events** for the location stream — a persistent connection the server pushes over. Similarly, for internal high-throughput service calls I'd reach for **gRPC streaming**, and for "accepted now, delivered later" work I'd use **`202 Accepted` + webhooks** rather than pretending it's synchronous. The senior move is recognizing REST isn't wrong globally — it's the wrong tool for one sub-problem — and drawing the boundary explicitly rather than forcing everything through one paradigm.

</details>

<details>
<summary><b>Q16. [Staff] Explain at-least-once vs exactly-once delivery and how it shapes your API contract.</b></summary>

Most messaging/webhook systems guarantee **at-least-once**: a message will be delivered, but retries after ambiguous failures mean it may arrive *more than once* and possibly out of order. **At-most-once** never duplicates but may drop. **Exactly-once** is famously near-impossible end-to-end and is practically approximated as at-least-once plus consumer-side deduplication. This shapes the contract directly: if I offer at-least-once (as Stripe webhooks and AWS SNS do), I must document it and require the *consumer* to dedupe on a stable `event_id`/`message_id` and to treat handlers as idempotent. The API therefore always includes a unique event ID, and I acknowledge receipt with a fast `200` then process async so retries don't pile up. Saying "we guarantee at-least-once, dedupe on `id`" is the precise, honest contract that lets clients build correctly on top.

</details>

<details>
<summary><b>Q17. [Staff] How do you design bulk operations with partial success?</b></summary>

A single `200`/`400` can't express "8,000 succeeded, 2,000 failed," so for `POST /notifications/batch` I return **`207 Multi-Status`** with a per-item result array: each entry carries its own status (`QUEUED`/`REJECTED`) and, on failure, an error code and reason. This lets the client retry only the failures rather than blindly resending everything (which without idempotency keys would duplicate the successes). I'd cap batch size, make the whole call idempotent via an `Idempotency-Key`, and for very large jobs switch to an async model — `202 Accepted` returning a job resource the client polls (`GET /jobs/{id}`) for aggregate progress. The anti-pattern to avoid is all-or-nothing semantics on a huge batch (one bad address fails 10,000 sends) or a bare `200` that hides failures. Honest per-item modeling is the hallmark of senior async API design.

</details>

<details>
<summary><b>Q18. [Staff] What makes an API backward compatible, and how do you deprecate safely at scale?</b></summary>

Backward compatibility means old clients keep working unchanged. Safe (additive) changes: new optional fields, new endpoints, new optional params, new enum values *if clients tolerate unknowns*. Breaking changes: removing/renaming fields, changing types, making optional fields required, changing defaults, or repurposing fields. To deprecate safely I run a lifecycle rather than a hard cutover: announce it, emit `Deprecation` and `Sunset` HTTP headers, and — most importantly — **instrument per-client usage of the deprecated path**, because you can't remove what you can't measure who still calls. I reach out to heavy users, wait for usage to fall below a threshold past the sunset date, then remove. The traps: enums breaking exhaustive client switches, silent default changes altering behavior overnight, and "optional → required" feeling additive but being breaking. Stripe's version-pinning with transformation shims is the reference implementation for evolving freely while freezing each client's contract.

</details>

<details>
<summary><b>Q19. [Staff] Why HTTP/3, and when does it actually matter?</b></summary>

HTTP/2 solved application-layer head-of-line blocking with multiplexing, but it still runs on TCP, so a *single lost packet* stalls *all* streams on the connection (transport-level HOL blocking). **HTTP/3 replaces TCP with QUIC over UDP**, giving each stream independent delivery (a lost packet only stalls its own stream), combining the transport and TLS handshakes into fewer round trips (often 0-RTT on reconnect), and — the killer feature for mobile — **connection migration**: because QUIC identifies connections by an ID rather than the IP/port 4-tuple, switching from Wi-Fi to cellular doesn't drop the connection. It matters most for **lossy, high-latency, mobile networks** — ride-sharing, social, video apps where users move between networks and packet loss is common. On a clean datacenter link the gains are marginal, which is why I'd deploy it at the mobile-facing edge/CDN rather than for internal service traffic.

</details>

<details>
<summary><b>Q20. [Staff] How do you prevent microservices from silently breaking each other's API contracts?</b></summary>

**Consumer-driven contract testing** (Pact, Spring Cloud Contract). Each consumer publishes the exact subset of the provider's API it depends on — specific fields, shapes, and status codes — as a contract. In CI, the provider's build replays every consumer's contract against its current implementation and **fails the build** if a change would break any consumer, *before* deploy. This catches drift (a provider renames a field, a consumer breaks) without the cost, flakiness, and slowness of spinning up full end-to-end environments for every change. I'd pair it with a **design-first OpenAPI spec** as the source of truth (so both sides build against an agreed contract), schema validation at the gateway, and versioning discipline for genuine breaking changes. The combination — OpenAPI contract + automated consumer-driven verification in CI — is the mature answer to "how do you keep dozens of independently-deployed services from breaking each other."

</details>

---

## 29. 📝 STAR Behavioral Questions

These use the **Situation → Task → Action → Result** format. Adapt the specifics to your own experience — the value is in the structure and the depth of reasoning.

<details>
<summary><b>STAR 1 — Tell me about a time you designed an API that had to scale.</b></summary>

**Situation:** Our team owned a notifications service whose `GET /notifications` endpoint returned a user's full history with offset pagination. As active users crossed a few million, p99 latency on deep pages climbed past two seconds and the database CPU spiked during peak hours.

**Task:** I was asked to fix the latency without breaking the existing mobile and web clients already calling the endpoint.

**Action:** I diagnosed that offset pagination was forcing full scans of discarded rows on deep pages, and that concurrent inserts were causing duplicate items across pages. I introduced **cursor-based pagination** keyed on `(created_at, id)` with a matching composite index, exposed via an opaque `next_cursor`. To avoid breaking existing clients, I kept the offset path alive behind the old contract, added the cursor path as an additive, opt-in parameter, and instrumented per-client usage so I could see who migrated.

**Result:** p99 on deep pages dropped from ~2s to under 50ms and stayed flat regardless of depth, database CPU at peak fell by roughly 40%, and because the change was additive we migrated clients gradually and retired the offset path months later once usage hit zero.

</details>

<details>
<summary><b>STAR 2 — Describe a production incident you helped resolve.</b></summary>

**Situation:** We got paged for a spike in duplicate charges — a small number of customers were billed twice within seconds. It correlated with a mobile carrier having intermittent connectivity that afternoon.

**Task:** I needed to stop the bleeding immediately and then eliminate the root cause so it couldn't recur.

**Action:** I traced it to clients timing out on `POST /payments` and retrying, while the original request had actually succeeded — the response was just lost. As an immediate mitigation we added a short-window server-side dedupe on `(account, amount, payment_method)` to catch the obvious duplicates. The real fix was to require a client-generated **`Idempotency-Key`** and store `key → result`, inserting the key **atomically** with a unique constraint so even *concurrent* retries couldn't both execute. I also added a request-fingerprint check to reject key reuse with a different body.

**Result:** Duplicate charges went to zero, we refunded the affected customers proactively, and the idempotency-key pattern became the standard for every money-moving endpoint in the org. We documented it and added a contract test so no new write endpoint could ship without it.

</details>

<details>
<summary><b>STAR 3 — Tell me about a technical decision where you had to weigh tradeoffs.</b></summary>

**Situation:** A new internal service needed to serve data to three consumers — a mobile app, a web dashboard, and a batch analytics job — each wanting a different shape of the same underlying data. The team's instinct was to build one generic REST API and let each client over-fetch.

**Task:** As the design lead I had to choose an architecture that served all three efficiently without bloating a single endpoint or exploding into dozens of bespoke ones.

**Action:** I laid out the options: a fat generic REST API (over-fetching on mobile, wasteful), per-client endpoints (duplication, maintenance drag), or a **GraphQL BFF layer** over gRPC backend services. I built a small prototype of the GraphQL-BFF approach, measured mobile payload sizes and round trips against the REST baseline, and presented the tradeoff honestly — GraphQL cured over-fetching and let each client request exactly its shape, at the cost of losing HTTP caching and needing query-cost guards.

**Result:** We adopted GraphQL only at the BFF layer while keeping gRPC between backend services and REST for the public partner API. Mobile payloads shrank meaningfully and round trips per screen dropped, while we contained GraphQL's complexity to one layer instead of rewriting everything. Just as important, the team aligned on *when* each protocol was appropriate rather than defaulting to one dogmatically.

</details>

<details>
<summary><b>STAR 4 — Describe a time you had to evolve an API without breaking clients.</b></summary>

**Situation:** We needed to restructure a core `Order` response — splitting a single `address` string into structured fields and renaming a misleading `amount` field — but the API was consumed by a mobile app with millions of installs we couldn't force to upgrade.

**Task:** I had to ship the improved contract for new integrations while guaranteeing every existing client kept working untouched.

**Action:** Rather than a hard `/v2` cutover that would strand old clients, I adopted a **version-pinning** approach inspired by Stripe: new fields were added additively, the old fields were kept and populated by a transformation layer, and the response served to each client was shaped according to the API version it was pinned to. I emitted `Deprecation` and `Sunset` headers on the old fields and stood up **per-client usage dashboards** so we knew exactly who still relied on the legacy shape.

**Result:** New clients got the clean structured contract immediately, not a single existing client broke, and we drove legacy-field usage down over two quarters by reaching out to the heaviest callers directly. When usage finally hit zero we removed the shim. The episode established version-pinning plus usage instrumentation as our standard playbook for breaking changes.

</details>

---

## 30. 🔗 Further Reading

- **RFC 9110** — HTTP Semantics (the modern, consolidated HTTP spec)
- **RFC 7807 / RFC 9457** — Problem Details for HTTP APIs (standard error format)
- **RFC 9114** — HTTP/3
- **Roy Fielding's dissertation (2000)** — the original definition of REST and its constraints
- **Richardson Maturity Model** — Martin Fowler's write-up on the levels of REST
- **Stripe API docs** — the reference implementation for idempotency keys, versioning, and webhooks
- **Google API Design Guide (AIP)** — Google's opinionated, battle-tested REST conventions
- **Microsoft REST API Guidelines** — another mature, detailed public style guide
- **OpenAPI Specification** — the standard for machine-readable API contracts
- **Uber Engineering: H3** — hexagonal geospatial indexing behind ride-sharing queries

---

*End of guide. Read the ⚡ Quick Revision the night before an interview; work the case studies out loud as if whiteboarding them.*


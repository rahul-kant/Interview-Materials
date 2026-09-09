# 03. System Design Scenarios: The Blueprints

This section covers the most common design problems. We will follow the standard interview flow for each.

> [!IMPORTANT]
> **Production-Ready Versions Available:** The scenarios below are "Lite" versions for quick understanding. For comprehensive, Staff Engineer level deep dives (including Failure Scenarios, Monitoring, Cost, and Security), please refer to the **[Top 20 Scenarios](./Top-20-HLD-Scenarios.md)** directory.

## Scenario 1: Design a URL Shortener (TinyURL)
**Difficulty:** Easy/Medium
**Core Concept:** Unique ID Generation

### 1. Requirements
*   **Functional:** Given a long URL, return a short URL. Redirect short URL to long URL.
*   **Non-Functional:** Highly available, low latency, non-guessable (optional).

### 2. Back-of-Envelope
*   Writes: 100M URLs/month.
*   Reads: 100:1 ratio -> 10B reads/month.
*   Storage: 500 bytes/URL * 100M * 12 months * 5 years = ~30TB.

### 3. API Design
*   `POST /api/v1/shorten` -> `{ longUrl: "..." }` -> Returns `{ shortUrl: "..." }`
*   `GET /{shortUrl}` -> 301 Redirect to `longUrl`.

### 4. Database Schema
*   **Table:** `Urls`
*   **Columns:** `id (PK)`, `long_url`, `short_url`, `created_at`.
*   **DB Choice:** NoSQL (DynamoDB/Cassandra) or SQL. Since we have massive reads, NoSQL is good for scaling, but SQL is fine for this scale too.

### 5. The Core Problem: Generating the Short URL
How to convert a DB ID to a 7-character string?
*   **Base62 Encoding:** [a-z, A-Z, 0-9] = 62 chars.
*   $62^7 = 3.5$ Trillion combinations. Enough for years.
*   **Algorithm:**
    1.  Generate a unique integer ID.
    2.  Convert integer to Base62.
    3.  Save to DB.

### 6. Scaling: Unique ID Generation in Distributed System
We can't use Auto-Increment in a distributed DB.
*   **Solution A: UUID.** Too long (128-bit).
*   **Solution B: KGS (Key Generation Service).** A dedicated service that pre-generates keys and stores them in a DB. Web servers fetch a batch of keys.
*   **Solution C: Twitter Snowflake.** 64-bit ID (Timestamp + MachineID + Sequence). Sortable by time.

---

## Scenario 2: Design a Rate Limiter
**Difficulty:** Medium
**Core Concept:** Algorithms & Redis

### 1. Requirements
*   Limit users to X requests per minute.
*   Return 429 Too Many Requests.

### 2. Algorithms
*   **Token Bucket:** Tokens are added to a bucket at a fixed rate. Request consumes a token. If empty, reject. (Allows bursts).
*   **Leaky Bucket:** Requests enter a queue processed at a constant rate. (Smoothes traffic).
*   **Fixed Window:** Counter resets every minute. (Issue: Burst at edges of window).
*   **Sliding Window Log:** Store timestamps of all requests. Precise but expensive memory.
*   **Sliding Window Counter:** Hybrid approach. Best for approximation and memory.

### 3. High Level Design
*   Client -> Load Balancer -> **Rate Limiter Middleware** -> API Server.
*   **Storage:** Redis (In-memory is a must for speed).
*   **Operation:**
    1.  Request comes for `User_123`.
    2.  Read counter from Redis for `User_123`.
    3.  If `count > limit`, return 429.
    4.  Else, `INCR` counter and forward request.

---

## Scenario 3: Design Instagram/Twitter Feed
**Difficulty:** Hard
**Core Concept:** Fan-out (Push vs Pull)

### 1. Requirements
*   User posts a tweet. Followers see it.
*   Scrollable timeline.

### 2. The Core Problem: How to build the feed?
*   **Pull Model (Fan-out on Load):**
    *   User visits home page.
    *   System queries: "Get all people I follow, get their recent tweets, merge and sort."
    *   *Pros:* Simple write.
    *   *Cons:* Expensive read. Slow for users with many follows.
*   **Push Model (Fan-out on Write):**
    *   User A tweets.
    *   System finds all followers of A.
    *   System inserts the tweet ID into every follower's "Feed Cache" (Redis List).
    *   User B visits home page -> Read from their pre-computed Feed Cache.
    *   *Pros:* Super fast reads.
    *   *Cons:* "The Justin Bieber Problem" (Celebrities with millions of followers). Writing to 100M queues takes time.

### 3. Hybrid Approach (The Winner)
*   **Normal Users:** Use Push Model.
*   **Celebrities:** Use Pull Model.
*   When User B loads feed: Merge content from their "Push Cache" + Query tweets from celebrities they follow.

---

## Scenario 4: Design a Chat System (WhatsApp)
**Difficulty:** Hard
**Core Concept:** Real-time communication

### 1. Protocols
*   **HTTP:** Request/Response. Not good for "server pushing message to client".
*   **Polling:** Client asks "Any new messages?" every 1s. Wasteful.
*   **Long Polling:** Client asks, Server holds connection open until message arrives. Better.
*   **WebSockets:** Bi-directional persistent connection. **(Best Choice)**.

### 2. High Level Design
*   **Chat Server:** Maintains WebSocket connections.
*   **Presence Server:** Tracks who is Online/Offline.
*   **Message Store:**
    *   *Recent messages:* Redis (for speed).
    *   *Chat History:* Cassandra/HBase (Write heavy, infinite scale).

### 3. Flow (User A sends to User B)
1.  User A sends message to Chat Server 1 (via WebSocket).
2.  Server 1 saves to DB.
3.  Server 1 asks Redis: "Which server is User B connected to?" -> "Server 2".
4.  Server 1 forwards message to Server 2.
5.  Server 2 pushes message to User B (via WebSocket).

---
**Next Step:** Read `04-Advanced-Topics.md` for the Staff-level differentiators.

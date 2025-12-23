# 02. Building Blocks: The Lego Bricks

Every complex system is built using these standard components. Your job as an architect is to know which block to pick and how to connect them.

## 1. Load Balancers (LB)
Distributes incoming traffic across multiple servers to ensure no single server is overwhelmed.

### Where to place them?
1.  Between User and Web Server.
2.  Between Web Server and App Server.
3.  Between App Server and Database.

### Algorithms:
*   **Round Robin:** Sequential (A, B, C, A, B, C...). Simple, but doesn't account for server load.
*   **Least Connections:** Sends traffic to the server with the fewest active connections. Good for long sessions (e.g., Chat).
*   **IP Hash:** Hashes the client IP to ensure a user always connects to the same server (Sticky Sessions).

### L4 vs L7 Load Balancing:
*   **L4 (Transport Layer):** Routing based on IP and Port. Fast, simple. (e.g., TCP/UDP level).
*   **L7 (Application Layer):** Routing based on URL, Headers, Cookies. Smarter but slower. (e.g., `/api/video` goes to Video Service, `/api/chat` goes to Chat Service).

---

## 2. Caching
Storing frequently accessed data in fast memory (RAM) to reduce database load.

### Where to cache?
*   **Client Side:** Browser cache.
*   **CDN:** Static content (Images, CSS, JS).
*   **Server Side:** Redis/Memcached.

### Caching Strategies (Crucial for Interviews):
1.  **Cache Aside (Lazy Loading):**
    *   App checks Cache.
    *   If miss, App reads DB.
    *   App writes to Cache.
    *   *Pros:* Only requested data is cached. Resilient to cache failure.
    *   *Cons:* First request is slow (Cache Miss). Data can become stale.
2.  **Write Through:**
    *   App writes to Cache and DB at the same time.
    *   *Pros:* Data is always consistent.
    *   *Cons:* Slow writes (2 network calls).
3.  **Write Back (Write Behind):**
    *   App writes ONLY to Cache.
    *   Cache asynchronously writes to DB later.
    *   *Pros:* Super fast writes.
    *   *Cons:* Data loss risk if Cache crashes before syncing to DB.

### Eviction Policies:
*   **LRU (Least Recently Used):** Remove the item that hasn't been used for the longest time. (Most common).
*   **LFU (Least Frequently Used):** Remove the item used the fewest times.

---

## 3. Databases: SQL vs. NoSQL
The most common interview question: "Which DB should we use?"

### SQL (Relational - MySQL, PostgreSQL)
*   **Structure:** Structured data, predefined schema.
*   **Joins:** Powerful JOIN operations.
*   **Transactions:** ACID compliance.
*   **Use Case:** Financial systems, E-commerce (Orders/Payments), User Auth.
*   **Scaling:** Vertical is easy, Horizontal is hard.

### NoSQL (Non-Relational)
1.  **Key-Value (Redis, DynamoDB):**
    *   *Use Case:* Caching, Sessions, Shopping Cart.
2.  **Document (MongoDB, CouchDB):**
    *   *Use Case:* CMS, Catalogs, User Profiles (Flexible schema).
3.  **Columnar (Cassandra, HBase):**
    *   *Use Case:* Big Data, Analytics, Time-series data (Write heavy).
4.  **Graph (Neo4j):**
    *   *Use Case:* Social Networks (Friends of Friends), Recommendation Engines.

### Decision Framework:
*   Need ACID? -> **SQL**
*   Complex Relationships (Joins)? -> **SQL**
*   Unstructured data? -> **NoSQL (Document)**
*   Massive Write volume? -> **NoSQL (Cassandra/DynamoDB)**
*   Ultra-low latency? -> **NoSQL (Redis)**

---

## 4. Message Queues (Asynchronous Processing)
Decoupling components. If Service A sends a message to Service B, A shouldn't wait for B to finish.

### Models:
1.  **Point-to-Point (Queue):** One consumer gets the message. (e.g., Job processing).
2.  **Pub-Sub (Topic):** All subscribers get the message. (e.g., Notification system).

### Kafka vs. RabbitMQ:
*   **RabbitMQ:**
    *   Traditional Message Queue.
    *   Smart Broker, Dumb Consumer.
    *   Messages are deleted after consumption.
    *   *Use Case:* Complex routing, Task processing.
*   **Kafka:**
    *   Distributed Streaming Platform.
    *   Dumb Broker, Smart Consumer.
    *   Messages are persisted (Log) for a retention period.
    *   *Use Case:* High throughput, Event sourcing, Log aggregation, Analytics.

---
**Next Step:** Read `03-System-Design-Scenarios.md` to see how to combine these blocks to build Uber, Twitter, etc.

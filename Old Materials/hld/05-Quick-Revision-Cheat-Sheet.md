# 05. Quick Revision Cheat Sheet

Read this 30 minutes before your interview.

## 1. Back-of-the-Envelope Math
*   **1 Day** = 86,400 seconds (~10^5).
*   **1 Million requests/day** = ~12 requests/sec.
*   **Char** = 2 bytes (Java).
*   **Long** = 8 bytes.
*   **UUID** = 16 bytes.

### Latency Numbers (Approx)
*   **L1 Cache:** 0.5 ns
*   **RAM:** 100 ns
*   **SSD Read:** 100 us (microseconds)
*   **Network (Same Data Center):** 500 us
*   **HDD Read:** 10 ms (milliseconds) -> *Avoid HDDs for random reads!*
*   **Network (Cross Region):** 150 ms

## 2. Decision Trees

### Database
*   **Structured + ACID?** -> SQL (PostgreSQL/MySQL).
*   **Unstructured + Read Heavy?** -> MongoDB.
*   **Key-Value + High Speed?** -> Redis.
*   **Massive Writes + Time Series?** -> Cassandra/HBase.
*   **Search?** -> Elasticsearch.
*   **Blob/Files?** -> S3.

### Communication
*   **Internal Microservices?** -> gRPC (Fast, binary) or REST (Simple).
*   **Client to Server?** -> REST or GraphQL (Flexible).
*   **Real-time?** -> WebSockets.

## 3. The "Magic" Spells (Keywords to drop)
*   **"Thundering Herd":** When many clients retry at once. *Fix:* Exponential Backoff + Jitter.
*   **"Hot Partition":** When one shard gets too much traffic (e.g., Justin Bieber). *Fix:* Add random suffix to key to spread load.
*   **"Bloom Filter":** Probabilistic data structure to quickly check if item exists. *Use:* To avoid hitting DB for non-existent keys.
*   **"Consistent Hashing":** For distributing keys across servers with minimal movement.
*   **"Idempotency":** Making sure retrying a request doesn't duplicate the action (e.g., Payment). *Fix:* Use a unique `idempotency_key`.
*   **"Backpressure":** When consumer is slow, tell producer to slow down.
*   **"Saga Pattern":** Managing distributed transactions via compensating actions.
*   **"Circuit Breaker":** Stopping requests to a failing service to prevent cascading failure.
*   **"HyperLogLog":** Probabilistic counting of unique items with minimal memory.
*   **"Golden Signals":** Latency, Traffic, Errors, Saturation (The 4 pillars of monitoring).
*   **"Idempotency Key":** Ensuring a request is processed exactly once even if retried.

## 4. System Design Template (Staff Level)
1.  **Requirements:** Functional + Non-Functional (Scale, Latency, Availability).
2.  **Estimations:** QPS, Storage, Bandwidth.
3.  **API Design:** Contract, Versioning, Idempotency.
4.  **Data Model:** Schema, SQL vs NoSQL, Sharding strategy.
5.  **HLD Diagram:** Client -> CDN -> LB -> API Gateway -> Service -> Cache -> DB.
6.  **Deep Dive:** 
    - **Scalability:** How to handle 10x traffic?
    - **Reliability:** Failure scenarios & Disaster Recovery.
    - **Observability:** Monitoring, Alerting, Tracing.
7.  **Production Readiness:** Cost, Security, Rollout (Canary/Blue-Green).

## 5. Common Bottlenecks & Fixes
*   **DB CPU High?** -> Add Read Replicas or Cache.
*   **Latency High?** -> Use CDN, check N+1 queries.
*   **Single Point of Failure?** -> Add redundancy (Master-Slave).
*   **Too much data?** -> Shard/Partition.

---
**Good Luck! You got this.**

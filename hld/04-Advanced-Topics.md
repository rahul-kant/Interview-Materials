# 04. Advanced Topics: The Staff Engineer Level

These topics separate "Senior" from "Staff". They show you understand the complexities of distributed systems beyond just "putting a load balancer in front".

## 1. Distributed Transactions
How do you ensure data consistency across multiple microservices? (e.g., Order Service and Payment Service).

### The Problem
You can't use a simple database transaction (ACID) because the data lives in two different DBs.

### Solution A: Two-Phase Commit (2PC)
*   **Coordinator** asks all services: "Can you commit?" (Prepare Phase).
*   If everyone says "Yes", Coordinator says: "Commit!" (Commit Phase).
*   **Pros:** Strong consistency.
*   **Cons:** Blocking. If Coordinator dies, everyone waits. Slow. **Avoid in high-scale systems.**

### Solution B: SAGA Pattern (The Standard)
*   A sequence of local transactions. Each service updates its own DB and publishes an event.
*   **Choreography:** Service A emits event -> Service B listens and acts -> Service C listens and acts.
*   **Orchestration:** A central "Orchestrator" service tells A, then B, then C what to do.
*   **Compensation:** If Service B fails, you must run a "Compensating Transaction" to undo what Service A did (e.g., Refund the money).
*   **Pros:** Non-blocking, scalable.
*   **Cons:** Complex to debug. Eventual consistency.

---

## 2. Consensus Algorithms
How do distributed nodes agree on a single value (e.g., "Who is the Master node?")?

### Paxos & Raft
*   **Concept:** A family of protocols to solve consensus in a network of unreliable processors.
*   **Raft:** Easier to understand than Paxos. Used by **Etcd, Consul, Kubernetes**.
*   **Leader Election:** Nodes vote for a leader. The leader handles all writes and replicates to followers.
*   **Log Replication:** The leader ensures all followers have the same log of events.
*   **Interview Tip:** You don't need to implement Raft code. Just know *why* it's used: "To maintain a consistent state across a cluster, like in Etcd for Kubernetes config."

---

## 3. Service Discovery
In a microservices world (Kubernetes), IP addresses change dynamically. How does Service A find Service B?

### Client-Side Discovery
*   Service A queries the Service Registry (e.g., Eureka, Consul) to get B's IP.
*   Service A calls B directly.
*   *Pros:* Less hops.
*   *Cons:* Client needs to know discovery logic.

### Server-Side Discovery (Load Balancer)
*   Service A calls a Load Balancer (e.g., Nginx, AWS ALB).
*   LB queries the Service Registry and forwards traffic to B.
*   *Pros:* Simple client.
*   *Cons:* LB is a bottleneck/extra hop.

---

## 4. Observability
"How do you know your system is broken before the users do?"

### The Three Pillars:
1.  **Metrics (Aggregatable):**
    *   "What is happening?"
    *   Examples: CPU usage, Request Count, Error Rate, Latency (p99).
    *   *Tools:* Prometheus, Grafana, Datadog.
2.  **Logging (Events):**
    *   "Why is it happening?"
    *   Detailed record of events.
    *   *Tools:* ELK Stack (Elasticsearch, Logstash, Kibana), Splunk.
3.  **Tracing (Context):**
    *   "Where is it happening?"
    *   Follow a single request across 10 microservices.
    *   *Tools:* Jaeger, Zipkin.

### Health Checks
*   **Liveness Probe:** "Is the container running?" (If no, restart it).
*   **Readiness Probe:** "Is the app ready to take traffic?" (e.g., DB connection established). (If no, don't send traffic).

---

## 5. Cost Analysis & Optimization (FinOps)
At the Staff level, you must consider the financial impact of your architectural choices.
- **Compute:** On-demand vs. Reserved vs. Spot instances.
- **Storage:** Tiered storage (S3 Standard vs. Glacier).
- **Data Transfer:** Egress costs are the "hidden killer" in cloud bills.
- **Optimization:** Using probabilistic data structures (HyperLogLog, Bloom Filters) to save memory and compute.

---

## 6. Security & Compliance
"How do you protect user data and stay legal?"
- **Data Protection:** Encryption at rest (AES-256) and in transit (TLS 1.3).
- **Authentication/Authorization:** OAuth2, JWT, and Role-Based Access Control (RBAC).
- **Compliance:** 
    - **PCI-DSS:** For payment data (Tokenization).
    - **GDPR/CCPA:** Right to be forgotten, data residency.
    - **SOC2:** Operational security and privacy.

---

## 7. Testing & Rollout Strategies
"How do you deploy changes without breaking the world?"
- **Testing:**
    - **Chaos Engineering:** Intentionally breaking things (Netflix Chaos Monkey) to test resilience.
    - **Jepsen Testing:** Testing distributed consistency under network partitions.
- **Rollout:**
    - **Blue-Green:** Two identical environments; flip the switch.
    - **Canary:** Deploy to 1% of users first.
    - **Feature Flags:** Decouple deployment from release.

---

## 8. Capacity Planning
"How do you know when to buy more servers?"
- **Scaling Triggers:** CPU > 70%, Memory > 80%, or Queue Lag > 5 minutes.
- **Throughput Projections:** Calculating peak load (e.g., Black Friday) vs. average load.
- **Lead Time:** How long does it take to provision new resources?

---

**Next Step:** Read `05-Quick-Revision-Cheat-Sheet.md` for your last-minute prep.

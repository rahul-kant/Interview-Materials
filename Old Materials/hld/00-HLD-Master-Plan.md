# 00. HLD Master Plan & Roadmap

**Target Audience:** Java Tech Lead / Architect (14+ YOE)
**Goal:** Crack FAANG/Tier-1 System Design Interviews (L6/L7 - Staff/Principal level)

## 1. The Mindset Shift (Senior vs. Staff/Principal)
At 14+ years of experience, you are not just expected to "design a system that works." You are expected to demonstrate **Production Readiness**:
*   **Drive the requirements:** Don't wait for the interviewer to tell you everything. Ask clarifying questions about scale, geography, and compliance.
*   **Own the Trade-offs:** Every decision (SQL vs NoSQL, Consistency vs Availability) has a cost. You must articulate *why* you chose one over the other.
*   **Operational Excellence:** Discuss Monitoring, Alerting, and Disaster Recovery as first-class citizens.
*   **Cost & Security:** Show you understand the financial and legal implications of your architecture.
*   **Breadth then Depth:** Cover the high-level flow quickly (10-15 mins), then deep dive into the hardest parts (bottlenecks).

## 2. The 4-Week Preparation Roadmap

### Week 1: The Fundamentals (The "Why" and "How")
*   **Focus:** Scalability, Performance, CAP Theorem, Consistency Patterns.
*   **Action:** Read `01-Core-Concepts.md`.
*   **Goal:** Be able to explain *why* a system needs to scale and *how* it breaks when it does.

### Week 2: The Building Blocks (The "Lego Bricks")
*   **Focus:** Load Balancers, Caching, Databases, Queues, CDNs.
*   **Action:** Read `02-Building-Blocks.md`.
*   **Goal:** Know the internal working of these components. E.g., How does a Load Balancer know a server is down? (Health checks). How does Redis evict keys? (LRU/LFU).

### Week 3: System Design Scenarios (The "Blueprints")
*   **Focus:** Standard problems (URL Shortener, Rate Limiter, Chat, Uber, Twitter).
*   **Action:** Read `03-System-Design-Scenarios.md`.
*   **Goal:** Practice the "Flow" of the interview.
    1.  Requirements (Functional/Non-Functional).
    2.  Back-of-envelope estimation.
    3.  API Design.
    4.  DB Schema.
    5.  High-Level Design (Diagram).
    6.  Deep Dive & Scaling.

### Week 4: Advanced Topics & Mock Interviews
*   **Focus:** Distributed Transactions, Consensus, AI Systems, Mock Interviews.
*   **Action:** Read `04-Advanced-Topics.md` and `05-Quick-Revision-Cheat-Sheet.md`.
*   **Goal:** Polish your delivery. Do at least 3-5 mock interviews (peer-to-peer or paid).

## 3. The Standard Interview Framework (45 Minutes)

| Time | Section | What to do |
| :--- | :--- | :--- |
| **0-5 mins** | **Requirements & Scope** | Ask questions! "Is this a global app?", "DAU/MAU?", "Read heavy or Write heavy?" |
| **5-10 mins** | **Estimations & Constraints** | Calculate QPS (Queries Per Second) and Storage needs. This decides your DB and Sharding strategy. |
| **10-15 mins** | **High-Level Design (HLD)** | Draw the "Boxes and Arrows". Client -> LB -> Service -> DB. Keep it simple initially. |
| **15-20 mins** | **API & Data Model** | Define REST/gRPC endpoints. Define Tables/Collections. Choose SQL vs NoSQL here. |
| **20-35 mins** | **Deep Dive** | The core of the interview. Pick the hardest component (e.g., "How to generate unique IDs at scale?" or "How to handle the Thundering Herd problem?"). |
| **35-40 mins** | **Wrap up & Bottlenecks** | Identify single points of failure. Discuss monitoring/alerting. |
| **40-45 mins** | **Q&A** | Ask about their engineering culture. |

---
**Next Step:** Start with `01-Core-Concepts.md` to build your theoretical foundation.

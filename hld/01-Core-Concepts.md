# 01. Core Concepts: The Foundation

Before designing complex systems, you must understand the fundamental laws that govern them.

## 1. Vertical vs. Horizontal Scaling

### Vertical Scaling (Scale Up)
*   **Concept:** Adding more power (CPU, RAM) to your existing machine.
*   **Analogy:** Buying a bigger backpack to carry more books.
*   **Pros:** Simple. No code changes required.
*   **Cons:** Hard limit (hardware ceiling). Single point of failure. Expensive.
*   **When to use:** Small to medium applications, MVP phase.

### Horizontal Scaling (Scale Out)
*   **Concept:** Adding more machines (nodes) to your pool of resources.
*   **Analogy:** Buying more backpacks and hiring more people to carry them.
*   **Pros:** Infinite scaling (theoretically). Fault tolerance (if one node dies, others take over). Cheaper (commodity hardware).
*   **Cons:** Complex. Requires load balancing, data partitioning, and handling distributed consistency.
*   **When to use:** Large scale systems (Google, Facebook, Uber).

---

## 2. The CAP Theorem
In a distributed system, you can only have **two** of the following three guarantees:

1.  **Consistency (C):** Every read receives the most recent write or an error. (All nodes see the same data at the same time).
2.  **Availability (A):** Every request receives a (non-error) response, without the guarantee that it contains the most recent write.
3.  **Partition Tolerance (P):** The system continues to operate despite an arbitrary number of messages being dropped or delayed by the network between nodes.

**The Reality:** In a distributed system (like Cloud), network failures (**P**) are inevitable. So you essentially have to choose between **CP** or **AP**.

*   **CP (Consistency + Partition Tolerance):** If a partition happens, the system stops accepting writes to ensure data doesn't diverge.
    *   *Example:* Banking systems (ATM). You'd rather show "System Down" than show the wrong balance.
    *   *Tech:* HBase, MongoDB (default), Redis (default).
*   **AP (Availability + Partition Tolerance):** If a partition happens, the system keeps accepting writes, even if nodes are out of sync. They will sync up later (Eventual Consistency).
    *   *Example:* Social Media feeds (Facebook/Twitter). It's okay if you see a post 5 seconds later than your friend.
    *   *Tech:* Cassandra, DynamoDB, Couchbase.

---

## 3. ACID vs. BASE

### ACID (Relational Databases - MySQL, PostgreSQL)
*   **Atomicity:** All or nothing.
*   **Consistency:** Data is valid before and after transaction.
*   **Isolation:** Transactions don't interfere with each other.
*   **Durability:** Once committed, it stays committed (even after power loss).
*   **Focus:** Strong Consistency.

### BASE (NoSQL Databases - Cassandra, MongoDB)
*   **Basically Available:** The system guarantees availability.
*   **Soft state:** The state of the system may change over time, even without input (due to eventual consistency).
*   **Eventual consistency:** The system will become consistent over time.
*   **Focus:** High Availability.

---

## 4. Data Partitioning (Sharding)
When your dataset is too large for a single server, you split it across multiple servers.

### Strategies:
1.  **Vertical Partitioning:** Splitting by feature. (e.g., User table on Server A, Photos table on Server B).
    *   *Issue:* What if the User table grows too big?
2.  **Horizontal Partitioning (Sharding):** Splitting rows. (e.g., Users ID 1-1000 on Server A, 1001-2000 on Server B).
    *   *Key Challenge:* How do you decide which server holds the data?

### The Problem with Simple Hashing: `hash(ID) % N`
If you have `N=4` servers, and you add one (`N=5`), the remainder changes for almost ALL keys. This requires moving massive amounts of data (Resharding).

### The Solution: Consistent Hashing
*   **Concept:** Imagine a ring (0 to 360 degrees).
*   **Placement:** Place servers on the ring using `hash(ServerIP)`.
*   **Data:** Place data on the ring using `hash(Key)`.
*   **Lookup:** To find data, go clockwise on the ring until you hit a server.
*   **Benefit:** When you add/remove a server, only the data between that server and its neighbor is affected. Minimal data movement.
*   **Used by:** Cassandra, DynamoDB, Discord.

---

## 5. Replication
Keeping copies of data for redundancy and performance.

1.  **Master-Slave (Primary-Secondary):**
    *   Writes go to Master.
    *   Reads go to Slaves.
    *   *Pros:* Read scalability.
    *   *Cons:* Master is a single point of failure for writes. Replication lag (Slaves might be slightly behind).
2.  **Master-Master (Multi-Leader):**
    *   Writes can go to any Master.
    *   *Pros:* High write availability.
    *   *Cons:* Write conflicts (What if two users update the same record on different masters at the same time?).
3.  **Leaderless (Peer-to-Peer):**
    *   Writes go to all/quorum of nodes. (e.g., Cassandra).
    *   *Concept:* Quorum `R + W > N`. (Read nodes + Write nodes > Total nodes).

---
**Next Step:** Read `02-Building-Blocks.md` to learn about the components we use to implement these concepts.

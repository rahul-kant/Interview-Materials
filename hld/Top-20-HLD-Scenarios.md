# Top 20 HLD Interview Scenarios

This section contains detailed, step-by-step guides for the 20 most important System Design questions. Each guide is structured to fit a **45-minute interview slot** and includes beginner-friendly explanations, real-world analogies, and professional diagrams.

## 🎯 How to Use This Guide

- **For Beginners:** Start with scenarios 1-5, read the analogies and ELI5 sections first
- **For Interview Prep:** Focus on scenarios relevant to your target company/role
- **For Learning:** Work through all 20 systematically, one per day
- **For Review:** Use the diagrams and key concepts for quick revision
- **🚀 For Staff/Senior Prep:** Every scenario now includes **10 Production-Ready Sections** (Failure Scenarios, Monitoring, Cost, Security, etc.) to help you demonstrate "Staff Engineer" level depth.

---

## 🌟 New: Production-Ready Enhancements
Each of the 20 scenarios has been upgraded with a comprehensive deep dive into real-world production concerns. Look for these sections at the end of every guide:

1.  **Failure Scenarios & Disaster Recovery:** What happens when things break?
2.  **Monitoring & Observability:** Golden signals, dashboards, and alerts.
3.  **API Design & Versioning:** Professional contract design.
4.  **Cost Analysis & Optimization:** Cloud infrastructure cost estimations.
5.  **Security & Compliance:** PCI-DSS, GDPR, and data protection.
6.  **Testing Strategies:** From Unit tests to Chaos Engineering.
7.  **Migration & Rollout:** Zero-downtime deployment strategies.
8.  **Performance Optimization:** Latency and throughput tuning.
9.  **Capacity Planning:** Scaling triggers and throughput projections.
10. **Interview Cheat Sheet:** A 1-minute summary of key numbers and trade-offs.

---

## 📋 The Complete List

### Basic Systems (Scenarios 1-10)

1.  **[Design a URL Shortener (TinyURL)](./Top-20-Scenarios/01-URL-Shortener.md)**
    *   *Key Concepts:* Unique ID Generation, Base62 Encoding, Key Generation Service
    *   *Analogy:* Coat check at restaurant

2.  **[Design a Rate Limiter](./Top-20-Scenarios/02-Rate-Limiter.md)**
    *   *Key Concepts:* Token Bucket Algorithm, Redis Lua Scripts, Distributed Counting
    *   *Analogy:* Coffee shop punch card

3.  **[Design a Social Media Feed (Twitter/Instagram)](./Top-20-Scenarios/03-Social-Media-Feed.md)**
    *   *Key Concepts:* Fan-out on Write vs Read, Hybrid Architecture, Caching
    *   *Analogy:* Newspaper delivery system

4.  **[Design a Chat Application (WhatsApp)](./Top-20-Scenarios/04-Chat-App.md)**
    *   *Key Concepts:* WebSockets, Message Consistency, Presence Service
    *   *Analogy:* Phone call vs traditional mail

5.  **[Design Video Streaming (YouTube/Netflix)](./Top-20-Scenarios/05-Video-Streaming.md)**
    *   *Key Concepts:* CDN, Adaptive Bitrate Streaming (HLS/DASH), Transcoding
    *   *Analogy:* Highway delivery system

6.  **[Design a Ride-Hailing Service (Uber)](./Top-20-Scenarios/06-Ride-Hailing.md)**
    *   *Key Concepts:* Geospatial Indexing (Geohash/QuadTree), Real-time Matching
    *   *Analogy:* Postal code system

7.  **[Design Typeahead (Google Search)](./Top-20-Scenarios/07-Typeahead.md)**
    *   *Key Concepts:* Trie Data Structure, Prefix Caching, Debouncing
    *   *Analogy:* Filing cabinet

8.  **[Design a Web Crawler (Google Bot)](./Top-20-Scenarios/08-Web-Crawler.md)**
    *   *Key Concepts:* URL Frontier, Politeness, Bloom Filters, Deduplication
    *   *Analogy:* Library exploration

9.  **[Design File Storage (Google Drive)](./Top-20-Scenarios/09-File-Storage.md)**
    *   *Key Concepts:* Block Storage, Chunking, Delta Sync, Metadata Separation
    *   *Analogy:* Pizza delivery

10. **[Design a Notification System](./Top-20-Scenarios/10-Notification-System.md)**
    *   *Key Concepts:* Message Queues, Retry Mechanisms, Pluggable Providers
    *   *Analogy:* Post office

### Advanced Systems (Scenarios 11-20)

11. **[Design a Distributed Key-Value Store (DynamoDB)](./Top-20-Scenarios/11-Distributed-KV-Store.md)**
    *   *Key Concepts:* Consistent Hashing, Tunable Consistency (Quorum), Merkle Trees, Gossip Protocol
    *   *Analogy:* Circular parking lot

12. **[Design a Payment System (Stripe)](./Top-20-Scenarios/12-Payment-System.md)**
    *   *Key Concepts:* ACID Transactions, Idempotency, Double-Entry Ledger, Reconciliation
    *   *Analogy:* Checkbook

13. **[Design a Metrics/Monitoring System (Datadog)](./Top-20-Scenarios/13-Metrics-System.md)**
    *   *Key Concepts:* Time-Series Database, Push vs Pull, Downsampling, Alerting
    *   *Analogy:* Book summary

14. **[Design a Distributed Job Scheduler (Cron)](./Top-20-Scenarios/14-Job-Scheduler.md)**
    *   *Key Concepts:* Leader Election, Hierarchical Timing Wheel, Partitioning
    *   *Analogy:* Kitchen timer

15. **[Design a Real-time Leaderboard (Gaming)](./Top-20-Scenarios/15-Leaderboard.md)**
    *   *Key Concepts:* Redis Sorted Sets (Skip List), Scatter-Gather Pattern
    *   *Analogy:* Classroom scoreboard

16. **[Design a Collaborative Editor (Google Docs)](./Top-20-Scenarios/16-Google-Docs.md)**
    *   *Key Concepts:* Operational Transformation (OT), CRDTs, WebSockets
    *   *Analogy:* Shared grocery list

17. **[Design a Ticket Booking System (Ticketmaster)](./Top-20-Scenarios/17-Ticket-Booking.md)**
    *   *Key Concepts:* Virtual Waiting Room, Optimistic Locking, Redis Distributed Locks
    *   *Analogy:* Deli counter

18. **[Design an Ad Click Aggregator](./Top-20-Scenarios/18-Ad-Aggregator.md)**
    *   *Key Concepts:* Streaming Windows, Watermarks, Exact-Once Processing (Kappa Architecture)
    *   *Analogy:* Counting cars on highway

19. **[Design a Hotel Booking System (Airbnb)](./Top-20-Scenarios/19-Hotel-Booking.md)**
    *   *Key Concepts:* Elasticsearch Integration, Availability Management, Geosharding
    *   *Analogy:* Library catalog

20. **[Design a Distributed Search Engine (Elasticsearch)](./Top-20-Scenarios/20-Search-Engine.md)**
    *   *Key Concepts:* Inverted Index, Sharding strategies, TF-IDF Scoring
    *   *Analogy:* Book index

---

## 💡 Study Recommendations

### For General Backend Roles
Focus on: **1, 2, 3, 4, 5, 6, 11, 12, 13**

### For Staff/Senior Roles
Study all 20, with emphasis on: **11-20** (advanced distributed systems)

### For Specific Domains
- **FinTech:** 12 (Payment), 11 (KV Store), 13 (Metrics)
- **Gaming:** 15 (Leaderboard), 4 (Chat), 13 (Metrics)
- **E-commerce:** 17 (Ticket Booking), 19 (Hotel Booking), 12 (Payment)
- **Social Media:** 3 (Feed), 4 (Chat), 6 (Geospatial)
- **Search/Data:** 20 (Search Engine), 18 (Ad Aggregator), 13 (Metrics)

---

## 📚 What Makes These Guides Unique

- **🎯 Real-World Analogies:** Every scenario starts with a relatable comparison
- **📚 ELI5 Explanations:** Complex concepts explained simply
- **📝 Step-by-Step Walkthroughs:** Detailed process breakdowns
- **🤔 Common Misconceptions:** Typical mistakes addressed
- **💡 Industry Examples:** Real companies and their implementations
- **📊 Professional Diagrams:** 22 architecture diagrams included
- **✅ Interview-Ready:** Structured for 45-minute interview slots

---

**How to Practice:**
1. Read one scenario per day
2. Try to draw the diagram on a whiteboard without looking
3. Explain the "Deep Dive" section out loud
4. If you can explain it clearly, you're ready for that question!

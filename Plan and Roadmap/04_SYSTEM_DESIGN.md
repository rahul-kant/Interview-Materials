# System Design - Complete Guide

## Overview
System design is critical for senior/staff engineer positions. This guide covers fundamental concepts, patterns, and practice problems tailored for FAANG interviews.

---

## System Design Interview Framework

### Interview Structure (45-60 minutes)

1. **Requirements Clarification (5 mins)**
2. **Back-of-envelope Estimation (5 mins)**
3. **System Interface/API Design (5 mins)**
4. **High-Level Design (10 mins)**
5. **Deep Dive into Components (20 mins)**
6. **Trade-offs & Bottlenecks (10 mins)**
7. **Questions (5 mins)**

---

## 1. Requirements Clarification

### Questions to Ask

#### Functional Requirements
- What are the core features?
- Who are the users?
- What are the main use cases?
- Any specific workflows?

#### Non-Functional Requirements
- **Scale**: How many users? DAU/MAU?
- **Performance**: Latency requirements? (< 200ms?)
- **Availability**: 99.9%? 99.99%?
- **Consistency**: Strong or eventual consistency?
- **Durability**: Data loss acceptable?

#### Out of Scope
- Analytics/reporting?
- Admin features?
- Authentication/authorization?

### Example for URL Shortener:
```
Functional:
✓ Generate short URL from long URL
✓ Redirect short URL to original URL
✓ Custom short URLs (optional)
✓ URL expiration (optional)

Non-Functional:
✓ 100M URLs generated per month
✓ Read-heavy (100:1 read-to-write ratio)
✓ Low latency (< 200ms)
✓ High availability (99.9%)
✓ URLs never expire (simplification)
```

---

## 2. Back-of-Envelope Estimation

### Key Metrics to Calculate

#### Traffic Estimates
```
- DAU (Daily Active Users)
- Requests per second (RPS)
- Peak traffic multiplier (2-3x)
```

#### Storage Estimates
```
- Size per record
- Total records
- Storage needed for X years
- Growth rate
```

#### Bandwidth Estimates
```
- Incoming data rate
- Outgoing data rate
- Network bandwidth needed
```

#### Memory/Cache Estimates
```
- Cache size (80-20 rule)
- Cache hit ratio
```

### Example Calculation for URL Shortener:

```
Assumptions:
- 100M new URLs per month
- Read:Write ratio = 100:1
- URL stored for 10 years

Traffic:
- Writes: 100M / (30 days * 24 hrs * 3600 sec) ≈ 40 URLs/sec
- Reads: 40 * 100 = 4000 reads/sec

Storage:
- Size per URL: ~500 bytes (URL + metadata)
- 100M URLs/month * 12 months * 10 years = 12B URLs
- 12B * 500 bytes = 6 TB

Bandwidth:
- Write: 40 URLs/sec * 500 bytes = 20 KB/sec
- Read: 4000 URLs/sec * 500 bytes = 2 MB/sec

Cache:
- 20% of URLs generate 80% of traffic
- Cache 20% of daily traffic
- 4000 req/sec * 86400 sec * 0.2 ≈ 70M requests
- 70M * 500 bytes ≈ 35 GB cache memory
```

---

## 3. Core System Design Concepts

### 3.1 Load Balancing

**Purpose**: Distribute traffic across multiple servers

**Types:**
- **L4 (Transport Layer)**: Based on IP/Port
- **L7 (Application Layer)**: Based on content (HTTP headers, cookies)

**Algorithms:**
- Round Robin
- Least Connections
- Weighted Round Robin
- IP Hash
- Least Response Time

**Tools**: Nginx, HAProxy, AWS ELB, Google Cloud Load Balancer

**When to Use:**
- Multiple application servers
- Need horizontal scaling
- High availability required

---

### 3.2 Caching

**Purpose**: Reduce latency and database load

**Cache Strategies:**

#### Write-Through Cache
```
Write → Cache → Database
- Data always in sync
- Higher write latency
- Use: Financial transactions
```

#### Write-Back (Write-Behind) Cache
```
Write → Cache → (Async) → Database
- Low write latency
- Risk of data loss
- Use: Logging, analytics
```

#### Write-Around Cache
```
Write → Database, Read → Cache if miss
- Prevents cache pollution
- Recent writes have cache miss
- Use: Infrequent reads
```

#### Cache-Aside (Lazy Loading)
```
Read → Cache miss → Database → Update cache
- Most common pattern
- Use: Read-heavy applications
```

**Eviction Policies:**
- **LRU** (Least Recently Used) - Most common
- **LFU** (Least Frequently Used)
- **FIFO** (First In First Out)
- **TTL** (Time To Live)

**Cache Levels:**
- **CDN** (Content Delivery Network) - Static content
- **Application Cache** - Redis, Memcached
- **Database Cache** - Query cache
- **CPU Cache** - L1, L2, L3

**Tools**: Redis, Memcached, Varnish, CloudFront (CDN)

---

### 3.3 Database Design

#### SQL vs NoSQL

**SQL (Relational)**
- **Use When**: ACID required, complex queries, joins
- **Examples**: PostgreSQL, MySQL
- **Scaling**: Vertical (expensive), Sharding (complex)

**NoSQL**
- **Document**: MongoDB, CouchDB (flexible schema)
- **Key-Value**: Redis, DynamoDB (fast, simple)
- **Wide-Column**: Cassandra, HBase (time-series, IoT)
- **Graph**: Neo4j (relationships, social networks)

#### Database Scaling Patterns

**1. Replication**
```
Master-Slave:
- Master: Write operations
- Slaves: Read operations
- Use: Read-heavy applications

Master-Master:
- Both handle writes
- Conflict resolution needed
- Use: High availability
```

**2. Sharding (Horizontal Partitioning)**
```
Split data across multiple databases

Strategies:
- Hash-based: hash(key) % N
- Range-based: user_id 1-1M → Shard1
- Geography-based: US → Shard1, EU → Shard2

Challenges:
- Cross-shard queries
- Resharding complexity
- Uneven distribution (hotspots)
```

**3. Partitioning (Vertical)**
```
Split by columns/tables
- User profile → DB1
- User posts → DB2
- User messages → DB3
```

#### Indexing
```
B-Tree Index: Default, range queries
Hash Index: Exact match, fast
Full-Text Index: Search engines
Geospatial Index: Location-based queries

Trade-off: Read speed ↑, Write speed ↓, Storage ↑
```

---

### 3.4 CAP Theorem

**Choose 2 of 3:**

- **C (Consistency)**: All nodes see same data
- **A (Availability)**: System always responds
- **P (Partition Tolerance)**: Works despite network failures

**Real Systems:**
- **CA**: Traditional RDBMS (single node)
- **CP**: MongoDB, HBase, Redis (consistency over availability)
- **AP**: Cassandra, DynamoDB, Couchbase (availability over consistency)

**PACELC Theorem:**
```
If Partition: Choose between Availability and Consistency
Else: Choose between Latency and Consistency

Examples:
- DynamoDB: PA/EL (Available, Low latency)
- MongoDB: PC/EC (Consistent, Consistent)
```

---

### 3.5 Message Queues

**Purpose**: Asynchronous communication, decoupling, buffering

**Use Cases:**
- Email notifications
- Image/video processing
- Order processing
- Log aggregation

**Patterns:**

#### Publish-Subscribe
```
Producer → Topic → Multiple Consumers
- All consumers get all messages
- Use: Notifications, event broadcasting
```

#### Point-to-Point (Queue)
```
Producer → Queue → Single Consumer
- Message consumed once
- Use: Task processing, job queue
```

**Tools:**
- **RabbitMQ**: Traditional message broker, AMQP
- **Apache Kafka**: High throughput, event streaming
- **AWS SQS**: Managed queue service
- **Redis Pub/Sub**: Lightweight, in-memory

**Key Concepts:**
- **At-most-once**: May lose messages
- **At-least-once**: May duplicate messages
- **Exactly-once**: No loss, no duplication (hard to achieve)

---

### 3.6 Microservices Architecture

**Benefits:**
- Independent deployment
- Technology diversity
- Fault isolation
- Easier scaling

**Challenges:**
- Distributed system complexity
- Network latency
- Data consistency
- Service discovery
- Monitoring complexity

**Key Patterns:**

#### API Gateway
```
Client → API Gateway → Microservices
- Authentication
- Rate limiting
- Request routing
- Response aggregation
```

#### Service Discovery
```
- Consul, Eureka, etcd
- Dynamic service registration
- Health checks
```

#### Circuit Breaker
```
- Prevent cascading failures
- Fallback mechanisms
- Tools: Hystrix, Resilience4j
```

---

### 3.7 Consistency Patterns

#### Strong Consistency
```
- Read always returns latest write
- Higher latency
- Use: Banking, inventory
- Examples: RDBMS with ACID
```

#### Eventual Consistency
```
- Reads may return stale data
- Lower latency, higher availability
- Use: Social media feeds, comments
- Examples: DynamoDB, Cassandra
```

#### Causal Consistency
```
- Related operations ordered
- Unrelated can be concurrent
- Use: Collaborative editing
```

---

### 3.8 Data Replication

#### Synchronous Replication
```
Write → Master → Slave ACK → Client ACK
- Strong consistency
- Higher latency
- Lower throughput
```

#### Asynchronous Replication
```
Write → Master → Client ACK → Slave (async)
- Lower latency
- Eventual consistency
- Risk of data loss
```

#### Semi-Synchronous
```
Write → Master → At least 1 Slave ACK → Client ACK
- Balance of both
```

---

### 3.9 Rate Limiting

**Purpose**: Prevent abuse, ensure fair usage

**Algorithms:**

#### Token Bucket
```java
class TokenBucket {
    private int capacity;
    private int tokens;
    private long lastRefill;
    private long refillRate;
    
    public boolean allowRequest() {
        refill();
        if (tokens > 0) {
            tokens--;
            return true;
        }
        return false;
    }
    
    private void refill() {
        long now = System.currentTimeMillis();
        long tokensToAdd = (now - lastRefill) / 1000 * refillRate;
        tokens = Math.min(capacity, tokens + (int)tokensToAdd);
        lastRefill = now;
    }
}
```

#### Leaky Bucket
```
- Fixed rate of processing
- Queue incoming requests
- Drop if queue full
```

#### Fixed Window Counter
```
- Count requests per fixed time window
- Simple but allows burst at window boundaries
```

#### Sliding Window Log
```
- Track timestamp of each request
- Remove old timestamps
- Accurate but memory-intensive
```

---

## 4. System Design Patterns

### 4.1 CQRS (Command Query Responsibility Segregation)

```
Write Model (Commands) → Write DB
Read Model (Queries) → Read DB (optimized for reads)

Benefits:
- Optimize read and write separately
- Scale independently
- Different data models

Use Cases:
- Event sourcing
- Analytics + Operational data
```

### 4.2 Event Sourcing

```
Store all changes as events (immutable log)
- Rebuild state by replaying events
- Complete audit trail
- Time travel capabilities

Use Cases:
- Banking transactions
- Order management
- Audit requirements
```

### 4.3 Saga Pattern

```
Distributed transactions across microservices

Choreography: Event-driven, decentralized
Orchestration: Central coordinator

Use Cases:
- Order processing
- Payment workflows
```

---

## 5. Practice System Design Problems

### 5.1 URL Shortener (Bit.ly)

**Components:**
- Application servers (REST API)
- Database (URL mappings)
- Cache (Redis)
- Load balancer

**Key Design Decisions:**
```
1. URL Generation:
   - Base62 encoding (a-zA-Z0-9)
   - Auto-increment ID vs Random
   - Collision handling

2. Database Schema:
   Table: urls
   - id (bigint, primary key)
   - long_url (varchar)
   - short_url (varchar, indexed)
   - created_at (timestamp)
   - expires_at (timestamp)
   
3. Scaling:
   - Cache popular URLs
   - Database sharding by hash(short_url)
   - CDN for static content
```

---

### 5.2 Design Twitter

**Core Features:**
- Post tweets
- Follow users
- View timeline (home + user)
- Search tweets

**Components:**
- Load balancer
- Application servers
- Tweet storage (Cassandra)
- Timeline service
- User graph database
- Cache (Redis)
- Media storage (S3)
- Search service (Elasticsearch)

**Key Design Decisions:**
```
1. Timeline Generation:
   Fan-out on Write (Push):
   - Pre-compute timelines when tweet posted
   - Fast reads, slow writes
   - Use for normal users
   
   Fan-out on Read (Pull):
   - Compute timeline on demand
   - Fast writes, slow reads
   - Use for celebrities (millions of followers)
   
   Hybrid: Combine both approaches

2. Data Model:
   Users: user_id, name, bio, followers_count
   Tweets: tweet_id, user_id, content, timestamp
   Timeline: user_id, tweet_ids (sorted by time)
   Followers: user_id, follower_id

3. Scaling:
   - Shard users by user_id
   - Shard tweets by tweet_id
   - Cache timelines for active users
   - Rate limiting for API
```

---

### 5.3 Design Instagram

**Core Features:**
- Upload photos/videos
- Follow users
- View feed
- Like/comment

**Components:**
- API Gateway
- Photo upload service
- Image processing service (resize, thumbnails)
- Newsfeed service
- Object storage (S3)
- CDN (CloudFront)
- Database (photos, users)
- Cache

**Key Design Decisions:**
```
1. Image Storage:
   - Store in S3 (distributed object storage)
   - Generate multiple sizes (thumbnail, medium, full)
   - Use CDN for delivery
   - Store metadata in database

2. Newsfeed:
   - Similar to Twitter (push vs pull)
   - Rank by relevance (ML model)
   - Cache feed for active users

3. Consistency:
   - Eventual consistency for likes/views
   - Strong consistency for uploads
```

---

### 5.4 Design Netflix

**Core Features:**
- Video streaming
- Search & recommendation
- User profiles

**Components:**
- CDN (99% of traffic)
- Video encoding service
- Recommendation engine
- User service
- Content database
- Search service (Elasticsearch)

**Key Design Decisions:**
```
1. Video Delivery:
   - Encode in multiple formats (1080p, 720p, 480p)
   - Adaptive bitrate streaming (HLS/DASH)
   - CDN with edge locations globally
   - Pre-populate CDN based on predictions

2. Recommendation:
   - Collaborative filtering
   - Content-based filtering
   - Batch processing (Spark)
   - Real-time updates

3. Scale:
   - Separate read/write databases
   - Cache user preferences
   - Async processing for non-critical data
```

---

### 5.5 Design Uber

**Core Features:**
- Match riders with drivers
- Real-time location tracking
- Fare calculation
- Payment processing

**Components:**
- Location service
- Matching service
- Trip service
- Pricing service
- Payment gateway
- Notification service
- WebSocket servers (real-time)

**Key Design Decisions:**
```
1. Location Tracking:
   - QuadTree or Geohash for spatial indexing
   - Update driver location every 4-5 seconds
   - Store in Redis for fast access

2. Matching Algorithm:
   - Find nearby drivers (radius search)
   - Consider: distance, rating, car type
   - Optimized with geospatial indexes

3. Real-time Communication:
   - WebSockets for driver-rider communication
   - Push notifications for updates

4. Data Partitioning:
   - Partition by city/region
   - Each region has dedicated resources
```

---

### 5.6 Design WhatsApp

**Core Features:**
- Send/receive messages
- Group chats
- Online/offline status
- Message delivery receipts

**Components:**
- WebSocket servers
- Message queue (Kafka)
- Message storage
- User database
- Presence service
- Media storage (S3)

**Key Design Decisions:**
```
1. Real-time Messaging:
   - WebSocket for persistent connections
   - Each user connects to a chat server
   - Message routing via user-server mapping

2. Message Delivery:
   - Store messages in queue if user offline
   - Deliver when user comes online
   - Acknowledgments: sent, delivered, read

3. Group Chat:
   - Fan-out messages to all members
   - Store group metadata
   - Optimize for small groups (<256)

4. Encryption:
   - End-to-end encryption
   - Keys never stored on server
```

---

### 5.7 Design Rate Limiter

**Requirements:**
- Limit requests per user/IP
- Distributed system
- Low latency

**Design:**
```
1. Algorithm: Token Bucket (sliding window)

2. Storage: Redis
   Key: user_id or IP
   Value: {tokens, last_refill_time}

3. Distributed:
   - Centralized Redis cluster
   - Or rate limit at API Gateway

4. Handling:
   - Return 429 (Too Many Requests)
   - Add Retry-After header
```

---

### 5.8 Design Web Crawler

**Requirements:**
- Crawl billions of pages
- Extract links
- Handle duplicates
- Politeness (don't overwhelm servers)

**Components:**
- URL frontier (queue)
- HTML fetcher
- Link extractor
- Duplicate detector (bloom filter)
- Storage (distributed file system)
- URL deduplication (hash table)

**Key Design Decisions:**
```
1. URL Frontier:
   - Priority queue (important pages first)
   - Per-host queue (politeness)
   - Distributed across workers

2. Duplicate Detection:
   - Bloom filter (probabilistic)
   - Hash of URL for exact check
   - Hash of content for similar pages

3. Politeness:
   - Delay between requests to same host
   - Respect robots.txt
   - User-agent identification

4. Scale:
   - Distribute URLs by domain hash
   - Each worker handles subset of domains
```

---

### 5.9 Design Distributed Cache

**Requirements:**
- Get/Put operations
- High availability
- Horizontal scaling
- LRU eviction

**Design:**
```
1. Consistent Hashing:
   - Map keys to servers
   - Minimize rehashing on server add/remove
   - Virtual nodes for better distribution

2. Replication:
   - Replicate to N servers
   - Read from any replica
   - Write to all replicas (quorum)

3. Eviction:
   - LRU per server
   - TTL support
   - Memory monitoring

4. High Availability:
   - Monitor node health
   - Redistribute keys on failure
```

---

### 5.10 Design Notification System

**Requirements:**
- Multiple channels (email, SMS, push)
- High throughput
- Reliable delivery
- Priority handling

**Components:**
- API service
- Queue (Kafka/RabbitMQ)
- Workers (per channel)
- Template service
- Tracking database
- Third-party services (Twilio, SendGrid)

**Key Design Decisions:**
```
1. Queue-based:
   - Producer: API service
   - Consumer: Channel-specific workers
   - Retry mechanism for failures

2. Priority:
   - Multiple queues (high, medium, low)
   - Process high priority first

3. Rate Limiting:
   - Respect provider limits
   - Token bucket per channel

4. Tracking:
   - Store notification status
   - Enable querying delivery status
```

---

## 6. Common Pitfalls

### ❌ Don't
1. Jump into implementation without clarifying requirements
2. Design everything at once (start simple, then scale)
3. Forget to discuss trade-offs
4. Ignore non-functional requirements
5. Over-engineer for Day 1
6. Forget to ask questions
7. Use buzzwords without understanding

### ✅ Do
1. Start with requirements and constraints
2. Think about scale and bottlenecks
3. Discuss trade-offs explicitly
4. Drive the conversation
5. Think out loud
6. Ask clarifying questions
7. Consider availability, reliability, scalability

---

## 7. System Design Cheat Sheet

### Component Checklist
- [ ] Load Balancer
- [ ] Application Servers
- [ ] Database (SQL/NoSQL)
- [ ] Cache (Redis/Memcached)
- [ ] CDN (for static content)
- [ ] Message Queue
- [ ] Object Storage (S3)
- [ ] Search Engine (Elasticsearch)
- [ ] API Gateway
- [ ] Monitoring & Logging

### Scaling Techniques
- [ ] Horizontal scaling (add more servers)
- [ ] Vertical scaling (bigger servers)
- [ ] Caching (multiple levels)
- [ ] Database replication (master-slave)
- [ ] Database sharding (partition data)
- [ ] CDN (cache at edge)
- [ ] Asynchronous processing (queues)
- [ ] Load balancing

### Non-Functional Requirements
- [ ] Scalability
- [ ] Availability
- [ ] Reliability
- [ ] Latency
- [ ] Consistency
- [ ] Durability
- [ ] Security

---

## 8. Recommended Resources

**Books:**
- Designing Data-Intensive Applications (Martin Kleppmann)
- System Design Interview (Alex Xu) - Vol 1 & 2

**Courses:**
- Grokking the System Design Interview
- System Design Primer (GitHub)

**YouTube Channels:**
- Gaurav Sen
- Tech Dummies
- System Design Interview

**Practice:**
- Exponent.com
- Interviewing.io (mock interviews)
- Pramp.com

---

## Next Steps
- Practice 1-2 designs per week
- Draw diagrams for each design
- Discuss with peers or mentors
- Review `02_PREPARATION_ROADMAP.md` for timeline

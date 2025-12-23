# 01. Design a URL Shortener (TinyURL)

**Difficulty:** Easy/Medium
**Focus:** Unique ID Generation, Database Choice, High Read/Write Ratio.

---

## 🎯 Real-World Analogy

**Think of URL shortening like a coat check at a restaurant:**
- You arrive with a bulky coat (long URL: `https://www.example.com/products/category/shoes/brand/nike/model/air-max-2024?color=red&size=10`)
- The coat check person gives you a small ticket with a number: `#42` (short URL: `tiny.url/AbCd12`)
- When you leave, you show ticket `#42`, and they retrieve your specific coat
- The ticket is much easier to carry than the coat itself!

**Key Insight:** We're trading a long, hard-to-remember URL for a short, easy-to-share one. The system maintains a mapping between them.

---

## 1. Requirements (0-5 Minutes)
*Always start by clarifying the scope. Don't assume.*

**Candidate:** "Before I design, I'd like to clarify the requirements."

**Functional Requirements:**
1.  **Shortening:** Given a long URL, return a unique short URL.
2.  **Redirection:** When a user accesses the short URL, redirect them to the original long URL.
3.  **Custom Alias:** (Optional) Users can pick a custom alias (e.g., `tiny.url/my-blog`).
4.  **Expiry:** Users can set an expiration time for the link.

**Non-Functional Requirements:**
1.  **High Availability:** The system should never be down. Redirection must always work.
2.  **Low Latency:** Redirection should happen in milliseconds.
3.  **Read-Heavy:** There will be many more redirections (reads) than link creations (writes).

---

## 2. Back-of-the-Envelope Estimation (5-10 Minutes)
*Show you can do math. This determines your storage and scaling needs.*

### 📚 ELI5: Why Do We Need Math?

**Beginner Question:** "Why can't we just build it and see what happens?"

**Answer:** Imagine building a parking lot without knowing how many cars will come. Too small? Cars can't park. Too big? You wasted money on unused space. 

In system design, we estimate:
- **How much traffic?** (Do we need 1 server or 1000?)
- **How much storage?** (Can one database handle it, or do we need to split it?)
- **How fast must it be?** (Can we use a simple solution, or do we need caching?)

### 🔢 Let's Do the Math (With Real Numbers)

**Assumptions:**
*   **Write Traffic:** 100 Million new URLs per month.
    - *Think:* That's like every person in a large country creating 1 short link per month
*   **Read Traffic:** 100:1 ratio. So, 10 Billion reads per month.
    - *Think:* Each short link gets clicked 100 times on average (viral links get clicked millions of times!)
*   **Duration:** We need to store data for 5 years.

**Calculations:**
*   **QPS (Writes):** 100M / (30 days * 24 hrs * 3600 sec) ≈ **40 writes/sec**. (Low)
*   **QPS (Reads):** 10B / (30 * 24 * 3600) ≈ **4,000 reads/sec**. (Manageable)
*   **Storage:**
    *   Each URL entry ≈ 500 bytes (ID, LongURL, ShortURL, CreatedAt).
    *   Total Objects: 100M * 12 months * 5 years = 6 Billion URLs.
    *   Total Storage: 6 Billion * 500 bytes = **3 TB**.
    *   *Conclusion:* 3TB fits easily into a modern distributed database (or even a large single instance, but we'll distribute it for availability).

---

## 3. API Design (10-15 Minutes)
*Define the contract.*

**REST API:**

1.  **Create Short URL**
    *   `POST /api/v1/urls`
    *   **Request:** `{ "long_url": "https://...", "expire_date": "2025-12-31" }`
    *   **Response:** `{ "short_url": "https://tiny.url/AbCd12" }`

2.  **Redirect**
    *   `GET /{short_url}`
    *   **Response:** `301 Permanent Redirect` (or `302 Found`).
    *   *Interview Tip:* Mention that **301** is better for reducing server load (browser caches the redirect), but **302** is better if you want to track analytics (every request hits your server).

---

## 4. Database Schema (15-20 Minutes)
*Choose the right tool.*

**Candidate:** "Since we need to store billions of rows and the data is not relational (no complex joins), a **NoSQL** store like **DynamoDB**, **Cassandra**, or **MongoDB** is ideal. It scales easier. However, since 3TB is manageable, a sharded **MySQL/PostgreSQL** setup also works perfectly fine."

**Table: `UrlMapping`**
*   `id` (PK): BigInt (or String)
*   `short_code`: String (Indexed)
*   `long_url`: String
*   `created_at`: Timestamp
*   `expires_at`: Timestamp

---

## 5. High-Level Design (20-30 Minutes)
*Draw the boxes.*

**System Architecture:**

![URL Shortener Architecture](../diagrams/url_shortener_architecture_1763559821180.png)

**Components:**
1.  **Client:** Sends request.
2.  **Load Balancer (LB):** Distributes traffic.
3.  **Web Servers:** Stateless API servers.
4.  **Cache (Redis):** Stores the mapping of `short_code -> long_url` for fast redirection.
5.  **Database:** Persistent storage.
6.  **KGS:** Key Generation Service for unique IDs.

**Flow (Write - Create Short URL):**

![URL Shortener Write Flow](../diagrams/url_shortener_write_flow_1763559833496.png)

**Flow (Read - Redirect):**

![URL Shortener Read Flow](../diagrams/url_shortener_read_flow_1763559843974.png)

---

## 6. Deep Dive: The Core Algorithm (30-40 Minutes)
*This is where you pass or fail. How do you generate the short code?*

### 🤔 The Core Challenge

**Problem:** We need to convert `https://www.example.com/very/long/url` into `tiny.url/AbCd12`

The question is: **How do we generate `AbCd12`?**

### ❌ Approach A: Random String (Seems Easy, But...)

**How it works:**
1. Generate a random 7-character string (e.g., `xY9kLm2`)
2. Check database: "Does this code already exist?"
3. If yes, try again with a different random string
4. If no, use it!

**Why this fails at scale:**
- **Birthday Paradox:** With 1 billion URLs in the system, collision probability becomes high
- **Example:** Imagine a parking lot with 1000 spots. First 100 cars park easily. But as it fills up, finding an empty spot takes longer and longer.
- **Performance killer:** Each retry = 1 database query. With high collision rates, you might retry 10+ times!

### ✅ Approach B: Base62 Encoding (The Winner)

### 📚 ELI5: What is Base62?

**The Concept:**
- **Base10 (Decimal):** 0-9 (10 symbols). Used by humans.
- **Base2 (Binary):** 0-1 (2 symbols). Used by computers.
- **Base62:** 0-9, A-Z, a-z (62 symbols). Used for URLs.

**Why Base62?**
- It's **URL Safe** (no special characters like `+` or `/` which Base64 has).
- It's **Compact**. A large number becomes a short string.

**Capacity Analysis:**
| Length | Combinations (62^n) | Capacity |
|--------|---------------------|----------|
| 5 chars | 916 Million | Small App |
| 6 chars | 56 Billion | Medium App |
| 7 chars | 3.5 Trillion | **TinyURL Scale** |
| 8 chars | 218 Trillion | Massive Scale |

**Conclusion:** 7 characters is plenty for 100 years!

### 📝 Step-by-Step: Base62 Conversion

**The Math:**
To convert a number (ID) to Base62, we repeatedly divide by 62 and take the remainder.

**Example: Convert ID `12345` to Base62**

**Symbols:**
`0-9` = 0-9
`A-Z` = 10-35
`a-z` = 36-61

**Calculation:**
1. `12345 ÷ 62` = **199** remainder **7**
   - Symbol for 7 is **'7'**
   
2. `199 ÷ 62` = **3** remainder **13**
   - Symbol for 13 is **'D'** (A=10, B=11, C=12, D=13)
   
3. `3 ÷ 62` = **0** remainder **3**
   - Symbol for 3 is **'3'**

**Result:** Read remainders backwards → **"3D7"**

**Verification:**
`3 * (62^2) + 13 * (62^1) + 7 * (62^0)`
`= 3 * 3844 + 13 * 62 + 7`
`= 11532 + 806 + 7`
`= 12345` ✅

### 💻 Python Implementation

```python
class Base62:
    CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    
    @classmethod
    def encode(cls, num):
        if num == 0:
            return cls.CHARSET[0]
        
        result = []
        while num > 0:
            remainder = num % 62
            result.append(cls.CHARSET[remainder])
            num //= 62
            
        # Reverse to get correct order
        return ''.join(reversed(result))

    @classmethod
    def decode(cls, string):
        num = 0
        for char in string:
            num = num * 62 + cls.CHARSET.index(char)
        return num

# Test it
print(Base62.encode(12345))  # Output: "3D7"
print(Base62.decode("3D7"))  # Output: 12345
```

### ☕ Java Implementation

```java
public class Base62 {
    private static final String CHARSET = 
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;
    
    /**
     * Encode a number to Base62 string
     */
    public static String encode(long num) {
        if (num == 0) {
            return String.valueOf(CHARSET.charAt(0));
        }
        
        StringBuilder result = new StringBuilder();
        
        while (num > 0) {
            int remainder = (int) (num % BASE);
            result.append(CHARSET.charAt(remainder));
            num /= BASE;
        }
        
        // Reverse to get correct order
        return result.reverse().toString();
    }
    
    /**
     * Decode a Base62 string back to number
     */
    public static long decode(String str) {
        long num = 0;
        
        for (char c : str.toCharArray()) {
            num = num * BASE + CHARSET.indexOf(c);
        }
        
        return num;
    }
    
    // Test it
    public static void main(String[] args) {
        System.out.println(encode(12345));  // Output: "3D7"
        System.out.println(decode("3D7"));  // Output: 12345
    }
}
```

**🎯 Key Insight:** If we have a unique ID (number), we can generate a unique Short URL (string). The problem now shifts to: **How to generate unique IDs efficiently?**

### 🔑 The Real Problem: Generating Unique IDs in a Distributed System

**🤔 Common Misconception:** "Why not just use database AUTO_INCREMENT?"

**Answer:** Let's see what happens:

**Scenario with AUTO_INCREMENT:**
- You have 3 database servers (for high availability)
- Server A generates ID: 1, 2, 3...
- Server B generates ID: 1, 2, 3... ❌ **Collision!**

Even if you configure them to use different ranges:
- Server A: 1, 4, 7, 10... (every 3rd number)
- Server B: 2, 5, 8, 11...
- Server C: 3, 6, 9, 12...

**Problems:**
- ❌ Coordination overhead (servers must talk to each other)
- ❌ Single point of failure (if one server dies, gaps in sequence)
- ❌ Doesn't work well with database sharding

### ✅ Solution: Key Generation Service (KGS)

**🎯 Analogy:** Think of KGS like a **ticket dispenser at a deli counter**
- The machine has pre-printed tickets: 1, 2, 3, 4, 5...
- When you arrive, you pull a ticket
- The machine guarantees you get a unique number
- Multiple people can pull tickets simultaneously without collision

### 🏗️ KGS Architecture

```
┌──────────────┐      ┌──────────────┐
│ Web Server 1 │      │ Web Server 2 │
└──────┬───────┘      └──────┬───────┘
       │ Request Batch       │
       │ (e.g., 1000 keys)   │
       ▼                     ▼
┌────────────────────────────────────┐
│    Key Generation Service (KGS)    │
│  (Maintains "Unused" Key Ranges)   │
└────────────────┬───────────────────┘
                 │
         ┌───────▼───────┐
         │  KGS Database │
         └───────────────┘
```

### 💻 KGS Implementation Details

**Step 1: The KGS Database Table**
We don't store every single key. We store **ranges**.

```sql
CREATE TABLE key_ranges (
    id INT AUTO_INCREMENT PRIMARY KEY,
    start_id BIGINT NOT NULL,
    end_id BIGINT NOT NULL,
    is_used BOOLEAN DEFAULT FALSE
);

-- Example Data:
-- 1, 1, 1000, TRUE      (Allocated to Server A)
-- 2, 1001, 2000, TRUE   (Allocated to Server B)
-- 3, 2001, 3000, FALSE  (Available)
```

**Step 2: KGS Logic (Python)**

```python
class KeyGenerationService:
    def __init__(self, db_connection):
        self.db = db_connection
        
    def get_key_batch(self):
        # Transaction start
        with self.db.transaction():
            # Find an unused range
            range = self.db.query(
                "SELECT * FROM key_ranges WHERE is_used = FALSE LIMIT 1 FOR UPDATE"
            )
            
            if not range:
                return None  # Alert: Generate more ranges!
                
            # Mark as used
            self.db.execute(
                "UPDATE key_ranges SET is_used = TRUE WHERE id = %s", 
                range.id
            )
            
            return (range.start_id, range.end_id)
```

**Step 3: Web Server Logic**

```python
class URLShortener:
    def __init__(self):
        self.key_buffer = []
        self.kgs = KGSClient()
        
    def get_unique_id(self):
        # If buffer is empty, fetch new batch
        if not self.key_buffer:
            start, end = self.kgs.get_key_batch()
            self.key_buffer = list(range(start, end + 1))
            
        # Return next key from memory
        return self.key_buffer.pop(0)
```

### 🛡️ Handling Edge Cases

**1. What if the KGS crashes?**
- **Solution:** Use a standby replica.
- If Primary KGS dies, Standby takes over.
- To avoid giving out duplicate ranges, KGS must commit "range allocation" to DB *before* returning to Web Server.

**2. What if a Web Server crashes?**
- **Scenario:** Web Server A grabs range `1001-2000`. It uses `1001-1050` then crashes.
- **Result:** Keys `1051-2000` are lost in memory.
- **Impact:** Is this bad? **No.** We have 3.5 trillion keys. Losing 950 keys is negligible.
- **Benefit:** Simplicity! We don't need complex recovery logic.

**3. Concurrency?**
- KGS uses database row locks (`FOR UPDATE`) to ensure two servers never get the same range.

---

## 7. Deep Dive: Caching Strategy (40-42 Minutes)

### 📚 ELI5: The 80/20 Rule

**Observation:**
- 20% of URLs generate 80% of the traffic.
- Viral links (e.g., Super Bowl ad) get millions of clicks.
- Old links (e.g., 2015 blog post) get zero clicks.

**Strategy:**
- We don't need to cache *everything*.
- We only cache the **hot** 20%.
- This saves massive amounts of RAM.

### 🧠 Cache Implementation (Redis)

**Flow:**
1. Check Redis.
2. If found (Hit) → Redirect.
3. If not found (Miss) → Query DB → Update Redis → Redirect.

```python
def redirect(short_code):
    # 1. Try Cache
    long_url = redis.get(f"url:{short_code}")
    
    if long_url:
        return HTTP_301(long_url)
        
    # 2. Query DB
    long_url = db.query("SELECT url FROM mappings WHERE code=%s", short_code)
    
    if long_url:
        # 3. Update Cache (with TTL)
        # TTL = Time To Live (e.g., 24 hours)
        redis.setex(f"url:{short_code}", 86400, long_url)
        return HTTP_301(long_url)
        
    return HTTP_404
```

### 🛡️ Cache Stampede (Thundering Herd)

**Problem:**
- A viral link (Justin Bieber's new song) is in cache.
- 10,000 users/sec are clicking it.
- **Cache expires** at 10:00:00.
- Suddenly, 10,000 requests hit the Database simultaneously!
- Database crashes 💥

**Solution:**
- **Mutex Lock:** Only allow *one* request to query DB and refill cache. Others wait.
- **Probabilistic Early Expiration:** If TTL is close to expiring, randomly choose one request to refresh it *before* it actually expires.

---

## 8. Deep Dive: Database Sharding (42-45 Minutes)

**Problem:**
- 3TB of data is okay for one DB, but 100M writes/month might choke a single master.
- We need to split the data (Sharding).

**Strategy A: Range-Based Sharding**
- Shard 1: URLs starting with 'A'
- Shard 2: URLs starting with 'B'
- **Problem:** Unbalanced. 'A' might have 10x more URLs than 'Z'. **Hotspots!** ❌

**Strategy B: Hash-Based Sharding (Recommended)**
- `Shard_ID = hash(short_code) % Num_Shards`
- **Pros:** Even distribution.
- **Cons:** Resharding (adding servers) is hard.
- **Fix:** Use **Consistent Hashing** (see Scenario 11).

---

## 9. Deep Dive: Analytics (Async Processing)

**Requirement:** Track clicks, browser type, country, etc.

**Bad Design:**
- User clicks -> Update DB (increment count) -> Redirect.
- **Why bad?** Adds latency to the redirect. Writes are slow.

**Good Design (Async):**
1. User clicks.
2. Server sends `301 Redirect` immediately.
3. **Fire-and-forget** event to Kafka: `{ "short_code": "AbCd12", "ip": "1.2.3.4", "ts": 12345 }`.
4. **Analytics Worker** reads Kafka -> Aggregates data -> Writes to ClickHouse/Hadoop.

---

## 10. Deep Dive: Security (Preventing Enumeration)

**The Attack:**
- Attacker writes a script to try `tiny.url/1`, `tiny.url/2`, `tiny.url/3`...
- They scrape all your private URLs!

**Solution 1: Non-Sequential IDs**
- Don't use `1, 2, 3`.
- Use KGS to shuffle the ranges or use a Feistel Cipher to permute IDs.

**Solution 2: Rate Limiting**
- If IP `1.2.3.4` requests 100 invalid URLs in 1 minute -> Ban IP.

---

---

## 11. Real-World Engineering Case Studies (Deep Dive)

### 1. Twitter (t.co): The "Safety First" Shortener

**Source:** Twitter Engineering Blog

**The Challenge:**
Twitter shortens *every* link shared on the platform to `t.co`.
- **Scale:** Billions of tweets per day.
- **Safety:** Malicious links (phishing/malware) must be blocked instantly.
- **Analytics:** Advertisers need to know exactly who clicked.

**The Solution:**
Twitter doesn't just redirect; it acts as a **Proxy**.

![Twitter t.co Architecture](../diagrams/twitter_tco_architecture.png)

**Architecture:**
1.  **Ingestion:** User tweets a link.
2.  **Shortening:** System generates `t.co/xyz`.
3.  **Async Safety Check:**
    - A background worker crawls the target URL.
    - Checks against Google Safe Browsing API and internal blocklists.
    - If bad, the link is flagged.
4.  **Redirection Flow:**
    - User clicks `t.co/xyz`.
    - Request hits Twitter's Edge Server.
    - **Check:** Is this link flagged?
        - **Yes:** Show "Warning: Unsafe Link" page.
        - **No:** Return `301 Redirect`.

**Key Insight:**
`t.co` isn't just about saving characters (especially now that Twitter counts links as 23 chars regardless of length). It's about **Control** and **Security**.

---

### 2. Bitly: The "Enterprise" Shortener

**Source:** High Scalability / Bitly Engineering

**The Challenge:**
Bitly sells "Branded Short Links" (e.g., `nyti.ms/article`) to enterprises.
- **Requirement:** Real-time analytics dashboard for customers.
- **Scale:** 10 Billion+ clicks per month.

**The Solution: Distributed Counting with Hbase**

![Bitly Architecture](../diagrams/bitly_architecture.png)

**Architecture:**
1.  **The Click:** User clicks `bit.ly/abc`.
2.  **The Redirect:** Fast `302 Redirect` (Temporary) is used.
    - *Why 302?* It forces the browser to hit Bitly *every time*, ensuring accurate stats. `301` would cache locally.
3.  **The Async Write:**
    - Click event pushed to **NSQ** (Message Queue).
    - **Stream Processing:** Storm/Spark aggregates counts by Country, Device, Referrer.
    - **Storage:** Data written to **HBase** (Hadoop Database) for fast random writes and range scans.

**Key Insight:**
For analytics-heavy products, use `302 Redirects` and decouple the "Redirect Path" (Latency sensitive) from the "Analytics Path" (Throughput sensitive).

---

## 11. Failure Scenarios & Disaster Recovery

### Scenario 1: Database Failure
**Impact:** Cannot create new short URLs or resolve existing ones
**Detection:**
- Health check failures on database endpoints
- Error rate spike (500 errors)
- Alert triggers when error rate > 1% for 2 minutes

**Mitigation:**
```python
class URLShortenerWithFailover:
    def __init__(self):
        self.primary_db = DynamoDB(region='us-east-1')
        self.replica_db = DynamoDB(region='us-west-2')
        self.cache = Redis()
    
    def get_long_url(self, short_code):
        # Try cache first
        cached = self.cache.get(short_code)
        if cached:
            return cached
        
        # Try primary database
        try:
            long_url = self.primary_db.get(short_code)
            self.cache.set(short_code, long_url, ttl=3600)
            return long_url
        except DatabaseException:
            # Failover to replica
            long_url = self.replica_db.get(short_code)
            self.cache.set(short_code, long_url, ttl=3600)
            return long_url
```

**Recovery Time:** < 30 seconds (automatic failover)

### Scenario 2: KGS (Key Generation Service) Failure
**Impact:** Cannot generate new short URLs
**Detection:** KGS health check fails, key pool exhausted

**Mitigation:**
- **Pre-generated Key Pool:** Each API server maintains local pool of 10,000 keys
- **Multiple KGS Instances:** Run 3 KGS instances, each generating non-overlapping ranges
- **Graceful Degradation:** If KGS down, use timestamp-based generation (less optimal but functional)

### Scenario 3: Traffic Spike (10x Normal Load)
**Impact:** Increased latency, potential service degradation
**Detection:** Request rate > 10,000/sec, p99 latency > 100ms

**Mitigation:**
- **Auto-scaling:** Horizontal pod autoscaler adds servers when CPU > 70%
- **Rate Limiting:** Per-user rate limit (100 requests/minute)
- **CDN Caching:** Cache popular redirects at edge (Cloudflare)

---

## 12. Monitoring & Observability

### Golden Signals Dashboard

**1. Latency (Response Time)**
```
P50: < 10ms
P95: < 50ms
P99: < 100ms
```

**Metrics to Track:**
```python
# Prometheus metrics
redirect_latency = Histogram(
    'url_redirect_latency_seconds',
    'Time to resolve and redirect',
    buckets=[0.01, 0.05, 0.1, 0.5, 1.0]
)

shorten_latency = Histogram(
    'url_shorten_latency_seconds',
    'Time to create short URL',
    buckets=[0.05, 0.1, 0.5, 1.0, 2.0]
)
```

**2. Traffic (Requests Per Second)**
```
Normal: 1,000 RPS
Peak: 10,000 RPS
```

**3. Errors (Error Rate)**
```
Target: < 0.1%
Alert: > 1% for 2 minutes
```

**4. Saturation (Resource Usage)**
```
CPU: < 70%
Memory: < 80%
Database Connections: < 80% of pool
```

### Alert Rules
```yaml
alerts:
  - name: HighErrorRate
    condition: error_rate > 0.01
    duration: 2m
    severity: critical
    action: page_oncall
  
  - name: HighLatency
    condition: p99_latency > 100ms
    duration: 5m
    severity: warning
    action: slack_notification
  
  - name: DatabaseDown
    condition: db_health_check == false
    duration: 30s
    severity: critical
    action: page_oncall + auto_failover
```

### Sample Grafana Dashboard
```
+----------------------+----------------------+
| Requests/sec         | Error Rate %         |
| (Line chart)         | (Line chart)         |
+----------------------+----------------------+
| P99 Latency          | Cache Hit Rate       |
| (Line chart)         | (Gauge: 85%)         |
+----------------------+----------------------+
| Active Short URLs    | Database Connections |
| (Counter: 50M)       | (Gauge: 45/100)      |
+----------------------+----------------------+
```

---

## 13. API Design & Versioning

### RESTful API Endpoints

**Create Short URL**
```http
POST /api/v1/shorten
Content-Type: application/json

Request:
{
  "long_url": "https://example.com/very/long/path",
  "custom_alias": "my-link",  // Optional
  "expiration_days": 30        // Optional
}

Response (201 Created):
{
  "short_url": "https://short.ly/abc123",
  "short_code": "abc123",
  "long_url": "https://example.com/very/long/path",
  "created_at": "2024-01-15T10:30:00Z",
  "expires_at": "2024-02-14T10:30:00Z"
}
```

**Redirect (GET Short URL)**
```http
GET /{short_code}

Response (301 Moved Permanently):
Location: https://example.com/very/long/path
```

**Get URL Stats**
```http
GET /api/v1/stats/{short_code}
Authorization: Bearer <token>

Response (200 OK):
{
  "short_code": "abc123",
  "total_clicks": 1523,
  "unique_clicks": 892,
  "created_at": "2024-01-15T10:30:00Z",
  "last_accessed": "2024-01-20T15:45:00Z",
  "top_countries": ["US", "IN", "UK"],
  "top_referrers": ["twitter.com", "facebook.com"]
}
```

### Versioning Strategy
- **URL Versioning:** `/api/v1/`, `/api/v2/`
- **Deprecation Policy:** Support v1 for 12 months after v2 launch
- **Breaking Changes:** Only in new major versions

### Error Responses
```json
{
  "error": {
    "code": "INVALID_URL",
    "message": "The provided URL is not valid",
    "field": "long_url",
    "timestamp": "2024-01-15T10:30:00Z",
    "request_id": "req_abc123"
  }
}
```

**Error Codes:**
- `400 INVALID_URL`: Malformed URL
- `409 ALIAS_TAKEN`: Custom alias already exists
- `429 RATE_LIMIT_EXCEEDED`: Too many requests
- `500 INTERNAL_ERROR`: Server error

---

## 14. Cost Analysis & Optimization

### Monthly Infrastructure Costs (at 100M URLs/month)

**Compute (API Servers)**
- 20 EC2 instances (t3.medium) × $30/month = **$600/month**
- Auto-scaling: +10 instances during peak = **+$300/month**

**Storage (DynamoDB)**
- 100M URLs × 1KB average = 100 GB
- DynamoDB storage: 100 GB × $0.25/GB = **$25/month**
- Read capacity: 1,000 RCU × $0.00013/hour × 730 hours = **$95/month**
- Write capacity: 100 RCU × $0.00065/hour × 730 hours = **$47/month**

**Cache (Redis/ElastiCache)**
- 1 cache.r5.large instance = **$150/month**

**CDN (CloudFront)**
- 10 TB data transfer × $0.085/GB = **$850/month**

**Total Monthly Cost:** ~**$2,067/month**

### Cost Optimization Opportunities

**1. Cache Hit Rate Optimization**
- Current cache hit rate: 80%
- Target: 95%
- **Savings:** Reduce database reads by 75% → Save $70/month on DynamoDB

**2. Reserved Instances**
- Buy 1-year reserved instances for base capacity
- **Savings:** 30% discount on compute → Save $180/month

**3. S3 for Cold Storage**
- Move URLs not accessed in 90 days to S3
- S3 storage: $0.023/GB vs DynamoDB $0.25/GB
- **Savings:** ~$200/month for 10M cold URLs

**4. Compression**
- Compress analytics data before storing
- **Savings:** 60% reduction in storage → Save $150/month

### Cost vs Performance Trade-offs
| Option | Cost/Month | Availability | Latency |
|--------|------------|--------------|---------|
| Single Region | $2,000 | 99.9% | 50ms |
| Multi-Region | $4,500 | 99.99% | 20ms |
| Global CDN | $3,000 | 99.95% | 10ms |

---

## 15. Security & Compliance

### Authentication & Authorization

**API Key Authentication**
```python
class APIKeyAuth:
    def authenticate(self, request):
        api_key = request.headers.get('X-API-Key')
        if not api_key:
            raise Unauthorized("API key required")
        
        user = self.validate_api_key(api_key)
        if not user:
            raise Unauthorized("Invalid API key")
        
        # Rate limiting per user
        if self.is_rate_limited(user.id):
            raise RateLimitExceeded("Too many requests")
        
        return user
```

**OAuth 2.0 for User Actions**
```http
POST /oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials&
client_id=abc123&
client_secret=xyz789

Response:
{
  "access_token": "eyJhbGc...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

### Data Protection

**Encryption at Rest**
- DynamoDB encryption using AWS KMS
- Key rotation every 90 days

**Encryption in Transit**
- TLS 1.3 for all API endpoints
- HSTS header: `Strict-Transport-Security: max-age=31536000`

### Malicious URL Protection

**URL Validation**
```python
import re
from urllib.parse import urlparse

class URLValidator:
    BLACKLIST_DOMAINS = ['malware.com', 'phishing.com']
    
    def validate_url(self, url):
        # Check format
        if not re.match(r'https?://', url):
            raise InvalidURL("URL must start with http:// or https://")
        
        # Parse URL
        parsed = urlparse(url)
        
        # Check blacklist
        if parsed.netloc in self.BLACKLIST_DOMAINS:
            raise BlockedURL("Domain is blacklisted")
        
        # Check against Google Safe Browsing API
        if self.is_unsafe(url):
            raise UnsafeURL("URL flagged as unsafe")
        
        return True
```

### Compliance

**GDPR Compliance**
- Right to deletion: API endpoint to delete user's URLs
- Data portability: Export user's URL data as JSON
- Consent: Explicit consent for analytics tracking

**Audit Logging**
```python
def log_audit_event(action, user_id, resource, result):
    audit_log.write({
        "timestamp": datetime.utcnow(),
        "action": action,  # "CREATE_URL", "DELETE_URL", "ACCESS_STATS"
        "user_id": user_id,
        "resource": resource,
        "result": result,  # "SUCCESS", "FAILURE"
        "ip_address": request.remote_addr,
        "user_agent": request.headers.get('User-Agent')
    })
```

---

## 16. Testing Strategies

### Unit Tests
```python
import unittest

class TestURLShortener(unittest.TestCase):
    def setUp(self):
        self.shortener = URLShortener()
    
    def test_base62_encoding(self):
        # Test encoding
        encoded = self.shortener.base62_encode(125)
        self.assertEqual(encoded, "2B")
        
        # Test decoding
        decoded = self.shortener.base62_decode("2B")
        self.assertEqual(decoded, 125)
    
    def test_create_short_url(self):
        long_url = "https://example.com/test"
        result = self.shortener.create(long_url)
        
        self.assertIsNotNone(result['short_code'])
        self.assertEqual(len(result['short_code']), 7)
    
    def test_duplicate_url_returns_same_code(self):
        long_url = "https://example.com/test"
        result1 = self.shortener.create(long_url)
        result2 = self.shortener.create(long_url)
        
        self.assertEqual(result1['short_code'], result2['short_code'])
```

### Integration Tests
```python
def test_end_to_end_flow():
    # Create short URL
    response = client.post('/api/v1/shorten', json={
        'long_url': 'https://example.com/test'
    })
    assert response.status_code == 201
    short_code = response.json['short_code']
    
    # Verify redirect works
    response = client.get(f'/{short_code}', follow_redirects=False)
    assert response.status_code == 301
    assert response.headers['Location'] == 'https://example.com/test'
    
    # Verify analytics tracked
    time.sleep(1)  # Allow async processing
    response = client.get(f'/api/v1/stats/{short_code}')
    assert response.json['total_clicks'] == 1
```

### Load Testing (using k6)
```javascript
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  stages: [
    { duration: '1m', target: 100 },   // Ramp up to 100 users
    { duration: '3m', target: 1000 },  // Ramp up to 1000 users
    { duration: '1m', target: 0 },     // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<100'],  // 95% of requests < 100ms
    http_req_failed: ['rate<0.01'],    // Error rate < 1%
  },
};

export default function () {
  // Test redirect
  let response = http.get('https://short.ly/abc123');
  check(response, {
    'status is 301': (r) => r.status === 301,
    'redirect time < 50ms': (r) => r.timings.duration < 50,
  });
}
```

### Chaos Engineering
```python
# Simulate database failure
def test_database_failover():
    # Kill primary database
    primary_db.shutdown()
    
    # Verify service still works (using replica)
    response = client.get('/abc123')
    assert response.status_code == 301
    
    # Verify failover time < 30 seconds
    assert failover_time < 30
```

---

## 17. Migration & Rollout Strategies

### Blue-Green Deployment

**Step 1: Deploy Green Environment**
```bash
# Deploy new version
kubectl apply -f deployment-v2.yaml

# Verify health
kubectl get pods -l version=v2
```

**Step 2: Canary Release (10% Traffic)**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: url-shortener
spec:
  selector:
    app: url-shortener
  sessionAffinity: ClientIP
---
# Route 10% to v2, 90% to v1
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: url-shortener
spec:
  http:
  - match:
    - headers:
        canary:
          exact: "true"
    route:
    - destination:
        host: url-shortener-v2
      weight: 10
    - destination:
        host: url-shortener-v1
      weight: 90
```

**Step 3: Monitor Metrics**
- Error rate: v2 should be ≤ v1
- Latency: p99 should be ≤ v1
- Monitor for 1 hour

**Step 4: Full Rollout or Rollback**
```bash
# If successful, route 100% to v2
kubectl apply -f service-v2-100percent.yaml

# If issues, rollback
kubectl rollout undo deployment/url-shortener
```

### Database Migration

**Adding Expiration Feature**
```sql
-- Step 1: Add column (nullable)
ALTER TABLE urls ADD COLUMN expires_at TIMESTAMP NULL;

-- Step 2: Backfill existing data (default: never expire)
UPDATE urls SET expires_at = '2099-12-31' WHERE expires_at IS NULL;

-- Step 3: Make column non-nullable
ALTER TABLE urls ALTER COLUMN expires_at SET NOT NULL;

-- Step 4: Add index
CREATE INDEX idx_expires_at ON urls(expires_at);
```

### Feature Flags
```python
class FeatureFlags:
    def __init__(self):
        self.flags = {
            'custom_aliases': True,
            'analytics_v2': False,  # New analytics engine
            'auto_expiration': False
        }
    
    def is_enabled(self, feature, user_id=None):
        if not self.flags.get(feature, False):
            return False
        
        # Gradual rollout: enable for 10% of users
        if user_id:
            return hash(user_id) % 100 < 10
        
        return True
```

---

## 18. Performance Optimization

### Caching Strategy

**Multi-Level Cache**
```python
class MultiLevelCache:
    def __init__(self):
        self.l1_cache = {}  # In-memory (local to each server)
        self.l2_cache = Redis()  # Distributed cache
    
    def get(self, short_code):
        # L1 cache (fastest)
        if short_code in self.l1_cache:
            return self.l1_cache[short_code]
        
        # L2 cache (fast)
        value = self.l2_cache.get(short_code)
        if value:
            self.l1_cache[short_code] = value  # Populate L1
            return value
        
        # Database (slowest)
        value = self.database.get(short_code)
        if value:
            self.l2_cache.set(short_code, value, ttl=3600)
            self.l1_cache[short_code] = value
        
        return value
```

### Connection Pooling
```python
from sqlalchemy import create_engine
from sqlalchemy.pool import QueuePool

engine = create_engine(
    'postgresql://user:pass@host/db',
    poolclass=QueuePool,
    pool_size=20,        # Max connections
    max_overflow=10,     # Extra connections during spike
    pool_timeout=30,     # Wait time for connection
    pool_recycle=3600    # Recycle connections every hour
)
```

### Batch Processing for Analytics
```python
class AnalyticsBatcher:
    def __init__(self, batch_size=1000, flush_interval=5):
        self.batch = []
        self.batch_size = batch_size
        self.flush_interval = flush_interval
        self.last_flush = time.time()
    
    def record_click(self, short_code, metadata):
        self.batch.append({
            'short_code': short_code,
            'timestamp': time.time(),
            'metadata': metadata
        })
        
        # Flush if batch full or time elapsed
        if len(self.batch) >= self.batch_size or \
           time.time() - self.last_flush > self.flush_interval:
            self.flush()
    
    def flush(self):
        if not self.batch:
            return
        
        # Bulk insert to database
        self.database.bulk_insert('analytics', self.batch)
        self.batch = []
        self.last_flush = time.time()
```

### Database Query Optimization
```sql
-- Bad: Sequential scan
SELECT * FROM urls WHERE long_url = 'https://example.com';

-- Good: Index scan
CREATE INDEX idx_long_url_hash ON urls(MD5(long_url));
SELECT * FROM urls WHERE MD5(long_url) = MD5('https://example.com');

-- Better: Covering index
CREATE INDEX idx_short_code_long_url ON urls(short_code, long_url);
SELECT long_url FROM urls WHERE short_code = 'abc123';
```

---

## 19. Capacity Planning

### Growth Projections

**Current State (Month 0)**
- URLs created: 100M
- Daily active URLs: 10M
- Requests/second: 1,000 RPS

**6-Month Projection**
- URLs created: 500M (5x growth)
- Daily active URLs: 50M (5x growth)
- Requests/second: 5,000 RPS (5x growth)

**12-Month Projection**
- URLs created: 1B (10x growth)
- Daily active URLs: 100M (10x growth)
- Requests/second: 10,000 RPS (10x growth)

### Scaling Triggers

**Horizontal Scaling (Add More Servers)**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: url-shortener-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: url-shortener
  minReplicas: 10
  maxReplicas: 100
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

**Database Scaling**
- **Vertical:** Upgrade to larger instance when CPU > 70%
- **Horizontal:** Add read replicas when read latency > 50ms
- **Sharding:** Shard when single database > 1TB

### Resource Allocation (12-Month Projection)

**Compute**
- API servers: 100 instances (t3.medium)
- Cost: $3,000/month

**Storage**
- DynamoDB: 1TB storage, 5,000 RCU, 500 WCU
- Cost: $1,500/month

**Cache**
- Redis: 3 instances (cache.r5.xlarge)
- Cost: $900/month

**Network**
- CDN: 50 TB/month
- Cost: $4,000/month

**Total Projected Cost:** $9,400/month

### Bottleneck Analysis

**Current Bottlenecks**
1. **Database Write Capacity:** Limited to 100 WCU
   - **Solution:** Increase to 500 WCU or use DynamoDB On-Demand

2. **Cache Memory:** 16 GB cache can hold ~16M URLs
   - **Solution:** Upgrade to 64 GB cache

3. **KGS Throughput:** Single KGS can generate 10,000 keys/sec
   - **Solution:** Add 2 more KGS instances

---

## 20. Interview Cheat Sheet

### Key Numbers to Remember
- **Scale:** 100M URLs/month, 1,000 RPS
- **Storage:** 1KB per URL = 100 GB/month
- **Cache:** 80% hit rate, 16 GB Redis
- **Latency:** <10ms redirect, <50ms create
- **Availability:** 99.9% (3 nines)

### Core Components (5)
1. **API Servers:** Handle HTTP requests, stateless
2. **KGS (Key Generation Service):** Pre-generate unique keys
3. **Database (DynamoDB):** Store short_code → long_url mapping
4. **Cache (Redis):** Cache hot URLs (80/20 rule)
5. **Analytics Pipeline:** Kafka → Flink → Data warehouse

### Critical Trade-offs

**1. Base62 vs UUID**
- Base62: Shorter (7 chars), sequential, predictable
- UUID: Longer (36 chars), random, unpredictable
- **Choice:** Base62 for user-friendly URLs

**2. 301 vs 302 Redirect**
- 301: Permanent, cached by browser, no analytics
- 302: Temporary, not cached, enables analytics
- **Choice:** 302 for analytics, 301 for performance

**3. SQL vs NoSQL**
- SQL: ACID, joins, complex queries
- NoSQL: Horizontal scaling, high throughput
- **Choice:** NoSQL (DynamoDB) for scale

**4. Synchronous vs Asynchronous Analytics**
- Sync: Accurate, slow redirects
- Async: Fast redirects, eventual consistency
- **Choice:** Async (Kafka) for performance

### Common Follow-up Questions

**Q1: How do you handle custom aliases?**
A: Check if alias exists before creating. Use unique constraint on short_code column.

**Q2: How do you prevent abuse (spam URLs)?**
A: Rate limiting (100 URLs/user/day), CAPTCHA, URL validation, blacklist domains.

**Q3: How do you handle URL expiration?**
A: Add `expires_at` column. Background job deletes expired URLs daily. Return 404 for expired links.

**Q4: How do you scale to 1 billion URLs?**
A: Shard database by hash(short_code), use DynamoDB On-Demand, add more cache nodes.

**Q5: How do you ensure high availability?**
A: Multi-region deployment, database replication, automatic failover, CDN for static content.

### Interview Talking Points
- "I chose Base62 encoding because it creates short, user-friendly URLs"
- "The bottleneck is database writes, which I addressed with DynamoDB's auto-scaling"
- "To scale to 1B URLs, I would shard the database and add more cache nodes"
- "I decoupled analytics using Kafka to avoid slowing down redirects"

---

## 21. Wrap-up (Deep Dive)

**Summary:**
"I designed a distributed URL shortener capable of handling 100M writes/month.
1.  **Core Logic:** I chose **Base62 encoding** with a **Key Generation Service (KGS)** to guarantee uniqueness without collision checks.
2.  **Storage:** I used a **NoSQL store** (like DynamoDB) for horizontal scalability, sharded by the hash of the short code.
3.  **Performance:** I implemented a **Redis Cache** with LRU eviction to handle the '80/20' traffic pattern, ensuring < 10ms latency for hot links.
4.  **Analytics:** I decoupled analytics using **Kafka**, allowing us to track clicks without slowing down the user experience, similar to Bitly's architecture.
5.  **Production-Ready:** I added comprehensive failure handling, monitoring, security, and cost optimization to ensure the system is enterprise-grade."

---

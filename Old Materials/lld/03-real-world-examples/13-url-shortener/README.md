# URL Shortener (TinyURL) - LLD Interview Problem

## 📋 Problem Statement

Design a URL shortening service like TinyURL or Bitly that:
- Converts long URLs to short URLs
- Redirects short URLs to original URLs
- Supports custom aliases
- Tracks click analytics
- Handles expiration

## 🎯 Key Requirements

### Functional Requirements:
1. Generate short URL for long URL
2. Redirect short URL to original URL
3. Custom short URL aliases
4. URL expiration
5. Analytics (click count, last accessed)
6. Delete/update URLs
7. User-based URL management

### Non-Functional Requirements:
1. High availability
2. Low latency for redirects
3. Scalable (billions of URLs)
4. No collisions in short URLs
5. Durability (no data loss)

## 🏗️ Design Components

### Core Classes:
1. **URL** - URL entity
2. **URLShortener** - Main service
3. **Base62Encoder** - Encoding utility
4. **URLRepository** - Storage
5. **Analytics** - Click tracking
6. **User** - User management

## 🎨 Design Patterns Used

1. **Singleton Pattern** - Single URL shortener instance
2. **Factory Pattern** - URL creation
3. **Strategy Pattern** - Different encoding strategies
4. **Repository Pattern** - Data access abstraction

## 💡 SOLID Principles Applied

- **SRP**: Separate encoding, storage, analytics
- **OCP**: Extensible encoding algorithms
- **DIP**: Depend on repository interface

## 🔍 Interview Discussion Points

1. **Encoding Algorithm**:
   - Base62 vs Base64
   - Hash collision handling
   - Counter-based vs hash-based

2. **Scalability**:
   - Database sharding
   - Caching strategy (Redis)
   - Load balancing
   - CDN for popular URLs

3. **URL Length**:
   - 6 characters = 62^6 = 56 billion URLs
   - 7 characters = 62^7 = 3.5 trillion URLs

4. **Storage**:
   - SQL vs NoSQL
   - Cassandra for writes
   - Redis for reads

5. **Analytics**:
   - Real-time vs batch processing
   - Click tracking without affecting latency

6. **Collision Handling**:
   - Retry with different hash
   - Append counter
   - Use UUID

## 📊 Estimation

For 100M URLs/day:
- Storage: ~100 bytes/URL × 100M = 10 GB/day
- 5 years: ~18 TB
- Read:Write = 100:1
- Peak QPS: ~1000/sec

## 🔒 Security Considerations

- Rate limiting per user
- Spam detection
- Malicious URL filtering
- CAPTCHA for public API

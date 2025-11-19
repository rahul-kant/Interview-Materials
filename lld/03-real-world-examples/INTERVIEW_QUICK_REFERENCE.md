# Interview Quick Reference Guide

## 🎯 Quick Overview of All 10 Systems

---

## 1. Vending Machine 📦

**One-Line Summary:** State-based machine that accepts money, dispenses products, and returns change.

**Core Classes:**
```
VendingMachine (Singleton)
├── Inventory
├── State (Interface)
│   ├── IdleState
│   ├── AcceptingMoneyState
│   └── DispensingState
├── Product
└── Coin (Enum)
```

**Key Points for Interview:**
- Use State Pattern for clean state transitions
- Singleton for single machine instance
- Money in cents to avoid floating point
- Thread-safe inventory management
- Change calculation algorithm

**Common Questions:**
1. Q: How to handle concurrent requests?
   A: Synchronize critical sections, use ConcurrentHashMap for inventory

2. Q: How to add new products dynamically?
   A: Factory pattern for product creation, configuration file for products

3. Q: How to handle partial payments?
   A: Track current balance in AcceptingMoneyState, allow multiple coin insertions

**Time to Code:** 30-40 minutes

---

## 2. ATM Machine 🏧

**One-Line Summary:** Secure financial transaction system with authentication, balance, withdraw, deposit.

**Core Classes:**
```
ATM
├── Card
├── Account
├── State (Interface)
│   ├── IdleState
│   ├── CardInsertedState
│   ├── PinVerifiedState
│   └── TransactionState
├── Transaction (Interface)
│   ├── WithdrawTransaction
│   ├── DepositTransaction
│   └── BalanceInquiry
└── CashDispenser
```

**Key Points for Interview:**
- State Pattern for ATM states
- Factory Pattern for transactions
- PIN security (encryption, 3 attempts max)
- Cash denomination optimization
- Transaction atomicity (all or nothing)

**Common Questions:**
1. Q: How to handle concurrent withdrawals?
   A: Database transactions with optimistic/pessimistic locking

2. Q: How to optimize cash denomination?
   A: Greedy algorithm or dynamic programming

3. Q: What if network fails during transaction?
   A: Implement timeout, rollback mechanism, transaction log

**Time to Code:** 40-50 minutes

---

## 3. Elevator System 🛗

**One-Line Summary:** Multi-elevator control with optimized request scheduling.

**Core Classes:**
```
ElevatorSystem
├── Elevator[]
│   ├── currentFloor
│   ├── direction (UP/DOWN/IDLE)
│   └── requests[]
├── Request
│   ├── floor
│   └── direction
├── Scheduler (Strategy)
│   ├── FCFSScheduler
│   ├── SCANScheduler
│   └── LOOKScheduler
└── Door
```

**Key Points for Interview:**
- Strategy Pattern for different algorithms
- State Pattern for elevator states
- Request optimization for efficiency
- Load balancing across elevators

**Common Questions:**
1. Q: Which scheduling algorithm is best?
   A: SCAN/LOOK for efficiency, FCFS for fairness, depends on building type

2. Q: How to handle emergency?
   A: Priority queue, interrupt current requests, safety checks

3. Q: How to optimize for peak hours?
   A: Zone elevators, predict patterns, dynamic allocation

**Time to Code:** 45-60 minutes

---

## 4. Movie Ticket Booking 🎬

**One-Line Summary:** Concurrent seat booking system with locking mechanism.

**Core Classes:**
```
BookingSystem
├── Theater[]
│   └── Screen[]
│       └── Show[]
│           └── Seat[][]
├── Booking
├── Payment
└── SeatLock
    ├── userId
    ├── expiryTime
    └── seats[]
```

**Key Points for Interview:**
- Observer Pattern for seat availability
- Pessimistic locking for concurrent booking
- Time-bound seat holds (10 minutes)
- Payment gateway integration
- Cancellation with refund

**Common Questions:**
1. Q: How to handle concurrent seat selection?
   A: Implement seat locking with TTL, optimistic locking in DB

2. Q: What if payment fails after seat locked?
   A: Release lock, implement retry mechanism, compensation logic

3. Q: How to scale for multiple cities?
   A: Shard by city, separate databases, caching layer

**Time to Code:** 50-60 minutes

---

## 5. Hotel Booking System 🏨

**One-Line Summary:** Search, book, and manage hotel reservations with availability tracking.

**Core Classes:**
```
HotelBookingSystem
├── Hotel[]
│   └── Room[]
│       ├── roomType
│       └── amenities[]
├── Booking
│   ├── checkIn/checkOut
│   └── rooms[]
├── SearchService
│   └── filters[]
├── AvailabilityManager
└── Payment
```

**Key Points for Interview:**
- Builder Pattern for complex searches
- Strategy Pattern for pricing
- Real-time availability tracking
- Overbooking management
- Dynamic pricing based on demand

**Common Questions:**
1. Q: How to handle room availability?
   A: Calendar-based tracking, block dates on booking, release on cancellation

2. Q: How to implement dynamic pricing?
   A: Strategy pattern with factors: season, occupancy, events, demand

3. Q: What if overbooked?
   A: Intentional overbooking (like airlines), alternative room, compensation

**Time to Code:** 45-55 minutes

---

## 6. Ride Sharing (Uber/Lyft) 🚗

**One-Line Summary:** Match riders with nearby drivers, track in real-time, calculate dynamic fares.

**Core Classes:**
```
RideSharingSystem
├── User
│   ├── Rider
│   └── Driver
├── Ride
│   ├── pickup/dropoff
│   ├── status (enum)
│   └── rideType
├── Location (GPS)
├── MatchingService
│   └── strategy (closest, highest rated, etc.)
├── PricingStrategy
│   ├── BasePricing
│   ├── SurgePricing
│   └── PoolPricing
└── Rating
```

**Key Points for Interview:**
- Strategy Pattern for matching and pricing
- State Pattern for ride states
- Observer Pattern for location updates
- Real-time driver matching
- Surge pricing algorithm

**Common Questions:**
1. Q: How to match driver with rider?
   A: Geospatial indexing (QuadTree, Geohash), find drivers within radius

2. Q: How to implement surge pricing?
   A: Monitor demand/supply ratio, apply multiplier, notify users

3. Q: How to calculate ETA?
   A: Distance + traffic data + historical patterns, update in real-time

**Time to Code:** 50-65 minutes

---

## 7. Online Shopping (E-Commerce) 🛒

**One-Line Summary:** Complete e-commerce platform with catalog, cart, orders, and inventory.

**Core Classes:**
```
ECommerceSystem
├── User (Customer/Seller)
├── Product
│   └── Category
├── ShoppingCart
│   └── CartItem[]
├── Order
│   ├── orderItems[]
│   └── status (enum)
├── Inventory
│   └── stock tracking
├── Payment
│   └── strategies[]
└── Shipment
    └── tracking
```

**Key Points for Interview:**
- Factory Pattern for products
- Decorator Pattern for discounts/offers
- Observer Pattern for order notifications
- Inventory consistency (ACID)
- Search and recommendation

**Common Questions:**
1. Q: How to handle inventory consistency?
   A: Database transactions, optimistic locking, reservation pattern

2. Q: How to apply multiple discounts?
   A: Decorator/Chain of Responsibility, define stacking rules

3. Q: How to scale search?
   A: ElasticSearch, caching, faceted search, personalization

**Time to Code:** 60+ minutes (very broad)

---

## 8. Social Media News Feed 📱

**One-Line Summary:** Generate personalized feed with ranking, likes, comments, and real-time updates.

**Core Classes:**
```
SocialMediaSystem
├── User
│   └── followers/following[]
├── Post
│   ├── content
│   ├── likes[]
│   └── comments[]
├── Feed
│   └── posts[] (ranked)
├── FeedGenerator
│   └── strategy (pull/push/hybrid)
├── RankingAlgorithm
│   └── factors[] (recency, engagement, relevance)
├── Friendship (graph)
└── Notification
```

**Key Points for Interview:**
- Strategy Pattern for feed generation and ranking
- Observer Pattern for notifications
- Composite Pattern for nested comments
- Fan-out on write vs read
- Caching strategy

**Common Questions:**
1. Q: Pull vs push vs hybrid for feed?
   A: Pull: fetch on demand (slow, fresh). Push: precompute (fast, stale). Hybrid: push for active, pull for inactive

2. Q: How to handle celebrity users?
   A: Don't fan-out for celebrities, fetch on-demand, cache heavily

3. Q: How to rank posts?
   A: Weighted score: recency + engagement + relationship strength + content type

**Time to Code:** 55-70 minutes

---

## 9. LRU Cache 💾

**One-Line Summary:** Fixed-size cache with O(1) operations, evicting least recently used items.

**Core Classes:**
```
LRUCache<K, V>
├── HashMap<K, Node>  // O(1) lookup
├── DoublyLinkedList  // O(1) reorder
│   ├── head (MRU)
│   └── tail (LRU)
└── capacity
```

**Key Points for Interview:**
- HashMap for O(1) access
- Doubly Linked List for O(1) removal/insertion
- Dummy head/tail to avoid null checks
- Move to front on access
- Evict from tail when full

**Common Questions:**
1. Q: Why HashMap + Doubly Linked List?
   A: HashMap for fast lookup, DLL for maintaining order with O(1) removal

2. Q: Why not just HashMap?
   A: Can't efficiently track access order (need to iterate)

3. Q: Why not just LinkedHashMap?
   A: LinkedHashMap is actually implemented this way! Good to mention in interview

4. Q: How to make thread-safe?
   A: Synchronized methods, ReentrantReadWriteLock, or ConcurrentHashMap with custom sync

**Variations:**
- LFU Cache (Least Frequently Used)
- Time-based eviction
- Size-based eviction (not just count)

**Time to Code:** 20-30 minutes

---

## 10. Rate Limiter ⏱️

**One-Line Summary:** Control request rate per user/IP using token bucket or sliding window.

**Core Classes:**
```
RateLimiter (Interface)
├── TokenBucketRateLimiter
│   ├── capacity (max burst)
│   ├── refillRate (tokens/sec)
│   └── Map<User, Bucket>
└── SlidingWindowRateLimiter
    ├── maxRequests
    ├── windowSize
    └── Map<User, Queue<Timestamp>>
```

**Key Points for Interview:**
- Strategy Pattern for different algorithms
- Token Bucket: allows bursts, smooth refill
- Sliding Window: more accurate, memory intensive
- Thread-safe operations
- Distributed rate limiting (Redis)

**Common Questions:**
1. Q: Token Bucket vs Sliding Window?
   A: TB allows bursts, simple, memory efficient. SW precise, no burst, memory intensive

2. Q: How to distribute across servers?
   A: Centralized (Redis/DB), sticky sessions, approximate (sync periodically)

3. Q: What to return when rate limited?
   A: HTTP 429, headers: X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset

**Algorithms Comparison:**

| Algorithm | Burst Handling | Accuracy | Memory | Complexity |
|-----------|----------------|----------|--------|------------|
| Token Bucket | ✅ Yes | Good | Low | O(1) |
| Sliding Window | ❌ No | Excellent | High | O(N) |
| Fixed Window | ⚠️ Boundary issue | Poor | Low | O(1) |
| Leaky Bucket | ❌ No | Good | Low | O(1) |

**Time to Code:** 25-35 minutes

---

## 🎯 Interview Strategy by Time Available

### 45 minutes:
1. Clarify requirements (5 min)
2. High-level design (10 min)
3. Core classes and relationships (10 min)
4. Code 2-3 main classes (15 min)
5. Discuss tradeoffs (5 min)

### 60 minutes:
- Add more complete implementation
- Add error handling
- Discuss scalability
- Write some test cases

---

## 💡 Common Design Patterns Quick Reference

| Pattern | When to Use | Example |
|---------|-------------|---------|
| **Singleton** | One instance needed | VendingMachine, ATM |
| **Factory** | Create objects without specifying exact class | Transaction types |
| **Builder** | Complex object construction | Search queries, Orders |
| **Strategy** | Interchangeable algorithms | Pricing, Matching, Ranking |
| **State** | Behavior changes with state | Vending Machine, ATM, Ride |
| **Observer** | Notify multiple objects | Seat availability, Notifications |
| **Decorator** | Add functionality dynamically | Discounts, Caching |
| **Facade** | Simplify complex system | Feed generation, Booking |

---

## 🚦 Difficulty Ranking

**Easy (20-30 min):**
- LRU Cache ⭐

**Medium (30-45 min):**
- Vending Machine ⭐⭐
- Rate Limiter ⭐⭐
- ATM Machine ⭐⭐

**Hard (45-60 min):**
- Elevator System ⭐⭐⭐
- Movie Booking ⭐⭐⭐
- Hotel Booking ⭐⭐⭐
- Ride Sharing ⭐⭐⭐

**Very Hard (60+ min):**
- E-Commerce ⭐⭐⭐⭐
- Social Media Feed ⭐⭐⭐⭐

---

## 📊 Company-Specific Preferences

**Google:** Elevator, LRU Cache, Rate Limiter
**Amazon:** E-Commerce (obviously), Inventory systems
**Microsoft:** Elevator, Calendar/Meeting Room booking
**Meta/Facebook:** Social Media Feed, Messenger
**Uber:** Ride Sharing, Location services
**Netflix:** Recommendation system, Content delivery
**Airbnb:** Hotel Booking, Search systems

---

## ✅ Pre-Interview Checklist

Before the interview, make sure you can:

- [ ] Explain each design pattern and when to use it
- [ ] Draw class diagrams quickly
- [ ] Code without IDE (whiteboard/shared editor)
- [ ] Discuss time/space complexity
- [ ] Handle edge cases
- [ ] Explain tradeoffs
- [ ] Scale the system (if asked)
- [ ] Make your code extensible

---

## 🎤 Interview Communication Template

### 1. **Clarify Requirements** (5 min)
"Let me make sure I understand the requirements..."
- Functional requirements?
- Non-functional requirements?
- Scale (users, QPS)?
- Focus areas (performance, scalability, accuracy)?

### 2. **High-Level Design** (10 min)
"I'll start with a high-level design..."
- Core entities
- Main workflows
- Design patterns to use
- Tradeoffs

### 3. **Detailed Design** (20 min)
"Now let me dive into the implementation..."
- Class diagram
- Key methods
- Data structures
- Algorithms

### 4. **Code** (15 min)
"Let me implement the core components..."
- Clean, readable code
- Think aloud
- Handle edge cases

### 5. **Discussion** (10 min)
"Some things we could improve..."
- Scalability
- Edge cases
- Alternative approaches
- Performance optimization

---

**Remember:** It's not about memorizing solutions, it's about demonstrating your thought process and design skills!

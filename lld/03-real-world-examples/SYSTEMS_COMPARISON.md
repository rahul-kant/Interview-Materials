# Systems Comparison Table

## 📊 Quick Comparison of All 10 Systems

| # | System | Difficulty | Time | Key Pattern | Main Focus | Interview Frequency |
|---|--------|------------|------|-------------|------------|---------------------|
| 1 | Vending Machine | ⭐⭐ Medium | 30-40 min | State | State transitions | ⭐⭐⭐ Very High |
| 2 | ATM Machine | ⭐⭐ Medium-High | 40-50 min | State | Security, Transactions | ⭐⭐ High |
| 3 | Elevator | ⭐⭐⭐ High | 45-60 min | Strategy | Scheduling algorithms | ⭐⭐⭐ Very High |
| 4 | Movie Booking | ⭐⭐⭐ High | 50-60 min | Observer | Concurrency | ⭐⭐ High |
| 5 | Hotel Booking | ⭐⭐⭐ High | 45-55 min | Builder | Search, Availability | ⭐⭐ High |
| 6 | Ride Sharing | ⭐⭐⭐ High | 50-65 min | Strategy | Real-time matching | ⭐⭐ High |
| 7 | E-Commerce | ⭐⭐⭐⭐ Very High | 60+ min | Multiple | Complete platform | ⭐ Medium |
| 8 | Social Media | ⭐⭐⭐⭐ Very High | 55-70 min | Strategy | Feed generation | ⭐ Medium |
| 9 | LRU Cache | ⭐ Easy-Medium | 20-30 min | - | Data structures | ⭐⭐⭐ Extremely High |
| 10 | Rate Limiter | ⭐⭐ Medium | 25-35 min | Strategy | Algorithms | ⭐⭐⭐ Very High |

---

## 🎯 Detailed Comparison

### 1. Vending Machine 📦
| Aspect | Details |
|--------|---------|
| **Core Challenge** | State management and transitions |
| **Key Classes** | VendingMachine, State, Product, Inventory |
| **Design Patterns** | State, Singleton |
| **Data Structures** | HashMap (inventory), Enum (states) |
| **Concurrency** | Thread-safe inventory management |
| **Scalability** | Single machine, minimal scaling needed |
| **Common Questions** | State transitions, change calculation, concurrent requests |
| **Real-World Use** | IoT devices, self-service kiosks |
| **Companies Ask** | Amazon, Microsoft, Various startups |

---

### 2. ATM Machine 🏧
| Aspect | Details |
|--------|---------|
| **Core Challenge** | Security and transaction atomicity |
| **Key Classes** | ATM, Card, Account, Transaction, CashDispenser |
| **Design Patterns** | State, Factory, Strategy |
| **Data Structures** | HashMap (accounts), Queue (transactions) |
| **Concurrency** | Transaction locking, account synchronization |
| **Scalability** | Distributed transactions, network handling |
| **Common Questions** | Concurrent withdrawals, PIN security, rollback |
| **Real-World Use** | Banking systems, financial transactions |
| **Companies Ask** | Banks, Financial institutions, Microsoft |

---

### 3. Elevator System 🛗
| Aspect | Details |
|--------|---------|
| **Core Challenge** | Optimal scheduling and efficiency |
| **Key Classes** | ElevatorSystem, Elevator, Request, Scheduler |
| **Design Patterns** | Strategy, State, Observer |
| **Data Structures** | PriorityQueue (requests), List (elevators) |
| **Concurrency** | Multiple elevators, concurrent requests |
| **Scalability** | Load balancing across elevators |
| **Common Questions** | FCFS vs SCAN vs LOOK, peak hours, emergency |
| **Real-World Use** | Building management systems |
| **Companies Ask** | Google, Microsoft, Building automation companies |

---

### 4. Movie Ticket Booking 🎬
| Aspect | Details |
|--------|---------|
| **Core Challenge** | Concurrent seat booking without conflicts |
| **Key Classes** | Theater, Screen, Show, Seat, Booking, SeatLock |
| **Design Patterns** | Observer, Factory, Strategy |
| **Data Structures** | 2D Array (seats), HashMap (locks), Queue (bookings) |
| **Concurrency** | Pessimistic locking, time-bound holds |
| **Scalability** | Sharding by city, caching |
| **Common Questions** | Seat locking, timeout handling, overbooking |
| **Real-World Use** | BookMyShow, Fandango, event ticketing |
| **Companies Ask** | BookMyShow, Ticketing platforms, E-commerce |

---

### 5. Hotel Booking System 🏨
| Aspect | Details |
|--------|---------|
| **Core Challenge** | Search optimization and availability |
| **Key Classes** | Hotel, Room, Booking, SearchService, AvailabilityManager |
| **Design Patterns** | Builder, Strategy, Factory, Observer |
| **Data Structures** | HashMap (hotels), TreeMap (availability), Set (amenities) |
| **Concurrency** | Room locking during booking |
| **Scalability** | Search indexing, distributed availability |
| **Common Questions** | Search optimization, overbooking, dynamic pricing |
| **Real-World Use** | Booking.com, Airbnb, hotel systems |
| **Companies Ask** | Booking.com, Airbnb, OYO, travel companies |

---

### 6. Ride Sharing System 🚗
| Aspect | Details |
|--------|---------|
| **Core Challenge** | Real-time matching and location tracking |
| **Key Classes** | User, Rider, Driver, Ride, Location, MatchingService |
| **Design Patterns** | Strategy, State, Observer, Factory |
| **Data Structures** | QuadTree/Geohash (location), PriorityQueue (matching) |
| **Concurrency** | Real-time updates, concurrent ride requests |
| **Scalability** | Geospatial sharding, microservices |
| **Common Questions** | Matching algorithm, surge pricing, ETA calculation |
| **Real-World Use** | Uber, Lyft, Ola, ride-hailing apps |
| **Companies Ask** | Uber, Lyft, Ola, location-based services |

---

### 7. Online Shopping System 🛒
| Aspect | Details |
|--------|---------|
| **Core Challenge** | Inventory consistency and order management |
| **Key Classes** | Product, Cart, Order, Inventory, Payment, Shipment |
| **Design Patterns** | Factory, Decorator, Observer, Builder, Strategy |
| **Data Structures** | HashMap (catalog), List (cart), Queue (orders) |
| **Concurrency** | Inventory locking, order processing |
| **Scalability** | Microservices, distributed inventory, caching |
| **Common Questions** | Inventory consistency, cart abandonment, recommendations |
| **Real-World Use** | Amazon, Flipkart, all e-commerce platforms |
| **Companies Ask** | Amazon, E-commerce companies |

---

### 8. Social Media Feed 📱
| Aspect | Details |
|--------|---------|
| **Core Challenge** | Feed generation and ranking at scale |
| **Key Classes** | User, Post, Feed, FeedGenerator, RankingAlgorithm |
| **Design Patterns** | Strategy, Observer, Composite, Facade |
| **Data Structures** | Graph (social network), PriorityQueue (ranking), Cache |
| **Concurrency** | Real-time updates, concurrent posts |
| **Scalability** | Fan-out on write/read, sharding, caching |
| **Common Questions** | Feed generation, fan-out, ranking algorithm, celebrities |
| **Real-World Use** | Facebook, Twitter, Instagram, social platforms |
| **Companies Ask** | Meta, Twitter, LinkedIn, social media companies |

---

### 9. LRU Cache 💾
| Aspect | Details |
|--------|---------|
| **Core Challenge** | O(1) operations with fixed capacity |
| **Key Classes** | LRUCache, Node (doubly linked list) |
| **Design Patterns** | - (data structure problem) |
| **Data Structures** | HashMap + Doubly Linked List |
| **Concurrency** | Optional thread-safety with synchronized |
| **Scalability** | Distributed cache (Redis), sharding |
| **Common Questions** | Why HashMap+DLL? Other eviction policies? Thread-safety? |
| **Real-World Use** | Redis, Memcached, browser cache, CPU cache |
| **Companies Ask** | EVERYONE! Most common LLD question |

---

### 10. Rate Limiter ⏱️
| Aspect | Details |
|--------|---------|
| **Core Challenge** | Accurate rate limiting with performance |
| **Key Classes** | RateLimiter, TokenBucketLimiter, SlidingWindowLimiter |
| **Design Patterns** | Strategy, Factory, Decorator, Singleton |
| **Data Structures** | HashMap (user buckets), Queue (timestamps) |
| **Concurrency** | Thread-safe bucket updates |
| **Scalability** | Distributed limiting (Redis), approximate algorithms |
| **Common Questions** | Token bucket vs sliding window? Distributed? Race conditions? |
| **Real-World Use** | API gateways, DDoS protection, resource control |
| **Companies Ask** | Google, Amazon, Microsoft, cloud platforms |

---

## 🎨 Design Patterns Summary

| Pattern | Used In | Purpose |
|---------|---------|---------|
| **Singleton** | Vending Machine, ATM, Rate Limiter | Single instance management |
| **State** | Vending Machine, ATM, Elevator, Ride Sharing | State-dependent behavior |
| **Strategy** | Elevator, Rate Limiter, Ride Sharing, Hotel, E-Commerce, Social Media | Interchangeable algorithms |
| **Factory** | ATM, Movie Booking, Hotel, E-Commerce | Object creation |
| **Observer** | Elevator, Movie Booking, Hotel, E-Commerce, Social Media | Event notification |
| **Builder** | Hotel (search), E-Commerce (orders) | Complex object construction |
| **Decorator** | E-Commerce (discounts), Rate Limiter (distributed) | Add functionality dynamically |
| **Facade** | Social Media (feed generation) | Simplify complex subsystems |
| **Composite** | Social Media (nested comments) | Tree structures |

---

## 🔧 Technical Complexity

### Data Structure Complexity:

| System | Primary Data Structures | Complexity Operations |
|--------|-------------------------|----------------------|
| Vending Machine | HashMap, Enum | O(1) lookup, O(1) state change |
| ATM | HashMap, Queue | O(1) account lookup, O(n) transaction history |
| Elevator | PriorityQueue, List | O(log n) request insertion, O(1) elevator selection |
| Movie Booking | 2D Array, HashMap | O(1) seat lookup, O(n) availability check |
| Hotel Booking | TreeMap, HashSet | O(log n) availability, O(n) search/filter |
| Ride Sharing | QuadTree/Geohash | O(log n) nearby drivers, O(1) location update |
| E-Commerce | HashMap, List, Queue | O(1) product lookup, O(n) search |
| Social Media | Graph, PriorityQueue | O(V+E) graph traversal, O(log n) ranking |
| LRU Cache | HashMap, DoublyLinkedList | O(1) get, O(1) put |
| Rate Limiter | HashMap, Queue | O(1) token check, O(n) window cleanup |

---

## 🎯 Interview Focus Areas

### Must Know (Practice First):
1. **LRU Cache** - Almost guaranteed to be asked
2. **Rate Limiter** - Very common in system design
3. **Vending Machine** - Classic state machine problem

### Should Know (Frequently Asked):
4. **Elevator System** - Tests algorithm thinking
5. **Movie Booking** - Tests concurrency understanding
6. **Ride Sharing** - Tests real-time systems

### Good to Know (Sometimes Asked):
7. **Hotel Booking** - Tests search and availability
8. **ATM Machine** - Tests security and transactions
9. **E-Commerce** - Tests overall design skills
10. **Social Media** - Tests scalability thinking

---

## 💼 Company Preferences

| Company | Prefers | Reasoning |
|---------|---------|-----------|
| **Google** | Elevator, LRU Cache, Rate Limiter | Algorithm-heavy, scalability |
| **Amazon** | E-Commerce, Vending Machine, LRU Cache | Domain-relevant, practical |
| **Microsoft** | Elevator, ATM, Calendar/Meeting Room | Enterprise systems |
| **Meta** | Social Media Feed, Messaging, LRU Cache | Social features |
| **Uber** | Ride Sharing, Location services, Rate Limiter | Core business |
| **Netflix** | Recommendation, LRU Cache, Rate Limiter | Streaming, caching |
| **Airbnb** | Hotel Booking, Search systems | Core business |
| **Booking.com** | Hotel Booking, Travel systems | Core business |

---

## 📊 Implementation Status

| System | Code | Design Doc | Demo | Interview Guide |
|--------|------|------------|------|-----------------|
| Vending Machine | ✅ | ✅ | ✅ | ✅ |
| ATM Machine | ⏳ | ✅ | ⏳ | ✅ |
| Elevator | ⏳ | ✅ | ⏳ | ✅ |
| Movie Booking | ⏳ | ✅ | ⏳ | ✅ |
| Hotel Booking | ⏳ | ✅ | ⏳ | ✅ |
| Ride Sharing | ⏳ | ✅ | ⏳ | ✅ |
| E-Commerce | ⏳ | ✅ | ⏳ | ✅ |
| Social Media | ⏳ | ✅ | ⏳ | ✅ |
| LRU Cache | ✅ | ✅ | ✅ | ✅ |
| Rate Limiter | ✅ | ✅ | ✅ | ✅ |

**Legend:**
- ✅ Complete
- ⏳ Design document available, ready for implementation
- ❌ Not started

---

## 🚀 Learning Path by Difficulty

### Level 1: Beginner (Start Here)
1. **LRU Cache** - Learn data structures
   - Time: 2-3 hours to master
   - Focus: HashMap + Doubly Linked List

2. **Rate Limiter** - Learn algorithms
   - Time: 3-4 hours to master
   - Focus: Token bucket, Sliding window

### Level 2: Intermediate
3. **Vending Machine** - Learn State pattern
   - Time: 4-5 hours to master
   - Focus: State transitions, clean code

4. **ATM Machine** - Learn transactions
   - Time: 4-6 hours to master
   - Focus: Security, atomicity

### Level 3: Advanced
5. **Elevator System** - Learn scheduling
   - Time: 6-8 hours to master
   - Focus: Algorithm optimization

6. **Movie Booking** - Learn concurrency
   - Time: 6-8 hours to master
   - Focus: Locking mechanisms

7. **Ride Sharing** - Learn real-time systems
   - Time: 8-10 hours to master
   - Focus: Geospatial, matching

### Level 4: Expert
8. **Hotel Booking** - Complex search
   - Time: 8-10 hours to master
   - Focus: Search optimization, availability

9. **E-Commerce** - Complete platform
   - Time: 10-12 hours to master
   - Focus: End-to-end flow, microservices

10. **Social Media** - Massive scale
    - Time: 10-12 hours to master
    - Focus: Feed generation, scalability

---

## 🎓 Total Preparation Time

**Minimum (Just to understand):** 20-30 hours
- Study all READMEs: 5 hours
- Run all demos: 2 hours
- Understand patterns: 5 hours
- Practice explaining: 8-10 hours

**Recommended (To be interview-ready):** 50-70 hours
- Deep study of all systems: 20 hours
- Code all systems yourself: 30-40 hours
- Mock interviews: 10 hours

**Ideal (To be expert):** 100+ hours
- Master all implementations
- Try variations and extensions
- Teach others
- Contribute improvements

---

## ✅ Self-Assessment Checklist

After studying these systems, you should be able to answer:

**Design Questions:**
- [ ] When to use Singleton vs Factory?
- [ ] State vs Strategy pattern?
- [ ] How to handle concurrency?
- [ ] How to optimize algorithms?
- [ ] How to scale systems?

**Implementation Questions:**
- [ ] Code LRU Cache in 20 minutes?
- [ ] Implement any state machine in 30 minutes?
- [ ] Design elevator scheduler in 40 minutes?
- [ ] Handle concurrent booking in 50 minutes?

**Discussion Questions:**
- [ ] Explain tradeoffs between approaches?
- [ ] Discuss scalability challenges?
- [ ] Handle interviewer's follow-ups?
- [ ] Suggest improvements?

---

**Use this comparison table as your study guide!**

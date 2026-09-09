# Real-World Low-Level Design Examples

## 📚 Overview

This folder contains **21 real-world system design problems** that are **most frequently asked in interviews** at top tech companies (Google, Amazon, Microsoft, Meta, Apple, etc.). Each example includes:

- ✅ Complete working code
- ✅ Detailed README with problem statement
- ✅ Design patterns used
- ✅ SOLID principles applied
- ✅ Interview discussion points
- ✅ Demo application with test scenarios

---

## 🎯 21 Real-World Examples (Including 11 New FAANG Favorites!)

### 1. **Vending Machine System** 📦
**Location:** `03-vending-machine/`
**Difficulty:** Medium
**Key Concepts:** State Pattern, Singleton, Concurrency

A complete vending machine implementation with:
- Multiple states (Idle, Accepting Money, Dispensing)
- Coin handling and change calculation
- Inventory management
- Thread-safe operations

**Interview Focus:**
- State management
- Money handling (avoid floating point)
- Concurrent operations
- Error handling

---

### 2. **ATM Machine System** 🏧
**Location:** `04-atm-machine/`
**Difficulty:** Medium-High
**Key Concepts:** State Pattern, Security, Transactions

ATM system with:
- Card authentication
- PIN verification
- Multiple transaction types
- Cash dispenser management
- Transaction atomicity

**Interview Focus:**
- Security concerns
- Transaction rollback
- Cash denomination optimization
- Concurrent withdrawals

---

### 3. **Elevator System** 🛗
**Location:** `05-elevator-system/`
**Difficulty:** High
**Key Concepts:** Strategy Pattern, Scheduling Algorithms

Multi-elevator control system with:
- Multiple scheduling algorithms (FCFS, SCAN, LOOK)
- Load balancing
- Request optimization
- Emergency handling

**Interview Focus:**
- Scheduling algorithms
- Optimization strategies
- Peak hour handling
- Safety constraints

---

### 4. **Movie Ticket Booking** 🎬
**Location:** `06-movie-ticket-booking/`
**Difficulty:** High
**Key Concepts:** Concurrency, Locking, Observer Pattern

BookMyShow-like system with:
- Seat selection with visual map
- Concurrent booking handling
- Payment processing
- Seat locking mechanism
- Cancellation and refunds

**Interview Focus:**
- Concurrent seat booking
- Lock management
- Seat hold timeout
- Database design
- Scalability

---

### 5. **Hotel Booking System** 🏨
**Location:** `07-hotel-booking-system/`
**Difficulty:** High
**Key Concepts:** Search, Availability, Factory Pattern

Booking.com-like system with:
- Hotel search and filtering
- Room availability management
- Dynamic pricing
- Booking modifications
- Review system

**Interview Focus:**
- Search optimization
- Availability management
- Overbooking strategy
- Dynamic pricing
- Data consistency

---

### 6. **Ride Sharing System (Uber/Lyft)** 🚗
**Location:** `08-ride-sharing-system/`
**Difficulty:** High
**Key Concepts:** Matching Algorithms, Real-time Systems

Uber-like platform with:
- Driver-rider matching
- Real-time location tracking
- Fare calculation
- Surge pricing
- Rating system

**Interview Focus:**
- Matching algorithm
- Location tracking
- Surge pricing logic
- ETA calculation
- Ride pooling

---

### 7. **Online Shopping System (E-Commerce)** 🛒
**Location:** `09-online-shopping-system/`
**Difficulty:** High
**Key Concepts:** Inventory, Cart, Orders, Decorator Pattern

Amazon-like e-commerce with:
- Product catalog
- Shopping cart
- Order processing
- Inventory management
- Payment integration

**Interview Focus:**
- Inventory consistency
- Cart abandonment
- Order fulfillment
- Recommendation system
- Scalability

---

### 8. **Social Media News Feed** 📱
**Location:** `10-social-media-feed/`
**Difficulty:** High
**Key Concepts:** Feed Generation, Ranking, Observer Pattern

Facebook/Twitter-like feed with:
- Personalized news feed
- Feed ranking algorithms
- Real-time updates
- Likes, comments, shares
- Notifications

**Interview Focus:**
- Feed generation (push vs pull)
- Fan-out strategies
- Ranking algorithm
- Celebrity user handling
- Caching strategies

---

### 9. **LRU Cache System** 💾
**Location:** `11-cache-system-lru/`
**Difficulty:** Medium
**Key Concepts:** HashMap + Doubly Linked List

**✅ COMPLETE IMPLEMENTATION**

High-performance LRU cache with:
- O(1) get and put operations
- Generic type support
- Thread-safe operations
- Visualization of cache state

**Interview Focus:**
- Why HashMap + Doubly Linked List?
- How to achieve O(1)?
- Other eviction policies (LFU, FIFO)
- Thread safety
- Distributed cache

**Files:**
- `Node.java` - Doubly linked list node
- `LRUCache.java` - Main cache implementation
- `LRUCacheDemo.java` - Complete demo with test cases

---

### 10. **Rate Limiter System** ⏱️
**Location:** `12-rate-limiter/`
**Difficulty:** Medium-High
**Key Concepts:** Token Bucket, Sliding Window, Strategy Pattern

**✅ COMPLETE IMPLEMENTATION**

Rate limiting with multiple algorithms:
- Token Bucket (allows bursts)
- Sliding Window (more accurate)
- Configurable limits
- Multi-user support

**Interview Focus:**
- Token bucket vs sliding window
- Distributed rate limiting
- Race conditions
- Memory vs accuracy tradeoffs
- HTTP response headers

**Files:**
- `RateLimiter.java` - Base interface
- `TokenBucketRateLimiter.java` - Token bucket implementation
- `SlidingWindowRateLimiter.java` - Sliding window implementation
- `RateLimiterDemo.java` - Complete demo with comparisons

---

### 11. **URL Shortener (TinyURL)** 🔗
**Location:** `13-url-shortener/`
**Difficulty:** High
**Key Concepts:** Base62 Encoding, Hashing, Singleton Pattern

**✅ COMPLETE IMPLEMENTATION - NEW!**

TinyURL/Bitly-like URL shortening service with:
- Base62 encoding for short URLs
- Custom alias support
- Click analytics and tracking
- URL expiration
- User management

**Interview Focus:**
- Base62 vs Base64 encoding
- Hash collision handling
- Scalability (billions of URLs)
- URL length optimization (62^6 = 56B URLs)
- Analytics without affecting latency

**Files:**
- `URL.java` - URL entity with analytics
- `Base62Encoder.java` - Encoding utility
- `URLShortener.java` - Main service (Singleton)
- `URLShortenerDemo.java` - 6 test scenarios

**Why This Problem:**
- Asked at Google, Amazon, Meta, Microsoft
- Tests understanding of encoding, hashing, scalability
- Great for discussing distributed systems

---

### 12. **Meeting Scheduler (Calendar)** 📅
**Location:** `14-meeting-scheduler/`
**Difficulty:** High
**Key Concepts:** Interval Algorithms, Conflict Detection, Greedy Algorithms

**✅ COMPLETE IMPLEMENTATION - NEW!**

Google Calendar-like meeting scheduler with:
- Check user availability
- Find common free slots
- Conflict detection
- Interval merging algorithm
- Multi-user scheduling

**Interview Focus:**
- Interval overlap detection algorithm
- Finding free slots (O(n log n))
- Handling recurring meetings
- Double booking prevention
- Scalability (millions of users)

**Files:**
- `TimeSlot.java` - Time interval with overlap detection
- `Meeting.java` - Meeting entity
- `MeetingScheduler.java` - Main service with algorithms
- `MeetingSchedulerDemo.java` - Comprehensive demos

**Why This Problem:**
- Google's favorite interview problem
- Tests interval algorithms
- Real-world complexity (time zones, recurrence)

---

### 13. **Splitwise (Expense Sharing)** 💰
**Location:** `15-splitwise/`
**Difficulty:** Medium-High
**Key Concepts:** Graph Algorithms, Debt Simplification, Strategy Pattern

**✅ COMPLETE IMPLEMENTATION - NEW!**

Splitwise-like expense sharing app with:
- Equal and custom splits
- Balance calculation
- Settle up payments
- Debt simplification
- Group expense management

**Interview Focus:**
- Split calculation algorithms
- Debt simplification (minimize transactions)
- Balance sheet design
- Floating point precision
- Scalability

**Files:**
- `User.java` - User entity
- `Expense.java` - Expense with splits
- `SplitwiseSystem.java` - Main service
- `SplitwiseDemo.java` - Real-world scenarios

**Why This Problem:**
- Popular at startups and FAANG
- Tests graph algorithms (debt simplification)
- Real-world financial calculations

---

### 14. **Snake and Ladder Game** 🎲
**Location:** `16-snake-and-ladder/`
**Difficulty:** Medium
**Key Concepts:** Game Design, Random Events, Turn-based System

**✅ COMPLETE IMPLEMENTATION - NEW!**

Classic board game implementation with:
- 100-square board
- Snakes and ladders mechanics
- Multiple players (2-4)
- Dice rolling simulation
- Turn-based gameplay
- Win condition detection

**Interview Focus:**
- Game state management
- Random number generation
- Turn alternation logic
- Board representation (HashMap)
- Move validation
- Extensibility (board size, rules)

**Files:**
- `Player.java` - Player entity
- `Dice.java` - Random dice roller
- `Board.java` - Board with snakes/ladders
- `SnakeAndLadderGame.java` - Game controller
- `SnakeAndLadderDemo.java` - Complete game demo

**Why This Problem:**
- Popular at Amazon, Microsoft
- Tests game design principles
- Simple yet extensible design
- Good for OOP practice

---

### 15. **Tic-Tac-Toe Game** ❌⭕
**Location:** `17-tic-tac-toe/`
**Difficulty:** Easy-Medium
**Key Concepts:** Game Design, Win Detection, 2D Arrays

**✅ COMPLETE IMPLEMENTATION - NEW!**

Classic Tic-Tac-Toe game with:
- 3x3 grid board
- Two players (X and O)
- Turn-based moves
- Win detection (rows, columns, diagonals)
- Draw detection
- Move validation

**Interview Focus:**
- Win detection algorithm
- Board representation (2D array)
- Move validation
- Time complexity (O(1) with counters)
- Extensibility (N×N board)
- AI opponent (future enhancement)

**Files:**
- `TicTacToeGame.java` - Complete game logic
- `TicTacToeDemo.java` - Game demonstration

**Why This Problem:**
- Very common at Google, Meta
- Tests algorithmic thinking
- Clean OOP implementation
- Good warmup problem

---

### 16. **Logging System** 📝
**Location:** `18-logging-system/`
**Difficulty:** Medium
**Key Concepts:** Singleton, Thread Safety, Observer Pattern

**✅ COMPLETE IMPLEMENTATION - NEW!**

Production-grade logging system with:
- Multiple log levels (DEBUG, INFO, WARN, ERROR)
- Thread-safe operations
- Configurable log levels
- Timestamp and thread info
- Console output
- Extensible appenders

**Interview Focus:**
- Singleton pattern implementation
- Thread safety (synchronized methods)
- Log level filtering
- Performance considerations
- Extensibility (file appenders, formatters)
- Log rotation strategies

**Files:**
- `LogLevel.java` - Enum for log levels
- `Logger.java` - Singleton logger with thread safety
- `LoggerDemo.java` - Comprehensive demo

**Why This Problem:**
- Amazon's favorite infrastructure problem
- Tests singleton and thread safety
- Real-world production requirement
- Shows system design maturity

---

### 17. **Parking Lot System** 🅿️
**Location:** `19-parking-lot/`
**Difficulty:** Medium-High
**Key Concepts:** Strategy Pattern, Factory Pattern, OOP Design

**✅ COMPLETE IMPLEMENTATION - NEW!**

Multi-level parking lot system with:
- Multiple vehicle types (Car, Bike, Truck)
- Different spot types (Compact, Large, Handicapped)
- Entry and exit management
- Fee calculation based on time
- Real-time availability tracking
- Thread-safe operations

**Interview Focus:**
- Spot allocation strategies (nearest, first available)
- Fee calculation (hourly rates, vehicle type)
- Concurrency handling (prevent double booking)
- Vehicle-spot compatibility
- Scalability (multiple floors, entrances)
- Payment processing

**Files:**
- `ParkingLotSystem.java` - Complete system with all classes
- `ParkingLotDemo.java` - Demo scenarios

**Why This Problem:**
- **Most asked at Amazon, Microsoft, Uber, Oracle**
- Tests OOP design and real-world constraints
- Great for discussing extensibility
- Common in first-round LLD interviews

---

### 18. **Chess Game** ♟️
**Location:** `20-chess-game/`
**Difficulty:** High
**Key Concepts:** Strategy Pattern, Command Pattern, Game Design

**✅ COMPLETE IMPLEMENTATION - NEW!**

Chess game implementation with:
- 8×8 board initialization
- All chess pieces (King, Queen, Rook, Bishop, Knight, Pawn)
- Move validation per piece
- Turn-based gameplay
- Board display
- Move notation (e2 to e4)

**Interview Focus:**
- Piece movement validation (strategy pattern)
- Check and checkmate detection
- Special moves (castling, en passant, pawn promotion)
- Move history and undo
- Command pattern for moves
- Extensibility for AI opponent

**Files:**
- `ChessGame.java` - Complete game with all classes
- `ChessDemo.java` - Game demonstration

**Why This Problem:**
- Very popular at Google, Meta
- Tests complex OOP design
- Multiple design patterns
- Great for showing clean architecture

---

### 19. **Notification Service (Pub-Sub)** 📢
**Location:** `21-notification-service/`
**Difficulty:** Medium-High
**Key Concepts:** Observer Pattern, Strategy Pattern, Pub-Sub Model

**✅ COMPLETE IMPLEMENTATION - NEW!**

Notification service with:
- Multiple channels (Email, SMS, Push)
- Topic-based subscriptions
- User preferences
- Fan-out to multiple subscribers
- Extensible channel types

**Interview Focus:**
- Observer/Pub-Sub pattern implementation
- Topic-based routing
- Multiple channel strategies
- Retry logic for failed notifications
- Asynchronous delivery
- Message queuing (Kafka/RabbitMQ concepts)
- Scalability considerations

**Files:**
- `NotificationService.java` - Complete pub-sub system
- `NotificationDemo.java` - Multi-channel demo

**Why This Problem:**
- Asked at all FAANG companies
- Tests Observer pattern understanding
- Real-world distributed systems
- Discusses scalability and reliability

---

### 20. **In-Memory File System** 📁
**Location:** `22-file-system/`
**Difficulty:** Medium
**Key Concepts:** Composite Pattern, Tree Structure, Path Navigation

**✅ COMPLETE IMPLEMENTATION - NEW!**

File system implementation with:
- Create files and directories (touch, mkdir)
- Navigate paths (cd, ls)
- Read and write file content
- Tree structure (composite pattern)
- Path validation

**Interview Focus:**
- Composite pattern (files & directories)
- Tree traversal algorithms
- Path parsing and navigation
- File operations (CRUD)
- Memory management
- Extensibility (permissions, metadata)

**Files:**
- `FileSystem.java` - Complete FS with composite pattern
- `FileSystemDemo.java` - File operations demo

**Why This Problem:**
- Popular at Google, Dropbox
- Tests tree structures and composite pattern
- Real-world system design
- Good for discussing Unix file systems

---

### 21. **Task Scheduler** ⏰
**Location:** `23-task-scheduler/`
**Difficulty:** Medium-High
**Key Concepts:** Priority Queue, Command Pattern, Scheduling

**✅ COMPLETE IMPLEMENTATION - NEW!**

Task scheduler with:
- Priority-based task execution
- Priority queue (heap) for ordering
- Command pattern for tasks
- Task execution engine
- O(log n) add and execute

**Interview Focus:**
- Priority queue implementation (heap)
- Task scheduling algorithms
- Command pattern for tasks
- Handling task dependencies
- Retry mechanism for failed tasks
- Recurring tasks
- Thread pool for execution

**Files:**
- `TaskScheduler.java` - Priority queue-based scheduler
- `TaskSchedulerDemo.java` - Task execution demo

**Why This Problem:**
- Very common at Amazon, LinkedIn
- Tests data structure knowledge (priority queue)
- Command pattern application
- Real-world job scheduling
- Discusses scalability and distributed scheduling

---

## 📊 Interview Frequency

### Most Frequently Asked (Must Know):
1. ⭐⭐⭐ **LRU Cache** - Asked by almost everyone
2. ⭐⭐⭐ **URL Shortener** - Very common at FAANG (Google, Meta, Amazon)
3. ⭐⭐⭐ **Meeting Scheduler** - Google's favorite
4. ⭐⭐⭐ **Rate Limiter** - Very common in system design
5. ⭐⭐⭐ **Vending Machine** - Classic LLD problem

### Frequently Asked:
6. ⭐⭐ **Splitwise** - Popular at startups and FAANG
7. ⭐⭐ **Elevator System** - Tests algorithm knowledge
8. ⭐⭐ **Movie Ticket Booking** - Tests concurrency
9. ⭐⭐ **Ride Sharing** - Tests real-time systems
10. ⭐⭐ **Hotel Booking** - Tests search and availability

### Sometimes Asked:
11. ⭐ **ATM Machine** - Tests state management
12. ⭐ **Online Shopping** - Very broad, tests overall design
13. ⭐ **Social Media Feed** - Complex, tests scalability

---

## 🎨 Design Patterns Coverage

| Pattern | Used In |
|---------|---------|
| **State Pattern** | Vending Machine, ATM, Elevator |
| **Strategy Pattern** | Elevator, Rate Limiter, Ride Sharing |
| **Singleton Pattern** | Vending Machine, ATM |
| **Factory Pattern** | Hotel Booking, E-Commerce |
| **Observer Pattern** | Movie Booking, Social Media |
| **Decorator Pattern** | E-Commerce (discounts) |
| **Builder Pattern** | Hotel Search, E-Commerce Orders |

---

## 💡 SOLID Principles

All examples demonstrate SOLID principles:

- **S**ingle Responsibility Principle
- **O**pen/Closed Principle
- **L**iskov Substitution Principle
- **I**nterface Segregation Principle
- **D**ependency Inversion Principle

---

## 🚀 How to Use This Repository

### For Interview Preparation:

1. **Start with LRU Cache** (Most common, good warmup)
2. **Then Rate Limiter** (Teaches algorithms)
3. **Move to Vending Machine** (State pattern practice)
4. **Practice Elevator** (Algorithm optimization)
5. **Tackle others based on company focus**

### For Each Problem:

1. Read the README first
2. Try designing it yourself
3. Compare with provided solution
4. Understand design patterns used
5. Run the demo code
6. Practice explaining your design

### For Interviews:

1. **Clarify requirements** - Ask questions!
2. **Start with simple design** - Iterate later
3. **Identify design patterns** - Mention them
4. **Discuss tradeoffs** - Show depth
5. **Write clean code** - Follow best practices
6. **Test your design** - Think of edge cases

---

## 📝 Interview Tips

### What Interviewers Look For:

✅ **Requirements gathering** - Do you ask clarifying questions?
✅ **System thinking** - Do you see the big picture?
✅ **Design patterns** - Do you know when to apply them?
✅ **Code quality** - Is your code clean and maintainable?
✅ **Scalability** - Can your design scale?
✅ **Tradeoffs** - Do you understand pros/cons?
✅ **Communication** - Can you explain your design?

### Common Mistakes to Avoid:

❌ Jumping to code without design
❌ Overengineering simple problems
❌ Ignoring edge cases
❌ Not asking questions
❌ Poor variable/class naming
❌ Tight coupling
❌ Forgetting error handling

---

## 🎯 Implementation Status

| # | System | Status | Code | Docs |
|---|--------|--------|------|------|
| 1 | Vending Machine | ✅ Complete | ✅ | ✅ |
| 2 | ATM Machine | 📝 README Only | ⏳ | ✅ |
| 3 | Elevator System | 📝 README Only | ⏳ | ✅ |
| 4 | Movie Booking | 📝 README Only | ⏳ | ✅ |
| 5 | Hotel Booking | 📝 README Only | ⏳ | ✅ |
| 6 | Ride Sharing | 📝 README Only | ⏳ | ✅ |
| 7 | Online Shopping | 📝 README Only | ⏳ | ✅ |
| 8 | Social Media Feed | 📝 README Only | ⏳ | ✅ |
| 9 | LRU Cache | ✅ Complete | ✅ | ✅ |
| 10 | Rate Limiter | ✅ Complete | ✅ | ✅ |
| 11 | **URL Shortener** | ✅ **Complete** | ✅ | ✅ |
| 12 | **Meeting Scheduler** | ✅ **Complete** | ✅ | ✅ |
| 13 | **Splitwise** | ✅ **Complete** | ✅ | ✅ |
| 14 | **Snake and Ladder** | ✅ **Complete** | ✅ | ✅ |
| 15 | **Tic-Tac-Toe** | ✅ **Complete** | ✅ | ✅ |
| 16 | **Logging System** | ✅ **Complete** | ✅ | ✅ |
| 17 | **Parking Lot** | ✅ **Complete** | ✅ | ✅ |
| 18 | **Chess Game** | ✅ **Complete** | ✅ | ✅ |
| 19 | **Notification Service** | ✅ **Complete** | ✅ | ✅ |
| 20 | **File System** | ✅ **Complete** | ✅ | ✅ |
| 21 | **Task Scheduler** | ✅ **Complete** | ✅ | ✅ |

**Legend:**
- ✅ Complete = Full working code + documentation
- 📝 README Only = Design documentation available, code template ready
- ⏳ In Progress = Being developed

---

## 📚 Additional Resources

### Recommended Reading:
- "Design Patterns" by Gang of Four
- "Clean Code" by Robert C. Martin
- "Effective Java" by Joshua Bloch
- "System Design Interview" by Alex Xu

### Practice Platforms:
- LeetCode (Premium for system design)
- Educative.io (Grokking System Design)
- InterviewBit
- Pramp (Mock interviews)

---

## 🤝 Contributing

These examples are designed for interview preparation. Feel free to:
- Extend implementations
- Add new test scenarios
- Improve documentation
- Suggest new examples

---

## 📧 Questions?

If you have questions about any implementation or need clarification on design decisions, check the README in each folder for detailed explanations and interview discussion points.

---

**Good luck with your interviews! 🎉**

Remember: The goal is not to memorize solutions, but to understand the thought process and design principles. Focus on understanding **why** certain decisions were made, not just **what** was implemented.

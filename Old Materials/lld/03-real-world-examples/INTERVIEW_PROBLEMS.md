# Top 10 LLD Interview Problems

## 🎯 Most Frequently Asked LLD Problems

This document covers the **10 most common Low-Level Design problems** asked in technical interviews at top companies like Google, Amazon, Microsoft, Meta, Apple, etc.

---

## 1. Parking Lot System ⭐⭐⭐

### Difficulty: Medium
### Frequency: Very High (Asked at: Amazon, Microsoft, Google, Walmart)

### Problem Statement:
Design a parking lot system that can accommodate different types of vehicles.

### Key Requirements:
- Multiple floors
- Different spot sizes (Small, Medium, Large, Handicapped)
- Different vehicle types (Motorcycle, Car, Truck, Van)
- Entry/Exit with ticket system
- Fee calculation based on duration
- Display board showing available spots
- Vehicle can't park in spot smaller than its size

### Core Entities:
1. **ParkingLot** - Main system
2. **Floor** - Each floor in parking lot
3. **ParkingSpot** - Individual parking spot
4. **Vehicle** (abstract) - Base for all vehicles
   - Motorcycle, Car, Truck (concrete types)
5. **Ticket** - Entry/exit tracking
6. **FeeCalculator** - Calculate parking fees
7. **DisplayBoard** - Show availability

### Key Design Patterns:
- **Singleton**: ParkingLot instance
- **Factory**: Create vehicles
- **Strategy**: Different fee calculation strategies

### SOLID Principles Applied:
- **SRP**: Each class has single responsibility
- **OCP**: Easy to add new vehicle types
- **LSP**: All vehicles substitutable
- **DIP**: Depend on Vehicle interface

### Interview Discussion Points:
- Thread safety for concurrent access
- Spot assignment algorithm (nearest to entrance)
- Handling full parking lot
- Reservation system
- Monthly pass holders

### Variations Asked:
- Airport parking (long-term vs short-term)
- Mall parking (first 2 hours free)
- Valet parking system

### Key Methods:
```java
// ParkingLot
- parkVehicle(Vehicle) : Ticket
- removeVehicle(Ticket) : double
- getAvailableSpots(SpotType) : int

// ParkingSpot
- assignVehicle(Vehicle) : boolean
- removeVehicle() : void
- isAvailable() : boolean
```

### Time to Complete: 45-60 minutes

---

## 2. Library Management System ⭐⭐⭐

### Difficulty: Medium
### Frequency: Very High (Asked at: Flipkart, Amazon, Microsoft)

### Problem Statement:
Design a library management system for managing books, members, and borrowing.

### Key Requirements:
- Multiple copies of same book
- Different member types (Student, Faculty, Premium)
- Book categories (Fiction, Non-fiction, Reference)
- Borrow/Return books
- Fine calculation for late returns
- Search books by title, author, ISBN
- Reserve books
- Max books limit per member type

### Core Entities:
1. **Library** - Main system
2. **Book** - Book information
3. **BookItem** - Physical copy of book
4. **Member** (abstract) - Library member
   - Student, Faculty, Premium
5. **Librarian** - Staff member
6. **BookLending** - Lending transaction
7. **Catalog** - Search functionality

### Key Design Patterns:
- **Singleton**: Library instance
- **Factory**: Create members
- **Observer**: Notify when reserved book available
- **Strategy**: Different fine calculation strategies

### SOLID Principles Applied:
- **SRP**: BookItem manages availability, Member manages borrowing
- **OCP**: Easy to add new member types
- **ISP**: Separate interfaces for Borrowable, Reservable
- **DIP**: Depend on Search interface

### Interview Discussion Points:
- How to handle same book multiple copies
- Fine calculation logic
- Reservation queue management
- Concurrency (two people trying to borrow same book)
- Book recommendation system

### Key Methods:
```java
// Library
- addBook(Book) : boolean
- searchByTitle(String) : List<Book>
- checkout(Member, BookItem) : boolean
- returnBook(BookItem) : double

// Member
- borrowBook(BookItem) : boolean
- returnBook(BookItem) : void
- getTotalBooksCheckedOut() : int
```

### Time to Complete: 45-60 minutes

---

## 3. Elevator System ⭐⭐⭐

### Difficulty: Hard
### Frequency: High (Asked at: Microsoft, Google, Amazon)

### Problem Statement:
Design an elevator control system for a building.

### Key Requirements:
- Multiple elevators
- Multiple floors
- Efficient elevator assignment
- Up/Down buttons on each floor
- Floor buttons inside elevator
- Door open/close mechanism
- Emergency handling
- Weight limit
- Display current floor

### Core Entities:
1. **ElevatorSystem** - Main controller
2. **Elevator** - Individual elevator
3. **Floor** - Building floor
4. **Request** - User request (internal/external)
5. **Door** - Elevator door
6. **Button** - Floor/direction buttons
7. **Display** - Show current floor

### Key Design Patterns:
- **Singleton**: ElevatorSystem
- **State**: Elevator states (Moving, Idle, Maintenance)
- **Strategy**: Different scheduling algorithms (FCFS, SCAN, LOOK)
- **Observer**: Update displays

### Scheduling Algorithms:
1. **FCFS** - First Come First Serve
2. **SCAN** - Move in one direction, serve all
3. **LOOK** - Move only till last request
4. **Optimal** - Minimize total waiting time

### SOLID Principles Applied:
- **SRP**: Elevator handles movement, Scheduler handles requests
- **OCP**: Easy to add new scheduling algorithms
- **DIP**: Depend on SchedulingStrategy interface

### Interview Discussion Points:
- Optimal elevator selection algorithm
- How to handle concurrent requests
- Emergency scenarios
- Peak hours optimization
- Energy saving mode

### Key Methods:
```java
// ElevatorSystem
- requestElevator(int floor, Direction) : void
- selectFloor(Elevator, int floor) : void
- optimizeScheduling() : void

// Elevator
- move(int floor) : void
- addRequest(Request) : void
- openDoor() : void
- closeDoor() : void
```

### Time to Complete: 60-75 minutes

---

## 4. Hotel Booking System ⭐⭐⭐

### Difficulty: Medium-Hard
### Frequency: High (Asked at: Airbnb, Booking.com, MakeMyTrip, OYO)

### Problem Statement:
Design a hotel reservation system.

### Key Requirements:
- Multiple hotels, multiple rooms
- Different room types (Single, Double, Suite)
- Search hotels by location, dates
- Book/Cancel reservations
- Payment processing
- Reviews and ratings
- Room service
- Check-in/Check-out

### Core Entities:
1. **Hotel** - Hotel information
2. **Room** - Individual room
3. **Booking** - Reservation details
4. **Guest** - Hotel guest
5. **Payment** - Payment processing
6. **Search** - Hotel search functionality
7. **Review** - Guest reviews

### Key Design Patterns:
- **Factory**: Create different room types
- **Observer**: Notify about booking status
- **Strategy**: Different pricing strategies
- **State**: Booking states (Pending, Confirmed, Cancelled)

### SOLID Principles Applied:
- **SRP**: Booking handles reservation, Payment handles transactions
- **OCP**: Easy to add new room types
- **ISP**: Separate interfaces for Searchable, Bookable
- **DIP**: Depend on PaymentGateway interface

### Interview Discussion Points:
- Concurrent booking prevention (same room)
- Dynamic pricing
- Cancellation and refund policy
- Overbooking handling
- Search optimization

### Key Methods:
```java
// Hotel
- searchRooms(Date checkIn, Date checkOut, RoomType) : List<Room>
- bookRoom(Guest, Room, Date, Date) : Booking
- cancelBooking(Booking) : boolean

// Booking
- confirm() : boolean
- cancel() : void
- calculateTotal() : double
```

### Time to Complete: 45-60 minutes

---

## 5. ATM Machine ⭐⭐

### Difficulty: Medium
### Frequency: High (Asked at: Banks, Financial companies)

### Problem Statement:
Design an ATM machine system.

### Key Requirements:
- Multiple accounts support
- Withdraw cash
- Deposit cash
- Check balance
- Transfer money
- Print receipt
- Card validation
- PIN verification
- Cash dispenser with different denominations

### Core Entities:
1. **ATM** - Main ATM machine
2. **Card** - Bank card
3. **Account** - Bank account
4. **Bank** - Banking system
5. **CashDispenser** - Dispense cash
6. **Transaction** - Transaction record
7. **Screen** - Display interface

### Key Design Patterns:
- **State**: ATM states (Idle, CardInserted, PINEntered, TransactionInProgress)
- **Chain of Responsibility**: Cash dispensing
- **Factory**: Create transactions
- **Singleton**: ATM instance

### SOLID Principles Applied:
- **SRP**: CashDispenser handles cash, Transaction records activity
- **OCP**: Easy to add new transaction types
- **DIP**: Depend on BankService interface

### Interview Discussion Points:
- Cash denomination algorithm
- PIN validation (max 3 attempts)
- Concurrent user handling
- Network failure handling
- Security considerations

### Key Methods:
```java
// ATM
- insertCard(Card) : boolean
- enterPIN(String) : boolean
- withdraw(double amount) : boolean
- checkBalance() : double
- ejectCard() : void

// CashDispenser
- dispense(double amount) : boolean
- getCashAvailable() : double
```

### Time to Complete: 30-45 minutes

---

## 6. Online Shopping System (Amazon/Flipkart) ⭐⭐⭐

### Difficulty: Hard
### Frequency: Very High (Asked at: Amazon, Flipkart, Walmart)

### Problem Statement:
Design an e-commerce platform like Amazon.

### Key Requirements:
- Product catalog
- Search and filters
- Shopping cart
- Order placement
- Payment processing
- Inventory management
- Shipping tracking
- Reviews and ratings
- Wishlist
- Notifications

### Core Entities:
1. **Product** - Product details
2. **User** - Platform user
3. **ShoppingCart** - User's cart
4. **Order** - Placed order
5. **Payment** - Payment processing
6. **Inventory** - Stock management
7. **Shipping** - Delivery tracking
8. **Review** - Product reviews

### Key Design Patterns:
- **Singleton**: ShoppingCart per user
- **Observer**: Notify about order status
- **Strategy**: Different payment methods, shipping methods
- **Factory**: Create orders
- **Decorator**: Add gift wrapping, express shipping

### SOLID Principles Applied:
- **SRP**: Each entity manages its own data
- **OCP**: Easy to add payment methods, shipping options
- **ISP**: Separate interfaces for Purchasable, Reviewable
- **DIP**: Depend on PaymentGateway, ShippingProvider interfaces

### Interview Discussion Points:
- Inventory management (stock updates)
- Concurrent order placement
- Flash sales handling
- Recommendation engine
- Search optimization
- Distributed system aspects

### Key Methods:
```java
// ShoppingCart
- addItem(Product, int quantity) : void
- removeItem(Product) : void
- checkout() : Order

// Order
- placeOrder() : boolean
- trackShipment() : String
- cancelOrder() : boolean
```

### Time to Complete: 60-90 minutes

---

## 7. Ride Sharing System (Uber/Ola) ⭐⭐⭐

### Difficulty: Hard
### Frequency: Very High (Asked at: Uber, Lyft, Ola)

### Problem Statement:
Design a ride-sharing platform like Uber.

### Key Requirements:
- Rider and Driver profiles
- Request ride
- Match rider with nearby driver
- Track ride in real-time
- Multiple ride types (Pool, Go, Premier)
- Fare calculation
- Payment processing
- Rating system
- Surge pricing

### Core Entities:
1. **Rider** - Person requesting ride
2. **Driver** - Person providing ride
3. **Ride** - Ride details
4. **Location** - GPS coordinates
5. **Vehicle** - Driver's vehicle
6. **Payment** - Payment processing
7. **Rating** - Rating system
8. **Pricing** - Fare calculation

### Key Design Patterns:
- **Strategy**: Different pricing strategies, matching algorithms
- **Observer**: Notify about ride status
- **State**: Ride states (Requested, Accepted, InProgress, Completed)
- **Factory**: Create different ride types

### SOLID Principles Applied:
- **SRP**: Matching service finds drivers, Pricing calculates fare
- **OCP**: Easy to add new ride types
- **DIP**: Depend on LocationService, PaymentGateway interfaces

### Interview Discussion Points:
- Driver-rider matching algorithm
- Real-time location tracking
- Surge pricing logic
- Handling cancellations
- ETA calculation
- Map services integration

### Key Methods:
```java
// RideService
- requestRide(Rider, Location from, Location to) : Ride
- findNearbyDrivers(Location, int radius) : List<Driver>
- calculateFare(Ride) : double

// Ride
- acceptRide(Driver) : boolean
- startRide() : void
- endRide() : void
- rateDriver(int rating) : void
```

### Time to Complete: 60-90 minutes

---

## 8. Movie Ticket Booking System ⭐⭐⭐

### Difficulty: Medium
### Frequency: High (Asked at: BookMyShow, Fandango, entertainment companies)

### Problem Statement:
Design a movie ticket booking system.

### Key Requirements:
- Multiple cities, theaters, screens
- Show timings
- Seat selection
- Multiple seat types (Regular, Premium, Recliner)
- Booking and cancellation
- Payment processing
- Concurrency handling (same seat selection)
- Food booking
- Ticket generation

### Core Entities:
1. **City** - City location
2. **Theater** - Movie theater
3. **Screen** - Theater screen
4. **Show** - Movie showing
5. **Seat** - Individual seat
6. **Booking** - Ticket booking
7. **Payment** - Payment processing
8. **Movie** - Movie details

### Key Design Patterns:
- **Singleton**: BookingSystem per theater
- **Observer**: Notify about booking confirmation
- **State**: Seat states (Available, Locked, Booked)
- **Factory**: Create bookings

### SOLID Principles Applied:
- **SRP**: Theater manages screens, Booking handles reservations
- **OCP**: Easy to add new seat types, show types
- **DIP**: Depend on PaymentGateway interface

### Interview Discussion Points:
- Seat locking mechanism (10 min hold)
- Concurrent booking prevention
- Show scheduling
- Dynamic pricing (weekend vs weekday)
- Cancellation and refunds
- Waitlist management

### Key Methods:
```java
// Theater
- getShows(Movie, Date) : List<Show>
- searchMovies(String) : List<Movie>

// Show
- getAvailableSeats() : List<Seat>
- bookSeats(List<Seat>, User) : Booking

// Booking
- lockSeats(List<Seat>) : boolean
- confirmBooking() : boolean
- cancelBooking() : void
```

### Time to Complete: 45-60 minutes

---

## 9. Social Media Platform (Facebook/Twitter) ⭐⭐⭐

### Difficulty: Hard
### Frequency: High (Asked at: Meta, Twitter, LinkedIn)

### Problem Statement:
Design a social media platform.

### Key Requirements:
- User profiles
- Post creation (text, images, videos)
- Follow/Unfollow users
- News feed
- Like, Comment, Share
- Notifications
- Search users
- Privacy settings
- Messaging

### Core Entities:
1. **User** - Platform user
2. **Post** - User post
3. **Comment** - Post comment
4. **Like** - Like functionality
5. **Follow** - Follow relationship
6. **NewsFeed** - User's feed
7. **Notification** - User notifications
8. **Message** - Direct messaging

### Key Design Patterns:
- **Observer**: Notify followers about new posts
- **Strategy**: Different newsfeed algorithms
- **Factory**: Create different post types
- **Decorator**: Add features to posts (location, tags)
- **Composite**: Comment threads

### SOLID Principles Applied:
- **SRP**: NewsFeed generates feed, Post handles content
- **OCP**: Easy to add new post types
- **ISP**: Separate interfaces for Likeable, Commentable, Shareable
- **DIP**: Depend on NotificationService interface

### Interview Discussion Points:
- Newsfeed generation algorithm
- Scaling for millions of users
- Real-time updates
- Privacy controls
- Content moderation
- Storage optimization

### Key Methods:
```java
// User
- createPost(String content) : Post
- follow(User) : void
- getNewsFeed() : List<Post>

// Post
- addComment(Comment) : void
- like(User) : void
- share(User) : void

// NewsFeed
- generateFeed(User) : List<Post>
- updateFeed() : void
```

### Time to Complete: 60-90 minutes

---

## 10. Online Chess Game ⭐⭐

### Difficulty: Medium-Hard
### Frequency: Medium (Asked at: Chess.com, game companies)

### Problem Statement:
Design an online chess game system.

### Key Requirements:
- Two players
- Chess board (8x8)
- All piece movements
- Turn management
- Check/Checkmate detection
- Castling, En passant
- Pawn promotion
- Move validation
- Game history
- Timer (optional)

### Core Entities:
1. **Game** - Chess game
2. **Board** - 8x8 chess board
3. **Piece** (abstract) - Chess piece
   - Pawn, Rook, Knight, Bishop, Queen, King
4. **Player** - Game player
5. **Move** - Player move
6. **Position** - Board position
7. **GameController** - Manage game flow

### Key Design Patterns:
- **Strategy**: Different piece movement strategies
- **Command**: Encapsulate moves (for undo)
- **State**: Game states (Setup, InProgress, Checkmate, Stalemate)
- **Factory**: Create pieces

### SOLID Principles Applied:
- **SRP**: Piece handles movement rules, Board manages positions
- **OCP**: Easy to add new game variants
- **LSP**: All pieces are substitutable
- **DIP**: Depend on MoveValidator interface

### Interview Discussion Points:
- Move validation algorithm
- Check/Checkmate detection
- Special moves (castling, en passant)
- Undo/Redo functionality
- Game replay
- AI opponent (minimax algorithm)

### Key Methods:
```java
// Game
- makeMove(Player, Move) : boolean
- isCheck(Player) : boolean
- isCheckmate(Player) : boolean

// Board
- movePiece(Position from, Position to) : boolean
- getPiece(Position) : Piece

// Piece
- canMove(Position to, Board) : boolean
- getValidMoves(Board) : List<Position>
```

### Time to Complete: 60-75 minutes

---

## 📝 Interview Strategy

### Time Management:
1. **5-10 min**: Understand requirements, ask clarifying questions
2. **10-15 min**: Identify entities and relationships
3. **20-30 min**: Design classes, methods, apply design patterns
4. **10-15 min**: Discuss edge cases, trade-offs, scalability

### Key Points to Cover:
✅ Clarify requirements
✅ Identify core entities
✅ Define relationships (has-a, is-a)
✅ Apply SOLID principles
✅ Use appropriate design patterns
✅ Discuss concurrency/thread-safety
✅ Handle edge cases
✅ Consider scalability
✅ Discuss trade-offs

### Common Mistakes to Avoid:
❌ Jumping to code without design
❌ Over-engineering simple problems
❌ Ignoring edge cases
❌ Not discussing trade-offs
❌ Missing SOLID principles
❌ Poor naming conventions
❌ Not considering scalability

---

## 🎯 Practice Approach

1. **Start with easier problems** (Parking Lot, ATM)
2. **Practice time-boxed** (45-60 minutes per problem)
3. **Implement complete code** for 2-3 problems
4. **Draw class diagrams** for all problems
5. **Discuss with peers** for feedback
6. **Focus on design, not implementation details**

---

## 📚 Additional Resources

- Practice on platforms: LeetCode Design, InterviewBit
- Read: "Head First Design Patterns"
- Study: Real system architectures
- Mock interviews with peers

---

**Good luck with your LLD interviews! 🚀**

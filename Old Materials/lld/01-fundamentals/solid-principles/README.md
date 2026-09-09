# SOLID Principles

## 📖 Introduction

SOLID is an acronym for five design principles that make software designs more understandable, flexible, and maintainable. These principles were introduced by Robert C. Martin (Uncle Bob).

## 🎯 The Five SOLID Principles

1. **S** - Single Responsibility Principle (SRP)
2. **O** - Open/Closed Principle (OCP)
3. **L** - Liskov Substitution Principle (LSP)
4. **I** - Interface Segregation Principle (ISP)
5. **D** - Dependency Inversion Principle (DIP)

## 🌟 Why SOLID Matters

### Benefits:
✅ **Maintainable Code** - Easy to modify and extend
✅ **Testable Code** - Each component can be tested independently
✅ **Flexible Design** - Easy to adapt to changing requirements
✅ **Reusable Components** - Code can be reused in different contexts
✅ **Reduced Coupling** - Components are loosely coupled
✅ **Better Collaboration** - Team members can work on different parts

### Without SOLID:
❌ Rigid code that's hard to change
❌ Fragile code that breaks easily
❌ Code that's difficult to test
❌ Code with tight coupling
❌ Code that's hard to understand

## 📚 Learning Path

### Start Here:
1. **Single Responsibility Principle (SRP)** ⭐ Start with this!
   - One class, one responsibility
   - Easiest to understand
   - Foundation for other principles

2. **Open/Closed Principle (OCP)**
   - Open for extension, closed for modification
   - Use abstraction and polymorphism

3. **Liskov Substitution Principle (LSP)**
   - Subtypes must be substitutable for their base types
   - Proper inheritance hierarchy

4. **Interface Segregation Principle (ISP)**
   - Many specific interfaces better than one general interface
   - Clients shouldn't depend on interfaces they don't use

5. **Dependency Inversion Principle (DIP)**
   - Depend on abstractions, not concrete implementations
   - High-level modules shouldn't depend on low-level modules

## 🚀 How to Study

### For Each Principle:
1. **Understand the Problem** - See what happens without the principle
2. **Learn the Solution** - Understand how the principle solves it
3. **Code Examples** - Study both "bad" and "good" examples
4. **Practice** - Refactor code to apply principles
5. **Review** - Come back after a few days

### Study Schedule:
- **Day 1-2**: Single Responsibility Principle
- **Day 3-4**: Open/Closed Principle
- **Day 5-6**: Liskov Substitution Principle
- **Day 7-8**: Interface Segregation Principle
- **Day 9-10**: Dependency Inversion Principle
- **Day 11-12**: Review and practice all principles

## 💡 FAANG Interview FAQ

### Most Frequently Asked Questions

#### Q1: Explain SOLID principles with real-world examples

**Answer:**

SOLID is an acronym for five design principles that help create maintainable, scalable software:

**S - Single Responsibility Principle (SRP):**
- A class should have only one reason to change
- Example: Separate `User` (data), `UserValidator` (validation), `UserRepository` (storage)
```java
// Bad - Multiple responsibilities
class User {
    String name;
    void validate() { } // Validation
    void save() { }     // Persistence
    void sendEmail() { } // Notification
}

// Good - Single responsibility each
class User { String name; }
class UserValidator { void validate(User u) { } }
class UserRepository { void save(User u) { } }
class EmailService { void sendEmail(User u) { } }
```

**O - Open/Closed Principle (OCP):**
- Open for extension, closed for modification
- Use interfaces and polymorphism
```java
// Bad - Needs modification for new payment types
class PaymentProcessor {
    void process(String type) {
        if (type.equals("CREDIT")) { /* ... */ }
        else if (type.equals("DEBIT")) { /* ... */ }
    }
}

// Good - Extend without modifying
interface PaymentMethod { void pay(); }
class CreditCard implements PaymentMethod { }
class DebitCard implements PaymentMethod { }
```

**L - Liskov Substitution Principle (LSP):**
- Subclasses should be substitutable for parent classes
- Don't break parent class contracts
```java
// Bad - Square breaks Rectangle's contract
class Rectangle {
    void setWidth(int w) { width = w; }
    void setHeight(int h) { height = h; }
}
class Square extends Rectangle {
    void setWidth(int w) { width = height = w; } // Breaks LSP!
}

// Good - Use proper abstraction
interface Shape { int getArea(); }
class Rectangle implements Shape { }
class Square implements Shape { }
```

**I - Interface Segregation Principle (ISP):**
- Many specific interfaces > one general interface
- Clients shouldn't implement unused methods
```java
// Bad - RobotWorker forced to implement eat()
interface Worker {
    void work();
    void eat();
}

// Good - Segregated interfaces
interface Workable { void work(); }
interface Eatable { void eat(); }
class HumanWorker implements Workable, Eatable { }
class RobotWorker implements Workable { }
```

**D - Dependency Inversion Principle (DIP):**
- Depend on abstractions, not concrete implementations
- High-level modules independent of low-level details
```java
// Bad - Tight coupling
class NotificationService {
    private EmailService email = new EmailService();
}

// Good - Depend on abstraction
class NotificationService {
    private MessageService service; // Interface
    public NotificationService(MessageService service) {
        this.service = service;
    }
}
```

---

#### Q2: How does Single Responsibility Principle help in testing?

**Answer:**

SRP makes testing easier because:

1. **Focused Tests**: Each class has one responsibility, so tests are simpler
2. **Isolated Testing**: Can test each component independently
3. **Mock Dependencies**: Easier to mock when responsibilities are separated
4. **Fewer Test Cases**: One responsibility = fewer scenarios to test

**Example:**
```java
// Bad - Hard to test
class UserService {
    void register(User user) {
        // Validate
        if (user.email == null) throw new Exception();
        // Save to DB
        db.save(user);
        // Send email
        smtp.send(user.email, "Welcome!");
        // Log
        logger.log("User registered");
    }
}
// Need to mock DB, SMTP, Logger all at once!

// Good - Easy to test each
class UserValidator {
    boolean isValid(User user) {
        return user.email != null;
    }
}
// Test only validation logic

class UserRepository {
    void save(User user) { db.save(user); }
}
// Test only persistence

class EmailService {
    void sendWelcome(User user) { smtp.send(...); }
}
// Test only email logic
```

---

#### Q3: What's the difference between Open/Closed Principle and Strategy Pattern?

**Answer:**

**Open/Closed Principle (OCP):**
- A **principle** (guideline) - says "what" to achieve
- Code should be open for extension, closed for modification
- Can be achieved through multiple techniques

**Strategy Pattern:**
- A **design pattern** (solution) - shows "how" to achieve OCP
- One of many ways to implement OCP
- Encapsulates algorithms and makes them interchangeable

**Relationship:**
Strategy Pattern is one way to implement OCP.

**Example:**
```java
// OCP Principle says: "Don't modify existing code for new features"

// Strategy Pattern implements OCP:
interface SortStrategy { void sort(int[] arr); }

class BubbleSort implements SortStrategy {
    void sort(int[] arr) { /* bubble sort */ }
}

class QuickSort implements SortStrategy {
    void sort(int[] arr) { /* quick sort */ }
}

// Add new strategy without modifying existing code
class MergeSort implements SortStrategy {
    void sort(int[] arr) { /* merge sort */ }
}
```

**Other ways to achieve OCP:**
- Template Method Pattern
- Decorator Pattern
- Observer Pattern
- Inheritance with polymorphism

---

#### Q4: Give an example where you violated SOLID and refactored it

**Answer:**

**Scenario:** Early in a project, I had a `ReportGenerator` class that violated SRP.

**Violation:**
```java
class ReportGenerator {
    // Violates SRP - multiple responsibilities
    void generateReport(List<User> users) {
        // 1. Fetch data from database
        users = database.fetchUsers();
        
        // 2. Calculate statistics
        double avgAge = calculateAverage(users);
        
        // 3. Format as PDF
        PDF pdf = new PDF();
        pdf.add("Average Age: " + avgAge);
        
        // 4. Send email
        emailService.send(pdf);
        
        // 5. Log activity
        logger.log("Report generated");
    }
}
```

**Problems Identified:**
- Hard to test (needs DB, PDF library, email, logger)
- Hard to reuse (what if I want JSON instead of PDF?)
- Hard to maintain (change in email affects whole class)

**Refactored (Applied SRP, DIP):**
```java
// 1. Separate data access
class UserRepository {
    List<User> fetchUsers() { return database.fetchUsers(); }
}

// 2. Separate calculation
class UserStatistics {
    double calculateAverageAge(List<User> users) { /* ... */ }
}

// 3. Separate formatting (also OCP)
interface ReportFormatter {
    String format(ReportData data);
}
class PDFFormatter implements ReportFormatter { }
class JSONFormatter implements ReportFormatter { }

// 4. Separate delivery (also DIP)
interface ReportDelivery {
    void deliver(String report);
}
class EmailDelivery implements ReportDelivery { }

// 5. Orchestrator with single responsibility
class ReportService {
    private UserRepository repo;
    private UserStatistics stats;
    private ReportFormatter formatter;
    private ReportDelivery delivery;
    
    // Dependency Injection (DIP)
    public ReportService(UserRepository repo, 
                        UserStatistics stats,
                        ReportFormatter formatter,
                        ReportDelivery delivery) {
        this.repo = repo;
        this.stats = stats;
        this.formatter = formatter;
        this.delivery = delivery;
    }
    
    void generateReport() {
        List<User> users = repo.fetchUsers();
        double avgAge = stats.calculateAverageAge(users);
        String report = formatter.format(new ReportData(avgAge));
        delivery.deliver(report);
    }
}
```

**Benefits After Refactoring:**
- ✅ Each class easily testable in isolation
- ✅ Can switch PDF to JSON without changing other parts
- ✅ Can add SMS delivery without modifying existing code
- ✅ Can reuse `UserStatistics` in other features
- ✅ Much easier to maintain and extend

---

#### Q5: How do you decide when to apply SOLID principles?

**Answer:**

**Apply SOLID When:**
1. **Code Smells Appear:**
   - Large classes (>200 lines often violate SRP)
   - Long methods (>20 lines)
   - Many if-else statements (violates OCP)
   - Duplicate code

2. **Testing Is Hard:**
   - Can't test in isolation
   - Need to mock too many dependencies
   - Setup is complex

3. **Frequent Changes:**
   - Same class modified for different reasons
   - Adding features requires modifying existing code
   - Bugs in one area break other areas

4. **Team Growth:**
   - Multiple developers working on same code
   - Need clear separation of concerns
   - Want to enable parallel development

**Don't Over-Apply:**
1. **Simple CRUD Operations:**
   ```java
   // OK for simple use case
   class User {
       String name;
       void save() { db.save(this); }
   }
   ```

2. **Prototypes/POCs:**
   - Quick validation needed
   - Will rewrite anyway

3. **Scripts/One-time Use:**
   - Not maintained long-term

**Pragmatic Approach:**
```
Start Simple → Code Smells? → Refactor with SOLID → Review

- Week 1-2: Write simple code that works
- Week 3: Refactor when patterns emerge
- Week 4+: Apply SOLID based on actual needs
```

---

#### Q6: Explain Dependency Inversion with Spring Framework example

**Answer:**

**Dependency Inversion Principle states:**
- High-level modules shouldn't depend on low-level modules
- Both should depend on abstractions (interfaces)
- Abstractions shouldn't depend on details

**Spring Framework Example:**

**Without DIP (Bad):**
```java
@Service
class OrderService {
    // Tight coupling to concrete implementation
    private MySQLOrderRepository repository = new MySQLOrderRepository();
    
    void placeOrder(Order order) {
        repository.save(order);
    }
}

class MySQLOrderRepository {
    void save(Order order) { /* MySQL specific */ }
}
```

**Problems:**
- Can't switch to PostgreSQL without modifying OrderService
- Hard to test (need actual MySQL database)
- OrderService depends on low-level details

**With DIP (Good):**
```java
// Abstraction
interface OrderRepository {
    void save(Order order);
}

// High-level module depends on abstraction
@Service
class OrderService {
    private final OrderRepository repository;
    
    // Dependency injected via constructor
    @Autowired
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
    
    void placeOrder(Order order) {
        repository.save(order); // Uses abstraction
    }
}

// Low-level implementations
@Repository
class MySQLOrderRepository implements OrderRepository {
    void save(Order order) { /* MySQL */ }
}

@Repository
class PostgreSQLOrderRepository implements OrderRepository {
    void save(Order order) { /* PostgreSQL */ }
}
```

**Benefits:**
- ✅ Switch databases by changing configuration
- ✅ Easy to test with mock repository
- ✅ OrderService independent of database choice
- ✅ Can add new implementations without changing OrderService

**Testing:**
```java
@Test
void testPlaceOrder() {
    // Mock the abstraction
    OrderRepository mockRepo = mock(OrderRepository.class);
    OrderService service = new OrderService(mockRepo);
    
    service.placeOrder(new Order());
    
    verify(mockRepo).save(any(Order.class));
}
```

---

#### Q7: What's wrong with this code? (LSP Violation)

```java
class Bird {
    void fly() {
        System.out.println("Flying");
    }
}

class Penguin extends Bird {
    @Override
    void fly() {
        throw new UnsupportedOperationException("Penguins can't fly!");
    }
}
```

**Answer:**

**Violation:** Liskov Substitution Principle

**Problem:**
- `Penguin` cannot be substituted for `Bird` without breaking program
- Client code expecting `Bird` to fly will fail with `Penguin`
- Violates IS-A relationship contract

**Why It's Bad:**
```java
void makeBirdFly(Bird bird) {
    bird.fly(); // Will throw exception if bird is Penguin!
}

Bird penguin = new Penguin();
makeBirdFly(penguin); // Crashes!
```

**Correct Design:**
```java
// Option 1: Use composition instead of inheritance
interface Flyable {
    void fly();
}

class Bird {
    String name;
}

class Sparrow extends Bird implements Flyable {
    public void fly() {
        System.out.println("Sparrow flying");
    }
}

class Penguin extends Bird {
    // Doesn't implement Flyable
    void swim() {
        System.out.println("Penguin swimming");
    }
}

// Option 2: Better abstraction
abstract class Bird {
    abstract void move();
}

class Sparrow extends Bird {
    void move() {
        System.out.println("Flying");
    }
}

class Penguin extends Bird {
    void move() {
        System.out.println("Swimming/Walking");
    }
}
```

**Key Lesson:**
- Don't force inheritance where it doesn't fit
- Subclasses should enhance, not restrict parent behavior
- Use composition when inheritance doesn't make sense

---

#### Q8: How do SOLID principles relate to microservices?

**Answer:**

SOLID principles apply to microservices architecture:

**1. Single Responsibility (Service level):**
```
Each microservice should have one responsibility

✅ Good:
- UserService: User management only
- PaymentService: Payment processing only
- NotificationService: Notifications only

❌ Bad:
- MonolithService: Users + Payments + Notifications + Orders
```

**2. Open/Closed (API design):**
```
APIs should be versioned and backward compatible

✅ Good:
- /api/v1/users
- /api/v2/users (new version, v1 still works)

❌ Bad:
- /api/users (breaking changes affect all clients)
```

**3. Liskov Substitution (Service contracts):**
```
Services implementing same interface should be interchangeable

✅ Good:
interface PaymentGateway {
    PaymentResult process(Payment payment);
}
- StripeService implements PaymentGateway
- PayPalService implements PaymentGateway
- Both can replace each other

❌ Bad:
- StripeService requires additional setup
- PayPalService throws different exceptions
- Not truly interchangeable
```

**4. Interface Segregation (API endpoints):**
```
Don't force clients to depend on endpoints they don't use

✅ Good:
- /api/users/profile (read operations)
- /api/users/settings (write operations)
- Mobile app uses only /profile
- Admin panel uses both

❌ Bad:
- /api/users (returns everything)
- Mobile app gets unnecessary data
- Wastes bandwidth
```

**5. Dependency Inversion (Service communication):**
```
Services should communicate through abstractions

✅ Good:
OrderService → (Message Queue) → PaymentService
- Decoupled through message broker
- Can replace either service

❌ Bad:
OrderService → HTTP Call → PaymentService
- Tight coupling
- PaymentService downtime breaks OrderService
```

---

### Interview Tips

**Do:**
- ✅ Always provide real-world examples
- ✅ Explain the "why" behind each principle
- ✅ Show code examples when possible
- ✅ Mention trade-offs and when to be pragmatic
- ✅ Connect principles to design patterns
- ✅ Discuss testability benefits
- ✅ Share personal refactoring experiences

**Don't:**
- ❌ Just recite definitions
- ❌ Say "I always follow SOLID" (be honest)
- ❌ Over-engineer simple examples
- ❌ Claim SOLID is always the answer
- ❌ Forget to mention when NOT to apply

**Follow-up Questions to Prepare:**
1. "Can you give a counterexample where SOLID made code worse?"
2. "How do you balance SOLID with deadlines?"
3. "What code smells indicate SOLID violations?"
4. "How does SOLID relate to YAGNI and KISS?"
5. "Explain SOLID in the context of your last project"

## 🔑 Key Takeaways

### Remember:
1. SOLID principles are **guidelines**, not strict rules
2. Apply them **pragmatically** - don't over-engineer
3. They work **together** - one principle supports others
4. **Refactoring** is key - apply principles when code smells appear
5. **Experience** matters - you'll get better with practice

### Warning Signs Your Code Violates SOLID:
- 🚨 A class has multiple reasons to change (SRP)
- 🚨 You modify existing code for new features (OCP)
- 🚨 Subclasses break parent class contracts (LSP)
- 🚨 Interfaces force implementations of unused methods (ISP)
- 🚨 High-level modules depend on low-level details (DIP)

## 📂 Folder Structure

```
solid-principles/
├── README.md (this file)
├── 01-single-responsibility/
│   ├── README.md
│   ├── bad-example/
│   └── good-example/
├── 02-open-closed/
│   ├── README.md
│   ├── bad-example/
│   └── good-example/
├── 03-liskov-substitution/
│   ├── README.md
│   ├── bad-example/
│   └── good-example/
├── 04-interface-segregation/
│   ├── README.md
│   ├── bad-example/
│   └── good-example/
└── 05-dependency-inversion/
    ├── README.md
    ├── bad-example/
    └── good-example/
```

## ➡️ Next Steps

1. Start with `01-single-responsibility/` - It's the foundation
2. Complete each example before moving to the next
3. Try to identify SOLID violations in your own code
4. Refactor existing code to apply these principles
5. Move on to Design Patterns after mastering SOLID

---

**Remember**: Understanding SOLID principles is crucial for writing maintainable, scalable, and testable code. Take your time with each principle!

# Dependency Inversion Principle (DIP)

## 📖 Definition

> "High-level modules should not depend on low-level modules. Both should depend on abstractions.
> Abstractions should not depend on details. Details should depend on abstractions."
> — Robert C. Martin

In simpler terms: **Depend on interfaces/abstractions, not concrete implementations.**

## 🎯 What Does This Mean?

### Two Key Rules:

1. **High-level modules** (business logic) should NOT depend on **low-level modules** (implementation details)
2. **Both should depend on abstractions** (interfaces/abstract classes)

### Why It's Called "Inversion":
Traditional dependency flow: High-level → Low-level
DIP inverts this: Both → Abstraction

## ❌ Why Violating DIP is Bad

### Problems:

1. **Tight Coupling**
   - High-level code directly depends on low-level details
   - Changes in low-level code break high-level code
   - Hard to modify or extend

2. **Difficult Testing**
   - Can't mock low-level modules
   - Need real implementations for tests
   - Slow, fragile tests

3. **Low Reusability**
   - High-level logic tied to specific implementations
   - Can't reuse with different low-level modules
   - Inflexible design

4. **Hard to Change**
   - Switching implementations requires code changes
   - Ripple effects throughout codebase
   - Fear of making changes

## 💡 Real-World Analogy

Think of electrical devices and power outlets:

**Bad (Violates DIP):**
```
LightBulb directly wired to PowerPlant

Problem:
- Can't change power source
- Can't use bulb elsewhere
- Tightly coupled
```

**Good (Follows DIP):**
```
LightBulb → PowerOutlet (abstraction) ← PowerPlant

Solution:
- Bulb depends on outlet interface
- Any power source can plug in
- Flexible, reusable
```

## 📝 Classic Example: Notification System

### Bad Example (Violates DIP):

```java
// Low-level module
class EmailService {
    public void sendEmail(String message) {
        System.out.println("Sending email: " + message);
    }
}

// High-level module depends on low-level
class NotificationService {
    private EmailService emailService; // Direct dependency on concrete class!
    
    public NotificationService() {
        this.emailService = new EmailService(); // Creating dependency itself!
    }
    
    public void sendNotification(String message) {
        emailService.sendEmail(message); // Tightly coupled to email
    }
}
```

**Problems:**
- NotificationService directly depends on EmailService
- Can't switch to SMS or Push notifications without code changes
- Hard to test (need real EmailService)
- Not following DIP

### Good Example (Follows DIP):

```java
// Abstraction (interface)
interface MessageService {
    void sendMessage(String message);
}

// Low-level modules implement abstraction
class EmailService implements MessageService {
    public void sendMessage(String message) {
        System.out.println("Sending email: " + message);
    }
}

class SMSService implements MessageService {
    public void sendMessage(String message) {
        System.out.println("Sending SMS: " + message);
    }
}

class PushNotificationService implements MessageService {
    public void sendMessage(String message) {
        System.out.println("Sending push notification: " + message);
    }
}

// High-level module depends on abstraction
class NotificationService {
    private MessageService messageService; // Depends on interface!
    
    // Dependency injection through constructor
    public NotificationService(MessageService messageService) {
        this.messageService = messageService;
    }
    
    public void sendNotification(String message) {
        messageService.sendMessage(message); // Works with any implementation
    }
}

// Usage
MessageService email = new EmailService();
NotificationService notifier = new NotificationService(email);
notifier.sendNotification("Hello!");

// Easy to switch
MessageService sms = new SMSService();
NotificationService notifier2 = new NotificationService(sms);
notifier2.sendNotification("Hello via SMS!");
```

**Benefits:**
- NotificationService depends on interface, not concrete class
- Easy to add new notification types
- Easy to test (mock MessageService)
- Flexible and maintainable

## 🔍 How to Identify DIP Violations

### Warning Signs:

1. **Direct Instantiation of Dependencies**
   ```java
   class Service {
       private Database db = new MySQLDatabase(); // BAD!
   }
   ```

2. **Depending on Concrete Classes**
   ```java
   class OrderProcessor {
       private EmailSender sender; // BAD if EmailSender is concrete
   }
   ```

3. **Hard to Test**
   - Can't pass mock objects
   - Need real database, network, etc.
   - Tests are slow and fragile

4. **Frequent Code Changes**
   - Adding new implementation requires changing existing code
   - Violates Open/Closed Principle

## ✅ How to Apply DIP

### Design Guidelines:

1. **Define Abstractions**
   - Create interfaces for dependencies
   - Focus on what, not how
   - Keep interfaces simple

2. **Dependency Injection (DI)**
   - Constructor Injection (preferred)
   - Setter Injection
   - Interface Injection
   - Don't create dependencies inside class

3. **Inversion of Control (IoC)**
   - Framework manages dependencies
   - Spring, Guice, etc.
   - Focus on business logic

4. **Program to Interface**
   - Declare variables as interface types
   - Return interfaces from methods
   - Accept interfaces as parameters

## 📊 Dependency Injection Methods

### 1. Constructor Injection (Best Practice):
```java
class OrderService {
    private final Repository repository; // final = immutable
    
    public OrderService(Repository repository) {
        this.repository = repository; // Injected
    }
}
```

### 2. Setter Injection:
```java
class OrderService {
    private Repository repository;
    
    public void setRepository(Repository repository) {
        this.repository = repository; // Injected later
    }
}
```

### 3. Interface Injection:
```java
interface RepositoryInjector {
    void injectRepository(Repository repository);
}

class OrderService implements RepositoryInjector {
    private Repository repository;
    
    public void injectRepository(Repository repository) {
        this.repository = repository;
    }
}
```

## 🎓 Practice Exercise

Identify DIP violation:

```java
class ReportGenerator {
    private MySQLDatabase database;
    
    public ReportGenerator() {
        this.database = new MySQLDatabase(); // Violation!
    }
    
    public void generateReport() {
        List<Data> data = database.query("SELECT * FROM reports");
        // process data
    }
}
```

### Problems:
1. Directly instantiates MySQLDatabase
2. Depends on concrete implementation
3. Can't switch to PostgreSQL without code changes
4. Hard to test (need real MySQL)

### Solution:
```java
// Abstraction
interface Database {
    List<Data> query(String sql);
}

// Implementations
class MySQLDatabase implements Database {
    public List<Data> query(String sql) { /* MySQL implementation */ }
}

class PostgreSQLDatabase implements Database {
    public List<Data> query(String sql) { /* PostgreSQL implementation */ }
}

// High-level module depends on abstraction
class ReportGenerator {
    private final Database database; // Depends on interface
    
    public ReportGenerator(Database database) { // Dependency injection
        this.database = database;
    }
    
    public void generateReport() {
        List<Data> data = database.query("SELECT * FROM reports");
        // process data
    }
}

// Usage
Database mysql = new MySQLDatabase();
ReportGenerator generator = new ReportGenerator(mysql);

// Easy to switch
Database postgres = new PostgreSQLDatabase();
ReportGenerator generator2 = new ReportGenerator(postgres);
```

## 🔑 Key Takeaways

✅ **Depend on abstractions** - Not concrete classes
✅ **Use dependency injection** - Don't create dependencies
✅ **Program to interface** - Declare types as interfaces
✅ **Invert control** - Let framework manage dependencies
✅ **Easy to test** - Mock dependencies
✅ **Flexible design** - Switch implementations easily

## 💡 Interview Tips

### Common Questions:

**Q: What is Dependency Inversion Principle?**
A: High-level modules should not depend on low-level modules. Both should depend on abstractions (interfaces). This makes code flexible, testable, and maintainable.

**Q: What's the difference between DIP and Dependency Injection?**
A: DIP is a principle (design guideline). Dependency Injection is a technique to achieve DIP. DIP says "depend on abstractions"; DI is how you provide those dependencies.

**Q: How does DIP help with testing?**
A: Since classes depend on interfaces, you can easily pass mock implementations during testing. No need for real databases, networks, etc.

**Q: Give a real-world example of DIP.**
A: Notification system - instead of NotificationService depending on EmailService directly, it depends on MessageService interface. Can then use Email, SMS, Push notifications interchangeably.

**Q: How is DIP related to other SOLID principles?**
A: DIP works with OCP (open for extension through new implementations), ISP (depend on focused interfaces), and LSP (implementations must be substitutable).

## 🎯 DIP in Action

### Without DIP:
```
OrderService → MySQLRepository (concrete)
              ↓
              (tightly coupled)
```

**Problems:**
- Can't switch databases
- Hard to test
- Changes ripple

### With DIP:
```
OrderService → Repository (interface) ← MySQLRepository
                                      ← PostgreSQLRepository
                                      ← InMemoryRepository (for tests)
```

**Benefits:**
- Easy to switch
- Easy to test
- Flexible design

## 📁 Code Examples

- `bad-example/` - Shows DIP violations (tight coupling)
- `good-example/` - Shows proper design (dependency injection)

Study both to understand the difference!

## 🌟 Best Practices

### Do:
✅ Define interfaces for dependencies
✅ Use constructor injection
✅ Depend on abstractions
✅ Let IoC container manage dependencies
✅ Keep interfaces focused (ISP)

### Don't:
❌ Create dependencies inside classes
❌ Depend on concrete implementations
❌ Use `new` for dependencies
❌ Mix business logic with object creation
❌ Ignore testability

## 🔗 DIP and Design Patterns

### Patterns That Use DIP:
- **Strategy** - Depends on algorithm interface
- **Factory** - Creates objects based on interface
- **Observer** - Depends on observer interface
- **Decorator** - Depends on component interface
- **Adapter** - Implements target interface

## ➡️ Next Steps

1. Review the bad example to see violations
2. Study the good example to see the solution
3. Compare the approaches
4. Practice dependency injection
5. Review all SOLID principles together

---

**Remember**: DIP is about depending on abstractions (what) not implementations (how). It makes your code flexible, testable, and maintainable!

## 🎉 SOLID Principles Complete!

You've now learned all 5 SOLID principles:
- ✅ **S**ingle Responsibility
- ✅ **O**pen/Closed
- ✅ **L**iskov Substitution
- ✅ **I**nterface Segregation
- ✅ **D**ependency Inversion

**These principles work together to create maintainable, flexible, and robust software!**

# Creational Patterns - FAANG Interview FAQ

## 📚 Overview

**Creational patterns** deal with object creation mechanisms, trying to create objects in a manner suitable to the situation.

### The 5 Creational Patterns:
1. **Singleton** ⭐⭐⭐ - One instance only
2. **Factory Method** ⭐⭐⭐ - Defer creation to subclasses
3. **Abstract Factory** ⭐⭐ - Families of related objects
4. **Builder** ⭐⭐ - Construct complex objects step-by-step
5. **Prototype** ⭐ - Clone objects

---

## 🎯 Most Frequently Asked Questions (FAANG)

### Q1: What are Creational Design Patterns?

**Answer:**

Creational patterns abstract the instantiation process, making the system independent of how objects are created, composed, and represented.

**Key Goals:**
- Encapsulate knowledge about concrete classes
- Hide object creation logic
- Make system flexible about what gets created
- Promote loose coupling

**Gang of Four (GoF) Quote:**
> "Creational design patterns abstract the instantiation process. They help make a system independent of how its objects are created, composed, and represented."

---

### Q2: When should I use which Creational pattern?

**Answer:**

| Pattern | When to Use | Example |
|---------|-------------|---------|
| **Singleton** | Need exactly one instance | Logger, Config, DB Pool |
| **Factory Method** | Don't know exact type until runtime | Document creator |
| **Abstract Factory** | Create families of related objects | UI themes (Button+Window for Win/Mac) |
| **Builder** | Object has many optional parameters | HTTP Request, Computer |
| **Prototype** | Object creation is expensive | Game objects, Templates |

**Decision Tree:**
```
Need multiple instances?
├─ No → Singleton
└─ Yes → Need complex construction?
    ├─ Yes → Many optional params?
    │   ├─ Yes → Builder
    │   └─ No → Factory Method or Abstract Factory
    └─ No → Creation expensive?
        ├─ Yes → Prototype
        └─ No → Factory Method
```

---

### Q3: Explain Singleton pattern and its thread safety issues

**Answer:**

**Singleton ensures one instance:**
```java
public class Singleton {
    private static volatile Singleton instance;
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

**Thread Safety Issues:**
- Without synchronization: Multiple instances possible
- With synchronized method: Performance hit
- Double-checked locking: Need `volatile` keyword
- Best: Bill Pugh (static inner class) or Enum

**Most Asked Follow-ups:**
1. Why volatile keyword?
2. How to prevent reflection attacks?
3. Serialization issues?
4. Singleton vs Static class?

---

### Q4: Factory Method vs Abstract Factory - Key Differences?

**Answer:**

| Aspect | Factory Method | Abstract Factory |
|--------|---------------|------------------|
| **Focus** | Single product type | Family of products |
| **Methods** | One factory method | Multiple factory methods |
| **Structure** | Inheritance (subclasses) | Composition (interface) |
| **Example** | Document (PDF or Word) | UI Theme (Button+Checkbox for Win/Mac) |
| **Complexity** | Simpler | More complex |

**Factory Method Example:**
```java
abstract class DocumentFactory {
    abstract Document createDocument();
}

class PDFFactory extends DocumentFactory {
    Document createDocument() { return new PDFDocument(); }
}
```

**Abstract Factory Example:**
```java
interface GUIFactory {
    Button createButton();
    Checkbox createCheckbox();
}

class WindowsFactory implements GUIFactory {
    Button createButton() { return new WindowsButton(); }
    Checkbox createCheckbox() { return new WindowsCheckbox(); }
}
```

**Key Point:** Abstract Factory is composition of Factory Methods

---

### Q5: When to use Builder pattern? Give example.

**Answer:**

**Use Builder when:**
- Object has 4+ optional parameters
- Need immutable objects
- Avoid telescoping constructors

**Problem (Telescoping Constructors):**
```java
// BAD - confusing!
Computer c = new Computer("Intel i7", "16GB", "512GB", null, true, false);
```

**Solution (Builder Pattern):**
```java
Computer c = new Computer.Builder("Intel i7", "16GB")
                .storage("512GB")
                .hasWiFi(true)
                .build();
```

**Implementation:**
```java
public class Computer {
    private final String CPU;
    private final String RAM;
    private final String storage;
    private final boolean hasWiFi;
    
    private Computer(Builder builder) {
        this.CPU = builder.CPU;
        this.RAM = builder.RAM;
        this.storage = builder.storage;
        this.hasWiFi = builder.hasWiFi;
    }
    
    public static class Builder {
        private final String CPU;
        private final String RAM;
        private String storage = "256GB";
        private boolean hasWiFi = false;
        
        public Builder(String CPU, String RAM) {
            this.CPU = CPU;
            this.RAM = RAM;
        }
        
        public Builder storage(String storage) {
            this.storage = storage;
            return this;
        }
        
        public Builder hasWiFi(boolean hasWiFi) {
            this.hasWiFi = hasWiFi;
            return this;
        }
        
        public Computer build() {
            return new Computer(this);
        }
    }
}
```

---

### Q6: Prototype pattern - Shallow vs Deep copy?

**Answer:**

**Shallow Copy:**
- Copies primitive fields
- Shares reference to objects
- Problem: Modifying nested object affects both

```java
class Person implements Cloneable {
    String name;
    Address address; // Reference
    
    public Person clone() {
        return (Person) super.clone(); // Shallow
    }
}

Person p1 = new Person("John", new Address("NYC"));
Person p2 = p1.clone();
p2.address.city = "LA"; // Changes p1's address too!
```

**Deep Copy:**
- Copies everything including nested objects
- Each object fully independent

```java
class Person implements Cloneable {
    String name;
    Address address;
    
    public Person clone() {
        Person cloned = (Person) super.clone();
        cloned.address = new Address(this.address); // Deep
        return cloned;
    }
}

Person p1 = new Person("John", new Address("NYC"));
Person p2 = p1.clone();
p2.address.city = "LA"; // Only p2 changes
```

**Interview Tip:** Always clarify if interviewer wants shallow or deep copy!

---

## 🎓 GoF Principles for Creational Patterns

### From Gang of Four Book:

**1. Encapsulate Object Creation**
> "A creational pattern encapsulates knowledge about which concrete classes the system uses and hides how instances of these classes are created and composed."

**2. Factory Method Principle**
> "Define an interface for creating an object, but let subclasses decide which class to instantiate. Factory Method lets a class defer instantiation to subclasses."

**3. Abstract Factory Principle**
> "Provide an interface for creating families of related or dependent objects without specifying their concrete classes."

**4. Builder Principle**
> "Separate the construction of a complex object from its representation so that the same construction process can create different representations."

**5. Prototype Principle**
> "Specify the kinds of objects to create using a prototypical instance, and create new objects by copying this prototype."

**6. Singleton Principle**
> "Ensure a class has only one instance, and provide a global point of access to it."

---

## 🔥 Common Interview Scenarios

### Scenario 1: "Design a connection pool"

**Answer:** Use **Singleton + Object Pool**

```java
public class ConnectionPool {
    private static ConnectionPool instance;
    private Queue<Connection> pool;
    private static final int MAX = 10;
    
    private ConnectionPool() {
        pool = new LinkedList<>();
        for(int i = 0; i < MAX; i++) {
            pool.offer(createConnection());
        }
    }
    
    public static synchronized ConnectionPool getInstance() {
        if(instance == null) {
            instance = new ConnectionPool();
        }
        return instance;
    }
    
    public synchronized Connection getConnection() {
        if(pool.isEmpty()) {
            return createConnection();
        }
        return pool.poll();
    }
    
    public synchronized void releaseConnection(Connection conn) {
        pool.offer(conn);
    }
}
```

---

### Scenario 2: "Build a document editor that supports multiple formats"

**Answer:** Use **Abstract Factory**

```java
interface DocumentFactory {
    Header createHeader();
    Body createBody();
    Footer createFooter();
}

class PDFFactory implements DocumentFactory {
    Header createHeader() { return new PDFHeader(); }
    Body createBody() { return new PDFBody(); }
    Footer createFooter() { return new PDFFooter(); }
}

class HTMLFactory implements DocumentFactory {
    Header createHeader() { return new HTMLHeader(); }
    Body createBody() { return new HTMLBody(); }
    Footer createFooter() { return new HTMLFooter(); }
}
```

---

### Scenario 3: "Create objects with complex configuration"

**Answer:** Use **Builder**

```java
HttpRequest request = new HttpRequest.Builder("https://api.com")
    .method("POST")
    .addHeader("Content-Type", "application/json")
    .body("{\"name\":\"John\"}")
    .timeout(5000)
    .build();
```

---

## 💡 Pro Tips for Interviews

### 1. Start Simple
- Begin with basic implementation
- Mention improvements (thread safety, etc.)
- Show you understand tradeoffs

### 2. Discuss Tradeoffs
Every pattern has pros and cons:
- **Singleton:** Global state vs single instance
- **Factory:** Flexibility vs complexity
- **Builder:** Readability vs boilerplate
- **Prototype:** Performance vs deep copy complexity

### 3. Know Real-World Usage
Mention where patterns are used:
- **Singleton:** `java.lang.Runtime`, Spring Beans
- **Factory:** `Calendar.getInstance()`
- **Abstract Factory:** JDBC `DriverManager`
- **Builder:** `StringBuilder`, Lombok `@Builder`
- **Prototype:** `Object.clone()`

### 4. Common Mistakes to Avoid
- ❌ Not handling thread safety in Singleton
- ❌ Forgetting deep copy in Prototype
- ❌ Overusing patterns where simple `new` would work
- ❌ Not knowing when to use which pattern

---

## 📊 Comparison Matrix

| Pattern | Complexity | Performance | Flexibility | When to Use |
|---------|-----------|-------------|-------------|-------------|
| **Singleton** | Low | High | Low | Need one instance |
| **Factory Method** | Medium | Medium | High | Runtime type decision |
| **Abstract Factory** | High | Medium | High | Family of objects |
| **Builder** | Medium | Medium | High | Many optional params |
| **Prototype** | Low | High | Medium | Expensive creation |

---

## 🎯 Quick Reference

### Most Common Interview Questions:
1. ✅ Implement thread-safe Singleton
2. ✅ Factory Method vs Abstract Factory
3. ✅ When to use Builder
4. ✅ Shallow vs Deep copy in Prototype
5. ✅ Singleton disadvantages

### Must Know for FAANG:
- **Google:** Builder, Factory Method
- **Amazon:** Singleton (thread safety focus)
- **Microsoft:** All creational patterns
- **Meta:** Factory patterns, Builder

---

**Next:** [Behavioral Patterns FAQ →](../behavioral/INTERVIEW_FAQ.md)

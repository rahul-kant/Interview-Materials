# Singleton Pattern - Interview Questions & Answers

## 🎯 Frequently Asked Questions

### Q1: Implement a thread-safe Singleton in Java

**Answer:**

```java
public class ThreadSafeSingleton {
    private static volatile ThreadSafeSingleton instance;
    
    private ThreadSafeSingleton() {
        // Prevent reflection attack
        if (instance != null) {
            throw new RuntimeException("Use getInstance() method");
        }
    }
    
    public static ThreadSafeSingleton getInstance() {
        if (instance == null) {
            synchronized (ThreadSafeSingleton.class) {
                if (instance == null) {
                    instance = new ThreadSafeSingleton();
                }
            }
        }
        return instance;
    }
}
```

**Key Points:**
- Double-checked locking
- `volatile` prevents instruction reordering
- Synchronized block for thread safety
- Check prevents reflection attack

---

### Q2: Why do we need `volatile` keyword in double-checked locking?

**Answer:**

Without `volatile`, due to Java Memory Model, another thread might see a partially constructed object.

**Problem without volatile:**
```
Thread 1: Allocates memory
Thread 1: Starts constructor
Thread 2: Sees instance != null
Thread 2: Returns half-initialized object!
Thread 1: Finishes constructor
```

**With volatile:**
- Prevents instruction reordering
- Ensures visibility across threads
- Happens-before guarantee

---

### Q3: What's the difference between lazy and eager initialization?

**Answer:**

**Eager Initialization:**
```java
public class EagerSingleton {
    private static final EagerSingleton instance = new EagerSingleton();
    
    private EagerSingleton() {}
    
    public static EagerSingleton getInstance() {
        return instance;
    }
}
```
- Created at class loading
- Thread-safe by default
- Wastes memory if never used

**Lazy Initialization:**
```java
public class LazySingleton {
    private static LazySingleton instance;
    
    private LazySingleton() {}
    
    public static synchronized LazySingleton getInstance() {
        if (instance == null) {
            instance = new LazySingleton();
        }
        return instance;
    }
}
```
- Created when first requested
- Saves memory
- Needs thread safety handling

---

### Q4: How can Singleton be broken?

**Answer:**

**1. Reflection:**
```java
Constructor<Singleton> constructor = Singleton.class.getDeclaredConstructor();
constructor.setAccessible(true);
Singleton instance = constructor.newInstance(); // New instance!
```

**2. Serialization:**
```java
// Serialize
ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("singleton.ser"));
oos.writeObject(Singleton.getInstance());

// Deserialize - NEW instance created!
ObjectInputStream ois = new ObjectInputStream(new FileInputStream("singleton.ser"));
Singleton instance = (Singleton) ois.readObject();
```

**3. Cloning:**
```java
Singleton clone = (Singleton) Singleton.getInstance().clone(); // New instance!
```

**Solutions:**
- Use Enum Singleton (best)
- Throw exception in constructor if instance exists
- Implement `readResolve()` for serialization
- Override `clone()` to throw exception

---

### Q5: What's the best way to implement Singleton?

**Answer:**

**Enum Singleton (Recommended by Joshua Bloch):**
```java
public enum Singleton {
    INSTANCE;
    
    public void doSomething() {
        // business logic
    }
}
```

**Advantages:**
- Thread-safe by default
- Serialization-safe
- Reflection-safe
- Simple and concise

**Alternative - Bill Pugh Singleton:**
```java
public class Singleton {
    private Singleton() {}
    
    private static class SingletonHolder {
        private static final Singleton INSTANCE = new Singleton();
    }
    
    public static Singleton getInstance() {
        return SingletonHolder.INSTANCE;
    }
}
```

**Advantages:**
- Lazy initialization
- Thread-safe without synchronization
- High performance

---

### Q6: Singleton vs Static Class?

**Answer:**

| Aspect | Singleton | Static Class |
|--------|-----------|--------------|
| Instantiation | One instance | No instances |
| Interface | Can implement | Cannot |
| Inheritance | Can extend | Cannot |
| Polymorphism | Supports | No |
| Testing | Can mock | Hard to mock |
| Lazy Loading | Yes | No |

**Use Singleton when:**
- Need to implement interface
- Want lazy initialization
- Need to pass as parameter
- Want to subclass

**Use Static Class when:**
- Utility functions only
- No state needed
- Simplicity preferred

---

### Q7: How do you test Singleton classes?

**Answer:**

**Problem:** Hard to mock, global state

**Solutions:**

**1. Dependency Injection:**
```java
public class Service {
    private Logger logger;
    
    // Constructor injection
    public Service(Logger logger) {
        this.logger = logger;
    }
    
    // In production: new Service(Logger.getInstance())
    // In tests: new Service(mockLogger)
}
```

**2. Reset Method (for tests only):**
```java
public class Singleton {
    private static Singleton instance;
    
    // Only for testing!
    public static void reset() {
        instance = null;
    }
}
```

**3. Use Interface:**
```java
public interface Logger {
    void log(String message);
}

public class LoggerImpl implements Logger {
    // Singleton implementation
}

// Easy to mock Logger interface in tests
```

---

### Q8: What are the disadvantages of Singleton?

**Answer:**

**1. Global State**
- Tight coupling
- Hard to track dependencies
- Hidden dependencies

**2. Testing Difficulties**
- Hard to mock
- State persists between tests
- Can cause test interdependencies

**3. Single Responsibility Violation**
- Controls its own creation
- Does business logic
- Two responsibilities

**4. Concurrency Issues**
- Thread safety complexity
- Performance overhead (if synchronized)

**5. Flexibility**
- Hard to change to multiple instances later
- Limits extensibility

---

### Q9: When should you NOT use Singleton?

**Answer:**

❌ **Don't use when:**
- You might need multiple instances in future
- In unit tests (use dependency injection)
- State varies per user/request
- Creating tight coupling
- The class is stateless (use static methods)

✅ **Use when:**
- Truly need only one instance
- Global access is required
- Resource is expensive to create
- Examples: Logger, Config, DB Connection Pool

---

### Q10: Explain Bill Pugh Singleton approach

**Answer:**

```java
public class Singleton {
    
    private Singleton() {}
    
    // Inner static class
    private static class SingletonHolder {
        private static final Singleton INSTANCE = new Singleton();
    }
    
    public static Singleton getInstance() {
        return SingletonHolder.INSTANCE;
    }
}
```

**How it works:**
1. Inner class not loaded until `getInstance()` called
2. Class loading is thread-safe in Java
3. No synchronization needed
4. Lazy initialization achieved

**Why it's best:**
- Lazy loading ✅
- Thread-safe ✅
- High performance ✅
- No synchronization overhead ✅

---

## 🎓 Advanced Questions

### Q11: Can Singleton be subclassed?

**Answer:**

**Technically yes, but problematic:**

```java
public class Singleton {
    private static Singleton instance;
    
    protected Singleton() {} // Protected, not private
    
    public static Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }
}

public class SingletonChild extends Singleton {
    private static SingletonChild instance;
    
    private SingletonChild() {}
    
    public static SingletonChild getInstance() {
        if (instance == null) {
            instance = new SingletonChild();
        }
        return instance;
    }
}
```

**Problems:**
- Violates Singleton principle
- Multiple instances possible
- Confusing design

**Better approach:**
- Use composition over inheritance
- Use Strategy pattern for variations

---

### Q12: Singleton in distributed systems?

**Answer:**

**Problem:** Singleton guarantees one instance per JVM, not per cluster

**Solutions:**

**1. Distributed Cache (Redis):**
```java
public class DistributedSingleton {
    public static synchronized DistributedSingleton getInstance() {
        String key = "singleton:instance";
        if (redis.exists(key)) {
            return redis.get(key);
        } else {
            DistributedSingleton instance = new DistributedSingleton();
            redis.set(key, instance);
            return instance;
        }
    }
}
```

**2. Leader Election (ZooKeeper)**

**3. Database-based Locking**

**Best Practice:**
- Rethink if you need Singleton in distributed system
- Consider service per instance
- Use proper distributed coordination

---

## 💡 Coding Challenges

### Challenge 1: Implement serialization-safe Singleton

```java
public class SerializableSingleton implements Serializable {
    private static final long serialVersionUID = 1L;
    private static volatile SerializableSingleton instance;
    
    private SerializableSingleton() {
        if (instance != null) {
            throw new RuntimeException("Use getInstance()");
        }
    }
    
    public static SerializableSingleton getInstance() {
        if (instance == null) {
            synchronized (SerializableSingleton.class) {
                if (instance == null) {
                    instance = new SerializableSingleton();
                }
            }
        }
        return instance;
    }
    
    // Prevent creating new instance on deserialization
    protected Object readResolve() {
        return getInstance();
    }
    
    // Prevent cloning
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
```

---

### Challenge 2: Implement connection pool using Singleton

```java
public class ConnectionPool {
    private static final int MAX_CONNECTIONS = 10;
    private Queue<Connection> availableConnections;
    private Set<Connection> usedConnections;
    
    private ConnectionPool() {
        availableConnections = new LinkedList<>();
        usedConnections = new HashSet<>();
        
        // Initialize pool
        for (int i = 0; i < MAX_CONNECTIONS; i++) {
            availableConnections.add(createConnection());
        }
    }
    
    private static class ConnectionPoolHolder {
        private static final ConnectionPool INSTANCE = new ConnectionPool();
    }
    
    public static ConnectionPool getInstance() {
        return ConnectionPoolHolder.INSTANCE;
    }
    
    public synchronized Connection getConnection() {
        if (availableConnections.isEmpty()) {
            throw new RuntimeException("No connections available");
        }
        Connection conn = availableConnections.poll();
        usedConnections.add(conn);
        return conn;
    }
    
    public synchronized void releaseConnection(Connection conn) {
        usedConnections.remove(conn);
        availableConnections.offer(conn);
    }
    
    private Connection createConnection() {
        // Create actual DB connection
        return new Connection();
    }
}
```

---

## 🎯 Quick Reference

### Implementation Checklist:
- [ ] Private constructor
- [ ] Static instance variable
- [ ] Static getInstance() method
- [ ] Thread safety handled
- [ ] Lazy or eager initialization decided
- [ ] Serialization considered
- [ ] Reflection attack prevented

### Interview Tips:
1. Start with Bill Pugh or Enum
2. Explain thread safety clearly
3. Mention `volatile` keyword importance
4. Discuss pros and cons
5. Give real-world examples
6. Be ready for follow-ups on weaknesses

---

**Next:** [Factory Method Q&A →](../02-factory-method/INTERVIEW_QA.md)

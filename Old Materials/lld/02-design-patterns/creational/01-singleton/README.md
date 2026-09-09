# Singleton Pattern ⭐⭐⭐

## 📋 Intent

**Ensure a class has only one instance and provide a global point of access to it.**

---

## 🎯 Problem

- You need exactly one instance of a class
- That instance must be accessible globally
- The instance should be created only when needed (lazy initialization)

**Example Scenario:**
- Database connection pool
- Logger
- Configuration manager
- Cache

---

## ✅ Solution

Create a class with:
1. Private constructor (prevent direct instantiation)
2. Static instance variable
3. Static method to get the instance

---

## 💻 Implementation

### Basic Singleton (Not Thread-Safe)

```java
public class BasicSingleton {
    private static BasicSingleton instance;
    
    private BasicSingleton() {
        // Private constructor
    }
    
    public static BasicSingleton getInstance() {
        if (instance == null) {
            instance = new BasicSingleton();
        }
        return instance;
    }
}
```

**Problem:** Not thread-safe! Multiple threads can create multiple instances.

---

### Thread-Safe Singleton (Synchronized Method)

```java
public class ThreadSafeSingleton {
    private static ThreadSafeSingleton instance;
    
    private ThreadSafeSingleton() {}
    
    public static synchronized ThreadSafeSingleton getInstance() {
        if (instance == null) {
            instance = new ThreadSafeSingleton();
        }
        return instance;
    }
}
```

**Pros:** Thread-safe
**Cons:** Performance overhead (synchronized on every call)

---

### Double-Checked Locking (Best for Lazy Init)

```java
public class DoubleCheckedSingleton {
    private static volatile DoubleCheckedSingleton instance;
    
    private DoubleCheckedSingleton() {}
    
    public static DoubleCheckedSingleton getInstance() {
        if (instance == null) {
            synchronized (DoubleCheckedSingleton.class) {
                if (instance == null) {
                    instance = new DoubleCheckedSingleton();
                }
            }
        }
        return instance;
    }
}
```

**Pros:** Thread-safe, better performance
**Key:** `volatile` keyword prevents instruction reordering

---

### Eager Initialization (Thread-Safe by Default)

```java
public class EagerSingleton {
    private static final EagerSingleton instance = new EagerSingleton();
    
    private EagerSingleton() {}
    
    public static EagerSingleton getInstance() {
        return instance;
    }
}
```

**Pros:** Simple, thread-safe
**Cons:** Instance created even if never used

---

### Bill Pugh Singleton (Best Practice)

```java
public class BillPughSingleton {
    
    private BillPughSingleton() {}
    
    private static class SingletonHelper {
        private static final BillPughSingleton INSTANCE = new BillPughSingleton();
    }
    
    public static BillPughSingleton getInstance() {
        return SingletonHelper.INSTANCE;
    }
}
```

**Pros:** Lazy initialization, thread-safe, no synchronization overhead
**How:** Inner static class loaded only when getInstance() called

---

### Enum Singleton (Best for Serialization)

```java
public enum EnumSingleton {
    INSTANCE;
    
    public void doSomething() {
        System.out.println("Doing something");
    }
}

// Usage: EnumSingleton.INSTANCE.doSomething();
```

**Pros:** Thread-safe, serialization-safe, prevents reflection attacks
**Recommended by Joshua Bloch (Effective Java)**

---

## 🎨 Real-World Example

See [Logger.java](Logger.java) for complete example

---

## ✅ When to Use

- **Database Connections:** Connection pool should be single instance
- **Logger:** Centralized logging
- **Configuration:** Single config manager
- **Cache:** Single cache instance
- **Thread Pools:** Single pool manager

---

## ❌ When NOT to Use

- When you need multiple instances
- When state needs to vary
- In unit tests (hard to mock)
- When it creates tight coupling

---

## 🎯 Pros and Cons

### Pros ✅
- Controlled access to single instance
- Reduced memory footprint
- Lazy initialization possible
- Can be subclassed (with care)

### Cons ❌
- Global state (can lead to tight coupling)
- Difficult to unit test
- Violates Single Responsibility Principle (controls its creation + business logic)
- Multithreading complexity

---

## 🔄 Comparison with Other Patterns

### Singleton vs Factory
- **Singleton:** Controls instantiation to one instance
- **Factory:** Creates multiple instances of different types

### Singleton vs Static Class
- **Singleton:** Can implement interfaces, be subclassed
- **Static Class:** Cannot, but simpler

---

## 📊 Thread Safety Comparison

| Approach | Thread Safe | Performance | Lazy Init |
|----------|-------------|-------------|-----------|
| Basic | ❌ | ⚡⚡⚡ | ✅ |
| Synchronized | ✅ | ⚡ | ✅ |
| Double-Check | ✅ | ⚡⚡ | ✅ |
| Eager | ✅ | ⚡⚡⚡ | ❌ |
| Bill Pugh | ✅ | ⚡⚡⚡ | ✅ |
| Enum | ✅ | ⚡⚡⚡ | ❌ |

**Best Choice:** Bill Pugh or Enum

---

## 💡 Common Pitfalls

### 1. Reflection Attack
```java
// Can break Singleton!
Constructor<SingletonClass> constructor = SingletonClass.class.getDeclaredConstructor();
constructor.setAccessible(true);
SingletonClass instance = constructor.newInstance();
```

**Solution:** Use Enum or throw exception in constructor if instance exists

### 2. Serialization Issue
```java
// After deserialization, you get a NEW instance!
```

**Solution:** Implement `readResolve()` method
```java
protected Object readResolve() {
    return getInstance();
}
```

### 3. Cloning Issue
**Solution:** Override `clone()` and throw exception

---

## 🎓 Interview Tips

### Common Questions:
1. "Implement a thread-safe Singleton"
2. "What's the difference between eager and lazy initialization?"
3. "How do you prevent reflection attacks?"
4. "Why use double-checked locking?"
5. "What's wrong with Singleton pattern?"

### What to Mention:
- Thread safety concerns
- Lazy vs Eager initialization
- Double-checked locking with volatile
- Bill Pugh approach
- Enum singleton (best practice)
- Serialization issues
- Testing difficulties

---

## 📝 Interview Questions & Answers

See [INTERVIEW_QA.md](INTERVIEW_QA.md) for detailed Q&A

---

## 🔗 Related Patterns

- **Factory Method:** Often implemented as Singleton
- **Abstract Factory:** Often implemented as Singleton
- **Facade:** Often implemented as Singleton

---

## 📚 Further Reading

- Effective Java by Joshua Bloch
- Design Patterns by Gang of Four
- Java Concurrency in Practice

---

**Next Pattern:** [Factory Method →](../02-factory-method/)

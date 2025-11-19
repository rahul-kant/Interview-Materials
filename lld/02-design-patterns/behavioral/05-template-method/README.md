# Template Method Pattern ⭐⭐

## 📋 Intent

**Define the skeleton of an algorithm in an operation, deferring some steps to subclasses. Template Method lets subclasses redefine certain steps of an algorithm without changing the algorithm's structure.**

---

## 💻 Implementation

```java
abstract class DataProcessor {
    // Template method
    public final void process() {
        readData();
        processData();
        saveData();
    }
    
    abstract void readData();
    abstract void processData();
    
    void saveData() {
        System.out.println("Saving data");
    }
}

class CSVProcessor extends DataProcessor {
    void readData() {
        System.out.println("Reading CSV");
    }
    
    void processData() {
        System.out.println("Processing CSV");
    }
}

class JSONProcessor extends DataProcessor {
    void readData() {
        System.out.println("Reading JSON");
    }
    
    void processData() {
        System.out.println("Processing JSON");
    }
}
```

---

## 🔄 Template Method vs Strategy

| Aspect | Template Method | Strategy |
|--------|----------------|----------|
| **Mechanism** | Inheritance | Composition |
| **Flexibility** | Compile-time | Runtime |
| **Coupling** | Tight | Loose |

---

## 🎯 Key Takeaways

1. **Defines algorithm skeleton**
2. **Subclasses implement steps**
3. **Reuses common code**
4. **Inheritance-based**

---

**Next Pattern:** [Iterator →](../18-iterator/)

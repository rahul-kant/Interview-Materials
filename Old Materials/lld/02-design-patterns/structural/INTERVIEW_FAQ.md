# Structural Patterns - FAANG Interview FAQ

## 📚 Overview

**Structural patterns** deal with object composition, creating relationships between objects to form larger structures while keeping them flexible and efficient.

### The 7 Structural Patterns:
1. **Adapter** ⭐⭐ - Convert interfaces
2. **Decorator** ⭐⭐⭐ - Add behavior dynamically
3. **Proxy** ⭐⭐ - Control access
4. **Composite** ⭐⭐ - Tree structures
5. **Bridge** ⭐ - Separate abstraction from implementation
6. **Facade** ⭐⭐ - Simplified interface
7. **Flyweight** ⭐ - Share objects efficiently

---

## 🎯 Most Frequently Asked Questions (FAANG)

### Q1: What are Structural Design Patterns?

**Answer:**

Structural patterns explain how to assemble objects and classes into larger structures while keeping them flexible and efficient.

**Key Goals:**
- Compose objects into larger structures
- Keep structure flexible
- Maintain efficiency
- Define relationships

**Gang of Four (GoF) Quote:**
> "Structural patterns are concerned with how classes and objects are composed to form larger structures."

---

### Q2: Adapter vs Decorator vs Proxy - What's the difference?

**Answer:**

All three wrap objects, but with different intents:

| Pattern | Intent | Changes Interface | Example |
|---------|--------|-------------------|---------|
| **Adapter** | Make incompatible interfaces work | Yes | Legacy code integration |
| **Decorator** | Add behavior dynamically | No | Coffee + milk + sugar |
| **Proxy** | Control access | No | Lazy loading images |

**Adapter Example:**
```java
// Convert incompatible interface
class PaymentAdapter implements NewPayment {
    private LegacyPayment legacy;
    
    public void pay(double amount) {
        legacy.makePayment((int)(amount * 100));
    }
}
```

**Decorator Example:**
```java
// Add features dynamically
Coffee coffee = new SimpleCoffee();
coffee = new MilkDecorator(coffee);
coffee = new SugarDecorator(coffee);
```

**Proxy Example:**
```java
// Control access (lazy loading)
class ProxyImage implements Image {
    private RealImage real;
    
    public void display() {
        if (real == null) {
            real = new RealImage(); // Load only when needed
        }
        real.display();
    }
}
```

---

### Q3: Explain Decorator pattern with Java I/O example

**Answer:**

Decorator is heavily used in Java I/O to add functionality:

```java
// Plain file reading
InputStream in = new FileInputStream("file.txt");

// Add buffering (Decorator)
in = new BufferedInputStream(in);

// Add data reading capability (Decorator)
in = new DataInputStream(in);

// Stack of decorators!
```

**Structure:**
- `InputStream` - Component interface
- `FileInputStream` - Concrete component
- `BufferedInputStream` - Decorator (adds buffering)
- `DataInputStream` - Decorator (adds data reading)

**Why Decorator?**
- Avoids class explosion (BufferedFileInputStream, DataFileInputStream, etc.)
- Mix and match at runtime
- Open/Closed Principle

---

### Q4: When to use Composite pattern?

**Answer:**

Use Composite when you need to represent **part-whole hierarchies** and treat individual and composite objects uniformly.

**Classic Example - File System:**
```java
interface FileSystemItem {
    void display();
    int getSize();
}

class File implements FileSystemItem {
    private String name;
    private int size;
    
    public void display() {
        System.out.println("File: " + name);
    }
    
    public int getSize() {
        return size;
    }
}

class Folder implements FileSystemItem {
    private List<FileSystemItem> items = new ArrayList<>();
    
    public void add(FileSystemItem item) {
        items.add(item);
    }
    
    public void display() {
        for (FileSystemItem item : items) {
            item.display(); // Recursive!
        }
    }
    
    public int getSize() {
        int total = 0;
        for (FileSystemItem item : items) {
            total += item.getSize();
        }
        return total;
    }
}
```

**Key Point:** Client treats File and Folder the same way!

**When to Use:**
- Tree structures (file systems, org charts)
- UI component hierarchies
- Menu systems

---

### Q5: Facade vs Adapter - What's the difference?

**Answer:**

| Aspect | Facade | Adapter |
|--------|--------|---------|
| **Intent** | Simplify complex subsystem | Make incompatible compatible |
| **Interface** | New simplified interface | Convert to expected interface |
| **Complexity** | Simplifies many classes | Wraps one class |
| **Goal** | Ease of use | Compatibility |

**Facade Example:**
```java
class HomeTheaterFacade {
    private DVDPlayer dvd;
    private Projector projector;
    private SoundSystem sound;
    
    public void watchMovie(String movie) {
        // Simplifies complex operations
        projector.on();
        sound.on();
        dvd.play(movie);
    }
}
```

**Adapter Example:**
```java
class TemperatureAdapter implements CelsiusSensor {
    private FahrenheitSensor fahrenheit;
    
    public double getTemperature() {
        // Converts interface
        return (fahrenheit.getTemp() - 32) * 5/9;
    }
}
```

---

### Q6: What is Flyweight pattern and when to use it?

**Answer:**

Flyweight uses **sharing** to support large numbers of fine-grained objects efficiently.

**Key Concepts:**
- **Intrinsic state** - Shared (immutable)
- **Extrinsic state** - Not shared (passed in)

**Example - Text Editor:**
```java
// Intrinsic state (shared)
class CharacterStyle {
    private String font;
    private int size;
    private String color;
    // Shared among many characters
}

// Extrinsic state (not shared)
class Character {
    private int position; // Unique for each
    private CharacterStyle style; // Shared!
}
```

**Real-World Example:**
```java
// Java's String pool
String s1 = "hello";
String s2 = "hello"; // Same object!
System.out.println(s1 == s2); // true
```

**When to Use:**
- Large number of similar objects
- Memory is a concern
- Most state can be shared

---

## 🎓 GoF Principles for Structural Patterns

### From Gang of Four Book:

**1. Composition Over Inheritance**
> "Favor object composition over class inheritance."

**2. Adapter Principle**
> "Convert the interface of a class into another interface clients expect. Adapter lets classes work together that couldn't otherwise because of incompatible interfaces."

**3. Decorator Principle**
> "Attach additional responsibilities to an object dynamically. Decorators provide a flexible alternative to subclassing for extending functionality."

**4. Proxy Principle**
> "Provide a surrogate or placeholder for another object to control access to it."

**5. Composite Principle**
> "Compose objects into tree structures to represent part-whole hierarchies. Composite lets clients treat individual objects and compositions of objects uniformly."

**6. Bridge Principle**
> "Decouple an abstraction from its implementation so that the two can vary independently."

**7. Facade Principle**
> "Provide a unified interface to a set of interfaces in a subsystem. Facade defines a higher-level interface that makes the subsystem easier to use."

**8. Flyweight Principle**
> "Use sharing to support large numbers of fine-grained objects efficiently."

---

## 🔥 Common Interview Scenarios

### Scenario 1: "Design a logging system with different outputs"

**Answer:** Use **Decorator**

```java
interface Logger {
    void log(String message);
}

class SimpleLogger implements Logger {
    public void log(String message) {
        System.out.println(message);
    }
}

class TimestampDecorator implements Logger {
    private Logger logger;
    
    public TimestampDecorator(Logger logger) {
        this.logger = logger;
    }
    
    public void log(String message) {
        logger.log(LocalDateTime.now() + ": " + message);
    }
}

class EncryptionDecorator implements Logger {
    private Logger logger;
    
    public EncryptionDecorator(Logger logger) {
        this.logger = logger;
    }
    
    public void log(String message) {
        logger.log(encrypt(message));
    }
}

// Usage
Logger logger = new SimpleLogger();
logger = new TimestampDecorator(logger);
logger = new EncryptionDecorator(logger);
logger.log("Important message");
```

---

### Scenario 2: "Integrate with third-party payment system"

**Answer:** Use **Adapter**

```java
// Target interface
interface PaymentGateway {
    void processPayment(double amount, String currency);
}

// Third-party system (can't modify)
class StripePaymentSystem {
    public void makePayment(int cents) {
        System.out.println("Stripe: Processing " + cents + " cents");
    }
}

// Adapter
class StripeAdapter implements PaymentGateway {
    private StripePaymentSystem stripe;
    
    public StripeAdapter() {
        this.stripe = new StripePaymentSystem();
    }
    
    public void processPayment(double amount, String currency) {
        int cents = (int)(amount * 100);
        stripe.makePayment(cents);
    }
}
```

---

### Scenario 3: "Build UI component hierarchy"

**Answer:** Use **Composite**

```java
interface UIComponent {
    void render();
}

class Button implements UIComponent {
    public void render() {
        System.out.println("Render button");
    }
}

class Panel implements UIComponent {
    private List<UIComponent> children = new ArrayList<>();
    
    public void add(UIComponent component) {
        children.add(component);
    }
    
    public void render() {
        for (UIComponent child : children) {
            child.render();
        }
    }
}

// Usage
Panel mainPanel = new Panel();
mainPanel.add(new Button());
Panel subPanel = new Panel();
subPanel.add(new Button());
mainPanel.add(subPanel);
mainPanel.render(); // Renders all!
```

---

## 💡 Pro Tips for Interviews

### 1. Know the Intent
Don't just memorize structure - understand the problem each pattern solves:
- **Adapter** - "I have incompatible interfaces"
- **Decorator** - "I need to add features dynamically"
- **Proxy** - "I need to control access"
- **Composite** - "I have tree structures"
- **Facade** - "System is too complex"

### 2. Recognize Patterns in Code
Interviewers may show code and ask "What pattern is this?"

**Quick Recognition:**
- Wraps object + same interface → **Decorator or Proxy**
- Wraps object + different interface → **Adapter**
- Tree structure with same interface → **Composite**
- Simplifies many classes → **Facade**

### 3. Draw Diagrams
Visual representation helps:
```
Decorator:
Component ← Decorator ← ConcreteDecorator
             ↓
        Component

Adapter:
Target ← Adapter → Adaptee
```

### 4. Common Mistakes to Avoid
- ❌ Confusing Decorator with Proxy
- ❌ Using Adapter when Decorator is better
- ❌ Overusing Facade (God object)
- ❌ Not understanding intrinsic vs extrinsic state in Flyweight

---

## 📊 Comparison Matrix

| Pattern | Complexity | Use Case | Frequency |
|---------|-----------|----------|-----------|
| **Adapter** | Low | Legacy integration | ⭐⭐ |
| **Decorator** | Medium | Add features dynamically | ⭐⭐⭐ |
| **Proxy** | Low | Control access | ⭐⭐ |
| **Composite** | Medium | Tree structures | ⭐⭐ |
| **Bridge** | High | Separate abstraction | ⭐ |
| **Facade** | Low | Simplify subsystem | ⭐⭐ |
| **Flyweight** | High | Memory optimization | ⭐ |

---

## 🎯 Quick Reference

### Most Common Interview Questions:
1. ✅ Adapter vs Decorator vs Proxy
2. ✅ Java I/O and Decorator pattern
3. ✅ When to use Composite
4. ✅ Facade vs Adapter
5. ✅ Flyweight intrinsic vs extrinsic state

### Must Know for FAANG:
- **Google:** Decorator, Composite
- **Amazon:** Adapter, Facade
- **Microsoft:** All structural patterns
- **Meta:** Proxy, Decorator

---

**Next:** [Behavioral Patterns FAQ →](../behavioral/INTERVIEW_FAQ.md)

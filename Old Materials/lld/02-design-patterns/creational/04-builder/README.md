# Builder Pattern ⭐⭐

## 📋 Intent

**Separate the construction of a complex object from its representation, allowing the same construction process to create different representations.**

---

## 🎯 Problem

- Object has many optional parameters (constructor with too many parameters)
- Step-by-step construction needed
- Same construction process should create different representations
- Immutable objects with many fields

**Example Scenarios:**
- Building complex objects (House, Car, Computer)
- Creating HTTP requests with many optional parameters
- Building SQL queries
- Constructing documents with multiple parts

---

## ✅ Solution

1. Create a Builder class
2. Use method chaining (fluent interface)
3. Build method returns final object
4. Make object immutable (optional but recommended)

---

## 💻 Implementation

### Basic Structure

```java
// Product
class Computer {
    // Required parameters
    private final String CPU;
    private final String RAM;
    
    // Optional parameters
    private final String storage;
    private final String GPU;
    private final boolean hasWiFi;
    private final boolean hasBluetooth;
    
    // Private constructor
    private Computer(ComputerBuilder builder) {
        this.CPU = builder.CPU;
        this.RAM = builder.RAM;
        this.storage = builder.storage;
        this.GPU = builder.GPU;
        this.hasWiFi = builder.hasWiFi;
        this.hasBluetooth = builder.hasBluetooth;
    }
    
    // Static nested Builder class
    public static class ComputerBuilder {
        // Required parameters
        private final String CPU;
        private final String RAM;
        
        // Optional parameters - initialize with default values
        private String storage = "256GB SSD";
        private String GPU = "Integrated";
        private boolean hasWiFi = false;
        private boolean hasBluetooth = false;
        
        // Constructor with required parameters
        public ComputerBuilder(String CPU, String RAM) {
            this.CPU = CPU;
            this.RAM = RAM;
        }
        
        // Setter methods return Builder for chaining
        public ComputerBuilder storage(String storage) {
            this.storage = storage;
            return this;
        }
        
        public ComputerBuilder GPU(String GPU) {
            this.GPU = GPU;
            return this;
        }
        
        public ComputerBuilder hasWiFi(boolean hasWiFi) {
            this.hasWiFi = hasWiFi;
            return this;
        }
        
        public ComputerBuilder hasBluetooth(boolean hasBluetooth) {
            this.hasBluetooth = hasBluetooth;
            return this;
        }
        
        // Build method
        public Computer build() {
            return new Computer(this);
        }
    }
    
    @Override
    public String toString() {
        return "Computer [CPU=" + CPU + ", RAM=" + RAM + 
               ", storage=" + storage + ", GPU=" + GPU +
               ", WiFi=" + hasWiFi + ", Bluetooth=" + hasBluetooth + "]";
    }
}

// Usage
Computer computer = new Computer.ComputerBuilder("Intel i7", "16GB")
                        .storage("512GB SSD")
                        .GPU("RTX 3080")
                        .hasWiFi(true)
                        .hasBluetooth(true)
                        .build();
```

---

## 🎨 Real-World Examples

### Example 1: HTTP Request Builder

```java
class HttpRequest {
    private final String url;
    private final String method;
    private final Map<String, String> headers;
    private final Map<String, String> params;
    private final String body;
    private final int timeout;
    
    private HttpRequest(HttpRequestBuilder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.headers = builder.headers;
        this.params = builder.params;
        this.body = builder.body;
        this.timeout = builder.timeout;
    }
    
    public static class HttpRequestBuilder {
        // Required
        private final String url;
        
        // Optional with defaults
        private String method = "GET";
        private Map<String, String> headers = new HashMap<>();
        private Map<String, String> params = new HashMap<>();
        private String body = "";
        private int timeout = 30000; // 30 seconds
        
        public HttpRequestBuilder(String url) {
            this.url = url;
        }
        
        public HttpRequestBuilder method(String method) {
            this.method = method;
            return this;
        }
        
        public HttpRequestBuilder addHeader(String key, String value) {
            this.headers.put(key, value);
            return this;
        }
        
        public HttpRequestBuilder addParam(String key, String value) {
            this.params.put(key, value);
            return this;
        }
        
        public HttpRequestBuilder body(String body) {
            this.body = body;
            return this;
        }
        
        public HttpRequestBuilder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public HttpRequest build() {
            return new HttpRequest(this);
        }
    }
}

// Usage
HttpRequest request = new HttpRequest.HttpRequestBuilder("https://api.example.com/users")
                          .method("POST")
                          .addHeader("Content-Type", "application/json")
                          .addHeader("Authorization", "Bearer token123")
                          .body("{\"name\":\"John\"}")
                          .timeout(5000)
                          .build();
```

### Example 2: Pizza Builder

```java
class Pizza {
    private final String size;
    private final boolean cheese;
    private final boolean pepperoni;
    private final boolean mushrooms;
    private final boolean onions;
    
    private Pizza(PizzaBuilder builder) {
        this.size = builder.size;
        this.cheese = builder.cheese;
        this.pepperoni = builder.pepperoni;
        this.mushrooms = builder.mushrooms;
        this.onions = builder.onions;
    }
    
    public static class PizzaBuilder {
        private final String size;
        private boolean cheese = false;
        private boolean pepperoni = false;
        private boolean mushrooms = false;
        private boolean onions = false;
        
        public PizzaBuilder(String size) {
            this.size = size;
        }
        
        public PizzaBuilder cheese(boolean value) {
            cheese = value;
            return this;
        }
        
        public PizzaBuilder pepperoni(boolean value) {
            pepperoni = value;
            return this;
        }
        
        public PizzaBuilder mushrooms(boolean value) {
            mushrooms = value;
            return this;
        }
        
        public PizzaBuilder onions(boolean value) {
            onions = value;
            return this;
        }
        
        public Pizza build() {
            return new Pizza(this);
        }
    }
}

// Usage
Pizza pizza = new Pizza.PizzaBuilder("Large")
                  .cheese(true)
                  .pepperoni(true)
                  .mushrooms(true)
                  .build();
```

---

## ✅ When to Use

- **Many optional parameters** (more than 3-4)
- **Immutable objects** needed
- **Step-by-step construction** required
- **Different representations** of same object
- **Telescoping constructor problem** (too many constructors)

---

## ❌ When NOT to Use

- Simple objects with few fields
- All fields are required
- No optional parameters
- Object is mutable

---

## 🎯 Pros and Cons

### Pros ✅
- **Readable code** - method chaining
- **Immutability** - object can be immutable
- **Flexible** - optional parameters handled well
- **No telescoping constructors** 
- **Validation** - can validate in build()

### Cons ❌
- **More code** - need to write Builder class
- **Duplication** - fields defined twice
- **Memory overhead** - additional Builder object

---

## 🔄 Builder vs Constructor

**Problem with Constructors:**
```java
// Telescoping constructors - BAD!
public Computer(String CPU, String RAM) { ... }
public Computer(String CPU, String RAM, String storage) { ... }
public Computer(String CPU, String RAM, String storage, String GPU) { ... }
public Computer(String CPU, String RAM, String storage, String GPU, 
                boolean WiFi) { ... }
// Becomes unmanageable!
```

**Builder Solution:**
```java
// Clean and readable - GOOD!
Computer computer = new Computer.ComputerBuilder("Intel i7", "16GB")
                        .storage("512GB")
                        .GPU("RTX 3080")
                        .hasWiFi(true)
                        .build();
```

---

## 🔄 Builder vs Abstract Factory

| Aspect | Builder | Abstract Factory |
|--------|---------|------------------|
| **Focus** | Construct complex object | Create family of objects |
| **Process** | Step-by-step | One-step |
| **Return** | Single type | Multiple types |
| **Emphasis** | How it's built | What is built |

---

## 💡 Design Principles

### Follows:
- ✅ **Single Responsibility** - Separates construction from representation
- ✅ **Immutability** - Objects can be immutable
- ✅ **Fluent Interface** - Readable API

### Variations:
1. **Static Nested Builder** (most common)
2. **Separate Builder Class**
3. **Director + Builder** (classic GoF version)

---

## 🎓 Interview Focus

### Must Know:
1. **Intent** - Construct complex objects step-by-step
2. **When to use** - Many optional parameters
3. **Method chaining** - Fluent interface
4. **vs Constructor** - Solves telescoping constructor problem
5. **Immutability** - How Builder enables it

### Common Questions:
1. "Implement a Builder for User class"
2. "Builder vs Constructor?"
3. "When would you use Builder?"
4. "How to make object immutable with Builder?"
5. "Real-world examples?"

---

## 📝 Classic GoF Version (with Director)

```java
// Product
class House {
    private String foundation;
    private String walls;
    private String roof;
    
    public void setFoundation(String foundation) {
        this.foundation = foundation;
    }
    
    public void setWalls(String walls) {
        this.walls = walls;
    }
    
    public void setRoof(String roof) {
        this.roof = roof;
    }
}

// Builder Interface
interface HouseBuilder {
    void buildFoundation();
    void buildWalls();
    void buildRoof();
    House getHouse();
}

// Concrete Builder
class ConcreteHouseBuilder implements HouseBuilder {
    private House house;
    
    public ConcreteHouseBuilder() {
        this.house = new House();
    }
    
    public void buildFoundation() {
        house.setFoundation("Concrete foundation");
    }
    
    public void buildWalls() {
        house.setWalls("Concrete walls");
    }
    
    public void buildRoof() {
        house.setRoof("Concrete roof");
    }
    
    public House getHouse() {
        return this.house;
    }
}

// Director
class ConstructionEngineer {
    private HouseBuilder builder;
    
    public ConstructionEngineer(HouseBuilder builder) {
        this.builder = builder;
    }
    
    public House constructHouse() {
        builder.buildFoundation();
        builder.buildWalls();
        builder.buildRoof();
        return builder.getHouse();
    }
}

// Usage
HouseBuilder builder = new ConcreteHouseBuilder();
ConstructionEngineer engineer = new ConstructionEngineer(builder);
House house = engineer.constructHouse();
```

**Note:** Modern Java uses simpler static nested builder approach

---

## 📝 Interview Questions & Answers

See [INTERVIEW_QA.md](INTERVIEW_QA.md) for detailed Q&A

---

## 🔗 Related Patterns

- **Abstract Factory** - Can use Builder to create products
- **Composite** - Can use Builder to build tree
- **Singleton** - Builder can be singleton
- **Fluent Interface** - Builder uses fluent API

---

## 📚 Where It's Used

### In Java:
- `StringBuilder` / `StringBuffer`
- `java.util.Calendar.Builder`
- `java.util.stream.Stream.Builder`
- `java.nio.ByteBuffer`

### In Libraries:
- Lombok `@Builder` annotation
- Apache Commons `Builder` interface
- Retrofit (HTTP client)
- OkHttp (HTTP client)

---

## 🎯 Key Takeaways

1. **Solves telescoping constructor** problem
2. **Fluent interface** for readability
3. **Immutable objects** easily created
4. **Optional parameters** handled elegantly
5. **Modern approach** - static nested builder

---

**Complete Example:** [ComputerBuilder.java](ComputerBuilder.java)  
**Demo:** [BuilderDemo.java](BuilderDemo.java)  
**Interview Q&A:** [INTERVIEW_QA.md](INTERVIEW_QA.md)

---

**Next Pattern:** [Prototype →](../05-prototype/)

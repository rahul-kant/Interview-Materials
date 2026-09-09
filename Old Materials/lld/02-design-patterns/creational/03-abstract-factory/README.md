# Abstract Factory Pattern ⭐⭐

## 📋 Intent

**Provide an interface for creating families of related or dependent objects without specifying their concrete classes.**

---

## 🎯 Problem

- You need to create families of related objects
- Objects must be compatible with each other
- You want to enforce consistency among products
- You don't want client to depend on concrete classes

**Example Scenarios:**
- UI themes (Windows, Mac - each has Button, Checkbox, Window)
- Database drivers (MySQL, PostgreSQL - each has Connection, Command, DataReader)
- Game engines (2D, 3D - each has Renderer, Physics, Audio)
- Document exporters (PDF, HTML - each has Header, Body, Footer)

---

## ✅ Solution

1. Declare abstract product interfaces for each product type
2. Create concrete products for each variant
3. Declare abstract factory interface with creation methods
4. Create concrete factories for each variant

---

## 💻 Implementation

### Basic Structure

```java
// Abstract Products
interface Button {
    void render();
    void onClick();
}

interface Checkbox {
    void render();
    void toggle();
}

// Concrete Products for Windows
class WindowsButton implements Button {
    public void render() {
        System.out.println("Rendering Windows button");
    }
    
    public void onClick() {
        System.out.println("Windows button clicked");
    }
}

class WindowsCheckbox implements Checkbox {
    public void render() {
        System.out.println("Rendering Windows checkbox");
    }
    
    public void toggle() {
        System.out.println("Windows checkbox toggled");
    }
}

// Concrete Products for Mac
class MacButton implements Button {
    public void render() {
        System.out.println("Rendering Mac button");
    }
    
    public void onClick() {
        System.out.println("Mac button clicked");
    }
}

class MacCheckbox implements Checkbox {
    public void render() {
        System.out.println("Rendering Mac checkbox");
    }
    
    public void toggle() {
        System.out.println("Mac checkbox toggled");
    }
}

// Abstract Factory
interface GUIFactory {
    Button createButton();
    Checkbox createCheckbox();
}

// Concrete Factories
class WindowsFactory implements GUIFactory {
    public Button createButton() {
        return new WindowsButton();
    }
    
    public Checkbox createCheckbox() {
        return new WindowsCheckbox();
    }
}

class MacFactory implements GUIFactory {
    public Button createButton() {
        return new MacButton();
    }
    
    public Checkbox createCheckbox() {
        return new MacCheckbox();
    }
}

// Client Code
class Application {
    private Button button;
    private Checkbox checkbox;
    
    public Application(GUIFactory factory) {
        button = factory.createButton();
        checkbox = factory.createCheckbox();
    }
    
    public void render() {
        button.render();
        checkbox.render();
    }
}

// Usage
GUIFactory factory = new WindowsFactory();
Application app = new Application(factory);
app.render();
```

---

## 🎨 Real-World Examples

### Example 1: Database Factory

```java
// Abstract Products
interface Connection {
    void connect();
    void disconnect();
}

interface Command {
    void execute(String sql);
}

interface DataReader {
    void read();
}

// MySQL Products
class MySQLConnection implements Connection {
    public void connect() {
        System.out.println("Connected to MySQL");
    }
    
    public void disconnect() {
        System.out.println("Disconnected from MySQL");
    }
}

class MySQLCommand implements Command {
    public void execute(String sql) {
        System.out.println("MySQL executing: " + sql);
    }
}

class MySQLDataReader implements DataReader {
    public void read() {
        System.out.println("Reading from MySQL");
    }
}

// PostgreSQL Products
class PostgreSQLConnection implements Connection {
    public void connect() {
        System.out.println("Connected to PostgreSQL");
    }
    
    public void disconnect() {
        System.out.println("Disconnected from PostgreSQL");
    }
}

class PostgreSQLCommand implements Command {
    public void execute(String sql) {
        System.out.println("PostgreSQL executing: " + sql);
    }
}

class PostgreSQLDataReader implements DataReader {
    public void read() {
        System.out.println("Reading from PostgreSQL");
    }
}

// Abstract Factory
interface DatabaseFactory {
    Connection createConnection();
    Command createCommand();
    DataReader createDataReader();
}

// Concrete Factories
class MySQLFactory implements DatabaseFactory {
    public Connection createConnection() {
        return new MySQLConnection();
    }
    
    public Command createCommand() {
        return new MySQLCommand();
    }
    
    public DataReader createDataReader() {
        return new MySQLDataReader();
    }
}

class PostgreSQLFactory implements DatabaseFactory {
    public Connection createConnection() {
        return new PostgreSQLConnection();
    }
    
    public Command createCommand() {
        return new PostgreSQLCommand();
    }
    
    public DataReader createDataReader() {
        return new PostgreSQLDataReader();
    }
}

// Client
class DatabaseClient {
    private DatabaseFactory factory;
    
    public DatabaseClient(DatabaseFactory factory) {
        this.factory = factory;
    }
    
    public void performOperation() {
        Connection conn = factory.createConnection();
        Command cmd = factory.createCommand();
        DataReader reader = factory.createDataReader();
        
        conn.connect();
        cmd.execute("SELECT * FROM users");
        reader.read();
        conn.disconnect();
    }
}
```

### Example 2: Document Exporter

```java
// Abstract Products
interface Header {
    void render();
}

interface Body {
    void render();
}

interface Footer {
    void render();
}

// PDF Products
class PDFHeader implements Header {
    public void render() {
        System.out.println("PDF Header");
    }
}

class PDFBody implements Body {
    public void render() {
        System.out.println("PDF Body");
    }
}

class PDFFooter implements Footer {
    public void render() {
        System.out.println("PDF Footer");
    }
}

// HTML Products
class HTMLHeader implements Header {
    public void render() {
        System.out.println("HTML Header");
    }
}

class HTMLBody implements Body {
    public void render() {
        System.out.println("HTML Body");
    }
}

class HTMLFooter implements Footer {
    public void render() {
        System.out.println("HTML Footer");
    }
}

// Abstract Factory
interface DocumentFactory {
    Header createHeader();
    Body createBody();
    Footer createFooter();
}

// Concrete Factories
class PDFFactory implements DocumentFactory {
    public Header createHeader() { return new PDFHeader(); }
    public Body createBody() { return new PDFBody(); }
    public Footer createFooter() { return new PDFFooter(); }
}

class HTMLFactory implements DocumentFactory {
    public Header createHeader() { return new HTMLHeader(); }
    public Body createBody() { return new HTMLBody(); }
    public Footer createFooter() { return new HTMLFooter(); }
}
```

---

## ✅ When to Use

- **Family of related objects** needed
- **Product consistency** must be enforced
- **Hide implementations** from client
- **Support multiple variants** (themes, platforms)
- **Runtime selection** of product families

---

## ❌ When NOT to Use

- Only one product family
- Products are independent
- Simple object creation
- No need for product consistency

---

## 🎯 Pros and Cons

### Pros ✅
- **Product consistency** - ensures compatible products
- **Loose coupling** - client doesn't know concrete classes
- **Open/Closed** - easy to add new families
- **Single Responsibility** - creation isolated
- **Dependency Inversion** - depend on abstractions

### Cons ❌
- **Complexity** - many classes and interfaces
- **Rigidity** - hard to add new product types
- **Overhead** - might be overkill for simple cases

---

## 🔄 Abstract Factory vs Factory Method

| Aspect | Abstract Factory | Factory Method |
|--------|-----------------|----------------|
| **Focus** | Family of products | Single product |
| **Methods** | Multiple creation methods | One factory method |
| **Structure** | Interface composition | Inheritance |
| **Complexity** | More complex | Simpler |
| **Use Case** | Related products | Single product types |

**Example:**
- **Factory Method:** Create Document (PDF or Word)
- **Abstract Factory:** Create UI Theme (Button + Checkbox + Window for Windows or Mac)

---

## 🔄 Abstract Factory vs Builder

| Aspect | Abstract Factory | Builder |
|--------|-----------------|---------|
| **Intent** | Create families | Build complex object |
| **Focus** | Product variety | Product construction |
| **Return** | Different products | Same product type |
| **Steps** | Atomic creation | Step-by-step |

---

## 💡 Design Principles

### Follows:
- ✅ **Dependency Inversion** - Depend on abstractions
- ✅ **Open/Closed** - Add families without modification
- ✅ **Single Responsibility** - Each factory creates one family
- ✅ **Interface Segregation** - Client uses only needed methods

### Pattern Relationships:
- **Factory Method** - Abstract Factory uses multiple Factory Methods
- **Singleton** - Factories often implemented as Singleton
- **Prototype** - Can be alternative to create products

---

## 🎓 Interview Focus

### Must Know:
1. **Intent** - Create families of related objects
2. **Structure** - Multiple product types + Abstract factory
3. **vs Factory Method** - Family vs single product
4. **Product consistency** - Key benefit
5. **When to use** - Multiple variants, consistency needed

### Common Questions:
1. "Implement a UI theme factory"
2. "Abstract Factory vs Factory Method?"
3. "When would you use this?"
4. "How to add new product type?"
5. "Real-world examples?"

---

## 📝 Common Pitfalls

### 1. Adding New Product Type
**Problem:** Hard to extend with new product types

```java
// If you want to add Window:
interface Window {
    void render();
}

// Must modify ALL factories!
interface GUIFactory {
    Button createButton();
    Checkbox createCheckbox();
    Window createWindow(); // New method
}
```

**Solution:** Plan product types carefully upfront

### 2. Factory Explosion
**Problem:** Too many factories

**Solution:** 
- Use configuration
- Combine with other patterns
- Keep number of variants reasonable

---

## 📝 Interview Questions & Answers

See [INTERVIEW_QA.md](INTERVIEW_QA.md) for detailed Q&A

---

## 🔗 Related Patterns

- **Factory Method** - Used within Abstract Factory
- **Singleton** - Factories often Singleton
- **Prototype** - Alternative creation approach
- **Builder** - Different creational approach

---

## 📚 Where It's Used

### In Java:
- `javax.xml.parsers.DocumentBuilderFactory`
- `javax.xml.transform.TransformerFactory`
- JDBC `DriverManager` (family of database objects)

### In Frameworks:
- Spring `BeanFactory` hierarchy
- Hibernate `SessionFactory`
- GUI frameworks (Swing, JavaFX)

---

## 🎯 Key Takeaways

1. **Creates families** of related objects
2. **Ensures consistency** among products
3. **Loose coupling** from concrete classes
4. **More complex** than Factory Method
5. **Best for** multiple product variants

---

**Complete Example:** [GUIFactory.java](GUIFactory.java)  
**Demo:** [AbstractFactoryDemo.java](AbstractFactoryDemo.java)  
**Interview Q&A:** [INTERVIEW_QA.md](INTERVIEW_QA.md)

---

**Next Pattern:** [Builder →](../04-builder/)

# Factory Method Pattern ⭐⭐⭐

## 📋 Intent

**Define an interface for creating an object, but let subclasses decide which class to instantiate. Factory Method lets a class defer instantiation to subclasses.**

---

## 🎯 Problem

- You need to create objects but don't know the exact class until runtime
- Object creation logic is complex
- You want to delegate object creation to subclasses
- You want to follow Open/Closed Principle

**Example Scenarios:**
- Document creation (PDF, Word, Excel)
- Database connections (MySQL, PostgreSQL, Oracle)
- UI components (Windows, Mac, Linux)
- Logistics (Truck, Ship transport)

---

## ✅ Solution

1. Create an interface for the product
2. Create concrete product classes
3. Create a creator interface with factory method
4. Create concrete creators that implement factory method

---

## 💻 Implementation

### Basic Structure

```java
// Product Interface
interface Document {
    void open();
    void save();
    void close();
}

// Concrete Products
class PDFDocument implements Document {
    public void open() {
        System.out.println("Opening PDF document");
    }
    
    public void save() {
        System.out.println("Saving PDF document");
    }
    
    public void close() {
        System.out.println("Closing PDF document");
    }
}

class WordDocument implements Document {
    public void open() {
        System.out.println("Opening Word document");
    }
    
    public void save() {
        System.out.println("Saving Word document");
    }
    
    public void close() {
        System.out.println("Closing Word document");
    }
}

// Creator (Factory)
abstract class DocumentFactory {
    // Factory Method
    public abstract Document createDocument();
    
    // Template method using factory method
    public void openDocument() {
        Document doc = createDocument();
        doc.open();
    }
}

// Concrete Creators
class PDFFactory extends DocumentFactory {
    @Override
    public Document createDocument() {
        return new PDFDocument();
    }
}

class WordFactory extends DocumentFactory {
    @Override
    public Document createDocument() {
        return new WordDocument();
    }
}

// Usage
DocumentFactory factory = new PDFFactory();
Document doc = factory.createDocument();
doc.open();
```

---

## 🎨 Real-World Examples

### Example 1: Logistics System

```java
// Product
interface Transport {
    void deliver();
}

class Truck implements Transport {
    public void deliver() {
        System.out.println("Delivery by land in a truck");
    }
}

class Ship implements Transport {
    public void deliver() {
        System.out.println("Delivery by sea in a ship");
    }
}

// Creator
abstract class Logistics {
    public abstract Transport createTransport();
    
    public void planDelivery() {
        Transport transport = createTransport();
        transport.deliver();
    }
}

class RoadLogistics extends Logistics {
    public Transport createTransport() {
        return new Truck();
    }
}

class SeaLogistics extends Logistics {
    public Transport createTransport() {
        return new Ship();
    }
}
```

### Example 2: Database Connection

```java
interface DatabaseConnection {
    void connect();
    void executeQuery(String query);
    void disconnect();
}

class MySQLConnection implements DatabaseConnection {
    public void connect() {
        System.out.println("Connecting to MySQL");
    }
    
    public void executeQuery(String query) {
        System.out.println("Executing MySQL query: " + query);
    }
    
    public void disconnect() {
        System.out.println("Disconnecting from MySQL");
    }
}

class PostgreSQLConnection implements DatabaseConnection {
    public void connect() {
        System.out.println("Connecting to PostgreSQL");
    }
    
    public void executeQuery(String query) {
        System.out.println("Executing PostgreSQL query: " + query);
    }
    
    public void disconnect() {
        System.out.println("Disconnecting from PostgreSQL");
    }
}

abstract class DatabaseFactory {
    public abstract DatabaseConnection createConnection();
    
    public void executeOperation(String query) {
        DatabaseConnection conn = createConnection();
        conn.connect();
        conn.executeQuery(query);
        conn.disconnect();
    }
}

class MySQLFactory extends DatabaseFactory {
    public DatabaseConnection createConnection() {
        return new MySQLConnection();
    }
}

class PostgreSQLFactory extends DatabaseFactory {
    public DatabaseConnection createConnection() {
        return new PostgreSQLConnection();
    }
}
```

---

## ✅ When to Use

- **Don't know exact types** beforehand
- **Delegate creation** to subclasses
- **Complex creation logic** needs encapsulation
- **Open/Closed Principle** - add new types without modifying code
- **Single place for creation** logic

---

## ❌ When NOT to Use

- Simple object creation (use `new`)
- Only one product type
- No variation in creation logic
- Overkill for simple scenarios

---

## 🎯 Pros and Cons

### Pros ✅
- **Single Responsibility** - creation logic in one place
- **Open/Closed** - easy to add new products
- **Loose coupling** - code doesn't depend on concrete classes
- **Flexibility** - subclasses control creation

### Cons ❌
- **More classes** - can become complex
- **Indirection** - extra layer of abstraction

---

## 🔄 Factory Method vs Simple Factory

| Aspect | Factory Method | Simple Factory |
|--------|---------------|----------------|
| **Structure** | Uses inheritance | Uses composition |
| **Extensibility** | Add subclass | Modify factory class |
| **Flexibility** | More flexible | Less flexible |
| **Complexity** | More complex | Simpler |

**Simple Factory (Not a pattern, but useful):**
```java
class SimpleDocumentFactory {
    public static Document createDocument(String type) {
        switch(type) {
            case "PDF": return new PDFDocument();
            case "Word": return new WordDocument();
            default: throw new IllegalArgumentException();
        }
    }
}
```

---

## 🔄 Factory Method vs Abstract Factory

| Aspect | Factory Method | Abstract Factory |
|--------|---------------|------------------|
| **Focus** | One product | Family of products |
| **Methods** | One factory method | Multiple factory methods |
| **Use Case** | Create single type | Create related types |
| **Example** | Document creator | UI theme (button+checkbox+window) |

---

## 💡 Design Principles

### Follows:
- ✅ **Dependency Inversion** - Depend on abstractions
- ✅ **Open/Closed** - Open for extension
- ✅ **Single Responsibility** - Creation isolated

### Pattern:
- **Template Method** often uses Factory Method
- **Prototype** can be alternative
- **Abstract Factory** is composition of Factory Methods

---

## 🎓 Interview Focus

### Must Know:
1. **Intent** - Defer instantiation to subclasses
2. **Structure** - Product + Creator hierarchy
3. **vs Simple Factory** - Key differences
4. **vs Abstract Factory** - Single vs family
5. **When to use** - Complex creation, extensibility

### Common Questions:
1. "Implement a document factory"
2. "Factory Method vs Abstract Factory?"
3. "When would you use this?"
4. "How does it follow Open/Closed?"
5. "Real-world examples?"

---

## 📝 Interview Questions & Answers

See [INTERVIEW_QA.md](INTERVIEW_QA.md) for detailed Q&A

---

## 🔗 Related Patterns

- **Abstract Factory** - Uses Factory Method
- **Template Method** - Often uses Factory Method
- **Prototype** - Alternative approach
- **Singleton** - Factory often singleton

---

## 📚 Where It's Used

### In Java:
- `java.util.Calendar.getInstance()`
- `java.text.NumberFormat.getInstance()`
- `java.nio.charset.Charset.forName()`
- JDBC `DriverManager.getConnection()`

### In Frameworks:
- Spring's `BeanFactory`
- Hibernate's `SessionFactory`
- JUnit's `@TestFactory`

---

## 🎯 Key Takeaways

1. **Defers instantiation** to subclasses
2. **Template Method** pattern in creation
3. **Open/Closed Principle** in action
4. **Loose coupling** achieved
5. **Single product type** focus

---

**Complete Example:** [DocumentFactory.java](DocumentFactory.java)  
**Demo:** [FactoryDemo.java](FactoryDemo.java)  
**Interview Q&A:** [INTERVIEW_QA.md](INTERVIEW_QA.md)

---

**Next Pattern:** [Abstract Factory →](../03-abstract-factory/)

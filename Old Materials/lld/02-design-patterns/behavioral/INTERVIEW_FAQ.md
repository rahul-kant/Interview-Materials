# Behavioral Patterns - FAANG Interview FAQ

## 📚 Overview

**Behavioral patterns** are concerned with algorithms and the assignment of responsibilities between objects. They describe not just patterns of objects or classes but also the patterns of communication between them.

### The 11 Behavioral Patterns:
1. **Strategy** ⭐⭐⭐ - Family of algorithms
2. **Observer** ⭐⭐⭐ - Pub-Sub notification
3. **Command** ⭐⭐ - Encapsulate requests
4. **State** ⭐⭐ - Change behavior based on state
5. **Template Method** ⭐⭐ - Define algorithm skeleton
6. **Iterator** ⭐ - Sequential access
7. **Chain of Responsibility** ⭐ - Pass request along chain
8. **Mediator** ⭐ - Centralized communication
9. **Memento** ⭐ - Save/restore state
10. **Visitor** ⭐ - Add operations without changing classes
11. **Interpreter** ⭐ - Language grammar

---

## 🎯 Most Frequently Asked Questions (FAANG)

### Q1: Strategy vs State - What's the difference?

**Answer:**

Both have similar structure but **different intent**:

| Aspect | Strategy | State |
|--------|----------|-------|
| **Intent** | Choose algorithm | Change behavior based on state |
| **Who decides** | Client explicitly | Object automatically |
| **Awareness** | Client knows strategies | Client unaware of states |
| **Switching** | Client switches | State transitions internally |
| **Example** | Payment methods | Order workflow |
| **States aware** | Strategies independent | States know each other |

**Strategy Example:**
```java
// Client explicitly chooses strategy
PaymentStrategy strategy = new CreditCardStrategy();
cart.setPaymentStrategy(strategy);
cart.checkout();

// Client can switch
cart.setPaymentStrategy(new PayPalStrategy());
cart.checkout();
```

**State Example:**
```java
// Object manages its own state transitions
Order order = new Order(); // Starts in PendingState
order.pay();   // Automatically transitions to PaidState
order.ship();  // Automatically transitions to ShippedState
// Client doesn't know or control state transitions
```

**Key Difference:** 
- Strategy: Client chooses (Composition)
- State: Object changes its own behavior (Self-management)

---

### Q2: Explain Observer pattern with real example

**Answer:**

Observer defines **one-to-many dependency**. When one object changes state, all dependents are notified.

**Real-World Example - Stock Market:**
```java
interface StockObserver {
    void update(String stock, double price);
}

class StockExchange {
    private Map<String, List<StockObserver>> observers = new HashMap<>();
    
    public void subscribe(String stock, StockObserver observer) {
        observers.computeIfAbsent(stock, k -> new ArrayList<>()).add(observer);
    }
    
    public void unsubscribe(String stock, StockObserver observer) {
        List<StockObserver> stockObservers = observers.get(stock);
        if (stockObservers != null) {
            stockObservers.remove(observer);
        }
    }
    
    public void updatePrice(String stock, double price) {
        List<StockObserver> stockObservers = observers.get(stock);
        if (stockObservers != null) {
            for (StockObserver observer : stockObservers) {
                observer.update(stock, price);
            }
        }
    }
}

class Investor implements StockObserver {
    private String name;
    
    public Investor(String name) {
        this.name = name;
    }
    
    public void update(String stock, double price) {
        System.out.println(name + ": " + stock + " price is now $" + price);
        // Can decide to buy/sell based on price
    }
}

// Usage
StockExchange exchange = new StockExchange();
Investor john = new Investor("John");
Investor jane = new Investor("Jane");

exchange.subscribe("AAPL", john);
exchange.subscribe("AAPL", jane);

exchange.updatePrice("AAPL", 150.0);
// Both John and Jane get notified
```

**Push vs Pull Model:**

**Push Model** (shown above):
- Subject pushes data to observers
- Simple but less flexible

**Pull Model** (preferred):
```java
interface Observer {
    void update(Subject subject);
}

class Investor implements Observer {
    public void update(Subject subject) {
        // Observer pulls only what it needs
        double price = ((StockExchange)subject).getPrice("AAPL");
    }
}
```

**Used In:**
- MVC architecture (Model notifies Views)
- Event systems
- RxJava, React hooks
- Java Swing listeners

---

### Q3: What is Command pattern and when to use it?

**Answer:**

Command **encapsulates a request as an object**, enabling parameterization, queuing, logging, and undo.

**Complete Example - Text Editor:**
```java
// Command Interface
interface Command {
    void execute();
    void undo();
}

// Receiver
class TextEditor {
    private StringBuilder text = new StringBuilder();
    
    public void write(String text) {
        this.text.append(text);
    }
    
    public void delete(int length) {
        int start = text.length() - length;
        if (start >= 0) {
            text.delete(start, text.length());
        }
    }
    
    public String getContent() {
        return text.toString();
    }
}

// Concrete Commands
class WriteCommand implements Command {
    private TextEditor editor;
    private String text;
    
    public WriteCommand(TextEditor editor, String text) {
        this.editor = editor;
        this.text = text;
    }
    
    public void execute() {
        editor.write(text);
    }
    
    public void undo() {
        editor.delete(text.length());
    }
}

// Invoker
class EditorInvoker {
    private Stack<Command> history = new Stack<>();
    
    public void executeCommand(Command command) {
        command.execute();
        history.push(command);
    }
    
    public void undo() {
        if (!history.isEmpty()) {
            Command command = history.pop();
            command.undo();
        }
    }
}

// Usage
TextEditor editor = new TextEditor();
EditorInvoker invoker = new EditorInvoker();

invoker.executeCommand(new WriteCommand(editor, "Hello "));
invoker.executeCommand(new WriteCommand(editor, "World"));
System.out.println(editor.getContent()); // "Hello World"

invoker.undo();
System.out.println(editor.getContent()); // "Hello "
```

**When to Use:**
- Undo/Redo functionality
- Transaction management
- Request queuing
- Macro recording
- Logging operations

---

### Q4: Template Method vs Strategy?

**Answer:**

Both define algorithms but with different approaches:

| Aspect | Template Method | Strategy |
|--------|----------------|----------|
| **Mechanism** | Inheritance | Composition |
| **Flexibility** | Compile-time | Runtime |
| **Coupling** | Tight (subclass) | Loose (interface) |
| **Algorithm** | Same skeleton, vary steps | Completely different |
| **Change** | Override methods | Switch objects |

**Template Method Example:**
```java
abstract class DataMiner {
    // Template method - defines skeleton
    public final void mine(String path) {
        openFile(path);
        extractData();
        parseData();
        analyzeData();
        closeFile();
    }
    
    abstract void extractData();
    abstract void parseData();
    
    void openFile(String path) {
        System.out.println("Opening: " + path);
    }
    
    void analyzeData() {
        System.out.println("Analyzing data");
    }
    
    void closeFile() {
        System.out.println("Closing file");
    }
}

class PDFDataMiner extends DataMiner {
    void extractData() {
        System.out.println("Extracting PDF data");
    }
    
    void parseData() {
        System.out.println("Parsing PDF data");
    }
}

class CSVDataMiner extends DataMiner {
    void extractData() {
        System.out.println("Extracting CSV data");
    }
    
    void parseData() {
        System.out.println("Parsing CSV data");
    }
}
```

**Strategy Example:**
```java
interface DataExtractionStrategy {
    void extract(String path);
}

class PDFExtraction implements DataExtractionStrategy {
    public void extract(String path) {
        System.out.println("Extracting PDF");
    }
}

class DataProcessor {
    private DataExtractionStrategy strategy;
    
    public void setStrategy(DataExtractionStrategy strategy) {
        this.strategy = strategy;
    }
    
    public void process(String path) {
        strategy.extract(path);
    }
}
```

**Use Template Method when:**
- Common algorithm with varying steps
- Control over algorithm structure needed
- Inheritance is acceptable

**Use Strategy when:**
- Multiple interchangeable algorithms
- Runtime selection needed
- Prefer composition over inheritance

---

### Q5: Observer vs Mediator - What's the difference?

**Answer:**

| Aspect | Observer | Mediator |
|--------|----------|----------|
| **Communication** | One-to-many broadcast | Many-to-many through mediator |
| **Coupling** | Subject knows observers | Components don't know each other |
| **Direction** | One-way notification | Two-way communication |
| **Example** | Newsletter subscription | Chat room |

**Observer:** Subject broadcasts to all observers
**Mediator:** Components communicate through central mediator

---

## 🎓 GoF Principles for Behavioral Patterns

### From Gang of Four Book:

**1. Overall Principle**
> "Behavioral patterns are concerned with algorithms and the assignment of responsibilities between objects. Behavioral patterns describe not just patterns of objects or classes but also the patterns of communication between them."

**2. Strategy Principle**
> "Define a family of algorithms, encapsulate each one, and make them interchangeable. Strategy lets the algorithm vary independently from clients that use it."

**3. Observer Principle**
> "Define a one-to-many dependency between objects so that when one object changes state, all its dependents are notified and updated automatically."

**4. Command Principle**
> "Encapsulate a request as an object, thereby letting you parameterize clients with different requests, queue or log requests, and support undoable operations."

**5. State Principle**
> "Allow an object to alter its behavior when its internal state changes. The object will appear to change its class."

**6. Template Method Principle**
> "Define the skeleton of an algorithm in an operation, deferring some steps to subclasses. Template Method lets subclasses redefine certain steps of an algorithm without changing the algorithm's structure."

**7. Iterator Principle**
> "Provide a way to access the elements of an aggregate object sequentially without exposing its underlying representation."

**8. Chain of Responsibility Principle**
> "Avoid coupling the sender of a request to its receiver by giving more than one object a chance to handle the request."

**9. Mediator Principle**
> "Define an object that encapsulates how a set of objects interact. Mediator promotes loose coupling by keeping objects from referring to each other explicitly."

**10. Memento Principle**
> "Without violating encapsulation, capture and externalize an object's internal state so that the object can be restored to this state later."

**11. Visitor Principle**
> "Represent an operation to be performed on the elements of an object structure. Visitor lets you define a new operation without changing the classes of the elements on which it operates."

**12. Interpreter Principle**
> "Given a language, define a representation for its grammar along with an interpreter that uses the representation to interpret sentences in the language."

---

## 🔥 Common Interview Scenarios

### Scenario 1: "Design an undo/redo system for a text editor"

**Answer:** Use **Command + Memento**

```java
// Command for undo/redo
interface TextCommand {
    void execute();
    void undo();
}

class TypeCommand implements TextCommand {
    private TextEditor editor;
    private String text;
    private int position;
    
    public TypeCommand(TextEditor editor, String text) {
        this.editor = editor;
        this.text = text;
        this.position = editor.getCursorPosition();
    }
    
    public void execute() {
        editor.insertAt(position, text);
    }
    
    public void undo() {
        editor.deleteAt(position, text.length());
    }
}

class CommandManager {
    private Stack<TextCommand> undoStack = new Stack<>();
    private Stack<TextCommand> redoStack = new Stack<>();
    
    public void execute(TextCommand command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear(); // Clear redo on new action
    }
    
    public void undo() {
        if (!undoStack.isEmpty()) {
            TextCommand command = undoStack.pop();
            command.undo();
            redoStack.push(command);
        }
    }
    
    public void redo() {
        if (!redoStack.isEmpty()) {
            TextCommand command = redoStack.pop();
            command.execute();
            undoStack.push(command);
        }
    }
}
```

---

### Scenario 2: "Implement a notification system"

**Answer:** Use **Observer**

```java
interface NotificationObserver {
    void update(String message, String priority);
}

class NotificationService {
    private Map<String, List<NotificationObserver>> observers = new HashMap<>();
    
    public void subscribe(String topic, NotificationObserver observer) {
        observers.computeIfAbsent(topic, k -> new ArrayList<>()).add(observer);
    }
    
    public void notify(String topic, String message, String priority) {
        List<NotificationObserver> topicObservers = observers.get(topic);
        if (topicObservers != null) {
            for (NotificationObserver observer : topicObservers) {
                observer.update(message, priority);
            }
        }
    }
}

class EmailNotifier implements NotificationObserver {
    public void update(String message, String priority) {
        if (priority.equals("HIGH")) {
            System.out.println("Sending email: " + message);
        }
    }
}

class SMSNotifier implements NotificationObserver {
    public void update(String message, String priority) {
        if (priority.equals("CRITICAL")) {
            System.out.println("Sending SMS: " + message);
        }
    }
}
```

---

### Scenario 3: "Design a state machine for order processing"

**Answer:** Use **State**

```java
interface OrderState {
    void next(Order order);
    void previous(Order order);
    void printStatus();
}

class PendingState implements OrderState {
    public void next(Order order) {
        order.setState(new ProcessingState());
    }
    
    public void previous(Order order) {
        System.out.println("Already at first state");
    }
    
    public void printStatus() {
        System.out.println("Order is pending");
    }
}

class ProcessingState implements OrderState {
    public void next(Order order) {
        order.setState(new ShippedState());
    }
    
    public void previous(Order order) {
        order.setState(new PendingState());
    }
    
    public void printStatus() {
        System.out.println("Order is being processed");
    }
}

class Order {
    private OrderState state;
    
    public Order() {
        state = new PendingState();
    }
    
    public void setState(OrderState state) {
        this.state = state;
    }
    
    public void nextState() {
        state.next(this);
    }
    
    public void previousState() {
        state.previous(this);
    }
    
    public void printStatus() {
        state.printStatus();
    }
}
```

---

## 💡 Pro Tips for Interviews

### 1. Pattern Recognition
Know how to identify patterns in requirements:
- "Undo/redo" → **Command**
- "Notify multiple objects" → **Observer**
- "Behavior changes with state" → **State**
- "Multiple algorithms" → **Strategy**
- "Algorithm skeleton" → **Template Method**

### 2. Common Combinations
Patterns often work together:
- **Command + Memento** = Undo/Redo
- **Observer + Mediator** = Event system
- **Strategy + Factory** = Algorithm selection
- **State + Strategy** = Complex state behavior

### 3. Real-World Examples
Always mention real-world usage:
- **Observer:** MVC, Event systems, RxJava
- **Strategy:** Collections.sort(Comparator)
- **Command:** Swing Actions, Transaction systems
- **Iterator:** Java Collections, for-each loop
- **Template Method:** Servlet HttpServlet

### 4. Common Mistakes
- ❌ Confusing Strategy with State
- ❌ Not understanding Observer memory leaks
- ❌ Missing Command undo implementation
- ❌ Overusing patterns when simple if-else suffices

---

## 📊 Comparison Matrix

| Pattern | Complexity | Coupling | Flexibility | Interview Frequency |
|---------|-----------|----------|-------------|---------------------|
| **Strategy** | Low | Loose | High | ⭐⭐⭐ Very High |
| **Observer** | Medium | Loose | High | ⭐⭐⭐ Very High |
| **Command** | Medium | Loose | High | ⭐⭐ High |
| **State** | Medium | Medium | High | ⭐⭐ High |
| **Template Method** | Low | Tight | Medium | ⭐⭐ High |
| **Iterator** | Low | Loose | Medium | ⭐ Medium |
| **Chain of Resp.** | Medium | Loose | High | ⭐ Medium |
| **Mediator** | High | Loose | Medium | ⭐ Medium |
| **Memento** | Low | Loose | Low | ⭐ Low |
| **Visitor** | High | Tight | Low | ⭐ Low |
| **Interpreter** | High | Medium | Low | ⭐ Low |

---

## 🎯 Quick Reference

### Most Common Interview Questions:
1. ✅ Strategy vs State
2. ✅ Observer implementation
3. ✅ Command for undo/redo
4. ✅ Template Method vs Strategy
5. ✅ Iterator pattern basics
6. ✅ Observer memory leaks
7. ✅ State machine design
8. ✅ Chain of Responsibility use cases

### Must Know for FAANG:
- **Google:** Strategy, Observer, Command
- **Amazon:** State, Command, Observer
- **Microsoft:** Observer, Strategy, Template Method
- **Meta:** Observer, Command, Strategy

---

**Previous:** [Structural Patterns FAQ ←](../structural/INTERVIEW_FAQ.md)  
**Next:** [Main Design Patterns README →](../README.md)

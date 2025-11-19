# Interface Segregation Principle (ISP)

## 📖 Definition

> "Clients should not be forced to depend on interfaces they do not use."
> — Robert C. Martin

In simpler terms: **Create specific, focused interfaces rather than large, general-purpose ones.**

## 🎯 What Does This Mean?

- Split large interfaces into smaller, specific ones
- Classes should only implement methods they actually need
- Don't force clients to depend on methods they don't use
- Many client-specific interfaces are better than one general-purpose interface

## ❌ Why Violating ISP is Bad

### Problems:

1. **Unnecessary Dependencies**
   - Classes depend on methods they don't need
   - Changes in unused methods force recompilation
   - Increased coupling

2. **Forced Implementation**
   - Must implement methods that don't apply
   - Often leads to empty implementations or exceptions
   - Violates LSP

3. **Hard to Understand**
   - Large interfaces are confusing
   - Unclear what a class actually needs
   - Difficult to maintain

4. **Difficult Testing**
   - Mock large interfaces for testing
   - Test unnecessary methods
   - Complex test setup

## 💡 Real-World Analogy

Think of a restaurant menu:

**Bad (Violates ISP):**
```
One Universal Menu Interface:
- orderBreakfast()
- orderLunch()
- orderDinner()
- orderDessert()
- orderDrinks()

Problem: Breakfast customers forced to see dinner options!
Lunch-only restaurant must implement breakfast methods!
```

**Good (Follows ISP):**
```
Separate Menus:
- BreakfastMenu
- LunchMenu
- DinnerMenu
- DessertMenu
- DrinksMenu

Solution: Each customer/restaurant uses only what they need!
```

## 📝 Classic Example: Worker Interface

### Bad Example (Violates ISP):

```java
// Fat interface - not all workers can do all tasks
interface Worker {
    void work();
    void eat();
    void sleep();
    void getPaid();
}

// Human worker - can do everything
class HumanWorker implements Worker {
    public void work() { /* works */ }
    public void eat() { /* eats */ }
    public void sleep() { /* sleeps */ }
    public void getPaid() { /* gets paid */ }
}

// Robot worker - forced to implement eat() and sleep()!
class RobotWorker implements Worker {
    public void work() { /* works */ }
    public void eat() { /* DOESN'T EAT! */ }
    public void sleep() { /* DOESN'T SLEEP! */ }
    public void getPaid() { /* DOESN'T GET PAID! */ }
}
```

**Problems:**
- RobotWorker forced to implement eat(), sleep(), getPaid()
- Often throws UnsupportedOperationException
- Violates LSP
- Clients depend on methods they don't use

### Good Example (Follows ISP):

```java
// Split into specific interfaces
interface Workable {
    void work();
}

interface Eatable {
    void eat();
}

interface Sleepable {
    void sleep();
}

interface Payable {
    void getPaid();
}

// Human implements all relevant interfaces
class HumanWorker implements Workable, Eatable, Sleepable, Payable {
    public void work() { /* works */ }
    public void eat() { /* eats */ }
    public void sleep() { /* sleeps */ }
    public void getPaid() { /* gets paid */ }
}

// Robot implements only relevant interfaces
class RobotWorker implements Workable {
    public void work() { /* works */ }
    // No need to implement eat(), sleep(), getPaid()
}
```

**Benefits:**
- Each class implements only what it needs
- No empty or exception-throwing methods
- Clear, focused interfaces
- Better separation of concerns

## 🔍 How to Identify ISP Violations

### Warning Signs:

1. **Empty Method Implementations**
   ```java
   @Override
   public void someMethod() {
       // Empty - class doesn't need this
   }
   ```

2. **Exception-Throwing Implementations**
   ```java
   @Override
   public void someMethod() {
       throw new UnsupportedOperationException();
   }
   ```

3. **Large Interfaces**
   - More than 5-7 methods
   - Methods serve different purposes
   - Not all implementations use all methods

4. **Interface Names with "And"**
   - `ReaderAndWriter` ❌
   - Better: `Reader`, `Writer` ✅

## ✅ How to Apply ISP

### Design Guidelines:

1. **Start with Small Interfaces**
   - Define minimal, focused interfaces
   - Each interface has single purpose
   - Combine interfaces when needed

2. **Role-Based Interfaces**
   - Interface per role/capability
   - `Drawable`, `Clickable`, `Serializable`
   - Classes implement relevant roles

3. **Compose Interfaces**
   - Create larger interfaces from smaller ones
   - Use interface inheritance
   - Flexibility in implementation

4. **Client-Specific Interfaces**
   - Design interfaces for clients' needs
   - Don't add methods "just in case"
   - YAGNI principle (You Aren't Gonna Need It)

## 📊 Before and After Comparison

### Before (Bad - Violates ISP):
```
[Large Interface]
Worker
- work()
- eat()
- sleep()
- getPaid()
- takeBreak()
- attendMeeting()

↓ implements

HumanWorker (needs all)
RobotWorker (needs only work)
ContractWorker (needs work, getPaid)
InternWorker (needs work, eat, sleep - no pay!)

Problem: Everyone implements everything! ❌
```

### After (Good - Follows ISP):
```
[Small, Focused Interfaces]
Workable        Eatable         Sleepable       Payable
- work()        - eat()         - sleep()       - getPaid()

↓ implements selectively

HumanWorker: Workable, Eatable, Sleepable, Payable
RobotWorker: Workable
ContractWorker: Workable, Payable
InternWorker: Workable, Eatable, Sleepable

Solution: Each implements only what it needs! ✅
```

## 🎓 Practice Exercise

Identify ISP violation:

```java
interface MultifunctionPrinter {
    void print(Document doc);
    void scan(Document doc);
    void fax(Document doc);
    void photocopy(Document doc);
}

class SimplePrinter implements MultifunctionPrinter {
    public void print(Document doc) { /* prints */ }
    public void scan(Document doc) { throw new UnsupportedOperationException(); }
    public void fax(Document doc) { throw new UnsupportedOperationException(); }
    public void photocopy(Document doc) { throw new UnsupportedOperationException(); }
}
```

### Problems:
1. SimplePrinter only needs print()
2. Forced to implement 3 unused methods
3. Throws exceptions - bad design

### Solution:
```java
interface Printer {
    void print(Document doc);
}

interface Scanner {
    void scan(Document doc);
}

interface Fax {
    void fax(Document doc);
}

interface Photocopier {
    void photocopy(Document doc);
}

// Simple printer
class SimplePrinter implements Printer {
    public void print(Document doc) { /* prints */ }
}

// All-in-one printer
class MultifunctionPrinter implements Printer, Scanner, Fax, Photocopier {
    public void print(Document doc) { /* prints */ }
    public void scan(Document doc) { /* scans */ }
    public void fax(Document doc) { /* faxes */ }
    public void photocopy(Document doc) { /* photocopies */ }
}
```

## 🔑 Key Takeaways

✅ **Small, focused interfaces** - Single purpose
✅ **Client-specific** - Design for actual needs
✅ **No forced implementations** - Implement only what's needed
✅ **Better flexibility** - Mix and match interfaces
✅ **Easier testing** - Mock smaller interfaces
✅ **Clear contracts** - Obvious what class can do

## 💡 Interview Tips

### Common Questions:

**Q: What is Interface Segregation Principle?**
A: Clients should not be forced to depend on interfaces they don't use. Create specific, focused interfaces rather than large, general-purpose ones.

**Q: How is ISP different from SRP?**
A: SRP is about classes having one responsibility. ISP is about interfaces being specific to client needs. Both promote focused design but at different levels.

**Q: Give a real-world example of ISP.**
A: Printer interfaces - not all printers can scan/fax. Instead of one MultifunctionPrinter interface, have separate Printer, Scanner, Fax interfaces. Simple printers implement only Printer.

**Q: What are the signs of ISP violation?**
A: Empty method implementations, methods throwing UnsupportedOperationException, large interfaces with many unrelated methods, classes implementing interfaces but not using all methods.

**Q: How do you refactor to follow ISP?**
A: Split large interfaces into smaller, focused ones based on client needs. Use interface inheritance to compose larger interfaces. Each implementation chooses relevant interfaces.

## 🎯 ISP vs Other SOLID Principles

### ISP + SRP:
- SRP: Class has one responsibility
- ISP: Interface has one purpose
- Work together for focused design

### ISP + LSP:
- ISP prevents forced implementations
- Forced implementations often violate LSP
- ISP makes LSP easier to follow

### ISP + DIP:
- ISP creates focused abstractions
- DIP depends on these abstractions
- ISP makes DIP more effective

## 📁 Code Examples

- `bad-example/` - Shows ISP violations (fat interfaces)
- `good-example/` - Shows proper design (role interfaces)

Study both to understand the difference!

## 🌟 Best Practices

### Do:
✅ Keep interfaces small and focused
✅ Design interfaces for client needs
✅ Use interface composition
✅ Follow YAGNI principle
✅ Create role-based interfaces

### Don't:
❌ Create god interfaces
❌ Force unnecessary method implementations
❌ Add methods "just in case"
❌ Mix unrelated operations
❌ Ignore client requirements

## ➡️ Next Steps

1. Review the bad example to see violations
2. Study the good example to see the solution
3. Compare the approaches
4. Try refactoring fat interfaces
5. Move on to Dependency Inversion Principle

---

**Remember**: ISP promotes focused, client-specific interfaces that don't burden implementations with unnecessary methods!

# Decorator Pattern ⭐⭐⭐

## 📋 Intent

**Attach additional responsibilities to an object dynamically. Decorators provide a flexible alternative to subclassing for extending functionality.**

---

## 🎯 Problem

- Need to add responsibilities to individual objects, not entire class
- Want to add functionality without modifying existing code
- Subclassing becomes unwieldy with many combinations
- Need runtime flexibility in adding features

**Example Scenarios:**
- Coffee shop (coffee + milk + sugar + whipped cream)
- Text formatting (bold + italic + underline)
- I/O streams (buffering + compression + encryption)
- UI components (borders + scrollbars)

---

## ✅ Solution

1. Create component interface
2. Create concrete component
3. Create base decorator implementing component
4. Create concrete decorators extending base decorator
5. Wrap objects at runtime

---

## 💻 Implementation

### Basic Structure

```java
// Component Interface
interface Coffee {
    String getDescription();
    double getCost();
}

// Concrete Component
class SimpleCoffee implements Coffee {
    public String getDescription() {
        return "Simple Coffee";
    }
    
    public double getCost() {
        return 2.0;
    }
}

// Base Decorator
abstract class CoffeeDecorator implements Coffee {
    protected Coffee coffee;
    
    public CoffeeDecorator(Coffee coffee) {
        this.coffee = coffee;
    }
    
    public String getDescription() {
        return coffee.getDescription();
    }
    
    public double getCost() {
        return coffee.getCost();
    }
}

// Concrete Decorators
class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }
    
    public String getDescription() {
        return coffee.getDescription() + ", Milk";
    }
    
    public double getCost() {
        return coffee.getCost() + 0.5;
    }
}

class SugarDecorator extends CoffeeDecorator {
    public SugarDecorator(Coffee coffee) {
        super(coffee);
    }
    
    public String getDescription() {
        return coffee.getDescription() + ", Sugar";
    }
    
    public double getCost() {
        return coffee.getCost() + 0.3;
    }
}

// Usage
Coffee coffee = new SimpleCoffee();
coffee = new MilkDecorator(coffee);
coffee = new SugarDecorator(coffee);
System.out.println(coffee.getDescription() + " = $" + coffee.getCost());
// Output: Simple Coffee, Milk, Sugar = $2.8
```

---

## 🎨 Real-World Examples

### Example 1: Text Formatting

```java
interface Text {
    String render();
}

class PlainText implements Text {
    private String text;
    
    public PlainText(String text) {
        this.text = text;
    }
    
    public String render() {
        return text;
    }
}

abstract class TextDecorator implements Text {
    protected Text text;
    
    public TextDecorator(Text text) {
        this.text = text;
    }
}

class BoldDecorator extends TextDecorator {
    public BoldDecorator(Text text) {
        super(text);
    }
    
    public String render() {
        return "<b>" + text.render() + "</b>";
    }
}

class ItalicDecorator extends TextDecorator {
    public ItalicDecorator(Text text) {
        super(text);
    }
    
    public String render() {
        return "<i>" + text.render() + "</i>";
    }
}

// Usage
Text text = new PlainText("Hello");
text = new BoldDecorator(text);
text = new ItalicDecorator(text);
System.out.println(text.render());
// Output: <i><b>Hello</b></i>
```

### Example 2: Pizza with Toppings

```java
interface Pizza {
    String getDescription();
    double getPrice();
}

class MargheritaPizza implements Pizza {
    public String getDescription() {
        return "Margherita Pizza";
    }
    
    public double getPrice() {
        return 8.0;
    }
}

abstract class ToppingDecorator implements Pizza {
    protected Pizza pizza;
    
    public ToppingDecorator(Pizza pizza) {
        this.pizza = pizza;
    }
}

class CheeseDecorator extends ToppingDecorator {
    public CheeseDecorator(Pizza pizza) {
        super(pizza);
    }
    
    public String getDescription() {
        return pizza.getDescription() + ", Extra Cheese";
    }
    
    public double getPrice() {
        return pizza.getPrice() + 1.5;
    }
}

class MushroomDecorator extends ToppingDecorator {
    public MushroomDecorator(Pizza pizza) {
        super(pizza);
    }
    
    public String getDescription() {
        return pizza.getDescription() + ", Mushrooms";
    }
    
    public double getPrice() {
        return pizza.getPrice() + 1.0;
    }
}

// Usage
Pizza pizza = new MargheritaPizza();
pizza = new CheeseDecorator(pizza);
pizza = new MushroomDecorator(pizza);
System.out.println(pizza.getDescription() + " = $" + pizza.getPrice());
// Output: Margherita Pizza, Extra Cheese, Mushrooms = $10.5
```

---

## ✅ When to Use

- **Add responsibilities dynamically** without affecting other objects
- **Avoid class explosion** from subclassing all combinations
- **Runtime flexibility** in adding features
- **Open/Closed Principle** - extend without modifying

---

## ❌ When NOT to Use

- Simple extension is enough (use inheritance)
- Need to add features to all instances
- Order of decorators matters and is complex
- Performance-critical (wrapping has overhead)

---

## 🎯 Pros and Cons

### Pros ✅
- **More flexible than inheritance** - runtime composition
- **Open/Closed Principle** - extend without modification
- **Single Responsibility** - each decorator one feature
- **Combine decorators** - mix and match features

### Cons ❌
- **Many small classes** - can be overwhelming
- **Hard to debug** - many wrapped objects
- **Order matters** - decorators not commutative
- **Identity issues** - decorated ≠ original object

---

## 🔄 Decorator vs Inheritance

| Aspect | Decorator | Inheritance |
|--------|-----------|-------------|
| **Flexibility** | Runtime | Compile-time |
| **Combinations** | Mix at runtime | All subclasses needed |
| **Modification** | No source change | Need source code |
| **Coupling** | Loose | Tight |

**Example:**
```java
// Inheritance - need many classes
class BoldText extends Text { }
class ItalicText extends Text { }
class BoldItalicText extends Text { } // Explosion!

// Decorator - compose at runtime
Text text = new ItalicDecorator(new BoldDecorator(new PlainText("Hi")));
```

---

## 🔄 Decorator vs Proxy

| Aspect | Decorator | Proxy |
|--------|-----------|-------|
| **Intent** | Add behavior | Control access |
| **Object** | Can change | Same object |
| **Purpose** | Enhancement | Protection/optimization |
| **Awareness** | Client knows | Client may not know |

---

## 💡 Design Principles

### Follows:
- ✅ **Open/Closed** - Open for extension, closed for modification
- ✅ **Single Responsibility** - Each decorator one feature
- ✅ **Composition over Inheritance**
- ✅ **Liskov Substitution** - Decorators substitutable

---

## 🎓 Interview Focus

### Must Know:
1. **Intent** - Add responsibilities dynamically
2. **vs Inheritance** - More flexible
3. **Structure** - Component + Decorator + Concrete decorators
4. **Java I/O** - Classic example
5. **When to use** - Avoid class explosion

### Common Questions:
1. "Implement coffee shop with decorators"
2. "Decorator vs Inheritance?"
3. "How does Java I/O use Decorator?"
4. "Decorator vs Proxy?"
5. "Disadvantages of Decorator?"

---

## 📚 Where It's Used

### In Java:
- `java.io` package - BufferedInputStream, DataInputStream
- `javax.swing` - JScrollPane wrapping JPanel
- `java.util.Collections` - synchronizedList(), unmodifiableList()

**I/O Streams Example:**
```java
InputStream in = new FileInputStream("file.txt");
in = new BufferedInputStream(in);  // Add buffering
in = new DataInputStream(in);      // Add data reading
```

### In Real World:
- Beverage customization (Starbucks)
- Car options (GPS, leather seats)
- UI frameworks
- Middleware in web frameworks

---

## 🎯 Key Takeaways

1. **Adds features dynamically** at runtime
2. **Alternative to subclassing** - prevents class explosion
3. **Wraps objects** recursively
4. **Java I/O** - prime example
5. **Open/Closed Principle** exemplified

---

**Complete Example:** [CoffeeDecorator.java](CoffeeDecorator.java)  
**Demo:** [DecoratorDemo.java](DecoratorDemo.java)  
**Interview Q&A:** [INTERVIEW_QA.md](INTERVIEW_QA.md)

---

**Next Pattern:** [Proxy →](../03-proxy/)

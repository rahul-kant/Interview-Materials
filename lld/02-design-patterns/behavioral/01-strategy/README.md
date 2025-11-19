# Strategy Pattern ⭐⭐⭐

## 📋 Intent

**Define a family of algorithms, encapsulate each one, and make them interchangeable. Strategy lets the algorithm vary independently from clients that use it.**

---

## 🎯 Problem

- You have multiple ways to do the same thing
- You want to switch algorithms at runtime
- You want to avoid multiple if-else or switch statements
- You need different behaviors in different situations

**Example Scenarios:**
- Different payment methods (Credit Card, PayPal, Crypto)
- Different sorting algorithms (Quick Sort, Merge Sort, Bubble Sort)
- Different compression algorithms (ZIP, RAR, TAR)
- Different routing algorithms (Fastest, Shortest, Scenic)

---

## ✅ Solution

1. Define a Strategy interface
2. Create concrete strategy classes implementing the interface
3. Context class uses a strategy
4. Client can switch strategies at runtime

---

## 💻 Implementation

### Basic Structure

```java
// Strategy Interface
interface PaymentStrategy {
    void pay(int amount);
}

// Concrete Strategies
class CreditCardPayment implements PaymentStrategy {
    private String cardNumber;
    
    public CreditCardPayment(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    @Override
    public void pay(int amount) {
        System.out.println("Paid $" + amount + " using Credit Card: " + cardNumber);
    }
}

class PayPalPayment implements PaymentStrategy {
    private String email;
    
    public PayPalPayment(String email) {
        this.email = email;
    }
    
    @Override
    public void pay(int amount) {
        System.out.println("Paid $" + amount + " using PayPal: " + email);
    }
}

class CryptoPayment implements PaymentStrategy {
    private String walletAddress;
    
    public CryptoPayment(String walletAddress) {
        this.walletAddress = walletAddress;
    }
    
    @Override
    public void pay(int amount) {
        System.out.println("Paid $" + amount + " using Crypto: " + walletAddress);
    }
}

// Context
class ShoppingCart {
    private PaymentStrategy paymentStrategy;
    
    public void setPaymentStrategy(PaymentStrategy strategy) {
        this.paymentStrategy = strategy;
    }
    
    public void checkout(int amount) {
        paymentStrategy.pay(amount);
    }
}

// Usage
ShoppingCart cart = new ShoppingCart();

// Use Credit Card
cart.setPaymentStrategy(new CreditCardPayment("1234-5678-9012-3456"));
cart.checkout(100);

// Switch to PayPal
cart.setPaymentStrategy(new PayPalPayment("user@example.com"));
cart.checkout(200);
```

---

## 🎨 Real-World Examples

### Example 1: Sorting Strategies

```java
interface SortStrategy {
    void sort(int[] array);
}

class QuickSort implements SortStrategy {
    public void sort(int[] array) {
        System.out.println("Sorting using Quick Sort");
        // Quick sort implementation
    }
}

class MergeSort implements SortStrategy {
    public void sort(int[] array) {
        System.out.println("Sorting using Merge Sort");
        // Merge sort implementation
    }
}

class BubbleSort implements SortStrategy {
    public void sort(int[] array) {
        System.out.println("Sorting using Bubble Sort");
        // Bubble sort implementation
    }
}

class Sorter {
    private SortStrategy strategy;
    
    public void setStrategy(SortStrategy strategy) {
        this.strategy = strategy;
    }
    
    public void sort(int[] array) {
        strategy.sort(array);
    }
}
```

### Example 2: Compression Strategies

```java
interface CompressionStrategy {
    void compress(String file);
}

class ZipCompression implements CompressionStrategy {
    public void compress(String file) {
        System.out.println("Compressing " + file + " using ZIP");
    }
}

class RarCompression implements CompressionStrategy {
    public void compress(String file) {
        System.out.println("Compressing " + file + " using RAR");
    }
}

class FileCompressor {
    private CompressionStrategy strategy;
    
    public void setStrategy(CompressionStrategy strategy) {
        this.strategy = strategy;
    }
    
    public void compress(String file) {
        strategy.compress(file);
    }
}
```

---

## ✅ When to Use

- **Multiple algorithms** for the same task
- **Runtime selection** of algorithm needed
- **Avoid conditionals** (if-else, switch)
- **Encapsulate complex algorithms**
- **Open/Closed Principle** - add new strategies without modifying existing code

---

## ❌ When NOT to Use

- Only one algorithm needed
- Algorithms are tightly coupled to context
- Simple logic (overkill for simple cases)
- Performance-critical code (slight overhead)

---

## 🎯 Pros and Cons

### Pros ✅
- **Open/Closed Principle** - easy to add new strategies
- **Single Responsibility** - each strategy isolated
- **Runtime switching** of algorithms
- **Eliminates conditionals** - no if-else chains
- **Easy testing** - test each strategy independently

### Cons ❌
- **More classes** - can increase complexity
- **Client awareness** - client must know about strategies
- **Overhead** - slight performance overhead

---

## 🔄 Strategy vs State

**Common Confusion!** Both have similar structure but different intent.

| Aspect | Strategy | State |
|--------|----------|-------|
| **Intent** | Choose algorithm | Change behavior based on state |
| **Change** | Client switches | Object switches internally |
| **Awareness** | Client knows strategies | Client unaware of states |
| **Example** | Payment methods | Order status |

**Strategy:**
```java
// Client chooses
cart.setPaymentStrategy(new CreditCard());
```

**State:**
```java
// Object changes state internally
order.ship(); // Changes from Processing to Shipped state
```

---

## 🔄 Strategy vs Template Method

| Aspect | Strategy | Template Method |
|--------|----------|-----------------|
| **Pattern Type** | Composition | Inheritance |
| **Flexibility** | Runtime switch | Compile-time |
| **Coupling** | Loose | Tight |
| **Algorithms** | Completely different | Similar with variations |

---

## 💡 Design Principles

### Follows:
- ✅ **Open/Closed Principle** - Open for extension, closed for modification
- ✅ **Single Responsibility** - Each strategy has one job
- ✅ **Dependency Inversion** - Depend on abstractions
- ✅ **Composition over Inheritance**

### Enables:
- Runtime behavior change
- Easy testing
- Clean code (no if-else chains)

---

## 🎓 Interview Focus

### Must Know:
1. **Structure** - Strategy interface + concrete strategies + context
2. **When to use** - Multiple algorithms, runtime switching
3. **vs State** - Key differences
4. **Open/Closed** - How it enables OCP
5. **Real examples** - Payment, sorting, compression

### Common Questions:
1. "Implement payment strategies"
2. "Strategy vs State pattern?"
3. "How does Strategy follow Open/Closed?"
4. "Real-world examples?"
5. "When would you use this?"

---

## 📝 Interview Questions & Answers

See [INTERVIEW_QA.md](INTERVIEW_QA.md) for detailed Q&A

---

## 🔗 Related Patterns

- **State** - Similar structure, different intent
- **Template Method** - Alternative using inheritance
- **Command** - Can be used together
- **Factory** - Often used to create strategies

---

## 📚 Where It's Used

### In Java:
- `Comparator` - Different sorting strategies
- `java.util.concurrent.ThreadPoolExecutor.RejectedExecutionHandler`
- Layout managers in GUI frameworks

### In Real World:
- Payment gateways
- Data compression tools
- Routing algorithms
- Pricing strategies
- Authentication methods

---

## 🎯 Key Takeaways

1. **Family of algorithms** made interchangeable
2. **Runtime switching** possible
3. **Eliminates conditionals**
4. **Open/Closed Principle** exemplified
5. **Composition over inheritance**

---

**Complete Example:** [PaymentStrategy.java](PaymentStrategy.java)  
**Demo:** [StrategyDemo.java](StrategyDemo.java)  
**Interview Q&A:** [INTERVIEW_QA.md](INTERVIEW_QA.md)

---

**Next Pattern:** [Observer →](../14-observer/)

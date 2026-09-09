# State Pattern ⭐⭐

## 📋 Intent

**Allow an object to alter its behavior when its internal state changes. The object will appear to change its class.**

---

## 🎯 Problem

- Object behavior depends on its state
- Large conditional statements based on state
- State transitions are complex
- Want to add new states easily

**Example Scenarios:**
- Vending machine states
- Order processing (Pending → Paid → Shipped)
- TCP connection states
- Document workflow

---

## 💻 Implementation

```java
// State Interface
interface State {
    void handle(Context context);
}

// Concrete States
class StartState implements State {
    public void handle(Context context) {
        System.out.println("In START state");
        context.setState(new ProcessingState());
    }
}

class ProcessingState implements State {
    public void handle(Context context) {
        System.out.println("In PROCESSING state");
        context.setState(new EndState());
    }
}

class EndState implements State {
    public void handle(Context context) {
        System.out.println("In END state");
    }
}

// Context
class Context {
    private State state;
    
    public Context() {
        state = new StartState();
    }
    
    public void setState(State state) {
        this.state = state;
    }
    
    public void request() {
        state.handle(this);
    }
}
```

---

## 🔄 State vs Strategy

| Aspect | State | Strategy |
|--------|-------|----------|
| **Intent** | Change behavior based on state | Choose algorithm |
| **Switching** | Automatic (by object) | Manual (by client) |
| **Awareness** | States know each other | Strategies independent |

---

## 🎯 Key Takeaways

1. **Behavior changes** with state
2. **State transitions** encapsulated
3. **Eliminates conditionals**
4. **States know each other** (vs Strategy)

---

**Next Pattern:** [Template Method →](../17-template-method/)

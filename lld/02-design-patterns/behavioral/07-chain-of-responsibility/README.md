# Chain of Responsibility Pattern ⭐

## 📋 Intent

**Avoid coupling the sender of a request to its receiver by giving more than one object a chance to handle the request. Chain the receiving objects and pass the request along the chain until an object handles it.**

---

## 💻 Implementation

```java
abstract class Handler {
    protected Handler nextHandler;
    
    public void setNext(Handler handler) {
        this.nextHandler = handler;
    }
    
    public abstract void handleRequest(String request);
}

class ConcreteHandler1 extends Handler {
    public void handleRequest(String request) {
        if (request.equals("Type1")) {
            System.out.println("Handler1 processed");
        } else if (nextHandler != null) {
            nextHandler.handleRequest(request);
        }
    }
}

class ConcreteHandler2 extends Handler {
    public void handleRequest(String request) {
        if (request.equals("Type2")) {
            System.out.println("Handler2 processed");
        } else if (nextHandler != null) {
            nextHandler.handleRequest(request);
        }
    }
}

// Usage
Handler h1 = new ConcreteHandler1();
Handler h2 = new ConcreteHandler2();
h1.setNext(h2);
h1.handleRequest("Type2");
```

---

## ✅ When to Use

- Multiple objects can handle request
- Handler not known beforehand
- Dynamic chain of handlers

---

**Next Pattern:** [Mediator →](../20-mediator/)

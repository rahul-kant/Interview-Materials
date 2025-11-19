# Proxy Pattern ⭐⭐

## 📋 Intent

**Provide a surrogate or placeholder for another object to control access to it.**

---

## 🎯 Problem

- Need to control access to an object
- Object creation is expensive
- Want to add security checks
- Need lazy initialization
- Want to add logging/caching

**Example Scenarios:**
- Virtual proxy (lazy loading images)
- Protection proxy (access control)
- Remote proxy (remote object access)
- Smart reference (reference counting)

---

## ✅ Solution

Create a proxy class that:
1. Implements same interface as real object
2. Controls access to real object
3. May add additional behavior

---

## 💻 Implementation

### Virtual Proxy (Lazy Loading)

```java
interface Image {
    void display();
}

class RealImage implements Image {
    private String filename;
    
    public RealImage(String filename) {
        this.filename = filename;
        loadFromDisk();
    }
    
    private void loadFromDisk() {
        System.out.println("Loading " + filename);
    }
    
    public void display() {
        System.out.println("Displaying " + filename);
    }
}

class ProxyImage implements Image {
    private RealImage realImage;
    private String filename;
    
    public ProxyImage(String filename) {
        this.filename = filename;
    }
    
    public void display() {
        if (realImage == null) {
            realImage = new RealImage(filename); // Lazy load
        }
        realImage.display();
    }
}

// Usage
Image image = new ProxyImage("photo.jpg");
// Image not loaded yet
image.display(); // Loaded now
```

### Protection Proxy (Access Control)

```java
interface BankAccount {
    void withdraw(double amount);
    double getBalance();
}

class RealBankAccount implements BankAccount {
    private double balance = 1000;
    
    public void withdraw(double amount) {
        balance -= amount;
        System.out.println("Withdrawn: $" + amount);
    }
    
    public double getBalance() {
        return balance;
    }
}

class ProtectionProxy implements BankAccount {
    private RealBankAccount account;
    private String userRole;
    
    public ProtectionProxy(String userRole) {
        this.account = new RealBankAccount();
        this.userRole = userRole;
    }
    
    public void withdraw(double amount) {
        if (userRole.equals("OWNER")) {
            account.withdraw(amount);
        } else {
            System.out.println("Access denied!");
        }
    }
    
    public double getBalance() {
        return account.getBalance();
    }
}
```

---

## 🎨 Real-World Examples

### Example 1: Caching Proxy

```java
interface DataService {
    String getData(String key);
}

class RealDataService implements DataService {
    public String getData(String key) {
        System.out.println("Fetching from database...");
        return "Data for " + key;
    }
}

class CachingProxy implements DataService {
    private RealDataService realService;
    private Map<String, String> cache;
    
    public CachingProxy() {
        this.realService = new RealDataService();
        this.cache = new HashMap<>();
    }
    
    public String getData(String key) {
        if (cache.containsKey(key)) {
            System.out.println("Returning from cache");
            return cache.get(key);
        }
        
        String data = realService.getData(key);
        cache.put(key, data);
        return data;
    }
}
```

---

## ✅ When to Use

- **Lazy initialization** - delay expensive object creation
- **Access control** - protect access to object
- **Logging** - log requests to object
- **Caching** - cache results
- **Remote object** - represent remote object locally

---

## 🎯 Pros and Cons

### Pros ✅
- **Control access** to real object
- **Lazy initialization** - improve performance
- **Open/Closed** - add proxy without changing real object

### Cons ❌
- **Complexity** - extra layer
- **Response delay** - if creating real object

---

## 🔄 Proxy vs Decorator

| Aspect | Proxy | Decorator |
|--------|-------|-----------|
| **Intent** | Control access | Add behavior |
| **Identity** | Same object | Can change behavior |
| **Creation** | Controls creation | Wraps existing |

---

## 🎓 Interview Focus

### Must Know:
1. **Intent** - Control access to object
2. **Types** - Virtual, Protection, Remote
3. **vs Decorator** - Key differences
4. **When to use** - Lazy loading, access control

---

**Complete Example:** [ImageProxy.java](ImageProxy.java)

---

**Next Pattern:** [Composite →](../04-composite/)

# Observer Pattern ⭐⭐⭐

## 📋 Intent

**Define a one-to-many dependency between objects so that when one object changes state, all its dependents are notified and updated automatically.**

---

## 🎯 Problem

- Need to notify multiple objects when another object changes
- Don't want tight coupling between objects
- Want to add/remove observers dynamically
- Object state changes need to trigger updates

**Example Scenarios:**
- Newsletter subscriptions
- Event systems
- MVC architecture (Model notifies Views)
- Stock price updates
- Social media notifications

---

## ✅ Solution

1. Subject maintains list of observers
2. Observers register/unregister with subject
3. Subject notifies all observers when state changes
4. Observers update themselves

---

## 💻 Implementation

### Basic Structure

```java
// Observer Interface
interface Observer {
    void update(String message);
}

// Subject Interface
interface Subject {
    void attach(Observer observer);
    void detach(Observer observer);
    void notifyObservers();
}

// Concrete Subject
class NewsAgency implements Subject {
    private List<Observer> observers = new ArrayList<>();
    private String news;
    
    public void attach(Observer observer) {
        observers.add(observer);
    }
    
    public void detach(Observer observer) {
        observers.remove(observer);
    }
    
    public void notifyObservers() {
        for (Observer observer : observers) {
            observer.update(news);
        }
    }
    
    public void setNews(String news) {
        this.news = news;
        notifyObservers();
    }
}

// Concrete Observer
class NewsChannel implements Observer {
    private String name;
    
    public NewsChannel(String name) {
        this.name = name;
    }
    
    public void update(String news) {
        System.out.println(name + " received: " + news);
    }
}

// Usage
NewsAgency agency = new NewsAgency();
NewsChannel cnn = new NewsChannel("CNN");
NewsChannel bbc = new NewsChannel("BBC");

agency.attach(cnn);
agency.attach(bbc);
agency.setNews("Breaking News!");
// Both CNN and BBC get notified
```

---

## 🎨 Real-World Examples

### Example 1: Stock Market

```java
interface StockObserver {
    void update(String stock, double price);
}

class StockMarket {
    private Map<String, List<StockObserver>> observers = new HashMap<>();
    
    public void subscribe(String stock, StockObserver observer) {
        observers.computeIfAbsent(stock, k -> new ArrayList<>()).add(observer);
    }
    
    public void updateStockPrice(String stock, double price) {
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
        System.out.println(name + ": " + stock + " is now $" + price);
    }
}
```

### Example 2: Weather Station

```java
class WeatherStation {
    private List<Observer> observers = new ArrayList<>();
    private float temperature;
    private float humidity;
    
    public void attach(Observer observer) {
        observers.add(observer);
    }
    
    public void setMeasurements(float temp, float humidity) {
        this.temperature = temp;
        this.humidity = humidity;
        notifyObservers();
    }
    
    private void notifyObservers() {
        String data = "Temp: " + temperature + "°C, Humidity: " + humidity + "%";
        for (Observer observer : observers) {
            observer.update(data);
        }
    }
}

class PhoneDisplay implements Observer {
    public void update(String data) {
        System.out.println("Phone Display: " + data);
    }
}

class TVDisplay implements Observer {
    public void update(String data) {
        System.out.println("TV Display: " + data);
    }
}
```

---

## ✅ When to Use

- **One-to-many relationship** between objects
- **Loose coupling** needed
- **Dynamic subscription** - add/remove observers at runtime
- **Event-driven systems**
- **MVC architecture**

---

## 🎯 Pros and Cons

### Pros ✅
- **Loose coupling** - subject and observers independent
- **Dynamic relationships** - add/remove at runtime
- **Broadcast communication** - notify multiple objects
- **Open/Closed Principle** - add observers without modifying subject

### Cons ❌
- **Unexpected updates** - observers may not know about each other
- **Memory leaks** - if observers not properly detached
- **Performance** - many observers = slow notifications
- **Update order** - unpredictable notification sequence

---

## 🔄 Push vs Pull Model

### Push Model
```java
// Subject pushes all data to observers
interface Observer {
    void update(float temp, float humidity, float pressure);
}

// Observers get all data even if they don't need it
```

### Pull Model
```java
// Observers pull data they need
interface Observer {
    void update(Subject subject);
}

class ConcreteObserver implements Observer {
    public void update(Subject subject) {
        // Observer pulls only what it needs
        float temp = subject.getTemperature();
    }
}
```

**Pull model is preferred** - more flexible

---

## 💡 Design Principles

### Follows:
- ✅ **Loose Coupling** - Subject and observers independent
- ✅ **Open/Closed** - Add observers without modifying subject
- ✅ **Single Responsibility** - Each observer handles its own update

---

## 🎓 Interview Focus

### Must Know:
1. **Intent** - One-to-many notification
2. **Structure** - Subject + Observers
3. **Push vs Pull** - Key difference
4. **Real examples** - MVC, Event systems
5. **Memory leaks** - Detach observers

### Common Questions:
1. "Implement Observer pattern"
2. "Push vs Pull model?"
3. "How is Observer used in MVC?"
4. "Observer vs Mediator?"
5. "Memory leak prevention?"

---

## 📚 Where It's Used

### In Java:
- `java.util.Observer` (deprecated but educational)
- `java.util.EventListener`
- `java.beans.PropertyChangeListener`
- JavaFX properties
- RxJava Observables

### In Frameworks:
- Spring ApplicationEventPublisher
- Android LiveData
- Vue.js reactivity
- React useState

---

## 🎯 Key Takeaways

1. **One-to-many dependency** - notify all observers
2. **Loose coupling** - observers don't know each other
3. **Dynamic subscription** - add/remove at runtime
4. **Used everywhere** - events, MVC, reactive programming
5. **Watch for memory leaks** - always detach

---

**Complete Example:** [NewsObserver.java](NewsObserver.java)  
**Demo:** [ObserverDemo.java](ObserverDemo.java)

---

**Next Pattern:** [Command →](../15-command/)

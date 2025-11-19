# Facade Pattern ⭐⭐

## 📋 Intent

**Provide a unified interface to a set of interfaces in a subsystem. Facade defines a higher-level interface that makes the subsystem easier to use.**

---

## 🎯 Problem

- Complex subsystem with many classes
- Client needs to interact with many classes
- Want to hide complexity
- Need simplified interface

**Example Scenarios:**
- Home theater system
- Computer startup
- Library wrappers
- API simplification

---

## 💻 Implementation

```java
// Complex subsystem classes
class DVDPlayer {
    public void on() { System.out.println("DVD Player on"); }
    public void play(String movie) {
        System.out.println("Playing: " + movie);
    }
    public void off() { System.out.println("DVD Player off"); }
}

class Projector {
    public void on() { System.out.println("Projector on"); }
    public void wideScreenMode() { System.out.println("Wide screen mode"); }
    public void off() { System.out.println("Projector off"); }
}

class SoundSystem {
    public void on() { System.out.println("Sound system on"); }
    public void setVolume(int level) {
        System.out.println("Volume: " + level);
    }
    public void off() { System.out.println("Sound system off"); }
}

// Facade - simplified interface
class HomeTheaterFacade {
    private DVDPlayer dvd;
    private Projector projector;
    private SoundSystem sound;
    
    public HomeTheaterFacade(DVDPlayer dvd, Projector projector, 
                            SoundSystem sound) {
        this.dvd = dvd;
        this.projector = projector;
        this.sound = sound;
    }
    
    public void watchMovie(String movie) {
        System.out.println("Get ready to watch a movie...");
        projector.on();
        projector.wideScreenMode();
        sound.on();
        sound.setVolume(10);
        dvd.on();
        dvd.play(movie);
    }
    
    public void endMovie() {
        System.out.println("Shutting down...");
        dvd.off();
        sound.off();
        projector.off();
    }
}

// Usage
HomeTheaterFacade homeTheater = new HomeTheaterFacade(
    new DVDPlayer(), new Projector(), new SoundSystem());
homeTheater.watchMovie("Inception");
homeTheater.endMovie();
```

---

## ✅ When to Use

- **Complex subsystem** needs simplification
- **Layer between** client and complex system
- **Reduce dependencies** on subsystem

---

## 🎯 Pros and Cons

### Pros ✅
- **Simplifies interface** - easier to use
- **Decouples** client from subsystem
- **Layers** - provides entry point

### Cons ❌
- **God object** - facade can become too large
- **Limited** - may not expose all functionality

---

## 🎯 Key Takeaways

1. **Simplifies complex systems**
2. **Unified interface** to subsystem
3. **Not about hiding** - about simplifying

---

**Next Pattern:** [Flyweight →](../07-flyweight/)

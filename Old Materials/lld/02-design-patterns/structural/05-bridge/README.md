# Bridge Pattern ⭐

## 📋 Intent

**Decouple an abstraction from its implementation so that the two can vary independently.**

---

## 🎯 Problem

- Abstraction and implementation tightly coupled
- Changes to one affect the other
- Combinatorial explosion of subclasses
- Need both to vary independently

**Example Scenarios:**
- Remote controls for different devices
- Graphics rendering for different platforms
- Notification system (message type + channel)

---

## 💻 Implementation

```java
// Implementation (platform-specific)
interface Device {
    void turnOn();
    void turnOff();
    void setVolume(int volume);
}

class TV implements Device {
    public void turnOn() { System.out.println("TV on"); }
    public void turnOff() { System.out.println("TV off"); }
    public void setVolume(int volume) {
        System.out.println("TV volume: " + volume);
    }
}

class Radio implements Device {
    public void turnOn() { System.out.println("Radio on"); }
    public void turnOff() { System.out.println("Radio off"); }
    public void setVolume(int volume) {
        System.out.println("Radio volume: " + volume);
    }
}

// Abstraction
abstract class RemoteControl {
    protected Device device;
    
    public RemoteControl(Device device) {
        this.device = device;
    }
    
    public void togglePower() {
        // Default implementation
    }
}

// Refined Abstraction
class AdvancedRemoteControl extends RemoteControl {
    public AdvancedRemoteControl(Device device) {
        super(device);
    }
    
    public void mute() {
        device.setVolume(0);
    }
}

// Usage
Device tv = new TV();
RemoteControl remote = new AdvancedRemoteControl(tv);
remote.mute();
```

---

## ✅ When to Use

- **Separate abstraction from implementation**
- **Both should vary independently**
- **Avoid permanent binding**

---

## 🔄 Bridge vs Adapter

| Aspect | Bridge | Adapter |
|--------|--------|---------|
| **When** | Design time | After creation |
| **Intent** | Separate abstraction | Make compatible |

---

**Next Pattern:** [Facade →](../06-facade/)

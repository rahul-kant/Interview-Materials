# Command Pattern ⭐⭐

## 📋 Intent

**Encapsulate a request as an object, thereby letting you parameterize clients with different requests, queue or log requests, and support undoable operations.**

---

## 🎯 Problem

- Need to parameterize objects with operations
- Want to queue, log, or schedule operations
- Need undo/redo functionality
- Decouple sender from receiver

**Example Scenarios:**
- Remote control
- Text editor (undo/redo)
- Transaction systems
- Job queues
- Macro recording

---

## 💻 Implementation

```java
// Command Interface
interface Command {
    void execute();
    void undo();
}

// Receiver
class Light {
    public void on() {
        System.out.println("Light is ON");
    }
    
    public void off() {
        System.out.println("Light is OFF");
    }
}

// Concrete Commands
class LightOnCommand implements Command {
    private Light light;
    
    public LightOnCommand(Light light) {
        this.light = light;
    }
    
    public void execute() {
        light.on();
    }
    
    public void undo() {
        light.off();
    }
}

class LightOffCommand implements Command {
    private Light light;
    
    public LightOffCommand(Light light) {
        this.light = light;
    }
    
    public void execute() {
        light.off();
    }
    
    public void undo() {
        light.on();
    }
}

// Invoker
class RemoteControl {
    private Command command;
    private Stack<Command> history = new Stack<>();
    
    public void setCommand(Command command) {
        this.command = command;
    }
    
    public void pressButton() {
        command.execute();
        history.push(command);
    }
    
    public void pressUndo() {
        if (!history.isEmpty()) {
            Command lastCommand = history.pop();
            lastCommand.undo();
        }
    }
}

// Usage
Light light = new Light();
Command lightOn = new LightOnCommand(light);
Command lightOff = new LightOffCommand(light);

RemoteControl remote = new RemoteControl();
remote.setCommand(lightOn);
remote.pressButton(); // Light is ON
remote.pressUndo();   // Light is OFF
```

---

## ✅ When to Use

- **Undo/Redo** functionality needed
- **Queue operations** for later execution
- **Log operations** for audit trail
- **Parameterize objects** with actions
- **Transaction management**

---

## 🎯 Key Takeaways

1. **Encapsulates request** as object
2. **Decouples** sender from receiver
3. **Supports undo/redo**
4. **Queue and log** operations
5. **Macro commands** possible

---

**Next Pattern:** [State →](../16-state/)

# Adapter Pattern ⭐⭐

## 📋 Intent

**Convert the interface of a class into another interface clients expect. Adapter lets classes work together that couldn't otherwise because of incompatible interfaces.**

---

## 🎯 Problem

- You have an existing class with useful functionality
- But its interface doesn't match what client expects
- You can't or don't want to modify either class
- Need to make incompatible interfaces work together

**Example Scenarios:**
- Legacy code integration
- Third-party library with different interface
- Database drivers (JDBC adapters)
- Media players supporting different formats

---

## ✅ Solution

Create an adapter class that:
1. Implements the target interface
2. Wraps the adaptee (existing class)
3. Translates calls from target to adaptee

---

## 💻 Implementation

### Class Adapter (Using Inheritance)

```java
// Target Interface (what client expects)
interface MediaPlayer {
    void play(String audioType, String fileName);
}

// Adaptee (existing class with different interface)
class AdvancedMediaPlayer {
    public void playVlc(String fileName) {
        System.out.println("Playing vlc file: " + fileName);
    }
    
    public void playMp4(String fileName) {
        System.out.println("Playing mp4 file: " + fileName);
    }
}

// Adapter (Class Adapter using inheritance)
class MediaAdapter extends AdvancedMediaPlayer implements MediaPlayer {
    public void play(String audioType, String fileName) {
        if(audioType.equalsIgnoreCase("vlc")) {
            playVlc(fileName);
        } else if(audioType.equalsIgnoreCase("mp4")) {
            playMp4(fileName);
        }
    }
}

// Client
class AudioPlayer implements MediaPlayer {
    MediaAdapter mediaAdapter;
    
    public void play(String audioType, String fileName) {
        if(audioType.equalsIgnoreCase("mp3")) {
            System.out.println("Playing mp3 file: " + fileName);
        } else if(audioType.equalsIgnoreCase("vlc") || 
                  audioType.equalsIgnoreCase("mp4")) {
            mediaAdapter = new MediaAdapter();
            mediaAdapter.play(audioType, fileName);
        } else {
            System.out.println("Invalid format");
        }
    }
}
```

### Object Adapter (Using Composition) - Preferred

```java
// Target Interface
interface MediaPlayer {
    void play(String audioType, String fileName);
}

// Adaptee
class AdvancedMediaPlayer {
    public void playVlc(String fileName) {
        System.out.println("Playing vlc file: " + fileName);
    }
    
    public void playMp4(String fileName) {
        System.out.println("Playing mp4 file: " + fileName);
    }
}

// Adapter (Object Adapter using composition)
class MediaAdapter implements MediaPlayer {
    AdvancedMediaPlayer advancedPlayer;
    
    public MediaAdapter(String audioType) {
        advancedPlayer = new AdvancedMediaPlayer();
    }
    
    public void play(String audioType, String fileName) {
        if(audioType.equalsIgnoreCase("vlc")) {
            advancedPlayer.playVlc(fileName);
        } else if(audioType.equalsIgnoreCase("mp4")) {
            advancedPlayer.playMp4(fileName);
        }
    }
}
```

---

## 🎨 Real-World Examples

### Example 1: Legacy System Integration

```java
// New interface
interface PaymentProcessor {
    void processPayment(double amount);
}

// Legacy class (can't modify)
class LegacyPaymentGateway {
    public void makePayment(int cents) {
        System.out.println("Processing " + cents + " cents");
    }
}

// Adapter
class PaymentAdapter implements PaymentProcessor {
    private LegacyPaymentGateway legacyGateway;
    
    public PaymentAdapter() {
        this.legacyGateway = new LegacyPaymentGateway();
    }
    
    public void processPayment(double amount) {
        // Convert dollars to cents
        int cents = (int)(amount * 100);
        legacyGateway.makePayment(cents);
    }
}

// Usage
PaymentProcessor processor = new PaymentAdapter();
processor.processPayment(10.50); // Converts to 1050 cents
```

### Example 2: Temperature Converter

```java
// Target Interface (Celsius)
interface TemperatureSensor {
    double getTemperature(); // Returns Celsius
}

// Adaptee (Fahrenheit sensor)
class FahrenheitSensor {
    public double getTemperatureInFahrenheit() {
        return 98.6; // Body temperature in F
    }
}

// Adapter
class TemperatureAdapter implements TemperatureSensor {
    private FahrenheitSensor fahrenheitSensor;
    
    public TemperatureAdapter(FahrenheitSensor sensor) {
        this.fahrenheitSensor = sensor;
    }
    
    public double getTemperature() {
        double fahrenheit = fahrenheitSensor.getTemperatureInFahrenheit();
        return (fahrenheit - 32) * 5/9; // Convert to Celsius
    }
}

// Usage
FahrenheitSensor fSensor = new FahrenheitSensor();
TemperatureSensor cSensor = new TemperatureAdapter(fSensor);
System.out.println("Temperature: " + cSensor.getTemperature() + "°C");
```

---

## ✅ When to Use

- **Legacy code integration** - Old code with incompatible interface
- **Third-party libraries** - Can't modify external code
- **Multiple incompatible interfaces** - Need unified interface
- **Reuse existing code** - With different interface

---

## ❌ When NOT to Use

- You can modify the original class
- Simple wrapper is enough
- Interfaces are already compatible
- Adding unnecessary complexity

---

## 🎯 Pros and Cons

### Pros ✅
- **Single Responsibility** - Separates interface conversion
- **Open/Closed** - Add new adapters without modifying code
- **Reusability** - Reuse existing code with new interfaces
- **Flexibility** - Adapt multiple incompatible interfaces

### Cons ❌
- **Complexity** - Extra layer of indirection
- **Performance** - Slight overhead
- **Can be overused** - Sometimes direct modification better

---

## 🔄 Class Adapter vs Object Adapter

| Aspect | Class Adapter | Object Adapter |
|--------|--------------|----------------|
| **Mechanism** | Inheritance | Composition |
| **Flexibility** | Less flexible | More flexible |
| **Overriding** | Can override adaptee methods | Cannot override |
| **Multiple Adaptees** | No (single inheritance) | Yes |
| **Preferred** | No | Yes (composition over inheritance) |

---

## 🔄 Adapter vs Bridge

| Aspect | Adapter | Bridge |
|--------|---------|--------|
| **Intent** | Make incompatible work together | Separate abstraction from implementation |
| **When Applied** | After classes created | During design |
| **Focus** | Interface compatibility | Extensibility |
| **Structure** | Wraps existing | Designed from start |

---

## 💡 Design Principles

### Follows:
- ✅ **Single Responsibility** - Conversion logic isolated
- ✅ **Open/Closed** - Add adapters without modification
- ✅ **Dependency Inversion** - Depend on target interface
- ✅ **Composition over Inheritance** (Object Adapter)

---

## 🎓 Interview Focus

### Must Know:
1. **Intent** - Make incompatible interfaces compatible
2. **Two types** - Class vs Object adapter
3. **When to use** - Legacy integration, third-party libraries
4. **vs Bridge** - Key differences
5. **Real examples** - JDBC, I/O streams

### Common Questions:
1. "Implement an adapter for legacy system"
2. "Class vs Object adapter?"
3. "When would you use Adapter?"
4. "Adapter vs Decorator vs Proxy?"
5. "Real-world examples?"

---

## 📚 Where It's Used

### In Java:
- `java.io.InputStreamReader` - Adapts InputStream to Reader
- `java.io.OutputStreamWriter` - Adapts OutputStream to Writer
- `java.util.Arrays.asList()` - Adapts array to List
- JDBC `DriverManager` - Adapts different DB drivers

### In Real World:
- Power adapters (110V to 220V)
- Memory card readers
- USB-C to USB-A adapters
- API wrappers

---

## 🎯 Key Takeaways

1. **Converts interfaces** for compatibility
2. **Object adapter preferred** (composition)
3. **Used after classes exist** (vs Bridge)
4. **Enables reuse** of existing code
5. **Common in integration** scenarios

---

**Complete Example:** [MediaPlayerAdapter.java](MediaPlayerAdapter.java)  
**Demo:** [AdapterDemo.java](AdapterDemo.java)  
**Interview Q&A:** [INTERVIEW_QA.md](INTERVIEW_QA.md)

---

**Next Pattern:** [Decorator →](../02-decorator/)

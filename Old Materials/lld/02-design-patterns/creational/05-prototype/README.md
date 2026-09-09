# Prototype Pattern ⭐

## 📋 Intent

**Specify the kinds of objects to create using a prototypical instance, and create new objects by copying this prototype.**

---

## 🎯 Problem

- Object creation is expensive (complex initialization, DB queries, network calls)
- Want to avoid subclassing just to create objects
- Need to create objects dynamically at runtime
- Want to hide complexity of creating new instances

**Example Scenarios:**
- Cloning game objects (avoiding expensive texture loading)
- Copying database records
- Creating document templates
- Cloning configuration objects

---

## ✅ Solution

1. Create a Prototype interface with `clone()` method
2. Implement `clone()` in concrete classes
3. Client clones objects instead of creating new ones
4. Use Java's `Cloneable` interface and `Object.clone()`

---

## 💻 Implementation

### Basic Structure

```java
// Prototype Interface
interface Prototype {
    Prototype clone();
}

// Concrete Prototype
class Circle implements Prototype {
    private int x;
    private int y;
    private int radius;
    private String color;
    
    public Circle(int x, int y, int radius, String color) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
    }
    
    // Copy constructor (preferred way)
    public Circle(Circle circle) {
        this.x = circle.x;
        this.y = circle.y;
        this.radius = circle.radius;
        this.color = circle.color;
    }
    
    @Override
    public Circle clone() {
        return new Circle(this);
    }
    
    @Override
    public String toString() {
        return "Circle [x=" + x + ", y=" + y + 
               ", radius=" + radius + ", color=" + color + "]";
    }
}

// Usage
Circle original = new Circle(10, 20, 5, "red");
Circle copy = original.clone();
```

---

## 🎨 Real-World Examples

### Example 1: Using Java's Cloneable

```java
class Employee implements Cloneable {
    private String name;
    private int age;
    private Department department; // Reference type
    
    public Employee(String name, int age, Department department) {
        this.name = name;
        this.age = age;
        this.department = department;
    }
    
    // Shallow copy
    @Override
    public Employee clone() {
        try {
            return (Employee) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
    
    // Deep copy
    public Employee deepClone() {
        try {
            Employee cloned = (Employee) super.clone();
            // Clone the department too for deep copy
            cloned.department = new Department(this.department.getName());
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
    
    // Getters and setters...
}

class Department {
    private String name;
    
    public Department(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
}

// Usage
Department dept = new Department("Engineering");
Employee original = new Employee("John", 30, dept);

// Shallow copy - shares Department reference
Employee shallowCopy = original.clone();

// Deep copy - has its own Department instance
Employee deepCopy = original.deepClone();
```

### Example 2: Prototype Registry

```java
// Prototype Registry (cache of prototypes)
class ShapeCache {
    private static Map<String, Shape> shapeMap = new HashMap<>();
    
    public static Shape getShape(String shapeId) {
        Shape cachedShape = shapeMap.get(shapeId);
        return (Shape) cachedShape.clone();
    }
    
    public static void loadCache() {
        Circle circle = new Circle(10, 10, 5, "red");
        shapeMap.put("circle", circle);
        
        Rectangle rectangle = new Rectangle(0, 0, 10, 20, "blue");
        shapeMap.put("rectangle", rectangle);
        
        Square square = new Square(0, 0, 10, "green");
        shapeMap.put("square", square);
    }
}

// Usage
ShapeCache.loadCache();

Shape clonedCircle = ShapeCache.getShape("circle");
Shape clonedRectangle = ShapeCache.getShape("rectangle");
```

### Example 3: Game Object Cloning

```java
class GameCharacter implements Cloneable {
    private String name;
    private int health;
    private int mana;
    private Weapon weapon;
    private Armor armor;
    
    public GameCharacter(String name, int health, int mana,
                        Weapon weapon, Armor armor) {
        this.name = name;
        this.health = health;
        this.mana = mana;
        this.weapon = weapon;
        this.armor = armor;
    }
    
    @Override
    public GameCharacter clone() {
        try {
            GameCharacter cloned = (GameCharacter) super.clone();
            // Deep copy weapon and armor
            cloned.weapon = this.weapon.clone();
            cloned.armor = this.armor.clone();
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}

// Usage - create template and clone multiple enemies
GameCharacter enemyTemplate = new GameCharacter("Orc", 100, 50,
                                 new Sword(), new LeatherArmor());

// Clone 10 enemies quickly
List<GameCharacter> enemies = new ArrayList<>();
for (int i = 0; i < 10; i++) {
    enemies.add(enemyTemplate.clone());
}
```

---

## ✅ When to Use

- **Object creation is expensive** (network, DB, file I/O)
- **Avoid subclassing** for object creation
- **Runtime object creation** needed
- **Template/default objects** to clone from
- **Reduce number of classes** in system

---

## ❌ When NOT to Use

- Object creation is simple and cheap
- Deep copy is complex (many nested objects)
- Objects have circular references
- Immutable objects (no need to clone)

---

## 🎯 Pros and Cons

### Pros ✅
- **Performance** - avoids expensive initialization
- **Flexibility** - add/remove objects at runtime
- **Fewer classes** - no need for subclassing
- **Complex initialization** - clone configured objects

### Cons ❌
- **Deep copy complexity** - need to handle all reference types
- **Circular references** - can be problematic
- **Cloneable issues** - Java's implementation has problems

---

## 🔄 Shallow Copy vs Deep Copy

### Shallow Copy
```java
class Person implements Cloneable {
    String name;
    Address address; // Reference type
    
    public Person clone() {
        return (Person) super.clone(); // Shallow copy
    }
}

// Problem:
Person p1 = new Person("John", new Address("NYC"));
Person p2 = p1.clone();
p2.address.city = "LA"; // Changes p1's address too!
```

### Deep Copy
```java
class Person implements Cloneable {
    String name;
    Address address;
    
    public Person clone() {
        Person cloned = (Person) super.clone();
        cloned.address = new Address(this.address); // Deep copy
        return cloned;
    }
}

// Better:
Person p1 = new Person("John", new Address("NYC"));
Person p2 = p1.clone();
p2.address.city = "LA"; // Only p2's address changes
```

---

## 💡 Alternatives to Java's Cloneable

### 1. Copy Constructor (Recommended)
```java
class Employee {
    private String name;
    private int age;
    
    // Copy constructor
    public Employee(Employee employee) {
        this.name = employee.name;
        this.age = employee.age;
    }
}
```

### 2. Factory Method
```java
class Employee {
    public static Employee createCopy(Employee employee) {
        return new Employee(employee.name, employee.age);
    }
}
```

### 3. Serialization (for deep copy)
```java
public static <T> T deepCopy(T object) {
    try {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(object);
        
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (T) ois.readObject();
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}
```

---

## 🔄 Prototype vs Factory Method

| Aspect | Prototype | Factory Method |
|--------|-----------|----------------|
| **Creation** | Clone existing | Create new |
| **Performance** | Faster (if creation expensive) | Slower |
| **Configuration** | Pre-configured objects | Created from scratch |
| **Complexity** | Cloning logic | Creation logic |

---

## 🔄 Prototype vs Builder

| Aspect | Prototype | Builder |
|--------|-----------|---------|
| **Focus** | Copy existing | Build new |
| **Use Case** | Similar objects | Complex construction |
| **Performance** | Fast cloning | Step-by-step |

---

## 💡 Design Principles

### Follows:
- ✅ **Encapsulation** - hides creation complexity
- ✅ **Dependency Inversion** - client depends on interface

### Issues with Java's Cloneable:
- ❌ Requires `CloneNotSupportedException` handling
- ❌ Shallow copy by default
- ❌ Protected `clone()` method
- ❌ Violates contract if not implemented properly

---

## 🎓 Interview Focus

### Must Know:
1. **Intent** - Clone objects instead of creating new
2. **Shallow vs Deep copy** - Critical difference
3. **When to use** - Expensive object creation
4. **Java's Cloneable** - Issues and alternatives
5. **Prototype registry** - Cache of prototypes

### Common Questions:
1. "Implement Prototype with deep copy"
2. "Shallow vs Deep copy?"
3. "When would you use Prototype?"
4. "Issues with Java's Cloneable?"
5. "Prototype vs Factory Method?"

---

## 📝 Best Practices

### 1. Prefer Copy Constructor
```java
// Better than Cloneable
public Employee(Employee other) {
    this.name = other.name;
    this.department = new Department(other.department);
}
```

### 2. Override clone() Properly
```java
@Override
public Object clone() {
    try {
        return super.clone();
    } catch (CloneNotSupportedException e) {
        throw new AssertionError(); // Can't happen
    }
}
```

### 3. Document Clone Behavior
```java
/**
 * Creates a deep copy of this Employee.
 * Clones all mutable fields including Department.
 */
@Override
public Employee clone() { ... }
```

---

## 📝 Interview Questions & Answers

See [INTERVIEW_QA.md](INTERVIEW_QA.md) for detailed Q&A

---

## 🔗 Related Patterns

- **Abstract Factory** - Can use prototypes instead
- **Composite** - Often uses Prototype
- **Decorator** - Often uses Prototype
- **Memento** - Similar concept (saving state)

---

## 📚 Where It's Used

### In Java:
- `Object.clone()`
- `ArrayList.clone()`
- `HashMap.clone()`
- `LinkedList.clone()`

### In Real World:
- Game development (enemy cloning)
- Document templates
- Configuration management
- Database record copying

---

## 🎯 Key Takeaways

1. **Clone instead of create** new objects
2. **Shallow vs deep** copy is critical
3. **Performance benefit** for expensive objects
4. **Java's Cloneable** has issues
5. **Copy constructor** is better alternative

---

**Complete Example:** [ShapePrototype.java](ShapePrototype.java)  
**Demo:** [PrototypeDemo.java](PrototypeDemo.java)  
**Interview Q&A:** [INTERVIEW_QA.md](INTERVIEW_QA.md)

---

**Next Pattern:** [Adapter →](../06-adapter/)

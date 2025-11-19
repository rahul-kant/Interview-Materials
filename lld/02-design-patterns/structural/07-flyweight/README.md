# Flyweight Pattern ⭐

## 📋 Intent

**Use sharing to support large numbers of fine-grained objects efficiently.**

---

## 🎯 Problem

- Need many similar objects
- Memory consumption is high
- Most object state can be shared
- Want to reduce memory usage

**Example Scenarios:**
- Text editor (character objects)
- Game particles (bullets, trees)
- String pool in Java
- Icon cache in UI

---

## 💻 Implementation

```java
// Flyweight Interface
interface TreeType {
    void draw(int x, int y);
}

// Concrete Flyweight (shared state)
class TreeTypeImpl implements TreeType {
    private String name;
    private String color;
    private String texture;
    
    public TreeTypeImpl(String name, String color, String texture) {
        this.name = name;
        this.color = color;
        this.texture = texture;
    }
    
    public void draw(int x, int y) {
        System.out.println("Drawing " + name + " tree at (" + x + "," + y + ")");
    }
}

// Flyweight Factory
class TreeFactory {
    private static Map<String, TreeType> treeTypes = new HashMap<>();
    
    public static TreeType getTreeType(String name, String color, String texture) {
        String key = name + color + texture;
        TreeType type = treeTypes.get(key);
        
        if (type == null) {
            type = new TreeTypeImpl(name, color, texture);
            treeTypes.put(key, type);
            System.out.println("Creating new tree type: " + name);
        }
        return type;
    }
}

// Context (unshared state)
class Tree {
    private int x, y;
    private TreeType type;
    
    public Tree(int x, int y, TreeType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
    
    public void draw() {
        type.draw(x, y);
    }
}

// Usage
TreeType oakType = TreeFactory.getTreeType("Oak", "Green", "Rough");
TreeType pineType = TreeFactory.getTreeType("Pine", "Dark Green", "Smooth");

Tree tree1 = new Tree(10, 20, oakType);
Tree tree2 = new Tree(30, 40, oakType); // Reuses oakType
Tree tree3 = new Tree(50, 60, pineType);

tree1.draw();
tree2.draw();
tree3.draw();
```

---

## ✅ When to Use

- **Many similar objects** needed
- **Memory is concern** - large number of objects
- **Shared state** - intrinsic state can be shared
- **Extrinsic state** - can be computed or passed

---

## 🎯 Pros and Cons

### Pros ✅
- **Memory savings** - reduced object count
- **Performance** - fewer object creations

### Cons ❌
- **Complexity** - separating shared/unshared state
- **Runtime costs** - extrinsic state computation

---

## 📚 Where It's Used

### In Java:
- `String.intern()` - String pool
- `Integer.valueOf()` - Integer cache (-128 to 127)
- `Boolean.TRUE`, `Boolean.FALSE`

---

## 🎯 Key Takeaways

1. **Share objects** to save memory
2. **Intrinsic state** - shared
3. **Extrinsic state** - passed in
4. **Factory pattern** - manages flyweights

---

**Complete Pattern List:** [Structural Patterns →](../)

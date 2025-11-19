# Visitor Pattern ⭐

## 📋 Intent

**Represent an operation to be performed on elements of an object structure. Visitor lets you define a new operation without changing the classes of the elements on which it operates.**

---

## 💻 Implementation

```java
// Element Interface
interface Shape {
    void accept(Visitor visitor);
}

// Concrete Elements
class Circle implements Shape {
    private int radius;
    
    public Circle(int radius) {
        this.radius = radius;
    }
    
    public int getRadius() {
        return radius;
    }
    
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}

class Rectangle implements Shape {
    private int width, height;
    
    public Rectangle(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}

// Visitor Interface
interface Visitor {
    void visit(Circle circle);
    void visit(Rectangle rectangle);
}

// Concrete Visitors
class AreaCalculator implements Visitor {
    public void visit(Circle circle) {
        double area = Math.PI * circle.getRadius() * circle.getRadius();
        System.out.println("Circle area: " + area);
    }
    
    public void visit(Rectangle rectangle) {
        int area = rectangle.getWidth() * rectangle.getHeight();
        System.out.println("Rectangle area: " + area);
    }
}

// Usage
List<Shape> shapes = Arrays.asList(
    new Circle(5),
    new Rectangle(4, 6)
);

Visitor areaCalculator = new AreaCalculator();
for (Shape shape : shapes) {
    shape.accept(areaCalculator);
}
```

---

## ✅ When to Use

- **Add operations** without modifying classes
- **Many unrelated operations** on object structure
- **Stable class hierarchy**

---

## 🎯 Key Takeaways

1. **Separates algorithm from object structure**
2. **Double dispatch** mechanism
3. **Easy to add operations**
4. **Hard to add new element types**

---

**Next Pattern:** [Interpreter →](../23-interpreter/)

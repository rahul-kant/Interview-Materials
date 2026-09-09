# Liskov Substitution Principle (LSP)

## 📖 Definition

> "Objects of a superclass should be replaceable with objects of a subclass without breaking the application."
> — Barbara Liskov

In simpler terms: **Subtypes must be substitutable for their base types without altering the correctness of the program.**

## 🎯 What Does This Mean?

If class B is a subtype of class A, then objects of class A should be replaceable with objects of class B without:
- Breaking the program
- Changing expected behavior
- Requiring code changes

### The Contract:
- **Preconditions** cannot be strengthened in subclass
- **Postconditions** cannot be weakened in subclass
- **Invariants** must be preserved
- **History constraint** - subclass shouldn't allow states impossible in parent

## ❌ Why Violating LSP is Bad

### Problems:

1. **Unexpected Behavior**
   - Substitution causes bugs
   - Runtime errors
   - Incorrect results

2. **Breaks Polymorphism**
   - Can't use parent class reference
   - Need type checking (instanceof)
   - Defeats purpose of inheritance

3. **Fragile Code**
   - Code breaks when using subclass
   - Need special handling for subtypes
   - Violation of "is-a" relationship

## 💡 Real-World Analogy

Think of vehicles:

**Bad (Violates LSP):**
```
Vehicle (has start() method)
├── Car (start() works normally)
└── BrokenCar (start() throws exception)

Problem: Can't use BrokenCar wherever Vehicle is expected!
```

**Good (Follows LSP):**
```
Vehicle (has start() method)
├── Car (start() works)
└── ElectricCar (start() works differently but still works)

Success: Both can be used interchangeably!
```

## 📝 Classic Example: Rectangle-Square Problem

### Scenario:
In mathematics, a square IS-A rectangle (all squares are rectangles).
But in programming, this creates LSP violation!

### Why It Violates LSP:

```java
// Rectangle can have different width and height
Rectangle r = new Rectangle();
r.setWidth(5);
r.setHeight(10);
assert r.getArea() == 50; // ✓ Works

// Square must have equal width and height
Rectangle s = new Square();
s.setWidth(5);
s.setHeight(10); // Problem: Square changes both!
assert s.getArea() == 50; // ✗ Fails! Area is 100
```

**Problem:** Square changes the behavior expected from Rectangle!

### The Fix:
Don't use inheritance if the subclass changes parent's behavior.
Use composition or separate hierarchies instead.

## 🔍 How to Identify LSP Violations

### Warning Signs:

1. **Type Checking in Client Code**
   ```java
   if (shape instanceof Square) {
       // special handling
   } else if (shape instanceof Rectangle) {
       // different handling
   }
   ```

2. **Empty or Exception-Throwing Overrides**
   ```java
   @Override
   public void fly() {
       throw new UnsupportedOperationException("Penguins can't fly!");
   }
   ```

3. **Strengthened Preconditions**
   ```java
   // Parent: accepts any positive number
   // Child: only accepts numbers > 100
   ```

4. **Weakened Postconditions**
   ```java
   // Parent: always returns non-null
   // Child: sometimes returns null
   ```

## ✅ How to Apply LSP

### Design Guidelines:

1. **Honor the Contract**
   - Follow parent's behavior expectations
   - Don't strengthen preconditions
   - Don't weaken postconditions

2. **Use Composition Over Inheritance**
   - When "is-a" relationship is conceptual but not behavioral
   - Example: Square HAS-A dimension, not IS-A Rectangle

3. **Design by Contract**
   - Clearly document expectations
   - Use assertions
   - Follow interface contracts

4. **Test Substitutability**
   - Write tests using parent type
   - Should work with all subtypes
   - No special cases needed

## 📊 Before and After Comparison

### Before (Bad - Violates LSP):
```
Bird (can fly())
├── Sparrow (flies normally)
├── Eagle (flies normally)
└── Penguin (throws exception!) ❌

Problem: Can't use Penguin wherever Bird is expected!
```

### After (Good - Follows LSP):
```
Bird (abstract)
├── FlyingBird (can fly())
│   ├── Sparrow
│   └── Eagle
└── FlightlessBird (can't fly())
    └── Penguin

Solution: Separate hierarchies based on actual capabilities!
```

## 🎓 Practice Exercise

Identify LSP violation:

```java
class Vehicle {
    public void refuel() {
        // Add fuel
    }
}

class ElectricCar extends Vehicle {
    @Override
    public void refuel() {
        throw new UnsupportedOperationException("Electric cars don't use fuel!");
    }
}
```

### Problems:
1. ElectricCar violates LSP
2. Can't substitute ElectricCar for Vehicle
3. Client code needs special handling

### Solution:
```java
interface Vehicle {
    void recharge();
}

interface FuelVehicle extends Vehicle {
    void refuel();
}

interface ElectricVehicle extends Vehicle {
    void rechargeBattery();
}
```

## 🔑 Key Takeaways

✅ **Subtypes must be substitutable** - Without breaking code
✅ **Honor parent's contract** - Don't change expected behavior
✅ **No type checking needed** - Polymorphism should just work
✅ **Prefer composition** - When inheritance doesn't fit behaviorally
✅ **Design hierarchies carefully** - Based on actual capabilities
✅ **Test with parent type** - Ensure all subtypes work

## 💡 Interview Tips

### Common Questions:

**Q: What is Liskov Substitution Principle?**
A: Subtypes must be substitutable for their base types without altering program correctness. Objects of a superclass should be replaceable with objects of subclass without breaking the application.

**Q: What's the Rectangle-Square problem?**
A: Classic LSP violation. Mathematically square is-a rectangle, but in code, Square changes Rectangle's behavior (maintaining equal sides), violating LSP.

**Q: How do you fix LSP violations?**
A: 
1. Use composition instead of inheritance
2. Create separate hierarchies based on behavior
3. Use interfaces appropriately
4. Honor parent class contracts

**Q: What's the difference between LSP and IS-A relationship?**
A: IS-A is conceptual (square is-a rectangle mathematically). LSP is behavioral (square behaves differently than rectangle in code). LSP requires behavioral substitutability.

**Q: Can you give a real-world example?**
A: Bird hierarchy - not all birds can fly (penguins, ostriches). Solution: Create FlyingBird and FlightlessBird hierarchies instead of making all birds flyable.

## 📁 Code Examples

- `bad-example/` - Shows LSP violations (Rectangle-Square, Bird-Penguin)
- `good-example/` - Shows proper design (separate hierarchies)

Study both to understand the difference!

## ➡️ Next Steps

1. Review the bad example to see violations
2. Study the good example to see the solution
3. Compare the approaches
4. Try refactoring LSP violations
5. Move on to Interface Segregation Principle

---

**Remember**: LSP ensures that inheritance hierarchies are designed correctly based on behavior, not just conceptual relationships!

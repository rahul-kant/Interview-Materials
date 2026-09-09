# Classes and Objects

## 🎯 What You'll Learn
- What are classes and objects
- How to create classes in Java
- Instance variables and methods
- Constructors
- The `this` keyword
- Creating and using objects

## 📖 Theory

### What is a Class?
A **class** is a blueprint or template for creating objects. It defines:
- **Attributes** (data/properties) - what the object knows
- **Methods** (behavior) - what the object can do

Think of a class as a cookie cutter, and objects as the cookies made from it.

### What is an Object?
An **object** is an instance of a class. It's a concrete entity created from the class blueprint.

**Real-World Analogy:**
- Class: "Car" (blueprint)
- Objects: Your Honda Civic, your friend's Tesla, another person's Toyota

## 💻 Example: Student Management System

Let's build a simple system to manage students.

### Step 1: Understanding Requirements
We need to store information about students:
- Name
- Roll Number
- Age
- Grade

We need to perform operations:
- Display student information
- Update grades
- Calculate if student passed

### Step 2: Design the Class

```
Class: Student
Attributes:
- name (String)
- rollNumber (int)
- age (int)
- grade (double)

Methods:
- displayInfo()
- updateGrade(double)
- hasPassed()
```

### Complete Code

See `Student.java` and `StudentDemo.java` for the implementation.

## 🔑 Key Concepts

### 1. Instance Variables
```java
private String name;
private int rollNumber;
```
- Belong to each object
- Each object has its own copy
- Use `private` to implement encapsulation

### 2. Constructor
```java
public Student(String name, int rollNumber, int age, double grade) {
    this.name = name;
    this.rollNumber = rollNumber;
    this.age = age;
    this.grade = grade;
}
```
- Special method to initialize objects
- Same name as class
- No return type
- Called automatically when object is created

### 3. The `this` Keyword
```java
this.name = name;
```
- Refers to the current object
- Used to distinguish between instance variables and parameters
- Makes code clearer

### 4. Methods
```java
public void displayInfo() {
    System.out.println("Name: " + name);
}
```
- Define behavior of objects
- Can access and modify instance variables
- Can be `public`, `private`, or `protected`

### 5. Creating Objects
```java
Student student1 = new Student("Alice", 101, 20, 85.5);
```
- `Student` - Class name (type)
- `student1` - Reference variable
- `new` - Creates object in memory
- `Student(...)` - Constructor call

## ✅ Best Practices

1. **Use meaningful names**: `Student` not `S`, `studentName` not `n`
2. **Keep variables private**: Use getters/setters for access
3. **Single Responsibility**: Each class should have one clear purpose
4. **Constructor overloading**: Provide multiple ways to create objects
5. **Default values**: Initialize variables properly

## 🎓 Practice Exercises

Try implementing these on your own:

1. **Book Class**
   - Attributes: title, author, ISBN, price, pages
   - Methods: displayInfo(), applyDiscount(), isLongBook()

2. **BankAccount Class**
   - Attributes: accountNumber, holderName, balance
   - Methods: deposit(), withdraw(), displayBalance()

3. **Product Class**
   - Attributes: productId, name, price, quantity
   - Methods: updatePrice(), updateQuantity(), getTotalValue()

## 🤔 Common Interview Questions

**Q: What is the difference between a class and an object?**
A: A class is a blueprint/template, while an object is an instance created from that class. One class can create multiple objects.

**Q: What is a constructor?**
A: A special method used to initialize objects. It has the same name as the class and no return type.

**Q: What is the `this` keyword?**
A: It's a reference to the current object. Used to refer to instance variables and methods of the current object.

**Q: Can we have multiple constructors in a class?**
A: Yes! This is called constructor overloading. Each constructor must have a different parameter list.

## 🎯 Key Takeaways

✅ Classes are blueprints; objects are instances
✅ Use constructors to initialize objects
✅ Instance variables store object state
✅ Methods define object behavior
✅ `this` refers to the current object
✅ Each object has its own copy of instance variables
✅ Multiple objects can be created from one class

## ➡️ Next Steps

Once you're comfortable with classes and objects, move on to:
- `02-encapsulation/` - Learn how to protect your data

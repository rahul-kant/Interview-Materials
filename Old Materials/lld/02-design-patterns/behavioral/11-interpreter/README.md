# Interpreter Pattern ⭐

## 📋 Intent

**Given a language, define a representation for its grammar along with an interpreter that uses the representation to interpret sentences in the language.**

---

## 💻 Implementation

```java
// Expression Interface
interface Expression {
    int interpret();
}

// Terminal Expression
class NumberExpression implements Expression {
    private int number;
    
    public NumberExpression(int number) {
        this.number = number;
    }
    
    public int interpret() {
        return number;
    }
}

// Non-terminal Expressions
class AddExpression implements Expression {
    private Expression left, right;
    
    public AddExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }
    
    public int interpret() {
        return left.interpret() + right.interpret();
    }
}

class SubtractExpression implements Expression {
    private Expression left, right;
    
    public SubtractExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }
    
    public int interpret() {
        return left.interpret() - right.interpret();
    }
}

// Usage - Represents: (5 + 3) - 2
Expression five = new NumberExpression(5);
Expression three = new NumberExpression(3);
Expression two = new NumberExpression(2);

Expression sum = new AddExpression(five, three);
Expression result = new SubtractExpression(sum, two);

System.out.println("Result: " + result.interpret()); // 6
```

---

## ✅ When to Use

- **Simple grammar** to interpret
- **Efficiency not critical**
- **Grammar rarely changes**

---

## ❌ When NOT to Use

- Complex grammar (use parser generator)
- Performance critical
- Grammar changes frequently

---

## 🎯 Key Takeaways

1. **Defines grammar** as class hierarchy
2. **Each rule** is a class
3. **Rarely used** in practice
4. **Better alternatives** exist (ANTLR, regex)

---

## 📚 Where It's Used

- `java.util.regex.Pattern`
- SQL parsers
- Expression evaluators
- Configuration language parsers

---

**All Behavioral Patterns Complete!**

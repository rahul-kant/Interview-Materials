# Visitor Pattern ⭐⭐⭐⭐ (Hard)

> Behavioral design pattern. One of the hardest GoF patterns to grasp because it relies on **double dispatch** to simulate multiple dispatch in a single-dispatch language like Java.

## Table of Contents

- [📋 Intent](#-intent)
- [🎯 Problem](#-problem)
- [✅ Solution](#-solution)
- [💻 Implementation](#-implementation)
  - [Variant 1: Naive `instanceof` type-checking](#variant-1-naive-instanceof-type-checking)
  - [Variant 2: Classic double-dispatch Visitor](#variant-2-classic-double-dispatch-visitor)
  - [Variant 3: Generic `Visitor<R>` returning typed results](#variant-3-generic-visitorr-returning-typed-results)
- [🎨 Real-World Example](#-real-world-example)
- [✅ When to Use](#-when-to-use)
- [❌ When NOT to Use](#-when-not-to-use)
- [🎯 Pros and Cons](#-pros-and-cons)
- [🔄 Comparison with Related/Similar Patterns](#-comparison-with-relatedsimilar-patterns)
- [📊 Comparison Table](#-comparison-table)
- [💡 Common Pitfalls](#-common-pitfalls)
- [🎓 Interview Tips](#-interview-tips)
- [🔗 Related Patterns](#-related-patterns)
- [📚 Library/Framework Implementation](#-libraryframework-implementation)
- [📝 Interview Questions & Answers (FAANG Top 20)](#-interview-questions--answers-faang-top-20)
- [⚡ Quick Revision](#-quick-revision)

---

## 📋 Intent

**GoF:** *Represent an operation to be performed on the elements of an object structure. Visitor lets you define a new operation without changing the classes of the elements on which it operates.*

In plain terms: Visitor lets you **add new operations** to a family of object types **without modifying those types**. You pull the operation logic out of the element classes and into a separate "visitor" object. When you want a new operation, you write a new visitor — the element classes stay untouched (Open/Closed Principle with respect to *operations*).

The catch, and the reason it exists at all, is **double dispatch**: Java dispatches methods on the runtime type of *one* object (the receiver). Visitor uses two chained virtual calls (`element.accept(visitor)` → `visitor.visit(this)`) to select behavior based on the runtime types of **both** the element and the visitor.

---

## 🎯 Problem

You have a **stable hierarchy of element classes** but an **open-ended, growing set of operations** you want to perform over them. If you put every operation as a method on each element class, then:

- Every new operation forces you to edit **every** element class (violates OCP, causes merge conflicts, touches well-tested code).
- Unrelated concerns (rendering, serialization, cost calculation, type checking) get **scattered across and tangled into** the element classes, which should ideally be simple data holders.
- Operations that need to accumulate state across the whole structure become awkward.

Concrete scenarios where Visitor shines:

1. **Compilers / AST processing.** An abstract syntax tree has a fixed set of node types (`Literal`, `Add`, `Multiply`, `Variable`). You want many operations over it — evaluate, pretty-print, type-check, optimize, generate bytecode. Each is a visitor; nodes never change.
2. **Document object models.** A document tree (`Paragraph`, `Image`, `Table`) that you must export to HTML, Markdown, PDF, and plain text. Each exporter is a visitor.
3. **Shopping cart / pricing.** A cart of `Book`, `Electronic`, `Groceries` items over which you run tax calculation, shipping-weight calculation, and discount computation — three visitors, one item hierarchy.
4. **File systems.** A tree of `File` and `Directory` nodes; visitors compute total size, build a report, search for viruses, or count nodes.

---

## ✅ Solution

Move each operation into its own **Visitor** object. Give every element an `accept(Visitor v)` method whose *only* job is to call back the visitor's type-specific method, passing itself: `v.visit(this)`. Because `this` is statically typed to the concrete element inside each element class, the compiler picks the correct overload — and because `accept` is virtual, the correct element's `accept` runs first. This two-step callback is **double dispatch**.

Key structural elements:

- **`Visitor` (interface)** — declares one `visit(ConcreteElementX)` overload per concrete element type.
- **`ConcreteVisitor`** — implements the operation for every element type; can carry accumulated state.
- **`Element` (interface)** — declares `accept(Visitor)`.
- **`ConcreteElement`** — implements `accept` as exactly `visitor.visit(this)`.
- **Object structure** — the collection/tree that holds elements and iterates them, calling `accept` on each.

The crux — **double dispatch** — works like this: `element.accept(visitor)` dispatches on the *runtime type of the element* (first dispatch). Inside that element's `accept`, the call `visitor.visit(this)` dispatches on the *runtime type of the visitor* while `this` is statically bound to the concrete element type, so overload resolution picks the right `visit` overload (second dispatch). The net effect: the method executed depends on **both** types, which single-dispatch Java cannot do in one call.

---

## 💻 Implementation

### Variant 1: Naive `instanceof` type-checking

The "obvious" approach without Visitor: one method with an `if/else instanceof` chain (or a `switch` on a type tag).

<details>
<summary>💻 Click to expand code</summary>

```java
// Element hierarchy — simple shapes
interface Shape {}
class Circle implements Shape { double radius = 2; }
class Rectangle implements Shape { double w = 3, h = 4; }
class Triangle implements Shape { double base = 3, height = 6; }

// Operation lives OUTSIDE, but uses runtime type checks
class AreaCalculator {
    double area(Shape s) {
        if (s instanceof Circle) {
            return Math.PI * ((Circle) s).radius * ((Circle) s).radius;
        } else if (s instanceof Rectangle) {
            Rectangle r = (Rectangle) s;
            return r.w * r.h;
        } else if (s instanceof Triangle) {
            Triangle t = (Triangle) s;
            return 0.5 * t.base * t.height;
        }
        throw new IllegalArgumentException("Unknown shape: " + s);
    }
}
```

</details>

**Mechanism:** Manual runtime type discrimination via `instanceof` + downcast — essentially hand-rolled single dispatch on a type tag.

**Pros:**
- Trivial to write for a tiny, throwaway hierarchy.
- All logic for one operation is in one place.

**Cons:**
- **Not type-safe / not exhaustive:** the compiler cannot tell you a case is missing. Add a `Hexagon` and this silently throws at runtime instead of failing to compile.
- **`instanceof` order matters** and downcasts are error-prone.
- Slower than virtual dispatch and scales linearly with the number of types.
- Encourages the exact scattering Visitor is meant to prevent — every operation repeats the same fragile chain.

### Variant 2: Classic double-dispatch Visitor

Introduce `accept()` on elements and a `Visitor` interface with one `visit` per concrete type. The visitor accumulates state via fields since `visit` returns `void`.

<details>
<summary>💻 Click to expand code</summary>

```java
// --- Element hierarchy ---
interface Shape {
    void accept(ShapeVisitor v);   // enables double dispatch
}

class Circle implements Shape {
    final double radius;
    Circle(double radius) { this.radius = radius; }
    @Override public void accept(ShapeVisitor v) { v.visit(this); } // 'this' is Circle -> picks visit(Circle)
}

class Rectangle implements Shape {
    final double w, h;
    Rectangle(double w, double h) { this.w = w; this.h = h; }
    @Override public void accept(ShapeVisitor v) { v.visit(this); }
}

class Triangle implements Shape {
    final double base, height;
    Triangle(double base, double height) { this.base = base; this.height = height; }
    @Override public void accept(ShapeVisitor v) { v.visit(this); }
}

// --- Visitor interface: one overload per concrete element ---
interface ShapeVisitor {
    void visit(Circle c);
    void visit(Rectangle r);
    void visit(Triangle t);
}

// --- A concrete visitor that accumulates total area (state in a field) ---
class AreaVisitor implements ShapeVisitor {
    private double totalArea = 0;
    @Override public void visit(Circle c)    { totalArea += Math.PI * c.radius * c.radius; }
    @Override public void visit(Rectangle r) { totalArea += r.w * r.h; }
    @Override public void visit(Triangle t)  { totalArea += 0.5 * t.base * t.height; }
    public double getTotalArea() { return totalArea; }
}

// --- Usage ---
class Demo {
    public static void main(String[] args) {
        List<Shape> shapes = List.of(new Circle(2), new Rectangle(3, 4), new Triangle(3, 6));
        AreaVisitor av = new AreaVisitor();
        for (Shape s : shapes) s.accept(av);       // double dispatch per element
        System.out.println("Total area = " + av.getTotalArea());
    }
}
```

</details>

**Mechanism — double dispatch:** `s.accept(av)` dispatches virtually on the runtime type of `s` (first dispatch). Inside, `v.visit(this)` resolves the `visit` **overload** at compile time using the static type of `this` (which is the concrete element inside each `accept`), and dispatches virtually on the runtime type of `v` (second dispatch). Behavior therefore depends on both types. This is why you cannot replace `accept` with a single `visitor.visit(shape)` from the outside — there the static type is `Shape`, so overload resolution fails/binds wrong.

**Pros:**
- **Adding a new operation** = write a new visitor. Element classes never change (OCP for operations).
- Type-safe and exhaustive: forgetting to implement `visit(Triangle)` is a compile error.
- Related logic for one operation is co-located in one visitor.

**Cons:**
- **Adding a new element type** = change the `Visitor` interface and *every* existing visitor (the Expression Problem — see pitfalls).
- `void` return forces mutable state in the visitor; awkward for pure functional-style results.
- Boilerplate `accept` in every element.

### Variant 3: Generic `Visitor<R>` returning typed results

Parameterize the visitor by a return type so `visit` can return a value directly — cleaner than mutable state, composable, and functional.

<details>
<summary>💻 Click to expand code</summary>

```java
// --- Element hierarchy uses a generic accept ---
interface Shape {
    <R> R accept(ShapeVisitor<R> v);
}

class Circle implements Shape {
    final double radius;
    Circle(double radius) { this.radius = radius; }
    @Override public <R> R accept(ShapeVisitor<R> v) { return v.visit(this); }
}
class Rectangle implements Shape {
    final double w, h;
    Rectangle(double w, double h) { this.w = w; this.h = h; }
    @Override public <R> R accept(ShapeVisitor<R> v) { return v.visit(this); }
}
class Triangle implements Shape {
    final double base, height;
    Triangle(double base, double height) { this.base = base; this.height = height; }
    @Override public <R> R accept(ShapeVisitor<R> v) { return v.visit(this); }
}

// --- Generic visitor: R is the operation's result type ---
interface ShapeVisitor<R> {
    R visit(Circle c);
    R visit(Rectangle r);
    R visit(Triangle t);
}

// Area as a pure function returning Double — no mutable state
class AreaVisitor implements ShapeVisitor<Double> {
    @Override public Double visit(Circle c)    { return Math.PI * c.radius * c.radius; }
    @Override public Double visit(Rectangle r) { return r.w * r.h; }
    @Override public Double visit(Triangle t)  { return 0.5 * t.base * t.height; }
}

// A second operation reusing the same hierarchy, returning String
class NameVisitor implements ShapeVisitor<String> {
    @Override public String visit(Circle c)    { return "Circle(r=" + c.radius + ")"; }
    @Override public String visit(Rectangle r) { return "Rectangle(" + r.w + "x" + r.h + ")"; }
    @Override public String visit(Triangle t)  { return "Triangle(b=" + t.base + ")"; }
}

class Demo {
    public static void main(String[] args) {
        List<Shape> shapes = List.of(new Circle(2), new Rectangle(3, 4), new Triangle(3, 6));
        AreaVisitor area = new AreaVisitor();
        NameVisitor name = new NameVisitor();
        double total = shapes.stream().mapToDouble(s -> s.accept(area)).sum();
        shapes.forEach(s -> System.out.println(s.accept(name) + " -> " + s.accept(area)));
        System.out.println("Total area = " + total);
    }
}
```

</details>

You can go further and add **`default` methods** (Java 8) on the visitor interface so that new visitors only override the cases they care about, falling back to a default (e.g., return 0 / no-op). This softens the Expression Problem slightly for *visitors* but not for *elements*.

<details>
<summary>💻 Click to expand code (default-method visitor)</summary>

```java
interface ShapeVisitor<R> {
    R defaultValue();                       // fallback
    default R visit(Circle c)    { return defaultValue(); }
    default R visit(Rectangle r) { return defaultValue(); }
    default R visit(Triangle t)  { return defaultValue(); }
}

// Only cares about circles; everything else falls back to 0.0
class CircleAreaOnly implements ShapeVisitor<Double> {
    public Double defaultValue() { return 0.0; }
    @Override public Double visit(Circle c) { return Math.PI * c.radius * c.radius; }
}
```

</details>

**Mechanism:** Same double dispatch, but the generic type parameter `<R>` threads the operation's result type through `accept`, eliminating mutable accumulator fields and enabling stream composition (`map`, `reduce`).

**Pros:**
- No mutable visitor state needed; each `visit` is a pure function → easier to test and parallelize.
- Composable with the Streams API.
- Default methods reduce boilerplate for partial visitors.

**Cons:**
- Generic `accept` signature is slightly more intimidating to read.
- Still fully exposed to the Expression Problem for new element types.
- Default methods trade compile-time exhaustiveness for convenience (you can silently forget a case).

---

## 🎨 Real-World Example

A production-style **arithmetic expression AST** with three independent operations over one node hierarchy: an **evaluator**, a **pretty-printer**, and a **type/validity checker** (here, a "contains division by literal zero" checker). This is the canonical FAANG example because it mirrors real compiler/query-engine work.

<details>
<summary>💻 Click to expand code</summary>

```java
import java.util.*;

// ============ Element hierarchy (stable) ============
interface Expr {
    <R> R accept(ExprVisitor<R> v);
}

final class Num implements Expr {
    final double value;
    Num(double value) { this.value = value; }
    public <R> R accept(ExprVisitor<R> v) { return v.visit(this); }
}
final class Var implements Expr {
    final String name;
    Var(String name) { this.name = name; }
    public <R> R accept(ExprVisitor<R> v) { return v.visit(this); }
}
final class BinOp implements Expr {
    enum Op { ADD, SUB, MUL, DIV }
    final Op op; final Expr left, right;
    BinOp(Op op, Expr left, Expr right) { this.op = op; this.left = left; this.right = right; }
    public <R> R accept(ExprVisitor<R> v) { return v.visit(this); }
}

// ============ Visitor interface ============
interface ExprVisitor<R> {
    R visit(Num n);
    R visit(Var v);
    R visit(BinOp b);
}

// ============ Visitor 1: Evaluate with an environment ============
class EvalVisitor implements ExprVisitor<Double> {
    private final Map<String, Double> env;
    EvalVisitor(Map<String, Double> env) { this.env = env; }
    public Double visit(Num n) { return n.value; }
    public Double visit(Var v) {
        Double val = env.get(v.name);
        if (val == null) throw new IllegalStateException("Unbound variable: " + v.name);
        return val;
    }
    public Double visit(BinOp b) {
        double l = b.left.accept(this);   // recursive double dispatch
        double r = b.right.accept(this);
        switch (b.op) {
            case ADD: return l + r;
            case SUB: return l - r;
            case MUL: return l * r;
            case DIV: return l / r;
            default: throw new AssertionError();
        }
    }
}

// ============ Visitor 2: Pretty-print (fully parenthesized) ============
class PrintVisitor implements ExprVisitor<String> {
    public String visit(Num n) {
        return (n.value == Math.rint(n.value)) ? String.valueOf((long) n.value) : String.valueOf(n.value);
    }
    public String visit(Var v) { return v.name; }
    public String visit(BinOp b) {
        String sym = switch (b.op) { case ADD -> "+"; case SUB -> "-"; case MUL -> "*"; case DIV -> "/"; };
        return "(" + b.left.accept(this) + " " + sym + " " + b.right.accept(this) + ")";
    }
}

// ============ Visitor 3: Validity check (division by literal zero) ============
class DivByZeroCheck implements ExprVisitor<Boolean> {
    // returns true if the sub-expression is "safe"
    public Boolean visit(Num n) { return true; }
    public Boolean visit(Var v) { return true; }
    public Boolean visit(BinOp b) {
        boolean safe = b.left.accept(this) && b.right.accept(this);
        if (b.op == BinOp.Op.DIV && b.right instanceof Num && ((Num) b.right).value == 0.0) {
            return false;
        }
        return safe;
    }
}

// ============ Demo / usage ============
public class ExprDemo {
    public static void main(String[] args) {
        // Expression: (x * 3) + (10 / 2)
        Expr e = new BinOp(BinOp.Op.ADD,
                    new BinOp(BinOp.Op.MUL, new Var("x"), new Num(3)),
                    new BinOp(BinOp.Op.DIV, new Num(10), new Num(2)));

        System.out.println("Pretty : " + e.accept(new PrintVisitor()));          // ((x * 3) + (10 / 2))
        System.out.println("Value  : " + e.accept(new EvalVisitor(Map.of("x", 4.0)))); // 17.0
        System.out.println("Safe?  : " + e.accept(new DivByZeroCheck()));         // true

        // A broken expression: 5 / 0
        Expr bad = new BinOp(BinOp.Op.DIV, new Num(5), new Num(0));
        System.out.println("Bad safe? : " + bad.accept(new DivByZeroCheck()));    // false
    }
}
```

</details>

The point: three completely different operations (`Double`, `String`, `Boolean` results) live in three cohesive classes, the `Expr` hierarchy never changed, and adding a fourth operation (say, an optimizer that folds constants) means writing one more visitor.

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- The **object structure is stable** (element types rarely change) but you expect to keep **adding new operations** over it.
- You have **many distinct, unrelated operations** over the same hierarchy and don't want to pollute element classes with all of them.
- You need to run an operation that **accumulates state** across a whole structure (e.g., total size, report generation).
- The elements form a **tree/composite** and you want to keep traversal logic separate from per-node operations (Visitor + Composite is a classic combo — e.g., ASTs, file systems, scene graphs).
- You want operation logic **type-safe and exhaustive** (compiler forces you to handle every element type).

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- The **element hierarchy changes frequently** — every new element type breaks the visitor interface and all concrete visitors (Expression Problem). Prefer polymorphic methods on the elements themselves.
- You have **few operations** and they naturally belong on the element (e.g., `render()` on a UI widget). Visitor is over-engineering here.
- The set of element types is genuinely open/extensible by third parties — you can't force them to update your visitor interface.
- You'd need the visitor to access **private internals** of elements it shouldn't see (Visitor can force you to widen element APIs, breaking encapsulation).
- Modern Java alternative fits better: **sealed interfaces + `switch` pattern matching** give exhaustiveness without the double-dispatch boilerplate when you control the hierarchy.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**
- **Open/Closed for operations:** add operations without touching element classes.
- **Single Responsibility:** each visitor bundles one coherent operation; elements stay lean.
- **Type-safe & exhaustive** (with the classic interface form) — missing a case fails at compile time.
- **State accumulation** across a structure is natural (visitor fields).
- Keeps unrelated concerns out of the domain model.

**Cons**
- **Expression Problem:** adding a new element type forces edits to the visitor interface and every visitor.
- **Breaks encapsulation:** visitors often need access to element internals, pushing you toward public getters.
- **Boilerplate:** every element needs `accept`; every visitor implements every case.
- **Harder to read:** double dispatch is non-obvious to newcomers.
- **Tight coupling** between the visitor interface and the concrete element set.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

| Pattern | What it does | vs Visitor |
|---|---|---|
| **Strategy** | Encapsulates one interchangeable algorithm behind an interface | Strategy varies *one* algorithm at *one* call site; Visitor applies an operation across a *whole hierarchy* using double dispatch. |
| **Iterator** | Sequentially accesses elements of a collection | Iterator only *traverses*; Visitor *operates* on each element type-specifically. Often used together (iterate, then `accept`). |
| **Composite** | Tree of part-whole objects treated uniformly | Composite is the *structure*; Visitor is the *operation* over that structure. They pair constantly (traverse composite, run visitor). |
| **Interpreter** | Evaluates sentences of a grammar via an AST | Interpreter puts `interpret()` on each node (polymorphism). Visitor externalizes operations so you can add many (eval, print, optimize) without editing nodes. |
| **Sealed interfaces + pattern-matching `switch`** (Java 17+/21) | Exhaustive matching over a closed type set | Achieves Visitor's exhaustiveness/adding-operations benefit with far less boilerplate — but only when *you* own a sealed hierarchy. |

</details>

## 📊 Comparison Table

<details>
<summary>📖 Click to expand</summary>

| Axis | Variant 1: `instanceof` | Variant 2: Classic Visitor (void) | Variant 3: Generic `Visitor<R>` |
|---|---|---|---|
| Adding a new **operation** | Easy (new method with chain) | Easy (new visitor class) | Easy (new visitor class) |
| Adding a new **element type** | Edit every chain (runtime failure risk) | Edit interface + all visitors (compile error) | Edit interface + all visitors (compile error) |
| Compile-time **exhaustiveness** | ❌ No | ✅ Yes | ✅ Yes (unless default methods used) |
| **Type safety** | ❌ downcasts | ✅ | ✅ (generics) |
| Uses **double dispatch** | ❌ (manual single) | ✅ | ✅ |
| **Return values** | Direct | Via mutable state | Direct, typed |
| **Functional / stream-friendly** | Poor | Poor | ✅ Good |
| Boilerplate | Low | High (`accept` everywhere) | High |
| Best for | Throwaway/tiny | Classic, teaching, stateful accumulation | Modern production, composable results |

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

**Pitfall 1 — The Expression Problem: adding element types is expensive.**
Visitor makes *operations* easy to add but *element types* hard. Add one element and every visitor breaks.

*What goes wrong:*

<details>
<summary>💻 Failure snippet</summary>

```java
// Add a new element...
class Pentagon implements Shape {
    public <R> R accept(ShapeVisitor<R> v) { return v.visit(this); } // compile error: no visit(Pentagon)
}
// ...and now ShapeVisitor and EVERY implementing class must add visit(Pentagon).
```

</details>

*The fix:* Accept the trade-off consciously — use Visitor only when the element set is stable. If elements change more than operations, invert: put operations as polymorphic methods on elements, or use sealed types + pattern matching.

<details>
<summary>💻 Fix snippet</summary>

```java
// Modern Java: exhaustive switch, no accept() boilerplate, adding an op is a new method,
// adding an element makes the compiler flag every switch that isn't exhaustive.
sealed interface Shape permits Circle, Rectangle, Triangle {}
record Circle(double radius) implements Shape {}
record Rectangle(double w, double h) implements Shape {}
record Triangle(double base, double height) implements Shape {}

static double area(Shape s) {
    return switch (s) {                       // exhaustive; compiler enforces all cases
        case Circle c    -> Math.PI * c.radius() * c.radius();
        case Rectangle r -> r.w() * r.h();
        case Triangle t  -> 0.5 * t.base() * t.height();
    };
}
```

</details>

**Pitfall 2 — Forgetting to implement `accept` correctly (or at all).**
Copy-pasting `accept` between elements and forgetting to update `this` breaks double dispatch silently.

<details>
<summary>💻 Failure snippet</summary>

```java
class Rectangle implements Shape {
    // BUG: copied from Circle, still constructs/visits as if Circle
    public void accept(ShapeVisitor v) { v.visit(new Circle(0)); } // wrong element dispatched!
}
```

</details>

*The fix:* `accept` must be **exactly** `visitor.visit(this)` — nothing else. Keep it a one-liner and review it.

<details>
<summary>💻 Fix snippet</summary>

```java
class Rectangle implements Shape {
    public void accept(ShapeVisitor v) { v.visit(this); } // 'this' is Rectangle -> visit(Rectangle)
}
```

</details>

**Pitfall 3 — Trying to dispatch from outside instead of via `accept` (breaking double dispatch).**
People try `visitor.visit(shape)` directly and are surprised it won't compile or binds to the wrong overload.

<details>
<summary>💻 Failure snippet</summary>

```java
Shape s = new Circle(2);
visitor.visit(s); // static type is Shape -> no visit(Shape) overload -> compile error
```

</details>

*The fix:* Always go through `accept`. The first virtual call on the element is what recovers the concrete type.

<details>
<summary>💻 Fix snippet</summary>

```java
Shape s = new Circle(2);
s.accept(visitor);   // element.accept dispatches on Circle, then calls visit(Circle)
```

</details>

**Pitfall 4 — Broken encapsulation.**
To do its work, a visitor often needs element internals, so you expose public getters/fields you'd rather keep private.

*The fix:* Give elements a **minimal, intention-revealing API** the visitor needs (not raw fields), or keep the visitor as a static nested class / same package to limit exposure. Decide deliberately what to expose.

</details>

## 🎓 Interview Tips

**What interviewers commonly ask**

<details>
<summary>📖 Click to expand</summary>

- "Explain **double dispatch** and why Java needs `accept()` to achieve it." (This is *the* question.)
- "What is the **Expression Problem** and how does Visitor sit on it?" (Easy to add operations, hard to add types.)
- "Implement a Visitor for an AST / file system / shopping cart." (Live-code.)
- "When would you **not** use Visitor?" (Changing hierarchies, few operations, broken encapsulation.)
- "How would modern Java (**sealed types + pattern matching**) change your answer?"

</details>

**What to proactively mention (even if not asked)**

<details>
<summary>📖 Click to expand</summary>

- Name the **double dispatch mechanism** explicitly and trace the two calls — this instantly signals depth.
- Bring up the **Expression Problem** and the operations-easy/types-hard trade-off; contrast with putting methods on elements.
- Mention the **sealed interface + `switch` pattern matching** alternative in Java 17/21 as the modern replacement when you own the hierarchy.
- Note the **encapsulation cost** and how you'd mitigate it.
- Mention real usages (`Files.walkFileTree`, annotation processing `ElementVisitor`, ASM/JavaParser) to show you've seen it in the wild.

</details>

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Composite** — Visitor almost always operates over a Composite tree (AST, DOM, file system).
- **Iterator** — used to traverse the structure and call `accept` on each element.
- **Interpreter** — an AST is often built for Interpreter; a Visitor can add operations (optimize, print) over that same AST without bloating each node's `interpret()`.
- **Strategy** — a visitor is conceptually a "strategy" applied across a whole hierarchy via double dispatch.
- **Acyclic Visitor** (Robert Martin's variant) — breaks the cyclic dependency between visitor and elements using per-element visitor interfaces + `instanceof`, trading exhaustiveness for looser coupling.

</details>

## 📚 Library/Framework Implementation

**1. `java.nio.file.FileVisitor` / `Files.walkFileTree`.** The JDK's canonical Visitor. You implement `FileVisitor` (or extend `SimpleFileVisitor`) with `visitFile`, `preVisitDirectory`, `postVisitDirectory`, `visitFileFailed`, and `walkFileTree` traverses the tree calling them, letting you steer with `FileVisitResult` (CONTINUE/SKIP_SUBTREE/TERMINATE).

<details>
<summary>💻 Click to expand code</summary>

```java
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;

long[] totalBytes = {0};
Files.walkFileTree(Paths.get("/tmp/project"), new SimpleFileVisitor<>() {
    @Override public FileVisitResult visitFile(Path f, BasicFileAttributes a) {
        totalBytes[0] += a.size();
        return FileVisitResult.CONTINUE;
    }
    @Override public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes a) {
        if (d.getFileName().toString().equals(".git")) return FileVisitResult.SKIP_SUBTREE;
        return FileVisitResult.CONTINUE;
    }
});
System.out.println("Total = " + totalBytes[0] + " bytes");
```

</details>

**2. `javax.lang.model.element.ElementVisitor` (annotation processing).** The `javac` annotation-processing API models program elements (`TypeElement`, `ExecutableElement`, `VariableElement`) and provides `ElementVisitor` / `SimpleElementVisitor` so annotation processors can operate over compiled program structure — a textbook Visitor over a stable element hierarchy.

<details>
<summary>💻 Click to expand code</summary>

```java
import javax.lang.model.element.*;
import javax.lang.model.util.SimpleElementVisitor14;

ElementVisitor<String, Void> namePrinter = new SimpleElementVisitor14<>() {
    @Override public String visitType(TypeElement e, Void p)      { return "class " + e.getSimpleName(); }
    @Override public String visitExecutable(ExecutableElement e, Void p) { return "method " + e.getSimpleName(); }
    @Override protected String defaultAction(Element e, Void p)   { return "element " + e.getSimpleName(); }
};
// element.accept(namePrinter, null);  // double dispatch inside the compiler
```

</details>

**3. ASM / JavaParser / Eclipse JDT AST.** Bytecode and source-code tooling lean heavily on Visitor. ASM's `ClassVisitor`/`MethodVisitor` stream class-file structure through visitor callbacks; JavaParser and JDT expose `VoidVisitor`/`GenericVisitor` and `ASTVisitor` to walk and transform source ASTs. (Spring's `BeanDefinitionVisitor` similarly visits/transforms bean-definition metadata.)

<details>
<summary>💻 Click to expand code</summary>

```java
// JavaParser: collect all method names via a visitor
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

new VoidVisitorAdapter<List<String>>() {
    @Override public void visit(MethodDeclaration md, List<String> out) {
        super.visit(md, out);
        out.add(md.getNameAsString());
    }
}.visit(compilationUnit, methodNames);
```

</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1 [Conceptual]: What problem does the Visitor pattern solve?</strong></summary>

Visitor lets you add new operations to a fixed family of object types without modifying those types. Instead of scattering many operations (render, evaluate, serialize, validate) across the element classes — which violates the Open/Closed Principle and Single Responsibility — you extract each operation into its own visitor object. Each visitor declares a `visit` method per concrete element type. Elements expose `accept(visitor)`, which calls back `visitor.visit(this)`. The result: adding an operation means writing one new class, and the element hierarchy never changes. It's the standard answer when you have a stable structure but an open-ended set of operations over it, e.g., an AST processed by an evaluator, printer, and optimizer.

</details>

<details>
<summary><strong>Q2 [Conceptual]: Explain double dispatch and why Visitor needs it.</strong></summary>

Java uses **single dispatch**: a virtual method call selects the implementation based on the runtime type of exactly one object (the receiver). Overload resolution, by contrast, happens at **compile time** based on static argument types. Visitor needs behavior to depend on *two* runtime types — the element and the visitor. It simulates this with two chained calls:

1. `element.accept(visitor)` — virtual dispatch on the element's runtime type (first dispatch).
2. Inside that element's `accept`, `visitor.visit(this)` — `this` is statically the concrete element type, so the compiler picks the right overload, and the call dispatches virtually on the visitor's runtime type (second dispatch).

The composition of the two selects a method based on both types, which a single call in Java cannot do.

</details>

<details>
<summary><strong>Q3 [Conceptual]: What is the "Expression Problem" and how does Visitor relate to it?</strong></summary>

The Expression Problem asks: can you add both new *types* and new *operations* to a data abstraction without modifying existing code and while keeping type safety? Two axes:

- **OO with methods on classes:** adding a new type is easy (new subclass), adding a new operation is hard (edit every class).
- **Visitor:** the reverse — adding a new operation is easy (new visitor), adding a new type is hard (edit the visitor interface + every visitor).

So Visitor doesn't *solve* the Expression Problem; it *flips* which axis is cheap. You choose Visitor when operations change more than types.

</details>

<details>
<summary><strong>Q4 [Conceptual]: What are the participants in the Visitor pattern?</strong></summary>

Five roles: (1) **Visitor** interface — one `visit(ConcreteElementX)` per concrete element; (2) **ConcreteVisitor** — implements the operation for each element and may hold accumulated state; (3) **Element** interface — declares `accept(Visitor)`; (4) **ConcreteElement** — implements `accept` as `visitor.visit(this)`; (5) **Object Structure** — the collection/tree holding elements, which iterates and calls `accept`. The Visitor and ConcreteElement set are tightly coupled: the Visitor interface enumerates every concrete element type.

</details>

<details>
<summary><strong>Q5 [Conceptual]: How does Visitor differ from simply putting methods on the element classes?</strong></summary>

Putting methods on elements (the "polymorphic" approach) keeps behavior local and makes adding a *type* trivial, but every new *operation* requires editing all element classes, and unrelated concerns pile up inside them. Visitor externalizes operations so each is cohesive and independently addable, and elements stay lean. The trade-off is coupling and the Expression Problem inversion. Rule of thumb: methods-on-elements when the operation set is small/stable and belongs semantically to the object; Visitor when operations are numerous, cross-cutting, and evolving over a stable type set.

</details>

<details>
<summary><strong>Q6 [Implementation]: Implement a basic Visitor for a shape hierarchy computing area.</strong></summary>

<details>
<summary>💻 Solution</summary>

```java
interface Shape { void accept(ShapeVisitor v); }
class Circle implements Shape { double r; Circle(double r){this.r=r;} public void accept(ShapeVisitor v){v.visit(this);} }
class Square implements Shape { double s; Square(double s){this.s=s;} public void accept(ShapeVisitor v){v.visit(this);} }

interface ShapeVisitor { void visit(Circle c); void visit(Square s); }

class AreaVisitor implements ShapeVisitor {
    double total = 0;
    public void visit(Circle c){ total += Math.PI*c.r*c.r; }
    public void visit(Square s){ total += s.s*s.s; }
}

// usage
List<Shape> shapes = List.of(new Circle(1), new Square(2));
AreaVisitor v = new AreaVisitor();
shapes.forEach(s -> s.accept(v));
System.out.println(v.total);
```

</details>

The key line is `accept(v){ v.visit(this); }` in each element — that's where double dispatch happens.

</details>

<details>
<summary><strong>Q7 [Implementation]: Refactor the void-returning visitor into a generic one that returns results.</strong></summary>

<details>
<summary>💻 Solution</summary>

```java
interface Shape { <R> R accept(ShapeVisitor<R> v); }
class Circle implements Shape { double r; Circle(double r){this.r=r;} public <R> R accept(ShapeVisitor<R> v){return v.visit(this);} }
class Square implements Shape { double s; Square(double s){this.s=s;} public <R> R accept(ShapeVisitor<R> v){return v.visit(this);} }

interface ShapeVisitor<R> { R visit(Circle c); R visit(Square s); }

class AreaVisitor implements ShapeVisitor<Double> {
    public Double visit(Circle c){ return Math.PI*c.r*c.r; }
    public Double visit(Square s){ return (double)(s.s*s.s); }
}

double total = List.of(new Circle(1), new Square(2)).stream()
    .mapToDouble(s -> s.accept(new AreaVisitor())).sum();
```

</details>

Benefit: no mutable accumulator; each `visit` is pure and stream-composable.

</details>

<details>
<summary><strong>Q8 [Implementation]: How do you handle recursive structures (a tree) with Visitor?</strong></summary>

The visitor recurses by calling `accept(this)` on children inside the composite node's `visit`. For example, in an AST `visit(BinOp b)` calls `b.left.accept(this)` and `b.right.accept(this)`. This keeps traversal logic inside the visitor (so different visitors can traverse differently — pre-order, short-circuit, etc.), or you can put a fixed traversal in the element's `accept` and let the visitor only handle per-node work.

<details>
<summary>💻 Snippet</summary>

```java
public Integer visit(BinOp b) {         // count nodes in a subtree
    return 1 + b.left.accept(this) + b.right.accept(this);
}
public Integer visit(Num n) { return 1; }
public Integer visit(Var v) { return 1; }
```

</details>

</details>

<details>
<summary><strong>Q9 [Implementation]: Add a second, unrelated operation to an existing element hierarchy — show it costs zero element changes.</strong></summary>

Given the shape hierarchy with `accept`, adding a "perimeter" operation is just a new visitor:

<details>
<summary>💻 Solution</summary>

```java
class PerimeterVisitor implements ShapeVisitor<Double> {
    public Double visit(Circle c){ return 2*Math.PI*c.r; }
    public Double visit(Square s){ return 4.0*s.s; }
}
double perimeter = shape.accept(new PerimeterVisitor());
```

</details>

No element class changed — that's the core selling point of Visitor (OCP for operations).

</details>

<details>
<summary><strong>Q10 [Implementation]: Show how default methods reduce visitor boilerplate.</strong></summary>

Declare defaults on the visitor interface so a concrete visitor only overrides cases it cares about:

<details>
<summary>💻 Solution</summary>

```java
interface ShapeVisitor<R> {
    R defaultValue();
    default R visit(Circle c){ return defaultValue(); }
    default R visit(Square s){ return defaultValue(); }
}
class CircleCounter implements ShapeVisitor<Integer> {
    public Integer defaultValue(){ return 0; }
    public Integer visit(Circle c){ return 1; }   // only cares about circles
}
```

</details>

Trade-off: you lose compile-time exhaustiveness — forgetting a case now silently returns the default instead of failing to compile.

</details>

<details>
<summary><strong>Q11 [Breaking]: What breaks when you add a new element type to a Visitor hierarchy?</strong></summary>

Adding a type (e.g., `Pentagon`) forces you to add `visit(Pentagon)` to the `Visitor` interface, which in turn breaks the compilation of *every* existing concrete visitor until each implements it. In a large codebase or a published API, that's a breaking change for all consumers. This is the Expression Problem in action. Mitigations: use `default` methods so old visitors keep compiling (at the cost of exhaustiveness), use an Acyclic Visitor, or reconsider whether Visitor is the right pattern for a growing hierarchy.

</details>

<details>
<summary><strong>Q12 [Breaking]: Someone writes `visitor.visit(shape)` directly and it won't compile. Why?</strong></summary>

Because overload resolution is done at **compile time** on the **static** type. `shape` is declared as `Shape`, and the visitor has no `visit(Shape)` overload — only `visit(Circle)`, `visit(Square)`, etc. So the compiler can't pick one and fails (or, if a `visit(Shape)` existed, it would always bind to *that* regardless of runtime type). The `accept` indirection exists precisely to recover the concrete type via the first virtual dispatch before the overload is resolved.

</details>

<details>
<summary><strong>Q13 [Breaking]: How can a copy-paste error silently break double dispatch?</strong></summary>

If a developer copies `accept` from one element to another and doesn't adjust it — e.g., leaves `v.visit(someOtherObject)` or hardcodes a wrong instance — the wrong overload runs and the bug is silent because it still compiles. The invariant is that `accept` in class `X` must be *exactly* `visitor.visit(this)` so that `this`'s static type is `X`. Enforce via code review, keep `accept` a strict one-liner, and consider a test that asserts each element routes to its matching `visit`.

</details>

<details>
<summary><strong>Q14 [Breaking]: How does Visitor threaten encapsulation, and how do you contain it?</strong></summary>

To compute their result, visitors typically need element data, tempting you to expose public getters or fields, leaking internals that should be private. This couples external visitors to the element's representation. Containment strategies: expose a **minimal, behavior-oriented API** rather than raw state; keep visitors in the **same package** or as **static nested classes** of the element to use package-private access; or have elements pass exactly the data the visitor needs into `visit` rather than exposing themselves wholesale.

</details>

<details>
<summary><strong>Q15 [Trade-off]: Visitor vs Strategy — when do you pick which?</strong></summary>

Strategy encapsulates a single interchangeable algorithm behind an interface and is selected at one call site (e.g., a `Comparator`, a compression algorithm). Visitor applies a *type-dependent* operation across an entire hierarchy using double dispatch. Use Strategy when you're varying one algorithm over one type; use Visitor when the operation must branch on many element types and you want to add whole operations without editing those types. A visitor is essentially "a strategy that knows how to handle every node type in a structure."

</details>

<details>
<summary><strong>Q16 [Trade-off]: Visitor vs Iterator — are they competitors?</strong></summary>

No — they're complementary. Iterator gives you sequential access to elements but treats them uniformly (no type-specific behavior). Visitor provides type-specific operations but doesn't itself define traversal. In practice you iterate the structure and call `accept(visitor)` on each element, or the visitor recurses through a composite. Iterator answers "how do I reach each element?"; Visitor answers "what do I do with each element based on its type?".

</details>

<details>
<summary><strong>Q17 [Trade-off]: Visitor vs sealed interfaces + pattern-matching switch (modern Java) — which is better?</strong></summary>

If you own the hierarchy and can seal it, `sealed interface` + `switch` pattern matching (Java 17 preview, 21 stable) gives you exhaustiveness (compiler flags missing cases when you add a permitted type — the *opposite* of Visitor's silent breakage), no `accept` boilerplate, and inline results. It's usually the better modern choice for closed hierarchies. Classic Visitor still wins when: you can't seal the type set, you want to ship operations as pluggable objects/strategies, you need to carry rich accumulated state, or you're on older Java. Mentioning this trade-off signals up-to-date knowledge in interviews.

</details>

<details>
<summary><strong>Q18 [Advanced]: What is the Acyclic Visitor and what problem does it solve?</strong></summary>

The classic Visitor has a **cyclic dependency**: elements depend on the Visitor interface, and the Visitor interface depends on every concrete element. Adding an element ripples everywhere. Robert Martin's **Acyclic Visitor** breaks the cycle: the base `Visitor` is an empty marker interface, and each element defines its *own* small visitor interface (e.g., `CircleVisitor { void visit(Circle); }`). In `accept`, the element checks `if (v instanceof CircleVisitor)` before calling. This lets you add elements and partial visitors without touching unrelated code, at the cost of runtime `instanceof` checks and losing compile-time exhaustiveness.

<details>
<summary>💻 Snippet</summary>

```java
interface Visitor {}                               // marker
interface CircleVisitor extends Visitor { void visit(Circle c); }
class Circle implements Shape {
    public void accept(Visitor v){ if (v instanceof CircleVisitor cv) cv.visit(this); }
}
```

</details>

</details>

<details>
<summary><strong>Q19 [Advanced]: Compare the performance of double dispatch vs a single virtual call or an instanceof chain.</strong></summary>

Double dispatch is two virtual calls (`accept` then `visit`). Each virtual call is a cheap vtable lookup, well-optimized by the JIT, often inlined when the receiver type is monomorphic/bimorphic at a call site. An `instanceof` chain is O(k) in the number of types and defeats good branch prediction as k grows; a `switch` on a type tag or sealed pattern-matching compiles to a table/hash and is typically faster than a long `instanceof` chain. In practice the difference is negligible for most workloads; correctness, extensibility, and readability dominate the decision. For extreme hot paths (e.g., interpreters), people sometimes replace Visitor with generated code or bytecode compilation.

</details>

<details>
<summary><strong>Q20 [Advanced]: How would you use Visitor in a plugin architecture where third parties add operations?</strong></summary>

Visitor is ideal when *operations* are the extension point and the *element* set is controlled by you (the platform). You publish a stable element hierarchy and the `Visitor` interface; plugins ship new `ConcreteVisitor` implementations without recompiling your core — add operations freely. The moment third parties need to add *element types*, Visitor becomes the wrong choice (they'd have to force everyone to update the visitor interface), and you'd switch to element-side polymorphism or an Acyclic Visitor. So: platform owns types, plugins own operations → Visitor fits perfectly.

</details>

### Coding Challenge 1 (full solution): File-system tree — total size + report

<details>
<summary><strong>Challenge: Model a file system of files and directories. Write visitors that (a) compute total size and (b) produce an indented listing report.</strong></summary>

<details>
<summary>💻 Full solution</summary>

```java
import java.util.*;

interface FsNode { <R> R accept(FsVisitor<R> v); }

final class FileNode implements FsNode {
    final String name; final long size;
    FileNode(String name, long size){ this.name=name; this.size=size; }
    public <R> R accept(FsVisitor<R> v){ return v.visit(this); }
}
final class DirNode implements FsNode {
    final String name; final List<FsNode> children = new ArrayList<>();
    DirNode(String name){ this.name=name; }
    DirNode add(FsNode n){ children.add(n); return this; }
    public <R> R accept(FsVisitor<R> v){ return v.visit(this); }
}

interface FsVisitor<R> { R visit(FileNode f); R visit(DirNode d); }

// (a) total size — recurse through directories
class SizeVisitor implements FsVisitor<Long> {
    public Long visit(FileNode f){ return f.size; }
    public Long visit(DirNode d){
        long sum = 0;
        for (FsNode c : d.children) sum += c.accept(this);
        return sum;
    }
}

// (b) indented report — carries depth via a small stateful visitor
class ReportVisitor implements FsVisitor<String> {
    private int depth = 0;
    public String visit(FileNode f){ return "  ".repeat(depth) + f.name + " (" + f.size + "B)\n"; }
    public String visit(DirNode d){
        StringBuilder sb = new StringBuilder("  ".repeat(depth)).append(d.name).append("/\n");
        depth++;
        for (FsNode c : d.children) sb.append(c.accept(this));
        depth--;
        return sb.toString();
    }
}

public class FsDemo {
    public static void main(String[] args){
        DirNode root = new DirNode("root")
            .add(new FileNode("a.txt", 100))
            .add(new DirNode("sub")
                .add(new FileNode("b.txt", 200))
                .add(new FileNode("c.txt", 50)));
        System.out.println("Total: " + root.accept(new SizeVisitor()) + "B");  // 350B
        System.out.print(root.accept(new ReportVisitor()));
        // root/
        //   a.txt (100B)
        //   sub/
        //     b.txt (200B)
        //     c.txt (50B)
    }
}
```

</details>

</details>

### Coding Challenge 2 (full solution): Shopping cart — tax and shipping-weight visitors

<details>
<summary><strong>Challenge: A cart holds Book, Electronic, and Food items. Compute total tax (different rates per category) and total shipping weight with one visitor each.</strong></summary>

<details>
<summary>💻 Full solution</summary>

```java
import java.util.*;

interface Item { <R> R accept(ItemVisitor<R> v); }

final class Book implements Item {
    final double price; final double weightKg;
    Book(double p, double w){ price=p; weightKg=w; }
    public <R> R accept(ItemVisitor<R> v){ return v.visit(this); }
}
final class Electronic implements Item {
    final double price; final double weightKg;
    Electronic(double p, double w){ price=p; weightKg=w; }
    public <R> R accept(ItemVisitor<R> v){ return v.visit(this); }
}
final class Food implements Item {
    final double price; final double weightKg;
    Food(double p, double w){ price=p; weightKg=w; }
    public <R> R accept(ItemVisitor<R> v){ return v.visit(this); }
}

interface ItemVisitor<R> { R visit(Book b); R visit(Electronic e); R visit(Food f); }

// Tax: books 0%, electronics 18%, food 5%
class TaxVisitor implements ItemVisitor<Double> {
    public Double visit(Book b){ return 0.0; }
    public Double visit(Electronic e){ return e.price * 0.18; }
    public Double visit(Food f){ return f.price * 0.05; }
}
// Shipping weight (kg), just sums each item's weight
class WeightVisitor implements ItemVisitor<Double> {
    public Double visit(Book b){ return b.weightKg; }
    public Double visit(Electronic e){ return e.weightKg; }
    public Double visit(Food f){ return f.weightKg; }
}

public class CartDemo {
    public static void main(String[] args){
        List<Item> cart = List.of(new Book(20, 0.5), new Electronic(1000, 2.0), new Food(50, 1.2));
        TaxVisitor tax = new TaxVisitor();
        WeightVisitor wt = new WeightVisitor();
        double totalTax = cart.stream().mapToDouble(i -> i.accept(tax)).sum();     // 182.5
        double totalWeight = cart.stream().mapToDouble(i -> i.accept(wt)).sum();   // 3.7
        System.out.printf("Tax=%.2f  Weight=%.1fkg%n", totalTax, totalWeight);
    }
}
```

</details>

Adding a "discount" or "insurance-cost" operation later is just another `ItemVisitor` — no item class touched.

</details>

### Staff / Principal Engineer Level

<details>
<summary><strong>SP1 [Staff]: Discuss the Expression Problem trade-offs at an architectural level and how you'd decide between Visitor, element-polymorphism, and sealed+pattern-matching for a long-lived system.</strong></summary>

The decision hinges on **which axis changes faster** and **who owns extension**. If element types are stable and operations proliferate (compilers, query planners, document exporters), Visitor keeps the core closed while operations grow — good. If element types churn (a domain model that keeps gaining subtypes), Visitor's ripple cost is punishing; put behavior on elements or use sealed+switch so the compiler pinpoints every incomplete operation when a type is added. Ownership matters too: if a platform owns types and third parties add operations, publish a stable Visitor interface. If third parties add *types*, Visitor is untenable — no one can force a global visitor-interface update. Modern languages (Scala, Kotlin, Rust, Java 21) increasingly resolve this with sealed hierarchies + exhaustive pattern matching, which is the pragmatic default for closed hierarchies you control; reserve Visitor for when you need operations as first-class pluggable objects with rich state.

</details>

<details>
<summary><strong>SP2 [Staff]: How does Visitor interact with immutability and thread-safety at scale?</strong></summary>

The generic `Visitor<R>` form with pure `visit` methods and no visitor state is naturally thread-safe and lets you run the same visitor over disjoint subtrees in parallel (fork/join, parallel streams) — the elements are read-only. Stateful visitors (accumulator fields, depth counters) are **not** thread-safe and must not be shared across threads; give each thread its own instance or make them return partial results that you reduce. For parallel tree processing, prefer the functional form and a commutative/associative reduction (like `SizeVisitor` returning `Long`) so partial results combine correctly. Beware visitors that mutate the elements themselves (optimizers/rewriters) — those need either immutable elements returning new trees or explicit synchronization.

</details>

<details>
<summary><strong>SP3 [Staff]: In an interpreter/compiler, how do you avoid Visitor becoming a performance bottleneck?</strong></summary>

Two virtual calls per node are cheap but multiply across millions of nodes and repeated evaluations. Techniques: (1) **cache/compile** — don't re-walk the AST every evaluation; compile it once to a closure tree (`Function<Ctx,V>`), bytecode (via ASM/`invokedynamic`), or an instruction array for a bytecode VM; (2) keep call sites **monomorphic** so the JIT inlines the vtable lookups; (3) use **object pooling / flyweight** for terminal nodes; (4) for the hottest interpreters, replace polymorphic dispatch with a `switch` on an integer opcode (data-oriented) which the JIT turns into a jump table. The Visitor stays the clean authoring model; a compilation pass produces the fast executable form.

</details>

<details>
<summary><strong>SP4 [Principal]: How would you evolve a published Visitor-based API without breaking downstream consumers when you must add an element type?</strong></summary>

Adding a `visit(NewType)` to a published interface is a **binary/source-breaking change**. Options, least to most disruptive: (1) add the method as a `default` on the visitor interface with a sensible fallback — old visitors keep compiling and running (they just won't handle the new type specially); (2) introduce a `VisitorV2` interface extending `Visitor` with the new method, and route `NewType.accept` to call the V2 method only if `visitor instanceof VisitorV2` (an Acyclic-Visitor-style bridge); (3) version the whole API (`v2` package) and deprecate `v1`. Combine with clear semantics for the default (fail loud vs. silent no-op) — for correctness-critical operations, a default that throws `UnsupportedOperationException` surfaces gaps rather than hiding them.

</details>

<details>
<summary><strong>SP5 [Principal]: When is Visitor an anti-pattern, and what have you seen go wrong in real systems?</strong></summary>

Visitor becomes an anti-pattern when applied to a **rapidly evolving domain model**: every new entity type triggers edits across a large, coupled visitor surface, causing merge conflicts and discouraging teams from adding types — the pattern actively fights the domain's natural growth. It's also misused as a generic "traversal" tool where a simple Iterator or a recursive method would do, adding indirection and double-dispatch cognitive load for no benefit. And it frequently **erodes encapsulation**: teams expose getters on domain objects purely to satisfy visitors, leaking representation. Symptoms I watch for: visitor interfaces with 20+ `visit` methods, frequent "add a case to every visitor" changesets, and domain objects that are all getters and no behavior. The fix is usually to move behavior back onto the types, adopt sealed+pattern-matching, or split the hierarchy so the stable-vs-volatile parts are separated.

</details>

<details>
<summary><strong>SP6 [Principal]: How do reflective or annotation-driven "visitors" (e.g., annotation processors, serializers) relate to the classic pattern, and what are the trade-offs?</strong></summary>

Frameworks often implement a Visitor-like dispatch **reflectively** — inspecting an object's runtime class and routing to the right handler (e.g., Jackson serializers, `javac` annotation processing, bean-definition visitors). This trades compile-time safety and speed for open extensibility: you can register handlers for types discovered at runtime, breaking the classic cyclic coupling entirely. Costs: reflection is slower (mitigated by caching `MethodHandle`s or generating code), errors surface at runtime not compile time, and it's harder to reason about exhaustiveness. Principal-level judgment: use the classic type-safe Visitor for closed, performance-sensitive hierarchies (interpreters), and reflective/registry-based dispatch for open, plugin-heavy, or IO-bound frameworks (serialization, DI) where extensibility and decoupling outweigh the safety and speed of static dispatch.

</details>

---

## ⚡ Quick Revision

**One-liner:** Visitor lets you add new operations to a stable set of element types without modifying them, using double dispatch (`element.accept(visitor)` → `visitor.visit(this)`).

**The whole pattern in one paragraph:** You have a *stable* hierarchy of element types and a *growing* set of operations. Rather than bloating each element with every operation (which violates OCP and scatters concerns), you externalize each operation into a **Visitor** object that declares one `visit(ConcreteElement)` per type. Each element implements `accept(visitor)` as exactly `visitor.visit(this)`. This achieves **double dispatch**: the first virtual call resolves the element's runtime type, then inside `accept` the compiler picks the right `visit` overload (because `this` is statically the concrete type) while dispatching on the visitor's runtime type — so behavior depends on *both* types, which single-dispatch Java can't do in one call. Adding an *operation* is cheap (a new visitor class, elements untouched) but adding an *element type* is expensive (edit the visitor interface + every visitor) — this is the **Expression Problem**, and Visitor deliberately makes operations-easy/types-hard. Variants progress from a fragile `instanceof` chain, to the classic `void`-returning stateful visitor, to a generic `Visitor<R>` returning typed results (stream-friendly, no mutable state), optionally with `default` methods for partial visitors. It pairs constantly with **Composite** (traverse a tree) and **Iterator**, and in modern Java (17/21) **sealed interfaces + pattern-matching `switch`** are the go-to alternative when you own the hierarchy, giving exhaustiveness without boilerplate. Real uses: `Files.walkFileTree`/`FileVisitor`, annotation-processing `ElementVisitor`, ASM/JavaParser AST visitors. Watch for broken encapsulation (visitors needing element internals) and the ripple cost when types change.

**Top 5 interview answers to memorize:**
1. *What is it?* → "Add operations to a fixed type hierarchy without editing the types, via double dispatch."
2. *Double dispatch?* → "`accept` dispatches on the element's runtime type; inside it, `visit(this)` picks the overload by static type and dispatches on the visitor's type — behavior depends on both."
3. *Main drawback?* → "Expression Problem: easy to add operations, painful to add element types — breaks the visitor interface and all visitors."
4. *When not to use?* → "When the element hierarchy changes often, or operations naturally belong on the element; consider sealed types + pattern matching instead."
5. *Real example?* → "`java.nio.file.FileVisitor` / `Files.walkFileTree`, and compiler AST visitors (evaluate, print, optimize over one node hierarchy)."

**Trigger words** (hear these → think Visitor): "add operations without changing the classes," "AST / syntax tree / compiler passes," "double dispatch," "export to many formats," "operations over a stable hierarchy," "traverse a tree and do type-specific work," "keep the elements simple / pull logic out of them," "one operation per class over many node types."

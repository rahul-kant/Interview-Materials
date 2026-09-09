# Functional Programming in Java 8+ — Functional Interfaces, Lambdas & Streams

> A friendly, complete walk-through of functional programming in Java: **why** it exists, **what** functional interfaces and lambdas really are, and **how** the Stream API uses them to process data declaratively. Reorganized for clarity, with runnable examples and FAANG interview questions. Code targets **Java 8+**, with Java 16+ features (like `toList()`) flagged inline.

---

## Table of Contents

**Part I — Functional Interfaces & Lambdas**

- [1. What Is Functional Programming?](#1-what-is-functional-programming)
- [2. Java Is Not a Functional Language (and That's OK)](#2-java-is-not-a-functional-language-and-thats-ok)
- [3. Life Before Java 8 — The Anonymous Class Era](#3-life-before-java-8--the-anonymous-class-era)
- [4. Functional Interfaces](#4-functional-interfaces)
- [5. Lambda Expressions](#5-lambda-expressions)
- [6. Using Lambdas: Store, Pass, Return](#6-using-lambdas-store-pass-return)
- [7. Method References](#7-method-references)
- [8. The Built-in Functional Interfaces](#8-the-built-in-functional-interfaces)
- [9. Lambdas vs Anonymous Classes — Seeing the Difference](#9-lambdas-vs-anonymous-classes--seeing-the-difference)
- [10. The Autoboxing Problem & Primitive Interfaces](#10-the-autoboxing-problem--primitive-interfaces)

**Part II — The Stream API**

- [11. What Is a Stream? (The Conveyor Belt)](#11-what-is-a-stream-the-conveyor-belt)
- [12. Creating a Stream](#12-creating-a-stream)
- [13. Intermediate vs Terminal Operations & Lazy Evaluation](#13-intermediate-vs-terminal-operations--lazy-evaluation)
- [14. Intermediate Operations](#14-intermediate-operations)
- [15. Terminal Operations](#15-terminal-operations)
- [16. Collectors — Materializing Results](#16-collectors--materializing-results)
- [17. Putting It All Together](#17-putting-it-all-together)
  - [17.1 Data Slicing & Dicing Patterns](#171-data-slicing--dicing-patterns)
  - [17.2 Frequently-Asked FAANG Stream Coding Problems](#172-frequently-asked-faang-stream-coding-problems)

**Reference**

- [18. FAANG Interview Questions](#18-faang-interview-questions)
- [19. Quick Revision Cheat Sheet](#19-quick-revision-cheat-sheet)

---

# Part I — Functional Interfaces & Lambdas

## 1. What Is Functional Programming?

Functional programming (FP) is a style of building programs out of **functions** — and treating those functions as ordinary values you can pass around. Before the syntax, it helps to know the five ideas FP is built on, because everything Java added in Java 8 is an attempt to *simulate* these ideas.

**A function takes inputs and returns one output.** Inspired by math: `f(x) = x²` takes one number and gives one number back. A function receives zero or more inputs and returns exactly one output.

**Pure functions** are the ideal. A function is *pure* when it satisfies two rules:
- **Determinism** — the same inputs always produce the same output. `square(3)` is always `9`.
- **No side effects** — it doesn't change anything outside itself: no modifying global variables, no writing to files or the database, no printing. It just computes and returns.

**Immutability** — data isn't modified in place; it's *transformed* into new values, leaving the original untouched. Instead of changing a list, you produce a new list.

**Declarative style** — you say *what* you want, not *how* to compute it step by step. "Give me the even numbers" rather than "start a loop, check each index, if even add to a result list..."

**Functions as first-class citizens** — the big one. A function can be treated like any other value. You can:
- assign it to a variable,
- pass it as a parameter to another method,
- return it from a method,
- store it in a collection.

This is what lets you *compose* programs from small functions, like clicking Lego bricks together.

> **In one sentence:** FP focuses on *what* result you want, builds it from small pure functions, avoids mutation and side effects, and treats functions as values you can move around freely.

## 2. Java Is Not a Functional Language (and That's OK)

Be clear from the start: **Java is, and will remain, fundamentally object-oriented.** It does not have free-standing functions the way purely functional languages do. In Java, behavior lives in **methods**, and methods always belong to a class. Historically this made Java verbose — to "pass some logic" you had to wrap it in an object.

Starting with **Java 8**, Oracle added tools that let us *adopt a functional style within* the object-oriented model, without breaking the billions of existing lines of Java or changing the JVM. The goal isn't to become Haskell; it's to make developers *feel* like they're working with functions, while Java quietly keeps managing classes, methods, and objects underneath.

What we gain by adopting the functional style in Java 8+:
- More **declarative** code instead of imperative loops.
- Fewer bugs from **mutability**.
- Less **verbosity**, better readability.
- The ability to **simulate functions** and **function composition**.

Java builds this on **three pillars**, which are exactly the topics of Part I:

```
        Functional style in Java 8+
        ┌───────────┬────────────┬──────────────────┐
        │ Functional│   Lambda   │     Method        │
        │ interfaces│ expressions│   references      │
        └───────────┴────────────┴──────────────────┘
         (the TYPE)   (the LOGIC)   (shorthand logic)
```

From here on, when this guide says **"function,"** it means *the behavior represented by a functional interface and implemented by a lambda* — Java's way of simulating a function.

## 3. Life Before Java 8 — The Anonymous Class Era

To appreciate *why* functional interfaces and lambdas exist, look at the pain they solved.

Before Java 8, if you wanted to pass logic into a method, store it, or return it, you couldn't do it directly. The standard trick was to wrap the logic in an **anonymous class** — a class with no name, created right where it's used, usually to implement an interface that has a single method (like `Runnable`, `Callable`, `Comparator`, `ActionListener`).

When the compiler sees `new SomeInterface() { ... }`, it does three things:
1. At compile time, it generates a real class implementing the interface (a `.class` file).
2. It overrides the single abstract method with the logic in the braces.
3. At runtime, it creates an instance of that class and hands it where it's needed.

So you *were* passing behavior — but buried under a mountain of ceremony.

<details>
<summary><b>▶ Example — passing logic with an anonymous class (the old way)</b></summary>

```java
import java.util.*;

public class BeforeJava8 {
    public static void main(String[] args) {
        // 1) Runnable — logic for a thread to run
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Hello from the past");
            }
        });
        thread.start();

        // 2) Comparator — logic for how to sort
        List<Integer> numbers = new ArrayList<>(Arrays.asList(5, 2, 8, 1));
        Collections.sort(numbers, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return a - b; // ascending
            }
        });
        System.out.println(numbers); // [1, 2, 5, 8]
    }
}
```
```text
Hello from the past
[1, 2, 5, 8]
```
`Thread` needs a `Runnable`; `Collections.sort` needs a `Comparator`. In both cases we supplied an *object* whose single method holds the real logic. The interface defines *what* to do (`run`, `compare`); we supply *how*.

</details>

<details>
<summary><b>▶ Example — storing that logic in variables, reusing it</b></summary>

```java
import java.util.*;

public class StoringLogic {
    public static void main(String[] args) {
        List<Integer> numbers = new ArrayList<>(Arrays.asList(5, 2, 8, 1));

        // Store comparison logic in a variable
        Comparator<Integer> ascending = new Comparator<Integer>() {
            @Override public int compare(Integer a, Integer b) { return a - b; }
        };

        // A different logic in another variable
        Comparator<Integer> descending = new Comparator<Integer>() {
            @Override public int compare(Integer a, Integer b) { return b - a; }
        };

        Collections.sort(numbers, ascending);
        System.out.println(numbers);   // [1, 2, 5, 8]

        Collections.sort(numbers, descending);
        System.out.println(numbers);   // [8, 5, 2, 1]
    }
}
```
```text
[1, 2, 5, 8]
[8, 5, 2, 1]
```
Two variables, both of type `Comparator<Integer>`, each holding *different behavior*. This is the whole point of single-method interfaces: the interface fixes the action, you supply the specific logic.

</details>

Notice the pattern in every example: **there's always (a) an interface with a single abstract method, and (b) a local anonymous-class implementation.** Java didn't yet call this "functional programming," but the foundation was already there. Java 8 simply gave this pattern a name — **functional interfaces** — and a shorthand — **lambdas**.

```
BEFORE JAVA 8 — to perform a task you had to:
  1. Create an anonymous class
  2. @Override and write the method signature
  3. Finally, write the actual logic you cared about
     (steps 1–2 are pure noise around step 3)
```

## 4. Functional Interfaces

In Java 8, an interface with **exactly one abstract method** got a formal name: a **functional interface**. That single method is the heart of it.

- **The abstract method** defines the function's *signature*: what parameters it takes, what it returns, and the method name that runs the logic.
- The interface **may also** contain `default` and `static` methods — those don't count. Only **one abstract method** is allowed.

A functional interface's job isn't to run logic by itself. Its job is to give a function a **type** and a **signature**, so Java (which always needs a type) can let you:
- store a function in a variable,
- pass a function as a parameter,
- return a function from a method.

You can use Java's built-in ones (Section 8) or write your own. To write your own, add the optional but recommended **`@FunctionalInterface`** annotation — it tells the compiler to *verify* there's exactly one abstract method and fail the build if someone accidentally adds a second.

<details>
<summary><b>▶ Example — defining your own functional interface</b></summary>

```java
@FunctionalInterface
public interface Operation {   // a functional interface
    // THE SINGLE ABSTRACT METHOD — defines the signature
    int execute(int a, int b);

    // default or static methods are allowed here, but only ONE abstract method
}
```

That's it. `Operation` now represents "a function that takes two ints and returns an int." Anything that implements it — a class, an anonymous class, or a lambda — behaves like a function of that type.

</details>

> **Key idea:** Whenever you want to use a function as a value in Java, a functional interface is always involved. It provides the **TYPE** and the **SIGNATURE**; you inject the **IMPLEMENTATION** (the real logic) with a lambda.

## 5. Lambda Expressions

A **lambda expression** is the concise, functional way to implement a functional interface's single abstract method — without writing an anonymous class. It looks like a standalone function, but under the hood it still creates an implementation of a functional interface and assigns your logic to that interface's abstract method.

**Lambdas are only usable where a functional interface is expected.** No functional interface, no lambda.

**Anatomy** — three parts:

```
(PARAMETERS) -> { BODY }
      │        │      │
      │        │      └── the logic
      │        └── the "arrow" operator, separates params from body
      └── inputs (types usually inferred — you rarely write them)
```

**Parameters** — Java uses **type inference**: it figures out the parameter types from the functional interface, so you usually omit them.

```java
(x, y) -> ...    // multiple parameters
x -> ...         // single parameter (parentheses optional)
() -> ...        // no parameters
```

**Body** — a single expression or a block:

```java
x -> x * x                 // single expression: NO 'return' needed, value is implicit
(x, y) -> {                // block body: 'return' IS required
    int result = x + y;
    return result;
}
```

<details>
<summary><b>▶ Example — three ways to implement the same interface</b></summary>

```java
public class LambdaIntro {
    @FunctionalInterface
    interface Operation { int apply(int a, int b); }

    // A normal class that implements the interface
    static class Sum implements Operation {
        @Override public int apply(int a, int b) { return a + b; }
    }

    public static void main(String[] args) {
        // 1) Normal class
        Operation op1 = new Sum();
        System.out.println(op1.apply(2, 3)); // 5

        // 2) Anonymous class (the old, verbose way)
        Operation op2 = new Operation() {
            @Override public int apply(int a, int b) { return a + b; }
        };
        System.out.println(op2.apply(2, 3)); // 5

        // 3) Lambda (the modern, concise way) — SAME result
        Operation op3 = (x, y) -> x + y;
        System.out.println(op3.apply(2, 3)); // 5
    }
}
```
```text
5
5
5
```
All three produce an object of type `Operation` with identical behavior. The lambda `(x, y) -> x + y` is just a far shorter way to write what the anonymous class spells out — and what a whole separate `Sum` class spells out even more verbosely.

</details>

> **Mental model:** "I want to pass a function as a value" → first *define the type* (use or create a functional interface) → then *provide the implementation* with a lambda. The lambda is the logic; the interface is the type that lets Java accept it.

## 6. Using Lambdas: Store, Pass, Return

Because a functional interface is a real type, a lambda (which produces an object of that type) can be used anywhere a value can: stored in a variable, passed as an argument, or returned from a method. These three abilities are exactly what "functions as first-class citizens" means in Java.

A crucial detail: **defining a lambda does not run it.** Just like assigning a value to a variable doesn't "use" the value, creating a lambda only stores the behavior. The logic runs only when the interface's abstract method (`apply`, `test`, `accept`, `get`, …) is actually called — sometimes by you, sometimes later by a library or framework.

<details>
<summary><b>▶ Example — storing a function in a variable</b></summary>

```java
import java.util.function.Predicate;

public class StoreFunction {
    public static void main(String[] args) {
        // TYPE = functional interface     LAMBDA = the logic
        Predicate<Integer> validator = number -> number > 2;

        // Nothing has run yet — 'validator' holds BEHAVIOR, not true/false.
        // The logic runs only when we call test():
        System.out.println(validator.test(5));  // true
        System.out.println(validator.test(1));  // false
    }
}
```
```text
true
false
```
`Predicate<Integer>` has one abstract method, `boolean test(T t)`. The lambda implements it. The variable stores the *complete behavior*, and Java runs it when `validator.test(...)` is invoked.

</details>

<details>
<summary><b>▶ Example — passing a function as a parameter</b></summary>

```java
import java.util.*;
import java.util.function.*;

public class PassFunction {
    // Method accepts a Predicate — i.e. it accepts behavior
    static void printMatching(List<Integer> numbers, Predicate<Integer> condition) {
        numbers.stream().filter(condition).forEach(System.out::println);
    }

    // Method accepts a Function — a transformation
    static void printTransformed(List<Integer> numbers, Function<Integer, Integer> transform) {
        numbers.stream().map(transform).forEach(System.out::println);
    }

    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6);

        System.out.println("Even:");
        printMatching(numbers, x -> x % 2 == 0);   // pass behavior: even
        System.out.println("Odd:");
        printMatching(numbers, x -> x % 2 != 0);    // pass behavior: odd
        System.out.println("Squares:");
        printTransformed(numbers, x -> x * x);       // pass behavior: square
    }
}
```
```text
Even:
2
4
6
Odd:
1
3
5
Squares:
1
4
9
16
25
36
```
The same method behaves differently depending on the lambda we hand it. The method *receives an object* implementing the functional interface — even though it looks like we're passing a function.

</details>

<details>
<summary><b>▶ Example — returning a function from a method</b></summary>

```java
import java.util.function.Function;

public class ReturnFunction {
    // The return type is a functional interface → we return a "function"
    static Function<Integer, String> toLabel() {
        return x -> x + " is number ONE";
    }

    public static void main(String[] args) {
        Function<Integer, String> labeler = toLabel(); // get the function
        System.out.println(labeler.apply(1));          // run it: "1 is number ONE"
        System.out.println(labeler.apply(42));         // "42 is number ONE"
    }
}
```
```text
1 is number ONE
42 is number ONE
```
`toLabel()` hands back behavior. What's *actually* returned is an object implementing `Function<Integer,String>`, created from the lambda.

</details>

> **Why this matters:** Any method whose parameter type is, say, `Function<String,Integer>` will happily accept (a) a lambda `s -> s.length()`, (b) a variable already holding that function, or (c) a method call that returns such a function. As long as the *type* matches, Java doesn't care how you produced it.

## 7. Method References

A **method reference** is an even shorter way to write a lambda — but *only* when the lambda does nothing except call one existing method. It adds no new behavior; it's pure readability.

Like lambdas, method references aren't executed immediately and aren't standalone functions — Java still uses them to build an implementation of a functional interface.

Use a method reference when your lambda **only** forwards to an existing method and adds **no** extra logic (no validation, no calculation, no condition). If the body does anything more, you must keep the lambda.

```java
// Lambda that only forwards to an existing method:
.map(x -> x.toUpperCase())
// Can become a method reference:
.map(String::toUpperCase)

// But this CANNOT be a method reference — it has real logic:
.filter(x -> x.startsWith("a"))
```

**The four kinds of method reference:**

| Kind | Syntax | Equivalent lambda |
|---|---|---|
| Static method | `Integer::parseInt` | `s -> Integer.parseInt(s)` |
| Instance method of a *specific* object | `System.out::println` | `x -> System.out.println(x)` |
| Instance method of an *arbitrary* object of a type | `String::toUpperCase` | `s -> s.toUpperCase()` |
| Constructor | `ArrayList::new` | `() -> new ArrayList<>()` |

<details>
<summary><b>▶ Example — all four kinds in action</b></summary>

```java
import java.util.*;
import java.util.function.*;

public class MethodRefDemo {
    public static void main(String[] args) {
        // 1) Static
        Function<String, Integer> parse = Integer::parseInt;
        System.out.println(parse.apply("42"));   // 42

        // 2) Instance method of a specific object (System.out)
        Consumer<String> printer = System.out::println;
        printer.accept("hello");                  // hello

        // 3) Instance method of an arbitrary object of a type
        //    The argument becomes the receiver: s -> s.toUpperCase()
        Function<String, String> upper = String::toUpperCase;
        System.out.println(upper.apply("abc"));   // ABC

        // 4) Constructor
        Supplier<List<String>> newList = ArrayList::new;
        List<String> list = newList.get();
        list.add("created via constructor ref");
        System.out.println(list);                 // [created via constructor ref]
    }
}
```
```text
42
hello
ABC
[created via constructor ref]
```
Each method reference is just a tidier alias for the lambda in the right-hand column of the table. Kind #3 is the subtle one: `String::toUpperCase` becomes `s -> s.toUpperCase()`, where the passed-in string *becomes the object the method is called on*.

</details>

## 8. The Built-in Functional Interfaces

Rather than make you define a new interface every time, Java ships a set of ready-to-use generic functional interfaces in the **`java.util.function`** package. These cover the common shapes of behavior. They're used *everywhere* in the Stream API, which is why mastering them now pays off in Part II.

These interfaces rely on **generics**: you declare up front what type(s) they accept and return, then just supply the logic with a lambda.

| Interface | Abstract method | Takes → Returns | Use it for |
|---|---|---|---|
| `Predicate<T>` | `boolean test(T t)` | T → boolean | a yes/no test (filtering) |
| `Function<T,R>` | `R apply(T t)` | T → R | transforming a value into another |
| `Consumer<T>` | `void accept(T t)` | T → nothing | doing something with a value (printing, saving) |
| `Supplier<T>` | `T get()` | nothing → T | producing/providing a value on demand |
| `BiFunction<T,U,R>` | `R apply(T,U)` | (T,U) → R | combining two values into one |
| `BiPredicate<T,U>` | `boolean test(T,U)` | (T,U) → boolean | a test on two inputs |
| `BiConsumer<T,U>` | `void accept(T,U)` | (T,U) → nothing | acting on two inputs (e.g. `Map.forEach`) |
| `UnaryOperator<T>` | `T apply(T t)` | T → T | a `Function` where input and output types are the same |
| `BinaryOperator<T>` | `T apply(T,T)` | (T,T) → T | a `BiFunction` with all-same types (used by `reduce`) |

The generic letters: **`T`** = input **T**ype, **`R`** = **R**eturn type, **`U`** = a second input type. So `Function<T,R>` reads "takes a `T`, returns an `R`."

To learn them, it helps to see each implemented *both* ways — the old anonymous class and the modern lambda — using a simple domain class.

<details>
<summary><b>▶ The domain class used in the examples</b></summary>

```java
public class Order {
    int id;
    double amount;
    String status; // "PENDING", "PROCESSED"

    public Order(int id, double amount, String status) {
        this.id = id;
        this.amount = amount;
        this.status = status;
    }
    public int getId() { return id; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
}
```

</details>

<details>
<summary><b>▶ Predicate — a validation (is this a high-value order?)</b></summary>

```java
import java.util.function.Predicate;

public class PredicateExample {
    public static void main(String[] args) {
        // OLD: anonymous class (verbose)
        Predicate<Order> highValueOld = new Predicate<Order>() {
            @Override public boolean test(Order o) { return o.amount > 100; }
        };

        // MODERN: lambda
        Predicate<Order> highValue = order -> order.amount > 100;

        Order myOrder = new Order(1, 500, "PENDING");
        System.out.println(highValueOld.test(myOrder)); // true
        System.out.println(highValue.test(myOrder));    // true
    }
}
```
```text
true
true
```
A `Predicate<Order>` answers a yes/no question about an `Order`. Its method is `test`, returning a boolean. The lambda version is the same logic without the `new`/`@Override`/braces noise.

</details>

<details>
<summary><b>▶ Function — a transformation (turn an Order into a message)</b></summary>

```java
import java.util.function.Function;

public class FunctionExample {
    public static void main(String[] args) {
        // OLD: anonymous class
        Function<Order, String> toMessageOld = new Function<Order, String>() {
            @Override public String apply(Order o) {
                return "Order #" + o.id + " for $" + o.amount;
            }
        };

        // MODERN: lambda
        Function<Order, String> toMessage =
            order -> "Order #" + order.id + " for $" + order.amount;

        Order myOrder = new Order(1, 500, "PENDING");
        System.out.println(toMessageOld.apply(myOrder));
        System.out.println(toMessage.apply(myOrder));
    }
}
```
```text
Order #1 for $500.0
Order #1 for $500.0
```
A `Function<Order,String>` transforms an `Order` into a `String`. Its method is `apply`. Note the input and output types differ — that's the general `Function`.

</details>

<details>
<summary><b>▶ Consumer & Supplier — acting on a value, and producing one</b></summary>

```java
import java.util.function.*;
import java.util.UUID;

public class ConsumerSupplierExample {
    public static void main(String[] args) {
        // Consumer<T>: takes a value, returns nothing (a "sink")
        Consumer<String> printer = msg -> System.out.println("LOG: " + msg);
        printer.accept("order processed");   // LOG: order processed

        // Supplier<T>: takes nothing, produces a value (a "factory")
        Supplier<UUID> idGenerator = UUID::randomUUID;
        System.out.println("generated id length: " + idGenerator.get().toString().length());

        // BiFunction<T,U,R>: two inputs → one output
        BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
        System.out.println(add.apply(2, 3)); // 5
    }
}
```
```text
LOG: order processed
generated id length: 36
5
```
`Consumer` consumes a value and returns nothing (great for `forEach`). `Supplier` produces a value on demand (great for lazy defaults). `BiFunction` combines two inputs.

</details>

## 9. Lambdas vs Anonymous Classes — Seeing the Difference

The clearest way to feel *why* lambdas matter is to write the same Stream pipeline both ways. Suppose we want to: **filter names starting with "A", uppercase them, and print them.**

<details>
<summary><b>▶ The painful way — anonymous classes everywhere</b></summary>

```java
import java.util.*;
import java.util.function.*;

public class AnonymousNoise {
    public static void main(String[] args) {
        List<String> names = List.of("Naomi", "Mario", "Antonio", "Ana", "Angela");

        names.stream()
            .filter(new Predicate<String>() {        // NOISE
                @Override public boolean test(String s) {
                    return s.startsWith("A");        // real logic
                }
            })
            .map(new Function<String, String>() {    // NOISE
                @Override public String apply(String s) {
                    return s.toUpperCase();          // real logic
                }
            })
            .forEach(new Consumer<String>() {        // NOISE
                @Override public void accept(String s) {
                    System.out.println(s);           // real logic
                }
            });
    }
}
```
```text
ANTONIO
ANA
ANGELA
```
It works — but look how the three lines of *real logic* are drowned in `new`, `@Override`, and braces. Add a few more operations and it becomes unmanageable.

</details>

<details>
<summary><b>▶ The clean way — lambdas (and a method reference)</b></summary>

```java
import java.util.*;

public class LambdaClean {
    public static void main(String[] args) {
        List<String> names = List.of("Naomi", "Mario", "Antonio", "Ana", "Angela");

        names.stream()
            .filter(name -> name.startsWith("A"))   // real logic, nothing else
            .map(name -> name.toUpperCase())        // (or String::toUpperCase)
            .forEach(System.out::println);          // method reference
    }
}
```
```text
ANTONIO
ANA
ANGELA
```
Identical result, but now you read only the *intent*: filter, map, print. This is the readability win that makes lambdas the default in functional-style Java.

</details>

**The conceptual difference** (they're *not* identical under the hood): with an anonymous class you explicitly build a whole object containing an implemented method. With a lambda you focus only on the logic, far more concisely, and Java generates the implementation in an optimized way. Both reach the same behavior; the lambda just removes the ceremony.

## 10. The Autoboxing Problem & Primitive Interfaces

All the built-in interfaces in Section 8 use **generics** — and Java generics only work with **object types**, never primitives:

```java
Function<Integer, Integer> square = x -> x * x;   // OK — uses Integer (object)
Function<int, int> wrong = ...;                     // ❌ illegal — primitives not allowed in generics
```

So when you call `square.apply(2)`, Java takes the primitive `int` `2` and converts it to an `Integer` object (`int → Integer`). That automatic conversion is called **autoboxing**. It's convenient, but with large volumes of data it becomes inefficient — every value is wrapped in a heap object.

To avoid this, Java provides **primitive specializations** of the functional interfaces that work directly with `int`, `long`, and `double` — no boxing:

```
IntPredicate          (int  -> boolean)
IntFunction<R>        (int  -> R)
IntUnaryOperator      (int  -> int)
IntBinaryOperator     (int, int -> int)
IntConsumer           (int  -> void)
ToIntFunction<T>      (T    -> int)
// ...and Long/Double equivalents
```

<details>
<summary><b>▶ Example — boxing vs primitive interface</b></summary>

```java
import java.util.function.*;

public class AutoboxingExample {
    public static void main(String[] args) {
        // Boxes every int into an Integer (allocation per value)
        Function<Integer, Integer> boxedSquare = x -> x * x;
        System.out.println(boxedSquare.apply(2));   // 4  (2 was autoboxed to Integer)

        // No boxing — operates on primitive int directly
        IntUnaryOperator primitiveSquare = x -> x * x;
        System.out.println(primitiveSquare.applyAsInt(2)); // 4

        // ToIntFunction<T>: object in, primitive int out (no boxing of the result)
        ToIntFunction<String> length = String::length;
        System.out.println(length.applyAsInt("hello")); // 5
    }
}
```
```text
4
4
5
```
Functionally the same answer, but `IntUnaryOperator` avoids creating `Integer` objects. Over millions of elements this is a real performance difference — which is exactly why the primitive stream types (`IntStream`, etc.) exist in Part II.

</details>

> **Reflection — type inference:** When you write a lambda you don't specify the parameter type (`Order order`). The compiler infers it from the functional interface's generics. `Predicate<Order>` ⇒ the lambda's parameter must be an `Order`, so you just write `order -> ...`.

---

# Part II — The Stream API

Everything in Part I was setup. The **Stream API** is the payoff: it's where functional interfaces and lambdas come together to let you process collections *declaratively*. Instead of writing loops that say *how* to iterate, you describe *what* you want done, and the API handles the traversal.

## 11. What Is a Stream? (The Conveyor Belt)

The `.stream()` method turns a collection (a `List`, `Set`, …) into a **stream** — a flow of elements you can run operations on. Three things a Stream is **not**:

- It does **not store data**. It holds no copy of the elements.
- It is **not a collection**. It's a *pipeline of operations*, not a container.
- It does **not modify the original** collection. It only *reads* from it.

**The conveyor-belt analogy.** Picture a factory conveyor belt. The original collection's data is placed on the belt one element at a time. As each element travels, it passes through stations — filter here, transform there, sort there — each doing one job. At the very end, the finished items are collected into a result (a new list, a count, a sum). The original pile of raw material is untouched; you've produced something new from it.

```
 List/Set ──.stream()──▶  [filter] ─▶ [map] ─▶ [sorted] ─▶  collect/forEach
 (source)                 └──── intermediate ops ────┘      └─ terminal op ─┘
                          (each element flows through, one at a time)
```

So a Stream lets you chain operations into a **pipeline**; each operation receives the result of the previous one. To keep a result, you finish with a **terminal operation** that *materializes* the outcome (e.g. into a new collection) — and the source stays intact.

## 12. Creating a Stream

The most common source is a collection, via `.stream()`. There are several other ways too.

<details>
<summary><b>▶ Example — common ways to get a Stream</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class CreatingStreams {
    public static void main(String[] args) {
        // From a collection
        List<String> names = List.of("Mario", "Naomi", "Jose");
        System.out.println(names.stream().count());          // 3

        // From explicit values
        System.out.println(Stream.of("a", "b", "c").count()); // 3

        // From an array
        int[] nums = {1, 2, 3, 4};
        System.out.println(Arrays.stream(nums).sum());        // 10

        // A range of ints (no boxing)
        System.out.println(IntStream.rangeClosed(1, 5).sum()); // 15
    }
}
```
```text
3
3
10
15
```
`collection.stream()` is what you'll use 90% of the time. The key point: creating a stream copies nothing — it's a lazy view over the source.

</details>

## 13. Intermediate vs Terminal Operations & Lazy Evaluation

Every stream operation is one of two kinds, and the distinction is the most important idea in Part II.

**Intermediate operations** transform the stream and **return a new Stream**, so they can be chained. Crucially, they are **lazy** — they do *not* process any data when you call them. They just record *what should happen*, building an execution plan. Examples: `filter`, `map`, `sorted`, `distinct`, `flatMap`, `limit`, `skip`, `peek`.

**Terminal operations** end the pipeline. They **trigger the actual processing** and produce a final result (a value, a collection, or a side effect like printing). After a terminal operation, the stream is **consumed and cannot be reused**. Examples: `forEach`, `collect`, `count`, `reduce`, `findFirst`.

This behaviour is called **lazy evaluation**: nothing runs until a terminal operation activates the flow.

```java
stream
  .filter(...)   // not executed yet — just planned
  .map(...)      // not executed yet — just planned
  .sorted();     // not executed yet — just planned
// ↑ only an execution PLAN exists so far. No data has moved.

// Add a terminal op and EVERYTHING runs:
  .forEach(...)  // executes the whole pipeline now!
```

<details>
<summary><b>▶ Example — proof that intermediates are lazy</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class LazyEvaluation {
    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3);

        System.out.println("Building pipeline (no terminal op):");
        Stream<Integer> pipeline = numbers.stream()
            .peek(n -> System.out.println("  peek " + n))  // intermediate
            .map(n -> n * 2);                               // intermediate
        System.out.println("  -> nothing printed above, nothing ran");

        System.out.println("Now adding a terminal op:");
        List<Integer> result = pipeline.toList();           // terminal → runs everything
        System.out.println("  result = " + result);
    }
}
```
```text
Building pipeline (no terminal op):
  -> nothing printed above, nothing ran
Now adding a terminal op:
  peek 1
  peek 2
  peek 3
  result = [2, 4, 6]
```
The `peek` prints nothing until the terminal `toList()` runs. That's lazy evaluation: intermediates only build a plan; the terminal operation executes it.

</details>

## 14. Intermediate Operations

Each of these returns a new `Stream`, so they chain. They're lazy. Here are the main ones with signatures and runnable examples.

### `.filter()` — keep elements that pass a test

```java
Stream<T> filter(Predicate<? super T> predicate)
```
Only elements for which the `Predicate` returns `true` continue down the pipeline.

<details>
<summary><b>▶ Example — keep numbers less than 20</b></summary>

```java
import java.util.*;

public class FilterExample {
    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 5, 25, 8, 40, 12);
        numbers.stream()
            .filter(number -> number < 20)
            .forEach(System.out::println);
    }
}
```
```text
1
5
8
12
```
The `Predicate` `number -> number < 20` is the gate; only passing elements move on.

</details>

### `.map()` — transform each element

```java
<R> Stream<R> map(Function<? super T, ? extends R> mapper)
```
Transforms each element into a new value, possibly of a different type. Cardinality stays the same (one in → one out).

<details>
<summary><b>▶ Example — uppercase each name</b></summary>

```java
import java.util.*;

public class MapExample {
    public static void main(String[] args) {
        List<String> names = List.of("jose", "mario", "naomi");
        names.stream()
            .map(name -> name.toUpperCase())   // or String::toUpperCase
            .forEach(System.out::println);
    }
}
```
```text
JOSE
MARIO
NAOMI
```
`map` applies a `Function` to every element. Here `String → String`, but it could be `Order → String`, `String → Integer`, etc.

</details>

### `.sorted()` — order the elements

```java
Stream<T> sorted()
Stream<T> sorted(Comparator<? super T> comparator)
```
Sorts by natural order (if the type implements `Comparable`) or by a custom `Comparator`.

<details>
<summary><b>▶ Example — natural and custom ordering</b></summary>

```java
import java.util.*;

public class SortedExample {
    public static void main(String[] args) {
        List<Integer> numbers = List.of(5, 2, 8, 1, 9, 3);

        // Natural order (ascending)
        System.out.print("ascending: ");
        numbers.stream().sorted().forEach(n -> System.out.print(n + " "));
        System.out.println();

        // Custom order (descending) via a Comparator
        System.out.print("descending: ");
        numbers.stream()
            .sorted(Comparator.reverseOrder())
            .forEach(n -> System.out.print(n + " "));
        System.out.println();
    }
}
```
```text
ascending: 1 2 3 5 8 9 
descending: 9 8 5 3 2 1 
```
No-arg `sorted()` uses natural ordering; the `Comparator` overload lets you define any ordering.

</details>

### `.distinct()` — remove duplicates

```java
Stream<T> distinct()
```
Keeps only unique elements (using `equals`).

<details>
<summary><b>▶ Example — drop duplicate numbers</b></summary>

```java
import java.util.*;

public class DistinctExample {
    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 1, 1, 2, 3, 4, 4, 5, 0, 0);
        numbers.stream()
            .distinct()
            .forEach(n -> System.out.print(n + " "));
    }
}
```
```text
1 2 3 4 5 0 
```
Each value appears once; later duplicates are discarded.

</details>

### `.flatMap()` — transform and flatten nested structures

```java
<R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper)
```
The trickiest one. `flatMap` turns each element into a **stream**, then flattens all those streams into a **single** stream. Use it when each element contains a collection and you want one flat result instead of a "stream of lists."

<details>
<summary><b>▶ Example — map vs flatMap (the classic comparison)</b></summary>

```java
import java.util.*;

public class FlatMapVsMap {
    static class Employee {
        String name;
        Employee(String name) { this.name = name; }
        public String toString() { return name; }
    }
    static class Department {
        String name;
        List<Employee> employees;
        Department(String name, List<Employee> employees) {
            this.name = name; this.employees = employees;
        }
        String getName() { return name; }
        List<Employee> getEmployees() { return employees; }
    }

    public static void main(String[] args) {
        List<Department> departments = List.of(
            new Department("IT", List.of(new Employee("Juan"), new Employee("Ana"))),
            new Department("HR", List.of(new Employee("Luis"), new Employee("Maria")))
        );

        // 1) Just the department names — map is perfect (Department -> String)
        List<String> deptNames = departments.stream()
            .map(Department::getName)
            .toList();
        System.out.println("names: " + deptNames);

        // 2) All employees in ONE list — map gives the WRONG shape:
        //    Stream<Department> -> Stream<List<Employee>> -> List<List<Employee>>
        List<List<Employee>> nested = departments.stream()
            .map(Department::getEmployees)
            .toList();
        System.out.println("map (nested): " + nested);

        //    flatMap turns each List<Employee> into a stream, then flattens → List<Employee>
        List<Employee> allEmployees = departments.stream()
            .flatMap(dept -> dept.getEmployees().stream())
            .toList();
        System.out.println("flatMap (flat): " + allEmployees);
    }
}
```
```text
names: [IT, HR]
map (nested): [[Juan, Ana], [Luis, Maria]]
flatMap (flat): [Juan, Ana, Luis, Maria]
```
With `map` you get a `List<List<Employee>>` — nested, awkward. With `flatMap` you get a single flat `List<Employee>`. Whenever an element holds a collection and you want everything in one stream, reach for `flatMap`.

</details>

### `.limit()`, `.skip()`, `.peek()` — three more useful ones

```java
Stream<T> limit(long maxSize)              // take the first n elements
Stream<T> skip(long n)                     // skip the first n elements
Stream<T> peek(Consumer<? super T> action) // inspect each element without changing it
```

<details>
<summary><b>▶ Example — limit, skip, peek</b></summary>

```java
import java.util.stream.*;

public class LimitSkipPeek {
    public static void main(String[] args) {
        // limit: first 3
        System.out.print("limit(3): ");
        IntStream.rangeClosed(1, 10).limit(3).forEach(n -> System.out.print(n + " "));
        System.out.println();

        // skip: drop first 7, keep the rest
        System.out.print("skip(7): ");
        IntStream.rangeClosed(1, 10).skip(7).forEach(n -> System.out.print(n + " "));
        System.out.println();

        // peek: look at elements mid-pipeline (debugging only)
        long count = Stream.of("a", "b", "c")
            .peek(s -> System.out.println("peeking: " + s))
            .map(String::toUpperCase)
            .count();
        System.out.println("count: " + count);
    }
}
```
```text
limit(3): 1 2 3 
skip(7): 8 9 10 
peeking: a
peeking: b
peeking: c
count: 3
```
`peek` is meant for debugging (inspecting the flow), not for logic — it doesn't change elements.

</details>

## 15. Terminal Operations

A **terminal operation** ends the pipeline, triggers all the lazy intermediate work, and produces a result. You can't chain anything after it, and the stream is consumed.

### `.forEach()` — do something with each element

```java
void forEach(Consumer<? super T> action)
```
Iterates and performs an action; returns nothing. Mostly for printing/logging.

<details>
<summary><b>▶ Example — sort then print</b></summary>

```java
import java.util.*;

public class ForEachExample {
    public static void main(String[] args) {
        List<Integer> numbers = List.of(5, 2, 8, 1);
        numbers.stream()
            .sorted()
            .forEach(System.out::println);
    }
}
```
```text
1
2
5
8
```

</details>

### `.collect()` — gather into a collection (with `Collectors`)

```java
<R,A> R collect(Collector<? super T,A,R> collector)
```
The workhorse for materializing results, almost always with the helper class `Collectors`.

<details>
<summary><b>▶ Example — collect to List and to Set</b></summary>

```java
import java.util.*;
import java.util.stream.Collectors;

public class CollectExample {
    public static void main(String[] args) {
        List<String> names = List.of("jose", "mario", "naomi", "jose");

        // To a List
        List<String> upper = names.stream()
            .map(String::toUpperCase)
            .collect(Collectors.toList());
        System.out.println("list: " + upper);

        // To a Set (removes duplicates)
        Set<String> unique = names.stream()
            .collect(Collectors.toSet());
        System.out.println("set: " + unique);

        // Modern (Java 16+): toList() returns an IMMUTABLE list, no Collectors needed
        List<Integer> sorted = List.of(3, 1, 2).stream().sorted().toList();
        System.out.println("toList(): " + sorted);
    }
}
```
```text
list: [JOSE, MARIO, NAOMI, JOSE]
set: [jose, mario, naomi]
toList(): [1, 2, 3]
```
`Collectors.toList()` / `toSet()` materialize the stream into a collection. `Stream.toList()` (Java 16+) is shorter but returns an *unmodifiable* list.

</details>

### `.count()` — how many elements

```java
long count()
```

<details>
<summary><b>▶ Example — total, and count after filtering</b></summary>

```java
import java.util.*;

public class CountExample {
    public static void main(String[] args) {
        List<String> names = List.of("Maria", "Jose", "Miguel", "Ana");

        System.out.println(names.stream().count());                       // 4

        long withM = names.stream()
            .filter(name -> name.contains("M"))
            .count();
        System.out.println(withM);                                         // 2  (Maria, Miguel)
    }
}
```
```text
4
2
```

</details>

### `.findFirst()` — get the first element (as an `Optional`)

```java
Optional<T> findFirst()
```
Returns an `Optional` because the stream might be empty. `Optional` is a box that holds a value *or* nothing — it forces you to handle the empty case instead of risking a `NullPointerException`.

<details>
<summary><b>▶ Example — first element, safely</b></summary>

```java
import java.util.*;

public class FindFirstExample {
    public static void main(String[] args) {
        List<String> names = List.of("Mario", "Naomi", "Jose");

        Optional<String> first = names.stream().findFirst();

        first.ifPresent(System.out::println);                  // Mario
        System.out.println(first.orElse("list is empty"));     // Mario

        // On an empty stream:
        Optional<String> none = List.<String>of().stream().findFirst();
        System.out.println(none.orElse("list is empty"));      // list is empty
    }
}
```
```text
Mario
Mario
list is empty
```
`ifPresent` runs only if a value exists; `orElse` supplies a fallback. This is why `findFirst` returns `Optional` — no nulls to trip over.

</details>

### `.reduce()` — combine everything into one value

```java
Optional<T> reduce(BinaryOperator<T> accumulator)        // no seed → Optional
T reduce(T identity, BinaryOperator<T> accumulator)      // with seed → plain value
```
Folds all elements into a single value using a `BinaryOperator` (two same-typed inputs → one output of that type).

<details>
<summary><b>▶ Example — sum with reduce</b></summary>

```java
import java.util.*;

public class ReduceExample {
    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);

        // identity = 0, accumulator = a + b
        int sum = numbers.stream().reduce(0, (a, b) -> a + b);
        System.out.println("sum: " + sum);   // 15

        // Without identity → Optional (stream could be empty)
        Optional<Integer> product = numbers.stream().reduce((a, b) -> a * b);
        System.out.println("product: " + product.orElse(0)); // 120
    }
}
```
```text
sum: 15
product: 120
```
With an identity (`0`) you always get a value back. Without one you get an `Optional`, because an empty stream has nothing to reduce.

</details>

### The matching/searching family — `anyMatch`, `allMatch`, `noneMatch`, `findAny`, `toArray`

```java
boolean anyMatch(Predicate)   // at least one element matches?
boolean allMatch(Predicate)   // do all match?
boolean noneMatch(Predicate)  // do none match?
Optional<T> findAny()         // any element (no order guarantee)
Object[] toArray()            // dump to an array
```

<details>
<summary><b>▶ Example — the boolean matchers</b></summary>

```java
import java.util.*;

public class MatchExample {
    public static void main(String[] args) {
        List<Integer> numbers = List.of(2, 4, 6, 8);

        System.out.println(numbers.stream().anyMatch(n -> n > 5));   // true (6, 8)
        System.out.println(numbers.stream().allMatch(n -> n % 2 == 0)); // true (all even)
        System.out.println(numbers.stream().noneMatch(n -> n > 100)); // true (none > 100)

        Optional<Integer> any = numbers.stream().findAny();
        System.out.println(any.orElse(-1)); // 2 (in a sequential stream, usually the first)
    }
}
```
```text
true
true
true
2
```
These return early as soon as the answer is known (e.g. `anyMatch` stops at the first match) — efficient for "does anything satisfy X?" questions.

</details>

> **Remember:** a stream is consumed by its terminal operation. Trying to reuse it afterwards throws `IllegalStateException`. If you need to process the data twice, create a new stream from the source.

## 16. Collectors — Materializing Results

`Collectors` (used with `.collect()`) is how you shape a stream into something concrete: a list, set, map, a joined string, a count, a sum, or grouped buckets. Beyond `toList()`/`toSet()` from Section 15, these are the ones you'll reach for constantly.

<details>
<summary><b>▶ Example — joining, counting, summing, averaging</b></summary>

```java
import java.util.*;
import java.util.stream.Collectors;

public class CollectorsBasics {
    public static void main(String[] args) {
        List<String> names = List.of("Mario", "Naomi", "Jose");

        // joining: concatenate into one String, with separator
        String joined = names.stream().collect(Collectors.joining(", "));
        System.out.println(joined);   // Mario, Naomi, Jose

        // counting
        long count = names.stream().collect(Collectors.counting());
        System.out.println(count);    // 3

        List<Integer> nums = List.of(10, 20, 30);
        // summing
        int total = nums.stream().collect(Collectors.summingInt(n -> n));
        System.out.println(total);    // 60
        // averaging
        double avg = nums.stream().collect(Collectors.averagingInt(n -> n));
        System.out.println(avg);      // 20.0
    }
}
```
```text
Mario, Naomi, Jose
3
60
20.0
```
`joining` is great for building CSV-like strings; `counting`/`summingInt`/`averagingInt` produce aggregates.

</details>

<details>
<summary><b>▶ Example — toMap, groupingBy, partitioningBy</b></summary>

```java
import java.util.*;
import java.util.stream.Collectors;

public class CollectorsGrouping {
    record Person(String name, String city, int age) {}

    public static void main(String[] args) {
        List<Person> people = List.of(
            new Person("Ana", "NY", 30),
            new Person("Luis", "LA", 25),
            new Person("Mia", "NY", 40)
        );

        // toMap: name -> age   (needs a merge function if keys can collide)
        Map<String, Integer> ageByName = people.stream()
            .collect(Collectors.toMap(Person::name, Person::age));
        System.out.println("toMap: " + ageByName);

        // groupingBy: city -> list of people in that city
        Map<String, List<Person>> byCity = people.stream()
            .collect(Collectors.groupingBy(Person::city));
        System.out.println("groupingBy keys: " + byCity.keySet());

        // groupingBy with downstream: city -> count
        Map<String, Long> countByCity = people.stream()
            .collect(Collectors.groupingBy(Person::city, Collectors.counting()));
        System.out.println("count by city: " + countByCity);

        // partitioningBy: split into two groups by a true/false test
        Map<Boolean, List<Person>> adults = people.stream()
            .collect(Collectors.partitioningBy(p -> p.age() >= 30));
        System.out.println("adults (>=30): " + adults.get(true).size()
                         + ", others: " + adults.get(false).size());
    }
}
```
```text
toMap: {Mia=40, Ana=30, Luis=25}
groupingBy keys: [LA, NY]
count by city: {LA=1, NY=2}
adults (>=30): 2, others: 1
```
`toMap` builds a key→value map (supply a merge function like `(a,b)->a` if two elements could map to the same key, or it throws "Duplicate key"). `groupingBy` is SQL's `GROUP BY` — it can take a *downstream* collector (here `counting()`) to aggregate each group. `partitioningBy` always splits into exactly `true` and `false` buckets.

</details>

## 17. Putting It All Together

Here's a single realistic pipeline that uses Part I (lambdas, method references, functional interfaces) and Part II (intermediate + terminal ops, collectors) together — the kind of declarative code the whole series builds toward. After it, [17.1](#171-data-slicing--dicing-patterns) collects the everyday "slice and dice" patterns, and [17.2](#172-frequently-asked-faang-stream-coding-problems) walks through the stream coding problems that show up most in real FAANG interviews — each with full, runnable code.

<details>
<summary><b>▶ Example — a full real-world pipeline</b></summary>

```java
import java.util.*;
import java.util.stream.Collectors;

public class FullPipeline {
    record Order(int id, String customer, double amount, String status) {}

    public static void main(String[] args) {
        List<Order> orders = List.of(
            new Order(1, "Ana",  500, "PROCESSED"),
            new Order(2, "Luis", 80,  "PROCESSED"),
            new Order(3, "Ana",  220, "PENDING"),
            new Order(4, "Mia",  150, "PROCESSED"),
            new Order(5, "Luis", 300, "PROCESSED")
        );

        // Total revenue from PROCESSED orders over $100, grouped by customer
        Map<String, Double> revenueByCustomer = orders.stream()
            .filter(o -> o.status().equals("PROCESSED"))   // Predicate
            .filter(o -> o.amount() > 100)                  // Predicate
            .collect(Collectors.groupingBy(
                Order::customer,                            // classify by customer
                Collectors.summingDouble(Order::amount)));  // sum amounts per customer
        System.out.println("revenue by customer: " + revenueByCustomer);

        // Names of customers with a processed order, uppercased, sorted, unique
        List<String> activeCustomers = orders.stream()
            .filter(o -> o.status().equals("PROCESSED"))
            .map(Order::customer)              // Function (method reference)
            .map(String::toUpperCase)          // Function (method reference)
            .distinct()
            .sorted()
            .toList();
        System.out.println("active customers: " + activeCustomers);
    }
}
```
```text
revenue by customer: {Mia=150.0, Ana=500.0, Luis=300.0}
active customers: [ANA, LUIS, MIA]
```
Read it like a sentence: *filter to processed orders over $100, group by customer, sum their amounts.* That's the declarative promise of Part I + Part II working together — you state the **what**, and the Stream API handles the **how**.

</details>

### 17.1 Data Slicing & Dicing Patterns

These are the small, reusable recipes you combine to solve bigger problems. Each one is a one-liner you should be able to write from memory. Treat this as a pattern library.

<details>
<summary><b>▶ 1. Filter then collect — select a subset</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class P01_FilterCollect {
    public static void main(String[] args) {
        List<Integer> nums = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        List<Integer> evens = nums.stream()
            .filter(n -> n % 2 == 0)
            .toList();
        System.out.println(evens);   // [2, 4, 6, 8, 10]
    }
}
```
```text
[2, 4, 6, 8, 10]
```
The bread-and-butter pattern: `filter` keeps what passes the predicate, a terminal op materializes it.

</details>

<details>
<summary><b>▶ 2. map — transform a list into another shape</b></summary>

```java
import java.util.*;

public class P02_Map {
    record User(String name, int age) {}
    public static void main(String[] args) {
        List<User> users = List.of(new User("Ana", 30), new User("Luis", 25));

        List<String> names = users.stream().map(User::name).toList();
        System.out.println(names);   // [Ana, Luis]

        List<Integer> nameLengths = users.stream().map(u -> u.name().length()).toList();
        System.out.println(nameLengths); // [3, 4]
    }
}
```
```text
[Ana, Luis]
[3, 4]
```
Extract one field, or compute a derived value, for every element.

</details>

<details>
<summary><b>▶ 3. Sort by a field (and by multiple fields)</b></summary>

```java
import java.util.*;

public class P03_SortByField {
    record Employee(String name, String dept, int salary) {}
    public static void main(String[] args) {
        List<Employee> emps = List.of(
            new Employee("Ana", "IT", 90),
            new Employee("Luis", "HR", 70),
            new Employee("Mia", "IT", 90)
        );

        // Sort by salary descending
        emps.stream()
            .sorted(Comparator.comparingInt(Employee::salary).reversed())
            .forEach(e -> System.out.println(e.name() + " " + e.salary()));

        System.out.println("---");
        // Sort by dept asc, then salary desc, then name asc
        emps.stream()
            .sorted(Comparator.comparing(Employee::dept)
                              .thenComparing(Comparator.comparingInt(Employee::salary).reversed())
                              .thenComparing(Employee::name))
            .forEach(e -> System.out.println(e.dept() + " " + e.salary() + " " + e.name()));
    }
}
```
```text
Ana 90
Mia 90
Luis 70
---
HR 70 Luis
IT 90 Ana
IT 90 Mia
```
`Comparator.comparing(...).thenComparing(...)` is the multi-key sort interviewers love. `.reversed()` flips the order — mind its scope (it reverses whatever comparator it's attached to).

</details>

<details>
<summary><b>▶ 4. limit + skip — pagination / top-N / slicing a window</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class P04_Pagination {
    public static void main(String[] args) {
        List<Integer> data = IntStream.rangeClosed(1, 100).boxed().toList();

        int pageSize = 10, pageNumber = 3; // want page 3 (elements 21-30)

        List<Integer> page = data.stream()
            .skip((long) (pageNumber - 1) * pageSize)  // skip first 20
            .limit(pageSize)                            // take next 10
            .toList();
        System.out.println(page);  // [21, 22, ... 30]

        // Top-3 largest: sort desc, take 3
        List<Integer> top3 = data.stream()
            .sorted(Comparator.reverseOrder())
            .limit(3)
            .toList();
        System.out.println(top3);  // [100, 99, 98]
    }
}
```
```text
[21, 22, 23, 24, 25, 26, 27, 28, 29, 30]
[100, 99, 98]
```
`skip(n).limit(m)` is exactly pagination. `sorted(...).limit(k)` gives top-K.

</details>

<details>
<summary><b>▶ 5. distinct + count — unique values and frequencies</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class P05_DistinctCount {
    public static void main(String[] args) {
        List<String> tags = List.of("java", "sql", "java", "aws", "sql", "java");

        List<String> unique = tags.stream().distinct().toList();
        System.out.println(unique);  // [java, sql, aws]

        long uniqueCount = tags.stream().distinct().count();
        System.out.println(uniqueCount);  // 3

        // Frequency of each tag
        Map<String, Long> freq = tags.stream()
            .collect(Collectors.groupingBy(t -> t, Collectors.counting()));
        System.out.println(freq);  // {java=3, aws=1, sql=2}
    }
}
```
```text
[java, sql, aws]
3
{java=3, aws=1, sql=2}
```
`distinct()` for uniqueness; `groupingBy + counting` for a frequency map (note `HashMap` order isn't guaranteed — yours may print the keys in a different order).

</details>

<details>
<summary><b>▶ 6. reduce / sum / min / max — aggregate to one value</b></summary>

```java
import java.util.*;

public class P06_Aggregate {
    public static void main(String[] args) {
        List<Integer> nums = List.of(4, 8, 15, 16, 23, 42);

        int sum = nums.stream().mapToInt(Integer::intValue).sum();
        System.out.println("sum: " + sum);            // 108

        OptionalInt max = nums.stream().mapToInt(Integer::intValue).max();
        System.out.println("max: " + max.getAsInt()); // 42

        double avg = nums.stream().mapToInt(Integer::intValue).average().orElse(0);
        System.out.println("avg: " + avg);            // 18.0

        // Generic reduce (string concatenation)
        String joined = nums.stream().map(String::valueOf).reduce("", (a, b) -> a + b);
        System.out.println("concat: " + joined);      // 4815162342
    }
}
```
```text
sum: 108
max: 42
avg: 18.0
concat: 4815162342
```
Prefer primitive streams (`mapToInt`) for numeric aggregates — they offer `sum/min/max/average` directly and avoid boxing.

</details>

<details>
<summary><b>▶ 7. groupingBy with downstream — slice into buckets and aggregate</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class P07_GroupAggregate {
    record Sale(String region, String product, int amount) {}
    public static void main(String[] args) {
        List<Sale> sales = List.of(
            new Sale("US", "book", 30), new Sale("US", "pen", 5),
            new Sale("EU", "book", 40), new Sale("EU", "pen", 8),
            new Sale("US", "book", 20)
        );

        // Total amount per region
        Map<String, Integer> byRegion = sales.stream()
            .collect(Collectors.groupingBy(Sale::region, Collectors.summingInt(Sale::amount)));
        System.out.println(byRegion);   // {EU=48, US=55}

        // Product names per region (mapping downstream)
        Map<String, List<String>> productsByRegion = sales.stream()
            .collect(Collectors.groupingBy(Sale::region,
                     Collectors.mapping(Sale::product, Collectors.toList())));
        System.out.println(productsByRegion);

        // Nested grouping: region -> product -> total
        Map<String, Map<String, Integer>> nested = sales.stream()
            .collect(Collectors.groupingBy(Sale::region,
                     Collectors.groupingBy(Sale::product, Collectors.summingInt(Sale::amount))));
        System.out.println(nested);
    }
}
```
```text
{EU=48, US=55}
{EU=[book, pen], US=[book, pen, book]}
{EU={book=40, pen=8}, US={book=50, pen=5}}
```
The "dicing" workhorse: classify into buckets, then aggregate each bucket with a downstream collector. Nesting two `groupingBy`s gives a pivot-table shape. (The map is a `HashMap`, so top-level key order isn't guaranteed — the values are the point.)

</details>

<details>
<summary><b>▶ 8. partitioningBy — split into pass/fail in one pass</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class P08_Partition {
    public static void main(String[] args) {
        List<Integer> nums = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        Map<Boolean, List<Integer>> parts = nums.stream()
            .collect(Collectors.partitioningBy(n -> n % 2 == 0));

        System.out.println("evens: " + parts.get(true));   // [2,4,6,8,10]
        System.out.println("odds:  " + parts.get(false));  // [1,3,5,7,9]
    }
}
```
```text
evens: [2, 4, 6, 8, 10]
odds:  [1, 3, 5, 7, 9]
```
When you need both the "passed" and "failed" groups, `partitioningBy` gets both in a single traversal (both keys always present).

</details>

<details>
<summary><b>▶ 9. flatMap — flatten nested collections</b></summary>

```java
import java.util.*;

public class P09_Flatten {
    public static void main(String[] args) {
        List<List<Integer>> matrix = List.of(
            List.of(1, 2, 3), List.of(4, 5), List.of(6, 7, 8, 9));

        List<Integer> flat = matrix.stream()
            .flatMap(List::stream)
            .toList();
        System.out.println(flat);  // [1, 2, 3, 4, 5, 6, 7, 8, 9]

        // Words from sentences
        List<String> sentences = List.of("hello world", "foo bar");
        List<String> words = sentences.stream()
            .flatMap(s -> Arrays.stream(s.split(" ")))
            .toList();
        System.out.println(words); // [hello, world, foo, bar]
    }
}
```
```text
[1, 2, 3, 4, 5, 6, 7, 8, 9]
[hello, world, foo, bar]
```
`flatMap(List::stream)` flattens a list-of-lists; `flatMap(s -> Arrays.stream(s.split(" ")))` explodes sentences into words.

</details>

<details>
<summary><b>▶ 10. toMap — build a lookup index from a list</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class P10_ToMap {
    record Product(String sku, String name, double price) {}
    public static void main(String[] args) {
        List<Product> products = List.of(
            new Product("A1", "Book", 12.5),
            new Product("B2", "Pen", 1.5),
            new Product("C3", "Mug", 8.0));

        // sku -> Product (fast lookup index)
        Map<String, Product> bySku = products.stream()
            .collect(Collectors.toMap(Product::sku, p -> p));
        System.out.println(bySku.get("B2").name());  // Pen

        // sku -> price
        Map<String, Double> priceBySku = products.stream()
            .collect(Collectors.toMap(Product::sku, Product::price));
        System.out.println(priceBySku);  // {A1=12.5, B2=1.5, C3=8.0}
    }
}
```
```text
Pen
{A1=12.5, B2=1.5, C3=8.0}
```
Turning a list into a `Map` keyed by an id is one of the most common real tasks. Remember: add a merge function `(a,b)->a` if keys can collide.

</details>

### 17.2 Frequently-Asked FAANG Stream Coding Problems

These are the actual stream problems that recur in interviews and online assessments. For each: the problem, the idea, and full runnable code with output. Many have a "follow-up" an interviewer adds — note those.

<details>
<summary><b>▶ 1. Find the first non-repeated character in a String</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class FAANG01_FirstNonRepeated {
    static Character firstNonRepeated(String s) {
        Map<Character, Long> counts = s.chars()
            .mapToObj(c -> (char) c)
            .collect(Collectors.groupingBy(c -> c, LinkedHashMap::new, Collectors.counting()));
        //                                        ^ LinkedHashMap preserves first-seen order

        return counts.entrySet().stream()
            .filter(e -> e.getValue() == 1)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }

    public static void main(String[] args) {
        System.out.println(firstNonRepeated("swiss"));      // w
        System.out.println(firstNonRepeated("aabbcc"));     // null
        System.out.println(firstNonRepeated("leetcode"));   // l
    }
}
```
```text
w
null
l
```
**Idea:** count chars into a *`LinkedHashMap`* (so insertion order is kept), then `findFirst` with count == 1. The `LinkedHashMap::new` supplier is the trick — a plain `HashMap` would lose order. **Follow-up:** "do it in one pass without a map?" — you can't easily; this two-pass approach is the expected answer.

</details>

<details>
<summary><b>▶ 2. Count frequency of each word and find the most frequent</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class FAANG02_WordFrequency {
    public static void main(String[] args) {
        String text = "the quick brown fox the lazy dog the fox";

        Map<String, Long> freq = Arrays.stream(text.split(" "))
            .collect(Collectors.groupingBy(w -> w, Collectors.counting()));
        System.out.println(freq);

        // Most frequent word
        String top = freq.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("");
        System.out.println("most frequent: " + top);   // the

        // Top 2 by frequency (sorted desc)
        List<String> top2 = freq.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(2)
            .map(Map.Entry::getKey)
            .toList();
        System.out.println("top 2: " + top2);
    }
}
```
```text
{the=3, quick=1, lazy=1, brown=1, dog=1, fox=2}
most frequent: the
top 2: [the, fox]
```
**Idea:** `groupingBy + counting` builds the frequency map (its key order is a `HashMap`'s, so don't rely on it); `max(comparingByValue())` or `sorted(...).limit(k)` finds the top. **Follow-up:** "break ties alphabetically" — add `.thenComparing(Map.Entry.comparingByKey())`.

</details>

<details>
<summary><b>▶ 3. Group a list of employees by department</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class FAANG03_GroupByDept {
    record Employee(String name, String dept, int salary) {}
    public static void main(String[] args) {
        List<Employee> emps = List.of(
            new Employee("Ana", "IT", 90), new Employee("Luis", "HR", 70),
            new Employee("Mia", "IT", 110), new Employee("Sam", "HR", 60));

        // dept -> list of names
        Map<String, List<String>> namesByDept = emps.stream()
            .collect(Collectors.groupingBy(Employee::dept,
                     Collectors.mapping(Employee::name, Collectors.toList())));
        System.out.println(namesByDept);

        // dept -> highest-paid employee
        Map<String, Optional<Employee>> topPaid = emps.stream()
            .collect(Collectors.groupingBy(Employee::dept,
                     Collectors.maxBy(Comparator.comparingInt(Employee::salary))));
        topPaid.forEach((d, e) -> System.out.println(d + " -> " + e.get().name()));

        // dept -> average salary
        Map<String, Double> avgByDept = emps.stream()
            .collect(Collectors.groupingBy(Employee::dept,
                     Collectors.averagingInt(Employee::salary)));
        System.out.println(avgByDept);
    }
}
```
```text
{HR=[Luis, Sam], IT=[Ana, Mia]}
HR -> Luis
IT -> Mia
{HR=65.0, IT=100.0}
```
**Idea:** classic `groupingBy` with three different downstream collectors (`mapping`, `maxBy`, `averagingInt`). This single question tests whether you can compose collectors fluently. (Maps are `HashMap`s — key order isn't guaranteed.)

</details>

<details>
<summary><b>▶ 4. Sum of squares of even numbers</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class FAANG04_SumSquaresEven {
    public static void main(String[] args) {
        List<Integer> nums = List.of(1, 2, 3, 4, 5, 6);

        int result = nums.stream()
            .filter(n -> n % 2 == 0)   // 2, 4, 6
            .mapToInt(n -> n * n)       // 4, 16, 36
            .sum();                     // 56
        System.out.println(result);    // 56
    }
}
```
```text
56
```
**Idea:** filter → mapToInt → sum. A warm-up that checks you reach for primitive streams for numeric work.

</details>

<details>
<summary><b>▶ 5. Find duplicate elements in a list</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class FAANG05_FindDuplicates {
    public static void main(String[] args) {
        List<Integer> nums = List.of(1, 2, 3, 2, 4, 5, 1, 6, 1);

        // Approach A: frequency map, keep entries with count > 1
        Set<Integer> dupes = nums.stream()
            .collect(Collectors.groupingBy(n -> n, Collectors.counting()))
            .entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        System.out.println(dupes);   // [1, 2]

        // Approach B: a Set.add() returns false when the element already exists
        Set<Integer> seen = new HashSet<>();
        Set<Integer> dupes2 = nums.stream()
            .filter(n -> !seen.add(n))   // add returns false → it's a duplicate
            .collect(Collectors.toSet());
        System.out.println(dupes2);  // [1, 2]
    }
}
```
```text
[1, 2]
[1, 2]
```
**Idea:** Two idiomatic approaches. Approach B (`!seen.add(n)`) is clever and concise but uses stateful side-effects — fine sequentially, **unsafe in parallel**. Mention that trade-off; interviewers probe it.

</details>

<details>
<summary><b>▶ 6. Convert List to Map (and handle duplicate keys)</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class FAANG06_ListToMap {
    record Person(int id, String name, String city) {}
    public static void main(String[] args) {
        List<Person> people = List.of(
            new Person(1, "Ana", "NY"), new Person(2, "Luis", "LA"),
            new Person(3, "Mia", "NY"));

        // id -> name (ids unique, no merge needed)
        Map<Integer, String> byId = people.stream()
            .collect(Collectors.toMap(Person::id, Person::name));
        System.out.println(byId);

        // city -> names (DUPLICATE keys! must merge)
        Map<String, String> namesByCity = people.stream()
            .collect(Collectors.toMap(Person::city, Person::name, (a, b) -> a + ", " + b));
        System.out.println(namesByCity);  // {LA=Luis, NY=Ana, Mia}
    }
}
```
```text
{1=Ana, 2=Luis, 3=Mia}
{LA=Luis, NY=Ana, Mia}
```
**Idea:** `toMap` with and without a merge function. The duplicate-key case (city "NY" appears twice) is the exact trap interviewers set — without the merge function it throws `IllegalStateException`.

</details>

<details>
<summary><b>▶ 7. Partition numbers into primes and non-primes</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class FAANG07_PartitionPrimes {
    static boolean isPrime(int n) {
        if (n < 2) return false;
        return IntStream.rangeClosed(2, (int) Math.sqrt(n)).noneMatch(i -> n % i == 0);
    }
    public static void main(String[] args) {
        Map<Boolean, List<Integer>> parts = IntStream.rangeClosed(2, 20).boxed()
            .collect(Collectors.partitioningBy(FAANG07_PartitionPrimes::isPrime));

        System.out.println("primes:     " + parts.get(true));
        System.out.println("non-primes: " + parts.get(false));
    }
}
```
```text
primes:     [2, 3, 5, 7, 11, 13, 17, 19]
non-primes: [4, 6, 8, 9, 10, 12, 14, 15, 16, 18, 20]
```
**Idea:** `partitioningBy` + a helper predicate. Note the elegant prime test using `noneMatch` over a range — a nice "streams within streams" touch.

</details>

<details>
<summary><b>▶ 8. Find the Nth highest / second-largest element</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class FAANG08_NthHighest {
    public static void main(String[] args) {
        List<Integer> nums = List.of(10, 5, 20, 8, 20, 15, 3);

        // Second largest DISTINCT value
        Optional<Integer> secondLargest = nums.stream()
            .distinct()
            .sorted(Comparator.reverseOrder())
            .skip(1)        // skip the largest
            .findFirst();   // next one
        System.out.println(secondLargest.orElse(-1));  // 15

        // Generalize: Nth highest distinct (N=3)
        int n = 3;
        Optional<Integer> nth = nums.stream()
            .distinct()
            .sorted(Comparator.reverseOrder())
            .skip(n - 1)
            .findFirst();
        System.out.println(nth.orElse(-1));  // 10
    }
}
```
```text
15
10
```
**Idea:** `distinct().sorted(reverseOrder()).skip(n-1).findFirst()`. The `distinct()` matters — interviewers often clarify "second *distinct* largest" to catch you (20 appears twice here).

</details>

<details>
<summary><b>▶ 9. Join a list of strings with a delimiter (and prefix/suffix)</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class FAANG09_Joining {
    public static void main(String[] args) {
        List<String> names = List.of("Ana", "Luis", "Mia");

        String csv = names.stream().collect(Collectors.joining(", "));
        System.out.println(csv);   // Ana, Luis, Mia

        String bracketed = names.stream().collect(Collectors.joining(", ", "[", "]"));
        System.out.println(bracketed);   // [Ana, Luis, Mia]

        // Join uppercased names
        String upper = names.stream()
            .map(String::toUpperCase)
            .collect(Collectors.joining(" | "));
        System.out.println(upper);  // ANA | LUIS | MIA
    }
}
```
```text
Ana, Luis, Mia
[Ana, Luis, Mia]
ANA | LUIS | MIA
```
**Idea:** `Collectors.joining` has a 1-arg (delimiter) and 3-arg (delimiter, prefix, suffix) form. Very common for building CSV or display strings.

</details>

<details>
<summary><b>▶ 10. Check if any / all / none match a condition</b></summary>

```java
import java.util.*;

public class FAANG10_Matching {
    record Order(int id, String status) {}
    public static void main(String[] args) {
        List<Order> orders = List.of(
            new Order(1, "SHIPPED"), new Order(2, "SHIPPED"), new Order(3, "PENDING"));

        boolean anyPending = orders.stream().anyMatch(o -> o.status().equals("PENDING"));
        boolean allShipped = orders.stream().allMatch(o -> o.status().equals("SHIPPED"));
        boolean noneCancelled = orders.stream().noneMatch(o -> o.status().equals("CANCELLED"));

        System.out.println("any pending:   " + anyPending);   // true
        System.out.println("all shipped:   " + allShipped);   // false
        System.out.println("none cancelled:" + noneCancelled);// true
    }
}
```
```text
any pending:   true
all shipped:   false
none cancelled:true
```
**Idea:** the three short-circuiting boolean matchers. They stop as soon as the answer is determined — efficient for "is there at least one...?" style checks.

</details>

<details>
<summary><b>▶ 11. Sort a Map by value (top spenders, leaderboard)</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class FAANG11_SortMapByValue {
    public static void main(String[] args) {
        Map<String, Integer> scores = Map.of("Ana", 85, "Luis", 92, "Mia", 78, "Sam", 92);

        // Extract the comparator to a variable — chaining reversed().thenComparing()
        // inline inside sorted(...) can defeat type inference and fail to compile.
        Comparator<Map.Entry<String, Integer>> byValueDescThenKey =
            Map.Entry.<String, Integer>comparingByValue().reversed()
                     .thenComparing(Map.Entry.comparingByKey());

        // Sort entries by value desc (key tie-breaker), then collect into a LinkedHashMap
        LinkedHashMap<String, Integer> sorted = scores.entrySet().stream()
            .sorted(byValueDescThenKey)
            .collect(Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue,
                (a, b) -> a, LinkedHashMap::new));  // LinkedHashMap keeps sorted order
        System.out.println(sorted);
    }
}
```
```text
{Luis=92, Sam=92, Ana=85, Mia=78}
```
**Idea:** stream the `entrySet`, sort by value (with a key tie-breaker), then collect into a `LinkedHashMap` to *preserve* the sorted order. Collecting into a plain `HashMap` would scramble it — the `LinkedHashMap::new` supplier is essential. **Gotcha worth knowing:** chaining `comparingByValue().reversed().thenComparing(...)` *inline* inside `sorted(...)` often fails to compile due to generic inference — assign it to a typed `Comparator` variable first (as shown).

</details>

<details>
<summary><b>▶ 12. Flatten and deduplicate across nested lists (e.g. all unique skills)</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class FAANG12_FlattenDistinct {
    record Dev(String name, List<String> skills) {}
    public static void main(String[] args) {
        List<Dev> devs = List.of(
            new Dev("Ana", List.of("java", "sql")),
            new Dev("Luis", List.of("java", "aws", "sql")),
            new Dev("Mia", List.of("python", "aws")));

        // All unique skills across the team, sorted
        List<String> allSkills = devs.stream()
            .flatMap(d -> d.skills().stream())
            .distinct()
            .sorted()
            .toList();
        System.out.println(allSkills);  // [aws, java, python, sql]

        // Skill -> how many devs have it
        Map<String, Long> skillCount = devs.stream()
            .flatMap(d -> d.skills().stream())
            .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        System.out.println(skillCount);
    }
}
```
```text
[aws, java, python, sql]
{python=1, java=2, aws=2, sql=2}
```
**Idea:** `flatMap` to merge the nested skill lists into one stream, then `distinct().sorted()` for the unique set, or `groupingBy + counting` for popularity. The flatten-then-aggregate combo is extremely common. (The count map is a `HashMap`, so key order varies.)

</details>

<details>
<summary><b>▶ 13. Generate a stream — Fibonacci / running series</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class FAANG13_Generate {
    public static void main(String[] args) {
        // First 10 Fibonacci numbers using Stream.iterate with a pair
        List<Integer> fib = Stream.iterate(new int[]{0, 1}, a -> new int[]{a[1], a[0] + a[1]})
            .limit(10)
            .map(a -> a[0])
            .toList();
        System.out.println(fib);  // [0, 1, 1, 2, 3, 5, 8, 13, 21, 34]

        // First 5 powers of 2
        List<Integer> powers = Stream.iterate(1, n -> n * 2).limit(5).toList();
        System.out.println(powers);  // [1, 2, 4, 8, 16]
    }
}
```
```text
[0, 1, 1, 2, 3, 5, 8, 13, 21, 34]
[1, 2, 4, 8, 16]
```
**Idea:** `Stream.iterate(seed, next)` generates an infinite sequence; always bound it with `limit(n)`. The Fibonacci "carry a pair in an int[]" trick is a known interview favorite.

</details>

<details>
<summary><b>▶ 14. Average / statistics in a single pass</b></summary>

```java
import java.util.*;

public class FAANG14_Statistics {
    record Product(String name, double price) {}
    public static void main(String[] args) {
        List<Product> products = List.of(
            new Product("Book", 12.5), new Product("Pen", 1.5),
            new Product("Mug", 8.0), new Product("Bag", 25.0));

        DoubleSummaryStatistics stats = products.stream()
            .mapToDouble(Product::price)
            .summaryStatistics();

        System.out.println("count: " + stats.getCount());     // 4
        System.out.println("sum:   " + stats.getSum());       // 47.0
        System.out.println("min:   " + stats.getMin());       // 1.5
        System.out.println("max:   " + stats.getMax());       // 25.0
        System.out.println("avg:   " + stats.getAverage());   // 11.75
    }
}
```
```text
count: 4
sum:   47.0
min:   1.5
max:   25.0
avg:   11.75
```
**Idea:** `summaryStatistics()` computes count, sum, min, max, and average in **one** traversal. Beats calling `.average()`, `.max()`, etc. separately (which would be multiple passes).

</details>

<details>
<summary><b>▶ 15. Find common elements between two lists (intersection)</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class FAANG15_Intersection {
    public static void main(String[] args) {
        List<Integer> a = List.of(1, 2, 3, 4, 5, 6);
        List<Integer> b = List.of(4, 5, 6, 7, 8);

        Set<Integer> setB = new HashSet<>(b);   // O(1) lookups
        List<Integer> intersection = a.stream()
            .filter(setB::contains)
            .toList();
        System.out.println(intersection);  // [4, 5, 6]

        // Elements in A but not in B (difference)
        List<Integer> diff = a.stream()
            .filter(x -> !setB.contains(x))
            .toList();
        System.out.println(diff);  // [1, 2, 3]
    }
}
```
```text
[4, 5, 6]
[1, 2, 3]
```
**Idea:** put one list in a `HashSet` for O(1) `contains`, then `filter`. Naively using `b.contains` inside the filter would be O(n·m) — the set turns it into O(n+m). Interviewers care about that complexity point.

</details>

<details>
<summary><b>▶ 16. Count occurrences of each character (anagram grouping)</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class FAANG16_GroupAnagrams {
    static String signature(String s) {
        return s.chars().sorted()
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
    }
    public static void main(String[] args) {
        List<String> words = List.of("eat", "tea", "tan", "ate", "nat", "bat");

        // Group words that are anagrams of each other (same sorted letters)
        Map<String, List<String>> groups = words.stream()
            .collect(Collectors.groupingBy(FAANG16_GroupAnagrams::signature));
        System.out.println(new ArrayList<>(groups.values()));
    }
}
```
```text
[[eat, tea, ate], [bat], [tan, nat]]
```
**Idea:** the classic "group anagrams" problem solved with streams — the key is a *signature* (sorted characters), and `groupingBy` buckets words sharing it. The `chars().sorted().collect(...)` builds the sorted-letter signature without boxing each char. (Bucket order isn't guaranteed — `groupingBy` returns a `HashMap`; the words *within* each bucket keep encounter order.)

</details>

<details>
<summary><b>▶ 17. Find max/min object by a field</b></summary>

```java
import java.util.*;

public class FAANG17_MaxByField {
    record Employee(String name, int salary) {}
    public static void main(String[] args) {
        List<Employee> emps = List.of(
            new Employee("Ana", 90), new Employee("Luis", 120), new Employee("Mia", 75));

        Optional<Employee> highest = emps.stream()
            .max(Comparator.comparingInt(Employee::salary));
        highest.ifPresent(e -> System.out.println("highest: " + e.name())); // Luis

        Optional<Employee> lowest = emps.stream()
            .min(Comparator.comparingInt(Employee::salary));
        lowest.ifPresent(e -> System.out.println("lowest: " + e.name()));   // Mia
    }
}
```
```text
highest: Luis
lowest: Mia
```
**Idea:** `max`/`min` with a `Comparator.comparingInt(...)` finds the extreme *object* by a field (not just the field value). Returns `Optional` because the stream could be empty.

</details>

<details>
<summary><b>▶ 18. Filter a Map by entries (keep entries matching a condition)</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class FAANG18_FilterMap {
    public static void main(String[] args) {
        Map<String, Integer> inventory = Map.of(
            "apple", 50, "banana", 0, "cherry", 12, "date", 0);

        // Keep only in-stock items (value > 0)
        Map<String, Integer> inStock = inventory.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        System.out.println(inStock);  // {apple=50, cherry=12}

        // Just the names of out-of-stock items, sorted
        List<String> outOfStock = inventory.entrySet().stream()
            .filter(e -> e.getValue() == 0)
            .map(Map.Entry::getKey)
            .sorted()
            .toList();
        System.out.println(outOfStock);  // [banana, date]
    }
}
```
```text
{apple=50, cherry=12}
[banana, date]
```
**Idea:** to "filter a Map", stream its `entrySet()`, filter, then collect back to a map (or extract keys/values). Maps have no `.filter()` of their own — this is the idiom.

</details>

<details>
<summary><b>▶ 19. Sum grouped values / build a summary report</b></summary>

```java
import java.util.*;
import java.util.stream.*;

public class FAANG19_SummaryReport {
    record Txn(String category, String type, double amount) {}
    public static void main(String[] args) {
        List<Txn> txns = List.of(
            new Txn("food", "debit", 30), new Txn("food", "debit", 20),
            new Txn("salary", "credit", 5000), new Txn("rent", "debit", 1200),
            new Txn("food", "credit", 10));

        // Total spent (debits) per category
        Map<String, Double> spentPerCategory = txns.stream()
            .filter(t -> t.type().equals("debit"))
            .collect(Collectors.groupingBy(Txn::category, Collectors.summingDouble(Txn::amount)));
        System.out.println(spentPerCategory);  // {rent=1200.0, food=50.0}

        // Net total (credits - debits)
        double net = txns.stream()
            .mapToDouble(t -> t.type().equals("credit") ? t.amount() : -t.amount())
            .sum();
        System.out.println("net: " + net);  // 5000 +10 -30 -20 -1200 = 3760.0
    }
}
```
```text
{rent=1200.0, food=50.0}
net: 3760.0
```
**Idea:** combine `filter`, `groupingBy`, and `summingDouble` for a per-bucket report; use a ternary inside `mapToDouble` to compute signed net totals. This mirrors real reporting/aggregation tasks.

</details>

<details>
<summary><b>▶ 20. Remove duplicate objects by a key field (distinct-by)</b></summary>

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.*;

public class FAANG20_DistinctByKey {
    // Helper: distinct by an extracted key (Java has no built-in distinctBy)
    static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    record Person(String name, String email) {}
    public static void main(String[] args) {
        List<Person> people = List.of(
            new Person("Ana", "ana@x.com"),
            new Person("Ana2", "ana@x.com"),   // same email
            new Person("Luis", "luis@x.com"));

        List<Person> unique = people.stream()
            .filter(distinctByKey(Person::email))
            .toList();
        unique.forEach(p -> System.out.println(p.name() + " " + p.email()));
    }
}
```
```text
Ana ana@x.com
Luis luis@x.com
```
**Idea:** Java's `distinct()` uses `equals` on the whole object — there's no built-in "distinct by a field." The standard trick is a stateful `Predicate` backed by a `Set` that returns `true` only the first time a key is seen. Mention it's a stateful filter (use a thread-safe set if parallel). This is a *very* common "do you know the workaround?" question.

</details>

---

# Reference

## 18. FAANG Interview Questions

Read the question, answer it in your head, then expand to check. These cover everything in both parts and are phrased the way interviewers actually ask them.

### Part I — Functional Interfaces & Lambdas

<details>
<summary><b>Q1. What is a functional interface? What does <code>@FunctionalInterface</code> do?</b></summary>

A functional interface is an interface with **exactly one abstract method** (the single abstract method, or SAM). It may also have any number of `default` and `static` methods — those don't count toward the limit. The single abstract method defines the "function signature": its parameters, return type, and name. `@FunctionalInterface` is an optional annotation that asks the compiler to *verify* the interface qualifies — if someone later adds a second abstract method, the build fails. It documents intent and prevents accidental breakage.

</details>

<details>
<summary><b>Q2. Is a lambda the same as an anonymous class? What's the real difference?</b></summary>

They achieve the same result (both implement a functional interface), but they're not identical. An anonymous class **explicitly creates a full object** with an overridden method and its own `this`. A lambda focuses only on the **logic**, far more concisely, and the compiler generates the implementation in an optimized way (via `invokedynamic`, not a separate `.class` file per lambda). Also, inside a lambda `this` refers to the **enclosing instance**, while in an anonymous class `this` refers to the **anonymous object itself**. So lambdas are cleaner *and* behave differently with `this`.

</details>

<details>
<summary><b>Q3. Why can a lambda be assigned to a variable? What's actually stored?</b></summary>

Because a functional interface is a real **type**. When you write `Predicate<Integer> p = n -> n > 2`, Java creates an **object** that implements `Predicate<Integer>`, with the lambda body as its `test` method. The variable doesn't store `true`/`false` or "a function" — it stores that object (the behavior). The logic runs only when you call `p.test(...)`. This is how Java simulates "functions as values" while staying object-oriented.

</details>

<details>
<summary><b>Q4. Name the four kinds of method reference with an example of each.</b></summary>

(1) **Static**: `Integer::parseInt` ≡ `s -> Integer.parseInt(s)`. (2) **Instance method of a specific object**: `System.out::println` ≡ `x -> System.out.println(x)`. (3) **Instance method of an arbitrary object of a type**: `String::toUpperCase` ≡ `s -> s.toUpperCase()` (the argument becomes the receiver). (4) **Constructor**: `ArrayList::new` ≡ `() -> new ArrayList<>()`. Use a method reference only when the lambda *just* forwards to an existing method and adds no extra logic.

</details>

<details>
<summary><b>Q5. When can you NOT use a method reference?</b></summary>

When the lambda does more than directly call one existing method — any validation, calculation, condition, or combining of values. For example `x -> x.startsWith("a")` can't be a method reference (it calls `startsWith` *with an argument and that's logic specific to this call site*), but `x -> x.toUpperCase()` can become `String::toUpperCase`. Rule of thumb: if the lambda body is exactly one method call with no added work, it's a candidate.

</details>

<details>
<summary><b>Q6. Explain <code>Predicate</code>, <code>Function</code>, <code>Consumer</code>, and <code>Supplier</code> — method and shape.</b></summary>

`Predicate<T>` → `boolean test(T)`, a yes/no test (used by `filter`). `Function<T,R>` → `R apply(T)`, transforms one value into another (used by `map`). `Consumer<T>` → `void accept(T)`, takes a value and returns nothing, a side-effect sink (used by `forEach`). `Supplier<T>` → `T get()`, takes nothing and produces a value, a factory/lazy provider. Knowing these four by heart is essential because the Stream API methods are defined in terms of them.

</details>

<details>
<summary><b>Q7. What's the difference between <code>Function</code> and <code>UnaryOperator</code>? <code>BiFunction</code> and <code>BinaryOperator</code>?</b></summary>

`UnaryOperator<T>` is a special case of `Function<T,T>` where the input and output types are the **same**. `BinaryOperator<T>` is a special case of `BiFunction<T,T,T>` where both inputs and the output are all the **same** type — this is the shape `reduce` uses to combine two elements into one. They exist as convenient, readable aliases when the types line up.

</details>

<details>
<summary><b>Q8. What is autoboxing in functional interfaces, and how do you avoid it?</b></summary>

Generics only work with object types, so `Function<Integer,Integer>` stores `Integer` objects. When you pass a primitive `int`, Java wraps it into an `Integer` (`int → Integer`) — that's **autoboxing**. With large data this is inefficient (an allocation per value). To avoid it, use the **primitive specializations**: `IntPredicate`, `IntFunction<R>`, `IntUnaryOperator`, `ToIntFunction<T>`, etc. (and `Long`/`Double` variants), plus primitive streams like `IntStream`. They operate on raw primitives, no boxing.

</details>

<details>
<summary><b>Q9. How does type inference work for lambda parameters?</b></summary>

You usually don't write the parameter types; the compiler infers them from the **target functional interface's generics**. If a method expects `Predicate<Order>`, then in `order -> order.amount > 100` the compiler knows `order` is an `Order`. This is why lambdas are so concise — the type information is already carried by the functional interface, so repeating it would be redundant.

</details>

<details>
<summary><b>Q10. Before Java 8, how did you pass behavior into a method? Why was it painful?</b></summary>

You wrapped the behavior in an **anonymous class** implementing a single-method interface (`Runnable`, `Comparator`, etc.). The compiler generated a real class, overrode the method, and instantiated it at runtime. It worked, but it was extremely verbose: every bit of real logic was buried under `new`, `@Override`, method signatures, and braces. Java 8's lambdas removed that ceremony while keeping the same underlying mechanism (an object implementing a functional interface).

</details>

### Part II — Streams

<details>
<summary><b>[Important] Why are Java Streams efficient?</b></summary>
Java Streams are efficient because they use **lazy evaluation**, **one-by-one processing**, **pipeline fusion**, and **short-circuiting**, which reduce memory usage and unnecessary computations.

### 1. Lazy Evaluation
Streams are **lazy**. Intermediate operations (`filter`, `map`, `sorted`) do not execute immediately; they only build a pipeline.

```java
Stream<String> s = names.stream()
                        .filter(n -> n.length() > 3)
                        .map(String::toUpperCase);
```

No processing happens until a terminal operation is called.

---

### 2. Execution Starts Only with a Terminal Operation
A terminal operation (`collect`, `count`, `findFirst`, `average`, etc.) triggers execution.

```java
List<String> result = s.collect(Collectors.toList());
```

Think of it as **switching ON the assembly line**.

---

### 3. Streams Process Elements One-by-One (Vertical Processing)

```java
names.stream()
     .filter(n -> n.length() > 3)
     .map(String::toUpperCase)
     .collect(Collectors.toList());
```

Input:

```text
[Alice, Bob, Charlie, Dave]
```

❌ Horizontal Processing (Not Streams)

```text
Filter everyone
↓
[Alice, Charlie, Dave]

Map everyone
↓
[ALICE, CHARLIE, DAVE]
```

✅ Vertical Processing (Actual Streams)

```text
Alice
  ↓ Filter ✓
  ↓ Map
  ↓ Collect

Bob
  ↓ Filter ✗
  ↓ Discard

Charlie
  ↓ Filter ✓
  ↓ Map
  ↓ Collect
```
> Collections process data **horizontally (stage-by-stage)**, while Streams process data **vertically (one item through all stages at a time)**.

---

### 4. No Intermediate Collections
Streams avoid creating temporary collections for stateless operations.

❌ Traditional

```java
List<String> filtered = ...;
List<String> upper = ...;
```

✅ Streams

```java
List<String> result = names.stream()
                           .filter(n -> n.length() > 3)
                           .map(String::toUpperCase)
                           .toList();
```

Only the final result list is created.

---

### 5. Short-Circuiting Saves Work
Operations like `findFirst()`, `anyMatch()`, and `limit()` stop processing as soon as enough data is found.

```java
String first = names.stream()
                    .filter(n -> n.length() > 3)
                    .findFirst()
                    .orElse("");
```

```text
Alice ✓
↓
findFirst()
↓
STOP
```

Bob, Charlie and Dave are never processed.

---

### 6. Infinite Streams Are Possible

```java
List<Double> nums = Stream.generate(Math::random)
                          .filter(n -> n > 0.5)
                          .limit(5)
                          .toList();
```

```text
0.12 ✗
0.81 ✓
0.77 ✓
0.65 ✓
0.91 ✓
0.88 ✓

Got 5 values
↓
STOP
```

Without laziness, infinite streams would cause memory issues.

---

### 7. Put Cheap Filters Before Expensive Operations

❌ Bad

```java
users.stream()
     .map(this::fetchUserFromDB)
     .filter(User::isAdmin)
     .findFirst();
```

May fetch hundreds of users.

✅ Better

```java
userIds.stream()
       .filter(id -> id.startsWith("admin"))
       .map(this::fetchUserFromDB)
       .findFirst();
```

Filter first, then do expensive work.

---

### 8. Pipeline Fusion (Single Traversal)

```java
stream()
.filter(...)
.map(...)
.collect(...)
```

Internally behaves like:

```text
Item
 ↓
Filter
 ↓
Map
 ↓
Collect
```

Instead of multiple passes over the collection.

---

### 9. Not All Stream Operations Are Equally Efficient

Examples:

```java
sorted()
distinct()
groupingBy()
```

These may require buffering elements internally.

---

### 10. Stateless Operations Use O(1) Memory

Examples:

```java
filter()
map()
peek()
```

Only the current element is needed.

```text
Memory: O(1)
```

---

### 11. Stateful Operations May Use O(N) Memory

Examples:

```java
sorted()
distinct()
groupingBy()
```

```java
stream()
.sorted()
.collect(toList());
```

Sorting requires seeing all elements first.

```text
Memory: O(N)
```

---

### 12. For Small Collections, Loops Can Be Faster

```java
int sum = 0;
for (int n : nums) {
    sum += n;
}
```

may outperform

```java
int sum = nums.stream()
              .mapToInt(Integer::intValue)
              .sum();
```

because streams have lambda and pipeline overhead.

---

## Quick Revision Table

| Feature | Benefit |
|---------|---------|
| Lazy Evaluation | Work only when needed |
| Terminal Operation | Starts execution |
| One-by-One Processing | No unnecessary storage |
| No Intermediate Collections | Lower memory & GC |
| Short-Circuiting | Stops early |
| Infinite Streams | Safe processing of endless data |
| Cheap Filter First | Avoid expensive computations |
| Pipeline Fusion | Single traversal |
| Stateless Ops | O(1) memory |
| Stateful Ops | May require O(N) memory |
| Small Data | Loops may be faster |

### One-Line Interview Answer

> Java Streams are efficient because they use lazy evaluation, process elements one-by-one through a fused pipeline, avoid intermediate collections, support short-circuiting (`findFirst`, `anyMatch`, `limit`), and can safely handle infinite data sources. However, stateful operations like `sorted()` and `distinct()` may require additional memory, and simple loops can be faster for very small datasets.

</details>

<details>
<summary><b>Q11. What is a Stream? Is it a data structure?</b></summary>

A Stream is **not** a data structure and **not** a collection — it stores no elements and holds no copy of the source. It's a **pipeline of operations** over a flow of elements. Think of a conveyor belt: data from a collection flows through stations (filter, map, sort), and a terminal operation collects the result. It reads from the source but never modifies it, and it's consumed once.

</details>

<details>
<summary><b>Q12. Intermediate vs terminal operations — define both and give examples.</b></summary>

**Intermediate** operations return a new `Stream` and are **lazy** — they build a plan but process nothing until triggered. Examples: `filter`, `map`, `sorted`, `distinct`, `flatMap`, `limit`, `skip`, `peek`. **Terminal** operations end the pipeline, **trigger execution**, and produce a result (value, collection, or side effect); the stream is then consumed. Examples: `forEach`, `collect`, `count`, `reduce`, `findFirst`, `anyMatch`, `toArray`. A pipeline with no terminal operation does nothing at all.

</details>

<details>
<summary><b>Q13. What is lazy evaluation in streams, and why does it matter?</b></summary>

Lazy evaluation means intermediate operations don't run when you call them — they only record what to do. Execution begins **only when a terminal operation** is invoked, and then elements flow through the whole pipeline. It matters because it lets the stream avoid unnecessary work (e.g. short-circuiting with `findFirst`/`anyMatch`/`limit` can stop early) and process each element through all stages in one pass rather than building intermediate collections.

</details>

<details>
<summary><b>Q14. What's the difference between <code>map</code> and <code>flatMap</code>?</b></summary>

`map` transforms each element into exactly one new element (one in → one out), so `Stream<Department>` mapped by `getEmployees` becomes `Stream<List<Employee>>` — nested. `flatMap` transforms each element into a **stream** and then **flattens** all those streams into one, so the same data becomes a single `Stream<Employee>`. Use `flatMap` when each element contains a collection (or another stream) and you want one flat result instead of a stream of lists.

</details>

<details>
<summary><b>Q15. Why does <code>findFirst</code> return an <code>Optional</code>?</b></summary>

Because the stream might be **empty**, in which case there is no first element. Returning `Optional<T>` forces the caller to handle the "nothing there" case explicitly (via `ifPresent`, `orElse`, `orElseThrow`) instead of returning `null` and risking a `NullPointerException`. It makes absence part of the type, which is safer and more honest.

</details>

<details>
<summary><b>Q16. Explain <code>reduce</code> and its two main forms.</b></summary>

`reduce` folds all elements into a single value using a `BinaryOperator` (combine two same-typed values into one). With an **identity** seed — `reduce(0, (a,b)->a+b)` — it always returns a plain value (the identity itself for an empty stream). Without an identity — `reduce((a,b)->a*b)` — it returns an `Optional<T>`, because an empty stream has nothing to combine. Use it for sums, products, min/max, concatenation, etc.

</details>

<details>
<summary><b>Q17. Can you reuse a Stream after a terminal operation?</b></summary>

No. Once a terminal operation runs, the stream is **consumed** and any further use throws `IllegalStateException: stream has already been operated upon or closed`. If you need to traverse the data again, create a fresh stream from the source collection (or wrap the source in a `Supplier<Stream<T>>` and call it again).

</details>

<details>
<summary><b>Q18. What does <code>Collectors.toMap</code> do when two elements produce the same key?</b></summary>

By default it throws `IllegalStateException: Duplicate key`. To handle collisions you supply a **merge function** as the third argument: `toMap(keyFn, valFn, (existing, replacement) -> ...)` — e.g. keep the first `(a,b)->a`, keep the last `(a,b)->b`, or combine them. If you actually want *all* values per key, use `groupingBy` instead, which yields a `Map<K, List<V>>`.

</details>

<details>
<summary><b>Q19. Difference between <code>groupingBy</code> and <code>partitioningBy</code>?</b></summary>

`groupingBy(classifier)` splits elements into any number of groups keyed by the classifier's return value, producing `Map<K, List<T>>`, and can take a downstream collector (like `counting()` or `summingInt()`) to aggregate each group. `partitioningBy(predicate)` always splits into exactly **two** groups keyed `true` and `false` (both keys always present, even if one is empty). Use partitioning for a boolean split, grouping for general multi-way classification.

</details>

<details>
<summary><b>Q20. What's the difference between <code>Collectors.toList()</code> and <code>Stream.toList()</code>?</b></summary>

`Collectors.toList()` (Java 8) returns a list with no strong guarantee about mutability (typically a mutable `ArrayList`). `Stream.toList()` (Java 16+) is more concise and returns an **unmodifiable** list — calling `.add()` on it throws `UnsupportedOperationException`. Prefer `Stream.toList()` for read-only results; use `collect(Collectors.toCollection(ArrayList::new))` when you specifically need a mutable list.

</details>

<details>
<summary><b>Q21. What's the difference between <code>findFirst</code> and <code>findAny</code>? <code>anyMatch</code> vs <code>allMatch</code> vs <code>noneMatch</code>?</b></summary>

`findFirst` returns the first element in encounter order; `findAny` returns *any* element (it can be faster in parallel streams since it needn't pick the first). Both return `Optional`. The matchers return `boolean` and short-circuit: `anyMatch(p)` is true if **at least one** element matches (stops at the first match); `allMatch(p)` is true if **every** element matches (stops at the first failure); `noneMatch(p)` is true if **no** element matches. They're efficient for "does anything/everything/nothing satisfy X?" checks.

</details>

<details>
<summary><b>Q22. Does a Stream modify the original collection?</b></summary>

No. A Stream only **reads** from the source; it produces new results without changing the original. That's central to the functional style — immutability of the source. If you want a changed collection, you materialize a **new** one with a terminal operation like `collect`/`toList`, leaving the original intact.

</details>

<details>
<summary><b>Q23. Walk through what happens (and in what order) for <code>list.stream().filter(f).map(g).collect(toList())</code>.</b></summary>

Building the pipeline does nothing — `filter` and `map` are lazy and just record the plan. When `collect` (the terminal op) runs, execution starts: each element is pulled from the source, passed through `filter`; if it survives, it's passed through `map`; the result is accumulated into the list. Elements flow one at a time through the whole chain (not stage-by-stage over the whole list), and `collect` materializes the final `List`. The source list is unchanged.

</details>

<details>
<summary><b>Q24. How do lambdas and the Stream API relate? Why learn lambdas first?</b></summary>

The Stream API methods are *defined to accept functional interfaces*: `filter` takes a `Predicate`, `map` takes a `Function`, `forEach` takes a `Consumer`, `reduce` takes a `BinaryOperator`. Lambdas are how you supply those behaviors concisely. So streams are essentially "a pipeline that applies your lambdas to a flow of data." Without understanding functional interfaces and lambdas (Part I), the Stream API (Part II) looks like magic; with them, every method signature makes sense.

</details>

<details>
<summary><b>Q25. Why use streams instead of plain loops — and when might a loop be better?</b></summary>

Streams give **declarative, readable** code: you state *what* (filter, map, group) instead of *how* (loop mechanics, temporary lists), and they compose cleanly. They also keep the source immutable. A plain loop can be clearer when you need indices, complex break/continue control flow, checked exceptions in the body, or when the collection is tiny and on a hot path (streams add a little overhead). The mature answer: use streams for data transformations, loops for index-based or side-effect-heavy imperative logic.

</details>

## 19. Quick Revision Cheat Sheet

**Functional programming idea:** build programs from functions; prefer pure functions (deterministic, no side effects), immutability, declarative style, and functions-as-values. Java *simulates* this — it stays object-oriented.

**Three pillars (Part I):** functional interfaces (the **type**), lambdas (the **logic**), method references (shorthand logic).

**Functional interface:** interface with **exactly one abstract method** (`@FunctionalInterface` enforces it). May have `default`/`static` methods. Gives a function a type + signature so you can store/pass/return it.

**Lambda anatomy:** `(params) -> body`. Types inferred. Single-expression body → no `return`; block body `{ }` → `return` required. Only usable where a functional interface is expected. Defining ≠ running — runs when the SAM is called.

**Method references (4):** static `Integer::parseInt`; specific-object instance `System.out::println`; arbitrary-object instance `String::toUpperCase`; constructor `ArrayList::new`. Only when the lambda just forwards to one method.

**Built-in interfaces:** `Predicate<T>` `test`→boolean; `Function<T,R>` `apply`; `Consumer<T>` `accept`→void; `Supplier<T>` `get`; `BiFunction<T,U,R>`; `UnaryOperator<T>`=`Function<T,T>`; `BinaryOperator<T>`=`BiFunction<T,T,T>` (used by `reduce`). Generics: `T`=input, `R`=return, `U`=second input.

**Autoboxing:** generics box primitives (`int→Integer`) → inefficient at scale. Use primitive specializations (`IntPredicate`, `IntFunction`, `IntUnaryOperator`, `ToIntFunction`…) and `IntStream`/`LongStream`/`DoubleStream`.

**Stream:** not a data structure, stores nothing, doesn't modify the source, consumed once. `collection.stream()` to create.

**Intermediate ops (lazy, return Stream):** `filter` (Predicate), `map` (Function), `sorted` (natural/Comparator), `distinct`, `flatMap` (transform + flatten), `limit(n)`, `skip(n)`, `peek` (debug only).

**Terminal ops (eager, trigger execution):** `forEach` (Consumer), `collect` (with Collectors), `count`, `findFirst`/`findAny` (→ Optional), `reduce` (BinaryOperator), `anyMatch`/`allMatch`/`noneMatch` (→ boolean), `toArray`.

**Lazy evaluation:** intermediates build a plan; nothing runs until a terminal op. No terminal op ⇒ nothing happens.

**map vs flatMap:** `map` = 1→1 (can give nested `Stream<List<X>>`); `flatMap` = 1→many flattened into one `Stream<X>`.

**Collectors:** `toList()`/`toSet()`; `joining(", ")`; `counting()`; `summingInt`/`averagingInt`; `toMap(k, v, merge)` (merge avoids "Duplicate key"); `groupingBy(classifier[, downstream])` = GROUP BY; `partitioningBy(predicate)` = true/false split. `Stream.toList()` (Java 16+) is unmodifiable.

**If you remember nothing else:** *A functional interface is the type, a lambda is the logic, and a Stream is a lazy conveyor belt that applies your lambdas to a flow of data without touching the original — finishing only when a terminal operation pulls the result.*

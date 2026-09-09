# Decorator Pattern ⭐⭐⭐⭐ (Difficulty: 4/5 — structurally simple, but easy to misuse; deep in real frameworks)

> **Category:** Structural Pattern (GoF)
> **Also known as:** Wrapper

The Decorator pattern lets you **attach new behavior to an object dynamically** by wrapping it in another object that shares the same interface. It's the pattern behind `java.io` streams, Spring's `HttpServletRequestWrapper`, and countless middleware chains.

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 0: The Naive Subclass Explosion (Anti-pattern)](#variant-0-the-naive-subclass-explosion-anti-pattern)
   - [Variant 1: Basic Decorator](#variant-1-basic-decorator)
   - [Variant 2: Abstract Decorator Base Class](#variant-2-abstract-decorator-base-class)
   - [Variant 3: Production-Grade — Interface + Composition + Ordering](#variant-3-production-grade--interface--composition--ordering)
   - [Variant 4: Functional Decorators (Java 8+)](#variant-4-functional-decorators-java-8)
5. [🎨 Real-World Example](#-real-world-example)
6. [✅ When to Use](#-when-to-use)
7. [❌ When NOT to Use](#-when-not-to-use)
8. [🎯 Pros and Cons](#-pros-and-cons)
9. [🔄 Comparison with Related/Similar Patterns](#-comparison-with-relatedsimilar-patterns)
10. [📊 Comparison Table](#-comparison-table-of-variants)
11. [💡 Common Pitfalls](#-common-pitfalls)
12. [🎓 Interview Tips](#-interview-tips)
13. [🔗 Related Patterns](#-related-patterns)
14. [📚 Library/Framework Implementation](#-libraryframework-implementation)
15. [📝 Interview Questions & Answers (FAANG Top 20)](#-interview-questions--answers-faang-top-20)
16. [⚡ Quick Revision](#-quick-revision)

---

## 📋 Intent

> **GoF Definition:** *"Attach additional responsibilities to an object dynamically. Decorators provide a flexible alternative to subclassing for extending functionality."*

The Decorator pattern exists to solve a single core tension: you want to add responsibilities to **individual objects, not to an entire class**, and you want to do it **at runtime, transparently, and reversibly**, without exploding your class hierarchy through inheritance.

The key insight is that a decorator **implements the same interface as the object it wraps** and **holds a reference to that object**. This lets a decorator stand in wherever the original object was expected (Liskov substitution holds), while adding behavior *before* and/or *after* delegating to the wrapped object. Because decorators share the component interface, they can be **stacked recursively** — a decorator can wrap another decorator, forming a chain of behavior.

---

## 🎯 Problem

Imagine you're building a system and you need to add "extras" or "responsibilities" to objects. The naive approach is **inheritance**, but inheritance is static (fixed at compile time), applies to all instances of a class, and combinatorially explodes when features are independent and combinable.

**The pain points that lead you to Decorator:**

- You need to add behavior to *some* objects but not others of the same class — inheritance forces the behavior on the whole class.
- Features are **independent and combinable** (order matters or doesn't), and modeling every combination via subclasses produces an exponential number of classes (`N` features → up to `2^N` subclasses).
- You want to add/remove responsibilities at **runtime**, not compile time.
- The base class is `final` or you otherwise cannot subclass it, but you can wrap it.

**Concrete example scenarios:**

1. **Coffee shop pricing (the classic).** A `Beverage` can have milk, soy, whip, caramel, extra shot — in any combination. Subclassing (`CoffeeWithMilkAndWhip`, `CoffeeWithSoyAndCaramelAndWhip`, …) explodes. Decorators let you wrap a base `Espresso` with `Milk`, then `Whip`, computing cost and description incrementally.

2. **Java I/O streams.** You have a raw `InputStream` from a socket or file. You want buffering, then decompression (GZIP), then decoding to characters, then line-reading. `new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f))))` — each wrapper adds one responsibility.

3. **HTTP middleware / servlet filters.** Add logging, authentication, rate-limiting, compression, and caching around a core request handler — each as a wrapper that can be enabled/disabled per route.

4. **UI components.** A text view that can be given scrollbars, borders, and shadows independently — a scrollbar decorator wraps a bordered decorator wraps the text view.

---

## ✅ Solution

The core idea, in plain language: **create a wrapper class that implements the same interface as the thing it wraps, holds a reference to a wrapped instance of that interface, delegates calls to it, and adds its own behavior around the delegation.** Because the wrapper *is* the interface, callers can't tell the difference, and wrappers can be nested arbitrarily.

**Key structural elements:**

- **Component (interface / abstract class):** defines the common contract that both the concrete object and all decorators implement. This is what makes wrappers transparent and stackable.
- **Concrete Component:** the "real" object being decorated — the base behavior (e.g., `Espresso`, `FileInputStream`).
- **Decorator (abstract base, optional but recommended):** implements `Component` and holds a `Component` reference (the wrapped object). It delegates every method to the wrapped object by default, so concrete decorators only override what they need to change.
- **Concrete Decorators:** subclass the Decorator, add state/behavior, override methods to add responsibilities *before/after* calling `super` (the delegate).

**The mechanism that makes it work:** *composition over inheritance* combined with *interface polymorphism*. The decorator has-a component (composition) rather than is-a specialized component (inheritance). Since both conform to the same type, the outermost decorator is returned to the client as the component type, and each layer's method call cascades inward through the chain — a recursive delegation. Behavior can be injected on the way in (before delegation) or on the way out (after delegation), which is why **order of wrapping matters**.

---

## 💻 Implementation

We'll model a **coffee shop beverage pricing system** — a FAANG-favorite because it makes the "subclass explosion vs. composition" trade-off concrete. Each variant improves on the previous one.

### Variant 0: The Naive Subclass Explosion (Anti-pattern)

**What's wrong with it:** Before showing Decorator, understand the disease it cures. If you model every combination of add-ons as a subclass, you get combinatorial explosion. With 4 independent condiments you already need up to 2⁴ = 16 classes, and adding one new condiment *doubles* the hierarchy. Pricing logic is duplicated, and you cannot add condiments at runtime.

<details>
<summary>💻 Click to expand code — the anti-pattern</summary>

```java
// DON'T DO THIS — inheritance-based combination explosion
abstract class Beverage {
    abstract String description();
    abstract double cost();
}

class Espresso extends Beverage {
    String description() { return "Espresso"; }
    double cost() { return 1.99; }
}

// One class per combination... this does not scale.
class EspressoWithMilk extends Beverage {
    String description() { return "Espresso, Milk"; }
    double cost() { return 1.99 + 0.30; }
}

class EspressoWithMilkAndWhip extends Beverage {
    String description() { return "Espresso, Milk, Whip"; }
    double cost() { return 1.99 + 0.30 + 0.20; }
}

class EspressoWithSoyAndWhipAndCaramel extends Beverage { /* ... */ }
// ...and 12+ more. Add "Mocha"? Now double everything again.
```
</details>

**Pros:** Simple to read for a *single* combination; behavior is static and predictable.
**Cons:** Exponential class count (`2^N`), massive code duplication, no runtime composition, violates Open/Closed Principle (every new condiment forces edits across the hierarchy).
**Mechanism (why it fails):** inheritance binds behavior at **compile time** and applies it per-class, not per-object — the exact rigidity Decorator removes.

---

### Variant 1: Basic Decorator

**What problem it solves:** Replaces inheritance-of-combinations with composition. A decorator implements `Beverage` and wraps a `Beverage`, adding its cost/description on top. Combinations become *runtime wrapping* instead of *compile-time classes*.

<details>
<summary>💻 Click to expand code</summary>

```java
// Component
interface Beverage {
    String description();
    double cost();
}

// Concrete Component
class Espresso implements Beverage {
    public String description() { return "Espresso"; }
    public double cost() { return 1.99; }
}

class HouseBlend implements Beverage {
    public String description() { return "House Blend Coffee"; }
    public double cost() { return 0.89; }
}

// A concrete decorator — note it implements Beverage AND holds a Beverage
class MilkDecorator implements Beverage {
    private final Beverage beverage;               // composition: HAS-A
    MilkDecorator(Beverage beverage) { this.beverage = beverage; }

    public String description() { return beverage.description() + ", Milk"; }
    public double cost() { return beverage.cost() + 0.30; }   // add after delegating
}

class WhipDecorator implements Beverage {
    private final Beverage beverage;
    WhipDecorator(Beverage beverage) { this.beverage = beverage; }

    public String description() { return beverage.description() + ", Whip"; }
    public double cost() { return beverage.cost() + 0.20; }
}

// Usage
class Demo {
    public static void main(String[] args) {
        Beverage drink = new WhipDecorator(new MilkDecorator(new Espresso()));
        System.out.println(drink.description()); // Espresso, Milk, Whip
        System.out.println(drink.cost());        // 2.49
    }
}
```
</details>

**Pros:** Kills the class explosion; combinations are composed at runtime; each condiment lives in exactly one place.
**Cons:** Every decorator repeats the boilerplate `private final Beverage beverage;` field, constructor, and default delegation. If `Beverage` had 10 methods, each decorator would need to re-implement all 10 even if it only changes one.
**Mechanism:** *polymorphic delegation* — `MilkDecorator` is-a `Beverage` (satisfies the client type) and has-a `Beverage` (the delegate). `cost()` calls the inner `cost()` first, then adds — behavior injected **after** delegation.

---

### Variant 2: Abstract Decorator Base Class

**What problem it solves:** Removes the delegation boilerplate. An abstract `BeverageDecorator` implements `Beverage`, stores the wrapped component, and provides **default pass-through implementations** of every method. Concrete decorators extend it and override *only* what they change. This is the canonical GoF structure.

<details>
<summary>💻 Click to expand code</summary>

```java
interface Beverage {
    String description();
    double cost();
}

class Espresso implements Beverage {
    public String description() { return "Espresso"; }
    public double cost() { return 1.99; }
}

// Abstract Decorator — the reusable base. Delegates everything by default.
abstract class BeverageDecorator implements Beverage {
    protected final Beverage beverage;
    protected BeverageDecorator(Beverage beverage) {
        this.beverage = beverage;
    }
    // Default pass-through; subclasses override selectively
    public String description() { return beverage.description(); }
    public double cost() { return beverage.cost(); }
}

class MilkDecorator extends BeverageDecorator {
    MilkDecorator(Beverage b) { super(b); }
    @Override public String description() { return super.description() + ", Milk"; }
    @Override public double cost() { return super.cost() + 0.30; }
}

class MochaDecorator extends BeverageDecorator {
    MochaDecorator(Beverage b) { super(b); }
    @Override public String description() { return super.description() + ", Mocha"; }
    @Override public double cost() { return super.cost() + 0.45; }
}

class Demo {
    public static void main(String[] args) {
        Beverage b = new MochaDecorator(new MilkDecorator(new Espresso()));
        System.out.printf("%s = $%.2f%n", b.description(), b.cost());
        // Espresso, Milk, Mocha = $2.74
    }
}
```
</details>

**Pros:** No boilerplate per decorator; adding a new condiment = one tiny class; `super.cost()` cleanly expresses "delegate then add"; scales to wide interfaces gracefully.
**Cons:** Introduces an extra abstract class in the hierarchy; the base class is technically a "useless" abstract class (it adds nothing on its own) — some interviewers will ask why it exists (answer: to centralize delegation).
**Mechanism:** *default delegation via an intermediate abstract class*. `super.cost()` resolves to `BeverageDecorator.cost()`, which forwards to `beverage.cost()` — the wrapped object, which may itself be another decorator. This is the recursion that lets chains form.

---

### Variant 3: Production-Grade — Interface + Composition + Ordering

**Why it's recommended:** Real systems need more than description/cost. Production decorators (a) make ordering explicit and safe, (b) handle the case where a method's semantics depend on position in the chain, and (c) are often built with a fluent builder so call sites read top-to-bottom. Below is a request-processing pipeline — a pattern you'll see in every FAANG service layer.

<details>
<summary>💻 Click to expand code — production data-processing pipeline</summary>

```java
// Component
interface DataSource {
    void writeData(String data);
    String readData();
}

// Concrete Component — the real resource
class FileDataSource implements DataSource {
    private final String name;
    private String storage = "";
    FileDataSource(String name) { this.name = name; }
    public void writeData(String data) { this.storage = data; /* write to disk */ }
    public String readData() { return storage; }
}

// Base Decorator
abstract class DataSourceDecorator implements DataSource {
    protected final DataSource wrappee;
    protected DataSourceDecorator(DataSource source) { this.wrappee = source; }
    public void writeData(String data) { wrappee.writeData(data); }
    public String readData() { return wrappee.readData(); }
}

// Concrete Decorator: encryption. Encrypt on write, decrypt on read.
class EncryptionDecorator extends DataSourceDecorator {
    EncryptionDecorator(DataSource source) { super(source); }
    @Override public void writeData(String data) { super.writeData(encode(data)); }
    @Override public String readData() { return decode(super.readData()); }
    private String encode(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
    private String decode(String s) {
        return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
    }
}

// Concrete Decorator: compression (simulated). Order relative to encryption matters!
class CompressionDecorator extends DataSourceDecorator {
    CompressionDecorator(DataSource source) { super(source); }
    @Override public void writeData(String data) { super.writeData("[zip]" + data); }
    @Override public String readData() {
        String r = super.readData();
        return r.startsWith("[zip]") ? r.substring(5) : r;
    }
}

class Demo {
    public static void main(String[] args) {
        // Compress FIRST (inner), then encrypt (outer): write = encrypt(compress(data))
        DataSource src = new EncryptionDecorator(
                             new CompressionDecorator(
                                 new FileDataSource("out.dat")));
        src.writeData("Top secret payload");
        // On disk it's compressed-then-encrypted; readData reverses in exact opposite order.
        System.out.println(src.readData()); // Top secret payload
    }
}
```

*Required imports:* `java.util.Base64`, `java.nio.charset.StandardCharsets`.
</details>

**Pros:** Realistic; demonstrates the crucial **ordering property** (encrypt-then-compress compresses ciphertext = poor ratio; compress-then-encrypt is correct); read reverses write order automatically because the chain unwinds symmetrically.
**Cons:** Requires the author to reason carefully about ordering; a wrong stack order silently produces wrong (but non-crashing) results — a subtle, hard-to-debug bug.
**Mechanism:** *symmetric chain unwinding*. `writeData` flows outer→inner (encrypt wraps compress wraps file); `readData` flows the same physical direction but each layer *reverses* its transform, so the net effect is the exact inverse. This only works if the transforms are inverses and applied in mirror order.

---

### Variant 4: Functional Decorators (Java 8+)

**Why it's recommended (for the right shape):** When your component is a single-method (functional) interface, decorators collapse into higher-order functions — no classes needed. This is how modern middleware, `Function` composition, and reactive operators are built. It's the most idiomatic modern Java when the interface is functional.

<details>
<summary>💻 Click to expand code — functional/HOF decorators</summary>

```java
import java.util.function.Function;

class FunctionalDecoratorDemo {
    // Scenario: process a raw chat message before displaying it.
    // The "component" is just a function: String -> String.
    public static void main(String[] args) {
        // Base behavior: return the message as-is.
        Function<String, String> pipeline = Function.identity();

        // Each decorator is a plain function; andThen wraps the previous step.
        pipeline = pipeline
                .andThen(String::trim)                 // 1. remove surrounding spaces
                .andThen(FunctionalDecoratorDemo::censor)   // 2. hide banned words
                .andThen(msg -> "[chat] " + msg);      // 3. add a prefix

        System.out.println(pipeline.apply("   hello damn world   "));
        // [chat] hello **** world

        // Because decorators are just values, you can add another one anytime:
        Function<String, String> shouting = pipeline.andThen(String::toUpperCase);
        System.out.println(shouting.apply("  hi  "));
        // [CHAT] HI
    }

    static String censor(String msg) {
        return msg.replace("damn", "****");
    }
}
```
</details>

**Pros:** Zero boilerplate; composition via `andThen`/`compose` is built into `Function`; decorators are first-class values you can store and pass; extremely testable.
**Cons:** Only works cleanly for single-method interfaces; loses the self-documenting class names (a stack trace shows lambdas, not `EncryptionDecorator`); harder to attach per-decorator state.
**Mechanism:** *function composition* — `f.andThen(g)` returns `x -> g(f(x))`, the functional equivalent of wrapping. The JVM captures the inner function in a closure, exactly analogous to the `wrappee` field.

---

## 🎨 Real-World Example

The definitive real-world Decorator is **`java.io`**. `InputStream`/`Reader` (the components) are wrapped by `BufferedInputStream`, `GZIPInputStream`, `InputStreamReader`, `BufferedReader`, etc. Below is a **complete, production-realistic example**: a metrics-and-cache decorated `Repository` layer — the exact shape used in FAANG service tiers, where a raw data access object is wrapped with caching, metrics, and retry, each toggleable via configuration.

<details>
<summary>💻 Click to expand full real-world example (decorated repository with demo + concurrency test)</summary>

```java
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

// ---------- Component ----------
interface UserRepository {
    String findNameById(long id);   // simulates a slow backing store
}

// ---------- Concrete Component: the real, expensive data source ----------
class DatabaseUserRepository implements UserRepository {
    private final Map<Long, String> table = Map.of(1L, "Ada", 2L, "Linus", 3L, "Grace");

    @Override
    public String findNameById(long id) {
        sleep(50); // simulate network + DB latency
        return table.get(id);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// ---------- Base Decorator ----------
abstract class UserRepositoryDecorator implements UserRepository {
    protected final UserRepository delegate;

    protected UserRepositoryDecorator(UserRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public String findNameById(long id) {
        return delegate.findNameById(id);
    }
}

// ---------- Decorator 1: thread-safe caching ----------
class CachingUserRepository extends UserRepositoryDecorator {
    private final ConcurrentMap<Long, String> cache = new ConcurrentHashMap<>();

    CachingUserRepository(UserRepository delegate) {
        super(delegate);
    }

    @Override
    public String findNameById(long id) {
        // computeIfAbsent gives us atomic "check-then-load" per key
        return cache.computeIfAbsent(id, super::findNameById);
    }
}

// ---------- Decorator 2: metrics (count + latency) ----------
class MetricsUserRepository extends UserRepositoryDecorator {
    private final AtomicLong calls = new AtomicLong();
    private final AtomicLong totalNanos = new AtomicLong();

    MetricsUserRepository(UserRepository delegate) {
        super(delegate);
    }

    @Override
    public String findNameById(long id) {
        long start = System.nanoTime();
        try {
            return super.findNameById(id);           // delegate (may hit cache or DB)
        } finally {
            calls.incrementAndGet();
            totalNanos.addAndGet(System.nanoTime() - start);
        }
    }

    void printStats() {
        long c = calls.get();
        System.out.printf("calls=%d, avgMicros=%.1f%n",
                c, c == 0 ? 0 : (totalNanos.get() / 1000.0) / c);
    }
}

// ---------- Decorator 3: retry with fallback ----------
class RetryingUserRepository extends UserRepositoryDecorator {
    private final int maxAttempts;

    RetryingUserRepository(UserRepository delegate, int maxAttempts) {
        super(delegate);
        this.maxAttempts = maxAttempts;
    }

    @Override
    public String findNameById(long id) {
        RuntimeException last = null;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                return super.findNameById(id);
            } catch (RuntimeException e) {
                last = e;
            }
        }
        throw last;
    }
}

// ---------- Demo / usage: multiple call sites + concurrency test ----------
public class RepositoryDecoratorDemo {
    public static void main(String[] args) throws Exception {
        // Order: metrics(retry(cache(db))) -> metrics sees ALL calls incl. cache hits;
        // cache sits closest to the slow DB so it shields it.
        MetricsUserRepository metrics =
            new MetricsUserRepository(
                new RetryingUserRepository(
                    new CachingUserRepository(
                        new DatabaseUserRepository()), 3));
        UserRepository repo = metrics;

        // Call site 1: cold + warm
        System.out.println(repo.findNameById(1)); // Ada  (DB hit, ~50ms)
        System.out.println(repo.findNameById(1)); // Ada  (cache hit, fast)

        // Call site 2: concurrency test — 100 threads hammering the same 3 ids.
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch latch = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            final long id = (i % 3) + 1;
            pool.submit(() -> {
                try {
                    repo.findNameById(id);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();

        // Cache (ConcurrentHashMap.computeIfAbsent) ensures the DB is hit at most
        // ~3 times despite 102 total logical reads -> proves thread safety.
        metrics.printStats(); // e.g. calls=102, avgMicros small after warmup
    }
}
```
</details>

**Why this is a strong FAANG answer:** it shows (1) the canonical base-decorator structure, (2) *ordering reasoning* — putting the cache closest to the DB so it shields the expensive resource while metrics on the outside counts every logical call, (3) genuine **thread safety** via `ConcurrentHashMap.computeIfAbsent` and atomic counters, and (4) that each concern (caching, metrics, retry) is independently testable and toggleable — the whole point of Decorator.

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- You need to add responsibilities to **individual objects dynamically and transparently**, without affecting other objects of the same class.
- Responsibilities are **combinable and optional**, and modeling every combination via subclassing would explode the class count.
- You want to be able to **add and withdraw** responsibilities at runtime (e.g., turn on compression only for large payloads).
- Extension by subclassing is **impractical or impossible** — the class is `final`, or the number of independent extensions is large.
- You're building **layered/middleware pipelines** (I/O, HTTP filters, interceptors) where each layer adds one orthogonal concern.
- You want to keep each concern in its **own single-responsibility class** that can be unit-tested in isolation.

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- **Behavior depends on the wrapper's position in ways clients must know about.** Decorators are meant to be transparent; if callers must reason about the exact stack, the abstraction leaks.
- **You need to add responsibilities to a whole class, permanently.** Plain inheritance or just editing the class is simpler than a wrapper.
- **The component interface is large and volatile.** Every method must be delegated by every decorator; a churning interface makes decorators a maintenance burden. (Consider that Java's `FilterInputStream` has this exact wart.)
- **Identity matters.** `decorated == original` is false, and `instanceof ConcreteComponent` fails through a wrapper. If code relies on object identity or concrete type checks, decorators break it.
- **Only one combination is ever needed.** If there's exactly one variant, a decorator is over-engineering — just write the class.
- **Deep chains hurt performance/debuggability.** Very long decorator stacks add call-stack depth and make stack traces noisy; if hot-path latency is critical, measure.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- More flexible than static inheritance — behavior is composed at **runtime**.
- Avoids feature-combination class explosion (`2^N` → `N` decorators).
- Honors the **Single Responsibility Principle** (one concern per decorator) and **Open/Closed Principle** (add behavior without modifying existing code).
- Responsibilities can be added, removed, and **reordered** by rearranging wrappers.
- A client sees only the component interface — decorators are transparent.

**Cons**

- Lots of **small classes** that look similar; the design can be hard for newcomers to follow.
- **Order-dependence** can introduce subtle bugs (compress-then-encrypt vs. encrypt-then-compress).
- **Removing a specific decorator** from the middle of a chain is awkward — you must rebuild the chain.
- Breaks **object identity** and `instanceof`/`getClass()` checks against the concrete component.
- Delegation **boilerplate** for wide interfaces; deep chains complicate debugging and add stack depth.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

All four "wrapper-ish" patterns share structure (an object holding a reference to another) but differ in **intent** — the classic interview trap.

| Pattern | Intent | Interface vs. wrapped | Stackable? | Key tell |
|---|---|---|---|---|
| **Decorator** | Add/augment responsibilities *transparently* | **Same** interface as wrappee | **Yes**, recursively | Adds behavior, keeps the type |
| **Adapter** | Convert one interface into another | **Different** interface | No | Makes incompatible things work together |
| **Proxy** | Control *access* to an object (lazy, remote, security) | **Same** interface | Rarely stacked | Controls, doesn't add features |
| **Composite** | Treat individual + groups uniformly (tree) | Same interface, but **1-to-many** | Tree, not chain | Aggregates children |
| **Strategy** | Swap an *algorithm* via composition | Different (a collaborator) | No | Changes the *inside*, not the *skin* |

Concise contrasts: **Decorator vs. Proxy** — same interface, but Decorator's job is to *enhance* while Proxy's is to *control access* (both wrap; intent differs). **Decorator vs. Composite** — both use recursive composition over a shared interface, but Composite builds *trees of many children* while a decorator wraps *exactly one* component and adds behavior. **Decorator vs. Strategy** — Strategy changes an object's guts by plugging in an algorithm; Decorator changes its *skin* by wrapping it. **Decorator vs. Adapter** — Adapter deliberately *changes* the interface to bridge incompatibility; Decorator deliberately *keeps* it to stay transparent.

</details>

## 📊 Comparison Table (of Variants)

<details>
<summary>📖 Click to expand</summary>

| Axis | V0: Subclass Explosion | V1: Basic Decorator | V2: Abstract Base Decorator | V3: Production (ordering) | V4: Functional |
|---|---|---|---|---|---|
| Runtime composition | ❌ No | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| Boilerplate per feature | High (whole class) | Medium (field+ctor+all methods) | **Low** (override only) | Low | **None** |
| Class count for N features | up to 2^N | N | N | N | 0 (lambdas) |
| Wide interface friendliness | ❌ | ❌ (re-impl all) | ✅ (default pass-through) | ✅ | ❌ (single-method only) |
| Self-documenting names | ✅ | ✅ | ✅ | ✅ | ❌ (lambdas in traces) |
| Per-decorator state | ✅ | ✅ | ✅ | ✅ | ⚠️ Awkward |
| Idiomatic use case | Never | Learning | **GoF standard** | Services/pipelines | Functional interfaces |
| Ordering-aware | N/A | Manual | Manual | **Explicit** | via andThen/compose |

</details>

---

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

### Pitfall 1: Wrong wrapping order produces silently incorrect results

**What goes wrong:** Decorators are applied in the order you nest them. For non-commutative operations (compress + encrypt, buffer + gzip, retry + cache) the wrong order compiles, runs, and returns *wrong-but-plausible* output — the nastiest kind of bug.

<details>
<summary>💻 Click to expand — the failure</summary>

```java
// BUG: encrypt first, then compress. Compressing ciphertext barely shrinks it
// (encrypted data is high-entropy) — you get near-zero compression + wasted CPU.
DataSource bad = new CompressionDecorator(
                     new EncryptionDecorator(
                         new FileDataSource("out.dat")));
```
</details>

<details>
<summary>💻 Click to expand — the fix</summary>

```java
// FIX: compress first (inner), then encrypt (outer). Compress raw data (compressible),
// then encrypt the compressed bytes. Reads unwind in exact reverse.
DataSource good = new EncryptionDecorator(
                      new CompressionDecorator(
                          new FileDataSource("out.dat")));
// Rule of thumb: decorators that transform data must be inverses applied in mirror order.
```
</details>

### Pitfall 2: Relying on object identity or `instanceof` through the chain

**What goes wrong:** A decorator is a *different object* of a *different class* than the component. Identity checks, `equals`, and `instanceof ConcreteComponent` all fail once wrapped.

<details>
<summary>💻 Click to expand — the failure</summary>

```java
Beverage core = new Espresso();
Beverage wrapped = new MilkDecorator(core);
System.out.println(wrapped == core);            // false
System.out.println(wrapped instanceof Espresso);// false — it's a MilkDecorator!
// Any code doing `if (b instanceof Espresso)` silently misses decorated instances.
```
</details>

<details>
<summary>💻 Click to expand — the fix</summary>

```java
// FIX 1: don't branch on concrete type — that's what the interface is for.
// FIX 2: if you MUST unwrap, expose an explicit accessor (like JDBC's Wrapper API):
interface Unwrappable { <T> T unwrap(Class<T> type); }
// java.sql.Wrapper.unwrap(...) exists precisely because decorators/proxies hide the target.
```
</details>

### Pitfall 3: Wide interface → delegation boilerplate and "forgotten method" bugs

**What goes wrong:** If the component interface has many methods and you *don't* use an abstract base decorator, each concrete decorator must re-implement every method. Miss one, and calls silently bypass your logic (or worse, NPE). This is the real-world wart in `java.io.FilterInputStream`.

<details>
<summary>💻 Click to expand — the failure</summary>

```java
// Directly implementing a wide interface — easy to forget a method or mis-delegate.
class LeakyDecorator implements BigInterface {
    private final BigInterface inner;
    LeakyDecorator(BigInterface inner) { this.inner = inner; }
    public void a() { /* my logic */ inner.a(); }
    public void b() { inner.b(); }
    // OOPS: forgot to override c() and d() — behavior silently missing / compile error
}
```
</details>

<details>
<summary>💻 Click to expand — the fix</summary>

```java
// FIX: always route through an abstract base decorator that provides default pass-through
// delegation for EVERY method. Concrete decorators then override only what they change.
abstract class BaseDecorator implements BigInterface {
    protected final BigInterface inner;
    protected BaseDecorator(BigInterface inner) { this.inner = inner; }
    public void a() { inner.a(); }
    public void b() { inner.b(); }
    public void c() { inner.c(); }
    public void d() { inner.d(); }   // one place, guaranteed complete
}
```
</details>

### Pitfall 4: Stateful decorators + shared/re-wrapped components

**What goes wrong:** If a decorator holds mutable state (a buffer, a counter) and the same underlying component is wrapped by multiple chains, or a chain is shared across threads without synchronization, you get corrupted state or lost buffered data (e.g., forgetting to `flush()`/`close()` a `BufferedOutputStream` loses the tail of your data).

<details>
<summary>💻 Click to expand — the fix</summary>

```java
// FIX: honor lifecycle. try-with-resources closes the OUTERMOST decorator,
// which cascades close()/flush() inward through the whole chain.
try (var out = new BufferedOutputStream(new FileOutputStream("f.dat"))) {
    out.write(payload);
} // auto-flush + close propagates down the chain — no lost bytes.
```
</details>

</details>

## 🎓 Interview Tips

**What interviewers commonly ask:**

- "You have a coffee shop / pizza / notification system with many optional add-ons — design it." (The tell that they want Decorator, not inheritance.)
- "Name a real JDK use of Decorator." (Answer instantly: `java.io` streams/readers, `Collections.unmodifiableList/synchronizedList`.)
- "How is Decorator different from Proxy / Adapter / Composite?" (Intent-based distinction — see the comparison table.)
- "Draw the UML" — Component, ConcreteComponent, Decorator (abstract, has-a Component), ConcreteDecorators.
- "Why not just use inheritance?" (Combination explosion + static binding + per-class vs. per-object.)

**What you should proactively mention even if not asked:**

- **Composition over inheritance** — state this as the guiding principle; it signals maturity.
- The **abstract base decorator** exists purely to centralize delegation and avoid boilerplate on wide interfaces.
- **Order matters** for non-commutative decorators, and reads unwind in reverse — give the compress/encrypt example.
- Decorator **breaks identity and `instanceof`**; mention JDBC's `unwrap()` and `java.sql.Wrapper` as the industry escape hatch.
- Decorators should ideally be **transparent** (Liskov-substitutable) — a decorator that changes the contract is a code smell.
- For single-method interfaces, mention the **functional** form (`Function.andThen`) as the modern idiom.
- Contrast with **AOP/dynamic proxies** (`java.lang.reflect.Proxy`, CGLIB, Spring AOP) as the "decorator generated at runtime" generalization.

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Adapter** — also wraps, but *changes* the interface instead of preserving it.
- **Proxy** — same interface; controls access (lazy loading, remote, security) rather than adding features. A "smart reference."
- **Composite** — recursive composition over a shared interface, but forms *trees of children*; decorators are often described as a degenerate composite with a single child.
- **Strategy** — changes an object's behavior by swapping an internal algorithm (changes the guts, not the skin).
- **Chain of Responsibility** — a chain of handlers like a decorator stack, but each handler may *stop* the chain; decorators always delegate.
- **Factory / Builder** — often used to *assemble* complex decorator stacks so clients don't hand-nest constructors.
- **Dynamic Proxy / AOP** — reflection-based generalization that synthesizes decorator-like wrappers at runtime (Spring AOP, `java.lang.reflect.Proxy`).

</details>

## 📚 Library/Framework Implementation

**1. `java.io` streams and readers (the archetype).** Every `FilterInputStream`/`FilterReader` subclass is a decorator. `BufferedInputStream` adds buffering, `GZIPInputStream` adds decompression, `InputStreamReader` bridges bytes→chars, `BufferedReader` adds line reading — all sharing the `InputStream`/`Reader` type and stackable.

<details>
<summary>💻 Click to expand — java.io decorator stack</summary>

```java
// Each constructor wraps the previous stream — a live decorator chain.
try (BufferedReader in = new BufferedReader(              // + line reading, buffering
                             new InputStreamReader(       // bytes -> chars (also an Adapter!)
                                 new GZIPInputStream(     // + gunzip
                                     new FileInputStream("data.gz"))))) { // raw bytes
    String line;
    while ((line = in.readLine()) != null) System.out.println(line);
}
// FilterInputStream is java.io's abstract base decorator.
```
</details>

**2. `java.util.Collections` wrappers.** `unmodifiableList`, `synchronizedMap`, and `checkedCollection` return decorators that share the collection interface and add a single concern (immutability, synchronization, runtime type checking) around the wrapped collection.

<details>
<summary>💻 Click to expand — Collections decorators</summary>

```java
List<String> base = new ArrayList<>(List.of("a", "b"));
List<String> ro   = Collections.unmodifiableList(base);   // decorator: blocks mutators
List<String> sync = Collections.synchronizedList(base);    // decorator: adds locking
ro.add("c"); // throws UnsupportedOperationException — decorator intercepts the mutator.
```
</details>

**3. Spring Framework.** `HttpServletRequestWrapper`/`HttpServletResponseWrapper` (Servlet spec) are decorators used by Spring filters to add behavior (e.g., caching the request body via `ContentCachingRequestWrapper`, wrapping responses for compression). Spring's `TransactionAwareCacheDecorator` and `BeanDefinitionDecorator` are named decorators; Spring AOP synthesizes decorator-like proxies at runtime.

<details>
<summary>💻 Click to expand — Spring request wrapper</summary>

```java
// A servlet filter decorating the request so the body can be read multiple times.
public class CachingFilter implements Filter {
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        ContentCachingRequestWrapper wrapped =                  // <- Decorator
            new ContentCachingRequestWrapper((HttpServletRequest) req);
        chain.doFilter(wrapped, res);      // downstream sees the decorated request
        byte[] body = wrapped.getContentAsByteArray(); // now re-readable for logging
    }
}
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1: [Conceptual] What is the Decorator pattern and what problem does it solve?</strong></summary>

The Decorator is a **structural** GoF pattern that attaches additional responsibilities to an object **dynamically**, providing a flexible alternative to subclassing for extending functionality. The problem it solves is **combinatorial class explosion** and the rigidity of static inheritance: when you have several independent, optional, combinable features, modeling every combination as a subclass produces up to `2^N` classes and binds behavior at compile time.

A decorator implements the **same interface** as the object it wraps and **holds a reference** to a wrapped instance of that interface. It delegates to the wrapped object and adds behavior before and/or after the delegation. Because it shares the component type, it's transparent to clients and can be stacked recursively (a decorator wrapping a decorator).

The guiding principle is **composition over inheritance** — the decorator *has-a* component rather than *is-a* specialized subclass.
</details>

<details>
<summary><strong>Q2: [Conceptual] Name the participants in the Decorator pattern and their roles.</strong></summary>

There are four participants:

1. **Component** — the shared interface (or abstract class) implemented by both real objects and decorators. It's what makes decorators transparent and stackable.
2. **Concrete Component** — the real object being decorated; provides the base behavior (e.g., `FileInputStream`, `Espresso`).
3. **Decorator** — an abstract class that implements `Component` and holds a `Component` reference. It provides **default pass-through delegation** for all methods so concrete decorators override only what they change.
4. **Concrete Decorator** — extends the abstract Decorator and adds state/behavior, overriding methods to inject logic around `super`'s delegation.

The abstract Decorator is optional in theory but strongly recommended in practice — it eliminates delegation boilerplate on wide interfaces.
</details>

<details>
<summary><strong>Q3: [Conceptual] Which OO principles does Decorator embody?</strong></summary>

Three primarily:

- **Open/Closed Principle** — you extend behavior by adding new decorator classes without modifying existing components or decorators.
- **Single Responsibility Principle** — each decorator encapsulates exactly one concern (buffering, encryption, logging), so responsibilities are separated into small, testable units.
- **Composition over inheritance** — behavior is assembled at runtime by wrapping, not fixed at compile time by subclassing.

It also supports the **Liskov Substitution Principle**: because a decorator honors the same contract, it can substitute for the component anywhere. A decorator that *violates* the contract (changes semantics unexpectedly) is a code smell.
</details>

<details>
<summary><strong>Q4: [Conceptual] Give three real-world JDK examples of the Decorator pattern.</strong></summary>

1. **`java.io`** — `BufferedInputStream`, `GZIPInputStream`, `InputStreamReader`, `BufferedReader`, `DataInputStream` all decorate `InputStream`/`Reader`. `FilterInputStream` is the abstract base decorator.
2. **`java.util.Collections`** — `unmodifiableList`, `synchronizedMap`, `checkedCollection` return decorators that share the collection interface and add immutability, synchronization, or type checking.
3. **Servlet / Spring** — `HttpServletRequestWrapper` and `HttpServletResponseWrapper` (e.g., `ContentCachingRequestWrapper`) decorate requests/responses in filters.

Bonus: `java.io` is the *canonical teaching example* and also a *cautionary tale* — its wide interface means many `FilterInputStream` subclasses must delegate lots of methods, and forgetting one is a classic bug.
</details>

<details>
<summary><strong>Q5: [Conceptual] Why is Decorator preferred over inheritance for adding responsibilities?</strong></summary>

Inheritance is **static** and **class-level**: behavior is fixed at compile time and applies to *all* instances of a subclass. If you have `N` independent, combinable features, inheritance forces you to create a subclass for each combination — up to `2^N` classes — and you cannot mix and match at runtime.

Decorator is **dynamic** and **object-level**: you compose exactly the behaviors you want for *this particular object* at runtime, using `N` decorator classes instead of `2^N`. You can add, remove, and reorder responsibilities without touching existing code. The trade-off is more (but smaller) classes and the loss of object identity through the chain.
</details>

<details>
<summary><strong>Q6: [Implementation] Code a basic Decorator for a text-notification system (SMS + Slack + email).</strong></summary>

The classic "notifier" problem: a base notifier that can be augmented with additional channels.

<details>
<summary>💻 Click to expand solution</summary>

```java
interface Notifier { void send(String msg); }

class BaseNotifier implements Notifier {
    public void send(String msg) { System.out.println("EMAIL: " + msg); }
}

abstract class NotifierDecorator implements Notifier {
    protected final Notifier wrappee;
    protected NotifierDecorator(Notifier n) { this.wrappee = n; }
    public void send(String msg) { wrappee.send(msg); }
}

class SmsDecorator extends NotifierDecorator {
    SmsDecorator(Notifier n) { super(n); }

    @Override public void send(String msg) {
        super.send(msg);
        System.out.println("SMS: " + msg);
    }
}

class SlackDecorator extends NotifierDecorator {
    SlackDecorator(Notifier n) { super(n); }

    @Override public void send(String msg) {
        super.send(msg);
        System.out.println("SLACK: " + msg);
    }
}

// Usage: email + sms + slack, chosen at runtime
Notifier n = new SlackDecorator(new SmsDecorator(new BaseNotifier()));
n.send("Deploy finished"); // prints EMAIL, then SMS, then SLACK
```
</details>

The key points to verbalize: `NotifierDecorator` provides default delegation; each concrete decorator calls `super.send()` then adds its channel; combinations are runtime composition.
</details>

<details>
<summary><strong>Q7: [Implementation] How do you avoid re-implementing every method in each decorator?</strong></summary>

Introduce an **abstract base decorator** that implements the component interface, stores the wrapped reference, and provides **default pass-through implementations** for *every* method that simply forward to the wrappee. Concrete decorators extend it and override only the methods whose behavior they change; all other calls fall through to the base's delegation.

<details>
<summary>💻 Click to expand</summary>

```java
abstract class BaseDecorator implements Service {
    protected final Service delegate;
    protected BaseDecorator(Service d) { this.delegate = d; }
    public int op1() { return delegate.op1(); }   // default pass-through
    public int op2() { return delegate.op2(); }
    public int op3() { return delegate.op3(); }
}
class LoggingDecorator extends BaseDecorator {
    LoggingDecorator(Service d) { super(d); }
    @Override public int op1() { log("op1"); return super.op1(); } // override just one
}
```
</details>

Without this, a decorator directly implementing a wide interface must re-declare all methods — and forgetting one either fails to compile (good) or, if extending a class, silently bypasses your logic (bad).
</details>

<details>
<summary><strong>Q8: [Implementation] Implement a functional/lambda-based decorator in modern Java.</strong></summary>

When the component is a functional (single-method) interface, decorators become higher-order functions.

<details>
<summary>💻 Click to expand</summary>

```java
import java.util.function.Function;

Function<String, String> core = s -> "handled:" + s;

// A decorator is a function that takes and returns a Function
Function<Function<String,String>, Function<String,String>> withTiming =
    f -> s -> {
        long t = System.nanoTime();
        try { return f.apply(s); }
        finally { System.out.println("took " + (System.nanoTime()-t) + "ns"); }
    };

Function<String,String> decorated = withTiming.apply(core);
System.out.println(decorated.apply("x"));

// Or simply compose: core.andThen(String::toUpperCase) wraps core's output.
```
</details>

`Function.andThen`/`compose` are built-in decorator combinators. This is how middleware and reactive operators are built. The trade-off: no per-decorator class name in stack traces and awkward state.
</details>

<details>
<summary><strong>Q9: [Implementation] How would you let clients build a decorator chain without hand-nesting constructors?</strong></summary>

Nested constructors (`new A(new B(new C(x))))`) read inside-out and are error-prone. Wrap chain assembly in a **Builder** or **factory** so clients declare intent top-to-bottom.

<details>
<summary>💻 Click to expand</summary>

```java
class PipelineBuilder {
    private DataSource src;
    PipelineBuilder(DataSource base) { this.src = base; }
    PipelineBuilder compress() { src = new CompressionDecorator(src); return this; }
    PipelineBuilder encrypt()  { src = new EncryptionDecorator(src);  return this; }
    DataSource build() { return src; }
}

// Reads in application order; builder controls nesting so ordering is centralized.
DataSource ds = new PipelineBuilder(new FileDataSource("f"))
                    .compress()   // inner
                    .encrypt()    // outer
                    .build();
```
</details>

This also gives you one place to enforce/validate ordering constraints (e.g., "encryption must be outermost").
</details>

<details>
<summary><strong>Q10: [Breaking] How can a decorator chain produce silently wrong results?</strong></summary>

The most common failure is **wrong ordering of non-commutative decorators**. Compress-then-encrypt vs. encrypt-then-compress both compile and run, but encrypting first yields high-entropy data that barely compresses — wasting CPU and space with no error. Similarly, wrapping a `BufferedOutputStream` *inside* rather than *outside* a compressing stream changes flush semantics.

Other silent-failure modes: (1) a decorator that **forgets to call `super`/delegate** swallows the inner behavior; (2) a decorator that adds behavior on the *wrong side* of delegation (before vs. after) — e.g., counting a cache-shielded call at the wrong layer; (3) **not honoring lifecycle** (`flush`/`close`) so buffered data is lost. None of these throw — they just produce incorrect output, which is why decorator ordering deserves explicit reasoning and tests.
</details>

<details>
<summary><strong>Q11: [Breaking] Why do `instanceof` and `==` fail against decorated objects, and how do you cope?</strong></summary>

A decorator is a **distinct object of a distinct class**. `wrapped == original` is `false` and `wrapped instanceof ConcreteComponent` is `false` because the outermost reference is the decorator's type, not the component's. Any code that branches on concrete type or relies on identity silently misbehaves through a wrapper.

Coping strategies: (1) **Don't branch on concrete type** — program to the interface. (2) If you truly must reach the underlying object, expose an explicit **unwrap** contract, exactly like JDBC's `java.sql.Wrapper` with `unwrap(Class<T>)`/`isWrapperFor(...)`, which exists precisely because drivers wrap connections/statements in decorator/proxy layers. (3) For equality, decide deliberately whether decorators should delegate `equals`/`hashCode` — usually they should *not* claim equality with the raw component.
</details>

<details>
<summary><strong>Q12: [Breaking] What goes wrong with decorators in a multithreaded context?</strong></summary>

Decorators are only as thread-safe as their state and their wrappee. A **stateful decorator** (buffer, counter, cache) shared across threads without synchronization can corrupt state or lose data. Example: `BufferedOutputStream` is not safe for concurrent writers; two threads writing interleave into the same buffer.

Fixes: make decorator state thread-safe (`ConcurrentHashMap`, `AtomicLong`, proper locking as in `Collections.synchronizedList`), or make decorators **stateless** and push state to a thread-safe wrappee, or confine each chain to a single thread. Note the caching example from the real-world section uses `ConcurrentHashMap.computeIfAbsent` specifically so 100 concurrent reads still hit the DB at most once per key. Also beware **visibility**: without proper happens-before (volatile/locks/concurrent collections), one thread's decorator state may not be visible to another.
</details>

<details>
<summary><strong>Q13: [Breaking] What's the danger of deep decorator chains?</strong></summary>

Very long chains add **call-stack depth** (each layer is a frame), which makes stack traces noisy and, in pathological recursion-heavy designs, risks `StackOverflowError`. They also add per-call **indirection overhead** — usually negligible, but measurable on hot paths (each `super.op()` is a virtual dispatch). Debugging is harder because a single logical call fans out through many small classes, and it's non-obvious which layer produced a given side effect.

Mitigations: keep chains shallow, name decorators clearly, add logging/naming at layer boundaries, and on latency-critical paths **measure** rather than assume. If you find yourself with a 10-deep chain, reconsider whether some concerns belong in one class or behind an AOP proxy.
</details>

<details>
<summary><strong>Q14: [Trade-off] Decorator vs. Proxy — same structure, so how do they differ?</strong></summary>

They're structurally near-identical (both wrap an object of the same interface), so the distinction is **intent**:

- **Decorator** *adds or augments* responsibilities. The wrapped object always exists and is fully functional; the decorator enhances it. Decorators are designed to be **stacked**.
- **Proxy** *controls access* to the object — lazy instantiation (virtual proxy), remote access (remote proxy), access control (protection proxy), or reference management (smart proxy). A proxy often *creates or manages* the target's lifecycle and is rarely stacked.

Rule of thumb: if the wrapper's purpose is "do more," it's a Decorator; if it's "decide whether/when/how you may touch the real thing," it's a Proxy.
</details>

<details>
<summary><strong>Q15: [Trade-off] Decorator vs. Strategy — when do you choose which?</strong></summary>

**Strategy** changes an object's behavior by plugging a different **algorithm** into it — it changes the object's *guts* (a `Comparator`, a compression algorithm) while the object keeps its identity. **Decorator** changes an object's behavior by **wrapping** it — it changes the *skin*, layering behavior around the original without the original knowing.

Choose Strategy when there's a single, well-defined variation point inside one object and you want to swap the implementation. Choose Decorator when you want to **stack multiple, independent, orthogonal responsibilities** around an object and possibly reorder them. Strategy is one-at-a-time internal substitution; Decorator is many-at-a-time external layering. They compose well: a decorator can itself hold a strategy.
</details>

<details>
<summary><strong>Q16: [Trade-off] Decorator vs. Composite — both are recursive over a shared interface. Distinguish them.</strong></summary>

Both use recursive composition over a common interface, and structurally a decorator looks like a Composite node with exactly one child. The difference is **intent and cardinality**:

- **Composite** models **part-whole hierarchies (trees)**: a node holds *many* children and its purpose is to let clients treat individual objects and compositions uniformly. Its operations typically *aggregate* over children.
- **Decorator** wraps **exactly one** component and its purpose is to *add responsibilities*, not to aggregate.

GoF explicitly note the relationship: "A decorator can be viewed as a degenerate composite with only one component. However, a decorator adds additional responsibilities — it isn't intended for object aggregation." They're often combined: decorators can add behavior to composite trees.
</details>

<details>
<summary><strong>Q17: [Trade-off] When would you deliberately NOT use Decorator?</strong></summary>

Avoid it when: (1) there's **only one combination** ever needed — a plain subclass or direct edit is simpler; (2) the component **interface is wide and volatile** — every decorator must delegate every method, and churn makes maintenance painful (`FilterInputStream` is the cautionary example); (3) **object identity or concrete-type checks** are central to the design — decorators break `==` and `instanceof`; (4) the added behavior belongs to the **whole class permanently** — just change the class; (5) **hot-path latency** is critical and chains would add measurable indirection/stack depth.

The meta-answer interviewers like: decorators trade a small number of large classes for a large number of small classes; if that trade doesn't buy you runtime flexibility or SRP separation, it's over-engineering.
</details>

<details>
<summary><strong>Q18: [Advanced] How does Decorator relate to dynamic proxies and AOP?</strong></summary>

`java.lang.reflect.Proxy` (JDK dynamic proxies), CGLIB, and Spring AOP generate wrapper objects **at runtime** that intercept method calls and add cross-cutting behavior (transactions, security, logging, caching) — conceptually **decorators/proxies synthesized dynamically** instead of hand-written. Instead of writing a `LoggingDecorator` per interface, you write one `InvocationHandler`/`MethodInterceptor` and apply it to any interface.

Trade-offs vs. hand-written decorators: dynamic proxies work only through interfaces (JDK) or non-final classes (CGLIB), incur reflection/dispatch overhead, and obscure the call path in stack traces; but they scale to *any* interface without per-method boilerplate and centralize the cross-cutting logic. Spring's `@Transactional`, `@Cacheable`, and `@Async` are all delivered this way. Mentioning this shows you understand Decorator as a *concept* that frameworks industrialize, not just a textbook class layout.
</details>

<details>
<summary><strong>Q19: [Advanced] How do decorators interact with serialization?</strong></summary>

Serializing a decorated object serializes the **entire chain** (the outer decorator plus its `wrappee` reference graph), provided every layer is `Serializable`. Pitfalls: (1) if any layer isn't `Serializable`, the whole thing throws `NotSerializableException`; (2) **transient decorator state** (caches, buffers, counters) is lost on deserialization and must be rebuilt — mark it `transient` and re-initialize in `readObject`; (3) the deserialized object graph is a **new chain**, so identity is again not preserved. For stream decorators specifically, you generally serialize the *data*, not the stream objects — streams wrap live resources (file handles, sockets) that can't be meaningfully serialized. Also, decorators that add behavior via lambdas (functional form) are typically **not serializable** unless the functional interface extends `Serializable`. In distributed systems, prefer serializing the underlying value/DTO and re-applying decorators (compression/encryption) on the receiving side.
</details>

<details>
<summary><strong>Q20: [Advanced/Coding Challenge] Design a rate-limiting + retry + circuit-breaker resilience stack as decorators.</strong></summary>

A staff-level favorite: build resilience concerns as independent, composable decorators around a `Client` — the design behind Resilience4j and Hystrix.

<details>
<summary>💻 Click to expand full solution</summary>

```java
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;

interface Client { String call(String req) throws Exception; }

abstract class ClientDecorator implements Client {
    protected final Client delegate;
    protected ClientDecorator(Client d) { this.delegate = d; }
    public String call(String req) throws Exception { return delegate.call(req); }
}

// Retry with fixed attempts
class RetryClient extends ClientDecorator {
    private final int attempts;
    RetryClient(Client d, int attempts) { super(d); this.attempts = attempts; }
    @Override public String call(String req) throws Exception {
        Exception last = null;
        for (int i = 0; i < attempts; i++) {
            try { return super.call(req); }
            catch (Exception e) { last = e; }
        }
        throw last;
    }
}

// Token-bucket-ish rate limiter (simplified)
class RateLimitClient extends ClientDecorator {
    private final Semaphore permits;
    RateLimitClient(Client d, int maxConcurrent) { super(d); this.permits = new Semaphore(maxConcurrent); }
    @Override public String call(String req) throws Exception {
        if (!permits.tryAcquire()) throw new RejectedExecutionException("rate limited");
        try { return super.call(req); } finally { permits.release(); }
    }
}

// Circuit breaker: open after N consecutive failures
class CircuitBreakerClient extends ClientDecorator {
    private final int threshold;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private volatile boolean open = false;
    CircuitBreakerClient(Client d, int threshold) { super(d); this.threshold = threshold; }
    @Override public String call(String req) throws Exception {
        if (open) throw new IllegalStateException("circuit OPEN");
        try {
            String r = super.call(req);
            consecutiveFailures.set(0);        // reset on success
            return r;
        } catch (Exception e) {
            if (consecutiveFailures.incrementAndGet() >= threshold) open = true;
            throw e;
        }
    }
}

// Assemble: breaker(retry(ratelimit(realClient)))
class Demo {
    public static void main(String[] args) throws Exception {
        Client base = req -> { throw new RuntimeException("boom"); };
        Client resilient = new CircuitBreakerClient(
                               new RetryClient(
                                   new RateLimitClient(base, 10), 3), 2);
        try { resilient.call("ping"); } catch (Exception e) { System.out.println(e.getMessage()); }
    }
}
```
</details>

**Talking points:** each concern is independent and reorderable; ordering matters (do you want retries to count against the rate limit? put rate-limit inner; do you want the breaker to trip on post-retry failures? put breaker outer). `volatile` on the breaker flag ensures cross-thread visibility. This is exactly how Resilience4j composes decorators via `Decorators.ofSupplier(...).withRetry(...).withCircuitBreaker(...)`.
</details>

---

### 🧠 Staff / Principal Engineer Level

<details>
<summary><strong>SP1: [Staff] How do you make a decorator stack observable and debuggable in production?</strong></summary>

At scale, a decorator chain is effectively an unlabeled call pipeline, so invest in observability: (1) give each decorator a **name/id** and emit structured logs/spans at layer boundaries (e.g., OpenTelemetry spans named `cache`, `retry`, `db`) so a distributed trace shows the chain top-to-bottom; (2) expose **per-decorator metrics** (cache hit ratio, retry count, breaker state) via the decorator itself, since each already intercepts calls; (3) provide an **introspection/unwrap API** so operators can query "what layers are active on this instance?"; (4) ensure exceptions **preserve context** — wrap-and-rethrow with which layer failed rather than swallowing. The principle: because decorators are the natural interception points, they're also the natural instrumentation points — bake telemetry into the base decorator so every layer inherits it. Resilience4j and gRPC interceptors do exactly this.
</details>

<details>
<summary><strong>SP2: [Staff] Compare hand-written decorators vs. runtime-generated proxies (AOP) for a large service codebase.</strong></summary>

Hand-written decorators are **explicit, type-safe, and traceable** — you see the class, IDE navigation works, no reflection cost — but they require per-interface boilerplate and don't scale when 50 services all need the same 6 cross-cutting concerns. Runtime proxies (Spring AOP, dynamic proxies, bytecode weaving) **centralize** cross-cutting logic into one aspect applied declaratively (`@Transactional`), scaling effortlessly across many types, but at the cost of reflection/weaving overhead, interface-or-non-final constraints, confusing stack traces, and "spooky action at a distance" where behavior isn't visible at the call site. 

The staff-level judgment: use **explicit decorators** for behavior that's core to a component's contract and few in number (a caching repository), and use **AOP/proxies** for truly cross-cutting concerns that apply uniformly across many components (security, transactions, tracing). Mixing both is normal; the anti-pattern is using reflection-heavy AOP for something that's really just one decorator, or hand-writing the same decorator 40 times.
</details>

<details>
<summary><strong>SP3: [Principal] How does Decorator generalize to middleware in distributed systems and API gateways?</strong></summary>

The decorator concept scales beyond in-process objects to **network middleware**: an API gateway or service mesh sidecar (Envoy, gRPC interceptors, Express/Koa middleware, servlet filters) is a decorator chain over request handling — each layer (auth, rate-limit, TLS termination, compression, tracing, retries) wraps the next and can act before/after delegation. The same properties hold: order matters (auth before business logic, compression as the outermost transform), transparency (the handler doesn't know it's decorated), and composability. 

The key *difference* at distributed scale: failures, latency, and partial results become first-class. A network "decorator" must handle timeouts, backpressure, and the fact that "delegation" is now an RPC that may never return — so resilience decorators (retry, circuit breaker, bulkhead) move to the forefront. The mental model transfers directly: gRPC's `ClientInterceptor`/`ServerInterceptor` and Envoy's HTTP filter chain are Decorator applied at the wire level.
</details>

<details>
<summary><strong>SP4: [Principal] A team keeps adding decorators until chains are 12 layers deep and latency regresses. How do you address it architecturally?</strong></summary>

First **measure** — attribute latency per layer (spans/metrics already in the base decorator) to find whether the regression is indirection overhead (rarely the cause) or one expensive layer (usually the cause, e.g., a synchronous serialization decorator). Then apply judgment: (1) **collapse** decorators that always appear together and are cheap into a single class — decorators are a design tool, not a mandate for one-concern-per-class at any cost; (2) **short-circuit** — decorators that no-op for most requests (a feature-flag layer) should exit fast; (3) **move cross-cutting concerns to AOP/interceptors** so they're applied once at the boundary rather than nested per object; (4) enforce an **ordering/inventory contract** via a builder so the chain is intentional, not accreted. The organizational fix is a review gate: adding a decorator to a shared path requires justifying why it can't be merged and showing its latency budget. The principle — Decorator optimizes for *flexibility and separation*, and when a system needs *performance and simplicity* on a hot path, deliberately trade some decorator granularity away.
</details>

<details>
<summary><strong>SP5: [Principal] How do you design a decorator so it remains backward-compatible as the component interface evolves?</strong></summary>

Interface evolution is the Achilles' heel of Decorator (every layer must delegate every method). Strategies: (1) prefer **narrow, stable component interfaces** — segregate large interfaces (ISP) so decorators wrap small contracts; (2) add new methods as **`default` methods** on the interface so existing decorators keep compiling and inherit sensible pass-through/no-op behavior — but audit them, because a `default` that *doesn't* delegate through the chain silently bypasses inner decorators (a real hazard); (3) route all decorators through an **abstract base decorator** so a new method needs a delegating implementation in exactly *one* place, and every decorator inherits correct forwarding; (4) treat the base decorator's completeness as an **invariant enforced by tests** — a contract test that verifies each method is forwarded. 

The subtle trap with `default` methods: if the interface's `default` implementation does real work instead of delegating, a decorator that doesn't override it will run the interface default rather than the wrappee's version, breaking the chain. So on decorated interfaces, `default` methods should be *pure conveniences that call other interface methods*, never standalone behavior.
</details>

---

## ⚡ Quick Revision

**One-liner:** Decorator wraps an object in another object of the *same interface* to add behavior dynamically at runtime — composition over inheritance, stackable.

**The whole pattern in one paragraph:** The Decorator is a structural GoF pattern — *"attach additional responsibilities to an object dynamically; a flexible alternative to subclassing."* It exists to kill **combination explosion**: with `N` independent, combinable features, inheritance needs up to `2^N` subclasses and binds behavior at compile time, whereas Decorator needs just `N` classes composed at runtime. The structure is four participants — a **Component** interface, a **Concrete Component** (the real object), an abstract **Decorator** that implements Component *and holds a Component reference* providing default pass-through delegation, and **Concrete Decorators** that override methods to add behavior before/after calling `super` (the delegate). It works via **polymorphic delegation**: each decorator *is-a* Component (so it's substitutable and transparent to clients) and *has-a* Component (the wrappee), so calls cascade recursively inward and the chain unwinds symmetrically — which is why **order matters** for non-commutative decorators (compress-then-encrypt, not the reverse) and why reads reverse writes automatically. The canonical JDK example is **`java.io`** (`BufferedReader`→`InputStreamReader`→`GZIPInputStream`→`FileInputStream`), plus `Collections.unmodifiableList/synchronizedList` and Servlet `HttpServletRequestWrapper`. Use it for optional, combinable, runtime-toggleable responsibilities and layered pipelines (I/O, middleware, resilience stacks like Resilience4j); avoid it when only one combination exists, the interface is wide/volatile, or object identity/`instanceof` matters (a decorator breaks `==` and type checks — hence JDBC's `unwrap()`). Its cousins share structure but differ in intent: **Proxy** controls access, **Adapter** changes the interface, **Composite** aggregates a tree, **Strategy** swaps internal algorithm. Watch the pitfalls: wrong ordering (silent), broken identity, delegation boilerplate on wide interfaces (fix with the abstract base decorator), and thread-unsafe stateful decorators (use `ConcurrentHashMap`/atomics/`volatile`). At scale it generalizes to AOP/dynamic proxies and network middleware (gRPC interceptors, Envoy filters).

**Top 5 interview answers to memorize:**

1. **"What is it?"** → Attach responsibilities to an object dynamically by wrapping it in an object of the same interface; a flexible alternative to subclassing (composition over inheritance).
2. **"Why not inheritance?"** → Inheritance is static and per-class and explodes to `2^N` classes for `N` combinable features; Decorator composes `N` classes at runtime per object.
3. **"Real JDK example?"** → `java.io` streams (`BufferedInputStream`, `GZIPInputStream`, `InputStreamReader`); `FilterInputStream` is the abstract base decorator; also `Collections.unmodifiableList`.
4. **"Decorator vs. Proxy?"** → Same interface; Decorator *adds behavior* and is stackable, Proxy *controls access* (lazy/remote/security) and manages the target's lifecycle.
5. **"How does it break?"** → Wrong ordering gives silently wrong output; it breaks `==`/`instanceof` (use `unwrap()`); stateful decorators need thread safety; wide interfaces need the abstract base decorator to avoid forgotten-method bugs.

**Trigger words (hear these → think Decorator):** "add features/responsibilities dynamically", "at runtime", "optional add-ons / toppings / condiments", "in any combination", "wrap / wrapper", "layers", "middleware / interceptors / filters", "without modifying the original class", "avoid subclass explosion", "buffering + compression + encryption", "toggle behavior on/off", "same interface but extra behavior", "stack of behaviors".

---

*End of Decorator Pattern study guide.*







# Builder Pattern ⭐⭐ (Difficulty: 2/5 — the fluent builder is easy to write, but the GoF Director form, Step Builder state machines, recursive-generics inheritance, and the records/Lombok trade-offs are where senior discussions go)

> **Category:** Creational Pattern (GoF)
> **Also known as:** (fluent) Builder

The Builder pattern **separates the construction of a complex object from its representation**, so the same construction process can build different results — and, in its modern Java form, it lets you assemble an immutable object step by step with a readable, fluent API instead of a monstrous constructor. Rather than calling `new User("John","Doe",30,null,null,true,"US")` (a telescoping-constructor nightmare) or exposing setters (which make the object mutable), you chain named calls on a builder and finish with `build()`, which validates and produces a finished, immutable product. It's the pattern behind `StringBuilder`, `HttpClient`/`HttpRequest`, Protobuf message builders, Lombok's `@Builder`, and virtually every well-designed configuration object in production Java.

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 0: Telescoping Constructors & JavaBeans Setters (Anti-patterns)](#variant-0-telescoping-constructors--javabeans-setters-anti-patterns)
   - [Variant 1: Classic GoF Builder with a Director](#variant-1-classic-gof-builder-with-a-director)
   - [Variant 2: Effective-Java Fluent Static-Inner-Class Builder](#variant-2-effective-java-fluent-static-inner-class-builder)
   - [Variant 3: Validated Builder (Invariants + Defensive Copies)](#variant-3-validated-builder-invariants--defensive-copies)
   - [Variant 4: Step / Staged Builder (Compile-Time Required Fields)](#variant-4-step--staged-builder-compile-time-required-fields)
5. [🎨 Real-World Example](#-real-world-example)
   - [Scenario 1 — HTTP Request / Notification Builder](#scenario-1--http-request--notification-builder)
   - [Scenario 2 — SQL Query Builder](#scenario-2--sql-query-builder)
   - [Scenario 3 — AWS SDK Request Builder (S3 PutObject)](#scenario-3--aws-sdk-request-builder-s3-putobject)
   - [Scenario 4 — Kubernetes Pod Spec Builder](#scenario-4--kubernetes-pod-spec-builder)
6. [✅ When to Use](#-when-to-use)
7. [❌ When NOT to Use](#-when-not-to-use)
8. [🎯 Pros and Cons](#-pros-and-cons)
9. [🔄 Comparison with Related/Similar Patterns](#-comparison-with-relatedsimilar-patterns)
10. [📊 Comparison Table of Variants](#-comparison-table-of-variants)
11. [💡 Common Pitfalls](#-common-pitfalls)
12. [🎓 Interview Tips](#-interview-tips)
13. [🔗 Related Patterns](#-related-patterns)
14. [📚 Library/Framework Implementation](#-libraryframework-implementation)
15. [📝 Interview Questions & Answers (FAANG Top 20)](#-interview-questions--answers-faang-top-20)
16. [⚡ Quick Revision](#-quick-revision)

---

## 📋 Intent

> **GoF Definition:** *"Separate the construction of a complex object from its representation so that the same construction process can create different representations."*

There are really two lenses on Builder, and a strong answer names both. The **original GoF intent** is about *decoupling how an object is assembled from what it ends up being*: a **Director** runs a fixed construction sequence, while interchangeable **Builder** implementations produce different representations from that same sequence (e.g., the same "parse this document" process driving either an HTML builder or a plain-text builder). The **modern Java intent** (popularized by *Effective Java* Item 2) is narrower and far more common: use a fluent builder to construct an **immutable object that has many parameters, several of them optional**, without telescoping constructors and without mutable setters.

The unifying insight is the same: **move construction out of the object's constructor and into a dedicated builder that accumulates state and produces the finished product in one final step.** This gives you readable, order-independent, named parameters; the ability to validate the whole object atomically at `build()` time; and immutability (the product has no setters, only a private constructor the builder calls). The construction *process* becomes a first-class thing you can control, validate, stage, or vary — instead of a fixed, positional, all-or-nothing constructor call.

---

## 🎯 Problem

You have a class with **many constructor parameters, several optional**, and constructing it cleanly is hard. The classic bad solutions each fail in their own way: **telescoping constructors** (a chain of overloads with growing argument lists) are unreadable and fragile, and **JavaBeans setters** (a no-arg constructor plus `setX()` calls) make the object mutable and allow it to be observed in a half-built, inconsistent state.

The pain points that lead you to Builder: the **telescoping-constructor anti-pattern** — `new User(a,b,c,d,e,f)` where you can't tell what each argument means and can swap two same-typed parameters without a compile error; **poor readability** at the call site; the **immutability-vs-flexibility conflict** — to avoid telescoping you reach for setters, but setters destroy immutability and thread-safety; and **inconsistent/partial state** — an object built via setters can be used before all required fields are set.

**Concrete example scenarios:**

1. **HTTP request construction.** A request has a required URL but optional headers, body, method, timeout, and redirect policy. `HttpRequest.newBuilder(uri).header(...).POST(...).timeout(...).build()` reads clearly; a 6-arg constructor would not.

2. **Configuration objects.** An `AppConfig` / client config with dozens of tunables, most with sensible defaults — you want to set only the few you care about, by name, and get back an immutable, validated config.

3. **Complex domain objects / DTOs.** A `User`, `Order`, or `Pizza` with required and optional fields, where you want immutability and validation (no negative ages, no null required fields) enforced centrally.

4. **SQL / query construction.** A query assembled from optional `WHERE`, `JOIN`, `ORDER BY`, `LIMIT` clauses — a fluent builder maps naturally onto "add a clause if you need it."

---

## ✅ Solution

The core idea, in plain language: **give the complex object a private constructor, and create a companion Builder that collects each field through named methods, then produces the finished object in a single `build()` call.** The client never touches the object's constructor; it talks only to the builder, which acts as a mutable staging area for the eventually-immutable product.

**Key structural elements:**

- **Product:** the complex object being built. Its fields are `final`; its constructor is `private` (takes the builder). It has no setters, so it's immutable once built.
- **Builder:** usually a `static` nested class of the product. It holds a mutable copy of each field, exposes a **fluent** method per field (each returns `this` for chaining), and has a `build()` method.
- **Fluent interface:** the `return this` mechanism that enables method chaining (`.setX(..).setY(..)`).
- **`build()`:** the terminal operation that validates invariants, makes defensive copies of mutable inputs, and invokes the product's private constructor.
- **(GoF-only) Director:** an optional object that encapsulates a *fixed construction sequence*, driving a builder to produce a specific representation. Absent from the common fluent form; central to the original GoF form.

**The mechanism that makes it work:** *a static nested Builder has access to the product's `private` constructor* (so no external code can bypass it), and *`return this` from each setter enables fluent chaining* (so you get named, order-independent parameters). Immutability is achieved by making product fields `final`, defensive-copying mutable inputs, and exposing no setters — the builder is the *only* path to an instance, and `build()` is a single **validation gate** where all invariants are checked atomically, preventing "zombie objects" (technically-constructed but semantically-invalid).

The **central trade-off** to name: Builder adds **boilerplate** (duplicated fields in product and builder, a nested class, one method per field) and an **extra allocation** (the builder object) per product. For simple objects this is pure ceremony; in ultra-low-latency hot loops the extra allocation adds GC pressure. The payoffs — readability, immutability, atomic validation, and easy optionality — dominate for the "many parameters, mostly optional, want immutable" case, and tools like Lombok `@Builder` or Java `record`s reduce the boilerplate.

---

## 💻 Implementation

We'll build a `User`-style object, moving from the telescoping/setters anti-patterns to a compile-time-safe Step Builder. Read the variants top-to-bottom: each fixes a specific weakness of the previous one.

### Variant 0: Telescoping Constructors & JavaBeans Setters (Anti-patterns)

**What's wrong with them:** Two bad solutions to the same problem. **Telescoping constructors** chain overloads with increasing arguments — unreadable at the call site, easy to transpose two same-typed parameters without a compile error, and a maintenance nightmare (each new optional field spawns more overloads). **JavaBeans setters** (no-arg constructor + `setX()`) fix readability but destroy **immutability**: the object is mutable, not thread-safe, and can be observed in a **partially-initialized, inconsistent state** (used before required fields are set). Neither can validate the whole object atomically.

<details>
<summary>💻 Click to expand code — telescoping constructors + setters (both anti-patterns)</summary>

```java
// ANTI-PATTERN 1: Telescoping constructors
public class UserTelescoping {
    private final String firstName, lastName, phone, address;
    private final int age;
    public UserTelescoping(String f, String l) { this(f, l, 0, null, null); }
    public UserTelescoping(String f, String l, int age) { this(f, l, age, null, null); }
    public UserTelescoping(String f, String l, int age, String phone) { this(f, l, age, phone, null); }
    public UserTelescoping(String f, String l, int age, String phone, String address) {
        this.firstName = f; this.lastName = l; this.age = age; this.phone = phone; this.address = address;
    }
    // Call site: new UserTelescoping("John","Doe",30,"555","NYC") — which arg is which?
}

// ANTI-PATTERN 2: JavaBeans setters — mutable, can be half-built
public class UserBeans {
    private String firstName; private int age;
    public UserBeans() {}
    public void setFirstName(String f) { this.firstName = f; }
    public void setAge(int a) { this.age = a; }
    // Mutable forever; usable before setFirstName is ever called -> inconsistent state.
}
```
</details>

**Pros:** No extra classes; telescoping executes fast; setters are readable.
**Cons:** Telescoping is unreadable and parameter-order-fragile; setters break immutability, thread-safety, and allow inconsistent/partial state; neither validates atomically.
**Mechanism:** *Constructor overloading via `this(...)` forwarding* (telescoping) or *mutable field assignment* (beans) — both lack a single controlled construction+validation point.

### Variant 1: Classic GoF Builder with a Director

**What problem it solves vs. V0:** This is the *original* GoF form, which the modern fluent form is often mistaken for. A `Builder` interface declares granular build steps; concrete builders produce **different representations**; and a **Director** encapsulates a *fixed construction sequence*, calling the steps in order. The client picks a builder, hands it to the director, and the director runs the same process to yield whatever representation that builder implements. This decouples *how* an object is assembled (the sequence, in the director) from *what* it becomes (the representation, in the builder).

**What's still limited:** For the common "immutable object with optional params" need, the Director is overkill — you rarely have multiple representations, and the extra interface + director is ceremony. That's why the fluent form (Variant 2) dominates modern Java. But knowing this form separates candidates who understand *GoF Builder* from those who only know *fluent builders*.

<details>
<summary>💻 Click to expand code — GoF Director + Builder (multiple representations)</summary>

```java
// Product: a report that can be built as HTML or plain text (different representations)
interface Report { String render(); }

// Builder interface: granular construction steps
interface ReportBuilder {
    void addTitle(String title);
    void addParagraph(String text);
    Report getResult();
}

// Concrete builder 1 — HTML representation
class HtmlReportBuilder implements ReportBuilder {
    private final StringBuilder sb = new StringBuilder();
    public void addTitle(String t) { sb.append("<h1>").append(t).append("</h1>"); }
    public void addParagraph(String p) { sb.append("<p>").append(p).append("</p>"); }
    public Report getResult() { String html = sb.toString(); return () -> html; }
}
// Concrete builder 2 — plain-text representation
class TextReportBuilder implements ReportBuilder {
    private final StringBuilder sb = new StringBuilder();
    public void addTitle(String t) { sb.append(t.toUpperCase()).append("\n"); }
    public void addParagraph(String p) { sb.append(p).append("\n"); }
    public Report getResult() { String txt = sb.toString(); return () -> txt; }
}

// Director — owns the FIXED construction sequence, agnostic of representation
class ReportDirector {
    void construct(ReportBuilder b) {              // same process...
        b.addTitle("Quarterly Results");
        b.addParagraph("Revenue up 12%.");
        b.addParagraph("Costs flat.");
    }
}

// Client: same director + process -> different representations by swapping the builder
class GofDemo {
    public static void main(String[] a) {
        ReportDirector director = new ReportDirector();
        ReportBuilder html = new HtmlReportBuilder();
        director.construct(html);
        System.out.println(html.getResult().render());   // HTML

        ReportBuilder text = new TextReportBuilder();
        director.construct(text);                          // identical process
        System.out.println(text.getResult().render());     // plain text
    }
}
```
</details>

**Pros:** Cleanly separates construction *process* (director) from *representation* (builder); the same sequence produces multiple output types; good when you truly have several representations.
**Cons:** Heavy for the common "one immutable object" case; the director adds ceremony rarely needed in modern Java; not a fluent API.
**Mechanism:** *Director-driven sequencing + polymorphic builder implementations.* The director calls abstract steps; the concrete builder decides how each step contributes to its representation.

### Variant 2: Effective-Java Fluent Static-Inner-Class Builder

**What problem it solves vs. V0/V1:** This is the idiom you'll write 95% of the time. A `static` nested `Builder` holds mutable fields, exposes a **fluent setter per field** (`return this`), and a `build()` that calls the product's `private` constructor. Required fields go in the builder's constructor; optional ones default and are set fluently. It eliminates telescoping, gives named/optional parameters, and produces an **immutable** product — without a director.

**What's still limited:** By itself it doesn't **validate** (you can `build()` a semantically invalid object) and doesn't enforce **required fields at compile time** (a caller can forget one). Those are addressed in Variants 3 and 4.

<details>
<summary>💻 Click to expand code — fluent static-inner-class builder</summary>

```java
public class User {
    private final String firstName;   // required
    private final String lastName;    // required
    private final int age;            // optional
    private final String phone;       // optional

    private User(Builder b) {         // private: only the builder can construct
        this.firstName = b.firstName;
        this.lastName  = b.lastName;
        this.age       = b.age;
        this.phone     = b.phone;
    }

    public static Builder builder(String firstName, String lastName) {
        return new Builder(firstName, lastName);
    }

    public static class Builder {
        private final String firstName;   // required -> in builder constructor
        private final String lastName;
        private int age = 0;               // optional -> defaults
        private String phone = null;

        public Builder(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName  = lastName;
        }
        public Builder age(int age)      { this.age = age; return this; }     // fluent
        public Builder phone(String p)   { this.phone = p; return this; }
        public User build()              { return new User(this); }
    }

    public String getFirstName() { return firstName; }
    // ...other getters; NO setters -> immutable
}
```

```java
// Call site — readable, named, order-independent, only set what you need:
User u = User.builder("John", "Doe")
             .age(30)
             .phone("555-1234")
             .build();
```
</details>

**Pros:** Eliminates telescoping; readable named parameters; optional fields with defaults; immutable product; single terminal `build()`.
**Cons:** Boilerplate (fields duplicated in product + builder, one method per field); no validation or required-field enforcement yet; a builder allocation per product.
**Mechanism:** *Static nested class accessing the outer product's `private` constructor* + *`return this` fluent chaining*. The builder is the sole construction path; `final` fields + no setters ⇒ immutability.

### Variant 3: Validated Builder (Invariants + Defensive Copies)

**What problem it solves vs. V2:** In production you can't trust callers to supply valid data, and you must protect immutability against reference leaks. This variant adds a **validation gate** in `build()`/the private constructor (`Objects.requireNonNull` for required fields, range/business checks) so an invalid object can never be created — the failure happens at construction, not deep in business logic later. It also **defensively copies mutable inputs** (collections) so the "immutable" product can't be mutated through a retained reference.

**What's still limited:** It validates at *runtime*; it still can't force required fields to be set at *compile time* (that's the Step Builder, Variant 4).

<details>
<summary>💻 Click to expand code — validated builder with defensive copies</summary>

```java
import java.util.*;

public class User {
    private final String firstName;
    private final String lastName;
    private final int age;
    private final List<String> roles;   // mutable input -> must defensively copy

    private User(Builder b) {
        // Validation gate: invalid objects can NEVER be constructed.
        this.firstName = Objects.requireNonNull(b.firstName, "firstName is required");
        this.lastName  = Objects.requireNonNull(b.lastName,  "lastName is required");
        if (b.age < 0 || b.age > 150) throw new IllegalArgumentException("age out of range: " + b.age);
        // Defensive copy + unmodifiable wrapper -> true immutability.
        this.roles = List.copyOf(b.roles);
        this.age = b.age;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String firstName, lastName;
        private int age;
        private List<String> roles = new ArrayList<>();

        public Builder firstName(String f) { this.firstName = f; return this; }
        public Builder lastName(String l)  { this.lastName = l;  return this; }
        public Builder age(int a)          { this.age = a;       return this; }
        public Builder addRole(String r)   { this.roles.add(r);  return this; }

        public User build() { return new User(this); }   // constructor enforces invariants
    }

    public List<String> getRoles() { return roles; }     // already unmodifiable
}
```

```java
// Valid:
User u = User.builder().firstName("Jane").lastName("Smith").age(28).addRole("ADMIN").build();
// Throws at build() — never creates a zombie object:
// User bad = User.builder().age(28).build();   // NPE: firstName is required
```
</details>

**Pros:** Guarantees object integrity (no zombie objects); centralizes validation; true immutability via defensive copies + unmodifiable collections; fails fast at construction.
**Cons:** More code in `build()`/constructor; slight validation overhead; still no compile-time required-field enforcement.
**Mechanism:** *Validation gate at the single construction point* (`Objects.requireNonNull`, range checks) + *defensive copying* (`List.copyOf`) to prevent reference leaks into an immutable object.

### Variant 4: Step / Staged Builder (Compile-Time Required Fields)

**What problem it solves vs. V3:** V3 catches missing required fields at *runtime*; the **Step Builder** catches them at *compile time*. You define a chain of single-method interfaces (`CustomerStep → ItemStep → QuantityStep`), each returning the *next* step, so the compiler forces the caller to set required fields **in order** before `build()` is even reachable. This is effectively a compile-time state machine over construction — impossible to forget a required field or call `build()` too early.

**What's the trade-off:** Considerably more boilerplate (an interface per required step) and a rigid call order, so it's reserved for objects where getting construction wrong is costly (public APIs, critical domain objects). For inheritance across a hierarchy, a related technique is **recursive generics** (CRTP): `Builder<T extends Builder<T>>` so base setters return the subclass type and keep the fluent chain.

<details>
<summary>💻 Click to expand code — Step Builder (compile-time required fields)</summary>

```java
public class Order {
    private final String customerId;
    private final String item;
    private final int quantity;

    private Order(OrderBuilder b) {
        this.customerId = b.customerId; this.item = b.item; this.quantity = b.quantity;
    }

    // One interface per mandatory step; each returns the NEXT step.
    public interface CustomerStep { ItemStep customer(String id); }
    public interface ItemStep     { QuantityStep item(String name); }
    public interface QuantityStep { Order build(int qty); }

    public static CustomerStep builder() { return new OrderBuilder(); }

    private static class OrderBuilder implements CustomerStep, ItemStep, QuantityStep {
        private String customerId, item; private int quantity;
        public ItemStep customer(String id)  { this.customerId = id; return this; }
        public QuantityStep item(String name){ this.item = name;     return this; }
        public Order build(int qty)          { this.quantity = qty;  return new Order(this); }
    }
}

// Usage — compiler enforces order and completeness:
// Order o = Order.builder().customer("C-1").item("MacBook").build(1);
// Order bad = Order.builder().item("MacBook");  // COMPILE ERROR: item() not on CustomerStep
```
</details>

<details>
<summary>💻 Click to expand — recursive-generics builder for inheritance (CRTP)</summary>

```java
abstract class Vehicle {
    final int wheels;
    Vehicle(Builder<?> b) { this.wheels = b.wheels; }
    // CRTP: T is the concrete subclass builder, so base setters keep the fluent chain typed.
    abstract static class Builder<T extends Builder<T>> {
        private int wheels;
        @SuppressWarnings("unchecked")
        T self() { return (T) this; }
        public T wheels(int w) { this.wheels = w; return self(); }
        abstract Vehicle build();
    }
}
class Car extends Vehicle {
    final int doors;
    private Car(Builder b) { super(b); this.doors = b.doors; }
    static class Builder extends Vehicle.Builder<Builder> {
        private int doors;
        public Builder doors(int d) { this.doors = d; return self(); }
        Car build() { return new Car(this); }
    }
}
// new Car.Builder().wheels(4).doors(2).build();  // base setter returns Car.Builder, chain intact
```
</details>

**Pros:** Compile-time enforcement of required fields and order (impossible to forget one); self-documenting construction flow; CRTP extends fluency to class hierarchies.
**Cons:** Heavy boilerplate (interface per step); rigid mandatory order; CRTP generics are advanced/verbose.
**Mechanism:** *Staged interfaces as a compile-time state machine* — each step's return type only exposes the next legal method, so illegal construction sequences don't compile. CRTP's `self()` cast preserves the concrete builder type through inherited setters.

---

## 🎨 Real-World Example

Four complete, production-shaped examples, each fully collapsible. The first is a **notification/HTTP-style builder** with optional fields, defaults, and a defensive-copied collection. The second is a **SQL query builder** that assembles optional clauses. The third mirrors a real **AWS SDK request builder** (S3 `PutObjectRequest`). The fourth builds a **Kubernetes Pod spec** with nested builders. Each ends with a walkthrough of *why the code is shaped the way it is*, and every scenario notes that the *builder* is not thread-safe while the *product* is immutable.

### Scenario 1 — HTTP Request / Notification Builder

<details>
<summary>💻 Click to expand — notification builder + demo + explanation</summary>

A `Notification` with a required recipient and message, plus optional subject, urgency, and tags. `build()` validates and defensively copies the tag list. The demo builds a minimal and a fully-configured notification.

```java
import java.util.*;

public final class Notification {
    private final String recipient;      // required
    private final String message;        // required
    private final String subject;        // optional (default)
    private final boolean urgent;        // optional (default)
    private final List<String> tags;     // optional, defensively copied

    private Notification(Builder b) {
        this.recipient = Objects.requireNonNull(b.recipient, "recipient is required");
        this.message   = Objects.requireNonNull(b.message,   "message is required");
        this.subject   = b.subject;
        this.urgent    = b.urgent;
        this.tags      = List.copyOf(b.tags);   // immutable snapshot
    }

    public static Builder builder(String recipient, String message) {
        return new Builder(recipient, message);
    }

    public static final class Builder {
        private final String recipient;
        private final String message;
        private String subject = "(no subject)";
        private boolean urgent = false;
        private final List<String> tags = new ArrayList<>();

        private Builder(String recipient, String message) {
            this.recipient = recipient; this.message = message;
        }
        public Builder subject(String s) { this.subject = s; return this; }
        public Builder urgent(boolean u) { this.urgent = u; return this; }
        public Builder addTag(String t)  { this.tags.add(t); return this; }
        public Notification build()      { return new Notification(this); }
    }

    @Override public String toString() {
        return "To " + recipient + " [" + subject + "] urgent=" + urgent + " tags=" + tags;
    }
}
```

```java
public class NotificationDemo {
    public static void main(String[] args) {
        // Minimal: only required fields.
        Notification basic = Notification.builder("user@x.com", "Hello!").build();

        // Fully configured — readable, order-independent:
        Notification alert = Notification.builder("oncall@x.com", "Server down!")
                .subject("CRITICAL")
                .urgent(true)
                .addTag("infra").addTag("prod").addTag("p1")
                .build();

        System.out.println(basic);   // To user@x.com [(no subject)] urgent=false tags=[]
        System.out.println(alert);   // To oncall@x.com [CRITICAL] urgent=true tags=[infra, prod, p1]

        // NOTE: a Builder instance is NOT thread-safe — confine it to one thread while setting.
        // The Notification product IS immutable and safely shareable across threads.
    }
}
```

**Code explanation.** The **product** `Notification` is immutable: every field is `final`, the constructor is `private`, and there are no setters — so once `build()` returns, the object can't change. Required fields (`recipient`, `message`) are passed to `builder(...)` so you can't even start a builder without them, while optional fields (`subject`, `urgent`, `tags`) get sensible defaults in the builder and are set through fluent methods that each `return this` for chaining. Two production details matter: (1) `build()` (via the private constructor) is the single **validation gate** — `Objects.requireNonNull` guarantees no "zombie" notification with a null recipient ever escapes; and (2) `List.copyOf(b.tags)` makes a **defensive, unmodifiable snapshot** of the tags, so a caller who keeps mutating the builder (or a leaked list reference) can't retroactively alter a built notification. The demo shows the ergonomic payoff — a minimal call sets only the two required fields, a rich call reads like named parameters in any order — and the closing note captures the thread-safety contract: the mutable builder must be confined to one thread while you configure it, but the resulting immutable `Notification` is freely shareable.

</details>

### Scenario 2 — SQL Query Builder

<details>
<summary>💻 Click to expand — SQL query builder + demo + explanation</summary>

A fluent builder that assembles a `SELECT` with optional `WHERE`, `ORDER BY`, and `LIMIT`. Only the table is required; everything else is added if needed.

```java
import java.util.*;

public final class Query {
    private final String table;
    private final List<String> columns;
    private final String where;
    private final String orderBy;
    private final Integer limit;

    private Query(Builder b) {
        this.table   = Objects.requireNonNull(b.table, "table is required");
        this.columns = b.columns.isEmpty() ? List.of("*") : List.copyOf(b.columns);
        this.where   = b.where;
        this.orderBy = b.orderBy;
        this.limit   = b.limit;
    }

    public static Builder from(String table) { return new Builder(table); }

    public String toSql() {
        StringBuilder sb = new StringBuilder("SELECT ")
                .append(String.join(", ", columns)).append(" FROM ").append(table);
        if (where   != null) sb.append(" WHERE ").append(where);
        if (orderBy != null) sb.append(" ORDER BY ").append(orderBy);
        if (limit   != null) sb.append(" LIMIT ").append(limit);
        return sb.toString();
    }

    public static final class Builder {
        private final String table;
        private final List<String> columns = new ArrayList<>();
        private String where, orderBy;
        private Integer limit;

        private Builder(String table) { this.table = table; }
        public Builder select(String... cols) { columns.addAll(Arrays.asList(cols)); return this; }
        public Builder where(String cond)      { this.where = cond;    return this; }
        public Builder orderBy(String col)     { this.orderBy = col;   return this; }
        public Builder limit(int n)            { this.limit = n;       return this; }
        public Query build()                   { return new Query(this); }
    }
}
```

```java
public class QueryDemo {
    public static void main(String[] args) {
        Query q1 = Query.from("users").build();
        Query q2 = Query.from("users")
                .select("id", "name")
                .where("age > 18")
                .orderBy("name")
                .limit(50)
                .build();

        System.out.println(q1.toSql()); // SELECT * FROM users
        System.out.println(q2.toSql()); // SELECT id, name FROM users WHERE age > 18 ORDER BY name LIMIT 50
    }
}
```

**Code explanation.** This shows Builder handling **optional, order-independent clauses**: only `table` is required (it's passed to the `from(...)` entry point), while `select`, `where`, `orderBy`, and `limit` are all optional and may be supplied in any order or omitted entirely. The private constructor turns the accumulated builder state into an immutable `Query` and applies a small piece of business logic — defaulting `columns` to `["*"]` when none were chosen — which is exactly the kind of normalization a `build()` gate is good for. `toSql()` then assembles the string by appending each clause only if its field is non-null, so the same builder naturally produces `SELECT * FROM users` or a fully-qualified query. Note the use of `Integer limit` (a nullable boxed type) rather than `int`: it lets "no limit" be represented as `null`, distinct from `LIMIT 0` — a common Builder technique for optional primitives. As with all builders, the `Query.Builder` is a mutable, single-thread accumulator, while the resulting `Query` is immutable and safe to cache or share. (In real code you'd bind values as parameters rather than concatenating, to avoid SQL injection — the builder structure is identical.)

</details>

### Scenario 3 — AWS SDK Request Builder (S3 PutObject)

<details>
<summary>💻 Click to expand — AWS-SDK-style request builder + demo + explanation</summary>

This mirrors how the real **AWS SDK for Java v2** models requests: every request object (e.g. `PutObjectRequest`) is immutable and built through a nested fluent builder obtained from `Builder`/`builder()`, with required fields validated in `build()` and optional metadata accumulated into a defensively-copied map. Here we reconstruct a faithful miniature of `S3Client.putObject(PutObjectRequest)`.

```java
import java.util.*;

// Immutable request object — the "product". Mirrors software.amazon.awssdk...PutObjectRequest.
public final class PutObjectRequest {
    private final String bucket;                 // required
    private final String key;                    // required
    private final String contentType;            // optional
    private final String storageClass;           // optional (default STANDARD)
    private final Map<String, String> metadata;  // optional, defensively copied

    private PutObjectRequest(Builder b) {
        this.bucket       = Objects.requireNonNull(b.bucket, "bucket must not be null");
        this.key          = Objects.requireNonNull(b.key, "key must not be null");
        this.contentType  = b.contentType;
        this.storageClass = b.storageClass;
        this.metadata     = Map.copyOf(b.metadata);   // immutable snapshot
    }

    public static Builder builder() { return new Builder(); }   // SDK-style entry point

    public String bucket() { return bucket; }
    public String key()    { return key; }
    @Override public String toString() {
        return "PutObjectRequest{bucket=" + bucket + ", key=" + key
             + ", contentType=" + contentType + ", storageClass=" + storageClass
             + ", metadata=" + metadata + "}";
    }

    public static final class Builder {
        private String bucket;
        private String key;
        private String contentType;
        private String storageClass = "STANDARD";
        private final Map<String, String> metadata = new HashMap<>();

        private Builder() {}
        public Builder bucket(String b)        { this.bucket = b; return this; }
        public Builder key(String k)           { this.key = k; return this; }
        public Builder contentType(String ct)  { this.contentType = ct; return this; }
        public Builder storageClass(String sc) { this.storageClass = sc; return this; }
        public Builder metadata(String k, String v) { this.metadata.put(k, v); return this; }
        public PutObjectRequest build()        { return new PutObjectRequest(this); }
    }
}

// Minimal client that accepts the built request — mirrors S3Client.putObject(request).
class S3Client {
    String putObject(PutObjectRequest req) {
        System.out.println("PUT s3://" + req.bucket() + "/" + req.key());
        return "etag-" + Math.abs(req.key().hashCode());
    }
}
```

```java
public class AwsRequestDemo {
    public static void main(String[] args) {
        // Fluent, SDK-idiomatic construction of an immutable request:
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket("my-app-uploads")
                .key("2026/report.pdf")
                .contentType("application/pdf")
                .metadata("x-owner", "billing")
                .metadata("x-env", "prod")
                .build();

        String etag = new S3Client().putObject(request);
        System.out.println(request);
        System.out.println("stored, etag=" + etag);
        // Output:
        // PUT s3://my-app-uploads/2026/report.pdf
        // PutObjectRequest{bucket=my-app-uploads, key=2026/report.pdf, contentType=application/pdf,
        //                  storageClass=STANDARD, metadata={x-owner=billing, x-env=prod}}
        // stored, etag=...
    }
}
```

**Code explanation.** This is the exact idiom the AWS SDK v2 uses across thousands of request types, and it's worth knowing because interviewers love "where have you seen Builder in a real API?" The **product** `PutObjectRequest` is immutable (all `final`, private constructor, no setters), and the SDK deliberately exposes construction only through `builder()` → fluent setters → `build()`, so a request can never be half-configured or mutated after creation — important when the SDK may retry and re-serialize the same request object across threads. Required identifiers (`bucket`, `key`) are validated in `build()` with `requireNonNull`, turning a missing field into an immediate, well-located error rather than a confusing failure deep in the HTTP layer. Optional fields carry defaults (`storageClass = "STANDARD"`), and the free-form `metadata` map is accumulated via a repeatable `metadata(k, v)` method then frozen with `Map.copyOf(...)` — a defensive copy so the immutable request can't be mutated through a retained builder reference. The client call `s3.putObject(request)` takes the finished product, exactly like the real `S3Client`. The takeaway to state in an interview: SDKs favor Builder precisely because request objects have **many optional fields**, must be **immutable and thread-safe** (for safe retries), and need to **evolve without breaking callers** — adding a new optional field is just a new builder method, never a constructor signature change.

</details>

### Scenario 4 — Kubernetes Pod Spec Builder

<details>
<summary>💻 Click to expand — Kubernetes Pod spec builder (nested builders) + demo + explanation</summary>

Kubernetes client libraries (e.g. the Fabric8 Java client) build resource specs with **nested fluent builders**: a `Pod` contains a spec with a list of `Container`s, each with its own ports, env vars, and resource limits. This scenario shows a Builder composing *sub-builders*, which is how Builder scales to deeply-structured objects.

```java
import java.util.*;

// ---- Leaf product: a container ----
final class Container {
    private final String name;               // required
    private final String image;              // required
    private final List<Integer> ports;       // optional
    private final Map<String, String> env;   // optional

    private Container(Builder b) {
        this.name  = Objects.requireNonNull(b.name, "container name required");
        this.image = Objects.requireNonNull(b.image, "container image required");
        this.ports = List.copyOf(b.ports);
        this.env   = Map.copyOf(b.env);
    }
    public static Builder builder() { return new Builder(); }
    @Override public String toString() {
        return "Container{name=" + name + ", image=" + image + ", ports=" + ports + ", env=" + env + "}";
    }
    static final class Builder {
        private String name, image;
        private final List<Integer> ports = new ArrayList<>();
        private final Map<String, String> env = new HashMap<>();
        public Builder name(String n)  { this.name = n; return this; }
        public Builder image(String i) { this.image = i; return this; }
        public Builder port(int p)     { this.ports.add(p); return this; }
        public Builder env(String k, String v) { this.env.put(k, v); return this; }
        public Container build()       { return new Container(this); }
    }
}

// ---- Root product: a Pod that composes containers ----
final class Pod {
    private final String name;                 // required
    private final Map<String, String> labels;  // optional
    private final List<Container> containers;   // >=1 required

    private Pod(Builder b) {
        this.name = Objects.requireNonNull(b.name, "pod name required");
        if (b.containers.isEmpty()) throw new IllegalStateException("a Pod needs >=1 container");
        this.labels     = Map.copyOf(b.labels);
        this.containers = List.copyOf(b.containers);
    }
    public static Builder builder() { return new Builder(); }
    @Override public String toString() {
        return "Pod{name=" + name + ", labels=" + labels + ", containers=" + containers + "}";
    }
    static final class Builder {
        private String name;
        private final Map<String, String> labels = new HashMap<>();
        private final List<Container> containers = new ArrayList<>();
        public Builder name(String n)  { this.name = n; return this; }
        public Builder label(String k, String v) { this.labels.put(k, v); return this; }
        // Accept a built Container...
        public Builder container(Container c) { this.containers.add(c); return this; }
        // ...or a Container.Builder, building it here (nested-builder convenience):
        public Builder container(Container.Builder cb) { this.containers.add(cb.build()); return this; }
        public Pod build() { return new Pod(this); }
    }
}
```

```java
public class PodSpecDemo {
    public static void main(String[] args) {
        Pod pod = Pod.builder()
                .name("web")
                .label("app", "storefront")
                .label("tier", "frontend")
                .container(Container.builder()
                        .name("nginx")
                        .image("nginx:1.27")
                        .port(80).port(443)
                        .env("WORKERS", "4"))              // nested builder, built by the Pod builder
                .container(Container.builder()
                        .name("sidecar")
                        .image("envoyproxy/envoy:v1.30")
                        .port(9901))
                .build();

        System.out.println(pod);
        // Pod{name=web, labels={app=storefront, tier=frontend}, containers=[
        //   Container{name=nginx, image=nginx:1.27, ports=[80, 443], env={WORKERS=4}},
        //   Container{name=sidecar, image=envoyproxy/envoy:v1.30, ports=[9901], env={}}]}
    }
}
```

**Code explanation.** This is Builder applied to a **deeply nested, tree-shaped object** — the shape you actually hit with Kubernetes manifests, Protobuf messages, or any rich config — and it demonstrates two techniques beyond the flat builders above. First, **nested/composed builders**: `Container` has its own builder, and `Pod.Builder` accepts either an already-built `Container` *or* a `Container.Builder` (calling `.build()` internally). Accepting the sub-builder is the ergonomic trick that keeps the fluent tree readable — you never break the chain to declare intermediate variables — and it mirrors how the Fabric8 Kubernetes client and Protobuf generated code let you assemble `spec.containers[].ports[]` inline. Second, **structural invariants enforced at `build()`**: the `Pod` constructor rejects a spec with zero containers (`IllegalStateException`) and both products deep-copy their collections with `List.copyOf`/`Map.copyOf`, so the finished `Pod` is a fully immutable snapshot — no half-valid manifest and no post-build mutation of ports or labels. Each level validates its own required fields (`name`, `image`) at its own gate, so errors point precisely at the offending node. As always the builders are single-thread mutable scaffolding while the resulting `Pod`/`Container` graph is immutable; in an interview this scenario is a strong answer to "how does Builder scale to complex objects?" — the answer is *recursively*, one builder per level, composing sub-builders.

</details>

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- A constructor would have **more than ~4 parameters**, especially with **several optional** ones.
- You want to create an **immutable** object with many fields (Builder is the standard Java way to do this cleanly).
- You need **atomic validation** of the whole object at construction time (fail fast, no partially-built objects).
- Different callers set **different subsets** of parameters, and you want readable, named, order-independent construction.
- (GoF form) The **same construction process** must produce **different representations** (an HTML vs. text report from one sequence) — use a Director + interchangeable builders.

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- **Simple objects** with 2–3 fields — a constructor, a static factory, or a `record` is simpler; Builder is pure boilerplate here.
- **Mutable objects** meant to change after creation — plain setters are more appropriate than an immutable-product builder.
- **Performance-critical hot loops** — a builder allocation per product adds GC pressure; consider object reuse or a mutable design.
- **All fields required with a natural order** — a constructor (or `record`) communicates intent without a builder.
- **A framework/DI container already constructs the object** from configuration — you may not need a hand-rolled builder.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- **Readability** — named, self-documenting method calls instead of positional arguments.
- **Immutability** — `final` fields + private constructor + no setters ⇒ a thread-safe, immutable product.
- **Flexibility** — add optional parameters without breaking existing call sites (new fluent method, unchanged callers).
- **Atomic validation** — the `build()` gate enforces invariants in one place, preventing zombie objects.
- **Optional-parameter friendly** — set only what you need; defaults handle the rest.

**Cons**

- **Boilerplate (the headline con)** — duplicated fields in product and builder, one method per field, a nested class (mitigated by Lombok `@Builder`).
- **Extra allocation** — the builder object is created before the product; adds GC pressure in ultra-low-latency code.
- **Overkill for simple objects** — indirection with no payoff when there are few fields.
- **The builder is not thread-safe** — a shared builder mutated by multiple threads yields undefined state (confine per thread).

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

Builder is most confused with the **Factory** patterns (all creational) and with the plain **Parameter Object**. The distinctions are about *how many objects, how much assembly*.

| Pattern | What it does | Mechanism | Key tell |
|---|---|---|---|
| **Builder** | Assembles *one* complex object step by step | Fluent accumulation + `build()` | "Many optional params / staged construction / immutable" |
| **Factory Method** | Creates *one* product, type chosen by subclass | Overridable method (inheritance) | "Subclass decides which one product" |
| **Abstract Factory** | Creates a *family* of related products | Object with several creation methods | "Matched set of consistent products" |
| **Prototype** | Creates by *cloning* a configured instance | `clone()` | "Copy an existing object; cloning cheaper than building" |
| **Parameter Object** | Bundles arguments into one class | A simple data holder | "Group of args always passed together" |

**Builder vs. Factory Method / Abstract Factory:** the factories decide *which* object (or family) to create and return it in essentially one step; Builder is about *how* to assemble *one* object over several steps. "Factory = what to create; Builder = how to construct it." They compose — a factory method can decide *which builder* to return.

**Builder vs. Prototype:** Prototype creates a new object by copying an existing configured one; Builder constructs from scratch via an explicit process. If you often need slight variations of a costly-to-build object, Prototype (clone + tweak) can beat rebuilding via a Builder each time.

**Builder vs. Parameter Object:** a Parameter Object just *wraps* arguments (often mutable, no validation/optionality machinery); a Builder *manages the construction process* and yields an immutable, validated product. Use a Parameter Object when a fixed group of args always travels together; use a Builder when you need immutability, optionality, or staged/validated construction.

</details>

## 📊 Comparison Table of Variants

<details>
<summary>📖 Click to expand</summary>

| Axis | V0a Telescoping | V0b Setters | V1 GoF Director | V2 Fluent | V3 Validated | V4 Step Builder |
|---|---|---|---|---|---|---|
| **Immutable product** | ✅ | ❌ | depends | ✅ | ✅ | ✅ |
| **Readable call site** | ❌ | ✅ | n/a | ✅ | ✅ | ✅ |
| **Optional params** | ❌ (overloads) | ✅ | n/a | ✅ | ✅ | ✅ |
| **Runtime validation** | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| **Compile-time required fields** | partial | ❌ | ❌ | ❌ | ❌ | ✅ |
| **Multiple representations** | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| **Boilerplate** | Med | Low | High | High | High | Very high |
| **Best for** | tiny fixed objects | mutable beans | several representations | most prod objects | API/critical models | must-not-fail construction |

**Best choice:** the **fluent builder (V2)** for most immutable objects, upgraded to **validated (V3)** for anything with invariants, and **Step Builder (V4)** when compile-time required-field safety is worth the boilerplate. Reach for the **GoF Director (V1)** only when one process must yield multiple representations.

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

### Pitfall 1: Public product constructor bypasses the builder

**What goes wrong:** If the product's constructor is public, callers can skip the builder — and its validation — creating objects in states the builder would have rejected.

<details>
<summary>💻 Broken</summary>

```java
public class User {
    public User(String name) { this.name = name; }   // public -> builder & validation bypassed
}
// new User(null);  // no validation ran
```
</details>

<details>
<summary>💻 Fix — private constructor, builder is the only path</summary>

```java
public class User {
    private User(Builder b) { /* validate here */ }   // only Builder can call it
}
```
</details>

### Pitfall 2: Leaking mutable collections into the "immutable" product

**What goes wrong:** Assigning the builder's `List` directly to the product lets the caller mutate the product afterward through a retained reference.

<details>
<summary>💻 Broken</summary>

```java
private User(Builder b) { this.roles = b.roles; }   // shared reference -> not immutable
```
</details>

<details>
<summary>💻 Fix — defensive copy + unmodifiable</summary>

```java
private User(Builder b) { this.roles = List.copyOf(b.roles); }   // snapshot, unmodifiable
```
</details>

### Pitfall 3: No validation in build() → zombie objects

**What goes wrong:** `build()` returns an object with null required fields or invalid values, and the failure surfaces far away as an NPE deep in business logic — hard to trace.

<details>
<summary>💻 Broken</summary>

```java
public User build() { return new User(this); }   // no checks; firstName may be null
```
</details>

<details>
<summary>💻 Fix — validate at the gate</summary>

```java
public User build() {
    Objects.requireNonNull(firstName, "firstName is required");
    if (age < 0) throw new IllegalArgumentException("age < 0");
    return new User(this);
}
```
</details>

### Pitfall 4: Sharing/reusing a builder across threads or products

**What goes wrong:** A `Builder` is mutable and **not thread-safe**; sharing one across threads yields interleaved, undefined state. Reusing one builder to make several products can also leak state (a mutable collection built up across `build()` calls).

<details>
<summary>💻 Fix</summary>

```java
// Confine each builder to one thread; create a fresh builder per product:
User a = User.builder().firstName("A").build();
User b = User.builder().firstName("B").build();   // independent builders, no shared state
```
</details>

</details>

---

## 🎓 Interview Tips

**What interviewers commonly ask:**

- "Why use Builder over a constructor / setters?" (Telescoping + immutability + validation.)
- "Code a Builder for X." (User, HttpRequest, SQL query — show private constructor, static nested builder, fluent `return this`, `build()`.)
- "How does it guarantee immutability?" (Private constructor, `final` fields, no setters, defensive copies.)
- "What's a Step Builder?" (Compile-time required-field enforcement via staged interfaces.)
- "GoF Builder vs. the fluent builder everyone writes?" (Director + representations vs. Effective-Java Item 2.)

**What you should proactively mention even if not asked:**

- The **two intents**: GoF (Director + multiple representations) vs. modern *Effective Java* fluent builder — knowing both signals depth.
- **Immutability + atomic validation** are the real wins; call out defensive copies for collections.
- In real projects you'd use **Lombok `@Builder`** to kill the boilerplate, or a **`record`** when there are no optional fields / no validation needs.
- The **builder is not thread-safe** (confine per thread); the **product is immutable** and safely shared.
- **Step Builder** for compile-time required fields and **recursive generics (CRTP)** for builder inheritance are the advanced variants worth naming.
- The **allocation/GC** cost in ultra-low-latency systems, and mitigation (reuse/mutable design).

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Factory Method** — often *returns* a builder, or a builder's `build()` may delegate to a factory; "factory = what, builder = how."
- **Abstract Factory** — can return family-specific builders when family products need staged construction.
- **Prototype** — an alternative when you want slight variations of a costly object (clone + tweak) instead of rebuilding.
- **Composite** — builders are handy for assembling composite trees fluently.
- **Fluent Interface** — the API style Builder popularized (`return this` chaining); not a GoF pattern itself.
- **Lombok `@Builder` / Java `record`** — code-generation and language features that reduce or replace hand-written builders.

</details>

## 📚 Library/Framework Implementation

**1. `java.lang.StringBuilder` / `StringBuffer`.** The most famous JDK builder: instead of creating many immutable `String` objects via concatenation, you `append(...)` step by step and call `toString()` (the `build()` analog) once. `StringBuilder` is the fast, single-threaded form; `StringBuffer` is its synchronized sibling.

<details>
<summary>💻 Click to expand — StringBuilder</summary>

```java
String result = new StringBuilder()
        .append("Hello").append(", ").append("World").append("!")
        .toString();          // toString() == build()
// One mutable accumulator -> one finished immutable String, no intermediate String garbage.
```
</details>

**2. `java.net.http.HttpClient` / `HttpRequest` (Java 11+).** The modern HTTP API is builder-based end to end: you build an immutable client (timeouts, redirect policy, proxy) and immutable requests (URI, headers, method, body), each via `newBuilder()...build()`.

<details>
<summary>💻 Click to expand — HttpClient / HttpRequest builders</summary>

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.URI;
import java.time.Duration;

HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.example.com/data"))
        .header("Content-Type", "application/json")
        .timeout(Duration.ofSeconds(5))
        .GET()
        .build();
```
</details>

**3. Lombok `@Builder`, Protobuf builders, and Guava `ImmutableList.Builder`.** Lombok's `@Builder` annotation generates the entire fluent builder at compile time, eliminating the boilerplate. Protocol Buffers generate a `Message.Builder` for every message type (mutable builder → immutable message). Guava's immutable collections expose `Builder`s (`ImmutableList.builder().add(...).build()`) to assemble immutable collections incrementally.

<details>
<summary>💻 Click to expand — Lombok @Builder and Guava ImmutableList.Builder</summary>

```java
// Lombok generates the builder; you write only the fields.
@lombok.Builder
public class User { String firstName; String lastName; int age; }
// Usage: User.builder().firstName("Jane").age(30).build();

// Guava: assemble an immutable list incrementally, then freeze it.
com.google.common.collect.ImmutableList<String> roles =
        com.google.common.collect.ImmutableList.<String>builder()
                .add("ADMIN").add("USER").build();
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1: [Conceptual] What is the Builder pattern and when should you use it?</strong></summary>

Builder is a **creational** GoF pattern that separates the construction of a complex object from its representation, letting you assemble it step by step. In modern Java it's mainly used to construct an **immutable object with many parameters, several optional**, without telescoping constructors or mutable setters.

Use it when a constructor would have more than ~4 parameters (especially optional ones), when you want an immutable, validated object, or when different callers set different subsets of fields. The client chains named methods on a builder and calls `build()`, which validates and produces the finished product. It trades some boilerplate for readability, immutability, and atomic validation.
</details>

<details>
<summary><strong>Q2: [Conceptual] What's the difference between the GoF Builder and the fluent builder everyone writes?</strong></summary>

The **GoF Builder** centers on a **Director** that runs a *fixed construction sequence*, plus interchangeable **Builder** implementations that produce *different representations* from that sequence (e.g., the same "build a report" process yielding HTML or plain text). Its intent is decoupling the construction *process* from the *representation*.

The **fluent builder** (*Effective Java* Item 2) drops the Director entirely and focuses on one goal: build a single **immutable** object with optional parameters via a chained API. It's what 95% of Java code means by "Builder." A strong answer names both: GoF for multiple representations, fluent for immutable-object-with-many-params.
</details>

<details>
<summary><strong>Q3: [Conceptual] How does the Builder pattern achieve immutability?</strong></summary>

Four things together: (1) the product's fields are `final`; (2) its constructor is `private`, so only the builder can call it; (3) the product exposes **no setters**; and (4) mutable inputs (collections, arrays, mutable objects) are **defensively copied** in the constructor so no external reference can mutate the product afterward.

<details>
<summary>💻 Code</summary>

```java
private final List<String> roles;
private User(Builder b) { this.roles = List.copyOf(b.roles); }  // final + defensive copy
// no setRoles(...) exists -> once built, the User can't change.
```
</details>

The builder is a mutable staging area; the product is frozen the moment `build()` returns.
</details>

<details>
<summary><strong>Q4: [Conceptual] How does Builder relate to the Single Responsibility Principle?</strong></summary>

It separates **construction logic** (how to assemble and validate the object) from **business logic** (what the object does). The product class no longer needs multiple constructor permutations or self-validation scattered across them; that responsibility moves to the builder and its `build()` gate.

This keeps the product focused on its domain behavior while the builder owns assembly concerns — optional defaults, validation, defensive copying, staged ordering. It also means changes to *how* an object is constructed (a new optional field, a new invariant) don't ripple into the product's core responsibilities. The result is two cohesive classes instead of one class doing both jobs.
</details>

<details>
<summary><strong>Q5: [Implementation] Why is a static nested class typically used for the Builder?</strong></summary>

Three reasons: (1) a nested class has access to the outer product's **`private` constructor**, so the builder can construct it while nobody else can; (2) being **`static`**, it doesn't need an instance of the product to exist (you're building the product, so there isn't one yet); and (3) it keeps the builder **tightly coupled and co-located** with the product it builds, aiding discoverability and encapsulation.

<details>
<summary>💻 Code</summary>

```java
public class User {
    private User(Builder b) { /* ... */ }          // private ctor
    public static class Builder {                   // static nested -> sees private ctor
        public User build() { return new User(this); }
    }
}
```
</details>

A non-static inner class would (needlessly) require an enclosing `User` instance, which defeats the purpose.
</details>

<details>
<summary><strong>Q6: [Implementation] How do you implement a fluent interface, and what's the mechanism?</strong></summary>

Each builder setter returns `this` (the builder), so calls can be chained in one expression. The mechanism is simply returning the receiver, enabling `builder.a(..).b(..).c(..)`.

<details>
<summary>💻 Code</summary>

```java
public Builder age(int age)    { this.age = age;   return this; }   // return this -> chain
public Builder phone(String p) { this.phone = p;   return this; }
// User.builder("J","D").age(30).phone("555").build();
```
</details>

For inheritance, returning `this` from a base setter drops the type to the base builder, breaking the chain for subclass methods — solved with **recursive generics (CRTP)**: `Builder<T extends Builder<T>>` and a `self()` cast so base setters return the concrete subclass type.
</details>

<details>
<summary><strong>Q7: [Implementation] How do you enforce required fields — at runtime and at compile time?</strong></summary>

**Runtime:** put required fields in the builder's constructor (so you can't even start without them) and/or validate in `build()` with `Objects.requireNonNull` and range checks — an invalid object never gets constructed.

**Compile time:** use a **Step Builder** — a chain of single-method interfaces where each step returns the next, so the compiler forces required fields to be set in order before `build()` is reachable.

<details>
<summary>💻 Step Builder skeleton</summary>

```java
interface NameStep { AgeStep name(String n); }
interface AgeStep  { BuildStep age(int a); }
interface BuildStep{ User build(); }
// User.builder().name("J").age(30).build();  // omitting name() won't compile
```
</details>
</details>

<details>
<summary><strong>Q8: [Implementation] Coding challenge — implement a validated, thread-considered Config builder.</strong></summary>

<details>
<summary>💻 Complete solution</summary>

```java
import java.util.Objects;

public final class AppConfig {
    private final String apiKey;      // required
    private final int timeoutMs;      // optional, must be > 0
    private final boolean retry;

    private AppConfig(Builder b) {
        this.apiKey    = Objects.requireNonNull(b.apiKey, "apiKey is required");
        if (b.timeoutMs <= 0) throw new IllegalArgumentException("timeoutMs must be > 0");
        this.timeoutMs = b.timeoutMs;
        this.retry     = b.retry;
    }
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String apiKey;
        private int timeoutMs = 5000;     // sensible default
        private boolean retry = true;
        public Builder apiKey(String k)  { this.apiKey = k; return this; }
        public Builder timeoutMs(int t)  { this.timeoutMs = t; return this; }
        public Builder retry(boolean r)  { this.retry = r; return this; }
        public AppConfig build()         { return new AppConfig(this); }  // validation gate
    }
}
// Builder is NOT thread-safe: create one per thread. AppConfig is immutable -> shareable.
// AppConfig c = AppConfig.builder().apiKey("k").timeoutMs(3000).build();
```
</details>
</details>

<details>
<summary><strong>Q9: [Implementation] Coding challenge — implement a Step Builder for an Order (compile-time required fields).</strong></summary>

<details>
<summary>💻 Complete solution</summary>

```java
public class Order {
    private final String customerId, item;
    private final int quantity;
    private Order(B b) { this.customerId = b.customerId; this.item = b.item; this.quantity = b.quantity; }

    public interface CustomerStep { ItemStep customer(String id); }
    public interface ItemStep     { QtyStep item(String name); }
    public interface QtyStep      { Order quantity(int q); }   // terminal builds the Order

    public static CustomerStep builder() { return new B(); }

    private static class B implements CustomerStep, ItemStep, QtyStep {
        private String customerId, item; private int quantity;
        public ItemStep customer(String id) { this.customerId = id; return this; }
        public QtyStep item(String name)    { this.item = name;     return this; }
        public Order quantity(int q)        { this.quantity = q;    return new Order(this); }
    }
}
// Order o = Order.builder().customer("C1").item("Book").quantity(2);
// Order bad = Order.builder().item("Book");  // COMPILE ERROR: item() not on CustomerStep
```
</details>
</details>

<details>
<summary><strong>Q10: [Breaking] What happens if you forget validation in build()?</strong></summary>

You create **zombie objects** — objects that are technically constructed but semantically invalid (a `User` with a null `firstName`, an `Order` with quantity 0). The bug doesn't fail at construction; it surfaces **later and elsewhere** as an NPE or logic error deep in business code, far from the actual mistake, making it expensive to diagnose.

<details>
<summary>💻 Fix — fail fast at the gate</summary>

```java
public User build() {
    Objects.requireNonNull(firstName, "firstName is required");
    if (age < 0) throw new IllegalArgumentException("age < 0: " + age);
    return new User(this);   // an invalid User can never exist
}
```
</details>

Centralizing validation in `build()` converts scattered, delayed failures into one immediate, well-located one.
</details>

<details>
<summary><strong>Q11: [Breaking] Is the Builder thread-safe? What breaks if it isn't handled?</strong></summary>

The **product** is typically thread-safe because it's immutable. The **builder itself is not** — it's a mutable accumulator. If multiple threads share one builder and call setters/`build()` concurrently, you get interleaved writes and an object with unpredictable state (lost updates, torn reads of the tags list, etc.).

<details>
<summary>💻 Fix — confine a builder to one thread; fresh builder per product</summary>

```java
// Each thread/product uses its own builder instance:
Runnable task = () -> { User u = User.builder("A","B").age(30).build(); use(u); };
```
</details>

Making `build()` `synchronized` doesn't fix the *setting* phase; the real fix is thread confinement. Reusing one builder to emit multiple products can also leak accumulated state (e.g., a growing tags list) — create a new builder each time.
</details>

<details>
<summary><strong>Q12: [Breaking] How can the "immutable" product still be mutated, and how do you prevent it?</strong></summary>

Through **reference leaks** of mutable fields. If the constructor assigns a caller-provided (or builder-held) `List`/array/mutable object directly, the holder of that reference can mutate the product's internals after construction.

<details>
<summary>💻 Broken</summary>

```java
private Order(Builder b) { this.items = b.items; }   // caller can later mutate b.items
```
</details>

<details>
<summary>💻 Fix — defensive copy in, unmodifiable out</summary>

```java
private Order(Builder b) { this.items = List.copyOf(b.items); }  // snapshot; unmodifiable
public List<Item> items() { return items; }                       // already unmodifiable
```
</details>

Do the same for arrays (`clone()`), dates (copy), and any mutable object you don't own — true immutability requires no live references escape or enter.
</details>

<details>
<summary><strong>Q13: [Trade-off] Builder vs. Factory — when is each preferred?</strong></summary>

A **Factory** (Method or Abstract) decides *which* object or family to create and returns it in essentially one step; it hides *which class*. A **Builder** constructs *one* object over *several* steps; it manages *how* the object is assembled and configured. "Factory = what to create; Builder = how to construct it."

Prefer a Factory when the challenge is **selecting a type/family**; prefer a Builder when the challenge is **configuring one object** with many optional fields or staged assembly. They combine: a factory method can return the appropriate builder, separating type selection (factory) from assembly (builder). If construction is trivial, neither is needed — just use a constructor.
</details>

<details>
<summary><strong>Q14: [Trade-off] Builder vs. Parameter Object — when is one better?</strong></summary>

A **Parameter Object** simply groups related arguments into one class so you pass one thing instead of many; it's usually a plain (often mutable) data holder with no construction machinery. A **Builder** actively *manages construction* — optionality, defaults, validation, immutability, staged order.

Use a **Parameter Object** when a fixed group of arguments always travels together and needs no validation or optionality (e.g., a `Coordinate(x, y)` passed around). Use a **Builder** when the object is complex, must be immutable, has many optional fields, or needs a controlled/validated construction sequence. A Parameter Object can even *be built by* a Builder if it's complex enough.
</details>

<details>
<summary><strong>Q15: [Trade-off] When should you use a Java `record` instead of a Builder?</strong></summary>

`record`s (Java 16+) are ideal when you have a **fixed set of required components, no optional fields, and simple/absent validation** — they give you immutability, a canonical constructor, `equals`/`hashCode`/`toString` for free, with almost no code. They shine as immutable DTOs and value objects.

Reach for a **Builder** when there are **many optional fields** (records have no optional-parameter mechanism — you'd need many overloaded constructors, i.e., telescoping), when construction needs **complex validation or defaults**, or when you want a **fluent, order-independent** API. The two can combine: a record's compact constructor can validate, and you can hand-write (or Lombok-generate) a builder that produces a record. Rule of thumb: few required fields → record; many optional fields → builder.
</details>

<details>
<summary><strong>Q16: [Advanced] How do you provide "default" or "prototype" configurations with a Builder?</strong></summary>

Expose a static method returning a **pre-configured builder** (a baseline callers can override), or copy an existing product's state into a new builder ("toBuilder"). This lets clients start from a known-good template and change only what differs.

<details>
<summary>💻 Code</summary>

```java
public static Builder adminDefaults() {
    return new Builder("Admin", "System").age(99).addRole("SUPERUSER");
}
// Override just what you need:
User u = User.adminDefaults().age(40).build();

// toBuilder-style copy for "clone and tweak":
public Builder toBuilder() { return new Builder(firstName, lastName).age(age); }
```
</details>

This overlaps with Prototype (clone + modify); a `toBuilder()` is essentially a builder-flavored prototype.
</details>

<details>
<summary><strong>Q17: [Advanced] How do you build a Builder across a polymorphic class hierarchy?</strong></summary>

Use **recursive generics** (the Curiously Recurring Template Pattern). The base builder is `Builder<T extends Builder<T>>` with a `self()` method returning `(T) this`; base setters `return self()` so they keep the *subclass* builder type, preserving the fluent chain when you mix base and subclass setters.

<details>
<summary>💻 Code</summary>

```java
abstract static class Builder<T extends Builder<T>> {
    int wheels;
    @SuppressWarnings("unchecked") T self() { return (T) this; }
    public T wheels(int w) { this.wheels = w; return self(); }   // returns subclass builder
    abstract Vehicle build();
}
class CarBuilder extends Builder<CarBuilder> {
    int doors;
    public CarBuilder doors(int d) { this.doors = d; return self(); }
    Car build() { return new Car(this); }
}
// new CarBuilder().wheels(4).doors(2).build();  // wheels() returns CarBuilder, chain intact
```
</details>

Without CRTP, `wheels(4)` would return the base `Builder` and you couldn't then call `doors(2)`.
</details>

<details>
<summary><strong>Q18: [Advanced] What's the impact of Builder on GC / memory in low-latency systems?</strong></summary>

Every product creation allocates **two** objects — the builder and the product — plus any intermediate collections. In ultra-low-latency systems (HFT, tight hot loops), this "allocation noise" increases the frequency of young-generation GC pauses, which matters when you're chasing microseconds.

Mitigations: **reuse a mutable builder** (reset and rebuild) confined to one thread; use **object pooling** for builders; or abandon the builder in the hottest paths for a **mutable, reused object** (a flyweight-style scratch object) — sacrificing immutability/thread-safety for zero allocation. Another option is to skip the builder and use a plain constructor or a `record` where the field set is fixed. The Staff point: the builder's ergonomics are usually worth it, but on measured hot paths, allocation cost can justify a less elegant, allocation-free design.
</details>

<details>
<summary><strong>Q19: [Advanced] How does Builder integrate with JSON/Protobuf deserialization in distributed systems?</strong></summary>

A builder is a natural **accumulator** for incremental deserialization. As a parser reads a stream/message field by field, it populates a builder; only when the full message is received does it call `build()`, producing a validated, immutable domain object. This prevents **partially-initialized objects** from entering the heap and being processed by business logic — important for both stability and security (no "half-baked" objects).

Protobuf embodies this: generated code gives each message a mutable `Builder` that you populate during parsing, then `build()` into an immutable `Message`. Jackson can use `@JsonPOJOBuilder` to deserialize directly through a builder, so validation and defaults run as part of construction. The Staff angle: builders give you a clean seam to enforce invariants at the *edge* (deserialization boundary), so invalid external data is rejected before it becomes a domain object.
</details>

<details>
<summary><strong>Q20: [Trade-off] Would you hand-write builders or use Lombok `@Builder` in production, and what are the risks?</strong></summary>

In production Java, most teams use **Lombok `@Builder`** (or `@SuperBuilder` for inheritance) to eliminate the boilerplate — you declare fields and get the fluent builder generated at compile time. It's a big readability/maintenance win.

Risks to mention: Lombok is a **compile-time annotation processor that manipulates the AST**, so it requires IDE plugins, can break on JDK upgrades, and hides generated code from reviewers; validation must be added via `@Builder.Default`, a custom `build()`, or bean validation, since the generated builder doesn't validate by default; and defensive copying of collections isn't automatic. For **public API models** where you want precise control (validation, Step Builder, documented contracts), a hand-written builder can be preferable. The pragmatic answer: Lombok for internal DTOs/configs, hand-written (or `record` + compact constructor) where you need explicit control or can't take the Lombok dependency.
</details>

### 🏛️ Staff / Principal Engineer Deep-Dive

<details>
<summary><strong>SP1: How do you decide between a Builder, a record, and a constructor across a large codebase, and how do you keep it consistent?</strong></summary>

Establish a decision rule and encode it in conventions/linting. Use a **constructor** for 1–3 required fields with no optionality; a **`record`** for immutable value objects/DTOs with a fixed component set and simple validation (compact constructor); a **Builder** when there are many optional fields, non-trivial validation/defaults, staged construction, or a need for a fluent, evolvable API. The consistency risk at scale is a mix of styles that makes the codebase unpredictable, so document the rule, prefer `record`s as the default for data carriers (less code, free equality), and reserve builders for genuinely complex construction. Watch for the **records + optionality** anti-pattern (a record with five overloaded constructors is telescoping in disguise — switch to a builder). Also standardize on one builder flavor (Lombok vs. hand-written) per module and one approach to validation (bean validation vs. explicit `build()` checks) so reviewers and tools can rely on it.
</details>

<details>
<summary><strong>SP2: When is a Step Builder worth its heavy boilerplate, and what are its limits?</strong></summary>

A Step Builder buys **compile-time guarantees**: required fields must be set, in a defined order, before `build()` is reachable — turning a class of runtime bugs into compile errors. It's worth the boilerplate for **public APIs and SDKs** (where misuse by external callers is costly and you want the compiler to guide them), **safety-critical domain objects** (financial orders, medical records), and **complex construction where ordering matters**. Its limits: the boilerplate scales with the number of required fields (one interface per step), it imposes a **rigid order** that can feel unnatural and is awkward when required fields are genuinely order-independent, and it interacts poorly with optional fields (usually you funnel into a final "optionals" step). For most internal objects the runtime-validated fluent builder is the better cost/benefit; Step Builders are a targeted tool for high-stakes construction, not a default. Generating them (rather than hand-writing) mitigates the boilerplate.
</details>

<details>
<summary><strong>SP3: How would you design builders for backward/forward-compatible evolution of a widely-used object?</strong></summary>

The builder is your compatibility shield. Because callers set fields by **name**, you can **add new optional fields** with sensible defaults without breaking any existing call site — the number-one reason builders beat constructors for evolving APIs (adding a constructor parameter is a breaking change; adding a builder method isn't). Rules for safe evolution: never remove or rename existing builder methods (deprecate instead); give every new field a **backward-compatible default** so old callers behave as before; validate new invariants leniently at first if old data can't satisfy them; and add a **`toBuilder()`** so consumers can derive modified copies as the type grows. For serialized objects, pair the builder with schema-evolution rules (Protobuf/Avro) so wire compatibility matches API compatibility. Avoid **required-field creep** — promoting an optional field to required *is* a breaking change; if you must, do it via a new Step or a separate factory. The Staff principle: builders let a type grow additively for years, which is exactly why library authors favor them.
</details>

<details>
<summary><strong>SP4: Discuss immutability, safe publication, and thread-safety across the builder→product boundary in the JMM.</strong></summary>

The builder is a **thread-confined mutable object**; the product is **immutable and safely shareable** — but only if immutability is done correctly per the Java Memory Model. Make product fields `final`: the JMM's **final-field freeze guarantee (JLS 17.5)** ensures that once the constructor completes, other threads that see the object via a **safe publication** observe correctly-initialized final fields *without* extra synchronization. That's why a properly-built immutable product needs no locks to share. Caveats: the guarantee applies to `final` fields and objects they *reachably* reference at construction end, so **defensive copies** must happen inside the constructor (a `final` field pointing at a still-mutable list isn't truly immutable); and the object must be **safely published** (via a `final`/`volatile` field, a concurrent collection, or `static` initializer) — leaking `this` from the constructor or publishing through a data race can expose a partially-constructed object even with `final` fields. The builder side needs no JMM guarantees because it's confined to one thread; the discipline is entirely about how the product is frozen and published.
</details>

<details>
<summary><strong>SP5: When is reaching for a Builder the wrong call, and what deeper design smell can it hide?</strong></summary>

It's wrong when it's **ceremony without payoff** — a builder for a 2-field object, or for something that should just be a `record`. More subtly, a builder with a **huge number of fields** can *mask* a design smell: the object is doing too much (a God object), or unrelated concerns have been merged into one type. The builder makes constructing that bloated object *tolerable*, which removes the pain that would otherwise push you to **decompose** it into smaller, cohesive objects. Another masked smell is **primitive obsession** — a builder taking ten `String`/`int` parameters often wants domain types (an `EmailAddress`, a `Money`) that carry their own validation, shrinking the builder. And a builder used to allow **optional-everything** construction can hide missing invariants (the object has no coherent required core). The Staff move is diagnostic: if a builder has grown past ~10–15 fields, ask whether the *object* should be split, whether groups of fields should become value objects, and whether some "optional" fields are actually required — fix the model rather than making an unwieldy object easier to build. A builder should smooth legitimate construction complexity, not paper over an incoherent type.
</details>

---

## ⚡ Quick Revision

**One-liner:** Builder separates construction of a complex object from its representation — in modern Java, a fluent static-inner-class builder assembles an immutable, validated object step by step, replacing telescoping constructors and mutable setters.

**The whole pattern in a paragraph:** When a class has many parameters, several optional, telescoping constructors are unreadable and fragile and setters destroy immutability and allow inconsistent state. Builder gives the product a `private` constructor and a companion Builder (usually a `static` nested class) that holds mutable fields, exposes a fluent method per field (`return this`), and finishes with `build()` — a single **validation gate** that enforces invariants (`Objects.requireNonNull`, range/business checks) and defensively copies mutable inputs, then calls the private constructor. `final` fields + no setters + defensive copies ⇒ an **immutable, safely publishable** product (JMM final-field guarantee); the builder is a **thread-confined** mutable staging area (not thread-safe). The classic **GoF form** adds a **Director** that runs a fixed sequence while interchangeable builders produce **different representations**; the common **fluent form** (Effective Java Item 2) drops the Director. Advanced variants: the **Step Builder** enforces required fields *at compile time* via staged interfaces (a compile-time state machine), and **recursive generics (CRTP)** keep the fluent chain typed across builder inheritance. The headline trade-off is **boilerplate** (mitigated by Lombok `@Builder` or `record`s) and an **extra allocation** per product (matters in low-latency hot loops). Builders also shine as **accumulators** in JSON/Protobuf deserialization and make objects **evolvable** (add optional fields without breaking callers). JDK/library examples: `StringBuilder`, `HttpClient`/`HttpRequest`, Protobuf builders, Guava `ImmutableList.Builder`.

**Top 5 interview answers to memorize:**

1. **"What / why?"** → Build a complex immutable object step by step via a fluent API; solves telescoping constructors and the mutability of setters, with atomic validation in `build()`.
2. **"Builder vs. Factory?"** → Factory decides *what* object/family to create in one step; Builder controls *how* one object is assembled over several steps. They compose (factory returns a builder).
3. **"How does it ensure immutability?"** → Private constructor + `final` fields + no setters + defensive copies of mutable inputs; builder is the only construction path.
4. **"Why a static inner class?"** → It can access the product's private constructor, needs no product instance (it's `static`), and stays co-located with the product.
5. **"What's a Step Builder?"** → Staged single-method interfaces where each returns the next step, so the compiler forces required fields to be set in order before `build()` — compile-time required-field safety.

**Trigger words** (if the interviewer says these, think Builder): *"too many constructor parameters," "optional fields / mostly optional," "telescoping constructor," "immutable object," "fluent API / method chaining," "step-by-step / staged construction," "configure then build," "different representations from one process" (GoF), "required vs optional at compile time" (Step Builder).*



# Prototype Pattern ⭐⭐ (Difficulty: 2/5 — "call clone()" is trivial, but shallow-vs-deep copy, why `Cloneable` is broken, copy constructors, circular references, and deserialization security are where it gets deep)

> **Category:** Creational Pattern (GoF)
> **Also known as:** Clone

The Prototype pattern **creates new objects by copying an existing, already-configured instance (the "prototype") instead of constructing them from scratch**. When building an object is expensive (heavy I/O, DB/API calls, complex setup) or when you'd otherwise need a swarm of subclasses for slightly different configurations, you keep one fully-initialized specimen and `clone()` it whenever you need another. The client doesn't need to know the object's concrete class — it just asks a prototype to copy itself. It's the idea behind object registries/caches, game-asset spawning, message-envelope duplication, and Spring's `prototype` bean scope — and its whole difficulty lives in one question: *how deep does the copy go?*

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 0: Shallow Copy via Object.clone() / Cloneable (Anti-pattern)](#variant-0-shallow-copy-via-objectclone--cloneable-anti-pattern)
   - [Variant 1: Deep Copy via a Custom Prototype Interface](#variant-1-deep-copy-via-a-custom-prototype-interface)
   - [Variant 2: Copy Constructor (Effective Java's Preferred Approach)](#variant-2-copy-constructor-effective-javas-preferred-approach)
   - [Variant 3: Serialization-Based Deep Copy](#variant-3-serialization-based-deep-copy)
   - [Variant 4: Production Prototype Registry](#variant-4-production-prototype-registry)
5. [🎨 Real-World Example](#-real-world-example)
   - [Scenario 1 — Kubernetes Pod Template Registry](#scenario-1--kubernetes-pod-template-registry)
   - [Scenario 2 — VM / EC2 Image (AMI) Registry](#scenario-2--vm--ec2-image-ami-registry)
   - [Scenario 3 — Workflow / Job Template Registry](#scenario-3--workflow--job-template-registry)
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

> **GoF Definition:** *"Specify the kinds of objects to create using a prototypical instance, and create new objects by copying this prototype."*

The Prototype pattern exists for situations where **constructing an object from scratch is undesirable** — either because it's *expensive* (the constructor does heavy lifting: reads files, queries databases, loads models, sets up complex state) or because it would force an *explosion of subclasses/factories* to represent many slightly-different configurations. Rather than calling `new` and re-doing all that work each time, you build one fully-configured "prototype" and produce further instances by **copying** it.

The key insight is that an object can be responsible for **producing copies of itself** through a `clone()` (or copy-constructor) operation, so the client that needs a new instance doesn't need to know the concrete class or how to build it — it just asks the prototype to duplicate itself. This decouples the client from concrete types (like a factory does) but via *copying a configured specimen* rather than *invoking a constructor*. The entire practical subtlety of the pattern collapses into one distinction that every interview drills: a **shallow copy** duplicates the top-level object but shares references to its nested mutable objects (so the copy and original secretly share state), while a **deep copy** recursively duplicates the whole object graph so the copy is truly independent. Getting this wrong is the pattern's classic, dangerous bug.

---

## 🎯 Problem

You need many instances of an object, but creating them with `new` is either **costly** or **awkward**. Costly: the constructor performs expensive initialization (I/O, network, computation) that you don't want to repeat for every instance. Awkward: the objects differ only slightly in configuration, and modeling each configuration as its own subclass or wiring a bespoke factory for each leads to a **subclass/factory explosion**. On top of that, sometimes the client must create an object **without knowing its concrete class** — it only has a reference through an interface.

The pain points that lead you to Prototype: **high instantiation cost** (repeating heavy setup is wasteful); **complex initial state** that's tedious to reconstruct correctly each time; **subclass explosion** when using factories for many minor variants (`RedCircle`, `BigRedCircle`, …); and the need to **create objects decoupled from their concrete class**, cloning through an interface.

**Concrete example scenarios:**

1. **High-frequency event/message templates.** In an event pipeline processing millions of messages/sec, each message shares a heavy "envelope" (metadata, routing headers, trace context). Rebuilding that envelope per message is wasteful; clone a pre-built template and set only the per-message fields.

2. **Game asset spawning.** A game has many enemy/obstacle types, each with configured stats, meshes, and sounds. Instead of a factory subclass per variant, keep one configured prototype per type in a registry and `clone()` to spawn.

3. **Costly-to-construct configuration objects.** An object whose defaults come from a database or a parsed config file — load it once into a prototype, then clone-and-tweak for each consumer rather than re-reading the source.

4. **Objects created without knowing their class.** Frameworks that must copy user-supplied objects (whose concrete types they don't know) rely on the objects being able to clone themselves through a common interface.

---

## ✅ Solution

The core idea, in plain language: **keep one fully-initialized instance and make new objects by copying it, rather than by constructing from scratch.** Give objects the ability to duplicate themselves through a `clone()` method (or a copy constructor), and — optionally — store pre-configured prototypes in a registry so clients can request "a copy of the X prototype" by key.

**Key structural elements:**

- **Prototype (interface/abstract type):** declares the copy operation — a `clone()` method (or, better in Java, an agreed copy mechanism). Clients depend on this, not on concrete classes.
- **Concrete Prototype:** implements the copy operation, correctly duplicating its own state (crucially, deep-copying mutable fields).
- **Client:** obtains new objects by invoking `clone()` on a prototype, never calling a concrete constructor.
- **Prototype Registry (optional but common in production):** a cache/map of pre-configured prototypes keyed by name/type; `registry.get(key).clone()` yields a fresh, independent instance of a complex configured object.

**The mechanism that makes it work:** *copying a configured object graph instead of running a constructor.* The heavy/complex initialization happens **once** (when the prototype is built); every subsequent instance is a memory-to-memory copy, which is typically far cheaper. Because the copy is produced by the object itself (through an interface), the client stays decoupled from concrete types. The linchpin — and the source of every Prototype bug — is **copy depth**: `Object.clone()` performs a *shallow*, field-by-field bitwise copy, so reference-typed fields (lists, maps, nested objects) are *shared* between original and clone; a correct prototype must **deep-copy** every mutable field so the clone is independent.

The **central trade-off** to name: Java's built-in cloning is genuinely broken and dangerous. `Cloneable` is a **marker interface with no `clone()` method**; the actual `clone()` lives in `Object`, is `protected`, and only shallow-copies; it bypasses constructors (so `final` fields and invariants are problematic); and it throws checked `CloneNotSupportedException`. *Effective Java* (Item 13) advises avoiding `Cloneable` altogether in favor of **copy constructors / copy factories**. So the "best" Prototype in modern Java rarely uses `Object.clone()` — it uses an explicit copy constructor or a well-defined `copy()` method, and for arbitrary object graphs sometimes serialization. And deep copying itself has costs: it allocates more, and naive recursion over **circular references** causes `StackOverflowError` unless you track already-copied objects.

---

## 💻 Implementation

We'll model an **event/message template** — a domain where prototype shines (a heavy envelope cloned per message) — evolving from the broken shallow copy to a production registry. Read the variants top-to-bottom: each fixes a specific defect of the previous one.

### Variant 0: Shallow Copy via Object.clone() / Cloneable (Anti-pattern)

**What's wrong with it:** This is the trap. Implementing `Cloneable` and returning `super.clone()` gives a **shallow copy** — primitives and references are copied field-by-field, so the clone and the original **share the same nested mutable objects** (the same `HashMap`, `List`, etc.). Mutating the clone's map mutates the original's, and vice versa — catastrophic data leakage between "independent" instances (in a distributed system, one event's `TraceId` bleeds into another). It also inherits all of `Cloneable`'s design flaws: it's a marker interface with no method, `clone()` is `protected` and throws a checked exception, and it bypasses the constructor.

<details>
<summary>💻 Click to expand code — shallow clone bug (anti-pattern)</summary>

```java
import java.util.*;

class EventTemplate implements Cloneable {
    String eventType;
    Map<String, String> headers = new HashMap<>();     // MUTABLE reference field

    EventTemplate(String type) {
        this.eventType = type;
        headers.put("Version", "1.0");
    }

    @Override
    public EventTemplate clone() {
        try {
            return (EventTemplate) super.clone();       // SHALLOW: headers map is SHARED!
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}

public class ShallowBug {
    public static void main(String[] args) {
        EventTemplate template = new EventTemplate("SIGNUP");

        EventTemplate e1 = template.clone();
        e1.headers.put("TraceId", "TXN-001");           // writes into the SHARED map

        EventTemplate e2 = template.clone();
        e2.headers.put("TraceId", "TXN-002");           // overwrites TXN-001 in the SAME map

        System.out.println(e1.headers.get("TraceId"));  // "TXN-002" — DATA LEAK between events!
    }
}
```
</details>

**Pros:** Minimal CPU for the copy itself; almost no code.
**Cons:** Shared mutable state ⇒ silent data corruption; `Cloneable` is a broken marker interface; `clone()` is `protected`, throws a checked exception, and bypasses the constructor (breaks `final` fields/invariants).
**Mechanism:** *`Object.clone()` does a bitwise, field-by-field shallow copy* — reference fields are copied as references, so both objects point at the same nested mutable objects.

### Variant 1: Deep Copy via a Custom Prototype Interface

**What problem it solves vs. V0:** It fixes the sharing bug and abandons `Cloneable`. You define your **own** `Prototype<T>` interface with a public `clone()`/`copy()` method, and each concrete prototype **deep-copies its mutable fields** (creates a *new* `HashMap`, copies each element) so the clone is fully independent. This is the correct, safe form of the pattern for object-oriented cloning, and it gives type safety (`Prototype<EventTemplate>.clone()` returns an `EventTemplate`, no casting).

**What's still limited:** You must remember to deep-copy *every* mutable field by hand — miss one and you're back to the V0 bug. For deeply nested graphs this is verbose and error-prone, and it doesn't yet handle `final` fields cleanly (that's the copy-constructor's strength) or circular references.

<details>
<summary>💻 Click to expand code — deep copy via custom Prototype interface</summary>

```java
import java.util.*;

interface Prototype<T> { T copy(); }                    // our own interface, public method

class EventTemplate implements Prototype<EventTemplate> {
    private String eventType;
    private Map<String, String> headers = new HashMap<>();

    EventTemplate(String type) { this.eventType = type; headers.put("Version", "1.0"); }
    void addHeader(String k, String v) { headers.put(k, v); }

    @Override
    public EventTemplate copy() {
        EventTemplate c = new EventTemplate(this.eventType);
        c.headers = new HashMap<>(this.headers);        // DEEP copy of the mutable map
        return c;                                        // clone is fully independent
    }
    void show() { System.out.println(eventType + " " + headers); }
}

public class DeepCopy {
    public static void main(String[] args) {
        EventTemplate payment = new EventTemplate("PAYMENT");
        payment.addHeader("Priority", "High");

        EventTemplate t1 = payment.copy(); t1.addHeader("TxId", "T100");
        EventTemplate t2 = payment.copy(); t2.addHeader("TxId", "T200");

        t1.show();   // PAYMENT {Version=1.0, Priority=High, TxId=T100}
        t2.show();   // PAYMENT {Version=1.0, Priority=High, TxId=T200}  <- isolated!
    }
}
```
</details>

**Pros:** True isolation (no shared mutable state); avoids broken `Cloneable`; type-safe (no casts); clear, explicit intent.
**Cons:** Must manually deep-copy every mutable field (easy to miss one); verbose for deep graphs; doesn't inherently handle `final` fields or cycles.
**Mechanism:** *Explicit reconstruction + per-field deep duplication.* You build a new instance and copy each mutable field into a fresh container, so no references are shared.

### Variant 2: Copy Constructor (Effective Java's Preferred Approach)

**What problem it solves vs. V1:** *Effective Java* Item 13 recommends **copy constructors/copy factories over `Cloneable`/`clone()`**, and this variant is the idiomatic Java form. A copy constructor `new EventTemplate(other)` (or a static `EventTemplate.copyOf(other)`) reads the source's state and builds a new object through a *real constructor* — so it works with **`final` fields**, runs normal **validation/invariants**, doesn't throw checked exceptions, needs no `Cloneable`, and returns the exact type with no casting. It's explicit and easy to reason about.

**What's still limited:** It's still manual per-field work (you must deep-copy mutable fields inside the constructor), and, like V1, it doesn't automatically solve circular references or extremely deep graphs — for those, serialization (V3) can be pragmatic.

<details>
<summary>💻 Click to expand code — copy constructor</summary>

```java
import java.util.*;

final class EventTemplate {
    private final String eventType;              // works with FINAL fields (clone() can't easily)
    private final Map<String, String> headers;

    EventTemplate(String type) {
        this.eventType = type;
        this.headers = new HashMap<>(Map.of("Version", "1.0"));
    }

    // Copy constructor: build a new object from an existing one, deep-copying mutables.
    EventTemplate(EventTemplate other) {
        this.eventType = other.eventType;                 // immutable String: fine to share
        this.headers   = new HashMap<>(other.headers);    // deep copy of mutable map
    }
    // Optional copy factory (reads better at call sites):
    static EventTemplate copyOf(EventTemplate o) { return new EventTemplate(o); }

    EventTemplate withHeader(String k, String v) { headers.put(k, v); return this; }
}

// Usage:
// EventTemplate base = new EventTemplate("AUTH");
// EventTemplate copy = new EventTemplate(base);   // or EventTemplate.copyOf(base)
```
</details>

**Pros:** Works with `final` fields; runs constructor validation/invariants; no `Cloneable`, no checked exception, no casting; explicit and readable; the *Effective Java*-recommended approach.
**Cons:** Manual per-field deep copy; doesn't auto-handle cycles or very deep graphs; a copy constructor per class.
**Mechanism:** *Construction from an existing instance.* A normal constructor reads the source's fields and initializes a new object, deep-copying mutable state — so `final` fields and invariants are honored (unlike constructor-bypassing `clone()`).

### Variant 3: Serialization-Based Deep Copy

**What problem it solves vs. V1/V2:** For **arbitrarily complex object graphs** (deep nesting, many mutable fields, or graphs you don't control), hand-writing deep copies is tedious and error-prone. Serializing an object to bytes and deserializing it produces a **complete deep copy of the entire reachable graph** in one operation — and Java's serialization even **handles circular references** automatically (it tracks object identity during the graph walk). This is a pragmatic "deep copy everything" hammer.

**What's the trade-off:** It's **much slower** (serialization overhead + reflection), requires everything in the graph to be `Serializable`, ignores `transient` fields, and — critically — **deserialization is a known security risk** (attacker-controlled byte streams can trigger gadget-chain remote code execution). Libraries like Apache Commons `SerializationUtils.clone()` wrap this. Use it for convenience on trusted data, not on hot paths or untrusted input.

<details>
<summary>💻 Click to expand code — serialization deep copy</summary>

```java
import java.io.*;

// Generic deep copy via serialization — works for any Serializable graph, incl. cycles.
static <T extends Serializable> T deepCopy(T obj) {
    try {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) { oos.writeObject(obj); }
        try (ObjectInputStream ois =
                 new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            @SuppressWarnings("unchecked")
            T copy = (T) ois.readObject();     // fresh, fully-independent graph
            return copy;
        }
    } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException("Deep copy failed", e);
    }
}
// Apache Commons Lang provides this as SerializationUtils.clone(obj).
// WARNING: slow; requires Serializable; NEVER deserialize untrusted bytes (RCE risk).
```
</details>

**Pros:** One-shot deep copy of arbitrarily complex graphs; automatically handles circular references and shared sub-objects; no per-field code.
**Cons:** Slow (serialization + reflection); everything must be `Serializable`; drops `transient` fields; **deserialization security vulnerability** on untrusted input; not for hot paths.
**Mechanism:** *Serialize-then-deserialize round-trip.* Writing the object graph to bytes and reading it back reconstructs a brand-new, disconnected graph; the serialization engine's identity tracking resolves cycles and shared references.

### Variant 4: Production Prototype Registry

**What problem it solves vs. V1–V3:** In production you combine deep copying with a **registry** — a cache of pre-configured "gold copy" prototypes keyed by name. Expensive setup (building the configured template) happens once; clients call `registry.get(key)` which returns a **deep copy** (using a copy constructor or `copy()`), so each caller gets a fresh, independent instance of a complex object without re-doing the setup. A `ConcurrentHashMap` makes the registry safe for concurrent access in multi-threaded event loops. This is the form that shows up in event pipelines and game engines.

**What's the trade-off:** You pay memory to hold the prototypes, and you must guarantee the registry hands out **copies** (never the shared prototype itself) so callers can't mutate the master. The prototypes themselves should be treated as immutable masters.

<details>
<summary>💻 Click to expand code — prototype registry</summary>

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

interface Prototype<T> { T copy(); }

class Event implements Prototype<Event> {
    private final String type;
    private Map<String, String> meta;
    Event(String type, Map<String, String> meta) { this.type = type; this.meta = meta; }
    Event(Event o) { this.type = o.type; this.meta = new HashMap<>(o.meta); }  // deep copy ctor
    public Event copy() { return new Event(this); }
    void setPayload(String p) { meta.put("payload", p); }
    void dispatch() { System.out.println("Dispatch " + type + " " + meta); }
}

class EventRegistry {
    private final Map<String, Event> templates = new ConcurrentHashMap<>();   // thread-safe
    void register(String key, Event prototype) { templates.put(key, prototype); }
    Event create(String key) {
        Event p = templates.get(key);
        if (p == null) throw new IllegalArgumentException("No template: " + key);
        return p.copy();                     // ALWAYS return a copy, never the shared master
    }
}

public class EventStream {
    public static void main(String[] args) {
        EventRegistry registry = new EventRegistry();
        // Expensive setup done ONCE:
        registry.register("AUTH", new Event("USER_AUTH",
                new HashMap<>(Map.of("SecurityLevel", "L3", "AuthServer", "auth-01"))));

        for (int i = 0; i < 3; i++) {
            Event e = registry.create("AUTH");   // fresh independent copy per message
            e.setPayload("user_" + i);
            e.dispatch();
        }
    }
}
```
</details>

**Pros:** Amortizes expensive setup (build once, clone many); constant-time retrieval of complex configured objects; `ConcurrentHashMap` gives thread-safe access; avoids subclass/factory explosion.
**Cons:** Memory to hold prototypes; must strictly return copies (never the master); prototypes must be maintained as immutable gold copies.
**Mechanism:** *Object caching + deep-copy on read.* The registry stores gold-copy prototypes; each `create()` deep-copies one, so heavy construction is replaced by cheaper memory copies.

---

## 🎨 Real-World Example

Three complete, production-shaped examples, each fully collapsible. All three follow the same production shape — a registry of expensive-to-build "gold copy" prototypes that are **deep-copied on read** so each caller gets an independent instance — because that's exactly how real orchestration systems reuse templates. The first is a **Kubernetes Pod template**, the second a **VM / EC2 image (AMI)**, the third a **workflow / job template**. Each ends with a walkthrough of *why the code is shaped the way it is*.

### Scenario 1 — Kubernetes Pod Template Registry

<details>
<summary>💻 Click to expand — Pod template registry + demo + explanation</summary>

Kubernetes itself is built on prototypes: a Deployment holds a `PodTemplateSpec`, and every Pod it creates is a **copy** of that template with per-instance fields (name, labels) filled in. Here we model a registry of configured Pod templates that are deep-copied to produce concrete Pods, so mutating one Pod's labels or env never touches the template or sibling Pods.

```java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

interface Prototype<T> {
    T copy();
}

/** A Pod spec: cheap fields plus mutable collections that MUST be deep-copied. */
final class PodSpec implements Prototype<PodSpec> {
    private String name;
    private final String image;                 // immutable, safe to share
    private final Map<String, String> labels;   // mutable -> deep copy
    private final List<String> containers;       // mutable -> deep copy

    PodSpec(String name, String image) {
        this.name = name;
        this.image = image;
        this.labels = new HashMap<>();
        this.containers = new ArrayList<>();
    }

    /** Deep-copy constructor: duplicate every mutable field so the copy is independent. */
    PodSpec(PodSpec other) {
        this.name = other.name;
        this.image = other.image;                       // String is immutable -> share
        this.labels = new HashMap<>(other.labels);      // fresh map
        this.containers = new ArrayList<>(other.containers);
    }

    @Override
    public PodSpec copy() {
        return new PodSpec(this);
    }

    PodSpec name(String n)          { this.name = n; return this; }
    PodSpec label(String k, String v) { this.labels.put(k, v); return this; }
    PodSpec addContainer(String c)  { this.containers.add(c); return this; }

    void describe() {
        System.out.println("Pod{name=" + name + ", image=" + image
                + ", labels=" + labels + ", containers=" + containers + "}");
    }
}

/** Registry of pre-configured Pod templates; hands out COPIES, never the master. */
final class PodTemplateRegistry {
    private final Map<String, PodSpec> templates = new ConcurrentHashMap<>();

    void register(String key, PodSpec template) {
        templates.put(key, template);
    }

    PodSpec instantiate(String key) {
        PodSpec template = templates.get(key);
        if (template == null) {
            throw new IllegalArgumentException("No Pod template: " + key);
        }
        return template.copy();   // deep copy -> caller gets an independent Pod
    }
}

public class PodTemplateDemo {
    public static void main(String[] args) {
        PodTemplateRegistry registry = new PodTemplateRegistry();

        // Expensive-to-configure template built ONCE.
        PodSpec webTemplate = new PodSpec("web-template", "nginx:1.27")
                .label("app", "storefront")
                .label("tier", "frontend")
                .addContainer("nginx");
        registry.register("web", webTemplate);

        // Each replica is a clone with only per-instance fields changed.
        PodSpec replica1 = registry.instantiate("web").name("web-0").label("pod", "web-0");
        PodSpec replica2 = registry.instantiate("web").name("web-1").label("pod", "web-1");

        replica1.describe();
        replica2.describe();
        // Pod{name=web-0, image=nginx:1.27, labels={app=storefront, tier=frontend, pod=web-0}, containers=[nginx]}
        // Pod{name=web-1, image=nginx:1.27, labels={app=storefront, tier=frontend, pod=web-1}, containers=[nginx]}
        // replica1's pod=web-0 label does NOT appear on replica2 -> deep copy verified.
    }
}
```

**Code explanation.** The `PodSpec` is the prototype: it mixes an immutable field (`image`, a `String`) with two mutable collections (`labels`, `containers`) — and the whole correctness of the pattern hinges on treating those differently. The **deep-copy constructor** `PodSpec(PodSpec other)` shares the immutable `String` by reference (safe — it can never change) but allocates a **new** `HashMap` and `ArrayList` from the originals, so the clone and the template share no mutable state. This is exactly the shallow-vs-deep distinction: had we written `this.labels = other.labels`, adding a `pod=web-0` label to one replica would corrupt the template and every sibling. `copy()` simply delegates to that constructor, and the `PodTemplateRegistry` stores one configured "gold copy" per key and returns `template.copy()` on every `instantiate(...)` — never the master itself, so callers can freely mutate their Pod. `ConcurrentHashMap` makes the registry safe for concurrent reads in a controller loop. The demo proves independence: two replicas cloned from the same template each get their own name/label without leaking into the other — precisely how a real Deployment stamps out Pods from a `PodTemplateSpec`. The builder is single-thread scaffolding; the cloned Pods are independent objects safe to hand to different reconcile threads.

</details>

### Scenario 2 — VM / EC2 Image (AMI) Registry

<details>
<summary>💻 Click to expand — AMI registry + demo + explanation</summary>

Launching cloud VMs is the canonical Prototype: an **AMI** (Amazon Machine Image) is a pre-baked "golden image" — OS, packages, config all installed once — and every EC2 instance is booted as a **copy** of that image with per-instance settings (hostname, tags) applied. Baking the image is expensive; cloning it is cheap. This registry models golden images that are deep-copied into launchable instances.

```java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

interface Prototype<T> {
    T copy();
}

/** A golden machine image: expensive to bake, cheap to clone. */
final class MachineImage implements Prototype<MachineImage> {
    private String hostname;
    private final String baseOs;                    // immutable
    private final List<String> installedPackages;    // mutable -> deep copy
    private final Map<String, String> tags;          // mutable -> deep copy

    MachineImage(String baseOs) {
        this.hostname = "unassigned";
        this.baseOs = baseOs;
        this.installedPackages = new ArrayList<>();
        this.tags = new HashMap<>();
    }

    /** Deep-copy constructor. */
    MachineImage(MachineImage other) {
        this.hostname = other.hostname;
        this.baseOs = other.baseOs;                              // immutable -> share
        this.installedPackages = new ArrayList<>(other.installedPackages);
        this.tags = new HashMap<>(other.tags);
    }

    @Override
    public MachineImage copy() {
        return new MachineImage(this);
    }

    MachineImage install(String pkg)        { this.installedPackages.add(pkg); return this; }
    MachineImage tag(String k, String v)    { this.tags.put(k, v); return this; }
    MachineImage hostname(String h)         { this.hostname = h; return this; }

    void boot() {
        System.out.println("Booting " + hostname + " [" + baseOs + "] "
                + "packages=" + installedPackages + " tags=" + tags);
    }
}

/** Registry of golden images (AMIs). Each launch clones the image. */
final class ImageRegistry {
    private final Map<String, MachineImage> images = new ConcurrentHashMap<>();

    void bake(String amiId, MachineImage image) {
        images.put(amiId, image);   // expensive baking happens once, up front
    }

    MachineImage launch(String amiId) {
        MachineImage image = images.get(amiId);
        if (image == null) {
            throw new IllegalArgumentException("No such AMI: " + amiId);
        }
        return image.copy();   // clone the golden image into a fresh instance
    }
}

public class AmiDemo {
    public static void main(String[] args) {
        ImageRegistry registry = new ImageRegistry();

        // Bake a golden web-server image ONCE (the expensive step).
        MachineImage webAmi = new MachineImage("ubuntu-22.04")
                .install("nginx")
                .install("openjdk-21")
                .tag("role", "web");
        registry.bake("ami-web-1", webAmi);

        // Launch two instances — each is an independent clone.
        MachineImage i1 = registry.launch("ami-web-1").hostname("web-a").tag("az", "us-east-1a");
        MachineImage i2 = registry.launch("ami-web-1").hostname("web-b").tag("az", "us-east-1b");

        i1.boot();
        i2.boot();
        // Booting web-a [ubuntu-22.04] packages=[nginx, openjdk-21] tags={role=web, az=us-east-1a}
        // Booting web-b [ubuntu-22.04] packages=[nginx, openjdk-21] tags={role=web, az=us-east-1b}
        // i1's az tag does NOT leak into i2 -> deep copy verified.
    }
}
```

**Code explanation.** This is the textbook justification for Prototype — "creation is expensive, copying is cheap." Baking an AMI (installing the OS and packages) is modeled by configuring the `MachineImage` prototype once; every `launch(...)` then produces an instance by **cloning** rather than reinstalling everything from scratch. The **deep-copy constructor** again separates the immutable `baseOs` (shared) from the mutable `installedPackages` list and `tags` map (freshly copied), so per-instance changes — a hostname, an availability-zone tag — stay local to that instance. The registry's vocabulary makes the mapping explicit: `bake(amiId, image)` registers a golden image (the costly one-time step) and `launch(amiId)` returns `image.copy()`, mirroring `RunInstances` booting from an AMI. Crucially the registry returns copies, never the golden master, so a rogue instance can't mutate the shared image and poison future launches. The demo boots two instances from one AMI and shows their tags don't cross-contaminate. The performance argument to state in an interview: if you needed 500 identical web servers, you bake once and clone 500 times, instead of paying full construction cost 500 times — memory-to-memory copy versus repeated heavy setup.

</details>

### Scenario 3 — Workflow / Job Template Registry

<details>
<summary>💻 Click to expand — workflow/job template registry + demo + explanation</summary>

CI/CD systems, Airflow, and Argo Workflows all reuse **job/workflow templates**: define a pipeline once (its ordered steps, parameters, retry policy) and instantiate a fresh, independent run each time it's triggered. Cloning a template to create a run — rather than rebuilding the step graph — is Prototype, and the deep copy ensures one run's parameter overrides never bleed into another.

```java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

interface Prototype<T> {
    T copy();
}

/** A workflow template: ordered steps + parameters + retry policy. */
final class WorkflowTemplate implements Prototype<WorkflowTemplate> {
    private String runId;
    private final List<String> steps;              // mutable -> deep copy
    private final Map<String, String> parameters;  // mutable -> deep copy
    private int maxRetries;                         // primitive -> copied by value

    WorkflowTemplate(String name) {
        this.runId = name + "-template";
        this.steps = new ArrayList<>();
        this.parameters = new HashMap<>();
        this.maxRetries = 0;
    }

    /** Deep-copy constructor. */
    WorkflowTemplate(WorkflowTemplate other) {
        this.runId = other.runId;
        this.steps = new ArrayList<>(other.steps);          // independent step list
        this.parameters = new HashMap<>(other.parameters);  // independent params
        this.maxRetries = other.maxRetries;
    }

    @Override
    public WorkflowTemplate copy() {
        return new WorkflowTemplate(this);
    }

    WorkflowTemplate step(String s)              { this.steps.add(s); return this; }
    WorkflowTemplate param(String k, String v)   { this.parameters.put(k, v); return this; }
    WorkflowTemplate retries(int n)              { this.maxRetries = n; return this; }
    WorkflowTemplate runId(String id)            { this.runId = id; return this; }

    void execute() {
        System.out.println("Run " + runId + " steps=" + steps
                + " params=" + parameters + " maxRetries=" + maxRetries);
    }
}

/** Registry of workflow templates; each trigger clones a template into a run. */
final class WorkflowRegistry {
    private final Map<String, WorkflowTemplate> templates = new ConcurrentHashMap<>();

    void register(String name, WorkflowTemplate template) {
        templates.put(name, template);
    }

    WorkflowTemplate newRun(String name) {
        WorkflowTemplate template = templates.get(name);
        if (template == null) {
            throw new IllegalArgumentException("No workflow template: " + name);
        }
        return template.copy();   // each run is an independent clone
    }
}

public class WorkflowDemo {
    public static void main(String[] args) {
        WorkflowRegistry registry = new WorkflowRegistry();

        // Define the CI pipeline template ONCE.
        WorkflowTemplate ciPipeline = new WorkflowTemplate("ci")
                .step("checkout")
                .step("build")
                .step("test")
                .step("deploy")
                .param("branch", "main")
                .retries(2);
        registry.register("ci", ciPipeline);

        // Two triggered runs — independent, with their own parameter overrides.
        WorkflowTemplate run1 = registry.newRun("ci").runId("ci-1001").param("branch", "feature-x");
        WorkflowTemplate run2 = registry.newRun("ci").runId("ci-1002");  // keeps default branch

        run1.execute();
        run2.execute();
        // Run ci-1001 steps=[checkout, build, test, deploy] params={branch=feature-x} maxRetries=2
        // Run ci-1002 steps=[checkout, build, test, deploy] params={branch=main} maxRetries=2
        // run1's branch override does NOT affect run2 -> deep copy verified.
    }
}
```

**Code explanation.** A workflow template carries an ordered `steps` list, a `parameters` map, and a scalar `maxRetries` — the mix that makes the deep-vs-shallow choice concrete. The **deep-copy constructor** copies `maxRetries` by value (primitives are trivially independent) and allocates fresh `ArrayList`/`HashMap` copies of the step graph and parameters, so each run can override its own parameters (`run1` switches `branch` to `feature-x`) without mutating the template or any concurrent run — the demo shows `run2` still sees the default `branch=main`. This is why the pattern fits orchestration: you define an expensive-to-assemble pipeline once and stamp out isolated executions cheaply, exactly as Argo Workflows clones a `WorkflowTemplate` into a `Workflow` or Airflow instantiates a DAG run. The `WorkflowRegistry` keeps the master template and returns `template.copy()` per trigger, so a run that mutates its parameters (or, in a fuller model, records step statuses) can never corrupt the shared definition. As with the other scenarios, the registry hands out clones rather than the master, and `ConcurrentHashMap` allows concurrent triggering. In an interview this is a clean answer to "where is Prototype used at scale?" — anywhere a reusable template is instantiated repeatedly into independent, mutable runs.

</details>

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- **Object creation is expensive** (heavy I/O, DB/API calls, computation) and copying a pre-built instance is cheaper than reconstructing.
- You need **many objects that differ only slightly** in state and want to avoid a subclass/factory per variant.
- You must **create objects without knowing their concrete class** — cloning through a common interface.
- You want to **snapshot a configured object** once and hand out independent copies (a registry of gold-copy prototypes).
- The object's **initial configuration is complex** and error-prone to rebuild manually each time.

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- **Simple objects** with a few primitive fields — `new` is faster, clearer, and safer than cloning machinery.
- **Immutable objects** — you can safely *share* the reference; cloning is pointless (there's no state to isolate).
- **Deep, circular, or externally-owned object graphs** — correct deep copying is hard and error-prone; consider whether you really need independent copies.
- **When construction is cheap** — the prototype's whole justification (avoiding costly `new`) disappears.
- **Untrusted data with serialization-based copy** — deserialization is a security risk; never round-trip attacker-controlled bytes.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- **Performance** — avoids repeating expensive initialization; memory copy replaces heavy construction.
- **Fewer subclasses** — configure prototypes at runtime instead of modeling each variant as a class.
- **Decoupling** — clients clone through an interface, unaware of concrete classes.
- **Runtime flexibility** — add/remove/reconfigure prototypes (in a registry) at runtime.
- **Snapshotting** — capture a configured state once and reproduce it independently many times.

**Cons**

- **Deep-copy complexity (the headline con)** — you must deep-copy every mutable field; missing one causes silent shared-state bugs.
- **Circular references** — naive recursive deep copy overflows the stack unless you track visited objects.
- **`Cloneable` is broken** — the JDK's cloning mechanism is a marker interface, `protected`, shallow, constructor-bypassing; prefer copy constructors.
- **Cloning overhead** — deep copies allocate; heavy per-clone graphs add GC pressure.
- **Security** — serialization-based deep copy exposes deserialization vulnerabilities on untrusted input.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

Prototype is compared with the other creationals (it's an *alternative* to constructing via factories) and with **Flyweight** and **Memento** (both involve object state). The distinctions are about *copy vs. construct vs. share vs. restore*.

| Pattern | What it does | Mechanism | Key tell |
|---|---|---|---|
| **Prototype** | Create by **copying** a configured instance | `clone()` / copy constructor | "Cloning is cheaper than building; avoid subclass explosion" |
| **Factory Method** | Create **from scratch**, type chosen by subclass | Overridable method | "Subclass decides which class to build" |
| **Abstract Factory** | Create a **family** from scratch | Object with creation methods | "Matched set of consistent products" |
| **Singleton** | Ensure **one** instance | Private ctor + static accessor | "Exactly one" — the opposite of Prototype's "make many" |
| **Flyweight** | **Share** one instance to save memory | Interning/pooling | "Many references, one shared object" |
| **Memento** | **Capture/restore** an object's state | Snapshot object | "Undo/history" — restore, not duplicate |

**Prototype vs. Factory Method/Abstract Factory:** the factories build objects *from scratch* via constructors (and can require a subclass/factory per type); Prototype builds by *copying* a configured specimen, which sidesteps costly construction and subclass explosion. A factory can even *use* prototypes internally (its "create" clones a registered prototype).

**Prototype vs. Singleton:** they're opposites — Singleton guarantees exactly one instance; Prototype exists to churn out many independent instances. (Amusingly, a prototype *registry* is often itself a Singleton.)

**Prototype vs. Flyweight:** Flyweight *shares* a single immutable instance among many clients to save memory; Prototype *copies* to create independent instances. Flyweight = sharing; Prototype = duplication — the exact inverse intent.

**Prototype vs. Memento:** both deal with object state, but Memento *captures* state to *restore* it later (undo/history), while Prototype *uses* current state to *create* a new independent object. Memento is about time (history); Prototype is about multiplicity (copies).

</details>

## 📊 Comparison Table of Variants

<details>
<summary>📖 Click to expand</summary>

| Axis | V0 Shallow clone | V1 Deep (custom iface) | V2 Copy constructor | V3 Serialization | V4 Registry |
|---|---|---|---|---|---|
| **Copy depth** | Shallow (shared!) | Deep (manual) | Deep (manual) | Deep (whole graph) | Deep (via V1/V2) |
| **Safe (isolated)** | ❌ | ✅ | ✅ | ✅ | ✅ |
| **Handles `final` fields** | ❌ (bypasses ctor) | ⚠️ | ✅ | ✅ | ✅ |
| **Handles circular refs** | n/a | ❌ (manual) | ❌ (manual) | ✅ (auto) | depends |
| **Performance** | Fastest (but broken) | Fast | Fast | Slow | Fast (amortized setup) |
| **Uses broken `Cloneable`** | Yes | No | No | No | No |
| **Security risk** | — | — | — | ⚠️ (deserialization) | — |
| **Best for** | never (teaching only) | OO cloning | idiomatic Java | arbitrary graphs (trusted) | configured-object caches |

**Best choice:** the **copy constructor (V2)** for idiomatic Java cloning (works with `final` fields, runs validation), combined with a **registry (V4)** when you're caching expensive-to-build configured objects. Use **serialization (V3)** only as a convenience for complex trusted graphs. Never ship **V0**.

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

### Pitfall 1: Forgetting to deep-copy mutable fields (shared state)

**What goes wrong:** The clone and original share a nested mutable object (list/map), so mutating one corrupts the other — the classic Prototype bug.

<details>
<summary>💻 Broken</summary>

```java
public Order copy() { return (Order) super.clone(); }   // shallow -> items list is SHARED
```
</details>

<details>
<summary>💻 Fix — deep-copy each mutable field</summary>

```java
public Order copy() {
    Order c = (Order) super.clone();
    c.items = new ArrayList<>(this.items);   // independent list
    return c;
}
// Better: a copy constructor that new-copies every mutable field.
```
</details>

### Pitfall 2: Relying on `Cloneable` / `super.clone()`

**What goes wrong:** `Cloneable` is a marker interface with no `clone()`; `Object.clone()` is `protected`, shallow, throws a checked exception, and bypasses the constructor (so `final` fields and invariants break).

<details>
<summary>💻 Fragile</summary>

```java
class Foo implements Cloneable {                 // marker only; clone() is protected in Object
    public Object clone() throws CloneNotSupportedException { return super.clone(); } // shallow
}
```
</details>

<details>
<summary>💻 Fix — copy constructor / copy factory (Effective Java Item 13)</summary>

```java
final class Foo {
    private final int x; private final List<Integer> xs;
    Foo(Foo o) { this.x = o.x; this.xs = new ArrayList<>(o.xs); }  // works with final fields
    static Foo copyOf(Foo o) { return new Foo(o); }
}
```
</details>

### Pitfall 3: Ignoring circular references (StackOverflow)

**What goes wrong:** Naive recursive deep copy of a graph with a cycle (A→B→A) recurses forever and throws `StackOverflowError`.

<details>
<summary>💻 Broken</summary>

```java
Node copy() { Node n = new Node(val); n.next = this.next.copy(); return n; }  // cycle -> infinite
```
</details>

<details>
<summary>💻 Fix — track already-copied objects in an identity map</summary>

```java
Node copy(Map<Node, Node> seen) {
    if (seen.containsKey(this)) return seen.get(this);     // already copied -> reuse
    Node n = new Node(val); seen.put(this, n);
    if (next != null) n.next = next.copy(seen);
    return n;
}
```
</details>

### Pitfall 4: Deserialization-based deep copy on untrusted input (security)

**What goes wrong:** Using serialize/deserialize to deep-copy attacker-controlled objects exposes **deserialization gadget-chain vulnerabilities** (potential remote code execution).

<details>
<summary>💻 Fix</summary>

```java
// Never deserialize untrusted bytes to "clone". Use explicit copy constructors,
// or if you must serialize, restrict classes via ObjectInputFilter (JEP 290):
ObjectInputStream ois = new ObjectInputStream(in);
ois.setObjectInputFilter(filterInfo -> /* allow only known-safe classes */ ...);
```
</details>

</details>

---

## 🎓 Interview Tips

**What interviewers commonly ask:**

- "What's the difference between a shallow and a deep copy?" (The #1 Prototype question — be ready with the shared-mutable-field example.)
- "Why is `Cloneable`/`Object.clone()` considered broken?" (Marker interface, protected, shallow, bypasses constructor, checked exception.)
- "When would you use Prototype over a Factory?" (Costly construction; avoid subclass explosion; copy without knowing the class.)
- "How do you deep-copy an object with circular references?" (Identity map of already-copied objects.)
- "Implement a deep copy / a prototype registry."

**What you should proactively mention even if not asked:**

- **Copy constructors / copy factories are preferred over `clone()`** in modern Java (Effective Java Item 13) — say this; it signals you know the JDK pitfalls.
- The **shallow-vs-deep** distinction and the exact bug shallow copies cause (shared nested mutable state).
- **Serialization** as a one-shot deep-copy for complex graphs — and its **security** (deserialization RCE) and performance caveats.
- **Circular references** need an identity map to avoid `StackOverflowError`.
- A **prototype registry** (often a Singleton) is the production form; it must always return copies, never the master.
- Prototype is the **inverse of Flyweight** (copy vs. share) and **opposite of Singleton** (many vs. one).

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Abstract Factory** — can be *implemented with* prototypes: instead of subclassed factories, it clones registered prototype family members.
- **Factory Method** — a factory method may return a **clone** of a prototype rather than a freshly constructed object.
- **Singleton** — a prototype **registry** is frequently a Singleton (one shared cache of gold copies).
- **Flyweight** — the conceptual inverse: Flyweight shares one instance; Prototype duplicates. They can coexist (shared immutable parts + cloned mutable wrappers).
- **Memento** — related to state handling: Memento snapshots to *restore*; Prototype copies to *create*.
- **Composite / Decorator** — often need clone support so entire trees/wrapped structures can be duplicated.

</details>

## 📚 Library/Framework Implementation

**1. `Object.clone()` / `ArrayList.clone()` / array cloning (JDK).** The JDK's built-in cloning is the canonical (shallow) prototype mechanism — and a cautionary tale. `ArrayList.clone()` copies the internal array but **shallowly**: the elements are shared references, which is why the docs warn you to copy mutable elements yourself.

<details>
<summary>💻 Click to expand — ArrayList.clone() shallow behavior</summary>

```java
import java.util.*;

class User { String name; User(String n){ name = n; } }

ArrayList<User> a = new ArrayList<>(List.of(new User("John")));
@SuppressWarnings("unchecked")
ArrayList<User> b = (ArrayList<User>) a.clone();   // shallow: same User objects
b.get(0).name = "Jane";
System.out.println(a.get(0).name);                 // "Jane" — shared element! (shallow copy)
```
</details>

**2. Spring `@Scope("prototype")` bean scope.** Spring's prototype scope is a direct application of the pattern's *intent*: unlike a singleton bean (one shared instance), a prototype-scoped bean yields a **new instance on every injection/lookup**. The container acts as a factory that produces a fresh object per request rather than sharing one.

<details>
<summary>💻 Click to expand — Spring prototype scope</summary>

```java
@Component
@Scope("prototype")                 // new instance every time it's requested
class RequestContext { /* per-request state */ }

// applicationContext.getBean(RequestContext.class) returns a DIFFERENT instance each call.
```
</details>

**3. Apache Commons Lang `SerializationUtils.clone()`.** A widely-used utility that performs a **serialization-based deep clone** of any `Serializable` object graph in one call — handling nesting and cycles automatically. It's convenient but slow and carries the deserialization caveats, so it's for trusted data and non-hot paths.

<details>
<summary>💻 Click to expand — SerializationUtils.clone</summary>

```java
import org.apache.commons.lang3.SerializationUtils;
import java.io.Serializable;

class Config implements Serializable { /* ... complex nested graph ... */ }

Config original = loadConfig();
Config deepCopy = SerializationUtils.clone(original);   // full deep copy of the graph
// Convenient, but slower than a copy constructor; requires Serializable; trusted data only.
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1: [Conceptual] What is the Prototype pattern and what problem does it solve?</strong></summary>

Prototype is a **creational** GoF pattern that creates new objects by **copying an existing, configured instance** (the prototype) rather than constructing from scratch. The client asks a prototype to duplicate itself via `clone()`/a copy constructor, so it doesn't need to know the concrete class or how to build it.

It solves two problems: **expensive construction** (if `new` does heavy I/O/computation, cloning a pre-built instance is cheaper) and **subclass/factory explosion** (instead of a class per minor configuration variant, keep one configured prototype per variant and copy it). It also lets you create objects **without knowing their concrete class**, cloning through a common interface. The whole practical difficulty is copy depth — shallow vs. deep.
</details>

<details>
<summary><strong>Q2: [Conceptual] What is the difference between a shallow copy and a deep copy?</strong></summary>

A **shallow copy** duplicates the top-level object field-by-field: primitives are copied by value, but **reference fields are copied as references**, so the original and the copy **share** the same nested mutable objects (lists, maps, sub-objects). Mutating a shared nested object through one affects the other.

A **deep copy** recursively duplicates the entire object graph — every mutable nested object is itself copied — so the copy is **fully independent** and shares no mutable state with the original.

`Object.clone()` gives a shallow copy by default; a correct prototype must deep-copy every mutable field. This distinction is the single most common Prototype interview question, and getting it wrong is the pattern's classic production bug.
</details>

<details>
<summary><strong>Q3: [Conceptual] What are the participants of the Prototype pattern?</strong></summary>

Four:

1. **Prototype** — an interface/abstract type declaring the copy operation (`clone()`/`copy()`).
2. **ConcretePrototype** — implements the copy operation, correctly duplicating its state (deep-copying mutables).
3. **Client** — creates new objects by calling `copy()` on a prototype, never a concrete constructor.
4. **Prototype Registry** (optional) — a cache/map of pre-configured prototypes keyed by name; `registry.get(key).copy()` returns a fresh independent instance. In production this registry is the common form and is often a Singleton.
</details>

<details>
<summary><strong>Q4: [Conceptual] When would you prefer Prototype over a Factory?</strong></summary>

Prefer Prototype when: (1) construction is **expensive** (DB/API/file load, heavy computation) and copying is cheaper; (2) you'd otherwise need a **subclass/factory per configuration** (Prototype replaces that with one configured instance per variant, cloned on demand); or (3) you must create an object **without knowing its concrete class**, cloning through an interface.

Prefer a Factory when construction is cheap and you're selecting *which type/family* to build from scratch. They're not mutually exclusive — a factory can be *implemented with* prototypes (its "create" clones a registered prototype), combining "decouple the client from concrete classes" (factory intent) with "avoid costly construction / subclass explosion" (prototype benefit).
</details>

<details>
<summary><strong>Q5: [Breaking] Why is Java's `Cloneable` interface considered poorly designed?</strong></summary>

Several reasons, and citing them signals depth:

- `Cloneable` is a **marker interface with no methods** — it doesn't declare `clone()`. The actual `clone()` lives in `Object`, is `protected`, and only shallow-copies.
- To use it you must **override `clone()` as public** and call `super.clone()`, casting the result — and it throws a **checked `CloneNotSupportedException`** you must handle.
- `clone()` **bypasses constructors**, so `final` fields can't be set normally and constructor validation/invariants don't run.
- It shallow-copies by default, so you must manually deep-copy mutable fields anyway.

*Effective Java* Item 13 concludes: avoid `Cloneable`; provide a **copy constructor or copy factory** instead. That's the modern, recommended way to implement Prototype in Java.
</details>

<details>
<summary><strong>Q6: [Implementation] How do you implement a thread-safe prototype registry?</strong></summary>

Back the registry with a `ConcurrentHashMap` so concurrent registration and lookup are safe, and ensure `create()` always returns a **copy** (never the shared master), so callers can't mutate the prototype.

<details>
<summary>💻 Code</summary>

```java
class Registry {
    private final Map<String, Prototype<?>> map = new ConcurrentHashMap<>();
    void register(String k, Prototype<?> p) { map.put(k, p); }
    Prototype<?> create(String k) {
        Prototype<?> p = map.get(k);
        if (p == null) throw new IllegalArgumentException("No prototype: " + k);
        return p.copy();     // fresh copy per call; master never escapes
    }
}
```
</details>

The prototypes themselves should be treated as **immutable gold copies** (or the copy operation must be internally thread-safe), so concurrent `copy()` calls don't race on the master's state.
</details>

<details>
<summary><strong>Q7: [Implementation] What is a copy constructor and how does it relate to Prototype?</strong></summary>

A **copy constructor** takes an instance of the same class and builds a new object from its state: `new Foo(existingFoo)`. It's *Effective Java*'s recommended way to implement Prototype's copy operation because it works with **`final` fields**, runs normal **constructor validation**, doesn't require `Cloneable`, throws no checked exception, and returns the exact type without casting.

<details>
<summary>💻 Code</summary>

```java
final class Order {
    private final String id;
    private final List<Item> items;
    Order(Order o) { this.id = o.id; this.items = new ArrayList<>(o.items); }  // deep copy
}
// Order copy = new Order(original);  // or a static Order.copyOf(original)
```
</details>

A copy *factory* (`Order.copyOf(o)`) is the static-method flavor and often reads better at call sites. Both are strictly preferable to `clone()`.
</details>

<details>
<summary><strong>Q8: [Implementation] Coding challenge — deep-copy a Document containing a List of Pages.</strong></summary>

<details>
<summary>💻 Complete solution</summary>

```java
import java.util.*;

class Page {
    private String content;
    Page(String c) { this.content = c; }
    Page(Page o) { this.content = o.content; }     // copy constructor
    Page copy() { return new Page(this); }
}

class Document {
    private String title;
    private List<Page> pages;
    Document(String title) { this.title = title; this.pages = new ArrayList<>(); }
    Document(Document o) {                           // deep-copy constructor
        this.title = o.title;
        this.pages = new ArrayList<>();
        for (Page p : o.pages) this.pages.add(p.copy());   // copy each page, not the refs
    }
    Document copy() { return new Document(this); }
    void addPage(Page p) { pages.add(p); }
    int pageCount() { return pages.size(); }
}

// Demo: modifying the copy's pages doesn't touch the original.
// Document d1 = new Document("Spec"); d1.addPage(new Page("intro"));
// Document d2 = d1.copy(); d2.addPage(new Page("appendix"));
// d1.pageCount() == 1, d2.pageCount() == 2  -> independent
```
</details>
</details>

<details>
<summary><strong>Q9: [Implementation] Coding challenge — a ShapeRegistry that registers shapes at runtime and clones them.</strong></summary>

<details>
<summary>💻 Complete solution</summary>

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

interface Shape { Shape copy(); void draw(); }

class Circle implements Shape {
    private int radius;
    Circle(int r) { this.radius = r; }
    public Shape copy() { return new Circle(this.radius); }
    public void draw() { System.out.println("Circle r=" + radius); }
}
class Square implements Shape {
    private int side;
    Square(int s) { this.side = s; }
    public Shape copy() { return new Square(this.side); }
    public void draw() { System.out.println("Square s=" + side); }
}

class ShapeRegistry {
    private final Map<String, Shape> cache = new ConcurrentHashMap<>();
    void register(String key, Shape prototype) { cache.put(key, prototype); }
    Shape create(String key) {
        Shape p = cache.get(key);
        if (p == null) throw new IllegalArgumentException("No shape: " + key);
        return p.copy();     // clone, never the master
    }
}

// ShapeRegistry r = new ShapeRegistry();
// r.register("big-circle", new Circle(100));
// r.create("big-circle").draw();   // independent copy each time
```
</details>
</details>

<details>
<summary><strong>Q10: [Breaking] What happens when you clone an object that references itself, and how do you fix it?</strong></summary>

A naive recursive deep copy of a **circular** graph (A→B→A, or a node pointing back to itself) recurses forever and throws `StackOverflowError`, because each clone triggers cloning of the referenced object, which triggers cloning back, endlessly.

<details>
<summary>💻 Fix — identity map of already-cloned objects</summary>

```java
Node copy(Map<Node, Node> seen) {
    Node existing = seen.get(this);
    if (existing != null) return existing;       // already cloned -> return the clone
    Node n = new Node(this.value);
    seen.put(this, n);                            // record BEFORE recursing
    for (Node child : this.children) n.children.add(child.copy(seen));
    return n;
}
```
</details>

The `IdentityHashMap` tracks originals→clones so a revisited node returns its existing clone instead of recursing, correctly reproducing cycles and shared sub-objects. (Serialization-based deep copy does this bookkeeping automatically.)
</details>

<details>
<summary><strong>Q11: [Breaking] What's the classic shallow-copy bug in a prototype, with an example?</strong></summary>

The prototype copies a reference to a mutable field instead of duplicating it, so all "copies" share one nested object. Writes through any copy corrupt the others.

<details>
<summary>💻 The bug</summary>

```java
class Event implements Cloneable {
    Map<String,String> headers = new HashMap<>();
    public Event clone() throws CloneNotSupportedException { return (Event) super.clone(); }
}
Event a = template.clone(); a.headers.put("id","1");
Event b = template.clone(); b.headers.put("id","2");   // same map as a!
// a.headers.get("id") == "2"  -> data leak across "independent" events
```
</details>

<details>
<summary>💻 The fix</summary>

```java
public Event copy() {
    Event c = new Event();
    c.headers = new HashMap<>(this.headers);   // deep copy the mutable map
    return c;
}
```
</details>

In distributed systems this manifests as one request's trace/headers bleeding into another — a nasty, intermittent production bug.
</details>

<details>
<summary><strong>Q12: [Breaking] What are the risks of implementing deep copy via serialization?</strong></summary>

Three big ones: (1) **Performance** — serialize+deserialize is far slower than a copy constructor (byte encoding + reflection), unacceptable on hot paths. (2) **Constraints** — every class in the graph must be `Serializable`, and `transient` fields are dropped (so the copy loses them). (3) **Security** — deserialization is a well-known attack vector: deserializing attacker-controlled bytes can trigger **gadget-chain remote code execution**. Never deserialize untrusted input to "clone" an object.

<details>
<summary>💻 Mitigation</summary>

```java
// If you must deserialize, restrict allowed classes (JEP 290, Java 9+):
ois.setObjectInputFilter(info ->
    isAllowed(info.serialClass()) ? ObjectInputFilter.Status.ALLOWED
                                  : ObjectInputFilter.Status.REJECTED);
```
</details>

For most cases, prefer explicit copy constructors; reserve serialization-clone for complex, trusted graphs where convenience outweighs the costs.
</details>

<details>
<summary><strong>Q13: [Trade-off] How does Prototype relate to the Flyweight pattern?</strong></summary>

They're **opposites in intent** but complementary. **Flyweight** *shares* a single immutable instance among many clients to **save memory** (interning identical objects). **Prototype** *copies* a configured instance to create many **independent** objects. Flyweight = sharing (one object, many references); Prototype = duplication (many independent objects).

They can coexist: an object might have an immutable, *shared* Flyweight part (intrinsic state) and a *cloned* mutable wrapper (extrinsic state) produced via Prototype. The interview tell: if the goal is "reduce memory by sharing identical objects," that's Flyweight; if it's "produce many independent copies cheaply," that's Prototype.
</details>

<details>
<summary><strong>Q14: [Trade-off] How does Prototype improve performance, and when does it *not*?</strong></summary>

It improves performance by doing **expensive initialization once** (building the prototype) and then producing further instances via **memory-to-memory copies** instead of repeating heavy work (DB reads, file/model loading, computation). For costly constructors cloned at high frequency, this is a large win — the whole justification of the pattern.

It does **not** help (and can hurt) when: construction is already cheap (copying adds overhead and code for no gain); the object is **immutable** (just share it — no copy needed); or the deep copy itself is expensive (a huge graph cloned per request can create more GC pressure than it saves). Measure: Prototype pays off only when copy cost ≪ construction cost.
</details>

<details>
<summary><strong>Q15: [Trade-off] Prototype vs. Memento — how do they differ?</strong></summary>

Both involve capturing object state, but for different purposes. **Memento** captures a snapshot of an object's state so it can be **restored later** — it's about *history and undo*, and the snapshot is typically not a usable standalone object, just a saved state. **Prototype** uses an object's current state to **create a new, independent instance** — it's about *duplication/multiplicity*.

Put simply: Memento is about *time* (go back to a previous state); Prototype is about *quantity* (make another one like this). A Memento is handed back to its originator to restore; a Prototype's copy is a fully functional peer object you use immediately. If the requirement is undo/checkpoints, reach for Memento; if it's "spawn many similar objects," reach for Prototype.
</details>

<details>
<summary><strong>Q16: [Advanced] How do you handle `final` fields when cloning?</strong></summary>

This is exactly where `Object.clone()` fails and copy constructors shine. `clone()` **bypasses the constructor**, so it can't assign `final` fields normally — you'd be forced to abandon `final` (losing immutability) or use reflection hacks. A **copy constructor** runs as a real constructor, so it can assign `final` fields directly from the source object.

<details>
<summary>💻 Code</summary>

```java
final class Money {
    private final long cents;             // final -> clone() can't set this cleanly
    private final String currency;
    Money(Money o) {                       // copy constructor sets finals normally
        this.cents = o.cents;
        this.currency = o.currency;
    }
}
```
</details>

So for objects with `final` fields (i.e., properly immutable objects), the copy-constructor approach isn't just preferred — it's essentially required, which is another reason *Effective Java* steers away from `Cloneable`.
</details>

<details>
<summary><strong>Q17: [Advanced] How does Prototype relate to distributed "state transfer" / serialization across nodes?</strong></summary>

Prototype is essentially the **in-memory, local analog of distributed state transfer**. When an object's state moves between nodes (serialized to Protobuf/JSON/Avro and reconstructed on the receiver), the receiving node effectively *clones* the object from the wire representation — it produces a new, independent instance with the same state, in a different JVM. That's a deep copy across a network boundary.

The connection is practical: the same **deep-copy discipline** (duplicate all mutable state, handle cycles, drop/rehydrate transient state) applies whether you're cloning in-process or across nodes, and serialization-based deep copy (V3) literally reuses the distributed transfer machinery locally. The Staff insight: think of "clone" and "serialize/deserialize round-trip" as the same operation at different scopes, with the same correctness (independence) and security (untrusted-input) concerns.
</details>

<details>
<summary><strong>Q18: [Advanced] How would you implement a copy-on-write "lazy prototype"?</strong></summary>

A copy-on-write (COW) prototype does a cheap **shallow copy** initially and defers the expensive **deep copy** of a field until it's actually **mutated**. Until a write happens, the clone shares the original's nested structures (fast, memory-light); on first mutation of a shared field, it copies that field so the write doesn't affect the original.

<details>
<summary>💻 Sketch</summary>

```java
class CowDoc {
    private List<String> pages;
    private boolean shared;                    // true right after a shallow copy
    CowDoc shallowCopy() {
        CowDoc c = new CowDoc(); c.pages = this.pages; c.shared = true; return c;
    }
    void addPage(String p) {
        if (shared) { pages = new ArrayList<>(pages); shared = false; }  // copy-on-write
        pages.add(p);
    }
}
```
</details>

This is how efficient immutable/persistent data structures and OS process forking work; it's a Prototype variant that trades eager copy cost for lazy, on-demand copying — powerful when most clones are read-mostly. (It's closely related to the Proxy/COW techniques.)
</details>

<details>
<summary><strong>Q19: [Advanced] Discuss the security implications of Prototype in Java.</strong></summary>

The main hazard is **serialization-based deep cloning of untrusted data**. Java deserialization instantiates objects and runs `readObject`/gadget code without invoking normal constructors, so a crafted byte stream can chain library "gadgets" into **remote code execution** — a class of CVEs that has hit many Java systems. Using `SerializationUtils.clone()`-style deep copy on attacker-controlled objects is dangerous.

Mitigations: prefer **explicit copy constructors** (no deserialization, no gadget surface); if you must deserialize, apply **`ObjectInputFilter`/JEP 290 allow-lists** to restrict instantiable classes; avoid making sensitive classes `Serializable`; and validate invariants after copying. There's also a subtler point: because `clone()` bypasses constructors, a malicious subclass overriding `clone()` can violate invariants your constructor would enforce — another reason to make classes `final` or use copy constructors. Security-critical code should deep-copy via explicit, constructor-based means, never a serialization round-trip on untrusted input.
</details>

<details>
<summary><strong>Q20: [Trade-off] How can Prototype eliminate the "class explosion" that factories can cause?</strong></summary>

With a factory-per-variant approach, many slightly-different configurations become many classes: `RedCircleFactory`, `BlueCircleFactory`, `BigRedCircleFactory`, … — an explosion driven by *configuration*, not behavior. Prototype collapses this: you define **one** `Circle` class, instantiate and configure one prototype per needed variant at runtime, store them in a `Map<String, Circle>`, and **clone** the right one on demand.

<details>
<summary>💻 Code</summary>

```java
Map<String, Circle> prototypes = new HashMap<>();
prototypes.put("red-big",  new Circle(Color.RED, 100));   // configured instances, not classes
prototypes.put("blue-sm",  new Circle(Color.BLUE, 10));
Circle c = prototypes.get("red-big").copy();               // clone to get a new one
```
</details>

Variants become *data* (configured instances) instead of *code* (subclasses/factories), which is far more maintainable and lets you add variants at runtime without new classes.
</details>

### 🏛️ Staff / Principal Engineer Deep-Dive

<details>
<summary><strong>SP1: When is Prototype the wrong choice, and what deeper design issue can it mask?</strong></summary>

Prototype is wrong when construction is cheap (copying adds cost and code for nothing), when the object is immutable (share it — cloning is pointless), or when the object graph is deep/circular/externally-owned such that correct deep copying is fragile and expensive. The deeper smell it can mask: reaching for cloning because objects are **large, mutable, and entangled** — Prototype makes duplicating that mess *tolerable* and removes the pressure to fix it. Often the real answer is **immutability + sharing** (if objects didn't have mutable state, you wouldn't need deep copies at all) or **better decomposition** (a smaller mutable core cloned cheaply, with immutable parts shared). Another masked issue is using clone to snapshot state that really wants a **Memento** (for undo) or a proper **persistence/versioning** mechanism. The Staff move is to ask *why* you're copying: if it's to isolate mutable state, consider whether that state should exist at all, or whether value objects/immutability would eliminate the need. Prototype should optimize legitimate expensive construction, not paper over a mutable, over-shared object model.
</details>

<details>
<summary><strong>SP2: How does Prototype interact with garbage collection and memory in high-throughput systems?</strong></summary>

Two opposing effects. Prototype *reduces* pressure when construction creates many short-lived temporaries (parsing config, building maps) — doing that once for the prototype and cloning avoids repeating the churn. But deep cloning *creates* pressure: each clone allocates fresh copies of every mutable field, so high-frequency cloning of large graphs floods the young generation and increases minor-GC frequency and allocation-rate-driven pauses. The Staff calculus is to measure allocation per clone vs. per construction and per operation budget. Mitigations: **copy-on-write** so unmutated fields stay shared (allocate only on write); **share immutable sub-objects** (don't deep-copy what can't change — e.g., interned strings, immutable value objects); **object pooling** for the cloned instances if lifecycle allows; and choosing a **copy constructor** over serialization (which allocates byte buffers and intermediate objects). In the extreme (HFT-style), you might abandon per-message clones for a reused mutable scratch object. The principle: deep copy only the mutable, per-instance state; everything immutable should be shared, not copied.
</details>

<details>
<summary><strong>SP3: How would you design a robust deep-copy strategy for a large, evolving domain model?</strong></summary>

Standardize the mechanism and make correctness maintainable as the model grows. (1) **Copy constructors / `copyOf` factories** as the canonical approach (not `Cloneable`), one per class, each responsible for deep-copying its own mutable fields — so copy logic lives next to the fields it copies and evolves with them. (2) **Prefer immutability** so many objects need no copying at all (immutable value objects are shared freely), shrinking the deep-copy surface. (3) For **cyclic/shared graphs**, provide a graph-copy utility that takes an `IdentityHashMap` of visited nodes, and route complex copies through it. (4) **Guard against field drift** — the classic bug is adding a mutable field and forgetting to copy it; mitigate with tests that assert independence (mutate the copy, assert the original is unchanged) and, where feasible, reflection-based or generated copy code (Lombok, MapStruct) so new fields are handled automatically. (5) Be explicit about **transient/derived state** (caches, listeners) — decide per field whether a copy shares, recomputes, or drops it. (6) For distributed boundaries, align the in-memory copy semantics with the serialization schema so local clone and wire round-trip behave identically. The Staff goal is that "how do we copy X?" has one documented answer, and adding a field can't silently reintroduce a shared-state bug.
</details>

<details>
<summary><strong>SP4: Contrast a prototype registry with a DI container's prototype scope for producing fresh instances.</strong></summary>

Both produce a fresh instance per request rather than a shared singleton, but the *mechanism and cost model* differ. A **DI container's prototype scope** (Spring `@Scope("prototype")`) **constructs a new object each time** through the normal wiring/instantiation path — full construction cost per request, but always a clean object built from current configuration. A **prototype registry** **copies a pre-built configured instance**, so it amortizes expensive setup (do it once for the master, clone thereafter) — cheaper when construction is heavy, at the cost of getting deep-copy correctness right and holding masters in memory. Choose the container's prototype scope when construction is cheap and you want the container to manage dependencies/lifecycle; choose a prototype registry when construction is **expensive** and copying is materially cheaper, or when you need runtime-configurable variants (game assets, message templates) that would be awkward as beans. Note the container doesn't manage the full lifecycle of prototype-scoped beans (no destruction callbacks), similar to how a registry doesn't track the copies it hands out. In practice teams sometimes combine them: a singleton bean *is* the prototype registry, and it clones configured masters to serve requests.
</details>

<details>
<summary><strong>SP5: How do you make cloning safe and correct in an inheritance hierarchy?</strong></summary>

Cloning across a hierarchy is where `Object.clone()` is most treacherous and copy constructors have a known limitation, so you design deliberately. The `clone()` route's hazard: if a superclass's `clone()` does `super.clone()` and returns the right runtime type, subclasses *seem* to inherit cloning, but any subclass that adds mutable fields **must** override `clone()` to deep-copy them — miss it and you get shared state; and a subclass can't easily call a superclass copy constructor to fill inherited `final` fields. The robust designs: (1) provide an **abstract `copy()`** (or a `Prototype<T>` contract) that every concrete subclass must implement, forcing each to deep-copy its own fields — the compiler reminds you. (2) Use **copy constructors that chain**: `Sub(Sub o) { super(o); this.extra = deepCopy(o.extra); }`, so each level copies its own state and inherited state via the superclass copy constructor. (3) Consider making leaf classes **`final`** to avoid the "subclass forgot to extend clone" trap, or use a **`self-type`/CRTP** style for typed copies. (4) Add **independence tests per subclass**. The Staff principle: make "every subclass must define how it copies its own new state" an *enforced* contract (abstract method or chained copy constructor), never an implicit expectation — that's what prevents the silent shared-state regressions inheritance invites.
</details>

---

## ⚡ Quick Revision

**One-liner:** Prototype creates new objects by **copying a pre-configured instance** (via `clone()`/copy constructor) instead of constructing from scratch — cheap when `new` is expensive, and it avoids a subclass per configuration.

**The whole pattern in a paragraph:** When constructing an object is costly (heavy I/O, DB/API, complex setup) or would demand a subclass/factory per minor variant, keep one fully-configured **prototype** and produce further instances by **copying** it, so the client clones through an interface without knowing the concrete class. The entire practical difficulty is **copy depth**: `Object.clone()` does a **shallow** field-by-field copy, so reference fields (lists/maps/sub-objects) are **shared** — the classic bug where mutating a "copy" corrupts the original — whereas a correct prototype does a **deep copy**, recursively duplicating all mutable state so the copy is independent. Java's `Cloneable` is **broken** (a method-less marker interface, `protected` shallow `clone()`, checked exception, bypasses constructors so `final` fields/invariants break), so *Effective Java* Item 13 says prefer **copy constructors / copy factories** — they work with `final` fields, run validation, and need no casting. For arbitrary graphs, **serialization** gives one-shot deep copy (auto-handles cycles) but is slow, needs `Serializable`, drops `transient`, and is a **deserialization security risk** on untrusted input. **Circular references** need an identity map to avoid `StackOverflowError`. The production form is a **prototype registry** (often a Singleton, `ConcurrentHashMap`-backed) that caches expensive gold-copy masters and hands out **copies**. Prototype is the **inverse of Flyweight** (copy vs. share), the **opposite of Singleton** (many vs. one), and distinct from **Memento** (copy-to-create vs. snapshot-to-restore). Library examples: `ArrayList.clone()` (shallow!), Spring `@Scope("prototype")`, Apache Commons `SerializationUtils.clone()`.

**Top 5 interview answers to memorize:**

1. **"Shallow vs. deep copy?"** → Shallow copies the object but shares references to nested mutable fields (they stay linked); deep recursively duplicates the whole graph so the copy is fully independent.
2. **"When Prototype over Factory?"** → When construction is expensive (clone is cheaper) or to avoid a subclass per configuration, and to copy objects without knowing their concrete class.
3. **"Why avoid `Object.clone()`/`Cloneable`?"** → Marker interface with no method, `clone()` is `protected` + shallow + throws a checked exception + bypasses the constructor (breaks `final` fields); prefer copy constructors (Effective Java Item 13).
4. **"How to handle circular references in deep copy?"** → Track already-copied objects in an `IdentityHashMap`; on revisiting a node, return its existing copy instead of recursing.
5. **"What's the prototype registry for?"** → A cache of pre-configured master prototypes; `registry.get(key).copy()` returns a fresh independent instance, amortizing expensive setup — and it must always return copies, never the master.

**Trigger words** (if the interviewer says these, think Prototype): *"costly/expensive to construct," "copy/clone an object," "shallow vs deep copy," "avoid subclass explosion," "pre-configured / template instance," "spawn many similar objects," "clone and tweak," "registry of templates," "copy constructor," "deep copy with cycles."*



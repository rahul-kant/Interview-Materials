# Singleton Pattern ⭐⭐⭐ (Difficulty: 3/5 — the *idea* is one line, but thread-safety, the Java Memory Model, serialization, reflection, classloaders, and distributed "one instance" are a bottomless well interviewers love)

> **Category:** Creational Pattern (GoF)
> **Also known as:** Single Instance

The Singleton pattern **guarantees that a class has exactly one instance for the lifetime of a process and hands every caller a shared reference to that same instance**. It bundles two responsibilities into one class: *controlling its own instantiation* (nobody can `new` it) and *providing a global access point* (`getInstance()`). It's the pattern behind loggers, configuration holders, connection pools, caches, thread pools, and metric registries — and it's simultaneously the most *asked-about* and most *criticized* pattern in the GoF book, because a naive implementation is broken under concurrency and the "global access" it provides is easily abused into hidden global state.

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 0: Public Constructor / Global Variable (Anti-pattern)](#variant-0-public-constructor--global-variable-anti-pattern)
   - [Variant 1: Lazy, Non-Thread-Safe Singleton](#variant-1-lazy-non-thread-safe-singleton)
   - [Variant 2: Synchronized `getInstance()`](#variant-2-synchronized-getinstance)
   - [Variant 3: Double-Checked Locking with `volatile`](#variant-3-double-checked-locking-with-volatile)
   - [Variant 4: Eager Initialization](#variant-4-eager-initialization)
   - [Variant 5: Bill Pugh — Initialization-on-Demand Holder (Best for lazy classes)](#variant-5-bill-pugh--initialization-on-demand-holder-best-for-lazy-classes)
   - [Variant 6: Enum Singleton (Best overall — Joshua Bloch's recommendation)](#variant-6-enum-singleton-best-overall--joshua-blochs-recommendation)
5. [🎨 Real-World Example](#-real-world-example)
   - [Scenario 1 — Application Logger](#scenario-1--application-logger)
   - [Scenario 2 — Database Connection Pool](#scenario-2--database-connection-pool)
   - [Scenario 3 — Shared AWS SDK Client (S3)](#scenario-3--shared-aws-sdk-client-s3)
   - [Scenario 4 — Shared HTTP Client (OkHttp)](#scenario-4--shared-http-client-okhttp)
   - [Scenario 5 — Shared Redis Client (Lettuce)](#scenario-5--shared-redis-client-lettuce)
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

> **GoF Definition:** *"Ensure a class has only one instance, and provide a global point of access to it."*

The Singleton exists to solve a specific need: some objects should exist **exactly once** in a process because they represent a single real resource or a single logical authority — a file-backed logger, the one configuration loaded at startup, the single pool that owns a fixed set of database connections, the one in-memory cache. Creating a second instance would be wrong: two loggers fighting over one file, two config objects disagreeing on a value, two pools each opening the "max" connections and blowing past the database limit.

The pattern's insight is to **make the class itself responsible for enforcing its own uniqueness** rather than trusting every caller to cooperate. It does this with two moves: (1) **hide the constructor** so no external code can create instances, and (2) **expose a single static accessor** (`getInstance()`) that lazily or eagerly creates *the one* instance and returns that same reference forever after. The class controls creation; callers get access without ever seeing a constructor.

The subtle part — and the reason this "trivial" pattern fills interview loops — is that "exactly one" is a *concurrency*, *serialization*, *reflection*, and even *classloader* guarantee. A correct Singleton must survive many threads racing into `getInstance()` at once, must not silently multiply when serialized/deserialized, must resist reflective and clone-based attacks, and must be clear about the fact that "one per JVM" is **not** "one per cluster."

---

## 🎯 Problem

You have a class where a second instance is either wasteful or outright incorrect, and you want a single, well-known access point to it. The naive approaches — a plain global variable, or letting each caller `new` its own copy and "just agree" to share — fail: globals can be reassigned and offer no lazy creation, and voluntary sharing is unenforceable (any code can `new` another). You need the *class* to guarantee uniqueness, not a convention.

**The pain points that lead you to Singleton:**

- **Duplicate instances are incorrect, not just wasteful.** Two connection pools each open `MAX` connections → you exceed the database's connection cap. Two caches → cache misses and inconsistency. Two config objects → different parts of the app read different values.
- **You need a single global access point.** Many unrelated modules need the *same* logger/config/cache, and threading it through every constructor is impractical (though, as we'll discuss, dependency injection is often the *better* answer).
- **Creation is expensive and should happen once.** Reading a config file, opening sockets, warming a cache — do it a single time and reuse.
- **Uniqueness must survive concurrency.** In a multithreaded server, dozens of threads may call `getInstance()` on the very first request; a naive check-then-create races and produces multiple instances.

**Concrete example scenarios:**

1. **Logger.** The whole application logs through one logger that owns a file handle / async appender. A second logger instance could interleave writes or double-open the file. `Logger.getInstance().info(...)` is called from everywhere.

2. **Configuration manager.** Settings loaded once from a file/environment at startup and read everywhere. There must be *one* source of truth; a second load could see a different file state.

3. **Database connection pool.** The pool owns a fixed, bounded set of physical connections. Exactly one pool must exist, or the "max connections" invariant is violated and the DB rejects connections.

4. **In-memory cache / thread pool / metrics registry.** One shared cache so all readers/writers see the same data; one shared thread pool so you don't accidentally spawn thousands of threads; one metrics registry so counters aggregate correctly.

---

## ✅ Solution

The core idea, in plain language: **take away the caller's ability to create the object, and give the class a static method that owns creation and always returns the same instance.** The class becomes the guardian of its own cardinality.

**Key structural elements:**

- **Private (or hidden) constructor.** Prevents any external `new`. This is the linchpin — without it, "exactly one" is unenforceable.
- **A static field holding the single instance.** Either created eagerly at class load (`static final ... = new ...`) or lazily on first access. `static` ties the instance to the class (one per classloader), not to any caller.
- **A static accessor `getInstance()`.** The single global access point. It creates the instance if needed (lazy) or just returns the pre-built one (eager), and it must be **thread-safe** if creation is lazy.
- **(For robustness) defenses against the back doors:** `readResolve()` for serialization, throwing in the constructor (or using an enum) against reflection, and forbidding `clone()`.

**The mechanism that makes it work:** *the private constructor closes the front door, and the static accessor is the only door left open.* Because the field is `static`, it belongs to the class object itself, and the JVM guarantees a class is initialized **once** and in a **thread-safe** way by the classloader (this is the deep reason the best lazy implementations — Bill Pugh's holder and enum — need no explicit synchronization: they piggyback on the JVM's class-initialization lock). The whole game in the implementation variants below is *how* you create that one instance safely and lazily under concurrency, and how you seal the remaining back doors (serialization, reflection, cloning) that can smuggle a second instance into existence.

The **central trade-off**, which every strong answer must name, is that a Singleton is **global mutable state wearing an object-oriented costume**. The uniqueness guarantee is genuinely useful; the "global access point" is genuinely dangerous — it creates hidden dependencies (any method can reach the singleton without declaring it), makes unit testing hard (shared state leaks between tests, hard to mock), and can violate the Single Responsibility Principle (the class does its job *and* manages its own lifecycle). The mature view: prefer **one instance managed by a dependency-injection container** (Spring `@Bean` singleton scope) over a hand-rolled `getInstance()`, reserving the classic pattern for when a framework isn't available.

---

## 💻 Implementation

We'll implement the same class — a `ConfigManager` that loads settings once — seven ways, from a broken global to the two production-grade forms (Bill Pugh holder and enum). Read them top-to-bottom as one story: each variant fixes a specific defect of the previous one. The recurring theme is **how to create the one instance safely and lazily under concurrency**, and the recurring mechanism is the **Java Memory Model** and the **JVM's guaranteed-once, thread-safe class initialization**.

### Variant 0: Public Constructor / Global Variable (Anti-pattern)

**What's wrong with it:** This is the disease Singleton cures. With a public constructor (or a plain mutable `public static` field), nothing stops any caller from creating a second instance, and a `public static` reference can be reassigned to `null` or to a different object at any time. There is no enforcement of "exactly one" — uniqueness is a *hope*, not a *guarantee*. Any refactor or careless teammate breaks the invariant silently.

<details>
<summary>💻 Click to expand code — the anti-pattern (no enforcement)</summary>

```java
// DON'T DO THIS — nothing enforces a single instance.
public class ConfigManagerBad {
    // Public + mutable: any code can reassign or null it.
    public static ConfigManagerBad INSTANCE = new ConfigManagerBad();

    public ConfigManagerBad() {              // public constructor: anyone can make more
        // expensive load...
    }
}

// Elsewhere, all of these compile and "work":
ConfigManagerBad a = ConfigManagerBad.INSTANCE;
ConfigManagerBad b = new ConfigManagerBad();     // a second instance — invariant broken
ConfigManagerBad.INSTANCE = null;                // global reference nuked
```
</details>

**Pros:** Trivially simple; no ceremony.
**Cons:** No uniqueness guarantee, no encapsulation of creation, mutable global reference, no lazy control. It's a global variable, not a Singleton.
**Mechanism (absent):** There is no private constructor, so the front door is wide open.

### Variant 1: Lazy, Non-Thread-Safe Singleton

**What problem it solves vs. V0:** It closes the front door — the constructor is `private`, so no external `new` — and adds lazy creation (the instance is built on first `getInstance()`, not before). This is the "textbook" first cut and is correct in a **single-threaded** context.

**What's still wrong:** The `if (instance == null) { instance = new ... }` is a **check-then-act race**. Two threads can both evaluate `instance == null` as true before either assigns, and you get **two instances** — the exact thing the pattern must prevent. Fine for single-threaded code; broken in any concurrent server.

<details>
<summary>💻 Click to expand code — lazy, not thread-safe</summary>

```java
public class ConfigManager {
    private static ConfigManager instance;      // not volatile, not initialized
    private final Map<String, String> settings;

    private ConfigManager() {                    // private: front door closed
        settings = loadFromDisk();               // pretend this is expensive
    }

    public static ConfigManager getInstance() {
        if (instance == null) {                  // RACE: two threads can both see null
            instance = new ConfigManager();      // ...and both create an instance
        }
        return instance;
    }

    public String get(String key) { return settings.get(key); }
    private Map<String, String> loadFromDisk() { return new HashMap<>(); }
}
```
</details>

**Pros:** Lazy; simple; enforces the private constructor.
**Cons:** **Not thread-safe** — concurrent first calls can create multiple instances. Also vulnerable to reflection/serialization (like all class-based variants).
**Mechanism:** Private constructor enforces uniqueness *only* against the `new` back door, not against the concurrency race.

### Variant 2: Synchronized `getInstance()`

**What problem it solves vs. V1:** Wrapping the whole accessor in `synchronized` makes the check-then-act **atomic** — only one thread at a time enters the method, so the race is gone and you get exactly one instance. This is *correct*.

**What's still wrong:** Every single call acquires the monitor lock, even after the instance exists (which is 99.999% of calls). Locking is only needed *once* — during creation — but you pay for it forever. On a hot path called by many threads, this serializes all callers on one lock and hurts throughput. It's correct but not performant.

<details>
<summary>💻 Click to expand code — synchronized method</summary>

```java
public class ConfigManager {
    private static ConfigManager instance;

    private ConfigManager() { /* expensive load */ }

    // synchronized makes check-then-act atomic — correct, but every call locks.
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
}
```
</details>

**Pros:** Thread-safe; still lazy; simple to reason about.
**Cons:** Lock acquired on *every* call, not just creation → contention and reduced throughput on hot paths.
**Mechanism:** `synchronized` establishes mutual exclusion + a happens-before relationship, so the write to `instance` is visible to the next thread. The waste is that the lock is held even when no write happens.

### Variant 3: Double-Checked Locking with `volatile`

**What problem it solves vs. V2:** It removes the per-call locking. Check `instance == null` *without* a lock (the common, fast path); only if it's null do you enter the `synchronized` block, then **check again** inside the lock before creating. After the instance exists, callers take the lock-free fast path. Best of both: lazy + thread-safe + no lock on the hot path.

**The critical detail — `volatile`:** the field **must** be `volatile`, and this is the single most-tested subtlety in Singleton interviews. `instance = new ConfigManager()` is **not atomic**; it's roughly three steps: (1) allocate memory, (2) run the constructor, (3) publish the reference into `instance`. The JIT/CPU are allowed to **reorder** (2) and (3). Without `volatile`, Thread B running the lock-free first check could observe a **non-null but not-yet-constructed** object (step 3 done before step 2) and return a **half-initialized instance** — a heisenbug that reads uninitialized fields. `volatile` forbids that reordering and guarantees visibility (a happens-before edge), so a non-null read is guaranteed to see a *fully constructed* object. (Note: this was actually *broken* before Java 5's revised Java Memory Model — pre-JMM `volatile` didn't give the needed ordering, which is why DCL had a bad reputation historically.)

<details>
<summary>💻 Click to expand code — double-checked locking</summary>

```java
public class ConfigManager {
    // volatile is MANDATORY — prevents seeing a half-constructed object.
    private static volatile ConfigManager instance;

    private ConfigManager() { /* expensive load */ }

    public static ConfigManager getInstance() {
        if (instance == null) {                          // 1st check: lock-free fast path
            synchronized (ConfigManager.class) {         // lock only on first-time creation
                if (instance == null) {                  // 2nd check: another thread may have created it
                    instance = new ConfigManager();      // publish (volatile write)
                }
            }
        }
        return instance;                                 // volatile read: sees fully-built object
    }
}
```
</details>

**Pros:** Lazy, thread-safe, and **no lock after creation** — fast on the hot path.
**Cons:** Verbose and easy to get wrong (forget `volatile` → subtle heisenbug; forget the second check → multiple instances). Most engineers prefer the holder idiom (V5) which achieves the same for free.
**Mechanism:** `volatile` gives an acquire/release memory barrier: the write in the constructor *happens-before* the write to `instance`, and a non-null `volatile` read *happens-after* that write — so no thread can see a partially constructed object, and the reordering of "publish" before "construct" is forbidden.

### Variant 4: Eager Initialization

**What problem it solves vs. V1–V3:** Sidesteps *all* the concurrency complexity by creating the instance at **class-load time** in a `static final` field. Because the JVM guarantees class initialization runs **once** and is **thread-safe** (protected by an internal initialization lock), the instance is fully built before any thread can call `getInstance()`. No `synchronized`, no `volatile`, no DCL — and `final` guarantees safe publication.

**What's the drawback:** It's **eager** — the instance is created whether or not it's ever used, and it's created when the class is *loaded/initialized* (which for many classes is early), so you can't defer expensive initialization and can't easily pass constructor parameters. If creation is cheap and always needed, this is a great, simple choice; if it's expensive and possibly unused, you want laziness (V5/V6).

<details>
<summary>💻 Click to expand code — eager initialization</summary>

```java
public class ConfigManager {
    // Created once at class initialization; final => safe publication, no races.
    private static final ConfigManager INSTANCE = new ConfigManager();

    private ConfigManager() { /* load */ }

    public static ConfigManager getInstance() {
        return INSTANCE;                 // no locking, no null check
    }
}
```
</details>

**Pros:** Simplest thread-safe form; no synchronization; `final` guarantees visibility.
**Cons:** Not lazy — built even if never used; created at class-init time; awkward if construction can fail or needs runtime parameters.
**Mechanism:** JVM class initialization is guaranteed by the JLS to be **serialized and run exactly once**; `static final` fields set during `<clinit>` are safely published to all threads. Uniqueness and thread-safety come for free from the classloader.

### Variant 5: Bill Pugh — Initialization-on-Demand Holder (Best for lazy classes)

**What problem it solves vs. V3 and V4:** It gets V4's *effortless* thread-safety **and** V1/V3's *laziness* — with zero synchronization code. The trick: put the instance in a **private static nested class** (the "holder"). A nested class is **not initialized until it's first referenced**. Since the only reference is inside `getInstance()`, the holder — and thus the instance — is created **lazily, on the first `getInstance()` call**, and the JVM's guaranteed-once, thread-safe class-initialization mechanism makes it thread-safe with no locks.

**Why it's the idiomatic class-based choice:** No `volatile`, no `synchronized`, no double-check boilerplate, no per-call cost, *and* lazy. It's the recommended hand-written Singleton for classes that can't be enums (e.g., must extend a base class, or need generics).

<details>
<summary>💻 Click to expand code — Bill Pugh holder idiom</summary>

```java
public class ConfigManager {

    private ConfigManager() { /* expensive load */ }

    // Not loaded until Holder is first referenced (inside getInstance).
    private static class Holder {
        private static final ConfigManager INSTANCE = new ConfigManager();
    }

    public static ConfigManager getInstance() {
        return Holder.INSTANCE;          // triggers Holder init: lazy + thread-safe, lock-free
    }
}
```
</details>

**Pros:** Lazy **and** thread-safe with **no synchronization** and **no per-call overhead**; simple to read; works when enum isn't an option.
**Cons:** Slightly less obvious to newcomers; still needs `readResolve()`/reflection guards if you want serialization/reflection safety (an enum gets those for free).
**Mechanism:** The JLS says a class is initialized *the first time it is actively used* (here, the first read of `Holder.INSTANCE`), and initialization is thread-safe (guarded by the JVM's class-init lock) and happens exactly once. Laziness comes from *deferred class loading of the nested class*; safety comes from the *class-initialization guarantee* — no application-level locking required.

### Variant 6: Enum Singleton (Best overall — Joshua Bloch's recommendation)

**What problem it solves vs. all above:** It's the only variant that is **thread-safe, serialization-safe, and reflection-safe by construction**, in a few lines. Enum constants are instantiated once by the JVM (class-init guarantee, like V4/V5), serialization of enums is handled specially by the JVM (it serializes the *name* and resolves back to the existing constant — so deserialization returns the same instance, no `readResolve()` needed), and the reflective `Constructor.newInstance()` **explicitly throws `IllegalArgumentException` for enum types** — so the reflection back door is sealed at the language level.

**Trade-offs:** It's **eager** (constants created at enum-class init, so not lazy), it **cannot extend** another class (enums implicitly extend `java.lang.Enum`), and some find "a value that's really a service" stylistically odd. But for the common case, *Effective Java* (Item 3) calls it "the best way to implement a singleton."

<details>
<summary>💻 Click to expand code — enum singleton</summary>

```java
public enum ConfigManager {
    INSTANCE;                                   // the single instance, created once by the JVM

    private final Map<String, String> settings;

    ConfigManager() {                           // runs once, at enum class initialization
        settings = loadFromDisk();
    }

    public String get(String key) { return settings.get(key); }
    private Map<String, String> loadFromDisk() { return new HashMap<>(); }
}

// Usage:
String v = ConfigManager.INSTANCE.get("timeout");
```
</details>

**Pros:** Thread-safe, **serialization-safe and reflection-safe for free**, extremely concise. The recommended default when you don't need laziness or inheritance.
**Cons:** Eager (not lazy); can't extend a class; can feel unidiomatic for stateful services; awkward if construction needs runtime arguments.
**Mechanism:** Enum instances are `public static final` fields initialized in the enum's `<clinit>` — one-time, thread-safe class init. The JVM's serialization spec handles enums specially (by name), and `Constructor.newInstance` special-cases enums to throw — so *both* the serialization and reflection back doors are closed by the platform, not by your code.

---

## 🎨 Real-World Example

Five complete, production-shaped examples, each fully collapsible. The first is the canonical **application logger** using the Bill Pugh holder idiom, with a concurrency test that proves a single instance under contention. The second is a **bounded database connection pool** — the scenario where "exactly one" is a *correctness* requirement (a second pool would double the physical connections and violate the DB's cap). The last three are the most common *real* singletons you'll build at a FAANG shop — **shared, heavyweight network clients** (AWS SDK, HTTP, Redis) whose duplication silently exhausts connection pools, sockets, and threads — each shown separately with its own demo. Every scenario ends with a walkthrough of *why the code is shaped the way it is*.

### Scenario 1 — Application Logger

<details>
<summary>💻 Click to expand — Logger singleton + concurrency demo + explanation</summary>

A thread-safe, lazily-initialized logger shared across the whole app. It uses the **Bill Pugh holder** (lazy + lock-free), guards the reflection back door in the constructor, and demonstrates that many threads hammering `getInstance()` all receive the *same* object.

```java
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Production-style singleton logger: lazy, thread-safe, lock-free on the hot path. */
public final class Logger {

    // A single, thread-safe sink for log lines. In real code this wraps a file/async appender.
    private final BlockingQueue<String> sink = new LinkedBlockingQueue<>();
    private final AtomicInteger written = new AtomicInteger();

    private Logger() {
        // Reflection guard: if someone reflectively calls the constructor after init, fail loudly.
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("Use Logger.getInstance()");
        }
    }

    // Holder is not initialized until getInstance() is first called -> lazy + thread-safe.
    private static class Holder {
        private static final Logger INSTANCE = new Logger();
    }

    public static Logger getInstance() {
        return Holder.INSTANCE;
    }

    public void log(String level, String msg) {
        String line = Instant.now() + " [" + level + "] "
                + "[" + Thread.currentThread().getName() + "] " + msg;
        sink.offer(line);
        written.incrementAndGet();
    }

    public void info(String msg)  { log("INFO", msg); }
    public void error(String msg) { log("ERROR", msg); }

    public int writtenCount() { return written.get(); }
}
```

```java
/** Demo: prove a single instance under heavy concurrency + exercise many call sites. */
public class LoggerDemo {
    public static void main(String[] args) throws InterruptedException {
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentHashMap<Integer, Boolean> identities = new ConcurrentHashMap<>();
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final int id = i;
            pool.submit(() -> {
                try {
                    start.await();                              // release all threads at once
                    Logger log = Logger.getInstance();          // racing first calls
                    identities.put(System.identityHashCode(log), true);
                    log.info("hello from task " + id);
                } catch (InterruptedException ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();                                      // GO
        done.await();
        pool.shutdown();

        // Multiple call sites from the main thread:
        Logger.getInstance().error("startup complete");
        Logger.getInstance().info("shutting down");

        System.out.println("Distinct Logger instances seen: " + identities.size()); // -> 1
        System.out.println("Total log lines written:       " + Logger.getInstance().writtenCount()); // -> 52
        // Distinct instances == 1 proves the singleton held under 50-way contention.
    }
}
```

**Code explanation.** The `Logger` uses the **Bill Pugh holder idiom**: the nested `Holder` class isn't initialized until `getInstance()` first touches `Holder.INSTANCE`, so creation is **lazy**, and the JVM's guaranteed-once, thread-safe class-initialization makes it **thread-safe with no locks** — no `synchronized`, no `volatile`, no per-call cost. The private constructor throws if `Holder.INSTANCE` is already set, closing the **reflection back door**. Internally the logger holds a `BlockingQueue` sink and an `AtomicInteger` counter, so the *shared* instance is safe to hammer from many threads. The demo is the important part: 50 threads are released simultaneously via a `CountDownLatch` and all race into `getInstance()`; each records `System.identityHashCode` of what it got into a `ConcurrentHashMap`. Because the set of distinct identities prints `1`, we've *proven* the singleton held under maximal contention — the exact property a naive lazy singleton would fail. The write count (`52` = 50 threads + 2 main-thread calls) confirms every call site shared one logger.

</details>

### Scenario 2 — Database Connection Pool

<details>
<summary>💻 Click to expand — bounded connection pool singleton + demo + explanation</summary>

Here "exactly one" is non-negotiable: the pool owns a *bounded* set of physical connections. Two pools would each open `MAX_CONNECTIONS`, blowing past the database's limit. Uses the holder idiom, blocks (with timeout) when exhausted, and is fully thread-safe.

```java
import java.util.concurrent.*;

/** One pool per JVM guarding a fixed number of physical connections. */
public final class ConnectionPool {
    private static final int MAX_CONNECTIONS = 10;

    // Bounded blocking queue = the pool + built-in back-pressure when exhausted.
    private final BlockingQueue<Connection> available;

    private ConnectionPool() {
        available = new ArrayBlockingQueue<>(MAX_CONNECTIONS);
        for (int i = 0; i < MAX_CONNECTIONS; i++) {
            available.offer(new Connection("conn-" + i));   // pretend: open a real socket
        }
    }

    private static class Holder {
        private static final ConnectionPool INSTANCE = new ConnectionPool();
    }

    public static ConnectionPool getInstance() {
        return Holder.INSTANCE;
    }

    /** Borrow a connection, waiting up to `timeoutMs` if the pool is drained. */
    public Connection acquire(long timeoutMs) throws InterruptedException {
        Connection c = available.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (c == null) throw new IllegalStateException("Pool exhausted; try later");
        return c;
    }

    /** Always return connections in a finally block. */
    public void release(Connection c) {
        if (c != null) available.offer(c);
    }

    public int idleCount() { return available.size(); }

    /** Stand-in for a real JDBC Connection. */
    public static final class Connection {
        private final String id;
        Connection(String id) { this.id = id; }
        public String id() { return id; }
    }
}
```

```java
public class PoolDemo {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService workers = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 20; i++) {
            workers.submit(() -> {
                ConnectionPool pool = ConnectionPool.getInstance();  // same pool for all
                ConnectionPool.Connection c = null;
                try {
                    c = pool.acquire(1000);
                    Thread.sleep(50);                                // simulate query
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                } finally {
                    pool.release(c);                                 // never leak connections
                }
            });
        }
        workers.shutdown();
        workers.awaitTermination(5, TimeUnit.SECONDS);
        // After all work: every connection returned to the single pool.
        System.out.println("Idle connections back in pool: " + ConnectionPool.getInstance().idleCount()); // -> 10
    }
}
```

**Code explanation.** This is the scenario where "exactly one" is a hard **correctness** invariant, not an optimization: the pool pre-opens `MAX_CONNECTIONS` physical connections into an `ArrayBlockingQueue`, so a *second* pool would open another 10 and blow past the database's connection cap. The **holder idiom** guarantees the single pool, and the `ArrayBlockingQueue` does double duty — it's both the storage of idle connections and the **back-pressure mechanism**: `acquire(timeoutMs)` uses `poll(timeout)` so a caller blocks briefly when the pool is drained and fails cleanly instead of opening a rogue connection. The `release()` in a `finally` is the critical discipline — connections must return to the queue even on exception, or the pool leaks capacity until it's empty. The demo runs 20 workers against a 10-connection pool: they contend, some briefly wait, and after all finish the idle count is back to `10`, proving no connection was lost or duplicated. The takeaway: Singleton + a bounded blocking queue is the canonical resource-pool shape.

</details>

### Scenario 3 — Shared AWS SDK Client (S3)

<details>
<summary>💻 Click to expand — shared AWS S3 client singleton + demo + explanation</summary>

SDK clients like `S3Client`/`DynamoDbClient` (AWS SDK v2) are **expensive, heavyweight, and thread-safe by design** — each owns an HTTP connection pool, I/O threads, TLS state, DNS caches, and a credential provider. AWS's own guidance is to **build one client and reuse it** for the app's lifetime. Creating a fresh client per request is a classic production incident: you exhaust file descriptors/sockets, thrash connection pools, and pay TLS-handshake latency on every call — invisible in a load test, fatal under real traffic. The fix is a lazily-initialized holder that builds the client once and registers a **shutdown hook** to `close()` it cleanly.

```java
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * One S3Client for the whole JVM. S3Client is thread-safe and expensive
 * (HTTP connection pool + I/O threads + credential provider). AWS's own guidance:
 * "build the client once and reuse it." Duplicating it exhausts sockets/threads.
 */
public final class S3ClientProvider {

    private S3ClientProvider() {}   // no instances of the provider itself

    // Holder: the client is built lazily on first getClient(), thread-safe & lock-free.
    private static class Holder {
        private static final S3Client CLIENT = build();

        private static S3Client build() {
            S3Client client = S3Client.builder()
                    .region(Region.US_EAST_1)
                    // .credentialsProvider(DefaultCredentialsProvider.create())  // default chain
                    // .httpClientBuilder(...)  // tune the shared connection pool here
                    .build();
            // CRITICAL: close the client (and its connection/thread pools) on JVM shutdown.
            Runtime.getRuntime().addShutdownHook(new Thread(client::close, "s3-client-closer"));
            return client;
        }
    }

    public static S3Client getClient() {
        return Holder.CLIENT;   // same shared, thread-safe client everywhere
    }
}
```

```java
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Demo: many concurrent request handlers all share ONE S3 client. */
public class S3ClientDemo {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService requests = Executors.newFixedThreadPool(32);
        ConcurrentHashMap<Integer, Boolean> identities = new ConcurrentHashMap<>();

        for (int i = 0; i < 200; i++) {
            requests.submit(() -> {
                // Every handler calls getClient() — but there is only ONE client object,
                // so all 200 requests reuse the same connection pool & threads.
                S3Client s3 = S3ClientProvider.getClient();
                identities.put(System.identityHashCode(s3), true);
                // s3.getObject(GetObjectRequest.builder().bucket("b").key("k").build());
            });
        }
        requests.shutdown();
        requests.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("Distinct S3Client instances used: " + identities.size()); // -> 1
        // One client shared across 200 concurrent requests: no socket/thread explosion.
    }
}
```

**Code explanation.** The `S3ClientProvider` never instantiates *itself* (private constructor, all static) — it's a namespace around a single client. The **Bill Pugh holder** builds the `S3Client` lazily on the first `getClient()` call and thread-safely without locks, which matters because building an SDK client is expensive and you want it to happen exactly once. The one production detail people miss is the **shutdown hook**: `client::close` releases the underlying HTTP connection pool and I/O threads on JVM exit — skip it and you leak sockets and can hang shutdown. The demo fires 200 tasks across 32 threads, each grabbing `getClient()` and recording its identity hash; the distinct-instance count is `1`, proving all 200 requests share one client (and therefore one connection pool), which is the entire point — a per-request client would spawn 200 pools. In a Spring app you'd instead register this as a `@Bean` (container-managed singleton) and inject it; this hand-rolled provider is what you write in a plain-Java service or a Lambda handler with no container.

</details>

### Scenario 4 — Shared HTTP Client (OkHttp)

<details>
<summary>💻 Click to expand — shared OkHttpClient singleton + demo + explanation</summary>

OkHttp's documentation is explicit: *"OkHttp performs best when you create a single `OkHttpClient` instance and reuse it,"* because each instance holds its own connection pool and thread pools. A new client per request means no connection reuse (a fresh TCP + TLS handshake every time) and an ever-growing thread count. The same holder idiom gives one shared, thread-safe client.

```java
import okhttp3.OkHttpClient;

/**
 * One OkHttpClient for the whole JVM. Each instance owns a connection pool and
 * thread pools, so reusing a single client enables connection/thread reuse.
 */
public final class HttpClientProvider {

    private HttpClientProvider() {}   // no instances of the provider itself

    // Holder: built lazily on first getClient(), thread-safe & lock-free.
    private static class Holder {
        private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();

        static {
            // OkHttp reaps idle threads on its own, but for eager teardown release its pools.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                CLIENT.dispatcher().executorService().shutdown();
                CLIENT.connectionPool().evictAll();
            }, "okhttp-closer"));
        }
    }

    public static OkHttpClient getClient() {
        return Holder.CLIENT;   // same shared, thread-safe client everywhere
    }
}
```

```java
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Demo: concurrent callers share ONE OkHttpClient (and therefore one connection pool). */
public class HttpClientDemo {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService callers = Executors.newFixedThreadPool(16);
        ConcurrentHashMap<Integer, Boolean> identities = new ConcurrentHashMap<>();

        for (int i = 0; i < 100; i++) {
            callers.submit(() -> {
                OkHttpClient http = HttpClientProvider.getClient();
                identities.put(System.identityHashCode(http), true);
                // Request req = new Request.Builder().url("https://api.example.com").build();
                // try (Response r = http.newCall(req).execute()) { ... }  // reuses pooled conns
            });
        }
        callers.shutdown();
        callers.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("Distinct OkHttpClient instances used: " + identities.size()); // -> 1
        // One client => one connection pool reused across all 100 callers.
    }
}
```

**Code explanation.** Structurally identical to the S3 provider — private constructor, Bill Pugh holder, static accessor — which is the point: the shared-client idiom is uniform across SDKs. The client is built once via `OkHttpClient.Builder()`, so every caller reuses the same **connection pool** (keep-alive sockets get recycled instead of re-handshaking) and the same dispatcher **thread pool**. The shutdown hook here is slightly different from S3: OkHttp has no single `close()`, so you release its two resource pools explicitly — `dispatcher().executorService().shutdown()` stops the async worker threads and `connectionPool().evictAll()` closes idle sockets — otherwise non-daemon threads can keep the JVM alive. The demo confirms 100 concurrent callers see one client instance; in real code you'd derive per-request `Request` objects and short-lived `Response`s from that one client, which is exactly how OkHttp is meant to be used (the *client* is the expensive shared singleton; requests are cheap and transient).

</details>

### Scenario 5 — Shared Redis Client (Lettuce)

<details>
<summary>💻 Click to expand — shared Lettuce Redis client singleton + demo + explanation</summary>

Lettuce's `RedisClient` is thread-safe and designed to be shared, and a single `StatefulRedisConnection` **multiplexes** commands from many threads over one socket (Redis is single-threaded server-side, so one multiplexed connection is efficient). Creating a client or connection per call defeats multiplexing, leaks connections, and exhausts the server's client limit. One shared client + one shared connection is the recommended shape.

```java
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * One RedisClient + one StatefulRedisConnection for the whole JVM.
 * The connection multiplexes commands from many threads over a single socket.
 */
public final class RedisProvider {

    private RedisProvider() {}   // no instances of the provider itself

    // Holder: client + connection built lazily on first access, thread-safe & lock-free.
    private static class Holder {
        private static final RedisClient CLIENT =
                RedisClient.create("redis://localhost:6379");
        private static final StatefulRedisConnection<String, String> CONN = CLIENT.connect();

        static {
            // Close the connection and release event-loop threads on JVM shutdown.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                CONN.close();
                CLIENT.shutdown();
            }, "redis-closer"));
        }
    }

    public static StatefulRedisConnection<String, String> connection() {
        return Holder.CONN;   // same shared, multiplexed connection everywhere
    }
}
```

```java
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Demo: many threads share ONE multiplexed Redis connection. */
public class RedisClientDemo {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService workers = Executors.newFixedThreadPool(16);
        ConcurrentHashMap<Integer, Boolean> identities = new ConcurrentHashMap<>();

        for (int i = 0; i < 100; i++) {
            final int n = i;
            workers.submit(() -> {
                StatefulRedisConnection<String, String> conn = RedisProvider.connection();
                identities.put(System.identityHashCode(conn), true);
                // Commands from many threads are multiplexed over the one connection:
                // RedisCommands<String, String> cmd = conn.sync();
                // cmd.set("key:" + n, "value");
            });
        }
        workers.shutdown();
        workers.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("Distinct Redis connections used: " + identities.size()); // -> 1
        // One multiplexed connection shared across all 100 workers.
    }
}
```

**Code explanation.** Same idiom again, but note the Redis-specific reason it matters: Lettuce is built on Netty and **multiplexes** — a single `StatefulRedisConnection` safely carries commands from many threads concurrently over one TCP socket, pipelining them to the (single-threaded) Redis server. So the singleton here holds *both* the `RedisClient` (the heavyweight, Netty-event-loop-owning object) and one shared connection, and the demo proves all 100 workers use the same connection instance. Contrast with the anti-pattern: a connection-per-operation would open 100 sockets, each with its own handshake, quickly hitting Redis's `maxclients` limit and adding latency — the multiplexed singleton avoids all of that. The shutdown hook closes the connection and calls `CLIENT.shutdown()` to release Netty's event-loop threads (non-daemon), so the JVM can exit cleanly. If you needed blocking commands (e.g. `BLPOP`) or transactions/`WATCH`, you'd take a *dedicated* connection for those, while everything else keeps sharing the singleton — a nuance worth mentioning in an interview.

</details>

**Why a singleton here and not just DI?** In a Spring app you'd register each client as a `@Bean` (container-managed singleton) and inject it — that's the preferred form. These hand-rolled providers are what you write in a **plain-Java service, a Lambda handler, or library code** with no container: same guarantee (one client per JVM), obtained via a static accessor. The two things people forget are (1) **laziness/thread-safety** (the holder idiom handles both) and (2) **cleanup** (a shutdown hook to release the client's connection and I/O thread pools — otherwise you leak resources and shutdown can hang).

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- **Exactly one instance is a correctness requirement**, not just an optimization — a bounded resource pool (connections, threads) where a second instance would violate a hard limit.
- **A single shared resource with global reach**: an application logger, a configuration/settings holder loaded once at startup, a shared in-memory cache, a metrics/registry object.
- **Creation is expensive and the result is immutable or safely shared** — read a config file, warm a cache, open a shared client once and reuse it everywhere.
- **A single logical authority** must coordinate access to something (an ID generator, a lock/registry manager) and duplicating it would break the invariant it guards.
- **No dependency-injection framework is available** and you still need one well-known instance (in a Spring/Guice app, prefer a container-managed singleton bean instead).

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- **You might need more than one instance later.** Singleton bakes "exactly one" into the type; loosening it later is a painful refactor. If cardinality is uncertain, don't hard-code it.
- **The class is stateless.** If there's no shared state, a bag of `static` utility methods (or just injecting a plain object) is simpler and clearer than singleton ceremony.
- **In code you need to unit-test in isolation.** A `getInstance()` call buried in a method is a hidden dependency and shared mutable state that leaks between tests — prefer **dependency injection** so you can pass a mock/fake.
- **Per-request / per-user / per-tenant state.** A singleton is process-wide; storing request- or user-scoped data in it causes cross-talk and data leaks between concurrent requests.
- **You actually need "one per cluster."** A JVM singleton is one-per-process; across a distributed system you need Redis/ZooKeeper/leader-election, not this pattern.
- **It's just a convenient global.** Reaching for a singleton to avoid passing an argument is how singletons become the dumping ground for global mutable state and tight coupling.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- **Guaranteed single instance** — the invariant is enforced by the class, not by convention.
- **Global access point** — any code can reach the shared instance without threading it through constructors.
- **Lazy initialization possible** — expensive setup deferred until first use (holder idiom, DCL).
- **Controlled, one-time creation** — do costly work (file read, socket open, cache warm) exactly once.
- **Memory-efficient** — one object reused instead of many duplicates.

**Cons**

- **Global mutable state (the headline con)** — creates hidden dependencies and tight coupling; any method can silently reach the singleton, so dependencies aren't visible in signatures.
- **Hard to unit-test** — shared state persists across tests; you can't easily substitute a mock unless you inject an interface.
- **Violates Single Responsibility** — the class does its real job *and* manages its own lifecycle/uniqueness.
- **Concurrency complexity** — a correct lazy singleton needs care (volatile/DCL, or the holder/enum idioms).
- **Back doors** — reflection, serialization, and cloning can each create a second instance unless explicitly defended (or you use an enum).
- **"One per JVM" ≠ "one per cluster"** — a false sense of uniqueness in distributed systems.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

Singleton is most often confused with a **static utility class**, and with the **Factory** patterns (because factories are frequently *implemented* as singletons). The distinctions are about *identity, polymorphism, and lifecycle*.

| Aspect | **Singleton** | **Static Utility Class** | **Factory (Method / Abstract)** | **Monostate / Borg** |
|---|---|---|---|---|
| Instances | Exactly one object | Zero (only static members) | Many objects it *creates* | Many instances, one shared state |
| Implements interface / polymorphic | Yes | No | Yes (the factory can be) | Yes |
| Can be subclassed / injected | Yes (as an interface) | No | Yes | Yes |
| Lazy init | Yes | No (loaded with class) | N/A | N/A |
| Purpose | Single shared *instance* | Stateless helper functions | Encapsulate object *creation* | Share state across instances |
| Key tell | "There must be only one" | "No state, just functions" | "Decide which class to build" | "Instances differ but share state" |

**Singleton vs. Static class:** a static class is just namespaced functions — it can't implement an interface, can't be subclassed, can't be passed as a parameter or mocked, and initializes eagerly with its class. Choose Singleton when you need **an object** (polymorphism, interface, lazy init, injectability); choose a static class for **pure stateless utilities**.

**Singleton vs. Factory:** different intents that often combine. A Factory's job is to *create* objects (possibly many, of varying concrete types); a Singleton's job is to *limit* creation to one. A factory is very commonly made a singleton (one shared factory), which is why they co-occur — but they answer different questions ("how do I build the right object?" vs. "how do I ensure just one?").

**Singleton vs. Monostate (Borg):** Monostate achieves the *effect* of a singleton (all instances share state via `static` fields) while allowing `new` and polymorphism normally. It's less common but occasionally preferred because it's transparent to callers (they just `new` it) and easier to substitute — at the cost of surprising "these look like separate objects but aren't" semantics.

</details>

## 📊 Comparison Table of Variants

<details>
<summary>📖 Click to expand</summary>

| Axis | V1 Lazy | V2 Synchronized | V3 DCL (`volatile`) | V4 Eager | V5 Bill Pugh | V6 Enum |
|---|---|---|---|---|---|---|
| **Thread-safe** | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Lazy init** | ✅ | ✅ | ✅ | ❌ | ✅ | ❌ |
| **Per-call lock cost** | none | **every call** | first call only | none | none | none |
| **Serialization-safe** | ❌ | ❌ | ❌ | ❌ | ❌ (needs `readResolve`) | ✅ (free) |
| **Reflection-safe** | ❌ | ❌ | ❌ | ❌ | ❌ (needs guard) | ✅ (free) |
| **Code complexity** | trivial | low | **high (easy to botch)** | trivial | low | very low |
| **Can extend a class** | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| **Key mechanism** | none (broken) | monitor lock | `volatile` + double check | class-init guarantee | nested-class lazy init | enum + JVM guarantees |
| **Verdict** | never in prod | ok, slow | ok if you must | good if cheap+always used | **best lazy class** | **best default overall** |

**Best choice:** **Enum** when you don't need laziness or inheritance; **Bill Pugh holder** when you need lazy initialization or must extend a class. Avoid hand-written DCL unless you have a specific reason — the holder idiom gives the same result with less risk.

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

### Pitfall 1: Forgetting `volatile` in double-checked locking

**What goes wrong:** Without `volatile`, the write `instance = new Foo()` can be reordered so the reference is published *before* the constructor finishes. A second thread taking the lock-free fast path sees a non-null reference and returns a **half-constructed object** with uninitialized fields — an intermittent, near-impossible-to-reproduce bug.

<details>
<summary>💻 Broken (no volatile)</summary>

```java
private static Foo instance;                 // NOT volatile — bug
public static Foo getInstance() {
    if (instance == null) {
        synchronized (Foo.class) {
            if (instance == null) instance = new Foo();  // publish may precede construction
        }
    }
    return instance;                          // may return a not-yet-built object
}
```
</details>

<details>
<summary>💻 Fix (volatile — or just use the holder idiom)</summary>

```java
private static volatile Foo instance;        // volatile => happens-before, no reordering
// ...same DCL body... now a non-null read is guaranteed to see a fully constructed object.

// Better: skip DCL entirely.
private static class Holder { static final Foo INSTANCE = new Foo(); }
public static Foo getInstance() { return Holder.INSTANCE; }
```
</details>

### Pitfall 2: Reflection breaks the singleton

**What goes wrong:** A private constructor is not private to reflection. `setAccessible(true)` lets any caller invoke it and mint a *second* instance, defeating the whole guarantee.

<details>
<summary>💻 The attack</summary>

```java
Constructor<Singleton> c = Singleton.class.getDeclaredConstructor();
c.setAccessible(true);
Singleton rogue = c.newInstance();           // a brand-new second instance!
System.out.println(rogue == Singleton.getInstance()); // false
```
</details>

<details>
<summary>💻 Fix (guard in constructor, or use enum)</summary>

```java
private Singleton() {
    if (Holder.INSTANCE != null) {           // second construction => fail loudly
        throw new IllegalStateException("Use getInstance()");
    }
}
// Bulletproof alternative: an enum singleton — Constructor.newInstance()
// throws IllegalArgumentException for enums, so reflection cannot create a second constant.
```
</details>

### Pitfall 3: Serialization creates a new instance

**What goes wrong:** Deserializing a `Serializable` singleton constructs a **fresh** object (deserialization bypasses the constructor), so `deserialized != getInstance()` — you now have two.

<details>
<summary>💻 The problem</summary>

```java
class Singleton implements Serializable { /* holder-based getInstance */ }
// round-trip through ObjectOutputStream/ObjectInputStream -> a DIFFERENT object comes back.
```
</details>

<details>
<summary>💻 Fix (readResolve — or use enum)</summary>

```java
// Add to the class: on deserialize, discard the fresh object and return the canonical one.
protected Object readResolve() {
    return getInstance();
}
// Enums are serialization-safe automatically — they serialize by name and resolve to the constant.
```
</details>

### Pitfall 4: Cloning duplicates the instance

**What goes wrong:** If the class is `Cloneable` (or extends something that is), `clone()` produces a copy — another instance.

<details>
<summary>💻 Fix</summary>

```java
@Override
protected Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException("Singleton is not cloneable");
}
```
</details>

### Pitfall 5: Multiple classloaders → multiple singletons

**What goes wrong:** "One per JVM" is really "one per classloader." In app servers, OSGi, or plugin systems, the same `Singleton` class loaded by two classloaders yields **two** independent instances (the `static` field lives per loaded-class, and class identity includes its classloader). This surprises people in Tomcat/EE environments.

**The fix:** Be aware of the deployment; if you truly need one-per-JVM across classloaders, load the singleton from a shared parent classloader, or externalize the single instance (JNDI, a shared service) rather than relying on `static`.

### Pitfall 6: Using a singleton for per-request state

**What goes wrong:** Storing request/user-scoped mutable data in a process-wide singleton causes **data leaking across concurrent requests** (thread A's data overwritten/read by thread B). Singletons should hold shared, thread-safe, request-independent state only.

**The fix:** Keep the singleton stateless or holding only immutable/shared config; put per-request data in method parameters, `ThreadLocal`, or request-scoped beans.

</details>

## 🎓 Interview Tips

**What interviewers commonly ask:**

- "Implement a thread-safe Singleton." (Then they poke: *why* `volatile`? what if you remove it? why the *second* null-check?)
- "What's the difference between eager and lazy initialization, and when would you pick each?"
- "How can a Singleton be broken?" (Reflection, serialization, cloning, multiple classloaders — and the fix for each.)
- "What's the best way to implement Singleton in Java?" (Enum, per *Effective Java*; Bill Pugh when you need lazy/inheritance.)
- "Singleton vs. static class?" and "Why is Singleton considered an anti-pattern by some?"
- "How would you make 'one instance' work across a cluster?" (It's no longer Singleton — leader election / distributed lock.)

**What you should proactively mention even if not asked:**

- **`volatile` + double-checked locking**: explain the reordering / half-constructed-object problem and that the fix relies on the post-Java-5 Java Memory Model.
- **Bill Pugh holder** and **enum** are the two production choices — and *why* they're thread-safe *without* synchronization (the JVM's guaranteed-once, thread-safe class initialization).
- **The back doors** (reflection, serialization, cloning) and that **enum closes all of them for free**.
- **The critique**: Singleton is global mutable state → hidden dependencies, hard testing, tight coupling. Say you'd prefer **dependency injection / a container-managed singleton bean** in a real service, reserving hand-rolled Singleton for when no framework exists.
- **"One per JVM ≠ one per cluster"** and the classloader caveat — shows senior awareness.
- The distinction between **enforcing uniqueness** (the useful half) and **providing a global access point** (the dangerous half).

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Factory Method / Abstract Factory** — factories are frequently *implemented as* singletons (one shared factory), and a singleton is often obtained via a factory-like `getInstance()`.
- **Facade** — a facade over a subsystem is commonly a singleton, since you typically want one shared entry point.
- **Builder** — sometimes used to configure the single instance's construction before it's cached.
- **Monostate (Borg)** — an alternative that shares state across many instances instead of restricting to one object; achieves singleton-*like* behavior with normal `new` semantics.
- **Dependency Injection / IoC container** — the modern replacement for hand-rolled singletons: the *container* guarantees one instance (singleton scope) and injects it, decoupling callers from `getInstance()`.
- **Object Pool** — related when the singleton *is* a pool: one pool object managing many reusable resources (connections, threads).
- **Prototype** — the conceptual opposite: Prototype exists to make *copies*, Singleton exists to prevent them.

</details>

## 📚 Library/Framework Implementation

**1. `java.lang.Runtime` (JDK).** The textbook eager singleton in the standard library: `Runtime.getRuntime()` returns the one `Runtime` object for the JVM, created in a `private static final Runtime currentRuntime = new Runtime();` field with a private constructor. It's the canonical "there is exactly one runtime environment per JVM" example.

<details>
<summary>💻 Click to expand — java.lang.Runtime shape & usage</summary>

```java
// Conceptually how the JDK defines it:
public class Runtime {
    private static final Runtime currentRuntime = new Runtime();   // eager, thread-safe
    public static Runtime getRuntime() { return currentRuntime; }  // global access point
    private Runtime() {}                                           // no external new
}

// Usage — always the same instance:
Runtime.getRuntime().availableProcessors();
Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("bye")));
```
</details>

**2. Spring `@Bean` / `@Component` singleton scope.** Spring's *default* bean scope is `singleton` — the container creates and caches exactly one instance per `ApplicationContext` and injects that same instance everywhere it's needed. This is the modern, testable way to get "one instance": the framework enforces cardinality, but callers receive it via **dependency injection** instead of a static `getInstance()`, so you can swap it for a mock in tests. (Note: Spring "singleton" is per-container, not per-classloader like the GoF pattern.)

<details>
<summary>💻 Click to expand — Spring container-managed singleton</summary>

```java
@Service                                   // singleton scope by default
class MetricsRegistry {
    private final Map<String, Long> counters = new ConcurrentHashMap<>();
    public void inc(String k) { counters.merge(k, 1L, Long::sum); }
}

@RestController
class OrderController {
    private final MetricsRegistry metrics;             // injected, not fetched statically
    OrderController(MetricsRegistry metrics) { this.metrics = metrics; } // same instance app-wide
    // In tests: new OrderController(mock(MetricsRegistry.class)) — trivially mockable.
}
```
</details>

**3. SLF4J/Logback loggers, `java.awt.Desktop`, `Collections.emptyList()` cached instances, and Guava/Spring singletons.** Logging frameworks return effectively-singleton logger/appender contexts; `Desktop.getDesktop()` is a singleton accessor; the JDK caches shared immutable singletons like `Collections.EMPTY_LIST`. `Calendar`, `DateFormat` factories and many "registry" objects follow the same one-shared-instance idiom. Most modern frameworks favor the *container-managed* form over hand-written `getInstance()`.

<details>
<summary>💻 Click to expand — everyday singleton accessors in the JDK</summary>

```java
// A shared, immutable singleton instance reused everywhere:
List<String> empty = Collections.emptyList();     // same cached EMPTY_LIST object

// Singleton accessor for the desktop integration object:
if (Desktop.isDesktopSupported()) {
    Desktop.getDesktop().browse(URI.create("https://example.com"));
}
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1: [Conceptual] What is the Singleton pattern and what problem does it solve?</strong></summary>

The Singleton is a **creational** GoF pattern that **ensures a class has only one instance and provides a global point of access to it**. It bundles two responsibilities: controlling instantiation (nobody can `new` the class) and exposing a single accessor (`getInstance()`).

It solves the problem of objects that must exist *exactly once* because a duplicate would be wrong or wasteful — a connection pool that owns a bounded set of connections, a logger owning a file handle, a config loaded once at startup, a shared cache. Voluntary sharing is unenforceable (any code can `new` another), and a plain global variable can be reassigned and offers no lazy control. Singleton makes the *class itself* the guardian of its cardinality via a private constructor plus a static accessor.

The nuance is that "exactly one" is a **concurrency, serialization, reflection, and classloader** guarantee — a correct implementation must survive threads racing into `getInstance()` and seal the back doors that can smuggle in a second instance.
</details>

<details>
<summary><strong>Q2: [Conceptual] What are the key structural elements of a Singleton?</strong></summary>

Three essentials plus optional defenses:

1. **Private constructor** — prevents any external `new`; the linchpin that makes uniqueness enforceable.
2. **A private static field** holding the one instance — `static` ties it to the class (one per classloader), created eagerly (`static final`) or lazily.
3. **A public static accessor** (`getInstance()`) — the single global access point; must be thread-safe if creation is lazy.

Optional hardening: `readResolve()` for serialization safety, a construction guard (or enum) against reflection, and blocking `clone()`. An **enum** singleton provides #1–#3 plus all the hardening for free.
</details>

<details>
<summary><strong>Q3: [Conceptual] Eager vs. lazy initialization — what's the difference and when do you choose each?</strong></summary>

**Eager**: the instance is created at class-initialization time (`static final Foo INSTANCE = new Foo();`). It's thread-safe for free (JVM class-init guarantee) and dead simple, but it's built whether or not it's ever used, and it happens early. Choose it when creation is **cheap and the instance is (almost) always needed**.

**Lazy**: the instance is created on the first `getInstance()` call. It defers expensive work and avoids building unused objects, but you must make the first-time creation thread-safe (holder idiom, DCL, or synchronized). Choose it when creation is **expensive and possibly unused**, or when you need control over *when* it's built.

In modern Java the lazy choice is almost always the **Bill Pugh holder idiom** (lazy + thread-safe + lock-free), and the eager-but-bulletproof choice is the **enum**.
</details>

<details>
<summary><strong>Q4: [Conceptual] Why is Singleton often called an anti-pattern? Is it?</strong></summary>

Because its second half — the *global access point* — is easily abused into **global mutable state**. That brings hidden dependencies (a method can reach the singleton without declaring it in its signature, so you can't tell what it touches), tightly couples code to a concrete instance, and makes unit testing hard (shared state leaks across tests; you can't inject a mock through a static `getInstance()`). It also tends to violate SRP (the class does its job *and* manages its lifecycle).

It's not inherently evil — *enforcing uniqueness* is legitimately useful. The critique is really about the *global access* and hand-rolled `static` accessor. The mature stance: keep the "one instance" guarantee but obtain it via **dependency injection / a container-managed singleton bean**, so callers receive it explicitly and tests can substitute a fake. Reserve hand-written `getInstance()` for when no DI framework exists.
</details>

<details>
<summary><strong>Q5: [Implementation] Implement a thread-safe Singleton. Walk through your choices.</strong></summary>

I'd reach for the **Bill Pugh holder** (lazy + thread-safe + lock-free) or an **enum** (bulletproof). Holder version:

<details>
<summary>💻 Code</summary>

```java
public final class Singleton {
    private Singleton() {
        if (Holder.INSTANCE != null) throw new IllegalStateException("Use getInstance()");
    }
    private static class Holder {                       // not loaded until first getInstance()
        private static final Singleton INSTANCE = new Singleton();
    }
    public static Singleton getInstance() {
        return Holder.INSTANCE;                          // lazy + thread-safe, no locks
    }
}
```
</details>

Why: the nested `Holder` class isn't initialized until `Holder.INSTANCE` is first referenced, so it's **lazy**; and the JVM guarantees class initialization runs **once** and **thread-safely** (an internal init lock), so it's **thread-safe with no `synchronized`/`volatile`**. If serialization/reflection safety matters and I don't need inheritance, I'd use an enum instead — it seals those back doors for free.
</details>

<details>
<summary><strong>Q6: [Implementation] Why is `volatile` required in double-checked locking? What breaks without it?</strong></summary>

`instance = new Singleton()` is **not atomic** — it's roughly: (1) allocate memory, (2) run the constructor, (3) publish the reference to `instance`. The JIT/CPU may **reorder** (2) and (3). Without `volatile`, a thread on the lock-free first check can observe a **non-null but not-yet-constructed** object (step 3 before step 2) and return a **half-initialized instance**, whose fields are still defaults — an intermittent heisenbug.

`volatile` inserts a memory barrier that (a) forbids that reordering and (b) establishes a **happens-before** edge: the constructor's writes happen-before the write to `instance`, and any non-null `volatile` read happens-after that write. So a non-null read is guaranteed to see a fully constructed object. Historically DCL was *broken* before Java 5 because the old memory model didn't give `volatile` these guarantees; the revised JMM (JSR-133) fixed it.

<details>
<summary>💻 The dangerous ordering (without volatile)</summary>

```
Thread A: mem = allocate()
Thread A: instance = mem            // (3) publish BEFORE construct — legal reorder!
Thread B: if (instance == null) ->  // false; returns 'instance'...
Thread B: uses half-built object    // fields are still zero/null  <-- BUG
Thread A: run constructor on mem     // too late
```
</details>
</details>

<details>
<summary><strong>Q7: [Implementation] Explain the Bill Pugh (initialization-on-demand holder) idiom and why it's thread-safe without synchronization.</strong></summary>

You place the instance in a **private static nested class**:

<details>
<summary>💻 Code</summary>

```java
public class Singleton {
    private Singleton() {}
    private static class Holder { static final Singleton INSTANCE = new Singleton(); }
    public static Singleton getInstance() { return Holder.INSTANCE; }
}
```
</details>

**Laziness:** per the JLS, a class is initialized only when it's *actively used*. Loading `Singleton` does **not** load `Holder`; `Holder` is initialized only on the first read of `Holder.INSTANCE`, which happens inside `getInstance()`. So the instance is created lazily, on first call.

**Thread-safety:** the JVM guarantees class initialization is **serialized and executed exactly once** — the classloader holds an initialization lock around `<clinit>`. So even if 100 threads call `getInstance()` simultaneously, exactly one initializes `Holder`, and the rest block until it's done and then see the finished `INSTANCE`. No `synchronized`, no `volatile`, no per-call cost — the JVM's own class-init lock does the work.
</details>

<details>
<summary><strong>Q8: [Implementation] What's the best Singleton implementation and why? Show it.</strong></summary>

For most cases, the **enum singleton** — Joshua Bloch's recommendation in *Effective Java* (Item 3):

<details>
<summary>💻 Code</summary>

```java
public enum Singleton {
    INSTANCE;
    private final Map<String,String> state = new ConcurrentHashMap<>();
    public void put(String k, String v) { state.put(k, v); }
    public String get(String k) { return state.get(k); }
}
// Usage: Singleton.INSTANCE.put("a","1");
```
</details>

It's **thread-safe** (enum constants are created once in the class initializer), **serialization-safe for free** (the JVM serializes enums by name and resolves to the existing constant — no `readResolve` needed), and **reflection-safe for free** (`Constructor.newInstance` throws `IllegalArgumentException` for enum types). All that in a few lines.

Caveat: it's **eager** and **can't extend a class**. When I need laziness or inheritance, I use the **Bill Pugh holder** instead.
</details>

<details>
<summary><strong>Q9: [Trade-off] Singleton vs. a static utility class — when do you use which?</strong></summary>

| Aspect | Singleton | Static class |
|---|---|---|
| Instance | One object | None (only statics) |
| Implements interface | Yes | No |
| Subclass / polymorphism | Yes | No |
| Pass as parameter / inject | Yes | No |
| Lazy init | Yes | No (loads with class) |
| Mockable in tests | Yes (via interface) | Hard |

Use a **Singleton** when you need an **object** — to implement an interface, be injected/mocked, participate in polymorphism, or be lazily initialized (e.g., a service like a logger or cache). Use a **static utility class** for **pure, stateless functions** (`Math`, `Collections`) where there's no state and no need for polymorphism — it's simpler and needs no instance.
</details>

<details>
<summary><strong>Q10: [Breaking] Enumerate every way a Singleton can be broken, and the fix for each.</strong></summary>

1. **Concurrency** — check-then-create race in a naive lazy singleton creates multiple instances. *Fix:* holder idiom, enum, or DCL with `volatile`.
2. **Reflection** — `setAccessible(true)` + `newInstance()` invokes the private constructor. *Fix:* throw in the constructor if an instance exists; or use an enum (reflection can't instantiate enums).
3. **Serialization** — deserialization builds a fresh object bypassing the constructor. *Fix:* implement `readResolve()` returning `getInstance()`; or use an enum (serialized by name).
4. **Cloning** — `clone()` copies the instance. *Fix:* override `clone()` to throw `CloneNotSupportedException`; or don't implement `Cloneable`.
5. **Multiple classloaders** — the same class loaded by two loaders yields two `static` fields → two instances. *Fix:* load from a shared parent classloader or externalize the instance.

The one-line takeaway interviewers want: **an enum singleton closes #2, #3, #4 automatically and #1 by construction** — which is why it's the recommended default.
</details>

<details>
<summary><strong>Q11: [Breaking] Why does deserialization create a new instance, and how exactly does <code>readResolve()</code> fix it?</strong></summary>

Java deserialization reconstructs an object **without calling its constructor** — it allocates the object and populates fields from the stream. So your private-constructor guard never runs, and you get a brand-new instance distinct from `getInstance()`.

`readResolve()` is a hook the serialization mechanism calls **after** an object is deserialized: whatever it returns *replaces* the freshly-read object as the result of `readObject()`. By returning the canonical instance, you discard the duplicate:

<details>
<summary>💻 Code</summary>

```java
class Singleton implements Serializable {
    private static final long serialVersionUID = 1L;
    // ... holder-based getInstance ...
    protected Object readResolve() {          // discard the deserialized copy
        return getInstance();                 // return the one true instance
    }
}
```
</details>

Also mark fields `transient` where appropriate so their values aren't restored onto the throwaway object. An **enum** needs none of this — the spec resolves enums to their existing constant by name.
</details>

<details>
<summary><strong>Q12: [Breaking] How do you make a singleton reflection-proof? Why can't the same attack work on an enum?</strong></summary>

For a class-based singleton, put a guard in the private constructor so a *second* construction fails:

<details>
<summary>💻 Code</summary>

```java
private Singleton() {
    if (Holder.INSTANCE != null)                      // already built => reflective attempt
        throw new IllegalStateException("Use getInstance()");
}
```
</details>

This isn't perfectly airtight (ordering games during class init), which is why the truly bulletproof answer is the **enum**: the reflection API special-cases enums. `java.lang.reflect.Constructor.newInstance()` explicitly checks `if ((clazz.getModifiers() & Modifier.ENUM) != 0) throw new IllegalArgumentException("Cannot reflectively create enum objects");`. So the JVM itself refuses to instantiate enum constants reflectively — the back door is sealed at the platform level, not by your code.
</details>

<details>
<summary><strong>Q13: [Trade-off] How does dependency injection relate to Singleton? Is Spring's singleton the same thing?</strong></summary>

DI is the modern answer to the *problem* Singleton addresses ("I need one shared instance") **without** the *downsides* of the GoF form. Instead of a static `getInstance()` that callers reach into, the **container** creates one instance and **injects** it wherever declared. That flips the dependency: callers receive the instance explicitly (visible in constructors), so dependencies are honest and you can pass a **mock** in tests — solving the testability and hidden-coupling critiques.

Spring's default bean scope *is* "singleton," but it's **per `ApplicationContext`**, not per-JVM/per-classloader like the GoF pattern, and it's obtained via injection rather than a static accessor. So: same *goal* (one instance), much better *ergonomics and testability*. In a real service, prefer a container-managed singleton; hand-rolled Singleton is for when there's no container.
</details>

<details>
<summary><strong>Q14: [Trade-off] Why does Singleton make unit testing hard, and how do you make singleton-dependent code testable?</strong></summary>

Two reasons: (1) **shared mutable state persists across tests** — one test mutates the singleton, the next sees the dirty state, causing order-dependent flakiness; and (2) **hidden, hard-to-substitute dependency** — code that calls `Logger.getInstance()` internally can't be handed a mock, so you can't isolate it.

Fixes, best to worst:

- **Program to an interface + inject it** (best): the class depends on a `Logger` interface passed in; production wires the singleton, tests pass a fake. The singleton stays, but callers don't hard-reference `getInstance()`.
- **DI container** manages the singleton scope and injects mocks in tests.
- **A test-only `reset()`** (last resort) to clear state between tests — pragmatic but leaks test concerns into production code.

<details>
<summary>💻 Interface injection</summary>

```java
interface Logger { void log(String m); }
class Service {
    private final Logger logger;
    Service(Logger logger) { this.logger = logger; }   // inject: prod=singleton, test=mock
}
```
</details>
</details>

<details>
<summary><strong>Q15: [Advanced] Can a Singleton be subclassed? Should it be?</strong></summary>

**Technically** you can make the constructor `protected` and subclass it, but it's problematic: a subclass with its own instance field gives you a *second* instance, breaking the "one instance" invariant, and `getInstance()`'s return type/registry gets confusing. Enums can't be subclassed at all.

If you need *varying behavior* behind a single access point, the clean approach is a **registry singleton** returning an interface, choosing the implementation at initialization (composition/Strategy) rather than inheriting the singleton:

<details>
<summary>💻 Registry-style variation instead of subclassing</summary>

```java
interface Cache { Object get(String k); }
class CacheProvider {
    private static final Cache INSTANCE =
        "redis".equals(System.getenv("CACHE")) ? new RedisCache() : new LocalCache();
    public static Cache getInstance() { return INSTANCE; }   // one instance, chosen impl
}
```
</details>

So: subclassing a singleton is usually a design smell — prefer composition/Strategy behind the accessor.
</details>

<details>
<summary><strong>Q16: [Advanced] How do you achieve "exactly one instance" across a distributed cluster?</strong></summary>

You **can't** with the GoF Singleton — it guarantees one instance **per JVM** (really per classloader), and a cluster has many JVMs. "One logical instance across nodes" is a **distributed-coordination** problem, not a language pattern:

- **Leader election** (ZooKeeper, etcd, Raft, or Kubernetes lease): elect one node as the active "instance"; others stand by. This is how you get a single active scheduler/coordinator.
- **Distributed lock** (Redis `SETNX`/Redlock, ZooKeeper ephemeral node): whoever holds the lock is "the one," with lease/TTL for failover.
- **Externalized shared state** (Redis/DB): the "single instance" is really shared state all nodes read/write, with the DB enforcing invariants.

The senior insight: first **question the requirement** — often you don't need a global singleton, just per-node instances plus a shared source of truth. If you truly need one active actor (a leader), use leader election and design for **failover**, because that single instance is now a SPOF.
</details>

<details>
<summary><strong>Q17: [Advanced] What is the "multiple classloaders → multiple singletons" problem?</strong></summary>

Class identity in the JVM is `(fully-qualified name, defining classloader)` — the *same* `.class` loaded by two different classloaders produces **two distinct `Class` objects**, each with its **own** copy of the `static` instance field. So a "singleton" can exist twice in one JVM: common in **application servers** (each webapp has its own classloader), **OSGi**, and **plugin frameworks**.

Mitigations: load the singleton class from a **shared parent classloader** (so both children delegate to the same loaded class), or **externalize** the single instance to something classloader-agnostic (a JNDI-bound object, a shared service). Mentioning this shows you understand that "one per JVM" is a simplification — it's really "one per class-loading."
</details>

<details>
<summary><strong>Q18: [Advanced] Are there memory/lifecycle concerns with singletons (leaks, GC, "static hell")?</strong></summary>

Yes. A singleton held in a `static` field lives as long as its `Class` is loaded — effectively the process lifetime. Two consequences:

- **Memory leaks via the singleton.** If the singleton holds references (listeners, caches, `ThreadLocal`s, session objects) and never releases them, those objects are never GC'd — a classic long-running-server leak. In app servers, a singleton in a shared classloader holding references to *webapp* classes prevents the webapp from unloading on redeploy (a notorious PermGen/Metaspace leak).
- **Non-deterministic init/teardown.** You don't control *when* an eager singleton initializes or *whether* it gets a clean shutdown. Register a shutdown hook or lifecycle callback for resources (close pools/files).

Design guidance: keep singletons **stateless or bounded** (cap caches, use weak references where appropriate), and be deliberate about cleanup. This is part of why DI containers — which manage bean lifecycle and destruction — are preferred.
</details>

<details>
<summary><strong>Q19: [Coding Challenge] Implement a fully hardened, serialization- and reflection-safe lazy Singleton (class-based, not enum). Complete solution.</strong></summary>

Requirements: lazy, thread-safe, serialization-safe, reflection-guarded, non-cloneable.

<details>
<summary>💻 Complete solution</summary>

```java
import java.io.Serializable;

public final class SafeSingleton implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    // Bill Pugh holder: lazy + thread-safe + lock-free.
    private static class Holder {
        private static final SafeSingleton INSTANCE = new SafeSingleton();
    }

    private SafeSingleton() {
        // Reflection guard: second construction after the holder is built => fail.
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("Use getInstance()");
        }
    }

    public static SafeSingleton getInstance() {
        return Holder.INSTANCE;
    }

    // Serialization guard: return the canonical instance, discard the deserialized copy.
    protected Object readResolve() {
        return getInstance();
    }

    // Cloning guard.
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("SafeSingleton is not cloneable");
    }

    public void doWork() { System.out.println("working on " + System.identityHashCode(this)); }
}
```
</details>

Notes: the holder gives laziness + thread-safety with no locks; `readResolve` seals serialization; the constructor guard seals (most) reflection; `clone()` override seals cloning. Honest caveat I'd state in the interview: the reflection guard isn't 100% airtight, so if reflection-safety is paramount, an **enum** is strictly better — this class-based form is what you use when you *need* laziness or a superclass.
</details>

<details>
<summary><strong>Q20: [Coding Challenge] Design a thread-safe, generic object pool as a singleton (e.g., a bounded connection pool). Complete solution.</strong></summary>

Requirements: single pool per JVM, bounded capacity, block-with-timeout when exhausted, never leak resources.

<details>
<summary>💻 Complete solution</summary>

```java
import java.util.concurrent.*;
import java.util.function.Supplier;

public final class ObjectPool<T> {
    private final BlockingQueue<T> pool;

    private ObjectPool(int size, Supplier<T> factory) {
        pool = new ArrayBlockingQueue<>(size);
        for (int i = 0; i < size; i++) pool.offer(factory.get());   // pre-populate
    }

    // Holder keyed for a specific pool type — here a String connection pool as the singleton.
    private static class Holder {
        static final ObjectPool<String> INSTANCE =
            new ObjectPool<>(10, () -> "conn-" + System.nanoTime());
    }
    public static ObjectPool<String> getInstance() { return Holder.INSTANCE; }

    /** Borrow, waiting up to timeoutMs; throws if still unavailable (back-pressure). */
    public T borrow(long timeoutMs) throws InterruptedException {
        T item = pool.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (item == null) throw new IllegalStateException("Pool exhausted");
        return item;
    }

    /** Always return in a finally block so the resource can't leak. */
    public void giveBack(T item) { if (item != null) pool.offer(item); }

    public int available() { return pool.size(); }
}
```

```java
// Usage under contention:
ExecutorService ex = Executors.newFixedThreadPool(20);
for (int i = 0; i < 100; i++) {
    ex.submit(() -> {
        ObjectPool<String> p = ObjectPool.getInstance();  // same pool everywhere
        String c = null;
        try { c = p.borrow(500); /* use c */ }
        catch (Exception e) { /* handle exhaustion */ }
        finally { p.giveBack(c); }                          // never leak
    });
}
ex.shutdown();
```
</details>

Design points to call out: singleton (holder idiom) guarantees one pool → the "max connections" invariant holds; `ArrayBlockingQueue` gives thread-safe borrow/return **and** built-in blocking/back-pressure; borrow-with-timeout avoids indefinite hangs; `giveBack` in `finally` prevents leaks. This is the real reason Singleton and Object Pool co-occur.
</details>

### 🏛️ Staff / Principal Engineer Deep-Dive

<details>
<summary><strong>SP1: You inherited a codebase littered with <code>getInstance()</code> calls and it's untestable. How do you migrate it toward DI without a big-bang rewrite?</strong></summary>

Strangle it incrementally. (1) **Introduce an interface** for each singleton's responsibility and make the singleton implement it — now call sites *can* depend on the abstraction. (2) **Add constructor parameters** to classes that currently call `getInstance()` internally, defaulting them to the singleton (`this(Logger.getInstance())`) so nothing breaks, but tests can inject a fake. (3) **Adopt a DI container** at the composition root and register the former singletons as singleton-scoped beans; replace `getInstance()` calls with injected fields class-by-class, leaving the static accessor as a temporary bridge (`getInstance()` can delegate to the container). (4) **Delete the static accessor last**, once no code references it. Throughout, add characterization tests as you touch each area. The key Staff move is preserving behavior (the accessor keeps working) while inverting control, so the migration is safe and reviewable in small PRs rather than a risky rewrite.
</details>

<details>
<summary><strong>SP2: When is a Singleton genuinely the right call in a modern microservice, given DI exists?</strong></summary>

Even in a DI world, a hand-rolled singleton is justified when you're **below or outside the container**: (1) **framework/library code** that can't assume a container exists (a logging facade, a metrics client) and needs a well-known access point; (2) **bootstrap/infra objects** created before the container is up (the container itself, early config, a classpath scanner); (3) **JVM-wide facts** that are legitimately process-global (`Runtime`, a shared `ForkJoinPool.commonPool()`-style resource); (4) **hot-path performance** where you want a plain static field read with zero container indirection. Outside those, prefer container-managed singleton scope. The discriminating question I ask: *"Does this need to be reachable without an injected reference, and is one-per-process actually correct?"* If both yes, a Singleton (holder/enum) is appropriate; otherwise it's global state in disguise.
</details>

<details>
<summary><strong>SP3: Explain the Java Memory Model guarantees that make the holder idiom and DCL correct. Be precise.</strong></summary>

Two JLS guarantees do the work. **First, class initialization (JLS §12.4.2)**: the JVM acquires a per-class initialization **lock (LC)** before running `<clinit>`, ensures it runs **exactly once**, and any thread that finds initialization already complete proceeds without re-running it. Critically, the actions of `<clinit>` **happen-before** any subsequent use of the class by other threads — so once `Holder.INSTANCE` is assigned during `Holder`'s init, every thread that reads `Holder.INSTANCE` is guaranteed to see the fully constructed object with correct memory visibility. That's why the holder idiom needs no `volatile`/`synchronized`: the class-init lock provides both atomicity and the happens-before edge.

**Second, `volatile` (JLS §17.4, JSR-133)**: a write to a `volatile` field happens-before every subsequent read of it, and volatile accesses can't be reordered across each other. In DCL, making `instance` volatile means the constructor's writes (sequenced-before the volatile write to `instance`) happen-before any thread's non-null volatile read — so no thread can observe a partially constructed object, and the "publish before construct" reordering is forbidden. Pre-JSR-133 (Java ≤1.4) the model was weaker and `volatile` didn't provide this, which is why DCL was historically labeled broken. `final` fields have their own guarantee (JLS §17.5): correctly constructed `final` fields are visible without synchronization once the constructor finishes — which is why eager `static final` is safe.
</details>

<details>
<summary><strong>SP4: A singleton cache is causing a memory leak / OOM in a long-running server. How do you diagnose and fix it?</strong></summary>

**Diagnose:** take a heap dump (`jmap`/JFR), open in Eclipse MAT, and run "dominator tree" / "path to GC roots" — a leaking singleton shows as a `static` field that is a GC root retaining a growing collection. Confirm the retained set grows over time (compare two dumps). Common culprits: an unbounded `Map` cache, listener/callback lists that are added-to but never removed, `ThreadLocal`s not cleared on thread reuse (pooled threads), or (in app servers) a shared-classloader singleton retaining webapp classes across redeploys (Metaspace leak).

**Fix:** bound the cache (size/TTL — e.g., Caffeine/`LinkedHashMap` LRU) instead of an unbounded map; use **weak/soft references** where entries should be collectible; ensure symmetric register/deregister for listeners; always `remove()` `ThreadLocal`s in a `finally`; and register a **shutdown/lifecycle hook** to release resources. Longer term, move the cache under the DI container's managed lifecycle so it has a defined destruction phase, and add a metric/alert on cache size. The root lesson: a singleton's process-long lifetime turns any unbounded retention into a permanent leak, so singletons must own **bounded** state.
</details>

<details>
<summary><strong>SP5: Critique "Singleton" from a systems-design perspective. When does insisting on one instance actively hurt scalability?</strong></summary>

The pattern optimizes for a **single point of coordination**, which is exactly what limits horizontal scaling. A stateful process-wide singleton becomes a **contention hotspot** (every thread funnels through it — the synchronized-accessor variant literally serializes callers) and, when promoted to a *logical* cluster singleton via leader election, a **SPOF and throughput ceiling** (all work flows through one active node). It also encourages **shared mutable state**, which is the enemy of the shared-nothing architectures that scale.

The Staff-level reframe: most "we need a singleton" requirements are really "we need a **consistent view** or a **single source of truth**," which is better served by **stateless services + an external store** (each node runs its own instance; the DB/Redis/Kafka holds the shared truth and enforces invariants) than by forcing singularity in-process. Reserve true singularity for cases where **exactly one actor must act at a time** (a leader/scheduler/sequencer), and then design explicitly for **failover, fencing tokens** (to prevent a zombie old-leader from acting), and **partition tolerance**. In short: uniqueness is a real requirement sometimes, but "make it a singleton" is often a premature answer that trades scalability for a convenience you didn't actually need.
</details>

---

## ⚡ Quick Revision

**One-liner:** Singleton ensures a class has exactly one instance per JVM and gives every caller a global access point to it, via a private constructor plus a static `getInstance()`.

**The whole pattern in a paragraph:** Some objects must exist exactly once — a connection pool, logger, config, cache — because a duplicate is wrong, not merely wasteful. Singleton makes the class enforce this itself: **hide the constructor** so nothing external can `new` it, hold the instance in a **`static` field**, and expose one **`static` accessor**. The engineering meat is *how you create that one instance safely and lazily under concurrency*. Naive lazy (`if(null) new`) races and makes multiples; `synchronized getInstance()` fixes it but locks on every call; **double-checked locking** locks only on first creation but **requires `volatile`** so threads never see a half-constructed object (the write `instance = new X()` isn't atomic and can be reordered — `volatile` gives the happens-before edge, correct only since the Java 5 memory model). Better still are the two idioms that piggyback on the JVM's guaranteed-once, thread-safe class initialization: the **Bill Pugh holder** (a private static nested class → lazy + thread-safe + lock-free) and the **enum** (thread-safe *and* serialization- and reflection-safe for free, per *Effective Java*, but eager and non-extensible). Beyond concurrency, seal the back doors — **reflection** (guard the constructor / use enum), **serialization** (`readResolve()` / use enum), **cloning** (throw), and beware **multiple classloaders** (one-per-JVM is really one-per-classloader). The headline critique: a Singleton is **global mutable state** — hidden dependencies, hard testing, tight coupling — so in real services prefer a **DI-container-managed singleton bean** and reserve the hand-rolled form for framework/bootstrap code; and remember "one per JVM" ≠ "one per cluster" (that needs leader election / distributed locks).

**Top 5 interview answers to memorize:**

1. **"Best implementation?"** → Enum (thread-, serialization-, reflection-safe for free — *Effective Java*); Bill Pugh holder when you need lazy init or inheritance.
2. **"Why `volatile` in DCL?"** → `instance = new X()` isn't atomic and can be reordered; without `volatile` another thread can see a non-null, half-constructed object. `volatile` forbids the reorder and gives a happens-before guarantee (JMM, Java 5+).
3. **"Why is Bill Pugh thread-safe with no locks?"** → The nested holder class initializes lazily on first reference, and the JVM guarantees class init runs exactly once and thread-safely via the class-init lock.
4. **"How is it broken / hardened?"** → Reflection, serialization, cloning, and multiple classloaders each defeat it; enum closes reflection+serialization automatically, `readResolve`/constructor-guard/`clone`-throw for class-based.
5. **"Downside / anti-pattern?"** → Global mutable state → hidden deps, untestable, tight coupling; prefer dependency injection / container-managed singleton scope.

**Trigger words** (if the interviewer says these, think Singleton): *"exactly one instance," "single shared," "global access point," "one per JVM," "connection pool / logger / config / cache," "lazy initialization," "double-checked locking," "why volatile," "thread-safe instance," "getInstance," "make it a singleton," "one instance across the cluster"* (→ that's *distributed coordination*, not Singleton).


# Proxy Pattern ⭐⭐⭐⭐ (Difficulty: 4/5 — simple structure, but the *variants* and *runtime-proxy internals* are where interviews get deep)

> **Category:** Structural Pattern (GoF)
> **Also known as:** Surrogate

The Proxy pattern provides a **stand-in (surrogate) for another object to control access to it**. The proxy implements the *same interface* as the real object, so clients can't tell the difference — but the proxy gets to decide *when*, *whether*, and *how* the call reaches the real object. It's the pattern behind Hibernate lazy loading, Spring AOP (`@Transactional`), Java RMI stubs, and `java.lang.reflect.Proxy`.

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 0: No Proxy — Client Calls the Real Object Directly (Baseline)](#variant-0-no-proxy--client-calls-the-real-object-directly-baseline)
   - [Variant 1: Virtual Proxy (Lazy Initialization)](#variant-1-virtual-proxy-lazy-initialization)
   - [Variant 2: Protection Proxy (Access Control)](#variant-2-protection-proxy-access-control)
   - [Variant 3: Remote Proxy (Stub over the Network)](#variant-3-remote-proxy-stub-over-the-network)
   - [Variant 4: Smart Reference / Caching / Logging Proxy](#variant-4-smart-reference--caching--logging-proxy)
   - [Variant 5: Dynamic Proxy (java.lang.reflect.Proxy) — Production Idiom](#variant-5-dynamic-proxy-javalangreflectproxy--production-idiom)
5. [🎨 Real-World Example](#-real-world-example)
6. [✅ When to Use](#-when-to-use)
7. [❌ When NOT to Use](#-when-not-to-use)
8. [🎯 Pros and Cons](#-pros-and-cons)
9. [🔄 Comparison with Related/Similar Patterns](#-comparison-with-relatedsimilar-patterns)
10. [📊 Comparison Table (of Variants)](#-comparison-table-of-variants)
11. [💡 Common Pitfalls](#-common-pitfalls)
12. [🎓 Interview Tips](#-interview-tips)
13. [🔗 Related Patterns](#-related-patterns)
14. [📚 Library/Framework Implementation](#-libraryframework-implementation)
15. [📝 Interview Questions & Answers (FAANG Top 20)](#-interview-questions--answers-faang-top-20)
16. [⚡ Quick Revision](#-quick-revision)

---

## 📋 Intent

> **GoF Definition:** *"Provide a surrogate or placeholder for another object to control access to it."*

The Proxy pattern exists to insert a **level of indirection** between a client and a real object (the "real subject") so that some behavior can happen *around* access to that object — **without the client knowing**. The proxy implements the same interface as the real subject and holds a reference to it (or knows how to obtain it). Every call the client makes hits the proxy first; the proxy can then do work *before* forwarding, *decide not to forward at all*, forward to a **remote** or **not-yet-created** object, or do work *after* the real object responds.

The single unifying idea across all proxy flavors is **controlled access**. What "control" means depends on the variant: *deferring creation* (virtual proxy), *checking permissions* (protection proxy), *hiding the network* (remote proxy), or *adding bookkeeping like caching, reference counting, and logging* (smart reference). The structure is identical; only the intent of the control differs.

---

## 🎯 Problem

You have a real object that is **expensive to create, sensitive to access, physically remote, or in need of extra bookkeeping** on every use. You could bake that concern *into the object itself* — but that violates the Single Responsibility Principle, couples the object to policy it shouldn't own, and forces every client to deal with it. Alternatively you could make every *client* handle it — but that scatters the logic and is easy to get wrong.

**The pain points that lead you to Proxy:**

- Creating the real object is **costly** (loads a huge file, opens a DB connection, spins up a model), but the client may not even use it — you want to **defer** creation until first real use.
- The real object must be **guarded** — not every caller is allowed to invoke every method (role/permission checks), and you don't want that logic inside the domain object.
- The real object lives in **another process, JVM, or machine** — the client wants a local-looking handle while the proxy hides serialization, connection, and transport.
- You need **cross-cutting bookkeeping** on every access — caching results, logging/auditing, counting references, lazy-locking, retry — but the real object shouldn't know about any of it.

**Concrete example scenarios:**

1. **Lazy-loading a heavyweight resource.** A `HighResImage` reads a 40 MB file from disk. A document with 100 images shouldn't load all of them on open; a `ProxyImage` shows a placeholder and loads the real bytes only when `display()` is first called. (The GoF book's own example.)

2. **Access control / security.** A `BankAccount` service where `withdraw()` must be blocked for read-only auditor roles. A protection proxy checks the caller's role before delegating, keeping authorization out of the account's business logic.

3. **Remote service stub.** Java RMI, gRPC stubs, or a REST client: the client holds a `UserService` reference and calls `getUser(1)` as if local; the proxy marshals the call, sends it over the wire, and unmarshals the response.

4. **ORM lazy associations.** Hibernate returns a proxy for `order.getCustomer()`; the `Customer` row isn't fetched until you actually touch a field, avoiding an expensive join you may not need.

---

## ✅ Solution

The core idea, in plain language: **create a surrogate class that implements the same interface as the real object and holds (or can obtain) a reference to it. Clients talk to the surrogate. The surrogate adds access-control logic around delegating to the real object.** Because the proxy *is* the interface, the client is oblivious — substitution is transparent (Liskov holds).

**Key structural elements:**

- **Subject (interface / abstract class):** the common contract shared by the real object and the proxy. This is what makes the proxy substitutable and transparent — the client depends only on `Subject`.
- **RealSubject:** the actual object that does the real work (`RealImage`, the real `UserService`, the DB-backed entity). It's kept focused on its job, ignorant of the proxy.
- **Proxy:** implements `Subject`, holds a reference to a `RealSubject` (created eagerly, lazily, or resolved remotely), and **controls access** — it may create the real subject on demand, check permissions, forward over a network, cache, log, or count references, then delegate.
- **Client:** depends only on the `Subject` interface and cannot tell whether it's holding a proxy or the real object.

**The mechanism that makes it work:** *interface substitution + indirection*. Because both proxy and real subject share the `Subject` type, the proxy can be returned wherever the real subject is expected. The proxy owns the *decision* of if/when/how delegation happens — this is the difference from Decorator (which always delegates and only *adds* behavior). A proxy may **short-circuit** (deny access, serve from cache, throw), **defer** (create the real subject lazily — often guarded by `volatile`/synchronization for thread safety), or **relocate** (send the call to another JVM). The real subject stays a Single-Responsibility object; the *access policy* lives entirely in the proxy.

---

## 💻 Implementation

We'll model a **document viewer that displays high-resolution images** — a FAANG-favorite because it makes the "expensive resource you may not need" trade-off concrete, and it's literally the GoF's own motivating example. Each variant demonstrates a *different flavor* of proxy (they're not strictly "worse → better"; rather naive → the full taxonomy → the production idiom that subsumes them all via reflection).

### Variant 0: No Proxy — Client Calls the Real Object Directly (Baseline)

**What's wrong with it:** Before adding a proxy, understand the disease it cures. If the client news-up the expensive `RealImage` directly, the 40 MB file is loaded from disk **at construction time** — even for images that scroll off-screen and are never displayed. Open a 100-image document and you pay 100 disk loads up front. There's no place to hang access control, caching, or remoting either — every client would have to add that itself.

<details>
<summary>💻 Click to expand code — the baseline (eager, no indirection)</summary>

```java
interface Image {
    void display();
}

// The expensive real object: loading happens in the constructor.
class RealImage implements Image {
    private final String filename;

    RealImage(String filename) {
        this.filename = filename;
        loadFromDisk();               // <-- expensive work done EAGERLY
    }

    private void loadFromDisk() {
        System.out.println("Loading 40MB from disk: " + filename);
        // ...simulate heavy I/O...
    }

    @Override public void display() {
        System.out.println("Displaying " + filename);
    }
}

class DocumentNoProxy {
    public static void main(String[] args) {
        // All 3 loaded from disk immediately, even if we only ever display one.
        Image a = new RealImage("photo1.raw");  // loads now
        Image b = new RealImage("photo2.raw");  // loads now
        Image c = new RealImage("photo3.raw");  // loads now
        a.display();                            // only this one is ever shown
    }
}
```
</details>

**Pros:** Dead simple; no indirection; direct type — `instanceof RealImage` works.
**Cons:** Eager loading wastes I/O/memory for objects that may never be used; nowhere to put access control, caching, logging, or remoting without polluting either the client or `RealImage`.
**Mechanism:** none — the client is coupled directly to the concrete expensive class. This is exactly the coupling and eagerness that a proxy removes.

### Variant 1: Virtual Proxy (Lazy Initialization)

**What problem it solves:** Defer the expensive construction of `RealImage` until the first time it's actually needed (first `display()`). The client still gets an `Image` immediately and cheaply; the 40 MB load happens **on demand**, and only for images that are truly shown. This is the single most common proxy flavor in interviews.

<details>
<summary>💻 Click to expand code — virtual (lazy-loading) proxy</summary>

```java
class ProxyImage implements Image {
    private final String filename;
    private RealImage realImage;      // created lazily, only on first display()

    ProxyImage(String filename) {
        this.filename = filename;     // cheap: just remember the name, no disk I/O
    }

    @Override public void display() {
        if (realImage == null) {                 // first real use -> create now
            realImage = new RealImage(filename);  // expensive load happens HERE
        }
        realImage.display();                      // delegate
    }
}

class DocumentVirtualProxy {
    public static void main(String[] args) {
        Image a = new ProxyImage("photo1.raw");   // no disk load
        Image b = new ProxyImage("photo2.raw");   // no disk load
        a.display();   // NOW loads photo1, then displays
        a.display();   // already loaded -> just displays (no reload)
        // photo2 was never displayed -> never loaded. That's the win.
    }
}
```
</details>

**Pros:** Avoids paying for objects that are never used; caches the created instance so subsequent calls are cheap; client is unchanged and unaware.
**Cons:** The shown lazy-init is **not thread-safe** — two threads calling `display()` concurrently can both see `realImage == null` and each construct a `RealImage` (double load). First call also has a latency spike.
**Mechanism:** *lazy instantiation via a null-check guard*. The proxy holds the identity (`filename`) cheaply and only materializes the heavy `RealImage` on first access. For concurrent use, this guard must be hardened (see Variant 4 and the double-checked-locking pitfall).

### Variant 2: Protection Proxy (Access Control)

**What problem it solves:** Not every caller is allowed to invoke every operation. A protection proxy checks the caller's **role/permission** before delegating, keeping authorization logic *out* of the domain object. Here a bank account: auditors can `getBalance()` but must be denied `withdraw()`.

<details>
<summary>💻 Click to expand code — protection proxy</summary>

```java
interface BankAccount {
    void withdraw(double amount);
    double getBalance();
}

class RealBankAccount implements BankAccount {
    private double balance;
    RealBankAccount(double opening) { this.balance = opening; }
    @Override public void withdraw(double amount) {
        balance -= amount;
        System.out.println("Withdrew " + amount + ", balance=" + balance);
    }
    @Override public double getBalance() { return balance; }
}

enum Role { OWNER, AUDITOR }

class ProtectedBankAccount implements BankAccount {
    private final BankAccount real;
    private final Role callerRole;

    ProtectedBankAccount(BankAccount real, Role callerRole) {
        this.real = real;
        this.callerRole = callerRole;
    }

    @Override public void withdraw(double amount) {
        if (callerRole != Role.OWNER) {                       // access CONTROL
            throw new SecurityException("Auditors cannot withdraw");
        }
        real.withdraw(amount);                                // delegate only if allowed
    }

    @Override public double getBalance() {
        return real.getBalance();   // reads allowed for everyone
    }
}

class ProtectionProxyDemo {
    public static void main(String[] args) {
        BankAccount real = new RealBankAccount(1000);
        BankAccount owner   = new ProtectedBankAccount(real, Role.OWNER);
        BankAccount auditor = new ProtectedBankAccount(real, Role.AUDITOR);

        owner.withdraw(100);                 // OK -> balance 900
        System.out.println(auditor.getBalance()); // OK -> 900
        auditor.withdraw(50);                // throws SecurityException
    }
}
```
</details>

**Pros:** Authorization is centralized in the proxy and completely absent from `RealBankAccount` (SRP); different proxies can enforce different policies over the same real object; easy to unit-test policy in isolation.
**Cons:** The real object is still directly instantiable, so the proxy only protects if clients are *forced* to go through it (typically via a factory/DI that never hands out the raw object); policy and object can drift if the interface changes.
**Mechanism:** *pre-delegation guard on selected methods*. The proxy shares the `BankAccount` type but conditionally **refuses to delegate** based on caller context — the defining "control access" behavior of Proxy, in contrast to Decorator which never refuses.

### Variant 3: Remote Proxy (Stub over the Network)

**What problem it solves:** The real object lives in another JVM/process/host. A remote proxy (a.k.a. *stub*) gives the client a local-looking `Subject` while hiding connection setup, request marshaling, transport, and response unmarshaling — the foundation of RMI, gRPC stubs, and REST clients.

<details>
<summary>💻 Click to expand code — remote proxy (stub) sketch</summary>

```java
interface UserService {
    String getUser(long id);
}

// Runs on a different machine in reality; shown local for illustration.
class RealUserService implements UserService {
    @Override public String getUser(long id) {
        return "User#" + id;   // would hit a DB in the remote process
    }
}

// The client holds THIS and thinks it's calling a local service.
class UserServiceRemoteProxy implements UserService {
    private final String host;
    private final int port;

    UserServiceRemoteProxy(String host, int port) { this.host = host; this.port = port; }

    @Override public String getUser(long id) {
        // 1) open/reuse connection to host:port
        // 2) MARSHAL the method name + args into bytes
        String request = "getUser|" + id;
        // 3) send over socket, block for response
        String response = sendOverNetwork(host, port, request);
        // 4) UNMARSHAL response back into a return value
        return response;
    }

    private String sendOverNetwork(String host, int port, String request) {
        // real impl: Socket / HTTP / gRPC channel; here we simulate.
        System.out.println("RPC -> " + host + ":" + port + "  " + request);
        return "User#" + request.substring(request.indexOf('|') + 1);
    }
}

class RemoteProxyDemo {
    public static void main(String[] args) {
        UserService svc = new UserServiceRemoteProxy("10.0.0.7", 8080);
        System.out.println(svc.getUser(42)); // looks local; actually a remote call
    }
}
```
</details>

**Pros:** Location transparency — the client codes against a local interface; network details are fully encapsulated; the transport can change (RMI→gRPC) without touching client code.
**Cons:** Hides the fact that calls are slow, can fail, and can time out — "location transparency" is a leaky abstraction (the Fallacies of Distributed Computing); needs serialization, error handling, and resilience the local case never had.
**Mechanism:** *marshaling + transport indirection*. The proxy translates an in-JVM method call into a wire message and back, so the same `Subject` interface spans a process boundary. gRPC/RMI generate this stub for you.

### Variant 4: Smart Reference / Caching / Logging Proxy

**What problem it solves:** Add cross-cutting bookkeeping on every access — cache results, log/audit, count references, or lazily lock — without touching the real object. This variant also shows the **thread-safe** lazy init (fixing Variant 1's race) with **double-checked locking + `volatile`**.

<details>
<summary>💻 Click to expand code — thread-safe smart-reference/caching proxy</summary>

```java
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

interface WeatherApi {
    String getForecast(String city);   // expensive remote call
}

class RealWeatherApi implements WeatherApi {
    @Override public String getForecast(String city) {
        sleep(200);                     // simulate slow network
        return city + ": 24C sunny";
    }
    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

class SmartWeatherProxy implements WeatherApi {
    // Thread-safe DCL: 'real' is volatile so publication is visible across threads.
    private volatile RealWeatherApi real;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    private final AtomicLong calls = new AtomicLong();     // smart-reference bookkeeping

    private RealWeatherApi real() {
        RealWeatherApi r = real;                 // read volatile once (local)
        if (r == null) {                         // 1st check (no lock, fast path)
            synchronized (this) {
                r = real;
                if (r == null) {                 // 2nd check (locked)
                    r = real = new RealWeatherApi();
                }
            }
        }
        return r;
    }

    @Override public String getForecast(String city) {
        calls.incrementAndGet();                             // logging/metrics
        // caching: atomic check-then-load per key, delegates only on miss
        return cache.computeIfAbsent(city, c -> real().getForecast(c));
    }

    long callCount() { return calls.get(); }
    int cacheSize()  { return cache.size(); }
}
```
</details>

**Pros:** Caching turns repeated expensive calls into O(1) map hits; metrics/logging live in one place; lazy `RealWeatherApi` creation is now **thread-safe**; `computeIfAbsent` gives atomic per-key load so concurrent misses don't stampede the backend.
**Cons:** More moving parts; cache introduces staleness/eviction concerns; a caching proxy that returns mutable objects can leak shared state.
**Mechanism:** *double-checked locking with `volatile` + concurrent caching*. `volatile` on `real` guarantees the **Java Memory Model happens-before** relationship so a partially-constructed object can never be observed (the classic pre-Java-5 DCL bug); reading `volatile` into a local `r` avoids re-reading it. `ConcurrentHashMap.computeIfAbsent` provides atomic "check then load" per key.

### Variant 5: Dynamic Proxy (java.lang.reflect.Proxy) — Production Idiom

**Why it's recommended:** Writing a hand-coded proxy class per interface is boilerplate that explodes when you need the *same* cross-cutting concern (logging, timing, transactions, retry) across many interfaces. `java.lang.reflect.Proxy` **generates a proxy class at runtime** implementing any set of interfaces, routing every call through a single `InvocationHandler`. This is the exact machinery behind Spring AOP, MyBatis mappers, and RMI stubs.

<details>
<summary>💻 Click to expand code — JDK dynamic proxy with a reusable handler</summary>

```java
import java.lang.reflect.*;

interface OrderService {
    String placeOrder(String item);
    String cancelOrder(String id);
}

class RealOrderService implements OrderService {
    @Override public String placeOrder(String item) { return "placed:" + item; }
    @Override public String cancelOrder(String id)  { return "cancelled:" + id; }
}

// One handler adds timing+logging to ANY interface's methods.
class LoggingTimingHandler implements InvocationHandler {
    private final Object target;
    LoggingTimingHandler(Object target) { this.target = target; }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long start = System.nanoTime();
        System.out.println("-> " + method.getName() + Arrays.toString(args));
        try {
            Object result = method.invoke(target, args);        // delegate via reflection
            return result;
        } catch (InvocationTargetException e) {
            throw e.getCause();          // unwrap so caller sees the real exception
        } finally {
            long us = (System.nanoTime() - start) / 1000;
            System.out.println("<- " + method.getName() + " took " + us + "us");
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T wrap(T target, Class<T> iface) {
        return (T) Proxy.newProxyInstance(
            iface.getClassLoader(),
            new Class<?>[]{ iface },
            new LoggingTimingHandler(target));
    }
}

class DynamicProxyDemo {
    public static void main(String[] args) {
        OrderService svc = LoggingTimingHandler.wrap(new RealOrderService(), OrderService.class);
        System.out.println(svc.placeOrder("book"));
        System.out.println(svc.cancelOrder("A17"));
        // Every call is transparently logged + timed, no hand-written proxy class.
    }
}
```
</details>

**Pros:** Zero per-interface boilerplate; one handler applies a concern across many types; the foundation of AOP and framework magic; interfaces can be added/changed freely.
**Cons:** **Only proxies interfaces** (for classes you need CGLIB/ByteBuddy subclassing); reflection has overhead and produces noisy stack traces; `InvocationTargetException` must be unwrapped; harder to debug ("where did this behavior come from?").
**Mechanism:** *runtime bytecode generation + reflective dispatch*. `Proxy.newProxyInstance` synthesizes a class implementing the given interfaces whose every method forwards to `InvocationHandler.invoke(proxy, method, args)`. Because dispatch is reflective, one handler handles arbitrary signatures — this is Proxy generalized to "interception."

---

## 🎨 Real-World Example

The definitive real-world Proxy is **ORM lazy loading** — exactly how Hibernate returns a proxy for a lazily-fetched association so the database row isn't hit until you actually touch it. Below is a **complete, production-realistic example**: a lazy-loading, caching, access-checked `ProductRepository` proxy of the shape used in FAANG service tiers, with a demo that exercises multiple call sites and a concurrency test proving thread safety.

<details>
<summary>💻 Click to expand full real-world example (lazy + caching + protection proxy with demo + concurrency test)</summary>

```java
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

// ---------- Subject ----------
interface ProductRepository {
    String findById(long id);   // simulates a slow backing store
}

// ---------- RealSubject: expensive to construct AND to query ----------
class DatabaseProductRepository implements ProductRepository {
    private final Map<Long, String> table;

    DatabaseProductRepository() {
        // Expensive construction: open pool, warm caches, etc.
        System.out.println("[RealSubject] opening DB connection pool (expensive)...");
        sleep(100);
        table = Map.of(1L, "Keyboard", 2L, "Monitor", 3L, "Mouse");
    }

    @Override public String findById(long id) {
        sleep(50);                    // simulate query latency
        return table.get(id);
    }
    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

// ---------- Proxy: lazy creation + caching + read-only protection ----------
class ProductRepositoryProxy implements ProductRepository {
    private volatile DatabaseProductRepository real;                 // lazy, thread-safe
    private final ConcurrentMap<Long, String> cache = new ConcurrentHashMap<>();
    private final AtomicLong dbHits = new AtomicLong();
    private final boolean readOnlyAllowed;

    ProductRepositoryProxy(boolean readOnlyAllowed) { this.readOnlyAllowed = readOnlyAllowed; }

    // Double-checked locking so the expensive RealSubject is built at most once.
    private DatabaseProductRepository real() {
        DatabaseProductRepository r = real;
        if (r == null) {
            synchronized (this) {
                r = real;
                if (r == null) r = real = new DatabaseProductRepository();
            }
        }
        return r;
    }

    @Override public String findById(long id) {
        if (!readOnlyAllowed && id < 0) {          // trivial protection example
            throw new SecurityException("access denied for id " + id);
        }
        // Cache shields the expensive DB; computeIfAbsent is atomic per key.
        return cache.computeIfAbsent(id, key -> {
            dbHits.incrementAndGet();
            return real().findById(key);
        });
    }

    long dbHits() { return dbHits.get(); }
}

// ---------- Demo + concurrency test ----------
public class ProxyRealWorldDemo {
    public static void main(String[] args) throws InterruptedException {
        ProductRepositoryProxy repo = new ProductRepositoryProxy(true);

        // (1) No DB work happened at construction — proxy is cheap. Prove laziness:
        System.out.println("Proxy created; DB not opened yet.");

        // (2) First real access triggers lazy DB creation + a query.
        System.out.println(repo.findById(1)); // opens DB, hits DB -> Keyboard
        System.out.println(repo.findById(1)); // served from cache -> Keyboard (no DB hit)

        // (3) Concurrency test: 100 threads hammer 3 keys.
        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            long id = (i % 3) + 1;
            pool.submit(() -> {
                try { start.await(); repo.findById(id); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                finally { done.countDown(); }
            });
        }
        start.countDown();   // release all at once
        done.await();
        pool.shutdown();

        // Despite 100+ logical reads across 3 keys, the DB is hit at most 3 times
        // (once per distinct key) -> proves cache + thread-safe lazy init work.
        System.out.println("Distinct DB hits = " + repo.dbHits()); // ~3
    }
}
```
</details>

**Why this is a strong FAANG answer:** it combines the three most-asked proxy flavors in one believable object — **virtual** (lazy, thread-safe DB creation via DCL+`volatile`), **smart reference** (caching + hit counting), and **protection** (access check) — while keeping `DatabaseProductRepository` a clean Single-Responsibility object. The concurrency test *proves* the invariants (DB opened once, hit at most once per key) rather than just asserting them — exactly the rigor interviewers reward.

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- **Virtual proxy:** the real object is **expensive to create** (large file, DB connection, ML model) and may not be used — defer creation until first real access.
- **Protection proxy:** you need **access control** (role/permission checks) that shouldn't live inside the domain object.
- **Remote proxy:** the real object is in **another process/JVM/host** and you want a local-looking handle that hides the network (RMI, gRPC, REST clients).
- **Smart reference:** you need **cross-cutting bookkeeping** on every access — caching, logging/auditing, reference counting, lazy locking, retry/circuit-breaking.
- You want to add these concerns **transparently**, keeping the client and the real object unchanged (client depends only on the interface).
- You need **framework-level interception** across many types — use a *dynamic proxy* so one handler covers all (transactions, security, tracing).

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- **The real object is cheap and always used.** Lazy/virtual proxying adds indirection for no benefit — just use the object directly.
- **You're actually *adding* behavior, not *controlling access*.** If every call is always delegated and you're layering features, that's **Decorator**, not Proxy — using "Proxy" muddies intent.
- **You need to change the interface.** That's **Adapter**; a proxy must keep the same interface to stay transparent.
- **Latency/identity are critical and must be visible.** A remote proxy hides that calls can be slow and fail; if callers must reason about that explicitly, a transparent proxy is a leaky, dangerous abstraction.
- **Only one trivial concern, applied once.** A full dynamic-proxy/AOP setup for a single log statement is over-engineering — a plain method call or one hand-written wrapper is simpler.
- **Hot paths where indirection/reflection cost matters** and profiling shows the proxy is the bottleneck.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- **Controls access** to the real object transparently — the client is unaware and unchanged (Open/Closed).
- **Lazy loading** avoids paying for objects that are never used; **caching** avoids repeated expensive work.
- Keeps the real object a **Single-Responsibility** object — policy (security, logging, remoting) lives in the proxy.
- Manages the real object's **lifecycle** (create on demand, reference count, dispose) without the client knowing.
- With **dynamic proxies**, one interceptor applies a concern across many types (the basis of AOP).

**Cons**

- Adds a **layer of indirection** — more classes, more latency, sometimes a first-call spike (lazy init).
- Can **hide important truths** — a remote proxy masks that calls fail/timeout; a caching proxy masks staleness.
- Breaks **object identity** and `instanceof`/`getClass()` against the concrete real subject (a proxy is not the real type).
- **Thread-safety** of lazy init is easy to get wrong (needs `volatile` + double-checked locking).
- Dynamic proxies bring **reflection overhead**, noisy stack traces, and interface-only (JDK) constraints.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

All four "wrapper-ish" patterns share structure (an object holding a reference to another) but differ in **intent** — the classic interview trap.

| Pattern | Intent | Interface vs. wrapped | Who controls delegation | Key tell |
|---|---|---|---|---|
| **Proxy** | **Control access** to an object (lazy, remote, security, caching) | **Same** interface | The proxy may **refuse/defer/relocate** | Controls access; may not delegate at all |
| **Decorator** | **Add/augment** responsibilities transparently | **Same** interface | Always delegates, **adds** around it | Enhances behavior; stackable |
| **Adapter** | **Convert** one interface into another | **Different** interface | Always delegates (translating) | Makes incompatible things fit |
| **Facade** | **Simplify** a complex subsystem | **New, simpler** interface | Delegates to many objects | One door to a subsystem |

Concise contrasts: **Proxy vs. Decorator** — structurally near-identical (same interface, holds a reference), but Decorator's job is to *add behavior and always delegate*, while Proxy's job is to *control access and may choose not to delegate* (deny, serve from cache, defer creation). Intent, not structure, distinguishes them. **Proxy vs. Adapter** — Adapter deliberately *changes* the interface to bridge incompatibility; Proxy deliberately *keeps* it to stay transparent. **Proxy vs. Facade** — Facade exposes a *new, simplified* interface over *many* objects; a proxy mirrors *one* object's *existing* interface. A proxy usually manages the *same* object's lifecycle; a facade just routes to a subsystem.

</details>

## 📊 Comparison Table (of Variants)

<details>
<summary>📖 Click to expand</summary>

| Variant | Primary intent | Lazy creation? | Thread-safe (as shown)? | Adds network? | Typical real use |
|---|---|---|---|---|---|
| **V0: No proxy** | — (baseline) | No (eager) | N/A | No | Direct object use |
| **V1: Virtual** | Defer expensive creation | Yes | ❌ No (null-check race) | No | Lazy image/entity load |
| **V2: Protection** | Access control | No | Depends on real object | No | Role/permission gating |
| **V3: Remote** | Location transparency | Connection lazy | Depends on transport | **Yes** | RMI / gRPC / REST stub |
| **V4: Smart reference** | Caching/logging/ref-count | Yes | ✅ Yes (DCL + `volatile`) | No | Cache + metrics layer |
| **V5: Dynamic proxy** | Generic interception | Handler-defined | Handler-defined | Handler-defined | Spring AOP, MyBatis, RMI stubs |

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

### Pitfall 1: Non-thread-safe lazy initialization (the double-checked-locking trap)

**What goes wrong:** A virtual proxy that lazily creates the real subject with a plain `if (real == null)` has a **race**: two threads can both see `null` and each construct the real subject (double load), and — worse — without `volatile`, a thread can observe a **partially constructed** object because the JMM allows the write of the reference to be reordered *before* the constructor finishes.

<details>
<summary>💻 Click to expand — the failure</summary>

```java
class BrokenLazyProxy implements Image {
    private RealImage real;                  // NOT volatile -> unsafe publication
    private final String file;
    BrokenLazyProxy(String file) { this.file = file; }
    @Override public void display() {
        if (real == null) {                  // two threads can both enter here
            real = new RealImage(file);      // double construction; reordering hazard
        }
        real.display();                      // may see a half-built RealImage
    }
}
```
</details>

<details>
<summary>💻 Click to expand — the fix</summary>

```java
class SafeLazyProxy implements Image {
    private volatile RealImage real;         // volatile => happens-before on publication
    private final String file;
    SafeLazyProxy(String file) { this.file = file; }
    @Override public void display() {
        RealImage r = real;                  // read volatile once into a local
        if (r == null) {
            synchronized (this) {
                r = real;
                if (r == null) r = real = new RealImage(file);  // built at most once
            }
        }
        r.display();
    }
}
// Alternative fixes: an initialization-on-demand holder class, or a Supplier<Memoized>.
```
</details>

### Pitfall 2: Confusing Proxy with Decorator (wrong intent)

**What goes wrong:** Because the structure is identical, engineers label a *feature-adding wrapper* a "Proxy" (or vice versa). This misleads readers about whether the wrapper can **refuse** a call. A Proxy's contract allows short-circuiting (deny, cache-hit, lazy); a Decorator's contract is "always delegate, add around." Mislabeling causes callers to make wrong assumptions.

<details>
<summary>💻 Click to expand — the tell</summary>

```java
// PROXY: may NOT delegate — this is access control.
public String find(long id) {
    if (!allowed()) throw new SecurityException(); // short-circuit, no delegation
    return real.find(id);
}
// DECORATOR: ALWAYS delegates, only adds behavior around it.
public String find(long id) {
    log(id);
    String r = inner.find(id);   // always calls through
    return enrich(r);
}
```
</details>

### Pitfall 3: Remote proxy hides failure and latency (leaky abstraction)

**What goes wrong:** Treating a remote call as if it were local. "Location transparency" hides that the call can be slow, can time out, can fail partially, and serializes arguments — the Fallacies of Distributed Computing. Code written assuming local semantics has no timeouts, no retries, and no error handling, and falls over in production.

<details>
<summary>💻 Click to expand — the fix (make remote-ness explicit where it matters)</summary>

```java
// Don't pretend it's free. Add timeouts, retries, and circuit breaking in the proxy.
@Override public String getUser(long id) {
    return executeWithTimeout(2000 /*ms*/, () ->
        retry(3, () -> transport.call("getUser", id)));   // resilience lives in the proxy
}
// And propagate a checked/again-retryable exception rather than swallowing failures.
```
</details>

### Pitfall 4: Dynamic proxy leaks `InvocationTargetException` and breaks on `equals`/`hashCode`/`toString`

**What goes wrong:** `method.invoke(target, args)` wraps any exception thrown by the target in an `InvocationTargetException`; if you don't unwrap it, callers see the wrong exception type. Also, `Object` methods (`equals`, `hashCode`, `toString`) flow through `invoke` too, so a naive handler can produce broken equality or infinite recursion.

<details>
<summary>💻 Click to expand — the fix</summary>

```java
@Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // Handle Object methods explicitly to avoid surprises:
    if (method.getName().equals("toString") && args == null)
        return "Proxy(" + target + ")";
    try {
        return method.invoke(target, args);
    } catch (InvocationTargetException e) {
        throw e.getCause();      // unwrap so the caller sees the REAL exception
    }
}
```
</details>

### Pitfall 5: Proxy breaks identity and `instanceof`

**What goes wrong:** `proxy == realObject` is false and `proxy instanceof RealSubject` is false (a JDK dynamic proxy is a `$ProxyN` class implementing the interface, not the real class). Code that branches on concrete type or relies on identity silently misbehaves through a proxy — the same wart as Decorator.

<details>
<summary>💻 Click to expand — the fix</summary>

```java
// Don't type-check the concrete class; depend on the interface.
// If you must reach the target, expose an explicit unwrap (JDBC does exactly this):
interface Unwrappable { <T> T unwrap(Class<T> type); }
// java.sql.Wrapper.unwrap(...) exists precisely because proxies hide the target.
```
</details>

</details>

## 🎓 Interview Tips

**What interviewers commonly ask:**

<details>
<summary>📖 Click to expand</summary>

- "What's the difference between Proxy and Decorator?" (The #1 question — answer with *intent*: control access vs. add behavior; a proxy may refuse to delegate.)
- "Name the types of proxy." (Virtual/lazy, Protection, Remote, Smart reference — and dynamic proxies as the runtime generalization.)
- "How does Hibernate lazy loading work?" (A proxy for the association; the row is fetched on first field access; leads to `LazyInitializationException` if the session is closed.)
- "Implement a lazy-loading image proxy" or "a caching proxy" — then "make it thread-safe" (double-checked locking + `volatile`).
- "How does Spring AOP / `@Transactional` work?" (JDK dynamic proxy for interfaces, CGLIB subclass proxy otherwise; the proxy opens/commits the transaction around the call.)
- "Draw the UML" — Subject interface, RealSubject, Proxy (implements Subject, has-a RealSubject), Client depends on Subject.

</details>

**What you should proactively mention even if not asked:**

<details>
<summary>📖 Click to expand</summary>

- **Same interface, controls access** — this is the one-line essence; contrast with Decorator's "adds behavior, always delegates."
- **Thread safety of lazy init** — bring up `volatile` + double-checked locking and *why* the JMM makes plain DCL unsafe (reordering / unsafe publication) unprompted; it signals depth.
- **JDK dynamic proxy vs. CGLIB/ByteBuddy** — JDK proxies interfaces only via `InvocationHandler`; CGLIB subclasses concrete classes (so the class/methods can't be `final`). Spring picks automatically.
- **The self-invocation gotcha** in Spring AOP — a method calling `this.other()` bypasses the proxy, so `@Transactional`/`@Cacheable` on `other()` won't fire.
- **Remote proxies are a leaky abstraction** — mention the Fallacies of Distributed Computing; add timeouts/retries/circuit breakers.
- **Proxy breaks identity/`instanceof`** — cite JDBC's `unwrap()`/`java.sql.Wrapper` as the industry escape hatch.
- **Relationship to AOP** — Proxy is the mechanism; AOP (cross-cutting concerns applied declaratively) is the architecture built on it.

</details>

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Decorator** — same structure (same interface, holds a reference), but *adds* behavior and always delegates; Proxy *controls access* and may not. Often conflated.
- **Adapter** — also wraps, but *changes* the interface to bridge incompatibility; Proxy keeps it.
- **Facade** — provides a new *simpler* interface over a *whole subsystem*; a proxy mirrors *one* object's existing interface.
- **Decorator + Proxy stacks** — resilience libraries chain both (a caching proxy wrapped by a retry decorator, etc.).
- **Chain of Responsibility** — middleware/interceptor chains generalize proxy interception across many handlers.
- **Flyweight** — a proxy can front shared flyweight objects, controlling access to a pool.
- **Dynamic Proxy / AOP** — reflection-based generalization (`java.lang.reflect.Proxy`, CGLIB, Spring AOP) that synthesizes proxies at runtime.
- **Factory / DI container** — usually *hand out the proxy* instead of the real object, so clients are forced through it (essential for protection proxies).

</details>

## 📚 Library/Framework Implementation

**1. Hibernate / JPA lazy loading (virtual proxy).** For a `@ManyToOne(fetch = LAZY)` association, Hibernate returns a runtime-generated proxy (via ByteBuddy) subclassing the entity. The real row is fetched from the DB on the *first* method/field access. If the persistence session is already closed at that point, you get the infamous `LazyInitializationException`.

<details>
<summary>💻 Click to expand — Hibernate lazy proxy</summary>

```java
@Entity class Order {
    @Id long id;
    @ManyToOne(fetch = FetchType.LAZY)
    Customer customer;          // Hibernate injects a PROXY here
}

// Usage:
Order o = session.get(Order.class, 1L);   // customer NOT loaded yet (proxy)
String name = o.getCustomer().getName();  // <- first access triggers the SELECT
// If the session was closed before this line -> LazyInitializationException.
```
</details>

**2. Spring AOP (`@Transactional`, `@Cacheable`) — dynamic proxy.** Spring wraps beans in a proxy: a **JDK dynamic proxy** if the bean implements interfaces, otherwise a **CGLIB** subclass proxy. The proxy runs the advice (open/commit transaction, check cache) around the target method. Because it's a proxy, **self-invocation** (`this.foo()`) bypasses the advice.

<details>
<summary>💻 Click to expand — Spring transactional proxy</summary>

```java
@Service
class PaymentService {
    @Transactional                       // proxy opens a tx, commits/rolls back around this call
    public void charge(String acct, long cents) {
        // ...debit / insert ledger row...
    }

    public void batch() {
        charge("A", 100);   // SELF-INVOCATION -> goes through 'this', NOT the proxy:
                            // @Transactional here would NOT start a new transaction!
    }
}
// Fix: call via an injected reference to the proxied bean, or restructure.
```
</details>

**3. `java.lang.reflect.Proxy` + RMI stubs (remote/dynamic proxy).** The JDK's own dynamic-proxy API generates interface proxies routed through an `InvocationHandler`; Java RMI uses this to create client-side **stubs** that marshal calls across the network. MyBatis mapper interfaces are also backed by dynamic proxies.

<details>
<summary>💻 Click to expand — JDK Proxy backing a "remote" call</summary>

```java
UserService svc = (UserService) Proxy.newProxyInstance(
    UserService.class.getClassLoader(),
    new Class<?>[]{ UserService.class },
    (proxy, method, args) -> {
        // marshal method + args, send over the wire, unmarshal the response
        return transport.invoke(method.getName(), args);   // RMI/gRPC do this for you
    });
System.out.println(svc.getUser(1)); // looks local; actually remote
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1: [Conceptual] What is the Proxy pattern and what problem does it solve?</strong></summary>

The Proxy is a **structural** GoF pattern that **provides a surrogate or placeholder for another object to control access to it**. The proxy implements the **same interface** as the real subject and holds a reference to it, so clients talk to the proxy exactly as they would to the real object — but the proxy gets to run logic *before*, *instead of*, or *after* forwarding the call.

The problem it solves is that you often need to do something *around* access to an object without changing the object or the client: defer its expensive creation (virtual proxy), enforce permissions (protection proxy), hide the fact that it lives on another machine (remote proxy), or add bookkeeping like caching, logging, and reference-counting (smart reference). The unifying idea is **controlled access via a same-interface stand-in** — the client stays oblivious.
</details>

<details>
<summary><strong>Q2: [Conceptual] Name the participants and the four canonical proxy types.</strong></summary>

**Participants:** the **Subject** (common interface shared by proxy and real object), the **RealSubject** (the actual object doing the work), the **Proxy** (implements Subject, holds/obtains a reference to RealSubject, controls access), and the **Client** (codes against Subject, unaware whether it holds a proxy or the real thing).

**Four canonical types:**
1. **Virtual proxy** — lazily creates/loads an expensive object on first use (e.g., Hibernate lazy loading, image placeholders).
2. **Protection proxy** — checks access rights before forwarding (authorization).
3. **Remote proxy** — local stand-in for an object in another address space/machine; marshals calls over the wire (RMI/gRPC stubs).
4. **Smart reference (smart proxy)** — adds extra behavior on access: caching, logging, reference counting, lazy locking.
</details>

<details>
<summary><strong>Q3: [Conceptual] Proxy vs. Decorator vs. Adapter vs. Facade — how do they differ?</strong></summary>

All four are structural wrappers, but their **intent** differs:

- **Proxy** — *same interface*, controls **access** (when/whether/where the call reaches the real object). The proxy typically *owns or manages the lifecycle* of the real subject.
- **Decorator** — *same interface*, **adds behavior/responsibility** and is designed to **stack** (coffee + milk + sugar). The client usually supplies the wrapped object.
- **Adapter** — **changes the interface** so an incompatible class fits what the client expects; adds no behavior.
- **Facade** — invents a **new, simpler interface** over a whole subsystem; doesn't implement the subsystem's interface.

The tell: Proxy and Decorator have identical structure (same interface, hold a reference) — the difference is *purpose*. Proxy manages access to a **single** subject it often creates itself; Decorator enriches an object handed to it and expects to be composed with other decorators.
</details>

<details>
<summary><strong>Q4: [Conceptual] Give real JDK/framework examples of Proxy.</strong></summary>

- **`java.lang.reflect.Proxy`** — the JDK's dynamic-proxy factory; generates an interface proxy routed through an `InvocationHandler`.
- **Java RMI stubs** — client-side remote proxies that marshal method calls across the network.
- **Hibernate/JPA lazy loading** — virtual proxies (ByteBuddy/CGLIB subclasses) that fetch the row on first access.
- **Spring AOP** (`@Transactional`, `@Cacheable`, `@Async`, `@PreAuthorize`) — JDK or CGLIB proxies that run advice around the target.
- **MyBatis mapper interfaces** — dynamic proxies that turn interface calls into SQL execution.
- **gRPC / Feign / Retrofit clients** — generated remote proxies for typed RPC/HTTP calls.
</details>

<details>
<summary><strong>Q5: [Conceptual] What's the difference between a static (compile-time) proxy and a dynamic proxy?</strong></summary>

A **static proxy** is a hand-written class that implements the Subject interface and delegates to the real subject. It's explicit and debuggable, but you need one proxy class per interface, and every method must be written out — adding a method to the interface means editing the proxy. It doesn't scale to cross-cutting concerns across many types.

A **dynamic proxy** is generated at runtime. In Java you have two mechanisms: **JDK `java.lang.reflect.Proxy`**, which proxies **interfaces** and routes every call through a single `InvocationHandler.invoke(proxy, method, args)`; and **CGLIB/ByteBuddy**, which generates a **subclass** of a concrete class (so it can proxy classes without interfaces, but can't proxy `final` classes/methods). Dynamic proxies let one handler apply the same logic (transactions, logging, security) uniformly across any interface/class — this is how Spring AOP works.
</details>

<details>
<summary><strong>Q6: [Implementation] Implement a virtual proxy for an expensive-to-load image.</strong></summary>

Classic "load on first render" virtual proxy. The proxy defers the costly disk load until `display()` is actually called.

<details>
<summary>💻 Click to expand solution</summary>

```java
interface Image { void display(); }

class RealImage implements Image {          // expensive: loads from disk in ctor
    private final String file;
    RealImage(String file) { this.file = file; loadFromDisk(); }
    private void loadFromDisk() { System.out.println("Loading " + file); }
    public void display() { System.out.println("Displaying " + file); }
}

class ImageProxy implements Image {         // virtual proxy
    private final String file;
    private RealImage real;                 // created lazily
    ImageProxy(String file) { this.file = file; }
    public void display() {
        if (real == null) real = new RealImage(file);  // load on first use
        real.display();
    }
}

// Usage:
Image img = new ImageProxy("photo.png"); // NO disk load yet
img.display();  // "Loading photo.png" then "Displaying photo.png"
img.display();  // just "Displaying photo.png" — already loaded
```
</details>

**Mechanism:** the proxy holds only the cheap identifier (filename) until first access, then creates and caches the `RealImage`. Note: this simple version is **not thread-safe** — concurrent first calls could each build a `RealImage`. Guard with double-checked locking + `volatile` if shared.
</details>

<details>
<summary><strong>Q7: [Implementation] Implement a protection proxy that enforces role-based access.</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
interface Document { String read(); void delete(); }

class RealDocument implements Document {
    private final String content;
    RealDocument(String content) { this.content = content; }
    public String read() { return content; }
    public void delete() { System.out.println("Document deleted"); }
}

class ProtectedDocument implements Document {   // protection proxy
    private final RealDocument real;
    private final String role;
    ProtectedDocument(RealDocument real, String role) {
        this.real = real; this.role = role;
    }
    public String read() { return real.read(); } // everyone can read
    public void delete() {
        if (!"ADMIN".equals(role))
            throw new SecurityException("Only ADMIN may delete");
        real.delete();
    }
}

// Usage:
Document userView  = new ProtectedDocument(new RealDocument("secret"), "USER");
userView.read();    // OK
userView.delete();  // throws SecurityException
```
</details>

**Mechanism:** the proxy interposes an authorization check before forwarding. The `RealDocument` stays free of security logic (SRP); the policy lives entirely in the proxy.
</details>

<details>
<summary><strong>Q8: [Implementation] Use <code>java.lang.reflect.Proxy</code> to add logging to any interface.</strong></summary>

This is the dynamic-proxy idiom — one handler, works for any interface.

<details>
<summary>💻 Click to expand solution</summary>

```java
import java.lang.reflect.*;

interface UserService { String getUser(int id); }

class RealUserService implements UserService {
    public String getUser(int id) { return "User#" + id; }
}

class LoggingHandler implements InvocationHandler {
    private final Object target;
    LoggingHandler(Object target) { this.target = target; }
    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        long t0 = System.nanoTime();
        try {
            Object result = m.invoke(target, args);      // forward to real object
            System.out.printf("%s -> %s%n", m.getName(), result);
            return result;
        } catch (InvocationTargetException e) {
            throw e.getCause();   // unwrap! don't leak reflection exception
        } finally {
            System.out.printf("%s took %d ns%n", m.getName(), System.nanoTime() - t0);
        }
    }
}

@SuppressWarnings("unchecked")
static <T> T proxy(Class<T> iface, T target) {
    return (T) Proxy.newProxyInstance(
        iface.getClassLoader(), new Class<?>[]{ iface },
        new LoggingHandler(target));
}

// Usage:
UserService svc = proxy(UserService.class, new RealUserService());
svc.getUser(7);   // logs the call, timing, and result
```
</details>

**Mechanism:** `Proxy.newProxyInstance` builds a synthetic class implementing `UserService`; every call is dispatched to `invoke`. Note the critical `InvocationTargetException` unwrap so callers see the *real* exception, not a reflection wrapper.
</details>

<details>
<summary><strong>Q9: [Implementation] Implement a caching smart proxy that memoizes results.</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
interface PriceService { double price(String symbol); }

class SlowPriceService implements PriceService {
    public double price(String symbol) {
        try { Thread.sleep(200); } catch (InterruptedException e) {} // simulate remote call
        return symbol.hashCode() % 1000 / 10.0;
    }
}

class CachingPriceProxy implements PriceService {           // smart reference
    private final PriceService real;
    private final Map<String, Double> cache = new ConcurrentHashMap<>();
    CachingPriceProxy(PriceService real) { this.real = real; }
    public double price(String symbol) {
        return cache.computeIfAbsent(symbol, real::price);  // atomic, memoized
    }
}

// Usage:
PriceService svc = new CachingPriceProxy(new SlowPriceService());
svc.price("AAPL"); // slow — hits real service
svc.price("AAPL"); // instant — served from cache
```
</details>

**Mechanism:** `computeIfAbsent` on a `ConcurrentHashMap` gives thread-safe memoization. Caveat: `computeIfAbsent` holds a bin lock during the mapping function, so a slow `real.price` can block other keys hashing to the same bin — for heavy contention, prefer a real cache (Caffeine) with proper loading semantics.
</details>

<details>
<summary><strong>Q10: [Breaking] Why is naive lazy initialization in a virtual proxy dangerous, and how do you fix it?</strong></summary>

The naive `if (real == null) real = new RealSubject();` is a **race condition**: two threads can both see `null`, both construct the real subject, and one instance is silently discarded — wasteful, and outright wrong if the real object holds a unique resource (a connection, a file handle).

The fix is **double-checked locking with `volatile`**:

<details>
<summary>💻 Click to expand fix</summary>

```java
class SafeProxy implements Subject {
    private volatile RealSubject real;   // volatile is MANDATORY
    private final Object lock = new Object();
    public void request() {
        RealSubject r = real;            // read volatile once
        if (r == null) {
            synchronized (lock) {
                r = real;
                if (r == null) real = r = new RealSubject();
            }
        }
        r.request();
    }
}
```
</details>

`volatile` is essential: without it, another thread could observe a **non-null but partially constructed** `real` due to instruction reordering (the reference publish can be reordered before the constructor's writes complete). `volatile` establishes a happens-before edge so a reader that sees the reference sees a fully-initialized object.
</details>

<details>
<summary><strong>Q11: [Breaking] What goes wrong with Spring's <code>@Transactional</code> self-invocation, and why is it a proxy problem?</strong></summary>

When a bean calls its own annotated method via `this.method()`, the annotation is **ignored** — no transaction starts, no cache is checked. This is a direct consequence of *how* the proxy works: Spring wraps the bean in an external proxy object; advice runs only when the call goes **through the proxy**. A self-call uses the raw `this` reference, which points at the target, not the proxy, so it bypasses the interception entirely.

<details>
<summary>💻 Click to expand the trap + fix</summary>

```java
@Service
class OrderService {
    @Transactional
    public void save(Order o) { /* ... */ }

    public void batch(List<Order> list) {
        for (Order o : list) save(o);   // BUG: self-call -> no per-item transaction
    }
}
// Fixes:
// 1. Inject a self-reference to the proxied bean and call self.save(o).
// 2. Move save() to a separate bean (proxy sits between beans).
// 3. Use AspectJ compile/load-time weaving instead of proxy-based AOP.
```
</details>

The deeper lesson: proxy-based AOP only intercepts **external** calls that cross the proxy boundary. AspectJ weaving (which modifies bytecode) doesn't have this limitation.
</details>

<details>
<summary><strong>Q12: [Breaking] How do dynamic proxies break <code>equals</code>, <code>hashCode</code>, <code>instanceof</code>, and identity?</strong></summary>

A JDK dynamic proxy is a **synthetic class** that implements the interface(s) but is **not** the real subject's type. So:

- `proxy instanceof RealSubjectClass` → **false** (it's only an instance of the interface). Code that casts to the concrete class throws `ClassCastException`. Proxy your objects against interfaces, never concrete types.
- `Object` methods: `equals`, `hashCode`, and `toString` are **also routed through `invoke`**, so if your handler doesn't special-case them you'll get surprising behavior (e.g., calling `target.equals` compares identity of the target, so `proxy.equals(proxy)` may return `false`). You typically must handle these methods explicitly in the handler.
- **Identity:** `proxy == realSubject` is always false; two proxies wrapping the same target aren't `==` either. This breaks identity-based caches and `IdentityHashMap` keys.
- For **CGLIB** subclass proxies, `instanceof RealSubjectClass` is **true** (it's a subclass), but `final` methods aren't intercepted and `getClass()` returns the generated subclass, which can confuse reflection.
</details>

<details>
<summary><strong>Q13: [Breaking] Why is a remote proxy a "leaky abstraction," and what should you do about it?</strong></summary>

A remote proxy makes a network call *look* like a local method call, which hides the two things that matter most about distribution: **latency** and **partial failure**. A local call is nanoseconds and either returns or throws deterministically; a remote call can take hundreds of milliseconds, time out, retry, or fail *after* the server already performed the work (so retries may double-execute). Pretending it's local — the "fallacies of distributed computing" — leads to code with no timeouts, no retries, no idempotency, and cascading failures.

The mitigation is to **not fully hide** the remoteness: give remote methods explicit timeouts, wrap them with **circuit breakers, retries with backoff, and bulkheads** (Resilience4j/Hystrix), make write operations **idempotent** (idempotency keys) so retries are safe, and surface remote failures as checked/typed exceptions the caller must handle. The interface can stay clean, but the operational reality (SLOs, failure modes) must be designed in, not abstracted away.
</details>

<details>
<summary><strong>Q14: [Trade-off] When should you use a static proxy vs. a JDK dynamic proxy vs. CGLIB?</strong></summary>

- **Static (hand-written) proxy** — few interfaces, logic specific to one type, you want it explicit and debuggable, zero runtime magic. Downside: boilerplate per method, doesn't scale to cross-cutting concerns.
- **JDK dynamic proxy** — the target implements **interfaces** and you want one handler reused across many types (logging, security, transactions). No external dependency. Limitation: **interface-only** — can't proxy a concrete class with no interface.
- **CGLIB / ByteBuddy** — you need to proxy a **concrete class without an interface** (subclass-based). Used by Spring when a bean has no interface. Limitations: can't proxy `final` classes or `final`/`private` methods, slightly heavier, requires a no-arg constructor consideration, and the proxy is a subclass so `getClass()` differs.

Rule of thumb: prefer interfaces + JDK proxies; fall back to CGLIB only when you must proxy a class; use static proxies for one-off, explicit cases.
</details>

<details>
<summary><strong>Q15: [Trade-off] Proxy vs. Decorator — they have identical structure. How do you decide?</strong></summary>

Decide by **intent and ownership**, not structure. Ask:

- *Am I controlling whether/when/where the call reaches the object* (access, lifecycle, location, permissions)? → **Proxy**. The proxy usually **creates or manages** the real subject itself and there's typically **one** proxy.
- *Am I adding new behavior/responsibilities and might I stack several such wrappers?* → **Decorator**. The client **supplies** the object to wrap, and decorators are designed to **compose** (buffered + gzipped + encrypted stream).

Concretely: Hibernate creating a lazy proxy for you = Proxy (it manages loading). `BufferedInputStream(new FileInputStream(...))` = Decorator (you compose, it adds buffering). If you find yourself stacking wrappers to add features, it's Decorator; if you're gating or deferring access to one managed object, it's Proxy.
</details>

<details>
<summary><strong>Q16: [Trade-off] What are the costs of introducing a proxy layer, and when is it overkill?</strong></summary>

Costs: an **extra indirection** on every call (a virtual dispatch — negligible for static proxies, but reflection-based dynamic proxies add real overhead per call and defeat some JIT inlining); **added classes/complexity** and harder debugging (stack traces show synthetic `$Proxy0` frames); **lifecycle subtleties** (self-invocation, identity/`instanceof` breakage); and for lazy proxies, **surprising latency** at the deferred load point (Hibernate's `LazyInitializationException` is the poster child).

It's overkill when: the real object is cheap to create (virtual proxy adds complexity for no gain), you don't actually need to gate access, or a simpler mechanism (a plain method, a Guava `Suppliers.memoize`, an `Optional`) suffices. Don't reach for a dynamic proxy where a two-line static wrapper does the job, and don't hide network calls behind a proxy without building in resilience.
</details>

<details>
<summary><strong>Q17: [Advanced] How does <code>java.lang.reflect.Proxy</code> generate its class at runtime, and what are the performance implications?</strong></summary>

`Proxy.newProxyInstance` produces (and caches, per classloader + interface set) a synthetic class named like `com.sun.proxy.$Proxy0` that implements the requested interfaces. For each interface method it generates a body that packages the `Method` object and arguments and calls `handler.invoke(this, method, args)`. Argument boxing (primitives → wrappers into an `Object[]`) and the reflective `Method.invoke` inside a typical handler are the cost centers: boxing allocates, and `Method.invoke` historically involved reflection overhead and inhibited inlining.

Performance-wise: the **class generation** happens once and is cached, so it's not the concern; the **per-call** cost is the boxing + dispatch through `invoke` + usually a `Method.invoke`. For hot paths this matters — which is why high-performance frameworks prefer **ByteBuddy/ASM-generated direct-dispatch** proxies (no reflection per call) or **compile-time** code generation. Modern JITs and the improvements to `MethodHandle`/reflection have narrowed the gap, but a reflective dynamic proxy is still measurably slower than a hand-written static proxy or a generated direct-call proxy.
</details>

<details>
<summary><strong>Q18: [Advanced] In a remote proxy, what is marshalling and why does serialization identity matter?</strong></summary>

**Marshalling** is converting a method invocation — the method identity plus its arguments — into a byte stream that can travel over the network, and unmarshalling is reconstructing it on the other side (and the return value/exception on the way back). In RMI this uses Java serialization; in gRPC it's Protobuf; in REST it's JSON. The proxy (stub) marshals on the client; a skeleton/dispatcher unmarshals on the server.

**Serialization identity matters** because both ends must agree on the type contract. In Java serialization, a mismatched `serialVersionUID` between client and server class versions causes `InvalidClassException` — so remote proxies are brittle across independent deployments. This is why schema-based, versioned formats (Protobuf/Thrift/Avro) with explicit field numbers and backward/forward-compatibility rules are preferred for remote proxies at scale: they decouple client and server evolution, whereas Java serialization tightly couples them and is also a notorious security risk (deserialization gadget attacks).
</details>

<details>
<summary><strong>Q19: [Advanced] How does Hibernate's lazy proxy work internally, and what causes <code>LazyInitializationException</code>?</strong></summary>

For a lazy association, Hibernate injects a runtime-generated **subclass proxy** (via ByteBuddy; historically CGLIB) of the entity, backed by a `LazyInitializer`. The proxy carries only the identifier; every non-identifier accessor triggers `initialize()`, which uses the **currently associated persistence context (Session)** to issue the `SELECT` and populate the target. It's a textbook virtual proxy: creation of the fully-hydrated entity is deferred to first access.

`LazyInitializationException` occurs when that first access happens **after the Session is closed** (e.g., in the view layer, after the transaction/`@Transactional` method returned). With no open Session, the proxy can't run its `SELECT`. Fixes: fetch eagerly *within* the transaction (`JOIN FETCH`, entity graphs), map to DTOs before leaving the transactional boundary, or (anti-pattern) keep the session open longer (`OpenSessionInView` — masks the problem and can cause N+1). The right answer in interviews is "load what the view needs inside the transaction via a fetch join or projection," not extending session lifetime.
</details>

<details>
<summary><strong>Q20: [Advanced/Coding] Build a rate-limiting protection proxy using a dynamic proxy that throttles calls per method.</strong></summary>

Full coding challenge: a generic dynamic proxy that enforces a per-method call budget using a token-bucket-ish counter, thread-safe.

<details>
<summary>💻 Click to expand full solution</summary>

```java
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

interface Api { String fetch(String key); }

class RealApi implements Api {
    public String fetch(String key) { return "value:" + key; }
}

/** Rate-limiting proxy: at most `maxPerSecond` calls per method name. */
class RateLimitHandler implements InvocationHandler {
    private final Object target;
    private final int maxPerSecond;
    private final ConcurrentMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();

    RateLimitHandler(Object target, int maxPerSecond) {
        this.target = target;
        this.maxPerSecond = maxPerSecond;
        // reset windows every second
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r); t.setDaemon(true); return t;
        }).scheduleAtFixedRate(counts::clear, 1, 1, TimeUnit.SECONDS);
    }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        // Object methods must be handled to avoid surprising behavior
        if (m.getDeclaringClass() == Object.class) return m.invoke(this, args);

        AtomicInteger c = counts.computeIfAbsent(m.getName(), k -> new AtomicInteger());
        if (c.incrementAndGet() > maxPerSecond)
            throw new IllegalStateException("Rate limit exceeded for " + m.getName());
        try {
            return m.invoke(target, args);       // forward
        } catch (InvocationTargetException e) {
            throw e.getCause();                  // unwrap real exception
        }
    }
}

@SuppressWarnings("unchecked")
static <T> T rateLimited(Class<T> iface, T target, int perSec) {
    return (T) Proxy.newProxyInstance(iface.getClassLoader(),
            new Class<?>[]{ iface }, new RateLimitHandler(target, perSec));
}

// Usage:
Api api = rateLimited(Api.class, new RealApi(), 2);
api.fetch("a");  // ok
api.fetch("b");  // ok
api.fetch("c");  // throws IllegalStateException: Rate limit exceeded
```
</details>

**Key points to mention:** handle `Object`-declared methods explicitly, unwrap `InvocationTargetException`, use atomic/concurrent structures for thread safety, and note that a production limiter would use a proper token bucket (Guava `RateLimiter`/Resilience4j) with monotonic time rather than a coarse per-second clear.
</details>

---

### 🧠 Staff / Principal Engineer Level

<details>
<summary><strong>SP1: [Staff] You're designing a service mesh sidecar (like Envoy). Explain how it is a proxy and what design decisions dominate.</strong></summary>

A sidecar is a **remote/smart proxy** deployed next to every service instance: all inbound and outbound traffic is transparently routed through it (via iptables/eBPF redirection), so the application talks to `localhost` and the proxy handles service discovery, mTLS, load balancing, retries, circuit breaking, and telemetry. It's the Proxy pattern at infrastructure scale — the app is oblivious that its "local" call is being authenticated, encrypted, retried, and observed.

The dominant design decisions: (1) **transparency vs. control** — full transparency (app unchanged) is the selling point, but it hides latency and failure, so you must expose config (timeouts, retry budgets) and rich telemetry to compensate; (2) **data-plane performance** — the proxy is on the hot path for *every* request, so per-hop latency and CPU/memory overhead per connection are critical (this is why Envoy is C++ with a tuned event loop, and why eBPF-based approaches try to shortcut the userspace hop); (3) **failure isolation** — the proxy must not become a single point of failure or add correlated failures (retry storms, retry amplification across hops — bounded retry *budgets* not per-call retries); (4) **control-plane/data-plane split** — config (xDS) is pushed dynamically without restarting proxies. The staff-level insight is that adding a proxy per hop multiplies its cost and its failure modes across the whole mesh, so its resilience and performance envelope, not its features, decide whether the mesh helps or hurts.
</details>

<details>
<summary><strong>SP2: [Staff] Retries in a proxy layer can cause cascading failure. How do you design them safely?</strong></summary>

Naive per-call retries are dangerous because they **amplify load exactly when the system is already struggling**: if a downstream is at capacity and every proxy retries 3×, you've tripled load and turned a brownout into an outage — and in a multi-hop call chain retries **multiply** (3 hops × 3 retries = up to 27× amplification).

Safe design: (1) **retry budgets** — cap retries as a *percentage* of total requests (e.g., ≤10%), so retries can't dominate traffic (Envoy/gRPC do this); (2) **retry only idempotent/safe operations**, and require idempotency keys for writes so retries don't double-execute; (3) **exponential backoff with jitter** to avoid synchronized retry storms; (4) **circuit breakers** so a failing downstream is shed fast instead of retried; (5) **retry at one layer only** — pick a single tier to own retries rather than every hop retrying (otherwise multiplication); (6) **deadline propagation** — pass a remaining-time budget down the call chain so no hop retries past the caller's deadline. The principal-level framing: retries are a **load-management** problem, not just a correctness feature; the proxy is the right place to enforce global budgets precisely because it sees all traffic.
</details>

<details>
<summary><strong>SP3: [Principal] Compare proxy-based AOP with AspectJ bytecode weaving. When does the proxy model's limitation force a change?</strong></summary>

Proxy-based AOP (Spring's default) wraps beans in JDK/CGLIB proxies and intercepts only calls that **cross the proxy boundary**. Its limitations: **self-invocation** isn't intercepted, only **public** methods on Spring-managed beans are advisable, `final` methods/classes can't be proxied by CGLIB, and object identity/`instanceof` can surprise. Its virtues: no build-time step, no special agent, easy to reason about, and advice applies only at well-defined boundaries.

**AspectJ weaving** modifies the actual bytecode (compile-time or load-time via an agent), so advice applies to **any** join point — private methods, self-calls, field access, constructors, even non-Spring objects — with no proxy indirection and lower per-call cost. The limitation shifts to build/deploy complexity (weaver or `-javaagent`), harder debugging (bytecode differs from source), and the risk of "spooky action at a distance" since any code can be advised.

You're forced off the proxy model when you need to advise self-invocations, private methods, or plain (non-container-managed) objects — e.g., domain entities — or when the per-call reflection overhead of proxies is unacceptable on a hot path. The principal judgment: stay with proxies for their simplicity and boundary clarity; adopt AspectJ only when a concrete requirement (self-calls, field-level advice, entity instrumentation) genuinely can't be met otherwise, because it trades a lot of operational simplicity.
</details>

<details>
<summary><strong>SP4: [Principal] Design a client-side caching proxy for a read-heavy distributed system. What consistency and invalidation issues arise?</strong></summary>

A caching smart proxy in front of a remote service dramatically cuts latency and load, but introduces the hard part of distributed systems: **cache coherence**. Key decisions: (1) **freshness model** — TTL (simple, eventually consistent, tolerates staleness) vs. write-through/write-behind (stronger, more coupling) vs. explicit invalidation on writes; (2) **invalidation** — the classic hard problem: broadcast invalidations (pub/sub), versioned keys, or ETag/conditional-GET revalidation to avoid serving stale data; (3) **stampede protection** — when a hot key expires, thousands of clients miss simultaneously and hammer the origin; mitigate with request coalescing (single-flight), probabilistic early expiration, and stale-while-revalidate; (4) **negative caching** with short TTLs to avoid re-fetching known-missing keys, but bounded so you don't pin errors; (5) **memory bounds and eviction** (LRU/LFU, Caffeine) so the proxy doesn't OOM; (6) **thundering herd on cold start** and **consistency across many client caches** (there's no single cache, so per-client staleness diverges).

The principal-level tension is **staleness vs. load vs. complexity**: a pure TTL cache is trivial and usually good enough; strong consistency requires invalidation infrastructure that often costs more than it's worth. The right answer states the read/write ratio and staleness tolerance first, then picks the weakest consistency the product can accept — and always adds stampede protection because that's the failure that actually pages you.
</details>

<details>
<summary><strong>SP5: [Principal] Java deserialization in remote proxies (e.g., RMI) is a major security risk. Explain the attack and the mitigations.</strong></summary>

Remote proxies that use **Java native serialization** (RMI, some legacy RPC) unmarshal attacker-influenced bytes into objects by invoking `readObject`/`readResolve`. The **deserialization gadget attack** exploits this: an attacker crafts a byte stream that, during deserialization, chains together `readObject` side effects across classes present on the classpath (a "gadget chain" — e.g., Apache Commons Collections `InvokerTransformer`) to achieve **arbitrary code execution** — all before any application logic runs. The proxy's transparency is exactly what makes it dangerous: it will happily deserialize whatever arrives.

Mitigations: (1) **stop using Java native serialization** for remote proxies — prefer schema-based, data-only formats (Protobuf, Thrift, Avro, JSON) that don't reconstruct arbitrary object graphs; (2) if you must use it, apply **serialization filters** (`ObjectInputFilter`, JEP 290) to allow-list acceptable classes and bound depth/array sizes; (3) keep dangerous gadget libraries off the classpath and patch them; (4) run remote endpoints with least privilege, network segmentation, and mutual auth so untrusted parties can't reach the deserialization surface; (5) validate/authenticate the *source* before deserializing. The principal takeaway: a remote proxy's job is to hide the wire, but security demands you treat every incoming byte as hostile — so the marshalling format is a first-order security decision, not an implementation detail. This is why the industry moved off Java serialization for RPC entirely.
</details>

---

## ⚡ Quick Revision

**One-liner:** Proxy provides a same-interface surrogate for another object to **control access** to it — the client can't tell it isn't talking to the real thing.

**The whole pattern in one paragraph:** The Proxy is a structural GoF pattern — *"provide a surrogate or placeholder for another object to control access to it."* The **proxy** implements the **same interface** (Subject) as the **RealSubject** and holds/obtains a reference to it, so every client call hits the proxy first, which can run logic *before*, *instead of*, or *after* forwarding. The unifying idea is **controlled access**; the four canonical flavors are **virtual** (lazy-create an expensive object — Hibernate lazy loading, image placeholders), **protection** (authorization checks before forwarding), **remote** (local stand-in that marshals calls across the network — RMI/gRPC stubs), and **smart reference** (caching, logging, reference-counting, locking). Implementations range from a hand-written **static proxy** (explicit, one class per interface, boilerplate) to **dynamic proxies**: JDK `java.lang.reflect.Proxy` (interfaces only, routes every call through one `InvocationHandler.invoke`) and **CGLIB/ByteBuddy** (subclass proxies for concrete classes, but can't proxy `final`). Structurally identical to **Decorator** — the difference is intent: Proxy *controls/manages access* to one subject it often owns, Decorator *adds stackable behavior* to an object handed to it; **Adapter** *changes* the interface, **Facade** invents a *simpler* one. The classic traps: non-thread-safe lazy init (fix with double-checked locking + `volatile` so a reader never sees a partially constructed object — JMM happens-before); Spring `@Transactional`/`@Cacheable` **self-invocation** silently bypassing advice because self-calls don't cross the proxy boundary; dynamic proxies breaking `instanceof`/identity and needing `Object`-method + `InvocationTargetException` handling; and remote proxies being **leaky abstractions** that hide latency and partial failure (so add timeouts, retry budgets, circuit breakers, idempotency). At scale — service mesh sidecars — the proxy's *reliability and performance envelope* per hop matters more than its features, and for remote proxies the **marshalling format is a security decision** (Java native serialization → deserialization gadget RCE; prefer Protobuf + serialization filters).

**Top 5 interview answers to memorize:**

1. **"What is it?"** → A same-interface surrogate that controls access to a real object; client is unaware. Four types: virtual, protection, remote, smart reference.
2. **"Proxy vs. Decorator?"** → Identical structure; different intent. Proxy *controls access* to a subject it typically manages/owns (one wrapper); Decorator *adds stackable behavior* to an object the client supplies.
3. **"Static vs. dynamic proxy?"** → Static = hand-written class per interface (explicit, boilerplate). Dynamic = generated at runtime; JDK `Proxy` for interfaces via one `InvocationHandler`, CGLIB subclasses for concrete classes (not `final`).
4. **"How does it break?"** → Non-thread-safe lazy init (needs DCL + `volatile`); Spring self-invocation bypasses `@Transactional`; dynamic proxies break `instanceof`/identity; remote proxies hide latency/failure.
5. **"Real examples?"** → Hibernate lazy loading (virtual), Spring AOP `@Transactional`/`@Cacheable` (dynamic), RMI/gRPC stubs (remote), `java.lang.reflect.Proxy`, MyBatis mappers.

**Trigger words (hear these → think Proxy):** "control access to", "lazy load / defer expensive creation", "placeholder / stand-in / surrogate", "add logging/caching/security without changing the class", "check permissions before calling", "call an object on another machine / remote object", "stub", "wrap a bean to add transactions", "reference counting", "add behavior transparently around every method", "intercept calls to", "same interface but gate the call".

---

*End of Proxy Pattern study guide.*



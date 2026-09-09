# Observer Pattern ⭐⭐⭐⭐ (Difficulty: Medium-Hard)

> A behavioral design pattern that establishes a one-to-many dependency so that when one object changes state, all its dependents are notified and updated automatically. The foundation of event-driven programming, MVC, and reactive systems — a FAANG interview staple with deep concurrency and memory-leak nuance.

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 1 — Naive Subject with List (Baseline)](#variant-1--naive-subject-with-list-baseline)
   - [Variant 2 — Push vs Pull Models](#variant-2--push-vs-pull-models)
   - [Variant 3 — Generic, Typed Observers](#variant-3--generic-typed-observers)
   - [Variant 4 — Weak References (Lapsed-Listener Fix)](#variant-4--weak-references-lapsed-listener-fix)
   - [Variant 5 — Thread-Safe with CopyOnWriteArrayList (Production)](#variant-5--thread-safe-with-copyonwritearraylist-production)
   - [Variant 6 — Functional Listeners / Event Bus](#variant-6--functional-listeners--event-bus)
5. [🎨 Real-World Example](#-real-world-example)
6. [✅ When to Use](#-when-to-use)
7. [❌ When NOT to Use](#-when-not-to-use)
8. [🎯 Pros and Cons](#-pros-and-cons)
9. [🔄 Comparison with Related/Similar Patterns](#-comparison-with-relatedsimilar-patterns)
10. [📊 Comparison Table](#-comparison-table)
11. [💡 Common Pitfalls](#-common-pitfalls)
12. [🎓 Interview Tips](#-interview-tips)
13. [🔗 Related Patterns](#-related-patterns)
14. [📚 Library/Framework Implementation](#-libraryframework-implementation)
15. [📝 Interview Questions & Answers (FAANG Top 20)](#-interview-questions--answers-faang-top-20)
16. [🏛️ Staff/Principal Engineer Questions](#-staffprincipal-engineer-questions)
17. [⚡ Quick Revision](#-quick-revision)

---

## 📋 Intent

**GoF:** *"Define a one-to-many dependency between objects so that when one object changes state, all its dependents are notified and updated automatically."*

The Observer pattern lets a **subject** (also called observable) maintain a list of **observers** (listeners/subscribers) and notify them of state changes, without the subject knowing anything about the observers' concrete types. Observers register and unregister themselves; the subject just walks its list and calls a well-known method (`update`/`onEvent`) on each.

It exists to achieve **loose coupling** between the source of a change and the parties interested in that change. Without it, the subject would need hard references to every dependent and explicit calls to each, coupling it to their concrete types and making it impossible to add/remove interested parties without editing the subject. Observer inverts this: interested parties subscribe themselves, and the subject broadcasts blindly. This is the backbone of event handling, MVC (model notifies views), pub/sub within a process, and reactive streams.

---

## 🎯 Problem

You have an object whose state changes matter to *other* objects, and the set of interested parties changes over time and shouldn't be baked into the source. If the source directly calls each dependent, it becomes tightly coupled to their concrete types, you must edit the source to add/remove a dependent, and you can't reuse the source in a context with different dependents.

**Concrete scenarios where Observer shines:**

- **Stock ticker / market data** — a price feed changes; dozens of dashboards, alerting rules, and trading algorithms must react. The feed shouldn't know who's watching.
- **Notification service** — an order ships; email, SMS, push, and analytics subsystems each need to react independently. New channels get added often.
- **UI / MVC** — a model's data changes; every view bound to it must repaint. Multiple views observe one model.
- **Cache invalidation** — a source-of-truth record updates; caches, search indexes, and read replicas must invalidate/refresh.
- **Domain events** — "UserRegistered" fires; welcome email, CRM sync, and audit log all react without the registration code knowing about them.

The common thread: **one source of change, many independent reactors, membership that varies at runtime, and a hard requirement that the source not depend on the reactors.**

---

## ✅ Solution

The core idea in plain language: **let interested objects subscribe to a source; when the source changes, it walks its subscriber list and calls a standard method on each — without knowing who they are.** The source depends only on an `Observer` interface, so any object implementing it can listen.

Key structural elements:

- **Subject / Observable** — maintains the observer list and exposes `subscribe(observer)`, `unsubscribe(observer)`, and (usually private) `notifyObservers()`. Holds the state that observers care about.
- **Observer interface** — declares the callback the subject invokes, e.g. `update(...)` or `onEvent(event)`. This is the only thing the subject depends on.
- **Concrete observers** — implement the interface and react to notifications; they register themselves with the subject.
- **Notification models:**
  - **Push** — the subject sends the changed data *in* the notification (`update(newPrice)`). Simple, but the subject must guess what observers need, and the interface can bloat.
  - **Pull** — the subject just says "something changed" and passes a reference to itself; observers query back for exactly the data they need (`update(subject)` → `subject.getPrice()`). More flexible, slightly more coupling to the subject's query API.

The mechanisms that make Observer work: **loose coupling via the interface** (subject ⟶ Observer abstraction only), **dynamic subscription** (membership changes at runtime), and — critically in production — **safe concurrent iteration** (e.g., `CopyOnWriteArrayList`) and **lifecycle management** (unsubscribe / weak references) to avoid the two classic failure modes: `ConcurrentModificationException` during notify, and the **lapsed-listener memory leak**.

---

## 💻 Implementation

We'll evolve a **stock price feed** from a naive subject to a thread-safe, leak-safe, functional production design — the arc a FAANG interviewer wants.

### Variant 1 — Naive Subject with List (Baseline)

The classic textbook version: a subject holding a `List<Observer>`, with `subscribe`/`unsubscribe`/`notify`. **What's wrong:** it's not thread-safe (registration during iteration throws `ConcurrentModificationException`), it holds strong references (memory-leak risk), and one misbehaving observer throwing an exception breaks the whole notification loop.

<details>
<summary>💻 Click to expand code</summary>

```java
// Observer interface (push model)
interface PriceObserver {
    void update(String symbol, double price);
}

// Subject
class StockTicker {
    private final List<PriceObserver> observers = new ArrayList<>(); // strong refs
    private double price;

    void subscribe(PriceObserver o)   { observers.add(o); }
    void unsubscribe(PriceObserver o) { observers.remove(o); }

    void setPrice(String symbol, double price) {
        this.price = price;
        notifyObservers(symbol, price);
    }
    private void notifyObservers(String symbol, double price) {
        for (PriceObserver o : observers) {  // CME if list mutated during loop
            o.update(symbol, price);         // one throw kills the rest
        }
    }
}

// Concrete observer
class Dashboard implements PriceObserver {
    public void update(String symbol, double price) {
        System.out.println("Dashboard: " + symbol + " = " + price);
    }
}
```
</details>

**Pros:** simple; demonstrates the core structure; fine for single-threaded, short-lived cases.
**Cons:** not thread-safe (CME); strong refs cause lapsed-listener leaks; an exception in one observer aborts notification of the rest; notification order = insertion order (accidental coupling).
**Mechanism (why it's fragile):** iterating an `ArrayList` while another thread (or a re-entrant observer) mutates it violates the fail-fast contract, and `ArrayList` holds strong references indefinitely.

### Variant 2 — Push vs Pull Models

Two ways to deliver the change. **Push** sends data in the call; **pull** notifies and lets observers query. Push keeps observers simple but can bloat the interface and over-send; pull decouples what data is needed but couples observers to the subject's query API.

<details>
<summary>💻 Click to expand code</summary>

```java
// ---------- PUSH model: subject sends the data ----------
interface PushObserver { void update(String symbol, double price, long volume); }

class PushTicker {
    private final List<PushObserver> obs = new ArrayList<>();
    void subscribe(PushObserver o){ obs.add(o); }
    void change(String sym, double p, long v){
        for (PushObserver o : obs) o.update(sym, p, v); // sends everything
    }
}

// ---------- PULL model: subject says "changed", observer queries ----------
interface PullObserver { void update(StockData subject); }

class StockData {
    private String symbol; private double price; private long volume;
    private final List<PullObserver> obs = new ArrayList<>();
    void subscribe(PullObserver o){ obs.add(o); }
    void change(String sym, double p, long v){
        this.symbol = sym; this.price = p; this.volume = v;
        for (PullObserver o : obs) o.update(this);      // pass self
    }
    // observers pull exactly what they need:
    public double getPrice(){ return price; }
    public long getVolume(){ return volume; }
    public String getSymbol(){ return symbol; }
}

class PriceOnlyObserver implements PullObserver {
    public void update(StockData s){ System.out.println(s.getSymbol()+" "+s.getPrice()); }
    // ignores volume — pull lets each observer take only what it wants
}

// ---------- DEMO: exercising the PUSH model ----------
public class PushDemo {
    public static void main(String[] args) {
        PushTicker ticker = new PushTicker();

        // Observer receives everything in the callback — no back-reference needed
        ticker.subscribe((sym, price, vol) ->
            System.out.println("Dashboard: " + sym + " @ " + price + " vol=" + vol));
        ticker.subscribe((sym, price, vol) -> {
            if (price > 1000) System.out.println("ALERT: " + sym + " crossed 1000");
        });

        ticker.change("AAPL", 995.0, 12_000);   // Dashboard prints; no alert
        ticker.change("AAPL", 1010.0, 15_000);  // Dashboard prints; ALERT fires
        // Notice: the subject PUSHED (sym, price, vol); observers used what they wanted.
    }
}
```
</details>

**Pros (push):** observers are trivial, no back-reference to subject; **Pros (pull):** subject interface stays stable, observers fetch only needed fields, avoids over-sending.
**Cons (push):** interface bloats as data grows, subject must anticipate needs, sends data observers may ignore; **Cons (pull):** observers coupled to subject's query API, extra calls, risk of reading inconsistent state if subject mutates concurrently.
**Mechanism:** push optimizes for observer simplicity and decoupling from the subject's API; pull optimizes for interface stability and selective data access.

### Variant 3 — Generic, Typed Observers

Parameterize the observer/subject over the event type for type safety and reuse across domains. **What it solves:** removes casting, one reusable `Observable<T>` base for any event, compile-time guarantees on the payload.

<details>
<summary>💻 Click to expand code</summary>

```java
interface Observer<T> {
    void onEvent(T event);
}

class Observable<T> {
    private final List<Observer<T>> observers = new ArrayList<>();
    public void subscribe(Observer<T> o)   { observers.add(o); }
    public void unsubscribe(Observer<T> o) { observers.remove(o); }
    protected void publish(T event) {
        for (Observer<T> o : observers) o.onEvent(event);
    }
}

// Domain event as an immutable record (safe to share with observers)
record PriceChanged(String symbol, double price, long timestamp) {}

class PriceFeed extends Observable<PriceChanged> {
    public void setPrice(String symbol, double price) {
        publish(new PriceChanged(symbol, price, System.currentTimeMillis()));
    }
}

// Usage:
class AlertObserver implements Observer<PriceChanged> {
    public void onEvent(PriceChanged e) {
        if (e.price() > 1000) System.out.println("ALERT " + e.symbol());
    }
}

// ---------- DEMO: PUSH style (event carries all the data) ----------
public class GenericPushDemo {
    public static void main(String[] args) {
        PriceFeed feed = new PriceFeed();
        feed.subscribe(new AlertObserver());
        feed.subscribe(e ->                                   // lambda observer
            System.out.println("Log: " + e.symbol() + " " + e.price() + " @ " + e.timestamp()));

        feed.setPrice("AAPL", 995.0);    // Log prints; no alert
        feed.setPrice("AAPL", 1200.0);   // Log prints; AlertObserver fires
        // PUSH: everything the observer needs is already inside the PriceChanged event.
    }
}

// ---------- DEMO: PULL style (event carries the SOURCE; observer queries it) ----------
// The generic base also supports pull — just make the event a reference to the subject
// so observers fetch exactly the fields they need on demand.
class PullPriceFeed extends Observable<PullPriceFeed> {
    private String symbol; private double price; private long volume;
    public void setPrice(String sym, double p, long v) {
        this.symbol = sym; this.price = p; this.volume = v;
        publish(this);                                        // notify with self as the event
    }
    public String getSymbol() { return symbol; }
    public double getPrice()  { return price; }
    public long   getVolume() { return volume; }
}

public class GenericPullDemo {
    public static void main(String[] args) {
        PullPriceFeed feed = new PullPriceFeed();
        feed.subscribe(src -> System.out.println("PriceView: " + src.getPrice())); // pulls price only
        feed.subscribe(src -> {                                                     // pulls volume only
            if (src.getVolume() > 10_000) System.out.println("HighVolume: " + src.getSymbol());
        });

        feed.setPrice("AAPL", 995.0, 12_000);
        // PULL: the event is the source; each observer reads only the fields it cares about.
    }
}
```
</details>

**Pros:** type-safe payloads, no casts, reusable base class, immutable event records prevent shared-mutable-state bugs.
**Cons:** still not thread-safe or leak-safe on its own; generics don't help with multi-event-type subjects (need multiple observables or a typed event bus).
**Mechanism:** **generics + immutable event objects** — safe, self-describing notifications the subject can broadcast without observers mutating shared state.

### Variant 4 — Weak References (Lapsed-Listener Fix)

Observers that forget to unsubscribe are kept alive forever by the subject's strong references — the **lapsed-listener leak**. Holding observers via `WeakReference` lets the GC collect unreferenced observers. **What it solves:** memory leaks from observers that outlive their usefulness but never unsubscribed.

<details>
<summary>💻 Click to expand code</summary>

```java
class WeakSubject<T> {
    // Weak refs: GC can reclaim observers no longer strongly referenced elsewhere
    private final List<WeakReference<Observer<T>>> observers = new ArrayList<>();

    public void subscribe(Observer<T> o) {
        observers.add(new WeakReference<>(o));
    }

    public void publish(T event) {
        Iterator<WeakReference<Observer<T>>> it = observers.iterator();
        while (it.hasNext()) {
            Observer<T> o = it.next().get();
            if (o == null) it.remove();     // collected → purge dead ref
            else o.onEvent(event);
        }
    }
}
```
</details>

**Pros:** prevents lapsed-listener leaks; forgotten observers are auto-reclaimed.
**Cons:** surprising semantics — an observer with no other strong reference (e.g., an anonymous lambda or a local) may be collected and silently stop receiving events; must periodically purge dead refs; still not thread-safe as written. This is why `java.beans.PropertyChangeSupport` uses strong refs by default and leaves lifecycle to the developer.
**Mechanism:** `WeakReference` doesn't prevent GC, so the subject's list no longer pins observers in memory — but the caller must keep a strong reference for as long as it wants notifications.

### Variant 5 — Thread-Safe with CopyOnWriteArrayList (Production)

In real systems, subscription and notification happen on different threads. `CopyOnWriteArrayList` makes iteration during notification safe without locking readers, at the cost of copying on each mutation. **What it solves:** eliminates `ConcurrentModificationException` and the need to lock during notify; also isolate per-observer exceptions so one bad observer doesn't break the rest.

<details>
<summary>💻 Click to expand code</summary>

```java
import java.util.concurrent.CopyOnWriteArrayList;

class ThreadSafeSubject<T> {
    // COW: snapshot iteration; writers copy the array, readers never block/CME
    private final CopyOnWriteArrayList<Observer<T>> observers = new CopyOnWriteArrayList<>();

    public void subscribe(Observer<T> o)   { observers.addIfAbsent(o); }
    public void unsubscribe(Observer<T> o) { observers.remove(o); }

    protected void publish(T event) {
        for (Observer<T> o : observers) {   // iterates a stable snapshot, no lock
            try {
                o.onEvent(event);
            } catch (RuntimeException ex) {  // isolate failures
                log.warn("observer {} failed", o, ex); // one bad observer ≠ all fail
            }
        }
    }
}
```
</details>

**Pros:** safe concurrent subscribe/unsubscribe during notification; readers never lock; per-observer exception isolation; `addIfAbsent` prevents duplicate registration.
**Cons:** every mutation copies the whole array — great for read-heavy/notify-heavy, write-rare workloads (typical for observers), but costly if subscriptions churn rapidly; iteration sees a snapshot, so an observer added mid-notify may miss the current event (usually acceptable/desirable).
**Mechanism:** **copy-on-write** gives each iteration an immutable snapshot of the backing array, so notification never sees concurrent structural modification — no lock, no CME.

### Variant 6 — Functional Listeners / Event Bus

This is the most decoupled form of Observer, and the one you'll see most in large production systems. In every earlier variant the subject still held a list of `Observer` objects and called a known method (`onEvent`, `update`) on them — so the subject and observer still shared an interface, and the subject still "knew" its observers as a group. An **event bus** removes even that coupling. There is no `Subject` and no `Observer` interface anymore. Instead:

- **Subscribers** tell the bus, "when an event of *this type* happens, run *this function*." The handler is just a lambda (`Consumer<T>`), so there's no class to implement.
- **Publishers** hand the bus an event object and say `post(event)`. They don't know who — if anyone — is listening.
- The **bus** is a middleman that keeps a map of *event type → list of handlers*. When something is posted, it looks up the handlers registered for that exact type and calls each one.

The key insight: coupling is now only to the **event type** (e.g. `OrderShipped`) and the **bus**, never between the publisher and subscriber directly. You can add a new subscriber (say, a loyalty-points updater) without touching the code that ships orders. This is exactly how Guava's `EventBus` and Spring's `ApplicationEventPublisher` / `@EventListener` work.

Below is a deliberately minimal, single-threaded bus so the mechanism is easy to follow. (Production buses add thread-safety, async dispatch, and error handling — see the Libraries section.)

<details>
<summary>💻 Click to expand code</summary>

```java
import java.util.*;
import java.util.function.Consumer;

// A tiny event bus: it maps an event's TYPE to the handlers that care about it.
class EventBus {

    // Key = event class (e.g. OrderShipped.class); Value = handlers for that class.
    private final Map<Class<?>, List<Consumer<Object>>> handlers = new HashMap<>();

    // SUBSCRIBE: "when an event of type `type` is posted, run `handler`."
    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> type, Consumer<T> handler) {
        handlers.computeIfAbsent(type, k -> new ArrayList<>())   // first handler? make the list
                .add((Consumer<Object>) handler);                // store the lambda
    }

    // POST: look up handlers for this exact event type and call each one.
    public void post(Object event) {
        List<Consumer<Object>> list = handlers.get(event.getClass());
        if (list == null) return;               // nobody subscribed to this type
        for (Consumer<Object> handler : list) {
            handler.accept(event);              // run the subscriber's lambda
        }
    }
}

// ---------- Usage: publisher and subscriber never reference each other ----------
record OrderShipped(String orderId) {}   // the event: a plain immutable data carrier

public class EventBusDemo {
    public static void main(String[] args) {
        EventBus bus = new EventBus();

        // Two independent subscribers register handlers for the SAME event type.
        bus.subscribe(OrderShipped.class, e -> System.out.println("Email sent for " + e.orderId()));
        bus.subscribe(OrderShipped.class, e -> System.out.println("SMS sent for "   + e.orderId()));

        // The publisher just posts the event; it has no idea Email/SMS handlers exist.
        bus.post(new OrderShipped("ORD-42"));
        // Output:
        //   Email sent for ORD-42
        //   SMS sent for ORD-42
    }
}
```
</details>

**How to read the code:** `subscribe` records a lambda under its event class in the `handlers` map (creating the list on first use with `computeIfAbsent`). `post` takes any event, looks up `event.getClass()` in the map, and calls every stored handler with that event. Adding a third reaction (e.g. "update loyalty points") is one more `subscribe(...)` line — the `post` call and the order-shipping code never change.

**Pros:** ultimate decoupling (publishers/subscribers know only the event type and the bus); trivially add handlers as lambdas; central place for async dispatch, error handling, and metrics.
**Cons:** indirection hurts traceability ("who handles this event?"); the unchecked cast is needed because the map can't express "type → handler for that type" at compile time (type erasure); risk of "event soup" where control flow is hard to follow; ordering and delivery guarantees must be defined explicitly.
**Mechanism:** a **type-keyed dispatch map of functional handlers** replaces explicit subject/observer wiring with a mediator-like bus.

---

## 🎨 Real-World Example

A production-shaped **stock market notification system**: a thread-safe price feed notifies multiple observer types (dashboard, threshold alert, audit logger). The demo exercises multiple observers, dynamic subscribe/unsubscribe, a concurrency test, and exception isolation.

<details>
<summary>💻 Click to expand code</summary>

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// ---------- Event (immutable, safe to broadcast) ----------
record PriceEvent(String symbol, double price, long ts) {}

// ---------- Observer contract ----------
interface PriceObserver {
    void onPrice(PriceEvent e);
}

// ---------- Subject (thread-safe, leak-aware, fault-isolating) ----------
class StockFeed {
    private final CopyOnWriteArrayList<PriceObserver> observers = new CopyOnWriteArrayList<>();

    public void subscribe(PriceObserver o)   { observers.addIfAbsent(o); }
    public void unsubscribe(PriceObserver o) { observers.remove(o); }

    public void publish(String symbol, double price) {
        PriceEvent e = new PriceEvent(symbol, price, System.currentTimeMillis());
        for (PriceObserver o : observers) {
            try { o.onPrice(e); }
            catch (RuntimeException ex) {
                System.err.println("observer failed, continuing: " + ex.getMessage());
            }
        }
    }
    int observerCount() { return observers.size(); }
}

// ---------- Concrete observers ----------
class Dashboard implements PriceObserver {
    public void onPrice(PriceEvent e) {
        System.out.println("[Dashboard] " + e.symbol() + " = $" + e.price());
    }
}
class ThresholdAlert implements PriceObserver {
    private final double limit;
    ThresholdAlert(double limit) { this.limit = limit; }
    public void onPrice(PriceEvent e) {
        if (e.price() >= limit)
            System.out.println("[ALERT] " + e.symbol() + " hit $" + e.price());
    }
}
class AuditLogger implements PriceObserver {
    private final AtomicInteger count = new AtomicInteger();  // thread-safe counter
    public void onPrice(PriceEvent e) { count.incrementAndGet(); }
    int total() { return count.get(); }
}
class FlakyObserver implements PriceObserver {   // proves fault isolation
    public void onPrice(PriceEvent e) { throw new RuntimeException("boom"); }
}

// ---------- Demo ----------
public class StockDemo {
    public static void main(String[] args) throws Exception {
        StockFeed feed = new StockFeed();
        Dashboard dash = new Dashboard();
        ThresholdAlert alert = new ThresholdAlert(1000);
        AuditLogger audit = new AuditLogger();

        feed.subscribe(dash);
        feed.subscribe(alert);
        feed.subscribe(audit);
        feed.subscribe(new FlakyObserver());  // will throw but must not break others

        feed.publish("AAPL", 950.0);   // Dashboard + audit, no alert
        feed.publish("AAPL", 1050.0);  // Dashboard + alert + audit

        feed.unsubscribe(dash);        // dynamic unsubscribe
        feed.publish("AAPL", 1100.0);  // no Dashboard line now

        // Concurrency test: many threads publish + (un)subscribe simultaneously
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch done = new CountDownLatch(200);
        for (int i = 0; i < 200; i++) {
            pool.submit(() -> {
                feed.publish("AAPL", Math.random() * 2000); // no CME under COW
                done.countDown();
            });
        }
        done.await();
        pool.shutdown();
        System.out.println("Audit received all events safely: " + audit.total());
        System.out.println("Observers still registered: " + feed.observerCount());
    }
}
```
</details>

Why this is FAANG-grade: the subject is **thread-safe** (`CopyOnWriteArrayList`), **fault-isolating** (a throwing observer doesn't break the loop), broadcasts an **immutable event** (no shared-mutable-state bugs), supports **dynamic subscribe/unsubscribe under concurrency**, and observers manage their own thread-safe state (`AtomicInteger`).

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- A change to one object requires **an unknown/variable number of other objects to react**, and you want them decoupled.
- The set of interested parties **changes dynamically at runtime** (subscribe/unsubscribe).
- The subject **should not know the concrete types** of its dependents.
- You're building **event-driven** systems: UI events, MVC (model → views), domain events.
- **Cache invalidation, index updates, replicas** reacting to a source-of-truth change.
- **Publish/subscribe within a process** where a message broker would be overkill.
- You want to broadcast **notifications to multiple subsystems** (email/SMS/push/analytics) added independently over time.

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- There's exactly **one dependent that never changes** — a direct method call is clearer.
- You need **guaranteed ordering, delivery, retries, or durability** across processes — use a real message broker (Kafka, SQS), not in-process Observer.
- The notification chain risks **cascading/reentrant updates** or cycles that are hard to reason about.
- **Synchronous** notification to slow observers would block the subject's critical path (unless you make it async).
- You need a **strict, well-understood control flow** — heavy Observer/event-bus use can create "spooky action at a distance" that's hard to debug.
- Simple parent→child communication where a **callback or direct reference** is simpler and sufficient.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- **Loose coupling** — subject depends only on the Observer interface, not concrete observers.
- **Open/Closed** — add new observers without modifying the subject.
- **Dynamic relationships** — subscribe/unsubscribe at runtime.
- **Broadcast communication** — one event, many independent reactions.
- **Separation of concerns** — the source of change is decoupled from the reactions to it.

**Cons**

- **Lapsed-listener memory leaks** if observers don't unsubscribe (strong refs).
- **Unexpected/complex update cascades** — reentrancy, cycles, ordering surprises.
- **Notification order** is often undefined; observers must not depend on it.
- **Debugging is harder** — implicit control flow, "who fired this?"
- **Performance** — synchronous notification of many/slow observers blocks the subject; exceptions can break the loop if not isolated.
- **Concurrency hazards** — `ConcurrentModificationException`, visibility issues without care.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

| Pattern | Intent | Key difference from Observer |
|---|---|---|
| **Pub/Sub (message broker)** | Decouple publishers and subscribers via a broker/topic | Observer is typically **in-process** with the subject holding direct references to observers; Pub/Sub adds a **broker/channel** intermediary, often cross-process, async, with delivery guarantees. Observer subject *knows its observers*; pub/sub publisher knows only a topic. |
| **Mediator** | Centralize complex many-to-many communication in one object | Mediator coordinates **bidirectional** interactions among many colleagues; Observer is **one-to-many broadcast** in one direction. An event bus is a Mediator-flavored Observer. |
| **Chain of Responsibility** | Pass a request along a chain until one handles it | CoR delivers to **one** handler (first that handles it) and can stop; Observer notifies **all** observers unconditionally. |
| **Callback** | Pass a function to be invoked later | A callback is usually **one** listener for **one** call; Observer manages **many** listeners over **many** events with subscription lifecycle. |
| **Publish via events (Reactive/Streams)** | Async streams of events with operators & backpressure | Reactive (RxJava/Reactor) is Observer **plus** composition, async scheduling, and **backpressure**; plain Observer has none of these. |

**One-liner hooks:** Observer = in-process one-to-many push; Pub/Sub = broker-mediated, often cross-process; Mediator = centralizes many-to-many; CoR = one handler wins.

</details>

## 📊 Comparison Table

<details>
<summary>📖 Click to expand</summary>

| Axis | V1 List | V2 Push/Pull | V3 Generic | V4 Weak Refs | V5 COW (prod) | V6 Event Bus |
|---|---|---|---|---|---|---|
| Thread-safe | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ (if COW/concurrent) |
| CME-safe during notify | ❌ | ❌ | ❌ | ⚠️ manual | ✅ | ✅ |
| Leak-safe (lapsed listener) | ❌ | ❌ | ❌ | ✅ | ❌ (needs unsubscribe) | depends |
| Model | push | push & pull | push | push | push | push |
| Type safety | low | low | ✅ | ✅ | ✅ | ⚠️ (erasure/cast) |
| Fault isolation | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ (if wrapped) |
| Coupling | med | med | med | med | med | lowest |
| Best for | learning | choosing data delivery | typed domains | GUI/cache listeners | concurrent production | fully decoupled events |

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

**Pitfall 1 — Lapsed-listener memory leak.**
Observers that never unsubscribe are pinned by the subject's strong references and never GC'd — a classic leak in long-lived subjects (caches, singletons, UI models).

<details>
<summary>❌ Failure</summary>

```java
feed.subscribe(new Dashboard());  // no reference kept, never unsubscribed
// Dashboard lives forever because feed's List holds a strong ref
```
</details>

<details>
<summary>✅ Fix — explicit unsubscribe (try/finally or lifecycle hook), or WeakReference</summary>

```java
Dashboard d = new Dashboard();
feed.subscribe(d);
try { /* ... use ... */ } finally { feed.unsubscribe(d); }
// or hold observers via WeakReference in the subject (Variant 4)
```
</details>

**Pitfall 2 — ConcurrentModificationException during notification.**
An observer that unsubscribes (or subscribes another) *while being notified*, or notification racing with subscription on another thread, mutates the list mid-iteration.

<details>
<summary>❌ Failure</summary>

```java
for (Observer o : observers) {   // ArrayList
    o.update(e);                 // o calls unsubscribe(o) → CME
}
```
</details>

<details>
<summary>✅ Fix — CopyOnWriteArrayList (snapshot iteration) or iterate a copy</summary>

```java
private final CopyOnWriteArrayList<Observer> observers = new CopyOnWriteArrayList<>();
for (Observer o : observers) o.update(e); // safe: iterates immutable snapshot
// alternative: for (Observer o : new ArrayList<>(observers)) o.update(e);
```
</details>

**Pitfall 3 — One observer's exception aborts the rest.**
If notify loops without try/catch, the first observer to throw prevents all later observers from being notified.

<details>
<summary>✅ Fix — isolate each callback</summary>

```java
for (Observer o : observers) {
    try { o.update(e); }
    catch (RuntimeException ex) { log.warn("observer {} failed", o, ex); }
}
```
</details>

**Pitfall 4 — Depending on notification order / reentrant cascades.**
Observers that assume they're notified first/last, or that trigger further state changes during `update` (causing recursive/cyclic notifications), create fragile, hard-to-debug behavior. Fix: treat order as undefined; make observers independent; detect/guard reentrancy (e.g., a "notifying" flag or queueing changes) and avoid mutating the subject from within `update`.

**Pitfall 5 — Blocking the subject with slow synchronous observers.**
A slow observer stalls the subject's thread and every other observer. Fix: dispatch notifications asynchronously (executor/queue) when observers may be slow, accepting the added complexity of ordering and error handling.

</details>

## 🎓 Interview Tips

<details>
<summary>📖 What interviewers commonly ask</summary>

- "Implement a subject/observer for a stock ticker or notification system."
- "Push vs pull model — trade-offs?"
- "How do you make notification thread-safe?" (CopyOnWriteArrayList / snapshot).
- "What's the lapsed-listener problem and how do you fix it?" (weak refs / unsubscribe).
- "What happens if an observer throws during notify?"
- "Observer vs Pub/Sub vs Mediator?"
- "Why is `java.util.Observable` deprecated?"

</details>

<details>
<summary>💬 What to proactively mention</summary>

- Observer gives **loose coupling** and **one-to-many broadcast**; the subject depends only on the interface.
- Name the two classic hazards up front: **CME during notify** (fix: `CopyOnWriteArrayList`) and **lapsed-listener leaks** (fix: unsubscribe / `WeakReference`).
- **Isolate exceptions** per observer so one failure doesn't break the rest.
- Broadcast an **immutable event** to avoid shared-mutable-state bugs.
- Distinguish in-process **Observer** from **distributed Pub/Sub** (Kafka) — delivery guarantees, ordering, durability differ.
- `java.util.Observable` is **deprecated** (class not interface, not thread-safe, no generics) — prefer `PropertyChangeListener`, Guava `EventBus`, or reactive streams.
- Mention **sync vs async** dispatch trade-offs.

</details>

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Mediator** — centralizes complex many-to-many communication; an event bus blends Mediator + Observer.
- **Publish/Subscribe** — the distributed, broker-mediated evolution of Observer.
- **Reactive Streams (RxJava/Reactor)** — Observer + composition + async + backpressure.
- **MVC / MVP / MVVM** — the model is a subject; views observe it.
- **Chain of Responsibility** — alternative for "one handler processes" vs "all react."
- **Singleton** — subjects/event buses are often singletons (watch the leak risk).
- **Command** — notifications can be modeled as command/event objects.

</details>

## 📚 Library/Framework Implementation

<details>
<summary>1️⃣ java.util.Observer / Observable (deprecated since Java 9)</summary>

The original JDK implementation — now **deprecated**. Why: `Observable` is a **class** (forces inheritance, can't extend anything else), it's **not thread-safe**, it has **no generics** (updates are `Object`, requiring casts), and its `setChanged()`/`notifyObservers()` protocol is error-prone. Prefer `PropertyChangeListener`, `Flow` (reactive), or a library bus.

```java
// Legacy — avoid in new code
class Feed extends Observable {
    void setPrice(double p){ setChanged(); notifyObservers(p); } // Object payload
}
```
</details>

<details>
<summary>2️⃣ java.beans.PropertyChangeSupport / PropertyChangeListener</summary>

The standard JavaBeans observer utility. A bean composes a `PropertyChangeSupport`, and listeners subscribe to property changes (with old/new values — a push model). Thread-safety and lifecycle are the developer's responsibility.

```java
class Stock {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private double price;
    public void addListener(PropertyChangeListener l){ pcs.addPropertyChangeListener(l); }
    public void setPrice(double p){
        double old = price; price = p;
        pcs.firePropertyChange("price", old, p); // notifies all listeners
    }
}
```
</details>

<details>
<summary>3️⃣ Guava EventBus / Spring ApplicationEvent / Reactor & RxJava</summary>

**Guava `EventBus`**: annotate handlers with `@Subscribe`, register objects, `post()` events — decoupled in-process pub/sub.
**Spring `ApplicationEventPublisher`**: `publishEvent(event)`; beans implementing `ApplicationListener` or annotated `@EventListener` react — supports async via `@Async`.
**RxJava / Project Reactor**: `Observable`/`Flux` are Observer-based streams with operators and **backpressure** — the reactive evolution.

```java
// Spring
@Component class Emailer {
    @EventListener void on(OrderShipped e){ /* send email */ }
}
publisher.publishEvent(new OrderShipped("ORD-1")); // all @EventListeners fire

// RxJava
Observable.just("AAPL", "GOOG")
          .subscribe(sym -> System.out.println("tick " + sym));
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1 [Conceptual]: What is the Observer pattern and what problem does it solve?</strong></summary>

Observer is a behavioral pattern that defines a one-to-many dependency: when a subject changes state, all registered observers are notified automatically. It solves the problem of keeping multiple objects in sync with a source of truth without coupling the source to their concrete types. Observers subscribe/unsubscribe at runtime, and the subject broadcasts changes through a common `Observer` interface, so new reactors can be added without modifying the subject (Open/Closed). It's the backbone of event handling, MVC, and in-process pub/sub. The essential benefit is **loose coupling** between the source of change and the parties that react to it.

</details>

<details>
<summary><strong>Q2 [Conceptual]: Who are the participants and how do they interact?</strong></summary>

Four participants: **Subject/Observable** (holds state + observer list, exposes subscribe/unsubscribe/notify), **Observer** (interface with the callback, e.g. `update`/`onEvent`), **ConcreteObserver** (implements the callback and reacts), and **ConcreteSubject** (holds the real state and fires notifications on change). Observers register with the subject; on a state change the subject iterates its list and invokes the callback on each. The subject depends only on the Observer *interface*, never on concrete observer types — that abstraction is what enables loose coupling and dynamic membership.

</details>

<details>
<summary><strong>Q3 [Conceptual]: Explain the push vs pull notification models.</strong></summary>

In the **push** model the subject sends the changed data as arguments to `update(...)`; observers get everything immediately without querying back. It keeps observers simple but can bloat the interface and send data some observers don't need. In the **pull** model the subject only signals "I changed" and passes a reference to itself; each observer pulls exactly the fields it needs via getters. Pull keeps the interface stable and lets observers be selective, but couples them to the subject's query API and risks reading inconsistent state if the subject mutates concurrently. Choose push for simple/decoupled observers, pull when data needs vary widely.

</details>

<details>
<summary><strong>Q4 [Conceptual]: How does Observer differ from Pub/Sub?</strong></summary>

Classic Observer is **in-process**: the subject holds direct references to its observers and notifies them synchronously; publisher and subscriber are aware of each other through the subject. **Pub/Sub** introduces a broker/topic intermediary (often cross-process, e.g. Kafka/SQS): publishers post to a topic and never know who subscribes, subscribers may be remote, and the broker adds async delivery, buffering, ordering, and durability guarantees. Observer is a design pattern for object relationships; pub/sub is an architectural/messaging style. An in-process event bus sits between the two.

</details>

<details>
<summary><strong>Q5 [Implementation]: Implement a basic subject/observer for a stock ticker. (code)</strong></summary>

```java
interface Observer { void update(String symbol, double price); }

class Stock {
    private final List<Observer> observers = new ArrayList<>();
    private double price;
    void subscribe(Observer o){ observers.add(o); }
    void unsubscribe(Observer o){ observers.remove(o); }
    void setPrice(String sym, double p){
        this.price = p;
        for (Observer o : observers) o.update(sym, p);
    }
}
class Display implements Observer {
    public void update(String s, double p){ System.out.println(s+" "+p); }
}
```

I'd then note this baseline isn't thread-safe or leak-safe and evolve it (COW list, unsubscribe, exception isolation).

</details>

<details>
<summary><strong>Q6 [Implementation]: Make the observer list generic and type-safe. (code)</strong></summary>

Parameterize over the event type and broadcast immutable event objects:

```java
interface Observer<T> { void onEvent(T e); }
class Observable<T> {
    private final List<Observer<T>> obs = new ArrayList<>();
    void subscribe(Observer<T> o){ obs.add(o); }
    protected void publish(T e){ for (Observer<T> o : obs) o.onEvent(e); }
}
record PriceChanged(String symbol, double price) {}
class Feed extends Observable<PriceChanged> {
    void set(String s, double p){ publish(new PriceChanged(s, p)); }
}
```

Generics remove casts; an immutable `record` event prevents observers from corrupting shared state.

</details>

<details>
<summary><strong>Q7 [Implementation]: Build an in-process event bus that dispatches by event type. (code)</strong></summary>

```java
class EventBus {
    private final Map<Class<?>, List<Consumer<Object>>> handlers = new ConcurrentHashMap<>();
    @SuppressWarnings("unchecked")
    <T> void subscribe(Class<T> type, Consumer<T> h){
        handlers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                .add((Consumer<Object>) h);
    }
    void post(Object e){
        var list = handlers.get(e.getClass());
        if (list != null) for (var h : list) h.accept(e);
    }
}
```

Publishers and subscribers never reference each other — only the event type and the bus. This is the Guava `EventBus` idea in miniature.

</details>

<details>
<summary><strong>Q8 [Implementation]: How would you add asynchronous notification? (code)</strong></summary>

Dispatch each observer callback on an executor so slow observers don't block the subject:

```java
class AsyncSubject<T> {
    private final CopyOnWriteArrayList<Observer<T>> obs = new CopyOnWriteArrayList<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    void subscribe(Observer<T> o){ obs.add(o); }
    void publish(T e){
        for (Observer<T> o : obs)
            pool.submit(() -> { try { o.onEvent(e); } catch (Exception ex){ /*log*/ } });
    }
}
```

Trade-off: you gain isolation from slow observers but lose ordering guarantees and must handle backpressure, errors, and shutdown; events may be processed out of order.

</details>

<details>
<summary><strong>Q9 [Breaking]: What is the lapsed-listener problem and how do you fix it?</strong></summary>

The lapsed-listener problem is a **memory leak**: a subject holds strong references to observers, so observers that are no longer needed but never call `unsubscribe` can't be garbage-collected — they "lapse" but linger. It's common with long-lived subjects (singletons, caches, UI models) and short-lived observers. Fixes: (1) always unsubscribe deterministically (try/finally, `close()`/lifecycle hooks); (2) have the subject hold observers via `WeakReference` so the GC can reclaim them, purging dead refs on notify — but then callers must keep a strong reference or the observer may be collected prematurely. Explicit unsubscribe is preferred; weak refs are a safety net.

</details>

<details>
<summary><strong>Q10 [Breaking]: Why can notification throw ConcurrentModificationException, and how do you prevent it?</strong></summary>

If notification iterates a plain `ArrayList` while the list is structurally modified — either an observer unsubscribes/subscribes *during* its own `update` (reentrancy), or another thread subscribes concurrently — the fail-fast iterator throws `ConcurrentModificationException`. Prevent it by iterating a stable snapshot: use `CopyOnWriteArrayList` (writers copy the array, iteration sees an immutable snapshot, no lock), or iterate a defensive copy (`new ArrayList<>(observers)`), or synchronize and collect the list before notifying outside the lock. COW is the idiomatic choice for the read-heavy, write-rare observer workload.

</details>

<details>
<summary><strong>Q11 [Breaking]: What happens if one observer throws during notification?</strong></summary>

In a naive loop, an unchecked exception from one observer propagates out of `notifyObservers`, so every observer *after* it in the list is silently skipped, and the exception may bubble into the subject's own operation, corrupting its flow. The fix is **per-observer fault isolation**: wrap each callback in try/catch, log the failure, and continue notifying the rest. For stricter needs, collect failures and rethrow an aggregate after all observers run, or route failures to a dead-letter/error handler. Never let one buggy subscriber break the broadcast.

</details>

<details>
<summary><strong>Q12 [Breaking]: What problems arise from reentrant or cascading notifications?</strong></summary>

If an observer, while handling `update`, changes the subject's state (or another subject that eventually notifies back), you get reentrant/cyclic notifications that can cause infinite loops, stack overflows, `CME`, or observers seeing half-updated state. It also makes order-dependence bugs worse. Mitigations: forbid state mutation from within `update`; if changes are needed, **queue** them and process after the current notification completes; guard with a "currently notifying" flag to detect reentrancy; or model changes as immutable events processed by a single-threaded dispatcher. Designing observers to be independent and side-effect-light avoids most of this.

</details>

<details>
<summary><strong>Q13 [Trade-off]: Observer vs Mediator — when do you choose each?</strong></summary>

**Observer** is one-directional one-to-many broadcast: a subject notifies many observers that don't talk back through the pattern. **Mediator** centralizes **many-to-many, often bidirectional** interactions among a set of colleagues, encapsulating complex coordination logic in one place (e.g., a form where changing one field enables/disables others). Choose Observer when you simply need "when X changes, notify interested parties"; choose Mediator when multiple components must coordinate complex mutual interactions and you want that logic in one hub. An event bus is essentially a Mediator implemented with Observer mechanics.

</details>

<details>
<summary><strong>Q14 [Trade-off]: Why is java.util.Observable deprecated? What do you use instead?</strong></summary>

`java.util.Observable` (deprecated in Java 9) has several flaws: it's a **class**, not an interface, so subjects must extend it and can't extend anything else; it's **not thread-safe**; it has **no generics**, so `update(Object)` forces casts; and its `setChanged()`/`notifyObservers()` two-step protocol is easy to misuse. Modern alternatives: `java.beans.PropertyChangeListener`/`PropertyChangeSupport` for bean-style change events, `java.util.concurrent.Flow` (reactive streams) or RxJava/Reactor for async streams with backpressure, or a library event bus (Guava, Spring events). For a simple in-process case, a hand-rolled `CopyOnWriteArrayList`-backed subject is fine.

</details>

<details>
<summary><strong>Q15 [Trade-off]: Synchronous vs asynchronous notification — trade-offs?</strong></summary>

**Synchronous** notification is simple, preserves ordering (observers fire in list order), and gives the subject immediate feedback, but a slow or blocking observer stalls the subject's thread and all subsequent observers, and exceptions can disrupt the flow. **Asynchronous** notification (dispatch to an executor/queue) isolates the subject from slow observers and improves throughput, but sacrifices ordering guarantees, complicates error handling and testing, introduces backpressure concerns, and means observers see events "eventually." Use sync for fast, in-critical-path observers; async when observers may be slow, numerous, or independent.

</details>

<details>
<summary><strong>Q16 [Advanced/Concurrency]: Why CopyOnWriteArrayList specifically, and what are its costs?</strong></summary>

`CopyOnWriteArrayList` fits the observer workload — **frequent iteration (notify), rare mutation (subscribe/unsubscribe)** — because reads/iteration require no locking and never throw `ConcurrentModificationException`: each write replaces the internal array atomically, and iterators operate over the snapshot present at iteration start. Costs: every mutation copies the entire backing array (O(n) and GC pressure), so churn-heavy subscription patterns are expensive; and because iteration uses a snapshot, an observer subscribed mid-notification won't receive the in-flight event (usually desirable). If subscriptions churn rapidly, a `ConcurrentHashMap`-backed set or lock-with-snapshot approach may be better.

</details>

<details>
<summary><strong>Q17 [Advanced]: How do weak references interact with the GC here, and what's the gotcha?</strong></summary>

A `WeakReference` doesn't prevent its referent from being garbage-collected; if the only remaining reference to an observer is the subject's weak ref, the GC may reclaim it, and the subject purges the now-null ref on the next notify. This cleanly avoids the lapsed-listener leak. The gotcha: if the caller doesn't keep a **strong** reference (e.g., they register an anonymous lambda or a local object), the observer can be collected at an arbitrary time and silently stop receiving events — producing baffling "my listener stopped firing" bugs. So weak-ref subjects require callers to own the observer's lifetime explicitly. This is why many APIs default to strong refs + explicit unsubscribe.

</details>

<details>
<summary><strong>Q18 [Advanced]: How does Observer relate to Reactive Streams and backpressure?</strong></summary>

Reactive Streams (RxJava, Project Reactor, `java.util.concurrent.Flow`) are Observer at their core — a `Publisher` notifies `Subscriber`s — but add composition (map/filter/merge), scheduling/async, error channels, completion signals, and crucially **backpressure**: the subscriber signals demand (`request(n)`) so a fast producer can't overwhelm a slow consumer. Plain Observer has no backpressure — the subject pushes as fast as it changes, and a slow synchronous observer becomes a bottleneck while an async one risks unbounded queue growth/OOM. When events can outpace consumers, reactive streams' demand-based flow control is the principled solution.

</details>

<details>
<summary><strong>Q19 [Advanced/Coding challenge]: Design a thread-safe, fault-isolating, leak-aware subject. Full solution. (code)</strong></summary>

```java
class RobustSubject<T> {
    private final CopyOnWriteArrayList<Observer<T>> observers = new CopyOnWriteArrayList<>();

    public AutoCloseable subscribe(Observer<T> o) {
        observers.addIfAbsent(o);
        return () -> observers.remove(o);   // return a handle → deterministic unsubscribe
    }

    public void publish(T event) {
        for (Observer<T> o : observers) {   // snapshot iteration, no CME
            try { o.onEvent(event); }
            catch (RuntimeException ex) { log.warn("observer {} failed", o, ex); } // isolate
        }
    }
}

// Usage with try-with-resources → no lapsed listener:
try (AutoCloseable sub = subject.subscribe(e -> handle(e))) {
    // ... receive events ...
} // auto-unsubscribed here
```

This addresses all three hazards at once: **CME** (COW), **exception propagation** (per-observer catch), and **lapsed listener** (subscription returns an `AutoCloseable` for deterministic cleanup).

</details>

<details>
<summary><strong>Q20 [Advanced/Coding challenge]: Implement a typed event bus with async dispatch and error handling. Full solution. (code)</strong></summary>

```java
class AsyncEventBus {
    private final Map<Class<?>, List<Consumer<Object>>> handlers = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());
    private final Consumer<Throwable> errorHandler;

    AsyncEventBus(Consumer<Throwable> errorHandler){ this.errorHandler = errorHandler; }

    @SuppressWarnings("unchecked")
    <T> void subscribe(Class<T> type, Consumer<T> handler){
        handlers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                .add((Consumer<Object>) handler);
    }
    void post(Object event){
        var list = handlers.get(event.getClass());
        if (list == null) return;
        for (var h : list) {
            pool.submit(() -> {
                try { h.accept(event); }
                catch (Throwable t){ errorHandler.accept(t); } // isolate + report
            });
        }
    }
    void shutdown(){ pool.shutdown(); }
}

// Usage:
var bus = new AsyncEventBus(t -> log.error("handler failed", t));
bus.subscribe(OrderShipped.class, e -> emailService.send(e));
bus.subscribe(OrderShipped.class, e -> analytics.track(e));
bus.post(new OrderShipped("ORD-99"));  // both handlers run concurrently, failures isolated
```

Handlers run in parallel, one failure is reported without affecting others, and dispatch by `event.getClass()` keeps publishers and subscribers fully decoupled. Note the trade-off: no ordering guarantee across handlers, and you'd add bounded queues/backpressure for high volume.

</details>

---

## 🏛️ Staff/Principal Engineer Questions

<details>
<summary><strong>SP1: When do you graduate from in-process Observer to a distributed pub/sub system (e.g., Kafka)?</strong></summary>

Move to distributed pub/sub when reactions must cross **process/service boundaries**, must survive producer/consumer restarts (**durability**), need **replay** or **fan-out to many independent consumer groups**, require **decoupled scaling** of producers vs consumers, or need delivery/ordering guarantees the in-process pattern can't provide. In-process Observer is synchronous, best-effort, and dies with the JVM; a broker (Kafka/Pulsar/SQS) adds persistence, partitioned ordering, consumer offsets, replay, and back-pressure via pull. The cost is operational complexity, at-least-once semantics (dedup/idempotency needed), eventual consistency, and serialization/schema management. A common architecture uses in-process events inside a service and a broker between services — with an outbox pattern to bridge them atomically.

</details>

<details>
<summary><strong>SP2: How do you reason about delivery guarantees and ordering in an event-driven system built on Observer/pub-sub?</strong></summary>

In-process synchronous Observer gives **exactly-once, in-order** delivery within the process (each observer called once, in list order) but no durability — a crash loses everything mid-notify. Distributed systems force a choice: **at-most-once** (fire-and-forget, may drop), **at-least-once** (retries → duplicates → require idempotent consumers/dedup keys), or **exactly-once** (expensive; usually approximated via idempotency + transactional outbox/offsets). Ordering is only guaranteed within a partition/key, not globally, so you partition by an entity key when order matters. As a principal, I'd make these guarantees explicit contracts, design consumers to be idempotent, and use sequence numbers/versioning so out-of-order or duplicate events are detectable and safe.

</details>

<details>
<summary><strong>SP3: How do you prevent event-driven architectures from becoming an untraceable "event soup"?</strong></summary>

The risk with heavy Observer/event-bus use is implicit control flow — nobody can tell what happens when an event fires. Mitigations: (1) a **documented event catalog** with schemas and ownership; (2) **correlation/trace IDs** propagated through events so distributed tracing (OpenTelemetry) reconstructs causal chains; (3) clear **naming and versioning** conventions for events; (4) limiting **event chains** (avoid events that trigger events that trigger events — cap the depth, forbid cycles); (5) **observability** — metrics per event type/handler (throughput, latency, error rate, dead-letters); (6) preferring **explicit orchestration** (a saga/workflow) over implicit choreography when a business process spans many steps. The goal is that any event's downstream effects are discoverable and testable.

</details>

<details>
<summary><strong>SP4: How do you test event-driven / observer-heavy systems effectively?</strong></summary>

Test in layers. (1) **Observers in isolation** — pure unit tests feeding synthetic events; this is the payoff of loose coupling. (2) **Subject/dispatch logic** — verify subscribe/unsubscribe, that all observers are notified, exception isolation (a throwing observer doesn't stop others), and thread-safety under concurrent notify/subscribe (stress tests, `CompletableFuture` fan-out, tools like jcstress for JMM issues). (3) **Contract tests** every observer/handler must pass. (4) **Integration** — with real bus/broker using testcontainers, asserting delivery, ordering-within-partition, idempotency, and dead-letter behavior. (5) **Determinism** — because async ordering is nondeterministic, use awaitility-style polling and idempotent assertions rather than sleeps. I'd also inject a controllable clock/executor so async dispatch is testable synchronously.

</details>

<details>
<summary><strong>SP5: A long-lived singleton subject is leaking memory in production. Walk through diagnosis and fix.</strong></summary>

This is almost certainly the **lapsed-listener leak**: a singleton/subject holds strong references to observers that were never unsubscribed, so their entire object graphs are pinned. Diagnosis: take a **heap dump**, look for the subject's observer collection dominating retained size, and trace GC roots from leaked observers back to the subject. Fixes, in order of preference: (1) enforce **deterministic unsubscribe** via lifecycle hooks (`close()`, framework `@PreDestroy`, try-with-resources returning an `AutoCloseable` subscription handle); (2) switch the subject to hold **weak references** (with dead-ref purging) as a safety net, accepting the premature-collection caveat; (3) add **registration limits/leak detection** (log when observer count grows unboundedly). Systemically, I'd make subscription APIs return a disposable handle (RxJava `Disposable` style) so the lifecycle is impossible to ignore, and add a monitored metric on observer-count to catch regressions early.

</details>

---

## ⚡ Quick Revision

**One-liner:** Observer defines a one-to-many dependency so that when a subject's state changes, all subscribed observers are notified automatically — the foundation of event-driven programming.

**The whole pattern in one paragraph:** Observer is a behavioral GoF pattern — *"define a one-to-many dependency so when one object changes state, all its dependents are notified and updated automatically."* A **Subject/Observable** keeps a list of **Observers** and exposes `subscribe`/`unsubscribe`/`notify`; on a state change it walks the list and calls a standard callback (`update`/`onEvent`) on each, depending only on the **Observer interface** (loose coupling, Open/Closed — add observers without touching the subject). Delivery is **push** (subject sends data) or **pull** (subject signals change, observers query back). It powers MVC (model→views), UI events, cache invalidation, and in-process pub/sub. The three production hazards to name proactively: **ConcurrentModificationException during notify** (fix: `CopyOnWriteArrayList` snapshot iteration), the **lapsed-listener memory leak** from un-removed strong refs (fix: deterministic unsubscribe or `WeakReference`), and **one observer's exception aborting the loop** (fix: per-observer try/catch isolation). Broadcast **immutable events** to avoid shared-state bugs; consider **async dispatch** for slow observers (losing ordering). It differs from **Pub/Sub** (broker-mediated, often cross-process, durable) and **Mediator** (centralized many-to-many); **Reactive Streams** (RxJava/Reactor) extend it with composition and **backpressure**. `java.util.Observable` is **deprecated** (class not interface, no generics, not thread-safe) — prefer `PropertyChangeListener`, Guava `EventBus`, Spring events, or `Flow`.

**Top 5 answers to memorize:**

1. *What is it?* → One-to-many dependency; subject notifies all subscribed observers on state change, coupled only to the Observer interface (loose coupling + OCP).
2. *Push vs pull?* → Push sends data in the callback (simple, can bloat/over-send); pull signals change and observers query the subject (flexible, couples to subject's API).
3. *Thread-safety fix?* → Use `CopyOnWriteArrayList` so notify iterates an immutable snapshot — no lock, no `ConcurrentModificationException`.
4. *Lapsed-listener leak?* → Subject's strong refs pin observers that never unsubscribed; fix with deterministic unsubscribe (AutoCloseable handle) or `WeakReference`.
5. *Observer vs Pub/Sub?* → Observer is in-process, synchronous, subject knows observers; Pub/Sub adds a broker, is often cross-process/async with durability and delivery guarantees.

**Trigger words** (hear these → think Observer): "notify when something changes," "one-to-many," "subscribe/unsubscribe," "listeners," "event-driven," "stock ticker / price feed," "notification service," "model and views (MVC)," "cache invalidation," "publish/subscribe," "broadcast to multiple subsystems," "callback registry," "when X happens, do Y automatically," "decouple sender from receivers."

# State Pattern ⭐⭐⭐⭐ (Difficulty: 4/5 — conceptually clean, but the transition-ownership and shared-vs-per-instance decisions get subtle fast)

> **Category:** Behavioral Pattern (GoF)
> **Also known as:** Objects for States

The State pattern lets an object **alter its behavior when its internal state changes** — the object appears to *change its class*. It's the object-oriented answer to a sprawling `switch (state)` statement, and it's the machinery behind vending machines, TCP connection handling, order/workflow engines, media players, and thread lifecycles.

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 0: The Naive `if/switch` State Machine (Anti-pattern)](#variant-0-the-naive-ifswitch-state-machine-anti-pattern)
   - [Variant 1: Basic State Objects (Context holds state)](#variant-1-basic-state-objects-context-holds-state)
   - [Variant 2: States Own Transitions (State decides the next state)](#variant-2-states-own-transitions-state-decides-the-next-state)
   - [Variant 3: Enum-Based State Machine (Java idiom)](#variant-3-enum-based-state-machine-java-idiom)
   - [Variant 4: Production-Grade — Stateless Flyweight States + Transition Table](#variant-4-production-grade--stateless-flyweight-states--transition-table)
5. [🎨 Real-World Example](#-real-world-example)
   - [Scenario 1 — Document Review Workflow](#scenario-1--document-review-workflow-guards--audit--concurrency)
   - [Scenario 2 — Ride Lifecycle (Uber/Lyft)](#scenario-2--ride-lifecycle-uberlyft)
   - [Scenario 3 — Food Delivery (DoorDash/Swiggy)](#scenario-3--food-delivery-doordashswiggy)
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

> **GoF Definition:** *"Allow an object to alter its behavior when its internal state changes. The object will appear to change its class."*

The State pattern exists to solve a single core tension: an object's behavior depends on its **current mode/state**, and it must **change that behavior at runtime** as the state changes — without drowning in conditional logic that repeats the same `if (state == X)` dispatch across every method.

The key insight is to **represent each state as its own class** that implements a common interface, and to have the main object (the *Context*) **delegate behavior to the current state object**. Changing state is then just swapping which state object the context points to. Each state class encapsulates the behavior *and* the legal transitions for that state, so state-specific logic lives in exactly one place instead of being scattered across giant conditionals. This turns an implicit state machine (encoded in flags and branches) into an **explicit, first-class one** (encoded in types).

---

## 🎯 Problem

You have an object whose behavior must change depending on what "mode" it's in, and the number of modes and mode-dependent operations is non-trivial. The naive approach uses a state field plus conditionals in every method — but this becomes a maintenance nightmare: the same `switch` is duplicated everywhere, transitions are implicit and easy to get wrong, and adding a new state means editing every method.

**The pain points that lead you to State:**

- Behavior depends on state, and you have **many operations** each of which branches on the same state variable — the conditionals are duplicated and drift out of sync.
- **State transitions are implicit** — buried in scattered `if` statements — so illegal transitions (e.g., ship an already-cancelled order) slip through and are hard to audit.
- Adding a new state forces you to **touch every method** that switches on state (violates Open/Closed).
- The state variable is a primitive/enum and the "what's legal from here" knowledge is **not localized** anywhere.

**Concrete example scenarios:**

1. **Vending machine (the classic).** Behavior of `insertCoin()`, `selectProduct()`, `dispense()` depends entirely on whether the machine is `NoCoin`, `HasCoin`, `Dispensing`, or `SoldOut`. Each action is legal only in some states and triggers a transition.

2. **TCP connection.** `Closed`, `Listen`, `Established`, `FinWait`, etc. The same `open()`, `close()`, `acknowledge()` calls do wildly different things depending on the connection state — this is literally the GoF book's motivating example.

3. **Order / workflow engine.** An e-commerce order moves `Created → Paid → Shipped → Delivered`, with `Cancelled` and `Refunded` branches. `cancel()` is allowed from `Paid` but not from `Delivered`; `ship()` only from `Paid`. Encoding the legal moves in state objects prevents illegal transitions at the type level.

4. **Media player / document lifecycle / thread lifecycle.** A player toggles `Playing ↔ Paused ↔ Stopped`; a document is `Draft → Moderation → Published`; a Java `Thread` is `NEW → RUNNABLE → BLOCKED/WAITING → TERMINATED`. Each responds differently to the same commands.

---

## ✅ Solution

The core idea, in plain language: **extract each state into its own class implementing a shared `State` interface, and have the `Context` delegate all state-dependent calls to the current state object. To transition, swap the current state object.** The context no longer branches on a state variable — it just forwards to `currentState.doSomething()`, and the *behavior changes automatically* because a different object is now on the receiving end.

**Key structural elements:**

- **Context:** the object whose behavior varies (e.g., `VendingMachine`, `Order`). It holds a reference to a *current* `State` and delegates state-dependent requests to it. It also exposes a `setState(State)` method (often package-private) so states can transition it.
- **State (interface / abstract class):** declares the operations that vary by state (`insertCoin()`, `dispense()`, `handle()`…). One method per state-dependent action.
- **Concrete States:** each implements `State` for one specific state, providing the behavior for that state and (usually) deciding the *next* state by calling `context.setState(...)`. This is where transition logic lives.

**The mechanism that makes it work:** *polymorphic delegation replaces conditional dispatch*. Instead of `switch (state) { case A: ...; case B: ...; }` inside every method, the JVM's virtual method dispatch picks the right implementation based on which concrete `State` object the context currently references. State-specific behavior and the legal transitions out of that state are **co-located in one class**, so adding a state = adding a class (Open/Closed), and illegal actions can be handled uniformly (throw, ignore, or no-op) in the state that forbids them.

A crucial design decision — **who owns transitions?** Either (a) the concrete states decide the next state (decentralized: states know their successors — flexible but couples states to each other), or (b) the context / a transition table decides (centralized: transitions in one place — easier to audit, harder to have states drive complex logic). Both are valid; the choice is a recurring interview discussion point.

---

## 💻 Implementation

We'll model an **e-commerce Order lifecycle** — a canonical FAANG State question and a domain everyone understands — evolving from the naive conditional mess to a production-grade stateless-flyweight design. The order moves through `CREATED → PAID → SHIPPED → DELIVERED`, with a `CANCELLED` branch reachable from the early states. Each state responds differently to four actions — `pay()`, `ship()`, `deliver()`, `cancel()` — and, crucially, some actions are **illegal** in some states (you can't `ship()` an unpaid order, you can't `cancel()` a delivered one). Each variant fixes a specific weakness of the previous one, so read them top-to-bottom as one story.

**The state graph we're implementing (same for every variant):**

```
                pay()            ship()          deliver()
   CREATED ───────────▶ PAID ───────────▶ SHIPPED ───────────▶ DELIVERED  (terminal)
      │                  │
      │ cancel()         │ cancel()
      ▼                  ▼
   CANCELLED (terminal) ◀┘
```

### Variant 0: The Naive `if/switch` State Machine (Anti-pattern)

**What's wrong with it:** Before showing State, understand the disease it cures. The order's status lives in an `enum` field, and *every* method has to re-branch on it. The same `switch (status)` is duplicated across `pay()`, `ship()`, `deliver()`, and `cancel()`; transitions are scattered and implicit; and adding a new status (say, `RETURNED` or `REFUNDED`) means editing *every* method and praying you covered every case. This violates the Open/Closed Principle and is a classic source of "we forgot to handle status X in method Y" bugs — the kind that lets a `DELIVERED` order get silently cancelled in production.

<details>
<summary>💻 Click to expand code — the anti-pattern (Order lifecycle)</summary>

```java
// DON'T DO THIS — conditional dispatch duplicated in every method.
public class OrderNaive {
    enum Status { CREATED, PAID, SHIPPED, DELIVERED, CANCELLED }
    private Status status = Status.CREATED;
    private final String id;

    public OrderNaive(String id) { this.id = id; }

    public void pay() {
        // Every action must re-check the CURRENT status and decide what's legal — switch #1.
        switch (status) {
            case CREATED:   status = Status.PAID;  System.out.println(id + ": payment captured"); break;
            case PAID:      System.out.println(id + ": already paid");                            break;
            case SHIPPED:
            case DELIVERED: System.out.println(id + ": cannot pay — already " + status);          break;
            case CANCELLED: System.out.println(id + ": cannot pay — order cancelled");            break;
        }
    }

    public void ship() {
        // switch #2 — SAME set of cases, copy-pasted with different outcomes.
        switch (status) {
            case PAID:      status = Status.SHIPPED; System.out.println(id + ": shipped");        break;
            case CREATED:   System.out.println(id + ": cannot ship — not paid yet");              break;
            case SHIPPED:   System.out.println(id + ": already shipped");                         break;
            case DELIVERED: System.out.println(id + ": already delivered");                       break;
            case CANCELLED: System.out.println(id + ": cannot ship — cancelled");                 break;
        }
    }

    public void deliver() {
        // switch #3 — and again. Miss a case here and delivery silently does nothing.
        switch (status) {
            case SHIPPED:   status = Status.DELIVERED; System.out.println(id + ": delivered");    break;
            default:        System.out.println(id + ": cannot deliver from " + status);           break;
        }
    }

    public void cancel() {
        // switch #4 — the "which states allow cancel?" rule is trapped HERE, invisible elsewhere.
        switch (status) {
            case CREATED:
            case PAID:      status = Status.CANCELLED; System.out.println(id + ": cancelled + refunded"); break;
            case SHIPPED:   System.out.println(id + ": cannot cancel — already shipped");         break;
            case DELIVERED: System.out.println(id + ": cannot cancel — already delivered");       break;
            case CANCELLED: System.out.println(id + ": already cancelled");                       break;
        }
    }

    // ---------- Usage demo ----------
    public static void main(String[] args) {
        // Happy path: pay -> ship -> deliver.
        OrderNaive o1 = new OrderNaive("ORD-1");
        o1.pay();      // ORD-1: payment captured   (CREATED -> PAID)
        o1.ship();     // ORD-1: shipped            (PAID -> SHIPPED)
        o1.deliver();  // ORD-1: delivered          (SHIPPED -> DELIVERED)

        // Illegal action: shipping before paying just prints a message — no compiler help.
        OrderNaive o2 = new OrderNaive("ORD-2");
        o2.ship();     // ORD-2: cannot ship — not paid yet

        // Cancel is allowed early...
        OrderNaive o3 = new OrderNaive("ORD-3");
        o3.pay();      // CREATED -> PAID
        o3.cancel();   // ORD-3: cancelled + refunded  (PAID -> CANCELLED)
        // ...but not after shipping — the rule is buried inside cancel()'s switch.
        OrderNaive o4 = new OrderNaive("ORD-4");
        o4.pay(); o4.ship();
        o4.cancel();   // ORD-4: cannot cancel — already shipped
    }
}
```
</details>

**Code walkthrough:** The `status` field is the entire state machine, and it's *implicit* — nothing enforces the legal graph. Notice how the same five `case` labels (`CREATED`, `PAID`, `SHIPPED`, `DELIVERED`, `CANCELLED`) appear in `pay()`, `ship()`, and `cancel()`, each time with slightly different outcomes. The rule "you can only cancel from `CREATED` or `PAID`" lives *inside* `cancel()`, so if a teammate later asks "which states allow cancellation?", they must read every branch of every method to find out. Adding a `RETURNED` status means opening all four methods and adding a case to each — and the compiler will not warn you if you forget one; the missing case just falls into `default` or silently does nothing.

**Pros:** Trivial to write for 2–3 states; no indirection; all logic visible in one file.
**Cons:** The identical `switch` is repeated in every method (N states × M methods = N·M cases to keep consistent); transitions are implicit and un-auditable; adding a state edits every method (violates OCP); no single place answers "what can happen from PAID?".
**Mechanism (why it's fragile):** dispatch is *data-driven conditional branching*, so the compiler can't help you — forgetting a `case` in one method silently falls through to `default` or does nothing. There is no type-level guarantee that all states handle all actions.

---

### Variant 1: Basic State Objects (Context holds state)

**What problem it solves:** Extract each order status into a class implementing a shared `OrderState` interface. The `Order` (Context) delegates each action to its *current* state object. Now the "what does `ship()` do while `PAID`" logic lives in exactly one class (`PaidState`), not scattered across a `switch`. Adding a status = adding a class, not editing four methods. In this first cut, the **Context pre-creates one object per state and still owns the set of states** — states transition by asking the context for the target via accessor methods (`ctx.paid()`, `ctx.shipped()`, …).

<details>
<summary>💻 Click to expand code — basic state objects (Order lifecycle)</summary>

```java
// State interface: one method per state-dependent action on the order.
interface OrderState {
    void pay();
    void ship();
    void deliver();
    void cancel();
}

// Context: holds the current state + the order's data; delegates every action.
class Order {
    // Pre-create ONE object per state (each bound to this order).
    private final OrderState created   = new CreatedState(this);
    private final OrderState paid      = new PaidState(this);
    private final OrderState shipped   = new ShippedState(this);
    private final OrderState delivered = new DeliveredState(this);
    private final OrderState cancelled = new CancelledState(this);

    private OrderState current;
    private final String id;

    Order(String id) {
        this.id = id;
        this.current = created;          // every order starts in CREATED
    }

    // Public API — pure delegation, NO switch anywhere.
    public void pay()     { current.pay(); }
    public void ship()    { current.ship(); }
    public void deliver() { current.deliver(); }
    public void cancel()  { current.cancel(); }

    // Transition API + state accessors used by the state classes.
    void setState(OrderState s) { this.current = s; }
    OrderState created()   { return created; }
    OrderState paid()      { return paid; }
    OrderState shipped()   { return shipped; }
    OrderState delivered() { return delivered; }
    OrderState cancelled() { return cancelled; }
    String id()            { return id; }
}

// CREATED: can be paid or cancelled; cannot ship/deliver yet.
class CreatedState implements OrderState {
    private final Order o;
    CreatedState(Order o) { this.o = o; }
    public void pay()     { System.out.println(o.id() + ": payment captured"); o.setState(o.paid()); }
    public void ship()    { System.out.println(o.id() + ": cannot ship — not paid"); }
    public void deliver() { System.out.println(o.id() + ": cannot deliver — not paid"); }
    public void cancel()  { System.out.println(o.id() + ": cancelled");               o.setState(o.cancelled()); }
}

// PAID: can be shipped or cancelled (refund); cannot pay again.
class PaidState implements OrderState {
    private final Order o;
    PaidState(Order o) { this.o = o; }
    public void pay()     { System.out.println(o.id() + ": already paid"); }
    public void ship()    { System.out.println(o.id() + ": shipped");            o.setState(o.shipped()); }
    public void deliver() { System.out.println(o.id() + ": cannot deliver — not shipped"); }
    public void cancel()  { System.out.println(o.id() + ": cancelled + refunded"); o.setState(o.cancelled()); }
}

// SHIPPED: can only be delivered; too late to cancel.
class ShippedState implements OrderState {
    private final Order o;
    ShippedState(Order o) { this.o = o; }
    public void pay()     { System.out.println(o.id() + ": already paid"); }
    public void ship()    { System.out.println(o.id() + ": already shipped"); }
    public void deliver() { System.out.println(o.id() + ": delivered"); o.setState(o.delivered()); }
    public void cancel()  { System.out.println(o.id() + ": cannot cancel — already shipped"); }
}

// DELIVERED: terminal — nothing is legal.
class DeliveredState implements OrderState {
    private final Order o;
    DeliveredState(Order o) { this.o = o; }
    public void pay()     { System.out.println(o.id() + ": already delivered"); }
    public void ship()    { System.out.println(o.id() + ": already delivered"); }
    public void deliver() { System.out.println(o.id() + ": already delivered"); }
    public void cancel()  { System.out.println(o.id() + ": cannot cancel — delivered"); }
}

// CANCELLED: terminal — nothing is legal.
class CancelledState implements OrderState {
    private final Order o;
    CancelledState(Order o) { this.o = o; }
    public void pay()     { System.out.println(o.id() + ": order is cancelled"); }
    public void ship()    { System.out.println(o.id() + ": order is cancelled"); }
    public void deliver() { System.out.println(o.id() + ": order is cancelled"); }
    public void cancel()  { System.out.println(o.id() + ": already cancelled"); }
}

// ---------- Usage demo ----------
class OrderDemoV1 {
    public static void main(String[] args) {
        // Happy path — the Order never branches; each call delegates to the current state.
        Order o1 = new Order("ORD-1");
        o1.pay();      // ORD-1: payment captured   (CreatedState -> PaidState)
        o1.ship();     // ORD-1: shipped            (PaidState -> ShippedState)
        o1.deliver();  // ORD-1: delivered          (ShippedState -> DeliveredState)

        // Illegal action handled by the CURRENT state, not a switch.
        Order o2 = new Order("ORD-2");
        o2.ship();     // ORD-2: cannot ship — not paid   (handled inside CreatedState)

        // Cancel legal from PAID, illegal from SHIPPED.
        Order o3 = new Order("ORD-3");
        o3.pay();
        o3.cancel();   // ORD-3: cancelled + refunded     (PaidState -> CancelledState)
        Order o4 = new Order("ORD-4");
        o4.pay(); o4.ship();
        o4.cancel();   // ORD-4: cannot cancel — already shipped (handled inside ShippedState)
    }
}
```
</details>

**Code walkthrough:** The four `switch` statements from Variant 0 are gone. `Order.pay()` is now a one-liner — `current.pay()` — and *which* behavior runs is decided by polymorphism: if `current` is a `PaidState`, `PaidState.pay()` runs and prints "already paid." Each state class reads like a self-contained answer to "while in this status, what happens for each action?" — `PaidState` alone tells you paying again is a no-op, shipping advances to `SHIPPED`, and cancelling refunds. The legal-transition rule that was scattered across four methods in Variant 0 is now *localized*: to learn everything about the `PAID` status you read exactly one class. Transitions happen via `o.setState(o.shipped())` — the state asks the context for the pre-built target object and swaps it in. Adding a `RETURNED` status is now purely additive: write one `ReturnedState` class and wire an accessor, touching none of the existing states.

**Pros:** No duplicated `switch`; each state's behavior is localized; adding a state is adding a class (OCP); illegal actions handled uniformly per state.
**Cons:** Every state holds a back-reference to the Context (coupling); the Context exposes many accessor/transition methods (`created()`, `paid()`, …), leaking which states exist; there's noticeable boilerplate because every state must implement every method — even the many illegal no-ops. State objects are bound to one order, so they can't be shared across orders.
**Mechanism:** *virtual dispatch replaces the switch* — `current.pay()` resolves at runtime to the concrete state's method (`CreatedState.pay`, `PaidState.pay`, …). The Context pre-creates one state object per state and swaps the `current` reference to transition.

---

### Variant 2: States Own Transitions (State decides the next state)

**What problem it solves:** Variant 1 had two annoyances: the Context leaked accessor methods for every state, and every state had to spell out *all four* actions even though most are illegal no-ops. Here we fix both. We introduce an **abstract base state** that provides a default "illegal transition" for every action, so a concrete state overrides **only the actions it actually supports** — the boilerplate collapses. And we make transitions **fully owned by the states**: a state directly constructs its successor via `ctx.setState(new PaidState(ctx))`, so the Context no longer exposes accessors. Each class now fully answers "where do I go next?" and the successor graph is readable state-by-state. This is the most common textbook form and reads cleanly in interviews.

<details>
<summary>💻 Click to expand code — states own transitions (Order lifecycle)</summary>

```java
// Abstract base: every action defaults to "illegal in this state".
// Concrete states override ONLY the actions they legally support.
abstract class OrderState {
    protected final Order ctx;
    protected OrderState(Order ctx) { this.ctx = ctx; }

    void pay()     { illegal("pay"); }
    void ship()    { illegal("ship"); }
    void deliver() { illegal("deliver"); }
    void cancel()  { illegal("cancel"); }

    // One place defines what "illegal" means — no copy-paste across states.
    private void illegal(String action) {
        System.out.println(ctx.id() + ": illegal action '" + action
                           + "' in state " + getClass().getSimpleName());
    }
}

class Order {
    private OrderState current;
    private final String id;
    Order(String id) {
        this.id = id;
        this.current = new CreatedState(this);   // start in CREATED
    }
    public void pay()     { current.pay(); }
    public void ship()    { current.ship(); }
    public void deliver() { current.deliver(); }
    public void cancel()  { current.cancel(); }
    void setState(OrderState s) { this.current = s; }
    String id() { return id; }
}

// CREATED: overrides ONLY pay + cancel; ship/deliver inherit "illegal" from the base.
class CreatedState extends OrderState {
    CreatedState(Order o) { super(o); }
    @Override void pay() {
        System.out.println(ctx.id() + ": payment captured");
        ctx.setState(new PaidState(ctx));         // state builds its own successor
    }
    @Override void cancel() {
        System.out.println(ctx.id() + ": cancelled");
        ctx.setState(new CancelledState(ctx));
    }
}

// PAID: overrides ship + cancel.
class PaidState extends OrderState {
    PaidState(Order o) { super(o); }
    @Override void ship() {
        System.out.println(ctx.id() + ": shipped");
        ctx.setState(new ShippedState(ctx));
    }
    @Override void cancel() {
        System.out.println(ctx.id() + ": cancelled + refunded");
        ctx.setState(new CancelledState(ctx));
    }
}

// SHIPPED: overrides only deliver — cancel is now (correctly) illegal by default.
class ShippedState extends OrderState {
    ShippedState(Order o) { super(o); }
    @Override void deliver() {
        System.out.println(ctx.id() + ": delivered");
        ctx.setState(new DeliveredState(ctx));
    }
}

// DELIVERED & CANCELLED: terminal — override NOTHING, so every action is illegal.
class DeliveredState extends OrderState {
    DeliveredState(Order o) { super(o); }
}
class CancelledState extends OrderState {
    CancelledState(Order o) { super(o); }
}

// ---------- Usage demo ----------
class OrderDemoV2 {
    public static void main(String[] args) {
        // Happy path — states build their own successors as they run.
        Order o1 = new Order("ORD-1");
        o1.pay();      // ORD-1: payment captured   (CreatedState -> PaidState)
        o1.ship();     // ORD-1: shipped            (PaidState -> ShippedState)
        o1.deliver();  // ORD-1: delivered          (ShippedState -> DeliveredState)

        // Illegal action falls through to the abstract base's default handler.
        Order o2 = new Order("ORD-2");
        o2.deliver();  // ORD-2: illegal action 'deliver' in state CreatedState

        // "Cannot cancel a shipped order" is true BY DEFAULT — ShippedState doesn't override cancel().
        Order o3 = new Order("ORD-3");
        o3.pay(); o3.ship();
        o3.cancel();   // ORD-3: illegal action 'cancel' in state ShippedState

        // But cancelling while PAID is fine.
        Order o4 = new Order("ORD-4");
        o4.pay();
        o4.cancel();   // ORD-4: cancelled + refunded  (PaidState -> CancelledState)
    }
}
```
</details>

**Code walkthrough:** Compare `ShippedState` here to Variant 1 — it shrank from four methods to one. It overrides `deliver()` (the only legal action from `SHIPPED`) and *inherits* the base's illegal-handler for `pay()`, `ship()`, and `cancel()`. That inheritance is what makes "you cannot cancel a shipped order" true **by default** rather than by remembering to write a no-op. The two terminal states, `DeliveredState` and `CancelledState`, are now empty — they legally do nothing, so they override nothing, and every action on them prints the illegal message. Transitions are self-contained: `CreatedState.pay()` prints the receipt line and then hands the context a brand-new `PaidState`. The Context shrank too — no more `paid()`/`shipped()` accessors — because states construct their own successors. The cost of that convenience: `CreatedState` now *names* `PaidState` directly (`new PaidState(ctx)`), so the states are compile-time-coupled to their neighbors, and each transition allocates a fresh object.

**Pros:** Default "illegal action" behavior lives in the abstract base, so concrete states override only legal actions (much less boilerplate — terminal states are empty); each state fully owns its outgoing transitions; the successor graph is readable class-by-class.
**Cons:** States are coupled to their successor classes (`new PaidState(...)` names a concrete type); allocating a new state object per transition creates garbage; the *global* transition graph isn't visible in one place (you must read every class to reconstruct it).
**Mechanism:** *decentralized transition ownership* — behavior and next-state selection are co-located, and the abstract base provides a **uniform default** (throw/log) for actions a state doesn't support, an elegant way to encode "illegal transition" through inheritance rather than repetition.

---

### Variant 3: Enum-Based State Machine (Java idiom)

**What problem it solves:** Variant 2 allocated a new state object on every transition and scattered the states across five class files. Java `enum`s fix both: an enum can declare **abstract methods overridden per constant**, so each state becomes a **singleton enum constant** — no `new` per transition, no separate files, and states get free `name()`, `valueOf()`, `EnumSet`/`EnumMap`, and clean serialization. Critically, because the action methods are `abstract`, the compiler **forces every state to implement every action** — you literally cannot forget a case, which a `switch` can never guarantee. This is Joshua Bloch's recommended idiom (*Effective Java*, Item 34) for a fixed, known set of states. Each action returns the next state constant, and the Context just reassigns its field.

<details>
<summary>💻 Click to expand code — enum state machine (Order lifecycle)</summary>

```java
// Each enum constant IS a state and defines the behavior for all four actions.
// Returning `this` = "stay here" (a no-op / illegal action); returning another
// constant = a transition.
public enum OrderStatus {
    CREATED {
        @Override OrderStatus pay(Order o)     { log(o, "payment captured"); return PAID; }
        @Override OrderStatus cancel(Order o)  { log(o, "cancelled");        return CANCELLED; }
        @Override OrderStatus ship(Order o)    { illegal(o, "ship");    return this; }
        @Override OrderStatus deliver(Order o) { illegal(o, "deliver"); return this; }
    },
    PAID {
        @Override OrderStatus ship(Order o)    { log(o, "shipped");            return SHIPPED; }
        @Override OrderStatus cancel(Order o)  { log(o, "cancelled + refunded"); return CANCELLED; }
        @Override OrderStatus pay(Order o)     { illegal(o, "pay");     return this; }
        @Override OrderStatus deliver(Order o) { illegal(o, "deliver"); return this; }
    },
    SHIPPED {
        @Override OrderStatus deliver(Order o) { log(o, "delivered"); return DELIVERED; }
        @Override OrderStatus pay(Order o)     { illegal(o, "pay");    return this; }
        @Override OrderStatus ship(Order o)    { illegal(o, "ship");   return this; }
        @Override OrderStatus cancel(Order o)  { illegal(o, "cancel — already shipped"); return this; }
    },
    DELIVERED {   // terminal: every action illegal, stays put
        @Override OrderStatus pay(Order o)     { illegal(o, "pay");     return this; }
        @Override OrderStatus ship(Order o)    { illegal(o, "ship");    return this; }
        @Override OrderStatus deliver(Order o) { illegal(o, "deliver"); return this; }
        @Override OrderStatus cancel(Order o)  { illegal(o, "cancel");  return this; }
    },
    CANCELLED {   // terminal
        @Override OrderStatus pay(Order o)     { illegal(o, "pay");     return this; }
        @Override OrderStatus ship(Order o)    { illegal(o, "ship");    return this; }
        @Override OrderStatus deliver(Order o) { illegal(o, "deliver"); return this; }
        @Override OrderStatus cancel(Order o)  { illegal(o, "cancel");  return this; }
    };

    // Abstract => EVERY constant MUST implement each action (compiler-enforced completeness).
    abstract OrderStatus pay(Order o);
    abstract OrderStatus ship(Order o);
    abstract OrderStatus deliver(Order o);
    abstract OrderStatus cancel(Order o);

    // Shared helpers (enums can't share a base class, so use static helpers).
    void log(Order o, String msg)     { System.out.println(o.id() + ": " + msg); }
    void illegal(Order o, String act) { System.out.println(o.id() + ": illegal '" + act + "' in " + name()); }
}

class Order {
    private OrderStatus status = OrderStatus.CREATED;   // singleton constant, no allocation
    private final String id;
    Order(String id) { this.id = id; }

    // Each action reassigns the field to whatever the current state returns.
    public void pay()     { status = status.pay(this); }
    public void ship()    { status = status.ship(this); }
    public void deliver() { status = status.deliver(this); }
    public void cancel()  { status = status.cancel(this); }

    public OrderStatus status() { return status; }
    String id() { return id; }
}

// ---------- Usage demo ----------
class OrderDemoV3 {
    public static void main(String[] args) {
        // Happy path — each call reassigns the enum field to the returned constant.
        Order o1 = new Order("ORD-1");
        o1.pay();      // ORD-1: payment captured   (CREATED -> PAID)
        o1.ship();     // ORD-1: shipped            (PAID -> SHIPPED)
        o1.deliver();  // ORD-1: delivered          (SHIPPED -> DELIVERED)
        System.out.println(o1.id() + " final: " + o1.status()); // ORD-1 final: DELIVERED

        // Illegal action: the state returns `this`, so the status is unchanged.
        Order o2 = new Order("ORD-2");
        o2.ship();     // ORD-2: illegal 'ship' in CREATED
        System.out.println(o2.status()); // still CREATED

        // Cancel legal from PAID, illegal from SHIPPED.
        Order o3 = new Order("ORD-3");
        o3.pay();
        o3.cancel();   // ORD-3: cancelled + refunded  (PAID -> CANCELLED)
        Order o4 = new Order("ORD-4");
        o4.pay(); o4.ship();
        o4.cancel();   // ORD-4: illegal 'cancel — already shipped' in SHIPPED
    }
}
```
</details>

**Code walkthrough:** There are no `new` states anywhere and no separate files — the five constants `CREATED … CANCELLED` *are* the five singleton state objects. `Order.pay()` calls `status.pay(this)` and assigns the result back: from `CREATED`, `pay()` returns `PAID`, so the field advances; from `PAID`, `pay()` calls `illegal(...)` and returns `this`, so the field stays put. The convention "return a different constant to transition, return `this` to stay" makes the whole graph readable at a glance. The real payoff is the four `abstract` declarations at the bottom: if you add a `RETURNED` constant, the code **won't compile** until `RETURNED` implements `pay`, `ship`, `deliver`, and `cancel` — the compiler hands you a checklist. Note the shared `log`/`illegal` helpers: because enum constants already extend `java.lang.Enum`, they can't share a common base class the way Variant 2's states did, so cross-state helpers must be plain methods or statics on the enum itself. Mutable order data (id, totals) stays on the `Order` context, never on the enum, because the constant is a singleton shared by every order.

**Pros:** **Compiler-enforced completeness** — because the enum methods are `abstract`, every state *must* implement every action (impossible to forget a case, unlike a `switch`); states are singletons (zero allocation per transition); free `name()`, `ordinal()`, `valueOf()`, `EnumSet`/`EnumMap`, and serialization; all states live in one readable file.
**Cons:** All states in one enum file can get large; you can't add states at runtime (fixed set at compile time); enum constants can't extend a shared base class (enums already extend `java.lang.Enum`), so you can't share implementation via inheritance — only via helper methods; per-instance state can't live on the enum (it's a singleton), so mutable data must stay on the Context.
**Mechanism:** *constant-specific method bodies* — each enum constant is effectively an anonymous subclass of the enum with its own method overrides, dispatched virtually. The `abstract` declaration is the completeness guarantee that a `switch` cannot give.

---

### Variant 4: Production-Grade — Stateless Flyweight States + Transition Table

**Why it's recommended:** In high-throughput systems (many Context instances — millions of orders, connections, sessions) you don't want a state object *per instance*. The production pattern makes concrete states **stateless singletons (flyweights)**: the state object holds *no* per-context data, so a single shared instance serves all contexts; all mutable data lives on the Context passed in as a parameter. Optionally, a **centralized transition table** (`Map<State, Map<Event, State>>`) makes the whole state graph auditable in one place and lets you validate/visualize it. This is what real order engines, protocol stacks, and workflow engines use.

<details>
<summary>💻 Click to expand code — stateless flyweight states + explicit transition validation</summary>

```java
import java.util.*;

// The Context holds ALL mutable data; states are stateless singletons.
final class Order {
    private OrderState state = NewState.INSTANCE;   // shared singleton
    private final String id;
    private final List<String> auditLog = new ArrayList<>();

    Order(String id) { this.id = id; }

    // Public API delegates to current state, passing `this` so the state stays stateless.
    void pay()    { state.pay(this); }
    void ship()   { state.ship(this); }
    void deliver(){ state.deliver(this); }
    void cancel() { state.cancel(this); }

    // Package-private transition hook used by states.
    void transitionTo(OrderState next) {
        auditLog.add(state.name() + " -> " + next.name());
        this.state = next;
    }
    String currentState() { return state.name(); }
    List<String> audit()  { return auditLog; }
    String id()           { return id; }
}

// State interface with a default "illegal transition" for unsupported actions.
interface OrderState {
    String name();
    default void pay(Order o)     { illegal(o, "pay"); }
    default void ship(Order o)    { illegal(o, "ship"); }
    default void deliver(Order o) { illegal(o, "deliver"); }
    default void cancel(Order o)  { illegal(o, "cancel"); }
    default void illegal(Order o, String action) {
        throw new IllegalStateException(
            "Cannot " + action + " order " + o.id() + " in state " + name());
    }
}

// Each concrete state is a STATELESS singleton (flyweight) — one instance, shared by all orders.
enum NewState implements OrderState {
    INSTANCE;
    public String name() { return "NEW"; }
    public void pay(Order o)    { o.transitionTo(PaidState.INSTANCE); }
    public void cancel(Order o) { o.transitionTo(CancelledState.INSTANCE); }
}
enum PaidState implements OrderState {
    INSTANCE;
    public String name() { return "PAID"; }
    public void ship(Order o)   { o.transitionTo(ShippedState.INSTANCE); }
    public void cancel(Order o) { o.transitionTo(CancelledState.INSTANCE); } // refund path
}
enum ShippedState implements OrderState {
    INSTANCE;
    public String name() { return "SHIPPED"; }
    public void deliver(Order o) { o.transitionTo(DeliveredState.INSTANCE); }
    // cancel/ pay illegal here -> throws via default
}
enum DeliveredState implements OrderState {
    INSTANCE;
    public String name() { return "DELIVERED"; } // terminal: all actions illegal
}
enum CancelledState implements OrderState {
    INSTANCE;
    public String name() { return "CANCELLED"; } // terminal
}

// ---------- Usage demo ----------
class OrderDemoV4 {
    public static void main(String[] args) {
        // Two orders SHARE the same singleton state objects — no per-order state allocation.
        Order o1 = new Order("ORD-1");
        o1.pay();      // NEW -> PAID
        o1.ship();     // PAID -> SHIPPED
        o1.deliver();  // SHIPPED -> DELIVERED
        System.out.println(o1.currentState() + " " + o1.audit());
        // DELIVERED [NEW -> PAID, PAID -> SHIPPED, SHIPPED -> DELIVERED]

        // Illegal transition throws loudly (default handler on the interface).
        Order o2 = new Order("ORD-2");
        try {
            o2.ship();  // can't ship a NEW (unpaid) order
        } catch (IllegalStateException ex) {
            System.out.println("Blocked: " + ex.getMessage()); // Cannot ship order ORD-2 in state NEW
        }

        // Proof the flyweights are shared, not per-order: both orders point at the SAME instance.
        Order o3 = new Order("ORD-3"); o3.pay();
        Order o4 = new Order("ORD-4"); o4.pay();
        System.out.println(o3.currentState().equals(o4.currentState())); // true — one PaidState.INSTANCE
    }
}
```
</details>

**Pros:** **Zero per-instance state-object allocation** — one shared flyweight per state, so 10M orders share 5 state objects; inherently thread-safe *state objects* (they're immutable singletons); illegal transitions throw loudly via the interface `default`; using enum singletons gives free identity, serialization, and `==` comparison. A transition table (below) makes the graph auditable.
**Cons:** Requires discipline that state objects stay *truly stateless* (any field defeats sharing and breaks thread safety); passing the Context into every method is slightly more verbose; a purely decentralized graph is still spread across classes unless you add an explicit table.
**Mechanism:** *Flyweight + State* — the state's *intrinsic* state (its behavior/transitions) is shared and immutable; the *extrinsic* state (order id, audit log, amounts) is passed in via the `Order` parameter. Because the flyweight holds no mutable fields, a single instance is safely shared across threads and contexts (thread safety by immutability).

<details>
<summary>💻 Click to expand — optional centralized transition table (auditable graph)</summary>

```java
import java.util.*;

// When you want the WHOLE graph in one auditable place (great for validation/visualization).
enum OrderEvent { PAY, SHIP, DELIVER, CANCEL }
enum St { NEW, PAID, SHIPPED, DELIVERED, CANCELLED }

class OrderMachine {
    // state -> (event -> nextState). Missing entry = illegal transition.
    private static final Map<St, Map<OrderEvent, St>> TABLE = Map.of(
        St.NEW,     Map.of(OrderEvent.PAY, St.PAID, OrderEvent.CANCEL, St.CANCELLED),
        St.PAID,    Map.of(OrderEvent.SHIP, St.SHIPPED, OrderEvent.CANCEL, St.CANCELLED),
        St.SHIPPED, Map.of(OrderEvent.DELIVER, St.DELIVERED),
        St.DELIVERED, Map.of(),
        St.CANCELLED, Map.of()
    );

    private St state = St.NEW;

    public synchronized void fire(OrderEvent e) {
        St next = TABLE.getOrDefault(state, Map.of()).get(e);
        if (next == null)
            throw new IllegalStateException("Illegal event " + e + " in state " + state);
        System.out.println(state + " --" + e + "--> " + next);
        state = next;
    }
    public St state() { return state; }
}
// The table is data: you can validate reachability, detect dead states, or render a diagram
// without executing anything — a big win for compliance-heavy workflow engines.
```
</details>

---

## 🎨 Real-World Example

Below are **three** complete, production-style scenarios. Read them together: they show the *same* State pattern applied to three different domains, so you can see what stays constant (stateless singleton states, Context owns mutable data, illegal transitions fail loudly) and what changes per domain (guards, side effects, timing rules). Each is self-contained and collapsible.

- **Scenario 1 — Document Review Workflow** (CMS / approval system): guards, notifications, audit, concurrency.
- **Scenario 2 — Ride Lifecycle** (Uber/Lyft): `REQUESTED → ACCEPTED → ARRIVING → IN_PROGRESS → COMPLETED` with a driver/GPS side effects and a cancellation branch.
- **Scenario 3 — Food Delivery** (DoorDash/Swiggy): `PLACED → PREPARING → READY → PICKED_UP → DELIVERED` spanning three actors (restaurant, courier, customer).

### Scenario 1 — Document Review Workflow (guards + audit + concurrency)

**The problem:** In a content platform (a CMS, or a code-review/approval system), a `Document` moves through `DRAFT → IN_REVIEW → APPROVED → PUBLISHED`, with a `reject()` branch that sends it back to `DRAFT`. The behavior of `submit()`, `approve()`, `reject()`, and `publish()` depends entirely on the current state, *and* there are extra real-world concerns: only **editors** may approve/reject (authorization guards), each transition triggers **side effects** (notify reviewers, notify the author, notify subscribers), every move must be **audited** for compliance, and multiple reviewers may act on the same document **concurrently** — so a naive design can approve a document twice. This is a FAANG-favorite because it forces you to layer guards, side effects, auditing, and thread safety onto the core state machine.

**The design:** States are **stateless enum singletons** (`DocState`); the `Document` (Context) owns all mutable data (content, audit log) and serializes transitions under a lock; illegal actions throw via a default handler; a `Notifier` collaborator performs side effects co-located with the transition that causes them.

<details>
<summary>💻 Click to expand Scenario 1 — full code (document workflow with guards, audit, demo + concurrency test)</summary>

```java
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// ---------- Context ----------
final class Document {
    private volatile DocState state = DocState.DRAFT;   // volatile: visible across threads
    private final String id;
    private String content;
    private final Deque<String> audit = new ConcurrentLinkedDeque<>();
    private final Object lock = new Object();            // guards transition atomicity

    Document(String id, String content) { this.id = id; this.content = content; }

    // Public API — each call is a guarded, atomic transition.
    void submit(String user)  { withTransition(() -> state.submit(this, user)); }
    void approve(String user) { withTransition(() -> state.approve(this, user)); }
    void reject(String user, String reason) { withTransition(() -> state.reject(this, user, reason)); }
    void publish(String user) { withTransition(() -> state.publish(this, user)); }

    private void withTransition(Runnable action) {
        synchronized (lock) { action.run(); }            // serialize concurrent transitions
    }

    // Called by states to move + record. Only invoked while holding `lock`.
    void moveTo(DocState next, String note) {
        audit.add(state + " -> " + next + " (" + note + ")");
        this.state = next;
    }
    DocState state()        { return state; }
    String id()             { return id; }
    List<String> auditLog() { return new ArrayList<>(audit); }
}

// ---------- State: stateless singletons via enum + default illegal handling ----------
enum DocState {
    DRAFT {
        @Override void submit(Document d, String user) {
            d.moveTo(IN_REVIEW, "submitted by " + user);
            Notifier.notifyReviewers(d.id());
        }
    },
    IN_REVIEW {
        @Override void approve(Document d, String user) {
            requireEditor(user);
            d.moveTo(APPROVED, "approved by " + user);
        }
        @Override void reject(Document d, String user, String reason) {
            requireEditor(user);
            d.moveTo(DRAFT, "rejected by " + user + ": " + reason); // back to author
            Notifier.notifyAuthor(d.id(), reason);
        }
    },
    APPROVED {
        @Override void publish(Document d, String user) {
            d.moveTo(PUBLISHED, "published by " + user);
            Notifier.notifySubscribers(d.id());
        }
        // an approved doc can still be sent back
        @Override void reject(Document d, String user, String reason) {
            requireEditor(user);
            d.moveTo(DRAFT, "un-approved by " + user + ": " + reason);
        }
    },
    PUBLISHED { /* terminal — every action illegal by default */ };

    // State-dependent operations, defaulting to "illegal transition".
    void submit(Document d, String user)                 { illegal("submit"); }
    void approve(Document d, String user)                { illegal("approve"); }
    void reject(Document d, String user, String reason)  { illegal("reject"); }
    void publish(Document d, String user)                { illegal("publish"); }

    private void illegal(String action) {
        throw new IllegalStateException("Cannot '" + action + "' while " + name());
    }
    // Shared guard (states can't share a base class, so use a static helper).
    static void requireEditor(String user) {
        if (!user.startsWith("editor:"))
            throw new SecurityException(user + " is not authorized to review");
    }
}

// ---------- Collaborator (side effects on transition) ----------
class Notifier {
    static void notifyReviewers(String id)   { /* enqueue emails */ }
    static void notifyAuthor(String id, String r) { /* ... */ }
    static void notifySubscribers(String id) { /* ... */ }
}

// ---------- Demo / usage: multiple call sites + illegal transition + concurrency ----------
public class DocumentWorkflowDemo {
    public static void main(String[] args) throws Exception {
        Document doc = new Document("DOC-1", "Design proposal");

        // Call site 1: happy path
        doc.submit("author:alice");        // DRAFT -> IN_REVIEW
        doc.approve("editor:bob");         // IN_REVIEW -> APPROVED
        doc.publish("editor:bob");         // APPROVED -> PUBLISHED
        System.out.println(doc.state());   // PUBLISHED

        // Call site 2: illegal transition is loud, not silent
        try {
            doc.submit("author:alice");    // can't submit a PUBLISHED doc
        } catch (IllegalStateException ex) {
            System.out.println("Blocked: " + ex.getMessage()); // Cannot 'submit' while PUBLISHED
        }

        // Call site 3: authorization guard
        Document d2 = new Document("DOC-2", "Draft");
        d2.submit("author:carol");
        try {
            d2.approve("author:carol");    // authors can't approve
        } catch (SecurityException ex) {
            System.out.println("Blocked: " + ex.getMessage());
        }

        // Call site 4: concurrency test — 50 threads race to approve the same in-review doc.
        // Because transitions are synchronized, exactly ONE approve wins; the rest hit
        // "Cannot 'approve' while APPROVED" — proving transitions are atomic (no double-approve).
        Document d3 = new Document("DOC-3", "x");
        d3.submit("editor:root");
        ExecutorService pool = Executors.newFixedThreadPool(16);
        AtomicInteger approvals = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(50);
        for (int i = 0; i < 50; i++) {
            pool.submit(() -> {
                try { d3.approve("editor:root"); approvals.incrementAndGet(); }
                catch (IllegalStateException ignored) { /* lost the race — expected */ }
                finally { latch.countDown(); }
            });
        }
        latch.await();
        pool.shutdown();
        System.out.println("Successful approvals: " + approvals.get()); // exactly 1
        System.out.println("Audit: " + d3.auditLog());
    }
}
```
</details>

**Code walkthrough:** The `Document` context never branches on state — `approve(user)` just calls `state.approve(this, user)` inside `withTransition`, which holds a lock so two threads can't transition at once. Each `DocState` constant overrides only the actions legal in that state; everything else inherits the `illegal(...)` default that throws. Notice how the **guard** (`requireEditor`) and the **side effect** (`Notifier.notifyReviewers`) live *inside* the specific transition that needs them — authorization is checked only in `IN_REVIEW.approve`, not smeared across the context. `moveTo` is the single choke point that records the audit line and swaps the state, so every transition is logged exactly once. In the demo, call site 4 fires 50 threads at the same `approve()`; because the transition is serialized and the second approve finds the doc already `APPROVED`, exactly one succeeds and the other 49 hit the illegal handler — proving the design prevents double-approval.

**Why this is a strong FAANG answer:** it shows (1) the canonical State structure with **stateless enum-singleton states**, (2) illegal transitions failing *loudly* via a default handler instead of silent no-ops, (3) **per-state guards** (authorization checked only where meaningful) and **transition side effects** (notifications) co-located with the transition, (4) an **audit trail** for compliance, and (5) genuine **thread safety** — transitions are serialized under a lock and the state field is `volatile`, so a burst of concurrent `approve()` calls produces exactly one successful transition (no double-approve), which is the real bug this design prevents.

### Scenario 2 — Ride Lifecycle (Uber/Lyft)

**The problem:** A ride-hailing trip moves through `REQUESTED → ACCEPTED → ARRIVING → IN_PROGRESS → COMPLETED`, with a `CANCELLED` branch reachable *only before the trip starts* (once `IN_PROGRESS`, you can't cancel — you've been picked up). Each action means something completely different depending on where the ride is: `assignDriver()` is legal only from `REQUESTED`; `startTrip()` only after the driver has arrived; `cancel()` refunds nothing before `ACCEPTED` but may charge a cancellation fee after. There are heavy **side effects** per transition — matching a driver, sending push notifications, starting GPS tracking, starting the fare meter, charging the rider — and the wrong transition (starting a trip before the driver arrives, completing a ride that never started) is a serious production bug. This is the archetypal "behavior + illegal-transition + side-effect-per-state" problem State was built for.

**The design:** `RideStatus` is an enum of stateless singletons; the `Ride` context carries the mutable trip data (rider, driver, fare, GPS). `cancel()` is legal from the three pre-trip states and illegal afterward — encoded by which constants override it. Side effects (dispatch, notify, meter) are attached to the exact transition that triggers them.

<details>
<summary>💻 Click to expand Scenario 2 — full code (ride lifecycle with side effects + demo)</summary>

```java
import java.util.*;

// ---------- Context: owns all mutable trip data ----------
final class Ride {
    private RideStatus status = RideStatus.REQUESTED;
    private final String id;
    private final String rider;
    private String driver;            // assigned when matched
    private double fare;              // accrues during the trip
    private final List<String> events = new ArrayList<>();

    Ride(String id, String rider) { this.id = id; this.rider = rider; }

    // Public API — each delegates to the current state, passing `this`.
    void assignDriver(String driver) { status = status.assignDriver(this, driver); }
    void driverArrived()             { status = status.driverArrived(this); }
    void startTrip()                 { status = status.startTrip(this); }
    void endTrip(double finalFare)   { status = status.endTrip(this, finalFare); }
    void cancel(String who)          { status = status.cancel(this, who); }

    // Data mutators used by states (extrinsic state lives here, not on the singletons).
    void setDriver(String d) { this.driver = d; }
    void setFare(double f)   { this.fare = f; }
    void log(String e)       { events.add(e); System.out.println(id + ": " + e); }

    RideStatus status() { return status; }
    String id()     { return id; }
    String driver() { return driver; }
    double fare()   { return fare; }
}

// ---------- State: stateless enum singletons ----------
enum RideStatus {
    REQUESTED {
        @Override RideStatus assignDriver(Ride r, String driver) {
            r.setDriver(driver);
            r.log("driver " + driver + " matched — heading to rider");   // side effect: dispatch + notify
            return ACCEPTED;
        }
        @Override RideStatus cancel(Ride r, String who) {
            r.log("cancelled by " + who + " (no charge — no driver yet)");
            return CANCELLED;
        }
    },
    ACCEPTED {
        @Override RideStatus driverArrived(Ride r) {
            r.log("driver " + r.driver() + " has arrived at pickup");     // side effect: push notification
            return ARRIVING;
        }
        @Override RideStatus cancel(Ride r, String who) {
            r.log("cancelled by " + who + " (driver en route — small fee may apply)");
            return CANCELLED;
        }
    },
    ARRIVING {   // driver is at the curb, waiting for the rider to board
        @Override RideStatus startTrip(Ride r) {
            r.setFare(2.50);                                              // base fare
            r.log("trip started — fare meter running, GPS tracking on");  // side effect: start meter + tracking
            return IN_PROGRESS;
        }
        @Override RideStatus cancel(Ride r, String who) {
            r.log("cancelled by " + who + " before boarding (cancellation fee charged)");
            return CANCELLED;
        }
    },
    IN_PROGRESS {   // rider is in the car — NO cancel allowed anymore
        @Override RideStatus endTrip(Ride r, double finalFare) {
            r.setFare(finalFare);
            r.log("trip completed — charging rider $" + finalFare);       // side effect: capture payment + receipt
            return COMPLETED;
        }
    },
    COMPLETED  { /* terminal */ },
    CANCELLED  { /* terminal */ };

    // All actions default to illegal; each state overrides only what it allows.
    RideStatus assignDriver(Ride r, String d) { return illegal(r, "assignDriver"); }
    RideStatus driverArrived(Ride r)          { return illegal(r, "driverArrived"); }
    RideStatus startTrip(Ride r)              { return illegal(r, "startTrip"); }
    RideStatus endTrip(Ride r, double fare)   { return illegal(r, "endTrip"); }
    RideStatus cancel(Ride r, String who)     { return illegal(r, "cancel"); }

    private RideStatus illegal(Ride r, String action) {
        throw new IllegalStateException(r.id() + ": cannot '" + action + "' while " + name());
    }

    // ---------- Demo ----------
    public static void main(String[] args) {
        // Happy path: request -> match -> arrive -> start -> complete
        Ride ride = new Ride("RIDE-1", "alice");
        ride.assignDriver("bob");     // REQUESTED  -> ACCEPTED
        ride.driverArrived();          // ACCEPTED   -> ARRIVING
        ride.startTrip();              // ARRIVING   -> IN_PROGRESS
        ride.endTrip(14.75);           // IN_PROGRESS -> COMPLETED
        System.out.println("Final status: " + ride.status() + ", fare $" + ride.fare());

        // Illegal transition: cannot cancel once the trip is underway.
        Ride ride2 = new Ride("RIDE-2", "carol");
        ride2.assignDriver("dave");
        ride2.driverArrived();
        ride2.startTrip();             // now IN_PROGRESS
        try {
            ride2.cancel("carol");     // rider tries to cancel mid-trip
        } catch (IllegalStateException ex) {
            System.out.println("Blocked: " + ex.getMessage()); // cannot 'cancel' while IN_PROGRESS
        }

        // Legal cancel BEFORE boarding.
        Ride ride3 = new Ride("RIDE-3", "erin");
        ride3.assignDriver("frank");   // ACCEPTED
        ride3.cancel("erin");          // ACCEPTED -> CANCELLED (fee may apply)
        System.out.println("Ride3 status: " + ride3.status());
    }
}
```
</details>

**Code walkthrough:** The single most important design decision here is *which states override `cancel()`*. `REQUESTED`, `ACCEPTED`, and `ARRIVING` each override it (with different fee messaging), but `IN_PROGRESS` does **not** — so it inherits the default `illegal(...)` and throws. That one structural fact — "cancel is simply not defined on `IN_PROGRESS`" — is how the business rule "you can't cancel a ride you're already taking" becomes impossible to violate, instead of being an `if` someone might forget. Each transition carries its natural side effect exactly where it belongs: matching a driver in `REQUESTED.assignDriver`, starting the meter and GPS in `ARRIVING.startTrip`, capturing payment in `IN_PROGRESS.endTrip`. All the mutable trip data (driver, fare) lives on the `Ride` context and is passed into the singleton states, so the enum constants stay stateless and shareable across every ride in the system.

**Why this is a strong FAANG answer:** it models a genuinely stateful, high-stakes lifecycle where **illegal transitions are safety bugs** (charging for a trip that never started), demonstrates **per-transition side effects** (dispatch, notifications, metering, payment) attached to the precise state that owns them, and shows the pattern's core payoff — encoding "cancel is legal only before boarding" purely by *which constants define the method*, with zero conditional logic.

### Scenario 3 — Food Delivery (DoorDash/Swiggy)

**The problem:** A food-delivery order moves through `PLACED → PREPARING → READY → PICKED_UP → DELIVERED`. What makes this interesting — and different from the ride example — is that the transitions are driven by **three different actors**: the *restaurant* accepts and prepares (`PLACED → PREPARING → READY`), the *courier* collects and delivers (`READY → PICKED_UP → DELIVERED`), and the *customer* can cancel, but **only before the food is being prepared** (once the kitchen has started, the ingredients are committed). Each action is legal only in a specific state and often only for a specific actor, and each transition fires side effects across the system: notify the courier when food is ready, send the customer a live-tracking link on pickup, release payment to the restaurant on delivery. Getting the ordering wrong (marking an order `READY` before it's `PREPARING`, or `DELIVERED` before `PICKED_UP`) corrupts the whole logistics pipeline.

**The design:** `DeliveryStatus` is an enum of stateless singletons. Actions are named for the actor-driven event that causes them (`startPreparing`, `markReady`, `pickUp`, `deliver`, `cancel`). `cancel()` is overridden only by `PLACED` (and, as a courtesy, we allow it during `PREPARING` with a fee) but not by the later states — encoding "you can't cancel food that's already out for delivery." The `DeliveryOrder` context holds the restaurant, courier, and item data.

<details>
<summary>💻 Click to expand Scenario 3 — full code (food delivery across three actors + demo)</summary>

```java
import java.util.*;

// ---------- Context: owns all mutable order data ----------
final class DeliveryOrder {
    private DeliveryStatus status = DeliveryStatus.PLACED;
    private final String id;
    private final String customer;
    private final String restaurant;
    private String courier;                 // assigned when food is ready
    private final List<String> timeline = new ArrayList<>();

    DeliveryOrder(String id, String customer, String restaurant) {
        this.id = id; this.customer = customer; this.restaurant = restaurant;
    }

    // Public API — delegates each actor-driven event to the current state.
    void startPreparing()          { status = status.startPreparing(this); }   // restaurant accepts
    void markReady()               { status = status.markReady(this); }         // kitchen done
    void pickUp(String courier)    { status = status.pickUp(this, courier); }   // courier collects
    void deliver()                 { status = status.deliver(this); }           // courier drops off
    void cancel(String who)        { status = status.cancel(this, who); }       // customer cancels

    // Data mutators used by states.
    void setCourier(String c) { this.courier = c; }
    void log(String e)        { timeline.add(e); System.out.println(id + ": " + e); }

    DeliveryStatus status() { return status; }
    String id()         { return id; }
    String restaurant() { return restaurant; }
    String courier()    { return courier; }
}

// ---------- State: stateless enum singletons ----------
enum DeliveryStatus {
    PLACED {   // waiting for the restaurant to accept
        @Override DeliveryStatus startPreparing(DeliveryOrder o) {
            o.log(o.restaurant() + " accepted the order — now cooking");   // side effect: notify restaurant kitchen
            return PREPARING;
        }
        @Override DeliveryStatus cancel(DeliveryOrder o, String who) {
            o.log("cancelled by " + who + " (full refund — kitchen hadn't started)");
            return CANCELLED;
        }
    },
    PREPARING {   // food is being cooked
        @Override DeliveryStatus markReady(DeliveryOrder o) {
            o.log("food is ready — finding a nearby courier");             // side effect: dispatch courier search
            return READY;
        }
        @Override DeliveryStatus cancel(DeliveryOrder o, String who) {
            o.log("cancelled by " + who + " (partial refund — ingredients already used)");
            return CANCELLED;
        }
    },
    READY {   // waiting for a courier to collect — cancel NO LONGER allowed
        @Override DeliveryStatus pickUp(DeliveryOrder o, String courier) {
            o.setCourier(courier);
            o.log("picked up by " + courier + " — customer gets live tracking link"); // side effect: start tracking
            return PICKED_UP;
        }
    },
    PICKED_UP {   // courier is en route to the customer
        @Override DeliveryStatus deliver(DeliveryOrder o) {
            o.log("delivered to customer — releasing payment to " + o.restaurant()); // side effect: settle payment
            return DELIVERED;
        }
    },
    DELIVERED { /* terminal */ },
    CANCELLED { /* terminal */ };

    // All actions default to illegal; each state overrides only its legal ones.
    DeliveryStatus startPreparing(DeliveryOrder o)        { return illegal(o, "startPreparing"); }
    DeliveryStatus markReady(DeliveryOrder o)             { return illegal(o, "markReady"); }
    DeliveryStatus pickUp(DeliveryOrder o, String c)      { return illegal(o, "pickUp"); }
    DeliveryStatus deliver(DeliveryOrder o)               { return illegal(o, "deliver"); }
    DeliveryStatus cancel(DeliveryOrder o, String who)    { return illegal(o, "cancel"); }

    private DeliveryStatus illegal(DeliveryOrder o, String action) {
        throw new IllegalStateException(o.id() + ": cannot '" + action + "' while " + name());
    }

    // ---------- Demo ----------
    public static void main(String[] args) {
        // Happy path across three actors: customer places, restaurant cooks, courier delivers.
        DeliveryOrder order = new DeliveryOrder("FD-1", "alice", "Pizza Palace");
        order.startPreparing();      // PLACED     -> PREPARING  (restaurant)
        order.markReady();            // PREPARING  -> READY      (kitchen)
        order.pickUp("bob");          // READY      -> PICKED_UP  (courier)
        order.deliver();              // PICKED_UP  -> DELIVERED  (courier)
        System.out.println("Final: " + order.status());

        // Illegal: courier can't pick up food that isn't ready yet.
        DeliveryOrder o2 = new DeliveryOrder("FD-2", "carol", "Sushi Co");
        o2.startPreparing();          // PREPARING
        try {
            o2.pickUp("dave");        // food still cooking!
        } catch (IllegalStateException ex) {
            System.out.println("Blocked: " + ex.getMessage()); // cannot 'pickUp' while PREPARING
        }

        // Illegal: customer can't cancel once it's out for delivery.
        DeliveryOrder o3 = new DeliveryOrder("FD-3", "erin", "Taco Town");
        o3.startPreparing();
        o3.markReady();
        o3.pickUp("frank");           // PICKED_UP
        try {
            o3.cancel("erin");        // too late — courier already has it
        } catch (IllegalStateException ex) {
            System.out.println("Blocked: " + ex.getMessage()); // cannot 'cancel' while PICKED_UP
        }

        // Legal cancel while still cooking (partial refund).
        DeliveryOrder o4 = new DeliveryOrder("FD-4", "grace", "Curry House");
        o4.startPreparing();          // PREPARING
        o4.cancel("grace");           // PREPARING -> CANCELLED
        System.out.println("FD-4: " + o4.status());
    }
}
```
</details>

**Code walkthrough:** This scenario highlights how State cleanly handles a lifecycle with **multiple actors**. Even though the restaurant, courier, and customer are different parties, they all interact with one `DeliveryOrder` through the same delegating API — the state object decides whether their action is legal *right now*. The ordering guarantees fall straight out of the structure: `pickUp()` is defined only on `READY`, so a courier physically cannot mark an order picked-up before the kitchen calls `markReady()` — the demo's second case proves it throws. The cancellation policy is expressed by *which* states override `cancel()`: `PLACED` (full refund) and `PREPARING` (partial refund) do, but `READY`, `PICKED_UP`, and `DELIVERED` do not, so cancelling an out-for-delivery order is impossible by construction — the third demo case shows the exception. Side effects sit on the transition that causes them: dispatching a courier search when the food becomes `READY`, starting the tracking link on `pickUp`, and settling payment to the restaurant on `deliver`.

**Why this is a strong FAANG answer:** it demonstrates State coordinating a **multi-actor workflow** through a single context, encodes ordering guarantees (`pickUp` requires `READY`, `deliver` requires `PICKED_UP`) and a nuanced cancellation policy purely in the *shape* of the state classes, and attaches cross-system side effects (courier dispatch, live tracking, payment settlement) to precisely the transitions that trigger them — exactly the correctness properties an interviewer probes for in a logistics-style design question.

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- An object's **behavior depends on its state**, and it must **change behavior at runtime** as the state changes.
- You have **operations with large, multipart conditionals** that branch on the same state variable, duplicated across many methods.
- **State transitions are complex** and you want them explicit, auditable, and hard to get wrong (illegal transitions rejected rather than silently ignored).
- The set of states and their legal transitions form a **well-defined state machine** (vending machine, TCP, order/workflow lifecycle, media player, game AI, parser/lexer).
- You want to add new states **without modifying** existing state code (Open/Closed) — each state is an independent class.
- You need **per-state guards, entry/exit actions, or side effects** attached cleanly to specific states.

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- **Few states, few state-dependent operations.** With 2 states and one method, a simple `if`/`boolean` is clearer than 3 classes. State pays off past ~3 states × several operations.
- **State rarely changes / behavior doesn't really vary by state.** If the "state" is just a data flag with no behavioral divergence, you don't need the pattern.
- **Transitions are trivial and linear** with no illegal-move concerns — a plain enum field may suffice.
- **You need runtime-defined states/transitions loaded from config.** A hardcoded class-per-state is rigid; a **data-driven transition table** or a dedicated state-machine engine (e.g., Spring StateMachine) fits better.
- **Over-engineering risk:** introducing State for a two-line toggle adds indirection and class sprawl that hurts readability more than it helps.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- **Single Responsibility Principle** — each state's behavior lives in its own class.
- **Open/Closed Principle** — add a new state by adding a class, without touching existing states or the context.
- Eliminates **duplicated conditionals** — no repeated `switch (state)` across methods.
- Makes **state transitions explicit** and localized; illegal transitions handled uniformly.
- State objects can be **shared as flyweights** (stateless singletons) for scale.

**Cons**

- **Class proliferation** — many small classes for many states; overkill for simple machines.
- **Transition logic can be scattered** across state classes (decentralized), making the *global* graph hard to see unless you add a transition table.
- **Coupling** — states often need a reference to the context (and sometimes to sibling states/successors).
- **Indirection** — behavior is one virtual call away; can obscure control flow for readers unfamiliar with the pattern.
- Deciding **who owns transitions** (states vs. context vs. table) is a real design cost.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

State is **structurally identical to Strategy** (both delegate to a swappable object implementing a common interface) — the difference is entirely **intent**, which is the classic interview trap.

| Pattern | Intent | Who swaps the object? | Do the objects know each other? | Key tell |
|---|---|---|---|---|
| **State** | Change behavior as *internal state* evolves; model a state machine | The state objects themselves (or the context) — **internally**, as a side effect | **Yes** — states know their successors and drive transitions | Object *appears to change class* over its lifecycle; transitions are the point |
| **Strategy** | Swap an interchangeable *algorithm* chosen by the client | The **client**, externally | **No** — strategies are independent, unaware of each other | Client picks an algorithm; no transitions |
| **State Machine (table-driven)** | Same as State but transitions are **data**, not code | A generic engine reads a transition table | N/A — states are data | Graph is config/data, not classes |
| **Command** | Encapsulate a *request* as an object (queue/undo/log) | Client creates commands; invoker runs them | No | Encapsulates an *action to perform*, not a mode of being |

The mnemonic: **Strategy is "how" (algorithm), State is "what mode am I in" (lifecycle). Strategy objects don't replace themselves; State objects do.**

</details>

## 📊 Comparison Table of Variants

<details>
<summary>📖 Click to expand</summary>

| Axis | V0: if/switch | V1: Basic state objects | V2: States own transitions | V3: Enum machine | V4: Flyweight + table |
|---|---|---|---|---|---|
| **Duplication** | High (N·M cases) | None | None | None | None |
| **Completeness guarantee** | ❌ (forget a case) | ❌ (forget a method) | ⚠️ (base default) | ✅ compiler-enforced | ✅ (default throws) |
| **Adding a state** | Edit every method | Add a class | Add a class | Add a constant | Add a constant/table row |
| **Transition ownership** | Scattered | Context | States (decentralized) | States (enum) | States or central table |
| **Allocation per transition** | 0 | 0 (pre-created) | 1 (new state obj) | 0 (singletons) | 0 (flyweights) |
| **Thread-safe state objects** | N/A | ⚠️ (bound to ctx) | ⚠️ | ✅ (singletons) | ✅ (immutable) |
| **Global graph visible?** | Somewhat | No | No | Somewhat (one file) | ✅ (table) |
| **Best for** | 2–3 trivial states | Learning/small | Textbook clarity | Fixed state set | High-scale/auditable |

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

### Pitfall 1: Putting mutable per-context data on a shared flyweight state

**What goes wrong:** You make states singletons (for scale) but then store context-specific data (like `count` or `currentUser`) as a *field* on the state. Now all contexts sharing that singleton corrupt each other's data, and it's a data race under concurrency.

<details>
<summary>💻 Click to expand — the failure</summary>

```java
enum HasCoinState implements State {
    INSTANCE;
    private int coins;                 // BUG: shared across ALL machines using this singleton!
    public void insertCoin(Machine m) { coins++; } // corrupts every other machine's count
}
```
</details>

<details>
<summary>💻 Click to expand — the fix</summary>

```java
enum HasCoinState implements State {
    INSTANCE;
    // NO fields. All mutable data lives on the Context, passed in.
    public void insertCoin(Machine m) { m.addCoin(); } // per-context state stays on the context
}
```
</details>

### Pitfall 2: Transition happens but side effects / entry-exit actions are duplicated or forgotten

**What goes wrong:** Entry/exit actions (log, notify, start timer) get copy-pasted into every transition into a state, and one path forgets it — so entering `Shipped` sometimes notifies and sometimes doesn't.

<details>
<summary>💻 Click to expand — the fix (centralize entry/exit in the transition)</summary>

```java
// Route ALL transitions through one method so entry/exit actions run exactly once, everywhere.
void transitionTo(State next) {
    state.onExit(this);      // exit action of old state
    this.state = next;
    state.onEntry(this);     // entry action of new state — impossible to forget
}
```
</details>

### Pitfall 3: Illegal transitions silently ignored (no-op) instead of surfaced

**What goes wrong:** Unsupported actions in a state do nothing (empty method), so bugs where the caller expected a transition go unnoticed — the order silently stays `Delivered` when someone calls `cancel()`, and no one knows why the refund never happened.

<details>
<summary>💻 Click to expand — the fix</summary>

```java
// Default to throwing (or returning a Result) so illegal transitions are LOUD.
interface OrderState {
    default void cancel(Order o) {
        throw new IllegalStateException("cancel not allowed in " + name());
    }
    String name();
}
// Callers now get a clear failure instead of a silent no-op.
```
</details>

### Pitfall 4: Circular references / memory leaks from state ↔ context back-pointers

**What goes wrong:** Each state holds the context and the context holds the state; if states are per-instance and long-lived, and you also cache states elsewhere, you can create retention cycles or leak contexts. (Less an issue with GC, but real in listener/observer-heavy state machines that register callbacks on entry and forget to deregister on exit.)

<details>
<summary>💻 Click to expand — the fix</summary>

```java
// Symmetric entry/exit: whatever you register on entry, deregister on exit.
void onEntry(Context c) { c.eventBus().register(this); }
void onExit(Context c)  { c.eventBus().unregister(this); } // prevents listener leak
```
</details>

</details>

## 🎓 Interview Tips

**What interviewers commonly ask:**

- "Design a vending machine / traffic light / TCP connection / elevator." (The tell that they want State — behavior varies by mode with legal transitions.)
- "What's the difference between State and Strategy?" (Intent, not structure — the #1 State question.)
- "How do you handle illegal transitions?" (Throw / return a Result / no-op — and *why* loud beats silent.)
- "Who should own the transition logic — the states or the context?" (Trade-off discussion: decentralized flexibility vs. centralized auditability.)
- "How would you make this thread-safe / scale to millions of instances?" (Flyweight stateless states + volatile/locking on the context.)

**What you should proactively mention even if not asked:**

- **State ≡ Strategy structurally; the difference is intent** — say this early; it signals you understand both.
- Prefer **stateless flyweight states** (singletons / enums) so a single instance serves all contexts — mention memory and thread-safety implications.
- **Java `enum` with abstract methods** is the idiomatic finite state machine (*Effective Java* Item 34) because it gives **compiler-enforced completeness** a `switch` cannot.
- Distinguish **who owns transitions** and when a **data-driven transition table** beats class-per-state (runtime-configurable, auditable, visualizable).
- Mention **entry/exit actions** and routing all transitions through one method so side effects run exactly once.
- For distributed workflows, note that state must be **persisted** (DB column / event-sourced) and transitions made **idempotent** and **atomic** (optimistic locking) — the in-memory pattern is only half the story.

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Strategy** — structurally identical (delegate to a swappable object); differs only in intent (algorithm vs. lifecycle mode). Most-confused sibling.
- **Flyweight** — used to share stateless state objects as singletons across many contexts.
- **Singleton** — concrete states are frequently singletons/enums (one instance per state).
- **Command** — transitions can be modeled as commands; state machines often *fire events* that are commands.
- **Observer** — states often notify observers on entry/exit (transition side effects).
- **Memento** — capture/restore a context's state (including its current State) for undo/checkpointing.
- **Chain of Responsibility** — an alternative when "which handler" depends on request attributes rather than a persistent mode.

</details>

## 📚 Library/Framework Implementation

**1. Java `Thread` lifecycle (`Thread.State`).** The JDK models a thread's lifecycle as an explicit enum state machine — `NEW`, `RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`, `TERMINATED` — queryable via `Thread.getState()`. Transitions are driven by the scheduler and synchronization events; it's a textbook enum-based FSM inside the platform.

<details>
<summary>💻 Click to expand — Thread.State FSM</summary>

```java
Thread t = new Thread(() -> {
    synchronized (LibraryDemo.class) { /* work */ }
});
System.out.println(t.getState()); // NEW
t.start();
System.out.println(t.getState()); // RUNNABLE (or already running)
// While blocked on the monitor it becomes BLOCKED; on wait() -> WAITING; after run -> TERMINATED
// Thread.State is a public enum — the JDK's own State pattern for lifecycle.
```
</details>

**2. Spring Statemachine.** A dedicated framework that implements the State pattern as a configurable, data-driven engine — you declare states, events, transitions, guards, and actions (often UML-style), and it manages the current state, persistence, hierarchical/parallel regions, and transition side effects. It's State-pattern-as-infrastructure for complex workflows.

<details>
<summary>💻 Click to expand — Spring Statemachine config sketch</summary>

```java
@Configuration
@EnableStateMachine
class OrderSMConfig extends StateMachineConfigurerAdapter<St, OrderEvent> {
    @Override public void configure(StateMachineStateConfigurer<St, OrderEvent> s) throws Exception {
        s.withStates().initial(St.NEW).states(EnumSet.allOf(St.class)).end(St.DELIVERED);
    }
    @Override public void configure(StateMachineTransitionConfigurer<St, OrderEvent> t) throws Exception {
        t.withExternal().source(St.NEW).target(St.PAID).event(OrderEvent.PAY)
         .and().withExternal().source(St.PAID).target(St.SHIPPED).event(OrderEvent.SHIP);
        // guards, actions, and persistence configured declaratively.
    }
}
```
</details>

**3. Apache Commons SCXML / state-machine engines (and Akka FSM in Scala/Java).** Harmonized with the W3C SCXML standard, Commons SCXML executes state-chart documents (states + transitions + conditions) at runtime — the transition graph is *data* (XML), the ultimate data-driven State machine. Similarly, Akka's FSM DSL models actors as `when(State) { ... }` blocks with `goto(next)` transitions.

<details>
<summary>💻 Click to expand — SCXML idea</summary>

```xml
<!-- The state machine as data: no recompilation to change the graph. -->
<scxml initial="draft" xmlns="http://www.w3.org/2005/07/scxml">
  <state id="draft">   <transition event="submit"  target="inReview"/></state>
  <state id="inReview"><transition event="approve" target="approved"/>
                       <transition event="reject"  target="draft"/></state>
  <state id="approved"><transition event="publish" target="published"/></state>
  <final id="published"/>
</scxml>
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1: [Conceptual] What is the State pattern and what problem does it solve?</strong></summary>

The State pattern is a **behavioral** GoF pattern that lets an object **alter its behavior when its internal state changes**, so the object *appears to change its class*. It solves the problem of **sprawling, duplicated conditional logic**: when behavior depends on state, the naive design puts a `switch (state)` in every method, and the same cases are repeated everywhere, transitions are implicit, and adding a state means editing every method.

State fixes this by representing **each state as its own class** implementing a shared `State` interface. The main object (the *Context*) holds a reference to the current state and **delegates** state-dependent calls to it. Behavior changes because a different object is now handling the call; transitioning is just swapping the current state object.

This localizes each state's behavior and legal transitions into one class (Single Responsibility), makes adding a state additive (Open/Closed), and turns an implicit state machine into an explicit, type-safe one.
</details>

<details>
<summary><strong>Q2: [Conceptual] Name the participants of the State pattern and their roles.</strong></summary>

Three participants:

1. **Context** — the object whose behavior varies (e.g., `VendingMachine`, `Order`). It holds a reference to the *current* `State`, delegates state-dependent requests to it, and exposes a transition method (e.g., `setState`/`transitionTo`), usually package-private.
2. **State** — an interface (or abstract class) declaring the operations that vary by state (one method per state-dependent action).
3. **Concrete States** — each implements `State` for one specific state: it provides that state's behavior and typically decides the *next* state (by calling `context.setState(...)`), so transition logic is co-located with behavior.

Optionally, an **abstract base state** provides a default "illegal transition" implementation (throw/log) so concrete states override only the actions they legally support.
</details>

<details>
<summary><strong>Q3: [Conceptual] How does State differ from a simple enum field with a switch statement?</strong></summary>

A `switch`-on-enum approach keeps *all* state-dependent behavior in the Context, re-branching in every method. The State pattern **distributes** behavior into per-state classes and uses **polymorphic dispatch** instead of conditionals.

Key differences: (1) **No duplication** — the switch is repeated per method; state objects aren't. (2) **Open/Closed** — adding a state adds a class vs. editing every switch. (3) **Completeness** — with a `switch` you can forget a case; with an abstract-method enum or interface you get compiler/uniform enforcement. (4) **Locality** — "what can happen from state X" is answered by one class, not scattered.

That said, for **2–3 states and one operation**, the enum/switch is simpler and the pattern is over-engineering. State earns its keep as states × operations grows.
</details>

<details>
<summary><strong>Q4: [Conceptual] What OO principles does the State pattern embody?</strong></summary>

Primarily:

- **Single Responsibility Principle** — each state's behavior and transitions live in one dedicated class.
- **Open/Closed Principle** — you extend the machine by adding a new state class, without modifying existing states or the context.
- **Polymorphism over conditionals** — virtual dispatch replaces `switch`/`if` chains.

It also supports the **Liskov Substitution Principle** (all states honor the same `State` contract, so the context can hold any of them interchangeably) and often uses **Flyweight** (stateless shared state singletons) and **Singleton** (one instance per state). The net effect is that the state machine becomes explicit and type-driven rather than implicit and data-driven.
</details>

<details>
<summary><strong>Q5: [Trade-off] Who should own transition logic — the Context, the States, or a transition table? Discuss the trade-offs.</strong></summary>

There are three common designs, and the choice is a real engineering trade-off:

- **States own transitions (decentralized):** each state calls `context.setState(nextState)`. Pros: behavior and its successor are co-located; very readable per state. Cons: states are coupled to their successors (they name concrete next-state types); the *global* graph is spread across classes.
- **Context owns transitions (centralized in code):** the context inspects results and decides. Pros: states stay decoupled. Cons: the context re-accumulates conditional logic — partly defeating the pattern.
- **Transition table (data-driven):** a `Map<State, Map<Event, State>>` (or SCXML/Spring Statemachine). Pros: the whole graph is in one auditable, visualizable, even runtime-configurable place; easy to validate reachability/dead states. Cons: less type-safe; behavior/actions may live separately from the graph.

Rule of thumb: **decentralized** for small/medium machines valuing readability; **table-driven** for large, compliance-heavy, or configurable workflows.
</details>

<details>
<summary><strong>Q6: [Implementation] Code a State-based traffic light that cycles Red → Green → Yellow → Red.</strong></summary>

A minimal, clean FSM — a common warm-up.

<details>
<summary>💻 Click to expand solution</summary>

```java
interface LightState { void next(TrafficLight l); String color(); }

class Red implements LightState {
    public void next(TrafficLight l) { l.setState(new Green()); }
    public String color() { return "RED"; }
}
class Green implements LightState {
    public void next(TrafficLight l) { l.setState(new Yellow()); }
    public String color() { return "GREEN"; }
}
class Yellow implements LightState {
    public void next(TrafficLight l) { l.setState(new Red()); }
    public String color() { return "YELLOW"; }
}

class TrafficLight {
    private LightState state = new Red();
    void setState(LightState s) { this.state = s; }
    void change() { state.next(this); }
    String color() { return state.color(); }

    public static void main(String[] args) {
        TrafficLight t = new TrafficLight();
        for (int i = 0; i < 4; i++) {
            System.out.println(t.color()); // RED, GREEN, YELLOW, RED
            t.change();
        }
    }
}
```
</details>

The transitions form a cycle, each state knows only its successor, and `TrafficLight` never branches on color. To avoid per-transition allocation, make the states enum singletons (Variant 3).
</details>

<details>
<summary><strong>Q6b: [Implementation] Rewrite that traffic light as an enum-based state machine and explain the advantage.</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
enum Light {
    RED    { Light next() { return GREEN;  } },
    GREEN  { Light next() { return YELLOW; } },
    YELLOW { Light next() { return RED;    } };
    abstract Light next();     // abstract => every constant MUST define it
}

class TrafficLight {
    private Light state = Light.RED;
    void change() { state = state.next(); }
    Light color() { return state; }
}
```
</details>

Advantages over the class-per-state version: states are **singletons** (zero allocation per transition), the `abstract next()` gives **compiler-enforced completeness** (add a state and it won't compile until you define its transition), and you get free `name()`/`valueOf()`/`EnumSet`/serialization. This is the idiomatic Java FSM for a fixed state set (*Effective Java* Item 34).
</details>

<details>
<summary><strong>Q7: [Implementation] How do you handle actions that are illegal in the current state?</strong></summary>

Provide a **default implementation** in an abstract base state (or interface `default` method) that represents "this action isn't allowed here," and let concrete states override only the actions they support. The default should be **loud** — throw an `IllegalStateException` (or return a `Result`/`Optional`) rather than silently no-op, so bugs surface.

<details>
<summary>💻 Click to expand</summary>

```java
interface State {
    String name();
    default void insertCoin(Machine m) { illegal("insertCoin"); }
    default void dispense(Machine m)   { illegal("dispense"); }
    default void illegal(String a) {
        throw new IllegalStateException(a + " not allowed in " + name());
    }
}
// Concrete state overrides ONLY what's legal:
enum HasCoin implements State {
    INSTANCE;
    public String name() { return "HAS_COIN"; }
    public void dispense(Machine m) { /* legal here */ }
    // insertCoin left as default -> throws
}
```
</details>

Choosing throw vs. no-op vs. Result is a design call: throw for programmer errors, Result for expected user input rejections. The key is *uniformity* — one place defines "illegal."
</details>

<details>
<summary><strong>Q8: [Implementation] Add entry and exit actions to a state machine. Where do they belong?</strong></summary>

Entry/exit actions (log, notify, start/stop timers, acquire/release resources) belong on the **state**, and *all* transitions must route through a single method so they run exactly once — never duplicated per call site.

<details>
<summary>💻 Click to expand</summary>

```java
interface State {
    default void onEntry(Context c) {}
    default void onExit(Context c)  {}
    String name();
}

class Context {
    private State state;
    void transitionTo(State next) {
        state.onExit(this);     // exit action of current state
        this.state = next;
        state.onEntry(this);    // entry action of new state
    }
}
```
</details>

This mirrors UML statecharts (entry/exit/do activities). The pitfall it prevents: copy-pasting "send notification" into every transition *into* `Shipped` and forgetting one path. Centralizing guarantees symmetry (e.g., register on entry, deregister on exit — avoids listener leaks).
</details>

<details>
<summary><strong>Q9: [Breaking] Why is putting fields on a shared/singleton state object a bug?</strong></summary>

If states are **shared flyweights/singletons** (for scale) but you store context-specific mutable data as a *field* on the state, then every context using that singleton reads and writes the *same* field — so contexts corrupt each other, and under concurrency it's a data race.

<details>
<summary>💻 Click to expand — the bug and the rule</summary>

```java
enum Counting implements State {
    INSTANCE;
    int count;                     // BUG: one field shared by ALL contexts
    public void tick(Ctx c) { count++; } // machine A's tick increments machine B's count
}
// RULE: flyweight states must be STATELESS. Move mutable data to the Context:
enum Counting2 implements State {
    INSTANCE;
    public void tick(Ctx c) { c.incr(); } // per-context data on the context
}
```
</details>

The fix is the **Flyweight discipline**: the state's *intrinsic* data (behavior/transitions) is shared and immutable; the *extrinsic* data (counts, ids) is passed in via the Context. Only then is a single shared instance safe.
</details>

<details>
<summary><strong>Q10: [Breaking] What goes wrong if transitions aren't atomic in a concurrent context?</strong></summary>

If two threads read the current state, both see it as valid for an action, and both perform the transition, you get **double transitions** — e.g., an order approved twice, a payment captured twice, or a lost update where one transition overwrites another. The state field also needs a **visibility** guarantee across threads.

Fixes: (1) make the state field `volatile` (visibility) and, more importantly, (2) perform read-check-transition **atomically** — either `synchronized`/`ReentrantLock` around the transition, or a lock-free CAS via `AtomicReference<State>` with `compareAndSet`. In distributed systems, use **optimistic locking** (a version column) or a single-writer/event-sourced model so concurrent transitions on the persisted state are rejected.

<details>
<summary>💻 Click to expand — lock-free atomic transition</summary>

```java
AtomicReference<State> state = new AtomicReference<>(NEW);
boolean pay() {
    return state.compareAndSet(NEW, PAID); // only ONE thread wins the NEW->PAID move
}
```
</details>

The interview point: the classic State pattern is *single-threaded by default*; making it concurrent requires deliberate atomicity, and CAS elegantly guarantees exactly-once transitions.
</details>

<details>
<summary><strong>Q11: [Breaking] Your state machine has 8 states and someone reports an "impossible" transition happened. How do you find the bug?</strong></summary>

An "impossible" transition usually means the graph isn't actually enforced in one place. Debug approach: (1) confirm every action routes through a **single `transitionTo`** method and add an assertion there that the (from, event) → to move is in the allowed set; (2) check for **direct `setState` bypasses** — a state or the context mutating the state field outside the transition method; (3) look for **shared mutable state on flyweight states** (Pitfall 1) that makes one context's transition affect another; (4) verify **atomicity** — a race can produce an interleaving that no single path allows.

The systemic fix is to make the graph **data** (a transition table) and validate every move against it, so an illegal move throws at the exact point of violation with a clear `from --event--> to` message. Decentralized code-only transitions make "impossible" transitions hard to audit; a table makes them one lookup.
</details>

<details>
<summary><strong>Q12: [Trade-off] State vs. Strategy — they look identical. When do you call it which?</strong></summary>

They are **structurally identical** — both delegate to a swappable object implementing a common interface. The difference is **intent and who drives the swap**:

- **Strategy** encapsulates an **interchangeable algorithm** chosen by the **client**, externally, usually once. Strategies are independent and unaware of each other; there are no "transitions."
- **State** encapsulates a **mode in a lifecycle**; the swap happens **internally** as a side effect of operations, and states typically **know their successors** and drive transitions. The object "appears to change its class" over time.

Tell them apart by asking: *do the swappable objects replace themselves as a consequence of being used?* If yes → State. *Does the client pick one and it stays put?* → Strategy. Example: a `Comparator` is a Strategy; a `TCPConnection`'s `Established`/`Closed` are States.
</details>

<details>
<summary><strong>Q13: [Trade-off] When is a data-driven transition table better than class-per-state, and vice versa?</strong></summary>

**Table (data-driven)** wins when: the graph is large; it must be **auditable/visualizable** (compliance workflows); transitions may be **configured at runtime** or by non-developers; you want to statically **validate** reachability and detect dead/unreachable states; or many machines share one graph shape. Downside: behavior/actions are decoupled from the graph and it's less type-safe.

**Class-per-state (or enum)** wins when: each state has **rich, distinct behavior** (not just "go to next"); you want **type safety** and IDE navigation; the state set is fixed and modest; and behavior + transitions reading together per state aids comprehension. Downside: the global graph is spread across files.

Many production systems combine them: a table for the *graph* plus per-state classes/handlers for the *behavior/actions*, wired together — best of both.
</details>

<details>
<summary><strong>Q14: [Trade-off] Does the Context or the State hold the mutable data, and why does it matter?</strong></summary>

Put mutable data on the **Context**, not the states — especially if states are shared singletons. The Context is the single, per-instance owner of extrinsic data (counts, ids, buffers, timers), and it passes itself (or the needed data) into state methods. States should ideally be **stateless behavior + transition holders**.

Why it matters: (1) **shareability** — stateless states can be flyweights/enum singletons, so millions of contexts share a handful of state objects; (2) **thread safety** — immutable/stateless states are inherently safe to share; (3) **serialization/persistence** — persisting the Context (plus a state *identifier*) is clean, whereas serializing stateful state objects that also reference the context creates cycles. If a state genuinely needs per-transition scratch data, keep it local to the method, not a field.
</details>

<details>
<summary><strong>Q15: [Advanced/Serialization] How do you persist and restore a State machine (e.g., an order that lives for weeks)?</strong></summary>

Persist the **state identity**, not the state object. Store a stable discriminator — an enum name or a string/int code — in a DB column (or as events in an event-sourced log), plus the context's data. On load, map the discriminator back to the state singleton.

<details>
<summary>💻 Click to expand</summary>

```java
// Persist:
String col = order.state().name();      // "PAID"  (enum name is stable & serialization-friendly)
// Restore:
OrderState s = OrderStateEnum.valueOf(col); // back to the singleton
```
</details>

Key concerns: (1) use **enum name, not ordinal** (ordinal shifts if you reorder constants — a classic bug); (2) make transitions **idempotent** and guarded by **optimistic locking** (a `version` column) so a retried or concurrent transition can't double-apply; (3) for auditability and replay, prefer **event sourcing** — store the sequence of events and derive the state, which also gives you a natural history and time-travel debugging. Long-lived machines must treat the persisted state as the source of truth, re-reading before each transition.
</details>

<details>
<summary><strong>Q16: [Advanced/Concurrency] Design a thread-safe state machine that guarantees exactly-once transitions without locks.</strong></summary>

Use an `AtomicReference<State>` and `compareAndSet` so only one thread can win a given transition; retries re-read the current state and re-evaluate legality.

<details>
<summary>💻 Click to expand solution</summary>

```java
import java.util.concurrent.atomic.AtomicReference;

class LockFreeMachine {
    enum S { NEW, PAID, SHIPPED, DELIVERED, CANCELLED }
    private final AtomicReference<S> state = new AtomicReference<>(S.NEW);

    /** Returns true iff THIS call performed the transition. */
    boolean transition(S from, S to) {
        return state.compareAndSet(from, to);
    }
    boolean pay()  { return transition(S.NEW, S.PAID); }
    boolean ship() { return transition(S.PAID, S.SHIPPED); }

    // For a computed successor, loop until we win or the move becomes illegal:
    boolean advance(java.util.function.Function<S,S> nextFn) {
        for (;;) {
            S cur = state.get();
            S next = nextFn.apply(cur);
            if (next == null) return false;          // illegal from cur
            if (state.compareAndSet(cur, next)) return true; // won
            // else: someone else moved; retry with fresh state
        }
    }
    S state() { return state.get(); }
}
```
</details>

Properties: `compareAndSet` is a single atomic CPU instruction (CAS), so of N concurrent `pay()` calls exactly one returns `true`. No lock, no blocking; the CAS loop handles contention by retrying against the fresh state. This is how high-throughput state machines (and lock-free data structures) guarantee exactly-once transitions.
</details>

<details>
<summary><strong>Q17: [Advanced] How do hierarchical / nested states (statecharts) extend the basic State pattern, and why do they matter?</strong></summary>

Basic State is a **flat** FSM. Harel **statecharts** add **hierarchy** (a superstate containing substates), **orthogonal regions** (parallel independent sub-machines), and **history** (return to the last active substate). They matter because flat FSMs suffer **state explosion**: if a `PhoneCall` can be `Muted`/`Unmuted` in *every* of its `Ringing`/`Connected`/`OnHold` states, a flat model multiplies states (3×2), while a statechart factors "muted" into an orthogonal region.

Hierarchy also lets a superstate define **shared behavior/transitions** inherited by all substates (e.g., "any state → Disconnected on hangup" defined once on the parent). In code, you model this with an abstract superstate whose method a substate calls via `super`, or delegate unhandled events up to a parent state. Frameworks like Spring Statemachine and SCXML support hierarchical/parallel states natively — reach for them when a hand-rolled flat machine starts multiplying states combinatorially.
</details>

<details>
<summary><strong>Q18: [Advanced] In an event-driven/distributed system, how does the State pattern relate to sagas and workflow orchestration?</strong></summary>

A **saga** (distributed transaction via a sequence of local steps with compensations) is a state machine whose states are the saga's steps and whose transitions are triggered by **events/messages** — with **compensating transitions** on failure (e.g., `PaymentFailed` → run refund → move to `Cancelled`). Orchestration engines (Temporal, AWS Step Functions, Camunda, Netflix Conductor) are essentially **durable, distributed State machines**: they persist the current state, drive transitions from events, and survive process restarts.

The State pattern gives you the in-memory model; distributed reality adds: **durability** (persist every transition), **idempotency** (events may be redelivered — a transition must be safe to reprocess), **at-least-once delivery** handling, **timeouts** as first-class transitions, and **compensation** paths. The mental model transfers directly, but "delegating to the next state" becomes "emit an event and durably record the new state," and illegal transitions become rejected/ignored duplicate events rather than exceptions.
</details>

<details>
<summary><strong>Q19: [Coding Challenge] Implement a media player state machine (Stopped/Playing/Paused) with play/pause/stop, using stateless enum states and a Context holding playback position.</strong></summary>

A complete, FAANG-realistic solution showing stateless states, extrinsic data on the context, and illegal-transition handling.

<details>
<summary>💻 Click to expand full solution</summary>

```java
// Context: owns ALL mutable data (position, track). States are stateless singletons.
class MediaPlayer {
    private PlayerState state = PlayerState.STOPPED;
    private int positionSec = 0;
    private String track = "none";

    void load(String t) { this.track = t; this.positionSec = 0; }

    void play()  { state = state.play(this); }
    void pause() { state = state.pause(this); }
    void stop()  { state = state.stop(this); }

    // package-private data ops used by states (extrinsic state stays here)
    void resetPosition() { positionSec = 0; }
    void advance(int s)  { positionSec += s; }
    int position()       { return positionSec; }
    String track()       { return track; }
    PlayerState state()  { return state; }
}

enum PlayerState {
    STOPPED {
        PlayerState play(MediaPlayer p) {
            System.out.println("Playing '" + p.track() + "' from start");
            p.resetPosition();
            return PLAYING;
        }
        // pause/stop illegal -> default no-op-with-message
    },
    PLAYING {
        PlayerState pause(MediaPlayer p) {
            System.out.println("Paused at " + p.position() + "s");
            return PAUSED;
        }
        PlayerState stop(MediaPlayer p) {
            System.out.println("Stopped");
            p.resetPosition();
            return STOPPED;
        }
        PlayerState play(MediaPlayer p) { System.out.println("Already playing"); return this; }
    },
    PAUSED {
        PlayerState play(MediaPlayer p) {
            System.out.println("Resuming at " + p.position() + "s");
            return PLAYING;
        }
        PlayerState stop(MediaPlayer p) {
            System.out.println("Stopped");
            p.resetPosition();
            return STOPPED;
        }
    };

    // Defaults = illegal action in this state (loud-ish, but non-fatal for a UI).
    PlayerState play(MediaPlayer p)  { System.out.println("Can't play in "  + name()); return this; }
    PlayerState pause(MediaPlayer p) { System.out.println("Can't pause in " + name()); return this; }
    PlayerState stop(MediaPlayer p)  { System.out.println("Can't stop in "  + name()); return this; }

    public static void main(String[] args) {
        MediaPlayer p = new MediaPlayer();
        p.load("song.mp3");
        p.play();   // Playing from start   (STOPPED -> PLAYING)
        p.advance(30);
        p.pause();  // Paused at 30s         (PLAYING -> PAUSED)
        p.play();   // Resuming at 30s       (PAUSED -> PLAYING)
        p.stop();   // Stopped               (PLAYING -> STOPPED)
        p.pause();  // Can't pause in STOPPED (illegal, handled)
        System.out.println("Final: " + p.state()); // STOPPED
    }
}
```
</details>

Highlights the interviewer looks for: **stateless enum states** (position lives on the Context), **compiler-forced** handling of all three actions per state, and **graceful illegal-action handling** appropriate to a UI (message + stay) rather than throwing.
</details>

<details>
<summary><strong>Q20: [Coding Challenge] Build a generic, reusable state-machine engine (transition table + guards + actions) that any domain can configure.</strong></summary>

This is the "design a framework" version — shows you can generalize State into infrastructure.

<details>
<summary>💻 Click to expand full solution</summary>

```java
import java.util.*;
import java.util.function.*;

// Generic FSM: S = state type, E = event type, C = context type.
class StateMachine<S, E, C> {
    private static final class Transition<S, C> {
        final S target;
        final Predicate<C> guard;      // optional condition
        final Consumer<C> action;      // optional side effect on transition
        Transition(S t, Predicate<C> g, Consumer<C> a) { target = t; guard = g; action = a; }
    }

    private final Map<S, Map<E, Transition<S, C>>> table = new HashMap<>();
    private S current;
    private final C context;

    StateMachine(S initial, C context) { this.current = initial; this.context = context; }

    // Fluent registration
    StateMachine<S, E, C> on(S from, E event, S to, Predicate<C> guard, Consumer<C> action) {
        table.computeIfAbsent(from, k -> new HashMap<>())
             .put(event, new Transition<>(to, guard == null ? c -> true : guard,
                                          action == null ? c -> {} : action));
        return this;
    }

    /** Returns true iff the event caused a transition. */
    boolean fire(E event) {
        Transition<S, C> t = table.getOrDefault(current, Map.of()).get(event);
        if (t == null) return false;                 // no transition for this event
        if (!t.guard.test(context)) return false;    // guard blocked it
        t.action.accept(context);                    // run side effect
        System.out.println(current + " --" + event + "--> " + t.target);
        current = t.target;
        return true;
    }
    S state() { return current; }

    // ---- Demo: an ATM machine configured declaratively ----
    enum St { IDLE, CARD_IN, AUTH, DISPENSING }
    enum Ev { INSERT, PIN_OK, PIN_BAD, WITHDRAW, EJECT }
    static class Atm { int wrongPins = 0; }

    public static void main(String[] args) {
        Atm atm = new Atm();
        StateMachine<St, Ev, Atm> m = new StateMachine<>(St.IDLE, atm);
        m.on(St.IDLE, Ev.INSERT, St.CARD_IN, null, c -> System.out.println("Read card"))
         .on(St.CARD_IN, Ev.PIN_OK, St.AUTH, null, null)
         .on(St.CARD_IN, Ev.PIN_BAD, St.IDLE, c -> ++c.wrongPins >= 3, c -> System.out.println("Card retained!"))
         .on(St.CARD_IN, Ev.PIN_BAD, St.CARD_IN, c -> c.wrongPins < 3, c -> System.out.println("Try again"))
         .on(St.AUTH, Ev.WITHDRAW, St.DISPENSING, null, c -> System.out.println("Dispensing cash"))
         .on(St.DISPENSING, Ev.EJECT, St.IDLE, null, c -> System.out.println("Card ejected"));

        m.fire(Ev.INSERT);    // IDLE -> CARD_IN
        m.fire(Ev.PIN_BAD);   // guard wrongPins<3 -> stays CARD_IN ("Try again")
        m.fire(Ev.PIN_OK);    // CARD_IN -> AUTH
        m.fire(Ev.WITHDRAW);  // AUTH -> DISPENSING
        m.fire(Ev.EJECT);     // DISPENSING -> IDLE
        System.out.println("End state: " + m.state());
    }
}
```
</details>

This demonstrates the **data-driven** State design: transitions, **guards**, and **actions** are registered as data; the engine is domain-agnostic and reusable; illegal events return `false` instead of throwing (caller decides). It's essentially a miniature Spring Statemachine and shows you can lift the pattern into a framework — a strong senior/staff signal.
</details>

### 🧠 Staff / Principal Engineer Level

<details>
<summary><strong>SP1: [Staff] You're designing the order-lifecycle state machine for a payments platform used by 50 services. How do you make it correct, evolvable, and safe?</strong></summary>

I'd treat the state machine as a **shared, versioned contract**, not just code. Correctness: model the graph **explicitly as data** (a transition table) so it's auditable, statically checkable (reachability, dead states, no illegal edges), and renderable as a diagram for stakeholders; every transition goes through one guarded, logged `transitionTo` with **optimistic locking** so concurrent/duplicate events can't double-apply. Evolvability: version the state definition and require that new states/transitions are **additive** (never repurpose an existing state's meaning), because 50 services have baked-in assumptions; deprecate via new terminal states rather than deleting. Safety: make transitions **idempotent** and driven by **events** with at-least-once semantics; persist via **event sourcing** so we get a full audit trail, replay, and time-travel debugging for disputes. Operationally: emit metrics per transition (funnel/conversion, stuck-in-state alerts), and add **timeouts as first-class transitions** (e.g., `Paid` with no `Ship` in 48h → alert). The pattern-level point: for a shared critical machine, the *graph is the API* — invest in making it declarative, validated, observable, and backward-compatible.
</details>

<details>
<summary><strong>SP2: [Staff] When would you deliberately NOT use the OO State pattern and reach for a workflow engine or table instead?</strong></summary>

I'd avoid hand-rolled class-per-state when the machine is **large, cross-cutting, long-lived, or business-configurable**. Concretely: (1) if non-engineers need to change the flow, a **workflow engine** (Temporal, Step Functions, Camunda) or config-driven table beats recompiling classes; (2) if transitions must be **durable across process restarts and span services** (sagas), I want an orchestrator that persists state and handles retries/timeouts/compensation — the in-memory pattern doesn't give durability; (3) if I need **audit, replay, and visualization** for compliance, a declarative graph (SCXML/BPMN) is first-class; (4) if the state count is exploding combinatorially, I want **hierarchical/orthogonal statecharts**, not a flat class-per-state. Conversely I *keep* the OO pattern for **rich per-state behavior** local to one service with a modest, stable state set. The staff judgment is recognizing that "State pattern" and "state machine engine" sit on a spectrum from code to data, and choosing the point that matches the change-frequency, durability, and audience of the machine.
</details>

<details>
<summary><strong>SP3: [Principal] How do you evolve a live state machine that has millions of in-flight instances persisted in the old shape?</strong></summary>

This is a **schema/graph migration** problem. Principles: (1) **never rename or reuse** an existing state's semantics — old persisted rows carry the old meaning; add new states instead. (2) Introduce changes as **backward-compatible supersets**: the new graph must accept every state an in-flight instance could currently be in and define valid outgoing transitions for them. (3) For genuinely new required steps, add a **migration transition** and, if needed, a one-time **backfill** that moves eligible instances, done idempotently and in batches with monitoring. (4) Prefer **persisting state by stable name/code, never enum ordinal**, so reordering constants doesn't corrupt meaning. (5) If event-sourced, you can **replay** with new logic, but must version events and keep old event handlers. (6) Roll out behind a **flag/dual-write**, validate on a shadow population, and keep the old and new machines interpretable simultaneously during the transition window. The core risk is that a deploy silently reinterprets in-flight state; the mitigation is additive evolution plus explicit migration paths, verified against real in-flight distributions before cutover.
</details>

<details>
<summary><strong>SP4: [Principal] Compare hand-rolled State classes, enum FSMs, transition tables, and workflow engines as an architectural decision. What's your decision framework?</strong></summary>

I decide along four axes: **complexity of per-state behavior**, **change frequency/audience of the graph**, **durability/distribution needs**, and **scale/observability requirements**. Hand-rolled **State classes** fit rich, distinct per-state behavior with a modest fixed state set inside one service — great type safety and locality, poor global-graph visibility. **Enum FSMs** are the sweet spot for fixed, moderate state sets needing compiler-enforced completeness and zero allocation, but can't do hierarchy or runtime config. **Transition tables** win when the graph must be auditable, validated, or configured at runtime, at the cost of type safety and behavior-locality. **Workflow engines** are mandatory when transitions must be durable, distributed, retryable, timeout-driven, and survive restarts (sagas) — but add operational weight and latency. My framework: start with the simplest (enum) and move right only when a forcing function appears — combinatorial states → statecharts; non-engineer configurability → tables/BPMN; cross-service durability → orchestrator. Document the choice and its trigger, because teams often over-adopt engines (operational cost) or under-adopt them (reinventing durability badly). The anti-patterns at the extremes are equally expensive.
</details>

<details>
<summary><strong>SP5: [Principal] How do you make a distributed state machine's transitions exactly-once and consistent given at-least-once message delivery and partial failures?</strong></summary>

You can't get exactly-once *delivery*, so you engineer exactly-once *effect* through **idempotency + atomic state commit**. Concretely: (1) each event carries a stable **idempotency key**; the transition first checks whether that key was already applied (dedup table) and no-ops on replays. (2) Commit the **new state and the side-effect record in one atomic unit** — either a local DB transaction (state row + outbox row) using the **transactional outbox** pattern, or event-sourcing where appending the event *is* the commit. (3) Publish downstream effects from the **outbox** via a relay, so the effect is emitted at-least-once but consumers dedup by key — end-to-end exactly-once effect. (4) Guard the state write with **optimistic concurrency** (version/expected-current-state CAS) so two concurrent handlers can't both transition; the loser retries against fresh state or drops as a duplicate. (5) Model **timeouts and compensations** as explicit transitions so partial failures have defined recovery paths. (6) Ensure transitions are **commutative/idempotent where possible** so reordering under retries still converges. The mental model: treat every transition as `if (currentState == expected && !alreadyApplied(key)) atomically { apply; record key; enqueue effects }`. This is exactly what Temporal/Kafka-based sagas implement; the State pattern supplies the graph, and these mechanisms supply the distributed-systems guarantees.
</details>

---

## ⚡ Quick Revision

**One-liner:** State lets an object change its behavior when its internal state changes by delegating to a per-state object — it turns a sprawling `switch (state)` into a set of polymorphic state classes, so the object *appears to change its class*.

**The whole pattern in one paragraph:** The State pattern is a behavioral GoF pattern — *"allow an object to alter its behavior when its internal state changes; the object appears to change its class."* It kills **duplicated conditional dispatch**: instead of `switch (state)` repeated in every method, you extract **each state into a class** implementing a shared `State` interface, and the **Context** delegates state-dependent calls to its *current* state object; transitioning is just swapping that object (`context.setState(next)`). Three participants: **Context** (holds current state, delegates, exposes a transition hook), **State** interface (one method per state-dependent action), and **Concrete States** (behavior + the legal transitions out of that state). An abstract base / interface `default` supplies a uniform **illegal-transition** handler (throw loudly, not silent no-op) so states override only legal actions. Key design axis: **who owns transitions** — decentralized (states name their successors: readable but coupled), centralized in the context, or a **data-driven transition table** (`Map<State,Map<Event,State>>`: auditable, validatable, runtime-configurable). It's **structurally identical to Strategy** — the difference is intent: Strategy is a client-chosen *algorithm* that stays put; State is a *lifecycle mode* whose objects replace themselves as a side effect. For scale, make states **stateless flyweight singletons** (enum constants are ideal — *Effective Java* Item 34 — giving compiler-enforced completeness, zero per-transition allocation, and free serialization) with all mutable data on the Context (extrinsic state); putting fields on a shared state is the #1 bug. Concurrency needs deliberate atomicity — `volatile` for visibility plus `synchronized`/`AtomicReference.compareAndSet` for exactly-once transitions; distributed versions add persistence (state *name*, never ordinal), idempotency, optimistic locking, event sourcing, and become **sagas/workflow engines** (Temporal, Step Functions, Spring Statemachine, SCXML). Real JDK example: `Thread.State`. Use it when behavior varies across many states with complex/illegal-transition-sensitive lifecycles; avoid it for 2–3 trivial states where an `if`/enum is clearer.

**Top 5 interview answers to memorize:**

1. **"What is it?"** → Encapsulate each state as an object implementing a common interface; the context delegates to the current state and swaps it to transition, so behavior changes by polymorphism instead of `switch`.
2. **"State vs. Strategy?"** → Structurally identical; intent differs — Strategy is a client-selected algorithm that stays fixed; State is a lifecycle mode whose objects transition (replace) themselves internally.
3. **"How do you handle illegal transitions?"** → A default (abstract base / interface `default`) that throws `IllegalStateException` (or returns a Result) — loud, not a silent no-op — with concrete states overriding only legal actions.
4. **"How do you scale/make it thread-safe?"** → Stateless flyweight/enum-singleton states shared across contexts, all mutable data on the context; `volatile` + `AtomicReference.compareAndSet` (or a lock) for exactly-once atomic transitions.
5. **"Class-per-state vs. transition table?"** → Classes/enums for rich per-state behavior + type safety; a data-driven table when the graph must be auditable, validated, visualized, or runtime-configurable (or use a workflow engine when transitions must be durable/distributed).

**Trigger words (hear these → think State):** "behavior depends on state", "state machine / FSM", "lifecycle", "transitions", "vending machine / traffic light / TCP connection / elevator / ATM", "order/document workflow", "modes", "legal vs. illegal transitions", "changes behavior at runtime", "big switch on a status field", "Draft/Published/Approved", "Playing/Paused/Stopped", "appears to change its class".

---

*End of State Pattern study guide.*




# Strategy Pattern ⭐⭐⭐ (Difficulty: Medium)

> A behavioral design pattern that lets you define a family of algorithms, encapsulate each one, and make them interchangeable at runtime. One of the most commonly asked and most practically useful patterns in FAANG interviews.

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 1 — Naive if/else (Anti-pattern)](#variant-1--naive-ifelse-anti-pattern)
   - [Variant 2 — Strategy Interface + Concrete Classes](#variant-2--strategy-interface--concrete-classes)
   - [Variant 3 — Enum Strategies](#variant-3--enum-strategies)
   - [Variant 4 — Functional Strategies (Lambdas)](#variant-4--functional-strategies-lambdas)
   - [Variant 5 — DI + Strategy Registry (Production)](#variant-5--di--strategy-registry-production)
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

**GoF:** *"Define a family of algorithms, encapsulate each one, and make them interchangeable. Strategy lets the algorithm vary independently from clients that use it."*

The Strategy pattern extracts a set of related algorithms into separate classes (strategies) that share a common interface. A context object holds a reference to one strategy and delegates the work to it, rather than implementing the behavior itself. Because the context talks to strategies only through the interface, you can swap the algorithm at runtime, add new algorithms without touching the context, and test each algorithm in isolation.

It exists to solve a fundamental problem: **behavior that varies should not be hard-coded into the class that uses it.** When a class contains many conditional branches selecting between variants of an algorithm, the class becomes rigid, hard to test, and violates the Open/Closed Principle. Strategy replaces conditional logic with polymorphism and favors **composition over inheritance** — instead of subclassing to change behavior, you inject the behavior as an object.

---

## 🎯 Problem

You start with a class that does one job one way. Then requirements multiply: the same operation needs several interchangeable implementations chosen at runtime, per-request, per-customer, or per-config. If you cram all variants into one class using `if/else` or `switch`, the class swells, every new variant forces you to edit and re-test existing code, and the algorithms can't be reused or tested independently.

**Concrete scenarios where Strategy shines:**

- **Payment processing** — an e-commerce checkout must support credit card, PayPal, UPI, Apple Pay, and crypto. Each has completely different logic, but the checkout flow is identical: `pay(amount)`. Hard-coding a giant `switch(paymentType)` means every new provider modifies the checkout class.
- **Sorting / comparison** — the same collection needs to be sorted by price, by rating, by date, or by relevance depending on the user's choice. `java.util.Comparator` *is* the Strategy pattern.
- **Compression** — a file archiver supports ZIP, GZIP, LZ4, and Brotli. The archiving pipeline is the same; only the compression algorithm changes.
- **Pricing / discounts** — a cart applies "no discount," "seasonal 10%," "loyalty tier," or "BOGO" pricing. Business wants to add promos weekly without redeploying the cart engine.
- **Routing / load balancing** — a gateway picks round-robin, least-connections, or weighted routing per service.

The common thread: **one stable workflow, many interchangeable algorithms, selection deferred to runtime.**

---

## ✅ Solution

The core idea in plain language: **pull each algorithm out into its own object behind a shared interface, then hand the right object to the class that needs the behavior.** The class ("context") no longer knows *how* the work is done — it only knows it can call `execute()` on whatever strategy it was given.

Key structural elements:

- **Strategy interface** — declares the operation common to all algorithms (e.g. `PaymentStrategy.pay(amount)`). This is the contract the context depends on.
- **Concrete strategies** — each implements the interface with one specific algorithm (`CreditCardPayment`, `PayPalPayment`, ...). They are independent and interchangeable.
- **Context** — holds a reference to a strategy, exposes a method to set/inject it, and delegates the real work to it. The context is closed for modification but open for extension via new strategies.
- **Client** — chooses which concrete strategy to plug in (often via a factory, DI container, enum, or a registry keyed by an ID).

The two mechanisms that make Strategy work are **composition** (the context *has-a* strategy rather than *is-a* subclass) and **polymorphism / interface segregation** (the context depends only on the abstract interface, so new algorithms never force changes to the context — satisfying the Open/Closed Principle). Prefer **stateless strategies** so a single instance can be shared safely across threads.

---

## 💻 Implementation

We'll evolve a **payment processing** example from a naive conditional mess to a production-grade, DI-driven, registry-backed design — the exact progression a FAANG interviewer wants to see.

### Variant 1 — Naive if/else (Anti-pattern)

Everything lives in one class with a `switch` on a type string. **What's wrong:** it violates Open/Closed (every new method edits this class), it's untestable in isolation, the branches share accidental state, and it grows without bound. This is the "before" you should be able to critique on sight.

<details>
<summary>💻 Click to expand code</summary>

```java
// ❌ ANTI-PATTERN: one class knows every algorithm
public class PaymentProcessor {

    public void pay(String type, double amount) {
        if (type.equals("CREDIT_CARD")) {
            System.out.println("Validating card number, CVV, expiry...");
            System.out.println("Charging $" + amount + " to credit card");
        } else if (type.equals("PAYPAL")) {
            System.out.println("Redirecting to PayPal OAuth...");
            System.out.println("Charging $" + amount + " via PayPal");
        } else if (type.equals("UPI")) {
            System.out.println("Generating UPI collect request...");
            System.out.println("Charging $" + amount + " via UPI");
        } else {
            throw new IllegalArgumentException("Unknown payment type: " + type);
        }
        // Adding Apple Pay / crypto = editing (and re-testing) THIS method again.
    }
}
```
</details>

**Pros:** trivial for 2 cases; no indirection.
**Cons:** violates OCP; every new payment type modifies and risks breaking existing code; impossible to unit-test one algorithm alone; branches accumulate shared mutable state; `type` is a stringly-typed footgun.
**Mechanism (why it's bad):** behavior is coupled to the context by conditionals instead of polymorphism — there is no seam to extend.

### Variant 2 — Strategy Interface + Concrete Classes

Extract each algorithm into a class implementing a shared interface; the context holds a `PaymentStrategy` and delegates. **What it solves:** adding a payment type is now a *new class*, zero edits to `PaymentProcessor` (Open/Closed satisfied). Each strategy is independently unit-testable.

<details>
<summary>💻 Click to expand code</summary>

```java
// The Strategy interface — the stable contract the context depends on
public interface PaymentStrategy {
    void pay(double amount);
}

// Concrete strategies — each encapsulates ONE algorithm
public class CreditCardPayment implements PaymentStrategy {
    private final String cardNumber;
    public CreditCardPayment(String cardNumber) { this.cardNumber = cardNumber; }
    @Override public void pay(double amount) {
        System.out.println("Paid $" + amount + " with credit card " + mask(cardNumber));
    }
    private String mask(String c) { return "****" + c.substring(c.length() - 4); }
}

public class PayPalPayment implements PaymentStrategy {
    private final String email;
    public PayPalPayment(String email) { this.email = email; }
    @Override public void pay(double amount) {
        System.out.println("Paid $" + amount + " via PayPal (" + email + ")");
    }
}

// Context — depends ONLY on the interface, delegates the real work
public class PaymentProcessor {
    private PaymentStrategy strategy;                 // composition: HAS-A strategy
    public void setStrategy(PaymentStrategy s) { this.strategy = s; }
    public void checkout(double amount) {
        if (strategy == null) throw new IllegalStateException("No payment strategy set");
        strategy.pay(amount);                         // polymorphic delegation
    }
}
```
</details>

**Pros:** OCP-compliant; each algorithm testable/reusable in isolation; runtime swapping via `setStrategy`.
**Cons:** one class per algorithm (more files); client must know how to instantiate the right strategy; boilerplate for trivial one-line algorithms.
**Mechanism:** **composition + interface segregation** — the context depends on the abstraction, so concrete strategies vary independently of it.

### Variant 3 — Enum Strategies

When strategies are stateless and the set is fixed/known at compile time, an `enum` where each constant implements the interface is a compact, type-safe, singleton-per-strategy design. **What it solves:** eliminates scattered classes and unsafe strings; each constant is a JVM-guaranteed singleton (great for stateless strategies); `valueOf` gives free string→strategy lookup.

<details>
<summary>💻 Click to expand code</summary>

```java
public enum PaymentStrategy {
    CREDIT_CARD {
        @Override public void pay(double amount) {
            System.out.println("Paid $" + amount + " with credit card");
        }
    },
    PAYPAL {
        @Override public void pay(double amount) {
            System.out.println("Paid $" + amount + " via PayPal");
        }
    },
    UPI {
        @Override public void pay(double amount) {
            System.out.println("Paid $" + amount + " via UPI");
        }
    };

    public abstract void pay(double amount);   // each constant MUST implement
}

// Context — the SAME PaymentProcessor works with the enum strategy.
// It depends only on the PaymentStrategy type; here the "type" is the enum.
public class PaymentProcessor {
    private PaymentStrategy strategy;                  // holds an enum constant
    public void setStrategy(PaymentStrategy s) { this.strategy = s; }
    public void checkout(double amount) {
        if (strategy == null) throw new IllegalStateException("No payment strategy set");
        strategy.pay(amount);                          // delegates to the enum constant
    }
}

// Usage — type-safe, no strings, free lookup, and driven through the Processor:
class Demo {
    void run() {
        PaymentProcessor processor = new PaymentProcessor();

        // 1) Direct call on the constant (each constant IS a singleton strategy)
        PaymentStrategy.CREDIT_CARD.pay(100.0);

        // 2) Inject the enum constant into the context, then delegate
        processor.setStrategy(PaymentStrategy.CREDIT_CARD);
        processor.checkout(100.0);                     // Paid $100.0 with credit card

        processor.setStrategy(PaymentStrategy.PAYPAL);
        processor.checkout(50.0);                      // Paid $50.0 via PayPal

        // 3) Config/request-driven selection — free string->strategy via valueOf
        String fromConfig = "UPI";
        processor.setStrategy(PaymentStrategy.valueOf(fromConfig));
        processor.checkout(30.0);                      // Paid $30.0 via UPI

        // 4) Iterate every supported method (e.g., to render checkout options)
        for (PaymentStrategy method : PaymentStrategy.values()) {
            System.out.println("Supported: " + method.name());
        }
    }
}
```
</details>

**Pros:** type-safe; each constant is a thread-safe singleton (no allocation); compact; built-in `valueOf`/`values()`; ideal for stateless algorithms.
**Cons:** fixed at compile time (can't add strategies from a plugin/at runtime); constants can't easily carry per-instance configuration; harder to inject dependencies (no constructor injection from a DI container).
**Mechanism:** enum constants are effectively **eagerly-initialized singletons** guarded by the classloader — perfect for stateless, shareable strategies.

### Variant 4 — Functional Strategies (Lambdas)

If the strategy interface is a single-method (functional) interface, callers can pass a lambda or method reference instead of a class. **What it solves:** removes class-per-algorithm boilerplate for simple behaviors; strategies become first-class values you can store, compose, and pass around.

<details>
<summary>💻 Click to expand code</summary>

```java
@FunctionalInterface
public interface PaymentStrategy {
    void pay(double amount);
}

public class PaymentProcessor {
    private PaymentStrategy strategy;
    public void setStrategy(PaymentStrategy s) { this.strategy = s; }
    public void checkout(double amount) { strategy.pay(amount); }
}

// Usage — algorithms as lambdas / method references:
class Demo {
    void run() {
        PaymentProcessor p = new PaymentProcessor();

        p.setStrategy(amt -> System.out.println("Paid $" + amt + " by card"));
        p.checkout(100);

        p.setStrategy(amt -> System.out.println("Paid $" + amt + " via PayPal"));
        p.checkout(50);

        // A Map<String, Strategy> of lambdas is a lightweight registry:
        Map<String, PaymentStrategy> strategies = Map.of(
            "CARD",   amt -> System.out.println("card "   + amt),
            "PAYPAL", amt -> System.out.println("paypal " + amt)
        );
        strategies.get("CARD").checkout(25);  // (illustrative)
    }
}
```

**Real-world example: this is exactly how `Comparator` works —**
```java
List<Product> products = new ArrayList<>();
products.sort(Comparator.comparingDouble(Product::getPrice));      // strategy 1
products.sort(Comparator.comparing(Product::getName));             // strategy 2
products.sort(Comparator.comparingDouble(Product::getPrice)
                        .reversed()
                        .thenComparing(Product::getName));         // composed strategies
```
</details>

**Pros:** minimal boilerplate; strategies are values (store in maps, compose with `andThen`/`thenComparing`); great for stateless one-liners.
**Cons:** lambdas capturing mutable state can hurt thread-safety and readability; harder to give a lambda a good name/stack trace; complex algorithms belong in named classes; you lose a place to hang shared helper methods.
**Mechanism:** a **functional interface** lets the compiler treat a lambda/method reference as a strategy instance — behavior parameterization without a named class.

### Variant 5 — DI + Strategy Registry (Production)

The production-grade approach in a Spring/Guice codebase: each strategy is a managed bean that self-identifies with a key; a registry auto-collects them into a `Map<Key, Strategy>`; the context looks the strategy up by a runtime value (config, request field, feature flag). **What it solves:** new strategies are added by dropping in a new bean — **zero edits** to the context or the registry; dependencies are injected cleanly; selection is data-driven.

<details>
<summary>💻 Click to expand code</summary>

```java
public interface PaymentStrategy {
    PaymentType type();          // self-identifying key
    PaymentResult pay(PaymentRequest req);
}

public enum PaymentType { CREDIT_CARD, PAYPAL, UPI }

@Component
public class CreditCardStrategy implements PaymentStrategy {
    private final FraudService fraud;          // injected dependency
    public CreditCardStrategy(FraudService fraud) { this.fraud = fraud; }
    @Override public PaymentType type() { return PaymentType.CREDIT_CARD; }
    @Override public PaymentResult pay(PaymentRequest req) {
        fraud.check(req);
        return PaymentResult.ok("Charged card " + req.maskedCard());
    }
}

// Registry: Spring injects ALL PaymentStrategy beans; we index them by type
@Component
public class PaymentStrategyRegistry {
    private final Map<PaymentType, PaymentStrategy> byType;
    public PaymentStrategyRegistry(List<PaymentStrategy> strategies) {
        this.byType = strategies.stream()
            .collect(Collectors.toMap(PaymentStrategy::type, s -> s));
    }
    public PaymentStrategy resolve(PaymentType type) {
        PaymentStrategy s = byType.get(type);
        if (s == null) throw new IllegalArgumentException("Unsupported: " + type);
        return s;
    }
}

// Context — data-driven selection, closed for modification:
@Service
public class CheckoutService {
    private final PaymentStrategyRegistry registry;
    public CheckoutService(PaymentStrategyRegistry registry) { this.registry = registry; }
    public PaymentResult checkout(PaymentRequest req) {
        return registry.resolve(req.getType()).pay(req);   // O(1) lookup + delegate
    }
}
```
</details>

**Pros:** truly Open/Closed (add a bean, done); dependencies injected; O(1) data-driven selection; testable via mock beans; supports feature flags/A-B testing by resolving on a runtime key.
**Cons:** requires a DI framework/wiring; registry indirection adds a small learning curve; must guard against duplicate keys and missing strategies.
**Mechanism:** **dependency injection + a keyed registry** turns "which algorithm?" into a data lookup, removing all conditional logic from the context.

---

## 🎨 Real-World Example

Two complete, production-shaped scenarios. **Scenario 1** is an e-commerce discount/pricing engine (the classic "vary the business rule" use). **Scenario 2** is a Google-Maps-style routing engine (the classic "vary the algorithm" use). Both keep a stable workflow and swap the algorithm behind a common interface.

### Scenario 1 — E-commerce Discount / Pricing Engine

<details>
<summary>💻 Click to expand code</summary>

**The setup.** A cart must compute a final price, but the discount rule changes constantly: no promo, a flat seasonal percentage, a loyalty-tier rate, BOGO, etc. Marketing adds promotions weekly and must never force a redeploy of the checkout code. So we make the *workflow* (`finalPrice`) stable and the *discount algorithm* a pluggable `DiscountStrategy`. Each strategy is **stateless or immutable-config**, which makes a single instance safe to share across every request thread.

Note the three concrete strategies we exercise below:
- **`NoDiscount`** — the **Null Object**. It returns the subtotal unchanged, so the engine never needs a null check and "no promo" is just another strategy. We expose it as a shared singleton (`NoDiscount.INSTANCE`) and also use it as the engine's safe **default** for unknown promo codes.
- **`PercentageDiscount`** — carries its percentage as immutable per-instance config, so one class serves `SEASON10` (10%), `FLASH25` (25%), etc., just by constructing it with a different rate. This shows a strategy can be *parameterized* rather than duplicated per value.
- **`LoyaltyTierDiscount`** — reads the customer's tier from the order and applies a rate, demonstrating a strategy that depends on request data.

```java
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;

// ---------- Strategy ----------
@FunctionalInterface
interface DiscountStrategy {
    /** Returns the discounted total. MUST be stateless / thread-safe. */
    BigDecimal apply(BigDecimal subtotal, Order order);
}

// ---------- Concrete strategies (stateless singletons) ----------
// Null Object strategy: "no promo" is a first-class algorithm, not a null.
final class NoDiscount implements DiscountStrategy {
    static final NoDiscount INSTANCE = new NoDiscount();
    public BigDecimal apply(BigDecimal s, Order o) { return s; }  // unchanged
}

// Parameterized strategy: one class, many promos, via immutable config.
final class PercentageDiscount implements DiscountStrategy {
    private final BigDecimal pct;                 // config carried per-instance (immutable)
    PercentageDiscount(double pct) { this.pct = BigDecimal.valueOf(pct); }
    public BigDecimal apply(BigDecimal s, Order o) {
        return s.subtract(s.multiply(pct)).setScale(2, RoundingMode.HALF_UP);
    }
}

// Request-data-driven strategy.
final class LoyaltyTierDiscount implements DiscountStrategy {
    public BigDecimal apply(BigDecimal s, Order o) {
        BigDecimal rate = switch (o.tier()) {
            case GOLD     -> new BigDecimal("0.15");
            case SILVER   -> new BigDecimal("0.10");
            case STANDARD -> new BigDecimal("0.02");
        };
        return s.subtract(s.multiply(rate)).setScale(2, RoundingMode.HALF_UP);
    }
}

// ---------- Context ----------
final class PricingEngine {
    private final Map<String, DiscountStrategy> registry;
    PricingEngine(Map<String, DiscountStrategy> registry) {
        this.registry = Map.copyOf(registry);     // immutable => shareable
    }
    BigDecimal finalPrice(String promoCode, BigDecimal subtotal, Order order) {
        // Unknown code falls back to the NoDiscount Null Object — no null checks needed.
        DiscountStrategy s = registry.getOrDefault(promoCode, NoDiscount.INSTANCE);
        return s.apply(subtotal, order);
    }
}

// ---------- Supporting types ----------
enum Tier { STANDARD, SILVER, GOLD }
record Order(String id, Tier tier) { }

// ---------- Demo / usage (multiple call sites + concurrency test) ----------
public class PricingDemo {
    public static void main(String[] args) throws Exception {
        Map<String, DiscountStrategy> promos = new HashMap<>();
        promos.put("NONE",     NoDiscount.INSTANCE);          // Null Object strategy
        promos.put("SEASON10", new PercentageDiscount(0.10)); // 10% off
        promos.put("FLASH25",  new PercentageDiscount(0.25)); // same class, 25% off
        promos.put("LOYALTY",  new LoyaltyTierDiscount());
        PricingEngine engine = new PricingEngine(promos);

        Order gold = new Order("A1", Tier.GOLD);
        BigDecimal sub = new BigDecimal("200.00");

        // --- NoDiscount in action (explicit code + default fallback) ---
        System.out.println(engine.finalPrice("NONE",  sub, gold)); // 200.00  (NoDiscount)
        System.out.println(engine.finalPrice("BOGUS", sub, gold)); // 200.00  (default -> NoDiscount)

        // --- PercentageDiscount in action (one class, two promos) ---
        System.out.println(engine.finalPrice("SEASON10", sub, gold)); // 180.00 (10% off)
        System.out.println(engine.finalPrice("FLASH25",  sub, gold)); // 150.00 (25% off)

        // --- LoyaltyTierDiscount in action (reads order data) ---
        System.out.println(engine.finalPrice("LOYALTY",  sub, gold)); // 170.00 (GOLD = 15%)

        // Thread-safety: stateless strategies shared across 100 threads safely
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<BigDecimal>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            futures.add(pool.submit(() -> engine.finalPrice("LOYALTY", sub, gold)));
        }
        for (Future<BigDecimal> f : futures) {
            assert f.get().equals(new BigDecimal("170.00")); // all consistent
        }
        pool.shutdown();
        System.out.println("All 100 concurrent computations consistent ✅");
    }
}
```

**Why this is FAANG-grade:** strategies are **stateless** (or immutable-config) so one instance serves all threads; `NoDiscount` is a **Null Object** that removes null checks and doubles as the safe default; `PercentageDiscount` is **parameterized** so one class covers every percentage promo; the registry is **immutable** (`Map.copyOf`); selection is **data-driven** with `getOrDefault`; `BigDecimal` avoids float money bugs; and new promos are added without touching `PricingEngine`.

</details>

### Scenario 2 — Google Maps Routing Engine

<details>
<summary>💻 Click to expand code</summary>

**The setup.** A maps product must find a route from A to B, but "best" depends on the user's chosen mode: driving optimizes for time (traffic-aware), walking optimizes for distance and ignores one-way roads, cycling prefers bike lanes and avoids highways, and transit uses timetables. These are genuinely *different algorithms* over the same road graph — the textbook Strategy case (this mirrors how the real Google Maps / Directions API exposes a `mode` and `avoid` set). The `RoutePlanner` (context) stays identical; only the `RoutingStrategy` changes.

Each strategy is **stateless** and receives an immutable `RouteRequest` (origin, destination, avoid-set), so a single instance is shared across all planning threads. We select the strategy from a registry keyed by `TravelMode`, exactly like a `mode=driving` query parameter.

```java
import java.util.*;

// ---------- Domain ----------
enum TravelMode { DRIVING, WALKING, CYCLING, TRANSIT }
record LatLng(double lat, double lng) {}
record RouteRequest(LatLng origin, LatLng dest, Set<String> avoid) {}      // immutable
record Route(String mode, double distanceKm, int etaMinutes, List<String> steps) {
    @Override public String toString() {
        return mode + ": " + distanceKm + "km, " + etaMinutes + "min, steps=" + steps;
    }
}

// ---------- Strategy ----------
interface RoutingStrategy {
    TravelMode mode();                 // self-identifying key for the registry
    Route findRoute(RouteRequest req); // the varying algorithm over the road graph
}

// ---------- Concrete strategies (stateless, shareable) ----------
final class DrivingStrategy implements RoutingStrategy {
    private final TrafficService traffic;          // injected dependency
    DrivingStrategy(TrafficService traffic) { this.traffic = traffic; }
    public TravelMode mode() { return TravelMode.DRIVING; }
    public Route findRoute(RouteRequest req) {
        // Time-optimal, traffic-aware shortest path (e.g., A* with live-traffic weights).
        double km = 12.4;
        int eta = traffic.adjustedEta(km, /*baseMinutes=*/14); // congestion multiplier
        return new Route("driving", km, eta, List.of("Take I-280 N", "Exit 43", "Arrive"));
    }
}

final class WalkingStrategy implements RoutingStrategy {
    public TravelMode mode() { return TravelMode.WALKING; }
    public Route findRoute(RouteRequest req) {
        // Distance-optimal; ignores one-way restrictions, allows footpaths.
        return new Route("walking", 3.1, 38, List.of("Cut through park", "Cross at 5th", "Arrive"));
    }
}

final class CyclingStrategy implements RoutingStrategy {
    public TravelMode mode() { return TravelMode.CYCLING; }
    public Route findRoute(RouteRequest req) {
        // Prefers bike lanes, penalizes highways and steep grades.
        boolean avoidHills = req.avoid().contains("hills");
        int eta = avoidHills ? 26 : 22;
        return new Route("cycling", 5.0, eta, List.of("Bike lane on Elm", "River trail", "Arrive"));
    }
}

// A dependency of the driving strategy (mocked here).
interface TrafficService { int adjustedEta(double km, int baseMinutes); }

// ---------- Context ----------
final class RoutePlanner {
    private final Map<TravelMode, RoutingStrategy> byMode;
    RoutePlanner(List<RoutingStrategy> strategies) {
        Map<TravelMode, RoutingStrategy> m = new EnumMap<>(TravelMode.class);
        for (RoutingStrategy s : strategies) m.put(s.mode(), s);   // self-registration
        this.byMode = Map.copyOf(m);
    }
    Route plan(TravelMode mode, RouteRequest req) {
        RoutingStrategy s = byMode.get(mode);
        if (s == null) throw new IllegalArgumentException("Unsupported mode: " + mode);
        return s.findRoute(req);                                   // O(1) lookup + delegate
    }
}

// ---------- Demo / usage (multiple call sites, like a directions API) ----------
public class MapsDemo {
    public static void main(String[] args) {
        TrafficService traffic = (km, base) -> (int) Math.round(base * 1.35); // 35% congestion
        RoutePlanner planner = new RoutePlanner(List.of(
            new DrivingStrategy(traffic),
            new WalkingStrategy(),
            new CyclingStrategy()
        ));

        RouteRequest req = new RouteRequest(
            new LatLng(37.42, -122.08), new LatLng(37.39, -122.05), Set.of("hills"));

        // Same context, different algorithm per mode (mode=driving/walking/cycling)
        System.out.println(planner.plan(TravelMode.DRIVING, req)); // driving: 12.4km, 19min, ...
        System.out.println(planner.plan(TravelMode.WALKING, req)); // walking: 3.1km, 38min, ...
        System.out.println(planner.plan(TravelMode.CYCLING, req)); // cycling: 5.0km, 26min, ...
    }
}
```

**Why this is FAANG-grade:** the four travel modes are genuinely distinct algorithms sharing one interface, so adding `TRANSIT` (timetable-based) is a new class registered in the list with **zero edits** to `RoutePlanner`; strategies are **stateless** and take an **immutable request**, so instances are shared safely across concurrent requests; the `DrivingStrategy` gets its `TrafficService` **injected**, showing strategies can carry real dependencies; and an **`EnumMap`** registry gives fast, allocation-light `mode → strategy` lookup — mirroring a real `mode=` API parameter.

</details>

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- You have **multiple variants of an algorithm** and need to switch between them at runtime (per request, per user, per config).
- A class is riddled with **conditional statements (`if/else`, `switch`) selecting behavior** — Strategy replaces them with polymorphism.
- You want to **isolate the algorithm's implementation details** from the code that uses it.
- Different variants of behavior should be **independently testable and reusable**.
- You need to add new algorithms frequently **without modifying existing, working code** (Open/Closed).
- You want to **prefer composition over inheritance** instead of exploding a subclass hierarchy to vary behavior.
- Sorting/ordering (`Comparator`), pricing/discounts, payment methods, compression, routing, retry/backoff policies, validation rules, serialization formats.

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- You have **only one algorithm** and no realistic prospect of a second — Strategy is premature abstraction.
- The variants **never change at runtime** and are trivial — a simple `if` or a `Map` may be clearer.
- The algorithms are **so different they can't share a meaningful interface** — forcing a common interface leaks abstractions.
- The behavior differences are **tiny** (one constant, one flag) — parameterize a method instead of creating strategy classes.
- Clients would need to understand the differences between strategies deeply to pick one — the abstraction isn't buying you decoupling.
- Excessive strategies for micro-variations create **class explosion** and cognitive overhead without benefit.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- **Open/Closed Principle** — add new algorithms without modifying the context.
- **Eliminates conditionals** — replaces `switch`/`if-else` chains with polymorphism.
- **Runtime flexibility** — swap algorithms dynamically.
- **Isolation & testability** — each algorithm is a small, independently unit-testable unit.
- **Composition over inheritance** — avoids rigid subclass hierarchies.
- **Reuse** — the same strategy can be shared across many contexts.

**Cons**

- **More classes/objects** — potential class explosion for many small variants.
- **Client awareness** — the client must know the strategies exist to choose one (mitigated by factory/registry/DI).
- **Communication overhead** — if a strategy needs lots of data from the context, the interface can get chatty.
- **Overkill** for a single or rarely-changing algorithm.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

| Pattern | Intent | Key difference from Strategy |
|---|---|---|
| **State** | Allow an object to alter behavior when its internal state changes | *Structurally identical* to Strategy, but State transitions between states (states often know about each other), and change is driven by internal state; Strategy variants are independent and chosen by the client. |
| **Command** | Encapsulate a request as an object | Command encapsulates *what to do and when* (supports queue/undo/log); Strategy encapsulates *how to do one thing*. Command is about invocation, Strategy about algorithm selection. |
| **Template Method** | Define algorithm skeleton, defer steps to subclasses | Template Method uses **inheritance** and fixes the skeleton at compile time; Strategy uses **composition** and swaps the whole algorithm at runtime. |
| **Factory Method** | Create objects without specifying concrete class | A Factory often *produces* the Strategy instance; they're complementary, not competing. |
| **Decorator** | Add responsibilities to an object dynamically | Decorator *layers/augments* behavior; Strategy *replaces* the algorithm entirely. |

**One-liner memory hooks:** Strategy = *how*; State = *what mode I'm in*; Command = *do this later/undo*; Template Method = *inheritance version of Strategy*.

</details>

## 📊 Comparison Table

<details>
<summary>📖 Click to expand</summary>

| Axis | V1 if/else | V2 Interface+Classes | V3 Enum | V4 Lambda | V5 DI + Registry |
|---|---|---|---|---|---|
| Open/Closed | ❌ | ✅ | ⚠️ (fixed set) | ✅ | ✅✅ |
| Runtime switching | ✅ | ✅ | ✅ | ✅ | ✅ |
| Add strategy at runtime/plugin | ❌ | ✅ | ❌ | ✅ | ✅ |
| Type safety | ❌ (strings) | ✅ | ✅✅ | ✅ | ✅ |
| Boilerplate | Low | High | Medium | Lowest | Medium |
| Per-instance config | n/a | ✅ | ⚠️ limited | ⚠️ via capture | ✅ |
| DI / dependency support | ❌ | ✅ | ❌ | ⚠️ | ✅✅ |
| Thread-safe by default | depends | if stateless | ✅ (singletons) | if stateless | if stateless |
| Best for | 2 fixed cases | classic OOP | fixed stateless set | simple one-liners | large, evolving systems |

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

**Pitfall 1 — Stateful strategies shared across threads (data races).**
A strategy holding mutable instance state, shared as a singleton, corrupts under concurrency.

<details>
<summary>❌ Failure</summary>

```java
class SumStrategy implements Strategy {
    private int total = 0;                 // mutable shared state!
    public void apply(int x) { total += x; } // race across threads
}
```
</details>

<details>
<summary>✅ Fix — make strategies stateless; pass state as parameters/return values</summary>

```java
class SumStrategy implements Strategy {
    public int apply(int running, int x) { return running + x; } // pure, thread-safe
}
```
</details>

**Pitfall 2 — Leaking a giant `switch` into the client / factory.**
You add strategy classes but the selection logic is still one big `switch(type)` — you've just moved the smell.

<details>
<summary>✅ Fix — data-driven registry (self-registering strategies)</summary>

```java
Map<Type, Strategy> registry = strategies.stream()
    .collect(Collectors.toMap(Strategy::type, s -> s));
Strategy s = registry.get(type);  // no switch anywhere
```
</details>

**Pitfall 3 — Over-fragmenting into micro-strategies.**
Creating a strategy class for a one-line difference (e.g., different tax constant) causes class explosion and hurts readability. Fix: parameterize a single strategy, or use a lambda. Reserve full classes for genuinely different algorithms.

**Pitfall 4 — Chatty context/strategy interface.**
If a strategy needs 8 fields from the context, you either pass a huge parameter list or hand it the whole context (tight coupling). Fix: pass a focused, immutable *request/DTO* object carrying exactly what strategies need.

</details>

## 🎓 Interview Tips

<details>
<summary>📖 What interviewers commonly ask</summary>

- "Refactor this `switch`-heavy class using Strategy" (payment/discount/sort are classic prompts).
- "Difference between Strategy and State?" (structurally identical, semantically different).
- "How does `Comparator` relate to Strategy?"
- "How do you select the right strategy without another giant `switch`?" (registry/DI/enum-map).
- "How do you keep strategies thread-safe?" (stateless/immutable, share instances).
- "Strategy vs Template Method?" (composition vs inheritance).

</details>

<details>
<summary>💬 What to proactively mention</summary>

- Strategy embodies **composition over inheritance** and the **Open/Closed Principle**.
- Prefer **stateless strategies** so a single instance is shareable and thread-safe.
- Use a **registry/DI map** keyed by an enum or ID to avoid replacing one conditional with another.
- Java's **functional interfaces** make lightweight strategies (lambdas/method refs) idiomatic.
- Real JDK/framework uses: `Comparator`, `ThreadPoolExecutor`'s `RejectedExecutionHandler`, Spring's `PlatformTransactionManager`.
- Mention trade-off: class explosion vs the flexibility gained.

</details>

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Factory Method / Abstract Factory** — commonly create/resolve the strategy instance.
- **State** — same structure; models state-dependent behavior with transitions.
- **Template Method** — inheritance-based alternative when only *steps* vary.
- **Command** — encapsulates an action; strategies can be implemented as commands.
- **Decorator** — can wrap/augment a strategy's behavior.
- **Dependency Injection** — the modern delivery mechanism for strategies.

</details>

## 📚 Library/Framework Implementation

<details>
<summary>1️⃣ java.util.Comparator (the canonical Strategy)</summary>

`Collections.sort(list, comparator)` / `list.sort(comparator)` take a comparison *strategy*. The sorting algorithm (TimSort) is fixed; the ordering algorithm is pluggable and composable.

```java
List<Employee> emps = ...;
emps.sort(Comparator.comparing(Employee::getDept)
                    .thenComparing(Employee::getSalary, Comparator.reverseOrder()));
```
</details>

<details>
<summary>2️⃣ ThreadPoolExecutor.RejectedExecutionHandler</summary>

When the queue is full, the executor delegates to a rejection *strategy*: `AbortPolicy`, `CallerRunsPolicy`, `DiscardPolicy`, `DiscardOldestPolicy` — swap the algorithm without changing the executor.

```java
new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(10),
    new ThreadPoolExecutor.CallerRunsPolicy()); // strategy injected
```
</details>

<details>
<summary>3️⃣ Spring PlatformTransactionManager / Resource loading</summary>

Spring injects a transaction *strategy* (`DataSourceTransactionManager`, `JpaTransactionManager`, `JtaTransactionManager`) behind the `PlatformTransactionManager` interface; the same transactional code runs against any backend. Similarly, `ResourceLoader` picks a resource-access strategy by URL prefix.

```java
@Bean
PlatformTransactionManager txManager(DataSource ds) {
    return new DataSourceTransactionManager(ds); // swap impl, code unchanged
}
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1 [Conceptual]: What is the Strategy pattern and what problem does it solve?</strong></summary>

Strategy is a behavioral pattern that defines a family of interchangeable algorithms, encapsulates each behind a common interface, and lets clients swap them at runtime. It solves the problem of a class that must support multiple variants of a behavior without hard-coding them via conditionals. Instead of `if/else` branches selecting behavior, the context holds a strategy object and delegates to it. This satisfies the Open/Closed Principle (add algorithms without editing the context), enables independent testing of each algorithm, and favors composition over inheritance. The canonical use cases are payment processing, sorting/comparison, and pricing/discount engines.

</details>

<details>
<summary><strong>Q2 [Conceptual]: What are the participants in the Strategy pattern?</strong></summary>

Four participants: (1) **Strategy** — the interface declaring the algorithm's operation; (2) **ConcreteStrategy** — classes implementing specific algorithms; (3) **Context** — holds a reference to a Strategy and delegates work to it via the interface; (4) **Client** — chooses and injects the concrete strategy into the context. The critical relationship is that the Context depends only on the Strategy *interface* (abstraction), never on concrete strategies, which is what allows algorithms to vary independently of clients.

</details>

<details>
<summary><strong>Q3 [Conceptual]: How does Strategy embody "composition over inheritance"?</strong></summary>

Inheritance varies behavior by subclassing: to support N behaviors you create N subclasses, fixing the choice at compile time and locking you into a single dimension of variation. Strategy instead *composes* the behavior as an injected object — the context *has-a* strategy rather than *is-a* subtype. This lets behavior change at runtime, allows the same strategy to be reused by multiple contexts, keeps the behavior independently testable, and avoids the combinatorial subclass explosion that occurs when multiple behaviors vary independently.

</details>

<details>
<summary><strong>Q4 [Conceptual]: How is Strategy different from the State pattern?</strong></summary>

They are structurally near-identical (a context delegating to an interchangeable object), but differ in intent. In **Strategy**, the algorithms are independent, the client picks one, and it usually doesn't change mid-operation; strategies don't know about each other. In **State**, the "strategy" represents a *mode* of the object, transitions between states are part of the pattern (states often trigger the next state), and the change is driven by the object's internal condition, not the client. Rule of thumb: Strategy is about *how to do something*; State is about *what mode the object is in*.

</details>

<details>
<summary><strong>Q5 [Implementation]: Refactor a switch-based payment class into Strategy. (code)</strong></summary>

Extract each branch into a class implementing a `PaymentStrategy` interface; the context holds and delegates to a strategy.

```java
interface PaymentStrategy { void pay(double amount); }
class CardPayment  implements PaymentStrategy { public void pay(double a){ /*...*/ } }
class PayPalPayment implements PaymentStrategy { public void pay(double a){ /*...*/ } }

class Checkout {
    private PaymentStrategy strategy;
    void setStrategy(PaymentStrategy s){ this.strategy = s; }
    void pay(double amount){ strategy.pay(amount); }
}
```

Adding a new method (Apple Pay) is now a new class, with zero edits to `Checkout`.

</details>

<details>
<summary><strong>Q6 [Implementation]: Implement Strategy using enums. When is this best? (code)</strong></summary>

Use an enum where each constant implements the interface — best when the strategy set is fixed at compile time and the strategies are stateless (each constant is a thread-safe singleton).

```java
enum Op {
    ADD { public int apply(int a,int b){ return a+b; } },
    MUL { public int apply(int a,int b){ return a*b; } };
    public abstract int apply(int a, int b);
}
int r = Op.valueOf("ADD").apply(2,3); // config-driven, type-safe
```

You get free `valueOf` lookup and no allocation, at the cost of losing runtime extensibility and easy DI.

</details>

<details>
<summary><strong>Q7 [Implementation]: How do lambdas simplify Strategy in Java 8+? (code)</strong></summary>

If the strategy is a single-method (functional) interface, callers pass a lambda or method reference instead of writing a class — behavior parameterization with minimal boilerplate. Strategies become first-class values you can store in maps and compose.

```java
@FunctionalInterface interface Discount { double apply(double p); }
Map<String, Discount> promos = Map.of(
    "NONE",  p -> p,
    "TEN",   p -> p * 0.9,
    "HALF",  p -> p * 0.5);
double price = promos.get("TEN").apply(100); // 90.0
```

`Comparator.comparing(...)` returning composable comparators is the JDK's own example.

</details>

<details>
<summary><strong>Q8 [Implementation]: Build a strategy registry that avoids a selection switch. (code)</strong></summary>

Have each strategy declare its key, then collect all strategies into a map — selection becomes an O(1) lookup with no conditionals.

```java
interface Strategy { String key(); void run(); }
class Registry {
    private final Map<String,Strategy> map;
    Registry(List<Strategy> all){
        map = all.stream().collect(Collectors.toMap(Strategy::key, s->s));
    }
    Strategy get(String k){
        return Optional.ofNullable(map.get(k))
                       .orElseThrow(() -> new IllegalArgumentException(k));
    }
}
```

In Spring, inject `List<Strategy>` and Spring supplies every bean automatically.

</details>

<details>
<summary><strong>Q9 [Breaking]: A shared strategy instance produces wrong results under load. Why?</strong></summary>

Almost always because the strategy holds **mutable instance state** and is shared across threads, creating data races. For example, a strategy accumulating into an instance field will interleave updates from concurrent callers. The fix is to make strategies **stateless** — pass all inputs as parameters and return results, so a single instance can be shared safely — or, if state is unavoidable, create a new strategy per operation or guard state with synchronization. This is why the idiomatic guidance is "prefer stateless strategies."

</details>

<details>
<summary><strong>Q10 [Breaking]: What's wrong with replacing if/else with a factory that still uses a big switch?</strong></summary>

You've relocated the smell, not removed it. The giant `switch(type)` in the factory still violates Open/Closed — every new strategy edits it — and it becomes a central choke point and merge-conflict magnet. The proper fix is a **self-registering, data-driven registry**: each strategy declares its key and is auto-collected into a `Map<Key, Strategy>` (via DI or a static registration block), so adding a strategy touches no existing code.

</details>

<details>
<summary><strong>Q11 [Breaking]: How can Strategy hurt readability/maintainability if misused?</strong></summary>

By over-fragmentation: creating a full strategy class for trivial, one-line differences causes **class explosion**, scatters closely-related logic across many files, and forces readers to jump around to understand behavior. It also hurts when the strategy interface becomes **chatty** (needs many fields from the context), coupling strategies tightly to context internals. Mitigate by using lambdas for trivial variants, parameterizing a single strategy where differences are just data, and passing a focused DTO to strategies rather than the whole context.

</details>

<details>
<summary><strong>Q12 [Breaking]: What happens if no strategy is set on the context?</strong></summary>

You get a `NullPointerException` at delegation time, often far from the root cause, which is a poor failure mode. Defenses: (a) require the strategy in the constructor so the context can never exist without one; (b) provide a sensible **default/Null Object strategy** (e.g., `NoDiscount`) so behavior degrades gracefully; (c) validate eagerly in `setStrategy` and throw a clear `IllegalStateException` at `checkout` if still unset. Constructor injection plus a Null Object default is the most robust combination.

</details>

<details>
<summary><strong>Q13 [Trade-off]: Strategy vs Template Method — when do you pick each?</strong></summary>

Use **Template Method** when the overall algorithm skeleton is fixed and only a few *steps* vary, and compile-time variation via subclassing is acceptable — it's simpler and keeps shared logic in the base class. Use **Strategy** when you need to swap the *whole* algorithm at runtime, share/reuse algorithms across unrelated contexts, or avoid a subclass explosion when multiple behaviors vary independently. Template Method = inheritance + fixed skeleton; Strategy = composition + runtime swappable. They can combine: a template method whose varying step is itself a strategy.

</details>

<details>
<summary><strong>Q14 [Trade-off]: Enum strategy vs class-based strategy vs lambda — trade-offs?</strong></summary>

**Enum:** type-safe, thread-safe singletons, free `valueOf`; but fixed at compile time, awkward for DI/per-instance config. **Class-based:** full OOP flexibility, DI, per-instance config, runtime/plugin extensibility; but most boilerplate. **Lambda:** least boilerplate, composable, great for stateless one-liners; but poor for complex logic, harder to name/trace, and captured mutable state risks thread-safety. Choose enum for a small fixed stateless set, classes for large evolving systems with dependencies, lambdas for simple inline behaviors.

</details>

<details>
<summary><strong>Q15 [Trade-off]: How do you decide between Strategy and just using a Map/config?</strong></summary>

If the variation is purely *data* (a rate, a threshold, a message template), a `Map` or config value is simpler and you don't need Strategy at all. Reach for Strategy when the variation is genuinely *behavioral* — different control flow, different collaborators, different validation — that can't be reduced to a parameter. A good tell: if you'd write meaningfully different code (not just a different constant) per case, it's a Strategy; if only a value changes, it's config.

</details>

<details>
<summary><strong>Q16 [Advanced/Concurrency]: How do you make a strategy-based system thread-safe?</strong></summary>

Prefer **stateless** strategies (pure functions of their inputs) so a single instance is safely shared by all threads — this is the primary technique and why enum/singleton strategies are popular. If a strategy must hold configuration, make it **immutable** (final fields, defensive copies) so it's safe to publish and share. If a strategy genuinely needs per-operation mutable state, instantiate it per call or confine it to a thread. The registry/map holding strategies should also be immutable or concurrent so lookups don't race with registration.

</details>

<details>
<summary><strong>Q17 [Advanced]: How would you support A/B testing or feature flags with Strategy?</strong></summary>

Keep the strategies in a registry keyed by variant, and select the strategy using the experiment/flag value at request time: `registry.resolve(experimentService.variantFor(userId))`. Because selection is data-driven, you can roll out a new algorithm to 5% of traffic by having the flag service return the new key for those users, with zero code changes to the context. This cleanly separates *which* algorithm from *how* it runs and makes experiments observable and reversible via config.

</details>

<details>
<summary><strong>Q18 [Advanced]: Can strategies be composed? How?</strong></summary>

Yes — composition is a major benefit when the interface is functional. `Comparator` composes with `thenComparing`/`reversed`; `Function` with `andThen`/`compose`. You can also build a **CompositeStrategy** that holds a list of strategies and applies them in sequence (e.g., a pricing pipeline: loyalty discount → coupon → tax), or a **chain** where each strategy may short-circuit. This turns small, single-purpose strategies into rich behaviors without a combinatorial number of classes.

```java
Discount pipeline = base.andThen(coupon).andThen(tax); // functional composition
```

</details>

<details>
<summary><strong>Q19 [Advanced/Coding challenge]: Design a pluggable compression system. Full solution. (code)</strong></summary>

Requirements: support GZIP/LZ4/etc., choose at runtime, add algorithms without editing the pipeline.

```java
interface CompressionStrategy {
    String name();
    byte[] compress(byte[] data);
    byte[] decompress(byte[] data);
}

class GzipStrategy implements CompressionStrategy {
    public String name(){ return "gzip"; }
    public byte[] compress(byte[] d){ /* GZIPOutputStream */ return d; }
    public byte[] decompress(byte[] d){ /* GZIPInputStream */ return d; }
}
class Lz4Strategy implements CompressionStrategy {
    public String name(){ return "lz4"; }
    public byte[] compress(byte[] d){ return d; }
    public byte[] decompress(byte[] d){ return d; }
}

class Compressor {
    private final Map<String, CompressionStrategy> byName;
    Compressor(List<CompressionStrategy> strategies){
        this.byName = strategies.stream()
            .collect(Collectors.toMap(CompressionStrategy::name, s -> s));
    }
    byte[] compress(String algo, byte[] data){
        CompressionStrategy s = byName.get(algo);
        if (s == null) throw new IllegalArgumentException("Unknown algo: " + algo);
        return s.compress(data);
    }
}

// Usage:
Compressor c = new Compressor(List.of(new GzipStrategy(), new Lz4Strategy()));
byte[] out = c.compress("gzip", "hello".getBytes());
```

Adding Brotli = one new class registered in the list; `Compressor` never changes.

</details>

<details>
<summary><strong>Q20 [Advanced/Coding challenge]: Implement a retry system with pluggable backoff strategies. Full solution. (code)</strong></summary>

Backoff (fixed, exponential, jittered) is the varying algorithm; the retry loop is stable.

```java
@FunctionalInterface
interface BackoffStrategy { long delayMillis(int attempt); }

class Backoffs {
    static final BackoffStrategy FIXED       = a -> 100L;
    static final BackoffStrategy EXPONENTIAL = a -> (long) (100 * Math.pow(2, a));
    static final BackoffStrategy JITTERED    = a -> {
        long base = (long) (100 * Math.pow(2, a));
        return base / 2 + (long) (Math.random() * base / 2);
    };
}

class Retryer {
    private final int maxAttempts;
    private final BackoffStrategy backoff;
    Retryer(int maxAttempts, BackoffStrategy backoff){
        this.maxAttempts = maxAttempts; this.backoff = backoff;
    }
    <T> T execute(Callable<T> task) throws Exception {
        Exception last = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++){
            try { return task.call(); }
            catch (Exception e){
                last = e;
                Thread.sleep(backoff.delayMillis(attempt)); // strategy decides delay
            }
        }
        throw last;
    }
}

// Usage:
Retryer r = new Retryer(5, Backoffs.JITTERED);
String body = r.execute(() -> httpClient.get("/api"));
```

Swapping `FIXED`→`JITTERED` needs no change to `Retryer`; jittered backoff prevents thundering-herd retries.

</details>

---

## 🏛️ Staff/Principal Engineer Questions

<details>
<summary><strong>SP1: How do you architect a plugin system where strategies are loaded dynamically at runtime (not known at compile time)?</strong></summary>

Use Java's `ServiceLoader` (SPI) or an OSGi/module system: define the strategy interface in a core module, let plugin JARs provide implementations declared in `META-INF/services`, and discover them at startup via `ServiceLoader.load(Strategy.class)`, populating a registry keyed by each strategy's declared ID. For hot-reload, load plugin JARs in isolated `ClassLoader`s so they can be swapped/unloaded, and guard the registry with a concurrent map so lookups don't race with (de)registration. Key concerns: versioning the SPI contract (backward compatibility), classloader leaks on unload, validating/sandboxing untrusted plugins, and observability (which plugin served a request). This keeps the core closed while third parties extend behavior without recompiling the host.

</details>

<details>
<summary><strong>SP2: Config-driven strategy selection across a fleet — how do you roll out and govern it safely?</strong></summary>

Centralize the "key → strategy" mapping in a dynamic config service (e.g., a config store with live updates). Strategies self-register into a registry; the runtime selection key comes from config that can target by tenant, region, or percentage. For safe rollout: version the config, support gradual percentage ramps, provide instant rollback (flip the key back), and emit metrics tagged by chosen strategy so you can compare error rates/latency per variant. Guard against config referencing a strategy the binary doesn't have (fail closed to a safe default and alert). Treat the config as an audited, reviewed artifact — a bad strategy selection is a production change even though no code shipped.

</details>

<details>
<summary><strong>SP3: How do you test a large strategy-based subsystem effectively?</strong></summary>

Test at three levels. (1) **Each strategy in isolation** as a pure unit — this is the payoff of the pattern; stateless strategies make these tests trivial and parallelizable. (2) **The registry/selection logic** — verify every key resolves, no duplicate keys, unknown keys fail as intended, and the default is applied correctly. (3) **The context/integration** — verify delegation and that the correct strategy is chosen given inputs/flags, ideally with a **contract test** (a shared test suite every strategy implementation must pass) to guarantee behavioral consistency across implementations. Use property-based tests where strategies must satisfy invariants (e.g., discount ≤ subtotal). Mock injected dependencies at the strategy boundary.

</details>

<details>
<summary><strong>SP4: When does Strategy become the wrong abstraction at scale, and what replaces it?</strong></summary>

Strategy assumes a *stable interface* and *independent, single-step* algorithms. It strains when: variants need very different inputs (interface becomes a lowest-common-denominator or a chatty bag of optionals); behavior is multi-step with cross-cutting concerns (better served by a **pipeline/Chain of Responsibility** or a workflow engine); selection depends on complex rules (a **rules engine** or decision table beats hand-rolled selection); or when variants must coordinate/transition (that's **State** or a state machine). At true scale you may externalize the algorithm entirely to a separate service (strategy-as-a-service) for independent deployment. Recognizing the interface strain — many `instanceof` checks, optional params, or downcasts — is the signal to graduate beyond plain Strategy.

</details>

<details>
<summary><strong>SP5: How do you handle strategies with different dependencies, lifecycles, and failure modes in a DI system?</strong></summary>

Let the DI container own strategy lifecycles: each strategy is a bean that declares its own dependencies (fraud service, external clients), so wiring is explicit and testable. For heavy or optional strategies, inject **lazily** or via a `Provider`/factory so you don't pay initialization cost unless selected. Isolate failure: wrap each strategy invocation with per-strategy timeouts, circuit breakers, and bulkheads so one flaky provider (e.g., a payment gateway) can't exhaust threads for others. Emit per-strategy metrics and traces. For strategies backed by external systems, standardize error mapping in the interface contract (e.g., return a `Result` type rather than throwing provider-specific exceptions) so the context handles all strategies uniformly. This preserves the pattern's decoupling while acknowledging that real strategies have very different operational profiles.

</details>

---

## ⚡ Quick Revision

**One-liner:** Strategy defines a family of interchangeable algorithms behind a common interface and lets you swap them at runtime via composition instead of conditionals.

**The whole pattern in one paragraph:** Strategy is a behavioral GoF pattern — *"define a family of algorithms, encapsulate each, make them interchangeable."* You extract each variant of a behavior into its own class implementing a shared **Strategy interface**; a **Context** holds a strategy and delegates to it, depending only on the abstraction so it stays closed for modification but open to new strategies (OCP). It replaces `if/else`/`switch` chains with **polymorphism** and favors **composition over inheritance** (context *has-a* strategy, not *is-a* subclass). Implementations evolve from naive conditionals → interface+classes → **enum** strategies (fixed, stateless, thread-safe singletons) → **lambdas** (functional interfaces, composable, minimal boilerplate) → **DI + registry** (self-registering beans indexed in a `Map<key,strategy>` for O(1) data-driven selection, feature flags, and A/B tests). Prefer **stateless/immutable** strategies so one instance is thread-safe and shareable; avoid replacing one big `switch` with another by using a registry; avoid class explosion for trivial variations by using lambdas or parameters. It's structurally identical to **State** (differs by intent: State transitions modes) and contrasts with **Template Method** (inheritance, fixed skeleton). Real-world: `Comparator`, `ThreadPoolExecutor` rejection policies, Spring's `PlatformTransactionManager`.

**Top 5 answers to memorize:**

1. *What is it?* → A family of interchangeable algorithms behind one interface, swappable at runtime; replaces conditionals with polymorphism (OCP + composition over inheritance).
2. *Strategy vs State?* → Same structure; Strategy = *how* (client picks, independent), State = *mode* (self-transitions, driven by internal state).
3. *How to select without a big switch?* → A registry: strategies self-declare a key and are collected into a `Map<key,strategy>` (via DI); selection is an O(1) lookup.
4. *How to make it thread-safe?* → Make strategies stateless (or immutable) so a single instance is safely shared across threads.
5. *Strategy vs Template Method?* → Composition + runtime swap (Strategy) vs inheritance + fixed skeleton with varying steps (Template Method).

**Trigger words** (hear these → think Strategy): "family of algorithms," "interchangeable," "switch/if-else on a type," "swap behavior at runtime," "payment methods," "sort by / comparator," "pricing/discount rules," "compression formats," "pluggable / plugin," "different algorithm per user/config," "avoid subclass explosion," "composition over inheritance."

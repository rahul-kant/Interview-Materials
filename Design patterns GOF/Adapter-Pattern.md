# Adapter Pattern ⭐⭐⭐ (Difficulty: 3/5 — conceptually simple, but the class-vs-object trade-off and "adapter vs. facade vs. decorator" distinctions run deep)

> **Category:** Structural Pattern (GoF)
> **Also known as:** Wrapper (a name it shares, confusingly, with Decorator)

The Adapter pattern **converts the interface of a class into another interface clients expect.** It's the universal "impedance matcher" of software — the reason a `Scanner` can read from an `InputStream`, an `Arrays.asList` can present an array as a `List`, and a legacy XML service can be dropped behind a modern JSON-based port.

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 0: No Adapter — Shotgun Surgery / If-Else Branching (Anti-pattern)](#variant-0-no-adapter--shotgun-surgery--if-else-branching-anti-pattern)
   - [Variant 1: Object Adapter (composition)](#variant-1-object-adapter-composition)
   - [Variant 2: Class Adapter (inheritance)](#variant-2-class-adapter-inheritance)
   - [Variant 3: Two-Way Adapter](#variant-3-two-way-adapter)
   - [Variant 4: Pluggable / Default-Method Adapter (production)](#variant-4-pluggable--default-method-adapter-production)
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

> **GoF Definition:** *"Convert the interface of a class into another interface clients expect. Adapter lets classes work together that couldn't otherwise because of incompatible interfaces."*

The Adapter pattern exists to resolve **interface incompatibility** between two pieces of code that *should* collaborate but *can't*, because one exposes a shape the other doesn't understand. You have a **client** that speaks in terms of a **target interface**, and an existing **adaptee** whose interface is different (a legacy class, a third-party library, a system component you don't control). The adapter is a thin translation layer that implements the target interface and internally delegates to the adaptee, converting calls, data, and sometimes error models between the two worlds.

The key insight is that Adapter **changes an interface without changing behavior**. It does not add responsibilities (that's Decorator), it does not simplify a subsystem (that's Facade), and it does not control access (that's Proxy). Its sole job is *translation* — making a square peg fit a round hole without reshaping the peg or the hole.

---

## 🎯 Problem

You have working code on both sides, but they can't talk. Rewriting the adaptee is impossible (you don't own it), risky (it's battle-tested legacy), or wasteful (it works fine — it's just shaped wrong). You need a translator.

**The pain points that lead you to Adapter:**

- You want to reuse an **existing class** but its interface doesn't match what your client code requires.
- You're integrating a **third-party library** whose API you can't (or shouldn't) modify, and you don't want its types leaking throughout your codebase.
- You need several **interchangeable classes with inconsistent interfaces** to be usable through one uniform interface (each gets its own adapter).
- You're migrating: a **new abstraction (port)** must sit in front of an **old implementation** so callers can switch later without breaking.

**Concrete example scenarios:**

1. **Payment gateway integration.** Your checkout code calls a clean `PaymentProcessor.pay(amount)` interface, but Stripe's SDK exposes `chargeCard(cents, token, currency)` and PayPal exposes `makePayment(PayPalRequest)`. A `StripeAdapter` and `PayPalAdapter` each implement `PaymentProcessor` and translate to the vendor SDK.

2. **Legacy logging / analytics.** New code logs via SLF4J's `Logger`, but a legacy subsystem exposes `LegacyLog.write(int level, String msg)`. An adapter bridges the two so you don't rewrite the legacy module.

3. **Data-format bridging.** A reporting engine consumes `List<Row>`, but your data arrives as a `ResultSet`, a CSV stream, or a third-party `XmlNode` tree. Each source gets an adapter that presents the common `List<Row>` (or `Iterator<Row>`) shape.

4. **Hexagonal architecture / ports & adapters.** Your domain defines an outbound port `NotificationSender`; infrastructure supplies a Twilio SMS client. A `TwilioNotificationAdapter` implements the port and delegates to Twilio — the entire architectural style is named after this pattern.

---

## ✅ Solution

The core idea, in plain language: **write a new class (the adapter) that implements the interface the client expects (the target), holds or extends the incompatible class (the adaptee), and translates each target method call into one or more adaptee calls** — converting parameters, return types, and error models as needed.

**Key structural elements:**

- **Target** — the interface the client codes against (what the client *expects*). This is the "round hole."
- **Client** — the code that uses objects through the Target interface. It stays blissfully unaware of the adaptee.
- **Adaptee** — the existing class with the incompatible-but-useful interface (the "square peg"). You don't modify it.
- **Adapter** — implements Target and translates its calls into calls on the Adaptee. Two flavors: an **object adapter** *holds* an adaptee (composition/delegation), or a **class adapter** *extends* the adaptee (inheritance, requires multiple inheritance of type — in Java, extend a class and implement the target interface).

**The mechanism that makes it work:** *indirection through interface conformance*. The adapter satisfies the Target type (so the client accepts it via polymorphism) while privately speaking the adaptee's language. The two dominant realizations differ in *how* the adapter reaches the adaptee: the **object adapter** uses **composition** (holds a reference, delegates) and can adapt an adaptee *and all its subclasses*; the **class adapter** uses **inheritance** (subclasses the adaptee) and can override adaptee behavior but is locked to that one concrete adaptee type. Composition is generally preferred (favor composition over inheritance, and Java has no multiple class inheritance), which is why the object adapter is the default answer in interviews.

---

## 💻 Implementation

We'll model a **payment processing** integration — the FAANG-favorite Adapter scenario because vendor SDKs are genuinely incompatible and you genuinely can't modify them. The client wants one clean `PaymentProcessor` interface; each vendor gets an adapter.

### Variant 0: No Adapter — Shotgun Surgery / If-Else Branching (Anti-pattern)

**What's wrong with it:** Without an adapter, the vendor's incompatible API leaks straight into your business logic. You branch on vendor type at every call site, mixing translation concerns with domain logic. Adding a vendor means editing every `if/else` chain (shotgun surgery), and vendor types (`StripeCharge`, `PayPalRequest`) contaminate your entire codebase — violating the Dependency Inversion and Open/Closed principles.

<details>
<summary>💻 Click to expand code — the anti-pattern</summary>

```java
// DON'T DO THIS — vendor APIs leak into business logic, branching everywhere.
class CheckoutService {
    void checkout(String vendor, double amount) {
        if (vendor.equals("stripe")) {
            StripeApi stripe = new StripeApi();
            stripe.chargeInCents((long)(amount * 100), "usd");   // Stripe's shape
        } else if (vendor.equals("paypal")) {
            PayPalApi paypal = new PayPalApi();
            PayPalPayment p = new PayPalPayment();
            p.setTotal(amount);
            p.setCurrency("USD");
            paypal.makePayment(p);                                // PayPal's shape
        }
        // Add Square? Edit this method AND every other place that pays.
    }
}
```
</details>

**Pros:** Nothing to learn; works for exactly one or two vendors and never changing.
**Cons:** Business logic is coupled to concrete vendor SDKs; adding/removing a vendor touches many files; impossible to unit-test checkout without real SDKs; violates OCP and DIP.
**Mechanism (why it fails):** there is no *indirection* — the client depends directly on concrete, incompatible types, so every difference between them surfaces as a conditional.

---

### Variant 1: Object Adapter (composition)

**What problem it solves:** Introduces the Target interface and one adapter per vendor. The adapter *holds* the adaptee and delegates, translating parameters/units/errors. The client now depends only on `PaymentProcessor`. This is the canonical, recommended form.

<details>
<summary>💻 Click to expand code</summary>

```java
// Target — what the client expects
interface PaymentProcessor {
    PaymentResult pay(long amountCents, String currency);
}

record PaymentResult(boolean success, String reference) {}

// Adaptee 1 — third-party SDK we cannot modify
class StripeApi {
    String chargeInCents(long cents, String lowerCaseCurrency) {
        return "stripe_txn_" + cents;   // returns a raw txn id
    }
}

// Adaptee 2 — another vendor with a totally different shape
class PayPalApi {
    static class PayPalPayment { double total; String currency; }
    boolean makePayment(PayPalPayment p) { return p.total > 0; }
}

// Object Adapter for Stripe — HOLDS the adaptee (composition)
class StripePaymentAdapter implements PaymentProcessor {
    private final StripeApi stripe;                       // composition
    StripePaymentAdapter(StripeApi stripe) { this.stripe = stripe; }

    @Override public PaymentResult pay(long amountCents, String currency) {
        String txn = stripe.chargeInCents(amountCents, currency.toLowerCase()); // translate
        return new PaymentResult(txn != null, txn);                             // adapt return
    }
}

// Object Adapter for PayPal — different translation, same Target
class PayPalPaymentAdapter implements PaymentProcessor {
    private final PayPalApi paypal;
    PayPalPaymentAdapter(PayPalApi paypal) { this.paypal = paypal; }

    @Override public PaymentResult pay(long amountCents, String currency) {
        var p = new PayPalApi.PayPalPayment();
        p.total = amountCents / 100.0;          // unit conversion: cents -> dollars
        p.currency = currency.toUpperCase();    // format conversion
        boolean ok = paypal.makePayment(p);
        return new PaymentResult(ok, ok ? "paypal_ok" : "paypal_fail");
    }
}

// Client depends ONLY on the Target
class Checkout {
    void charge(PaymentProcessor processor) {
        PaymentResult r = processor.pay(1999, "USD");
        System.out.println(r.success() + " / " + r.reference());
    }
}
```
</details>

**Pros:** Client fully decoupled from vendors; add a vendor = one new adapter class (OCP); one adapter can adapt the adaptee *and its subclasses*; easy to mock the Target in tests.
**Cons:** One extra class per adaptee; a small amount of delegation code; can't override adaptee internals (it's held, not extended).
**Mechanism:** *composition + delegation*. The adapter satisfies `PaymentProcessor` polymorphically while privately calling the vendor SDK; unit conversion (cents↔dollars), case conversion, and return-type mapping all happen inside `pay()`.

---

### Variant 2: Class Adapter (inheritance)

**What problem it solves:** When you *want to override or specialize* the adaptee's behavior, or the adaptee is a class you can extend, the class adapter subclasses the adaptee and implements the Target. In Java (no multiple class inheritance) this means `extends Adaptee implements Target`. It avoids a wrapper object and lets you call `super`. A classic FAANG scenario: your new UI code expects a modern `Shape.draw(x, y, width, height)` interface, but you have a battle-tested `LegacyRectangle` class whose method signature and coordinate model are different — and you can't rewrite it.

<details>
<summary>💻 Click to expand code</summary>

```java
// Target — what the new drawing code expects
interface Shape {
    void draw(int x1, int y1, int x2, int y2);   // two corner points
}

// Adaptee — a proven legacy class we can't change. It uses (x, y, width, height)
// and its own method name. We WANT to reuse its rendering logic as-is.
class LegacyRectangle {
    void drawRectangle(int x, int y, int width, int height) {
        System.out.println("LegacyRectangle: drawing at (" + x + "," + y + ")"
                + " size " + width + "x" + height);
    }
}

// Class Adapter: extend the adaptee, implement the Target, translate the call.
class RectangleAdapter extends LegacyRectangle implements Shape {
    @Override
    public void draw(int x1, int y1, int x2, int y2) {
        int x = Math.min(x1, x2);
        int y = Math.min(y1, y2);
        int width = Math.abs(x2 - x1);
        int height = Math.abs(y2 - y1);
        super.drawRectangle(x, y, width, height);   // reuse legacy logic via super
    }
}

// Usage — new code talks to the Shape target; legacy rendering runs underneath.
Shape shape = new RectangleAdapter();
shape.draw(10, 20, 40, 60);
// LegacyRectangle: drawing at (10,20) size 30x40
```
</details>

**Pros:** No separate wrapper object (the adapter *is* the adaptee); can override adaptee methods and reuse them via `super`; slightly less indirection.
**Cons:** Locks the adapter to **one concrete adaptee class** (can't adapt its subclasses); exposes the *entire adaptee interface* to clients (leaky — a client could call `drawRectangle` directly, bypassing the translation); impossible if the adaptee is `final`; couples tightly via inheritance; in Java you can implement multiple target interfaces but extend only one adaptee.
**Mechanism:** *inheritance of implementation*. Because the adapter IS-A adaptee and IS-A target simultaneously, method mapping is done by overriding; `super.drawRectangle(...)` reuses legacy behavior directly instead of delegating to a held reference — after translating the two-corner-point model into the legacy (x, y, width, height) model.

---

### Variant 3: Two-Way Adapter

**What problem it solves:** Sometimes both sides need to see the object as *their own* type — old code needs the legacy interface, new code needs the modern one, simultaneously. A two-way adapter implements **both** interfaces, so it's transparent in either direction.

<details>
<summary>💻 Click to expand code</summary>

```java
// Two incompatible interfaces for the same concept: a point/coordinate
interface CartesianPoint { double getX(); double getY(); }
interface PolarPoint     { double getRadius(); double getAngle(); }

// A two-way adapter IS both a CartesianPoint AND a PolarPoint.
class PointAdapter implements CartesianPoint, PolarPoint {
    private final double x, y;
    PointAdapter(double x, double y) { this.x = x; this.y = y; }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return Math.hypot(x, y); }
    public double getAngle()  { return Math.atan2(y, x); }
}

// Legacy code that wants Cartesian and new code that wants Polar both accept the SAME object.
void legacy(CartesianPoint p) { /* ... */ }
void modern(PolarPoint p)     { /* ... */ }
PointAdapter pt = new PointAdapter(3, 4);
// legacy(pt); modern(pt);  // works both ways
```
</details>

**Pros:** One object usable by clients expecting either interface; great for gradual migrations where both APIs coexist.
**Cons:** The adapter must fully honor two contracts, which can be hard if the interfaces have conflicting semantics; risks becoming a god-object if overused.
**Mechanism:** *multiple interface conformance*. Because Java allows implementing many interfaces, the single object presents two faces; each interface's methods are computed from the same underlying state.

---

### Variant 4: Pluggable / Default-Method Adapter (production)

**Why it's recommended:** In real systems you often adapt *many* sources to one Target, and some sources only support *part* of the Target. A "pluggable adapter" (GoF term) parameterizes the adaptation — via a lambda/strategy or via interface `default` methods that provide no-op/sensible fallbacks so concrete adapters override only what they can support. A production scenario every FAANG engineer knows is **multi-cloud storage** (S3 / GCS / Azure Blob): AWS's SDK exposes a `TransferListener` with empty `default` upload-progress callbacks (override only what you care about), and you adapt each provider's *different* upload-response object to one uniform `UploadResult` — with a lambda per provider instead of a hand-written adapter class each.

<details>
<summary>💻 Click to expand code — default-method progress listener + functional per-provider upload adapter</summary>

```java
import java.util.function.Function;

// =====================================================================
// Approach A: default-method adapter — "override only what you need".
// This is EXACTLY how the AWS SDK's TransferListener works: a wide callback
// interface with empty defaults so you implement only the events you care about.
// =====================================================================
interface TransferListener {
    default void onStart(String key)                 {}  // empty defaults =>
    default void onBytesTransferred(long bytes)      {}  // override ONLY what you need
    default void onComplete(String key)              {}
    default void onFailure(String key, Exception e)  {}
}

// A real listener that only cares about failures — no boilerplate for the rest.
class FailureAlertListener implements TransferListener {
    @Override public void onFailure(String key, Exception e) {
        System.out.println("ALERT: upload of " + key + " failed: " + e.getMessage());
    }
}

// =====================================================================
// Approach B: pluggable/functional adapter — one uniform result, many clouds.
// Each cloud SDK returns a DIFFERENT upload-response shape (the adaptees);
// our app wants ONE UploadResult (the target). Instead of an S3Adapter +
// GcsAdapter CLASS per provider, we pass a lambda that maps response -> result.
// =====================================================================
record UploadResult(String url, long sizeBytes) {}   // Target shape our app uses

// Two providers, two totally different SDK response shapes.
record S3Response(String bucket, String objectKey, long contentLength) {}
record GcsResponse(String selfLink, long size) {}

// One reusable adapter. The lambda says HOW to map this provider's response.
class UploadAdapter<T> {
    private final T sdkResponse;
    private final Function<T, UploadResult> howToConvert;   // the plugged-in translation
    UploadAdapter(T sdkResponse, Function<T, UploadResult> howToConvert) {
        this.sdkResponse = sdkResponse;
        this.howToConvert = howToConvert;
    }
    UploadResult toResult() { return howToConvert.apply(sdkResponse); }
}

class Demo {
    public static void main(String[] args) {
        // Approach A: listener that only handles failures; defaults cover the rest.
        TransferListener listener = new FailureAlertListener();
        listener.onStart("report.pdf");                                  // no-op default
        listener.onFailure("report.pdf", new RuntimeException("timeout")); // ALERT: ...

        // Approach B: adapt each cloud's response to our UploadResult with one lambda.
        var s3 = new UploadAdapter<>(
                new S3Response("my-bucket", "report.pdf", 2048),
                r -> new UploadResult("https://s3/" + r.bucket() + "/" + r.objectKey(),
                                      r.contentLength()));

        var gcs = new UploadAdapter<>(
                new GcsResponse("https://storage.googleapis.com/report.pdf", 4096),
                r -> new UploadResult(r.selfLink(), r.size()));

        System.out.println(s3.toResult());   // UploadResult[url=https://s3/my-bucket/report.pdf, sizeBytes=2048]
        System.out.println(gcs.toResult());  // UploadResult[url=https://storage.googleapis.com/report.pdf, sizeBytes=4096]
    }
}
```
</details>

**Pros:** `default`-method adapters let you implement a wide Target interface while overriding only the one or two methods you care about (no empty-method boilerplate — exactly why the AWS `TransferListener` uses them); the functional upload adapter lets you support many cloud providers (S3, GCS, Azure, …) with a one-line lambda instead of a new adapter class per provider.
**Cons:** `default` no-ops can silently swallow an event you *meant* to handle (a forgotten `onFailure` override looks identical to a deliberate no-op); the functional adapter shows lambdas instead of descriptive class names in stack traces.
**Mechanism:** *interface default methods* supply a ready-made "do-nothing" base so an implementation is minimal; the *functional* variant captures the adaptee→target conversion in a lambda (a closure), so the "how to translate this provider's response" step becomes a value you pass in rather than a class you write.

---

## 🎨 Real-World Example

A definitive real-world Adapter is the **hexagonal-architecture outbound port**: your domain defines a clean `NotificationSender` port; each infrastructure provider (Twilio SMS, SendGrid email, Slack webhook) has a totally different SDK, and each gets an adapter. Below is a **complete, production-realistic example** — the exact shape used in FAANG service tiers — including a demo that swaps providers at runtime and shows unit/error-model translation.

<details>
<summary>💻 Click to expand full real-world example (multi-provider notification adapters + demo)</summary>

```java
import java.util.*;

// ===== Domain layer: the Target port. Domain code depends ONLY on this. =====
interface NotificationSender {
    /** Uniform contract: returns a provider-agnostic receipt or throws NotificationException. */
    Receipt send(Notification n) throws NotificationException;
}

record Notification(String to, String subject, String body) {}
record Receipt(String providerId, String messageId, long timestampMillis) {}

class NotificationException extends Exception {
    NotificationException(String msg, Throwable cause) { super(msg, cause); }
}

// ===== Adaptee 1: Twilio-style SMS SDK (we can't modify it) =====
class TwilioSmsClient {
    // Twilio speaks phone numbers and returns its own Message object; throws TwilioException.
    Message messages_create(String toNumber, String text) throws TwilioException {
        if (!toNumber.startsWith("+")) throw new TwilioException("E.164 number required");
        return new Message("SM" + Math.abs(text.hashCode()));
    }
    static class Message {
        final String sid;
        Message(String sid) { this.sid = sid; }
    }

    static class TwilioException extends RuntimeException {
        TwilioException(String m) { super(m); }
    }
}

// ===== Adaptee 2: SendGrid-style email SDK (different shape entirely) =====
class SendGridClient {
    int dispatch(Map<String,String> payload) {  // returns an HTTP-ish status code
        return payload.containsKey("to") ? 202 : 400;
    }
}

// ===== Adapter 1: Twilio -> NotificationSender =====
class TwilioNotificationAdapter implements NotificationSender {
    private final TwilioSmsClient twilio;
    TwilioNotificationAdapter(TwilioSmsClient twilio) { this.twilio = twilio; }

    @Override public Receipt send(Notification n) throws NotificationException {
        try {
            // Translate: our "to" is a phone number; SMS ignores subject; body -> text.
            var msg = twilio.messages_create(n.to(), n.body());
            return new Receipt("twilio", msg.sid, System.currentTimeMillis());
        } catch (TwilioSmsClient.TwilioException e) {          // adapt the ERROR MODEL
            throw new NotificationException("Twilio send failed", e);
        }
    }
}

// ===== Adapter 2: SendGrid -> NotificationSender =====
class SendGridNotificationAdapter implements NotificationSender {
    private final SendGridClient sendgrid;
    SendGridNotificationAdapter(SendGridClient sendgrid) { this.sendgrid = sendgrid; }

    @Override public Receipt send(Notification n) throws NotificationException {
        Map<String,String> payload = new HashMap<>();          // translate to SDK's shape
        payload.put("to", n.to());
        payload.put("subject", n.subject());
        payload.put("content", n.body());
        int code = sendgrid.dispatch(payload);
        if (code != 202) throw new NotificationException("SendGrid HTTP " + code, null);
        return new Receipt("sendgrid", "sg-" + code + "-" + n.hashCode(), System.currentTimeMillis());
    }
}

// ===== Client / demo: domain code is provider-agnostic; providers swap at runtime =====
public class NotificationDemo {
    // Depends on the port, not on any vendor — the whole point of Adapter.
    static void notifyUser(NotificationSender sender, Notification n) {
        try {
            Receipt r = sender.send(n);
            System.out.printf("sent via %s, id=%s%n", r.providerId(), r.messageId());
        } catch (NotificationException e) {
            System.out.println("FAILED: " + e.getMessage() + " (cause: " + e.getCause() + ")");
        }
    }

    public static void main(String[] args) {
        List<NotificationSender> providers = List.of(
            new TwilioNotificationAdapter(new TwilioSmsClient()),
            new SendGridNotificationAdapter(new SendGridClient())
        );

        // Call site 1: valid email-style notification through both providers.
        Notification ok = new Notification("+14155550100", "Hi", "Your order shipped");
        providers.forEach(p -> notifyUser(p, ok));

        // Call site 2: bad phone number — Twilio's TwilioException is translated into
        // our uniform NotificationException so the client never sees vendor error types.
        Notification badForSms = new Notification("4155550100", "Hi", "no plus sign");
        notifyUser(new TwilioNotificationAdapter(new TwilioSmsClient()), badForSms);
        // Output: FAILED: Twilio send failed (cause: ...E.164 number required)
    }
}
```
</details>

**Why this is a strong FAANG answer:** it shows (1) the Target port defined by the *domain*, not the vendor (Dependency Inversion), (2) real translation work — units/fields, ignoring irrelevant fields like `subject` for SMS, mapping return types to a uniform `Receipt`, (3) crucially, **error-model adaptation** — vendor exceptions are caught and re-thrown as the domain's `NotificationException`, so vendor types never leak, and (4) runtime provider swapping with zero client changes, which is exactly what ports-and-adapters buys you.

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- You want to **reuse an existing class** whose interface doesn't match what your client code needs, and you can't or shouldn't change either side.
- You're integrating a **third-party library or legacy system** and want to isolate its API behind your own interface so its types don't leak everywhere.
- You need **several interchangeable but inconsistent classes** to be usable through one uniform interface (each variant gets its own adapter).
- You're doing a **migration** — a new abstraction/port must front an old implementation so callers can switch later.
- You're building **hexagonal / ports-and-adapters** architecture and need infrastructure to satisfy domain-defined ports.
- You need to convert **data formats or error models** (units, encodings, exception types) at a boundary.

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- **You control both sides and they're not yet released.** Just fix the interface directly instead of adding a permanent translation layer.
- **The mismatch is trivial or cosmetic.** A one-line method rename doesn't justify a whole class; refactor instead.
- **You actually need to add behavior** (that's Decorator), **simplify a complex subsystem** (that's Facade), or **control access** (that's Proxy). Using "adapter" as a catch-all wrapper name blurs intent.
- **The two interfaces are semantically irreconcilable.** If a Target method has no meaningful mapping to the adaptee (e.g., `rewind()` on a forward-only stream), an adapter can only fake it, throw, or lie — a design smell.
- **Performance-critical hot paths** where the extra indirection/allocation per call matters and the adaptation could be pushed to a build-time codegen instead.
- **Too many adapters signal a deeper problem.** If every integration needs a thick adapter, your Target interface may be wrong or too vendor-shaped.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- **Reuse** existing/legacy/third-party code without modifying it.
- **Decouples** the client from concrete adaptee types (Dependency Inversion) — vendor types stay contained.
- Honors **Single Responsibility** (translation logic lives in one place) and **Open/Closed** (add a new adapter without touching clients).
- Enables **runtime substitution** of interchangeable implementations behind one interface.
- Object adapter can adapt an **adaptee and all its subclasses**.

**Cons**

- Adds a layer of **indirection** — more classes, slight runtime overhead.
- Can hide the fact that two systems are a **poor conceptual fit**; adapters can accumulate "translation debt."
- **Class adapter** leaks the whole adaptee interface and locks to one concrete type; can't adapt subclasses.
- Complex adaptation (stateful, lossy, or error-prone mappings) can make the adapter itself a bug-prone hotspot.
- Overuse as a generic "wrapper" **blurs pattern intent** (Adapter vs. Decorator vs. Facade vs. Proxy).

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

All of these wrap another object; the difference is **intent** — the single most common Adapter interview trap.

| Pattern | Intent | Interface vs. wrapped | Adds behavior? | Key tell |
|---|---|---|---|---|
| **Adapter** | **Convert** an interface to another clients expect | **Different** (that's the point) | No | Makes incompatible things work together |
| **Decorator** | Add responsibilities transparently | **Same** interface | **Yes** | Enhances, keeps the type, stackable |
| **Facade** | Provide a **simplified** interface to a whole subsystem | New, simpler interface over **many** classes | No (simplifies) | One entry point over a complex subsystem |
| **Proxy** | **Control access** to an object (lazy/remote/security) | **Same** interface | No | Same interface, gates access |
| **Bridge** | **Decouple** abstraction from implementation (designed up front) | Two parallel hierarchies | No | Planned separation, not retrofitted |
| **Mediator** | Centralize how objects interact | New coordination interface | N/A | Manages many-to-many communication |

Concise contrasts: **Adapter vs. Decorator** — Decorator keeps the *same* interface and *adds* behavior; Adapter deliberately *changes* the interface and adds *no* behavior (both are sometimes called "Wrapper," which is why intent is the deciding factor). **Adapter vs. Facade** — a Facade defines a *new, simpler* interface over a *whole subsystem* to reduce complexity; an Adapter matches an *existing, required* interface for a *single* adaptee to fix incompatibility. **Adapter vs. Bridge** — Bridge is designed *before* the fact to let abstraction and implementation vary independently; Adapter is applied *after* the fact to make unrelated existing things cooperate. **Adapter vs. Proxy** — Proxy keeps the same interface and controls *when/whether* you reach the target; Adapter changes the interface and always delegates.

</details>

## 📊 Comparison Table (of Variants)

<details>
<summary>📖 Click to expand</summary>

| Axis | V0: No Adapter | V1: Object Adapter | V2: Class Adapter | V3: Two-Way | V4: Pluggable/Default |
|---|---|---|---|---|---|
| Mechanism | if/else branching | composition + delegation | inheritance | multi-interface impl | default methods / closure |
| Client decoupled from adaptee | ❌ No | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| Adapts adaptee's subclasses too | N/A | ✅ Yes | ❌ No (one concrete class) | Depends | ✅ Yes |
| Can override adaptee behavior | N/A | ❌ No (held) | ✅ Yes (`super`) | Partial | ❌ No |
| Works if adaptee is `final` | N/A | ✅ Yes | ❌ No | ✅ Yes | ✅ Yes |
| Leaks full adaptee interface | ✅ (everywhere) | ❌ No | ⚠️ Yes | ❌ No | ❌ No |
| Extra classes per adaptee | 0 | 1 | 1 | 1 | 0–1 (reusable) |
| Idiomatic use case | Never | **Default choice** | Need to override adaptee | Coexisting old+new APIs | Wide/partial interfaces, many one-offs |

</details>

---

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

### Pitfall 1: Leaky adapter — the adaptee's types/exceptions escape

**What goes wrong:** The adapter faithfully implements the Target's *method signatures* but lets the adaptee's return types or exceptions bleed through, so clients still end up coupled to the vendor. The decoupling benefit evaporates.

<details>
<summary>💻 Click to expand — the failure</summary>

```java
// BUG: Target method returns our type, but throws the VENDOR's exception.
interface PaymentProcessor { PaymentResult pay(long cents, String cur); }

class LeakyStripeAdapter implements PaymentProcessor {
    private final StripeApi stripe = new StripeApi();
    public PaymentResult pay(long cents, String cur) {
        // StripeException is a vendor type — now the client must catch/handle it. Leak!
        String txn = stripe.chargeInCents(cents, cur); // may throw StripeException
        return new PaymentResult(true, txn);
    }
}
```
</details>

<details>
<summary>💻 Click to expand — the fix</summary>

```java
// FIX: translate the error model too — catch vendor exceptions, rethrow domain ones.
class CleanStripeAdapter implements PaymentProcessor {
    private final StripeApi stripe = new StripeApi();
    public PaymentResult pay(long cents, String cur) {
        try {
            String txn = stripe.chargeInCents(cents, cur.toLowerCase());
            return new PaymentResult(txn != null, txn);
        } catch (StripeApi.StripeException e) {
            throw new PaymentException("payment failed", e); // domain type; cause preserved
        }
    }
}
```
</details>

### Pitfall 2: Lossy or fake translation for unmappable methods

**What goes wrong:** The Target has a method the adaptee simply can't support. Developers "fill it in" with a silent no-op, a wrong default, or a lie — producing subtle correctness bugs instead of an honest failure.

<details>
<summary>💻 Click to expand — the failure</summary>

```java
interface SeekableSource { int read(); void rewind(); }

// Adaptee is a forward-only network stream — it CANNOT rewind.
class StreamAdapter implements SeekableSource {
    private final java.io.InputStream in;
    StreamAdapter(java.io.InputStream in) { this.in = in; }
    public int read() { try { return in.read(); } catch (Exception e) { return -1; } }
    public void rewind() { /* silently does nothing — callers think it worked! */ }
}
```
</details>

<details>
<summary>💻 Click to expand — the fix</summary>

```java
// FIX: fail loudly (UnsupportedOperationException) OR add real buffering to honor the contract.
public void rewind() {
    throw new UnsupportedOperationException("underlying stream is forward-only");
}
// Better: if rewind is truly required, buffer bytes so the contract can actually be met.
```
</details>

### Pitfall 3: Confusing Adapter with Decorator/Facade — wrong pattern, wrong shape

**What goes wrong:** Calling everything a "wrapper" leads to building an Adapter when you needed a Decorator (add behavior, same interface) or a Facade (simplify a subsystem). The result is a class with a confused responsibility.

<details>
<summary>💻 Click to expand — the smell and the fix</summary>

```java
// SMELL: an "adapter" that keeps the SAME interface but ALSO adds logging + retries.
// That's a Decorator wearing an Adapter's name. Keep responsibilities honest:
//  - Different target interface + no new behavior      -> Adapter
//  - Same interface + added behavior (logging/caching) -> Decorator
//  - New simpler API over many subsystem classes       -> Facade
// If you're doing two of these, use two classes (e.g., Decorator around an Adapter).
```
</details>

### Pitfall 4: Stateful / non-thread-safe adapters

**What goes wrong:** An adapter that caches translated state or buffers data becomes a shared mutable object; used across threads without synchronization it corrupts state. Adapters are often assumed to be cheap stateless translators — a stateful one breaks that assumption silently.

<details>
<summary>💻 Click to expand — the fix</summary>

```java
// FIX: keep adapters stateless where possible; if state is required, make it thread-safe.
class IteratorEnumerationAdapter<T> implements java.util.Enumeration<T> {
    private final java.util.Iterator<T> it;                 // adaptee holds the state
    IteratorEnumerationAdapter(java.util.Iterator<T> it) { this.it = it; }
    public boolean hasMoreElements() { return it.hasNext(); } // pure delegation, no own state
    public T nextElement() { return it.next(); }
    // If shared across threads, guard the underlying iterator or confine to one thread.
}
```
</details>

</details>

## 🎓 Interview Tips

**What interviewers commonly ask:**

- "You need to integrate a third-party payment/SMS/logging SDK behind your own interface — design it." (The tell for Adapter.)
- "Difference between **object adapter and class adapter**?" (Composition vs. inheritance; know all trade-offs in the table.)
- "Adapter vs. Decorator vs. Facade vs. Proxy?" (Intent-based — the #1 discriminator question.)
- "Give real JDK examples." (`Arrays.asList`, `Collections.list`/`enumeration`, `InputStreamReader`, `java.io.Reader` bridges.)
- "Why is the object adapter usually preferred in Java?" (No multiple class inheritance; favor composition; adapts subclasses; works with `final` adaptees.)

**What you should proactively mention even if not asked:**

- **Adapter changes the interface but adds no behavior** — state this crisply to distinguish it from Decorator.
- The **object adapter is the default**; reach for a class adapter only when you must override adaptee behavior.
- A good adapter also **translates the error model** (vendor exceptions → domain exceptions), not just method names.
- Adapter is the concrete mechanism behind **hexagonal / ports-and-adapters** architecture and Dependency Inversion.
- `InputStreamReader` is a great example that is *both* an adapter (bytes→chars, interface change) and sits in a decorator chain — shows nuanced understanding.
- Mention the **pluggable adapter** (`default` methods, e.g., Swing's `MouseAdapter`) as the idiom for wide/partially-supported interfaces.
- Beware **lossy translation**: fail loudly rather than fake unsupported operations.

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Decorator** — also wraps and shares the "Wrapper" nickname, but keeps the *same* interface and *adds* behavior instead of converting.
- **Facade** — defines a new, simpler interface over an entire subsystem; Adapter matches one specific required interface for one adaptee.
- **Proxy** — same interface as the subject; controls access rather than converting the interface.
- **Bridge** — separates abstraction from implementation *by design, up front*; Adapter reconciles incompatible interfaces *after the fact*.
- **Strategy** — an adapter can wrap a third-party algorithm so it conforms to your `Strategy` interface.
- **Mediator** — for many-to-many interaction; sometimes adapters normalize participants before a mediator coordinates them.
- **Ports & Adapters (Hexagonal Architecture)** — an architectural style built entirely on this pattern: domain ports + infrastructure adapters.

</details>

## 📚 Library/Framework Implementation

**1. `java.util.Arrays.asList` and `Collections.list`/`enumeration` (JDK collections bridges).** `Arrays.asList(T[])` adapts a raw array to the `List` interface (a fixed-size view). `Collections.list(Enumeration)` adapts the legacy `Enumeration` to a `List`, and `Collections.enumeration(Collection)` adapts the modern `Iterator`/`Collection` world back to legacy `Enumeration` — a two-way bridge across a JDK-generation gap.

<details>
<summary>💻 Click to expand — JDK collection adapters</summary>

```java
// Array -> List (Adapter: array interface -> List interface, fixed-size view)
Integer[] arr = {1, 2, 3};
java.util.List<Integer> list = java.util.Arrays.asList(arr);

// Legacy Enumeration <-> modern Iterator/Collection (two-way adapters)
java.util.Vector<String> v = new java.util.Vector<>(java.util.List.of("a","b"));
java.util.Enumeration<String> e = v.elements();
java.util.List<String> back = java.util.Collections.list(e);      // Enumeration -> List
java.util.Enumeration<String> e2 =
        java.util.Collections.enumeration(java.util.List.of("x")); // Collection -> Enumeration
```
</details>

**2. `java.io.InputStreamReader` / `OutputStreamWriter` (byte↔char bridge).** These adapt the *byte* world (`InputStream`) to the *character* world (`Reader`), applying a charset. It's the textbook example of an adapter that changes the fundamental data unit of the interface — and it commonly participates in a decorator chain too.

<details>
<summary>💻 Click to expand — InputStreamReader as adapter</summary>

```java
// Adapts an InputStream (bytes) to a Reader (chars) — interface + data-unit conversion.
try (java.io.Reader reader = new java.io.InputStreamReader(
                                 System.in, java.nio.charset.StandardCharsets.UTF_8)) {
    int c = reader.read(); // now reading CHARS, not bytes
}
```
</details>

**3. Spring `HandlerAdapter` / `MVC`.** Spring MVC's `DispatcherServlet` doesn't know how to invoke the many kinds of handlers (`@Controller` methods, `HttpRequestHandler`, functional endpoints). Each is invoked through a `HandlerAdapter` implementation that adapts the handler's specific calling convention to the uniform `handle(request, response, handler)` contract the dispatcher expects. Spring also offers many `*Adapter` base classes (e.g., `WebMvcConfigurerAdapter` historically) using the default-method/abstract-adapter idiom.

<details>
<summary>💻 Click to expand — Spring HandlerAdapter shape</summary>

```java
// Simplified: DispatcherServlet iterates HandlerAdapters, using the one that "supports" the handler.
public interface HandlerAdapter {
    boolean supports(Object handler);
    ModelAndView handle(HttpServletRequest req, HttpServletResponse res, Object handler)
            throws Exception;          // uniform Target the dispatcher calls
}
// RequestMappingHandlerAdapter adapts @RequestMapping methods to this contract via reflection.
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1: [Conceptual] What is the Adapter pattern and what problem does it solve?</strong></summary>

The Adapter is a **structural** GoF pattern that **converts the interface of a class into another interface clients expect**, letting classes with incompatible interfaces work together. The problem it solves is **interface incompatibility**: you have a client coded against a *target interface* and an existing *adaptee* (legacy class, third-party SDK, system component) whose interface is different and which you can't or shouldn't modify.

The adapter is a thin translation layer that implements the target interface and delegates to the adaptee, converting method names, parameters, data formats, and error models. Its defining characteristic is that it **changes the interface without changing behavior** — it does not add features (Decorator), simplify a subsystem (Facade), or control access (Proxy). The metaphor is a power-plug adapter: it reshapes the connection without altering the electricity.
</details>

<details>
<summary><strong>Q2: [Conceptual] Name the participants in the Adapter pattern.</strong></summary>

Four participants:

1. **Target** — the interface the client expects and codes against (the "round hole").
2. **Client** — the code that uses objects via the Target interface; unaware of the adaptee.
3. **Adaptee** — the existing class with the incompatible-but-useful interface (the "square peg"); left unmodified.
4. **Adapter** — implements Target and translates its calls into calls on the Adaptee.

There are two structural realizations: the **object adapter** (holds an adaptee via composition and delegates) and the **class adapter** (subclasses the adaptee and implements the target via inheritance).
</details>

<details>
<summary><strong>Q3: [Conceptual] Object adapter vs. class adapter — what's the difference?</strong></summary>

- **Object adapter** uses **composition**: it *holds a reference* to the adaptee and delegates. It can adapt the adaptee *and all its subclasses*, works even if the adaptee is `final`, and keeps the adaptee's interface hidden. It's the default choice.
- **Class adapter** uses **inheritance**: it *extends* the adaptee and implements the target. It can **override** adaptee methods and call `super`, and needs no wrapper object — but it's locked to **one concrete adaptee class**, exposes the entire adaptee interface, and is impossible if the adaptee is `final`. In Java, since there's no multiple class inheritance, a class adapter can `extends` only one adaptee (though it may `implements` multiple targets).

Interview one-liner: prefer the **object adapter** (composition over inheritance, more flexible); use a class adapter only when you specifically need to override adaptee behavior.
</details>

<details>
<summary><strong>Q4: [Conceptual] Give real JDK/framework examples of Adapter.</strong></summary>

- `java.util.Arrays.asList(T[])` — adapts an array to the `List` interface.
- `Collections.list(Enumeration)` and `Collections.enumeration(Collection)` — adapt between legacy `Enumeration` and modern `Iterator`/`Collection`.
- `java.io.InputStreamReader` / `OutputStreamWriter` — adapt byte streams to character streams (data-unit conversion).
- Swing's `MouseAdapter`, `KeyAdapter`, `WindowAdapter` — default/abstract adapters for wide listener interfaces.
- Spring MVC `HandlerAdapter` — adapts diverse handler types to the dispatcher's uniform contract.
- `java.util.concurrent.Executors.callable(Runnable)` — adapts a `Runnable` to a `Callable`.
</details>

<details>
<summary><strong>Q5: [Conceptual] Why is Adapter central to hexagonal (ports-and-adapters) architecture?</strong></summary>

Hexagonal architecture defines **ports** (interfaces owned by the domain) and **adapters** (infrastructure implementations of those ports). The domain depends only on its ports, never on concrete infrastructure — a direct application of the **Dependency Inversion Principle**. An outbound port like `NotificationSender` is a Target; a `TwilioNotificationAdapter` is an Adapter that translates the port's calls to the Twilio SDK.

This lets you swap databases, message brokers, or vendors by writing a new adapter, with zero changes to domain code, and lets you test the domain against in-memory adapters. The entire architectural style is essentially "the Adapter pattern applied at system boundaries," which is why interviewers love connecting the two.
</details>

<details>
<summary><strong>Q6: [Implementation] Implement an object adapter to make a third-party <code>Temperature</code> sensor fit your <code>WeatherReading</code> interface.</strong></summary>

Classic translation including unit conversion.

<details>
<summary>💻 Click to expand solution</summary>

```java
// Target the client wants
interface WeatherReading { double celsius(); }

// Adaptee: third-party sensor reports Fahrenheit via an odd method name
class FahrenheitSensor { double readF() { return 98.6; } }

// Object adapter: holds adaptee, converts units
class SensorAdapter implements WeatherReading {
    private final FahrenheitSensor sensor;
    SensorAdapter(FahrenheitSensor sensor) { this.sensor = sensor; }
    @Override public double celsius() {
        return (sensor.readF() - 32) * 5.0 / 9.0;   // unit + method-name translation
    }
}

// Client
WeatherReading r = new SensorAdapter(new FahrenheitSensor());
System.out.printf("%.1f C%n", r.celsius()); // 37.0 C
```
</details>

Points to verbalize: composition (holds `sensor`), the adapter implements the Target so the client is decoupled, and the real work is unit conversion inside the single mapped method.
</details>

<details>
<summary><strong>Q7: [Implementation] Implement a class adapter and explain when you'd choose it.</strong></summary>

Choose a class adapter when you need to **override or reuse** adaptee behavior via `super`.

<details>
<summary>💻 Click to expand solution</summary>

```java
interface Stack<T> { void push(T x); T pop(); boolean isEmpty(); }

// Class adapter: extend the adaptee (ArrayDeque), implement the target (Stack)
class DequeStack<T> extends java.util.ArrayDeque<T> implements Stack<T> {
    @Override public void push(T x)    { super.addFirst(x); }  // reuse adaptee via super
    @Override public T pop()           { return super.removeFirst(); }
    @Override public boolean isEmpty() { return super.isEmpty(); }
}
```
</details>

Trade-off to state: this is bound to `ArrayDeque` specifically and leaks all of `ArrayDeque`'s methods (a client could call `addLast`), so it's less encapsulated than an object adapter. Use it only when overriding adaptee behavior is the goal; otherwise prefer composition.
</details>

<details>
<summary><strong>Q8: [Implementation] Implement a two-way adapter and give a use case.</strong></summary>

A two-way adapter implements **both** interfaces so a single object is usable by clients of either — ideal for gradual migration where old and new APIs coexist.

<details>
<summary>💻 Click to expand solution</summary>

```java
interface LegacyLogger { void log(int level, String msg); }
interface Slf4jLike    { void info(String msg); void error(String msg); }

// Implements BOTH: legacy code and new code can each use it as their own type.
class LoggerBridge implements LegacyLogger, Slf4jLike {
    public void log(int level, String msg) { route(level, msg); }
    public void info(String msg)  { route(1, msg); }
    public void error(String msg) { route(3, msg); }
    private void route(int level, String msg) {
        System.out.println("[" + level + "] " + msg);
    }
}
```
</details>

Use case: migrating a large codebase from a legacy logger to an SLF4J-style API — modules can adopt the new interface incrementally while legacy call sites keep working, all backed by one object.
</details>

<details>
<summary><strong>Q9: [Implementation] Adapt a <code>Runnable</code> to a <code>Callable&lt;T&gt;</code> without writing a named class.</strong></summary>

The JDK already does this (`Executors.callable`), but implementing it shows you understand functional adapters.

<details>
<summary>💻 Click to expand solution</summary>

```java
import java.util.concurrent.Callable;

// Adapter as a factory method returning a lambda — no named class needed.
static <T> Callable<T> adapt(Runnable r, T result) {
    return () -> { r.run(); return result; };   // translate: run() -> call() returning result
}

// Usage
Runnable task = () -> System.out.println("working");
Callable<String> c = adapt(task, "done");
// c.call() prints "working" and returns "done"
```
</details>

This is exactly what `java.util.concurrent.Executors.callable(Runnable, T)` returns. The Target (`Callable`) has a return value the adaptee (`Runnable`) lacks, so the adapter supplies a fixed result — an honest, documented mapping rather than a fake.
</details>

<details>
<summary><strong>Q10: [Breaking] How can a "correct-looking" adapter still leak the adaptee?</strong></summary>

The most common leak is the **error model**: the adapter maps method names and return types but lets the adaptee's *exceptions* propagate. Clients then must import and catch vendor exception types, re-coupling them to the adaptee. Other leaks: returning the adaptee's own object types from Target methods (e.g., handing back a `StripeCharge` instead of your `PaymentResult`), or exposing the adaptee via a getter "for convenience."

The fix is to make the adapter a **complete boundary**: translate exceptions into domain exceptions (preserving the cause), map all returned data into your own types, and never expose the adaptee. A class adapter is especially leaky because inheritance exposes the *entire* adaptee interface to clients — another reason object adapters are preferred.
</details>

<details>
<summary><strong>Q11: [Breaking] What happens when a Target method has no valid mapping to the adaptee?</strong></summary>

You face an **impedance mismatch with no honest translation**. Three bad options and one good one: (1) silent no-op — the worst, because callers believe the operation succeeded; (2) return a wrong/default value — corrupts data downstream; (3) fake it partially — subtle bugs. The honest options are: **throw `UnsupportedOperationException`** to fail loudly (as `Arrays.asList`'s fixed-size list does for `add`), or **do real work** to actually satisfy the contract (e.g., add buffering so a forward-only stream can support `rewind`).

The deeper lesson: if many Target methods are unmappable, the Target interface is a poor fit for this adaptee — reconsider the abstraction rather than papering over it with fakes.
</details>

<details>
<summary><strong>Q12: [Breaking] Why can a stateful adapter be a concurrency hazard?</strong></summary>

Adapters are usually assumed to be cheap, stateless translators, so developers freely share a single instance across threads. If the adapter caches translated results, buffers data, or holds a mutable iterator/cursor, that shared mutable state can be corrupted under concurrent access — and because the bug hides inside an "obviously trivial" wrapper, it's easy to overlook.

Fixes: keep adapters **stateless** (pure delegation, state lives in the adaptee), or if state is unavoidable, make it thread-safe (synchronization, concurrent collections, atomics) or **confine each adapter to one thread**. Note that adapting an inherently single-threaded adaptee (like an `Iterator`) doesn't magically make it thread-safe — the adapter inherits the adaptee's threading constraints.
</details>

<details>
<summary><strong>Q13: [Breaking] What are the risks of using a class adapter with `equals`, `hashCode`, or `final`?</strong></summary>

Because a class adapter **extends** the adaptee, it inherits the adaptee's `equals`/`hashCode`, which may cause an adapter instance to compare equal to a raw adaptee or behave unexpectedly in collections — often not what you want. It also inherits every public method, so clients can bypass your Target and call adaptee methods directly, breaking encapsulation. And if the adaptee is `final` (or has only `final`/private constructors, or is effectively sealed), you **cannot subclass it at all**, making a class adapter impossible.

These are concrete reasons the object adapter (composition) is the safer default: it exposes only the Target, controls `equals`/`hashCode` itself, and works regardless of the adaptee's finality.
</details>

<details>
<summary><strong>Q14: [Trade-off] Adapter vs. Decorator — they're both "wrappers." How do you tell them apart?</strong></summary>

Both wrap an object, and both are historically nicknamed "Wrapper," so the discriminator is **intent and interface**:

- **Adapter** presents a **different** interface than the wrapped object and **adds no behavior** — it only translates so incompatible parties can connect.
- **Decorator** presents the **same** interface as the wrapped object and **adds behavior** (logging, buffering, caching), and is designed to be **stacked** recursively.

So: same interface + more behavior = Decorator; different interface + same behavior = Adapter. If you find yourself both changing the interface *and* adding behavior, that's two responsibilities — compose a Decorator around an Adapter rather than conflating them.
</details>

<details>
<summary><strong>Q15: [Trade-off] Adapter vs. Facade — both wrap other code. Which is which?</strong></summary>

- **Facade** provides a **new, simplified** interface over an **entire subsystem** of many classes, to reduce complexity for the client. It's about *simplification* and is defined by the facade author for convenience.
- **Adapter** matches a **specific, pre-existing** interface that the client *already requires*, typically for a **single** adaptee, to fix *incompatibility*.

Rule of thumb: if you're inventing a friendlier front door to a complex set of classes, that's a Facade; if you're conforming to an interface someone else already expects so two things can plug together, that's an Adapter. A Facade *could* internally use adapters, and both reduce coupling, but the driving intent differs (simplify vs. convert).
</details>

<details>
<summary><strong>Q16: [Trade-off] Adapter vs. Bridge — both involve two interfaces. Distinguish them.</strong></summary>

- **Adapter** is **retrofitted**: applied *after* two incompatible things already exist, to make them cooperate. You didn't plan for them to work together.
- **Bridge** is **designed up front**: you deliberately split an *abstraction* hierarchy from an *implementation* hierarchy so they can vary independently, before either has many concrete types.

Put differently, Adapter reacts to an existing mismatch you didn't control; Bridge is a proactive decoupling you chose during design. Structurally they can look similar (an object holding a reference to another interface), but Bridge's two sides evolve together as a planned pair, whereas Adapter's adaptee is a fixed external constraint.
</details>

<details>
<summary><strong>Q17: [Trade-off] When is an adapter the wrong choice, and what would you do instead?</strong></summary>

Avoid an adapter when: (1) **you own both sides and they're not released** — just fix the interface; (2) the mismatch is a **trivial rename** — refactor instead of adding a class; (3) you actually need to **add behavior** (use Decorator) or **simplify a subsystem** (use Facade); (4) the interfaces are **semantically irreconcilable**, so any adapter must fake or lie — reconsider the abstraction; (5) you'd need adapters *everywhere*, which signals your Target interface is **too vendor-shaped** — redesign the port to reflect your domain, not the vendor.

The meta-point: Adapter is glue for *existing, external, immutable* interfaces. If you have the freedom to change the design, changing it is usually cleaner than gluing.
</details>

<details>
<summary><strong>Q18: [Advanced] How do reflection/dynamic proxies let you build adapters generically?</strong></summary>

`java.lang.reflect.Proxy` can synthesize an object implementing the Target interface at runtime, routing each call through an `InvocationHandler` that maps to adaptee methods — a **generic adapter** without hand-writing one class per adaptee. Frameworks use this heavily: Spring adapts and proxies beans this way; MapStruct/Dozer generate mapping adapters; JDK proxies back many client stubs.

<details>
<summary>💻 Click to expand — dynamic adapter via Proxy</summary>

```java
import java.lang.reflect.*;

interface Target { String greet(String name); }
class Adaptee { String salute(String who) { return "Hi " + who; } }

@SuppressWarnings("unchecked")
static Target adapt(Adaptee a) {
    return (Target) Proxy.newProxyInstance(
        Target.class.getClassLoader(), new Class[]{Target.class},
        (proxy, method, args) -> {
            if (method.getName().equals("greet"))
                return a.salute((String) args[0]);   // route Target call -> adaptee call
            throw new UnsupportedOperationException(method.getName());
        });
}
```
</details>

Trade-offs: reflection has overhead and loses compile-time type safety and clear stack traces, and mapping by method name is brittle. For a handful of adapters, hand-written is clearer; for hundreds (or annotation-driven mapping), generated/proxy adapters scale better. This is also how mocking frameworks and AOP conceptually relate to adaptation.
</details>

<details>
<summary><strong>Q19: [Advanced] How does Adapter show up at the distributed-systems / API boundary (anti-corruption layer)?</strong></summary>

In DDD, an **Anti-Corruption Layer (ACL)** is Adapter applied at a service boundary: it translates another bounded context's (or an external API's) model and vocabulary into your own, so their concepts don't corrupt your domain. Concretely, a service consuming a partner's REST/gRPC API wraps it in an adapter that converts their DTOs, IDs, enums, units, pagination, and error codes into your domain types and exceptions.

At the wire level this also involves **protocol adaptation** — REST↔gRPC gateways, message-format translators (Avro↔JSON), and API gateways that adapt external contracts to internal services. The distributed twist is that translation now spans the network, so the adapter must also handle partial failure, timeouts, retries, and schema evolution/versioning. The design value is the same as in-process Adapter (isolate the foreign model), amplified because external contracts change independently and unpredictably.
</details>

<details>
<summary><strong>Q20: [Advanced/Coding Challenge] Design a uniform <code>KeyValueStore</code> adapter layer over Redis-, in-memory-, and DynamoDB-style backends.</strong></summary>

A staff-favorite: one Target, several structurally different adaptees, with error-model and type translation.

<details>
<summary>💻 Click to expand full solution</summary>

```java
import java.util.*;

// ===== Target =====
interface KeyValueStore {
    Optional<String> get(String key);
    void put(String key, String value);
    boolean delete(String key);
}
class StoreException extends RuntimeException {
    StoreException(String m, Throwable c) { super(m, c); }
}

// ===== Adaptee A: in-memory map (trivial) =====
class InMemoryAdapter implements KeyValueStore {
    private final Map<String, String> m = new HashMap<>();

    public Optional<String> get(String k) { return Optional.ofNullable(m.get(k)); }
    public void put(String k, String v) { m.put(k, v); }
    public boolean delete(String k) { return m.remove(k) != null; }
}

// ===== Adaptee B: Redis-style client with different method names/return codes =====
class RedisClient {                                       // pretend third-party
    private final Map<String, String> data = new HashMap<>();

    String GET(String k) { return data.get(k); }          // null if missing
    String SET(String k, String v) {
        data.put(k, v);
        return "OK";
    }
    long DEL(String k) { return data.remove(k) != null ? 1 : 0; } // returns count
}

class RedisAdapter implements KeyValueStore {
    private final RedisClient redis;

    RedisAdapter(RedisClient redis) { this.redis = redis; }

    public Optional<String> get(String k) { return Optional.ofNullable(redis.GET(k)); }

    public void put(String k, String v) {
        if (!"OK".equals(redis.SET(k, v))) throw new StoreException("SET failed", null);
    }

    public boolean delete(String k) { return redis.DEL(k) == 1; }  // long -> boolean
}

// ===== Adaptee C: DynamoDB-style item API (Map<String,Attr>) =====
class DynamoClient {
    private final Map<String, Map<String, String>> table = new HashMap<>();

    Map<String, String> getItem(Map<String, String> key) { return table.get(key.get("pk")); }
    void putItem(Map<String, String> item) { table.put(item.get("pk"), item); }
    void deleteItem(Map<String, String> key) { table.remove(key.get("pk")); }
    boolean exists(String pk) { return table.containsKey(pk); }
}

class DynamoAdapter implements KeyValueStore {
    private final DynamoClient dynamo;

    DynamoAdapter(DynamoClient dynamo) { this.dynamo = dynamo; }

    public Optional<String> get(String k) {
        var item = dynamo.getItem(Map.of("pk", k));
        return item == null ? Optional.empty() : Optional.ofNullable(item.get("val"));
    }

    public void put(String k, String v) { dynamo.putItem(Map.of("pk", k, "val", v)); }

    public boolean delete(String k) {
        boolean existed = dynamo.exists(k);
        dynamo.deleteItem(Map.of("pk", k));
        return existed;
    }
}

// ===== Client works against the Target only; backends swap freely =====
public class KvDemo {
    static void exercise(KeyValueStore store) {
        store.put("user:1", "Ada");
        System.out.println(store.get("user:1").orElse("<none>")); // Ada
        System.out.println(store.delete("user:1"));               // true
        System.out.println(store.get("user:1").orElse("<none>")); // <none>
    }
    public static void main(String[] args) {
        for (KeyValueStore s : List.of(
                new InMemoryAdapter(),
                new RedisAdapter(new RedisClient()),
                new DynamoAdapter(new DynamoClient()))) {
            System.out.println("-- " + s.getClass().getSimpleName());
            exercise(s);
        }
    }
}
```
</details>

**Talking points:** each adaptee has a genuinely different shape (null returns, `"OK"` strings, `long` counts, item maps); every adapter normalizes to `Optional`/`boolean`/domain exceptions; the client is identical across all three. Extending to a new backend = one new adapter, no client change (OCP + DIP). Mention that this is exactly how a storage abstraction/port is built in real services, and how you'd test the client with the in-memory adapter.
</details>

---

### 🧠 Staff / Principal Engineer Level

<details>
<summary><strong>SP1: [Staff] How do you design a Target interface so you don't end up with leaky, vendor-shaped adapters?</strong></summary>

Design the Target from the **consumer's domain needs**, not from any single vendor's API — otherwise the "port" is just one vendor's interface wearing a disguise, and every other adapter fights it. Practically: (1) derive the interface from *use cases* the domain actually performs, using domain vocabulary and domain types (`Money`, `Receipt`), not vendor DTOs; (2) keep it **narrow and cohesive** (Interface Segregation) so adapters only implement what they can support; (3) define the **error contract** explicitly (domain exceptions, nullability, `Optional`) so every adapter translates to the same failure model; (4) validate the design by sketching **two or three very different adaptees** — if a second vendor forces awkward fakes, the abstraction is wrong. 

The staff-level tell is treating the Target as a first-class domain artifact with its own tests (a contract test suite every adapter must pass), rather than as glue reverse-engineered from whichever SDK you integrated first.
</details>

<details>
<summary><strong>SP2: [Staff] You have 15 external integrations each behind an adapter. How do you keep them consistent and maintainable?</strong></summary>

Standardize the boundary so 15 adapters look and behave alike: (1) a **shared contract test suite** run against every adapter (same inputs → same normalized outputs/errors), so conformance is enforced, not hoped for; (2) a common **error-translation and observability layer** — wrap each adapter with cross-cutting concerns (metrics, tracing, timeouts, retries) via a Decorator or interceptor rather than copy-pasting into each adapter; (3) a **registry/factory** that selects the adapter by config so the wiring is centralized; (4) **schema/version pinning** and consumer-driven contract tests (e.g., Pact) against the external APIs to catch upstream breaks early; (5) clear ownership and a template/scaffold so new adapters follow the same structure.

The principle: the adapters should carry *only* translation logic; everything cross-cutting (resilience, telemetry, auth) belongs in a shared layer composed around them. This keeps each adapter small, comparable, and independently testable, and prevents the "every integration is a special snowflake" decay.
</details>

<details>
<summary><strong>SP3: [Principal] How does the Adapter pattern support strangler-fig migration of a legacy system?</strong></summary>

In a strangler-fig migration you incrementally replace a legacy system while it keeps running. Adapters are the seams: (1) put an adapter (facade/ACL) **in front of the legacy system** exposing a clean modern Target, so new code depends on the target, not the legacy internals; (2) route calls through a **dispatcher/router** that sends some operations to the legacy adaptee and others to the new implementation — both behind the same Target — so you can migrate feature-by-feature; (3) optionally use a **two-way adapter** so legacy callers and new callers both keep working during the transition; (4) once a capability is fully migrated, delete its legacy adapter — the strangler "kills" that branch.

The value is that the Target interface stays stable while the *implementation behind it* moves from legacy to new, invisibly to callers. Risks a principal should call out: behavioral drift between old and new implementations (mitigate with **shadow/dual-run comparison**), data consistency across the two backends during cutover, and adapters accumulating translation debt if the migration stalls — so migrations need a deadline and a plan to remove the scaffolding.
</details>

<details>
<summary><strong>SP4: [Principal] What are the performance and reliability implications of adapter layers at scale, and how do you manage them?</strong></summary>

Per-call, an in-process adapter's overhead (an extra virtual call, maybe an object allocation for translated types) is almost always negligible — **measure before optimizing**. The real costs appear when adapters (a) allocate heavily per call (creating maps/DTOs for translation) on hot paths, causing GC pressure — fix by reusing buffers, streaming, or pushing mapping to build-time codegen (MapStruct) instead of reflection; (b) wrap **network** adaptees, where the adapter becomes the natural home for timeouts, retries, circuit breakers, and bulkheads — reliability, not CPU, dominates; (c) do reflection/dynamic-proxy dispatch, which is meaningfully slower than direct calls and worth avoiding on tight loops.

Management approach: instrument each adapter with latency/error metrics and tracing spans; set explicit **timeouts and resilience policies** on every network adapter (compose them in rather than hand-rolling per adapter); load-test the translation cost on representative payloads; and treat "the adapter" as the enforcement point for backpressure so a slow external dependency can't exhaust your threads/connections. The principal-level judgment is knowing that at scale the adapter's *reliability envelope* (timeouts, failure isolation) matters far more than its microsecond translation cost.
</details>

<details>
<summary><strong>SP5: [Principal] How do you evolve a Target interface that many adapters and clients depend on, without breaking everyone?</strong></summary>

Treat the Target as a **published contract** and evolve it with the same discipline as a public API: (1) prefer **additive, backward-compatible** changes; add new capabilities as new methods with `default` implementations so existing adapters keep compiling and get a safe fallback (but audit those defaults — a default that silently no-ops can hide missing functionality); (2) for breaking changes, **version the interface** (`KeyValueStoreV2`) and provide an adapter from old to new so migration is incremental; (3) use **consumer-driven contract tests** so you know which clients rely on which behaviors before you change anything; (4) roll out behind **feature flags** and run **shadow comparisons** between old and new behavior; (5) deprecate with a clear timeline and telemetry on usage so you can retire the old contract safely.

The subtle trap: interface segregation reduces blast radius — a smaller, focused Target is far easier to evolve than a fat one that every adapter partially implements. If you find the interface constantly churning, that's a signal the abstraction doesn't match the domain and needs a redesign, not just another version. The principal move is to minimize the number of consumers coupled to volatile parts of the contract in the first place.
</details>

---

## ⚡ Quick Revision

**One-liner:** Adapter converts one class's interface into another that clients expect, so incompatible interfaces can work together — it changes the *interface*, not the *behavior*.

**The whole pattern in one paragraph:** The Adapter is a structural GoF pattern — *"convert the interface of a class into another interface clients expect; lets classes work together that couldn't otherwise because of incompatible interfaces."* It exists to resolve **interface incompatibility** between a **client** (which speaks a **Target** interface) and an existing **adaptee** (a legacy class or third-party SDK you can't modify). The **adapter** implements the Target and delegates to the adaptee, translating method names, parameters, units, data formats, and — crucially — the **error model** (vendor exceptions → domain exceptions). It comes in two forms: the **object adapter** (composition — *holds* the adaptee, delegates; the default because it favors composition, adapts subclasses, works with `final` adaptees, and hides the adaptee) and the **class adapter** (inheritance — *extends* the adaptee and implements the Target; use only when you must override adaptee behavior, and beware it leaks the whole adaptee interface and locks to one concrete type). Its defining trait — *changes interface, adds no behavior* — separates it from its wrapper cousins: **Decorator** keeps the same interface and *adds* behavior (stackable), **Facade** invents a *simpler* interface over a whole subsystem, **Proxy** keeps the same interface and *controls access*, and **Bridge** decouples abstraction from implementation *by design up front* while Adapter reconciles things *after the fact*. Canonical JDK examples: `Arrays.asList`, `Collections.list`/`enumeration` (two-way legacy bridge), and `InputStreamReader` (bytes→chars). It's the mechanism behind **hexagonal / ports-and-adapters** architecture and the **anti-corruption layer** (Adapter at a service boundary). Watch the pitfalls: leaky adapters (translate exceptions and return types too), lossy/fake translation for unmappable methods (fail loudly with `UnsupportedOperationException` instead of lying), and stateful non-thread-safe adapters. At scale the adapter's *reliability envelope* (timeouts, retries, circuit breakers on network adaptees) matters more than its microsecond translation cost, and the Target should be shaped by your domain — not by whichever vendor you integrated first.

**Top 5 interview answers to memorize:**

1. **"What is it?"** → Converts a class's interface into another interface clients expect, so incompatible interfaces cooperate; changes the interface but adds no behavior.
2. **"Object vs. class adapter?"** → Object adapter *holds* the adaptee (composition, default, adapts subclasses, works with `final`); class adapter *extends* it (inheritance, can override via `super`, but locked to one type and leaks its interface).
3. **"Real JDK example?"** → `Arrays.asList` (array→List), `InputStreamReader` (bytes→chars), `Collections.enumeration`/`list` (Iterator↔Enumeration).
4. **"Adapter vs. Decorator vs. Facade?"** → Adapter *changes* interface, no new behavior; Decorator *keeps* interface, *adds* behavior (stackable); Facade invents a *simpler* interface over a whole subsystem.
5. **"How does it break?"** → Leaking the adaptee's exceptions/types (translate them); faking unmappable methods (throw `UnsupportedOperationException`); stateful adapters shared across threads.

**Trigger words (hear these → think Adapter):** "incompatible interface", "make X work with Y", "integrate a third-party / legacy / vendor SDK", "can't modify the existing class", "wrap the old API", "convert / translate between interfaces", "our code expects interface A but the library gives B", "square peg in a round hole", "impedance mismatch", "ports and adapters", "anti-corruption layer", "bridge two different APIs", "uniform interface over different providers".

---

*End of Adapter Pattern study guide.*







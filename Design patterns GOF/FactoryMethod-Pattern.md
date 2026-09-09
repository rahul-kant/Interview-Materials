# Factory Method Pattern ⭐⭐ (Difficulty: 2/5 — the mechanism is simple inheritance, but the Simple-Factory-vs-Factory-Method-vs-Abstract-Factory boundary and the "class explosion vs registry" trade-off are what interviewers push on)

> **Category:** Creational Pattern (GoF)
> **Also known as:** Virtual Constructor

The Factory Method pattern **defines a creation method in a base class and lets subclasses decide which concrete product it returns**. Instead of scattering `new ConcreteThing()` through your code, you call an overridable factory method, and the choice of *which* concrete class to instantiate is deferred to a subclass. The base class contains the workflow that *uses* the product; the subclass supplies the product. This inverts the dependency — high-level code depends on a product interface, not on concrete classes — and it's the pattern behind pluggable document creators, log appenders, payment processors, and framework "template + hook" designs where the framework calls *your* factory method.

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 0: Simple Factory with a Switch (Not the GoF Pattern)](#variant-0-simple-factory-with-a-switch-not-the-gof-pattern)
   - [Variant 1: Classic Factory Method (Abstract Creator + Overriding Subclasses)](#variant-1-classic-factory-method-abstract-creator--overriding-subclasses)
   - [Variant 2: Parameterized Factory Method](#variant-2-parameterized-factory-method)
   - [Variant 3: Production Registry / Supplier-Based Factory](#variant-3-production-registry--supplier-based-factory)
5. [🎨 Real-World Example](#-real-world-example)
   - [Scenario 1 — Message Queue Consumer (Kafka / RabbitMQ / SQS)](#scenario-1--message-queue-consumer-kafka--rabbitmq--sqs)
   - [Scenario 2 — Storage Client (S3 / GCS / Azure Blob)](#scenario-2--storage-client-s3--gcs--azure-blob)
   - [Scenario 3 — Payment Processor Factory](#scenario-3--payment-processor-factory)
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

> **GoF Definition:** *"Define an interface for creating an object, but let subclasses decide which class to instantiate. Factory Method lets a class defer instantiation to subclasses."*

The Factory Method exists to break the coupling between the code that *needs* an object and the code that *decides which concrete object to build*. When a class hard-codes `new ConcreteProduct()`, it's welded to that implementation — you can't substitute another product without editing the class, and the class's high-level logic is now entangled with a low-level construction detail. The pattern replaces the direct constructor call with a call to an overridable method (`createProduct()`) that returns a product *interface*.

The key insight is that the base class can contain a complete workflow — "create a product, then do X, Y, Z with it" — while leaving a single hole: *which* product. Subclasses fill that hole by overriding the factory method. This is the same collaboration as Template Method, applied specifically to object creation: the base class owns the algorithm and calls a *creational* hook that subclasses implement. Because callers work through the base class and the product interface, adding a new product type means adding a new subclass — existing code is untouched (Open/Closed), and the high-level module depends on the abstraction, not the concretion (Dependency Inversion).

---

## 🎯 Problem

An application needs to create objects, but the **exact concrete type isn't known to the code that needs them** — it depends on configuration, environment, user input, or a plugin. Hard-coding `new` couples the client to specific classes, and centralizing the choice in a big conditional means every new product type forces an edit to that conditional (violating Open/Closed) and re-testing of unrelated paths.

The pain points that lead you to Factory Method: **tight coupling** between client code and concrete product classes; **repeated construction logic** duplicated across the codebase; **violation of Open/Closed** whenever a `switch`/`if-else` must grow to accommodate a new type; and the desire to let a **framework or library defer the choice of concrete class to the code that extends it** (the framework calls your factory method).

**Concrete example scenarios:**

1. **Cross-platform UI toolkit.** A `Dialog` base class has the workflow "create a button, lay it out, wire events." `WindowsDialog` overrides `createButton()` to return a `WindowsButton`, `WebDialog` returns an `HtmlButton`. The dialog's layout logic is written once; the button type is chosen by the subclass.

2. **Log appenders.** A logging framework must send output to a `ConsoleAppender` in dev and a `FileAppender` (or a network appender) in prod. A `Logger` base class defines `createAppender()`; `DevLogger` and `ProdLogger` decide the concrete appender.

3. **Payment processing.** An e-commerce checkout supports Stripe, PayPal, and Square. The system picks the right `PaymentProcessor` based on the user's selection — ideally without a central `switch` that must change every time a new provider is added.

4. **Document / parser creation.** An editor opens `.pdf`, `.docx`, `.md`. A creator per format returns the right `Document`/`Parser`, and the "open, render, save" workflow lives in the base class.

---

## ✅ Solution

The core idea, in plain language: **replace direct `new` calls with a call to a method that returns a product interface, and let a subclass override that method to choose the concrete class.** The base class ("Creator") owns any logic that *uses* the product; the concrete creators own the decision of *which* product.

**Key structural elements:**

- **Product (interface/abstract class):** the type the factory method returns (`Document`, `Appender`, `PaymentProcessor`). Callers depend on this, never on concretes.
- **Concrete Product:** the actual implementation (`PdfDocument`, `FileAppender`, `StripeProcessor`).
- **Creator (abstract class or interface):** declares the **factory method** (`createProduct()`), often also containing higher-level operations that call it (the "template" that uses the product).
- **Concrete Creator:** overrides the factory method to return a specific concrete product.

**The mechanism that makes it work:** *polymorphic dispatch of a creational hook.* When the base class's workflow calls `createProduct()`, the JVM dispatches to whichever concrete creator's override is in play, so the base algorithm transparently gets the right product without knowing its class. This is dynamic binding applied to *construction* — the "virtual constructor." Crucially, this distinguishes the true GoF Factory Method (which uses **inheritance and overriding**) from a "Simple Factory" (a single class with a `switch`), which is *not* a GoF pattern and doesn't get the Open/Closed benefit.

The **central trade-off** to name: the classic inheritance form can cause **class explosion** — each new product typically wants its own creator subclass, doubling the class count. For applications that just need "a value → a product" mapping without a per-product workflow, a **registry of `Supplier`s** (the production variant) gives the same decoupling and Open/Closed extensibility with far fewer classes, at the cost of the inheritance-based "creator owns a workflow" structure and some compile-time discoverability.

---

## 💻 Implementation

We'll build a **document/notification-style creator**, evolving from a Simple Factory (the thing people *call* Factory Method but isn't) to a production registry. Read the variants top-to-bottom: each fixes a specific weakness of the previous one, and the first is deliberately the anti-pattern so the distinction is crisp.

### Variant 0: Simple Factory with a Switch (Not the GoF Pattern)

**What's wrong with it:** A single `ShapeFactory` class returns different concrete products based on a string/enum inside one method. This is the **Simple Factory idiom** — extremely common, frequently *called* "Factory Method" in interviews, but **not** the GoF pattern (it uses no inheritance/overriding). Its flaw is the Open/Closed violation: every new product type forces you to edit this one method and re-test it, and the factory is coupled to every concrete class. It centralizes creation (a genuine, if small, benefit over scattering `new` everywhere), but it doesn't let subclasses or plugins extend the set without touching the factory.

<details>
<summary>💻 Click to expand code — Simple Factory (anti-pattern for our purposes)</summary>

```java
interface Shape { void draw(); }
class Circle implements Shape { public void draw() { System.out.println("Circle"); } }
class Square implements Shape { public void draw() { System.out.println("Square"); } }

// Simple Factory: one class, one switch. NOT the GoF Factory Method.
class ShapeFactory {
    Shape create(String type) {
        // Every new shape edits THIS method -> Open/Closed violation.
        return switch (type.toUpperCase()) {
            case "CIRCLE" -> new Circle();
            case "SQUARE" -> new Square();
            default -> throw new IllegalArgumentException("Unknown shape: " + type);
        };
    }
}
```
</details>

**Pros:** Dead simple; centralizes construction in one place (better than `new` scattered everywhere); fine for a tiny, fixed set of types.
**Cons:** Violates Open/Closed (edit the switch for every new type); the factory is coupled to all concrete classes; not extensible by subclasses or plugins; string keys defer errors to runtime.
**Mechanism:** *Conditional dispatch in a single method.* There is no polymorphic creational hook — hence not the GoF pattern.

### Variant 1: Classic Factory Method (Abstract Creator + Overriding Subclasses)

**What problem it solves vs. V0:** This is the real pattern. An abstract `DocumentCreator` declares `createDocument()` and provides a workflow (`openDocument()`) that *uses* it. `PdfCreator` and `WordCreator` each override the factory method to return their concrete product. Adding a new document type is now a **new subclass** — the abstract creator and existing subclasses are untouched (Open/Closed satisfied), and clients depend on the `Document` interface (Dependency Inversion).

**What's still limited:** **Class explosion.** Every product wants its own creator subclass, so N products ⇒ up to N creators plus N products. When there's no meaningful per-product workflow — you just need "value → product" — that's a lot of classes for little benefit.

<details>
<summary>💻 Click to expand code — classic Factory Method</summary>

```java
// Product
interface Document { void open(); }
class PdfDocument  implements Document { public void open() { System.out.println("Open PDF"); } }
class WordDocument implements Document { public void open() { System.out.println("Open Word"); } }

// Creator — declares the factory method AND a workflow that uses the product
abstract class DocumentCreator {
    protected abstract Document createDocument();   // the factory method (the "hole")

    public void openDocument() {                    // template that USES the product
        Document doc = createDocument();            // polymorphic dispatch picks the concrete
        doc.open();
        System.out.println("...rendered in viewer");
    }
}

// Concrete Creators — decide WHICH product
class PdfCreator  extends DocumentCreator { protected Document createDocument() { return new PdfDocument(); } }
class WordCreator extends DocumentCreator { protected Document createDocument() { return new WordDocument(); } }

// Client
class App {
    void run(DocumentCreator creator) { creator.openDocument(); }  // agnostic of concrete type
}
```
</details>

**Pros:** Open/Closed (new product = new subclass, no edits); client decoupled from concrete products; the creator can encapsulate a reusable workflow around the product; trivially mockable.
**Cons:** Class explosion (a creator per product); indirection can feel heavy for simple needs; the factory method's parameterlessness makes passing construction args awkward.
**Mechanism:** *Polymorphic override of a creational hook (virtual constructor).* The base `openDocument()` calls `createDocument()`, and dynamic binding routes it to the concrete creator's override — construction chosen at runtime by type.

### Variant 2: Parameterized Factory Method

**What problem it solves vs. V1:** Sometimes you don't want a subclass per product — you want *one* creator that returns different products based on a parameter, while still exposing an overridable factory method for extension. A parameterized factory method (`create(Type t)`) keeps a single creator for the common cases but remains overridable by a subclass that wants to add or replace behavior. This reduces class count versus pure V1 while retaining an inheritance seam.

**What's still limited:** The parameter switch inside the method is still an OCP seam (adding a type edits the method) unless subclasses override it, and it blends the Simple-Factory style with the inheritance style — cleaner than V0, but not as open as a registry.

<details>
<summary>💻 Click to expand code — parameterized factory method</summary>

```java
enum DocType { PDF, WORD }

class DocumentCreator {
    // Parameterized factory method — still overridable by subclasses for extension.
    protected Document createDocument(DocType type) {
        return switch (type) {
            case PDF  -> new PdfDocument();
            case WORD -> new WordDocument();
        };
    }
    public void openDocument(DocType type) { createDocument(type).open(); }
}

// A subclass can extend the set without editing the parent's switch:
class ExtendedCreator extends DocumentCreator {
    @Override protected Document createDocument(DocType type) {
        // handle new/overridden types, delegate the rest to super
        return super.createDocument(type);
    }
}
```
</details>

**Pros:** One creator handles many products (fewer classes than V1); enum parameter gives compile-time exhaustiveness; still overridable for extension.
**Cons:** The internal switch is an OCP seam unless subclassed; mixes two styles; less "one subclass = one clear responsibility" than V1.
**Mechanism:** *Overridable method + parameter dispatch.* Combines dynamic binding (subclass may override) with in-method selection (enum switch).

### Variant 3: Production Registry / Supplier-Based Factory

**What problem it solves vs. V1–V2:** It eliminates class explosion *and* the OCP seam, and enables **runtime/plugin registration**. A `Map<String, Supplier<Product>>` maps a key to a constructor reference; new products register themselves without editing the factory. This is the form you'll actually see in production Java (payment processors, message handlers, plugin systems). Using a thread-safe map allows concurrent registration during startup.

**What's the trade-off:** You give up the inheritance-based "creator owns a workflow" structure and some compile-time type safety (a bad key fails at runtime, and creation returns the base type). For pure "value → product" needs, that's usually a good exchange; when you genuinely need a reusable per-product workflow, V1 is better.

<details>
<summary>💻 Click to expand code — registry/Supplier factory</summary>

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

interface PaymentProcessor { void process(double amount); }
class StripeProcessor implements PaymentProcessor { public void process(double a){ System.out.println("Stripe $"+a);} }
class PayPalProcessor implements PaymentProcessor { public void process(double a){ System.out.println("PayPal $"+a);} }

class PaymentFactory {
    // Thread-safe registry: key -> lazy constructor. Add types without editing this class.
    private static final Map<String, Supplier<PaymentProcessor>> REGISTRY = new ConcurrentHashMap<>();
    static {
        REGISTRY.put("STRIPE", StripeProcessor::new);
        REGISTRY.put("PAYPAL", PayPalProcessor::new);
    }
    public static void register(String type, Supplier<PaymentProcessor> supplier) {
        REGISTRY.put(type.toUpperCase(), supplier);      // plugins register here at runtime
    }
    public PaymentProcessor create(String type) {
        Supplier<PaymentProcessor> s = REGISTRY.get(type.toUpperCase());
        if (s == null) throw new IllegalArgumentException("Unknown payment type: " + type);
        return s.get();
    }
}
```
</details>

**Pros:** No class explosion, no OCP seam; runtime/plugin extensibility; thread-safe registration with `ConcurrentHashMap`; method references are concise lazy constructors.
**Cons:** Weaker compile-time safety (bad key → runtime error; returns base type); requires a registration step; no built-in per-product workflow like the inheritance form.
**Mechanism:** *Supplier registry (`Map<String, Supplier<T>>`).* Turns "which products exist" from code (subclasses/switch) into data (map entries), decoupling the name from the instantiation logic.

---

## 🎨 Real-World Example

Three complete, production-shaped examples, each fully collapsible. The first two use the **classic inheritance form** — a creator subclass per backend that decides which concrete product to build — for a **message-queue consumer** and a **storage client**. The third shows the **production registry form** — a payment-processor factory with runtime registration and thread safety. Every scenario ends with a walkthrough of *why the code is shaped the way it is*.

### Scenario 1 — Message Queue Consumer (Kafka / RabbitMQ / SQS)

<details>
<summary>💻 Click to expand — MQ consumer factory method + demo + explanation</summary>

A `MessageConsumerApp` base class owns the invariant workflow — "create a consumer, poll a batch, process each message, acknowledge" — and defers *which* broker's consumer to build to a `createConsumer()` factory method. `KafkaConsumerApp`, `RabbitConsumerApp`, and `SqsConsumerApp` each override it to return their broker's concrete consumer. The consume loop is written once; only the concrete `MessageConsumer` varies.

```java
import java.util.List;

// Product — the broker-agnostic contract the workflow depends on
interface MessageConsumer {
    List<String> poll(int max);   // fetch up to `max` messages
    void ack(String messageId);   // acknowledge/commit a processed message
}

// Concrete Products — one per broker, each with its own native mechanism
class KafkaConsumer implements MessageConsumer {
    public List<String> poll(int max) {
        System.out.println("[Kafka] poll() up to " + max + " records by offset");
        return List.of("k-1", "k-2");
    }

    public void ack(String id) {
        System.out.println("[Kafka] commit offset for " + id);
    }
}

class RabbitConsumer implements MessageConsumer {
    public List<String> poll(int max) {
        System.out.println("[Rabbit] basic.get up to " + max + " from queue");
        return List.of("r-1");
    }

    public void ack(String id) {
        System.out.println("[Rabbit] basic.ack " + id);
    }
}

class SqsConsumer implements MessageConsumer {
    public List<String> poll(int max) {
        System.out.println("[SQS] ReceiveMessage (long-poll) up to " + max);
        return List.of("s-1", "s-2", "s-3");
    }

    public void ack(String id) {
        System.out.println("[SQS] DeleteMessage " + id);
    }
}

// Creator — owns the consume loop, defers the consumer choice to subclasses
abstract class MessageConsumerApp {
    protected abstract MessageConsumer createConsumer();   // THE factory method

    public void runOnce() {                                // template that USES the product
        MessageConsumer consumer = createConsumer();
        for (String msg : consumer.poll(10)) {
            System.out.println("  processing " + msg);
            consumer.ack(msg);                             // ack after successful processing
        }
    }
}

// Concrete Creators — each decides WHICH consumer
class KafkaConsumerApp extends MessageConsumerApp {
    protected MessageConsumer createConsumer() {
        return new KafkaConsumer();
    }
}

class RabbitConsumerApp extends MessageConsumerApp {
    protected MessageConsumer createConsumer() {
        return new RabbitConsumer();
    }
}

class SqsConsumerApp extends MessageConsumerApp {
    protected MessageConsumer createConsumer() {
        return new SqsConsumer();
    }
}
```

```java
public class ConsumerDemo {
    public static void main(String[] args) {
        // Broker chosen once from config; the consume loop never changes.
        String broker = System.getenv().getOrDefault("BROKER", "KAFKA");
        MessageConsumerApp app = switch (broker) {
            case "RABBIT" -> new RabbitConsumerApp();
            case "SQS"    -> new SqsConsumerApp();
            default       -> new KafkaConsumerApp();
        };
        app.runOnce();
        // KAFKA output:
        // [Kafka] poll() up to 10 records by offset
        //   processing k-1
        // [Kafka] commit offset for k-1
        //   processing k-2
        // [Kafka] commit offset for k-2
    }
}
```

**Code explanation.** The **product** interface `MessageConsumer` captures the only two operations the polling loop needs — `poll` and `ack` — in broker-neutral terms, so the workflow depends on nothing broker-specific. Each **concrete product** encodes its broker's real mechanism (Kafka commits *offsets*, RabbitMQ does `basic.ack`, SQS does `DeleteMessage`), which is precisely the detail we want hidden behind the interface. The **creator** `MessageConsumerApp` is where Factory Method differs from a plain "return an object" method: it owns a genuine *workflow* (`runOnce()` = poll → process → ack) and calls the overridable hook `createConsumer()` for the one varying piece. This is the pattern's signature — a base algorithm with a single creational hole. Each **concrete creator** is a one-line override choosing its broker's consumer, so adding a new broker (say, Google Pub/Sub) means adding one subclass and touching nothing else (Open/Closed). The demo shows the payoff: the broker is selected in exactly one place, the consume loop is written once and reused across all brokers, and a test can subclass `MessageConsumerApp` to return a fake consumer and verify the loop with zero infrastructure.

</details>

### Scenario 2 — Storage Client (S3 / GCS / Azure Blob)

<details>
<summary>💻 Click to expand — storage client factory method + demo + explanation</summary>

A `BackupJob` base class owns the "serialize data, then store it" workflow and defers *which* cloud's client to create to a `createClient()` factory method. `S3BackupJob`, `GcsBackupJob`, and `AzureBackupJob` each return their provider's `StorageClient`. The backup logic is written once and stays cloud-agnostic.

```java
import java.nio.charset.StandardCharsets;

// Product — cloud-agnostic storage contract
interface StorageClient {
    String put(String bucket, String key, byte[] data);   // returns the stored object's URI
    byte[] get(String bucket, String key);
}

// Concrete Products — one per cloud
class S3Client implements StorageClient {
    public String put(String bucket, String key, byte[] data) {
        System.out.println("[S3] PutObject " + data.length + "B -> s3://" + bucket + "/" + key);
        return "s3://" + bucket + "/" + key;
    }

    public byte[] get(String bucket, String key) {
        System.out.println("[S3] GetObject " + key);
        return new byte[0];
    }
}

class GcsClient implements StorageClient {
    public String put(String bucket, String key, byte[] data) {
        System.out.println("[GCS] insert " + data.length + "B -> gs://" + bucket + "/" + key);
        return "gs://" + bucket + "/" + key;
    }

    public byte[] get(String bucket, String key) {
        System.out.println("[GCS] get " + key);
        return new byte[0];
    }
}

class AzureBlobClient implements StorageClient {
    public String put(String container, String key, byte[] data) {
        System.out.println("[Azure] PutBlob " + data.length + "B -> " + container + "/" + key);
        return "https://acct.blob.core.windows.net/" + container + "/" + key;
    }

    public byte[] get(String container, String key) {
        System.out.println("[Azure] GetBlob " + key);
        return new byte[0];
    }
}

// Creator — owns the backup workflow, defers the client choice
abstract class BackupJob {
    protected abstract StorageClient createClient();      // THE factory method

    public String backup(String bucket, String key, String payload) {   // template using product
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        StorageClient client = createClient();
        String uri = client.put(bucket, key, bytes);       // cloud-agnostic call
        System.out.println("  backup complete -> " + uri);
        return uri;
    }
}

// Concrete Creators
class S3BackupJob extends BackupJob {
    protected StorageClient createClient() {
        return new S3Client();
    }
}

class GcsBackupJob extends BackupJob {
    protected StorageClient createClient() {
        return new GcsClient();
    }
}

class AzureBackupJob extends BackupJob {
    protected StorageClient createClient() {
        return new AzureBlobClient();
    }
}
```

```java
public class BackupDemo {
    public static void main(String[] args) {
        String cloud = System.getenv().getOrDefault("CLOUD", "S3");
        BackupJob job = switch (cloud) {
            case "GCS"   -> new GcsBackupJob();
            case "AZURE" -> new AzureBackupJob();
            default      -> new S3BackupJob();
        };
        job.backup("backups", "2026/db-snapshot.sql", "CREATE TABLE ...");
        // S3 output:
        // [S3] PutObject 16B -> s3://backups/2026/db-snapshot.sql
        //   backup complete -> s3://backups/2026/db-snapshot.sql
    }
}
```

**Code explanation.** The **product** `StorageClient` reduces every cloud to two operations, `put` and `get`, using a neutral vocabulary (a `bucket` and a `key`) even though the providers differ — S3/GCS call it a "bucket", Azure a "container", and each has its own API verbs (`PutObject`, object `insert`, `PutBlob`). Normalizing them to one interface is what lets the backup logic stay cloud-agnostic. The **creator** `BackupJob` again owns a real workflow — serialize the payload, then store it — with a single creational hook, `createClient()`, so the "how to back up" logic isn't duplicated per cloud. Each **concrete creator** is a trivial override selecting its provider's client; adding a new provider is one subclass, no edits to `backup()` (Open/Closed), and clients depend only on the `StorageClient` interface (Dependency Inversion). Contrast this with Scenario 1: same structure, different domain — which is the point of Factory Method, a reusable base workflow parameterized by a swappable creation step. In a test you'd subclass `BackupJob` to return an in-memory `StorageClient` and assert on the captured bytes without hitting any cloud.

</details>

### Scenario 3 — Payment Processor Factory

<details>
<summary>💻 Click to expand — payment processor registry + demo + explanation</summary>

A registry-based factory that maps a provider key to a `PaymentProcessor`, supports runtime plugin registration, and is safe for concurrent access. This is the *registry* variant of the pattern (rather than inheritance), which suits a pure "value → product" mapping with no per-product workflow.

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

interface PaymentProcessor {
    void process(double amount);
}

class StripeProcessor implements PaymentProcessor {
    public void process(double amount) {
        System.out.println("Stripe charged $" + amount);
    }
}

class PayPalProcessor implements PaymentProcessor {
    public void process(double amount) {
        System.out.println("PayPal charged $" + amount);
    }
}

class SquareProcessor implements PaymentProcessor {
    public void process(double amount) {
        System.out.println("Square charged $" + amount);
    }
}

class PaymentFactory {
    // ConcurrentHashMap => safe to register from multiple threads during startup/plugin load.
    private static final Map<String, Supplier<PaymentProcessor>> REGISTRY = new ConcurrentHashMap<>();

    static {
        REGISTRY.put("STRIPE", StripeProcessor::new);
        REGISTRY.put("PAYPAL", PayPalProcessor::new);
    }

    public static void register(String key, Supplier<PaymentProcessor> supplier) {
        REGISTRY.put(key.toUpperCase(), supplier);
    }

    public static PaymentProcessor create(String key) {
        Supplier<PaymentProcessor> supplier = REGISTRY.get(key.toUpperCase());
        if (supplier == null) {
            throw new IllegalArgumentException("Unknown provider: " + key);
        }
        return supplier.get();   // fresh, thread-confined processor per call
    }
}

public class CheckoutDemo {
    public static void main(String[] args) {
        PaymentFactory.create("STRIPE").process(49.99);
        PaymentFactory.create("PAYPAL").process(19.00);

        // Plugin registers a new provider at runtime — no factory edit:
        PaymentFactory.register("SQUARE", SquareProcessor::new);
        PaymentFactory.create("SQUARE").process(75.50);
        // Output:
        // Stripe charged $49.99
        // PayPal charged $19.0
        // Square charged $75.5
    }
}
```

**Code explanation.** Unlike Scenarios 1–2, there's no per-product *workflow* here — the caller just needs "give me the processor for this provider key" — so the inheritance form (a creator subclass per provider) would be pure class explosion. The **registry variant** fits better: a `Map<String, Supplier<PaymentProcessor>>` maps each key to a lazy constructor (`StripeProcessor::new` is a method reference that builds one on demand). This turns "which products exist" from *code* (subclasses/switch) into *data* (map entries), so new providers are added with a `register()` call — no edit to `PaymentFactory`, satisfying Open/Closed at runtime, which is exactly what a plugin architecture needs. The `ConcurrentHashMap` makes registration and lookup thread-safe when plugins load on different startup threads, and each `create()` returns a **fresh, thread-confined** processor, so the products need no synchronization. The demo highlights the plugin story: `SQUARE` is registered after the factory is already in use, then immediately resolvable — impossible with a hard-coded `switch`. Note the deliberate trade-off versus the inheritance form: you lose compile-time safety (a bad key fails at runtime, and `create()` returns the base type), which is the accepted price for open-ended extensibility.

</details>

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- You **don't know the concrete class** the code should instantiate ahead of time — it depends on config, environment, input, or a plugin.
- You want a base class to own a **workflow that uses a product** while letting subclasses choose the concrete product (a creational Template Method).
- You're building a **framework/library** and want extenders to supply the concrete types via overriding your factory method.
- You want new product types to be **additive** (new subclass or new registry entry) without editing existing client code — Open/Closed.
- You want to **decouple and test**: clients depend on the product interface, so a mock creator/product slots in.

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- **Only one or two fixed concrete types that never change.** A direct constructor or a tiny static method is clearer than a creator hierarchy.
- **No per-product workflow and a large, volatile product set.** The inheritance form causes class explosion; prefer a registry — or question whether you need the pattern at all.
- **You actually need a family of related products.** That's Abstract Factory, not a single Factory Method.
- **You need step-by-step construction of one complex object.** That's Builder.
- **A DI container already resolves the concrete type** from configuration — the container is doing the factory's job.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- **Loose coupling** — clients depend on the product interface, not concrete classes (Dependency Inversion).
- **Open/Closed** — add a new product via a new subclass or registry entry; existing code untouched.
- **Single Responsibility** — construction logic lives in the creator, separate from business logic.
- **Reusable workflow** — the creator can wrap a template around the product (create → use).
- **Testability** — inject a mock creator/product to isolate the logic under test.

**Cons**

- **Class explosion (the headline con)** — the classic inheritance form wants a creator per product, doubling class count.
- **Indirection** — an extra abstraction layer can obscure "what gets created" for newcomers.
- **Parameter awkwardness** — the parameterless factory method makes passing construction arguments clumsy (mitigated by parameterized/registry variants).
- **Easily confused** — with Simple Factory (not GoF) and Abstract Factory (families), leading to muddled designs.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

Factory Method sits between the informal **Simple Factory** and the family-oriented **Abstract Factory**, and is often confused with both. It also relates to **Builder** and **Template Method**.

| Pattern | What it is | Mechanism | Key tell |
|---|---|---|---|
| **Simple Factory** | A class with a `switch` returning products | Conditional in one method | Not GoF; central switch edited per new type |
| **Factory Method** | An overridable method returning one product | Inheritance / overriding | "Subclass decides which one product" |
| **Abstract Factory** | An object creating a *family* of products | Composition of factory methods | "Related products must be consistent" |
| **Builder** | Step-by-step assembly of one complex object | Fluent accumulation + `build()` | "Many optional params / staged construction" |
| **Template Method** | Algorithm skeleton with overridable steps | Inheritance / overriding | "Fixed workflow, variable steps" — Factory Method is its creational cousin |

**Simple Factory vs. Factory Method (the trap):** a `switch` in a single class is a *Simple Factory* — a useful idiom but **not** the GoF pattern, and it violates Open/Closed. The true *Factory Method* uses **inheritance**: subclasses override the creational method, so new products are new subclasses, not edits to a switch. Say this crisply in interviews; many candidates conflate them.

**Factory Method vs. Abstract Factory:** Factory Method creates *one* product via a single overridable method; Abstract Factory creates a *family* of related products via an object with several methods (and is typically *built from* Factory Methods). One product ⇒ Factory Method; a matched set ⇒ Abstract Factory.

**Factory Method vs. Builder:** Factory Method decides *which* product in one call; Builder assembles *one* product step by step with many optional parameters. They combine: a factory method can decide which Builder to return.

**Factory Method vs. Template Method:** they're structurally identical (base class calls an overridable hook); Factory Method is simply the special case where the hook's job is *object creation*.

</details>

## 📊 Comparison Table of Variants

<details>
<summary>📖 Click to expand</summary>

| Axis | V0: Simple Factory | V1: Classic FM | V2: Parameterized | V3: Registry |
|---|---|---|---|---|
| **Is it the GoF pattern?** | No | Yes | Yes (variant) | Yes (variant) |
| **Mechanism** | switch in one class | subclass override | override + param | Supplier map |
| **Open/Closed** | ❌ (edit switch) | ✅ (new subclass) | ⚠️ (switch unless overridden) | ✅ (register) |
| **Class count** | Low | High (creator per product) | Medium | Low |
| **Runtime/plugin extensibility** | ❌ | ⚠️ (recompile) | ⚠️ | ✅ |
| **Compile-time type safety** | Low (strings) | High | High (enum) | Medium (base type) |
| **Per-product workflow** | No | Yes (creator template) | Partial | No |
| **Best for** | tiny fixed sets | frameworks, template+hook | few types, one creator | plugins, enterprise dispatch |

**Best choice:** the **classic inheritance form (V1)** when the creator owns a meaningful workflow or you're building a framework; the **registry (V3)** when you just need "value → product" with open-ended, plugin-style extensibility.

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

### Pitfall 1: Calling a Simple Factory "Factory Method"

**What goes wrong:** You present a single class with a `switch` and claim it's the GoF Factory Method. It isn't — it uses no inheritance and violates Open/Closed, so the "benefits" you cite don't hold.

<details>
<summary>💻 Not the pattern</summary>

```java
class ShapeFactory { Shape create(String t) { /* switch */ } }  // Simple Factory
```
</details>

<details>
<summary>💻 The actual pattern — inheritance</summary>

```java
abstract class ShapeCreator { abstract Shape createShape(); }   // GoF Factory Method
class CircleCreator extends ShapeCreator { Shape createShape() { return new Circle(); } }
```
</details>

### Pitfall 2: Returning the concrete type instead of the interface

**What goes wrong:** A concrete creator declares its return type as the concrete product, re-coupling callers to implementations and defeating decoupling.

<details>
<summary>💻 Broken</summary>

```java
class PdfCreator extends DocumentCreator {
    PdfDocument createDocument() { return new PdfDocument(); }  // concrete return type!
}
```
</details>

<details>
<summary>💻 Fix</summary>

```java
class PdfCreator extends DocumentCreator {
    Document createDocument() { return new PdfDocument(); }     // abstract return type
}
```
</details>

### Pitfall 3: Over-engineering simple objects

**What goes wrong:** You wrap trivial construction in a creator hierarchy, adding indirection for no benefit.

<details>
<summary>💻 Overkill</summary>

```java
User u = new UserCreator().createUser("John");   // why not just new User("John")?
```
</details>

<details>
<summary>💻 Fix — use a plain constructor or a small static factory</summary>

```java
User u = new User("John");                        // or User.of("John")
```
</details>

### Pitfall 4: Class explosion from one creator per product

**What goes wrong:** Every product spawns a creator subclass, so N products balloon into 2N classes with no shared workflow to justify them.

<details>
<summary>💻 Fix — collapse to a registry when there's no per-product workflow</summary>

```java
Map<String, Supplier<Product>> reg = new ConcurrentHashMap<>();
reg.put("A", ProductA::new);   // add products as data, not classes
reg.put("B", ProductB::new);
Product p = reg.get(key).get();
```
</details>

</details>

---

## 🎓 Interview Tips

**What interviewers commonly ask:**

- "What's the difference between Simple Factory, Factory Method, and Abstract Factory?" (The core taxonomy question — be precise that Simple Factory isn't GoF and Factory Method uses inheritance.)
- "Why is Factory Method better than an `if-else` in a constructor?" (Open/Closed + Dependency Inversion.)
- "Code a Factory Method for X" (notifications, documents, payments — show the abstract creator + overriding subclasses).
- "How do you avoid class explosion?" (Registry of `Supplier`s.)
- "How does a framework use it?" (The framework's base class calls *your* overridden factory method — inversion of control.)

**What you should proactively mention even if not asked:**

- Factory Method is the **creational cousin of Template Method** — a base workflow with an overridable creation hook (say this; it shows you see the structure).
- The **Simple-Factory-is-not-GoF** distinction — many candidates conflate them; getting it right signals depth.
- **Open/Closed + Dependency Inversion** are the motivations — new products as new subclasses/registry entries, clients depend on the product interface.
- The **class-explosion trade-off** and the **registry** escape hatch for "value → product" needs.
- It's the **building block of Abstract Factory** (each family method is a factory method) and combines with **Builder** (choose which builder) and **Prototype** (return a clone).

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Abstract Factory** — a coordinated set of Factory Methods producing a whole family; Factory Method is the single-product building block.
- **Template Method** — structurally identical; Factory Method is the special case whose overridable step *creates an object*.
- **Prototype** — a factory method can return a **clone** of a pre-configured prototype instead of `new`, avoiding costly construction.
- **Singleton** — a factory method may return a shared singleton rather than a fresh instance.
- **Builder** — combine when the chosen product needs staged construction: the factory method decides which builder to use.
- **Dependency Injection / IoC container** — the modern generalization; the container resolves the concrete type from configuration, doing the factory's job.

</details>

## 📚 Library/Framework Implementation

**1. JDK static factory methods — `Calendar.getInstance()`, `NumberFormat.getInstance()`, `Integer.valueOf()`.** These return a concrete implementation chosen by locale/config/caching without the caller naming it. `Calendar.getInstance()` returns a `GregorianCalendar` (or a locale-specific subclass); `NumberFormat.getInstance()` returns a locale-appropriate formatter. `Integer.valueOf()` even returns cached instances for small values — the caller just gets a `Number`/`Integer`.

<details>
<summary>💻 Click to expand — JDK static factory methods</summary>

```java
import java.util.Calendar;
import java.text.NumberFormat;
import java.util.Locale;

Calendar cal = Calendar.getInstance();                         // concrete subclass chosen internally
NumberFormat nf = NumberFormat.getInstance(Locale.GERMANY);    // locale-specific formatter
Integer boxed = Integer.valueOf(42);                           // may return a cached instance
```
</details>

**2. `Collection.iterator()` — Factory Method in the JDK core.** Every collection is a Creator whose `iterator()` is a factory method returning a concrete `Iterator` (e.g., `ArrayList$Itr`) that the caller uses only through the `Iterator` interface. The collection subclass "decides" the concrete iterator; the enhanced for-loop and Streams rely on this.

<details>
<summary>💻 Click to expand — Collection.iterator() as factory method</summary>

```java
import java.util.*;

List<String> list = new ArrayList<>(List.of("a", "b"));
Iterator<String> it = list.iterator();    // factory method -> concrete ArrayList$Itr
while (it.hasNext()) System.out.println(it.next());
// LinkedList.iterator() returns a different concrete Iterator; callers never know or care.
```
</details>

**3. Spring `BeanFactory` / `FactoryBean` and `java.net.URLStreamHandlerFactory`.** Spring's `BeanFactory.getBean(...)` is a configurable factory that decides whether to return a singleton, a prototype, or a proxy; a `FactoryBean<T>` lets a bean itself be a factory method for `T`. In the JDK, `URLStreamHandlerFactory` lets you plug in handlers for URL protocols — the framework calls your factory to obtain the concrete handler.

<details>
<summary>💻 Click to expand — Spring FactoryBean</summary>

```java
// A FactoryBean is Spring's factory-method hook: Spring calls getObject() to create the bean.
public class ConnectionFactoryBean implements FactoryBean<Connection> {
    public Connection getObject() throws Exception {
        return DriverManager.getConnection(url, user, pass);  // your creation logic
    }
    public Class<?> getObjectType() { return Connection.class; }
    public boolean isSingleton() { return false; }            // prototype-scoped product
}
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1: [Conceptual] What is the Factory Method pattern?</strong></summary>

Factory Method is a **creational** GoF pattern that defines a method for creating an object in a base class but lets **subclasses decide which concrete class to instantiate**. The base class calls the overridable factory method (often as part of a larger workflow); subclasses override it to return a specific product.

It promotes loose coupling: callers depend on the **product interface** and the **creator abstraction**, never on concrete products. Adding a new product means adding a new subclass (or registry entry), so existing code isn't modified. The defining feature is that instantiation is a *polymorphic, overridable* operation — hence the nickname "virtual constructor."
</details>

<details>
<summary><strong>Q2: [Conceptual] What are the participants?</strong></summary>

Four:

1. **Product** — the interface/abstract class the factory method returns (`Document`).
2. **ConcreteProduct** — the implementation (`PdfDocument`).
3. **Creator** — declares the factory method `createProduct()`, and often contains higher-level operations that call it.
4. **ConcreteCreator** — overrides the factory method to return a `ConcreteProduct`.

The Creator's non-creational methods (like `openDocument()` calling `createDocument()`) are what make Factory Method more than "a method that returns an object" — the base class owns a workflow that consumes the product.
</details>

<details>
<summary><strong>Q3: [Conceptual] Why is it better than an `if-else`/`switch` in a constructor?</strong></summary>

A conditional in a constructor (or Simple Factory) violates the **Open/Closed Principle**: every new product type forces you to edit and re-test that conditional, and the class is coupled to all concrete products. Factory Method makes new products **additive** — you add a subclass that overrides the creational method, leaving the base class and existing subclasses untouched.

It also enforces **Dependency Inversion**: the high-level creator depends on the `Product` abstraction rather than concrete classes, so the design is modular and easy to test with mocks. The conditional approach couples high-level logic to low-level construction details.
</details>

<details>
<summary><strong>Q4: [Conceptual] How does Factory Method relate to Template Method?</strong></summary>

They're structurally the same collaboration: a base class defines a skeleton and calls **overridable hook methods** that subclasses implement. Template Method's hooks are *algorithm steps*; Factory Method's hook is specifically *object creation*. In fact, Factory Method is frequently *used inside* a Template Method — the skeleton says "create a product (hook), then process it (fixed steps)."

Recognizing this shows depth: `DocumentCreator.openDocument()` is a template method whose variable step, `createDocument()`, is the factory method. The base owns the invariant workflow; the subclass supplies the variant product.
</details>

<details>
<summary><strong>Q5: [Trade-off] Factory Method vs. Simple Factory — what's the real difference?</strong></summary>

A **Simple Factory** is a single class with a `switch`/`if-else` that returns different products. It's a common, useful idiom but **not a GoF pattern**, and it violates Open/Closed (the switch grows with each new type). A **Factory Method** uses **inheritance**: an abstract creator declares the method, and subclasses override it to choose the product, so new products are new subclasses, not switch edits.

Use Simple Factory for a small, fixed set of types where centralizing `new` is enough. Use Factory Method when you need extensibility (framework/plugin authors add types by subclassing) or when the creator owns a workflow around the product. Confusing the two is the most common Factory-Method interview mistake.
</details>

<details>
<summary><strong>Q6: [Trade-off] Factory Method vs. Abstract Factory?</strong></summary>

Factory Method creates **one** product through **one** overridable method (inheritance). Abstract Factory creates a **family** of related products through **an object** exposing several creation methods (composition) — and each of those methods is itself a factory method.

The discriminator: if you need a single product whose concrete type varies, use Factory Method; if you need a *matched set* of products that must be mutually consistent (all one OS, one theme, one provider), use Abstract Factory. Abstract Factory is essentially "several coordinated Factory Methods bundled into one family object."
</details>

<details>
<summary><strong>Q7: [Trade-off] What's the risk of "class explosion," and how do you mitigate it?</strong></summary>

In the classic inheritance form, each new product typically needs its own creator subclass, so N products can mean ~2N classes. When the creators add no real workflow — you just need "value → product" — that's a lot of ceremony.

Mitigations: use a **parameterized factory method** (one creator, an enum/param switch, still overridable) to reduce subclasses; or move to a **registry** (`Map<String, Supplier<Product>>`) so products are data, not classes, and can be added at runtime. Reserve the full inheritance form for cases where the creator genuinely owns a reusable workflow around the product (frameworks, template+hook designs).
</details>

<details>
<summary><strong>Q8: [Implementation] How do you pass construction parameters through a Factory Method?</strong></summary>

The parameterless factory method makes this awkward; three common approaches:

1. **Parameterize the method** — `createProduct(Config cfg)`; simple but couples the signature to the params.
2. **Configure the concrete creator** — pass params to the creator's constructor and have `createProduct()` read fields.
3. **Return a Builder** — the factory method returns a family/type-specific builder the caller configures, separating "which type" from "how to configure."

<details>
<summary>💻 Configured creator</summary>

```java
class ConfiguredCreator extends DocumentCreator {
    private final String path;
    ConfiguredCreator(String path) { this.path = path; }
    protected Document createDocument() { return new PdfDocument(path); }
}
```
</details>
</details>

<details>
<summary><strong>Q9: [Implementation] Can a Factory Method return a Singleton or a cached instance?</strong></summary>

Yes. The factory method doesn't have to return a fresh object — it can return a pre-built singleton or a cached instance, which is exactly what `Integer.valueOf()` does (cached boxes for -128..127) and how many `getInstance()`-style methods work.

<details>
<summary>💻 Code</summary>

```java
class CachedCreator extends DocumentCreator {
    private static final Document SHARED = new PdfDocument();  // reuse one instance
    protected Document createDocument() { return SHARED; }
}
```
</details>

Caveat: if you return shared instances, the product should be **immutable or thread-safe**, since multiple callers now hold the same object. This is a clean way to combine Factory Method with Singleton or Flyweight.
</details>

<details>
<summary><strong>Q10: [Implementation] How do you make a registry-based factory thread-safe?</strong></summary>

Back the registry with a `ConcurrentHashMap` so concurrent registration (e.g., plugins loading on different threads) and lookup are safe without external locking. Each `create()` call returns a **fresh, thread-confined** product, so the products themselves don't need synchronization unless they hold shared state.

<details>
<summary>💻 Code</summary>

```java
private static final Map<String, Supplier<Product>> REG = new ConcurrentHashMap<>();
public static void register(String k, Supplier<Product> s) { REG.put(k.toUpperCase(), s); }
public static Product create(String k) {
    var s = REG.get(k.toUpperCase());
    if (s == null) throw new IllegalArgumentException("Unknown: " + k);
    return s.get();   // new instance per call
}
```
</details>

If registration must complete before any lookups, an alternative is to build an immutable map once at startup and publish it via a `final`/`volatile` field.
</details>

<details>
<summary><strong>Q11: [Implementation] Coding challenge — implement a NotificationCreator supporting Email and SMS (classic form).</strong></summary>

<details>
<summary>💻 Complete solution</summary>

```java
interface Notification { void send(String msg); }
class EmailNotification implements Notification { public void send(String m) { System.out.println("Email: " + m); } }
class SmsNotification   implements Notification { public void send(String m) { System.out.println("SMS: " + m); } }

abstract class NotificationCreator {
    protected abstract Notification create();     // factory method
    public void notify(String msg) {              // workflow using the product
        Notification n = create();
        n.send(msg);
    }
}
class EmailCreator extends NotificationCreator { protected Notification create() { return new EmailNotification(); } }
class SmsCreator   extends NotificationCreator { protected Notification create() { return new SmsNotification(); } }

class Demo {
    public static void main(String[] a) {
        NotificationCreator c = a.length > 0 && a[0].equals("sms") ? new SmsCreator() : new EmailCreator();
        c.notify("Your order shipped");
    }
}
```
</details>
</details>

<details>
<summary><strong>Q12: [Implementation] Coding challenge — implement a registry-based plugin factory.</strong></summary>

<details>
<summary>💻 Complete solution</summary>

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

interface Plugin { void execute(); }
class AuthPlugin  implements Plugin { public void execute() { System.out.println("Auth check"); } }
class CachePlugin implements Plugin { public void execute() { System.out.println("Cache warm"); } }

class PluginFactory {
    private static final Map<String, Supplier<Plugin>> REG = new ConcurrentHashMap<>();
    public static void register(String name, Supplier<Plugin> s) { REG.put(name, s); }
    public static Plugin get(String name) {
        Supplier<Plugin> s = REG.get(name);
        if (s == null) throw new IllegalArgumentException("No plugin: " + name);
        return s.get();
    }
}

class PluginDemo {
    public static void main(String[] a) {
        PluginFactory.register("auth", AuthPlugin::new);
        PluginFactory.register("cache", CachePlugin::new);
        for (String name : List.of("auth", "cache")) PluginFactory.get(name).execute();
    }
}
```
</details>
</details>

<details>
<summary><strong>Q13: [Breaking] What breaks if a concrete creator returns the concrete product type instead of the interface?</strong></summary>

You silently re-introduce the coupling the pattern exists to remove. If `PdfCreator.createDocument()` returns `PdfDocument` rather than `Document`, any caller that uses that method now depends on the concrete class, and code written against the base `Document` interface can't transparently accept it as a substitute in all cases (covariant returns compile, but callers that captured the concrete type are stuck).

<details>
<summary>💻 Fix</summary>

```java
protected Document createDocument() { return new PdfDocument(); }  // always the abstraction
```
</details>

Always declare the factory method's return type as the **product interface** so clients stay decoupled and any concrete product is interchangeable.
</details>

<details>
<summary><strong>Q14: [Breaking] How can a Simple Factory's switch become a maintenance hazard, and how does Factory Method avoid it?</strong></summary>

The switch is a **single point that must change for every new product** and a magnet for merge conflicts; it also couples the factory to every concrete class, and a missing `case` or typo'd string key surfaces at runtime. As the product set grows, the switch becomes long, and unrelated changes risk breaking existing branches (you must re-test everything).

Factory Method avoids this by making product selection **polymorphic**: a new product is a new subclass overriding the creational method, so the base and existing subclasses are never edited. If you must keep a value→product mapping, a **registry** removes the switch entirely — new products are `register()` calls, and the mapping is data you can validate at startup.
</details>

<details>
<summary><strong>Q15: [Breaking] What goes wrong when Factory Method is overused for trivial objects?</strong></summary>

You pay all the pattern's costs (extra classes, indirection, harder-to-follow flow) for none of its benefits. A `UserCreator` that just does `new User(...)` adds a layer that obscures intent and bloats the codebase, and readers must chase the abstraction to learn what's actually created.

<details>
<summary>💻 Fix — prefer the constructor or a small static factory</summary>

```java
User u = new User("John");          // trivial construction: no pattern needed
// or a static factory only if it adds clarity/caching:
User u2 = User.of("John");
```
</details>

Reach for Factory Method only when the concrete type genuinely varies, a framework needs an extension hook, or the creator owns a real workflow — otherwise it's ceremony.
</details>

<details>
<summary><strong>Q16: [Advanced] How do you combine Factory Method with Builder for complex products?</strong></summary>

When the chosen product needs rich, staged construction, split the two concerns: the **factory method decides *which* builder** to return, and the **builder handles *how* to assemble** the product. This keeps "type selection" separate from "object assembly."

<details>
<summary>💻 Code</summary>

```java
abstract class ReportCreator {
    protected abstract ReportBuilder newBuilder();      // factory method -> a builder
    public Report create(ReportSpec spec) {
        return newBuilder().title(spec.title()).rows(spec.rows()).build();
    }
}
class PdfReportCreator extends ReportCreator { protected ReportBuilder newBuilder() { return new PdfReportBuilder(); } }
```
</details>

This is common when different product families need different construction steps but share a client-facing configuration flow.
</details>

<details>
<summary><strong>Q17: [Advanced] How does Factory Method apply to message-handler dispatch in microservices?</strong></summary>

A service consuming many event types (`OrderCreated`, `PaymentFailed`, `InventoryLow`) uses a `HandlerFactory` that maps the event type (from the message header) to the right `EventHandler`. The main consumer loop reads a message, asks the factory for a handler, and invokes it — so adding a new event type means registering a new handler, not editing the dispatch loop.

<details>
<summary>💻 Code</summary>

```java
Map<String, Supplier<EventHandler>> handlers = new ConcurrentHashMap<>();
handlers.put("OrderCreated",  OrderHandler::new);
handlers.put("PaymentFailed", PaymentHandler::new);

void onMessage(Message m) {
    EventHandler h = handlers.getOrDefault(m.type(), NoOpHandler::new).get();
    h.handle(m);     // dispatch loop never changes when new event types are added
}
```
</details>

This registry form scales cleanly and keeps the hot consumer loop closed for modification.
</details>

<details>
<summary><strong>Q18: [Advanced] How would you implement a "lazy" factory that creates the product only on first request?</strong></summary>

Combine Factory Method with lazy initialization (holder idiom or double-checked locking) or a Proxy. The creator holds no product until `create()` is first called, then builds and caches it thread-safely.

<details>
<summary>💻 Code (double-checked locking)</summary>

```java
class LazyCreator {
    private volatile Product product;     // volatile => safe publication
    Product create() {
        if (product == null) {
            synchronized (this) {
                if (product == null) product = new ExpensiveProduct();
            }
        }
        return product;
    }
}
```
</details>

If you want per-JVM laziness without locking, the Bill Pugh holder idiom (a static nested class) is cleaner; if you want lazy *behavior* rather than lazy *construction*, return a Proxy that builds the real object on first use.
</details>

<details>
<summary><strong>Q19: [Advanced] Reflection vs. a Supplier registry for a dynamic factory — trade-offs?</strong></summary>

**Reflection** (instantiate by class name from config) gives "zero-config" extensibility but is **slower** (reflective construction), **bypasses compile-time checks** (bad name / missing no-arg constructor fails at runtime), and struggles to pass constructor arguments. **Supplier registry** (`Ctor::new`) is **type-safe**, **fast** (direct construction via method reference, JIT-friendly), and supports **arbitrary construction logic** (lambdas that pass params) — at the cost of an explicit `register()` step.

FAANG-level code prefers the **registry**; reflection is reserved for genuine open plugin systems where classes aren't known at build time, and even then `ServiceLoader`/SPI (which self-registers implementations via `META-INF/services`) is usually cleaner than raw reflection.
</details>

<details>
<summary><strong>Q20: [Trade-off] Compare Factory Method with the Service Locator pattern.</strong></summary>

Factory Method is about **creating** an object whose concrete type is decided by a subclass. A **Service Locator** is about **finding/retrieving** an existing service instance from a central registry by key. Both can return an interface, but one *constructs* and the other *looks up* (often returning a shared, already-constructed instance).

Service Locator is frequently considered an anti-pattern in modern design because it hides dependencies (callers reach into a global locator instead of declaring what they need), whereas Dependency Injection makes dependencies explicit. Factory Method, by contrast, is a legitimate creational tool — and DI containers often use factory methods (`@Bean` methods, `FactoryBean`) internally to produce the objects they then inject.
</details>

### 🏛️ Staff / Principal Engineer Deep-Dive

<details>
<summary><strong>SP1: When designing a framework, how do you expose Factory Method as an extension point without leaking internals?</strong></summary>

Expose a **narrow, stable abstract method (or SPI interface)** that extenders implement, and keep the surrounding workflow — the parts that call the factory method — internal and final. The framework owns the *when* and *how it's used*; the extender owns only *which concrete product*. Guard the contract with: a clear `protected abstract createX()` (or a `ProviderFactory` SPI), documented invariants about what a valid product must satisfy, and ideally `sealed`/package-private base types so extenders can't subvert the workflow. Use `ServiceLoader`/SPI for discovery so third-party JARs register factories via `META-INF/services` without core edits. The Staff concern is **contract evolution**: because extenders implement your hook, adding a parameter or a new abstract method is a breaking change — so default methods, versioned SPIs, and capability interfaces (extenders opt into new features) are how you keep the extension point evolvable. This is exactly how JDBC drivers, SLF4J bindings, and `URLStreamHandlerFactory` work.
</details>

<details>
<summary><strong>SP2: How do you decide between the inheritance form and the registry form across a large codebase?</strong></summary>

Decide by whether the creator owns **behavior** or just a **mapping**. Use the **inheritance form** when the base class has a genuine, reusable *workflow* around the product (a template that creates then processes it), when it's a **framework extension point** meant to be subclassed, or when you want the compiler to enforce that each variant is a distinct, named type. Use the **registry form** when it's a pure **value → product** lookup, when the product set is **large or open** (plugins, event handlers), or when you need **runtime registration**. Consistency matters at scale: pick one idiom per subsystem and document it, because mixing them arbitrarily makes the codebase hard to navigate. A common mature outcome is inheritance at framework boundaries and registries for high-cardinality dispatch (message handlers, feature flags), with a DI container replacing hand-rolled factories wherever the concrete type can be resolved from configuration.
</details>

<details>
<summary><strong>SP3: What are the testing and mocking implications of Factory Method, and how do you avoid the "hard-coded new" testability trap?</strong></summary>

The pattern *improves* testability precisely because it removes hard-coded `new`: client code depends on the `Product` interface and the `Creator` abstraction, so a test injects a **mock creator returning a mock product** and verifies the client's logic without instantiating heavy real products (DB connections, HTTP clients). The trap is a class that *internally* calls a `static` factory or `new` — that's untestable because there's no seam. The fix is to make the factory an **injected collaborator** (constructor-injected `Creator`/`Supplier`) rather than a static call, so tests can substitute it. Staff-level guidance: treat "can I substitute what this code creates?" as a design smell detector — if not, extract a factory/supplier and inject it. Combine with DI so production wiring is declarative and tests override just the factory bean. Beware over-mocking: mock the *creation* seam, but prefer real (fast, in-memory) products where feasible so tests exercise real behavior.
</details>

<details>
<summary><strong>SP4: How does Factory Method interact with dependency-injection containers — does DI make it obsolete?</strong></summary>

DI containers *are* a generalized factory: they resolve which concrete type to instantiate from configuration (profiles, qualifiers, conditional beans) and inject it, doing what a hand-written factory method would. For most application code, that **replaces** hand-rolled Factory Method — you annotate implementations and let the container choose. But Factory Method remains relevant: (1) the container itself uses factory methods internally (`@Bean` methods, `FactoryBean.getObject()`); (2) when the concrete type depends on **runtime data** (a per-request payment provider from the order), a container's static wiring can't decide — you inject a factory/`ObjectProvider` and call it per request; (3) in **library code with no container**, you write the factory explicitly. The Staff judgment: default to DI for compile-time/config-time type resolution, and use an explicit factory (often injected *as* a bean) for runtime, data-driven creation — the two compose rather than compete.
</details>

<details>
<summary><strong>SP5: When is Factory Method the wrong abstraction, and what deeper design issue can it mask?</strong></summary>

It's wrong when there's **no real variation** in the created type (a factory around a single always-used product is pure indirection), when it's used as a **Simple Factory in disguise** (a switch dressed up as "the pattern," gaining none of the inheritance benefits), or when the true need is a **family** (Abstract Factory) or **staged construction** (Builder) and Factory Method is forced to do their jobs. The deeper smell it can mask: **primitive obsession / stringly-typed dispatch** — a factory keyed on strings can hide that the domain lacks proper types, so instead of `create("PREMIUM")` scattered around, the design might want a `CustomerTier` value object with behavior. Another masked issue is **excessive branching that should be polymorphism on the product itself** — if the factory chooses a type only so callers can `instanceof`-switch on it later, the behavior belongs *on* the products (Strategy/polymorphism), not in a factory plus downstream conditionals. The Staff move is to ask whether the factory is enabling clean polymorphism or merely relocating a conditional; if the latter, fix the model, not the factory.
</details>

---

## ⚡ Quick Revision

**One-liner:** Factory Method defines an overridable method for creating an object and lets subclasses decide which concrete class it returns — a "virtual constructor" that decouples callers from concrete products.

**The whole pattern in a paragraph:** When code needs an object whose concrete type isn't known ahead of time, hard-coding `new` couples it to that class and a central `switch` violates Open/Closed. Factory Method replaces the constructor call with an overridable `createProduct()` that returns a product *interface*; a base "Creator" can own a workflow that *uses* the product, while concrete creators override the method to choose the concrete class. Dynamic dispatch of this creational hook is the mechanism — the same collaboration as Template Method, specialized to construction. Crucially, the true GoF pattern uses **inheritance** (subclasses override), distinguishing it from a **Simple Factory** (one class + switch, *not* GoF) and from **Abstract Factory** (an object creating a *family*). It delivers Open/Closed (new product = new subclass) and Dependency Inversion (clients depend on the product interface). The headline trade-off is **class explosion** — a creator per product — which the **registry variant** (`Map<String, Supplier<Product>>`, thread-safe via `ConcurrentHashMap`) removes by turning products into data and enabling runtime/plugin registration, at the cost of some compile-time safety. It's the building block of Abstract Factory, combines with Builder and Prototype, and is generalized by DI containers. JDK examples: `Calendar.getInstance()`, `Collection.iterator()`, Spring `FactoryBean`.

**Top 5 interview answers to memorize:**

1. **"What is it / why?"** → An overridable method that creates an object, with subclasses deciding the concrete class; decouples callers from concretes and satisfies Open/Closed + Dependency Inversion.
2. **"Factory Method vs. Simple Factory?"** → Simple Factory is one class with a switch (not GoF, violates OCP); Factory Method uses inheritance/overriding, so new products are new subclasses, not switch edits.
3. **"Factory Method vs. Abstract Factory?"** → Factory Method makes *one* product via one overridable method; Abstract Factory makes a *family* via an object with several factory methods (built *from* factory methods).
4. **"Biggest downside?"** → Class explosion (a creator per product); mitigate with a parameterized factory or a `Supplier` registry (products as data, runtime-registerable).
5. **"How does a framework use it?"** → The framework's base workflow calls *your* overridden factory method — inversion of control; you supply the concrete type, the framework drives the flow.

**Trigger words** (if the interviewer says these, think Factory Method): *"subclasses decide which class," "defer instantiation," "avoid the `new` keyword," "virtual constructor," "pluggable/extensible product types," "framework extension point," "choose the concrete type at runtime," "one product (not a family)," "template with a creation step."*



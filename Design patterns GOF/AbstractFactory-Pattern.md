# Abstract Factory Pattern ⭐⭐⭐ (Difficulty: 3/5 — the core idea is one sentence, but the "family consistency" guarantee, the interface-bloat trap, and the Factory-Method / Builder / DI boundaries are what interviewers actually probe)

> **Category:** Creational Pattern (GoF)
> **Also known as:** Kit

The Abstract Factory pattern provides **an interface for creating whole families of related objects without naming their concrete classes**. It is a "factory of factories": instead of one method that builds one product, you get an object that exposes several creation methods (`createButton()`, `createCheckbox()`, `createTextField()`), and each concrete factory implements *all* of them to produce one internally-consistent *family* (all Windows-styled, or all macOS-styled, or all dark-themed). The client picks a factory once and is then guaranteed that every product it receives belongs together — you can never accidentally pair a Windows button with a macOS checkbox. It's the pattern behind cross-platform UI toolkits, cloud-provider abstractions, database-driver families, and theme systems.

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 0: The "God Factory" with String Flags (Anti-pattern)](#variant-0-the-god-factory-with-string-flags-anti-pattern)
   - [Variant 1: Classic Abstract Factory (Interface per Family)](#variant-1-classic-abstract-factory-interface-per-family)
   - [Variant 2: Parameterized / Enum-Selected Factory](#variant-2-parameterized--enum-selected-factory)
   - [Variant 3: Production Registry / Supplier-Based Factory](#variant-3-production-registry--supplier-based-factory)
5. [🎨 Real-World Example](#-real-world-example)
   - [Scenario 1 — Messaging Platform Abstraction (Kafka / RabbitMQ / SQS)](#scenario-1--messaging-platform-abstraction-kafka--rabbitmq--sqs)
   - [Scenario 2 — Storage Provider Abstraction (S3 / GCS / Azure Blob)](#scenario-2--storage-provider-abstraction-s3--gcs--azure-blob)
   - [Scenario 3 — Payment Gateway Family (Stripe / PayPal / Adyen)](#scenario-3--payment-gateway-family-stripe--paypal--adyen)
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

> **GoF Definition:** *"Provide an interface for creating families of related or dependent objects without specifying their concrete classes."*

The Abstract Factory exists to solve a specific tension: a system needs to create several *different kinds* of objects that must **belong to the same family** and work together, but you don't want the client hard-coding which concrete family it's using. Think of a UI that needs a button, a checkbox, and a scrollbar — all of which must match the current operating system or theme. If the client writes `new WindowsButton()` and `new MacCheckbox()`, two things go wrong: the client is now welded to concrete classes (you can't swap the whole look-and-feel without editing it), and nothing stops it from mixing incompatible products.

The key insight is to **elevate object creation to an interface that produces an entire product family at once**. You define an abstract factory with one creation method per product type, and each concrete factory implements the whole set for one coherent family. The client is handed *a* factory (via injection or configuration) and calls its creation methods, receiving products through *abstract* interfaces. It never sees a concrete class, and because a single factory produces the whole family, consistency is structurally guaranteed. Switching families — a new OS, a new cloud provider, a new theme — becomes a one-line change (which concrete factory you instantiate), with zero edits to the client. This is the Dependency Inversion Principle applied to *families* of objects.

---

## 🎯 Problem

You have a system that must be **independent of how its products are created**, but those products come in **variants that must not be mixed**. Building them with direct `new` calls scattered through the client couples the client to every concrete class and makes it impossible to guarantee that the products you assemble are compatible. Worse, when the logic to choose products lives in one giant conditional, adding a new family (or a new product) means editing that central switch — a magnet for bugs and merge conflicts.

The pain points that lead you to Abstract Factory: the client is **coupled to concrete product classes**, so swapping the whole family means rewriting it; there is **no structural guarantee of family consistency** (nothing prevents a Windows button next to a Mac scrollbar); the **creation logic is centralized in conditionals** that violate the Open/Closed and Single Responsibility principles; and you want to **ship a library exposing only interfaces**, hiding all concrete implementations from consumers.

**Concrete example scenarios:**

1. **Cross-platform UI toolkit (the canonical GoF example).** The app renders a `Button`, `Checkbox`, and `TextField`. On Windows all three must be Windows-styled; on macOS all three Mac-styled. Mixing them breaks visual consistency and native behavior. A `WindowsWidgetFactory` and `MacWidgetFactory` each build the full, matching set.

2. **Cloud-provider abstraction.** An infrastructure tool provisions `Compute`, `Storage`, and `Network`. On AWS that's EC2 / S3 / VPC; on Azure it's VM / Blob / VNet. All three must belong to the *same* provider — you can't attach an S3 bucket to an Azure VM's network. An `AwsFactory` and `AzureFactory` produce provider-consistent stacks.

3. **Database-driver family.** A data layer needs a `Connection`, `Command`, and `Transaction`, and all three must be specific to the chosen database (MySQL vs PostgreSQL) to interoperate. A `MySqlFactory` and `PostgresFactory` keep them consistent.

4. **Theme / branding system.** "Light mode" vs "dark mode" — buttons, panels, text, and icons must all switch simultaneously. A `LightThemeFactory` and `DarkThemeFactory` guarantee the whole component set flips together.

---

## ✅ Solution

The core idea, in plain language: **define one interface that knows how to create every product in a family, then provide one implementation of that interface per family.** The client depends only on the abstract factory and the abstract products; it selects a concrete factory once (at configuration/startup) and from then on receives a guaranteed-consistent set of products without ever referencing a concrete class.

**Key structural elements:**

- **Abstract Factory (interface):** declares one creation method per product type — `createButton()`, `createCheckbox()`, etc. This is the contract the client depends on.
- **Concrete Factory:** implements the abstract factory for one specific family, returning that family's concrete products (`WindowsButton`, `WindowsCheckbox`). One concrete factory = one coherent family.
- **Abstract Product (interface):** the interface for a kind of product (`Button`), returned by the factory so the client stays decoupled.
- **Concrete Product:** the actual family-specific implementation (`WindowsButton`).
- **Client:** works exclusively through the abstract factory and abstract product interfaces. It is injected with a factory and is entirely agnostic about which family it's using.

**The mechanism that makes it work:** *interface segregation plus dependency inversion applied to a whole product family.* Because all creation flows through one factory object, and that factory only ever returns products from a single family, family consistency is not a discipline you must remember — it's structurally impossible to violate. Because the client depends on the abstract factory and abstract products, the concrete family is a swappable implementation detail (Liskov substitution: any concrete factory can stand in for the abstract one). Switching families is a single instantiation change.

The **central trade-off**, which every strong answer must name, is the **"interface bloat" / Open-Closed asymmetry**: Abstract Factory makes adding a new *family* trivial (write one new concrete factory, touch nothing else) but adding a new *product type* to the family expensive (you must add a method to the abstract factory interface *and* implement it in every existing concrete factory). The pattern is Open/Closed with respect to families and closed-then-broken with respect to products. The art of applying it well — and the production variant below — is choosing the classic rigid form when the set of product types is stable, and a registry/Supplier-based form when products are added dynamically.

---

## 💻 Implementation

We'll model a **cloud-infrastructure provisioner** — a domain FAANG interviewers love because it maps cleanly onto "families that must not be mixed" (you can't provision an AWS compute node into an Azure network). We evolve from a hardcoded God-factory to a dynamic registry. Read the variants top-to-bottom: each fixes a specific weakness of the one before.

### Variant 0: The "God Factory" with String Flags (Anti-pattern)

**What's wrong with it:** A single class instantiates every product for every provider using nested `if/switch` blocks on string flags. This is the disease Abstract Factory cures, and it's the anti-pattern you show first in an interview. One class knows about *every* concrete product across *every* family — maximal coupling. It violates the **Single Responsibility Principle** (it's responsible for all providers and all product types) and the **Open/Closed Principle** (adding GCP, or adding a new product type, means editing this class and re-testing everything). There's no compile-time guarantee of family consistency — it returns a bare base type, and a caller can freely combine an AWS compute with an Azure storage. String flags mean typos become runtime failures, not compile errors.

<details>
<summary>💻 Click to expand code — the God Factory anti-pattern</summary>

```java
// DON'T DO THIS — one class coupled to every product of every family.
class CloudResource {
    void provision() { System.out.println("Provisioning generic resource"); }
}
class AwsCompute  extends CloudResource { void provision() { System.out.println("AWS EC2"); } }
class AwsStorage  extends CloudResource { void provision() { System.out.println("AWS S3"); } }
class AzureCompute extends CloudResource { void provision() { System.out.println("Azure VM"); } }
class AzureStorage extends CloudResource { void provision() { System.out.println("Azure Blob"); } }

public class CloudManager {
    // Stringly-typed, nested conditionals: every new provider/product edits this method.
    public CloudResource create(String provider, String type) {
        if (provider.equalsIgnoreCase("AWS")) {
            if (type.equals("COMPUTE")) return new AwsCompute();
            if (type.equals("STORAGE")) return new AwsStorage();
        } else if (provider.equalsIgnoreCase("AZURE")) {
            if (type.equals("COMPUTE")) return new AzureCompute();
            if (type.equals("STORAGE")) return new AzureStorage();
        }
        throw new IllegalArgumentException("Unsupported: " + provider + "/" + type);
        // Nothing stops: create("AWS","COMPUTE") + create("AZURE","STORAGE") -> mixed families!
    }
}
```
</details>

**Pros:** Trivial to write for two or three products; everything is in one place.
**Cons:** Violates SRP and OCP; maximal coupling to every concrete class; no family-consistency guarantee (families can be mixed); string flags defer errors to runtime.
**Mechanism (absent):** There is no factory abstraction — just conditional dispatch on strings. The pattern's whole value (interface segregation + guaranteed families) is missing.

### Variant 1: Classic Abstract Factory (Interface per Family)

**What problem it solves vs. V0:** It introduces the actual pattern. An `CloudFactory` interface declares `createCompute()` and `createStorage()`; `AwsCloudFactory` and `AzureCloudFactory` each implement *both* for one provider. The client (a `CloudOrchestrator`) is injected with a `CloudFactory` and never names a concrete class. Family consistency is now structural — a single factory only ever returns its own provider's products, so you *cannot* mix AWS compute with Azure storage.

**What's still limited:** **Interface bloat.** If the provider later offers a `Network` resource, you must add `createNetwork()` to the `CloudFactory` interface and implement it in *every* concrete factory — an Open/Closed violation with respect to product types. Fine when the product set is stable; painful when it grows.

<details>
<summary>💻 Click to expand code — classic Abstract Factory</summary>

```java
// Abstract Products
interface Compute { void provision(); }
interface Storage { void provision(); }

// Concrete Products — AWS family
class AwsEc2 implements Compute { public void provision() { System.out.println("AWS: EC2 instance"); } }
class AwsS3  implements Storage { public void provision() { System.out.println("AWS: S3 bucket"); } }

// Concrete Products — Azure family
class AzureVm   implements Compute { public void provision() { System.out.println("Azure: VM"); } }
class AzureBlob implements Storage { public void provision() { System.out.println("Azure: Blob storage"); } }

// Abstract Factory — one creation method per product type
interface CloudFactory {
    Compute createCompute();
    Storage createStorage();
}

// Concrete Factories — each builds ONE consistent family
class AwsCloudFactory implements CloudFactory {
    public Compute createCompute() { return new AwsEc2(); }
    public Storage createStorage() { return new AwsS3(); }
}
class AzureCloudFactory implements CloudFactory {
    public Compute createCompute() { return new AzureVm(); }
    public Storage createStorage() { return new AzureBlob(); }
}

// Client — depends only on the abstract factory + abstract products
class CloudOrchestrator {
    private final Compute compute;
    private final Storage storage;
    CloudOrchestrator(CloudFactory factory) {   // injected: agnostic of provider
        this.compute = factory.createCompute();
        this.storage = factory.createStorage(); // guaranteed same family
    }
    void deployStack() { compute.provision(); storage.provision(); }
}
```
</details>

**Pros:** Client fully decoupled from concrete products; guaranteed family consistency; adding a *new family* is trivial (one new factory class); highly testable (inject a mock factory).
**Cons:** Interface bloat — adding a new *product type* forces edits to the interface and every concrete factory; more classes/interfaces than a naive approach.
**Mechanism:** *Interface segregation + polymorphic dispatch.* The client binds to `CloudFactory`; the JVM dispatches `createCompute()` to whichever concrete factory was injected. One factory ⇒ one family ⇒ structural consistency.

### Variant 2: Parameterized / Enum-Selected Factory

**What problem it solves vs. V1:** In V1 the client still has to obtain the right concrete factory somewhere. A common improvement centralizes *family selection* behind a static `getFactory(Provider)` method (a Simple Factory that returns Abstract Factories), keyed by an **enum** rather than a string so selection is type-safe and exhaustive. This keeps the classic interface's consistency guarantee while giving one clean, testable place that maps a configuration value to a family — without leaking concrete factory names to the client.

**What's still limited:** It still suffers the same interface-bloat problem for new product types, and the `switch` over the enum must be updated when a new family is added (though that's one localized spot, and `switch` exhaustiveness on enums can be compiler-checked).

<details>
<summary>💻 Click to expand code — enum-selected factory provider</summary>

```java
enum Provider { AWS, AZURE }

class CloudFactoryProvider {
    // Type-safe family selection in ONE place; client passes an enum, not a string.
    static CloudFactory getFactory(Provider p) {
        return switch (p) {
            case AWS   -> new AwsCloudFactory();
            case AZURE -> new AzureCloudFactory();
        };  // exhaustive switch: compiler flags a missing case if we add a Provider
    }
}

// Client usage — no concrete factory names anywhere:
class Deployment {
    void run(Provider provider) {
        CloudFactory factory = CloudFactoryProvider.getFactory(provider);
        new CloudOrchestrator(factory).deployStack();
    }
}
```
</details>

**Pros:** Type-safe, exhaustive family selection; single localized place to map config → family; client never sees concrete factory classes; easy to unit-test the mapping.
**Cons:** Still interface-bloated for new product types; the selection `switch` is a (small, localized) OCP seam; concrete factories are still compile-time-known.
**Mechanism:** *A Simple Factory of Abstract Factories, keyed by an enum.* Enum exhaustiveness gives compile-time safety; the rest is still the classic pattern underneath.

### Variant 3: Production Registry / Supplier-Based Factory

**What problem it solves vs. V1–V2:** This directly attacks interface bloat and enables *runtime/plugin* extensibility. Instead of a fixed method per product type, the factory holds a `Map<ProductType, Supplier<Product>>`. New product types (and whole families) can be **registered at runtime** without ever modifying a base interface — ideal for plugin architectures, multi-tenant systems, and feature-flagged products. Using a `ConcurrentHashMap` makes registration/lookup thread-safe under concurrent initialization.

**What's the trade-off:** You lose some compile-time type safety — creation is keyed by a value and returns a common base type, so a bad key is a runtime error, not a compile error. That's the deliberate exchange for open-ended extensibility.

<details>
<summary>💻 Click to expand code — registry/Supplier-based factory</summary>

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

interface CloudResource { void provision(); }
class AwsEc2 implements CloudResource { public void provision() { System.out.println("AWS: EC2"); } }
class AwsS3  implements CloudResource { public void provision() { System.out.println("AWS: S3"); } }
class AzureVm   implements CloudResource { public void provision() { System.out.println("Azure: VM"); } }
class AzureBlob implements CloudResource { public void provision() { System.out.println("Azure: Blob"); } }

interface ProviderFactory { CloudResource create(String type); }

// Concrete factory backed by a registry — extensible without editing the interface.
class RegistryCloudFactory implements ProviderFactory {
    private final Map<String, Supplier<CloudResource>> registry = new ConcurrentHashMap<>();

    RegistryCloudFactory register(String type, Supplier<CloudResource> supplier) {
        registry.put(type.toUpperCase(), supplier);   // thread-safe registration
        return this;                                    // fluent for setup
    }
    public CloudResource create(String type) {
        Supplier<CloudResource> s = registry.get(type.toUpperCase());
        if (s == null) throw new IllegalArgumentException("Unsupported resource: " + type);
        return s.get();
    }
}

public class CloudMain {
    public static void main(String[] args) {
        ProviderFactory aws = new RegistryCloudFactory()
                .register("COMPUTE", AwsEc2::new)
                .register("STORAGE", AwsS3::new);        // add "NETWORK" later w/o interface change

        ProviderFactory azure = new RegistryCloudFactory()
                .register("COMPUTE", AzureVm::new)
                .register("STORAGE", AzureBlob::new);

        provision(aws);   // AWS stack
        provision(azure); // Azure stack
    }
    private static void provision(ProviderFactory f) {
        f.create("COMPUTE").provision();
        f.create("STORAGE").provision();
    }
}
```
</details>

**Pros:** Eliminates interface bloat; add product types/families at runtime; supports plugins and multi-tenancy; `ConcurrentHashMap` gives thread-safe dynamic registration.
**Cons:** Weaker compile-time type safety (returns a base type; bad keys fail at runtime); the "family" contract is looser (you rely on setup to register a complete family).
**Mechanism:** *Supplier registry (`Map<String, Supplier<T>>`) replacing fixed factory methods with data.* Method references (`AwsEc2::new`) are lazy constructors; the map turns "which products exist" from code into configuration.

---

## 🎨 Real-World Example

Three complete, production-shaped examples, each fully collapsible, where mixing families is a genuine correctness bug — exactly where Abstract Factory earns its keep. The first is a **messaging-platform abstraction** (Kafka / RabbitMQ / SQS): a producer and consumer must speak the *same* broker's protocol. The second is a **storage-provider abstraction** (S3 / GCS / Azure Blob): an uploader and a signed-URL generator must target the *same* cloud. The third is a **payment-gateway family** (Stripe / PayPal / Adyen): a charge client, refund client, and webhook verifier must all belong to the *same* gateway. Each scenario ends with a walkthrough of *why the code is shaped the way it is*.

### Scenario 1 — Messaging Platform Abstraction (Kafka / RabbitMQ / SQS)

<details>
<summary>💻 Click to expand — messaging factory + demo + explanation</summary>

A service publishes and consumes messages, but the business logic must not be welded to a specific broker (teams routinely start on RabbitMQ, migrate to Kafka, or run SQS in the cloud). The catch is that a `Producer` and a `Consumer` **must belong to the same broker** — you cannot publish to Kafka and poll from SQS and expect to see your own messages. A `MessagingFactory` yields a matched `Producer`/`Consumer` pair per broker, so the pairing is structurally guaranteed.

```java
// ---- Abstract Products: the broker-agnostic contracts the app depends on ----
interface MessageProducer {
    void publish(String topic, String payload);
}

interface MessageConsumer {
    String poll(String topic);   // returns the next message (or null)
}

// ---- Kafka family ----
class KafkaProducer implements MessageProducer {
    public void publish(String topic, String payload) {
        System.out.println("[Kafka] append to partition of '" + topic + "': " + payload);
    }
}

class KafkaConsumer implements MessageConsumer {
    public String poll(String topic) {
        System.out.println("[Kafka] fetch by offset from '" + topic + "'");
        return "kafka-msg";
    }
}

// ---- RabbitMQ family ----
class RabbitProducer implements MessageProducer {
    public void publish(String topic, String payload) {
        System.out.println("[Rabbit] route via exchange to queue '" + topic + "': " + payload);
    }
}

class RabbitConsumer implements MessageConsumer {
    public String poll(String topic) {
        System.out.println("[Rabbit] basic.get from queue '" + topic + "'");
        return "rabbit-msg";
    }
}

// ---- SQS family ----
class SqsProducer implements MessageProducer {
    public void publish(String topic, String payload) {
        System.out.println("[SQS] SendMessage to queue '" + topic + "': " + payload);
    }
}

class SqsConsumer implements MessageConsumer {
    public String poll(String topic) {
        System.out.println("[SQS] ReceiveMessage (long-poll) from '" + topic + "'");
        return "sqs-msg";
    }
}

// ---- Abstract Factory: one creation method per product in the family ----
interface MessagingFactory {
    MessageProducer createProducer();
    MessageConsumer createConsumer();
}

// ---- Concrete Factories: each returns ONE broker's matched pair ----
class KafkaFactory implements MessagingFactory {
    public MessageProducer createProducer() {
        return new KafkaProducer();
    }

    public MessageConsumer createConsumer() {
        return new KafkaConsumer();
    }
}

class RabbitFactory implements MessagingFactory {
    public MessageProducer createProducer() {
        return new RabbitProducer();
    }

    public MessageConsumer createConsumer() {
        return new RabbitConsumer();
    }
}

class SqsFactory implements MessagingFactory {
    public MessageProducer createProducer() {
        return new SqsProducer();
    }

    public MessageConsumer createConsumer() {
        return new SqsConsumer();
    }
}

// ---- Client: broker-agnostic; only knows the abstract factory + abstract products ----
class OrderEventService {
    private final MessageProducer producer;
    private final MessageConsumer consumer;

    OrderEventService(MessagingFactory factory) {
        this.producer = factory.createProducer();
        this.consumer = factory.createConsumer();   // guaranteed same broker as the producer
    }

    void emitOrder(String orderId) {
        producer.publish("orders", "OrderPlaced:" + orderId);
    }

    String readNext() {
        return consumer.poll("orders");
    }
}
```

```java
public class MessagingDemo {
    public static void main(String[] args) {
        // The broker family is selected ONCE, from config/env.
        MessagingFactory factory = switch (System.getenv().getOrDefault("BROKER", "KAFKA")) {
            case "RABBIT" -> new RabbitFactory();
            case "SQS"    -> new SqsFactory();
            default       -> new KafkaFactory();
        };

        OrderEventService service = new OrderEventService(factory);
        service.emitOrder("ORD-1001");
        System.out.println("consumed: " + service.readNext());
        // KAFKA output:
        // [Kafka] append to partition of 'orders': OrderPlaced:ORD-1001
        // [Kafka] fetch by offset from 'orders'
        // consumed: kafka-msg
        // Switching to RABBIT/SQS changes ONLY the factory line — OrderEventService is untouched.
    }
}
```

**Code explanation.** The two **abstract products** (`MessageProducer`, `MessageConsumer`) capture the only two operations the business logic needs — publish and poll — expressed in broker-neutral terms (a "topic", a payload). Everything the app depends on lives at this interface level. Each broker then supplies a **concrete family**: notice the *implementations differ in mechanism but not in contract* — Kafka appends to a partition and fetches by offset, RabbitMQ routes through an exchange and does `basic.get`, SQS calls `SendMessage`/`ReceiveMessage` with long-polling. Those differences are exactly the details we want to hide. The `MessagingFactory` interface declares one creation method per product; each **concrete factory** (`KafkaFactory`, etc.) implements *both*, which is the crux: because a single factory only ever returns its own broker's producer and consumer, it is **structurally impossible** to end up with a Kafka producer and an SQS consumer. The client, `OrderEventService`, receives a factory in its constructor and immediately builds its matched pair — it never names `KafkaProducer` or any concrete class, so it depends only on abstractions (Dependency Inversion). Finally, the demo shows the payoff: the broker is chosen in exactly **one place** (the `switch` on an env var), and swapping RabbitMQ for Kafka is a one-line change with the entire service left untouched — and trivially testable by passing a mock `MessagingFactory`.

</details>

### Scenario 2 — Storage Provider Abstraction (S3 / GCS / Azure Blob)

<details>
<summary>💻 Click to expand — storage provider factory + demo + explanation</summary>

An application stores blobs and hands out time-limited download links, but should stay portable across clouds. Here the family-consistency requirement bites hard: the object you **upload** and the **signed URL** you generate must target the *same* provider — an S3 uploader paired with a GCS URL signer produces links that 404. A `StorageFactory` returns a matched `BlobUploader`/`UrlSigner` pair per cloud.

```java
// ---- Abstract Products: cloud-agnostic contracts ----
interface BlobUploader {
    String upload(String bucket, String key, byte[] data);   // returns stored URI
}

interface UrlSigner {
    String presignGet(String bucket, String key, int ttlSeconds);
}

// ---- AWS S3 family ----
class S3Uploader implements BlobUploader {
    public String upload(String bucket, String key, byte[] data) {
        System.out.println("[S3] PutObject " + data.length + "B -> s3://" + bucket + "/" + key);
        return "s3://" + bucket + "/" + key;
    }
}

class S3UrlSigner implements UrlSigner {
    public String presignGet(String bucket, String key, int ttl) {
        return "https://" + bucket + ".s3.amazonaws.com/" + key + "?X-Amz-Expires=" + ttl;
    }
}

// ---- Google Cloud Storage family ----
class GcsUploader implements BlobUploader {
    public String upload(String bucket, String key, byte[] data) {
        System.out.println("[GCS] insert object " + data.length + "B -> gs://" + bucket + "/" + key);
        return "gs://" + bucket + "/" + key;
    }
}

class GcsUrlSigner implements UrlSigner {
    public String presignGet(String bucket, String key, int ttl) {
        return "https://storage.googleapis.com/" + bucket + "/" + key + "?X-Goog-Expires=" + ttl;
    }
}

// ---- Azure Blob family ----
class AzureBlobUploader implements BlobUploader {
    public String upload(String container, String key, byte[] data) {
        System.out.println("[Azure] PutBlob " + data.length + "B -> " + container + "/" + key);
        return "https://acct.blob.core.windows.net/" + container + "/" + key;
    }
}

class AzureSasSigner implements UrlSigner {
    public String presignGet(String container, String key, int ttl) {
        return "https://acct.blob.core.windows.net/" + container + "/" + key + "?sig=SAS&se=" + ttl;
    }
}

// ---- Abstract Factory ----
interface StorageFactory {
    BlobUploader createUploader();
    UrlSigner createUrlSigner();
}

// ---- Concrete Factories: each returns ONE provider's matched pair ----
class S3Factory implements StorageFactory {
    public BlobUploader createUploader() {
        return new S3Uploader();
    }

    public UrlSigner createUrlSigner() {
        return new S3UrlSigner();
    }
}

class GcsFactory implements StorageFactory {
    public BlobUploader createUploader() {
        return new GcsUploader();
    }

    public UrlSigner createUrlSigner() {
        return new GcsUrlSigner();
    }
}

class AzureStorageFactory implements StorageFactory {
    public BlobUploader createUploader() {
        return new AzureBlobUploader();
    }

    public UrlSigner createUrlSigner() {
        return new AzureSasSigner();
    }
}

// ---- Client: uploads then signs, provider-agnostic ----
class DocumentService {
    private final BlobUploader uploader;
    private final UrlSigner signer;

    DocumentService(StorageFactory factory) {
        this.uploader = factory.createUploader();
        this.signer = factory.createUrlSigner();   // same provider as the uploader
    }

    String store(String bucket, String key, byte[] bytes) {
        uploader.upload(bucket, key, bytes);
        return signer.presignGet(bucket, key, 3600);   // link that actually resolves
    }
}
```

```java
public class StorageDemo {
    public static void main(String[] args) {
        StorageFactory factory = switch (System.getenv().getOrDefault("CLOUD", "S3")) {
            case "GCS"   -> new GcsFactory();
            case "AZURE" -> new AzureStorageFactory();
            default      -> new S3Factory();
        };

        DocumentService docs = new DocumentService(factory);
        String url = docs.store("invoices", "2026/inv-1001.pdf", "PDFDATA".getBytes());
        System.out.println("download: " + url);
        // S3 output:
        // [S3] PutObject 7B -> s3://invoices/2026/inv-1001.pdf
        // download: https://invoices.s3.amazonaws.com/2026/inv-1001.pdf?X-Amz-Expires=3600
        // The signed URL and the stored object are ALWAYS the same provider — no cross-cloud 404s.
    }
}
```

**Code explanation.** The **abstract products** are again the minimal operations the app performs: `BlobUploader.upload(...)` returns a provider-neutral stored URI, and `UrlSigner.presignGet(...)` returns a time-limited link. These two are *dependent* products — a signed URL is only valid for an object stored in the same provider — which is precisely why they belong in one family rather than being created independently. Each cloud's **concrete family** encodes its own reality: S3 does `PutObject` and signs with `X-Amz-Expires`, GCS does an object insert and signs with `X-Goog-Expires`, Azure does `PutBlob` and appends a SAS token. Note the vocabulary even differs (S3/GCS "bucket" vs. Azure "container"), but the interface normalizes it to a single parameter name, so callers write one code path. The `StorageFactory` bundles the two creation methods, and each **concrete factory** returns a provider-matched pair — so `DocumentService.store()` can upload and then immediately sign, confident the link resolves. The client holds only the two abstract products (assigned once in the constructor from the injected factory), never a concrete class, so the *entire* provider choice collapses to the one `switch` in the demo. Swapping S3 for GCS is a single line; the `DocumentService`, and every other consumer, is untouched — and in a test you'd inject a fake `StorageFactory` whose uploader records bytes in memory and whose signer returns a stub URL, verifying business logic with zero cloud calls.

</details>

### Scenario 3 — Payment Gateway Family (Stripe / PayPal / Adyen)

<details>
<summary>💻 Click to expand — payment gateway factory + demo + explanation</summary>

A checkout service charges cards, issues refunds, and verifies incoming webhooks — but must stay portable across payment gateways (merchants switch providers for pricing, coverage, or redundancy). The family-consistency requirement is strict and security-critical: a `ChargeClient`, a `RefundClient`, and a `WebhookVerifier` **must all belong to the same gateway**. A refund issued through PayPal cannot reference a Stripe charge id, and a webhook signed by Stripe must be verified with Stripe's secret — verify it with Adyen's HMAC scheme and you either reject valid events or (worse) accept forged ones. A `PaymentGatewayFactory` returns a matched trio per gateway so these can never be mismatched.

```java
// ---- Abstract Products: gateway-agnostic contracts ----
interface ChargeClient {
    String charge(String customerId, long cents);   // returns a charge id
}

interface RefundClient {
    void refund(String chargeId, long cents);        // refunds a prior charge
}

interface WebhookVerifier {
    boolean verify(String payload, String signature);   // validates a callback
}

// ---- Stripe family ----
class StripeCharge implements ChargeClient {
    public String charge(String customerId, long cents) {
        System.out.println("[Stripe] POST /v1/charges " + cents + " for " + customerId);
        return "ch_stripe_1";
    }
}

class StripeRefund implements RefundClient {
    public void refund(String chargeId, long cents) {
        System.out.println("[Stripe] POST /v1/refunds for " + chargeId + " amount " + cents);
    }
}

class StripeWebhook implements WebhookVerifier {
    public boolean verify(String payload, String signature) {
        System.out.println("[Stripe] verify Stripe-Signature (HMAC-SHA256)");
        return signature.startsWith("whsec_");
    }
}

// ---- PayPal family ----
class PayPalCharge implements ChargeClient {
    public String charge(String customerId, long cents) {
        System.out.println("[PayPal] /v2/checkout/orders capture " + cents + " for " + customerId);
        return "PAYID-1";
    }
}

class PayPalRefund implements RefundClient {
    public void refund(String chargeId, long cents) {
        System.out.println("[PayPal] /v2/payments/captures/" + chargeId + "/refund " + cents);
    }
}

class PayPalWebhook implements WebhookVerifier {
    public boolean verify(String payload, String signature) {
        System.out.println("[PayPal] verify via /v1/notifications/verify-webhook-signature");
        return signature.startsWith("PAYPAL-");
    }
}

// ---- Adyen family ----
class AdyenCharge implements ChargeClient {
    public String charge(String customerId, long cents) {
        System.out.println("[Adyen] /payments authorise " + cents + " for " + customerId);
        return "psp_adyen_1";
    }
}

class AdyenRefund implements RefundClient {
    public void refund(String chargeId, long cents) {
        System.out.println("[Adyen] /payments/" + chargeId + "/refunds " + cents);
    }
}

class AdyenWebhook implements WebhookVerifier {
    public boolean verify(String payload, String signature) {
        System.out.println("[Adyen] verify HMAC (additionalData.hmacSignature)");
        return signature.startsWith("adyen_");
    }
}

// ---- Abstract Factory: one creation method per product in the family ----
interface PaymentGatewayFactory {
    ChargeClient createChargeClient();
    RefundClient createRefundClient();
    WebhookVerifier createWebhookVerifier();
}

// ---- Concrete Factories: each returns ONE gateway's matched trio ----
class StripeFactory implements PaymentGatewayFactory {
    public ChargeClient createChargeClient() {
        return new StripeCharge();
    }

    public RefundClient createRefundClient() {
        return new StripeRefund();
    }

    public WebhookVerifier createWebhookVerifier() {
        return new StripeWebhook();
    }
}

class PayPalFactory implements PaymentGatewayFactory {
    public ChargeClient createChargeClient() {
        return new PayPalCharge();
    }

    public RefundClient createRefundClient() {
        return new PayPalRefund();
    }

    public WebhookVerifier createWebhookVerifier() {
        return new PayPalWebhook();
    }
}

class AdyenFactory implements PaymentGatewayFactory {
    public ChargeClient createChargeClient() {
        return new AdyenCharge();
    }

    public RefundClient createRefundClient() {
        return new AdyenRefund();
    }

    public WebhookVerifier createWebhookVerifier() {
        return new AdyenWebhook();
    }
}

// ---- Client: gateway-agnostic checkout flow ----
class CheckoutService {
    private final ChargeClient charger;
    private final RefundClient refunder;
    private final WebhookVerifier verifier;

    CheckoutService(PaymentGatewayFactory factory) {
        this.charger = factory.createChargeClient();
        this.refunder = factory.createRefundClient();      // same gateway as charger
        this.verifier = factory.createWebhookVerifier();   // ...and the verifier too
    }

    String pay(String customer, long cents) {
        return charger.charge(customer, cents);
    }

    void cancel(String chargeId, long cents) {
        refunder.refund(chargeId, cents);
    }

    boolean isCallbackAuthentic(String body, String sig) {
        return verifier.verify(body, sig);
    }
}
```

```java
public class PaymentDemo {
    public static void main(String[] args) {
        // Gateway family selected ONCE, from config/env.
        PaymentGatewayFactory factory = switch (System.getenv().getOrDefault("GATEWAY", "STRIPE")) {
            case "PAYPAL" -> new PayPalFactory();
            case "ADYEN"  -> new AdyenFactory();
            default       -> new StripeFactory();
        };

        CheckoutService checkout = new CheckoutService(factory);
        String chargeId = checkout.pay("cust_42", 4999);          // charge $49.99
        checkout.cancel(chargeId, 4999);                          // refund the SAME charge
        System.out.println("webhook ok: " + checkout.isCallbackAuthentic("{...}", "whsec_abc"));
        // STRIPE output:
        // [Stripe] POST /v1/charges 4999 for cust_42
        // [Stripe] POST /v1/refunds for ch_stripe_1 amount 4999
        // [Stripe] verify Stripe-Signature (HMAC-SHA256)
        // webhook ok: true
        // The refund targets a Stripe charge id and the verifier uses Stripe's scheme — always consistent.
    }
}
```

**Code explanation.** The three **abstract products** — `ChargeClient`, `RefundClient`, `WebhookVerifier` — are the operations a checkout flow performs, in gateway-neutral terms (a customer id, an amount in cents, a charge id, a payload+signature). They are deeply *interdependent*: a refund must reference a charge id **minted by the same gateway's** charge client, and a webhook must be validated with **that gateway's** signature scheme — which is why they belong in one family rather than being wired up independently. Each gateway's **concrete family** encodes its real API surface (Stripe's `/v1/charges` and `Stripe-Signature` HMAC, PayPal's order-capture and verify-webhook endpoint, Adyen's `authorise` and `hmacSignature`), all hidden behind the shared interfaces. The `PaymentGatewayFactory` declares one creation method per product, and each **concrete factory** implements all three — so a single factory only ever hands out one gateway's charger, refunder, and verifier, making a cross-gateway mismatch **structurally impossible**. That impossibility is not cosmetic here: mixing gateways would produce refunds against unknown charge ids and, most dangerously, webhook verification with the wrong secret — a security hole. The client `CheckoutService` receives one factory and builds its trio in the constructor, never naming a concrete class (Dependency Inversion), so the whole gateway choice collapses to the one `switch` in the demo; swapping Stripe for Adyen is a single line, every consumer untouched, and tests inject a fake `PaymentGatewayFactory` whose clients record calls and whose verifier returns a canned result — validating checkout logic with zero real payment traffic.

</details>

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- Your system must be **independent of how its products are created**, and those products come in **families that must stay consistent** (all one OS, one cloud provider, one theme, one DB driver).
- You want to **enforce a compatibility constraint** across a set of related objects so clients cannot accidentally mix incompatible parts.
- You expect to **add new families** over time (new OS, new provider, new theme) and want that to be a drop-in addition rather than an edit across the codebase.
- You're **shipping a library** and want to expose only product interfaces, hiding all concrete implementations behind factory interfaces.
- You want **testability**: inject a mock factory that returns mock products to test client logic without real resources.

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- **You only create one kind of product.** If there's no *family*, a Factory Method (or a simple static factory) is the right, lighter tool.
- **The set of product types changes frequently.** Interface bloat makes classic Abstract Factory a maintenance burden; either accept a registry variant or reconsider the pattern.
- **The app is simple and families never vary.** Introducing several interfaces and factory classes for two objects that never change is over-engineering.
- **Products need lots of construction parameters or step-by-step assembly.** That's Builder's job — Abstract Factory picks *which family*, not *how to configure a complex object* (though they combine: a factory can return builders).
- **A DI container already manages this.** In Spring, wiring a family of beans via profiles/qualifiers often replaces a hand-rolled abstract factory.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- **Guaranteed family consistency** — a single factory only yields products from one family, so incompatible mixes are structurally impossible.
- **Decoupling** — the client depends only on abstract factory + abstract product interfaces, never concrete classes.
- **Open/Closed for families** — add a whole new family by writing one new concrete factory; no client edits.
- **Single Responsibility** — creation logic for a family is isolated in its factory, away from business logic.
- **Testability** — trivially mock the factory to feed mock products into client tests.

**Cons**

- **Interface bloat (the headline con)** — adding a new *product type* forces a change to the abstract factory interface and every concrete factory (Open/Closed violation for products).
- **Class explosion** — many interfaces and classes even for modest families.
- **Indirection** — more layers between "I want a button" and the concrete object, which can obscure flow for newcomers.
- **Rigidity of the classic form** — the fixed method-per-product contract is inflexible; escaping it (registry) trades away compile-time type safety.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

Abstract Factory is most confused with **Factory Method** (both defer instantiation) and, at a distance, with **Builder** and **Prototype**. The distinctions are about *scope and mechanism*.

| Pattern | What it creates | Mechanism | Key tell |
|---|---|---|---|
| **Abstract Factory** | A *family* of related products | An object with multiple creation methods; composition | "These products must be used together / same family" |
| **Factory Method** | *One* product, type chosen by subclass | A single overridable method; inheritance | "Subclass decides which one concrete product" |
| **Builder** | *One* complex object, step by step | Fluent accumulation + `build()` | "Many optional params / staged assembly of one object" |
| **Prototype** | A copy of an existing configured object | `clone()` of a prototypical instance | "Cloning is cheaper than constructing; avoid subclass explosion" |

**Abstract Factory vs. Factory Method (the money comparison):** Factory Method is *one method* that produces *one* product, with the concrete type chosen by a subclass overriding it (inheritance). Abstract Factory is *an object* with *several* factory methods that together produce a *family*, chosen by composition (you swap the factory instance). In fact, an Abstract Factory is frequently *implemented as a set of Factory Methods* — the concrete factory's `createCompute()`, `createStorage()` are each factory methods. The discriminator: **one product ⇒ Factory Method; a family of related products ⇒ Abstract Factory.**

**Abstract Factory vs. Builder:** Abstract Factory answers *"which family of objects?"* and returns them ready-made; Builder answers *"how do I assemble this one complex object step by step?"*. They compose: an abstract factory can return family-specific builders when the products themselves need rich configuration.

**Abstract Factory vs. Prototype:** both hide concrete classes, but Abstract Factory creates from scratch via specialized interfaces, while Prototype creates by cloning pre-configured instances. A factory can even *use* prototypes internally to produce its family members.

</details>

## 📊 Comparison Table of Variants

<details>
<summary>📖 Click to expand</summary>

| Axis | V0: God Factory | V1: Classic | V2: Enum-selected | V3: Registry |
|---|---|---|---|---|
| **Client coupling** | High (all concretes) | Low (interfaces) | Low | Very low |
| **Family consistency** | ❌ (mixable) | ✅ structural | ✅ structural | ⚠️ by setup |
| **Add a new family** | Edit central class | New factory class | New factory + 1 switch case | `register()` calls |
| **Add a new product type** | Edit central class | Edit interface + all factories | Same as V1 | `register()` (no interface change) |
| **Type safety** | Low (strings) | High | High (enum) | Medium (base type) |
| **Runtime/plugin extensibility** | ❌ | ❌ | ❌ | ✅ |
| **Compile-time selection** | n/a | Yes | Yes (exhaustive) | No |
| **Best for** | tiny scripts | stable product sets | config-driven selection | plugins, multi-tenant |

**Best choice:** the **classic form (V1)**, optionally with **enum selection (V2)**, when the product types are stable; the **registry (V3)** when you need runtime/plugin extensibility and can accept looser compile-time typing.

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

### Pitfall 1: Returning concrete types from factory methods

**What goes wrong:** A concrete factory declares its method's return type as the *concrete* product, re-coupling the client to implementation classes and defeating the pattern.

<details>
<summary>💻 Broken</summary>

```java
class WindowsFactory implements GuiFactory {
    public WindowsButton createButton() { return new WindowsButton(); } // concrete return!
}
// Client that calls createButton() now sees WindowsButton — coupling leaks back in.
```
</details>

<details>
<summary>💻 Fix — always return the abstract product</summary>

```java
class WindowsFactory implements GuiFactory {
    public Button createButton() { return new WindowsButton(); } // abstract return type
}
```
</details>

### Pitfall 2: Interface bloat from over-stuffing the factory

**What goes wrong:** Every conceivable product gets a method on the abstract factory, so each new product breaks every concrete factory, and some families can't sensibly implement all of them.

<details>
<summary>💻 Broken</summary>

```java
interface GuiFactory {
    Button createButton();  Checkbox createCheckbox();  Slider createSlider();
    Tab createTab();  Window createWindow();  Tooltip createTooltip(); // ...and 40 more
}   // adding one method forces edits to Windows/Mac/Linux/... factories
```
</details>

<details>
<summary>💻 Fix — registry/Supplier for open-ended product sets</summary>

```java
interface GuiFactory { <T> T create(Class<T> type); }  // or a Map<Key, Supplier<?>>
// New product types register without changing the interface.
```
</details>

### Pitfall 3: Using Abstract Factory when a Simple/Factory Method suffices

**What goes wrong:** You build a full interface hierarchy to instantiate a couple of unrelated objects that never vary as a family — pure ceremony.

<details>
<summary>💻 Fix</summary>

```java
// If there's no family and no runtime family-switching, a static method is enough:
Button b = ButtonFactory.create(Theme.DARK);   // Simple Factory, not Abstract Factory
```
</details>

### Pitfall 4: Accidentally mixing families

**What goes wrong:** Client code obtains products from two different factories and combines them, silently breaking the consistency the pattern is supposed to guarantee.

<details>
<summary>💻 Broken</summary>

```java
Button b = new DarkThemeFactory().createButton();
Panel  p = new LightThemeFactory().createPanel();   // mismatched family!
```
</details>

<details>
<summary>💻 Fix — pass ONE factory through the client; never let two coexist</summary>

```java
class Screen {
    Screen(ThemeFactory f) { this.button = f.createButton(); this.panel = f.createPanel(); }
    // Single injected factory -> impossible to mix families within a Screen.
}
```
</details>

</details>

---

## 🎓 Interview Tips

**What interviewers commonly ask:**

- "What's the difference between Abstract Factory and Factory Method?" (The #1 question — *one product via inheritance* vs *a family via composition*, and that Abstract Factory is often built *from* Factory Methods.)
- "Design a cross-platform UI toolkit / a cloud-provider abstraction." (The canonical Abstract Factory prompts.)
- "How do you add a new product to the family? Why is that painful?" (Interface bloat / the Open-Closed asymmetry.)
- "How would you make the factory extensible at runtime?" (Registry of `Supplier`s.)
- "How does this help testing?" (Inject a mock factory returning mock products.)

**What you should proactively mention even if not asked:**

- The **family-consistency guarantee** is the whole point — say it early: one factory ⇒ one coherent family ⇒ mixing is structurally impossible.
- The **Open/Closed asymmetry**: adding a *family* is easy, adding a *product type* is hard (interface bloat) — name this unprompted; it shows depth.
- Abstract Factory is usually **implemented as a set of Factory Methods**, and concrete factories are often **Singletons** (stateless).
- It embodies **Dependency Inversion** — client depends on abstractions; switching families is a one-line change (Liskov substitution).
- At scale, the classic rigid form gives way to **registries / DI containers** (Spring profiles + qualifiers wiring a bean family) — mention the production evolution.

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Factory Method** — the building block: each method of an Abstract Factory is typically a Factory Method. Abstract Factory is a coordinated set of them.
- **Singleton** — concrete factories are usually stateless and shared, so they're frequently Singletons (one factory instance per family).
- **Prototype** — a factory can produce family members by cloning pre-configured prototypes instead of `new`.
- **Builder** — combine when family products need rich, staged construction: the factory returns family-specific builders.
- **Bridge** — Abstract Factory (creational) often instantiates the implementor objects that Bridge (structural) then decouples from their abstraction.
- **Dependency Injection / IoC container** — the modern way to select and wire a product family (profiles, qualifiers), frequently replacing a hand-written abstract factory.

</details>

## 📚 Library/Framework Implementation

**1. `javax.xml.parsers.DocumentBuilderFactory` / `TransformerFactory` (JDK).** The classic JDK Abstract Factory: `DocumentBuilderFactory.newInstance()` returns a concrete factory (discovered via the JAXP lookup mechanism), and that factory creates a *family* of related parsing objects (`DocumentBuilder`, which in turn produces `Document`, `Element`, etc.) all from one consistent implementation. Swapping the XML implementation swaps the whole family transparently.

<details>
<summary>💻 Click to expand — DocumentBuilderFactory</summary>

```java
import javax.xml.parsers.*;
import org.w3c.dom.Document;

DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // abstract factory
factory.setNamespaceAware(true);
DocumentBuilder builder = factory.newDocumentBuilder();                // family member
Document doc = builder.newDocument();                                  // consistent products
// The concrete implementation is chosen by JAXP; client code never names it.
```
</details>

**2. `java.awt.Toolkit`.** `Toolkit.getDefaultToolkit()` returns an OS-specific abstract factory that creates a family of peer/UI objects (images, event queues, system-specific components) all matching the underlying platform. It's the AWT mechanism for producing a platform-consistent set of windowing primitives.

<details>
<summary>💻 Click to expand — AWT Toolkit</summary>

```java
import java.awt.Toolkit;
import java.awt.Image;

Toolkit tk = Toolkit.getDefaultToolkit();     // OS-specific abstract factory
Image img = tk.getImage("icon.png");          // platform-consistent product
int dpi = tk.getScreenResolution();           // another product from the same family
```
</details>

**3. JDBC `Connection` as a family factory (and Spring `BeanFactory`).** A JDBC `Connection` acts as an abstract factory for a *driver-consistent* family: `createStatement()`, `prepareStatement()`, `createBlob()` all return objects specific to that driver, guaranteed to interoperate. More broadly, Spring's `BeanFactory`/`ApplicationContext` is a generalized configurable factory that, via profiles and qualifiers, wires whole families of related beans — the enterprise evolution of Abstract Factory.

<details>
<summary>💻 Click to expand — JDBC Connection as abstract factory</summary>

```java
import java.sql.*;

Connection conn = DriverManager.getConnection(url, user, pass); // driver-specific factory
PreparedStatement ps = conn.prepareStatement("SELECT * FROM t WHERE id = ?"); // family member
Blob blob = conn.createBlob();                                  // same driver family
// All products come from one Connection => one driver => guaranteed compatible.
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1: [Conceptual] What is the Abstract Factory pattern and what problem does it solve?</strong></summary>

Abstract Factory is a **creational** GoF pattern that provides an interface for creating **families of related or dependent objects** without specifying their concrete classes. It's often described as a "factory of factories."

It solves the problem of a client that must create several kinds of objects which have to **belong to the same family** (all one OS look-and-feel, one cloud provider, one theme) while staying decoupled from concrete classes. Direct `new` calls couple the client to implementations and offer no guarantee that assembled products are compatible.

The fix: one abstract factory with a creation method per product type, and one concrete factory per family implementing them all. The client selects a factory once and receives a guaranteed-consistent set of products through abstract interfaces, so switching families is a one-line change.
</details>

<details>
<summary><strong>Q2: [Conceptual] Name the participants of the Abstract Factory pattern.</strong></summary>

Five participants:

1. **AbstractFactory** — interface declaring one creation method per product type (`createButton()`, `createCheckbox()`).
2. **ConcreteFactory** — implements all creation methods for one family (`WindowsFactory`).
3. **AbstractProduct** — interface for a kind of product (`Button`).
4. **ConcreteProduct** — the family-specific implementation (`WindowsButton`).
5. **Client** — uses only the AbstractFactory and AbstractProduct interfaces; it's injected with a factory and is agnostic about the concrete family.
</details>

<details>
<summary><strong>Q3: [Conceptual] Why is it called "Abstract Factory"?</strong></summary>

Because the factory itself is defined as an **abstraction** (an interface or abstract class), and the actual instantiation is deferred to concrete factory implementations. The client programs against the abstract factory type and never learns which concrete factory it's using.

The "abstract" also emphasizes that both the *factory* and the *products* it returns are abstract to the client — it sees `ThemeFactory`, `Button`, `Panel`, never `DarkThemeFactory` or `DarkButton`. This double layer of abstraction (abstract factory + abstract products) is what fully decouples the client and lets you swap an entire product family by changing one instantiation.
</details>

<details>
<summary><strong>Q4: [Conceptual] How does Abstract Factory support the Dependency Inversion and Open/Closed principles?</strong></summary>

**Dependency Inversion:** the high-level client depends on abstractions (`ThemeFactory`, `Button`) rather than concretions (`DarkThemeFactory`, `DarkButton`). Both high-level and low-level modules depend on the interfaces, so the concrete family is a swappable detail.

**Open/Closed (with an asymmetry):** it's *open* for new **families** — add a new concrete factory and its products, touching no existing code. But it's *not* closed for new **product types** — adding `createSlider()` forces editing the interface and every concrete factory. Strong answers name this asymmetry explicitly: Abstract Factory is OCP-friendly along the "family" axis and OCP-hostile along the "product" axis.
</details>

<details>
<summary><strong>Q5: [Trade-off] What is the difference between Abstract Factory and Factory Method?</strong></summary>

Factory Method is **one method** that creates **one** product, with the concrete type chosen by a **subclass** overriding the method (inheritance). Abstract Factory is **an object** exposing **several** creation methods that together produce a **family** of related products, chosen by **composition** (you inject a different factory instance).

Key relationship: an Abstract Factory is usually *implemented using Factory Methods* — each `create*()` on the concrete factory is itself a factory method. The discriminator to state in an interview: **need one product? Factory Method. Need a family of products that must be consistent? Abstract Factory.**
</details>

<details>
<summary><strong>Q6: [Trade-off] When would you choose Abstract Factory over Builder?</strong></summary>

They answer different questions. Abstract Factory answers *"which family of related objects should I create?"* and returns them ready-made and consistent. Builder answers *"how do I assemble this one complex object step by step?"* — it's about optional parameters and staged construction of a *single* object.

Choose Abstract Factory when the challenge is **selecting and guaranteeing a coherent set** of objects (OS widgets, cloud resources). Choose Builder when the challenge is **configuring one object** with many optional fields. They compose: an abstract factory can return family-specific *builders* when the products themselves need rich configuration.
</details>

<details>
<summary><strong>Q7: [Trade-off] What are the main drawbacks of Abstract Factory?</strong></summary>

Three big ones:

1. **Interface bloat / OCP asymmetry** — adding a new product type changes the abstract factory interface and forces edits to every concrete factory.
2. **Class explosion** — even a modest family needs an abstract factory, N concrete factories, an abstract product per type, and N×M concrete products.
3. **Indirection** — extra layers between "I want X" and the object can obscure control flow.

The mitigations: use the classic form only when the product set is stable; switch to a registry/`Supplier`-based factory for open-ended products (trading some compile-time type safety); or lean on a DI container.
</details>

<details>
<summary><strong>Q8: [Implementation] Is the Abstract Factory thread-safe? What do you watch for?</strong></summary>

The products created are typically independent, and concrete factories are usually **stateless**, so they're inherently thread-safe and often implemented as Singletons. The concern arises only in the **registry variant**: if you register product suppliers at runtime while other threads read them, the backing map must be a `ConcurrentHashMap` (or the registry must be fully populated before publication).

<details>
<summary>💻 Thread-safe registry</summary>

```java
private final Map<String, Supplier<Product>> registry = new ConcurrentHashMap<>();
// register() and create() can then be called concurrently without external locking.
```
</details>

Also, the *products* themselves have whatever thread-safety they were designed with — the factory doesn't add or remove it.
</details>

<details>
<summary><strong>Q9: [Implementation] How does the registry/Supplier variant eliminate interface bloat?</strong></summary>

It replaces the fixed method-per-product contract with **data**: a `Map<Key, Supplier<Product>>`. Adding a new product type becomes a `register(key, Ctor::new)` call rather than an interface edit that ripples through every concrete factory.

<details>
<summary>💻 Code</summary>

```java
class RegistryFactory {
    private final Map<String, Supplier<Product>> reg = new ConcurrentHashMap<>();
    void register(String k, Supplier<Product> s) { reg.put(k, s); }   // add types at runtime
    Product create(String k) {
        var s = reg.get(k);
        if (s == null) throw new IllegalArgumentException("Unknown: " + k);
        return s.get();
    }
}
```
</details>

The cost: creation is keyed by a value and returns a base type, so an unknown key fails at runtime rather than compile time. You trade compile-time safety for open-ended, plugin-style extensibility.
</details>

<details>
<summary><strong>Q10: [Implementation] Should concrete factories be Singletons? How would you make one?</strong></summary>

Usually yes — a concrete factory typically holds no per-call state (just the logic to create products), so a single shared instance suffices and avoids needless allocation. Combining Abstract Factory with Singleton is idiomatic.

<details>
<summary>💻 Code</summary>

```java
class WindowsFactory implements GuiFactory {
    private static final WindowsFactory INSTANCE = new WindowsFactory();
    private WindowsFactory() {}
    static WindowsFactory getInstance() { return INSTANCE; }
    public Button createButton() { return new WindowsButton(); }
    public Checkbox createCheckbox() { return new WindowsCheckbox(); }
}
```
</details>

Prefer eager `static final` or the Bill Pugh holder for the singleton, and expose the factory to clients through the `GuiFactory` interface so they stay decoupled.
</details>

<details>
<summary><strong>Q11: [Breaking] How can Abstract Factory's consistency guarantee be broken in practice?</strong></summary>

The guarantee holds *within* a single factory, but client code can defeat it by pulling products from **two different factories** and combining them:

<details>
<summary>💻 The break</summary>

```java
Button b = new DarkThemeFactory().createButton();
Panel  p = new LightThemeFactory().createPanel();   // mixed families — inconsistent UI
```
</details>

Other ways to break it: returning **concrete** types from factory methods (re-couples the client), or a registry factory where setup registered an **incomplete family** (a compute supplier but no matching network). The fix is to thread a **single** factory instance through the client and never let two coexist in the same scope, and to validate registry completeness at startup.
</details>

<details>
<summary><strong>Q12: [Breaking] What goes wrong if a new product type must be added to a mature family hierarchy?</strong></summary>

You hit the Open/Closed asymmetry head-on. Adding `createTooltip()` to `GuiFactory` means: (1) edit the interface, and (2) implement it in *every* existing concrete factory (`Windows`, `Mac`, `Linux`, …), or they won't compile. If some families can't meaningfully support the new product, you're tempted to throw `UnsupportedOperationException`, which violates Liskov substitution.

Mitigations: add the method with a **sensible default** in an abstract base factory class; **split** the factory into smaller cohesive factories (interface segregation) so not every family must implement everything; or move to a **registry** so new products are additive. The best answer names the asymmetry and offers these escapes rather than pretending the pattern is painlessly extensible.
</details>

<details>
<summary><strong>Q13: [Breaking] Why is using string flags to select the family a problem, and what's better?</strong></summary>

String flags (`create("AWS", "COMPUTE")`) push errors to runtime — a typo like `"AWZ"` compiles fine and blows up in production. They also make the valid set of families/products invisible to the compiler and to IDE autocomplete, and encourage a central `if/switch` that violates OCP.

<details>
<summary>💻 Fix — enum selection with exhaustive switch</summary>

```java
enum Provider { AWS, AZURE }
static CloudFactory getFactory(Provider p) {
    return switch (p) {                 // compiler flags a missing case if Provider grows
        case AWS   -> new AwsFactory();
        case AZURE -> new AzureFactory();
    };
}
```
</details>

Enums give compile-time exhaustiveness and type safety; the registry variant goes further for runtime-open sets but reintroduces value-keyed lookups (mitigate with typed keys like `Class<T>`).
</details>

<details>
<summary><strong>Q14: [Implementation] Coding challenge — implement a ThemeFactory producing a Header and Footer for "Modern" and "Classic" themes.</strong></summary>

<details>
<summary>💻 Complete solution</summary>

```java
// Abstract products
interface Header { void render(); }
interface Footer { void render(); }

// Modern family
class ModernHeader implements Header { public void render() { System.out.println("Modern header"); } }
class ModernFooter implements Footer { public void render() { System.out.println("Modern footer"); } }
// Classic family
class ClassicHeader implements Header { public void render() { System.out.println("Classic header"); } }
class ClassicFooter implements Footer { public void render() { System.out.println("Classic footer"); } }

// Abstract factory
interface ThemeFactory {
    Header createHeader();
    Footer createFooter();
}
class ModernThemeFactory implements ThemeFactory {
    public Header createHeader() { return new ModernHeader(); }
    public Footer createFooter() { return new ModernFooter(); }
}
class ClassicThemeFactory implements ThemeFactory {
    public Header createHeader() { return new ClassicHeader(); }
    public Footer createFooter() { return new ClassicFooter(); }
}

// Client + demo
class Page {
    private final Header h; private final Footer f;
    Page(ThemeFactory tf) { this.h = tf.createHeader(); this.f = tf.createFooter(); }
    void render() { h.render(); f.render(); }
}
class Demo {
    public static void main(String[] a) {
        ThemeFactory tf = a.length > 0 && a[0].equals("classic")
                ? new ClassicThemeFactory() : new ModernThemeFactory();
        new Page(tf).render();   // header + footer always same theme
    }
}
```
</details>
</details>

<details>
<summary><strong>Q15: [Implementation] Coding challenge — implement a dynamic (registry-based) Abstract Factory for game levels (Forest/Desert) producing an Enemy and a Sound.</strong></summary>

<details>
<summary>💻 Complete solution</summary>

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

interface GameEntity { void spawn(); }

class Wolf     implements GameEntity { public void spawn() { System.out.println("Spawn wolf"); } }
class BirdSong implements GameEntity { public void spawn() { System.out.println("Play bird chirps"); } }
class Scorpion implements GameEntity { public void spawn() { System.out.println("Spawn scorpion"); } }
class WindHowl implements GameEntity { public void spawn() { System.out.println("Play wind howl"); } }

class LevelFactory {                                   // one instance == one level family
    private final Map<String, Supplier<GameEntity>> reg = new ConcurrentHashMap<>();
    LevelFactory register(String key, Supplier<GameEntity> s) { reg.put(key, s); return this; }
    GameEntity create(String key) {
        var s = reg.get(key);
        if (s == null) throw new IllegalArgumentException("No entity: " + key);
        return s.get();
    }
}

class Game {
    public static void main(String[] args) {
        LevelFactory forest = new LevelFactory()
                .register("ENEMY", Wolf::new).register("SOUND", BirdSong::new);
        LevelFactory desert = new LevelFactory()
                .register("ENEMY", Scorpion::new).register("SOUND", WindHowl::new);

        load(forest);   // consistent forest family
        load(desert);   // consistent desert family
    }
    static void load(LevelFactory f) { f.create("ENEMY").spawn(); f.create("SOUND").spawn(); }
}
```
</details>
</details>

<details>
<summary><strong>Q16: [Advanced] How does Abstract Factory improve unit testing and mocking?</strong></summary>

Because the client depends only on the `AbstractFactory` interface, tests can inject a **mock factory** that returns **mock/stub products**, letting you exercise the client's logic without instantiating any real (expensive) products.

<details>
<summary>💻 Code</summary>

```java
@Test void orchestratorProvisionsBothProducts() {
    Compute compute = mock(Compute.class);
    Storage storage = mock(Storage.class);
    CloudFactory factory = mock(CloudFactory.class);
    when(factory.createCompute()).thenReturn(compute);
    when(factory.createStorage()).thenReturn(storage);

    new CloudOrchestrator(factory).deployStack();

    verify(compute).provision();     // client logic verified, no real cloud calls
    verify(storage).provision();
}
```
</details>

This is a major practical selling point: the seam between "which products" (factory) and "what the client does with them" is exactly where a test double slots in.
</details>

<details>
<summary><strong>Q17: [Advanced] How would you provide a "default fallback" factory when a specific family is unavailable?</strong></summary>

Wrap the lookup in a **decorator/proxy factory** that delegates to a specialized factory when one exists and to a `DefaultFactory` otherwise, so the system degrades gracefully instead of crashing on a missing family.

<details>
<summary>💻 Code</summary>

```java
class FallbackFactory implements CloudFactory {
    private final CloudFactory primary;      // may be null / partial
    private final CloudFactory fallback;     // always complete
    FallbackFactory(CloudFactory primary, CloudFactory fallback) {
        this.primary = primary; this.fallback = fallback;
    }
    public Compute createCompute() {
        return primary != null ? primary.createCompute() : fallback.createCompute();
    }
    public Storage createStorage() {
        return primary != null ? primary.createStorage() : fallback.createStorage();
    }
    public Network createNetwork() {
        return primary != null ? primary.createNetwork() : fallback.createNetwork();
    }
}
```
</details>

For registry factories, `registry.getOrDefault(key, defaultSupplier)` achieves the same at the product level. Be careful that the fallback family is still internally consistent.
</details>

<details>
<summary><strong>Q18: [Advanced] How does Abstract Factory apply to cloud-agnostic microservice architectures?</strong></summary>

A `CloudResourceFactory` interface defines `createStorage()`, `createCompute()`, `createQueue()`, etc. `AwsFactory` implements them with S3/EC2/SQS; `AzureFactory` with Blob/VM/Service Bus; `GcpFactory` with GCS/GCE/PubSub. Business services depend only on the abstract factory and abstract resource interfaces, so the entire application becomes **cloud-portable** — you switch providers by changing which factory is wired in (often via a Spring profile), not by editing service code.

The Staff-level caveats: the abstraction must be the **intersection** of provider capabilities (a leaky abstraction if one provider has features others lack), provider-specific tuning may force escape hatches, and the factory is frequently paired with a **DI container** (profiles/qualifiers) rather than hand-instantiated. Done well, it's how teams keep a multi-cloud or cloud-migration option open without rewriting business logic.
</details>

<details>
<summary><strong>Q19: [Advanced] Reflection vs. a Supplier registry for a dynamic Abstract Factory — trade-offs?</strong></summary>

**Reflection** (instantiate by class name from config) gives "zero-code" extensibility — list a class in a properties/YAML file and the factory creates it via `Class.forName(name).getDeclaredConstructor().newInstance()`. But it's **slower** (reflective construction, no JIT inlining), **bypasses compile-time checks** (a bad name or missing no-arg constructor fails at runtime), and complicates passing constructor arguments.

**Supplier registry** (`Map<Key, Supplier<T>>` with `Ctor::new`) is **type-safe**, **fast** (direct constructor calls via method references), and supports **arbitrary construction logic** (lambdas that pass parameters or do setup). The cost is an explicit registration step in code.

FAANG-level code strongly prefers the **registry**; reflection is reserved for truly open plugin systems where classes aren't known at build time (and even then, service-loading via `ServiceLoader` is often cleaner than raw reflection).
</details>

<details>
<summary><strong>Q20: [Trade-off] How does Abstract Factory compare to the Bridge pattern, and do they combine?</strong></summary>

They're orthogonal — one creational, one structural — and they combine well. **Abstract Factory** decides *which family of objects to create*; **Bridge** decouples an *abstraction* from its *implementation* so the two can vary independently.

In a combined design, an Abstract Factory **instantiates the implementor objects** that a Bridge then uses. For example, a `RenderingFactory` (abstract factory) produces a family of `Renderer` implementors (OpenGL vs. Vulkan), and a `Shape` abstraction (Bridge) holds a `Renderer` reference — the factory picks the implementor family, the bridge lets shapes and renderers evolve separately. The tell in an interview: if the question is "how do I create a matched set of implementations," that's Abstract Factory; if it's "how do I let abstraction and implementation vary independently," that's Bridge.
</details>

### 🏛️ Staff / Principal Engineer Deep-Dive

<details>
<summary><strong>SP1: When is introducing an Abstract Factory the wrong architectural call, and what's the deeper smell it can hide?</strong></summary>

It's wrong when there's **no real family** — when the objects don't actually have to be used together, an abstract factory imposes ceremony and a false coupling. It's also wrong when the product set is **highly volatile**: the interface-bloat tax turns every new product into a cross-cutting edit, and you spend more time maintaining the abstraction than it saves.

The deeper smell: reaching for Abstract Factory to paper over a **leaky cross-provider abstraction**. If you're forcing AWS and GCP behind one interface but the two diverge constantly (features one has and the other doesn't), the factory becomes a lowest-common-denominator abstraction riddled with `UnsupportedOperationException` and provider-specific escape hatches — a sign the abstraction boundary is wrong, not that you need more factories. The Staff move is to ask whether the "family" is genuine and stable; if not, prefer per-provider adapters chosen by configuration, or a capability-based design, over a rigid factory hierarchy.
</details>

<details>
<summary><strong>SP2: How do you evolve a classic Abstract Factory into a plugin architecture at scale?</strong></summary>

Progressively trade compile-time rigidity for runtime openness. (1) Replace fixed factory methods with a **registry** (`Map<Key, Supplier<T>>`), so products are data, not interface members. (2) Move registration to **`ServiceLoader`/SPI** or a DI container so third parties drop a JAR on the classpath and their factory self-registers via `META-INF/services` — no core edits. (3) Introduce **typed keys** (`Class<T>` or a sealed key enum) to recover some type safety lost by string keys. (4) Add **validation at startup** that each registered family is complete and coherent, converting "missing product" runtime failures into fail-fast boot errors. (5) Version the plugin contract so old plugins keep working as products are added. The end state resembles how JDBC drivers, SLF4J bindings, and Jackson modules self-register — an Abstract Factory whose family membership is discovered, not hard-coded.
</details>

<details>
<summary><strong>SP3: How does Abstract Factory relate to a DI container, and when should the container replace it?</strong></summary>

A DI container *is* a generalized, configurable factory. Where a hand-written Abstract Factory hard-codes which concrete products a family uses, a container wires them from configuration (Spring **profiles** select the family; **qualifiers** disambiguate products within it). For most enterprise Java, the container **replaces** the hand-rolled factory: annotate provider-specific beans, activate a profile, and the container hands the right family to injected clients.

You still write an explicit Abstract Factory when: you're **below/outside the container** (library code, no Spring), you need **runtime family switching per request** (a container's singleton scope is startup-bound), or the selection logic is **too dynamic** for static wiring. The Staff judgment: default to the container for whole-application family wiring; reserve the explicit pattern for library boundaries and dynamic, per-invocation family selection.
</details>

<details>
<summary><strong>SP4: A product in a family needs construction parameters that differ per family. How do you keep the abstraction clean?</strong></summary>

Adding parameters to the abstract factory's methods is risky — it leaks family-specific needs into the shared contract and constrains other families. Two cleaner options:

1. **Factory returns a Builder.** `createComputeBuilder()` returns a family-specific builder; the client configures common, family-agnostic options and calls `build()`. This separates *which family* (factory) from *how to configure* (builder), so parameters live where they belong.
2. **Pass a family-agnostic configuration/context object.** Define a `ProvisionSpec` value object with the cross-family options; each concrete factory interprets it in its own way. Family-specific extras go in a typed sub-config the concrete factory downcasts, guarded by validation.

Both preserve a narrow abstract-factory contract while letting concrete families handle their own construction complexity. Avoid the anti-pattern of a giant parameter list on the interface that only some families honor.
</details>

<details>
<summary><strong>SP5: How would you test and enforce family consistency across a large codebase where many factories exist?</strong></summary>

Consistency is a *system* property, so enforce it structurally and with automated checks, not code review alone. (1) **Architecture tests** (ArchUnit) that assert clients depend only on abstract factory/product interfaces, never concrete product classes — this prevents the "reach around the factory and `new` a product" leak. (2) **A family-completeness test** that, for every registered/concrete factory, asserts it produces a full, non-null set of products (catches partial families in the registry variant). (3) **A "no mixed families" invariant**: give products a `family()` tag and add a runtime assertion (dev/test builds) that a client's products all share one family — surfacing accidental cross-factory mixing. (4) **Contract tests** run against every concrete family so behavioral consistency (not just type consistency) is verified uniformly. (5) In plugin systems, **fail-fast startup validation** rejects incomplete or incompatible registered families before traffic hits them. The principle: make the consistency guarantee *checkable*, so it survives many contributors and many factories rather than relying on everyone remembering the rule.
</details>

---

## ⚡ Quick Revision

**One-liner:** Abstract Factory provides an interface for creating whole *families* of related objects without naming their concrete classes, guaranteeing that everything you get from one factory belongs together.

**The whole pattern in a paragraph:** When a client must create several kinds of objects that have to be mutually consistent (all one OS, one cloud provider, one theme, one DB driver), hard-coding `new` couples it to concretes and lets it mix incompatible parts. Abstract Factory defines one factory interface with a creation method per product type, and one concrete factory per family implementing them all; the client is injected with a factory and receives products through abstract interfaces, so it never sees a concrete class and family consistency is *structural* — one factory only ever yields its own family. Switching families is a one-line change (Dependency Inversion + Liskov). It's usually implemented *from* Factory Methods, and concrete factories are often stateless Singletons. The headline trade-off is the **Open/Closed asymmetry**: adding a new *family* is trivial, but adding a new *product type* forces edits to the interface and every concrete factory (interface bloat). The classic rigid form fits stable product sets; a **registry of `Supplier`s** (thread-safe via `ConcurrentHashMap`) removes the bloat and enables runtime/plugin extensibility at the cost of compile-time type safety; at enterprise scale a **DI container** (Spring profiles + qualifiers) generalizes and often replaces the hand-written factory. JDK examples: `DocumentBuilderFactory`, `Toolkit`, JDBC `Connection`.

**Top 5 interview answers to memorize:**

1. **"What is it / why?"** → An interface for creating *families* of related objects without naming concrete classes; one factory ⇒ one consistent family, so incompatible mixes are structurally impossible.
2. **"Abstract Factory vs. Factory Method?"** → Factory Method makes *one* product via subclass/inheritance; Abstract Factory makes a *family* via an object with several methods, chosen by composition — and it's typically built *from* Factory Methods.
3. **"Biggest downside?"** → The Open/Closed asymmetry / interface bloat: adding a *family* is easy, adding a *product type* forces changing the interface and every concrete factory.
4. **"How do you make it extensible at runtime?"** → Replace fixed methods with a `Map<Key, Supplier<Product>>` registry (ConcurrentHashMap), registering products/families dynamically — trading some compile-time type safety.
5. **"How does it help testing?"** → Inject a mock factory returning mock products; the client's logic is tested with no real products instantiated.

**Trigger words** (if the interviewer says these, think Abstract Factory): *"family of related objects," "must be used together / consistent," "cross-platform," "OS-specific widgets," "theme / look-and-feel," "cloud-provider abstraction," "database driver family," "factory of factories," "swap the whole set," "kit," "don't mix incompatible products."*



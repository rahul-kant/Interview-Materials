# Template Method Pattern ⭐⭐⭐ (Difficulty: 3/5 — the core is a single `final` method calling abstract steps, but the Java constructor/init traps, LSP-safe hooks, and the "prefer composition" trade-offs get genuinely subtle)

> **Category:** Behavioral Pattern (GoF)
> **Also known as:** — (no common alias; sometimes called a "skeleton method" or "hook method" design)

The Template Method pattern **defines the fixed skeleton of an algorithm in a base-class method and lets subclasses fill in specific steps** without changing the algorithm's overall structure. It is the pattern behind virtually every framework lifecycle you've ever overridden — `HttpServlet.service()`, JUnit `@Before`/`@After`, Spring's `JdbcTemplate`, `AbstractList` — and it is the canonical expression of the **Hollywood Principle**: "Don't call us, we'll call you."

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 0: Duplicated Code Across Similar Classes (Anti-pattern)](#variant-0-duplicated-code-across-similar-classes-anti-pattern)
   - [Variant 1: Basic Template Method with Abstract Primitives](#variant-1-basic-template-method-with-abstract-primitives)
   - [Variant 2: Add Hook Methods + Make the Template Method final](#variant-2-add-hook-methods--make-the-template-method-final)
   - [Variant 3: Reduce Inheritance Rigidity — Template Method + Strategy / Injected Steps](#variant-3-reduce-inheritance-rigidity--template-method--strategy--injected-steps)
   - [Variant 4: Production Framework-Style Base Class (Lifecycle + Exception Safety)](#variant-4-production-framework-style-base-class-lifecycle--exception-safety)
5. [🎨 Real-World Example](#-real-world-example)
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

> **GoF Definition:** *"Define the skeleton of an algorithm in an operation, deferring some steps to subclasses. Template Method lets subclasses redefine certain steps of an algorithm without changing the algorithm's structure."*

The Template Method pattern exists to **capture the invariant parts of an algorithm once, in a base class, while allowing the variant parts to be supplied by subclasses.** The base class defines a single method — the *template method* — that lays out the algorithm as an ordered sequence of steps. Some of those steps are implemented directly in the base class (the parts that never change); others are declared abstract (or as overridable hooks) and are filled in by each subclass. Because the template method controls the *order and structure* of the steps, the algorithm's shape is guaranteed to stay consistent across all subclasses, even as the individual steps vary.

The defining idea is **inversion of control**, popularly stated as the **Hollywood Principle — "Don't call us, we'll call you."** In ordinary code, *your* code calls into a library to get work done. With Template Method, the relationship flips: the base class (the framework) owns the high-level flow and *calls down into your subclass's* methods at the right moments. You don't invoke the algorithm step by step; you plug your steps into predetermined slots and the framework orchestrates them. This inversion is exactly why Template Method is the backbone of framework design — a framework must own the control flow (setup, dispatch, teardown, error handling) while letting application code customize the pieces that matter.

---

## 🎯 Problem

You have several classes (or several situations) that carry out **the same overall procedure in the same fixed order**, but a few individual steps differ between them. If you copy the whole procedure into each class and tweak the differing steps, you get **duplicated control flow**: the shared skeleton (and every bug and future change in it) is repeated N times, and nothing enforces that all the copies stay structurally identical.

**The pain points that lead you to Template Method:**

- **Duplicated algorithm structure.** Multiple classes repeat the same sequence of steps (open → read → transform → write → close), differing only in one or two steps. The boilerplate skeleton is copy-pasted everywhere.
- **No enforced invariant.** Nothing guarantees the steps run in the correct order, or that mandatory steps (validation, resource cleanup) actually happen — each copy can drift.
- **Cross-cutting flow concerns are scattered.** Setup, teardown, logging, timing, and exception handling that *should* wrap every run of the algorithm get re-implemented (often inconsistently, often forgotten) in each variant.
- **You want to let others customize only specific steps** without giving them the ability to break the overall flow.

**Concrete example scenarios:**

1. **Data-processing / ETL pipelines with a fixed order but varying steps.** Every import job does the same thing: connect to a source, read records, validate, transform, load into a destination, and close resources. A CSV importer and a JSON importer differ only in *how* they parse and map records — the surrounding pipeline is identical. Duplicating that pipeline per format is the smell.

2. **Framework lifecycle hooks.** A servlet container calls `init()`, then `service()` per request (dispatching to `doGet`/`doPost`), then `destroy()`. A test framework calls `setUp()`, the test, then `tearDown()`. The framework owns the lifecycle; you fill in the callbacks. This *is* Template Method.

3. **Sorting with a compare hook.** A generic sort algorithm (merge sort, quicksort) is fixed; only the *comparison* between two elements varies per type. The algorithm is the template; `compareTo`/`compare` is the deferred step. (This is also where Template Method shades into Strategy.)

4. **Build / test / request lifecycles.** A build tool runs `clean → compile → test → package → deploy` in order; individual projects override how a step behaves but not the order. Same for HTTP request handling: parse → authenticate → authorize → handle → render → log.

---

## ✅ Solution

The core idea, in plain language: **write the algorithm once as a single method in a base class. That method calls a series of smaller step-methods in a fixed order. Steps that are the same for everyone are implemented right there in the base class; steps that vary are declared `abstract` so each subclass must supply them, or are given a default (a "hook") that subclasses may optionally override. Make the top-level method `final` so subclasses can customize the steps but can never alter the skeleton.**

**Key structural elements:**

- **Abstract Class (the base):** declares the **template method** and defines the algorithm's skeleton by calling the primitive operations and hooks in a fixed sequence.
- **Template Method:** a **`final`** method containing the invariant algorithm structure. It calls the steps in order and typically should not be overridable — its whole job is to *lock in the flow*.
- **Primitive Operations (abstract steps):** `abstract` methods representing the mandatory, must-be-implemented variant steps. Subclasses are *forced* to provide them (the compiler enforces completeness).
- **Hook Methods:** methods with a **default (often empty or benign) implementation** in the base class that subclasses *may* override to inject optional behavior or influence control flow (e.g., a `boolean shouldCache()` hook the template consults). Hooks make the algorithm extensible without requiring every subclass to care.
- **Concrete Operations (shared steps):** ordinary methods fully implemented in the base class, providing the common behavior all subclasses reuse.
- **Concrete Subclasses:** implement the abstract primitive operations and optionally override hooks; they never touch the template method itself.

**The mechanism that makes it work:** *inversion of control via subtype polymorphism*. The base class's `final` template method calls step methods; at runtime, the JVM dispatches each call to the subclass's override (dynamic dispatch). So the base class dictates *when and in what order* steps run, while the subclass dictates *what each step does*. Making the template method `final` is what enforces the invariant — subclasses can fill the slots but cannot rearrange or skip them. This is the Hollywood Principle in code: the base class calls you; you don't call it.

---

## 💻 Implementation

We'll model a **data-import / ETL job** — every importer connects to a source, reads records, validates, transforms, and loads them, then closes resources. We evolve from copy-pasted pipelines (the anti-pattern that motivates Template Method) to a production framework-style base class with a full lifecycle and exception safety. Each variant fixes a specific weakness of the one before it.

### Variant 0: Duplicated Code Across Similar Classes (Anti-pattern)

**What's wrong with it:** Two importers do essentially the same thing in the same order, but each re-implements the *entire* pipeline. The shared skeleton (open → read → validate → transform → load → close) is copy-pasted. Any change to the flow — say, adding a validation step or fixing resource cleanup — must be made in every copy, and nothing enforces that the copies stay structurally identical. The variation between them is tiny (parsing and mapping); the duplication is huge.

<details>

<summary>💻 Click to expand code — the anti-pattern (duplicated pipelines)</summary>

```java
// DON'T DO THIS — the whole pipeline is copied; only parse/map actually differ.
class CsvImporter {
    void importData(String path) {
        System.out.println("Open " + path);            // duplicated
        List<String> raw = readLines(path);             // duplicated shape
        List<Record> records = new ArrayList<>();
        for (String line : raw) {                       // CSV-specific parse
            String[] cols = line.split(",");
            records.add(new Record(cols[0], cols[1]));
        }
        for (Record r : records) if (r.id == null) throw new IllegalStateException("bad"); // duplicated validation
        for (Record r : records) r.name = r.name.trim(); // duplicated transform
        save(records);                                   // duplicated load
        System.out.println("Close " + path);            // duplicated cleanup
    }
    // ... readLines, save ...
}

class JsonImporter {
    void importData(String path) {
        System.out.println("Open " + path);            // SAME code, copied
        String json = readWhole(path);
        List<Record> records = parseJson(json);          // JSON-specific parse (the ONLY real difference)
        for (Record r : records) if (r.id == null) throw new IllegalStateException("bad"); // copied again
        for (Record r : records) r.name = r.name.trim(); // copied again
        save(records);                                   // copied again
        System.out.println("Close " + path);            // copied again
    }
    // ... readWhole, parseJson, save ...
}
```
</details>

**Pros:** Trivial to understand in isolation; no abstraction to learn; each class is self-contained.
**Cons:** Massive structural duplication; a change to the pipeline must be replicated across every importer; nothing enforces the same order or the presence of mandatory steps (validation, cleanup); bugs fixed in one copy silently persist in the others. Violates DRY and Open/Closed.
**Mechanism (why it's fragile):** the algorithm's *structure* is expressed as inline code in each class rather than captured once, so there is no single source of truth for the flow — the very thing Template Method centralizes.

---

### Variant 1: Basic Template Method with Abstract Primitives

**What problem it solves:** Pull the invariant skeleton up into an abstract base class as a single `importData()` method that calls step-methods in a fixed order. The steps that never change (validate, transform, load) are implemented once in the base; the steps that vary (parse the source) are declared `abstract` so each subclass *must* supply them. Now the pipeline lives in exactly one place, and the compiler forces every subclass to implement the variant steps.

<details>

<summary>💻 Click to expand code — basic template method</summary>

```java
// Abstract base defines the skeleton once.
abstract class DataImporter {

    // The TEMPLATE METHOD: fixed algorithm structure.
    public void importData(String path) {
        List<Record> records = readRecords(path);  // varies  -> abstract
        validate(records);                          // invariant -> concrete
        transform(records);                         // invariant -> concrete
        load(records);                              // invariant -> concrete
    }

    // Primitive operation — MUST be supplied by each subclass.
    protected abstract List<Record> readRecords(String path);

    // Concrete operations — shared by all subclasses.
    protected void validate(List<Record> records) {
        for (Record r : records)
            if (r.id == null) throw new IllegalStateException("record missing id");
    }
    protected void transform(List<Record> records) {
        for (Record r : records) r.name = r.name.trim();
    }
    protected void load(List<Record> records) {
        System.out.println("Loaded " + records.size() + " records");
    }
}

// Concrete subclass — supplies ONLY the step that differs.
class CsvImporter extends DataImporter {
    protected List<Record> readRecords(String path) {
        // CSV-specific parsing only; the pipeline is inherited.
        List<Record> out = new ArrayList<>();
        for (String line : readLines(path)) {
            String[] c = line.split(",");
            out.add(new Record(c[0], c[1]));
        }
        return out;
    }
    private List<String> readLines(String path) { /* ... */ return List.of("1,Ada", "2,Alan"); }
}

class JsonImporter extends DataImporter {
    protected List<Record> readRecords(String path) {
        return parseJson(readWhole(path)); // JSON-specific parsing only
    }
    private String readWhole(String path) { /* ... */ return "[...]"; }
    private List<Record> parseJson(String json) { /* ... */ return List.of(new Record("1", "Ada")); }
}
```
</details>

**Pros:** The skeleton lives in one place (DRY); changing the pipeline is a one-line edit in the base; the compiler *forces* subclasses to implement the abstract primitives (no forgotten step); adding a new format = one new subclass overriding one method (Open/Closed).
**Cons:** The template method isn't `final` yet, so a subclass could still override `importData()` and wreck the flow; every subclass must implement *every* abstract step even when a sensible default exists; variation is bound at compile time via inheritance (one behavior per subclass).
**Mechanism:** *dynamic dispatch inside a fixed sequence* — `importData()` calls `readRecords()`, which the JVM routes to the concrete subclass's override, while the surrounding order and the shared steps come from the base. Structure from the base, content from the subclass.

---

### Variant 2: Add Hook Methods + Make the Template Method final

**What problem it solves:** Two gaps remain. (1) The template method should be **`final`** so no subclass can override the skeleton and break the invariant — this is the whole point of the pattern. (2) Some steps are *optional*: not every importer needs them. Introduce **hook methods** — methods with a default (often empty or benign) implementation that subclasses *may* override but aren't required to. A hook can also return a value that the template consults to alter control flow (e.g., "should I deduplicate?").

<details>

<summary>💻 Click to expand code — final template method + hooks</summary>

```java
abstract class DataImporter {

    // FINAL: subclasses fill steps but can NEVER change the skeleton.
    public final void importData(String path) {
        List<Record> records = readRecords(path);
        validate(records);
        if (shouldDeduplicate()) {          // HOOK influences control flow
            records = deduplicate(records);
        }
        transform(records);
        afterTransform(records);            // HOOK: optional extension point (default no-op)
        load(records);
    }

    // Mandatory primitive.
    protected abstract List<Record> readRecords(String path);

    // Hook with a DEFAULT — subclasses override only if they want to.
    protected boolean shouldDeduplicate() { return false; }   // default: off

    // Hook that is a no-op by default (a classic "do nothing" hook).
    protected void afterTransform(List<Record> records) { /* no-op; subclasses may extend */ }

    // Concrete shared steps.
    protected void validate(List<Record> records) { /* ... */ }
    protected void transform(List<Record> records) { for (Record r : records) r.name = r.name.trim(); }
    protected List<Record> deduplicate(List<Record> records) {
        return new ArrayList<>(new LinkedHashSet<>(records));
    }
    protected void load(List<Record> records) { System.out.println("Loaded " + records.size()); }
}

// A subclass that opts into a hook.
class CsvImporter extends DataImporter {
    protected List<Record> readRecords(String path) { /* parse CSV */ return List.of(); }
    @Override protected boolean shouldDeduplicate() { return true; } // opt-in: CSVs often have dupes
    @Override protected void afterTransform(List<Record> records) {
        System.out.println("CSV transform done: " + records.size()); // opt-in extension
    }
}
```
</details>

**Pros:** `final` locks the invariant flow — subclasses can *never* reorder or skip steps; **hooks** make the algorithm extensible without forcing every subclass to implement optional steps; boolean hooks let subclasses *influence* control flow declaratively; the base class provides sane defaults so subclasses stay minimal.
**Cons:** Distinguishing "abstract primitive (must implement)" from "hook (may override)" is a design judgment that's easy to get wrong; too many hooks make the flow hard to follow; hooks that subclasses override incorrectly can still violate expectations (LSP) even though they can't touch the skeleton.
**Mechanism:** *`final` skeleton + defaulted extension points*. `final` removes the ability to override the template, guaranteeing the flow; hook methods provide **default behavior in the base** that dynamic dispatch replaces *only* when a subclass chooses to override — the difference between `abstract` (compiler-enforced) and a concrete-with-default hook (optional) is precisely the "mandatory vs. optional step" distinction.

---

### Variant 3: Reduce Inheritance Rigidity — Template Method + Strategy / Injected Steps

**What problem it solves:** Classic Template Method binds each variation to a *subclass* at compile time. If you have two axes of variation (e.g., 3 parse formats × 4 load targets) you get a combinatorial explosion of subclasses, and you can't change a step at runtime. The fix is **composition over inheritance**: keep the template method for the fixed skeleton, but supply the varying steps as **injected functions/Strategies** (`Function`, `Consumer`, or small interfaces) instead of subclass overrides. This trades the compile-time simplicity of inheritance for runtime flexibility and composability.

<details>

<summary>💻 Click to expand code — template skeleton with injected step strategies</summary>

```java
import java.util.*;
import java.util.function.*;

// The skeleton is FIXED, but the varying steps are supplied as functions.
final class ImportPipeline {
    private final Function<String, List<Record>> reader;   // parse strategy (injected)
    private final Consumer<List<Record>> loader;           // load strategy (injected)
    private final boolean deduplicate;

    ImportPipeline(Function<String, List<Record>> reader,
                   Consumer<List<Record>> loader,
                   boolean deduplicate) {
        this.reader = reader;
        this.loader = loader;
        this.deduplicate = deduplicate;
    }

    // Template method: same invariant flow, steps come from injected functions.
    void importData(String path) {
        List<Record> records = reader.apply(path);           // was abstract; now injected
        validate(records);                                   // still shared/fixed
        if (deduplicate) records = new ArrayList<>(new LinkedHashSet<>(records));
        records.forEach(r -> r.name = r.name.trim());        // fixed transform
        loader.accept(records);                              // was abstract; now injected
    }
    private void validate(List<Record> records) {
        for (Record r : records) if (r.id == null) throw new IllegalStateException("bad");
    }
}

class PipelineDemo {
    public static void main(String[] args) {
        // Mix and match steps at RUNTIME — no new subclass needed per combination.
        ImportPipeline csvToDb = new ImportPipeline(
            path -> List.of(new Record("1", " Ada ")),        // CSV parse strategy
            recs -> System.out.println("DB load " + recs.size()), // DB load strategy
            true);
        csvToDb.importData("users.csv");

        ImportPipeline jsonToS3 = new ImportPipeline(
            path -> List.of(new Record("2", "Alan")),         // JSON parse strategy
            recs -> System.out.println("S3 load " + recs.size()), // S3 load strategy
            false);
        jsonToS3.importData("users.json");
    }
}
```
</details>

**Pros:** No subclass explosion — steps compose freely at runtime (3 readers × 4 loaders = 7 objects, not 12 subclasses); steps are swappable at runtime and independently testable/mockable; favors composition over inheritance, sidestepping the fragile-base-class problem; steps can be reused across different skeletons.
**Cons:** Loses the compiler's "you must implement this step" guarantee (a `null` function fails at runtime, not compile time); the skeleton no longer reads as a self-documenting subclass contract; slightly more wiring; if you truly have *one* dimension of variation and a stable hierarchy, plain inheritance is simpler. This is the **Template-Method-vs-Strategy trade-off** made explicit: inheritance (compile-time, one behavior per type) vs. composition (runtime, freely combinable).
**Mechanism:** *skeleton stays, steps become first-class objects*. Instead of dynamic dispatch to a subclass override, the template invokes an **injected function/Strategy** held in a field. The invariant flow is still owned centrally (Template Method's essence), but the variant steps are now *composed in* rather than *inherited* — the same inversion of control, implemented with delegation instead of subclassing.

---

### Variant 4: Production Framework-Style Base Class (Lifecycle + Exception Safety)

**Why it's recommended:** Real frameworks wrap the varying steps in a robust lifecycle: `setUp() → process() → tearDown()`, with **exception-safe `try/finally`** so cleanup *always* runs, timing/logging around the whole thing, and **guarded hooks** so a misbehaving subclass step doesn't leave resources leaked or the framework in a bad state. This is how `HttpServlet`, JUnit, and Spring's `*Template` classes are actually built.

<details>

<summary>💻 Click to expand code — framework-style lifecycle with try/finally + guarded hooks</summary>

```java
import java.util.*;

abstract class BatchJob {

    // FINAL template method: owns setup, processing, teardown, timing, error handling.
    public final void run(JobContext ctx) {
        long start = System.nanoTime();
        boolean setUpDone = false;
        try {
            onStart(ctx);              // guarded hook (default no-op)
            setUp(ctx);                // mandatory resource acquisition
            setUpDone = true;
            process(ctx);              // mandatory core work (the varying step)
            onSuccess(ctx);            // guarded hook
        } catch (RuntimeException ex) {
            onError(ctx, ex);          // guarded hook — subclasses log/alert; framework still cleans up
            throw ex;                  // rethrow after the hook; don't swallow
        } finally {
            if (setUpDone) {
                tearDown(ctx);         // ALWAYS runs if setUp succeeded — exception safety
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.println("[" + name() + "] finished in " + ms + "ms");
        }
    }

    // ---- Mandatory primitive operations (subclasses MUST implement) ----
    protected abstract void setUp(JobContext ctx);
    protected abstract void process(JobContext ctx);
    protected abstract void tearDown(JobContext ctx);
    protected abstract String name();

    // ---- Guarded hooks: default no-op, and we isolate their failures ----
    protected void onStart(JobContext ctx)   { /* no-op */ }
    protected void onSuccess(JobContext ctx) { /* no-op */ }
    protected void onError(JobContext ctx, RuntimeException ex) { /* no-op */ }
}

// A concrete job — fills the slots; never touches run().
class ReportJob extends BatchJob {
    private java.io.Closeable resource;
    protected String name() { return "nightly-report"; }
    protected void setUp(JobContext ctx)   { System.out.println("acquire DB connection"); resource = () -> System.out.println("close DB connection"); }
    protected void process(JobContext ctx) { System.out.println("generate + write report"); }
    protected void tearDown(JobContext ctx){ try { if (resource != null) resource.close(); } catch (Exception ignore) {} }
    @Override protected void onError(JobContext ctx, RuntimeException ex) { System.out.println("ALERT: job failed: " + ex.getMessage()); }
}

class JobContext { /* shared state passed through the lifecycle */ }
```
</details>

**Pros:** Cleanup via `finally` is **guaranteed** (only if `setUp` succeeded — no cleaning up what was never acquired); cross-cutting concerns (timing, logging, error alerting) are centralized once and applied to *every* job; hooks let subclasses observe lifecycle events without touching the flow; rethrowing after `onError` keeps the framework honest (it doesn't silently swallow failures). This is production-grade framework design.
**Cons:** More ceremony; the lifecycle contract (what runs when, what's guaranteed on failure) must be *documented* clearly or subclasses will misuse it; guarding hooks against their own exceptions adds complexity; deep lifecycles can be hard to reason about.
**Mechanism:** *`final` lifecycle skeleton + `try/finally` + guarded extension points*. The template method encodes not just the happy-path order but the **failure and cleanup semantics**, so every subclass inherits correct resource handling for free. The `setUpDone` flag is the subtle correctness detail — it ensures `tearDown` runs exactly when there is something to tear down. This is the difference between a toy Template Method and one you'd ship in a framework.

---

## 🎨 Real-World Example

A believable production scenario: a **generic batch ETL / data-import framework** used across a company's data platform. Every import job — whether it reads a **CSV file**, a **JSON API payload**, or a **fixed-width mainframe extract** — must follow the *same* governed pipeline: acquire resources, extract raw records, validate against a schema, transform to the canonical model, optionally deduplicate, load into the warehouse, and *always* release resources with timing and audit logging around the whole run. The pipeline (order, validation, cleanup, logging) is a company-wide invariant that must not vary per team; only **extract** and **map** legitimately differ per source. This is a textbook Template Method: the base `ImportJob` owns the governed skeleton (`final`), declares the varying steps as abstract primitives, offers hooks for optional behavior, and guarantees resource cleanup via `try/finally`.

<details>

<summary>💻 Click to expand full real-world example (ETL import framework with CSV + JSON + demo)</summary>

```java
import java.util.*;

// ---------- Domain model ----------
final class Record {
    String id;
    String name;
    Record(String id, String name) { this.id = id; this.name = name; }
    public boolean equals(Object o) { return o instanceof Record r && Objects.equals(id, r.id); }
    public int hashCode() { return Objects.hashCode(id); }
    public String toString() { return "Record(" + id + "," + name + ")"; }
}

final class ImportResult {
    int read, loaded, rejected;
    public String toString() { return "read=" + read + " loaded=" + loaded + " rejected=" + rejected; }
}

// ---------- Abstract base: the governed pipeline ----------
abstract class ImportJob {

    // TEMPLATE METHOD — final: the company-wide pipeline no team may alter.
    public final ImportResult run(String source) {
        long start = System.nanoTime();
        ImportResult result = new ImportResult();
        boolean opened = false;
        try {
            open(source);                                   // mandatory: acquire resources
            opened = true;

            List<String> raw = extract(source);             // VARIES per source (abstract)
            result.read = raw.size();

            List<Record> records = new ArrayList<>();
            for (String chunk : raw) {
                Record r = map(chunk);                      // VARIES per source (abstract)
                if (isValid(r)) records.add(r);             // shared validation (overridable hook)
                else result.rejected++;
            }

            if (shouldDeduplicate()) {                      // HOOK influences flow (default off)
                records = new ArrayList<>(new LinkedHashSet<>(records));
            }
            transform(records);                             // shared transform
            load(records);                                  // shared load
            result.loaded = records.size();
            onSuccess(result);                              // guarded hook
            return result;
        } catch (RuntimeException ex) {
            onError(source, ex);                            // guarded hook (alert/log), then rethrow
            throw ex;
        } finally {
            if (opened) close(source);                      // ALWAYS release if we opened
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.println("[" + sourceType() + "] " + source + " -> " + result + " (" + ms + "ms)");
        }
    }

    // ---- Abstract primitive operations: each source MUST supply these ----
    protected abstract String sourceType();
    protected abstract List<String> extract(String source);  // read raw chunks
    protected abstract Record map(String chunk);              // parse one chunk -> Record

    // ---- Hooks with defaults: subclasses MAY override ----
    protected boolean isValid(Record r)        { return r != null && r.id != null && !r.id.isBlank(); }
    protected boolean shouldDeduplicate()      { return false; }
    protected void onSuccess(ImportResult res) { /* no-op */ }
    protected void onError(String source, RuntimeException ex) { /* no-op */ }

    // ---- Concrete shared steps ----
    protected void open(String source)  { System.out.println("open " + source); }
    protected void close(String source) { System.out.println("close " + source); }
    protected void transform(List<Record> records) { for (Record r : records) if (r.name != null) r.name = r.name.trim(); }
    protected void load(List<Record> records)      { /* write to warehouse */ }
}

// ---------- Concrete subclass 1: CSV ----------
class CsvImportJob extends ImportJob {
    protected String sourceType() { return "CSV"; }
    protected List<String> extract(String source) {
        // Simulated file lines "id,name"; a blank id row will be rejected by isValid.
        return List.of("1, Ada ", "2,Alan", "2,Alan", ",Ghost");
    }
    protected Record map(String line) {
        String[] c = line.split(",", -1);
        return new Record(c[0].trim(), c.length > 1 ? c[1] : "");
    }
    @Override protected boolean shouldDeduplicate() { return true; }  // CSVs often have dupes
    @Override protected void onSuccess(ImportResult res) { System.out.println("CSV audit: " + res); }
}

// ---------- Concrete subclass 2: JSON ----------
class JsonImportJob extends ImportJob {
    protected String sourceType() { return "JSON"; }
    protected List<String> extract(String source) {
        // Simulated already-split JSON objects "id:name".
        return List.of("10:Grace", "11:Katherine");
    }
    protected Record map(String obj) {
        String[] kv = obj.split(":", 2);
        return new Record(kv[0], kv[1]);
    }
    @Override protected void onError(String source, RuntimeException ex) {
        System.out.println("PAGER: JSON import " + source + " failed: " + ex.getMessage());
    }
}

// ---------- Demo: same pipeline, different sources ----------
public class EtlDemo {
    public static void main(String[] args) {
        ImportJob csv = new CsvImportJob();
        ImportJob json = new JsonImportJob();

        csv.run("users.csv");
        // open users.csv
        // CSV audit: read=4 loaded=2 rejected=1     (blank id rejected; "2,Alan" deduped)
        // close users.csv
        // [CSV] users.csv -> read=4 loaded=2 rejected=1 (Xms)

        json.run("users.json");
        // open users.json
        // close users.json
        // [JSON] users.json -> read=2 loaded=2 rejected=0 (Xms)
    }
}
```
</details>

**Why this is a strong FAANG answer:** it shows (1) a **`final` template method** encoding a *governed, company-wide* pipeline that no subclass can reorder or bypass — the invariant is enforced by the language; (2) a clean separation of **abstract primitives** (`extract`, `map` — the parts that genuinely differ per source) from **concrete shared steps** (`open`, `transform`, `load`, `close`) and **hooks** (`isValid`, `shouldDeduplicate`, `onSuccess`, `onError`) that subclasses opt into; (3) **exception-safe resource handling** — `close()` runs in `finally` but only when `open()` succeeded, and errors are surfaced (via `onError` + rethrow) rather than swallowed; and (4) **inversion of control** — the framework calls the subclass's steps at the right moments, so a new source (Parquet, XML) is one small subclass, not a copied pipeline. This is exactly the shape of real ingestion frameworks, and it demonstrates the pattern's core value: *own the flow, defer the steps.*

---

## ✅ When to Use

<details>

<summary>📖 Click to expand</summary>

- You have **several classes that share the same algorithm structure** but differ in a few steps — factor the invariant skeleton into a base class and defer the varying steps.
- You want to **control the order and completeness of an algorithm** while letting subclasses customize specific steps (the base guarantees the flow; subclasses fill slots).
- You're **building a framework** and need to own the control flow (setup, dispatch, teardown, error handling) while application code plugs in behavior — the Hollywood Principle / inversion of control.
- You want to **localize common behavior** to eliminate duplication across similar classes (DRY) and centralize cross-cutting flow concerns (logging, timing, resource cleanup).
- You need **optional extension points** (hooks) that subclasses may override but aren't required to, without changing the algorithm.
- You want to **prevent subclasses from changing the algorithm's structure** — mark the template method `final`.

</details>

## ❌ When NOT to Use

<details>

<summary>📖 Click to expand</summary>

- **There is no shared skeleton** — the "algorithms" don't actually share a fixed structure, so forcing them under one template creates a leaky, over-generalized base class.
- **You need runtime-swappable behavior** or to combine variations along multiple axes — prefer **Strategy / composition** (inheritance binds one behavior per subclass at compile time and explodes combinatorially).
- **The variation is a single, cleanly-isolated function** — a lambda/`Comparator`/injected `Function` is lighter than a subclass hierarchy.
- **Deep or unstable inheritance hierarchies** would result — Template Method couples subclasses tightly to the base class's evolving internals (the *fragile base class* problem); composition is safer at scale.
- **Subclasses would need to override the template method itself** to work — that's a sign the "invariant" isn't really invariant, and the abstraction is wrong.
- **Only one implementation exists and no others are foreseen** — introducing an abstract base "just in case" is speculative generality.

</details>

## 🎯 Pros and Cons

<details>

<summary>📖 Click to expand</summary>

**Pros**

- **Eliminates duplication** — the invariant algorithm structure lives in exactly one place (DRY); subclasses supply only what differs.
- **Enforces the algorithm's structure** — a `final` template method guarantees the order and presence of steps; subclasses can't reorder or skip them.
- **Inversion of control** — the base class owns the flow and calls into subclasses; the foundation of framework/lifecycle design (Hollywood Principle).
- **Open/Closed for steps** — add a new variant by adding a subclass (or injected step), without touching the skeleton or existing subclasses.
- **Centralizes cross-cutting concerns** — timing, logging, and especially resource cleanup (`try/finally`) are written once and inherited by every variant.
- **Compiler-enforced completeness** — `abstract` primitives force subclasses to implement mandatory steps.

**Cons**

- **Inheritance coupling** — subclasses are tightly bound to the base class's structure; changing the base can break subclasses (the *fragile base class* problem).
- **Rigid, compile-time variation** — one behavior per subclass; no runtime swapping and combinatorial subclass explosion across multiple variation axes (Strategy handles this better).
- **LSP hazards** — a subclass step that violates the base's implicit contract can break the algorithm even though it can't touch the skeleton.
- **Java constructor trap** — calling an overridable step from a constructor runs the subclass override *before* the subclass is initialized (see Pitfalls).
- **Readability cost** — the flow is split across base and subclasses; you must read both to understand a run.
- **Too many hooks/abstract steps** make subclasses fragile and the contract hard to follow.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>

<summary>📖 Click to expand</summary>

Template Method is most often contrasted with **Strategy** (its closest cousin) and **Factory Method** (frequently a *specialization* of it), and occasionally with **Bridge**. The distinctions are about *how* variation is achieved and *when* it's bound.

| Pattern | How variation is achieved | Binding time | Structure | Key tell |
|---|---|---|---|---|
| **Template Method** | **Inheritance** — subclass overrides steps of a base-class algorithm | **Compile-time** (one behavior per subclass) | One class hierarchy; base owns the flow | "Skeleton in the base, steps in subclasses; base calls you" |
| **Strategy** | **Composition** — client injects an interchangeable algorithm object | **Runtime** (swap the strategy) | Separate strategy objects held by a context | "The whole algorithm is a pluggable object you can swap" |
| **Factory Method** | Subclass overrides a method that **creates an object** | Compile-time | A Factory Method *is often a primitive step of a Template Method* | "The deferred step *constructs* something" |
| **Bridge** | Composition — separate an abstraction from its implementation so both vary | Runtime | Two independent hierarchies linked by delegation | "Two dimensions vary independently via delegation" |

**Template Method vs. Strategy (the big one):** both let you vary behavior behind a stable interface, but Template Method uses **inheritance** and defers *individual steps* of an algorithm whose skeleton is fixed in the base class — variation is chosen by *which subclass* you instantiate (compile-time). Strategy uses **composition** and swaps the *entire algorithm* as an injected object at *runtime*, and one context can use different strategies over its lifetime. Template Method: "here's the algorithm; override these steps." Strategy: "here's a slot; plug in any algorithm." Strategy avoids the fragile-base-class and combinatorial-subclass problems, which is why modern APIs often prefer it — but Template Method is simpler when there's genuinely one fixed skeleton with a small, stable set of variant steps.

**Template Method vs. Factory Method:** they compose rather than compete. A **Factory Method is frequently one of the primitive operations** that a Template Method defers — the template says "at this point, create the thing you need" and the subclass's factory method decides *which* concrete thing. GoF explicitly notes "Factory Methods are often called by template methods."

**Template Method vs. Bridge:** Bridge separates an abstraction from its implementation so the two can vary independently *via composition/delegation across two hierarchies*. Template Method varies *steps within a single algorithm via inheritance*. Bridge is structural (about decoupling two dimensions); Template Method is behavioral (about a fixed algorithm with pluggable steps).

</details>

## 📊 Comparison Table of Variants

<details>

<summary>📖 Click to expand</summary>

| Axis | V0: Duplicated | V1: Basic Abstract | V2: Hooks + final | V3: Injected Steps (Strategy) | V4: Framework Lifecycle |
|---|---|---|---|---|---|
| **Duplication removed** | ❌ (copy-paste) | ✅ | ✅ | ✅ | ✅ |
| **Skeleton enforced** | ❌ | ⚠️ (not `final`) | ✅ (`final`) | ✅ (no override path) | ✅ (`final`) |
| **Variation binding** | N/A | Compile-time | Compile-time | **Runtime** | Compile-time |
| **Multiple variation axes** | Copy per combo | Subclass explosion | Subclass explosion | ✅ (compose freely) | Subclass explosion |
| **Optional steps** | Manual | ❌ (all abstract) | ✅ (hooks) | ✅ (flags/functions) | ✅ (guarded hooks) |
| **Coupling** | None (but duplicated) | Inheritance | Inheritance | **Composition (loose)** | Inheritance |
| **Testability of steps** | Poor | Moderate (subclass) | Moderate | **High (inject mocks)** | Moderate |
| **Inheritance depth** | Flat | Shallow | Shallow | **None (flat)** | Shallow–moderate |
| **Exception/resource safety** | Ad hoc per copy | Not addressed | Not addressed | Caller's job | ✅ (`try/finally`) |
| **Best for** | Nothing (smell) | A single fixed skeleton | Extensible fixed algorithm | Multi-axis / runtime variation | Real frameworks/lifecycles |

</details>

## 💡 Common Pitfalls

<details>

<summary>📖 Click to expand</summary>

### Pitfall 1: Template method not `final` — subclass overrides the skeleton

**What goes wrong:** If the template method is left overridable, a subclass can override it and reorder, skip, or replace the invariant steps — defeating the entire point of the pattern (guaranteeing the flow). Worse, it can silently omit mandatory steps like validation or cleanup.

<details>

<summary>💻 Click to expand — the failure and the fix</summary>

```java
// BUG: template method is overridable, so a subclass can wreck the flow.
abstract class Job { public void run() { setUp(); process(); tearDown(); } }
class RogueJob extends Job {
    @Override public void run() { process(); }   // skips setUp AND tearDown -> resource leak
    protected void process() { /* ... */ }
    protected void setUp() {} protected void tearDown() {}
}

// FIX: make the template method final; subclasses fill steps, never the skeleton.
abstract class SafeJob {
    public final void run() { setUp(); process(); tearDown(); } // final locks the flow
    protected abstract void setUp(); protected abstract void process(); protected abstract void tearDown();
}
```
</details>

### Pitfall 2: Too many abstract steps → fragile, high-friction subclasses

**What goes wrong:** If the base declares a dozen abstract primitives, every subclass must implement all twelve even when most have obvious defaults — huge boilerplate, and each new subclass is a chore. It also makes the base's contract brittle: adding a new abstract step breaks *every* existing subclass.

<details>

<summary>💻 Click to expand — the fix</summary>

```java
// FIX: make truly-mandatory steps abstract; give everything else a sensible default HOOK.
abstract class ReportBuilder {
    public final void build() { header(); body(); footer(); }
    protected abstract void body();              // the ONE step that must differ
    protected void header() { /* sensible default */ }  // hook: override only if needed
    protected void footer() { /* sensible default */ }  // hook: override only if needed
}
// Adding a new optional step later = a new hook with a default => zero existing subclasses break.
```
</details>

### Pitfall 3: Calling an overridable method from a constructor (Java initialization-order trap)

**What goes wrong (Java-specific and nasty):** If the base constructor calls a step method that a subclass overrides, the override runs **before the subclass's fields are initialized** — because in Java the superclass constructor runs first, then the subclass's field initializers and constructor body. The overridden method sees `null`/default fields.

<details>

<summary>💻 Click to expand — the trap and the fix</summary>

```java
// BUG: base constructor calls an overridable step before the subclass is initialized.
abstract class Base {
    Base() { init(); }                    // called during Base construction...
    protected abstract void init();
}
class Sub extends Base {
    private String name = "configured";   // ...but THIS runs AFTER Base() completes
    protected void init() { System.out.println(name.toUpperCase()); } // NPE: name is still null!
}

// FIX options:
// 1) Never call overridable methods from a constructor. Use a separate init()/start() the caller invokes,
//    or a factory method that constructs THEN initializes.
// 2) Make the called method private/static/final (non-overridable) so no dynamic dispatch occurs.
abstract class BaseFixed {
    BaseFixed() { /* do NOT call overridable steps here */ }
    public final void start() { init(); }  // caller invokes start() after construction is complete
    protected abstract void init();
}
```
</details>

### Pitfall 4: Deep inheritance hierarchies (fragile base class)

**What goes wrong:** Stacking Template Methods across many inheritance levels (`A → B → C → D`, each refining steps) makes behavior nearly impossible to trace and couples every level to the internals of the ones above. A change in a base's step ordering or a step's semantics can silently break distant descendants.

<details>

<summary>💻 Click to expand — the fix</summary>

```java
// FIX: prefer shallow hierarchies; push variation into injected collaborators (composition).
final class Pipeline {                       // no deep hierarchy
    private final Step extract, load;        // inject varying steps
    Pipeline(Step extract, Step load) { this.extract = extract; this.load = load; }
    void run() { extract.run(); transform(); load.run(); } // fixed skeleton, composed steps
    private void transform() { /* shared */ }
}
interface Step { void run(); }
// One class, flat; combine steps freely instead of subclassing at every level.
```
</details>

### Pitfall 5: LSP violations from hooks / overridden steps

**What goes wrong:** A subclass overrides a step (or hook) in a way that violates the base algorithm's implicit contract — e.g., a step expected to be non-throwing now throws, or a `boolean` hook returns something that leaves the flow in an impossible state, or an overridden `validate()` weakens validation and lets bad data through. The skeleton is intact, but the *semantics* are broken (a Liskov Substitution Principle violation).

<details>

<summary>💻 Click to expand — the fix</summary>

```java
// FIX: document each step's contract (pre/postconditions, may-it-throw, allowed return values),
// and give hooks safe, well-defined defaults. Consider guarding hook calls in the template.
protected boolean shouldRetry() { return false; } // contract: pure, no side effects, safe default
// In the template, treat hook output defensively and never let a hook silently corrupt invariants:
if (shouldRetry()) { /* bounded retry */ }         // template still controls the bound/limits
```
</details>

</details>

---

## 🎓 Interview Tips

**What interviewers commonly ask:**

<details>

<summary>📖 Click to expand</summary>

- "What is the Template Method pattern and how does it differ from Strategy?" (Inheritance + fixed skeleton with deferred steps vs. composition + swappable whole algorithm; compile-time vs. runtime.)
- "Give a real JDK/framework example." (`HttpServlet.service()` → `doGet`/`doPost`; `AbstractList`; JUnit `setUp`/`tearDown`; Spring `JdbcTemplate`.)
- "Why should the template method be `final`?" (To enforce the invariant skeleton — the whole point.)
- "What's the difference between an abstract primitive operation and a hook method?" (Mandatory-must-implement vs. optional-with-default.)
- "What's the Hollywood Principle?" ("Don't call us, we'll call you" — inversion of control; the framework calls your steps.)
- "What's the danger of calling an overridable method from a constructor in Java?" (The Java init-order trap — the override runs before subclass fields are set.)

</details>

**What you should proactively mention even if not asked:**

<details>

<summary>📖 Click to expand</summary>

- Template Method **is inversion of control** — name the **Hollywood Principle** explicitly; it signals you understand *why* frameworks use it.
- Distinguish the three kinds of step: **concrete (shared)**, **abstract primitive (mandatory)**, and **hook (optional, defaulted)** — and note that the abstract-vs-hook choice encodes "mandatory vs. optional."
- **Make the template method `final`**; that's what turns "a base class with some methods" into the Template Method *pattern* by locking the invariant.
- **Factory Method is often a step of a Template Method** — GoF says template methods call factory methods.
- The modern trade-off: **prefer composition/Strategy over inheritance** when you need runtime flexibility or multiple variation axes; Template Method shines when there's one stable skeleton.
- Call out the **Java constructor/overridable-method trap** and the **fragile base class problem** — these show senior-level awareness of inheritance's costs.
- Real frameworks add **`try/finally` for resource safety** and **guarded lifecycle hooks** — the toy version isn't production-grade.

</details>

## 🔗 Related Patterns

<details>

<summary>📖 Click to expand</summary>

- **Strategy** — the composition-based alternative: swap the whole algorithm at runtime instead of overriding steps via inheritance. The closest cousin and the usual "prefer this at scale" answer.
- **Factory Method** — a specialization of Template Method: the deferred step *creates* an object. GoF: "Factory Methods are often called by template methods."
- **Abstract Factory** — a factory step within a template can itself be an Abstract Factory producing families of related objects.
- **Bridge** — an alternative decoupling using composition across two independently-varying hierarchies (vs. Template Method's single-hierarchy step overriding).
- **Hook / Observer** — Template Method's hook methods are a static, subclass-based form of extension; Observer offers a dynamic, subscription-based alternative for reacting to lifecycle events.
- **Composite** — the shared algorithm may traverse a Composite structure while deferring per-node behavior to steps.
- **Command** — a framework's lifecycle template may execute steps that are themselves Command objects (deferred/queued work).

</details>

## 📚 Library/Framework Implementation

**1. `javax.servlet.http.HttpServlet.service()` (the canonical framework Template Method).** The servlet container calls `service(req, resp)`, which is a template method that inspects the HTTP method and **dispatches to `doGet`, `doPost`, `doPut`, `doDelete`, etc.** — the primitive operations you override. You never call `service()` yourself; the container does (Hollywood Principle), and you fill in only the method(s) you care about. `doGet`/`doPost` have default implementations that return HTTP 405, so they're effectively hooks with a benign default.

<details>

<summary>💻 Click to expand — HttpServlet as Template Method</summary>

```java
public class HelloServlet extends HttpServlet {
    // service() (in HttpServlet) is the template method: it dispatches to doGet/doPost.
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().println("Hello, GET");   // fill in ONE step
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().println("Hello, POST");  // fill in another step
    }
    // service() decides which to call based on req.getMethod(); we never touch service().
}
```
</details>

**2. `java.util.AbstractList` / `AbstractMap` / `AbstractSet` (skeletal implementations) and `java.io.InputStream.read()`.** The JDK's "skeletal" abstract classes implement most of a collection's methods in terms of a few primitive operations you must supply. `AbstractList` implements `iterator()`, `indexOf()`, `contains()`, `equals()`, etc. entirely in terms of the abstract `get(int)` and `size()` — those two are your primitive operations; everything else is the inherited algorithm. Likewise, `InputStream` implements the bulk `read(byte[], int, int)` (and `read(byte[])`) in terms of the single abstract `read()` — subclasses supply one byte, the template supplies buffered reading.

<details>

<summary>💻 Click to expand — AbstractList skeletal implementation</summary>

```java
// Supply the two primitives; inherit iterator(), contains(), indexOf(), subList(), etc.
class RangeList extends java.util.AbstractList<Integer> {
    private final int from, to;
    RangeList(int from, int to) { this.from = from; this.to = to; }
    @Override public Integer get(int index) {          // primitive operation 1
        if (index < 0 || index >= size()) throw new IndexOutOfBoundsException();
        return from + index;
    }
    @Override public int size() { return to - from; }  // primitive operation 2
    // get() + size() are enough: AbstractList's template methods provide the rest.
}
// new RangeList(1, 5).contains(3) -> true, using inherited algorithm built on get()/size().
```
</details>

**3. Spring `JdbcTemplate` / `AbstractApplicationContext.refresh()` and JUnit lifecycle.** Spring's `JdbcTemplate.execute()` owns the invariant JDBC flow — acquire connection, create statement, **execute (your callback)**, handle exceptions/translate, and **close resources in `finally`** — deferring only the statement-specific work to a callback (a `PreparedStatementCallback`, which is the "step"). `AbstractApplicationContext.refresh()` is a large `final` template method orchestrating the container startup lifecycle with well-defined `postProcess*` hooks. JUnit's runner calls `@BeforeEach` → test → `@AfterEach` as a lifecycle template.

<details>

<summary>💻 Click to expand — JdbcTemplate template method with callback step</summary>

```java
// JdbcTemplate owns the connect/execute/cleanup skeleton; you supply only the SQL "step".
JdbcTemplate jdbc = new JdbcTemplate(dataSource);
List<String> names = jdbc.query(
    "SELECT name FROM users WHERE active = ?",       // template runs connect -> prepare -> execute -> map -> close
    (rs, rowNum) -> rs.getString("name"),            // RowMapper: the deferred step you fill in
    true);
// You never write the try/finally to close the Connection/Statement/ResultSet — the template does,
// guaranteeing cleanup even on exception. This is Template Method with an injected callback (Strategy-ish).
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>

<summary><strong>Q1: [Conceptual] What is the Template Method pattern and what problem does it solve?</strong></summary>

Template Method is a **behavioral** GoF pattern that **defines the skeleton of an algorithm in a base-class method, deferring some steps to subclasses.** The base class's *template method* lays out the algorithm as a fixed sequence of step-calls; steps that never vary are implemented in the base, and steps that vary are declared abstract (or as overridable hooks) for subclasses to fill in.

It solves the problem of **duplicated algorithm structure**: when several classes carry out the same procedure in the same order but differ in a few steps, copying the whole procedure into each class duplicates the skeleton and lets the copies drift. Template Method captures the invariant skeleton *once* and lets subclasses supply only what differs.

The key idea is **inversion of control** — the base class (framework) owns the high-level flow and calls *down* into the subclass's steps at the right moments (the **Hollywood Principle**, "Don't call us, we'll call you"). This is why it's the backbone of framework and lifecycle design.
</details>

<details>

<summary><strong>Q2: [Conceptual] What are the participants and the kinds of methods in Template Method?</strong></summary>

Two participants and three kinds of methods:

**Participants:**
1. **AbstractClass** — declares the template method and defines the algorithm skeleton by calling steps in order.
2. **ConcreteClass** — implements the abstract primitive operations (and optionally overrides hooks); never touches the template method.

**Kinds of methods inside the base:**
- **Template method** — the `final` method holding the invariant algorithm structure (the sequence of step-calls).
- **Primitive operations** — `abstract` methods for mandatory variant steps; subclasses *must* implement them (compiler-enforced).
- **Hook methods** — methods with a default (often empty/benign) implementation that subclasses *may* override to add optional behavior or influence control flow.
- **Concrete operations** — fully-implemented shared steps reused by all subclasses.

The critical relationship: the base owns *when/in what order* steps run; subclasses own *what each step does*.
</details>

<details>

<summary><strong>Q3: [Conceptual] Explain the Hollywood Principle and how Template Method embodies it.</strong></summary>

The **Hollywood Principle** is "**Don't call us, we'll call you**" — a statement of **inversion of control**. In normal code, *your* code calls into a library to get work done. With inversion of control, the relationship flips: the framework/base class owns the control flow and calls *your* code at the right moments.

Template Method embodies this literally: you don't invoke the algorithm step by step. Instead, you plug your step implementations into predetermined slots (by overriding abstract primitives or hooks), and the base class's template method orchestrates *when* each of your steps runs. The base "calls you."

This is exactly why frameworks use it: a framework must own the lifecycle (setup, dispatch, teardown, error handling) to provide guarantees (resources always cleaned up, steps always in order), while letting application code customize the pieces that matter. `HttpServlet` calling your `doGet`, JUnit calling your `@BeforeEach` — both are the Hollywood Principle via Template Method.
</details>

<details>

<summary><strong>Q4: [Conceptual] What's the difference between an abstract primitive operation and a hook method?</strong></summary>

Both are extension points the template method calls, but they encode different obligations:

- An **abstract primitive operation** has *no* implementation in the base (`abstract`). Subclasses are **required** to implement it — the compiler enforces this. Use it for steps that are **mandatory and genuinely differ** per subclass (e.g., "parse this source format").
- A **hook method** has a **default implementation** in the base (often empty, or a benign default like `return false`). Subclasses **may** override it but aren't required to. Use it for **optional** behavior or to let subclasses *influence* the flow (e.g., a `boolean shouldCache()` hook the template consults).

The design signal: `abstract` = "you must supply this step"; hook = "this step is optional / has a sensible default." Choosing well matters — too many abstract steps make subclasses tedious and brittle; hooks with defaults keep subclasses minimal and let you add extension points later without breaking existing subclasses.
</details>

<details>

<summary><strong>Q5: [Implementation] Implement a basic Template Method: a beverage maker for coffee and tea.</strong></summary>

<details>

<summary>💻 Click to expand solution</summary>

```java
abstract class Beverage {
    // Template method — the fixed recipe. final so nobody rearranges it.
    public final void prepare() {
        boilWater();
        brew();               // varies -> abstract
        pourInCup();
        if (wantsCondiments()) // hook influences flow
            addCondiments();  // varies -> abstract
    }
    protected abstract void brew();
    protected abstract void addCondiments();
    protected boolean wantsCondiments() { return true; } // hook, default on
    private void boilWater() { System.out.println("Boiling water"); }
    private void pourInCup() { System.out.println("Pouring into cup"); }
}

class Coffee extends Beverage {
    protected void brew() { System.out.println("Dripping coffee through filter"); }
    protected void addCondiments() { System.out.println("Adding sugar and milk"); }
}
class Tea extends Beverage {
    protected void brew() { System.out.println("Steeping the tea"); }
    protected void addCondiments() { System.out.println("Adding lemon"); }
    @Override protected boolean wantsCondiments() { return false; } // opt out via hook
}

class Demo {
    public static void main(String[] a) {
        new Coffee().prepare(); // boil, drip, pour, sugar+milk
        new Tea().prepare();    // boil, steep, pour (no condiments)
    }
}
```
</details>

This is the classic Head-First example: `prepare()` is the template method, `brew()`/`addCondiments()` are abstract primitives, and `wantsCondiments()` is a hook that Tea uses to skip condiments.
</details>

<details>

<summary><strong>Q6: [Implementation] Show a template method that guarantees resource cleanup with try/finally.</strong></summary>

<details>

<summary>💻 Click to expand solution</summary>

```java
abstract class ResourceTask {
    public final void execute() {
        boolean opened = false;
        try {
            open();                 // acquire
            opened = true;
            doWork();               // the varying step
        } finally {
            if (opened) close();    // ALWAYS release if we opened — even on exception
        }
    }
    protected abstract void open();
    protected abstract void doWork();
    protected abstract void close();
}

class FileTask extends ResourceTask {
    protected void open()   { System.out.println("open file"); }
    protected void doWork() { System.out.println("process file"); /* may throw */ }
    protected void close()  { System.out.println("close file"); }
}
```
</details>

The value: cleanup logic is written **once** in the base and inherited by every subclass, so no subclass can forget it or get it wrong. The `opened` flag ensures `close()` runs only when `open()` succeeded — you never clean up a resource you never acquired. This is exactly the shape of Spring's `JdbcTemplate` and `try-with-resources`-style frameworks.
</details>

<details>

<summary><strong>Q7: [Implementation] Implement generic sort with a deferred compare step, then contrast it with Strategy.</strong></summary>

<details>

<summary>💻 Click to expand solution</summary>

```java
// Template Method style: fixed sort skeleton, abstract compare step.
abstract class Sorter<T> {
    public final void sort(List<T> list) {         // template: bubble skeleton (illustrative)
        for (int i = 0; i < list.size(); i++)
            for (int j = 0; j < list.size() - 1 - i; j++)
                if (compare(list.get(j), list.get(j + 1)) > 0)
                    Collections.swap(list, j, j + 1);
    }
    protected abstract int compare(T a, T b);       // deferred step
}
class IntSorter extends Sorter<Integer> {
    protected int compare(Integer a, Integer b) { return Integer.compare(a, b); }
}

// Strategy style (what the JDK actually does): inject the comparison as an object.
List<Integer> nums = new ArrayList<>(List.of(3, 1, 2));
nums.sort(Comparator.naturalOrder());   // Comparator IS a Strategy, swappable at runtime
```
</details>

The Template Method version binds the comparison to a *subclass* at compile time; the Strategy version (`Collections.sort(list, comparator)`) injects the comparison as a swappable object at runtime. The JDK chose Strategy (`Comparator`) precisely because you want to sort the *same* type different ways at runtime without a subclass per ordering — a great illustration of why composition often wins over inheritance for single-step variation.
</details>

<details>

<summary><strong>Q8: [Implementation] Implement a framework-style lifecycle base class with setUp/process/tearDown and hooks.</strong></summary>

<details>

<summary>💻 Click to expand solution</summary>

```java
abstract class Lifecycle {
    public final void run() {
        boolean up = false;
        try {
            onStart();               // hook (default no-op)
            setUp(); up = true;      // mandatory
            process();               // mandatory core work
            onSuccess();             // hook
        } catch (RuntimeException e) {
            onError(e);              // hook: observe failure, don't swallow
            throw e;
        } finally {
            if (up) tearDown();      // guaranteed cleanup
        }
    }
    protected abstract void setUp();
    protected abstract void process();
    protected abstract void tearDown();
    protected void onStart()   {}
    protected void onSuccess() {}
    protected void onError(RuntimeException e) {}
}
```
</details>

This mirrors JUnit (`@BeforeEach`/test/`@AfterEach`) and servlet lifecycles. The key production details: the template method is `final`; `tearDown` runs in `finally` guarded by `up` so it only cleans up what was set up; `onError` lets subclasses react but the exception is **rethrown** so the framework doesn't silently swallow failures; and hooks default to no-ops so subclasses implement only what they need.
</details>

<details>

<summary><strong>Q9: [Breaking] What happens if you call an overridable step method from the base-class constructor?</strong></summary>

You hit the **Java initialization-order trap**. In Java, constructing a subclass runs the **superclass constructor first**, *then* the subclass's field initializers and constructor body. If the superclass constructor calls a method the subclass overrides, the override executes **before the subclass's fields are initialized** — so it sees `null`/`0`/`false` defaults, often causing a `NullPointerException` or silently wrong behavior.

<details>

<summary>💻 Click to expand — the trap</summary>

```java
abstract class Base { Base() { step(); } protected abstract void step(); }
class Sub extends Base {
    private String data = "ready";       // runs AFTER Base()'s constructor
    protected void step() { System.out.println(data.length()); } // NPE: data == null here
}
new Sub(); // boom
```
</details>

**Fixes:** (1) never call overridable methods from a constructor — expose a separate `init()`/`start()` the caller invokes after construction, or use a static factory that constructs then initializes; (2) make the called method `final`/`private`/`static` so there's no dynamic dispatch. This is a favorite senior-level "gotcha" and Effective Java Item 19 warns about it explicitly.
</details>

<details>

<summary><strong>Q10: [Breaking] How does a subclass break a Template Method even when it can't override the template method itself?</strong></summary>

Making the template method `final` locks the *structure*, but subclasses can still break the *semantics* by violating the implicit **contract** of the steps they override — a Liskov Substitution Principle violation. Examples: (1) an overridden `validate()` that weakens checks and lets bad data through; (2) a step expected to be non-throwing that now throws, blowing up the whole algorithm (or skipping later steps); (3) a `boolean` hook returning a value that leaves the flow in an impossible state; (4) a step with a side effect the base didn't anticipate (mutating shared state the next step relies on); (5) a hook that never terminates or is not idempotent when the template may call it more than once.

The skeleton is intact, but the algorithm is now wrong. **Fixes:** document each step's contract (preconditions, postconditions, whether it may throw, allowed return values); give hooks safe defaults; and have the template call hooks defensively (bounding retries, catching/isolating hook exceptions where appropriate) so a misbehaving step can't silently corrupt invariants.
</details>

<details>

<summary><strong>Q11: [Breaking] Why are deep Template Method inheritance hierarchies dangerous, and what's the fragile base class problem?</strong></summary>

The **fragile base class problem**: because subclasses depend on the base class's *internal* structure (the order and semantics of the steps the template calls), a change to the base — reordering steps, adding a step, changing when a hook is invoked, or altering a shared step's behavior — can **silently break distant subclasses** that assumed the old behavior. The base and its subclasses are tightly coupled through implementation details, not just a stable interface.

Deep hierarchies (`A → B → C → D`, each refining the template or steps) amplify this: behavior is smeared across levels, hard to trace, and a change at the top can ripple unpredictably to the bottom. It also makes the "true" algorithm ambiguous — which level owns the flow?

**Mitigations:** keep hierarchies **shallow** (ideally one level of concrete subclasses under one abstract base); **document the step contracts and lifecycle** rigorously; mark the template `final`; and when variation is significant or multi-dimensional, **prefer composition (Strategy / injected steps)** over inheritance so subclasses don't couple to base internals at all.
</details>

<details>

<summary><strong>Q12: [Trade-off] Template Method vs. Strategy — when do you choose each?</strong></summary>

Both vary behavior behind a stable structure, but differently:

- **Template Method** uses **inheritance** and defers **individual steps** of an algorithm whose skeleton is fixed in a base class. Variation is chosen by *which subclass* you instantiate — **compile-time**, one behavior per subclass.
- **Strategy** uses **composition** and swaps the **entire algorithm** as an injected object — **runtime**, and one context can use different strategies over its life.

**Choose Template Method** when there's genuinely **one fixed skeleton** with a small, stable set of variant steps, and you're building a base class / framework hierarchy. **Choose Strategy** when you need **runtime swapping**, **multiple independent variation axes** (to avoid combinatorial subclass explosion), or **better testability** (inject mocks). Modern APIs often prefer Strategy/composition because inheritance's coupling (fragile base class) and rigidity (compile-time binding) don't scale — but Template Method is simpler and clearer for the single-skeleton case. They also combine: a template skeleton whose steps are injected Strategies (Variant 3).
</details>

<details>

<summary><strong>Q13: [Trade-off] How is Factory Method related to Template Method?</strong></summary>

They **compose** rather than compete: a **Factory Method is very often one of the primitive operations that a Template Method defers to subclasses.** The template method runs the algorithm and, at the point where it needs an object, calls a factory method; the subclass's factory method decides *which* concrete object to create. GoF states directly that "Factory Methods are often called by template methods."

Example: a document framework's `newDocument()` template method might do `Document doc = createDocument(); doc.open(); addToRecentList(doc);` where `createDocument()` is the abstract factory method a `TextApp` or `DrawingApp` subclass overrides to return a `TextDocument` or `DrawingDocument`. The *creation step* varies (Factory Method), the *surrounding flow* is fixed (Template Method).

So Factory Method can be seen as a **specialization** of Template Method focused specifically on the "create an object" step. If asked "how do they differ," the crisp answer: Factory Method is about deferring *object creation*; Template Method is about deferring *any step(s)* of an algorithm — and the former is frequently a step of the latter.
</details>

<details>

<summary><strong>Q14: [Trade-off] Why do modern frameworks/APIs often favor composition (Strategy, callbacks) over Template Method inheritance?</strong></summary>

Several reasons rooted in inheritance's costs: (1) **single inheritance in Java** — extending a template base "uses up" your one superclass slot, blocking other reuse; composition has no such limit. (2) **Fragile base class** — subclasses couple to base internals, so evolving the base risks breaking them; injected collaborators couple only to a small interface. (3) **Runtime flexibility** — composition lets you swap/reconfigure behavior at runtime and combine variations freely, avoiding compile-time binding and subclass explosion. (4) **Testability** — injected steps are trivially mocked; overridden steps require subclassing the system-under-test. (5) **Composability** — small strategy objects can be reused across different skeletons.

That's why the JDK uses `Comparator` (Strategy) for sorting rather than a `Sorter` subclass per ordering, why Spring's `JdbcTemplate` takes a **callback** (`RowMapper`) rather than requiring you to subclass it, and why functional interfaces + lambdas replaced many small abstract classes. The nuance for a strong answer: **Template Method is still the right tool for a stable single-skeleton framework lifecycle** (`HttpServlet`, JUnit) — the "prefer composition" guidance is about *significant or multi-axis variation*, not an absolute.
</details>

<details>

<summary><strong>Q15: [Advanced] How do you make a Template Method class testable, and what's hard about it?</strong></summary>

The hard part: because steps are **overridden subclass methods**, you can't easily inject test doubles the way you can with composition — you often have to subclass the class under test to stub steps, and the `final` template method plus tightly-coupled steps can resist isolation.

Approaches: (1) **test through the template** with a concrete test-subclass that implements the abstract steps with observable/stub behavior, asserting the *order* and *effects* (you can record call order in a list to verify the skeleton). (2) Keep steps **`protected`** (not `private`) so a test subclass can override them. (3) Better: **refactor toward composition** — extract the varying steps into injected collaborators (Variant 3) so tests pass mocks directly, no subclassing needed. (4) Use **property-based tests** for invariants (e.g., "cleanup always runs even when `process` throws"). The senior insight: *if a Template Method is painful to test, that's often a signal the variation should be composed in (Strategy/callback) rather than inherited* — testability is one of the strongest arguments for composition over inheritance.
</details>

<details>

<summary><strong>Q16: [Advanced] Can you implement Template Method with interfaces and default methods instead of an abstract class? Trade-offs?</strong></summary>

Yes. Since Java 8, an **interface `default` method can be the template method**, calling abstract interface methods (the primitive operations) that implementers supply.

<details>

<summary>💻 Click to expand</summary>

```java
interface ImportFlow {
    default void run(String src) {            // template method as a default method
        var recs = extract(src);              // abstract (implementer supplies)
        validate(recs);                        // default hook
        load(recs);                            // abstract
    }
    List<Record> extract(String src);
    void load(List<Record> recs);
    default void validate(List<Record> recs) { /* default hook */ }
}
```
</details>

**Trade-offs:** interfaces allow **multiple inheritance of behavior** (a class can implement several such interfaces), sidestepping the single-superclass limit — a real advantage. **But** default methods **cannot be `final`**, so you *lose the ability to lock the skeleton* — an implementer can override `run()` and break the invariant (the very guarantee Template Method exists to provide). Interfaces also **can't hold instance state** (no protected fields for shared step data). So: use an **abstract class** when you must enforce the skeleton (`final`) or share state; use an **interface default method** when you need mix-in flexibility and can tolerate an unenforceable skeleton.
</details>

<details>

<summary><strong>Q17: [Advanced] How do hooks let a subclass influence control flow, and how do you keep that safe?</strong></summary>

A **hook that returns a value** lets the template *consult* the subclass to decide flow, without the subclass touching the skeleton. Classic form: a `boolean` hook the template branches on (`if (shouldCache()) cache();`), or a hook returning a limit/config the template uses. The subclass declares *intent*; the template retains *control* (it decides what the intent means and enforces bounds).

<details>

<summary>💻 Click to expand</summary>

```java
public final void run() {
    process();
    if (shouldNotify()) notifyListeners();   // subclass influences flow via hook
    for (int i = 0; i < Math.min(maxRetries(), 5); i++) { /* template BOUNDS the hook */ }
}
protected boolean shouldNotify() { return false; }  // safe default
protected int maxRetries()       { return 0; }      // safe default; template clamps to <=5
```
</details>

**Keeping it safe:** give every hook a **safe default** (so unaware subclasses get correct behavior); document the hook's **contract** (pure, no side effects, allowed range); and have the **template validate/bound** the hook's return (clamp `maxRetries` to a ceiling, ignore nonsensical values) so a bad hook can't corrupt the algorithm. The template must never *cede* control to a hook — it only *consults* it. This preserves the invariant while still allowing declarative customization.
</details>

<details>

<summary><strong>Q18: [Advanced] Coding challenge — build an extensible generic report generator (PDF + HTML) with a fixed skeleton, hooks, and cleanup.</strong></summary>

<details>

<summary>💻 Click to expand full solution</summary>

```java
import java.util.*;

abstract class ReportGenerator {
    // TEMPLATE METHOD: fixed report pipeline; final so no subclass alters it.
    public final String generate(List<Map<String, Object>> data) {
        StringBuilder out = new StringBuilder();
        boolean opened = false;
        try {
            openDocument(out); opened = true;   // shared with format-specific header
            writeHeader(out);                    // hook (default title)
            if (showColumnHeaders()) writeColumnHeaders(out, data); // hook influences flow
            for (Map<String, Object> row : data)
                writeRow(out, row);              // abstract: format-specific row rendering
            writeFooter(out);                    // hook
            return out.toString();
        } finally {
            if (opened) closeDocument(out);      // guaranteed cleanup/close tag
        }
    }

    // ---- Abstract primitives (each format supplies) ----
    protected abstract void openDocument(StringBuilder out);
    protected abstract void closeDocument(StringBuilder out);
    protected abstract void writeRow(StringBuilder out, Map<String, Object> row);

    // ---- Hooks (optional) ----
    protected boolean showColumnHeaders() { return true; }
    protected void writeHeader(StringBuilder out) { /* default: nothing */ }
    protected void writeFooter(StringBuilder out) { /* default: nothing */ }
    protected void writeColumnHeaders(StringBuilder out, List<Map<String, Object>> data) { /* default */ }
}

class HtmlReport extends ReportGenerator {
    protected void openDocument(StringBuilder o)  { o.append("<table>\n"); }
    protected void closeDocument(StringBuilder o) { o.append("</table>\n"); }
    protected void writeRow(StringBuilder o, Map<String, Object> row) {
        o.append("  <tr>");
        row.values().forEach(v -> o.append("<td>").append(v).append("</td>"));
        o.append("</tr>\n");
    }
    @Override protected void writeColumnHeaders(StringBuilder o, List<Map<String, Object>> data) {
        if (data.isEmpty()) return;
        o.append("  <tr>");
        data.get(0).keySet().forEach(k -> o.append("<th>").append(k).append("</th>"));
        o.append("</tr>\n");
    }
}

class CsvReport extends ReportGenerator {
    protected void openDocument(StringBuilder o)  { /* nothing */ }
    protected void closeDocument(StringBuilder o) { /* nothing */ }
    protected void writeRow(StringBuilder o, Map<String, Object> row) {
        o.append(String.join(",", row.values().stream().map(String::valueOf).toList())).append("\n");
    }
    @Override protected boolean showColumnHeaders() { return false; } // opt out via hook
}

public class ReportDemo {
    public static void main(String[] args) {
        var data = List.of(
            Map.<String, Object>of("id", 1, "name", "Ada"),
            Map.<String, Object>of("id", 2, "name", "Alan"));
        System.out.println(new HtmlReport().generate(data)); // <table> with <th> + rows
        System.out.println(new CsvReport().generate(data));  // plain CSV rows, no headers
    }
}
```
</details>

Highlights: one `final` skeleton drives both formats; **abstract primitives** (`openDocument`, `writeRow`, `closeDocument`) are format-specific and mandatory; **hooks** (`showColumnHeaders`, `writeHeader`, `writeColumnHeaders`, `writeFooter`) let each format opt into optional behavior with sensible defaults (CSV opts out of headers); and cleanup (`closeDocument`) runs in `finally`. Adding a Markdown report is one new subclass overriding three methods — no skeleton change.
</details>

<details>

<summary><strong>Q19: [Advanced] Coding challenge — implement a data-pipeline base class where the number/order of steps is fixed but each subclass validates differently, and prove the skeleton can't be broken.</strong></summary>

<details>

<summary>💻 Click to expand full solution</summary>

```java
import java.util.*;

abstract class Pipeline<I, O> {
    // Records the invariant order so we can PROVE the skeleton is fixed.
    protected final List<String> trace = new ArrayList<>();

    // final template method: subclasses cannot override or reorder these steps.
    public final O run(I input) {
        trace.add("read");     I raw = read(input);
        trace.add("validate"); validate(raw);           // varies per subclass
        trace.add("map");      O mapped = map(raw);
        trace.add("persist");  persist(mapped);
        return mapped;
    }
    protected abstract I read(I input);
    protected abstract void validate(I raw);            // the differing step
    protected abstract O map(I raw);
    protected abstract void persist(O out);
}

class UserPipeline extends Pipeline<String, Integer> {
    protected String read(String in) { return in.trim(); }
    protected void validate(String raw) {               // custom validation
        if (raw.isBlank()) throw new IllegalArgumentException("empty");
    }
    protected Integer map(String raw) { return raw.length(); }
    protected void persist(Integer out) { /* save */ }
}

public class PipelineProof {
    public static void main(String[] args) {
        UserPipeline p = new UserPipeline();
        p.run("  hello ");
        System.out.println(p.trace); // [read, validate, map, persist] — ALWAYS this order

        // Proof the skeleton can't change: run() is final. This will NOT compile:
        //   class Rogue extends Pipeline<String,Integer> { public Integer run(String i){ return 0; } }
        //   error: run(String) in Rogue cannot override run(I) ... overridden method is final
    }
}
```
</details>

The design proof points: (1) `run()` is **`final`**, so any subclass attempting to override it **fails to compile** — the invariant order (`read → validate → map → persist`) is guaranteed by the language, not by convention; (2) only `validate` (and the other primitives) vary per subclass; (3) the `trace` list demonstrates at runtime that every subclass executes the identical skeleton. This is the crispest way to *show* an interviewer that `final` is what elevates "a base class with methods" into the Template Method pattern.
</details>

<details>

<summary><strong>Q20: [Trade-off] When would you deliberately NOT use Template Method, and what would you use instead?</strong></summary>

Avoid it when: (1) **there's no genuinely shared, fixed skeleton** — forcing dissimilar procedures under one template produces a leaky, over-generalized base with many special-case hooks; (2) **you need runtime-swappable or multi-axis variation** — inheritance binds one behavior per subclass at compile time and explodes combinatorially → use **Strategy / composition**; (3) **the variation is a single isolated function** — a lambda / `Comparator` / injected `Function` is far lighter than a class hierarchy; (4) **you'd create deep or unstable hierarchies** — the fragile base class problem makes composition safer; (5) **you need mix-in behavior across unrelated types** — use interface default methods or delegation; (6) **only one implementation exists and none are foreseen** — adding an abstract base is speculative generality (YAGNI).

The replacements, briefly: **Strategy** for runtime/multi-axis variation; **callbacks/functional interfaces** for single-step variation (as `JdbcTemplate` does with `RowMapper`); **Decorator** for layering optional behavior; and plain **composition/delegation** to avoid inheritance coupling entirely. The judgment: reach for Template Method when the *flow* is the stable, valuable invariant and the variant steps are few and stable — otherwise prefer composition.
</details>

### 🧠 Staff / Principal Engineer Level

<details>

<summary><strong>SP1: [Staff] Why do modern API and framework designs increasingly favor composition (Strategy/callbacks) over Template Method inheritance, and how do you decide at scale?</strong></summary>

The shift is driven by inheritance's structural costs, which compound at scale. **First, the fragile base class problem**: every subclass couples to the base's internal step order and semantics, so evolving a widely-extended base class safely becomes nearly impossible — you can't tell which of thousands of subclasses relied on a subtle behavior. Composition couples clients only to a narrow, stable interface. **Second, single inheritance**: a Template Method base "uses up" the one superclass slot, blocking other reuse and forcing awkward hierarchies. **Third, rigidity**: compile-time binding and combinatorial subclass explosion across multiple variation axes; composition lets behaviors combine and swap at runtime. **Fourth, testability**: injected collaborators mock trivially; overridden steps require subclassing the SUT.

That's why the JDK uses `Comparator` (Strategy) not `Sorter` subclasses, and Spring's `*Template` classes take **callbacks** rather than requiring subclassing. **How I decide at scale:** use Template Method when the *skeleton itself is the stable, valuable invariant* and variation is a **small, fixed set of steps within a single dimension** — classic framework lifecycles (`HttpServlet`, JUnit) where the framework must own control flow. Switch to **composition** the moment variation becomes **multi-dimensional, runtime-selected, or likely to grow**, or when the base class is a **published extension point many teams subclass** (there, coupling to internals is a liability). Often the best answer is the hybrid (Variant 3): a `final` template skeleton whose *steps are injected strategies* — you keep centralized control flow *and* composable, testable, runtime-swappable steps.
</details>

<details>

<summary><strong>SP2: [Staff] How do you design an extensible framework with stable hooks so you can evolve the base class without breaking downstream subclasses?</strong></summary>

The core challenge is that a framework's base class is a **published contract**: once teams subclass it, its internal step ordering and hook semantics become de facto API. My design principles: (1) **Make the template method `final`** and expose *only* well-named, minimal extension points — abstract primitives for mandatory steps, defaulted hooks for optional ones — so the surface you must keep stable is small and explicit. (2) **Document each hook's contract precisely** (when it's called, how often, threading, whether it may throw, allowed return values, idempotency) — undocumented behavior *becomes* the contract via Hyrum's Law. (3) **Add extension points as *new hooks with safe defaults*, never by adding abstract methods** — a new abstract method breaks every existing subclass; a new defaulted hook is backward-compatible. (4) **Never remove or reorder existing steps**; deprecate hooks with a migration path rather than deleting them. (5) **Prefer `protected` narrow methods over exposing internal state** so subclasses can't couple to fields you might refactor. (6) Consider **sealing** the hierarchy or providing an **adapter/abstract-adapter** class so common subclasses need override little. (7) Where variation is likely to grow, **offer a callback/Strategy seam instead of a subclass hook**, so extension happens by composition (no coupling to base internals) — this is Spring's `JdbcTemplate`/`RowMapper` philosophy. The meta-principle: treat the base class's hooks and lifecycle guarantees as a versioned API with the same backward-compatibility discipline you'd apply to any public interface.
</details>

<details>

<summary><strong>SP3: [Principal] Explain the fragile base class problem in depth and strategies to evolve a Template Method base class in a large codebase.</strong></summary>

The **fragile base class problem** arises because inheritance exposes the base class's *implementation*, not just its interface, to subclasses. Subclasses depend on **how** the template method sequences steps, **when** hooks fire, and the **semantics** of shared/protected methods. So a change that's behaviorally innocuous from the base's perspective — reordering two steps, calling a hook one extra time, adding a step before an existing one, tightening a shared method — can silently break subclasses that (often implicitly, per Hyrum's Law) relied on the old behavior. At scale, with hundreds of subclasses across teams you don't control, you can't even enumerate the assumptions being violated.

**Strategies to evolve safely:** (1) **Freeze the observable contract** — treat step order and hook timing as a versioned API; changes go through deprecation cycles. (2) **Additive-only evolution** — introduce new behavior via *new defaulted hooks*, never by adding abstract methods or reordering existing calls. (3) **Characterization tests** — write tests that pin the *observed* lifecycle (call order, hook counts) so refactors that change it fail loudly; property-based tests for invariants (cleanup always runs, etc.). (4) **Strangler-style migration to composition** — introduce a parallel callback/Strategy-based API, migrate subclasses incrementally, then deprecate the inheritance seam. (5) **Reduce hierarchy depth** — flatten `A→B→C→D` chains; each level is a fragility multiplier. (6) **Restrict extension** — `final` on the template and on steps not meant to be overridden; `sealed` hierarchies (Java 17+) to bound who can subclass so you *can* enumerate and update them. (7) **Contract enforcement** — validate hook outputs in the template so a misbehaving override is contained. The principal-level framing: inheritance-based extension is a **tight, implementation-level coupling**; the long-term move in a large codebase is to convert that coupling into **interface-level coupling via composition**, using deprecation and characterization tests to migrate without a big-bang break.
</details>

<details>

<summary><strong>SP4: [Principal] How do you combine Template Method with dependency injection, and what are the pitfalls?</strong></summary>

The productive combination is a **`final` template skeleton whose *steps are injected collaborators*** (interfaces/Strategies) rather than subclass overrides — you keep Template Method's centralized, guaranteed control flow while getting DI's testability, runtime configurability, and loose coupling. In a DI container (Spring/Guice), the "template" is a singleton service that orchestrates the flow and delegates each varying step to injected beans; you no longer subclass to vary behavior — you wire different collaborators.

**Pitfalls, especially with frameworks that subclass your beans:** (1) **The constructor/overridable-method trap meets proxying** — DI frameworks often create **CGLIB/dynamic proxies** by subclassing your bean; if your template calls an overridable method, calls may route through the proxy (or *not*, for `self`-invocation), causing surprising behavior with `@Transactional`/`@Cacheable` self-calls that silently bypass the proxy. Prefer injecting collaborators over `this.step()` self-invocation. (2) **`final` vs. proxying** — CGLIB can't proxy `final` classes/methods, so aggressively `final`-ing a bean can break AOP; balance skeleton-locking against proxy needs. (3) **Initialization order** — don't invoke injected steps in constructors (fields/dependencies may not be set); use `@PostConstruct` or an explicit `start()`. (4) **Testability regained** — with injected steps you mock collaborators directly; with subclass-override Template Method you'd have to subclass the SUT, which is why composition + DI is preferred. (5) **Scope/lifecycle mismatches** — a singleton template invoking a request-scoped step needs a proper scoped-proxy, or you leak state across requests. The principal takeaway: **use Template Method for the *flow*, DI for the *steps*** — this "template of injected strategies" is how mature systems get both a guaranteed lifecycle and composable, testable, independently-deployable behavior, but you must respect the container's proxying and initialization semantics.
</details>

<details>

<summary><strong>SP5: [Principal] A base class calls an overridable method during construction and it causes intermittent production NPEs. Diagnose and design a systematic fix.</strong></summary>

The symptom — *intermittent* NPEs in a subclass step invoked during base-class construction — is the classic Java **initialization-order trap**, and the intermittency usually comes from timing/data-dependence (the override only NPEs when a not-yet-initialized field is dereferenced on certain inputs) or from the affected code path being one of several subclasses. Root cause: Java runs the **superclass constructor before the subclass's field initializers and constructor body**, so if the base constructor calls an overridable method, the subclass's override executes while the subclass's fields are still `null`/default.

**Diagnosis:** (1) Look for the base constructor invoking a non-`final`, non-`private` method — that's the smoking gun; a stack trace showing the override called *from within `<init>` of the base* confirms it. (2) Reproduce by constructing each subclass and asserting field state at the moment the step runs (log `this.field` inside the override). (3) Check whether the method is also proxied by a DI/AOP framework, which can add a second layer of surprise.

<details>

<summary>💻 Click to expand — diagnosis and fix</summary>

```java
// SMOKING GUN: base ctor calls overridable step.
abstract class Base { Base() { configure(); } protected abstract void configure(); }

// SYSTEMATIC FIX: two-phase construction — construct, THEN initialize via a final template.
abstract class BaseFixed {
    protected BaseFixed() { /* no overridable calls here */ }
    public final void initialize() { configure(); }   // caller/factory invokes AFTER construction
    protected abstract void configure();
    // Factory guarantees the two phases happen in order and can't be skipped:
    static <T extends BaseFixed> T create(java.util.function.Supplier<T> ctor) {
        T t = ctor.get(); t.initialize(); return t;
    }
}
```
</details>

**Systematic prevention (design-level, not just this bug):** (1) **Never call overridable methods from constructors** — enforce with a static-analysis rule (SpotBugs/Error Prone flag exactly this) so it can't reoccur. (2) Adopt **two-phase construction**: constructors only set final state; a `final initialize()`/`start()` template runs the overridable steps, invoked by a **factory method** so callers can't forget it. (3) Make internally-called steps **`final`/`private`/`static`** where they don't need overriding, removing dynamic dispatch entirely. (4) In DI contexts, move such logic to **`@PostConstruct`**, not the constructor. (5) Add **characterization/property tests** asserting subclass invariants hold after construction for all subclasses. The principal framing: this is a *class of bug* born from mixing Template Method (base calls down into steps) with constructor semantics — the durable fix is an architectural convention (two-phase init + factory + lint rule), not a one-off patch, so the entire team is protected from re-introducing it.
</details>

---

## ⚡ Quick Revision

**One-liner:** Template Method defines the fixed skeleton of an algorithm in a `final` base-class method, deferring the varying steps to subclasses (via abstract primitives and overridable hooks) — the base owns the flow and calls down into your steps (Hollywood Principle / inversion of control).

**The whole pattern in one paragraph:** Template Method is a behavioral GoF pattern — *"define the skeleton of an algorithm in an operation, deferring some steps to subclasses; subclasses redefine certain steps without changing the algorithm's structure."* Its essence is **inversion of control**: instead of your code calling the library step by step, the base class (framework) owns the high-level flow and calls *down* into your subclass's steps at the right moments — the **Hollywood Principle, "Don't call us, we'll call you."** Structure: an **abstract base class** holds a **`final` template method** that calls steps in a fixed order; steps come in three kinds — **concrete operations** (shared, implemented in the base), **abstract primitive operations** (mandatory, compiler-forces subclasses to implement), and **hook methods** (optional, defaulted, may be overridden to add behavior or *influence* the flow via a returned value). Making the template method **`final`** is what enforces the invariant and turns "a base class with methods" into the pattern — subclasses fill slots but can't reorder or skip steps. It works via **dynamic dispatch inside a fixed sequence**: the base decides *when* each step runs, the subclass decides *what* it does. Production versions add a **lifecycle** (`setUp → process → tearDown`) with **`try/finally`** so cleanup always runs (guarded by a "was it set up?" flag) and **guarded hooks** that don't let a bad override corrupt invariants. Its closest cousin is **Strategy** (composition, runtime-swappable *whole algorithm* vs. inheritance, compile-time *step* variation); **Factory Method** is often *a step* of a Template Method (deferring object creation). The JDK/frameworks are full of it: **`HttpServlet.service()`** dispatching to `doGet`/`doPost`, **`AbstractList`/`AbstractMap`** skeletal implementations built on `get`/`size`, **`InputStream.read(byte[])`** built on the abstract `read()`, **JUnit** `@BeforeEach`/`@AfterEach`, and **Spring `JdbcTemplate`** (which actually uses a *callback* — Template Method leaning toward Strategy). Watch for the traps: **not making the template `final`** (subclass wrecks the skeleton), **too many abstract steps** (fragile subclasses), the **Java constructor-calling-overridable-method init trap** (override runs before subclass fields are set — NPE), **deep hierarchies / fragile base class**, and **LSP violations from hooks**. At scale, modern designs often **prefer composition (Strategy, injected steps, callbacks + DI)** over inheritance because of the fragile-base-class problem, single-inheritance limits, and testability — but Template Method remains the right tool when there's a single stable skeleton the framework must own.

**Top 5 interview answers to memorize:**

1. **"What is it?"** → Put the fixed algorithm skeleton in a `final` base-class method that calls steps in order; subclasses implement the abstract (mandatory) steps and override hooks (optional) — the base owns the flow, subclasses own the steps.
2. **"Hollywood Principle?"** → "Don't call us, we'll call you" — inversion of control; the framework/base class calls your steps rather than you calling it. Template Method is the code embodiment.
3. **"Template Method vs. Strategy?"** → Template Method = inheritance, defers *individual steps* of a fixed skeleton, compile-time (one behavior per subclass); Strategy = composition, swaps the *whole algorithm* as an injected object, runtime. Prefer Strategy for runtime/multi-axis variation.
4. **"Why `final` on the template method, and abstract vs. hook?"** → `final` enforces the invariant skeleton (subclasses can't reorder/skip). Abstract primitive = mandatory (compiler forces it); hook = optional with a default the subclass may override.
5. **"Biggest Java trap?"** → Calling an overridable step from a constructor: the superclass ctor runs first, so the subclass override executes before its fields are initialized → NPE. Fix: two-phase init (`init()`/factory) or make the method `final`/`private`.

**Trigger words (hear these → think Template Method):** "skeleton of an algorithm", "fixed steps in a fixed order", "override just this step", "framework lifecycle / hooks", "setUp/tearDown", "Don't call us we'll call you", "Hollywood Principle", "inversion of control", "abstract base class with a final method", "same procedure, different details", "doGet/doPost", "AbstractList / skeletal implementation", "callback template (JdbcTemplate)", "boilerplate pipeline", "hook method", "defer to subclasses".

---

*End of Template Method Pattern study guide.*

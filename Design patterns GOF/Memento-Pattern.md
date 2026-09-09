# Memento Pattern ⭐⭐⭐⭐ (Behavioral · Medium-Hard)

> A behavioral design pattern that lets you save and restore an object's previous state without exposing its internal structure. Difficulty is bumped to 4 stars because the *encapsulation-preserving* mechanism (narrow vs. wide interface) and the memory/deep-copy trade-offs are what separate a junior answer from a staff-level one.

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 1: Naive — Public Getters/Setters](#variant-1-naive--public-getterssetters-breaks-encapsulation)
   - [Variant 2: Improved — Narrow/Wide Interface with Inner Class](#variant-2-improved--narrowwide-interface-with-inner-static-class)
   - [Variant 3: Production — Undo/Redo with History Stack](#variant-3-production--undoredo-with-history--redo-stacks)
5. [🎨 Real-World Example](#-real-world-example)
   - [Scenario 1: Database Transaction Rollback](#scenario-1-database-transaction-rollback-savepoints)
   - [Scenario 2: Kubernetes Rollout Rollback](#scenario-2-kubernetes-rollout-rollback-deployment-revisions)
   - [Scenario 3: Configuration Versioning](#scenario-3-configuration-versioning-snapshot--restore-app-config)
   - [Scenario 4: Text Editor Undo/Redo](#scenario-4-text-editor-undoredo)
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
16. [⚡ Quick Revision](#-quick-revision)

---

## 📋 Intent

**GoF:** "Without violating encapsulation, capture and externalize an object's internal state so that the object can be restored to this state later."

In plain terms: the Memento pattern captures and externalizes an object's internal state so the object can be restored to this state later, **without violating encapsulation**. The object whose state we snapshot (the **Originator**) hands out an opaque token (the **Memento**). Some other object (the **Caretaker**) holds onto these tokens and hands them back to restore state, but can never peek inside them.

The critical, often-missed word is *encapsulation*. Anyone can save state by exposing public getters — the pattern earns its name only because it does so while keeping the originator's internal representation private from everyone except the originator itself.

---

## 🎯 Problem

You need to record snapshots of an object so you can roll it back later — implementing undo, checkpoints, or transactional rollback. The naive approach is to expose all the fields with public getters and setters so an outside object can copy them out and later push them back in.

That approach fails for two reasons:

1. **It breaks encapsulation.** To snapshot state you must expose every field publicly. Now any code can mutate the object's internals, invariants can be violated, and you can never refactor the private representation without breaking every consumer.
2. **The caretaker becomes coupled to the originator's internals.** The class managing history now has to understand the meaning and structure of every field, so a change to the originator ripples through the history-management code.

Concrete real-world scenarios that lead to Memento:

- **Undo/redo in editors** — a text editor, IDE, or graphics program (Photoshop, Figma) must let the user step backward and forward through document states.
- **Transactional rollback** — a database or in-memory transaction takes a savepoint before applying changes and rolls back to it on failure.
- **Game save states / checkpoints** — a game serializes the world at a checkpoint so the player can respawn.
- **Multi-step form / wizard state** — a booking or onboarding wizard lets the user go "back" and restore prior form input, then "forward" again.
- **Configuration management** — apply a config change, and if health checks fail, restore the last-known-good configuration.

---

## ✅ Solution

**Core idea (plain language):** Let the object that owns the state be the *only* thing that can read and write that state. When you want a snapshot, ask the owner to produce a sealed box containing a copy of its state. The owner knows how to fill the box and how to unpack it; nobody else can open it. A separate bookkeeper holds the boxes and decides *when* to restore, but never looks *inside*.

**Key structural elements:**

- **Originator** — the object whose state we want to save/restore. It creates a Memento containing a snapshot of its current internal state, and can restore its state from a Memento it is given.
- **Memento** — the opaque value object that stores the Originator's internal state. It exposes two conceptual interfaces:
  - a **wide interface** (full access to the stored state) visible only to the Originator, and
  - a **narrow interface** (essentially none — maybe metadata like a timestamp or a name) visible to the Caretaker.
- **Caretaker** — keeps track of mementos (typically in a stack or list for undo/redo). It requests mementos from the Originator, stores them, and hands them back to trigger a restore. It never inspects or modifies memento contents.

**How encapsulation is preserved — narrow vs. wide interface:** In Java the classic trick is to make `Memento` a **private static nested class** of the Originator, exposed to the outside world only through a marker interface with no state-accessing methods. The Caretaker sees only the marker type; the Originator, being the enclosing class, can access the nested class's private fields. This gives the Originator the wide interface and everyone else the narrow one — enforced by the compiler, not merely by convention.

---

## 💻 Implementation

We build three variants, weakest to strongest.

### Variant 1: Naive — Public Getters/Setters (breaks encapsulation)

**What's wrong with it:** The "memento" is just the originator's fields exposed publicly. To snapshot, the caretaker reads every getter; to restore, it calls every setter. This technically works but is *not* the Memento pattern — it defeats the entire purpose, which is to snapshot **without violating encapsulation**. The internal representation leaks, invariants can be broken by anyone, and refactoring `Editor`'s fields breaks all callers.

<details>
<summary>💻 Click to expand code</summary>

```java
// ANTI-PATTERN: state is fully exposed. This is NOT the Memento pattern.
public class Editor {
    private String content;
    private int cursor;
    private String fontName;

    // Every field must be public-readable and public-writable to snapshot/restore.
    public String getContent()      { return content; }
    public void   setContent(String c) { this.content = c; }
    public int    getCursor()       { return cursor; }
    public void   setCursor(int c)   { this.cursor = c; }
    public String getFontName()     { return fontName; }
    public void   setFontName(String f) { this.fontName = f; }
}

// The caretaker must understand the ENTIRE internal structure of Editor.
public class NaiveHistory {
    private String savedContent;
    private int    savedCursor;
    private String savedFont;

    public void save(Editor e) {
        savedContent = e.getContent();   // caretaker reads internals
        savedCursor  = e.getCursor();
        savedFont    = e.getFontName();
    }

    public void restore(Editor e) {
        e.setContent(savedContent);      // caretaker writes internals
        e.setCursor(savedCursor);
        e.setFontName(savedFont);
    }
}
```

</details>

**Pros:**
- Trivial to write; no extra classes.
- Easy to understand for a demo.

**Cons:**
- **Breaks encapsulation** — the whole point of the pattern is lost.
- Caretaker is tightly coupled to every field of the originator; adding a field means editing the caretaker.
- Any external code can now mutate the editor's internals via the public setters, so invariants aren't protected.
- No support for a *history* of states (only the last one), so no real undo/redo.

**Mechanism (why it "works" but shouldn't be used):** It works only because the state is fully public — which is exactly the coupling and encapsulation loss the pattern exists to prevent.

---

### Variant 2: Improved — Narrow/Wide Interface with Inner Static Class

**What it solves:** Restores encapsulation. The snapshot lives in a `Memento` object whose internal accessors are visible *only* to the Originator. The Caretaker holds a `Memento` typed as an empty marker interface (`EditorState`) and cannot read or write its fields. Adding fields to the Editor no longer touches the Caretaker.

<details>
<summary>💻 Click to expand code</summary>

```java
// Narrow interface: what the OUTSIDE world (Caretaker) is allowed to see.
// Deliberately empty (or metadata-only) so callers can hold it but not read state.
public interface EditorState {
    // Optionally expose harmless metadata for the caretaker's UI:
    String getName();
}

public class Editor {
    private String content;
    private int cursor;
    private String fontName;

    public void type(String text) { this.content = (content == null ? "" : content) + text; }
    public void setCursor(int c)  { this.cursor = c; }
    public void setFont(String f) { this.fontName = f; }
    public String getContent()    { return content; }

    // --- Wide interface: Originator creates the snapshot ---
    public EditorState save(String label) {
        return new Memento(content, cursor, fontName, label);
    }

    // --- Wide interface: Originator restores from a snapshot ---
    public void restore(EditorState state) {
        // Safe downcast: only Editor's own Memento implements EditorState here.
        Memento m = (Memento) state;
        this.content  = m.content;   // ENCLOSING class can read the nested class's private fields
        this.cursor   = m.cursor;
        this.fontName = m.fontName;
    }

    // PRIVATE static nested class => the Caretaker cannot reference Memento's fields.
    // Editor (the enclosing class) has full ("wide") access to them.
    private static final class Memento implements EditorState {
        private final String content;   // immutable snapshot
        private final int cursor;
        private final String fontName;
        private final String label;

        private Memento(String content, int cursor, String fontName, String label) {
            this.content = content;
            this.cursor = cursor;
            this.fontName = fontName;
            this.label = label;
        }

        @Override public String getName() { return label; } // narrow interface only
    }
}

// Caretaker sees only EditorState — it cannot open the box.
public class Caretaker {
    private EditorState snapshot;
    public void backup(Editor e)  { snapshot = e.save("checkpoint"); }
    public void undo(Editor e)    { if (snapshot != null) e.restore(snapshot); }
}
```

</details>

**Pros:**
- **Encapsulation is compiler-enforced:** `Memento` is `private static`, its fields invisible outside `Editor`; the Caretaker only ever sees the empty `EditorState` narrow interface.
- Caretaker is decoupled from the Editor's internal representation.
- Memento is **immutable** (`final` fields), so a stored snapshot cannot be corrupted after capture.

**Cons:**
- Still only stores one snapshot — no history/redo yet.
- The `(Memento) state` downcast assumes the same enclosing class produced it (fine in practice; can throw `ClassCastException` if abused).
- Snapshot is a shallow copy of references — fine for `String`/`int` (immutable/primitive), but dangerous if fields are mutable objects (see pitfalls).

**Mechanism (what makes it work):** Java's rule that an **enclosing class can access the private members of its nested classes** gives the Originator the wide interface, while the `private` modifier on the nested class + the empty public interface gives everyone else the narrow interface. That's the entire pattern, enforced by the language.

---

### Variant 3: Production — Undo/Redo with History + Redo Stacks

**What it solves:** Real applications need a *history* of states and both undo and redo. We give the Caretaker two stacks. This variant also addresses **deep copy** (so mutable fields are safe) and discusses **memory cost**.

<details>
<summary>💻 Click to expand code</summary>

```java
import java.util.*;

/** Narrow interface — caretaker only sees metadata. */
public interface DocState { String label(); long timestamp(); }

public class Document {
    private final StringBuilder body;         // MUTABLE field -> must deep-copy on snapshot
    private final List<String> tags;          // MUTABLE collection
    private int caretPos;

    public Document() { this.body = new StringBuilder(); this.tags = new ArrayList<>(); }

    public void write(String s) { body.append(s); caretPos = body.length(); }
    public void addTag(String t) { tags.add(t); }
    public String render() { return body + " " + tags; }

    // Originator produces a DEEP snapshot so later mutations don't leak into the memento.
    public DocState save(String label) {
        return new Memento(body.toString(),          // String is immutable => safe copy
                           new ArrayList<>(tags),     // defensive copy of the list
                           caretPos, label);
    }

    public void restore(DocState state) {
        Memento m = (Memento) state;
        body.setLength(0);
        body.append(m.body);
        tags.clear();
        tags.addAll(m.tags);          // copy back out of the memento
        caretPos = m.caretPos;
    }

    private static final class Memento implements DocState {
        private final String body;
        private final List<String> tags;
        private final int caretPos;
        private final String label;
        private final long ts = System.currentTimeMillis();

        private Memento(String body, List<String> tags, int caretPos, String label) {
            this.body = body;
            this.tags = List.copyOf(tags);   // immutable copy inside the memento
            this.caretPos = caretPos;
            this.label = label;
        }
        @Override public String label()   { return label; }
        @Override public long timestamp() { return ts; }
    }
}

/** Caretaker: history for undo, a redo stack, and a bounded size to cap memory. */
public class History {
    private final Deque<DocState> undo = new ArrayDeque<>();
    private final Deque<DocState> redo = new ArrayDeque<>();
    private final int maxDepth;

    public History(int maxDepth) { this.maxDepth = maxDepth; }

    /** Call BEFORE mutating the document. */
    public void backup(Document doc, String label) {
        undo.push(doc.save(label));
        if (undo.size() > maxDepth) undo.removeLast();  // bound memory: drop oldest
        redo.clear();                                   // new action invalidates redo history
    }

    public void undo(Document doc) {
        if (undo.isEmpty()) return;
        redo.push(doc.save("pre-undo")); // save current so we can redo
        doc.restore(undo.pop());
    }

    public void redo(Document doc) {
        if (redo.isEmpty()) return;
        undo.push(doc.save("pre-redo"));
        doc.restore(redo.pop());
    }
}
```

</details>

**Pros:**
- Full **undo + redo** via two stacks.
- **Deep copy** of mutable fields (`ArrayList`, `StringBuilder`) means snapshots are truly independent of later edits.
- **Bounded history** (`maxDepth`) caps memory — a real concern when each memento is large.
- Snapshots are immutable (`List.copyOf`), so history entries can't be mutated after the fact.

**Cons:**
- **Memory cost:** each memento stores a full copy of the (deep-copied) state. For large documents and long histories this is expensive — motivating *incremental* mementos or command-based diffs.
- Deep copy has a CPU cost on every snapshot.
- Not thread-safe as written; concurrent edits + snapshots need external synchronization or a lock (see advanced Q&A).

**Mechanism (what makes it work):** The two-stack invariant. Every state-changing operation pushes the *pre-change* state onto `undo` and clears `redo`. Undo moves a state from `undo` to `redo` (after saving current), redo does the reverse. Deep-copying at `save()` time guarantees each stack entry is a stable, independent snapshot rather than an alias of live mutable state.

---

## 🎨 Real-World Example

Below are four Memento scenarios that show up constantly in real backend, infrastructure, and application systems. Each maps the same three roles — **Originator** (owns the state), **Memento** (opaque snapshot), **Caretaker** (holds history / decides when to restore).

#### Scenario 1: Database Transaction Rollback (savepoints)

<details>
<summary>💻 Click to expand code</summary>

```java
import java.util.*;

/**
 * ORIGINATOR — an in-memory table whose committed state can be snapshotted
 * so a transaction can roll back to a SAVEPOINT (like SQL's SAVEPOINT/ROLLBACK TO).
 */
class InMemoryTable {
    private final Map<Integer, String> rows = new HashMap<>();

    public void insert(int id, String value) { rows.put(id, value); }
    public void update(int id, String value) { rows.put(id, value); }
    public void delete(int id)                { rows.remove(id); }
    public Map<Integer, String> dump()        { return new TreeMap<>(rows); }

    /** Wide interface: capture a DEEP COPY of the row map into an opaque memento. */
    public Savepoint mark(String name) {
        return new Savepoint(name, new HashMap<>(rows)); // defensive deep copy of the map
    }

    /** Wide interface: restore rows from a savepoint (ROLLBACK TO savepoint). */
    public void rollbackTo(Savepoint sp) {
        rows.clear();
        rows.putAll(sp.snapshot);
    }

    /** MEMENTO — opaque to the transaction manager; only the table reads its contents. */
    static final class Savepoint {
        private final String name;
        private final Map<Integer, String> snapshot;
        private Savepoint(String name, Map<Integer, String> snapshot) {
            this.name = name; this.snapshot = snapshot;
        }
        public String name() { return name; }               // narrow interface: metadata only
    }
}

/** CARETAKER — a transaction that stacks savepoints and can roll back to any of them. */
class Transaction {
    private final InMemoryTable table;
    private final Deque<InMemoryTable.Savepoint> savepoints = new ArrayDeque<>();
    Transaction(InMemoryTable table) { this.table = table; }

    public void savepoint(String name) { savepoints.push(table.mark(name)); }

    public void rollbackTo(String name) {
        while (!savepoints.isEmpty()) {
            InMemoryTable.Savepoint sp = savepoints.pop();
            if (sp.name().equals(name)) { table.rollbackTo(sp); return; }
        }
        throw new NoSuchElementException("No savepoint named " + name);
    }
}

public class TxnDemo {
    public static void main(String[] args) {
        InMemoryTable accounts = new InMemoryTable();
        accounts.insert(1, "alice:100");
        Transaction txn = new Transaction(accounts);

        txn.savepoint("sp1");                 // snapshot: {1=alice:100}
        accounts.update(1, "alice:80");       // debit alice
        accounts.insert(2, "bob:20");         // credit bob

        txn.savepoint("sp2");                 // snapshot: {1=alice:80, 2=bob:20}
        accounts.delete(2);                   // oops — bad write
        System.out.println(accounts.dump());  // {1=alice:80}

        txn.rollbackTo("sp2");                // restore to sp2
        System.out.println(accounts.dump());  // {1=alice:80, 2=bob:20}

        txn.rollbackTo("sp1");                // restore all the way back
        System.out.println(accounts.dump());  // {1=alice:100}
    }
}
```

</details>

**Explanation:** This mirrors SQL `SAVEPOINT` / `ROLLBACK TO SAVEPOINT`. The `InMemoryTable` is the **Originator** — it alone knows how to copy and restore its row map. `mark()` builds a `Savepoint` **Memento** that holds a *deep copy* of the rows (`new HashMap<>(rows)`), which is the critical detail: a shallow reference would let subsequent writes mutate the "saved" state and defeat rollback. The `Transaction` is the **Caretaker** — it stacks savepoints by name and, on `rollbackTo`, pops until it finds the target and asks the table to restore. Notice the manager never reads individual rows; the `Savepoint` only exposes `name()` (the narrow interface), so encapsulation of the table's representation is preserved. A real database captures this far more efficiently via a write-ahead log / undo segments rather than copying whole pages, but the conceptual role split is identical. Watch the memory cost: full-map copies are O(n) per savepoint, so production engines snapshot *deltas*, not the whole dataset.

#### Scenario 2: Kubernetes Rollout Rollback (Deployment revisions)

<details>
<summary>💻 Click to expand code</summary>

```java
import java.util.*;

/** MEMENTO — an immutable revision of a Deployment's spec (like a ReplicaSet revision in K8s). */
final class DeploymentRevision {
    final int revision;          // narrow interface: metadata K8s exposes via `kubectl rollout history`
    final String image;          // captured spec state
    final int replicas;
    final Instant createdAt;
    DeploymentRevision(int revision, String image, int replicas, Instant createdAt) {
        this.revision = revision; this.image = image; this.replicas = replicas; this.createdAt = createdAt;
    }
    @Override public String toString() {
        return "rev#" + revision + " {image=" + image + ", replicas=" + replicas + "}";
    }
}

/** ORIGINATOR — a Deployment whose live spec can be snapshotted and restored. */
class Deployment {
    private String image;
    private int replicas;

    public void setImage(String image) { this.image = image; }
    public void scale(int replicas)    { this.replicas = replicas; }
    public String status()             { return "LIVE {image=" + image + ", replicas=" + replicas + "}"; }

    /** Wide interface: snapshot current spec as an opaque revision. */
    public DeploymentRevision snapshot(int revision) {
        return new DeploymentRevision(revision, image, replicas, Instant.now());
    }
    /** Wide interface: restore spec from a prior revision. */
    public void restore(DeploymentRevision r) {
        this.image = r.image;
        this.replicas = r.replicas;
    }
}

/** CARETAKER — records every rollout as a revision and can roll back like `kubectl rollout undo`. */
class RolloutController {
    private final Deployment deployment;
    private final List<DeploymentRevision> history = new ArrayList<>();
    private int nextRevision = 1;
    RolloutController(Deployment d) { this.deployment = d; }

    /** Apply a new spec, then record the resulting revision (rollout). */
    public void apply(String image, int replicas) {
        deployment.setImage(image);
        deployment.scale(replicas);
        history.add(deployment.snapshot(nextRevision++));
    }
    /** `kubectl rollout undo` — go back to the previous good revision. */
    public void undo() {
        if (history.size() < 2) throw new IllegalStateException("No prior revision");
        DeploymentRevision previous = history.get(history.size() - 2);
        deployment.restore(previous);
        history.add(deployment.snapshot(nextRevision++)); // K8s records the rollback as a NEW revision
    }
    /** `kubectl rollout undo --to-revision=N` */
    public void undoTo(int revision) {
        DeploymentRevision target = history.stream()
            .filter(r -> r.revision == revision).findFirst()
            .orElseThrow(() -> new NoSuchElementException("No revision " + revision));
        deployment.restore(target);
        history.add(deployment.snapshot(nextRevision++));
    }
    public List<DeploymentRevision> history() { return List.copyOf(history); }
}

public class RolloutDemo {
    public static void main(String[] args) {
        Deployment web = new Deployment();
        RolloutController rollout = new RolloutController(web);

        rollout.apply("web:v1", 3);           // rev#1
        rollout.apply("web:v2", 5);           // rev#2
        rollout.apply("web:v3-broken", 5);    // rev#3 — bad image, crashloop
        System.out.println(web.status());     // LIVE {image=web:v3-broken, replicas=5}

        rollout.undo();                        // roll back to rev#2
        System.out.println(web.status());     // LIVE {image=web:v2, replicas=5}

        rollout.undoTo(1);                     // pin back to rev#1
        System.out.println(web.status());     // LIVE {image=web:v1, replicas=3}

        rollout.history().forEach(System.out::println);
    }
}
```

</details>

**Explanation:** This models `kubectl rollout undo`. The `Deployment` is the **Originator** — its live spec (image + replica count) is the mutable state. Each successful `apply` produces a `DeploymentRevision` **Memento**, exactly like Kubernetes keeping a `ReplicaSet` per revision so you can `kubectl rollout history` and roll back. The `RolloutController` is the **Caretaker**: it owns the ordered list of revisions and decides *which* one to restore, but it never mutates the deployment's fields directly — it calls `snapshot()`/`restore()`. Two production-faithful details are worth calling out: revisions are **immutable** (a rolled-back spec can't corrupt a stored revision), and a rollback is itself **recorded as a new revision** (`history.add(...)` inside `undo`), which is exactly how Kubernetes behaves — undoing to rev#2 creates rev#4 with rev#2's contents, preserving a linear, auditable history. The bounded-history concern applies here too: K8s caps stored revisions via `revisionHistoryLimit`, the real-world analog of capping the caretaker's list.

#### Scenario 3: Configuration Versioning (snapshot / restore app config)

<details>
<summary>💻 Click to expand code</summary>

```java
import java.util.*;

/**
 * ORIGINATOR — a mutable application configuration whose state can be
 * snapshotted into named versions and restored (feature flags, tuning knobs, etc.).
 */
class AppConfig {
    private final Map<String, String> settings = new HashMap<>();

    public void set(String key, String value) { settings.put(key, value); }
    public String get(String key)              { return settings.get(key); }
    public Map<String, String> view()          { return new TreeMap<>(settings); }

    /** Wide interface: capture a deep, immutable copy as an opaque version. */
    public ConfigVersion snapshot(String label) {
        return new ConfigVersion(label, Map.copyOf(settings)); // immutable deep copy
    }
    /** Wide interface: replace current settings from a version. */
    public void restore(ConfigVersion v) {
        settings.clear();
        settings.putAll(v.data);
    }

    /** MEMENTO — opaque version; the store only sees label + timestamp. */
    static final class ConfigVersion {
        private final String label;
        private final Instant takenAt;
        private final Map<String, String> data;   // hidden from the caretaker
        private ConfigVersion(String label, Map<String, String> data) {
            this.label = label; this.takenAt = Instant.now(); this.data = data;
        }
        public String label()   { return label; }    // narrow interface
        public Instant takenAt() { return takenAt; }  // narrow interface
    }
}

/** CARETAKER — a version store that keeps the last N versions and can restore any by label. */
class ConfigVersionStore {
    private final AppConfig config;
    private final Deque<AppConfig.ConfigVersion> versions = new ArrayDeque<>();
    private final int maxVersions;
    ConfigVersionStore(AppConfig config, int maxVersions) {
        this.config = config; this.maxVersions = maxVersions;
    }

    public void commit(String label) {
        versions.push(config.snapshot(label));
        while (versions.size() > maxVersions) versions.removeLast(); // bound memory
    }
    public void restore(String label) {
        config.restore(versions.stream()
            .filter(v -> v.label().equals(label)).findFirst()
            .orElseThrow(() -> new NoSuchElementException("No version " + label)));
    }
    public List<String> labels() {
        List<String> out = new ArrayList<>();
        versions.forEach(v -> out.add(v.label() + " @ " + v.takenAt()));
        return out;
    }
}

public class ConfigDemo {
    public static void main(String[] args) {
        AppConfig config = new AppConfig();
        ConfigVersionStore store = new ConfigVersionStore(config, 5);

        config.set("cache.size", "256");
        config.set("feature.newCheckout", "false");
        store.commit("v1-baseline");                  // snapshot v1

        config.set("feature.newCheckout", "true");    // enable feature
        config.set("cache.size", "1024");
        store.commit("v2-rollout");                   // snapshot v2
        System.out.println(config.view());            // {cache.size=1024, feature.newCheckout=true}

        config.set("cache.size", "16");               // bad tuning -> latency spike
        System.out.println(config.get("cache.size")); // 16

        store.restore("v2-rollout");                  // revert to known-good config
        System.out.println(config.view());            // {cache.size=1024, feature.newCheckout=true}

        store.restore("v1-baseline");                 // full rollback (disable feature)
        System.out.println(config.view());            // {cache.size=256, feature.newCheckout=false}
    }
}
```

</details>

**Explanation:** This is the pattern behind config management systems (Spring Cloud Config, Consul KV, AWS AppConfig) where you can pin and roll back configuration versions. `AppConfig` is the **Originator** holding live settings. `snapshot()` produces a `ConfigVersion` **Memento** built with `Map.copyOf(settings)` — an *immutable* deep copy, so a stored version can never be mutated by later `set()` calls (the single most common Memento bug is snapshotting a live reference instead of a copy). `ConfigVersionStore` is the **Caretaker**: it commits versions, restores by label, and — importantly — **bounds memory** by evicting the oldest version once it exceeds `maxVersions`, the classic answer to "won't storing every snapshot blow up memory?". The store only ever reads `label()` and `takenAt()` (the narrow interface); the actual settings map (`data`) stays private to `AppConfig`, so the version store can manage history without any knowledge of the config's internal representation. For large configs you'd store *diffs* between versions rather than full copies — the same memory-vs-simplicity trade-off seen in the transaction and rollout scenarios.

#### Scenario 4: Text Editor Undo/Redo

<details>
<summary>💻 Click to expand code</summary>

```java
import java.util.*;

/** Narrow interface exposed to the caretaker. */
interface EditorSnapshot {
    String describe();   // metadata only; no state accessors
}

/** ORIGINATOR */
class TextEditor {
    private StringBuilder text = new StringBuilder();
    private int selectionStart = 0;
    private int selectionEnd = 0;

    public void insert(String s) {
        text.append(s);
        selectionStart = selectionEnd = text.length();
    }
    public void deleteLast(int n) {
        int start = Math.max(0, text.length() - n);
        text.delete(start, text.length());
        selectionStart = selectionEnd = text.length();
    }
    public String content() { return text.toString(); }

    /** Wide interface: build an opaque snapshot. String copy is a safe deep copy. */
    public EditorSnapshot snapshot() {
        return new Snapshot(text.toString(), selectionStart, selectionEnd);
    }

    /** Wide interface: restore from an opaque snapshot. */
    public void restore(EditorSnapshot snap) {
        Snapshot s = (Snapshot) snap;
        this.text = new StringBuilder(s.text);
        this.selectionStart = s.start;
        this.selectionEnd = s.end;
    }

    /** Private nested Memento: only TextEditor can read these fields. */
    private static final class Snapshot implements EditorSnapshot {
        private final String text;
        private final int start, end;
        private Snapshot(String text, int start, int end) {
            this.text = text; this.start = start; this.end = end;
        }
        @Override public String describe() {
            return "len=" + text.length() + " sel=[" + start + "," + end + "]";
        }
    }
}

/** CARETAKER */
class UndoManager {
    private final Deque<EditorSnapshot> undo = new ArrayDeque<>();
    private final Deque<EditorSnapshot> redo = new ArrayDeque<>();

    public void record(TextEditor e) { undo.push(e.snapshot()); redo.clear(); }

    public boolean undo(TextEditor e) {
        if (undo.isEmpty()) return false;
        redo.push(e.snapshot());
        e.restore(undo.pop());
        return true;
    }
    public boolean redo(TextEditor e) {
        if (redo.isEmpty()) return false;
        undo.push(e.snapshot());
        e.restore(redo.pop());
        return true;
    }
}

/** DEMO */
public class EditorDemo {
    public static void main(String[] args) {
        TextEditor editor = new TextEditor();
        UndoManager history = new UndoManager();

        history.record(editor);                 // snapshot empty state
        editor.insert("Hello");
        System.out.println(editor.content());   // "Hello"

        history.record(editor);                 // snapshot "Hello"
        editor.insert(", World");
        System.out.println(editor.content());   // "Hello, World"

        history.record(editor);                 // snapshot "Hello, World"
        editor.deleteLast(7);
        System.out.println(editor.content());   // "Hello"

        history.undo(editor);
        System.out.println(editor.content());   // "Hello, World"  (delete undone)

        history.undo(editor);
        System.out.println(editor.content());   // "Hello"         (2nd insert undone)

        history.redo(editor);
        System.out.println(editor.content());   // "Hello, World"  (redo)
    }
}
```

</details>

**Explanation:** This is the textbook Memento use case — undo/redo in an editor. `TextEditor` is the **Originator**; its mutable state is the `StringBuilder` plus the selection indices. `snapshot()` builds an opaque `Snapshot` **Memento** implemented as a *private nested class*, so only `TextEditor` can read its fields — the caretaker sees only the narrow `EditorSnapshot` interface (`describe()`), never the raw text. `UndoManager` is the **Caretaker**: it keeps two stacks, one for undo and one for redo, and orchestrates the flow without ever touching the editor's internals — it only calls `snapshot()`/`restore()` and shuffles opaque marker objects between the stacks. The key mechanisms: `text.toString()` is a genuine deep copy (Java strings are immutable, so no aliasing bug), pushing the current state onto the redo stack *before* restoring makes redo work, and `record()` clears the redo stack because a fresh edit invalidates any "future" you could have redone into — exactly how real editors behave. As with the other scenarios, unbounded history is the memory risk; production editors cap the undo depth or coalesce keystrokes into larger edit units.

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

Use the Memento pattern when:

- **You need snapshots for undo/redo, rollback, or checkpoints.** The canonical use case — you must restore an object to a prior state on demand.
- **Direct access to fields would break encapsulation.** You want to capture state *without* exposing the originator's internal representation via public getters/setters.
- **The originator owns invariants you must protect.** Because only the originator reads/writes the memento's contents, external code can't corrupt state during save/restore.
- **You want to decouple state-history management from the state itself.** The caretaker can manage arbitrarily complex histories (stacks, trees of branches) without knowing anything about what's inside a snapshot.
- **State transitions are hard to reverse operationally.** If you cannot easily compute an inverse operation (unlike Command's `undo()`), snapshotting the whole state is simpler and safer.

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

Avoid or reconsider Memento when:

- **State is large and snapshots are frequent.** Storing full copies can blow up memory. Prefer Command-based diffs, incremental/delta mementos, or copy-on-write structures.
- **Operations are cheaply and precisely reversible.** If each action has an obvious inverse (e.g., `add(x)` ↔ `remove(x)`), the **Command** pattern's `undo()` is more memory-efficient than snapshotting whole state.
- **The originator's state is mostly references to shared, mutable objects.** A shallow snapshot will alias live data (restore does nothing useful, or worse); a deep copy may be prohibitively expensive or even impossible (e.g., open sockets, threads).
- **A language/framework already provides it.** If you only need serialization-based persistence, `Serializable`, JSON, or a purpose-built library may suffice without hand-rolling the pattern.
- **The caretaker genuinely needs to inspect state.** If external code must read the snapshot's contents, you're not really preserving encapsulation and a plain value object/DTO may be the honest design.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**
- **Preserves encapsulation** — snapshot without exposing internal fields (the defining benefit).
- **Simplifies the originator** — the originator doesn't have to manage its own version history; the caretaker does.
- **Single Responsibility & separation of concerns** — state lives in the originator; history management lives in the caretaker.
- **Enables clean undo/redo, rollback, and checkpointing.**
- **Immutable mementos** are inherently safe to store and share.

**Cons**
- **Memory footprint** — full-state snapshots are costly; frequent snapshots of large objects can dominate memory.
- **CPU cost of deep copies** — safe snapshots of mutable graphs are expensive to produce.
- **Lifecycle management burden** — the caretaker must know when to discard mementos, or it leaks memory (unbounded history).
- **Deep vs. shallow copy bugs** — easy to accidentally alias mutable state and get "restores" that share references with live data.
- **Dynamic languages / weak access control** — the encapsulation guarantee depends on language features (nested-class access, `private`); harder to enforce where those don't exist.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

**Memento vs. Command**
- *Command* encapsulates an *operation* and can support undo by storing the inverse action (or the info needed to reverse it). *Memento* stores the *state* itself.
- Command undo is memory-efficient when operations are cheaply invertible; Memento is simpler and more robust when operations are hard to invert or when you want a true "restore point."
- They are frequently combined: a Command stores a Memento of the state it's about to change, and `undo()` restores it.

**Memento vs. Prototype**
- *Prototype* clones an object to create a *new independent object* (`clone()`), typically for creation/copying. *Memento* copies state to *restore the same object* later.
- A Memento often *uses* cloning/copy semantics internally, but its purpose is temporal restoration, not producing new instances for general use.

**Memento vs. State**
- *State* changes an object's behavior by swapping its current state object; it is about *behavioral variation*, not saving/restoring history.
- *Memento* captures a snapshot to roll back. They can coexist: a state machine might snapshot which State it was in, using Memento to restore it.

**Memento vs. plain serialization / DTO**
- Serialization is one *implementation technique* for a Memento (a serialized blob is an opaque snapshot). A DTO exposes fields publicly and is the *opposite* of the narrow-interface encapsulation Memento provides.

</details>

## 📊 Comparison Table

<details>
<summary>📖 Click to expand</summary>

Comparing the three implementation variants:

| Axis | V1: Naive getters/setters | V2: Narrow/wide inner class | V3: Production undo/redo |
|---|---|---|---|
| **Preserves encapsulation** | ❌ No — fully public | ✅ Yes — compiler-enforced | ✅ Yes — compiler-enforced |
| **Caretaker coupled to internals** | ❌ Tightly | ✅ Decoupled (marker interface) | ✅ Decoupled |
| **Copy semantics** | Shallow (field-by-field) | Shallow (fine for immutables) | ✅ Deep copy of mutable fields |
| **Memory cost** | Low (1 snapshot) | Low (1 snapshot) | Higher (bounded history of full copies) |
| **Undo support** | Last state only | Single snapshot | ✅ Full undo stack |
| **Redo support** | ❌ | ❌ | ✅ Redo stack |
| **Memento immutability** | ❌ Mutable | ✅ `final` fields | ✅ `final` + `List.copyOf` |
| **Memory bounding** | N/A | N/A | ✅ `maxDepth` cap |
| **Interview verdict** | "This breaks encapsulation" | Correct textbook pattern | Production-ready answer |

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

**Pitfall 1 — Shallow copy of mutable fields (the classic bug).** The memento stores a *reference* to a mutable object, so later mutations of the originator silently change the "saved" snapshot; restoring then does nothing (or restores the already-mutated state).

<details>
<summary>💻 What goes wrong</summary>

```java
class Board {
    private List<String> pieces = new ArrayList<>();
    void add(String p) { pieces.add(p); }
    // BUG: stores the SAME list reference, not a copy.
    Object save() { return pieces; }
    @SuppressWarnings("unchecked")
    void restore(Object m) { pieces = (List<String>) m; }
}
// save(); add("Q"); restore(); -> "Q" is STILL there: the snapshot aliased live state.
```

</details>

<details>
<summary>💻 The fix — deep copy on capture</summary>

```java
Object save() { return new ArrayList<>(pieces); }   // defensive copy
@SuppressWarnings("unchecked")
void restore(Object m) { pieces = new ArrayList<>((List<String>) m); } // copy back too
// For nested mutable objects, copy recursively (deep copy) or use immutable elements.
```

</details>

**Pitfall 2 — Memory bloat from unbounded history of full snapshots.** Every undo step keeps a complete copy of state; a long-running editor accumulates gigabytes.

<details>
<summary>💻 What goes wrong</summary>

```java
Deque<State> undo = new ArrayDeque<>();
void backup(Doc d) { undo.push(d.snapshot()); } // never bounded, never evicted -> OOM
```

</details>

<details>
<summary>💻 The fix — bound the history and/or store diffs</summary>

```java
private static final int MAX = 100;
void backup(Doc d) {
    undo.push(d.snapshot());
    if (undo.size() > MAX) undo.removeLast(); // drop oldest
}
// Better for large state: store incremental deltas (Command-style) instead of full copies.
```

</details>

**Pitfall 3 — Breaking encapsulation by exposing the memento's state.** Giving the memento public getters so the caretaker (or logs, or UI) can read fields turns it back into a leaky DTO.

<details>
<summary>💻 What goes wrong</summary>

```java
class Memento {
    public String content;  // public field -> caretaker mutates internals
    public int cursor;
}
```

</details>

<details>
<summary>💻 The fix — narrow interface, private nested class</summary>

```java
interface Snapshot { String label(); }          // narrow: metadata only
class Editor {
    private static final class Memento implements Snapshot { // private nested
        private final String content; private final int cursor; // package-invisible
        /* only Editor can read these */
    }
}
```

</details>

**Pitfall 4 — Serialization traps.** Using `Serializable` as the memento mechanism: non-serializable fields throw at runtime, `transient` fields silently vanish (restore to defaults), and `serialVersionUID` mismatches break old snapshots after a class change.

<details>
<summary>💻 What goes wrong</summary>

```java
class State implements Serializable {
    private Connection db;        // NotSerializableException at write time
    private transient int cache;  // silently becomes 0 on restore
    // no serialVersionUID -> auto-generated; class edit invalidates saved blobs
}
```

</details>

<details>
<summary>💻 The fix — control what serializes</summary>

```java
class State implements Serializable {
    private static final long serialVersionUID = 1L;  // stable, explicit
    private transient Connection db;                   // reconnect after restore, don't serialize
    private void readObject(ObjectInputStream in) throws Exception {
        in.defaultReadObject();
        this.db = reconnect();   // rebuild non-serializable resources
    }
}
```

</details>

</details>

## 🎓 Interview Tips

<details>
<summary>💬 What interviewers commonly ask</summary>

- "Design undo/redo for a text editor / drawing app." (This is the Memento interview in disguise — sometimes combined with Command.)
- "How do you snapshot an object's state *without* exposing its internals?" — they're testing whether you know the narrow/wide interface trick.
- "Memento vs. Command for undo — when would you pick each?" (memory vs. reversibility trade-off).
- "How do you avoid running out of memory with a long undo history?" (bounding, diffs, incremental snapshots).
- "Shallow vs. deep copy — where does Memento go wrong?" (aliasing mutable state).
- Advanced: "How would you snapshot state across a distributed system?" (Chandy–Lamport, checkpointing) or "Memento vs. event sourcing."

</details>

<details>
<summary>💬 What to proactively mention</summary>

- Say the words **"without violating encapsulation"** — that's the differentiator from a DTO/getter dump.
- Explain the **narrow interface (caretaker) vs. wide interface (originator)** and how Java's **private static nested class** enforces it.
- Note that mementos should be **immutable**.
- Raise **deep vs. shallow copy** for mutable fields *before* they ask.
- Bring up **memory bounding** and the option of **incremental/delta mementos** or combining with **Command** for reversible ops.
- Mention real implementations: `javax.swing.undo.UndoManager`, DB **savepoints**, and the relationship to **checkpointing/event sourcing** at scale. This signals staff-level breadth.

</details>

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Command** — often paired with Memento: a command stores a memento of pre-execution state so `undo()` can restore it. Command favors reversible operations; Memento favors full-state snapshots.
- **State** — represents behavioral modes as objects; a Memento can capture *which* state an object was in so a state machine can be rolled back.
- **Prototype** — provides cloning/copy semantics that a Memento may reuse internally to produce deep snapshots. Prototype creates new independent objects; Memento restores an existing one.
- **Iterator** — GoF notes both keep some external record of internal position/state; a Memento can even store an iterator's traversal state so iteration can be resumed.
- **Caretaker + Observer** — in UI apps, an undo manager (caretaker) is frequently notified of edits via Observer, then records a memento.

</details>

## 📚 Library/Framework Implementation

Real systems that implement Memento (or its ideas):

**1. `javax.swing.undo.UndoManager` + `UndoableEdit` (Java Swing).** Swing's edit framework is a textbook Memento+Command hybrid. `UndoManager` is the *caretaker*: it holds a list of `UndoableEdit` objects and coordinates `undo()`/`redo()`. Each `UndoableEdit` (e.g., `AbstractUndoableEdit`) captures the information needed to reverse an edit — effectively a memento of the affected state.

<details>
<summary>💻 Click to expand code</summary>

```java
import javax.swing.undo.*;
import javax.swing.text.*;

// A JTextComponent's Document fires UndoableEditEvents; UndoManager collects them.
Document doc = new PlainDocument();
UndoManager undoManager = new UndoManager();               // the caretaker
doc.addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));

doc.insertString(0, "Hello", null);   // generates an UndoableEdit (a memento)
if (undoManager.canUndo()) undoManager.undo();  // restores prior document state
if (undoManager.canRedo()) undoManager.redo();
```

</details>

**2. Database transaction SAVEPOINTs (JDBC / SQL).** A `SAVEPOINT` is a Memento of transaction state: you mark a restore point, and `ROLLBACK TO SAVEPOINT` restores the transaction to that snapshot without exposing the engine's internals. In JDBC this is `Connection.setSavepoint()` / `rollback(Savepoint)`.

<details>
<summary>💻 Click to expand code</summary>

```java
Connection conn = dataSource.getConnection();
conn.setAutoCommit(false);
Savepoint sp = conn.setSavepoint("beforeRisky");  // memento of transaction state
try {
    riskyUpdates(conn);
    conn.commit();
} catch (SQLException e) {
    conn.rollback(sp);   // restore to the savepoint (Memento restore)
    conn.commit();
}
```

</details>

**3. Spring Statemachine / Spring Web Flow state persistence.** Spring Statemachine exposes `StateMachineContext` and a `StateMachinePersister` that externalizes a machine's state so it can be stored (e.g., in Redis/DB) and restored later — a distributed Memento. Similarly, Spring Web Flow snapshots flow/conversation state so a user can navigate "back" through a wizard.

<details>
<summary>💻 Click to expand code</summary>

```java
// Spring Statemachine: persist and restore machine state (memento externalized to a store).
StateMachinePersister<States, Events, String> persister =
    new DefaultStateMachinePersister<>(new InMemoryStateMachinePersist());

persister.persist(stateMachine, "order-42");   // capture memento
// ... later, possibly on another node ...
persister.restore(stateMachine, "order-42");   // restore from memento
```

</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1 (Conceptual): What is the Memento pattern and what problem does it solve?</strong></summary>

The Memento pattern captures and externalizes an object's internal state so the object can be restored to that state later, **without violating encapsulation**. It solves the problem of implementing undo/rollback/checkpoints when directly exposing an object's fields (public getters/setters) would leak its internal representation and let external code corrupt invariants. It introduces three roles: the **Originator** (owns state, creates/consumes mementos), the **Memento** (opaque snapshot), and the **Caretaker** (stores mementos, decides when to restore, never inspects them). The defining feature — the thing that makes it more than a DTO copy — is that the caretaker sees only a *narrow* interface while the originator retains a *wide* interface to the snapshot's contents.

</details>

<details>
<summary><strong>Q2 (Conceptual): Explain the three participants and their responsibilities.</strong></summary>

- **Originator:** the object whose state we snapshot. It has a `save()` method that produces a Memento containing a copy of its current state, and a `restore(memento)` method that resets its state from a given Memento. Only the originator can read the memento's stored fields.
- **Memento:** an immutable value object holding the snapshot. It exposes a *wide* interface (full state access) to the originator and a *narrow* interface (no state, maybe metadata) to everyone else.
- **Caretaker:** manages the history of mementos (often a stack for undo/redo). It asks the originator for mementos and hands them back to trigger restores, but treats them as opaque tokens.

The separation means state ownership (originator) and history management (caretaker) are decoupled, honoring the Single Responsibility Principle.

</details>

<details>
<summary><strong>Q3 (Conceptual): What is the narrow vs. wide interface, and why does it matter?</strong></summary>

The **wide interface** exposes full access to the memento's stored state; the **narrow interface** exposes little or nothing (perhaps a name or timestamp). The **originator** gets the wide interface so it can capture and restore state; the **caretaker** gets only the narrow interface so it can hold and reorder mementos but never read or corrupt their contents. This dual-interface design is *the* mechanism that preserves encapsulation. In Java it's implemented with a `private static` nested class (wide access for the enclosing originator) exposed to outsiders only through a marker interface (narrow). Without this distinction, a "memento" is just a public DTO and encapsulation is lost.

</details>

<details>
<summary><strong>Q4 (Conceptual): How does Memento differ from simply serializing the object?</strong></summary>

Serialization is *one implementation technique* for a memento — a serialized byte array is an opaque snapshot. But the pattern is about the *roles and encapsulation guarantee*, not the storage mechanism. You can implement a memento with in-memory field copies, a clone, or serialization. Serialization adds concerns the pure pattern doesn't require: `transient` fields, non-serializable resources, versioning (`serialVersionUID`), and performance. Also, serialization typically captures the *whole* object graph, whereas a hand-written memento can capture exactly the fields relevant to restoration. So: serialization can *implement* Memento, but Memento ≠ serialization.

</details>

<details>
<summary><strong>Q5 (Conceptual): Is the Memento a creational, structural, or behavioral pattern, and why?</strong></summary>

It's a **behavioral** pattern. Behavioral patterns are concerned with how objects communicate and distribute responsibility. Memento distributes the responsibility of state ownership (originator) versus state-history management (caretaker) and defines a controlled communication protocol (save/restore through an opaque token). It doesn't create object hierarchies (structural) or abstract instantiation (creational); it governs behavior over time — capturing and restoring state — which is squarely behavioral.

</details>

<details>
<summary><strong>Q6 (Implementation): Code a minimal Memento in Java with proper encapsulation.</strong></summary>

The key is the private nested class + marker interface.

<details>
<summary>💻 Click to expand code</summary>

```java
interface Snapshot { }                 // narrow interface

class Counter {
    private int value;
    void inc() { value++; }
    int value() { return value; }

    Snapshot save() { return new Memento(value); }          // wide: create
    void restore(Snapshot s) { this.value = ((Memento) s).value; } // wide: consume

    private static final class Memento implements Snapshot {  // private nested
        private final int value;                              // invisible outside Counter
        private Memento(int value) { this.value = value; }
    }
}
```

</details>

The `Memento` fields are unreadable outside `Counter`, but `Counter` (the enclosing class) can access them — that's the wide/narrow split enforced by Java.

</details>

<details>
<summary><strong>Q7 (Implementation): Add full undo/redo support. Show the caretaker.</strong></summary>

Use two stacks: undo and redo. Push pre-change state on edit; move between stacks on undo/redo.

<details>
<summary>💻 Click to expand code</summary>

```java
class Caretaker {
    private final Deque<Snapshot> undo = new ArrayDeque<>();
    private final Deque<Snapshot> redo = new ArrayDeque<>();

    void backup(Counter c) { undo.push(c.save()); redo.clear(); } // call before mutating
    void undo(Counter c) {
        if (undo.isEmpty()) return;
        redo.push(c.save());   // save current so redo can return here
        c.restore(undo.pop());
    }
    void redo(Counter c) {
        if (redo.isEmpty()) return;
        undo.push(c.save());
        c.restore(redo.pop());
    }
}
```

</details>

The invariant: any *new* edit clears the redo stack (you've branched away from the redo history), matching user expectations in editors.

</details>

<details>
<summary><strong>Q8 (Implementation): How would you deep-copy mutable state into the memento?</strong></summary>

You must copy mutable fields at capture time and again at restore time so the snapshot and live state never alias.

<details>
<summary>💻 Click to expand code</summary>

```java
class Doc {
    private List<String> lines = new ArrayList<>();
    void add(String l) { lines.add(l); }

    Snapshot save() { return new Memento(new ArrayList<>(lines)); } // copy IN
    void restore(Snapshot s) { this.lines = new ArrayList<>(((Memento) s).lines); } // copy OUT

    private static final class Memento implements Snapshot {
        private final List<String> lines;
        private Memento(List<String> lines) { this.lines = List.copyOf(lines); } // immutable
    }
}
```

</details>

For deeply nested mutable graphs you either recursively deep-copy, make elements immutable, or serialize/deserialize to get a deep copy for free (at a performance cost).

</details>

<details>
<summary><strong>Q9 (Implementation): Implement a serialization-based memento for deep snapshots.</strong></summary>

Round-tripping through serialization yields a deep copy of the whole graph in one step.

<details>
<summary>💻 Click to expand code</summary>

```java
static <T extends Serializable> T deepSnapshot(T obj) {
    try {
        var bos = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bos)) { out.writeObject(obj); }
        var in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        @SuppressWarnings("unchecked") T copy = (T) in.readObject();
        return copy;   // fully independent deep copy = the memento's state
    } catch (Exception e) { throw new RuntimeException("snapshot failed", e); }
}
```

</details>

Caveats to mention: every field in the graph must be `Serializable`, `transient`/non-serializable resources need `readObject` handling, and it's slower and more memory-hungry than targeted copying. Good for correctness, often too heavy for hot paths.

</details>

<details>
<summary><strong>Q10 (Implementation): How do you expose harmless metadata (like a timestamp/label) on the memento without leaking state?</strong></summary>

Put the metadata on the *narrow* interface. The narrow interface can expose read-only, non-sensitive descriptors (name, timestamp, size) that the caretaker legitimately needs for its UI/history list, while the actual state stays behind the wide interface.

<details>
<summary>💻 Click to expand code</summary>

```java
interface Snapshot {                 // narrow: metadata only
    String label();
    long   createdAt();
}
class Editor {
    private static final class Memento implements Snapshot {
        private final String content;         // state — NOT on narrow interface
        private final String label;
        private final long createdAt = System.currentTimeMillis();
        private Memento(String content, String label) { this.content = content; this.label = label; }
        public String label()    { return label; }      // safe metadata
        public long   createdAt(){ return createdAt; }
    }
}
```

</details>

The caretaker can render "Undo: typed 'foo' at 10:31" without ever touching `content`.

</details>

<details>
<summary><strong>Q11 (Breaking): What goes wrong with a shallow copy in a memento?</strong></summary>

If the memento stores a *reference* to a mutable field rather than a copy, the snapshot aliases live state. After you mutate the originator, the "saved" memento reflects the new value, so restoring is a no-op (or restores corrupted state). This is the single most common Memento bug. Symptom: undo appears to do nothing, or all history entries mysteriously show the latest state because they all point to the same underlying list/map/StringBuilder. The fix is a defensive/deep copy at capture (and again at restore), or making the captured fields immutable.

</details>

<details>
<summary><strong>Q12 (Breaking): How can a naive undo history cause an OutOfMemoryError?</strong></summary>

Each memento holds a full copy of state. An unbounded history (no eviction) in a long-lived application accumulates one full snapshot per edit — a large document edited thousands of times can consume gigabytes. It's worsened if mementos transitively retain large object graphs, preventing GC. Fixes: bound the history depth (drop the oldest), coalesce rapid edits into a single memento, store **incremental diffs** instead of full states, or use copy-on-write/persistent data structures so unchanged parts are shared between snapshots. Also ensure mementos don't hold references to resources (streams, listeners) that keep other subsystems alive.

</details>

<details>
<summary><strong>Q13 (Breaking): What are the serialization pitfalls when a memento is Serializable?</strong></summary>

Several: (1) A non-`Serializable` field (e.g., `Connection`, `Thread`) throws `NotSerializableException` at capture. (2) `transient` fields are skipped and restore to defaults (null/0), silently losing state. (3) Missing/auto-generated `serialVersionUID` means any class change can invalidate previously stored snapshots with `InvalidClassException`. (4) Deserialization of untrusted memento data is a **security risk** (gadget-chain RCE). (5) The whole object graph is captured, which may be far larger than intended. Fixes: explicit `serialVersionUID`, mark and rebuild non-serializable fields in `readObject`, validate/whitelist classes on deserialization, and prefer explicit field copying for hot paths.

</details>

<details>
<summary><strong>Q14 (Breaking): If the caretaker can downcast the narrow interface to the concrete memento, is encapsulation really preserved?</strong></summary>

In pure Java, if the memento is a `private static` nested class of the originator, outside code *cannot even name the concrete type*, so it cannot downcast to it — encapsulation holds at compile time. Encapsulation can still be defeated by **reflection** (`setAccessible(true)`) unless a `SecurityManager`/module system restricts it. Also, if a developer sloppily makes the memento `public` with `public` fields "for convenience," the guarantee is gone. So the guarantee is real but depends on: keeping the memento private/nested, exposing only a narrow interface, and not opening reflective access. This is a great point to raise proactively — it shows you understand the mechanism's limits.

</details>

<details>
<summary><strong>Q15 (Trade-off): Memento vs. Command for implementing undo — how do you choose?</strong></summary>

**Command undo** stores the *operation* and reverses it (either an explicit inverse or enough info to invert). It's memory-efficient when operations are small and cleanly invertible (typing a char ↔ deleting it). **Memento** stores the *whole relevant state* and restores it wholesale. Choose Command when operations are numerous, cheap, and reversible; choose Memento when operations are hard to invert, when the reverse is error-prone, or when you want a guaranteed exact restore point regardless of intervening logic. In practice they're combined: each Command captures a Memento of the state it will change and restores it on `undo()`. Trade-off summary: Command = less memory, more logic; Memento = more memory, simpler and safer.

</details>

<details>
<summary><strong>Q16 (Trade-off): Full snapshots vs. incremental/delta mementos — pros and cons?</strong></summary>

**Full snapshots** are simple and O(1) to restore (just swap state in) but O(state size) memory per entry. **Incremental/delta mementos** store only what changed since the previous memento, so memory per edit is O(change size) — vastly cheaper for large state with small edits. The cost: restoring to an old point may require replaying/undoing a chain of deltas (O(number of steps)), and delta logic is more complex and bug-prone. A common hybrid is **periodic full snapshots + deltas in between** (like database checkpoints + WAL), giving bounded restore time and bounded memory. Choose based on state size, edit granularity, and how often you restore vs. capture.

</details>

<details>
<summary><strong>Q17 (Trade-off): Where should the deep-copy responsibility live — originator or caretaker?</strong></summary>

The **originator**, always. Only the originator knows its internal representation and its invariants, so only it can correctly deep-copy mutable fields and rebuild non-serializable resources. If the caretaker tried to copy, it would need to understand the originator's internals — reintroducing the coupling and encapsulation loss the pattern exists to prevent. So `save()`/`restore()` on the originator own copy semantics; the caretaker only decides *when* and *which* memento, and manages the collection. This keeps copy correctness co-located with the knowledge required to get it right.

</details>

<details>
<summary><strong>Q18 (Advanced): How is Memento related to distributed snapshots and the Chandy–Lamport algorithm?</strong></summary>

Memento is the single-object version of the general problem "capture state so you can restore/analyze it later." In a **distributed system**, capturing a globally consistent snapshot is hard because there's no global clock and messages are in flight. The **Chandy–Lamport** algorithm solves this: a process records its own local state (its local *memento*) and then records the state of incoming channels by using marker messages, producing a consistent global snapshot without stopping the system. Stream processors like **Apache Flink** use a Chandy–Lamport variant (asynchronous barrier snapshotting) to checkpoint operator state for exactly-once recovery — each operator's checkpointed state is essentially a distributed memento. Bringing this up signals staff-level understanding: local mementos + a consistency protocol = distributed checkpointing.

</details>

<details>
<summary><strong>Q19 (Advanced): Compare Memento with Event Sourcing.</strong></summary>

**Memento** stores *state snapshots* — the "what it looks like now." **Event Sourcing** stores the *sequence of events* (state changes) and reconstructs state by replaying them from the beginning — the "how it got here." Event sourcing gives a full audit log, time-travel to any point, and natural derivation of multiple read models, but replay can be slow for long histories — which is why event-sourced systems periodically store **snapshots** (mementos!) to avoid replaying from the origin. So they're complementary: event sourcing = deltas as first-class events; memento snapshots = periodic full-state checkpoints layered on top for performance. Memento is memory/compute-cheap to restore but loses intermediate history; event sourcing preserves everything at the cost of replay complexity.

</details>

<details>
<summary><strong>Q20 (Advanced): Is the Memento pattern thread-safe? How would you make the history safe under concurrency?</strong></summary>

Not inherently. Two hazards: (1) capturing a snapshot while another thread mutates the originator yields a torn/inconsistent memento; (2) concurrent access to the caretaker's undo/redo stacks corrupts them. Fixes: make `save()`/`restore()` and the mutation methods mutually exclusive via a lock on the originator (or make the originator immutable so snapshots are trivially consistent — capture just references the current immutable value). For the caretaker, use a concurrent/locked structure or confine undo/redo to a single thread (common in UI toolkits — the Event Dispatch Thread). If mementos are immutable (recommended), they're safe to *share* across threads; the danger is purely in the capture moment and the mutable stacks.

<details>
<summary>💻 Click to expand code</summary>

```java
class SafeOriginator {
    private final Object lock = new Object();
    private State state;
    Snapshot save()               { synchronized (lock) { return new Memento(state); } }
    void restore(Snapshot s)      { synchronized (lock) { this.state = ((Memento) s).state; } }
    void mutate(State next)       { synchronized (lock) { this.state = next; } }
}
```

</details>

</details>

### 🧩 Full Coding Challenge #1 — Bank Account with Transactional Rollback

<details>
<summary><strong>Challenge: Implement a BankAccount supporting begin/commit/rollback using Memento. A rollback must restore balance and transaction log exactly, without exposing internal fields.</strong></summary>

**Approach:** The account is the originator. On `begin()` we capture a memento (deep-copying the mutable transaction log). `rollback()` restores it; `commit()` discards it. Encapsulation is preserved via a private nested memento and a narrow interface.

<details>
<summary>💻 Click to expand full solution</summary>

```java
import java.util.*;

interface AccountSnapshot { long createdAt(); }   // narrow interface

class BankAccount {
    private long balanceCents;
    private final List<String> ledger = new ArrayList<>();
    private final Deque<AccountSnapshot> txStack = new ArrayDeque<>(); // savepoints

    public void deposit(long cents) {
        if (cents <= 0) throw new IllegalArgumentException("positive only");
        balanceCents += cents;
        ledger.add("deposit " + cents);
    }
    public void withdraw(long cents) {
        if (cents > balanceCents) throw new IllegalStateException("insufficient funds");
        balanceCents -= cents;
        ledger.add("withdraw " + cents);
    }
    public long balance() { return balanceCents; }
    public List<String> ledgerView() { return List.copyOf(ledger); }

    // --- transaction control using mementos ---
    public void begin()  { txStack.push(save()); }          // savepoint
    public void commit() { if (!txStack.isEmpty()) txStack.pop(); } // discard savepoint
    public void rollback() {                                // restore savepoint
        if (txStack.isEmpty()) throw new IllegalStateException("no active transaction");
        restore(txStack.pop());
    }

    private AccountSnapshot save() {
        return new Memento(balanceCents, new ArrayList<>(ledger)); // deep copy ledger
    }
    private void restore(AccountSnapshot s) {
        Memento m = (Memento) s;
        this.balanceCents = m.balanceCents;
        this.ledger.clear();
        this.ledger.addAll(m.ledger);                       // copy back out
    }

    private static final class Memento implements AccountSnapshot {
        private final long balanceCents;
        private final List<String> ledger;
        private final long createdAt = System.currentTimeMillis();
        private Memento(long balanceCents, List<String> ledger) {
            this.balanceCents = balanceCents;
            this.ledger = List.copyOf(ledger);              // immutable inside memento
        }
        public long createdAt() { return createdAt; }
    }
}

class BankDemo {
    public static void main(String[] args) {
        BankAccount acc = new BankAccount();
        acc.deposit(10_000);            // $100.00
        acc.begin();                    // savepoint A
        acc.withdraw(3_000);            // $70.00
        System.out.println(acc.balance()); // 7000
        try {
            acc.begin();                // savepoint B
            acc.withdraw(9_000);        // throws: insufficient funds
        } catch (IllegalStateException e) {
            acc.rollback();             // undo the failed nested tx -> back to $70.00
        }
        System.out.println(acc.balance()); // 7000
        acc.rollback();                 // roll back savepoint A -> back to $100.00
        System.out.println(acc.balance()); // 10000
        System.out.println(acc.ledgerView()); // [deposit 10000]
    }
}
```

</details>

Note how `txStack` gives nested savepoints (like SQL `SAVEPOINT`), the ledger is deep-copied so rollback truly restores it, and the caretaker role is folded into the account for a transaction API while still keeping memento fields private.

</details>

### 🧩 Full Coding Challenge #2 — Generic Reusable History<T>

<details>
<summary><strong>Challenge: Build a generic, reusable undo/redo History<M> that works for any originator, with bounded memory and redo support.</strong></summary>

**Approach:** Define generic `Originator<M>` (produces/consumes snapshots of type `M`) and a `History<M>` caretaker with bounded undo/redo stacks. This decouples history management entirely from any specific originator.

<details>
<summary>💻 Click to expand full solution</summary>

```java
import java.util.*;

/** Any originator can plug in by producing/consuming its own snapshot type M. */
interface Originator<M> {
    M save();
    void restore(M memento);
}

/** Reusable caretaker: bounded undo + redo. */
class History<M> {
    private final Originator<M> origin;
    private final Deque<M> undo = new ArrayDeque<>();
    private final Deque<M> redo = new ArrayDeque<>();
    private final int maxDepth;

    History(Originator<M> origin, int maxDepth) {
        this.origin = origin; this.maxDepth = maxDepth;
    }
    /** Call before each state-changing action. */
    void checkpoint() {
        undo.push(origin.save());
        if (undo.size() > maxDepth) undo.removeLast();
        redo.clear();
    }
    boolean undo() {
        if (undo.isEmpty()) return false;
        redo.push(origin.save());
        origin.restore(undo.pop());
        return true;
    }
    boolean redo() {
        if (redo.isEmpty()) return false;
        undo.push(origin.save());
        origin.restore(redo.pop());
        return true;
    }
}

/** Example originator: a shape with position. Its memento is a simple immutable record. */
class Shape implements Originator<Shape.Memo> {
    private int x, y;
    void moveTo(int x, int y) { this.x = x; this.y = y; }
    String pos() { return "(" + x + "," + y + ")"; }

    public Memo save() { return new Memo(x, y); }
    public void restore(Memo m) { this.x = m.x(); this.y = m.y(); }

    record Memo(int x, int y) { }   // immutable snapshot; fields private to record
}

class HistoryDemo {
    public static void main(String[] args) {
        Shape s = new Shape();
        History<Shape.Memo> h = new History<>(s, 50);

        h.checkpoint(); s.moveTo(10, 10);
        h.checkpoint(); s.moveTo(20, 25);
        System.out.println(s.pos()); // (20,25)

        h.undo(); System.out.println(s.pos()); // (10,10)
        h.undo(); System.out.println(s.pos()); // (0,0)
        h.redo(); System.out.println(s.pos()); // (10,10)
    }
}
```

</details>

This is the reusable, production shape of the pattern: `History<M>` knows nothing about `Shape`; the memento type `M` is chosen by the originator. Swap in any `Originator<M>` and you get undo/redo for free.

</details>

---

### 🏛️ Staff / Principal Engineer Deep Dive

<details>
<summary><strong>SP1: Design an undo system for a collaborative real-time editor (like Google Docs). How does Memento interact with OT/CRDTs?</strong></summary>

In single-user editing, a Memento stack works. In **collaborative** editing, "undo" is subtle: undoing *your* last edit while others have concurrently edited means you can't just restore an old global snapshot — that would discard their work. So you shift from global mementos to **selective/local undo** built on **Operational Transformation (OT)** or **CRDTs**. You represent each edit as an operation (Command-like), and undo generates an *inverse operation* that is then transformed against concurrent operations so it only reverses *your* change. Mementos still appear as **periodic snapshots/checkpoints** of the CRDT/document state to bound the size of the operation log and speed up loading. So the staff-level answer: full-state Memento doesn't scale to multi-user undo; you use inverse-operations (Command) + transformation for undo semantics, and Memento-style snapshots for compaction and fast open. This mirrors event sourcing (operations) + snapshots.

</details>

<details>
<summary><strong>SP2: How would you design memory-efficient incremental snapshots for a large in-memory dataset with frequent small edits?</strong></summary>

Use **persistent (immutable) data structures** with **structural sharing** — a "snapshot" is just a new root pointer; unchanged subtrees are shared between versions, so each memento costs O(change) not O(size). This is how Clojure's persistent maps/vectors and Git's tree objects work. Alternatively, keep a **base full snapshot + append-only delta log**, and periodically **compact** (fold deltas into a new base) to bound restore time — the checkpoint + WAL model. Add **copy-on-write** at the page/segment level for very large flat data. Combine with **eviction policies** (keep every edit for the last N, then coarsen older history to milestones). The key insight to state: full-copy mementos are the naive baseline; production systems get memory efficiency from structural sharing or delta+checkpoint schemes, trading restore-time complexity for memory.

</details>

<details>
<summary><strong>SP3: Relate Memento to database MVCC and snapshot isolation.</strong></summary>

Database **MVCC (Multi-Version Concurrency Control)** is Memento at systems scale: instead of overwriting rows, the engine keeps multiple *versions* (mementos) of each row tagged with transaction IDs. A transaction reads a consistent **snapshot** — effectively a memento of the database as of its start — providing snapshot isolation without read locks. `SAVEPOINT`/`ROLLBACK TO` expose transaction-level mementos to the application. Old versions are reclaimed by **garbage collection / VACUUM** once no transaction can see them — the same lifecycle concern as evicting stale mementos to avoid memory bloat. The parallels to draw: versions = mementos, snapshot isolation = restoring/reading a consistent past state, VACUUM = caretaker eviction. It shows the pattern's ideas scale from a single object to a storage engine.

</details>

<details>
<summary><strong>SP4: How does Memento relate to checkpoint/restart and fault tolerance in long-running systems (e.g., Flink, Spark, ML training)?</strong></summary>

Checkpointing *is* Memento applied to fault tolerance: periodically snapshot enough state to resume after a crash without restarting from scratch. In **Apache Flink**, operator state is snapshotted via asynchronous barrier snapshotting (a Chandy–Lamport variant) to durable storage; on failure the job restores the last consistent checkpoint (memento) and replays inputs from the recorded offsets — giving exactly-once semantics. **Spark** persists RDD lineage plus periodic checkpoints to truncate long lineage chains. **ML training** saves model weights + optimizer state + step number as checkpoints so a preempted job resumes mid-epoch. The universal design tensions are exactly the Memento trade-offs at scale: snapshot frequency (memory/IO cost vs. lost-work-on-failure), full vs. incremental checkpoints, and consistency of the captured state. Framing it this way — "checkpointing is distributed Memento with a durability + consistency protocol" — is a strong staff-level signal.

</details>

<details>
<summary><strong>SP5: When would you deliberately choose event sourcing over Memento snapshots at the architecture level, and how do you combine them?</strong></summary>

Choose **event sourcing** when the *history itself* is a first-class business requirement: audit/compliance, temporal queries ("what did the balance look like on March 3?"), debugging by replay, deriving multiple read models (CQRS), or reconstructing state after a bug fix by replaying corrected logic. Choose **pure Memento snapshots** when you only care about *current state* and fast restore, and history has no business value — it's cheaper and simpler. The mature answer is you combine them: event sourcing as the source of truth (append-only event log) plus **periodic snapshots (mementos)** so you don't replay from the beginning of time — you load the latest snapshot and replay only subsequent events. Snapshot cadence is a tuning knob: more frequent = faster recovery but more storage. This is the canonical event-sourcing-with-snapshots architecture, and naming both the "why event sourcing" business drivers and the "snapshots for performance" optimization demonstrates principal-level judgment.

</details>

---

## ⚡ Quick Revision

**One-liner:** Memento captures and restores an object's internal state *without violating encapsulation*, using an opaque snapshot handed from the Originator to a Caretaker.

**The whole pattern in one paragraph:** The **Originator** owns some state and can produce a **Memento** (an immutable snapshot) via `save()` and reset itself via `restore(memento)`. The **Caretaker** stores mementos (typically in undo/redo stacks) and decides when to restore, but treats each memento as an opaque token — it never reads the contents. Encapsulation is preserved through the **wide interface** (full state access, available only to the Originator) versus the **narrow interface** (metadata only, available to the Caretaker); in Java this is enforced by making the Memento a `private static` nested class exposed through an empty marker interface, so only the enclosing Originator can read its private fields. Mementos should be **immutable** and must **deep-copy** any mutable fields at capture time (and copy back on restore) to avoid aliasing live state — the #1 bug. Because full-state snapshots cost memory, production systems **bound history depth**, coalesce edits, or switch to **incremental/delta mementos** or persistent structures with structural sharing. It's the state-snapshot counterpart to **Command** (which stores reversible operations); the two are often combined. At scale the same idea becomes DB savepoints/MVCC, checkpointing (Flink/Spark), and event-sourcing-with-snapshots, with the distributed-consistency version being the **Chandy–Lamport** algorithm.

**Top 5 interview answers to memorize:**

1. **What is it?** → "Capture and externalize an object's state so it can be restored later *without violating encapsulation*, via Originator, Memento, and Caretaker."
2. **How is encapsulation preserved?** → "Wide interface to the Originator, narrow interface to the Caretaker — implemented as a `private static` nested Memento class exposed through a marker interface, so only the Originator can read its fields."
3. **Biggest bug?** → "Shallow copy of mutable fields — the memento aliases live state, so undo does nothing. Fix: deep-copy on capture and immutable mementos."
4. **Memento vs. Command for undo?** → "Command stores reversible operations (memory-cheap when invertible); Memento stores whole-state snapshots (simpler, robust when ops aren't invertible). Often combined."
5. **How to control memory?** → "Bound history depth, coalesce edits, or use incremental/delta mementos or persistent structures with structural sharing instead of full copies."

**Trigger words (hear these → think Memento):** "undo/redo", "restore previous state", "rollback", "savepoint", "checkpoint", "snapshot", "revert", "without exposing internals / without breaking encapsulation", "game save", "transaction rollback", "back button in a wizard", "restore point".

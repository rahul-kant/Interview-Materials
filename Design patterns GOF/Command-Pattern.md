# Command Pattern ⭐⭐⭐⭐ (Difficulty: 4/5 — simple core, but undo/redo, queuing, and idempotency in distributed systems get deep)

> **Category:** Behavioral Pattern (GoF)
> **Also known as:** Action, Transaction

The Command pattern turns a **request into a standalone object** that carries everything needed to perform it. That reification is what makes undo/redo, queues, job schedulers, macros, transaction logs, and retryable distributed operations possible. It's the pattern behind `Runnable`, Swing `Action`s, thread-pool tasks, and every "command bus" in a CQRS system.

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 0: Direct Coupling — No Command (Anti-pattern)](#variant-0-direct-coupling--no-command-anti-pattern)
   - [Variant 1: Basic Command Objects](#variant-1-basic-command-objects)
   - [Variant 2: Command with Undo (Reversible Commands)](#variant-2-command-with-undo-reversible-commands)
   - [Variant 3: Invoker with History — Undo/Redo Stacks + Macro Commands](#variant-3-invoker-with-history--undoredo-stacks--macro-commands)
   - [Variant 4: Functional Commands (Java 8+ lambdas & method references)](#variant-4-functional-commands-java-8-lambdas--method-references)
   - [Variant 5: Production-Grade — Command Bus / Queue with Retry & Idempotency](#variant-5-production-grade--command-bus--queue-with-retry--idempotency)
5. [🎨 Real-World Example](#-real-world-example)
   - [Scenario 1 — Collaborative Text Editor (undo/redo)](#scenario-1--collaborative-text-editor-undoredo--macros--coalescing)
   - [Scenario 2 — Job Scheduler / Task Queue](#scenario-2--job-scheduler--task-queue)
   - [Scenario 3 — Payment Processing Pipeline](#scenario-3--payment-processing-pipeline-saga-with-compensating-undo)
   - [Scenario 4 — Kafka Consumer Command Executor](#scenario-4--kafka-consumer-command-executor)
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

> **GoF Definition:** *"Encapsulate a request as an object, thereby letting you parameterize clients with different requests, queue or log requests, and support undoable operations."*

The Command pattern exists to **decouple the object that *invokes* an operation from the object that *knows how to perform it*.** By wrapping a request — the receiver, the method to call, and the arguments — inside a first-class object, you can treat the request itself as data: pass it around, store it in a list, put it on a queue, log it to disk, execute it later or on another thread, and reverse it.

The key insight is **reification**: a method call (`light.on()`) is normally a transient event that happens and is gone. Command makes it a *thing* (`new TurnOnCommand(light)`) that exists independently of when or whether it runs. Once a request is an object, all the powerful capabilities the GoF definition lists — parameterization, queuing, logging, and undo — fall out naturally, because you can now do to a request anything you can do to any object.

---

## 🎯 Problem

You need to issue requests to objects, but you don't want the issuing code to know *what* the request does or *who* handles it — and you often need to do more with the request than just fire it immediately (delay it, queue it, log it, undo it, retry it). Hard-wiring the caller to call a specific method on a specific receiver makes all of that impossible.

**The pain points that lead you to Command:**

- The **invoker** (a button, a menu item, a scheduler, a queue consumer) must trigger an operation but should be **decoupled** from the concrete operation and its receiver.
- You need to **parameterize objects with an action to perform** — e.g., a generic `Button` that can be configured to do *anything* without subclassing per action.
- You need to **queue, schedule, or run requests on a different thread** (thread pools, job systems) — which requires the request to outlive the moment of calling.
- You need **undo/redo**, **transaction logs / replay**, or **macros** — all of which require the request to be stored and, for undo, reversed.

**Concrete example scenarios:**

1. **GUI actions (the classic).** A menu item, a toolbar button, and a keyboard shortcut should all trigger "Paste" without each knowing how paste works. Each holds a `PasteCommand`; the widget just calls `command.execute()`.

2. **Text editor undo/redo.** Every edit (insert, delete, format) is a command pushed onto an undo stack; `undo()` reverses the last one, `redo()` reapplies it. Impossible without reified, reversible requests.

3. **Job/task queues & thread pools.** `ExecutorService.submit(Runnable)` — the `Runnable` *is* a command: the work is packaged as an object, queued, and executed later by a worker thread. Same for message-queue consumers processing command messages.

4. **Transactional / distributed operations (CQRS).** A `PlaceOrderCommand` is validated, logged, put on a bus, handled asynchronously, retried on failure, and replayed for recovery — the command object is the unit of work, audit, and idempotency.

---

## ✅ Solution

The core idea, in plain language: **wrap each request in an object that exposes a single `execute()` method. The object holds a reference to the *receiver* (who does the real work) and any parameters. An *invoker* triggers the command by calling `execute()` without knowing what it does; the *client* wires up which command goes to which invoker.** Because the request is now an object, you can store it, queue it, log it, and (if it also implements `undo()`) reverse it.

**Key structural elements:**

- **Command (interface):** declares `execute()` (and often `undo()`). This is the common contract that lets invokers treat all requests uniformly.
- **Concrete Command:** implements `execute()` by invoking one or more operations on a **Receiver**. It stores the receiver and the parameters needed, and (for undo) records the information required to reverse the effect.
- **Receiver:** the object that knows how to actually perform the work (`Light`, `Document`, `Account`). The command delegates to it.
- **Invoker:** holds and triggers commands (`Button`, `RemoteControl`, `Scheduler`, `Queue`). It calls `execute()` and may keep a history for undo/redo. It's decoupled from concrete commands and receivers.
- **Client:** creates concrete commands, sets their receivers, and assigns them to invokers (the wiring).

**The mechanism that makes it work:** *reification of a request into an object plus indirection through a uniform interface*. The invoker depends only on the `Command` interface (dependency inversion), so it can hold, delay, queue, or log any command identically. Undo works because the concrete command **captures the state or inverse operation needed to reverse itself** at execute time (either storing a Memento of prior state, or knowing its logical inverse). Macros work because a `MacroCommand` is itself a Command containing a list of commands — the Composite pattern applied to Command.

---

## 💻 Implementation

We'll model a **cloud file-manager** (think the backend behind Dropbox / Google Drive / an S3 console) — a domain every engineer understands and one that naturally exercises *every* capability of the pattern. Users perform operations (`upload`, `delete`, `rename`, `move`) that must be **undoable** (a "Trash" with restore), **batchable** (organize 100 files in one action), and — on the server — **queued, retried, and made idempotent** (uploads processed by background workers). We'll evolve from tight coupling to a production, retryable, idempotent command pipeline. Each variant fixes a specific weakness of the previous one, so read them top-to-bottom as one story.

### Variant 0: Direct Coupling — No Command (Anti-pattern)

**What's wrong with it:** The invoker — here a `FileToolbar` (the UI/CLI layer that turns a clicked button into an action) — hard-codes exactly which receiver and method each button triggers, with a giant `switch` on the button id. Adding an operation or reassigning a button means editing the toolbar. The toolbar *is* coupled to the concrete `StorageService`; there's no way to queue, log, undo, or dynamically reassign actions. This is the tight coupling Command is designed to remove.

<details>
<summary>💻 Click to expand code — the anti-pattern (file-manager toolbar)</summary>

```java
// The receiver: the thing that actually does the work.
class StorageService {
    void upload(String file) { System.out.println("Uploaded " + file); }
    void delete(String file) { System.out.println("Deleted "  + file); }
    void rename(String from, String to) { System.out.println("Renamed " + from + " -> " + to); }
}

// DON'T DO THIS — the invoker knows the receiver and hard-wires every dispatch.
class FileToolbarNaive {
    private final StorageService storage;
    FileToolbarNaive(StorageService storage) { this.storage = storage; }

    void click(String button, String arg1, String arg2) {
        switch (button) {                         // coupled to concrete receiver + methods
            case "upload": storage.upload(arg1);         break;
            case "delete": storage.delete(arg1);         break;
            case "rename": storage.rename(arg1, arg2);   break;
            // add an operation? edit this class. Reassign a button? edit this class.
            // want undo? log the click? run it on a worker thread? impossible here.
            default: System.out.println("Unknown button: " + button);
        }
    }

    // ---------- Usage demo ----------
    public static void main(String[] args) {
        FileToolbarNaive toolbar = new FileToolbarNaive(new StorageService());
        toolbar.click("upload", "budget.xlsx", null);   // Uploaded budget.xlsx
        toolbar.click("rename", "budget.xlsx", "q3.xlsx"); // Renamed budget.xlsx -> q3.xlsx
        toolbar.click("delete", "q3.xlsx", null);        // Deleted q3.xlsx
        // No way to undo that delete. No history. No queuing. The request vanished.
    }
}
```
</details>

**Pros:** Dead simple for a fixed, tiny set of actions.
**Cons:** Invoker is tightly coupled to the receiver; buttons can't be reassigned at runtime; no queuing, logging, undo, or macros possible; adding operations violates Open/Closed. The request is *not* an object, so you can't do anything with it except fire it immediately.
**Mechanism (why it's fragile):** the request is an *inline method call*, not data — there's no seam to store, defer, or reverse it.

---

### Variant 1: Basic Command Objects

**What problem it solves:** Introduce a `Command` interface with `execute()`. Each file operation becomes a concrete command holding its **receiver** (`StorageService`) plus its arguments; the **invoker** (`FileToolbar`) holds `Command` references and just calls `execute()`, decoupled from what the command does. Now buttons are reassignable at runtime and the toolbar knows nothing about `StorageService` or its method signatures.

<details>
<summary>💻 Click to expand code — basic commands (file operations)</summary>

```java
// Command interface — the uniform contract.
interface Command { void execute(); }

// Receiver — knows how to do the real work.
class StorageService {
    void upload(String file) { System.out.println("Uploaded " + file); }
    void delete(String file) { System.out.println("Deleted "  + file); }
    void rename(String from, String to) { System.out.println("Renamed " + from + " -> " + to); }
}

// Concrete commands — each binds a receiver + the arguments for one operation.
class UploadCommand implements Command {
    private final StorageService storage;
    private final String file;
    UploadCommand(StorageService storage, String file) { this.storage = storage; this.file = file; }
    public void execute() { storage.upload(file); }
}
class DeleteCommand implements Command {
    private final StorageService storage;
    private final String file;
    DeleteCommand(StorageService storage, String file) { this.storage = storage; this.file = file; }
    public void execute() { storage.delete(file); }
}
class RenameCommand implements Command {
    private final StorageService storage;
    private final String from, to;
    RenameCommand(StorageService storage, String from, String to) { this.storage = storage; this.from = from; this.to = to; }
    public void execute() { storage.rename(from, to); }
}

// Invoker — holds commands, triggers them, knows NOTHING about the receiver.
class FileToolbar {
    private final Map<String, Command> buttons = new HashMap<>();
    void bind(String button, Command c) { buttons.put(button, c); }
    void click(String button) {
        buttons.getOrDefault(button, () -> System.out.println("(unbound button)")) // Null Object
               .execute();
    }
}

// Client — wires commands to the invoker.
class ToolbarDemo {
    public static void main(String[] args) {
        StorageService storage = new StorageService();
        FileToolbar toolbar = new FileToolbar();

        // Buttons are configured with commands — the toolbar is generic.
        toolbar.bind("upload", new UploadCommand(storage, "budget.xlsx"));
        toolbar.bind("rename", new RenameCommand(storage, "budget.xlsx", "q3.xlsx"));
        toolbar.bind("delete", new DeleteCommand(storage, "q3.xlsx"));

        toolbar.click("upload"); // Uploaded budget.xlsx
        toolbar.click("rename"); // Renamed budget.xlsx -> q3.xlsx
        toolbar.click("delete"); // Deleted q3.xlsx
        toolbar.click("share");  // (unbound button) — no NPE thanks to the Null Object default
    }
}
```
</details>

**Pros:** Invoker fully decoupled from the receiver (depends only on `Command`); buttons reassignable at runtime; adding a new operation = adding a command class (Open/Closed); the **Null Object** command removes null checks.
**Cons:** One class per operation → class proliferation for many trivial actions; no undo yet; the client still does the wiring.
**Mechanism:** *indirection through the `Command` interface* — `buttons.get(button).execute()` dispatches virtually to whatever command is bound, so the invoker is oblivious to the concrete operation. The request is now an **object stored in a map** — the seam that enables everything later.

---

### Variant 2: Command with Undo (Reversible Commands)

**What problem it solves:** The whole point of a "Trash" and a Ctrl+Z in a file manager is that operations are reversible. Add `undo()` to the interface so a command can **reverse its own effect**. Two strategies show up naturally here: (a) **logical inverse** — the undo of `rename(a→b)` is `rename(b→a)`; (b) **state Memento** — a `delete` isn't cleanly invertible (the file is gone), so before executing we capture the deleted file's bytes/metadata and restore them on undo. This is exactly how a real trash-and-restore works.

<details>
<summary>💻 Click to expand code — reversible commands (inverse + memento)</summary>

```java
interface Command {
    void execute();
    void undo();
}

// Receiver with enough surface to support restore (soft delete).
class StorageService {
    void rename(String from, String to) { System.out.println("Renamed " + from + " -> " + to); }
    byte[] read(String file)             { return ("contents-of-" + file).getBytes(); }
    void deleteHard(String file)         { System.out.println("Deleted " + file); }
    void restore(String file, byte[] data) { System.out.println("Restored " + file + " (" + data.length + " bytes)"); }
}

// (a) Logical inverse — rename back to the original name.
class RenameCommand implements Command {
    private final StorageService storage;
    private final String from, to;
    RenameCommand(StorageService storage, String from, String to) { this.storage = storage; this.from = from; this.to = to; }
    public void execute() { storage.rename(from, to); }
    public void undo()    { storage.rename(to, from); }   // inverse: swap the arguments
}

// (b) Memento — a delete destroys data, so capture it BEFORE deleting, restore on undo.
class DeleteCommand implements Command {
    private final StorageService storage;
    private final String file;
    private byte[] backup;                  // the Memento: captured file contents
    DeleteCommand(StorageService storage, String file) { this.storage = storage; this.file = file; }
    public void execute() {
        backup = storage.read(file);        // snapshot BEFORE destroying
        storage.deleteHard(file);
    }
    public void undo() {
        storage.restore(file, backup);      // put back exactly what was there
    }
}
```
</details>

**Pros:** Enables undo/redo (the Trash feature); the memento approach handles non-invertible operations correctly by restoring captured prior state; each command is self-contained (knows how to reverse itself).
**Cons:** Every command must implement `undo()` correctly — a real burden and a common bug source; memento commands hold captured state (a deleted large file's bytes cost memory); complex operations may need deep snapshots; commands become stateful (not freely shareable).
**Mechanism:** *self-inverse capture* — a command either knows its **logical inverse** (rename back) or stores a **Memento of prior state at execute time** (the deleted bytes), so `undo()` deterministically reverses exactly what `execute()` did. Capturing *before* mutating is the crucial ordering — snapshot the file *before* the delete, or there's nothing left to restore.

---

### Variant 3: Invoker with History — Undo/Redo Stacks + Macro Commands

**What problem it solves:** A file manager needs multi-level undo (Ctrl+Z repeatedly) and "batch" operations — e.g., "Organize" that renames + moves 50 files in one click, undone as a single unit. Move history management into an invoker: an **undo stack** and a **redo stack** give full multi-level undo/redo. Add a **`MacroCommand`** (Composite pattern) that bundles several commands into one — executing runs them in order, undoing reverses them in *reverse* order. This is exactly how the "Undo move of 50 items" in Drive/Dropbox works.

<details>
<summary>💻 Click to expand code — undo/redo history + macro command (batch organize)</summary>

```java
import java.util.*;

// Invoker managing full undo/redo history for the file manager.
class FileHistory {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    void run(Command c) {
        c.execute();
        undoStack.push(c);
        redoStack.clear();          // a new action invalidates the redo branch
    }
    void undo() {
        if (undoStack.isEmpty()) { System.out.println("(nothing to undo)"); return; }
        Command c = undoStack.pop();
        c.undo();
        redoStack.push(c);
    }
    void redo() {
        if (redoStack.isEmpty()) { System.out.println("(nothing to redo)"); return; }
        Command c = redoStack.pop();
        c.execute();
        undoStack.push(c);
    }
}

// MacroCommand = Composite: a command made of commands (the "Organize" batch).
class MacroCommand implements Command {
    private final String label;
    private final List<Command> commands;
    MacroCommand(String label, List<Command> commands) { this.label = label; this.commands = List.copyOf(commands); }
    public void execute() {
        System.out.println("== " + label + " ==");
        for (Command c : commands) c.execute();          // forward order
    }
    public void undo() {
        System.out.println("== undo " + label + " ==");
        // reverse order — critical: undo dependencies in the opposite sequence
        ListIterator<Command> it = commands.listIterator(commands.size());
        while (it.hasPrevious()) it.previous().undo();
    }
}

// ---------- Usage demo ----------
class HistoryDemo {
    public static void main(String[] args) {
        StorageService storage = new StorageService();
        FileHistory history = new FileHistory();

        // A batch "Organize" = rename a file AND delete a temp file, as ONE undoable unit.
        Command organize = new MacroCommand("Organize project", List.of(
            new RenameCommand(storage, "IMG_001.png", "cover.png"),
            new DeleteCommand(storage, "~tmp.lock")
        ));

        history.run(organize);     // runs both: rename + delete
        history.undo();            // ONE undo reverses BOTH, in reverse order:
                                    //   restore ~tmp.lock, then rename cover.png -> IMG_001.png
        history.redo();            // re-applies the whole batch
    }
}
```
</details>

**Pros:** Full multi-level undo/redo; redo correctly invalidated on a new action; `MacroCommand` composes commands transparently (it *is* a `Command`), enabling batch operations; history is a clean list you can also **log/persist/replay**.
**Cons:** Undo/redo memory grows with history (may need a cap or coalescing); macro undo ordering must be reverse (easy to get wrong); interleaving undoable and non-undoable commands needs care.
**Mechanism:** *two-stack history* — executing pushes onto undo and clears redo; undo pops from undo and pushes to redo (and vice versa), giving a linear timeline. `MacroCommand` applies **Composite** so a group of commands is indistinguishable from a single command, and undo reverses the list to respect inter-command dependencies (restore the temp file *before* undoing the rename).

---

### Variant 4: Functional Commands (Java 8+ lambdas & method references)

**What problem it solves:** Variant 1's one-class-per-operation gets heavy fast. `Command` is a **functional interface** (one abstract method), so a command can be a **lambda or method reference** — no concrete class per trivial action. This slashes boilerplate while keeping the same invoker/history machinery. For undoable operations you pass a pair of lambdas (do/undo).

<details>
<summary>💻 Click to expand code — functional commands (file operations)</summary>

```java
import java.util.*;

@FunctionalInterface
interface Command { void execute(); }
interface ReversibleCommand extends Command { void undo(); }

// A reversible command built from two lambdas — no bespoke class per operation.
class LambdaCommand implements ReversibleCommand {
    private final String label;
    private final Runnable doOp, undoOp;
    LambdaCommand(String label, Runnable doOp, Runnable undoOp) { this.label = label; this.doOp = doOp; this.undoOp = undoOp; }
    public void execute() { doOp.run(); }
    public void undo()    { undoOp.run(); }
    public String toString() { return label; }
}

class FunctionalDemo {
    public static void main(String[] args) {
        StorageService storage = new StorageService();
        FileToolbar toolbar = new FileToolbar();

        // Trivial commands as method references / lambdas — no UploadCommand/DeleteCommand classes:
        toolbar.bind("upload", () -> storage.upload("notes.md"));   // lambda IS a Command
        toolbar.bind("delete", () -> storage.deleteHard("old.tmp"));
        toolbar.click("upload"); // Uploaded notes.md

        // Reversible via a do/undo lambda pair — undo(rename) is just rename back:
        Deque<ReversibleCommand> history = new ArrayDeque<>();
        ReversibleCommand rename = new LambdaCommand("rename a->b",
            () -> storage.rename("a.txt", "b.txt"),   // do
            () -> storage.rename("b.txt", "a.txt"));  // undo
        rename.execute(); history.push(rename);       // Renamed a.txt -> b.txt
        history.pop().undo();                          // Renamed b.txt -> a.txt
    }
}
```
</details>

**Pros:** Eliminates boilerplate for simple operations; `() -> storage.upload("notes.md")` reads clearly and *is* a `Command`; composes with the same invoker/history; extremely testable.
**Cons:** Loses self-documenting class names (a stack trace shows a lambda, not `UploadCommand`); harder to attach rich per-command metadata (id, description) or serialize (lambdas aren't cleanly serializable); undo via lambda pairs is less structured. For anything that must be **logged, queued across a network, or persisted** (like the async upload pipeline in Variant 5), prefer real command classes.
**Mechanism:** *SAM (single-abstract-method) conversion* — the compiler adapts a lambda/method reference to the functional `Command` interface, capturing the receiver and arguments in a closure exactly as a concrete command captures them in fields.

---

### Variant 5: Production-Grade — Command Bus / Queue with Retry & Idempotency

**Why it's recommended:** In a real cloud file service, a big upload isn't done inline on the request thread — the API enqueues an `UploadFile` command and returns immediately; **background workers** pull commands off a queue and process them, **retrying** transient failures (S3 blip, network timeout) and staying **idempotent** so a redelivered command doesn't upload the same file twice. Each command carries an **id** (for idempotency/audit) and is a serializable value object. This is how uploads, thumbnail generation, and virus scans actually run behind Dropbox/Drive — and the same shape powers payments and order processing.

<details>
<summary>💻 Click to expand code — command bus with handlers, retry, idempotency (async upload pipeline)</summary>

```java
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// Commands carry identity + payload; handlers are separate (CQRS style).
interface Command { UUID id(); }
interface CommandHandler<C extends Command> { void handle(C cmd); }

// A concrete command — an immutable, serializable-friendly value object.
final class UploadFile implements Command {
    private final UUID id;
    final String path; final long sizeBytes;
    UploadFile(UUID id, String path, long sizeBytes) { this.id = id; this.path = path; this.sizeBytes = sizeBytes; }
    public UUID id() { return id; }
}

// Handler holds the business logic (thin command, fat handler). Idempotent + flaky-on-purpose.
class UploadHandler implements CommandHandler<UploadFile> {
    private final Set<UUID> processed = ConcurrentHashMap.newKeySet(); // idempotency store (a DB in prod)
    private final AtomicInteger flaky = new AtomicInteger();           // simulate a transient failure

    public void handle(UploadFile c) {
        if (!processed.add(c.id())) {                 // already uploaded -> no-op (idempotent)
            System.out.println("Duplicate upload " + c.path + " ignored");
            return;
        }
        // Fail the very first attempt to demonstrate safe retry.
        if (flaky.getAndIncrement() == 0)
            throw new RuntimeException("S3 timeout — transient");
        System.out.println("Uploaded " + c.path + " (" + c.sizeBytes + " bytes) to object store");
    }
}

// The bus: routes a command to its handler; adds retry with backoff for transient failures.
class CommandBus {
    private final Map<Class<?>, CommandHandler<Command>> handlers = new HashMap<>();
    @SuppressWarnings("unchecked")
    <C extends Command> void register(Class<C> type, CommandHandler<C> h) {
        handlers.put(type, (CommandHandler<Command>) h);
    }
    void dispatch(Command c) {
        CommandHandler<Command> h = handlers.get(c.getClass());
        if (h == null) throw new IllegalStateException("No handler for " + c.getClass());
        int attempts = 0;
        for (;;) {
            try { h.handle(c); return; }
            catch (RuntimeException transientEx) {
                if (++attempts >= 3) throw transientEx;       // give up after retries -> dead-letter
                System.out.println("Retry " + attempts + " after: " + transientEx.getMessage());
                sleep(100L * attempts);                        // linear backoff
                // retry is SAFE because handle() is idempotent by command id
            }
        }
    }
    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}

// ---------- Usage demo ----------
class UploadPipelineDemo {
    public static void main(String[] args) {
        CommandBus bus = new CommandBus();
        bus.register(UploadFile.class, new UploadHandler());

        UUID id = UUID.randomUUID();
        UploadFile cmd = new UploadFile(id, "vacation.mp4", 250_000_000L);

        bus.dispatch(cmd);   // attempt 1 throws (S3 timeout) -> retry -> "Uploaded vacation.mp4 ..."
        bus.dispatch(cmd);   // redelivered by the queue -> "Duplicate upload ... ignored" (exactly-once effect)
    }
}
```
</details>

**Pros:** Full decoupling of *what* (command, a serializable value object) from *who handles it* (handler) and *when* (sync/async/queued); **retry** is safe because handlers are **idempotent by command id** (exactly-once *effect* under at-least-once delivery); commands are naturally **loggable/auditable/replayable**; scales to async workers and message queues.
**Cons:** More moving parts (bus, handlers, registry, idempotency store); command/handler separation is more ceremony than a simple `execute()`; requires a durable idempotency store for real exactly-once; ordering across commands needs explicit handling.
**Mechanism:** *thin command (data) + fat handler (behavior) + idempotency key*. The command is a pure immutable value carrying an **id**; the bus routes by type; **retry with backoff** plus a **dedup set keyed by command id** turns unreliable at-least-once delivery into exactly-once *effect*. This is the CQRS "command" and the backbone of durable job systems — and it leads directly into the three real-world scenarios below.

---

## 🎨 Real-World Example

Below are **four** complete, production-style scenarios. Read them together: they show the *same* Command pattern powering very different systems, so you can see what stays constant (a request reified as an object, an invoker decoupled from the receiver) and what changes per domain (undo history vs. queue vs. retry-idempotency vs. offset-commit). Each is self-contained and collapsible.

- **Scenario 1 — Collaborative Text Editor** (undo/redo): reversible commands, `MacroCommand` (Composite), history stacks, undo coalescing.
- **Scenario 2 — Job Scheduler / Task Queue**: commands as `Runnable` units submitted to a worker pool, with priority, delay, and graceful shutdown.
- **Scenario 3 — Payment Processing Pipeline**: a `MacroCommand`-style saga of steps with **compensating undo** (auth → capture → ledger → receipt), rolling back on failure.
- **Scenario 4 — Kafka Consumer Command Executor**: deserialize messages into commands, dispatch to handlers, retry/dead-letter, and **commit the offset only after success** (at-least-once + idempotency).

### Scenario 1 — Collaborative Text Editor (undo/redo + macros + coalescing)

**The problem:** A text editor's edit engine needs full multi-level undo/redo — the kind of feature every FAANG interview loves because it exercises reversible commands, a `MacroCommand` (Composite), history stacks, and even undo *coalescing*. Every user edit (insert, delete, replace) is a reversible command against a shared `Document`; the `EditHistory` invoker manages undo/redo; a `MacroCommand` groups a find-and-replace-all into one undoable unit; and consecutive character insertions are coalesced so one Ctrl+Z undoes a whole typed word, not one letter.

<details>
<summary>💻 Click to expand Scenario 1 — full code (editor undo/redo with macros, coalescing, demo)</summary>

```java
import java.util.*;

// ---------- Receiver ----------
final class Document {
    private final StringBuilder text = new StringBuilder();
    void insert(int pos, String s) { text.insert(pos, s); }
    void delete(int pos, int len)  { text.delete(pos, pos + len); }
    String snapshot()              { return text.toString(); }
    int length()                   { return text.length(); }
}

// ---------- Command ----------
interface EditCommand {
    void execute();
    void undo();
    // Allow adjacent same-type commands to merge (undo coalescing).
    default boolean mergeWith(EditCommand next) { return false; }
}

// Insert text at a position; undo removes exactly what it inserted.
final class InsertText implements EditCommand {
    private final Document doc;
    private int pos;
    private StringBuilder inserted;   // mutable to support coalescing
    InsertText(Document doc, int pos, String s) { this.doc = doc; this.pos = pos; this.inserted = new StringBuilder(s); }
    public void execute() { doc.insert(pos, inserted.toString()); }
    public void undo()    { doc.delete(pos, inserted.length()); }
    @Override public boolean mergeWith(EditCommand next) {
        // Merge a subsequent single-char insert that is contiguous (typing a word).
        if (next instanceof InsertText it && it.pos == this.pos + this.inserted.length()) {
            this.inserted.append(it.inserted);   // extend this command's payload
            return true;                           // absorbed -> don't push a new history entry
        }
        return false;
    }
}

// Delete a range; undo re-inserts the removed text (captured at execute time = Memento).
final class DeleteText implements EditCommand {
    private final Document doc;
    private final int pos, len;
    private String removed;           // captured for undo
    DeleteText(Document doc, int pos, int len) { this.doc = doc; this.pos = pos; this.len = len; }
    public void execute() { removed = doc.snapshot().substring(pos, pos + len); doc.delete(pos, len); }
    public void undo()    { doc.insert(pos, removed); }
}

// MacroCommand (Composite): several edits as one undoable unit (e.g., replace-all).
final class MacroCommand implements EditCommand {
    private final List<EditCommand> cmds;
    MacroCommand(List<EditCommand> cmds) { this.cmds = List.copyOf(cmds); }
    public void execute() { cmds.forEach(EditCommand::execute); }
    public void undo() {
        for (int i = cmds.size() - 1; i >= 0; i--) cmds.get(i).undo(); // reverse order!
    }
}

// ---------- Invoker: history with undo/redo + coalescing ----------
final class EditHistory {
    private final Deque<EditCommand> undo = new ArrayDeque<>();
    private final Deque<EditCommand> redo = new ArrayDeque<>();

    void run(EditCommand c) {
        c.execute();
        // Try to merge into the previous command (coalesce rapid typing).
        EditCommand prev = undo.peek();
        if (prev != null && prev.mergeWith(c)) {
            redo.clear();
            return;                    // merged — no new history entry
        }
        undo.push(c);
        redo.clear();
    }
    void undo() { if (!undo.isEmpty()) { EditCommand c = undo.pop(); c.undo(); redo.push(c); } }
    void redo() { if (!redo.isEmpty()) { EditCommand c = redo.pop(); c.execute(); undo.push(c); } }
}

// ---------- Demo: multiple call sites ----------
public class EditorDemo {
    public static void main(String[] args) {
        Document doc = new Document();
        EditHistory history = new EditHistory();

        // Call site 1: type "Hello" one char at a time — coalesced into ONE undo unit.
        int[] pos = {0};
        for (char ch : "Hello".toCharArray()) {
            history.run(new InsertText(doc, pos[0], String.valueOf(ch)));
            pos[0]++;
        }
        System.out.println(doc.snapshot()); // Hello

        // Call site 2: replace-all "l" -> "L" as a single MacroCommand (one undo reverts both).
        List<EditCommand> repl = new ArrayList<>();
        String s = doc.snapshot();
        for (int i = s.length() - 1; i >= 0; i--) {          // right-to-left keeps indices valid
            if (s.charAt(i) == 'l') {
                repl.add(new DeleteText(doc, i, 1));
                repl.add(new InsertText(doc, i, "L"));
            }
        }
        history.run(new MacroCommand(repl));
        System.out.println(doc.snapshot()); // HeLLo

        // Call site 3: undo once -> reverts the ENTIRE replace-all macro.
        history.undo();
        System.out.println(doc.snapshot()); // Hello

        // Call site 4: undo again -> reverts the whole coalesced "Hello" typing at once.
        history.undo();
        System.out.println("'" + doc.snapshot() + "'"); // ''  (empty — one Ctrl+Z removed the word)

        // Call site 5: redo -> retypes "Hello".
        history.redo();
        System.out.println(doc.snapshot()); // Hello
    }
}
```
</details>

**Why this is a strong FAANG answer:** it shows (1) **reversible commands** with two correct undo strategies — logical inverse for insert, and **captured-state Memento** for delete (the removed text is snapshotted at execute time), (2) a **`MacroCommand`** (Composite) that makes a multi-step replace-all a single undoable unit and correctly **undoes in reverse order**, (3) **undo coalescing** via `mergeWith` so rapid typing collapses into one history entry (a genuine editor UX concern), and (4) clean **undo/redo stacks** with redo invalidation on a new action — exactly the design real editors and design tools ship.

### Scenario 2 — Job Scheduler / Task Queue

**The problem:** A backend needs to run work **asynchronously** — send an email, generate a thumbnail, rebuild a search index — off the request thread. Each unit of work is a **command** (`Runnable` is the JDK's Command interface), submitted to a queue that worker threads drain. Real schedulers add **priority** (run urgent jobs first), **delay** (run this in 5 minutes), and **graceful shutdown** (finish in-flight jobs, reject new ones). The Command pattern is what lets the scheduler treat every job uniformly — it queues, times, and runs an *object* without knowing what the job does.

**The design:** A `Job` is a `Command` with a priority and an id; a `JobScheduler` (invoker) wraps a `PriorityBlockingQueue` + a worker pool. `submit()` enqueues a command; workers pull the highest-priority ready job and call `execute()`. Because jobs are objects, the scheduler can log them, time them, and retry them without any job-specific code.

<details>
<summary>💻 Click to expand Scenario 2 — full code (priority job queue + worker pool + demo)</summary>

```java
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// ---------- Command ----------
interface Job extends Runnable {         // Runnable == the JDK's Command; run() == execute()
    String id();
    int priority();                       // higher = more urgent
}

// A concrete job: the work is packaged as an object, decoupled from the worker that runs it.
class EmailJob implements Job {
    private final String id, to;
    private final int priority;
    EmailJob(String id, String to, int priority) { this.id = id; this.to = to; this.priority = priority; }
    public String id()   { return id; }
    public int priority(){ return priority; }
    public void run()    { System.out.println("[" + Thread.currentThread().getName() + "] emailing " + to + " (job " + id + ")"); }
}

// ---------- Invoker: priority queue + worker pool ----------
class JobScheduler {
    // Highest priority first; ties broken by submission order (FIFO) via a sequence number.
    private record Entry(Job job, long seq) {}
    private final PriorityBlockingQueue<Entry> queue = new PriorityBlockingQueue<>(
        16, Comparator.<Entry>comparingInt(e -> -e.job().priority()).thenComparingLong(Entry::seq));
    private final AtomicInteger seq = new AtomicInteger();
    private final List<Thread> workers = new ArrayList<>();
    private volatile boolean running = true;

    JobScheduler(int workerCount) {
        for (int i = 0; i < workerCount; i++) {
            Thread t = new Thread(this::workerLoop, "worker-" + i);
            t.start();
            workers.add(t);
        }
    }

    void submit(Job job) {                       // producer side — thread-safe enqueue
        queue.offer(new Entry(job, seq.getAndIncrement()));
    }

    private void workerLoop() {
        while (running || !queue.isEmpty()) {
            try {
                Entry e = queue.poll(200, TimeUnit.MILLISECONDS);
                if (e == null) continue;         // idle — re-check running flag
                long start = System.nanoTime();
                try {
                    e.job().run();                // === command.execute() ===
                } catch (RuntimeException ex) {
                    System.out.println("Job " + e.job().id() + " failed: " + ex.getMessage()); // log + could re-queue
                }
                // Because the job is an object, the scheduler can time/log it generically:
                long micros = (System.nanoTime() - start) / 1_000;
                // metrics.record(e.job().id(), micros);
            } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
        }
    }

    void shutdownGracefully() throws InterruptedException {
        running = false;                         // stop accepting; workers drain remaining queue
        for (Thread t : workers) t.join();
        System.out.println("All workers stopped; queue drained.");
    }

    // ---------- Demo ----------
    public static void main(String[] args) throws InterruptedException {
        JobScheduler scheduler = new JobScheduler(2);   // 2 worker threads

        // Submit jobs with different priorities — high-priority ones run first.
        scheduler.submit(new EmailJob("J1", "alice@x.com", 1));    // low
        scheduler.submit(new EmailJob("J2", "vip@x.com",   10));   // high — jumps the queue
        scheduler.submit(new EmailJob("J3", "bob@x.com",   1));

        // A delayed job: a one-shot timer that submits the command later.
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        timer.schedule(() -> scheduler.submit(new EmailJob("J4-delayed", "carol@x.com", 5)),
                       300, TimeUnit.MILLISECONDS);

        Thread.sleep(600);
        timer.shutdown();
        scheduler.shutdownGracefully();  // finishes in-flight + queued work, then stops
    }
}
```
</details>

**Code walkthrough:** The scheduler never knows what a job *does* — it only knows `Job.run()` (i.e., `execute()`). That's the entire payoff: `EmailJob`, a hypothetical `ThumbnailJob`, and `IndexJob` all queue and run through the exact same machinery. Priority is handled by the `PriorityBlockingQueue` comparator (`-priority` so higher runs first, `seq` to keep FIFO within a priority so no job starves). Delay is just "submit the command later" via a `ScheduledExecutorService`. Graceful shutdown flips a `volatile` flag so workers stop *accepting* but keep *draining* the queue — because the pending work is a queue of objects, "finish what's queued" is trivial. This is essentially what `ThreadPoolExecutor`, Quartz, and Sidekiq-style systems do; in an interview, note that `ExecutorService.submit(Runnable)` already *is* this pattern and you'd normally use it directly rather than hand-rolling the pool.

**Why this is a strong FAANG answer:** it grounds Command in the JDK's own `Runnable`/executor model, shows the request outliving the call (queued, prioritized, delayed, run on another thread), and demonstrates the operational concerns interviewers probe — priority/fairness, graceful shutdown, per-job metrics — all enabled *because the job is an object*.

### Scenario 3 — Payment Processing Pipeline (saga with compensating undo)

**The problem:** Charging a customer is a **multi-step pipeline** where each step can fail and earlier steps must be **rolled back** (compensated): authorize the card → capture the funds → post to the ledger → send a receipt. If the ledger write fails after the capture succeeded, you must **refund the capture and void the authorization** — you can't just throw. This is a *saga*: a sequence of commands where undo means "compensating action," not literal reversal. It's Command + `MacroCommand`, but with all-or-nothing semantics and reverse-order compensation on failure.

**The design:** Each step is a `PaymentStep` command with `execute()` and `compensate()`. A `PaymentSaga` (invoker) runs steps forward, tracking which ones succeeded; on any failure it **compensates the completed steps in reverse order** — exactly the `MacroCommand.undo()` discipline, applied to distributed side effects.

<details>
<summary>💻 Click to expand Scenario 3 — full code (payment saga with compensation + demo)</summary>

```java
import java.util.*;

// ---------- Command: a step that can execute and compensate (undo its effect) ----------
interface PaymentStep {
    void execute(PaymentContext ctx);      // do the step
    void compensate(PaymentContext ctx);   // undo it if a LATER step fails
    String name();
}

// Shared context carrying data produced by steps (auth id, capture id, ...).
class PaymentContext {
    final String orderId; final long amountCents;
    String authId, captureId;
    PaymentContext(String orderId, long amountCents) { this.orderId = orderId; this.amountCents = amountCents; }
}

// ---------- Concrete steps ----------
class AuthorizeCard implements PaymentStep {
    public void execute(PaymentContext c) { c.authId = "AUTH-" + c.orderId; System.out.println("Authorized " + c.amountCents + "c -> " + c.authId); }
    public void compensate(PaymentContext c) { System.out.println("Voided authorization " + c.authId); }
    public String name() { return "AuthorizeCard"; }
}
class CaptureFunds implements PaymentStep {
    public void execute(PaymentContext c) { c.captureId = "CAP-" + c.orderId; System.out.println("Captured funds -> " + c.captureId); }
    public void compensate(PaymentContext c) { System.out.println("Refunded capture " + c.captureId); }
    public String name() { return "CaptureFunds"; }
}
class PostToLedger implements PaymentStep {
    private final boolean failHere;                    // simulate a downstream failure
    PostToLedger(boolean failHere) { this.failHere = failHere; }
    public void execute(PaymentContext c) {
        if (failHere) throw new RuntimeException("ledger unavailable");
        System.out.println("Posted " + c.amountCents + "c to ledger for " + c.orderId);
    }
    public void compensate(PaymentContext c) { System.out.println("Reversed ledger entry for " + c.orderId); }
    public String name() { return "PostToLedger"; }
}
class SendReceipt implements PaymentStep {
    public void execute(PaymentContext c) { System.out.println("Emailed receipt for " + c.orderId); }
    public void compensate(PaymentContext c) { /* nothing to undo for a sent email */ }
    public String name() { return "SendReceipt"; }
}

// ---------- Invoker: runs the saga, compensates in reverse on failure ----------
class PaymentSaga {
    private final List<PaymentStep> steps;
    PaymentSaga(List<PaymentStep> steps) { this.steps = List.copyOf(steps); }

    void run(PaymentContext ctx) {
        Deque<PaymentStep> completed = new ArrayDeque<>();   // track for rollback
        try {
            for (PaymentStep step : steps) {
                step.execute(ctx);
                completed.push(step);                        // remember it so we can compensate
            }
            System.out.println("Payment succeeded for " + ctx.orderId);
        } catch (RuntimeException ex) {
            System.out.println("Step failed (" + ex.getMessage() + ") — rolling back:");
            while (!completed.isEmpty())                     // compensate in REVERSE order
                completed.pop().compensate(ctx);
            throw new RuntimeException("Payment rolled back for " + ctx.orderId, ex);
        }
    }

    // ---------- Demo ----------
    public static void main(String[] args) {
        // Happy path: all steps succeed.
        new PaymentSaga(List.of(new AuthorizeCard(), new CaptureFunds(), new PostToLedger(false), new SendReceipt()))
            .run(new PaymentContext("ORD-1", 4999));

        System.out.println("----");

        // Failure at the ledger step: capture + auth are compensated in reverse.
        try {
            new PaymentSaga(List.of(new AuthorizeCard(), new CaptureFunds(), new PostToLedger(true), new SendReceipt()))
                .run(new PaymentContext("ORD-2", 8800));
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage()); // Payment rolled back for ORD-2
        }
        // Output shows: Refunded capture CAP-ORD-2, then Voided authorization AUTH-ORD-2.
    }
}
```
</details>

**Code walkthrough:** This is the `MacroCommand` idea taken to its logical extreme: each step's "undo" isn't a literal reversal but a **compensating action** (void the auth, refund the capture) — the correct model when effects are external and can't be un-happened. The saga pushes each successful step onto a `completed` stack; if a later step throws, it pops that stack and compensates in **reverse order**, so the capture is refunded *before* the authorization is voided — the same reverse-order rule as macro undo, and for the same reason (later steps may depend on earlier ones). Note `SendReceipt.compensate` is a no-op: not every step has a meaningful undo, and that's fine. Because each step is a self-contained command, you can reorder the pipeline, insert a fraud-check step, or unit-test each step's execute/compensate in isolation.

**Why this is a strong FAANG answer:** it demonstrates Command powering a **saga / distributed-transaction** pattern — the go-to answer for "how do you keep a multi-service payment consistent without a distributed transaction." It shows compensation-as-undo, reverse-order rollback, and per-step isolation, which are exactly the correctness properties an interviewer wants to hear for money-movement systems.

### Scenario 4 — Kafka Consumer Command Executor

**The problem:** Events arriving on a Kafka topic (e.g., `OrderPlaced`, `InventoryReserved`) must be turned into **actions**. The clean design deserializes each message into a **command object**, routes it to the right **handler**, and — critically — only **commits the Kafka offset after the command succeeds**, so a crash mid-processing causes the message to be redelivered (at-least-once). Because redelivery means a command may run twice, handlers must be **idempotent**, and permanently-failing messages go to a **dead-letter topic** instead of blocking the partition forever. This is Command applied to stream processing.

**The design:** Incoming records are deserialized into `Command` value objects (each with a stable id); a `CommandDispatcher` (invoker) routes by type to a `CommandHandler`; a retry policy handles transient failures; the consumer loop commits the offset only on success. Idempotency is keyed on the command id.

<details>
<summary>💻 Click to expand Scenario 4 — full code (Kafka-style consumer + dispatch + offset commit + DLQ)</summary>

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// ---------- Command (deserialized from a Kafka record) ----------
interface Command { String id(); }                 // id = idempotency/dedup key

record OrderPlaced(String id, String orderId, long amountCents) implements Command {}
record InventoryReserved(String id, String orderId, int qty) implements Command {}

interface CommandHandler<C extends Command> { void handle(C cmd); }

// ---------- Handlers (idempotent by command id) ----------
class OrderPlacedHandler implements CommandHandler<OrderPlaced> {
    private final Set<String> seen = ConcurrentHashMap.newKeySet();   // a DB table in prod
    public void handle(OrderPlaced c) {
        if (!seen.add(c.id())) { System.out.println("dup " + c.id() + " skipped"); return; } // idempotent
        System.out.println("Fulfilling order " + c.orderId() + " for " + c.amountCents() + "c");
    }
}
class InventoryReservedHandler implements CommandHandler<InventoryReserved> {
    private final Set<String> seen = ConcurrentHashMap.newKeySet();
    public void handle(InventoryReserved c) {
        if (!seen.add(c.id())) { System.out.println("dup " + c.id() + " skipped"); return; }
        System.out.println("Reserved " + c.qty() + " units for " + c.orderId());
    }
}

// ---------- Invoker: dispatch by type, retry, dead-letter ----------
class CommandDispatcher {
    private final Map<Class<?>, CommandHandler<Command>> handlers = new HashMap<>();
    private final List<Command> deadLetter = new ArrayList<>();

    @SuppressWarnings("unchecked")
    <C extends Command> void register(Class<C> type, CommandHandler<C> h) {
        handlers.put(type, (CommandHandler<Command>) h);
    }

    /** @return true if handled (offset may be committed); false if dead-lettered. */
    boolean dispatch(Command c) {
        CommandHandler<Command> h = handlers.get(c.getClass());
        if (h == null) { deadLetter.add(c); return true; } // unknown type -> DLQ, don't block
        for (int attempt = 1; attempt <= 3; attempt++) {
            try { h.handle(c); return true; }
            catch (RuntimeException ex) {
                System.out.println("attempt " + attempt + " failed for " + c.id() + ": " + ex.getMessage());
                if (attempt == 3) { deadLetter.add(c); return true; } // exhausted -> DLQ
            }
        }
        return false;
    }
    List<Command> deadLetter() { return deadLetter; }
}

// ---------- Consumer loop (poll -> deserialize -> dispatch -> commit offset) ----------
class KafkaConsumerExecutor {
    private final CommandDispatcher dispatcher;
    KafkaConsumerExecutor(CommandDispatcher d) { this.dispatcher = d; }

    // Simulated poll: a batch of already-deserialized commands with their offsets.
    void processBatch(List<Map.Entry<Long, Command>> records, long[] committedOffset) {
        for (var rec : records) {
            long offset = rec.getKey();
            Command cmd = rec.getValue();
            boolean ok = dispatcher.dispatch(cmd);
            if (ok) {
                committedOffset[0] = offset + 1;   // COMMIT only after success (at-least-once)
            } else {
                System.out.println("not committing offset " + offset + " — will be redelivered");
                break;                              // stop; reprocess from here next poll
            }
        }
    }

    // ---------- Demo ----------
    public static void main(String[] args) {
        CommandDispatcher dispatcher = new CommandDispatcher();
        dispatcher.register(OrderPlaced.class, new OrderPlacedHandler());
        dispatcher.register(InventoryReserved.class, new InventoryReservedHandler());
        KafkaConsumerExecutor consumer = new KafkaConsumerExecutor(dispatcher);

        long[] committed = {0};
        // A polled batch: two distinct commands, then a REDELIVERY of the first (crash-replay).
        List<Map.Entry<Long, Command>> batch = List.of(
            Map.entry(0L, new OrderPlaced("evt-1", "ORD-1", 4999)),
            Map.entry(1L, new InventoryReserved("evt-2", "ORD-1", 3)),
            Map.entry(2L, new OrderPlaced("evt-1", "ORD-1", 4999))  // same id -> idempotent skip
        );
        consumer.processBatch(batch, committed);
        System.out.println("Committed up to offset " + committed[0]); // 3
        System.out.println("Dead-letter size: " + dispatcher.deadLetter().size()); // 0
    }
}
```
</details>

**Code walkthrough:** Each Kafka record is deserialized into a **command value object** carrying a stable `id` — that id is the idempotency key, so when the third record redelivers `evt-1` (a crash-replay), `OrderPlacedHandler` sees it in `seen` and skips it, giving exactly-once *effect* despite at-least-once *delivery*. The dispatcher routes by the command's runtime type to the registered handler — adding a new event type is just a new record + handler, no consumer changes. The single most important line is `committedOffset[0] = offset + 1` running **only after** a successful dispatch: if the process crashes before that, Kafka redelivers from the uncommitted offset, which is safe precisely because handlers are idempotent. Transient failures retry up to 3 times; permanent failures (poison messages) go to a **dead-letter** list so one bad message can't block the whole partition. This is the standard shape of a robust Kafka consumer — and it's the Command pattern (reified request + handler dispatch) wearing a streaming hat.

**Why this is a strong FAANG answer:** it ties Command to **stream processing and delivery semantics** — deserialize-to-command, type-routed handlers, retry + dead-letter, and offset-commit-after-success for at-least-once + idempotent exactly-once effect. These are the exact talking points for "design a reliable event consumer," and framing them as the Command pattern shows you see the underlying design, not just the Kafka API.

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- You want to **parameterize objects with an action to perform** — a generic button/menu/scheduler configured with *any* command, without subclassing.
- You need to **queue, schedule, or execute requests asynchronously** or on another thread (thread pools, job systems, message consumers).
- You need **undo/redo**, or to record a **history** of operations for auditing/replay.
- You want to **log requests** so they can be re-executed after a crash (transaction log / write-ahead log / event replay).
- You need **macros** — compose several operations into one reusable, undoable unit.
- You're building a **CQRS** system where write requests are explicit, validated, routed command objects.
- You want to **decouple invoker from receiver** so either can change independently.

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- **The call is a simple, immediate, direct method invocation** with no need to store, defer, undo, or decouple — wrapping it in a command is pure ceremony.
- **You'll never need undo, queuing, logging, or dynamic reassignment** — the indirection buys nothing.
- **A lambda/method reference suffices** and you don't need command identity, metadata, or serialization — don't build a class hierarchy for one-liners (use Variant 4).
- **Undo is required but operations aren't reversible** and full-state snapshots are prohibitively expensive — reconsider the approach (event sourcing or coarse checkpoints may fit better).
- **Over-abstraction risk:** introducing a command bus + handlers for a CRUD app with no async/audit needs adds layers that slow everyone down.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- **Decouples** the invoker (who triggers) from the receiver (who performs) — Single Responsibility + dependency inversion.
- **Open/Closed** — add new commands without changing invokers.
- Requests become **first-class objects**: queue, delay, log, schedule, replay, and undo them.
- Enables **undo/redo**, **macros** (Composite), and **transaction logs**.
- Naturally supports **async execution** and **thread pools** (`Runnable` is a command).

**Cons**

- **Class proliferation** — potentially one class per action (mitigated by lambdas for trivial ones).
- Correct **undo is hard** — every command must reverse itself faithfully; a frequent bug source.
- Undo/redo **history consumes memory** (especially memento-based commands).
- Adds **indirection** that can obscure a simple call for readers unfamiliar with the pattern.
- Full command-bus/CQRS setups add **operational and cognitive overhead**.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

Command is often confused with Strategy (both wrap behavior in an object) and with Chain of Responsibility / Observer (all decouple sender from receiver). The distinctions are about **intent**.

| Pattern | Intent | What's encapsulated | Cardinality (sender→handler) | Key tell |
|---|---|---|---|---|
| **Command** | Encapsulate a *request* as an object to queue/log/undo | An **action to perform** (+ receiver + args) | One command → one receiver | You can *store, defer, or undo* the request |
| **Strategy** | Make an *algorithm* interchangeable | A **way of doing** one thing (a policy) | Client picks one strategy | Swappable "how"; no queuing/undo semantics |
| **Chain of Responsibility** | Let *multiple* handlers get a chance to handle a request | A **handler** that may pass along | One request → many potential handlers | Request travels a chain until handled |
| **Observer** | Notify many dependents of an event | A **subscription/callback** | One event → many observers | Broadcast on state change |
| **Memento** | Capture/restore an object's state | A **state snapshot** | N/A | Used *by* Command to implement undo |

The mnemonic: **Strategy = "how to do it" (algorithm); Command = "do this thing" (request as object).** A Strategy is usually chosen once and stays; a Command is fired, and can be stored, replayed, or reversed.

</details>

## 📊 Comparison Table of Variants

<details>
<summary>📖 Click to expand</summary>

| Axis | V0: Direct | V1: Basic | V2: Undo | V3: History+Macro | V4: Functional | V5: Command Bus |
|---|---|---|---|---|---|---|
| **Invoker↔receiver coupling** | Tight | Decoupled | Decoupled | Decoupled | Decoupled | Fully decoupled |
| **Undo/redo** | ❌ | ❌ | ✅ (single) | ✅ (multi-level) | ⚠️ (do/undo pair) | Usually N/A (replay) |
| **Macros** | ❌ | ❌ | ❌ | ✅ (Composite) | ✅ (list of lambdas) | ✅ (batch) |
| **Boilerplate** | Low | High (class/action) | High | High | Low (lambdas) | High (bus+handlers) |
| **Queue/async** | ❌ | Possible | Possible | Possible | Possible | ✅ native |
| **Logging/replay** | ❌ | Manual | Manual | ✅ (history) | Hard (lambdas) | ✅ (id + audit) |
| **Serializable** | N/A | ✅ (classes) | ✅ | ✅ | ❌ (lambdas) | ✅ (value objects) |
| **Idempotency/retry** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ (id-keyed) |
| **Best for** | Trivial fixed calls | GUI actions | Simple undo | Editors/tools | Simple actions | CQRS/jobs/queues |

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

### Pitfall 1: Capturing undo state at the wrong time (after mutation)

**What goes wrong:** A memento-style command snapshots the "previous" state *after* it has already mutated the receiver, so undo restores the *new* state — undo silently does nothing.

<details>
<summary>💻 Click to expand — the failure</summary>

```java
public void execute() {
    fan.set(target);            // BUG: mutated first...
    previous = fan.speed();     // ...now `previous` == target! undo() is a no-op.
}
```
</details>

<details>
<summary>💻 Click to expand — the fix</summary>

```java
public void execute() {
    previous = fan.speed();     // snapshot BEFORE mutating
    fan.set(target);
}
public void undo() { fan.set(previous); } // now restores the true prior state
```
</details>

### Pitfall 2: Macro undo in the wrong order

**What goes wrong:** A `MacroCommand` undoes its children in the *same* order it executed them. If commands depend on each other (e.g., "create file" then "write file"), undoing in forward order tries to write to a deleted file or corrupts state.

<details>
<summary>💻 Click to expand — the fix</summary>

```java
public void undo() {
    for (int i = cmds.size() - 1; i >= 0; i--)  // REVERSE order
        cmds.get(i).undo();
}
```
</details>

### Pitfall 3: The "fat command" — business logic living in the command instead of the receiver/handler

**What goes wrong:** Putting substantial logic directly in `execute()` makes commands untestable in isolation, un-reusable, and duplicates logic that belongs on the receiver. Commands should be **thin** — delegate to a receiver/handler.

<details>
<summary>💻 Click to expand — bad vs. good</summary>

```java
// BAD: command IS the business logic (fat command)
class TransferCmd implements Command {
    public void execute() { /* 80 lines of validation, DB calls, emails... */ }
}
// GOOD: thin command delegates to a receiver/handler that owns the logic
class TransferCmd implements Command {
    private final BankService bank; private final Transfer data;
    public void execute() { bank.transfer(data); } // logic lives in BankService
}
```
</details>

### Pitfall 4: Retrying non-idempotent commands (double execution)

**What goes wrong:** In a queued/distributed setup, a command is redelivered (at-least-once) or retried after a timeout, and because `execute()` isn't idempotent, the effect applies twice — a double charge, a duplicated order.

<details>
<summary>💻 Click to expand — the fix</summary>

```java
// Give each command a stable id; dedup on it before applying the effect.
void handle(Command c) {
    if (!processed.add(c.id())) return;   // already applied -> skip (idempotent)
    apply(c);                              // safe: runs at most once per id
}
// Commit the effect + the id atomically (transactional outbox) for real exactly-once effect.
```
</details>

### Pitfall 5: Memory blowup from unbounded undo history / memento retention

**What goes wrong:** Keeping every command with a full state snapshot forever grows memory without bound, and memento commands may retain large receiver state.

<details>
<summary>💻 Click to expand — the fix</summary>

```java
// Cap history size (drop oldest) and/or coalesce adjacent commands.
if (undoStack.size() >= MAX_HISTORY) undoStack.removeLast(); // bounded deque
// Prefer storing minimal diffs over full snapshots in mementos.
```
</details>

</details>

## 🎓 Interview Tips

**What interviewers commonly ask:**

- "Design an undo/redo system for a text editor / drawing app." (The canonical Command + Memento + Composite question.)
- "How does a thread pool / `ExecutorService` relate to Command?" (`Runnable`/`Callable` *is* a command — work as an object.)
- "Command vs. Strategy — what's the difference?" (Request-as-object vs. interchangeable algorithm.)
- "How would you implement macros?" (`MacroCommand` = Composite of commands.)
- "How do you make commands safe to retry in a distributed system?" (Idempotency by command id + atomic commit.)

**What you should proactively mention even if not asked:**

- Command **reifies a request** — that's the whole point; everything (undo, queue, log, replay) follows from the request being an object.
- Distinguish the **participants** crisply: Command, ConcreteCommand, Receiver, Invoker, Client — and note the invoker depends only on the interface.
- **Undo strategies:** logical inverse vs. Memento (capture prior state) — and *when* each is required.
- **`Runnable`/`Callable` are Command** in the JDK; `ExecutorService` is an invoker/queue. Mention this — it grounds the pattern.
- For trivial actions, prefer **lambdas/method references** (functional Command); for logged/queued/serialized commands, prefer **real classes/value objects**.
- **`MacroCommand` uses Composite**; undo runs in **reverse order**.
- In distributed/CQRS contexts, commands need **ids, idempotency, retry, and durable logging** — the in-memory pattern is only the beginning.

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Composite** — `MacroCommand` is a Composite of commands (execute all, undo in reverse).
- **Memento** — commands use mementos to capture prior state for undo.
- **Strategy** — both encapsulate behavior in an object; Command is a *request*, Strategy is an *algorithm*.
- **Prototype** — commands placed on a history/queue may be cloned to preserve their state at execution time.
- **Chain of Responsibility** — a command can be passed along a chain of handlers; both decouple sender from handler.
- **Observer** — an alternative decoupling mechanism (broadcast) vs. Command's point-to-point request.
- **Mediator** — a command bus is mediator-like, routing commands to handlers.
- **Event Sourcing / CQRS** — architectural patterns built on reified commands and events.

</details>

## 📚 Library/Framework Implementation

**1. `java.lang.Runnable` / `Callable` + `java.util.concurrent.ExecutorService` (the JDK's Command).** A `Runnable` is a parameterless command; `ExecutorService.submit()` is the invoker that queues and executes it (possibly later, on a worker thread). This is the single most-used Command implementation in Java — work packaged as an object, decoupled from the thread that runs it.

<details>
<summary>💻 Click to expand — Runnable as Command on an executor</summary>

```java
ExecutorService pool = Executors.newFixedThreadPool(4);   // invoker + queue
Runnable command = () -> System.out.println("do work on " + Thread.currentThread().getName());
pool.submit(command);                                     // command queued, executed later
Future<Integer> f = pool.submit(() -> 40 + 2);            // Callable = command returning a result
// The task object outlives the call site — the defining trait of Command.
pool.shutdown();
```
</details>

**2. Swing `Action` / `AbstractAction` (and `javax.swing.undo`).** Swing's `Action` interface *is* the Command pattern: an `Action` encapsulates the code plus metadata (name, icon, enabled state) and is shared by a menu item, toolbar button, and key binding. Swing also ships `UndoManager` + `UndoableEdit` — a ready-made command-history/undo framework.

<details>
<summary>💻 Click to expand — Swing Action + UndoManager</summary>

```java
Action paste = new AbstractAction("Paste") {
    public void actionPerformed(ActionEvent e) { editor.paste(); } // the command body
};
menuItem.setAction(paste);   // same command reused by...
toolbarButton.setAction(paste); // ...multiple invokers

UndoManager undo = new UndoManager();          // command history/invoker
undo.addEdit(new AbstractUndoableEdit() {       // an UndoableEdit == reversible command
    public void undo() { super.undo(); doc.revert(); }
    public void redo() { super.redo(); doc.reapply(); }
});
undo.undo(); // reverses the last edit
```
</details>

**3. Spring's `@Transactional` / CQRS frameworks (Axon) & job schedulers (Quartz).** Axon Framework models writes as explicit `Command` objects routed by a `CommandGateway`/`CommandBus` to `@CommandHandler` methods — textbook Command + CQRS. Quartz `Job`s are commands scheduled for later execution. Even Spring Batch steps and `@Async` methods are command-like units of deferred work.

<details>
<summary>💻 Click to expand — Axon-style command + handler</summary>

```java
// The command (a value object) — validated, routed, logged, replayable.
record CreateOrder(String orderId, String product, int qty) {}

@Component
class OrderCommandHandler {
    @CommandHandler
    public void handle(CreateOrder cmd) {           // routed by the CommandBus
        // apply an OrderCreated event / persist aggregate
    }
}
// Dispatch: commandGateway.send(new CreateOrder("o-1", "book", 2));
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1: [Conceptual] What is the Command pattern and what problem does it solve?</strong></summary>

The Command pattern is a **behavioral** GoF pattern that **encapsulates a request as an object**, letting you parameterize clients with different requests, queue or log requests, and support undoable operations. It solves the problem of an invoker being **tightly coupled** to a specific operation on a specific receiver, and the inability to do anything with a request except fire it immediately.

By wrapping the receiver, the method, and the arguments in a `Command` object with an `execute()` method, the request becomes **first-class data**: you can store it, pass it around, put it on a queue, run it on another thread, log it, replay it, and (with `undo()`) reverse it.

The key insight is **reification** — turning a transient method call into a persistent object. Once the request is an object, capabilities like undo/redo, macros, and job queues fall out naturally.
</details>

<details>
<summary><strong>Q2: [Conceptual] Name the participants of the Command pattern.</strong></summary>

Five participants:

1. **Command** — interface declaring `execute()` (and often `undo()`).
2. **Concrete Command** — implements `execute()` by calling operations on a **Receiver**; stores the receiver and parameters (and, for undo, the info needed to reverse).
3. **Receiver** — the object that knows how to perform the real work (`Light`, `Document`, `BankService`).
4. **Invoker** — holds and triggers commands (`Button`, `RemoteControl`, `ExecutorService`); calls `execute()` and may maintain history. Depends only on the `Command` interface.
5. **Client** — creates concrete commands, sets their receivers, and assigns them to invokers (the wiring).

The critical relationship: the **invoker is decoupled from the receiver** — it knows only the `Command` interface, which is what enables queuing, logging, and swapping actions at runtime.
</details>

<details>
<summary><strong>Q3: [Conceptual] Why is "encapsulating a request as an object" so powerful? List the capabilities it unlocks.</strong></summary>

Because once a request is an object rather than a transient call, you can do to it anything you can do to any object:

- **Parameterize** invokers with different requests (a generic button configured with any command).
- **Queue** and **schedule** it — put it on a list/queue and execute later or elsewhere (thread pools).
- **Log** it to disk — enabling crash recovery via replay (write-ahead / transaction logs).
- **Undo/redo** it — if it can reverse itself.
- **Compose** it into macros (Composite).
- **Transmit** it across a network (serialize the command) — the basis of CQRS/RPC-ish designs.

All of these require the request to *outlive the moment of calling*, which is exactly what reification provides. The single decision to make a request an object is what unlocks the entire list.
</details>

<details>
<summary><strong>Q4: [Conceptual] How does the Command pattern relate to Runnable and ExecutorService in Java?</strong></summary>

`java.lang.Runnable` is a **parameterless Command**: `run()` is `execute()`, and the lambda/class captures its receiver and arguments. `java.util.concurrent.ExecutorService` is the **Invoker + queue**: `submit(Runnable)` stores the command and executes it later, possibly on a different worker thread. `Callable<V>` is a command that also returns a result (via a `Future`).

<details>
<summary>💻 Click to expand</summary>

```java
ExecutorService pool = Executors.newFixedThreadPool(2);
Runnable cmd = () -> process(order);      // request as object
pool.submit(cmd);                          // queued, executed later by a worker
```
</details>

This is the most pervasive real Command usage in Java and a great answer to "give a JDK example" — it also shows why Command underpins all deferred/asynchronous execution.
</details>

<details>
<summary><strong>Q5: [Implementation] Implement a basic remote control (invoker) with light on/off commands.</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
interface Command { void execute(); }

class Light { void on() { System.out.println("ON"); } void off() { System.out.println("OFF"); } }

class LightOnCommand  implements Command { final Light l; LightOnCommand(Light l){this.l=l;}  public void execute(){ l.on(); } }
class LightOffCommand implements Command { final Light l; LightOffCommand(Light l){this.l=l;} public void execute(){ l.off(); } }

class Remote {
    private Command slot = () -> {};              // Null Object default
    void setCommand(Command c) { slot = c; }
    void press() { slot.execute(); }
}

class Demo {
    public static void main(String[] a) {
        Light light = new Light();
        Remote r = new Remote();
        r.setCommand(new LightOnCommand(light));  r.press(); // ON
        r.setCommand(new LightOffCommand(light)); r.press(); // OFF
    }
}
```
</details>

Note the **Null Object** default command so `press()` never NPEs on an unassigned slot — a detail interviewers appreciate.
</details>

<details>
<summary><strong>Q6: [Implementation] Add undo() to a command. Show both the "logical inverse" and "memento" approaches.</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
interface Command { void execute(); void undo(); }

// (a) Logical inverse: undo is the opposite operation.
class LightOnCommand implements Command {
    final Light l; LightOnCommand(Light l){this.l=l;}
    public void execute(){ l.on(); }
    public void undo()   { l.off(); }   // inverse
}

// (b) Memento: capture prior state because the op isn't self-inverting.
class SetVolumeCommand implements Command {
    final Stereo s; final int target; int previous;
    SetVolumeCommand(Stereo s, int target){ this.s=s; this.target=target; }
    public void execute(){ previous = s.volume(); s.setVolume(target); } // snapshot BEFORE
    public void undo()   { s.setVolume(previous); }                       // restore
}
```
</details>

Use **logical inverse** for cleanly reversible ops (on/off, add/remove); use **memento** (capture prior state at execute time, restore on undo) when the operation overwrites state (set value, format text). Capturing *before* mutating is essential — capturing after is the classic undo bug.
</details>

<details>
<summary><strong>Q7: [Implementation] Implement multi-level undo/redo with two stacks.</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
import java.util.*;
class History {
    private final Deque<Command> undo = new ArrayDeque<>();
    private final Deque<Command> redo = new ArrayDeque<>();
    void run(Command c) { c.execute(); undo.push(c); redo.clear(); } // new action kills redo
    void undo() { if (!undo.isEmpty()) { Command c = undo.pop(); c.undo();    redo.push(c); } }
    void redo() { if (!redo.isEmpty()) { Command c = redo.pop(); c.execute(); undo.push(c); } }
}
```
</details>

The invariants: executing a fresh command **clears the redo stack** (you've branched off the timeline); `undo` moves a command from undo→redo (reversing it); `redo` moves it back (re-executing). This linear model is what most editors ship; branching/tree undo is a more advanced variant.
</details>

<details>
<summary><strong>Q8: [Implementation] Implement a MacroCommand that composes commands and undoes correctly.</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
import java.util.*;
class MacroCommand implements Command {
    private final List<Command> cmds;
    MacroCommand(List<Command> cmds) { this.cmds = List.copyOf(cmds); }
    public void execute() { for (Command c : cmds) c.execute(); }         // forward
    public void undo() {
        for (int i = cmds.size() - 1; i >= 0; i--) cmds.get(i).undo();    // REVERSE
    }
}
```
</details>

`MacroCommand` is the **Composite** pattern applied to Command: it *is* a `Command` containing commands, so it plugs into the same invoker/history transparently. The must-get-right detail is **undo in reverse order** — later commands may depend on earlier ones, so they must be reversed last-in-first-out.
</details>

<details>
<summary><strong>Q9: [Breaking] What are the most common ways an undo implementation breaks?</strong></summary>

Several classic failures: (1) **capturing state after mutation** so undo restores the new value (a no-op); (2) **macro undo in forward order** corrupting dependent state; (3) **not clearing the redo stack** on a new action, so redo replays a stale, now-invalid branch; (4) **shared mutable receiver state** changing between execute and undo so the captured memento no longer applies at the right position (e.g., text indices shift); (5) **non-deterministic commands** (random, timestamps, external I/O) that can't be faithfully reversed or replayed.

<details>
<summary>💻 Click to expand — index-shift failure</summary>

```java
// DeleteText at pos 5 undone AFTER other edits shifted the text -> re-inserts at wrong spot.
// FIX: capture enough context (absolute anchors / operational transforms) or serialize edits,
// so undo re-computes the correct position rather than assuming a stale index.
```
</details>

The safe design: capture prior state *before* mutating, undo composites in reverse, clear redo on new actions, and keep commands deterministic (inject clocks/ids rather than reading them inside execute).
</details>

<details>
<summary><strong>Q10: [Breaking] In a queued/distributed system, how can retrying a command cause data corruption, and how do you prevent it?</strong></summary>

Queues typically guarantee **at-least-once** delivery, and clients retry after timeouts, so a command's `execute()` can run **more than once** — a non-idempotent command (charge card, create order, increment counter) then double-applies its effect.

Prevention: (1) give each command a **stable idempotency key/id**; (2) before applying, **check a dedup store** for that id and no-op if seen; (3) **commit the effect and the id atomically** (same DB transaction, or transactional outbox) so you can't record "done" without doing it, or vice versa; (4) prefer **idempotent operations** where possible (set instead of increment); (5) use **optimistic concurrency** (expected-version) for state changes.

<details>
<summary>💻 Click to expand</summary>

```java
void handle(Command c) {
    if (dedup.contains(c.id())) return;      // already applied
    tx(() -> { apply(c); dedup.add(c.id()); }); // effect + id committed atomically
}
```
</details>

This converts unreliable at-least-once delivery into exactly-once *effect* — the standard guarantee real command pipelines provide.
</details>

<details>
<summary><strong>Q11: [Breaking] Why is a "fat command" (business logic inside execute()) an anti-pattern?</strong></summary>

A command should be a **thin wrapper** that delegates to a receiver/handler. When you stuff substantial business logic directly into `execute()`, you get: (1) **poor testability** — you can't test the logic without constructing and running the command; (2) **duplication** — the same logic can't be reused outside the command; (3) **violation of SRP** — the command now has two jobs (being a request + doing the work); (4) hard-to-mock dependencies baked into the command.

<details>
<summary>💻 Click to expand — fix</summary>

```java
// Thin command delegates; the Receiver/Handler owns and is unit-tested independently.
class PlaceOrderCommand implements Command {
    private final OrderService svc; private final OrderData data;
    public void execute() { svc.place(data); }   // one line: delegate
}
```
</details>

The receiver holds reusable, testable logic; the command just names *what* to do and *with what*. This separation is also what makes CQRS "thin command, fat handler" work.
</details>

<details>
<summary><strong>Q12: [Trade-off] Command vs. Strategy — they both wrap behavior in an object. How do you distinguish them?</strong></summary>

Both encapsulate behavior behind an interface, but the **intent** differs:

- **Command** encapsulates a **request/action** — "do this thing" — bundling a receiver and arguments, designed to be **stored, queued, logged, undone, or replayed**. Its lifecycle extends beyond the call.
- **Strategy** encapsulates an **interchangeable algorithm** — "how to do one thing" (e.g., a sorting comparator, a pricing policy). The client picks one and it typically stays put; there's no notion of queuing or undo.

Tell them apart by asking: *do I want to keep this around, defer it, or reverse it?* → Command. *Do I want to swap how a single operation is performed?* → Strategy. A `Comparator` is a Strategy; a `PasteCommand` on an undo stack is a Command. Structurally similar; semantically distinct.
</details>

<details>
<summary><strong>Q13: [Trade-off] When should commands be concrete classes vs. lambdas/method references?</strong></summary>

Use **lambdas/method references** (functional Command) for **trivial, transient actions** where you don't need identity, metadata, serialization, or rich undo — they eliminate boilerplate and read cleanly (`button.setCommand(light::on)`).

Use **concrete command classes / value objects** when the command must be: **serialized** (lambdas don't serialize cleanly and are brittle across versions), **logged/audited** with an id and description, **queued across a network** or persisted, **undoable with captured state**, or **routed by type** (a command bus dispatching by `getClass()`). Classes also give meaningful stack traces (`TransferCommand` vs. an anonymous lambda).

Rule of thumb: **in-process, throwaway → lambda; anything that crosses a boundary (disk, network, time) or needs identity → class**.
</details>

<details>
<summary><strong>Q14: [Trade-off] What are the costs of adopting a full command bus / CQRS approach, and when is it worth it?</strong></summary>

Costs: **more indirection and moving parts** (bus, handler registry, dispatch), **operational complexity** (a durable idempotency store, retry/backoff, dead-letter handling), **cognitive overhead** (developers must learn command/handler separation and eventual consistency), and **potential latency** from async processing. For a simple CRUD app it's massive over-engineering.

It's worth it when you have: **high write volume** needing async/queued processing; **audit/compliance** requirements (every write is a logged command); **complex domains** where separating writes (commands) from reads (queries) clarifies the model; **integration** across services via messages; or a need for **replay/recovery** and **temporal decoupling** (accept now, process later). The judgment: adopt CQRS/command-bus when decoupling, auditability, and scalability justify the ceremony — not by default.
</details>

<details>
<summary><strong>Q15: [Advanced/Serialization] How do you serialize commands for logging/replay, and what breaks?</strong></summary>

Prefer commands that are **immutable value objects** (records) serialized to a stable format (JSON/Avro/Protobuf) with an explicit **schema version** and a **command id**. On replay, deserialize and re-dispatch to the current handler.

What breaks: (1) **lambda commands don't serialize** reliably (serializable lambdas encode brittle synthetic method names that break across recompiles/JVMs) — use classes for anything persisted; (2) **schema evolution** — adding/removing fields breaks old logs unless you version and tolerate missing fields; (3) **receiver references** — a command must serialize *identifiers* (accountId), not live object references; (4) **non-determinism** — a command that read `now()` or a random inside `execute()` won't replay identically, so **inject** clocks/ids as fields captured at creation; (5) **enum ordinals** — persist enum *names*, not ordinals.

<details>
<summary>💻 Click to expand</summary>

```java
record TransferMoney(UUID id, int schemaVersion, String fromId, String toId, long cents, Instant issuedAt) {}
// Everything needed to replay deterministically is captured as data at creation time.
```
</details>
</details>

<details>
<summary><strong>Q16: [Advanced/Concurrency] How do you build a thread-safe command queue that workers pull from?</strong></summary>

Use a **thread-safe blocking queue** as the buffer between producers (submitting commands) and consumer worker threads (executing them) — the producer/consumer form of the Command pattern.

<details>
<summary>💻 Click to expand solution</summary>

```java
import java.util.concurrent.*;

class CommandQueue {
    private final BlockingQueue<Command> queue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    CommandQueue(int workers) {
        for (int i = 0; i < workers; i++) {
            Thread t = new Thread(this::workerLoop, "cmd-worker-" + i);
            t.setDaemon(true); t.start();
        }
    }
    void submit(Command c) { queue.offer(c); }        // producer (thread-safe)
    private void workerLoop() {
        while (running) {
            try {
                Command c = queue.take();              // blocks until a command is available
                c.execute();                            // command must be thread-safe / independent
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            catch (RuntimeException ex) { /* log + dead-letter, don't kill the worker */ }
        }
    }
    void shutdown() { running = false; }
}
```
</details>

Key points: `BlockingQueue.take()` handles the wait/notify for you; each command must be **self-contained and safe to run concurrently** (no shared mutable receiver without synchronization); and a worker must **not die on one command's exception** (catch, log, dead-letter). This is essentially a hand-rolled `ExecutorService`, which you'd normally just use directly.
</details>

<details>
<summary><strong>Q17: [Advanced] How does the Command pattern underpin event sourcing and CQRS?</strong></summary>

In **CQRS**, writes are modeled as explicit **Command** objects (intent: "PlaceOrder") that a command handler validates and applies to an aggregate; the aggregate then emits **Events** (facts: "OrderPlaced"). **Event sourcing** stores the sequence of events as the source of truth and rebuilds state by replaying them.

The relationship: a **command is a request that may be rejected** (validation, business rules) and represents *intent*; an **event is an immutable fact that already happened** and cannot be rejected. Command → (handler validates) → Event(s) → (applied to state / projections). The Command pattern's reification is exactly what makes commands routable, loggable, and retryable; combined with idempotency keys, this yields durable, replayable, auditable write pipelines. Frameworks like Axon formalize `@CommandHandler`/`@EventSourcingHandler` around this split.
</details>

<details>
<summary><strong>Q18: [Advanced] How do you implement undo in a collaborative, multi-user setting where the document changes between execute and undo?</strong></summary>

Naive positional undo breaks because another user's edit shifts the indices your command captured. The production solutions: (1) **Operational Transformation (OT)** — transform a pending command against concurrent commands so its effect applies at the correct, adjusted position; undo becomes "apply the inverse operation, transformed against everything since"; (2) **CRDTs** — represent edits against stable, position-independent identifiers (not integer offsets) so operations commute and undo is well-defined regardless of concurrent edits; (3) **per-user undo** semantics — undo *your* last operation (its inverse), transformed past others' operations, rather than global LIFO undo.

The core shift: commands must reference **stable anchors** (character ids, logical positions) instead of raw offsets, and undo must be **transformed against concurrent history** rather than assuming the document is unchanged. This is why Google Docs-style editors use OT/CRDTs — plain Command+Memento assumes a single-writer timeline.
</details>

<details>
<summary><strong>Q19: [Coding Challenge] Design a bank account system where deposit/withdraw are commands with undo, plus a transaction log for replay/recovery.</strong></summary>

<details>
<summary>💻 Click to expand full solution</summary>

```java
import java.util.*;

class Account {
    private long balanceCents;
    Account(long opening) { this.balanceCents = opening; }
    void deposit(long c)  { balanceCents += c; }
    boolean withdraw(long c) { if (c > balanceCents) return false; balanceCents -= c; return true; }
    long balance() { return balanceCents; }
}

interface TxCommand {
    boolean execute();   // returns success
    void undo();
    String describe();   // for the log
}

class Deposit implements TxCommand {
    private final Account a; private final long cents;
    Deposit(Account a, long cents) { this.a = a; this.cents = cents; }
    public boolean execute() { a.deposit(cents); return true; }
    public void undo()       { a.withdraw(cents); }          // inverse
    public String describe() { return "DEPOSIT " + cents; }
}

class Withdraw implements TxCommand {
    private final Account a; private final long cents; private boolean applied;
    Withdraw(Account a, long cents) { this.a = a; this.cents = cents; }
    public boolean execute() { applied = a.withdraw(cents); return applied; } // may fail (NSF)
    public void undo()       { if (applied) a.deposit(cents); }  // only reverse if it happened
    public String describe() { return "WITHDRAW " + cents; }
}

class Bank {
    private final Deque<TxCommand> history = new ArrayDeque<>();
    private final List<String> log = new ArrayList<>();       // append-only transaction log

    boolean run(TxCommand c) {
        boolean ok = c.execute();
        if (ok) { history.push(c); log.add(c.describe()); }    // only successful ops are logged
        return ok;
    }
    void undoLast() { if (!history.isEmpty()) { TxCommand c = history.pop(); c.undo(); log.add("UNDO " + c.describe()); } }
    List<String> auditLog() { return List.copyOf(log); }

    public static void main(String[] args) {
        Account acct = new Account(100_00);          // $100.00
        Bank bank = new Bank();
        System.out.println(bank.run(new Deposit(acct, 50_00)));   // true, balance 150
        System.out.println(bank.run(new Withdraw(acct, 200_00))); // false — insufficient funds (not logged)
        System.out.println(bank.run(new Withdraw(acct, 30_00)));  // true, balance 120
        System.out.println("Balance: " + acct.balance());        // 12000
        bank.undoLast();                                          // undo the $30 withdrawal
        System.out.println("Balance: " + acct.balance());        // 15000
        System.out.println("Audit: " + bank.auditLog());
    }
}
```
</details>

Highlights: `Withdraw` tracks whether it **actually applied** (so a failed, unfunded withdraw isn't wrongly reversed); only successful commands enter history and the log; the **append-only log** is the seed of crash recovery (replay the log to rebuild state) and audit — the exact shape banks and ledgers use.
</details>

<details>
<summary><strong>Q20: [Coding Challenge] Implement a generic type-safe command bus that dispatches commands to registered handlers.</strong></summary>

<details>
<summary>💻 Click to expand full solution</summary>

```java
import java.util.*;

interface Command {}
interface Handler<C extends Command> { void handle(C command); }

class CommandBus {
    private final Map<Class<?>, Handler<?>> handlers = new HashMap<>();

    <C extends Command> void register(Class<C> type, Handler<C> handler) {
        handlers.put(type, handler);
    }

    @SuppressWarnings("unchecked")
    <C extends Command> void dispatch(C command) {
        Handler<C> h = (Handler<C>) handlers.get(command.getClass());
        if (h == null) throw new IllegalStateException("No handler for " + command.getClass().getSimpleName());
        h.handle(command);
    }

    // ---- Demo ----
    record CreateUser(String name) implements Command {}
    record DeleteUser(String id) implements Command {}

    public static void main(String[] args) {
        CommandBus bus = new CommandBus();
        bus.register(CreateUser.class, (CreateUser c) -> System.out.println("Creating user " + c.name()));
        bus.register(DeleteUser.class, (DeleteUser c) -> System.out.println("Deleting user " + c.id()));

        bus.dispatch(new CreateUser("Ada"));   // Creating user Ada
        bus.dispatch(new DeleteUser("u-42"));   // Deleting user u-42
    }
}
```
</details>

The bus routes by **command runtime type** to the registered handler — a mediator that fully decouples senders from handlers. The generic bounds (`<C extends Command>`) give compile-time safety at registration; the single unchecked cast at dispatch is the standard, contained compromise. Extend it with middleware (logging, validation, retry, transactions) wrapping `dispatch` — the natural place cross-cutting concerns live in CQRS.
</details>

### 🧠 Staff / Principal Engineer Level

<details>
<summary><strong>SP1: [Staff] You're designing a durable job-processing system on top of the Command pattern for millions of jobs/day. What are the key decisions?</strong></summary>

I'd treat each job as an **immutable command value object with a stable id**, persisted to a durable queue (Kafka/SQS/DB-backed) — the command *is* the unit of durability, retry, and audit. Key decisions: (1) **serialization & versioning** — a schema-evolvable format (Protobuf/Avro/JSON) with an explicit version so old queued commands still deserialize after deploys; (2) **idempotency** — every handler dedups on command id and commits effect+id atomically (transactional outbox) so at-least-once delivery yields exactly-once effect; (3) **retry policy** — exponential backoff with jitter, a max-attempt cap, and a **dead-letter queue** for poison messages; (4) **ordering** — decide whether commands need per-key ordering (partition by entity id) or can run out of order; (5) **visibility/observability** — per-command-type metrics (throughput, latency, failure rate), tracing, and a queryable status; (6) **backpressure & isolation** — separate queues/pools per command type so a slow handler can't starve others (bulkheads). The pattern gives me the "request as object"; the engineering is making that object durable, idempotent, ordered-where-needed, observable, and safely retryable. I'd lean on an existing broker + framework rather than hand-rolling, and keep commands thin with fat, independently testable handlers.
</details>

<details>
<summary><strong>SP2: [Staff] How do you add cross-cutting concerns (auth, validation, logging, retry, transactions) to a command bus without polluting handlers?</strong></summary>

Use a **middleware/decorator pipeline** around the bus — the same idea as HTTP middleware or gRPC interceptors. `dispatch(command)` passes through an ordered chain of decorators, each wrapping the next: a `LoggingMiddleware` logs entry/exit, a `ValidationMiddleware` rejects invalid commands early, an `AuthMiddleware` checks permissions, a `TransactionMiddleware` opens/commits a DB transaction around the handler, a `RetryMiddleware` retries transient failures, an `IdempotencyMiddleware` dedups by id. Handlers stay pure business logic, unaware of these concerns (SRP). Ordering matters and must be explicit (auth before validation before transaction before handler; retry outside transaction so each attempt is a fresh tx). This is Decorator applied to the command handler, and it's exactly how MediatR (`IPipelineBehavior`) and Axon interceptors work. The staff-level insight: cross-cutting concerns belong in **composable middleware keyed off the uniform command interface**, not sprinkled into handlers — the uniform `handle(Command)` seam is what makes this clean, and it keeps the concerns individually testable and reusable across all command types.
</details>

<details>
<summary><strong>SP3: [Principal] Undo/redo in a large collaborative product (like Figma/Google Docs) — how does Command scale to that, and where does it break?</strong></summary>

Single-user Command+Memento assumes a **linear, single-writer timeline**, which collapses under concurrent editing: another user's operation shifts the positions your captured command assumed, so naive inverse-undo corrupts the document. The scaled design keeps the *spirit* of Command (operations as first-class objects) but changes two things. First, operations reference **stable, position-independent identifiers** (object ids, CRDT element ids) instead of integer offsets, so they commute and remain meaningful under concurrent edits. Second, undo is redefined as **"apply the inverse operation, transformed against all operations that happened since"** — via **Operational Transformation** or **CRDTs** — and is typically **per-user** (undo *my* last change) rather than global LIFO. You also need **server-authoritative ordering** (a sequencer) or a convergent CRDT so all clients agree on the final state, plus **bounded, coalesced history** for memory. Where it breaks: global undo semantics, integer-offset commands, and assuming the receiver is unchanged between execute and undo. The principal-level framing: Command still gives you reified, loggable, invertible operations — the collaboration layer (OT/CRDT + ordering) is what makes those operations correct under concurrency, and the two compose rather than conflict.
</details>

<details>
<summary><strong>SP4: [Principal] Compare Command-as-request (CQRS) with event-driven architectures. When do you choose commands vs. events?</strong></summary>

A **command** expresses **intent directed at a specific handler** — "PlaceOrder" — is imperative, can be **rejected** (validation/business rules), and typically has one logical handler; it's a request that may fail. An **event** expresses a **fact that already happened** — "OrderPlaced" — is past-tense, **cannot be rejected**, and may have **many** interested subscribers (fan-out). Choose **commands** when you need a decision point with validation and a single owner of the write (the aggregate), when the caller expects accept/reject semantics, or when modeling explicit user/system intent (CQRS write side). Choose **events** for decoupled notification and fan-out, integration between services/teams, and building read-model projections or triggering downstream reactions. In practice they compose: a command is validated by a handler which, on success, emits one or more events; other services react to events (possibly issuing their *own* commands). The principal judgment: use commands where you need a **single point of decision and rejection**, events where you need **broadcast of immutable facts and loose coupling** — misusing events as commands (expecting a specific reaction) or commands as events (pretending a rejectable request is a fact) creates hidden coupling and consistency bugs.
</details>

<details>
<summary><strong>SP5: [Principal] A team's undo system occasionally corrupts state in production and it's non-deterministic. How do you diagnose and systematically prevent this class of bug?</strong></summary>

Non-deterministic undo corruption almost always means commands are **not pure/deterministic** or **capture state at the wrong time**. Diagnosis: (1) reproduce by **recording the command stream** (they're objects — log them) and replaying it deterministically offline; if replay diverges, a command is reading hidden state (clock, random, external I/O, shared mutable receiver) inside `execute()`/`undo()`. (2) Add **invariant assertions** around each transition (e.g., after undo, state must equal the pre-execute snapshot hash) to catch the exact command that violates reversibility. (3) Check for **capture-after-mutate**, **macro forward-undo**, **stale indices under concurrency**, and **shared flyweight state**. Systematic prevention: make commands **deterministic** by injecting all external inputs (clock, ids, random) as captured fields at creation; capture undo state **before** mutation; route all edits through **one** execute path that snapshots and validates; make macros undo in **reverse**; add **property-based tests** asserting `execute` then `undo` returns to the original state for random command sequences (a powerful invariant test); and for concurrent editing, move to **stable anchors + OT/CRDT** rather than offsets. The meta-point: because commands are reified objects, they're **recordable and replayable**, which turns a Heisenbug into a deterministic, testable artifact — exploit that property to both diagnose and to write the regression test that locks it down.
</details>

---

## ⚡ Quick Revision

**One-liner:** Command encapsulates a request as an object (holding the receiver + method + args behind an `execute()`), decoupling the invoker from the receiver and letting you queue, log, schedule, replay, and undo requests.

**The whole pattern in one paragraph:** The Command pattern is a behavioral GoF pattern — *"encapsulate a request as an object, thereby letting you parameterize clients with different requests, queue or log requests, and support undoable operations."* Its essence is **reification**: turn a transient method call into a first-class object so you can do to a request anything you can do to any object. Five participants: **Command** interface (`execute()`, often `undo()`), **ConcreteCommand** (binds a **Receiver** + parameters and delegates the real work to it — keep commands *thin*), **Receiver** (does the work), **Invoker** (triggers `execute()`, depends only on the interface, may keep history — e.g., a button, `ExecutorService`, a command bus), and **Client** (wires commands to invokers). Because the invoker knows only the interface, requests become storable/deferrable/loggable/reversible. **Undo** uses either a **logical inverse** (undo of `on()` is `off()`) or a **Memento** (snapshot prior state *before* mutating, restore on undo) — capturing after mutating is the classic bug. **Multi-level undo/redo** = two stacks (executing clears redo); **macros** = `MacroCommand` (Composite of commands, undone in **reverse order**). For trivial actions use **lambdas/method references** (functional Command); for anything crossing disk/network/time or needing identity, use **class/value-object commands** (lambdas don't serialize well). The JDK's `Runnable`/`Callable` + `ExecutorService` *is* Command (work as an object, queued and run later); Swing `Action` + `UndoManager` and CQRS frameworks (Axon `@CommandHandler`) are others. At scale, commands become a **command bus / durable queue**: thin immutable commands with a **stable id**, routed to handlers, made **idempotent** (dedup by id + atomic effect-and-id commit via transactional outbox) so at-least-once delivery yields exactly-once *effect*, with retry/backoff, dead-letter, versioned serialization, and middleware (Decorator) for auth/validation/logging/transactions. It underpins **CQRS/event sourcing** (command = rejectable intent; event = immutable fact). Confused with **Strategy** (interchangeable algorithm, not a stored/undoable request), **Chain of Responsibility** (many handlers), and **Observer** (broadcast). Avoid it for simple immediate calls with no need to store/defer/undo/decouple.

**Top 5 interview answers to memorize:**

1. **"What is it?"** → Wrap a request (receiver + action + args) in an object with `execute()`, so the invoker is decoupled from the receiver and the request can be queued, logged, replayed, or undone.
2. **"JDK example?"** → `Runnable`/`Callable` are commands; `ExecutorService.submit()` is the invoker that queues and runs them later — work packaged as an object. Also Swing `Action` + `UndoManager`.
3. **"How does undo work?"** → Either a logical inverse or a Memento (capture prior state *before* mutating, restore on undo); multi-level undo/redo uses two stacks and executing a new command clears redo.
4. **"Command vs. Strategy?"** → Command is a stored/deferrable/undoable *request* ("do this thing"); Strategy is an interchangeable *algorithm* ("how to do one thing") chosen by the client and left in place.
5. **"Retry safety in distributed systems?"** → Give commands a stable id, dedup on it, and commit the effect + id atomically (transactional outbox) so at-least-once delivery becomes exactly-once effect.

**Trigger words (hear these → think Command):** "undo/redo", "encapsulate a request", "queue/schedule tasks", "job/task/worker", "thread pool / Runnable", "macro / record actions", "transaction log / replay", "decouple invoker from receiver", "reassignable buttons / menu actions", "CQRS / command bus / handler", "retry / idempotent operations", "operations as objects", "parameterize with an action", "defer execution".

---

*End of Command Pattern study guide.*




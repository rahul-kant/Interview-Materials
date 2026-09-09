# Chain of Responsibility Pattern ⭐⭐⭐ (Difficulty: 3/5 — the core idea is simple, but ordering, termination, async chains, and knowing when it becomes an anti-pattern get deep)

> **Category:** Behavioral Pattern (GoF)
> **Also known as:** Chain of Command, CoR

Chain of Responsibility lets you **pass a request along a line of handlers**, each of which decides either to process it or to forward it to the next handler. The sender never knows which handler (if any) will deal with the request — it just drops it into the front of the chain. It's the pattern behind servlet filters, Spring Security's filter chain, logging levels, HTTP middleware, and every "interceptor pipeline" you've ever configured.

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 0: Monolithic if-else / switch (Anti-pattern)](#variant-0-monolithic-if-else--switch-anti-pattern)
   - [Variant 1: Classic CoR — Handler Interface + Successor](#variant-1-classic-cor--handler-interface--successor)
   - [Variant 2: Abstract Base Handler Centralizing the Pass-to-Next Logic](#variant-2-abstract-base-handler-centralizing-the-pass-to-next-logic)
   - [Variant 3: Functional / Modern Java 8+ Pipeline (List + Function composition)](#variant-3-functional--modern-java-8-pipeline-list--function-composition)
   - [Variant 4: Production Middleware / Filter Chain with Builder Assembly](#variant-4-production-middleware--filter-chain-with-builder-assembly)
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

> **GoF Definition:** *"Avoid coupling the sender of a request to its receiver by giving more than one object a chance to handle the request. Chain the receiving objects and pass the request along the chain until an object handles it."*

The Chain of Responsibility pattern exists to **decouple the object that issues a request from the objects that might handle it**. Instead of the sender picking a receiver explicitly (`if this then handlerA, else handlerB…`), it hands the request to the *head* of a chain of candidate handlers. Each handler in turn gets a chance: it either processes the request and stops, or passes it to its successor. The sender is oblivious to how long the chain is, what order handlers run in, or which one ultimately does the work.

The key insight is **indirection through a linked series of handlers plus a shared handling contract**. Because every handler implements the same interface and holds a reference to the *next* handler, you can add, remove, or reorder handlers without touching the sender or the other handlers. This turns a rigid conditional (`if/else if/else`) into a configurable, composable pipeline — the same structural move that makes middleware, filters, and interceptors so flexible. Note the GoF's phrase "*until an object handles it*": the classic intent is that **exactly one** handler processes the request, but the pattern is routinely relaxed into a *pipeline* where **every** handler contributes (logging, then auth, then compression) — both are legitimate uses of the same structure.

---

## 🎯 Problem

You have a request that could be handled by one of several objects, and you don't want the sender hard-wired to a specific handler. Worse, the set of potential handlers — and the order they should be tried in — may change at runtime or per deployment. Encoding this as a big conditional in the sender couples it to every handler, violates Open/Closed, and makes the ordering logic impossible to reuse or reconfigure.

**The pain points that lead you to Chain of Responsibility:**

- The **sender should be decoupled** from the concrete receiver: it shouldn't know *who* handles the request, only that *someone* in the chain might.
- **Multiple objects can handle a request**, and which one does isn't known until runtime (depends on the request's content, the current state, or configuration).
- You want to **add, remove, or reorder handlers dynamically** without editing the sender or a central dispatcher (Open/Closed).
- You have a growing **`if/else if/else` or `switch`** that keeps accreting new cases for each new kind of handling — a classic smell that a chain would dissolve.
- You need to attach **cross-cutting steps** (logging, authentication, rate-limiting, validation) as independent, composable stages around a core operation.

**Concrete example scenarios:**

1. **Logging with levels (the GoF/classic).** A log request tagged `INFO`, `DEBUG`, or `ERROR` is passed down a chain of loggers (console logger, file logger, email-alert logger). Each logger handles the request only if it's responsible for that level, then forwards it on — so one message can be picked up by several handlers.

2. **UI event bubbling.** A click on a button propagates up the widget hierarchy: the button gets first chance, then its panel, then the window. Whichever component knows how to handle the event consumes it; unhandled events bubble to the parent. This is CoR built into GUI toolkits and the DOM.

3. **Servlet filters / HTTP middleware.** An incoming request passes through a chain — authentication, then logging, then compression, then the actual servlet. Each filter can process, short-circuit (reject), or pass the request to the next. This is the single most common real-world CoR.

4. **Approval / authorization workflows.** A purchase request goes to a team lead (can approve up to $1k), then a manager (up to $10k), then a director (up to $100k). Each approver either approves within their limit or escalates to the next — the textbook "escalation chain."

---

## ✅ Solution

The core idea, in plain language: **give every potential handler a common interface with a `handle(request)` method and a reference to the *next* handler in line. The sender calls `handle` on the first handler. Each handler inspects the request: if it can deal with it, it does (and may stop the chain); otherwise — or additionally — it delegates to its successor. The sender knows nothing about the chain's length, order, or membership.** Adding a handler is just inserting a link; reordering is just relinking.

**Key structural elements:**

- **Handler (interface or abstract class):** declares the handling method (`handle(request)`) and, usually, a way to set the successor (`setNext(handler)`). This is the common contract that lets the sender and every handler treat all handlers uniformly.
- **Successor reference (`next`):** each concrete handler holds a link to the next handler in the chain. This link is what makes the "pass it along" behavior possible and what you manipulate to add/remove/reorder handlers.
- **`handleRequest` logic:** each concrete handler implements the decision "*is this mine?*" — if yes, process; if no (or after processing), forward to `next`. Whether it stops on first match (pure CoR) or always forwards (pipeline) is a design choice.
- **Abstract Base Handler (very common refinement):** an abstract class that stores the `next` reference and centralizes the "pass to next if present" boilerplate, so each concrete handler only writes its own "can I handle this?" logic. Without it, every handler duplicates the null-check-and-forward code.
- **Client / sender:** builds the chain (wires successors) and kicks off the request by calling the head handler — it depends only on the `Handler` interface.

**The mechanism that makes it work:** *a linked list of polymorphic handlers behind a uniform interface*. Because the sender depends only on the `Handler` type and each handler forwards through the abstract `next` link (dependency inversion), the chain's composition is entirely decoupled from both the sender and the individual handlers. You've replaced a hard-coded conditional dispatch with a **runtime-configurable data structure** (the chain), which is why you can reorder, insert, or swap handlers freely. The trade-off, baked into the intent, is that **receipt is not guaranteed** — if no handler accepts the request, it falls off the end of the chain unhandled, so a well-designed chain either guarantees a terminal handler or defines what "unhandled" means.

---

## 💻 Implementation

We'll model a **purchase-approval / expense-escalation system** (the canonical "amount decides who approves") and a **request-processing filter chain**, evolving from a tangled conditional to a production, builder-assembled middleware pipeline. Each variant fixes a specific weakness of the previous one.

### Variant 0: Monolithic if-else / switch (Anti-pattern)

**What's wrong with it:** A single method holds every handling rule in one growing `if/else if/else` ladder. The dispatcher is coupled to *every* approver and their thresholds; adding a new tier (or reordering them) means editing this method; the logic can't be reused, tested in isolation, or reconfigured at runtime. This is exactly the rigid conditional CoR is designed to dissolve.

<details>
<summary>💻 Click to expand code — the anti-pattern</summary>

```java
// DON'T DO THIS — one method knows every approver and every threshold.
class ExpenseApproverNaive {

    String approve(double amount) {
        if (amount <= 1_000) {
            return "Team Lead approved $" + amount;
        } else if (amount <= 10_000) {
            return "Manager approved $" + amount;
        } else if (amount <= 100_000) {
            return "Director approved $" + amount;
        } else if (amount <= 500_000) {
            return "VP approved $" + amount;
        } else {
            return "Rejected — exceeds all limits";
        }
        // Add a new tier (e.g., CFO)? Edit this method.
        // Reorder / change a threshold? Edit this method.
        // Reuse just the "manager" rule elsewhere? Impossible.
    }
}
```
</details>

**Pros:** Trivial to read for a tiny, fixed rule set; no indirection.
**Cons:** The dispatcher is coupled to every tier and threshold; violates Open/Closed (every new tier edits the method); rules can't be individually reused, unit-tested, or reordered at runtime; the ladder grows without bound; no way to insert cross-cutting steps between tiers.
**Mechanism (why it's fragile):** the branching is *hard-coded control flow*, not data — there's no seam to add, remove, or reorder a rule without editing the one method that owns them all.

---

### Variant 1: Classic CoR — Handler Interface + Successor

**What problem it solves:** Turn each rule into its own **handler object** that holds a reference to the **next** handler. Each approver decides "can I handle this?" — if yes it approves and stops; if not it forwards to its successor. The client wires the chain (lead → manager → director) and calls only the head. Adding a tier is now a new class + one link; reordering is relinking. The sender is fully decoupled from the concrete approvers.

<details>
<summary>💻 Click to expand code — classic handler + successor</summary>

```java
// The request (a simple value object).
record PurchaseRequest(String item, double amount) {}

// Handler interface — the uniform contract + successor wiring.
interface Approver {
    void setNext(Approver next);
    void handle(PurchaseRequest request);
}

// Concrete handler: each stores its own successor and forwards manually.
class TeamLead implements Approver {
    private Approver next;
    public void setNext(Approver next) { this.next = next; }
    public void handle(PurchaseRequest r) {
        if (r.amount() <= 1_000) {
            System.out.println("Team Lead approved " + r.item() + " ($" + r.amount() + ")");
        } else if (next != null) {
            next.handle(r);                     // forward to successor
        } else {
            System.out.println("No approver for $" + r.amount());  // fell off the end
        }
    }
}

class Manager implements Approver {
    private Approver next;
    public void setNext(Approver next) { this.next = next; }
    public void handle(PurchaseRequest r) {
        if (r.amount() <= 10_000) {
            System.out.println("Manager approved " + r.item() + " ($" + r.amount() + ")");
        } else if (next != null) {
            next.handle(r);
        } else {
            System.out.println("No approver for $" + r.amount());
        }
    }
}

class Director implements Approver {
    private Approver next;
    public void setNext(Approver next) { this.next = next; }
    public void handle(PurchaseRequest r) {
        if (r.amount() <= 100_000) {
            System.out.println("Director approved " + r.item() + " ($" + r.amount() + ")");
        } else if (next != null) {
            next.handle(r);
        } else {
            System.out.println("No approver for $" + r.amount());
        }
    }
}

// Client — builds the chain and calls only the head.
class Demo {
    public static void main(String[] args) {
        Approver lead = new TeamLead(), mgr = new Manager(), dir = new Director();
        lead.setNext(mgr);
        mgr.setNext(dir);                        // chain: lead -> mgr -> dir

        lead.handle(new PurchaseRequest("Stapler", 50));       // Team Lead approved
        lead.handle(new PurchaseRequest("Laptop", 2_500));     // Manager approved
        lead.handle(new PurchaseRequest("Server", 40_000));    // Director approved
        lead.handle(new PurchaseRequest("Datacenter", 5_000_000)); // No approver (fell off end)
    }
}
```
</details>

**Pros:** Sender fully decoupled from concrete handlers (calls only the head); each rule is its own testable class; add a tier = new class + one `setNext` (Open/Closed); order is chosen at wiring time, not hard-coded.
**Cons:** Every concrete handler **duplicates the null-check-and-forward boilerplate** (`else if (next != null) next.handle(r)`); the unhandled case is handled ad-hoc in each class; the client must remember to wire the whole chain correctly.
**Mechanism:** *a linked list of polymorphic handlers* — each handler holds an abstract `next` reference and dispatches virtually, so the sender is oblivious to the concrete chain. The request now travels a **runtime-configurable data structure** rather than a hard-coded conditional.

---

### Variant 2: Abstract Base Handler Centralizing the Pass-to-Next Logic

**What problem it solves:** The boilerplate duplication in Variant 1 (every handler repeats the "forward to next / handle the end-of-chain" logic) is factored into an **abstract base handler**. The base stores the `next` link, provides `setNext`, and offers a `passToNext()` helper (Template Method style). Each concrete handler now writes only its own `canHandle` + `doHandle` logic. This is the form you'll most often see in textbooks and real code.

<details>
<summary>💻 Click to expand code — abstract base handler</summary>

```java
record PurchaseRequest(String item, double amount) {}

// Abstract base: owns the successor link + the "pass to next" boilerplate.
abstract class Approver {
    protected Approver next;

    // Fluent wiring: returns the argument so you can chain setNext calls.
    Approver setNext(Approver next) { this.next = next; return next; }

    // Template method: decide, then act or forward.
    public final void handle(PurchaseRequest r) {
        if (canHandle(r)) {
            doHandle(r);
        } else {
            passToNext(r);
        }
    }

    // Centralized end-of-chain handling — written ONCE, not per handler.
    protected void passToNext(PurchaseRequest r) {
        if (next != null) {
            next.handle(r);
        } else {
            System.out.println("No approver could handle $" + r.amount() + " for " + r.item());
        }
    }

    // Subclass responsibilities — the only thing each concrete handler writes.
    protected abstract boolean canHandle(PurchaseRequest r);
    protected abstract void doHandle(PurchaseRequest r);
}

class TeamLead extends Approver {
    protected boolean canHandle(PurchaseRequest r) { return r.amount() <= 1_000; }
    protected void doHandle(PurchaseRequest r) { System.out.println("Team Lead approved " + r.item()); }
}
class Manager extends Approver {
    protected boolean canHandle(PurchaseRequest r) { return r.amount() <= 10_000; }
    protected void doHandle(PurchaseRequest r) { System.out.println("Manager approved " + r.item()); }
}
class Director extends Approver {
    protected boolean canHandle(PurchaseRequest r) { return r.amount() <= 100_000; }
    protected void doHandle(PurchaseRequest r) { System.out.println("Director approved " + r.item()); }
}

class Demo {
    public static void main(String[] args) {
        Approver lead = new TeamLead();
        lead.setNext(new Manager()).setNext(new Director());  // fluent chaining

        lead.handle(new PurchaseRequest("Laptop", 2_500));    // Manager approved
        lead.handle(new PurchaseRequest("Yacht", 900_000));   // No approver could handle
    }
}
```
</details>

**Pros:** Eliminates the forwarding boilerplate (DRY) — each handler is reduced to `canHandle` + `doHandle`; the end-of-chain behavior is defined in exactly one place; `setNext` returns the successor for **fluent chain assembly**; `handle` is `final`, so subclasses can't accidentally break the forwarding contract (Template Method).
**Cons:** Uses inheritance (a concrete handler is now *tied* to the base class — can't extend anything else); the split between `canHandle` and `doHandle` is slightly more indirection; still forces one-handler-per-class for trivial rules.
**Mechanism:** *Template Method over a linked list* — the base class's `final handle()` fixes the algorithm (decide → act-or-forward) while deferring only the variable parts (`canHandle`, `doHandle`) to subclasses, and centralizes the successor traversal so no handler can get the forwarding wrong.

---

### Variant 3: Functional / Modern Java 8+ Pipeline (List + Function composition)

**What problem it solves:** Rather than a hand-linked chain of classes, model the chain as an **ordered `List` of handlers** the framework iterates, or compose handlers with **function composition** (`UnaryOperator`/`Function.andThen`). This removes the `setNext` wiring entirely, makes the order explicit and reorderable as data, and lets trivial handlers be lambdas. Two flavors are shown: (a) a list-driven pipeline where each stage returns whether to continue, and (b) `Function` composition for a transform pipeline.

<details>
<summary>💻 Click to expand code — list pipeline + function composition</summary>

```java
import java.util.*;
import java.util.function.*;

// ---------- (a) List-driven chain: each handler returns true if it consumed the request ----------
record PurchaseRequest(String item, double amount) {}

@FunctionalInterface
interface Handler {
    // return true = handled (stop the chain); false = pass to next.
    boolean handle(PurchaseRequest r);
}

class Chain {
    private final List<Handler> handlers = new ArrayList<>();
    Chain add(Handler h) { handlers.add(h); return this; }   // order = list order, reorderable as data

    void process(PurchaseRequest r) {
        for (Handler h : handlers) {
            if (h.handle(r)) return;                          // first handler to consume it stops the chain
        }
        System.out.println("Unhandled: $" + r.amount());     // fell off the end — defined once
    }
}

// ---------- (b) Function composition: a transform/validation pipeline where every stage runs ----------
class FunctionalDemo {
    public static void main(String[] args) {
        // (a) trivial handlers as lambdas — no classes, order is explicit.
        Chain approval = new Chain()
            .add(r -> { if (r.amount() <= 1_000)   { System.out.println("Lead OK");     return true; } return false; })
            .add(r -> { if (r.amount() <= 10_000)  { System.out.println("Manager OK");  return true; } return false; })
            .add(r -> { if (r.amount() <= 100_000) { System.out.println("Director OK"); return true; } return false; });

        approval.process(new PurchaseRequest("Laptop", 2_500));   // Manager OK
        approval.process(new PurchaseRequest("Jet", 9_000_000));  // Unhandled

        // (b) compose text-processing stages: trim -> strip HTML -> truncate. Every stage transforms.
        UnaryOperator<String> trim      = String::trim;
        UnaryOperator<String> stripHtml = s -> s.replaceAll("<[^>]+>", "");
        UnaryOperator<String> truncate  = s -> s.length() > 10 ? s.substring(0, 10) + "…" : s;

        Function<String, String> pipeline = trim.andThen(stripHtml).andThen(truncate);
        System.out.println(pipeline.apply("  <b>Hello world!</b>  ")); // "Hello worl…"
    }
}
```
</details>

**Pros:** No `setNext` wiring — order is just list order (data you can build, sort, filter, or read from config); trivial handlers become **lambdas** (no class per rule); the "unhandled" case is defined once in the runner; function composition is perfect when **every** stage transforms the request (a pipeline, not first-match); trivially testable stages.
**Cons:** Loses per-handler identity in stack traces (a lambda vs. `Manager`); harder to insert one handler *between* two others than to append; a `List` runner is first-match/all-run by the runner's rule, not by each handler forwarding (less flexible per-stage control than explicit `next`); function composition can't easily *short-circuit* (every stage runs).
**Mechanism:** *the chain is reified as a data structure (a `List`) iterated by a runner, or as composed functions* — replacing the object-linked-list traversal with either an explicit loop (`for … if handled return`) or `Function.andThen` chaining. The forwarding logic lives in the runner, not in each handler, so handlers shrink to pure decision functions.

---

### Variant 4: Production Middleware / Filter Chain with Builder Assembly

**Why it's recommended:** Real request pipelines (HTTP servers, RPC interceptors, servlet filters) give each handler **explicit control over calling the next stage** — a handler can run code *before* AND *after* the rest of the chain (wrap it), short-circuit it, or catch exceptions from downstream. This is the `(request, next) -> ...` "onion" model. Each middleware receives a reference to *invoke the rest of the chain*, and a **builder** assembles the stages in order. This is how Express, ASP.NET Core, OkHttp, and Netty pipelines work.

<details>
<summary>💻 Click to expand code — onion-model middleware with builder</summary>

```java
import java.util.*;

// A mutable request/response context passed through the chain.
class HttpContext {
    final String path; final Map<String,String> headers;
    int status = 200; String body = "";
    boolean aborted = false;
    HttpContext(String path, Map<String,String> headers) { this.path = path; this.headers = headers; }
}

// "next" is a handle to invoke the REST of the chain — the key to the onion model.
@FunctionalInterface
interface Next { void proceed(HttpContext ctx); }

// Middleware can run code before AND after next.proceed(), or skip it (short-circuit).
@FunctionalInterface
interface Middleware {
    void handle(HttpContext ctx, Next next);
}

// Builder assembles middlewares in order, then folds them into a single Next.
class Pipeline {
    private final List<Middleware> middlewares = new ArrayList<>();
    Pipeline use(Middleware m) { middlewares.add(m); return this; }   // fluent assembly

    void run(HttpContext ctx) {
        // Fold right-to-left so the FIRST added middleware is the OUTERMOST layer.
        Next chain = c -> {};                          // terminal no-op (end of chain)
        for (int i = middlewares.size() - 1; i >= 0; i--) {
            Middleware m = middlewares.get(i);
            Next downstream = chain;                     // capture the rest of the chain
            chain = c -> m.handle(c, downstream);        // wrap it
        }
        chain.proceed(ctx);                              // invoke the outermost layer
    }
}

class MiddlewareDemo {
    public static void main(String[] args) {
        Pipeline app = new Pipeline()
            // 1. Logging — runs before AND after everything downstream (wraps the chain).
            .use((ctx, next) -> {
                long start = System.nanoTime();
                System.out.println("--> " + ctx.path);
                next.proceed(ctx);                        // call the rest of the chain
                System.out.println("<-- " + ctx.status + " (" + (System.nanoTime()-start)/1000 + "us)");
            })
            // 2. Auth — SHORT-CIRCUITS: if unauthenticated, never calls next.
            .use((ctx, next) -> {
                if (!"secret".equals(ctx.headers.get("Authorization"))) {
                    ctx.status = 401; ctx.body = "Unauthorized"; ctx.aborted = true;
                    return;                               // do NOT proceed — chain stops here
                }
                next.proceed(ctx);
            })
            // 3. The actual handler (terminal business logic).
            .use((ctx, next) -> {
                ctx.body = "Hello from " + ctx.path;
                next.proceed(ctx);                        // reaches the terminal no-op
            });

        System.out.println("--- authorized ---");
        app.run(new HttpContext("/home", Map.of("Authorization", "secret")));
        // --> /home ; <-- 200 ... ; body = "Hello from /home"

        System.out.println("--- unauthorized ---");
        app.run(new HttpContext("/home", Map.of()));
        // --> /home ; <-- 401 ...  (handler #3 never runs — auth short-circuited)
    }
}
```
</details>

**Pros:** Each stage has **full control** — run code before/after downstream, short-circuit (auth rejecting), wrap in try/catch/finally (error handling, timing), or modify request then response; the **builder** makes order explicit and configurable; cross-cutting concerns become independent, reusable, composable layers; matches the mental model of every real web framework.
**Cons:** The right-to-left fold that builds the onion is subtle (easy to get order backwards); each middleware *must* call `next.proceed()` unless it deliberately short-circuits (forgetting to call it silently drops the request); deep chains add call-stack depth and make debugging stack traces longer; the mutable shared context invites accidental coupling between stages.
**Mechanism:** *closure-based continuation passing* — the builder folds the list of middlewares into a single nested `Next`, where each layer closes over "the rest of the chain" (`downstream`). Calling `next.proceed()` is literally invoking the continuation, so a handler wraps downstream execution (the "onion"): code before `proceed` is the request phase, code after is the response phase, and *not* calling `proceed` short-circuits. This is Chain of Responsibility fused with Decorator.

---

## 🎨 Real-World Example

A believable production scenario: a **fraud / risk-check pipeline for a payments API**. An incoming payment request passes through an ordered chain of independent risk checks — schema validation, velocity limits, blocklist screening, geo/IP anomaly detection, and an ML risk-score gate. Each check either **passes the request downstream**, **short-circuits with a rejection** (blocklisted card, too many attempts), or **annotates the context** (attaching a risk score) and continues. The chain is assembled by a builder from configuration, so the risk team can insert, remove, or reorder checks per market without redeploying the caller. This is a favorite FAANG design question because it exercises ordering, short-circuiting, shared-context mutation, and observability all at once.

<details>
<summary>💻 Click to expand full real-world example (payment fraud-check chain with builder + demo)</summary>

```java
import java.util.*;

// ---------- The request/response context flowing through the chain ----------
final class PaymentContext {
    final String cardId, ipCountry, cardCountry;
    final long amountCents;
    final int recentAttempts;                 // velocity signal (last 60s)

    // Mutable outcome fields the chain fills in.
    boolean approved = true;
    String rejectionReason = null;
    int riskScore = 0;                        // accumulated by scoring checks
    final List<String> trace = new ArrayList<>(); // observability: which checks ran

    PaymentContext(String cardId, long amountCents, int recentAttempts,
                   String ipCountry, String cardCountry) {
        this.cardId = cardId; this.amountCents = amountCents;
        this.recentAttempts = recentAttempts;
        this.ipCountry = ipCountry; this.cardCountry = cardCountry;
    }
    void reject(String reason) { approved = false; rejectionReason = reason; }
}

// ---------- Handler: base class centralizes forwarding + short-circuit on rejection ----------
abstract class RiskCheck {
    private RiskCheck next;
    RiskCheck setNext(RiskCheck next) { this.next = next; return next; }

    // Template method: record trace, run the check, then forward UNLESS already rejected.
    public final void check(PaymentContext ctx) {
        ctx.trace.add(name());
        doCheck(ctx);
        if (ctx.approved && next != null) {   // short-circuit: stop forwarding once rejected
            next.check(ctx);
        }
    }
    protected abstract String name();
    protected abstract void doCheck(PaymentContext ctx);
}

// ---------- Concrete checks — each an independent, testable stage ----------
class SchemaCheck extends RiskCheck {
    protected String name() { return "schema"; }
    protected void doCheck(PaymentContext ctx) {
        if (ctx.amountCents <= 0) ctx.reject("Invalid amount");
    }
}

class VelocityCheck extends RiskCheck {
    private final int maxAttempts;
    VelocityCheck(int maxAttempts) { this.maxAttempts = maxAttempts; }
    protected String name() { return "velocity"; }
    protected void doCheck(PaymentContext ctx) {
        if (ctx.recentAttempts > maxAttempts) ctx.reject("Velocity limit exceeded");
    }
}

class BlocklistCheck extends RiskCheck {
    private final Set<String> blocked;
    BlocklistCheck(Set<String> blocked) { this.blocked = blocked; }
    protected String name() { return "blocklist"; }
    protected void doCheck(PaymentContext ctx) {
        if (blocked.contains(ctx.cardId)) ctx.reject("Card blocklisted");
    }
}

// A "scoring" check that ANNOTATES rather than rejects — it contributes, then continues.
class GeoAnomalyCheck extends RiskCheck {
    protected String name() { return "geo"; }
    protected void doCheck(PaymentContext ctx) {
        if (!ctx.ipCountry.equals(ctx.cardCountry)) ctx.riskScore += 40; // suspicious, not fatal
    }
}

class RiskScoreGate extends RiskCheck {
    private final int threshold;
    RiskScoreGate(int threshold) { this.threshold = threshold; }
    protected String name() { return "risk-gate"; }
    protected void doCheck(PaymentContext ctx) {
        if (ctx.riskScore >= threshold) ctx.reject("Risk score too high (" + ctx.riskScore + ")");
    }
}

// ---------- Builder: assemble the chain from an ordered list (config-driven) ----------
class RiskPipeline {
    private RiskCheck head, tail;
    RiskPipeline add(RiskCheck check) {
        if (head == null) head = tail = check;
        else tail = tail.setNext(check);       // append and advance the tail
        return this;
    }
    PaymentContext evaluate(PaymentContext ctx) {
        if (head != null) head.check(ctx);
        return ctx;
    }
}

// ---------- Demo: multiple call sites showing pass, short-circuit, and scoring ----------
public class FraudCheckDemo {
    static RiskPipeline buildPipeline() {
        // Order matters: cheap/decisive checks first, expensive scoring later.
        return new RiskPipeline()
            .add(new SchemaCheck())
            .add(new VelocityCheck(3))
            .add(new BlocklistCheck(Set.of("card-BAD")))
            .add(new GeoAnomalyCheck())        // annotates riskScore
            .add(new RiskScoreGate(50));       // decides based on accumulated score
    }

    public static void main(String[] args) {
        RiskPipeline pipeline = buildPipeline();

        // Call site 1: clean payment, same country -> approved.
        PaymentContext ok = pipeline.evaluate(
            new PaymentContext("card-1", 5_000, 1, "US", "US"));
        System.out.println(ok.approved + " " + ok.trace);
        // true [schema, velocity, blocklist, geo, risk-gate]

        // Call site 2: blocklisted card -> short-circuits AFTER blocklist (geo/gate never run).
        PaymentContext bad = pipeline.evaluate(
            new PaymentContext("card-BAD", 5_000, 1, "US", "US"));
        System.out.println(bad.approved + " " + bad.rejectionReason + " " + bad.trace);
        // false Card blocklisted [schema, velocity, blocklist]

        // Call site 3: geo mismatch pushes risk score to 40 — still below 50 -> approved but flagged.
        PaymentContext flagged = pipeline.evaluate(
            new PaymentContext("card-2", 5_000, 1, "NG", "US"));
        System.out.println(flagged.approved + " score=" + flagged.riskScore);
        // true score=40

        // Call site 4: velocity abuse -> short-circuits early at velocity check.
        PaymentContext abuse = pipeline.evaluate(
            new PaymentContext("card-3", 5_000, 9, "US", "US"));
        System.out.println(abuse.approved + " " + abuse.rejectionReason + " " + abuse.trace);
        // false Velocity limit exceeded [schema, velocity]
    }
}
```
</details>

**Why this is a strong FAANG answer:** it shows (1) an **abstract base handler** that centralizes forwarding *and* the short-circuit-on-rejection rule, so each check is reduced to its own `doCheck`; (2) two distinct handler behaviors in one chain — **rejecting checks** that short-circuit and **scoring checks** that annotate the shared context and continue (the "pipeline vs. first-match" hybrid real systems use); (3) a **builder** that assembles the chain from an ordered, config-driven list so the risk team reorders/inserts checks without touching the caller; (4) **observability baked in** via the `trace` list recording exactly which checks ran — which the demo uses to prove short-circuiting works; and (5) deliberate **ordering** (cheap decisive checks before expensive scoring) — a real performance concern interviewers probe.

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- **More than one object may handle a request**, and the handler isn't known until runtime (depends on request content, current state, or configuration).
- You want to **decouple the sender from the receiver** — the issuer should not know which concrete handler will process the request.
- You want to **issue a request to one of several handlers without specifying the receiver explicitly**, letting the chain figure it out.
- The set of handlers, or their **order, should be configurable at runtime** (add/remove/reorder without editing the sender).
- You're building a **pipeline of cross-cutting stages** (logging, auth, validation, rate-limiting, compression) around a core operation — middleware, filters, interceptors.
- You have a **growing `if/else if/else` or `switch`** dispatching on request type/state, and each branch is really an independent responsibility.
- You want **escalation semantics** — try a handler, and if it can't cope, escalate to a more capable one (approval workflows, support tiers).

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- **Exactly one handler always handles the request and it's known statically** — a direct call or a simple map lookup (`Map<Type, Handler>`) is clearer and O(1) versus walking a chain.
- **The request must be guaranteed to be handled** and you don't add a terminal/default handler — CoR's "may fall off the end unhandled" is a liability here.
- **Order doesn't matter and every handler always runs** independently — that's just iterating a collection; the ceremony of successors buys nothing.
- **The chain would be long and latency-critical** — walking many handlers per request adds up; a dispatch table or precomputed routing may be required.
- **Handlers have complex interdependencies** (handler C only makes sense if B ran) — a chain hides these couplings; an explicit workflow/state machine is clearer.
- **You need to know deterministically which handler will act** for correctness/debugging — the runtime indirection makes tracing harder; sometimes explicit dispatch is better.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- **Decouples sender from receiver** — the sender depends only on the head/interface, not on any concrete handler (Single Responsibility + dependency inversion).
- **Open/Closed** — add a new handler by inserting a link; no change to the sender or existing handlers.
- **Runtime-configurable order and membership** — build, reorder, or filter the chain as data (from config).
- Each handler has a **single responsibility** and is independently testable.
- Naturally models **pipelines and cross-cutting concerns** (middleware/filters/interceptors) as composable stages.

**Cons**

- **Receipt is not guaranteed** — a request can fall off the end unhandled unless you add a terminal/default handler.
- **Harder to debug/trace** — which handler acted (or why none did) is a runtime property, not visible in the code.
- **Performance** — walking a long chain per request adds overhead versus direct dispatch or a lookup table.
- **Order sensitivity** — correctness can depend on handler order; a subtle misordering is an easy bug.
- **Potential for broken or circular links** if the chain is wired manually and incorrectly.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

Chain of Responsibility is frequently confused with Decorator (both wrap/link objects), Command (both decouple sender from handling), Observer (both can involve multiple recipients), and Composite (both are tree/link structures). The distinctions are about **intent and control flow**.

| Pattern | Intent | Structure | Who handles / control flow | Key tell |
|---|---|---|---|---|
| **Chain of Responsibility** | Give several objects a chance to handle a request | Linked list of handlers, each may forward | Request travels until a handler **consumes** it (or all run); any handler may **stop** it | "Pass it along until handled"; a handler can *decline* |
| **Decorator** | Add responsibilities to an object transparently | Linked wrappers, each delegates to the wrapped object | **Every** wrapper adds behavior and **always** delegates inward | All layers run; none *declines* — it's augmentation, not routing |
| **Command** | Encapsulate a request as an object (queue/log/undo) | A request object + invoker + receiver | One command → one receiver; invoker triggers it | You can *store, defer, or undo* the request |
| **Observer** | Notify many dependents of an event | Subject with a list of observers | **All** observers are notified (broadcast, unordered) | Fan-out on state change; observers don't "consume" or stop it |
| **Composite** | Treat part-whole hierarchies uniformly | Tree of components | Operations recurse through the tree | Nesting/containment, not a decision-to-forward |
| **Mediator** | Centralize complex interactions | Hub with spokes | A mediator coordinates all colleagues | Central hub, not a linear pass-along |

The sharpest confusion is **CoR vs. Decorator** — structurally nearly identical (a linked list of objects sharing an interface, each holding the next). The difference is **intent and whether a link can stop**: a Decorator *always* delegates to the wrapped object and *adds* behavior (no link declines); a CoR handler *decides* whether to handle and *may stop the chain* (a link can consume the request and not forward). Middleware blurs these because it does both — wraps (Decorator) *and* can short-circuit (CoR).

The mnemonic: **CoR = "one of you handle this" (routing/decision, may stop); Decorator = "all of you enhance this" (augmentation, always continues); Observer = "all of you be notified" (broadcast).**

</details>

## 📊 Comparison Table of Variants

<details>
<summary>📖 Click to expand</summary>

| Axis | V0: if-else | V1: Classic CoR | V2: Abstract Base | V3: Functional/List | V4: Middleware+Builder |
|---|---|---|---|---|---|
| **Sender↔handler coupling** | Tight | Decoupled | Decoupled | Decoupled | Fully decoupled |
| **Forwarding boilerplate** | N/A | High (per handler) | Centralized (base) | None (runner loops) | Folded by builder |
| **Ordering control** | Hard-coded | Wiring-time (`setNext`) | Fluent `setNext` | List order (data) | Builder order (data) |
| **Dynamic reconfiguration** | ❌ (edit code) | Manual relink | Manual relink | ✅ (rebuild list) | ✅ (rebuild pipeline) |
| **Per-stage before/after control** | ❌ | Forward only | Forward only | ❌ (runner decides) | ✅ (onion model) |
| **Short-circuit ability** | ❌ | ✅ (don't forward) | ✅ (base rule) | ✅ (runner/return) | ✅ (skip `next`) |
| **Trivial handler as lambda** | N/A | ❌ (class) | ❌ (class) | ✅ | ✅ |
| **Readability for small sets** | High | Medium | Medium | High | Medium |
| **Performance (per request)** | Fast (branch) | Chain walk | Chain walk | List iterate | Nested calls (deepest) |
| **Best for** | Trivial fixed rules | Textbook escalation | Most real chains | Config-driven pipelines | Web/RPC middleware |

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

### Pitfall 1: The request falls off the end of the chain unhandled

**What goes wrong:** No handler accepts the request and there's no terminal/default handler, so the request silently vanishes — nothing happens, no error, no log. Callers assume "someone handled it," but nobody did.

<details>
<summary>💻 Click to expand — the fix (terminal / default handler)</summary>

```java
// Add a guaranteed-to-run terminal handler at the end of the chain (Null Object style).
class DefaultHandler extends Approver {
    protected boolean canHandle(PurchaseRequest r) { return true; }   // always accepts
    protected void doHandle(PurchaseRequest r) {
        throw new IllegalStateException("No approver for $" + r.amount()); // or log / route to dead-letter
    }
}
// lead.setNext(mgr).setNext(dir).setNext(new DefaultHandler());  // chain can never "fall off"
```
</details>

### Pitfall 2: Broken or circular links in the chain

**What goes wrong:** Manual `setNext` wiring is error-prone — forgetting a link truncates the chain (later handlers never run), and accidentally linking a handler back to an earlier one creates an **infinite loop** (`A.next = B; B.next = A`) that blows the stack.

<details>
<summary>💻 Click to expand — the fix (assemble via builder, detect cycles)</summary>

```java
// Prefer a builder/list that appends in order — you can't accidentally create a cycle.
class ChainBuilder {
    private final List<Approver> stages = new ArrayList<>();
    ChainBuilder add(Approver a) {
        if (stages.contains(a)) throw new IllegalArgumentException("Duplicate handler -> cycle risk");
        stages.add(a); return this;
    }
    Approver build() {
        for (int i = 0; i < stages.size() - 1; i++) stages.get(i).setNext(stages.get(i + 1));
        return stages.get(0);
    }
}
```
</details>

### Pitfall 3: Order sensitivity — correctness depends on handler order

**What goes wrong:** The chain produces wrong results if handlers are ordered incorrectly — e.g., an auth check placed *after* an expensive DB-writing handler, or a decompression step after the parser. Because order is configuration, a reorder can silently break behavior.

<details>
<summary>💻 Click to expand — the fix (make order explicit + tested)</summary>

```java
// Encode required ordering as an explicit, reviewed list and add an ordering invariant test.
List<Middleware> ORDER = List.of(
    security,      // MUST run before anything that touches data
    validation,    // reject malformed input before business logic
    rateLimit,
    handler);      // core work last
// Test: assert that a request rejected by security never reaches handler (trace/spy).
```
</details>

### Pitfall 4: Long-chain performance — walking every handler per request

**What goes wrong:** A chain with dozens of handlers is walked on *every* request; if each does non-trivial work (regex, I/O, deserialization), latency and CPU add up. CoR turns O(1) dispatch into O(n) traversal.

<details>
<summary>💻 Click to expand — the fix (index / short-circuit / dispatch table)</summary>

```java
// Option A: put the cheapest, most-selective handlers FIRST so most requests short-circuit early.
// Option B: if handlers key off a discrete type, replace the chain with a Map dispatch:
Map<RequestType, Handler> table = ...;
table.getOrDefault(req.type(), defaultHandler).handle(req);   // O(1), no walk
// Use a chain only where multiple/ordered/overlapping handlers genuinely apply.
```
</details>

### Pitfall 5: Handlers mutating shared request state unsafely

**What goes wrong:** When the request/context is mutable and shared across handlers (and possibly threads), one handler's mutation leaks into others in surprising ways, or two threads processing the *same* context race. Debugging becomes "who changed this field?"

<details>
<summary>💻 Click to expand — the fix (immutability / per-request context / clear ownership)</summary>

```java
// Prefer immutable requests + explicit results, or a fresh context object PER request (never shared across threads).
record Result(boolean handled, String outcome) {}
// If mutation is necessary, document which fields each stage owns, and never reuse a context across requests/threads.
// For concurrency: one context instance per in-flight request; handlers must be stateless or thread-safe.
```
</details>

</details>

## 🎓 Interview Tips

**What interviewers commonly ask:**

- "Design a logging framework with levels (INFO/DEBUG/ERROR)." (The classic CoR: a chain of loggers, each handling its level.)
- "How do servlet filters / HTTP middleware work?" (CoR: request passes through an ordered chain, each may process or short-circuit.)
- "Design an expense-approval / escalation workflow." (Amount decides who approves; escalate up the chain.)
- "CoR vs. Decorator — they look identical. What's the difference?" (Decorator always delegates + augments; CoR decides and may stop.)
- "What happens if no handler handles the request?" (It falls off the end — you need a terminal/default handler or a defined 'unhandled' contract.)

**What you should proactively mention even if not asked:**

- CoR **decouples sender from receiver** by passing the request along a chain until handled — the sender knows only the head.
- Distinguish the two flavors: **pure CoR** (first handler that can, handles it, then stops) vs. **pipeline** (every handler runs, e.g., filters/middleware).
- The **abstract base handler** (Template Method) centralizes the successor/forwarding logic so concrete handlers only write "can I handle this?"
- **Receipt is not guaranteed** — always design the end of the chain (terminal handler / default / error).
- Order matters and is **configuration** — mention builder-based assembly and that cheap/selective handlers should go first for performance.
- Real-world anchors: **servlet `FilterChain`**, **Spring Security filter chain**, **`java.util.logging` Handlers**, **OkHttp/Netty interceptors** — CoR is everywhere in frameworks.
- The **middleware "onion"** (handler gets a `next` to invoke) is CoR fused with Decorator — code before/after `next`, or short-circuit.
- At scale, watch **latency of long chains**, **debuggability**, and whether a **dispatch table** would be simpler than a chain.

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Decorator** — structurally almost identical (linked wrappers sharing an interface). Decorator *always* delegates and *adds* behavior; CoR *decides* and *may stop*. Middleware combines both.
- **Composite** — a chain is a degenerate (linear) tree; CoR is often used *with* Composite so a component forwards a request up to its parent (event bubbling in GUIs).
- **Command** — a command object can be passed *along* a chain of handlers; both decouple sender from the eventual handler.
- **Mediator** — an alternative to CoR for coordinating objects: a mediator centralizes routing in one hub, whereas CoR distributes it along a chain.
- **Observer** — another multi-recipient pattern, but broadcast (all observers notified) rather than pass-until-handled.
- **Template Method** — the abstract base handler uses Template Method to fix the "decide → act-or-forward" algorithm while deferring the decision to subclasses.
- **Strategy** — each handler's decision logic can be a Strategy; and a chain can be seen as a way to select among strategies dynamically.
- **Interceptor / Pipes and Filters** — architectural patterns that are essentially CoR applied to request processing at scale.

</details>

## 📚 Library/Framework Implementation

**1. `javax.servlet.Filter` + `FilterChain` (the canonical Java CoR).** A servlet container builds a chain of `Filter`s; each filter's `doFilter(request, response, chain)` can inspect/modify the request, then calls `chain.doFilter(...)` to invoke the next filter (or the target servlet), and can run code *after* that call returns — the onion model. Not calling `chain.doFilter` short-circuits the request (e.g., an auth filter rejecting with 401). This is CoR + Decorator in the standard servlet API.

<details>
<summary>💻 Click to expand — servlet Filter as a CoR handler</summary>

```java
public class AuthFilter implements Filter {
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) req;
        if (http.getHeader("Authorization") == null) {
            ((HttpServletResponse) res).sendError(401);   // SHORT-CIRCUIT: don't call chain
            return;
        }
        // pre-processing here...
        chain.doFilter(req, res);                          // pass to next filter / servlet
        // post-processing here (after downstream returns)...
    }
}
// web.xml / @WebFilter ordering defines the chain order.
```
</details>

**2. Spring Security `FilterChainProxy` / `SecurityFilterChain`.** Spring Security is a textbook CoR: an ordered list of filters (`UsernamePasswordAuthenticationFilter`, `BasicAuthenticationFilter`, `ExceptionTranslationFilter`, `FilterSecurityInterceptor`, …) each get a chance at the request. Any filter can authenticate, reject, or pass along. You configure the chain declaratively; Spring assembles and orders it. The `DelegatingFilterProxy` bridges the servlet container's chain into Spring's.

<details>
<summary>💻 Click to expand — Spring Security filter chain config</summary>

```java
@Bean
SecurityFilterChain chain(HttpSecurity http) throws Exception {
    return http
        .authorizeHttpRequests(a -> a.requestMatchers("/public/**").permitAll()
                                     .anyRequest().authenticated())
        .httpBasic(Customizer.withDefaults())   // adds BasicAuthenticationFilter to the chain
        .addFilterBefore(new RateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
        .build();                                // Spring assembles an ORDERED chain of filters
}
```
</details>

**3. `java.util.logging` — `Logger` + `Handler` (and Log4j/Logback appenders).** A `Logger` passes a `LogRecord` to each of its `Handler`s, and — crucially — forwards up to its **parent logger** (via the namespace hierarchy, e.g., `com.acme.svc` → `com.acme` → root) unless `setUseParentHandlers(false)` stops the chain. Level filtering means each handler/logger decides whether to process the record. This is CoR along the logger hierarchy — the GoF's own motivating example.

<details>
<summary>💻 Click to expand — j.u.l. logger/handler chain</summary>

```java
Logger logger = Logger.getLogger("com.acme.svc");
logger.addHandler(new ConsoleHandler());        // one handler in the chain
logger.setLevel(Level.INFO);                     // records below INFO are dropped here
// A LogRecord propagates to this logger's handlers, THEN to parent ("com.acme"), THEN root,
// each deciding by level whether to publish — until useParentHandlers is false.
logger.setUseParentHandlers(false);              // stop the chain from bubbling to parents
```
</details>

**4. OkHttp `Interceptor` / Netty `ChannelPipeline` (bonus).** OkHttp's `Interceptor.intercept(Chain chain)` receives a `Chain`, can modify the request, calls `chain.proceed(request)` to invoke the next interceptor, and can modify the response — identical to servlet filters. Netty's `ChannelPipeline` is an ordered list of `ChannelHandler`s that inbound/outbound events traverse; each handler processes and forwards via `ctx.fireChannelRead(...)`. Both are production CoR at massive scale.

<details>
<summary>💻 Click to expand — OkHttp interceptor</summary>

```java
class LoggingInterceptor implements Interceptor {
    @Override public Response intercept(Chain chain) throws IOException {
        Request req = chain.request();
        long t0 = System.nanoTime();
        Response res = chain.proceed(req);        // invoke the rest of the chain
        System.out.printf("%s in %.1fms%n", req.url(), (System.nanoTime()-t0)/1e6);
        return res;                                // can wrap/modify the response too
    }
}
// new OkHttpClient.Builder().addInterceptor(new LoggingInterceptor()).build();
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1: [Conceptual] What is the Chain of Responsibility pattern and what problem does it solve?</strong></summary>

Chain of Responsibility is a **behavioral** GoF pattern that **avoids coupling the sender of a request to its receiver by giving more than one object a chance to handle it**. The candidate handlers are linked into a chain; the request is passed along until a handler processes it (or it falls off the end).

It solves the problem of a sender being **tightly coupled** to a specific receiver, and of a rigid `if/else if/else` dispatch that must be edited every time a new kind of handling is added. Instead, each handler is an independent object that decides "*is this mine?*" — if yes it handles (and may stop); if no it forwards to its successor.

The key insight is turning **hard-coded conditional dispatch into a runtime-configurable linked list of handlers** behind a uniform interface. Because the sender depends only on the head/interface, you can add, remove, or reorder handlers without touching it — which is exactly why filters, middleware, and logging frameworks are built this way.
</details>

<details>
<summary><strong>Q2: [Conceptual] Name the participants of the Chain of Responsibility pattern.</strong></summary>

Three core participants:

1. **Handler** — the interface (or abstract class) declaring the request-handling method (`handle(request)`) and usually a `setNext(handler)` for wiring the successor. Defines the uniform contract.
2. **Concrete Handler** — implements `handle`: decides whether it can process the request; if so it does (and may stop the chain), otherwise it forwards to its successor. Each concrete handler is a single responsibility.
3. **Client** — builds the chain (wires the successors) and sends the request to the head handler. Depends only on the `Handler` interface.

A very common fourth element is the **Abstract Base Handler**, which stores the `next` reference and centralizes the "forward to successor" boilerplate (Template Method), so concrete handlers only implement their decision logic. The critical relationship: **the client is decoupled from the concrete handlers** — it knows only the interface and the head of the chain.
</details>

<details>
<summary><strong>Q3: [Conceptual] What's the difference between "pure" Chain of Responsibility and a "pipeline"?</strong></summary>

The GoF's original intent is **pure CoR**: the request travels the chain until **exactly one** handler consumes it, and that handler **stops** the chain (e.g., an approval chain — the first approver with sufficient authority approves, and no one downstream sees it).

A **pipeline** relaxes this: **every** handler runs in turn, each contributing something (logging, then auth, then compression), and the request is passed all the way through unless a handler deliberately short-circuits. Servlet filters and HTTP middleware are pipelines.

Both use the identical structure — a chain of handlers each forwarding to the next. The only difference is the **forwarding policy**: pure CoR stops on first match; a pipeline continues through all stages. Real systems often blend the two (a fraud pipeline where scoring stages annotate and continue but a blocklist stage short-circuits). Knowing both flavors — and that they share one structure — is a strong signal in interviews.
</details>

<details>
<summary><strong>Q4: [Conceptual] How does CoR relate to servlet filters and HTTP middleware?</strong></summary>

They *are* Chain of Responsibility (in its pipeline form, fused with Decorator). An incoming request passes through an ordered chain of filters/middleware; each one receives the request plus a handle to **invoke the rest of the chain** (`chain.doFilter(...)` in servlets, `next()` in Express/ASP.NET). A filter can: run code **before** the rest of the chain, call the next stage, run code **after** it returns, or **short-circuit** by not calling next (e.g., an auth filter returning 401).

<details>
<summary>💻 Click to expand</summary>

```java
void doFilter(req, res, chain) {
    // before...
    chain.doFilter(req, res);   // invoke rest of chain (or skip to short-circuit)
    // after...
}
```
</details>

This is the most pervasive real-world CoR and a great answer to "give a framework example" — it grounds the pattern and shows you understand both the routing (CoR) and wrapping (Decorator) aspects.
</details>

<details>
<summary><strong>Q5: [Implementation] Implement a classic logging chain with levels (INFO / DEBUG / ERROR).</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
abstract class Logger {
    enum Level { INFO, DEBUG, ERROR }
    protected final Level level;
    protected Logger next;
    Logger(Level level) { this.level = level; }
    Logger setNext(Logger next) { this.next = next; return next; }

    // Every logger handles messages at OR above its level, then forwards (pipeline flavor).
    public void log(Level msgLevel, String message) {
        if (msgLevel.ordinal() >= level.ordinal()) write(message);
        if (next != null) next.log(msgLevel, message);   // always forward
    }
    protected abstract void write(String message);
}

class ConsoleLogger extends Logger {
    ConsoleLogger(Level l) { super(l); }
    protected void write(String m) { System.out.println("[Console] " + m); }
}
class FileLogger extends Logger {
    FileLogger(Level l) { super(l); }
    protected void write(String m) { System.out.println("[File] " + m); }
}
class EmailLogger extends Logger {
    EmailLogger(Level l) { super(l); }
    protected void write(String m) { System.out.println("[Email alert] " + m); }
}

class Demo {
    public static void main(String[] args) {
        Logger chain = new ConsoleLogger(Logger.Level.INFO);
        chain.setNext(new FileLogger(Logger.Level.DEBUG))
             .setNext(new EmailLogger(Logger.Level.ERROR));

        chain.log(Logger.Level.INFO,  "user logged in");  // Console only
        chain.log(Logger.Level.ERROR, "disk failure");    // Console + File + Email
    }
}
```
</details>

This is the GoF's own motivating example. Note it's the **pipeline** flavor — each logger handles the message if it's at/above its level and *always* forwards, so one message can be picked up by several handlers.
</details>

<details>
<summary><strong>Q6: [Implementation] Implement an expense-approval chain where the amount decides who approves.</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
record Expense(String desc, double amount) {}

abstract class Approver {
    protected Approver next;
    Approver setNext(Approver n) { this.next = n; return n; }
    public final void approve(Expense e) {
        if (canApprove(e)) System.out.println(title() + " approved " + e.desc());
        else if (next != null) next.approve(e);
        else System.out.println("No one can approve " + e.desc() + " ($" + e.amount() + ")");
    }
    protected abstract boolean canApprove(Expense e);
    protected abstract String title();
}
class Lead     extends Approver { boolean canApprove(Expense e){return e.amount()<=1_000;}   String title(){return "Lead";} }
class Manager  extends Approver { boolean canApprove(Expense e){return e.amount()<=10_000;}  String title(){return "Manager";} }
class Director extends Approver { boolean canApprove(Expense e){return e.amount()<=100_000;} String title(){return "Director";} }

class Demo {
    public static void main(String[] a) {
        Approver lead = new Lead();
        lead.setNext(new Manager()).setNext(new Director());
        lead.approve(new Expense("pens", 50));       // Lead
        lead.approve(new Expense("laptop", 5_000));  // Manager
        lead.approve(new Expense("server", 90_000)); // Director
        lead.approve(new Expense("building", 5_000_000)); // No one can approve
    }
}
```
</details>

This is **pure CoR** — the first approver with authority handles it and the chain stops. The `else` on the last handler shows the crucial "fell off the end" case that a robust design must address (here a message; in production, a terminal/default handler).
</details>

<details>
<summary><strong>Q7: [Implementation] Refactor an abstract base handler so concrete handlers only write their decision logic.</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
abstract class Handler {
    private Handler next;
    Handler setNext(Handler next) { this.next = next; return next; }

    // Template Method: fixed algorithm (decide -> act or forward); forwarding written ONCE.
    public final void handle(Request r) {
        if (canHandle(r)) doHandle(r);
        else passToNext(r);
    }
    protected final void passToNext(Request r) {
        if (next != null) next.handle(r);
        else onUnhandled(r);                       // single, overridable end-of-chain policy
    }
    protected void onUnhandled(Request r) { throw new IllegalStateException("Unhandled: " + r); }
    protected abstract boolean canHandle(Request r);
    protected abstract void doHandle(Request r);
}
```
</details>

The base class owns the `next` link, the forwarding, and the end-of-chain policy; each concrete handler shrinks to `canHandle` + `doHandle`. Making `handle` and `passToNext` **`final`** guarantees no subclass can accidentally break the forwarding contract — the Template Method fixes the skeleton and defers only the variable parts. This is the form most production chains take.
</details>

<details>
<summary><strong>Q8: [Implementation] Implement an "onion" middleware chain where each stage can run code before and after the next stage.</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
import java.util.*;

@FunctionalInterface interface Next { void run(Ctx c); }
@FunctionalInterface interface Middleware { void handle(Ctx c, Next next); }
class Ctx { String path; int status = 200; Ctx(String p){path=p;} }

class Pipeline {
    private final List<Middleware> mws = new ArrayList<>();
    Pipeline use(Middleware m) { mws.add(m); return this; }
    void run(Ctx c) {
        Next chain = ctx -> {};                          // terminal
        for (int i = mws.size() - 1; i >= 0; i--) {      // fold right-to-left
            Middleware m = mws.get(i); Next downstream = chain;
            chain = ctx -> m.handle(ctx, downstream);    // each layer wraps the rest
        }
        chain.run(c);
    }
}

class Demo {
    public static void main(String[] a) {
        new Pipeline()
            .use((c, next) -> { System.out.println("-> " + c.path); next.run(c); System.out.println("<- " + c.status); })
            .use((c, next) -> { if (c.path.equals("/admin")) { c.status = 403; return; } next.run(c); }) // short-circuit
            .use((c, next) -> { System.out.println("handling"); next.run(c); })
            .run(new Ctx("/home"));
    }
}
```
</details>

The **right-to-left fold** makes the first-added middleware the outermost layer. Each middleware closes over `downstream` ("the rest of the chain"), so calling `next.run()` wraps downstream execution — code before is the request phase, code after is the response phase, and skipping `next.run()` short-circuits. This is exactly Express/OkHttp/ASP.NET middleware.
</details>

<details>
<summary><strong>Q9: [Breaking] What happens if no handler in the chain handles the request, and how do you design for it?</strong></summary>

The request **falls off the end of the chain and silently disappears** — no handler acted, and typically no error is raised. This is the single most common CoR bug: the sender assumes "someone handled it," but nobody did, so the effect just doesn't happen (a payment isn't processed, an event is dropped).

Design fixes: (1) add a **terminal/default handler** that always accepts and either performs a sensible default or **throws/logs/dead-letters** the unhandled request (Null Object variant); (2) make the chain's contract explicit — e.g., `handle` returns a `boolean`/`Optional<Result>` so the caller can detect "unhandled" and react; (3) validate at assembly time that a terminal handler exists.

<details>
<summary>💻 Click to expand — return-value contract</summary>

```java
boolean handled = head.handle(request);      // each handler returns true if it consumed it
if (!handled) throw new UnhandledRequestException(request);  // caller can't ignore it
```
</details>

The principle: never let "unhandled" be indistinguishable from "handled successfully."
</details>

<details>
<summary><strong>Q10: [Breaking] How can a chain end up in an infinite loop or a truncated chain, and how do you prevent it?</strong></summary>

Both come from **manual `setNext` wiring**. An **infinite loop** happens if a handler's successor points back to an earlier handler (`A.next=B; B.next=A`) — `handle` recurses forever and blows the stack. A **truncated chain** happens if you forget to wire a link, so later handlers never run and requests silently skip them.

Prevention: (1) assemble the chain via a **builder or ordered `List`** that appends in sequence — you can't create a cycle or a gap by construction; (2) detect duplicates/cycles at build time (reject a handler already in the chain); (3) prefer **immutable, once-built chains** over mutable `setNext` calls scattered across the codebase; (4) add a **depth guard** as a safety net for dynamically built chains.

<details>
<summary>💻 Click to expand — depth guard + build-time cycle check</summary>

```java
// Build-time: reject duplicates to prevent cycles.
if (!seen.add(handler)) throw new IllegalStateException("Cycle: handler already in chain");
// Runtime safety net: cap traversal depth.
if (++hops > MAX_HOPS) throw new IllegalStateException("Chain too deep — probable cycle");
```
</details>

The root cause is treating the chain as ad-hoc mutable links; treating it as an assembled, validated data structure eliminates the class of bug.
</details>

<details>
<summary><strong>Q11: [Breaking] Why is handler order a source of subtle bugs, and how do you make ordering safe?</strong></summary>

Because the chain's behavior depends on order, and order is **configuration** (often in code far from the handlers), a reorder can silently change correctness. Classic failures: an auth/validation check placed *after* a handler that already wrote to the DB; a decompression stage placed *after* the parser; a rate-limiter *after* the expensive work it was meant to protect. Everything compiles and often "works" in the happy path — the bug only shows under specific inputs.

Making ordering safe: (1) treat order as an **explicit, reviewed artifact** (a single ordered list, not scattered `setNext` calls); (2) encode hard constraints — e.g., some frameworks let handlers declare `@Order` or "must run before X"; (3) write **invariant tests** that assert ordering guarantees (a request rejected by auth never reaches the handler — verify via a spy/trace); (4) keep handlers **order-independent where possible** (pure, no shared-state assumptions) so order matters less.

<details>
<summary>💻 Click to expand — ordering invariant test</summary>

```java
// Spy handler records execution; assert security ran before dataWrite for every request.
assertTrue(trace.indexOf("security") < trace.indexOf("dataWrite"));
```
</details>

The defensive stance: assume any chain will eventually be reordered by someone, so make required orderings explicit and test them.
</details>

<details>
<summary><strong>Q12: [Trade-off] CoR vs. Decorator — they're structurally almost identical. How do you distinguish them?</strong></summary>

Both are a **linked list of objects sharing a common interface, each holding a reference to the next**. The difference is **intent and whether a link can decline/stop**:

- **Decorator** *always* delegates to the wrapped object and *adds* behavior around it — every layer runs, none declines. Its purpose is **augmentation** (add logging, caching, compression to an object transparently). The wrapped object is "the same thing, enhanced."
- **Chain of Responsibility** handlers *decide* whether to handle the request and *may stop the chain* (consume it and not forward). Its purpose is **routing/decision** — "which of you handles this?" A handler can decline (pass along) or consume (stop).

Ask: *does every link always run and augment (Decorator), or does a link get to decide whether to handle and possibly stop (CoR)?* Middleware blurs the line because it does both — it wraps like a Decorator *and* can short-circuit like CoR. Structurally twins; semantically distinct.
</details>

<details>
<summary><strong>Q13: [Trade-off] When should you use CoR versus a simple map/dispatch table?</strong></summary>

Use a **`Map<Key, Handler>` dispatch table** when: the handler is chosen by a **discrete, known key** (request type, command name, status code); **exactly one** handler applies; and you want **O(1)** lookup with no traversal. It's clearer and faster — no chain to walk, no "fell off the end" ambiguity.

Use **Chain of Responsibility** when: handlers are selected by **richer/overlapping conditions** (ranges, predicates, multiple criteria) rather than a single key; **multiple** handlers may need to run (a pipeline); **order matters**; you want handlers to be able to **short-circuit or wrap** downstream; or the set of handlers is **dynamically composed** and each is independently responsible.

Rule of thumb: **discrete key → one handler → map**; **predicate-based / ordered / multi-stage / wrapping → chain**. Many systems use both — a map to route to the right *chain*, then the chain for the ordered pipeline of that route.
</details>

<details>
<summary><strong>Q14: [Trade-off] What are the costs of CoR, and when does the indirection stop being worth it?</strong></summary>

Costs: (1) **debuggability** — which handler acted (or why none did) is a runtime property, not visible in the code; stack traces through deep chains are long and hard to follow. (2) **Performance** — every request walks the chain (O(n)); with many non-trivial handlers this adds latency and CPU. (3) **Implicit control flow** — the actual behavior depends on chain composition and order, which lives in configuration, so reasoning locally about "what handles this request" is hard. (4) **Hidden coupling** — handlers that depend on shared mutable context or on earlier handlers having run are coupled in ways the chain structure hides.

It stops being worth it when: there's really only one static handler (use a direct call/map); the chain is short and fixed (a plain conditional is clearer); latency is critical and the chain is long; or handler interdependencies are complex enough that an explicit workflow/state machine communicates intent better. The judgment: CoR pays off when you genuinely need **decoupling + runtime-configurable, ordered, multi-stage handling** — not for a two-branch decision.
</details>

<details>
<summary><strong>Q15: [Advanced] How do you make a Chain of Responsibility thread-safe and reusable across concurrent requests?</strong></summary>

The key rule: **handlers must be stateless (or safely shared), and per-request mutable state must live in the request/context object, not in the handlers.** A single immutable chain can then serve unlimited concurrent requests, because each request carries its own context and the handlers only read shared config.

What breaks thread safety: (1) a handler storing per-request state in **instance fields** (two threads clobber each other) — keep handlers stateless; (2) **sharing one mutable context object across threads/requests** — create a fresh context per request; (3) mutating shared config inside `handle`. For concurrent *building* of the chain, build it once at startup and publish it safely (final field / effectively immutable), so no synchronization is needed on the hot path.

<details>
<summary>💻 Click to expand</summary>

```java
// Stateless handlers + per-request context = safe concurrent reuse of ONE chain instance.
final Handler chain = buildChainOnce();           // immutable, shared
// per request (each on its own thread):
chain.handle(new RequestContext(...));            // fresh context; handlers hold no per-request state
```
</details>

This mirrors how servlet filters work: one filter instance handles all requests concurrently precisely because per-request state is in the `HttpServletRequest`, not the filter.
</details>

<details>
<summary><strong>Q16: [Advanced/Async] How do you build an asynchronous / non-blocking Chain of Responsibility?</strong></summary>

Change the handler contract to return a **future/promise** (or accept a callback) instead of running synchronously, so each stage can do non-blocking I/O and the chain composes asynchronously. Each handler returns `CompletableFuture<Result>` (or a reactive `Mono`), and forwarding chains the futures.

<details>
<summary>💻 Click to expand — async chain with CompletableFuture</summary>

```java
import java.util.concurrent.CompletableFuture;

interface AsyncHandler {
    CompletableFuture<Response> handle(Request req, AsyncHandler next);
}

// A stage does async work, then invokes the next stage (which returns a future).
AsyncHandler authThenNext = (req, next) ->
    authService.checkAsync(req)                       // returns CompletableFuture<Boolean>
        .thenCompose(ok -> ok ? next.handle(req, null)
                              : CompletableFuture.completedFuture(Response.unauthorized()));
```
</details>

Key considerations: (1) **error propagation** — a downstream failure must propagate through `exceptionally`/`handle` so upstream stages can react (retry, fallback); (2) **timeouts** per stage (`orTimeout`); (3) avoid blocking (`.get()`) inside a handler, which defeats the point; (4) in reactive stacks (Reactor/RxJava), model the chain as composed `Mono`/`Flux` operators. Netty's async `ChannelPipeline` is the canonical production example — events flow through handlers without blocking threads.
</details>

<details>
<summary><strong>Q17: [Advanced] How does backpressure interact with a Chain of Responsibility pipeline?</strong></summary>

In a **streaming/reactive** pipeline (a chain processing a high-volume flow of events), **backpressure** is the mechanism by which a slow downstream handler tells upstream stages to slow down, preventing unbounded buffering and OOM. A naive push-based chain (each stage calls the next as fast as it can) breaks when one stage is slower — events pile up in queues between stages until memory is exhausted.

Handling it: (1) use a **demand-driven (pull) protocol** — Reactive Streams (`Publisher`/`Subscriber` with `request(n)`) so downstream signals how much it can accept, and that demand propagates up the chain; (2) put **bounded buffers** between stages and define an overflow strategy (drop, block, error, latest); (3) apply **bulkheads** — separate thread pools/queues per stage so a slow stage doesn't starve others; (4) consider **load shedding** (reject early in the chain) when the pipeline is saturated. The principle: in an async chain, forwarding must respect **downstream capacity**, not just "call next" — otherwise the chain becomes a memory bomb. Reactor/Akka Streams/Netty build this into their pipeline abstractions.
</details>

<details>
<summary><strong>Q18: [Advanced/Distributed] How do you implement a Chain of Responsibility across distributed services?</strong></summary>

A distributed CoR is a **request flowing through a sequence of services**, each of which may handle, transform, enrich, reject, or forward it — think an API gateway → auth service → rate-limiter → routing → the target service, or a message passing through a series of processors on a bus (the **Pipes and Filters** architecture).

Design considerations: (1) the "successor link" becomes a **network hop** (routing table, service mesh, or message-queue topology) rather than an object reference; (2) each stage must handle **partial failure** — timeouts, retries with idempotency, circuit breakers — because any hop can fail; (3) **short-circuiting** is a service returning an error/terminal response instead of forwarding; (4) **ordering and correlation** — carry a correlation/trace id so you can reconstruct the path across services; (5) **observability** is essential — distributed tracing (OpenTelemetry) to see which stage handled or dropped a request, since you can't read a stack trace across services. Frameworks: API gateways (Kong, Envoy filters), service meshes, and message pipelines (Kafka Streams processors, Camel routes) are distributed CoR. The pattern's spirit survives; the engineering is failure-handling, idempotency, and observability across hops.
</details>

<details>
<summary><strong>Q19: [Coding Challenge] Design a request-validation chain that collects ALL errors (not first-match) and returns a result.</strong></summary>

<details>
<summary>💻 Click to expand full solution</summary>

```java
import java.util.*;
import java.util.function.*;

// Request to validate.
record SignupRequest(String email, String password, int age) {}

// Result accumulates errors across the whole chain (pipeline flavor — every validator runs).
class ValidationResult {
    private final List<String> errors = new ArrayList<>();
    void addError(String e) { errors.add(e); }
    boolean isValid() { return errors.isEmpty(); }
    List<String> errors() { return List.copyOf(errors); }
}

// A validator is a handler that inspects the request and appends errors; it never stops the chain.
@FunctionalInterface
interface Validator { void validate(SignupRequest req, ValidationResult result); }

class ValidationChain {
    private final List<Validator> validators = new ArrayList<>();
    ValidationChain add(Validator v) { validators.add(v); return this; }
    ValidationResult validate(SignupRequest req) {
        ValidationResult result = new ValidationResult();
        for (Validator v : validators) v.validate(req, result);  // ALL run — collect every error
        return result;
    }
}

class Demo {
    public static void main(String[] args) {
        ValidationChain chain = new ValidationChain()
            .add((r, res) -> { if (r.email() == null || !r.email().contains("@")) res.addError("Invalid email"); })
            .add((r, res) -> { if (r.password() == null || r.password().length() < 8) res.addError("Password too short"); })
            .add((r, res) -> { if (r.age() < 18) res.addError("Must be 18+"); });

        ValidationResult ok  = chain.validate(new SignupRequest("a@b.com", "longenough", 25));
        System.out.println(ok.isValid());                 // true

        ValidationResult bad = chain.validate(new SignupRequest("bad", "x", 12));
        System.out.println(bad.isValid() + " " + bad.errors());
        // false [Invalid email, Password too short, Must be 18+]   <-- ALL errors collected
    }
}
```
</details>

Highlights: this is the **pipeline** flavor where *every* handler runs (no short-circuit), because a good validation UX reports **all** problems at once, not just the first. The shared `ValidationResult` is the accumulator; validators are trivial lambdas; the chain is a reorderable list. Contrast with pure CoR (stop on first match) — the choice of forwarding policy is driven by the requirement.
</details>

<details>
<summary><strong>Q20: [Coding Challenge] Implement a generic, reusable chain builder that supports both short-circuit and pass-through handlers.</strong></summary>

<details>
<summary>💻 Click to expand full solution</summary>

```java
import java.util.*;

// Generic handler: returns whether it CONSUMED the request (true = stop the chain).
@FunctionalInterface
interface Handler<T> { boolean handle(T request); }

// Reusable, immutable chain built from an ordered list.
class Chain<T> {
    private final List<Handler<T>> handlers;
    private final Handler<T> fallback;                     // terminal — runs if nothing consumed
    private Chain(List<Handler<T>> handlers, Handler<T> fallback) {
        this.handlers = List.copyOf(handlers); this.fallback = fallback;
    }
    // returns true if some handler (or the fallback) consumed the request.
    boolean process(T request) {
        for (Handler<T> h : handlers) if (h.handle(request)) return true;  // short-circuit on consume
        return fallback != null && fallback.handle(request);               // guaranteed terminal
    }

    static class Builder<T> {
        private final List<Handler<T>> handlers = new ArrayList<>();
        private Handler<T> fallback;
        Builder<T> add(Handler<T> h) { handlers.add(h); return this; }      // pass-through unless it returns true
        Builder<T> fallback(Handler<T> h) { this.fallback = h; return this; }
        Chain<T> build() { return new Chain<>(handlers, fallback); }
    }

    public static void main(String[] args) {
        Chain<String> router = new Chain.Builder<String>()
            .add(cmd -> { if (cmd.startsWith("/help"))  { System.out.println("help");  return true; } return false; })
            .add(cmd -> { if (cmd.startsWith("/quit"))  { System.out.println("quit");  return true; } return false; })
            .fallback(cmd -> { System.out.println("unknown command: " + cmd); return true; }) // never falls off
            .build();

        router.process("/help");      // help
        router.process("/foo");       // unknown command: /foo  (fallback ran)
    }
}
```
</details>

The design shows the key CoR decisions in one reusable component: a `boolean` **consume contract** (return true to stop the chain), a **builder** that assembles an immutable ordered chain (no cycles/gaps possible), and a **guaranteed terminal fallback** so a request can never silently fall off the end. Generic over `<T>`, so it works for commands, HTTP requests, events — anything.
</details>

### 🧠 Staff / Principal Engineer Level

<details>
<summary><strong>SP1: [Staff] Design an async, reactive middleware pipeline for a high-throughput API gateway. What are the key decisions?</strong></summary>

I'd model each middleware as a **non-blocking stage** returning a reactive type (`Mono<Response>`), composed into a chain where each stage receives a handle to invoke the rest — the onion model, but async. Key decisions: (1) **non-blocking end to end** — no stage may block a worker thread; all I/O (auth lookups, rate-limit checks) is async, or offloaded to a bounded scheduler, so a small event-loop pool serves huge concurrency; (2) **backpressure** — use Reactive Streams demand signaling so a slow downstream (e.g., the origin service) propagates pressure upstream rather than buffering unboundedly; (3) **short-circuiting** — auth/rate-limit stages return a terminal response without invoking downstream, and this must compose cleanly with the reactive chain (`switchIfEmpty`/`flatMap` returning early); (4) **error handling & resilience** — per-stage timeouts, circuit breakers, and retries-with-idempotency, with errors propagating through the reactive chain so upstream stages can map them to responses; (5) **ordering as config** — the stage order is declarative and hot-reloadable (security first, then rate-limit, then routing); (6) **observability** — every stage emits spans/metrics with a correlation id, because you can't debug an async chain from a stack trace; (7) **isolation/bulkheads** — separate resources per route class so one slow backend can't starve the whole gateway. The pattern gives me composable stages; the staff-level engineering is making them non-blocking, backpressure-aware, resilient, and observable. Envoy/Spring Cloud Gateway are the reference implementations.
</details>

<details>
<summary><strong>SP2: [Staff] How do you add observability to a Chain of Responsibility so you can answer "which handler processed (or dropped) this request?"</strong></summary>

CoR's biggest operational weakness is that the acting handler is a runtime property invisible in code, so **observability must be built into the chain infrastructure, not bolted onto handlers**. I'd centralize it in the base handler / pipeline runner (the one place forwarding happens): (1) **tracing** — each stage opens a span (`stage.name`) around its execution, so a distributed trace shows the exact path, which stage short-circuited, and per-stage latency; (2) **structured trace/record** — carry a per-request list of "stages visited + decision (handled/passed/rejected)" (like the `trace` list in the fraud example) that's logged on completion, especially on rejection; (3) **metrics** — per-handler counters (invocations, handled, rejected, errors) and latency histograms, so you can see a handler that suddenly rejects everything; (4) **correlation id** propagated through the context so logs across handlers (and services) stitch together; (5) **an explicit "unhandled" signal** — emit a metric/alert when a request falls off the end, since silent drops are the worst failure mode. The staff insight: because forwarding is centralized (base handler / runner), instrumentation lives there once and covers every handler uniformly — handlers stay pure business logic. This turns the chain from a black box into something you can debug in production.
</details>

<details>
<summary><strong>SP3: [Principal] When is Chain of Responsibility an anti-pattern at scale, and what do you replace it with?</strong></summary>

CoR becomes an anti-pattern when its **implicit control flow and O(n) traversal** stop paying for themselves. Warning signs: (1) **very long chains walked per request** where most handlers just say "not mine" — you've turned an O(1) routing decision into an O(n) scan; a **dispatch table / trie / rules engine** is better. (2) **Complex interdependencies** — handler C only works if B ran, D undoes part of A — the chain *hides* a real workflow; an explicit **state machine or orchestration** (e.g., a workflow engine) communicates intent and makes invalid sequences impossible. (3) **Debuggability collapse** — teams can't reason about which handler acts because behavior lives in scattered ordering config; the indirection now costs more than the coupling it removed. (4) **Order-dependent correctness** that's constantly bitten by reorders. (5) **Every handler always runs anyway** — that's just iterating a list; the successor machinery is pure ceremony. Replacements: **map/dispatch tables** for keyed routing, **rules engines** for many declarative conditions, **explicit workflow/state machines** for interdependent multi-step processes, and **event-driven fan-out** when you actually want broadcast, not pass-until-handled. The principal judgment: CoR is excellent for *decoupled, ordered, short pipelines with genuine multi-handler semantics*; when the chain becomes the de-facto architecture of a complex process, make the process explicit instead of hiding it in a chain.
</details>

<details>
<summary><strong>SP4: [Principal] Compare Chain of Responsibility with the Pipes and Filters architectural pattern and with event-driven fan-out. When do you choose each?</strong></summary>

All three move a request/data through multiple processors, but the semantics and coupling differ. **CoR** is a *decision/routing* chain: handlers get a chance in order, any may **consume and stop** it; classically one handler acts; it's request/response and typically in-process. **Pipes and Filters** is CoR's architectural cousin for *data transformation*: an ordered series of filters each **transform and always forward** a stream (ETL, media processing, Unix pipes) — it's the pipeline flavor, often distributed, focused on throughput and composability, not on "who handles it." **Event-driven fan-out** (pub/sub) is *broadcast*: an event goes to **all** interested subscribers independently, with no ordering or consumption semantics — maximum decoupling, used for integration and reactions.

Choose **CoR** when you need an ordered decision with possible short-circuit and a single logical handler (auth, approval, routing). Choose **Pipes and Filters** when you're transforming a data stream through reusable, reorderable stages that all run (processing pipelines, gateways). Choose **fan-out/events** when multiple independent consumers must react to a fact and you want them fully decoupled (no ordering, no single owner). The principal framing: **CoR = "one of you, in order, may handle and stop"; Pipes and Filters = "all of you transform, in order"; events = "all of you, independently, react."** Misusing one for another creates either hidden coupling (events used as commands) or lost decoupling (a chain where a bus belonged).
</details>

<details>
<summary><strong>SP5: [Principal] A team's request pipeline occasionally drops requests in production and it's non-deterministic. How do you diagnose and systematically prevent this class of bug?</strong></summary>

Non-deterministic dropped requests in a chain almost always mean one of: **requests falling off the end unhandled** (no terminal handler), a **handler short-circuiting incorrectly** under certain inputs (returning "handled" or failing to call `next`), an **ordering/race issue** with shared mutable context under concurrency, or a **stage swallowing an exception** and neither handling nor forwarding. Diagnosis: (1) exploit the fact that the chain is instrumentable — add a **per-request trace** of stages-visited + decisions and a **counter for "fell off end"**; the drop will correlate with a specific stage or the terminal gap. (2) Reproduce with the **recorded request stream** replayed deterministically; if a stage reads hidden state (clock, shared mutable field, external service) its decision varies — that's your non-determinism. (3) Add **invariant assertions**: every request must reach a terminal decision (handled/rejected/dead-lettered) — assert this and it fires at the exact drop. Systematic prevention: (a) **guarantee a terminal handler** so nothing can silently fall off the end (Null Object / fallback); (b) make the **contract explicit** — `handle` returns a decision the runner checks, so "did nothing" is impossible to ignore; (c) ensure **stateless handlers + per-request context** to kill concurrency races; (d) forbid **swallowing exceptions** in stages — the runner catches, records, and routes to a dead-letter; (e) add **property-based tests** asserting "for any request and any valid ordering, the pipeline reaches a terminal decision"; (f) build in **observability** (traces/metrics/alert on unhandled) so the next occurrence is caught in seconds, not by user reports. The meta-point: because the pipeline is centralized infrastructure, you fix the *class* of bug at the runner (terminal guarantee + explicit contract + tracing), not by patching individual handlers — that's what turns a recurring Heisenbug into an impossible state.
</details>

---

## ⚡ Quick Revision

**One-liner:** Chain of Responsibility passes a request along a linked series of handlers, each deciding whether to handle it or forward it to the next — decoupling the sender from the receiver and letting you add, remove, or reorder handlers freely.

**The whole pattern in one paragraph:** Chain of Responsibility is a behavioral GoF pattern — *"avoid coupling the sender of a request to its receiver by giving more than one object a chance to handle the request; chain the receiving objects and pass the request along the chain until an object handles it."* Its essence is replacing a rigid `if/else if/else` dispatch with a **runtime-configurable linked list of handlers** behind a uniform interface. Three participants: the **Handler** interface (`handle(request)` + `setNext`), **ConcreteHandlers** (each decides "*is this mine?*" — handle and maybe stop, or forward to the successor), and a **Client** that builds the chain and calls only the head. A near-universal refinement is the **Abstract Base Handler** (Template Method) that stores the `next` link and centralizes the forwarding boilerplate, so concrete handlers write only `canHandle`/`doHandle`. Two flavors share this one structure: **pure CoR** (first capable handler consumes it and stops — approval/escalation) and **pipeline** (every handler runs — filters, middleware, validation collecting all errors). The **middleware "onion"** gives each stage a `next` handle so it can run code before AND after downstream, short-circuit (skip `next`), or wrap it in try/catch — this is CoR fused with **Decorator** (the pattern it's most confused with; the difference is Decorator always delegates + augments while a CoR handler may decline/stop). Modern implementations reify the chain as a **`List` iterated by a runner** or compose stages with `Function.andThen`, letting trivial handlers be lambdas and making order pure data assembled by a **builder** (which also prevents broken/circular links). The classic bug is a request **falling off the end unhandled** — always add a terminal/default handler or an explicit "unhandled" contract. Other pitfalls: **order sensitivity** (correctness depends on config-driven order — test invariants), **long-chain latency** (O(n) walk — put cheap/selective handlers first or use a dispatch table), and **shared mutable context** races (keep handlers stateless, one context per request for thread safety). Real-world CoR is everywhere: **servlet `FilterChain`**, **Spring Security's filter chain**, **`java.util.logging` logger/handler hierarchy**, and **OkHttp/Netty interceptors**. At scale it goes async (handlers returning `CompletableFuture`/`Mono`), needs **backpressure** in streaming pipelines, spans services as **Pipes and Filters** with tracing/idempotency, and becomes an **anti-pattern** when chains are long, interdependent, or hide a real workflow — then prefer a **map/dispatch table, rules engine, state machine, or event fan-out**. Contrast with **Decorator** (augment, always continue), **Command** (request as object), **Observer** (broadcast to all), and **Mediator** (central hub vs. linear pass-along).

**Top 5 interview answers to memorize:**

1. **"What is it?"** → Pass a request along a chain of handlers; each either handles it (and may stop) or forwards to the next, so the sender is decoupled from whichever handler acts.
2. **"Framework example?"** → Servlet `FilterChain` / Spring Security filter chain / `java.util.logging` handlers / OkHttp interceptors — each filter processes, short-circuits, or calls the next.
3. **"CoR vs. Decorator?"** → Structurally twins (linked handlers), but Decorator *always* delegates and *adds* behavior; a CoR handler *decides* and *may stop* the chain. Middleware does both.
4. **"What if nobody handles it?"** → It falls off the end and silently vanishes — the classic bug; add a terminal/default handler or make `handle` return a decision the caller must check.
5. **"Pure CoR vs. pipeline?"** → Pure = first capable handler consumes it and stops (approval); pipeline = every handler runs contributing something (filters, validation collecting all errors) — same structure, different forwarding policy.

**Trigger words (hear these → think Chain of Responsibility):** "pass it along / until handled", "filter / middleware / interceptor chain", "servlet FilterChain / Spring Security", "logging levels", "approval / escalation workflow", "event bubbling", "one of several handlers", "decouple sender from receiver", "add/reorder handlers at runtime", "short-circuit the chain", "pipeline of stages", "before/after next()", "process or forward", "growing if/else-if ladder".

---

*End of Chain of Responsibility Pattern study guide.*

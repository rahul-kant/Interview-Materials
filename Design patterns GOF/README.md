# 🧭 Design Patterns — Study Hub & Interview Prep Guide

> A single home page for all **23 Gang-of-Four design patterns**, each written as a deep, FAANG-interview-ready study guide. Use this page to decide **what to study, in what order, and how deep to go**.

Every pattern below links to its own self-contained `.md` file (intent → problem → implementation variants → real-world examples → pitfalls → 20+ interview Q&A → staff-level deep dives → quick revision).

---

## 📚 How to Use This Guide

1. **Short on time before an interview?** Study the **[🔥 Priority Study Order](#-priority-study-order-start-here)** — it's ranked by how often each pattern appears in real FAANG loops.
2. **Targeting Staff/Principal?** Jump to **[🏛️ Must-Know for Staff / Principal](#️-must-know-for-staff--principal-engineers)** — architecture-level patterns that show up in system-design and design-review rounds.
3. **Learning from scratch?** Go group by group: **[Creational](#-creational-patterns)** → **[Structural](#-structural-patterns)** → **[Behavioral](#-behavioral-patterns)**.
4. **Legend used throughout:**

| Symbol | Meaning |
|---|---|
| ⭐ Difficulty | 1 = trivial, 5 = deep internals (JMM, concurrency, distributed) |
| 🔥 FAANG Frequency | How often it appears in coding/design interviews: **Very High / High / Medium / Low** |
| 🏛️ Staff | Load-bearing at architecture/system-design level |
| 📖 Depth | How deep interviewers tend to probe — read accordingly |

---

## 🔥 Priority Study Order (start here)

Ranked for a candidate optimizing interview ROI. **Tier 1 first** — these are the patterns you're most likely to be asked to explain, code, or spot in a design.

| # | Pattern | Group | ⭐ | 🔥 Frequency | 📖 Depth to Study | Why it's prioritized |
|---|---------|-------|----|----|----|----|
| 1 | [Singleton](Singleton-Pattern.md) | Creational | 3 | **Very High** | **Deep** | The #1 asked pattern; JMM, `volatile`, DCL, enum, testing critiques |
| 2 | [Strategy](Strategy-Pattern.md) | Behavioral | 3 | **Very High** | **Deep** | Ubiquitous; the "composition over inheritance" poster child |
| 3 | [Factory Method](FactoryMethod-Pattern.md) | Creational | 2 | **Very High** | **Deep** | Constantly confused with Simple/Abstract Factory — nail the distinction |
| 4 | [Observer](Observer-Pattern.md) | Behavioral | 4 | **Very High** | **Deep** | Backbone of event-driven systems; pub/sub, reactive streams |
| 5 | [Decorator](Decorator-Pattern.md) | Structural | 4 | **Very High** | **Deep** | Wrapping/middleware; contrasted with Proxy & inheritance |
| 6 | [Builder](Builder-Pattern.md) | Creational | 2 | **Very High** | Medium | Immutable objects, fluent APIs; easy points if you know Step Builder |
| 7 | [Adapter](Adapter-Pattern.md) | Structural | 3 | **High** | Medium | Integration/legacy glue; anti-corruption layer |
| 8 | [Abstract Factory](AbstractFactory-Pattern.md) | Creational | 3 | **High** | **Deep** | Families of products; the Factory-Method comparison is a classic |
| 9 | [Proxy](Proxy-Pattern.md) | Structural | 4 | **High** | **Deep** | RPC, lazy loading, service mesh; vs Decorator/Facade |
| 10 | [Command](Command-Pattern.md) | Behavioral | 4 | **High** | **Deep** | Undo/redo, queues, event sourcing / CQRS |
| 11 | [State](State-Pattern.md) | Behavioral | 4 | **High** | **Deep** | State machines, workflows; vs Strategy |
| 12 | [Template Method](TemplateMethod-Pattern.md) | Behavioral | 3 | **High** | Medium | Framework hooks; the inheritance cousin of Strategy |
| 13 | [Facade](Facade-Pattern.md) | Structural | 2 | **High** | Light | API gateway/simplification; quick to master |
| 14 | [Iterator](Iterator-Pattern.md) | Behavioral | 3 | **High** | Medium | Built into every language; fail-fast, lazy sequences |
| 15 | [Chain of Responsibility](ChainOfResponsibility-Pattern.md) | Behavioral | 3 | Medium | Medium | Middleware/filter pipelines (servlets, interceptors) |
| 16 | [Composite](Composite-Pattern.md) | Structural | 3 | Medium | Medium | Trees/hierarchies; UI, filesystems, org charts |
| 17 | [Prototype](Prototype-Pattern.md) | Creational | 2 | Medium | Medium | Cloning; shallow-vs-deep copy is the whole interview |
| 18 | [Mediator](Mediator-Pattern.md) | Behavioral | 3 | Medium | Medium | Hub-and-spoke; orchestration vs choreography at scale |
| 19 | [Flyweight](Flyweight-Pattern.md) | Structural | 4 | Medium | Medium | Memory optimization; caching/interning at scale |
| 20 | [Bridge](Bridge-Pattern.md) | Structural | 4 | Low | Medium | Decouple abstraction/implementation; often confused w/ Adapter |
| 21 | [Memento](Memento-Pattern.md) | Behavioral | 4 | Low | Light | Snapshots/undo; vs Prototype |
| 22 | [Visitor](Visitor-Pattern.md) | Behavioral | 4 | Low | Medium | Double dispatch; compilers, AST traversal |
| 23 | [Interpreter](Interpreter-Pattern.md) | Behavioral | 3 | Low | Light | Grammars/DSLs; rarely asked, niche |

> **Rule of thumb:** master **Tier 1 (rows 1–14)** cold. Rows 15–19 are "know the intent + one example." Rows 20–23 are "recognize and one-line it" unless a role specifically calls for them (compilers → Visitor/Interpreter; graphics/memory → Flyweight/Bridge).

---

## 🏛️ Must-Know for Staff / Principal Engineers

At the Staff+ level, interviews shift from "code this pattern" to "**where does this live in a distributed architecture, and what breaks at scale?**" These patterns recur in system-design, design-review, and architecture rounds. Study their **staff-level deep-dive sections** specifically.

| Pattern | Architecture-level relevance |
|---------|------------------------------|
| [Singleton](Singleton-Pattern.md) | "One per JVM ≠ one per cluster" → leader election, distributed locks; DI-container scope vs hand-rolled |
| [Observer](Observer-Pattern.md) | Event-driven architecture, pub/sub, Kafka, reactive backpressure |
| [Command](Command-Pattern.md) | Event sourcing, CQRS, command bus, durable task queues |
| [Mediator](Mediator-Pattern.md) | Orchestration vs choreography; saga coordinators; message brokers |
| [Proxy](Proxy-Pattern.md) | RPC stubs, service mesh sidecars, caching/rate-limiting proxies |
| [Facade](Facade-Pattern.md) | API gateway, backend-for-frontend, subsystem boundaries |
| [Adapter](Adapter-Pattern.md) | Anti-corruption layer, cloud-provider abstraction, legacy integration |
| [Strategy](Strategy-Pattern.md) | Pluggable policies (routing, pricing, retries) resolved at runtime/config |
| [Chain of Responsibility](ChainOfResponsibility-Pattern.md) | Middleware/interceptor pipelines, request filters, auth chains |
| [Decorator](Decorator-Pattern.md) | Cross-cutting concerns: logging, metrics, retry, circuit-breaker wrappers |
| [Abstract Factory](AbstractFactory-Pattern.md) | Multi-cloud/provider families, DI wiring, environment-specific product sets |
| [Flyweight](Flyweight-Pattern.md) | Memory efficiency at scale — object pooling, interning, shared caches |
| [State](State-Pattern.md) | Workflow engines, order/lifecycle state machines, protocol handling |

---

## 📖 Read In-Depth for Interviews

These have the **deepest follow-up questions** — the interviewer rarely stops at the definition. Budget extra time on their *Implementation variants*, *Common Pitfalls*, and *Interview Q&A* sections.

- **[Singleton](Singleton-Pattern.md)** — thread-safety (naive → synchronized → DCL+`volatile` → Bill Pugh → enum), Java Memory Model, serialization/reflection/cloning attacks, why it's called an anti-pattern.
- **[Factory Method](FactoryMethod-Pattern.md)** vs **[Abstract Factory](AbstractFactory-Pattern.md)** — the single most-confused pair; one product via inheritance vs a family via composition.
- **[Strategy](Strategy-Pattern.md)** vs **[State](State-Pattern.md)** — identical structure, opposite intent (pluggable algorithm vs self-transitioning behavior).
- **[Decorator](Decorator-Pattern.md)** vs **[Proxy](Proxy-Pattern.md)** — same wrapping shape; different intent (add behavior vs control access).
- **[Observer](Observer-Pattern.md)** vs **[Mediator](Mediator-Pattern.md)** — broadcast vs coordination; they're often composed.
- **[Builder](Builder-Pattern.md)** — telescoping constructors, immutability, Step Builder, records-vs-builder.
- **[Command](Command-Pattern.md)** — undo/redo, invoker/receiver decoupling, event sourcing.
- **[Proxy](Proxy-Pattern.md)** — dynamic proxies, AOP, lazy loading, remote stubs.

---

## 🎨 Patterns by Group

### 🏗️ Creational Patterns
*How objects get created — decoupling construction from use.*

| Pattern | ⭐ | 🔥 | One-liner |
|---------|----|----|-----------|
| [Singleton](Singleton-Pattern.md) | 3 | Very High | Guarantee one instance + a global access point |
| [Factory Method](FactoryMethod-Pattern.md) | 2 | Very High | Let subclasses decide which concrete class to instantiate |
| [Abstract Factory](AbstractFactory-Pattern.md) | 3 | High | Create families of related products without concrete classes |
| [Builder](Builder-Pattern.md) | 2 | Very High | Assemble a complex/immutable object step by step |
| [Prototype](Prototype-Pattern.md) | 2 | Medium | Create new objects by cloning a configured prototype |

### 🧱 Structural Patterns
*How objects and classes are composed into larger structures.*

| Pattern | ⭐ | 🔥 | One-liner |
|---------|----|----|-----------|
| [Adapter](Adapter-Pattern.md) | 3 | High | Make an incompatible interface fit an expected one |
| [Decorator](Decorator-Pattern.md) | 4 | Very High | Add behavior by wrapping, without subclassing |
| [Proxy](Proxy-Pattern.md) | 4 | High | A stand-in that controls access to a real object |
| [Facade](Facade-Pattern.md) | 2 | High | A simple front over a complex subsystem |
| [Composite](Composite-Pattern.md) | 3 | Medium | Treat individual objects and trees uniformly |
| [Bridge](Bridge-Pattern.md) | 4 | Low | Decouple an abstraction from its implementation |
| [Flyweight](Flyweight-Pattern.md) | 4 | Medium | Share fine-grained objects to save memory |

### 🔄 Behavioral Patterns
*How objects communicate and distribute responsibility.*

| Pattern | ⭐ | 🔥 | One-liner |
|---------|----|----|-----------|
| [Strategy](Strategy-Pattern.md) | 3 | Very High | Swap interchangeable algorithms behind one interface |
| [Observer](Observer-Pattern.md) | 4 | Very High | Notify many dependents when one subject changes |
| [Command](Command-Pattern.md) | 4 | High | Encapsulate a request as an object (queue/undo/log) |
| [State](State-Pattern.md) | 4 | High | Let an object change behavior as its state changes |
| [Template Method](TemplateMethod-Pattern.md) | 3 | High | Fix an algorithm skeleton, let subclasses fill steps |
| [Iterator](Iterator-Pattern.md) | 3 | High | Traverse a collection without exposing its internals |
| [Chain of Responsibility](ChainOfResponsibility-Pattern.md) | 3 | Medium | Pass a request along a chain of handlers |
| [Mediator](Mediator-Pattern.md) | 3 | Medium | Centralize many-to-many communication in a hub |
| [Memento](Memento-Pattern.md) | 4 | Low | Capture and restore an object's state (undo) |
| [Visitor](Visitor-Pattern.md) | 4 | Low | Add operations to a type hierarchy via double dispatch |
| [Interpreter](Interpreter-Pattern.md) | 3 | Low | Represent and evaluate a grammar/DSL |

---

## 🗺️ Suggested Learning Paths

**⚡ Crash course (1–2 days before an interview):**
[Singleton](Singleton-Pattern.md) → [Factory Method](FactoryMethod-Pattern.md) → [Strategy](Strategy-Pattern.md) → [Observer](Observer-Pattern.md) → [Decorator](Decorator-Pattern.md) → [Builder](Builder-Pattern.md) → [Adapter](Adapter-Pattern.md)

**📗 Solid coverage (1 week):** the above **+** [Abstract Factory](AbstractFactory-Pattern.md), [Proxy](Proxy-Pattern.md), [Command](Command-Pattern.md), [State](State-Pattern.md), [Template Method](TemplateMethod-Pattern.md), [Facade](Facade-Pattern.md), [Iterator](Iterator-Pattern.md).

**🏛️ Staff/Principal track:** focus on the [Must-Know table](#️-must-know-for-staff--principal-engineers) and read each pattern's **Staff/Principal Deep-Dive** section, emphasizing distributed-systems trade-offs (SPOF, scaling, orchestration vs choreography, DI).

**🎓 Completionist (all 23):** finish the remaining Structural ([Composite](Composite-Pattern.md), [Bridge](Bridge-Pattern.md), [Flyweight](Flyweight-Pattern.md)) and Behavioral ([Chain of Responsibility](ChainOfResponsibility-Pattern.md), [Mediator](Mediator-Pattern.md), [Memento](Memento-Pattern.md), [Visitor](Visitor-Pattern.md), [Interpreter](Interpreter-Pattern.md)) patterns.

---

## 📊 At a Glance — All 23 Patterns

| Group | Count | Patterns |
|-------|-------|----------|
| 🏗️ Creational | 5 | Singleton, Factory Method, Abstract Factory, Builder, Prototype |
| 🧱 Structural | 7 | Adapter, Decorator, Proxy, Facade, Composite, Bridge, Flyweight |
| 🔄 Behavioral | 11 | Strategy, Observer, Command, State, Template Method, Iterator, Chain of Responsibility, Mediator, Memento, Visitor, Interpreter |

*Each linked file follows the same structure: 📋 Intent · 🎯 Problem · ✅ Solution · 💻 Implementation (variants) · 🎨 Real-World Examples · ✅❌ When to (not) Use · 🔄 Comparisons · 💡 Pitfalls · 🎓 Interview Tips · 📚 Library usage · 📝 20 Q&A + Staff deep-dives · ⚡ Quick Revision.*

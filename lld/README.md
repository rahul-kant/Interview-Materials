# 🏛️ Low-Level Design — Master Index & Study Guide

> A single lookup home for all 23 LLD study guides. Use it to decide **what to study, in what order, and how deeply** — driven by interview frequency, seniority expectations, and pattern-transfer value.

---

## 📚 Table of Contents

- [How to Use This Guide](#-how-to-use-this-guide)
- [Priority Roadmap — What to Study in What Order](#-priority-roadmap--what-to-study-in-what-order)
- [Problems Grouped by Dominant Theme](#-problems-grouped-by-dominant-theme)
- [Full Metrics Matrix](#-full-metrics-matrix)
- [Pattern-Transfer Map](#-pattern-transfer-map)
- [Complete A–Z Topic Index](#-complete-az-topic-index)
- [Legend](#-legend)

---

## 🎯 How to Use This Guide

Interviewers at L5/L6 rarely test whether you have *memorised* one design. They test whether you can **transfer a pattern** from a problem you know to a problem you have never seen. That is why the problems below are grouped by **dominant theme** rather than alphabetically: once you have internalised the state machine in the Vending Machine, the ATM and the Elevator become variations, not new problems.

Read in this sequence:

1. Start with the [Priority Roadmap](#-priority-roadmap--what-to-study-in-what-order) to pick your track.
2. Study one full group at a time from [Problems Grouped by Dominant Theme](#-problems-grouped-by-dominant-theme) so the shared patterns compound.
3. Use the [Full Metrics Matrix](#-full-metrics-matrix) to know which topics deserve deep, line-by-line study versus a quick skim.

---

## 🚦 Priority Roadmap — What to Study in What Order

The tiers below are ordered so that each one builds the vocabulary needed for the next. Complete a tier before advancing.

### Tier 1 — Foundations (build the core vocabulary first)

These teach the State, Strategy, and Factory patterns on small, self-contained problems. Everything later reuses them.

| Order | Problem | Why first |
|:---:|---|---|
| 1 | [Parking Lot](Parking-Lot.md) | The universal warm-up; teaches entity modelling, Strategy for spot allocation, and clean OOP. |
| 2 | [Vending Machine](Vending-Machine.md) | The canonical State-machine problem — the clearest introduction to State transitions. |
| 3 | [LRU Cache](LRU-Cache.md) | Data-structure discipline: `HashMap` + doubly linked list under `O(1)` constraints. |
| 4 | [Tic-Tac-Toe](Tic-Tac-Toe.md) | Smallest board game; Strategy for win-detection, State for game lifecycle. |

### Tier 2 — High-Frequency Core (the most-asked in real loops)

| Order | Problem | Why now |
|:---:|---|---|
| 5 | [LFU Cache](LFU-Cache.md) | Builds directly on LRU: three-structure `O(1)` design with a `minFreq` pointer and LRU tie-break. |
| 6 | [Elevator System](Elevator-System.md) | Scheduling Strategy + State; the archetypal "control system" question. |
| 7 | [Rate Limiter](Rate-Limiter.md) | Algorithm-driven design (token/leaky bucket, sliding window) with concurrency. |
| 8 | [URL Shortener](URL-Shortener.md) | Encoding strategy, collision handling, and repository/scaling discussion. |
| 9 | [ATM Machine](ATM-Machine.md) | State + Chain of Responsibility; transaction integrity under failure. |
| 10 | [Movie Ticket Booking](Movie-Ticket-Booking.md) | First real concurrency problem — seat locking and double-booking. |

### Tier 3 — Staff / Principal Depth (where senior signal is earned)

These reward discussion of concurrency, consistency, extensibility, and scale. Interviewers push hardest here.

| Order | Problem | Senior signal |
|:---:|---|---|
| 11 | [Notification Service](Notification-Service.md) | Multi-channel pipeline, retries, Builder + Chain of Responsibility + Adapter. |
| 12 | [Splitwise](Splitwise.md) | Debt-simplification algorithm + precise money modelling. |
| 13 | [Ride-Sharing System](Ride-Sharing-System.md) | Matching engine, geospatial lookup, driver/rider state machines. |
| 14 | [Task Scheduler](Task-Scheduler.md) | Producer–consumer, priority queues, Command pattern, thread pools. |
| 15 | [Meeting Scheduler](Meeting-Scheduler.md) | Interval/conflict logic, Observer notifications, calendar consistency. |
| 16 | [Hotel Booking System](Hotel-Booking-System.md) | Inventory + reservation concurrency at scale. |

### Tier 4 — Breadth & Round-Out (cover for completeness)

| Order | Problem | Value |
|:---:|---|---|
| 17 | [Online Shopping System](Online-Shopping-System.md) | Large composite domain — cart, order, payment, catalog. |
| 18 | [Logging System](Logging-System.md) | Chain of Responsibility + async appenders; classic library design. |
| 19 | [Social Media Feed](Social-Media-Feed.md) | Fan-out strategies, Repository, feed ranking. |
| 20 | [File System](File-System.md) | Composite + Visitor tree modelling done well. |
| 21 | [Chess Game](Chess-Game.md) | Rich rules engine; Strategy per piece, Command for undo. |
| 22 | [Snake and Ladder](Snake-and-Ladder.md) | Board-game variant; consolidates State + Builder. |
| 23 | [Library Management System](Library-Management-System.md) | Entity/CRUD modelling with Observer for due dates. |

---

## 🗂️ Problems Grouped by Dominant Theme

Grouping is by the *primary* design challenge. The **Core patterns tested** column lists what an interviewer actively probes — master a group and the whole cluster transfers.

### 1. ⚙️ Finite-State Machines & Hardware Simulators

The interviewer wants a clean, explicit state machine with no `if/else` sprawl.

| Problems | Core patterns tested | Transfer skill |
|---|---|---|
| [Vending Machine](Vending-Machine.md) · [ATM Machine](ATM-Machine.md) · [Elevator System](Elevator-System.md) · [Parking Lot](Parking-Lot.md) | **State**, **Strategy**, Singleton, Factory, Observer | Model discrete states as objects; push transition logic into the state, not the context. |

### 2. 🎲 Turn-Based Games

Rules engine + game lifecycle + (often) undo.

| Problems | Core patterns tested | Transfer skill |
|---|---|---|
| [Tic-Tac-Toe](Tic-Tac-Toe.md) · [Snake and Ladder](Snake-and-Ladder.md) · [Chess Game](Chess-Game.md) | **Strategy** (rules/win-check), **State** (game phase), **Command** (undo/replay), Observer, Factory, Builder | Separate rules from board state; represent moves as first-class objects. |

### 3. 🔒 Booking & Reservation Systems (Concurrency-Heavy)

The differentiator is correct behaviour under concurrent access.

| Problems | Core patterns tested | Transfer skill |
|---|---|---|
| [Movie Ticket Booking](Movie-Ticket-Booking.md) · [Hotel Booking System](Hotel-Booking-System.md) · [Meeting Scheduler](Meeting-Scheduler.md) · [Ride-Sharing System](Ride-Sharing-System.md) · [Online Shopping System](Online-Shopping-System.md) | **Strategy**, **State**, **Observer**, Factory, Facade + optimistic/pessimistic locking | Reserve-then-confirm flows, idempotency, and preventing double-allocation. |

### 4. 🧩 Infra / Systems Components (Data Structure + Algorithm + Scale)

Small surface area, deep on data structures, algorithms, and horizontal scale.

| Problems | Core patterns tested | Transfer skill |
|---|---|---|
| [LRU Cache](LRU-Cache.md) · [LFU Cache](LFU-Cache.md) · [Rate Limiter](Rate-Limiter.md) · [URL Shortener](URL-Shortener.md) | **Strategy** (eviction/limiting/encoding), Decorator, Template Method, Repository | `O(1)` design under constraints; swap algorithms behind a stable interface; shard for scale. |

### 5. 🔁 Async Pipelines & Messaging

Requests flow through configurable, ordered stages, often asynchronously.

| Problems | Core patterns tested | Transfer skill |
|---|---|---|
| [Notification Service](Notification-Service.md) · [Logging System](Logging-System.md) · [Task Scheduler](Task-Scheduler.md) | **Chain of Responsibility**, **Strategy**, **Command**, Builder, Producer–Consumer, Adapter, Observer | Compose processing stages; decouple submission from execution with queues and pools. |

### 6. 🌳 Content & Data Modeling

Getting the domain model and data access right dominates.

| Problems | Core patterns tested | Transfer skill |
|---|---|---|
| [File System](File-System.md) · [Splitwise](Splitwise.md) · [Social Media Feed](Social-Media-Feed.md) · [Library Management System](Library-Management-System.md) | **Composite**, **Visitor**, **Repository**, **Strategy**, Observer, Builder | Tree/graph modelling, separating traversal from structure, and clean persistence boundaries. |

---

## 📊 Full Metrics Matrix

Sorted by overall study priority. Use this to allocate depth: spend most time on ⭐⭐⭐⭐+ rows.

| Priority | Problem | Theme | FAANG Frequency | Staff/Principal Must-Know | Read In Depth | Difficulty |
|:---:|---|---|:---:|:---:|:---:|:---:|
| 1 | [Parking Lot](Parking-Lot.md) | State Machine | ⭐⭐⭐⭐⭐ | ✅ Yes | ✅ Deep | 🟢 Easy |
| 2 | [LRU Cache](LRU-Cache.md) | Infra Component | ⭐⭐⭐⭐⭐ | ✅ Yes | ✅ Deep | 🟢 Easy |
| 3 | [LFU Cache](LFU-Cache.md) | Infra Component | ⭐⭐⭐⭐ | ✅ Yes | ✅ Deep | 🟡 Medium |
| 4 | [Rate Limiter](Rate-Limiter.md) | Infra Component | ⭐⭐⭐⭐⭐ | ✅ Yes | ✅ Deep | 🟡 Medium |
| 5 | [URL Shortener](URL-Shortener.md) | Infra Component | ⭐⭐⭐⭐⭐ | ✅ Yes | ✅ Deep | 🟡 Medium |
| 6 | [Elevator System](Elevator-System.md) | State Machine | ⭐⭐⭐⭐ | ✅ Yes | ✅ Deep | 🟡 Medium |
| 7 | [Vending Machine](Vending-Machine.md) | State Machine | ⭐⭐⭐⭐ | ➖ Helpful | ✅ Deep | 🟢 Easy |
| 8 | [Movie Ticket Booking](Movie-Ticket-Booking.md) | Booking / Concurrency | ⭐⭐⭐⭐ | ✅ Yes | ✅ Deep | 🟡 Medium |
| 9 | [Notification Service](Notification-Service.md) | Async Pipeline | ⭐⭐⭐⭐ | ✅ Yes | ✅ Deep | 🔴 Hard |
| 10 | [Splitwise](Splitwise.md) | Data Modeling | ⭐⭐⭐⭐ | ✅ Yes | ✅ Deep | 🟡 Medium |
| 11 | [ATM Machine](ATM-Machine.md) | State Machine | ⭐⭐⭐ | ➖ Helpful | ✅ Deep | 🟡 Medium |
| 12 | [Ride-Sharing System](Ride-Sharing-System.md) | Booking / Concurrency | ⭐⭐⭐ | ✅ Yes | ✅ Deep | 🔴 Hard |
| 13 | [Task Scheduler](Task-Scheduler.md) | Async Pipeline | ⭐⭐⭐ | ✅ Yes | ✅ Deep | 🔴 Hard |
| 14 | [Meeting Scheduler](Meeting-Scheduler.md) | Booking / Concurrency | ⭐⭐⭐ | ➖ Helpful | 🟨 Selective | 🟡 Medium |
| 15 | [Hotel Booking System](Hotel-Booking-System.md) | Booking / Concurrency | ⭐⭐⭐ | ✅ Yes | 🟨 Selective | 🟡 Medium |
| 16 | [Online Shopping System](Online-Shopping-System.md) | Booking / Concurrency | ⭐⭐⭐ | ➖ Helpful | 🟨 Selective | 🔴 Hard |
| 17 | [Tic-Tac-Toe](Tic-Tac-Toe.md) | Turn-Based Game | ⭐⭐⭐ | ➖ Helpful | ✅ Deep | 🟢 Easy |
| 18 | [Logging System](Logging-System.md) | Async Pipeline | ⭐⭐⭐ | ✅ Yes | 🟨 Selective | 🟡 Medium |
| 19 | [Social Media Feed](Social-Media-Feed.md) | Data Modeling | ⭐⭐⭐ | ✅ Yes | 🟨 Selective | 🔴 Hard |
| 20 | [File System](File-System.md) | Data Modeling | ⭐⭐ | ➖ Helpful | 🟨 Selective | 🟡 Medium |
| 21 | [Chess Game](Chess-Game.md) | Turn-Based Game | ⭐⭐ | ➖ Helpful | 🟨 Selective | 🔴 Hard |
| 22 | [Snake and Ladder](Snake-and-Ladder.md) | Turn-Based Game | ⭐⭐ | ➖ Helpful | 🟨 Selective | 🟢 Easy |
| 23 | [Library Management System](Library-Management-System.md) | Data Modeling | ⭐⭐ | ➖ Helpful | 🟨 Selective | 🟢 Easy |

---

## 🔗 Pattern-Transfer Map

If you learn a pattern once, here is where it pays off again. Study the **anchor** deeply, then the rest become quick.

| Pattern | Anchor problem (learn here) | Reappears in |
|---|---|---|
| **State** | Vending Machine | ATM, Elevator, Parking Lot, Movie Booking, all games |
| **Strategy** | Rate Limiter | Elevator scheduling, URL encoding, LRU/LFU eviction, pricing, matching |
| **Chain of Responsibility** | Notification Service | ATM transactions, Logging appenders |
| **Command** | Chess Game | Task Scheduler, Snake and Ladder (move/undo) |
| **Observer** | Library Management | Meeting Scheduler, feeds, booking notifications |
| **Composite + Visitor** | File System | Any tree/hierarchy modelling, org charts, menus |
| **Builder** | Notification Service | Splitwise, games, complex request objects |
| **Producer–Consumer** | Task Scheduler | Notification Service, async logging |

---

## 🔤 Complete A–Z Topic Index

- [ATM Machine](ATM-Machine.md)
- [Chess Game](Chess-Game.md)
- [Elevator System](Elevator-System.md)
- [File System](File-System.md)
- [Hotel Booking System](Hotel-Booking-System.md)
- [LFU Cache](LFU-Cache.md)
- [Library Management System](Library-Management-System.md)
- [Logging System](Logging-System.md)
- [LRU Cache](LRU-Cache.md)
- [Meeting Scheduler](Meeting-Scheduler.md)
- [Movie Ticket Booking](Movie-Ticket-Booking.md)
- [Notification Service](Notification-Service.md)
- [Online Shopping System](Online-Shopping-System.md)
- [Parking Lot](Parking-Lot.md)
- [Rate Limiter](Rate-Limiter.md)
- [Ride-Sharing System](Ride-Sharing-System.md)
- [Snake and Ladder](Snake-and-Ladder.md)
- [Social Media Feed](Social-Media-Feed.md)
- [Splitwise](Splitwise.md)
- [Task Scheduler](Task-Scheduler.md)
- [Tic-Tac-Toe](Tic-Tac-Toe.md)
- [URL Shortener](URL-Shortener.md)
- [Vending Machine](Vending-Machine.md)

---

## 🧭 Legend

**FAANG Frequency** — how often the problem appears in real interview loops.

- ⭐⭐⭐⭐⭐ Extremely common (expect it in a warm-up or core round)
- ⭐⭐⭐⭐ Frequently asked
- ⭐⭐⭐ Regularly asked
- ⭐⭐ Occasional / company-specific

**Staff/Principal Must-Know** — ✅ Yes means the follow-up depth (concurrency, scale, extensibility) is where senior candidates are separated; ➖ Helpful means good practice but lower differentiation.

**Read In Depth** — ✅ Deep: study line by line including the Java implementation and follow-ups. 🟨 Selective: understand the model and patterns; skim the rest.

**Difficulty** — 🟢 Easy · 🟡 Medium · 🔴 Hard (relative to a 45-minute LLD round).

---

*Home index for the LLD study guides. Each linked file is a complete, standalone guide with requirements, domain model, UML, patterns, full Java implementation, and a FAANG Q&A section.*

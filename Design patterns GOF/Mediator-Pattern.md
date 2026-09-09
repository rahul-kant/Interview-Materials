# Mediator Pattern ⭐⭐⭐ (Difficulty: 3/5 — the core idea is simple, but the god-object risk, the Mediator↔Observer↔Event-Bus boundary, and distributed orchestration-vs-choreography get deep fast)

> **Category:** Behavioral Pattern (GoF)
> **Also known as:** Intermediary, Controller

The Mediator pattern **replaces a tangled mesh of objects that all talk directly to each other with a single hub through which they communicate**. Instead of every colleague holding references to every other colleague (an `O(n²)` web of dependencies), each colleague knows only the mediator, and the mediator holds the interaction logic. It turns a *many-to-many* rats-nest into a *star*. It's the pattern behind chat rooms, air-traffic-control towers, GUI dialogs with interdependent widgets, smart-home hubs, and — scaled up — event buses, message brokers, and saga orchestrators.

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 0: Colleagues Referencing Each Other Directly (Anti-pattern)](#variant-0-colleagues-referencing-each-other-directly-anti-pattern)
   - [Variant 1: Basic Mediator Interface + ConcreteMediator + Colleague Base](#variant-1-basic-mediator-interface--concretemediator--colleague-base)
   - [Variant 2: Mediator with an Event/Notification Method](#variant-2-mediator-with-an-eventnotification-method)
   - [Variant 3: Decoupled, Event-Driven Mediator (Typed Events, No Concrete Coupling)](#variant-3-decoupled-event-driven-mediator-typed-events-no-concrete-coupling)
   - [Variant 4: Production-Grade — Event Bus / Message-Broker-Style Mediator](#variant-4-production-grade--event-bus--message-broker-style-mediator)
5. [🎨 Real-World Example](#-real-world-example)
   - [Scenario 1 — Chat Room Server](#scenario-1--chat-room-server)
   - [Scenario 2 — Air Traffic Control Tower](#scenario-2--air-traffic-control-tower)
   - [Scenario 3 — Order Coordination Hub (Event-Driven)](#scenario-3--order-coordination-hub-event-driven)
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

> **GoF Definition:** *"Define an object that encapsulates how a set of objects interact. Mediator promotes loose coupling by keeping objects from referring to each other explicitly, and it lets you vary their interaction independently."*

The Mediator pattern exists to solve a single core tension: when **many objects must collaborate**, wiring them to talk *directly* to one another creates a dense web of dependencies where every object knows about — and is coupled to — many others. Change one, and you ripple through the rest; reuse one in isolation, and you can't, because it drags its collaborators with it.

The key insight is to **introduce a mediator object that sits between the colleagues and owns the interaction logic**. Colleagues no longer reference each other; they reference only the mediator. When something happens to a colleague, it tells the mediator ("I changed", "a message arrived", "a button was clicked"), and the mediator decides what — if anything — the other colleagues should do in response. The many-to-many communication graph collapses into a one-to-many star centered on the mediator: coupling among colleagues drops from `O(n²)` potential edges to `O(n)` (each colleague ↔ the mediator). The *interaction protocol* — which was previously smeared across all the colleagues — is now centralized in one place where it can be understood, changed, and tested independently.

---

## 🎯 Problem

You have a set of objects that must cooperate to accomplish something, and the number of objects and the richness of their interactions is non-trivial. The naive approach has each object hold references to the others it needs to coordinate with and call them directly. This *works* for two or three objects, but as the set grows, the interconnections explode: with `n` objects that all potentially interact, you can have up to `n·(n−1)` directed references — an `O(n²)` mesh. The behavior of the whole system is now an emergent property of countless point-to-point wires, and no single place describes "how these objects work together."

**The pain points that lead you to Mediator:**

- **Combinatorial coupling.** Every colleague references every colleague it coordinates with. Adding one colleague can require touching many existing ones. The dependency graph is a dense mesh, not a tree.
- **Interaction logic is scattered.** The rules of "when A changes, B and C must update" live *inside* A, B, and C, duplicated and drifting. There's no single place that describes the collaboration.
- **Colleagues aren't reusable.** A widget or component can't be lifted into another context because it hard-references its siblings. Reuse drags the whole clique along.
- **Behavior is hard to change.** Altering how the objects coordinate means editing many classes. You can't "vary the interaction independently" of the objects.

**Concrete example scenarios:**

1. **GUI dialog with interdependent widgets (the classic GoF example).** A font dialog: selecting a font family in a list should enable/disable certain checkboxes, update a preview, and toggle the OK button. If the list box directly calls the checkbox, the preview, and the button — and the checkbox calls the button, and so on — every widget knows about every other. A `DialogDirector` (mediator) instead receives "widget changed" notifications and orchestrates the rest.

2. **Chat room.** `n` users in a room. If each user held references to all others and sent messages peer-to-peer, that's an `O(n²)` mesh, and joining/leaving means updating everyone. A `ChatRoom` mediator receives a message from one user and fans it out to the others; users know only the room.

3. **Air traffic control.** Dozens of aircraft near an airport. If each plane coordinated landing/spacing with every other plane directly, the coordination is `O(n²)` and unsafe. A **control tower** (mediator) is the single authority: planes report to the tower, and the tower issues instructions. Planes never talk to each other.

4. **Microservice orchestration.** A "place order" flow touches payment, inventory, shipping, and notification services. Choreographing this by having each service call the next directly produces a brittle, hard-to-trace chain. An **orchestrator / saga coordinator** (a distributed mediator) drives the steps, so the workflow lives in one place and each service stays decoupled from the others.

---

## ✅ Solution

The core idea, in plain language: **stop letting the colleagues talk to each other. Insert a mediator that every colleague knows, and route all cross-colleague communication through it. When a colleague has news, it notifies the mediator; the mediator contains the logic for how the other colleagues should react and drives them.** The colleagues become simple and independent — each knows only *itself* and *the mediator* — while the mediator becomes the single home for the collaboration logic.

**Key structural elements:**

- **Mediator (interface):** declares the method(s) colleagues use to communicate with the hub — typically a `notify(sender, event)` or domain-specific methods like `send(message, from)`. This is the contract colleagues depend on instead of depending on each other.
- **ConcreteMediator:** implements the coordination logic. It knows the colleagues (holds references to them or a registry) and, when notified, decides which colleagues to invoke and how. **All the interaction rules live here.**
- **Colleague (base class / interface):** each colleague holds a reference to the mediator (not to sibling colleagues). When something noteworthy happens, it calls the mediator rather than a peer. Colleagues are ignorant of one another.
- **Concrete Colleagues:** the actual participating objects (users, widgets, aircraft, services). They send to the mediator and receive from it, but never reference each other.

**The mechanism that makes it work:** *centralization of many-to-many interaction into a single hub, replacing a mesh with a star.* Because every colleague depends only on the mediator interface (dependency inversion), the number of coupling edges drops from a potential `O(n²)` to `O(n)`. The interaction protocol — previously an emergent property of scattered point-to-point calls — becomes an explicit, single-responsibility object you can read, test, and change in isolation. This is exactly what the GoF mean by "vary their interaction independently": you can rewrite how colleagues coordinate by editing only the mediator, without touching a single colleague.

The **central trade-off**, which every strong answer must name, is the **god-object risk**: because the mediator absorbs all the coordination logic, it can grow into a bloated, do-everything class that's hard to maintain — you've traded a distributed-complexity problem (the mesh) for a centralized-complexity problem (the hub). The art of applying Mediator well is keeping the mediator focused on *coordination* while leaving *domain behavior* on the colleagues, and, at scale, decomposing one god-mediator into several focused mediators or a publish/subscribe event bus.

---

## 💻 Implementation

We'll model a **chat room** — the canonical FAANG Mediator question and a domain everyone understands — evolving from a peer-to-peer mesh to a production event-bus-style hub. Users send messages; the room must deliver each message to the *other* users, handle join/leave, and (later) support features like private messages, moderation, and typed events. The chat room is the mediator; users are the colleagues. Each variant fixes a specific weakness of the previous one, so read them top-to-bottom as one story.

**The coupling we're fixing (same for every variant):**

```
   Peer-to-peer mesh (Variant 0):          Star via mediator (Variant 1+):

       User A ───── User B                     User A     User B
        │  \       /  │                            \       /
        │   \     /   │                             \     /
        │    \   /    │                          ┌──────────┐
       User D ─── User C                         │ ChatRoom │  (mediator)
                                                 └──────────┘
     n·(n−1) edges  = O(n²)                        /     \
                                              User D     User C
                                              n edges = O(n)
```

### Variant 0: Colleagues Referencing Each Other Directly (Anti-pattern)

**What's wrong with it:** Before showing Mediator, understand the disease it cures. Each `User` holds a list of *every other user* and delivers messages by iterating that list and calling each peer directly. Every user is coupled to every other user — an `O(n²)` mesh. Adding a user means registering it with everyone; removing one means every user must forget it. There is no single place that describes "how messages flow" — the routing logic is smeared across every `User`. Cross-cutting features (logging, moderation, blocking) must be copy-pasted into each user. This is precisely the tangle Mediator dissolves.

<details>
<summary>💻 Click to expand code — the anti-pattern (peer-to-peer chat mesh)</summary>

```java
import java.util.*;

// DON'T DO THIS — each user references every other user directly.
class UserMesh {
    private final String name;
    private final List<UserMesh> peers = new ArrayList<>();   // knows ALL other users

    UserMesh(String name) { this.name = name; }

    // Wiring: everyone must be introduced to everyone else (O(n^2) connections).
    void connect(UserMesh other) {
        peers.add(other);
        other.peers.add(this);   // bidirectional — both sides now coupled
    }

    void send(String msg) {
        System.out.println(name + " sends: " + msg);
        // Routing logic lives HERE, duplicated in every user.
        for (UserMesh p : peers) p.receive(msg, this);
    }

    void receive(String msg, UserMesh from) {
        System.out.println("  " + name + " got from " + from.name + ": " + msg);
    }

    // ---------- Usage demo ----------
    public static void main(String[] args) {
        UserMesh alice = new UserMesh("Alice");
        UserMesh bob   = new UserMesh("Bob");
        UserMesh carol = new UserMesh("Carol");

        // Every pair must be wired together by hand — 3 users -> 3 edges, 10 users -> 45 edges.
        alice.connect(bob);
        alice.connect(carol);
        bob.connect(carol);

        alice.send("Hi everyone");   // Alice loops over her peers and calls each one
        // Add a 4th user? You must connect() it to all 3 existing users.
        // Want to log or moderate messages? Edit EVERY user's send()/receive().
    }
}
```
</details>

**Code walkthrough:** The `peers` list *is* the mesh. With 3 users there are 3 undirected edges; with 10 there are 45; with `n` there are `n·(n−1)/2` — quadratic growth. The routing loop lives inside `User.send`, so every user is a mini message-router, and the moment you want a cross-cutting concern (audit log, profanity filter, "Alice blocked Bob") you must edit every user. There is nowhere that answers "how do messages flow in this room?" — the answer is "read all of the users and reconstruct it in your head."

**Pros:** Trivially simple for 2–3 objects; no extra hub class; direct calls are easy to trace for a tiny fixed set.
**Cons:** `O(n²)` coupling; every colleague knows every other; adding/removing a colleague ripples through all of them; interaction logic is duplicated and scattered; colleagues aren't reusable in isolation; cross-cutting concerns can't be added in one place.
**Mechanism (why it's fragile):** communication is *direct point-to-point references*, so the dependency graph is a dense mesh with no central authority — the very thing Mediator replaces with a star.

---

### Variant 1: Basic Mediator Interface + ConcreteMediator + Colleague Base

**What problem it solves:** Introduce a `ChatMediator` interface and a `ChatRoom` concrete mediator. Users no longer reference each other — each holds a reference only to the mediator. To send, a user calls `mediator.send(msg, this)`; the room owns the routing loop and delivers to *everyone except the sender*. Coupling drops from `O(n²)` to `O(n)` (each user ↔ the room). The routing logic now lives in exactly one place, so adding logging or a new delivery rule is a one-file change.

<details>
<summary>💻 Click to expand code — basic mediator (classic chat room)</summary>

```java
import java.util.*;

// Mediator interface — the contract colleagues depend on instead of each other.
interface ChatMediator {
    void register(User user);
    void send(String message, User sender);
}

// Colleague base — every colleague knows the mediator, NOT its siblings.
abstract class User {
    protected final ChatMediator mediator;
    protected final String name;
    User(ChatMediator mediator, String name) { this.mediator = mediator; this.name = name; }
    abstract void send(String message);
    abstract void receive(String message, String from);
    String name() { return name; }
}

// ConcreteMediator — owns the colleague registry AND the interaction logic.
class ChatRoom implements ChatMediator {
    private final List<User> users = new ArrayList<>();
    public void register(User user) { users.add(user); }
    public void send(String message, User sender) {
        // The routing rule lives in ONE place: deliver to everyone except the sender.
        for (User u : users) {
            if (u != sender) u.receive(message, sender.name());
        }
    }
}

// Concrete colleague — talks only to the mediator.
class ChatUser extends User {
    ChatUser(ChatMediator mediator, String name) { super(mediator, name); }
    void send(String message)  { System.out.println(name + " sends: " + message); mediator.send(message, this); }
    void receive(String message, String from) { System.out.println("  " + name + " <- " + from + ": " + message); }
}

// ---------- Usage demo ----------
class ChatDemoV1 {
    public static void main(String[] args) {
        ChatMediator room = new ChatRoom();                 // the hub
        User alice = new ChatUser(room, "Alice");
        User bob   = new ChatUser(room, "Bob");
        User carol = new ChatUser(room, "Carol");

        // Each user is registered with the room ONCE — no peer-to-peer wiring.
        room.register(alice); room.register(bob); room.register(carol);

        alice.send("Hi everyone");   // room fans out to Bob + Carol (not Alice)
        bob.send("Hey Alice");        // room fans out to Alice + Carol
        // Add a 4th user? register() it once. Want logging? edit ChatRoom.send() only.
    }
}
```
</details>

**Code walkthrough:** `ChatUser.send` no longer loops over peers — it just hands the message to the room. `ChatRoom.send` is the single home of the routing rule ("everyone except sender"). Adding a fourth user is one `register()` call, not four `connect()` calls. If you now want to log every message or drop profanity, you edit `ChatRoom.send` alone — the users are untouched and stay reusable. The users depend only on the `ChatMediator` interface, so you could swap in a `ModeratedChatRoom` without changing a single user.

**Pros:** Coupling collapses from `O(n²)` to `O(n)`; users don't reference each other and become reusable; routing/interaction logic is centralized in one testable class; adding a colleague is a single `register()`; cross-cutting concerns (logging, moderation) go in one place.
**Cons:** The mediator now depends on the concrete `User` API (`receive`); the `send(msg, sender)` signature is chat-specific (not yet a general notification); the mediator can start accreting logic — the seed of the god-object problem.
**Mechanism:** *indirection through the mediator interface* — colleagues call `mediator.send(...)` and the mediator dispatches, so the only edges in the graph are colleague→mediator. The interaction protocol is reified as a single object.

---

### Variant 2: Mediator with an Event/Notification Method

**What problem it solves:** Variant 1's mediator had a chat-specific `send` method. Real dialogs/hubs coordinate *heterogeneous* colleagues reacting to *different kinds of events* — a widget was clicked, a value changed, a user joined. The idiomatic GoF form gives the mediator a **single generic `notify(sender, event)` method**: a colleague reports *what happened to it*, and the mediator decides *how everyone else should react*. This is the classic "dialog director" shape. We'll switch domains to a **login dialog** (username field, password field, login button, status label) so the notification style is obvious — the mediator enables the button only when both fields are non-empty.

<details>
<summary>💻 Click to expand code — notification-style mediator (login dialog)</summary>

```java
// Mediator: ONE generic notification entry point.
interface DialogMediator {
    void notify(Component sender, String event);
}

// Colleague base — holds the mediator, reports events to it.
abstract class Component {
    protected DialogMediator mediator;
    void setMediator(DialogMediator m) { this.mediator = m; }
}

// Concrete colleagues — each reports its own events; none know the others.
class TextField extends Component {
    private final String label;
    private String text = "";
    TextField(String label) { this.label = label; }
    void type(String value) {
        this.text = value;
        System.out.println(label + " field = '" + value + "'");
        mediator.notify(this, "textChanged");     // tell the mediator; don't touch other widgets
    }
    boolean isEmpty() { return text.isEmpty(); }
    String label() { return label; }
}

class Button extends Component {
    private final String label;
    private boolean enabled = false;
    Button(String label) { this.label = label; }
    void setEnabled(boolean e) { this.enabled = e; System.out.println(label + " button " + (e ? "ENABLED" : "disabled")); }
    void click() {
        if (!enabled) { System.out.println("(" + label + " is disabled — click ignored)"); return; }
        mediator.notify(this, "clicked");
    }
}

class Label extends Component {
    void setText(String t) { System.out.println("Status: " + t); }
}

// ConcreteMediator — the interaction rules live here, keyed on (sender, event).
class LoginDialog implements DialogMediator {
    private final TextField username = new TextField("Username");
    private final TextField password = new TextField("Password");
    private final Button    login    = new Button("Login");
    private final Label     status   = new Label();

    LoginDialog() {
        for (Component c : new Component[]{username, password, login, status}) c.setMediator(this);
    }

    public void notify(Component sender, String event) {
        // Rule 1: whenever a field changes, (re)compute whether Login should be enabled.
        if (event.equals("textChanged")) {
            boolean ready = !username.isEmpty() && !password.isEmpty();
            login.setEnabled(ready);
        }
        // Rule 2: when Login is clicked, run the login flow and update the status label.
        else if (sender == login && event.equals("clicked")) {
            status.setText("Authenticating...");
            // (call auth service) ...
            status.setText("Welcome!");
        }
    }

    // expose widgets for the demo
    TextField username() { return username; }
    TextField password() { return password; }
    Button    login()    { return login; }
}

// ---------- Usage demo ----------
class DialogDemoV2 {
    public static void main(String[] args) {
        LoginDialog dialog = new LoginDialog();

        dialog.login().click();              // (Login is disabled — click ignored)
        dialog.username().type("alice");     // mediator: only one field filled -> stays disabled
        dialog.password().type("s3cret");    // mediator: both filled -> Login ENABLED
        dialog.login().click();              // Authenticating... / Welcome!
    }
}
```
</details>

**Code walkthrough:** The single `notify(sender, event)` method is the whole interaction protocol. A `TextField` doesn't know the `Button` exists — it just reports `"textChanged"`, and `LoginDialog.notify` decides that a change means "recompute the button's enabled state." The rule "Login is enabled iff both fields are non-empty" lives in exactly one place; if product wants to add "and the username is a valid email," you edit only the mediator. Widgets are pure, reusable UI primitives — the same `TextField` class works in any dialog. This is the exact structure GoF's `FontDialogDirector` uses.

**Pros:** A single generic `notify` decouples colleagues completely — they emit events without knowing who reacts; heterogeneous colleagues coordinate through one protocol; all cross-widget rules live in the mediator; colleagues are fully reusable primitives.
**Cons:** `notify` becomes a big `if/else` on `(sender, event)` that can grow unwieldy (god-object smell); using `String` event names is stringly-typed and error-prone; the mediator still references concrete colleague types.
**Mechanism:** *event-notification indirection* — colleagues push "what happened to me" to the mediator, which owns the reaction logic. The colleagues are now sources of events, not drivers of behavior; the mediator is the single decision-maker.

---

### Variant 3: Decoupled, Event-Driven Mediator (Typed Events, No Concrete Coupling)

**What problem it solves:** Variant 2 still had two weaknesses: colleagues were coupled to a *concrete* mediator idea, and events were `String`s (typos compile fine, then fail at runtime). Here we (a) make colleagues depend only on a narrow **`EventPublisher` interface** so they don't know the concrete mediator type at all, and (b) replace stringly-typed events with **typed event objects / an enum**, so the compiler checks them and the mediator can pattern-match. This is the bridge from "classic GoF Mediator" toward "event bus." Colleagues publish typed events; the mediator subscribes handlers per event type. We return to the chat domain and add richer events (`JOIN`, `MESSAGE`, `LEAVE`).

<details>
<summary>💻 Click to expand code — typed, decoupled event-driven mediator (chat)</summary>

```java
import java.util.*;

// Typed events — no stringly-typed keys. A sealed hierarchy the mediator can switch on.
sealed interface ChatEvent permits ChatEvent.Join, ChatEvent.Message, ChatEvent.Leave {
    String user();
    record Join(String user)                  implements ChatEvent {}
    record Message(String user, String text)  implements ChatEvent {}
    record Leave(String user)                 implements ChatEvent {}
}

// Narrow interface colleagues depend on — they know only "I can publish an event".
interface EventPublisher {
    void publish(ChatEvent event);
}

// Colleague — depends ONLY on EventPublisher, never on ChatRoom's concrete type.
class Participant {
    private final String name;
    private final EventPublisher hub;
    private final Deque<String> inbox = new ArrayDeque<>();
    Participant(String name, EventPublisher hub) { this.name = name; this.hub = hub; }

    void join()               { hub.publish(new ChatEvent.Join(name)); }
    void say(String text)     { hub.publish(new ChatEvent.Message(name, text)); }
    void leave()              { hub.publish(new ChatEvent.Leave(name)); }

    // The mediator calls this to deliver; the participant doesn't know who sent it.
    void deliver(String line) { inbox.add(line); System.out.println("  [" + name + "] " + line); }
    String name() { return name; }
}

// ConcreteMediator — subscribes reactions per typed event; owns membership + routing.
class ChatRoomHub implements EventPublisher {
    private final Map<String, Participant> members = new LinkedHashMap<>();

    void enter(Participant p) { members.put(p.name(), p); }

    public void publish(ChatEvent event) {
        // Typed dispatch — the compiler ensures we handle every ChatEvent subtype.
        switch (event) {
            case ChatEvent.Join j    -> broadcast(j.user(), j.user() + " joined the room");
            case ChatEvent.Message m -> broadcast(m.user(), m.user() + ": " + m.text());
            case ChatEvent.Leave l   -> { broadcast(l.user(), l.user() + " left the room"); members.remove(l.user()); }
        }
    }

    // Interaction rule in ONE place: deliver to everyone except the originator.
    private void broadcast(String from, String line) {
        for (Participant p : members.values())
            if (!p.name().equals(from)) p.deliver(line);
    }
}

// ---------- Usage demo ----------
class ChatDemoV3 {
    public static void main(String[] args) {
        ChatRoomHub hub = new ChatRoomHub();
        Participant alice = new Participant("Alice", hub);
        Participant bob   = new Participant("Bob", hub);
        Participant carol = new Participant("Carol", hub);
        hub.enter(alice); hub.enter(bob); hub.enter(carol);

        alice.join();            // Bob + Carol see "Alice joined the room"
        alice.say("Hi all!");    // Bob + Carol see "Alice: Hi all!"
        bob.leave();             // Alice + Carol see "Bob left the room"; Bob removed
        carol.say("Bye Bob");    // only Alice sees it now
    }
}
```
</details>

**Code walkthrough:** A `Participant` knows only `EventPublisher.publish` — it has no idea `ChatRoomHub` exists, so you could hand it a test double, a networked publisher, or a Kafka-backed hub without changing the participant. Events are a `sealed interface`, so the `switch` in `publish` is *exhaustive*: add a `ChatEvent.PrivateMessage` and the compiler flags every switch that doesn't handle it (the same completeness guarantee the State pattern got from enum abstract methods). The routing rule still lives in exactly one place (`broadcast`). This variant is the hinge: it's still a single mediator with centralized logic, but the *publish typed event → mediator reacts* shape is one small step from a full publish/subscribe event bus.

**Pros:** Colleagues depend only on a narrow publisher interface — zero coupling to the concrete mediator; typed events give compile-time safety and exhaustive dispatch; easy to test colleagues with a mock publisher; naturally extends to networked/async delivery.
**Cons:** More types to define (event hierarchy); still a single hub owning all reactions (god-object risk remains if events multiply); the `switch` grows with event types unless you move to per-type subscribers (Variant 4).
**Mechanism:** *typed-event publish + interface-narrowed colleagues* — colleagues are pure event sources bound only to `EventPublisher`; the mediator pattern-matches typed events and owns all reactions. This is Mediator with the dial turned toward decoupling.

---

### Variant 4: Production-Grade — Event Bus / Message-Broker-Style Mediator

**Why it's recommended:** In a real system the mediator shouldn't hard-code *who reacts to what* — that just moves the god-object into one giant `switch`. The production form is a **publish/subscribe event bus**: colleagues (publishers) emit typed events to the bus; other colleagues (subscribers) register handlers for the event *types* they care about; the bus routes events to matching handlers. Publishers and subscribers are now **fully mutually anonymous** — neither knows the other exists. This is the natural end of the spectrum:

> **Mediator → Event Bus → Message Broker.**
> A **Mediator** knows its colleagues and contains the interaction logic (star topology, logic in the hub). An **Event Bus** (in-process pub/sub) generalizes the mediator so the *routing is generic* (by event type) and the *logic moves into subscribers* — the bus is a "dumb" mediator. A **Message Broker** (Kafka, RabbitMQ, SNS/SQS) is an event bus that is out-of-process, durable, and distributed, adding persistence, delivery guarantees, and topics/partitions. All three are the same idea — decouple senders from receivers via an intermediary — at increasing scale and infrastructure weight.

<details>
<summary>💻 Click to expand code — production event bus (typed pub/sub mediator with async + error isolation)</summary>

```java
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

// A generic, type-safe in-process event bus — a "dumb mediator" that routes by event type.
class EventBus {
    // event type -> list of subscribers interested in it
    private final Map<Class<?>, List<Consumer<Object>>> subscribers = new ConcurrentHashMap<>();
    private final Executor executor;   // sync (Runnable::run) or a thread pool for async

    EventBus(Executor executor) { this.executor = executor; }

    // Subscribe a handler for a specific event type. Publisher & subscriber never meet.
    @SuppressWarnings("unchecked")
    <E> void subscribe(Class<E> type, Consumer<E> handler) {
        subscribers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                   .add((Consumer<Object>) handler);
    }

    // Publish an event — the bus fans it out to every subscriber of that type.
    <E> void publish(E event) {
        List<Consumer<Object>> handlers = subscribers.getOrDefault(event.getClass(), List.of());
        for (Consumer<Object> h : handlers) {
            executor.execute(() -> {
                try { h.accept(event); }
                catch (RuntimeException ex) {   // one bad subscriber must not break the others
                    System.out.println("subscriber failed for " + event.getClass().getSimpleName()
                                       + ": " + ex.getMessage());
                }
            });
        }
    }
}

// Domain events (immutable value objects).
record UserRegistered(String email) {}
record OrderPlaced(String orderId, long amountCents) {}

// ---------- Usage demo ----------
class EventBusDemo {
    public static void main(String[] args) {
        EventBus bus = new EventBus(Runnable::run);   // synchronous for a deterministic demo

        // Multiple independent subscribers react to the SAME event — none know each other.
        bus.subscribe(UserRegistered.class, e -> System.out.println("Email service: welcome " + e.email()));
        bus.subscribe(UserRegistered.class, e -> System.out.println("Analytics: signup " + e.email()));
        bus.subscribe(UserRegistered.class, e -> { throw new RuntimeException("CRM down"); }); // fails in isolation

        bus.subscribe(OrderPlaced.class, e -> System.out.println("Fulfillment: pack " + e.orderId()));
        bus.subscribe(OrderPlaced.class, e -> System.out.println("Billing: charge " + e.amountCents() + "c"));

        // Publishers just emit — they don't know or care who is listening.
        bus.publish(new UserRegistered("alice@x.com"));
        //   Email service: welcome alice@x.com
        //   Analytics: signup alice@x.com
        //   subscriber failed for UserRegistered: CRM down     (isolated — others still ran)
        bus.publish(new OrderPlaced("ORD-1", 4999));
        //   Fulfillment: pack ORD-1
        //   Billing: charge 4999c
    }
}
```
</details>

**Code walkthrough:** The bus is a *generic* mediator: it holds no domain logic at all — just a `Map<eventType → handlers>` and a fan-out loop. Publishers call `publish(event)`; the bus looks up handlers by the event's runtime type and invokes each. A publisher of `UserRegistered` has no reference to the email service, analytics, or CRM — you can add a fourth subscriber (fraud check) without touching any publisher. Swapping the `Executor` from `Runnable::run` to a thread pool makes delivery asynchronous with zero changes to publishers/subscribers. The try/catch around each handler is the crucial production detail: **one failing subscriber (CRM down) is isolated** so it can't break delivery to the others — the analog of a broker not losing a message because one consumer crashed. This is exactly Guava's `EventBus`, Spring's `ApplicationEventPublisher`, and (out-of-process) Kafka's producer/consumer model.

**Pros:** Publishers and subscribers are *fully* decoupled (mutually anonymous); routing is generic (by type) so the hub carries no domain logic — the god-object is dissolved; trivially extensible (add a subscriber, touch nothing else); sync/async is a one-line swap; error isolation per subscriber; maps directly onto real frameworks and brokers.
**Cons:** Control flow becomes *implicit* — hard to trace "what happens when I publish X" without tooling (the flip side of decoupling); ordering/delivery guarantees need care (especially async); a "dumb" bus loses the explicit, readable interaction protocol a classic mediator gives you; debugging and testing end-to-end flows is harder; can encourage event spaghetti if overused.
**Mechanism:** *type-keyed publish/subscribe* — the intermediary routes events to handlers registered by type, so senders and receivers share no references. The interaction logic that a classic mediator centralized is now *distributed into subscribers*, and the hub is a pure router. This is the same "decouple via an intermediary" idea as Mediator, generalized to `n` anonymous publishers and `m` anonymous subscribers.

---

## 🎨 Real-World Example

Below are **three** complete, production-style scenarios. Read them together: they show the *same* Mediator pattern coordinating very different systems, so you can see what stays constant (colleagues know only the hub, all interaction logic lives in the mediator, the mesh becomes a star) and what changes per domain (broadcast vs. authoritative control vs. workflow orchestration). Each is self-contained and collapsible.

- **Scenario 1 — Chat Room Server**: a broadcast mediator with membership, private messages, and moderation — the canonical Mediator.
- **Scenario 2 — Air Traffic Control Tower**: an *authoritative* mediator that arbitrates a shared resource (the runway) so colleagues never coordinate directly.
- **Scenario 3 — Order Coordination Hub**: an event-driven mediator that orchestrates a multi-service workflow (payment, inventory, shipping, notification) — Mediator as a saga orchestrator.

### Scenario 1 — Chat Room Server

**The problem:** A chat server hosts rooms of users. Each message from one user must reach the *other* users in the room; users join and leave dynamically; the room supports **private (whispered) messages** to a single recipient, **moderation** (a banned word filter and the ability to mute a user), and a **system announcement** channel. Doing this peer-to-peer would be an `O(n²)` mesh where every user routes and filters messages itself. The `ChatRoom` mediator is the single authority: users send to the room, and the room decides who receives what, applying moderation centrally.

**The design:** `ChatRoom` (mediator) owns the member registry, the mute set, and the banned-word filter. `User` (colleague) holds only a reference to the room. All routing, filtering, and membership logic lives in the mediator; users are dumb endpoints that `send` and `receive`.

<details>
<summary>💻 Click to expand Scenario 1 — full code (chat room with broadcast, private msg, moderation, demo)</summary>

```java
import java.util.*;

// ---------- Mediator ----------
interface ChatMediator {
    void addUser(User user);
    void removeUser(User user);
    void broadcast(String text, User sender);
    void whisper(String text, User sender, String toName);   // private message
}

// ---------- Colleague ----------
class User {
    private final String name;
    private final ChatMediator room;
    private final List<String> history = new ArrayList<>();
    User(String name, ChatMediator room) { this.name = name; this.room = room; }

    void send(String text)                 { room.broadcast(text, this); }
    void sendPrivate(String text, String to){ room.whisper(text, this, to); }
    void receive(String line)              { history.add(line); System.out.println("  [" + name + "] " + line); }
    String name() { return name; }
}

// ---------- ConcreteMediator: owns ALL interaction logic ----------
class ChatRoom implements ChatMediator {
    private final String roomName;
    private final Map<String, User> members = new LinkedHashMap<>();
    private final Set<String> muted = new HashSet<>();
    private final Set<String> bannedWords = Set.of("spam", "scam");

    ChatRoom(String roomName) { this.roomName = roomName; }

    public void addUser(User user) {
        members.put(user.name(), user);
        systemAnnounce(user.name() + " joined " + roomName);   // side effect owned by the mediator
    }
    public void removeUser(User user) {
        members.remove(user.name());
        systemAnnounce(user.name() + " left " + roomName);
    }

    public void broadcast(String text, User sender) {
        if (isMuted(sender)) return;                            // moderation: muted users are silenced
        String clean = filter(text);                            // moderation: banned-word filter
        String line = sender.name() + ": " + clean;
        for (User u : members.values())
            if (u != sender) u.receive(line);                   // routing rule: everyone except sender
    }

    public void whisper(String text, User sender, String toName) {
        if (isMuted(sender)) return;
        User target = members.get(toName);
        if (target == null) { sender.receive("(no such user: " + toName + ")"); return; }
        target.receive("(private) " + sender.name() + ": " + filter(text));
    }

    // ----- moderation helpers, centralized in the mediator -----
    void mute(String userName)   { muted.add(userName); }
    private boolean isMuted(User u) { return muted.contains(u.name()); }
    private String filter(String text) {
        String out = text;
        for (String w : bannedWords) out = out.replaceAll("(?i)" + w, "***");
        return out;
    }
    private void systemAnnounce(String msg) {
        for (User u : members.values()) u.receive("* " + msg);
    }
}

// ---------- Demo ----------
public class ChatServerDemo {
    public static void main(String[] args) {
        ChatRoom room = new ChatRoom("#general");
        User alice = new User("Alice", room);
        User bob   = new User("Bob",   room);
        User carol = new User("Carol", room);

        room.addUser(alice);   // * Alice joined #general (only Alice present)
        room.addUser(bob);     // Alice sees "* Bob joined #general"
        room.addUser(carol);   // Alice, Bob see "* Carol joined #general"

        // Broadcast: reaches everyone except the sender.
        alice.send("Hello team");        // Bob + Carol get "Alice: Hello team"

        // Moderation: banned words are filtered centrally.
        bob.send("buy my spam link");    // Alice + Carol get "Bob: buy my *** link"

        // Private message: only the target receives it.
        carol.sendPrivate("psst, meeting at 3", "Alice");  // only Alice gets "(private) Carol: ..."

        // Mute Bob, then he tries to talk — the mediator silences him.
        room.mute("Bob");
        bob.send("anyone there?");       // dropped by the mediator; no one receives it

        // Leave: membership + announcement handled by the mediator.
        room.removeUser(carol);          // Alice + Bob see "* Carol left #general"
    }
}
```
</details>

**Code walkthrough:** Every rule that governs "how users interact" — broadcast routing, private-message targeting, the banned-word filter, muting, and join/leave announcements — lives inside `ChatRoom`. A `User` has no reference to any other `User`; it only calls `room.broadcast` / `room.whisper`. That's why moderation is trivial: `filter` and `isMuted` are applied in one place, on every path, and adding a new rule (rate-limiting, a slow-mode) is a change to the mediator alone. If this were peer-to-peer, the filter would have to be copy-pasted into every user's send path and would inevitably drift.

**Why this is a strong FAANG answer:** it's the canonical Mediator, and it shows the pattern earning its keep: the `O(n²)` mesh becomes an `O(n)` star, and cross-cutting concerns (moderation, announcements, private routing) are centralized in the hub where they belong — while users stay dumb, reusable endpoints. It also naturally sets up the god-object discussion: as features pile on, `ChatRoom` is where they land, so you'd eventually split moderation into its own collaborator the mediator delegates to.

### Scenario 2 — Air Traffic Control Tower

**The problem:** Several aircraft operate near one airport with a **single runway**. Planes must land and take off without colliding, but they must **never coordinate with each other directly** — that's how mid-air conflicts happen. Instead, every aircraft communicates only with the **control tower**, which is the sole authority on the shared resource: it grants or denies runway access, queues planes that must wait, and clears the next plane when the runway frees up. This is Mediator in its *authoritative arbiter* form: unlike the chat room (which broadcasts), the tower makes **decisions** and issues **commands** to specific colleagues.

**The design:** `ControlTower` (mediator) owns the runway state and a landing queue. `Aircraft` (colleague) holds only a reference to the tower and reports its intentions (`requestLanding`, `notifyRunwayCleared`); it never references another aircraft. The tower serializes access to the runway and drives the next aircraft when the runway is freed.

<details>
<summary>💻 Click to expand Scenario 2 — full code (ATC tower arbitrating a shared runway, demo)</summary>

```java
import java.util.*;

// ---------- Mediator ----------
interface AirTrafficControl {
    void registerFlight(Aircraft aircraft);
    void requestLanding(Aircraft aircraft);
    void runwayVacated(Aircraft aircraft);   // a plane reports it has cleared the runway
}

// ---------- Colleague ----------
class Aircraft {
    private final String flightId;
    private final AirTrafficControl tower;   // knows ONLY the tower, never other aircraft
    Aircraft(String flightId, AirTrafficControl tower) { this.flightId = flightId; this.tower = tower; }

    void requestLanding() {
        System.out.println(flightId + ": requesting landing clearance");
        tower.requestLanding(this);
    }
    // The tower calls these to command the aircraft.
    void land()           { System.out.println("  " + flightId + ": CLEARED to land — landing now"); tower.runwayVacated(this); }
    void hold()           { System.out.println("  " + flightId + ": HOLD — entering holding pattern"); }
    String flightId()     { return flightId; }
}

// ---------- ConcreteMediator: the single authority on the shared runway ----------
class ControlTower implements AirTrafficControl {
    private final Set<Aircraft> flights = new LinkedHashSet<>();
    private final Queue<Aircraft> landingQueue = new LinkedList<>();
    private Aircraft runwayOccupiedBy = null;      // the shared resource's state

    public void registerFlight(Aircraft aircraft) { flights.add(aircraft); }

    public void requestLanding(Aircraft aircraft) {
        if (runwayOccupiedBy == null) {
            grantRunway(aircraft);                 // runway free -> clear immediately
        } else {
            landingQueue.add(aircraft);            // busy -> queue and tell it to hold
            System.out.println("  Tower: runway busy (" + runwayOccupiedBy.flightId()
                               + " on it) -> " + aircraft.flightId() + " queued at position " + landingQueue.size());
            aircraft.hold();
        }
    }

    public void runwayVacated(Aircraft aircraft) {
        System.out.println("  Tower: " + aircraft.flightId() + " has cleared the runway");
        runwayOccupiedBy = null;
        // Drive the next queued aircraft — colleagues never do this themselves.
        Aircraft next = landingQueue.poll();
        if (next != null) {
            System.out.println("  Tower: clearing next in queue -> " + next.flightId());
            grantRunway(next);
        }
    }

    private void grantRunway(Aircraft aircraft) {
        runwayOccupiedBy = aircraft;
        aircraft.land();                           // authoritative command to a specific colleague
    }
}

// ---------- Demo ----------
public class AirTrafficControlDemo {
    public static void main(String[] args) {
        ControlTower tower = new ControlTower();
        Aircraft ba100 = new Aircraft("BA100", tower);
        Aircraft ua200 = new Aircraft("UA200", tower);
        Aircraft dl300 = new Aircraft("DL300", tower);
        tower.registerFlight(ba100); tower.registerFlight(ua200); tower.registerFlight(dl300);

        ba100.requestLanding();   // runway free -> BA100 CLEARED, lands, vacates -> queue empty
        // Now two planes contend for the runway while it's occupied:
        ua200.requestLanding();   // runway free again -> UA200 CLEARED to land, then vacates
        // Simulate contention: request before the previous vacates by interleaving in real systems.
        // Here each landing completes synchronously, so let's show queuing explicitly:
        System.out.println("--- contention ---");
        ControlTower busy = new ControlTower();
        Aircraft f1 = new Aircraft("F1", busy), f2 = new Aircraft("F2", busy), f3 = new Aircraft("F3", busy);
        busy.registerFlight(f1); busy.registerFlight(f2); busy.registerFlight(f3);
        // Manually occupy the runway, then queue two more:
        busy.requestLanding(f1); // F1 lands + vacates immediately in this simple model
        // To demonstrate the queue we'd hold the runway; see note below.
    }
}
```
</details>

**Code walkthrough:** The tower is the *only* object that knows the runway's state (`runwayOccupiedBy`) and the landing queue. An `Aircraft` never asks another aircraft "are you using the runway?" — it asks the tower, which arbitrates. When a plane vacates, the tower — not the plane — decides who goes next by polling the queue and issuing `land()` to that specific colleague. This is the key difference from the chat room: the mediator here is an **authoritative controller** issuing targeted commands, not a broadcaster. It's also why the pattern is safety-critical here: centralizing runway arbitration in one authority is exactly how you *prevent* the `O(n²)` peer negotiation that would risk collisions. (In a real system, landings are asynchronous — the tower would hold the runway until an aircraft reports `runwayVacated` after actually landing; the queue then genuinely fills. The synchronous demo keeps the control flow readable.)

**Why this is a strong FAANG answer:** it demonstrates Mediator arbitrating a **shared resource** among colleagues that must not coordinate directly — a distinct and powerful use beyond broadcasting. It shows the mediator holding authoritative state (runway occupancy), serializing access, queuing, and issuing *targeted commands* to specific colleagues, which is the shape of resource schedulers, lock managers, and connection pools. It also gives you a crisp answer to "isn't the mediator a single point of failure?" — yes, and in ATC that centralization is a feature (one authority), which segues into the SPOF/bottleneck discussion at scale.

### Scenario 3 — Order Coordination Hub (Event-Driven)

**The problem:** Placing an order touches four services — **payment** (charge the card), **inventory** (reserve stock), **shipping** (schedule a delivery), and **notification** (email the customer). If each service called the next directly, you'd get a brittle chain where inventory knows about shipping, shipping knows about notification, and changing the flow means editing every service. Worse, if a later step fails (shipping can't schedule), earlier steps must be **compensated** (refund the payment, release the reserved stock). An **order coordination hub** (a mediator acting as a *saga orchestrator*) owns the workflow: services report outcomes to the hub, and the hub drives the next step or the rollback.

**The design:** `OrderCoordinator` (mediator) owns the workflow steps and the compensation logic. Each service (colleague) exposes a simple operation and reports success/failure back to the coordinator via typed events — services never call each other. This is Mediator scaled up to microservice orchestration, and it's the bridge to the "orchestration vs. choreography" Staff-level discussion.

<details>
<summary>💻 Click to expand Scenario 3 — full code (order coordinator / saga orchestrator with compensation, demo)</summary>

```java
import java.util.*;

// ---------- Colleagues: independent services that DON'T know each other ----------
class PaymentService {
    String charge(String orderId, long cents) { System.out.println("Payment: charged " + cents + "c for " + orderId); return "PAY-" + orderId; }
    void refund(String paymentId)             { System.out.println("Payment: refunded " + paymentId); }
}
class InventoryService {
    boolean fail = false;
    String reserve(String orderId, int qty)   { if (fail) throw new RuntimeException("out of stock");
                                                 System.out.println("Inventory: reserved " + qty + " for " + orderId); return "RES-" + orderId; }
    void release(String reservationId)         { System.out.println("Inventory: released " + reservationId); }
}
class ShippingService {
    boolean fail = false;
    String schedule(String orderId)            { if (fail) throw new RuntimeException("no couriers available");
                                                 System.out.println("Shipping: scheduled delivery for " + orderId); return "SHIP-" + orderId; }
}
class NotificationService {
    void notify(String orderId, String msg)    { System.out.println("Notify: order " + orderId + " -> " + msg); }
}

// ---------- ConcreteMediator: the saga orchestrator ----------
class OrderCoordinator {
    private final PaymentService payment;
    private final InventoryService inventory;
    private final ShippingService shipping;
    private final NotificationService notifier;

    OrderCoordinator(PaymentService p, InventoryService i, ShippingService s, NotificationService n) {
        this.payment = p; this.inventory = i; this.shipping = s; this.notifier = n;
    }

    // The ENTIRE workflow — and its rollback — lives in ONE place.
    void placeOrder(String orderId, int qty, long cents) {
        String paymentId = null, reservationId = null;
        try {
            paymentId     = payment.charge(orderId, cents);      // step 1
            reservationId = inventory.reserve(orderId, qty);     // step 2
            shipping.schedule(orderId);                          // step 3
            notifier.notify(orderId, "confirmed");               // step 4
            System.out.println("Order " + orderId + " COMPLETED");
        } catch (RuntimeException ex) {
            System.out.println("Order " + orderId + " FAILED (" + ex.getMessage() + ") -> compensating");
            // Compensate completed steps in REVERSE order (same discipline as a saga).
            if (reservationId != null) inventory.release(reservationId);
            if (paymentId != null)     payment.refund(paymentId);
            notifier.notify(orderId, "failed: " + ex.getMessage());
        }
    }
}

// ---------- Demo ----------
public class OrderHubDemo {
    public static void main(String[] args) {
        PaymentService payment = new PaymentService();
        InventoryService inventory = new InventoryService();
        ShippingService shipping = new ShippingService();
        NotificationService notifier = new NotificationService();
        OrderCoordinator hub = new OrderCoordinator(payment, inventory, shipping, notifier);

        // Happy path: all four steps run in order.
        hub.placeOrder("ORD-1", 2, 4999);
        System.out.println("----");

        // Failure at shipping: payment + inventory are compensated in reverse.
        shipping.fail = true;
        hub.placeOrder("ORD-2", 1, 8800);
        // Output: charge -> reserve -> shipping throws -> release reservation -> refund payment -> notify failed
    }
}
```
</details>

**Code walkthrough:** The four services are mutually ignorant — `PaymentService` has no idea `ShippingService` exists. The *workflow* (charge → reserve → schedule → notify) and the *compensation* (release → refund on failure) live entirely in `OrderCoordinator`. This is the microservice analog of the GUI dialog director: one place describes the collaboration, so changing the flow (insert a fraud check between payment and inventory) is a one-file edit, and each service stays independently deployable and testable. The reverse-order compensation is the saga discipline — undo later steps before earlier ones because later steps may depend on earlier ones.

**Why this is a strong FAANG answer:** it shows Mediator as the backbone of **microservice orchestration / the saga orchestrator pattern**, the go-to answer for "how do you coordinate a multi-service transaction without a distributed lock." It demonstrates the workflow living in one auditable place, services decoupled from each other, and compensating rollback — and it directly frames the Staff-level debate of **orchestration (a central mediator drives the flow) vs. choreography (services react to each other's events with no central hub)**, which is Mediator vs. Event-Bus at architecture scale.

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- A **set of objects communicate in complex, well-defined ways**, and the resulting interdependencies are unstructured and hard to understand — a mesh you want to turn into a star.
- **Reusing a colleague is hard** because it references and depends on many other objects — extracting the coordination into a mediator frees the colleague.
- A **behavior distributed across several classes** should be customizable or varied **without a lot of subclassing** — put the varying interaction in one mediator you can swap or edit.
- You have **many-to-many relationships** among objects and want to centralize control (a chat room, a dialog of interdependent widgets, a smart-home hub coordinating devices).
- You need a **single authority over a shared resource** that colleagues must not contend for directly (runway, connection pool, lock manager).
- You want to **orchestrate a multi-step workflow** across independent components/services in one auditable place (a saga orchestrator).

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- **Few objects with simple, stable interactions.** Two or three objects that rarely change don't justify a hub; a direct reference is clearer.
- **The interaction is genuinely one-directional or trivial.** If A just calls B, you don't need a mediator between them.
- **You'd be creating a god object.** If the coordination logic is so large and varied that the mediator becomes an unmaintainable do-everything class, reconsider — split into multiple focused mediators, move logic back to colleagues, or use a pub/sub bus.
- **You need maximum decoupling with dynamic, anonymous many-to-many delivery.** A publish/subscribe **event bus** (or a real message broker) may fit better than a classic mediator that knows all its colleagues.
- **Broadcasting where a mediator adds nothing.** If all you need is "notify observers of a state change," plain **Observer** is lighter than a full mediator.
- **Over-centralization risk in distributed systems.** A single orchestrator can become a bottleneck / single point of failure; choreography may be more scalable for some flows.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- **Reduces coupling** from a potential `O(n²)` mesh to an `O(n)` star — colleagues depend only on the mediator, not on each other.
- **Single Responsibility** — the interaction protocol lives in one place, so it's easy to understand, test, and change.
- **Open/Closed for the interaction** — you can vary how colleagues collaborate by editing/swapping the mediator without touching the colleagues.
- **Colleagues become reusable** — a widget/service no longer drags its siblings along.
- **Centralizes cross-cutting concerns** (logging, moderation, auditing, arbitration) at the hub.
- **Simplifies object protocols** — many-to-many is replaced by one-to-many between mediator and colleagues.

**Cons**

- **God-object risk (the headline con)** — the mediator can grow into a bloated, hard-to-maintain class that concentrates all complexity. You trade a distributed-complexity problem for a centralized one.
- **Single point of failure / bottleneck** — in distributed use, the hub can become a SPOF and a scaling chokepoint.
- **Indirection** — control flow goes through the mediator, which can obscure "what happens when X occurs," especially in event-bus form.
- **Mediator can become coupled to concrete colleagues** unless you use interfaces/typed events.
- **Can hide poor design** — sometimes a mediator is papering over objects that shouldn't be so entangled in the first place.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

Mediator is most often confused with **Observer** (both decouple communicating objects) and **Facade** (both introduce a central object). The distinctions are about **intent and topology** — and Mediator vs. Observer is one of the highest-value interview discriminators.

| Pattern | Intent | Topology | Who knows whom | Key tell |
|---|---|---|---|---|
| **Mediator** | Encapsulate *how a set of objects interact*; centralize many-to-many coordination | Star — colleagues ↔ one hub | Colleagues know the mediator; mediator knows colleagues | Bidirectional coordination logic lives in the hub |
| **Observer** | Notify many dependents when *one subject* changes state | One subject → many observers | Observers subscribe to a subject | One-way broadcast on state change; no central coordinator of *interactions* |
| **Facade** | Provide a *simplified interface* to a subsystem | Client → facade → subsystem | Facade knows the subsystem; subsystem doesn't know the facade | One-directional simplification; subsystem objects still call each other |
| **Command** | Encapsulate a *request* as an object (queue/undo/log) | Invoker → command → receiver | Invoker knows only the command interface | Reifies an action; a command bus is mediator-like routing |
| **Event Bus / Broker** | Route events between *anonymous* publishers and subscribers | Many publishers ↔ hub ↔ many subscribers | Neither side knows the other | Generic type-routed pub/sub; the "dumb mediator" |

**Mediator vs. Observer (the money question):** Observer is *one-directional broadcast* — a subject emits, observers react, and the subject doesn't coordinate the observers' interactions. Mediator is *multi-directional coordination* — colleagues talk *to and through* the hub, and the hub contains the logic of how they affect each other. Crucially, **they are often used together**: colleagues frequently *notify* the mediator via an Observer-style mechanism (the mediator observes the colleagues), and then the mediator applies coordination logic. Observer is the *delivery mechanism*; Mediator is the *coordination policy*.

**Mediator vs. Facade:** both add an object in front of others, but a Facade only *simplifies access* to a subsystem in **one direction** (clients → subsystem) and the subsystem objects are unaware of it and still interact among themselves. A Mediator's colleagues actively **route their peer communication through it** (bidirectional), and it *adds* coordination logic rather than merely forwarding.

**Mediator vs. Event Bus/Broker:** an event bus is a *generalized, "dumb" mediator* — it routes by event type between anonymous parties and carries no domain logic (the logic moves into subscribers). A classic mediator *knows* its colleagues and *contains* the interaction logic. As you scale a mediator (more colleagues, more decoupling), it tends to evolve into an event bus, then a message broker.

</details>

## 📊 Comparison Table of Variants

<details>
<summary>📖 Click to expand</summary>

| Axis | V0: Direct mesh | V1: Basic mediator | V2: notify() event | V3: Typed decoupled | V4: Event bus |
|---|---|---|---|---|---|
| **Colleague coupling** | `O(n²)` mesh | `O(n)` (→ mediator) | `O(n)` | `O(n)` (interface only) | Fully anonymous |
| **Interaction logic location** | Scattered in colleagues | In mediator | In mediator (`notify`) | In mediator (typed) | In subscribers (hub is dumb) |
| **Colleague ↔ mediator coupling** | N/A | Concrete colleague API | Concrete colleague API | Narrow interface | None (type-routed) |
| **Event typing** | N/A | Method-specific | Stringly-typed | Compile-time typed | Typed (by class) |
| **Add a colleague** | Wire to all others | `register()` once | `setMediator()` | `publish` via interface | `subscribe()` |
| **God-object risk** | N/A (no hub) | Moderate | High (big `if/else`) | Moderate | Low (logic distributed) |
| **Traceability** | Hard (mesh) | Easy (one hub) | Easy | Easy | Hard (implicit flow) |
| **Sync/async** | Sync | Sync | Sync | Sync/async | One-line swap |
| **Best for** | 2–3 fixed objects | Classic chat/dialog | GoF dialog director | Decoupled, safe events | Large-scale pub/sub |

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

### Pitfall 1: The mediator becomes a god object

**What goes wrong:** Every new feature lands in the mediator until it's a 2,000-line class that knows every colleague and every rule — the coordination complexity you removed from the mesh has simply re-concentrated in one unmaintainable hub.

<details>
<summary>💻 Click to expand — the failure</summary>

```java
class UiMediator {
    void notify(Component sender, String event) {
        // hundreds of lines: login rules, search rules, cart rules, checkout rules,
        // analytics, feature flags, A/B tests... EVERYTHING is here.
        if (sender == loginBtn) { /* 60 lines */ }
        else if (sender == searchBox) { /* 80 lines */ }
        else if (sender == cartWidget) { /* 120 lines */ }
        // ... this class only grows, never shrinks. Nobody understands it.
    }
}
```
</details>

<details>
<summary>💻 Click to expand — the fix (split into focused mediators / delegate)</summary>

```java
// Split by cohesive area: one mediator per dialog/feature, or delegate to sub-mediators.
class LoginMediator   { void notify(Component s, String e) { /* only login rules */ } }
class SearchMediator  { void notify(Component s, String e) { /* only search rules */ } }
// Or push domain logic back onto colleagues; the mediator only COORDINATES, it doesn't
// contain business logic that belongs on a service/colleague. Keep the hub thin.
```
</details>

### Pitfall 2: Coupling the colleague to the concrete mediator type

**What goes wrong:** Colleagues reference `ChatRoom` directly instead of a `ChatMediator` interface, so they can't be tested with a mock, reused with a different mediator, or run against a networked hub — you've decoupled colleagues from each other but re-coupled them to one concrete hub.

<details>
<summary>💻 Click to expand — the fix</summary>

```java
// BAD: colleague nails itself to a concrete mediator.
class User { private final ChatRoom room; /* concrete */ }

// GOOD: depend on the mediator interface (or a narrow publisher interface).
class User { private final ChatMediator room; /* swappable, mockable */ }
```
</details>

### Pitfall 3: Business/domain logic that belongs on colleagues leaks into the mediator

**What goes wrong:** The mediator starts computing tax, validating passwords, or running fraud checks — logic that belongs to a domain service or the colleague itself. The mediator should own *coordination* ("when A changes, tell B"), not *domain behavior*.

<details>
<summary>💻 Click to expand — bad vs. good</summary>

```java
// BAD: mediator does domain work.
void notify(Component s, String e) {
    if (e.equals("checkout")) {
        double tax = subtotal * rateFor(region);   // domain logic in the hub!
        double total = subtotal + tax - discount;   // belongs in a PricingService
        // ...
    }
}
// GOOD: mediator coordinates; domain logic lives in a collaborator.
void notify(Component s, String e) {
    if (e.equals("checkout")) {
        Money total = pricingService.total(cart);    // delegate domain work
        checkoutPanel.showTotal(total);              // mediator only wires colleagues
    }
}
```
</details>

### Pitfall 4: Infinite notification loops

**What goes wrong:** A colleague notifies the mediator, which updates another colleague, whose update fires *another* notification back to the mediator, which updates the first colleague again... a cycle that never terminates (or stack-overflows). Common when the mediator's reaction mutates a colleague in a way that re-triggers the same event.

<details>
<summary>💻 Click to expand — the fix (guard re-entrancy / suppress echo)</summary>

```java
class Mediator {
    private boolean updating = false;   // re-entrancy guard
    void notify(Component sender, String event) {
        if (updating) return;            // ignore events caused by our own updates
        updating = true;
        try {
            // programmatic updates here won't recurse, because re-entrant
            // notify() calls are short-circuited by the guard above.
            fieldB.setValueSilently(derive(sender));
        } finally {
            updating = false;
        }
    }
}
// Alternatives: distinguish user-initiated vs. programmatic events, or make the
// update idempotent so "set to the value it already has" fires no event.
```
</details>

</details>

## 🎓 Interview Tips

**What interviewers commonly ask:**

- "Design a chat room / chat server." (The canonical Mediator — mention `O(n²)` → `O(n)`.)
- "What's the difference between Mediator and Observer?" (The #1 Mediator question — coordination vs. broadcast, and that they're often combined.)
- "How do you keep the mediator from becoming a god object?" (Split by cohesion, keep it thin, delegate domain logic, evolve to pub/sub.)
- "Design an air traffic control system / elevator bank / resource scheduler." (Mediator as an authoritative arbiter of a shared resource.)
- "Orchestration vs. choreography in microservices?" (Central mediator/orchestrator vs. event-driven choreography — Mediator vs. Event Bus at scale.)

**What you should proactively mention even if not asked:**

- Mediator turns a **many-to-many mesh into a one-to-many star**, cutting coupling from `O(n²)` to `O(n)` — say this early, it's the whole point.
- The **god-object risk** is the central trade-off — name it unprompted and describe mitigations (thin mediator, multiple focused mediators, delegate domain logic, move to pub/sub).
- **Mediator vs. Observer**: Observer is one-way broadcast (subject → observers); Mediator is multi-directional coordination logic in a hub — and they're frequently *composed* (the mediator observes its colleagues).
- **The spectrum Mediator → Event Bus → Message Broker**: same "intermediary decouples senders from receivers" idea at increasing scale/decoupling; know where each fits.
- Keep the mediator focused on **coordination, not domain logic** — domain behavior belongs on colleagues/services.
- In distributed systems, a mediator/orchestrator can be a **SPOF/bottleneck**; discuss when **choreography** (decentralized events) is the better fit.
- Depend on a **mediator interface** (or a narrow publisher interface), not the concrete hub, so colleagues stay testable and reusable.

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Observer** — the delivery mechanism a mediator often uses; colleagues notify the mediator via observer-style events. Observer broadcasts; Mediator coordinates. Most-confused sibling.
- **Facade** — also introduces a central object, but only simplifies one-directional access to a subsystem; it adds no coordination logic and subsystem objects still talk to each other.
- **Command** — a **command bus** is mediator-like: it routes commands to handlers, decoupling invokers from receivers. Transitions/actions the mediator drives are often commands.
- **Colleague notification via Observer** — the standard implementation combines the two: the mediator *observes* its colleagues and *drives* their interactions.
- **Event Sourcing / CQRS / Sagas** — the distributed descendants: a saga orchestrator is a mediator; an event bus/broker is a generalized mediator.
- **Singleton** — mediators are frequently singletons (one shared hub for a set of colleagues).
- **Publisher/Subscriber (Pub/Sub)** — the fully-decoupled generalization of Mediator where publishers and subscribers are mutually anonymous.

</details>

## 📚 Library/Framework Implementation

**1. Spring `ApplicationEventPublisher` + `@EventListener`.** Spring's application-event mechanism is a textbook in-process mediator/event-bus: a bean publishes an event via `ApplicationEventPublisher.publishEvent(...)`, and any bean with an `@EventListener` method for that event type is invoked — publisher and listener never reference each other. The `ApplicationContext` *is* the mediator. Supports `@Async` listeners, ordering, and conditional listening. This is the most common Mediator-style tool in the Java ecosystem.

<details>
<summary>💻 Click to expand — Spring ApplicationEventPublisher + @EventListener</summary>

```java
// Event (a value object).
record OrderPlaced(String orderId, long amountCents) {}

@Service
class OrderService {
    private final ApplicationEventPublisher publisher;   // the mediator
    OrderService(ApplicationEventPublisher publisher) { this.publisher = publisher; }
    public void place(String id, long cents) {
        // ... persist order ...
        publisher.publishEvent(new OrderPlaced(id, cents));   // fire-and-forget; no listener refs
    }
}

@Component
class EmailListener {
    @EventListener                                    // subscribes by event type
    public void onOrder(OrderPlaced e) { /* send confirmation email */ }
}

@Component
class InventoryListener {
    @Async                                            // handled on another thread
    @EventListener
    public void onOrder(OrderPlaced e) { /* reserve stock */ }
}
// OrderService knows neither listener; add a fraud-check listener without touching it.
```
</details>

**2. Guava `EventBus` (and Vert.x `EventBus`).** Guava's `EventBus` is an explicit in-process mediator: objects `register(this)` and annotate handler methods with `@Subscribe`; `post(event)` routes to handlers by the event's type. Vert.x provides a clustered `EventBus` that extends the same idea across a cluster over addresses — sending Mediator toward the message-broker end of the spectrum.

<details>
<summary>💻 Click to expand — Guava EventBus</summary>

```java
import com.google.common.eventbus.*;

class UserRegistered { final String email; UserRegistered(String e){ this.email = e; } }

class WelcomeService {
    @Subscribe public void on(UserRegistered e) { System.out.println("welcome " + e.email); }
}
class AnalyticsService {
    @Subscribe public void on(UserRegistered e) { System.out.println("track signup " + e.email); }
}

EventBus bus = new EventBus();          // the mediator
bus.register(new WelcomeService());     // subscribers register themselves
bus.register(new AnalyticsService());
bus.post(new UserRegistered("alice@x.com"));  // fanned out to both by type — no direct refs
```
</details>

**3. Spring MVC `DispatcherServlet` (front controller as mediator) & JMS/message brokers.** `DispatcherServlet` is a front controller that mediates between incoming HTTP requests and the handler methods/controllers, view resolvers, and exception handlers — request handling logic is centralized in one dispatcher rather than smeared across servlets. At the distributed end, **JMS brokers, RabbitMQ, Kafka, and AWS SNS/SQS** are out-of-process mediators: producers publish to a destination/topic and consumers subscribe, fully decoupled and durable — the same mediator idea with persistence and delivery guarantees. **Akka actors** and Swing/JavaFX dialog controllers are further examples (an actor system's message routing, and a dialog controller coordinating widgets).

<details>
<summary>💻 Click to expand — JMS-style broker mediation (conceptual)</summary>

```java
// Producer: publishes to a topic, unaware of any consumer.
MessageProducer producer = session.createProducer(topic);
producer.send(session.createTextMessage("OrderPlaced:ORD-1"));

// Consumer(s): subscribe to the topic, unaware of the producer.
MessageConsumer consumer = session.createConsumer(topic);
consumer.setMessageListener(msg -> handle(msg));   // broker (mediator) routes producer -> consumer
// The broker decouples the two sides, adds durability, ordering, and delivery guarantees —
// Mediator scaled out to a distributed message broker.
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1: [Conceptual] What is the Mediator pattern and what problem does it solve?</strong></summary>

The Mediator pattern is a **behavioral** GoF pattern that **defines an object encapsulating how a set of objects interact**. It promotes loose coupling by keeping objects from referring to each other explicitly, and lets you vary their interaction independently.

It solves the problem of **combinatorial coupling**: when many objects must collaborate, wiring them to talk directly to each other creates an `O(n²)` mesh where every object knows about many others. Change one, and the change ripples; reuse one, and it drags its collaborators along; and the interaction logic is scattered across all the objects with no single home.

Mediator inserts a hub that every colleague knows. Colleagues stop referencing each other and instead notify the mediator ("I changed", "a message arrived"), and the mediator contains the logic for how the others should react. The many-to-many mesh becomes a one-to-many star, coupling drops to `O(n)`, and the interaction protocol becomes one explicit, testable object.
</details>

<details>
<summary><strong>Q2: [Conceptual] Name the participants of the Mediator pattern.</strong></summary>

Four participants:

1. **Mediator** — an interface declaring the communication method(s) colleagues use to talk to the hub (e.g., `notify(sender, event)` or `send(msg, from)`).
2. **ConcreteMediator** — implements the coordination logic; knows the colleagues (holds references or a registry) and decides how they react to each other. **All interaction rules live here.**
3. **Colleague** — a base class/interface for participants; each holds a reference to the mediator (not to sibling colleagues) and notifies it of events.
4. **Concrete Colleagues** — the actual objects (users, widgets, aircraft, services). They communicate only *through* the mediator, never directly with one another.

The critical relationship: **colleagues are coupled only to the mediator**, so the mediator is the single place the collaboration is defined — which is what lets you vary the interaction independently of the colleagues.
</details>

<details>
<summary><strong>Q3: [Conceptual] How does Mediator reduce coupling from O(n²) to O(n)? Explain concretely.</strong></summary>

In a direct peer-to-peer design, any of `n` objects may need to talk to any other, so there are up to `n·(n−1)` directed references (or `n·(n−1)/2` undirected edges) — quadratic in `n`. Each object holds and maintains references to many others; adding one object can require touching many.

With a mediator, each colleague holds exactly **one** reference — to the mediator — and the mediator holds references to the `n` colleagues. That's `2n` edges total, i.e., `O(n)`. Adding a colleague is a single `register()` call; the existing colleagues are untouched.

Concretely, in a 10-user chat: peer-to-peer is 45 connections; via a `ChatRoom` mediator it's 10 (each user ↔ room). The mesh (everyone wired to everyone) becomes a star (everyone wired to the hub). The trade-off is that all that coordination logic now concentrates in the hub — the god-object risk.
</details>

<details>
<summary><strong>Q4: [Conceptual] Where does the interaction logic live in Mediator, and why is that both the benefit and the risk?</strong></summary>

The interaction logic lives **entirely in the ConcreteMediator**. That's the benefit: instead of "when A changes, update B and C" being smeared across A, B, and C (duplicated, drifting), it's one explicit object you can read, test, and change. You can vary how colleagues collaborate by editing only the mediator, and colleagues become dumb, reusable endpoints.

It's also the risk: because *all* coordination concentrates in the hub, the mediator can grow into a **god object** — a bloated, do-everything class that knows every colleague and every rule. You've traded distributed complexity (the mesh) for centralized complexity (the hub). The mitigation is to keep the mediator focused on *coordination* (not domain logic), split it into multiple cohesive mediators when it grows, and, at scale, move toward a publish/subscribe bus where the logic redistributes into subscribers.
</details>

<details>
<summary><strong>Q5: [Conceptual] Explain the spectrum: Mediator → Event Bus → Message Broker.</strong></summary>

All three embody the same idea — **decouple senders from receivers via an intermediary** — at increasing scale and infrastructure weight:

- **Mediator**: knows its specific colleagues and *contains* the interaction logic. Star topology, logic in the hub. In-process, domain-aware.
- **Event Bus**: a *generalized, "dumb" mediator*. Routing is generic (by event type) between anonymous publishers and subscribers; the logic moves out of the hub and into subscribers. Still in-process (Guava `EventBus`, Spring `ApplicationEventPublisher`).
- **Message Broker**: an event bus that is *out-of-process, durable, and distributed* (Kafka, RabbitMQ, SNS/SQS). Adds persistence, delivery guarantees, topics/partitions, and cross-service decoupling.

As a mediator accumulates colleagues and you push for more decoupling, it naturally evolves into an event bus; scale that across services and it becomes a broker. Knowing this spectrum shows you see Mediator as an architectural idea, not just a GoF diagram.
</details>

<details>
<summary><strong>Q6: [Implementation] Implement a basic chat room mediator (coding challenge).</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
import java.util.*;

interface ChatMediator {
    void register(User u);
    void send(String msg, User from);
}

abstract class User {
    protected final ChatMediator room;
    protected final String name;
    User(ChatMediator room, String name) { this.room = room; this.name = name; }
    void send(String msg) { room.send(msg, this); }
    abstract void receive(String msg, String from);
    String name() { return name; }
}

class ChatRoom implements ChatMediator {
    private final List<User> users = new ArrayList<>();
    public void register(User u) { users.add(u); }
    public void send(String msg, User from) {
        for (User u : users) if (u != from) u.receive(msg, from.name());  // routing in ONE place
    }
}

class ChatUser extends User {
    ChatUser(ChatMediator room, String name) { super(room, name); }
    void receive(String msg, String from) { System.out.println(name + " <- " + from + ": " + msg); }
}

class Demo {
    public static void main(String[] a) {
        ChatMediator room = new ChatRoom();
        User alice = new ChatUser(room, "Alice"), bob = new ChatUser(room, "Bob");
        room.register(alice); room.register(bob);
        alice.send("hi");   // Bob <- Alice: hi
    }
}
```
</details>

The key points to state: users depend only on `ChatMediator`, never on each other; the routing rule ("everyone except sender") lives once in `ChatRoom.send`; adding a user is one `register()`.
</details>

<details>
<summary><strong>Q7: [Implementation] Implement an air-traffic-control mediator arbitrating a single runway (coding challenge).</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
import java.util.*;

interface ATC {
    void register(Plane p);
    void requestRunway(Plane p);
    void runwayCleared(Plane p);
}

class Plane {
    private final String id; private final ATC tower;
    Plane(String id, ATC tower) { this.id = id; this.tower = tower; }
    void requestLanding() { tower.requestRunway(this); }
    void land() { System.out.println(id + ": cleared, landing"); tower.runwayCleared(this); }
    void hold() { System.out.println(id + ": holding"); }
    String id() { return id; }
}

class Tower implements ATC {
    private final Queue<Plane> queue = new LinkedList<>();
    private Plane onRunway = null;                      // shared-resource state in the hub
    public void register(Plane p) {}
    public void requestRunway(Plane p) {
        if (onRunway == null) { onRunway = p; p.land(); }
        else { queue.add(p); p.hold(); }               // busy -> queue + hold
    }
    public void runwayCleared(Plane p) {
        onRunway = null;
        Plane next = queue.poll();
        if (next != null) { onRunway = next; next.land(); }  // tower drives the next plane
    }
}
```
</details>

The distinguishing point: unlike the chat room (broadcast), the tower is an **authoritative arbiter** of a shared resource — it holds the runway state, serializes access, queues contenders, and issues *targeted commands* (`land()`) to specific colleagues. Planes never coordinate with each other, which is the safety property.
</details>

<details>
<summary><strong>Q8: [Implementation] Show the idiomatic GoF `notify(sender, event)` form for a dialog with interdependent widgets.</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
interface Mediator { void notify(Widget sender, String event); }

abstract class Widget {
    protected Mediator mediator;
    void setMediator(Mediator m) { this.mediator = m; }
}

class CheckBox extends Widget {
    private boolean checked;
    void toggle() { checked = !checked; mediator.notify(this, "toggled"); }
    boolean checked() { return checked; }
}
class TextField extends Widget {
    private boolean enabled;
    void setEnabled(boolean e) { enabled = e; System.out.println("field " + (e ? "on" : "off")); }
}

class FormMediator implements Mediator {
    private final CheckBox agree = new CheckBox();
    private final TextField notes = new TextField();
    FormMediator() { agree.setMediator(this); notes.setMediator(this); }
    public void notify(Widget sender, String event) {
        if (sender == agree && event.equals("toggled"))
            notes.setEnabled(agree.checked());   // rule: notes editable only when "agree" checked
    }
    CheckBox agree() { return agree; }
}
```
</details>

The single generic `notify(sender, event)` is the whole interaction protocol; widgets emit events without knowing who reacts, and every cross-widget rule lives in the mediator. This is exactly GoF's `DialogDirector`.
</details>

<details>
<summary><strong>Q9: [Implementation] How would you make colleagues depend on the mediator without coupling to its concrete type?</strong></summary>

Depend on a **narrow interface** — either the full `Mediator` interface or an even narrower publisher interface exposing just the method the colleague needs.

<details>
<summary>💻 Click to expand</summary>

```java
// Narrowest possible contract: "I can publish an event."
interface EventPublisher { void publish(Object event); }

class Colleague {
    private final EventPublisher hub;                 // NOT a concrete ChatRoom
    Colleague(EventPublisher hub) { this.hub = hub; }
    void doThing() { hub.publish(new ThingHappened()); }
}
// The concrete mediator implements EventPublisher; colleagues are trivially mockable
// and reusable with any hub (in-process, networked, test double).
```
</details>

This keeps colleagues testable (pass a mock publisher), reusable (swap the hub), and prevents re-coupling colleagues to a single concrete mediator after you decoupled them from each other. Typed events (a sealed hierarchy or records) add compile-time safety over stringly-typed event names.
</details>

<details>
<summary><strong>Q10: [Implementation] Implement a generic type-safe event bus (the "dumb mediator").</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
import java.util.*;
import java.util.function.Consumer;
import java.util.concurrent.*;

class EventBus {
    private final Map<Class<?>, List<Consumer<Object>>> subs = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    <E> void subscribe(Class<E> type, Consumer<E> handler) {
        subs.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
            .add((Consumer<Object>) handler);
    }
    <E> void publish(E event) {
        for (Consumer<Object> h : subs.getOrDefault(event.getClass(), List.of())) {
            try { h.accept(event); }                       // isolate failures
            catch (RuntimeException ex) { /* log; don't break other subscribers */ }
        }
    }
}
// Usage:
// bus.subscribe(OrderPlaced.class, e -> ship(e));
// bus.publish(new OrderPlaced("ORD-1"));   // routed to all OrderPlaced subscribers
```
</details>

Key points: the bus holds *no* domain logic (it's a pure `Map<type → handlers>` router), publishers and subscribers are mutually anonymous, per-handler try/catch isolates failures, and swapping in an `Executor` makes it async with no caller changes. This is Guava `EventBus` / Spring events in miniature.
</details>

<details>
<summary><strong>Q11: [Breaking] How can a mediator cause an infinite notification loop, and how do you prevent it?</strong></summary>

A loop happens when the mediator's *reaction* to an event mutates a colleague in a way that **re-fires the same event**, which the mediator handles by mutating again, forever. Example: field A changes → mediator updates field B → B's setter fires "changed" → mediator updates A → A fires "changed" → ...

Prevention strategies:

<details>
<summary>💻 Click to expand — re-entrancy guard</summary>

```java
private boolean updating = false;
void notify(Widget sender, String event) {
    if (updating) return;                 // ignore events caused by our own updates
    updating = true;
    try { fieldB.setValue(derive(sender)); }
    finally { updating = false; }
}
```
</details>

Other options: (1) distinguish **user-initiated** vs. **programmatic** events and only react to user events; (2) make updates **idempotent** — "set to the value it already has" fires no event; (3) in async/event-bus form, detect cycles with a correlation/causation id and drop events that would re-trigger their own cause. The re-entrancy guard is the simplest and most common in GUI mediators.
</details>

<details>
<summary><strong>Q12: [Breaking] Your mediator has become a 2,000-line god object. How did that happen and how do you fix it?</strong></summary>

It happens because the mediator is the *natural home* for every new coordination rule, so features accrete there indefinitely — plus, teams often dump **domain logic** (pricing, validation, fraud) into the mediator that actually belongs on services/colleagues.

Fixes, in order:

1. **Move domain logic out.** The mediator should only *coordinate* ("when A happens, tell B"); push business behavior into domain services the mediator delegates to.
2. **Split by cohesion.** One giant UI mediator becomes several focused mediators (login, search, cart), each owning a cohesive slice.
3. **Introduce sub-mediators / hierarchy.** A top-level mediator coordinates sub-mediators.
4. **Move to publish/subscribe.** Replace hard-coded `if (sender == x)` reactions with a typed event bus where each concern is a separate subscriber — the logic redistributes out of the hub.

The meta-point for interviews: name the god-object risk *before* the interviewer does, and show you know the mitigations. It signals seniority.
</details>

<details>
<summary><strong>Q13: [Breaking] In an async event-bus mediator, what breaks that didn't in a synchronous one?</strong></summary>

Several things:

- **Ordering.** With async delivery, handlers may run out of order or concurrently, so "A must be processed before B" is no longer guaranteed by call order. You need explicit sequencing, partitioning by key, or ordered topics.
- **Error handling / lost work.** A synchronous handler exception propagates to the publisher; an async one is swallowed unless you build retry/dead-letter handling. A crash between publish and handling can lose the event without durability.
- **Traceability.** Synchronous flow is a stack trace; async pub/sub flow is implicit — you need correlation ids and distributed tracing to follow "what happened when X was published."
- **Backpressure.** A fast publisher can overwhelm slow subscribers; you need bounded queues/backpressure.
- **Delivery semantics.** At-least-once delivery means handlers must be **idempotent** (dedup by event id); exactly-once effect needs an idempotency store.

The fix set: idempotent handlers keyed by event id, durable/retried delivery with dead-letter queues, ordering via partition keys, and tracing across the async boundary.
</details>

<details>
<summary><strong>Q14: [Breaking] A colleague still holds a stale reference / gets notified after it "left." What went wrong?</strong></summary>

The mediator's registry wasn't updated on removal, or a colleague registered a callback/listener on entry and never deregistered it — a **listener leak**. The mediator keeps delivering to a colleague that has logically departed (a chat user who left still gets messages; a widget that was disposed still gets `notify`), which can cause errors, wasted work, or memory retention (the mediator holds the colleague alive).

<details>
<summary>💻 Click to expand — the fix (symmetric register/unregister)</summary>

```java
void removeUser(User u) {
    members.remove(u.name());        // stop routing to it
    u.onLeave();                     // let it release resources
}
// Symmetric lifecycle: whatever you register on join, deregister on leave.
// For long-lived hubs holding short-lived colleagues, consider weak references
// so a forgotten colleague can still be GC'd.
```
</details>

The discipline is *symmetric lifecycle management*: every `register` has a matching `unregister`, ideally guaranteed via try/finally or the colleague's dispose hook.
</details>

<details>
<summary><strong>Q15: [Trade-off] Mediator vs. Observer — what's the real difference, and are they mutually exclusive?</strong></summary>

They're different in **intent and topology**, and they're *not* mutually exclusive — they're frequently composed.

- **Observer** is *one-directional broadcast*: one subject changes state and notifies many observers, which react. The subject doesn't coordinate the observers' interactions; observers don't affect each other through the subject.
- **Mediator** is *multi-directional coordination*: colleagues communicate to and through the hub, and the hub contains the logic of how they affect one another. It's bidirectional and holds a coordination *policy*, not just a notification.

They combine naturally: a mediator often *observes* its colleagues (colleagues notify the mediator via an Observer-style event), and then applies coordination logic to drive the other colleagues. So Observer is the **delivery mechanism**; Mediator is the **coordination policy**. The tell: if there's a subject-with-dependents broadcasting state changes, that's Observer; if there's a hub owning "when A does X, B and C should do Y," that's Mediator.
</details>

<details>
<summary><strong>Q16: [Trade-off] Mediator vs. Facade — both add a central object. How do they differ?</strong></summary>

Both introduce an object in front of a group of others, but the intent and direction differ:

- **Facade** provides a *simplified interface* to a subsystem. It's **one-directional**: clients call the facade, the facade forwards to the subsystem, and the subsystem objects are **unaware of the facade** and still interact among themselves. The facade adds no new logic — it just simplifies access.
- **Mediator** *centralizes and owns the interaction* among its colleagues. It's **bidirectional/multi-directional**: colleagues actively route their peer communication *through* the mediator, they *know* the mediator, and the mediator *adds coordination logic* they'd otherwise perform among themselves.

Mnemonic: **Facade simplifies access to a subsystem (the subsystem doesn't change); Mediator changes how the objects communicate (they now go through the hub instead of each other).** A Facade could exist over objects that also use a Mediator internally — they solve different problems.
</details>

<details>
<summary><strong>Q17: [Trade-off] When would you choose a plain Observer or a raw event bus over a classic Mediator, and vice versa?</strong></summary>

Choose a **classic Mediator** when:
- The interaction is *rich and stateful* — there's genuine coordination logic ("when A changes, enable B, disable C, update D") that benefits from living in one readable, testable place.
- You want *explicit* control flow you can trace, and the colleague set is known and bounded (a dialog, a small device hub).

Choose a **plain Observer** when:
- You only need *one-way broadcast* on state change with no cross-colleague coordination. A full mediator would be overkill.

Choose an **event bus** when:
- You want *maximum decoupling* with many anonymous publishers/subscribers, and you're willing to trade explicit, traceable flow for extensibility. Adding a subscriber shouldn't touch publishers.
- The coordination logic is naturally *per-subscriber* rather than centralized.

The trade-off axis is **explicit centralized logic (Mediator) vs. implicit distributed decoupling (event bus)**, with Observer as the lightweight one-way broadcast in between.
</details>

<details>
<summary><strong>Q18: [Advanced] Orchestration vs. choreography in microservices — how does this map to Mediator?</strong></summary>

- **Orchestration** = **Mediator**. A central **orchestrator** (saga coordinator) drives the workflow: it calls each service in turn, handles failures, and runs compensations. The workflow lives in one auditable place; services stay decoupled from each other. Pro: clear, centralized, easy to reason about and change; con: the orchestrator is a coupling point, a potential SPOF/bottleneck, and can become a distributed god object.
- **Choreography** = **Event Bus / Pub-Sub**. There's no central hub; each service emits events and reacts to others' events. Pro: fully decoupled, scalable, no single bottleneck; con: the end-to-end flow is *emergent* and hard to trace, cycles/ordering are subtle, and "what's the overall workflow?" has no single answer.

Rule of thumb: use **orchestration** for complex, multi-step business transactions needing clear control and compensation (checkout, onboarding); use **choreography** for loosely-coupled reactive flows where services just respond to facts. Many systems mix both — orchestrate the critical path, choreograph the peripheral reactions.
</details>

<details>
<summary><strong>Q19: [Advanced] When does the mediator become a bottleneck / SPOF, and how do you mitigate it at scale?</strong></summary>

Because *all* communication flows through the hub, at scale the mediator concentrates both **load** (every interaction hits it) and **risk** (if it's down, colleagues can't coordinate). In-process this manifests as a synchronization chokepoint (a single lock/thread serializing everything); distributed, it's a service that every flow depends on.

Mitigations:
- **Make the hub stateless and horizontally scalable** — run many instances behind a load balancer; keep coordination state in a shared store, not in the instance.
- **Partition/shard the mediator** — one hub per room/tenant/key instead of one global hub (a chat server shards by room; a broker shards by topic partition).
- **Go async and durable** — a message broker with persistence decouples timing and survives hub restarts, turning a hard dependency into a buffered one.
- **Replicate for HA** — leader/follower or consensus so a hub failure fails over rather than halting the system.
- **Push logic to the edges (choreography)** for flows that don't need central control, so the hub isn't on every path.

The interview point: acknowledge the SPOF/bottleneck honestly, then show the toolbox — sharding, statelessness, async/durable delivery, and selective choreography.
</details>

<details>
<summary><strong>Q20: [Advanced] Design a saga coordinator (mediator) for a distributed order transaction. What are the key concerns?</strong></summary>

A saga coordinator is a distributed Mediator that sequences local transactions across services, each with a **compensating action** for rollback (there's no distributed ACID transaction).

<details>
<summary>💻 Click to expand — coordinator skeleton</summary>

```java
class OrderSaga {
    void run(Order order) {
        Deque<Runnable> compensations = new ArrayDeque<>();
        try {
            String pay = payment.charge(order);      compensations.push(() -> payment.refund(pay));
            String res = inventory.reserve(order);   compensations.push(() -> inventory.release(res));
            shipping.schedule(order);                // last step, nothing after to compensate its success
            notifier.confirm(order);
        } catch (RuntimeException ex) {
            while (!compensations.isEmpty()) compensations.pop().run();  // compensate in REVERSE
            throw new SagaFailed(order.id(), ex);
        }
    }
}
```
</details>

Key concerns: (1) **compensation in reverse order** — later effects undone before earlier ones; (2) **idempotency** — every step and compensation must be safe to retry (dedup by saga/step id), because the coordinator may crash and resume; (3) **durable saga state** — persist which steps completed so a restarted coordinator knows where to resume/compensate (event-sourced or a state machine in a DB); (4) **timeouts & stuck sagas** — steps can hang; need timeouts and a way to force-compensate; (5) **orchestration vs. choreography** — this is the orchestrated form (central mediator); the choreographed alternative uses events with no coordinator. The coordinator is a mediator, so it faces the same god-object/SPOF trade-offs — keep it thin, make it stateless-plus-durable-store, and shard by order.
</details>

---

### 🏛️ Staff / Principal Engineer Deep-Dive

<details>
<summary><strong>SP1: Orchestration vs. choreography — how do you actually decide, and how do you evolve between them?</strong></summary>

Decide by **who owns the workflow and how much you need to reason about it centrally**. Orchestration (a mediator/orchestrator) wins when the business process is complex, has explicit ordering and compensation, and stakeholders ask "show me the checkout flow" — you want one place that *is* the flow. Choreography (events) wins when services should react autonomously to facts, coupling must be minimal, and the "flow" is really many independent reactions.

In practice, mature systems are **hybrid**: orchestrate the *critical, compensating* path (payment → inventory → shipping) with a saga coordinator, and choreograph the *peripheral* reactions (analytics, recommendations, emails) off domain events. Evolution usually goes *choreography → orchestration* as an initially simple event chain grows tangled ("which service emits what, and in what order?" becomes unanswerable) and teams extract an explicit orchestrator to regain visibility — or *orchestration → choreography* when a central orchestrator becomes a bottleneck and non-critical steps are peeled off onto events. The Staff-level insight: the choice isn't binary or permanent; it's a per-flow decision revisited as complexity and scale change, and the failure mode of each (invisible flow for choreography, god-orchestrator/SPOF for orchestration) is exactly the Mediator trade-off at architecture scale.
</details>

<details>
<summary><strong>SP2: How do you prevent the god-object anti-pattern at the architecture level, not just in one class?</strong></summary>

At the class level you split a fat mediator by cohesion and move domain logic to services. At the *architecture* level the same disease appears as a **central orchestrator/ESB that everything depends on** — the "enterprise service bus with all the logic" anti-pattern that killed many SOA deployments.

Principles to prevent it: (1) **Bounded-context mediators** — one coordinator per bounded context, not a global one; each owns only its context's workflows. (2) **Thin orchestrators, smart domains** — orchestrators sequence and compensate; they must not contain domain rules (pricing, eligibility) — those live in the owning service. (3) **Prefer choreography for cross-context reactions** so no single hub is on every path. (4) **Explicit workflow-as-code/state-machine** (e.g., a durable-execution engine like Temporal/Cadence, Step Functions) so orchestration logic is versioned, testable, and observable rather than sprawling imperative glue. (5) **Ownership boundaries** — the orchestrator is owned by the team that owns the business process, not a central "integration team" that becomes a bottleneck for every change. The recurring test: "can I change service X's internals without editing the hub?" If no, the hub knows too much.
</details>

<details>
<summary><strong>SP3: Designing an in-process event bus for a large monolith — what are the subtle correctness and performance concerns?</strong></summary>

An in-process bus (Guava/Spring events) looks trivial but has sharp edges at scale:

- **Sync vs. async semantics must be explicit and consistent.** Synchronous listeners run in the publisher's thread and transaction — an exception rolls back the publisher, which is sometimes desired (transactional consistency) and sometimes catastrophic (one listener fails the whole request). Async listeners run outside the publisher's transaction, so they see committed state but lose transactional atomicity and can be lost on crash. Spring's `@TransactionalEventListener(phase = AFTER_COMMIT)` exists precisely to fire only after the publisher's transaction commits — a subtle correctness point.
- **Ordering and re-entrancy.** Listeners that publish further events can create deep or cyclic chains; you need cycle detection or phase separation.
- **Error isolation.** One slow/throwing subscriber must not break others (per-handler try/catch, timeouts).
- **Performance.** Reflection-based dispatch (annotation scanning) and per-event allocation add overhead on hot paths; a high-frequency domain may need a typed, allocation-light bus or a ring buffer (LMAX Disruptor) instead.
- **Observability.** Implicit flow needs instrumentation — metrics per event type, tracing that spans publish→handle — or debugging becomes archaeology.

The Staff call is often "should this even be an event, or a direct call?" — events buy decoupling at the cost of traceability, and overusing them turns a monolith into a distributed system's worst properties (implicit flow) without its benefits (independent scaling).
</details>

<details>
<summary><strong>SP4: How do you scale a stateful mediator (like a chat/presence hub) to millions of connections?</strong></summary>

A single chat/presence hub can't hold millions of connections or fan out globally, so you shard and layer:

- **Shard by room/conversation.** Partition rooms across many hub instances (consistent hashing on room id), so each hub owns a slice. A user connects to the hub that owns their room(s). This turns one god-hub into many bounded mediators.
- **Separate the connection layer from the routing layer.** Edge gateways hold the WebSocket connections; a backplane (Redis pub/sub, Kafka, or a dedicated messaging fabric) routes messages between gateways so a message from a user on gateway A reaches a user on gateway B. The mediator logic (who's in the room, delivery rules) sits above the backplane.
- **Externalize state.** Presence/membership lives in a shared store (Redis), not in a hub instance's heap, so hubs are stateless and horizontally scalable and a hub restart doesn't lose the room.
- **Backpressure & fan-out limits.** Huge rooms (a celebrity broadcast) need fan-out throttling, batching, or a different topology (pull/feed instead of push).
- **HA.** Replicate shards; on hub failure, connections reconnect and the owning shard fails over.

The pattern insight: the *logical* mediator ("a room coordinates its users") is preserved, but the *physical* hub is decomposed into sharded, stateless instances over a messaging backplane — Mediator's star topology re-implemented as a scalable distributed system. This is essentially how Slack, Discord, and WhatsApp fan-out layers are built.
</details>

<details>
<summary><strong>SP5: When is introducing a Mediator the wrong call, and what's the deeper design smell it sometimes hides?</strong></summary>

Introducing a mediator is wrong when it's used to *manage complexity that shouldn't exist*. If objects are so entangled that you reach for a mediator to referee them, sometimes the real problem is **poor boundaries** — the objects have the wrong responsibilities, and a mediator just formalizes and hides a bad decomposition behind a hub. You get a mediator that's a god object because the domain model underneath is incoherent.

Other wrong calls: (1) using a mediator for **two or three objects with simple interactions** — direct references are clearer and the hub is pure ceremony; (2) using it where **Observer suffices** (one-way broadcast) — you don't need bidirectional coordination; (3) reaching for a distributed orchestrator when **choreography** would decouple better and avoid a SPOF; (4) letting a mediator become a **dumping ground for domain logic** that belongs on aggregates/services.

The Staff-level move is diagnostic: before adding a mediator, ask *why* these objects are so interconnected. If the answer is "they genuinely represent a many-to-many collaboration with real coordination rules" (a dialog, a chat room, a resource arbiter), Mediator is right. If the answer is "we split responsibilities badly and now everything talks to everything," a mediator is a band-aid — the fix is to redraw the boundaries (merge over-split objects, extract a proper aggregate, or rethink the model) so the coordination need largely disappears. A mediator should *centralize necessary coordination*, not *paper over accidental coupling*.
</details>

---

## ⚡ Quick Revision

**One-liner:** Mediator replaces a tangled `O(n²)` mesh of objects talking directly to each other with an `O(n)` star where every colleague talks only to a central hub that owns the interaction logic.

**The whole pattern in a paragraph:** When many objects must collaborate, wiring them peer-to-peer creates a dense web where everyone knows everyone — coupling explodes, interaction logic scatters, and nothing is reusable. Mediator inserts a hub (the ConcreteMediator) that every colleague references *instead of* referencing each other. A colleague notifies the mediator when something happens (`notify(sender, event)` or `send(msg, from)`); the mediator contains the logic of how the other colleagues should react and drives them. This turns many-to-many into one-to-many, cutting coupling from `O(n²)` to `O(n)` and centralizing the collaboration into one explicit, testable, changeable object. The headline trade-off is the **god-object risk** — all coordination complexity now concentrates in the hub — mitigated by keeping the mediator thin (coordination, not domain logic), splitting it by cohesion, and, at scale, evolving toward a publish/subscribe **event bus** and then a distributed **message broker**. Mediator is the pattern behind chat rooms, GUI dialog directors, air-traffic control, smart-home hubs, and saga orchestrators.

**Top 5 interview answers to memorize:**

1. **"What is it / why?"** → Encapsulates how a set of objects interact so they don't reference each other directly; turns an `O(n²)` mesh into an `O(n)` star with coordination logic in one hub.
2. **"Mediator vs. Observer?"** → Observer is one-way broadcast (subject → observers); Mediator is multi-directional coordination logic in a hub. They're often *combined* — the mediator observes its colleagues, then coordinates them.
3. **"Biggest downside?"** → The god-object risk: the hub concentrates all complexity and can become an unmaintainable do-everything class / SPOF. Keep it thin, split by cohesion, move to pub/sub at scale.
4. **"Mediator vs. Facade?"** → Facade is one-directional simplification of a subsystem (subsystem unaware, still self-interacting); Mediator is bidirectional coordination the colleagues route through and that adds interaction logic.
5. **"At scale / distributed?"** → Mediator → Event Bus → Message Broker is one spectrum. Orchestration (central mediator/saga coordinator) vs. choreography (decentralized events) is the same trade-off: centralized clarity vs. decoupled scalability.

**Trigger words** (if the interviewer says these, think Mediator): *"objects talk to each other," "everything is coupled to everything," "many-to-many," "chat room," "dialog with interdependent widgets," "control tower," "coordinate/orchestrate," "hub," "central authority over a shared resource," "reduce coupling between components," "who tells whom," "event bus," "saga orchestrator," "orchestration vs. choreography."*


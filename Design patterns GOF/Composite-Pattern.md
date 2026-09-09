# Composite Pattern ⭐⭐⭐ (Difficulty: 3/5 — the tree idea is intuitive, but the *transparent-vs-safe* trade-off, parent-pointer invariants, and the "don't type-check leaf-vs-composite" discipline run surprisingly deep)

> **Category:** Structural Pattern (GoF)
> **Also known as:** Object Tree

The Composite pattern lets you **compose objects into tree structures to represent part-whole hierarchies, and then treat individual objects (leaves) and groups of objects (composites) through one uniform interface.** It's the reason a file system can compute the size of a single file and an entire directory tree with the same call, a GUI can `paint()` a button and a whole window identically, and an arithmetic expression like `(3 + 4) * 5` can be evaluated by asking the root node to evaluate itself. The client writes code against *one* type and never asks "is this a single thing or a group?"

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 0: No Composite — Type-Checking & Branching (Anti-pattern)](#variant-0-no-composite--type-checking--branching-anti-pattern)
   - [Variant 1: Transparent Composite (child ops in the Component)](#variant-1-transparent-composite-child-ops-in-the-component)
   - [Variant 2: Safe Composite (child ops only in the Composite)](#variant-2-safe-composite-child-ops-only-in-the-composite)
   - [Variant 3: Production Composite (parent pointers, caching, iterator)](#variant-3-production-composite-parent-pointers-caching-iterator)
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

> **GoF Definition:** *"Compose objects into tree structures to represent part-whole hierarchies. Composite lets clients treat individual objects and compositions of objects uniformly."*

The Composite pattern exists to erase the distinction — from the client's point of view — between a **single object** and a **container of objects**. Many domains are naturally recursive: a directory contains files *and* directories; a UI panel contains widgets *and* panels; a graphics group contains shapes *and* groups. If clients must constantly ask "am I holding one thing or a collection?" and branch accordingly, the code fills with type checks and duplicated traversal logic.

Composite solves this by defining a common **Component** interface that *both* leaves and containers implement. A leaf does the real work; a composite forwards the request to its children and aggregates the result. Because both share the interface, the client calls `component.render()` (or `getSize()`, `evaluate()`, `print()`) without knowing or caring whether it's talking to a leaf or a whole subtree. The recursion is hidden *inside* the composite, not scattered across the client.

---

## 🎯 Problem

You have a **part-whole hierarchy** — objects that contain other objects, arbitrarily nested — and you want clients to operate on the whole tree and on individual elements *the same way*. Without a unifying abstraction, every client that walks the structure must distinguish leaves from containers, recurse manually, and repeat that logic everywhere.

**The pain points that lead you to Composite:**

- You're modeling something **recursively nested** (trees), and the nesting depth is unbounded or unknown at compile time.
- Client code is littered with `if (obj instanceof Group) { for each child ... } else { ... }` branching — the recursion and type-checking leak everywhere.
- You want **operations that apply uniformly** to a single element and to a group of elements (compute total size, render, serialize, count nodes).
- Adding a new kind of leaf or container should not force you to rewrite every traversal in the codebase.

**Concrete example scenarios:**

1. **File system.** A `File` has a size; a `Directory` contains files and sub-directories. `getSize()` on a file returns its bytes; on a directory it returns the sum of everything beneath it. The client just calls `node.getSize()`.

2. **GUI widget trees.** A `Button` or `TextField` is a leaf; a `Panel` or `Window` is a container of widgets *and* other panels. `render()`, `resize()`, and event dispatch all recurse through the tree uniformly (this is literally how AWT/Swing work).

3. **Org chart / salary rollup.** An `Employee` (individual contributor) is a leaf; a `Manager` contains reports who may themselves be managers. `getHeadcount()` or `getTotalCompensation()` rolls up the whole subtree.

4. **Arithmetic / expression trees.** A `Number` is a leaf; `Add`, `Multiply` are composites holding operand nodes. `evaluate()` on the root recursively evaluates the whole expression `(3 + 4) * 5`. Compilers, spreadsheet engines, and query planners all use this.

---

## ✅ Solution

The core idea, in plain language: **define one interface (the Component) that represents both a single element and a group of elements. Leaves implement it directly. Composites implement it too, but hold a list of child Components and implement each operation by delegating to their children and combining the results.** Because a Composite *is-a* Component and *holds* Components, you get recursive tree structures where the client treats every node identically.

**Key structural elements:**

- **Component** — the common interface (or abstract class) declaring the operations that apply to both leaves and composites (`render()`, `getSize()`, `operation()`). It may also declare child-management operations (`add`, `remove`, `getChild`) — where those live is the central design decision (see Variants 1 vs 2).
- **Leaf** — a primitive object with no children. It implements the Component operations by doing the actual work. It has no meaningful child-management behavior.
- **Composite** — a node that has children. It stores child Components (typically a `List<Component>`), implements child-management (`add`/`remove`), and implements the business operations by iterating its children and aggregating.
- **Client** — code that manipulates objects through the Component interface. It never needs to distinguish leaf from composite.

**The mechanism that makes it work:** *recursive composition through a shared supertype*. A Composite holds references of the very interface it implements, so trees nest to any depth. When the client invokes an operation on the root, the call **recurses**: each Composite forwards to its children until it reaches leaves, which return concrete values that bubble back up and aggregate. Polymorphism erases the leaf/composite distinction at every call site — the `for` loop and the base case both live *inside* the type hierarchy, not in the client. This is the same shape as a recursive data structure (a tree) expressed in objects: the base case is the Leaf, the recursive case is the Composite.

## 💻 Implementation

We'll model a **file system** — the canonical FAANG Composite scenario because it's genuinely recursive (directories contain files *and* directories) and the "compute total size of a folder" operation naturally recurses. The client wants one `FileSystemNode` type and one `getSize()` call, regardless of whether it points at a single file or a deep tree.

### Variant 0: No Composite — Type-Checking & Branching (Anti-pattern)

**What's wrong with it:** Without a shared abstraction, the client must distinguish files from folders everywhere. `getSize()` becomes a recursive free function full of `instanceof` checks and casts. Every new operation (count files, search by name, print tree) duplicates the same branching-and-recursion skeleton. Add a new node type (e.g., a symbolic link) and you must hunt down every such method — shotgun surgery, and a violation of Open/Closed.

<details>
<summary>💻 Click to expand code — the anti-pattern</summary>

```java
// DON'T DO THIS — leaf/container distinction leaks into every traversal.
class MyFile   { String name; long size; }
class MyFolder { String name; List<Object> children = new ArrayList<>(); } // File or Folder

class SizeCalculator {
    // Recursion + type-checking hard-coded into the CLIENT.
    static long getSize(Object node) {
        if (node instanceof MyFile) {
            return ((MyFile) node).size;
        } else if (node instanceof MyFolder) {
            long total = 0;
            for (Object child : ((MyFolder) node).children) {
                total += getSize(child);          // manual recursion, repeated everywhere
            }
            return total;
        }
        throw new IllegalArgumentException("Unknown node type");
        // Add MySymlink? Edit this method AND count(), AND search(), AND print()...
    }
}
```
</details>

**Pros:** Nothing to learn; fine for a fixed, tiny structure that never grows a new node type.
**Cons:** Every operation re-implements the same `instanceof`/cast/recurse skeleton; adding a node type touches every operation; `List<Object>` is untyped and error-prone; impossible to extend without editing existing code (OCP violation).
**Mechanism (why it fails):** there is no *shared supertype*, so the recursion's base case and recursive case can't be dispatched polymorphically — they collapse into hand-written conditionals in the client.

---

### Variant 1: Transparent Composite (child ops in the Component)

**What problem it solves:** Introduce a common `FileSystemNode` interface implemented by both `File` (leaf) and `Directory` (composite). Now the client calls `node.getSize()` and polymorphism handles the recursion. In the **transparent** variant, the child-management methods (`add`, `remove`, `getChild`) are declared on the *Component* interface itself. This makes leaves and composites **completely interchangeable** — a client can call `add()` on any node reference without knowing its concrete type, which is the "transparency" GoF favors because it maximizes uniformity.

The catch: a `File` has no children, so it must implement `add()`/`remove()` somehow — typically by throwing `UnsupportedOperationException`. That pushes a *possible runtime error* onto operations the type system says are legal. This is the classic **transparency vs. type-safety** trade-off, and it has a Liskov-substitution smell (a File is-a Node but can't honor the whole Node contract).

<details>
<summary>💻 Click to expand code — transparent composite</summary>

```java
import java.util.*;

// COMPONENT — declares BOTH business ops AND child-management ops.
interface FileSystemNode {
    String getName();
    long getSize();
    void print(String indent);

    // Child management on the base type = "transparent". Leaves must cope.
    default void add(FileSystemNode child)    { throw new UnsupportedOperationException("Not a directory"); }
    default void remove(FileSystemNode child) { throw new UnsupportedOperationException("Not a directory"); }
    default FileSystemNode getChild(int i)    { throw new UnsupportedOperationException("Not a directory"); }
}

// LEAF — real work, no children.
class TextFile implements FileSystemNode {
    private final String name;
    private final long size;
    TextFile(String name, long size) { this.name = name; this.size = size; }
    public String getName() { return name; }
    public long getSize()   { return size; }              // base case of the recursion
    public void print(String indent) { System.out.println(indent + "📄 " + name + " (" + size + "B)"); }
    // add/remove/getChild inherited as throwing defaults — a File cannot hold children.
}

// COMPOSITE — holds children, delegates + aggregates.
class Directory implements FileSystemNode {
    private final String name;
    private final List<FileSystemNode> children = new ArrayList<>();
    Directory(String name) { this.name = name; }
    public String getName() { return name; }

    @Override public void add(FileSystemNode child)    { children.add(child); }
    @Override public void remove(FileSystemNode child) { children.remove(child); }
    @Override public FileSystemNode getChild(int i)    { return children.get(i); }

    @Override public long getSize() {                     // recursive case
        long total = 0;
        for (FileSystemNode child : children) total += child.getSize(); // delegate, don't type-check
        return total;
    }
    @Override public void print(String indent) {
        System.out.println(indent + "📁 " + name + "/");
        for (FileSystemNode child : children) child.print(indent + "  ");
    }
}

// CLIENT — treats leaf and composite identically; never checks a type.
class Client {
    public static void main(String[] args) {
        Directory root = new Directory("root");
        root.add(new TextFile("readme.md", 100));
        Directory src = new Directory("src");
        src.add(new TextFile("Main.java", 500));
        src.add(new TextFile("Util.java", 300));
        root.add(src);

        System.out.println("Total: " + root.getSize() + "B"); // 900 — recursion is hidden
        root.print("");
        // Danger: this compiles but throws at runtime —
        // new TextFile("x", 1).add(new TextFile("y", 2));  // UnsupportedOperationException
    }
}
```
</details>

**Pros:** Maximum uniformity — client code can call *any* method on *any* node without casting; adding a child looks the same everywhere; matches GoF's recommended default.
**Cons:** Leaves expose child operations they can't fulfill → runtime `UnsupportedOperationException` on calls the compiler accepts; violates the Interface Segregation Principle and has a Liskov smell.
**Mechanism:** the shared supertype declares child ops so *every* reference is polymorphically substitutable — safety is traded away for that substitutability, and the risk is deferred to runtime.

---

### Variant 2: Safe Composite (child ops only in the Composite)

**What problem it solves:** Move `add`/`remove`/`getChild` *off* the Component interface and put them **only on `Directory`**. Now the type system guarantees you can never call `add()` on a `File` — it won't compile. This is the **safe** variant: leaves and composites are no longer 100% interchangeable, so any code that manages children must hold a `Directory` reference (or check/cast once at the boundary). You trade some transparency for compile-time safety. Many production codebases and modern style guides prefer this because "fails at compile time" beats "throws at runtime."

<details>
<summary>💻 Click to expand code — safe composite</summary>

```java
import java.util.*;

// COMPONENT — ONLY the business operations. No child management here.
interface FileSystemNode {
    String getName();
    long getSize();
    void print(String indent);
}

// LEAF — cannot accidentally expose child ops; there are none to expose.
class TextFile implements FileSystemNode {
    private final String name; private final long size;
    TextFile(String name, long size) { this.name = name; this.size = size; }
    public String getName() { return name; }
    public long getSize()   { return size; }
    public void print(String indent) { System.out.println(indent + "📄 " + name + " (" + size + "B)"); }
}

// COMPOSITE — child management lives HERE only.
class Directory implements FileSystemNode {
    private final String name;
    private final List<FileSystemNode> children = new ArrayList<>();
    Directory(String name) { this.name = name; }
    public String getName() { return name; }

    // These exist only on Directory — the compiler stops file.add(...).
    public Directory add(FileSystemNode child) { children.add(child); return this; } // fluent
    public void remove(FileSystemNode child)   { children.remove(child); }
    public List<FileSystemNode> getChildren()  { return Collections.unmodifiableList(children); }

    public long getSize() {
        long total = 0;
        for (FileSystemNode c : children) total += c.getSize();
        return total;
    }
    public void print(String indent) {
        System.out.println(indent + "📁 " + name + "/");
        for (FileSystemNode c : children) c.print(indent + "  ");
    }
}
```
</details>

**Pros:** Compile-time safety — impossible to call `add()` on a leaf; honors Interface Segregation and Liskov; the Component interface stays lean and honest.
**Cons:** Less uniform — client code that mutates the tree must know it's holding a `Directory`; generic tree-building code sometimes needs a cast or an `instanceof` at the boundary; slightly less "pure" than GoF transparency.
**Mechanism:** *interface segregation* — child-mutation capability is a narrower type (`Directory`) than the read/operate capability (`FileSystemNode`), so the type checker enforces the constraint the runtime otherwise would.

---

### Variant 3: Production Composite (parent pointers, caching, iterator)

**What problem it solves:** Real trees need more than `getSize()`. You often want to walk *up* (find a node's full path, or bubble an event to the root), to avoid re-summing an unchanged subtree on every call, and to iterate the whole tree without exposing its internal list. This production variant adds a **parent pointer** (maintained as an invariant on `add`/`remove`), a **cached size** invalidated on mutation, and an **`Iterator` (depth-first)** so clients traverse without touching internals. This is the shape you'd defend in a system-design-flavored interview.

<details>
<summary>💻 Click to expand code — production composite</summary>

```java
import java.util.*;

abstract class FileSystemNode {
    protected final String name;
    protected Directory parent;                     // parent pointer — enables upward navigation
    protected FileSystemNode(String name) { this.name = name; }

    public String getName()  { return name; }
    public Directory getParent() { return parent; }
    public abstract long getSize();
    public abstract void print(String indent);

    // Full path by walking parent pointers up to the root.
    public String getPath() {
        Deque<String> parts = new ArrayDeque<>();
        for (FileSystemNode n = this; n != null; n = n.parent) parts.addFirst(n.name);
        return String.join("/", parts);
    }
}

final class TextFile extends FileSystemNode {
    private final long size;
    TextFile(String name, long size) { super(name); this.size = size; }
    public long getSize() { return size; }
    public void print(String indent) { System.out.println(indent + "📄 " + name + " (" + size + "B)"); }
}

final class Directory extends FileSystemNode implements Iterable<FileSystemNode> {
    private final List<FileSystemNode> children = new ArrayList<>();
    private long cachedSize = -1;                    // -1 = dirty; lazily recomputed

    Directory(String name) { super(name); }

    public Directory add(FileSystemNode child) {
        if (child.parent != null) child.parent.remove(child); // maintain single-parent invariant
        child.parent = this;
        children.add(child);
        invalidateUpToRoot();                        // my size and every ancestor's is now stale
        return this;
    }
    public void remove(FileSystemNode child) {
        if (children.remove(child)) { child.parent = null; invalidateUpToRoot(); }
    }

    @Override public long getSize() {
        if (cachedSize < 0) {                        // recompute once, then serve from cache
            long total = 0;
            for (FileSystemNode c : children) total += c.getSize();
            cachedSize = total;
        }
        return cachedSize;
    }

    private void invalidateUpToRoot() {
        for (Directory d = this; d != null; d = (Directory) d.parent) d.cachedSize = -1;
    }

    @Override public void print(String indent) {
        System.out.println(indent + "📁 " + name + "/ (" + getSize() + "B)");
        for (FileSystemNode c : children) c.print(indent + "  ");
    }

    // Depth-first iterator — clients traverse without seeing the internal list.
    @Override public Iterator<FileSystemNode> iterator() {
        Deque<FileSystemNode> stack = new ArrayDeque<>(children);
        return new Iterator<>() {
            public boolean hasNext() { return !stack.isEmpty(); }
            public FileSystemNode next() {
                FileSystemNode n = stack.pop();
                if (n instanceof Directory d) d.children.forEach(stack::push);
                return n;
            }
        };
    }
}
```
</details>

**Pros:** Upward navigation via parent pointers (paths, event bubbling); O(1) repeat `getSize()` via caching; safe traversal via `Iterator`; single-parent invariant prevents accidental tree corruption.
**Cons:** More moving parts; caching + parent pointers introduce invariants you must maintain on *every* mutation (miss one and you get stale sizes or dangling parents); not thread-safe as written.
**Mechanism:** *invariant maintenance on mutation* — `add`/`remove` are the only paths that change structure, so they're the single place to reassign `parent` and invalidate the cache up to the root. Centralizing mutation is what keeps the derived data (path, size) correct.

---

## 🎨 Real-World Example

**Scenario:** an **organization chart with salary/headcount rollups** — a favorite FAANG design exercise. An `Employee` (individual contributor) is a leaf; a `Manager` is a composite of subordinates who may themselves be managers. The CFO wants "total compensation of the whole org" and "headcount under any manager" with the *same* call at *any* node. This is Composite verbatim, and it also shows a composite operation that aggregates a non-trivial computation (sum of comp + count) recursively.

<details>
<summary>💻 Click to expand code — org chart with rollups + demo</summary>

```java
import java.util.*;

/** COMPONENT: everything an org node can answer, leaf or manager alike. */
abstract class OrgNode {
    protected final String name;
    protected final long salary;
    protected OrgNode(String name, long salary) { this.name = name; this.salary = salary; }

    public abstract long totalCompensation();   // rollup: this node + everyone beneath
    public abstract int  headcount();            // rollup: number of people in subtree
    public abstract void printOrg(String indent);

    // Convenience shared by all nodes.
    public String getName() { return name; }
}

/** LEAF: an individual contributor — no reports. */
final class Employee extends OrgNode {
    Employee(String name, long salary) { super(name, salary); }
    @Override public long totalCompensation() { return salary; }        // base case
    @Override public int  headcount()         { return 1; }             // base case
    @Override public void printOrg(String indent) {
        System.out.println(indent + "• " + name + " ($" + salary + ")");
    }
}

/** COMPOSITE: a manager — has a salary AND a team of OrgNodes. */
final class Manager extends OrgNode {
    private final List<OrgNode> reports = new ArrayList<>();
    Manager(String name, long salary) { super(name, salary); }

    public Manager addReport(OrgNode node) { reports.add(node); return this; }

    @Override public long totalCompensation() {
        long total = salary;                                            // manager's own comp
        for (OrgNode r : reports) total += r.totalCompensation();       // + recurse into team
        return total;
    }
    @Override public int headcount() {
        int count = 1;                                                  // count the manager
        for (OrgNode r : reports) count += r.headcount();              // + everyone below
        return count;
    }
    @Override public void printOrg(String indent) {
        System.out.println(indent + "▸ " + name + " ($" + salary + ") — manages " + reports.size());
        for (OrgNode r : reports) r.printOrg(indent + "    ");
    }
}

/** DEMO — the client treats a single employee and a whole org identically. */
public class OrgChartDemo {
    public static void main(String[] args) {
        Employee alice = new Employee("Alice", 150_000);
        Employee bob   = new Employee("Bob",   140_000);

        Manager eng = new Manager("Eng-Lead Carol", 220_000)
                .addReport(alice)
                .addReport(bob);

        Manager cto = new Manager("CTO Dave", 400_000)
                .addReport(eng)
                .addReport(new Employee("Erin (staff eng)", 260_000));

        // Same two calls work on a leaf and on the root of the whole company:
        System.out.println("Alice comp:  $" + alice.totalCompensation()); // $150000  (leaf)
        System.out.println("Eng comp:    $" + eng.totalCompensation());   // $510000  (subtree)
        System.out.println("Company comp: $" + cto.totalCompensation());  // $1170000 (whole tree)
        System.out.println("Company headcount: " + cto.headcount());      // 5

        cto.printOrg("");
        /* Output:
           ▸ CTO Dave ($400000) — manages 2
               ▸ Eng-Lead Carol ($220000) — manages 2
                   • Alice ($150000)
                   • Bob ($140000)
               • Erin (staff eng) ($260000)
        */
    }
}
```
</details>

Notice that `totalCompensation()` and `headcount()` are written **once per node type** with a clean base case (leaf) and recursive case (composite). The client (`OrgChartDemo`) never writes a loop, never checks a type, and calls the exact same method on `alice` (a leaf), `eng` (a subtree), and `cto` (the whole company). That uniformity is the entire payoff of Composite.

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- You need to represent **part-whole hierarchies** — trees where a node is either a primitive or a container of nodes, nested to arbitrary depth.
- You want clients to **ignore the difference** between individual objects and compositions, calling the same operations on both.
- The structure is **recursive by nature**: file systems, GUI component trees, scene graphs, DOM/HTML, org charts, menu systems, expression/AST trees, bill-of-materials, nested UI layouts.
- You want to add **new operations that traverse the whole tree** (size, count, render, serialize, validate) and have them work uniformly at every level.
- The set of node *types* is relatively stable, but the *shapes* of trees vary widely at runtime.

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- The structure is **not actually a tree** — it's flat, or a general graph with cycles/shared nodes (Composite assumes a tree; cycles break naive recursion and single-parent invariants).
- Leaves and composites have **almost nothing in common** — forcing a shared interface produces a bloated Component full of methods half the types can't honor (a smell toward `UnsupportedOperationException`).
- You need **strong compile-time guarantees** that certain operations only apply to containers, and the transparent design's runtime failures are unacceptable (then prefer the safe variant — or reconsider Composite entirely).
- The hierarchy is **fixed and shallow** (e.g., always exactly "order → line items") — a plain `List` inside one class is simpler than a full Component/Leaf/Composite trio.
- Operations differ so much per node type that a **Visitor** (to separate algorithms from structure) or a discriminated union would serve better.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- **Uniformity:** clients treat leaves and composites the same — no type-checking, no branching, no duplicated recursion.
- **Open/Closed friendly:** add a new leaf or composite type without touching existing client code or traversals.
- **Natural recursion:** the tree structure and its operations mirror the domain; base case = leaf, recursive case = composite.
- **Composability:** build arbitrarily complex trees from simple parts at runtime.

**Cons**

- **Over-general interface:** to keep things uniform (transparent variant), the Component may declare methods that leaves can't meaningfully implement → runtime errors / ISP violation.
- **Type-safety vs. transparency tension:** you must choose which to sacrifice; neither choice is free.
- **Hard to restrict components:** the type system can't easily say "this composite may only contain these leaf types" without extra runtime checks.
- **Traversal cost & correctness:** deep trees make recursion expensive (and risk stack overflow); derived data (sizes, parent pointers) needs careful invalidation.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

| Pattern | Core intent | Relationship to Composite |
|---|---|---|
| **Decorator** | Add responsibilities to an object dynamically, keeping its interface | Structurally similar (both hold a reference of the shared interface and forward calls), but Decorator has **exactly one** child and *adds behavior*; Composite has **many** children and *aggregates* them. |
| **Iterator** | Traverse a collection's elements without exposing its structure | Frequently paired with Composite to walk the tree (DFS/BFS) without the client touching the child list. |
| **Visitor** | Represent an operation to perform on elements of an object structure | The go-to companion when you have *many* operations over a *stable* Composite tree — Visitor externalizes algorithms so you don't bloat every node type with each new operation. |
| **Chain of Responsibility** | Pass a request along a chain until handled | Often layered onto a Composite's parent pointers so an unhandled request bubbles up to ancestors. |

**The classic confusion — Composite vs. Decorator:** both are "a wrapper implementing the same interface as what it wraps." The distinguishing question: *how many children, and why?* Composite = **a list of children, purpose is aggregation into a whole**. Decorator = **one child, purpose is adding a responsibility**. If your `render()` loops over children, it's Composite; if it calls one `super.render()` and adds a border, it's Decorator.

</details>

## 📊 Comparison Table (of variants)

<details>
<summary>📖 Click to expand</summary>

| Axis | V0: No Composite | V1: Transparent | V2: Safe | V3: Production |
|---|---|---|---|---|
| Child ops location | free functions | on Component (leaves throw) | on Composite only | on Composite only |
| Leaf/composite interchangeable? | no (instanceof) | fully (compile-time) | partially | partially |
| Fails when? | — | **runtime** (`UnsupportedOperationException` on leaf) | **compile-time** | compile-time |
| ISP / Liskov | violated | violated (leaf can't honor add) | honored | honored |
| Upward navigation | no | no | no | **yes** (parent pointers) |
| Repeat-query cost | recompute | recompute | recompute | **O(1) cached** |
| Extensible (new node type) | painful (edit all) | easy | easy | easy |
| Complexity | low but leaky | low | low–medium | medium–high |
| Interview default | never | GoF's default answer | modern-safety answer | system-design answer |

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

**Pitfall 1 — Transparent-interface runtime explosions.** Declaring `add()`/`remove()` on the Component and letting leaves throw means perfectly-compiling code blows up at runtime.

<details>
<summary>💻 The failure</summary>

```java
FileSystemNode node = getSomeNode();   // could be a File
node.add(new TextFile("x", 1));        // compiles fine... throws UnsupportedOperationException if leaf
```
</details>

<details>
<summary>💻 The fix — use the safe variant (child ops only on Composite)</summary>

```java
if (node instanceof Directory dir) {   // one deliberate check at the boundary
    dir.add(new TextFile("x", 1));     // compiler now guarantees this is legal
}
// or design so you only ever hold Directory references where you mutate the tree.
```
</details>

**Pitfall 2 — Cycles and shared subtrees.** Composite assumes a *tree*. If a node ends up as its own ancestor (a cycle), recursive `getSize()`/`print()` loops forever; if a node is shared by two parents, mutations and parent-pointer invariants become ambiguous.

<details>
<summary>💻 The failure</summary>

```java
Directory a = new Directory("a");
Directory b = new Directory("b");
a.add(b);
b.add(a);                 // cycle! a contains b contains a...
a.getSize();              // StackOverflowError
```
</details>

<details>
<summary>💻 The fix — guard against cycles on add (and enforce single parent)</summary>

```java
public Directory add(FileSystemNode child) {
    for (FileSystemNode n = this; n != null; n = n.getParent())
        if (n == child) throw new IllegalArgumentException("Cycle: node is an ancestor");
    if (child.getParent() != null) child.getParent().remove(child); // single-parent invariant
    child.parent = this; children.add(child);
    return this;
}
```
</details>

**Pitfall 3 — Stale derived data after mutation.** If you cache a subtree's size (or count, or bounding box) and forget to invalidate it up the ancestor chain when a child is added/removed, queries return wrong answers silently.

<details>
<summary>💻 The failure</summary>

```java
dir.getSize();            // caches, say, 900
dir.add(new TextFile("big", 5000));  // forgot to invalidate...
dir.getSize();            // still returns 900 — WRONG (should be 5900)
```
</details>

<details>
<summary>💻 The fix — invalidate the cache up to the root on every mutation</summary>

```java
public Directory add(FileSystemNode child) {
    children.add(child); child.parent = this;
    for (Directory d = this; d != null; d = (Directory) d.parent) d.cachedSize = -1; // dirty chain
    return this;
}
```
</details>

**Pitfall 4 — Deep recursion → StackOverflowError.** A pathologically deep tree (e.g., a linked-list-shaped directory millions deep) overflows the call stack. For untrusted or unbounded depth, convert recursion to an explicit stack/queue (iterative traversal).

<details>
<summary>💻 The fix — iterative DFS instead of recursive</summary>

```java
long iterativeSize(FileSystemNode root) {
    long total = 0; Deque<FileSystemNode> stack = new ArrayDeque<>(); stack.push(root);
    while (!stack.isEmpty()) {
        FileSystemNode n = stack.pop();
        if (n instanceof Directory d) d.getChildren().forEach(stack::push);
        else total += n.getSize();
    }
    return total;
}
```
</details>

</details>

## 🎓 Interview Tips

<details>
<summary>📖 What interviewers commonly ask</summary>

- "Design a file system / render an HTML DOM / evaluate an arithmetic expression / model an org chart." All of these are Composite — recognize the recursive part-whole shape immediately.
- "Where do you put `add()`/`remove()` — on the base interface or only on the composite?" This is the **transparent vs. safe** question; they want you to articulate the trade-off, not just pick one.
- "How does the client avoid checking whether it's holding a leaf or a group?" Answer: polymorphism through the shared Component type; the recursion lives inside the composite.
- "How would you traverse it? Iteratively vs. recursively?" Be ready to discuss stack overflow on deep trees and an explicit-stack DFS.

</details>

<details>
<summary>📖 What you should proactively mention</summary>

- Name the **transparent-vs-safe trade-off** unprompted and state your choice with a reason ("I'll use the safe variant so `file.add()` fails at compile time rather than runtime").
- Mention **Visitor** as the companion when there are many operations over a stable tree — it keeps you from bloating each node type with every new algorithm.
- Call out **cycle prevention** and the **single-parent invariant** — it shows you think about correctness, not just the happy path.
- Bring up **caching derived aggregates** (size/count) and invalidating them up the ancestor chain — a system-design signal.
- Note that **AWT/Swing, the DOM, and `java.io.File`** are real-world Composites — grounding the pattern in shipped code is a strong signal.

</details>

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Iterator** — traverse the composite tree without exposing its internal child collections.
- **Visitor** — apply many distinct operations across a stable composite structure without modifying node classes.
- **Decorator** — same "implements-the-interface-it-wraps" shape, but one wrapped child + adds behavior (vs. many children + aggregation).
- **Chain of Responsibility** — leverage the composite's parent pointers to bubble unhandled requests upward.
- **Builder** — often used to construct complex composite trees fluently.
- **Flyweight** — share leaf objects when a composite tree contains huge numbers of identical leaves (e.g., document characters).

</details>

## 📚 Library/Framework Implementation

**1. `java.awt.Component` / `java.awt.Container` (AWT & Swing).** The textbook JDK Composite. `Component` is the leaf-capable base; `Container` (a `Component` that holds `Component`s) is the composite. `Container.add(Component)` builds the tree; `paint()`, `doLayout()`, and event dispatch all recurse through it. Swing's `JComponent`/`JPanel`/`JFrame` extend this — a whole window paints itself by recursing into its children.

<details>
<summary>💻 Click to expand code — AWT/Swing composite in action</summary>

```java
import javax.swing.*;
import java.awt.*;

JPanel panel = new JPanel();               // Container = Composite
panel.add(new JButton("OK"));              // JButton = leaf Component
panel.add(new JButton("Cancel"));
JPanel toolbar = new JPanel();
toolbar.add(new JLabel("Tools"));
panel.add(toolbar);                        // a Container inside a Container — recursive nesting

JFrame frame = new JFrame();               // also a Container
frame.add(panel);
// frame.pack()/repaint() recurse: layout + paint traverse the whole component tree uniformly.
```
</details>

**2. The DOM (`org.w3c.dom.Node`).** Every DOM node — `Element`, `Text`, `Comment`, `Document` — implements `Node`. `Node.getChildNodes()`, `appendChild()`, and `removeChild()` are the composite child-management ops; elements are composites, text nodes are leaves. XML/HTML parsers build a Composite tree, and serialization/XPath walk it uniformly. (JavaScript's browser DOM is the same design.)

<details>
<summary>💻 Click to expand code — DOM as a Composite</summary>

```java
import org.w3c.dom.*;
import javax.xml.parsers.*;

Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
Element root = doc.createElement("html");          // composite
Element body = doc.createElement("body");          // composite
body.appendChild(doc.createTextNode("Hello"));     // leaf inside composite
root.appendChild(body);
doc.appendChild(root);
// Walking the tree: each Node exposes getChildNodes() uniformly, leaf or element alike.
```
</details>

**3. `java.io.File` (and modern `java.nio.file.Path`).** A `File` transparently represents both a regular file (leaf) and a directory (composite). `file.isDirectory()` and `file.listFiles()` expose the composite structure; recursive size/search utilities (or `Files.walk`) treat both uniformly. It's a *transparent* composite (a File instance can be either), which is why you must check `isDirectory()` — a real-world illustration of the transparency trade-off.

<details>
<summary>💻 Click to expand code — recursive size over java.io.File</summary>

```java
import java.io.File;

static long size(File node) {                     // one method, both cases
    if (node.isFile()) return node.length();      // leaf
    long total = 0;
    File[] kids = node.listFiles();
    if (kids != null) for (File k : kids) total += size(k); // recurse (composite)
    return total;
}
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1: [Conceptual] What is the Composite pattern and what problem does it solve?</strong></summary>

Composite is a structural GoF pattern that composes objects into **tree structures to represent part-whole hierarchies**, letting clients treat individual objects (leaves) and compositions (composites) **uniformly** through a shared interface. The problem it solves is the proliferation of `instanceof`/branching code that arises when clients must distinguish "one thing" from "a group of things" while walking a recursive structure. By defining a common `Component` interface implemented by both `Leaf` and `Composite`, the recursion and the leaf/composite distinction move *inside* the type hierarchy — a leaf is the base case, a composite the recursive case — so the client calls one method (`getSize()`, `render()`) and never branches on type. Classic domains: file systems, GUI widget trees, org charts, and expression/AST trees.

</details>

<details>
<summary><strong>Q2: [Conceptual] Name the participants in the Composite pattern and their roles.</strong></summary>

There are four participants. **Component** is the abstraction (interface or abstract class) declaring operations common to both simple and complex elements — and, optionally, child-management operations. **Leaf** represents primitive objects with no children; it implements the Component operations by doing the actual work (it's the recursion's base case). **Composite** stores child Components (usually a `List<Component>`), implements child-management (`add`/`remove`/`getChild`), and implements the business operations by iterating children and aggregating results (the recursive case). **Client** manipulates objects only through the Component interface, remaining oblivious to whether any given reference is a leaf or a composite. The key structural fact: a Composite *is-a* Component and *holds* Components, which is what enables arbitrary nesting.

</details>

<details>
<summary><strong>Q3: [Conceptual] Why is Composite called a "recursive" pattern?</strong></summary>

Because a Composite holds references of the very interface it implements (`Composite` implements `Component` and contains a `List<Component>`), the structure is self-referential — a tree. Operations follow the same recursion: invoking `getSize()` on the root composite delegates to each child's `getSize()`; children that are composites recurse further, while leaf children return concrete values that bubble up and aggregate. This is precisely a recursive algorithm expressed in objects: the **base case** is the Leaf (returns a value directly) and the **recursive case** is the Composite (combines the results of its children). The pattern is the object-oriented embodiment of a recursive data structure plus a fold over it.

</details>

<details>
<summary><strong>Q4: [Conceptual] What is the difference between a "transparent" and a "safe" Composite?</strong></summary>

It's about *where the child-management methods live*. In a **transparent** composite, `add`/`remove`/`getChild` are declared on the **Component interface** itself, so leaves and composites are perfectly interchangeable — you can call `add()` on any `Component` reference. The cost: leaves have no children, so they must implement those methods by throwing `UnsupportedOperationException`, deferring failure to *runtime* and violating Interface Segregation / Liskov. In a **safe** composite, child-management lives **only on the Composite class**, so the compiler prevents `leaf.add(...)` entirely — failure is caught at *compile time*. The trade-off is fundamental: transparency maximizes uniformity (GoF's stated preference) while safety maximizes type-safety (often preferred in modern production code).

</details>

<details>
<summary><strong>Q5: [Implementation] Implement a basic file-system Composite that computes total size.</strong></summary>

The essence is a `FileSystemNode` interface with a `getSize()` implemented directly by `File` (base case) and recursively by `Directory` (sum over children).

<details>
<summary>💻 Solution</summary>

```java
interface FileSystemNode { long getSize(); }

class TextFile implements FileSystemNode {
    private final long size;
    TextFile(long size) { this.size = size; }
    public long getSize() { return size; }                 // base case
}

class Directory implements FileSystemNode {
    private final List<FileSystemNode> children = new ArrayList<>();
    public Directory add(FileSystemNode n) { children.add(n); return this; }
    public long getSize() {                                  // recursive case
        long total = 0;
        for (FileSystemNode c : children) total += c.getSize();
        return total;
    }
}
```
</details>

The client calls `root.getSize()` and the recursion + aggregation happen entirely inside `Directory` — no type-checking anywhere.

</details>

<details>
<summary><strong>Q6: [Implementation] How do you add a "count nodes" or "search by name" operation without breaking the pattern?</strong></summary>

Add the operation to the `Component` interface and implement it in both Leaf and Composite — the Leaf provides the base case, the Composite recurses. The beauty is that no client code changes and no traversal is duplicated.

<details>
<summary>💻 Solution</summary>

```java
interface FileSystemNode {
    long getSize();
    Optional<FileSystemNode> findByName(String name);   // new op
}
class TextFile implements FileSystemNode {
    private final String name; /* ... */
    public Optional<FileSystemNode> findByName(String n) {
        return name.equals(n) ? Optional.of(this) : Optional.empty();  // base case
    }
}
class Directory implements FileSystemNode {
    private final String name; private final List<FileSystemNode> children = new ArrayList<>();
    public Optional<FileSystemNode> findByName(String n) {
        if (name.equals(n)) return Optional.of(this);
        for (FileSystemNode c : children) {                            // recurse
            var hit = c.findByName(n);
            if (hit.isPresent()) return hit;
        }
        return Optional.empty();
    }
}
```
</details>

Note: if you anticipate *many* such operations over a *stable* tree, prefer a **Visitor** so you don't keep growing the node interface.

</details>

<details>
<summary><strong>Q7: [Implementation] Implement an arithmetic expression tree with Composite and evaluate (3 + 4) * 5.</strong></summary>

Numbers are leaves; binary operations are composites holding two operand nodes.

<details>
<summary>💻 Solution</summary>

```java
interface Expr { double evaluate(); }

class Num implements Expr {                       // leaf
    private final double v; Num(double v) { this.v = v; }
    public double evaluate() { return v; }
}
class BinOp implements Expr {                     // composite (exactly two children)
    enum Op { ADD, MUL }
    private final Expr left, right; private final Op op;
    BinOp(Op op, Expr l, Expr r) { this.op = op; this.left = l; this.right = r; }
    public double evaluate() {
        double a = left.evaluate(), b = right.evaluate();  // recurse into operands
        return switch (op) { case ADD -> a + b; case MUL -> a * b; };
    }
}
// (3 + 4) * 5
Expr e = new BinOp(BinOp.Op.MUL, new BinOp(BinOp.Op.ADD, new Num(3), new Num(4)), new Num(5));
System.out.println(e.evaluate()); // 35.0
```
</details>

This is how interpreters and spreadsheet engines evaluate formulas — a Composite of expression nodes.

</details>

<details>
<summary><strong>Q8: [Implementation] Add a depth-first Iterator so clients can traverse the tree without touching internals.</strong></summary>

Make the Composite implement `Iterable<Component>` and return an iterator backed by an explicit stack for DFS (which also avoids recursion depth limits).

<details>
<summary>💻 Solution</summary>

```java
class Directory implements FileSystemNode, Iterable<FileSystemNode> {
    private final List<FileSystemNode> children = new ArrayList<>();
    /* ... add/getSize ... */
    public Iterator<FileSystemNode> iterator() {
        Deque<FileSystemNode> stack = new ArrayDeque<>(children);
        return new Iterator<>() {
            public boolean hasNext() { return !stack.isEmpty(); }
            public FileSystemNode next() {
                FileSystemNode n = stack.pop();
                if (n instanceof Directory d) d.children.forEach(stack::push);
                return n;
            }
        };
    }
}
```
</details>

Now `for (FileSystemNode n : dir)` walks the whole subtree, and the internal `List` stays encapsulated — this is Composite + Iterator working together.

</details>

<details>
<summary><strong>Q9: [Breaking] How can a transparent Composite blow up at runtime, and how do you prevent it?</strong></summary>

In a transparent composite, `add`/`remove` are on the `Component` interface, so calling `leaf.add(child)` **compiles** but throws `UnsupportedOperationException` because a leaf has no children. This is dangerous because the type system claims the operation is legal — the failure only surfaces at runtime, possibly deep in production. Prevention options: (1) use the **safe variant** (child ops only on the Composite class) so the compiler rejects `leaf.add`; (2) if you must stay transparent, at least make the failure loud, documented, and covered by tests; (3) use `instanceof`/pattern-matching at the single boundary where you mutate the tree. The interview point is recognizing that transparency trades compile-time safety for uniformity.

</details>

<details>
<summary><strong>Q10: [Breaking] What happens if your Composite tree contains a cycle, and how do you guard against it?</strong></summary>

Composite fundamentally assumes an acyclic tree. If a node becomes its own ancestor (`a.add(b); b.add(a)`), any recursive operation — `getSize()`, `print()`, iteration — recurses forever and throws `StackOverflowError` (or spins indefinitely with an explicit stack). Guarding requires a check on `add()`: walk the parent chain (or the subtree) and reject the insertion if the candidate child is already an ancestor. You should also enforce the **single-parent invariant** (remove a node from its old parent before re-parenting) so the structure stays a tree rather than a DAG. Mentioning both cycle-prevention and single-parent enforcement signals you're thinking about structural correctness, not just the happy path.

</details>

<details>
<summary><strong>Q11: [Breaking] You cache a directory's size. What bug appears and how do you fix it?</strong></summary>

If you memoize `getSize()` in each Composite but forget to **invalidate** the cache when the subtree changes, queries silently return stale values: add a 5 KB file to a directory that already cached 900 B, and it keeps reporting 900 B. The fix is to treat mutation as the single choke point: every `add`/`remove` must mark the cache dirty *for this node and all its ancestors up to the root*, because a change to a leaf changes the size of every enclosing directory. This is a general lesson about derived/aggregate data over trees — the correctness burden lives entirely in the mutation methods, so centralize mutation and always propagate invalidation upward via parent pointers.

</details>

<details>
<summary><strong>Q12: [Breaking] Why can a naive recursive traversal fail on a very deep tree, and what's the remedy?</strong></summary>

Each level of recursion consumes a JVM stack frame. A pathologically deep tree — say a directory nested a million levels — exhausts the thread's stack and throws `StackOverflowError` before you ever run out of heap. The remedy is to convert the recursion into an **iterative traversal using an explicit `Deque`/stack** (for DFS) or `Queue` (for BFS) on the heap, which scales to whatever memory allows. For trusted, shallow trees recursion is cleaner and fine; for untrusted input or unbounded depth (parsing arbitrary user data), always prefer iterative traversal. This is a real concern for parsers and serializers processing adversarial nested input (a known DoS vector).

</details>

<details>
<summary><strong>Q13: [Trade-off] Composite vs. Decorator — they look identical. How do you tell them apart?</strong></summary>

Both are "a class that implements an interface and holds a reference to that same interface, forwarding calls." The distinguishing questions are *how many children* and *why*. **Composite holds many children and its purpose is aggregation** — combining child results into a whole (a directory's size is the sum of its contents). **Decorator holds exactly one child and its purpose is augmentation** — wrapping a single component to add a responsibility (a `BufferedInputStream` wraps one stream and adds buffering). Concretely: if your `operation()` contains a `for` loop over children, it's Composite; if it calls `wrapped.operation()` once and adds behavior around it, it's Decorator. They're often combined — a decorated node inside a composite tree.

</details>

<details>
<summary><strong>Q14: [Trade-off] When would you choose Visitor over adding methods to the Composite?</strong></summary>

Composite handles the *structure*; the question is where the *operations* live. Adding each new operation directly to the `Component` interface works well when operations are **few and stable** but node types are **many/growing** — you touch every node type once per operation, which is the natural Composite grain. But when operations are **many and growing** over a **stable set of node types**, spreading every algorithm across every node class becomes unwieldy and mixes unrelated concerns. **Visitor** externalizes operations into separate visitor classes, so you add `SizeVisitor`, `SearchVisitor`, `SerializeVisitor` without modifying node classes. The trade-off is the classic "expression problem": Composite-with-methods is easy to add *types*, Visitor is easy to add *operations*.

</details>

<details>
<summary><strong>Q15: [Trade-off] Should the Component be an interface or an abstract class?</strong></summary>

Use an **interface** when nodes only need to share behavior contracts and you want to allow multiple inheritance of type (a node could also be `Serializable`, `Comparable`, etc.) — and with Java's `default` methods you can even supply the throwing child-op defaults for a transparent composite. Use an **abstract class** when leaves and composites share *state or common implementation* — e.g., a `name` field, a `parent` pointer, a `getPath()` implemented once in the base. In practice production composites often use an abstract base class precisely because parent pointers and shared helpers benefit from a single implementation. The decision hinges on whether you're sharing *contract only* (interface) or *contract + state/code* (abstract class).

</details>

<details>
<summary><strong>Q16: [Advanced] How do you make a Composite tree thread-safe?</strong></summary>

The mutable parts are the children lists (and any caches/parent pointers). Options, in order of increasing sophistication: (1) **Immutability** — build the tree once, then never mutate; immutable trees are trivially safe to share and read concurrently (favor this whenever the structure is fixed after construction). (2) **Coarse locking** — guard the whole tree with one lock; simple but a bottleneck. (3) **Concurrent collections + careful invariants** — use `CopyOnWriteArrayList` for child lists that are read-heavy and rarely mutated, but beware that multi-step invariants (single-parent, cache invalidation up the chain) aren't atomic across nodes, so you may still need per-subtree locking. Crucially, cached aggregates that invalidate *up to the root* create cross-node dependencies that are hard to make lock-free — often the cleanest answer is "make it immutable, or recompute on read." A strong answer names immutability first.

</details>

<details>
<summary><strong>Q17: [Advanced] How would you serialize a large Composite tree and reconstruct it, handling shared/absent nodes?</strong></summary>

Serialize by walking the tree and emitting each node with its type discriminator and children (JSON/protobuf). Two subtleties: **depth** — a naive recursive serializer overflows on deep trees, so use iterative traversal or bounded depth; and **identity** — if the tree is truly a tree, plain nesting works, but if nodes are *shared* (a DAG), you must assign IDs and emit references to avoid duplicating (and infinitely expanding on cycles) subtrees. On reconstruction, parse into node instances, re-link children, and **rebuild derived data** (parent pointers, cached sizes) rather than trusting serialized copies of it. Java's built-in `Serializable` handles object graphs and shared references automatically via its reference-tracking mechanism, but it's slow, insecure against crafted input, and couples you to class internals — a custom, schema-based format is the production choice.

</details>

<details>
<summary><strong>Q18: [Advanced/Coding Challenge] Implement a generic Composite with a Visitor to run multiple operations over a UI component tree.</strong></summary>

Combine Composite (structure) with Visitor (operations) — the FAANG "expression problem" showcase.

<details>
<summary>💻 Full solution</summary>

```java
import java.util.*;

// ---- Visitor ----
interface UIVisitor {
    void visit(Button b);
    void visit(Panel p);
}

// ---- Composite structure ----
interface UIComponent { void accept(UIVisitor v); }

class Button implements UIComponent {                 // leaf
    final String label; Button(String label) { this.label = label; }
    public void accept(UIVisitor v) { v.visit(this); }
}
class Panel implements UIComponent {                  // composite
    final String name; final List<UIComponent> children = new ArrayList<>();
    Panel(String name) { this.name = name; }
    public Panel add(UIComponent c) { children.add(c); return this; }
    public void accept(UIVisitor v) {
        v.visit(this);
        for (UIComponent c : children) c.accept(v);   // recurse — visitor sees whole tree
    }
}

// ---- Two operations, zero changes to node classes ----
class CountButtonsVisitor implements UIVisitor {
    int count = 0;
    public void visit(Button b) { count++; }
    public void visit(Panel p) { /* nothing */ }
}
class RenderVisitor implements UIVisitor {
    public void visit(Button b) { System.out.println("[Button: " + b.label + "]"); }
    public void visit(Panel p)  { System.out.println("<Panel: " + p.name + ">"); }
}

class Demo {
    public static void main(String[] args) {
        Panel root = new Panel("root")
            .add(new Button("OK"))
            .add(new Panel("toolbar").add(new Button("Save")).add(new Button("Load")))
            .add(new Button("Cancel"));

        var counter = new CountButtonsVisitor();
        root.accept(counter);
        System.out.println("Buttons: " + counter.count);  // 4

        root.accept(new RenderVisitor());                  // renders whole tree
    }
}
```
</details>

Adding a new operation = a new visitor, no node-class edits. Adding a new node type = touch every visitor — the inherent Visitor trade-off.

</details>

<details>
<summary><strong>Q19: [Advanced/Coding Challenge] Design a menu system (nested menus + items) where selecting an item runs an action, using Composite.</strong></summary>

Menu items are leaves with an action; sub-menus are composites. `display()` recurses; a leaf's `select()` runs its action.

<details>
<summary>💻 Full solution</summary>

```java
import java.util.*;

abstract class MenuComponent {
    protected final String title;
    protected MenuComponent(String title) { this.title = title; }
    public abstract void display(String indent);
    public void add(MenuComponent c) { throw new UnsupportedOperationException(); }
    public void select()             { throw new UnsupportedOperationException(); }
}

class MenuItem extends MenuComponent {                 // leaf
    private final Runnable action;
    MenuItem(String title, Runnable action) { super(title); this.action = action; }
    public void display(String indent) { System.out.println(indent + "- " + title); }
    @Override public void select() { System.out.println("Running: " + title); action.run(); }
}

class Menu extends MenuComponent {                     // composite
    private final List<MenuComponent> items = new ArrayList<>();
    Menu(String title) { super(title); }
    @Override public void add(MenuComponent c) { items.add(c); }
    @Override public void display(String indent) {
        System.out.println(indent + "+ " + title);
        for (MenuComponent c : items) c.display(indent + "   ");  // recurse
    }
}

class MenuDemo {
    public static void main(String[] args) {
        Menu root = new Menu("Main");
        Menu file = new Menu("File");
        file.add(new MenuItem("New",  () -> System.out.println("created")));
        file.add(new MenuItem("Open", () -> System.out.println("opened")));
        root.add(file);
        root.add(new MenuItem("Quit", () -> System.out.println("bye")));

        root.display("");                 // renders the nested menu tree
        file.getClass();                  // ...
        // Select a leaf action:
        new MenuItem("Save", () -> System.out.println("saved")).select();
    }
}
```
</details>

This is the exact example from the *Head First Design Patterns* book and mirrors real GUI/CLI menu frameworks — a transparent composite (note the throwing `add`/`select` defaults).

</details>

<details>
<summary><strong>Q20: [Advanced] How does the DOM / a browser use Composite, and what performance concern does that create?</strong></summary>

The DOM is a Composite: every node (`Document`, `Element`, `Text`, `Comment`) implements a common `Node` interface with `appendChild`/`removeChild`/`childNodes`. Rendering, layout (reflow), and style computation recurse through this tree. The performance concern is that **operations propagate through the tree**: a single mutation (adding a node, changing a style) can invalidate layout for a whole subtree or the entire document, triggering expensive reflow/repaint. This is why front-end frameworks (React's virtual DOM) *batch* mutations and *diff* two composite trees to compute a minimal set of real-DOM changes — a direct application of Composite plus a tree-diffing algorithm to bound the cost of propagation. It mirrors the "invalidate up the ancestor chain" concern from caching, at browser scale.

</details>

### 🧠 Staff / Principal Engineer Level

<details>
<summary><strong>SP1: [Staff] How do you keep a Composite tree's derived/aggregate data (sizes, counts, bounding boxes) correct and cheap at scale?</strong></summary>

Treat mutation as the single source of truth and push **incremental maintenance** into `add`/`remove`. Two viable strategies: (1) **lazy invalidation** — mark the node and its ancestors dirty on mutation, recompute on next read (great for read-light, write-heavy periods, amortizes recomputation); (2) **eager incremental update** — on adding a child of size *s*, add *s* to this node's cached size and propagate the delta up the parent chain (O(depth) per mutation, O(1) reads — ideal when reads dominate). Both rely on **parent pointers** to walk upward. At scale you also cap tree depth (or balance it) to bound the O(depth) propagation, and you make the invariant impossible to violate by funnelling *all* structural changes through the two mutation methods — no public access to the child list. The principal-level insight is that correctness of derived data is a *design* problem (centralize mutation) not a *discipline* problem (remember to invalidate).

</details>

<details>
<summary><strong>SP2: [Staff] A Composite tree is shared across many threads with mostly reads and occasional structural edits. Design for concurrency.</strong></summary>

Start by asking whether the tree can be **immutable after construction** — if so, share freely, no locks, and represent "edits" as building a new tree and swapping an `AtomicReference<Node> root` (copy-on-write at the root, structural sharing of unchanged subtrees to bound copy cost — this is how persistent data structures work). If in-place mutation is required, isolate it: use `CopyOnWriteArrayList` for read-dominated child lists, and protect multi-node invariants (single-parent, cache invalidation up the chain) with per-subtree locks acquired in a consistent order to avoid deadlock. Avoid a single global lock (bottleneck) and avoid fine-grained lock-free schemes here — the cross-node ancestor-invalidation dependency makes lock-free correctness very hard. The senior signal is leading with immutability + structural sharing rather than reaching for locks.

</details>

<details>
<summary><strong>SP3: [Principal] Your team keeps bloating the Component interface as every feature adds a method. How do you stop the erosion?</strong></summary>

This is the classic Composite failure mode: the shared interface accretes methods that only some node types can honor, and leaves fill up with `UnsupportedOperationException`. The structural fix is to **separate structure from operations** via **Visitor** — the `Component` interface exposes just `accept(Visitor)` plus truly-universal structural methods, and every feature becomes a visitor class rather than a new interface method. Where the double-dispatch of Visitor is too heavy, consider **capability interfaces** (small role interfaces a node implements only if it supports them, checked at the boundary) or an **externalized operation registry**. Organizationally, put a review gate on the Component interface: adding a method there should require justification that it's genuinely universal. The principal framing is recognizing this as the *expression problem* and choosing your axis of extension deliberately (types vs. operations) rather than letting the interface grow by default.

</details>

<details>
<summary><strong>SP4: [Principal] How would you design a Composite that must load lazily from a slow backing store (e.g., a remote file system or huge catalog)?</strong></summary>

Introduce a **virtual/lazy composite** where children are not materialized until first accessed — the node holds enough metadata (a key/handle, cached size if the store provides it) and populates its child list on demand inside `getChildren()`, caching the result. This is Composite + **Proxy** (a lazy-loading proxy stands in for an unloaded subtree). Key design points: bound memory with an **eviction policy** (LRU on subtrees, or weak references) so a giant tree doesn't pin the whole store in heap; make loading **thread-safe** (double-checked or `computeIfAbsent`) so concurrent traversals don't double-fetch; expose async loading (`CompletableFuture`) if the backing store is remote so you don't block the traversal thread; and prefetch/batch child loads to amortize round-trips. Aggregate queries like `getSize()` must decide between "cheap approximate from metadata" versus "exact, forcing a full load" — expose both. This is exactly how IDE project trees, cloud storage browsers, and DB catalog explorers work.

</details>

<details>
<summary><strong>SP5: [Principal] Contrast modeling a hierarchy as a Composite object tree vs. persisting it in a relational/graph database. When does the pattern break down?</strong></summary>

The Composite object tree is an in-memory representation optimized for traversal and polymorphic operations; a database needs a *storage* model. Common persistence encodings: **adjacency list** (each row has a `parent_id` — simple writes, but computing a subtree needs recursive CTEs or N queries), **materialized path** (store `/root/src/main` — fast subtree/prefix queries, costly moves), **nested set** (left/right bounds — fast reads, expensive writes), or a **graph DB** (natural fit, native traversal). The pattern "breaks down" when the hierarchy is really a **DAG or cyclic graph** (shared nodes, multiple parents) — Composite's single-parent tree assumption no longer holds, and you should model it as a graph with explicit edges and handle cycles/visited-sets in traversal. It also breaks when the tree is **too large to hold in memory**, pushing you to lazy/virtual composites (see SP4) or to doing aggregation in the database (recursive SQL) instead of by walking objects. The principal-level judgment is separating the *domain* pattern (Composite for in-memory operations) from the *persistence* strategy, and recognizing when "tree" is the wrong abstraction entirely.

</details>

---

## ⚡ Quick Revision

**One-liner:** Composite composes objects into a tree of leaves and containers behind one shared interface, so clients treat a single object and a whole subtree identically — the recursion lives inside the type, not the client.

**The whole pattern in one paragraph:** Composite is a structural GoF pattern — *"compose objects into tree structures to represent part-whole hierarchies; lets clients treat individual objects and compositions uniformly."* It defines a **Component** interface implemented by both **Leaf** (primitive, no children — the recursion's base case) and **Composite** (holds a `List<Component>` and implements each operation by delegating to children and aggregating — the recursive case). Because a Composite *is-a* Component and *holds* Components, trees nest to any depth, and polymorphism erases the leaf/composite distinction so the **Client** never type-checks or writes a traversal loop. The central design decision is **transparent vs. safe**: put child-management (`add`/`remove`) on the Component (transparent — fully interchangeable, but leaves throw `UnsupportedOperationException` at *runtime* and it violates ISP/Liskov) or only on the Composite (safe — the compiler blocks `leaf.add()`, at the cost of some uniformity). Production versions add **parent pointers** (upward navigation, paths, event bubbling), **cached aggregates** invalidated up the ancestor chain on mutation, **cycle prevention** + a **single-parent invariant**, and **iterative traversal** to avoid `StackOverflowError` on deep trees. It pairs naturally with **Iterator** (traversal) and **Visitor** (many operations over a stable tree — the answer to interface bloat / the expression problem), and is distinguished from **Decorator** by "many children + aggregation" vs "one child + added behavior." Canonical real-world composites: `java.awt.Container`/Swing `JComponent`, the DOM `Node`, and `java.io.File`.

**Top 5 interview answers to memorize:**

1. **"What is it?"** → Compose objects into a tree of part-whole hierarchies and treat leaves and composites uniformly through a shared Component interface; leaf = base case, composite = recursive case.
2. **"Transparent vs. safe?"** → Transparent puts `add`/`remove` on the Component (interchangeable but leaves throw at runtime); safe puts them only on the Composite (compiler-enforced, less uniform). Safe is usually the better production choice.
3. **"Composite vs. Decorator?"** → Both wrap the shared interface, but Composite holds *many* children for *aggregation*; Decorator holds *one* child to *add behavior*. `for` loop over children ⇒ Composite.
4. **"When add Visitor?"** → When operations are many/growing over a stable set of node types — externalize algorithms into visitors instead of bloating the Component interface (the expression problem).
5. **"How does it break?"** → Runtime `UnsupportedOperationException` on transparent leaves; cycles → `StackOverflowError`; stale cached aggregates when mutation doesn't invalidate up the chain; deep trees overflow the stack (use iterative traversal).

**Trigger words (hear these → think Composite):** "part-whole", "tree structure", "nested", "hierarchy", "treat individual and group the same", "file system / directory", "GUI component / widget tree", "org chart / reporting structure", "menu with submenus", "DOM / HTML / XML tree", "expression / AST / parse tree", "recursively compute total (size/cost/count)", "arbitrary nesting depth", "render the whole tree", "bill of materials", "scene graph".

---

*End of Composite Pattern study guide.*




# Iterator Pattern ⭐⭐⭐ (Difficulty: 3/5 — the core is easy to grasp, but fail-fast internals, laziness, and Spliterator/parallel design get deep fast)

> **Category:** Behavioral Pattern (GoF)
> **Also known as:** Cursor

The Iterator pattern gives you a **uniform way to walk the elements of a collection without exposing how that collection is built inside**. It's the pattern behind every `for (X x : collection)` loop you've ever written, behind `java.util.Iterator`, `Scanner`, `ResultSet`, and Java Streams' `Spliterator`. Its power is that the *same* traversal code works over an `ArrayList`, a `HashSet`, a tree, a database cursor, or an infinite lazily-generated sequence — because all of them hand you a cursor that answers just two questions: *is there more?* and *give me the next one.*

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 0: Exposing the Internal Collection / Index-Coupled Traversal (Anti-pattern)](#variant-0-exposing-the-internal-collection--index-coupled-traversal-anti-pattern)
   - [Variant 1: A Basic Custom Iterator Interface + ConcreteIterator](#variant-1-a-basic-custom-iterator-interface--concreteiterator)
   - [Variant 2: Implementing java.util.Iterator + Iterable (idiomatic, for-each)](#variant-2-implementing-javautiliterator--iterable-idiomatic-for-each)
   - [Variant 3: Fail-Fast Iterator with modCount (Concurrent Modification Detection)](#variant-3-fail-fast-iterator-with-modcount-concurrent-modification-detection)
   - [Variant 4: Lazy / Infinite / Generator-Style Iterator](#variant-4-lazy--infinite--generator-style-iterator)
   - [Variant 5: Production-Grade — Paginated REST API Iterator & Tree (DFS/BFS) Traversal](#variant-5-production-grade--paginated-rest-api-iterator--tree-dfsbfs-traversal)
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

> **GoF Definition:** *"Provide a way to access the elements of an aggregate object sequentially without exposing its underlying representation."*

The Iterator pattern exists to solve one core tension: you want client code to **traverse a collection's elements one by one**, but you *don't* want that client code to depend on *how* the collection stores those elements. An `ArrayList` stores elements in a backing array; a `LinkedList` in chained nodes; a `HashSet` in buckets; a tree in parent/child links. If traversal code had to know which one it was walking, every consumer would break the moment you swapped the data structure.

The key insight is to **extract the responsibility of traversal into a separate object — the *iterator* (a cursor)** — that knows how to walk *one specific* aggregate but exposes a tiny, uniform interface: *"is there a next element?"* (`hasNext()`) and *"give it to me and advance"* (`next()`). The collection (the *Aggregate* / *Iterable*) becomes a factory that produces these cursors. Because the cursor holds the traversal state (the current position), you can have **multiple independent traversals** of the same collection at once, and you can define **different traversal orders** (forward, reverse, DFS, BFS) as different iterators — all without bloating the collection's own interface or leaking its internals.

---

## 🎯 Problem

You have a collection of objects and you need to visit each element. The naive approach is to expose the collection's internals (its backing array, its size, its node links) so callers can loop over them directly. But this couples every caller to the exact storage strategy, makes it impossible to offer alternative traversal orders cleanly, prevents having more than one traversal in flight, and forces the collection's public interface to grow traversal methods it shouldn't own.

**The pain points that lead you to Iterator:**

- **Traversal code is coupled to the collection's representation.** If clients index into a backing array, switching to a linked structure breaks them all.
- **You want a uniform traversal API across different data structures** so the *same* algorithm (print all, sum, filter) works over lists, sets, trees, and streams unchanged.
- **You need multiple simultaneous, independent traversals** of the same collection (two cursors at two positions) — impossible if the position lives on the collection itself.
- **You want different traversal orders** (forward/reverse, in-order/pre-order, BFS/DFS) without stuffing all of them into the collection class.
- **The collection shouldn't have to expose or grow traversal bookkeeping** — that's a separate responsibility (Single Responsibility Principle).

**Concrete example scenarios:**

1. **The for-each loop (the everyday one).** `for (String s : list)` must work identically whether `list` is an `ArrayList`, `LinkedList`, or a custom collection. The compiler desugars this into `Iterator` calls, so *any* `Iterable` plugs in — the archetypal use of the pattern.

2. **Paginated REST APIs.** You call `GET /users?page=1`, get 100 users plus a `nextPageToken`, and want to *treat the whole result set as one sequence* — `for (User u : allUsers)` — while the iterator transparently fetches the next page only when you exhaust the current one. The caller never sees pagination.

3. **Tree / filesystem traversal.** Walking a directory tree or a DOM/AST in depth-first or breadth-first order. The traversal order is a property of the *iterator*, not the tree, so the tree stays simple and you can offer both a `dfsIterator()` and a `bfsIterator()`.

4. **Streaming / huge or infinite data.** Reading lines from a multi-gigabyte file, rows from a database `ResultSet`, or an unbounded sequence (Fibonacci, natural numbers). You cannot load all of it into memory — a **lazy** iterator produces each element on demand, so memory stays flat regardless of size.

---

## ✅ Solution

The core idea, in plain language: **give the collection a method that returns a fresh *cursor* object. The cursor holds the current position and exposes just `hasNext()` and `next()`. Client code loops on the cursor, oblivious to whether it's walking an array, a linked list, a tree, or a network stream.** To offer another traversal order or another simultaneous walk, you just hand out a different (or another) cursor — the collection's own interface never changes.

**Key structural elements:**

- **Iterator (interface):** declares the traversal operations — `hasNext()` (is there another element?), `next()` (return the current element and advance), and optionally `remove()` (delete the last-returned element through the cursor). This is the uniform contract every cursor honors.
- **ConcreteIterator:** implements `Iterator` for **one specific aggregate**. It holds a reference to the aggregate (or its internals) plus the **traversal state** — a cursor index, a current node pointer, a stack/queue for tree walks — and knows how to advance through *that* structure.
- **Aggregate / Iterable (interface):** declares a factory method — `iterator()` in Java — that **creates and returns a fresh iterator**. Anything implementing `java.lang.Iterable` works with the enhanced for-loop.
- **ConcreteAggregate:** the actual collection (`ArrayList`, a custom `NameRepository`, a `Tree`). Its `iterator()` returns a `ConcreteIterator` bound to it.

**External vs. internal iteration** — a distinction interviewers love:

- **External iteration:** the *client* controls the loop and explicitly pulls each element (`while (it.hasNext()) use(it.next());`). Java's `Iterator` and for-each are external. The client decides when to advance and can stop early.
- **Internal iteration:** the *collection* controls the loop; you hand it a function and it applies it to each element (`list.forEach(x -> use(x))`, `stream.map(...).filter(...)`). The traversal mechanics are hidden entirely; the client only says *what* to do per element, not *how* to iterate.

**The mechanism that makes it work:** *the traversal state lives on the cursor, not the collection, and the cursor is produced by a factory method on the collection.* Because state is on the cursor, calling `iterator()` twice yields two **independent** cursors — two loops can run at different positions simultaneously without interfering. Because the cursor implements a **uniform interface**, all traversal algorithms depend only on `Iterator`/`Iterable` (dependency inversion), so the concrete data structure is swappable. And because the cursor *encapsulates advancement*, you can make it **lazy** — computing or fetching each element only when `next()` is called — which is what lets a single abstraction cover both a 3-element list and an infinite sequence or a paginated API.

---

## 💻 Implementation

We'll model a **collection of names** and then a **tree** and a **paginated API**, evolving from leaking the collection's internals to a production-grade lazy/pageable iterator. Each variant fixes a specific weakness of the previous one.

### Variant 0: Exposing the Internal Collection / Index-Coupled Traversal (Anti-pattern)

**What's wrong with it:** Before showing Iterator, understand the disease it cures. The collection exposes its backing `String[]` (or its size + an `int get(int)` accessor), and every caller writes an index loop coupled to that array representation. Switch the backing store to a `LinkedList`, a `Set`, or a tree and **every caller breaks**. There's no way to offer a reverse or filtered traversal without adding yet more accessors, no way to run two independent traversals cleanly, and the collection's interface is now polluted with representation details it shouldn't expose.

<details>
<summary>💻 Click to expand code — the anti-pattern</summary>

```java
// DON'T DO THIS — callers are coupled to the backing array representation.
class NameRepository {
    private String[] names = {"Alice", "Bob", "Charlie"};
    private int size = 3;

    // Leaks internals: the caller must know it's an indexable array.
    public String[] getNames() { return names; }   // hands out the raw store
    public int getSize()       { return size; }
    public String get(int i)   { return names[i]; }
}

class Client {
    void printAll(NameRepository repo) {
        // Traversal logic is coupled to "it's an array with an int index".
        for (int i = 0; i < repo.getSize(); i++) {   // if repo becomes a LinkedList/Set/Tree,
            System.out.println(repo.get(i));          // this loop is wrong or impossible.
        }
        // Want reverse order? Filtered? A second simultaneous walk? Add more accessors + more coupling.
    }
}
```
</details>

**Pros:** Trivial for a fixed array; no extra classes; the loop is right there in the caller.
**Cons:** Every caller is coupled to the storage strategy (array + integer index); changing the representation breaks all clients; no clean way to add alternative traversal orders, filtering, laziness, or independent simultaneous walks; the collection leaks its internals (breaks encapsulation and SRP); `getNames()` even hands out a reference to the mutable backing array.
**Mechanism (why it's fragile):** traversal state (the index `i`) and knowledge of the representation live *in the caller*, so there is no seam between "how the data is stored" and "how it's walked." Any storage change ripples out to every consumer.

---

### Variant 1: A Basic Custom Iterator Interface + ConcreteIterator

**What problem it solves:** Introduce a small `Iterator` interface (`hasNext()`, `next()`) and an `Aggregate` interface with a `createIterator()` factory method. The repository returns a `ConcreteIterator` that holds the traversal state (a cursor index) *internally*, so the caller loops on `hasNext()`/`next()` and no longer knows or cares that the backing store is an array. Swap the array for a linked list and only the iterator changes — every caller is untouched.

<details>
<summary>💻 Click to expand code — basic custom iterator</summary>

```java
// The uniform traversal contract (GoF's own Iterator interface).
interface Iterator<T> {
    boolean hasNext();
    T next();
}

// The Aggregate: a factory for iterators — hides how elements are stored.
interface Aggregate<T> {
    Iterator<T> createIterator();
}

class NameRepository implements Aggregate<String> {
    private final String[] names = {"Alice", "Bob", "Charlie"};

    @Override public Iterator<String> createIterator() {
        return new NameIterator();   // fresh cursor, bound to this repo
    }

    // Inner class sees the backing store but nobody outside does.
    private class NameIterator implements Iterator<String> {
        private int cursor = 0;                     // traversal state lives HERE, not on the repo
        @Override public boolean hasNext() { return cursor < names.length; }
        @Override public String next() {
            if (!hasNext()) throw new java.util.NoSuchElementException();
            return names[cursor++];                 // return current, then advance
        }
    }
}

class Client {
    public static void main(String[] args) {
        NameRepository repo = new NameRepository();
        Iterator<String> it = repo.createIterator();
        while (it.hasNext()) {
            System.out.println(it.next());          // Alice, Bob, Charlie
        }
        // Two independent cursors — each has its own position:
        Iterator<String> a = repo.createIterator();
        Iterator<String> b = repo.createIterator();
        a.next();                                    // a at Bob
        System.out.println(b.next());                // b still at Alice
    }
}
```
</details>

**Pros:** Callers depend only on the `Iterator` interface, not the representation; the backing store can change with zero caller impact; traversal state is on the cursor, so **independent simultaneous walks** work; the collection's interface stays clean (just `createIterator()`).
**Cons:** It's a *custom* interface, so it doesn't work with Java's enhanced for-loop (that needs `java.lang.Iterable`/`java.util.Iterator`); no `remove()`; every collection reinvents this contract; `next()` on an exhausted iterator must be handled (throw `NoSuchElementException`).
**Mechanism:** *traversal state extracted onto a cursor produced by a factory method*. The inner class `NameIterator` can see `names` (encapsulation preserved — outsiders can't), while exposing only `hasNext`/`next`. A fresh call to `createIterator()` yields a fresh `cursor`, giving each traversal its own independent position.

---

### Variant 2: Implementing java.util.Iterator + Iterable (idiomatic, for-each)

**What problem it solves:** Instead of a hand-rolled interface, implement the JDK's `java.lang.Iterable<T>` (one method: `iterator()`) and `java.util.Iterator<T>` (`hasNext`, `next`, and a default `remove`). This makes your collection a **first-class citizen of the language**: it works directly with the enhanced for-loop (`for (T t : myCollection)`), with `forEach`, and with anything that accepts an `Iterable`. This is what you should almost always do in real Java.

<details>
<summary>💻 Click to expand code — idiomatic Iterable + for-each</summary>

```java
import java.util.Iterator;
import java.util.NoSuchElementException;

// Implement java.lang.Iterable -> the enhanced for-loop works automatically.
class NameRepository implements Iterable<String> {
    private final String[] names = {"Alice", "Bob", "Charlie"};

    @Override public Iterator<String> iterator() {
        return new Iterator<>() {                    // anonymous class, Java 8+ diamond
            private int cursor = 0;
            @Override public boolean hasNext() { return cursor < names.length; }
            @Override public String next() {
                if (!hasNext()) throw new NoSuchElementException();
                return names[cursor++];
            }
            // remove() is a JDK default that throws UnsupportedOperationException — fine here,
            // since an array-backed store can't cheaply remove. Override it if removal is supported.
        };
    }
}

class Client {
    public static void main(String[] args) {
        NameRepository repo = new NameRepository();

        // The whole point: the enhanced for-loop just works.
        for (String name : repo) {                   // desugars to iterator()/hasNext()/next()
            System.out.println(name);                // Alice, Bob, Charlie
        }

        // And internal iteration via forEach (Iterable default method):
        repo.forEach(System.out::println);
    }
}
```
</details>

The compiler **desugars** `for (String name : repo)` into roughly:

<details>
<summary>💻 Click to expand — what for-each compiles to</summary>

```java
for (Iterator<String> it = repo.iterator(); it.hasNext(); ) {
    String name = it.next();
    System.out.println(name);
}
```
</details>

**Pros:** Works with the enhanced for-loop, `forEach`, and every API that takes an `Iterable` (huge interoperability win); uses the standard, well-understood contract; `Iterable` gives you a free `forEach` and `spliterator()` default; readers instantly recognize the idiom.
**Cons:** Anonymous/inner iterators capturing mutable outer state can be surprising; the default `remove()` throws unless you implement it; still no protection against the collection being mutated mid-iteration (that's Variant 3); a `next()` without checking `hasNext()` still throws `NoSuchElementException`.
**Mechanism:** *the language contract*. The enhanced for-loop is pure syntactic sugar over `Iterable.iterator()` + `Iterator.hasNext()/next()`. By implementing the standard interfaces, your type becomes indistinguishable from a JDK collection to all traversal code — the ultimate expression of "uniform traversal, hidden representation."

---

### Variant 3: Fail-Fast Iterator with modCount (Concurrent Modification Detection)

**What problem it solves:** A subtle, dangerous bug: if the collection is *structurally modified* (add/remove) **while** an iterator is walking it, the cursor's position becomes meaningless — it can skip elements, revisit them, or read stale slots. JDK collections defend against this by being **fail-fast**: they detect the modification and throw `ConcurrentModificationException` immediately, rather than silently returning corrupt results. The mechanism is a **`modCount`** — a counter of structural modifications — that the iterator snapshots at creation and re-checks on every `next()`.

<details>
<summary>💻 Click to expand code — fail-fast iterator with modCount</summary>

```java
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

class FastList<T> implements Iterable<T> {
    private Object[] data = new Object[16];
    private int size = 0;
    private int modCount = 0;          // # of STRUCTURAL modifications (add/remove)

    public void add(T item) {
        if (size == data.length) data = java.util.Arrays.copyOf(data, size * 2);
        data[size++] = item;
        modCount++;                    // structural change -> bump the counter
    }
    public T removeAt(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        @SuppressWarnings("unchecked") T old = (T) data[index];
        System.arraycopy(data, index + 1, data, index, size - index - 1);
        data[--size] = null;
        modCount++;                    // structural change -> bump the counter
        return old;
    }

    @Override public Iterator<T> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<T> {
        private int cursor = 0;
        private int lastRet = -1;
        private int expectedModCount = modCount;    // snapshot at creation

        @Override public boolean hasNext() { return cursor < size; }

        @Override public T next() {
            checkForComodification();               // detect interleaved structural change
            if (cursor >= size) throw new NoSuchElementException();
            @SuppressWarnings("unchecked") T item = (T) data[cursor];
            lastRet = cursor;
            cursor++;
            return item;
        }

        // The safe way to remove DURING iteration: go through the iterator itself.
        @Override public void remove() {
            if (lastRet < 0) throw new IllegalStateException();
            checkForComodification();
            FastList.this.removeAt(lastRet);         // mutate the list...
            cursor = lastRet;                         // ...and fix the cursor
            lastRet = -1;
            expectedModCount = modCount;              // re-sync so we don't trip our own check
        }

        private void checkForComodification() {
            if (modCount != expectedModCount)         // list changed behind our back?
                throw new ConcurrentModificationException();
        }
    }
}

class Client {
    public static void main(String[] args) {
        FastList<String> list = new FastList<>();
        list.add("a"); list.add("b"); list.add("c");

        // BAD: structural modification during iteration -> fail-fast throws.
        try {
            for (String s : list) {
                if (s.equals("b")) list.removeAt(1);   // modifies list, not via iterator
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("Caught CME — modified during iteration");
        }

        // GOOD: remove via the iterator (updates expectedModCount).
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().equals("a")) it.remove();    // safe removal
        }
    }
}
```
</details>

**Pros:** Turns silent data corruption into a **loud, immediate failure** (`ConcurrentModificationException`) — you find the bug in development, not in production with wrong results; the iterator's own `remove()` provides a *safe* way to mutate during traversal; `modCount` is cheap (a single int compare per `next()`).
**Cons:** Fail-fast is **best-effort, not guaranteed** — it detects most misuse but the check is unsynchronized, so it can miss races and must not be relied on for correctness in concurrent code; a shared `modCount` under real multithreading has visibility issues (`modCount` isn't `volatile` in the JDK); the mechanism protects a *single-threaded* misuse pattern, not thread safety. For concurrent access you need a **fail-safe** (snapshot) iterator like `CopyOnWriteArrayList`'s.
**Mechanism (why `modCount` makes it fail-fast):** the collection maintains `modCount`, incremented on every *structural* modification (add/remove that changes size). When an iterator is created it copies `modCount` into `expectedModCount`. On each `next()` (and `remove()`), it compares the two; a mismatch means "the collection changed since I started" → throw immediately. Removing *through the iterator* re-syncs `expectedModCount = modCount`, which is precisely why iterator-driven removal is the only safe in-loop mutation.

---

### Variant 4: Lazy / Infinite / Generator-Style Iterator

**What problem it solves:** So far the elements all exist up front in a backing store. But an iterator doesn't have to be backed by stored data — it can **compute or fetch each element on demand** inside `next()`. This lets one uniform abstraction cover **infinite sequences** (Fibonacci, natural numbers), **huge data** you can't hold in memory (file lines, DB rows), and **generated** data — all with flat, constant memory, because only the *current* element (and minimal state) is materialized at a time.

<details>
<summary>💻 Click to expand code — lazy infinite Fibonacci iterator</summary>

```java
import java.util.Iterator;

// An INFINITE sequence: hasNext() is always true; next() computes on demand.
class FibonacciIterator implements Iterator<Long> {
    private long a = 0, b = 1;
    @Override public boolean hasNext() { return true; }      // never ends
    @Override public Long next() {
        long current = a;
        long nextB = a + b;
        a = b;
        b = nextB;
        return current;                                       // 0,1,1,2,3,5,8,...
    }
}

// Wrap in an Iterable so it composes with for-each / streams (bounded by the caller!).
class Fibonacci implements Iterable<Long> {
    @Override public Iterator<Long> iterator() { return new FibonacciIterator(); }
}

class Client {
    public static void main(String[] args) {
        Fibonacci fibs = new Fibonacci();
        int count = 0;
        for (long f : fibs) {                 // MUST bound it yourself — the iterator is infinite
            System.out.print(f + " ");
            if (++count == 10) break;         // 0 1 1 2 3 5 8 13 21 34
        }

        // Bridge to a Stream and take a finite prefix (lazy all the way):
        java.util.stream.Stream
            .iterate(new long[]{0, 1}, x -> new long[]{x[1], x[0] + x[1]})
            .limit(10)
            .forEach(x -> System.out.print(x[0] + " "));
    }
}
```
</details>

A lazily-computed **range** iterator (like Python's `range`) that never materializes the numbers:

<details>
<summary>💻 Click to expand — lazy range iterator (constant memory)</summary>

```java
import java.util.Iterator;
import java.util.NoSuchElementException;

class Range implements Iterable<Integer> {
    private final int start, end, step;
    Range(int start, int end, int step) { this.start = start; this.end = end; this.step = step; }

    @Override public Iterator<Integer> iterator() {
        return new Iterator<>() {
            private int current = start;
            @Override public boolean hasNext() { return step > 0 ? current < end : current > end; }
            @Override public Integer next() {
                if (!hasNext()) throw new NoSuchElementException();
                int val = current;
                current += step;                 // compute the next value lazily; store nothing
                return val;
            }
        };
    }
}
// for (int i : new Range(0, 1_000_000_000, 1)) { ... }  // O(1) memory, no billion-element array
```
</details>

**Pros:** **Constant memory** regardless of sequence length — you never materialize the whole collection; supports **infinite** sequences (impossible with an eager collection); elements are computed **only if consumed** (short-circuiting `break`/`limit` avoids wasted work); the same `Iterable` contract composes with for-each and Streams.
**Cons:** An infinite iterator will hang forever if the caller forgets to bound it (`limit`/`break`); `hasNext()` may need to *pre-compute or peek* the next element (e.g., reading a file line to know if one exists), which complicates state; not repeatable — once consumed, a lazy stream/iterator is exhausted (you must create a fresh one); side-effecting `next()` (I/O) makes error handling and resource cleanup trickier.
**Mechanism:** *deferred production in `next()`*. Instead of walking stored elements, the iterator holds just enough **state to produce the next value** (`a`, `b` for Fibonacci; `current` for range) and generates it when asked. `hasNext()` encodes the termination rule (always-true for infinite, a bound for range, or a peek/read for I/O). This "pull one at a time" model is exactly how `BufferedReader.lines()`, `Stream.iterate`, and generators work.

---

### Variant 5: Production-Grade — Paginated REST API Iterator & Tree (DFS/BFS) Traversal

**Why it's recommended:** Two production shapes dominate real systems. (a) A **paginated API iterator** that presents a multi-page HTTP result set as one flat sequence, **fetching the next page only when the current page is exhausted** — the caller writes `for (User u : api.allUsers())` and never sees paging, tokens, or HTTP. (b) A **tree traversal iterator** where the *order* (DFS vs. BFS) is a property of the iterator (backed by an explicit stack or queue — an internal state machine), so the tree stays a plain data structure and you can offer multiple orders. Both are canonical FAANG "code an iterator" questions.

<details>
<summary>💻 Click to expand code — lazy paginated API iterator (auto-fetch next page)</summary>

```java
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

// A page of results + a token to fetch the next page (null when no more pages).
record Page<T>(List<T> items, String nextPageToken) {}

// The data source: given a page token (null = first page), return that page.
interface PageFetcher<T> {
    Page<T> fetch(String pageToken);
}

// Presents all pages as ONE lazy sequence. Fetches a page only when the current one runs out.
class PaginatedIterator<T> implements Iterator<T> {
    private final PageFetcher<T> fetcher;
    private Iterator<T> currentPage;     // iterator over the current page's items
    private String nextToken;            // token for the NEXT page to fetch
    private boolean firstFetchDone = false;

    PaginatedIterator(PageFetcher<T> fetcher) { this.fetcher = fetcher; }

    @Override public boolean hasNext() {
        // If the current page still has items, we're done.
        if (currentPage != null && currentPage.hasNext()) return true;
        // Otherwise, try to advance to the next page (lazily, on demand).
        return advanceToNextNonEmptyPage();
    }

    @Override public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        return currentPage.next();
    }

    // Fetch pages until we find one with items, or run out of pages.
    private boolean advanceToNextNonEmptyPage() {
        while (true) {
            if (!firstFetchDone) {                      // very first fetch (token = null)
                Page<T> page = fetcher.fetch(null);
                firstFetchDone = true;
                currentPage = page.items().iterator();
                nextToken = page.nextPageToken();
                if (currentPage.hasNext()) return true;
                if (nextToken == null) return false;    // no items, no more pages
            } else {
                if (nextToken == null) return false;    // exhausted all pages
                Page<T> page = fetcher.fetch(nextToken);// network call ONLY when needed
                currentPage = page.items().iterator();
                nextToken = page.nextPageToken();
                if (currentPage.hasNext()) return true;
                // empty page but more tokens? loop and fetch again
            }
        }
    }
}

// Wrap as Iterable so callers use a plain for-each.
class UserApi implements Iterable<String> {
    private final PageFetcher<String> fetcher;
    UserApi(PageFetcher<String> fetcher) { this.fetcher = fetcher; }
    @Override public Iterator<String> iterator() { return new PaginatedIterator<>(fetcher); }
}

class Client {
    public static void main(String[] args) {
        // Simulate a 3-page API: page tokens "p1"->"p2"->null.
        PageFetcher<String> fakeApi = token -> {
            if (token == null)  return new Page<>(List.of("u1", "u2"), "p1");
            if (token.equals("p1")) return new Page<>(List.of("u3", "u4"), "p2");
            if (token.equals("p2")) return new Page<>(List.of("u5"), null);
            throw new IllegalArgumentException("bad token " + token);
        };

        UserApi api = new UserApi(fakeApi);
        for (String user : api) {          // pages fetched lazily behind the scenes
            System.out.println(user);      // u1, u2, u3, u4, u5
        }
        // Only 3 HTTP calls total, made on demand — caller never sees pagination.
    }
}
```
</details>

<details>
<summary>💻 Click to expand code — tree DFS & BFS iterators (explicit stack/queue state machine)</summary>

```java
import java.util.*;

class TreeNode<T> {
    final T value;
    final List<TreeNode<T>> children = new ArrayList<>();
    TreeNode(T value) { this.value = value; }
    TreeNode<T> add(TreeNode<T> child) { children.add(child); return this; }
}

// DFS (pre-order) iterator: an explicit stack IS the traversal state machine.
class DepthFirstIterator<T> implements Iterator<T> {
    private final Deque<TreeNode<T>> stack = new ArrayDeque<>();
    DepthFirstIterator(TreeNode<T> root) { if (root != null) stack.push(root); }

    @Override public boolean hasNext() { return !stack.isEmpty(); }
    @Override public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        TreeNode<T> node = stack.pop();
        // Push children in reverse so leftmost is visited first (pre-order).
        for (int i = node.children.size() - 1; i >= 0; i--) stack.push(node.children.get(i));
        return node.value;
    }
}

// BFS (level-order) iterator: same structure, a QUEUE instead of a stack.
class BreadthFirstIterator<T> implements Iterator<T> {
    private final Queue<TreeNode<T>> queue = new LinkedList<>();
    BreadthFirstIterator(TreeNode<T> root) { if (root != null) queue.add(root); }

    @Override public boolean hasNext() { return !queue.isEmpty(); }
    @Override public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        TreeNode<T> node = queue.poll();
        queue.addAll(node.children);
        return node.value;
    }
}

// The tree exposes BOTH orders as different Iterables — the tree itself stays dumb.
class Tree<T> {
    private final TreeNode<T> root;
    Tree(TreeNode<T> root) { this.root = root; }
    Iterable<T> depthFirst()   { return () -> new DepthFirstIterator<>(root); }
    Iterable<T> breadthFirst() { return () -> new BreadthFirstIterator<>(root); }
}

class Client {
    public static void main(String[] args) {
        //        A
        //       / \
        //      B   C
        //     / \   \
        //    D   E   F
        TreeNode<String> a = new TreeNode<>("A");
        TreeNode<String> b = new TreeNode<>("B"), c = new TreeNode<>("C");
        a.add(b).add(c);
        b.add(new TreeNode<>("D")).add(new TreeNode<>("E"));
        c.add(new TreeNode<>("F"));
        Tree<String> tree = new Tree<>(a);

        for (String v : tree.depthFirst())   System.out.print(v + " "); // A B D E C F
        System.out.println();
        for (String v : tree.breadthFirst()) System.out.print(v + " "); // A B C D E F
    }
}
```
</details>

**Pros:** The paginated iterator gives callers a **single flat sequence** while fetching lazily (minimal HTTP calls, flat memory) — pagination is completely hidden; the tree iterators put **traversal order on the cursor**, so the tree class stays a plain data holder and you can offer DFS *and* BFS (and reverse, in-order, etc.) as independent iterables; both are **composable** with for-each and Streams. This is production-shaped code (it's exactly how the Google/AWS SDK "paged iterables" work).
**Cons:** Lazy network iterators mix **I/O with iteration** — exceptions can surface from `hasNext()`/`next()`, retries/timeouts/rate-limits need handling, and resources (connections) may need closing (favor `AutoCloseable`/`try-with-resources` or `Stream.close`); the explicit stack/queue for trees uses O(width) or O(depth) memory; a recursive DFS is simpler to write but can't pause between elements, which is why an explicit stack is needed for a *pull-based* iterator.
**Mechanism:** *lazy, on-demand advancement driven by an explicit state machine.* The paginated iterator keeps a **current-page iterator + a next-page token**; `hasNext()` transparently fetches the next page only when the current one is drained (and skips empty pages), turning N pages into one sequence with I/O deferred to the moment of need. The tree iterators replace recursion with an **explicit `Deque` (stack → DFS) or `Queue` (queue → BFS)** so traversal can be *suspended and resumed* one element per `next()` call — the essence of converting a recursive walk into a pull-based cursor.

---

## 🎨 Real-World Example

A believable production scenario: a **cloud storage / object-store client** (think S3, GCS, or a Drive-style API) where you must **list all objects under a prefix**. The API is paginated (tokens), the result set can be millions of objects (you cannot hold them all in memory), calls can fail transiently (retry), and you want callers to just write `for (StorageObject o : bucket.list("photos/"))`. This is a FAANG-favorite because it exercises everything: lazy on-demand fetching, an `Iterable` façade over pagination, transient-error retry inside `hasNext()`, resource-safe closing, and a clean bridge to `Stream` for filtering/mapping. Callers never see a page, a token, or an HTTP call.

<details>
<summary>💻 Click to expand full real-world example (paginated object-store iterator with retry, streaming, demo)</summary>

```java
import java.util.*;
import java.util.stream.*;
import java.util.spi.*;

// ---------- Domain ----------
record StorageObject(String key, long sizeBytes) {}
record ObjectPage(List<StorageObject> objects, String nextToken) {}

// ---------- The transport (would be HTTP in reality) ----------
interface StorageTransport {
    /** Fetch one page; token == null means the first page. May throw on transient failure. */
    ObjectPage listPage(String prefix, String pageToken);
}

// ---------- The Aggregate: a bucket handle that exposes a lazy, retrying Iterable ----------
class Bucket {
    private final StorageTransport transport;
    private final String name;
    Bucket(String name, StorageTransport transport) { this.name = name; this.transport = transport; }

    /** The whole result set as ONE lazy sequence — pagination is invisible to callers. */
    Iterable<StorageObject> list(String prefix) {
        return () -> new ObjectIterator(prefix);   // fresh, independent cursor each call
    }

    /** A Stream view for functional pipelines (map/filter/limit) — still lazy & paged. */
    Stream<StorageObject> stream(String prefix) {
        Iterable<StorageObject> it = list(prefix);
        return StreamSupport.stream(it.spliterator(), /*parallel=*/ false);
    }

    // ---------- ConcreteIterator: lazy, page-fetching, retrying ----------
    private class ObjectIterator implements Iterator<StorageObject> {
        private final String prefix;
        private Iterator<StorageObject> pageCursor = Collections.emptyIterator();
        private String nextToken = null;
        private boolean exhausted = false;      // no more pages to fetch
        private boolean started = false;        // have we fetched the first page yet?

        ObjectIterator(String prefix) { this.prefix = prefix; }

        @Override public boolean hasNext() {
            if (pageCursor.hasNext()) return true;      // current page still has items
            // Drain-and-advance: fetch subsequent pages until we find items or run out.
            while (!exhausted) {
                fetchNextPageWithRetry();
                if (pageCursor.hasNext()) return true;
            }
            return false;
        }

        @Override public StorageObject next() {
            if (!hasNext()) throw new NoSuchElementException();
            return pageCursor.next();
        }

        // Fetch the next page, retrying transient failures with bounded exponential backoff.
        private void fetchNextPageWithRetry() {
            if (exhausted) return;
            RuntimeException last = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    ObjectPage page = transport.listPage(prefix, started ? nextToken : null);
                    started = true;
                    pageCursor = page.objects().iterator();
                    nextToken = page.nextToken();
                    if (nextToken == null) exhausted = true;   // last page reached
                    return;
                } catch (RuntimeException transientEx) {
                    last = transientEx;
                    sleep(50L * (1L << (attempt - 1)));        // 50, 100, 200 ms backoff
                }
            }
            throw new IllegalStateException("listPage failed after retries", last);
        }

        private void sleep(long ms) {
            try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
}

// ---------- Demo / usage: multiple call sites ----------
public class ObjectStoreDemo {
    public static void main(String[] args) {
        // A fake transport: 3 pages, with ONE transient failure injected on page 2.
        StorageTransport transport = new StorageTransport() {
            int page2Attempts = 0;
            public ObjectPage listPage(String prefix, String token) {
                if (token == null)
                    return new ObjectPage(List.of(new StorageObject("photos/a.jpg", 100),
                                                  new StorageObject("photos/b.jpg", 200)), "t2");
                if (token.equals("t2")) {
                    if (page2Attempts++ == 0) throw new RuntimeException("503 transient");  // retried
                    return new ObjectPage(List.of(new StorageObject("photos/c.jpg", 300)), "t3");
                }
                if (token.equals("t3"))
                    return new ObjectPage(List.of(new StorageObject("photos/d.jpg", 400)), null);
                throw new IllegalArgumentException("bad token " + token);
            }
        };

        Bucket bucket = new Bucket("my-bucket", transport);

        // Call site 1: plain for-each over the entire (paged) result set.
        System.out.println("--- all objects ---");
        for (StorageObject o : bucket.list("photos/")) {
            System.out.println(o.key() + " (" + o.sizeBytes() + " bytes)");
        }   // a.jpg, b.jpg, c.jpg (after retry), d.jpg

        // Call site 2: independent second traversal — its own cursor, own paging.
        long total = 0;
        for (StorageObject o : bucket.list("photos/")) total += o.sizeBytes();
        System.out.println("Total bytes: " + total);   // 1000

        // Call site 3: Stream pipeline — filter + map + limit, still lazy and paged.
        System.out.println("--- big objects (>150 bytes), first 2 keys ---");
        bucket.stream("photos/")
              .filter(o -> o.sizeBytes() > 150)
              .map(StorageObject::key)
              .limit(2)
              .forEach(System.out::println);   // b.jpg, c.jpg  (only fetches pages until 2 found)

        // Call site 4: early termination — stop after the first object; only page 1 is fetched.
        Iterator<StorageObject> it = bucket.list("photos/").iterator();
        if (it.hasNext()) System.out.println("First only: " + it.next().key()); // a.jpg
    }
}
```
</details>

**Why this is a strong FAANG answer:** it shows (1) an `Iterable` **façade over pagination** so callers write ordinary for-each loops and never touch tokens or HTTP; (2) **true laziness** — pages are fetched on demand inside `hasNext()`, so early termination (`limit`, `break`) fetches only what's consumed and memory stays flat over a million-object bucket; (3) **transient-fault retry with backoff** embedded in the fetch, the real-world concern that separates production code from toy iterators; (4) **independent cursors** — each `list()` call yields a fresh iterator with its own paging state, so two traversals don't interfere; and (5) a clean **bridge to `Stream`** via `spliterator()`, letting callers compose `filter/map/limit` while the underlying paging remains lazy — exactly the shape of Google Cloud's `Page`/`PageIterable` and the AWS SDK v2 paginators.

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- You want to **traverse a collection without exposing its internal representation** (array vs. linked nodes vs. tree vs. buckets) — clients depend only on `hasNext()`/`next()`.
- You need a **uniform traversal interface** so the same algorithm works across many different data structures (and integrates with the enhanced for-loop, `forEach`, and Streams via `Iterable`).
- You need to support **multiple simultaneous, independent traversals** of the same collection (two cursors at two positions).
- You want to provide **different traversal orders** (forward/reverse, DFS/BFS, in-order/pre-order) as separate iterators without bloating the collection.
- The data is **large, streamed, or infinite** and must be produced **lazily** (file lines, DB rows, paginated APIs, generated sequences) with constant memory.
- You want to **decouple traversal from the collection** so the storage strategy can change without breaking clients (Single Responsibility).

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- **You just need a plain in-memory list/array walk** and the JDK already gives you `Iterable`/for-each — writing a custom `Iterator` adds nothing. Use the built-in one.
- **You need random access / indexed access by position** (`list.get(i)`), or **bidirectional / random jumps** — an iterator is inherently sequential; use `List`/`ListIterator`/index access instead.
- **The collection is tiny and fixed** and the traversal never varies — a simple loop is clearer than an iterator abstraction.
- **You need rich queries** (filter, map, group, join). Prefer **Streams** (internal iteration) which compose these declaratively rather than hand-rolling stateful external iterators.
- **Heavy concurrent mutation during traversal** is required — a plain fail-fast iterator throws; you need a **concurrent/fail-safe** collection (`CopyOnWriteArrayList`, `ConcurrentHashMap`) or a snapshot, not a custom iterator bolted on.

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- **Encapsulation** — the collection's internal representation stays hidden; clients depend only on the iterator interface (Single Responsibility Principle).
- **Uniform interface** — the same traversal code works over any aggregate; integrates with for-each, `forEach`, and Streams.
- **Multiple independent traversals** — state lives on the cursor, so many walks can run at once at different positions.
- **Multiple traversal orders** — different iterators (DFS/BFS/reverse) without changing the collection (Open/Closed).
- **Laziness** — supports infinite/streamed/huge data with constant memory; elements produced only when consumed.
- Decouples **algorithms from data structures** (the whole point of `java.util.Iterator`).

**Cons**

- **Overhead / ceremony** for trivial cases — a simple index loop can be clearer for a small fixed array.
- **Sequential only** — no efficient random access, and single-pass iterators can't be rewound (must create a fresh one).
- **Fragile under mutation** — structural changes during iteration cause `ConcurrentModificationException` (fail-fast) or, worse, silent corruption if not guarded.
- **Statefulness bugs** — reusing an exhausted iterator, forgetting to bound an infinite one, or calling `next()` without `hasNext()` are common errors.
- **Lazy I/O iterators** complicate error handling and resource cleanup (exceptions from `next()`, connections to close).

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

Iterator is frequently discussed alongside Composite (you often iterate a Composite), Visitor (both traverse structures), and Java Streams / the old Enumeration (which supersede/relate to it).

| Pattern / API | Intent | Who controls the loop? | Key tell |
|---|---|---|---|
| **Iterator** | Access elements of an aggregate sequentially without exposing its representation | **Client** (external iteration) — pulls each element | `hasNext()`/`next()`; a *cursor* over one structure |
| **Composite** | Treat individual objects and compositions uniformly (tree of parts) | N/A (a structure) — but you often **use an Iterator to walk it** | A recursive whole-part hierarchy; Iterator is how you traverse it |
| **Visitor** | Add operations to an object structure without changing its classes (double dispatch) | Traversal + operation are separate; Visitor defines *what to do* per element type | You want *many operations* over a stable structure of *varied types* |
| **Streams (internal iteration)** | Declaratively transform/aggregate sequences via a pipeline | **The library** (internal iteration) — you supply functions | `filter/map/reduce`; laziness + parallelism built in |
| **Enumeration (legacy)** | Original JDK 1.0 traversal interface (`hasMoreElements`/`nextElement`) | Client | Older, no `remove()`, not fail-fast; superseded by `Iterator` |

**Iterator vs. Visitor:** Iterator gives you elements **one type at a time in sequence**; you decide what to do. Visitor is for structures of **heterogeneous element types** where you want to define **operations externally** with double dispatch (e.g., an AST with many node types and many passes). You can combine them — iterate a structure and apply a visitor.

**Iterator vs. Streams:** an `Iterator` is **external** iteration (client pulls, can stop early, is stateful and single-use); a `Stream` is **internal** iteration (you describe a pipeline; the library drives the loop, adds laziness, short-circuiting, and easy parallelism via `Spliterator`). Streams are built *on top of* the same "advance one element" idea — `Spliterator` is the Iterator generalized for splitting/parallelism.

The mnemonic: **Iterator = "give me the next one, I'll decide what to do" (external, pull); Streams = "here's what to do with each; you run the loop" (internal, push-ish).**

</details>

## 📊 Comparison Table of Variants

<details>
<summary>📖 Click to expand</summary>

| Axis | V0: Expose internals | V1: Custom Iterator | V2: JDK Iterable | V3: Fail-fast (modCount) | V4: Lazy/Infinite | V5: Paginated / Tree |
|---|---|---|---|---|---|---|
| **Hides representation** | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Works with for-each** | ❌ | ❌ (custom) | ✅ | ✅ | ✅ | ✅ |
| **Laziness** | ❌ | ❌ (eager) | ❌ (eager) | ❌ (eager) | ✅ (on demand) | ✅ (on demand) |
| **Handles infinite/huge** | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| **Memory** | O(n) exposed | O(n) | O(n) | O(n) | O(1) | O(1) per page / O(width\|depth) |
| **Fail-fast detection** | ❌ | ❌ | ❌ | ✅ | N/A | N/A (external source) |
| **Thread-safety** | ❌ | ❌ | ❌ | ⚠️ best-effort detect | ❌ (single-use) | ❌ (add retry, not safety) |
| **Supports remove()** | N/A | ❌ | default throws | ✅ (via iterator) | usually ❌ | usually ❌ |
| **Multiple independent walks** | ❌ | ✅ | ✅ | ✅ | ✅ (fresh each) | ✅ (fresh each) |
| **Best for** | (never) | Learning/custom store | Idiomatic in-memory collections | JDK-style safe collections | Streams/generators/files | APIs, trees, huge datasets |

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

### Pitfall 1: Modifying a collection during iteration (ConcurrentModificationException)

**What goes wrong:** You add/remove from the collection inside a for-each loop. The loop uses an iterator whose `expectedModCount` no longer matches `modCount`, so the next `next()` throws `ConcurrentModificationException` (or, in an unguarded custom iterator, silently skips/corrupts).

<details>
<summary>💻 Click to expand — the failure</summary>

```java
List<String> list = new ArrayList<>(List.of("a", "b", "c"));
for (String s : list) {
    if (s.equals("b")) list.remove(s);   // BUG: structural change during for-each -> CME
}
```
</details>

<details>
<summary>💻 Click to expand — the fixes</summary>

```java
// Fix 1: remove via the iterator itself (it re-syncs expectedModCount).
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (it.next().equals("b")) it.remove();     // safe
}

// Fix 2: Collection.removeIf (does it correctly under the hood).
list.removeIf(s -> s.equals("b"));

// Fix 3: iterate a copy if you must mutate the original.
for (String s : new ArrayList<>(list)) {
    if (s.equals("b")) list.remove(s);
}
```
</details>

### Pitfall 2: Calling next() without checking hasNext() (NoSuchElementException)

**What goes wrong:** Assuming a fixed number of elements and calling `next()` past the end throws `NoSuchElementException`. Also happens when two `next()` calls follow a single `hasNext()`.

<details>
<summary>💻 Click to expand — the failure and fix</summary>

```java
Iterator<String> it = list.iterator();
String a = it.next();
String b = it.next();   // BUG if list has only 1 element -> NoSuchElementException

// FIX: always guard each advance.
while (it.hasNext()) {
    String x = it.next();   // exactly one next() per confirmed hasNext()
}
```
</details>

### Pitfall 3: Reusing an exhausted (single-pass) iterator or Stream

**What goes wrong:** Iterators are single-use cursors. Once `hasNext()` returns false, the iterator is spent; a Stream throws `IllegalStateException` if you try to operate on it twice. People cache an iterator/stream and try to loop again, getting nothing (or an exception).

<details>
<summary>💻 Click to expand — the failure and fix</summary>

```java
Stream<String> s = list.stream();
s.forEach(System.out::println);
s.count();   // BUG: IllegalStateException — stream already operated upon

// FIX: get a FRESH iterator/stream for each traversal. That's why Iterable.iterator()
// is a FACTORY — call it again for a new independent cursor.
list.stream().forEach(System.out::println);
long n = list.stream().count();       // fresh stream
```
</details>

### Pitfall 4: hasNext() with side effects, or an infinite iterator with no bound

**What goes wrong:** (a) An infinite iterator (`hasNext()` always true) with no `limit`/`break` loops forever. (b) A lazy I/O iterator that reads inside `hasNext()` must be idempotent — calling `hasNext()` twice must not consume two elements. Buggy peeking either double-reads or loses an element.

<details>
<summary>💻 Click to expand — safe peeking pattern (buffer one element)</summary>

```java
// Correct lazy I/O iterator: buffer the peeked element so hasNext() is idempotent.
class LineIterator implements Iterator<String> {
    private final BufferedReader reader;
    private String buffered;           // holds the next line once peeked
    private boolean done = false;
    LineIterator(BufferedReader r) { this.reader = r; }

    @Override public boolean hasNext() {
        if (buffered != null) return true;   // already peeked -> idempotent
        if (done) return false;
        try {
            buffered = reader.readLine();     // read ONCE, stash it
            if (buffered == null) { done = true; return false; }
            return true;
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    @Override public String next() {
        if (!hasNext()) throw new NoSuchElementException();
        String line = buffered; buffered = null;   // hand out the buffered value, clear it
        return line;
    }
}
```
</details>

### Pitfall 5: Putting iteration state on the collection instead of the cursor

**What goes wrong:** A tempting shortcut is to store `currentIndex` on the collection and expose `hasNext()`/`next()` on the collection itself. Now the collection can only be traversed **once at a time**, and two concurrent loops corrupt each other's position — you've lost the pattern's core benefit.

<details>
<summary>💻 Click to expand — the fix</summary>

```java
// BAD: position on the collection -> only one traversal possible, not reentrant.
class Bad { private int pos; boolean hasNext(){...} Object next(){ return data[pos++]; } }

// GOOD: iterator() returns a FRESH cursor holding its OWN position.
class Good implements Iterable<T> {
    public Iterator<T> iterator() {
        return new Iterator<>() { private int pos = 0; /* independent per call */ };
    }
}
```
</details>

</details>

## 🎓 Interview Tips

**What interviewers commonly ask:**

- "Implement a custom iterator for a data structure (a tree, a 2D matrix, a zigzag, a range)." (The most common hands-on Iterator question.)
- "What is `ConcurrentModificationException` and how does fail-fast work?" (Explain `modCount`/`expectedModCount`.)
- "Fail-fast vs. fail-safe iterators — difference?" (`ArrayList` throws; `CopyOnWriteArrayList`/`ConcurrentHashMap` iterate a snapshot/weakly-consistent view.)
- "External vs. internal iteration — Iterator vs. Streams?" (Who drives the loop; laziness; parallelism.)
- "How would you iterate a paginated API / an infinite sequence lazily?" (Fetch/compute on demand in `next()`.)
- "What's a `Spliterator` and why does it exist?" (Iterator generalized for splitting → parallel streams.)

**What you should proactively mention even if not asked:**

- The GoF intent in one line: **access elements sequentially without exposing the representation** — and that the state lives on the **cursor**, enabling independent simultaneous traversals.
- `java.lang.Iterable` + `java.util.Iterator` is the idiomatic form; **for-each is pure sugar** over `iterator()`/`hasNext()`/`next()`.
- **Fail-fast is best-effort, not a correctness guarantee** — for concurrency use concurrent collections (fail-safe/weakly-consistent), not fail-fast + prayers.
- **Remove during iteration only via `Iterator.remove()`** (or `removeIf`); anything else risks CME.
- Distinguish **external (Iterator, pull, can stop early, single-use) vs. internal (Streams/forEach, push, composable, parallelizable)** iteration.
- For huge/infinite/streamed data, prefer a **lazy** iterator (compute/fetch in `next()`), and remember to **bound** infinite ones and **close** I/O-backed ones.
- Mention **`Spliterator`** as the modern Iterator for parallelism (`tryAdvance` + `trySplit` + characteristics like `SIZED`, `ORDERED`, `IMMUTABLE`).

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Composite** — Iterator is the standard way to traverse a Composite (tree of whole/part objects); the two are frequently used together.
- **Factory Method** — `iterator()`/`createIterator()` *is* a Factory Method: the aggregate decides which concrete iterator to instantiate.
- **Visitor** — an alternative for applying operations across a structure of varied element types (double dispatch); often combined with iteration.
- **Memento** — an iterator's position/state can be captured as a memento to save/restore a traversal point.
- **Streams (internal iteration)** — the functional-pipeline evolution of iteration; built on `Spliterator`, itself a generalized Iterator.
- **Observer** — a push model (data pushed to subscribers) contrasted with Iterator's pull model; reactive streams blend both with backpressure.
- **Null Object** — an empty iterator (`Collections.emptyIterator()`) is a Null Object avoiding null checks.

</details>

## 📚 Library/Framework Implementation

**1. `java.util.Iterator` / `java.lang.Iterable` (the JDK core).** Every JDK collection implements `Iterable`, returning a `ConcreteIterator` (often an inner class named `Itr`) that uses `modCount` for fail-fast detection. The enhanced for-loop, `forEach`, and `Iterable.spliterator()` all build on this. `ListIterator` extends it with bidirectional traversal and `set`/`add`.

<details>
<summary>💻 Click to expand — JDK Iterator/Iterable in action</summary>

```java
List<Integer> list = new ArrayList<>(List.of(1, 2, 3));

Iterator<Integer> it = list.iterator();       // ArrayList.Itr — uses modCount fail-fast
while (it.hasNext()) {
    int x = it.next();
    if (x == 2) it.remove();                    // safe removal via the iterator
}
System.out.println(list);                       // [1, 3]

// ListIterator: bidirectional + set/add.
ListIterator<Integer> lit = list.listIterator();
while (lit.hasNext()) { int v = lit.next(); lit.set(v * 10); }
System.out.println(list);                       // [10, 30]

// A custom Iterable plugs into for-each for free:
Iterable<Integer> once = () -> List.of(7, 8, 9).iterator();
for (int v : once) System.out.print(v + " ");   // 7 8 9
```
</details>

**2. Java Streams & `Spliterator` (internal iteration + parallelism).** `Spliterator` (Java 8) is the Iterator generalized for **splitting**: `tryAdvance` (advance one), `trySplit` (partition for parallel processing), plus characteristics (`SIZED`, `ORDERED`, `IMMUTABLE`, `SORTED`). It underpins `Stream`, enabling lazy pipelines and `parallelStream()`. `StreamSupport.stream(spliterator, parallel)` bridges any `Iterable`/`Spliterator` into a Stream.

<details>
<summary>💻 Click to expand — Spliterator and Streams</summary>

```java
import java.util.*;
import java.util.stream.*;

List<Integer> nums = IntStream.rangeClosed(1, 8).boxed().collect(Collectors.toList());

// A Spliterator can split its work for parallelism.
Spliterator<Integer> s1 = nums.spliterator();
Spliterator<Integer> s2 = s1.trySplit();        // s2 gets ~half, s1 keeps the rest
System.out.println(s1.estimateSize() + " + " + s2.estimateSize()); // ~4 + ~4

// Internal iteration: the library drives the loop; you supply the pipeline.
int sumOfSquaresOfEvens = nums.parallelStream()  // parallel thanks to Spliterator splitting
    .filter(n -> n % 2 == 0)
    .mapToInt(n -> n * n)
    .sum();                                       // 4+16+36+64 = 120
System.out.println(sumOfSquaresOfEvens);

// Bridge a plain Iterable to a Stream:
Iterable<String> it = List.of("a", "b", "c");
Stream<String> stream = StreamSupport.stream(it.spliterator(), false);
```
</details>

**3. Guava `Iterators`/`AbstractIterator`, `Scanner`, and JDBC `ResultSet`.** Guava's `AbstractIterator` lets you implement a lazy iterator by defining just `computeNext()` (return the next element or call `endOfData()`), removing `hasNext`/`next` boilerplate. `java.util.Scanner` is an iterator over tokens (`hasNextInt()`/`nextInt()`). JDBC `ResultSet` is a database **cursor** — `next()` advances one row lazily over a potentially huge result set — a textbook streaming iterator.

<details>
<summary>💻 Click to expand — Guava AbstractIterator, Scanner, ResultSet</summary>

```java
// Guava: implement only computeNext() — a clean lazy generator iterator.
import com.google.common.collect.AbstractIterator;
class EvenNumbers extends AbstractIterator<Integer> {
    private int n = 0;
    private final int max;
    EvenNumbers(int max) { this.max = max; }
    @Override protected Integer computeNext() {
        n += 2;
        return n <= max ? n : endOfData();   // endOfData() makes hasNext() return false
    }
}

// Scanner: an iterator over tokens.
Scanner sc = new Scanner("10 20 30");
while (sc.hasNextInt()) System.out.println(sc.nextInt());   // 10, 20, 30

// JDBC ResultSet: a lazy DB cursor — one row per next(), never loads all rows.
// try (ResultSet rs = stmt.executeQuery("SELECT id, name FROM users")) {
//     while (rs.next()) {                       // advance the cursor one row at a time
//         System.out.println(rs.getLong("id") + " " + rs.getString("name"));
//     }
// }
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1: [Conceptual] What is the Iterator pattern and what problem does it solve?</strong></summary>

The Iterator pattern is a **behavioral** GoF pattern that provides a way to **access the elements of an aggregate object sequentially without exposing its underlying representation**. It solves the problem of traversal code being **coupled to how a collection stores its elements** — index into an array, walk linked nodes, hash buckets, tree links — which would break every consumer whenever the storage strategy changes.

It works by extracting traversal into a separate **cursor** object (the iterator) that exposes a tiny uniform interface: `hasNext()` and `next()`. The collection (Aggregate/Iterable) is a **factory** that hands out these cursors.

Because the traversal state lives on the cursor, you get **independent simultaneous traversals**, and because the interface is uniform, the *same* algorithm walks any data structure. This decouples algorithms from data structures — exactly what `java.util.Iterator` gives Java.
</details>

<details>
<summary><strong>Q2: [Conceptual] Name the participants of the Iterator pattern.</strong></summary>

Four participants:

1. **Iterator** — the interface declaring `hasNext()`, `next()`, and optionally `remove()`. The uniform traversal contract.
2. **ConcreteIterator** — implements `Iterator` for one specific aggregate; holds the **traversal state** (cursor index, node pointer, stack/queue) and knows how to advance through that structure.
3. **Aggregate / Iterable** — an interface declaring the factory method (`iterator()` in Java) that creates a fresh iterator.
4. **ConcreteAggregate** — the actual collection (`ArrayList`, a tree, a custom repository); its `iterator()` returns a `ConcreteIterator` bound to it.

The critical relationship: the client depends only on `Iterator`/`Iterable`, never on the concrete collection or iterator — that decoupling is what lets storage change freely and traversal code stay uniform.
</details>

<details>
<summary><strong>Q3: [Conceptual] What is the difference between external and internal iteration?</strong></summary>

**External iteration** — the *client* controls the loop and explicitly pulls each element: `while (it.hasNext()) use(it.next());` or the enhanced for-loop. The client decides when to advance and can **stop early**. Java's `Iterator` is external. It's flexible but stateful and single-use.

**Internal iteration** — the *collection/library* controls the loop; you hand it a function and it applies that to each element: `list.forEach(x -> ...)`, `stream.map(...).filter(...)`. The traversal mechanics are hidden; you only say *what* to do per element. This enables **laziness, short-circuiting, and easy parallelism** (`parallelStream`).

Trade-off: external gives you fine control and early exit with simple debugging; internal gives you composability, optimization freedom (the library can reorder/parallelize), and less boilerplate. Streams are internal; the classic Iterator is external. Both build on "advance one element at a time."
</details>

<details>
<summary><strong>Q4: [Conceptual] How does the enhanced for-loop relate to Iterator, and what does it desugar to?</strong></summary>

The enhanced for-loop (`for (T t : iterable)`) is **pure syntactic sugar** over the Iterator pattern. Any type implementing `java.lang.Iterable` (or an array) can be used. The compiler rewrites it into explicit iterator calls.

<details>
<summary>💻 Click to expand</summary>

```java
for (String s : collection) { use(s); }
// desugars to:
for (Iterator<String> it = collection.iterator(); it.hasNext(); ) {
    String s = it.next();
    use(s);
}
```
</details>

This is why implementing `Iterable` makes your custom collection a first-class citizen — it instantly works with for-each, `forEach`, and any API taking an `Iterable`. It also explains why you **can't call `remove()`** inside a for-each (you don't have a handle on the hidden iterator) and why modifying the collection during a for-each triggers `ConcurrentModificationException`.
</details>

<details>
<summary><strong>Q5: [Implementation] Implement a custom Iterable collection that works with the for-each loop.</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
import java.util.Iterator;
import java.util.NoSuchElementException;

class IntBag implements Iterable<Integer> {
    private int[] data = new int[8];
    private int size = 0;
    void add(int x) {
        if (size == data.length) data = java.util.Arrays.copyOf(data, size * 2);
        data[size++] = x;
    }
    @Override public Iterator<Integer> iterator() {
        return new Iterator<>() {
            private int cursor = 0;
            @Override public boolean hasNext() { return cursor < size; }
            @Override public Integer next() {
                if (!hasNext()) throw new NoSuchElementException();
                return data[cursor++];
            }
        };
    }
    public static void main(String[] args) {
        IntBag bag = new IntBag();
        bag.add(10); bag.add(20); bag.add(30);
        for (int x : bag) System.out.println(x);   // 10, 20, 30
    }
}
```
</details>

The two essentials: implement `Iterable.iterator()` returning a fresh cursor, and have the cursor hold its own `cursor` position (so each for-each gets an independent traversal). Guard `next()` with a `NoSuchElementException` when exhausted.
</details>

<details>
<summary><strong>Q6: [Implementation] Implement an iterator over a binary tree that yields elements in in-order (sorted) without recursion.</strong></summary>

A classic — convert the recursive in-order walk into a pull-based iterator using an explicit stack.

<details>
<summary>💻 Click to expand solution</summary>

```java
import java.util.*;

class BSTIterator implements Iterator<Integer> {
    static class Node { int val; Node left, right; Node(int v){ val = v; } }
    private final Deque<Node> stack = new ArrayDeque<>();

    BSTIterator(Node root) { pushLeft(root); }

    private void pushLeft(Node n) {          // push the leftmost spine
        while (n != null) { stack.push(n); n = n.left; }
    }
    @Override public boolean hasNext() { return !stack.isEmpty(); }
    @Override public Integer next() {
        if (!hasNext()) throw new NoSuchElementException();
        Node node = stack.pop();             // smallest unvisited
        if (node.right != null) pushLeft(node.right);  // then its right subtree's left spine
        return node.val;
    }
}
```
</details>

The invariant: the stack always holds the path of ancestors whose left subtrees are fully queued. `next()` pops the smallest, then pushes the left spine of its right child. This yields sorted order for a BST with **O(h) space** and amortized **O(1)** per `next()` — and, crucially, it's *pausable* between elements, which recursion is not.
</details>

<details>
<summary><strong>Q7: [Implementation] Implement a filtering iterator that wraps another iterator and yields only elements matching a predicate.</strong></summary>

A decorator over an iterator — shows lazy composition.

<details>
<summary>💻 Click to expand solution</summary>

```java
import java.util.*;
import java.util.function.Predicate;

class FilterIterator<T> implements Iterator<T> {
    private final Iterator<T> source;
    private final Predicate<T> predicate;
    private T nextMatch;
    private boolean hasMatch = false;

    FilterIterator(Iterator<T> source, Predicate<T> predicate) {
        this.source = source; this.predicate = predicate;
    }
    @Override public boolean hasNext() {
        if (hasMatch) return true;
        while (source.hasNext()) {           // pull from source until a match (or exhaustion)
            T candidate = source.next();
            if (predicate.test(candidate)) { nextMatch = candidate; hasMatch = true; return true; }
        }
        return false;
    }
    @Override public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        hasMatch = false;                    // consume the buffered match
        T result = nextMatch; nextMatch = null;
        return result;
    }
    public static void main(String[] args) {
        Iterator<Integer> nums = List.of(1,2,3,4,5,6).iterator();
        Iterator<Integer> evens = new FilterIterator<>(nums, n -> n % 2 == 0);
        while (evens.hasNext()) System.out.print(evens.next() + " ");  // 2 4 6
    }
}
```
</details>

Key technique: **buffer one matching element** in `hasNext()` so it's idempotent, and clear it in `next()`. This is exactly how `Stream.filter` works lazily — it pulls from upstream only as needed. It composes: you can wrap a `FilterIterator` in a `MapIterator`, forming a pipeline.
</details>

<details>
<summary><strong>Q8: [Implementation] Implement a 2D matrix iterator (row-major) that also supports remove is not needed — just traversal in one pass.</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
import java.util.*;

class MatrixIterator implements Iterator<Integer> {
    private final int[][] matrix;
    private int row = 0, col = 0;

    MatrixIterator(int[][] matrix) { this.matrix = matrix; advanceToNonEmptyRow(); }

    private void advanceToNonEmptyRow() {        // skip empty rows (jagged arrays)
        while (row < matrix.length && (matrix[row] == null || col >= matrix[row].length)) {
            row++; col = 0;
        }
    }
    @Override public boolean hasNext() { return row < matrix.length; }
    @Override public Integer next() {
        if (!hasNext()) throw new NoSuchElementException();
        int val = matrix[row][col++];
        advanceToNonEmptyRow();                  // move to next valid cell (handles row wrap + empties)
        return val;
    }
    public static void main(String[] args) {
        int[][] m = {{1, 2, 3}, {}, {4}, {5, 6}};   // note the empty row
        MatrixIterator it = new MatrixIterator(m);
        while (it.hasNext()) System.out.print(it.next() + " ");  // 1 2 3 4 5 6
    }
}
```
</details>

The subtlety interviewers probe: **jagged/empty rows**. After each element (and at construction) you must advance past empty rows so `hasNext()` is correct. Keeping `(row, col)` as the cursor state and normalizing it after every move is the clean way to handle wrap-around and gaps.
</details>

<details>
<summary><strong>Q9: [Implementation] Implement a "flatten" iterator that iterates over a list of lists as one flat sequence.</strong></summary>

<details>
<summary>💻 Click to expand solution</summary>

```java
import java.util.*;

class FlattenIterator<T> implements Iterator<T> {
    private final Iterator<? extends Iterable<T>> outer;
    private Iterator<T> inner = Collections.emptyIterator();

    FlattenIterator(Iterable<? extends Iterable<T>> lists) { this.outer = lists.iterator(); }

    @Override public boolean hasNext() {
        while (!inner.hasNext() && outer.hasNext()) {   // advance to the next non-empty inner list
            inner = outer.next().iterator();
        }
        return inner.hasNext();
    }
    @Override public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        return inner.next();
    }
    public static void main(String[] args) {
        List<List<Integer>> nested = List.of(List.of(1, 2), List.of(), List.of(3), List.of(4, 5));
        FlattenIterator<Integer> it = new FlattenIterator<>(nested);
        while (it.hasNext()) System.out.print(it.next() + " ");   // 1 2 3 4 5
    }
}
```
</details>

The core idea: hold an **outer** iterator (over the lists) and an **inner** iterator (over the current list). `hasNext()` loops to skip **empty inner lists** by pulling the next one from the outer iterator. This is the essence of `Stream.flatMap` and the LeetCode "Flatten Nested List Iterator" — the same drain-and-advance pattern used by the paginated iterator.
</details>

<details>
<summary><strong>Q10: [Breaking] Explain ConcurrentModificationException in detail. When exactly is it thrown, and when might it NOT be thrown?</strong></summary>

`ConcurrentModificationException` (CME) is thrown by **fail-fast** iterators when they detect the backing collection was **structurally modified** (add/remove changing size) during iteration, other than through the iterator's own `remove()`. Mechanism: the collection tracks `modCount`; the iterator snapshots it into `expectedModCount` and compares on each `next()` (and `remove()`); a mismatch throws.

When it's thrown: modifying a list inside a for-each (`list.remove(x)` in the loop), or one thread mutating while another iterates.

When it might **not** be thrown: fail-fast is **best-effort**. The check isn't synchronized, so a concurrent modification can slip through by luck of timing. A notorious case is removing the **second-to-last** element — `hasNext()` compares `cursor != size` and can return false *before* the `modCount` check in `next()` runs, so the loop ends without ever detecting the modification. The lesson: CME is a debugging aid, **not** a thread-safety guarantee — never rely on it for correctness in concurrent code.
</details>

<details>
<summary><strong>Q11: [Breaking] Someone's custom iterator sometimes skips elements or returns duplicates. What are the likely causes?</strong></summary>

Several classic bugs: (1) **`hasNext()` has side effects / isn't idempotent** — e.g., it reads a line/row to check for existence but doesn't buffer it, so a following `next()` reads a *different* element (skip) or two `hasNext()` calls consume two elements. (2) **State on the collection, not the cursor** — two traversals share a position and clobber each other. (3) **Off-by-one in advancement** — incrementing the cursor before vs. after reading, or `<=` vs. `<` in the bound. (4) **Mutating the source during iteration** shifts indices (delete at `i` then read `i` skips the shifted element). (5) **Reusing an exhausted iterator** without resetting.

<details>
<summary>💻 Click to expand — the non-idempotent hasNext bug and fix</summary>

```java
// BUG: hasNext() consumes an element by reading it and not buffering.
public boolean hasNext() { return reader.readLine() != null; } // reads & DISCARDS a line!

// FIX: buffer the peeked value so hasNext() is idempotent (see LineIterator in Pitfall 4).
```
</details>

Systematic fix: keep **all state on the cursor**, make `hasNext()` **idempotent** (buffer any peeked element), and be consistent about read-then-advance ordering.
</details>

<details>
<summary><strong>Q12: [Breaking] Why can't you safely remove elements in a for-each loop, and what are the correct alternatives?</strong></summary>

The for-each loop hides the iterator, so you have **no handle to call `iterator.remove()`**. If you instead call `collection.remove(x)` directly, you structurally modify the collection behind the iterator's back, bumping `modCount`, so the next `next()` throws `ConcurrentModificationException`.

Correct alternatives:

<details>
<summary>💻 Click to expand</summary>

```java
// 1) Explicit iterator + Iterator.remove() (re-syncs expectedModCount):
Iterator<T> it = list.iterator();
while (it.hasNext()) if (shouldRemove(it.next())) it.remove();

// 2) Collection.removeIf(predicate) — cleanest, does it correctly internally:
list.removeIf(this::shouldRemove);

// 3) Collect-then-remove, or iterate a copy:
list.removeAll(list.stream().filter(this::shouldRemove).collect(Collectors.toList()));
```
</details>

`removeIf` is usually best (concise, correct, O(n)). Building a new filtered list via Streams and reassigning is a good immutable alternative. The one thing you must *not* do is mutate the original inside a for-each over it.
</details>

<details>
<summary><strong>Q13: [Breaking] What's the danger of an iterator that does I/O, and how do you handle resources and errors?</strong></summary>

An I/O-backed iterator (file lines, DB rows, HTTP pages) mixes **traversal with side effects**, which creates three hazards: (1) **resource leaks** — the underlying reader/connection must be closed even if iteration stops early or throws; (2) **exceptions surface from `hasNext()`/`next()`** — but `Iterator` methods can't throw checked exceptions, so `IOException` must be wrapped (`UncheckedIOException`); (3) **non-idempotent peeking** corrupts the stream (Pitfall 4).

Handling: implement `AutoCloseable` and use try-with-resources (or use `Stream` which is `AutoCloseable` — `Files.lines()` must be closed). Buffer one element for idempotent `hasNext()`. Wrap checked exceptions. For network iterators, add **retry with backoff** and **timeouts** inside the fetch, and consider a `close()` that releases the connection.

<details>
<summary>💻 Click to expand — closeable streaming iterator</summary>

```java
try (Stream<String> lines = java.nio.file.Files.lines(path)) {  // closed automatically
    lines.filter(l -> !l.isBlank()).forEach(System.out::println);
}   // the file handle is released here even on exception/early exit
```
</details>
</details>

<details>
<summary><strong>Q14: [Trade-off] Iterator vs. Streams — when do you use which?</strong></summary>

Both traverse sequences, but differ in **who drives the loop** and **what they optimize for**:

- **Iterator (external):** the client pulls each element, can **stop early** trivially, integrates with imperative control flow (break/continue/return), is easy to debug and step through, and is the right choice when you need **fine-grained manual control** or a stateful cursor (e.g., a DB `ResultSet`, merging sorted streams).
- **Streams (internal):** you declare a **pipeline** (`filter/map/reduce`); the library drives iteration, enabling **laziness, short-circuiting, fusion, and parallelism** (`parallelStream`). Best for **declarative transformations and aggregations** with little boilerplate.

Use Streams for data-processing pipelines and when parallelism helps; use an Iterator when you need explicit control, incremental/manual advancement, interop with old APIs, or a genuinely stateful cursor. They interoperate: `StreamSupport.stream(iterable.spliterator(), false)` and `stream.iterator()` bridge both ways.
</details>

<details>
<summary><strong>Q15: [Trade-off] Fail-fast vs. fail-safe iterators — compare them with examples.</strong></summary>

**Fail-fast** iterators (`ArrayList`, `HashMap`, `HashSet`) throw `ConcurrentModificationException` as soon as they detect structural modification during iteration (via `modCount`). They iterate the **live** collection, so they're memory-cheap, but they're **best-effort** and unsafe for concurrent mutation.

**Fail-safe** (better called **weakly consistent**) iterators (`CopyOnWriteArrayList`, `ConcurrentHashMap`, `ConcurrentLinkedQueue`) never throw CME. They iterate over a **snapshot** (COW copies the array on write) or a **weakly consistent view** (ConcurrentHashMap reflects some but not necessarily all concurrent updates). They tolerate concurrent modification but may not reflect the latest state and (for COW) cost memory/time on writes.

<details>
<summary>💻 Click to expand</summary>

```java
// Fail-safe: no CME even while mutating during iteration.
List<String> cow = new java.util.concurrent.CopyOnWriteArrayList<>(List.of("a","b","c"));
for (String s : cow) {           // iterates a snapshot taken at iterator creation
    cow.add("x");                // safe — the snapshot doesn't change; no CME
}
```
</details>

Trade-off: fail-fast surfaces single-threaded misuse loudly and is cheap; fail-safe supports true concurrency at the cost of staleness/overhead. Choose concurrent collections for multi-threaded traversal — not fail-fast + external locking bolted on.
</details>

<details>
<summary><strong>Q16: [Trade-off] When would you NOT implement the Iterator pattern, and what would you use instead?</strong></summary>

Skip a custom iterator when: (1) **the JDK already gives you one** — for a plain in-memory list/set, just implement `Iterable` (or use the collection directly); rolling your own adds nothing. (2) **You need random/indexed access** (`get(i)`) or bidirectional/jumping traversal — an iterator is inherently sequential and forward-only; use `List` indexing or `ListIterator`. (3) **You need rich queries** (filter/map/group/join) — Streams express these far better than a hand-written stateful iterator. (4) **The collection is tiny and fixed** — a simple loop is clearer. (5) **Heavy concurrent mutation during traversal** — use a concurrent/fail-safe collection, not a custom fail-fast iterator.

The judgment: the Iterator pattern earns its keep when you're **hiding a non-trivial or changeable representation**, offering **multiple traversal orders**, needing **independent cursors**, or producing data **lazily** (streamed/infinite/paginated). Otherwise reach for the standard library.
</details>

<details>
<summary><strong>Q17: [Advanced] What is a Spliterator, why was it introduced, and how does it enable parallel streams?</strong></summary>

`Spliterator` (Java 8) is the Iterator **generalized for parallelism**. Beyond advancing one element (`tryAdvance`), it can **split** itself: `trySplit()` returns a new Spliterator covering *part* of the elements, letting the fork/join framework recursively partition the data across threads. It also exposes **characteristics** (`SIZED`, `SUBSIZED`, `ORDERED`, `SORTED`, `DISTINCT`, `IMMUTABLE`, `NONNULL`, `CONCURRENT`) and `estimateSize()`, which the Stream framework uses to optimize (e.g., skip a merge step for unordered, pre-size arrays for `SIZED`).

Why introduced: the classic `Iterator` **cannot be split** — it's purely sequential — so it can't drive efficient parallel processing. Streams need to partition work; `Spliterator` is the abstraction that makes `parallelStream()` possible. A good `trySplit` yields balanced halves (an `ArrayList`'s splits by index range cheaply; a `LinkedList`'s splits poorly, which is why linked structures parallelize badly).

<details>
<summary>💻 Click to expand</summary>

```java
Spliterator<Integer> sp = IntStream.range(0, 1000).boxed().spliterator();
Spliterator<Integer> half = sp.trySplit();   // ~500 each; framework recurses to fill threads
System.out.println(sp.characteristics() & Spliterator.SIZED);  // non-zero: size known
```
</details>
</details>

<details>
<summary><strong>Q18: [Advanced] How do you design an iterator for streaming/distributed data with backpressure? Contrast pull-based iteration with reactive streams.</strong></summary>

A classic `Iterator` is **pull-based**: the consumer calls `next()` when ready, so it inherently has backpressure — the producer only does work when pulled (great for lazy/paginated sources). But pull is **synchronous/blocking**: `next()` blocks while a page/row is fetched, which wastes threads for high-latency I/O and doesn't compose across async boundaries.

For distributed/streaming data you move to **reactive streams** (Reactive Streams spec / `Flow` API in Java 9): a **push** model where the publisher pushes items to a subscriber, but with **`request(n)` backpressure** — the subscriber signals how many items it can handle, so the publisher never overwhelms it. This decouples producer and consumer rates asynchronously and supports non-blocking I/O.

Design points: for a paginated/cursor source, expose an async iterator (`CompletableFuture<Page>` per fetch) or a reactive `Publisher` that fetches a page when demand arrives; **prefetch** a bounded buffer to hide latency; propagate **cancellation** (stop fetching when the subscriber unsubscribes); handle **retries/timeouts** per fetch; and preserve **ordering** if required. The mental model: pull-Iterator = "consumer asks, one at a time, blocking"; reactive = "producer pushes up to `n` requested, async, with flow control." Both keep memory bounded; reactive scales better for many concurrent high-latency streams.
</details>

<details>
<summary><strong>Q19: [Coding Challenge] Implement a "peeking" iterator that supports peek() (look at the next element without consuming) on top of any Iterator.</strong></summary>

A common FAANG question (LeetCode "Peeking Iterator") — tests the buffer-one-element technique.

<details>
<summary>💻 Click to expand full solution</summary>

```java
import java.util.*;

class PeekingIterator<T> implements Iterator<T> {
    private final Iterator<T> it;
    private T buffered;
    private boolean hasBuffered = false;

    PeekingIterator(Iterator<T> it) { this.it = it; }

    /** Look at the next element without advancing. */
    public T peek() {
        if (!hasBuffered) {                 // fetch-and-stash on first peek
            if (!it.hasNext()) throw new NoSuchElementException();
            buffered = it.next();
            hasBuffered = true;
        }
        return buffered;
    }
    @Override public boolean hasNext() { return hasBuffered || it.hasNext(); }
    @Override public T next() {
        if (hasBuffered) {                  // return and clear the buffered element
            T result = buffered; buffered = null; hasBuffered = false;
            return result;
        }
        return it.next();
    }
    public static void main(String[] args) {
        PeekingIterator<Integer> p = new PeekingIterator<>(List.of(1, 2, 3).iterator());
        System.out.println(p.peek());  // 1 (not consumed)
        System.out.println(p.next());  // 1
        System.out.println(p.peek());  // 2
        System.out.println(p.next());  // 2
        System.out.println(p.next());  // 3
        System.out.println(p.hasNext());// false
    }
}
```
</details>

The technique: **buffer one element**. `peek()` pulls from the source once and stashes it, setting a flag; `next()` returns the buffered element if present (and clears the flag) else pulls fresh; `hasNext()` is true if buffered *or* the source has more. This one-element lookahead buffer is the same primitive used by filter/merge iterators and parsers.
</details>

<details>
<summary><strong>Q20: [Coding Challenge] Implement a "zigzag"/round-robin iterator that interleaves elements from k iterators (v1, v2, ...).</strong></summary>

Generalized LeetCode "Zigzag Iterator" — tests managing multiple sub-iterators fairly.

<details>
<summary>💻 Click to expand full solution</summary>

```java
import java.util.*;

class ZigzagIterator<T> implements Iterator<T> {
    private final Queue<Iterator<T>> queue = new LinkedList<>();

    @SafeVarargs
    ZigzagIterator(Iterator<T>... iterators) {
        for (Iterator<T> it : iterators) if (it.hasNext()) queue.add(it);  // only non-empty
    }
    @Override public boolean hasNext() { return !queue.isEmpty(); }
    @Override public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        Iterator<T> it = queue.poll();      // take the next iterator in round-robin order
        T result = it.next();
        if (it.hasNext()) queue.add(it);    // if it still has elements, re-enqueue at the back
        return result;
    }
    public static void main(String[] args) {
        Iterator<Integer> v1 = List.of(1, 2).iterator();
        Iterator<Integer> v2 = List.of(3, 4, 5, 6).iterator();
        Iterator<Integer> v3 = List.of(7).iterator();
        ZigzagIterator<Integer> zz = new ZigzagIterator<>(v1, v2, v3);
        while (zz.hasNext()) System.out.print(zz.next() + " ");  // 1 3 7 2 4 5 6
    }
}
```
</details>

The elegant trick: a **queue of iterators**. Each `next()` dequeues the front iterator, takes one element, and re-enqueues that iterator at the back if it still has elements. This naturally round-robins across k iterators and self-adjusts as some exhaust — generalizing the two-list zigzag to any number of sources cleanly and in O(1) per element.
</details>

### 🧠 Staff / Principal Engineer Level

<details>
<summary><strong>SP1: [Staff] Design an iterator abstraction for cursor-based pagination over a dataset of billions of rows served by a REST/gRPC API. What are the key decisions?</strong></summary>

I'd expose an `Iterable`/`Stream` façade so callers write ordinary loops, backed by a **lazy cursor iterator** that fetches one page per network round-trip **only when the current page drains**. Key decisions: (1) **Cursor-based, not offset-based pagination** — pass an opaque `nextPageToken`/cursor rather than `OFFSET n`, because offset pagination is O(n) on the DB and breaks under concurrent inserts (skipped/duplicated rows); a stable cursor (keyset on an indexed sort key) gives O(1) page fetches and consistency. (2) **Prefetching** — optionally fetch page N+1 while the caller consumes page N to hide latency, with a bounded buffer for backpressure. (3) **Retry/timeout/rate-limit handling** inside the fetch (exponential backoff + jitter, respect `Retry-After`), since network calls fail. (4) **Idempotent, resumable iteration** — the cursor token *is* a resume point; persist it so a crashed job restarts mid-stream without re-reading everything. (5) **Resource + error semantics** — make it `AutoCloseable`, wrap checked exceptions, propagate cancellation to stop fetching. (6) **Consistency model** — decide snapshot vs. live; document that long scans may see concurrent mutations unless the API offers a snapshot/consistent cursor. (7) **Observability** — metrics per page (latency, size, retry count) and total rows. The pattern gives the "one flat sequence" abstraction; the engineering is making the cursor stable, resumable, backpressured, fault-tolerant, and observable — which is exactly how Google Cloud `Page`/`PageIterable` and AWS SDK paginators are built.
</details>

<details>
<summary><strong>SP2: [Staff] Why is offset-based pagination a scalability trap, and how does keyset/cursor pagination fix it? Relate this to iterator design.</strong></summary>

Offset pagination (`LIMIT 20 OFFSET 10000`) forces the database to **scan and discard** the first 10,000 rows on every page fetch, so page N costs O(N·pageSize) — deep pages get catastrophically slow (the classic "page 5000 times out" incident). Worse, it's **inconsistent under concurrent writes**: if a row is inserted/deleted before your offset between fetches, you **skip or duplicate** rows, so an iterator built on it silently returns a corrupt sequence.

**Keyset (cursor) pagination** instead remembers the **last-seen sort key** and queries `WHERE (sort_key) > :lastKey ORDER BY sort_key LIMIT 20`, using an index to jump straight to the next page — O(log n + pageSize) regardless of depth, and stable because the cursor anchors to actual data, not a positional count. From the **iterator design** angle, this maps perfectly: the iterator's *traversal state* is the opaque cursor token (the last key), `next()`/page-fetch advances from that token, and the token is **serializable/resumable** — you can persist it and continue the iteration later or on another machine. The staff insight: an iterator over remote data must carry **position as data anchored to the content** (a cursor), not as an ordinal index, exactly so it stays correct and cheap under concurrency and at scale. This is why every mature API (Stripe, Slack, GitHub, Google, AWS) exposes opaque `cursor`/`page_token` values, not offsets.
</details>

<details>
<summary><strong>SP3: [Principal] How would you design a Spliterator for a custom data structure to make parallel stream processing efficient? What characteristics matter?</strong></summary>

The goal is a `trySplit()` that yields **balanced, cheap-to-produce partitions** and accurate **characteristics** so the Stream framework can optimize. For an array-backed or index-addressable structure, `trySplit` bisects the index range — O(1), balanced, ideal. For a tree, split at subtrees (each child becomes a sub-spliterator) — balance depends on the tree's shape, so you may split recursively until partitions are small enough (guided by a threshold near `estimateSize()/parallelism`). For inherently sequential structures (linked lists, streams-of-unknown-size), splitting is poor or impossible, so parallelism won't help and you should say so via characteristics.

Characteristics that matter: **`SIZED`/`SUBSIZED`** (exact size known and splits stay sized) let the framework pre-allocate arrays and skip resizing; **`ORDERED`** determines whether encounter order must be preserved (dropping it enables faster unordered ops); **`IMMUTABLE`/`CONCURRENT`** tell the framework it needn't guard against interference (no late-binding snapshot needed); **`DISTINCT`/`SORTED`** let `distinct()`/`sorted()` be elided; **`NONNULL`** skips null checks. A good `estimateSize()` drives the split decision — a bad estimate causes over- or under-splitting. The principal-level point: parallel speedup hinges on **decomposability** (can you split cheaply and evenly?) and **honest characteristics** (so the pipeline can prune work); a structure that can't split well (linked list) or lies about size will parallelize slowly or incorrectly. Prefer extending `Spliterators.AbstractSpliterator` only for sequential fallbacks; hand-write `trySplit` when you can partition cheaply.
</details>

<details>
<summary><strong>SP4: [Principal] Compare pull-based iteration, push-based (observer/reactive), and internal iteration (streams) as architectural choices for data processing. When do you pick each?</strong></summary>

Three models, chosen by **who controls flow, latency profile, and composition needs**. **Pull (Iterator):** the consumer drives, requesting one element at a time — inherent backpressure, trivial early termination, simple debugging, and perfect for **lazy, high-latency, or unbounded sources you consume synchronously** (paginated APIs, DB cursors, file scans). Downside: blocking `next()` ties up a thread per stream, so it scales poorly for *many* concurrent high-latency streams. **Push (Observer/Reactive):** the producer drives, emitting items to subscribers; with **`request(n)` backpressure** (Reactive Streams/`Flow`) it stays bounded while being **non-blocking and async**, ideal for **many concurrent I/O-bound streams, event-driven systems, and rate-mismatched producer/consumer** — at the cost of much higher complexity (error/cancellation/backpressure semantics are hard). **Internal iteration (Streams):** you declare a pipeline and the library drives the loop, giving **laziness, fusion, short-circuiting, and easy data parallelism** for **in-memory or splittable bulk transforms**; less suited to async I/O or fine-grained control. My decision framework: default to **Streams** for in-process bulk data transforms; use a **pull Iterator** for synchronous lazy traversal of external/unbounded sources with simple control flow; escalate to **reactive push** only when you have many concurrent async streams or genuine producer/consumer rate mismatch that demands non-blocking backpressure. Over-adopting reactive is a common, expensive mistake (operational and cognitive cost); under-adopting it (blocking pull for thousands of concurrent streams) exhausts threads. Match the model to the concurrency and latency reality, and remember all three are refinements of "advance through elements."
</details>

<details>
<summary><strong>SP5: [Principal] A batch job iterates a huge remote dataset and occasionally produces wrong/duplicated/missing results non-deterministically. How do you diagnose and prevent this class of bug?</strong></summary>

Non-deterministic wrong results over a remote iteration almost always trace to **unstable pagination, non-idempotent retries, or concurrent mutation** during the scan. Diagnosis: (1) **log the cursor/token and page boundaries** per fetch and replay — if replay diverges, the cursor isn't stable. (2) Check for **offset-based pagination under concurrent writes** (skips/dupes) — switch to keyset/cursor pagination anchored to an immutable sort key. (3) Check **retry semantics** — if a page fetch times out, is retried, and the server had already advanced a stateful cursor, you can skip or double-read a page; make fetches **idempotent** (token fully determines the page, no server-side mutable cursor) and dedup by a stable element key. (4) Look for **non-idempotent `hasNext()`** (peeking that consumes) and **shared iterator state** across threads. (5) Verify the **consistency model**: a long scan over a mutating table without a snapshot will legitimately see partial updates — require a **consistent snapshot / MVCC read** or accept and document eventual semantics. Prevention: use **opaque cursors anchored to content**, make paging **idempotent and resumable** (persist the cursor as a checkpoint so restarts don't reprocess or skip), add **property/invariant checks** (e.g., total count reconciliation, no duplicate keys via a bloom filter/dedup set for spot checks), enforce **at-least-once + dedup = effectively-once** processing downstream, and add **observability** (per-page metrics, gap detection on the sort key). The meta-point: a remote iterator is a distributed-systems artifact — its correctness depends on a stable cursor, idempotent fetches, a defined consistency model, and resumable checkpoints, not just the `hasNext()/next()` contract.
</details>

---

## ⚡ Quick Revision

**One-liner:** Iterator provides sequential access to a collection's elements without exposing its internal representation, by extracting traversal into a cursor object (`hasNext()`/`next()`) produced by a factory method on the collection.

**The whole pattern in one paragraph:** The Iterator pattern is a behavioral GoF pattern — *"provide a way to access the elements of an aggregate object sequentially without exposing its underlying representation."* Its essence: put the **traversal state on a cursor**, not the collection, and let the collection be a **factory** (`iterator()`) that hands out cursors. Four participants: **Iterator** (`hasNext`, `next`, optional `remove`), **ConcreteIterator** (holds the position/state and knows how to walk one structure), **Aggregate/Iterable** (the `iterator()` factory — implement `java.lang.Iterable` and the **enhanced for-loop works for free**, since it's pure sugar over `iterator()/hasNext()/next()`), and **ConcreteAggregate** (the real collection). Because state is on the cursor, you get **independent simultaneous traversals**; because the interface is uniform, the same algorithm walks lists, sets, trees, DB cursors, or infinite sequences (dependency inversion); and because `next()` *encapsulates advancement*, iterators can be **lazy** — computing/fetching on demand — covering infinite (Fibonacci), streamed (file lines, `ResultSet`), and **paginated API** data with flat memory. Distinguish **external** iteration (client pulls, can stop early, single-use — the classic Iterator) from **internal** iteration (Streams/`forEach` — library drives the loop, enabling laziness, short-circuiting, and parallelism). JDK collections are **fail-fast**: a `modCount` bumped on every structural change is snapshotted into the iterator's `expectedModCount` and re-checked on each `next()`; a mismatch throws `ConcurrentModificationException` — but this is **best-effort, not a thread-safety guarantee**. Removing during iteration is only safe via `Iterator.remove()` (or `removeIf`); direct `collection.remove()` in a for-each throws CME. For concurrency use **fail-safe/weakly-consistent** iterators (`CopyOnWriteArrayList` snapshot, `ConcurrentHashMap` weak view). **`Spliterator`** is the Iterator generalized for parallelism (`tryAdvance` + `trySplit` + characteristics like `SIZED`/`ORDERED`/`IMMUTABLE`), the foundation of parallel Streams. For remote/distributed iteration, use **cursor/keyset pagination** (opaque token as resumable traversal state), idempotent fetches, retry/backoff, and prefetch/backpressure; pull-Iterators give natural backpressure but block, whereas reactive push (`Flow`, `request(n)`) handles many async streams. Real JDK examples: `java.util.Iterator`/`Iterable`, Streams/`Spliterator`, `Scanner`, JDBC `ResultSet`, Guava `AbstractIterator`. Use it to hide representation, offer multiple traversal orders, run independent cursors, or produce data lazily; skip it for trivial in-memory walks (use the built-in), random access (use indexing), or rich queries (use Streams).

**Top 5 interview answers to memorize:**

1. **"What is it?"** → Access elements sequentially without exposing the representation; the cursor holds the position and exposes `hasNext()`/`next()`, so traversal is decoupled from storage and multiple independent walks are possible.
2. **"How does for-each work / ConcurrentModificationException?"** → for-each desugars to `iterator()/hasNext()/next()`; fail-fast iterators compare `modCount` vs. `expectedModCount` on each `next()` and throw CME on structural modification not done through `Iterator.remove()`.
3. **"External vs. internal / Iterator vs. Streams?"** → External = client pulls, can stop early, single-use (Iterator); internal = library drives the loop, lazy, composable, parallelizable (Streams built on `Spliterator`).
4. **"Fail-fast vs. fail-safe?"** → Fail-fast (`ArrayList`) throws CME on concurrent modification (best-effort, live view); fail-safe/weakly-consistent (`CopyOnWriteArrayList`, `ConcurrentHashMap`) iterate a snapshot/weak view and never throw, at the cost of staleness/overhead.
5. **"Iterate a paginated API or infinite sequence?"** → A lazy iterator that fetches/computes in `next()`; hold a current-page cursor + next-page token, fetch the next page only when the current drains; use cursor (keyset) pagination for stable, resumable, O(1) paging.

**Trigger words (hear these → think Iterator):** "traverse without exposing internals", "hasNext/next", "for-each / Iterable", "cursor", "ConcurrentModificationException / fail-fast", "modCount", "custom iterator for a tree/matrix/list", "lazy / infinite sequence", "paginated API / next page token", "DFS/BFS traversal", "flatten nested list", "peeking iterator", "zigzag/round-robin", "Spliterator / parallel stream", "ResultSet / Scanner", "external vs. internal iteration", "fail-safe / snapshot iterator".

---

*End of Iterator Pattern study guide.*


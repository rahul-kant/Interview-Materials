# Flyweight Pattern ⭐⭐⭐⭐ (Difficulty: 4/5 — the factory-and-cache mechanics are easy, but correctly splitting intrinsic vs. extrinsic state, guaranteeing immutability/thread-safety, and knowing when sharing actually pays are what separate a real answer from a memorized one)

> **Category:** Structural Pattern (GoF)
> **Also known as:** (no common alias; closely related to *interning* and *canonicalization*)

The Flyweight pattern **uses sharing to support huge numbers of fine-grained objects efficiently** by splitting each object's state into *intrinsic* state (shared, context-independent, stored inside the flyweight) and *extrinsic* state (context-dependent, passed in by the client). It's why a text editor can render a million-character document with only ~100 glyph objects, a game can draw a forest of a million trees from a handful of shared meshes, and `Integer.valueOf(127) == Integer.valueOf(127)` is `true` in the JVM.

---

## Table of Contents

1. [📋 Intent](#-intent)
2. [🎯 Problem](#-problem)
3. [✅ Solution](#-solution)
4. [💻 Implementation](#-implementation)
   - [Variant 0: One Heavyweight Object Per Instance (Anti-pattern)](#variant-0-one-heavyweight-object-per-instance-anti-pattern)
   - [Variant 1: Shared Objects Without a Factory](#variant-1-shared-objects-without-a-factory)
   - [Variant 2: Flyweight Factory with a Cache](#variant-2-flyweight-factory-with-a-cache)
   - [Variant 3: Production Flyweight (immutable + thread-safe factory)](#variant-3-production-flyweight-immutable--thread-safe-factory)
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

> **GoF Definition:** *"Use sharing to support large numbers of fine-grained objects efficiently."*

The Flyweight pattern exists to solve a **memory** problem: you need an enormous number of objects that are individually tiny but collectively ruinous, and most of them are *identical in the part that costs memory*. The core insight is that an object's state can be partitioned. **Intrinsic state** is the part that is context-independent and shared across many logical objects — a character's font family and glyph outline, a tree's mesh and texture, a chess piece's shape and color rules. **Extrinsic state** is the part that varies per logical instance and depends on context — the character's position in the document, the tree's (x, y) coordinate, the piece's board square.

Flyweight stores only the intrinsic state inside a small pool of **shared, immutable flyweight objects** (typically one per distinct intrinsic value), and requires the client to supply the extrinsic state at each method call rather than storing it. A **factory** guarantees sharing: ask it for a flyweight with given intrinsic state and it returns the existing instance if one exists, creating (and caching) it only on first request. The payoff: a million logical objects backed by a few dozen real objects.

---

## 🎯 Problem

You must represent a **very large number of similar objects**, and doing so naively blows the heap. Each object carries heavyweight, repeated data — a font, an image, a lookup table — and you're storing that data redundantly millions of times even though only a handful of distinct values exist.

**The pain points that lead you to Flyweight:**

- The application creates **millions (or more) of fine-grained objects**, and their combined memory footprint is the bottleneck (`OutOfMemoryError`, GC thrashing).
- Most of each object's memory is **duplicated identical data** — the objects differ only in a small amount of per-instance context.
- That heavyweight data is **immutable or effectively constant** (fonts, sprites, config), so it *can* be safely shared.
- Removing the redundancy would collapse the object count from millions of full objects to a few shared cores plus cheap per-instance context.

**Concrete example scenarios:**

1. **Text editor / word processor.** A document has millions of characters. A naive `Character` object storing font, size, style, *and* glyph bitmap per character is enormous. Intrinsic (the glyph for 'a' in Times 12pt bold) is shared; extrinsic (its row/column position) is passed in when rendering.

2. **Game with massive scenery.** A forest of a million trees, or a bullet-hell shooter with 100k projectiles. The mesh, texture, and material (intrinsic) are shared across all trees of a species; each tree's position, scale, and rotation (extrinsic) are per-instance.

3. **Map/graphics with many markers.** Thousands of identical pin icons on a map — the icon bitmap is intrinsic and shared; each pin's coordinate is extrinsic.

4. **Syntax highlighting / token streams.** A source file tokenized into millions of tokens; the token *type* metadata (keyword styling, color) is intrinsic and shared; the token's offset/length is extrinsic.

---

## ✅ Solution

The core idea, in plain language: **stop storing the heavy, repeated data in every object. Identify the part of the state that is shared and unchanging (intrinsic) and store it in a small pool of shared objects; identify the part that varies per instance (extrinsic) and have the client pass it into methods instead of storing it. Route all creation through a factory that hands back an already-existing shared object whenever the intrinsic state matches.**

**Key structural elements:**

- **Flyweight** — the interface declaring methods that accept **extrinsic** state as parameters (e.g., `render(int x, int y)`). Flyweights must not store extrinsic state.
- **ConcreteFlyweight** — a shared, **immutable** object holding only intrinsic state. Because it's immutable and context-free, it's safe to share across any number of contexts and threads.
- **UnsharedConcreteFlyweight** — (optional) an object that implements the interface but isn't shared, for the rare case where some objects genuinely can't be shared.
- **FlyweightFactory** — maintains a **cache/pool** (usually a `Map` keyed by intrinsic state) and returns a shared flyweight, creating it lazily on first request. This is the *only* legitimate way to obtain flyweights, which is what enforces sharing.
- **Client** — holds/computes the extrinsic state per logical object and passes it to flyweight methods; it references flyweights only through the factory.

**The mechanism that makes it work:** *canonicalization plus immutability*. The factory **canonicalizes** — maps each distinct intrinsic value to exactly one instance — so N logical objects sharing the same intrinsic value cost one real object plus N cheap extrinsic contexts. **Immutability** is what makes the sharing safe: because a flyweight's intrinsic state never changes, two unrelated clients holding the same flyweight can't interfere with each other, and the object is inherently thread-safe to read. Moving extrinsic state *out* of the object (into method parameters or a thin context object) is the discipline that lets the intrinsic core be shared at all — store extrinsic state inside the flyweight and the whole scheme collapses back into per-instance objects.

## 💻 Implementation

We'll model a **text editor's character glyphs** — the canonical GoF Flyweight scenario (it's literally the example in the original book) and a FAANG favorite because the memory arithmetic is stark: millions of characters, but only a few hundred distinct (character, font) combinations.

### Variant 0: One Heavyweight Object Per Instance (Anti-pattern)

**What's wrong with it:** Every character in the document is a full object storing its symbol *and* a copy of its font metadata (family, size, style, and — worst — the rendered glyph bitmap). A 5-million-character document allocates 5 million fat objects, most holding byte-for-byte identical font data. Memory explodes and the GC chokes, even though there are only a few dozen distinct fonts.

<details>
<summary>💻 Click to expand code — the anti-pattern</summary>

```java
// DON'T DO THIS — every character duplicates its heavy font data.
class FatCharacter {
    private final char symbol;
    private final String fontFamily;   // duplicated millions of times
    private final int    fontSize;     // duplicated
    private final boolean bold;         // duplicated
    private final byte[]  glyphBitmap;  // HUGE and duplicated — the real killer
    private final int x, y;             // the only genuinely per-instance data

    FatCharacter(char s, String f, int sz, boolean b, byte[] glyph, int x, int y) {
        this.symbol=s; this.fontFamily=f; this.fontSize=sz; this.bold=b;
        this.glyphBitmap=glyph; this.x=x; this.y=y;
    }
}
// A 5,000,000-char document => 5,000,000 glyph bitmaps in memory. OutOfMemoryError.
```
</details>

**Pros:** Dead simple; fine for a few hundred objects.
**Cons:** Memory scales linearly with object *count* even though distinct *values* are few; catastrophic GC pressure; cache-unfriendly. It conflates shared data (font/glyph) with per-instance data (position).
**Mechanism (why it fails):** there is **no separation of intrinsic from extrinsic state** and **no sharing** — identical heavy data is copied into every object.

---

### Variant 1: Shared Objects Without a Factory

**What problem it solves:** Split state. A `Glyph` holds only **intrinsic** state (symbol + font + bitmap) and exposes `draw(x, y)` taking **extrinsic** state as parameters. The document stores lightweight positioned references. This removes the per-character duplication — *but* without a factory, nothing stops callers from `new Glyph(...)`-ing duplicates, so sharing is accidental, not guaranteed.

<details>
<summary>💻 Click to expand code — split state, but no factory</summary>

```java
// FLYWEIGHT — intrinsic state only; extrinsic (x,y) comes in as parameters.
final class Glyph {
    private final char symbol;
    private final String font;
    private final byte[] bitmap;   // heavy, but now shared (in principle)
    Glyph(char symbol, String font, byte[] bitmap) {
        this.symbol = symbol; this.font = font; this.bitmap = bitmap;
    }
    void draw(int x, int y) {       // extrinsic state passed IN, not stored
        System.out.println("Draw '" + symbol + "' [" + font + "] at (" + x + "," + y + ")");
    }
}

// Client stores light per-instance context referencing a Glyph.
class PositionedChar {
    final Glyph glyph; final int x, y;   // x,y = extrinsic; glyph = shared intrinsic
    PositionedChar(Glyph g, int x, int y) { this.glyph = g; this.x = x; this.y = y; }
}

// PROBLEM: nothing prevents this —
// Glyph a1 = new Glyph('a', "Times12", bitmap);
// Glyph a2 = new Glyph('a', "Times12", bitmap);  // duplicate! sharing not enforced
```
</details>

**Pros:** Extrinsic state is out of the object; the intrinsic core *can* now be shared; memory drops if callers are disciplined.
**Cons:** Sharing is not *enforced* — any caller can create duplicate flyweights, quietly reintroducing the bloat; no single source of truth for "the glyph for 'a' in Times 12."
**Mechanism:** *state partitioning* is in place (intrinsic vs. extrinsic), but the *canonicalization* guarantee is missing.

---

### Variant 2: Flyweight Factory with a Cache

**What problem it solves:** Introduce a **FlyweightFactory** that owns a `Map` keyed by intrinsic state. Clients *never* construct glyphs directly — they ask the factory, which returns the cached instance or creates-and-caches on first request. Now sharing is **guaranteed**: exactly one `Glyph` object per distinct (symbol, font). The document of 5M characters is backed by a few hundred glyphs.

<details>
<summary>💻 Click to expand code — factory-enforced sharing</summary>

```java
import java.util.*;

final class Glyph {
    private final char symbol; private final String font;
    // pretend bitmap loading is expensive/heavy
    Glyph(char symbol, String font) { this.symbol = symbol; this.font = font; }
    void draw(int x, int y) { System.out.println("'" + symbol + "'[" + font + "]@(" + x + "," + y + ")"); }
}

// FACTORY — the ONLY way to get a Glyph; enforces one-per-intrinsic-value.
class GlyphFactory {
    private final Map<String, Glyph> pool = new HashMap<>();

    Glyph getGlyph(char symbol, String font) {
        String key = symbol + "|" + font;          // key = the intrinsic state
        Glyph g = pool.get(key);
        if (g == null) {                            // lazy creation on first request
            g = new Glyph(symbol, font);
            pool.put(key, g);
        }
        return g;                                    // same instance returned every time
    }
    int distinctGlyphs() { return pool.size(); }
}

class Demo {
    public static void main(String[] args) {
        GlyphFactory factory = new GlyphFactory();
        String text = "banana bandana";              // lots of repeated letters
        int x = 0;
        for (char c : text.toCharArray()) {
            Glyph g = factory.getGlyph(c, "Times12"); // shared instance for each distinct char
            g.draw(x++, 0);                            // extrinsic position passed in
        }
        System.out.println("Chars: " + text.length() + ", distinct glyphs: " + factory.distinctGlyphs());
        // Chars: 14, distinct glyphs: 6  (b,a,n,space,d) -> huge ratio at document scale
    }
}
```
</details>

**Pros:** Sharing is enforced — impossible to create duplicates; lazy creation means you only pay for glyphs actually used; memory now scales with *distinct values*, not total count.
**Cons:** Not thread-safe as written (`HashMap` + check-then-put race); the cache grows unbounded; `Glyph` isn't declared immutable (relies on discipline).
**Mechanism:** *canonicalization via a factory cache* — the `Map` maps each intrinsic key to exactly one instance, guaranteeing structural sharing.

---

### Variant 3: Production Flyweight (immutable + thread-safe factory)

**What problem it solves:** Make it correct under concurrency and safe by construction. The flyweight is made **strictly immutable** (all fields `final`, no setters) so sharing across threads is inherently safe. The factory uses `ConcurrentHashMap.computeIfAbsent` for **atomic, thread-safe, race-free** get-or-create. This is the version you defend in an interview: immutable flyweights + a concurrent, lazily-populated canonicalizing factory.

<details>
<summary>💻 Click to expand code — production flyweight</summary>

```java
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Objects;

// FLYWEIGHT — strictly immutable: safe to share across any number of threads.
final class Glyph {
    private final char symbol;      // intrinsic
    private final String font;      // intrinsic
    // (a real glyph would hold an immutable bitmap; kept small here)
    Glyph(char symbol, String font) { this.symbol = symbol; this.font = font; }

    // Extrinsic state (x, y, color) is passed IN — never stored.
    void draw(int x, int y, String color) {
        System.out.println("'" + symbol + "'[" + font + "," + color + "]@(" + x + "," + y + ")");
    }
    @Override public String toString() { return symbol + "/" + font; }
}

// FACTORY — thread-safe, race-free lazy canonicalization.
final class GlyphFactory {
    private final Map<String, Glyph> pool = new ConcurrentHashMap<>();

    Glyph getGlyph(char symbol, String font) {
        String key = symbol + " " + font;               // unambiguous composite key
        // computeIfAbsent is atomic: no double-creation even under concurrent access.
        return pool.computeIfAbsent(key, k -> new Glyph(symbol, font));
    }
    int distinctGlyphs() { return pool.size(); }
}

// DEMO — concurrent access; still exactly one Glyph per (symbol, font).
public class TextEditorDemo {
    public static void main(String[] args) throws InterruptedException {
        GlyphFactory factory = new GlyphFactory();
        String doc = "the quick brown fox jumps over the lazy dog".repeat(100_000); // ~4.3M chars

        // Render (single-threaded here for output brevity) — position is extrinsic:
        // int x = 0; for (char c : doc.toCharArray()) factory.getGlyph(c, "Mono12").draw(x++, 0, "black");

        // Thread-safety test: many threads request glyphs concurrently.
        Runnable job = () -> { for (char c : doc.toCharArray()) factory.getGlyph(c, "Mono12"); };
        Thread t1 = new Thread(job), t2 = new Thread(job), t3 = new Thread(job);
        t1.start(); t2.start(); t3.start();
        t1.join();  t2.join();  t3.join();

        System.out.println("Document chars: " + doc.length());          // ~4,300,000
        System.out.println("Distinct glyphs: " + factory.distinctGlyphs()); // ~27 (26 letters + space)
        // 4.3M logical characters backed by ~27 shared Glyph objects, safely under concurrency.
    }
}
```
</details>

**Pros:** Immutable flyweights are inherently thread-safe to share; `computeIfAbsent` guarantees exactly one instance even under heavy concurrency; memory scales with distinct intrinsic values; lazy population avoids paying for unused glyphs.
**Cons:** Unbounded cache is still a potential leak for open-ended key spaces (needs bounding/eviction — see pitfalls); `computeIfAbsent` briefly holds a bin lock, so the mapping function must be fast and must not modify the same map; slight overhead per lookup vs. a raw field.
**Mechanism:** *immutability (safe publication + no shared mutable state) + atomic canonicalization (`computeIfAbsent`)*. Because `Glyph` is immutable, the JMM guarantees any thread that obtains a reference sees a fully-constructed object; because the map operation is atomic, two threads racing on the same key still get the same single instance.

#### Variant 3b: Production Flyweight at scale — shared spreadsheet cell styles (the FAANG-grade example)

**Why this example is stronger:** the glyph demo above has to *manufacture* concurrency (a text editor really renders on one UI thread, so three threads hammering `getGlyph` is a bit contrived). A **spreadsheet** exhibits the same Flyweight the way real production systems do, with concurrency that is *native* to the problem. A large sheet has **millions of cells** but only a **few hundred distinct styles** — the tuple of font, weight, background color, number format, and alignment. That style is **intrinsic** (identical wherever it's applied) and heavy-ish (a real engine stores font handles, parsed format patterns, color objects); the cell's **value and (row, col) coordinate** are **extrinsic**. Concurrency is genuine here: collaborative editors, background recalculation, and server-side export/render workers all create and look up styles simultaneously — exactly what a thread-safe canonicalizing factory is for. This is precisely how **Apache POI** models `CellStyle` (styles are shared per-workbook and capped at ~64k), and how Google-Sheets-scale engines keep a massive grid in memory.

<details>
<summary>💻 Click to expand code — shared cell-style flyweight + concurrent demo</summary>

```java
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/* ============================================================
 * FLYWEIGHT (intrinsic, immutable, shared): the visual STYLE of a cell.
 *   One instance per distinct (font, size, bold, bg, numberFormat, align).
 *   Shared by every cell that looks the same — millions of them.
 * ============================================================ */
final class CellStyle {
    final String  fontFamily;     // intrinsic
    final int     fontSize;       // intrinsic
    final boolean bold;           // intrinsic
    final String  bgColor;        // intrinsic
    final String  numberFormat;   // intrinsic (e.g., "#,##0.00", "0%", "yyyy-mm-dd")
    final String  align;          // intrinsic ("LEFT"/"CENTER"/"RIGHT")

    CellStyle(String fontFamily, int fontSize, boolean bold,
              String bgColor, String numberFormat, String align) {
        this.fontFamily = fontFamily; this.fontSize = fontSize; this.bold = bold;
        this.bgColor = bgColor; this.numberFormat = numberFormat; this.align = align;
    }
    // equals/hashCode over ALL intrinsic fields => used as the canonicalizing key.
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CellStyle c)) return false;
        return fontSize == c.fontSize && bold == c.bold
            && fontFamily.equals(c.fontFamily) && bgColor.equals(c.bgColor)
            && numberFormat.equals(c.numberFormat) && align.equals(c.align);
    }
    @Override public int hashCode() {
        return Objects.hash(fontFamily, fontSize, bold, bgColor, numberFormat, align);
    }
}

/* ============================================================
 * FLYWEIGHT FACTORY: canonicalizes styles; thread-safe & lazy.
 *   Uses the CellStyle itself as the key (value-based equals/hashCode).
 * ============================================================ */
final class CellStyleFactory {
    private final ConcurrentMap<CellStyle, CellStyle> pool = new ConcurrentHashMap<>();
    private final AtomicInteger created = new AtomicInteger();

    /** Return the canonical shared instance for this style (creating it once). */
    CellStyle intern(String font, int size, boolean bold, String bg, String fmt, String align) {
        CellStyle probe = new CellStyle(font, size, bold, bg, fmt, align);   // cheap, transient key
        // putIfAbsent-style canonicalization: first writer wins, everyone else shares it.
        CellStyle canonical = pool.putIfAbsent(probe, probe);
        if (canonical == null) { created.incrementAndGet(); return probe; }  // we created it
        return canonical;                                                    // reuse the shared one
    }
    int distinctStyles() { return pool.size(); }
    int stylesCreated()  { return created.get(); }
}

/* ============================================================
 * CONTEXT (extrinsic, tiny): a Cell = value + coordinate + shared style ref.
 * ============================================================ */
final class Cell {
    final int row, col;            // extrinsic
    final Object value;            // extrinsic
    final CellStyle style;         // SHARED flyweight reference (not a copy)
    Cell(int row, int col, Object value, CellStyle style) {
        this.row = row; this.col = col; this.value = value; this.style = style;
    }
}

/* ============================================================
 * DEMO: many threads populate a 1,000 x 1,000 sheet (1,000,000 cells)
 *       drawing from a small palette of styles — concurrently.
 * ============================================================ */
public class SpreadsheetDemo {
    public static void main(String[] args) throws InterruptedException {
        final CellStyleFactory styles = new CellStyleFactory();
        final Cell[][] grid = new Cell[1000][1000];

        // A realistic small palette: header, currency, percent, plain, highlighted...
        String[] fmts   = { "GENERAL", "#,##0.00", "0%", "yyyy-mm-dd" };
        String[] bgs    = { "WHITE", "YELLOW", "LTGRAY" };
        String[] aligns = { "LEFT", "CENTER", "RIGHT" };

        ExecutorService pool = Executors.newFixedThreadPool(8);   // 8 worker threads
        for (int r = 0; r < 1000; r++) {
            final int row = r;
            pool.submit(() -> {
                for (int col = 0; col < 1000; col++) {
                    boolean header = row == 0;
                    CellStyle s = styles.intern(                    // concurrent get-or-create
                        header ? "Arial" : "Calibri",
                        header ? 12 : 11,
                        header,                                     // header row is bold
                        header ? "LTGRAY" : bgs[col % bgs.length],
                        fmts[col % fmts.length],
                        aligns[col % aligns.length]);
                    grid[row][col] = new Cell(row, col, row * 1000 + col, s);  // share the style
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        System.out.println("Cells:            " + (1000 * 1000));        // 1,000,000
        System.out.println("Distinct styles:  " + styles.distinctStyles()); // ~ a few dozen
        System.out.println("Styles created:   " + styles.stylesCreated());  // == distinct (each made ONCE)
        // 1,000,000 Cell contexts share a few dozen CellStyle flyweights — created exactly once each,
        // even though 8 threads raced to intern them concurrently.
    }
}
```
</details>

**Line-by-line of what makes it production-grade:**

- **Intrinsic = the style tuple; extrinsic = value + (row, col).** The moment you notice "a million cells, but everyone uses one of ~40 looks," you have the split. Storing a full style object per cell would be hundreds of MB and enormous GC pressure; sharing collapses it to a few dozen `CellStyle` objects plus a cheap `Cell` per coordinate.
- **The flyweight is its own key.** `CellStyle` defines value-based `equals`/`hashCode` over *all* intrinsic fields, so two cells described identically map to the same canonical instance. This is the correct fix for the "broken key" pitfall — no fragile string concatenation, no accidental collisions.
- **`putIfAbsent` gives race-free canonicalization.** Eight threads populate the grid simultaneously; if two race to intern the same style, `putIfAbsent` ensures exactly one instance survives and both threads end up sharing it. The `stylesCreated == distinctStyles` assertion in the output *proves* each style object was constructed only once despite the concurrency — the guarantee interviewers want to see.
- **Concurrency is intrinsic, not staged.** Unlike the glyph demo, the threads here exist because the *workload* is naturally parallel (collaborative edits, parallel row loading, server-side export). That's why the thread-safe factory earns its keep rather than looking decorative.
- **The cons surface naturally.** If a user applies a unique style to every cell (huge cardinality), the pool grows toward one entry per cell and the savings vanish — which is exactly why Apache POI **caps a workbook at ~64,000 styles** and throws if you exceed it. That real-world guardrail is the "bound the cache / high-cardinality leak" pitfall made concrete.

**Pros:** genuinely concurrent workload (the thread-safe factory is justified, not contrived); value-based key via `equals`/`hashCode` is the idiomatic, collision-proof canonicalization; mirrors a real, well-known library (Apache POI `CellStyle`); the memory win is huge and intuitive.
**Cons:** requires a correct `equals`/`hashCode` over every intrinsic field (get it wrong and you silently duplicate or over-merge styles); still unbounded unless you cap cardinality (POI's 64k limit is exactly this concern); the transient `probe` object is allocated on every `intern` call (cheap and GC-friendly, but worth noting in a hot path).
**Mechanism:** *value-based canonicalization under concurrency* — `ConcurrentHashMap.putIfAbsent` keyed on an immutable, fully-`equals`-defined flyweight guarantees one canonical instance per distinct intrinsic value even when many threads race, and immutability makes those shared instances safe to read from every thread without locks.

---

## 🎨 Real-World Example

### Scenario 1 — Architected production-grade Flyweight using Uber's map interface (driver markers on the map)

<details>
<summary>📖 Click to expand</summary>

**Scenario:** an **Uber-style ride-hailing map that renders thousands of live driver markers**. Open the rider app in a dense city and the map paints every nearby driver as a small vehicle icon that rotates to face its heading and refreshes its position several times a second. Each visible marker needs an **icon bitmap** (a car / SUV / auto-rickshaw / bike sprite), a **vehicle-category style** (color, size class, "premium" badge), and often a **base rotation-sprite sheet** — all of which are *heavy* (decoded bitmaps, GPU textures) and *identical* across every driver of the same category. What differs per driver is only the **live telemetry**: latitude/longitude, bearing (heading), driver id, and ETA. That is a textbook intrinsic/extrinsic split, and at city scale (tens of thousands of drivers streamed to hundreds of thousands of rider devices) the difference between "one bitmap per driver" and "one bitmap per *category*" is the difference between an OOM-crashing app and a smooth 60fps map.

The intrinsic state is the **`VehicleMarkerType`** — the icon, category style, and pixel dimensions, shared by every driver of that class. The extrinsic state is the **`DriverMarker`** — a tiny per-driver record of `(lat, lng, bearing, driverId)` that is *updated in place* on every location ping and handed to the shared flyweight at draw time. A `MarkerTypeFactory` canonicalizes the flyweights so a fleet of 40,000 drivers across ~6 vehicle categories is backed by just ~6 heavy marker objects.

<details>
<summary>💻 Click to expand code — Uber-style driver-marker flyweight + demo</summary>

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/* ============================================================
 * FLYWEIGHT (intrinsic, heavy, IMMUTABLE, shared):
 *   the visual identity of a *vehicle category* on the map.
 *   One instance per (category, tier) — shared by every driver of that kind.
 * ============================================================ */
final class VehicleMarkerType {
    private final String category;      // intrinsic: "UberX", "UberXL", "Auto", "Moto", ...
    private final String iconSprite;    // intrinsic: pretend this is a decoded bitmap / GPU texture (HEAVY)
    private final int    widthPx;       // intrinsic: rendered size class
    private final int    heightPx;      // intrinsic
    private final String tintColor;     // intrinsic: category styling (e.g., black for "Premium")

    VehicleMarkerType(String category, String iconSprite, int widthPx, int heightPx, String tintColor) {
        this.category = category; this.iconSprite = iconSprite;
        this.widthPx = widthPx;   this.heightPx = heightPx; this.tintColor = tintColor;
        // In production this constructor would DECODE/UPLOAD the sprite once — the expensive part.
    }

    /* Extrinsic state (position, bearing, id) is passed IN — never stored on the shared object.
       The flyweight just knows HOW to draw itself given a driver's live telemetry. */
    void draw(MapCanvas canvas, double lat, double lng, float bearing, String driverId) {
        canvas.drawSprite(iconSprite, tintColor, widthPx, heightPx, lat, lng, bearing);
        // e.g., rotate the shared sprite by `bearing` and blit at the projected (lat,lng) pixel.
    }
    String category() { return category; }
}

/* ============================================================
 * FLYWEIGHT FACTORY: canonicalizes marker types, thread-safe & lazy.
 *   Location pings arrive on many I/O threads, so get-or-create must be atomic.
 * ============================================================ */
final class MarkerTypeFactory {
    private static final Map<String, VehicleMarkerType> POOL = new ConcurrentHashMap<>();
    private static final AtomicInteger spritesDecoded = new AtomicInteger();  // proves "created once"

    static VehicleMarkerType forCategory(String category) {
        return POOL.computeIfAbsent(category, cat -> {
            spritesDecoded.incrementAndGet();                 // heavy decode happens exactly once per category
            return switch (cat) {
                case "UberXL"  -> new VehicleMarkerType(cat, "suv.png",   48, 48, "#111111");
                case "Auto"    -> new VehicleMarkerType(cat, "auto.png",  40, 40, "#F6C324");
                case "Moto"    -> new VehicleMarkerType(cat, "moto.png",  32, 32, "#00A15C");
                default         -> new VehicleMarkerType(cat, "sedan.png", 44, 44, "#1A1A1A"); // UberX
            };
        });
    }
    static int distinctTypes()  { return POOL.size(); }
    static int spritesDecoded() { return spritesDecoded.get(); }
}

/* ============================================================
 * CONTEXT (extrinsic, tiny, MUTABLE per-instance):
 *   one per live driver. Updated on every GPS ping; holds only telemetry + a shared type ref.
 * ============================================================ */
final class DriverMarker {
    private final String driverId;               // extrinsic
    private volatile double lat, lng;            // extrinsic — updated by location stream
    private volatile float  bearing;             // extrinsic — vehicle heading in degrees
    private final VehicleMarkerType type;        // SHARED flyweight reference (NOT a copy)

    DriverMarker(String driverId, VehicleMarkerType type) { this.driverId = driverId; this.type = type; }

    /** Called several times per second from the location-update pipeline. */
    void onLocationPing(double lat, double lng, float bearing) {
        this.lat = lat; this.lng = lng; this.bearing = bearing;   // mutate only the cheap per-driver data
    }
    void render(MapCanvas canvas) { type.draw(canvas, lat, lng, bearing, driverId); }
}

/* ============================================================
 * The map view: holds every visible driver marker.
 * ============================================================ */
class DriverMap {
    private final Map<String, DriverMarker> markers = new ConcurrentHashMap<>();

    /** A driver enters the viewport — attach the shared flyweight, allocate only a tiny context. */
    void upsertDriver(String driverId, String category, double lat, double lng, float bearing) {
        markers.computeIfAbsent(driverId,
                id -> new DriverMarker(id, MarkerTypeFactory.forCategory(category)))
               .onLocationPing(lat, lng, bearing);
    }
    void driverExitedViewport(String driverId) { markers.remove(driverId); } // context GC'd; flyweight stays
    void renderFrame(MapCanvas canvas) { markers.values().forEach(m -> m.render(canvas)); }
    int visibleDrivers() { return markers.size(); }
}

class MapCanvas {
    void drawSprite(String sprite, String tint, int w, int h, double lat, double lng, float bearing) {
        /* project (lat,lng) -> screen pixels, rotate `sprite` by `bearing`, blit at size w×h */
    }
}

/* ============================================================
 * DEMO: 40,000 drivers across 4 categories streaming location updates.
 * ============================================================ */
public class UberMapDemo {
    public static void main(String[] args) {
        DriverMap map = new DriverMap();
        String[] categories = { "UberX", "UberXL", "Auto", "Moto" };
        Random rnd = new Random(7);

        // Simulate a burst of drivers entering the viewport (as in a dense city center).
        for (int i = 0; i < 40_000; i++) {
            String cat = categories[rnd.nextInt(categories.length)];
            map.upsertDriver("drv-" + i, cat,
                    12.90 + rnd.nextDouble() * 0.1,     // lat (≈ Bengaluru)
                    77.55 + rnd.nextDouble() * 0.1,     // lng
                    rnd.nextInt(360));                  // bearing
        }

        // Simulate a few animation frames with live location pings.
        for (int frame = 0; frame < 3; frame++) {
            for (int i = 0; i < 40_000; i++) {
                map.upsertDriver("drv-" + i, categories[i % 4],
                        12.90 + rnd.nextDouble() * 0.1, 77.55 + rnd.nextDouble() * 0.1, rnd.nextInt(360));
            }
            map.renderFrame(new MapCanvas());
        }

        System.out.println("Visible drivers:      " + map.visibleDrivers());        // 40,000
        System.out.println("Distinct marker types: " + MarkerTypeFactory.distinctTypes()); // 4
        System.out.println("Sprites decoded:       " + MarkerTypeFactory.spritesDecoded()); // 4 (once each!)
        // 40,000 live markers backed by 4 shared, heavy VehicleMarkerType flyweights.
        // Without Flyweight: 40,000 decoded bitmaps in memory -> hundreds of MB, GC churn, dropped frames.
    }
}
```
</details>

**Why this is the right design (and what an interviewer is listening for):**

The heavy, category-level visuals — the decoded bitmap, the tint, the size class — are **intrinsic** and live in the single shared `VehicleMarkerType`, so they are *decoded exactly once per category* (the demo proves it: `spritesDecoded == 4` even after 40,000 drivers and multiple frames). The volatile, per-driver telemetry — `lat`, `lng`, `bearing`, `driverId` — is **extrinsic**: it's the *only* thing that mutates on each location ping, and it's cheap (a few longs/floats). Crucially, the mutable state sits in the per-driver `DriverMarker` **context**, never on the shared flyweight — mutating the flyweight would corrupt every driver of that category and break the whole scheme. The `MarkerTypeFactory` uses `ConcurrentHashMap.computeIfAbsent` because Uber's location updates fan in on many I/O threads, so get-or-create must be atomic to avoid decoding the same sprite twice. When a driver leaves the viewport you drop the tiny context (`driverExitedViewport`) while the shared flyweight lives on for the next driver of that category — an implicit, natural cache. The payoff is exactly the Flyweight promise at production scale: memory scales with the **number of vehicle categories (≈ a handful)**, not the **number of live drivers (tens of thousands)**, which is what keeps the rider's map smooth instead of OOM-ing the device.

</details>

---

### Scenario 2 — Game rendering a forest of a million trees

<details>
<summary>📖 Click to expand</summary>

**Scenario:** a **game rendering a forest of a million trees** — a realistic, FAANG-appreciated Flyweight case because the memory arithmetic is dramatic and the intrinsic/extrinsic split is crisp. Every tree of a given species shares the same **mesh, texture, and material** (intrinsic, heavy — think megabytes of vertex/texture data), while each tree has its own **position, scale, and rotation** (extrinsic, a few bytes). Storing a full copy of the mesh per tree would need gigabytes; sharing collapses it to one `TreeType` per species plus a light context per tree.

<details>
<summary>💻 Click to expand code — game forest flyweight + demo</summary>

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/* =============== FLYWEIGHT: shared, immutable, heavy intrinsic state =============== */
final class TreeType {                     // one per species — SHARED
    private final String name;             // intrinsic
    private final String texture;          // intrinsic (pretend: a large loaded texture)
    private final String mesh;             // intrinsic (pretend: heavy vertex data)
    TreeType(String name, String texture, String mesh) {
        this.name = name; this.texture = texture; this.mesh = mesh;
    }
    // Extrinsic state (x, y, scale) is passed IN at draw time — never stored here.
    void draw(Canvas canvas, int x, int y, double scale) {
        canvas.render(name, texture, x, y, scale);
    }
}

/* =============== FACTORY: guarantees one TreeType per (name,texture,mesh) =============== */
final class TreeTypeFactory {
    private static final Map<String, TreeType> types = new ConcurrentHashMap<>();
    static TreeType get(String name, String texture, String mesh) {
        return types.computeIfAbsent(name + "|" + texture + "|" + mesh,
                                     k -> new TreeType(name, texture, mesh));
    }
    static int distinctTypes() { return types.size(); }
}

/* =============== CONTEXT: the lightweight per-instance object =============== */
final class Tree {                          // one per tree — CHEAP (holds only extrinsic + ref)
    private final int x, y; private final double scale;
    private final TreeType type;            // shared reference, not a copy
    Tree(int x, int y, double scale, TreeType type) {
        this.x = x; this.y = y; this.scale = scale; this.type = type;
    }
    void draw(Canvas canvas) { type.draw(canvas, x, y, scale); }  // hands extrinsic to flyweight
}

/* =============== The world holds a million trees =============== */
class Forest {
    private final List<Tree> trees = new ArrayList<>();
    void plant(int x, int y, double scale, String name, String texture, String mesh) {
        TreeType type = TreeTypeFactory.get(name, texture, mesh);  // shared flyweight
        trees.add(new Tree(x, y, scale, type));                    // cheap context
    }
    void draw(Canvas canvas) { for (Tree t : trees) t.draw(canvas); }
    int size() { return trees.size(); }
}

class Canvas { void render(String name, String tex, int x, int y, double s) { /* GPU draw */ } }

/* =============== DEMO =============== */
public class ForestDemo {
    public static void main(String[] args) {
        Forest forest = new Forest();
        Random rnd = new Random(42);
        String[] species = { "Oak", "Pine", "Birch" };            // only 3 distinct intrinsic states
        for (int i = 0; i < 1_000_000; i++) {                      // ONE MILLION trees
            String s = species[rnd.nextInt(species.length)];
            forest.plant(rnd.nextInt(10_000), rnd.nextInt(10_000),
                         0.5 + rnd.nextDouble(), s, s + ".png", s + ".mesh");
        }
        forest.draw(new Canvas());
        System.out.println("Trees planted:   " + forest.size());              // 1,000,000
        System.out.println("Distinct TreeTypes: " + TreeTypeFactory.distinctTypes()); // 3
        // 1,000,000 Tree contexts (a few bytes each) share just 3 heavy TreeType objects.
        // Without Flyweight: 1,000,000 copies of mesh+texture => gigabytes and OOM.
    }
}
```
</details>

The essence: **`TreeType` (intrinsic, heavy) is shared; `Tree` (extrinsic, light) is per-instance.** A million `Tree` objects hold nothing but a few ints, a double, and a *reference* to one of three shared `TreeType` flyweights. This is exactly how real game engines handle instanced rendering, and it's the mental model interviewers want: identify what's shared, share it once, pass the rest in.

</details>

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- The application uses a **very large number of objects** and their memory cost is a real bottleneck (measured, not assumed).
- A large fraction of each object's state is **duplicated identical data** that could be shared (the intrinsic part dominates the extrinsic part).
- The shareable state is **immutable or effectively constant** (fonts, sprites, meshes, config, lookup tables) so it's safe to share.
- The objects' **extrinsic state can be computed or passed in** cheaply rather than stored, so removing it from the object doesn't just move the cost elsewhere.
- The set of **distinct intrinsic values is small** relative to the number of objects (high sharing ratio) — e.g., millions of characters but hundreds of glyphs.

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- Object counts are **modest** — the memory savings don't justify the added indirection and factory complexity (premature optimization).
- **Little or no state is shared** — if every object's data is unique, there's nothing to canonicalize and Flyweight buys nothing.
- The shareable state is **mutable and context-dependent** — sharing a mutable object corrupts every context that references it (unless you can make it immutable).
- Extracting extrinsic state makes the client code **so complex or slow** (recomputing context on every call) that it costs more than the memory it saves.
- The lifetime/key space is **open-ended and unbounded**, turning the factory cache into a memory leak that outweighs the per-object savings (unless you add eviction).

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**

- **Dramatic memory reduction:** RAM scales with the number of *distinct intrinsic values*, not the number of logical objects.
- **Fewer allocations / less GC pressure:** shared flyweights are created once and reused, reducing churn.
- **Better cache locality** in some workloads: fewer, reused objects.
- **Centralized creation:** the factory is a single choke point for object management and instrumentation.

**Cons**

- **Complexity:** you split state, introduce a factory, and thread extrinsic state through method calls — more moving parts.
- **CPU-for-memory trade:** you may recompute or pass extrinsic state repeatedly instead of storing it; sometimes a net loss.
- **Immutability constraint:** flyweights must be immutable (or the sharing is unsafe) — this can be restrictive.
- **Cache management:** the pool can grow unbounded and leak memory; eviction/weak references add complexity.
- **Harder to reason about identity:** shared instances break naive `==`/identity assumptions (e.g., autoboxing surprises).

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

| Pattern | Core intent | How it differs from Flyweight |
|---|---|---|
| **Singleton** | Ensure exactly **one** instance of a class | Flyweight has **many** shared instances (one per distinct intrinsic value), managed by a factory keyed on that value; Singleton is a special case of "one per everything." |
| **Object Pool** | Reuse a set of **reusable, mutable** objects (connections, threads) by checking them out and back in | Pool objects are *mutable* and *exclusively borrowed* then returned/reset; flyweights are *immutable* and *concurrently shared* with no check-in. Pool manages lifecycle; Flyweight manages identity/sharing. |
| **Factory Method / Simple Factory** | Encapsulate object creation | Flyweight *uses* a factory as its enforcement mechanism, but adds caching/canonicalization; a plain factory doesn't guarantee sharing. |
| **Prototype** | Create objects by cloning a template | Prototype *copies* (more objects); Flyweight *shares* (fewer objects) — nearly opposite memory strategies. |
| **Memoization / Caching** | Store computed results for reuse | Structurally similar (a keyed cache), but memoization caches *results of computations*; Flyweight caches *shared value-objects* to reduce memory. |

**The classic confusions — Flyweight vs. Singleton vs. Object Pool:** Singleton = one instance total. Flyweight = one instance *per distinct intrinsic value*, shared and immutable. Object Pool = a bounded set of *mutable* instances lent out one-at-a-time and returned. If you're keying instances on their content and sharing immutably, it's Flyweight; if you're guaranteeing a single global, it's Singleton; if you're recycling mutable resources, it's a Pool.

</details>

## 📊 Comparison Table (of variants)

<details>
<summary>📖 Click to expand</summary>

| Axis | V0: Heavyweight | V1: Split, no factory | V2: Factory cache | V3: Production |
|---|---|---|---|---|
| Intrinsic/extrinsic split | none | yes | yes | yes |
| Sharing enforced? | no (no sharing) | no (accidental) | **yes** | **yes** |
| Memory scales with | object count | count (if undisciplined) | distinct values | **distinct values** |
| Thread-safe factory | n/a | n/a | no (`HashMap` race) | **yes (`computeIfAbsent`)** |
| Flyweight immutable? | n/a | not enforced | not enforced | **enforced (`final`)** |
| Lazy creation | no | no | yes | yes |
| Cache bounded? | n/a | n/a | no | no (needs eviction — see pitfalls) |
| Interview verdict | never | incomplete | good | **defend this** |

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

**Pitfall 1 — Mutable flyweight shared across contexts (state corruption).** If a flyweight stores mutable state (or you let extrinsic state creep in as a field), one client mutating it corrupts *every* client that shares it — the bugs are spooky and non-local.

<details>
<summary>💻 The failure</summary>

```java
class Glyph {
    char symbol; int x, y;                  // ❌ x,y are extrinsic but stored as fields
    void setPosition(int x,int y){ this.x=x; this.y=y; }
}
Glyph a = factory.getGlyph('a');
a.setPosition(10, 5);                       // client A sets position
// ...client B shares the SAME 'a' instance and overwrites it:
a.setPosition(99, 99);                      // now A's position is wrong too — shared mutation!
```
</details>

<details>
<summary>💻 The fix — immutable flyweight; extrinsic state passed in</summary>

```java
final class Glyph {
    private final char symbol;              // intrinsic, final
    Glyph(char symbol){ this.symbol = symbol; }
    void draw(int x, int y){ /* extrinsic passed in, not stored */ }
}
```
</details>

**Pitfall 2 — Storing extrinsic state inside the flyweight (defeats the purpose).** If per-instance data ends up as a flyweight field, each distinct context needs its own instance and sharing collapses — you're back to one object per logical instance.

<details>
<summary>💻 The failure</summary>

```java
// "Flyweight" keyed on symbol+position => a new instance per position => NO sharing.
factory.getGlyph('a', 10, 5);   // ❌ position is part of the key/state
factory.getGlyph('a', 11, 5);   // different instance — pool grows like object count
```
</details>

<details>
<summary>💻 The fix — key only on intrinsic state</summary>

```java
Glyph g = factory.getGlyph('a');   // key = 'a' only (intrinsic)
g.draw(10, 5);                     // position (extrinsic) supplied at call time
```
</details>

**Pitfall 3 — Unbounded factory cache = memory leak.** If the intrinsic key space is large or open-ended (e.g., keying on arbitrary user strings), the pool grows forever and the "memory saving" pattern becomes a memory *leak*.

<details>
<summary>💻 The fix — bound the cache (eviction) or use weak/soft references</summary>

```java
// Option A: size-bounded LRU (e.g., via a LinkedHashMap or a cache library like Caffeine).
// Option B: let unused flyweights be GC'd when no client references them:
Map<String, Glyph> pool = new java.util.WeakHashMap<>();  // keys GC-eligible when unreferenced
// (Better in production: Caffeine/Guava cache with maximumSize / expireAfterAccess.)
```
</details>

**Pitfall 4 — Broken `equals`/`hashCode` on the intrinsic key (silent duplicates).** If the factory key doesn't correctly and uniquely represent the intrinsic state (or a composite key type has no proper `equals`/`hashCode`), you get either duplicate flyweights (no sharing) or wrong sharing (collisions merging distinct values).

<details>
<summary>💻 The failure — using an array or mutable object as a key</summary>

```java
Map<char[], Glyph> pool = new HashMap<>();   // ❌ arrays use identity equals/hashCode
pool.get(new char[]{'a'});                    // never finds the entry put with a different array
```
</details>

<details>
<summary>💻 The fix — a value-based key (record / properly-defined key)</summary>

```java
record GlyphKey(char symbol, String font) {}          // record => correct equals/hashCode
Map<GlyphKey, Glyph> pool = new ConcurrentHashMap<>();
pool.computeIfAbsent(new GlyphKey('a', "Times12"), k -> new Glyph(k.symbol(), k.font()));
```
</details>

**Pitfall 5 — Premature Flyweight (optimizing without measuring).** Applying Flyweight when object counts are small adds complexity for negligible savings, and can even *hurt* (extra indirection, factory lookups, CPU to pass extrinsic state). Profile first; the pattern is a *memory* optimization for genuinely large populations.

</details>

## 🎓 Interview Tips

<details>
<summary>📖 What interviewers commonly ask</summary>

- "You have millions of objects and you're running out of memory — how do you fix it?" → identify shared vs. per-instance state and reach for Flyweight.
- "**What's the difference between intrinsic and extrinsic state?**" → the single most-asked Flyweight question; define both precisely and give an example (glyph outline vs. position).
- "Why does `Integer.valueOf(127) == Integer.valueOf(127)` return `true` but `128` returns `false`?" → the JVM's `Integer` cache is a Flyweight; answer nails autoboxing + the −128..127 cache.
- "How do you make the factory thread-safe?" → `ConcurrentHashMap.computeIfAbsent`; and why immutability makes the flyweights themselves safe.
- "How do you keep the cache from leaking?" → bounded/LRU or weak/soft references.

</details>

<details>
<summary>📖 What you should proactively mention</summary>

- Lead with the **intrinsic/extrinsic split** — it's the heart of the pattern; everything else follows.
- Stress that flyweights must be **immutable** and *why* (safe sharing across contexts and threads).
- Mention the **factory + cache** as the sharing-enforcement mechanism and that creation is **lazy**.
- Volunteer real JVM flyweights unprompted: **`Integer.valueOf` cache**, **String pool / `intern()`**, **`Boolean.valueOf`** — grounding in the platform is a strong signal.
- Bring up **cache eviction / weak references** and **measuring actual savings** — shows you think about the pattern's downsides, not just its upside.
- Note it's a **memory** optimization with a possible **CPU** cost (recomputing/passing extrinsic state) — a trade-off, not a free win.

</details>

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Factory Method / Simple Factory** — the mechanism Flyweight uses to enforce sharing and lazily create instances.
- **Singleton** — often the factory itself is a singleton; conceptually a flyweight is "one instance per key" vs. singleton's "one instance."
- **Composite** — flyweights are frequently the *leaf* nodes of a Composite (e.g., shared glyphs as leaves of a document tree).
- **State / Strategy** — these are often implemented as flyweights because they're typically stateless and shareable.
- **Object Pool** — the sibling for *mutable* reusable resources (contrast, not combine).
- **Memoization/Caching** — same keyed-cache machinery applied to computed results.

</details>

## 📚 Library/Framework Implementation

**1. `java.lang.Integer.valueOf` — the Integer cache.** The JDK caches boxed `Integer` objects for values −128 to 127 (the high end is tunable via `-XX:AutoBoxCacheMax`/`java.lang.Integer.IntegerCache.high`). `Integer.valueOf(int)` — which autoboxing calls — returns the shared cached instance in that range, so `Integer.valueOf(127) == Integer.valueOf(127)` is `true` (same object) but `128` is `false` (two new objects). This is a textbook Flyweight: the small-integer values are the shared intrinsic flyweights.

<details>
<summary>💻 Click to expand code — the Integer cache flyweight</summary>

```java
Integer a = Integer.valueOf(127), b = Integer.valueOf(127);
System.out.println(a == b);          // true  — same cached flyweight instance

Integer c = Integer.valueOf(128), d = Integer.valueOf(128);
System.out.println(c == d);          // false — outside cache, two distinct objects
System.out.println(c.equals(d));     // true  — value equality still holds

// Autoboxing uses valueOf under the hood, so the same surprise appears with:
Integer e = 127, f = 127;  System.out.println(e == f); // true
Integer g = 128, h = 128;  System.out.println(g == h); // false
// Lesson: always compare boxed types with .equals(), never ==.
```
</details>

**2. `String` pool / `String.intern()`.** String literals are automatically interned into a shared pool, so identical literals refer to the same object; `intern()` lets you canonicalize runtime-built strings into that pool. This is Flyweight applied to immutable strings — one shared instance per distinct string value.

<details>
<summary>💻 Click to expand code — String interning flyweight</summary>

```java
String a = "hello";                 // literal — pooled (interned) automatically
String b = "hello";
System.out.println(a == b);         // true  — same pooled flyweight

String c = new String("hello");     // explicitly a new object, NOT pooled
System.out.println(a == c);         // false
System.out.println(a == c.intern()); // true — intern() returns the shared pooled instance
// Interning millions of repeated tokens (e.g., XML tag names) can save large amounts of heap.
```
</details>

**3. `Boolean.valueOf` / `Character` cache.** `Boolean.valueOf(true/false)` returns the shared `Boolean.TRUE`/`Boolean.FALSE` constants — only two boolean flyweights ever exist. `Character` similarly caches values 0–127. Both are Flyweights for their tiny, fixed intrinsic value sets.

<details>
<summary>💻 Click to expand code — Boolean/Character flyweights</summary>

```java
System.out.println(Boolean.valueOf(true) == Boolean.valueOf(true)); // true — shared TRUE
System.out.println(Boolean.valueOf(true) == Boolean.TRUE);          // true

Character x = Character.valueOf('A'), y = Character.valueOf('A');
System.out.println(x == y);                                          // true — 'A' (65) is cached
Character big = Character.valueOf((char) 200), big2 = Character.valueOf((char) 200);
System.out.println(big == big2);                                     // false — outside 0..127 cache
```
</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1: [Conceptual] What is the Flyweight pattern and what problem does it solve?</strong></summary>

Flyweight is a structural GoF pattern that **uses sharing to support large numbers of fine-grained objects efficiently.** It solves a *memory* problem: when you need millions of small objects that are mostly identical, storing each fully duplicates enormous amounts of common data and exhausts the heap. Flyweight partitions each object's state into **intrinsic** (context-independent, shared) and **extrinsic** (context-dependent, supplied by the client) parts. It stores only intrinsic state inside a small pool of shared, immutable flyweight objects — one per distinct intrinsic value — and passes extrinsic state into methods instead of storing it. A factory guarantees the sharing. The result: a million logical objects backed by a few dozen real ones, so memory scales with the number of *distinct values*, not the total count.

</details>

<details>
<summary><strong>Q2: [Conceptual] Define intrinsic vs. extrinsic state with a concrete example.</strong></summary>

**Intrinsic state** is context-independent, immutable, and shareable — it's the same no matter where or how the object is used, so it can be stored once inside the shared flyweight. **Extrinsic state** is context-dependent and varies per logical instance — it can't be shared, so the client stores or computes it and passes it into flyweight methods. Example (text editor): for a character, the **intrinsic** state is the glyph outline/bitmap, font family, and style (the 'a' in Times 12pt bold looks identical everywhere it appears); the **extrinsic** state is its (row, column) position in the document (different for every occurrence). You share one flyweight for "'a' in Times 12pt bold" across all its million appearances, and pass each appearance's position to `draw(x, y)`.

</details>

<details>
<summary><strong>Q3: [Conceptual] Why must a flyweight be immutable, and what breaks if it isn't?</strong></summary>

A flyweight is shared by many unrelated clients (and often many threads) simultaneously. If it held mutable state, one client mutating the shared instance would silently change what every other client sees — producing non-local, hard-to-reproduce corruption. Immutability makes sharing *safe*: since the intrinsic state never changes after construction, no client can interfere with another, and the object is inherently thread-safe to read without any synchronization. Immutability also gives the JMM guarantee that a properly-constructed `final`-field object is safely visible to any thread that obtains a reference. If you can't make the shared part immutable, you generally can't safely apply Flyweight — the pattern's correctness rests on the shared core being read-only.

</details>

<details>
<summary><strong>Q4: [Conceptual] What are the participants of the Flyweight pattern?</strong></summary>

Five participants. **Flyweight** — the interface declaring operations that accept *extrinsic* state as parameters (`draw(x, y)`). **ConcreteFlyweight** — a shared, immutable object storing only *intrinsic* state. **UnsharedConcreteFlyweight** — (optional) a flyweight-interface implementer that isn't shared, for objects that genuinely can't be shared (often composites of flyweights). **FlyweightFactory** — owns the pool/cache and returns a shared flyweight per intrinsic key, creating it lazily on first request; it's the only sanctioned way to obtain flyweights, which is what enforces sharing. **Client** — maintains references to flyweights (via the factory) and computes/stores the extrinsic state it passes into flyweight methods.

</details>

<details>
<summary><strong>Q5: [Implementation] Implement a basic FlyweightFactory for character glyphs.</strong></summary>

Key the pool on the intrinsic state (symbol + font); return the cached instance or create-and-cache lazily.

<details>
<summary>💻 Solution</summary>

```java
final class Glyph {                       // flyweight — intrinsic only, immutable
    private final char symbol; private final String font;
    Glyph(char symbol, String font) { this.symbol = symbol; this.font = font; }
    void draw(int x, int y) { /* render at extrinsic position */ }
}
class GlyphFactory {
    private final Map<String, Glyph> pool = new HashMap<>();
    Glyph get(char symbol, String font) {
        return pool.computeIfAbsent(symbol + "|" + font, k -> new Glyph(symbol, font));
    }
}
// Client: factory.get('a', "Times12").draw(10, 5);  // shared glyph, extrinsic position passed in
```
</details>

Every request for the same (symbol, font) returns the identical shared instance; the pool size equals the number of distinct glyphs, not the number of characters.

</details>

<details>
<summary><strong>Q6: [Implementation] Make the factory thread-safe. Show the correct approach and the subtle race in the naive one.</strong></summary>

A naive `HashMap` factory has a check-then-act race: two threads both see "absent," both create, and one instance is lost — worse, `HashMap` itself can corrupt under concurrent writes. The fix is `ConcurrentHashMap.computeIfAbsent`, which performs the get-or-create atomically per key.

<details>
<summary>💻 Solution</summary>

```java
// ❌ Racy: two threads can both create a Glyph for the same key.
Glyph get(String key) {
    Glyph g = map.get(key);
    if (g == null) { g = new Glyph(...); map.put(key, g); }  // check-then-act race
    return g;
}

// ✅ Thread-safe & atomic:
private final Map<String, Glyph> pool = new ConcurrentHashMap<>();
Glyph get(char c, String font) {
    return pool.computeIfAbsent(c + "|" + font, k -> new Glyph(c, font));
}
```
</details>

Caveat: `computeIfAbsent`'s mapping function runs while holding a bin lock, so it must be fast and must not update the same map (risking deadlock/`ConcurrentModificationException`). For very expensive creation, a `putIfAbsent` with a benign occasional double-create, or a memoizing `Supplier`, may be preferable.

</details>

<details>
<summary><strong>Q7: [Implementation] Model a chessboard/board game with Flyweight — what's intrinsic vs. extrinsic?</strong></summary>

The piece *type* (shape, movement rules, color) is intrinsic and shared (all white pawns behave identically); the *position* on the board is extrinsic.

<details>
<summary>💻 Solution</summary>

```java
enum Color { WHITE, BLACK }

final class PieceType {                    // flyweight: shared, immutable
    final String name; final Color color; final String moveRules;
    PieceType(String name, Color color, String moveRules) { this.name=name; this.color=color; this.moveRules=moveRules; }
    void render(int row, int col) { System.out.println(color + " " + name + " @ (" + row + "," + col + ")"); }
}
class PieceFactory {
    private final Map<String, PieceType> pool = new ConcurrentHashMap<>();
    PieceType get(String name, Color color) {
        return pool.computeIfAbsent(name + color, k -> new PieceType(name, color, rulesFor(name)));
    }
    private String rulesFor(String n){ return n + "-rules"; }
}
// 32 pieces on the board, but only ~12 distinct PieceType flyweights (6 kinds × 2 colors).
// A Piece "placement" stores just (type, row, col) — extrinsic position + shared type ref.
```
</details>

At larger scale (e.g., simulating thousands of boards) the sharing ratio grows and the memory win becomes significant.

</details>

<details>
<summary><strong>Q8: [Implementation] How do you bound the factory cache so it doesn't leak? Show two approaches.</strong></summary>

If the intrinsic key space is open-ended, an unbounded pool leaks. Two standard fixes: size-bounded eviction (LRU) or reference-based eviction (weak/soft references so unused flyweights are GC-eligible).

<details>
<summary>💻 Solution</summary>

```java
// Approach A — LRU bound via LinkedHashMap (single-threaded / externally synchronized):
Map<String, Glyph> lru = new LinkedHashMap<>(256, 0.75f, true) {
    protected boolean removeEldestEntry(Map.Entry<String, Glyph> e) { return size() > 1000; }
};

// Approach B — let GC reclaim unreferenced flyweights:
Map<String, Glyph> weak = new WeakHashMap<>();   // entries vanish when keys unreferenced

// Production: a cache library (Caffeine/Guava) — maximumSize + expireAfterAccess + weak values:
// Cache<String,Glyph> cache = Caffeine.newBuilder().maximumSize(10_000).build();
```
</details>

The right choice depends on whether flyweights are cheap to recreate (favor eviction) and whether the working set is bounded (LRU) or driven by live references (weak/soft).

</details>

<details>
<summary><strong>Q9: [Breaking] Explain why <code>Integer.valueOf(127) == Integer.valueOf(127)</code> is true but 128 is false.</strong></summary>

The JVM maintains a Flyweight cache of boxed `Integer` objects for values −128 through 127 (the default range; the upper bound is configurable via `-XX:AutoBoxCacheMax` or the `java.lang.Integer.IntegerCache.high` property). `Integer.valueOf(int)` — which autoboxing invokes — returns the *shared cached instance* for values in that range, so both `valueOf(127)` calls return the identical object and `==` (reference equality) is `true`. For 128, which is outside the cache, each `valueOf` allocates a *new* `Integer`, so `==` compares two distinct references and is `false`. Both cases are `equals()`-`true` because that compares values. The lesson: never compare boxed numeric types with `==`; use `.equals()` or unbox deliberately. This is the JDK's most famous Flyweight, and it's a beloved "gotcha" interview question.

</details>

<details>
<summary><strong>Q10: [Breaking] What happens if extrinsic state accidentally becomes part of the flyweight, and how do you detect it?</strong></summary>

Sharing collapses. If per-instance data (like position) is stored in the flyweight or included in the factory key, every distinct context requires its own instance, so the pool grows proportionally to the object count — you get all the complexity of Flyweight with none of the memory savings, and possibly worse performance from the extra indirection. You detect it by monitoring the **pool size vs. the number of logical objects**: if the ratio is near 1:1 (pool grows as fast as instances), extrinsic state has leaked into the key/state. The fix is to key the factory *only* on truly intrinsic, immutable, context-free attributes and pass everything else as method parameters. A good design review question is "what's in the key, and is every field of it genuinely context-independent?"

</details>

<details>
<summary><strong>Q11: [Breaking] Why can interning huge numbers of strings with <code>String.intern()</code> backfire?</strong></summary>

`intern()` canonicalizes strings into the JVM's shared string pool — a Flyweight — which saves heap when you have many *duplicate* strings. But it backfires in two ways. First, if the strings are mostly *unique* (e.g., interning user IDs or UUIDs), the pool bloats with one entry per distinct value and you consume *more* memory plus lookup overhead, with little sharing benefit. Second, historically the pool had a fixed hashtable size and lived in PermGen, so mass interning caused slow lookups or `OutOfMemoryError`; modern JVMs moved it to the heap and made the table size tunable (`-XX:StringTableSize`), but interned strings still can't be individually GC'd as easily and the table can become a bottleneck. Rule: intern only when you have a *bounded, high-duplication* set; otherwise use an application-level bounded cache.

</details>

<details>
<summary><strong>Q12: [Breaking] A team applied Flyweight and memory got worse, not better. What are the likely causes?</strong></summary>

Several: (1) **low sharing ratio** — the objects weren't actually duplicative, so the pool has nearly as many entries as there were objects, and you added factory/cache overhead on top; (2) **extrinsic state leaked into the key** (see Q10), preventing sharing; (3) the **cache is unbounded and never evicts**, so it retains flyweights long after they're needed, holding memory the old design would have released; (4) the **extrinsic-state context objects** the client now holds are themselves not lightweight, so you moved the cost rather than removing it; (5) **premature application** — object counts were small and the machinery's fixed overhead exceeds the tiny savings. The meta-lesson: Flyweight is a *measured* optimization; without profiling before and after, you can easily make things worse.

</details>

<details>
<summary><strong>Q13: [Trade-off] Flyweight vs. Object Pool — how are they different?</strong></summary>

Both reuse objects, but for opposite reasons and with opposite constraints. **Flyweight** shares **immutable** objects **concurrently** among many clients to save *memory* — there's no "return"; a flyweight is read-only and used by everyone at once, keyed on its intrinsic content. **Object Pool** recycles **mutable** objects (DB connections, threads, byte buffers) that are *expensive to create*; a client **borrows** one *exclusively*, mutates it, and **returns/resets** it — the goal is to save *creation cost / manage a scarce resource*, not memory-through-sharing, and the object is never used by two clients simultaneously. So: shared vs. exclusive, immutable vs. mutable, keyed-by-value vs. interchangeable, memory-saving vs. lifecycle-managing. Confusing them leads to disasters (sharing a mutable pooled connection, or "returning" a flyweight).

</details>

<details>
<summary><strong>Q14: [Trade-off] Flyweight vs. Singleton — clarify the relationship.</strong></summary>

A Singleton guarantees **exactly one** instance of a class for the whole application. A Flyweight maintains **one instance per distinct intrinsic value** — many shared instances, each canonical for its value, dispensed by a factory keyed on that value. You can view Singleton as the degenerate case of Flyweight where there's a single key (or no key). In practice the Flyweight *factory* is often itself a Singleton (one pool for the app), but the flyweights it hands out are many. The distinguishing question: are you sharing *the one and only* object (Singleton), or *the canonical object for a given value* among potentially thousands of such canonical objects (Flyweight)?

</details>

<details>
<summary><strong>Q15: [Trade-off] When is Flyweight the wrong choice, and what would you use instead?</strong></summary>

Flyweight is wrong when the preconditions don't hold. If object counts are **small**, skip it — a plain object is simpler and the savings are negligible (YAGNI). If **little state is shared** (objects are mostly unique), there's nothing to canonicalize — Flyweight adds overhead for no benefit. If the shareable state is **mutable and must vary per context**, you can't safely share it — use ordinary objects, or Prototype if you need many similar-but-independent copies. If the goal is reusing **expensive mutable resources**, use an **Object Pool**. If you're caching *computed results* rather than value-objects, that's **memoization**. And if the real problem is representation, sometimes a more compact data layout (primitive arrays / columnar/struct-of-arrays) beats an object-per-item scheme entirely, regardless of sharing.

</details>

<details>
<summary><strong>Q16: [Advanced] How does Flyweight interact with the JVM's garbage collector and long-lived caches?</strong></summary>

The factory's pool holds **strong references** to flyweights, which keeps them alive for the pool's lifetime — great for hot, reused flyweights, but a leak if the key space is unbounded or flyweights outlive their usefulness (they get promoted to old gen and are never collected, increasing GC pause pressure over time). To let the GC reclaim unused flyweights, use **weak references** (`WeakHashMap` keys, or weak/soft *values* via a cache library): weakly-referenced flyweights become collectible once no client strongly references them, and softly-referenced ones are kept until memory pressure. There's a subtlety: interned/canonicalized objects that live forever (like the String pool) trade GC-collectibility for guaranteed sharing. The engineering judgment is matching reference strength to the flyweight's cost and lifetime: strong for cheap-to-recreate hot flyweights with a bounded key set; weak/soft (plus size bounds) for large or open-ended key spaces.

</details>

<details>
<summary><strong>Q17: [Advanced] How would you make Flyweight work in a distributed cache across many JVMs?</strong></summary>

In-process Flyweight relies on object *identity* (`==` sharing) within one heap, which doesn't extend across JVMs — each node has its own object graph. To get the memory benefit cluster-wide you shift from *identity sharing* to *value canonicalization plus a shared store*: (1) a **distributed cache** (Redis, Memcached, Hazelcast) holds the canonical intrinsic values keyed by a stable, serializable key; each JVM caches locally (near-cache) and falls back to the shared store on miss, so hot flyweights live once per node rather than once per logical object. (2) You must define a **stable serialization + key scheme** for intrinsic state so all nodes agree on identity by value. (3) Handle **consistency/immutability** — flyweights must remain immutable so stale reads are impossible; if intrinsic data can change, you need versioning/invalidation. (4) Weigh **network cost**: a remote fetch to save memory only pays off if the intrinsic payload is large and reused heavily. The pattern's *spirit* (share the heavy, immutable, common part; pass context separately) carries over, but the *mechanism* becomes a caching tier, not object references.

</details>

<details>
<summary><strong>Q18: [Advanced/Coding Challenge] Implement a memory-efficient particle system for a game with 1,000,000 particles using Flyweight. Full solution + savings analysis.</strong></summary>

Particles share intrinsic appearance (sprite, color, size class) via flyweights; each particle stores only extrinsic physics state.

<details>
<summary>💻 Full solution</summary>

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// FLYWEIGHT — heavy, shared, immutable appearance.
final class ParticleType {
    private final String sprite;   // pretend: a loaded texture (heavy)
    private final String color;
    private final int    sizeClass;
    ParticleType(String sprite, String color, int sizeClass) {
        this.sprite = sprite; this.color = color; this.sizeClass = sizeClass;
    }
    // Extrinsic physics passed in — never stored.
    void render(double x, double y, double vx, double vy) {
        // draw sprite at (x,y) moving (vx,vy)
    }
}

// FACTORY — canonicalizes appearance; thread-safe.
final class ParticleTypeFactory {
    private static final Map<String, ParticleType> pool = new ConcurrentHashMap<>();
    static ParticleType get(String sprite, String color, int sizeClass) {
        return pool.computeIfAbsent(sprite + "|" + color + "|" + sizeClass,
                                    k -> new ParticleType(sprite, color, sizeClass));
    }
    static int distinct() { return pool.size(); }
}

// CONTEXT — one per particle; holds ONLY extrinsic state + a shared type reference.
final class Particle {
    double x, y, vx, vy;                 // extrinsic (mutable physics is fine — it's per-instance)
    final ParticleType type;             // shared flyweight reference
    Particle(double x,double y,double vx,double vy, ParticleType type) {
        this.x=x; this.y=y; this.vx=vx; this.vy=vy; this.type=type;
    }
    void update(double dt){ x += vx*dt; y += vy*dt; }
    void render(){ type.render(x, y, vx, vy); }
}

public class ParticleSystemDemo {
    public static void main(String[] args) {
        List<Particle> particles = new ArrayList<>(1_000_000);
        Random r = new Random(1);
        String[] sprites = {"spark", "smoke", "fire"};
        for (int i = 0; i < 1_000_000; i++) {
            String s = sprites[r.nextInt(3)];
            ParticleType t = ParticleTypeFactory.get(s, "orange", 1);  // shared
            particles.add(new Particle(r.nextInt(1920), r.nextInt(1080), r.nextGaussian(), r.nextGaussian(), t));
        }
        System.out.println("Particles: " + particles.size());            // 1,000,000
        System.out.println("Distinct types: " + ParticleTypeFactory.distinct()); // 3
        /* Savings: without Flyweight each particle stores its own sprite/texture — say ~1 KB each
           => ~1 GB. With Flyweight: 3 ParticleType objects hold the heavy data once (~3 KB total),
           and 1,000,000 Particle contexts hold ~40 bytes each (~40 MB). ~25x smaller. */
    }
}
```
</details>

Note the particle's physics state is mutable, but that's fine — it's *extrinsic* (per-instance), living in `Particle`, not in the shared `ParticleType`.

</details>

<details>
<summary><strong>Q19: [Advanced/Coding Challenge] Build a thread-safe, size-bounded Flyweight factory (LRU eviction) and explain the concurrency reasoning. Full solution.</strong></summary>

Combine canonicalization with a bounded LRU so an open-ended key space can't leak, while staying safe under concurrency.

<details>
<summary>💻 Full solution</summary>

```java
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

final class Color {                                  // flyweight — immutable
    final int rgb; Color(int rgb) { this.rgb = rgb; }
}

final class BoundedColorFactory {
    private final int maxSize;
    private final ReentrantLock lock = new ReentrantLock();
    // access-order LinkedHashMap = LRU; guarded by an explicit lock for atomic get-or-create + evict.
    private final LinkedHashMap<Integer, Color> pool;

    BoundedColorFactory(int maxSize) {
        this.maxSize = maxSize;
        this.pool = new LinkedHashMap<>(16, 0.75f, true) {   // true => access order (LRU)
            protected boolean removeEldestEntry(Map.Entry<Integer, Color> e) { return size() > maxSize; }
        };
    }

    Color get(int rgb) {
        lock.lock();                                  // LinkedHashMap is NOT thread-safe; guard it
        try {
            Color c = pool.get(rgb);                  // get() mutates access order => must be inside lock
            if (c == null) { c = new Color(rgb); pool.put(rgb, c); } // put may evict eldest
            return c;
        } finally { lock.unlock(); }
    }
    int size() { lock.lock(); try { return pool.size(); } finally { lock.unlock(); } }
}
```
</details>

Concurrency reasoning: `LinkedHashMap` in access-order mode mutates its internal ordering on *reads* (`get`), so even lookups aren't safe concurrently — hence a single `ReentrantLock` around the whole get-or-create-and-maybe-evict sequence, making it atomic. A `ConcurrentHashMap` alone can't do LRU (no ordering/eviction), so for bounded + concurrent + LRU you either lock a `LinkedHashMap` (simple, shown here) or use a purpose-built concurrent cache like Caffeine (segmented, near-lock-free) in production. The flyweight `Color` being immutable means returned instances are safe to share once handed out.

</details>

<details>
<summary><strong>Q20: [Advanced] How do Flyweight, immutability, and future JVM value types (Project Valhalla) relate?</strong></summary>

Flyweight exists partly because in current Java every object has *identity* and *header overhead* (a mark word + class pointer, ~12–16 bytes) plus indirection, so a million tiny objects cost far more than their raw data — sharing amortizes that. Flyweight's requirement that shared state be immutable aligns exactly with **value semantics**: a flyweight is conceptually a value (equal-by-content, no meaningful identity). **Project Valhalla's value classes / primitive types** aim to let the JVM store such identity-free, immutable aggregates *inline* (flattened into arrays/containers with no header and no pointer chasing), which addresses the *same* memory problem Flyweight attacks — but structurally rather than by sharing. Where Flyweight *shares one instance* among many references, value types *flatten* the data so there's no per-instance header to save in the first place. They're complementary: value types reduce the per-object tax; Flyweight eliminates duplication of *identical* heavy data. In a Valhalla world, some Flyweight use-cases (many small immutable value-like objects) may be better served by value types, while genuinely heavy shared payloads (a megabyte texture referenced by a million sprites) still call for sharing.

</details>

### 🧠 Staff / Principal Engineer Level

<details>
<summary><strong>SP1: [Staff] How do you decide, rigorously, whether Flyweight will actually pay off before implementing it?</strong></summary>

Quantify before committing. Estimate (1) the **object count** (N logical objects), (2) the number of **distinct intrinsic values** (D), and (3) the **per-object intrinsic size** (S_i) vs **extrinsic size** (S_e). Naive memory ≈ N × (S_i + S_e + header); Flyweight memory ≈ D × (S_i + header) + N × (S_e + reference + header). Flyweight wins when N ≫ D and S_i is large relative to S_e — i.e., a **high sharing ratio** and **heavy shared payload**. Compute the ratio; if N/D is near 1 or S_i is tiny, don't bother. Then **profile a prototype** with a real heap dump (jmap/MAT) comparing before/after — estimates miss GC promotion effects, retained-set surprises, and the cost of the context objects. Also factor the **CPU cost** of passing/recomputing extrinsic state in hot paths. The staff-level discipline is treating it as a measured trade-off with an explicit model, not a reflex.

</details>

<details>
<summary><strong>SP2: [Staff] Design the eviction and reference strategy for a Flyweight cache serving an unbounded, long-running service.</strong></summary>

Match reference strength and eviction to flyweight cost, key-space size, and access pattern. If flyweights are **cheap to recreate** and the working set is bounded, use a **size-bounded LRU** (Caffeine `maximumSize`) — simple, predictable memory. If flyweights are **expensive** but usage is **bursty**, add **`expireAfterAccess`** so cold entries drain without thrashing hot ones. For flyweights whose lifetime should track live client references, use **weak values** so they're collected once unreferenced (avoids leaks when the key space is open-ended), or **soft values** to keep them until memory pressure. Instrument **hit rate, size, and eviction counts** as first-class metrics — a collapsing hit rate signals the working set exceeds the bound (raise it or the pattern isn't helping). Guard against **cache stampede** on hot keys with atomic `computeIfAbsent`/loading caches so concurrent misses don't create duplicates. The principal-level point: the cache *is* the design — get its bounds, references, and observability right, or the "optimization" becomes the incident.

</details>

<details>
<summary><strong>SP3: [Principal] Contrast the Flyweight (object-sharing) approach with data-oriented design (struct-of-arrays) for extreme-scale object populations.</strong></summary>

Flyweight keeps an *object-per-item* model and de-duplicates the shared part; it preserves OO ergonomics (you still have `Particle` objects) while cutting duplication. **Data-oriented design (DOD)** rejects the object-per-item model entirely: it stores fields in **parallel primitive arrays** (struct-of-arrays) — `double[] x, y, vx, vy; int[] typeId` — so a million particles are a handful of contiguous arrays with *zero* per-object header overhead and excellent cache locality and vectorization. DOD typically crushes Flyweight on both memory (no headers, no references) and throughput (sequential memory access, SIMD-friendly), which is why game engines and columnar databases use it. Flyweight still wins when the *shared* payload is genuinely heavy and irregular (a texture, a rules engine) that you don't want to flatten, or when you must retain rich object behavior/polymorphism. The principal-level judgment: for hot, homogeneous, numeric-heavy populations, reach for DOD/columnar layouts; use Flyweight when you need to keep objects but eliminate duplication of large immutable shared state — and know that the two can combine (a type-id column indexing a small flyweight table).

</details>

<details>
<summary><strong>SP4: [Principal] The intrinsic data behind flyweights can occasionally change (e.g., a shared config/pricing table updates). How do you handle mutation safely?</strong></summary>

Never mutate a live flyweight in place — that's the cardinal sin, since it changes what every sharer sees mid-flight and breaks the immutability contract that makes sharing safe. Instead use **copy-on-write / versioned replacement**: build a *new* immutable flyweight for the changed value and atomically swap the factory's mapping (`ConcurrentHashMap.put`/`AtomicReference`) so new lookups get the new version while in-flight users of the old instance finish safely against a consistent snapshot. If clients cache flyweight references long-term, add a **version/generation stamp** so they can detect staleness and re-fetch, or route all access through the factory each time (trading a lookup for freshness). For coordinated updates across many flyweights, publish a whole **new immutable snapshot** of the table behind one atomic reference swap (like a persistent data structure), giving readers a consistent point-in-time view without locks. The principle: mutation becomes *replacement of immutable versions*, preserving safe sharing while allowing change — the same discipline behind MVCC and RCU.

</details>

<details>
<summary><strong>SP5: [Principal] How would you retrofit Flyweight into a large legacy codebase suffering OOM from millions of duplicated objects, without a risky big-bang change?</strong></summary>

Incrementally and behind measurement. Step 1: **confirm the diagnosis** with a heap dump — verify that the OOM is duplicated *intrinsic* data (many identical heavy objects), not just too many genuinely distinct objects (which Flyweight can't fix). Step 2: **introduce a factory** as the single creation choke point, initially returning `new` instances (behavior-preserving) so you can route all construction through it without changing semantics. Step 3: **canonicalize behind the factory** — add the pool and start returning shared instances, guarded by a feature flag so you can roll back instantly; this is safe *only if the object is already effectively immutable*, so Step 3a is often "make the shared type immutable" (remove setters, `final` fields), which itself may need call-site fixes. Step 4: **move extrinsic state out** where objects were mutated per-context — the trickiest step, done type-by-type with characterization tests comparing old vs new behavior. Step 5: **add cache bounds/observability** and validate memory with before/after heap dumps and production canaries. Throughout, keep each step independently shippable and reversible, and let heap-dump evidence — not intuition — drive which types to convert first (target the ones dominating retained heap). The principal-level themes: measure to target the real culprit, make immutability the enabling refactor, guard with flags and characterization tests, and never convert everything at once.

</details>

---

## ⚡ Quick Revision

**One-liner:** Flyweight shares a small pool of immutable objects (one per distinct *intrinsic* value) among huge numbers of logical objects, passing the per-instance *extrinsic* state in at call time — so memory scales with distinct values, not object count.

**The whole pattern in one paragraph:** Flyweight is a structural GoF pattern — *"use sharing to support large numbers of fine-grained objects efficiently."* It attacks a **memory** problem: millions of tiny objects that mostly duplicate identical heavy data (fonts, textures, meshes, config). The core move is **partitioning state** into **intrinsic** (context-independent, immutable, shareable — the character's glyph, the tree's mesh) and **extrinsic** (context-dependent, per-instance — the character's position, the tree's coordinate). Intrinsic state lives in a pool of **shared, immutable ConcreteFlyweight** objects (one per distinct value); extrinsic state is **passed into methods** by the client rather than stored. A **FlyweightFactory** owns a cache (a `Map` keyed by intrinsic state) and is the *only* way to obtain flyweights — it **canonicalizes**, returning the existing instance or lazily creating one, which *enforces* sharing. **Immutability** is what makes shared instances safe across contexts and threads (no synchronization needed to read); in production the factory uses **`ConcurrentHashMap.computeIfAbsent`** for atomic, race-free get-or-create. The result: a million logical objects backed by a few dozen real ones. Watch the failure modes: a **mutable flyweight** corrupts all sharers; **extrinsic state leaking into the key/state** collapses sharing back to one-object-per-instance; an **unbounded cache leaks** (fix with LRU/`WeakHashMap`/Caffeine); a **broken `equals`/`hashCode`** on the key silently duplicates; and **premature use** at small scale just adds overhead. It's a **memory-for-CPU** trade (you may recompute/pass extrinsic state). Distinguish it from **Singleton** (one instance total vs. one per value), **Object Pool** (mutable, borrowed-and-returned, exclusive vs. immutable, concurrently shared), and **Prototype** (copies vs. shares). Canonical JDK flyweights: **`Integer.valueOf` cache (−128..127)** — the famous `==` autoboxing gotcha — the **String pool / `intern()`**, and **`Boolean.valueOf`**.

**Top 5 interview answers to memorize:**

1. **"What is it?"** → Share a small pool of immutable objects (one per distinct intrinsic value) among huge numbers of instances; store only intrinsic state, pass extrinsic state in at call time. Memory scales with distinct values, not count.
2. **"Intrinsic vs. extrinsic?"** → Intrinsic = context-independent, immutable, shared (glyph/font); extrinsic = context-dependent, per-instance, passed in (position). You share the intrinsic, supply the extrinsic.
3. **"Why `Integer.valueOf(127)==127` true, 128 false?"** → JVM caches boxed Integers −128..127 (a Flyweight); `valueOf` returns the shared instance in range (same reference), allocates new outside it. Always compare boxed types with `.equals()`.
4. **"How is the factory thread-safe?"** → `ConcurrentHashMap.computeIfAbsent` for atomic get-or-create; and flyweights are immutable, so sharing them across threads needs no synchronization.
5. **"How does it break?"** → Mutable flyweight → shared-state corruption; extrinsic state in the key → sharing collapses; unbounded cache → memory leak (use LRU/weak refs); premature use at small scale → pure overhead.

**Trigger words (hear these → think Flyweight):** "millions of objects", "running out of memory / OOM", "too many small objects", "lots of duplicate/identical data", "share objects", "intrinsic and extrinsic", "text editor / characters / glyphs", "game with thousands of trees/particles/bullets", "map with many markers/pins", "cache/pool of shared instances", "reduce memory footprint", "one instance per distinct value", "Integer cache / string interning", "fine-grained objects".

---

*End of Flyweight Pattern study guide.*




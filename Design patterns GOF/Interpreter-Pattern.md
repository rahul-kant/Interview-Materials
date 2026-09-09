# Interpreter Pattern ⭐⭐⭐ (Moderate–Advanced)

> A behavioral pattern that is conceptually simple (it's Composite + recursion) but earns its "advanced" reputation because real usage forces you to confront parsing, operator precedence, performance/caching, and the "when do I stop hand-rolling and reach for ANTLR?" decision. Rarely implemented from scratch in production, frequently *discussed* in interviews.

---

## Table of Contents

- [📋 Intent](#-intent)
- [🎯 Problem](#-problem)
- [✅ Solution](#-solution)
- [💻 Implementation](#-implementation)
  - [Variant 1 — Naive: giant if-else / switch string parser](#variant-1--naive-giant-if-else--switch-string-parser)
  - [Variant 2 — Improved: classic Interpreter (boolean expression tree)](#variant-2--improved-classic-interpreter-boolean-expression-tree)
  - [Variant 3 — Best practice: tokenizer + recursive-descent parser + AST interpret()](#variant-3--best-practice-tokenizer--recursive-descent-parser--ast-interpret)
- [🎨 Real-World Example](#-real-world-example)
- [✅ When to Use](#-when-to-use)
- [❌ When NOT to Use](#-when-not-to-use)
- [🎯 Pros and Cons](#-pros-and-cons)
- [🔄 Comparison with Related/Similar Patterns](#-comparison-with-relatedsimilar-patterns)
- [📊 Comparison Table](#-comparison-table)
- [💡 Common Pitfalls](#-common-pitfalls)
- [🎓 Interview Tips](#-interview-tips)
- [🔗 Related Patterns](#-related-patterns)
- [📚 Library/Framework Implementation](#-libraryframework-implementation)
- [📝 Interview Questions & Answers (FAANG Top 20)](#-interview-questions--answers-faang-top-20)
- [🧠 Staff/Principal Engineer Level](#-staffprincipal-engineer-level)
- [⚡ Quick Revision](#-quick-revision)

---

## 📋 Intent

**GoF:** *"Given a language, define a representation for its grammar along with an interpreter that uses the representation to interpret sentences in the language."*

In plainer terms: if you have a small, well-defined language (a grammar) whose "sentences" you need to evaluate repeatedly, model each grammar rule as a class. Arrange instances of those classes into an **abstract syntax tree (AST)** that represents a particular sentence, then walk the tree with an `interpret(Context)` method to produce a result.

The pattern's essence: **one class per grammar rule + recursive evaluation over a tree.** It is structurally the [Composite](#-related-patterns) pattern applied to language grammars.

---

## 🎯 Problem

You need to **evaluate sentences of a simple, well-defined grammar, over and over, with varying inputs.** The naive approach — hard-coding parsing and evaluation logic in one big procedure — becomes unmaintainable the moment the grammar grows or the rules change at runtime.

Symptoms that push you toward Interpreter:

- The same *kind* of expression is evaluated many times (e.g., a business rule applied to millions of records), so re-parsing the raw string each time is wasteful.
- The grammar has **recursive / nested structure** (`A AND (B OR C)`), which if-else chains and regexes handle badly.
- The rules are **authored by non-developers or change without a redeploy** (config DSLs, feature flags), so they must live as data, not code.

**Concrete scenarios:**

1. **Rule / business-logic engines** — evaluate predicates like `age > 18 AND country == 'US'` against user records for eligibility, fraud checks, or discounts.
2. **Arithmetic / boolean expression evaluators** — a spreadsheet cell formula, a calculator, or a search-filter expression (`price < 100 OR rating >= 4.5`).
3. **Query / filter languages** — a mini "WHERE clause" DSL for an API, or MongoDB-style filter documents translated into an evaluable tree.
4. **Regular expressions, feature-flag expressions, config DSLs** — a compact pattern language interpreted against input.

---

## ✅ Solution

**Core idea (plain language):** Represent every construct of your grammar as a small object that knows how to `interpret` itself. Numbers, variables, and literals are **leaf** objects; operators like `AND`, `+`, or `>` are **composite** objects that hold child expressions and combine their results. Assemble these objects into a tree that mirrors the sentence's structure, then call `interpret(context)` on the root. Each node recursively interprets its children and combines them. The **Context** carries whatever the interpretation needs — variable bindings, the input row, an output buffer.

**Key structural elements:**

| Element | Role |
|---|---|
| **AbstractExpression** | Interface/abstract class declaring `interpret(Context)`. |
| **TerminalExpression** | Leaf nodes — literals, variables, constants. They interpret without children. |
| **NonterminalExpression** | Composite nodes — operators, rules built from sub-expressions. They recurse into children and combine. |
| **Context** | Holds global/shared state for interpretation (variable values, the current input, accumulators). |
| **Client / Parser** | Builds the AST. Crucially, *building* the tree (parsing) is usually a **separate concern** from *evaluating* it (interpret). |

**Critical nuance:** the AST is typically constructed by a **separate parser** (hand-written recursive descent, or a generator like ANTLR), *not* inside `interpret()`. `interpret()` should only walk an already-built tree. Because the tree is a Composite and `interpret()` recurses, Interpreter is often described as **"Composite + a recursive operation."**

---

## 💻 Implementation

We'll build up three variants, weakest to strongest.

### Variant 1 — Naive: giant if-else / switch string parser

**What it solves:** the immediate need — "just evaluate this string."
**What's wrong:** parsing and evaluation are fused into one procedure. There's no tree, no reuse, no way to handle nesting/precedence cleanly, and every new operator bloats the same method. It re-parses on every call and collapses under recursion.

<details>
<summary>💻 Click to expand code</summary>

```java
// Evaluates strings like "age > 18 AND country == US".
// Works for exactly this shape and nothing else.
public class NaiveRuleEvaluator {

    public static boolean eval(String rule, Map<String, String> data) {
        // Split on AND — but what about OR? nesting? precedence? Escaping?
        String[] clauses = rule.split(" AND ");
        for (String clause : clauses) {
            String[] parts = clause.trim().split(" ");
            String field = parts[0];
            String op = parts[1];
            String expected = parts[2];
            String actual = data.get(field);

            boolean ok;
            if (op.equals("==")) {
                ok = expected.equals(actual);
            } else if (op.equals(">")) {
                ok = Integer.parseInt(actual) > Integer.parseInt(expected);
            } else if (op.equals("<")) {
                ok = Integer.parseInt(actual) < Integer.parseInt(expected);
            } else {
                throw new IllegalArgumentException("Unknown op: " + op);
            }
            if (!ok) return false; // implicit AND-only semantics
        }
        return true;
    }
}
```

</details>

**Pros:** trivially small; fine for a one-off, fixed-shape rule.
**Cons:** cannot handle OR, NOT, parentheses, or precedence; re-parses the string every call; adding an operator means editing the mega-method (violates Open/Closed); no separation of parse vs. evaluate; brittle string splitting.
**Mechanism:** none really — it's string surgery, not a grammar model. This is the anti-pattern the Interpreter pattern exists to replace.

---

### Variant 2 — Improved: classic Interpreter (boolean expression tree)

**What it solves:** introduces the real pattern — an `Expression` interface with **Terminal** (variable) and **Nonterminal** (AND/OR/NOT) nodes. The tree is built manually by the client, and `interpret(Context)` recurses. Now nesting and precedence are expressed *by the tree structure itself*.
**What's still missing:** we hand-build the tree in code; there's no parser turning a string into this tree yet (that's Variant 3).

<details>
<summary>💻 Click to expand code</summary>

```java
import java.util.Map;

// Context: variable bindings for this evaluation.
class Context {
    private final Map<String, Boolean> vars;
    Context(Map<String, Boolean> vars) { this.vars = vars; }
    boolean lookup(String name) {
        Boolean v = vars.get(name);
        if (v == null) throw new IllegalStateException("Unbound var: " + name);
        return v;
    }
}

// AbstractExpression
interface BooleanExpression {
    boolean interpret(Context ctx);
}

// TerminalExpression — a leaf that reads a variable from the context.
class Variable implements BooleanExpression {
    private final String name;
    Variable(String name) { this.name = name; }
    public boolean interpret(Context ctx) { return ctx.lookup(name); }
}

// TerminalExpression — a literal constant.
class Constant implements BooleanExpression {
    private final boolean value;
    Constant(boolean value) { this.value = value; }
    public boolean interpret(Context ctx) { return value; }
}

// NonterminalExpression — composite nodes hold sub-expressions.
class And implements BooleanExpression {
    private final BooleanExpression left, right;
    And(BooleanExpression l, BooleanExpression r) { this.left = l; this.right = r; }
    public boolean interpret(Context ctx) {
        return left.interpret(ctx) && right.interpret(ctx); // short-circuits
    }
}

class Or implements BooleanExpression {
    private final BooleanExpression left, right;
    Or(BooleanExpression l, BooleanExpression r) { this.left = l; this.right = r; }
    public boolean interpret(Context ctx) {
        return left.interpret(ctx) || right.interpret(ctx);
    }
}

class Not implements BooleanExpression {
    private final BooleanExpression expr;
    Not(BooleanExpression e) { this.expr = e; }
    public boolean interpret(Context ctx) { return !expr.interpret(ctx); }
}

// Client builds the AST for:  (isPremium OR isTrial) AND NOT isBanned
class Demo {
    public static void main(String[] args) {
        BooleanExpression rule =
            new And(
                new Or(new Variable("isPremium"), new Variable("isTrial")),
                new Not(new Variable("isBanned"))
            );

        Context ctx = new Context(Map.of(
            "isPremium", true, "isTrial", false, "isBanned", false));

        System.out.println(rule.interpret(ctx)); // true
    }
}
```

</details>

**Pros:** each rule is a class (single responsibility); nesting/precedence encoded in the tree; adding a new operator = adding a new class (Open/Closed friendly); the built tree is reusable across many contexts; short-circuit evaluation falls out naturally.
**Cons:** you must build the tree by hand (verbose); still no way to accept a string; grammar growth means many small classes ("class explosion").
**Mechanism:** **terminal vs. nonterminal** distinction (leaves read state, composites recurse), **context passing** down the tree, and **recursive tree evaluation** where structure encodes semantics.

---

### Variant 3 — Best practice: tokenizer + recursive-descent parser + AST interpret()

**What it solves:** the missing piece — a **separate parser** that turns a raw string into the AST, with correct **operator precedence** and parentheses. This is the mature form: *parse once, interpret many times.* We also show the **functional style** (`Function<Context,Integer>`) as an alternative to node classes.

Grammar (standard precedence, `*` `/` bind tighter than `+` `-`):

```
expr    := term (('+' | '-') term)*
term    := factor (('*' | '/') factor)*
factor  := NUMBER | VARIABLE | '(' expr ')' | '-' factor
```

<details>
<summary>💻 Click to expand code — Tokenizer</summary>

```java
import java.util.*;

enum TokType { NUMBER, VAR, PLUS, MINUS, STAR, SLASH, LPAREN, RPAREN, EOF }

record Token(TokType type, String text) {}

class Tokenizer {
    private final String src;
    private int pos = 0;

    Tokenizer(String src) { this.src = src; }

    List<Token> tokenize() {
        List<Token> out = new ArrayList<>();
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isWhitespace(c)) { pos++; continue; }
            switch (c) {
                case '+' -> { out.add(new Token(TokType.PLUS, "+"));  pos++; }
                case '-' -> { out.add(new Token(TokType.MINUS, "-")); pos++; }
                case '*' -> { out.add(new Token(TokType.STAR, "*"));  pos++; }
                case '/' -> { out.add(new Token(TokType.SLASH, "/")); pos++; }
                case '(' -> { out.add(new Token(TokType.LPAREN, "(")); pos++; }
                case ')' -> { out.add(new Token(TokType.RPAREN, ")")); pos++; }
                default -> {
                    if (Character.isDigit(c)) out.add(number());
                    else if (Character.isLetter(c)) out.add(identifier());
                    else throw new IllegalArgumentException("Bad char: " + c);
                }
            }
        }
        out.add(new Token(TokType.EOF, ""));
        return out;
    }

    private Token number() {
        int start = pos;
        while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) pos++;
        return new Token(TokType.NUMBER, src.substring(start, pos));
    }

    private Token identifier() {
        int start = pos;
        while (pos < src.length() && Character.isLetterOrDigit(src.charAt(pos))) pos++;
        return new Token(TokType.VAR, src.substring(start, pos));
    }
}
```

</details>

<details>
<summary>💻 Click to expand code — AST nodes (Expression interface + Terminal/Nonterminal)</summary>

```java
import java.util.Map;

class EvalContext {
    private final Map<String, Double> vars;
    EvalContext(Map<String, Double> vars) { this.vars = vars; }
    double value(String name) {
        Double v = vars.get(name);
        if (v == null) throw new IllegalStateException("Unbound: " + name);
        return v;
    }
}

interface Expr { double interpret(EvalContext ctx); }

// Terminals
record NumberExpr(double value) implements Expr {
    public double interpret(EvalContext ctx) { return value; }
}
record VarExpr(String name) implements Expr {
    public double interpret(EvalContext ctx) { return ctx.value(name); }
}

// Nonterminals
record BinaryExpr(Expr left, char op, Expr right) implements Expr {
    public double interpret(EvalContext ctx) {
        double l = left.interpret(ctx), r = right.interpret(ctx);
        return switch (op) {
            case '+' -> l + r;
            case '-' -> l - r;
            case '*' -> l * r;
            case '/' -> {
                if (r == 0) throw new ArithmeticException("Divide by zero");
                yield l / r;
            }
            default -> throw new IllegalStateException("op " + op);
        };
    }
}
record NegateExpr(Expr operand) implements Expr {
    public double interpret(EvalContext ctx) { return -operand.interpret(ctx); }
}
```

</details>

<details>
<summary>💻 Click to expand code — Recursive-descent parser (builds AST, handles precedence)</summary>

```java
import java.util.List;

// Parser is SEPARATE from interpretation. It produces an Expr tree.
class Parser {
    private final List<Token> tokens;
    private int pos = 0;

    Parser(List<Token> tokens) { this.tokens = tokens; }

    Expr parse() {
        Expr e = expr();
        expect(TokType.EOF);
        return e;
    }

    // expr := term (('+'|'-') term)*    -- lowest precedence
    private Expr expr() {
        Expr left = term();
        while (peek().type() == TokType.PLUS || peek().type() == TokType.MINUS) {
            char op = advance().type() == TokType.PLUS ? '+' : '-';
            left = new BinaryExpr(left, op, term());
        }
        return left;
    }

    // term := factor (('*'|'/') factor)*  -- higher precedence
    private Expr term() {
        Expr left = factor();
        while (peek().type() == TokType.STAR || peek().type() == TokType.SLASH) {
            char op = advance().type() == TokType.STAR ? '*' : '/';
            left = new BinaryExpr(left, op, factor());
        }
        return left;
    }

    // factor := NUMBER | VAR | '(' expr ')' | '-' factor
    private Expr factor() {
        Token t = peek();
        switch (t.type()) {
            case NUMBER -> { advance(); return new NumberExpr(Double.parseDouble(t.text())); }
            case VAR    -> { advance(); return new VarExpr(t.text()); }
            case MINUS  -> { advance(); return new NegateExpr(factor()); }
            case LPAREN -> {
                advance();
                Expr inner = expr();
                expect(TokType.RPAREN);
                return inner;
            }
            default -> throw new IllegalArgumentException("Unexpected: " + t.text());
        }
    }

    private Token peek()    { return tokens.get(pos); }
    private Token advance() { return tokens.get(pos++); }
    private void expect(TokType type) {
        if (peek().type() != type) throw new IllegalArgumentException("Expected " + type + " got " + peek().type());
        advance();
    }
}
```

</details>

<details>
<summary>💻 Click to expand code — Parse once, interpret many + functional style</summary>

```java
import java.util.*;
import java.util.function.Function;

class CalcDemo {
    public static void main(String[] args) {
        // Parse ONCE into an AST...
        Expr ast = new Parser(new Tokenizer("2 + 3 * (x - 1)").tokenize()).parse();

        // ...interpret MANY times against different contexts.
        System.out.println(ast.interpret(new EvalContext(Map.of("x", 5.0)))); // 2 + 3*4 = 14.0
        System.out.println(ast.interpret(new EvalContext(Map.of("x", 2.0)))); // 2 + 3*1 = 5.0

        // Functional flavor: an Expr is really Function<Context, Double>.
        Function<Map<String,Double>, Double> f =
            ctx -> ast.interpret(new EvalContext(ctx));
        System.out.println(f.apply(Map.of("x", 10.0))); // 2 + 3*9 = 29.0
    }
}
```

</details>

**Pros:** clean separation of tokenize → parse → interpret; correct precedence and parentheses via grammar-mirroring methods; the AST is built once and reused; extensible (add a production + node); the functional view (`Function<Context,T>`) is compact and composable.
**Cons:** more moving parts; hand-written parsers get painful as grammar complexity climbs — beyond a modest grammar, prefer a parser generator (ANTLR).
**Mechanism:** **separating parser from interpreter** (parsing builds structure once; `interpret()` only evaluates), **recursive-descent** encoding precedence in the call hierarchy, and **recursive tree evaluation** over Terminal/Nonterminal nodes with **context passing**.

---

## 🎨 Real-World Example

A **production-style rule engine** evaluating boolean predicates such as `age > 18 AND country == "US" AND (isPremium OR spend > 500)` against user records. This is the shape you'd see in eligibility, fraud, discount, or feature-targeting systems. It combines a comparison layer (terminals compare a field to a value) with boolean composition (nonterminals), plus a tiny parser so rules can live as configuration strings.

<details>
<summary>💻 Click to expand code — Rule engine (AST + comparisons)</summary>

```java
import java.util.*;

// Context = the record under evaluation.
final class RuleContext {
    private final Map<String, Object> fields;
    RuleContext(Map<String, Object> fields) { this.fields = fields; }
    Object get(String field) { return fields.get(field); }
}

interface Rule { boolean interpret(RuleContext ctx); }

// --- Terminal: field comparison ---
final class Comparison implements Rule {
    enum Op { GT, LT, GE, LE, EQ, NE }
    private final String field; private final Op op; private final Object literal;

    Comparison(String field, Op op, Object literal) {
        this.field = field; this.op = op; this.literal = literal;
    }

    public boolean interpret(RuleContext ctx) {
        Object actual = ctx.get(field);
        if (actual == null) return false;
        return switch (op) {
            case EQ -> Objects.equals(actual, literal);
            case NE -> !Objects.equals(actual, literal);
            default -> {
                double a = ((Number) actual).doubleValue();
                double b = ((Number) literal).doubleValue();
                yield switch (op) {
                    case GT -> a > b;  case LT -> a < b;
                    case GE -> a >= b; case LE -> a <= b;
                    default -> false;
                };
            }
        };
    }
}

// --- Nonterminals ---
final class AndRule implements Rule {
    private final Rule l, r;
    AndRule(Rule l, Rule r) { this.l = l; this.r = r; }
    public boolean interpret(RuleContext c) { return l.interpret(c) && r.interpret(c); }
}
final class OrRule implements Rule {
    private final Rule l, r;
    OrRule(Rule l, Rule r) { this.l = l; this.r = r; }
    public boolean interpret(RuleContext c) { return l.interpret(c) || r.interpret(c); }
}
final class NotRule implements Rule {
    private final Rule inner;
    NotRule(Rule inner) { this.inner = inner; }
    public boolean interpret(RuleContext c) { return !inner.interpret(c); }
}
```

</details>

<details>
<summary>💻 Click to expand code — Demo exercising multiple rules</summary>

```java
import java.util.*;
import static Comparison.Op.*;

public class RuleEngineDemo {
    public static void main(String[] args) {
        // Rule: age > 18 AND country == "US" AND (isPremium OR spend > 500)
        Rule rule =
            new AndRule(
                new AndRule(
                    new Comparison("age", GT, 18),
                    new Comparison("country", EQ, "US")
                ),
                new OrRule(
                    new Comparison("isPremium", EQ, Boolean.TRUE),
                    new Comparison("spend", GT, 500)
                )
            );

        RuleContext alice = new RuleContext(Map.of(
            "age", 30, "country", "US", "isPremium", false, "spend", 750));
        RuleContext bob = new RuleContext(Map.of(
            "age", 16, "country", "US", "isPremium", true, "spend", 999));
        RuleContext carol = new RuleContext(Map.of(
            "age", 40, "country", "CA", "isPremium", true, "spend", 100));

        System.out.println(rule.interpret(alice)); // true  (adult, US, spend>500)
        System.out.println(rule.interpret(bob));   // false (age 16 fails)
        System.out.println(rule.interpret(carol)); // false (country CA fails)
    }
}
```

</details>

The same immutable `rule` tree is evaluated against many records — that reuse is exactly the payoff Interpreter promises. In a full system, a small parser (like Variant 3's) would turn a config string into this tree so business users can author rules without a code deploy.

---

## ✅ When to Use

<details>
<summary>📖 Click to expand</summary>

- The grammar is **simple and stable** — a handful of rules, modest nesting. GoF explicitly scope Interpreter to *simple* grammars.
- Sentences of the language are **evaluated repeatedly** (parse once, interpret many), so building a reusable AST pays off.
- The grammar has **recursive structure** that if-else / regex handle poorly (`(A OR B) AND NOT C`).
- Rules need to be **data, authored/changed at runtime** (config DSLs, feature-flag expressions, business rules) rather than recompiled code.
- **Efficiency of the parser is not the top concern** — Interpreter tree-walking is not the fastest approach, but is fine when clarity and flexibility matter more.
- You want each grammar rule isolated so you can **add/modify operators independently** (Open/Closed).

</details>

## ❌ When NOT to Use

<details>
<summary>📖 Click to expand</summary>

- The grammar is **complex or likely to grow** (many precedence levels, real syntax) — you'll hit *class explosion* and a fragile hand-written parser. Reach for **ANTLR / JavaCC / a parser generator** instead.
- You need **maximum runtime performance**. Tree-walking interpretation is slow relative to compiling the AST to bytecode or a closure/lambda pipeline; if you evaluate the same expression billions of times, compile it.
- The "language" is actually **trivial** (a single fixed comparison). A plain method or `Predicate` is simpler than a whole AST.
- You must evaluate **untrusted user input** and can't afford to build a safe sandboxed grammar — a naive interpreter can be a DoS/injection risk (deeply nested input → stack overflow; unbounded loops).
- A well-established engine already exists for your domain (SpEL, Drools, a regex engine, a SQL parser) — **don't reinvent it.**

</details>

## 🎯 Pros and Cons

<details>
<summary>📖 Click to expand</summary>

**Pros**
- **Grammar rules become first-class objects** — easy to reason about each in isolation.
- **Easy to extend the grammar** — add a new node class (and a production if you have a parser); existing nodes untouched (Open/Closed).
- **Recursive structure handled naturally** — nesting and precedence are encoded by the tree shape.
- **Reusable ASTs** — parse once, evaluate against many contexts/inputs.
- **Composable / testable** — each node is a tiny unit; the functional view (`Function<Context,T>`) makes composition trivial.

**Cons**
- **Class explosion** — every grammar rule is a class; complex grammars become unwieldy.
- **Poor performance** for hot paths — tree-walking is slower than compiled code.
- **Only suits simple grammars** — GoF's own caveat; big grammars want a parser generator.
- **Parsing often needed anyway** — the pattern covers evaluation but you still need a (possibly complex) parser to build the tree.
- **Maintenance drift** — mixing parsing into `interpret()`, or scattering precedence logic, quickly rots.

</details>

## 🔄 Comparison with Related/Similar Patterns

<details>
<summary>📖 Click to expand</summary>

- **vs. Composite:** Interpreter *is* Composite specialized for grammars. Composite is about part-whole object trees generally; Interpreter's tree specifically represents a *sentence* and adds a semantic operation `interpret(context)`. If you understand Composite, you understand Interpreter's structure.
- **vs. Visitor:** With Interpreter, the operation (`interpret`) lives *inside* each node. If you need **many operations** over the same AST (evaluate, pretty-print, type-check, optimize), keeping them all as methods on nodes bloats the classes. **Visitor** externalizes those operations, letting you add new operations without touching node classes. Mature interpreters often combine both: Interpreter defines the AST, Visitor adds operations over it.
- **vs. Strategy:** Strategy swaps one interchangeable algorithm behind an interface. Interpreter composes *many* small objects into a tree to represent structured language. A single terminal node resembles a Strategy, but the tree-of-rules composition is what distinguishes Interpreter.
- **When to use a real parser generator instead (ANTLR / JavaCC):** once the grammar has meaningful complexity (multiple precedence levels, real syntax, error recovery, large rule sets), hand-writing nodes and a recursive-descent parser stops scaling. **ANTLR** generates the lexer/parser and gives you a parse-tree + visitor/listener API — you then apply Interpreter/Visitor *on top of* the generated tree. Interpreter is the pattern; ANTLR is the industrial tool for the same job at scale.

</details>

## 📊 Comparison Table

<details>
<summary>📖 Click to expand</summary>

| Axis | V1 Naive if-else parser | V2 Classic Interpreter (hand-built tree) | V3 Tokenizer + parser + AST | ANTLR / parser generator |
|---|---|---|---|---|
| **Grammar complexity supported** | Trivial, fixed shape | Simple, but tree built by hand | Simple–moderate w/ precedence & parens | Moderate–very complex |
| **Extensibility (new operator)** | Edit the mega-method | Add a node class | Add a node + production | Edit `.g4` grammar, regenerate |
| **Handles nesting / precedence** | No | Via tree structure only | Yes, via grammar productions | Yes, natively |
| **Parse vs. eval separated** | No (fused) | Partly (no parser at all) | Yes (clean) | Yes (generated parser) |
| **Performance** | Fast but re-parses each call | Fast eval, manual build | Parse once / eval many | Fast; can generate optimized parsers |
| **Maintainability** | Bad; rots fast | OK for small grammars | Good | Best at scale; extra tooling/build step |
| **When to pick** | One-off throwaway | Teaching / tiny fixed rules | Real small DSL / rule engine | Anything non-trivial or growing |

</details>

## 💡 Common Pitfalls

<details>
<summary>📖 Click to expand</summary>

**Pitfall 1 — Class explosion for complex grammars.** Every rule becomes a class; a real language balloons into dozens of near-identical node classes plus a fragile parser.

<details>
<summary>💻 What goes wrong</summary>

```java
// One class per operator... and it never ends.
class Add implements Expr { /*...*/ }
class Sub implements Expr { /*...*/ }
class Mul implements Expr { /*...*/ }
class Div implements Expr { /*...*/ }
class Mod implements Expr { /*...*/ }
class Pow implements Expr { /*...*/ }
class BitAnd implements Expr { /*...*/ }
// ...and comparisons, function calls, ternary, indexing, ... 40 classes later.
```

</details>

<details>
<summary>✅ Fix</summary>

```java
// Collapse families into one parameterized node, OR stop hand-rolling.
record BinaryExpr(Expr l, char op, Expr r) implements Expr {
    public double interpret(EvalContext c) {
        double a = l.interpret(c), b = r.interpret(c);
        return switch (op) { case '+' -> a+b; case '-' -> a-b;
                             case '*' -> a*b; case '/' -> a/b; default -> 0; };
    }
}
// If the grammar keeps growing, switch to ANTLR — don't fight class explosion.
```

</details>

---

**Pitfall 2 — Mixing parsing into `interpret()`.** Nodes that parse strings while evaluating re-parse on every call and tangle two concerns.

<details>
<summary>💻 What goes wrong</summary>

```java
class BadExpr implements Expr {
    private final String raw;
    BadExpr(String raw) { this.raw = raw; }
    public double interpret(EvalContext c) {
        // Re-tokenizing and re-parsing on EVERY evaluation. Slow + tangled.
        List<Token> toks = new Tokenizer(raw).tokenize();
        return new Parser(toks).parse().interpret(c);
    }
}
```

</details>

<details>
<summary>✅ Fix</summary>

```java
// Parse ONCE, keep the AST, interpret many times.
Expr ast = new Parser(new Tokenizer(raw).tokenize()).parse();
for (EvalContext c : contexts) {
    double result = ast.interpret(c); // no re-parsing
}
```

</details>

---

**Pitfall 3 — Poor performance vs. compiled/cached approaches.** Tree-walking each time, and re-parsing identical rule strings, wastes CPU on hot paths.

<details>
<summary>💻 What goes wrong</summary>

```java
double evaluate(String rule, EvalContext ctx) {
    // Called in a tight loop over millions of rows — parses every time!
    return new Parser(new Tokenizer(rule).tokenize()).parse().interpret(ctx);
}
```

</details>

<details>
<summary>✅ Fix</summary>

```java
// Cache compiled ASTs by rule string (thread-safe).
private final Map<String, Expr> cache = new ConcurrentHashMap<>();

double evaluate(String rule, EvalContext ctx) {
    Expr ast = cache.computeIfAbsent(rule,
        r -> new Parser(new Tokenizer(r).tokenize()).parse());
    return ast.interpret(ctx);
}
// For true hot paths, compile the AST to a lambda/bytecode instead of walking it.
```

</details>

---

**Pitfall 4 — Operator precedence / associativity bugs.** Flat parsing treats `2 + 3 * 4` as `(2+3)*4`, or right-associates subtraction.

<details>
<summary>💻 What goes wrong</summary>

```java
// One flat loop over all operators ignores precedence.
Expr e = factor();
while (isOperator(peek())) {
    char op = advance();          // treats + and * with EQUAL precedence
    e = new BinaryExpr(e, op, factor()); // "2 + 3 * 4" => (2+3)*4 = 20  (WRONG)
}
```

</details>

<details>
<summary>✅ Fix</summary>

```java
// Separate methods per precedence level; loops give LEFT associativity.
Expr expr() {                     // + and - : lowest
    Expr l = term();
    while (peek()==PLUS || peek()==MINUS) l = new BinaryExpr(l, adv(), term());
    return l;
}
Expr term() {                     // * and / : higher
    Expr l = factor();
    while (peek()==STAR || peek()==SLASH) l = new BinaryExpr(l, adv(), factor());
    return l;
}
// Now "2 + 3 * 4" => 2 + (3*4) = 14  (CORRECT)
```

</details>

</details>

## 🎓 Interview Tips

<details>
<summary>📖 What interviewers commonly ask</summary>

- "Define Interpreter and give the GoF one-liner." (Have the definition ready.)
- "How is it related to Composite?" — the #1 follow-up. Answer: Interpreter's AST *is* a Composite; `interpret()` is the recursive operation.
- "Where does parsing fit? Is parsing part of the pattern?" — parsing builds the tree and is usually a *separate* concern; the pattern proper is the evaluation over the tree.
- "How would you handle operator precedence?" — recursive-descent with one method per precedence level.
- "When would you NOT use it / when do you reach for ANTLR?" — this is almost always asked; answer that it only suits simple grammars.
- Coding: "evaluate a boolean rule" or "build a calculator with `+ - * /` and parentheses."

</details>

<details>
<summary>📖 What to proactively mention</summary>

- **It only suits *simple*, stable grammars.** For anything complex/growing, reach for a **parser generator like ANTLR** (or JavaCC) rather than hand-rolling nodes and parsers — say this unprompted; it signals maturity.
- **Interpreter = Composite + recursion.** Naming the relationship shows you see patterns as a system, not trivia.
- **Separate parsing from interpretation**, and **cache/compile the AST** ("parse once, interpret many") for performance.
- **Visitor** is the natural partner when you need many operations over the AST (eval, print, type-check).
- **Flyweight** can share immutable **terminal symbols** (constants, keywords) to cut memory when the tree is huge.
- **Security**: evaluating untrusted expressions needs a sandbox (bound recursion depth, no arbitrary calls, timeouts) — mention DoS via deep nesting.

</details>

## 🔗 Related Patterns

<details>
<summary>📖 Click to expand</summary>

- **Composite** — the structural backbone of the AST. Interpreter is Composite plus a recursive `interpret()` operation.
- **Visitor** — externalizes operations over the AST so you can add operations (evaluate, print, optimize, type-check) without modifying node classes. The classic pairing for non-trivial interpreters/compilers.
- **Iterator** — can traverse the AST when an operation needs to walk nodes without recursion baked into the nodes.
- **Flyweight** — shares immutable **terminal symbols** (constants, keywords, common literals) across many parts of the tree to reduce memory footprint for large sentences/many ASTs.

</details>

## 📚 Library/Framework Implementation

**1. `java.util.regex.Pattern`** — a regular expression is a *language*; `Pattern.compile(regex)` parses it once into an internal node tree (the AST of the regex), and `Matcher` interprets that tree against input. This is textbook "parse once (compile), interpret many (match repeatedly)."

<details>
<summary>💻 Click to expand code</summary>

```java
import java.util.regex.*;

// Compile ONCE (build the AST), match MANY times (interpret).
Pattern p = Pattern.compile("^[a-z]+@[a-z]+\\.(com|org)$");
System.out.println(p.matcher("bob@acme.com").matches()); // true
System.out.println(p.matcher("nope@x.io").matches());    // false
// Internally Pattern builds a tree of Node subclasses and walks it — Interpreter.
```

</details>

**2. Spring Expression Language (SpEL)** — `SpelExpressionParser` parses a string into an `Expression` (an AST) and `getValue(context)` interprets it against an `EvaluationContext`. Widely used in Spring annotations (`@Value("#{...}")`, security expressions). It's a production-grade Interpreter with a real parser.

<details>
<summary>💻 Click to expand code</summary>

```java
import org.springframework.expression.*;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

ExpressionParser parser = new SpelExpressionParser();
Expression exp = parser.parseExpression("age > 18 and country == 'US'"); // parse once

StandardEvaluationContext ctx = new StandardEvaluationContext();
ctx.setVariable("age", 25);
ctx.setVariable("country", "US");
// SpEL uses #var syntax; illustrative:
Expression e = parser.parseExpression("#age > 18 and #country == 'US'");
System.out.println(e.getValue(ctx, Boolean.class)); // true
```

</details>

**3. `java.text.MessageFormat`** — a small formatting DSL (`"{0} has {1,number,#.##} points"`). It parses the pattern into format elements once, then interprets them against argument arrays to produce output — an Interpreter over a formatting grammar. (Drools and ANTLR-generated interpreters are heavier-weight real-world examples of the same idea: Drools compiles rule DSLs; ANTLR generates parsers whose trees you interpret/visit.)

<details>
<summary>💻 Click to expand code</summary>

```java
import java.text.MessageFormat;

MessageFormat fmt = new MessageFormat("{0} scored {1,number,#.#} on {2,date,short}");
String out = fmt.format(new Object[]{ "Ada", 98.75, new java.util.Date() });
System.out.println(out); // e.g. "Ada scored 98.8 on 7/15/26"
// The pattern is parsed into format elements (the grammar), then interpreted per call.
```

</details>

---

## 📝 Interview Questions & Answers (FAANG Top 20)

<details>
<summary><strong>Q1 (Conceptual): What is the Interpreter pattern and what problem does it solve?</strong></summary>

Interpreter is a behavioral GoF pattern: *given a language, define a representation for its grammar plus an interpreter that uses that representation to interpret sentences of the language.* You model each grammar rule as a class, assemble instances into an abstract syntax tree that represents a specific sentence, then call `interpret(context)` on the root, which recurses through the tree to produce a result. It solves the problem of repeatedly evaluating sentences of a **simple, well-defined grammar** without drowning in ad-hoc if-else/regex parsing code. It shines when rules are recursive, change at runtime, or are authored as data (config DSLs, rule engines, expression evaluators).

</details>

<details>
<summary><strong>Q2 (Conceptual): Name the participants and their roles.</strong></summary>

- **AbstractExpression** — declares `interpret(Context)`.
- **TerminalExpression** — leaf nodes (literals, variables, constants) that interpret without children.
- **NonterminalExpression** — composite nodes (operators, compound rules) that hold sub-expressions and combine their results recursively.
- **Context** — carries shared state for interpretation (variable bindings, current input, accumulators).
- **Client / Parser** — builds the AST from a sentence. The parser is usually a *separate* concern from the interpretation.

The tree of terminals and nonterminals is a Composite; `interpret()` is the recursive operation over it.

</details>

<details>
<summary><strong>Q3 (Conceptual): How is Interpreter related to the Composite pattern?</strong></summary>

They're structurally the same idea: Interpreter's AST *is* a Composite tree. Terminal expressions are the leaves and nonterminal expressions are the composites that contain children. What Interpreter adds on top of Composite is a **domain-specific recursive operation** — `interpret(context)` — and the semantic interpretation of the tree as a *sentence in a grammar*. A common one-liner: "Interpreter = Composite + a recursive interpret operation." Recognizing this tells the interviewer you see patterns compositionally rather than as isolated recipes.

</details>

<details>
<summary><strong>Q4 (Conceptual): Is parsing part of the Interpreter pattern?</strong></summary>

Strictly, the pattern covers **representing the grammar** and **interpreting an already-built AST**; it does *not* prescribe how you build the tree. In practice you need a parser, but building the tree (parsing) should be a **separate concern** from evaluating it (`interpret`). GoF note the pattern "doesn't address parsing" — you supply a hand-written parser, a parser generator (ANTLR/JavaCC), or build the tree programmatically. Keeping parse and eval separate lets you *parse once and interpret many times*, which is the key performance idiom.

</details>

<details>
<summary><strong>Q5 (Conceptual): Give real-world examples where Interpreter is used.</strong></summary>

- `java.util.regex.Pattern` compiles a regex into a node tree and matches (interprets) input against it.
- Spring Expression Language (SpEL) parses expressions into an `Expression` AST and evaluates via `getValue(context)`.
- `java.text.MessageFormat` / `Format` parse a pattern into elements then interpret them.
- Rule engines (Drools), SQL/query filter DSLs, feature-flag expression evaluators, calculators/spreadsheet formulas.
- ANTLR-generated parsers produce a tree that you then interpret or visit.

All follow "parse once, interpret many" over a modest grammar.

</details>

<details>
<summary><strong>Q6 (Implementation): Code the classic boolean expression interpreter (AND/OR/NOT).</strong></summary>

Define an `Expression` interface, terminal `Variable`/`Constant`, and nonterminal `And`/`Or`/`Not`. Build the tree, pass a context of variable bindings, and recurse.

<details>
<summary>💻 Code</summary>

```java
interface Expr { boolean interpret(java.util.Map<String,Boolean> ctx); }

record Var(String name) implements Expr {
    public boolean interpret(java.util.Map<String,Boolean> c) { return c.get(name); }
}
record And(Expr l, Expr r) implements Expr {
    public boolean interpret(java.util.Map<String,Boolean> c) { return l.interpret(c) && r.interpret(c); }
}
record Or(Expr l, Expr r) implements Expr {
    public boolean interpret(java.util.Map<String,Boolean> c) { return l.interpret(c) || r.interpret(c); }
}
record Not(Expr e) implements Expr {
    public boolean interpret(java.util.Map<String,Boolean> c) { return !e.interpret(c); }
}

// (a OR b) AND NOT c
Expr rule = new And(new Or(new Var("a"), new Var("b")), new Not(new Var("c")));
System.out.println(rule.interpret(java.util.Map.of("a",true,"b",false,"c",false))); // true
```

</details>

</details>

<details>
<summary><strong>Q7 (Implementation): How do you pass state during interpretation? What goes in the Context?</strong></summary>

The **Context** is an argument threaded through every `interpret(context)` call. It holds whatever the nodes need: variable/symbol bindings, the current input record, an output buffer, a scope stack, or configuration. Terminals typically *read* from it (a `Variable` looks up its value); nonterminals mostly pass it down unchanged. Keeping shared state in an explicit context (rather than in node fields) is what lets you reuse one immutable AST across many evaluations — you just supply a different context each time. For thread-safety, prefer an immutable AST plus a per-call/per-thread context.

</details>

<details>
<summary><strong>Q8 (Implementation): Where should the parser live, and why keep it separate from interpret()?</strong></summary>

The parser is a distinct component (a tokenizer + a recursive-descent parser, or a generated parser) that consumes the raw string and produces the AST. Keeping it out of `interpret()` matters because (1) parsing is expensive and shouldn't repeat on every evaluation — you parse once, cache the AST, interpret many times; (2) it separates syntax concerns (tokens, precedence, error reporting) from semantics (evaluation); (3) it lets you swap parser implementations (hand-rolled vs. ANTLR) without touching node logic. A node that re-tokenizes a string inside `interpret()` is a classic anti-pattern.

</details>

<details>
<summary><strong>Q9 (Implementation): Show the functional style of Interpreter.</strong></summary>

An `Expr` is essentially a `Function<Context, T>`. Instead of node classes you can compose lambdas/closures; each combinator returns a function that closes over its sub-expressions. This is compact and highly composable, and it's how many modern interpreters/DSLs in Java are built.

<details>
<summary>💻 Code</summary>

```java
import java.util.*; import java.util.function.*;

interface E extends Function<Map<String,Double>, Double> {}

static E num(double v)      { return c -> v; }
static E var(String n)      { return c -> c.get(n); }
static E add(E a, E b)      { return c -> a.apply(c) + b.apply(c); }
static E mul(E a, E b)      { return c -> a.apply(c) * b.apply(c); }

// 2 + 3 * x
E ast = add(num(2), mul(num(3), var("x")));
System.out.println(ast.apply(Map.of("x", 4.0))); // 14.0
```

</details>

</details>

<details>
<summary><strong>Q10 (Implementation): Handle operator precedence in a hand-written parser.</strong></summary>

Use **recursive descent with one method per precedence level**, lowest at the top. Higher-precedence operators are parsed by methods called deeper in the recursion, so they bind tighter. Left-associativity comes from looping (`while`) at each level; right-associativity from recursing on the right operand.

<details>
<summary>💻 Code</summary>

```java
Expr expr()   { Expr l = term();   while (is(PLUS)||is(MINUS)) l = bin(l, term());   return l; } // low
Expr term()   { Expr l = factor(); while (is(STAR)||is(SLASH)) l = bin(l, factor()); return l; } // high
Expr factor() { /* NUMBER | VAR | '(' expr ')' | '-' factor */ }
// "2 + 3 * 4" parses to 2 + (3*4) because term() consumes * before expr() sees +.
```

</details>

</details>

<details>
<summary><strong>Q11 (Breaking): What breaks when you flat-parse without precedence levels?</strong></summary>

If you loop over all operators with equal precedence, `2 + 3 * 4` builds `((2+3)*4) = 20` instead of `2 + (3*4) = 14`. Similarly, naive right-recursion makes subtraction right-associative so `10 - 3 - 2` becomes `10 - (3-2) = 9` instead of `(10-3)-2 = 5`. The fix is separate precedence-level methods and looping (not recursing) for left-associative operators. In interviews, always test `2 + 3 * 4` and `10 - 3 - 2` to prove correctness.

</details>

<details>
<summary><strong>Q12 (Breaking): Why can re-parsing inside interpret() destroy performance?</strong></summary>

If a node tokenizes/parses the source string every time `interpret()` runs, you pay full parse cost on each evaluation. In a rule engine scanning millions of rows, that turns an O(1)-per-row evaluation into O(parse) per row — orders of magnitude slower — and tangles parsing with evaluation. Fix: parse once into an AST and reuse it; cache compiled ASTs keyed by rule string in a `ConcurrentHashMap`; for truly hot paths, compile the AST into a lambda pipeline or bytecode instead of tree-walking.

</details>

<details>
<summary><strong>Q13 (Breaking): What are the risks of interpreting untrusted expressions?</strong></summary>

Evaluating user-supplied expressions is a security surface. Risks: **DoS via deeply nested input** causing stack overflow in recursive `interpret()`/parser; **resource exhaustion** from expressions that loop or allocate heavily; **injection / arbitrary code execution** if the language exposes method calls or reflection (SpEL, for instance, can be dangerous if you evaluate untrusted input with a permissive context). Mitigations: bound recursion/nesting depth and input length, use timeouts, whitelist allowed operations/functions, run in a restricted (sandboxed) evaluation context, and never expose reflection or I/O to the grammar.

</details>

<details>
<summary><strong>Q14 (Breaking): How does class explosion manifest and how do you contain it?</strong></summary>

Each grammar rule becomes a class, so a language with many operators, comparisons, function calls, indexing, ternaries, etc. explodes into dozens of tiny classes plus an increasingly fragile parser. Contain it by (1) parameterizing node families (one `BinaryExpr` with an op field instead of `Add`/`Sub`/`Mul`/...), (2) using the functional/lambda style to avoid boilerplate classes, and (3) most importantly, recognizing the ceiling: once the grammar is genuinely complex, stop hand-rolling and adopt a parser generator (ANTLR/JavaCC), which handles the grammar declaratively.

</details>

<details>
<summary><strong>Q15 (Trade-off): Interpreter vs. Strategy — when is each right?</strong></summary>

Strategy encapsulates **one interchangeable algorithm** behind an interface and swaps implementations at runtime; there's no tree, no grammar. Interpreter composes **many small objects into a tree** to represent and evaluate a structured *language*. If your problem is "pick one of N algorithms," use Strategy. If it's "evaluate arbitrary nested sentences of a grammar," use Interpreter. A single terminal node can look like a tiny Strategy, but the defining feature of Interpreter is the recursive composition of rule objects.

</details>

<details>
<summary><strong>Q16 (Trade-off): Interpreter vs. Visitor over the same AST — which and when?</strong></summary>

Put the operation *inside* the nodes (Interpreter's `interpret()`) when there's essentially **one operation** and you'd rather add new node types easily. Use **Visitor** when you need **many operations** over a stable AST (evaluate, pretty-print, type-check, optimize) — Visitor externalizes operations so you add a new operation without editing every node class. Trade-off: Interpreter makes adding node types easy but adding operations hard; Visitor makes adding operations easy but adding node types hard (you must update every visitor). Real compilers use Interpreter for the AST structure and Visitor for the passes.

</details>

<details>
<summary><strong>Q17 (Trade-off): When should you abandon Interpreter for ANTLR/JavaCC?</strong></summary>

GoF scope Interpreter to **simple grammars**. Switch to a parser generator when: the grammar has many precedence levels or real syntax; you need robust error recovery/reporting; the rule set is large or evolving; or you want a maintainable declarative grammar (`.g4`) instead of hand-written parsing code. ANTLR generates the lexer/parser and gives you a parse tree plus listener/visitor APIs — you then layer Interpreter/Visitor semantics on top. Rule of thumb: hand-rolled Interpreter for a small stable DSL; ANTLR for anything non-trivial or growing.

</details>

<details>
<summary><strong>Q18 (Advanced): How and why would you cache/memoize ASTs?</strong></summary>

Because parsing is the expensive step and the same expression is usually evaluated repeatedly, cache the **compiled AST keyed by the source string**, e.g. `ConcurrentHashMap<String,Expr>` with `computeIfAbsent`. This turns repeated `eval(sameRule, ctx)` calls into pure tree-walks. If ASTs are immutable (recommended), the cache is trivially thread-safe to share. Beyond AST caching, you can **memoize pure sub-expressions** whose inputs don't change, or **compile** the AST to a lambda/bytecode for hot paths. Bound the cache (LRU) if rule strings are unbounded/user-generated to avoid memory leaks.

</details>

<details>
<summary><strong>Q19 (Advanced): How does Flyweight relate to Interpreter?</strong></summary>

Terminal symbols — constants, keywords, common literals, variable references — are often **immutable and repeated many times** across a large AST or across many ASTs. Flyweight lets you **share single instances** of these terminals instead of allocating duplicates, reducing memory. GoF explicitly mention Flyweight as a companion for terminal symbols. Since a shared terminal must be stateless (its varying state lives in the passed-in Context), Interpreter's context-passing design makes terminals naturally flyweight-able: `TRUE`, `ZERO`, or a `Var("x")` node can be a shared singleton.

</details>

<details>
<summary><strong>Q20 (Coding challenge): Build a calculator supporting + - * / and parentheses. Full solution.</strong></summary>

Tokenize, parse with recursive descent (precedence + parens), then interpret the AST. Parse once, evaluate with a variable context.

<details>
<summary>💻 Full solution</summary>

```java
import java.util.*;

public class Calculator {
    // ---- Tokens ----
    enum T { NUM, VAR, PLUS, MINUS, STAR, SLASH, LP, RP, EOF }
    record Tok(T t, String s) {}

    // ---- AST ----
    interface E { double eval(Map<String,Double> c); }
    record Num(double v) implements E { public double eval(Map<String,Double> c){ return v; } }
    record Var(String n) implements E { public double eval(Map<String,Double> c){ return c.get(n); } }
    record Bin(E l, char op, E r) implements E {
        public double eval(Map<String,Double> c){
            double a=l.eval(c), b=r.eval(c);
            return switch(op){ case '+'->a+b; case '-'->a-b; case '*'->a*b;
                case '/'-> { if(b==0) throw new ArithmeticException("/0"); yield a/b; }
                default -> throw new IllegalStateException(); };
        }
    }
    record Neg(E e) implements E { public double eval(Map<String,Double> c){ return -e.eval(c); } }

    // ---- Lexer ----
    static List<Tok> lex(String s){
        List<Tok> out=new ArrayList<>(); int i=0;
        while(i<s.length()){
            char c=s.charAt(i);
            if(Character.isWhitespace(c)){ i++; continue; }
            switch(c){
                case '+'->{ out.add(new Tok(T.PLUS,"+")); i++; }
                case '-'->{ out.add(new Tok(T.MINUS,"-")); i++; }
                case '*'->{ out.add(new Tok(T.STAR,"*")); i++; }
                case '/'->{ out.add(new Tok(T.SLASH,"/")); i++; }
                case '('->{ out.add(new Tok(T.LP,"(")); i++; }
                case ')'->{ out.add(new Tok(T.RP,")")); i++; }
                default->{
                    int j=i;
                    if(Character.isDigit(c)){ while(j<s.length()&&(Character.isDigit(s.charAt(j))||s.charAt(j)=='.'))j++; out.add(new Tok(T.NUM,s.substring(i,j))); }
                    else if(Character.isLetter(c)){ while(j<s.length()&&Character.isLetterOrDigit(s.charAt(j)))j++; out.add(new Tok(T.VAR,s.substring(i,j))); }
                    else throw new IllegalArgumentException("bad char "+c);
                    i=j;
                }
            }
        }
        out.add(new Tok(T.EOF,"")); return out;
    }

    // ---- Parser (recursive descent) ----
    static class P {
        final List<Tok> ts; int p=0;
        P(List<Tok> ts){ this.ts=ts; }
        E parse(){ E e=expr(); expect(T.EOF); return e; }
        E expr(){ E l=term(); while(pk()==T.PLUS||pk()==T.MINUS){ char o= adv().t()==T.PLUS?'+':'-'; l=new Bin(l,o,term()); } return l; }
        E term(){ E l=fac(); while(pk()==T.STAR||pk()==T.SLASH){ char o= adv().t()==T.STAR?'*':'/'; l=new Bin(l,o,fac()); } return l; }
        E fac(){
            Tok t=pk2();
            switch(t.t()){
                case NUM->{ adv(); return new Num(Double.parseDouble(t.s())); }
                case VAR->{ adv(); return new Var(t.s()); }
                case MINUS->{ adv(); return new Neg(fac()); }
                case LP->{ adv(); E e=expr(); expect(T.RP); return e; }
                default-> throw new IllegalArgumentException("unexpected "+t.s());
            }
        }
        T pk(){ return ts.get(p).t(); }
        Tok pk2(){ return ts.get(p); }
        Tok adv(){ return ts.get(p++); }
        void expect(T t){ if(pk()!=t) throw new IllegalArgumentException("expected "+t); adv(); }
    }

    public static void main(String[] a){
        E ast=new P(lex("2 + 3 * (x - 1)")).parse(); // parse once
        System.out.println(ast.eval(Map.of("x",5.0))); // 14.0
        System.out.println(ast.eval(Map.of("x",2.0))); // 5.0
        System.out.println(new P(lex("-(4 + 2) / 3")).parse().eval(Map.of())); // -2.0
    }
}
```

</details>

</details>

---

## 🧠 Staff/Principal Engineer Level

<details>
<summary><strong>SP1: Interpreter (tree-walking) vs. compiling to bytecode/closures — when and how do you cross that line?</strong></summary>

Tree-walking is simple and flexible but has per-node dispatch overhead (virtual calls, boxing, poor cache locality) — fine for thousands/millions of evaluations, painful for billions in a hot loop. As a staff engineer you'd stage it: start with a tree-walker; if profiling shows it's the bottleneck, **compile the AST**. Options in Java: (1) compile each node into a `DoubleUnaryOperator`/`Function` closure so evaluation is a chain of composed lambdas (JIT-friendly, no tree traversal); (2) generate JVM bytecode at runtime (ASM/ByteBuddy) or emit `MethodHandle` trees; (3) for numeric kernels, generate specialized code. The trade-off is complexity, warm-up cost, and debuggability vs. throughput. Keep the interpreter as the reference/fallback and the compiler as an optimization, validated to produce identical results.

</details>

<details>
<summary><strong>SP2: How do you make a shared interpreter thread-safe at scale?</strong></summary>

Make the **AST immutable and stateless** — no mutable fields on nodes — so a single compiled tree can be shared across all threads without synchronization. Put *all* varying state in a **per-call/per-thread Context** (variable bindings, buffers). The AST cache should be a `ConcurrentHashMap` populated via `computeIfAbsent`; ensure the compile function is idempotent (harmless if computed twice under race). Avoid shared mutable accumulators inside nodes; if you must aggregate, pass a fresh accumulator in the context or use thread-local scratch. Beware hidden state: memoization caches on nodes must be concurrent or per-context. This "immutable structure + mutable context" split is the crux of scaling Interpreter.

</details>

<details>
<summary><strong>SP3: Design AST caching/memoization for a rule engine evaluating millions of rows against thousands of rules.</strong></summary>

Two levels. **Compilation cache:** map rule-string → compiled AST (or compiled closure) in a bounded LRU/`Caffeine` cache, since user-authored rules can be unbounded — otherwise unbounded strings leak memory. **Evaluation:** the AST is immutable and shared; each row provides a context. Optimize further by (a) **common-subexpression elimination** during compile (share identical sub-trees, aided by Flyweight for terminals), (b) **short-circuit ordering** — evaluate cheap/high-selectivity predicates first, (c) **memoizing pure sub-expressions** whose inputs are constant across rows, and (d) batching/vectorizing when the same rule runs over a columnar dataset. Track cache hit rates and eval latency as SLIs; invalidate compiled entries when a rule version changes.

</details>

<details>
<summary><strong>SP4: You must evaluate untrusted expressions from customers. How do you sandbox it?</strong></summary>

Treat it as an attack surface. **Grammar minimization:** design the DSL to *only* allow what's needed — arithmetic/boolean/comparisons over a fixed set of fields; no method invocation, no reflection, no I/O, no loops. **Resource limits:** cap input length, bound parser recursion/nesting depth (reject deeply nested input to prevent stack overflow), enforce evaluation timeouts and step/opcode budgets. **Isolation:** run evaluation with no ambient authority; if using an off-the-shelf engine (e.g., SpEL) restrict the `EvaluationContext` (SimpleEvaluationContext, no type references). **Validation:** parse-and-validate at authoring time, store the compiled/validated AST, and never `eval` raw strings at request time. **Observability:** log/limit per-tenant evaluation cost. Prefer a purpose-built safe grammar over a general one; general expression languages are notorious RCE vectors.

</details>

<details>
<summary><strong>SP5: How does Visitor complement Interpreter when you need many operations over the AST, and what's the extensibility trade-off?</strong></summary>

Interpreter puts one operation (`interpret`) on each node — great when operations are few and node types churn. But real language tooling needs *many* passes over one AST: evaluate, pretty-print, type-check, constant-fold/optimize, serialize. Cramming all of these as methods on every node bloats classes and couples unrelated concerns. **Visitor** externalizes each operation into its own visitor class; nodes only expose `accept(visitor)`. This inverts the extensibility axis (the "expression problem"): Interpreter makes **adding node types** easy but **adding operations** hard; Visitor makes **adding operations** easy but **adding node types** hard (every visitor must handle the new node). Staff-level answer: pick based on which axis changes more; compilers freeze the node set and add many passes, so they favor Visitor — often *on top of* an ANTLR-generated tree, with Interpreter defining the AST shape.

</details>

<details>
<summary><strong>SP6: Scaling to large grammars — architectural options and how Interpreter fits.</strong></summary>

For large/evolving grammars, hand-written Interpreter nodes + recursive-descent parser don't scale (class explosion, fragile parser, weak error recovery). Options: (1) **Parser generator (ANTLR/JavaCC)** — declarative grammar, generated lexer/parser, listener/visitor APIs; you layer semantics via Visitor. (2) **Parser combinators** for medium grammars. (3) **Two-phase: parse → typed IR → interpret/compile**, so front-end and back-end evolve independently. Interpreter's role shrinks to the *semantics* layer over a professionally-parsed tree. Also introduce grammar versioning, exhaustive conformance tests, and a golden-file test suite. The senior instinct: use the pattern's *concepts* (AST, terminals/nonterminals, context) but delegate parsing to tooling and consider compilation for performance-critical evaluation.

</details>

---

## ⚡ Quick Revision

**One-liner:** Interpreter models each rule of a *simple* grammar as a class, builds sentences into an abstract syntax tree, and evaluates them via a recursive `interpret(context)` — it's Composite + recursion.

**The whole pattern in a paragraph:** You have a small, stable language whose sentences you evaluate repeatedly (rule engines, calculators, filter DSLs, regexes). Define an `AbstractExpression` with `interpret(Context)`. **Terminal** expressions (literals, variables) are leaves; **nonterminal** expressions (AND/OR/NOT, +/-/*//) are composites holding sub-expressions. A **separate parser** (hand-written recursive descent, or ANTLR) tokenizes the input and builds the AST *once*, encoding operator precedence via one method per precedence level. You then call `interpret(context)` on the root, which recurses down the tree, terminals reading from the context and nonterminals combining children's results. Keep the AST immutable and stateless so it's thread-safe and reusable; put varying state in the context. Cache/compile ASTs ("parse once, interpret many") for performance, and for hot paths compile to closures/bytecode instead of tree-walking. The pattern only suits *simple* grammars — for complex or growing ones, use a parser generator; for many operations over the AST, pair it with Visitor; share immutable terminals via Flyweight; and sandbox carefully when evaluating untrusted input.

**Top 5 answers to memorize:**
1. *What is it?* → "Define a grammar's rules as classes and interpret sentences by recursing over an AST of those classes — Composite plus an `interpret` operation."
2. *Relation to Composite?* → "The AST *is* a Composite; `interpret()` is its recursive operation."
3. *Parsing?* → "Separate concern — parse once to build the AST, interpret many times; the pattern doesn't prescribe the parser."
4. *When NOT to use / ANTLR?* → "Only for simple, stable grammars; anything complex or growing → ANTLR/JavaCC."
5. *Performance?* → "Tree-walking is slow; cache the compiled AST and, for hot paths, compile to closures/bytecode."

**Trigger words** (hear these → think Interpreter): "grammar," "evaluate an expression," "rule engine / business rules," "mini language / DSL," "boolean/arithmetic expression evaluator," "parse and evaluate," "abstract syntax tree," "operator precedence," "filter/query language," "feature-flag expression," "formula," "regex engine," "SpEL."

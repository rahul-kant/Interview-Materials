# 🎨 Design Patterns - Complete Guide for FAANG Interviews

## 📚 Overview

This section contains **comprehensive coverage of all major design patterns** organized by category with:
- ✅ Real-world examples in Java
- ✅ FAANG interview questions with answers
- ✅ Practical use cases
- ✅ When to use each pattern
- ✅ GoF (Gang of Four) principles
- ✅ Category-level interview FAQs

---

## 📁 New Organized Structure

Patterns are now organized into **3 main categories**:

```
02-design-patterns/
├── creational/          # Object creation patterns (5)
│   ├── INTERVIEW_FAQ.md (FAANG Q&A + GoF principles)
│   ├── 01-singleton/
│   ├── 02-factory-method/
│   ├── 03-abstract-factory/
│   ├── 04-builder/
│   └── 05-prototype/
├── structural/          # Object composition patterns (7)
│   ├── INTERVIEW_FAQ.md (FAANG Q&A + GoF principles)
│   ├── 01-adapter/
│   ├── 02-decorator/
│   ├── 03-proxy/
│   ├── 04-composite/
│   ├── 05-bridge/
│   ├── 06-facade/
│   └── 07-flyweight/
└── behavioral/          # Object interaction patterns (11)
    ├── INTERVIEW_FAQ.md (FAANG Q&A + GoF principles)
    ├── 01-strategy/
    ├── 02-observer/
    ├── 03-command/
    ├── 04-state/
    ├── 05-template-method/
    ├── 06-iterator/
    ├── 07-chain-of-responsibility/
    ├── 08-mediator/
    ├── 09-memento/
    ├── 10-visitor/
    └── 11-interpreter/
```

---

## 🎯 Pattern Categories

### 📦 Creational Patterns (5 patterns)
**Focus:** Object creation mechanisms

| Pattern | Rating | Description | Interview Frequency |
|---------|--------|-------------|---------------------|
| [Singleton](creational/01-singleton/) | ⭐⭐⭐ | Only one instance | Very High |
| [Factory Method](creational/02-factory-method/) | ⭐⭐⭐ | Defer creation to subclasses | Very High |
| [Abstract Factory](creational/03-abstract-factory/) | ⭐⭐ | Family of related objects | High |
| [Builder](creational/04-builder/) | ⭐⭐ | Construct complex objects step-by-step | High |
| [Prototype](creational/05-prototype/) | ⭐ | Clone existing objects | Medium |

**📖 [Creational Patterns - Interview FAQ](creational/INTERVIEW_FAQ.md)**
- Most asked questions for FAANG
- Gang of Four principles
- Pattern comparisons
- Real interview scenarios

---

### 🏗️ Structural Patterns (7 patterns)
**Focus:** Class and object composition

| Pattern | Rating | Description | Interview Frequency |
|---------|--------|-------------|---------------------|
| [Adapter](structural/01-adapter/) | ⭐⭐ | Convert interface to another | High |
| [Decorator](structural/02-decorator/) | ⭐⭐⭐ | Add behavior dynamically | Very High |
| [Proxy](structural/03-proxy/) | ⭐⭐ | Control access to object | High |
| [Composite](structural/04-composite/) | ⭐⭐ | Tree structure (part-whole) | High |
| [Bridge](structural/05-bridge/) | ⭐ | Separate abstraction from implementation | Medium |
| [Facade](structural/06-facade/) | ⭐⭐ | Simplified interface | High |
| [Flyweight](structural/07-flyweight/) | ⭐ | Share objects efficiently | Medium |

**📖 [Structural Patterns - Interview FAQ](structural/INTERVIEW_FAQ.md)**
- Most asked questions for FAANG
- Gang of Four principles
- Adapter vs Decorator vs Proxy
- Pattern recognition tips

---

### 🎭 Behavioral Patterns (11 patterns)
**Focus:** Communication between objects

| Pattern | Rating | Description | Interview Frequency |
|---------|--------|-------------|---------------------|
| [Strategy](behavioral/01-strategy/) | ⭐⭐⭐ | Family of algorithms | Very High |
| [Observer](behavioral/02-observer/) | ⭐⭐⭐ | Pub-Sub notification | Very High |
| [Command](behavioral/03-command/) | ⭐⭐ | Encapsulate requests | High |
| [State](behavioral/04-state/) | ⭐⭐ | Change behavior based on state | High |
| [Template Method](behavioral/05-template-method/) | ⭐⭐ | Define algorithm skeleton | High |
| [Iterator](behavioral/06-iterator/) | ⭐ | Sequential access | Medium |
| [Chain of Responsibility](behavioral/07-chain-of-responsibility/) | ⭐ | Pass request along chain | Medium |
| [Mediator](behavioral/08-mediator/) | ⭐ | Centralized communication | Medium |
| [Memento](behavioral/09-memento/) | ⭐ | Save/restore state | Low |
| [Visitor](behavioral/10-visitor/) | ⭐ | Add operations without changing classes | Low |
| [Interpreter](behavioral/11-interpreter/) | ⭐ | Language grammar | Low |

**📖 [Behavioral Patterns - Interview FAQ](behavioral/INTERVIEW_FAQ.md)**
- Most asked questions for FAANG
- Gang of Four principles
- Strategy vs State
- Observer implementations

---

## 📊 Interview Frequency Guide

### Must Know (Asked 80%+ of time) ⭐⭐⭐
**Creational:**
- Singleton (thread safety focus)
- Factory Method
- Builder

**Structural:**
- Decorator (Java I/O example)

**Behavioral:**
- Strategy
- Observer

### Should Know (Asked 50%+ of time) ⭐⭐
**Creational:**
- Abstract Factory

**Structural:**
- Adapter
- Proxy
- Composite
- Facade

**Behavioral:**
- Command
- State
- Template Method

### Good to Know (Asked 20%+ of time) ⭐
**Creational:**
- Prototype

**Structural:**
- Bridge
- Flyweight

**Behavioral:**
- Iterator
- Chain of Responsibility
- Mediator

---

## 🎯 Learning Path

### Week 1: Essential Patterns (Must Know)
**Days 1-2:** Creational
- Singleton (all implementations + thread safety)
- Factory Method

**Days 3-4:** Behavioral
- Strategy
- Observer

**Days 5-6:** Structural
- Decorator (Java I/O focus)

**Day 7:** Practice & Review

### Week 2: Important Patterns (Should Know)
**Days 1-2:** Creational
- Builder
- Abstract Factory

**Days 3-4:** Structural
- Adapter
- Proxy

**Days 5-6:** Behavioral
- Command
- State

**Day 7:** Practice & Review

### Week 3: Additional Patterns
**Days 1-2:** Structural
- Composite, Facade

**Days 3-4:** Behavioral
- Template Method, Iterator

**Days 5-6:** Review all patterns

**Day 7:** Mock interviews

---

## 💡 Interview Tips

### How Interviewers Test Patterns

**1. Recognition:**
```
"What pattern would you use for...?"
"How would you design...?"
```

**2. Implementation:**
```
"Implement a Singleton"
"Show me the Strategy pattern"
```

**3. Comparison:**
```
"Strategy vs State?"
"Adapter vs Decorator vs Proxy?"
```

**4. Real-World:**
```
"Where is Observer used?"
"Examples of Decorator in Java?"
```

---

## 🎓 FAANG-Specific Tips

### Google
**Focus on:** Strategy, Observer, Factory, Decorator
**Emphasize:** Clean code, extensibility, SOLID principles
**Be ready for:** Pattern comparisons, variations

### Amazon
**Focus on:** Singleton, Builder, Command, State
**Emphasize:** OOP principles, thread safety, scalability
**Be ready for:** Real-world scenarios, system design tie-ins

### Microsoft
**Focus on:** Factory, Decorator, State, Observer
**Emphasize:** Design principles, enterprise patterns
**Be ready for:** Implementation details, best practices

### Meta
**Focus on:** Observer, Strategy, Adapter, Proxy
**Emphasize:** Scalability, performance
**Be ready for:** System integration, large-scale systems

---

## 📖 Study Resources

### Books
1. **"Design Patterns"** - Gang of Four (GoF)
2. **"Head First Design Patterns"** - Freeman & Freeman
3. **"Effective Java"** - Joshua Bloch

### Online
- Refactoring.Guru
- SourceMaking
- GeeksforGeeks Design Patterns

### Video
- Derek Banas Design Patterns playlist
- Christopher Okhravi SOLID & Patterns

---

## ✅ Self-Assessment Checklist

### For Each Pattern:
- [ ] Understand the intent and problem it solves
- [ ] Know when to use (and when NOT to use)
- [ ] Can implement from scratch
- [ ] Know pros and cons
- [ ] Can give 2-3 real examples
- [ ] Understand variations/alternatives
- [ ] Can compare with similar patterns

### Category Understanding:
- [ ] Know all patterns in Creational category
- [ ] Know all patterns in Structural category
- [ ] Know key patterns in Behavioral category
- [ ] Can explain category differences
- [ ] Know GoF principles for each category

---

## 📊 Pattern Relationships

### Similar Patterns (Often Confused)

**Creational:**
- Singleton vs Static Class
- Factory Method vs Abstract Factory
- Builder vs Telescoping Constructor

**Structural:**
- Adapter vs Bridge vs Facade
- Decorator vs Proxy
- Composite vs Decorator

**Behavioral:**
- Strategy vs State
- Strategy vs Template Method
- Command vs Strategy

---

## 🔗 Quick Navigation

### By Category:
- 📦 [Creational Patterns](creational/) - Object creation
- 🏗️ [Structural Patterns](structural/) - Object composition
- 🎭 [Behavioral Patterns](behavioral/) - Object interaction

### Interview Preparation:
- 📖 [Creational FAQ](creational/INTERVIEW_FAQ.md) - FAANG Q&A + GoF
- 📖 [Structural FAQ](structural/INTERVIEW_FAQ.md) - FAANG Q&A + GoF
- 📖 [Behavioral FAQ](behavioral/INTERVIEW_FAQ.md) - FAANG Q&A + GoF

### Main Repository:
- [← Fundamentals](../01-fundamentals/)
- [Real-World Examples →](../03-real-world-examples/)
- [Main README →](../README.md)

---

## 🎯 Key Takeaways

### Design Pattern Principles:
1. **Program to interface, not implementation**
2. **Favor composition over inheritance**
3. **Encapsulate what varies**
4. **Depend on abstractions, not concretions**
5. **Open/Closed Principle** - Open for extension, closed for modification

### When to Use Patterns:
✅ **DO use patterns when:**
- Problem is recurring
- Solution is proven
- Increases flexibility
- Improves maintainability

❌ **DON'T use patterns when:**
- Simple solution exists
- Over-engineering the problem
- Performance is critical
- Team unfamiliar with pattern

---

## 📈 Success Metrics

### Pattern Mastery Levels:

**Level 1: Recognition** (Week 1)
- Can recognize patterns in code
- Understand basic intent
- Know when to use

**Level 2: Implementation** (Week 2)
- Can implement from scratch
- Understand variations
- Know trade-offs

**Level 3: Expertise** (Week 3+)
- Can choose best pattern
- Combine patterns effectively
- Explain to others
- Interview-ready

---

## 🎊 Congratulations!

You now have access to:
- ✅ **All 23 GoF Design Patterns** (5 Creational + 7 Structural + 11 Behavioral)
- ✅ **3 Category-level FAQs** with FAANG questions & GoF principles
- ✅ **Complete code examples** for each pattern
- ✅ **Interview scenarios** and solutions
- ✅ **Organized structure** for easy learning
- ✅ **Pattern comparisons** and best practices

**You're ready to ace design pattern questions in FAANG interviews!** 🚀

---

*Made with ❤️ for FAANG Interview Preparation*

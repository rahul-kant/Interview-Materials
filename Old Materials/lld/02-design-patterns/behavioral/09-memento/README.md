# Memento Pattern ⭐

## 📋 Intent

**Without violating encapsulation, capture and externalize an object's internal state so that the object can be restored to this state later.**

---

## 💻 Implementation

```java
// Memento
class EditorMemento {
    private final String content;
    
    public EditorMemento(String content) {
        this.content = content;
    }
    
    public String getContent() {
        return content;
    }
}

// Originator
class Editor {
    private String content;
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getContent() {
        return content;
    }
    
    public EditorMemento save() {
        return new EditorMemento(content);
    }
    
    public void restore(EditorMemento memento) {
        content = memento.getContent();
    }
}

// Caretaker
class History {
    private Stack<EditorMemento> mementos = new Stack<>();
    
    public void push(EditorMemento memento) {
        mementos.push(memento);
    }
    
    public EditorMemento pop() {
        return mementos.pop();
    }
}

// Usage
Editor editor = new Editor();
History history = new History();

editor.setContent("Version 1");
history.push(editor.save());

editor.setContent("Version 2");
history.push(editor.save());

editor.setContent("Version 3");

// Undo
editor.restore(history.pop()); // Back to Version 2
```

---

## ✅ When to Use

- **Undo/Redo** functionality
- **Snapshot** of object state
- **Preserve encapsulation** while saving state

---

## 🎯 Key Takeaways

1. **Saves object state** externally
2. **Preserves encapsulation**
3. **Enables undo/redo**
4. **Snapshot pattern**

---

**Next Pattern:** [Visitor →](../22-visitor/)

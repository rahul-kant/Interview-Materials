# Composite Pattern ⭐⭐

## 📋 Intent

**Compose objects into tree structures to represent part-whole hierarchies. Composite lets clients treat individual objects and compositions uniformly.**

---

## 🎯 Problem

- Need to represent part-whole hierarchies
- Want to treat individual and composite objects uniformly
- Building tree structures
- Need recursive composition

**Example Scenarios:**
- File system (files and folders)
- Organization hierarchy
- UI components (container with components)
- Graphics (shapes and groups)

---

## 💻 Implementation

```java
// Component
interface FileSystemItem {
    void display(String indent);
    int getSize();
}

// Leaf
class File implements FileSystemItem {
    private String name;
    private int size;
    
    public File(String name, int size) {
        this.name = name;
        this.size = size;
    }
    
    public void display(String indent) {
        System.out.println(indent + "File: " + name + " (" + size + "KB)");
    }
    
    public int getSize() {
        return size;
    }
}

// Composite
class Folder implements FileSystemItem {
    private String name;
    private List<FileSystemItem> items = new ArrayList<>();
    
    public Folder(String name) {
        this.name = name;
    }
    
    public void add(FileSystemItem item) {
        items.add(item);
    }
    
    public void display(String indent) {
        System.out.println(indent + "Folder: " + name);
        for (FileSystemItem item : items) {
            item.display(indent + "  ");
        }
    }
    
    public int getSize() {
        int total = 0;
        for (FileSystemItem item : items) {
            total += item.getSize();
        }
        return total;
    }
}

// Usage
Folder root = new Folder("root");
Folder docs = new Folder("documents");
docs.add(new File("resume.pdf", 100));
docs.add(new File("cover-letter.pdf", 50));
root.add(docs);
root.add(new File("photo.jpg", 200));
root.display("");
// Total size: 350KB
```

---

## ✅ When to Use

- **Tree structures** needed
- **Treat uniformly** - same interface for leaf and composite
- **Hierarchies** - part-whole relationships

---

## 🎯 Key Takeaways

1. **Tree structure** - recursive composition
2. **Uniform treatment** - leaf and composite same interface
3. **Add/remove** - dynamic tree modification

---

**Next Pattern:** [Bridge →](../05-bridge/)

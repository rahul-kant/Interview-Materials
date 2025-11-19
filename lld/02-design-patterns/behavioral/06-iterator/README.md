# Iterator Pattern ⭐

## 📋 Intent

**Provide a way to access the elements of an aggregate object sequentially without exposing its underlying representation.**

---

## 💻 Implementation

```java
interface Iterator<T> {
    boolean hasNext();
    T next();
}

interface Collection<T> {
    Iterator<T> createIterator();
}

class BookCollection implements Collection<String> {
    private List<String> books = new ArrayList<>();
    
    public void addBook(String book) {
        books.add(book);
    }
    
    public Iterator<String> createIterator() {
        return new BookIterator();
    }
    
    private class BookIterator implements Iterator<String> {
        private int index = 0;
        
        public boolean hasNext() {
            return index < books.size();
        }
        
        public String next() {
            return books.get(index++);
        }
    }
}

// Usage
BookCollection collection = new BookCollection();
collection.addBook("Book 1");
collection.addBook("Book 2");

Iterator<String> iterator = collection.createIterator();
while (iterator.hasNext()) {
    System.out.println(iterator.next());
}
```

---

## 📚 Where It's Used

- `java.util.Iterator`
- `java.util.Enumeration`
- Enhanced for loop

---

**Next Pattern:** [Chain of Responsibility →](../19-chain-of-responsibility/)

# Java Deep Dive - Advanced Topics for Tech Leads

## Overview
As a senior Java engineer with 14 years of experience, you likely know the basics. This guide focuses on advanced topics and interview questions specific to senior/lead positions at FAANG companies.

---

## 1. Java Memory Management & JVM Internals

### 1.1 Memory Model

**Heap Memory:**
```
Young Generation:
├── Eden Space (new objects)
├── Survivor Space S0
└── Survivor Space S1

Old Generation (Tenured):
└── Long-lived objects

Metaspace (Java 8+):
└── Class metadata (replaced PermGen)
```

**Stack Memory:**
- Thread-specific
- Local variables, method calls
- Fixed size per thread

### 1.2 Garbage Collection

**GC Algorithms:**

#### Serial GC (-XX:+UseSerialGC)
```
- Single-threaded
- Stop-the-world
- Good for: Small applications, single-CPU
```

#### Parallel GC (-XX:+UseParallelGC)
```
- Multi-threaded
- Throughput-focused
- Good for: Batch processing, high throughput
```

#### CMS - Concurrent Mark Sweep (-XX:+UseConcMarkSweepGC)
```
- Low pause times
- Concurrent marking
- Fragmentation issues
- Deprecated in Java 14
```

#### G1 GC (-XX:+UseG1GC) - Default in Java 9+
```
- Region-based
- Predictable pause times
- Good for: Large heaps, low latency
- Target: -XX:MaxGCPauseMillis=200
```

#### ZGC (-XX:+UseZGC) - Java 11+
```
- Ultra-low latency (< 10ms pauses)
- Scalable (TB heaps)
- Concurrent
- Good for: Latency-critical applications
```

#### Shenandoah GC
```
- Similar to ZGC
- Even lower pause times
- Good for: Real-time systems
```

### 1.3 GC Tuning Parameters

```bash
# Heap Size
-Xms2g                    # Initial heap
-Xmx4g                    # Max heap

# Young Generation
-Xmn1g                    # Young gen size
-XX:NewRatio=2            # Old/Young ratio

# GC Selection
-XX:+UseG1GC              # Use G1
-XX:MaxGCPauseMillis=200  # Target pause time

# GC Logging
-Xlog:gc*:file=gc.log     # Java 9+
-XX:+PrintGCDetails       # Java 8

# Metaspace
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m
```

### 1.4 Memory Leaks - Common Causes

```java
// 1. Static Collections
public class Cache {
    private static Map<String, Object> cache = new HashMap<>();
    // Objects never removed - memory leak
}

// 2. Unclosed Resources
public void readFile(String path) {
    BufferedReader reader = new BufferedReader(new FileReader(path));
    // If exception occurs, reader not closed
}

// Fix: Use try-with-resources
public void readFile(String path) throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
        // Reader auto-closed
    }
}

// 3. Inner Classes holding outer reference
public class Outer {
    private byte[] data = new byte[1000000];
    
    public Inner getInner() {
        return new Inner(); // Inner holds reference to Outer
    }
    
    class Inner {
        // Implicitly holds reference to Outer
    }
}

// Fix: Use static inner class
static class Inner {
    // No reference to Outer
}

// 4. ThreadLocal not cleaned
ThreadLocal<byte[]> threadLocal = new ThreadLocal<>();
// If not removed, causes leak in thread pools
threadLocal.remove(); // Always remove
```

---

## 2. Java Concurrency & Multithreading

### 2.1 Thread Creation

```java
// Method 1: Extend Thread
class MyThread extends Thread {
    public void run() {
        System.out.println("Thread running");
    }
}

// Method 2: Implement Runnable (Preferred)
class MyRunnable implements Runnable {
    public void run() {
        System.out.println("Runnable running");
    }
}

// Method 3: Lambda (Java 8+)
Thread t = new Thread(() -> System.out.println("Lambda thread"));

// Method 4: Callable (returns value)
Callable<Integer> task = () -> {
    return 42;
};
```

### 2.2 Thread Synchronization

```java
// Synchronized method
public synchronized void increment() {
    count++;
}

// Synchronized block
public void increment() {
    synchronized(this) {
        count++;
    }
}

// Static synchronization (class-level lock)
public static synchronized void method() {
    // Uses Class object as lock
}

// Double-checked locking (Singleton)
public class Singleton {
    private static volatile Singleton instance;
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

### 2.3 java.util.concurrent Package

#### Lock Interface
```java
Lock lock = new ReentrantLock();

try {
    lock.lock();
    // Critical section
} finally {
    lock.unlock(); // Always unlock in finally
}

// Try lock with timeout
if (lock.tryLock(1, TimeUnit.SECONDS)) {
    try {
        // Got lock
    } finally {
        lock.unlock();
    }
}

// ReadWriteLock
ReadWriteLock rwLock = new ReentrantReadWriteLock();
Lock readLock = rwLock.readLock();
Lock writeLock = rwLock.writeLock();
```

#### Atomic Classes
```java
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();        // Atomic increment
counter.compareAndSet(10, 20);    // CAS operation

AtomicReference<String> ref = new AtomicReference<>("initial");
ref.updateAndGet(s -> s + " updated");
```

#### CountDownLatch
```java
CountDownLatch latch = new CountDownLatch(3);

// Worker threads
new Thread(() -> {
    // Do work
    latch.countDown();
}).start();

// Main thread waits
latch.await(); // Blocks until count reaches 0
```

#### CyclicBarrier
```java
CyclicBarrier barrier = new CyclicBarrier(3, () -> {
    System.out.println("All threads reached barrier");
});

new Thread(() -> {
    // Do work
    barrier.await(); // Wait for others
}).start();
```

#### Semaphore
```java
Semaphore semaphore = new Semaphore(3); // 3 permits

semaphore.acquire(); // Get permit
try {
    // Access shared resource
} finally {
    semaphore.release(); // Release permit
}
```

### 2.4 ExecutorService

```java
// Fixed thread pool
ExecutorService executor = Executors.newFixedThreadPool(10);

// Submit tasks
Future<Integer> future = executor.submit(() -> {
    return 42;
});

// Get result
int result = future.get(); // Blocks until result available

// Shutdown
executor.shutdown();
executor.awaitTermination(1, TimeUnit.MINUTES);

// Custom thread pool
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                      // core pool size
    10,                     // max pool size
    60L,                    // keep alive time
    TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100),  // work queue
    new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
);
```

### 2.5 CompletableFuture (Java 8+)

```java
// Async computation
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "Hello";
});

// Chain operations
future.thenApply(s -> s + " World")
      .thenAccept(System.out::println)
      .exceptionally(ex -> {
          System.err.println(ex.getMessage());
          return null;
      });

// Combine futures
CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(() -> 10);
CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(() -> 20);

CompletableFuture<Integer> combined = f1.thenCombine(f2, (a, b) -> a + b);

// Wait for all
CompletableFuture.allOf(f1, f2).join();

// Wait for any
CompletableFuture.anyOf(f1, f2).join();
```

### 2.6 Thread-Safe Collections

```java
// Synchronized wrappers
List<String> syncList = Collections.synchronizedList(new ArrayList<>());
Map<String, String> syncMap = Collections.synchronizedMap(new HashMap<>());

// Concurrent collections (better performance)
ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
BlockingQueue<String> queue = new LinkedBlockingQueue<>();

// ConcurrentHashMap operations
map.putIfAbsent("key", "value");
map.computeIfAbsent("key", k -> computeValue(k));
map.merge("key", 1, Integer::sum); // Atomic increment
```

### 2.7 Common Concurrency Issues

```java
// 1. Race Condition
class Counter {
    private int count = 0;
    
    public void increment() {
        count++; // Not atomic! (read-modify-write)
    }
}
// Fix: Use AtomicInteger or synchronized

// 2. Deadlock
synchronized(lock1) {
    synchronized(lock2) {
        // Thread 1
    }
}
// Thread 2 locks in reverse order → Deadlock
// Fix: Always acquire locks in same order

// 3. Thread Starvation
// Low priority threads never get CPU
// Fix: Use fair locks, avoid priority manipulation

// 4. Livelock
// Threads keep changing state in response to each other
// Fix: Add randomization, backoff strategies
```

---

## 3. Java Collections Framework

### 3.1 Collections Complexity

| Collection | Add | Remove | Get | Contains | Next |
|-----------|-----|--------|-----|----------|------|
| ArrayList | O(1) | O(n) | O(1) | O(n) | O(1) |
| LinkedList | O(1) | O(1) | O(n) | O(n) | O(1) |
| HashSet | O(1) | O(1) | O(1) | O(1) | O(h/n) |
| TreeSet | O(log n) | O(log n) | O(log n) | O(log n) | O(log n) |
| HashMap | O(1) | O(1) | O(1) | O(1) | O(h/n) |
| TreeMap | O(log n) | O(log n) | O(log n) | O(log n) | O(log n) |

### 3.2 HashMap Internals

```java
// Java 7: Array + Linked List
// Java 8+: Array + Linked List/Red-Black Tree

// Internal structure
class Node<K,V> {
    final int hash;
    final K key;
    V value;
    Node<K,V> next;
}

// Hash function
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}

// Index calculation
index = (n - 1) & hash

// Tree conversion threshold (Java 8+)
static final int TREEIFY_THRESHOLD = 8;
static final int UNTREEIFY_THRESHOLD = 6;

// Load factor
static final float DEFAULT_LOAD_FACTOR = 0.75f;

// When to resize
if (size > capacity * loadFactor) {
    resize(); // Doubles capacity
}
```

### 3.3 Advanced Collection Operations

```java
// List operations
List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);

// Java 8+ Stream operations
list.stream()
    .filter(n -> n % 2 == 0)
    .map(n -> n * 2)
    .collect(Collectors.toList());

// Sorting
Collections.sort(list);
list.sort(Comparator.naturalOrder());
list.sort((a, b) -> a - b);

// Map operations
Map<String, Integer> map = new HashMap<>();
map.put("a", 1);
map.putIfAbsent("b", 2);
map.computeIfAbsent("c", k -> k.length());
map.merge("a", 1, Integer::sum); // Increment

// Map iteration
map.forEach((k, v) -> System.out.println(k + ":" + v));

// Collectors
Map<Integer, List<String>> grouped = list.stream()
    .collect(Collectors.groupingBy(String::length));

Map<String, Integer> counted = list.stream()
    .collect(Collectors.toMap(
        Function.identity(),
        s -> 1,
        Integer::sum
    ));
```

---

## 4. Java 8+ Features

### 4.1 Lambda Expressions

```java
// Old way
Runnable r = new Runnable() {
    @Override
    public void run() {
        System.out.println("Running");
    }
};

// Lambda
Runnable r = () -> System.out.println("Running");

// With parameters
Comparator<String> comp = (a, b) -> a.compareTo(b);

// Method reference
list.forEach(System.out::println);
list.sort(String::compareTo);
```

### 4.2 Stream API

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

// Intermediate operations (lazy)
numbers.stream()
    .filter(n -> n % 2 == 0)      // Filter
    .map(n -> n * 2)               // Transform
    .distinct()                     // Remove duplicates
    .sorted()                       // Sort
    .limit(10)                      // Take first 10
    .skip(2);                       // Skip first 2

// Terminal operations (trigger execution)
numbers.stream().forEach(System.out::println);
long count = numbers.stream().count();
Optional<Integer> max = numbers.stream().max(Integer::compare);
Optional<Integer> first = numbers.stream().findFirst();
boolean anyMatch = numbers.stream().anyMatch(n -> n > 5);

// Collectors
List<Integer> list = stream.collect(Collectors.toList());
Set<Integer> set = stream.collect(Collectors.toSet());
String joined = stream.map(String::valueOf)
                      .collect(Collectors.joining(", "));

// Reduce
int sum = numbers.stream().reduce(0, Integer::sum);
Optional<Integer> product = numbers.stream()
                                   .reduce((a, b) -> a * b);
```

### 4.3 Optional

```java
Optional<String> optional = Optional.of("value");
Optional<String> empty = Optional.empty();
Optional<String> nullable = Optional.ofNullable(null);

// Check presence
if (optional.isPresent()) {
    String value = optional.get();
}

// Modern approach
optional.ifPresent(System.out::println);
optional.ifPresentOrElse(
    System.out::println,
    () -> System.out.println("Empty")
);

// Default values
String value = optional.orElse("default");
String value = optional.orElseGet(() -> "computed default");
String value = optional.orElseThrow(() -> new Exception());

// Transform
Optional<Integer> length = optional.map(String::length);
Optional<String> upper = optional.flatMap(s -> Optional.of(s.toUpperCase()));

// Filter
Optional<String> filtered = optional.filter(s -> s.length() > 5);
```

### 4.4 Default Methods in Interfaces

```java
interface MyInterface {
    // Abstract method
    void abstractMethod();
    
    // Default method
    default void defaultMethod() {
        System.out.println("Default implementation");
    }
    
    // Static method
    static void staticMethod() {
        System.out.println("Static method");
    }
}
```

### 4.5 Date/Time API (java.time)

```java
// Current date/time
LocalDate today = LocalDate.now();
LocalTime time = LocalTime.now();
LocalDateTime dateTime = LocalDateTime.now();
ZonedDateTime zoned = ZonedDateTime.now();

// Create specific date
LocalDate date = LocalDate.of(2025, 6, 11);
LocalDate parsed = LocalDate.parse("2025-06-11");

// Operations
LocalDate tomorrow = today.plusDays(1);
LocalDate nextWeek = today.plus(1, ChronoUnit.WEEKS);
long daysBetween = ChronoUnit.DAYS.between(date1, date2);

// Formatting
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
String formatted = date.format(formatter);
LocalDate parsed = LocalDate.parse("11/06/2025", formatter);

// Time zones
ZoneId zoneId = ZoneId.of("America/New_York");
ZonedDateTime nyTime = ZonedDateTime.now(zoneId);
```

---

## 5. Design Patterns in Java

### 5.1 Singleton

```java
// Thread-safe singleton
public class Singleton {
    private static volatile Singleton instance;
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}

// Enum singleton (best approach)
public enum Singleton {
    INSTANCE;
    
    public void doSomething() {
        // ...
    }
}
```

### 5.2 Factory Pattern

```java
interface Animal {
    void speak();
}

class Dog implements Animal {
    public void speak() { System.out.println("Woof"); }
}

class Cat implements Animal {
    public void speak() { System.out.println("Meow"); }
}

class AnimalFactory {
    public static Animal createAnimal(String type) {
        switch (type) {
            case "dog": return new Dog();
            case "cat": return new Cat();
            default: throw new IllegalArgumentException();
        }
    }
}
```

### 5.3 Builder Pattern

```java
public class User {
    private final String name;      // Required
    private final String email;     // Required
    private final int age;          // Optional
    private final String phone;     // Optional
    
    private User(Builder builder) {
        this.name = builder.name;
        this.email = builder.email;
        this.age = builder.age;
        this.phone = builder.phone;
    }
    
    public static class Builder {
        private final String name;
        private final String email;
        private int age;
        private String phone;
        
        public Builder(String name, String email) {
            this.name = name;
            this.email = email;
        }
        
        public Builder age(int age) {
            this.age = age;
            return this;
        }
        
        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }
        
        public User build() {
            return new User(this);
        }
    }
}

// Usage
User user = new User.Builder("John", "john@example.com")
                .age(30)
                .phone("123-456-7890")
                .build();
```

### 5.4 Observer Pattern

```java
interface Observer {
    void update(String message);
}

class Subject {
    private List<Observer> observers = new ArrayList<>();
    
    public void attach(Observer observer) {
        observers.add(observer);
    }
    
    public void notifyObservers(String message) {
        for (Observer observer : observers) {
            observer.update(message);
        }
    }
}
```

---

## 6. Spring Boot Essentials

### 6.1 Dependency Injection

```java
@Component
public class UserService {
    private final UserRepository userRepository;
    
    // Constructor injection (recommended)
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// Configuration
@Configuration
public class AppConfig {
    @Bean
    public UserService userService() {
        return new UserService(userRepository());
    }
}
```

### 6.2 REST Controllers

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public List<User> getAllUsers() {
        return userService.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody @Valid User user) {
        User created = userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

### 6.3 Exception Handling

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
        ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

---

## 7. Performance Optimization

### 7.1 String Optimization

```java
// Bad: Creates many objects
String s = "";
for (int i = 0; i < 1000; i++) {
    s += i; // Creates new String each time
}

// Good: Use StringBuilder
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) {
    sb.append(i);
}
String s = sb.toString();

// String interning
String s1 = new String("hello").intern();
String s2 = "hello";
// s1 == s2 is true
```

### 7.2 Collection Sizing

```java
// Provide initial capacity if known
List<String> list = new ArrayList<>(1000);
Map<String, String> map = new HashMap<>(1000);

// This avoids resizing operations
```

### 7.3 Lazy Initialization

```java
public class Expensive {
    private static class Holder {
        private static final Expensive INSTANCE = new Expensive();
    }
    
    public static Expensive getInstance() {
        return Holder.INSTANCE;
    }
}
```

---

## 8. Common Interview Questions

### 8.1 What's the difference between == and equals()?

```
== : Reference comparison (same object?)
equals() : Content comparison (same value?)

String s1 = new String("hello");
String s2 = new String("hello");
s1 == s2        // false (different objects)
s1.equals(s2)   // true (same content)
```

### 8.2 Explain HashMap collision resolution

```
Java 7: Linked list (O(n) worst case)
Java 8+: Linked list → Red-Black Tree when > 8 entries
         Tree → Linked list when < 6 entries
         This improves worst case to O(log n)
```

### 8.3 What is volatile keyword?

```java
private volatile boolean flag = true;

// Volatile guarantees:
// 1. Visibility: Changes visible to all threads immediately
// 2. Happens-before relationship
// 3. No caching in thread-local memory
// 4. NOT atomic for compound operations (use AtomicXxx instead)
```

### 8.4 Explain Java Memory Leaks

```
Common causes:
1. Static collections never cleared
2. Unclosed resources (files, connections)
3. Listeners not deregistered
4. ThreadLocal not removed
5. Inner classes holding outer reference
6. Custom data structures (linked structures)
```

### 8.5 What is the difference between fail-fast and fail-safe iterators?

```
Fail-fast (ArrayList, HashMap):
- Throws ConcurrentModificationException
- Detects concurrent modification
- Uses modCount

Fail-safe (CopyOnWriteArrayList, ConcurrentHashMap):
- Works on copy of collection
- No exception
- May not reflect latest changes
```

---

## Next Steps
- Review Java concurrency in depth
- Practice implementing design patterns
- Study Spring Boot architecture
- Review JVM tuning for production
- Check `07_RESOURCES_AND_PRACTICE.md` for practice problems

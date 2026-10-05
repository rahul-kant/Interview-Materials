# Java Multithreading: A Complete, Confidence-Building Guide

> **Goal:** Take you from "what is a thread?" to comfortably answering FAANG concurrency interview questions. Every concept is explained with a plain-language idea first, then a runnable example, then the gotchas interviewers love to probe.

This guide reorganizes and expands the classic "Multithreading in Java" curriculum into a clear learning path. Read it top to bottom once; then use the **Cheat Sheet** and **Interview Questions** at the end to revise.

---

## 📑 Table of Contents

**Part 1 — Foundations**

- [1. What Is Multithreading?](#1-what-is-multithreading)
- [2. Why Multithreading? Benefits & Challenges](#2-why-multithreading-benefits--challenges)
- [3. Concurrency vs. Parallelism](#3-concurrency-vs-parallelism)
- [4. How the JVM Manages Threads](#4-how-the-jvm-manages-threads)
- [5. The Thread Lifecycle](#5-the-thread-lifecycle)

**Part 2 — Creating & Controlling Threads**

- [6. Four Ways to Create a Thread](#6-four-ways-to-create-a-thread)
- [7. Essential Thread Methods](#7-essential-thread-methods)

**Part 3 — The Core Problem & Synchronization**

- [8. Race Conditions](#8-race-conditions)
- [9. The `synchronized` Keyword & Intrinsic Locks](#9-the-synchronized-keyword--intrinsic-locks)
- [10. Object Locks vs. Class Locks](#10-object-locks-vs-class-locks)
- [11. Synchronized Methods vs. Blocks](#11-synchronized-methods-vs-blocks)
- [12. Thread Communication: `wait()` / `notify()`](#12-thread-communication-wait--notify)
- [13. Deadlock, Livelock & Starvation](#13-deadlock-livelock--starvation)

**Part 4 — Advanced Synchronization Tools**

- [14. ReentrantLock & ReadWriteLock](#14-reentrantlock--readwritelock)
- [15. CyclicBarrier](#15-cyclicbarrier)
- [16. Semaphore](#16-semaphore)
- [17. CountDownLatch](#17-countdownlatch)
- [18. Comparison of Synchronization Primitives](#18-comparison-of-synchronization-primitives)
- [19. `volatile` & Atomic Variables](#19-volatile--atomic-variables)

**Part 5 — High-Level Concurrency**

- [20. Thread Pools & the Executor Framework](#20-thread-pools--the-executor-framework)
- [21. Callable, Future & CompletableFuture](#21-callable-future--completablefuture)
- [22. Virtual Threads (Project Loom)](#22-virtual-threads-project-loom)

**Part 6 — Classic Coding Problems (worked solutions)**

- [23. Print Even & Odd Numbers with Two Threads](#23-print-even--odd-numbers-with-two-threads)
- [24. Print in Sequence with Three Threads (Zero-Even-Odd style)](#24-print-in-sequence-with-three-threads-zero-even-odd-style)
- [25. Producer-Consumer (three ways: `wait`/`notify`, `Lock`+`Condition`, `BlockingQueue`)](#25-producer-consumer-three-ways-waitnotify-lockcondition-blockingqueue)
- [26. Run Tasks Concurrently with the Executor Framework](#26-run-tasks-concurrently-with-the-executor-framework)
- [27. Parallel Sum with Callable & Future](#27-parallel-sum-with-callable--future)
- [28. Fan-out HTTP Calls with Virtual Threads](#28-fan-out-http-calls-with-virtual-threads)
- [29. Thread-Safe Singleton](#29-thread-safe-singleton)
- [30. Thread-Safe Counter (Three Ways)](#30-thread-safe-counter-three-ways)
- [31. Rate Limiter (Token Bucket, Sliding Window, Distributed)](#31-rate-limiter-token-bucket-sliding-window-distributed)
- [32. Concurrent LRU Cache](#32-concurrent-lru-cache)
- [33. Deadlock — Demonstration & Fix](#33-deadlock--demonstration--fix)
- [34. Dining Philosophers](#34-dining-philosophers)
- [35. Custom Thread Pool (From Scratch)](#35-custom-thread-pool-from-scratch)
- [36. Concurrent Web Crawler](#36-concurrent-web-crawler)

**Part 7 — Mastery**

- [37. The Java Memory Model & the `count++` Progression](#37-the-java-memory-model--the-count-progression)
- [38. Best Practices](#38-best-practices)
- [39. Common Pitfalls & Anti-Patterns](#39-common-pitfalls--anti-patterns)
- [40. FAANG Interview Questions](#40-faang-interview-questions)
- [41. FAQs](#41-faqs)
- [42. Quick Revision Cheat Sheet (whole-guide recall in one pass)](#42-quick-revision-cheat-sheet-whole-guide-recall-in-one-pass)

---

# Part 1 — Foundations

## 1. What Is Multithreading?

A **thread** is the smallest unit of execution within a program. **Multithreading** means a single program runs multiple threads concurrently, so it can do several things at once.

**The intuition:** Think of a video streaming app. One thread decodes the video frames, another decodes audio, and a third keeps them in sync — all at the same time, so playback feels smooth. If all of this ran on one thread sequentially, the experience would stutter.

A quick distinction you'll need constantly:

```
Process  = a running program with its OWN memory space.
Thread   = a unit of execution INSIDE a process.

One process can have many threads. They SHARE the heap
(objects), but each thread has its OWN stack and program counter.
```

```
        PROCESS (shared heap)
   ┌───────────────────────────────┐
   │   Heap:  [obj A] [obj B] ...   │  ← shared by all threads
   ├───────────────┬───────────────┤
   │   Thread 1    │   Thread 2    │
   │  ┌─────────┐  │  ┌─────────┐  │
   │  │ stack   │  │  │ stack   │  │  ← each thread private
   │  │ PC      │  │  │ PC      │  │
   │  └─────────┘  │  └─────────┘  │
   └───────────────┴───────────────┘
```

That shared heap is the whole story of concurrency: it's what makes threads powerful (cheap data sharing) **and** what makes them dangerous (uncoordinated access corrupts data).

Java builds multithreading into the language via the `java.lang.Thread` class and the rich `java.util.concurrent` package.

---

## 2. Why Multithreading? Benefits & Challenges

**Why bother?** Imagine a web server that handled requests one at a time. User #2 would wait for user #1 to finish completely. Multithreading lets the server handle many requests simultaneously, slashing latency.

### Benefits

| Benefit | What it means | Example |
|---|---|---|
| **Responsiveness** | The app stays interactive while doing slow work in the background | A download manager updates its progress bar without freezing the UI |
| **Resource sharing** | Threads share memory, so cooperating on data is cheap | Several threads each process a slice of one big dataset |
| **Parallelism** | Use all CPU cores at once | A video editor renders different segments simultaneously |
| **Throughput** | Do more total work per unit time | A server serves thousands of concurrent users |

### Challenges (the price you pay)

| Challenge | What goes wrong |
|---|---|
| **Concurrency bugs** | Unsynchronized access to shared data → race conditions, corrupted state |
| **Deadlocks** | Two threads each wait forever for a lock the other holds |
| **Overhead** | Threads cost memory and CPU; creating too many exhausts resources |
| **Hard to reason about** | Bugs are non-deterministic and may appear only under load |

The rest of this guide is essentially a toolbox for keeping the benefits while taming the challenges.

---

## 3. Concurrency vs. Parallelism

These two words get used interchangeably, but interviewers love the distinction:

- **Concurrency** = *dealing with* many things at once. It's a **structure** — tasks have overlapping lifetimes. Achievable even on a single CPU core by rapidly switching between tasks (time-slicing).
- **Parallelism** = *doing* many things at once. It's about **execution** — tasks literally run at the same instant on multiple cores.

```
CONCURRENCY (1 core, interleaved)        PARALLELISM (2 cores, simultaneous)
Core: A B A B A B A B                    Core 1: A A A A
                                         Core 2: B B B B
```

You can have concurrency without parallelism (one core juggling tasks) and parallelism is one way to *implement* concurrency. **Concurrency is about design; parallelism is about hardware.**

---

## 4. How the JVM Manages Threads

The JVM hides most of the messy details of thread management. Four things worth knowing:

- **Thread lifecycle:** Threads move through well-defined states (NEW → RUNNABLE → … → TERMINATED). The JVM manages the transitions.
- **Thread scheduling:** Java delegates to the OS scheduler, which uses **preemptive multitasking** — it allocates CPU slices based on priority and fairness. Important caveat: **thread priority in Java is only a hint.** Don't write code whose correctness depends on priority or execution order.
- **Garbage collection:** Modern collectors (G1GC, ZGC) run largely concurrently with your threads to minimize pauses.
- **Thread safety of the JVM itself:** The JVM uses intrinsic locks internally (e.g., during class loading and static initialization), and gives you those same primitives for your own code.

---

## 5. The Thread Lifecycle

Every Java thread is, at any moment, in exactly one of six states (`Thread.State`):

```mermaid
stateDiagram-v2
    [*] --> NEW: new Thread()
    NEW --> RUNNABLE: start()
    RUNNABLE --> BLOCKED: waiting for a monitor lock
    BLOCKED --> RUNNABLE: lock acquired
    RUNNABLE --> WAITING: wait() / join() / park()
    WAITING --> RUNNABLE: notify() / notifyAll()
    RUNNABLE --> TIMED_WAITING: sleep(t) / wait(t) / join(t)
    TIMED_WAITING --> RUNNABLE: timeout or notify
    RUNNABLE --> TERMINATED: run() returns
    TERMINATED --> [*]
```

| State | Meaning |
|---|---|
| **NEW** | Created but `start()` not yet called |
| **RUNNABLE** | Eligible to run (running or ready, waiting for CPU). Java merges "ready" and "running" here |
| **BLOCKED** | Waiting to acquire a monitor lock held by another thread |
| **WAITING** | Waiting indefinitely for another thread (`wait()`, `join()`) |
| **TIMED_WAITING** | Waiting for a bounded time (`sleep(t)`, `wait(t)`) |
| **TERMINATED** | `run()` has finished (normally or via exception) |

### 5.1 The analogy: an employee's workday

Think of a thread as an employee:

- **NEW** — hired but hasn't shown up yet (object created, `start()` not called).
- **RUNNABLE** — at their desk, either actively working *or* ready to work but waiting for the manager (CPU scheduler) to give them a turn. Java deliberately doesn't distinguish "running right now" from "ready to run" — both are RUNNABLE.
- **BLOCKED** — standing outside a meeting room (a `synchronized` section) whose door is locked because a colleague is inside. They wait for the **lock** on the room.
- **WAITING** — they've been told "wait here until I call you" with no deadline (`wait()`, `join()`, `LockSupport.park()`). They do nothing until explicitly signaled.
- **TIMED_WAITING** — same, but with an alarm clock: "wait, but no longer than 5 minutes" (`sleep(t)`, `wait(t)`, `join(t)`).
- **TERMINATED** — left for the day; the work (`run()`) is done and they can't come back.

### 5.2 Architecture: where the state lives and who changes it

A Java thread is more than the state enum — it's a stack of moving parts:

```
   Java Thread object (heap)        ── your Runnable, name, priority, state ──┐
        │                                                                     │
        ▼  start()                                                            │
   JVM thread (native)              ── maps 1:1 to an OS thread (platform)    │
        │                                                                     │
        ▼                                                                     │
   OS thread + kernel scheduler     ── decides which RUNNABLE thread gets a   │
        │                              CPU core, and for how long (time slice)│
        ▼                                                                     │
   CPU core                         ── actually executes instructions ────────┘
```

The crucial insight for interviews: **the JVM tracks the `Thread.State` enum, but it's the OS kernel scheduler that decides which RUNNABLE thread actually occupies a CPU core at any instant.** Java has no "RUNNING" state precisely because, from the JVM's point of view, "ready to run" and "currently on a core" are indistinguishable — that decision belongs to the OS. State transitions are driven by a mix of your code (`start()`, `wait()`, `sleep()`), other threads (`notify()`, releasing a lock, finishing a `join`ed task), and the scheduler.

### 5.3 The transitions in detail

| Transition | Triggered by |
|---|---|
| NEW → RUNNABLE | You call `start()` (which creates the native thread and schedules it). Calling `run()` directly does **not** transition state — it just runs on the current thread |
| RUNNABLE ↔ "on CPU" | The OS scheduler, via preemptive time-slicing. Not visible as a separate Java state |
| RUNNABLE → BLOCKED | Thread hits a `synchronized` boundary whose monitor is held by another thread |
| BLOCKED → RUNNABLE | The holder releases the monitor and this thread wins it |
| RUNNABLE → WAITING | `Object.wait()`, `Thread.join()`, or `LockSupport.park()` with no timeout |
| WAITING → RUNNABLE | `notify()`/`notifyAll()`, the joined thread terminates, or `unpark()` |
| RUNNABLE → TIMED_WAITING | `Thread.sleep(t)`, `wait(t)`, `join(t)`, `parkNanos`/`parkUntil` |
| TIMED_WAITING → RUNNABLE | The timeout elapses, or it's signaled early |
| RUNNABLE → TERMINATED | `run()` returns normally or throws an uncaught exception |

### 5.4 Subtleties interviewers probe

- **No RUNNING state.** As above — RUNNABLE covers both ready and executing. A common trap question.
- **BLOCKED vs. WAITING.** BLOCKED means "I want a *monitor lock* someone else holds" (only ever caused by `synchronized`). WAITING means "I'm parked until *signaled*" (`wait`, `join`, `park`). A thread waiting on a `ReentrantLock` shows as WAITING (it uses `LockSupport.park`), **not** BLOCKED — a subtle but real distinction, since only intrinsic-lock contention produces BLOCKED.
- **`sleep()` does not change lock ownership.** A sleeping thread is TIMED_WAITING but **still holds any locks it acquired** — a classic cause of others being stuck BLOCKED.
- **Terminated is final.** You can't restart a thread; calling `start()` on a TERMINATED (or already-started) thread throws `IllegalThreadStateException`.
- **Inspecting state at runtime:** `thread.getState()` returns the enum; thread dumps (`jstack`) show these states and are how you diagnose "why is everything stuck?" in production.

<details>
<summary><b>Example: observing thread states programmatically</b></summary>

```java
public class ThreadStateDemo {
    public static void main(String[] args) throws InterruptedException {
        Object lock = new Object();

        Thread t = new Thread(() -> {
            synchronized (lock) {
                try { lock.wait(); }                 // -> WAITING
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });

        System.out.println(t.getState());            // NEW
        t.start();
        Thread.sleep(100);
        System.out.println(t.getState());            // WAITING (parked in wait())

        synchronized (lock) { lock.notify(); }        // wake it
        Thread.sleep(100);
        System.out.println(t.getState());            // TERMINATED
    }
}
```

`getState()` is for monitoring/debugging only — never build program logic around polling it (it's inherently racy).

</details>

### 5.5 The classic five-state model (and how it maps to `Thread.State`)

Older references and certification material (the classic SCJP curriculum) describe the thread lifecycle with **five** conceptual states rather than the six the JVM actually exposes. You will still meet this vocabulary in interviews and legacy documentation, so it's worth mapping the two views onto each other.

| Classic (conceptual) state | What it means | Modern `Thread.State` equivalent |
|---|---|---|
| **New** | Object created, `start()` not yet called | `NEW` |
| **Runnable** | Eligible to run, waiting for the scheduler to pick it | `RUNNABLE` (the "ready" half) |
| **Running** | Currently executing on a CPU core | `RUNNABLE` (the "on-CPU" half — the JVM does not expose this separately) |
| **Waiting / Blocked / Sleeping** | Temporarily not eligible: waiting for a lock, for a signal, for a joined thread, or for a timed sleep | `BLOCKED`, `WAITING`, `TIMED_WAITING` |
| **Dead** | `run()` has completed | `TERMINATED` |

The one genuinely important difference is the **Running** state. The classic model treats "ready to run" and "actually on a core" as two distinct states; the modern JVM deliberately collapses both into a single `RUNNABLE`, because choosing which ready thread occupies a core is the OS scheduler's job, not the JVM's (see §5.2). Everything the classic model lumps together as "Waiting/Blocked/Sleeping" is, in the modern model, split into three precise states according to *why* the thread is off the CPU: `BLOCKED` (waiting for a monitor lock), `WAITING` (parked until signalled), and `TIMED_WAITING` (parked with a deadline).

```
NEW ──start()──▶ RUNNABLE ◀───────────────┐
                    │  (scheduler picks)   │
                    ▼                       │ lock acquired / notified /
              [on a CPU core]               │ timeout elapses / join done
                    │                       │
   wait / sleep / join / blocked on a lock  │
                    ▼                       │
        BLOCKED / WAITING / TIMED_WAITING ──┘
                    │
              run() returns
                    ▼
               TERMINATED
```

---

# Part 2 — Creating & Controlling Threads

## 6. Four Ways to Create a Thread

There are four common ways to define and run a task on a thread. Understanding the trade-offs is a classic warm-up question.

### (a) Implement `Runnable` (the preferred way)

`Runnable` separates *what to do* (the task) from *how it runs* (the thread). Because Java only allows single inheritance, implementing an interface keeps your class free to extend something else.

<details>
<summary><b>Example: print numbers in a separate thread (Runnable)</b></summary>

```java
public class RunnableExample {
    public static void main(String[] args) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i <= 5; i++) {
                    System.out.println("Worker: " + i);
                    sleepHalfSecond();
                }
            }
        };

        Thread worker = new Thread(task);
        worker.start();   // spawns a NEW thread that runs task.run()

        for (int i = 1; i <= 5; i++) {
            System.out.println("Main: " + i);
            sleepHalfSecond();
        }
    }

    private static void sleepHalfSecond() {
        try { Thread.sleep(500); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

The `Main:` and `Worker:` lines interleave because both threads run concurrently.

</details>

### (b) Extend `Thread` directly

You can subclass `Thread` and override `run()`. It's self-contained for small tasks, but it **couples the task to the thread** and **uses up your one shot at inheritance**.

<details>
<summary><b>Example: matrix multiplication by extending Thread</b></summary>

```java
public class MatrixMultiplicationThread extends Thread {
    private final int[][] a, b;
    private final int[][] result;

    public MatrixMultiplicationThread(int[][] a, int[][] b) {
        this.a = a;
        this.b = b;
        this.result = new int[a.length][b[0].length];
    }

    @Override
    public void run() {
        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < b[0].length; j++)
                for (int k = 0; k < b.length; k++)
                    result[i][j] += a[i][k] * b[k][j];
        System.out.println("Matrix multiplication completed.");
    }

    public static void main(String[] args) {
        int[][] a = {{1, 2}, {3, 4}};
        int[][] b = {{5, 6}, {7, 8}};
        new MatrixMultiplicationThread(a, b).start();
        System.out.println("Main thread is free to do other work.");
    }
}
```

</details>

### (c) Lambda + `Runnable` (Java 8+, concise)

Since `Runnable` is a functional interface, a lambda removes all the boilerplate. Best for short tasks.

<details>
<summary><b>Example: matrix multiplication with a lambda</b></summary>

```java
public class LambdaMatrixMultiplication {
    public static void main(String[] args) {
        int[][] a = {{1, 2}, {3, 4}};
        int[][] b = {{5, 6}, {7, 8}};
        int[][] result = new int[a.length][b[0].length];

        Runnable task = () -> {
            for (int i = 0; i < a.length; i++)
                for (int j = 0; j < b[0].length; j++)
                    for (int k = 0; k < b.length; k++)
                        result[i][j] += a[i][k] * b[k][j];
            System.out.println("Matrix multiplication completed.");
        };

        new Thread(task).start();
        System.out.println("Main thread is free to do other work.");
    }
}
```

</details>

### (d) `Callable` + `ExecutorService` (when you need a result)

`Runnable` can't return a value or throw checked exceptions. `Callable<T>` can. Submit it to an executor and get a `Future<T>` back (covered in §21).

### Which one should you use?

| Approach | Pros | Cons | Use when |
|---|---|---|---|
| `Runnable` | Decoupled, reusable, can extend another class | Slightly more verbose | **Default choice** |
| Extend `Thread` | Self-contained | Can't extend anything else; couples task to thread | Tiny throwaway tasks |
| Lambda `Runnable` | Minimal boilerplate | Hard to read when logic grows | Short, simple tasks |
| `Callable` + Executor | Returns values, throws checked exceptions, integrates with pools | Needs the executor framework | You need a result or use a pool |

**Bottom line:** In real code you rarely create raw `Thread`s at all — you submit `Runnable`/`Callable` tasks to a thread pool (§20).

---

## 7. Essential Thread Methods

A few methods you must be able to explain:

| Method | What it does | Key gotcha |
|---|---|---|
| `start()` | Spawns a new thread that runs `run()` | Calling `run()` directly does NOT create a thread — it runs on the caller. Calling `start()` twice throws `IllegalThreadStateException` |
| `join()` | Caller waits for this thread to finish | Used to collect results / order shutdown |
| `sleep(ms)` | Pauses the current thread for a time | Does **not** release any locks held |
| `interrupt()` | Requests the thread to stop | Cooperative — sets a flag / triggers `InterruptedException`; doesn't force-kill |
| `setDaemon(true)` | Marks thread as background | JVM exits when only daemon threads remain; must be set **before** `start()` |
| `yield()` | Hints the scheduler to let others run | Just a hint; often a no-op |

**On interruption:** never silently swallow `InterruptedException`. Either let it propagate, or restore the flag:

```java
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();   // restore the interrupt status
}
```

### 7.1 `start()` vs `run()`, and the "overloaded run" trap

`start()` and `run()` are the most commonly confused pair in the whole topic:

- `start()` asks the JVM to **create a new thread of execution**, which then calls `run()` on that new thread. Control returns to the caller immediately.
- Calling `run()` **directly** executes it like any ordinary method call — **on the current thread**. No new thread is created and nothing runs concurrently.

Two rules follow that interviewers test verbatim: you may call `start()` on a given `Thread` object **only once** (a second call throws `IllegalThreadStateException`, because a thread cannot be restarted), and it is perfectly legal to construct **many** `Thread` objects that share the **same** `Runnable` target.

A subtler trap is the **overloaded `run`**. If you extend `Thread` (or implement `Runnable`) and write a method such as `public void run(String name)`, you have *overloaded* `run`, not *overridden* it. The threading machinery only ever calls the no-argument `run()`, so your `run(String)` silently never executes when the thread starts.

<details>
<summary><b>Example: the overloaded run() that never runs</b></summary>

```java
public class RunOverloadDemo extends Thread {
    // GOTCHA: this OVERLOADS run — it does not override Thread.run().
    // start() only ever invokes the no-arg run(), so this never executes.
    public void run(String msg) {
        System.out.println("run(String) called: " + msg);
    }

    public static void main(String[] args) throws InterruptedException {
        RunOverloadDemo t = new RunOverloadDemo();
        t.start();     // JVM calls the inherited no-arg run() -> does nothing
        t.join();
        // Nothing was printed from run(String): it was never called by start().
        System.out.println("start() ignored the run(String) overload");
    }
}
```

The compiler won't warn you; the thread simply appears to "do nothing." The fix is to override the exact `public void run()` signature — adding `@Override` lets the compiler catch the mistake for you.

</details>

### 7.2 `sleep()`, `yield()`, and `join()` — precise semantics

These three are easy to describe loosely and easy to get wrong precisely:

- **`sleep(ms)`** is a **static** method that pauses the **currently executing** thread for at least the requested time (you cannot tell *another* thread to sleep). It moves the thread to `TIMED_WAITING` and — critically — **does not release any locks** it holds. A sleeping thread is only *guaranteed* to sleep for at least the given duration; when it wakes there is no guarantee it returns to a core immediately.
- **`yield()`** is a **hint** to the scheduler that the current thread is willing to give up the core to other **runnable threads of the same priority**. There's no guarantee any other thread runs, and the yielding thread may be re-selected immediately. Never rely on it for correctness — it's at best a soft tuning knob.
- **`join()`** makes the **calling** thread wait until the target thread finishes. `t.join()` means "the current thread pauses here until `t` terminates." It's the standard way to order shutdown or collect a thread's results, and `join(timeout)` bounds the wait.

### 7.3 Thread scheduling and priorities

Java threads are scheduled **preemptively** by the OS kernel: the scheduler hands each runnable thread a slice of CPU time and can interrupt it to run another, based on priority and fairness. On a single core only one thread runs at any instant even though many may be `RUNNABLE`; there is **no guarantee** about the order in which threads run or that they take turns fairly — that is entirely up to the JVM and OS.

`Thread.setPriority(int)` accepts values from `Thread.MIN_PRIORITY` (1) to `Thread.MAX_PRIORITY` (10), with `NORM_PRIORITY` (5) the default. If you never set it, a thread **inherits the priority of the thread that created it**. Crucially, priority is only a **hint**: it is mapped onto the host OS's own priority scheme, some platforms collapse or ignore levels, and the mapping varies by JVM and OS. The rule is absolute: **never write code whose correctness depends on thread priority or execution order.** If you need ordering guarantees, use explicit coordination (`join`, locks, `wait`/`notify`, the synchronizers in Part 4) — not priorities.

---

# Part 3 — The Core Problem & Synchronization

## 8. Race Conditions

A **race condition** happens when two or more threads access shared data at the same time, and the final result depends on the unpredictable *order* in which they run. The program becomes non-deterministic — different runs give different answers.

### The classic example: an unsynchronized counter

<details>
<summary><b>Example: two threads incrementing a shared counter (BROKEN)</b></summary>

```java
public class RaceConditionExample {
    private static int counter = 0;

    public static void main(String[] args) throws InterruptedException {
        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) counter++;
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Expected 2000... but you'll often see less!
        System.out.println("Final Counter Value: " + counter);
    }
}
```

Run it repeatedly and you'll see values like `1873`, `1991`, `2000` — inconsistent.

</details>

### Why it breaks

`counter++` looks atomic but is actually **three** machine operations:

```
1. READ  counter        (load current value)
2. ADD   1              (compute new value)
3. WRITE counter        (store it back)
```

Two threads can interleave disastrously:

```
Thread A: read counter -> 10
Thread B: read counter -> 10      (before A writes!)
Thread A: 10 + 1 = 11, write 11
Thread B: 10 + 1 = 11, write 11   ← lost update! Should be 12
```

Three ingredients cause race conditions: **shared state**, **no synchronization**, and **arbitrary interleaving** by the scheduler. Remove any one and the bug disappears — synchronization removes the second.

> Memorize the three hazards of concurrency: **atomicity** (operations can be interrupted mid-way), **visibility** (one thread may not see another's writes), and **ordering** (the compiler/CPU may reorder operations). Almost every fix addresses one of these.

### A second flavor: check-then-act (the "look before you leap" race)

The counter above is a **lost-update** race — two writes stomp on each other. The other classic flavor is **check-then-act** (also called *time-of-check-to-time-of-use*, TOCTOU): a thread checks a condition and then acts on it, but another thread invalidates the condition **in between**. Even if each individual step is safe, the *gap* between "check" and "act" is not.

A bank withdrawal is the textbook case. Two people share one account with a balance of 100 and both try to withdraw 100 at almost the same moment:

```
Fred:  balance >= 100 ?  yes   (check passes)
Lucy:  balance >= 100 ?  yes   (check also passes, before Fred subtracts!)
Fred:  balance = 100 - 100 = 0
Lucy:  balance = 0   - 100 = -100    <- overdrawn: the account went negative
```

Both threads read a balance of 100, both pass the `if (balance >= amount)` check, and both then subtract — leaving the account overdrawn. Note that no update was "lost" here; the bug is that the **decision** was made on a value that had already gone stale by the time the thread acted on it.

The fix is to make the check and the act **one atomic critical section**, so no other thread can slip in between them:

<details>
<summary><b>Example: check-then-act race and its fix</b></summary>

```java
public class Account {
    private int balance;

    public Account(int balance) { this.balance = balance; }

    // BROKEN: another thread can pass the same check before this one subtracts,
    // so two withdrawals of 100 can BOTH succeed against a balance of 100.
    public void unsafeWithdraw(int amount) {
        if (balance >= amount) {      // CHECK
            // ... a context switch here is all it takes ...
            balance -= amount;        // ACT (on a possibly-stale decision)
        }
    }

    // FIXED: check and act are now indivisible — one thread completes the whole
    // decide-and-update before any other thread can enter the method.
    public synchronized void safeWithdraw(int amount) {
        if (balance >= amount) {      // CHECK and ACT are atomic together
            balance -= amount;
        }
    }

    public synchronized int getBalance() { return balance; }
}
```

The lesson generalizes far beyond bank accounts: **whenever a decision and the action based on it must both see the same state, they belong inside the same lock.** This is exactly why an "is it present? then add it" sequence on a shared map must use an atomic method such as `putIfAbsent`/`computeIfAbsent` rather than a separate `containsKey` followed by `put`.

</details>

---

## 9. The `synchronized` Keyword & Intrinsic Locks

`synchronized` is the cornerstone of Java thread safety. It guarantees that **only one thread at a time** can execute a given critical section — and it also guarantees **visibility** (changes made by one thread become visible to the next thread that acquires the lock).

### Intrinsic locks (monitor locks)

**Every Java object has a built-in lock**, called its *intrinsic lock* or *monitor*. When a thread enters a `synchronized` block/method, it **acquires** that object's lock; when it leaves, it **releases** it. While held, no other thread can enter any synchronized region guarded by the same lock.

```java
synchronized (someObject) {
    // Only one thread at a time can be in here (per someObject).
}
```

What happens step by step:

1. **Acquire:** thread tries to grab the lock. If another thread holds it, this thread becomes `BLOCKED`.
2. **Execute:** once it has the lock, it runs the critical section.
3. **Release:** on exit (normal or via exception), the lock is released and a waiting thread can proceed.

### Fixing the counter

<details>
<summary><b>Example: a correctly synchronized counter</b></summary>

```java
public class SharedCounter {
    private int counter = 0;

    public synchronized void increment() { counter++; }
    public synchronized int getCounter() { return counter; }

    public static void main(String[] args) throws InterruptedException {
        SharedCounter c = new SharedCounter();
        Runnable task = () -> { for (int i = 0; i < 1000; i++) c.increment(); };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start(); t2.start();
        t1.join();  t2.join();

        System.out.println("Final Counter Value: " + c.getCounter()); // always 2000
    }
}
```

Both `increment()` and `getCounter()` are synchronized on the same object, so increments can't interleave and reads always see the latest value.

</details>

**Two important properties:**

- `synchronized` is **reentrant** — a thread that already holds a lock can re-acquire it (e.g., one synchronized method calling another on the same object) without deadlocking itself.
- Don't lock on `this` or on shared constants like `String`/`Integer` in library code; prefer a `private final Object lock = new Object();` so external code can't accidentally acquire your lock.

### Why `synchronized` also fixes *visibility* (not just mutual exclusion)

It's tempting to think `synchronized` only stops two threads from running a critical section at the same time. It does more, and the "more" is what makes it correct. Under the **Java Memory Model (JMM)**, each thread may keep values in registers or per-core caches and need not see another thread's writes in a timely way — unless a **happens-before** relationship connects them. Releasing a monitor **happens-before** any subsequent acquisition of that *same* monitor. Concretely: when a thread exits a `synchronized` block it **flushes** its writes so they become visible, and when the next thread enters a block on the same lock it is **guaranteed to see** those writes.

So the lock gives you two guarantees at once — **mutual exclusion** (only one thread inside) and **visibility** (the next thread in sees everything the previous thread did). This is why a field consistently guarded by a lock does not also need to be `volatile`, and, conversely, why *reading* shared state **outside** the lock can still observe stale values even when every *write* is locked. The takeaway: to be safe, both readers and writers of shared mutable state must synchronize on the same lock.

### Which classes are already thread-safe? (and the compound-operation trap)

You don't always have to synchronize by hand — parts of the JDK already do it for you. But knowing the boundaries of that guarantee is itself a favourite interview probe.

**`StringBuffer` vs `StringBuilder`.** They have identical APIs, but `StringBuffer`'s methods are `synchronized` (thread-safe), whereas `StringBuilder`'s are not (and it is therefore faster). The rule: use `StringBuilder` by default — string building is almost always confined to one thread — and reach for `StringBuffer` only when a single builder is genuinely shared across threads. (Better still, confine the builder to one thread and share only the finished `String`, which is immutable and always safe to share.)

**Legacy synchronized collections.** `Vector` and `Hashtable` synchronize every method, as do the wrappers returned by `Collections.synchronizedList/Map/Set`. This makes each *individual* operation atomic — but that is a weaker guarantee than it first appears.

**The compound-operation trap.** Making each method synchronized does **not** make a *sequence* of methods atomic. A "check-then-act" spread across two calls (§8) still races, because another thread can interleave *between* your two already-synchronized calls:

<details>
<summary><b>Example: a synchronized list still needs client-side locking</b></summary>

```java
import java.util.*;

public class NameList {
    // Each individual add/remove/isEmpty call is synchronized internally...
    private final List<String> names = Collections.synchronizedList(new ArrayList<>());

    public void add(String name) { names.add(name); }

    // BROKEN under concurrency: isEmpty() and remove() are each atomic on their
    // own, but the GAP between them is not — two threads can both see size 1,
    // and the second remove(0) then throws or removes the wrong element.
    public String removeFirstBroken() {
        if (!names.isEmpty()) {                 // CHECK (locks, then unlocks)
            return names.remove(0);             // ACT   (locks again — too late)
        }
        return null;
    }

    // FIXED: hold the list's own lock across the WHOLE check-then-act, so no
    // other thread can modify it in between. synchronizedList locks on the
    // returned wrapper, so we synchronize on that same object.
    public String removeFirstSafe() {
        synchronized (names) {                  // one lock spanning CHECK + ACT
            if (!names.isEmpty()) {
                return names.remove(0);
            }
            return null;
        }
    }
}
```

Two rules to take away: (1) a "thread-safe" collection only guarantees that *single* operations are atomic — any multi-step invariant needs your own lock; and (2) when you do lock, you must lock on the **same** monitor the collection uses internally, which for `Collections.synchronizedXxx` is the returned wrapper object itself. This same reasoning is why iterating a synchronized collection must be done inside a `synchronized (collection) { ... }` block, and why `ConcurrentHashMap` — with its atomic `compute`/`merge`/`putIfAbsent` methods — is usually the better modern choice (see §41).

</details>

---

## 10. Object Locks vs. Class Locks

There are two scopes of intrinsic lock, and they are **independent** of each other:

| | Object lock | Class lock |
|---|---|---|
| Tied to | A specific **instance** (`this`) | The **`Class` object** (`Foo.class`) |
| Acquired by | `synchronized` instance method / `synchronized(this)` | `static synchronized` method / `synchronized(Foo.class)` |
| Scope | One instance | All instances of the class |

```java
public class Example {
    public synchronized void instanceMethod() {
        // locks on 'this' (the instance)
    }
    public static synchronized void staticMethod() {
        // locks on Example.class (the class)
    }
    public void block() {
        synchronized (this)          { /* instance lock */ }
        synchronized (Example.class) { /* class lock     */ }
    }
}
```

**Key insight:** because they're different locks, one thread can run a synchronized *instance* method while another runs a synchronized *static* method **at the same time** — they don't block each other. This trips up many candidates.

### 10.1 The analogy: bathroom keys

Imagine an office building.

- **Object lock = the key to one specific bathroom.** Each instance of your class is its own bathroom with its own key. If two people want the *same* bathroom (the same object), they take turns. But two people can use *two different* bathrooms (two different instances) simultaneously — different keys, no waiting.
- **Class lock = the master key to the entire floor's facilities.** There's exactly one master key per class (per `Class` object), shared by everyone, regardless of which bathroom (instance) they came from. Static synchronized methods all compete for this single master key.

Because the bathroom key and the master key are *different physical keys*, holding one says nothing about the other — which is exactly why an instance-synchronized method and a static-synchronized method don't block each other.

### 10.2 Architecture: how the monitor actually works

Every Java object has, in its header, an association with a **monitor** — the low-level construct that backs intrinsic locking. A monitor conceptually holds three things:

```
        OBJECT  (its header references a Monitor)
          │
          ▼
   ┌──────────────────────────────────────────────┐
   │  MONITOR                                       │
   │  • owner         → the thread holding the lock │
   │  • recursion cnt → reentrancy depth            │
   │  • Entry Set     → threads BLOCKED waiting      │
   │                    to ACQUIRE the lock          │
   │  • Wait Set      → threads WAITING after        │
   │                    calling wait()               │
   └──────────────────────────────────────────────┘
```

When you enter `synchronized(obj)`, the JVM executes a `monitorenter` bytecode: if the monitor is unowned, the current thread becomes owner and the recursion count goes to 1; if the *same* thread re-enters, the count just increments (that's **reentrancy**); if a *different* thread owns it, the entrant goes into the **Entry Set** (state BLOCKED). Exiting runs `monitorexit`, decrementing the count; at zero the lock is released and one Entry-Set thread is unblocked.

The **Wait Set** is separate: when the owner calls `obj.wait()`, it releases the lock and moves into the Wait Set (state WAITING); `notify()` moves one thread from the Wait Set back to the Entry Set to re-compete for the lock. This is why §12's `wait`/`notify` only works while holding the monitor — it physically manipulates these monitor structures.

```mermaid
graph TD
    T[Thread wants synchronized region] --> Q{Monitor owned?}
    Q -->|No| OWN[Become owner, count = 1]
    Q -->|Yes, by me| INC[Increment count - reentrant]
    Q -->|Yes, by another| ENTRY[Enter Entry Set - BLOCKED]
    OWN --> CRIT[Run critical section]
    INC --> CRIT
    CRIT --> W{Call wait?}
    W -->|Yes| WS[Release lock, enter Wait Set - WAITING]
    W -->|No| EXIT[monitorexit, count--]
    EXIT --> Z{count == 0?}
    Z -->|Yes| REL[Release lock, wake an Entry Set thread]
    Z -->|No| CRIT
    ENTRY -->|lock freed & chosen| OWN
    WS -->|notify| ENTRY
```

### 10.3 Performance deep dive: lock states & escalation

Intrinsic locks aren't a single heavyweight mechanism — the HotSpot JVM optimizes them through escalating tiers, recorded in the object header's "mark word":

1. **Biased locking** (historical; deprecated/removed in modern JDKs) — if only one thread ever locks an object, the lock is "biased" to it and re-entry is almost free.
2. **Thin / lightweight lock** — under low contention, the JVM uses a fast CAS on the mark word instead of a real OS mutex. No kernel involvement.
3. **Fat / heavyweight lock** — under real contention, the lock "inflates" to a full OS-level monitor (mutex + wait queues), where BLOCKED threads actually park in the kernel.

The practical takeaway: uncontended `synchronized` is cheap (just a CAS); the cost appears under contention when the lock inflates and threads start parking/waking via the OS. This is *why* "keep critical sections small" matters — you want to minimize the window where contention (and inflation) can happen.

### 10.4 Subtleties & gotchas

- **Reentrancy is per-thread, per-monitor.** A thread holding `this`'s lock can call another `synchronized` method on the same object freely (count increments). Without reentrancy, a class calling its own synchronized methods would self-deadlock.
- **Static and instance locks are orthogonal.** Synchronizing a `static` method does **not** protect instance fields, and vice versa. If a field is `static`, guard it with the *class* lock; if it's an instance field, guard it with the *instance* lock. Mixing them is a real bug source.
- **Locking on the wrong object.** `synchronized(this)` in a class whose instances callers can see, or locking on a `String` literal / boxed `Integer` (which may be interned/cached and shared JVM-wide), lets unrelated code contend for or deadlock your lock. Use a `private final Object lock = new Object();`.
- **Changing the lock reference.** Never synchronize on a field you reassign — each thread may lock a different object and mutual exclusion silently breaks. Lock objects must be `final`.

<details>
<summary><b>Example: why instance and class locks don't interfere</b></summary>

```java
public class Counter {
    private int instanceCount;
    private static int classCount;

    public synchronized void incInstance() {        // locks on 'this'
        instanceCount++;
    }

    public static synchronized void incClass() {    // locks on Counter.class
        classCount++;
    }
}
```

Two threads calling `incInstance()` **on the same object** serialize (same instance lock). Two threads calling it on **different objects** run in parallel (different instance locks). A thread in `incInstance()` and another in `incClass()` run fully in parallel — different locks entirely. The mistake candidates make: assuming `static synchronized` somehow also guards instance state. It does not.

</details>

---

## 11. Synchronized Methods vs. Blocks

You can synchronize an entire method or just a block inside it.

**Synchronized method** — locks the whole method body:
```java
public synchronized void doWork() { /* entire method is the critical section */ }
```
Simple, but if only a few lines need protection, you're holding the lock longer than necessary, increasing contention.

**Synchronized block** — locks only the critical part:
```java
public void doWork() {
    // ... non-critical setup runs without the lock ...
    synchronized (lock) {
        // only the truly shared part
    }
    // ... more lock-free work ...
}
```
Finer-grained, better performance under contention. **Prefer blocks** that wrap the minimum necessary code.

### A bank transfer (and a deadlock warning)

<details>
<summary><b>Example: bank account transfer with synchronized blocks</b></summary>

```java
public class BankAccount {
    private int balance;

    public BankAccount(int initialBalance) { this.balance = initialBalance; }

    public void transfer(BankAccount target, int amount) {
        synchronized (this) {
            if (this.balance >= amount) {
                this.balance -= amount;
                synchronized (target) {     // nested lock — deadlock risk!
                    target.balance += amount;
                }
            }
        }
    }

    public int getBalance() { return balance; }
}
```

This works, but acquiring two locks (`this` then `target`) is a **deadlock trap**: if thread 1 transfers A→B while thread 2 transfers B→A, each can grab its first lock and wait forever for the second. The fix (next section) is to always acquire locks in a **consistent global order**.

</details>

---

## 12. Thread Communication: `wait()` / `notify()`

Sometimes a thread needs to **wait for a condition** that another thread will make true (e.g., a consumer waiting for an item to appear). That's what `wait()`, `notify()`, and `notifyAll()` are for. They live on `Object` (since every object has a monitor) and are deeply tied to intrinsic locks.

### The rules (interviewers test these precisely)

- All three **must** be called while holding the object's monitor (inside a `synchronized` block on that object), or you get `IllegalMonitorStateException`.
- `wait()` **releases the lock** and parks the thread. This is crucial — it lets another thread enter the synchronized region and change the condition.
- `notify()` wakes **one** waiting thread; `notifyAll()` wakes **all**. The notifying thread keeps the lock until it exits the synchronized block; only then can a woken thread re-acquire it and proceed.
- **Always call `wait()` inside a `while` loop**, never an `if` — to guard against *spurious wakeups* and the condition having changed before the woken thread reacquires the lock.

```java
synchronized (lock) {
    while (!conditionIsTrue) {   // while, NOT if
        lock.wait();
    }
    // safe to proceed: condition is true AND we hold the lock
}
```

### `notify()` vs `notifyAll()` — which to use, and why it matters

`notify()` wakes **one** arbitrary thread from the monitor's wait-set; `notifyAll()` wakes **all** of them (they then re-compete for the lock, and all but one immediately go back to waiting). If only one thread is ever waiting, the two behave identically. The difference bites when **multiple threads wait on the same monitor for different conditions.**

Consider a buffer with several producers *and* several consumers all waiting on the same lock. A consumer finishes and calls `notify()` to wake "a producer" — but `notify()` cannot choose *which* waiter it wakes. It may wake **another consumer**, which re-checks its `while` condition (buffer still empty), goes straight back to waiting, and the producer that actually needed the signal is never woken. The notification is effectively **lost**, and the system can stall. `notifyAll()` avoids this by waking everyone, guaranteeing that the thread which *can* make progress gets its chance.

The guidance is therefore: **prefer `notifyAll()` by default** — it is always correct. Use the narrower `notify()` only as an optimization when you can prove all waiters are interchangeable (all waiting on the exact same condition, any one of which can proceed); otherwise you risk the lost-signal stall.

### The missed-signal trap (why the condition, the lock, and the wait must line up)

`wait()` and `notify()` have no memory. A `notify()` delivered when **no thread is currently waiting** is simply discarded — it is not queued for a future `wait()`. So if a producer sets the condition and calls `notify()` *before* the consumer reaches `wait()`, the consumer can then call `wait()` and block **forever**, having missed the only signal. This is precisely why the correct idiom re-checks the condition **inside a `while` loop while holding the lock**: the waiter only calls `wait()` after confirming, under the lock, that the condition is still false — which closes the window in which a signal could be missed. The three requirements (call from a `synchronized` block, on the shared object, guarding a `while` condition) exist together to make this airtight.

### Producer–Consumer with `wait`/`notify`

This is *the* canonical coordination example.

<details>
<summary><b>Example: producer-consumer using wait() / notify()</b></summary>

```java
import java.util.LinkedList;
import java.util.Queue;

public class ProducerConsumer {
    private final Queue<Integer> buffer = new LinkedList<>();
    private static final int MAX_SIZE = 5;

    public void produce() throws InterruptedException {
        int value = 0;
        while (true) {
            synchronized (this) {
                while (buffer.size() == MAX_SIZE) {
                    wait();                  // buffer full -> release lock & wait
                }
                buffer.add(value);
                System.out.println("Produced: " + value);
                value++;
                notify();                    // wake a waiting consumer
            }
            Thread.sleep(500);
        }
    }

    public void consume() throws InterruptedException {
        while (true) {
            synchronized (this) {
                while (buffer.isEmpty()) {
                    wait();                  // buffer empty -> release lock & wait
                }
                int value = buffer.poll();
                System.out.println("Consumed: " + value);
                notify();                    // wake a waiting producer
            }
            Thread.sleep(500);
        }
    }

    public static void main(String[] args) {
        ProducerConsumer pc = new ProducerConsumer();
        new Thread(() -> { try { pc.produce(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }).start();
        new Thread(() -> { try { pc.consume(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }).start();
    }
}
```

The producer waits when the buffer is full; the consumer waits when it's empty; each `notify()`s the other after changing the buffer. In production you'd use a `BlockingQueue` (§20) instead of hand-writing this — but interviewers want to see you *can* write it.

</details>

---

## 13. Deadlock, Livelock & Starvation

Three liveness failures you must be able to name and fix:

**Deadlock** — threads wait on each other in a cycle, forever.
```
Thread 1: holds Lock A, wants Lock B
Thread 2: holds Lock B, wants Lock A   → neither can proceed
```
The four Coffman conditions (all must hold for deadlock): mutual exclusion, hold-and-wait, no preemption, circular wait. Break any one to prevent deadlock.

*Fixes:* acquire locks in a **consistent global order**; use `tryLock()` with a timeout; keep critical sections small; avoid calling unknown code while holding a lock.

**Livelock** — threads aren't blocked, but keep reacting to each other and make no progress (like two people repeatedly stepping aside in a hallway). *Fix:* add randomized backoff.

**Starvation** — a thread never gets the CPU or a lock because others monopolize it (e.g., unfair locking, or always-higher-priority threads). *Fix:* fair locks, avoid priority abuse.

**In plain English:** deadlock = everyone is frozen waiting on each other; livelock = everyone is busy but nobody progresses; starvation = one unlucky thread never gets a turn.

**Worked example —** a full deadlock demonstration plus two fixes (consistent lock ordering, and `tryLock` + timeout/backoff) is collected with the other coding problems in **§33 (Part 6)**.

---

# Part 4 — Advanced Synchronization Tools

`synchronized` is great but limited: you can't try-and-give-up, you can't time out, you can't interrupt a waiting thread, and there's only one implicit wait-set per lock. The `java.util.concurrent` package fixes all of that.

## 14. ReentrantLock & ReadWriteLock

### ReentrantLock — `synchronized` with superpowers

`ReentrantLock` is an explicit lock you `lock()` and `unlock()` yourself. It adds:

- **`tryLock()`** — attempt to acquire without blocking; returns `false` if unavailable (great for deadlock avoidance).
- **`tryLock(timeout)`** — wait only so long.
- **`lockInterruptibly()`** — a waiting thread can be interrupted.
- **Fairness** — `new ReentrantLock(true)` hands out the lock in FIFO order (prevents starvation, slightly slower).
- **Multiple `Condition`s** — separate wait-sets (e.g., `notFull` and `notEmpty`) on one lock.

| Feature | `synchronized` | `ReentrantLock` |
|---|---|---|
| Fairness | Not configurable | Configurable |
| Interruptible acquisition | No | Yes (`lockInterruptibly`) |
| Try-lock (non-blocking) | No | Yes |
| Timed lock | No | Yes |
| Condition variables | One implicit | Many explicit |
| Auto-release | Yes (block exit) | **No — you must `unlock()` in `finally`** |

<details>
<summary><b>Example: bank account with ReentrantLock</b></summary>

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BankAccount {
    private int balance;
    private final Lock lock = new ReentrantLock();

    public BankAccount(int initialBalance) { this.balance = initialBalance; }

    public void deposit(int amount) {
        lock.lock();
        try {
            balance += amount;
            System.out.println(Thread.currentThread().getName() + " deposited " + amount);
        } finally {
            lock.unlock();             // ALWAYS unlock in finally
        }
    }

    public void withdraw(int amount) {
        lock.lock();
        try {
            if (balance >= amount) {
                balance -= amount;
                System.out.println(Thread.currentThread().getName() + " withdrew " + amount);
            } else {
                System.out.println(Thread.currentThread().getName() + " insufficient balance.");
            }
        } finally {
            lock.unlock();
        }
    }

    public int getBalance() { return balance; }
}
```

The `try/finally` is non-negotiable: if the body throws, the `finally` still releases the lock, preventing a permanent lock leak.

</details>

### ReadWriteLock — many readers OR one writer

`ReentrantReadWriteLock` splits the lock in two: a **read lock** (shared — many threads can hold it at once) and a **write lock** (exclusive). Readers don't block each other; a writer blocks everyone.

**Use it when reads vastly outnumber writes** — caches, configuration that's read constantly but updated rarely, resource-state monitoring. For read-heavy data this dramatically reduces contention compared to a plain lock.

> Related: `StampedLock` (Java 8) adds an *optimistic read* mode that doesn't even take a lock on the happy path — you read, then validate a stamp. Very fast, but it's **not reentrant**, a common gotcha.

---

## 15. CyclicBarrier

A `CyclicBarrier` makes a fixed number of threads **wait for each other** at a common point; once all have arrived, they all proceed together. It's *cyclic* because it resets and can be reused for the next round.

**Use case:** split a computation into phases where every thread must finish phase 1 before anyone starts phase 2 (parallel simulations, multi-stage data processing).

<details>
<summary><b>Example: threads sync at a barrier before proceeding</b></summary>

```java
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class CyclicBarrierExample {
    public static void main(String[] args) {
        int numWorkers = 3;
        CyclicBarrier barrier = new CyclicBarrier(numWorkers,
            () -> System.out.println("All threads reached the barrier. Proceeding..."));

        Runnable task = () -> {
            try {
                System.out.println(Thread.currentThread().getName() + " working...");
                Thread.sleep((long) (Math.random() * 1000));
                System.out.println(Thread.currentThread().getName() + " reached the barrier.");
                barrier.await();    // wait until all 3 arrive
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
            }
        };

        for (int i = 0; i < numWorkers; i++) new Thread(task).start();
    }
}
```

The optional barrier action (the lambda) runs once, on the last thread to arrive, before any are released.

</details>

---

## 16. Semaphore

A `Semaphore` maintains a set of **permits**. A thread calls `acquire()` to take one (blocking if none are free) and `release()` to give it back. It limits how many threads can access a resource **concurrently** — perfect for a connection pool, a rate limiter, or any capped resource.

<details>
<summary><b>Example: limit concurrent access to 2 printers</b></summary>

```java
import java.util.concurrent.Semaphore;

public class PrintingQueue {
    private final Semaphore semaphore;

    public PrintingQueue(int availablePrinters) {
        this.semaphore = new Semaphore(availablePrinters);
    }

    public void printJob(String document) {
        try {
            semaphore.acquire();    // take a permit (waits if all are in use)
            System.out.println(Thread.currentThread().getName() + " printing: " + document);
            Thread.sleep((long) (Math.random() * 1000));
            System.out.println(Thread.currentThread().getName() + " finished.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaphore.release();    // return the permit
        }
    }

    public static void main(String[] args) {
        PrintingQueue queue = new PrintingQueue(2);   // only 2 at a time
        for (int i = 0; i < 5; i++) {
            new Thread(() -> queue.printJob("doc"), "Thread-" + i).start();
        }
    }
}
```

Even with 5 threads, at most 2 print simultaneously. A `Semaphore(1)` behaves like a lock (a "binary semaphore"), though it differs in that any thread can release it.

</details>

**Applied example —** a Semaphore-based rate limiter (token bucket), together with the sliding-window and distributed/Redis variants, is collected with the other coding problems in **§31 (Part 6)**.

---

## 17. CountDownLatch

A `CountDownLatch` lets one or more threads **wait until a set of operations completes**. It starts at a count N; each finishing task calls `countDown()`; threads blocked on `await()` are released when the count hits zero. **It's one-shot** — you can't reset it.

**Use case:** "wait until all N services have initialized before the system starts serving traffic."

<details>
<summary><b>Example: wait for 3 init tasks before starting</b></summary>

```java
import java.util.concurrent.CountDownLatch;

public class SystemInitialization {
    public static void main(String[] args) throws InterruptedException {
        int numTasks = 3;
        CountDownLatch latch = new CountDownLatch(numTasks);

        Runnable initTask = () -> {
            try {
                System.out.println(Thread.currentThread().getName() + " initializing...");
                Thread.sleep((long) (Math.random() * 1000));
                System.out.println(Thread.currentThread().getName() + " done.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();    // signal completion (always, even on failure)
            }
        };

        for (int i = 0; i < numTasks; i++) new Thread(initTask).start();

        latch.await();    // main waits here until count == 0
        System.out.println("All tasks complete. System starting...");
    }
}
```

</details>

### CountDownLatch vs. CyclicBarrier (a favorite comparison)

| | CountDownLatch | CyclicBarrier |
|---|---|---|
| Reusable? | No (one-shot) | Yes (resets) |
| Who waits? | One/more threads wait for **events** to complete | The participating threads wait for **each other** |
| Count changes via | `countDown()` from any thread | Threads calling `await()` |
| Mental model | "Wait for N things to finish" | "Everyone meet here, then go" |

---

## 18. Comparison of Synchronization Primitives

| Primitive | Key feature | Best use case |
|---|---|---|
| `synchronized` | Simple mutual exclusion + visibility | Basic critical sections |
| `ReentrantLock` | Explicit lock: tryLock, timeout, fairness, conditions | Fine-grained control, deadlock avoidance |
| `ReadWriteLock` | Shared reads, exclusive writes | High read-to-write ratio (caches) |
| `CyclicBarrier` | Threads wait for each other; reusable | Coordinating computation phases |
| `Semaphore` | N permits limit concurrency | Pools of limited resources |
| `CountDownLatch` | Wait for N events; one-shot | Startup / "wait for completion" |
| `volatile` | Visibility + ordering (no atomicity) | Flags, safe publication |
| Atomics | Lock-free atomic updates (CAS) | Counters, lock-free algorithms |

---

## 19. `volatile` & Atomic Variables

These weren't in the original article but are **essential interview territory** — they directly address the *visibility* and *atomicity* hazards.

### `volatile` — visibility, not atomicity

Without synchronization, a thread may cache a variable and never see another thread's update. `volatile` forces every read/write to go to main memory, so updates are immediately visible — and it prevents reordering across the access.

```java
private volatile boolean running = true;

public void stop()  { running = false; }            // instantly visible to all
public void loop()  { while (running) { /* work */ } } // actually sees the change
```

**Critical limitation:** `volatile` does **NOT** make `count++` safe — that's still a non-atomic read-modify-write. Use `volatile` for **flags** and **safely publishing a reference**, not for compound updates.

### Atomic variables — lock-free, atomic

`java.util.concurrent.atomic` (`AtomicInteger`, `AtomicLong`, `AtomicReference`, …) gives thread-safe single-variable updates **without locks**, using a CPU instruction called **Compare-And-Swap (CAS)**.

```java
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();        // atomic ++
counter.compareAndSet(5, 6);      // set to 6 only if currently 5
```

CAS means "if the value is still X, set it to Y; otherwise tell me it failed (and I'll retry)." This is how the counter from §8 can be made safe without `synchronized`:

```java
// Instead of synchronized increment:
private final AtomicInteger counter = new AtomicInteger();
public void increment() { counter.incrementAndGet(); }
```

**Two nuances worth knowing:**

- **ABA problem:** CAS only checks the value, not whether it changed and changed back (A→B→A). Fix with `AtomicStampedReference` (adds a version stamp).
- **`LongAdder` vs `AtomicLong`:** under heavy contention, `LongAdder` spreads writes across multiple internal cells and sums on read, giving much higher throughput for hot counters.

**Two full worked pieces live with the rest of the material for easy reference:** a thread-safe counter three ways (`synchronized` vs `AtomicLong` vs `LongAdder`) is in **§30 (Part 6)**, and the complete `count++ → synchronized → volatile → AtomicInteger → CAS → JMM → happens-before` progression is in **§37 (Part 7)**.

---

# Part 5 — High-Level Concurrency

## 20. Thread Pools & the Executor Framework

Creating a `new Thread()` per task doesn't scale: thread creation is expensive, and unbounded threads exhaust memory. The **Executor Framework** (Java 5+, in `java.util.concurrent`) decouples *submitting* a task from *how/when* it runs. You hand tasks to a pool; the pool reuses a fixed set of worker threads.

```mermaid
graph LR
    T1[Task 1] --> Q[(Work Queue)]
    T2[Task 2] --> Q
    T3[Task 3] --> Q
    Q --> W1[Worker Thread 1]
    Q --> W2[Worker Thread 2]
    Q --> W3[Worker Thread 3]
```

Why pools win: **performance** (reuse threads, no repeated creation), **resource control** (cap concurrent threads), **simpler error handling**, and **scalability** for many short tasks.

### 20.1 The analogy: a taxi dispatch service

Creating a `new Thread()` per task is like **building a brand-new car for every passenger and scrapping it at the destination** — absurdly wasteful. A thread pool is a **taxi company**:

- A fixed fleet of cars (worker threads) is kept running.
- Passengers (tasks) call in and wait in a **dispatch queue** (the work queue).
- A free driver picks up the next passenger, completes the trip, then returns to pick up another — the **car is reused**, not rebuilt.
- If all cars are busy and the waiting line is full, the dispatcher applies a **policy**: turn the passenger away, make the *caller* drive themselves, or bump someone from the queue (the rejection policies).

This captures the whole framework: a bounded fleet + a queue + a rejection policy = predictable resource usage under any load.

### 20.2 Architecture: the Executor framework's layers

The framework is a small hierarchy of interfaces, which is worth knowing by name:

```
Executor                  ── execute(Runnable): the bare "run this" contract
   │
   ▼
ExecutorService           ── adds lifecycle (shutdown) + submit()/invokeAll()
   │                          returning Futures
   ▼
ScheduledExecutorService  ── adds schedule()/scheduleAtFixedRate() for delays
   │
   ▼
ThreadPoolExecutor        ── the concrete workhorse: core/max threads,
ScheduledThreadPoolExecutor   work queue, thread factory, rejection handler
ForkJoinPool              ── work-stealing pool for recursive/parallel tasks
```

`Executors` (the utility class) is just a set of **factory methods** that pre-configure a `ThreadPoolExecutor` for you. Understanding that `newFixedThreadPool(n)` is literally `new ThreadPoolExecutor(n, n, 0L, MILLISECONDS, new LinkedBlockingQueue<>())` is what lets you reason about its (dangerous, unbounded-queue) behavior.

### 20.3 Deep dive: how ThreadPoolExecutor processes a task

When you call `execute(task)`, the executor follows a strict decision sequence. This is the single most-asked thread-pool interview topic:

```mermaid
graph TD
    S[execute task] --> A{poolSize < corePoolSize?}
    A -->|Yes| B[Start a NEW core thread to run it]
    A -->|No| C{Can enqueue?<br/>queue not full}
    C -->|Yes| D[Add task to work queue]
    C -->|No| E{poolSize < maxPoolSize?}
    E -->|Yes| F[Start a NEW non-core thread]
    E -->|No| G[Apply REJECTION policy]
```

In words: **(1)** below core size → always spin up a new core thread (even if others are idle); **(2)** at/above core size → try to **queue** the task; **(3)** queue full and below max size → create a temporary non-core thread; **(4)** queue full and at max size → **reject**. A subtle consequence: with an **unbounded queue** (the default `newFixedThreadPool`), step 2 never fails, so the pool **never grows past core size and never rejects** — it just queues forever until memory runs out. That's the production trap.

The worker threads themselves run a simple loop internally: take a task from the queue (blocking if empty), run it, repeat. Idle non-core threads above `corePoolSize` are reaped after `keepAliveTime`. This loop is why a single uncaught exception matters — it can terminate a worker (the pool replaces it, but the task's failure is swallowed unless you handle it).

### 20.4 The five constructor parameters (know each cold)

```java
new ThreadPoolExecutor(
    corePoolSize,      // threads kept alive even when idle
    maximumPoolSize,   // hard ceiling on worker threads
    keepAliveTime,     // how long idle non-core threads survive
    unit,
    workQueue,         // where tasks wait: bounded vs unbounded changes everything
    threadFactory,     // names threads, sets daemon/priority — vital for debugging
    rejectionHandler   // what to do when saturated
);
```

The **work queue** choice is architectural:
- `LinkedBlockingQueue` (unbounded) → pool stays at core size, risk of OOM.
- `ArrayBlockingQueue` (bounded) → enables growth to max + back-pressure via rejection. **Preferred for production.**
- `SynchronousQueue` (zero capacity, hand-off) → every task needs an immediately-available thread; used by `newCachedThreadPool`, which is why it grows threads without bound.

The **rejection policies** (`RejectedExecutionHandler`):
- `AbortPolicy` (default) — throws `RejectedExecutionException`.
- `CallerRunsPolicy` — runs the task on the *submitting* thread; naturally throttles producers (back-pressure) since they can't submit more while busy.
- `DiscardPolicy` — silently drops the new task.
- `DiscardOldestPolicy` — drops the oldest queued task and retries.

### The four built-in pool types

| Factory method | Behavior | Use case |
|---|---|---|
| `newFixedThreadPool(n)` | Exactly `n` threads; extra tasks queue up | Predictable, steady workloads |
| `newCachedThreadPool()` | Creates threads on demand, reuses idle ones, reaps after 60s idle | Many short-lived async tasks |
| `newScheduledThreadPool(n)` | Runs tasks after a delay or periodically | Heartbeats, periodic cleanup, cron-like jobs |
| `newSingleThreadExecutor()` | One thread, tasks run sequentially in order | Logging, event handling, ordered tasks |

<details>
<summary><b>Example: fixed thread pool running 6 tasks on 3 threads</b></summary>

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FixedThreadPoolExample {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        Runnable task = () -> {
            System.out.println(Thread.currentThread().getName() + " executing a task.");
            try { Thread.sleep(1000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        };

        for (int i = 1; i <= 6; i++) executor.execute(task);

        executor.shutdown();    // no new tasks; lets running ones finish
    }
}
```

Only 3 tasks run at once; the other 3 wait in the queue until a worker is free.

</details>

<details>
<summary><b>Example: scheduled pool for periodic execution</b></summary>

```java
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledThreadPoolExample {
    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        Runnable task = () ->
            System.out.println(Thread.currentThread().getName() + " running scheduled task.");

        scheduler.schedule(task, 3, TimeUnit.SECONDS);                 // once, after 3s
        scheduler.scheduleAtFixedRate(task, 1, 2, TimeUnit.SECONDS);   // every 2s after 1s
    }
}
```

</details>

### Production tip: build your own `ThreadPoolExecutor`

The convenience factories hide a dangerous detail: `newFixedThreadPool` uses an **unbounded queue** (tasks pile up → OutOfMemoryError under load), and `newCachedThreadPool` allows an **unbounded thread count**. In serious systems, construct the pool explicitly so you control the queue bound and what happens when it's full:

```java
ThreadPoolExecutor pool = new ThreadPoolExecutor(
    4,                                   // core threads
    8,                                   // max threads
    60, TimeUnit.SECONDS,                // idle keep-alive
    new ArrayBlockingQueue<>(100),       // BOUNDED queue → back-pressure
    new ThreadPoolExecutor.CallerRunsPolicy()  // rejection policy when full
);
```

How it decides to grow: use core threads → if busy, queue the task → if queue full, add threads up to max → if still full, apply the **rejection policy** (`AbortPolicy` default/throws, `CallerRunsPolicy`, `DiscardPolicy`, `DiscardOldestPolicy`).

### Sizing the pool

- **CPU-bound tasks:** ≈ number of CPU cores (sometimes cores + 1).
- **I/O-bound tasks:** more threads, since they spend time waiting — roughly `cores × (1 + waitTime/computeTime)`. (Or use virtual threads — §22.)

### Always handle exceptions and shut down

An uncaught exception in a task can silently kill a worker thread. Wrap task bodies in try/catch, and always `shutdown()` (or `shutdownNow()`) when done.

<details>
<summary><b>Example: graceful shutdown</b></summary>

```java
executor.shutdown();    // stop accepting new tasks
try {
    if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        executor.shutdownNow();   // force-cancel remaining tasks
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

</details>

---

## 21. Callable, Future & CompletableFuture

This is one of the two most important sections for interviews (along with virtual threads). Take your time here — async result-handling comes up constantly.

### 21.1 The analogy: ordering food

Think of the three abstractions as ways of getting a meal:

- **`Runnable` = drop off laundry.** You hand over a task and walk away. You get nothing back, and you can't easily tell when it's done.
- **`Callable` + `Future` = a restaurant buzzer.** You order (submit a `Callable`), and you're handed a buzzer (`Future`). You can do other things, but to actually *eat* you must walk back to the counter and **wait there** until the buzzer goes off — `future.get()` blocks you at the counter. If you want to combine two dishes from two restaurants, you stand at each counter in turn.
- **`CompletableFuture` = food delivery with instructions.** You order and say *"when it arrives, reheat it, plate it, and text me; if the kitchen fails, send a backup order."* You never stand and wait — you describe the whole pipeline up front (`thenApply`, `thenCompose`, `exceptionally`) and it runs itself as each stage completes. Two orders from two kitchens can be told to *"combine into one plate when both arrive"* (`thenCombine`).

The leap from `Future` to `CompletableFuture` is the leap from **pull** (you block and ask "is it done yet?") to **push** (you register what should happen and the result flows through your pipeline).

### 21.2 Runnable vs. Callable

`execute(Runnable)` is fire-and-forget: `run()` returns `void` and can't throw checked exceptions. When you need a **result** or want to throw checked exceptions, use `Callable<T>`, whose `call()` returns a `T`.

| | `Runnable` | `Callable<T>` |
|---|---|---|
| Method | `void run()` | `T call() throws Exception` |
| Returns a value? | No | Yes (`T`) |
| Can throw checked exceptions? | No | Yes |
| Submit via | `execute()` or `submit()` | `submit()` |
| You get back | `void` / `Future<?>` | `Future<T>` |

### 21.3 Future — the result you'll pick up later

When you `submit()` a `Callable` (or `Runnable`) to an `ExecutorService`, you immediately get a `Future<T>` — a *handle* to a result that may not exist yet. The task runs on a pool thread; you continue on yours.

`Future`'s core methods:

| Method | What it does |
|---|---|
| `get()` | **Blocks** until the result is ready, then returns it (or throws `ExecutionException` wrapping the task's exception) |
| `get(timeout, unit)` | Blocks up to a limit, then throws `TimeoutException` |
| `cancel(mayInterruptIfRunning)` | Attempts to cancel; interrupts the running thread if `true` |
| `isDone()` | Non-blocking check — has it finished (normally, exceptionally, or cancelled)? |
| `isCancelled()` | Was it cancelled before completing? |

<details>
<summary><b>Example: Callable + Future (basic)</b></summary>

```java
import java.util.concurrent.*;

public class FutureExample {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Integer> task = () -> {
            Thread.sleep(500);          // simulate slow work
            return 6 * 7;
        };

        Future<Integer> future = executor.submit(task);
        System.out.println("Doing other work while the task runs...");

        Integer result = future.get();   // blocks here until the result is ready
        System.out.println("Result: " + result);   // 42

        executor.shutdown();
    }
}
```

</details>

<details>
<summary><b>Example: timeouts, cancellation & exception handling with Future</b></summary>

```java
import java.util.concurrent.*;

public class FutureControlExample {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<Integer> future = executor.submit(() -> {
            Thread.sleep(2000);
            return 100;
        });

        try {
            // Only willing to wait 1 second:
            Integer value = future.get(1, TimeUnit.SECONDS);
            System.out.println("Got: " + value);
        } catch (TimeoutException e) {
            System.out.println("Too slow — cancelling.");
            future.cancel(true);             // interrupt the running task
        } catch (ExecutionException e) {
            // The task threw — the real cause is wrapped here:
            System.out.println("Task failed: " + e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }
    }
}
```

Three things interviewers probe: `get()` **wraps** the task's exception in `ExecutionException` (call `getCause()`), `get(timeout)` throws `TimeoutException` but does **not** cancel the task for you, and `cancel(true)` only helps if the task responds to interruption.

</details>

**The fundamental limitation of `Future`:** the only way to consume the result is `get()`, which **blocks** the calling thread. You can't say "when this finishes, do X" without parking a thread on `get()`. You also can't easily chain two async steps or combine results without writing clumsy blocking glue. That's exactly what `CompletableFuture` fixes.

### 21.4 CompletableFuture — composable, non-blocking pipelines

`CompletableFuture<T>` (Java 8+) implements `Future<T>` but adds a fluent API to **declare a pipeline of stages** that run automatically as each prior stage completes — no blocking `get()` in the middle. It's Java's answer to building reactive-style async workflows with plain methods.

Two ways a `CompletableFuture` gets its value:
- **You start async work:** `supplyAsync(supplier)` (returns a value) or `runAsync(runnable)` (no value). These run on the **common ForkJoinPool** by default, or an executor you pass.
- **You complete it manually:** `new CompletableFuture<>()` then later `cf.complete(value)` — useful for bridging callback-based APIs into the `CompletableFuture` world.

#### The method families (know these cold)

| Family | Signature shape | What it's for | Returns |
|---|---|---|---|
| `thenApply` | `fn: T -> U` | **Transform** the result synchronously | `CF<U>` |
| `thenCompose` | `fn: T -> CF<U>` | **Chain** another async call (flat-map; avoids `CF<CF<U>>`) | `CF<U>` |
| `thenAccept` | `consumer: T -> void` | **Consume** the result, return nothing | `CF<Void>` |
| `thenRun` | `Runnable` | Run an action, ignoring the value | `CF<Void>` |
| `thenCombine` | `(other, (T,U)->V)` | **Join two independent** futures into one result | `CF<V>` |
| `allOf` / `anyOf` | `CF...` | Wait for **all** / **any** of many futures | `CF<Void>` / `CF<Object>` |
| `exceptionally` | `fn: Throwable -> T` | **Recover** from a failure with a fallback value | `CF<T>` |
| `handle` | `(T, Throwable) -> U` | Handle **both** success and failure in one place | `CF<U>` |
| `whenComplete` | `(T, Throwable) -> void` | Side-effect (logging) without changing the result | `CF<T>` |

**`thenApply` vs `thenCompose` is the #1 confusion.** Use `thenApply` when your function returns a plain value (`User -> String`). Use `thenCompose` when your function itself returns a `CompletableFuture` (`User -> CompletableFuture<Orders>`) — otherwise you'd get a nested `CompletableFuture<CompletableFuture<Orders>>`. It's exactly `map` vs `flatMap`.

**The `…Async` suffix:** every method has an `…Async` variant (`thenApplyAsync`, etc.). Without `Async`, the callback runs on **whatever thread completed the previous stage** (could be a pool thread, could be your thread). With `…Async`, it's submitted to the common pool (or an executor you pass). Use `…Async` (with your own executor) when a stage does blocking work, so you don't tie up the completing thread.

<details>
<summary><b>Example: a realistic CompletableFuture pipeline</b></summary>

```java
import java.util.concurrent.CompletableFuture;

public class CompletableFuturePipeline {
    public static void main(String[] args) {
        CompletableFuture<Void> pipeline = CompletableFuture
            .supplyAsync(() -> fetchUserId())              // 1. start async work -> 7
            .thenApply(id -> "user-" + id)                 // 2. transform (sync map) -> "user-7"
            .thenCompose(name -> loadProfileAsync(name))   // 3. chain another async call (flatMap)
            .thenAccept(profile -> System.out.println("Loaded: " + profile))  // 4. consume
            .exceptionally(ex -> {                         // 5. recover from any failure above
                System.err.println("Failed: " + ex.getMessage());
                return null;
            });

        pipeline.join();   // block ONCE at the very end (in main, for the demo)
    }

    static int fetchUserId() { return 7; }

    static CompletableFuture<String> loadProfileAsync(String name) {
        return CompletableFuture.supplyAsync(() -> name + " (profile)");
    }
}
```

Notice there's no blocking *inside* the pipeline — each stage fires when the previous one completes. The single `join()` at the end exists only because `main` would otherwise exit before the async work runs.

</details>

<details>
<summary><b>Example: combining independent calls (thenCombine & allOf)</b></summary>

```java
import java.util.concurrent.CompletableFuture;
import java.util.List;

public class CombineExample {
    public static void main(String[] args) {
        // thenCombine: join TWO independent async results
        CompletableFuture<Integer> price = CompletableFuture.supplyAsync(() -> 100);
        CompletableFuture<Integer> tax   = CompletableFuture.supplyAsync(() -> 18);
        CompletableFuture<Integer> total = price.thenCombine(tax, Integer::sum);
        System.out.println("Total: " + total.join());     // 118

        // allOf: wait for MANY futures, then gather results
        var f1 = CompletableFuture.supplyAsync(() -> "A");
        var f2 = CompletableFuture.supplyAsync(() -> "B");
        var f3 = CompletableFuture.supplyAsync(() -> "C");

        CompletableFuture<Void> all = CompletableFuture.allOf(f1, f2, f3);
        all.join();   // completes when ALL three are done
        // allOf returns CF<Void>, so collect each result after it completes:
        List<String> results = List.of(f1.join(), f2.join(), f3.join());
        System.out.println(results);                       // [A, B, C]
    }
}
```

`thenCombine` is for a fixed, small number of futures; `allOf` scales to a list. A common idiom is `allOf(...).thenApply(v -> futures.stream().map(CompletableFuture::join).toList())`.

</details>

<details>
<summary><b>Example: error handling — exceptionally vs handle vs whenComplete</b></summary>

```java
import java.util.concurrent.CompletableFuture;

public class ErrorHandlingExample {
    public static void main(String[] args) {
        // exceptionally: provide a fallback ONLY on failure
        CompletableFuture.supplyAsync(() -> { throw new RuntimeException("boom"); })
            .exceptionally(ex -> "fallback")
            .thenAccept(System.out::println);              // prints "fallback"

        // handle: see BOTH outcomes; can transform either way
        CompletableFuture.<Integer>supplyAsync(() -> 10)
            .handle((result, ex) -> ex != null ? -1 : result * 2)
            .thenAccept(System.out::println);              // prints 20

        // whenComplete: observe the outcome (e.g., logging) WITHOUT changing it
        CompletableFuture.supplyAsync(() -> "data")
            .whenComplete((result, ex) -> {
                if (ex != null) System.err.println("error: " + ex);
                else            System.out.println("got: " + result);
            });

        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

Rule of thumb: `exceptionally` = recover with a fallback value; `handle` = deal with success *and* failure and produce a new result; `whenComplete` = side-effect only (it re-throws the original exception downstream).

</details>

#### Architecture: how CompletableFuture executes

A `CompletableFuture` is a **node in a dependency graph**. Each stage you add (`thenApply`, etc.) registers a *dependent* on the previous stage. When a stage completes, it fires its dependents — handing the value forward. There's no central scheduler polling for completion; completion *pushes* the chain forward.

```
supplyAsync ──done──▶ thenApply ──done──▶ thenCompose ──done──▶ thenAccept
   (pool)              (transform)          (async call)          (consume)
        \                                                        /
         └──────────────── exceptionally (catches any failure) ─┘
```

The default executor is the **common ForkJoinPool** (`ForkJoinPool.commonPool()`), sized to roughly `cores - 1`. **This is a critical gotcha:** if you run *blocking* work (DB/HTTP calls) on the common pool, you can starve it — every other `parallelStream()` and `CompletableFuture` in the JVM shares it. For blocking stages, always pass **your own executor**: `supplyAsync(task, myExecutor)` and `thenApplyAsync(fn, myExecutor)`.

---

## 22. Virtual Threads (Project Loom)

Introduced in **Java 21** (JEP 444, after years of incubation as Project Loom), virtual threads are the biggest change to Java concurrency in two decades. This is essential, high-frequency interview material.

### 22.1 The analogy: waiters in a restaurant

Imagine a restaurant where each table needs a waiter for its whole visit.

- **Platform threads = one dedicated waiter per table.** A waiter takes your order, then *stands at your table doing nothing* while the kitchen cooks. If you have 1,000 tables, you need 1,000 waiters — most of them just standing around waiting. Hire too many and the payroll (memory) and the chaos of everyone crowding the kitchen (context switching) sink you. This is the platform-thread ceiling: threads spend most of their life **blocked on I/O**, yet each one holds an expensive OS thread hostage the whole time.

- **Virtual threads = a small, smart waitstaff that never stands idle.** When a waiter takes your order and the kitchen starts cooking, the waiter **immediately goes to serve another table** instead of standing around. When your food is ready, *any* free waiter picks it up and brings it. A handful of real waiters (carrier threads) can serve thousands of tables (virtual threads), because no waiter ever wastes time waiting.

The magic: **the "waiting" (blocked) virtual thread costs almost nothing** — it's just a small object on the heap, parked until its I/O completes. The expensive resource (the OS thread / waiter) is only used while actually *doing CPU work*, never while waiting.

### 22.2 The problem they solve

A traditional **platform thread** is a thin wrapper over an **OS thread** (1:1 mapping). Two costs follow:

1. **Memory:** each platform thread reserves a large stack (commonly ~1 MB). 10,000 threads ≈ 10 GB of stacks — you run out of memory long before you run out of useful work.
2. **Context-switching:** the OS scheduler switches between threads in the kernel, which is relatively expensive and doesn't scale to hundreds of thousands of threads.

So the JVM realistically handles only a few thousand platform threads. For a server handling tens of thousands of concurrent, mostly-*waiting* connections, this is a hard wall. Historically the workaround was **asynchronous / reactive programming** (callbacks, `CompletableFuture` chains, reactive streams) — which scales, but shatters simple sequential code into hard-to-read, hard-to-debug fragments (the "callback hell" / "colored functions" problem). Virtual threads let you **keep the simple blocking style and still scale**.

### 22.3 Architecture: how virtual threads actually work

A virtual thread is a `Thread` (same API) whose execution is managed by the **JVM**, not the OS. The JVM keeps a small pool of **carrier threads** (platform threads, by default a `ForkJoinPool` sized to the number of CPU cores). Virtual threads are **mounted** onto carriers to run, and **unmounted** when they block.

```mermaid
graph TD
    subgraph VTs["Millions of Virtual Threads (cheap heap objects)"]
        V1[VT-1]
        V2[VT-2]
        V3[VT-3]
        V4[VT-...]
        V5[VT-N]
    end
    SCHED{{JVM Scheduler<br/>ForkJoinPool}}
    V1 --> SCHED
    V2 --> SCHED
    V3 --> SCHED
    V4 --> SCHED
    V5 --> SCHED
    SCHED --> C1[Carrier Thread 1]
    SCHED --> C2[Carrier Thread 2]
    C1 --> OS1[OS Thread]
    C2 --> OS2[OS Thread]
    OS1 --> CPU[(CPU cores)]
    OS2 --> CPU
```

**The mount/unmount cycle — the heart of it:**

```
1. VT-1 mounted on Carrier-A, runs your code.
2. VT-1 calls a blocking op (e.g., socket read, Thread.sleep).
3. JVM UNMOUNTS VT-1: its stack is copied off the carrier and parked on the heap.
4. Carrier-A is now free → JVM MOUNTS VT-2 and runs it.
5. When VT-1's I/O completes, it becomes runnable again.
6. JVM MOUNTS VT-1 on ANY free carrier (maybe Carrier-B) to continue
   right after the blocking call — as if it never stopped.
```

The key insight: **the carrier thread is never blocked by a blocked virtual thread.** When you write `socket.read()` on a virtual thread, the JDK's I/O libraries have been rewritten to, under the hood, register interest in the I/O and *unmount* the virtual thread instead of blocking the OS thread. Your code *looks* synchronous and blocking; the runtime makes it non-blocking underneath. This is sometimes called "continuations" — the JVM captures the virtual thread's stack as a resumable continuation.

```mermaid
sequenceDiagram
    participant VT as Virtual Thread
    participant JVM as JVM Scheduler
    participant C as Carrier (OS) Thread
    participant IO as I/O Subsystem
    VT->>C: mounted, running code
    VT->>IO: blocking read() called
    Note over VT,C: JVM unmounts VT, copies stack to heap
    C-->>JVM: carrier freed
    JVM->>C: mount a DIFFERENT virtual thread
    IO-->>JVM: read completes
    JVM->>C: remount original VT (any free carrier)
    C->>VT: resumes right after read()
```

### 22.4 Creating virtual threads

There are three idiomatic ways:

<details>
<summary><b>Example: the three ways to create virtual threads</b></summary>

```java
import java.util.concurrent.Executors;

public class VirtualThreadsExample {
    public static void main(String[] args) throws InterruptedException {
        // 1) Start one directly (fluent builder):
        Thread vThread = Thread.ofVirtual().start(() ->
            System.out.println("Running in: " + Thread.currentThread()));
        vThread.join();

        // 2) Build (unstarted) with a name, start later:
        Thread named = Thread.ofVirtual().name("worker-1").unstarted(() ->
            System.out.println("hi from " + Thread.currentThread().getName()));
        named.start();
        named.join();

        // 3) RECOMMENDED: one virtual thread PER TASK via an executor.
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 10_000; i++) {     // ten thousand — no problem!
                int id = i;
                executor.submit(() -> {
                    Thread.sleep(500);             // blocking is cheap here
                    return id;
                });
            }
        } // try-with-resources closes the executor and waits for all tasks
    }
}
```

Note `Thread.currentThread()` prints something like `VirtualThread[#23]/runnable@ForkJoinPool-1-worker-3` — the part after `@` is the *carrier* it's currently mounted on.

</details>

### 22.5 Virtual vs. platform threads

| Feature | Platform thread | Virtual thread |
|---|---|---|
| Backed by | A dedicated OS thread (1:1) | A heap object, multiplexed onto carriers (M:N) |
| Creation cost | High (syscall, OS bookkeeping) | Very low (just an object) |
| Memory | ~1 MB reserved stack | ~hundreds of bytes to KBs, grows/shrinks |
| Blocking I/O | Blocks the underlying OS thread | Unmounts; the carrier is freed for others |
| Scheduling | OS kernel scheduler (preemptive) | JVM scheduler (cooperative, at blocking points) |
| How many feasible? | Thousands | Millions |
| Pooling | Pool them (creation is expensive) | **Don't pool** — create one per task |
| Best for | CPU-bound work | High-concurrency I/O-bound work |

> **Memory benchmark intuition (illustrative):** at ~10,000 concurrent tasks, platform threads need on the order of gigabytes of stack memory, while virtual threads need megabytes. At ~1,000,000 concurrent tasks, platform threads simply can't exist, while virtual threads remain feasible. The exact numbers depend on stack usage, but the order-of-magnitude difference is the whole point.

### 22.6 Scheduling: OS vs. JVM

- **Platform threads** are scheduled **preemptively** by the OS kernel: the OS can interrupt a thread at almost any instruction to run another. Heavyweight, but fair even for CPU hogs.
- **Virtual threads** are scheduled **cooperatively** by the JVM: a virtual thread yields its carrier at **blocking points** (I/O, `sleep`, lock waits). This is why virtual threads are perfect for I/O-bound work (lots of yield points) but **don't speed up CPU-bound work** — a virtual thread running a tight compute loop has no blocking points to yield at, so it just occupies a carrier exactly like a platform thread would. You're still bounded by core count for raw computation.

### 22.7 When to use — and when not to

**Use virtual threads for:** high-concurrency, **I/O-bound** workloads — web/microservice request handlers, fan-out calls to other services or databases, chat and real-time systems, web scraping, file processing. The win is being able to write straightforward `thread-per-request` blocking code that scales to tens of thousands of concurrent requests.

**Don't use them for:**

- **CPU-bound work** — you're limited by cores; use a sized `ThreadPoolExecutor` or `ForkJoinPool`/`parallelStream()` instead.
- **Low-concurrency apps** — if you only ever have a handful of threads, platform threads are simpler and there's no benefit.

### 22.8 Pitfalls & limitations (interviewers love these)

- **Don't pool virtual threads.** Pools exist because platform threads are expensive to create; virtual threads are cheap, so pooling adds contention for no benefit. Use `newVirtualThreadPerTaskExecutor()` (a *new* thread per task), not a fixed pool.
- **Pinning.** If a virtual thread blocks *while inside a `synchronized` block/method*, or during a native (JNI) call, it can be **pinned** to its carrier — it can't unmount, so the carrier is blocked, defeating the purpose. The fix is to prefer `ReentrantLock` over `synchronized` around blocking I/O in hot paths. (JDK 21 had this limitation; later versions, e.g. JDK 24's JEP 491, largely removed `synchronized` pinning — but knowing the concept matters.)
- **`ThreadLocal` at scale.** Each virtual thread has its own `ThreadLocal` copies. With millions of threads, heavy `ThreadLocal` caching wastes a lot of memory and defeats the usual "reuse across pooled threads" benefit. Prefer `ScopedValue` (a Loom companion feature) where possible.
- **Not faster, just more scalable.** Virtual threads don't make any single task run faster — they let you have *many more* concurrent tasks. Don't expect lower latency for one request; expect higher throughput under massive concurrency.
- **GC and observability.** Millions of threads mean millions of stacks on the heap → more GC pressure and harder debugging/thread-dump reading. Good tooling and monitoring matter.

---

# Part 6 — Classic Coding Problems (worked solutions)

These are the exact patterns interviewers ask you to code on a whiteboard. Each one is complete and runnable. Study the *coordination mechanism* in each — that's what's being tested.

## 23. Print Even & Odd Numbers with Two Threads

**Problem:** Two threads print numbers 1..N in order — one thread prints only odd numbers, the other only even — so the output is `1 2 3 4 5 …` with the work split between them.

**Idea:** Share a counter and a monitor. Each thread, when it holds the lock, checks whether the current number is "its turn." If yes, it prints and advances; if no, it `wait()`s. After printing it `notifyAll()`s the other thread.

<details>
<summary><b>Solution — wait()/notify() with a turn check</b></summary>

```java
public class PrintOddEven {
    private int number = 1;
    private final int max;
    private final Object lock = new Object();

    public PrintOddEven(int max) { this.max = max; }

    // printOdd = true  -> this thread prints odd numbers
    // printOdd = false -> this thread prints even numbers
    private void print(boolean printOdd) {
        synchronized (lock) {
            while (number <= max) {
                boolean isOdd = (number % 2 == 1);
                if (isOdd == printOdd) {
                    System.out.println(Thread.currentThread().getName() + " -> " + number);
                    number++;
                    lock.notifyAll();        // wake the other thread
                } else {
                    try {
                        lock.wait();         // not my turn: release lock and wait
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            lock.notifyAll();                // release the partner so it can exit
        }
    }

    public static void main(String[] args) throws InterruptedException {
        PrintOddEven p = new PrintOddEven(10);
        Thread odd  = new Thread(() -> p.print(true),  "ODD");
        Thread even = new Thread(() -> p.print(false), "EVEN");
        odd.start();
        even.start();
        odd.join();
        even.join();
    }
}
```

**Output:** `ODD -> 1`, `EVEN -> 2`, `ODD -> 3`, `EVEN -> 4`, … up to 10. The `while` loop (not `if`) is essential so a spuriously-woken thread re-checks whose turn it is.

</details>

<details>
<summary><b>Alternative — two Semaphores ping-ponging permits</b></summary>

```java
import java.util.concurrent.Semaphore;

public class PrintOddEvenSemaphore {
    public static void main(String[] args) {
        int max = 10;
        Semaphore oddTurn  = new Semaphore(1);   // odd starts (1 permit)
        Semaphore evenTurn = new Semaphore(0);   // even waits (0 permits)

        Runnable oddTask = () -> {
            for (int i = 1; i <= max; i += 2) {
                try { oddTurn.acquire(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                System.out.println("ODD  -> " + i);
                evenTurn.release();              // hand the turn to even
            }
        };
        Runnable evenTask = () -> {
            for (int i = 2; i <= max; i += 2) {
                try { evenTurn.acquire(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                System.out.println("EVEN -> " + i);
                oddTurn.release();               // hand the turn back to odd
            }
        };

        new Thread(oddTask).start();
        new Thread(evenTask).start();
    }
}
```

This version makes the hand-off explicit: each thread acquires its own permit, prints, then releases the *other* thread's permit. Often considered cleaner than `wait`/`notify`.

</details>

<details>
<summary><b>Alternative — `ReentrantLock` + `Condition` (the explicit-lock version)</b></summary>

Same turn-check logic as the first solution, but using an explicit lock and condition instead of the intrinsic monitor. Interviewers sometimes ask for this to check you can use the `java.util.concurrent.locks` API and that you always `unlock()` in a `finally`.

```java
import java.util.concurrent.locks.*;

public class PrintOddEvenLock {
    private int number = 1;
    private final int max;
    private final Lock lock = new ReentrantLock();
    private final Condition turn = lock.newCondition();   // one shared waiting room

    public PrintOddEvenLock(int max) { this.max = max; }

    private void print(boolean printOdd) {
        lock.lock();
        try {
            while (number <= max) {
                if ((number % 2 == 1) == printOdd) {      // is it my turn?
                    System.out.println(Thread.currentThread().getName() + " -> " + number);
                    number++;
                    turn.signalAll();                     // wake the partner
                } else {
                    turn.await();                         // not my turn: release lock and wait
                }
            }
            turn.signalAll();                             // let the partner exit
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        PrintOddEvenLock p = new PrintOddEvenLock(10);
        Thread odd  = new Thread(() -> p.print(true),  "ODD");
        Thread even = new Thread(() -> p.print(false), "EVEN");
        odd.start(); even.start();
        odd.join(); even.join();
    }
}
```

`Condition.await()`/`signalAll()` are the explicit-lock twins of `Object.wait()`/`notifyAll()` — same idea, but you must hold the `Lock` (not the monitor) and release it in `finally`.

</details>

---

## 24. Print in Sequence with Three Threads (Zero-Even-Odd style)

**Problem (a LeetCode-favorite):** Three threads cooperate to print `0102030405…`. One thread prints `0` before every number, one prints the odd numbers, one prints the even numbers.

**Idea:** Three semaphores act as turn tokens. `zero` starts with a permit; after printing `0` it releases either `odd` or `even` depending on the next value; those release `zero` again.

<details>
<summary><b>Solution — three Semaphores as turn tokens</b></summary>

```java
import java.util.concurrent.Semaphore;
import java.util.function.IntConsumer;

public class ZeroEvenOdd {
    private final int n;
    private final Semaphore zero = new Semaphore(1);   // print 0 first
    private final Semaphore even = new Semaphore(0);
    private final Semaphore odd  = new Semaphore(0);

    public ZeroEvenOdd(int n) { this.n = n; }

    public void zero(IntConsumer printNumber) throws InterruptedException {
        for (int i = 1; i <= n; i++) {
            zero.acquire();
            printNumber.accept(0);
            if (i % 2 == 1) odd.release();   // next is odd
            else            even.release();  // next is even
        }
    }

    public void odd(IntConsumer printNumber) throws InterruptedException {
        for (int i = 1; i <= n; i += 2) {
            odd.acquire();
            printNumber.accept(i);
            zero.release();
        }
    }

    public void even(IntConsumer printNumber) throws InterruptedException {
        for (int i = 2; i <= n; i += 2) {
            even.acquire();
            printNumber.accept(i);
            zero.release();
        }
    }

    public static void main(String[] args) {
        ZeroEvenOdd zeo = new ZeroEvenOdd(5);
        IntConsumer print = x -> System.out.print(x);

        new Thread(() -> safe(() -> zeo.zero(print))).start();
        new Thread(() -> safe(() -> zeo.odd(print))).start();
        new Thread(() -> safe(() -> zeo.even(print))).start();
        // Output: 0102030405
    }

    private static void safe(RunnableEx r) {
        try { r.run(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    interface RunnableEx { void run() throws InterruptedException; }
}
```

Semaphores shine for "strict ordering between N threads" problems — each permit is literally a baton being passed.

</details>

<details>
<summary><b>Alternative — `wait`/`notify` with a shared "state" turn variable</b></summary>

The same problem without semaphores: keep a small `state` field saying whose turn it is (0 = print zero, 1 = print odd, 2 = print even). Each thread waits until `state` matches its role, prints, updates `state`, and calls `notifyAll()`. This is the pattern you adapt for the closely-related **"print `ABCABCABC...` with three threads"** question — there `state` cycles 0→1→2→0 for A, B, C.

```java
public class ZeroEvenOddWaitNotify {
    private final int n;
    private int state = 0;      // 0 -> zero's turn, 1 -> odd's turn, 2 -> even's turn
    private int next = 1;       // the next number to print
    private final Object lock = new Object();

    public ZeroEvenOddWaitNotify(int n) { this.n = n; }

    public void zero() throws InterruptedException {
        synchronized (lock) {
            for (int i = 0; i < n; i++) {
                while (state != 0) lock.wait();          // wait for my turn
                System.out.print(0);
                state = (next % 2 == 1) ? 1 : 2;         // decide who prints the number
                lock.notifyAll();
            }
        }
    }

    public void odd() throws InterruptedException {
        synchronized (lock) {
            while (next <= n) {
                while (state != 1) { if (next > n) { lock.notifyAll(); return; } lock.wait(); }
                System.out.print(next++);
                state = 0;                               // back to zero's turn
                lock.notifyAll();
            }
        }
    }

    public void even() throws InterruptedException {
        synchronized (lock) {
            while (next <= n) {
                while (state != 2) { if (next > n) { lock.notifyAll(); return; } lock.wait(); }
                System.out.print(next++);
                state = 0;
                lock.notifyAll();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ZeroEvenOddWaitNotify z = new ZeroEvenOddWaitNotify(5);
        Runnable r0 = () -> run(z::zero), r1 = () -> run(z::odd), r2 = () -> run(z::even);
        Thread t0 = new Thread(r0), t1 = new Thread(r1), t2 = new Thread(r2);
        t0.start(); t1.start(); t2.start();
        t0.join(); t1.join(); t2.join();          // prints 0102030405
    }

    interface Ex { void run() throws InterruptedException; }
    static void run(Ex e) { try { e.run(); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); } }
}
```

The semaphore version is usually shorter to reason about for strict hand-offs; the `wait`/`notify` version generalizes more naturally when the "whose turn" logic is complex (like FizzBuzz with four threads).

</details>

### Related problem: print `ABCABCABC...` with three threads (three ways)

**Problem:** three threads — one that only prints `A`, one only `B`, one only `C` — must cooperate to produce `ABCABCABC...` for a fixed number of rounds. It's the same *turn-taking* skeleton as odd/even and Zero-Even-Odd, just with three participants in a cycle, so interviewers use it to see whether your coordination generalizes past two threads. Below are the three standard implementations and when each is preferred.

**In plain English:** each thread waits until it's its turn, prints its one letter, then tells the *next* thread it's now their turn. The only question is which tool does the "wait" and the "tell the next one."

<details>
<summary><b>Approach 1 — `wait`/`notify` with a shared turn counter</b></summary>

A single `turn` field cycles `0 → 1 → 2 → 0` (A, B, C). Each thread loops: wait in a `while` until `turn` equals its id, print, advance `turn`, then `notifyAll()`. Because all three wait on one monitor, you must use `notifyAll()` (a `notify()` might wake the wrong thread and stall).

```java
public class AbcWaitNotify {
    private int turn = 0;                 // 0 -> A's turn, 1 -> B, 2 -> C
    private final int rounds;
    private final Object lock = new Object();

    AbcWaitNotify(int rounds) { this.rounds = rounds; }

    private void print(int id, char c) throws InterruptedException {
        synchronized (lock) {
            for (int r = 0; r < rounds; r++) {
                while (turn != id) lock.wait();       // not my turn -> release lock and park
                System.out.print(c);
                turn = (turn + 1) % 3;                // hand the turn to the next thread
                lock.notifyAll();                     // wake everyone; only the right one proceeds
            }
        }
    }
    // Thread A calls print(0,'A'), B calls print(1,'B'), C calls print(2,'C') -> ABCABC...
}
```

</details>

<details>
<summary><b>Approach 2 — `ReentrantLock` + three `Condition`s (one waiting room per thread)</b></summary>

The weakness of Approach 1 is that `notifyAll()` wakes all three threads even though only one can proceed. With three separate `Condition`s you `signal()` **exactly** the next thread, so nobody wakes up for nothing — the cleanest version for more than two participants.

```java
import java.util.concurrent.locks.*;

public class AbcLockCondition {
    private int turn = 0;
    private final int rounds;
    private final Lock lock = new ReentrantLock();
    private final Condition[] conds = new Condition[3];    // one Condition per thread

    AbcLockCondition(int rounds) {
        this.rounds = rounds;
        for (int i = 0; i < 3; i++) conds[i] = lock.newCondition();
    }

    private void print(int id, char c) throws InterruptedException {
        for (int r = 0; r < rounds; r++) {
            lock.lock();
            try {
                while (turn != id) conds[id].await();      // park in MY room only
                System.out.print(c);
                turn = (turn + 1) % 3;
                conds[turn].signal();                      // wake ONLY the next thread
            } finally {
                lock.unlock();                             // always unlock in finally
            }
        }
    }
}
```

</details>

<details>
<summary><b>Approach 3 — three `Semaphore`s passed like a baton (usually the shortest)</b></summary>

Give each thread its own permit. A starts with one permit (so it goes first); every other semaphore starts empty. Each thread acquires *its* permit, prints, then releases the *next* thread's permit — the permit is literally a baton handed around the circle. No shared `turn` variable and no condition checks at all.

```java
import java.util.concurrent.Semaphore;

public class AbcSemaphore {
    private final int rounds;
    private final Semaphore sa = new Semaphore(1);   // A holds the baton first
    private final Semaphore sb = new Semaphore(0);
    private final Semaphore sc = new Semaphore(0);

    AbcSemaphore(int rounds) { this.rounds = rounds; }

    private void print(Semaphore mine, Semaphore next, char c) throws InterruptedException {
        for (int r = 0; r < rounds; r++) {
            mine.acquire();          // wait for my baton
            System.out.print(c);
            next.release();          // pass the baton to the next thread
        }
    }
    // A: print(sa, sb, 'A');  B: print(sb, sc, 'B');  C: print(sc, sa, 'C')  -> ABCABC...
}
```

**Trade-offs at a glance:** the **semaphore** version is the shortest and reads most naturally as a strict hand-off (each permit is a baton). The **`wait`/`notify`** version proves you understand monitors but is forced to `notifyAll()` and wake threads that can't proceed. The **`Lock` + per-thread `Condition`** version is the most precise — targeted `signal()` wakes only the next thread — and it's the pattern that scales best when the "whose turn" logic gets more complex (e.g., FizzBuzz across four threads).

</details>

---

## 25. Producer-Consumer (three ways: `wait`/`notify`, `Lock`+`Condition`, `BlockingQueue`)

**Problem:** Multiple producers generate items; multiple consumers process them; bound the buffer so producers slow down when consumers fall behind. This is *the* most-asked concurrency coding problem, and interviewers often want to see **all three** ways to solve it and the trade-offs between them.

**In plain English:** a producer keeps making things and a consumer keeps using them. If the shelf (buffer) is full, the producer must wait; if it's empty, the consumer must wait. All three solutions below implement exactly that rule — they only differ in *which tool* does the waiting and waking.

**Idea (the modern default):** don't hand-write `wait`/`notify` — use a `BlockingQueue`. `put()` blocks when full (natural back-pressure), `take()` blocks when empty. Use a "poison pill" to signal shutdown.

<details>
<summary><b>Solution — BlockingQueue with poison-pill shutdown</b></summary>

```java
import java.util.concurrent.*;

public class ProducerConsumerBlockingQueue {
    private static final Integer POISON_PILL = Integer.MIN_VALUE;

    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(10);  // bounded buffer

        Runnable producer = () -> {
            try {
                for (int i = 1; i <= 20; i++) {
                    queue.put(i);                       // blocks if the queue is full
                    System.out.println("Produced: " + i);
                }
                queue.put(POISON_PILL);                 // tell the consumer to stop
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        };

        Runnable consumer = () -> {
            try {
                while (true) {
                    Integer item = queue.take();        // blocks if the queue is empty
                    if (item.equals(POISON_PILL)) break;
                    System.out.println("   Consumed: " + item);
                    Thread.sleep(100);                  // simulate processing
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        };

        Thread p = new Thread(producer);
        Thread c = new Thread(consumer);
        p.start(); c.start();
        p.join();  c.join();
        System.out.println("Done.");
    }
}
```

Compare this to the `wait`/`notify` version below — same behavior, a fraction of the code and far less room for bugs. **This is what you'd actually ship.**

</details>

<details>
<summary><b>Approach 2 — hand-written `wait`/`notify`/`notifyAll` (what interviewers ask you to code)</b></summary>

This is the version you write on a whiteboard to prove you understand the primitives. The buffer is guarded by the object's monitor; a producer that finds the buffer full calls `wait()` (releasing the lock so a consumer can run), and every change calls `notifyAll()` to wake whoever can now proceed.

```java
import java.util.*;

public class ProducerConsumerWaitNotify {
    static class BoundedBuffer {
        private final Queue<Integer> queue = new LinkedList<>();
        private final int capacity;

        BoundedBuffer(int capacity) { this.capacity = capacity; }

        // Producer side.
        public synchronized void put(int item) throws InterruptedException {
            while (queue.size() == capacity) {   // WHILE, not if: re-check after waking
                wait();                          // buffer full -> release lock and park
            }
            queue.add(item);
            notifyAll();                         // wake any thread waiting (consumers here)
        }

        // Consumer side.
        public synchronized int take() throws InterruptedException {
            while (queue.isEmpty()) {            // WHILE guards against spurious wakeups
                wait();                          // buffer empty -> release lock and park
            }
            int item = queue.poll();
            notifyAll();                         // wake any thread waiting (producers here)
            return item;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        BoundedBuffer buffer = new BoundedBuffer(5);   // small buffer forces back-pressure

        Thread producer = new Thread(() -> {
            try { for (int i = 1; i <= 10; i++) { buffer.put(i); System.out.println("Produced " + i); } }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread consumer = new Thread(() -> {
            try { for (int i = 0; i < 10; i++) System.out.println("   Consumed " + buffer.take()); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        producer.start(); consumer.start();
        producer.join(); consumer.join();
    }
}
```

**Why `notifyAll()` and not `notify()` here?** Because producers and consumers wait on the *same* monitor. `notify()` might wake another producer when only a consumer can make progress, stalling the system (the lost-signal problem from §12). `notifyAll()` is always safe.

</details>

<details>
<summary><b>Approach 3 — `ReentrantLock` + two `Condition`s (the precise, efficient version)</b></summary>

The weakness of Approach 2 is that `notifyAll()` wakes *everyone*, including threads that still can't proceed. `ReentrantLock` lets you keep **two separate waiting rooms** — one for "buffer not full" and one for "buffer not empty" — so a producer only ever wakes consumers and vice-versa. This is exactly how `ArrayBlockingQueue` is implemented internally.

```java
import java.util.*;
import java.util.concurrent.locks.*;

public class ProducerConsumerLockCondition {
    static class BoundedBuffer {
        private final Queue<Integer> queue = new LinkedList<>();
        private final int capacity;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notFull  = lock.newCondition();   // producers wait here
        private final Condition notEmpty = lock.newCondition();   // consumers wait here

        BoundedBuffer(int capacity) { this.capacity = capacity; }

        public void put(int item) throws InterruptedException {
            lock.lock();
            try {
                while (queue.size() == capacity) notFull.await();  // only producers park here
                queue.add(item);
                notEmpty.signal();                                 // wake ONE consumer
            } finally {
                lock.unlock();                                     // ALWAYS unlock in finally
            }
        }

        public int take() throws InterruptedException {
            lock.lock();
            try {
                while (queue.isEmpty()) notEmpty.await();          // only consumers park here
                int item = queue.poll();
                notFull.signal();                                  // wake ONE producer
                return item;
            } finally {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        BoundedBuffer buffer = new BoundedBuffer(5);
        Thread producer = new Thread(() -> {
            try { for (int i = 1; i <= 10; i++) { buffer.put(i); System.out.println("Produced " + i); } }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread consumer = new Thread(() -> {
            try { for (int i = 0; i < 10; i++) System.out.println("   Consumed " + buffer.take()); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        producer.start(); consumer.start();
        producer.join(); consumer.join();
    }
}
```

**Trade-offs at a glance:** `BlockingQueue` is what you ship (least code, no bugs). `wait`/`notify` proves you understand monitors but forces `notifyAll()` and a single waiting room. `Lock`+`Condition` is the middle ground — more code than a queue, but targeted `signal()` on separate conditions avoids waking threads that can't proceed, which matters under heavy contention.

</details>

---

## 26. Run Tasks Concurrently with the Executor Framework

**Problem:** Run a batch of independent tasks across a fixed pool of worker threads and shut down cleanly.

<details>
<summary><b>Solution — fixed pool + graceful shutdown</b></summary>

```java
import java.util.concurrent.*;

public class ExecutorBatchExample {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(4);

        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            executor.execute(() -> {
                String worker = Thread.currentThread().getName();
                System.out.println("Task " + taskId + " started on " + worker);
                try { Thread.sleep(300); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                System.out.println("Task " + taskId + " finished on " + worker);
            });
        }

        executor.shutdown();                                   // stop accepting new tasks
        if (!executor.awaitTermination(1, TimeUnit.MINUTES)) { // wait for in-flight tasks
            executor.shutdownNow();                            // force-cancel if it overruns
        }
        System.out.println("All tasks complete.");
    }
}
```

Ten tasks share four threads; you'll see worker names like `pool-1-thread-1..4` reused across tasks. The `shutdown()` → `awaitTermination()` → `shutdownNow()` pattern is the correct, leak-free way to close a pool.

</details>

<details>
<summary><b>Bonus — submit a batch and collect results with invokeAll</b></summary>

```java
import java.util.*;
import java.util.concurrent.*;

public class InvokeAllExample {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            final int x = i;
            tasks.add(() -> x * x);            // each task returns its square
        }

        List<Future<Integer>> results = executor.invokeAll(tasks);  // runs all, waits for all
        for (Future<Integer> f : results) {
            System.out.println("Result: " + f.get());               // 1, 4, 9, 16, 25
        }
        executor.shutdown();
    }
}
```

`invokeAll` submits every task, blocks until all finish, and returns their `Future`s in order.

</details>

---

## 27. Parallel Sum with Callable & Future

**Problem:** Sum a large array by splitting it into chunks, computing each chunk on its own thread, and combining the partial results.

<details>
<summary><b>Solution — split into Callables, combine Futures</b></summary>

```java
import java.util.*;
import java.util.concurrent.*;

public class ParallelSum {
    public static void main(String[] args) throws Exception {
        int[] data = new int[1_000_000];
        Arrays.fill(data, 1);                          // sum should be 1,000,000

        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        int chunk = data.length / threads;
        List<Future<Long>> futures = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int start = t * chunk;
            final int end = (t == threads - 1) ? data.length : start + chunk;
            futures.add(executor.submit(() -> {        // Callable<Long>
                long partial = 0;
                for (int i = start; i < end; i++) partial += data[i];
                return partial;
            }));
        }

        long total = 0;
        for (Future<Long> f : futures) total += f.get();   // combine partial sums
        System.out.println("Total: " + total);             // 1000000

        executor.shutdown();
    }
}
```

This is the map-reduce pattern in miniature: divide the work, compute partials in parallel via `Callable`/`Future`, then reduce. (For pure divide-and-conquer, `ForkJoinPool` / `parallelStream()` automate the splitting.)

</details>

<details>
<summary><b>Alternative — `parallelStream()` and an explicit `ForkJoinPool` (let the library split the work)</b></summary>

The manual chunking above is exactly what a parallel stream does for you: it recursively splits the array (a *spliterator*), sums each piece on a `ForkJoinPool` worker, and combines the partials. One line replaces the whole executor dance:

```java
import java.util.*;
import java.util.concurrent.*;

public class ParallelSumStream {
    public static void main(String[] args) throws Exception {
        int[] data = new int[1_000_000];
        Arrays.fill(data, 1);

        // (a) Simplest: parallel stream uses the shared common ForkJoinPool.
        long total = Arrays.stream(data).parallel().asLongStream().sum();
        System.out.println("Total (parallelStream): " + total);   // 1000000

        // (b) Run it on YOUR OWN pool when you must not touch the shared common pool
        //     (e.g., to avoid starving other parallel work in the JVM).
        ForkJoinPool pool = new ForkJoinPool(4);
        try {
            long onMyPool = pool.submit(
                () -> Arrays.stream(data).parallel().asLongStream().sum()
            ).get();
            System.out.println("Total (custom ForkJoinPool): " + onMyPool);
        } finally {
            pool.shutdown();
        }
    }
}
```

**When to use which:** the manual `Callable`/`Future` split gives you full control (uneven chunks, per-chunk error handling). `parallelStream()` is far less code and ideal for CPU-bound, side-effect-free reductions over large collections — but note it runs on the JVM-wide **common pool** by default, so wrap it in your own `ForkJoinPool` (as in (b)) when you can't afford to share, and **never** put blocking I/O inside a parallel stream.

</details>

---

## 28. Fan-out HTTP Calls with Virtual Threads

**Problem:** Make many independent blocking calls (e.g., fetch 1,000 URLs) concurrently without drowning in OS threads.

**Idea:** Give each task its own virtual thread. Blocking is cheap — the JVM unmounts a blocked virtual thread and reuses its carrier — so a thread-per-task model scales to thousands of concurrent I/O calls with simple, readable code.

<details>
<summary><b>Solution — newVirtualThreadPerTaskExecutor (Java 21+)</b></summary>

```java
import java.util.*;
import java.util.concurrent.*;

public class VirtualThreadFanOut {
    public static void main(String[] args) throws Exception {
        List<String> urls = List.of(
            "https://example.com/a",
            "https://example.com/b",
            "https://example.com/c"
            // ... imagine 1,000 of these
        );

        // One virtual thread per task — cheap even at thousands of tasks.
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (String url : urls) {
                futures.add(executor.submit(() -> fetchLength(url)));  // blocking call is fine
            }
            int total = 0;
            for (Future<Integer> f : futures) total += f.get();
            System.out.println("Total bytes fetched: " + total);
        } // try-with-resources closes the executor and waits for all tasks
    }

    // Simulates a blocking I/O call (replace with a real HttpClient request).
    static int fetchLength(String url) throws InterruptedException {
        Thread.sleep(200);          // pretend network latency
        return url.length();
    }
}
```

The same code with a fixed platform-thread pool would either cap your concurrency or exhaust memory at high thread counts. With virtual threads you simply create one per task. **Don't pool virtual threads** — creating them is the cheap part.

</details>

<details>
<summary><b>Structured concurrency variant (preview) — all-or-nothing fan-out</b></summary>

```java
import java.util.concurrent.StructuredTaskScope;

public class StructuredFanOut {
    record Page(String url, int length) {}

    static Page load(String url) throws InterruptedException {
        Thread.sleep(200);
        return new Page(url, url.length());
    }

    public static void main(String[] args) throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var a = scope.fork(() -> load("https://example.com/a"));
            var b = scope.fork(() -> load("https://example.com/b"));

            scope.join();              // wait for both
            scope.throwIfFailed();     // if either failed, cancel the other & throw

            System.out.println(a.get());
            System.out.println(b.get());
        }
    }
}
```

Structured concurrency ties related subtasks into one unit: if one fails, the siblings are cancelled automatically — no leaked threads. (Preview/incubating in recent JDKs.)

</details>

---

## 29. Thread-Safe Singleton

**Problem:** Create a single shared instance, lazily, that's safe under concurrent access.

<details>
<summary><b>Solution — static holder idiom (recommended)</b></summary>

```java
public class Config {
    private Config() { /* expensive init */ }

    private static class Holder {
        private static final Config INSTANCE = new Config();
    }

    public static Config getInstance() {
        return Holder.INSTANCE;     // class is loaded & initialized lazily, thread-safely
    }
}
```

The JVM guarantees a class is initialized exactly once, in a thread-safe way, on first use. `Holder` isn't loaded until `getInstance()` is first called — so it's lazy *and* needs no synchronization. This is the cleanest correct answer.

</details>

<details>
<summary><b>Alternative — double-checked locking (know why volatile is required)</b></summary>

```java
public class Config {
    private static volatile Config instance;   // volatile is ESSENTIAL
    private Config() {}

    public static Config getInstance() {
        if (instance == null) {                 // first check (no lock)
            synchronized (Config.class) {
                if (instance == null) {         // second check (with lock)
                    instance = new Config();
                }
            }
        }
        return instance;
    }
}
```

Without `volatile`, another thread could see a non-null but *partially constructed* object, because the allocation and constructor can be reordered. `volatile` forbids that reordering. The static-holder idiom avoids this subtlety entirely — prefer it unless asked specifically about DCL.

</details>

<details>
<summary><b>The other three idioms interviewers expect you to name</b></summary>

The Singleton question is really a test of the whole memory-model toolkit, so know all the common variants and their trade-offs:

```java
// 1) EAGER initialization — simplest and thread-safe (the JVM builds it once at class load).
//    Downside: created even if never used; can't do lazy or error-handling init.
public class EagerSingleton {
    private static final EagerSingleton INSTANCE = new EagerSingleton();
    private EagerSingleton() {}
    public static EagerSingleton getInstance() { return INSTANCE; }
}

// 2) SYNCHRONIZED METHOD — lazy and correct, but every call pays for locking.
//    Fine if getInstance() is rarely called; too slow on a hot path.
public class SyncSingleton {
    private static SyncSingleton instance;
    private SyncSingleton() {}
    public static synchronized SyncSingleton getInstance() {
        if (instance == null) instance = new SyncSingleton();
        return instance;
    }
}

// 3) ENUM — Joshua Bloch's recommended approach. Thread-safe and lazy by the JVM's
//    class-init guarantee, and uniquely it is safe against reflection AND serialization
//    attacks (which can break the other idioms by creating a second instance).
public enum EnumSingleton {
    INSTANCE;
    public void doWork() { /* ... */ }
}
```

**How to choose:** use **enum** when you want the strongest guarantees (reflection- and serialization-safe) and don't need lazy loading tied to parameters. Use the **static holder** (previous tab) when you want laziness with zero locking. Use **eager** when the object is cheap and always needed. Use the **synchronized method** only when `getInstance()` is called rarely. Reach for **double-checked locking** only when specifically asked — it's the most error-prone (the `volatile` is mandatory and easy to forget).

</details>

---

## 30. Thread-Safe Counter (Three Ways)

"Build a thread-safe counter" is a deceptively deep interview question because the *best* answer depends on the contention level. All three below are correct; they differ only in performance.

<details>
<summary><b>Solution — `synchronized` vs `AtomicLong` vs `LongAdder`</b></summary>

```java
import java.util.concurrent.atomic.*;

public class Counters {
    // 1) synchronized: correct, simple. One lock -> threads serialize on every increment.
    //    Fine at low contention; the lock becomes the bottleneck when many threads hammer it.
    static class SyncCounter {
        private long count;
        public synchronized void increment() { count++; }
        public synchronized long get() { return count; }
    }

    // 2) AtomicLong: lock-free via CAS. Faster than a lock at low/medium contention,
    //    but under HIGH contention many threads retry the same CAS on one memory location
    //    ("cache-line bouncing"), which caps throughput.
    static class AtomicCounter {
        private final AtomicLong count = new AtomicLong();
        public void increment() { count.incrementAndGet(); }
        public long get() { return count.get(); }
    }

    // 3) LongAdder: spreads increments across several internal cells (one per hot thread),
    //    so threads rarely collide; get()/sum() adds the cells up. Best for write-heavy,
    //    read-rarely counters (metrics, hit counts). Slightly more memory; sum() is a snapshot.
    static class AdderCounter {
        private final LongAdder count = new LongAdder();
        public void increment() { count.increment(); }
        public long get() { return count.sum(); }
    }
}
```

**Decision rule:** at low contention any of them is fine — pick `AtomicLong` for a clean lock-free API. Under heavy write contention (many threads incrementing a shared metric), pick **`LongAdder`**. Use **`synchronized`** when the increment is part of a larger compound invariant that also needs the lock (i.e., you're updating more than just the one number atomically).

</details>

---

## 31. Rate Limiter (Token Bucket, Sliding Window, Distributed)

A `Semaphore` is the natural in-process building block for a rate limiter. The solution below builds a token-bucket limiter with a background refill, then covers the sliding-window and distributed (Redis) variants interviewers ask for next.

<details>
<summary><b>Applied example — a simple rate limiter (token bucket) with a Semaphore</b></summary>

A very common follow-up is *"limit this to N operations per second."* A semaphore models a **token bucket** neatly: the permits are the tokens, `tryAcquire()` spends one (rejecting the request if none are left), and a scheduled job periodically refills the bucket back to capacity.

```java
import java.util.concurrent.*;

public class SemaphoreRateLimiter {
    private final Semaphore permits;
    private final int capacity;

    public SemaphoreRateLimiter(int permitsPerSecond) {
        this.capacity = permitsPerSecond;
        this.permits = new Semaphore(permitsPerSecond);
        // Refill the bucket once per second on a background scheduler.
        ScheduledExecutorService refiller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limiter-refill"); t.setDaemon(true); return t;
        });
        refiller.scheduleAtFixedRate(this::refill, 1, 1, TimeUnit.SECONDS);
    }

    // Non-blocking: returns false immediately if no tokens are left this second.
    public boolean tryAcquire() { return permits.tryAcquire(); }

    private void refill() {
        // Top the bucket back up to capacity (never above it).
        permits.release(capacity - permits.availablePermits());
    }

    public static void main(String[] args) {
        SemaphoreRateLimiter limiter = new SemaphoreRateLimiter(3);   // 3 requests / second
        int allowed = 0, rejected = 0;
        for (int i = 0; i < 10; i++) {
            if (limiter.tryAcquire()) allowed++; else rejected++;
        }
        System.out.println("allowed=" + allowed + " rejected=" + rejected); // allowed=3 rejected=7
    }
}
```

This is the in-process building block. Interviewers often extend it: *"make it a sliding window"* (track timestamps instead of a fixed bucket), or *"make it distributed"* (move the counter into Redis with an atomic `INCR`+`EXPIRE`, or a Lua script, so all app servers share one limit). The Java `Semaphore` version is the right answer for a single JVM.

</details>

---

## 32. Concurrent LRU Cache

<details>
<summary><b>Solution — thread-safe LRU cache</b></summary>

An **LRU (Least-Recently-Used) cache** holds a fixed number of entries and, when full, evicts the entry that hasn't been touched for the longest time. The elegant single-thread answer is `LinkedHashMap` with *access order* turned on and `removeEldestEntry` overridden — it keeps entries ordered by recency for free. The catch: `LinkedHashMap` is **not** thread-safe, and even a plain `get()` mutates its internal order, so concurrent reads can corrupt it. The simplest correct fix is to wrap every operation in a lock:

```java
import java.util.*;

public class LRUCache<K, V> {
    private final int capacity;
    private final LinkedHashMap<K, V> map;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        // accessOrder=true -> most-recently-accessed entry moves to the tail
        this.map = new LinkedHashMap<>(capacity, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LRUCache.this.capacity;   // auto-evict the least-recent
            }
        };
    }

    public synchronized V get(K key) { return map.get(key); }          // get() mutates order -> must lock
    public synchronized void put(K key, V value) { map.put(key, value); }
    public synchronized int size() { return map.size(); }
}
```

**Trade-off to raise:** one lock serializes *all* access, which becomes a bottleneck under heavy load. At scale you'd reach for a sharded/striped design or an off-the-shelf cache (Caffeine, Guava) that uses lock-striping and an approximate-LRU ("TinyLFU") policy instead of a global lock. If the interviewer only needs correctness, the locked `LinkedHashMap` is the crisp answer; if they push on throughput, describe striping.

</details>

---

## 33. Deadlock — Demonstration & Fix

The most common live-coding version of this is a **money-transfer** between two accounts, where each thread locks the accounts in a different order. Here is the bug and two standard fixes.

<details>
<summary><b>Solution — the deadlock, plus fix 1 (lock ordering) and fix 2 (tryLock + backoff)</b></summary>

```java
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class DeadlockFix {
    static class Account {
        final int id;
        double balance;
        final ReentrantLock lock = new ReentrantLock();
        Account(int id, double balance) { this.id = id; this.balance = balance; }
    }

    // THE BUG: transfer(a,b) locks a then b; transfer(b,a) locks b then a.
    // Run both directions concurrently and you get a circular wait -> deadlock.
    static void transferDeadlockProne(Account from, Account to, double amt) {
        from.lock.lock();
        try {
            to.lock.lock();                          // second thread is stuck here forever
            try { from.balance -= amt; to.balance += amt; }
            finally { to.lock.unlock(); }
        } finally { from.lock.unlock(); }
    }

    // FIX 1 — consistent global lock ordering: always lock the lower id first,
    // so no two threads can ever request the two locks in opposite orders.
    static void transferOrdered(Account from, Account to, double amt) {
        Account first  = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to   : from;
        first.lock.lock();
        try {
            second.lock.lock();
            try { from.balance -= amt; to.balance += amt; }
            finally { second.lock.unlock(); }
        } finally { first.lock.unlock(); }
    }

    // FIX 2 — tryLock with timeout + backoff: if I can't get BOTH locks, I release
    // what I have and retry, so a cycle can never persist. No ordering needed.
    static boolean transferTryLock(Account from, Account to, double amt) throws InterruptedException {
        while (true) {
            if (from.lock.tryLock(50, TimeUnit.MILLISECONDS)) {
                try {
                    if (to.lock.tryLock(50, TimeUnit.MILLISECONDS)) {
                        try { from.balance -= amt; to.balance += amt; return true; }
                        finally { to.lock.unlock(); }
                    }
                } finally { from.lock.unlock(); }
            }
            Thread.sleep(1);   // back off before retrying to break any live cycle
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Account a = new Account(1, 1000), b = new Account(2, 1000);
        // Using the ordered fix, these opposite-direction transfers never deadlock:
        Thread t1 = new Thread(() -> { for (int i = 0; i < 1000; i++) transferOrdered(a, b, 1); });
        Thread t2 = new Thread(() -> { for (int i = 0; i < 1000; i++) transferOrdered(b, a, 1); });
        t1.start(); t2.start(); t1.join(); t2.join();
        System.out.println("Total conserved = " + (a.balance + b.balance)); // 2000.0
    }
}
```

**How to detect it in production:** take a thread dump with `jstack <pid>` (or `jcmd <pid> Thread.print`). The JVM prints "Found one Java-level deadlock" and names the two threads and the locks each holds and wants — that cycle is your bug. Fix by imposing a global lock order (fix 1, preferred) or switching to `tryLock` with timeout (fix 2, when a natural ordering is hard to define).

</details>

---

## 34. Dining Philosophers

<details>
<summary><b>Solution — Dining Philosophers, deadlock-free via lock ordering</b></summary>

Five philosophers sit around a table with one fork between each pair; each needs **both** neighbouring forks to eat. The naive rule "pick up your left fork, then your right" **deadlocks**: if everyone grabs their left fork at once, everyone waits forever for a right fork that never frees — a textbook circular wait. It's the canonical demonstration of the four Coffman conditions. The cleanest fix is **resource ordering**: number the forks and require each philosopher to always pick up the **lower-numbered fork first**. That single rule breaks the circular-wait condition, so a cycle is impossible.

```java
import java.util.concurrent.locks.*;

public class DiningPhilosophers {
    static void philosopher(int id, int n, ReentrantLock[] forks) {
        int left = id, right = (id + 1) % n;
        int first = Math.min(left, right), second = Math.max(left, right);  // lower id first
        for (int k = 0; k < 3; k++) {
            forks[first].lock();
            try {
                forks[second].lock();
                try { /* eat */ }
                finally { forks[second].unlock(); }
            } finally { forks[first].unlock(); }
        }
    }
}
```

**Other valid fixes to mention:** allow at most `n-1` philosophers to sit at once (a `Semaphore(n-1)` — removes "hold and wait" as a possibility), use `tryLock` with timeout so a philosopher who can't get both forks puts the first one back and retries, or make one philosopher "left-handed" (asymmetric ordering). Resource ordering is the answer interviewers most want to hear because it generalizes to any multi-lock code.

</details>

---

## 35. Custom Thread Pool (From Scratch)

<details>
<summary><b>Solution — a custom thread pool from scratch</b></summary>

This tests whether you understand what `ThreadPoolExecutor` actually *is*: a set of long-lived worker threads pulling tasks off a shared **blocking queue**. The whole design is "workers loop forever, `take()` a task, run it." The `BlockingQueue` does all the coordination — workers block for free when there's no work, and `put()` gives back-pressure when the queue is full.

```java
import java.util.*;
import java.util.concurrent.*;

public class SimpleThreadPool {
    private final BlockingQueue<Runnable> queue;
    private final List<Thread> workers = new ArrayList<>();
    private volatile boolean running = true;

    public SimpleThreadPool(int nThreads, int queueCapacity) {
        queue = new LinkedBlockingQueue<>(queueCapacity);        // bounded -> back-pressure
        for (int i = 0; i < nThreads; i++) {
            Thread w = new Thread(this::workerLoop, "worker-" + i);
            workers.add(w);
            w.start();
        }
    }

    public void submit(Runnable task) throws InterruptedException { queue.put(task); }

    private void workerLoop() {
        while (running || !queue.isEmpty()) {                    // drain remaining tasks on shutdown
            try {
                Runnable task = queue.poll(100, TimeUnit.MILLISECONDS);
                if (task != null) task.run();
            } catch (InterruptedException e) {
                if (!running) return;
            }
        }
    }

    public void shutdown() { running = false; workers.forEach(Thread::interrupt); }  // graceful stop
}
```

**Points to raise:** a **bounded** queue is what makes this safe (unbounded → OOM under load); real pools add a **rejection policy** when the queue is full, distinguish **core vs max** threads that grow and shrink with demand, and support a clean two-phase shutdown (`shutdown()` then `shutdownNow()`). Also note you must catch exceptions *inside* the worker loop, otherwise one bad task kills a worker permanently and the pool silently shrinks.

</details>

---

## 36. Concurrent Web Crawler

<details>
<summary><b>Solution — a concurrent web crawler</b></summary>

A crawler fetches a page, extracts its links, and recursively fetches those — in parallel, but visiting each URL **exactly once**. The two concurrency hazards are (1) two threads visiting the same URL, and (2) knowing when the whole crawl is *done*. Solve (1) with a **`ConcurrentHashMap.newKeySet()`** and the atomic `add()` — it returns `false` if the URL was already present, so the thread that loses the race simply stops. Solve (2) by composing the recursion with **`CompletableFuture`** so a parent completes only when all its children do:

```java
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler {
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final ExecutorService exec;

    public WebCrawler(ExecutorService exec) { this.exec = exec; }

    public CompletableFuture<Void> crawl(String url) {
        if (!visited.add(url)) return CompletableFuture.completedFuture(null);  // atomic dedupe
        return CompletableFuture
            .supplyAsync(() -> fetchLinks(url), exec)                           // fetch off the pool
            .thenCompose(links -> {
                CompletableFuture<?>[] subs = links.stream()
                    .map(this::crawl)                                          // recurse in parallel
                    .toArray(CompletableFuture[]::new);
                return CompletableFuture.allOf(subs);                          // done when children done
            });
    }
    // fetchLinks(url) -> parse the page and return its outbound URLs
}
```

**Scaling notes to mention:** cap concurrency with a fixed pool (or virtual threads, since fetching is I/O-bound — see §14/§28), respect `robots.txt` and per-host rate limits (§31), add a depth/count bound so it terminates, and for a *distributed* crawler move the visited-set and frontier queue into shared infrastructure (Redis / a message queue) so many machines coordinate. The single-JVM version above is the right starting answer; grow it only if asked.

</details>

---

# Part 7 — Mastery

## 37. The Java Memory Model & the `count++` Progression

This section gathers, in one place, the memory-model ideas the rest of the guide depends on — the material interviewers probe when they ask *"why is that still wrong?"* about a shared variable.

**The three hazards.** Every concurrency bug is a failure of one of three guarantees. **Atomicity:** a multi-step operation (like read-modify-write) must happen as one indivisible unit, or two threads can interleave and corrupt it. **Visibility:** a write made by one thread must actually become observable to another — without coordination, a value can sit in a CPU register or per-core cache and never be seen. **Ordering:** the compiler and CPU may reorder instructions for speed; that is invisible in single-threaded code but can break assumptions across threads. Almost every tool in this guide exists to restore one or more of these.

**The visibility problem, concretely.** A plain `boolean running` flag written by one thread and read in another thread's loop may *never* be seen to change, because the reader can keep using a cached copy — the loop can spin forever. Marking the flag `volatile` (or accessing it under a lock) forces the read to observe the latest write.

**Why `count++` fails.** `count++` looks like one step but compiles to three — read `count`, add one, write back. Two threads can both read `5`, both compute `6`, and both write `6`, so one increment is silently lost. That is an **atomicity** failure (a lost update); the shared field also has a lurking **visibility** problem. This single example is the seed of the whole progression below.

**The Java Memory Model (JMM)** is the specification that says *what* one thread is guaranteed to see of another's writes, and *which* reorderings are forbidden. Without it, the runtime could optimize freely and correct multithreaded code would be impossible to write. The JMM's promises are expressed through **happens-before**: if action A happens-before action B, then A's effects are visible to B. The edges to memorize: program order within one thread; **an unlock happens-before a later lock** of the same monitor; **a volatile write happens-before a later read** of that field; `Thread.start()` happens-before the started thread's actions; a thread's actions happen-before another thread's return from its `join()`; and the relation is **transitive**. A shared write with **no** happens-before edge to a read is a **data race**, and its result is undefined.

**What `volatile` does and does not do.** `volatile` guarantees **visibility** (every read hits main memory, every write is flushed immediately) and **ordering** (it forbids reordering across the access and creates a happens-before edge). It does **not** guarantee **atomicity**: `volatile int count; count++` is *still* a race, because the read and the write remain two separate steps that can interleave. Correct uses are a stop/status flag and safe publication of an immutable object's reference; the wrong use is any read-modify-write such as a counter.

**CAS, ABA, and `LongAdder`.** Lock-free atomics (`AtomicInteger`, `AtomicLong`, `AtomicReference`) achieve atomicity without blocking using **compare-and-set (CAS)**: *"if the value is still the X I last read, set it to Y; otherwise report failure and I'll retry."* Two caveats: under very high contention, threads keep failing and retrying on one cache line, so a striped **`LongAdder`** (many cells, summed on read) wins for hot write-mostly counters; and CAS suffers the **ABA problem** (a value changes A→B→A and CAS can't tell) — use `AtomicStampedReference` when identity-over-time matters.

Putting it all together, the classic interview progression walks straight up this ladder:

<details>
<summary><b>The full chain: from a broken <code>count++</code> to happens-before</b></summary>

**1. `count++` (the bug).** `count++` looks atomic but compiles to three steps — read `count`, add one, write it back. Two threads can both read `5`, both compute `6`, both write `6`, so one increment is lost. This is a **race condition** and it breaks the **atomicity** guarantee. There is also a lurking **visibility** problem: even a single writer's update may sit in a CPU cache and never be seen by other threads.

**2. `synchronized` (the first correct fix).** Wrapping the increment in `synchronized` makes the read-modify-write **atomic** (only one thread inside at a time) *and* fixes **visibility** — the JMM guarantees that everything written before you release a monitor is visible to the next thread that acquires the same monitor ("unlock happens-before lock"). It works, but it is relatively heavy: threads that lose the race **block** and may be parked/woken by the OS, and it serializes every increment.

**3. `volatile` (fixes visibility, NOT atomicity).** A natural next guess is "just make `count` volatile." `volatile` guarantees **visibility** (every read hits main memory, every write is flushed immediately) and **ordering** (no reordering across the access; it establishes a happens-before edge). But it does **not** give **atomicity** — `volatile int count; count++` is *still* a race, because the read and the write are still two separate steps that can interleave. So `volatile` is the right tool for a **stop/status flag** or **safe publication of a reference**, but the wrong tool for a counter.

**4. `AtomicInteger` (lock-free atomicity).** `AtomicInteger.incrementAndGet()` gives you **atomicity without a lock**, using a CPU **compare-and-set (CAS)** instruction. It is the correct, high-performance answer for a shared counter at low-to-moderate contention — no blocking, no OS park/wake.

**5. CAS (how the atomic actually works).** CAS means: *"if memory still holds the value X I last read, set it to Y; otherwise tell me it failed and I'll re-read and retry."* It's an optimistic loop — no lock is ever taken. Two caveats to name: under **very high contention** many threads keep failing and retrying on the same cache line (which is why **`LongAdder`** — striped cells — wins for hot counters), and CAS is vulnerable to the **ABA problem** (a value goes A→B→A and CAS can't tell it changed) — fix with `AtomicStampedReference`.

**6. JMM (the rulebook underneath all of this).** Why do these tools work at all? Because the **Java Memory Model** defines what one thread is *guaranteed* to see of another's writes. Without the JMM's guarantees, the compiler and CPU are free to cache values in registers and **reorder** instructions for speed — perfectly fine for single-threaded code, disastrous across threads. The JMM is the contract that says "if you use these constructs, you get these visibility and ordering guarantees."

**7. happens-before (the JMM's core relation).** The JMM expresses all of this through **happens-before**: if action A happens-before action B, then A's effects are visible to B. The edges you must know: program order within one thread; **unlock happens-before a later lock** of the same monitor; **a volatile write happens-before a later read** of that field; `Thread.start()` happens-before the started thread's actions; a thread's actions happen-before another thread's return from its `join()`; and the relation is **transitive**. If there is *no* happens-before edge connecting a write and a read of shared data, you have a **data race** and the result is undefined. Every synchronizer in this guide — locks, `volatile`, atomics, latches, queues — works precisely because it creates happens-before edges.

**One-line summary of the ladder:** `count++` is not atomic → `synchronized` fixes atomicity + visibility but blocks → `volatile` fixes visibility/ordering but *not* atomicity → `AtomicInteger` fixes atomicity lock-free via CAS → CAS is an optimistic retry with ABA/contention caveats → the JMM is the rulebook and **happens-before** is the relation that makes any of it guaranteed.

</details>

---

## 38. Best Practices

A distilled checklist of the habits that prevent most concurrency bugs. In one line: **share as little as possible, lock for as short as possible, and use the highest-level tool that fits.**

<details>
<summary><b>The best-practices checklist (click to expand)</b></summary>

1. **Prefer high-level tools.** Use `java.util.concurrent` (executors, concurrent collections, synchronizers) instead of hand-rolling `wait`/`notify` and raw locks.
2. **Minimize shared mutable state.** The less you share, the fewer bugs. Favor immutability and confinement.
3. **Keep critical sections small.** Hold locks for the shortest time possible.
4. **Always unlock in `finally`.** For explicit locks, `lock()` then `try { } finally { unlock(); }`.
5. **Acquire multiple locks in a consistent global order** to prevent deadlock.
6. **Use `while`, not `if`, around `wait()`.** Guards against spurious wakeups.
7. **Bound your queues and pools.** Avoid `newFixedThreadPool`'s unbounded queue in production.
8. **Size pools to the workload:** cores for CPU-bound, more for I/O-bound.
9. **Never swallow `InterruptedException`** — propagate it or restore the interrupt flag.
10. **Always shut executors down** (`shutdown()` + `awaitTermination()`).
11. **Use atomics for simple counters**, locks/`synchronized` for compound invariants.
12. **Use `volatile` for visibility flags**, not for compound updates.
13. **Make check-then-act atomic.** If a decision and the action based on it must see the same state, put both inside one lock (or use an atomic method such as `putIfAbsent`/`computeIfAbsent`).
14. **Remember that "thread-safe" collections only make single operations atomic.** Guard any multi-step sequence with client-side locking on the collection, or use `ConcurrentHashMap` and its atomic `compute`/`merge` methods.
15. **Default to `StringBuilder`; use `StringBuffer` only when a builder is truly shared** — and prefer confining mutable state to one thread and sharing only immutable results.
16. **Prefer `notifyAll()` over `notify()`** unless every waiter is provably interchangeable.

</details>

---

## 39. Common Pitfalls & Anti-Patterns

These are the mistakes that show up most often in real code and interviews. Each one has bitten someone in production.

<details>
<summary><b>The anti-pattern list (click to expand)</b></summary>

- **`count++` on a `volatile`** — still racy; volatile ≠ atomic. Use an atomic or a lock.
- **`if` instead of `while` around `wait()`** — breaks on spurious wakeups / stolen signals.
- **Forgetting `unlock()` in `finally`** — a thrown exception leaks the lock permanently.
- **Calling `run()` instead of `start()`** — no new thread is created.
- **Locking on a mutable or shared object** (e.g., a `String` literal, a boxed `Integer`) — other code may lock on the same instance.
- **Inconsistent lock ordering** — the #1 cause of deadlock.
- **Unbounded thread pools / queues** — silent memory blow-up under load.
- **Sharing non-thread-safe objects** like `SimpleDateFormat` — use `DateTimeFormatter` (immutable) or a `ThreadLocal`.
- **Check-then-act on concurrent maps** — use `computeIfAbsent`/`putIfAbsent`/`merge` instead of `if (!map.containsKey(k)) map.put(...)`.
- **Swallowing `InterruptedException`** — destroys cancellation.
- **Relying on thread priority** for correctness — it's only a hint.
- **Compound operations on a synchronized collection** — `if (!list.isEmpty()) list.remove(0)` races even though each call is individually synchronized; lock the collection across the whole sequence.
- **Overloading `run()` instead of overriding it** — `public void run(String s)` is never called by `start()`; only the no-arg `run()` runs. Add `@Override` to catch this at compile time.
- **Using `notify()` when waiters aren't interchangeable** — it can wake the wrong waiter and lose the signal; prefer `notifyAll()`.
- **Sharing a `StringBuilder` across threads** — it isn't synchronized; use `StringBuffer` or confine the builder to one thread.

</details>

---

## 40. FAANG Interview Questions

Try to answer before expanding each one.

### Fundamentals

<details>
<summary><b>Q1: Process vs. thread? Concurrency vs. parallelism?</b></summary>

A **process** is an independent program with its own private memory space; two processes can't directly touch each other's data. A **thread** lives *inside* a process — many threads share the same heap (objects), but each thread gets its own stack and program counter (its own call chain and "current line"). Because threads share memory, they're cheap to create and switch between and can pass data just by reading shared objects — and that same sharing is exactly why concurrency bugs happen. **Concurrency** is a design idea: structure the program so multiple tasks are *in progress* at overlapping times (this works even on a single core, by rapidly switching between tasks). **Parallelism** is a hardware fact: tasks literally run *at the same instant* on multiple CPU cores. The one-liner: concurrency is about *dealing with* many things at once (structure); parallelism is about *doing* many things at once (execution). You can have concurrency without parallelism (one core, time-slicing), and parallelism is one way to *implement* concurrency.

</details>

<details>
<summary><b>Q2: Runnable vs. Thread vs. Callable — when to use each?</b></summary>

Think of it as *task* vs *worker*. `Runnable` and `Callable` describe the **work to do**; `Thread` is a **worker** that runs work. Prefer implementing **`Runnable`** by default: it keeps the task separate from the thread, and because it's an interface your class stays free to extend something else (Java has single inheritance, so extending `Thread` "uses up" that slot). Extend **`Thread`** only for tiny throwaway experiments — in real code you almost never do. Use a **lambda `Runnable`** for short inline tasks (`() -> doWork()`). Reach for **`Callable<T>`** when the task must **return a result** or is allowed to **throw a checked exception** — `Runnable.run()` can do neither. You don't run a `Callable` on a bare `Thread`; you submit it to an `ExecutorService`, which hands you a **`Future<T>`** to fetch the result later. Rule of thumb: fire-and-forget → `Runnable`; need a value back → `Callable` + executor.

</details>

<details>
<summary><b>Q3: What's the difference between start() and run()?</b></summary>

`start()` is the one that actually creates concurrency: it asks the JVM to spin up a **new thread of execution**, and that new thread then calls your `run()` method. Control returns to the caller immediately, so your code and the new thread now run side by side. Calling **`run()` directly** does *not* create a thread at all — it's just an ordinary method call that executes on the **current** thread, top to bottom, before the next line runs. So `t.run()` gives you zero concurrency, a very common trick-question mistake. A few precise rules interviewers check: you may call `start()` on a given `Thread` object **only once** — a second call throws `IllegalThreadStateException`, because a finished (or running) thread can't be restarted. And you can build **many** `Thread` objects around the **same** `Runnable`. One more trap: writing `run(String)` *overloads* rather than *overrides* `run()`, so `start()` silently ignores it (add `@Override` to catch that).

</details>

<details>
<summary><b>Q4: Explain the thread lifecycle states.</b></summary>

Java's `Thread.State` has **six** values. **NEW** — the object exists but `start()` hasn't been called yet. **RUNNABLE** — eligible to run; note Java folds "ready and waiting for a core" and "actually running on a core" into this one state, because picking which thread is on a core is the OS scheduler's job, not the JVM's. **BLOCKED** — parked trying to acquire an intrinsic **monitor lock** to enter a `synchronized` region another thread holds. **WAITING** — parked **indefinitely** until another thread signals it, via `Object.wait()`, `Thread.join()`, or `LockSupport.park()`. **TIMED_WAITING** — the same idea but with a deadline: `sleep(t)`, `wait(t)`, `join(t)`. **TERMINATED** — `run()` has finished (or threw); the thread can't be restarted. The distinction interviewers probe most is **BLOCKED vs WAITING**: BLOCKED is specifically about contending for a `synchronized` monitor lock, whereas a thread waiting on a `ReentrantLock` or `wait()` shows as WAITING. (Older "5-state" SCJP material calls these New / Runnable / Running / Waiting-Blocked-Sleeping / Dead — same idea, just merging some states.)

</details>

### Synchronization

<details>
<summary><b>Q5: What is a race condition and how do you fix it?</b></summary>

A **race condition** is when two or more threads touch shared data at the same time and the final result depends on the *exact timing* of who runs when — so the program gives different answers on different runs. The textbook case is `counter++`: it looks like one step but is really **three** (read the value, add one, write it back). Two threads can both read `5`, both compute `6`, and both write `6` — so two increments produce `6` instead of `7`, a **lost update**. There's a second flavor, **check-then-act** (also called TOCTOU): a thread checks a condition (`if (balance >= amount)`) and acts on it (`balance -= amount`), but another thread changes the balance in the gap between the check and the act, so the decision was made on stale data. You fix races by making the whole read-modify-write (or check-then-act) sequence **atomic**: guard it with `synchronized`/`ReentrantLock` so only one thread is inside at a time, or use a lock-free atomic type like `AtomicInteger.incrementAndGet()`. The mental checklist is: *shared + mutable + written concurrently + unsynchronized = race.* Remove any one of those and the race disappears.

</details>

<details>
<summary><b>Q6: How does the synchronized keyword work? What's an intrinsic lock?</b></summary>

Every Java object secretly carries a lock, called its **intrinsic lock** or **monitor**. When a thread enters a `synchronized` method or `synchronized(obj)` block, it **acquires** that object's monitor; when it leaves (normally or via an exception), it **releases** it. While one thread holds the monitor, any other thread that tries to enter a region guarded by the **same** lock must wait (it goes to the BLOCKED state). That gives you **mutual exclusion** — only one thread inside at a time. Just as important, `synchronized` also fixes **visibility**: the Java Memory Model guarantees that everything a thread wrote before releasing the lock becomes visible to the next thread that acquires it (this is the "unlock happens-before lock" rule). So a lock buys you two things at once — exclusive access *and* up-to-date data. Intrinsic locks are **reentrant**: a thread that already holds the lock can re-enter another `synchronized` method on the same object without deadlocking itself (the JVM just counts the nesting). This is why synchronized methods can safely call other synchronized methods on the same object.

</details>

<details>
<summary><b>Q7: Object lock vs. class lock — can they run concurrently?</b></summary>

There are two *different* locks in play. An **object (instance) lock** protects a specific instance: you take it with a `synchronized` instance method or `synchronized(this)`. A **class lock** protects the one shared `Class` object: you take it with a `static synchronized` method or `synchronized(Foo.class)`. The key insight is that these are **separate, independent locks**. So yes — a thread running a `synchronized` *instance* method and another thread running a `synchronized static` method **can run at the same time**, because they're holding different locks and don't block each other. This matters for correctness: `static` (shared-by-all-instances) data must be guarded by the **class** lock; if you mistakenly guard it with an instance lock, two threads working on two different instances hold two different locks and can corrupt the shared static field even though the code "looks synchronized." Also note two *different* instances each have their own object lock, so `synchronized` instance methods on instance A and instance B run concurrently — locking only serializes threads competing for the *same* lock.

</details>

<details>
<summary><b>Q8: Why must wait()/notify() be inside synchronized, and why a while loop?</b></summary>

`wait()`, `notify()`, and `notifyAll()` all operate directly on an object's **monitor**, so the calling thread must already **hold that monitor** — i.e., be inside a `synchronized` block/method on the same object. If you call them without holding the lock, you get an `IllegalMonitorStateException` at runtime. Here's the elegant part: `wait()` atomically **releases the lock and parks** the thread, which is essential — it lets *another* thread acquire the lock and change the condition you're waiting for; when you're notified, `wait()` **re-acquires the lock** before returning, so you're safely back in the critical section. You must always re-check the condition in a **`while` loop**, never an `if`, for two reasons: (1) **spurious wakeups** — the JVM is allowed to wake a waiter for no reason; and (2) by the time a notified thread actually re-acquires the lock, another thread may have already consumed the condition (a "stolen" signal). An `if` would blindly proceed on a stale assumption; a `while` re-tests and goes back to waiting if the condition still isn't true. The canonical idiom is `while (!condition) lock.wait();`.

</details>

<details>
<summary><b>Q9: synchronized vs. ReentrantLock?</b></summary>

Both provide **reentrant mutual exclusion** — one thread at a time, and the holder can re-enter. The difference is *convenience vs power*. `synchronized` is built into the language: it's simpler, less code, and — crucially — it **auto-releases** the lock when the block exits, even if an exception is thrown, so you can't leak it. Its limits: acquisition **blocks forever** (you can't give up), can't be interrupted, offers no fairness control, and gives you only **one** wait-set per lock. `ReentrantLock` is a class that adds the missing powers: `tryLock()` (take the lock only if free, or with a timeout — great for deadlock avoidance), `lockInterruptibly()` (a waiting thread can be cancelled), optional **fairness** (longest-waiter-first), and **multiple `Condition`s** so producers and consumers can wait in separate rooms. The price: you must manually `unlock()` in a **`finally`** block or you'll leak the lock permanently. Rule of thumb: use plain `synchronized` by default (simplest and safest), and reach for `ReentrantLock` only when you specifically need timed/interruptible locking, fairness, or multiple conditions.

</details>

<details>
<summary><b>Q10: What does volatile guarantee — and not guarantee?</b></summary>

`volatile` solves the **visibility** and **ordering** problems, but *not* atomicity. Normally each thread may cache a variable in a register or CPU cache and never notice another thread's update; marking a field `volatile` forces every read to come from, and every write to go to, main memory — so a write by one thread is **immediately visible** to all others. It also forbids the compiler/CPU from **reordering** operations around that access, and it establishes a **happens-before** edge (everything written before a volatile write is visible to a thread that later reads that volatile). What it does **NOT** give you is **atomicity of compound actions**: `volatile int count; count++` is *still* a race, because `count++` is read-modify-write — `volatile` makes each read and each write visible, but two threads can still interleave and lose an update. So the correct uses are: a simple **status/stop flag** (`volatile boolean running`), and **safe publication** of an immutable object's reference (write the fully-built object to a volatile field; readers see it fully constructed). For counters or any "read, change, write" logic, use an `AtomicInteger` or a lock instead.

</details>

### Coordination & design

<details>
<summary><b>Q11: CountDownLatch vs. CyclicBarrier vs. Semaphore?</b></summary>

All three are coordination tools, but they answer different questions. **`CountDownLatch`** is a **one-shot** gate: you start it with a count N, worker threads call `countDown()` as they finish work (or as events happen), and one or more threads sit in `await()` until the count hits zero, then proceed. It means *"wait until N things have happened"* — think a main thread waiting for 5 workers to finish. It **cannot be reset**; once it reaches zero it's done. **`CyclicBarrier`** is the mirror image and is **reusable**: N threads each call `await()` and *all* of them block until the **last one** arrives, then they're all released together (optionally running a barrier action first). It means *"everybody meet at this point, then we all go on together"* — think phased parallel computation where each phase must finish before the next begins; because it's cyclic, it automatically re-arms for the next phase. **`Semaphore`** is different in kind: it holds **N permits**, `acquire()` takes one (blocking if none left) and `release()` returns one, capping how many threads use a resource at once — *"at most N in the pool at a time"* (connection pools, rate limiting). Quick memory hook: latch = wait for events (one-shot), barrier = threads wait for each other (reusable), semaphore = limit concurrency (permits).

</details>

<details>
<summary><b>Q12: Design a producer-consumer system.</b></summary>

Start with the shippable answer: use a **`BlockingQueue`** (a bounded `ArrayBlockingQueue` or `LinkedBlockingQueue`). Producers call `put()`, which **blocks automatically when the queue is full** — that's your back-pressure, slowing producers down so they can't outrun consumers or blow up memory. Consumers call `take()`, which **blocks when the queue is empty**. The queue does all the `wait`/`notify` bookkeeping for you, so there's almost no room for bugs. Always **bound** the queue: an unbounded queue turns a slow consumer into an out-of-memory crash. For **shutdown**, use a *poison pill* (a special sentinel value that tells consumers to stop) or `poll(timeout)` combined with a volatile stop flag; if you have multiple consumers, put one poison pill per consumer. If the interviewer wants to see the fundamentals, show the manual **`wait`/`notify`** version with a `while`-loop condition and `notifyAll()` (§12/§25), and mention the middle option — **`ReentrantLock` + two `Condition`s** (`notFull`/`notEmpty`), which is how `ArrayBlockingQueue` is actually built and lets you wake only the right side. Be ready to discuss multiple producers/consumers, ordering guarantees, and why bounding matters.

</details>

<details>
<summary><b>Q13: How do you size and configure a thread pool for production?</b></summary>

Build a **`ThreadPoolExecutor` explicitly** rather than using the `Executors.newFixed/Cached` factories, because the factories hide dangerous defaults. Constructing it yourself lets you control all the levers: **core** and **maximum** pool size, **keep-alive** for idle non-core threads, a **bounded work queue** (this is what gives you back-pressure), a **thread factory** (so threads get meaningful names and daemon/priority settings — invaluable in a thread dump), and a **rejection policy** for when you're saturated. For **sizing**: CPU-bound work wants roughly **`cores` (or `cores + 1`)** threads, since more just cause context-switch churn; **I/O-bound** work wants **more**, scaled by how long tasks wait vs compute — a rough formula is `threads ≈ cores × (1 + waitTime/computeTime)`. Avoid `newFixedThreadPool` (it uses an **unbounded** queue → tasks pile up until `OutOfMemoryError`) and `newCachedThreadPool` (it can spawn a near-**unbounded number of threads** under a burst). For overload, a **bounded queue + `CallerRunsPolicy`** is a clean pattern — it throttles producers by running the task on the submitting thread. Finally, **monitor** queue depth and active count, and always **`shutdown()` + `awaitTermination()`** on the way out (and consider virtual threads if the work is I/O-bound).

</details>

### Advanced / staff-level

<details>
<summary><b>Q14: When would you choose virtual threads over a thread pool?</b></summary>

Choose **virtual threads** for **high-concurrency, I/O-bound** workloads — think web request handlers, database calls, or fanning out to many downstream services, where each task spends most of its time **waiting** rather than computing. The reason they win here is that blocking becomes almost free: when a virtual thread blocks on I/O, the JVM **unmounts** it from its carrier (a real OS thread) and lets the carrier run a different virtual thread, so a blocked task no longer wastes an expensive OS thread. That means you can have **hundreds of thousands or millions** of concurrent tasks while writing plain, sequential, **blocking** code with normal try/catch and readable stack traces — no reactive/callback complexity ("no function coloring"). They give **no benefit for CPU-bound** work: computation is still limited by the number of cores, and a tight loop has no blocking point to yield at — so for pure number-crunching, use a **fixed, sized platform-thread pool** instead. Three gotchas: **don't pool** virtual threads (creation is cheap; use `newVirtualThreadPerTaskExecutor()`, one per task), be careful with **`ThreadLocal`** at millions-of-threads scale (memory blow-up — prefer `ScopedValue`), and know about **pinning** (blocking inside `synchronized` or JNI can tie a virtual thread to its carrier, though newer JDKs have largely fixed the `synchronized` case).

</details>

<details>
<summary><b>Q15: How do you detect and fix a deadlock in production?</b></summary>

First, **confirm** it's a deadlock by taking a **thread dump** — `jstack <pid>` or `jcmd <pid> Thread.print` (or capture one from your APM). The JVM is genuinely helpful here: for intrinsic-lock deadlocks it prints **"Found one Java-level deadlock"** and shows the exact cycle — which thread holds which lock and which lock each is waiting for. That cycle *is* your bug: two (or more) threads acquiring the **same locks in opposite orders** (Thread-1 holds A wants B; Thread-2 holds B wants A). The primary **fix** is to impose a **consistent global lock-ordering** rule so all threads always acquire locks in the same order (e.g., by object id/hash), which makes a cycle impossible. Alternatives: shrink critical sections so you hold fewer locks at once, avoid calling unknown/callback code while holding a lock, or switch to **`tryLock()` with a timeout + backoff** so a thread that can't get all its locks releases what it has and retries. **Prevent recurrence** by codifying the lock-ordering convention, adding tests that hammer the concurrent paths, and using JFR / async-profiler to spot lock-contention hot spots before they become deadlocks. (Note: a dump won't auto-flag deadlocks on `ReentrantLock`/`Condition` the same way, but the stack traces still reveal the waiting cycle.)

</details>

<details>
<summary><b>Q16: A shared counter is a hot spot under heavy contention. Options?</b></summary>

At **low contention**, a `synchronized` counter or an `AtomicLong` (which uses lock-free **CAS**) is perfectly fine. The problem appears when the counter is **hot** — many threads incrementing the *same* memory location at once. With a lock, threads serialize and block each other; with `AtomicLong`, they don't block but they keep **failing and retrying** the CAS, and the cache line holding that value **bounces** between CPU cores' caches, which is expensive. The fix is **`LongAdder`**: instead of one value, it keeps an array of internal **cells** and lets different threads increment *different* cells, so they rarely collide; when you actually need the total, `sum()` adds the cells together. That trades a tiny bit of memory and an approximate/lazy read for **dramatically higher write throughput**. The deciding factor is the **read/write ratio**: if you write constantly but read rarely (metrics, hit counters, request counts), `LongAdder` wins big; if you read the exact current value as often as you write it, `AtomicLong` is simpler and better. So: measure contention, and reach for striping (`LongAdder`/`LongAccumulator`) only when a shared counter is a proven hot spot.

</details>

<details>
<summary><b>Q17 (coding): Print odd and even numbers 1..N using two alternating threads.</b></summary>

Use a shared monitor and a turn flag; each thread prints when it's its turn, then notifies the other and waits:

```java
class OddEven {
    private int n = 1;
    private final int max;
    private final Object lock = new Object();
    OddEven(int max) { this.max = max; }

    void run(boolean printOdd) {
        synchronized (lock) {
            while (n <= max) {
                if ((n % 2 == 1) == printOdd) {
                    System.out.println(Thread.currentThread().getName() + ": " + n++);
                    lock.notifyAll();
                } else {
                    try { lock.wait(); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                }
            }
            lock.notifyAll();   // release the partner at the end
        }
    }

    public static void main(String[] args) {
        OddEven oe = new OddEven(10);
        new Thread(() -> oe.run(true),  "Odd").start();
        new Thread(() -> oe.run(false), "Even").start();
    }
}
```

O(N) prints; each step is a guarded hand-off. Two `Semaphore`s ping-ponging permits also work.

</details>

### Future & CompletableFuture (deep dive)

<details>
<summary><b>Q18: Runnable vs. Callable vs. Future — how do they relate?</b></summary>

`Runnable.run()` returns `void` and can't throw checked exceptions — fire-and-forget. `Callable<T>.call()` returns a `T` and can throw checked exceptions — use it when you need a result. When you submit either to an `ExecutorService`, you get back a `Future<T>`, a handle to the eventual result. `Future` lets you block for the value (`get()`), poll (`isDone()`), bound the wait (`get(timeout)`), or cancel (`cancel()`). So: `Callable` produces the value, `Future` is how you retrieve it later.

</details>

<details>
<summary><b>Q19: What are the limitations of Future, and how does CompletableFuture address them?</b></summary>

`Future` has three big limits: (1) the only way to read the result is `get()`, which **blocks** the calling thread — there's no "call me back when done"; (2) you can't **chain** dependent async steps without nesting blocking calls; (3) you can't easily **combine** multiple futures or handle exceptions functionally. `CompletableFuture` fixes all three: it's *completion-driven* (register callbacks via `thenApply`/`thenAccept` that fire when the result is ready — no blocking), *composable* (`thenCompose` chains, `thenCombine`/`allOf` join), and has built-in error handling (`exceptionally`, `handle`). It moves you from a pull model to a push model.

</details>

<details>
<summary><b>Q20: thenApply vs. thenCompose vs. thenCombine — when do you use each?</b></summary>

`thenApply(fn)` transforms the result with a function that returns a **plain value** (`T -> U`) — it's `map`. `thenCompose(fn)` chains a function that itself returns **another `CompletableFuture`** (`T -> CF<U>`) — it's `flatMap`, and it flattens what would otherwise be a `CF<CF<U>>`. `thenCombine(other, biFn)` waits for **two independent** futures and merges their results (`(T,U) -> V`). Rule: transforming a value → `thenApply`; calling another async service that returns a future → `thenCompose`; joining two parallel calls → `thenCombine`.

</details>

<details>
<summary><b>Q21: What's the difference between thenApply and thenApplyAsync? Which thread runs the callback?</b></summary>

Without `Async`, the callback runs on **whichever thread completed the previous stage** — possibly a pool thread, possibly the thread that called `complete()`, possibly your own thread if the prior stage was already done. With `thenApplyAsync`, the callback is submitted to the **common ForkJoinPool** (or an `Executor` you pass). Use the `…Async` form with your **own executor** when the stage does blocking or long-running work, so you don't hijack the completing thread or starve the common pool. For cheap, non-blocking transforms the plain form is fine.

</details>

<details>
<summary><b>Q22: Why is running blocking work on CompletableFuture.supplyAsync() dangerous by default?</b></summary>

Because the default executor is the **common ForkJoinPool**, sized to about `cores - 1` and **shared across the entire JVM** (every `parallelStream()` and default `CompletableFuture`). If you run blocking I/O on it, those few threads sit blocked, starving all other parallel work — throughput collapses. The fix is to always pass a **dedicated `Executor`** for blocking stages: `supplyAsync(task, myIoExecutor)` and the `…Async(fn, myIoExecutor)` variants. (Or run the blocking calls on virtual threads.)

</details>

<details>
<summary><b>Q23: exceptionally vs. handle vs. whenComplete?</b></summary>

`exceptionally(fn)` runs **only on failure** and supplies a fallback value (`Throwable -> T`). `handle(biFn)` runs on **both** success and failure (`(T, Throwable) -> U`) and produces a new result — good for unified handling. `whenComplete(biConsumer)` is a **side-effect** callback (`(T, Throwable) -> void`) that observes the outcome (e.g., logging, metrics) without altering it — it passes the original result/exception downstream unchanged. Use `exceptionally` to recover, `handle` to transform either outcome, `whenComplete` to peek.

</details>

<details>
<summary><b>Q24 (coding/design): Call 3 services in parallel and combine results, with a timeout and fallback.</b></summary>

Kick off each call with `supplyAsync(task, executor)` (a dedicated executor, not the common pool). Combine independent results with `thenCombine` (for two) or `allOf(...).thenApply(v -> join each)` (for many). Bound the whole thing with `orTimeout(2, SECONDS)` (Java 9+) or `completeOnTimeout(fallback, ...)`, and attach `exceptionally`/`handle` for fallbacks. Sketch:

```java
var a = CompletableFuture.supplyAsync(this::callA, ex);
var b = CompletableFuture.supplyAsync(this::callB, ex);
var c = CompletableFuture.supplyAsync(this::callC, ex);
CompletableFuture.allOf(a, b, c)
    .orTimeout(2, TimeUnit.SECONDS)
    .thenApply(v -> merge(a.join(), b.join(), c.join()))
    .exceptionally(t -> degradedResult());
```

Talk about: running on a sized executor, not blocking inside stages, partial-failure strategy (fail-fast via `allOf` vs. best-effort by handling each future's exception individually), and that `join()` after `allOf` won't block since all are done.

</details>

### Virtual Threads (deep dive)

<details>
<summary><b>Q25: What is a virtual thread and how is it different from a platform thread?</b></summary>

A platform thread is a thin 1:1 wrapper over an OS thread — heavyweight (~1 MB stack, kernel-scheduled), so you can have only thousands. A virtual thread is a lightweight thread managed by the **JVM**: many virtual threads are multiplexed (M:N) onto a small pool of **carrier** platform threads. It's cheap (hundreds of bytes to KBs, grows as needed), so millions can exist. Crucially, when a virtual thread blocks on I/O, the JVM **unmounts** it and frees the carrier to run another virtual thread — so blocking no longer wastes an OS thread.

</details>

<details>
<summary><b>Q26: Walk me through what happens when a virtual thread does a blocking I/O call.</b></summary>

The JDK's I/O and concurrency libraries were rewritten to be Loom-aware. When the virtual thread calls a blocking operation, instead of blocking the OS thread, the runtime: (1) registers interest in the I/O completion, (2) **unmounts** the virtual thread — captures its stack as a continuation parked on the heap, (3) frees the carrier thread to run other virtual threads. When the I/O completes, the virtual thread becomes runnable and the scheduler **remounts** it on any free carrier, resuming execution right after the blocking call. Your code looks synchronous; the runtime makes it non-blocking underneath.

</details>

<details>
<summary><b>Q27: Do virtual threads make my program faster?</b></summary>

Not for a single task — a virtual thread runs your code at the same speed as a platform thread. What they improve is **scalability/throughput under high concurrency**: you can have orders of magnitude more concurrent tasks because blocked ones cost almost nothing. For CPU-bound work they give **no** speedup (you're still bounded by cores, and there are no blocking points to yield at). The win is "handle 50,000 concurrent connections with simple blocking code," not "compute this matrix faster."

</details>

<details>
<summary><b>Q28: Why shouldn't you pool virtual threads?</b></summary>

Thread pools exist to amortize the **high cost of creating platform threads**. Virtual threads are cheap to create, so a pool adds no benefit — and a fixed-size pool actively *caps* the concurrency that virtual threads are designed to unlock, reintroducing the very bottleneck you wanted to remove. The idiom is one virtual thread **per task** via `Executors.newVirtualThreadPerTaskExecutor()`, which creates a fresh virtual thread for each submitted task.

</details>

<details>
<summary><b>Q29: What is "pinning" and how do you avoid it?</b></summary>

Pinning is when a virtual thread **can't be unmounted** from its carrier, so a blocking call ties up the underlying OS thread — defeating the scalability benefit. The classic causes are blocking **inside a `synchronized` block/method** and blocking during a **native (JNI)** call. The mitigation is to replace `synchronized` with `ReentrantLock` around blocking I/O in hot paths. (In JDK 21 this was a real limitation; JDK 24's JEP 491 removed most `synchronized`-related pinning — but you should still understand the concept and recognize JNI as a remaining case.)

</details>

<details>
<summary><b>Q30: Virtual threads vs. reactive/async programming — when would you still choose reactive?</b></summary>

Virtual threads let you achieve reactive-level scalability for I/O-bound work while keeping **simple, debuggable, blocking, sequential** code — readable stack traces, normal try/catch, no "function coloring." For most request/response and fan-out services they're the simpler choice. Reactive (Project Reactor, RxJava) still has an edge when you need rich **stream composition and backpressure operators** (windowing, throttling, merging event streams) or you're already on a fully reactive stack. Rule of thumb: choose virtual threads for high-concurrency I/O with simple control flow; choose reactive for complex streaming/backpressure semantics.

</details>

<details>
<summary><b>Q31: How does the JVM schedule virtual threads, and why does that limit CPU-bound use?</b></summary>

Virtual threads are scheduled **cooperatively** by a JVM scheduler (a `ForkJoinPool` of carriers): a virtual thread yields its carrier at **blocking points** — I/O, `sleep`, lock waits. Platform threads, by contrast, are **preemptively** scheduled by the OS kernel, which can interrupt at almost any instruction. Because a CPU-bound virtual thread running a tight loop has **no blocking points to yield at**, it monopolizes its carrier exactly like a platform thread — so virtual threads give no advantage for pure computation. They shine only where there's lots of waiting (I/O) to overlap.

</details>

<details>
<summary><b>Q32: ThreadLocal with virtual threads — what's the concern, and the alternative?</b></summary>

Each virtual thread gets its own `ThreadLocal` values. With pooled platform threads, `ThreadLocal` is a useful per-thread cache reused across many tasks; but with **millions** of virtual threads (one per task), per-thread copies can consume huge memory and provide no reuse benefit. The Loom-era alternative is **`ScopedValue`** (immutable, bounded to a dynamic scope, shared efficiently across child tasks), which is cheaper and safer for passing context (like a request ID) down a call tree.

</details>

### Thread Lifecycle & Scheduling (deep dive)

<details>
<summary><b>Q33: Why is there no RUNNING state in Java's Thread.State?</b></summary>

Java collapses "ready to run" and "currently executing on a core" into a single **RUNNABLE** state. The reason is architectural: deciding which RUNNABLE thread actually occupies a CPU core at any instant is the **OS kernel scheduler's** job, not the JVM's. From the JVM's vantage point, a thread that's eligible to run and one that's mid-instruction on a core are indistinguishable — so exposing a separate RUNNING state would be misleading and unreliable. The six states are NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED.

</details>

<details>
<summary><b>Q34: A thread is stuck. How do you tell if it's BLOCKED vs WAITING, and what does each mean?</b></summary>

Take a thread dump (`jstack`) or call `getState()`. **BLOCKED** means the thread is trying to acquire an **intrinsic monitor lock** (entering a `synchronized` region) that another thread holds — it's sitting in the monitor's Entry Set. **WAITING** means it's parked until **signaled** — via `Object.wait()`, `Thread.join()`, or `LockSupport.park()` (which is what `ReentrantLock` uses). Key distinction: only `synchronized` contention produces BLOCKED; a thread waiting on a `ReentrantLock` shows as WAITING, not BLOCKED. The dump also names the lock/monitor, which is how you find deadlocks and contention.

</details>

<details>
<summary><b>Q35: Does a sleeping thread release the locks it holds? Why does that matter?</b></summary>

No. `Thread.sleep(t)` puts the thread in TIMED_WAITING but it **retains every monitor lock it already holds**. This matters because a thread that sleeps inside a `synchronized` block keeps other threads BLOCKED on that monitor for the whole sleep — a common cause of mysterious stalls. Contrast with `Object.wait(t)`, which *does* release the monitor while waiting. So "sleep holds locks, wait releases them" is the one-liner.

</details>

<details>
<summary><b>Q36: Explain preemptive scheduling and why thread priority is unreliable in Java.</b></summary>

Java threads are scheduled **preemptively** by the OS: the kernel allocates CPU time slices and can interrupt a thread to run another, based on priority and fairness. `Thread.setPriority(1..10)` is only a **hint** mapped onto the OS's own priority scheme, and the mapping varies by platform — some OSes largely ignore it. So you must never write code whose **correctness** depends on priority or execution order; at best it's a soft tuning knob. Relying on it for synchronization is a classic anti-pattern.

</details>

<details>
<summary><b>Q37: Walk through the state transitions for a thread that does some work, waits for a signal, then finishes.</b></summary>

NEW (after `new Thread()`) → RUNNABLE (after `start()`; the JVM creates the native thread and the OS schedules it) → it runs, then calls `obj.wait()` inside a `synchronized` block → WAITING (it releases the monitor and parks in the Wait Set) → another thread calls `notify()`/`notifyAll()` → it moves to the Entry Set and, once it reacquires the lock, returns to RUNNABLE → it finishes `run()` → TERMINATED. If instead it had been contending for a held lock, it would have passed through BLOCKED; if it had called `sleep(t)` or `wait(t)`, through TIMED_WAITING. A TERMINATED thread can't be restarted (`start()` throws `IllegalThreadStateException`).

</details>

### Intrinsic Locks & Monitors (deep dive)

<details>
<summary><b>Q38: Describe the internal structure of a Java monitor.</b></summary>

Every object can act as a monitor backing its intrinsic lock. A monitor conceptually has: an **owner** (the thread currently holding the lock), a **recursion/entry count** (for reentrancy), an **Entry Set** (threads BLOCKED waiting to *acquire* the lock), and a **Wait Set** (threads that called `wait()` and are parked until notified). `monitorenter` makes an unowned monitor owned (count 1) or increments the count for the same thread or blocks others into the Entry Set; `monitorexit` decrements and releases at zero. `wait()` moves the owner into the Wait Set (releasing the lock); `notify()` moves one Wait-Set thread to the Entry Set to re-compete.

</details>

<details>
<summary><b>Q39: What does it mean that intrinsic locks are reentrant, and why is reentrancy necessary?</b></summary>

Reentrant means a thread that already holds a monitor can acquire it again without blocking — the JVM just increments the recursion count, decrementing on each exit and releasing at zero. It's necessary because synchronized methods frequently call **other** synchronized methods on the same object (directly or via inheritance). Without reentrancy, such a self-call would block on a lock the thread itself holds — an instant self-deadlock. Reentrancy makes composition of synchronized code safe.

</details>

<details>
<summary><b>Q40: How does the JVM optimize synchronized? Explain lock escalation.</b></summary>

HotSpot doesn't always use a heavyweight OS mutex. It escalates through tiers recorded in the object header's mark word: **biased locking** (historical/now removed — near-free re-entry when only one thread ever locks the object), **thin/lightweight locking** (a fast CAS on the mark word under low contention, no kernel involvement), and **heavyweight/inflated locking** (a real OS monitor with kernel wait queues) once genuine contention appears. The takeaway: **uncontended `synchronized` is cheap** (a CAS); cost shows up under contention when the lock inflates and threads park/wake via the OS — which is exactly why you keep critical sections short.

</details>

<details>
<summary><b>Q41: Why is synchronizing on a String literal or boxed Integer dangerous? What should you lock on?</b></summary>

String literals are **interned** (a single shared instance JVM-wide) and small `Integer`s are **cached** by autoboxing, so `synchronized("lock")` or `synchronized(Integer.valueOf(1))` may acquire an object that *completely unrelated* code elsewhere also locks on — causing surprise contention or deadlock that's nearly impossible to trace. Likewise, locking on `this` exposes your lock to any caller holding your object. The fix: lock on a dedicated `private final Object lock = new Object();` that nothing else can reference. It must be `final` so the reference can't change underneath you.

</details>

<details>
<summary><b>Q42: If a static field is shared across threads, which lock protects it — instance or class? What's the bug if you get it wrong?</b></summary>

A `static` field is shared by **all instances**, so it must be guarded by the **class lock** (`static synchronized` or `synchronized(Foo.class)`). If you guard it with an *instance* lock (`synchronized(this)`), two threads operating on two different instances acquire two different locks and both touch the same static field concurrently — a race condition despite the code "looking synchronized." The mirror bug is guarding instance state with the class lock, which needlessly serializes all instances. Match the lock's scope to the data's scope.

</details>

### Executor Framework (deep dive)

<details>
<summary><b>Q43: Walk through exactly how ThreadPoolExecutor decides what to do with a submitted task.</b></summary>

In order: **(1)** if running threads < `corePoolSize`, start a new core thread for the task (even if other threads are idle); **(2)** otherwise try to add the task to the **work queue**; **(3)** if the queue is full and threads < `maximumPoolSize`, start a new (non-core) thread; **(4)** if the queue is full and threads are at max, invoke the **rejection policy**. A critical corollary: with an **unbounded** queue, step 2 always succeeds, so the pool never exceeds core size and never rejects — it just queues until OOM. That's why `newFixedThreadPool` is risky under overload.

</details>

<details>
<summary><b>Q44: Why are newFixedThreadPool and newCachedThreadPool considered dangerous in production?</b></summary>

`newFixedThreadPool(n)` uses an **unbounded `LinkedBlockingQueue`**: if tasks arrive faster than they're processed, the queue grows without limit → `OutOfMemoryError`, with no back-pressure signal. `newCachedThreadPool()` uses a `SynchronousQueue` with effectively unbounded `maximumPoolSize`: under a burst it creates a new thread per task, potentially **thousands of threads**, exhausting memory/CPU. Both hide the failure mode. The remedy is to construct `ThreadPoolExecutor` directly with a **bounded queue** and an explicit **rejection policy**, so overload produces controlled back-pressure instead of a crash.

</details>

<details>
<summary><b>Q45: Explain the work queue choices and how each changes pool behavior.</b></summary>

`LinkedBlockingQueue` (unbounded by default) → the pool stays at core size and never rejects, risking OOM. `ArrayBlockingQueue` (bounded) → enables growth to `maximumPoolSize` once the queue fills, then rejection/back-pressure — the production-friendly choice. `SynchronousQueue` (zero capacity — a direct hand-off) → every task demands an immediately available thread, forcing the pool to create threads on demand (this is how `newCachedThreadPool` works) or reject. `PriorityBlockingQueue` → tasks are dequeued by priority rather than FIFO. The queue is the single biggest lever on pool behavior.

</details>

<details>
<summary><b>Q46: Compare the four rejection policies. When would you use CallerRunsPolicy?</b></summary>

`AbortPolicy` (default) throws `RejectedExecutionException` — fail fast and let the caller decide. `DiscardPolicy` silently drops the new task — only acceptable when losing work is fine. `DiscardOldestPolicy` evicts the oldest queued task and retries — useful when newer data supersedes older. `CallerRunsPolicy` runs the task on the **submitting thread**, which is elegant **back-pressure**: while the producer is busy executing the task itself, it can't submit more, naturally slowing the input rate to what the pool can handle. Use `CallerRunsPolicy` when you want to throttle producers and never drop work.

</details>

<details>
<summary><b>Q47: What's the difference between shutdown() and shutdownNow(), and how do you shut a pool down cleanly?</b></summary>

`shutdown()` is graceful: it stops accepting new tasks but lets already-submitted ones finish. `shutdownNow()` attempts to stop everything: it tries to **interrupt** running tasks and returns the list of tasks that never started. Tasks must respond to interruption for `shutdownNow()` to be effective. The clean idiom is: call `shutdown()`, then `awaitTermination(timeout)`; if it returns `false` (didn't finish in time), call `shutdownNow()`, await again briefly, and restore the interrupt flag if interrupted. Always shut pools down — non-daemon worker threads otherwise keep the JVM alive.

</details>

### Race conditions, thread-safe classes & lifecycle (deep dive)

<details>
<summary><b>Q48: What are the two main kinds of race condition? Give an example of each.</b></summary>

**Lost update (read-modify-write):** two threads read a value, both compute from the same starting point, and one write overwrites the other — the canonical `counter++` losing increments. **Check-then-act (TOCTOU):** a thread checks a condition and acts on it, but another thread invalidates the condition in the gap — e.g., two withdrawals both pass `if (balance >= amount)` against the same balance and overdraw the account, or `if (!map.containsKey(k)) map.put(k, v)` double-inserts. Both are fixed by making the whole read-modify-write or check-then-act sequence atomic (a lock, or an atomic operation like `incrementAndGet`/`putIfAbsent`).

</details>

<details>
<summary><b>Q49: StringBuffer vs StringBuilder — what's the difference and which should you use?</b></summary>

Both build mutable strings with the same API. `StringBuffer` is thread-safe — its methods are `synchronized` — whereas `StringBuilder` is not, which makes it faster. Because string building is almost always confined to a single thread, `StringBuilder` is the right default; use `StringBuffer` only if a single builder instance is genuinely shared across threads (and even then, confining the mutable state to one thread and publishing only the finished immutable `String` is usually cleaner).

</details>

<details>
<summary><b>Q50: A List is wrapped with Collections.synchronizedList. Is `if (!list.isEmpty()) list.remove(0)` thread-safe?</b></summary>

No. `synchronizedList` makes each *individual* call (`isEmpty()`, `remove(0)`) atomic, but not the *sequence*: another thread can remove the last element between your `isEmpty()` check and your `remove(0)`, so `remove(0)` acts on a stale decision (throwing, or removing the wrong element). You must lock across the whole compound operation with `synchronized (list) { if (!list.isEmpty()) list.remove(0); }` — locking on the wrapper, which is the same monitor the wrapper uses internally. The same rule applies to iterating a synchronized collection.

</details>

<details>
<summary><b>Q51: Why prefer notifyAll() over notify()?</b></summary>

`notify()` wakes one arbitrary waiter and can't choose which. When multiple threads wait on the same monitor for *different* conditions (e.g., producers and consumers on one lock), `notify()` may wake a thread that can't make progress; it re-checks its `while` condition and goes back to waiting, and the thread that *could* have proceeded is never signalled — a lost-signal stall. `notifyAll()` wakes everyone, so the one that can proceed does and the rest simply re-wait. Prefer `notifyAll()`; use `notify()` only when all waiters are provably interchangeable.

</details>

<details>
<summary><b>Q52: What's the classic five-state thread model, and how does it relate to Java's six Thread.State values?</b></summary>

Older/certification material describes five conceptual states: New, Runnable, Running, Waiting/Blocked/Sleeping, and Dead. Java's `Thread.State` exposes six: NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED. The mapping: New→NEW, Dead→TERMINATED, and the classic "Waiting/Blocked/Sleeping" bucket splits into BLOCKED (waiting for a monitor lock), WAITING (parked until signalled), and TIMED_WAITING (parked with a timeout). The key difference is that the classic model's separate **Running** state is folded into **RUNNABLE** in the JVM, because deciding which ready thread is actually on a core is the OS scheduler's job, not the JVM's.

</details>

<details>
<summary><b>Q53: Does calling run() directly start a thread? Can you start a thread twice? What if run is overloaded?</b></summary>

Calling `run()` directly just executes it on the *current* thread — no new thread starts; only `start()` creates one. You can call `start()` on a `Thread` object only once; a second call throws `IllegalThreadStateException` (threads can't be restarted). And if you declare `run(String)` or any other signature, you've *overloaded* rather than *overridden* `run()` — `start()` only ever invokes the no-arg `run()`, so the overload silently never executes. Put `@Override` on `public void run()` to let the compiler catch that mistake.

</details>

**Live-coding & design problems.** The build-it-live classics — thread-safe LRU cache, Dining Philosophers, a custom thread pool, and a concurrent web crawler, alongside producer-consumer, the thread-safe counter, the rate limiter, the singleton and the deadlock demo — are collected together in **Part 6 (§23–§36)** so you can drill them in one place.

---

## 41. FAQs

<details>
<summary><b>Is HashMap safe for concurrent reads if nobody writes?</b></summary>

Yes — a `HashMap` that's fully built and then only read (after safe publication) is fine for concurrent reads. The danger is reading while another thread writes, which can corrupt internal structure (in Java 7 it could even infinite-loop on resize). If any thread writes, use `ConcurrentHashMap`.

</details>

<details>
<summary><b>sleep() vs. wait()?</b></summary>

`sleep()` is a static `Thread` method that pauses the current thread and does NOT release locks. `wait()` is an `Object` method, must be called holding that object's monitor, RELEASES the lock, and resumes on `notify`/`notifyAll` or timeout. `sleep` is about timing; `wait` is about coordination.

</details>

<details>
<summary><b>What does Thread.interrupt() actually do?</b></summary>

It sets the thread's interrupt flag. Blocking methods (`sleep`, `wait`, `join`, `BlockingQueue.take`) respond by throwing `InterruptedException` and clearing the flag. In non-blocking loops you check `Thread.currentThread().isInterrupted()` yourself. Interruption is cooperative — a request to stop, not a forced kill. Never swallow `InterruptedException`; propagate it or restore the flag.

</details>

<details>
<summary><b>What is a daemon thread?</b></summary>

A background thread (`setDaemon(true)` before `start()`) that doesn't keep the JVM alive — once only daemon threads remain, the JVM exits. Good for housekeeping (monitoring, GC helpers). Don't use daemons for work that must complete, since they can be killed at shutdown.

</details>

<details>
<summary><b>Why is ConcurrentHashMap better than Collections.synchronizedMap?</b></summary>

`synchronizedMap` wraps every method in one global lock, serializing all access. `ConcurrentHashMap` lets reads proceed largely lock-free and locks only the individual bucket being written (Java 8+), so many threads can read and write different buckets concurrently — far higher throughput. Note its `size()` is approximate under concurrent updates, and it disallows null keys/values.

</details>

<details>
<summary><b>What is the happens-before relationship?</b></summary>

It's the Java Memory Model's rule for when one thread's writes are guaranteed visible to another's reads. Key edges: program order within a thread; an unlock happens-before a later lock of the same monitor; a volatile write happens-before a later read of it; `Thread.start()` happens-before the thread's actions; a thread's actions happen-before another's return from `join()`; and it's transitive. No happens-before relationship + a write = a data race with undefined results.

</details>

---

## 42. Quick Revision Cheat Sheet (whole-guide recall in one pass)

This section is built for the night before an interview. Read it top to bottom and you should be able to *recall every major idea in this guide* — each paragraph is a compressed pointer back to a full section. If a line doesn't trigger the full concept, jump back to the section it names.

### The one mental model everything hangs off

**Shared, mutable state that is written by one thread and read/written by another, without coordination, is a bug.** Every tool in this guide exists to remove one of those words: make state *unshared* (thread confinement, `ThreadLocal`, stack variables), make it *immutable* (final fields, value objects), or add *coordination* (locks, volatile, atomics, synchronizers). Concurrency correctness is three separate guarantees — **atomicity** (a read-modify-write happens as one indivisible step), **visibility** (one thread's write becomes observable to another), and **ordering** (operations aren't reordered in a way that breaks your logic). Almost every bug and every tool maps to one of those three. (Parts 1–2.)

### Mental-model hierarchy (how the pieces relate)

```
Java Concurrency
|
+-- 1. Foundations (Part 1)
|     +-- process vs thread; concurrency (design) vs parallelism (hardware)
|     +-- creation: Thread / Runnable / Callable+Future / lambdas
|     +-- lifecycle: NEW - RUNNABLE - BLOCKED - WAITING - TIMED_WAITING - TERMINATED
|     +-- scheduler + priority (a hint only); sleep / yield / join / interrupt / daemon
|
+-- 2. The three hazards (Part 2)
|     +-- atomicity  -> lost update (count++), check-then-act (TOCTOU)
|     +-- visibility -> stale caches, needs volatile / lock / atomic
|     +-- ordering   -> reordering, fixed by happens-before edges (JMM)
|
+-- 3. Locking & coordination (Parts 3-4)
|     +-- synchronized (intrinsic monitor): object lock vs class lock; reentrant
|     +-- wait / notify / notifyAll (guarded blocks, while-loop, notifyAll)
|     +-- explicit locks: ReentrantLock, ReadWriteLock, StampedLock
|     +-- synchronizers: CountDownLatch, CyclicBarrier, Semaphore, Phaser, Exchanger
|
+-- 4. Lock-free & memory (Parts 2-4)
|     +-- volatile (visibility + ordering, NOT atomicity)
|     +-- atomics + CAS (AtomicInteger/Long/Reference), ABA -> AtomicStampedReference
|     +-- LongAdder (striped hot counters), false sharing / @Contended
|
+-- 5. High-level frameworks (Parts 5-6)
|     +-- Executors / ThreadPoolExecutor (bounded queue + rejection policy)
|     +-- Future / CompletableFuture (async pipelines)
|     +-- Fork/Join + parallel streams (divide-and-conquer)
|     +-- concurrent collections: ConcurrentHashMap, BlockingQueue family, CopyOnWrite
|
+-- 6. Virtual threads (Part 5)
      +-- JVM-managed, ~cheap, millions; unmount on blocking I/O
      +-- for I/O-bound not CPU-bound; don't pool; watch pinning under synchronized
```

### Part 1 — Foundations recall

A **process** owns its memory; **threads** live inside one process and share its heap but each has its own stack and program counter — cheap to create, and able to share data directly (the source of both their power and their bugs). **Concurrency** is a structuring idea (overlapping task lifetimes, possible on one core); **parallelism** is simultaneous execution on multiple cores. Create work as a **`Runnable`** (fire-and-forget) or **`Callable<T>`** (returns a value / throws checked exceptions → submit to an executor for a `Future`); extend `Thread` only for throwaway code. **`start()` creates a new thread that runs `run()`; calling `run()` directly is just an ordinary method call on the current thread** — and `start()` can be called only once (else `IllegalThreadStateException`). Lifecycle states: **NEW, RUNNABLE** (ready *or* running — the OS decides), **BLOCKED** (waiting for a monitor lock), **WAITING / TIMED_WAITING** (parked until signalled, optionally with a timeout), **TERMINATED**. `sleep()` is static, keeps its locks, and guarantees only a *minimum* delay; `yield()` is a hint; `join()` waits for another thread to finish; **`interrupt()` is a cooperative request to stop** (sets a flag; blocking calls throw `InterruptedException` — never swallow it, propagate or restore the flag). A **daemon** thread doesn't keep the JVM alive. Priority (1–10) is only a hint to the scheduler.

### Part 2 — The three hazards and the memory model

A **race condition** is when the result depends on thread interleaving. Two flavors to name out loud: **lost update** (`count++` is really read-modify-write, so two threads can both write the same value) and **check-then-act / TOCTOU** (`if (balance >= amt) withdraw(amt)` — the balance changes in the gap). Fix by making the *whole* sequence atomic. The **Java Memory Model** defines **happens-before**: the rules for when one thread's writes are guaranteed visible to another. Key edges: program order within a thread; **unlock happens-before a later lock** of the same monitor; **a volatile write happens-before a later read** of it; `Thread.start()` happens-before the thread's work; a thread's work happens-before another thread's return from `join()`; and it is transitive. No happens-before edge across a shared write = a **data race** with undefined results. **Safe publication** means fully constructing an object *then* handing off its reference through a happens-before edge (volatile field, final field, or lock).

### Parts 3–4 — Locking and coordination recall

**`synchronized`** takes an object's **intrinsic monitor**: mutual exclusion *plus* visibility (unlock→lock), and it is **reentrant**. The **object lock** (instance methods / `synchronized(this)`) and the **class lock** (`static synchronized` / `synchronized(Foo.class)`) are **independent** — static shared data must be guarded by the class lock. Prefer small `synchronized` *blocks* on a private final lock object over synchronizing whole methods. **Guarded blocks** use `wait`/`notify`/`notifyAll`: you must hold the monitor, `wait()` releases the lock and parks, you **always re-check the condition in a `while`** (spurious wakeups + stolen signals), and you **prefer `notifyAll()`** unless every waiter is interchangeable — Producer-Consumer (§25) is the canonical example. **Liveness failures:** *deadlock* (circular wait for locks — prevent with consistent global lock ordering or `tryLock` + timeout; detect with a `jstack` thread dump), *livelock* (threads keep reacting but never progress — add randomized backoff), *starvation* (one thread never gets a turn — use fair locks). **Explicit locks** add power over `synchronized`: **`ReentrantLock`** (`tryLock`, timed/interruptible acquisition, fairness, multiple `Condition`s — always `unlock()` in `finally`); **`ReadWriteLock`** (many concurrent readers *or* one writer — read-heavy data); **`StampedLock`** (optimistic reads, not reentrant). **Synchronizers:** **`CountDownLatch`** (wait for N events, one-shot), **`CyclicBarrier`** (N threads wait for each other, reusable), **`Semaphore`** (N permits → cap concurrent access), plus `Phaser` and `Exchanger` for phased/hand-off coordination.

### Parts 2–4 — Lock-free and memory-level tools

**`volatile`** guarantees **visibility and ordering but NOT atomicity** — perfect for a stop/status flag and for safe publication of a reference; useless for `count++`. **Atomics** (`AtomicInteger/Long/Reference`) give lock-free updates via **CAS** (compare-and-set: retry until the value you read is still there). CAS suffers the **ABA problem** (value changes A→B→A and CAS can't tell) → use `AtomicStampedReference`. Under heavy write contention a single atomic hot-spots on one cache line, so use **`LongAdder`** (striped cells, summed on read) for write-heavy counters. **False sharing** (unrelated fields on the same 64-byte cache line bouncing between cores) is mitigated with padding / `@Contended`. Under the hood, `ReentrantLock`, `Semaphore`, and `CountDownLatch` are all built on **AQS** (AbstractQueuedSynchronizer), a FIFO wait-queue over a single atomic `state`.

### Parts 5–6 — High-level frameworks recall

**Executors** decouple *submitting* work from *running* it. Know the factory pools (Fixed / Cached / Scheduled / SingleThread) but in production **build a `ThreadPoolExecutor` explicitly** with a **bounded queue** (back-pressure) and a **rejection policy** (e.g. `CallerRunsPolicy`); avoid `newFixedThreadPool` (unbounded queue → OOM) and `newCachedThreadPool` (unbounded threads). **Sizing:** CPU-bound ≈ number of cores; I/O-bound larger, scaled by wait/compute ratio. Always **`shutdown()` + `awaitTermination()`**. **`Future.get()`** blocks; **`CompletableFuture`** composes async pipelines (`thenApply`/`thenCompose`/`thenCombine`/`allOf`) without blocking. **Fork/Join** and **parallel streams** automate divide-and-conquer over CPU-bound, side-effect-free work (but never block inside a parallel stream, and mind the shared common pool). **Concurrent collections:** **`ConcurrentHashMap`** (lock-free reads, per-bucket write locks, atomic `compute`/`merge`; no nulls; approximate `size()`) beats `Collections.synchronizedMap`; the **`BlockingQueue`** family (`ArrayBlockingQueue`, `LinkedBlockingQueue`, `SynchronousQueue`, `PriorityBlockingQueue`) powers producer-consumer and thread pools; **`CopyOnWriteArrayList`** suits read-mostly listener lists. Remember `Vector`/`Hashtable`/`synchronizedXxx` make only *single* calls atomic — **compound operations still need client-side locking** on the collection.

### Part 5 — Virtual threads recall

**Virtual threads (Java 21+)** are JVM-managed and cheap (you can have millions); when one blocks on I/O the JVM **unmounts** it from its carrier OS thread so the carrier runs another. This lets you write simple **blocking** code for **high-concurrency, I/O-bound** work (request handlers, fan-out calls) instead of reactive pipelines. They give **no benefit for CPU-bound** work (still bounded by cores — use a sized platform-thread pool). Rules: **don't pool them** (use `newVirtualThreadPerTaskExecutor()`, one per task), avoid heavy `ThreadLocal` at scale (prefer `ScopedValue`), and beware **pinning** under `synchronized`/JNI.

<details>
<summary><b>Two-minute drill: rapid-fire recall (classic exam points)</b></summary>

**Creating & starting**

- Create threads by extending `Thread` and overriding `public void run()`, or by passing a `Runnable` target to the `Thread` constructor.
- `start()` may be called **once** per `Thread`; a second call throws `IllegalThreadStateException`. Many `Thread`s may share one `Runnable`.
- A `Thread` isn't "alive" until `start()`; before that it's in the new state.

**Transitions**

- A started thread always enters the runnable state; the scheduler moves it between runnable and running.
- On one core only one thread runs at a time; there's **no guarantee** of order or fairness — that's the scheduler's call.
- A running thread can leave the CPU via `wait()`, `sleep()`, or `join()`, or by blocking on a lock it can't acquire.
- When the sleep/wait ends or the lock is free, the thread returns to runnable (not directly to running). A dead thread can't be restarted.

**sleep / yield / join / priority**

- `sleep()` is static, delays the current thread, releases **no** locks, and guarantees only a *minimum* sleep time.
- `setPriority(1..10)` is only a hint; an unset priority is inherited from the creating thread.
- `yield()` may let another same-priority runnable thread run — with no guarantee it does, or that a different thread is chosen.
- `join()` makes the current thread wait for the target thread to complete.

**Synchronization**

- `synchronized` (method or block) lets only one thread into an object's critical section at a time; other threads can still run its *unsynchronized* code.
- A synchronized block needs the object whose lock you want; a `static synchronized` method uses the `Class` object's lock.
- A sleeping thread keeps its locks; a waiting thread (`wait()`) releases the monitor.

**wait / notify / notifyAll & deadlock**

- `wait()`, `notify()`, and `notifyAll()` must be called while holding the object's monitor, on the shared object, and `wait()` belongs in a `while` loop.
- `notify()` wakes one arbitrary waiter (you can't pick which); `notifyAll()` wakes all — prefer it.
- Deadlock: threads block forever, each holding a lock the other needs. Prevent it with a consistent global lock ordering.

**Memory & lock-free**

- `volatile` = visibility + ordering, never atomicity; good for flags and safe publication.
- Atomics use CAS (lock-free, may retry); `LongAdder` for hot counters; ABA → `AtomicStampedReference`.
- happens-before: unlock→lock, volatile write→read, start()→thread, thread→join(); transitive.

**Frameworks**

- Prefer executors to raw threads; in prod use a bounded `ThreadPoolExecutor` + rejection policy; always `shutdown()`.
- `ConcurrentHashMap` over `synchronizedMap`; `BlockingQueue` for producer-consumer; compound ops on synchronized collections still need external locking.
- Virtual threads for I/O-bound, not CPU-bound; don't pool; watch pinning under `synchronized`.

</details>

**If you remember one thing:** *Shared mutable state + a write + no coordination = bug. Remove the sharing, remove the mutation, or add coordination — and always reach for the highest-level tool that fits while keeping critical sections small.*

---

*This guide covers every topic from the source material (fundamentals, JVM scheduling and thread priorities, thread creation and the classic five-state lifecycle, race conditions — both lost-update and check-then-act, synchronized/intrinsic locks and monitor internals, object vs class locks, the Java Memory Model and happens-before, thread-safe classes and the compound-operation trap, wait/notify/notifyAll, producer-consumer, ReentrantLock, ReadWriteLock, CyclicBarrier, Semaphore, CountDownLatch, volatile and atomics, thread pools/executors, CompletableFuture, and virtual threads) and adds the deadlock-handling and FAANG interview material that rounds it out for interview prep.*

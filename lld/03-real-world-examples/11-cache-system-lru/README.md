# LRU Cache System - LLD Interview Problem

## 📋 Problem Statement

Design an LRU (Least Recently Used) Cache that:
- Stores key-value pairs
- Has fixed capacity
- Evicts least recently used items when full
- Provides O(1) get and put operations

## 🎯 Key Requirements

### Functional Requirements:
1. `get(key)` - Get value for key (O(1))
2. `put(key, value)` - Put key-value pair (O(1))
3. Evict LRU item when capacity reached
4. Update access order on get/put
5. Thread-safe operations (optional)

### Non-Functional Requirements:
1. O(1) time complexity for get/put
2. Fixed memory capacity
3. Thread-safety
4. Generic type support

## 🏗️ Design Components

### Core Classes:
1. **LRUCache** - Main cache class
2. **Node** - Doubly linked list node
3. **HashMap** - For O(1) access
4. **DoublyLinkedList** - For LRU ordering

## 🎨 Design Patterns Used

1. **Decorator Pattern** - Thread-safe wrapper
2. **Strategy Pattern** - Eviction strategies

## 💡 SOLID Principles Applied

- **SRP**: Separate cache logic from threading
- **OCP**: Extensible eviction policies
- **ISP**: Specific cache interface
- **DIP**: Depend on abstractions

## 🔍 Interview Discussion Points

1. Why doubly linked list + HashMap?
2. How to achieve O(1) operations?
3. Thread-safety implementation
4. Other eviction policies (LFU, FIFO)
5. Distributed cache considerations
6. Memory management

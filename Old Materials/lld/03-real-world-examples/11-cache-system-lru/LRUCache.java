import java.util.*;

/**
 * LRU Cache Implementation
 * 
 * Interview Key Points:
 * - HashMap for O(1) access
 * - Doubly linked list for O(1) reordering
 * - Head = Most recently used
 * - Tail = Least recently used
 * - Dummy head/tail nodes simplify edge cases
 * 
 * Time Complexity:
 * - get(): O(1)
 * - put(): O(1)
 * 
 * Space Complexity: O(capacity)
 * 
 * Design Decisions:
 * 1. Why HashMap? Fast key lookup
 * 2. Why Doubly Linked List? Need to remove from middle and add to front
 * 3. Why dummy nodes? Avoid null checks
 */
public class LRUCache<K, V> {
    private final int capacity;
    private final Map<K, Node<K, V>> cache;
    private final Node<K, V> head;  // Most recently used (dummy)
    private final Node<K, V> tail;  // Least recently used (dummy)
    private int size;
    
    /**
     * Constructor
     * @param capacity Maximum cache size
     */
    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.size = 0;
        
        // Initialize dummy head and tail
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        head.next = tail;
        tail.prev = head;
    }
    
    /**
     * Get value for key
     * Updates access order (moves to front)
     * 
     * @param key The key to lookup
     * @return Value if exists, null otherwise
     */
    public V get(K key) {
        Node<K, V> node = cache.get(key);
        
        if (node == null) {
            System.out.println("❌ Cache MISS: " + key);
            return null;
        }
        
        System.out.println("✓ Cache HIT: " + key + " = " + node.value);
        
        // Move to front (most recently used)
        moveToFront(node);
        
        return node.value;
    }
    
    /**
     * Put key-value pair
     * Evicts LRU item if capacity reached
     * 
     * @param key The key
     * @param value The value
     */
    public void put(K key, V value) {
        Node<K, V> node = cache.get(key);
        
        if (node != null) {
            // Update existing node
            System.out.println("📝 Updating: " + key + " = " + value);
            node.value = value;
            moveToFront(node);
        } else {
            // Add new node
            System.out.println("➕ Adding: " + key + " = " + value);
            
            // Check capacity
            if (size >= capacity) {
                evictLRU();
            }
            
            // Create new node and add to front
            Node<K, V> newNode = new Node<>(key, value);
            cache.put(key, newNode);
            addToFront(newNode);
            size++;
        }
        
        displayCache();
    }
    
    /**
     * Remove key from cache
     * 
     * @param key The key to remove
     * @return true if removed, false if not found
     */
    public boolean remove(K key) {
        Node<K, V> node = cache.get(key);
        
        if (node == null) {
            return false;
        }
        
        removeNode(node);
        cache.remove(key);
        size--;
        
        System.out.println("🗑️  Removed: " + key);
        displayCache();
        
        return true;
    }
    
    /**
     * Get current cache size
     */
    public int size() {
        return size;
    }
    
    /**
     * Check if cache is empty
     */
    public boolean isEmpty() {
        return size == 0;
    }
    
    /**
     * Check if key exists
     */
    public boolean containsKey(K key) {
        return cache.containsKey(key);
    }
    
    /**
     * Clear all entries
     */
    public void clear() {
        cache.clear();
        head.next = tail;
        tail.prev = head;
        size = 0;
        System.out.println("🧹 Cache cleared");
    }
    
    // ========== Private Helper Methods ==========
    
    /**
     * Move node to front (most recently used)
     */
    private void moveToFront(Node<K, V> node) {
        removeNode(node);
        addToFront(node);
    }
    
    /**
     * Add node to front (after head)
     */
    private void addToFront(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }
    
    /**
     * Remove node from its current position
     */
    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    
    /**
     * Evict least recently used item (before tail)
     */
    private void evictLRU() {
        Node<K, V> lru = tail.prev;
        
        if (lru == head) {
            return; // Empty cache
        }
        
        System.out.println("⚠️  Evicting LRU: " + lru.key + " = " + lru.value);
        
        removeNode(lru);
        cache.remove(lru.key);
        size--;
    }
    
    /**
     * Display current cache state
     * Shows order from MRU to LRU
     */
    public void displayCache() {
        System.out.print("📦 Cache [" + size + "/" + capacity + "]: ");
        
        if (size == 0) {
            System.out.println("(empty)");
            return;
        }
        
        List<String> items = new ArrayList<>();
        Node<K, V> current = head.next;
        
        while (current != tail) {
            items.add(current.key + "=" + current.value);
            current = current.next;
        }
        
        System.out.println(String.join(" → ", items));
        System.out.println("   (MRU) " + " ".repeat(Math.max(0, items.get(0).length() - 5)) + 
            "→ (LRU)\n");
    }
    
    /**
     * Get cache statistics
     */
    public String getStats() {
        return String.format("LRU Cache Stats: Size=%d, Capacity=%d, Usage=%.1f%%",
            size, capacity, (size * 100.0) / capacity);
    }
}

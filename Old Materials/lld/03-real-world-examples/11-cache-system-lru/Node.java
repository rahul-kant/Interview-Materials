/**
 * Node Class - Doubly Linked List Node for LRU Cache
 * 
 * Interview Key Points:
 * - Why doubly linked list? Need O(1) removal from middle
 * - Stores key for reverse lookup during eviction
 * - Generic type support
 * - Prev/next pointers for efficient reordering
 */
public class Node<K, V> {
    K key;
    V value;
    Node<K, V> prev;
    Node<K, V> next;
    
    public Node(K key, V value) {
        this.key = key;
        this.value = value;
        this.prev = null;
        this.next = null;
    }
    
    @Override
    public String toString() {
        return key + "=" + value;
    }
}

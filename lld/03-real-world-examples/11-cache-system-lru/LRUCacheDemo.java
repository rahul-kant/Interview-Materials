/**
 * LRU Cache Demo - Demonstrates LRU Cache functionality
 * 
 * Interview Key Points:
 * - Shows all cache operations
 * - Demonstrates LRU eviction
 * - Tests edge cases
 * - Visualizes cache state
 */
public class LRUCacheDemo {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     LRU CACHE SYSTEM DEMO                ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
        
        // Create cache with capacity 3
        LRUCache<String, Integer> cache = new LRUCache<>(3);
        
        System.out.println("═══════════════════════════════════════════");
        System.out.println("TEST 1: Basic Operations");
        System.out.println("═══════════════════════════════════════════\n");
        testBasicOperations(cache);
        
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("TEST 2: LRU Eviction");
        System.out.println("═══════════════════════════════════════════\n");
        cache.clear();
        testLRUEviction(cache);
        
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("TEST 3: Access Pattern");
        System.out.println("═══════════════════════════════════════════\n");
        cache.clear();
        testAccessPattern(cache);
        
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("TEST 4: Update Existing Key");
        System.out.println("═══════════════════════════════════════════\n");
        cache.clear();
        testUpdate(cache);
        
        // Real-world example
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("REAL-WORLD EXAMPLE: Page Cache");
        System.out.println("═══════════════════════════════════════════\n");
        demonstratePageCache();
    }
    
    /**
     * Test 1: Basic put and get operations
     */
    private static void testBasicOperations(LRUCache<String, Integer> cache) {
        System.out.println("Adding 3 items to cache (capacity = 3)...\n");
        
        cache.put("A", 1);
        cache.put("B", 2);
        cache.put("C", 3);
        
        System.out.println("\nRetrieving values...\n");
        
        Integer val = cache.get("A");
        System.out.println("Retrieved: " + val);
        
        val = cache.get("D");
        System.out.println("Retrieved: " + val + " (null - not found)");
        
        System.out.println("\n" + cache.getStats());
    }
    
    /**
     * Test 2: LRU eviction when capacity exceeded
     */
    private static void testLRUEviction(LRUCache<String, Integer> cache) {
        System.out.println("Adding items to trigger eviction...\n");
        
        cache.put("A", 1);
        cache.put("B", 2);
        cache.put("C", 3);
        
        System.out.println("\nCache is full. Adding D will evict A (LRU)...\n");
        cache.put("D", 4);
        
        System.out.println("\nTrying to get A (should be evicted)...\n");
        cache.get("A");
        
        System.out.println("\n" + cache.getStats());
    }
    
    /**
     * Test 3: Access pattern affects eviction order
     */
    private static void testAccessPattern(LRUCache<String, Integer> cache) {
        System.out.println("Demonstrating how access affects LRU order...\n");
        
        cache.put("A", 1);
        cache.put("B", 2);
        cache.put("C", 3);
        
        System.out.println("\nAccessing A (moves to front)...\n");
        cache.get("A");
        
        System.out.println("\nAccessing B (moves to front)...\n");
        cache.get("B");
        
        System.out.println("\nNow C is LRU. Adding D will evict C...\n");
        cache.put("D", 4);
        
        System.out.println("\nVerifying C was evicted...\n");
        cache.get("C");
    }
    
    /**
     * Test 4: Updating existing key
     */
    private static void testUpdate(LRUCache<String, Integer> cache) {
        System.out.println("Testing update of existing key...\n");
        
        cache.put("A", 1);
        cache.put("B", 2);
        cache.put("C", 3);
        
        System.out.println("\nUpdating B from 2 to 20 (moves to front)...\n");
        cache.put("B", 20);
        
        System.out.println("\nVerifying updated value...\n");
        Integer val = cache.get("B");
        System.out.println("B = " + val);
    }
    
    /**
     * Real-world example: Web page caching
     */
    private static void demonstratePageCache() {
        System.out.println("Simulating web page cache (capacity = 4)...\n");
        
        LRUCache<String, String> pageCache = new LRUCache<>(4);
        
        // Simulate page visits
        String[] pageVisits = {
            "/home", "/about", "/products", "/contact",
            "/home",      // Cache hit
            "/pricing",   // Evicts /about
            "/products",  // Cache hit
            "/home",      // Cache hit
            "/blog"       // Evicts /contact
        };
        
        for (String page : pageVisits) {
            System.out.println("Visiting: " + page);
            
            String content = pageCache.get(page);
            
            if (content == null) {
                // Cache miss - load from "database"
                content = "Content for " + page;
                pageCache.put(page, content);
            }
            
            System.out.println();
        }
        
        System.out.println("\nFinal " + pageCache.getStats());
    }
}

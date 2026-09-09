import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding Window Rate Limiter Implementation
 * 
 * Interview Key Points:
 * - More accurate than fixed window
 * - Prevents boundary burst problem
 * - Uses sliding time window
 * - Memory intensive (stores all requests)
 * 
 * Algorithm:
 * 1. Store timestamp of each request
 * 2. For new request, count requests in last N seconds
 * 3. Allow if count < limit
 * 4. Clean up old timestamps
 * 
 * Time Complexity: O(N) where N = requests in window
 * Space Complexity: O(users * requests per window)
 * 
 * Advantages:
 * - Highly accurate
 * - No boundary issues
 * - Fair rate limiting
 * 
 * Disadvantages:
 * - Memory intensive
 * - Slower than token bucket
 * - Need to store all timestamps
 */
public class SlidingWindowRateLimiter implements RateLimiter {
    
    private final int maxRequests;      // Max requests allowed
    private final long windowSizeMs;    // Window size in milliseconds
    private final Map<String, Queue<Long>> userRequests;
    
    /**
     * Constructor
     * 
     * @param maxRequests Max requests allowed in window
     * @param windowSizeSeconds Window size in seconds
     */
    public SlidingWindowRateLimiter(int maxRequests, long windowSizeSeconds) {
        this.maxRequests = maxRequests;
        this.windowSizeMs = windowSizeSeconds * 1000;
        this.userRequests = new ConcurrentHashMap<>();
    }
    
    @Override
    public synchronized boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();
        
        Queue<Long> requests = userRequests.computeIfAbsent(
            userId, k -> new LinkedList<>());
        
        // Remove requests outside sliding window
        cleanupOldRequests(requests, now);
        
        // Check if under limit
        if (requests.size() < maxRequests) {
            requests.offer(now);
            System.out.println("✓ Request ALLOWED for " + userId + 
                " (" + requests.size() + "/" + maxRequests + " in window)");
            return true;
        }
        
        System.out.println("❌ Request DENIED for " + userId + 
            " (" + requests.size() + "/" + maxRequests + " - limit reached)");
        return false;
    }
    
    /**
     * Remove requests older than sliding window
     */
    private void cleanupOldRequests(Queue<Long> requests, long now) {
        long windowStart = now - windowSizeMs;
        
        while (!requests.isEmpty() && requests.peek() < windowStart) {
            requests.poll();
        }
    }
    
    @Override
    public int getRemainingQuota(String userId) {
        Queue<Long> requests = userRequests.get(userId);
        
        if (requests == null) {
            return maxRequests;
        }
        
        cleanupOldRequests(requests, System.currentTimeMillis());
        return maxRequests - requests.size();
    }
    
    @Override
    public void reset(String userId) {
        userRequests.remove(userId);
        System.out.println("🔄 Reset rate limiter for " + userId);
    }
    
    /**
     * Get detailed status
     */
    public String getStatus(String userId) {
        Queue<Long> requests = userRequests.get(userId);
        
        if (requests == null) {
            return userId + ": No requests yet";
        }
        
        cleanupOldRequests(requests, System.currentTimeMillis());
        return String.format("%s: %d/%d requests in window", 
            userId, requests.size(), maxRequests);
    }
}

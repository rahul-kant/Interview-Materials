import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token Bucket Rate Limiter Implementation
 * 
 * Interview Key Points:
 * - Allows burst traffic up to bucket capacity
 * - Tokens refill at constant rate
 * - Smooth traffic distribution
 * - Memory efficient
 * 
 * Algorithm:
 * 1. Bucket has max capacity of tokens
 * 2. Tokens refill at fixed rate (refillRate per second)
 * 3. Request consumes 1 token
 * 4. Request allowed if token available
 * 
 * Time Complexity: O(1)
 * Space Complexity: O(number of users)
 * 
 * Advantages:
 * - Handles burst traffic well
 * - Simple to implement
 * - Smooth rate limiting
 * 
 * Disadvantages:
 * - Can allow burst of requests
 * - Memory per user
 */
public class TokenBucketRateLimiter implements RateLimiter {
    
    /**
     * Bucket state for each user
     */
    private static class Bucket {
        int tokens;              // Current tokens
        long lastRefillTime;     // Last refill timestamp
        
        Bucket(int capacity) {
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }
    }
    
    private final int capacity;           // Max tokens in bucket
    private final int refillRate;         // Tokens per second
    private final Map<String, Bucket> buckets;
    
    /**
     * Constructor
     * 
     * @param capacity Max tokens (max burst)
     * @param refillRate Tokens added per second
     */
    public TokenBucketRateLimiter(int capacity, int refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.buckets = new ConcurrentHashMap<>();
    }
    
    @Override
    public synchronized boolean allowRequest(String userId) {
        Bucket bucket = buckets.computeIfAbsent(userId, k -> new Bucket(capacity));
        
        // Refill tokens based on time passed
        refillBucket(bucket);
        
        // Check if token available
        if (bucket.tokens > 0) {
            bucket.tokens--;
            System.out.println("✓ Request ALLOWED for " + userId + 
                " (Tokens remaining: " + bucket.tokens + ")");
            return true;
        }
        
        System.out.println("❌ Request DENIED for " + userId + 
            " (Rate limit exceeded)");
        return false;
    }
    
    /**
     * Refill tokens based on elapsed time
     */
    private void refillBucket(Bucket bucket) {
        long now = System.currentTimeMillis();
        long timePassed = now - bucket.lastRefillTime;
        
        // Calculate tokens to add (refillRate tokens per second)
        int tokensToAdd = (int) ((timePassed / 1000.0) * refillRate);
        
        if (tokensToAdd > 0) {
            bucket.tokens = Math.min(capacity, bucket.tokens + tokensToAdd);
            bucket.lastRefillTime = now;
        }
    }
    
    @Override
    public int getRemainingQuota(String userId) {
        Bucket bucket = buckets.get(userId);
        if (bucket == null) {
            return capacity;
        }
        
        refillBucket(bucket);
        return bucket.tokens;
    }
    
    @Override
    public void reset(String userId) {
        buckets.remove(userId);
        System.out.println("🔄 Reset rate limiter for " + userId);
    }
    
    /**
     * Get current status
     */
    public String getStatus(String userId) {
        Bucket bucket = buckets.get(userId);
        if (bucket == null) {
            return userId + ": Not tracked yet";
        }
        
        refillBucket(bucket);
        return String.format("%s: %d/%d tokens", userId, bucket.tokens, capacity);
    }
}

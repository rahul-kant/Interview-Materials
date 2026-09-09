/**
 * RateLimiter Interface - Base interface for rate limiting
 * 
 * Interview Key Points:
 * - Strategy pattern for different algorithms
 * - Clean separation of concerns
 * - Easy to add new algorithms
 */
public interface RateLimiter {
    
    /**
     * Check if request is allowed
     * 
     * @param userId User identifier
     * @return true if allowed, false if rate limit exceeded
     */
    boolean allowRequest(String userId);
    
    /**
     * Get remaining quota for user
     * 
     * @param userId User identifier
     * @return Number of requests remaining in current window
     */
    int getRemainingQuota(String userId);
    
    /**
     * Reset rate limiter for user
     * 
     * @param userId User identifier
     */
    void reset(String userId);
}

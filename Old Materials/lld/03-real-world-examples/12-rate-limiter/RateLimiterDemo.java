/**
 * Rate Limiter Demo - Demonstrates different rate limiting algorithms
 * 
 * Interview Key Points:
 * - Compares Token Bucket vs Sliding Window
 * - Shows burst handling
 * - Demonstrates refill behavior
 * - Tests edge cases
 */
public class RateLimiterDemo {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     RATE LIMITER SYSTEM DEMO             ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
        
        System.out.println("═══════════════════════════════════════════");
        System.out.println("TEST 1: Token Bucket Rate Limiter");
        System.out.println("═══════════════════════════════════════════\n");
        testTokenBucket();
        
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("TEST 2: Sliding Window Rate Limiter");
        System.out.println("═══════════════════════════════════════════\n");
        testSlidingWindow();
        
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("TEST 3: Burst Traffic Handling");
        System.out.println("═══════════════════════════════════════════\n");
        testBurstTraffic();
        
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("TEST 4: Multiple Users");
        System.out.println("═══════════════════════════════════════════\n");
        testMultipleUsers();
        
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("REAL-WORLD: API Rate Limiting");
        System.out.println("═══════════════════════════════════════════\n");
        demonstrateAPIRateLimit();
    }
    
    /**
     * Test Token Bucket algorithm
     */
    private static void testTokenBucket() {
        System.out.println("Creating Token Bucket (capacity=5, refill=2/sec)\n");
        
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(5, 2);
        String user = "user1";
        
        // Make 5 requests (should all succeed)
        System.out.println("Making 5 requests (burst)...\n");
        for (int i = 1; i <= 5; i++) {
            System.out.print("Request " + i + ": ");
            limiter.allowRequest(user);
        }
        
        // Make 6th request (should fail)
        System.out.print("\nRequest 6: ");
        limiter.allowRequest(user);
        
        System.out.println("\nRemaining quota: " + limiter.getRemainingQuota(user));
    }
    
    /**
     * Test Sliding Window algorithm
     */
    private static void testSlidingWindow() {
        System.out.println("Creating Sliding Window (limit=3, window=5sec)\n");
        
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(3, 5);
        String user = "user1";
        
        // Make 3 requests (should all succeed)
        System.out.println("Making 3 requests...\n");
        for (int i = 1; i <= 3; i++) {
            System.out.print("Request " + i + ": ");
            limiter.allowRequest(user);
        }
        
        // Make 4th request (should fail)
        System.out.print("\nRequest 4: ");
        limiter.allowRequest(user);
        
        System.out.println("\nRemaining quota: " + limiter.getRemainingQuota(user));
    }
    
    /**
     * Test burst traffic handling
     */
    private static void testBurstTraffic() throws InterruptedException {
        System.out.println("Testing burst traffic with Token Bucket...\n");
        
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(3, 1);
        String user = "user1";
        
        // Use all tokens
        System.out.println("Using all 3 tokens...\n");
        for (int i = 1; i <= 3; i++) {
            limiter.allowRequest(user);
        }
        
        System.out.println("\nWaiting 2 seconds for refill...\n");
        Thread.sleep(2000);
        
        System.out.println("After refill (should have ~2 tokens)...\n");
        limiter.allowRequest(user);
        limiter.allowRequest(user);
        limiter.allowRequest(user); // Should fail
    }
    
    /**
     * Test multiple users
     */
    private static void testMultipleUsers() {
        System.out.println("Testing rate limiting for multiple users...\n");
        
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(2, 1);
        
        String[] users = {"Alice", "Bob", "Charlie"};
        
        for (String user : users) {
            System.out.println("\n--- Requests from " + user + " ---");
            for (int i = 1; i <= 3; i++) {
                System.out.print("Request " + i + ": ");
                limiter.allowRequest(user);
            }
        }
    }
    
    /**
     * Real-world example: API rate limiting
     */
    private static void demonstrateAPIRateLimit() {
        System.out.println("Simulating REST API rate limiting (100 req/min per user)\n");
        
        // 100 requests per 60 seconds
        SlidingWindowRateLimiter apiLimiter = new SlidingWindowRateLimiter(100, 60);
        
        String apiKey = "api-key-12345";
        
        System.out.println("Simulating API calls from " + apiKey + "...\n");
        
        // Simulate some API calls
        int[] callCounts = {10, 20, 30, 25, 20};
        int totalCalls = 0;
        
        for (int i = 0; i < callCounts.length; i++) {
            System.out.println("Batch " + (i + 1) + ": Making " + callCounts[i] + " calls");
            
            int allowed = 0;
            int denied = 0;
            
            for (int j = 0; j < callCounts[i]; j++) {
                if (apiLimiter.allowRequest(apiKey)) {
                    allowed++;
                } else {
                    denied++;
                }
            }
            
            totalCalls += allowed;
            
            System.out.println("  Allowed: " + allowed + ", Denied: " + denied);
            System.out.println("  Remaining quota: " + 
                apiLimiter.getRemainingQuota(apiKey) + "\n");
        }
        
        System.out.println("Total API calls processed: " + totalCalls);
        System.out.println("\n💡 In production, return HTTP 429 (Too Many Requests) when rate limit exceeded");
        System.out.println("💡 Include headers: X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset");
    }
}

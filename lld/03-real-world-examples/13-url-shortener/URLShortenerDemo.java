import java.time.LocalDateTime;

/**
 * URL Shortener Demo
 * 
 * Demonstrates:
 * 1. Basic URL shortening
 * 2. Custom aliases
 * 3. URL expansion (redirect)
 * 4. Click analytics
 * 5. Expiration handling
 * 6. User management
 */
public class URLShortenerDemo {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     URL SHORTENER SYSTEM DEMO            ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
        
        URLShortener shortener = URLShortener.getInstance();
        
        System.out.println("═══════════════════════════════════════════");
        System.out.println("TEST 1: Basic URL Shortening");
        System.out.println("═══════════════════════════════════════════\n");
        testBasicShortening(shortener);
        
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("TEST 2: Custom Aliases");
        System.out.println("═══════════════════════════════════════════\n");
        testCustomAliases(shortener);
        
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("TEST 3: URL Expansion & Analytics");
        System.out.println("═══════════════════════════════════════════\n");
        testExpansionAndAnalytics(shortener);
        
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("TEST 4: Expiration");
        System.out.println("═══════════════════════════════════════════\n");
        testExpiration(shortener);
        
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("TEST 5: User Management");
        System.out.println("═══════════════════════════════════════════\n");
        testUserManagement(shortener);
        
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("REAL-WORLD: Campaign Tracking");
        System.out.println("═══════════════════════════════════════════\n");
        demonstrateCampaignTracking(shortener);
    }
    
    /**
     * Test 1: Basic URL shortening
     */
    private static void testBasicShortening(URLShortener shortener) {
        System.out.println("Shortening long URLs...\n");
        
        String url1 = "https://www.example.com/very/long/url/with/many/parameters?id=123&source=email";
        String short1 = shortener.shortenURL(url1, "user1");
        System.out.println("Long:  " + url1);
        System.out.println("Short: " + short1);
        
        System.out.println("\n---");
        
        String url2 = "https://github.com/username/repository/blob/main/src/components/LongComponent.tsx";
        String short2 = shortener.shortenURL(url2, "user1");
        System.out.println("Long:  " + url2);
        System.out.println("Short: " + short2);
        
        // Try shortening same URL again
        System.out.println("\n--- Shortening same URL again ---\n");
        String short3 = shortener.shortenURL(url1, "user1");
    }
    
    /**
     * Test 2: Custom aliases
     */
    private static void testCustomAliases(URLShortener shortener) {
        System.out.println("Creating custom short URLs...\n");
        
        // Success case
        String url = "https://mycompany.com/product/amazing-offer";
        String custom = shortener.createCustomURL(url, "offer2024", "user2");
        System.out.println("Custom URL: " + custom);
        
        // Conflict case
        System.out.println("\n--- Trying to use same alias ---\n");
        String url2 = "https://different.com";
        String custom2 = shortener.createCustomURL(url2, "offer2024", "user2");
        if (custom2 == null) {
            System.out.println("✓ Alias conflict detected correctly");
        }
    }
    
    /**
     * Test 3: URL expansion and analytics
     */
    private static void testExpansionAndAnalytics(URLShortener shortener) {
        System.out.println("Expanding URLs and tracking clicks...\n");
        
        // Create URL
        String longUrl = "https://blog.example.com/article";
        String shortUrl = shortener.shortenURL(longUrl, "user3");
        String shortCode = shortUrl.substring(shortUrl.lastIndexOf('/') + 1);
        
        // Simulate clicks
        System.out.println("\n--- Simulating 5 clicks ---\n");
        for (int i = 1; i <= 5; i++) {
            System.out.println("Click " + i + ":");
            shortener.expandURL(shortCode);
        }
        
        // View analytics
        System.out.println("\n--- Analytics ---\n");
        System.out.println(shortener.getAnalytics(shortCode));
    }
    
    /**
     * Test 4: URL expiration
     */
    private static void testExpiration(URLShortener shortener) throws InterruptedException {
        System.out.println("Testing URL expiration...\n");
        
        // Create URL
        String url = "https://limited-time-offer.com";
        String shortUrl = shortener.shortenURL(url, "user4");
        String shortCode = shortUrl.substring(shortUrl.lastIndexOf('/') + 1);
        
        // Set expiration to 2 seconds from now
        LocalDateTime expires = LocalDateTime.now().plusSeconds(2);
        shortener.setExpiration(shortCode, expires);
        
        // Try accessing before expiration
        System.out.println("\n--- Before expiration ---\n");
        shortener.expandURL(shortCode);
        
        // Wait for expiration
        System.out.println("\nWaiting 3 seconds for expiration...\n");
        Thread.sleep(3000);
        
        // Try accessing after expiration
        System.out.println("--- After expiration ---\n");
        String result = shortener.expandURL(shortCode);
        if (result == null) {
            System.out.println("✓ URL correctly expired");
        }
    }
    
    /**
     * Test 5: User management
     */
    private static void testUserManagement(URLShortener shortener) {
        System.out.println("Managing user URLs...\n");
        
        String userId = "alice";
        
        // Create multiple URLs for user
        shortener.shortenURL("https://alice.com/blog/post1", userId);
        shortener.shortenURL("https://alice.com/blog/post2", userId);
        shortener.createCustomURL("https://alice.com/about", "alice-about", userId);
        
        System.out.println("\n--- Alice's URLs ---\n");
        var urls = shortener.getUserURLs(userId);
        for (URL url : urls) {
            System.out.println("  " + url);
        }
        
        System.out.println("\n" + shortener.getStats());
    }
    
    /**
     * Real-world example: Campaign tracking
     */
    private static void demonstrateCampaignTracking(URLShortener shortener) {
        System.out.println("Simulating marketing campaign tracking...\n");
        
        String marketingUrl = "https://shop.example.com/summer-sale?utm_source=email&utm_campaign=summer2024";
        
        // Create branded short URL
        String campaignUrl = shortener.createCustomURL(marketingUrl, "summer24", "marketing-team");
        
        System.out.println("Campaign URL created: " + campaignUrl);
        System.out.println("\nSimulating campaign clicks from different sources...\n");
        
        // Simulate clicks from email campaign
        for (int i = 1; i <= 10; i++) {
            shortener.expandURL("summer24");
        }
        
        // View campaign analytics
        System.out.println("\n--- Campaign Analytics ---\n");
        System.out.println(shortener.getAnalytics("summer24"));
        
        System.out.println("\n💡 Production Features:");
        System.out.println("   - Track geographic location");
        System.out.println("   - Track referrer source");
        System.out.println("   - Track device type");
        System.out.println("   - Generate QR codes");
        System.out.println("   - A/B testing support");
    }
}

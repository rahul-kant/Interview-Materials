import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * URL Shortener Service
 * 
 * EXPLANATION:
 * - Main service for URL shortening
 * - Singleton pattern
 * - Counter-based short code generation
 * - In-memory storage (can be replaced with DB)
 * 
 * DESIGN PATTERNS:
 * - Singleton: Single instance
 * - Repository: Data access abstraction
 * 
 * INTERVIEW POINTS:
 * - Counter vs Hash-based generation
 * - Collision handling
 * - Custom alias support
 * - Analytics tracking
 */
public class URLShortener {
    private static URLShortener instance;
    private final Map<String, URL> shortToUrl;
    private final Map<String, String> longToShort;
    private final AtomicLong counter;
    private static final String BASE_URL = "https://short.url/";
    private static final int SHORT_CODE_LENGTH = 6;
    
    private URLShortener() {
        this.shortToUrl = new ConcurrentHashMap<>();
        this.longToShort = new ConcurrentHashMap<>();
        this.counter = new AtomicLong(1000000); // Start from 1M
    }
    
    public static synchronized URLShortener getInstance() {
        if (instance == null) {
            instance = new URLShortener();
        }
        return instance;
    }
    
    /**
     * Shorten URL
     * 
     * @param longUrl Original URL
     * @param userId User creating short URL
     * @return Short URL
     */
    public String shortenURL(String longUrl, String userId) {
        // Check if already shortened
        if (longToShort.containsKey(longUrl)) {
            String shortCode = longToShort.get(longUrl);
            System.out.println("✓ URL already shortened: " + BASE_URL + shortCode);
            return BASE_URL + shortCode;
        }
        
        // Generate short code using counter
        long id = counter.getAndIncrement();
        String shortCode = Base62Encoder.encode(id);
        
        // Create URL entity
        URL url = new URL(shortCode, longUrl, userId);
        
        // Store mappings
        shortToUrl.put(shortCode, url);
        longToShort.put(longUrl, shortCode);
        
        System.out.println("✓ Created short URL: " + BASE_URL + shortCode);
        return BASE_URL + shortCode;
    }
    
    /**
     * Create custom short URL
     * 
     * @param longUrl Original URL
     * @param customAlias Custom short code
     * @param userId User ID
     * @return Short URL or null if alias taken
     */
    public String createCustomURL(String longUrl, String customAlias, String userId) {
        // Check if alias already taken
        if (shortToUrl.containsKey(customAlias)) {
            System.out.println("❌ Custom alias already taken: " + customAlias);
            return null;
        }
        
        // Create URL with custom alias
        URL url = new URL(customAlias, longUrl, userId);
        shortToUrl.put(customAlias, url);
        longToShort.put(longUrl, customAlias);
        
        System.out.println("✓ Created custom short URL: " + BASE_URL + customAlias);
        return BASE_URL + customAlias;
    }
    
    /**
     * Get original URL and record click
     * 
     * @param shortCode Short code
     * @return Original URL or null if not found/expired
     */
    public String expandURL(String shortCode) {
        URL url = shortToUrl.get(shortCode);
        
        if (url == null) {
            System.out.println("❌ Short URL not found: " + shortCode);
            return null;
        }
        
        // Check expiration
        if (url.isExpired()) {
            System.out.println("❌ Short URL expired: " + shortCode);
            return null;
        }
        
        // Record click
        url.recordClick();
        
        System.out.println("✓ Redirecting to: " + url.getOriginalUrl());
        return url.getOriginalUrl();
    }
    
    /**
     * Set expiration for URL
     */
    public void setExpiration(String shortCode, LocalDateTime expiresAt) {
        URL url = shortToUrl.get(shortCode);
        if (url != null) {
            url.setExpiresAt(expiresAt);
            System.out.println("✓ Expiration set for " + shortCode + ": " + expiresAt);
        }
    }
    
    /**
     * Delete short URL
     */
    public boolean deleteURL(String shortCode) {
        URL url = shortToUrl.remove(shortCode);
        if (url != null) {
            longToShort.remove(url.getOriginalUrl());
            System.out.println("✓ Deleted short URL: " + shortCode);
            return true;
        }
        return false;
    }
    
    /**
     * Get analytics for short URL
     */
    public String getAnalytics(String shortCode) {
        URL url = shortToUrl.get(shortCode);
        if (url == null) {
            return "URL not found";
        }
        
        return String.format(
            "Analytics for %s:\n" +
            "  Original: %s\n" +
            "  Clicks: %d\n" +
            "  Created: %s\n" +
            "  Last Accessed: %s\n" +
            "  Expires: %s",
            shortCode,
            url.getOriginalUrl(),
            url.getClickCount(),
            url.getCreatedAt(),
            url.getLastAccessedAt() != null ? url.getLastAccessedAt() : "Never",
            url.getExpiresAt() != null ? url.getExpiresAt() : "Never"
        );
    }
    
    /**
     * Get user's URLs
     */
    public List<URL> getUserURLs(String userId) {
        List<URL> userUrls = new ArrayList<>();
        for (URL url : shortToUrl.values()) {
            if (url.getUserId().equals(userId)) {
                userUrls.add(url);
            }
        }
        return userUrls;
    }
    
    /**
     * Get statistics
     */
    public String getStats() {
        return String.format(
            "URL Shortener Stats:\n" +
            "  Total URLs: %d\n" +
            "  Counter: %d\n" +
            "  Base URL: %s",
            shortToUrl.size(),
            counter.get(),
            BASE_URL
        );
    }
}

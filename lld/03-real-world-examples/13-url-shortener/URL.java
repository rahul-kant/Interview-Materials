import java.time.LocalDateTime;

/**
 * URL Entity - Represents a shortened URL
 * 
 * EXPLANATION:
 * - Stores original and short URL
 * - Tracks creation and expiration
 * - Analytics data (clicks)
 * 
 * SOLID PRINCIPLES:
 * - SRP: URL entity only manages URL data
 */
public class URL {
    private String shortCode;
    private String originalUrl;
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private int clickCount;
    private LocalDateTime lastAccessedAt;
    
    public URL(String shortCode, String originalUrl, String userId) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = null; // No expiration by default
        this.clickCount = 0;
        this.lastAccessedAt = null;
    }
    
    /**
     * Record a click/access
     */
    public void recordClick() {
        this.clickCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    /**
     * Check if URL has expired
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    // Getters and Setters
    public String getShortCode() { return shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public String getUserId() { return userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public int getClickCount() { return clickCount; }
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    
    @Override
    public String toString() {
        return String.format("URL{short=%s, clicks=%d, created=%s}", 
            shortCode, clickCount, createdAt);
    }
}

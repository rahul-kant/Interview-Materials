import java.time.LocalDateTime;
import java.util.*;

/**
 * Post class represents a social media post
 * 
 * EXPLANATION:
 * - Contains post content and metadata
 * - Tracks likes and comments
 * - Stores timestamp for chronological ordering
 * 
 * DESIGN PATTERNS:
 * - Can use Factory pattern for different post types
 * - Observer pattern for notifications
 */
public class Post {
    private String postId;
    private String userId;  // Author
    private String content;
    private LocalDateTime timestamp;
    private Set<String> likes;  // User IDs who liked
    private List<Comment> comments;
    private int shareCount;
    
    public Post(String postId, String userId, String content) {
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.likes = new HashSet<>();
        this.comments = new ArrayList<>();
        this.shareCount = 0;
    }
    
    /**
     * Like the post
     */
    public void like(String userId) {
        likes.add(userId);
    }
    
    /**
     * Unlike the post
     */
    public void unlike(String userId) {
        likes.remove(userId);
    }
    
    /**
     * Add comment
     */
    public void addComment(Comment comment) {
        comments.add(comment);
    }
    
    /**
     * Share the post
     */
    public void share() {
        shareCount++;
    }
    
    /**
     * Calculate engagement score for ranking
     * 
     * EXPLANATION:
     * - Higher engagement = higher score
     * - Used in feed ranking algorithms
     */
    public double getEngagementScore() {
        return likes.size() * 1.0 + 
               comments.size() * 2.0 + 
               shareCount * 3.0;
    }
    
    // Getters
    public String getPostId() { return postId; }
    public String getUserId() { return userId; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Set<String> getLikes() { return new HashSet<>(likes); }
    public List<Comment> getComments() { return new ArrayList<>(comments); }
    public int getLikeCount() { return likes.size(); }
    public int getCommentCount() { return comments.size(); }
    public int getShareCount() { return shareCount; }
    
    @Override
    public String toString() {
        return "Post #" + postId + " by " + userId + 
               "\n  " + content +
               "\n  " + likes.size() + " likes, " + 
               comments.size() + " comments, " +
               shareCount + " shares" +
               "\n  Posted: " + timestamp;
    }
}

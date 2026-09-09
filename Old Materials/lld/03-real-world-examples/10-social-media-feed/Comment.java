import java.time.LocalDateTime;

/**
 * Comment class represents a comment on a post
 * 
 * EXPLANATION:
 * - Simple comment with author and content
 * - Timestamp for ordering
 * - Can be extended for nested comments (Composite pattern)
 */
public class Comment {
    private String commentId;
    private String userId;
    private String content;
    private LocalDateTime timestamp;
    
    public Comment(String commentId, String userId, String content) {
        this.commentId = commentId;
        this.userId = userId;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters
    public String getCommentId() { return commentId; }
    public String getUserId() { return userId; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return userId + ": " + content;
    }
}

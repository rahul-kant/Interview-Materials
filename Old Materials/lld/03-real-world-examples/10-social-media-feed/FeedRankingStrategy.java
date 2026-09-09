import java.util.*;

/**
 * Strategy interface for feed ranking algorithms
 * 
 * EXPLANATION:
 * - Strategy pattern for flexible ranking
 * - Different algorithms for different users
 * - Easy to add new ranking strategies
 * 
 * SOLID PRINCIPLES:
 * - OCP: Extensible with new strategies
 * - SRP: Each strategy has one responsibility
 */
public interface FeedRankingStrategy {
    /**
     * Rank posts for a user's feed
     * 
     * @param posts List of posts to rank
     * @param userId User viewing the feed
     * @return Ranked list of posts
     */
    List<Post> rankPosts(List<Post> posts, String userId);
}

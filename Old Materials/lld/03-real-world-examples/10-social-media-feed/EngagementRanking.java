import java.util.*;
import java.util.stream.Collectors;

/**
 * Engagement-based ranking algorithm
 * 
 * EXPLANATION:
 * - Ranks by engagement (likes, comments, shares)
 * - Shows most popular posts first
 * - Similar to Facebook/Instagram algorithm
 * 
 * INTERVIEW POINT: How to balance recency and engagement?
 * - Add time decay factor
 * - Recent posts get boost
 */
public class EngagementRanking implements FeedRankingStrategy {
    
    @Override
    public List<Post> rankPosts(List<Post> posts, String userId) {
        return posts.stream()
                .sorted((p1, p2) -> 
                    Double.compare(p2.getEngagementScore(), 
                                 p1.getEngagementScore()))
                .collect(Collectors.toList());
    }
}

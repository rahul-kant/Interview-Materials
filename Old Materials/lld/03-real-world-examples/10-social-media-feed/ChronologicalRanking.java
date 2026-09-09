import java.util.*;
import java.util.stream.Collectors;

/**
 * Chronological ranking - newest first
 * 
 * EXPLANATION:
 * - Simple time-based ranking
 * - Shows most recent posts first
 * - Used in Twitter's "Latest Tweets" view
 */
public class ChronologicalRanking implements FeedRankingStrategy {
    
    @Override
    public List<Post> rankPosts(List<Post> posts, String userId) {
        return posts.stream()
                .sorted((p1, p2) -> p2.getTimestamp().compareTo(p1.getTimestamp()))
                .collect(Collectors.toList());
    }
}

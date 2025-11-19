import java.util.*;

/**
 * Main Social Media System
 * 
 * EXPLANATION:
 * - Manages users, posts, and feeds
 * - Uses Strategy pattern for feed ranking
 * - Singleton pattern for single instance
 * 
 * DESIGN PATTERNS:
 * - Singleton: Only one system instance
 * - Facade: Simplifies complex operations
 * - Strategy: Pluggable ranking algorithms
 */
public class SocialMediaSystem {
    private static SocialMediaSystem instance;
    private Map<String, User> users;
    private Map<String, Post> posts;
    private FeedRankingStrategy rankingStrategy;
    private int postIdCounter;
    private int commentIdCounter;
    
    private SocialMediaSystem() {
        this.users = new HashMap<>();
        this.posts = new HashMap<>();
        this.rankingStrategy = new ChronologicalRanking();
        this.postIdCounter = 1;
        this.commentIdCounter = 1;
    }
    
    public static synchronized SocialMediaSystem getInstance() {
        if (instance == null) {
            instance = new SocialMediaSystem();
        }
        return instance;
    }
    
    /**
     * Register user
     */
    public void addUser(User user) {
        users.put(user.getUserId(), user);
        System.out.println("User registered: " + user.getName());
    }
    
    /**
     * Create post
     */
    public Post createPost(String userId, String content) {
        String postId = "P" + postIdCounter++;
        Post post = new Post(postId, userId, content);
        posts.put(postId, post);
        System.out.println("Post created: " + postId);
        return post;
    }
    
    /**
     * Generate feed for user
     * 
     * EXPLANATION:
     * - Gets posts from followed users
     * - Ranks using strategy
     * - Returns personalized feed
     * 
     * INTERVIEW POINTS:
     * - Pull model: Generate on request (used here)
     * - Push model: Pre-generate and cache
     * - Hybrid: Cache for active users, generate for others
     */
    public List<Post> generateFeed(String userId) {
        User user = users.get(userId);
        if (user == null) return new ArrayList<>();
        
        // Get posts from followed users
        List<Post> feedPosts = new ArrayList<>();
        for (String followedId : user.getFollowing()) {
            for (Post post : posts.values()) {
                if (post.getUserId().equals(followedId)) {
                    feedPosts.add(post);
                }
            }
        }
        
        // Rank posts
        return rankingStrategy.rankPosts(feedPosts, userId);
    }
    
    /**
     * Like a post
     */
    public void likePost(String postId, String userId) {
        Post post = posts.get(postId);
        if (post != null) {
            post.like(userId);
            System.out.println(userId + " liked post " + postId);
        }
    }
    
    /**
     * Comment on post
     */
    public void commentOnPost(String postId, String userId, String content) {
        Post post = posts.get(postId);
        if (post != null) {
            String commentId = "C" + commentIdCounter++;
            Comment comment = new Comment(commentId, userId, content);
            post.addComment(comment);
            System.out.println(userId + " commented on post " + postId);
        }
    }
    
    /**
     * Share post
     */
    public void sharePost(String postId) {
        Post post = posts.get(postId);
        if (post != null) {
            post.share();
            System.out.println("Post " + postId + " shared");
        }
    }
    
    /**
     * Set ranking strategy
     */
    public void setRankingStrategy(FeedRankingStrategy strategy) {
        this.rankingStrategy = strategy;
    }
    
    /**
     * Get user
     */
    public User getUser(String userId) {
        return users.get(userId);
    }
    
    /**
     * Get post
     */
    public Post getPost(String postId) {
        return posts.get(postId);
    }
    
    /**
     * Display feed
     */
    public void displayFeed(String userId) {
        List<Post> feed = generateFeed(userId);
        System.out.println("\n=== Feed for " + users.get(userId).getName() + " ===");
        for (Post post : feed) {
            System.out.println("\n" + post);
        }
    }
}

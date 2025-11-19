/**
 * Social Media Feed System Demo
 * 
 * This demo demonstrates:
 * 1. User registration and following
 * 2. Creating posts
 * 3. Liking, commenting, sharing
 * 4. Feed generation with different ranking strategies
 * 5. Personalized feeds
 * 
 * LEARNING OBJECTIVES:
 * - Understand feed generation approaches
 * - Learn Strategy pattern for ranking
 * - See social graph implementation
 */
public class SocialMediaDemo {
    
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("  SOCIAL MEDIA FEED SYSTEM - DEMO");
        System.out.println("==========================================\n");
        
        SocialMediaSystem system = SocialMediaSystem.getInstance();
        
        // Demo 1: Setup users
        System.out.println("=== DEMO 1: User Registration ===");
        setupUsers(system);
        
        // Demo 2: Follow users
        System.out.println("\n\n=== DEMO 2: Follow Users ===");
        setupFollowers(system);
        
        // Demo 3: Create posts
        System.out.println("\n\n=== DEMO 3: Create Posts ===");
        createPosts(system);
        
        // Demo 4: Engage with posts
        System.out.println("\n\n=== DEMO 4: Engage with Posts ===");
        engagePosts(system);
        
        // Demo 5: Chronological feed
        System.out.println("\n\n=== DEMO 5: Chronological Feed ===");
        showChronologicalFeed(system);
        
        // Demo 6: Engagement-based feed
        System.out.println("\n\n=== DEMO 6: Engagement-Based Feed ===");
        showEngagementFeed(system);
        
        System.out.println("\n\n==========================================");
        System.out.println("  DEMO COMPLETE");
        System.out.println("==========================================");
    }
    
    /**
     * Demo 1: Register users
     */
    private static void setupUsers(SocialMediaSystem system) {
        User alice = new User("alice", "Alice", "alice@social.com");
        User bob = new User("bob", "Bob", "bob@social.com");
        User charlie = new User("charlie", "Charlie", "charlie@social.com");
        User david = new User("david", "David", "david@social.com");
        
        system.addUser(alice);
        system.addUser(bob);
        system.addUser(charlie);
        system.addUser(david);
        
        System.out.println("\n✓ 4 users registered");
    }
    
    /**
     * Demo 2: Setup follower relationships
     */
    private static void setupFollowers(SocialMediaSystem system) {
        User alice = system.getUser("alice");
        User bob = system.getUser("bob");
        User charlie = system.getUser("charlie");
        User david = system.getUser("david");
        
        // Alice follows Bob and Charlie
        alice.follow(bob);
        alice.follow(charlie);
        
        // Bob follows Charlie and David
        bob.follow(charlie);
        bob.follow(david);
        
        // Charlie follows David
        charlie.follow(david);
        
        System.out.println("\n--- Follower Stats ---");
        System.out.println(alice);
        System.out.println(bob);
        System.out.println(charlie);
        System.out.println(david);
    }
    
    /**
     * Demo 3: Create posts
     */
    private static void createPosts(SocialMediaSystem system) {
        // Bob's posts
        Post p1 = system.createPost("bob", "Just joined this platform! Excited to connect!");
        Post p2 = system.createPost("bob", "Beautiful sunset today 🌅");
        
        // Charlie's posts
        Post p3 = system.createPost("charlie", "Learning about design patterns in Java");
        Post p4 = system.createPost("charlie", "Coffee time ☕");
        
        // David's posts
        Post p5 = system.createPost("david", "Working on a new project");
        
        System.out.println("\n✓ 5 posts created");
    }
    
    /**
     * Demo 4: Engage with posts (likes, comments, shares)
     */
    private static void engagePosts(SocialMediaSystem system) {
        // Engagement on Bob's sunset post
        system.likePost("P2", "alice");
        system.likePost("P2", "charlie");
        system.likePost("P2", "david");
        system.commentOnPost("P2", "alice", "Gorgeous!");
        system.commentOnPost("P2", "charlie", "Where was this?");
        system.sharePost("P2");
        
        // Engagement on Charlie's design patterns post
        system.likePost("P3", "alice");
        system.likePost("P3", "bob");
        system.commentOnPost("P3", "alice", "Great topic!");
        system.sharePost("P3");
        system.sharePost("P3");
        
        // Some engagement on David's post
        system.likePost("P5", "bob");
        system.commentOnPost("P5", "bob", "Good luck!");
        
        System.out.println("\n✓ Engagement complete");
        System.out.println("Post P2: 3 likes, 2 comments, 1 share");
        System.out.println("Post P3: 2 likes, 1 comment, 2 shares");
        System.out.println("Post P5: 1 like, 1 comment, 0 shares");
    }
    
    /**
     * Demo 5: Show chronological feed
     */
    private static void showChronologicalFeed(SocialMediaSystem system) {
        system.setRankingStrategy(new ChronologicalRanking());
        System.out.println("Using: Chronological Ranking (newest first)");
        system.displayFeed("alice");
    }
    
    /**
     * Demo 6: Show engagement-based feed
     */
    private static void showEngagementFeed(SocialMediaSystem system) {
        system.setRankingStrategy(new EngagementRanking());
        System.out.println("Using: Engagement-Based Ranking (most popular first)");
        system.displayFeed("alice");
        
        System.out.println("\n--- Ranking Analysis ---");
        System.out.println("Post P2 (Bob's sunset): High engagement → Top of feed");
        System.out.println("Post P3 (Charlie's design): Medium engagement → Middle");
        System.out.println("Post P4 (Charlie's coffee): Low engagement → Bottom");
        System.out.println("\nStrategy Pattern allows easy switching between algorithms!");
    }
}

import java.util.*;

/**
 * User class represents a social media user
 * 
 * EXPLANATION:
 * - User profile with followers and following
 * - Can create posts
 * - Follow/unfollow other users
 * 
 * SOLID PRINCIPLES:
 * - SRP: User manages profile and relationships only
 * - OCP: Can extend for verified users, business accounts
 */
public class User {
    private String userId;
    private String name;
    private String email;
    private Set<String> following;  // Users this user follows
    private Set<String> followers;  // Users following this user
    
    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.following = new HashSet<>();
        this.followers = new HashSet<>();
    }
    
    /**
     * Follow another user
     */
    public void follow(User other) {
        if (!other.userId.equals(this.userId)) {
            this.following.add(other.userId);
            other.followers.add(this.userId);
            System.out.println(this.name + " followed " + other.name);
        }
    }
    
    /**
     * Unfollow a user
     */
    public void unfollow(User other) {
        this.following.remove(other.userId);
        other.followers.remove(this.userId);
        System.out.println(this.name + " unfollowed " + other.name);
    }
    
    // Getters
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Set<String> getFollowing() { return new HashSet<>(following); }
    public Set<String> getFollowers() { return new HashSet<>(followers); }
    public int getFollowingCount() { return following.size(); }
    public int getFollowersCount() { return followers.size(); }
    
    @Override
    public String toString() {
        return name + " (@" + userId + ") - " + 
               followers.size() + " followers, " +
               following.size() + " following";
    }
}

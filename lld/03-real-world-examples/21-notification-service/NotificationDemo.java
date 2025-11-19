import java.util.*;

/**
 * Notification Service Demo
 */
public class NotificationDemo {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   NOTIFICATION SERVICE DEMO              ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
        
        NotificationService service = new NotificationService();
        
        // Subscribe users
        Subscriber alice = new Subscriber("alice@example.com", 
            new HashSet<>(Arrays.asList(Channel.EMAIL, Channel.PUSH)));
        Subscriber bob = new Subscriber("bob@example.com",
            new HashSet<>(Arrays.asList(Channel.SMS)));
        
        service.subscribe("sports", alice);
        service.subscribe("sports", bob);
        service.subscribe("tech", alice);
        
        // Publish notifications
        System.out.println("\n=== Publishing Notifications ===");
        service.publish("sports", "Game starts at 7 PM!");
        service.publish("tech", "New iPhone released!");
        
        System.out.println("\n💡 Key Concepts:");
        System.out.println("   - Observer/Pub-Sub pattern");
        System.out.println("   - Multiple channels");
        System.out.println("   - Topic-based routing");
        System.out.println("   - Fan-out to subscribers");
    }
}

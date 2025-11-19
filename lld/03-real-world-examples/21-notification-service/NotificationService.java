import java.util.*;

enum Channel { EMAIL, SMS, PUSH }

interface NotificationChannel {
    void send(String recipient, String message);
}

class EmailChannel implements NotificationChannel {
    public void send(String recipient, String message) {
        System.out.println("📧 Email sent to " + recipient + ": " + message);
    }
}

class SMSChannel implements NotificationChannel {
    public void send(String recipient, String message) {
        System.out.println("📱 SMS sent to " + recipient + ": " + message);
    }
}

class PushChannel implements NotificationChannel {
    public void send(String recipient, String message) {
        System.out.println("🔔 Push notification to " + recipient + ": " + message);
    }
}

class Subscriber {
    String userId;
    Set<Channel> preferredChannels;
    
    public Subscriber(String userId, Set<Channel> channels) {
        this.userId = userId;
        this.preferredChannels = channels;
    }
}

public class NotificationService {
    private Map<String, List<Subscriber>> topicSubscribers;
    private Map<Channel, NotificationChannel> channels;
    
    public NotificationService() {
        topicSubscribers = new HashMap<>();
        channels = new HashMap<>();
        channels.put(Channel.EMAIL, new EmailChannel());
        channels.put(Channel.SMS, new SMSChannel());
        channels.put(Channel.PUSH, new PushChannel());
    }
    
    public void subscribe(String topic, Subscriber subscriber) {
        topicSubscribers.computeIfAbsent(topic, k -> new ArrayList<>()).add(subscriber);
        System.out.println("✓ " + subscriber.userId + " subscribed to " + topic);
    }
    
    public void unsubscribe(String topic, String userId) {
        List<Subscriber> subscribers = topicSubscribers.get(topic);
        if (subscribers != null) {
            subscribers.removeIf(s -> s.userId.equals(userId));
            System.out.println("✓ " + userId + " unsubscribed from " + topic);
        }
    }
    
    public void publish(String topic, String message) {
        List<Subscriber> subscribers = topicSubscribers.get(topic);
        if (subscribers == null || subscribers.isEmpty()) {
            System.out.println("No subscribers for topic: " + topic);
            return;
        }
        
        System.out.println("\n📢 Publishing to topic: " + topic);
        for (Subscriber subscriber : subscribers) {
            for (Channel channel : subscriber.preferredChannels) {
                NotificationChannel notifChannel = channels.get(channel);
                notifChannel.send(subscriber.userId, message);
            }
        }
    }
}

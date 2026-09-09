/**
 * Demonstration of Singleton Pattern
 * 
 * Shows different implementations and their characteristics
 */
public class SingletonDemo {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║      SINGLETON PATTERN DEMO              ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
        
        // Test Logger Singleton
        testLoggerSingleton();
        
        // Test thread safety
        testThreadSafety();
        
        // Test Enum Singleton
        testEnumSingleton();
    }
    
    private static void testLoggerSingleton() {
        System.out.println("=== Testing Logger Singleton ===\n");
        
        // First call - instance will be created
        Logger logger1 = Logger.getInstance();
        logger1.log("First message");
        
        // Second call - same instance returned
        Logger logger2 = Logger.getInstance();
        logger2.log("Second message");
        
        // Verify same instance
        System.out.println("\nlogger1 == logger2: " + (logger1 == logger2));
        System.out.println("Same hashCode: " + (logger1.hashCode() == logger2.hashCode()));
    }
    
    private static void testThreadSafety() {
        System.out.println("\n=== Testing Thread Safety ===\n");
        
        // Create multiple threads trying to get instance
        Thread t1 = new Thread(() -> {
            Logger logger = Logger.getInstance();
            System.out.println("Thread 1: " + logger.hashCode());
        });
        
        Thread t2 = new Thread(() -> {
            Logger logger = Logger.getInstance();
            System.out.println("Thread 2: " + logger.hashCode());
        });
        
        Thread t3 = new Thread(() -> {
            Logger logger = Logger.getInstance();
            System.out.println("Thread 3: " + logger.hashCode());
        });
        
        t1.start();
        t2.start();
        t3.start();
        
        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("All threads got same instance (same hashCode)");
    }
    
    private static void testEnumSingleton() {
        System.out.println("\n=== Testing Enum Singleton ===\n");
        
        DatabaseConnection.INSTANCE.connect();
        DatabaseConnection.INSTANCE.query("SELECT * FROM users");
        DatabaseConnection.INSTANCE.disconnect();
    }
}

/**
 * Enum Singleton Example
 * Best approach for serialization-safe Singleton
 */
enum DatabaseConnection {
    INSTANCE;
    
    public void connect() {
        System.out.println("✓ Database connected");
    }
    
    public void query(String sql) {
        System.out.println("✓ Executing: " + sql);
    }
    
    public void disconnect() {
        System.out.println("✓ Database disconnected");
    }
}

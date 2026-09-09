/**
 * Real-World Example: Logger using Bill Pugh Singleton
 * 
 * This is the BEST approach for Singleton in production:
 * - Thread-safe without synchronization
 * - Lazy initialization
 * - High performance
 */
public class Logger {
    
    // Private constructor prevents instantiation
    private Logger() {
        System.out.println("Logger instance created");
    }
    
    // Inner static class - loaded only when getInstance() is called
    private static class LoggerHolder {
        private static final Logger INSTANCE = new Logger();
    }
    
    // Global access point
    public static Logger getInstance() {
        return LoggerHolder.INSTANCE;
    }
    
    // Business methods
    public void log(String message) {
        System.out.println("[LOG] " + message);
    }
    
    public void error(String message) {
        System.err.println("[ERROR] " + message);
    }
    
    public void warn(String message) {
        System.out.println("[WARN] " + message);
    }
}

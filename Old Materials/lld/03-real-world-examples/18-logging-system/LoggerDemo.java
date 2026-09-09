/**
 * Logger Demo
 */
public class LoggerDemo {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║       LOGGING SYSTEM DEMO                ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
        
        Logger logger = Logger.getInstance();
        
        System.out.println("=== Test 1: Default log level (INFO) ===\n");
        logger.debug("This is a debug message");  // Won't show
        logger.info("This is an info message");
        logger.warn("This is a warning message");
        logger.error("This is an error message");
        
        System.out.println("\n=== Test 2: Change log level to DEBUG ===\n");
        logger.setLogLevel(LogLevel.DEBUG);
        logger.debug("Now debug messages show");
        logger.info("Info still shows");
        
        System.out.println("\n=== Test 3: Change log level to ERROR ===\n");
        logger.setLogLevel(LogLevel.ERROR);
        logger.debug("Debug won't show");
        logger.info("Info won't show");
        logger.warn("Warn won't show");
        logger.error("Only error shows");
        
        System.out.println("\n💡 Key Concepts:");
        System.out.println("   - Singleton pattern");
        System.out.println("   - Thread-safe logging");
        System.out.println("   - Log level filtering");
        System.out.println("   - Timestamp and thread info");
    }
}

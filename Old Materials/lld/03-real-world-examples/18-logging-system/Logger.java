import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Thread-safe Logger with Singleton pattern
 */
public class Logger {
    private static Logger instance;
    private LogLevel currentLevel;
    private final DateTimeFormatter formatter;
    
    private Logger() {
        this.currentLevel = LogLevel.INFO;
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }
    
    public static synchronized Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }
    
    public void setLogLevel(LogLevel level) {
        this.currentLevel = level;
    }
    
    public synchronized void debug(String message) {
        log(LogLevel.DEBUG, message);
    }
    
    public synchronized void info(String message) {
        log(LogLevel.INFO, message);
    }
    
    public synchronized void warn(String message) {
        log(LogLevel.WARN, message);
    }
    
    public synchronized void error(String message) {
        log(LogLevel.ERROR, message);
    }
    
    private void log(LogLevel level, String message) {
        if (level.getPriority() >= currentLevel.getPriority()) {
            String timestamp = LocalDateTime.now().format(formatter);
            String threadName = Thread.currentThread().getName();
            String logMessage = String.format("[%s] [%s] [%s] %s",
                timestamp, level, threadName, message);
            System.out.println(logMessage);
        }
    }
}

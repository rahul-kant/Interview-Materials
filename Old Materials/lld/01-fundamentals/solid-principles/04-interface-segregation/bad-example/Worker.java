/**
 * BAD EXAMPLE - Violates Interface Segregation Principle
 * 
 * Fat interface that forces all implementations to provide
 * all methods, even if they don't need them.
 * 
 * Problem: Not all workers eat, sleep, or get paid!
 * - Robots don't eat or sleep
 * - Volunteers don't get paid
 * - Interns might not get paid
 * 
 * This interface forces unnecessary implementations.
 */
public interface Worker {
    
    /**
     * Perform work
     */
    void work();
    
    /**
     * Take a meal break
     * Problem: Robots don't eat!
     */
    void eat();
    
    /**
     * Rest/sleep
     * Problem: Robots don't sleep!
     */
    void sleep();
    
    /**
     * Receive payment
     * Problem: Volunteers don't get paid!
     */
    void getPaid();
}

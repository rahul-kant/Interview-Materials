/**
 * GOOD EXAMPLE - Follows Liskov Substitution Principle
 * 
 * Shape interface - the abstraction
 * Both Rectangle and Square implement this independently
 * No inheritance relationship between Rectangle and Square!
 * 
 * This follows LSP because any Shape implementation can
 * substitute for the Shape interface without issues.
 */
public interface Shape {
    
    /**
     * Calculate and return the area of the shape
     * @return area of the shape
     */
    int getArea();
    
    /**
     * Get the perimeter of the shape
     * @return perimeter of the shape
     */
    int getPerimeter();
    
    /**
     * Display information about the shape
     */
    void display();
}

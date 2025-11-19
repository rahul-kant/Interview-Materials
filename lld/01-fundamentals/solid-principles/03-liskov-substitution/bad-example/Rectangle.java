/**
 * BAD EXAMPLE - Violates Liskov Substitution Principle
 * 
 * Rectangle class that will be extended by Square
 * This seems logical (mathematically, square IS-A rectangle)
 * But it violates LSP!
 */
public class Rectangle {
    protected int width;
    protected int height;
    
    public Rectangle(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public int getArea() {
        return width * height;
    }
    
    public void display() {
        System.out.println("Rectangle: " + width + "x" + height + ", Area: " + getArea());
    }
}

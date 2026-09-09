/**
 * BAD EXAMPLE - Violates Liskov Substitution Principle
 * 
 * Square extends Rectangle
 * Mathematically correct (square IS-A rectangle)
 * But breaks LSP because Square changes Rectangle's behavior!
 * 
 * Problem: Square must maintain equal sides, so setWidth and setHeight
 * must change BOTH dimensions. This violates the contract expected
 * from Rectangle where these methods should be independent.
 */
public class Square extends Rectangle {
    
    public Square(int side) {
        super(side, side);
    }
    
    /**
     * PROBLEM: Setting width also sets height!
     * This changes the expected behavior from Rectangle
     * Violates LSP - Square cannot substitute Rectangle
     */
    @Override
    public void setWidth(int width) {
        this.width = width;
        this.height = width; // Side effect! Must keep square property
    }
    
    /**
     * PROBLEM: Setting height also sets width!
     * This changes the expected behavior from Rectangle
     * Violates LSP - Square cannot substitute Rectangle
     */
    @Override
    public void setHeight(int height) {
        this.width = height; // Side effect! Must keep square property
        this.height = height;
    }
    
    @Override
    public void display() {
        System.out.println("Square: " + width + "x" + width + ", Area: " + getArea());
    }
}

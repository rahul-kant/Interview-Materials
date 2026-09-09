/**
 * Demo showing Liskov Substitution Principle Violation
 * 
 * This demonstrates the classic Rectangle-Square problem.
 * Even though mathematically "square IS-A rectangle", 
 * in code Square cannot substitute Rectangle without breaking behavior.
 */
public class LSPDemo {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  LSP VIOLATION - Rectangle/Square");
        System.out.println("========================================\n");
        
        // Test 1: Using Rectangle (works as expected)
        System.out.println("TEST 1: Using Rectangle");
        System.out.println("------------------------");
        Rectangle rectangle = new Rectangle(5, 10);
        testRectangle(rectangle);
        
        // Test 2: Using Square as Rectangle (BREAKS!)
        System.out.println("\n\nTEST 2: Using Square as Rectangle");
        System.out.println("----------------------------------");
        Rectangle square = new Square(5); // Square used as Rectangle
        testRectangle(square); // This will fail!
        
        // Analysis
        System.out.println("\n\n========================================");
        System.out.println("           PROBLEM ANALYSIS");
        System.out.println("========================================\n");
        
        System.out.println("THE LSP VIOLATION:");
        System.out.println("------------------");
        System.out.println("1. Rectangle expects width and height to be independent");
        System.out.println("2. Square changes both when either is set");
        System.out.println("3. This breaks the contract expected from Rectangle");
        System.out.println("4. Square CANNOT substitute Rectangle safely");
        
        System.out.println("\n\nWHY IT BREAKS:");
        System.out.println("--------------");
        System.out.println("• Rectangle.setWidth(5) sets only width");
        System.out.println("• Rectangle.setHeight(10) sets only height");
        System.out.println("• Expected area: 5 * 10 = 50");
        System.out.println("");
        System.out.println("• Square.setWidth(5) sets BOTH width and height to 5");
        System.out.println("• Square.setHeight(10) sets BOTH width and height to 10");
        System.out.println("• Actual area: 10 * 10 = 100 (NOT 50!)");
        
        System.out.println("\n\nCONSEQUENCES:");
        System.out.println("-------------");
        System.out.println("❌ Code that works with Rectangle breaks with Square");
        System.out.println("❌ Need instanceof checks (defeats polymorphism)");
        System.out.println("❌ Violates Liskov Substitution Principle");
        System.out.println("❌ Fragile, unpredictable code");
        
        System.out.println("\n\nTHE LESSON:");
        System.out.println("-----------");
        System.out.println("Just because \"Square IS-A Rectangle\" mathematically");
        System.out.println("doesn't mean Square should inherit from Rectangle!");
        System.out.println("");
        System.out.println("LSP requires behavioral substitutability,");
        System.out.println("not just conceptual IS-A relationship.");
        
        System.out.println("\n\n========================================");
        System.out.println("  Check good-example/ for the solution!");
        System.out.println("========================================\n");
    }
    
    /**
     * This method expects Rectangle behavior:
     * - setWidth() changes only width
     * - setHeight() changes only height
     * - area = width * height
     * 
     * Works fine with Rectangle, BREAKS with Square!
     */
    private static void testRectangle(Rectangle r) {
        System.out.println("Initial state:");
        r.display();
        
        System.out.println("\nSetting width to 5...");
        r.setWidth(5);
        System.out.println("After setWidth(5):");
        r.display();
        
        System.out.println("\nSetting height to 10...");
        r.setHeight(10);
        System.out.println("After setHeight(10):");
        r.display();
        
        int expectedArea = 5 * 10; // We expect 50
        int actualArea = r.getArea();
        
        System.out.println("\n>>> Expected area: " + expectedArea);
        System.out.println(">>> Actual area:   " + actualArea);
        
        if (expectedArea == actualArea) {
            System.out.println("✓ PASS: Area is as expected!");
        } else {
            System.out.println("✗ FAIL: Area is NOT as expected!");
            System.out.println("✗ This breaks the Rectangle contract!");
            System.out.println("✗ LSP VIOLATION!");
        }
    }
}

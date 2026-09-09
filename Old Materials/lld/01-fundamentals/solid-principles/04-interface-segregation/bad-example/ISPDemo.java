/**
 * Demo showing Interface Segregation Principle Violation
 * 
 * This demonstrates the "fat interface" problem.
 * Worker interface forces all implementations to provide
 * methods they don't need.
 */
public class ISPDemo {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  ISP VIOLATION - Fat Interface");
        System.out.println("========================================\n");
        
        // Test with HumanWorker (works fine)
        System.out.println("TEST 1: HumanWorker");
        System.out.println("--------------------");
        Worker human = new HumanWorker("John");
        manageWorker(human);
        
        // Test with RobotWorker (problems!)
        System.out.println("\n\nTEST 2: RobotWorker");
        System.out.println("-------------------");
        Worker robot = new RobotWorker("R2D2");
        manageWorker(robot);
        
        // Analysis
        System.out.println("\n\n========================================");
        System.out.println("           PROBLEM ANALYSIS");
        System.out.println("========================================\n");
        
        System.out.println("THE ISP VIOLATION:");
        System.out.println("------------------");
        System.out.println("1. Worker interface has 4 methods");
        System.out.println("2. RobotWorker only needs work()");
        System.out.println("3. Forced to implement eat(), sleep(), getPaid()");
        System.out.println("4. Results in empty methods or exceptions");
        
        System.out.println("\n\nWHY IT'S BAD:");
        System.out.println("-------------");
        System.out.println("❌ Unnecessary dependencies");
        System.out.println("❌ Empty method implementations");
        System.out.println("❌ Confusing interface contract");
        System.out.println("❌ Hard to test");
        System.out.println("❌ Violates ISP!");
        
        System.out.println("\n\nCONSEQUENCES:");
        System.out.println("-------------");
        System.out.println("• RobotWorker depends on methods it doesn't use");
        System.out.println("• Changes to eat/sleep/getPaid affect robots unnecessarily");
        System.out.println("• Mocking Worker for tests requires mocking unused methods");
        System.out.println("• Interface bloat gets worse as more methods are added");
        
        System.out.println("\n\nTHE LESSON:");
        System.out.println("-----------");
        System.out.println("Don't create fat interfaces that force");
        System.out.println("implementations to provide unused methods!");
        System.out.println("");
        System.out.println("Split into focused, role-based interfaces:");
        System.out.println("• Workable - for work()");
        System.out.println("• Eatable - for eat()");
        System.out.println("• Sleepable - for sleep()");
        System.out.println("• Payable - for getPaid()");
        
        System.out.println("\n\n========================================");
        System.out.println("  Check good-example/ for the solution!");
        System.out.println("========================================\n");
    }
    
    /**
     * Manages a worker through daily routine
     * Shows how RobotWorker's empty methods are problematic
     */
    private static void manageWorker(Worker worker) {
        worker.work();
        worker.eat();    // Robot doesn't need this!
        worker.sleep();  // Robot doesn't need this!
        worker.getPaid(); // Robot doesn't need this!
    }
}

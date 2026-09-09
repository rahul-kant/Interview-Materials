/**
 * Demo showing Interface Segregation Principle SOLUTION
 * 
 * This demonstrates how splitting interfaces solves the problem.
 * Each class implements only the interfaces it needs!
 */
public class ISPDemo {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  ISP SOLUTION - Segregated Interfaces");
        System.out.println("========================================\n");
        
        // Create workers
        HumanWorker human = new HumanWorker("John");
        RobotWorker robot = new RobotWorker("R2D2");
        
        // Test HumanWorker
        System.out.println("TEST 1: HumanWorker (implements all interfaces)");
        System.out.println("------------------------------------------------");
        manageHuman(human);
        
        // Test RobotWorker
        System.out.println("\n\nTEST 2: RobotWorker (implements only Workable)");
        System.out.println("-----------------------------------------------");
        manageRobot(robot);
        
        // Benefits
        System.out.println("\n\n========================================");
        System.out.println("           BENEFITS OF ISP");
        System.out.println("========================================\n");
        
        System.out.println("✅ SEGREGATED INTERFACES:");
        System.out.println("   • Workable - for work()");
        System.out.println("   • Eatable - for eat()");
        System.out.println("   • Sleepable - for sleep()");
        System.out.println("   • Payable - for getPaid()");
        
        System.out.println("\n✅ FLEXIBLE IMPLEMENTATION:");
        System.out.println("   • HumanWorker: Workable, Eatable, Sleepable, Payable");
        System.out.println("   • RobotWorker: Workable only");
        System.out.println("   • Each implements only what it needs!");
        
        System.out.println("\n✅ NO FORCED METHODS:");
        System.out.println("   • Robot doesn't have eat(), sleep(), getPaid()");
        System.out.println("   • No empty implementations");
        System.out.println("   • No UnsupportedOperationException");
        System.out.println("   • Clean, focused classes");
        
        System.out.println("\n✅ EASY TO EXTEND:");
        System.out.println("   • Want volunteer? Workable only");
        System.out.println("   • Want contractor? Workable + Payable");
        System.out.println("   • Want intern? Workable + Eatable + Sleepable");
        System.out.println("   • Mix and match interfaces!");
        
        System.out.println("\n✅ BETTER TESTING:");
        System.out.println("   • Mock only needed interfaces");
        System.out.println("   • Simpler test setup");
        System.out.println("   • Focused test cases");
        
        System.out.println("\n✅ LOW COUPLING:");
        System.out.println("   • Classes don't depend on unused methods");
        System.out.println("   • Changes don't ripple unnecessarily");
        System.out.println("   • More maintainable code");
        
        System.out.println("\n\n========================================");
        System.out.println("  ISP COMPARISON");
        System.out.println("========================================");
        
        System.out.println("\nBad (Fat Interface):");
        System.out.println("  • One Worker interface with 4 methods");
        System.out.println("  • All classes implement all methods");
        System.out.println("  • Robots have empty eat/sleep/getPaid");
        System.out.println("  • Violates ISP");
        
        System.out.println("\nGood (Segregated Interfaces):");
        System.out.println("  • Four focused interfaces");
        System.out.println("  • Classes implement only what they need");
        System.out.println("  • No empty or forced methods");
        System.out.println("  • Follows ISP!");
        
        System.out.println("\n\n========================================");
        System.out.println("  KEY LESSON");
        System.out.println("========================================");
        System.out.println("\n\"Clients should not be forced to depend");
        System.out.println(" on interfaces they do not use.\"");
        System.out.println("\nCreate small, focused interfaces!");
        System.out.println("Let classes pick what they need!");
        System.out.println("\n========================================\n");
    }
    
    /**
     * Manage human worker - uses all capabilities
     */
    private static void manageHuman(HumanWorker human) {
        // Human can do everything
        human.work();
        human.eat();
        human.sleep();
        human.getPaid();
    }
    
    /**
     * Manage robot worker - uses only work capability
     * Notice: We can only call methods from Workable interface
     */
    private static void manageRobot(RobotWorker robot) {
        // Robot can only work (and that's perfect!)
        robot.work();
        // Cannot call robot.eat() - method doesn't exist!
        // Cannot call robot.sleep() - method doesn't exist!
        // Cannot call robot.getPaid() - method doesn't exist!
        System.out.println("(Robot only has work() method - clean!)");
    }
}

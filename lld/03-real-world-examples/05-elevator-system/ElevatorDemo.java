/**
 * ElevatorDemo - Demonstrates Elevator System functionality
 * 
 * Interview Key Points:
 * - Multiple elevator coordination
 * - Different request patterns
 * - Optimal elevator selection
 * - Concurrent request handling
 */
public class ElevatorDemo {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║       ELEVATOR SYSTEM DEMO             ║");
        System.out.println("╚════════════════════════════════════════╝");
        
        // Initialize controller with 3 elevators and 10 floors
        ElevatorController controller = ElevatorController.getInstance(3, 10);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("SCENARIO 1: Basic Requests");
        System.out.println("=".repeat(50));
        testBasicRequests(controller);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("SCENARIO 2: Rush Hour (Multiple Requests)");
        System.out.println("=".repeat(50));
        testRushHour(controller);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("SCENARIO 3: Long Distance Travel");
        System.out.println("=".repeat(50));
        testLongDistance(controller);
    }
    
    /**
     * Scenario 1: Basic elevator requests
     */
    private static void testBasicRequests(ElevatorController controller) {
        // Person at floor 0 wants to go up
        controller.requestElevator(0, Direction.UP);
        
        // Once in elevator, select floor 5
        controller.addDestination(1, 5);
        
        // Display status
        controller.displayStatus();
        
        // Run simulation
        controller.simulate();
        
        controller.displayStatus();
    }
    
    /**
     * Scenario 2: Multiple requests at once (rush hour)
     */
    private static void testRushHour(ElevatorController controller) {
        // Multiple people at ground floor going up
        controller.requestElevator(0, Direction.UP);
        controller.requestElevator(0, Direction.UP);
        controller.requestElevator(0, Direction.UP);
        
        // Someone on floor 8 going down
        controller.requestElevator(8, Direction.DOWN);
        
        // Someone on floor 5 going up
        controller.requestElevator(5, Direction.UP);
        
        // Add destinations for first 3 elevators
        controller.addDestination(1, 3);
        controller.addDestination(1, 7);
        controller.addDestination(2, 4);
        controller.addDestination(2, 9);
        controller.addDestination(3, 2);
        controller.addDestination(3, 6);
        
        controller.displayStatus();
        
        // Run simulation
        controller.simulate();
        
        controller.displayStatus();
    }
    
    /**
     * Scenario 3: Long distance travel
     */
    private static void testLongDistance(ElevatorController controller) {
        // Someone at ground floor going to top
        controller.requestElevator(0, Direction.UP);
        controller.addDestination(1, 10);
        
        // Someone at top floor going to ground
        controller.requestElevator(10, Direction.DOWN);
        controller.addDestination(2, 0);
        
        // Someone in middle going up with stops
        controller.requestElevator(5, Direction.UP);
        controller.addDestination(3, 6);
        controller.addDestination(3, 7);
        controller.addDestination(3, 8);
        
        controller.displayStatus();
        
        // Run simulation
        controller.simulate();
        
        controller.displayStatus();
        
        System.out.println("\n✅ Demo completed successfully!");
        System.out.println("\n📊 Key Design Points Demonstrated:");
        System.out.println("  ✓ Optimal elevator selection");
        System.out.println("  ✓ Direction-based scheduling");
        System.out.println("  ✓ Multiple concurrent requests");
        System.out.println("  ✓ Efficient floor traversal");
        System.out.println("  ✓ State management");
    }
}

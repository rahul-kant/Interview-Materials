import java.util.*;
import java.util.concurrent.*;

/**
 * ElevatorController - Manages multiple elevators (Singleton)
 * 
 * Interview Key Points:
 * - Singleton pattern
 * - Optimal elevator selection algorithm
 * - Request queue management
 * - Thread-safe operations
 * - Multiple scheduling strategies
 */
public class ElevatorController {
    private static ElevatorController instance;
    
    private final List<Elevator> elevators;
    private final Queue<Request> pendingRequests;
    private final int numberOfFloors;
    private final SchedulingStrategy strategy;
    
    public enum SchedulingStrategy {
        NEAREST_CAR,      // Select nearest available elevator
        SCAN,             // SCAN algorithm (like disk scheduling)
        LOOK              // LOOK algorithm (improved SCAN)
    }
    
    private ElevatorController(int numberOfElevators, int numberOfFloors) {
        this(numberOfElevators, numberOfFloors, SchedulingStrategy.NEAREST_CAR);
    }
    
    private ElevatorController(int numberOfElevators, int numberOfFloors, 
                              SchedulingStrategy strategy) {
        this.numberOfFloors = numberOfFloors;
        this.strategy = strategy;
        this.elevators = new ArrayList<>();
        this.pendingRequests = new ConcurrentLinkedQueue<>();
        
        // Initialize elevators at ground floor
        for (int i = 0; i < numberOfElevators; i++) {
            elevators.add(new Elevator(i + 1, 0));
        }
        
        System.out.println("✓ Elevator Controller initialized:");
        System.out.println("  - Elevators: " + numberOfElevators);
        System.out.println("  - Floors: 0 to " + numberOfFloors);
        System.out.println("  - Strategy: " + strategy);
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized ElevatorController getInstance(int numberOfElevators, 
                                                             int numberOfFloors) {
        if (instance == null) {
            instance = new ElevatorController(numberOfElevators, numberOfFloors);
        }
        return instance;
    }
    
    public static ElevatorController getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Controller not initialized");
        }
        return instance;
    }
    
    /**
     * Request elevator from a floor
     */
    public synchronized void requestElevator(int floor, Direction direction) {
        if (floor < 0 || floor > numberOfFloors) {
            System.out.println("❌ Invalid floor: " + floor);
            return;
        }
        
        System.out.println("\n📞 External request: Floor " + floor + " going " + direction);
        
        // Find best elevator for this request
        Elevator bestElevator = selectBestElevator(floor, direction);
        
        if (bestElevator != null) {
            System.out.println("✓ Assigned Elevator " + bestElevator.getId() + 
                             " (currently at floor " + bestElevator.getCurrentFloor() + ")");
            bestElevator.addDestination(floor);
        } else {
            System.out.println("⏳ All elevators busy, request queued");
            Request request = new Request(floor, floor, Request.RequestType.EXTERNAL);
            pendingRequests.offer(request);
        }
    }
    
    /**
     * Add destination inside elevator
     */
    public void addDestination(int elevatorId, int destinationFloor) {
        if (destinationFloor < 0 || destinationFloor > numberOfFloors) {
            System.out.println("❌ Invalid floor: " + destinationFloor);
            return;
        }
        
        Elevator elevator = getElevator(elevatorId);
        if (elevator != null) {
            System.out.println("\n🔘 Internal request: Elevator " + elevatorId + 
                             " to floor " + destinationFloor);
            elevator.addDestination(destinationFloor);
        }
    }
    
    /**
     * Select best elevator based on strategy
     */
    private Elevator selectBestElevator(int floor, Direction direction) {
        switch (strategy) {
            case NEAREST_CAR:
                return selectNearestElevator(floor);
            case SCAN:
            case LOOK:
                return selectByScanAlgorithm(floor, direction);
            default:
                return selectNearestElevator(floor);
        }
    }
    
    /**
     * Nearest Car strategy - select closest available elevator
     */
    private Elevator selectNearestElevator(int floor) {
        Elevator nearest = null;
        int minDistance = Integer.MAX_VALUE;
        
        for (Elevator elevator : elevators) {
            int distance = elevator.distanceToFloor(floor);
            
            // Prefer idle elevators
            if (elevator.isIdle()) {
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = elevator;
                }
            } else if (nearest == null || !nearest.isIdle()) {
                // If no idle elevator, consider moving ones
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = elevator;
                }
            }
        }
        
        return nearest;
    }
    
    /**
     * SCAN/LOOK strategy - select elevator moving towards the floor
     */
    private Elevator selectByScanAlgorithm(int floor, Direction requestDirection) {
        Elevator best = null;
        int minDistance = Integer.MAX_VALUE;
        
        for (Elevator elevator : elevators) {
            // Check if elevator can serve this request
            Request request = new Request(floor, floor, Request.RequestType.EXTERNAL);
            
            if (elevator.canServeRequest(request)) {
                int distance = elevator.distanceToFloor(floor);
                if (distance < minDistance) {
                    minDistance = distance;
                    best = elevator;
                }
            }
        }
        
        // If no suitable elevator, fall back to nearest
        if (best == null) {
            best = selectNearestElevator(floor);
        }
        
        return best;
    }
    
    /**
     * Move all elevators one step
     */
    public void step() {
        for (Elevator elevator : elevators) {
            if (elevator.hasDestinations()) {
                elevator.move();
            }
        }
        
        // Process pending requests
        processPendingRequests();
    }
    
    /**
     * Process pending requests
     */
    private void processPendingRequests() {
        if (pendingRequests.isEmpty()) {
            return;
        }
        
        Iterator<Request> iterator = pendingRequests.iterator();
        while (iterator.hasNext()) {
            Request request = iterator.next();
            Elevator elevator = selectBestElevator(
                request.getSourceFloor(), 
                request.getDirection()
            );
            
            if (elevator != null && elevator.isIdle()) {
                elevator.addDestination(request.getSourceFloor());
                iterator.remove();
                System.out.println("✓ Processed pending request: " + request);
            }
        }
    }
    
    /**
     * Get elevator by ID
     */
    private Elevator getElevator(int elevatorId) {
        for (Elevator elevator : elevators) {
            if (elevator.getId() == elevatorId) {
                return elevator;
            }
        }
        System.out.println("❌ Elevator " + elevatorId + " not found");
        return null;
    }
    
    /**
     * Get all elevators status
     */
    public void displayStatus() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ELEVATOR SYSTEM STATUS");
        System.out.println("=".repeat(60));
        
        for (Elevator elevator : elevators) {
            System.out.println(elevator);
        }
        
        if (!pendingRequests.isEmpty()) {
            System.out.println("\nPending Requests: " + pendingRequests.size());
        }
        
        System.out.println("=".repeat(60) + "\n");
    }
    
    /**
     * Check if all elevators are idle
     */
    public boolean allIdle() {
        for (Elevator elevator : elevators) {
            if (!elevator.isIdle()) {
                return false;
            }
        }
        return pendingRequests.isEmpty();
    }
    
    /**
     * Run simulation
     */
    public void simulate() {
        int maxSteps = 100;
        int step = 0;
        
        while (!allIdle() && step < maxSteps) {
            step();
            
            try {
                Thread.sleep(500); // Simulate time between floors
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            
            step++;
        }
        
        if (allIdle()) {
            System.out.println("\n✅ All elevators are now idle");
        }
    }
}

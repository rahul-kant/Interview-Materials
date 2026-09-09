import java.util.*;

/**
 * Elevator Class - Represents single elevator car
 * 
 * Interview Key Points:
 * - State management (current floor, direction, door status)
 * - Request queue management
 * - Movement simulation
 * - Capacity limits
 */
public class Elevator {
    private final int id;
    private int currentFloor;
    private Direction currentDirection;
    private boolean isDoorOpen;
    private final int capacity;
    private int currentLoad;
    private final Set<Integer> destinationFloors; // Internal requests
    
    private static final int DEFAULT_CAPACITY = 10;
    
    public Elevator(int id, int startFloor) {
        this(id, startFloor, DEFAULT_CAPACITY);
    }
    
    public Elevator(int id, int startFloor, int capacity) {
        this.id = id;
        this.currentFloor = startFloor;
        this.currentDirection = Direction.IDLE;
        this.isDoorOpen = false;
        this.capacity = capacity;
        this.currentLoad = 0;
        this.destinationFloors = new TreeSet<>();
    }
    
    /**
     * Add internal destination (button pressed inside elevator)
     */
    public synchronized void addDestination(int floor) {
        if (floor < 0) {
            System.out.println("❌ Invalid floor: " + floor);
            return;
        }
        
        if (floor == currentFloor) {
            System.out.println("Already at floor " + floor);
            return;
        }
        
        destinationFloors.add(floor);
        System.out.println("✓ Elevator " + id + ": Added destination floor " + floor);
        
        // Update direction if idle
        if (currentDirection == Direction.IDLE) {
            currentDirection = floor > currentFloor ? Direction.UP : Direction.DOWN;
        }
    }
    
    /**
     * Move elevator one floor
     */
    public synchronized void move() {
        if (currentDirection == Direction.IDLE) {
            return;
        }
        
        // Move one floor in current direction
        if (currentDirection == Direction.UP) {
            currentFloor++;
            System.out.println("🛗 Elevator " + id + " moving UP to floor " + currentFloor);
        } else if (currentDirection == Direction.DOWN) {
            currentFloor--;
            System.out.println("🛗 Elevator " + id + " moving DOWN to floor " + currentFloor);
        }
        
        // Check if we've reached a destination
        if (destinationFloors.contains(currentFloor)) {
            arriveAtFloor();
        }
    }
    
    /**
     * Arrive at floor and handle stop
     */
    private void arriveAtFloor() {
        System.out.println("🔔 Elevator " + id + " arrived at floor " + currentFloor);
        openDoors();
        destinationFloors.remove(currentFloor);
        
        // Simulate door open time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        closeDoors();
        
        // Update direction if no more destinations
        if (destinationFloors.isEmpty()) {
            currentDirection = Direction.IDLE;
            System.out.println("⊙ Elevator " + id + " is now IDLE");
        } else {
            updateDirection();
        }
    }
    
    /**
     * Update direction based on remaining destinations
     */
    private void updateDirection() {
        if (destinationFloors.isEmpty()) {
            currentDirection = Direction.IDLE;
            return;
        }
        
        // Find next destination
        Integer nextFloor = null;
        
        if (currentDirection == Direction.UP) {
            // Find next floor above current
            for (int floor : destinationFloors) {
                if (floor > currentFloor) {
                    nextFloor = floor;
                    break;
                }
            }
            // If no floors above, reverse direction
            if (nextFloor == null) {
                currentDirection = Direction.DOWN;
            }
        } else if (currentDirection == Direction.DOWN) {
            // Find next floor below current
            Integer[] floors = destinationFloors.toArray(new Integer[0]);
            for (int i = floors.length - 1; i >= 0; i--) {
                if (floors[i] < currentFloor) {
                    nextFloor = floors[i];
                    break;
                }
            }
            // If no floors below, reverse direction
            if (nextFloor == null) {
                currentDirection = Direction.UP;
            }
        }
    }
    
    /**
     * Open doors
     */
    public void openDoors() {
        isDoorOpen = true;
        System.out.println("🚪 Elevator " + id + " doors OPEN");
    }
    
    /**
     * Close doors
     */
    public void closeDoors() {
        isDoorOpen = false;
        System.out.println("🚪 Elevator " + id + " doors CLOSED");
    }
    
    /**
     * Add passenger
     */
    public boolean addPassenger() {
        if (currentLoad >= capacity) {
            System.out.println("❌ Elevator " + id + " is at full capacity");
            return false;
        }
        currentLoad++;
        return true;
    }
    
    /**
     * Remove passenger
     */
    public void removePassenger() {
        if (currentLoad > 0) {
            currentLoad--;
        }
    }
    
    // Getters
    
    public int getId() {
        return id;
    }
    
    public int getCurrentFloor() {
        return currentFloor;
    }
    
    public Direction getCurrentDirection() {
        return currentDirection;
    }
    
    public boolean isDoorOpen() {
        return isDoorOpen;
    }
    
    public boolean hasDestinations() {
        return !destinationFloors.isEmpty();
    }
    
    public Set<Integer> getDestinationFloors() {
        return new HashSet<>(destinationFloors);
    }
    
    public boolean isIdle() {
        return currentDirection == Direction.IDLE && destinationFloors.isEmpty();
    }
    
    public int getCurrentLoad() {
        return currentLoad;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * Calculate distance to floor
     */
    public int distanceToFloor(int floor) {
        return Math.abs(currentFloor - floor);
    }
    
    /**
     * Check if elevator can serve request
     */
    public boolean canServeRequest(Request request) {
        int sourceFloor = request.getSourceFloor();
        Direction requestDirection = request.getDirection();
        
        // If idle, can serve any request
        if (currentDirection == Direction.IDLE) {
            return true;
        }
        
        // If moving in same direction and request is on the way
        if (currentDirection == requestDirection) {
            if (currentDirection == Direction.UP && sourceFloor >= currentFloor) {
                return true;
            }
            if (currentDirection == Direction.DOWN && sourceFloor <= currentFloor) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        return String.format("Elevator %d: Floor %d, %s, Load: %d/%d, Destinations: %s", 
            id, currentFloor, currentDirection, currentLoad, capacity, destinationFloors);
    }
}

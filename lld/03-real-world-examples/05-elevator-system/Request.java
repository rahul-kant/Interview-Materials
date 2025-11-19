/**
 * Request Class - Represents elevator request
 * 
 * Interview Key Points:
 * - Can be internal (from inside elevator) or external (from floor)
 * - Stores source and destination floor
 * - Immutable design
 */
public class Request {
    private final int sourceFloor;
    private final int destinationFloor;
    private final Direction direction;
    private final RequestType requestType;
    
    public enum RequestType {
        INTERNAL,  // Request from inside elevator
        EXTERNAL   // Request from floor button
    }
    
    public Request(int sourceFloor, int destinationFloor, RequestType requestType) {
        this.sourceFloor = sourceFloor;
        this.destinationFloor = destinationFloor;
        this.requestType = requestType;
        this.direction = calculateDirection();
    }
    
    private Direction calculateDirection() {
        if (destinationFloor > sourceFloor) {
            return Direction.UP;
        } else if (destinationFloor < sourceFloor) {
            return Direction.DOWN;
        } else {
            return Direction.IDLE;
        }
    }
    
    public int getSourceFloor() {
        return sourceFloor;
    }
    
    public int getDestinationFloor() {
        return destinationFloor;
    }
    
    public Direction getDirection() {
        return direction;
    }
    
    public RequestType getRequestType() {
        return requestType;
    }
    
    @Override
    public String toString() {
        return String.format("%s Request: Floor %d → Floor %d (%s)", 
            requestType, sourceFloor, destinationFloor, direction);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Request)) return false;
        Request other = (Request) obj;
        return this.sourceFloor == other.sourceFloor && 
               this.destinationFloor == other.destinationFloor;
    }
    
    @Override
    public int hashCode() {
        return sourceFloor * 31 + destinationFloor;
    }
}

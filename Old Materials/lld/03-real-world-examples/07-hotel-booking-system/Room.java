import java.time.LocalDate;
import java.util.*;

/**
 * Room class represents a physical room in a hotel
 * 
 * EXPLANATION:
 * - Each room has a unique number and type
 * - Rooms track their bookings to determine availability
 * - Thread-safe operations for concurrent booking scenarios
 * 
 * DESIGN PATTERNS:
 * - Uses Strategy pattern for pricing (through RoomType)
 * - Follows SRP - Room manages its own availability
 */
public class Room {
    private String roomNumber;
    private RoomType type;
    private int capacity;
    private List<String> amenities;  // WiFi, TV, AC, etc.
    private Map<LocalDate, Boolean> availabilityMap;  // Date -> isAvailable
    
    /**
     * Constructor
     * 
     * @param roomNumber Unique identifier for the room
     * @param type Type of room (STANDARD, DELUXE, SUITE, etc.)
     * @param capacity Maximum number of guests
     */
    public Room(String roomNumber, RoomType type, int capacity) {
        this.roomNumber = roomNumber;
        this.type = type;
        this.capacity = capacity;
        this.amenities = new ArrayList<>();
        this.availabilityMap = new HashMap<>();
    }
    
    /**
     * Check if room is available for given date range
     * 
     * EXPLANATION:
     * - Checks each date in the range
     * - Returns false if any date is booked
     * - Thread-safe with synchronized block
     */
    public synchronized boolean isAvailable(LocalDate checkIn, LocalDate checkOut) {
        LocalDate current = checkIn;
        
        while (!current.isAfter(checkOut)) {
            // If date is marked as unavailable, room can't be booked
            if (availabilityMap.getOrDefault(current, true) == false) {
                return false;
            }
            current = current.plusDays(1);
        }
        
        return true;
    }
    
    /**
     * Mark room as booked for date range
     * 
     * CONCURRENCY HANDLING:
     * - synchronized method prevents double booking
     * - Atomic operation - either all dates booked or none
     */
    public synchronized boolean book(LocalDate checkIn, LocalDate checkOut) {
        // First check if available
        if (!isAvailable(checkIn, checkOut)) {
            return false;
        }
        
        // Mark all dates as unavailable
        LocalDate current = checkIn;
        while (!current.isAfter(checkOut)) {
            availabilityMap.put(current, false);
            current = current.plusDays(1);
        }
        
        return true;
    }
    
    /**
     * Cancel booking and free up dates
     */
    public synchronized void cancelBooking(LocalDate checkIn, LocalDate checkOut) {
        LocalDate current = checkIn;
        while (!current.isAfter(checkOut)) {
            availabilityMap.put(current, true);
            current = current.plusDays(1);
        }
    }
    
    /**
     * Calculate price for stay duration
     * 
     * EXPLANATION:
     * - Base price comes from room type
     * - Can be extended with dynamic pricing strategies
     */
    public double calculatePrice(LocalDate checkIn, LocalDate checkOut) {
        long nights = checkOut.toEpochDay() - checkIn.toEpochDay();
        return nights * type.getBasePrice();
    }
    
    // Getters and setters
    public String getRoomNumber() { return roomNumber; }
    public RoomType getType() { return type; }
    public int getCapacity() { return capacity; }
    public List<String> getAmenities() { return amenities; }
    
    public void addAmenity(String amenity) {
        amenities.add(amenity);
    }
    
    @Override
    public String toString() {
        return "Room " + roomNumber + " (" + type + ", Capacity: " + capacity + ")";
    }
}

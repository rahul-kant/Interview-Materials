import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Hotel class represents a hotel with multiple rooms
 * 
 * EXPLANATION:
 * - A hotel has a collection of rooms
 * - Provides search functionality by room type and dates
 * - Manages room inventory
 * 
 * DESIGN PATTERNS:
 * - Facade: Provides simplified interface for complex room operations
 * - SRP: Hotel manages its rooms, delegates booking to Room class
 */
public class Hotel {
    private String hotelId;
    private String name;
    private String location;
    private String address;
    private int starRating;  // 1-5 stars
    private List<Room> rooms;
    private List<String> amenities;  // Pool, Gym, WiFi, etc.
    
    /**
     * Constructor
     * 
     * @param hotelId Unique identifier
     * @param name Hotel name
     * @param location City/area
     * @param address Full address
     * @param starRating Star rating (1-5)
     */
    public Hotel(String hotelId, String name, String location, String address, int starRating) {
        this.hotelId = hotelId;
        this.name = name;
        this.location = location;
        this.address = address;
        this.starRating = starRating;
        this.rooms = new ArrayList<>();
        this.amenities = new ArrayList<>();
    }
    
    /**
     * Add a room to the hotel
     */
    public void addRoom(Room room) {
        rooms.add(room);
    }
    
    /**
     * Search available rooms by type and dates
     * 
     * EXPLANATION:
     * - Filters rooms by type
     * - Checks availability for date range
     * - Returns list of available rooms
     * 
     * INTERVIEW POINT: How to optimize this search?
     * - Index rooms by type
     * - Cache availability for common date ranges
     * - Use database queries for large hotels
     */
    public List<Room> searchAvailableRooms(RoomType type, LocalDate checkIn, LocalDate checkOut) {
        return rooms.stream()
                .filter(room -> room.getType() == type)
                .filter(room -> room.isAvailable(checkIn, checkOut))
                .collect(Collectors.toList());
    }
    
    /**
     * Get all available rooms for date range (any type)
     */
    public List<Room> getAllAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        return rooms.stream()
                .filter(room -> room.isAvailable(checkIn, checkOut))
                .collect(Collectors.toList());
    }
    
    /**
     * Get room by room number
     */
    public Room getRoomByNumber(String roomNumber) {
        return rooms.stream()
                .filter(room -> room.getRoomNumber().equals(roomNumber))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get total number of rooms by type
     */
    public int getRoomCountByType(RoomType type) {
        return (int) rooms.stream()
                .filter(room -> room.getType() == type)
                .count();
    }
    
    // Getters and setters
    public String getHotelId() { return hotelId; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getAddress() { return address; }
    public int getStarRating() { return starRating; }
    public List<Room> getRooms() { return rooms; }
    public List<String> getAmenities() { return amenities; }
    
    public void addAmenity(String amenity) {
        amenities.add(amenity);
    }
    
    @Override
    public String toString() {
        return name + " (" + starRating + " star) - " + location + 
               " [" + rooms.size() + " rooms]";
    }
}

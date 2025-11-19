import java.time.LocalDate;
import java.util.*;

/**
 * BookingSystem - Main service class for hotel booking
 * 
 * EXPLANATION:
 * - Singleton pattern - only one instance
 * - Manages all hotels and bookings
 * - Provides search and booking operations
 * - Thread-safe for concurrent users
 * 
 * DESIGN PATTERNS:
 * - Singleton: Only one booking system instance
 * - Facade: Simplifies complex booking operations
 * - Factory: Creates bookings with unique IDs
 * 
 * SOLID PRINCIPLES:
 * - SRP: Booking system coordinates, doesn't handle details
 * - DIP: Depends on abstractions (uses interfaces ideally)
 * - OCP: Easy to extend with new hotel types
 */
public class BookingSystem {
    private static BookingSystem instance;
    private Map<String, Hotel> hotels;  // hotelId -> Hotel
    private Map<String, Booking> bookings;  // bookingId -> Booking
    private int bookingIdCounter;
    
    /**
     * Private constructor for Singleton
     */
    private BookingSystem() {
        this.hotels = new HashMap<>();
        this.bookings = new HashMap<>();
        this.bookingIdCounter = 1;
    }
    
    /**
     * Get singleton instance
     * 
     * THREAD SAFETY: synchronized for concurrent access
     */
    public static synchronized BookingSystem getInstance() {
        if (instance == null) {
            instance = new BookingSystem();
        }
        return instance;
    }
    
    /**
     * Register a hotel in the system
     */
    public void addHotel(Hotel hotel) {
        hotels.put(hotel.getHotelId(), hotel);
        System.out.println("Hotel registered: " + hotel.getName());
    }
    
    /**
     * Search hotels by location
     * 
     * EXPLANATION:
     * - Filters hotels by location (case-insensitive)
     * - Returns list of matching hotels
     * 
     * INTERVIEW POINT: How to optimize?
     * - Index hotels by location
     * - Use database with proper indexes
     * - Cache popular searches
     */
    public List<Hotel> searchHotelsByLocation(String location) {
        List<Hotel> results = new ArrayList<>();
        for (Hotel hotel : hotels.values()) {
            if (hotel.getLocation().equalsIgnoreCase(location)) {
                results.add(hotel);
            }
        }
        return results;
    }
    
    /**
     * Search available rooms across all hotels
     * 
     * EXPLANATION:
     * - Searches all hotels in location
     * - Checks room availability for dates
     * - Returns map of Hotel -> Available Rooms
     */
    public Map<Hotel, List<Room>> searchAvailableRooms(
            String location, RoomType type, LocalDate checkIn, LocalDate checkOut) {
        
        Map<Hotel, List<Room>> results = new HashMap<>();
        
        // Find hotels in location
        List<Hotel> hotelsInLocation = searchHotelsByLocation(location);
        
        // Search each hotel for available rooms
        for (Hotel hotel : hotelsInLocation) {
            List<Room> availableRooms = hotel.searchAvailableRooms(type, checkIn, checkOut);
            if (!availableRooms.isEmpty()) {
                results.put(hotel, availableRooms);
            }
        }
        
        return results;
    }
    
    /**
     * Create a booking
     * 
     * EXPLANATION:
     * - Generates unique booking ID
     * - Attempts to book the room
     * - Creates Booking object if successful
     * - Stores in bookings map
     * 
     * CONCURRENCY: Room.book() is synchronized
     * This prevents double booking of same room
     * 
     * INTERVIEW POINT: What if payment fails?
     * - Use two-phase commit
     * - Lock room temporarily (10 min)
     * - Confirm or release based on payment
     */
    public synchronized Booking createBooking(
            Customer customer, Hotel hotel, Room room, 
            LocalDate checkIn, LocalDate checkOut) {
        
        // Try to book the room (thread-safe operation)
        boolean booked = room.book(checkIn, checkOut);
        
        if (!booked) {
            System.out.println("Room not available for selected dates");
            return null;
        }
        
        // Create booking
        String bookingId = "BK" + String.format("%06d", bookingIdCounter++);
        Booking booking = new Booking(bookingId, customer, hotel, room, 
                                     checkIn, checkOut);
        
        // Store booking
        bookings.put(bookingId, booking);
        
        System.out.println("Booking created: " + bookingId);
        return booking;
    }
    
    /**
     * Confirm booking (after payment)
     */
    public boolean confirmBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            System.out.println("Booking not found: " + bookingId);
            return false;
        }
        
        booking.confirm();
        return true;
    }
    
    /**
     * Cancel booking
     * 
     * EXPLANATION:
     * - Cancels the booking
     * - Returns refund amount
     * - Frees up the room
     */
    public double cancelBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            System.out.println("Booking not found: " + bookingId);
            return 0.0;
        }
        
        return booking.cancel();
    }
    
    /**
     * Get booking by ID
     */
    public Booking getBooking(String bookingId) {
        return bookings.get(bookingId);
    }
    
    /**
     * Get all bookings for a customer
     */
    public List<Booking> getCustomerBookings(Customer customer) {
        List<Booking> customerBookings = new ArrayList<>();
        for (Booking booking : bookings.values()) {
            if (booking.getCustomer().getCustomerId().equals(
                    customer.getCustomerId())) {
                customerBookings.add(booking);
            }
        }
        return customerBookings;
    }
    
    /**
     * Get hotel by ID
     */
    public Hotel getHotel(String hotelId) {
        return hotels.get(hotelId);
    }
    
    /**
     * Display all hotels
     */
    public void displayAllHotels() {
        System.out.println("\n=== All Hotels ===");
        for (Hotel hotel : hotels.values()) {
            System.out.println(hotel);
        }
    }
}

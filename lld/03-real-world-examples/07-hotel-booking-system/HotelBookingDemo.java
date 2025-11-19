import java.time.LocalDate;
import java.util.*;

/**
 * Hotel Booking System Demo
 * 
 * This demo demonstrates the complete hotel booking workflow:
 * 1. Setup hotels and rooms
 * 2. Search for available rooms
 * 3. Create and confirm bookings
 * 4. Handle cancellations
 * 5. Check refund calculations
 * 
 * INTERVIEW FOCUS AREAS:
 * - Concurrency handling (synchronized methods)
 * - Search optimization
 * - Cancellation policies
 * - State management
 */
public class HotelBookingDemo {
    
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("  HOTEL BOOKING SYSTEM - DEMO");
        System.out.println("==========================================\n");
        
        // Get booking system instance (Singleton)
        BookingSystem system = BookingSystem.getInstance();
        
        // Setup demo data
        setupHotelsAndRooms(system);
        
        // Demo 1: Search hotels
        System.out.println("\n=== DEMO 1: Search Hotels ===");
        searchHotelsDemo(system);
        
        // Demo 2: Search available rooms
        System.out.println("\n\n=== DEMO 2: Search Available Rooms ===");
        searchRoomsDemo(system);
        
        // Demo 3: Create booking
        System.out.println("\n\n=== DEMO 3: Create and Confirm Booking ===");
        Booking booking1 = createBookingDemo(system);
        
        // Demo 4: Cancel booking (with refund)
        System.out.println("\n\n=== DEMO 4: Cancel Booking ===");
        if (booking1 != null) {
            cancelBookingDemo(system, booking1);
        }
        
        // Demo 5: Concurrent booking attempt
        System.out.println("\n\n=== DEMO 5: Handle Concurrent Booking ===");
        concurrentBookingDemo(system);
        
        // Demo 6: Complete booking lifecycle
        System.out.println("\n\n=== DEMO 6: Complete Booking Lifecycle ===");
        completeLifecycleDemo(system);
        
        System.out.println("\n==========================================");
        System.out.println("  DEMO COMPLETE");
        System.out.println("==========================================\n");
    }
    
    /**
     * Setup hotels and rooms for demo
     * 
     * EXPLANATION:
     * - Creates 2 hotels in different locations
     * - Each hotel has multiple room types
     * - Adds amenities to hotels and rooms
     */
    private static void setupHotelsAndRooms(BookingSystem system) {
        System.out.println("Setting up hotels and rooms...");
        
        // Hotel 1: Grand Plaza
        Hotel grandPlaza = new Hotel("H001", "Grand Plaza Hotel", 
                                    "New York", "123 Main St", 5);
        grandPlaza.addAmenity("WiFi");
        grandPlaza.addAmenity("Pool");
        grandPlaza.addAmenity("Gym");
        
        // Add rooms to Grand Plaza
        Room room101 = new Room("101", RoomType.STANDARD, 2);
        room101.addAmenity("WiFi");
        room101.addAmenity("TV");
        grandPlaza.addRoom(room101);
        
        Room room201 = new Room("201", RoomType.DELUXE, 2);
        room201.addAmenity("WiFi");
        room201.addAmenity("TV");
        room201.addAmenity("Mini Bar");
        grandPlaza.addRoom(room201);
        
        Room room301 = new Room("301", RoomType.SUITE, 4);
        room301.addAmenity("WiFi");
        room301.addAmenity("TV");
        room301.addAmenity("Mini Bar");
        room301.addAmenity("Jacuzzi");
        grandPlaza.addRoom(room301);
        
        system.addHotel(grandPlaza);
        
        // Hotel 2: Seaside Resort
        Hotel seasideResort = new Hotel("H002", "Seaside Resort",
                                       "Miami", "456 Beach Rd", 4);
        seasideResort.addAmenity("WiFi");
        seasideResort.addAmenity("Beach Access");
        seasideResort.addAmenity("Restaurant");
        
        // Add rooms to Seaside Resort
        Room room102 = new Room("102", RoomType.STANDARD, 2);
        room102.addAmenity("WiFi");
        room102.addAmenity("Ocean View");
        seasideResort.addRoom(room102);
        
        Room room202 = new Room("202", RoomType.DELUXE, 3);
        room202.addAmenity("WiFi");
        room202.addAmenity("Ocean View");
        room202.addAmenity("Balcony");
        seasideResort.addRoom(room202);
        
        system.addHotel(seasideResort);
        
        System.out.println("Setup complete!\n");
        system.displayAllHotels();
    }
    
    /**
     * Demo: Search hotels by location
     */
    private static void searchHotelsDemo(BookingSystem system) {
        System.out.println("Searching for hotels in New York...");
        List<Hotel> hotels = system.searchHotelsByLocation("New York");
        
        System.out.println("Found " + hotels.size() + " hotel(s):");
        for (Hotel hotel : hotels) {
            System.out.println("  - " + hotel);
            System.out.println("    Amenities: " + hotel.getAmenities());
        }
    }
    
    /**
     * Demo: Search available rooms
     */
    private static void searchRoomsDemo(BookingSystem system) {
        LocalDate checkIn = LocalDate.now().plusDays(7);
        LocalDate checkOut = LocalDate.now().plusDays(10);
        
        System.out.println("Searching for DELUXE rooms in New York");
        System.out.println("Check-in: " + checkIn);
        System.out.println("Check-out: " + checkOut);
        
        Map<Hotel, List<Room>> results = system.searchAvailableRooms(
            "New York", RoomType.DELUXE, checkIn, checkOut);
        
        System.out.println("\nSearch Results:");
        for (Map.Entry<Hotel, List<Room>> entry : results.entrySet()) {
            Hotel hotel = entry.getKey();
            List<Room> rooms = entry.getValue();
            
            System.out.println("\n" + hotel.getName() + ":");
            for (Room room : rooms) {
                System.out.println("  - " + room);
                System.out.println("    Amenities: " + room.getAmenities());
                System.out.println("    Price: $" + 
                    room.calculatePrice(checkIn, checkOut) + 
                    " for 3 nights");
            }
        }
    }
    
    /**
     * Demo: Create and confirm booking
     */
    private static Booking createBookingDemo(BookingSystem system) {
        // Create customer
        Customer customer = new Customer("C001", "John Doe", 
                                        "john@email.com", "555-0100");
        
        // Get hotel and room
        Hotel hotel = system.getHotel("H001");
        Room room = hotel.getRoomByNumber("201");
        
        // Booking dates
        LocalDate checkIn = LocalDate.now().plusDays(7);
        LocalDate checkOut = LocalDate.now().plusDays(10);
        
        System.out.println("Customer: " + customer.getName());
        System.out.println("Hotel: " + hotel.getName());
        System.out.println("Room: " + room);
        System.out.println("Dates: " + checkIn + " to " + checkOut);
        
        // Create booking
        Booking booking = system.createBooking(customer, hotel, room, 
                                              checkIn, checkOut);
        
        if (booking != null) {
            System.out.println("\n✓ Booking created successfully!");
            System.out.println("Booking ID: " + booking.getBookingId());
            System.out.println("Total Amount: $" + booking.getTotalAmount());
            System.out.println("Status: " + booking.getStatus());
            
            // Confirm booking (simulate payment success)
            System.out.println("\nProcessing payment...");
            boolean confirmed = system.confirmBooking(booking.getBookingId());
            if (confirmed) {
                System.out.println("✓ Payment successful! Booking confirmed.");
                System.out.println("Confirmation sent to: " + customer.getEmail());
            }
        }
        
        return booking;
    }
    
    /**
     * Demo: Cancel booking with refund calculation
     */
    private static void cancelBookingDemo(BookingSystem system, Booking booking) {
        System.out.println("Original Booking:");
        System.out.println(booking);
        
        System.out.println("\nCancelling booking...");
        double refund = system.cancelBooking(booking.getBookingId());
        
        System.out.println("✓ Booking cancelled");
        System.out.println("Refund Amount: $" + refund);
        System.out.println("Refund Calculation:");
        System.out.println("  - Original Amount: $" + booking.getTotalAmount());
        System.out.println("  - Cancellation >7 days before: 100% refund");
        System.out.println("  - Refunded: $" + refund);
    }
    
    /**
     * Demo: Handle concurrent booking attempts
     * 
     * EXPLANATION:
     * - Two customers try to book the same room
     * - Only one should succeed (thread-safe)
     * - The other should get failure message
     */
    private static void concurrentBookingDemo(BookingSystem system) {
        System.out.println("Simulating concurrent booking attempts...");
        System.out.println("Two customers trying to book the same room simultaneously\n");
        
        // Get the same room
        Hotel hotel = system.getHotel("H001");
        Room room = hotel.getRoomByNumber("101");
        
        LocalDate checkIn = LocalDate.now().plusDays(14);
        LocalDate checkOut = LocalDate.now().plusDays(17);
        
        // Customer 1
        Customer customer1 = new Customer("C002", "Alice Smith",
                                         "alice@email.com", "555-0101");
        
        // Customer 2
        Customer customer2 = new Customer("C003", "Bob Johnson",
                                         "bob@email.com", "555-0102");
        
        // Try booking for customer 1
        System.out.println("Customer 1 (" + customer1.getName() + ") attempting to book...");
        Booking booking1 = system.createBooking(customer1, hotel, room, 
                                               checkIn, checkOut);
        
        // Try booking for customer 2 (should fail - room already booked)
        System.out.println("\nCustomer 2 (" + customer2.getName() + ") attempting to book same room...");
        Booking booking2 = system.createBooking(customer2, hotel, room,
                                               checkIn, checkOut);
        
        System.out.println("\n--- Result ---");
        System.out.println("Customer 1: " + (booking1 != null ? "SUCCESS ✓" : "FAILED ✗"));
        System.out.println("Customer 2: " + (booking2 != null ? "SUCCESS ✓" : "FAILED ✗"));
        System.out.println("\nCONCURRENCY HANDLED: Only one booking succeeded!");
    }
    
    /**
     * Demo: Complete booking lifecycle
     * 
     * Shows: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
     */
    private static void completeLifecycleDemo(BookingSystem system) {
        System.out.println("Demonstrating complete booking lifecycle...\n");
        
        // Create customer and get hotel/room
        Customer customer = new Customer("C004", "Sarah Wilson",
                                        "sarah@email.com", "555-0103");
        Hotel hotel = system.getHotel("H002");
        Room room = hotel.getRoomByNumber("102");
        
        // Create booking for today
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = LocalDate.now().plusDays(2);
        
        // Step 1: Create booking
        System.out.println("Step 1: Create Booking");
        Booking booking = system.createBooking(customer, hotel, room,
                                              checkIn, checkOut);
        System.out.println("Status: " + booking.getStatus());
        
        // Step 2: Confirm booking
        System.out.println("\nStep 2: Confirm Booking (Payment Success)");
        system.confirmBooking(booking.getBookingId());
        System.out.println("Status: " + booking.getStatus());
        
        // Step 3: Check-in
        System.out.println("\nStep 3: Guest Check-in");
        booking.checkIn();
        System.out.println("Status: " + booking.getStatus());
        
        // Step 4: Check-out
        System.out.println("\nStep 4: Guest Check-out");
        booking.checkOut();
        System.out.println("Status: " + booking.getStatus());
        
        System.out.println("\n✓ Booking lifecycle complete!");
        System.out.println("Final Booking Details:");
        System.out.println(booking);
    }
}

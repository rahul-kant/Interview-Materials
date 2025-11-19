import java.time.LocalDateTime;
import java.util.*;

/**
 * MovieBookingDemo - Demonstrates movie ticket booking system
 * 
 * Interview Key Points:
 * - Complete booking workflow
 * - Concurrent booking scenarios
 * - Seat locking mechanism
 * - Payment integration
 */
public class MovieBookingDemo {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║    MOVIE TICKET BOOKING SYSTEM DEMO    ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        BookingSystem system = BookingSystem.getInstance();
        
        // Setup
        setupMoviesAndTheaters(system);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("SCENARIO 1: Successful Booking");
        System.out.println("=".repeat(50));
        testSuccessfulBooking(system);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("SCENARIO 2: Concurrent Booking (Same Seats)");
        System.out.println("=".repeat(50));
        testConcurrentBooking(system);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("SCENARIO 3: Booking Cancellation");
        System.out.println("=".repeat(50));
        testCancellation(system);
    }
    
    /**
     * Setup initial data
     */
    private static void setupMoviesAndTheaters(BookingSystem system) {
        System.out.println("Setting up movies and theaters...\n");
        
        // Add movies
        Movie avengers = new Movie(
            "MOV001",
            "Avengers: Endgame",
            "Epic superhero finale",
            181,
            Movie.Genre.ACTION,
            Movie.Rating.PG_13,
            Arrays.asList("Robert Downey Jr.", "Chris Evans", "Scarlett Johansson"),
            "Russo Brothers"
        );
        
        Movie inception = new Movie(
            "MOV002",
            "Inception",
            "Dreams within dreams",
            148,
            Movie.Genre.SCI_FI,
            Movie.Rating.PG_13,
            Arrays.asList("Leonardo DiCaprio", "Tom Hardy", "Joseph Gordon-Levitt"),
            "Christopher Nolan"
        );
        
        system.addMovie(avengers);
        system.addMovie(inception);
        
        // Add theaters
        Theater theater1 = new Theater("THR001", "IMAX Screen 1", 8, 10);
        Theater theater2 = new Theater("THR002", "Premium Hall 2", 6, 8);
        
        system.addTheater(theater1);
        system.addTheater(theater2);
        
        // Add shows
        Show show1 = new Show(
            "SHW001",
            avengers,
            theater1,
            LocalDateTime.now().plusHours(2)
        );
        
        Show show2 = new Show(
            "SHW002",
            inception,
            theater2,
            LocalDateTime.now().plusHours(4)
        );
        
        system.addShow(show1);
        system.addShow(show2);
        
        System.out.println();
    }
    
    /**
     * Scenario 1: Successful booking flow
     */
    private static void testSuccessfulBooking(BookingSystem system) {
        String userId = "USER001";
        String showId = "SHW001";
        
        // Display available seats
        system.displaySeatMap(showId);
        
        // User selects seats
        List<String> requestedSeats = Arrays.asList("A5", "A6", "A7");
        System.out.println("\n👤 " + userId + " requesting seats: " + requestedSeats);
        
        // Create booking (locks seats)
        Booking booking = system.createBooking(userId, showId, requestedSeats);
        
        if (booking != null) {
            System.out.println("📋 Booking Details:");
            System.out.println("   " + booking);
            
            // Show seat map with locked seats
            system.displaySeatMap(showId);
            
            // Simulate payment
            System.out.println("💳 Processing payment...");
            String paymentId = "PAY" + System.currentTimeMillis();
            
            // Confirm booking
            if (system.confirmBooking(booking.getId(), paymentId)) {
                System.out.println("✅ Booking successful!");
                System.out.println("   Payment ID: " + paymentId);
                
                // Show final seat map
                system.displaySeatMap(showId);
            }
        }
    }
    
    /**
     * Scenario 2: Two users try to book same seats
     */
    private static void testConcurrentBooking(BookingSystem system) throws InterruptedException {
        String showId = "SHW002";
        List<String> sameSeats = Arrays.asList("B3", "B4");
        
        System.out.println("\n👤 USER002 and USER003 both want seats: " + sameSeats);
        
        // User 1 creates booking
        System.out.println("\n👤 USER002 attempting to book...");
        Booking booking1 = system.createBooking("USER002", showId, sameSeats);
        
        if (booking1 != null) {
            System.out.println("✓ USER002 successfully locked seats");
        }
        
        // User 2 tries to book same seats (should fail)
        System.out.println("\n👤 USER003 attempting to book same seats...");
        Booking booking2 = system.createBooking("USER003", showId, sameSeats);
        
        if (booking2 == null) {
            System.out.println("✓ USER003 booking failed (seats already locked) - Working as expected!");
        }
        
        // User 1 completes booking
        System.out.println("\n💳 USER002 completing payment...");
        system.confirmBooking(booking1.getId(), "PAY" + System.currentTimeMillis());
        
        system.displaySeatMap(showId);
    }
    
    /**
     * Scenario 3: Booking and cancellation
     */
    private static void testCancellation(BookingSystem system) {
        String userId = "USER004";
        String showId = "SHW001";
        List<String> seats = Arrays.asList("C1", "C2");
        
        System.out.println("\n👤 " + userId + " booking seats: " + seats);
        
        // Create and confirm booking
        Booking booking = system.createBooking(userId, showId, seats);
        if (booking != null) {
            system.confirmBooking(booking.getId(), "PAY" + System.currentTimeMillis());
            
            System.out.println("\n📋 Current bookings for " + userId + ":");
            for (Booking b : system.getUserBookings(userId)) {
                System.out.println("   " + b);
            }
            
            // Cancel booking
            System.out.println("\n❌ " + userId + " cancelling booking...");
            system.cancelBooking(booking.getId());
            
            System.out.println("\n📋 After cancellation:");
            for (Booking b : system.getUserBookings(userId)) {
                System.out.println("   " + b);
            }
            
            system.displaySeatMap(showId);
        }
        
        System.out.println("\n✅ Demo completed successfully!");
        System.out.println("\n📊 Key Design Points Demonstrated:");
        System.out.println("  ✓ Thread-safe seat locking");
        System.out.println("  ✓ Concurrent booking prevention");
        System.out.println("  ✓ Seat status management");
        System.out.println("  ✓ Booking lifecycle (create, confirm, cancel)");
        System.out.println("  ✓ Real-time seat availability");
    }
}

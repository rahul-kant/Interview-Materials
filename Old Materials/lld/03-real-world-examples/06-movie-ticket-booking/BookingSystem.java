import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BookingSystem - Main controller for movie ticket booking (Singleton)
 * 
 * Interview Key Points:
 * - Singleton pattern
 * - Thread-safe booking operations
 * - Concurrency handling for simultaneous bookings
 * - Search and filter functionality
 */
public class BookingSystem {
    private static BookingSystem instance;
    
    private final Map<String, Movie> movies;
    private final Map<String, Theater> theaters;
    private final Map<String, Show> shows;
    private final Map<String, Booking> bookings;
    private final AtomicInteger bookingIdCounter;
    
    private BookingSystem() {
        this.movies = new ConcurrentHashMap<>();
        this.theaters = new ConcurrentHashMap<>();
        this.shows = new ConcurrentHashMap<>();
        this.bookings = new ConcurrentHashMap<>();
        this.bookingIdCounter = new AtomicInteger(1);
    }
    
    public static synchronized BookingSystem getInstance() {
        if (instance == null) {
            instance = new BookingSystem();
        }
        return instance;
    }
    
    // ========== Movie Management ==========
    
    public void addMovie(Movie movie) {
        movies.put(movie.getId(), movie);
        System.out.println("✓ Added movie: " + movie.getTitle());
    }
    
    public Movie getMovie(String movieId) {
        return movies.get(movieId);
    }
    
    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies.values());
    }
    
    // ========== Theater Management ==========
    
    public void addTheater(Theater theater) {
        theaters.put(theater.getId(), theater);
        System.out.println("✓ Added theater: " + theater.getName());
    }
    
    public Theater getTheater(String theaterId) {
        return theaters.get(theaterId);
    }
    
    // ========== Show Management ==========
    
    public void addShow(Show show) {
        shows.put(show.getId(), show);
        System.out.println("✓ Added show: " + show);
    }
    
    public Show getShow(String showId) {
        return shows.get(showId);
    }
    
    /**
     * Search shows by movie
     */
    public List<Show> getShowsByMovie(String movieId) {
        List<Show> result = new ArrayList<>();
        for (Show show : shows.values()) {
            if (show.getMovie().getId().equals(movieId) && 
                show.getStatus() == Show.ShowStatus.UPCOMING) {
                result.add(show);
            }
        }
        return result;
    }
    
    /**
     * Search shows by date
     */
    public List<Show> getShowsByDate(LocalDateTime date) {
        List<Show> result = new ArrayList<>();
        for (Show show : shows.values()) {
            if (show.getStartTime().toLocalDate().equals(date.toLocalDate()) &&
                show.getStatus() == Show.ShowStatus.UPCOMING) {
                result.add(show);
            }
        }
        return result;
    }
    
    // ========== Booking Operations ==========
    
    /**
     * Create a booking (locks seats)
     */
    public synchronized Booking createBooking(String userId, String showId, 
                                             List<String> seatNumbers) {
        Show show = shows.get(showId);
        if (show == null) {
            System.out.println("❌ Show not found");
            return null;
        }
        
        // Lock seats
        if (!show.lockSeats(seatNumbers, userId)) {
            System.out.println("❌ Could not lock requested seats");
            return null;
        }
        
        // Create booking
        String bookingId = "BKG" + bookingIdCounter.getAndIncrement();
        Booking booking = new Booking(bookingId, userId, show, seatNumbers);
        bookings.put(bookingId, booking);
        
        System.out.println("✓ Booking created: " + bookingId);
        System.out.println("  Seats locked for 5 minutes");
        
        return booking;
    }
    
    /**
     * Confirm booking with payment
     */
    public synchronized boolean confirmBooking(String bookingId, String paymentId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            System.out.println("❌ Booking not found");
            return false;
        }
        
        if (booking.confirm(paymentId)) {
            System.out.println("✅ Booking confirmed: " + bookingId);
            return true;
        }
        
        System.out.println("❌ Could not confirm booking");
        return false;
    }
    
    /**
     * Cancel booking
     */
    public synchronized boolean cancelBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            System.out.println("❌ Booking not found");
            return false;
        }
        
        if (booking.cancel()) {
            System.out.println("✓ Booking cancelled: " + bookingId);
            return true;
        }
        
        System.out.println("❌ Could not cancel booking");
        return false;
    }
    
    /**
     * Get user's bookings
     */
    public List<Booking> getUserBookings(String userId) {
        List<Booking> userBookings = new ArrayList<>();
        for (Booking booking : bookings.values()) {
            if (booking.getUserId().equals(userId)) {
                userBookings.add(booking);
            }
        }
        return userBookings;
    }
    
    /**
     * Display seat map for a show
     */
    public void displaySeatMap(String showId) {
        Show show = shows.get(showId);
        if (show == null) {
            System.out.println("❌ Show not found");
            return;
        }
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("SEAT MAP: " + show.getMovie().getTitle());
        System.out.println(show.getTheater().getName() + " - " + show.getStartTime());
        System.out.println("=".repeat(50));
        System.out.println("[ ] = Available | [L] = Locked | [X] = Booked");
        System.out.println("=".repeat(50));
        
        String[][] seatMap = show.getSeatMap();
        System.out.print("    ");
        for (int col = 0; col < seatMap[0].length; col++) {
            System.out.printf("%3d ", col + 1);
        }
        System.out.println("\n    " + "----".repeat(seatMap[0].length));
        
        for (int row = 0; row < seatMap.length; row++) {
            char rowLabel = (char) ('A' + row);
            System.out.printf("%c | ", rowLabel);
            for (int col = 0; col < seatMap[row].length; col++) {
                System.out.print(seatMap[row][col] + " ");
            }
            System.out.println();
        }
        
        System.out.println("=".repeat(50));
        System.out.println("Available seats: " + show.getAvailableSeatsCount());
        System.out.println("=".repeat(50) + "\n");
    }
}

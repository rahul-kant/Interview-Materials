import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Show Class - Represents a movie show/screening
 * 
 * Interview Key Points:
 * - Show timing management
 * - Seat availability tracking
 * - Thread-safe seat operations
 * - Show status (upcoming, running, completed)
 */
public class Show {
    private final String id;
    private final Movie movie;
    private final Theater theater;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Map<String, Seat> seats; // seatNumber -> Seat
    private ShowStatus status;
    
    private static final long LOCK_DURATION_MS = 5 * 60 * 1000; // 5 minutes
    
    public enum ShowStatus {
        UPCOMING,
        RUNNING,
        COMPLETED,
        CANCELLED
    }
    
    public Show(String id, Movie movie, Theater theater, LocalDateTime startTime) {
        this.id = id;
        this.movie = movie;
        this.theater = theater;
        this.startTime = startTime;
        this.endTime = startTime.plus(movie.getDuration());
        this.seats = new HashMap<>();
        this.status = ShowStatus.UPCOMING;
        
        initializeSeats();
    }
    
    /**
     * Initialize seats from theater layout
     */
    private void initializeSeats() {
        for (Seat seat : theater.getAllSeats()) {
            // Create new seat instance for this show
            Seat showSeat = new Seat(seat.getRow(), seat.getColumn(), seat.getType());
            seats.put(showSeat.getSeatNumber(), showSeat);
        }
    }
    
    /**
     * Get available seats
     */
    public synchronized List<Seat> getAvailableSeats() {
        List<Seat> available = new ArrayList<>();
        for (Seat seat : seats.values()) {
            if (seat.isAvailable()) {
                available.add(seat);
            }
        }
        return available;
    }
    
    /**
     * Lock seats for booking
     */
    public synchronized boolean lockSeats(List<String> seatNumbers, String userId) {
        // Check if all seats are available
        for (String seatNumber : seatNumbers) {
            Seat seat = seats.get(seatNumber);
            if (seat == null || !seat.isAvailable()) {
                return false;
            }
        }
        
        // Lock all seats
        for (String seatNumber : seatNumbers) {
            Seat seat = seats.get(seatNumber);
            if (!seat.lock(userId, LOCK_DURATION_MS)) {
                // Rollback if any lock fails
                unlockSeats(seatNumbers, userId);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Unlock seats (cancel lock)
     */
    public synchronized void unlockSeats(List<String> seatNumbers, String userId) {
        for (String seatNumber : seatNumbers) {
            Seat seat = seats.get(seatNumber);
            if (seat != null) {
                seat.release();
            }
        }
    }
    
    /**
     * Confirm booking for locked seats
     */
    public synchronized boolean confirmBooking(List<String> seatNumbers, String userId) {
        // Check if all seats are locked by this user
        for (String seatNumber : seatNumbers) {
            Seat seat = seats.get(seatNumber);
            if (seat == null || seat.getStatus() != Seat.SeatStatus.LOCKED) {
                return false;
            }
        }
        
        // Confirm all seats
        for (String seatNumber : seatNumbers) {
            Seat seat = seats.get(seatNumber);
            if (!seat.book(userId)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Calculate total price for seats
     */
    public double calculatePrice(List<String> seatNumbers) {
        double total = 0;
        for (String seatNumber : seatNumbers) {
            Seat seat = seats.get(seatNumber);
            if (seat != null) {
                total += seat.getPrice();
            }
        }
        return total;
    }
    
    /**
     * Get seat by number
     */
    public Seat getSeat(String seatNumber) {
        return seats.get(seatNumber);
    }
    
    /**
     * Get seat map for display
     */
    public String[][] getSeatMap() {
        int rows = theater.getRows();
        int cols = theater.getColumns();
        String[][] map = new String[rows][cols];
        
        for (Seat seat : seats.values()) {
            String symbol;
            switch (seat.getStatus()) {
                case AVAILABLE:
                    symbol = "[ ]";
                    break;
                case LOCKED:
                    symbol = "[L]";
                    break;
                case BOOKED:
                    symbol = "[X]";
                    break;
                default:
                    symbol = "[ ]";
            }
            map[seat.getRow()][seat.getColumn()] = symbol;
        }
        
        return map;
    }
    
    public String getId() {
        return id;
    }
    
    public Movie getMovie() {
        return movie;
    }
    
    public Theater getTheater() {
        return theater;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public ShowStatus getStatus() {
        return status;
    }
    
    public void setStatus(ShowStatus status) {
        this.status = status;
    }
    
    public int getAvailableSeatsCount() {
        return getAvailableSeats().size();
    }
    
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        return String.format("Show: %s at %s - %s (%d seats available)", 
            movie.getTitle(), 
            startTime.format(formatter),
            theater.getName(),
            getAvailableSeatsCount());
    }
}

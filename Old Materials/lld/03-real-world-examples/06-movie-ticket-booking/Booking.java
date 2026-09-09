import java.time.LocalDateTime;
import java.util.*;

/**
 * Booking Class - Represents a ticket booking
 * 
 * Interview Key Points:
 * - Booking state management
 * - Payment integration
 * - Cancellation logic
 */
public class Booking {
    private final String id;
    private final String userId;
    private final Show show;
    private final List<String> seatNumbers;
    private final double totalPrice;
    private final LocalDateTime bookingTime;
    private BookingStatus status;
    private String paymentId;
    
    public enum BookingStatus {
        PENDING,
        CONFIRMED,
        CANCELLED,
        EXPIRED
    }
    
    public Booking(String id, String userId, Show show, List<String> seatNumbers) {
        this.id = id;
        this.userId = userId;
        this.show = show;
        this.seatNumbers = new ArrayList<>(seatNumbers);
        this.totalPrice = show.calculatePrice(seatNumbers);
        this.bookingTime = LocalDateTime.now();
        this.status = BookingStatus.PENDING;
    }
    
    /**
     * Confirm booking after payment
     */
    public boolean confirm(String paymentId) {
        if (status != BookingStatus.PENDING) {
            return false;
        }
        
        if (show.confirmBooking(seatNumbers, userId)) {
            this.status = BookingStatus.CONFIRMED;
            this.paymentId = paymentId;
            return true;
        }
        
        return false;
    }
    
    /**
     * Cancel booking
     */
    public boolean cancel() {
        if (status != BookingStatus.CONFIRMED) {
            return false;
        }
        
        // In real system, would process refund here
        show.unlockSeats(seatNumbers, userId);
        this.status = BookingStatus.CANCELLED;
        return true;
    }
    
    public String getId() {
        return id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public Show getShow() {
        return show;
    }
    
    public List<String> getSeatNumbers() {
        return new ArrayList<>(seatNumbers);
    }
    
    public double getTotalPrice() {
        return totalPrice;
    }
    
    public LocalDateTime getBookingTime() {
        return bookingTime;
    }
    
    public BookingStatus getStatus() {
        return status;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    @Override
    public String toString() {
        return String.format("Booking %s: %s - Seats: %s - $%.2f [%s]",
            id, show.getMovie().getTitle(), seatNumbers, totalPrice, status);
    }
}

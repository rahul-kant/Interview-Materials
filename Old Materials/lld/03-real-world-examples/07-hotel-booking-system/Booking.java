import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Booking class represents a room reservation
 * 
 * EXPLANATION:
 * - Links customer, room, and hotel together
 * - Tracks booking status lifecycle
 * - Calculates total cost
 * - Handles cancellation
 * 
 * DESIGN PATTERNS:
 * - State pattern could be applied for status transitions
 * - SRP: Booking manages reservation details only
 * 
 * INTERVIEW POINTS:
 * - How to handle concurrent bookings?
 * - Cancellation policy implementation
 * - Refund calculation
 */
public class Booking {
    private String bookingId;
    private Customer customer;
    private Hotel hotel;
    private Room room;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private BookingStatus status;
    private double totalAmount;
    private LocalDate bookingDate;
    
    /**
     * Constructor
     * 
     * @param bookingId Unique identifier
     * @param customer Guest making booking
     * @param hotel Hotel being booked
     * @param room Room being booked
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     */
    public Booking(String bookingId, Customer customer, Hotel hotel, Room room,
                  LocalDate checkIn, LocalDate checkOut) {
        this.bookingId = bookingId;
        this.customer = customer;
        this.hotel = hotel;
        this.room = room;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.status = BookingStatus.PENDING;
        this.bookingDate = LocalDate.now();
        this.totalAmount = calculateTotalAmount();
    }
    
    /**
     * Calculate total booking amount
     * 
     * EXPLANATION:
     * - Gets base price from room type
     * - Multiplies by number of nights
     * - Can be extended with dynamic pricing, taxes, etc.
     */
    private double calculateTotalAmount() {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return nights * room.getType().getBasePrice();
    }
    
    /**
     * Confirm the booking (after payment)
     * 
     * EXPLANATION:
     * - Changes status to CONFIRMED
     * - In real system, would trigger:
     *   - Email confirmation
     *   - SMS notification
     *   - Calendar update
     */
    public void confirm() {
        if (status == BookingStatus.PENDING) {
            status = BookingStatus.CONFIRMED;
            System.out.println("Booking " + bookingId + " confirmed!");
        }
    }
    
    /**
     * Cancel the booking
     * 
     * EXPLANATION:
     * - Frees up the room
     * - Calculates refund based on policy
     * - Updates status
     * 
     * INTERVIEW POINT: Cancellation policy
     * - 100% refund if cancelled >7 days before
     * - 50% refund if cancelled 3-7 days before
     * - No refund if <3 days before
     */
    public double cancel() {
        if (status != BookingStatus.CONFIRMED) {
            System.out.println("Cannot cancel booking in " + status + " status");
            return 0.0;
        }
        
        // Free up the room
        room.cancelBooking(checkIn, checkOut);
        
        // Calculate refund
        double refund = calculateRefund();
        
        // Update status
        status = BookingStatus.CANCELLED;
        
        System.out.println("Booking " + bookingId + " cancelled. Refund: $" + refund);
        return refund;
    }
    
    /**
     * Calculate refund amount based on cancellation policy
     */
    private double calculateRefund() {
        long daysUntilCheckIn = ChronoUnit.DAYS.between(LocalDate.now(), checkIn);
        
        if (daysUntilCheckIn > 7) {
            return totalAmount;  // 100% refund
        } else if (daysUntilCheckIn >= 3) {
            return totalAmount * 0.5;  // 50% refund
        } else {
            return 0.0;  // No refund
        }
    }
    
    /**
     * Check-in the guest
     */
    public void checkIn() {
        if (status == BookingStatus.CONFIRMED && 
            LocalDate.now().equals(checkIn)) {
            status = BookingStatus.CHECKED_IN;
            System.out.println("Guest checked in for booking " + bookingId);
        }
    }
    
    /**
     * Check-out the guest
     */
    public void checkOut() {
        if (status == BookingStatus.CHECKED_IN) {
            status = BookingStatus.CHECKED_OUT;
            System.out.println("Guest checked out for booking " + bookingId);
        }
    }
    
    // Getters
    public String getBookingId() { return bookingId; }
    public Customer getCustomer() { return customer; }
    public Hotel getHotel() { return hotel; }
    public Room getRoom() { return room; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public BookingStatus getStatus() { return status; }
    public double getTotalAmount() { return totalAmount; }
    public LocalDate getBookingDate() { return bookingDate; }
    
    @Override
    public String toString() {
        return "Booking #" + bookingId + ": " + customer.getName() + 
               " at " + hotel.getName() + ", Room " + room.getRoomNumber() +
               " (" + checkIn + " to " + checkOut + ") - " + status +
               " - Total: $" + totalAmount;
    }
}

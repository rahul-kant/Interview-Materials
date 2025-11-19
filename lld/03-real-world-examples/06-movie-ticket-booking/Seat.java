/**
 * Seat Class - Represents a theater seat
 * 
 * Interview Key Points:
 * - Seat status management
 * - Seat type (regular, premium, VIP)
 * - Price variation by type
 */
public class Seat {
    private final String seatNumber;
    private final int row;
    private final int column;
    private final SeatType type;
    private SeatStatus status;
    private String lockedBy; // User ID who locked it
    private long lockExpiry;
    
    public enum SeatType {
        REGULAR(10.0),
        PREMIUM(15.0),
        VIP(25.0);
        
        private final double basePrice;
        
        SeatType(double basePrice) {
            this.basePrice = basePrice;
        }
        
        public double getBasePrice() {
            return basePrice;
        }
    }
    
    public enum SeatStatus {
        AVAILABLE,
        LOCKED,     // Temporarily locked during booking
        BOOKED      // Confirmed booking
    }
    
    public Seat(int row, int column, SeatType type) {
        this.row = row;
        this.column = column;
        this.seatNumber = generateSeatNumber(row, column);
        this.type = type;
        this.status = SeatStatus.AVAILABLE;
    }
    
    private String generateSeatNumber(int row, int column) {
        // Convert row to letter (A, B, C, ...)
        char rowLetter = (char) ('A' + row);
        return String.format("%c%d", rowLetter, column + 1);
    }
    
    /**
     * Lock seat for a user
     */
    public synchronized boolean lock(String userId, long lockDurationMs) {
        if (status != SeatStatus.AVAILABLE) {
            return false;
        }
        
        this.status = SeatStatus.LOCKED;
        this.lockedBy = userId;
        this.lockExpiry = System.currentTimeMillis() + lockDurationMs;
        return true;
    }
    
    /**
     * Check if lock has expired
     */
    public synchronized boolean isLockExpired() {
        if (status != SeatStatus.LOCKED) {
            return false;
        }
        
        if (System.currentTimeMillis() > lockExpiry) {
            // Auto-release expired lock
            release();
            return true;
        }
        
        return false;
    }
    
    /**
     * Release lock or booking
     */
    public synchronized void release() {
        this.status = SeatStatus.AVAILABLE;
        this.lockedBy = null;
        this.lockExpiry = 0;
    }
    
    /**
     * Confirm booking
     */
    public synchronized boolean book(String userId) {
        if (status == SeatStatus.LOCKED && userId.equals(lockedBy)) {
            this.status = SeatStatus.BOOKED;
            return true;
        }
        return false;
    }
    
    public String getSeatNumber() {
        return seatNumber;
    }
    
    public int getRow() {
        return row;
    }
    
    public int getColumn() {
        return column;
    }
    
    public SeatType getType() {
        return type;
    }
    
    public synchronized SeatStatus getStatus() {
        // Check for expired locks
        isLockExpired();
        return status;
    }
    
    public double getPrice() {
        return type.getBasePrice();
    }
    
    public synchronized boolean isAvailable() {
        isLockExpired();
        return status == SeatStatus.AVAILABLE;
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - %s", seatNumber, type, status);
    }
}

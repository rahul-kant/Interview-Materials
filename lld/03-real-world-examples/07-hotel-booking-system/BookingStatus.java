/**
 * Enum representing booking status
 * 
 * Tracks the lifecycle of a booking from creation to completion
 */
public enum BookingStatus {
    PENDING,      // Initial state, payment not confirmed
    CONFIRMED,    // Payment successful, booking confirmed
    CHECKED_IN,   // Guest has checked in
    CHECKED_OUT,  // Guest has checked out
    CANCELLED,    // Booking cancelled
    NO_SHOW       // Guest didn't show up
}

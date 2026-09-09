import java.time.LocalDateTime;

/**
 * TimeSlot - Represents a time interval
 * 
 * EXPLANATION:
 * - Immutable time interval
 * - Used for meetings and availability
 * - Provides overlap detection
 */
public class TimeSlot {
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    
    public TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    /**
     * Check if this slot overlaps with another
     * 
     * INTERVIEW KEY POINT:
     * Two intervals overlap if:
     * (start1 < end2) AND (start2 < end1)
     */
    public boolean overlaps(TimeSlot other) {
        return this.startTime.isBefore(other.endTime) && 
               other.startTime.isBefore(this.endTime);
    }
    
    /**
     * Check if this slot contains given time
     */
    public boolean contains(LocalDateTime time) {
        return !time.isBefore(startTime) && time.isBefore(endTime);
    }
    
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    
    @Override
    public String toString() {
        return startTime + " → " + endTime;
    }
}

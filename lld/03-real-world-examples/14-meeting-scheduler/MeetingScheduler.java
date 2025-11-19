import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Meeting Scheduler Service
 * 
 * CORE ALGORITHM:
 * - Check availability: O(n) where n = meetings in time range
 * - Find free slots: O(n log n) with interval merging
 */
public class MeetingScheduler {
    private static MeetingScheduler instance;
    private Map<String, List<Meeting>> userMeetings;
    private AtomicInteger meetingIdCounter;
    
    private MeetingScheduler() {
        this.userMeetings = new ConcurrentHashMap<>();
        this.meetingIdCounter = new AtomicInteger(1);
    }
    
    public static synchronized MeetingScheduler getInstance() {
        if (instance == null) {
            instance = new MeetingScheduler();
        }
        return instance;
    }
    
    /**
     * Schedule meeting
     */
    public Meeting scheduleMeeting(String title, TimeSlot timeSlot, 
                                  String organizer, List<String> attendees) {
        // Check if all attendees are available
        List<String> allParticipants = new ArrayList<>(attendees);
        allParticipants.add(organizer);
        
        for (String user : allParticipants) {
            if (!isAvailable(user, timeSlot)) {
                System.out.println("❌ " + user + " is not available");
                return null;
            }
        }
        
        // Create meeting
        String meetingId = "MTG" + meetingIdCounter.getAndIncrement();
        Meeting meeting = new Meeting(meetingId, title, timeSlot, organizer, attendees);
        
        // Add to all participants' calendars
        for (String user : allParticipants) {
            userMeetings.computeIfAbsent(user, k -> new ArrayList<>()).add(meeting);
        }
        
        System.out.println("✓ Meeting scheduled: " + meetingId);
        return meeting;
    }
    
    /**
     * Check if user is available
     */
    public boolean isAvailable(String userId, TimeSlot requestedSlot) {
        List<Meeting> meetings = userMeetings.getOrDefault(userId, new ArrayList<>());
        
        for (Meeting meeting : meetings) {
            if (meeting.getTimeSlot().overlaps(requestedSlot)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Find common free slots for all attendees
     * 
     * ALGORITHM:
     * 1. Collect all busy intervals
     * 2. Sort and merge overlapping intervals
     * 3. Find gaps >= duration
     */
    public List<TimeSlot> findCommonFreeSlots(List<String> attendees, 
                                             LocalDateTime searchStart,
                                             LocalDateTime searchEnd,
                                             int durationMinutes) {
        // Collect all busy intervals
        List<TimeSlot> busySlots = new ArrayList<>();
        for (String userId : attendees) {
            List<Meeting> meetings = userMeetings.getOrDefault(userId, new ArrayList<>());
            for (Meeting meeting : meetings) {
                TimeSlot slot = meeting.getTimeSlot();
                if (slot.getStartTime().isBefore(searchEnd) && 
                    slot.getEndTime().isAfter(searchStart)) {
                    busySlots.add(slot);
                }
            }
        }
        
        // Sort by start time
        busySlots.sort(Comparator.comparing(TimeSlot::getStartTime));
        
        // Merge overlapping intervals
        List<TimeSlot> mergedBusy = mergeIntervals(busySlots);
        
        // Find free slots
        List<TimeSlot> freeSlots = new ArrayList<>();
        LocalDateTime current = searchStart;
        
        for (TimeSlot busy : mergedBusy) {
            if (current.plusMinutes(durationMinutes).isBefore(busy.getStartTime()) ||
                current.plusMinutes(durationMinutes).isEqual(busy.getStartTime())) {
                freeSlots.add(new TimeSlot(current, busy.getStartTime()));
            }
            current = busy.getEndTime().isAfter(current) ? busy.getEndTime() : current;
        }
        
        // Check remaining time after last meeting
        if (current.plusMinutes(durationMinutes).isBefore(searchEnd) ||
            current.plusMinutes(durationMinutes).isEqual(searchEnd)) {
            freeSlots.add(new TimeSlot(current, searchEnd));
        }
        
        return freeSlots;
    }
    
    /**
     * Merge overlapping intervals
     */
    private List<TimeSlot> mergeIntervals(List<TimeSlot> intervals) {
        if (intervals.isEmpty()) return new ArrayList<>();
        
        List<TimeSlot> merged = new ArrayList<>();
        LocalDateTime currentStart = intervals.get(0).getStartTime();
        LocalDateTime currentEnd = intervals.get(0).getEndTime();
        
        for (int i = 1; i < intervals.size(); i++) {
            TimeSlot slot = intervals.get(i);
            if (slot.getStartTime().isBefore(currentEnd) || 
                slot.getStartTime().isEqual(currentEnd)) {
                // Overlapping, extend the end
                currentEnd = currentEnd.isAfter(slot.getEndTime()) ? 
                           currentEnd : slot.getEndTime();
            } else {
                // Non-overlapping, save current and start new
                merged.add(new TimeSlot(currentStart, currentEnd));
                currentStart = slot.getStartTime();
                currentEnd = slot.getEndTime();
            }
        }
        
        merged.add(new TimeSlot(currentStart, currentEnd));
        return merged;
    }
    
    /**
     * Get user's meetings
     */
    public List<Meeting> getUserMeetings(String userId) {
        return new ArrayList<>(userMeetings.getOrDefault(userId, new ArrayList<>()));
    }
    
    /**
     * Cancel meeting
     */
    public void cancelMeeting(String meetingId) {
        for (List<Meeting> meetings : userMeetings.values()) {
            meetings.removeIf(m -> m.getMeetingId().equals(meetingId));
        }
        System.out.println("✓ Meeting cancelled: " + meetingId);
    }
}

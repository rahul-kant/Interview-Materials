import java.util.*;

/**
 * Meeting entity
 */
public class Meeting {
    private String meetingId;
    private String title;
    private TimeSlot timeSlot;
    private String organizer;
    private List<String> attendees;
    private String location;
    
    public Meeting(String meetingId, String title, TimeSlot timeSlot, 
                  String organizer, List<String> attendees) {
        this.meetingId = meetingId;
        this.title = title;
        this.timeSlot = timeSlot;
        this.organizer = organizer;
        this.attendees = new ArrayList<>(attendees);
        this.location = "";
    }
    
    public String getMeetingId() { return meetingId; }
    public String getTitle() { return title; }
    public TimeSlot getTimeSlot() { return timeSlot; }
    public String getOrganizer() { return organizer; }
    public List<String> getAttendees() { return new ArrayList<>(attendees); }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    @Override
    public String toString() {
        return String.format("Meeting: %s\n  Time: %s\n  Organizer: %s\n  Attendees: %d",
            title, timeSlot, organizer, attendees.size());
    }
}

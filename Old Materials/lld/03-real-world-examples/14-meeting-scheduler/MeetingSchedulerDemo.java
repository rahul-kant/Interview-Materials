import java.time.LocalDateTime;
import java.util.*;

/**
 * Meeting Scheduler Demo
 */
public class MeetingSchedulerDemo {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   MEETING SCHEDULER SYSTEM DEMO          ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
        
        MeetingScheduler scheduler = MeetingScheduler.getInstance();
        
        // Test 1: Basic scheduling
        System.out.println("═══ TEST 1: Basic Meeting Scheduling ═══\n");
        testBasicScheduling(scheduler);
        
        // Test 2: Conflict detection
        System.out.println("\n═══ TEST 2: Conflict Detection ═══\n");
        testConflictDetection(scheduler);
        
        // Test 3: Find common free slots
        System.out.println("\n═══ TEST 3: Find Common Free Slots ═══\n");
        testFindFreeSlots(scheduler);
    }
    
    private static void testBasicScheduling(MeetingScheduler scheduler) {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusMinutes(30);
        TimeSlot slot = new TimeSlot(start, end);
        
        Meeting meeting = scheduler.scheduleMeeting(
            "Team Standup",
            slot,
            "alice",
            Arrays.asList("bob", "charlie")
        );
        
        if (meeting != null) {
            System.out.println(meeting);
        }
    }
    
    private static void testConflictDetection(MeetingScheduler scheduler) {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusMinutes(30);
        TimeSlot conflictSlot = new TimeSlot(start.plusMinutes(15), end.plusMinutes(15));
        
        Meeting meeting = scheduler.scheduleMeeting(
            "Design Review",
            conflictSlot,
            "alice",
            Arrays.asList("bob")
        );
        
        if (meeting == null) {
            System.out.println("✓ Conflict detected correctly");
        }
    }
    
    private static void testFindFreeSlots(MeetingScheduler scheduler) {
        LocalDateTime searchStart = LocalDateTime.now();
        LocalDateTime searchEnd = searchStart.plusHours(8);
        
        List<TimeSlot> freeSlots = scheduler.findCommonFreeSlots(
            Arrays.asList("alice", "bob"),
            searchStart,
            searchEnd,
            30
        );
        
        System.out.println("Free slots for 30-min meeting:");
        for (TimeSlot slot : freeSlots) {
            System.out.println("  " + slot);
        }
    }
}

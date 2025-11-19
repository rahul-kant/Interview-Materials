/**
 * Task Scheduler Demo
 */
public class TaskSchedulerDemo {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║      TASK SCHEDULER DEMO                 ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
        
        TaskScheduler scheduler = new TaskScheduler();
        
        // Add tasks with different priorities
        scheduler.addTask(new Task("T1", "Send Email", 1, 
            () -> System.out.println("  → Email sent successfully")));
        
        scheduler.addTask(new Task("T2", "Process Payment", 10, 
            () -> System.out.println("  → Payment processed")));
        
        scheduler.addTask(new Task("T3", "Generate Report", 5, 
            () -> System.out.println("  → Report generated")));
        
        scheduler.addTask(new Task("T4", "Backup Database", 8, 
            () -> System.out.println("  → Database backed up")));
        
        // Execute tasks (will run in priority order)
        scheduler.executeTasks();
        
        System.out.println("\n💡 Key Concepts:");
        System.out.println("   - Priority Queue (O(log n))");
        System.out.println("   - Command pattern");
        System.out.println("   - Task scheduling algorithms");
        System.out.println("   - Higher priority executes first");
    }
}

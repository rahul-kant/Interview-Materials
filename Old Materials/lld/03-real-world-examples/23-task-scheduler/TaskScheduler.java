import java.util.*;

class Task implements Comparable<Task> {
    String taskId;
    String name;
    int priority;
    Runnable action;
    
    public Task(String taskId, String name, int priority, Runnable action) {
        this.taskId = taskId;
        this.name = name;
        this.priority = priority;
        this.action = action;
    }
    
    @Override
    public int compareTo(Task other) {
        return Integer.compare(other.priority, this.priority); // Higher priority first
    }
}

public class TaskScheduler {
    private PriorityQueue<Task> taskQueue;
    private Set<String> executedTasks;
    
    public TaskScheduler() {
        taskQueue = new PriorityQueue<>();
        executedTasks = new HashSet<>();
    }
    
    public void addTask(Task task) {
        taskQueue.offer(task);
        System.out.println("✓ Task added: " + task.name + " (Priority: " + task.priority + ")");
    }
    
    public void executeTasks() {
        System.out.println("\n=== Executing Tasks ===\n");
        
        while (!taskQueue.isEmpty()) {
            Task task = taskQueue.poll();
            
            if (executedTasks.contains(task.taskId)) {
                continue;
            }
            
            System.out.println("⚙️ Executing: " + task.name + " (Priority: " + task.priority + ")");
            task.action.run();
            executedTasks.add(task.taskId);
        }
        
        System.out.println("\n✓ All tasks completed!");
    }
    
    public int getPendingTaskCount() {
        return taskQueue.size();
    }
}

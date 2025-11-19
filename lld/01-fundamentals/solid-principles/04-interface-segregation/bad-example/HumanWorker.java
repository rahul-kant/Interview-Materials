/**
 * BAD EXAMPLE - Forced to implement fat interface
 * 
 * HumanWorker can do everything, so this works fine.
 * But the problem is visible in RobotWorker!
 */
public class HumanWorker implements Worker {
    
    private String name;
    
    public HumanWorker(String name) {
        this.name = name;
    }
    
    @Override
    public void work() {
        System.out.println(name + " is working on tasks");
    }
    
    @Override
    public void eat() {
        System.out.println(name + " is eating lunch");
    }
    
    @Override
    public void sleep() {
        System.out.println(name + " is sleeping");
    }
    
    @Override
    public void getPaid() {
        System.out.println(name + " received salary");
    }
}

/**
 * BAD EXAMPLE - ISP Violation!
 * 
 * RobotWorker is FORCED to implement eat(), sleep(), and getPaid()
 * even though robots don't do these things!
 * 
 * This is the core problem with fat interfaces.
 */
public class RobotWorker implements Worker {
    
    private String id;
    
    public RobotWorker(String id) {
        this.id = id;
    }
    
    @Override
    public void work() {
        System.out.println("Robot " + id + " is working 24/7");
    }
    
    /**
     * PROBLEM: Robot doesn't eat!
     * Forced to provide empty implementation or throw exception
     */
    @Override
    public void eat() {
        // Empty implementation - waste!
        // Or throw new UnsupportedOperationException("Robots don't eat!");
        System.out.println("Robot " + id + " doesn't eat (empty method)");
    }
    
    /**
     * PROBLEM: Robot doesn't sleep!
     * Forced to provide empty implementation or throw exception
     */
    @Override
    public void sleep() {
        // Empty implementation - waste!
        // Or throw new UnsupportedOperationException("Robots don't sleep!");
        System.out.println("Robot " + id + " doesn't sleep (empty method)");
    }
    
    /**
     * PROBLEM: Robot doesn't get paid!
     * Forced to provide empty implementation or throw exception
     */
    @Override
    public void getPaid() {
        // Empty implementation - waste!
        // Or throw new UnsupportedOperationException("Robots don't get paid!");
        System.out.println("Robot " + id + " doesn't get paid (empty method)");
    }
}

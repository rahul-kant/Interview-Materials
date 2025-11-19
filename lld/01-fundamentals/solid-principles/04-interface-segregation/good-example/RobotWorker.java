/**
 * GOOD EXAMPLE - Follows ISP!
 * 
 * RobotWorker implements ONLY Workable interface.
 * No forced implementations of eat(), sleep(), getPaid()!
 * This is the power of ISP - implement only what you need!
 */
public class RobotWorker implements Workable {
    
    private String id;
    
    public RobotWorker(String id) {
        this.id = id;
    }
    
    @Override
    public void work() {
        System.out.println("Robot " + id + " is working 24/7");
    }
    
    // NO eat() method - robots don't eat!
    // NO sleep() method - robots don't sleep!
    // NO getPaid() method - robots don't get paid!
    // Clean, focused implementation!
}

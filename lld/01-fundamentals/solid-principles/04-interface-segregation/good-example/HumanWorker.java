/**
 * GOOD EXAMPLE - Follows ISP
 * 
 * HumanWorker implements ALL interfaces because humans can do all these things.
 * No forced implementations - only what's needed!
 */
public class HumanWorker implements Workable, Eatable, Sleepable, Payable {
    
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

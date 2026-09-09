import java.util.Random;

/**
 * Dice for Snake and Ladder game
 */
public class Dice {
    private Random random;
    private int sides;
    
    public Dice() {
        this.random = new Random();
        this.sides = 6;
    }
    
    public int roll() {
        return random.nextInt(sides) + 1;
    }
}

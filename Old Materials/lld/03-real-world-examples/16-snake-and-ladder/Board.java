import java.util.*;

/**
 * Board with snakes and ladders
 */
public class Board {
    private int size;
    private Map<Integer, Integer> snakes;
    private Map<Integer, Integer> ladders;
    
    public Board(int size) {
        this.size = size;
        this.snakes = new HashMap<>();
        this.ladders = new HashMap<>();
        initializeBoard();
    }
    
    private void initializeBoard() {
        // Snakes (head -> tail)
        snakes.put(99, 54);
        snakes.put(70, 55);
        snakes.put(52, 42);
        snakes.put(25, 2);
        snakes.put(95, 72);
        
        // Ladders (start -> end)
        ladders.put(6, 25);
        ladders.put(11, 40);
        ladders.put(60, 85);
        ladders.put(46, 90);
        ladders.put(17, 69);
    }
    
    public int getNewPosition(int currentPosition) {
        // Check for snake
        if (snakes.containsKey(currentPosition)) {
            System.out.println("🐍 Snake! Going down from " + currentPosition + " to " + snakes.get(currentPosition));
            return snakes.get(currentPosition);
        }
        
        // Check for ladder
        if (ladders.containsKey(currentPosition)) {
            System.out.println("🪜 Ladder! Climbing up from " + currentPosition + " to " + ladders.get(currentPosition));
            return ladders.get(currentPosition);
        }
        
        return currentPosition;
    }
    
    public int getSize() { return size; }
}

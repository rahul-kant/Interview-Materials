/**
 * Direction Enum - Direction of elevator movement
 * 
 * Interview Key Points:
 * - Simple enum for type safety
 * - Three states: UP, DOWN, IDLE
 */
public enum Direction {
    UP,
    DOWN,
    IDLE;
    
    @Override
    public String toString() {
        switch (this) {
            case UP:
                return "↑ UP";
            case DOWN:
                return "↓ DOWN";
            case IDLE:
                return "⊙ IDLE";
            default:
                return name();
        }
    }
}

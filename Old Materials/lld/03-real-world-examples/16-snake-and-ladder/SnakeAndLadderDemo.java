/**
 * Snake and Ladder Game Demo
 */
public class SnakeAndLadderDemo {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   SNAKE AND LADDER GAME DEMO             ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
        
        // Create game
        SnakeAndLadderGame game = new SnakeAndLadderGame(100);
        
        // Add players
        game.addPlayer(new Player("Alice"));
        game.addPlayer(new Player("Bob"));
        game.addPlayer(new Player("Charlie"));
        
        // Play game
        game.play();
        
        System.out.println("\n💡 Key Concepts:");
        System.out.println("   - Turn-based gameplay");
        System.out.println("   - Random dice rolling");
        System.out.println("   - Snake and ladder logic");
        System.out.println("   - Win condition (exact 100)");
    }
}

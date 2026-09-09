/**
 * Chess Game Demo
 */
public class ChessDemo {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║         CHESS GAME DEMO                  ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
        
        ChessGame game = new ChessGame();
        game.start();
        
        // Make some moves
        System.out.println("\n=== Making Moves ===");
        game.makeMove("e2", "e4");
        game.makeMove("e7", "e5");
        
        System.out.println("\n💡 Key Concepts:");
        System.out.println("   - Piece movement validation");
        System.out.println("   - Turn-based gameplay");
        System.out.println("   - Board state management");
        System.out.println("   - Strategy pattern for pieces");
    }
}

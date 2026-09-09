/**
 * Tic-Tac-Toe Demo
 */
public class TicTacToeDemo {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║      TIC-TAC-TOE GAME DEMO               ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
        
        TicTacToeGame game = new TicTacToeGame();
        
        // Simulate a game
        int[][] moves = {
            {0, 0}, // X
            {1, 1}, // O
            {0, 1}, // X
            {2, 2}, // O
            {0, 2}, // X wins
        };
        
        for (int[] move : moves) {
            int row = move[0];
            int col = move[1];
            
            System.out.println("Player " + game.getCurrentPlayer() + " moves to (" + row + ", " + col + ")");
            game.makeMove(row, col);
            game.printBoard();
            
            if (game.isGameOver()) {
                break;
            }
        }
        
        System.out.println("💡 Key Concepts:");
        System.out.println("   - Win detection (rows, columns, diagonals)");
        System.out.println("   - Draw detection");
        System.out.println("   - Move validation");
        System.out.println("   - Turn alternation");
    }
}

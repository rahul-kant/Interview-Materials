import java.util.*;

/**
 * Snake and Ladder Game Controller
 */
public class SnakeAndLadderGame {
    private Board board;
    private Dice dice;
    private Queue<Player> players;
    private Player winner;
    
    public SnakeAndLadderGame(int boardSize) {
        this.board = new Board(boardSize);
        this.dice = new Dice();
        this.players = new LinkedList<>();
        this.winner = null;
    }
    
    public void addPlayer(Player player) {
        players.add(player);
    }
    
    public void play() {
        System.out.println("🎲 Game Started! Board size: " + board.getSize());
        System.out.println("Players: " + players.size() + "\n");
        
        while (winner == null) {
            Player currentPlayer = players.poll();
            playTurn(currentPlayer);
            
            if (winner == null) {
                players.add(currentPlayer);
            }
        }
        
        System.out.println("\n🎉 " + winner.getName() + " WINS! 🎉");
    }
    
    private void playTurn(Player player) {
        System.out.println("\n--- " + player.getName() + "'s turn ---");
        System.out.println("Current position: " + player.getPosition());
        
        int diceValue = dice.roll();
        System.out.println("Dice rolled: " + diceValue);
        
        int newPosition = player.getPosition() + diceValue;
        
        // Check if exceeds board size
        if (newPosition > board.getSize()) {
            System.out.println("Cannot move! Need exact " + (board.getSize() - player.getPosition()));
            return;
        }
        
        // Check for snake or ladder
        newPosition = board.getNewPosition(newPosition);
        player.setPosition(newPosition);
        
        System.out.println("New position: " + newPosition);
        
        // Check win condition
        if (newPosition == board.getSize()) {
            winner = player;
        }
    }
    
    public Player getWinner() {
        return winner;
    }
}

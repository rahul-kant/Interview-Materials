/**
 * Simplified Chess Game Implementation
 * Focus on core concepts for interview
 */

enum PieceColor { WHITE, BLACK }
enum PieceType { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }

class ChessPiece {
    PieceType type;
    PieceColor color;
    
    public ChessPiece(PieceType type, PieceColor color) {
        this.type = type;
        this.color = color;
    }
    
    public String getSymbol() {
        String symbol = type.toString().substring(0, 1);
        return color == PieceColor.WHITE ? symbol.toUpperCase() : symbol.toLowerCase();
    }
}

class ChessBoard {
    private ChessPiece[][] board;
    
    public ChessBoard() {
        board = new ChessPiece[8][8];
        initializeBoard();
    }
    
    private void initializeBoard() {
        // White pieces
        board[0][0] = new ChessPiece(PieceType.ROOK, PieceColor.WHITE);
        board[0][1] = new ChessPiece(PieceType.KNIGHT, PieceColor.WHITE);
        board[0][2] = new ChessPiece(PieceType.BISHOP, PieceColor.WHITE);
        board[0][3] = new ChessPiece(PieceType.QUEEN, PieceColor.WHITE);
        board[0][4] = new ChessPiece(PieceType.KING, PieceColor.WHITE);
        board[0][5] = new ChessPiece(PieceType.BISHOP, PieceColor.WHITE);
        board[0][6] = new ChessPiece(PieceType.KNIGHT, PieceColor.WHITE);
        board[0][7] = new ChessPiece(PieceType.ROOK, PieceColor.WHITE);
        
        for (int i = 0; i < 8; i++) {
            board[1][i] = new ChessPiece(PieceType.PAWN, PieceColor.WHITE);
            board[6][i] = new ChessPiece(PieceType.PAWN, PieceColor.BLACK);
        }
        
        // Black pieces
        board[7][0] = new ChessPiece(PieceType.ROOK, PieceColor.BLACK);
        board[7][1] = new ChessPiece(PieceType.KNIGHT, PieceColor.BLACK);
        board[7][2] = new ChessPiece(PieceType.BISHOP, PieceColor.BLACK);
        board[7][3] = new ChessPiece(PieceType.QUEEN, PieceColor.BLACK);
        board[7][4] = new ChessPiece(PieceType.KING, PieceColor.BLACK);
        board[7][5] = new ChessPiece(PieceType.BISHOP, PieceColor.BLACK);
        board[7][6] = new ChessPiece(PieceType.KNIGHT, PieceColor.BLACK);
        board[7][7] = new ChessPiece(PieceType.ROOK, PieceColor.BLACK);
    }
    
    public void displayBoard() {
        System.out.println("\n  a b c d e f g h");
        for (int row = 7; row >= 0; row--) {
            System.out.print((row + 1) + " ");
            for (int col = 0; col < 8; col++) {
                if (board[row][col] != null) {
                    System.out.print(board[row][col].getSymbol() + " ");
                } else {
                    System.out.print(". ");
                }
            }
            System.out.println();
        }
    }
    
    public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        if (!isValidPosition(fromRow, fromCol) || !isValidPosition(toRow, toCol)) {
            return false;
        }
        
        ChessPiece piece = board[fromRow][fromCol];
        if (piece == null) {
            return false;
        }
        
        // Basic move validation (simplified)
        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = null;
        return true;
    }
    
    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }
}

public class ChessGame {
    private ChessBoard board;
    private PieceColor currentPlayer;
    
    public ChessGame() {
        board = new ChessBoard();
        currentPlayer = PieceColor.WHITE;
    }
    
    public void start() {
        System.out.println("Chess Game Started!");
        board.displayBoard();
    }
    
    public void makeMove(String from, String to) {
        // Simple format: e2 to e4
        int[] fromPos = parsePosition(from);
        int[] toPos = parsePosition(to);
        
        if (board.movePiece(fromPos[0], fromPos[1], toPos[0], toPos[1])) {
            System.out.println("\n" + currentPlayer + " moved from " + from + " to " + to);
            currentPlayer = (currentPlayer == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
            board.displayBoard();
        } else {
            System.out.println("Invalid move!");
        }
    }
    
    private int[] parsePosition(String pos) {
        int col = pos.charAt(0) - 'a';
        int row = pos.charAt(1) - '1';
        return new int[]{row, col};
    }
}

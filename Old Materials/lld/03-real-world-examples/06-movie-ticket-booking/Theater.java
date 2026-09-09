import java.util.*;

/**
 * Theater Class - Represents a movie theater/screen
 * 
 * Interview Key Points:
 * - Theater capacity and layout
 * - Multiple seat types
 * - Reusable for multiple shows
 */
public class Theater {
    private final String id;
    private final String name;
    private final int rows;
    private final int columns;
    private final List<Seat> seatLayout;
    
    public Theater(String id, String name, int rows, int columns) {
        this.id = id;
        this.name = name;
        this.rows = rows;
        this.columns = columns;
        this.seatLayout = new ArrayList<>();
        
        initializeSeats();
    }
    
    /**
     * Initialize seat layout
     * First 2 rows: VIP
     * Next 3 rows: Premium
     * Rest: Regular
     */
    private void initializeSeats() {
        for (int row = 0; row < rows; row++) {
            Seat.SeatType type;
            
            if (row < 2) {
                type = Seat.SeatType.VIP;
            } else if (row < 5) {
                type = Seat.SeatType.PREMIUM;
            } else {
                type = Seat.SeatType.REGULAR;
            }
            
            for (int col = 0; col < columns; col++) {
                seatLayout.add(new Seat(row, col, type));
            }
        }
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public int getRows() {
        return rows;
    }
    
    public int getColumns() {
        return columns;
    }
    
    public int getCapacity() {
        return rows * columns;
    }
    
    public List<Seat> getAllSeats() {
        return new ArrayList<>(seatLayout);
    }
    
    @Override
    public String toString() {
        return String.format("Theater: %s (%d seats, %dx%d)", 
            name, getCapacity(), rows, columns);
    }
}

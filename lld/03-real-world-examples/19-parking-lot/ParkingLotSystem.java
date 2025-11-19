import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

enum VehicleType { BIKE, CAR, TRUCK }
enum SpotType { COMPACT, LARGE, HANDICAPPED }

class Vehicle {
    String licensePlate;
    VehicleType type;
    
    public Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }
}

class ParkingSpot {
    int spotNumber;
    SpotType type;
    boolean isOccupied;
    Vehicle vehicle;
    
    public ParkingSpot(int spotNumber, SpotType type) {
        this.spotNumber = spotNumber;
        this.type = type;
        this.isOccupied = false;
    }
    
    public boolean canFitVehicle(VehicleType vehicleType) {
        if (vehicleType == VehicleType.TRUCK) {
            return type == SpotType.LARGE;
        }
        return true; // Bike and Car can fit in any spot
    }
}

class ParkingTicket {
    String ticketId;
    Vehicle vehicle;
    ParkingSpot spot;
    LocalDateTime entryTime;
    
    public ParkingTicket(String ticketId, Vehicle vehicle, ParkingSpot spot) {
        this.ticketId = ticketId;
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTime = LocalDateTime.now();
    }
}

public class ParkingLotSystem {
    private static ParkingLotSystem instance;
    private List<ParkingSpot> spots;
    private Map<String, ParkingTicket> tickets;
    private int ticketCounter;
    private static final double HOURLY_RATE = 10.0;
    
    private ParkingLotSystem(int capacity) {
        spots = new ArrayList<>();
        tickets = new HashMap<>();
        ticketCounter = 1;
        initializeSpots(capacity);
    }
    
    public static synchronized ParkingLotSystem getInstance(int capacity) {
        if (instance == null) {
            instance = new ParkingLotSystem(capacity);
        }
        return instance;
    }
    
    private void initializeSpots(int capacity) {
        for (int i = 1; i <= capacity; i++) {
            SpotType type;
            if (i <= capacity * 0.3) {
                type = SpotType.COMPACT;
            } else if (i <= capacity * 0.9) {
                type = SpotType.LARGE;
            } else {
                type = SpotType.HANDICAPPED;
            }
            spots.add(new ParkingSpot(i, type));
        }
    }
    
    public synchronized ParkingTicket parkVehicle(Vehicle vehicle) {
        ParkingSpot availableSpot = findAvailableSpot(vehicle.type);
        
        if (availableSpot == null) {
            System.out.println("❌ No available spot for " + vehicle.type);
            return null;
        }
        
        availableSpot.isOccupied = true;
        availableSpot.vehicle = vehicle;
        
        String ticketId = "T" + ticketCounter++;
        ParkingTicket ticket = new ParkingTicket(ticketId, vehicle, availableSpot);
        tickets.put(ticketId, ticket);
        
        System.out.println("✓ Vehicle " + vehicle.licensePlate + " parked at spot " + 
            availableSpot.spotNumber + " (" + availableSpot.type + ")");
        return ticket;
    }
    
    private ParkingSpot findAvailableSpot(VehicleType vehicleType) {
        for (ParkingSpot spot : spots) {
            if (!spot.isOccupied && spot.canFitVehicle(vehicleType)) {
                return spot;
            }
        }
        return null;
    }
    
    public synchronized double unparkVehicle(String ticketId) {
        ParkingTicket ticket = tickets.get(ticketId);
        
        if (ticket == null) {
            System.out.println("❌ Invalid ticket");
            return -1;
        }
        
        LocalDateTime exitTime = LocalDateTime.now();
        long hours = ChronoUnit.HOURS.between(ticket.entryTime, exitTime);
        if (hours == 0) hours = 1; // Minimum 1 hour
        
        double fee = hours * HOURLY_RATE;
        
        ticket.spot.isOccupied = false;
        ticket.spot.vehicle = null;
        tickets.remove(ticketId);
        
        System.out.println("✓ Vehicle " + ticket.vehicle.licensePlate + " exited");
        System.out.println("  Duration: " + hours + " hours");
        System.out.println("  Fee: $" + fee);
        
        return fee;
    }
    
    public void displayAvailability() {
        Map<SpotType, Integer> available = new HashMap<>();
        available.put(SpotType.COMPACT, 0);
        available.put(SpotType.LARGE, 0);
        available.put(SpotType.HANDICAPPED, 0);
        
        for (ParkingSpot spot : spots) {
            if (!spot.isOccupied) {
                available.put(spot.type, available.get(spot.type) + 1);
            }
        }
        
        System.out.println("\n=== Parking Availability ===");
        for (SpotType type : SpotType.values()) {
            System.out.println(type + ": " + available.get(type) + " spots");
        }
    }
}

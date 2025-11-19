package ride_sharing;

/**
 * Vehicle - Represents a driver's vehicle
 * 
 * Interview Key Points:
 * - Vehicle types
 * - Vehicle registration
 * - Seat capacity
 */
public class Vehicle {
    private final String vehicleId;
    private final String registrationNumber;
    private final VehicleType type;
    private final String model;
    private final int year;
    private final int capacity;
    private boolean active;
    
    public enum VehicleType {
        COMPACT(4, "UberX"),     // Regular car (4 seats)
        SEDAN(4, "UberX"),      // Premium car (4 seats)
        SUV(6, "UberXL"),       // SUV (6 seats)
        VAN(8, "UberXL");       // Van (8 seats)
        
        private final int capacity;
        private final String rideType;
        
        VehicleType(int capacity, String rideType) {
            this.capacity = capacity;
            this.rideType = rideType;
        }
        
        public int getCapacity() {
            return capacity;
        }
        
        public String getRideType() {
            return rideType;
        }
    }
    
    public Vehicle(String vehicleId, String registrationNumber, VehicleType type, 
                  String model, int year) {
        this.vehicleId = vehicleId;
        this.registrationNumber = registrationNumber;
        this.type = type;
        this.model = model;
        this.year = year;
        this.capacity = type.getCapacity();
        this.active = true;
    }
    
    public String getVehicleId() {
        return vehicleId;
    }
    
    public String getRegistrationNumber() {
        return registrationNumber;
    }
    
    public VehicleType getType() {
        return type;
    }
    
    public String getModel() {
        return model;
    }
    
    public int getYear() {
        return year;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    @Override
    public String toString() {
        return String.format("%s %s (%s) - %s", year, model, type, registrationNumber);
    }
}
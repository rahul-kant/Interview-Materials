/**
 * Parking Lot Demo
 */
public class ParkingLotDemo {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║      PARKING LOT SYSTEM DEMO             ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
        
        ParkingLotSystem parkingLot = ParkingLotSystem.getInstance(10);
        
        // Test 1: Park different vehicles
        System.out.println("=== TEST 1: Park Vehicles ===\n");
        Vehicle bike = new Vehicle("BIKE-001", VehicleType.BIKE);
        Vehicle car = new Vehicle("CAR-001", VehicleType.CAR);
        Vehicle truck = new Vehicle("TRUCK-001", VehicleType.TRUCK);
        
        ParkingTicket t1 = parkingLot.parkVehicle(bike);
        ParkingTicket t2 = parkingLot.parkVehicle(car);
        ParkingTicket t3 = parkingLot.parkVehicle(truck);
        
        // Test 2: Check availability
        System.out.println();
        parkingLot.displayAvailability();
        
        // Test 3: Unpark and calculate fee
        System.out.println("\n=== TEST 2: Unpark Vehicle ===\n");
        if (t1 != null) {
            parkingLot.unparkVehicle(t1.ticketId);
        }
        
        parkingLot.displayAvailability();
        
        System.out.println("\n💡 Key Concepts:");
        System.out.println("   - Multiple vehicle types");
        System.out.println("   - Spot allocation strategy");
        System.out.println("   - Fee calculation");
        System.out.println("   - Thread-safe operations");
    }
}

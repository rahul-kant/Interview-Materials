# Parking Lot System - Low-Level Design

## 📖 Problem Statement

Design a parking lot system that can:
1. Have multiple floors
2. Support different vehicle types (motorcycle, car, truck)
3. Have different parking spot sizes (small, medium, large)
4. Track available spots
5. Assign spots to vehicles
6. Calculate parking fees
7. Handle entry and exit

## 🎯 Learning Objectives

After completing this example, you will understand:
- How to identify entities and relationships
- How to apply SOLID principles in real systems
- How to handle different types through polymorphism
- How to manage system state
- How to design for extensibility

## 📋 Requirements Analysis

### Functional Requirements:
1. **Multiple Floors** - Parking lot can have multiple floors
2. **Different Spot Types** - Small, Medium, Large spots
3. **Vehicle Types** - Motorcycle, Car, Truck
4. **Parking Rules**:
   - Motorcycle can park in any spot
   - Car can park in Medium or Large spots
   - Truck can only park in Large spots
5. **Entry/Exit** - Track when vehicles enter and exit
6. **Fee Calculation** - Calculate fees based on time parked
7. **Spot Assignment** - Automatically find available spot
8. **Display Status** - Show available spots per floor

### Non-Functional Requirements:
1. System should be extendable (new vehicle types, new spot types)
2. Thread-safe operations (for multiple entry/exit points)
3. Efficient spot finding
4. Clear error handling

## 🏗️ Design Approach

### Step 1: Identify Core Entities

1. **ParkingLot** - The main system
2. **Floor** - A floor in the parking lot
3. **ParkingSpot** - A parking spot
4. **Vehicle** - A vehicle (abstract)
5. **Ticket** - Entry ticket
6. **Payment** - Payment information

### Step 2: Identify Relationships

```
ParkingLot (1) -----> (many) Floor
Floor (1) -----> (many) ParkingSpot
ParkingSpot (1) -----> (0..1) Vehicle
Vehicle (1) -----> (1) Ticket
```

### Step 3: Apply Design Principles

#### Single Responsibility Principle (SRP):
- `ParkingLot` - Manages floors and overall operations
- `Floor` - Manages spots on that floor
- `ParkingSpot` - Manages single spot state
- `Vehicle` - Represents vehicle data
- `FeeCalculator` - Calculates parking fees

#### Open/Closed Principle (OCP):
- Use inheritance for different vehicle types
- Use enums for spot types
- Easy to add new vehicle types without modifying existing code

#### Liskov Substitution Principle (LSP):
- All vehicle subclasses can be used interchangeably
- Polymorphism for vehicle operations

## 📊 Class Diagram

```
                    ParkingLot
                        |
                    manages
                        |
                      Floor
                        |
                    contains
                        |
                   ParkingSpot
                        |
                    occupied by
                        |
                     Vehicle
                   /    |    \
                  /     |     \
            Motorcycle  Car  Truck

           FeeCalculator
                |
           calculates fee for
                |
              Ticket
```

## 🎨 Design Decisions

### 1. Vehicle Hierarchy
**Decision:** Use abstract `Vehicle` class with concrete implementations
**Reason:** Follows OCP - can add new vehicle types without modifying existing code

### 2. Spot Size Enum
**Decision:** Use enum for spot sizes instead of inheritance
**Reason:** Spot sizes are fixed, behavior doesn't vary significantly

### 3. Separate FeeCalculator
**Decision:** Extract fee calculation to separate class
**Reason:** Follows SRP - parking lot focuses on parking, calculator on fees

### 4. Ticket as Proof of Parking
**Decision:** Issue ticket on entry, collect on exit
**Reason:** Real-world parking lot behavior, tracks entry time

## 💻 Implementation

See the following files:
1. `Vehicle.java` - Abstract vehicle and concrete implementations
2. `ParkingSpot.java` - Parking spot with size types
3. `Floor.java` - Floor management
4. `Ticket.java` - Entry ticket
5. `FeeCalculator.java` - Fee calculation logic
6. `ParkingLot.java` - Main parking lot system
7. `ParkingLotDemo.java` - Demo showing all features

## 🔍 Key Design Patterns Used

### 1. Factory Pattern (implicit)
When finding available spots, system acts like a factory

### 2. Strategy Pattern (in fee calculation)
Different fee calculation strategies can be applied

### 3. Singleton Pattern (could be applied)
ParkingLot could be singleton (one parking lot instance)

## 🎓 Interview Discussion Points

### Question 1: "How would you handle multiple entry/exit points?"
**Answer:** 
- Make operations synchronized for thread safety
- Use AtomicInteger for spot counting
- Consider using locks for critical sections

### Question 2: "How would you add a new vehicle type (e.g., Bus)?"
**Answer:**
```java
public class Bus extends Vehicle {
    public Bus(String licensePlate) {
        super(licensePlate, VehicleType.BUS);
    }
    
    @Override
    public boolean canFitInSpot(SpotSize size) {
        return size == SpotSize.EXTRA_LARGE;
    }
}
```
Add EXTRA_LARGE to SpotSize enum. No changes to existing code!

### Question 3: "How would you optimize spot finding?"
**Answer:**
- Maintain available spot count per floor/type
- Use priority queue for floor selection
- Cache frequently accessed data
- Index spots by type

### Question 4: "How would you handle reservations?"
**Answer:**
- Add `reservedUntil` field to ParkingSpot
- Add reservation ID to Ticket
- Check reservations before assigning spots

### Question 5: "How would you scale this to multiple parking lots?"
**Answer:**
- Add ParkingLotManager class
- Each ParkingLot has unique ID
- Centralized system to manage multiple lots
- API-based communication between lots

## ✅ Extension Ideas

Try implementing these on your own:
1. **Reservation System** - Pre-book parking spots
2. **Handicap Spots** - Special spots near entrance
3. **Monthly Passes** - Fixed fee for regular users
4. **Dynamic Pricing** - Higher fees during peak hours
5. **Multiple Entry/Exit Points** - Thread-safe operations
6. **Parking Spot Sensors** - Real-time spot availability
7. **Payment Gateway Integration** - Multiple payment methods
8. **Admin Dashboard** - View statistics and manage system

## 🔑 Key Takeaways

✅ **Start with requirements** - Understand before designing
✅ **Identify entities** - Find nouns in requirements
✅ **Define relationships** - How entities interact
✅ **Apply SOLID principles** - Makes code maintainable
✅ **Think about extensibility** - Future requirements
✅ **Handle edge cases** - Error conditions
✅ **Use appropriate patterns** - Don't over-engineer

## 📝 Practice Exercise

Implement these additional features:
1. VIP parking spots (higher fees, better location)
2. Electric vehicle charging spots
3. Parking history for a vehicle
4. Spot reservation cancellation
5. Discounts for longer stays

## ➡️ Next Steps

1. Study each class implementation
2. Run the demo to see it in action
3. Try implementing the extension ideas
4. Move to the next LLD problem

---

**Remember**: This is a classic LLD interview question. Understanding this design will help you tackle similar problems!

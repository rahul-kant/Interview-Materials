/**
 * Coin Enum - Represents different coin denominations
 * 
 * Interview Key Points:
 * - Why enum? Type-safety and fixed denominations
 * - Could extend to Note enum for larger denominations
 * - getValue() provides monetary value
 */
public enum Coin {
    PENNY(1),
    NICKEL(5),
    DIME(10),
    QUARTER(25),
    DOLLAR(100);
    
    private final int value; // Value in cents
    
    Coin(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    /**
     * Get coin by value
     * Useful for change calculation
     */
    public static Coin getByValue(int value) {
        for (Coin coin : Coin.values()) {
            if (coin.value == value) {
                return coin;
            }
        }
        throw new IllegalArgumentException("No coin with value: " + value);
    }
}

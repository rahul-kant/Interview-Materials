/**
 * Base62 Encoder - Converts numbers to Base62 strings
 * 
 * EXPLANATION:
 * - Base62 uses: 0-9, a-z, A-Z (62 characters)
 * - Used for creating short, readable URLs
 * - No special characters (URL-safe)
 * 
 * INTERVIEW POINTS:
 * - Why Base62? URL-safe, case-sensitive, compact
 * - Alternative: Base64 (but has +, / which need encoding)
 * - 6 chars = 62^6 = 56 billion unique URLs
 */
public class Base62Encoder {
    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;
    
    /**
     * Encode number to Base62 string
     * 
     * @param num Number to encode
     * @return Base62 string
     */
    public static String encode(long num) {
        if (num == 0) {
            return "0";
        }
        
        StringBuilder sb = new StringBuilder();
        
        while (num > 0) {
            int remainder = (int) (num % BASE);
            sb.append(BASE62.charAt(remainder));
            num = num / BASE;
        }
        
        return sb.reverse().toString();
    }
    
    /**
     * Decode Base62 string to number
     * 
     * @param str Base62 string
     * @return Decoded number
     */
    public static long decode(String str) {
        long num = 0;
        
        for (char c : str.toCharArray()) {
            num = num * BASE + BASE62.indexOf(c);
        }
        
        return num;
    }
    
    /**
     * Generate hash-based short code
     * Uses hashCode to generate deterministic short URL
     */
    public static String generateShortCode(String longUrl, int length) {
        long hash = Math.abs(longUrl.hashCode());
        String encoded = encode(hash);
        
        // Ensure minimum length
        if (encoded.length() < length) {
            encoded = String.format("%0" + length + "d", 0) + encoded;
            encoded = encoded.substring(encoded.length() - length);
        } else if (encoded.length() > length) {
            encoded = encoded.substring(0, length);
        }
        
        return encoded;
    }
}

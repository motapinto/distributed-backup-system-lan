package Common;

import java.security.MessageDigest;
import static Common.Logs.logError;

public class Utilities {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String hashAndEncode(String str) {
        MessageDigest digest;
        String result = null;

        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(str.getBytes("UTF-8"));
            result = bytesToHex(hash);
        } catch (Exception e) {
            logError("Hash algorithm not found: " + e.getMessage());
            return null;
        }

        return result;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}


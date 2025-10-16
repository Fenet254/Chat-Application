public class EncryptionUtil {
    private static final String KEY = "ChatAppSecretKey123"; // Simple key for XOR encryption

    public static String encrypt(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            result.append((char) (text.charAt(i) ^ KEY.charAt(i % KEY.length())));
        }
        return result.toString();
    }

    public static String decrypt(String encryptedText) {
        // XOR is symmetric, so decryption is the same as encryption
        return encrypt(encryptedText);
    }
}

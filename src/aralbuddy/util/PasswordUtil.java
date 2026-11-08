package aralbuddy.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Stores salted password hashes using only the Java security library. */
public final class PasswordUtil {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() { }

    /** Creates a different salted hash even when two passwords are equal. */
    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    /** Compares a password with its stored hash without an early byte comparison. */
    public static boolean matches(String password, String encoded) {
        if (password == null || encoded == null) return false;
        try {
            String[] fields = encoded.split(":");
            int iterations = Integer.parseInt(fields[0]);
            if (fields.length != 3 || iterations != ITERATIONS) return false;
            return MessageDigest.isEqual(Base64.getDecoder().decode(fields[2]),
                    derive(password, Base64.getDecoder().decode(fields[1]), iterations));
        } catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        char[] characters = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(characters, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password security is unavailable in this Java installation.", exception);
        } finally {
            spec.clearPassword();
            Arrays.fill(characters, '\0');
        }
    }
}

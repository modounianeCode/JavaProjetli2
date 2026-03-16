package univscheduler.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitaire pour le hachage des mots de passe (SHA-256).
 */
public class PasswordUtil {

    private PasswordUtil() {}

    /**
     * Hache un mot de passe avec SHA-256.
     * @param password Le mot de passe en clair
     * @return Le hash hexadécimal du mot de passe
     */
    public static String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithme SHA-256 non disponible", e);
        }
    }

    /**
     * Vérifie si un mot de passe correspond à un hash.
     * @param password Le mot de passe en clair
     * @param hashedPassword Le hash à comparer
     * @return true si le mot de passe correspond
     */
    public static boolean verify(String password, String hashedPassword) {
        return hash(password).equals(hashedPassword);
    }
}

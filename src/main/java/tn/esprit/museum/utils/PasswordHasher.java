package tn.esprit.museum.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {

    /**
     * Hash un mot de passe avec BCrypt
     * @param password Mot de passe en clair
     * @return Mot de passe hashé
     */
    public static String hashPasswordBCrypt(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide");
        }
        // gensalt(12) = 2^12 rounds (recommandé pour un bon équilibre sécurité/performance)
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    /**
     * Vérifie si un mot de passe correspond à un hash BCrypt
     * @param plainPassword Mot de passe en clair
     * @param hashedPassword Hash stocké
     * @return true si correspond
     */
    public static boolean verifyPasswordBCrypt(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isBCryptHash(String password) {
        return password != null
                && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }
}

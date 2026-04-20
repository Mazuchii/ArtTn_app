package tn.esprit.produit.utils;

import tn.esprit.produit.entities.User;

public class SessionManager {
    private static User currentUser;

    // Utilisateur par défaut pour les tests
    static {
        // Créer un utilisateur admin par défaut
        currentUser = new User(1, "admin", "admin@museum.com", "Administrateur", "ADMIN");
        System.out.println("✅ SessionManager: Utilisateur par défaut créé (ID: 1)");
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void login(User user) {
        currentUser = user;
        System.out.println("✅ Utilisateur connecté: " + user.getFullName() + " (ID: " + user.getId() + ")");
    }

    public static int getCurrentUserId() {
        if (currentUser == null) {
            System.out.println("⚠️ SessionManager: currentUser est null, création d'un utilisateur par défaut");
            currentUser = new User(1, "admin", "admin@museum.com", "Administrateur", "ADMIN");
        }
        System.out.println("✅ SessionManager: getCurrentUserId() retourne " + currentUser.getId());
        return currentUser.getId();
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void logout() {
        currentUser = null;
        System.out.println("👋 Utilisateur déconnecté");
    }
}
package tn.esprit.museum.utils;

import tn.esprit.museum.entities.User;

public class SessionManager {
    private static User currentUser = null;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
        if (user != null) {
            System.out.println("✅ SessionManager: Utilisateur défini - " + user.getFullName() + " (ID: " + user.getId() + ")");
        }
    }

    public static void login(User user) {
        currentUser = user;
        if (user != null) {
            System.out.println("✅ Utilisateur connecté: " + user.getFullName() + " (ID: " + user.getId() + ", Email: " + user.getEmail() + ")");
        } else {
            System.out.println("❌ Tentative de connexion avec user null");
        }
    }

    public static int getCurrentUserId() {
        if (currentUser == null) {
            System.err.println("❌ ERREUR: Aucun utilisateur connecté !");
            return -1;
        }
        return currentUser.getId();
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static String getCurrentUserEmail() {
        if (currentUser == null) {
            System.err.println("❌ ERREUR: Aucun utilisateur connecté !");
            return null;
        }
        return currentUser.getEmail();
    }

    public static String getCurrentUserName() {
        if (currentUser == null) {
            System.err.println("❌ ERREUR: Aucun utilisateur connecté !");
            return "Invité";
        }
        return currentUser.getFullName();
    }

    public static void logout() {
        System.out.println("👋 Déconnexion de: " + (currentUser != null ? currentUser.getFullName() : "inconnu"));
        currentUser = null;
    }
}
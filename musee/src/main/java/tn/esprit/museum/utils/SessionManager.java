package tn.esprit.museum.utils;

import tn.esprit.museum.entities.User;

public class SessionManager {
    private static User currentUser = null;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }
    public static void logout() { currentUser = null; }
    public static boolean isLoggedIn() { return currentUser != null; }
    public static boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }
}
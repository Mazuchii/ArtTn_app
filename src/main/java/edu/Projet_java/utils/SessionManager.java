package edu.Projet_java.utils;

public class SessionManager {

    private static int currentUserId = 1;  // ID par défaut (admin)
    private static String currentUserName = "Administrateur";
    private static boolean isLoggedIn = true;

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static void setCurrentUserId(int userId) {
        currentUserId = userId;
    }

    public static String getCurrentUserName() {
        return currentUserName;
    }

    public static void setCurrentUserName(String userName) {
        currentUserName = userName;
    }

    public static boolean isLoggedIn() {
        return isLoggedIn;
    }

    public static void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public static void logout() {
        currentUserId = 1;
        currentUserName = "Administrateur";
        isLoggedIn = false;
    }
}
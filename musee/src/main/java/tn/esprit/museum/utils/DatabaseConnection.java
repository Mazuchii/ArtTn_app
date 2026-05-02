package tn.esprit.museum.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Paramètres de connexion – ajustez si nécessaire
    private static final String HOST = "localhost";   // ou "127.0.0.1" si localhost ne fonctionne pas
    private static final String PORT = "3306";
    private static final String DB_NAME = "esprit_museum";
    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME + "?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "";   // laissez vide si pas de mot de passe

    // Test de la connexion au chargement de la classe (optionnel)
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver MySQL introuvable. Vérifiez la dépendance dans pom.xml");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connexion MySQL réussie !");
            return conn;
        } catch (SQLException e) {
            System.err.println("❌ Échec de connexion à MySQL :");
            System.err.println("   → Vérifiez que MySQL est démarré (XAMPP/WAMP/service)");
            System.err.println("   → Vérifiez l'URL : " + URL);
            System.err.println("   → Vérifiez utilisateur/mot de passe");
            System.err.println("   → Cause : " + e.getMessage());
            throw e;  // propage l'exception pour que l'appelant la traite
        }
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("🔒 Connexion fermée.");
            } catch (SQLException e) {
                System.err.println("Erreur lors de la fermeture : " + e.getMessage());
            }
        }
    }
}
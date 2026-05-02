package tn.esprit.museum;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.utils.SessionManager;

import java.net.URL;

public class MuseumApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        URL fxmlUrl = getClass().getResource("/fxml/login.fxml");

        if (fxmlUrl == null) {
            System.err.println("ERREUR: login.fxml non trouve !");
            System.err.println("Verifiez que le fichier est dans: src/main/resources/fxml/");
            return;
        }

        Parent root = FXMLLoader.load(fxmlUrl);

        primaryStage.setTitle("Museum Digital - Connexion");
        primaryStage.setScene(new Scene(root));
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public void startAdminDashboard(Stage primaryStage) throws Exception {
        User admin = new User(1, "admin", "admin@museum.com", "Administrateur", "ADMIN");
        SessionManager.login(admin);

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboardproduit.fxml"));
        primaryStage.setTitle("Administration - Museum Digital");
        primaryStage.setScene(new Scene(root));
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public void startBoutique(Stage primaryStage) throws Exception {
        User defaultUser = new User(1, "admin", "admin@museum.com", "Administrateur", "ADMIN");
        SessionManager.login(defaultUser);

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/products_view.fxml"));
        primaryStage.setTitle("Museum Digital - Boutique");
        primaryStage.setScene(new Scene(root));
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }
}

package tn.esprit.museum;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Méthode plus robuste avec vérification
        URL fxmlUrl = getClass().getResource("/fxml/login.fxml");

        if (fxmlUrl == null) {
            System.err.println("ERREUR: login.fxml non trouvé !");
            System.err.println("Vérifiez que le fichier est dans: src/main/resources/fxml/");
            return;
        }

        Parent root = FXMLLoader.load(fxmlUrl);

        primaryStage.setTitle("Museum Digital - Connexion");
        primaryStage.setScene(new Scene(root));
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
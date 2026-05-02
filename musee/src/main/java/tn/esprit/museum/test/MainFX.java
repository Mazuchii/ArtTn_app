package tn.esprit.museum.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(root, screen.getWidth(), screen.getHeight());

        primaryStage.setTitle("Museum Digital — Connexion");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);

        // Taille minimale raisonnable
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        // Taille de restauration (quand on clique sur le bouton restore)
        primaryStage.setWidth(screen.getWidth() * 0.85);
        primaryStage.setHeight(screen.getHeight() * 0.85);
        primaryStage.setX(screen.getWidth() * 0.075);
        primaryStage.setY(screen.getHeight() * 0.075);

        // Démarrer maximisé — le bouton restore ramènera aux dimensions ci-dessus
        primaryStage.setMaximized(true);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
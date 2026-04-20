package edu.Projet_java.tests;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class mainClass extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // ========================================
        // CHOISIR CE QUE VOUS VOULEZ LANCER
        // ========================================

        // OPTION 1 : Page d'accueil des catégories (RECOMMANDÉ)
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/forum_categories.fxml"));

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("Forum - Museum Digital");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.show();


        /* OPTION 2 : Page des posts (directement)
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/forum_posts.fxml"));
        primaryStage.setTitle("Forum - Museum Digital");
        primaryStage.setScene(new Scene(root));
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(700);
        primaryStage.show();
        */

        /* OPTION 3 : Dashboard Admin
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/forum_dashboard.fxml"));
        primaryStage.setTitle("Dashboard - Forum");
        primaryStage.setScene(new Scene(root));
        primaryStage.setMaximized(true);
        primaryStage.show();
*/
    }

    public static void main(String[] args) {
        launch(args);
    }
}
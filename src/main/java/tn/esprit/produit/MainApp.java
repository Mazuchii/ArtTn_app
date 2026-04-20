package tn.esprit.produit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.produit.entities.User;
import tn.esprit.produit.utils.SessionManager;

public class MainApp extends Application {

   @Override
    public void start(Stage primaryStage) throws Exception {

        User defaultUser = new User(1, "admin", "admin@museum.com", "Administrateur", "ADMIN");
        SessionManager.login(defaultUser);

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/products_view.fxml"));
        primaryStage.setTitle("Museum Digital - Boutique");
        primaryStage.setScene(new Scene(root));
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

  /*  @Override
   public void start(Stage primaryStage) throws Exception {

       User admin = new User(1, "admin", "admin@museum.com", "Administrateur", "ADMIN");
       SessionManager.login(admin);

       Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboardproduit.fxml"));
       primaryStage.setTitle("Administration - Museum Digital");
       primaryStage.setScene(new Scene(root));
       primaryStage.setMaximized(true);
       primaryStage.show();
   }*/
}
package tn.esprit.museum.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.services.UserService;
import tn.esprit.museum.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        errorLabel.setText("");
        passwordField.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("❌ Email et mot de passe obligatoires.");
            return;
        }

        try {
            User user = userService.login(email, password);

            if (user == null) {
                errorLabel.setText("❌ Email ou mot de passe incorrect.");
                passwordField.clear();
                return;
            }

            // Sauvegarder l'utilisateur dans la session
            SessionManager.setCurrentUser(user);

            Stage stage = (Stage) emailField.getScene().getWindow();

            if (user.isAdmin()) {
                // ADMIN → vue gestion des événements
                Parent root = FXMLLoader.load(getClass().getResource("/fxml/events.fxml"));
                stage.getScene().setRoot(root);
                stage.setTitle("Gestion - Museum Digital");
            } else {
                // USER → catalogue, avec passage de l'email
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/events_front.fxml"));
                Parent root = loader.load();
                EventFrontController controller = loader.getController();
                controller.setCurrentUserEmail(user.getEmail());
                stage.getScene().setRoot(root);
                stage.setTitle("Catalogue - Museum Digital");
            }

        } catch (SQLException e) {
            errorLabel.setText("❌ Erreur DB: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            errorLabel.setText("❌ Erreur navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
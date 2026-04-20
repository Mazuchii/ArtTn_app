package tn.esprit.museum.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.services.UserService;

import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class LoginController implements Initializable {

    @FXML private ImageView loginImageView;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Label errorLabel;

    private UserService userService;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userService = new UserService();
        errorLabel.setVisible(false);

        Platform.runLater(() -> {
            Scene scene = usernameField.getScene();
            if (scene != null) {
                loginImageView.fitHeightProperty().bind(scene.heightProperty().multiply(0.7));
                loginImageView.setPreserveRatio(true);
            }
        });

        // Au démarrage, on charge les identifiants sauvegardés
        loadSavedCredentials();
    }

    @FXML
    private void handleLogin() {
        String usernameOrEmail = usernameField.getText().trim();
        String password = passwordField.getText();

        if (usernameOrEmail.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        try {
            User user = userService.login(usernameOrEmail, password);

            if (user != null) {
                // ⭐ SI la case "Remember Me" est cochée, on sauvegarde
                if (rememberMeCheckbox.isSelected()) {
                    saveCredentials(usernameOrEmail, password);
                } else {
                    // SINON on efface les identifiants sauvegardés
                    clearSavedCredentials();
                }

                if ("ADMIN".equals(user.getRole())) {
                    navigateTo("/fxml/dashboard.fxml", "Dashboard Admin", user);
                } else {
                    navigateTo("/fxml/user_home.fxml", "Museum Digital - Accueil", user);
                }
            } else {
                showError("Nom d'utilisateur ou mot de passe incorrect");
            }
        } catch (Exception e) {
            showError("Erreur de connexion : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navigateTo(String fxmlPath, String title, User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof DashboardController) {
                ((DashboardController) controller).setCurrentUser(user);
            } else if (controller instanceof UserHomeController) {
                ((UserHomeController) controller).setCurrentUser(user);
            }

            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.setMaximized(true);
            stage.centerOnScreen();

        } catch (IOException e) {
            showError("Erreur de navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSignup() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/register.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    /**
     * 📁 Sauvegarde les identifiants dans les PREFERENCES (fichier local)
     * ⚠️ Ces données ne sont PAS dans la base de données !
     */
    private void saveCredentials(String username, String password) {
        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
        prefs.put("saved_username", username);

        // Encodage simple pour ne pas stocker le mot de passe en clair
        String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());
        prefs.put("saved_password", encodedPassword);

        // On sauvegarde l'état de la case
        prefs.putBoolean("remember", true);

        System.out.println("✅ Identifiants sauvegardés dans les préférences");
    }

    /**
     * 📁 Charge les identifiants depuis les PREFERENCES
     */
    private void loadSavedCredentials() {
        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

        // Si la case avait été cochée précédemment
        if (prefs.getBoolean("remember", false)) {
            String savedUsername = prefs.get("saved_username", "");
            String encodedPassword = prefs.get("saved_password", "");

            if (!savedUsername.isEmpty()) {
                usernameField.setText(savedUsername);

                // Décoder le mot de passe
                if (!encodedPassword.isEmpty()) {
                    String decodedPassword = new String(Base64.getDecoder().decode(encodedPassword));
                    passwordField.setText(decodedPassword);
                }

                rememberMeCheckbox.setSelected(true);
                System.out.println("✅ Identifiants chargés depuis les préférences");
            }
        }
    }

    /**
     * 📁 Efface les identifiants sauvegardés
     */
    private void clearSavedCredentials() {
        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
        prefs.remove("saved_username");
        prefs.remove("saved_password");
        prefs.putBoolean("remember", false);

        System.out.println("🗑️ Identifiants effacés des préférences");
    }

    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reset_password.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Réinitialisation du mot de passe - Museum Digital");
            stage.setScene(new Scene(root));
            stage.setResizable(false);  // Empêche le redimensionnement
            stage.setWidth(900);        // Largeur fixe
            stage.setHeight(600);       // Hauteur fixe
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(usernameField.getScene().getWindow());
            stage.showAndWait();

        } catch (IOException e) {
            showError("Erreur: Impossible d'ouvrir la page de réinitialisation");
            e.printStackTrace();
        }
    }
}
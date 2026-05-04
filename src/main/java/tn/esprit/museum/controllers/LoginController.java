package tn.esprit.museum.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.services.UserService;
import tn.esprit.museum.utils.GoogleAuthService;
import tn.esprit.museum.utils.SessionManager;

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
    @FXML private Button googleLoginButton;

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
            // Vérifier d'abord si l'utilisateur existe (pour connaître son statut)
            User existingUser = userService.getByEmail(usernameOrEmail);
            if (existingUser == null) {
                existingUser = userService.getByUsername(usernameOrEmail);
            }

            // Si l'utilisateur existe MAIS est désactivé
            if (existingUser != null && !existingUser.isActive()) {
                showBannedAccountAlert(existingUser);
                return;
            }

            // Tentative de connexion normale
            User user = userService.login(usernameOrEmail, password);

            if (user != null) {
                if (rememberMeCheckbox.isSelected()) {
                    saveCredentials(usernameOrEmail, password);
                } else {
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

    /**
     * Affiche une alerte pour compte désactivé/banni
     */
    private void showBannedAccountAlert(User user) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("⚠️ Compte désactivé");
        alert.setHeaderText("Votre compte a été désactivé");
        alert.setContentText(
                "Bonjour " + user.getFullName() + ",\n\n" +
                        "❌ Votre compte a été désactivé par l'administrateur.\n\n" +
                        "📧 Veuillez contacter le support pour plus d'informations.\n\n" +
                        "🔒 Vous ne pouvez pas accéder à l'application pour le moment."
        );

        // Style de l'alerte
        alert.getDialogPane().setStyle("-fx-background-color: #F8F5F0; -fx-border-color: #e74c3c; -fx-border-width: 2; -fx-border-radius: 10;");

        alert.showAndWait();
    }

    @FXML
    private void handleGoogleLogin() {
        try {
            Stage authStage = new Stage();
            authStage.setTitle("Connexion avec Google - Museum Digital");
            authStage.initModality(Modality.WINDOW_MODAL);
            authStage.initOwner(usernameField.getScene().getWindow());

            WebView webView = new WebView();
            String authUrl = GoogleAuthService.getAuthorizationUrl();
            webView.getEngine().load(authUrl);

            webView.getEngine().locationProperty().addListener((obs, oldUrl, newUrl) -> {
                if (newUrl != null && newUrl.startsWith(GoogleAuthService.REDIRECT_URI)) {
                    String code = extractCodeFromUrl(newUrl);
                    if (code != null) {
                        authStage.close();
                        processGoogleLogin(code);
                    }
                }
            });

            Scene scene = new Scene(webView, 800, 600);
            authStage.setScene(scene);
            authStage.showAndWait();

        } catch (Exception e) {
            showError("Erreur lors de la connexion Google: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String extractCodeFromUrl(String url) {
        if (url.contains("code=")) {
            String[] parts = url.split("code=");
            if (parts.length > 1) {
                String code = parts[1];
                if (code.contains("&")) {
                    code = code.split("&")[0];
                }
                return code;
            }
        }
        return null;
    }

    private void processGoogleLogin(String authorizationCode) {
        try {
            GoogleAuthService.GoogleUserInfo googleUser = GoogleAuthService.exchangeCodeForUserInfo(authorizationCode);

            if (googleUser == null || googleUser.getEmail() == null) {
                showError("Impossible de récupérer les informations Google");
                return;
            }

            // Vérifier si l'utilisateur existe déjà dans la base
            User existingUser = userService.getByEmail(googleUser.getEmail());

            if (existingUser != null) {
                // Utilisateur existe déjà - vérifier s'il est actif
                if (!existingUser.isActive()) {
                    showBannedAccountAlert(existingUser);
                    return;
                }

                if ("ADMIN".equals(existingUser.getRole())) {
                    navigateTo("/fxml/dashboard.fxml", "Dashboard Admin", existingUser);
                } else {
                    navigateTo("/fxml/user_home.fxml", "Museum Digital - Accueil", existingUser);
                }
            } else {
                // Créer un nouveau compte utilisateur
                User newUser = new User();
                String username = googleUser.getEmail().split("@")[0];

                if (userService.isUsernameTaken(username)) {
                    username = username + "_" + System.currentTimeMillis();
                }

                newUser.setUsername(username);
                newUser.setEmail(googleUser.getEmail());
                newUser.setFullName(googleUser.getName());
                newUser.setPassword(generateRandomPassword());
                newUser.setRole("USER");
                newUser.setActive(true);

                userService.insert(newUser);

                navigateTo("/fxml/user_home.fxml", "Museum Digital - Accueil", newUser);
            }

        } catch (Exception e) {
            showError("Erreur lors de l'authentification Google: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    private void navigateTo(String fxmlPath, String title, User user) {
        try {
            SessionManager.login(user);
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

    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reset_password.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Réinitialisation du mot de passe - Museum Digital");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.setWidth(900);
            stage.setHeight(600);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(usernameField.getScene().getWindow());
            stage.showAndWait();

        } catch (IOException e) {
            showError("Erreur: Impossible d'ouvrir la page de réinitialisation");
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void saveCredentials(String username, String password) {
        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
        prefs.put("saved_username", username);
        String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());
        prefs.put("saved_password", encodedPassword);
        prefs.putBoolean("remember", true);
    }

    private void loadSavedCredentials() {
        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
        if (prefs.getBoolean("remember", false)) {
            String savedUsername = prefs.get("saved_username", "");
            String encodedPassword = prefs.get("saved_password", "");
            if (!savedUsername.isEmpty()) {
                usernameField.setText(savedUsername);
                if (!encodedPassword.isEmpty()) {
                    String decodedPassword = new String(Base64.getDecoder().decode(encodedPassword));
                    passwordField.setText(decodedPassword);
                }
                rememberMeCheckbox.setSelected(true);
            }
        }
    }

    private void clearSavedCredentials() {
        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
        prefs.remove("saved_username");
        prefs.remove("saved_password");
        prefs.putBoolean("remember", false);
    }
}

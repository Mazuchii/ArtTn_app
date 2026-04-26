package tn.esprit.museum.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.services.UserService;

import java.sql.SQLException;

public class GoogleSetPasswordController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField fullNameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;

    private User tempUser;
    private UserService userService = new UserService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
    }

    public void initData(User user) {
        this.tempUser = user;
        usernameField.setText(user.getUsername());
        emailField.setText(user.getEmail());
        fullNameField.setText(user.getFullName());
    }

    @FXML
    private void handleSetPassword() {
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (password.isEmpty()) {
            showError("Veuillez choisir un mot de passe");
            return;
        }

        if (password.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas");
            return;
        }

        try {
            userService.updatePassword(tempUser.getId(), password);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText("✅ Mot de passe créé avec succès !\nVous pouvez maintenant vous connecter.");
            alert.showAndWait();

            Stage stage = (Stage) passwordField.getScene().getWindow();
            stage.close();

        } catch (SQLException e) {
            showError("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSkip() {
        try {
            String randomPassword = generateRandomPassword();
            userService.updatePassword(tempUser.getId(), randomPassword);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText("Un mot de passe temporaire a été généré.\n\nMot de passe: " + randomPassword);
            alert.showAndWait();

            Stage stage = (Stage) passwordField.getScene().getWindow();
            stage.close();

        } catch (SQLException e) {
            showError("Erreur: " + e.getMessage());
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

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
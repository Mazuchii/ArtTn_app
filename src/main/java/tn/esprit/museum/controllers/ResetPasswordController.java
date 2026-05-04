package tn.esprit.museum.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.services.UserService;
import tn.esprit.museum.utils.EmailService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ResetPasswordController {

    @FXML private VBox step1Container;
    @FXML private VBox step2Container;
    @FXML private VBox step3Container;
    @FXML private TextField emailField;
    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private UserService userService = new UserService();
    private User userToReset;
    private String generatedCode;
    private LocalDateTime codeExpiry;
    private static Map<String, CodeInfo> resetCodes = new HashMap<>();

    @FXML
    public void initialize() {
        // Afficher uniquement l'étape 1 au démarrage
        step1Container.setVisible(true);
        step2Container.setVisible(false);
        step3Container.setVisible(false);
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
    }

    @FXML
    private void handleSendResetEmail() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Veuillez entrer votre adresse email");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Format d'email invalide");
            return;
        }

        try {
            User user = userService.getByEmail(email);
            if (user == null) {
                showError("Aucun compte trouvé avec cet email");
                return;
            }

            generatedCode = EmailService.generateResetCode();
            userToReset = user;
            codeExpiry = LocalDateTime.now().plusMinutes(10);

            resetCodes.put(email, new CodeInfo(generatedCode, codeExpiry));

            boolean emailSent = EmailService.sendPasswordResetEmail(email, generatedCode);

            if (emailSent) {
                step1Container.setVisible(false);
                step2Container.setVisible(true);
                showSuccess("Un code a été envoyé à " + email);
                codeField.clear();
            } else {
                showError("Erreur lors de l'envoi. Code: " + generatedCode);
            }

        } catch (SQLException e) {
            showError("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleVerifyCode() {
        String enteredCode = codeField.getText().trim();
        String email = emailField.getText().trim();

        if (enteredCode.isEmpty()) {
            showError("Veuillez entrer le code");
            return;
        }

        CodeInfo storedCode = resetCodes.get(email);

        if (storedCode == null) {
            showError("Aucune demande trouvée");
            return;
        }

        if (LocalDateTime.now().isAfter(storedCode.expiry)) {
            showError("Le code a expiré");
            resetCodes.remove(email);
            step1Container.setVisible(true);
            step2Container.setVisible(false);
            return;
        }

        if (!storedCode.code.equals(enteredCode)) {
            showError("Code invalide");
            return;
        }

        step2Container.setVisible(false);
        step3Container.setVisible(true);
        showSuccess(" Code vérifié !");
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    @FXML
    private void handleResetPassword() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (newPassword.isEmpty()) {
            showError("Entrez un nouveau mot de passe");
            return;
        }

        if (newPassword.length() < 6) {
            showError("Minimum 6 caractères");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas");
            return;
        }

        try {
            userService.updatePassword(userToReset.getId(), newPassword);
            EmailService.sendPasswordChangedConfirmation(userToReset.getEmail());

            resetCodes.remove(emailField.getText().trim());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText(" Mot de passe modifié avec succès !");
            alert.showAndWait();

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.close();

        } catch (SQLException e) {
            showError("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void handleResendCode() {
        handleSendResetEmail();
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion - ART.TN");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }

    private static class CodeInfo {
        String code;
        LocalDateTime expiry;

        CodeInfo(String code, LocalDateTime expiry) {
            this.code = code;
            this.expiry = expiry;
        }
    }
}

package tn.esprit.museum.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.services.UserService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.UUID;

public class ProfileController {

    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private ImageView profileImageView;
    @FXML private Button changePhotoButton;

    private User currentUser;
    private UserService userService = new UserService();
    private UserHomeController mainController;

    public void initData(User user, UserHomeController mainController) {
        this.currentUser = user;
        this.mainController = mainController;

        fullNameField.setText(user.getFullName());
        usernameField.setText(user.getUsername());
        emailField.setText(user.getEmail());

        if (errorLabel != null) errorLabel.setVisible(false);

        // Charger la photo de profil
        loadProfilePicture();
    }

    private void loadProfilePicture() {
        if (profileImageView == null) return;

        try {
            String picturePath = userService.getProfilePicture(currentUser.getId());
            if (picturePath != null && !picturePath.isEmpty()) {
                File file = new File(picturePath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    profileImageView.setImage(image);
                    return;
                }
            }
            // Image par défaut
            setDefaultProfilePicture();
        } catch (SQLException e) {
            setDefaultProfilePicture();
            e.printStackTrace();
        }
    }

    private void setDefaultProfilePicture() {
        try {
            // Essayer de charger une image par défaut depuis les ressources
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/default-avatar.png"));
            profileImageView.setImage(defaultImage);
        } catch (Exception e) {
            // Si pas d'image, laisser vide ou mettre un placeholder
            profileImageView.setImage(null);
            profileImageView.setStyle("-fx-background-color: #d4af37; -fx-background-radius: 50;");
        }
    }

    @FXML
    private void handleChangeProfilePicture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        File selectedFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());

        if (selectedFile != null) {
            // Vérifier la taille du fichier (max 5MB)
            if (selectedFile.length() > 5 * 1024 * 1024) {
                showError("La photo ne doit pas dépasser 5MB");
                return;
            }

            try {
                // Créer le dossier s'il n'existe pas
                String userDir = "uploads/profile_pictures/";
                File dir = new File(userDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                // Générer un nom unique pour l'image
                String fileExtension = getFileExtension(selectedFile.getName());
                String fileName = currentUser.getId() + "_" + UUID.randomUUID().toString() + fileExtension;
                Path destination = Paths.get(userDir + fileName);

                // Copier le fichier
                Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

                // Sauvegarder le chemin dans la base de données
                String picturePath = destination.toAbsolutePath().toString();
                userService.updateProfilePicture(currentUser.getId(), picturePath);
                currentUser.setProfilePicture(picturePath);

                // Mettre à jour l'affichage
                Image image = new Image(selectedFile.toURI().toString());
                profileImageView.setImage(image);
                profileImageView.setStyle("");

                showSuccessMessage("Photo de profil mise à jour !");

            } catch (IOException | SQLException e) {
                showError("Erreur lors de l'upload: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot > 0) {
            return fileName.substring(lastDot);
        }
        return ".png";
    }

    @FXML
    private void handleSave() {
        String newFullName = fullNameField.getText().trim();
        String newUsername = usernameField.getText().trim();
        String newEmail = emailField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // 1. Validation des champs
        if (newFullName.isEmpty() || newUsername.isEmpty() || newEmail.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        if (newUsername.length() < 3) {
            showError("Le nom d'utilisateur doit contenir au moins 3 caractères");
            return;
        }

        if (!newEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Email invalide");
            return;
        }

        // 2. Vérification unicité username
        if (!newUsername.equals(currentUser.getUsername())) {
            try {
                if (userService.isUsernameTaken(newUsername)) {
                    showError("Ce nom d'utilisateur est déjà pris");
                    return;
                }
            } catch (SQLException e) {
                showError("Erreur de vérification");
                e.printStackTrace();
                return;
            }
        }

        // 3. Vérification unicité email
        if (!newEmail.equals(currentUser.getEmail())) {
            try {
                if (userService.isEmailTaken(newEmail)) {
                    showError("Cet email est déjà utilisé");
                    return;
                }
            } catch (SQLException e) {
                showError("Erreur de vérification");
                e.printStackTrace();
                return;
            }
        }

        // 4. Validation du mot de passe
        if (!newPassword.isEmpty()) {
            if (newPassword.length() < 6) {
                showError("Le mot de passe doit contenir au moins 6 caractères");
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                showError("Les mots de passe ne correspondent pas");
                return;
            }
        }

        try {
            // 5. Mise à jour de l'objet utilisateur
            currentUser.setFullName(newFullName);
            currentUser.setUsername(newUsername);
            currentUser.setEmail(newEmail);

            // 6. Sauvegarde dans la base
            userService.update(currentUser);

            // 7. Mise à jour du mot de passe si nécessaire
            if (!newPassword.isEmpty()) {
                userService.updatePassword(currentUser.getId(), newPassword);
            }

            // 8. Notification de succès
            showSuccessMessage("✅ Votre profil a été mis à jour avec succès !");

            // 9. Rafraîchissement de l'interface principale
            if (mainController != null) {
                mainController.refreshUserInfo();
                mainController.showDashboard();
            }

        } catch (SQLException e) {
            showError("Erreur lors de la mise à jour: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteProfilePicture() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la photo");
        confirm.setHeaderText(null);
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer votre photo de profil ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Supprimer le fichier s'il existe
                    String currentPicture = userService.getProfilePicture(currentUser.getId());
                    if (currentPicture != null && !currentPicture.isEmpty()) {
                        File file = new File(currentPicture);
                        if (file.exists()) {
                            file.delete();
                        }
                    }

                    // Mettre à jour la base de données
                    userService.updateProfilePicture(currentUser.getId(), null);
                    currentUser.setProfilePicture(null);

                    // Mettre l'image par défaut
                    setDefaultProfilePicture();

                    showSuccessMessage("Photo de profil supprimée");

                } catch (SQLException e) {
                    showError("Erreur lors de la suppression: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void handleCancel() {
        if (mainController != null) {
            mainController.showDashboard();
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #e74c3c;");
            errorLabel.setVisible(true);

            // Cacher le message après 3 secondes
            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException e) {}
                javafx.application.Platform.runLater(() -> errorLabel.setVisible(false));
            }).start();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }

    private void showSuccessMessage(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #27ae60;");
            errorLabel.setVisible(true);

            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException e) {}
                javafx.application.Platform.runLater(() -> errorLabel.setVisible(false));
            }).start();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }
}
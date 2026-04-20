package tn.esprit.museum.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
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

public class UserHomeController {

    @FXML private Label welcomeLabel;
    @FXML private ImageView profileImageView;
    @FXML private ImageView topProfileImageView;
    @FXML private VBox homeView;
    @FXML private VBox profileView;
    @FXML private Circle profileCircle;

    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label profileMessageLabel;

    private User currentUser;
    private UserService userService;

    @FXML
    public void initialize() {
        userService = new UserService();
        if (profileMessageLabel != null) {
            profileMessageLabel.setVisible(false);
        }

        makeImageCircular(profileImageView, 50);

    }

    private void makeImageCircular(ImageView imageView, double radius) {
        Circle clip = new Circle();
        clip.centerXProperty().bind(imageView.fitWidthProperty().divide(2));
        clip.centerYProperty().bind(imageView.fitHeightProperty().divide(2));
        clip.radiusProperty().bind(imageView.fitWidthProperty().divide(2));
        imageView.setClip(clip);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadUserData();
        showHome();
    }

    public void refreshUserInfo() {
        loadUserData();
    }

    public void showHome() {
        if (homeView != null) homeView.setVisible(true);
        if (profileView != null) profileView.setVisible(false);
    }

    public void showDashboard() {
        showHome();
    }

    private void loadUserData() {
        if (currentUser != null) {
            if (welcomeLabel != null) {
                welcomeLabel.setText("Bienvenue, " + currentUser.getFullName() + "!");
            }

            if (fullNameField != null) fullNameField.setText(currentUser.getFullName());
            if (usernameField != null) usernameField.setText(currentUser.getUsername());
            if (emailField != null) emailField.setText(currentUser.getEmail());

            loadProfilePicture();
        }
    }

    private void loadProfilePicture() {
        try {
            String picturePath = userService.getProfilePicture(currentUser.getId());
            Image image = null;

            if (picturePath != null && !picturePath.isEmpty()) {
                File file = new File(picturePath);
                if (file.exists()) {
                    image = new Image(file.toURI().toString());
                }
            }

            if (image == null) {
                image = getDefaultProfileImage();
            }

            if (profileImageView != null) profileImageView.setImage(image);
            if (topProfileImageView != null) topProfileImageView.setImage(image);

        } catch (SQLException e) {
            setDefaultProfilePicture();
            e.printStackTrace();
        }
    }

    private Image getDefaultProfileImage() {
        try {
            return new Image(getClass().getResourceAsStream("/images/default-avatar.png"));
        } catch (Exception e) {
            return null;
        }
    }

    private void setDefaultProfilePicture() {
        Image defaultImage = getDefaultProfileImage();
        if (profileImageView != null) profileImageView.setImage(defaultImage);
        if (topProfileImageView != null) topProfileImageView.setImage(defaultImage);
    }

    @FXML
    private void handleChangeProfilePicture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());

        if (selectedFile != null) {
            try {
                String userDir = "uploads/profile_pictures/";
                File dir = new File(userDir);
                if (!dir.exists()) dir.mkdirs();

                String fileExtension = getFileExtension(selectedFile.getName());
                String fileName = currentUser.getId() + "_" + UUID.randomUUID().toString() + fileExtension;
                Path destination = Paths.get(userDir + fileName);

                Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

                String picturePath = destination.toAbsolutePath().toString();
                userService.updateProfilePicture(currentUser.getId(), picturePath);
                currentUser.setProfilePicture(picturePath);

                Image image = new Image(selectedFile.toURI().toString());
                if (profileImageView != null) profileImageView.setImage(image);
                if (topProfileImageView != null) topProfileImageView.setImage(image);

                showProfileMessage("✅ Photo de profil mise à jour !", "success");

            } catch (IOException | SQLException e) {
                showProfileMessage("❌ Erreur: " + e.getMessage(), "error");
                e.printStackTrace();
            }
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        return lastDot > 0 ? fileName.substring(lastDot) : ".png";
    }

    @FXML
    private void handleSaveChanges() {
        String newFullName = fullNameField.getText().trim();
        String newUsername = usernameField.getText().trim();
        String newEmail = emailField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (newFullName.isEmpty() || newUsername.isEmpty() || newEmail.isEmpty()) {
            showProfileMessage("Veuillez remplir tous les champs", "error");
            return;
        }

        if (newUsername.length() < 3) {
            showProfileMessage("Username minimum 3 caractères", "error");
            return;
        }

        if (!newEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showProfileMessage("Email invalide", "error");
            return;
        }

        if (!newUsername.equals(currentUser.getUsername())) {
            try {
                if (userService.isUsernameTaken(newUsername)) {
                    showProfileMessage("Username déjà pris", "error");
                    return;
                }
            } catch (SQLException e) { showProfileMessage("Erreur", "error"); return; }
        }

        if (!newEmail.equals(currentUser.getEmail())) {
            try {
                if (userService.isEmailTaken(newEmail)) {
                    showProfileMessage("Email déjà utilisé", "error");
                    return;
                }
            } catch (SQLException e) { showProfileMessage("Erreur", "error"); return; }
        }

        if (!newPassword.isEmpty()) {
            if (newPassword.length() < 6) {
                showProfileMessage("Mot de passe minimum 6 caractères", "error");
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                showProfileMessage("Les mots de passe ne correspondent pas", "error");
                return;
            }
        }

        try {
            currentUser.setFullName(newFullName);
            currentUser.setUsername(newUsername);
            currentUser.setEmail(newEmail);

            userService.update(currentUser);

            if (!newPassword.isEmpty()) {
                userService.updatePassword(currentUser.getId(), newPassword);
            }

            loadUserData();
            newPasswordField.clear();
            confirmPasswordField.clear();

            showProfileMessage("✅ Informations mises à jour !", "success");

            // Retour à l'accueil après 1.5 secondes
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException e) {}
                javafx.application.Platform.runLater(() -> showHome());
            }).start();

        } catch (SQLException e) {
            showProfileMessage("Erreur: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void handleCancel() {
        loadUserData();
        newPasswordField.clear();
        confirmPasswordField.clear();
        showProfileMessage("Modifications annulées", "info");
        showHome();
    }

    private void showProfileMessage(String message, String type) {
        if (profileMessageLabel != null) {
            profileMessageLabel.setText(message);
            switch (type) {
                case "error": profileMessageLabel.setStyle("-fx-text-fill: #e74c3c;"); break;
                case "success": profileMessageLabel.setStyle("-fx-text-fill: #27ae60;"); break;
                default: profileMessageLabel.setStyle("-fx-text-fill: #3498db;");
            }
            profileMessageLabel.setVisible(true);

            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException e) {}
                javafx.application.Platform.runLater(() -> profileMessageLabel.setVisible(false));
            }).start();
        }
    }

    @FXML
    private void handleShowHome() {
        showHome();
    }

    @FXML
    private void handleShowProfilePage() {
        if (homeView != null) homeView.setVisible(false);
        if (profileView != null) profileView.setVisible(true);
        loadUserData();
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Déconnexion");
        confirm.setHeaderText(null);
        confirm.setContentText("Êtes-vous sûr de vouloir vous déconnecter?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                    javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());
                    javafx.stage.Stage stage = (javafx.stage.Stage) profileImageView.getScene().getWindow();
                    stage.setScene(scene);
                    stage.setTitle("Connexion - Museum Digital");
                    stage.centerOnScreen();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
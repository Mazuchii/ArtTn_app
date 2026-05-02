package tn.esprit.museum.controllers;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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
import java.util.regex.Pattern;

public class RegisterController {

    @FXML private TextField fullNameField, usernameField, emailField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private Label usernameErrorLabel, emailErrorLabel, passwordErrorLabel, matchErrorLabel;
    @FXML private Label usernameTakenLabel, emailTakenLabel;
    @FXML private Label errorLabel, photoLabel;
    @FXML private Button registerButton;
    @FXML private ImageView profileImageView;

    private UserService userService = new UserService();
    private String selectedProfilePicturePath = null;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    @FXML
    public void initialize() {
        setupLiveValidation();
        setupUniquenessValidation();

        makeImageCircular();

        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/default-avatar.png"));
            profileImageView.setImage(defaultImage);
        } catch (Exception e) {
            // Pas d'image par défaut
        }
    }

    private void makeImageCircular() {
        Circle clip = new Circle();
        clip.centerXProperty().bind(profileImageView.fitWidthProperty().divide(2));
        clip.centerYProperty().bind(profileImageView.fitHeightProperty().divide(2));
        clip.radiusProperty().bind(profileImageView.fitWidthProperty().divide(2));
        profileImageView.setClip(clip);
    }

    private void setupLiveValidation() {
        // Validation Username (format)
        usernameErrorLabel.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            String val = usernameField.getText().trim();
            return !val.isEmpty() && !USERNAME_PATTERN.matcher(val).matches();
        }, usernameField.textProperty()));
        usernameErrorLabel.managedProperty().bind(usernameErrorLabel.visibleProperty());

        // Validation Email (format)
        emailErrorLabel.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            String val = emailField.getText().trim();
            return !val.isEmpty() && !EMAIL_PATTERN.matcher(val).matches();
        }, emailField.textProperty()));
        emailErrorLabel.managedProperty().bind(emailErrorLabel.visibleProperty());

        // Validation Password
        passwordErrorLabel.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            String val = passwordField.getText();
            return !val.isEmpty() && val.length() < 6;
        }, passwordField.textProperty()));
        passwordErrorLabel.managedProperty().bind(passwordErrorLabel.visibleProperty());

        // Validation confirmation
        matchErrorLabel.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            String p1 = passwordField.getText();
            String p2 = confirmPasswordField.getText();
            return !p2.isEmpty() && !p1.equals(p2);
        }, passwordField.textProperty(), confirmPasswordField.textProperty()));
        matchErrorLabel.managedProperty().bind(matchErrorLabel.visibleProperty());

        // Désactivation du bouton
        registerButton.disableProperty().bind(
                usernameErrorLabel.visibleProperty()
                        .or(emailErrorLabel.visibleProperty())
                        .or(passwordErrorLabel.visibleProperty())
                        .or(matchErrorLabel.visibleProperty())
                        .or(fullNameField.textProperty().isEmpty())
                        .or(usernameField.textProperty().isEmpty())
                        .or(emailField.textProperty().isEmpty())
                        .or(passwordField.textProperty().isEmpty())
                        .or(usernameTakenLabel.visibleProperty())
                        .or(emailTakenLabel.visibleProperty())
        );

        registerButton.opacityProperty().bind(
                Bindings.when(registerButton.disableProperty()).then(0.6).otherwise(1.0)
        );
    }

    private void setupUniquenessValidation() {
        // Validation Username unique
        usernameTakenLabel.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            String val = usernameField.getText().trim();
            if (val.isEmpty() || !USERNAME_PATTERN.matcher(val).matches()) {
                return false;
            }
            try {
                return userService.isUsernameTaken(val);
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }, usernameField.textProperty()));
        usernameTakenLabel.managedProperty().bind(usernameTakenLabel.visibleProperty());

        // Validation Email unique
        emailTakenLabel.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            String val = emailField.getText().trim();
            if (val.isEmpty() || !EMAIL_PATTERN.matcher(val).matches()) {
                return false;
            }
            try {
                return userService.isEmailTaken(val);
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }, emailField.textProperty()));
        emailTakenLabel.managedProperty().bind(emailTakenLabel.visibleProperty());
    }

    @FXML
    private void handleChooseProfilePicture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        File selectedFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());

        if (selectedFile != null) {
            if (selectedFile.length() > 5 * 1024 * 1024) {
                errorLabel.setText(" La photo ne doit pas dépasser 5MB");
                errorLabel.setVisible(true);
                return;
            }

            try {
                String userDir = "uploads/profile_pictures/";
                File dir = new File(userDir);
                if (!dir.exists()) dir.mkdirs();

                String fileName = "temp_" + UUID.randomUUID().toString() + getFileExtension(selectedFile.getName());
                Path destination = Paths.get(userDir + fileName);

                Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

                selectedProfilePicturePath = destination.toAbsolutePath().toString();

                Image image = new Image(selectedFile.toURI().toString());
                profileImageView.setImage(image);

                photoLabel.setText(" Photo choisie");
                photoLabel.setStyle("-fx-text-fill: #27ae60;");
                errorLabel.setVisible(false);

            } catch (IOException e) {
                errorLabel.setText(" Erreur lors de l'upload: " + e.getMessage());
                errorLabel.setVisible(true);
                e.printStackTrace();
            }
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        return lastDot > 0 ? fileName.substring(lastDot) : ".png";
    }

    private String saveProfilePicture(int userId) {
        if (selectedProfilePicturePath == null || selectedProfilePicturePath.isEmpty()) {
            return null;
        }

        try {
            File tempFile = new File(selectedProfilePicturePath);
            if (!tempFile.exists()) return null;

            String fileExtension = getFileExtension(tempFile.getName());
            String fileName = userId + "_" + UUID.randomUUID().toString() + fileExtension;
            Path destination = Paths.get("uploads/profile_pictures/" + fileName);

            Files.copy(tempFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            tempFile.delete();

            return destination.toAbsolutePath().toString();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();

        try {
            if (userService.isUsernameTaken(username)) {
                errorLabel.setText(" Ce nom d'utilisateur est déjà pris");
                errorLabel.setVisible(true);
                return;
            }

            if (userService.isEmailTaken(email)) {
                errorLabel.setText(" Cet email est déjà utilisé");
                errorLabel.setVisible(true);
                return;
            }

            User newUser = new User(
                    username,
                    email,
                    passwordField.getText(),
                    fullNameField.getText().trim()
            );

            userService.insert(newUser);

            String picturePath = saveProfilePicture(newUser.getId());
            if (picturePath != null) {
                userService.updateProfilePicture(newUser.getId(), picturePath);
                newUser.setProfilePicture(picturePath);
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText(" Compte créé avec succès !\nBienvenue " + newUser.getFullName());
            alert.showAndWait();

            handleLogin();

        } catch (SQLException e) {
            errorLabel.setText(" Erreur : " + e.getMessage());
            errorLabel.setVisible(true);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion - Museum Digital");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
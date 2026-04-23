package tn.esprit.museum.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.services.UserService;
import tn.esprit.museum.utils.FraudDetectionService;
import tn.esprit.museum.utils.PDFExporter;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import java.util.Date;

import tn.esprit.museum.utils.FraudDetectionService;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label statsTotalUsers;
    @FXML private Label statsActiveUsers;
    @FXML private Label statsAdmins;
    @FXML private Label statsInactiveUsers;
    @FXML private VBox inactiveCard;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> fullNameColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, Boolean> activeColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterCombo;

    // Graphiques
    @FXML private PieChart rolePieChart;
    @FXML private BarChart<String, Number> statsBarChart;

    private UserService userService;
    private User currentUser;
    private ObservableList<User> userList;

    @FXML
    public void initialize() {
        userService = new UserService();
        userList = FXCollections.observableArrayList();

        // Initialiser les colonnes
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));

        // Initialiser le filtre de rôle
        roleFilterCombo.getItems().addAll("Tous", "ADMIN", "USER");
        roleFilterCombo.setValue("Tous");

        // Ajouter des écouteurs
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterUsers());
        roleFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> filterUsers());

        loadUsers();
        updateStats();
        updateCharts();

        // Ajouter l'effet de survol pour la carte des comptes désactivés
        addHoverEffectToInactiveCard();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        welcomeLabel.setText("Bienvenue, " + user.getFullName() + "!");
    }

    private void loadUsers() {
        try {
            userList.setAll(userService.getAll());
            usersTable.setItems(userList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les utilisateurs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void filterUsers() {
        String searchText = searchField.getText().toLowerCase();
        String selectedRole = roleFilterCombo.getValue();

        ObservableList<User> filteredList = FXCollections.observableArrayList();

        for (User user : userList) {
            boolean matchesSearch = searchText.isEmpty() ||
                    user.getUsername().toLowerCase().contains(searchText) ||
                    user.getEmail().toLowerCase().contains(searchText) ||
                    user.getFullName().toLowerCase().contains(searchText);

            boolean matchesRole = selectedRole.equals("Tous") ||
                    user.getRole().equals(selectedRole);

            if (matchesSearch && matchesRole) {
                filteredList.add(user);
            }
        }

        usersTable.setItems(filteredList);
    }

    private void updateStats() {
        try {
            int totalUsers = userService.getAll().size();
            statsTotalUsers.setText(String.valueOf(totalUsers));
            statsActiveUsers.setText(String.valueOf(countActiveUsers()));
            statsAdmins.setText(String.valueOf(countAdmins()));
            statsInactiveUsers.setText(String.valueOf(countInactiveUsers()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int countActiveUsers() throws SQLException {
        return (int) userService.getAll().stream().filter(User::isActive).count();
    }

    private int countAdmins() throws SQLException {
        return (int) userService.getAll().stream().filter(u -> "ADMIN".equals(u.getRole())).count();
    }

    private int countInactiveUsers() throws SQLException {
        return (int) userService.getAll().stream().filter(u -> !u.isActive()).count();
    }

    private void updateCharts() {
        try {
            int totalUsers = userService.getAll().size();
            int admins = countAdmins();
            int regularUsers = totalUsers - admins;
            int activeUsers = countActiveUsers();
            int inactiveUsers = totalUsers - activeUsers;

            // Mettre à jour le PieChart (répartition des rôles)
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                    new PieChart.Data("Administrateurs (" + admins + ")", admins),
                    new PieChart.Data("Utilisateurs (" + regularUsers + ")", regularUsers)
            );
            rolePieChart.setData(pieChartData);
            rolePieChart.setTitle("Répartition des rôles");
            rolePieChart.setClockwise(true);
            rolePieChart.setLabelLineLength(10);
            rolePieChart.setLabelsVisible(true);

            // Mettre à jour le BarChart (statut des comptes)
            statsBarChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Nombre d'utilisateurs");
            series.getData().add(new XYChart.Data<>("Actifs", activeUsers));
            series.getData().add(new XYChart.Data<>("Inactifs", inactiveUsers));

            statsBarChart.getData().add(series);
            statsBarChart.setTitle("Statut des comptes");
            statsBarChart.setLegendVisible(false);
            statsBarChart.setAnimated(true);

            // Appliquer les couleurs
            if (!series.getData().isEmpty()) {
                // Les couleurs seront appliquées automatiquement par JavaFX
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addHoverEffectToInactiveCard() {
        if (inactiveCard == null) return;

        inactiveCard.setOnMouseEntered(e -> {
            inactiveCard.setStyle("-fx-background-color: #c0392b; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 15, 0, 0, 5); -fx-cursor: hand;");
        });

        inactiveCard.setOnMouseExited(e -> {
            inactiveCard.setStyle("-fx-background-color: #e74c3c; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        });
    }

    @FXML
    private void handleShowInactiveUsers() {
        try {
            ObservableList<User> inactiveUsers = FXCollections.observableArrayList();
            for (User user : userList) {
                if (!user.isActive()) {
                    inactiveUsers.add(user);
                }
            }

            if (inactiveUsers.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Comptes désactivés");
                alert.setHeaderText(null);
                alert.setContentText("📭 Aucun compte désactivé pour le moment.");
                alert.showAndWait();
                return;
            }

            // Créer un dialog pour afficher la liste des comptes désactivés
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Comptes désactivés");
            dialog.setHeaderText("🚫 Liste des comptes désactivés (" + inactiveUsers.size() + ")");

            // Appliquer le style CSS
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            // Créer un tableau pour afficher les utilisateurs désactivés
            TableView<User> inactiveTable = new TableView<>();
            inactiveTable.setItems(inactiveUsers);
            inactiveTable.setPrefHeight(400);
            inactiveTable.setPrefWidth(600);
            inactiveTable.setStyle("-fx-background-radius: 10;");

            // Colonnes
            TableColumn<User, Integer> idCol = new TableColumn<>("🆔 ID");
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            idCol.setPrefWidth(60);
            idCol.setStyle("-fx-alignment: CENTER;");

            TableColumn<User, String> usernameCol = new TableColumn<>("👤 Nom d'utilisateur");
            usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
            usernameCol.setPrefWidth(150);

            TableColumn<User, String> emailCol = new TableColumn<>("📧 Email");
            emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
            emailCol.setPrefWidth(220);

            TableColumn<User, String> fullNameCol = new TableColumn<>("📛 Nom complet");
            fullNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
            fullNameCol.setPrefWidth(180);

            TableColumn<User, String> roleCol = new TableColumn<>("🎭 Rôle");
            roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
            roleCol.setPrefWidth(100);
            roleCol.setStyle("-fx-alignment: CENTER;");

            inactiveTable.getColumns().addAll(idCol, usernameCol, emailCol, fullNameCol, roleCol);

            // Bouton pour réactiver un utilisateur sélectionné
            Button reactivateButton = new Button("🔄 Réactiver le compte");
            reactivateButton.setStyle("-fx-background-color: #27ae60; -fx-background-radius: 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;");
            reactivateButton.setOnAction(e -> {
                User selected = inactiveTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Êtes-vous sûr de vouloir réactiver le compte de " + selected.getFullName() + " ?");

                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        try {
                            userService.activate(selected.getId());
                            loadUsers();
                            updateStats();
                            updateCharts();
                            showAlert("Succès", "✅ Compte de " + selected.getFullName() + " réactivé avec succès!");
                            dialog.close();
                        } catch (SQLException ex) {
                            showAlert("Erreur", "Impossible de réactiver: " + ex.getMessage());
                        }
                    }
                } else {
                    showAlert("Information", "Veuillez sélectionner un compte à réactiver.");
                }
            });

            VBox content = new VBox(15);
            content.setStyle("-fx-padding: 20; -fx-background-color: #F8F5F0;");
            content.getChildren().addAll(inactiveTable, reactivateButton);

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setPrefWidth(750);
            dialog.getDialogPane().setPrefHeight(550);

            Button closeButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
            closeButton.setText("Fermer");
            closeButton.setStyle("-fx-background-color: #7f8c8d; -fx-background-radius: 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;");

            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(usersTable.getScene().getWindow());
            dialog.showAndWait();

        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'afficher les comptes désactivés: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddUser() {
        showAdminDialog();
    }

    @FXML
    private void handleEditUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Aucune sélection", "Veuillez sélectionner un utilisateur à modifier.");
            return;
        }
        showEditUserDialog(selected);
    }

    @FXML
    private void handleDeleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Aucune sélection", "Veuillez sélectionner un utilisateur à supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'utilisateur");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer " + selected.getFullName() + " ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.delete(selected.getId());
                loadUsers();
                updateStats();
                updateCharts();
                showAlert("Succès", "Utilisateur supprimé avec succès!");
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible de supprimer: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleToggleUserStatus() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Aucune sélection", "Veuillez sélectionner un utilisateur.");
            return;
        }

        try {
            if (selected.isActive()) {
                userService.deactivate(selected.getId());
                showAlert("Succès", "Utilisateur désactivé avec succès!");
            } else {
                userService.activate(selected.getId());
                showAlert("Succès", "Utilisateur activé avec succès!");
            }
            loadUsers();
            updateStats();
            updateCharts();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de modifier le statut: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Déconnexion");
        confirm.setHeaderText(null);
        confirm.setContentText("Êtes-vous sûr de vouloir vous déconnecter?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/fxml/login.fxml")
                );
                javafx.scene.Parent root = loader.load();
                javafx.stage.Stage stage = (javafx.stage.Stage) welcomeLabel.getScene().getWindow();
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("Connexion - Museum Digital");
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Dialogue pour ajouter un ADMIN
     */
    private void showAdminDialog() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un administrateur");
        dialog.setHeaderText(null);

        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("custom-dialog");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new javafx.geometry.Insets(25, 25, 25, 25));
        grid.setStyle("-fx-background-color: #F8F5F0; -fx-background-radius: 15;");

        Label titleLabel = new Label("➕ Créer un nouvel administrateur");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 10 0;");
        GridPane.setColumnSpan(titleLabel, 2);
        grid.add(titleLabel, 0, 0);

        TextField usernameField = new TextField();
        usernameField.setPromptText("ex: mohamed ben mohamed");
        usernameField.setPrefHeight(45);
        usernameField.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dcdde1; -fx-border-radius: 10; -fx-padding: 0 15; -fx-font-size: 14px;");

        TextField emailField = new TextField();
        emailField.setPromptText("ex: mohamed@museum.com");
        emailField.setPrefHeight(45);
        emailField.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dcdde1; -fx-border-radius: 10; -fx-padding: 0 15; -fx-font-size: 14px;");

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("ex: Mohamed Ben Mohamed");
        fullNameField.setPrefHeight(45);
        fullNameField.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dcdde1; -fx-border-radius: 10; -fx-padding: 0 15; -fx-font-size: 14px;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Minimum 6 caractères");
        passwordField.setPrefHeight(45);
        passwordField.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dcdde1; -fx-border-radius: 10; -fx-padding: 0 15; -fx-font-size: 14px;");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirmer le mot de passe");
        confirmPasswordField.setPrefHeight(45);
        confirmPasswordField.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dcdde1; -fx-border-radius: 10; -fx-padding: 0 15; -fx-font-size: 14px;");

        Label usernameError = new Label();
        usernameError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px; -fx-padding: 5 0 0 0;");
        usernameError.setVisible(false);

        Label emailError = new Label();
        emailError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px; -fx-padding: 5 0 0 0;");
        emailError.setVisible(false);

        Label passwordError = new Label();
        passwordError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px; -fx-padding: 5 0 0 0;");
        passwordError.setVisible(false);

        Label usernameLabel = new Label("Nom d'utilisateur");
        usernameLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label emailLabel = new Label("Adresse email");
        emailLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label fullNameLabel = new Label("Nom complet");
        fullNameLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label passwordLabel = new Label("Mot de passe");
        passwordLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label confirmLabel = new Label("Confirmation");
        confirmLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 13px;");

        int row = 1;
        grid.add(usernameLabel, 0, row);
        grid.add(usernameField, 1, row);
        grid.add(usernameError, 1, ++row);
        row++;

        grid.add(emailLabel, 0, row);
        grid.add(emailField, 1, row);
        grid.add(emailError, 1, ++row);
        row++;

        grid.add(fullNameLabel, 0, row);
        grid.add(fullNameField, 1, row);
        row++;

        grid.add(passwordLabel, 0, row);
        grid.add(passwordField, 1, row);
        grid.add(passwordError, 1, ++row);
        row++;

        grid.add(confirmLabel, 0, row);
        grid.add(confirmPasswordField, 1, row);
        row++;

        Label infoLabel = new Label("⚠️ Seuls les administrateurs peuvent être créés depuis cette interface.");
        infoLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-size: 11px; -fx-padding: 15 0 0 0;");
        GridPane.setColumnSpan(infoLabel, 2);
        grid.add(infoLabel, 0, row);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Créer l'administrateur");
        okButton.setStyle("-fx-background-color: #d4af37; -fx-background-radius: 8; -fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20; -fx-cursor: hand;");
        okButton.setDisable(true);

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setText("Annuler");
        cancelButton.setStyle("-fx-background-color: transparent; -fx-border-color: #dcdde1; -fx-border-radius: 8; -fx-text-fill: #7f8c8d; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20; -fx-cursor: hand;");

        addFocusEffect(usernameField);
        addFocusEffect(emailField);
        addFocusEffect(fullNameField);
        addFocusEffect(passwordField);
        addFocusEffect(confirmPasswordField);

        Runnable validate = () -> {
            boolean isValid = true;

            String username = usernameField.getText().trim();
            if (username.isEmpty()) {
                usernameError.setText("⚠ Nom d'utilisateur requis");
                usernameError.setVisible(true);
                isValid = false;
            } else if (username.length() < 3) {
                usernameError.setText("⚠ Minimum 3 caractères");
                usernameError.setVisible(true);
                isValid = false;
            } else {
                try {
                    if (userService.isUsernameTaken(username)) {
                        usernameError.setText("⚠ Ce nom d'utilisateur est déjà pris");
                        usernameError.setVisible(true);
                        isValid = false;
                    } else {
                        usernameError.setVisible(false);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            String email = emailField.getText().trim();
            if (email.isEmpty()) {
                emailError.setText("⚠ Email requis");
                emailError.setVisible(true);
                isValid = false;
            } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                emailError.setText("⚠ Format d'email invalide");
                emailError.setVisible(true);
                isValid = false;
            } else {
                try {
                    if (userService.isEmailTaken(email)) {
                        emailError.setText("⚠ Cet email est déjà utilisé");
                        emailError.setVisible(true);
                        isValid = false;
                    } else {
                        emailError.setVisible(false);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            String password = passwordField.getText();
            String confirm = confirmPasswordField.getText();

            if (password.isEmpty()) {
                passwordError.setText("⚠ Mot de passe requis");
                passwordError.setVisible(true);
                isValid = false;
            } else if (password.length() < 6) {
                passwordError.setText("⚠ Minimum 6 caractères");
                passwordError.setVisible(true);
                isValid = false;
            } else if (!password.equals(confirm)) {
                passwordError.setText("⚠ Les mots de passe ne correspondent pas");
                passwordError.setVisible(true);
                isValid = false;
            } else {
                passwordError.setVisible(false);
            }

            okButton.setDisable(!isValid);
        };

        usernameField.textProperty().addListener((obs, old, val) -> validate.run());
        emailField.textProperty().addListener((obs, old, val) -> validate.run());
        passwordField.textProperty().addListener((obs, old, val) -> validate.run());
        confirmPasswordField.textProperty().addListener((obs, old, val) -> validate.run());

        validate.run();

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                User newAdmin = new User();
                newAdmin.setUsername(usernameField.getText().trim());
                newAdmin.setEmail(emailField.getText().trim());
                newAdmin.setFullName(fullNameField.getText().trim());
                newAdmin.setPassword(passwordField.getText());
                newAdmin.setRole("ADMIN");
                newAdmin.setActive(true);
                return newAdmin;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                userService.insert(result);
                loadUsers();
                updateStats();
                updateCharts();

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Succès");
                successAlert.setHeaderText(null);
                successAlert.setContentText("✅ Administrateur ajouté avec succès!\n\nNom: " + result.getUsername() + "\nEmail: " + result.getEmail());
                successAlert.showAndWait();

            } catch (SQLException e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Erreur");
                errorAlert.setHeaderText(null);
                errorAlert.setContentText("❌ Impossible d'ajouter l'administrateur: " + e.getMessage());
                errorAlert.showAndWait();
            }
        });
    }

    private void addFocusEffect(Control control) {
        control.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                control.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #d4af37; -fx-border-radius: 10; -fx-border-width: 2; -fx-padding: 0 15; -fx-font-size: 14px;");
            } else {
                control.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dcdde1; -fx-border-radius: 10; -fx-padding: 0 15; -fx-font-size: 14px;");
            }
        });
    }

    /**
     * Dialogue pour modifier un utilisateur
     */
    private void showEditUserDialog(User user) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'utilisateur");
        dialog.setHeaderText("✏️ Modifier les informations de " + user.getUsername());

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 20, 20));

        TextField usernameField = new TextField(user.getUsername());
        TextField emailField = new TextField(user.getEmail());
        TextField fullNameField = new TextField(user.getFullName());

        Label roleLabel = new Label(user.getRole());
        roleLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold;");

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Laisser vide pour garder l'ancien");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirmer le nouveau mot de passe");

        Label passwordError = new Label();
        passwordError.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
        passwordError.setVisible(false);

        int row = 0;
        grid.add(new Label("Nom d'utilisateur:"), 0, row);
        grid.add(usernameField, 1, row);
        row++;
        grid.add(new Label("Email:"), 0, row);
        grid.add(emailField, 1, row);
        row++;
        grid.add(new Label("Nom complet:"), 0, row);
        grid.add(fullNameField, 1, row);
        row++;
        grid.add(new Label("Rôle:"), 0, row);
        grid.add(roleLabel, 1, row);
        row++;

        Separator separator = new Separator();
        separator.setPrefWidth(400);
        grid.add(separator, 0, row, 2, 1);
        row++;

        grid.add(new Label("Nouveau mot de passe:"), 0, row);
        grid.add(newPasswordField, 1, row);
        row++;
        grid.add(new Label("Confirmer:"), 0, row);
        grid.add(confirmPasswordField, 1, row);
        grid.add(passwordError, 1, ++row);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        Runnable validatePassword = () -> {
            String newPass = newPasswordField.getText();
            String confirm = confirmPasswordField.getText();

            if (!newPass.isEmpty() && !newPass.equals(confirm)) {
                passwordError.setText("Les mots de passe ne correspondent pas");
                passwordError.setVisible(true);
                okButton.setDisable(true);
            } else if (!newPass.isEmpty() && newPass.length() < 6) {
                passwordError.setText("Minimum 6 caractères");
                passwordError.setVisible(true);
                okButton.setDisable(true);
            } else {
                passwordError.setVisible(false);
                okButton.setDisable(false);
            }
        };

        newPasswordField.textProperty().addListener((obs, old, val) -> validatePassword.run());
        confirmPasswordField.textProperty().addListener((obs, old, val) -> validatePassword.run());

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                user.setUsername(usernameField.getText().trim());
                user.setEmail(emailField.getText().trim());
                user.setFullName(fullNameField.getText().trim());

                String newPassword = newPasswordField.getText();
                if (!newPassword.isEmpty()) {
                    user.setPassword(newPassword);
                }
                return user;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                userService.update(result);
                String newPassword = newPasswordField.getText();
                if (!newPassword.isEmpty()) {
                    userService.updatePassword(result.getId(), newPassword);
                }
                loadUsers();
                updateStats();
                updateCharts();
                showAlert("Succès", "Utilisateur modifié avec succès!");
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible de modifier: " + e.getMessage());
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleExportPDF() {
        try {
            ObservableList<User> usersToExport = usersTable.getItems();

            if (usersToExport.isEmpty()) {
                showAlert("Information", "Aucun utilisateur à exporter.");
                return;
            }

            boolean success = PDFExporter.exportUsersToPDF(usersToExport,
                    (Stage) usersTable.getScene().getWindow());

            if (success) {
                showAlert("Succès", "✅ Le fichier PDF a été généré avec succès !");
            } else {
                showAlert("Erreur", "❌ L'exportation a été annulée.");
            }

        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de l'exportation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExportStats() {
        try {
            int totalUsers = Integer.parseInt(statsTotalUsers.getText());
            int activeUsers = Integer.parseInt(statsActiveUsers.getText());
            int admins = Integer.parseInt(statsAdmins.getText());

            boolean success = PDFExporter.exportStatsToPDF(totalUsers, activeUsers, admins,
                    (Stage) usersTable.getScene().getWindow());

            if (success) {
                showAlert("Succès", "✅ Les statistiques ont été exportées avec succès !");
            }

        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de l'exportation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    private void handleFraudDetection() {
        try {
            List<FraudDetectionService.FraudReport> reports = new ArrayList<>();

            for (User user : userList) {
                FraudDetectionService.FraudReport report =
                        FraudDetectionService.getDetailedReport(user);
                if (report.isFraudulent()) {
                    reports.add(report);
                }
            }

            if (reports.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("🔐 Détection de fraude");
                alert.setHeaderText("Résultat de l'analyse");
                alert.setContentText("✅ Aucun compte suspect détecté !\n\nTous les comptes sont considérés comme légitimes.");
                alert.showAndWait();
                return;
            }

            // Créer un Dialog personnalisé
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("🔐 Détection de fraude");
            dialog.setHeaderText(null);

            // Appliquer le style CSS
            dialog.getDialogPane().getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm()
            );
            dialog.getDialogPane().getStyleClass().add("fraud-dialog");

            // Créer le contenu principal
            VBox mainContent = new VBox(15);
            mainContent.setStyle("-fx-background-color: #F8F5F0; -fx-padding: 20;");

            // En-tête
            Label headerLabel = new Label("🚨 " + reports.size() + " compte(s) suspect(s) détecté(s)");
            headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

            Label subHeaderLabel = new Label("Voici la liste des comptes présentant un comportement suspect :");
            subHeaderLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");

            mainContent.getChildren().addAll(headerLabel, subHeaderLabel);

            // ScrollPane pour contenir tous les comptes
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

            VBox accountsContainer = new VBox(15);
            accountsContainer.setStyle("-fx-padding: 10 0 10 0;");

            // Créer une carte pour chaque compte suspect
            for (FraudDetectionService.FraudReport report : reports) {
                VBox accountCard = createAccountCard(report);
                accountsContainer.getChildren().add(accountCard);
            }

            scrollPane.setContent(accountsContainer);
            scrollPane.setPrefHeight(450);
            mainContent.getChildren().add(scrollPane);

            // Boutons en bas
            HBox buttonBox = new HBox(15);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);
            buttonBox.setStyle("-fx-padding: 15 0 0 0;");

            Button exportButton = new Button("📄 Exporter le rapport");
            exportButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 10 20; -fx-cursor: hand;");
            exportButton.setOnAction(e -> exportFraudReport(reports));

            Button closeButton = new Button("Fermer");
            closeButton.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 10 20; -fx-cursor: hand;");
            closeButton.setOnAction(e -> dialog.close());

            buttonBox.getChildren().addAll(exportButton, closeButton);
            mainContent.getChildren().add(buttonBox);

            dialog.getDialogPane().setContent(mainContent);
            dialog.getDialogPane().setPrefWidth(700);
            dialog.getDialogPane().setPrefHeight(650);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            // Cacher le bouton par défaut
            Node closeButtonDefault = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
            closeButtonDefault.setVisible(false);

            dialog.showAndWait();

        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la détection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Crée une carte pour un compte suspect
     */
    private VBox createAccountCard(FraudDetectionService.FraudReport report) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        // En-tête de la carte avec le nom d'utilisateur et le score
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label usernameLabel = new Label("👤 " + report.getUsername());
        usernameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Badge de score
        Label scoreBadge = new Label(report.getRiskScore() + "%");
        scoreBadge.setStyle(
                "-fx-background-color: " + (report.getRiskScore() >= 70 ? "#e74c3c" : "#f39c12") + "; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 15;"
        );

        headerBox.getChildren().addAll(usernameLabel, spacer, scoreBadge);
        card.getChildren().add(headerBox);

        // Détails du compte
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(15);
        detailsGrid.setVgap(8);
        detailsGrid.setStyle("-fx-padding: 5 0 5 20;");

        // Email
        Label emailIcon = new Label("📧");
        emailIcon.setStyle("-fx-font-size: 12px;");
        Label emailLabel = new Label(report.getEmail());
        emailLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");
        detailsGrid.add(emailIcon, 0, 0);
        detailsGrid.add(emailLabel, 1, 0);

        // Niveau de risque
        Label riskIcon = new Label("⚠️");
        riskIcon.setStyle("-fx-font-size: 12px;");
        Label riskLabel = new Label(report.getRiskLevelLabel());
        riskLabel.setStyle(
                "-fx-text-fill: " + (report.getRiskLevelLabel().contains("CRITIQUE") ? "#e74c3c" : "#f39c12") + "; " +
                        "-fx-font-weight: bold; -fx-font-size: 12px;"
        );
        detailsGrid.add(riskIcon, 0, 1);
        detailsGrid.add(riskLabel, 1, 1);

        card.getChildren().add(detailsGrid);

        // Drapeaux rouges (si présents)
        if (report.getRedFlags() != null && !report.getRedFlags().isEmpty()) {
            Separator sep = new Separator();
            sep.setStyle("-fx-background-color: #e0e0e0;");
            card.getChildren().add(sep);

            Label flagsTitle = new Label("🚩 Drapeaux rouges :");
            flagsTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c; -fx-font-size: 12px;");
            card.getChildren().add(flagsTitle);

            VBox flagsBox = new VBox(5);
            flagsBox.setStyle("-fx-padding: 0 0 0 20;");
            for (String flag : report.getRedFlags()) {
                Label flagLabel = new Label("• " + flag);
                flagLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");
                flagsBox.getChildren().add(flagLabel);
            }
            card.getChildren().add(flagsBox);
        }

        // Boutons d'action
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.setStyle("-fx-padding: 10 0 0 0;");

        Button viewButton = new Button("👁️ Voir le compte");
        viewButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 5 15; -fx-cursor: hand;");
        viewButton.setOnAction(e -> {
            User selectedUser = userList.stream()
                    .filter(u -> u.getId() == report.getId())
                    .findFirst()
                    .orElse(null);
            if (selectedUser != null) {
                usersTable.getSelectionModel().select(selectedUser);
                usersTable.scrollTo(selectedUser);
                // Fermer le dialog
                Stage stage = (Stage) viewButton.getScene().getWindow();
                stage.close();
            }
        });

        Button disableButton = new Button("🔒 Désactiver");
        disableButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 5 15; -fx-cursor: hand;");
        disableButton.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText(null);
            confirm.setContentText("Êtes-vous sûr de vouloir désactiver le compte " + report.getUsername() + " ?");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    userService.deactivate(report.getId());
                    loadUsers();
                    updateStats();
                    showAlert("Succès", "✅ Compte désactivé avec succès !");
                    // Fermer le dialog
                    Stage stage = (Stage) disableButton.getScene().getWindow();
                    stage.close();
                } catch (SQLException ex) {
                    showAlert("Erreur", ex.getMessage());
                }
            }
        });

        actionBox.getChildren().addAll(viewButton, disableButton);
        card.getChildren().add(actionBox);

        return card;
    }

    /**
     * Exporte le rapport de fraude
     */
    private void exportFraudReport(List<FraudDetectionService.FraudReport> reports) {
        StringBuilder sb = new StringBuilder();
        sb.append("RAPPORT DE DÉTECTION DE FRAUDE\n");
        sb.append("=".repeat(50)).append("\n");
        sb.append("Date: ").append(new Date()).append("\n\n");

        for (FraudDetectionService.FraudReport report : reports) {
            sb.append("ID: ").append(report.getId()).append("\n");
            sb.append("Utilisateur: ").append(report.getUsername()).append("\n");
            sb.append("Email: ").append(report.getEmail()).append("\n");
            sb.append("Score de risque: ").append(report.getRiskScore()).append("%\n");
            sb.append("Niveau: ").append(report.getRiskLevelLabel()).append("\n");
            if (report.getRedFlags() != null && !report.getRedFlags().isEmpty()) {
                sb.append("Drapeaux rouges:\n");
                for (String flag : report.getRedFlags()) {
                    sb.append("  - ").append(flag).append("\n");
                }
            }
            sb.append("\n").append("-".repeat(50)).append("\n");
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter le rapport");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichier texte", "*.txt")
        );
        fileChooser.setInitialFileName("rapport_fraude_" + System.currentTimeMillis() + ".txt");

        File file = fileChooser.showSaveDialog(usersTable.getScene().getWindow());
        if (file != null) {
            try (java.io.FileWriter fw = new java.io.FileWriter(file)) {
                fw.write(sb.toString());
                showAlert("Succès", "✅ Rapport exporté avec succès !");
            } catch (Exception e) {
                showAlert("Erreur", "Erreur lors de l'export: " + e.getMessage());
            }
        }
    }







}
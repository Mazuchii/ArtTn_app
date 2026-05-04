package tn.esprit.museum.controllers;

import javafx.scene.layout.FlowPane;
import tn.esprit.museum.services.CategoryServices;
import tn.esprit.museum.entities.Category;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class ForumCategoriesController {

    @FXML private FlowPane categoriesContainer;
    @FXML private Label messageLabel;
    private UserHomeController parentController;
    private VBox forumView;


    private CategoryServices categoryService;

    @FXML
    public void initialize() {
        categoryService = new CategoryServices();
        loadCategories();
    }
    public void setParentController(UserHomeController parent) {
        this.parentController = parent;
        this.forumView = UserHomeController.getForumView(); // ← Récupère la référence statique
        System.out.println("✅ forumView reçu: " + (forumView != null));
    }

    private void loadCategories() {
        List<Category> categories = categoryService.getAllCategories();
        categoriesContainer.getChildren().clear();

        double cardWidth = 250;

        for (Category category : categories) {
            VBox card = createCategoryCard(category, cardWidth);
            categoriesContainer.getChildren().add(card);
        }

        if (categories.isEmpty()) {
            showMessage("Aucune catégorie disponible", "info");
        }
    }

    private VBox createCategoryCard(Category category, double cardWidth) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);");
        card.setPrefWidth(cardWidth);
        card.setPrefHeight(220);
        card.setMaxWidth(cardWidth);

        Label iconLabel = new Label(category.getIcon());
        iconLabel.setStyle("-fx-font-size: 52px;");

        Label nameLabel = new Label(category.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");
        nameLabel.setWrapText(true);

        Text descText = new Text(category.getDescription());
        descText.setStyle("-fx-fill: #7f8c8d; -fx-font-size: 11px;");
        descText.setWrappingWidth(cardWidth - 40);

        Button browseBtn = new Button("Parcourir →");
        browseBtn.setStyle(
                "-fx-background-color: #f1f5f9; " +
                        "-fx-text-fill: #475569; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 8 20; " +
                        "-fx-background-radius: 25; " +
                        "-fx-cursor: hand;"
        );
        browseBtn.setOnMouseEntered(e -> browseBtn.setStyle(
                "-fx-background-color: #3b82f6; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 8 20; " +
                        "-fx-background-radius: 25; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(59,130,246,0.3), 5, 0, 0, 1);"
        ));
        browseBtn.setOnMouseExited(e -> browseBtn.setStyle(
                "-fx-background-color: #f1f5f9; " +
                        "-fx-text-fill: #475569; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 8 20; " +
                        "-fx-background-radius: 25; " +
                        "-fx-cursor: hand;"
        ));
        browseBtn.setOnAction(e -> openCategoryPosts(category.getId(), category.getName()));

        card.getChildren().addAll(iconLabel, nameLabel, descText, browseBtn);
        return card;
    }

    private void openCategoryPosts(int categoryId, String categoryName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/forum_posts.fxml"));
            Parent postsView = loader.load();

            ForumPostsController postsCtrl = loader.getController();
            postsCtrl.setCategory(categoryId, categoryName);

            // ⚠️ CRUCIAL : passer le parentController
            if (parentController != null) {
                postsCtrl.setParentController(parentController);
            }

            // ✅ Récupérer forumView via lookup si on n'a pas le parent
            VBox forumView = null;
            if (categoriesContainer.getScene() != null) {
                forumView = (VBox) categoriesContainer.getScene().lookup("#forumView");
            }
            
            if (forumView == null) {
                forumView = UserHomeController.getForumView();
            }

            if (parentController != null) {
                parentController.showForumPosts(postsView);
                System.out.println("✅ forumView mis à jour via parentController");
            } else if (forumView != null) {
                forumView.getChildren().clear();
                forumView.getChildren().add(postsView);
                VBox.setVgrow(postsView, javafx.scene.layout.Priority.ALWAYS);
                System.out.println("✅ forumView mis à jour directement");
            } else {
                System.err.println("❌ forumView introuvable !");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToHome() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/user_home.fxml"));
            Stage stage = (Stage) categoriesContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Erreur : impossible de charger l'accueil", "error");
        }
    }

    @FXML
    private void handleGoToBoutique() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/user_home.fxml"));
            Stage stage = (Stage) categoriesContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
            // Optionnel : tu peux ajouter un paramètre pour ouvrir directement la boutique
        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Erreur : impossible de charger la boutique", "error");
        }
    }

    @FXML
    private void handleGoToForum() {
        // Déjà sur le forum, on recharge la page des catégories
        loadCategories();
        showMessage("Vous êtes déjà sur le forum", "info");
    }

    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.setStyle(type.equals("error") ? "-fx-text-fill: red;" : "-fx-text-fill: #3498db;");
        messageLabel.setVisible(true);
    }
}

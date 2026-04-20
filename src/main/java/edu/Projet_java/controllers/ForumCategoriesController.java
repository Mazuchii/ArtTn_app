package edu.Projet_java.controllers;

import edu.Projet_java.entities.Category;
import edu.Projet_java.services.CategoryServices;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class ForumCategoriesController {

    @FXML private FlowPane categoriesContainer;
    @FXML private Label messageLabel;

    private CategoryServices categoryService;

    @FXML
    public void initialize() {
        categoryService = new CategoryServices();
        loadCategories();
    }

    private void loadCategories() {
        List<Category> categories = categoryService.getAllCategories();
        categoriesContainer.getChildren().clear();

        // Largeur fixe pour chaque carte
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

        // Icône
        Label iconLabel = new Label(category.getIcon());
        iconLabel.setStyle("-fx-font-size: 52px;");

        // Nom
        Label nameLabel = new Label(category.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");
        nameLabel.setWrapText(true);

        // Description
        Text descText = new Text(category.getDescription());
        descText.setStyle("-fx-fill: #7f8c8d; -fx-font-size: 11px;");
        descText.setWrappingWidth(cardWidth - 40);

        // Bouton
        Button browseBtn = new Button("Parcourir →");
        browseBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 25; -fx-cursor: hand; -fx-padding: 8 16;");
        browseBtn.setOnAction(e -> openCategoryPosts(category.getId(), category.getName()));

        card.getChildren().addAll(iconLabel, nameLabel, descText, browseBtn);
        return card;
    }

    private void openCategoryPosts(int categoryId, String categoryName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/forum_posts.fxml"));
            Parent root = loader.load();

            ForumPostsController controller = loader.getController();
            controller.setCategory(categoryId, categoryName);

            Stage stage = (Stage) categoriesContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Forum - " + categoryName);

        } catch (IOException e) {
            showMessage("Erreur: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.setStyle(type.equals("error") ? "-fx-text-fill: red;" : "-fx-text-fill: #3498db;");
        messageLabel.setVisible(true);
    }
}
package edu.Projet_java.controllers;

import edu.Projet_java.entities.Posts;
import edu.Projet_java.services.PostServices;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ForumEditPostController {

    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private Label errorLabel;

    private PostServices postService;
    private Posts currentPost;
    private ForumPostDetailController parentController;

    @FXML
    public void initialize() {
        postService = new PostServices();
    }

    public void setPost(Posts post, ForumPostDetailController parent) {
        this.currentPost = post;
        this.parentController = parent;
        titleField.setText(post.getPost_titre());
        contentArea.setText(post.getPost_contenu());
    }

    @FXML
    private void handleSave() {
        String title = titleField.getText().trim();
        String content = contentArea.getText().trim();

        // ✅ CONTROLE DE SAISIE
        if (title.isEmpty()) {
            showError("❌ Le titre ne peut pas être vide");
            return;
        }
        if (title.length() > 100) {
            showError("❌ Le titre ne doit pas dépasser 100 caractères");
            return;
        }
        if (title.length() < 3) {
            showError("❌ Le titre doit contenir au moins 3 caractères");
            return;
        }

        if (content.isEmpty()) {
            showError("❌ Le contenu ne peut pas être vide");
            return;
        }
        if (content.length() < 10) {
            showError("❌ Le contenu doit contenir au moins 10 caractères");
            return;
        }
        if (content.length() > 5000) {
            showError("❌ Le contenu ne doit pas dépasser 5000 caractères");
            return;
        }

        // Vérifier si le contenu a vraiment changé
        if (title.equals(currentPost.getPost_titre()) && content.equals(currentPost.getPost_contenu())) {
            showError("❌ Aucune modification détectée");
            return;
        }

        try {
            currentPost.setPost_titre(title);
            currentPost.setPost_contenu(content);
            postService.updatepost(currentPost.getPost_id(), currentPost);
            showSuccess("✅ Post modifié avec succès !");

            if (parentController != null) {
                Posts updatedPost = postService.getPostById(currentPost.getPost_id());
                parentController.refreshAfterPostEdit(updatedPost);
            }

            new Thread(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
                javafx.application.Platform.runLater(this::closeWindow);
            }).start();

        } catch (Exception e) {
            showError("❌ Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: green;");
        errorLabel.setVisible(true);
    }
}
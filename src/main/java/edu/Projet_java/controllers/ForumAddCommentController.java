package edu.Projet_java.controllers;

import edu.Projet_java.entities.commentaire;
import edu.Projet_java.services.CommentServices;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ForumAddCommentController {

    @FXML private TextArea commentArea;
    @FXML private Label errorLabel;

    private CommentServices commentService;
    private int postId;
    private ForumPostDetailController parentController;

    @FXML
    public void initialize() {
        commentService = new CommentServices();
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public void setParentController(ForumPostDetailController parent) {
        this.parentController = parent;
    }

    @FXML
    private void handleSave() {
        String content = commentArea.getText().trim();

        // ✅ CONTROLE DE SAISIE
        if (content.isEmpty()) {
            showError("❌ Le commentaire ne peut pas être vide");
            return;
        }
        if (content.length() < 3) {
            showError("❌ Le commentaire doit contenir au moins 3 caractères");
            return;
        }
        if (content.length() > 1000) {
            showError("❌ Le commentaire ne doit pas dépasser 1000 caractères");
            return;
        }

        try {
            commentaire comment = new commentaire(postId, content, 1);
            commentService.addComment(comment);
            showSuccess("✅ Commentaire ajouté avec succès !");

            if (parentController != null) {
                parentController.refreshAfterCommentAdded();
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
        Stage stage = (Stage) commentArea.getScene().getWindow();
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
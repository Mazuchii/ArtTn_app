package tn.esprit.museum.controllers;

import tn.esprit.museum.entities.commentaire;
import tn.esprit.museum.services.CommentServices;
import tn.esprit.museum.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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
        // Vérifier si l'utilisateur est connecté
        if (!SessionManager.isLoggedIn()) {
            showError("❌ Veuillez vous connecter pour commenter");
            return;
        }

        int currentUserId = SessionManager.getCurrentUserId();
        if (currentUserId == -1) {
            showError("❌ Erreur: Utilisateur non trouvé");
            return;
        }

        String content = commentArea.getText().trim();

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
            // ✅ Utilise le vrai userId
            commentaire comment = new commentaire(postId, content, currentUserId);
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
        errorLabel.setStyle("-fx-text-fill: #ef4444;");
        errorLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #10b981;");
        errorLabel.setVisible(true);
    }
}
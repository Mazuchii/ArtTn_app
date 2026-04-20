package edu.Projet_java.controllers;


import edu.Projet_java.entities.Posts;
import edu.Projet_java.entities.commentaire;
import edu.Projet_java.services.PostServices;
import edu.Projet_java.services.CommentServices;
import edu.Projet_java.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.io.File;


public class ForumPostDetailController {

    @FXML private Label postTitleLabel;
    @FXML private Label postDateLabel;
    @FXML private Label postAuthorLabel;
    @FXML private Label postContentLabel;
    @FXML private ListView<VBox> commentsListView;
    @FXML private Label messageLabel;

    @FXML private Label likesCountLabel;
    @FXML private Label dislikesCountLabel;
    @FXML private Label viewsCountLabel;
    @FXML private Button likeButton;
    @FXML private Button dislikeButton;
    @FXML private ImageView postImageView;

    private Posts currentPost;
    private PostServices postService;
    private CommentServices commentService;
    private ObservableList<VBox> commentItems;
    private ForumPostsController parentController;

    private int currentUserId = 1; // ID par défaut (à remplacer par l'utilisateur connecté)
    private String userReaction = null;

    @FXML
    public void initialize() {
        postService = new PostServices();
        commentService = new CommentServices();
        commentItems = FXCollections.observableArrayList();
        commentsListView.setItems(commentItems);

        // Récupérer l'ID de l'utilisateur connecté
        currentUserId = SessionManager.getCurrentUserId();
    }

    public void setPost(Posts post, ForumPostsController parent) {
        this.currentPost = post;
        this.parentController = parent;
        displayPostDetails();
        loadComments();

        // Gérer la vue unique
        handleView();

        // Gérer l'affichage des likes/dislikes existants
        loadUserReaction();
    }

    private void handleView() {
        // Vérifier si l'utilisateur a déjà vu ce post
        if (!postService.hasUserViewedPost(currentPost.getPost_id(), currentUserId)) {
            // Première vue : incrémenter et enregistrer
            postService.addView(currentPost.getPost_id(), currentUserId);
            currentPost.setViews(currentPost.getViews() + 1);
            if (viewsCountLabel != null) {
                viewsCountLabel.setText(String.valueOf(currentPost.getViews()));
            }
        }
    }

    private void loadUserReaction() {
        userReaction = postService.getUserReaction(currentPost.getPost_id(), currentUserId);
        updateButtonsStyle();
    }

    private void updateButtonsStyle() {
        if (likeButton == null || dislikeButton == null) return;

        if ("LIKE".equals(userReaction)) {
            // Like ACTIF : BLEU
            likeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #3b82f6; -fx-font-size: 16px; -fx-cursor: hand; -fx-font-weight: bold;");
            // Dislike INACTIF : GRIS
            dislikeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-font-size: 16px; -fx-cursor: hand;");
        } else if ("DISLIKE".equals(userReaction)) {
            // Like INACTIF : GRIS
            likeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-font-size: 16px; -fx-cursor: hand;");
            // Dislike ACTIF : ROUGE
            dislikeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-size: 16px; -fx-cursor: hand; -fx-font-weight: bold;");
        } else {
            // Aucune réaction : les deux GRIS
            likeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-font-size: 16px; -fx-cursor: hand;");
            dislikeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-font-size: 16px; -fx-cursor: hand;");
        }
    }

    private void displayPostDetails() {
        postTitleLabel.setText(currentPost.getPost_titre());
        postContentLabel.setText(currentPost.getPost_contenu());
        postAuthorLabel.setText("👤 Auteur #" + currentPost.getAuthor_id());
        postDateLabel.setText("📅 " + currentPost.getFormattedDate());

        if (likesCountLabel != null) {
            likesCountLabel.setText(String.valueOf(currentPost.getLikes()));
            likesCountLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
        }
        if (dislikesCountLabel != null) {
            dislikesCountLabel.setText(String.valueOf(currentPost.getDislikes()));
            dislikesCountLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");

        }
        if (viewsCountLabel != null) {
            viewsCountLabel.setText(String.valueOf(currentPost.getViews()));
            viewsCountLabel.setStyle("-fx-text-fill: #6b7280;");
        }

        displayImage();
    }

    private void displayImage() {
        if (postImageView == null) return;

        String imagePath = currentPost.getImageUrl();
        System.out.println("🔍 Chemin de l'image: " + imagePath); // DEBUG

        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                Image image = null;

                // Méthode 1: Charger depuis les ressources (recommandée)
                if (imagePath.startsWith("/images/")) {
                    try {
                        // Essayer de charger depuis les ressources
                        var resource = getClass().getResource(imagePath);
                        if (resource != null) {
                            image = new Image(resource.toExternalForm());
                            System.out.println("✅ Image chargée depuis ressources: " + resource.toExternalForm());
                        } else {
                            System.out.println("❌ Ressource non trouvée: " + imagePath);
                        }
                    } catch (Exception e) {
                        System.out.println("Erreur chargement ressource: " + e.getMessage());
                    }
                }

                // Méthode 2: Charger depuis le système de fichiers (fallback)
                if (image == null || image.isError()) {
                    String basePath = System.getProperty("user.dir");
                    String fullPath = basePath + "/src/main/resources" + imagePath;
                    File file = new File(fullPath);
                    if (file.exists()) {
                        image = new Image(file.toURI().toString());
                        System.out.println("✅ Image chargée depuis fichier: " + fullPath);
                    } else {
                        System.out.println("❌ Fichier non trouvé: " + fullPath);
                    }
                }

                // Méthode 3: Si c'est un chemin absolu déjà
                if (image == null || image.isError()) {
                    try {
                        image = new Image(imagePath);
                        System.out.println("✅ Image chargée depuis chemin absolu");
                    } catch (Exception e) {
                        System.out.println("Erreur chargement absolu: " + e.getMessage());
                    }
                }

                if (image != null && !image.isError()) {
                    postImageView.setImage(image);
                    postImageView.setVisible(true);
                    postImageView.setManaged(true);
                    System.out.println("✅ Image affichée avec succès");
                } else {
                    postImageView.setVisible(false);
                    postImageView.setManaged(false);
                    System.out.println("❌ Échec chargement image");
                }
            } catch (Exception e) {
                postImageView.setVisible(false);
                postImageView.setManaged(false);
                System.err.println("❌ Erreur affichage image: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            postImageView.setVisible(false);
            postImageView.setManaged(false);
            System.out.println("Aucune image associée à ce post");
        }
    }

    private void loadComments() {
        try {
            List<commentaire> comments = commentService.getCommentsByPostId(currentPost.getPost_id());

            commentItems.clear();
            for (commentaire c : comments) {
                commentItems.add(createCommentCard(c));
            }

            if (comments.isEmpty()) {
                showMessage("Aucun commentaire. Soyez le premier à commenter!", "info");
            }
        } catch (Exception e) {
            showMessage("Erreur: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    private VBox createCommentCard(commentaire comment) {
        VBox card = new VBox(8);
        card.getStyleClass().add("comment-card");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox metaBox = new HBox(10);
        Label authorLabel = new Label("👤 Auteur #" + comment.getAuthor_id());
        authorLabel.getStyleClass().add("comment-author");
        metaBox.getChildren().add(authorLabel);

        Label textLabel = new Label(comment.getContenu());
        textLabel.getStyleClass().add("comment-text");

        HBox actionsBox = new HBox(10);
        actionsBox.getStyleClass().add("comment-actions");

        Button editButton = new Button("✏️ Modifier");
        editButton.getStyleClass().add("comment-action-btn");
        editButton.setOnAction(e -> openEditCommentDialog(comment));

        Button deleteButton = new Button("🗑️ Supprimer");
        deleteButton.getStyleClass().add("comment-action-btn");
        deleteButton.setOnAction(e -> deleteComment(comment));

        actionsBox.getChildren().addAll(editButton, deleteButton);

        card.getChildren().addAll(metaBox, textLabel, actionsBox);
        return card;
    }

    @FXML
    private void handleLike() {
        boolean wasLiked = "LIKE".equals(userReaction);

        postService.addReaction(currentPost.getPost_id(), currentUserId, "LIKE");

        if (wasLiked) {
            // Annulation du like
            currentPost.setLikes(currentPost.getLikes() - 1);
            likesCountLabel.setText(String.valueOf(currentPost.getLikes()));
            userReaction = null;
            showMessage("👍 Like retiré", "info");
        } else {
            // Ajout d'un like
            if ("DISLIKE".equals(userReaction)) {
                currentPost.setDislikes(currentPost.getDislikes() - 1);
                dislikesCountLabel.setText(String.valueOf(currentPost.getDislikes()));
            }
            currentPost.setLikes(currentPost.getLikes() + 1);
            likesCountLabel.setText(String.valueOf(currentPost.getLikes()));
            userReaction = "LIKE";
            showMessage("👍 Vous avez aimé ce post !", "success");
        }

        updateButtonsStyle();

        if (parentController != null) {
            parentController.refreshPosts();
        }
    }

    @FXML
    private void handleDislike() {
        boolean wasDisliked = "DISLIKE".equals(userReaction);

        postService.addReaction(currentPost.getPost_id(), currentUserId, "DISLIKE");

        if (wasDisliked) {
            // Annulation du dislike
            currentPost.setDislikes(currentPost.getDislikes() - 1);
            dislikesCountLabel.setText(String.valueOf(currentPost.getDislikes()));
            userReaction = null;
            showMessage("👎 Dislike retiré", "info");
        } else {
            // Ajout d'un dislike
            if ("LIKE".equals(userReaction)) {
                currentPost.setLikes(currentPost.getLikes() - 1);
                likesCountLabel.setText(String.valueOf(currentPost.getLikes()));
            }
            currentPost.setDislikes(currentPost.getDislikes() + 1);
            dislikesCountLabel.setText(String.valueOf(currentPost.getDislikes()));
            userReaction = "DISLIKE";
            showMessage("👎 Vous n'avez pas aimé ce post !", "success");
        }

        updateButtonsStyle();

        if (parentController != null) {
            parentController.refreshPosts();
        }
    }

    private void openEditCommentDialog(commentaire comment) {
        TextInputDialog dialog = new TextInputDialog(comment.getContenu());
        dialog.setTitle("Modifier le commentaire");
        dialog.setHeaderText("Modifier votre commentaire");
        dialog.setContentText("Nouveau texte:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String newText = result.get().trim();

            if (newText.isEmpty()) {
                showMessage("❌ Le commentaire ne peut pas être vide", "error");
                return;
            }
            if (newText.length() < 3) {
                showMessage("❌ Le commentaire doit contenir au moins 3 caractères", "error");
                return;
            }
            if (newText.length() > 1000) {
                showMessage("❌ Le commentaire ne doit pas dépasser 1000 caractères", "error");
                return;
            }

            if (newText.equals(comment.getContenu())) {
                showMessage("❌ Aucune modification détectée", "error");
                return;
            }

            try {
                comment.setContenu(newText);
                commentService.updateComment(comment.getComment_id(), comment);
                showMessage("✅ Commentaire modifié avec succès !", "success");
                loadComments();
                if (parentController != null) {
                    parentController.refreshPosts();
                }
            } catch (Exception e) {
                showMessage("❌ Erreur: " + e.getMessage(), "error");
            }
        }
    }

    private void deleteComment(commentaire comment) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer ce commentaire ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                commentService.deleteComment(comment);
                showMessage("✅ Commentaire supprimé !", "success");
                loadComments();
                if (parentController != null) {
                    parentController.refreshPosts();
                }
            } catch (Exception e) {
                showMessage("❌ Erreur: " + e.getMessage(), "error");
            }
        }
    }

    @FXML
    private void handleAddComment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/forum_add_comment.fxml"));
            Parent root = loader.load();

            ForumAddCommentController controller = loader.getController();
            controller.setPostId(currentPost.getPost_id());
            controller.setParentController(this);

            Stage stage = new Stage();
            stage.setTitle("Ajouter un commentaire");
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            showMessage("Erreur: " + e.getMessage(), "error");
        }
    }

    public void refreshAfterCommentAdded() {
        loadComments();
        if (parentController != null) {
            parentController.refreshPosts();
        }
    }

    @FXML
    private void handleEditPost() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/forum_edit_post.fxml"));
            Parent root = loader.load();

            ForumEditPostController controller = loader.getController();
            controller.setPost(currentPost, this);

            Stage stage = new Stage();
            stage.setTitle("Modifier le post");
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            showMessage("Erreur: " + e.getMessage(), "error");
        }
    }

    public void refreshAfterPostEdit(Posts updatedPost) {
        this.currentPost = updatedPost;
        displayPostDetails();
        if (parentController != null) parentController.refreshPosts();
    }

    @FXML
    private void handleDeletePost() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer ce post ?\nTous les commentaires seront également supprimés.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                postService.deletepost(currentPost.getPost_id());
                showMessage("✅ Post supprimé avec succès !", "success");
                if (parentController != null) parentController.refreshPosts();
                handleBack();
            } catch (Exception e) {
                showMessage("❌ Erreur: " + e.getMessage(), "error");
            }
        }
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) postTitleLabel.getScene().getWindow();
        stage.close();
    }

    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        if (type.equals("error")) {
            messageLabel.setStyle("-fx-text-fill: #ef4444;");
        } else if (type.equals("success")) {
            messageLabel.setStyle("-fx-text-fill: #10b981;");
        } else {
            messageLabel.setStyle("-fx-text-fill: #3b82f6;");
        }
        messageLabel.setVisible(true);

        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> messageLabel.setVisible(false));
        }).start();
    }
}
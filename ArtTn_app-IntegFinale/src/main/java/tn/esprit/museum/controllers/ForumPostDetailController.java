package tn.esprit.museum.controllers;



import tn.esprit.museum.entities.Posts;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.entities.commentaire;
import tn.esprit.museum.services.PostServices;
import tn.esprit.museum.services.CommentServices;
import tn.esprit.museum.services.TranslationService;
import tn.esprit.museum.services.UserService;
import tn.esprit.museum.utils.SessionManager;
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
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.geometry.Insets;


import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
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
    @FXML private Button translateButton;
    @FXML private Button translateCommentsButton;
    @FXML private Button originalButton;
    @FXML private HBox translationButtons;
    @FXML private ComboBox<String> languageCombo;


    private Posts currentPost;
    private PostServices postService;
    private CommentServices commentService;
    private UserService userService;
    private ObservableList<VBox> commentItems;
    private tn.esprit.museum.controllers.ForumPostsController parentController;
    private String originalTitle;
    private String originalContent;
    private List<String> originalCommentsText = new ArrayList<>();
    private List<commentaire> originalCommentsList = new ArrayList<>();
    private boolean isTranslated = false;
    private String selectedLanguage = "Français";

    private int currentUserId = 1; // ID par défaut (à remplacer par l'utilisateur connecté)
    private String userReaction = null;

    @FXML
    public void initialize() {
        postService = new PostServices();
        commentService = new CommentServices();
        userService = new UserService();
        commentItems = FXCollections.observableArrayList();
        commentsListView.setItems(commentItems);

        // Récupérer l'ID de l'utilisateur connecté
        currentUserId = SessionManager.getCurrentUserId();
        if (translationButtons != null) {
            translationButtons.setVisible(true);
        }
        languageCombo.getItems().addAll("Français", "English");
        languageCombo.setValue("Français");
        languageCombo.valueProperty().addListener((obs, oldLang, newLang) -> {
            selectedLanguage = newLang;
        });

    }

    public void setPost(Posts post, tn.esprit.museum.controllers.ForumPostsController parent) {
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
        originalTitle = currentPost.getPost_titre();
        originalContent = currentPost.getPost_contenu();

        postTitleLabel.setText(currentPost.getPost_titre());
        postContentLabel.setText(currentPost.getPost_contenu());
        
        // Récupérer le nom réel de l'auteur
        String authorName = getUserNameById(currentPost.getAuthor_id());
        postAuthorLabel.setText("👤 " + authorName);
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

            // ✅ Sauvegarder les commentaires originaux
            originalCommentsList.clear();
            originalCommentsList.addAll(comments);

            commentItems.clear();
            for (commentaire c : comments) {
                commentItems.add(createCommentCard(c, false));
            }

            if (comments.isEmpty()) {
                showMessage("Aucun commentaire. Soyez le premier à commenter!", "info");
            }
        } catch (Exception e) {
            showMessage("Erreur: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    private VBox createCommentCard(commentaire comment, boolean isTranslated) {
        VBox card = new VBox(12);
        card.getStyleClass().add("comment-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPadding(new Insets(16));

        // En-tête du commentaire avec avatar et nom
        HBox headerBox = new HBox(12);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Avatar circulaire
        Label avatarLabel = new Label("👤");
        avatarLabel.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; " +
                           "-fx-background-radius: 50%; -fx-padding: 8 12; " +
                           "-fx-font-size: 16px; -fx-min-width: 40px; -fx-min-height: 40px; " +
                           "-fx-alignment: center;");
        
        // Récupérer le nom réel de l'auteur
        String authorName = getUserNameById(comment.getAuthor_id());
        
        VBox authorInfo = new VBox(2);
        Label authorLabel = new Label(authorName);
        authorLabel.getStyleClass().add("comment-author");
        authorLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        
        Label timeLabel = new Label("À l'instant");
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
        
        authorInfo.getChildren().addAll(authorLabel, timeLabel);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        headerBox.getChildren().addAll(avatarLabel, authorInfo, spacer);

        // Texte du commentaire avec meilleur style
        Label textLabel = new Label(isTranslated ? comment.getContenuTraduit() : comment.getContenu());
        textLabel.getStyleClass().add("comment-text");
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-line-spacing: 3px; " +
                          "-fx-padding: 8 0 8 0;");

        // Séparateur subtil
        javafx.scene.shape.Line separator = new javafx.scene.shape.Line();
        separator.setEndX(400);
        separator.setStroke(javafx.scene.paint.Color.web("#e5e7eb"));
        separator.setStrokeWidth(1);

        // Actions avec icônes améliorées
        HBox actionsBox = new HBox(12);
        actionsBox.getStyleClass().add("comment-actions");
        actionsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Button editButton = new Button("✏️ Modifier");
        editButton.getStyleClass().add("comment-action-btn");
        editButton.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280; " +
                           "-fx-font-size: 12px; -fx-padding: 6 14; -fx-background-radius: 20; " +
                           "-fx-cursor: hand; -fx-font-weight: 600;");
        editButton.setOnMouseEntered(e -> editButton.setStyle(
            "-fx-background-color: #3b82f6; -fx-text-fill: white; " +
            "-fx-font-size: 12px; -fx-padding: 6 14; -fx-background-radius: 20; " +
            "-fx-cursor: hand; -fx-font-weight: 600;"));
        editButton.setOnMouseExited(e -> editButton.setStyle(
            "-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280; " +
            "-fx-font-size: 12px; -fx-padding: 6 14; -fx-background-radius: 20; " +
            "-fx-cursor: hand; -fx-font-weight: 600;"));
        editButton.setOnAction(e -> openEditCommentDialog(comment));

        Button deleteButton = new Button("🗑️ Supprimer");
        deleteButton.getStyleClass().add("comment-action-btn");
        deleteButton.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; " +
                             "-fx-font-size: 12px; -fx-padding: 6 14; -fx-background-radius: 20; " +
                             "-fx-cursor: hand; -fx-font-weight: 600;");
        deleteButton.setOnMouseEntered(e -> deleteButton.setStyle(
            "-fx-background-color: #dc2626; -fx-text-fill: white; " +
            "-fx-font-size: 12px; -fx-padding: 6 14; -fx-background-radius: 20; " +
            "-fx-cursor: hand; -fx-font-weight: 600;"));
        deleteButton.setOnMouseExited(e -> deleteButton.setStyle(
            "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; " +
            "-fx-font-size: 12px; -fx-padding: 6 14; -fx-background-radius: 20; " +
            "-fx-cursor: hand; -fx-font-weight: 600;"));
        deleteButton.setOnAction(e -> deleteComment(comment));

        actionsBox.getChildren().addAll(editButton, deleteButton);

        card.getChildren().addAll(headerBox, textLabel, separator, actionsBox);
        
        // Style de la carte amélioré
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                     "-fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-border-width: 1; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");
        
        return card;
    }
    
    // Méthode utilitaire pour récupérer le nom d'utilisateur
    private String getUserNameById(int userId) {
        try {
            User user = userService.getById(userId);
            if (user != null && user.getFullName() != null && !user.getFullName().isEmpty()) {
                return user.getFullName();
            } else if (user != null && user.getUsername() != null) {
                return user.getUsername();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du nom d'utilisateur: " + e.getMessage());
        }
        return "Utilisateur #" + userId;
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

    @FXML
    private void handleSpeakPost() {
        String textToRead = currentPost.getPost_titre() + ". " + currentPost.getPost_contenu();

        new Thread(() -> {
            try {
                String voiceCommand = getVoiceCommand(textToRead, selectedLanguage);
                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", voiceCommand);
                pb.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    private String getVoiceCommand(String text, String language) {
        String escapedText = text.replace("'", "''");

        switch (language) {
            case "English":
                return "PowerShell -Command \"Add-Type –AssemblyName System.Speech; " +
                        "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "$speak.SelectVoice('Microsoft Zira Desktop'); " +
                        "$speak.Speak('" + escapedText + "')\"";

            case "Français":
                // Tentative voix française, sinon fallback Zira
                return "PowerShell -Command \"Add-Type –AssemblyName System.Speech; " +
                        "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "try { $speak.SelectVoice('Microsoft Hortense Desktop') } catch { $speak.SelectVoice('Microsoft Zira Desktop') }; " +
                        "$speak.Speak('" + escapedText + "')\"";

            default:
                return "PowerShell -Command \"Add-Type –AssemblyName System.Speech; " +
                        "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "$speak.Speak('" + escapedText + "')\"";
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
    @FXML
    private void handleTranslateToArabic() {
        if (isTranslated) return;

        showMessage("🔄 Traduction en cours vers l'arabe...", "info");

        new Thread(() -> {
            // Traduire le titre
            String translatedTitle = TranslationService.translateToArabic(originalTitle);

            // Traduire le contenu
            String translatedContent = TranslationService.translateToArabic(originalContent);

            // ✅ Traduire les commentaires
            List<String> translatedComments = new ArrayList<>();
            for (commentaire c : originalCommentsList) {
                String translated = TranslationService.translateToArabic(c.getContenu());
                translatedComments.add(translated);
            }

            javafx.application.Platform.runLater(() -> {
                // Mettre à jour le post
                postTitleLabel.setText(translatedTitle);
                postContentLabel.setText(translatedContent);

                // ✅ Mettre à jour les commentaires avec les versions traduites
                commentItems.clear();
                for (int i = 0; i < originalCommentsList.size(); i++) {
                    commentaire original = originalCommentsList.get(i);
                    commentaire translatedComment = new commentaire();
                    translatedComment.setComment_id(original.getComment_id());
                    translatedComment.setPost_id(original.getPost_id());
                    translatedComment.setAuthor_id(original.getAuthor_id());
                    translatedComment.setContenu(original.getContenu());
                    translatedComment.setContenuTraduit(translatedComments.get(i));
                    commentItems.add(createCommentCard(translatedComment, true));
                }

                isTranslated = true;
                originalButton.setVisible(true);
                translateButton.setVisible(false);
                showMessage("✅ Traduit en arabe", "success");
            });
        }).start();
    }

    @FXML
    private void handleShowOriginal() {
        postTitleLabel.setText(originalTitle);
        postContentLabel.setText(originalContent);

        // ✅ Recharger les commentaires originaux
        commentItems.clear();
        for (commentaire c : originalCommentsList) {
            commentItems.add(createCommentCard(c, false));
        }

        isTranslated = false;
        originalButton.setVisible(false);
        translateButton.setVisible(true);
        showMessage("🔙 Version originale restaurée", "info");
    }

    public void setTranslatedVersion(String translatedTitle, String translatedContent) {
        if (postTitleLabel != null) {
            postTitleLabel.setText(translatedTitle);
            postContentLabel.setText(translatedContent);
            isTranslated = true;
            if (originalButton != null) originalButton.setVisible(true);
            if (translateButton != null) translateButton.setVisible(false);
        }
    }
}

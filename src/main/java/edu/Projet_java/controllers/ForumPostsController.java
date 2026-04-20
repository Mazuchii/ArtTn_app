package edu.Projet_java.controllers;

import edu.Projet_java.entities.Posts;
import edu.Projet_java.services.CommentServices;
import edu.Projet_java.services.PostServices;
import edu.Projet_java.services.CategoryServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class ForumPostsController {

    @FXML private ListView<HBox> postsListView;
    @FXML private TextField searchField;
    @FXML private Label messageLabel;
    @FXML private Label breadcrumbLabel;
    @FXML private Button sortAlphaBtn;
    @FXML private Button sortDateBtn;

    private PostServices postService;
    private CommentServices commentService;
    private CategoryServices categoryService;
    private ObservableList<HBox> postItems;
    private List<Posts> allPosts;
    private String currentSort = "date";
    private boolean ascending = false;

    // Catégorie courante
    private int currentCategoryId = 0;
    private String currentCategoryName = "Tous les posts";

    @FXML
    public void initialize() {
        postService = new PostServices();
        commentService = new CommentServices();
        categoryService = new CategoryServices();
        postItems = FXCollections.observableArrayList();
        postsListView.setItems(postItems);

        searchField.textProperty().addListener((obs, old, val) -> filterAndSortPosts());
    }

    // ✅ Appelé depuis ForumCategoriesController
    public void setCategory(int categoryId, String categoryName) {
        this.currentCategoryId = categoryId;
        this.currentCategoryName = categoryName;
        updateBreadcrumb();
        loadPostsByCategory();
    }

    private void updateBreadcrumb() {
        if (breadcrumbLabel != null) {
            if (currentCategoryId > 0) {
                breadcrumbLabel.setText("🏠 Accueil > 📁 " + currentCategoryName);
            } else {
                breadcrumbLabel.setText("🏠 Accueil > 📁 Tous les posts");
            }
        }
    }

    private void loadPostsByCategory() {
        try {
            if (currentCategoryId > 0) {
                allPosts = postService.getPostsByCategory(currentCategoryId);
                System.out.println("Posts chargés pour " + currentCategoryName + " : " + allPosts.size());
            } else {
                allPosts = postService.getPostData();
            }

            if (allPosts != null && !allPosts.isEmpty()) {
                filterAndSortPosts();
            } else {
                postItems.clear();
                showMessage("Aucun post dans cette catégorie. Soyez le premier à poster !", "info");
            }
        } catch (Exception e) {
            showMessage("Erreur: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    public void refreshPosts() {
        loadPostsByCategory();
    }

    private void filterAndSortPosts() {
        if (allPosts == null) return;

        String search = searchField.getText().toLowerCase();
        List<Posts> filteredPosts;

        if (search.isEmpty()) {
            filteredPosts = allPosts;
        } else {
            filteredPosts = allPosts.stream()
                    .filter(p -> p.getPost_titre().toLowerCase().contains(search) ||
                            p.getPost_contenu().toLowerCase().contains(search))
                    .toList();
        }

        List<Posts> sortedPosts = sortPosts(filteredPosts);
        displayPosts(sortedPosts);
    }

    private List<Posts> sortPosts(List<Posts> posts) {
        List<Posts> sorted = new java.util.ArrayList<>(posts);

        if (currentSort.equals("alpha")) {
            if (ascending) {
                sorted.sort(Comparator.comparing(Posts::getPost_titre, String.CASE_INSENSITIVE_ORDER));
            } else {
                sorted.sort((a, b) -> b.getPost_titre().compareToIgnoreCase(a.getPost_titre()));
            }
        } else {
            if (ascending) {
                sorted.sort((a, b) -> a.getDate_creation().compareTo(b.getDate_creation()));
            } else {
                sorted.sort((a, b) -> b.getDate_creation().compareTo(a.getDate_creation()));
            }
        }
        return sorted;
    }

    // ✅ MÉTHODE PUBLIQUE pour le tri alphabétique
    @FXML
    public void handleSortAlphabetical() {
        if (currentSort.equals("alpha")) {
            ascending = !ascending;
            String newText = ascending ? "🔤 Trier A-Z" : "🔤 Trier Z-A";
            sortAlphaBtn.setText(newText);
        } else {
            currentSort = "alpha";
            ascending = true;
            sortAlphaBtn.setText("🔤 Trier Z-A");
            sortDateBtn.setText("📅 Plus récents");
        }
        sortAlphaBtn.setStyle("-fx-background-color: linear-gradient(to right, #4f46e5, #7c3aed);");
        sortDateBtn.setStyle("-fx-background-color: linear-gradient(to right, #06b6d4, #0891b2);");
        filterAndSortPosts();
        showMessage("Tri alphabétique activé", "success");
    }

    // ✅ MÉTHODE PUBLIQUE pour le tri par date
    @FXML
    public void handleSortByDate() {
        if (currentSort.equals("date")) {
            ascending = !ascending;
            String newText = ascending ? "📅 Plus anciens" : "📅 Plus récents";
            sortDateBtn.setText(newText);
        } else {
            currentSort = "date";
            ascending = false;
            sortDateBtn.setText("📅 Plus anciens");
            sortAlphaBtn.setText("🔤 Trier A-Z");
        }
        sortDateBtn.setStyle("-fx-background-color: linear-gradient(to right, #0891b2, #0e7490);");
        sortAlphaBtn.setStyle("-fx-background-color: linear-gradient(to right, #6366f1, #8b5cf6);");
        filterAndSortPosts();
        showMessage("Tri par date activé", "success");
    }

    private void displayPosts(List<Posts> posts) {
        postItems.clear();
        for (Posts post : posts) {
            postItems.add(createPostCard(post));
        }
    }

    private HBox createPostCard(Posts post) {
        HBox card = new HBox();
        card.getStyleClass().add("post-card");
        card.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(8);
        content.setMaxWidth(Double.MAX_VALUE);

        HBox metaTop = new HBox(12);
        metaTop.getStyleClass().add("post-meta");

        Label authorLabel = new Label("👤 Auteur #" + post.getAuthor_id());
        authorLabel.getStyleClass().add("post-author");

        Label dateLabel = new Label("📅 " + post.getFormattedDate());
        dateLabel.getStyleClass().add("post-date");

        metaTop.getChildren().addAll(authorLabel, dateLabel);

        Label titleLabel = new Label(post.getPost_titre());
        titleLabel.getStyleClass().add("post-title");

        String excerpt = post.getPost_contenu();
        if (excerpt.length() > 150) {
            excerpt = excerpt.substring(0, 150) + "...";
        }
        Label excerptLabel = new Label(excerpt);
        excerptLabel.getStyleClass().add("post-excerpt");

        HBox statsBox = new HBox(15);
        statsBox.getStyleClass().add("post-stats");

        Label viewsLabel = new Label("👁️ " + post.getViews());
        viewsLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        Label likesLabel = new Label("👍 " + post.getLikes());
        likesLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        Label dislikesLabel = new Label("👎 " + post.getDislikes());
        dislikesLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        statsBox.getChildren().addAll(viewsLabel, likesLabel, dislikesLabel);

        HBox actionBar = new HBox(15);
        actionBar.getStyleClass().add("post-stats");

        int commentCount = commentService.countCommentsByPostId(post.getPost_id());
        String commentText = (commentCount == 0) ? "💬 Aucun commentaire" :
                (commentCount == 1) ? "💬 1 commentaire" :
                        "💬 " + commentCount + " commentaires";
        Label commentLabel = new Label(commentText);
        commentLabel.getStyleClass().add("post-stats");

        Button readButton = new Button("Lire la suite →");
        readButton.getStyleClass().add("btn-info");
        readButton.setOnAction(e -> openPostDetail(post));

        actionBar.getChildren().addAll(commentLabel, readButton);

        content.getChildren().addAll(metaTop, titleLabel, excerptLabel, statsBox, actionBar);
        card.getChildren().add(content);

        return card;
    }

    private void openPostDetail(Posts post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/forum_post_detail.fxml"));
            Parent root = loader.load();

            ForumPostDetailController controller = loader.getController();
            controller.setPost(post, this);

            Stage stage = new Stage();
            stage.setTitle("Post - Forum");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            showMessage("Erreur: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void handleAddPost() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/forum_add_post.fxml"));
            Parent root = loader.load();

            ForumAddPostController controller = loader.getController();
            controller.setParentController(this);
            controller.setCategoryId(currentCategoryId);

            Stage stage = new Stage();
            stage.setTitle("Nouveau post");
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            showMessage("Erreur: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void handleBackToCategories() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/forum_categories.fxml"));
            Stage stage = (Stage) postsListView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Forum - Museum Digital");
        } catch (IOException e) {
            showMessage("Erreur: " + e.getMessage(), "error");
        }
    }

    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        if (type.equals("error")) {
            messageLabel.setStyle("-fx-text-fill: #ef4444;");
        } else if (type.equals("success")) {
            messageLabel.setStyle("-fx-text-fill: #10b981;");
        } else {
            messageLabel.setStyle("-fx-text-fill: #3498db;");
        }
        messageLabel.setVisible(true);

        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> messageLabel.setVisible(false));
        }).start();
    }
}
package tn.esprit.museum.controllers;

import javafx.scene.layout.BorderPane;
import tn.esprit.museum.entities.Posts;
import tn.esprit.museum.entities.Product;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.services.*;
import tn.esprit.museum.utils.SessionManager;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ForumPostsController {

    @FXML private ListView<HBox> postsListView;
    @FXML private TextField searchField;
    @FXML private Label messageLabel;
    @FXML private Label breadcrumbLabel;
    @FXML private Button sortAlphaBtn;
    @FXML private Button sortDateBtn;
    @FXML private BorderPane mainContainer;
    @FXML private Label pageInfoLabel;


    private PostServices postService;
    private CommentServices commentService;
    private CategoryServices categoryService;
    private ObservableList<HBox> postItems;
    private List<Posts> allPosts;
    private String currentSort = "date";
    private boolean ascending = false;
    private List<Product> cart;
    private List<Integer> cartQuantities;
    private UserHomeController parentController;

    // Catégorie courante
    private int currentCategoryId = 0;
    private String currentCategoryName = "Tous les posts";
    private int currentPage = 1;
    private int totalPages = 1;
    private List<Posts> allFilteredPosts;
    private final int POSTS_PER_PAGE = 5;

    @FXML
    public void initialize() {
        postService = new PostServices();
        commentService = new CommentServices();
        categoryService = new CategoryServices();
        postItems = FXCollections.observableArrayList();
        postsListView.setItems(postItems);
        cart = new ArrayList<>();
        cartQuantities = new ArrayList<>();

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

    public void setParentController(UserHomeController parent) {
        this.parentController = parent;
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
    @FXML
    private void handlePreviousPage() {
        if (currentPage > 1) {
            currentPage--;
            updatePagination();
        }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
        }
    }

    private void updatePagination() {
        int start = (currentPage - 1) * POSTS_PER_PAGE;
        int end = Math.min(start + POSTS_PER_PAGE, allFilteredPosts.size());
        List<Posts> pagedPosts = allFilteredPosts.subList(start, end);
        displayPosts(pagedPosts);
        pageInfoLabel.setText("Page " + currentPage + " / " + totalPages);
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
        updateSortButtonsStyle(); // ✅ Ajouter cette ligne
        filterAndSortPosts();
        showMessage("Tri alphabétique activé", "success");
    }
    @FXML
    private void handleOpenCart() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cart_view.fxml"));
            Parent root = loader.load();

            CartController cartController = loader.getController();
            cartController.setCart(cart, cartQuantities);

            Stage stage = new Stage();
            stage.setTitle("Mon Panier");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Erreur: Impossible d'ouvrir le panier", "error");
        }
    }

    @FXML
    private void handleOpenOrders() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/orders_history.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Mes Commandes");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Erreur: Impossible d'ouvrir l'historique", "error");
        }
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
        updateSortButtonsStyle(); // ✅ Ajouter cette ligne
        filterAndSortPosts();
        showMessage("Tri par date activé", "success");
    }

    private void displayPosts(List<Posts> posts) {
        postItems.clear();
        for (Posts post : posts) {
            postItems.add(createPostCard(post));
        }
    }

    private String getUserNameById(int userId) {
        try {
            UserService userService = new UserService();
            User user = userService.getById(userId);
            if (user != null) {
                return user.getFullName();
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération nom: " + e.getMessage());
        }
        return "Utilisateur #" + userId;
    }

    private HBox createPostCard(Posts post) {
        HBox card = new HBox();
        card.getStyleClass().add("post-card");
        card.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(8);
        content.setMaxWidth(Double.MAX_VALUE);

        HBox metaTop = new HBox(12);
        metaTop.getStyleClass().add("post-meta");

        // Récupérer le nom d'utilisateur depuis la base
        String authorName = getUserNameById(post.getAuthor_id());
        Label authorLabel = new Label("👤 " + authorName);
        authorLabel.getStyleClass().add("post-author");

        Label dateLabel = new Label("📅 " + post.getFormattedDate());
        dateLabel.getStyleClass().add("post-date");

        metaTop.getChildren().addAll(authorLabel, dateLabel);

        Label titleLabel = new Label(post.getPost_titre());
        titleLabel.getStyleClass().add("post-title");
        titleLabel.setOnMouseClicked(e -> openPostDetail(post));

        String excerpt = post.getPost_contenu();
        if (excerpt.length() > 150) {
            excerpt = excerpt.substring(0, 150) + "...";
        }
        Label excerptLabel = new Label(excerpt);
        excerptLabel.getStyleClass().add("post-excerpt");

        HBox statsBox = new HBox(15);
        statsBox.getStyleClass().add("post-stats");

        Label viewsLabel = new Label("👁️ " + post.getViews());
        Label likesLabel = new Label("👍 " + post.getLikes());
        Label dislikesLabel = new Label("👎 " + post.getDislikes());

        statsBox.getChildren().addAll(viewsLabel, likesLabel, dislikesLabel);

        HBox actionBar = new HBox(15);
        actionBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        int commentCount = commentService.countCommentsByPostId(post.getPost_id());
        String commentText = (commentCount == 0) ? "💬 Aucun commentaire" :
                (commentCount == 1) ? "💬 1 commentaire" :
                        "💬 " + commentCount + " commentaires";
        Label commentLabel = new Label(commentText);
        commentLabel.getStyleClass().add("post-stats");

        Button readButton = new Button("Lire la suite →");
        readButton.getStyleClass().add("btn-info");
        readButton.setOnAction(e -> openPostDetail(post));

        Button reportButton = new Button("🚩 Signaler");
        reportButton.getStyleClass().add("comment-action-btn");
        reportButton.setStyle("-fx-text-fill: #ef4444;");
        reportButton.setOnAction(e -> openReportDialog(post));

        Button translateBtn = new Button("🌐 ترجمة");
        translateBtn.getStyleClass().add("comment-action-btn");
        translateBtn.setStyle("-fx-text-fill: #10b981;");
        translateBtn.setOnAction(e -> translatePostAndComments(post));

        actionBar.getChildren().addAll(commentLabel, readButton, reportButton, translateBtn);

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

    private void openReportDialog(Posts post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/report_dialog.fxml"));
            Parent root = loader.load();

            tn.esprit.museum.controllers.ReportDialogController controller = loader.getController();
            controller.setPostId(post.getPost_id());
            controller.setUserId(SessionManager.getCurrentUserId());
            controller.setOnReportSuccess(() -> {
                // ✅ CORRECTION: utiliser refreshPosts() au lieu de loadPosts()
                refreshPosts();
            });

            Stage stage = new Stage();
            stage.setTitle("Signaler un post");
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (IOException ex) {
            showMessage("Erreur: " + ex.getMessage(), "error");
            ex.printStackTrace();
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/forum_categories.fxml"));
            Parent categoriesView = loader.load();

            // Récupérer le contrôleur et lui passer le parent
            ForumCategoriesController categoriesCtrl = loader.getController();
            if (parentController != null) {
                categoriesCtrl.setParentController(parentController);
            }

            // Récupérer forumView via lookup
            VBox forumView = null;
            if (postsListView.getScene() != null) {
                forumView = (VBox) postsListView.getScene().lookup("#forumView");
            }
            if (forumView == null) {
                forumView = UserHomeController.getForumView();
            }

            if (parentController != null) {
                parentController.showForumCategories();
                System.out.println("✅ Retour aux catégories via parentController");
            } else if (forumView != null) {
                forumView.getChildren().clear();
                forumView.getChildren().add(categoriesView);
                VBox.setVgrow(categoriesView, javafx.scene.layout.Priority.ALWAYS);
                System.out.println("✅ Retour aux catégories réussi (direct)");
            } else {
                System.err.println("❌ forumView introuvable lors du retour");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("message-success", "message-error", "message-info");
        
        if (type.equals("error")) {
            messageLabel.getStyleClass().add("message-error");
        } else if (type.equals("success")) {
            messageLabel.getStyleClass().add("message-success");
        } else {
            messageLabel.getStyleClass().add("message-info");
        }
        messageLabel.setVisible(true);

        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> messageLabel.setVisible(false));
        }).start();
    }
    private void translatePostAndComments(Posts post) {
        // Afficher un indicateur de chargement
        showMessage("🔄 Traduction en cours vers l'arabe...", "info");

        new Thread(() -> {
            String translatedTitle = TranslationService.translateToArabic(post.getPost_titre());
            String translatedContent = TranslationService.translateToArabic(post.getPost_contenu());

            javafx.application.Platform.runLater(() -> {
                // Ouvrir le post en mode traduction
                openTranslatedPostDetail(post, translatedTitle, translatedContent);
            });
        }).start();
    }
    private void openTranslatedPostDetail(Posts post, String translatedTitle, String translatedContent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/forum_post_detail.fxml"));
            Parent root = loader.load();

            ForumPostDetailController controller = loader.getController();
            controller.setPost(post, this);
            controller.setTranslatedVersion(translatedTitle, translatedContent);

            Stage stage = new Stage();
            stage.setTitle("Post - Forum (Traduit en Arabe)");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            showMessage("Erreur: " + e.getMessage(), "error");
        }
    }
    private void updateSortButtonsStyle() {
        if (currentSort.equals("alpha")) {
            sortAlphaBtn.getStyleClass().add("active");
            sortDateBtn.getStyleClass().remove("active");
        } else {
            sortDateBtn.getStyleClass().add("active");
            sortAlphaBtn.getStyleClass().remove("active");
        }
    }

}

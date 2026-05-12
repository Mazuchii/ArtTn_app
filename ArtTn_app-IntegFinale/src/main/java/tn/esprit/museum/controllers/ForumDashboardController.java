package tn.esprit.museum.controllers;

import javafx.scene.Node;
import tn.esprit.museum.entities.Category;
import tn.esprit.museum.entities.Posts;
import tn.esprit.museum.entities.Report;
import tn.esprit.museum.entities.commentaire;
import tn.esprit.museum.services.CategoryServices;
import tn.esprit.museum.services.PostServices;
import tn.esprit.museum.services.CommentServices;
import tn.esprit.museum.services.ReportServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Optional;

public class ForumDashboardController {

    // ==================== STATISTIQUES ====================
    @FXML private Label statsPosts;
    @FXML private Label statsComments;
    @FXML private Label statsCategories;
    @FXML private Label statsReports;
    @FXML private Label adminLabel;
    @FXML private Label messageLabel;

    // ==================== TABLE POSTS ====================
    @FXML private TableView<Posts> postsTable;
    @FXML private TableColumn<Posts, Integer> postIdColumn;
    @FXML private TableColumn<Posts, String> postTitleColumn;
    @FXML private TableColumn<Posts, String> postContentColumn;
    @FXML private TableColumn<Posts, String> postDateColumn;
    @FXML private TableColumn<Posts, Integer> postCommentCountColumn;
    @FXML private TableColumn<Posts, Integer> postReportCountColumn;
    @FXML private TableColumn<Posts, Void> postActionsColumn;
    @FXML private TextField postSearchField;

    // ==================== TABLE COMMENTAIRES ====================
    @FXML private TableView<commentaire> commentsTable;
    @FXML private TableColumn<commentaire, Integer> commentIdColumn;
    @FXML private TableColumn<commentaire, Integer> commentPostIdColumn;
    @FXML private TableColumn<commentaire, String> commentContentColumn;
    @FXML private TableColumn<commentaire, Void> commentActionsColumn;
    @FXML private TextField commentSearchField;

    // ==================== TABLE CATÉGORIES ====================
    @FXML private TableView<Category> categoriesTable;
    @FXML private TableColumn<Category, Integer> categoryIdColumn;
    @FXML private TableColumn<Category, String> categoryIconColumn;
    @FXML private TableColumn<Category, String> categoryNameColumn;
    @FXML private TableColumn<Category, String> categoryDescColumn;
    @FXML private TableColumn<Category, Void> categoryActionsColumn;
    @FXML private TextField categorySearchField;

    // ==================== TABLE SIGNALEMENTS ====================
    @FXML private TableView<Report> reportsTable;
    @FXML private TableColumn<Report, Integer> reportIdColumn;
    @FXML private TableColumn<Report, Integer> reportPostIdColumn;
    @FXML private TableColumn<Report, String> reportPostTitleColumn;
    @FXML private TableColumn<Report, String> reportReasonColumn;
    @FXML private TableColumn<Report, String> reportDetailsColumn;
    @FXML private TableColumn<Report, String> reportStatusColumn;
    @FXML private TableColumn<Report, String> reportDateColumn;
    @FXML private TableColumn<Report, Void> reportActionsColumn;
    @FXML private TextField reportSearchField;
    @FXML private TabPane mainTabPane; // Make sure to add fx:id="mainTabPane" in FXML

    private CategoryServices categoryService;
    private ObservableList<Category> categoryList;

    private PostServices postService;
    private CommentServices commentService;
    private ReportServices reportService;
    private ObservableList<Posts> postList;
    private ObservableList<commentaire> commentList;
    private ObservableList<Report> reportList;

    // Styles des boutons — remplacés par les classes CSS forum_style.css
    private static final String EDIT_STYLE   = null; // utilise styleClass btn-table-edit
    private static final String DELETE_STYLE = null; // utilise styleClass btn-table-delete

    /** Applique le style "Modifier" à un bouton via styleClass CSS. */
    private static void styleEdit(Button btn) {
        btn.getStyleClass().addAll("btn-table-edit");
    }

    /** Applique le style "Supprimer" à un bouton via styleClass CSS. */
    private static void styleDelete(Button btn) {
        btn.getStyleClass().addAll("btn-table-delete");
    }

    /** Applique le style "Résoudre" à un bouton via styleClass CSS. */
    private static void styleResolve(Button btn) {
        btn.getStyleClass().addAll("btn-table-resolve");
    }

    @FXML
    public void initialize() {
        System.out.println("=== INITIALISATION FORUM DASHBOARD ===");

        postService = new PostServices();
        commentService = new CommentServices();
        categoryService = new CategoryServices();
        reportService = new ReportServices();

        postList = FXCollections.observableArrayList();
        commentList = FXCollections.observableArrayList();
        categoryList = FXCollections.observableArrayList();
        reportList = FXCollections.observableArrayList();

        // ==================== CONFIGURATION POSTS ====================
        postIdColumn.setCellValueFactory(new PropertyValueFactory<>("post_id"));
        postTitleColumn.setCellValueFactory(new PropertyValueFactory<>("post_titre"));
        postContentColumn.setCellValueFactory(new PropertyValueFactory<>("post_contenu"));
        postDateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));

        postCommentCountColumn.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createIntegerBinding(
                        () -> commentService.countCommentsByPostId(cellData.getValue().getPost_id())
                ).asObject()
        );

        postReportCountColumn.setCellValueFactory(new PropertyValueFactory<>("reportCount"));

        setupPostActionsColumn();

        // ==================== CONFIGURATION COMMENTAIRES ====================
        commentIdColumn.setCellValueFactory(new PropertyValueFactory<>("comment_id"));
        commentPostIdColumn.setCellValueFactory(new PropertyValueFactory<>("post_id"));
        commentContentColumn.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        setupCommentActionsColumn();

        // ==================== CONFIGURATION CATÉGORIES ====================
        categoryIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        categoryIconColumn.setCellValueFactory(new PropertyValueFactory<>("icon"));
        categoryNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryDescColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        setupCategoryActionsColumn();

        // ==================== CONFIGURATION SIGNALEMENTS ====================
        reportIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        reportPostIdColumn.setCellValueFactory(new PropertyValueFactory<>("postId"));
        reportReasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));
        reportDetailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));
        reportStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        reportDateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // Titre du post
        reportPostTitleColumn.setCellValueFactory(cellData -> {
            Report report = cellData.getValue();
            Posts post = postService.getPostById(report.getPostId());
            return new javafx.beans.property.SimpleStringProperty(post != null ? post.getPost_titre() : "Post supprimé");
        });

        setupReportActionsColumn();

        // ==================== FILTRES ====================
        postSearchField.textProperty().addListener((obs, old, val) -> filterPosts());
        commentSearchField.textProperty().addListener((obs, old, val) -> filterComments());
        categorySearchField.textProperty().addListener((obs, old, val) -> filterCategories());
        reportSearchField.textProperty().addListener((obs, old, val) -> filterReports());

        // ==================== CHARGEMENT DES DONNÉES ====================
        loadPosts();
        loadComments();
        loadCategories();
        loadReports();

        // Mettre à jour les stats après chargement
        updateStats();

        // Appliquer le surlignage pour les posts avec +3 signalements
        setupPostHighlighting();

        Platform.runLater(() -> {
            Node header = postsTable.getScene().lookup(".tab-header-area");
            if (header != null) {
                header.setStyle("-fx-background-color: transparent;");
            }
            Node headerBg = postsTable.getScene().lookup(".tab-header-background");
            if (headerBg != null) {
                headerBg.setStyle("-fx-background-color: transparent;");
            }
        });

        System.out.println("=== INITIALISATION TERMINÉE ===");
    }

    // ==================== POSTS ====================
    private void setupPostHighlighting() {
        postsTable.setRowFactory(tv -> new TableRow<Posts>() {
            @Override
            protected void updateItem(Posts item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (item.getReportCount() >= 3) {
                    setStyle("-fx-background-color: #fee2e2; -fx-border-color: #ef4444;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void setupPostActionsColumn() {
        postActionsColumn.setCellFactory(param -> new TableCell<Posts, Void>() {
            private final Button editBtn   = new Button("✏️ Modifier");
            private final Button deleteBtn = new Button("🗑️ Supprimer");
            private final HBox buttons     = new HBox(8, editBtn, deleteBtn);

            {
                styleEdit(editBtn);
                styleDelete(deleteBtn);
                buttons.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                editBtn.setOnAction(event -> {
                    Posts post = getTableView().getItems().get(getIndex());
                    openEditPostDialog(post);
                });

                deleteBtn.setOnAction(event -> {
                    Posts post = getTableView().getItems().get(getIndex());
                    deletePost(post);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    private void loadPosts() {
        try {
            List<Posts> posts = postService.getPostData();
            postList.setAll(posts);
            postsTable.setItems(postList);
            System.out.println("Posts chargés: " + posts.size());
        } catch (Exception e) {
            showMessage("Erreur chargement posts: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    private void filterPosts() {
        String search = postSearchField.getText().toLowerCase();
        if (search.isEmpty()) {
            postsTable.setItems(postList);
        } else {
            ObservableList<Posts> filtered = FXCollections.observableArrayList();
            for (Posts p : postList) {
                if (p.getPost_titre().toLowerCase().contains(search) ||
                        p.getPost_contenu().toLowerCase().contains(search)) {
                    filtered.add(p);
                }
            }
            postsTable.setItems(filtered);
        }
        updateStats();
    }

    private void openEditPostDialog(Posts post) {
        TextInputDialog titleDialog = new TextInputDialog(post.getPost_titre());
        titleDialog.setTitle("Modifier le titre");
        titleDialog.setHeaderText("Modifier le titre du post");
        titleDialog.setContentText("Nouveau titre:");

        Optional<String> titleResult = titleDialog.showAndWait();
        if (titleResult.isPresent() && !titleResult.get().trim().isEmpty()) {
            TextInputDialog contentDialog = new TextInputDialog(post.getPost_contenu());
            contentDialog.setTitle("Modifier le contenu");
            contentDialog.setHeaderText("Modifier le contenu du post");
            contentDialog.setContentText("Nouveau contenu:");

            Optional<String> contentResult = contentDialog.showAndWait();
            if (contentResult.isPresent() && !contentResult.get().trim().isEmpty()) {
                try {
                    post.setPost_titre(titleResult.get().trim());
                    post.setPost_contenu(contentResult.get().trim());
                    postService.updatepost(post.getPost_id(), post);
                    showMessage("✅ Post modifié avec succès !", "success");
                    loadPosts();
                    updateStats();
                } catch (Exception e) {
                    showMessage("Erreur: " + e.getMessage(), "error");
                }
            }
        }
    }

    private void deletePost(Posts post) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer le post \"" + post.getPost_titre() + "\" ?\nTous les commentaires seront également supprimés.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                postService.deletepost(post.getPost_id());
                showMessage("✅ Post supprimé avec succès !", "success");
                loadPosts();
                loadComments();
                loadReports();
                updateStats();
            } catch (Exception e) {
                showMessage("Erreur: " + e.getMessage(), "error");
            }
        }
    }

    // ==================== COMMENTAIRES ====================
    private void setupCommentActionsColumn() {
        commentActionsColumn.setCellFactory(param -> new TableCell<commentaire, Void>() {
            private final Button editBtn   = new Button("✏️ Modifier");
            private final Button deleteBtn = new Button("🗑️ Supprimer");
            private final HBox buttons     = new HBox(8, editBtn, deleteBtn);

            {
                styleEdit(editBtn);
                styleDelete(deleteBtn);
                buttons.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                editBtn.setOnAction(event -> {
                    commentaire comment = getTableView().getItems().get(getIndex());
                    openEditCommentDialog(comment);
                });

                deleteBtn.setOnAction(event -> {
                    commentaire comment = getTableView().getItems().get(getIndex());
                    deleteComment(comment);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    private void loadComments() {
        try {
            List<commentaire> comments = commentService.getCommentData();
            commentList.setAll(comments);
            commentsTable.setItems(commentList);
            System.out.println("Commentaires chargés: " + comments.size());
        } catch (Exception e) {
            showMessage("Erreur chargement commentaires: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    private void filterComments() {
        String search = commentSearchField.getText().toLowerCase();
        if (search.isEmpty()) {
            commentsTable.setItems(commentList);
        } else {
            ObservableList<commentaire> filtered = FXCollections.observableArrayList();
            for (commentaire c : commentList) {
                if (c.getContenu().toLowerCase().contains(search)) {
                    filtered.add(c);
                }
            }
            commentsTable.setItems(filtered);
        }
        updateStats();
    }

    private void openEditCommentDialog(commentaire comment) {
        TextInputDialog dialog = new TextInputDialog(comment.getContenu());
        dialog.setTitle("Modifier le commentaire");
        dialog.setHeaderText("Modifier le commentaire");
        dialog.setContentText("Nouveau texte:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try {
                comment.setContenu(result.get().trim());
                commentService.updateComment(comment.getComment_id(), comment);
                showMessage("✅ Commentaire modifié avec succès !", "success");
                loadComments();
                loadPosts();
                updateStats();
            } catch (Exception e) {
                showMessage("Erreur: " + e.getMessage(), "error");
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
                showMessage("✅ Commentaire supprimé avec succès !", "success");
                loadComments();
                loadPosts();
                updateStats();
            } catch (Exception e) {
                showMessage("Erreur: " + e.getMessage(), "error");
            }
        }
    }

    // ==================== CATÉGORIES ====================
    private void setupCategoryActionsColumn() {
        categoryActionsColumn.setCellFactory(param -> new TableCell<Category, Void>() {
            private final Button editBtn   = new Button("✏️ Modifier");
            private final Button deleteBtn = new Button("🗑️ Supprimer");
            private final HBox buttons     = new HBox(8, editBtn, deleteBtn);

            {
                styleEdit(editBtn);
                styleDelete(deleteBtn);
                buttons.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                editBtn.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    openEditCategoryDialog(category);
                });

                deleteBtn.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    deleteCategory(category);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryService.getAllCategories();
            categoryList.setAll(categories);
            categoriesTable.setItems(categoryList);
            System.out.println("Catégories chargées: " + categories.size());
        } catch (Exception e) {
            showMessage("Erreur chargement catégories: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    private void filterCategories() {
        String search = categorySearchField.getText().toLowerCase();
        if (search.isEmpty()) {
            categoriesTable.setItems(categoryList);
        } else {
            ObservableList<Category> filtered = FXCollections.observableArrayList();
            for (Category c : categoryList) {
                if (c.getName().toLowerCase().contains(search) ||
                        c.getDescription().toLowerCase().contains(search)) {
                    filtered.add(c);
                }
            }
            categoriesTable.setItems(filtered);
        }
        updateStats();
    }

    @FXML
    private void handleAddCategory() {
        showCategoryDialog(null);
    }

    private void openEditCategoryDialog(Category category) {
        showCategoryDialog(category);
    }

    private void showCategoryDialog(Category category) {
        Dialog<Category> dialog = new Dialog<>();
        dialog.setTitle(category == null ? "Ajouter une catégorie" : "Modifier la catégorie");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Nom de la catégorie");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Description");
        descArea.setPrefRowCount(3);

        TextField iconField = new TextField();
        iconField.setPromptText("Icône (emoji) ex: 🖼️, 🏺, 🎨");

        if (category != null) {
            nameField.setText(category.getName());
            descArea.setText(category.getDescription());
            iconField.setText(category.getIcon());
        }

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descArea, 1, 1);
        grid.add(new Label("Icône:"), 0, 2);
        grid.add(iconField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                if (nameField.getText().trim().isEmpty()) {
                    showMessage("Le nom est obligatoire", "error");
                    return null;
                }
                if (category == null) {
                    Category c = new Category();
                    c.setName(nameField.getText().trim());
                    c.setDescription(descArea.getText().trim());
                    c.setIcon(iconField.getText().trim().isEmpty() ? "🏷️" : iconField.getText().trim());
                    return c;
                } else {
                    category.setName(nameField.getText().trim());
                    category.setDescription(descArea.getText().trim());
                    category.setIcon(iconField.getText().trim().isEmpty() ? "🏷️" : iconField.getText().trim());
                    return category;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                if (category == null) {
                    categoryService.addCategory(result);
                    showMessage("✅ Catégorie ajoutée !", "success");
                } else {
                    categoryService.updateCategory(category.getId(), result);
                    showMessage("✅ Catégorie modifiée !", "success");
                }
                loadCategories();
                updateStats();
            } catch (Exception e) {
                showMessage("Erreur: " + e.getMessage(), "error");
            }
        });
    }

    private void deleteCategory(Category category) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer la catégorie \"" + category.getName() + "\" ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                categoryService.deleteCategory(category.getId());
                showMessage("✅ Catégorie supprimée !", "success");
                loadCategories();
                updateStats();
            } catch (Exception e) {
                showMessage("Erreur: " + e.getMessage(), "error");
            }
        }
    }

    @FXML
    private void handleRefreshCategories() {
        loadCategories();
        showMessage("Liste des catégories actualisée", "success");
    }

    @FXML
    private void handleRefreshPosts() {
        loadPosts();
        showMessage("Liste des posts actualisée", "success");
    }

    @FXML
    private void handleRefreshComments() {
        loadComments();
        showMessage("Liste des commentaires actualisée", "success");
    }

    // ==================== SIGNALEMENTS ====================
    private void setupReportActionsColumn() {
        reportActionsColumn.setCellFactory(param -> new TableCell<Report, Void>() {
            private final Button viewBtn    = new Button("👁️ Voir");
            private final Button resolveBtn = new Button("✅ Résoudre");
            private final Button deleteBtn  = new Button("🗑️ Supprimer");
            private final HBox buttons      = new HBox(6, viewBtn, resolveBtn, deleteBtn);

            {
                styleEdit(viewBtn);
                styleResolve(resolveBtn);
                styleDelete(deleteBtn);
                buttons.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                viewBtn.setOnAction(event -> {
                    Report report = getTableView().getItems().get(getIndex());
                    viewReportedPost(report.getPostId());
                });

                resolveBtn.setOnAction(event -> {
                    Report report = getTableView().getItems().get(getIndex());
                    resolveReport(report);
                });

                deleteBtn.setOnAction(event -> {
                    Report report = getTableView().getItems().get(getIndex());
                    deleteReportedPost(report);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    private void loadReports() {
        try {
            List<Report> reports = reportService.getAllReports();
            reportList.setAll(reports);
            reportsTable.setItems(reportList);
            System.out.println("Signalements chargés: " + reports.size());
        } catch (Exception e) {
            showMessage("Erreur chargement signalements: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    private void filterReports() {
        String search = reportSearchField.getText().toLowerCase();
        if (search.isEmpty()) {
            reportsTable.setItems(reportList);
        } else {
            ObservableList<Report> filtered = FXCollections.observableArrayList();
            for (Report r : reportList) {
                if (String.valueOf(r.getPostId()).contains(search) ||
                        r.getReason().toLowerCase().contains(search) ||
                        r.getStatus().toLowerCase().contains(search)) {
                    filtered.add(r);
                }
            }
            reportsTable.setItems(filtered);
        }
        updateStats();
    }

    @FXML
    private void handleRefreshReports() {
        loadReports();
        updateStats();
        showMessage("Liste des signalements actualisée", "success");
    }

    private void viewReportedPost(int postId) {
        Posts post = postService.getPostById(postId);
        if (post == null) {
            showMessage("Ce post n'existe plus", "error");
            return;
        }

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Post signalé");
        info.setHeaderText("Post #" + postId + " - " + post.getPost_titre());
        info.setContentText("Contenu: " + post.getPost_contenu() + "\n\nDate: " + post.getFormattedDate());
        info.showAndWait();
    }

    private void resolveReport(Report report) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Résoudre le signalement");
        confirm.setHeaderText(null);
        confirm.setContentText("Ce signalement a-t-il été traité ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            reportService.updateReportStatus(report.getId(), "RESOLVED");
            loadReports();
            updateStats();
            showMessage("✅ Signalement marqué comme résolu", "success");
        }
    }

    private void deleteReportedPost(Report report) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le post");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer le post signalé et tous ses commentaires ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                postService.deletepost(report.getPostId());
                reportService.deleteReport(report.getId());
                loadReports();
                loadPosts();
                loadComments();
                updateStats();
                showMessage("✅ Post supprimé avec succès", "success");
            } catch (Exception e) {
                showMessage("Erreur: " + e.getMessage(), "error");
            }
        }
    }

    // ==================== STATISTIQUES ====================
    private void updateStats() {
        Platform.runLater(() -> {
            try {
                // Vérifier que les labels existent
                if (statsPosts == null || statsComments == null || statsCategories == null || statsReports == null) {
                    System.err.println("Erreur: Un ou plusieurs labels sont null!");
                    return;
                }

                // Récupérer les nombres depuis les listes
                int postCount = postsTable.getItems().size();
                int commentCount = commentsTable.getItems().size();
                int categoryCount = categoriesTable.getItems().size();
                int reportCount = reportsTable.getItems().size();

                // Mettre à jour les labels
                statsPosts.setText(String.valueOf(postCount));
                statsComments.setText(String.valueOf(commentCount));
                statsCategories.setText(String.valueOf(categoryCount));
                statsReports.setText(String.valueOf(reportCount));

                System.out.println("=== STATISTIQUES MISE À JOUR ===");
                System.out.println("Posts: " + postCount);
                System.out.println("Commentaires: " + commentCount);
                System.out.println("Catégories: " + categoryCount);
                System.out.println("Signalements: " + reportCount);

            } catch (Exception e) {
                System.err.println("Erreur updateStats: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // ==================== MESSAGES ====================
    private void showMessage(String message, String type) {
        Platform.runLater(() -> {
            messageLabel.setText(message);
            if (type.equals("error")) {
                messageLabel.setStyle("-fx-text-fill: #ef4444;");
            } else if (type.equals("success")) {
                messageLabel.setStyle("-fx-text-fill: #10b981;");
            } else {
                messageLabel.setStyle("-fx-text-fill: #d4af37;");
            }
            messageLabel.setVisible(true);

            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException e) {}
                Platform.runLater(() -> messageLabel.setVisible(false));
            }).start();
        });
    }
}

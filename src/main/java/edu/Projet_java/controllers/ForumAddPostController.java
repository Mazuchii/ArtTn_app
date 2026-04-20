package edu.Projet_java.controllers;

import edu.Projet_java.entities.Posts;
import edu.Projet_java.services.PostServices;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ForumAddPostController {

    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private Label errorLabel;
    @FXML private Button deleteImageBtn;
    @FXML private ImageView imagePreview;
    @FXML private VBox imagePreviewContainer;

    private PostServices postService;
    private ForumPostsController parentController;
    private int categoryId = 0;
    private String savedImagePath = null;
    private File selectedImageFile = null;

    // Dossier où les images seront stockées
    private static final String IMAGES_DIR = "src/main/resources/images/posts/";

    @FXML
    public void initialize() {
        postService = new PostServices();

        // Créer le dossier si inexistant
        File dir = new File(IMAGES_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void setParentController(ForumPostsController parent) {
        this.parentController = parent;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    // ✅ Choisir une image depuis le PC
    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        Stage stage = (Stage) titleField.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            selectedImageFile = selectedFile;

            // Afficher l'aperçu
            try {
                Image image = new Image(selectedFile.toURI().toString());
                imagePreview.setImage(image);
                imagePreviewContainer.setVisible(true);
                imagePreviewContainer.setManaged(true);
                deleteImageBtn.setDisable(false);
                showMessage("✅ Image chargée : " + selectedFile.getName(), "success");
            } catch (Exception e) {
                showMessage("❌ Impossible de charger l'image", "error");
            }
        }
    }

    // ✅ Supprimer l'image sélectionnée
    @FXML
    private void handleDeleteImage() {
        selectedImageFile = null;
        savedImagePath = null;
        imagePreview.setImage(null);
        imagePreviewContainer.setVisible(false);
        imagePreviewContainer.setManaged(false);
        deleteImageBtn.setDisable(true);
        showMessage("🗑️ Image supprimée", "success");
    }

    // ✅ Sauvegarder l'image dans le dossier du projet
    private String saveImage() {
        if (selectedImageFile == null) {
            return null;
        }

        try {
            String fileName = System.currentTimeMillis() + "_" + selectedImageFile.getName();
            Path destination = Path.of(IMAGES_DIR, fileName);
            Files.copy(selectedImageFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
            return "/images/posts/" + fileName;
        } catch (Exception e) {
            System.err.println("Erreur sauvegarde image: " + e.getMessage());
            return null;
        }
    }

    @FXML
    private void handleSave() {
        String title = titleField.getText().trim();
        String content = contentArea.getText().trim();

        // ========== VALIDATION DU TITRE ==========
        if (title.isEmpty()) {
            showMessage("❌ Le titre est obligatoire", "error");
            return;
        }
        if (title.length() > 100) {
            showMessage("❌ Le titre ne doit pas dépasser 100 caractères", "error");
            return;
        }
        if (title.length() < 3) {
            showMessage("❌ Le titre doit contenir au moins 3 caractères", "error");
            return;
        }

        // ========== VÉRIFICATION DOUBLON DE TITRE ==========
        try {
            if (postService.isTitleExists(title, 1)) {
                showMessage("❌ Vous avez déjà un post avec ce titre. Veuillez choisir un titre différent.", "error");
                return;
            }
        } catch (Exception e) {
            System.err.println("Erreur vérification titre: " + e.getMessage());
        }

        // ========== VALIDATION DU CONTENU ==========
        if (content.isEmpty()) {
            showMessage("❌ Le contenu est obligatoire", "error");
            return;
        }
        if (content.length() < 10) {
            showMessage("❌ Le contenu doit contenir au moins 10 caractères", "error");
            return;
        }

        // ========== VÉRIFICATION DOUBLON DE CONTENU ==========
        try {
            if (postService.isContentExists(content, 1)) {
                showMessage("❌ Vous avez déjà un post avec ce contenu similaire. Veuillez modifier votre message.", "error");
                return;
            }
        } catch (Exception e) {
            System.err.println("Erreur vérification contenu: " + e.getMessage());
        }

        // ========== SAUVEGARDE ==========
        try {
            String imagePath = saveImage();

            Posts post = new Posts(title, content, 1);
            post.setCategoryId(categoryId);
            post.setImageUrl(imagePath);

            postService.addpost(post);
            showMessage("✅ Post ajouté avec succès !", "success");

            if (parentController != null) {
                parentController.refreshPosts();
            }

            // Fermer la fenêtre après 1.5 secondes
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException e) {}
                javafx.application.Platform.runLater(this::closeWindow);
            }).start();

        } catch (Exception e) {
            showMessage("❌ Erreur: " + e.getMessage(), "error");
            e.printStackTrace();
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

    private void showMessage(String message, String type) {
        errorLabel.setText(message);
        if (type.equals("error")) {
            errorLabel.setStyle("-fx-text-fill: #ef4444;");
        } else {
            errorLabel.setStyle("-fx-text-fill: #10b981;");
        }
        errorLabel.setVisible(true);

        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> errorLabel.setVisible(false));
        }).start();
    }
}
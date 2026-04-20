package tn.esprit.produit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import tn.esprit.produit.entities.Product;
import tn.esprit.produit.services.ProductService;

import java.sql.SQLException;
import java.util.Optional;

public class ProductsManagementController {

    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, Integer> idColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> descriptionColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, Integer> stockColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, Boolean> availableColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilterCombo;
    @FXML private Label statsTotalProducts;
    @FXML private Label statsAvailableProducts;
    @FXML private Label statsLowStock;

    private ProductService productService;
    private ObservableList<Product> productList;

    @FXML
    public void initialize() {
        productService = new ProductService();
        productList = FXCollections.observableArrayList();

        // Configurer les colonnes
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        availableColumn.setCellValueFactory(new PropertyValueFactory<>("available"));

        // Formater le prix
        priceColumn.setCellFactory(column -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f €", item));
            }
        });

        // Formater le statut
        availableColumn.setCellFactory(column -> new TableCell<Product, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "✅ Disponible" : "❌ Indisponible");
                    setStyle(item ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
                }
            }
        });

        // Filtres
        categoryFilterCombo.getItems().addAll("Tous", "Coffrets", "Livres", "Accessoires", "Décorations", "Numérique");
        categoryFilterCombo.setValue("Tous");

        searchField.textProperty().addListener((obs, old, val) -> filterProducts());
        categoryFilterCombo.valueProperty().addListener((obs, old, val) -> filterProducts());

        loadProducts();
        updateStats();
    }

    private void loadProducts() {
        try {
            productList.setAll(productService.getAll());
            productsTable.setItems(productList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les produits: " + e.getMessage());
        }
    }

    private void filterProducts() {
        String searchText = searchField.getText().toLowerCase();
        String selectedCategory = categoryFilterCombo.getValue();

        ObservableList<Product> filtered = FXCollections.observableArrayList();
        for (Product p : productList) {
            boolean matchSearch = searchText.isEmpty() ||
                    p.getName().toLowerCase().contains(searchText) ||
                    p.getDescription().toLowerCase().contains(searchText);
            boolean matchCategory = selectedCategory.equals("Tous") || p.getCategory().equals(selectedCategory);
            if (matchSearch && matchCategory) filtered.add(p);
        }
        productsTable.setItems(filtered);
    }

    private void updateStats() {
        try {
            var all = productService.getAll();
            statsTotalProducts.setText(String.valueOf(all.size()));
            statsAvailableProducts.setText(String.valueOf(all.stream().filter(Product::isAvailable).count()));
            statsLowStock.setText(String.valueOf(all.stream().filter(p -> p.getStock() < 10 && p.getStock() > 0).count()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddProduct() {
        showProductDialog(null);
    }

    @FXML
    private void handleEditProduct() {
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Erreur", "Sélectionnez un produit à modifier");
            return;
        }
        showProductDialog(selected);
    }

    @FXML
    private void handleDeleteProduct() {
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Erreur", "Sélectionnez un produit à supprimer");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer " + selected.getName() + " ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                productService.delete(selected.getId());
                loadProducts();
                updateStats();
                showAlert("Succès", "Produit supprimé !");
            } catch (SQLException e) {
                showAlert("Erreur", e.getMessage());
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadProducts();
        updateStats();
    }

    private void showProductDialog(Product product) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle(product == null ? "Ajouter un produit" : "Modifier le produit");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        TextArea descArea = new TextArea();
        descArea.setPrefRowCount(3);
        TextField priceField = new TextField();
        TextField stockField = new TextField();
        TextField imageField = new TextField();
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Coffrets", "Livres", "Accessoires", "Décorations", "Numérique");
        CheckBox availableCheck = new CheckBox("Disponible");

        if (product != null) {
            nameField.setText(product.getName());
            descArea.setText(product.getDescription());
            priceField.setText(String.valueOf(product.getPrice()));
            stockField.setText(String.valueOf(product.getStock()));
            imageField.setText(product.getImageUrl());
            categoryCombo.setValue(product.getCategory());
            availableCheck.setSelected(product.isAvailable());
        } else {
            categoryCombo.setValue("Accessoires");
            availableCheck.setSelected(true);
        }

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descArea, 1, 1);
        grid.add(new Label("Prix (€):"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label("Stock:"), 0, 3);
        grid.add(stockField, 1, 3);
        grid.add(new Label("Image URL:"), 0, 4);
        grid.add(imageField, 1, 4);
        grid.add(new Label("Catégorie:"), 0, 5);
        grid.add(categoryCombo, 1, 5);
        grid.add(availableCheck, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                if (nameField.getText().trim().isEmpty()) {
                    showAlert("Erreur", "Nom requis");
                    return null;
                }
                if (descArea.getText().trim().length() < 3) {
                    showAlert("Erreur", "Description trop courte");
                    return null;
                }
                double price;
                int stock;
                try {
                    price = Double.parseDouble(priceField.getText());
                    if (price <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    showAlert("Erreur", "Prix invalide");
                    return null;
                }
                try {
                    stock = Integer.parseInt(stockField.getText());
                    if (stock < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    showAlert("Erreur", "Stock invalide");
                    return null;
                }

                if (product == null) {
                    Product p = new Product(
                            nameField.getText().trim(),
                            descArea.getText().trim(),
                            price,
                            stock,
                            categoryCombo.getValue(),
                            imageField.getText().trim()
                    );
                    p.setAvailable(availableCheck.isSelected());
                    return p;
                } else {
                    product.setName(nameField.getText().trim());
                    product.setDescription(descArea.getText().trim());
                    product.setPrice(price);
                    product.setStock(stock);
                    product.setCategory(categoryCombo.getValue());
                    product.setImageUrl(imageField.getText().trim());
                    product.setAvailable(availableCheck.isSelected());
                    return product;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                if (product == null) {
                    productService.insert(result);
                } else {
                    productService.update(result);
                }
                loadProducts();
                updateStats();
                showAlert("Succès", "Produit sauvegardé !");
            } catch (SQLException e) {
                showAlert("Erreur", e.getMessage());
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
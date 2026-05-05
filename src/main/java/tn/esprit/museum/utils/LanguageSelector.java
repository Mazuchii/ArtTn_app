package tn.esprit.museum.utils;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

import java.util.Locale;

/**
 * Factory pour créer un ComboBox de sélection de langue.
 */
public final class LanguageSelector {

    private LanguageSelector() {}

    private static final String[] LABELS = {"🇫🇷 Français", "🇬🇧 English", "🇹🇳 العربية"};
    private static final String[] CODES  = {"fr", "en", "ar"};

    /**
     * Initialise un ComboBox existant (issu du FXML) avec les langues disponibles.
     * C'est la méthode à utiliser dans les contrôleurs.
     */
    public static void setup(ComboBox<String> combo) {
        if (combo == null) return;

        combo.getItems().setAll(LABELS);
        combo.setStyle(
            "-fx-background-color: #f8f8f8;" +
            "-fx-border-color: #D4AF37;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-font-size: 12px;" +
            "-fx-cursor: hand;"
        );
        combo.setPrefWidth(145);

        // Sélectionner la langue courante
        String currentCode = LocalizationManager.getInstance().getCurrentCode();
        for (int i = 0; i < CODES.length; i++) {
            if (CODES[i].equals(currentCode)) {
                combo.getSelectionModel().select(i);
                break;
            }
        }

        // Action : changer la langue selon l'index sélectionné dans CE combo
        combo.setOnAction(e -> {
            int idx = combo.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < CODES.length) {
                Locale locale = switch (CODES[idx]) {
                    case "en" -> LocalizationManager.LOCALE_EN;
                    case "ar" -> LocalizationManager.LOCALE_AR;
                    default   -> LocalizationManager.LOCALE_FR;
                };
                LocalizationManager.getInstance().setLocale(locale);
            }
        });

        combo.setCellFactory(lv -> styledCell());
        combo.setButtonCell(styledCell());
    }

    /** Crée et retourne un nouveau ComboBox configuré (pour usage programmatique). */
    public static ComboBox<String> create() {
        ComboBox<String> combo = new ComboBox<>();
        setup(combo);
        return combo;
    }

    private static ListCell<String> styledCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty) setStyle("-fx-font-size: 12px; -fx-padding: 4 8;");
            }
        };
    }
}

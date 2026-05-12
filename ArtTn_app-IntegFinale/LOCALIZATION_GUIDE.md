# Guide d'intégration — Système de localisation Museum Digital

## Fichiers créés / modifiés

| Fichier | Action |
|---------|--------|
| `src/main/resources/i18n/messages_fr.properties` | Créé — traductions françaises |
| `src/main/resources/i18n/messages_en.properties` | Créé — traductions anglaises |
| `src/main/resources/i18n/messages_ar.properties` | Créé — traductions arabes (Unicode) |
| `src/main/java/.../utils/LocalizationManager.java` | Créé — gestionnaire centralisé |
| `src/main/java/.../utils/LanguageSelector.java` | Créé — factory du ComboBox de langue |
| `src/main/java/.../MuseumApplication.java` | Modifié — initialisation au démarrage |
| `src/main/java/.../controllers/LoginController.java` | Modifié — updateTexts() + ComboBox |
| `src/main/java/.../controllers/UserHomeController.java` | Modifié — updateTexts() + ComboBox navbar |
| `src/main/resources/fxml/login.fxml` | Modifié — fx:id sur labels + ComboBox |
| `src/main/resources/fxml/user_home.fxml` | Modifié — ComboBox dans navbar |

---

## Architecture

```
LocalizationManager (Singleton)
    ├── charge ResourceBundle depuis i18n/messages_XX.properties
    ├── sauvegarde la langue dans java.util.prefs.Preferences
    ├── notifie les listeners via LocaleChangeListener
    └── applique NodeOrientation (RTL pour l'arabe)

LanguageSelector
    └── factory statique → ComboBox<String> prêt à l'emploi

Chaque contrôleur :
    ├── lm.addListener(this::updateTexts)  ← dans initialize()
    └── updateTexts()                       ← met à jour tous les FXML
```

---

## Intégrer la localisation dans un nouveau contrôleur

### Étape 1 — Ajouter les fx:id dans le FXML

```xml
<Label fx:id="myTitleLabel" text="Titre" />
<Button fx:id="myButton" text="Cliquer" />
```

### Étape 2 — Dans le contrôleur Java

```java
import tn.esprit.museum.utils.LocalizationManager;

public class MonController {

    @FXML private Label myTitleLabel;
    @FXML private Button myButton;

    private final LocalizationManager lm = LocalizationManager.getInstance();

    @FXML
    public void initialize() {
        // Enregistrer le listener
        lm.addListener(this::updateTexts);
        // Appliquer immédiatement
        updateTexts();
    }

    private void updateTexts() {
        javafx.application.Platform.runLater(() -> {
            if (myTitleLabel != null) myTitleLabel.setText(lm.getString("ma.cle.titre"));
            if (myButton     != null) myButton.setText(lm.getString("ma.cle.bouton"));
            // Orientation RTL pour l'arabe
            if (myTitleLabel != null && myTitleLabel.getScene() != null)
                lm.applyOrientation(myTitleLabel.getScene().getRoot());
        });
    }
}
```

### Étape 3 — Ajouter les clés dans les 3 fichiers .properties

```properties
# messages_fr.properties
ma.cle.titre=Mon Titre
ma.cle.bouton=Cliquer

# messages_en.properties
ma.cle.titre=My Title
ma.cle.bouton=Click

# messages_ar.properties
ma.cle.titre=\u0639\u0646\u0648\u0627\u0646\u064a
ma.cle.bouton=\u0627\u0636\u063a\u0637
```

---

## Utiliser les traductions dans les alertes

```java
private void showError(String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(lm.getString("app.error"));
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
}

// Appel :
showError(lm.getString("login.error.empty"));
```

---

## Ajouter le sélecteur de langue dans n'importe quelle vue

### Option A — Dans le FXML (recommandé)

```xml
<ComboBox fx:id="myLanguageCombo" prefWidth="145"
          style="-fx-border-color: #D4AF37; -fx-border-radius: 8; -fx-background-radius: 8;" />
```

```java
@FXML private ComboBox<String> myLanguageCombo;

// Dans initialize() :
if (myLanguageCombo != null) {
    ComboBox<String> built = LanguageSelector.create();
    myLanguageCombo.getItems().setAll(built.getItems());
    myLanguageCombo.getSelectionModel().select(built.getSelectionModel().getSelectedIndex());
    myLanguageCombo.setCellFactory(built.getCellFactory());
    myLanguageCombo.setButtonCell(built.getButtonCell());
    myLanguageCombo.setOnAction(built.getOnAction());
}
```

### Option B — Programmatiquement (sans FXML)

```java
ComboBox<String> langBox = LanguageSelector.create();
myToolbar.getChildren().add(langBox);
```

---

## Contrôleurs restants à adapter

Les contrôleurs suivants peuvent être adaptés en suivant le même pattern :

| Contrôleur | Clés principales à utiliser |
|------------|----------------------------|
| `ReservationController` | `admin.reservations.*` |
| `EventManagementController` | `admin.events.*` |
| `SeatSelectionController` | `seat.*` |
| `MyReservationsController` | `event.col.*`, `event.my.title` |
| `ProfileController` | `profile.*` |
| `RegisterController` | `register.*` |

---

## Ajouter une nouvelle langue

1. Créer `src/main/resources/i18n/messages_XX.properties`
2. Dans `LanguageSelector.java`, ajouter dans les tableaux :
   ```java
   private static final String[] LABELS = {"🇫🇷 Français", "🇬🇧 English", "🇹🇳 العربية", "🇩🇪 Deutsch"};
   private static final String[] CODES  = {"fr", "en", "ar", "de"};
   ```
3. Dans `LocalizationManager.localeFromCode()`, ajouter le cas :
   ```java
   case "de" -> Locale.GERMAN;
   ```

---

## Notes importantes

- **Les textes de la DB** (titres d'événements, noms de produits) ne sont **pas** traduits — seulement l'interface statique.
- **L'arabe** utilise `NodeOrientation.RIGHT_TO_LEFT` appliqué sur le nœud racine de chaque scène.
- **La langue est persistée** dans `java.util.prefs.Preferences` sous la clé `museum_language`.
- **UTF-8** : la classe `UTF8Control` dans `LocalizationManager` garantit la lecture correcte des caractères arabes et accentués.
- **Thread-safety** : `updateTexts()` utilise toujours `Platform.runLater()` pour les mises à jour UI.

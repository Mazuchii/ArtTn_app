package tn.esprit.museum.utils;

import javafx.geometry.NodeOrientation;
import javafx.scene.Node;

import java.text.MessageFormat;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * Gestionnaire centralisé de localisation.
 *
 * Usage :
 *   LocalizationManager.getInstance().setLocale(Locale.FRENCH);
 *   String text = LocalizationManager.getInstance().getString("login.title");
 *
 * Pour écouter les changements :
 *   LocalizationManager.getInstance().addListener(this::updateTexts);
 */
public class LocalizationManager {

    // ── Constantes ──────────────────────────────────────────────────────────
    public static final Locale LOCALE_FR = Locale.FRENCH;
    public static final Locale LOCALE_EN = Locale.ENGLISH;
    public static final Locale LOCALE_AR = new Locale("ar");

    private static final String BUNDLE_BASE = "i18n/messages";
    private static final String PREF_KEY    = "museum_language";

    // ── Singleton ────────────────────────────────────────────────────────────
    private static LocalizationManager instance;

    public static LocalizationManager getInstance() {
        if (instance == null) instance = new LocalizationManager();
        return instance;
    }

    // ── État interne ─────────────────────────────────────────────────────────
    private Locale          currentLocale;
    private ResourceBundle  bundle;
    private final List<LocaleChangeListener> listeners = new ArrayList<>();
    private final Preferences prefs = Preferences.userRoot().node("tn/esprit/museum");

    // ── Constructeur privé ───────────────────────────────────────────────────
    private LocalizationManager() {
        // Restaurer la langue sauvegardée, défaut = français
        String saved = prefs.get(PREF_KEY, "fr");
        currentLocale = localeFromCode(saved);
        loadBundle();
    }

    // ── API publique ─────────────────────────────────────────────────────────

    /** Retourne la locale courante. */
    public Locale getCurrentLocale() { return currentLocale; }

    /** Retourne le code ISO de la locale courante ("fr", "en", "ar"). */
    public String getCurrentCode() { return currentLocale.getLanguage(); }

    /**
     * Change la langue et notifie tous les listeners.
     * @param locale nouvelle locale (LOCALE_FR, LOCALE_EN ou LOCALE_AR)
     */
    public void setLocale(Locale locale) {
        if (locale.equals(currentLocale)) return;
        currentLocale = locale;
        loadBundle();
        prefs.put(PREF_KEY, locale.getLanguage());
        notifyListeners();
    }

    /**
     * Retourne la traduction pour la clé donnée.
     * Si la clé est absente, retourne "!key!" pour faciliter le débogage.
     */
    public String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
    }

    /**
     * Retourne la traduction avec substitution de paramètres (MessageFormat).
     * Ex : getString("seat.reserved.tooltip", "A1") → "Siège A1 — déjà réservé"
     */
    public String getString(String key, Object... args) {
        String pattern = getString(key);
        return MessageFormat.format(pattern, args);
    }

    /**
     * Applique l'orientation RTL/LTR sur un nœud JavaFX selon la locale courante.
     */
    public void applyOrientation(Node node) {
        if (node == null) return;
        if ("ar".equals(currentLocale.getLanguage())) {
            node.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        } else {
            node.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        }
    }

    /** Indique si la locale courante est RTL. */
    public boolean isRTL() {
        return "ar".equals(currentLocale.getLanguage());
    }

    // ── Gestion des listeners ────────────────────────────────────────────────

    /** Enregistre un listener appelé à chaque changement de langue. */
    public void addListener(LocaleChangeListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    /** Supprime un listener. */
    public void removeListener(LocaleChangeListener listener) {
        listeners.remove(listener);
    }

    // ── Méthodes internes ────────────────────────────────────────────────────

    private void loadBundle() {
        bundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLocale,
                new UTF8Control());
    }

    private void notifyListeners() {
        // Copie défensive pour éviter ConcurrentModificationException
        new ArrayList<>(listeners).forEach(LocaleChangeListener::onLocaleChanged);
    }

    private static Locale localeFromCode(String code) {
        return switch (code) {
            case "en" -> LOCALE_EN;
            case "ar" -> LOCALE_AR;
            default   -> LOCALE_FR;
        };
    }

    // ── Interface listener ───────────────────────────────────────────────────

    @FunctionalInterface
    public interface LocaleChangeListener {
        void onLocaleChanged();
    }

    // ── Contrôle UTF-8 pour les .properties ─────────────────────────────────

    /**
     * ResourceBundle.Control qui force la lecture en UTF-8.
     * Nécessaire pour l'arabe et les caractères accentués.
     */
    public static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale,
                                        String format, ClassLoader loader,
                                        boolean reload)
                throws java.io.IOException {
            String bundleName    = toBundleName(baseName, locale);
            String resourceName  = toResourceName(bundleName, "properties");
            try (var stream = loader.getResourceAsStream(resourceName)) {
                if (stream == null) return null;
                return new java.util.PropertyResourceBundle(
                        new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
            }
        }
    }
}

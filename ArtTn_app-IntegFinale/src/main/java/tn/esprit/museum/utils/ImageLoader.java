package tn.esprit.museum.utils;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralised image loader for the Museum Digital JavaFX application.
 *
 * <p>Images are stored in the Symfony project's {@code public/uploads} folder.
 * The database only keeps a relative path (e.g. {@code events/1234_photo.jpg}).
 * This utility resolves that relative path to an actual file on disk using the
 * base path configured in {@code config.properties}.</p>
 *
 * <h3>Resolution order</h3>
 * <ol>
 *   <li>Symfony uploads folder on disk
 *       ({@code symfony.uploads.path} + {@code /} + stored path)</li>
 *   <li>Absolute path as-is (in case the DB stores a full path)</li>
 *   <li>JavaFX classpath resource
 *       ({@code javafx.images.classpath} + stored path)</li>
 *   <li>Fallback default placeholder image from classpath</li>
 * </ol>
 *
 * <h3>Configuration</h3>
 * Edit {@code src/main/resources/config.properties} and set
 * {@code symfony.uploads.path} to the absolute path of your Symfony
 * {@code public/uploads} directory.
 */
public final class ImageLoader {

    private static final Logger LOGGER = Logger.getLogger(ImageLoader.class.getName());

    /** Config key for the Symfony uploads base directory. */
    public static final String KEY_UPLOADS_PATH = "symfony.uploads.path";

    /** Config key for the JavaFX classpath images prefix. */
    public static final String KEY_CLASSPATH_PREFIX = "javafx.images.classpath";

    /** Classpath location of the fallback placeholder image. */
    private static final String FALLBACK_CLASSPATH = "/images/museum_bardo.jpg";

    // ── Loaded once at class-init time ──────────────────────────────────────
    private static final String UPLOADS_BASE;
    private static final String CLASSPATH_PREFIX;
    private static Image fallbackImage;

    static {
        Properties props = new Properties();
        try (InputStream in = ImageLoader.class.getResourceAsStream("/config.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                LOGGER.warning("config.properties not found on classpath – using defaults.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not load config.properties", e);
        }

        UPLOADS_BASE     = props.getProperty(KEY_UPLOADS_PATH,    "").trim();
        CLASSPATH_PREFIX = props.getProperty(KEY_CLASSPATH_PREFIX, "/images").trim();
    }

    private ImageLoader() {}

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Loads an event image from the given stored path.
     *
     * @param storedPath the value from the database (may be null / empty)
     * @return a valid {@link Image}, never {@code null}
     */
    public static Image load(String storedPath) {
        if (storedPath != null && !storedPath.isBlank()) {
            String path = storedPath.trim();

            // 1. Exact classpath match — fastest, works after images are copied to resources
            Image img = tryClasspathExact(path);
            if (img != null) return img;

            // 2. Symfony uploads folder on disk (configured path)
            img = trySymfonyUploads(path);
            if (img != null) return img;

            // 3. Absolute path as-is
            img = tryAbsolutePath(path);
            if (img != null) return img;

            // 4. Fuzzy hash-based match in classpath (last resort, only if no exact match)
            String baseName = extractBaseName(path);
            if (!baseName.isEmpty()) {
                img = tryClasspathFuzzy(baseName);
                if (img != null) return img;
            }
        }

        // 5. Fallback placeholder
        return getFallback();
    }

    /**
     * Returns the configured Symfony uploads base path (may be empty if not set).
     */
    public static String getUploadsBasePath() {
        return UPLOADS_BASE;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Tries an exact classpath lookup using the stored filename directly.
     * Works when the image file has been copied into src/main/resources/images/
     * with its original Symfony filename (e.g. "hash-suffix.jpg").
     */
    private static Image tryClasspathExact(String storedPath) {
        String filename = new File(storedPath).getName(); // strip any path prefix
        String[] candidates = {
            CLASSPATH_PREFIX + "/" + filename,   // /images/hash-suffix.jpg
            "/" + filename,                       // /hash-suffix.jpg
            storedPath.startsWith("/") ? storedPath : "/" + storedPath
        };
        for (String cp : candidates) {
            try (InputStream in = ImageLoader.class.getResourceAsStream(cp)) {
                if (in != null) {
                    Image img = new Image(in);
                    if (!img.isError()) {
                        LOGGER.fine("Exact classpath hit: " + cp);
                        return img;
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Exact classpath failed: " + cp, e);
            }
        }
        return null;
    }

    /**
     * Tries to load from the Symfony uploads directory.
     * Handles both "events/file.jpg" and "/events/file.jpg" stored paths.
     */
    private static Image trySymfonyUploads(String storedPath) {
        if (UPLOADS_BASE.isEmpty()) return null;

        // Normalise: remove leading slash so we can join cleanly
        String relative = storedPath.startsWith("/") ? storedPath.substring(1) : storedPath;
        File file = new File(UPLOADS_BASE, relative);

        if (file.exists() && file.isFile()) {
            try {
                Image img = new Image(file.toURI().toString());
                if (!img.isError()) {
                    return img;
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Symfony path load failed: " + file.getAbsolutePath(), e);
            }
        }
        return null;
    }

    /** Tries to load the path as an absolute file path or a file: URI. */
    private static Image tryAbsolutePath(String storedPath) {
        try {
            // Already a file: or http: URI
            if (storedPath.startsWith("file:") || storedPath.startsWith("http")) {
                Image img = new Image(storedPath, true);
                if (!img.isError()) return img;
            }

            // Plain absolute path
            File file = new File(storedPath);
            if (file.isAbsolute() && file.exists()) {
                Image img = new Image(file.toURI().toString());
                if (!img.isError()) return img;
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Absolute path load failed: " + storedPath, e);
        }
        return null;
    }

    /** Tries to load from the JavaFX classpath resources folder. */
    private static Image tryClasspath(String storedPath) {
        // Extract the base name from Symfony-style filenames.
        // DB format:     {hash}-{symfony_suffix}.jpg  e.g. "97590ffa-6a029.jpg"
        // JavaFX format: {timestamp}_{hash}.jpg       e.g. "1778553_97590ffa.jpg"
        // Strategy: strip the Symfony suffix (everything after the last '-') to get
        // the base name, then find any classpath file whose name contains that base.
        String baseName = extractBaseName(storedPath);

        // Build candidate classpath paths
        String[] candidates = {
            storedPath,                                                    // exact match
            CLASSPATH_PREFIX + "/" + stripLeadingSlash(storedPath),       // /images/exact
            CLASSPATH_PREFIX + "/" + new File(storedPath).getName()        // /images/filename
        };

        for (String candidate : candidates) {
            String cp = candidate.startsWith("/") ? candidate : "/" + candidate;
            try (InputStream in = ImageLoader.class.getResourceAsStream(cp)) {
                if (in != null) {
                    Image img = new Image(in);
                    if (!img.isError()) return img;
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Classpath load failed: " + cp, e);
            }
        }

        // Hash-based fuzzy match: scan the classpath images folder for a file
        // whose name contains the extracted base name.
        if (!baseName.isEmpty()) {
            Image img = tryClasspathFuzzy(baseName);
            if (img != null) return img;
        }

        return null;
    }

    /**
     * Scans the classpath images directory for a file whose name contains
     * {@code baseName} (case-insensitive).  This bridges the gap between
     * Symfony filenames ({@code hash-suffix.jpg}) and JavaFX resource
     * filenames ({@code timestamp_hash.jpg}).
     */
    private static Image tryClasspathFuzzy(String baseName) {
        // We need a real directory to scan – use the file-system path of the
        // resources/images folder when running from the IDE/Maven.
        String[] searchRoots = {
            System.getProperty("user.dir") + "/src/main/resources" + CLASSPATH_PREFIX,
            System.getProperty("user.dir") + "/target/classes" + CLASSPATH_PREFIX
        };

        for (String root : searchRoots) {
            File dir = new File(root);
            if (!dir.isDirectory()) continue;

            File[] files = dir.listFiles((d, name) ->
                    name.toLowerCase().contains(baseName.toLowerCase())
                    && (name.endsWith(".jpg") || name.endsWith(".jpeg")
                        || name.endsWith(".png") || name.endsWith(".gif")));

            if (files != null && files.length > 0) {
                // Prefer the most recently added file (highest timestamp prefix)
                File best = files[0];
                for (File f : files) {
                    if (f.getName().compareTo(best.getName()) > 0) best = f;
                }
                try {
                    Image img = new Image(best.toURI().toString());
                    if (!img.isError()) {
                        LOGGER.fine("Fuzzy match: " + best.getName() + " for base=" + baseName);
                        return img;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Fuzzy load failed: " + best.getAbsolutePath(), e);
                }
            }
        }
        return null;
    }

    /**
     * Extracts the base name from a Symfony-style image filename.
     * <ul>
     *   <li>{@code 97590ffa-6a029.jpg}  →  {@code 97590ffa}</li>
     *   <li>{@code theatre3-6a028.jpg}  →  {@code theatre3}</li>
     *   <li>{@code plain.jpg}           →  {@code plain}</li>
     * </ul>
     */
    private static String extractBaseName(String storedPath) {
        String name = new File(storedPath).getName();
        // Remove extension
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        // Remove Symfony suffix: last segment after '-' if it looks like a hex id
        int dash = name.lastIndexOf('-');
        if (dash > 0) {
            String suffix = name.substring(dash + 1);
            // Symfony suffixes are hex strings (e.g. "6a0291afa3f1e")
            if (suffix.matches("[0-9a-f]+")) {
                name = name.substring(0, dash);
            }
        }
        return name;
    }

    /** Returns (and caches) the fallback placeholder image. */
    private static Image getFallback() {
        if (fallbackImage == null || fallbackImage.isError()) {
            try (InputStream in = ImageLoader.class.getResourceAsStream(FALLBACK_CLASSPATH)) {
                if (in != null) {
                    fallbackImage = new Image(in);
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Fallback image not found at " + FALLBACK_CLASSPATH, e);
            }
        }
        // If even the fallback is missing, return a blank Image
        if (fallbackImage != null && !fallbackImage.isError()) return fallbackImage;
        try (InputStream in = ImageLoader.class.getResourceAsStream(FALLBACK_CLASSPATH)) {
            if (in != null) return new Image(in);
        } catch (Exception ignored) {}
        return new Image(""); // truly empty – JavaFX renders nothing
    }

    private static String stripLeadingSlash(String s) {
        return s.startsWith("/") ? s.substring(1) : s;
    }
}

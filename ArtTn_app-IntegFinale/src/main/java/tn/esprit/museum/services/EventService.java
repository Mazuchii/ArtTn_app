package tn.esprit.museum.services;

import tn.esprit.museum.entities.Event;
import tn.esprit.museum.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EventService {

    // ==================== VÉRIFICATION DOUBLONS ====================

    public boolean isTitleExists(String title) throws SQLException {
        String sql = "SELECT COUNT(*) FROM `" + getEventsTableName() + "` WHERE title = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public boolean isEventAtSameDateTimeExists(LocalDate date, LocalTime time) throws SQLException {
        String sql = "SELECT COUNT(*) FROM `" + getEventsTableName() + "` WHERE event_date = ? AND event_time = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(date));
            pstmt.setTime(2, Time.valueOf(time));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public boolean isExactDuplicate(String title, LocalDate date, LocalTime time, String location) throws SQLException {
        String sql = "SELECT COUNT(*) FROM `" + getEventsTableName() + "` WHERE title = ? AND event_date = ? AND event_time = ? AND location = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setDate(2, Date.valueOf(date));
            pstmt.setTime(3, Time.valueOf(time));
            pstmt.setString(4, location);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public boolean checkDuplicateBeforeInsert(String title, LocalDate date, LocalTime time, String location) throws SQLException {
        if (isTitleExists(title)) return true;
        if (isEventAtSameDateTimeExists(date, time)) return true;
        return isExactDuplicate(title, date, time, location);
    }

    public boolean insertIfNotExists(Event event) throws SQLException {
        if (checkDuplicateBeforeInsert(event.getTitle(), event.getEventDate(), event.getEventTime(), event.getLocation()))
            return false;
        insert(event);
        return true;
    }

    // ==================== CREATE ====================
    public void insert(Event event) throws SQLException {
        event.setCurrentCapacity(event.getMaxCapacity());
        String t = getEventsTableName();
        String sql = "INSERT INTO `" + t + "` (title, description, event_date, event_time, location, " +
                "max_capacity, current_capacity, price, category, status, image_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, event.getTitle());
            pstmt.setString(2, event.getDescription());
            pstmt.setDate(3, Date.valueOf(event.getEventDate()));
            pstmt.setTime(4, Time.valueOf(event.getEventTime()));
            pstmt.setString(5, event.getLocation());
            pstmt.setInt(6, event.getMaxCapacity());
            pstmt.setInt(7, event.getCurrentCapacity()); // = max_capacity
            pstmt.setBigDecimal(8, event.getPrice());
            pstmt.setString(9, event.getCategory());
            pstmt.setString(10, toDbEventStatus(event.getStatus()));
            pstmt.setString(11, event.getImageUrl());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) event.setId(rs.getInt(1));
            }
        }
    }

    // ==================== READ ====================
    public List<Event> getAll() throws SQLException {
        List<Event> events = new ArrayList<>();

        // Essayer d'abord 'events' (nom desktop), puis 'event' (nom Symfony par défaut)
        String tableName = getEventsTableName();
        String sql = "SELECT * FROM `" + tableName + "` ORDER BY event_date ASC, event_time ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                try {
                    events.add(mapResultSetToEvent(rs));
                } catch (SQLException e) {
                    System.err.println("⚠️ Erreur lecture événement id=" + safeGetId(rs) + ": " + e.getMessage());
                }
            }
        }
        System.out.println("✅ getAll() events: " + events.size() + " événement(s) chargé(s) depuis '" + tableName + "'");
        return events;
    }

    /** Détecte le nom réel de la table des événements (events ou event). */
    private static String cachedTableName = null;
    private static String getEventsTableName() {
        if (cachedTableName != null) return cachedTableName;
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Vérifier si 'events' existe
            try (ResultSet rs = conn.getMetaData().getTables(null, null, "events", new String[]{"TABLE"})) {
                if (rs.next()) { cachedTableName = "events"; return "events"; }
            }
            // Sinon essayer 'event'
            try (ResultSet rs = conn.getMetaData().getTables(null, null, "event", new String[]{"TABLE"})) {
                if (rs.next()) { cachedTableName = "event"; return "event"; }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Impossible de détecter le nom de la table events: " + e.getMessage());
        }
        cachedTableName = "events"; // fallback
        return "events";
    }

    private static int safeGetId(ResultSet rs) {
        try { return rs.getInt("id"); } catch (Exception e) { return -1; }
    }

    public List<Event> getData() {
        try {
            return getAll();
        } catch (SQLException e) {
            System.err.println("Erreur getData events: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<String> getAllCategories() {
        try {
            return getAll().stream()
                    .map(Event::getCategory)
                    .filter(category -> category != null && !category.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            System.err.println("Erreur getAllCategories events: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Event findById(int id) {
        try {
            return getById(id);
        } catch (SQLException e) {
            System.err.println("Erreur findById event: " + e.getMessage());
            return null;
        }
    }

    public Event getById(int id) throws SQLException {
        String sql = "SELECT * FROM `" + getEventsTableName() + "` WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToEvent(rs);
            }
        }
        return null;
    }

    public List<Event> getByCategory(String category) throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM `" + getEventsTableName() + "` WHERE category = ? ORDER BY event_date ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) events.add(mapResultSetToEvent(rs));
            }
        }
        return events;
    }

    public List<Event> getUpcomingEvents() throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM `" + getEventsTableName() + "` ORDER BY event_date ASC, event_time ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                try {
                    Event e = mapResultSetToEvent(rs);
                    String status = e.getStatus();
                    if (!Event.STATUS_TERMINE.equals(status) && !Event.STATUS_ANNULE.equals(status)) {
                        events.add(e);
                    }
                } catch (SQLException ex) {
                    System.err.println("⚠️ Erreur lecture événement: " + ex.getMessage());
                }
            }
        }
        return events;
    }

    public int getUpcomingEventsCount() throws SQLException {
        // Compter depuis Java après normalisation du statut pour être compatible Symfony
        return getUpcomingEvents().size();
    }

    public List<Event> search(String keyword) throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM `" + getEventsTableName() + "` WHERE title LIKE ? OR description LIKE ? OR location LIKE ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) events.add(mapResultSetToEvent(rs));
            }
        }
        return events;
    }

    // ==================== UPDATE ====================
    public void update(Event event) throws SQLException {
        String sql = "UPDATE `" + getEventsTableName() + "` SET title=?, description=?, event_date=?, event_time=?, " +
                "location=?, max_capacity=?, price=?, category=?, status=?, image_url=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, event.getTitle());
            pstmt.setString(2, event.getDescription());
            pstmt.setDate(3, Date.valueOf(event.getEventDate()));
            pstmt.setTime(4, Time.valueOf(event.getEventTime()));
            pstmt.setString(5, event.getLocation());
            pstmt.setInt(6, event.getMaxCapacity());
            pstmt.setBigDecimal(7, event.getPrice());
            pstmt.setString(8, event.getCategory());
            pstmt.setString(9, toDbEventStatus(event.getStatus()));
            pstmt.setString(10, event.getImageUrl());
            pstmt.setInt(11, event.getId());
            pstmt.executeUpdate();
        }
    }

    public void updateCapacity(int eventId, int newCurrentCapacity) throws SQLException {
        String sql = "UPDATE `" + getEventsTableName() + "` SET current_capacity = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newCurrentCapacity);
            pstmt.setInt(2, eventId);
            pstmt.executeUpdate();
        }
    }

    /** Version transactionnelle — utilise une connexion existante */
    public void updateCapacity(int eventId, int newCurrentCapacity, Connection conn) throws SQLException {
        String sql = "UPDATE `" + getEventsTableName() + "` SET current_capacity = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newCurrentCapacity);
            pstmt.setInt(2, eventId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Recalcule et synchronise current_capacity = max_capacity - SUM(tickets réservés actifs).
     * current_capacity représente les PLACES RESTANTES (disponibles).
     * Accepte les valeurs DB réelles pour les statuts annulé/refusé.
     */
    public void syncCapacityFromReservations(int eventId) throws SQLException {
        syncCapacityFromReservations(eventId, null);
    }

    /**
     * Version transactionnelle — utilise une connexion existante.
     */
    public void syncCapacityFromReservations(int eventId, Connection existingConn) throws SQLException {
        // Détecter les valeurs DB des statuts annulé/refusé
        String dbAnnulee = detectDbReservationStatus("ANNULEE");
        String dbRefusee = detectDbReservationStatus("REFUSEE");

        String sql = "UPDATE `" + getEventsTableName() + "` SET current_capacity = GREATEST(0, max_capacity - (" +
                "SELECT COALESCE(SUM(number_of_tickets), 0) FROM reservations " +
                "WHERE event_id = ? AND status NOT IN (?, ?)" +
                ")) WHERE id = ?";

        if (existingConn != null) {
            try (PreparedStatement pstmt = existingConn.prepareStatement(sql)) {
                pstmt.setInt(1, eventId);
                pstmt.setString(2, dbAnnulee);
                pstmt.setString(3, dbRefusee);
                pstmt.setInt(4, eventId);
                pstmt.executeUpdate();
            }
        } else {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, eventId);
                pstmt.setString(2, dbAnnulee);
                pstmt.setString(3, dbRefusee);
                pstmt.setInt(4, eventId);
                pstmt.executeUpdate();
            }
        }
    }

    /**
     * Détecte la valeur DB réelle d'un statut de réservation (ANNULEE, REFUSEE, etc.)
     * en interrogeant l'ENUM de la colonne status de la table reservations.
     */
    private static java.util.Map<String, String> reservationStatusCache = new java.util.HashMap<>();
    private static boolean reservationStatusDetected = false;

    private static synchronized void detectReservationStatuses() {
        if (reservationStatusDetected) return;
        reservationStatusDetected = true;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT COLUMN_TYPE FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reservations' AND COLUMN_NAME = 'status'";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String colType = rs.getString("COLUMN_TYPE");
                    if (colType != null && colType.toLowerCase().startsWith("enum")) {
                        String inner = colType.replaceAll("(?i)enum\\(", "").replaceAll("\\)$", "");
                        String[] values = inner.split("','");
                        for (String v : values) {
                            v = v.replace("'", "").trim();
                            String vl = v.toLowerCase();
                            if (vl.contains("annul") || vl.contains("cancel"))
                                reservationStatusCache.put("ANNULEE", v);
                            else if (vl.contains("refus") || vl.contains("reject") || vl.contains("refused"))
                                reservationStatusCache.put("REFUSEE", v);
                            else if (vl.contains("confirm"))
                                reservationStatusCache.put("CONFIRMEE", v);
                            else if (vl.contains("attente") || vl.contains("pending") || vl.contains("waiting"))
                                reservationStatusCache.put("EN_ATTENTE", v);
                        }
                        System.out.println("✅ Statuts réservation DB détectés: " + reservationStatusCache);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Impossible de détecter les statuts réservation: " + e.getMessage());
        }
    }

    public static String detectDbReservationStatus(String javaStatus) {
        detectReservationStatuses();
        return reservationStatusCache.getOrDefault(javaStatus, javaStatus);
    }

    /**
     * Retourne le nombre de tickets réellement réservés (actifs) pour un événement.
     */
    public int getReservedTicketsCount(int eventId) throws SQLException {
        String dbAnnulee = detectDbReservationStatus("ANNULEE");
        String dbRefusee = detectDbReservationStatus("REFUSEE");
        String sql = "SELECT COALESCE(SUM(number_of_tickets), 0) as reserved " +
                "FROM reservations WHERE event_id = ? AND status NOT IN (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setString(2, dbAnnulee);
            pstmt.setString(3, dbRefusee);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("reserved");
            }
        }
        return 0;
    }

    /**
     * Retourne le nombre de places restantes directement depuis la DB.
     * current_capacity = max_capacity - tickets_réservés_actifs.
     */
    public int getAvailableSeatsFromDB(int eventId) throws SQLException {
        String dbAnnulee = detectDbReservationStatus("ANNULEE");
        String dbRefusee = detectDbReservationStatus("REFUSEE");
        String sql = "SELECT GREATEST(0, max_capacity - COALESCE((" +
                "SELECT SUM(number_of_tickets) FROM reservations " +
                "WHERE event_id = ? AND status NOT IN (?, ?)" +
                "), 0)) as available FROM `" + getEventsTableName() + "` WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setString(2, dbAnnulee);
            pstmt.setString(3, dbRefusee);
            pstmt.setInt(4, eventId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("available");
            }
        }
        return 0;
    }

    public void updateStatus(int eventId, String status) throws SQLException {
        String sql = "UPDATE `" + getEventsTableName() + "` SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, toDbEventStatus(status));
            pstmt.setInt(2, eventId);
            pstmt.executeUpdate();
        }
    }

    // ==================== DELETE ====================
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM `" + getEventsTableName() + "` WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // ==================== STATISTIQUES ====================
    public int getTotalEvents() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM `" + getEventsTableName() + "`";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("total");
        }
        return 0;
    }

    // ==================== MAP ====================
    private Event mapResultSetToEvent(ResultSet rs) throws SQLException {
        Event event = new Event();

        // id — peut s'appeler id ou event_id
        try { event.setId(rs.getInt("id")); } catch (SQLException e1) {
            try { event.setId(rs.getInt("event_id")); } catch (SQLException ignored) {}
        }

        // title — peut s'appeler title ou name ou titre
        try { event.setTitle(rs.getString("title")); } catch (SQLException e1) {
            try { event.setTitle(rs.getString("name")); } catch (SQLException e2) {
                try { event.setTitle(rs.getString("titre")); } catch (SQLException ignored) {}
            }
        }

        // description
        try { event.setDescription(rs.getString("description")); } catch (SQLException ignored) {}

        // event_date — peut s'appeler date ou start_date ou event_date
        Date date = null;
        try { date = rs.getDate("event_date"); } catch (SQLException e1) {
            try { date = rs.getDate("date"); } catch (SQLException e2) {
                try { date = rs.getDate("start_date"); } catch (SQLException ignored) {}
            }
        }
        if (date != null) event.setEventDate(date.toLocalDate());

        // event_time — peut s'appeler time ou start_time ou event_time
        try {
            Time time = null;
            try { time = rs.getTime("event_time"); } catch (SQLException e1) {
                try { time = rs.getTime("time"); } catch (SQLException e2) {
                    try { time = rs.getTime("start_time"); } catch (SQLException ignored) {}
                }
            }
            if (time != null) event.setEventTime(time.toLocalTime());
            else event.setEventTime(java.time.LocalTime.of(0, 0));
        } catch (Exception ignored) {
            event.setEventTime(java.time.LocalTime.of(0, 0));
        }

        // location — peut s'appeler location ou lieu ou place ou venue
        try { event.setLocation(rs.getString("location")); } catch (SQLException e1) {
            try { event.setLocation(rs.getString("lieu")); } catch (SQLException e2) {
                try { event.setLocation(rs.getString("place")); } catch (SQLException e3) {
                    try { event.setLocation(rs.getString("venue")); } catch (SQLException ignored) {}
                }
            }
        }

        // max_capacity — peut s'appeler capacity ou max_capacity ou capacite
        try { event.setMaxCapacity(rs.getInt("max_capacity")); } catch (SQLException e1) {
            try { event.setMaxCapacity(rs.getInt("capacity")); } catch (SQLException e2) {
                try { event.setMaxCapacity(rs.getInt("capacite")); } catch (SQLException ignored) {}
            }
        }

        // current_capacity — places restantes
        try { event.setCurrentCapacity(rs.getInt("current_capacity")); } catch (SQLException ignored) {
            event.setCurrentCapacity(event.getMaxCapacity()); // par défaut toutes les places sont dispo
        }

        // price — peut s'appeler price ou prix ou tarif
        try { event.setPrice(rs.getBigDecimal("price")); } catch (SQLException e1) {
            try { event.setPrice(rs.getBigDecimal("prix")); } catch (SQLException e2) {
                try { event.setPrice(rs.getBigDecimal("tarif")); } catch (SQLException ignored) {}
            }
        }

        // category — peut s'appeler category ou categorie ou type
        try { event.setCategory(rs.getString("category")); } catch (SQLException e1) {
            try { event.setCategory(rs.getString("categorie")); } catch (SQLException e2) {
                try { event.setCategory(rs.getString("type")); } catch (SQLException ignored) {}
            }
        }
        // Valeur par défaut si category est null
        if (event.getCategory() == null || event.getCategory().isBlank()) {
            event.setCategory("EXPOSITION");
        }

        // status — normaliser les valeurs venant du web
        try {
            String rawStatus = rs.getString("status");
            event.setStatus(normalizeStatus(rawStatus));
        } catch (SQLException ignored) {
            event.setStatus(Event.STATUS_A_VENIR);
        }

        // image_url — peut s'appeler image_url, image, image_path, photo, affiche
        try { event.setImageUrl(rs.getString("image_url")); } catch (SQLException e1) {
            try { event.setImageUrl(rs.getString("image")); } catch (SQLException e2) {
                try { event.setImageUrl(rs.getString("image_path")); } catch (SQLException e3) {
                    try { event.setImageUrl(rs.getString("photo")); } catch (SQLException e4) {
                        try { event.setImageUrl(rs.getString("affiche")); } catch (SQLException ignored) {}
                    }
                }
            }
        }

        return event;
    }

    /**
     * Normalise les valeurs de statut venant du projet web (Symfony) vers les constantes
     * attendues par l'application desktop.
     * Symfony peut stocker : "a_venir", "en_cours", "termine", "annule", "active", "published", etc.
     */
    private String normalizeStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) return Event.STATUS_A_VENIR;
        switch (rawStatus.trim().toLowerCase()) {
            case "a_venir":
            case "avenir":
            case "upcoming":
            case "active":
            case "published":
            case "actif":
            case "1":
                return Event.STATUS_A_VENIR;
            case "en_cours":
            case "encours":
            case "ongoing":
            case "in_progress":
            case "2":
                return Event.STATUS_EN_COURS;
            case "termine":
            case "terminé":
            case "finished":
            case "done":
            case "past":
            case "3":
                return Event.STATUS_TERMINE;
            case "annule":
            case "annulé":
            case "cancelled":
            case "canceled":
            case "4":
                return Event.STATUS_ANNULE;
            default:
                // Retourner la valeur telle quelle si elle correspond déjà aux constantes
                if (rawStatus.equals(Event.STATUS_A_VENIR) || rawStatus.equals(Event.STATUS_EN_COURS)
                        || rawStatus.equals(Event.STATUS_TERMINE) || rawStatus.equals(Event.STATUS_ANNULE)) {
                    return rawStatus;
                }
                // Valeur inconnue → considérer comme A_VENIR pour ne pas masquer l'événement
                System.out.println("⚠️ Statut événement inconnu: '" + rawStatus + "' → traité comme A_VENIR");
                return Event.STATUS_A_VENIR;
        }
    }

    // ==================== CONVERSION STATUT JAVA → DB ====================

    /**
     * Cache des valeurs ENUM détectées dans la colonne status de la table events.
     * Clé = constante Java (A_VENIR, EN_COURS...), Valeur = valeur réelle en DB.
     */
    private static final java.util.Map<String, String> eventStatusCache = new java.util.HashMap<>();
    private static boolean eventStatusDetected = false;

    private static synchronized void detectEventStatuses() {
        if (eventStatusDetected) return;
        eventStatusDetected = true;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT COLUMN_TYPE FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '" + getEventsTableName() + "' AND COLUMN_NAME = 'status'";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String colType = rs.getString("COLUMN_TYPE");
                    if (colType != null && colType.toLowerCase().startsWith("enum")) {
                        String inner = colType.replaceAll("(?i)enum\\(", "").replaceAll("\\)$", "");
                        String[] values = inner.split("','");
                        for (String v : values) {
                            v = v.replace("'", "").trim();
                            String vl = v.toLowerCase();
                            if (vl.contains("a_venir") || vl.equals("active") || vl.equals("upcoming") || vl.equals("actif") || vl.equals("published"))
                                eventStatusCache.put(Event.STATUS_A_VENIR, v);
                            else if (vl.contains("en_cours") || vl.equals("ongoing") || vl.equals("in_progress"))
                                eventStatusCache.put(Event.STATUS_EN_COURS, v);
                            else if (vl.contains("termin") || vl.equals("finished") || vl.equals("done") || vl.equals("past"))
                                eventStatusCache.put(Event.STATUS_TERMINE, v);
                            else if (vl.contains("annul") || vl.equals("cancelled") || vl.equals("canceled"))
                                eventStatusCache.put(Event.STATUS_ANNULE, v);
                        }
                        System.out.println("✅ Statuts événement DB détectés: " + eventStatusCache);
                    }
                    // Si VARCHAR ou ENUM non reconnu → pas de mapping, on utilise la valeur Java directement
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Impossible de détecter les statuts événement: " + e.getMessage());
        }
    }

    /**
     * Convertit un statut Java (A_VENIR, EN_COURS...) vers la valeur réelle stockée en DB.
     * Si la DB utilise un ENUM Symfony (a_venir, en_cours...), adapte automatiquement.
     */
    private static String toDbEventStatus(String javaStatus) {
        detectEventStatuses();
        if (javaStatus == null) return eventStatusCache.getOrDefault(Event.STATUS_A_VENIR, Event.STATUS_A_VENIR);
        // Si la valeur Java est directement dans le cache, retourner la valeur DB
        if (eventStatusCache.containsKey(javaStatus)) return eventStatusCache.get(javaStatus);
        // Si le cache est vide (VARCHAR ou détection échouée), retourner la valeur Java telle quelle
        return javaStatus;
    }
}


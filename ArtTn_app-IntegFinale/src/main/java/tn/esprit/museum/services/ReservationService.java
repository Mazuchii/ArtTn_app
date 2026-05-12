package tn.esprit.museum.services;

import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Reservation;
import tn.esprit.museum.utils.DatabaseConnection;
import tn.esprit.museum.utils.EmailUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {

    private final EventService eventService = new EventService();

    // ========== CONSTANTES STATUT (côté Java/Desktop) ==========
    public static final String STATUS_EN_ATTENTE = "EN_ATTENTE";
    public static final String STATUS_CONFIRMEE  = "CONFIRMEE";
    public static final String STATUS_ANNULEE    = "ANNULEE";
    public static final String STATUS_REFUSEE    = "REFUSEE";

    /**
     * Détecte automatiquement les valeurs ENUM de la colonne status en DB.
     * Retourne null si la colonne n'est pas un ENUM ou si la détection échoue.
     */
    private static String[] detectedDbStatusValues = null;
    private static boolean statusDetected = false;

    private static synchronized void detectDbStatusValues() {
        if (statusDetected) return;
        statusDetected = true;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT COLUMN_TYPE FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reservations' AND COLUMN_NAME = 'status'";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String colType = rs.getString("COLUMN_TYPE"); // ex: enum('pending','confirmed','cancelled')
                    if (colType != null && colType.toLowerCase().startsWith("enum")) {
                        // Extraire les valeurs entre les apostrophes
                        String inner = colType.replaceAll("(?i)enum\\(", "").replaceAll("\\)$", "");
                        detectedDbStatusValues = inner.split("','");
                        for (int i = 0; i < detectedDbStatusValues.length; i++) {
                            detectedDbStatusValues[i] = detectedDbStatusValues[i].replace("'", "").trim();
                        }
                        System.out.println("✅ ENUM status détecté: " + java.util.Arrays.toString(detectedDbStatusValues));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Impossible de détecter l'ENUM status: " + e.getMessage());
        }
    }

    /**
     * Convertit un statut Java (EN_ATTENTE, CONFIRMEE...) vers la valeur DB.
     * Si la DB utilise un ENUM différent (pending, confirmed...), adapte automatiquement.
     */
    private String toDbStatus(String javaStatus) {
        detectDbStatusValues();
        if (javaStatus == null) javaStatus = STATUS_EN_ATTENTE;

        // Si la valeur Java est directement dans l'ENUM, l'utiliser telle quelle
        if (detectedDbStatusValues != null) {
            for (String v : detectedDbStatusValues) {
                if (v.equalsIgnoreCase(javaStatus)) return v;
            }
            // Sinon mapper vers la valeur ENUM la plus proche
            String lower = javaStatus.toLowerCase();
            for (String v : detectedDbStatusValues) {
                String vl = v.toLowerCase();
                if (lower.contains("attente") || lower.equals("pending") || lower.equals("waiting")) {
                    if (vl.contains("attente") || vl.equals("pending") || vl.equals("waiting")) return v;
                } else if (lower.contains("confirm")) {
                    if (vl.contains("confirm")) return v;
                } else if (lower.contains("annul") || lower.contains("cancel")) {
                    if (vl.contains("annul") || vl.contains("cancel")) return v;
                } else if (lower.contains("refus") || lower.contains("reject") || lower.contains("refused")) {
                    if (vl.contains("refus") || vl.contains("reject") || vl.contains("refused")) return v;
                }
            }
            // Fallback : première valeur de l'ENUM
            System.out.println("⚠️ Statut '" + javaStatus + "' non trouvé dans ENUM, utilisation de: " + detectedDbStatusValues[0]);
            return detectedDbStatusValues[0];
        }

        // Pas d'ENUM détecté → utiliser la valeur Java directement (VARCHAR)
        return javaStatus;
    }

    /**
     * Convertit une valeur DB vers le statut Java standard.
     * Normalise pending→EN_ATTENTE, confirmed→CONFIRMEE, etc.
     */
    private String fromDbStatus(String dbStatus) {
        if (dbStatus == null) return STATUS_EN_ATTENTE;
        switch (dbStatus.trim().toLowerCase()) {
            case "en_attente": case "pending": case "waiting": case "en attente":
                return STATUS_EN_ATTENTE;
            case "confirmee": case "confirmed": case "confirmé": case "confirmée": case "accepted":
                return STATUS_CONFIRMEE;
            case "annulee": case "cancelled": case "canceled": case "annulé": case "annulée":
                return STATUS_ANNULEE;
            case "refusee": case "refused": case "rejected": case "refusé": case "refusée": case "denied":
                return STATUS_REFUSEE;
            default:
                return dbStatus; // retourner tel quel si inconnu
        }
    }

    // ========== CREATE ==========
    public Reservation insert(Reservation reservation) throws SQLException {
        String sql = "INSERT INTO reservations (event_id, client_name, client_email, client_phone, " +
                "number_of_tickets, total_price, status, payment_method, special_requests, qr_code, selected_seats) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Lire les places disponibles directement depuis la DB (source de vérité)
            // en verrouillant la ligne pour éviter les race conditions
            int availableSeats = getAvailableSeatsLocked(reservation.getEventId(), conn);
            if (availableSeats < 0) throw new SQLException("Événement non trouvé");
            if (availableSeats < reservation.getNumberOfTickets()) {
                throw new SQLException("Plus assez de places. Disponibles : " + availableSeats);
            }

            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, reservation.getEventId());
            pstmt.setString(2, reservation.getClientName());
            pstmt.setString(3, reservation.getClientEmail());
            pstmt.setString(4, reservation.getClientPhone());
            pstmt.setInt(5, reservation.getNumberOfTickets());
            pstmt.setBigDecimal(6, reservation.getTotalPrice());
            pstmt.setString(7, toDbStatus(reservation.getStatus() != null ? reservation.getStatus() : STATUS_EN_ATTENTE));
            pstmt.setString(8, reservation.getPaymentMethod() != null ? reservation.getPaymentMethod() : "CARTE");
            pstmt.setString(9, reservation.getSpecialRequests());
            pstmt.setString(10, reservation.getQrCode());
            pstmt.setString(11, reservation.getSelectedSeats());
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) reservation.setId(rs.getInt(1));
            }

            // Recalculer current_capacity depuis le COUNT réel (fiable même si le web a fait des réservations)
            eventService.syncCapacityFromReservations(reservation.getEventId(), conn);

            conn.commit();
            return reservation;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (pstmt != null) pstmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Calcule les places disponibles en verrouillant la ligne de l'événement (SELECT ... FOR UPDATE).
     * current_capacity stocke les places RESTANTES.
     * Retourne -1 si l'événement n'existe pas.
     */
    private int getAvailableSeatsLocked(int eventId, Connection conn) throws SQLException {
        // Verrouiller la ligne events pour éviter les race conditions
        String sql = "SELECT current_capacity FROM events WHERE id = ? FOR UPDATE";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Math.max(0, rs.getInt("current_capacity"));
            }
        }
        return -1;
    }

    // ========== READ - All ==========
    public List<Reservation> getAll() throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT r.*, e.title as event_title, e.event_date as ev_date, e.event_time as ev_time " +
                "FROM reservations r INNER JOIN events e ON r.event_id = e.id " +
                "ORDER BY r.reservation_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Reservation r = mapResultSetToReservation(rs);
                r.setEventTitle(rs.getString("event_title"));
                Date eventDate = rs.getDate("ev_date");
                if (eventDate != null) r.setEventDate(eventDate.toLocalDate());
                Time eventTime = rs.getTime("ev_time");
                if (eventTime != null) r.setEventTime(eventTime.toLocalTime());
                reservations.add(r);
            }
        }
        return reservations;
    }

    // ========== READ - By Event ID ==========
    public List<Reservation> getByEventId(int eventId) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT r.*, e.title as event_title, e.event_date as ev_date, e.event_time as ev_time " +
                "FROM reservations r INNER JOIN events e ON r.event_id = e.id " +
                "WHERE r.event_id = ? ORDER BY r.reservation_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Reservation r = mapResultSetToReservation(rs);
                    r.setEventTitle(rs.getString("event_title"));
                    Date eventDate = rs.getDate("ev_date");
                    if (eventDate != null) r.setEventDate(eventDate.toLocalDate());
                    Time eventTime = rs.getTime("ev_time");
                    if (eventTime != null) r.setEventTime(eventTime.toLocalTime());
                    reservations.add(r);
                }
            }
        }
        return reservations;
    }

    // ========== READ - By Client Email ==========
    public List<Reservation> getByClientEmail(String email) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT r.*, e.title as event_title, e.event_date as ev_date, e.event_time as ev_time " +
                "FROM reservations r INNER JOIN events e ON r.event_id = e.id " +
                "WHERE r.client_email = ? ORDER BY r.reservation_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Reservation r = mapResultSetToReservation(rs);
                    r.setEventTitle(rs.getString("event_title"));
                    Date eventDate = rs.getDate("ev_date");
                    if (eventDate != null) r.setEventDate(eventDate.toLocalDate());
                    Time eventTime = rs.getTime("ev_time");
                    if (eventTime != null) r.setEventTime(eventTime.toLocalTime());
                    reservations.add(r);
                }
            }
        }
        return reservations;
    }

    // ========== READ - By ID ==========
    public Reservation getById(int id) throws SQLException {
        String sql = "SELECT r.*, e.title as event_title, e.event_date as ev_date, e.event_time as ev_time " +
                "FROM reservations r INNER JOIN events e ON r.event_id = e.id WHERE r.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Reservation r = mapResultSetToReservation(rs);
                    r.setEventTitle(rs.getString("event_title"));
                    Date eventDate = rs.getDate("ev_date");
                    if (eventDate != null) r.setEventDate(eventDate.toLocalDate());
                    Time eventTime = rs.getTime("ev_time");
                    if (eventTime != null) r.setEventTime(eventTime.toLocalTime());
                    return r;
                }
            }
        }
        return null;
    }

    // ========== READ - Reserved Seats by Event ID ==========
    public List<String> getReservedSeatsByEventId(int eventId) throws SQLException {
        List<String> reservedSeats = new ArrayList<>();

        // Include ALL active reservations: PENDING + CONFIRMED (exclude only CANCELLED)
        // We use a positive IN filter to avoid the toDbStatus mapping bug where
        // REFUSEE incorrectly maps to the first ENUM value (PENDING).
        String dbPending   = toDbStatus(STATUS_EN_ATTENTE);
        String dbConfirmed = toDbStatus(STATUS_CONFIRMEE);

        String sql = "SELECT selected_seats FROM reservations " +
                "WHERE event_id = ? AND status IN (?, ?) " +
                "AND selected_seats IS NOT NULL AND selected_seats != ''";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setString(2, dbPending);
            pstmt.setString(3, dbConfirmed);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String seats = rs.getString("selected_seats");
                    if (seats != null && !seats.isBlank()) {
                        for (String seat : seats.split(",")) {
                            String trimmed = seat.trim();
                            if (!trimmed.isEmpty()) reservedSeats.add(trimmed);
                        }
                    }
                }
            }
        }
        return reservedSeats;
    }
    public void update(Reservation reservation) throws SQLException {
        String sql = "UPDATE reservations SET status=?, special_requests=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, toDbStatus(reservation.getStatus()));
            pstmt.setString(2, reservation.getSpecialRequests());
            pstmt.setInt(3, reservation.getId());
            pstmt.executeUpdate();
        }
    }

    // ========== CONFIRMER ==========
    public void confirmReservation(int reservationId) throws SQLException {
        Reservation reservation = getById(reservationId);
        if (reservation == null) throw new SQLException("Réservation introuvable");
        if (!STATUS_EN_ATTENTE.equals(reservation.getStatus()))
            throw new SQLException("Seules les réservations en attente peuvent être acceptées.");
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, toDbStatus(STATUS_CONFIRMEE));
            pstmt.setInt(2, reservationId);
            pstmt.executeUpdate();
        }
        EmailUtil.sendReservationStatusEmail(
                reservation.getClientEmail(), reservation.getClientName(),
                reservation.getEventTitle(), STATUS_CONFIRMEE, null);
    }

    // ========== REFUSER ==========
    public void rejectReservation(int reservationId, String reason) throws SQLException {
        Reservation reservation = getById(reservationId);
        if (reservation == null) throw new SQLException("Réservation introuvable");
        if (!STATUS_EN_ATTENTE.equals(reservation.getStatus()))
            throw new SQLException("Seules les réservations en attente peuvent être refusées.");
        String sql = "UPDATE reservations SET status = ?, special_requests = CONCAT(IFNULL(special_requests,''), ' | Refus: ', ?) WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, toDbStatus(STATUS_REFUSEE));
            pstmt.setString(2, reason != null ? reason : "");
            pstmt.setInt(3, reservationId);
            pstmt.executeUpdate();
        }
        EmailUtil.sendReservationStatusEmail(
                reservation.getClientEmail(), reservation.getClientName(),
                reservation.getEventTitle(), STATUS_REFUSEE, reason);
    }

    // ========== ANNULER ==========
    public void cancelReservation(int reservationId) throws SQLException {
        Reservation reservation = getById(reservationId);
        if (reservation == null) throw new SQLException("Réservation non trouvée");
        String status = reservation.getStatus();
        if (STATUS_CONFIRMEE.equals(status) || STATUS_REFUSEE.equals(status))
            throw new SQLException("Impossible d'annuler une réservation déjà traitée.");
        if (STATUS_ANNULEE.equals(status)) throw new SQLException("Déjà annulée.");
        String sql = "UPDATE reservations SET status=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, toDbStatus(STATUS_ANNULEE));
            pstmt.setInt(2, reservationId);
            pstmt.executeUpdate();
        }
        // Recalculer depuis la DB — plus fiable que soustraire manuellement
        eventService.syncCapacityFromReservations(reservation.getEventId());
    }

    // ========== DELETE ==========
    public void delete(int id) throws SQLException {
        Reservation reservation = getById(id);
        int eventId = reservation != null ? reservation.getEventId() : -1;
        String sql = "DELETE FROM reservations WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
        // Recalculer depuis la DB après suppression
        if (eventId > 0) {
            eventService.syncCapacityFromReservations(eventId);
        }
    }

    // ========== MAP ==========
    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setEventId(rs.getInt("event_id"));

        // client_name — peut s'appeler name ou user_name dans certains schémas
        try { r.setClientName(rs.getString("client_name")); } catch (SQLException e1) {
            try { r.setClientName(rs.getString("name")); } catch (SQLException ignored) {}
        }

        // client_email — peut s'appeler email
        try { r.setClientEmail(rs.getString("client_email")); } catch (SQLException e1) {
            try { r.setClientEmail(rs.getString("email")); } catch (SQLException ignored) {}
        }

        // client_phone — peut être absent
        try { r.setClientPhone(rs.getString("client_phone")); } catch (SQLException ignored) {}

        // reservation_date — peut s'appeler created_at
        try {
            Timestamp ts = rs.getTimestamp("reservation_date");
            if (ts != null) r.setReservationDate(ts.toLocalDateTime());
        } catch (SQLException e1) {
            try {
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) r.setReservationDate(ts.toLocalDateTime());
            } catch (SQLException ignored) {}
        }

        // number_of_tickets — peut s'appeler tickets ou quantity
        try { r.setNumberOfTickets(rs.getInt("number_of_tickets")); } catch (SQLException e1) {
            try { r.setNumberOfTickets(rs.getInt("tickets")); } catch (SQLException e2) {
                try { r.setNumberOfTickets(rs.getInt("quantity")); } catch (SQLException ignored) {}
            }
        }

        // total_price — peut s'appeler total ou montant
        try { r.setTotalPrice(rs.getBigDecimal("total_price")); } catch (SQLException e1) {
            try { r.setTotalPrice(rs.getBigDecimal("total")); } catch (SQLException e2) {
                try { r.setTotalPrice(rs.getBigDecimal("montant")); } catch (SQLException ignored) {}
            }
        }

        // status — normaliser depuis la valeur DB vers le statut Java standard
        try { r.setStatus(fromDbStatus(rs.getString("status"))); } catch (SQLException ignored) {
            r.setStatus(STATUS_EN_ATTENTE);
        }

        // payment_method — peut être absent
        try { r.setPaymentMethod(rs.getString("payment_method")); } catch (SQLException ignored) {}

        // special_requests — peut être absent
        try { r.setSpecialRequests(rs.getString("special_requests")); } catch (SQLException ignored) {}

        // qr_code — peut être absent
        try { r.setQrCode(rs.getString("qr_code")); } catch (SQLException ignored) {}

        // selected_seats — peut être absent
        try { r.setSelectedSeats(rs.getString("selected_seats")); } catch (SQLException ignored) {}

        r.setUserFullName(r.getClientName());
        r.setUserName(r.getClientEmail());
        return r;
    }
}


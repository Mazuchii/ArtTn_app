package tn.esprit.museum.services;

import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Reservation;
import tn.esprit.museum.utils.DatabaseConnection;
import tn.esprit.museum.utils.EmailUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {

    private EventService eventService = new EventService();

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

            Event event = eventService.getById(reservation.getEventId());
            if (event == null) throw new SQLException("Événement non trouvé");

            int availableSeats = event.getMaxCapacity() - event.getCurrentCapacity();
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
            pstmt.setString(7, reservation.getStatus() != null ? reservation.getStatus() : "EN_ATTENTE");
            pstmt.setString(8, reservation.getPaymentMethod() != null ? reservation.getPaymentMethod() : "CARTE");
            pstmt.setString(9, reservation.getSpecialRequests());
            pstmt.setString(10, reservation.getQrCode());
            pstmt.setString(11, reservation.getSelectedSeats());

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) reservation.setId(rs.getInt(1));
            }

            int newCapacity = event.getCurrentCapacity() + reservation.getNumberOfTickets();
            eventService.updateCapacity(reservation.getEventId(), newCapacity, conn);

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

    // ========== READ - All ==========
    public List<Reservation> getAll() throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT r.*, e.title as event_title, e.event_date as event_date " +
                "FROM reservations r " +
                "INNER JOIN events e ON r.event_id = e.id " +
                "ORDER BY r.reservation_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Reservation r = mapResultSetToReservation(rs);
                r.setEventTitle(rs.getString("event_title"));
                Date eventDate = rs.getDate("event_date");
                if (eventDate != null) r.setEventDate(eventDate.toLocalDate());
                reservations.add(r);
            }
        }
        return reservations;
    }

    // ========== READ - By Event ID ==========
    public List<Reservation> getByEventId(int eventId) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT r.*, e.title as event_title, e.event_date as event_date " +
                "FROM reservations r " +
                "INNER JOIN events e ON r.event_id = e.id " +
                "WHERE r.event_id = ? " +
                "ORDER BY r.reservation_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Reservation r = mapResultSetToReservation(rs);
                    r.setEventTitle(rs.getString("event_title"));
                    Date eventDate = rs.getDate("event_date");
                    if (eventDate != null) r.setEventDate(eventDate.toLocalDate());
                    reservations.add(r);
                }
            }
        }
        return reservations;
    }

    // ========== READ - By Client Email ==========
    public List<Reservation> getByClientEmail(String email) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT r.*, e.title as event_title, e.event_date as event_date " +
                "FROM reservations r " +
                "INNER JOIN events e ON r.event_id = e.id " +
                "WHERE r.client_email = ? " +
                "ORDER BY r.reservation_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Reservation r = mapResultSetToReservation(rs);
                    r.setEventTitle(rs.getString("event_title"));
                    Date eventDate = rs.getDate("event_date");
                    if (eventDate != null) r.setEventDate(eventDate.toLocalDate());
                    reservations.add(r);
                }
            }
        }
        return reservations;
    }

    // ========== READ - By ID ==========
    public Reservation getById(int id) throws SQLException {
        String sql = "SELECT r.*, e.title as event_title, e.event_date as event_date " +
                "FROM reservations r " +
                "INNER JOIN events e ON r.event_id = e.id " +
                "WHERE r.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Reservation r = mapResultSetToReservation(rs);
                    r.setEventTitle(rs.getString("event_title"));
                    Date eventDate = rs.getDate("event_date");
                    if (eventDate != null) r.setEventDate(eventDate.toLocalDate());
                    return r;
                }
            }
        }
        return null;
    }

    // ========== UPDATE ==========
    public void update(Reservation reservation) throws SQLException {
        String sql = "UPDATE reservations SET status=?, special_requests=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, reservation.getStatus());
            pstmt.setString(2, reservation.getSpecialRequests());
            pstmt.setInt(3, reservation.getId());
            pstmt.executeUpdate();
        }
    }

    // ========== CONFIRMER (ACCEPTER) ==========
    public void confirmReservation(int reservationId) throws SQLException {
        Reservation reservation = getById(reservationId);
        if (reservation == null) throw new SQLException("Réservation introuvable");
        if (!"EN_ATTENTE".equals(reservation.getStatus())) {
            throw new SQLException("Seules les réservations en attente peuvent être acceptées.");
        }
        String sql = "UPDATE reservations SET status = 'CONFIRMEE' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reservationId);
            pstmt.executeUpdate();
        }
        EmailUtil.sendReservationStatusEmail(
                reservation.getClientEmail(),
                reservation.getClientName(),
                reservation.getEventTitle(),
                "CONFIRMEE",
                null
        );
    }

    // ========== REFUSER (REJETER) ==========
    public void rejectReservation(int reservationId, String reason) throws SQLException {
        Reservation reservation = getById(reservationId);
        if (reservation == null) throw new SQLException("Réservation introuvable");
        if (!"EN_ATTENTE".equals(reservation.getStatus())) {
            throw new SQLException("Seules les réservations en attente peuvent être refusées.");
        }
        String sql = "UPDATE reservations SET status = 'REFUSEE', special_requests = CONCAT(IFNULL(special_requests,''), ' | Refus: ', ?) WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, reason != null ? reason : "");
            pstmt.setInt(2, reservationId);
            pstmt.executeUpdate();
        }
        EmailUtil.sendReservationStatusEmail(
                reservation.getClientEmail(),
                reservation.getClientName(),
                reservation.getEventTitle(),
                "REFUSEE",
                reason
        );
    }

    // ========== ANNULER (avec vérification) ==========
    public void cancelReservation(int reservationId) throws SQLException {
        Reservation reservation = getById(reservationId);
        if (reservation == null) throw new SQLException("Réservation non trouvée");
        String status = reservation.getStatus();
        if ("CONFIRMEE".equals(status) || "REFUSEE".equals(status)) {
            throw new SQLException("Impossible d'annuler une réservation déjà acceptée ou refusée.");
        }
        if ("ANNULEE".equals(status)) {
            throw new SQLException("Déjà annulée.");
        }
        String sql = "UPDATE reservations SET status='ANNULEE' WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reservationId);
            pstmt.executeUpdate();
        }
        Event event = eventService.getById(reservation.getEventId());
        if (event != null) {
            int newCap = event.getCurrentCapacity() - reservation.getNumberOfTickets();
            if (newCap >= 0) eventService.updateCapacity(reservation.getEventId(), newCap);
        }
    }

    // ========== DELETE ==========
    public void delete(int id) throws SQLException {
        Reservation reservation = getById(id);
        if (reservation != null && !"ANNULEE".equals(reservation.getStatus())) {
            Event event = eventService.getById(reservation.getEventId());
            if (event != null) {
                int newCap = event.getCurrentCapacity() - reservation.getNumberOfTickets();
                if (newCap >= 0) eventService.updateCapacity(reservation.getEventId(), newCap);
            }
        }
        String sql = "DELETE FROM reservations WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // ========== MAP ==========
    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setEventId(rs.getInt("event_id"));
        r.setClientName(rs.getString("client_name"));
        r.setClientEmail(rs.getString("client_email"));
        r.setClientPhone(rs.getString("client_phone"));
        Timestamp ts = rs.getTimestamp("reservation_date");
        if (ts != null) r.setReservationDate(ts.toLocalDateTime());
        r.setNumberOfTickets(rs.getInt("number_of_tickets"));
        r.setTotalPrice(rs.getBigDecimal("total_price"));
        r.setStatus(rs.getString("status"));
        r.setPaymentMethod(rs.getString("payment_method"));
        r.setSpecialRequests(rs.getString("special_requests"));
        r.setQrCode(rs.getString("qr_code"));
        r.setSelectedSeats(rs.getString("selected_seats"));
        // Pour l'affichage
        r.setUserFullName(rs.getString("client_name"));
        r.setUserName(rs.getString("client_email"));
        return r;
    }
}
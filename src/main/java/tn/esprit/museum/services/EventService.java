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
        String sql = "SELECT COUNT(*) FROM events WHERE title = ?";
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
        String sql = "SELECT COUNT(*) FROM events WHERE event_date = ? AND event_time = ?";
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
        String sql = "SELECT COUNT(*) FROM events WHERE title = ? AND event_date = ? AND event_time = ? AND location = ?";
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
        String sql = "INSERT INTO events (title, description, event_date, event_time, location, " +
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
            pstmt.setInt(7, event.getCurrentCapacity());
            pstmt.setBigDecimal(8, event.getPrice());
            pstmt.setString(9, event.getCategory());
            pstmt.setString(10, event.getStatus());
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
        String sql = "SELECT * FROM events ORDER BY event_date ASC, event_time ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) events.add(mapResultSetToEvent(rs));
        }
        return events;
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
        String sql = "SELECT * FROM events WHERE id = ?";
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
        String sql = "SELECT * FROM events WHERE category = ? ORDER BY event_date ASC";
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
        String sql = "SELECT * FROM events WHERE event_date >= CURDATE() AND status = 'A_VENIR' ORDER BY event_date ASC, event_time ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) events.add(mapResultSetToEvent(rs));
        }
        return events;
    }

    public int getUpcomingEventsCount() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM events WHERE event_date >= CURDATE() AND status = 'A_VENIR'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("total");
        }
        return 0;
    }

    public List<Event> search(String keyword) throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM events WHERE title LIKE ? OR description LIKE ? OR location LIKE ?";
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
        String sql = "UPDATE events SET title=?, description=?, event_date=?, event_time=?, " +
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
            pstmt.setString(9, event.getStatus());
            pstmt.setString(10, event.getImageUrl());
            pstmt.setInt(11, event.getId());
            pstmt.executeUpdate();
        }
    }

    public void updateCapacity(int eventId, int newCurrentCapacity) throws SQLException {
        String sql = "UPDATE events SET current_capacity = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newCurrentCapacity);
            pstmt.setInt(2, eventId);
            pstmt.executeUpdate();
        }
    }

    /** Version transactionnelle — utilise une connexion existante */
    public void updateCapacity(int eventId, int newCurrentCapacity, Connection conn) throws SQLException {
        String sql = "UPDATE events SET current_capacity = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newCurrentCapacity);
            pstmt.setInt(2, eventId);
            pstmt.executeUpdate();
        }
    }

    public void updateStatus(int eventId, String status) throws SQLException {
        String sql = "UPDATE events SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, eventId);
            pstmt.executeUpdate();
        }
    }

    // ==================== DELETE ====================
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM events WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // ==================== STATISTIQUES ====================
    public int getTotalEvents() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM events";
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
        event.setId(rs.getInt("id"));
        event.setTitle(rs.getString("title"));
        event.setDescription(rs.getString("description"));
        Date date = rs.getDate("event_date");
        if (date != null) event.setEventDate(date.toLocalDate());
        Time time = rs.getTime("event_time");
        if (time != null) event.setEventTime(time.toLocalTime());
        event.setLocation(rs.getString("location"));
        event.setMaxCapacity(rs.getInt("max_capacity"));
        event.setCurrentCapacity(rs.getInt("current_capacity"));
        event.setPrice(rs.getBigDecimal("price"));
        event.setCategory(rs.getString("category"));
        event.setStatus(rs.getString("status"));
        event.setImageUrl(rs.getString("image_url"));
        return event;
    }
}


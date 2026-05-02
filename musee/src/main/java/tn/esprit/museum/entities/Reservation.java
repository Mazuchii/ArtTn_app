package tn.esprit.museum.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Reservation {

    private int id;
    private int eventId;
    private int userId;
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private LocalDateTime reservationDate;
    private int numberOfTickets;
    private BigDecimal totalPrice;
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private String specialRequests;
    private String qrCode;
    private String selectedSeats;

    // Champs pour les jointures
    private String userName;
    private String userFullName;
    private String eventTitle;
    private LocalDate eventDate;      // ⭐ Date de l'événement (non stockée, issue de la jointure)

    // ========== CONSTRUCTEURS ==========
    public Reservation() {}

    public Reservation(int eventId, int userId, int numberOfTickets, BigDecimal totalPrice) {
        this.eventId = eventId;
        this.userId = userId;
        this.numberOfTickets = numberOfTickets;
        this.totalPrice = totalPrice;
        this.reservationDate = LocalDateTime.now();
        this.status = "EN_ATTENTE";
        this.paymentStatus = "EN_ATTENTE";
    }

    public Reservation(int eventId, String clientName, String clientEmail, String clientPhone,
                       int numberOfTickets, BigDecimal totalPrice) {
        this.eventId = eventId;
        this.clientName = clientName;
        this.clientEmail = clientEmail;
        this.clientPhone = clientPhone;
        this.numberOfTickets = numberOfTickets;
        this.totalPrice = totalPrice;
        this.reservationDate = LocalDateTime.now();
        this.status = "EN_ATTENTE";
        this.paymentStatus = "EN_ATTENTE";
    }

    // ========== GETTERS / SETTERS ==========
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public String getClientPhone() { return clientPhone; }
    public void setClientPhone(String clientPhone) { this.clientPhone = clientPhone; }

    public LocalDateTime getReservationDate() { return reservationDate; }
    public void setReservationDate(LocalDateTime reservationDate) { this.reservationDate = reservationDate; }

    public int getNumberOfTickets() { return numberOfTickets; }
    public void setNumberOfTickets(int numberOfTickets) { this.numberOfTickets = numberOfTickets; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public String getSelectedSeats() { return selectedSeats; }
    public void setSelectedSeats(String selectedSeats) { this.selectedSeats = selectedSeats; }

    // Jointures
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    // ========== UTILITAIRES ==========
    public String getFormattedDate() {
        if (reservationDate == null) return "";
        return reservationDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public String getFormattedTotal() {
        return totalPrice != null ? totalPrice + " DT" : "0 DT";
    }

    @Override
    public String toString() {
        return "Reservation{id=" + id + ", eventId=" + eventId + ", status=" + status + '}';
    }
}
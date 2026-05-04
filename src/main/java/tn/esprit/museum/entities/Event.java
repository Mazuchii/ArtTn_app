package tn.esprit.museum.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class Event {
    private int id;
    private String title;
    private String description;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String location;
    private int maxCapacity;
    private int currentCapacity;
    private BigDecimal price;
    private String imageUrl;
    private String category;
    private String status;

    public static final String CATEGORY_EXPOSITION    = "EXPOSITION";
    public static final String CATEGORY_CONFERENCE    = "CONFERENCE";
    public static final String CATEGORY_ATELIER       = "ATELIER";
    public static final String CATEGORY_VISITE_GUIDEE = "VISITE_GUIDEE";
    public static final String CATEGORY_SPECTACLE     = "SPECTACLE";

    public static final String STATUS_A_VENIR  = "A_VENIR";
    public static final String STATUS_EN_COURS = "EN_COURS";
    public static final String STATUS_TERMINE  = "TERMINE";
    public static final String STATUS_ANNULE   = "ANNULE";

    public Event() {}

    public Event(String title, String description, LocalDate eventDate, LocalTime eventTime,
                 String location, int maxCapacity, BigDecimal price, String category, String imageUrl) {
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.location = location;
        this.maxCapacity = maxCapacity;
        this.currentCapacity = 0;
        this.price = price;
        this.category = category;
        this.status = STATUS_A_VENIR;
        this.imageUrl = imageUrl;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getEventDate() { return eventDate; }
    public LocalTime getEventTime() { return eventTime; }
    public String getLocation() { return location; }
    public int getMaxCapacity() { return maxCapacity; }
    public int getCurrentCapacity() { return currentCapacity; }
    public BigDecimal getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }

    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public void setEventTime(LocalTime eventTime) { this.eventTime = eventTime; }
    public void setLocation(String location) { this.location = location; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
    public void setCurrentCapacity(int currentCapacity) { this.currentCapacity = currentCapacity; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setCategory(String category) { this.category = category; }
    public void setStatus(String status) { this.status = status; }

    public int getAvailableSeats() { return maxCapacity - currentCapacity; }
    public boolean isAvailable() { return STATUS_A_VENIR.equals(status) && getAvailableSeats() > 0; }

    @Override
    public String toString() { return title + " - " + eventDate; }
}


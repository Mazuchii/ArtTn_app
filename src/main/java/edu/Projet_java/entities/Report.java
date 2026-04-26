package edu.Projet_java.entities;

import java.sql.Timestamp;

public class Report {
    private int id;
    private int postId;
    private int userId;
    private String reason;
    private String details;
    private String status;
    private Timestamp createdAt;

    public Report() {}

    public Report(int postId, int userId, String reason, String details) {
        this.postId = postId;
        this.userId = userId;
        this.reason = reason;
        this.details = details;
        this.status = "PENDING";
    }

    // Getters
    public int getId() { return id; }
    public int getPostId() { return postId; }
    public int getUserId() { return userId; }
    public String getReason() { return reason; }
    public String getDetails() { return details; }
    public String getStatus() { return status; }
    public Timestamp getCreatedAt() { return createdAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setPostId(int postId) { this.postId = postId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setReason(String reason) { this.reason = reason; }
    public void setDetails(String details) { this.details = details; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
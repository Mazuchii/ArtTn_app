package tn.esprit.museum.entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Posts {
    private int post_id;
    private String post_titre;
    private String post_contenu;
    private int author_id;
    private LocalDateTime date_creation;

    // ✅ NOUVEAUX CHAMPS
    private int categoryId;
    private int views;
    private int likes;
    private int dislikes;
    private String imageUrl;
    private int reportCount;


    public Posts() {}

    public Posts(String post_titre, String post_contenu, int author_id) {
        this.post_titre = post_titre;
        this.post_contenu = post_contenu;
        this.author_id = author_id;
        this.date_creation = LocalDateTime.now();
        this.views = 0;
        this.likes = 0;
        this.dislikes = 0;
    }

    // === GETTERS ===
    public int getPost_id() { return post_id; }
    public String getPost_titre() { return post_titre; }
    public String getPost_contenu() { return post_contenu; }
    public int getAuthor_id() { return author_id; }
    public LocalDateTime getDate_creation() { return date_creation; }

    // ✅ NOUVEAUX GETTERS
    public int getCategoryId() { return categoryId; }
    public int getViews() { return views; }
    public int getLikes() { return likes; }
    public int getDislikes() { return dislikes; }
    public String getImageUrl() { return imageUrl; }
    public int getReportCount() { return reportCount; }

    // === SETTERS ===
    public void setPost_id(int post_id) { this.post_id = post_id; }
    public void setPost_titre(String post_titre) { this.post_titre = post_titre; }
    public void setPost_contenu(String post_contenu) { this.post_contenu = post_contenu; }
    public void setAuthor_id(int author_id) { this.author_id = author_id; }
    public void setDate_creation(LocalDateTime date_creation) { this.date_creation = date_creation; }

    // ✅ NOUVEAUX SETTERS
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public void setViews(int views) { this.views = views; }
    public void setLikes(int likes) { this.likes = likes; }
    public void setDislikes(int dislikes) { this.dislikes = dislikes; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }


    public String getFormattedDate() {
        if (date_creation == null) return "Date inconnue";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return date_creation.format(formatter);
    }

    @Override
    public String toString() {
        return "Posts{" +
                "post_id=" + post_id +
                ", post_titre='" + post_titre + '\'' +
                ", author_id=" + author_id +
                ", date_creation=" + date_creation +
                ", views=" + views +
                ", likes=" + likes +
                '}';
    }
}

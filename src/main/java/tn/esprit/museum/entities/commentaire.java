package tn.esprit.museum.entities;

import java.util.Objects;

public class commentaire {
    private int comment_id;
    private int post_id;
    private String contenu;
    private int author_id;
    private String contenuTraduit;


    public commentaire() {}

    public commentaire(int post_id, String contenu, int author_id) {
        this.post_id = post_id;
        this.contenu = contenu;
        this.author_id = author_id;
    }

    // Getters and Setters
    public int getComment_id() { return comment_id; }
    public void setComment_id(int comment_id) { this.comment_id = comment_id; }
    public int getPost_id() { return post_id; }
    public void setPost_id(int post_id) { this.post_id = post_id; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public int getAuthor_id() { return author_id; }
    public void setAuthor_id(int author_id) { this.author_id = author_id; }
    public String getContenuTraduit() { return contenuTraduit; }
    public void setContenuTraduit(String contenuTraduit) { this.contenuTraduit = contenuTraduit; }


    @Override
    public String toString() {
        return "commentaire{" + "id=" + comment_id + ", post=" + post_id + ", auteur=" + author_id + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        commentaire comment = (commentaire) o;
        return comment_id == comment.comment_id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(comment_id);
    }
}
package tn.esprit.museum.entities;

import java.util.Date;
import java.util.Objects;

public class Sponsor {
    private int id;
    private String nom;
    private String description;
    private String type;
    private String logoUrl;
    private String email;
    private String telephone;
    private String siteWeb;
    private String adresse;
    private Date dateCreation;
    private String status;
    private Integer userId;
    private Integer packId;

    public Sponsor() {
    }

    public Sponsor(String nom, String description, String type, String logoUrl,
                   String email, String telephone, String siteWeb,
                   String adresse, Date dateCreation, String status) {
        this.nom = nom;
        this.description = description;
        this.type = type;
        this.logoUrl = logoUrl;
        this.email = email;
        this.telephone = telephone;
        this.siteWeb = siteWeb;
        this.adresse = adresse;
        this.dateCreation = dateCreation;
        this.status = status;
    }

    public Sponsor(int id, String nom, String description, String type, String logoUrl,
                   String email, String telephone, String siteWeb,
                   String adresse, Date dateCreation, String status) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.type = type;
        this.logoUrl = logoUrl;
        this.email = email;
        this.telephone = telephone;
        this.siteWeb = siteWeb;
        this.adresse = adresse;
        this.dateCreation = dateCreation;
        this.status = status;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getSiteWeb() {
        return siteWeb;
    }

    public void setSiteWeb(String siteWeb) {
        this.siteWeb = siteWeb;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public Date getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Date dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getPackId() {
        return packId;
    }

    public void setPackId(Integer packId) {
        this.packId = packId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sponsor)) return false;
        Sponsor sponsor = (Sponsor) o;
        return id == sponsor.id &&
                Objects.equals(nom, sponsor.nom) &&
                Objects.equals(description, sponsor.description) &&
                Objects.equals(type, sponsor.type) &&
                Objects.equals(logoUrl, sponsor.logoUrl) &&
                Objects.equals(email, sponsor.email) &&
                Objects.equals(telephone, sponsor.telephone) &&
                Objects.equals(siteWeb, sponsor.siteWeb) &&
                Objects.equals(adresse, sponsor.adresse) &&
                Objects.equals(dateCreation, sponsor.dateCreation) &&
                Objects.equals(status, sponsor.status) &&
                Objects.equals(userId, sponsor.userId) &&
                Objects.equals(packId, sponsor.packId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nom, description, type, logoUrl, email,
                telephone, siteWeb, adresse, dateCreation, status, userId, packId);
    }


    @Override
    public String toString() {
        return "Sponsor{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", logoUrl='" + logoUrl + '\'' +
                ", email='" + email + '\'' +
                ", telephone='" + telephone + '\'' +
                ", siteWeb='" + siteWeb + '\'' +
                ", adresse='" + adresse + '\'' +
                ", dateCreation=" + dateCreation +
                ", status='" + status + '\'' +
                ", userId=" + userId +
                ", packId=" + packId +
                '}';
    }
}


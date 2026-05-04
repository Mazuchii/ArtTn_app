package tn.esprit.museum.entities;

import java.util.Date;

public class UserDonation {
    private final int sponsorId;
    private final String sponsorName;
    private final String eventTitle;
    private final String packName;
    private final double amount;
    private final String status;
    private final Date dateCreation;

    public UserDonation(int sponsorId, String sponsorName, String eventTitle, String packName,
                        double amount, String status, Date dateCreation) {
        this.sponsorId = sponsorId;
        this.sponsorName = sponsorName;
        this.eventTitle = eventTitle;
        this.packName = packName;
        this.amount = amount;
        this.status = status;
        this.dateCreation = dateCreation;
    }

    public int getSponsorId() {
        return sponsorId;
    }

    public String getSponsorName() {
        return sponsorName;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public String getPackName() {
        return packName;
    }

    public double getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public Date getDateCreation() {
        return dateCreation;
    }
}

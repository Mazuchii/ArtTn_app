package tn.esprit.museum.controllers;

import tn.esprit.museum.entities.OfferJob;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class JobCardController {
    @FXML private Label lblTitre, lblDescription, lblSalaire;
    @FXML private Button btnPostuler;
    public void setOfferData(OfferJob offer, Runnable onAction) {
        lblTitre.setText(offer.getTitre());
        lblDescription.setText(offer.getDescription());
        lblSalaire.setText(String.valueOf(offer.getSalaire()));

        btnPostuler.setOnAction(event -> onAction.run());
    }
}
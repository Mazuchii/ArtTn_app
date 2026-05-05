package tn.esprit.museum.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.museum.utils.WindowManager;

import java.io.IOException;

public class SponsorNavigationController {

    @FXML
    private void navigateToDonations(ActionEvent event) {
        loadPage(event, "/fxml/SponsorList.fxml");
    }

    @FXML
    private void navigateToPacks(ActionEvent event) {
        loadPage(event, "/fxml/PackCatalogue.fxml");
    }

    @FXML
    private void navigateToEvents(ActionEvent event) {
        loadPage(event, "/fxml/events_front.fxml");
    }

    private void loadPage(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            WindowManager.applyStandardSize(stage);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

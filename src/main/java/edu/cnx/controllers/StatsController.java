package edu.cnx.controllers;

import edu.cnx.entités.OfferJob;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import java.util.List;
import java.util.Map;

public class StatsController {

    @FXML private BarChart<String, Number> barChart;
    @FXML private PieChart pieChart;

    public void setOfferData(List<OfferJob> offers) {
        if (barChart != null) {
            barChart.setVisible(true);
            if (pieChart != null) pieChart.setVisible(false);

            barChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Salaire par Offre (DT)");

            for (OfferJob offer : offers) {
                series.getData().add(new XYChart.Data<>(offer.getTitre(), offer.getSalaire()));
            }
            barChart.getData().add(series);
        }
    }

    public void setDemandeData(Map<String, Long> stats) {
        if (pieChart != null) {
            pieChart.setVisible(true);
            if (barChart != null) barChart.setVisible(false);

            pieChart.getData().clear();
            stats.forEach((statut, nombre) -> {
                pieChart.getData().add(new PieChart.Data(statut + " (" + nombre + ")", nombre));
            });
        }
    }
}
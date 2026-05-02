package tn.esprit.museum.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import tn.esprit.museum.entities.Event;
import tn.esprit.museum.services.EventService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatbotController {

    private static final Logger LOGGER = Logger.getLogger(ChatbotController.class.getName());

    @FXML private ListView<String> chatListView;
    @FXML private TextField messageField;

    private EventService eventService;
    private ObservableList<String> messages;

    @FXML
    public void initialize() {
        eventService = new EventService();
        messages = FXCollections.observableArrayList();
        chatListView.setItems(messages);

        // Message d'accueil
        addBotMessage("Bonjour ! 👋\nJe suis l'assistant de Museum Digital.\n\nPosez-moi une question sur les événements :\n- Par titre (ex: \"Atelier de Céramique\")\n- Par catégorie (Exposition, Conférence, Atelier, Visite Guidée, Spectacle)\n- Par lieu (Tunis, Ariana, etc.)\n- Par date (jj/mm/aaaa)\n\nExemples :\n• \"Je veux un spectacle\"\n• \"Conférence à Tunis\"\n• \"Le 26/04/2026\"\n• \"Atelier de poterie\"");

        // Envoyer avec la touche Entrée
        messageField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
        });
    }

    @FXML
    private void sendMessage() {
        String userMessage = messageField.getText().trim();
        if (userMessage.isEmpty()) return;

        addUserMessage(userMessage);
        messageField.clear();

        new Thread(() -> {
            String reply = processUserMessage(userMessage);
            Platform.runLater(() -> addBotMessage(reply));
        }).start();
    }

    private String processUserMessage(String userMessage) {
        try {
            List<Event> events = eventService.getAll();
            String lowerMsg = userMessage.toLowerCase();
            StringBuilder reply = new StringBuilder();

            for (Event e : events) {
                // Vérifier si le message contient le titre, la catégorie, le lieu ou la date
                boolean matchTitle = e.getTitle().toLowerCase().contains(lowerMsg);
                boolean matchCategory = (e.getCategory() != null && lowerMsg.contains(e.getCategory().toLowerCase()));
                boolean matchLocation = (e.getLocation() != null && lowerMsg.contains(e.getLocation().toLowerCase()));
                String dateStr = e.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                boolean matchDate = lowerMsg.contains(dateStr);

                if (matchTitle || matchCategory || matchLocation || matchDate) {
                    reply.append("• ").append(e.getTitle())
                            .append(" (").append(e.getCategory()).append(")")
                            .append(" le ").append(dateStr)
                            .append(" à ").append(e.getLocation())
                            .append(" – ").append(e.getAvailableSeats()).append(" places\n");
                }
            }

            if (reply.length() == 0) {
                return "❌ Désolé, je n'ai rien trouvé pour \"" + userMessage + "\".\nEssayez :\n- Une catégorie (Exposition, Conférence, Atelier, Visite Guidée, Spectacle)\n- Un lieu (ex: Tunis)\n- Une date (jj/mm/aaaa)";
            } else {
                return "✨ Voici ce que j'ai trouvé :\n\n" + reply.toString();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur base de données", e);
            return "⚠️ Erreur technique, veuillez réessayer plus tard.";
        }
    }

    private void addUserMessage(String msg) {
        messages.add("🗣️ Vous : " + msg);
        scrollToBottom();
    }

    private void addBotMessage(String msg) {
        messages.add("🤖 Assistant : " + msg);
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatListView.scrollTo(messages.size() - 1));
    }

    @FXML
    private void closeChat() {
        Stage stage = (Stage) chatListView.getScene().getWindow();
        stage.close();
    }
}
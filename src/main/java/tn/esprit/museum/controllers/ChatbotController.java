package tn.esprit.museum.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import tn.esprit.museum.services.ChatbotService;

public class ChatbotController {

    @FXML private ListView<String> chatListView;
    @FXML private TextField messageField;

    private ChatbotService chatbotService;
    private ObservableList<String> messages;

    @FXML
    public void initialize() {
        chatbotService = new ChatbotService();
        messages = FXCollections.observableArrayList();
        chatListView.setItems(messages);

        addBotMessage("Bonjour ! 👋\nJe suis l'assistant de Museum Digital.\n\n" +
                "Posez-moi une question sur les événements :\n" +
                "- Par catégorie (Exposition, Conférence, Atelier, Visite Guidée, Spectacle)\n" +
                "- Par lieu (Tunis, Ariana, etc.)\n" +
                "- Par date (jj/mm/aaaa)\n" +
                "- Par titre d'événement\n\n" +
                "Exemples :\n• \"Je veux un spectacle\"\n• \"Conférence à Tunis\"\n• \"Le 26/04/2026\"");

        messageField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) sendMessage();
        });
    }

    @FXML
    private void sendMessage() {
        String userMessage = messageField.getText().trim();
        if (userMessage.isEmpty()) return;
        addUserMessage(userMessage);
        messageField.clear();

        new Thread(() -> {
            String reply = chatbotService.ask(userMessage);
            Platform.runLater(() -> addBotMessage(reply));
        }).start();
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

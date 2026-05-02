package tn.esprit.museum.utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailUtil {

    // ⚙️ À MODIFIER AVEC VOS IDENTIFIANTS SMTP (voir tutoriel à la fin)
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String USERNAME = "guellouznour1234@gmail.com";
    private static final String PASSWORD = "deld dcxh tcnc hdof"; // mot de passe d'application Gmail

    public static void sendReservationStatusEmail(String toEmail, String clientName, String eventTitle, String status, String comment) {
        String subject = "Réservation " + status.toLowerCase() + " - Museum Digital";
        String body = buildEmailContent(clientName, eventTitle, status, comment);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println("Email envoyé à " + toEmail);
        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Erreur envoi email : " + e.getMessage());
        }
    }

    private static String buildEmailContent(String clientName, String eventTitle, String status, String comment) {
        if ("CONFIRMEE".equals(status)) {
            return "Bonjour " + clientName + ",\n\n"
                    + "Votre réservation pour l'événement \"" + eventTitle + "\" a été **ACCEPTÉE**.\n\n"
                    + "Vous pouvez consulter votre billet dans l'application.\n"
                    + "Merci de votre confiance.\n\n"
                    + "L'équipe Museum Digital";
        } else if ("REFUSEE".equals(status)) {
            return "Bonjour " + clientName + ",\n\n"
                    + "Votre réservation pour l'événement \"" + eventTitle + "\" a été **REFUSÉE**.\n"
                    + "Motif : " + (comment != null && !comment.isEmpty() ? comment : "Non spécifié") + "\n\n"
                    + "N'hésitez pas à nous contacter.\n\n"
                    + "L'équipe Museum Digital";
        } else {
            return "Bonjour " + clientName + ",\n\nLe statut de votre réservation a été mis à jour : " + status;
        }
    }
}
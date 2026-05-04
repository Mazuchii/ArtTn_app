package tn.esprit.museum.services;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailService {

    // IMPORTANT: Remplacez ces valeurs par vos vrais identifiants
    private static final String SENDER_EMAIL = "ahmed.atrousrm@gmail.com";
    private static final String SENDER_PASSWORD = "lqlz ytdu adeb pgwd";
    
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void sendStatusEmail(String toEmail, String sponsorName, String status) {
        if (toEmail == null || toEmail.isBlank()) {
            System.err.println("Email du sponsor non fourni.");
            return;
        }

        executorService.submit(() -> {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                }
            });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL, "Museum Digital"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));

                boolean isAccepted = status.equalsIgnoreCase("accepte") || status.equalsIgnoreCase("actif");
                String subject = isAccepted ? "Félicitations ! Votre demande de sponsoring est acceptée" 
                                            : "Mise à jour concernant votre demande de sponsoring";

                StringBuilder content = new StringBuilder();
                content.append("Bonjour ").append(sponsorName != null ? sponsorName : "Partenaire").append(",\n\n");
                
                if (isAccepted) {
                    content.append("Nous avons le plaisir de vous informer que votre demande de partenariat avec Museum Digital a été acceptée !\n");
                    content.append("Nous vous contacterons très prochainement pour finaliser les détails de notre collaboration.\n\n");
                } else {
                    content.append("Nous vous remercions pour l'intérêt que vous portez à Museum Digital.\n");
                    content.append("Malheureusement, après étude approfondie de votre dossier, nous ne pouvons pas donner suite à votre demande de partenariat pour le moment.\n\n");
                }
                content.append("Cordialement,\nL'équipe Museum Digital.");

                message.setSubject(subject);
                message.setText(content.toString());

                Transport.send(message);
                System.out.println("Email envoyé avec succès à " + toEmail);

            } catch (Exception e) {
                System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
            }
        });
    }
}


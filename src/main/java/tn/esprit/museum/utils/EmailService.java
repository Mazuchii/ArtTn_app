package tn.esprit.museum.utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;

public class EmailService {

    // Configuration SMTP (à modifier avec vos identifiants)
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_FROM = "yesmine.chourou@esprit.tn";
    private static final String EMAIL_PASSWORD = "uame lgva njlw vwgy";

    /**
     * Génère un code de réinitialisation à 6 chiffres
     */
    public static String generateResetCode() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000));
    }

    /**
     * Envoie un email avec code de réinitialisation à 6 chiffres
     */
    public static boolean sendPasswordResetEmail(String to, String resetCode) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, "Museum Digital Support"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("🔐 Code de réinitialisation - Museum Digital");

            // Contenu HTML avec CODE (pas de lien)
            String htmlContent = String.format(
                    "<!DOCTYPE html>" +
                            "<html>" +
                            "<head><meta charset='UTF-8'><title>Code de réinitialisation</title></head>" +
                            "<body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 40px;'>" +
                            "<div style='max-width: 500px; margin: 0 auto; background-color: white; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 8px rgba(0,0,0,0.1);'>" +
                            "<div style='background-color: #1a1a2e; padding: 20px; text-align: center;'>" +
                            "<h1 style='color: #d4af37; margin: 0;'>🏛️ MUSEUM DIGITAL</h1>" +
                            "</div>" +
                            "<div style='padding: 30px;'>" +
                            "<h2 style='color: #2c3e50;'>Réinitialisation du mot de passe</h2>" +
                            "<p style='color: #555; line-height: 1.6;'>Vous avez demandé la réinitialisation de votre mot de passe.</p>" +
                            "<div style='text-align: center; margin: 30px 0;'>" +
                            "<div style='background-color: #f8f9fa; padding: 20px; border-radius: 10px; border: 2px solid #d4af37;'>" +
                            "<p style='font-size: 14px; color: #555; margin: 0 0 10px 0;'>Votre code de réinitialisation est :</p>" +
                            "<span style='font-size: 36px; font-weight: bold; letter-spacing: 5px; color: #2c3e50;'>%s</span>" +
                            "</div>" +
                            "</div>" +
                            "<p style='color: #555; font-size: 12px;'>⚠️ Ce code expire dans 10 minutes. Entrez-le dans l'application pour réinitialiser votre mot de passe.</p>" +
                            "<p style='color: #555; font-size: 12px;'>Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.</p>" +
                            "<hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'>" +
                            "<p style='color: #999; font-size: 11px; text-align: center;'>© 2024 Museum Digital - Tous droits réservés</p>" +
                            "</div>" +
                            "</div>" +
                            "</body>" +
                            "</html>", resetCode
            );

            message.setContent(htmlContent, "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("✅ Email envoyé à: " + to + " avec code: " + resetCode);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Envoie un email de confirmation de changement de mot de passe
     */
    public static boolean sendPasswordChangedConfirmation(String to) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, "Museum Digital Support"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("✅ Votre mot de passe a été modifié - Museum Digital");

            String htmlContent =
                    "<!DOCTYPE html>" +
                            "<html>" +
                            "<head><meta charset='UTF-8'></head>" +
                            "<body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 40px;'>" +
                            "<div style='max-width: 500px; margin: 0 auto; background-color: white; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 8px rgba(0,0,0,0.1);'>" +
                            "<div style='background-color: #27ae60; padding: 20px; text-align: center;'>" +
                            "<h1 style='color: white; margin: 0;'>✅ Confirmation</h1>" +
                            "</div>" +
                            "<div style='padding: 30px;'>" +
                            "<h2 style='color: #2c3e50;'>Mot de passe modifié</h2>" +
                            "<p style='color: #555; line-height: 1.6;'>Votre mot de passe a été modifié avec succès.</p>" +
                            "<p style='color: #555;'>Si vous n'êtes pas à l'origine de cette modification, contactez immédiatement notre support.</p>" +
                            "<hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'>" +
                            "<p style='color: #999; font-size: 11px; text-align: center;'>© 2024 Museum Digital - Tous droits réservés</p>" +
                            "</div>" +
                            "</div>" +
                            "</body>" +
                            "</html>";

            message.setContent(htmlContent, "text/html; charset=utf-8");
            Transport.send(message);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
package tn.esprit.museum.utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;

public class EmailService {

    // Configuration GMAIL (à remplacer par vos identifiants réels)
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_FROM = "benzarbmalek0@gmail.com";
    private static final String EMAIL_PASSWORD = "dgnk lcmy fvhf adwp";

    public static String generateResetCode() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000));
    }

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

            String htmlContent = String.format(
                    "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
                            "<body style='font-family: Arial; background:#f4f4f4; padding:40px;'>" +
                            "<div style='max-width:500px; margin:0 auto; background:white; border-radius:10px;'>" +
                            "<div style='background:#1a1a2e; padding:20px; text-align:center;'>" +
                            "<h1 style='color:#d4af37;'>🏛️ MUSEUM DIGITAL</h1></div>" +
                            "<div style='padding:30px;'>" +
                            "<h2 style='color:#2c3e50;'>Réinitialisation du mot de passe</h2>" +
                            "<p>Votre code de réinitialisation est :</p>" +
                            "<div style='text-align:center; margin:30px 0; padding:20px; background:#f8f9fa; border:2px solid #d4af37; border-radius:10px;'>" +
                            "<span style='font-size:36px; font-weight:bold; letter-spacing:5px; color:#2c3e50;'>%s</span>" +
                            "</div>" +
                            "<p>⚠️ Ce code expire dans 10 minutes.</p>" +
                            "<p>© 2025 Museum Digital</p>" +
                            "</div></div></body></html>", resetCode
            );

            message.setContent(htmlContent, "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("✅ Email réinitialisation envoyé à: " + to);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            return false;
        }
    }

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
            message.setSubject("✅ Mot de passe modifié - Museum Digital");

            String htmlContent = "<!DOCTYPE html><body style='font-family:Arial; background:#f4f4f4; padding:40px;'>" +
                    "<div style='max-width:500px; margin:0 auto; background:white; border-radius:10px;'>" +
                    "<div style='background:#27ae60; padding:20px; text-align:center;'>" +
                    "<h1 style='color:white;'>✅ Confirmation</h1></div>" +
                    "<div style='padding:30px;'>" +
                    "<h2>Mot de passe modifié</h2>" +
                    "<p>Votre mot de passe a été modifié avec succès.</p>" +
                    "<p>© 2025 Museum Digital</p>" +
                    "</div></div></body></html>";

            message.setContent(htmlContent, "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("✅ Email confirmation envoyé à: " + to);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Envoie un email avec LIEN DE PAIEMENT STRIPE
     */
    public static boolean sendPaymentLinkEmail(String toEmail, String userName, int orderId, double amount, String paymentLink) {
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
            message.setFrom(new InternetAddress(EMAIL_FROM, "Museum Digital Boutique"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("💳 Paiement de votre commande #" + orderId);

            String htmlContent = String.format(
                    "<!DOCTYPE html>" +
                            "<html><head><meta charset='UTF-8'><style>" +
                            "body{font-family:Arial; background:#f4f4f4; padding:40px;}" +
                            ".container{max-width:500px; margin:0 auto; background:white; border-radius:10px;}" +
                            ".header{background:#1a1a2e; padding:20px; text-align:center;}" +
                            ".header h1{color:#d4af37; margin:0;}" +
                            ".content{padding:30px;}" +
                            ".amount-box{text-align:center; margin:30px 0; padding:20px; background:#f8f9fa; border:2px solid #d4af37; border-radius:10px;}" +
                            ".amount{font-size:28px; font-weight:bold; color:#d4af37;}" +
                            ".btn{display:inline-block; background:#d4af37; color:#2c3e50; padding:12px 25px; text-decoration:none; border-radius:25px; font-weight:bold;}" +
                            ".footer{text-align:center; color:#999; font-size:11px; padding:20px; border-top:1px solid #eee;}" +
                            "</style></head>" +
                            "<body>" +
                            "<div class='container'>" +
                            "<div class='header'><h1>🏛️ MUSEUM DIGITAL</h1></div>" +
                            "<div class='content'>" +
                            "<h2>Confirmation de commande</h2>" +
                            "<p>Bonjour <strong>%s</strong>,</p>" +
                            "<p>Merci pour votre commande #<strong>%d</strong>.</p>" +
                            "<div class='amount-box'>" +
                            "<p>Montant total :</p>" +
                            "<span class='amount'>%.2f D</span>" +
                            "</div>" +
                            "<p style='text-align:center;'>Cliquez ci-dessous pour payer :</p>" +
                            "<div style='text-align:center; margin:20px 0;'>" +
                            "<a href='%s' class='btn'>💳 Payer ma commande</a>" +
                            "</div>" +
                            "<p style='font-size:12px; color:#999; text-align:center;'>🔗 %s</p>" +
                            "</div>" +
                            "<div class='footer'><p>© 2025 Museum Digital</p></div>" +
                            "</div>" +
                            "</body></html>",
                    userName, orderId, amount, paymentLink, paymentLink
            );

            message.setContent(htmlContent, "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("✅ Email paiement envoyé à: " + toEmail);
            System.out.println("   🔗 Lien Stripe inclus: " + paymentLink);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email: " + e.getMessage());
            return false;
        }
    }
}
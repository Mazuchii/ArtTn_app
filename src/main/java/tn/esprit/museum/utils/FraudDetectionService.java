package tn.esprit.museum.utils;

import tn.esprit.museum.entities.User;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class FraudDetectionService {

    // Domaines d'emails jetables (sans "mail" pour éviter les faux positifs)
    private static final List<String> DISPOSABLE_DOMAINS = Arrays.asList(
            "10minutemail.com", "yopmail.com", "tempmail.com", "mailinator.com",
            "guerrillamail.com", "trashmail.com", "throwawaymail.com", "temp-mail.org",
            "fake-mail.net", "spam.com", "jetable.org", "guerrillamail.org",
            "guerrillamail.net", "guerrillamail.biz", "mailnator.com", "temp-mail.com"
    );

    // ==================== 1. APPEL AU MODÈLE ML (PYTHON) ====================

    public static double getMLPrediction(User user) {
        try {
            String email = (user.getEmail() != null) ? user.getEmail() : "unknown";
            String username = (user.getUsername() != null) ? user.getUsername() : "unknown";
            String fullName = (user.getFullName() != null) ? user.getFullName() : "unknown";
            String createdAt = (user.getCreatedAt() != null) ? formatTimestampForPython(user.getCreatedAt()) : LocalDateTime.now().toString();
            String hasPhoto = (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) ? "1" : "0";

            ProcessBuilder pb = new ProcessBuilder(
                    "python", "ml/predict.py",
                    email, username, fullName, "hidden", createdAt, hasPhoto
            );

            Process p = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;

            while ((line = in.readLine()) != null) {
                if (line.startsWith("PROBABILITY:")) {
                    return Double.parseDouble(line.split(":")[1]);
                }
            }
            p.waitFor();
        } catch (Exception e) {
            System.err.println("❌ Erreur ML : " + e.getMessage());
        }
        return -1.0;
    }

    private static String formatTimestampForPython(Timestamp timestamp) {
        if (timestamp == null) return LocalDateTime.now().toString();
        return timestamp.toString().substring(0, 10);
    }

    // ==================== 2. ANALYSE HEURISTIQUE (SANS MOT DE PASSE) ====================

    public static int calculateHeuristicScore(User user) {
        int score = 0;

        // 1. Email jetable (+30)
        if (isDisposableEmail(user.getEmail())) score += 30;

        // 2. Nom suspect (+25)
        if (isSuspiciousName(user.getFullName())) score += 25;

        // 3. Username suspect (+20)
        if (isSuspiciousUsername(user.getUsername())) score += 20;

        // 4. Pas de photo de profil (+10)
        if (user.getProfilePicture() == null || user.getProfilePicture().isEmpty()) score += 10;

        // 5. Compte récent (+15 si moins de 7 jours, +5 si aujourd'hui)
        if (user.getCreatedAt() != null) {
            long days = getDaysBetweenTimestamps(user.getCreatedAt(), new Timestamp(System.currentTimeMillis()));
            if (days < 1) score += 15;
            else if (days < 7) score += 10;
        }

        return Math.min(score, 100);
    }

    private static long getDaysBetweenTimestamps(Timestamp start, Timestamp end) {
        long diffMillis = end.getTime() - start.getTime();
        return diffMillis / (1000 * 60 * 60 * 24);
    }

    private static boolean isDisposableEmail(String email) {
        if (email == null) return false;
        String lowerEmail = email.toLowerCase();

        // Extraire le domaine pour une vérification précise
        String[] parts = lowerEmail.split("@");
        if (parts.length < 2) return false;
        String domain = parts[1];

        return DISPOSABLE_DOMAINS.stream().anyMatch(domain::equals);
    }

    private static boolean isSuspiciousName(String name) {
        if (name == null || name.isEmpty()) return true;
        if (name.length() <= 3) return true;

        // Détecter les noms avec caractères répétés (aaaaaa, bbbbbb)
        if (name.length() >= 4 && name.chars().distinct().count() == 1) return true;

        String n = name.toLowerCase();
        return n.contains("test") || n.contains("admin") || n.contains("fake") ||
                n.contains("user") || n.contains("temp") || n.contains("aaa") ||
                n.contains("bbb") || n.contains("xxx");
    }

    private static boolean isSuspiciousUsername(String username) {
        if (username == null || username.isEmpty()) return true;
        if (username.length() <= 3) return true;

        // Détecter les usernames avec caractères répétés
        if (username.length() >= 4 && username.chars().distinct().count() == 1) return true;

        String u = username.toLowerCase();
        if (u.startsWith("user") || u.startsWith("test") || u.startsWith("admin") ||
                u.startsWith("temp") || u.startsWith("new") || u.startsWith("guest")) return true;

        // Username composé uniquement de chiffres
        if (username.matches("\\d+")) return true;

        return false;
    }

    // ==================== 3. SYNTHÈSE ET RAPPORTS ====================

    public static int getFinalRiskScore(User user) {
        double mlProba = getMLPrediction(user);
        int heuristic = calculateHeuristicScore(user);

        if (mlProba < 0) return heuristic;

        double combinedScore = (mlProba * 100 * 0.7) + (heuristic * 0.3);
        return (int) Math.min(combinedScore, 100);
    }

    public static String getRiskLevelLabel(User user) {
        int score = getFinalRiskScore(user);
        if (score >= 60) return "🔴 CRITIQUE";
        if (score >= 35) return "🟠 SUSPECT";
        return "🟢 LÉGITIME";
    }

    public static boolean isFraudulent(User user) {
        return getFinalRiskScore(user) >= 40;
    }

    public static List<User> detectFraudulentUsers(List<User> users) {
        return users.stream()
                .filter(FraudDetectionService::isFraudulent)
                .collect(Collectors.toList());
    }

    public static FraudReport getDetailedReport(User user) {
        int score = getFinalRiskScore(user);
        String label = getRiskLevelLabel(user);

        FraudReport report = new FraudReport();
        report.setId(user.getId());
        report.setUsername(user.getUsername());
        report.setEmail(user.getEmail());
        report.setFullName(user.getFullName());
        report.setRiskScore(score);
        report.setRiskLevelLabel(label);
        report.setFraudulent(score >= 40);

        List<String> flags = new ArrayList<>();

        if (isDisposableEmail(user.getEmail())) flags.add("📧 Email jetable");
        if (isSuspiciousName(user.getFullName())) flags.add("👤 Nom suspect (" + user.getFullName() + ")");
        if (isSuspiciousUsername(user.getUsername())) flags.add("👤 Nom d'utilisateur suspect (" + user.getUsername() + ")");
        if (user.getProfilePicture() == null || user.getProfilePicture().isEmpty()) flags.add("🖼️ Photo de profil manquante");

        if (user.getCreatedAt() != null) {
            long days = getDaysBetweenTimestamps(user.getCreatedAt(), new Timestamp(System.currentTimeMillis()));
            if (days < 1) flags.add("📅 Compte créé aujourd'hui");
            else if (days < 7) flags.add("📅 Compte récent (" + days + " jours)");
        }

        if (score >= 60) flags.add("🤖 IA: Comportement très suspect");
        else if (score >= 35) flags.add("🤖 IA: Comportement suspect");

        report.setRedFlags(flags);
        return report;
    }

    public static FraudStatistics getStatistics(List<User> users) {
        FraudStatistics stats = new FraudStatistics();
        stats.setTotalUsers(users.size());

        int fraudulent = 0, critical = 0, suspect = 0, legit = 0;

        for (User user : users) {
            if (isFraudulent(user)) fraudulent++;
            String level = getRiskLevelLabel(user);
            if (level.contains("CRITIQUE")) critical++;
            else if (level.contains("SUSPECT")) suspect++;
            else legit++;
        }

        stats.setFraudulentCount(fraudulent);
        stats.setCriticalCount(critical);
        stats.setSuspectCount(suspect);
        stats.setLegitCount(legit);

        return stats;
    }

    // ==================== CLASSES DE DONNÉES ====================

    public static class FraudReport {
        private int id;
        private String username;
        private String email;
        private String fullName;
        private int riskScore;
        private String riskLevelLabel;
        private boolean isFraudulent;
        private List<String> redFlags;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public int getRiskScore() { return riskScore; }
        public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
        public String getRiskLevelLabel() { return riskLevelLabel; }
        public void setRiskLevelLabel(String riskLevelLabel) { this.riskLevelLabel = riskLevelLabel; }
        public boolean isFraudulent() { return isFraudulent; }
        public void setFraudulent(boolean fraudulent) { isFraudulent = fraudulent; }
        public List<String> getRedFlags() { return redFlags; }
        public void setRedFlags(List<String> redFlags) { this.redFlags = redFlags; }
    }

    public static class FraudStatistics {
        private int totalUsers;
        private int fraudulentCount;
        private int criticalCount;
        private int suspectCount;
        private int legitCount;

        public int getTotalUsers() { return totalUsers; }
        public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }
        public int getFraudulentCount() { return fraudulentCount; }
        public void setFraudulentCount(int fraudulentCount) { this.fraudulentCount = fraudulentCount; }
        public int getCriticalCount() { return criticalCount; }
        public void setCriticalCount(int criticalCount) { this.criticalCount = criticalCount; }
        public int getSuspectCount() { return suspectCount; }
        public void setSuspectCount(int suspectCount) { this.suspectCount = suspectCount; }
        public int getLegitCount() { return legitCount; }
        public void setLegitCount(int legitCount) { this.legitCount = legitCount; }
        public double getFraudulentPercentage() {
            return totalUsers > 0 ? (fraudulentCount * 100.0 / totalUsers) : 0;
        }
    }
}
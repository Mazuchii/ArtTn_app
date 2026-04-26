package edu.Projet_java.services;

import edu.Projet_java.entities.Report;
import edu.Projet_java.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportServices {

    // Ajouter un signalement
    public boolean addReport(Report report) {
        // Vérifier si l'utilisateur a déjà signalé ce post
        if (hasUserReported(report.getPostId(), report.getUserId())) {
            System.out.println("Vous avez déjà signalé ce post");
            return false;
        }

        String sql = "INSERT INTO post_reports (post_id, user_id, reason, details) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, report.getPostId());
            pst.setInt(2, report.getUserId());
            pst.setString(3, report.getReason());
            pst.setString(4, report.getDetails());
            pst.executeUpdate();

            // Incrémenter le compteur de signalements dans la table post
            incrementReportCount(report.getPostId());

            return true;
        } catch (SQLException e) {
            System.err.println("Erreur ajout signalement: " + e.getMessage());
            return false;
        }
    }

    // Vérifier si l'utilisateur a déjà signalé
    public boolean hasUserReported(int postId, int userId) {
        String sql = "SELECT 1 FROM post_reports WHERE post_id = ? AND user_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, postId);
            pst.setInt(2, userId);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Erreur vérification: " + e.getMessage());
            return false;
        }
    }

    // Incrémenter le compteur de signalements
    private void incrementReportCount(int postId) {
        String sql = "UPDATE post SET report_count = report_count + 1 WHERE post_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, postId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur incrémentation: " + e.getMessage());
        }
    }

    // Récupérer tous les signalements
    public List<Report> getAllReports() {
        List<Report> reports = new ArrayList<>();
        String sql = "SELECT * FROM post_reports ORDER BY created_at DESC";
        try {
            Statement st = MyConnection.getInstance().getCnx().createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                reports.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement signalements: " + e.getMessage());
        }
        return reports;
    }

    // Récupérer les signalements par post
    public List<Report> getReportsByPost(int postId) {
        List<Report> reports = new ArrayList<>();
        String sql = "SELECT * FROM post_reports WHERE post_id = ? ORDER BY created_at DESC";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, postId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                reports.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur: " + e.getMessage());
        }
        return reports;
    }

    // Changer le statut d'un signalement
    public void updateReportStatus(int reportId, String status) {
        String sql = "UPDATE post_reports SET status = ? WHERE id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setString(1, status);
            pst.setInt(2, reportId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur mise à jour: " + e.getMessage());
        }
    }

    // Supprimer un signalement (après traitement)
    public void deleteReport(int reportId) {
        String sql = "DELETE FROM post_reports WHERE id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, reportId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur suppression: " + e.getMessage());
        }
    }

    private Report mapResultSet(ResultSet rs) throws SQLException {
        Report report = new Report();
        report.setId(rs.getInt("id"));
        report.setPostId(rs.getInt("post_id"));
        report.setUserId(rs.getInt("user_id"));
        report.setReason(rs.getString("reason"));
        report.setDetails(rs.getString("details"));
        report.setStatus(rs.getString("status"));
        report.setCreatedAt(rs.getTimestamp("created_at"));
        return report;
    }
}
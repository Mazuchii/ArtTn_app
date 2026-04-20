package edu.Projet_java.services;

import edu.Projet_java.entities.commentaire;
import edu.Projet_java.interfaces.ICommentservice;
import edu.Projet_java.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentServices implements ICommentservice<commentaire> {

    @Override
    public void addComment(commentaire c) {
        String requete = "INSERT INTO commentaire (post_id, contenu, author_id) VALUES (?, ?, ?)";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
            pst.setInt(1, c.getPost_id());
            pst.setString(2, c.getContenu());
            pst.setInt(3, c.getAuthor_id());
            pst.executeUpdate();
            System.out.println("Commentaire ajouté avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du commentaire : " + e.getMessage());
        }
    }

    @Override
    public void deleteComment(commentaire c) {
        String requete = "DELETE FROM commentaire WHERE comment_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
            pst.setInt(1, c.getComment_id());
            pst.executeUpdate();
            System.out.println("Commentaire supprimé !");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du commentaire : " + e.getMessage());
        }
    }

    @Override
    public void updateComment(int id, commentaire c) {
        String requete = "UPDATE commentaire SET contenu = ? WHERE comment_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
            pst.setString(1, c.getContenu());
            pst.setInt(2, id);
            pst.executeUpdate();
            System.out.println("Commentaire mis à jour !");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du commentaire : " + e.getMessage());
        }
    }

    @Override
    public List<commentaire> getCommentData() {
        List<commentaire> data = new ArrayList<>();
        String requete = "SELECT * FROM commentaire";
        try {
            Statement st = MyConnection.getInstance().getCnx().createStatement();
            ResultSet rs = st.executeQuery(requete);
            while (rs.next()) {
                commentaire c = new commentaire();
                c.setComment_id(rs.getInt("comment_id"));
                c.setPost_id(rs.getInt("post_id"));
                c.setContenu(rs.getString("contenu"));
                c.setAuthor_id(rs.getInt("author_id"));
                data.add(c);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la lecture des commentaires : " + e.getMessage());
        }
        return data;
    }

    // ✅ Compter les commentaires par post
    public int countCommentsByPostId(int postId) {
        int count = 0;
        String requete = "SELECT COUNT(*) FROM commentaire WHERE post_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
            pst.setInt(1, postId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du comptage : " + e.getMessage());
        }
        return count;
    }

    // ✅ Récupérer les commentaires par post (CORRIGÉ)
    public List<commentaire> getCommentsByPostId(int postId) {
        List<commentaire> data = new ArrayList<>();
        String requete = "SELECT * FROM commentaire WHERE post_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
            pst.setInt(1, postId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                commentaire c = new commentaire();
                c.setComment_id(rs.getInt("comment_id"));
                c.setPost_id(rs.getInt("post_id"));
                c.setContenu(rs.getString("contenu"));
                c.setAuthor_id(rs.getInt("author_id"));
                data.add(c);
                System.out.println("Commentaire chargé: " + c.getContenu()); // Debug
            }
            System.out.println("Total commentaires pour post " + postId + ": " + data.size()); // Debug
        } catch (SQLException e) {
            System.err.println("Erreur lors de la lecture des commentaires : " + e.getMessage());
            e.printStackTrace();
        }
        return data;
    }
}
package edu.Projet_java.services;

import edu.Projet_java.entities.Posts;
import edu.Projet_java.interfaces.IPostServices;
import edu.Projet_java.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostServices implements IPostServices<Posts> {

    @Override
    public void addpost(Posts p) {
        // ✅ REQUÊTE CORRECTE (sans "status" qui n'existe pas)
        String requete = "INSERT INTO post (post_titre, post_contenu, author_id, date_creation, category_id, image_url) VALUES (?, ?, ?, NOW(), ?, ?)";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
            pst.setString(1, p.getPost_titre());
            pst.setString(2, p.getPost_contenu());
            pst.setInt(3, p.getAuthor_id());
            pst.setInt(4, p.getCategoryId());
            pst.setString(5, p.getImageUrl());
            pst.executeUpdate();
            System.out.println("✅ Post ajouté avec succès !");
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    @Override
    public List<Posts> getPostData() {
        List<Posts> data = new ArrayList<>();
        String requete = "SELECT * FROM post ORDER BY date_creation DESC";
        try {
            Statement st = MyConnection.getInstance().getCnx().createStatement();
            ResultSet rs = st.executeQuery(requete);
            while (rs.next()) {
                data.add(mapResultSetToPost(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la lecture : " + e.getMessage());
        }
        return data;
    }

    public Posts getPostById(int id) {
        String requete = "SELECT * FROM post WHERE post_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return mapResultSetToPost(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erreur: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void deletepost(int id) {
        String requete = "DELETE FROM post WHERE post_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("✅ Post supprimé !");
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression : " + e.getMessage());
        }
    }

    @Override
    public void updatepost(int id, Posts p) {
        String requete = "UPDATE post SET post_titre = ?, post_contenu = ?, category_id = ?, image_url = ? WHERE post_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
            pst.setString(1, p.getPost_titre());
            pst.setString(2, p.getPost_contenu());
            pst.setInt(3, p.getCategoryId());
            pst.setString(4, p.getImageUrl());
            pst.setInt(5, id);
            pst.executeUpdate();
            System.out.println("✅ Post mis à jour !");
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la mise à jour : " + e.getMessage());
        }
    }

    // ✅ NOUVELLE MÉTHODE : Récupérer les posts par catégorie
    public List<Posts> getPostsByCategory(int categoryId) {
        List<Posts> data = new ArrayList<>();
        String requete = "SELECT * FROM post WHERE category_id = ? ORDER BY date_creation DESC";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
            pst.setInt(1, categoryId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                data.add(mapResultSetToPost(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la lecture : " + e.getMessage());
        }
        return data;
    }

    // ✅ NOUVELLE MÉTHODE : Incrémenter les vues
    public void incrementViews(int postId) {
        String requete = "UPDATE post SET views = views + 1 WHERE post_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
            pst.setInt(1, postId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur incrémentation vues : " + e.getMessage());
        }
    }

    // ✅ NOUVELLE MÉTHODE : Ajouter un like
    public void addLike(int postId) {
        String requete = "UPDATE post SET likes = likes + 1 WHERE post_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
            pst.setInt(1, postId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur ajout like : " + e.getMessage());
        }
    }

    // ✅ NOUVELLE MÉTHODE : Ajouter un dislike
    public void addDislike(int postId) {
        String requete = "UPDATE post SET dislikes = dislikes + 1 WHERE post_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
            pst.setInt(1, postId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur ajout dislike : " + e.getMessage());
        }
    }

    // ✅ MÉTHODE UTILITAIRE : Mapping ResultSet → Posts
    private Posts mapResultSetToPost(ResultSet rs) throws SQLException {
        Posts p = new Posts();
        p.setPost_id(rs.getInt("post_id"));
        p.setPost_titre(rs.getString("post_titre"));
        p.setPost_contenu(rs.getString("post_contenu"));
        p.setAuthor_id(rs.getInt("author_id"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setViews(rs.getInt("views"));
        p.setLikes(rs.getInt("likes"));
        p.setDislikes(rs.getInt("dislikes"));
        p.setImageUrl(rs.getString("image_url"));
        p.setReportCount(rs.getInt("report_count"));

        Timestamp timestamp = rs.getTimestamp("date_creation");
        if (timestamp != null) {
            p.setDate_creation(timestamp.toLocalDateTime());
        }
        return p;
    }
    public boolean hasUserViewedPost(int postId, int userId) {
        String sql = "SELECT 1 FROM post_views WHERE post_id = ? AND user_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, postId);
            pst.setInt(2, userId);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Erreur: " + e.getMessage());
            return false;
        }
    }

    // ✅ Enregistrer une vue unique
    public void addView(int postId, int userId) {
        if (hasUserViewedPost(postId, userId)) return;

        String sql = "INSERT INTO post_views (post_id, user_id) VALUES (?, ?)";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, postId);
            pst.setInt(2, userId);
            pst.executeUpdate();

            // Incrémenter le compteur de vues dans la table post
            String updateSql = "UPDATE post SET views = views + 1 WHERE post_id = ?";
            PreparedStatement pst2 = MyConnection.getInstance().getCnx().prepareStatement(updateSql);
            pst2.setInt(1, postId);
            pst2.executeUpdate();

            System.out.println("✅ Vue enregistrée pour user " + userId + " sur post " + postId);
        } catch (SQLException e) {
            System.err.println("Erreur addView: " + e.getMessage());
        }
    }

    // ✅ Récupérer la réaction de l'utilisateur (LIKE, DISLIKE, ou null)
    public String getUserReaction(int postId, int userId) {
        String sql = "SELECT type FROM post_reactions WHERE post_id = ? AND user_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, postId);
            pst.setInt(2, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getString("type");
            }
        } catch (SQLException e) {
            System.err.println("Erreur: " + e.getMessage());
        }
        return null;
    }

    // ✅ Ajouter ou modifier un like/dislike
    public void addReaction(int postId, int userId, String type) {
        String existingReaction = getUserReaction(postId, userId);

        // Si l'utilisateur a déjà la même réaction -> on la supprime (toggle off)
        if (existingReaction != null && existingReaction.equals(type)) {
            // Supprimer la réaction
            String deleteSql = "DELETE FROM post_reactions WHERE post_id = ? AND user_id = ?";
            try {
                PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(deleteSql);
                pst.setInt(1, postId);
                pst.setInt(2, userId);
                pst.executeUpdate();

                // Décrémenter le compteur
                if (type.equals("LIKE")) {
                    decrementLikes(postId);
                } else {
                    decrementDislikes(postId);
                }
                System.out.println("✅ Réaction " + type + " retirée pour user " + userId);
            } catch (SQLException e) {
                System.err.println("Erreur suppression réaction: " + e.getMessage());
            }
            return;
        }

        // Si l'utilisateur a une réaction différente -> on change
        if (existingReaction != null) {
            // Supprimer l'ancienne réaction
            String deleteSql = "DELETE FROM post_reactions WHERE post_id = ? AND user_id = ?";
            try {
                PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(deleteSql);
                pst.setInt(1, postId);
                pst.setInt(2, userId);
                pst.executeUpdate();

                // Décrémenter l'ancien compteur
                if (existingReaction.equals("LIKE")) {
                    decrementLikes(postId);
                } else {
                    decrementDislikes(postId);
                }
            } catch (SQLException e) {
                System.err.println("Erreur suppression ancienne réaction: " + e.getMessage());
            }
        }

        // Ajouter la nouvelle réaction
        String insertSql = "INSERT INTO post_reactions (post_id, user_id, type) VALUES (?, ?, ?)";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(insertSql);
            pst.setInt(1, postId);
            pst.setInt(2, userId);
            pst.setString(3, type);
            pst.executeUpdate();

            // Incrémenter le nouveau compteur
            if (type.equals("LIKE")) {
                incrementLikes(postId);
            } else {
                incrementDislikes(postId);
            }

            System.out.println("✅ Réaction " + type + " enregistrée pour user " + userId);
        } catch (SQLException e) {
            System.err.println("Erreur addReaction: " + e.getMessage());
        }
    }

    private void incrementLikes(int postId) {
        String sql = "UPDATE post SET likes = likes + 1 WHERE post_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, postId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur incrementLikes: " + e.getMessage());
        }
    }

    private void decrementLikes(int postId) {
        String sql = "UPDATE post SET likes = likes - 1 WHERE post_id = ? AND likes > 0";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, postId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur decrementLikes: " + e.getMessage());
        }
    }

    private void incrementDislikes(int postId) {
        String sql = "UPDATE post SET dislikes = dislikes + 1 WHERE post_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, postId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur incrementDislikes: " + e.getMessage());
        }
    }

    private void decrementDislikes(int postId) {
        String sql = "UPDATE post SET dislikes = dislikes - 1 WHERE post_id = ? AND dislikes > 0";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, postId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur decrementDislikes: " + e.getMessage());
        }
    }
    public boolean isContentExists(String content, int userId) {
        String sql = "SELECT COUNT(*) FROM post WHERE post_contenu = ? AND author_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setString(1, content);
            pst.setInt(2, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur vérification contenu: " + e.getMessage());
        }
        return false;
    }

    // Vérifier si le même titre existe déjà (version plus stricte)
    public boolean isTitleExists(String title, int userId) {
        String sql = "SELECT COUNT(*) FROM post WHERE post_titre = ? AND author_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setString(1, title);
            pst.setInt(2, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur vérification titre: " + e.getMessage());
        }
        return false;
    }

}
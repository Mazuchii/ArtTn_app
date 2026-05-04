package tn.esprit.museum.services;

import tn.esprit.museum.entities.Category;
import tn.esprit.museum.interfaces.ICategoryService;
import tn.esprit.museum.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryServices implements ICategoryService<Category> {

    @Override
    public void addCategory(Category c) {
        String sql = "INSERT INTO categories (name, description, icon) VALUES (?, ?, ?)";
        try {
            PreparedStatement pst = DatabaseConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setString(1, c.getName());
            pst.setString(2, c.getDescription());
            pst.setString(3, c.getIcon());
            pst.executeUpdate();
            System.out.println("✅ Catégorie ajoutée : " + c.getName());
        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout catégorie : " + e.getMessage());
        }
    }

    @Override
    public void deleteCategory(int id) {
        String sql = "DELETE FROM categories WHERE id = ?";
        try {
            PreparedStatement pst = DatabaseConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("✅ Catégorie supprimée (ID: " + id + ")");
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression catégorie : " + e.getMessage());
        }
    }

    @Override
    public void updateCategory(int id, Category c) {
        String sql = "UPDATE categories SET name = ?, description = ?, icon = ? WHERE id = ?";
        try {
            PreparedStatement pst = DatabaseConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setString(1, c.getName());
            pst.setString(2, c.getDescription());
            pst.setString(3, c.getIcon());
            pst.setInt(4, id);
            pst.executeUpdate();
            System.out.println("✅ Catégorie modifiée : " + c.getName());
        } catch (SQLException e) {
            System.err.println("❌ Erreur modification catégorie : " + e.getMessage());
        }
    }

    @Override
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories ORDER BY name";
        try {
            Statement stmt = DatabaseConnection.getInstance().getCnx().createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Category c = new Category();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setDescription(rs.getString("description"));
                c.setIcon(rs.getString("icon"));
                categories.add(c);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement catégories : " + e.getMessage());
        }
        return categories;
    }

    @Override
    public Category getCategoryById(int id) {
        String sql = "SELECT * FROM categories WHERE id = ?";
        try {
            PreparedStatement pst = DatabaseConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                Category c = new Category();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setDescription(rs.getString("description"));
                c.setIcon(rs.getString("icon"));
                return c;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur : " + e.getMessage());
        }
        return null;
    }
}

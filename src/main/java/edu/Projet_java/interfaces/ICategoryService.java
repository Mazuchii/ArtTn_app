package edu.Projet_java.interfaces;

import edu.Projet_java.entities.Category;
import java.util.List;

public interface ICategoryService<T> {
    void addCategory(T t);
    void deleteCategory(int id);
    void updateCategory(int id, T t);
    List<Category> getAllCategories();
    Category getCategoryById(int id);
}
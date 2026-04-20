package edu.Projet_java.interfaces;

import edu.Projet_java.entities.Posts;
import java.util.List;

public interface IPostServices<T> {
    void addpost(T t);
    void deletepost(int id);
    void updatepost(int id, T t);
    List<Posts> getPostData();

    // ✅ NOUVELLES MÉTHODES
    List<Posts> getPostsByCategory(int categoryId);
    void incrementViews(int postId);
    void addLike(int postId);
    void addDislike(int postId);
}
package edu.Projet_java.interfaces;


import java.util.List;

public interface ICommentservice<T>{
    void addComment(T t);
    void deleteComment(T t);
    void updateComment(int id, T t);
    List<T> getCommentData();

}

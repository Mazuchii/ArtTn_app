package tn.esprit.produit.interfaces;

import java.sql.SQLException;
import java.util.List;


public interface Iservice<T> {


    void insert(T entity) throws SQLException;

    void update(T entity) throws SQLException;

    void delete(int id) throws SQLException;

    T getById(int id) throws SQLException;

    List<T> getAll() throws SQLException;
}
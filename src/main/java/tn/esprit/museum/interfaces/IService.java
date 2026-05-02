package tn.esprit.museum.interfaces;

import java.sql.SQLException;
import java.util.List;

public interface IService<T> {
    default void ajouter(T entity) {
        throw new UnsupportedOperationException("Not implemented");
    }

    default void supprimer(int id) {
        throw new UnsupportedOperationException("Not implemented");
    }

    default void modifier(T entity) {
        throw new UnsupportedOperationException("Not implemented");
    }

    default List<T> afficher() {
        throw new UnsupportedOperationException("Not implemented");
    }

    default void insert(T entity) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    default void update(T entity) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    default void delete(int id) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    default T getById(int id) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    default List<T> getAll() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }
}

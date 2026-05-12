package tn.esprit.museum.interfaces;

import java.util.List;

public interface ISponsorService<T>{

    void addEntity(T t);
    void deleteEntity(T t);
    void updateEntity(int id,T t);
    List<T> getData();
}


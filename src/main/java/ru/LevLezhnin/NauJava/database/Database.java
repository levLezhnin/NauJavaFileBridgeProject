package ru.LevLezhnin.NauJava.database;

import java.util.Optional;

public interface Database<EntityType, ID> {
    EntityType save(ID id, EntityType entity);
    Optional<EntityType> findById(ID id);
    void deleteById(ID id);
}

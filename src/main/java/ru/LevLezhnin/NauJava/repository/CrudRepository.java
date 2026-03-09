package ru.LevLezhnin.NauJava.repository;

import java.util.Optional;

public interface CrudRepository<EntityType, ID> {
    void create(EntityType entity);
    Optional<EntityType> findById(ID id);
    void update(EntityType entity);
    void delete(ID id);
}

package ru.LevLezhnin.NauJava.repository;

import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.database.Database;
import ru.LevLezhnin.NauJava.model.User;

import java.util.Optional;

@Component
public class UserRepository implements CrudRepository<User, Long> {

    private final Database<User, Long> userDatabase;

    public UserRepository(Database<User, Long> userDatabase) {
        this.userDatabase = userDatabase;
    }

    @Override
    public void create(User entity) {
        userDatabase.save(entity.getId(), entity);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userDatabase.findById(id);
    }

    @Override
    public void update(User entity) {
        userDatabase.save(entity.getId(), entity);
    }

    @Override
    public void delete(Long id) {
        userDatabase.deleteById(id);
    }
}

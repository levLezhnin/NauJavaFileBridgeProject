package ru.LevLezhnin.NauJava.database;

import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class UserDatabase implements Database<User, Long> {

    private final Map<Long, User> data;

    public UserDatabase() {
        this.data = new HashMap<>();
    }

    @Override
    public User save(Long id, User entity) {
        if (id < 0) {
            throw new IllegalArgumentException("id не может быть меньше 0");
        }
        data.put(id, entity);
        return entity;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public void deleteById(Long id) {
        data.remove(id);
    }
}

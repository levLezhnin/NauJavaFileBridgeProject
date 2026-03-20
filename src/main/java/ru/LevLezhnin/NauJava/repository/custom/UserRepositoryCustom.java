package ru.LevLezhnin.NauJava.repository.custom;

import org.springframework.data.domain.Pageable;
import ru.LevLezhnin.NauJava.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryCustom {
    Optional<User> findById(Long id);
    List<User> findAll(Pageable pageable);
    List<User> findByUsername(String username, Pageable pageable);
    Optional<User> findByEmail(String email);
    void deleteById(Long id);
}

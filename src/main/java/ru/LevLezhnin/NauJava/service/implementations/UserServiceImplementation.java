package ru.LevLezhnin.NauJava.service.implementations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.exceptions.EntityNotFoundException;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.repository.CrudRepository;
import ru.LevLezhnin.NauJava.service.interfaces.UserService;

@Component
public class UserServiceImplementation implements UserService {

    private final CrudRepository<User, Long> userCrudRepository;
    private final PasswordEncoder passwordEncryptor;

    @Autowired
    public UserServiceImplementation(CrudRepository<User, Long> userCrudRepository, PasswordEncoder passwordEncryptor) {
        this.userCrudRepository = userCrudRepository;
        this.passwordEncryptor = passwordEncryptor;
    }

    private boolean validate(String username, String email, String password) {
        return (username != null && !username.isBlank())
                && (email != null && !email.isBlank())
                && (password != null && !password.isBlank());
    }

    @Override
    public void createUser(long id, String username, String email, String password) {
        if (!validate(username, email, password)) {
            throw new IllegalArgumentException("Для нового пользователя должны быть заполнены поля: логин, email, пароль");
        }
        User user = User.builder().setId(id)
                .setUsername(username)
                .setEmail(email)
                .setPasswordHash(passwordEncryptor.encode(password))
                .build();
        userCrudRepository.create(user);
    }

    @Override
    public User findById(long id) {
        return userCrudRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id: %d не найден".formatted(id)));
    }

    @Override
    public void deleteById(long id) {
        userCrudRepository.delete(id);
    }

    @Override
    public void updateUsername(long id, String username) {
        User user = findById(id);
        user.setUsername(username);
        userCrudRepository.update(user);
    }

    @Override
    public void updatePassword(long id, String password) {
        User user = findById(id);
        user.setPasswordHash(passwordEncryptor.encode(password));
        userCrudRepository.update(user);
    }
}

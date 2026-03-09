package ru.LevLezhnin.NauJava.service.interfaces;

import ru.LevLezhnin.NauJava.model.User;

public interface UserService {
    void createUser(long id, String username, String email, String password);
    User findById(long id);
    void deleteById(long id);
    void updateUsername(long id, String username);
    void updatePassword(long id, String password);
}

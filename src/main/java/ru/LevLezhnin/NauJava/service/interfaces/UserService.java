package ru.LevLezhnin.NauJava.service.interfaces;

import ru.LevLezhnin.NauJava.model.User;

import java.util.List;

public interface UserService {
    void createUser(String username, String email, String password);
    User findById(long id);
    void deleteById(long id);
    void updateUsername(long id, String username);
    void updatePassword(long id, String password);
    List<User> findByCriteria(String searchBy, String searchValue, int page, int pageSize);
}

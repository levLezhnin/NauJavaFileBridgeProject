package ru.LevLezhnin.NauJava.service.interfaces;

import ru.LevLezhnin.NauJava.dto.auth.RegistrationRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UpdateUserRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UserProfileResponseDto;
import ru.LevLezhnin.NauJava.model.User;

import java.util.List;

public interface UserService {
    User createUser(RegistrationRequestDto registrationRequestDto);
    User findById(long id);
    User findByUsername(String username);
    User findByEmail(String email);
    UserProfileResponseDto getProfile();
    void updateUser(UpdateUserRequestDto updateUserRequestDto);
    void deleteUser();
    List<User> findByCriteria(String searchBy, String searchValue, int page, int pageSize);
}

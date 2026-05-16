package ru.LevLezhnin.NauJava.service.interfaces;

import ru.LevLezhnin.NauJava.dto.auth.RegistrationRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UpdateUserRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UserProfileAdminResponseDto;
import ru.LevLezhnin.NauJava.dto.user.UserProfileResponseDto;
import ru.LevLezhnin.NauJava.model.User;

import java.util.List;

public interface UserService {
    UserProfileResponseDto createUser(RegistrationRequestDto registrationRequestDto);
    UserProfileResponseDto getProfile();
    void updateUser(UpdateUserRequestDto updateUserRequestDto);
    void deleteUser();
    List<UserProfileAdminResponseDto> findByCriteria(String searchBy, String searchValue, int page, int pageSize);
}

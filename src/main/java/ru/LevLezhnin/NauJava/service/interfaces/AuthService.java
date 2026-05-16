package ru.LevLezhnin.NauJava.service.interfaces;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.LevLezhnin.NauJava.dto.auth.*;

public interface AuthService {
    JwtResponseDto register(RegistrationRequestDto registrationRequestDto);
    JwtResponseDto login(JwtLoginRequestDto jwtLoginRequestDto);
    JwtResponseDto refresh(JwtRefreshRequestDto jwtRefreshRequestDto);
    void logout(JwtLogoutRequestDto jwtLogoutRequestDto);
}

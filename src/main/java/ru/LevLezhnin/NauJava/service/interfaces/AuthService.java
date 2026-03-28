package ru.LevLezhnin.NauJava.service.interfaces;

import ru.LevLezhnin.NauJava.dto.auth.JwtLoginRequestDto;
import ru.LevLezhnin.NauJava.dto.auth.JwtRefreshRequestDto;
import ru.LevLezhnin.NauJava.dto.auth.JwtResponseDto;
import ru.LevLezhnin.NauJava.dto.auth.RegistrationRequestDto;

public interface AuthService {
    JwtResponseDto register(RegistrationRequestDto registrationRequestDto);
    JwtResponseDto login(JwtLoginRequestDto jwtLoginRequestDto);
    JwtResponseDto refresh(JwtRefreshRequestDto jwtRefreshRequestDto);
}

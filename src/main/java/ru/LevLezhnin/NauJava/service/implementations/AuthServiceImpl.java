package ru.LevLezhnin.NauJava.service.implementations;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ru.LevLezhnin.NauJava.dto.auth.JwtLoginRequestDto;
import ru.LevLezhnin.NauJava.dto.auth.JwtRefreshRequestDto;
import ru.LevLezhnin.NauJava.dto.auth.JwtResponseDto;
import ru.LevLezhnin.NauJava.dto.auth.RegistrationRequestDto;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.service.interfaces.AuthService;
import ru.LevLezhnin.NauJava.service.interfaces.UserService;
import ru.LevLezhnin.NauJava.utils.JwtTokenHelper;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserDetailsService userDetailsService;
    private final JwtTokenHelper jwtTokenHelper;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    public AuthServiceImpl(UserDetailsService userDetailsService, JwtTokenHelper jwtTokenHelper, AuthenticationManager authenticationManager, PasswordEncoder passwordEncryptor, UserService userService) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenHelper = jwtTokenHelper;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
    }


    @Override
    public JwtResponseDto register(RegistrationRequestDto registrationRequestDto) {
        User user = userService.createUser(registrationRequestDto);
        var userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        return new JwtResponseDto(
                jwtTokenHelper.generateAccessToken(userDetails),
                jwtTokenHelper.generateRefreshToken(userDetails)
        );
    }

    @Override
    public JwtResponseDto login(JwtLoginRequestDto jwtLoginRequestDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(jwtLoginRequestDto.username(), jwtLoginRequestDto.password())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return new JwtResponseDto(jwtTokenHelper.generateAccessToken(userDetails), jwtTokenHelper.generateRefreshToken(userDetails));
    }

    @Override
    public JwtResponseDto refresh(JwtRefreshRequestDto jwtRefreshRequestDto) {
        String refreshToken = jwtRefreshRequestDto.refreshToken();

        if (!jwtTokenHelper.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Невалидный refresh token");
        }

        String username = jwtTokenHelper.getUserNameFromRefreshToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String newAccessToken = jwtTokenHelper.generateAccessToken(userDetails);

        return new JwtResponseDto(newAccessToken, refreshToken);
    }
}

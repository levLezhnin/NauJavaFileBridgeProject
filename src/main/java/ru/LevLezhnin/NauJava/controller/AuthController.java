package ru.LevLezhnin.NauJava.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.LevLezhnin.NauJava.dto.auth.JwtLoginRequestDto;
import ru.LevLezhnin.NauJava.dto.auth.JwtRefreshRequestDto;
import ru.LevLezhnin.NauJava.dto.auth.JwtResponseDto;
import ru.LevLezhnin.NauJava.dto.auth.RegistrationRequestDto;
import ru.LevLezhnin.NauJava.security.JwtProperties;
import ru.LevLezhnin.NauJava.service.interfaces.AuthService;
import ru.LevLezhnin.NauJava.utils.TokenCookieService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final TokenCookieService tokenCookieService;

    public AuthController(AuthService authService, JwtProperties jwtProperties, TokenCookieService tokenCookieService) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
        this.tokenCookieService = tokenCookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @RequestBody RegistrationRequestDto request,
            HttpServletResponse response) {

        JwtResponseDto tokens = authService.register(request);
        setTokenCookies(response, tokens);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @RequestBody JwtLoginRequestDto request,
            HttpServletResponse response) {

        JwtResponseDto tokens = authService.login(request);
        setTokenCookies(response, tokens);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = tokenCookieService.getTokenFromCookie(request, JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME);

        JwtResponseDto tokens = authService.refresh(new JwtRefreshRequestDto(refreshToken));
        setTokenCookies(response, tokens);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        tokenCookieService.clearCookie(response, JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);
        tokenCookieService.clearCookie(response, JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME);

        SecurityContextHolder.clearContext();

        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            httpSession.invalidate();
        }

        return ResponseEntity.ok().build();
    }

    private void setTokenCookies(HttpServletResponse response, JwtResponseDto tokens) {
        tokenCookieService.setTokenCookie(response, JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, tokens.accessToken(), jwtProperties.getAccessTokenLifetime().getSeconds());
        tokenCookieService.setTokenCookie(response, JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME, tokens.refreshToken(), jwtProperties.getRefreshTokenLifetime().getSeconds());
    }
}

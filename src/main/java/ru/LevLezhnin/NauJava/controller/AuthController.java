package ru.LevLezhnin.NauJava.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.LevLezhnin.NauJava.dto.auth.*;
import ru.LevLezhnin.NauJava.security.properties.JwtProperties;
import ru.LevLezhnin.NauJava.service.interfaces.AuthService;
import ru.LevLezhnin.NauJava.utils.TokenCookieService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenCookieService tokenCookieService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, TokenCookieService tokenCookieService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.tokenCookieService = tokenCookieService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @RequestBody @Valid RegistrationRequestDto request,
            HttpServletResponse response) {

        JwtResponseDto tokens = authService.register(request);
        setTokenCookies(response, tokens);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @RequestBody @Valid JwtLoginRequestDto request,
            HttpServletResponse response) {

        JwtResponseDto tokens = authService.login(request);
        setTokenCookies(response, tokens);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = tokenCookieService.getTokenFromCookie(request, JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME);

        JwtResponseDto tokens = authService.refresh(new JwtRefreshRequestDto(refreshToken));

        tokenCookieService.setTokenCookie(response, JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, tokens.accessToken(), jwtProperties.getAccessTokenLifetime().getSeconds());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = tokenCookieService.getTokenFromCookie(request, JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);
        String refreshToken = tokenCookieService.getTokenFromCookie(request, JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME);

        authService.logout(new JwtLogoutRequestDto(accessToken, refreshToken));
        clearSession(request, response);

        return ResponseEntity.ok().build();
    }

    private void setTokenCookies(HttpServletResponse response, JwtResponseDto tokens) {
        tokenCookieService.setTokenCookie(response, JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, tokens.accessToken(), jwtProperties.getAccessTokenLifetime().getSeconds());
        tokenCookieService.setTokenCookie(response, JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME, tokens.refreshToken(), jwtProperties.getRefreshTokenLifetime().getSeconds());
    }

    private void clearSession(HttpServletRequest request, HttpServletResponse response) {
        tokenCookieService.clearCookie(response, JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);
        tokenCookieService.clearCookie(response, JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME);

        SecurityContextHolder.clearContext();

        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            httpSession.invalidate();
        }
    }
}

package ru.LevLezhnin.NauJava.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.LevLezhnin.NauJava.dto.auth.*;
import ru.LevLezhnin.NauJava.dto.error.ErrorResponse;
import ru.LevLezhnin.NauJava.dto.error.ValidationError;
import ru.LevLezhnin.NauJava.security.context.RequestContextService;
import ru.LevLezhnin.NauJava.security.properties.JwtProperties;
import ru.LevLezhnin.NauJava.security.utils.TokenCookieService;
import ru.LevLezhnin.NauJava.service.interfaces.AuthService;

/**
 * REST-контроллер аутентификации (регистрация, логин, refresh, logout).
 * <p>
 * Работает с JWT-токенами, хранящимися в httpOnly cookies.
 *
 * @author Лев Лежнин
 */
@Tag(name = "Authentication", description = "Регистрация, вход, обновление токенов и выход из системы")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final TokenCookieService tokenCookieService;
    private final JwtProperties jwtProperties;
    private final RequestContextService requestContextService;

    @Autowired
    public AuthController(AuthService authService, TokenCookieService tokenCookieService, JwtProperties jwtProperties, RequestContextService requestContextService) {
        this.authService = authService;
        this.tokenCookieService = tokenCookieService;
        this.jwtProperties = jwtProperties;
        this.requestContextService = requestContextService;
    }

    /**
     * Регистрация нового пользователя в системе.
     * <p>
     * Создаёт учётную запись, генерирует пару JWT-токенов и устанавливает их в httpOnly cookies.
     */
    @Operation(
        summary = "Регистрация нового пользователя",
        description = "Создаёт пользователя и сразу выдаёт access + refresh токены в httpOnly cookies. " +
                      "После успешной регистрации пользователь считается аутентифицированным."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Регистрация прошла успешно. Токены установлены в cookies",
                     content = @Content),
        @ApiResponse(responseCode = "400",
                     description = "Ошибка валидации входных данных",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ValidationError.class))),
        @ApiResponse(responseCode = "409",
                     description = "Пользователь с таким логином или email уже существует",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500",
                     description = "Внутренняя ошибка сервера",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @RequestBody @Valid RegistrationRequestDto request,
            HttpServletResponse response) {

        log.info("Запрошена регистрация нового пользователя. Логин: {}", request.username());

        JwtResponseDto tokens = authService.register(request);
        setTokenCookies(response, tokens);

        return ResponseEntity.ok().build();
    }

    /**
     * Аутентификация пользователя по логину и паролю.
     * <p>
     * При успешной проверке credentials генерирует новую пару access + refresh токенов.
     */
    @Operation(
        summary = "Вход в систему (login)",
        description = "Аутентифицирует пользователя и выдаёт JWT-токены в httpOnly cookies."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Успешная аутентификация. Токены установлены",
                     content = @Content),
        @ApiResponse(responseCode = "400",
                     description = "Ошибка валидации запроса",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ValidationError.class))),
        @ApiResponse(responseCode = "401",
                     description = "Неверный логин или пароль",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500",
                     description = "Внутренняя ошибка сервера",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @RequestBody @Valid JwtLoginRequestDto request,
            HttpServletResponse response) {

        JwtResponseDto tokens = authService.login(request);
        setTokenCookies(response, tokens);

        log.info("Успешный вход пользователя. Логин: {}", request.username());

        return ResponseEntity.ok().build();
    }

    /**
     * Обновление access-токена по refresh-токену.
     * <p>
     * Refresh-токен остаётся прежним. Access-токен обновляется.
     * Используется для продления сессии без повторного ввода пароля.
     */
    @Operation(
        summary = "Обновление access-токена",
        description = "Позволяет получить новый access-токен, используя валидный refresh-токен из cookies."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Токен успешно обновлён",
                     content = @Content),
        @ApiResponse(responseCode = "400",
                     description = "Некорректный refresh token в теле",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401",
                     description = "Refresh-токен недействителен, просрочен или отозван",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500",
                     description = "Внутренняя ошибка сервера",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = tokenCookieService.getTokenFromCookie(request, JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME);

        JwtResponseDto tokens = authService.refresh(new JwtRefreshRequestDto(refreshToken));

        tokenCookieService.setTokenCookie(response, JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, tokens.accessToken(), jwtProperties.getAccessTokenLifetime().getSeconds());

        try {
            log.debug("Успешно обновлён токен пользователя. ID пользователя: {}", requestContextService.getUserId());
        } catch (AuthenticationCredentialsNotFoundException e) {
            log.debug("Успешно обновлён токен пользователя. ID пользователя недоступен в текущем контексте.");
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Выход пользователя из системы (logout).
     * <p>
     * Отзывает (blacklist) текущие access и refresh токены.
     * Также очищает SecurityContext и HTTP-сессию.
     */
    @Operation(
        summary = "Выход из системы",
        description = "Отзывает текущие JWT-токены и завершает сессию пользователя."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Успешный выход. Токены отозваны",
                     content = @Content),
        @ApiResponse(responseCode = "401",
                     description = "Токен недействителен",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500",
                     description = "Внутренняя ошибка сервера",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = tokenCookieService.getTokenFromCookie(request, JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);
        String refreshToken = tokenCookieService.getTokenFromCookie(request, JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME);

        Long exitingUserId = null;
        try {
            exitingUserId = requestContextService.getUserId();
        } catch (AuthenticationCredentialsNotFoundException e) {
            log.debug("Logout запрошен без валидной аутентификации (токены отсутствуют или невалидны)");
        }

        log.info("Запрошен выход из системы. ID пользователя: {}", exitingUserId != null ? exitingUserId : "неизвестен");

        authService.logout(new JwtLogoutRequestDto(accessToken, refreshToken));
        clearSession(request, response);

        log.info("Успешный выход пользователя из системы. ID пользователя: {}", exitingUserId);

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

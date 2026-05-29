package ru.LevLezhnin.NauJava.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ru.LevLezhnin.NauJava.dto.error.ErrorResponse;
import ru.LevLezhnin.NauJava.dto.error.ValidationError;
import ru.LevLezhnin.NauJava.dto.user.UpdateUserRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UserProfileResponseDto;
import ru.LevLezhnin.NauJava.security.context.RequestContextService;
import ru.LevLezhnin.NauJava.service.interfaces.UserService;

/**
 * REST-контроллер для управления профилем текущего пользователя (API v1).
 *
 * @author Лев Лежнин
 */
@Tag(name = "User Profile", description = "Операции с профилем текущего аутентифицированного пользователя")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final RequestContextService requestContextService;

    @Autowired
    public UserController(UserService userService, RequestContextService requestContextService) {
        this.userService = userService;
        this.requestContextService = requestContextService;
    }

    /**
     * Получение профиля текущего пользователя.
     */
    @Operation(summary = "Мой профиль", description = "Возвращает данные текущего аутентифицированного пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Профиль успешно возвращён",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserProfileResponseDto.class))),
        @ApiResponse(responseCode = "401",
                     description = "Не аутентифицирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                     description = "Доступ запрещён (бан)",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404",
                     description = "Пользователь не найден (EntityNotFoundException)",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500",
                     description = "Внутренняя ошибка сервера",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public UserProfileResponseDto getProfile() {
        log.debug("Запрошен профиль пользователя. ID пользователя: {}", requestContextService.getUserId());
        return userService.getProfile();
    }

    /**
     * Частичное обновление профиля (смена логина и/или пароля).
     */
    @Operation(summary = "Обновить профиль", description = "Позволяет изменить username и/или пароль. При смене пароля требуется текущий пароль.")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Профиль успешно обновлён",
                     content = @Content),
        @ApiResponse(responseCode = "400",
                     description = "Неверный текущий пароль или попытка установить такой же новый, ошибка валидации, некорректный JSON",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(anyOf = {ValidationError.class, ErrorResponse.class}))),
        @ApiResponse(responseCode = "401",
                     description = "Не аутентифицирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                     description = "Доступ запрещён (бан)",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404",
                     description = "Пользователь не найден",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409",
                     description = "Новый username уже занят",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500",
                     description = "Внутренняя ошибка сервера",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/me")
    public void updateProfile(@RequestBody @NotNull @Valid UpdateUserRequestDto updateUserRequestDto) {
        userService.updateUser(updateUserRequestDto);
        log.debug("Профиль пользователя успешно обновлён. ID пользователя: {}", requestContextService.getUserId());
    }

    /**
     * Удаление своего аккаунта.
     * <p>
     * Внимание: операция необратимая.
     */
    @Operation(summary = "Удалить аккаунт", description = "Полностью удаляет профиль пользователя и все связанные данные.")
    @ApiResponses({
        @ApiResponse(responseCode = "204",
                     description = "Аккаунт успешно удалён",
                     content = @Content),
        @ApiResponse(responseCode = "401",
                     description = "Не аутентифицирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                     description = "Доступ запрещён (бан)",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404",
                     description = "Пользователь не найден",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500",
                     description = "Внутренняя ошибка сервера",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser() {
        log.warn("Запрошено удаление аккаунта пользователя. ID пользователя: {}", requestContextService.getUserId());
        userService.deleteUser();
    }
}

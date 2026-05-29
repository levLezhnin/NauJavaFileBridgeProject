package ru.LevLezhnin.NauJava.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ru.LevLezhnin.NauJava.dto.error.ErrorResponse;
import ru.LevLezhnin.NauJava.dto.error.ValidationError;
import ru.LevLezhnin.NauJava.dto.user.UserBanRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UserBanResponseDto;
import ru.LevLezhnin.NauJava.security.context.RequestContextService;
import ru.LevLezhnin.NauJava.service.interfaces.UserBanService;

import java.util.List;

/**
 * Административный REST-контроллер для управления банами пользователей.
 * <p>
 * Все эндпоинты требуют роль {@link ru.LevLezhnin.NauJava.model.UserRole#ADMIN}.
 * Реализует:
 * <ul>
 *   <li>Блокировку и разблокировку пользователей</li>
 *   <li>Получение активного/конкретного бана</li>
 *   <li>Историю банов пользователя и выданных админом (с пагинацией)</li>
 * </ul>
 * <p>
 * Защита от self-action и бана админов реализована на уровне сервиса.
 *
 * @author Лев Лежнин
 * @see UserBanService
 */
@Tag(name = "Admin - Bans", description = "Управление блокировками пользователей (бан/разбан, история)")
@RestController
@RequestMapping("/api/v1/admin/users")
public class UserBanController {

    private static final Logger log = LoggerFactory.getLogger(UserBanController.class);

    private final UserBanService userBanService;
    private final RequestContextService requestContextService;

    @Autowired
    public UserBanController(UserBanService userBanService, RequestContextService requestContextService) {
        this.userBanService = userBanService;
        this.requestContextService = requestContextService;
    }

    /**
     * Получение информации о конкретной блокировке по её ID.
     * <p>
     * Возвращает полные данные о бане (кто выдал, кому, причина, даты).
     */
    @Operation(summary = "Получить блокировку по ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Блокировка найдена",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserBanResponseDto.class))),
        @ApiResponse(responseCode = "401",
                     description = "Не аутентифицирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                     description = "Нет прав администратора или аккаунт заблокирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404",
                     description = "Блокировка не найдена",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/bans/{banId}")
    public UserBanResponseDto getBanById(@PathVariable("banId") Long banId) {
        log.debug("Запрос блокировки по ID. ID блокировки: {}, ID администратора, запросившего блокировку: {}",
                banId, requestContextService.getUserId());
        return userBanService.getUserBanById(banId);
    }

    /**
     * Получение активной блокировки пользователя.
     * <p>
     * Если у пользователя нет активного бана - возвращается 404.
     */
    @Operation(summary = "Активная блокировка пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Активная блокировка найдена",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserBanResponseDto.class))),
        @ApiResponse(responseCode = "401",
                     description = "Не аутентифицирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                     description = "Нет прав администратора или аккаунт заблокирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404",
                     description = "Пользователь не заблокирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/ban/{userId}")
    public UserBanResponseDto getActiveUserBanByUserId(@PathVariable("userId") Long bannedUserId) {
        log.debug("Запрос активной блокировки по ID пользователя. ID пользователя: {}, ID администратора, запросившего блокировку: {}",
                bannedUserId, requestContextService.getUserId());
        return userBanService.getActiveUserBanByUserId(bannedUserId);
    }

    /**
     * История блокировок, выданных конкретным администратором.
     * <p>
     * Пагинированный список всех банов (активных и снятых), выданных указанным админом.
     */
    @Operation(summary = "Блокировки, выданные администратором")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "История выданных блокировок",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = UserBanResponseDto.class)))),
        @ApiResponse(responseCode = "400",
                     description = "Целевой пользователь не является администратором",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401",
                     description = "Не аутентифицирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                     description = "Нет прав администратора или аккаунт заблокирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404",
                     description = "Администратор не найден",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/ban/issuedBans/{adminUserId}")
    public List<UserBanResponseDto> getBansIssuedByAdmin(@PathVariable("adminUserId") Long adminId,
                                                           @RequestParam("page") int page,
                                                           @RequestParam("page_size") int pageSize) {
         log.debug("Запрос выданных блокировок по ID администратора. ID целевого администратора: {}, ID администратора, запросившего блокировку: {}",
                 adminId, requestContextService.getUserId());
         return userBanService.getIssuedBansByAdmin(adminId, page, pageSize);
    }

    /**
     * История блокировок конкретного пользователя.
     * <p>
     * Включает как активные, так и завершённые (снятые) блокировки.
     * Пагинация поддерживается.
     */
    @Operation(summary = "История блокировок пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "История блокировок пользователя",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = UserBanResponseDto.class)))),
        @ApiResponse(responseCode = "401",
                     description = "Не аутентифицирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                     description = "Нет прав администратора или аккаунт заблокирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404",
                     description = "Пользователь не найден",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/ban/history/{userId}")
    public List<UserBanResponseDto> getUserBanHistory(@PathVariable("userId") Long userId,
                                                      @RequestParam("page") int page,
                                                      @RequestParam("page_size") int pageSize) {
        log.debug("Запрос истории полученных блокировок по ID пользователя. ID пользователя: {}, ID администратора, запросившего историю блокировок: {}",
                userId, requestContextService.getUserId());
        return userBanService.getUserBanHistory(userId, page, pageSize);
    }

    /**
     * Блокировка пользователя по ID пользователя.
     */
    @Operation(summary = "Заблокировать пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Пользователь успешно заблокирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserBanResponseDto.class))),
        @ApiResponse(responseCode = "400",
                     description = "Ошибка валидации",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ValidationError.class))),
        @ApiResponse(responseCode = "401",
                     description = "Не аутентифицирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                     description = "Попытка заблокировать себя или другого администратора или бан на текущем",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404",
                     description = "Пользователь не найден",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409",
                     description = "Пользователь уже заблокирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/ban")
    public UserBanResponseDto banUserById(@RequestBody @Valid UserBanRequestDto userBanRequestDto) {
        log.info("Администратор {} инициировал блокировку пользователя: targetUserId={}, reason={}",
                requestContextService.getUserId(), userBanRequestDto.banUserId(), userBanRequestDto.reason());
        return userBanService.banUserById(userBanRequestDto.banUserId(), userBanRequestDto.reason());
    }

    /**
     * Снятие блокировки с пользователя.
     */
    @Operation(summary = "Разблокировать пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Пользователь успешно разблокирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserBanResponseDto.class))),
        @ApiResponse(responseCode = "401",
                     description = "Не аутентифицирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                     description = "Нет прав администратора, попытка разблокировать себя или аккаунт заблокирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404",
                     description = "Пользователь не найден или не заблокирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/unban/{userId}")
    public UserBanResponseDto unbanUserById(@PathVariable("userId") Long bannedUserId) {
        log.info("Администратор {} инициировал снятие блокировки с пользователя: targetUserId={}",
                requestContextService.getUserId(), bannedUserId);
        return userBanService.unbanUserById(bannedUserId);
    }
}

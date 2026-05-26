package ru.LevLezhnin.NauJava.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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
     *
     * @param banId идентификатор записи о блокировке
     * @return DTO с информацией о бане
     * @throws ru.LevLezhnin.NauJava.exception.common.EntityNotFoundException если бан не найден (404)
     * @throws org.springframework.security.access.AccessDeniedException если нет прав ADMIN (403)
     */
    @Operation(summary = "Получить блокировку по ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Блокировка найдена"),
        @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        @ApiResponse(responseCode = "403", description = "Нет прав администратора или аккаунт заблокирован"),
        @ApiResponse(responseCode = "404", description = "Блокировка не найдена (EntityNotFoundException)")
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
     * Используется для проверки перед действиями над пользователем.
     *
     * @param bannedUserId ID пользователя, чей активный бан запрашивается
     * @return DTO активного бана (если есть)
     * @throws ru.LevLezhnin.NauJava.exception.common.EntityNotFoundException если пользователь не заблокирован (404)
     * @throws org.springframework.security.access.AccessDeniedException если нет прав ADMIN (403)
     */
    @Operation(summary = "Активная блокировка пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Активная блокировка найдена"),
        @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        @ApiResponse(responseCode = "403", description = "Нет прав администратора или аккаунт заблокирован"),
        @ApiResponse(responseCode = "404", description = "Пользователь не заблокирован (EntityNotFoundException в advice)")
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
     * Если целевой adminId не имеет роль ADMIN - 400 (IllegalArgumentException).
     *
     * @param adminId  ID администратора, чьи выданные баны запрашиваются
     * @param page     номер страницы (0-based)
     * @param pageSize записей на странице
     * @return список DTO банов
     * @throws IllegalArgumentException если целевой пользователь не ADMIN (400)
     * @throws ru.LevLezhnin.NauJava.exception.common.EntityNotFoundException если админ не найден (404)
     * @throws org.springframework.security.access.AccessDeniedException если текущий не ADMIN (403)
     */
    @Operation(summary = "Блокировки, выданные администратором")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "История выданных блокировок"),
        @ApiResponse(responseCode = "400", description = "Целевой пользователь не является администратором (IllegalArgumentException)"),
        @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        @ApiResponse(responseCode = "403", description = "Нет прав администратора или аккаунт заблокирован"),
        @ApiResponse(responseCode = "404", description = "Администратор не найден (EntityNotFoundException)")
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
     *
     * @param userId   ID пользователя, чья история банов запрашивается
     * @param page     номер страницы
     * @param pageSize размер страницы
     * @return список DTO истории банов (в хронологическом порядке)
     * @throws ru.LevLezhnin.NauJava.exception.common.EntityNotFoundException если пользователь не найден (404)
     * @throws org.springframework.security.access.AccessDeniedException если нет прав ADMIN (403)
     */
    @Operation(summary = "История блокировок пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "История блокировок пользователя"),
        @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        @ApiResponse(responseCode = "403", description = "Нет прав администратора или аккаунт заблокирован"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден (EntityNotFoundException)")
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
     * Блокировка пользователя.
     * <p>
     * <b>Бизнес-правила (строгие):</b>
     * <ul>
     *   <li>Нельзя заблокировать самого себя → 403 (SelfActionForbiddenException)</li>
     *   <li>Нельзя заблокировать другого администратора → 403 (AccessDeniedException)</li>
     *   <li>Если уже заблокирован → 409 (UserAlreadyBannedException)</li>
     * </ul>
     * Использует pessimistic lock. После бана пользователь не может выполнять аутентифицированные действия (UserBanFilter).
     *
     * @param userBanRequestDto DTO с ID пользователя и причиной блокировки (обязательна)
     * @return DTO созданной блокировки
     * @throws jakarta.validation.ConstraintViolationException / MethodArgumentNotValidException при невалидных данных (400)
     * @throws ru.LevLezhnin.NauJava.exception.common.SelfActionForbiddenException при self-ban (403)
     * @throws org.springframework.security.access.AccessDeniedException при попытке бана админа (403)
     * @throws ru.LevLezhnin.NauJava.exception.common.EntityNotFoundException если пользователь не найден (404)
     * @throws ru.LevLezhnin.NauJava.exception.user.UserAlreadyBannedException если уже заблокирован (409)
     */
    @Operation(summary = "Заблокировать пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Пользователь успешно заблокирован"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации (MethodArgumentNotValid)"),
        @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        @ApiResponse(responseCode = "403", description = "Попытка заблокировать себя (SelfActionForbiddenException) или другого администратора (AccessDeniedException) или бан на текущем"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден (EntityNotFoundException)"),
        @ApiResponse(responseCode = "409", description = "Пользователь уже заблокирован (UserAlreadyBannedException)")
    })
    @PostMapping("/ban")
    public UserBanResponseDto banUserById(@RequestBody @Valid UserBanRequestDto userBanRequestDto) {
        log.info("Администратор {} инициировал блокировку пользователя: targetUserId={}, reason={}",
                requestContextService.getUserId(), userBanRequestDto.banUserId(), userBanRequestDto.reason());
        return userBanService.banUserById(userBanRequestDto.banUserId(), userBanRequestDto.reason());
    }

    /**
     * Снятие блокировки с пользователя.
     * <p>
     * Аналогичные проверки, что и в {@link #banUserById(UserBanRequestDto)}:
     * self-forbidden, проверка ADMIN, pessimistic lock.
     * Устанавливает {@code unbannedAt} и возвращает пользователю возможность входа.
     *
     * @param bannedUserId ID пользователя для разблокировки
     * @return DTO бана с обновлённым временем снятия
     * @throws ru.LevLezhnin.NauJava.exception.common.SelfActionForbiddenException при попытке разбанить себя (403)
     * @throws org.springframework.security.access.AccessDeniedException если нет прав (403)
     * @throws ru.LevLezhnin.NauJava.exception.common.EntityNotFoundException если пользователь не найден (404)
     * @throws ru.LevLezhnin.NauJava.exception.user.UserNotBannedException если пользователь не заблокирован (404)
     */
    @Operation(summary = "Разблокировать пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Пользователь успешно разблокирован"),
        @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        @ApiResponse(responseCode = "403", description = "Нет прав администратора, попытка разблокировать себя (SelfActionForbiddenException) или аккаунт заблокирован"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден (EntityNotFoundException) или не заблокирован (UserNotBannedException)")
    })
    @PostMapping("/unban/{userId}")
    public UserBanResponseDto unbanUserById(@PathVariable("userId") Long bannedUserId) {
        log.info("Администратор {} инициировал снятие блокировки с пользователя: targetUserId={}",
                requestContextService.getUserId(), bannedUserId);
        return userBanService.unbanUserById(bannedUserId);
    }
}

package ru.LevLezhnin.NauJava.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.LevLezhnin.NauJava.dto.error.ErrorResponse;
import ru.LevLezhnin.NauJava.dto.user.UserProfileAdminResponseDto;
import ru.LevLezhnin.NauJava.security.context.RequestContextService;
import ru.LevLezhnin.NauJava.service.interfaces.UserService;

import java.util.List;

/**
 * Административный REST-контроллер для поиска и просмотра пользователей.
 * <p>
 * Доступен только пользователям с ролью {@link ru.LevLezhnin.NauJava.model.UserRole#ADMIN}.
 * Предоставляет поиск пользователей по username, email, роли и другим критериям с пагинацией.
 * Использует стратегии поиска из {@link ru.LevLezhnin.NauJava.repository.search.user.UserSearchStrategy}.
 *
 * @author Лев Лежнин
 * @see UserService#findByCriteria(String, String, int, int)
 */
@Tag(name = "Admin - Users", description = "Административный поиск и просмотр пользователей")
@RestController
@RequestMapping("/api/v1/admin/users")
public class UserAdminController {

    private static final Logger log = LoggerFactory.getLogger(UserAdminController.class);

    private final UserService userService;
    private final RequestContextService requestContextService;

    @Autowired
    public UserAdminController(UserService userService, RequestContextService requestContextService) {
        this.userService = userService;
        this.requestContextService = requestContextService;
    }

    /**
     * Административный поиск пользователей по различным критериям.
     * <p>
     * Поддерживаемые критерии см. в реализациях {@link ru.LevLezhnin.NauJava.repository.search.user.UserSearchStrategy}.
     */
    @Operation(summary = "Поиск пользователей (админ)", description = "Позволяет администратору искать пользователей по username, email, роли и другим критериям.")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Результаты поиска",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = UserProfileAdminResponseDto.class)))),
        @ApiResponse(responseCode = "400",
                     description = "Неверный критерий поиска или параметры",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401",
                     description = "Не аутентифицирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                     description = "Нет прав администратора или аккаунт заблокирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404",
                     description = "Администратор не найден",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500",
                     description = "Внутренняя ошибка сервера",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public List<UserProfileAdminResponseDto> findAllByCriteria(@RequestParam("searchBy") String searchBy,
                                                               @RequestParam("search") String search,
                                                               @RequestParam("page_size") int pageSize,
                                                               @RequestParam("page") int page) {
         log.info("Администратор {} выполнил поиск пользователей. searchBy: {}, Страница: {}, Записей на странице: {}",
                 requestContextService.getUserId(), searchBy, page, pageSize);
         return userService.findByCriteria(searchBy, search, page, pageSize);
    }
}

package ru.LevLezhnin.NauJava.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.LevLezhnin.NauJava.dto.file.FileAdminResponseDto;
import ru.LevLezhnin.NauJava.security.context.RequestContextService;
import ru.LevLezhnin.NauJava.service.interfaces.FileService;

import java.util.List;

/**
 * Административный REST-контроллер для просмотра всех файлов системы.
 * <p>
 * Доступен только пользователям с ролью {@link ru.LevLezhnin.NauJava.model.UserRole#ADMIN}.
 * Предоставляет поиск файлов по различным критериям (автор, имя файла и др.) с пагинацией.
 * Использует стратегии поиска из {@link ru.LevLezhnin.NauJava.repository.search.file.FileSearchStrategy}.
 *
 * @author Лев Лежнин
 * @see FileService#getAllFiles(String, String, int, int)
 */
@Tag(name = "Admin - Files", description = "Административные операции с файлами")
@RestController
@RequestMapping("/api/v1/admin/files")
public class FileAdminController {

    private static final Logger log = LoggerFactory.getLogger(FileAdminController.class);
    private final FileService fileService;
    private final RequestContextService requestContextService;

    @Autowired
    public FileAdminController(FileService fileService, RequestContextService requestContextService) {
        this.fileService = fileService;
        this.requestContextService = requestContextService;
    }

    /**
     * Административный поиск всех файлов в системе с поддержкой различных критериев.
     * <p>
     * <b>Требования:</b> текущий пользователь должен иметь роль ADMIN.
     * При неизвестном {@code searchBy} - возвращается 400 (InvalidSearchCriteriaException).
     * <p>
     * Поддерживаемые критерии см. в реализациях {@link ru.LevLezhnin.NauJava.repository.search.file.FileSearchStrategy}.
     *
     * @param searchBy ключ стратегии поиска (например: "authorId", "fileName", "mimeType")
     * @param search   значение для поиска (зависит от критерия)
     * @param pageSize количество записей на странице (должно быть > 0)
     * @param page     номер страницы (0-based)
     * @return список DTO с расширенной информацией о файлах (включая автора)
     * @throws org.springframework.security.access.AccessDeniedException если не ADMIN
     * @throws ru.LevLezhnin.NauJava.exception.common.EntityNotFoundException если администратор не найден
     * @throws ru.LevLezhnin.NauJava.exception.common.InvalidSearchCriteriaException при неизвестном searchBy
     */
    @Operation(summary = "Поиск файлов (админ)", description = "Позволяет администратору искать файлы по автору, имени и другим критериям")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Результаты поиска"),
        @ApiResponse(responseCode = "400", description = "Неверный критерий поиска (InvalidSearchCriteriaException) или параметры пагинации"),
        @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        @ApiResponse(responseCode = "403", description = "Нет прав администратора (AccessDeniedException) или аккаунт заблокирован"),
        @ApiResponse(responseCode = "404", description = "Администратор не найден (EntityNotFoundException)"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping
    public List<FileAdminResponseDto> getAllFiles(@RequestParam("searchBy") String searchBy,
                                                  @RequestParam("search") String search,
                                                  @RequestParam("page_size") int pageSize,
                                                  @RequestParam("page") int page) {
        log.info("Администратор {} выполнил поиск файлов. searchBy: {}, Страница: {}, Записей на странице: {}",
                requestContextService.getUserId(), searchBy, page, pageSize);
        return fileService.getAllFiles(searchBy, search, page, pageSize);
    }
}

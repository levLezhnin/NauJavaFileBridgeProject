package ru.LevLezhnin.NauJava.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.LevLezhnin.NauJava.dto.storageQuotas.StorageQuotaResponseDto;
import ru.LevLezhnin.NauJava.security.context.RequestContextService;
import ru.LevLezhnin.NauJava.service.interfaces.StorageQuotaService;

/**
 * REST-контроллер для получения информации о квоте хранения текущего пользователя.
 *
 * @author Лев Лежнин
 */
@Tag(name = "Storage Quota", description = "Информация о квоте хранения пользователя")
@RestController
@RequestMapping("/api/v1/quotas")
public class StorageQuotaController {

    private static final Logger log = LoggerFactory.getLogger(StorageQuotaController.class);
    private final StorageQuotaService storageQuotaService;
    private final RequestContextService requestContextService;

    @Autowired
    public StorageQuotaController(StorageQuotaService storageQuotaService, RequestContextService requestContextService) {
        this.storageQuotaService = storageQuotaService;
        this.requestContextService = requestContextService;
    }

    /**
     * Получение текущей квоты хранения пользователя.
     */
    @Operation(summary = "Моя квота хранилища", description = "Возвращает использованный и максимальный объём хранилища текущего пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Квота успешно возвращена"),
        @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещён (пользователь заблокирован)"),
        @ApiResponse(responseCode = "404", description = "Квота не найдена (EntityNotFoundException)"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/my")
    public StorageQuotaResponseDto getCurrentUserStorageQuota() {
        log.debug("Запрос квоты хранилища. ID пользователя: {}", requestContextService.getUserId());
        return storageQuotaService.getCurrentUserStorageQuota();
    }
}

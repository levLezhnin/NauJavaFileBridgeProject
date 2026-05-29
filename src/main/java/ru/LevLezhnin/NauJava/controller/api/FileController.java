package ru.LevLezhnin.NauJava.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.LevLezhnin.NauJava.dto.error.ErrorResponse;
import ru.LevLezhnin.NauJava.dto.error.ValidationError;
import ru.LevLezhnin.NauJava.dto.file.*;
import ru.LevLezhnin.NauJava.exception.file.FileUploadException;
import ru.LevLezhnin.NauJava.security.context.RequestContextService;
import ru.LevLezhnin.NauJava.service.interfaces.FileService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * REST-контроллер для работы с файлами (API v1).
 * <p>
 * Предоставляет эндпоинты для загрузки, списка, скачивания и удаления файлов.
 * Использует JWT-аутентификацию.
 *
 * @author Лев Лежнин
 * @see FileService
 */
@Tag(name = "Files", description = "Операции с пользовательскими файлами (загрузка, скачивание, удаление)")
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;
    private final RequestContextService requestContextService;
    private final Validator validator;

    @Autowired
    public FileController(FileService fileService, RequestContextService requestContextService, Validator validator) {
        this.fileService = fileService;
        this.requestContextService = requestContextService;
        this.validator = validator;
    }

    /**
     * Загрузка нового файла.
     * <p>
     * Поддерживает multipart/form-data. Метаданные передаются в части "payload", сам файл - в части "file".
     */
    @Operation(
        summary = "Загрузить файл",
        description = "Загружает файл в объектное хранилище с указанными ограничениями по времени жизни и количеству скачиваний."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201",
                     description = "Файл успешно загружен",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FileResponseDto.class))),
        @ApiResponse(responseCode = "400",
                     description = "Ошибка валидации, некорректный JSON, слишком большой файл",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(anyOf = { ErrorResponse.class, ValidationError.class }))),
        @ApiResponse(responseCode = "401",
                     description = "Пользователь не аутентифицирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                     description = "Доступ запрещён",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404",
                     description = "Пользователь не найден",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "422",
                     description = "Превышена квота хранилища",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500",
                     description = "Внутренняя ошибка сервера",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileResponseDto uploadFile(@RequestPart("file") MultipartFile multipartFile,
                                      @Valid @RequestPart("payload") FileUploadWebRequestPayloadDto fileUploadWebRequestPayloadDto) {
        FileUploadRequestDto fileUploadRequestDto = new FileUploadRequestDto(
                multipartFile.getOriginalFilename(),
                multipartFile.getContentType(),
                multipartFile.getSize(),
                fileUploadWebRequestPayloadDto.ttlMinutes(),
                fileUploadWebRequestPayloadDto.maxDownloads(),
                fileUploadWebRequestPayloadDto.password());

        Set<ConstraintViolation<FileUploadRequestDto>> violations = validator.validate(fileUploadRequestDto);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        try (InputStream fileDataInputStream = multipartFile.getInputStream()) {
            FileResponseDto fileResponseDto = fileService.uploadFile(
                    fileUploadRequestDto,
                    fileDataInputStream);

            log.info("Файл успешно загружен. ID пользователя: {}, Имя файла: {}, Размер файла: {}",
                    requestContextService.getUserId(), fileUploadRequestDto.fileName(), fileUploadRequestDto.fileSize());

            return fileResponseDto;
        } catch (IOException e) {
            log.error("Не удалось прочитать данные загружаемого файла", e);
            throw new FileUploadException("Ошибка чтения загруженного файла", e);
        }
    }

    /**
     * Получение списка своих файлов. Поддерживается пагинация.
     */
    @Operation(summary = "Мои файлы", description = "Возвращает постраничный список файлов текущего пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Список файлов успешно возвращён",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = FileResponseDto.class)))),
        @ApiResponse(responseCode = "400",
                     description = "Некорректные параметры пагинации",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401",
                     description = "Не аутентифицирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                     description = "Доступ запрещён (бан)",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500",
                     description = "Внутренняя ошибка сервера",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/my")
    @ResponseStatus(HttpStatus.OK)
    public List<FileResponseDto> getUserFiles(@RequestParam("page") int page,
                                              @RequestParam("page_size") int pageSize) {
        log.debug("Запрос файлов пользователя. ID пользователя: {}, Страница: {}, Записей на странице: {}",
                requestContextService.getUserId(), page, pageSize);
        return fileService.getCurrentUserFiles(page, pageSize);
    }

    /**
     * Получение метаданных файла по ID.
     */
    @Operation(summary = "Метаданные файла", description = "Возвращает информацию о файле (имя, размер, даты, количество скачиваний и т.д.)")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Метаданные файла успешно возвращены",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FileResponseDto.class))),
        @ApiResponse(responseCode = "401",
                     description = "Не аутентифицирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                     description = "Доступ запрещён (бан или нет прав)",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404",
                     description = "Файл не найден",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500",
                     description = "Внутренняя ошибка сервера",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{fileId}")
    @ResponseStatus(HttpStatus.OK)
    public FileResponseDto getFileDataById(@PathVariable String fileId) {
        log.debug("Запрос метаданных файла. ID пользователя: {}, ID файла: {}",
                requestContextService.getUserId(), fileId);
        return fileService.getById(fileId);
    }

    /**
     * Формирование ссылки на скачивание файла (только для владельца).
     */
    @Operation(summary = "Получить ссылку на скачивание", description = "Генерирует постоянную ссылку для скачивания. Доступно только владельцу файла.")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Ссылка успешно сформирована",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FileDownloadLinkResponseDto.class))),
        @ApiResponse(responseCode = "401",
                     description = "Не аутентифицирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                     description = "Нет прав или бан",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404",
                     description = "Файл не найден",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "410",
                     description = "Файл просрочен или исчерпан лимит",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500",
                     description = "Внутренняя ошибка сервера",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/link/{fileId}")
    @ResponseStatus(HttpStatus.OK)
    public FileDownloadLinkResponseDto getFileDownloadLinkById(@PathVariable String fileId) {
        log.debug("Запрос ссылки на скачивание файла. ID пользователя: {}, ID файла: {}",
                requestContextService.getUserId(), fileId);
        return fileService.formDownloadLinkPath(fileId);
    }

    /**
     * Скачивание файла (с проверкой пароля и лимитов).
     */
    @Operation(summary = "Скачать файл", description = "Выполняет скачивание файла. При наличии пароля - требуется его передача в теле запроса.")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Файл успешно скачан",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Resource.class))),
        @ApiResponse(responseCode = "400",
                     description = "Неверный пароль, некорректный JSON или запрос",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401",
                     description = "Не аутентифицирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                     description = "Нет прав на файл или бан аккаунта",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404",
                     description = "Файл не найден",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "410",
                     description = "Файл просрочен или исчерпан лимит скачиваний",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500",
                     description = "Внутренняя ошибка сервера",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/download")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Resource> downloadFileById(@RequestBody FileDownloadRequestDto fileDownloadRequestDto) {
        FileDownloadResponseDto fileDownloadResponseDto = fileService.downloadById(fileDownloadRequestDto);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(fileDownloadResponseDto.originalFilename(), StandardCharsets.UTF_8)
                .build();

        log.info("Файл скачан успешно. ID пользователя: {}, ID файла: {}", requestContextService.getUserId(), fileDownloadRequestDto.fileId());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileDownloadResponseDto.contentType()))
                .contentLength(fileDownloadResponseDto.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(new InputStreamResource(fileDownloadResponseDto.fileDataInputStream()));
    }

    /**
     * Удаление файла (владельцем или администратором).
     */
    @Operation(summary = "Удалить файл", description = "Удаляет файл из хранилища и возвращает квоту владельцу.")
    @ApiResponses({
        @ApiResponse(responseCode = "204",
                     description = "Файл успешно удалён",
                     content = @Content),
        @ApiResponse(responseCode = "401",
                     description = "Не аутентифицирован",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                     description = "Нет прав на удаление или бан",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404",
                     description = "Файл не найден или пользователь не найден",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500",
                     description = "Внутренняя ошибка сервера",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFileById(@PathVariable String fileId) {
        fileService.deleteById(fileId);
        log.info("Файл успешно удалён. ID пользователя: {}, ID файла: {}", requestContextService.getUserId(), fileId);
    }
}

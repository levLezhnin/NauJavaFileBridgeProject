package ru.LevLezhnin.NauJava.service.implementations;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.LevLezhnin.NauJava.dto.file.*;
import ru.LevLezhnin.NauJava.exception.common.EntityNotFoundException;
import ru.LevLezhnin.NauJava.exception.common.InvalidPasswordException;
import ru.LevLezhnin.NauJava.exception.common.InvalidSearchCriteriaException;
import ru.LevLezhnin.NauJava.exception.file.*;
import ru.LevLezhnin.NauJava.exception.storagequotas.StorageQuotaExceededException;
import ru.LevLezhnin.NauJava.mapper.Mapper;
import ru.LevLezhnin.NauJava.metrics.FileMetrics;
import ru.LevLezhnin.NauJava.model.*;
import ru.LevLezhnin.NauJava.repository.custom.ObjectStorageRepository;
import ru.LevLezhnin.NauJava.repository.jpa.FileRepository;
import ru.LevLezhnin.NauJava.repository.jpa.FileStatisticsRepository;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.repository.search.file.FileSearchStrategy;
import ru.LevLezhnin.NauJava.security.context.RequestContextService;
import ru.LevLezhnin.NauJava.service.interfaces.FileService;
import ru.LevLezhnin.NauJava.service.interfaces.StorageQuotaService;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    private static final String OBJECT_STORAGE_PATH_TEMPLATE = "/files/userId=%d/fileId=%s/data.bin";
    private static final String FILE_DOWNLOAD_LINK_PATH_TEMPLATE = "/download/%s";

    private final TransactionTemplate transactionTemplate;

    private final FileRepository fileRepository;
    private final FileStatisticsRepository fileStatisticsRepository;
    @Qualifier("fileStorageRepository")
    private final ObjectStorageRepository fileStorageRepository;
    private final UserRepository userRepository;

    private final StorageQuotaService storageQuotaService;
    private final RequestContextService requestContextService;
    private final PasswordEncoder passwordEncoder;

    private final Mapper<File, FileResponseDto> fileResponseDtoMapper;

    private final Map<String, FileSearchStrategy> fileSearchStrategyMap;

    private final FileMetrics fileMetrics;
    private final MeterRegistry meterRegistry;

    @Value("${app.api.base-url}")
    private String baseUrl;

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Autowired
    public FileServiceImpl(PlatformTransactionManager platformTransactionManager,
                           FileRepository fileRepository,
                           FileStatisticsRepository fileStatisticsRepository,
                           ObjectStorageRepository fileStorageRepository,
                           UserRepository userRepository,
                           StorageQuotaService storageQuotaService,
                           RequestContextService requestContextService,
                           PasswordEncoder passwordEncoder,
                           Mapper<File, FileResponseDto> fileResponseDtoMapper,
                           List<FileSearchStrategy> fileSearchStrategies,
                           FileMetrics fileMetrics,
                           MeterRegistry meterRegistry) {
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.userRepository = userRepository;
        this.storageQuotaService = storageQuotaService;
        this.fileRepository = fileRepository;
        this.fileStatisticsRepository = fileStatisticsRepository;
        this.fileStorageRepository = fileStorageRepository;
        this.requestContextService = requestContextService;
        this.passwordEncoder = passwordEncoder;
        this.fileResponseDtoMapper = fileResponseDtoMapper;
        this.fileSearchStrategyMap = fileSearchStrategies.stream().collect(Collectors.toMap(FileSearchStrategy::getCriteriaKey, s -> s));
        this.fileMetrics = fileMetrics;
        this.meterRegistry = meterRegistry;
    }

    private Long getCurrentUserId() {
        return requestContextService.getUserId();
    }

    private boolean isPasswordNotProvided(String incomingPassword) {
        return incomingPassword == null || incomingPassword.isBlank();
    }

    private boolean isValidId(String id) {
        if (id == null) {
            return false;
        }

        try {
            UUID.fromString(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public FileResponseDto uploadFile(FileUploadRequestDto fileUploadRequestDto, InputStream fileDataStream) {

        Timer.Sample uploadBeginSample = Timer.start(meterRegistry);

        Long authorId = getCurrentUserId();
        User author = userRepository.findWithDetailsById(authorId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id: %d не найден".formatted(authorId)));

        StorageQuota storageQuota = author.getStorageQuota();

        UUID fileId = UUID.randomUUID();
        String objectStorageFilePath = OBJECT_STORAGE_PATH_TEMPLATE.formatted(author.getId(), fileId.toString());
        Instant uploadedAt = Instant.now();
        Instant expireAt = uploadedAt.plus(Duration.ofMinutes(fileUploadRequestDto.ttlMinutes()));
        String passwordHash = isPasswordNotProvided(fileUploadRequestDto.password()) ? null : passwordEncoder.encode(fileUploadRequestDto.password());

        fileStorageRepository.uploadWithPath(
                objectStorageFilePath,
                fileDataStream,
                fileUploadRequestDto.fileSize(),
                MediaType.APPLICATION_OCTET_STREAM_VALUE
        );

        try {
            return transactionTemplate.execute(status -> {
                storageQuotaService.updateStorageQuota(storageQuota.getId(), fileUploadRequestDto.fileSize());

                FileStatistics fileStatistics = FileStatistics.builder()
                        .setTimesDownloaded(0L)
                        .setSizeBytes(fileUploadRequestDto.fileSize())
                        .build();

                File file = File.builder()
                        .setId(fileId)
                        .setPath(objectStorageFilePath)
                        .setName(fileUploadRequestDto.fileName())
                        .setMimeType(fileUploadRequestDto.contentType())
                        .setUploadedAt(uploadedAt)
                        .setExpireAt(expireAt)
                        .setMaxDownloads(fileUploadRequestDto.maxDownloads())
                        .setPasswordHash(passwordHash)
                        .setAuthor(author)
                        .setFileStatistics(fileStatistics)
                        .build();

                fileStatisticsRepository.save(fileStatistics);
                fileRepository.save(file);

                log.info("Файл успешно загружен. ID файла: {}, ID пользователя, загрузившего файл: {}, Имя файла: {}, Размер файла в байтах: {}, TTL файла в минутах: {}",
                        fileId, authorId, fileUploadRequestDto.fileName(),
                        fileUploadRequestDto.fileSize(), fileUploadRequestDto.ttlMinutes());

                fileMetrics.recordUploadSuccess(fileUploadRequestDto.fileSize(), uploadBeginSample);
                return fileResponseDtoMapper.map(file);
            });
        } catch (Exception e) {
            fileMetrics.recordOperationError("upload", e);
            log.error("Ошибка сохранения метаданных файла: ID файла: {}, ID пользователя: {}, Путь до файла: {}",
                fileId, authorId, objectStorageFilePath, e);
            try {
                fileStorageRepository.deleteByPath(objectStorageFilePath);
            } catch (Exception cleanUpEx) {
                log.error("Не удалось удалить загруженный в объектное хранилище файл. Путь: {}", objectStorageFilePath, cleanUpEx);
            }

            if (e instanceof StorageQuotaExceededException sqe) {
                fileMetrics.recordStorageQuotaRejection();
                throw sqe;
            }
            throw new FileUploadException("Ошибка сохранения метаданных файла", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileResponseDto> getCurrentUserFiles(int page, int pageSize) {
        Long currentUserId = getCurrentUserId();
        log.debug("Запрос списка файлов. ID пользователя: {}, Страница: {}, Записей на странице: {}", currentUserId, page, pageSize);
        return fileRepository.findAllByAuthorIdOrderByUploadedAtDesc(currentUserId, PageRequest.of(page, pageSize))
                .stream()
                .map(fileResponseDtoMapper::map)
                .toList();
    }

    private User checkAdminRightsAndReturnEntity(Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> {
                    log.error("Администратор не найден. ID администратора: {}", adminId);
                    return new EntityNotFoundException("Администратор с id: %d не найден".formatted(adminId));
                });

        if (!UserRole.ADMIN.equals(admin.getRole())) {
            log.warn("Пользователь {} попытался выполнить действие, не имея на это прав администратора", adminId);
            throw new AccessDeniedException("Недостаточно прав для выполнения операции");
        }
        return admin;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileAdminResponseDto> getAllFiles(String searchBy, String searchValue, int page, int pageSize) {

        User admin = checkAdminRightsAndReturnEntity(requestContextService.getUserId());

        log.debug("Админ-поиск файлов в системе. ID администратора: {}, Критерий: {}, Значение: {}, Страница: {}, Значений на странице: {}",
                admin.getId(), searchBy, searchValue, page, pageSize);

        Pageable pageable = PageRequest.of(page, pageSize);

        Specification<File> specification = Specification.unrestricted();

        FileSearchStrategy fileSearchStrategy = fileSearchStrategyMap.get(searchBy);

        if (fileSearchStrategy == null) {
            log.warn("Неверный критерий поиска файлов. searchBy: '{}', searchValue='{}'", searchBy, searchValue);
            throw new InvalidSearchCriteriaException("Неверный параметр search_by: " + searchBy);
        }

        specification = specification.and(fileSearchStrategy.getSpecification(searchValue));

        return fileRepository.findAll(specification, pageable)
                .map(file -> new FileAdminResponseDto(
                        file.getId().toString(),
                        file.getName(),
                        file.getMimeType(),
                        file.getAuthor() != null ? file.getAuthor().getId().toString() : "",
                        file.getAuthor().getUsername() != null ? file.getAuthor().getUsername() : "Неизвестно",
                        file.getFileStatistics().getSizeBytes(),
                        file.getUploadedAt(),
                        file.getExpireAt(),
                        file.getFileStatistics().getTimesDownloaded(),
                        file.getMaxDownloads(),
                        file.hasPassword()
                ))
                .toList();
    }

    private File getEntityById(String fileId) {

        if (!isValidId(fileId)) {
            throw new FileNotFoundException("Файл с таким id не найден");
        }

        return fileRepository.findById(UUID.fromString(fileId))
                .orElseThrow(() -> new EntityNotFoundException("Файл с таким id не найден"));
    }

    private File getEntityWithDetailsById(String fileId) {

        if (!isValidId(fileId)) {
            throw new FileNotFoundException("Файл с таким id не найден");
        }

        return fileRepository.findWithDetailsById(UUID.fromString(fileId))
                .orElseThrow(() -> new EntityNotFoundException("Файл с таким id не найден"));
    }

    private File getEntityByIdWithDetailsWithLock(String fileId) {

        if (!isValidId(fileId)) {
            throw new FileNotFoundException("Файл с таким id не найден");
        }

        return fileRepository.findForUpdateWithDetailsById(UUID.fromString(fileId))
                .orElseThrow(() -> new EntityNotFoundException("Файл с таким id не найден"));
    }

    @Override
    @Transactional(readOnly = true)
    public FileResponseDto getById(String fileId) {
        log.debug("Запрос метаданных файла. ID пользователя: {}, ID файла: {}", getCurrentUserId(), fileId);
        return fileResponseDtoMapper.map(getEntityById(fileId));
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownloadLinkResponseDto formDownloadLinkPath(String fileId) {

        Long currentUserId = getCurrentUserId();

        log.debug("Формирование ссылки на скачивание файла. ID пользователя: {}, ID файла: {}", currentUserId, fileId);

        File file = getEntityWithDetailsById(fileId);
        FileStatistics fileStatistics = file.getFileStatistics();

        Long authorUserId = file.getAuthor().getId();
        Instant expireAt = file.getExpireAt();
        long timesDownloaded = fileStatistics.getTimesDownloaded();
        long maxDownloads = file.getMaxDownloads();
        String path = file.getPath();

        if (expireAt.isBefore(Instant.now())) {
            throw new FileExpiredException("Срок жизни файла истёк. Дальнейшее распространение файла невозможно");
        }

        if (timesDownloaded >= maxDownloads) {
            throw new DownloadLimitExceededException("Лимит скачиваний исчерпан. Дальнейшее распространение файла невозможно");
        }

        if (!authorUserId.equals(currentUserId)) {
            log.warn("Попытка получения ссылки на чужой файл. ID файла: {}, ID пользователя: {}", fileId, currentUserId);
            throw new IllegalFileAccessException("Формирование ссылки на этот файл доступно только его владельцу");
        }

        if (!fileStorageRepository.fileExistsByPath(path)) {
            throw new FileNotFoundException("Файл не найден");
        }

        log.info("Ссылка на скачивание сформирована. ID файла: {}. Пользователь, запросивший формирование ссылки: {}",
                fileId, currentUserId);

        return new FileDownloadLinkResponseDto(baseUrl + FILE_DOWNLOAD_LINK_PATH_TEMPLATE.formatted(fileId));
    }

    @Override
    public FileDownloadResponseDto downloadById(FileDownloadRequestDto fileDownloadRequestDto) {

        Timer.Sample downloadBeginSample = Timer.start(meterRegistry);

        log.debug("Запрос скачивания файла. ID пользователя: {}, ID файла: {}", getCurrentUserId(), fileDownloadRequestDto.fileId());

        File fileMeta = getEntityById(fileDownloadRequestDto.fileId());

        if (!fileStorageRepository.fileExistsByPath(fileMeta.getPath())) {
            log.error("Данные о файле есть в БД, но файл отсутствует в хранилище. ID файла: {}, путь до файла: {}",
                    fileMeta.getId(), fileMeta.getPath());
            FileNotFoundException e = new FileNotFoundException("Файл не найден");
            fileMetrics.recordOperationError("download", e);
            throw e;
        }

        FileDownloadPreparation fileDownloadPreparation = transactionTemplate.execute(status -> {
            File file = getEntityByIdWithDetailsWithLock(fileDownloadRequestDto.fileId());

            FileStatistics fileStatistics = file.getFileStatistics();

            if (fileStatistics.getTimesDownloaded() >= file.getMaxDownloads()) {
                log.warn("Лимит скачиваний исчерпан. ID файла: {}, ID пользователя, запросившего скачивание: {}",
                        file.getId(), getCurrentUserId());
                DownloadLimitExceededException e = new DownloadLimitExceededException("Достигнуто максимальное количество скачиваний файла");
                fileMetrics.recordDownloadLimitHit();
                fileMetrics.recordOperationError("download", e);
                throw e;
            }

            if (Instant.now().isAfter(file.getExpireAt())) {
                log.debug("Истёк срок жизни файла. ID файла: {}, ID пользователя, запросившего скачивание: {}",
                        file.getId(), getCurrentUserId());
                FileExpiredException e = new FileExpiredException("Срок жизни файла истёк");
                fileMetrics.recordExpiredFileAttempt();
                fileMetrics.recordOperationError("download", e);
                throw e;
            }

            if (file.hasPassword()) {
                if (!passwordEncoder.matches(fileDownloadRequestDto.password(), file.getPasswordHash())) {
                    log.warn("Неверный пароль при скачивании. ID файла: {}, ID пользователя, запросившего скачивание: {}",
                            file.getId(), getCurrentUserId());

                    InvalidPasswordException e = new InvalidPasswordException("Указан неверный пароль для скачивания файла");
                    fileMetrics.recordPasswordCheckFailure();
                    fileMetrics.recordOperationError("download", e);
                    throw e;
                }
            }

            Instant downloadedAt = Instant.now();
            int updated = fileStatisticsRepository.onDownload(fileStatistics.getId(), file.getMaxDownloads(), downloadedAt);

            if (updated == 0) {
                if (downloadedAt.isAfter(file.getExpireAt())) {
                    log.debug("Файл просрочен. ID файла = {}", file.getId());
                    FileExpiredException e = new FileExpiredException("Срок жизни файла истёк");
                    fileMetrics.recordExpiredFileAttempt();
                    fileMetrics.recordOperationError("download", e);
                    throw e;
                }
                if (fileStatistics.getTimesDownloaded() >= file.getMaxDownloads()) {
                    log.warn("Лимит скачиваний исчерпан. ID файла = {}, путь до файла = {}", file.getId(), file.getPath());
                    DownloadLimitExceededException e = new DownloadLimitExceededException("Достигнуто максимальное количество скачиваний");
                    fileMetrics.recordDownloadLimitHit();
                    fileMetrics.recordOperationError("download", e);
                    throw e;
                }

                FileNotFoundException e = new FileNotFoundException("Файл недоступен для скачивания.");
                fileMetrics.recordOperationError("download", e);
                throw e;
            }

            return new FileDownloadPreparation(
                    file.getPath(),
                    file.getName(),
                    file.getMimeType(),
                    fileStatistics.getSizeBytes(),
                    fileStatistics.getTimesDownloaded() + 1
            );
        });

        InputStream fileDataInputStream = fileStorageRepository.downloadByPath(fileDownloadPreparation.filePath());

        log.info("Файл успешно скачан: ID файла: {}, ID пользователя, загрузившего файл: {}, Кол-во скачиваний: {}",
                fileDownloadRequestDto.fileId(), getCurrentUserId(), fileDownloadPreparation.newDownloadCount());

        fileMetrics.recordDownloadSuccess(downloadBeginSample);

        return new FileDownloadResponseDto(
                fileDataInputStream,
                fileDownloadPreparation.fileName(),
                fileDownloadPreparation.mimeType(),
                fileDownloadPreparation.fileSize()
        );
    }

    @Override
    @Transactional
    public void deleteById(String fileId) {

        Timer.Sample deleteBeginSample = Timer.start(meterRegistry);
        log.debug("Запрос удаления файла. ID пользователя: {}, ID файла: {}", getCurrentUserId(), fileId);

        Long deleteRequestUserId = getCurrentUserId();
        User deleteRequestUser = userRepository.findById(deleteRequestUserId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id: %d не найден".formatted(deleteRequestUserId)));
        UserRole deleteRequestUserRole = deleteRequestUser.getRole();

        File file = getEntityByIdWithDetailsWithLock(fileId);
        UUID fileUuid = UUID.fromString(fileId);

        if (deleteRequestUserRole != UserRole.ADMIN && !deleteRequestUser.getId().equals(file.getAuthor().getId())) {
            log.warn("Попытка неразрешённого удаления файла пользователем. ID файла: {}, ID пользователя: {}",
                    fileUuid, deleteRequestUserId);
            IllegalFileAccessException e = new IllegalFileAccessException("Удаление этого файла доступно только его владельцу и администраторам");
            fileMetrics.recordOperationError("delete", e);
            throw e;
        }

        FileStatistics fileStatistics = file.getFileStatistics();
        Long fileSizeBytes = fileStatistics.getSizeBytes();
        User author = file.getAuthor();

        String filePath = file.getPath();
        fileRepository.deleteById(fileUuid);

        StorageQuota storageQuota = author.getStorageQuota();
        storageQuotaService.updateStorageQuota(storageQuota.getId(), -fileStatistics.getSizeBytes());

        try {
            fileStorageRepository.deleteByPath(filePath);
            fileMetrics.recordDeleteSuccess(fileSizeBytes, deleteBeginSample);
        } catch (Exception e) {
            fileMetrics.recordOperationError("delete", e);
            log.error("Не удалось удалить файл из хранилища. ID файла: {}, путь до файла: {}", fileId, filePath, e);
        }

        if (deleteRequestUserRole == UserRole.ADMIN) {
            log.info("Файл успешно удалён администратором: ID файла: {}, ID администратора: {}, Освобождено байт: {}",
                    fileId, deleteRequestUserId, fileStatistics.getSizeBytes());
        } else {
            log.info("Файл успешно удалён автором: ID файла: {}, ID автора: {}, Освобождено байт: {}",
                    fileId, deleteRequestUserId, fileStatistics.getSizeBytes());
        }
    }

    private record FileDownloadPreparation(String filePath, String fileName, String mimeType, long fileSize, long newDownloadCount){}
}

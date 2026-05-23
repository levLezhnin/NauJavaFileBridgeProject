package ru.LevLezhnin.NauJava.service.implementations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.LevLezhnin.NauJava.dto.file.*;
import ru.LevLezhnin.NauJava.exceptions.common.EntityNotFoundException;
import ru.LevLezhnin.NauJava.exceptions.common.InvalidPasswordException;
import ru.LevLezhnin.NauJava.exceptions.file.*;
import ru.LevLezhnin.NauJava.mapper.Mapper;
import ru.LevLezhnin.NauJava.model.*;
import ru.LevLezhnin.NauJava.repository.custom.ObjectStorageRepository;
import ru.LevLezhnin.NauJava.repository.jpa.FileRepository;
import ru.LevLezhnin.NauJava.repository.jpa.FileStatisticsRepository;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.service.interfaces.FileService;
import ru.LevLezhnin.NauJava.service.interfaces.StorageQuotaService;
import ru.LevLezhnin.NauJava.utils.RequestContextService;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
                           Mapper<File, FileResponseDto> fileResponseDtoMapper) {
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.userRepository = userRepository;
        this.storageQuotaService = storageQuotaService;
        this.fileRepository = fileRepository;
        this.fileStatisticsRepository = fileStatisticsRepository;
        this.fileStorageRepository = fileStorageRepository;
        this.requestContextService = requestContextService;
        this.passwordEncoder = passwordEncoder;
        this.fileResponseDtoMapper = fileResponseDtoMapper;
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
    @Transactional
    public FileResponseDto uploadFile(FileUploadRequestDto fileUploadRequestDto,
                                      InputStream fileDataStream) {

        Long authorId = getCurrentUserId();
        User author = userRepository.findWithDetailsById(authorId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id: %d не найден".formatted(authorId)));

        StorageQuota storageQuota = author.getStorageQuota();

        UUID fileId = UUID.randomUUID();
        String objectStorageFilePath = OBJECT_STORAGE_PATH_TEMPLATE.formatted(author.getId(), fileId.toString());
        Instant uploadedAt = Instant.now();
        Instant expireAt = uploadedAt.plus(Duration.ofMinutes(fileUploadRequestDto.ttlMinutes()));
        String passwordHash = isPasswordNotProvided(fileUploadRequestDto.password()) ? null : passwordEncoder.encode(fileUploadRequestDto.password());

        storageQuotaService.updateStorageQuota(storageQuota.getId(), fileUploadRequestDto.fileSize());

        fileStorageRepository.uploadWithPath(
                objectStorageFilePath,
                fileDataStream,
                fileUploadRequestDto.fileSize(),
                MediaType.APPLICATION_OCTET_STREAM_VALUE
        );

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

        try {
            fileStatisticsRepository.save(fileStatistics);
            fileRepository.save(file);
        } catch (Exception e) {
            log.error("Ошибка сохранения метаданных файла: ID файла: {}, ID пользователя: {}, Путь до файла: {}",
                fileId, authorId, objectStorageFilePath, e);
            try {
                fileStorageRepository.deleteByPath(objectStorageFilePath);
            } catch (Exception cleanUpEx) {
                log.error("Не удалось удалить загруженный в объектное хранилище файл. Путь: {}", objectStorageFilePath, cleanUpEx);
            }
            throw new FileUploadException("Ошибка сохранения метаданных файла", e);
        }

        log.info("Файл успешно загружен. ID файла: {}, ID пользователя, загрузившего файл: {}, Имя файла: {}, Размер файла в байтах: {}, TTL файла в минутах: {}",
                fileId, authorId, fileUploadRequestDto.fileName(),
                fileUploadRequestDto.fileSize(), fileUploadRequestDto.ttlMinutes());

        return fileResponseDtoMapper.map(file);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileResponseDto> getCurrentUserFiles(int page, int pageSize) {
        return fileRepository.findAllByAuthorIdOrderByUploadedAtDesc(getCurrentUserId(), PageRequest.of(page, pageSize))
                .stream()
                .map(fileResponseDtoMapper::map)
                .toList();
    }

    private File getEntityById(String fileId) {

        if (!isValidId(fileId)) {
            throw new FileNotFoundException("Файл с id: '%s' не найден".formatted(fileId));
        }

        return fileRepository.findById(UUID.fromString(fileId))
                .orElseThrow(() -> new EntityNotFoundException("Файл с id: %s не найден".formatted(fileId)));
    }

    private File getEntityWithDetailsById(String fileId) {

        if (!isValidId(fileId)) {
            throw new FileNotFoundException("Файл с id: '%s' не найден".formatted(fileId));
        }

        return fileRepository.findWithDetailsById(UUID.fromString(fileId))
                .orElseThrow(() -> new EntityNotFoundException("Файл с id: %s не найден".formatted(fileId)));
    }

    private File getEntityByIdWithDetailsWithLock(String fileId) {

        if (!isValidId(fileId)) {
            throw new FileNotFoundException("Файл с id: '%s' не найден".formatted(fileId));
        }

        return fileRepository.findForUpdateWithDetailsById(UUID.fromString(fileId))
                .orElseThrow(() -> new EntityNotFoundException("Файл с id: %s не найден".formatted(fileId)));
    }

    @Override
    @Transactional(readOnly = true)
    public FileResponseDto getById(String fileId) {
        return fileResponseDtoMapper.map(getEntityById(fileId));
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownloadLinkResponseDto formDownloadLinkPath(String fileId) {

        Long currentUserId = getCurrentUserId();

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

        log.info("Ссылка на скачивание сформирована. ID файла: {}. Пользователь, запросивший формирование ссылки: {}", fileId, currentUserId);

        return new FileDownloadLinkResponseDto(baseUrl + FILE_DOWNLOAD_LINK_PATH_TEMPLATE.formatted(fileId));
    }

    @Override
    public FileDownloadResponseDto downloadById(FileDownloadRequestDto fileDownloadRequestDto) {

        File fileMeta = getEntityById(fileDownloadRequestDto.fileId());

        if (!fileStorageRepository.fileExistsByPath(fileMeta.getPath())) {
            log.error("Данные о файле есть в БД, но файл отсутствует в хранилище. ID файла: {}, путь до файла: {}", fileMeta.getId(), fileMeta.getPath());
            throw new FileNotFoundException("Файл не найден");
        }

        FileDownloadPreparation fileDownloadPreparation = transactionTemplate.execute(status -> {
            File file = getEntityByIdWithDetailsWithLock(fileDownloadRequestDto.fileId());

            FileStatistics fileStatistics = file.getFileStatistics();

            if (fileStatistics.getTimesDownloaded() >= file.getMaxDownloads()) {
                log.warn("Лимит скачиваний исчерпан. ID файла: {}, ID пользователя, запросившего скачивание: {}", file.getId(), getCurrentUserId());
                throw new DownloadLimitExceededException("Достигнуто максимальное количество скачиваний файла");
            }

            if (Instant.now().isAfter(file.getExpireAt())) {
                log.debug("Истёк срок жизни файла. ID файла: {}, ID пользователя, запросившего скачивание: {}", file.getId(), getCurrentUserId());
                throw new FileExpiredException("Срок жизни файла истёк");
            }

            if (file.hasPassword()) {
                if (!passwordEncoder.matches(fileDownloadRequestDto.password(), file.getPasswordHash())) {
                    log.warn("Неверный пароль при скачивании. ID файла: {}, ID пользователя, запросившего скачивание: {}", file.getId(), getCurrentUserId());
                    throw new InvalidPasswordException("Указан неверный пароль для скачивания файла");
                }
            }

            Instant downloadedAt = Instant.now();
            int updated = fileStatisticsRepository.onDownload(fileStatistics.getId(), file.getMaxDownloads(), downloadedAt);

            if (updated == 0) {
                if (downloadedAt.isAfter(file.getExpireAt())) {
                    log.debug("Файл просрочен. ID файла = {}", file.getId());
                    throw new FileExpiredException("Срок жизни файла истёк");
                }
                if (fileStatistics.getTimesDownloaded() >= file.getMaxDownloads()) {
                    log.warn("Лимит скачиваний исчерпан. ID файла = {}, путь до файла = {}", file.getId(), file.getPath());
                    throw new DownloadLimitExceededException("Достигнуто максимальное количество скачиваний");
                }
                throw new FileNotFoundException("Файл недоступен для скачивания.");
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

        Long deleteRequestUserId = getCurrentUserId();
        User deleteRequestUser = userRepository.findById(deleteRequestUserId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id: %d не найден".formatted(deleteRequestUserId)));

        File file = getEntityByIdWithDetailsWithLock(fileId);
        UUID fileUuid = UUID.fromString(fileId);

        if (deleteRequestUser.getRole() != UserRole.ADMIN && !deleteRequestUser.getId().equals(file.getAuthor().getId())) {
            log.warn("Попытка неразрешённого удаления файла пользователем. ID файла: {}, ID пользователя: {}", fileUuid, deleteRequestUserId);
            throw new IllegalFileAccessException("Удаление этого файла доступно только его владельцу и администраторам");
        }

        FileStatistics fileStatistics = file.getFileStatistics();
        User author = file.getAuthor();

        String filePath = file.getPath();
        fileRepository.deleteById(fileUuid);

        StorageQuota storageQuota = author.getStorageQuota();
        storageQuotaService.updateStorageQuota(storageQuota.getId(), -fileStatistics.getSizeBytes());

        try {
            fileStorageRepository.deleteByPath(filePath);
        } catch (Exception e) {
            log.error("Не удалось удалить файл из хранилища. ID файла: {}, путь до файла: {}", fileId, filePath, e);
        }

        log.info("Файл успешно удалён: ID файла: {}, ID пользователя, удалившего файл: {}, Освобождено байт: {}",
                fileId, deleteRequestUserId, fileStatistics.getSizeBytes());
    }

    private record FileDownloadPreparation(String filePath, String fileName, String mimeType, long fileSize, long newDownloadCount){}
}

package ru.LevLezhnin.NauJava.service.implementations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.MimeTypeUtils;
import ru.LevLezhnin.NauJava.dto.file.*;
import ru.LevLezhnin.NauJava.exception.common.EntityNotFoundException;
import ru.LevLezhnin.NauJava.exception.common.InvalidPasswordException;
import ru.LevLezhnin.NauJava.exception.common.InvalidSearchCriteriaException;
import ru.LevLezhnin.NauJava.exception.file.*;
import ru.LevLezhnin.NauJava.exception.storagequotas.StorageQuotaExceededException;
import ru.LevLezhnin.NauJava.mapper.FileResponseMapper;
import ru.LevLezhnin.NauJava.model.*;
import ru.LevLezhnin.NauJava.repository.custom.ObjectStorageRepository;
import ru.LevLezhnin.NauJava.repository.jpa.FileRepository;
import ru.LevLezhnin.NauJava.repository.jpa.FileStatisticsRepository;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.repository.search.file.FileSearchStrategy;
import ru.LevLezhnin.NauJava.security.context.RequestContextService;
import ru.LevLezhnin.NauJava.service.interfaces.StorageQuotaService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplUnitTest {

    private static final String API_BASE_URL = "https://api:8080";
    private static final String TEST_PASSWORD_HASH = "test_password_hash";
    private static final String TEST_DATA = "test data";
    public static final String TEST_FILE_NAME = "test.txt";
    public static final String TEST_PASSWORD = "test_password";

    @Mock
    private PlatformTransactionManager platformTransactionManager;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private FileStatisticsRepository fileStatisticsRepository;
    @Mock
    private ObjectStorageRepository fileStorageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FileSearchStrategy mockFileSearchStrategy;

    @Mock
    private StorageQuotaService storageQuotaService;
    @Mock
    private RequestContextService requestContextService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private FileServiceImpl fileService;

    private StorageQuota testUserStorageQuota, testAdminStorageQuota;

    private User testUser, testAdmin;
    private File testFile;
    private FileStatistics testFileStatistics;
    private UUID testFileId;
    private String testFilePath;

    @BeforeEach
    public void setUp() {

        mockFileSearchStrategy = mock(FileSearchStrategy.class);
        when(mockFileSearchStrategy.getCriteriaKey()).thenReturn("fileNameContains");

        TransactionStatus mockTransactionStatus = mock(TransactionStatus.class);

        lenient().when(platformTransactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(mockTransactionStatus);
        lenient().doNothing().when(platformTransactionManager).commit(eq(mockTransactionStatus));
        lenient().doNothing().when(platformTransactionManager).rollback(eq(mockTransactionStatus));

        fileService = new FileServiceImpl(
                platformTransactionManager,
                fileRepository,
                fileStatisticsRepository,
                fileStorageRepository,
                userRepository,
                storageQuotaService,
                requestContextService,
                passwordEncoder,
                new FileResponseMapper(),
                List.of(mockFileSearchStrategy)
        );

        fileService.setBaseUrl(API_BASE_URL);

        testUser = User.builder()
                .setId(1L)
                .setUsername("test_user")
                .setEmail("test_user@test.com")
                .setPasswordHash("test_user_hash")
                .setActive(true)
                .setRole(UserRole.USER)
                .setRegisteredAt(Instant.now())
                .build();

        testAdmin = User.builder()
                .setId(2L)
                .setUsername("test_admin")
                .setEmail("test_admin@test.com")
                .setPasswordHash("test_admin_hash")
                .setActive(true)
                .setRole(UserRole.ADMIN)
                .setRegisteredAt(Instant.now())
                .build();

        testUserStorageQuota = StorageQuota.builder()
                .setId(1L)
                .setUser(testUser)
                .setUsedStorageBytes(0L)
                .setMaxStorageBytes(2L)
                .build();
        testUser.setStorageQuota(testUserStorageQuota);

        testAdminStorageQuota = StorageQuota.builder()
                .setId(2L)
                .setUser(testAdmin)
                .setUsedStorageBytes(0L)
                .setMaxStorageBytes(2L)
                .build();
        testAdmin.setStorageQuota(testAdminStorageQuota);

        testFileId = UUID.randomUUID();
        testFilePath = "/files/userId=%d/fileId=%s/data.bin".formatted(testUser.getId(), testFileId);

        testFileStatistics = FileStatistics.builder()
                .setId(1L)
                .setSizeBytes(1L)
                .setTimesDownloaded(0L)
                .setLastDownloadedAt(null)
                .build();

        testFile = File.builder()
                .setId(testFileId)
                .setPath(testFilePath)
                .setName(TEST_FILE_NAME)
                .setMimeType(MediaType.TEXT_PLAIN_VALUE)
                .setUploadedAt(Instant.now())
                .setExpireAt(Instant.now().plus(Duration.ofDays(1L)))
                .setMaxDownloads(1L)
                .setFileStatistics(testFileStatistics)
                .setPasswordHash(TEST_PASSWORD_HASH)
                .setAuthor(testUser)
                .build();
    }

    /**
     * Возвращает builder по существующему file со всеми заполненными полями, кроме author, fileStatistics
     * @param file File, по которому надо построить builder
     * @return builder
     */
    private File.Builder getBuilderByExistingFile(File file) {
        return File.builder()
                .setId(file.getId())
                .setPath(file.getPath())
                .setName(file.getName())
                .setMimeType(file.getMimeType())
                .setUploadedAt(file.getUploadedAt())
                .setExpireAt(file.getExpireAt())
                .setMaxDownloads(file.getMaxDownloads())
                .setPasswordHash(file.getPasswordHash());
    }

    /**
     * Возвращает builder по существующему fileStatistics со всеми заполненными полями, кроме file
     * @param fileStatistics FileStatistics, по которому надо построить builder
     * @return builder
     */
    private FileStatistics.Builder getFileStatisticsBuilderByExistingFileStatistics(FileStatistics fileStatistics) {
        return FileStatistics.builder()
                .setId(fileStatistics.getId())
                .setSizeBytes(fileStatistics.getSizeBytes())
                .setTimesDownloaded(fileStatistics.getTimesDownloaded())
                .setLastDownloadedAt(fileStatistics.getLastDownloadedAt());
    }

    @Nested
    @DisplayName("uploadFile")
    class UploadFileTests {
        @Test
        @DisplayName("Позитивный тест: загрузка файла с паролем")
        public void shouldUploadFile_whenUploadWithPassword() throws IOException {
            when(requestContextService.getUserId()).thenReturn(testUser.getId());

            when(userRepository.findWithDetailsById(testUser.getId())).thenReturn(Optional.of(testUser));

            when(fileStatisticsRepository.save(any(FileStatistics.class))).thenAnswer(i -> i.getArguments()[0]);
            when(fileRepository.save(any(File.class))).thenAnswer(i -> i.getArguments()[0]);
            when(passwordEncoder.encode(any(String.class))).thenReturn(TEST_PASSWORD_HASH);

            InputStream testFileData = new ByteArrayInputStream(TEST_DATA.getBytes(StandardCharsets.UTF_8));

            FileUploadRequestDto fileUploadRequestDto = new FileUploadRequestDto(
                    TEST_FILE_NAME,
                    MimeTypeUtils.TEXT_HTML_VALUE,
                    1L,
                    1L,
                    1L,
                    TEST_PASSWORD
            );

            Instant saveTime = Instant.now();
            FileResponseDto fileResponseDto = fileService.uploadFile(fileUploadRequestDto, testFileData);
            Instant expireTime = saveTime.plus(Duration.ofMinutes(fileUploadRequestDto.ttlMinutes()));

            assertTrue(Duration.between(fileResponseDto.expireDate(), expireTime).compareTo(Duration.ofSeconds(1)) <= 0,
                    "Время истечения файла должно отличаться от расчётного не более чем на 1 секунду");
            assertEquals(0L,
                    fileResponseDto.timesDownloaded(),
                    "Новый файл должен иметь счётчик скачиваний = 0");
            assertEquals(fileUploadRequestDto.maxDownloads(),
                    fileResponseDto.maxDownloads(),
                    "Максимальное количество скачиваний должно совпадать с запрошенным");
            assertEquals(TEST_FILE_NAME,
                    fileResponseDto.fileName(),
                    "Имя файла в ответе должно совпадать с загруженным");
            assertTrue(fileResponseDto.hasPassword(),
                    "Файл должен быть помечен как защищённый паролем");

            ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
            verify(fileRepository, times(1)).save(fileArgumentCaptor.capture());

            File capturedFile = fileArgumentCaptor.getValue();
            assertTrue(capturedFile.getPath().matches("/files/userId=\\d+/fileId=.+/data.bin"),
                    "Путь к файлу в хранилище должен соответствовать шаблону");
            assertEquals(testUser.getId(), capturedFile.getAuthor().getId(),
                    "Автором файла должен быть текущий пользователь");
            assertEquals(TEST_PASSWORD_HASH, capturedFile.getPasswordHash(),
                    "Хеш пароля файла должен совпадать с закодированным значением");

            ArgumentCaptor<FileStatistics> statisticsArgumentCaptor = ArgumentCaptor.forClass(FileStatistics.class);
            verify(fileStatisticsRepository, times(1)).save(statisticsArgumentCaptor.capture());

            FileStatistics capturedStatistics = statisticsArgumentCaptor.getValue();
            assertEquals(fileUploadRequestDto.fileSize(), capturedStatistics.getSizeBytes(),
                    "Размер в статистике должен совпадать с размером загруженного файла");
            assertThat(capturedFile.getFileStatistics()).isSameAs(capturedStatistics);
            assertEquals(0L, capturedStatistics.getTimesDownloaded(),
                    "Счётчик скачиваний нового файла должен быть равен 0");

            verify(storageQuotaService).updateStorageQuota(testUserStorageQuota.getId(), fileUploadRequestDto.fileSize());

            ArgumentCaptor<InputStream> streamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);
            verify(fileStorageRepository, times(1)).uploadWithPath(any(String.class), streamArgumentCaptor.capture(), eq(1L), eq(MediaType.APPLICATION_OCTET_STREAM_VALUE));
            String savedContent = new String(streamArgumentCaptor.getValue().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TEST_DATA, savedContent,
                    "Содержимое файла в объектном хранилище должно совпадать с исходными данными");

            verify(passwordEncoder, times(1)).encode(any(String.class));
        }

        @Test
        @DisplayName("Позитивный тест: загрузка файла без пароля")
        public void shouldUploadFile_whenUploadWithoutPassword() throws IOException {
            when(requestContextService.getUserId()).thenReturn(testUser.getId());

            when(userRepository.findWithDetailsById(testUser.getId())).thenReturn(Optional.of(testUser));

            when(fileStatisticsRepository.save(any(FileStatistics.class))).thenAnswer(i -> i.getArguments()[0]);
            when(fileRepository.save(any(File.class))).thenAnswer(i -> i.getArguments()[0]);

            InputStream testFileData = new ByteArrayInputStream(TEST_DATA.getBytes(StandardCharsets.UTF_8));

            FileUploadRequestDto fileUploadRequestDto = new FileUploadRequestDto(
                    TEST_FILE_NAME,
                    MimeTypeUtils.TEXT_HTML_VALUE,
                    1L,
                    1L,
                    1L,
                    null
            );

            Instant saveTime = Instant.now();
            FileResponseDto fileResponseDto = fileService.uploadFile(fileUploadRequestDto, testFileData);
            Instant expireTime = saveTime.plus(Duration.ofMinutes(fileUploadRequestDto.ttlMinutes()));

            assertTrue(Duration.between(fileResponseDto.expireDate(), expireTime).compareTo(Duration.ofSeconds(1)) <= 0,
                    "Время истечения файла должно отличаться от расчётного не более чем на 1 секунду");
            assertEquals(0L, fileResponseDto.timesDownloaded(),
                    "Новый файл должен иметь счётчик скачиваний = 0");
            assertEquals(fileUploadRequestDto.maxDownloads(), fileResponseDto.maxDownloads(),
                    "Максимальное количество скачиваний должно совпадать с запрошенным");
            assertEquals(TEST_FILE_NAME, fileResponseDto.fileName(),
                    "Имя файла в ответе должно совпадать с загруженным");
            assertFalse(fileResponseDto.hasPassword(), "Файл должен быть помечен как незащищённый паролем");

            ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
            verify(fileRepository, times(1)).save(fileArgumentCaptor.capture());

            File capturedFile = fileArgumentCaptor.getValue();
            assertTrue(capturedFile.getPath().matches("/files/userId=\\d+/fileId=.+/data.bin"),
                    "Путь к файлу в хранилище должен соответствовать шаблону");
            assertEquals(testUser.getId(), capturedFile.getAuthor().getId(),
                    "Автором файла должен быть текущий пользователь");
            assertNull(capturedFile.getPasswordHash(),
                    "Хеш пароля файла должен совпадать с закодированным значением");

            ArgumentCaptor<FileStatistics> statisticsArgumentCaptor = ArgumentCaptor.forClass(FileStatistics.class);
            verify(fileStatisticsRepository, times(1)).save(statisticsArgumentCaptor.capture());

            FileStatistics capturedStatistics = statisticsArgumentCaptor.getValue();
            assertEquals(fileUploadRequestDto.fileSize(), capturedStatistics.getSizeBytes(),
                    "Размер в статистике должен совпадать с размером загруженного файла");
            assertThat(capturedFile.getFileStatistics()).isSameAs(capturedStatistics);
            assertEquals(0L, capturedStatistics.getTimesDownloaded(),
                    "Счётчик скачиваний нового файла должен быть равен 0");

            verify(storageQuotaService).updateStorageQuota(testUserStorageQuota.getId(), fileUploadRequestDto.fileSize());

            ArgumentCaptor<InputStream> streamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);
            verify(fileStorageRepository, times(1)).uploadWithPath(any(String.class), streamArgumentCaptor.capture(), eq(1L), eq(MediaType.APPLICATION_OCTET_STREAM_VALUE));
            String savedContent = new String(streamArgumentCaptor.getValue().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TEST_DATA, savedContent, "Содержимое файла в объектном хранилище должно совпадать с исходными данными");

            verify(passwordEncoder, never()).encode(any(String.class));
        }

        @Test
        @DisplayName("Негативный тест: пользователь не найден при загрузке")
        public void shouldThrow_whenUserNotFoundOnUpload() {
            when(requestContextService.getUserId()).thenReturn(999L);
            when(userRepository.findWithDetailsById(999L)).thenReturn(Optional.empty());

            var dto = new FileUploadRequestDto(TEST_FILE_NAME, MediaType.TEXT_PLAIN_VALUE, 1L, 1L, 1L, null);

            assertThrows(EntityNotFoundException.class,
                    () -> fileService.uploadFile(dto, new ByteArrayInputStream("data".getBytes())),
                    "Загрузка файла должна завершиться с ошибкой EntityNotFoundException, если пользователь, загружающий файл, не найден");
        }

        @Test
        @DisplayName("Негативный тест: загружаемый файл не помещается в остающееся место")
        public void shouldThrow_whenNoSpaceRemainingForUploadingFile() {
            when(requestContextService.getUserId()).thenReturn(testUser.getId());

            when(userRepository.findWithDetailsById(testUser.getId())).thenReturn(Optional.of(testUser));
            doThrow(StorageQuotaExceededException.class).when(storageQuotaService).updateStorageQuota(eq(testUserStorageQuota.getId()), any(Long.class));

            InputStream testFileData = new ByteArrayInputStream(TEST_DATA.getBytes(StandardCharsets.UTF_8));

            FileUploadRequestDto fileUploadRequestDto = new FileUploadRequestDto(
                    TEST_FILE_NAME,
                    MimeTypeUtils.TEXT_HTML_VALUE,
                    testUserStorageQuota.getBytesRemaining() + 1L,
                    1L,
                    1L,
                    null
            );

            assertThrows(StorageQuotaExceededException.class, () -> fileService.uploadFile(fileUploadRequestDto, testFileData),
                    "Загрузка файла должна завершиться с ошибкой StorageQuotaExceededException, если загружаемый файл не помещается в остающееся место");
        }

        @Test
        @DisplayName("Негативный тест: при ошибке сохранения метаданных должно произойти очистка объектного хранилища")
        public void shouldThrowAndRollback_whenMetadataSaveFails() throws IOException {
            when(requestContextService.getUserId()).thenReturn(testUser.getId());

            when(userRepository.findWithDetailsById(testUser.getId())).thenReturn(Optional.of(testUser));

            when(fileStatisticsRepository.save(any(FileStatistics.class))).thenAnswer(i -> i.getArguments()[0]);
            when(fileRepository.save(any(File.class))).thenThrow(new RuntimeException("test exception"));
            when(passwordEncoder.encode(any(String.class))).thenReturn(TEST_PASSWORD_HASH);

            InputStream testFileData = new ByteArrayInputStream(TEST_DATA.getBytes(StandardCharsets.UTF_8));

            FileUploadRequestDto fileUploadRequestDto = new FileUploadRequestDto(
                    TEST_FILE_NAME,
                    MimeTypeUtils.TEXT_HTML_VALUE,
                    1L,
                    1L,
                    1L,
                    TEST_PASSWORD
            );

            assertThrows(FileUploadException.class, () -> fileService.uploadFile(fileUploadRequestDto, testFileData));

            ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
            verify(fileRepository, times(1)).save(fileArgumentCaptor.capture());

            File capturedFile = fileArgumentCaptor.getValue();
            assertTrue(capturedFile.getPath().matches("/files/userId=\\d+/fileId=.+/data.bin"),
                    "Путь к файлу в хранилище должен соответствовать шаблону");
            assertEquals(testUser.getId(), capturedFile.getAuthor().getId(),
                    "Автором файла должен быть текущий пользователь");
            assertEquals(TEST_PASSWORD_HASH, capturedFile.getPasswordHash(),
                    "Хеш пароля файла должен совпадать с закодированным значением");

            ArgumentCaptor<FileStatistics> statisticsArgumentCaptor = ArgumentCaptor.forClass(FileStatistics.class);
            verify(fileStatisticsRepository, times(1)).save(statisticsArgumentCaptor.capture());

            FileStatistics capturedStatistics = statisticsArgumentCaptor.getValue();
            assertEquals(fileUploadRequestDto.fileSize(), capturedStatistics.getSizeBytes(),
                    "Размер в статистике должен совпадать с размером загруженного файла");
            assertThat(capturedFile.getFileStatistics()).isSameAs(capturedStatistics);
            assertEquals(0L, capturedStatistics.getTimesDownloaded(),
                    "Счётчик скачиваний нового файла должен быть равен 0");

            ArgumentCaptor<InputStream> streamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);
            verify(fileStorageRepository, times(1)).uploadWithPath(any(String.class), streamArgumentCaptor.capture(), eq(1L), eq(MediaType.APPLICATION_OCTET_STREAM_VALUE));
            String savedContent = new String(streamArgumentCaptor.getValue().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TEST_DATA, savedContent,
                    "Содержимое файла в объектном хранилище должно совпадать с исходными данными при откате");

            verify(passwordEncoder, times(1)).encode(any(String.class));

            verify(fileStorageRepository, times(1)).deleteByPath(capturedFile.getPath());
        }

        @Test
        @DisplayName("Граничный тест: файл вмещается ровно в оставшееся место")
        public void shouldUpload_whenFileSizeEqualsRemainingQuota() {
            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(userRepository.findWithDetailsById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(fileStatisticsRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
            when(fileRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

            var dto = new FileUploadRequestDto(TEST_FILE_NAME, MediaType.TEXT_PLAIN_VALUE,
                    testUserStorageQuota.getBytesRemaining(), 1L, 1L, null);

            assertDoesNotThrow(() -> fileService.uploadFile(dto,
                    new ByteArrayInputStream("x".repeat(dto.fileSize().intValue()).getBytes())));
        }
    }

    @Nested
    @DisplayName("getCurrentUserFiles")
    class GetCurrentUserFilesTests {
        @Test
        @DisplayName("Позитивный тест: получение списка файлов пользователя")
        public void shouldReturnUserFiles() {
            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(fileRepository.findAllByAuthorIdOrderByUploadedAtDesc(eq(testUser.getId()), any(PageRequest.class)))
                    .thenReturn(List.of(testFile));

            var result = fileService.getCurrentUserFiles(0, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).fileName()).isEqualTo(TEST_FILE_NAME);
            verify(fileRepository).findAllByAuthorIdOrderByUploadedAtDesc(eq(testUser.getId()), eq(PageRequest.of(0, 10)));
        }
    }

    @Nested
    @DisplayName("getAllFiles")
    class GetAllFilesTests {
        @Test
        @DisplayName("Позитивный тест: файлы вернулись по запросу администратора")
        void shouldReturnFiles_whenValidRequestProvided() {
            String searchBy = mockFileSearchStrategy.getCriteriaKey();
            String value = testFile.getName();

            Page<File> pageResult = new PageImpl<>(List.of(testFile));

            when(userRepository.findById(any(Long.class))).thenReturn(Optional.of(testAdmin));
            when(mockFileSearchStrategy.getSpecification(value)).thenReturn(Specification.unrestricted());
            when(fileRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(pageResult);

            List<FileAdminResponseDto> result = fileService.getAllFiles(searchBy, value, 0, 10);

            assertEquals(1, result.size());
            assertEquals(value, result.getFirst().fileName());
        }

        @Test
        @DisplayName("Негативный тест: невалидный критерий поиска в запросе")
        void shouldThrow_whenInvalidSearchByProvided() {
            String searchBy = "unknown";
            when(userRepository.findById(any(Long.class))).thenReturn(Optional.of(testAdmin));
            InvalidSearchCriteriaException e = assertThrows(InvalidSearchCriteriaException.class, () -> fileService.getAllFiles(searchBy, "value", 0, 10));
            assertTrue(e.getMessage().startsWith("Неверный параметр search_by: "));
        }

        @Test
        @DisplayName("Негативный тест: запрос пришёл не от админстратора")
        void shouldThrow_whenNotAdminRequested() {
            when(userRepository.findById(any(Long.class))).thenReturn(Optional.of(testUser));
            AccessDeniedException e = assertThrows(AccessDeniedException.class, () -> fileService.getAllFiles("criteria", "value", 0, 10));
            assertEquals("Недостаточно прав для выполнения операции", e.getMessage());
        }

        @Test
        @DisplayName("Негативный тест: запрашивающий пользователь не найден")
        void shouldThrow_whenRequestingUserNotFound() {
            when(userRepository.findById(any(Long.class))).thenReturn(Optional.empty());
            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> fileService.getAllFiles("criteria", "value", 0, 10));
            assertTrue(e.getMessage().startsWith("Администратор с id: "));
        }
    }

    @Nested
    @DisplayName("getById")
    class GetByIdTests {
        @Test
        @DisplayName("Позитивный тест: файл найден по валидному ID")
        void shouldReturnFile_whenValidRequestProvided() {
            when(fileRepository.findById(testFile.getId())).thenReturn(Optional.of(testFile));

            var result = fileService.getById(testFile.getId().toString());
            assertEquals(testFile.getId().toString(), result.id());
            assertEquals(testFile.getName(), result.fileName());
            assertEquals(testFile.getFileStatistics().getSizeBytes(), result.fileSizeBytes());
            assertEquals(testFile.getUploadedAt(), result.uploadDate());
            assertEquals(testFile.getExpireAt(), result.expireDate());
            assertEquals(testFile.getFileStatistics().getTimesDownloaded(), result.timesDownloaded());
            assertEquals(testFile.getMaxDownloads(), result.maxDownloads());
            assertEquals(testFile.hasPassword(), result.hasPassword());
        }

        @Test
        @DisplayName("Негативный тест: в запросе приведён невалидный UUID")
        void shouldThrow_whenNotValidUUID() {
            FileNotFoundException e = assertThrows(FileNotFoundException.class, () -> fileService.getById("12345"));
            assertEquals("Файл с таким id не найден", e.getMessage());
        }

        @Test
        @DisplayName("Негативный тест: запрашиваемый файл не существует")
        void shouldThrow_whenFileNotFound() {
            when(fileRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> fileService.getById(testFile.getId().toString()));
            assertEquals("Файл с таким id не найден", e.getMessage());
        }
    }

    @Nested
    @DisplayName("formDownloadLinkPath")
    class FormDownloadLinkPathTests {
        @Test
        @DisplayName("Позитивный тест: ссылка сформирована автором для неистёкшего файла")
        void shouldReturn_whenAuthorRequestedLinkForActiveFile() {
            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(fileRepository.findWithDetailsById(testFile.getId())).thenReturn(Optional.of(testFile));
            when(fileStorageRepository.fileExistsByPath(testFile.getPath())).thenReturn(true);

            FileDownloadLinkResponseDto response = fileService.formDownloadLinkPath(testFile.getId().toString());

            assertTrue(response.downloadLink().startsWith(API_BASE_URL));
            assertTrue(response.downloadLink().contains(testFile.getId().toString()));
        }

        @Test
        @DisplayName("Негативный тест: файл истёк")
        void shouldThrow_whenAuthorRequestedLinkForExpiredFile() {
            File testFileCopy = getBuilderByExistingFile(testFile)
                    .setExpireAt(Instant.now().minusNanos(1))
                    .setAuthor(testUser)
                    .setFileStatistics(testFileStatistics)
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(fileRepository.findWithDetailsById(testFileCopy.getId())).thenReturn(Optional.of(testFileCopy));

            FileExpiredException e = assertThrows(FileExpiredException.class, () -> fileService.formDownloadLinkPath(testFile.getId().toString()));
            assertEquals("Срок жизни файла истёк. Дальнейшее распространение файла невозможно", e.getMessage());
        }

        @Test
        @DisplayName("Негативный тест: исчерпан лимит скачиваний")
        void shouldThrow_whenAuthorRequestedLinkForFileWithDownloadLimitReached() {
            FileStatistics testFileStatisticsCopy = getFileStatisticsBuilderByExistingFileStatistics(testFileStatistics)
                    .setTimesDownloaded(testFile.getMaxDownloads())
                    .build();
            File testFileCopy = getBuilderByExistingFile(testFile)
                    .setAuthor(testUser)
                    .setFileStatistics(testFileStatisticsCopy)
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(fileRepository.findWithDetailsById(testFileCopy.getId())).thenReturn(Optional.of(testFileCopy));

            DownloadLimitExceededException e = assertThrows(DownloadLimitExceededException.class, () -> fileService.formDownloadLinkPath(testFile.getId().toString()));
            assertEquals("Лимит скачиваний исчерпан. Дальнейшее распространение файла невозможно", e.getMessage());
        }

        @Test
        @DisplayName("Негативный тест: ссылку формирует неавтор")
        void shouldThrow_whenNotAuthorRequestedLinkForFile() {
            File testFileCopy = getBuilderByExistingFile(testFile)
                    .setAuthor(User.builder().setId(123L).build())
                    .setFileStatistics(testFileStatistics)
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(fileRepository.findWithDetailsById(testFileCopy.getId())).thenReturn(Optional.of(testFileCopy));

            IllegalFileAccessException e = assertThrows(IllegalFileAccessException.class, () -> fileService.formDownloadLinkPath(testFile.getId().toString()));
            assertEquals("Формирование ссылки на этот файл доступно только его владельцу", e.getMessage());
        }

        @Test
        @DisplayName("Негативный тест: файла нет в объектном хранилище")
        void shouldThrow_whenAuthorRequestedLinkForFileNotInObjectStorage() {
            File testFileCopy = getBuilderByExistingFile(testFile)
                    .setAuthor(testUser)
                    .setFileStatistics(testFileStatistics)
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(fileRepository.findWithDetailsById(testFileCopy.getId())).thenReturn(Optional.of(testFileCopy));
            when(fileStorageRepository.fileExistsByPath(testFile.getPath())).thenReturn(false);

            FileNotFoundException e = assertThrows(FileNotFoundException.class, () -> fileService.formDownloadLinkPath(testFile.getId().toString()));
            assertEquals("Файл не найден", e.getMessage());
        }
    }

    @Nested
    @DisplayName("downloadById")
    class DownloadByIdTests {
        @Test
        @DisplayName("Позитивный тест: скачивание файла с паролем")
        public void shouldDownloadFile_whenCorrectFileIdAndPasswordProvided() throws IOException {

            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(fileRepository.findById(testFileId)).thenReturn(Optional.of(testFile));
            when(fileRepository.findForUpdateWithDetailsById(testFileId)).thenReturn(Optional.of(testFile));
            when(fileStorageRepository.fileExistsByPath(testFilePath)).thenReturn(true);
            when(passwordEncoder.matches(any(CharSequence.class), eq(TEST_PASSWORD_HASH))).thenAnswer(i -> TEST_PASSWORD.equals(i.getArguments()[0]));
            when(fileStatisticsRepository.onDownload(eq(testFileStatistics.getId()), any(Long.class), any(Instant.class))).thenReturn(1);
            when(fileStorageRepository.downloadByPath(testFilePath)).thenReturn(new ByteArrayInputStream(TEST_DATA.getBytes(StandardCharsets.UTF_8)));

            FileDownloadRequestDto fileDownloadRequestDto = new FileDownloadRequestDto(
                    testFileId.toString(),
                    TEST_PASSWORD
            );

            FileDownloadResponseDto fileDownloadResponseDto = fileService.downloadById(fileDownloadRequestDto);

            String returnedData = new String(fileDownloadResponseDto.fileDataInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TEST_DATA, returnedData,
                    "Возвращаемое содержимое файла должно совпадать с исходными данными в объектном хранилище");

            assertEquals(TEST_FILE_NAME, fileDownloadResponseDto.originalFilename(),
                    "Возвращаемый название файла должно совпадать с ожидаемым");
            assertEquals(MediaType.TEXT_PLAIN_VALUE, fileDownloadResponseDto.contentType(),
                    "Возвращаемый MIME-тип файла должен совпадать с ожидаемым");
            assertEquals(1L, fileDownloadResponseDto.sizeBytes(),
                    "Возвращаемый размер файла должен совпадать с ожидаемым");

            verify(passwordEncoder, times(1)).matches(any(CharSequence.class), any(String.class));
            verify(fileStatisticsRepository, times(1)).onDownload(eq(testFileStatistics.getId()), any(Long.class), any(Instant.class));
            verify(fileStorageRepository, times(1)).downloadByPath(eq(testFilePath));
        }

        @Test
        @DisplayName("Позитивный тест: скачивание файла без пароля")
        public void shouldDownloadFile_whenCorrectFileIdAndNoPasswordProvided() throws IOException {

            File testFileWithoutPassword = getBuilderByExistingFile(testFile)
                    .setPasswordHash(null)
                    .setAuthor(testUser)
                    .setFileStatistics(testFileStatistics)
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(fileRepository.findById(testFileWithoutPassword.getId())).thenReturn(Optional.of(testFileWithoutPassword));
            when(fileRepository.findForUpdateWithDetailsById(testFileWithoutPassword.getId())).thenReturn(Optional.of(testFileWithoutPassword));
            when(fileStorageRepository.fileExistsByPath(testFileWithoutPassword.getPath())).thenReturn(true);
            when(fileStatisticsRepository.onDownload(any(Long.class), any(Long.class), any(Instant.class))).thenReturn(1);
            when(fileStorageRepository.downloadByPath(testFileWithoutPassword.getPath())).thenReturn(new ByteArrayInputStream(TEST_DATA.getBytes(StandardCharsets.UTF_8)));

            FileDownloadRequestDto fileDownloadRequestDto = new FileDownloadRequestDto(
                    testFileWithoutPassword.getId().toString(),
                    null
            );

            FileDownloadResponseDto fileDownloadResponseDto = fileService.downloadById(fileDownloadRequestDto);

            String returnedData = new String(fileDownloadResponseDto.fileDataInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TEST_DATA, returnedData,
                    "Возвращаемое содержимое файла должно совпадать с исходными данными в объектном хранилище");

            assertEquals(TEST_FILE_NAME, fileDownloadResponseDto.originalFilename(),
                    "Возвращаемый название файла должно совпадать с ожидаемым");
            assertEquals(MediaType.TEXT_PLAIN_VALUE, fileDownloadResponseDto.contentType(),
                    "Возвращаемый MIME-тип файла должен совпадать с ожидаемым");
            assertEquals(1L, fileDownloadResponseDto.sizeBytes(),
                    "Возвращаемый размер файла должен совпадать с ожидаемым");

            verify(passwordEncoder, never()).matches(any(CharSequence.class), any(String.class));
            verify(fileStorageRepository, times(1)).downloadByPath(eq(testFilePath));
            verify(fileStatisticsRepository, times(1)).onDownload(eq(testFileStatistics.getId()), any(Long.class), any(Instant.class));
        }

        @Test
        @DisplayName("Негативный тест: невалидный UUID")
        public void shouldThrow_whenInvalidUuidFormat() {
            assertThrows(FileNotFoundException.class,
                    () -> fileService.downloadById(new FileDownloadRequestDto("not-a-uuid", null)));

            assertThrows(FileNotFoundException.class,
                    () -> fileService.downloadById(new FileDownloadRequestDto(null, null)));
        }

        @Test
        @DisplayName("Негативный тест: скачивание файла должно завершиться с ошибкой, если метаданные о файле не нашлись в БД")
        public void shouldThrow_whenFileMetadataNotFoundInDatabase() {
            assertThrows(EntityNotFoundException.class, () -> fileService.downloadById(new FileDownloadRequestDto(testFileId.toString(), null)),
                    "Скачивание файла должно завершиться с ошибкой EntityNotFoundException, если метаданные о файле не нашлись в БД");
        }

        @Test
        @DisplayName("Негативный тест: скачивание файла должно завершиться с ошибкой, если файл не нашёлся в хранилище")
        public void shouldThrow_whenFileNotFoundInFileStorage() {
            when(fileRepository.findById(testFileId)).thenReturn(Optional.of(testFile));
            when(fileStorageRepository.fileExistsByPath(testFilePath)).thenReturn(false);
            assertThrows(FileNotFoundException.class, () -> fileService.downloadById(new FileDownloadRequestDto(testFileId.toString(), null)),
                    "Скачивание файла должно завершиться с ошибкой FileNotFoundException, если файл не нашёлся в хранилище");
        }

        @Test
        @DisplayName("Негативный тест: файл удалён между чтением и блокировкой")
        public void shouldThrow_whenFileDeletedBetweenReads() {
            when(fileRepository.findById(eq(testFileId))).thenReturn(Optional.of(testFile));
            when(fileRepository.findForUpdateWithDetailsById(eq(testFileId))).thenReturn(Optional.empty());
            when(fileStorageRepository.fileExistsByPath(any())).thenReturn(true);

            assertThrows(EntityNotFoundException.class,
                    () -> fileService.downloadById(new FileDownloadRequestDto(testFileId.toString(), null)));
        }

        @Test
        @DisplayName("Негативный тест: скачивание файла должно завершиться с ошибкой, если лимит скачиваний достигнут")
        public void shouldThrow_whenDownloadLimitReached() {

            FileStatistics testFileStatisticsCopy = getFileStatisticsBuilderByExistingFileStatistics(testFileStatistics)
                    .setTimesDownloaded(testFile.getMaxDownloads()).build();

            File testFileCopy = getBuilderByExistingFile(testFile)
                    .setAuthor(testUser)
                    .setFileStatistics(testFileStatisticsCopy)
                    .build();

            when(fileStorageRepository.fileExistsByPath(testFileCopy.getPath())).thenReturn(true);
            when(fileRepository.findById(testFileCopy.getId())).thenReturn(Optional.of(testFileCopy));
            when(fileRepository.findForUpdateWithDetailsById(testFileCopy.getId())).thenReturn(Optional.of(testFileCopy));
            assertThrows(DownloadLimitExceededException.class, () -> fileService.downloadById(new FileDownloadRequestDto(testFileId.toString(), null)),
                    "Скачивание файла должно завершиться с ошибкой DownloadLimitExceededException, если лимит скачиваний достигнут");
        }

        @Test
        @DisplayName("Негативный тест: скачивание файла должно завершиться с ошибкой, если время жизни файла истекло")
        public void shouldThrow_whenFileTTLExceeded() {
            testFile.setExpireAt(Instant.now().minus(Duration.ofMinutes(1)));
            when(fileRepository.findById(testFileId)).thenReturn(Optional.of(testFile));
            when(fileRepository.findForUpdateWithDetailsById(testFileId)).thenReturn(Optional.of(testFile));
            when(fileStorageRepository.fileExistsByPath(testFilePath)).thenReturn(true);
            assertThrows(FileExpiredException.class, () -> fileService.downloadById(new FileDownloadRequestDto(testFileId.toString(), null)),
                    "Скачивание файла должно завершиться с ошибкой FileExpiredException, если время жизни файла истекло");
        }

        @Test
        @DisplayName("Негативный тест: скачивание файла должно завершиться с ошибкой, если пароль задан и не был передан в запросе")
        public void shouldThrow_whenFileIsSecuredButNoPasswordIsInRequest() {
            when(fileStorageRepository.fileExistsByPath(testFilePath)).thenReturn(true);
            when(fileRepository.findById(testFileId)).thenReturn(Optional.of(testFile));
            when(fileRepository.findForUpdateWithDetailsById(testFileId)).thenReturn(Optional.of(testFile));
            when(passwordEncoder.matches(any(), eq(TEST_PASSWORD_HASH))).thenAnswer(i -> i.getArguments()[0] != null && TEST_PASSWORD.equals(i.getArguments()[0]));
            assertThrows(InvalidPasswordException.class, () -> fileService.downloadById(new FileDownloadRequestDto(testFileId.toString(), null)),
                    "Скачивание файла должно завершиться с ошибкой InvalidPasswordException, если пароль задан и не был передан в запросе");
        }

        @Test
        @DisplayName("Негативный тест: скачивание файла должно завершиться с ошибкой, если пароль задан и в запросе был передан пустой пароль")
        public void shouldThrow_whenFileIsSecuredButEmptyPasswordIsInRequest() {
            when(fileStorageRepository.fileExistsByPath(testFilePath)).thenReturn(true);
            when(fileRepository.findById(testFileId)).thenReturn(Optional.of(testFile));
            when(fileRepository.findForUpdateWithDetailsById(testFileId)).thenReturn(Optional.of(testFile));
            when(passwordEncoder.matches(any(), eq(TEST_PASSWORD_HASH))).thenAnswer(i -> i.getArguments()[0] != null && TEST_PASSWORD.equals(i.getArguments()[0]));
            assertThrows(InvalidPasswordException.class, () -> fileService.downloadById(new FileDownloadRequestDto(testFileId.toString(), "")),
                    "Скачивание файла должно завершиться с ошибкой InvalidPasswordException, если пароль задан и не был передан в запросе");
        }

        @Test
        @DisplayName("Негативный тест: скачивание файла должно завершиться с ошибкой, если пароль задан и в запросе был передан неверный пароль")
        public void shouldThrow_whenFileIsSecuredButPasswordInRequestIsIncorrect() {
            when(fileRepository.findForUpdateWithDetailsById(testFileId)).thenReturn(Optional.of(testFile));
            when(fileRepository.findById(testFileId)).thenReturn(Optional.of(testFile));
            when(fileStorageRepository.fileExistsByPath(testFilePath)).thenReturn(true);
            when(passwordEncoder.matches(any(CharSequence.class), eq(TEST_PASSWORD_HASH))).thenAnswer(i -> i.getArguments()[0] != null && TEST_PASSWORD.equals(i.getArguments()[0]));
            assertThrows(InvalidPasswordException.class, () -> fileService.downloadById(new FileDownloadRequestDto(testFileId.toString(), "incorrect")),
                    "Скачивание файла должно завершиться с ошибкой InvalidPasswordException, если пароль задан и в запросе был передан неверный пароль");
        }

        @Test
        @DisplayName("Негативный тест: атомарное обновление вернуло 0, т.к. в конкурентной среде файл истёк раньше")
        public void shouldThrow_whenAtomicFileStatisticsUpdateReturns0BecauseFileExpired() {
            File testFileCopy = File.builder()
                    .setId(testFile.getId())
                    .setPath(testFile.getPath())
                    .setMaxDownloads(testFile.getMaxDownloads())
                    .setPasswordHash(testFile.getPasswordHash())
                    .setFileStatistics(testFile.getFileStatistics())
                    .setExpireAt(Instant.now().plusSeconds(10000))
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(fileRepository.findById(eq(testFileCopy.getId()))).thenReturn(Optional.of(testFileCopy));
            when(fileRepository.findForUpdateWithDetailsById(eq(testFileCopy.getId()))).thenReturn(Optional.of(testFileCopy));
            when(fileStorageRepository.fileExistsByPath(eq(testFileCopy.getPath()))).thenReturn(true);
            when(passwordEncoder.matches(any(CharSequence.class), eq(testFileCopy.getPasswordHash()))).thenAnswer(i -> TEST_PASSWORD.equals(i.getArguments()[0]));
            when(fileStatisticsRepository.onDownload(eq(testFileCopy.getFileStatistics().getId()), any(Long.class), any(Instant.class))).thenAnswer(args -> {
                testFileCopy.setExpireAt(Instant.now().minusMillis(1)); // не успели
                return 0;
            });

            FileDownloadRequestDto fileDownloadRequestDto = new FileDownloadRequestDto(
                    testFileCopy.getId().toString(),
                    TEST_PASSWORD
            );

            FileExpiredException e = assertThrows(FileExpiredException.class, () -> fileService.downloadById(fileDownloadRequestDto));
            assertEquals("Срок жизни файла истёк", e.getMessage());
        }

        @Test
        @DisplayName("Негативный тест: атомарное обновление вернуло 0, т.к. в конкурентной среде превысился лимит скачиваний")
        public void shouldThrow_whenAtomicFileStatisticsUpdateReturns0BecauseDownloadLimitReached() {
            FileStatistics testFileStatisticsCopy = FileStatistics.builder()
                    .setId(testFileStatistics.getId())
                    .setTimesDownloaded(testFile.getMaxDownloads() - 1)
                    .setSizeBytes(testFileStatistics.getSizeBytes())
                    .setLastDownloadedAt(testFileStatistics.getLastDownloadedAt())
                    .build();

            File testFileCopy = File.builder()
                    .setId(testFile.getId())
                    .setPath(testFile.getPath())
                    .setMaxDownloads(testFile.getMaxDownloads())
                    .setPasswordHash(testFile.getPasswordHash())
                    .setFileStatistics(testFileStatisticsCopy)
                    .setExpireAt(Instant.now().plusSeconds(10000))
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(fileRepository.findById(eq(testFileCopy.getId()))).thenReturn(Optional.of(testFileCopy));
            when(fileRepository.findForUpdateWithDetailsById(eq(testFileCopy.getId()))).thenReturn(Optional.of(testFileCopy));
            when(fileStorageRepository.fileExistsByPath(eq(testFileCopy.getPath()))).thenReturn(true);
            when(passwordEncoder.matches(any(CharSequence.class), eq(testFileCopy.getPasswordHash()))).thenAnswer(i -> TEST_PASSWORD.equals(i.getArguments()[0]));
            when(fileStatisticsRepository.onDownload(eq(testFileCopy.getFileStatistics().getId()), any(Long.class), any(Instant.class))).thenAnswer(args -> {
                testFileStatisticsCopy.setTimesDownloaded(testFile.getMaxDownloads());
                return 0;
            });

            FileDownloadRequestDto fileDownloadRequestDto = new FileDownloadRequestDto(
                    testFileCopy.getId().toString(),
                    TEST_PASSWORD
            );

            DownloadLimitExceededException e = assertThrows(DownloadLimitExceededException.class, () -> fileService.downloadById(fileDownloadRequestDto));
            assertEquals("Достигнуто максимальное количество скачиваний", e.getMessage());
        }

        @Test
        @DisplayName("Негативный тест: атомарное обновление вернуло 0, по другим причинам")
        public void shouldThrow_whenAtomicFileStatisticsUpdateReturns0() {
            FileStatistics testFileStatisticsCopy = FileStatistics.builder()
                    .setId(testFileStatistics.getId())
                    .setTimesDownloaded(testFile.getMaxDownloads() - 1)
                    .setSizeBytes(testFileStatistics.getSizeBytes())
                    .setLastDownloadedAt(testFileStatistics.getLastDownloadedAt())
                    .build();

            File testFileCopy = File.builder()
                    .setId(testFile.getId())
                    .setPath(testFile.getPath())
                    .setMaxDownloads(testFile.getMaxDownloads())
                    .setPasswordHash(testFile.getPasswordHash())
                    .setFileStatistics(testFileStatisticsCopy)
                    .setExpireAt(Instant.now().plusSeconds(10000))
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(fileRepository.findById(eq(testFileCopy.getId()))).thenReturn(Optional.of(testFileCopy));
            when(fileRepository.findForUpdateWithDetailsById(eq(testFileCopy.getId()))).thenReturn(Optional.of(testFileCopy));
            when(fileStorageRepository.fileExistsByPath(eq(testFileCopy.getPath()))).thenReturn(true);
            when(passwordEncoder.matches(any(CharSequence.class), eq(testFileCopy.getPasswordHash()))).thenAnswer(i -> TEST_PASSWORD.equals(i.getArguments()[0]));
            when(fileStatisticsRepository.onDownload(eq(testFileCopy.getFileStatistics().getId()), any(Long.class), any(Instant.class))).thenReturn(0);

            FileDownloadRequestDto fileDownloadRequestDto = new FileDownloadRequestDto(
                    testFileCopy.getId().toString(),
                    TEST_PASSWORD
            );

            FileNotFoundException e = assertThrows(FileNotFoundException.class, () -> fileService.downloadById(fileDownloadRequestDto));
            assertEquals("Файл недоступен для скачивания.", e.getMessage());
        }

        @Test
        @DisplayName("Негативный тест: ошибка чтения из хранилища")
        public void shouldThrow_whenStorageDownloadFails() {
            FileStatistics testFileStatisticsCopy = FileStatistics.builder()
                    .setId(testFileStatistics.getId())
                    .setTimesDownloaded(testFile.getMaxDownloads() - 1)
                    .setSizeBytes(testFileStatistics.getSizeBytes())
                    .setLastDownloadedAt(testFileStatistics.getLastDownloadedAt())
                    .build();

            File testFileCopy = File.builder()
                    .setId(testFile.getId())
                    .setPath(testFile.getPath())
                    .setMaxDownloads(testFile.getMaxDownloads())
                    .setPasswordHash(testFile.getPasswordHash())
                    .setFileStatistics(testFileStatisticsCopy)
                    .setExpireAt(Instant.now().plusSeconds(10000))
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(fileRepository.findById(eq(testFileCopy.getId()))).thenReturn(Optional.of(testFileCopy));
            when(fileRepository.findForUpdateWithDetailsById(eq(testFileCopy.getId()))).thenReturn(Optional.of(testFileCopy));
            when(fileStorageRepository.fileExistsByPath(eq(testFileCopy.getPath()))).thenReturn(true);
            when(passwordEncoder.matches(any(CharSequence.class), eq(testFileCopy.getPasswordHash()))).thenAnswer(i -> TEST_PASSWORD.equals(i.getArguments()[0]));
            when(fileStatisticsRepository.onDownload(eq(testFileCopy.getFileStatistics().getId()), any(Long.class), any(Instant.class))).thenReturn(1);
            when(fileStorageRepository.downloadByPath(eq(testFileCopy.getPath()))).thenThrow(new RuntimeException("Ошибка чтения"));

            FileDownloadRequestDto fileDownloadRequestDto = new FileDownloadRequestDto(
                    testFileCopy.getId().toString(),
                    TEST_PASSWORD
            );

            assertThrows(RuntimeException.class, () -> fileService.downloadById(fileDownloadRequestDto));
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteByIdTests {
        @Test
        @DisplayName("Позитивный тест: удаление файла его владельцем")
        public void shouldDeleteFile_whenDeleteByAuthorUser() {
            testUserStorageQuota.setUsedStorageBytes(testFile.getFileStatistics().getSizeBytes());

            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(fileRepository.findForUpdateWithDetailsById(testFile.getId())).thenReturn(Optional.of(testFile));
            doAnswer(args -> {
                Long delta = args.getArgument(1);
                testUserStorageQuota.setUsedStorageBytes(testUserStorageQuota.getUsedStorageBytes() + delta);
                return null;
            }).when(storageQuotaService).updateStorageQuota(eq(testUserStorageQuota.getId()), any(Long.class));

            fileService.deleteById(testFile.getId().toString());

            verify(fileStorageRepository, times(1)).deleteByPath(testFilePath);
            verify(fileRepository, times(1)).deleteById(testFile.getId());

            Long expectedDelta = -testFile.getFileStatistics().getSizeBytes();
            verify(storageQuotaService, times(1)).updateStorageQuota(testUserStorageQuota.getId(), expectedDelta);
        }

        @Test
        @DisplayName("Позитивный тест: удаление файла админом")
        public void shouldDeleteFile_whenDeleteByAdmin() {
            testUserStorageQuota.setUsedStorageBytes(testFile.getFileStatistics().getSizeBytes());

            when(requestContextService.getUserId()).thenReturn(testAdmin.getId());
            when(userRepository.findById(testAdmin.getId())).thenReturn(Optional.of(testAdmin));
            when(fileRepository.findForUpdateWithDetailsById(testFile.getId())).thenReturn(Optional.of(testFile));
            doAnswer(args -> {
                Long delta = args.getArgument(1);
                testUserStorageQuota.setUsedStorageBytes(testUserStorageQuota.getUsedStorageBytes() + delta);
                return null;
            }).when(storageQuotaService).updateStorageQuota(eq(testUserStorageQuota.getId()), any(Long.class));

            fileService.deleteById(testFile.getId().toString());

            verify(fileStorageRepository, times(1)).deleteByPath(testFilePath);
            verify(fileRepository, times(1)).deleteById(testFile.getId());

            Long expectedDelta = -testFile.getFileStatistics().getSizeBytes();
            verify(storageQuotaService, times(1)).updateStorageQuota(testUserStorageQuota.getId(), expectedDelta);
        }

        @Test
        @DisplayName("Негативный тест: удаление должно завершиться с ошибкой, если запрашивающий удаление пользователь не найден")
        public void shouldThrow_whenDeleteUserNotFound() {
            when(requestContextService.getUserId()).thenReturn(123L);
            when(userRepository.findById(123L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> fileService.deleteById(testFile.getId().toString()),
                    "Удаление должно завершиться с ошибкой EntityNotFoundException, если запрашивающий удаление пользователь не найден");
        }

        @Test
        @DisplayName("Негативный тест: удаление должно завершиться с ошибкой, если метаданные файла не найдены в БД")
        public void shouldThrow_whenDeleteFileMetadataNotFound() {
            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

            assertThrows(EntityNotFoundException.class, () -> fileService.deleteById(testFile.getId().toString()),
                    "Удаление должно завершиться с ошибкой EntityNotFoundException, если метаданные файла не найдены в БД");
        }

        @Test
        @DisplayName("Негативный тест: удаление должно завершиться с ошибкой, если файл удаляет пользователь-не-автор файла")
        public void shouldThrow_whenDeleteUserIsNotFileAuthor() {
            User notAuthor = User.builder()
                    .setId(123L)
                    .setRole(UserRole.USER)
                    .build();

            when(requestContextService.getUserId()).thenReturn(123L);
            when(userRepository.findById(123L)).thenReturn(Optional.of(notAuthor));
            when(fileRepository.findForUpdateWithDetailsById(testFile.getId())).thenReturn(Optional.of(testFile));

            assertThrows(IllegalFileAccessException.class, () -> fileService.deleteById(testFile.getId().toString()),
                    "Удаление должно завершиться с ошибкой IllegalFileAccessException, если файл удаляет пользователь-не-автор файла");
        }
    }
}
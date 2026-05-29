package ru.LevLezhnin.NauJava.service.implementations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.LevLezhnin.NauJava.dto.auth.RegistrationRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UpdateUserRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UserProfileAdminResponseDto;
import ru.LevLezhnin.NauJava.dto.user.UserProfileResponseDto;
import ru.LevLezhnin.NauJava.exception.common.EntityNotFoundException;
import ru.LevLezhnin.NauJava.exception.common.InvalidPasswordException;
import ru.LevLezhnin.NauJava.exception.common.InvalidSearchCriteriaException;
import ru.LevLezhnin.NauJava.exception.user.EmailTakenException;
import ru.LevLezhnin.NauJava.exception.user.InvalidLoginException;
import ru.LevLezhnin.NauJava.exception.user.UsernameTakenException;
import ru.LevLezhnin.NauJava.model.*;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.repository.search.user.UserSearchStrategy;
import ru.LevLezhnin.NauJava.security.context.RequestContextService;
import ru.LevLezhnin.NauJava.service.interfaces.StorageQuotaService;
import ru.LevLezhnin.NauJava.utils.DataSizeConstants;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StorageQuotaService storageQuotaService;
    @Mock
    private RequestContextService requestContextService;
    @Mock
    private UserSearchStrategy mockUsernameSearchStrategy;

    private final String testUserPassword = "testUserPassword";
    private User testUser, testAdmin;
    private StorageQuota testUserStorageQuota, testAdminStorageQuota;

    private UserServiceImpl userService;

    @BeforeEach
    public void setUp() {
        mockUsernameSearchStrategy = mock(UserSearchStrategy.class);
        when(mockUsernameSearchStrategy.getCriteriaKey()).thenReturn("username");

        userService = new UserServiceImpl(
                userRepository,
                requestContextService,
                passwordEncoder,
                List.of(mockUsernameSearchStrategy),
                storageQuotaService
        );

        testAdminStorageQuota = StorageQuota.builder()
                .setId(2L)
                .setUser(null)
                .setUsedStorageBytes(0L)
                .setMaxStorageBytes(100L)
                .build();

        testUserStorageQuota = StorageQuota.builder()
                .setId(1L)
                .setUser(null)
                .setUsedStorageBytes(0L)
                .setMaxStorageBytes(10L)
                .build();

        testAdmin = User.builder()
                .setId(1L)
                .setUsername("testAdmin")
                .setEmail("testAdmin@testAdmin.com")
                .setPasswordHash("testAdminPasswordHash")
                .setActive(true)
                .setRole(UserRole.ADMIN)
                .setRegisteredAt(Instant.now())
                .setStorageQuota(testAdminStorageQuota)
                .build();

        testAdminStorageQuota.setUser(testAdmin);

        testUser = User.builder()
                .setId(2L)
                .setUsername("testUser")
                .setEmail("testUser@testUser.com")
                .setPasswordHash("testUserPasswordHash")
                .setActive(true)
                .setRole(UserRole.USER)
                .setRegisteredAt(Instant.now())
                .setStorageQuota(testUserStorageQuota)
                .build();

        testUserStorageQuota.setUser(testUser);
    }

    @Nested
    @DisplayName("createUser")
    class CreateUserTests {

        private RegistrationRequestDto createRegistrationDtoByUser(User user, String userPassword) {
            return new RegistrationRequestDto(user.getUsername(), user.getEmail(), userPassword);
        }

        @Test
        @DisplayName("Позитивный тест: регистрация пользователя")
        public void shouldCreateUser() {
            RegistrationRequestDto registrationRequestDto = createRegistrationDtoByUser(testUser, testUserPassword);
            StorageQuota.Builder storageQuotaBuilder = StorageQuota.builder().setMaxStorageBytes(DataSizeConstants.GB).setUsedStorageBytes(0L);

            when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.empty());
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.empty());
            when(storageQuotaService.getQuotaBuilder(QuotaTariffs.BASIC)).thenReturn(storageQuotaBuilder);
            when(passwordEncoder.encode(testUserPassword)).thenReturn(testUser.getPasswordHash());
            when(userRepository.save(any(User.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

            UserProfileResponseDto result = userService.createUser(registrationRequestDto);

            assertAll(
                    () -> assertNotNull(result, "Сохранённый пользователь пуст"),
                    () -> assertEquals(testUser.getUsername(), result.username(), "Логин сохранённого пользователя не совпадает с тестовым"),
                    () -> assertEquals(testUser.getEmail(), result.email(), "Email сохранённого пользователя не совпадает с тестовым"),
                    () -> assertEquals(UserRole.USER.name(), result.role(), "Роль сохранённого пользователя не совпала с ожидаемой"),
                    () -> verify(userRepository).save(any(User.class)),
                    () -> verify(storageQuotaService).getQuotaBuilder(QuotaTariffs.BASIC),
                    () -> verify(passwordEncoder).encode(testUserPassword)
            );
        }

        @Test
        @DisplayName("Негативный тест: регистрация пользователя с логином, который уже существует")
        public void shouldThrow_whenCreateUserWithDuplicateLogin() {
            RegistrationRequestDto registrationRequestDto = createRegistrationDtoByUser(testUser, testUserPassword);
            when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
            assertThrows(UsernameTakenException.class, () -> userService.createUser(registrationRequestDto));
        }

        @Test
        @DisplayName("Негативный тест: регистрация пользователя с email-ом, который уже существует")
        public void shouldThrow_whenCreateUserWithDuplicateEmail() {
            RegistrationRequestDto registrationRequestDto = createRegistrationDtoByUser(testUser, testUserPassword);
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            assertThrows(EmailTakenException.class, () -> userService.createUser(registrationRequestDto));
        }

        @Test
        @DisplayName("Негативный тест: при DataIntegrityViolation и username и email свободны")
        public void shouldRecheckAndThrow_whenDataIntegrityViolationAndUsernameAndEmailAreFree() {
            RegistrationRequestDto registrationRequestDto = createRegistrationDtoByUser(testUser, testUserPassword);

            when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException(""));
            when(userRepository.findByUsername(eq(testUser.getUsername()))).thenReturn(Optional.empty());
            when(userRepository.findByEmail(eq(testUser.getEmail()))).thenReturn(Optional.empty());
            when(storageQuotaService.getQuotaBuilder(eq(QuotaTariffs.BASIC))).thenReturn(
                    StorageQuota.builder()
                            .setUsedStorageBytes(testUserStorageQuota.getUsedStorageBytes())
                            .setMaxStorageBytes(testUserStorageQuota.getMaxStorageBytes()));

            assertThrows(DataIntegrityViolationException.class, () -> userService.createUser(registrationRequestDto));
            verify(userRepository, times(2)).findByUsername(eq(testUser.getUsername()));
            verify(userRepository, times(2)).findByEmail(eq(testUser.getEmail()));
        }

        @Test
        @DisplayName("Негативный тест: при DataIntegrityViolation в конкурентной среде username заняли раньше")
        public void shouldRecheckUsernameAndThrow_whenDataIntegrityViolationAndUsernameIsNotUnique() {
            RegistrationRequestDto registrationRequestDto = createRegistrationDtoByUser(testUser, testUserPassword);

            when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException(""));
            when(userRepository.findByUsername(eq(testUser.getUsername())))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(new User()));
            when(userRepository.findByEmail(eq(testUser.getEmail()))).thenReturn(Optional.empty());
            when(storageQuotaService.getQuotaBuilder(eq(QuotaTariffs.BASIC))).thenReturn(
                    StorageQuota.builder()
                            .setUsedStorageBytes(testUserStorageQuota.getUsedStorageBytes())
                            .setMaxStorageBytes(testUserStorageQuota.getMaxStorageBytes()));

            assertThrows(UsernameTakenException.class, () -> userService.createUser(registrationRequestDto));
            verify(userRepository, times(2)).findByUsername(eq(testUser.getUsername()));
            verify(userRepository, times(1)).findByEmail(eq(testUser.getEmail()));
        }

        @Test
        @DisplayName("Негативный тест: при DataIntegrityViolation в конкурентной среде email заняли раньше")
        public void shouldRecheckUsernameThenEmailAndThrow_whenDataIntegrityViolationAndEmailIsNotUnique() {
            RegistrationRequestDto registrationRequestDto = createRegistrationDtoByUser(testUser, testUserPassword);

            when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException(""));
            when(userRepository.findByUsername(eq(testUser.getUsername()))).thenReturn(Optional.empty());
            when(userRepository.findByEmail(eq(testUser.getEmail())))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(new User()));
            when(storageQuotaService.getQuotaBuilder(eq(QuotaTariffs.BASIC))).thenReturn(
                    StorageQuota.builder()
                            .setUsedStorageBytes(testUserStorageQuota.getUsedStorageBytes())
                            .setMaxStorageBytes(testUserStorageQuota.getMaxStorageBytes()));

            assertThrows(EmailTakenException.class, () -> userService.createUser(registrationRequestDto));
            verify(userRepository, times(2)).findByUsername(eq(testUser.getUsername()));
            verify(userRepository, times(2)).findByEmail(eq(testUser.getEmail()));
        }
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfileTests {

        private UserProfileResponseDto getProfileByUser(User user) {
            return new UserProfileResponseDto(
                    user.getUsername(),
                    user.getEmail(),
                    user.getRegisteredAt(),
                    user.getRole() != null ? user.getRole().name() : null
            );
        }

        @Test
        @DisplayName("Позитивный тест: должен вернуть профиль, если пользователь есть в системе и имеет роль")
        void shouldReturnProfile_whenUserExistsAndHasRole() {
            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(userRepository.findById(eq(testUser.getId()))).thenReturn(Optional.of(testUser));

            assertEquals(getProfileByUser(testUser), userService.getProfile());
            verify(requestContextService, times(1)).getUserId();
            verify(userRepository, times(1)).findById(eq(testUser.getId()));
        }

        @Test
        @DisplayName("Позитивный тест: должен вернуть профиль, если пользователь есть в системе и имеет не имеет роли")
        void shouldReturnProfile_whenUserExistsAndHasNoRole() {

            User testUserNoRole = User.builder()
                    .setId(testUser.getId())
                    .setUsername(testUser.getUsername())
                    .setEmail(testUser.getEmail())
                    .setPasswordHash(testUser.getPasswordHash())
                    .setActive(testUser.isActive())
                    .setRegisteredAt(testUser.getRegisteredAt())
                    .setRole(null)
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUserNoRole.getId());
            when(userRepository.findById(eq(testUserNoRole.getId()))).thenReturn(Optional.of(testUserNoRole));

            assertEquals(getProfileByUser(testUserNoRole), userService.getProfile());
            verify(requestContextService, times(1)).getUserId();
            verify(userRepository, times(1)).findById(eq(testUser.getId()));
        }

        @Test
        @DisplayName("Негативный тест: должен выбросить исключение, если запрошен профиль несуществующего пользователя")
        void shouldThrow_whenUserDoesNotExist() {
            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(userRepository.findById(eq(testUser.getId()))).thenReturn(Optional.empty());

            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> userService.getProfile());
            assertTrue(e.getMessage().startsWith("Пользователь с id: "));
            verify(requestContextService, times(1)).getUserId();
            verify(userRepository, times(1)).findById(eq(testUser.getId()));
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUserTests {

        private final String newUsername = "newUsername";

        private final String newPassword = "newPassword";
        private final String newPasswordHash = "newPasswordHash";

        private final String someOtherPassword = "someOtherPassword";
        private final String someOtherPasswordHash = "someOtherPasswordHash";

        @Test
        @DisplayName("Позитивный тест: пользователь успешно обновил логин")
        void shouldUpdate_whenValidLoginUpdateRequest() {
            UpdateUserRequestDto updateUserRequestDto = new UpdateUserRequestDto(newUsername, null, null, null);

            User testUserCopy = User.builder()
                    .setId(testUser.getId())
                    .setUsername(testUser.getUsername())
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUserCopy.getId());
            when(userRepository.findById(eq(testUserCopy.getId()))).thenReturn(Optional.of(testUserCopy));
            when(userRepository.findByUsername(eq(newUsername))).thenReturn(Optional.empty());

            userService.updateUser(updateUserRequestDto);
            assertEquals(newUsername, testUserCopy.getUsername());
            verify(requestContextService, times(1)).getUserId();
            verify(userRepository, times(1)).findById(eq(testUserCopy.getId()));
            verify(userRepository, times(1)).findByUsername(eq(newUsername));
            verify(userRepository, times(1)).save(eq(testUserCopy));
        }

        @Test
        @DisplayName("Негативный тест: пользователь не найден")
        void shouldThrow_whenUserNotFound() {
            UpdateUserRequestDto updateUserRequestDto = new UpdateUserRequestDto(newUsername, testUserPassword, someOtherPassword, someOtherPassword);

            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> userService.updateUser(updateUserRequestDto));
            assertTrue(e.getMessage().startsWith("Пользователь с id: "));

            verify(requestContextService, times(1)).getUserId();
            verify(userRepository, times(1)).findById(eq(testUser.getId()));
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(userRepository, never()).findByUsername(eq(newUsername));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Негативный тест: новый логин совпадает со старым")
        void shouldThrow_whenNewUsernameEqualsOldUsername() {
            UpdateUserRequestDto updateUserRequestDto = new UpdateUserRequestDto(newUsername, null, null, null);

            User testUserCopy = User.builder()
                    .setId(testUser.getId())
                    .setUsername(newUsername)
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUserCopy.getId());
            when(userRepository.findById(eq(testUserCopy.getId()))).thenReturn(Optional.of(testUserCopy));

            InvalidLoginException e = assertThrows(InvalidLoginException.class, () -> userService.updateUser(updateUserRequestDto));
            assertEquals("Новый логин должен отличаться от старого", e.getMessage());
            verify(requestContextService, times(1)).getUserId();
            verify(userRepository, times(1)).findById(eq(testUserCopy.getId()));
            verify(userRepository, never()).findByUsername(eq(newUsername));
            verify(userRepository, never()).save(eq(testUserCopy));
        }

        @Test
        @DisplayName("Негативный тест: новый логин занят")
        void shouldThrow_whenNewUsernameIsTaken() {
            UpdateUserRequestDto updateUserRequestDto = new UpdateUserRequestDto(newUsername, null, null, null);

            User testUserCopy = User.builder()
                    .setId(testUser.getId())
                    .setUsername(testUser.getUsername())
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUserCopy.getId());
            when(userRepository.findById(eq(testUserCopy.getId()))).thenReturn(Optional.of(testUserCopy));
            when(userRepository.findByUsername(eq(newUsername))).thenReturn(Optional.of(new User()));

            UsernameTakenException e = assertThrows(UsernameTakenException.class, () -> userService.updateUser(updateUserRequestDto));
            assertTrue(e.getMessage().startsWith("Пользователь с логином "));
            verify(requestContextService, times(1)).getUserId();
            verify(userRepository, times(1)).findById(eq(testUserCopy.getId()));
            verify(userRepository, times(1)).findByUsername(eq(newUsername));
            verify(userRepository, never()).save(eq(testUserCopy));
        }

        @Test
        @DisplayName("Позитивный тест: пользователь успешно обновил пароль")
        void shouldUpdate_whenValidPasswordUpdateRequest() {
            UpdateUserRequestDto updateUserRequestDto = new UpdateUserRequestDto(null, testUserPassword, newPassword, newPassword);
            String oldPasswordHash = testUser.getPasswordHash();
            User testUserCopy = User.builder()
                    .setId(testUser.getId())
                    .setPasswordHash(oldPasswordHash)
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUserCopy.getId());
            when(userRepository.findById(eq(testUserCopy.getId()))).thenReturn(Optional.of(testUserCopy));
            when(passwordEncoder.encode(eq(testUserPassword))).thenReturn(testUserCopy.getPasswordHash());
            when(passwordEncoder.encode(eq(newPassword))).thenReturn(newPasswordHash);
            when(passwordEncoder.matches(anyString(), anyString()))
                    .thenAnswer(args -> passwordEncoder.encode(args.getArgument(0)).equals(args.getArgument(1)));

            userService.updateUser(updateUserRequestDto);
            assertEquals(newPasswordHash, testUserCopy.getPasswordHash());
            verify(requestContextService, times(1)).getUserId();
            verify(userRepository, times(1)).findById(eq(testUserCopy.getId()));
            verify(passwordEncoder, times(1)).matches(eq(newPassword), eq(oldPasswordHash));
            verify(passwordEncoder, times(1)).matches(eq(updateUserRequestDto.currentPassword()), eq(oldPasswordHash));
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Негативный тест: новый пароль совпадает со старым")
        void shouldThrow_whenNewPasswordMatchesOldPassword() {
            UpdateUserRequestDto updateUserRequestDto = new UpdateUserRequestDto(null, testUserPassword, newPassword, newPassword);
            User testUserCopy = User.builder()
                    .setId(testUser.getId())
                    .setPasswordHash(newPasswordHash)
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUserCopy.getId());
            when(userRepository.findById(eq(testUserCopy.getId()))).thenReturn(Optional.of(testUserCopy));
            when(passwordEncoder.encode(eq(newPassword))).thenReturn(newPasswordHash);
            when(passwordEncoder.matches(anyString(), anyString()))
                    .thenAnswer(args -> passwordEncoder.encode(args.getArgument(0)).equals(args.getArgument(1)));

            InvalidPasswordException e = assertThrows(InvalidPasswordException.class, () -> userService.updateUser(updateUserRequestDto));
            assertEquals("Новый пароль должен отличаться от старого", e.getMessage());
            verify(requestContextService, times(1)).getUserId();
            verify(userRepository, times(1)).findById(eq(testUserCopy.getId()));
            verify(passwordEncoder, times(1)).matches(eq(newPassword), eq(testUserCopy.getPasswordHash()));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Негативный тест: старый пароль не совпадает с переданным в запросе")
        void shouldThrow_whenCurrentPasswordDoesNotMatchOldPassword() {

            UpdateUserRequestDto updateUserRequestDto = new UpdateUserRequestDto(null, someOtherPassword, newPassword, newPassword);
            User testUserCopy = User.builder()
                    .setId(testUser.getId())
                    .setPasswordHash(testUser.getPasswordHash())
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUserCopy.getId());
            when(userRepository.findById(eq(testUserCopy.getId()))).thenReturn(Optional.of(testUserCopy));
            when(passwordEncoder.encode(eq(newPassword))).thenReturn(newPasswordHash);
            when(passwordEncoder.encode(eq(someOtherPassword))).thenReturn(someOtherPasswordHash);
            when(passwordEncoder.matches(anyString(), anyString()))
                    .thenAnswer(args -> passwordEncoder.encode(args.getArgument(0)).equals(args.getArgument(1)));

            InvalidPasswordException e = assertThrows(InvalidPasswordException.class, () -> userService.updateUser(updateUserRequestDto));
            assertEquals("Указан неверный текущий пароль", e.getMessage());
            verify(requestContextService, times(1)).getUserId();
            verify(userRepository, times(1)).findById(eq(testUserCopy.getId()));
            verify(passwordEncoder, times(1)).matches(eq(newPassword), eq(testUserCopy.getPasswordHash()));
            verify(passwordEncoder, times(1)).matches(eq(someOtherPassword), eq(testUserCopy.getPasswordHash()));
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUserTests {
        @Test
        @DisplayName("Удаление обычного пользователя: обнуляет авторов файлов и удаляет запись")
        void shouldClearFileAuthorAndDelete_whenDeleteRegularUser() {

            File file1 = new File(); file1.setAuthor(testUser);
            File file2 = new File(); file2.setAuthor(testUser);
            testUser.getActiveFiles().addAll(List.of(file1, file2));

            UserBan banGiven = new UserBan();
            banGiven.setAdmin(testAdmin);
            banGiven.setBannedUser(testUser);
            testUser.getBanHistory().add(banGiven);

            when(requestContextService.getUserId()).thenReturn(testUser.getId());
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

            userService.deleteUser();

            assertNull(file1.getAuthor(), "Ссылка на автора у файла должна быть обнулена");
            assertNull(file2.getAuthor(), "Ссылка на автора у файла должна быть обнулена");
            assertTrue(testUser.getActiveFiles().isEmpty(), "Коллекция активных файлов должна быть очищена");
            assertTrue(testUser.getBanHistory().isEmpty(), "История банов пользователя должна быть очищена");
            verify(userRepository, times(1)).delete(testUser);
        }

        @Test
        @DisplayName("Удаление Администратора: очищает файлы, выданные баны и историю банов")
        void shouldClearAllRelationsAndDelete_whenDeleteAdmin() {

            File file = new File(); file.setAuthor(testAdmin);
            UserBan banGiven = new UserBan();
            banGiven.setAdmin(testAdmin);

            UserBan banTaken = new UserBan();
            testAdmin.setRole(UserRole.USER); //админа могли заблокировать только тогда, когда он был пользователем
            banTaken.setBannedUser(testAdmin);
            testAdmin.setRole(UserRole.ADMIN);

            testAdmin.getActiveFiles().add(file);
            testAdmin.getProvidedBans().add(banGiven);
            testAdmin.getBanHistory().add(banTaken);

            when(requestContextService.getUserId()).thenReturn(testAdmin.getId());
            when(userRepository.findById(testAdmin.getId())).thenReturn(Optional.of(testAdmin));

            userService.deleteUser();

            assertNull(file.getAuthor());
            assertTrue(testAdmin.getActiveFiles().isEmpty());

            assertNull(banGiven.getAdmin(), "Поле admin у выданного бана должно быть обнулено");
            assertTrue(testAdmin.getProvidedBans().isEmpty());
            assertTrue(testAdmin.getBanHistory().isEmpty());
            verify(userRepository, times(1)).delete(testAdmin);
        }
    }

    @Nested
    @DisplayName("findByCriteria")
    class FindByCriteriaTests {
        @Test
        @DisplayName("Позитивный: поиск по стратегии")
        void shouldReturnFoundUsers_whenValidSearchCriteriaKeyProvided() {
            String criteria = "username";
            String value = testUser.getUsername();
            Page<User> pageResult = new PageImpl<>(List.of(testUser));

            when(userRepository.findById(any(Long.class))).thenReturn(Optional.of(testAdmin));
            when(mockUsernameSearchStrategy.getSpecification(value)).thenReturn(Specification.unrestricted());
            when(userRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(pageResult);

            List<UserProfileAdminResponseDto> result = userService.findByCriteria(criteria, value, 0, 10);

            assertEquals(1, result.size());
            assertEquals(value, result.getFirst().username());
        }

        @Test
        @DisplayName("Негативный тест: неверный параметр поиска")
        void shouldThrow_whenInvalidSearchCriteriaKeyProvided() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testAdmin));
            InvalidSearchCriteriaException ex = assertThrows(InvalidSearchCriteriaException.class,
                    () -> userService.findByCriteria("invalid_field", "value", 0, 10));
            assertTrue(ex.getMessage().contains("Неверный параметр"));
        }

        @Test
        @DisplayName("Негативный тест: админ не найден")
        void shouldThrow_whenAdminNotFound() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.empty());
            when(mockUsernameSearchStrategy.getCriteriaKey()).thenReturn("username");
            EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                    () -> userService.findByCriteria(
                            mockUsernameSearchStrategy.getCriteriaKey(),
                            testUser.getUsername(),
                            0,
                            1)
            );
            assertTrue(e.getMessage().startsWith("Администратор с id: "));
        }

        @Test
        @DisplayName("Негативный тест: операцию вызвал не администратор")
        void shouldThrow_whenNotAnAdminExecuted() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(mockUsernameSearchStrategy.getCriteriaKey()).thenReturn("username");
            AccessDeniedException e = assertThrows(AccessDeniedException.class,
                    () -> userService.findByCriteria(
                            mockUsernameSearchStrategy.getCriteriaKey(),
                            testUser.getUsername(),
                            0,
                            1)
            );
            assertEquals("Недостаточно прав для выполнения операции", e.getMessage());
        }
    }
}
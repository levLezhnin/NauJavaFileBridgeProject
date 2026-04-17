package ru.LevLezhnin.NauJava.service.implementations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.LevLezhnin.NauJava.dto.auth.RegistrationRequestDto;
import ru.LevLezhnin.NauJava.model.*;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.repository.user.search.UserSearchStrategy;
import ru.LevLezhnin.NauJava.service.interfaces.StorageQuotaService;
import ru.LevLezhnin.NauJava.utils.DataSizeConstants;
import ru.LevLezhnin.NauJava.utils.RequestContextService;

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
    }

    @Test
    @DisplayName("Позитивный тест: регистрация пользователя")
    public void shouldCreateUser() {
        String username = "test";
        String email = "test@test.com";
        String rawPassword = "123456";
        String encodedPassword = "encoded";
        RegistrationRequestDto registrationRequestDto = new RegistrationRequestDto(username, email, rawPassword);
        StorageQuota.Builder storageQuotaBuilder = StorageQuota.builder().setMaxStorageBytes(DataSizeConstants.GB).setUsedStorageBytes(0L);

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(storageQuotaService.getQuotaBuilder(QuotaTariffs.BASIC)).thenReturn(storageQuotaBuilder);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        User result = userService.createUser(registrationRequestDto);

        assertAll(
                () -> assertNotNull(result, "Сохранённый пользователь пуст"),
                () -> assertEquals(username, result.getUsername(), "Логин сохранённого пользователя не совпадает с тестовым"),
                () -> assertEquals(email, result.getEmail(), "Email сохранённого пользователя не совпадает с тестовым"),
                () -> assertEquals(encodedPassword, result.getPasswordHash(), "Хеш пароля сохранённого пользователя не совпадает с тестовым"),
                () -> assertEquals(UserRole.USER, result.getRole(), "Роль сохранённого пользователя не совпала с ожидаемой"),
                () -> assertTrue(result.isActive(), "Сохранённый пользователь не активен"),
                () -> verify(userRepository).save(any(User.class)),
                () -> verify(storageQuotaService).getQuotaBuilder(QuotaTariffs.BASIC),
                () -> verify(passwordEncoder).encode(rawPassword)
        );
    }

    @Test
    @DisplayName("Негативный тест: регистрация пользователя с логином, который уже существует")
    public void shouldThrow_whenCreateUserWithDuplicateLogin() {
        String username = "test";
        String email = "test@test.com";
        String rawPassword = "123456";
        RegistrationRequestDto registrationRequestDto = new RegistrationRequestDto(username, email, rawPassword);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(registrationRequestDto));
    }

    @Test
    @DisplayName("Негативный тест: регистрация пользователя с email-ом, который уже существует")
    public void shouldThrow_whenCreateUserWithDuplicateEmail() {
        String username = "test";
        String email = "test@test.com";
        String rawPassword = "123456";
        RegistrationRequestDto registrationRequestDto = new RegistrationRequestDto(username, email, rawPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(registrationRequestDto));
    }

    @Test
    @DisplayName("Позитивный: поиск по стратегии")
    void shouldReturnFoundUsers_whenValidSearchCriteriaKeyProvided() {
        String criteria = "username";
        String value = "john";
        User foundUser = User.builder().setUsername("john").build();
        Page<User> pageResult = new PageImpl<>(List.of(foundUser));

        when(mockUsernameSearchStrategy.getSpecification(value)).thenReturn(Specification.unrestricted());
        when(userRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(pageResult);

        List<User> result = userService.findByCriteria(criteria, value, 0, 10);

        assertEquals(1, result.size());
        assertEquals(value, result.getFirst().getUsername());
    }

    @Test
    @DisplayName("Негативный: неверный параметр поиска")
    void shouldThrows_whenInvalidSearchCriteriaKeyProvided() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.findByCriteria("invalid_field", "value", 0, 10));
        assertTrue(ex.getMessage().contains("Неверный параметр"));
    }

    @Test
    @DisplayName("Удаление обычного пользователя: обнуляет авторов файлов и удаляет запись")
    void shouldClearFileAuthorAndDelete_whenDeleteRegularUser() {
        Long userId = 1L;
        User user = User.builder()
                .setId(userId)
                .setRole(UserRole.USER)
                .build();

        File file1 = new File(); file1.setAuthor(user);
        File file2 = new File(); file2.setAuthor(user);
        user.getActiveFiles().addAll(List.of(file1, file2));

        when(requestContextService.getUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteUser();

        assertNull(file1.getAuthor(), "Ссылка на автора у файла должна быть обнулена");
        assertNull(file2.getAuthor(), "Ссылка на автора у файла должна быть обнулена");
        assertTrue(user.getActiveFiles().isEmpty(), "Коллекция активных файлов должна быть очищена");
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("Удаление Администратора: очищает файлы, выданные баны и историю банов")
    void shouldClearAllRelationsAndDelete_whenDeleteAdmin() {
        Long userId = 2L;
        User admin = User.builder()
                .setId(userId)
                .setRole(UserRole.ADMIN)
                .build();

        File file = new File(); file.setAuthor(admin);
        UserBan banGiven = new UserBan();
        banGiven.setAdmin(admin);

        admin.getActiveFiles().add(file);
        admin.getProvidedBans().add(banGiven);

        when(requestContextService.getUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));

        userService.deleteUser();

        assertNull(file.getAuthor());
        assertTrue(admin.getActiveFiles().isEmpty());

        assertNull(banGiven.getAdmin(), "Поле admin у выданного бана должно быть обнулено");
        assertTrue(admin.getProvidedBans().isEmpty());

        verify(userRepository).delete(admin);
    }
}
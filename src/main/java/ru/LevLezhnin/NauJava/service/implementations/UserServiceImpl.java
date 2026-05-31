package ru.LevLezhnin.NauJava.service.implementations;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import ru.LevLezhnin.NauJava.metrics.UserMetrics;
import ru.LevLezhnin.NauJava.model.*;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.repository.search.user.UserSearchStrategy;
import ru.LevLezhnin.NauJava.security.context.RequestContextService;
import ru.LevLezhnin.NauJava.service.interfaces.StorageQuotaService;
import ru.LevLezhnin.NauJava.service.interfaces.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final RequestContextService requestContextService;
    private final Map<String, UserSearchStrategy> userSearchStrategyMap;
    private final PasswordEncoder passwordEncryptor;
    private final StorageQuotaService storageQuotaService;

    private final MeterRegistry meterRegistry;
    private final UserMetrics userMetrics;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           RequestContextService requestContextService,
                           PasswordEncoder passwordEncryptor,
                           List<UserSearchStrategy> userSearchStrategies,
                           StorageQuotaService storageQuotaService,
                           MeterRegistry meterRegistry,
                           UserMetrics userMetrics) {
        this.userRepository = userRepository;
        this.requestContextService = requestContextService;
        this.passwordEncryptor = passwordEncryptor;
        this.userSearchStrategyMap = userSearchStrategies.stream().collect(Collectors.toMap(UserSearchStrategy::getCriteriaKey, s -> s));
        this.storageQuotaService = storageQuotaService;
        this.meterRegistry = meterRegistry;
        this.userMetrics = userMetrics;
    }

    private User findById(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id: %d не найден".formatted(id)));
    }

    private User getUserByAuthentication() {
        Long userId = requestContextService.getUserId();
        return findById(userId);
    }

    private void checkUsernameUnique(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            userMetrics.recordUsernameConflict();
            throw new UsernameTakenException("Пользователь с логином '%s' уже существует".formatted(username));
        }
    }

    private void checkEmailUnique(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            userMetrics.recordEmailConflict();
            throw new EmailTakenException("Пользователь с E-mail-ом '%s' уже существует".formatted(email));
        }
    }

    @Override
    @Transactional
    public UserProfileResponseDto createUser(RegistrationRequestDto registrationRequestDto) {

        Timer.Sample userCreateStart = Timer.start(meterRegistry);

        checkUsernameUnique(registrationRequestDto.username());
        checkEmailUnique(registrationRequestDto.email());

        StorageQuota storageQuota = storageQuotaService.getQuotaBuilder(QuotaTariffs.BASIC).build();

        User user = User.builder()
                .setUsername(registrationRequestDto.username())
                .setEmail(registrationRequestDto.email())
                .setPasswordHash(passwordEncryptor.encode(registrationRequestDto.password()))
                .setRole(UserRole.USER)
                .setActive(true)
                .setStorageQuota(storageQuota)
                .build();

        try {
            userRepository.save(user);
            userMetrics.recordUserCreated(userCreateStart);
        } catch (DataIntegrityViolationException e) {
            checkUsernameUnique(user.getUsername());
            checkEmailUnique(user.getEmail());
            throw e;
        }

        log.info("Создан новый пользователь. ID: {}, Логин: {}, Email: {}, Роль: {}",
                user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());

        return new UserProfileResponseDto(
                user.getUsername(),
                user.getEmail(),
                user.getRegisteredAt(),
                user.getRole() != null ? user.getRole().name() : null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDto getProfile() {
        User user = getUserByAuthentication();
        log.debug("Запрошен профиль. ID: {}", user.getId());
        userMetrics.recordProfileViewed();
        return new UserProfileResponseDto(
                user.getUsername(),
                user.getEmail(),
                user.getRegisteredAt(),
                user.getRole() != null ? user.getRole().name() : null
        );
    }

    @Override
    @Transactional
    public void updateUser(UpdateUserRequestDto updateUserRequestDto) {

        Timer.Sample userUpdateStart = Timer.start(meterRegistry);

        User user = getUserByAuthentication();
        List<String> changedFields = new ArrayList<>();

        if (updateUserRequestDto.containsNewUsername()) {
            if (user.getUsername().equals(updateUserRequestDto.newUsername())) {
                throw new InvalidLoginException("Новый логин должен отличаться от старого");
            }
            checkUsernameUnique(updateUserRequestDto.newUsername());
            user.setUsername(updateUserRequestDto.newUsername());
            changedFields.add("Логин");
        }

        if (updateUserRequestDto.containsNewPassword()) {
            if (passwordEncryptor.matches(updateUserRequestDto.newPassword(), user.getPasswordHash())) {
                userMetrics.recordPasswordUpdateFailure();
                throw new InvalidPasswordException("Новый пароль должен отличаться от старого");
            }
            if (!passwordEncryptor.matches(updateUserRequestDto.currentPassword(), user.getPasswordHash())) {
                userMetrics.recordPasswordUpdateFailure();
                throw new InvalidPasswordException("Указан неверный текущий пароль");
            }
            user.setPasswordHash(passwordEncryptor.encode(updateUserRequestDto.newPassword()));
            changedFields.add("Пароль");
        }

        userRepository.save(user);

        log.info("Профиль пользователя обновлён. ID: {}, Изменённые поля: [{}]", user.getId(), changedFields);
        userMetrics.recordUserUpdated(userUpdateStart);
    }

    @Override
    @Transactional
    public void deleteUser() {

        Timer.Sample deleteUserStart = Timer.start(meterRegistry);

        User user = getUserByAuthentication();

        Long userId = user.getId();
        String username = user.getUsername();

        for (File file : user.getActiveFiles()) {
            file.setAuthor(null);
        }
        user.getActiveFiles().clear();

        if (user.getRole() == UserRole.ADMIN) {
            for (UserBan providedBan : user.getProvidedBans()) {
                providedBan.setAdmin(null);
            }
            user.getProvidedBans().clear();
        }

        for (UserBan ban : user.getBanHistory()) {
            ban.setBannedUser(null);
        }
        user.getBanHistory().clear();

        userRepository.delete(user);

        log.warn("Пользователь удалён. ID: {}, Логин: {}. Связи с банами и файлами успешно очищены.", userId, username);
        userMetrics.recordUserDeleted(deleteUserStart);
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
    public List<UserProfileAdminResponseDto> findByCriteria(String searchBy, String searchValue, int page, int pageSize) {
        User admin = checkAdminRightsAndReturnEntity(requestContextService.getUserId());

        log.debug("Админ-поиск пользователей. ID администратора: {}, Критерий: {}, Значение: {}, Страница: {}, Значений на странице: {}",
                admin.getId(), searchBy, searchValue, page, pageSize);

        Pageable pageable = PageRequest.of(page, pageSize);

        Specification<User> specification = Specification.unrestricted();

        UserSearchStrategy userSearchStrategy = userSearchStrategyMap.get(searchBy);

        if (userSearchStrategy == null) {
            log.warn("Неверный критерий поиска пользователей. searchBy: '{}', searchValue='{}'", searchBy, searchValue);
            throw new InvalidSearchCriteriaException("Неверный параметр search_by: " + searchBy);
        }

        specification = specification.and(userSearchStrategy.getSpecification(searchValue));

        return userRepository.findAll(specification, pageable)
                .map(user -> new UserProfileAdminResponseDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole() == null ? "" : user.getRole().name(),
                        user.isActive(),
                        user.getRegisteredAt()
                ))
                .toList();
    }
}

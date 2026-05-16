package ru.LevLezhnin.NauJava.service.implementations;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.LevLezhnin.NauJava.dto.auth.RegistrationRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UpdateUserRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UserProfileAdminResponseDto;
import ru.LevLezhnin.NauJava.dto.user.UserProfileResponseDto;
import ru.LevLezhnin.NauJava.exceptions.InvalidSearchCriteriaException;
import ru.LevLezhnin.NauJava.exceptions.common.InvalidPasswordException;
import ru.LevLezhnin.NauJava.exceptions.user.EmailTakenException;
import ru.LevLezhnin.NauJava.exceptions.common.EntityNotFoundException;
import ru.LevLezhnin.NauJava.exceptions.user.InvalidLoginException;
import ru.LevLezhnin.NauJava.exceptions.user.UsernameTakenException;
import ru.LevLezhnin.NauJava.model.File;
import ru.LevLezhnin.NauJava.model.QuotaTariffs;
import ru.LevLezhnin.NauJava.model.StorageQuota;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.model.UserBan;
import ru.LevLezhnin.NauJava.model.UserRole;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.repository.user.search.UserSearchStrategy;
import ru.LevLezhnin.NauJava.service.interfaces.StorageQuotaService;
import ru.LevLezhnin.NauJava.service.interfaces.UserService;
import ru.LevLezhnin.NauJava.utils.RequestContextService;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RequestContextService requestContextService;
    private final Map<String, UserSearchStrategy> userSearchStrategyMap;
    private final PasswordEncoder passwordEncryptor;
    private final StorageQuotaService storageQuotaService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, RequestContextService requestContextService, PasswordEncoder passwordEncryptor, List<UserSearchStrategy> userSearchStrategies, StorageQuotaService storageQuotaService) {
        this.userRepository = userRepository;
        this.requestContextService = requestContextService;
        this.passwordEncryptor = passwordEncryptor;
        this.userSearchStrategyMap = userSearchStrategies.stream().collect(Collectors.toMap(UserSearchStrategy::getCriteriaKey, s -> s));
        this.storageQuotaService = storageQuotaService;
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
            throw new UsernameTakenException("Пользователь с логином '%s' уже существует".formatted(username));
        }
    }

    private void checkEmailUnique(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailTakenException("Пользователь с E-mail-ом '%s' уже существует".formatted(email));
        }
    }

    @Override
    @Transactional
    public UserProfileResponseDto createUser(RegistrationRequestDto registrationRequestDto) {

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
        } catch (DataIntegrityViolationException e) {
            checkUsernameUnique(user.getUsername());
            checkEmailUnique(user.getEmail());
            throw e;
        }

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

        User user = getUserByAuthentication();

        if (updateUserRequestDto.containsNewUsername()) {
            if (user.getUsername().equals(updateUserRequestDto.newUsername())) {
                throw new InvalidLoginException("Новый логин должен отличаться от старого");
            }
            checkUsernameUnique(updateUserRequestDto.newUsername());
            user.setUsername(updateUserRequestDto.newUsername());
        }

        if (updateUserRequestDto.containsNewPassword()) {
            if (passwordEncryptor.matches(updateUserRequestDto.newPassword(), user.getPasswordHash())) {
                throw new InvalidPasswordException("Новый пароль должен отличаться от старого");
            }
            if (!passwordEncryptor.matches(updateUserRequestDto.currentPassword(), user.getPasswordHash())) {
                throw new InvalidPasswordException("Указан неверный текущий пароль");
            }
            user.setPasswordHash(passwordEncryptor.encode(updateUserRequestDto.newPassword()));
        }

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser() {
        User user = getUserByAuthentication();

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
    }

    @Override
    public List<UserProfileAdminResponseDto> findByCriteria(String searchBy, String searchValue, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);

        Specification<User> specification = Specification.unrestricted();

        UserSearchStrategy userSearchStrategy = userSearchStrategyMap.get(searchBy);

        if (userSearchStrategy == null) {
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

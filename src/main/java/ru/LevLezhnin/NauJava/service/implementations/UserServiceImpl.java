package ru.LevLezhnin.NauJava.service.implementations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.LevLezhnin.NauJava.dto.auth.RegistrationRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UpdateUserRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UserProfileResponseDto;
import ru.LevLezhnin.NauJava.exceptions.EntityNotFoundException;
import ru.LevLezhnin.NauJava.model.*;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.repository.user.search.UserSearchStrategy;
import ru.LevLezhnin.NauJava.service.interfaces.StorageQuotaService;
import ru.LevLezhnin.NauJava.service.interfaces.UserService;
import ru.LevLezhnin.NauJava.utils.RequestContextService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private void checkUsernameUnique(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Пользователь с логином '%s' уже существует".formatted(username));
        }
    }

    private void checkEmailUnique(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Пользователь с E-mail-ом '%s' уже существует".formatted(email));
        }
    }

    private User getUserByAuthentication() {
        Long userId = requestContextService.getUserId();
        return findById(userId);
    }

    @Override
    @Transactional
    public User createUser(RegistrationRequestDto registrationRequestDto) {
        if (!registrationRequestDto.validate()) {
            throw new IllegalArgumentException("Для нового пользователя должны быть заполнены поля: логин, email, пароль");
        }
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

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id: %d не найден".formatted(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с логином: '%s' не найден".formatted(username)));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с логином: '%s' не найден".formatted(email)));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDto getProfile() {
        User user = getUserByAuthentication();
        return new UserProfileResponseDto(user.getUsername(), user.getEmail(), user.getRegisteredAt());
    }

    @Override
    @Transactional
    public void updateUser(UpdateUserRequestDto updateUserRequestDto) {
        if (!updateUserRequestDto.validate()) {
            throw new IllegalArgumentException("При обновлении пользователя должно быть заполнено хотя бы одно из двух: логин, пароль");
        }

        User user = getUserByAuthentication();

        if (updateUserRequestDto.containsUsername()) {
            checkUsernameUnique(updateUserRequestDto.username());
            user.setUsername(updateUserRequestDto.username());
        }

        if (updateUserRequestDto.containsPassword()) {
            if (passwordEncryptor.matches(updateUserRequestDto.password(), user.getPasswordHash())) {
                throw new IllegalArgumentException("Новый пароль должен отличаться от старого");
            }
            user.setPasswordHash(passwordEncryptor.encode(updateUserRequestDto.password()));
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
    public List<User> findByCriteria(String searchBy, String searchValue, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);

        if (searchValue == null || searchValue.isBlank()) {
            return List.of();
        }

        Specification<User> specification = Specification.unrestricted();

        UserSearchStrategy userSearchStrategy = userSearchStrategyMap.get(searchBy);

        if (userSearchStrategy == null) {
            throw new IllegalArgumentException("Неверный параметр search_by: " + searchBy);
        }

        specification = specification.and(userSearchStrategy.getSpecification(searchValue));

        return userRepository.findAll(specification, pageable).toList();
    }
}

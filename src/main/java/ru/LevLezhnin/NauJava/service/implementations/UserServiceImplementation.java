package ru.LevLezhnin.NauJava.service.implementations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.LevLezhnin.NauJava.exceptions.EntityNotFoundException;
import ru.LevLezhnin.NauJava.model.*;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.requests.users.findByCriteria.UserSearchStrategy;
import ru.LevLezhnin.NauJava.service.interfaces.UserService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserServiceImplementation implements UserService {

    private final UserRepository userRepository;
    private final Map<String, UserSearchStrategy> userSearchStrategyMap;
    private final PasswordEncoder passwordEncryptor;

    @Autowired
    public UserServiceImplementation(UserRepository userRepository, PasswordEncoder passwordEncryptor, List<UserSearchStrategy> userSearchStrategies) {
        this.userRepository = userRepository;
        this.passwordEncryptor = passwordEncryptor;
        this.userSearchStrategyMap = userSearchStrategies.stream().collect(Collectors.toMap(UserSearchStrategy::getCriteriaKey, s -> s));
    }

    private boolean validate(String username, String email, String password) {
        return (username != null && !username.isBlank())
                && (email != null && !email.isBlank())
                && (password != null && !password.isBlank());
    }

    @Override
    @Transactional
    public void createUser(String username, String email, String password) {
        if (!validate(username, email, password)) {
            throw new IllegalArgumentException("Для нового пользователя должны быть заполнены поля: логин, email, пароль");
        }

        StorageQuota storageQuota = QuotaTariffs.BASIC.getBasicQuotaBuilder().build();

        User user = User.builder()
                .setUsername(username)
                .setEmail(email)
                .setPasswordHash(passwordEncryptor.encode(password))
                .setRole(UserRole.USER)
                .setActive(true)
                .setStorageQuota(storageQuota)
                .build();

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id: %d не найден".formatted(id)));
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        User user = findById(id);

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
    @Transactional
    public void updateUsername(long id, String username) {
        User user = findById(id);
        user.setUsername(username);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updatePassword(long id, String password) {
        User user = findById(id);
        user.setPasswordHash(passwordEncryptor.encode(password));
        userRepository.save(user);
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

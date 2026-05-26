package ru.LevLezhnin.NauJava.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import ru.LevLezhnin.NauJava.model.StorageQuota;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.model.UserRole;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;

/**
 * Инициализатор, создающий дефолтного администратора при старте приложения
 * (только в профилях dev и prod).
 *
 * @author Лев Лежнин
 */
@Component
@Profile({"dev", "prod"})
public class DefaultAdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminInitializer.class);

    @Value("${app.default.default-admin.username}")
    private String defaultAdminUsername;
    @Value("${app.default.default-admin.email}")
    private String defaultAdminEmail;
    @Value("${app.default.default-admin.password}")
    private String defaultAdminPassword;
    @Value("${app.default.default-admin.quota-size}")
    private DataSize defaultStorageQuotaSize;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DefaultAdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initDefaultAdmin() {

        if (defaultAdminUsername == null || defaultAdminUsername.isBlank() ||
            defaultAdminEmail == null || defaultAdminEmail.isBlank() ||
            defaultAdminPassword == null || defaultAdminPassword.isBlank() ||
            defaultStorageQuotaSize == null || defaultStorageQuotaSize.equals(DataSize.ofBytes(0))) {
            log.warn("Не заданы переменные окружения для администратора по умолчанию. Администратор по умолчанию не создан");
            throw new RuntimeException("Не заданы переменнные окружения для администратора по умолчанию. Администратор по умолчанию не создан");
        }

        StorageQuota defaultAdminStorageQuota = StorageQuota.builder()
                .setUsedStorageBytes(0L)
                .setMaxStorageBytes(defaultStorageQuotaSize.toBytes())
                .build();

        User defaultAdmin = User.builder()
                .setUsername(defaultAdminUsername)
                .setEmail(defaultAdminEmail)
                .setPasswordHash(passwordEncoder.encode(defaultAdminPassword))
                .setRole(UserRole.ADMIN)
                .setActive(true)
                .setStorageQuota(defaultAdminStorageQuota)
                .build();

        if (userRepository.findByUsername(defaultAdminUsername).isPresent() || userRepository.findByEmail(defaultAdminEmail).isPresent()) {
            log.info("Администратор по умолчанию уже создан");
            return;
        }

        userRepository.saveAndFlush(defaultAdmin);
        log.info("Создан администратор по умолчанию. ID: {}, Логин: {}, Email: {}", defaultAdmin.getId(), defaultAdmin.getUsername(), defaultAdmin.getEmail());
    }
}

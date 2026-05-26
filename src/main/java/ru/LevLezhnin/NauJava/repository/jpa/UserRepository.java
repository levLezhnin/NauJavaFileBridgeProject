package ru.LevLezhnin.NauJava.repository.jpa;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import ru.LevLezhnin.NauJava.model.User;

import java.util.List;
import java.util.Optional;

/**
 * JPA-репозиторий для сущности {@link User}.
 * <p>
 * С поддержкой pessimistic locking и eager fetch для квот.
 *
 * @author Лев Лежнин
 */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * Загружает пользователя вместе с его квотой хранения (через EntityGraph).
     * <p>
     * Используется в большинстве сервисов, где нужна квота текущего пользователя.
     */
    @EntityGraph(attributePaths = {"storageQuota"})
    Optional<User> findWithDetailsById(Long id);

    /**
     * Поиск пользователя по username (уникальное поле).
     * <p>
     * Используется при аутентификации и проверке уникальности при регистрации/обновлении.
     */
    Optional<User> findByUsername(String username);

    /**
     * Поиск пользователя по email (уникальное поле).
     */
    Optional<User> findByEmail(String email);

    /**
     * Поиск пользователей по частичному совпадению username (case-insensitive).
     * <p>
     * Используется в административном поиске.
     */
    List<User> findByUsernameLikeIgnoreCase(String username, Pageable pageable);

    /**
     * Загружает пользователя с pessimistic write lock.
     * <p>
     * Используется при бане/разбане пользователя для атомарного изменения isActive.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findForUpdateById(Long id);
}

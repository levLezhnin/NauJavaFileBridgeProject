package ru.LevLezhnin.NauJava.repository.jpa;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.LevLezhnin.NauJava.model.UserBan;

import java.util.Optional;

/**
 * Репозиторий для работы с историей банов пользователей.
 * <p>
 * Содержит специализированные запросы для получения активных блокировок,
 * истории банов пользователя и истории банов, выданных конкретным администратором.
 * Много методов с pessimistic locking и EntityGraph.
 *
 * @author Лев Лежнин
 */
public interface UserBanRepository extends JpaRepository<UserBan, Long> {

    /**
     * Возвращает историю всех блокировок пользователя (включая уже снятые), отсортированную по дате бана убыв.
     * <p>
     * Используется в {@code UserBanService#getUserBanHistory}.
     *
     * @param userId   id заблокированного пользователя
     * @param pageable пагинация
     */
    @Query("SELECT ub FROM UserBan ub WHERE ub.bannedUser.id = :userId ORDER BY ub.bannedAt DESC")
    Page<UserBan> findUserBanHistory(@Param("userId") Long userId, Pageable pageable);

    /**
     * Возвращает историю блокировок, выданных конкретным администратором.
     * <p>
     * Используется в {@code UserBanService#getIssuedBansByAdmin}.
     *
     * @param adminId  id администратора, который выдавал баны
     * @param pageable пагинация
     */
    @Query("SELECT ub FROM UserBan ub WHERE ub.admin.id = :adminId ORDER BY ub.bannedAt DESC")
    Page<UserBan> findIssuedBanHistory(@Param("adminId") Long adminId, Pageable pageable);

    /**
     * Находит активную (не снятую) блокировку пользователя.
     * <p>
     * Используется для проверки, заблокирован ли пользователь в данный момент.
     */
    @Query("FROM UserBan WHERE bannedUser.id = :userId AND unbannedAt IS NULL")
    Optional<UserBan> findActiveUserBan(Long userId);

    /**
     * То же, что {@link #findActiveUserBan(Long)}, но с подгрузкой администратора через EntityGraph.
     */
    @Query("FROM UserBan WHERE bannedUser.id = :userId AND unbannedAt IS NULL")
    @EntityGraph(attributePaths = {"admin"})
    Optional<UserBan> findActiveUserBanWithDetails(Long userId);

    /**
     * Находит активную блокировку с pessimistic write lock.
     * <p>
     * Используется при операции разблокировки ({@code unbanUserById}), чтобы атомарно обновить unbannedAt.
     */
    @Query("FROM UserBan WHERE bannedUser.id = :userId AND unbannedAt IS NULL")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserBan> findActiveUserBanForUpdate(Long userId);

}

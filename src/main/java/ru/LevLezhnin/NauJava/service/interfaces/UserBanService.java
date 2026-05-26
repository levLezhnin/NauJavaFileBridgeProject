package ru.LevLezhnin.NauJava.service.interfaces;

import ru.LevLezhnin.NauJava.dto.user.UserBanResponseDto;

import java.util.List;

/**
 * Сервис управления банами пользователей (для администраторов).
 * <p>
 * Поддерживает:
 * <ul>
 *   <li>Бан и разбан пользователей</li>
 *   <li>Получение активного бана</li>
 *   <li>История банов пользователя</li>
 *   <li>История банов, выданных конкретным админом</li>
 * </ul>
 *
 * @author Лев Лежнин
 */
public interface UserBanService {

    /**
     * Возвращает конкретную запись о бане по её ID.
     * <p>
     * Требует роль ADMIN.
     */
    UserBanResponseDto getUserBanById(Long banId);

    /**
     * Блокирует пользователя.
     * <p>
     * <b>Контракт (строгий):</b>
     * <ul>
     *   <li>Нельзя заблокировать самого себя - SelfActionForbiddenException</li>
     *   <li>Нельзя заблокировать другого администратора - AccessDeniedException</li>
     *   <li>Если пользователь уже заблокирован - UserAlreadyBannedException</li>
     *   <li>Использует pessimistic lock на пользователе</li>
     *   <li>Устанавливает isActive = false у пользователя</li>
     * </ul>
     *
     * @param userId идентификатор блокируемого пользователя
     * @param reason причина блокировки (обязательна)
     */
    UserBanResponseDto banUserById(Long userId, String reason);

    /**
     * Снимает блокировку с пользователя.
     * <p>
     * Аналогичные проверки, что и в {@link #banUserById}: self-forbidden, admin rights, pessimistic lock.
     * Устанавливает unbannedAt и isActive = true.
     */
    UserBanResponseDto unbanUserById(Long userId);

    /**
     * Возвращает активную блокировку пользователя (если есть).
     * <p>
     * Требует ADMIN. Если пользователь не заблокирован - EntityNotFoundException.
     */
    UserBanResponseDto getActiveUserBanByUserId(Long userId);

    /**
     * История всех блокировок конкретного пользователя (включая завершённые).
     * <p>
     * Требует ADMIN. Пользователь должен существовать.
     */
    List<UserBanResponseDto> getUserBanHistory(Long userId, int page, int pageSize);

    /**
     * История блокировок, выданных конкретным администратором.
     * <p>
     * Требует ADMIN. Целевой пользователь должен иметь роль ADMIN, иначе IllegalArgumentException.
     */
    List<UserBanResponseDto> getIssuedBansByAdmin(Long adminId, int page, int pageSize);
}

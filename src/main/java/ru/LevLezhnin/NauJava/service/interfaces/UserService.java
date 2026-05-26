package ru.LevLezhnin.NauJava.service.interfaces;

import ru.LevLezhnin.NauJava.dto.auth.RegistrationRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UpdateUserRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UserProfileAdminResponseDto;
import ru.LevLezhnin.NauJava.dto.user.UserProfileResponseDto;

import java.util.List;

/**
 * Сервис управления пользователями и их профилями.
 * <p>
 * Операции:
 * <ul>
 *   <li>Создание пользователя (при регистрации)</li>
 *   <li>Получение текущего профиля</li>
 *   <li>Обновление данных пользователя</li>
 *   <li>Удаление аккаунта</li>
 *   <li>Административный поиск пользователей</li>
 * </ul>
 *
 * @author Лев Лежнин
 */
public interface UserService {

    /**
     * Создаёт нового пользователя (вызывается при регистрации).
     * <p>
     * Контракт:
     * <ul>
     *   <li>Проверяет уникальность username и email (двойная проверка при DataIntegrityViolation)</li>
     *   <li>Автоматически создаёт StorageQuota по тарифу BASIC</li>
     *   <li>Пароль хешируется</li>
     *   <li>Роль всегда USER, isActive = true</li>
     * </ul>
     *
     * @param registrationRequestDto данные регистрации
     * @return профиль созданного пользователя
     * @throws ru.LevLezhnin.NauJava.exception.user.UsernameTakenException
     * @throws ru.LevLezhnin.NauJava.exception.user.EmailTakenException
     */
    UserProfileResponseDto createUser(RegistrationRequestDto registrationRequestDto);

    /**
     * Возвращает профиль текущего аутентифицированного пользователя.
     */
    UserProfileResponseDto getProfile();

    /**
     * Обновляет username и/или пароль текущего пользователя.
     * <p>
     * Контракт:
     * <ul>
     *   <li>Для смены пароля обязательно указывать текущий пароль</li>
     *   <li>Новый пароль должен отличаться от старого</li>
     *   <li>Новый username должен отличаться от текущего и быть уникальным</li>
     * </ul>
     *
     * @param updateUserRequestDto новые значения (только изменённые поля)
     */
    void updateUser(UpdateUserRequestDto updateUserRequestDto);

    /**
     * Удаляет аккаунт текущего пользователя.
     * <p>
     * Контракт:
     * <ul>
     *   <li>Разрывает связи: файлы - author = null, история банов очищается</li>
     *   <li>Для администратора также очищает связи как выдавшего баны</li>
     *   <li>Физическое удаление записи пользователя</li>
     * </ul>
     */
    void deleteUser();

    /**
     * Административный поиск пользователей по различным критериям.
     * <p>
     * Требует роль ADMIN. При неизвестном {@code searchBy} - InvalidSearchCriteriaException.
     */
    List<UserProfileAdminResponseDto> findByCriteria(String searchBy, String searchValue, int page, int pageSize);
}

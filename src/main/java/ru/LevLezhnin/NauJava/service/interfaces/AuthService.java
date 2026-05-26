package ru.LevLezhnin.NauJava.service.interfaces;

import org.springframework.security.authentication.AuthenticationManager;
import ru.LevLezhnin.NauJava.dto.auth.*;

/**
 * Сервис аутентификации и авторизации пользователей.
 * <p>
 * Предоставляет операции:
 * <ul>
 *   <li>Регистрация нового пользователя</li>
 *   <li>Вход в систему (login)</li>
 *   <li>Обновление access токена</li>
 *   <li>Выход из системы (logout с blacklist)</li>
 * </ul>
 *
 * @author Лев Лежнин
 * @see JwtResponseDto
 */
public interface AuthService {

    /**
     * Регистрирует нового пользователя и сразу выдаёт пару JWT-токенов.
     * <p>
     * Контракт:
     * <ul>
     *   <li>Создаёт пользователя через {@link UserService#createUser} (с BASIC-квотой)</li>
     *   <li>Генерирует access + refresh токены для только что созданного пользователя</li>
     *   <li>Не требует предварительной аутентификации</li>
     * </ul>
     *
     * @param registrationRequestDto данные регистрации (username, email, password)
     * @return пара токенов (access + refresh)
     */
    JwtResponseDto register(RegistrationRequestDto registrationRequestDto);

    /**
     * Выполняет аутентификацию по логину/паролю и выдаёт пару JWT-токенов.
     * <p>
     * Контракт:
     * <ul>
     *   <li>Использует Spring {@link AuthenticationManager}</li>
     *   <li>В случае успеха возвращает access + refresh токены</li>
     *   <li>При неудаче - исключение от AuthenticationManager (обычно BadCredentials)</li>
     * </ul>
     *
     * @param jwtLoginRequestDto логин и пароль
     * @return пара токенов
     */
    JwtResponseDto login(JwtLoginRequestDto jwtLoginRequestDto);

    /**
     * Обновляет access-токен по валидному refresh-токену.
     * <p>
     * Контракт:
     * <ul>
     *   <li>Валидирует подпись и срок действия refresh-токена</li>
     *   <li>Проверяет, что refresh-токен не находится в blacklist</li>
     *   <li>Возвращает новый access-токен + тот же refresh-токен</li>
     *   <li>При отозванном токене - {@link ru.LevLezhnin.NauJava.exception.auth.TokenRevokedException}</li>
     * </ul>
     *
     * @param jwtRefreshRequestDto refresh-токен
     * @return новая пара (новый access + старый refresh)
     */
    JwtResponseDto refresh(JwtRefreshRequestDto jwtRefreshRequestDto);

    /**
     * Выполняет выход пользователя: отзывает (blacklist) переданные токены.
     * <p>
     * Контракт:
     * <ul>
     *   <li>Если access-токен валиден - добавляет его в blacklist на оставшееся время жизни</li>
     *   <li>Аналогично для refresh-токена</li>
     *   <li>Null или невалидные токены игнорируются (best-effort)</li>
     *   <li>Не выбрасывает исключений при отсутствии токенов</li>
     * </ul>
     *
     * @param jwtLogoutRequestDto пара токенов для отзыва
     */
    void logout(JwtLogoutRequestDto jwtLogoutRequestDto);
}

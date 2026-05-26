package ru.LevLezhnin.NauJava.security.userdetails;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Расширение стандартного {@link UserDetails} Spring Security,
 * добавляющее возможность получения внутреннего идентификатора пользователя.
 * <p>
 * Используется вместо обычного UserDetails в тех местах, где нужен доступ к ID
 * (например, при генерации refresh-токенов и их валидации).
 *
 * @author Lev Lezhnin
 * @see IdentifiableUserDetailsService
 */
public interface IdentifiableUserDetails extends UserDetails {

    /**
     * Возвращает внутренний идентификатор пользователя в системе (из БД).
     *
     * @return id пользователя (Long)
     */
    Long getId();
}

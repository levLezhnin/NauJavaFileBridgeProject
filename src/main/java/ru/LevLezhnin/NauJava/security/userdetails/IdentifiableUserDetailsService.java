package ru.LevLezhnin.NauJava.security.userdetails;

import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Расширение {@link UserDetailsService}, позволяющее загружать пользователя
 * не только по username, но и по внутреннему ID.
 * <p>
 * Необходимо для работы с refresh-токенами (в которых хранится ID пользователя,
 * а не username).
 *
 * @author Lev Lezhnin
 * @see IdentifiableUserDetails
 */
public interface IdentifiableUserDetailsService extends UserDetailsService {

    /**
     * Загружает пользователя по его внутреннему идентификатору.
     * <p>
     * Используется при refresh токенов (в {@link ru.LevLezhnin.NauJava.service.implementations.AuthServiceImpl#refresh}).
     *
     * @param id внутренний ID пользователя
     * @return объект, реализующий {@link IdentifiableUserDetails}
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException если пользователь не найден
     */
    IdentifiableUserDetails loadUserById(Long id);
}

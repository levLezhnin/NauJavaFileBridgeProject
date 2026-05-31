package ru.LevLezhnin.NauJava.security.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

/**
 * Request-scoped сервис для хранения идентификатора текущего пользователя в рамках одного HTTP-запроса.
 * <p>
 * Работает на основе ThreadLocal + Spring @Scope(SCOPE_REQUEST) с прокси.
 * Позволяет получать ID аутентифицированного пользователя из любого слоя приложения
 * (сервисы, репозитории, утилиты) без явной передачи контекста.
 * <p>
 * Заполняется в {@link ru.LevLezhnin.NauJava.security.filter.JwtRequestFilter}.
 * Используется почти во всех сервисах для определения "текущего" пользователя.
 *
 * @author Lev Lezhnin
 */
@Component
@Scope(scopeName = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContextService {
    private static final Logger log = LoggerFactory.getLogger(RequestContextService.class);
    private final ThreadLocal<Long> userId = new ThreadLocal<>();

    public void setUserId(Long id) {
        userId.set(id);
    }

    public Long getUserId() {
        Long id = userId.get();
        if (id == null) {
            throw new AuthenticationCredentialsNotFoundException("Пользователь не авторизован");
        }
        return id;
    }

    public void clear() {
        userId.remove();
    }
}

package ru.LevLezhnin.NauJava.utils;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@Scope(scopeName = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContextService {
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

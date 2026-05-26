package ru.LevLezhnin.NauJava.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация параметров HTTP-кук (префикс {@code cookie.*}).
 * <p>
 * Используется {@link ru.LevLezhnin.NauJava.security.utils.TokenCookieService}
 * при установке access/refresh токенов.
 *
 * @author Lev Lezhnin
 */
@Configuration
@ConfigurationProperties("cookie")
public class CookieProperties {

    private String path;
    private String sameSite;
    private boolean secure;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }
}

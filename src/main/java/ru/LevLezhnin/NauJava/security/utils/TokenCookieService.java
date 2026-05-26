package ru.LevLezhnin.NauJava.security.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.security.properties.CookieProperties;

@Component
public class TokenCookieService {

    private static final Logger log = LoggerFactory.getLogger(TokenCookieService.class);
    private final CookieProperties cookieProperties;

    public TokenCookieService(CookieProperties cookieProperties) {
        this.cookieProperties = cookieProperties;
    }

    public void setTokenCookie(HttpServletResponse response,
                                      String name,
                                      String token,
                                      long maxAgeSeconds) {
        ResponseCookie responseCookie = ResponseCookie.from(name, token)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .path(cookieProperties.getPath())
                .maxAge(maxAgeSeconds)
                .sameSite(cookieProperties.getSameSite())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());

        log.debug("Установлен куки. Имя: {}, TTL: {} секунд", name, maxAgeSeconds);
    }

    public String getTokenFromCookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .path(cookieProperties.getPath())
                .maxAge(0)
                .sameSite(cookieProperties.getSameSite())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.debug("Очищен куки. Имя: {}", name);
    }
}

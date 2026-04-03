package ru.LevLezhnin.NauJava.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.LevLezhnin.NauJava.utils.TokenCookieService;
import ru.LevLezhnin.NauJava.utils.JwtTokenHelper;
import ru.LevLezhnin.NauJava.utils.RequestContextService;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);

    private final JwtTokenHelper jwtTokenHelper;
    private final RequestContextService requestContextService;
    private final TokenCookieService tokenCookieService;

    public JwtRequestFilter(JwtTokenHelper jwtTokenHelper, RequestContextService requestContextService, TokenCookieService tokenCookieService) {
        this.jwtTokenHelper = jwtTokenHelper;
        this.requestContextService = requestContextService;
        this.tokenCookieService = tokenCookieService;
    }

    private String tryExtractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring("Bearer ".length());
        }
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String username = null;
        String jwt = tryExtractTokenFromHeader(request);
        Long userId = null;

        if (jwt == null) {
            jwt = tokenCookieService.getTokenFromCookie(request, "ACCESS_TOKEN");
        }

        if (jwt != null) {
            try {
                username = jwtTokenHelper.getUserNameFromAccessToken(jwt);
                userId = jwtTokenHelper.getUserIdFromAccessToken(jwt);
            } catch (ExpiredJwtException e) {
                log.info("Истёк срок действия токена");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            } catch (JwtException e) {
                log.warn("Невалидная подпись JWT-токена");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            log.debug("Пользователь {} (ID: {}) успешно аутентифицирован", username, userId);
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (userId != null) {
                requestContextService.setUserId(userId);
            }
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    jwtTokenHelper.getUserRoles(jwt).stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
            );
            SecurityContextHolder.getContext().setAuthentication(token);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            requestContextService.clear();
        }
    }
}
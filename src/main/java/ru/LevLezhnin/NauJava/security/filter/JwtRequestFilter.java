package ru.LevLezhnin.NauJava.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.LevLezhnin.NauJava.dto.error.ErrorResponse;
import ru.LevLezhnin.NauJava.exceptions.auth.InvalidTokenException;
import ru.LevLezhnin.NauJava.exceptions.auth.TokenExpiredException;
import ru.LevLezhnin.NauJava.security.properties.JwtProperties;
import ru.LevLezhnin.NauJava.service.interfaces.TokenBlacklistService;
import ru.LevLezhnin.NauJava.utils.TokenCookieService;
import ru.LevLezhnin.NauJava.utils.JwtTokenHelper;
import ru.LevLezhnin.NauJava.utils.RequestContextService;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Order(0)
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);

    private final JwtTokenHelper jwtTokenHelper;
    private final RequestContextService requestContextService;
    private final TokenCookieService tokenCookieService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JsonMapper jsonMapper;

    public JwtRequestFilter(JwtTokenHelper jwtTokenHelper, RequestContextService requestContextService, TokenCookieService tokenCookieService, TokenBlacklistService tokenBlacklistService, JsonMapper jsonMapper) {
        this.jwtTokenHelper = jwtTokenHelper;
        this.requestContextService = requestContextService;
        this.tokenCookieService = tokenCookieService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jsonMapper = jsonMapper;
    }

    private String tryExtractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring("Bearer ".length());
        }
        return null;
    }

    private ErrorResponse buildErrorResponse(int status, String error, String message, HttpServletRequest request) {
        return new ErrorResponse(
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                status,
                error,
                message,
                request.getRequestURI()
        );
    }

    private ErrorResponse buildJwtCheckErrorResponse(String message, HttpServletRequest request) {
        return buildErrorResponse(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Некорректный токен",
                message,
                request);
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorResponse errorResponse) {
        response.setStatus(errorResponse.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8);
        try {
            jsonMapper.writeValue(response.getOutputStream(), errorResponse);
        } catch (IOException e) {
            log.error("Не получилось вернуть пользователю подробности ошибки. Причина: {}", e.getMessage(), e);
        }
    }

    private void handleTokenError(HttpServletResponse response, HttpServletRequest request, String message) throws IOException {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("text/html")) {
            response.sendRedirect("/login");
        } else {
            var error = buildJwtCheckErrorResponse(message, request);
            writeErrorResponse(response, error);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        if (requestURI.equals("/") ||
            requestURI.equals("/login") ||
            requestURI.equals("/register") ||
            requestURI.equals("/forbidden") ||
            requestURI.contains("/css") ||
            requestURI.contains("/js") ||
            requestURI.contains("/assets") ||
            requestURI.startsWith("/api/v1/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = null;
        String jwt = tryExtractTokenFromHeader(request);
        Long userId = null;

        if (jwt == null) {
            jwt = tokenCookieService.getTokenFromCookie(request, JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);
        }

        if (jwt != null) {
            try {
                username = jwtTokenHelper.getUserNameFromAccessToken(jwt);
                userId = jwtTokenHelper.getUserIdFromAccessToken(jwt);
            } catch (TokenExpiredException | InvalidTokenException e) {
                handleTokenError(response, request, e.getMessage());
                return;
            }

            if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
                log.debug("Токен отклонён, т.к. находится в черном списке");
                var error = buildJwtCheckErrorResponse("Токен доступа отклонён, так как находится в черном списке", request);
                writeErrorResponse(response, error);
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (userId != null) {
                requestContextService.setUserId(userId);
            }

            List<GrantedAuthority> grantedAuthorities;
            try {
                grantedAuthorities = jwtTokenHelper.getUserRoles(jwt).stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
            } catch (TokenExpiredException | InvalidTokenException e) {
                handleTokenError(response, request, e.getMessage());
                return;
            }

            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    grantedAuthorities
            );
            SecurityContextHolder.getContext().setAuthentication(token);

            log.debug("Пользователь {} (ID: {}) успешно аутентифицирован", username, userId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            requestContextService.clear();
        }
    }
}
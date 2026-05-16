package ru.LevLezhnin.NauJava.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.LevLezhnin.NauJava.dto.error.ErrorResponse;
import ru.LevLezhnin.NauJava.model.UserBan;
import ru.LevLezhnin.NauJava.repository.jpa.UserBanRepository;
import ru.LevLezhnin.NauJava.utils.RequestContextService;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@Order(1)
public class UserBanFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UserBanFilter.class);

    private final UserBanRepository userBanRepository;
    private final RequestContextService requestContextService;
    private final JsonMapper jsonMapper;

    @Autowired
    public UserBanFilter(UserBanRepository userBanRepository, RequestContextService requestContextService, JsonMapper jsonMapper) {
        this.userBanRepository = userBanRepository;
        this.requestContextService = requestContextService;
        this.jsonMapper = jsonMapper;
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

    private ErrorResponse buildBanCheckErrorResponse(String adminUsername, String reason, Instant bannedAt, HttpServletRequest request) {
        return buildErrorResponse(
                HttpServletResponse.SC_FORBIDDEN,
                "Ваш аккаунт заблокирован",
                "Ваш аккаунт был заблокирован. Блокировка выдана: %s. Причина: %s. Время блокировки: %s".formatted(adminUsername, reason, bannedAt.toString()),
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        if ("/forbidden".equals(uri) ||
                "/api/v1/auth/logout".equals(uri) ||
                uri.startsWith("/css/") ||
                uri.startsWith("/js/") ||
                uri.startsWith("/assets/")) {
            filterChain.doFilter(request, response);
            return;
        }

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = requestContextService.getUserId();

        try {
            Optional<UserBan> activeBanOpt = userBanRepository.findActiveUserBanWithDetails(userId);

            if (activeBanOpt.isPresent()) {
                UserBan activeBan = activeBanOpt.get();
                String accept = request.getHeader("Accept");
                if (accept != null && accept.contains("text/html")) {
                    String encodedReason = URLEncoder.encode(activeBan.getReason(), StandardCharsets.UTF_8);
                    String encodedAdmin = URLEncoder.encode(activeBan.getAdmin().getUsername(), StandardCharsets.UTF_8);
                    String url = "/forbidden?ban=true&reason=" + encodedReason +
                                 "&bannedAt=" + activeBan.getBannedAt().toString() +
                                 "&admin=" + encodedAdmin;
                    response.sendRedirect(url);
                } else {
                    var error = buildBanCheckErrorResponse(activeBan.getAdmin().getUsername(), activeBan.getReason(), activeBan.getBannedAt(), request);
                    writeErrorResponse(response, error);
                }
                return;
            }
        } catch (Exception e) {
            log.error("Возникла ошибка при проверке бана пользователя. ID пользователя: {}", userId, e);
            var error = buildErrorResponse(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Сервис недоступен",
                    "Сервис временно недоступен. Повторите попытку позже",
                    request);
            writeErrorResponse(response, error);
            return;
        }

        filterChain.doFilter(request, response);
    }
}

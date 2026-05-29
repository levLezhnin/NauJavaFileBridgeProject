package ru.LevLezhnin.NauJava.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.model.UserBan;
import ru.LevLezhnin.NauJava.model.UserRole;
import ru.LevLezhnin.NauJava.repository.jpa.UserBanRepository;
import ru.LevLezhnin.NauJava.security.context.RequestContextService;
import tools.jackson.databind.json.JsonMapper;

import java.io.OutputStream;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserBanFilterUnitTest {

    @Mock
    private UserBanRepository userBanRepository;
    @Mock
    private RequestContextService requestContextService;
    @Mock
    private JsonMapper jsonMapper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    private MockedStatic<SecurityContextHolder> securityContextHolderMock;

    private UserBanFilter filter;
    private final Long testUserId = 1L;
    private final Long adminId = 2L;
    private final String adminUsername = "adminUser";
    private final String banReason = "Нарушение правил";
    private final Instant bannedAt = Instant.now();

    @BeforeEach
    void setUp() {
        securityContextHolderMock = mockStatic(SecurityContextHolder.class);
        lenient().when(SecurityContextHolder.getContext()).thenReturn(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);

        filter = new UserBanFilter(userBanRepository, requestContextService, jsonMapper);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("testUser", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        securityContextHolderMock.close();
    }

    @Nested
    @DisplayName("Пропускаемые URI и неавторизованные пользователи")
    class SkipAndAuthTests {

        @Test
        @DisplayName("Пропускает запрос к /forbidden без проверки бана")
        void shouldSkipForbiddenPath() throws Exception {
            when(request.getRequestURI()).thenReturn("/forbidden");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(userBanRepository);
        }

        @Test
        @DisplayName("Пропускает запрос к /api/v1/auth/logout")
        void shouldSkipLogoutPath() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/auth/logout");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(userBanRepository);
        }

        @Test
        @DisplayName("Пропускает запрос, если пользователь не аутентифицирован")
        void shouldSkip_whenNotAuthenticated() throws Exception {
            when(SecurityContextHolder.getContext()).thenReturn(securityContext);
            when(authentication.isAuthenticated()).thenReturn(false);

            when(request.getRequestURI()).thenReturn("/api/v1/profile");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(requestContextService, never()).getUserId();
        }
    }

    @Nested
    @DisplayName("Проверка блокировки пользователя")
    class BanCheckTests {

        @BeforeEach
        void setUpBanTests() {
            when(request.getRequestURI()).thenReturn("/api/v1/profile");
            when(requestContextService.getUserId()).thenReturn(testUserId);
        }

        @Test
        @DisplayName("Позитивный тест: активная блокировка отсутствует")
        void shouldPass_whenNoActiveBan() throws Exception {
            when(userBanRepository.findActiveUserBanWithDetails(testUserId)).thenReturn(Optional.empty());

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(userBanRepository).findActiveUserBanWithDetails(testUserId);
        }

        @Test
        @DisplayName("Негативный тест: активная блокировка, JSON запрос возвращает 403")
        void shouldReturn403Json_whenBanExistsAndJsonRequest() throws Exception {
            User admin = new User(); admin.setId(adminId); admin.setUsername(adminUsername); admin.setRole(UserRole.ADMIN);
            UserBan ban = new UserBan();
            ban.setAdmin(admin);
            ban.setReason(banReason);
            ban.setBannedAt(bannedAt);

            when(userBanRepository.findActiveUserBanWithDetails(testUserId)).thenReturn(Optional.of(ban));
            when(request.getHeader("Accept")).thenReturn("application/json");
            doNothing().when(jsonMapper).writeValue((OutputStream) any(), any());

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(response).setContentType("application/json");
            verify(filterChain, never()).doFilter(request, response);
            verify(jsonMapper).writeValue((OutputStream) any(), any());
        }

        @Test
        @DisplayName("Негативный тест: активная блокировка, HTML запрос редиректит на /forbidden")
        void shouldRedirect_whenBanExistsAndHtmlRequest() throws Exception {
            User admin = new User(); admin.setId(adminId); admin.setUsername(adminUsername); admin.setRole(UserRole.ADMIN);
            UserBan ban = new UserBan();
            ban.setAdmin(admin);
            ban.setReason(banReason);
            ban.setBannedAt(bannedAt);

            when(userBanRepository.findActiveUserBanWithDetails(testUserId)).thenReturn(Optional.of(ban));
            when(request.getHeader("Accept")).thenReturn("text/html");

            filter.doFilterInternal(request, response, filterChain);

            verify(response).sendRedirect(argThat(url -> url.startsWith("/forbidden?ban=true")));
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("Негативный тест: ошибка при проверке бана возвращает 403")
        void shouldReturn503Json_whenExceptionThrownDuringBanCheck() throws Exception {
            when(userBanRepository.findActiveUserBanWithDetails(testUserId)).thenThrow(new RuntimeException("DB Connection failed"));
            doNothing().when(jsonMapper).writeValue((OutputStream) any(), any());

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(response).setContentType("application/json");
            verify(filterChain, never()).doFilter(request, response);
            verify(jsonMapper).writeValue((OutputStream) any(), any());
        }
    }
}
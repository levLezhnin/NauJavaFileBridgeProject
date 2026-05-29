package ru.LevLezhnin.NauJava.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.LevLezhnin.NauJava.exception.auth.InvalidTokenException;
import ru.LevLezhnin.NauJava.exception.auth.TokenExpiredException;
import ru.LevLezhnin.NauJava.security.context.RequestContextService;
import ru.LevLezhnin.NauJava.security.utils.JwtTokenHelper;
import ru.LevLezhnin.NauJava.security.utils.TokenCookieService;
import ru.LevLezhnin.NauJava.service.interfaces.TokenBlacklistService;
import tools.jackson.databind.json.JsonMapper;

import java.io.OutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtRequestFilterUnitTest {

    @Mock
    private JwtTokenHelper jwtTokenHelper;
    @Mock
    private RequestContextService requestContextService;
    @Mock
    private TokenCookieService tokenCookieService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private JsonMapper jsonMapper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private JwtRequestFilter filter;
    private final String testToken = "valid.jwt.token";
    private final String testUsername = "testUser";
    private final Long testUserId = 1L;

    @BeforeEach
    void setUp() {
        filter = new JwtRequestFilter(jwtTokenHelper, requestContextService, tokenCookieService, tokenBlacklistService, jsonMapper);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Пропускаемые URI")
    class SkippedUriTests {

        @Test
        @DisplayName("Пропускает запрос к /login без проверки токена")
        void shouldSkipLoginPath() throws Exception {
            when(request.getRequestURI()).thenReturn("/login");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(jwtTokenHelper, tokenCookieService, tokenBlacklistService);
        }

        @Test
        @DisplayName("Пропускает запрос к /api/v1/auth/register")
        void shouldSkipAuthApiPath() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/auth/register");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Обработка JWT токена")
    class JwtProcessingTests {

        @BeforeEach
        void setUpJwtTests() {
            when(request.getRequestURI()).thenReturn("/api/v1/users/profile");
        }

        @Test
        @DisplayName("Позитивный тест: валидный токен в заголовке Authorization")
        void shouldAuthenticate_whenValidTokenInHeader() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
            when(jwtTokenHelper.getUserNameFromAccessToken(testToken)).thenReturn(testUsername);
            when(jwtTokenHelper.getUserIdFromAccessToken(testToken)).thenReturn(testUserId);
            when(tokenBlacklistService.isTokenBlacklisted(testToken)).thenReturn(false);
            when(jwtTokenHelper.getUserRoles(testToken)).thenReturn(List.of("ROLE_USER"));

            filter.doFilterInternal(request, response, filterChain);

            verify(requestContextService).setUserId(testUserId);
            verify(filterChain, times(1)).doFilter(request, response);
            verify(requestContextService, times(1)).clear();
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals(testUsername, SecurityContextHolder.getContext().getAuthentication().getName());
        }

        @Test
        @DisplayName("Позитивный тест: валидный токен в cookie")
        void shouldAuthenticate_whenValidTokenInCookie() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);
            when(tokenCookieService.getTokenFromCookie(eq(request), anyString())).thenReturn(testToken);
            when(jwtTokenHelper.getUserNameFromAccessToken(testToken)).thenReturn(testUsername);
            when(jwtTokenHelper.getUserIdFromAccessToken(testToken)).thenReturn(testUserId);
            when(tokenBlacklistService.isTokenBlacklisted(testToken)).thenReturn(false);
            when(jwtTokenHelper.getUserRoles(testToken)).thenReturn(List.of("ROLE_USER"));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(requestContextService).clear();
        }

        @Test
        @DisplayName("Негативный тест: истекший токен возвращает 401 JSON")
        void shouldReturn401Json_whenTokenExpired() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
            when(jwtTokenHelper.getUserNameFromAccessToken(testToken)).thenThrow(new TokenExpiredException("Токен истек"));
            when(request.getHeader("Accept")).thenReturn("application/json");
            doNothing().when(jsonMapper).writeValue((OutputStream) any(), any());

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(response).setContentType("application/json");
            verify(filterChain, never()).doFilter(request, response);
            verify(requestContextService, never()).clear();
        }

        @Test
        @DisplayName("Негативный тест: истекший токен редиректит на /login для HTML запроса")
        void shouldRedirectToLogin_whenTokenExpiredAndHtmlRequest() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
            when(jwtTokenHelper.getUserNameFromAccessToken(testToken)).thenThrow(new InvalidTokenException("Неверный токен"));
            when(request.getHeader("Accept")).thenReturn("text/html");

            filter.doFilterInternal(request, response, filterChain);

            verify(response).sendRedirect("/login");
            verify(filterChain, never()).doFilter(request, response);
            verify(requestContextService, never()).clear();
        }

        @Test
        @DisplayName("Негативный тест: токен в черном списке возвращает 401 JSON")
        void shouldReturn401Json_whenTokenBlacklisted() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
            when(jwtTokenHelper.getUserNameFromAccessToken(testToken)).thenReturn(testUsername);
            when(jwtTokenHelper.getUserIdFromAccessToken(testToken)).thenReturn(testUserId);
            when(tokenBlacklistService.isTokenBlacklisted(testToken)).thenReturn(true);
            doNothing().when(jsonMapper).writeValue((OutputStream) any(), any());

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
            verify(requestContextService, never()).clear();
        }
    }
}
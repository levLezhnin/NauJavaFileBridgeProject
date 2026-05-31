package ru.LevLezhnin.NauJava.service.implementations;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import ru.LevLezhnin.NauJava.dto.auth.*;
import ru.LevLezhnin.NauJava.dto.user.UserProfileResponseDto;
import ru.LevLezhnin.NauJava.exception.auth.TokenRevokedException;
import ru.LevLezhnin.NauJava.metrics.AuthMetrics;
import ru.LevLezhnin.NauJava.security.userdetails.IdentifiableUserDetails;
import ru.LevLezhnin.NauJava.security.userdetails.IdentifiableUserDetailsService;
import ru.LevLezhnin.NauJava.security.utils.JwtTokenHelper;
import ru.LevLezhnin.NauJava.service.interfaces.TokenBlacklistService;
import ru.LevLezhnin.NauJava.service.interfaces.UserService;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplUnitTest {

    @Mock
    private IdentifiableUserDetailsService userDetailsService;
    @Mock
    private JwtTokenHelper jwtTokenHelper;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserService userService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private AuthServiceImpl authService;

    private final String testUsername = "testUser";
    private final String testEmail = "test@test.com";
    private final String testPassword = "securePassword123";
    private final String testAccessToken = "eyJhbGciOiJIUzI1NiIsYWNjZXNz...";
    private final String testRefreshToken = "eyJhbGciOiJIUzI1NiJ9.refresh...";
    private final Long testUserId = 42L;

    private UserProfileResponseDto testUserProfile;
    private IdentifiableUserDetails testUserDetails;

    @BeforeEach
    void setUp() {

        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        AuthMetrics authMetrics = new AuthMetrics(meterRegistry);

        authService = new AuthServiceImpl(
                userDetailsService,
                jwtTokenHelper,
                authenticationManager,
                userService,
                tokenBlacklistService,
                meterRegistry,
                authMetrics
        );

        testUserProfile = new UserProfileResponseDto(
                testUsername,
                testEmail,
                null,
                "USER"
        );

        // Создаём мок UserDetails
        testUserDetails = mock(IdentifiableUserDetails.class);
        lenient().when(testUserDetails.getId()).thenReturn(testUserId);
        lenient().when(testUserDetails.getUsername()).thenReturn(testUsername);
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        private RegistrationRequestDto createRegistrationDto() {
            return new RegistrationRequestDto(testUsername, testEmail, testPassword);
        }

        @Test
        @DisplayName("Позитивный тест: успешная регистрация пользователя")
        void shouldRegisterUser_whenValidRegistrationRequest() {
            RegistrationRequestDto registrationDto = createRegistrationDto();

            when(userService.createUser(eq(registrationDto))).thenReturn(testUserProfile);
            when(userDetailsService.loadUserByUsername(eq(testUsername))).thenReturn(testUserDetails);
            when(jwtTokenHelper.generateAccessToken(eq(testUserDetails))).thenReturn(testAccessToken);
            when(jwtTokenHelper.generateRefreshToken(eq(testUserDetails))).thenReturn(testRefreshToken);

            JwtResponseDto result = authService.register(registrationDto);

            assertAll(
                    () -> assertNotNull(result, "JwtResponseDto не должен быть null"),
                    () -> assertEquals(testAccessToken, result.accessToken(), "Access token не совпадает"),
                    () -> assertEquals(testRefreshToken, result.refreshToken(), "Refresh token не совпадает"),
                    () -> verify(userService).createUser(eq(registrationDto)),
                    () -> verify(userDetailsService).loadUserByUsername(eq(testUsername)),
                    () -> verify(jwtTokenHelper).generateAccessToken(eq(testUserDetails)),
                    () -> verify(jwtTokenHelper).generateRefreshToken(eq(testUserDetails))
            );
        }

        @Test
        @DisplayName("Негативный тест: ошибка при создании пользователя (например, email занят)")
        void shouldPropagateException_whenUserServiceThrows() {
            RegistrationRequestDto registrationDto = createRegistrationDto();

            when(userService.createUser(eq(registrationDto)))
                    .thenThrow(new RuntimeException("Email already taken"));

            assertThrows(RuntimeException.class, () -> authService.register(registrationDto));
            verify(userService).createUser(eq(registrationDto));
            verify(userDetailsService, never()).loadUserByUsername(any());
            verify(jwtTokenHelper, never()).generateAccessToken(any());
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        private JwtLoginRequestDto createLoginDto() {
            return new JwtLoginRequestDto(testUsername, testPassword);
        }

        @Test
        @DisplayName("Позитивный тест: успешная аутентификация")
        void shouldLogin_whenValidCredentials() {
            JwtLoginRequestDto loginDto = createLoginDto();
            Authentication mockAuth = mock(Authentication.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(mockAuth.getPrincipal()).thenReturn(testUserDetails);
            when(jwtTokenHelper.generateAccessToken(eq(testUserDetails))).thenReturn(testAccessToken);
            when(jwtTokenHelper.generateRefreshToken(eq(testUserDetails))).thenReturn(testRefreshToken);

            JwtResponseDto result = authService.login(loginDto);

            assertAll(
                    () -> assertEquals(testAccessToken, result.accessToken()),
                    () -> assertEquals(testRefreshToken, result.refreshToken()),
                    () -> verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class)),
                    () -> verify(jwtTokenHelper).generateAccessToken(eq(testUserDetails)),
                    () -> verify(jwtTokenHelper).generateRefreshToken(eq(testUserDetails))
            );
        }

        @Test
        @DisplayName("Негативный тест: неверные учётные данные")
        void shouldThrow_whenInvalidCredentials() {
            JwtLoginRequestDto loginDto = createLoginDto();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            assertThrows(BadCredentialsException.class, () -> authService.login(loginDto));
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtTokenHelper, never()).generateAccessToken(any());
        }

        @Test
        @DisplayName("Негативный тест: principal не является UserDetails")
        void shouldThrow_whenPrincipalIsNotUserDetails() {
            JwtLoginRequestDto loginDto = createLoginDto();
            Authentication mockAuth = mock(Authentication.class);
            Object nonUserDetailsPrincipal = new Object();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(mockAuth.getPrincipal()).thenReturn(nonUserDetailsPrincipal);

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> authService.login(loginDto)
            );
            assertTrue(exception.getMessage().contains("principal не является UserDetails"));
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtTokenHelper, never()).generateAccessToken(any());
        }

        @Test
        @DisplayName("Негативный тест: principal равен null")
        void shouldThrow_whenPrincipalIsNull() {
            JwtLoginRequestDto loginDto = createLoginDto();
            Authentication mockAuth = mock(Authentication.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(mockAuth.getPrincipal()).thenReturn(null);

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> authService.login(loginDto)
            );
            assertTrue(exception.getMessage().contains("principal не является UserDetails"));
        }
    }

    @Nested
    @DisplayName("refresh")
    class RefreshTests {

        private JwtRefreshRequestDto createRefreshDto(String refreshToken) {
            return new JwtRefreshRequestDto(refreshToken);
        }

        @Test
        @DisplayName("Позитивный тест: успешное обновление access token")
        void shouldRefresh_whenValidRefreshToken() {
            JwtRefreshRequestDto refreshDto = createRefreshDto(testRefreshToken);

            when(tokenBlacklistService.isTokenBlacklisted(eq(testRefreshToken))).thenReturn(false);
            when(jwtTokenHelper.getUserIdFromRefreshToken(eq(testRefreshToken))).thenReturn(testUserId);
            when(userDetailsService.loadUserById(eq(testUserId))).thenReturn(testUserDetails);
            when(jwtTokenHelper.generateAccessToken(eq(testUserDetails))).thenReturn(testAccessToken);

            JwtResponseDto result = authService.refresh(refreshDto);

            assertAll(
                    () -> assertEquals(testAccessToken, result.accessToken(), "Новый access token не совпадает"),
                    () -> assertEquals(testRefreshToken, result.refreshToken(), "Refresh token должен остаться прежним"),
                    () -> verify(jwtTokenHelper).validateRefreshToken(eq(testRefreshToken)),
                    () -> verify(tokenBlacklistService).isTokenBlacklisted(eq(testRefreshToken)),
                    () -> verify(jwtTokenHelper).getUserIdFromRefreshToken(eq(testRefreshToken)),
                    () -> verify(userDetailsService).loadUserById(eq(testUserId)),
                    () -> verify(jwtTokenHelper).generateAccessToken(eq(testUserDetails))
            );
        }

        @Test
        @DisplayName("Негативный тест: refresh token невалиден")
        void shouldThrow_whenRefreshTokenInvalid() {
            JwtRefreshRequestDto refreshDto = createRefreshDto("invalid_token");

            doThrow(new IllegalArgumentException("Invalid refresh token")).when(jwtTokenHelper).validateRefreshToken(eq("invalid_token"));

            assertThrows(IllegalArgumentException.class, () -> authService.refresh(refreshDto));
            verify(jwtTokenHelper).validateRefreshToken(eq("invalid_token"));
            verify(tokenBlacklistService, never()).isTokenBlacklisted(any());
        }

        @Test
        @DisplayName("Негативный тест: refresh token в чёрном списке")
        void shouldThrow_whenRefreshTokenBlacklisted() {
            JwtRefreshRequestDto refreshDto = createRefreshDto(testRefreshToken);

            doNothing().when(jwtTokenHelper).validateRefreshToken(eq(testRefreshToken));
            when(tokenBlacklistService.isTokenBlacklisted(eq(testRefreshToken))).thenReturn(true);

            TokenRevokedException exception = assertThrows(
                    TokenRevokedException.class,
                    () -> authService.refresh(refreshDto)
            );
            assertEquals("Refresh токен отозван", exception.getMessage());
            verify(jwtTokenHelper).validateRefreshToken(eq(testRefreshToken));
            verify(tokenBlacklistService).isTokenBlacklisted(eq(testRefreshToken));
            verify(jwtTokenHelper, never()).getUserIdFromRefreshToken(any());
        }

        @Test
        @DisplayName("Негативный тест: пользователь не найден по ID из токена")
        void shouldThrow_whenUserNotFoundById() {
            JwtRefreshRequestDto refreshDto = createRefreshDto(testRefreshToken);

            doNothing().when(jwtTokenHelper).validateRefreshToken(eq(testRefreshToken));
            when(tokenBlacklistService.isTokenBlacklisted(eq(testRefreshToken))).thenReturn(false);
            when(jwtTokenHelper.getUserIdFromRefreshToken(eq(testRefreshToken))).thenReturn(testUserId);
            when(userDetailsService.loadUserById(eq(testUserId)))
                    .thenThrow(new RuntimeException("User not found"));

            assertThrows(RuntimeException.class, () -> authService.refresh(refreshDto));
            verify(userDetailsService).loadUserById(eq(testUserId));
            verify(jwtTokenHelper, never()).generateAccessToken(any());
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        private JwtLogoutRequestDto createLogoutDto(String accessToken, String refreshToken) {
            return new JwtLogoutRequestDto(accessToken, refreshToken);
        }

        @Test
        @DisplayName("Позитивный тест: успешный выход с обоими токенами")
        void shouldLogout_whenBothTokensProvided() {
            Duration accessTtl = Duration.ofMinutes(15);
            Duration refreshTtl = Duration.ofDays(7);

            JwtLogoutRequestDto logoutDto = createLogoutDto(testAccessToken, testRefreshToken);

            when(jwtTokenHelper.isAccessToken(eq(testAccessToken))).thenReturn(true);
            when(jwtTokenHelper.getRemainingAccessTtl(eq(testAccessToken))).thenReturn(accessTtl);
            when(jwtTokenHelper.isRefreshToken(eq(testRefreshToken))).thenReturn(true);
            when(jwtTokenHelper.getRemainingRefreshTtl(eq(testRefreshToken))).thenReturn(refreshTtl);

            authService.logout(logoutDto);

            assertAll(
                    () -> verify(jwtTokenHelper).isAccessToken(eq(testAccessToken)),
                    () -> verify(jwtTokenHelper).getRemainingAccessTtl(eq(testAccessToken)),
                    () -> verify(tokenBlacklistService).blacklistToken(eq(testAccessToken), eq(accessTtl)),
                    () -> verify(jwtTokenHelper).isRefreshToken(eq(testRefreshToken)),
                    () -> verify(jwtTokenHelper).getRemainingRefreshTtl(eq(testRefreshToken)),
                    () -> verify(tokenBlacklistService).blacklistToken(eq(testRefreshToken), eq(refreshTtl))
            );
        }

        @Test
        @DisplayName("Позитивный тест: выход только с access token")
        void shouldLogout_whenOnlyAccessTokenProvided() {
            Duration accessTtl = Duration.ofMinutes(15);

            JwtLogoutRequestDto logoutDto = createLogoutDto(testAccessToken, null);

            when(jwtTokenHelper.isAccessToken(eq(testAccessToken))).thenReturn(true);
            when(jwtTokenHelper.getRemainingAccessTtl(eq(testAccessToken))).thenReturn(accessTtl);

            authService.logout(logoutDto);

            assertAll(
                    () -> verify(jwtTokenHelper).isAccessToken(eq(testAccessToken)),
                    () -> verify(tokenBlacklistService).blacklistToken(eq(testAccessToken), eq(accessTtl)),
                    () -> verify(jwtTokenHelper, never()).validateRefreshToken(any()),
                    () -> verify(tokenBlacklistService, never()).blacklistToken(eq(null), any())
            );
        }

        @Test
        @DisplayName("Позитивный тест: выход только с refresh token")
        void shouldLogout_whenOnlyRefreshTokenProvided() {
            Duration refreshTtl = Duration.ofDays(7);

            JwtLogoutRequestDto logoutDto = createLogoutDto(null, testRefreshToken);

            when(jwtTokenHelper.isRefreshToken(eq(testRefreshToken))).thenReturn(true);
            when(jwtTokenHelper.getRemainingRefreshTtl(eq(testRefreshToken))).thenReturn(refreshTtl);

            authService.logout(logoutDto);

            assertAll(
                    () -> verify(jwtTokenHelper, never()).isAccessToken(any()),
                    () -> verify(jwtTokenHelper).isRefreshToken(eq(testRefreshToken)),
                    () -> verify(tokenBlacklistService).blacklistToken(eq(testRefreshToken), eq(refreshTtl))
            );
        }

        @Test
        @DisplayName("Позитивный тест: оба токена null - ничего не добавляется в чёрный список")
        void shouldDoNothing_whenBothTokensNull() {
            JwtLogoutRequestDto logoutDto = createLogoutDto(null, null);

            authService.logout(logoutDto);

            verify(jwtTokenHelper, never()).isAccessToken(any());
            verify(jwtTokenHelper, never()).validateRefreshToken(any());
            verify(tokenBlacklistService, never()).blacklistToken(any(), any());
        }

        @Test
        @DisplayName("Позитивный тест: токен не распознан как access/refresh - игнорируется")
        void shouldIgnoreUnrecognizedTokens() {
            JwtLogoutRequestDto logoutDto = createLogoutDto("not_an_access_token", "not_a_refresh_token");

            when(jwtTokenHelper.isAccessToken(eq("not_an_access_token"))).thenReturn(false);
            when(jwtTokenHelper.isRefreshToken(eq("not_a_refresh_token"))).thenReturn(false);

            authService.logout(logoutDto);

            verify(jwtTokenHelper).isAccessToken(eq("not_an_access_token"));
            verify(jwtTokenHelper).isRefreshToken(eq("not_a_refresh_token"));
            verify(tokenBlacklistService, never()).blacklistToken(any(), any());
        }

        @Test
        @DisplayName("Проверка: blacklistToken вызывается с корректным TTL")
        void shouldBlacklistWithCorrectTtl() {
            Duration expectedTtl = Duration.ofSeconds(120);
            String token = "some_token";

            JwtLogoutRequestDto logoutDto = createLogoutDto(token, null);

            when(jwtTokenHelper.isAccessToken(eq(token))).thenReturn(true);
            when(jwtTokenHelper.getRemainingAccessTtl(eq(token))).thenReturn(expectedTtl);

            authService.logout(logoutDto);

            verify(tokenBlacklistService).blacklistToken(eq(token), eq(expectedTtl));
        }
    }
}
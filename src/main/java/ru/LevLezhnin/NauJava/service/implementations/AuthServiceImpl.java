package ru.LevLezhnin.NauJava.service.implementations;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ru.LevLezhnin.NauJava.dto.auth.*;
import ru.LevLezhnin.NauJava.dto.user.UserProfileResponseDto;
import ru.LevLezhnin.NauJava.exception.auth.TokenRevokedException;
import ru.LevLezhnin.NauJava.metrics.AuthMetrics;
import ru.LevLezhnin.NauJava.security.userdetails.IdentifiableUserDetailsService;
import ru.LevLezhnin.NauJava.security.utils.JwtTokenHelper;
import ru.LevLezhnin.NauJava.service.interfaces.AuthService;
import ru.LevLezhnin.NauJava.service.interfaces.TokenBlacklistService;
import ru.LevLezhnin.NauJava.service.interfaces.UserService;

import java.time.Duration;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final IdentifiableUserDetailsService userDetailsService;
    private final JwtTokenHelper jwtTokenHelper;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final TokenBlacklistService tokenBlacklistService;

    private final MeterRegistry meterRegistry;
    private final AuthMetrics authMetrics;

    @Autowired
    public AuthServiceImpl(IdentifiableUserDetailsService userDetailsService,
                           JwtTokenHelper jwtTokenHelper,
                           AuthenticationManager authenticationManager,
                           UserService userService,
                           TokenBlacklistService tokenBlacklistService,
                           MeterRegistry meterRegistry,
                           AuthMetrics authMetrics) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenHelper = jwtTokenHelper;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.meterRegistry = meterRegistry;
        this.authMetrics = authMetrics;
    }


    @Override
    public JwtResponseDto register(RegistrationRequestDto registrationRequestDto) {

        Timer.Sample registerStart = Timer.start(meterRegistry);

        try {
            UserProfileResponseDto user = userService.createUser(registrationRequestDto);
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.username());
            log.info("Успешно зарегистрирован новый пользователь. Логин: {}, Email: {}", user.username(), user.email());

            authMetrics.recordRegistrationSuccess(registerStart);

            return new JwtResponseDto(
                    jwtTokenHelper.generateAccessToken(userDetails),
                    jwtTokenHelper.generateRefreshToken(userDetails)
            );
        } catch (Exception e) {
            authMetrics.recordRegistrationFailure(e);
            log.warn("Ошибка регистрации: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public JwtResponseDto login(JwtLoginRequestDto jwtLoginRequestDto) {

        Timer.Sample loginSampleStart = Timer.start(meterRegistry);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(jwtLoginRequestDto.username(), jwtLoginRequestDto.password())
            );
            Object principal = authentication.getPrincipal();

            if (!(principal instanceof UserDetails userDetails)) {
                log.error("Неожиданный тип principal: {}", principal != null ? principal.getClass().getName() : "null");
                throw new IllegalStateException("Аутентификация произошла успешно, но principal не является UserDetails");
            }

            log.info("Успешная аутентификация. Логин: {}", userDetails.getUsername());

            authMetrics.recordLoginSuccess(loginSampleStart);

            return new JwtResponseDto(jwtTokenHelper.generateAccessToken(userDetails), jwtTokenHelper.generateRefreshToken(userDetails));
        } catch (Exception e) {
            authMetrics.recordLoginFailure(e);
            log.warn("Ошибка входа: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public JwtResponseDto refresh(@Valid JwtRefreshRequestDto jwtRefreshRequestDto) {

        Timer.Sample refreshTokenStart = Timer.start(meterRegistry);

        try {
            String refreshToken = jwtRefreshRequestDto.refreshToken();

            jwtTokenHelper.validateRefreshToken(refreshToken);

            if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
                throw new TokenRevokedException("Refresh токен отозван");
            }

            Long userId = jwtTokenHelper.getUserIdFromRefreshToken(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserById(userId);

            String newAccessToken = jwtTokenHelper.generateAccessToken(userDetails);

            log.debug("Успешно обновлён токен доступа. ID пользователя: {}", userId);

            authMetrics.recordTokenRefreshSuccess(refreshTokenStart);

            return new JwtResponseDto(newAccessToken, refreshToken);
        } catch (Exception e) {
            authMetrics.recordTokenRefreshFailure(e);
            log.warn("Ошибка обновления токена: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void logout(@Valid JwtLogoutRequestDto jwtLogoutRequestDto) {

        try {
            String accessToken = jwtLogoutRequestDto.accessToken();
            String refreshToken = jwtLogoutRequestDto.refreshToken();

            if (accessToken != null && jwtTokenHelper.isAccessToken(accessToken)) {
                Duration ttl = jwtTokenHelper.getRemainingAccessTtl(accessToken);
                tokenBlacklistService.blacklistToken(accessToken, ttl);
            }

            if (refreshToken != null && jwtTokenHelper.isRefreshToken(refreshToken)) {
                Duration ttl = jwtTokenHelper.getRemainingRefreshTtl(refreshToken);
                tokenBlacklistService.blacklistToken(refreshToken, ttl);
            }

            authMetrics.recordLogout();
            log.info("Успешный выход из системы. Токены отозваны: access={}, refresh={}",
                    accessToken != null, refreshToken != null);
        } catch (Exception e) {
            log.warn("Ошибка при выходе: {}", e.getMessage(), e);
            throw e;
        }
    }
}

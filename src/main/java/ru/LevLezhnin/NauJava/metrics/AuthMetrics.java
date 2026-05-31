package ru.LevLezhnin.NauJava.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Метрики аутентификации: регистрация, вход, обновление токена, ошибки.
 */
@Component
public class AuthMetrics {

    private final Counter registrationSuccess;
    private final Meter.MeterProvider<Counter> registrationFailure;
    private final Counter loginSuccess;
    private final Meter.MeterProvider<Counter>  loginFailure;
    private final Counter tokenRefreshSuccess;
    private final Meter.MeterProvider<Counter>  tokenRefreshFailure;
    private final Counter logoutCount;

    private final Timer registrationTimer;
    private final Timer loginTimer;
    private final Timer tokenRefreshTimer;

    @Autowired
    public AuthMetrics(MeterRegistry registry) {
        registrationSuccess = Counter.builder("auth.registration.success")
                .description("Успешные регистрации новых пользователей")
                .register(registry);

        registrationFailure = Counter.builder("auth.registration.failure")
                .description("Неудачные попытки регистрации")
                .withRegistry(registry);

        loginSuccess = Counter.builder("auth.login.success")
                .description("Успешные входы в систему")
                .register(registry);

        loginFailure = Counter.builder("auth.login.failure")
                .description("Неудачные попытки входа")
                .withRegistry(registry);

        tokenRefreshSuccess = Counter.builder("auth.token.refresh.success")
                .description("Успешные обновления access-токена")
                .register(registry);

        tokenRefreshFailure = Counter.builder("auth.token.refresh.failure")
                .description("Неудачные обновления токена")
                .withRegistry(registry);

        logoutCount = Counter.builder("auth.logout.total")
                .description("Выходы из системы (отзыв токенов)")
                .register(registry);

        registrationTimer = Timer.builder("auth.registration.duration")
                .description("Время обработки запроса регистрации")
                .register(registry);

        loginTimer = Timer.builder("auth.login.duration")
                .description("Время аутентификации пользователя")
                .register(registry);

        tokenRefreshTimer = Timer.builder("auth.token.refresh.duration")
                .description("Время обновления токена")
                .register(registry);
    }

    public void recordRegistrationSuccess(Timer.Sample registrationSampleStart) {
        registrationSuccess.increment();
        registrationSampleStart.stop(registrationTimer);
    }

    public void recordRegistrationFailure(Throwable e) {
        registrationFailure.withTags(Tags.of("reason", extractFailureReason(e))).increment();
    }

    public void recordLoginSuccess(Timer.Sample loginSampleStart) {
        loginSuccess.increment();
        loginSampleStart.stop(loginTimer);
    }

    public void recordLoginFailure(Throwable e) {
        loginFailure.withTags(Tags.of("reason", extractFailureReason(e))).increment();
    }

    public void recordTokenRefreshSuccess(Timer.Sample tokenRefreshStart) {
        tokenRefreshSuccess.increment();
        tokenRefreshStart.stop(tokenRefreshTimer);
    }

    public void recordTokenRefreshFailure(Throwable e) {
        tokenRefreshFailure.withTags(Tags.of("reason", extractFailureReason(e))).increment();
    }

    public void recordLogout() {
        logoutCount.increment();
    }

    private String extractFailureReason(Throwable e) {
        if (e instanceof org.springframework.security.core.AuthenticationException) {
            return "invalid_credentials";
        } else if (e instanceof ru.LevLezhnin.NauJava.exception.auth.TokenRevokedException) {
            return "token_revoked";
        } else if (e instanceof org.springframework.dao.DuplicateKeyException) {
            return "username_taken";
        } else if (e instanceof jakarta.validation.ValidationException) {
            return "validation_error";
        } else if (e instanceof java.lang.IllegalStateException) {
            return "internal_error";
        }
        return "other";
    }
}
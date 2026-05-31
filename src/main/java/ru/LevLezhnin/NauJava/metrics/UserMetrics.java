package ru.LevLezhnin.NauJava.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Метрики управления пользователями: создание, обновление, удаление, конфликты.
 */
@Component
public class UserMetrics {

    private final Counter userCreated;
    private final Counter userUpdated;
    private final Counter userDeleted;
    private final Counter userProfileViewed;

    private final Counter usernameConflict;
    private final Counter emailConflict;
    private final Counter passwordUpdateFailure;
    private final Counter selfActionForbidden;

    private final Timer userCreateTimer;
    private final Timer userUpdateTimer;
    private final Timer userDeleteTimer;

    @Autowired
    public UserMetrics(MeterRegistry registry) {
        userCreated = Counter.builder("user.created.total")
                .description("Успешно созданные пользователи")
                .register(registry);

        userUpdated = Counter.builder("user.updated.total")
                .description("Успешно обновлённые профили")
                .register(registry);

        userDeleted = Counter.builder("user.deleted.total")
                .description("Удалённые пользователи")
                .register(registry);

        userProfileViewed = Counter.builder("user.profile.viewed.total")
                .description("Просмотры собственного профиля")
                .register(registry);

        usernameConflict = Counter.builder("user.conflict.username")
                .description("Попытки регистрации с занятым логином")
                .register(registry);

        emailConflict = Counter.builder("user.conflict.email")
                .description("Попытки регистрации с занятым email")
                .register(registry);

        passwordUpdateFailure = Counter.builder("user.password.update.failure")
                .description("Неудачные попытки смены пароля (неверный текущий)")
                .register(registry);

        selfActionForbidden = Counter.builder("user.operation.self_forbidden")
                .description("Попытки выполнить запрещённое действие над собой (бан/разбан)")
                .register(registry);

        userCreateTimer = Timer.builder("user.create.duration")
                .description("Время создания нового пользователя")
                .register(registry);

        userUpdateTimer = Timer.builder("user.update.duration")
                .description("Время обновления профиля")
                .register(registry);

        userDeleteTimer = Timer.builder("user.delete.duration")
                .description("Время удаления пользователя")
                .register(registry);
    }

    public void recordUserCreated(Timer.Sample createUserStart) {
        userCreated.increment();
        createUserStart.stop(userCreateTimer);
    }

    public void recordUserUpdated(Timer.Sample updateUserStart) {
        userUpdated.increment();
        updateUserStart.stop(userUpdateTimer);
    }

    public void recordUserDeleted(Timer.Sample deleteUserStart) {
        userDeleted.increment();
        deleteUserStart.stop(userDeleteTimer);
    }

    public void recordProfileViewed() {
        userProfileViewed.increment();
    }

    public void recordUsernameConflict() {
        usernameConflict.increment();
    }

    public void recordEmailConflict() {
        emailConflict.increment();
    }

    public void recordPasswordUpdateFailure() {
        passwordUpdateFailure.increment();
    }

    public void recordSelfActionForbidden() {
        selfActionForbidden.increment();
    }
}
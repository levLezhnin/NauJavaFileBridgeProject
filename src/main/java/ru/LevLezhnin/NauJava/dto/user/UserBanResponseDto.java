package ru.LevLezhnin.NauJava.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO ответа с данными о конкретной блокировке пользователя
 * @param id                    ID блокировки (обязательный, непустой)
 * @param bannedUserId          ID заблокированного пользователя (обязательный, может быть пустым, если пользователя нет в БД)
 * @param bannedUserUsername    Логин заблокированного пользователя (обязательный. Если пользователя нет в БД должен быть заполнен как 'Неизвестно')
 * @param adminId               ID админа, выдавшего блокировку (обязательный, может быть пустым, если администратора нет в БД)
 * @param adminUsername         Логин админа, выдавшего блокировку (обязательный. Если администратора нет в БД должен быть заполнен как 'Неизвестно')
 * @param reason                Причина, по которой блокировка была выдана (обязательный, непустой)
 * @param bannedAt              Дата и время блокировки (обязательный, непустой)
 * @param unbannedAt            Дата и время истечения блокировки (опционально, если блокировка была в когда-то снята / будет снята)
 * @author Лев Лежнин
 */
public record UserBanResponseDto(
        String id,
        @JsonProperty("banned_user_id") String bannedUserId,
        @JsonProperty("banned_user_username") String bannedUserUsername,
        @JsonProperty("admin_id") String adminId,
        @JsonProperty("admin_username") String adminUsername,
        String reason,
        @JsonProperty("banned_at") String bannedAt,
        @JsonProperty("unbanned_at") String unbannedAt
) {}

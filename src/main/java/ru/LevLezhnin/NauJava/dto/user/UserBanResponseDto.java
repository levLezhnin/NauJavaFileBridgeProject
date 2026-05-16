package ru.LevLezhnin.NauJava.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

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

package ru.LevLezhnin.NauJava.service.interfaces;

import ru.LevLezhnin.NauJava.dto.user.UserBanResponseDto;

import java.util.List;

public interface UserBanService {
    UserBanResponseDto getUserBanById(Long banId);
    UserBanResponseDto banUserById(Long userId, String reason);
    UserBanResponseDto unbanUserById(Long userId);
    UserBanResponseDto getActiveUserBanByUserId(Long userId);
    List<UserBanResponseDto> getUserBanHistory(Long userId, int page, int pageSize);
    List<UserBanResponseDto> getIssuedBansByAdmin(Long adminId, int page, int pageSize);
}

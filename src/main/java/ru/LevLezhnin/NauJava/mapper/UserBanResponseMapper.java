package ru.LevLezhnin.NauJava.mapper;

import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.dto.user.UserBanResponseDto;
import ru.LevLezhnin.NauJava.model.UserBan;

@Component
public class UserBanResponseMapper implements Mapper<UserBan, UserBanResponseDto> {
    @Override
    public UserBanResponseDto map(UserBan userBan) {
        return new UserBanResponseDto(
                userBan.getId().toString(),
                userBan.getBannedUser() == null ? "" : userBan.getBannedUser().getId().toString(),
                userBan.getBannedUser() == null ? "Неизвестно" : userBan.getBannedUser().getUsername(),
                userBan.getAdmin() == null ? "" : userBan.getAdmin().getId().toString(),
                userBan.getAdmin() == null ? "Неизвестно" : userBan.getAdmin().getUsername(),
                userBan.getReason(),
                userBan.getBannedAt().toString(),
                userBan.getUnbannedAt() == null ? "" : userBan.getUnbannedAt().toString()
        );
    }
}

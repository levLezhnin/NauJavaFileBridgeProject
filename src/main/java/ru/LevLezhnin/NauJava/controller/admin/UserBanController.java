package ru.LevLezhnin.NauJava.controller.admin;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.LevLezhnin.NauJava.dto.user.UserBanRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UserBanResponseDto;
import ru.LevLezhnin.NauJava.service.interfaces.UserBanService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
public class UserBanController {

    private final UserBanService userBanService;

    @Autowired
    public UserBanController(UserBanService userBanService) {
        this.userBanService = userBanService;
    }

    @GetMapping("/bans/{banId}")
    public UserBanResponseDto getBanById(@PathVariable("banId") Long banId) {
        return userBanService.getUserBanById(banId);
    }

    @GetMapping("/ban/{userId}")
    public UserBanResponseDto getActiveUserBanByUserId(@PathVariable("userId") Long bannedUserId) {
        return userBanService.getActiveUserBanByUserId(bannedUserId);
    }

    @GetMapping("/ban/issuedBans/{adminUserId}")
    public List<UserBanResponseDto> getBansIssuedByAdmin(@PathVariable("adminUserId") Long adminId,
                                                           @RequestParam("page") int page,
                                                           @RequestParam("page_size") int pageSize) {
        return userBanService.getIssuedBansByAdmin(adminId, page, pageSize);
    }

    @GetMapping("/ban/history/{userId}")
    public List<UserBanResponseDto> getUserBanHistory(@PathVariable("userId") Long userId,
                                                      @RequestParam("page") int page,
                                                      @RequestParam("page_size") int pageSize) {
        return userBanService.getUserBanHistory(userId, page, pageSize);
    }

    @PostMapping("/ban")
    public UserBanResponseDto banUserById(@RequestBody @Valid UserBanRequestDto userBanRequestDto) {
        return userBanService.banUserById(userBanRequestDto.banUserId(), userBanRequestDto.reason());
    }

    @PostMapping("/unban/{userId}")
    public UserBanResponseDto unbanUserById(@PathVariable("userId") Long bannedUserId) {
        return userBanService.unbanUserById(bannedUserId);
    }
}

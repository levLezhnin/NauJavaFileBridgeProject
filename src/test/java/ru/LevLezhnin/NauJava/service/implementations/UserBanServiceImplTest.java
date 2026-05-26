package ru.LevLezhnin.NauJava.service.implementations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import ru.LevLezhnin.NauJava.dto.user.UserBanResponseDto;
import ru.LevLezhnin.NauJava.exception.common.EntityNotFoundException;
import ru.LevLezhnin.NauJava.exception.common.SelfActionForbiddenException;
import ru.LevLezhnin.NauJava.exception.user.UserAlreadyBannedException;
import ru.LevLezhnin.NauJava.exception.user.UserNotBannedException;
import ru.LevLezhnin.NauJava.mapper.Mapper;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.model.UserBan;
import ru.LevLezhnin.NauJava.model.UserRole;
import ru.LevLezhnin.NauJava.repository.jpa.UserBanRepository;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.security.context.RequestContextService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserBanServiceImpl")
class UserBanServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserBanRepository userBanRepository;
    @Mock
    private Mapper<UserBan, UserBanResponseDto> userBanResponseDtoMapper;
    @Mock
    private RequestContextService requestContextService;

    private UserBanServiceImpl userBanService;

    private static final Long ADMIN_ID = 1L;
    private static final String ADMIN_USERNAME = "admin";
    private static final Long USER_ID = 2L;
    private static final String USER_USERNAME = "user";
    private static final Long BAN_ID = 100L;
    private static final String BAN_REASON = "Нарушение правил: публикация вредоносного контента без предупреждения";
    private static final Instant NOW = Instant.now();

    private User admin;
    private User user;
    private UserBan userBan;
    private UserBanResponseDto banResponseDto;

    @BeforeEach
    void setUp() {
        userBanService = new UserBanServiceImpl(
                userRepository, userBanRepository, userBanResponseDtoMapper, requestContextService
        );

        lenient().when(requestContextService.getUserId()).thenReturn(ADMIN_ID);

        admin = new User();
        admin.setId(ADMIN_ID);
        admin.setRole(UserRole.ADMIN);

        user = new User();
        user.setId(USER_ID);
        user.setRole(UserRole.USER);
        user.setActive(true);

        userBan = UserBan.builder()
                .setId(BAN_ID)
                .setAdmin(admin)
                .setBannedUser(user)
                .setReason(BAN_REASON)
                .setBannedAt(NOW)
                .setUnbannedAt(null)
                .build();

        banResponseDto = new UserBanResponseDto(BAN_ID.toString(), USER_ID.toString(), USER_USERNAME, ADMIN_ID.toString(), ADMIN_USERNAME, BAN_REASON, NOW.toString(), null);

        lenient().when(userBanResponseDtoMapper.map(any(UserBan.class))).thenReturn(banResponseDto);
    }

    @Nested
    @DisplayName("getUserBanById")
    class GetUserBanByIdTests {

        @Test
        @DisplayName("должен вернуть DTO при успешном поиске бана")
        void shouldReturnDtoWhenBanExists() {
            when(userBanRepository.findById(BAN_ID)).thenReturn(Optional.of(userBan));

            UserBanResponseDto result = userBanService.getUserBanById(BAN_ID);

            assertThat(result).isEqualTo(banResponseDto);
            verify(userBanRepository).findById(BAN_ID);
            verify(userBanResponseDtoMapper).map(userBan);
        }

        @Test
        @DisplayName("должен выбросить EntityNotFoundException при отсутствии бана")
        void shouldThrowExceptionWhenBanNotFound() {
            when(userBanRepository.findById(BAN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userBanService.getUserBanById(BAN_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найдена");

            verify(userBanRepository).findById(BAN_ID);
            verifyNoInteractions(userBanResponseDtoMapper);
        }
    }

    @Nested
    @DisplayName("banUserById")
    class BanUserByIdTests {

        @Test
        @DisplayName("должен успешно забанить пользователя")
        void shouldBanUserSuccessfully() {
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
            when(userRepository.findForUpdateById(USER_ID)).thenReturn(Optional.of(user));
            when(userBanRepository.findActiveUserBan(USER_ID)).thenReturn(Optional.empty());

            UserBanResponseDto result = userBanService.banUserById(USER_ID, BAN_REASON);

            assertThat(result).isEqualTo(banResponseDto);
            assertThat(user.isActive()).isFalse();

            verify(userRepository).findForUpdateById(USER_ID);
            verify(userBanRepository).findActiveUserBan(USER_ID);
            verify(userRepository).save(user);
            verify(userBanRepository).save(any(UserBan.class));
            verify(userBanResponseDtoMapper).map(any(UserBan.class));
        }

        @Test
        @DisplayName("должен выбросить SelfActionForbiddenException при попытке забанить себя")
        void shouldThrowExceptionWhenBanningSelf() {
            when(requestContextService.getUserId()).thenReturn(USER_ID);

            assertThatThrownBy(() -> userBanService.banUserById(USER_ID, BAN_REASON))
                    .isInstanceOf(SelfActionForbiddenException.class)
                    .hasMessageContaining("самого себя");

            verifyNoInteractions(userRepository, userBanRepository);
        }

        @Test
        @DisplayName("должен выбросить EntityNotFoundException при отсутствии администратора")
        void shouldThrowExceptionWhenAdminNotFound() {
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userBanService.banUserById(USER_ID, BAN_REASON))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Администратор");

            verify(userRepository).findById(ADMIN_ID);
        }

        @Test
        @DisplayName("должен выбросить AccessDeniedException если текущий пользователь не админ")
        void shouldThrowExceptionWhenCallerIsNotAdmin() {
            User nonAdmin = new User();
            nonAdmin.setId(ADMIN_ID);
            nonAdmin.setRole(UserRole.USER);
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(nonAdmin));

            assertThatThrownBy(() -> userBanService.banUserById(USER_ID, BAN_REASON))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Недостаточно прав");
        }

        @Test
        @DisplayName("должен выбросить EntityNotFoundException при отсутствии пользователя")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
            when(userRepository.findForUpdateById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userBanService.banUserById(USER_ID, BAN_REASON))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Пользователь с id: %d не найден".formatted(USER_ID));
        }

        @Test
        @DisplayName("должен выбросить AccessDeniedException при попытке забанить администратора")
        void shouldThrowExceptionWhenBanningAdmin() {
            User targetAdmin = new User();
            targetAdmin.setId(USER_ID);
            targetAdmin.setRole(UserRole.ADMIN);

            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
            when(userRepository.findForUpdateById(USER_ID)).thenReturn(Optional.of(targetAdmin));

            assertThatThrownBy(() -> userBanService.banUserById(USER_ID, BAN_REASON))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Нельзя заблокировать администратора");
        }

        @Test
        @DisplayName("должен выбросить UserAlreadyBannedException если пользователь уже забанен")
        void shouldThrowExceptionWhenUserAlreadyBanned() {
            UserBan existingBan = UserBan.builder().setId(999L).build();

            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
            when(userRepository.findForUpdateById(USER_ID)).thenReturn(Optional.of(user));
            when(userBanRepository.findActiveUserBan(USER_ID)).thenReturn(Optional.of(existingBan));

            assertThatThrownBy(() -> userBanService.banUserById(USER_ID, BAN_REASON))
                    .isInstanceOf(UserAlreadyBannedException.class)
                    .hasMessageContaining("уже заблокирован");
        }
    }

    @Nested
    @DisplayName("unbanUserById")
    class UnbanUserByIdTests {

        @Test
        @DisplayName("должен успешно разбанить пользователя")
        void shouldUnbanUserSuccessfully() {
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
            when(userRepository.findForUpdateById(USER_ID)).thenReturn(Optional.of(user));
            when(userBanRepository.findActiveUserBanForUpdate(USER_ID)).thenReturn(Optional.of(userBan));

            UserBanResponseDto result = userBanService.unbanUserById(USER_ID);

            assertThat(result).isEqualTo(banResponseDto);
            assertThat(user.isActive()).isTrue();
            assertThat(userBan.getUnbannedAt()).isNotNull();

            verify(userBanRepository).findActiveUserBanForUpdate(USER_ID);
            verify(userBanRepository).save(userBan);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("должен выбросить SelfActionForbiddenException при попытке разбанить себя")
        void shouldThrowExceptionWhenUnbanningSelf() {
            when(requestContextService.getUserId()).thenReturn(USER_ID);

            assertThatThrownBy(() -> userBanService.unbanUserById(USER_ID))
                    .isInstanceOf(SelfActionForbiddenException.class);
        }

        @Test
        @DisplayName("должен выбросить EntityNotFoundException при отсутствии пользователя")
        void shouldThrowExceptionWhenUserNotFoundForUnban() {
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
            when(userRepository.findForUpdateById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userBanService.unbanUserById(USER_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("должен выбросить UserNotBannedException если пользователь не забанен")
        void shouldThrowExceptionWhenUserNotBanned() {
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
            when(userRepository.findForUpdateById(USER_ID)).thenReturn(Optional.of(user));
            when(userBanRepository.findActiveUserBanForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userBanService.unbanUserById(USER_ID))
                    .isInstanceOf(UserNotBannedException.class)
                    .hasMessageContaining("не заблокирован");
        }
    }

    @Nested
    @DisplayName("getActiveUserBanByUserId")
    class GetActiveUserBanByUserIdTests {

        @Test
        @DisplayName("должен вернуть активный бан пользователя")
        void shouldReturnActiveBan() {
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
            when(userBanRepository.findActiveUserBan(USER_ID)).thenReturn(Optional.of(userBan));

            UserBanResponseDto result = userBanService.getActiveUserBanByUserId(USER_ID);

            assertThat(result).isEqualTo(banResponseDto);
            verify(userBanRepository).findActiveUserBan(USER_ID);
            verify(userBanResponseDtoMapper).map(userBan);
        }

        @Test
        @DisplayName("должен выбросить исключение если пользователь не забанен")
        void shouldThrowExceptionWhenNoActiveBan() {
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
            when(userBanRepository.findActiveUserBan(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userBanService.getActiveUserBanByUserId(USER_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не заблокирован");
        }
    }

    @Nested
    @DisplayName("getUserBanHistory")
    class GetUserBanHistoryTests {

        @Test
        @DisplayName("должен вернуть историю банов пользователя")
        void shouldReturnBanHistory() {
            List<UserBan> banHistory = List.of(userBan);
            List<UserBanResponseDto> expectedDtoList = List.of(banResponseDto);

            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userBanRepository.findUserBanHistory(eq(USER_ID), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(banHistory));
            when(userBanResponseDtoMapper.map(userBan)).thenReturn(banResponseDto);

            List<UserBanResponseDto> result = userBanService.getUserBanHistory(USER_ID, 0, 10);

            assertThat(result).isEqualTo(expectedDtoList);
            verify(userBanRepository).findUserBanHistory(eq(USER_ID), eq(PageRequest.of(0, 10)));
        }

        @Test
        @DisplayName("должен выбросить исключение если пользователь не найден")
        void shouldThrowExceptionWhenUserNotFoundForHistory() {
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userBanService.getUserBanHistory(USER_ID, 0, 10))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getIssuedBansByAdmin")
    class GetIssuedBansByAdminTests {

        @Test
        @DisplayName("должен вернуть историю выданных банов администратором")
        void shouldReturnIssuedBansHistory() {
            List<UserBan> issuedBans = List.of(userBan);
            List<UserBanResponseDto> expectedDtoList = List.of(banResponseDto);

            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
            when(userBanRepository.findIssuedBanHistory(eq(ADMIN_ID), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(issuedBans));
            when(userBanResponseDtoMapper.map(userBan)).thenReturn(banResponseDto);

            List<UserBanResponseDto> result = userBanService.getIssuedBansByAdmin(ADMIN_ID, 0, 10);

            assertThat(result).isEqualTo(expectedDtoList);
            verify(userBanRepository).findIssuedBanHistory(eq(ADMIN_ID), eq(PageRequest.of(0, 10)));
        }

        @Test
        @DisplayName("должен выбросить исключение если администратор не найден")
        void shouldThrowExceptionWhenTargetAdminNotFound() {
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userBanService.getIssuedBansByAdmin(ADMIN_ID, 0, 10))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("должен выбросить IllegalArgumentException если целевой пользователь не админ")
        void shouldThrowExceptionWhenTargetIsNotAdmin() {
            User nonAdminTarget = new User();
            nonAdminTarget.setId(ADMIN_ID);
            nonAdminTarget.setRole(UserRole.USER);

            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(nonAdminTarget));

            assertThatThrownBy(() -> userBanService.getIssuedBansByAdmin(ADMIN_ID, 0, 10))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Недостаточно прав");
        }
    }

    @Nested
    @DisplayName("Edge cases и дополнительные проверки")
    class EdgeCasesTests {
        @Test
        @DisplayName("должен корректно маппить несколько банов в истории")
        void shouldMapMultipleBansInHistory() {
            UserBan ban1 = UserBan.builder().setId(1L).build();
            UserBan ban2 = UserBan.builder().setId(2L).build();
            UserBanResponseDto dto1 = new UserBanResponseDto("1", null, null, null, null, null, null, null);
            UserBanResponseDto dto2 = new UserBanResponseDto("2", null, null, null, null, null, null, null);

            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userBanRepository.findUserBanHistory(eq(USER_ID), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of(ban1, ban2)));
            when(userBanResponseDtoMapper.map(ban1)).thenReturn(dto1);
            when(userBanResponseDtoMapper.map(ban2)).thenReturn(dto2);

            List<UserBanResponseDto> result = userBanService.getUserBanHistory(USER_ID, 0, 10);

            assertThat(result).containsExactly(dto1, dto2);
        }
    }
}
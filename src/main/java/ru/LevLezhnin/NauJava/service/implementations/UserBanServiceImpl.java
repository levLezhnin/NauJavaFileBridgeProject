package ru.LevLezhnin.NauJava.service.implementations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import ru.LevLezhnin.NauJava.service.interfaces.UserBanService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class UserBanServiceImpl implements UserBanService {

    private static final Logger log = LoggerFactory.getLogger(UserBanServiceImpl.class);

    private final UserRepository userRepository;
    private final UserBanRepository userBanRepository;

    private final Mapper<UserBan, UserBanResponseDto> userBanResponseDtoMapper;

    private final RequestContextService requestContextService;

    @Autowired
    public UserBanServiceImpl(UserRepository userRepository,
                              UserBanRepository userBanRepository,
                              Mapper<UserBan, UserBanResponseDto> userBanResponseDtoMapper,
                              RequestContextService requestContextService) {
        this.userRepository = userRepository;
        this.userBanRepository = userBanRepository;
        this.userBanResponseDtoMapper = userBanResponseDtoMapper;
        this.requestContextService = requestContextService;
    }

    private User checkAdminRightsAndReturnEntity(Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> {
                    log.error("Администратор не найден. ID администратора: {}", adminId);
                    return new EntityNotFoundException("Администратор с id: %d не найден".formatted(adminId));
                });

        if (!UserRole.ADMIN.equals(admin.getRole())) {
            log.warn("Пользователь {} попытался выполнить действие, не имея на это прав администратора", adminId);
            throw new AccessDeniedException("Недостаточно прав для выполнения операции");
        }
        return admin;
    }

    @Override
    @Transactional(readOnly = true)
    public UserBanResponseDto getUserBanById(Long banId) {
        log.debug("Запрос блокировки по id. ID бана: {}, ID администратора, запрашивающего бан: {}",
                banId, requestContextService.getUserId());
        UserBan userBan = userBanRepository.findById(banId)
                .orElseThrow(() -> new EntityNotFoundException("Блокировка с id: %d не найдена".formatted(banId)));
        return userBanResponseDtoMapper.map(userBan);
    }

    @Override
    @Transactional
    public UserBanResponseDto banUserById(Long userId, String reason) {

        Long adminId = requestContextService.getUserId();

        log.debug("Запрос на блокировку пользователя по id. ID пользователя: {}, ID администратора, запрашивающего блокировку: {}",
                userId, adminId);

        if (userId.equals(adminId)) {
            throw new SelfActionForbiddenException("Нельзя заблокировать самого себя");
        }

        User admin = checkAdminRightsAndReturnEntity(adminId);

        User user = userRepository.findForUpdateById(userId)
                .orElseThrow(() -> {
                    log.warn("Не найден пользователь, для которого запрошена блокировка. ID администратора: {}, ID пользователя: {}",
                            adminId, userId);
                    return new EntityNotFoundException("Пользователь с id: %d не найден".formatted(userId));
                });

        if (UserRole.ADMIN.equals(user.getRole())) {
            log.warn("Попытка заблокировать администратора с ID: {} другим администратором с ID: {}", user.getId(), admin.getId());
            throw new AccessDeniedException("Нельзя заблокировать администратора");
        }

        Optional<UserBan> activeBan = userBanRepository.findActiveUserBan(userId);
        if (activeBan.isPresent()) {
            throw new UserAlreadyBannedException("Пользователь с id: %d уже заблокирован. ID блокировки: %d".formatted(userId, activeBan.get().getId()));
        }

        UserBan userBan = UserBan.builder()
                .setAdmin(admin)
                .setBannedUser(user)
                .setReason(reason)
                .setBannedAt(Instant.now())
                .setUnbannedAt(null)
                .build();

        user.applyBan();
        userRepository.save(user);
        userBanRepository.save(userBan);

        log.info("Администратор {} заблокировал пользователя {} по причине: {}. ID блокировки: {}",
                adminId, userId, reason, userBan.getId());

        return userBanResponseDtoMapper.map(userBan);
    }

    @Override
    @Transactional
    public UserBanResponseDto unbanUserById(Long userId) {
        Long adminId = requestContextService.getUserId();

        log.debug("Запрос на разблокирование пользователя по id. ID пользователя: {}, ID администратора, запрашивающего разблокировку: {}",
                userId, adminId);

        if (userId.equals(adminId)) {
            throw new SelfActionForbiddenException("Нельзя разблокировать самого себя");
        }

        checkAdminRightsAndReturnEntity(adminId);

        Optional<User> userOpt = userRepository.findForUpdateById(userId);

        if (userOpt.isEmpty()) {
            log.warn("Не найден пользователь, для которого запрошено снятие блокировки. ID администратора: {}, ID пользователя: {}",
                    adminId, userId);
            throw new EntityNotFoundException("Пользователь с id: %d не найден".formatted(userId));
        }

        User user = userOpt.get();

        UserBan activeBan = userBanRepository.findActiveUserBanForUpdate(userId)
                .orElseThrow(() -> new UserNotBannedException("Пользователь с id: %d не заблокирован".formatted(userId)));

        activeBan.setUnbannedAt(Instant.now());
        user.applyUnban();

        userBanRepository.save(activeBan);
        userRepository.save(user);

        log.info("Администратор {} разблокировал пользователя {}. ID блокировки: {}", adminId, userId, activeBan.getId());

        return userBanResponseDtoMapper.map(activeBan);
    }

    @Override
    @Transactional(readOnly = true)
    public UserBanResponseDto getActiveUserBanByUserId(Long userId) {

        log.debug("Запрос активной блокировки по id пользователя. ID пользователя: {}, ID администратора, запрашивающего активную блокировку: {}",
                userId, requestContextService.getUserId());

        checkAdminRightsAndReturnEntity(requestContextService.getUserId());

        UserBan activeUserBan = userBanRepository.findActiveUserBan(userId)
                .orElseThrow(() -> {
                    log.warn("Запрошена активная блокировка для незаблокированного пользователя. ID пользователя: {}, ID администратора: {}",
                            userId, requestContextService.getUserId());
                    return new EntityNotFoundException("Пользователь с id: %d не заблокирован".formatted(userId));
                }
        );
        return userBanResponseDtoMapper.map(activeUserBan);
    }

    @Override
    public List<UserBanResponseDto> getUserBanHistory(Long userId, int page, int pageSize) {

        log.debug("Запрос истории блокировок по id пользователя. ID пользователя: {}, ID администратора, запрашивающего историю блокировок: {}",
                userId, requestContextService.getUserId());

        checkAdminRightsAndReturnEntity(requestContextService.getUserId());

        if (userRepository.findById(userId).isEmpty()) {
            log.warn("Попытка посмотреть историю блокировок несуществующего пользователя. ID администратора: {}, ID пользователя из запроса: {}",
                    requestContextService.getUserId(), userId);
            throw new EntityNotFoundException("Пользователь с id: %d не найден".formatted(userId));
        }

        return userBanRepository.findUserBanHistory(userId, PageRequest.of(page, pageSize))
                .stream()
                .map(userBanResponseDtoMapper::map)
                .toList();
    }

    @Override
    public List<UserBanResponseDto> getIssuedBansByAdmin(Long adminId, int page, int pageSize) {

        log.debug("Запрос истории выданных блокировок по id администратора. ID целевого администратора: {}, ID администратора, запрашивающего историю выданных блокировок: {}",
                adminId, requestContextService.getUserId());

        checkAdminRightsAndReturnEntity(requestContextService.getUserId());

        User admin = userRepository.findById(adminId).orElseThrow(() -> {
            log.warn("Попытка посмотреть историю выданных блокировок для несуществующего администратора. ID администратора: {}, ID запрашиваемого администратора: {}",
                    requestContextService.getUserId(), adminId);
            return new EntityNotFoundException("Пользователь с id: %d не найден".formatted(adminId));
        });

        if (!UserRole.ADMIN.equals(admin.getRole())) {
            log.warn("Попытка посмотреть историю выданных блокировок для пользователя (USER). ID администратора: {}, ID пользователя из запроса: {}",
                    requestContextService.getUserId(), adminId);
            throw new IllegalArgumentException("Нельзя посмотреть историю выданных блокировок, так как пользователь с id: %d - пользователь".formatted(adminId));
        }

        return userBanRepository.findIssuedBanHistory(adminId, PageRequest.of(page, pageSize))
                .stream()
                .map(userBanResponseDtoMapper::map)
                .toList();
    }
}

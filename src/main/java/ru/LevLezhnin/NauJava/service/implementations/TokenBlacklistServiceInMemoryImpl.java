package ru.LevLezhnin.NauJava.service.implementations;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import ru.LevLezhnin.NauJava.service.interfaces.TokenBlacklistService;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistServiceInMemoryImpl implements TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistServiceInMemoryImpl.class);
    private static final String TOKEN_BLACKLIST_KEY_PATTERN = "token:blacklist:%s";

    private final ConcurrentHashMap<String, Instant> blackList = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanUpJobExecutor = Executors.newSingleThreadScheduledExecutor();

    public TokenBlacklistServiceInMemoryImpl() {
        cleanUpJobExecutor.scheduleAtFixedRate(this::cleanUp, 5, 5, TimeUnit.MINUTES);
    }

    private String hashString(String string) {
        if (string == null) return "";
        return DigestUtils.md5DigestAsHex(string.getBytes());
    }

    private String getTokenKey(String token) {
        return TOKEN_BLACKLIST_KEY_PATTERN.formatted(hashString(token));
    }

    @Override
    public void blacklistToken(String token, Duration ttl) {
        if (token == null || ttl == null || ttl.isNegative()) {
            return;
        }
        blackList.put(getTokenKey(token), Instant.now().plus(ttl));
        log.debug("Токен добавлен в черный список на {} сек", ttl.getSeconds());
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        if (token == null) {
            return false;
        }
        String tokenKey = getTokenKey(token);

        Instant expiry = blackList.get(tokenKey);
        if (expiry == null) {
            return false;
        }

        if (Instant.now().isAfter(expiry)) {
            blackList.remove(tokenKey);
            return false;
        }
        return true;
    }

    private void cleanUp() {
        Instant now = Instant.now();
        blackList.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }

    @PreDestroy
    public void shutdown() {
        log.info("Запущена остановка очищающего потока для черного списка jwt-токенов");
        cleanUpJobExecutor.shutdown();
        try {
            if (!cleanUpJobExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanUpJobExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanUpJobExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

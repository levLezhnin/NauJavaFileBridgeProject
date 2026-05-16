package ru.LevLezhnin.NauJava.service.interfaces;

import java.time.Duration;

public interface TokenBlacklistService {
    void blacklistToken(String token, Duration ttl);
    boolean isTokenBlacklisted(String token);
}

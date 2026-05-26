package ru.LevLezhnin.NauJava.security.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.exception.auth.InvalidTokenException;
import ru.LevLezhnin.NauJava.exception.auth.TokenExpiredException;
import ru.LevLezhnin.NauJava.security.properties.JwtProperties;
import ru.LevLezhnin.NauJava.security.userdetails.AppUserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class JwtTokenHelper {

    private final JwtProperties jwtProperties;

    public JwtTokenHelper(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    private SecretKey getAccessSignKey() {
        byte[] keyBytes = jwtProperties.getAccessSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private SecretKey getRefreshSignKey() {
        byte[] keyBytes = jwtProperties.getRefreshSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> payload = new HashMap<>();
        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        payload.put("roles", roles);

        if (userDetails instanceof AppUserDetails appUserDetails) {
            payload.put("userId", appUserDetails.getId());
        }

        Date issuedDate = new Date();
        Date expiredDate = new Date(issuedDate.getTime() + jwtProperties.getAccessTokenLifetime().toMillis());
        return Jwts.builder()
                .claims(payload)
                .claim("type", "access")
                .subject(userDetails.getUsername())
                .issuedAt(issuedDate)
                .expiration(expiredDate)
                .signWith(getAccessSignKey())
                .compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> payload = new HashMap<>();
        if (userDetails instanceof AppUserDetails appUserDetails) {
            payload.put("userId", appUserDetails.getId());
        }

        Date issuedDate = new Date();
        Date expiredDate = new Date(issuedDate.getTime() + jwtProperties.getRefreshTokenLifetime().toMillis());

        return Jwts.builder()
                .claims(payload)
                .claim("type", "refresh")
                .subject(userDetails.getUsername())
                .issuedAt(issuedDate)
                .expiration(expiredDate)
                .signWith(getRefreshSignKey())
                .compact();
    }

    private Claims getClaimsFromAccessToken(String accessToken) {
        return Jwts.parser()
                .verifyWith(getAccessSignKey())
                .build()
                .parseSignedClaims(accessToken)
                .getPayload();
    }

    private Claims getClaimsFromRefreshToken(String refreshToken) {
        return Jwts.parser()
                .verifyWith(getRefreshSignKey())
                .build()
                .parseSignedClaims(refreshToken)
                .getPayload();
    }

    public Long getUserIdFromAccessToken(String token) {
        try {
            Claims claims = getClaimsFromAccessToken(token);
            return claims.get("userId", Long.class);
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("Истёк срок действия access токена");
        } catch (JwtException e) {
            throw new InvalidTokenException("Невалидный access токен");
        }
    }

    public Long getUserIdFromRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromRefreshToken(token);
            return claims.get("userId", Long.class);
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("Истёк срок действия refresh токена");
        } catch (JwtException e) {
            throw new InvalidTokenException("Невалидный refresh токен");
        }
    }

    public String getUserNameFromAccessToken(String token) {
        try {
            return getClaimsFromAccessToken(token).getSubject();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("Истёк срок действия access токена");
        } catch (JwtException e) {
            throw new InvalidTokenException("Невалидный access токен");
        }
    }
    public String getUserNameFromRefreshToken(String token) {
        try {
            return getClaimsFromRefreshToken(token).getSubject();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("Истёк срок действия refresh токена");
        } catch (JwtException e) {
            throw new InvalidTokenException("Невалидный refresh токен");
        }
    }

    public List<String> getUserRoles(String accessToken) {
        try {
            return getClaimsFromAccessToken(accessToken).get("roles", List.class);
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("Истёк срок действия access токена");
        } catch (JwtException e) {
            throw new InvalidTokenException("Приведён невалидный access токен");
        }
    }

    public boolean isAccessToken(String token) {
        try {
            return "access".equals(getClaimsFromAccessToken(token).get("type"));
        } catch (JwtException e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(getClaimsFromRefreshToken(token).get("type"));
        } catch (JwtException e) {
            return false;
        }
    }

    public void validateRefreshToken(String refreshToken) {
        try {
            Claims claims = getClaimsFromRefreshToken(refreshToken);
            if (!"refresh".equals(claims.get("type"))) {
                throw new InvalidTokenException("Невалидный тип токена");
            }
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("Истёк срок действия refresh токена");
        } catch (JwtException e) {
            throw new InvalidTokenException("Невалидный refresh токен");
        }
    }

    public Duration getRemainingAccessTtl(String accessToken) {
        return getRemainingTtl(() -> getClaimsFromAccessToken(accessToken));
    }

    public Duration getRemainingRefreshTtl(String refreshToken) {
        return getRemainingTtl(() -> getClaimsFromRefreshToken(refreshToken));
    }

    private Duration getRemainingTtl(Supplier<Claims> claimsSupplier) {
        try {
            Claims claims = claimsSupplier.get();
            Instant now = Instant.now();
            Instant expiry = claims.getExpiration().toInstant();
            Duration duration = Duration.between(now, expiry);
            return duration.isPositive() ? duration : Duration.ZERO;
        } catch (JwtException e) {
            return Duration.ZERO;
        }
    }
}
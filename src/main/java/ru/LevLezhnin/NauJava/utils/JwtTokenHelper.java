package ru.LevLezhnin.NauJava.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.security.AppUserDetails;
import ru.LevLezhnin.NauJava.security.JwtProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        Date issuedDate = new Date();
        Date expiredDate = new Date(issuedDate.getTime() + jwtProperties.getRefreshTokenLifetime().toMillis());

        return Jwts.builder()
                .claim("type", "refresh")
                .subject(userDetails.getUsername())
                .issuedAt(issuedDate)
                .expiration(expiredDate)
                .signWith(getRefreshSignKey())
                .compact();
    }

    public Long getUserIdFromAccessToken(String token) {
        Claims claims = getClaimsFromAccessToken(token);
        return claims.get("userId", Long.class);
    }

    public String getUserNameFromAccessToken(String token) {
        return getClaimsFromAccessToken(token).getSubject();
    }
    public String getUserNameFromRefreshToken(String token) {
        return getClaimsFromRefreshToken(token).getSubject();
    }

    public List<String> getUserRoles(String token) {
        return getClaimsFromAccessToken(token).get("roles", List.class);
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

    public boolean isAccessToken(String token) {
        return "access".equals(getClaimsFromAccessToken(token).get("type"));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getClaimsFromRefreshToken(token).get("type"));
    }
}
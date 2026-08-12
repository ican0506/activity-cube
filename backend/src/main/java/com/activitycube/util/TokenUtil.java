package com.activitycube.util;

import com.activitycube.common.BusinessException;
import com.activitycube.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TokenUtil {
    private static final int MIN_SECRET_BYTES = 32;
    private static final String ROLE_CLAIM = "role";

    private final JwtProperties properties;

    public String createToken(Long userId, String role) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getExpirationSeconds());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(requireIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(ROLE_CLAIM, role)
                .signWith(signingKey())
                .compact();
    }

    public Optional<ParsedToken> parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .requireIssuer(requireIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.parseLong(claims.getSubject());
            String role = claims.get(ROLE_CLAIM, String.class);
            return Optional.of(new ParsedToken(userId, role));
        } catch (JwtException | IllegalArgumentException | IllegalStateException | BusinessException exception) {
            return Optional.empty();
        }
    }

    public Optional<Long> parseUserId(String token) {
        return parseToken(token).map(ParsedToken::userId);
    }

    private SecretKey signingKey() {
        String secret = properties.getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new BusinessException("JWT_SECRET 未配置，无法生成登录凭证");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new BusinessException("JWT_SECRET 长度不足，至少需要 32 字节");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String requireIssuer() {
        if (!StringUtils.hasText(properties.getIssuer())) {
            throw new BusinessException("JWT_ISSUER 未配置");
        }
        return properties.getIssuer();
    }

    public record ParsedToken(Long userId, String role) {
    }
}

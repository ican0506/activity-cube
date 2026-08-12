package com.activitycube.util;

import com.activitycube.common.BusinessException;
import com.activitycube.config.JwtProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenUtilTest {
    private static final String SECRET = "12345678901234567890123456789012";

    @Test
    void createsAndParsesJwtToken() {
        TokenUtil tokenUtil = tokenUtil(SECRET, 7200, "activity-cube");

        String token = tokenUtil.createToken(9L, "student");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
        TokenUtil.ParsedToken parsed = tokenUtil.parseToken(token).orElseThrow();
        assertThat(parsed.userId()).isEqualTo(9L);
        assertThat(parsed.role()).isEqualTo("student");
    }

    @Test
    void rejectsInvalidTokenString() {
        TokenUtil tokenUtil = tokenUtil(SECRET, 7200, "activity-cube");

        assertThat(tokenUtil.parseToken("not-a-jwt")).isEmpty();
    }

    @Test
    void rejectsTamperedToken() {
        TokenUtil tokenUtil = tokenUtil(SECRET, 7200, "activity-cube");
        String token = tokenUtil.createToken(9L, "student");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(tokenUtil.parseToken(tampered)).isEmpty();
    }

    @Test
    void rejectsTokenSignedByDifferentSecret() {
        String token = tokenUtil(SECRET, 7200, "activity-cube").createToken(9L, "student");

        assertThat(tokenUtil("abcdefghijklmnopqrstuvwxyz123456", 7200, "activity-cube").parseToken(token)).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        TokenUtil tokenUtil = tokenUtil(SECRET, -1, "activity-cube");

        String token = tokenUtil.createToken(9L, "student");

        assertThat(tokenUtil.parseToken(token)).isEmpty();
    }

    @Test
    void rejectsTokenWithDifferentIssuer() {
        String token = tokenUtil(SECRET, 7200, "activity-cube").createToken(9L, "student");

        assertThat(tokenUtil(SECRET, 7200, "other-issuer").parseToken(token)).isEmpty();
    }

    @Test
    void rejectsEmptyToken() {
        TokenUtil tokenUtil = tokenUtil(SECRET, 7200, "activity-cube");

        assertThat(tokenUtil.parseToken(null)).isEmpty();
        assertThat(tokenUtil.parseToken("")).isEmpty();
        assertThat(tokenUtil.parseToken("   ")).isEmpty();
    }

    @Test
    void refusesToCreateTokenWhenSecretIsMissing() {
        TokenUtil tokenUtil = tokenUtil("", 7200, "activity-cube");

        assertThatThrownBy(() -> tokenUtil.createToken(9L, "student"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("JWT_SECRET 未配置，无法生成登录凭证");
    }

    @Test
    void refusesToCreateTokenWhenSecretIsTooShort() {
        TokenUtil tokenUtil = tokenUtil("short-secret", 7200, "activity-cube");

        assertThatThrownBy(() -> tokenUtil.createToken(9L, "student"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("JWT_SECRET 长度不足，至少需要 32 字节");
    }

    private TokenUtil tokenUtil(String secret, long expirationSeconds, String issuer) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        properties.setExpirationSeconds(expirationSeconds);
        properties.setIssuer(issuer);
        return new TokenUtil(properties);
    }
}

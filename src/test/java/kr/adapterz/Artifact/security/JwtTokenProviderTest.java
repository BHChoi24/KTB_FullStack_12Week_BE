package kr.adapterz.Artifact.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenProviderTest {
    @Test
    void 토큰에_사용자ID와_토큰버전을_함께_저장한다() {
        JwtTokenProvider provider = new JwtTokenProvider(
                "test-jwt-secret-key-must-be-at-least-32-bytes",
                60_000
        );

        String token = provider.createToken(7L, 3);
        JwtTokenProvider.JwtUserClaims claims = provider.getUserClaims(token);

        assertEquals(7L, claims.userId());
        assertEquals(3, claims.tokenVersion());
    }
}

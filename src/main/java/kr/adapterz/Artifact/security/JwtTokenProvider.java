package kr.adapterz.Artifact.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** JWT를 생성하고, 서명과 만료 시간을 검증하는 클래스입니다. */
@Component
public class JwtTokenProvider {
    //비밀키, 만료시간 설정
    private final SecretKey secretKey;
    private final long expirationMillis;


    // 비밀키, 만료시간 있는 곳 application.properties
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-millis}") long expirationMillis
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    /**
     * 로그인 사용자 ID와 현재 토큰 버전을 함께 넣어 JWT를 발급합니다.
     * 탈퇴·복구로 DB 버전이 바뀌면 이전 토큰은 즉시 사용할 수 없습니다.
     */
    public String createToken(Long userId, int tokenVersion) {
        // 현재 시간
        Date now = new Date();
        // 현재시간 + 발급 시간
        Date expiration = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(userId.toString())
                // 사용자의 현재 버전을 토큰에 고정하여 이후 DB 버전 변경을 감지합니다.
                .claim("tokenVersion", tokenVersion)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /** 서명과 만료 시간이 정상인 토큰에서 사용자 ID와 토큰 버전을 꺼냅니다. */
    // 토큰을 해석해서 Payload를 꺼내는 과정 자체에서 검증이 함께 진행 -> 다르면 에외나옴
    public JwtUserClaims getUserClaims(String token) {
        // parseSignedClaims 과정에서 서명 위조와 만료 시간이 함께 검증됩니다.
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Integer tokenVersion = claims.get("tokenVersion", Integer.class);
        return new JwtUserClaims(
                Long.valueOf(claims.getSubject()),
                // 배포 전에 발급된 JWT에는 버전 클레임이 없으므로 기존 활성 사용자는 0으로 호환합니다.
                tokenVersion == null ? 0 : tokenVersion
        );
    }

    public record JwtUserClaims(Long userId, int tokenVersion) {}
}

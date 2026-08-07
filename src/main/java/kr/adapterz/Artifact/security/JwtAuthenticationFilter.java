package kr.adapterz.Artifact.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.adapterz.Artifact.entity.User;
import kr.adapterz.Artifact.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** 각 HTTP 요청에서 Bearer 토큰을 한 번 검사하는 필터. */
//OncePerRequestFilter는 Spring에서 제공하는 HTTP 요청 필터의 기본 클래스
//상속받는 이유는, 클라이언트 요청이 Controller에 도착하기 전에 JWT를 확인하기 위해
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // Authorization 헤더에서 JWT 앞에 붙어야 하는 고정 문자열
    private static final String BEARER_PREFIX = "Bearer ";

    // JWT 생성/검증 로직이 든 클래스
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;


    //jwtTokenProvider: 토큰 검증과 userId 추출
    //userRepository: 토큰 사용자가 실제 DB에 존재하는지 확인
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }


    //요청이 들어올 때 실행되는 필터의 본문
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        // 헤더 앞 문자열 비교 확인
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            // 헤더 앞 문자열 제거하고 JWT 문자열만 가져오기
            String token = authorization.substring(BEARER_PREFIX.length());

            try {
                JwtTokenProvider.JwtUserClaims claims =
                        jwtTokenProvider.getUserClaims(token);
                Long userId = claims.userId();
                User user = userRepository.findById(userId).orElse(null);

                // 탈퇴 여부와 토큰 버전을 함께 확인해 탈퇴 전에 발급된 JWT의 재사용을 차단합니다.
                if (user != null
                        && !user.isDeleted()
                        && user.getTokenVersion() == claims.tokenVersion()) {
                    var authority = new SimpleGrantedAuthority(
                            "ROLE_" + user.getRole().name()
                    );

                    // 인증된 사용자를 인증 객체로 만듬
                    var authentication = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(authority)
                    );
                    // 교재에서 언급한 중요 포인트 -> 이 코드가 실행되어야 Spring Security는 "아, 이 요청은 인증된 사용자가 보낸 것이구나"라고 인지하고, 이후의 authorizeHttpRequests 검사를 통과
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // 잘못되거나 만료된 토큰은 인증을 등록X
                // 보호된 URL이면 이후 SecurityConfig에 의해 401 처리됨
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}

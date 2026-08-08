package kr.adapterz.Artifact.config;

import kr.adapterz.Artifact.security.JwtAuthenticationFilter;
import kr.adapterz.Artifact.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

//스프링 설정 클래스 -> Spring Boot가 실행될 때 이 파일을 읽고 보안 설정으로 등록
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    }

    /** 비밀번호 원문 대신 단방향 BCrypt 해시를 저장하기 위한 공통 인코더입니다. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    //보안 필터 체인을 직접 정의
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                //CORS 설정을 사용
                .cors(Customizer.withDefaults())
                // 쿠키 사용할 예정이니까 일단 CSRF보안 끔 -> 쿠키에 저장하면 JWT써도 위험하니까 꼭 헤더에
                .csrf(csrf -> csrf.disable())


                // JWT는 서버 세션을 저장하지 않고 요청마다 토큰을 확인합니다.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 토큰이 없거나 유효하지 않을 때 공통 JSON 형식의 401 응답을 보냅니다.
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                )


                // Spring Security는 기본적으로 iframe 렌더링을 막습니다.
                // H2 Console 화면은 iframe 구조라서 같은 출처(localhost)에서는 frame을 허용해야 정상 표시된다
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                )


                //URL별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // H2 Console 접속 경로를 인증 없이 허용합니다.
                        // application.properties의 spring.h2.console.path=/h2-console 설정과 맞춘 경로입니다.
                        .requestMatchers("/h2-console/**").permitAll()
                        // 회원가입과 로그인은 아직 로그인 전 사용해야 하므로 인증 없이 허용합니다.
                        .requestMatchers(
                                "/users/signup",
                                "/users/login",
                                "/users/recovery"
                        ).permitAll()
                        // Docker 내부 health check가 인증 토큰 없이 기동 상태를 확인합니다.
                        .requestMatchers("/actuator/health").permitAll()
                        // 게시글과 댓글에서 프로필 이미지를 보여줄 수 있도록 업로드 파일 조회를 공개합니다.
                        .requestMatchers("/uploads/**").permitAll()
                        .anyRequest().authenticated()
                )

                // 일반 로그인 필터보다 먼저 JWT를 검사해 인증 정보를 등록합니다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

package kr.adapterz.Artifact.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    // CORS 규칙을 담을 객체
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 프론트 개발 서버에서 오는 요청만 허용
        // 배포 환경에서는 실제 프론트 도메인을 여기에 추가하거나 분리해서 관리하면 된다
        config.setAllowedOrigins(List.of(
                "http://localhost:5500",
                "http://127.0.0.1:5500",
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));

        // API에서 사용할 HTTP 메서드를 허용합니다.
        // OPTIONS는 브라우저가 CORS 사전 확인 요청을 보낼 때 필요합니다.
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Authorization 헤더를 포함한 요청 헤더를 허용합니다.
        config.setAllowedHeaders(List.of("*"));

        // 브라우저의 인증 관련 요청을 허용합니다.
        // allowCredentials=true일 때는 allowedOrigins에 "*"를 사용할 수 없습니다.
        config.setAllowCredentials(true);


        // CORS 규칙 적용할 URL 담을 객체
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 API 경로에 위 CORS 규칙을 적용합니다.
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}

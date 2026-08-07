package kr.adapterz.Artifact.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.adapterz.Artifact.response.ErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static kr.adapterz.Artifact.response.code.ErrorCode.UNAUTHORIZED;

/** 인증 실패, 유효하지 않을 때 프로젝트의 공통 JSON 형식으로 401을 반환합니다. */
//AuthenticationEntryPoint 인증실패가 담긴 인터페이스 Spring Security가 제공
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    //ObjectMapper -> Java 객체와 JSON 사이를 변환
    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     *보호된 URL에 인증되지 않은 사용자가 접근하면 실행
     * request: 들어온 요청
     * response: 클라이언트에 보낼 응답
     * authException: 인증에 실패했다는 정보
     **/
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        //401 Unauthorized로 지정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        //한글 등이 깨지지 않도록 UTF-8을 지정
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        //응답 내용이 JSON
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        //작성했던 응답 메시지 형태
        objectMapper.writeValue(
                response.getWriter(),
                new ErrorResponse(UNAUTHORIZED, List.of())
        );
    }
}

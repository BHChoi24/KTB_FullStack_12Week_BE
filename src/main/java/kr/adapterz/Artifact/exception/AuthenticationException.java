package kr.adapterz.Artifact.exception;

import kr.adapterz.Artifact.response.code.ErrorCode;

/**
 * 인증에 실패했을 때 발생시키는 예외입니다.
 *
 * 인증은 "현재 요청을 보낸 사용자가 누구인지 확인하는 과정"입니다.
 * 다음과 같은 경우에 이 예외를 사용합니다.
 * - 로그인 이메일 또는 비밀번호가 올바르지 않은 경우
 * - JWT가 없거나 존재하지 않는 사용자 번호인 경우
 *
 * Service에서 이 예외를 throw하면 현재 메서드 실행이 즉시 중단됩니다.
 * 이후 GlobalExceptionHandler가 예외를 받아 HTTP 401 Unauthorized로 변환합니다.
 */
public class AuthenticationException extends ApiException {
    public AuthenticationException(ErrorCode errorCode) {
        // RuntimeException에 메시지를 저장합니다.
        // handler에서는 e.getMessage()로 이 값을 꺼내 응답 message에 사용합니다.
        super(errorCode);
    }
}

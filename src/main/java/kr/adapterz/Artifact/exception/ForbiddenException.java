package kr.adapterz.Artifact.exception;

import kr.adapterz.Artifact.response.code.ErrorCode;

/**
 * 인증된 사용자에게 해당 작업의 권한이 없을 때 발생시키는 예외
 * - 403: 로그인은 확인했지만 다른 사람의 글이나 댓글에 접근함
 * GlobalExceptionHandler가 이 예외를 HTTP 403 Forbidden으로 변환
 */
public class ForbiddenException extends ApiException {
    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }
}

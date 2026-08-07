package kr.adapterz.Artifact.exception;

import kr.adapterz.Artifact.response.code.ErrorCode;

/**
 * 요청한 사용자, 게시글 또는 댓글이 저장소에 없을 때 발생시키는 예외입니다.
 *
 * 예시:
 * - 없는 게시글 번호로 GET /posts/999 요청
 * - 없는 댓글 번호를 수정 또는 삭제
 *
 * 생성자에 posts_not_found, user_not_found 같은 메시지를 전달하면
 * GlobalExceptionHandler가 그 메시지를 유지하면서 HTTP 404로 반환합니다.
 */
public class NotFoundException extends ApiException {
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}

package kr.adapterz.Artifact.exception;

import kr.adapterz.Artifact.response.code.ErrorCode;

/**
 * 게시글 목록의 page 값이 잘못되었을 때 발생시키는 예외입니다.
 * 잘못된 요청값은 클라이언트가 수정해야 하므로 HTTP 400 Bad Request를 사용합니다.
 */
public class InvalidPageException extends ApiException {
    public InvalidPageException() {
        // 페이지 오류 메시지는 API 명세에서 정한 값으로 고정합니다.
        super(ErrorCode.INVALID_PAGE_PARAMETER);
    }
}

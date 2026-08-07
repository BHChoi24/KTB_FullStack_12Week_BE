package kr.adapterz.Artifact.response.code;

/** 실패 응답과 예외가 사용할 message 값을 한곳에서 관리합니다. */
public enum ErrorCode implements ResponseCode {
    UNAUTHORIZED("unauthorized"),
    EMAIL_PASSWORD_CHECK("email_password_check"),
    ACCOUNT_RECOVERY_UNAVAILABLE("account_recovery_unavailable"),
    USER_NOT_FOUND("user_not_found"),
    POSTS_NOT_FOUND("posts_not_found"),
    COMMENT_NOT_FOUND("comment_not_found"),
    RESOURCE_NOT_FOUND("resource_not_found"),
    FORBIDDEN_AUTHOR("forbidden_author"),
    FORBIDDEN_NOTICE_MANAGEMENT("forbidden_notice_management"),
    INVALID_INPUT_VALUE("invalid_input_value"),
    INVALID_PAGE_PARAMETER("invalid_page_parameter"),
    INTERNAL_SERVER_ERROR("internal_server_error");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}

package kr.adapterz.Artifact.response.code;

/** Bean Validation 어노테이션에서도 사용할 수 있도록 컴파일 상수로 관리합니다. */
public final class ValidationMessage {
    public static final String EMAIL_EMPTY = "email_empty";
    public static final String EMAIL_INVALID = "email_invalid";
    public static final String EMAIL_DUPLICATE = "email_duplicate";
    public static final String PASSWORD_EMPTY = "password_empty";
    public static final String PASSWORD_INVALID = "password_invalid";
    public static final String PASSWORD_CHECK_EMPTY = "password_check_empty";
    public static final String PASSWORD_CHECK_INVALID = "password_check_invalid";
    public static final String PASSWORD_CHECK_NOT_SAME = "password_check_not_same";
    public static final String NICKNAME_EMPTY = "nickname_empty";
    public static final String NICKNAME_INVALID = "nickname_invalid";
    public static final String NICKNAME_DUPLICATE = "nickname_duplicate";
    public static final String TITLE_EMPTY = "title_empty";
    public static final String TITLE_INVALID = "title_invalid";
    public static final String CONTENT_EMPTY = "content_empty";
    public static final String INVALID_JSON = "invalid_json";
    public static final String INVALID_PARAMETER_TYPE = "invalid_parameter_type";
    public static final String PROFILE_IMAGE_INVALID = "profile_image_invalid";
    public static final String PROFILE_IMAGE_TOO_LARGE = "profile_image_too_large";
    public static final String POST_IMAGE_INVALID = "post_image_invalid";
    public static final String POST_IMAGE_TOO_LARGE = "post_image_too_large";
    public static final String CATEGORY_EMPTY = "category_empty";
    public static final String PINNED_NOTICE_ONLY = "pinned_notice_only";
    public static final String PINNED_LIMIT_EXCEEDED = "pinned_limit_exceeded";
    public static final String KEYWORD_TOO_SHORT = "검색 가능한 단어는 2글자 이상 입력해야 합니다.";

    private ValidationMessage() {
    }
}

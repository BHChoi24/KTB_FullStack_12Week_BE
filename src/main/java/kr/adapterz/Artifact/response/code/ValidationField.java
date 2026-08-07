package kr.adapterz.Artifact.response.code;

/** 서비스와 공통 예외 처리에서 사용하는 검증 대상 필드명입니다. */
public final class ValidationField {
    public static final String EMAIL = "email";
    public static final String NICKNAME = "nickname";
    public static final String PASSWORD_CHECK = "password_check";
    public static final String TITLE = "title";
    public static final String CONTENT = "content";
    public static final String BODY = "body";
    public static final String PROFILE_IMAGE = "profile_image";
    public static final String POST_IMAGE = "post_image";
    public static final String CATEGORY = "category";
    public static final String PINNED = "pinned";

    private ValidationField() {
    }
}

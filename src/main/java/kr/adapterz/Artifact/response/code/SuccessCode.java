package kr.adapterz.Artifact.response.code;

/** 정상 처리 결과의 message 값을 한곳에서 관리합니다. */
public enum SuccessCode implements ResponseCode {
    LOGIN_SUCCESS("login_success"),
    USER_ADD_SUCCESS("user_add_success"),
    USER_ACCESS_SUCCESS("user_access_success"),
    USER_MODIFY_SUCCESS("user_modify_success"),
    DELETE_USER_SUCCESS("delete_user_success"),
    USER_RECOVERY_SUCCESS("user_recovery_success"),
    PASSWORD_CHANGE_SUCCESS("password_change_success"),

    POST_ADD_SUCCESS("posts_add_success"),
    POST_LIST_SUCCESS("post_list_success"),
    POST_DETAIL_SUCCESS("post_detail_success"),
    POST_LIKE_TOGGLE_SUCCESS("post_like_toggle_success"),
    POST_UPDATE_SUCCESS("update_success"),
    POST_DELETE_SUCCESS("delete_success"),

    COMMENT_CREATE_SUCCESS("comment_create_success"),
    COMMENT_UPDATE_SUCCESS("comment_update_success"),
    COMMENT_DELETE_SUCCESS("comment_delete_success");

    private final String code;

    SuccessCode(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}

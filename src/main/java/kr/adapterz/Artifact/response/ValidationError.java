package kr.adapterz.Artifact.response;

/** 어떤 입력 필드가 왜 잘못되었는지 표현합니다.
 * 에러 응답에 어떤 값들이 들어가는 지
 * */
public class ValidationError {
    // 예: email, password_check, nickname
    private String field;

    // 예: email_duplicate, password_check_not_same
    private String reason;

    public ValidationError(String field, String reason) {
        this.field = field;
        this.reason = reason;
    }

    public String getField() { return field; }
    public String getReason() { return reason; }
}

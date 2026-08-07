package kr.adapterz.Artifact.response;

import kr.adapterz.Artifact.response.code.ErrorCode;

import java.util.List;

/** 입력값 검증 실패 시 message, data, errors 구조를 만드는 응답 클래스입니다. */
public class ErrorResponse {
    // 입력값 오류의 공통 메시지: invalid_input_value
    private String message;

    // 실패 응답에서는 실제 결과 데이터가 없으므로 항상 null입니다.
    private Object data;

    // 잘못된 필드와 이유를 배열로 반환합니다.
    private List<ValidationError> errors;

    // 에러응답 형태 동일
    public ErrorResponse(ErrorCode message, List<ValidationError> errors) {
        this.message = message.getCode();
        this.data = null;
        this.errors = errors;
    }

    public String getMessage() { return message; }
    public Object getData() { return data; }
    public List<ValidationError> getErrors() { return errors; }
}

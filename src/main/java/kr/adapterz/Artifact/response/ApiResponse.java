package kr.adapterz.Artifact.response;

import kr.adapterz.Artifact.response.code.SuccessCode;

/** 모든 API가 같은 message/data JSON 구조를 사용하도록 만드는 공통 응답 클래스입니다. */
public class ApiResponse<T> {
    private String message;
    private T data;

    private ApiResponse(String message, T data) {
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> of(SuccessCode message, T data) {
        return new ApiResponse<>(message.getCode(), data);
    }

    public String getMessage() { return message; }
    public T getData() { return data; }
}

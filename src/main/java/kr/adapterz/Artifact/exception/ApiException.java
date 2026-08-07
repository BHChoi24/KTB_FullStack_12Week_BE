package kr.adapterz.Artifact.exception;

import kr.adapterz.Artifact.response.code.ErrorCode;

/** 애플리케이션 예외가 문자열 대신 타입이 보장된 오류 코드를 보관하도록 합니다. */
public abstract class ApiException extends RuntimeException {
    private final ErrorCode errorCode;

    protected ApiException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

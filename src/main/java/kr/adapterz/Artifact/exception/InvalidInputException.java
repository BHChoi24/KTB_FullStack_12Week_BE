package kr.adapterz.Artifact.exception;

import kr.adapterz.Artifact.response.ValidationError;
import kr.adapterz.Artifact.response.code.ErrorCode;

import java.util.List;

/**
 * Service에서 직접 발견한 입력값 오류를 표현합니다.
 *
 * DTO의 @NotBlank, @Email 같은 검사는 Spring이 자동 처리하지만,
 * 이메일 중복이나 비밀번호 확인처럼 저장소 조회 또는 값 비교가 필요한 검사는
 * Service에서 직접 검사해야 합니다.
 *
 * errors가 List인 이유는 API 명세의 errors 배열 구조에 맞추기 위해서입니다.
 */
// 잘못된 입력 예외 처리하는 클래스, 런타임예외를 상속 받음(예외처리를 강제하지 않기 위해)
public class InvalidInputException extends ApiException {
    // 어떤 필드가 어떤 이유로 잘못되었는지 보관합니다.
    private final List<ValidationError> errors;

    //
    public InvalidInputException(String field, String reason) {
        // 상위 예외 메시지는 공통 에러 코드로 저장합니다.
        super(ErrorCode.INVALID_INPUT_VALUE);

        // 현재는 한 번에 한 개의 서비스 검증 오류를 담습니다.
        this.errors = List.of(new ValidationError(field, reason));
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}

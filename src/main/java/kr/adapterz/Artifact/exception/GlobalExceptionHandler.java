package kr.adapterz.Artifact.exception;


import jakarta.servlet.http.HttpServletRequest;
import kr.adapterz.Artifact.response.ErrorResponse;
import kr.adapterz.Artifact.response.ValidationError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static kr.adapterz.Artifact.response.code.ErrorCode.INTERNAL_SERVER_ERROR;
import static kr.adapterz.Artifact.response.code.ErrorCode.INVALID_INPUT_VALUE;
import static kr.adapterz.Artifact.response.code.ErrorCode.FORBIDDEN_AUTHOR;
import static kr.adapterz.Artifact.response.code.ErrorCode.RESOURCE_NOT_FOUND;
import static kr.adapterz.Artifact.response.code.ValidationField.BODY;
import static kr.adapterz.Artifact.response.code.ValidationField.POST_IMAGE;
import static kr.adapterz.Artifact.response.code.ValidationField.PROFILE_IMAGE;
import static kr.adapterz.Artifact.response.code.ValidationMessage.INVALID_JSON;
import static kr.adapterz.Artifact.response.code.ValidationMessage.INVALID_PARAMETER_TYPE;
import static kr.adapterz.Artifact.response.code.ValidationMessage.POST_IMAGE_TOO_LARGE;
import static kr.adapterz.Artifact.response.code.ValidationMessage.PROFILE_IMAGE_TOO_LARGE;

/**
 * 애플리케이션에서 발생한 예외를 HTTP 응답으로 바꾸는 공통 처리 클래스입니다.
 *
 * 예외 처리 흐름:
 * 1. Controller가 Service 메서드를 호출합니다.
 * 2. Service가 잘못된 상황을 발견하면 throw new ...Exception()을 실행합니다.
 * 3. throw가 실행되면 Service와 Controller의 나머지 코드는 실행되지 않습니다.
 * 4. @RestControllerAdvice가 붙은 이 클래스가 예외를 가로챕니다.
 * 5. 예외 종류에 맞는 @ExceptionHandler 메서드가 상태 코드와 JSON을 반환합니다.
 *
 * 전역 예외 핸들러 사용이유 : 각 Controller에 try-catch를 반복해서 작성하지 않아도 되는 이점
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 요청한 리소스를 찾지 못한 경우를 처리합니다.
     *
     * 예: 없는 userId, postId, commentId로 조회/수정/삭제를 요청한 경우
     * HTTP 상태: 404 Not Found
     * 응답 형태: message에는 구체적인 not_found 코드, errors는 빈 배열
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public ErrorResponse handleNotFoundException(NotFoundException e) {
        // 예외 생성 시 저장한 메시지를 그대로 API 응답에 사용합니다.
        return new ErrorResponse(e.getErrorCode(), List.of());
    }

    /**
     * 중요: 삭제되었거나 존재하지 않는 업로드 파일은 서버 장애가 아니라 404로 응답합니다.
     * 이 예외를 따로 처리하지 않으면 아래의 Exception 안전망이 500으로 변환합니다.
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException.class)
    public ErrorResponse handleNoResourceFoundException(NoResourceFoundException e) {
        return new ErrorResponse(RESOURCE_NOT_FOUND, List.of());
    }

    /**
     * DTO의 @Valid 검증 실패를 처리합니다.
     *
     * 예: @NotBlank, @Email 같은 DTO 어노테이션 검증을 통과하지 못한 경우
     * HTTP 상태: 400 Bad Request
     * 응답 형태: message는 invalid_input_value, errors에는 실패한 필드 목록
     * Spring이 MethodArgumentNotValidException을 자동으로 발생시킵니다.
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException e) {
        // 한 요청에서 여러 필드가 잘못될 수 있으므로 모든 필드 오류를 List로 변환합니다.
        List<ValidationError> validationErrors = e.getBindingResult().getFieldErrors().stream()
                // error.getField(): 잘못된 필드명, getDefaultMessage(): DTO 어노테이션의 message
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
                .toList();

        return new ErrorResponse(INVALID_INPUT_VALUE, validationErrors);
    }

    /**
     * Service에서 발견한 중복 이메일, 비밀번호 불일치 등을 400으로 처리합니다.
     *
     * 예: 이메일 중복, 닉네임 중복, 비밀번호 확인 불일치, 빈 제목/본문
     * HTTP 상태: 400 Bad Request
     * 응답 형태: message는 invalid_input_value, errors에는 Service가 넣은 field/reason
     * 위 메서드는 어노테이션 검증용이고, 이 메서드는 직접 작성한 검증용입니다.
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidInputException.class)
    public ErrorResponse handleInvalidInputException(InvalidInputException e) {
        return new ErrorResponse(e.getErrorCode(), e.getErrors());
    }

    /**
     * 요청이 Controller에 도달하기 전에 Spring의 멀티파트 크기 제한을 초과한 경우를 처리합니다.
     * 일반 500 오류가 아니라 요청 경로에 따라 profile_image 또는 post_image의
     * 400 검증 오류로 응답합니다.
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ErrorResponse handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e,
            HttpServletRequest request
    ) {
        // 게시글 멀티파트 요청은 post_image, 회원가입 요청은 profile_image 오류로 구분합니다.
        boolean isPostImageRequest = request.getRequestURI().startsWith("/posts");
        return new ErrorResponse(
                INVALID_INPUT_VALUE,
                List.of(new ValidationError(
                        isPostImageRequest ? POST_IMAGE : PROFILE_IMAGE,
                        isPostImageRequest
                                ? POST_IMAGE_TOO_LARGE
                                : PROFILE_IMAGE_TOO_LARGE
                ))
        );
    }

    /**
     * 게시글 목록의 page 값이 잘못된 경우를 처리합니다.
     *
     * 예: page가 1보다 작거나, 존재하지 않는 페이지를 요청한 경우
     * HTTP 상태: 400 Bad Request
     * 응답 형태: message는 invalid_page_parameter, errors는 빈 배열
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidPageException.class)
    public ErrorResponse handleInvalidPageException(InvalidPageException e) {
        return new ErrorResponse(e.getErrorCode(), List.of());
    }

    /**
     * 인증에 실패한 경우를 처리합니다.
     *
     * 예: JWT가 없거나, 로그인 이메일/비밀번호가 틀렸거나, 탈퇴한 유저인 경우
     * HTTP 상태: 401 Unauthorized
     * 응답 형태: message는 unauthorized 또는 email_password_check, errors는 빈 배열
     */
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException.class)
    public ErrorResponse handleAuthenticationException(AuthenticationException e) {
        return new ErrorResponse(e.getErrorCode(), List.of());
    }

    /**
     * 인증은 됐지만 권한이 없는 경우를 처리합니다.
     *
     * 예: 다른 사용자의 게시글/댓글을 수정하거나 삭제하려는 경우
     * HTTP 상태: 403 Forbidden
     * 응답 형태: message는 forbidden_author, errors는 빈 배열
     */
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(ForbiddenException.class)
    public ErrorResponse handleForbiddenException(ForbiddenException e) {
        return new ErrorResponse(e.getErrorCode(), List.of());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ErrorResponse handleAccessDeniedException(AccessDeniedException e) {
        return new ErrorResponse(FORBIDDEN_AUTHOR, List.of());
    }

    /**
     * 위에서 예상하지 못한 나머지 모든 예외를 처리하는 마지막 안전망입니다.
     *
     * 예: 개발자가 미처 처리하지 못한 서버 내부 오류
     * HTTP 상태: 500 Internal Server Error
     * 응답 형태: message는 internal_server_error, errors는 빈 배열
     * 서버 내부 오류 내용은 보안상 클라이언트에게 노출하지 않습니다.
     * Exception.class는 범위가 넓으므로 반드시 구체적인 예외 처리 메서드보다 아래에 둡니다.
     */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
        // 서버 내부 오류 내용은 보안상 클라이언트에게 노출하지 않습니다.
        // ResponseEntity를 사용해 상태 코드 500과 응답 body를 함께 만듭니다.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(INTERNAL_SERVER_ERROR, List.of()));
    }


    /**
     * 요청 body를 Java 객체로 바꾸지 못한 경우를 처리합니다.
     *
     * 예: JSON 문법이 깨졌거나, body 타입이 DTO와 맞지 않는 경우
     * HTTP 상태: 400 Bad Request
     * 응답 형태: message는 invalid_input_value, errors에는 body/invalid_json
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ErrorResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException e){
        //메소드이름
        //JSON 문법이 깨졌을 때 500(기존)이 아니라 400으로 응답
        return new ErrorResponse(
                INVALID_INPUT_VALUE,
                List.of(new ValidationError(BODY, INVALID_JSON))
        );
    }
    /**
     * URL 경로 변수 타입이 맞지 않는 경우를 처리합니다.
     *
     * 예: /posts/{postId}에는 숫자가 와야 하는데 /posts/abc로 요청한 경우
     * HTTP 상태: 400 Bad Request
     * 응답 형태: message는 invalid_input_value, errors에는 postId/invalid_parameter_type
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ErrorResponse handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e
    ) {
        return new ErrorResponse(
                INVALID_INPUT_VALUE,
                List.of(new ValidationError(e.getName(), INVALID_PARAMETER_TYPE))
        );
    }

}

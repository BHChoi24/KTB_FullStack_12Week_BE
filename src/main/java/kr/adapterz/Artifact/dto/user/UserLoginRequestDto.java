package kr.adapterz.Artifact.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static kr.adapterz.Artifact.response.code.ValidationMessage.*;

/**
 * [클래스 역할] Postman으로 로그인 요청(POST /users/login) 시 클라이언트가 보내는 JSON 바디 데이터를 바인딩하고 검증하는 객체입니다.
 */
public class UserLoginRequestDto {

    // 시트 조건 반영: 비어있을 경우 GlobalExceptionHandler를 통해 "email_empty" 사유를 반환합니다.
    @NotBlank(message = EMAIL_EMPTY)
    @Email(message = EMAIL_INVALID)
    private String email;

    // 시트 조건 반영: 비어있을 경우 "password_empty" 사유를 반환합니다.
    @NotBlank(message = PASSWORD_EMPTY)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,20}$",
            message = PASSWORD_INVALID
    )
    private String password;

    public UserLoginRequestDto() {}

    public UserLoginRequestDto(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
}

package kr.adapterz.Artifact.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static kr.adapterz.Artifact.response.code.ValidationMessage.EMAIL_EMPTY;
import static kr.adapterz.Artifact.response.code.ValidationMessage.EMAIL_INVALID;
import static kr.adapterz.Artifact.response.code.ValidationMessage.PASSWORD_EMPTY;
import static kr.adapterz.Artifact.response.code.ValidationMessage.PASSWORD_INVALID;

/**
 * 탈퇴 계정 복구에 사용할 이메일과 기존 비밀번호를 전달받는 요청 DTO입니다.
 */
public class UserRecoveryRequestDto {
    @NotBlank(message = EMAIL_EMPTY)
    @Email(message = EMAIL_INVALID)
    private String email;

    @NotBlank(message = PASSWORD_EMPTY)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,20}$",
            message = PASSWORD_INVALID
    )
    private String password;

    public UserRecoveryRequestDto() {}

    public UserRecoveryRequestDto(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
}

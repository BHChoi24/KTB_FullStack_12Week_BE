package kr.adapterz.Artifact.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static kr.adapterz.Artifact.response.code.ValidationMessage.*;

public class PasswordUpdateRequestDto {
    @NotBlank(message = PASSWORD_EMPTY)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,20}$",
            message = PASSWORD_INVALID
    )
    private String password;

    @NotBlank(message = PASSWORD_CHECK_EMPTY)
    private String passwordCheck;

    public String getPassword() { return password; }
    public String getPasswordCheck() { return passwordCheck; }
}

package kr.adapterz.Artifact.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import static kr.adapterz.Artifact.response.code.ValidationMessage.*;

/**
 * [클래스 역할] 회원가입(POST /users/signup) 요청 시 입력 필드의 유효성을 일차적으로 검증
 */

//회원가입 요청 형식 변환, 각각의 회원가입 규칙
public class UserSignupRequestDto {

    //회원가입 형식에 맞게 각 값들 받음
    @NotBlank(message = EMAIL_EMPTY) //이메일이 비워져 있으면 메세지
    @Email(message = EMAIL_INVALID)//이메일 형식 유효성 체크, 안맞으면 메세지 던짐
    private String email;

    @NotBlank(message = PASSWORD_EMPTY) //비밀번호 비워져 있으면 오류 메세지
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,20}$",
            message = PASSWORD_INVALID
    )// 비밀번호는 8자 이상 20자 이하이고, 대문자/소문자/숫자/특수문자를 각각 1개 이상 포함
    private String password;

    //비번 확인 빈칸
    @NotBlank(message = PASSWORD_CHECK_EMPTY)
    private String passwordCheck;

    //닉네임 확인 빈칸
    @NotBlank(message = NICKNAME_EMPTY)
    @Pattern(regexp = "^\\S+$", message = NICKNAME_INVALID)
    @Size(max = 10, message = NICKNAME_INVALID)
    private String nickname;


    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getPasswordCheck() { return passwordCheck; }
    public String getNickname() { return nickname; }
}

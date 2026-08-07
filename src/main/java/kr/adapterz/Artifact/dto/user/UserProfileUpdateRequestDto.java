package kr.adapterz.Artifact.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static kr.adapterz.Artifact.response.code.ValidationMessage.NICKNAME_EMPTY;
import static kr.adapterz.Artifact.response.code.ValidationMessage.NICKNAME_INVALID;

public class UserProfileUpdateRequestDto {
    @NotBlank(message = NICKNAME_EMPTY)
    @Pattern(regexp = "^\\S+$", message = NICKNAME_INVALID)
    @Size(max = 10, message = NICKNAME_INVALID)
    private String nickname;
    // true이면 새 파일을 올리지 않고 현재 프로필 이미지를 제거해 기본 이미지로 돌아갑니다.
    private boolean removeProfileImage;

    public String getNickname() { return nickname; }
    public boolean isRemoveProfileImage() { return removeProfileImage; }
}

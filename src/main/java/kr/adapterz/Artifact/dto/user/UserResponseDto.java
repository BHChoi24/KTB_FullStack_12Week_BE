package kr.adapterz.Artifact.dto.user;

import kr.adapterz.Artifact.entity.User;
import kr.adapterz.Artifact.entity.UserRole;
import lombok.Getter;

/**
 * [클래스 역할] 내 정보 조회(GET /users/me) 시 비밀번호와 같은 민감 정보를 제외하고 안전한 데이터만 응답하기 위한 객체입니다.
 */
@Getter
public class UserResponseDto {
    private String email;
    private String nickname;
    private String profileImage;
    private UserRole role;

    public UserResponseDto(User user) {
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.profileImage = user.getProfileImage();
        this.role = user.getRole();
    }

    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public String getProfileImage() { return profileImage; }
    public UserRole getRole() { return role; }
}

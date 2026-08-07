package kr.adapterz.Artifact.dto.user;

import kr.adapterz.Artifact.entity.User;

/**
 * 프로필 수정 직후 프론트가 별도 조회 없이 닉네임과 이미지 경로를 갱신할 수 있는 응답입니다.
 */
public class UserProfileUpdateResponseDto {
    private final Long userId;
    private final String nickname;
    private final String profileImage;

    public UserProfileUpdateResponseDto(User user) {
        this.userId = user.getId();
        this.nickname = user.getNickname();
        this.profileImage = user.getProfileImage();
    }

    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getProfileImage() { return profileImage; }
}

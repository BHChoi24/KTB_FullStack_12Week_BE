package kr.adapterz.Artifact.dto.user;

/**
 * 게시글과 댓글에 표시할 최소 사용자 정보입니다.
 *
 * <p>비밀번호나 이메일처럼 화면에 필요하지 않은 회원 정보는 제외하고,
 * 표시용 닉네임과 프로필 이미지 경로만 조회 계층에 전달합니다.</p>
 */
public class UserDisplayInfoDto {
    // 게시글과 댓글 화면에 표시할 이름입니다. 탈퇴 회원은 "알 수 없음"이 들어갑니다.
    private final String nickname;
    // DB에 저장된 공개 상대 경로입니다. 이미지가 없거나 탈퇴 회원이면 null입니다.
    private final String profileImage;

    public UserDisplayInfoDto(String nickname, String profileImage) {
        // User 엔티티 전체를 전달하지 않고 화면에 필요한 두 값만 복사합니다.
        this.nickname = nickname;
        this.profileImage = profileImage;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImage() {
        return profileImage;
    }
}

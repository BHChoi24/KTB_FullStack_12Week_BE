package kr.adapterz.Artifact.dto.user;

import kr.adapterz.Artifact.entity.UserRole;

/**
 * [클래스 역할] 로그인 성공 시 시트 명세서에 정의된 JSON 포맷대로 응답 데이터를 내려주기 위한 객체입니다.
 */
public class UserLoginResponseDto {
    private String token;     // 로그인 성공 후 서버가 서명하여 발급한 JWT
    private Long userId;      // 로그인 사용자와 게시글·댓글 작성자를 비교할 사용자 번호
    private String nickname;  // 로그인에 성공한 사용자의 닉네임
    private String profileImage; // 실제 파일이 아닌 /uploads/profiles/... 공개 URL 경로
    private UserRole role;

    public UserLoginResponseDto(String token, Long userId, String nickname, String profileImage, UserRole role) {
        // 로그인 직후 헤더를 표시할 수 있도록 인증 정보와 프로필 경로를 함께 전달합니다.
        this.token = token;
        this.userId = userId;
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.role = role;
    }

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getProfileImage() { return profileImage; }
    public UserRole getRole() { return role; }
}

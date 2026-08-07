package kr.adapterz.Artifact.dto.user;


import kr.adapterz.Artifact.entity.User;

/** 회원가입이 완료된 사용자의 ID를 반환합니다. */
public class UserSignupResponseDto {
    private Long userId;

    public UserSignupResponseDto(User user) {
        this.userId = user.getId();
    }

    public Long getUserId() { return userId; }
}

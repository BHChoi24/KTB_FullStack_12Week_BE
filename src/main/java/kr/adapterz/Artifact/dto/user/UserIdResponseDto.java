package kr.adapterz.Artifact.dto.user;

/** 회원 관련 변경 작업 후 대상 회원 번호만 반환하는 응답 DTO입니다. */
public class UserIdResponseDto {
    private final Long userId;

    public UserIdResponseDto(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}

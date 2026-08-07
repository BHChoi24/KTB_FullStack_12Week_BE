package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 탈퇴 계정의 복구 가능 기간을 한곳에서 계산하는 정책 객체입니다. */
@Component
public class AccountRecoveryPolicy {
    // application.properties의 기본값은 30일이며 테스트·운영 환경에서 설정으로 바꿀 수 있습니다.
    private final int retentionDays;

    public AccountRecoveryPolicy(
            @Value("${app.account-recovery.retention-days:30}") int retentionDays
    ) {
        this.retentionDays = retentionDays;
    }

    /**
     * 복구 가능 조건을 한 메서드로 묶어 API와 만료 작업이 서로 다른 기준을 쓰지 않게 합니다.
     * deletedAt + 보관일이 현재 시각보다 뒤에 있어야 아직 만료되지 않은 상태입니다.
     */
    public boolean isRecoverable(User user, LocalDateTime now) {
        return user.isDeleted()
                && !user.isAnonymized()
                && user.getDeletedAt().plusDays(retentionDays).isAfter(now);
    }

    /** 스케줄러가 조회할 "이 시각보다 먼저 탈퇴한 사용자"의 경계값을 반환합니다. */
    public LocalDateTime expiredBefore(LocalDateTime now) {
        return now.minusDays(retentionDays);
    }
}

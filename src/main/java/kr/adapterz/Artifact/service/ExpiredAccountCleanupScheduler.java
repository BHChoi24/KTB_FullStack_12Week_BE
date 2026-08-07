package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.entity.User;
import kr.adapterz.Artifact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** 매시간 복구 기한이 지난 탈퇴 계정을 최대 100개씩 익명화합니다. */
@Component
@RequiredArgsConstructor
public class ExpiredAccountCleanupScheduler {
    private static final Logger log =
            LoggerFactory.getLogger(ExpiredAccountCleanupScheduler.class);

    private final UserRepository userRepository;
    private final AccountRecoveryPolicy recoveryPolicy;
    private final ExpiredAccountCleanupWorker cleanupWorker;

    @Scheduled(
            cron = "${app.account-recovery.cleanup-cron:0 0 * * * *}",
            zone = "${app.account-recovery.cleanup-zone:Asia/Seoul}"
    )
    public void cleanupExpiredAccounts() {
        // 한 번 계산한 now를 모든 사용자에게 전달하여 같은 실행 안에서 만료 기준이 달라지지 않게 합니다.
        LocalDateTime now = LocalDateTime.now();
        // 대량 사용자를 한 트랜잭션으로 처리하지 않고 오래된 계정부터 최대 100개만 가져옵니다.
        List<User> expiredUsers =
                userRepository
                        .findTop100ByDeletedAtBeforeAndAnonymizedAtIsNullOrderByDeletedAtAsc(
                                recoveryPolicy.expiredBefore(now)
                        );

        for (User user : expiredUsers) {
            try {
                // Worker가 사용자별 새 트랜잭션과 행 잠금을 담당합니다.
                cleanupWorker.cleanup(user.getId(), now);
            } catch (RuntimeException e) {
                // 다음 스케줄에서 이 사용자만 다시 시도할 수 있도록 나머지 계정 처리는 계속합니다.
                log.warn("만료 계정 익명화 실패: userId={}", user.getId(), e);
            }
        }
    }
}

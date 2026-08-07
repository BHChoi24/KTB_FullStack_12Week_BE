package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * DB가 참조하지 않는 공개 프로필 파일을 정리하여 탈퇴 후 삭제 실패나
 * 프로세스 중단으로 발생한 고아 파일을 다음 실행에서 다시 제거합니다.
 */
@Component
@RequiredArgsConstructor
public class ProfileImageOrphanCleanupScheduler {
    private final UserRepository userRepository;
    private final ProfileImageRecoveryStorageService recoveryStorageService;

    @Value("${app.account-recovery.orphan-grace-hours:2}")
    private long orphanGraceHours;

    @Scheduled(
            cron = "${app.account-recovery.orphan-cleanup-cron:0 30 * * * *}",
            zone = "${app.account-recovery.cleanup-zone:Asia/Seoul}"
    )
    public void cleanupOrphanedPublicProfiles() {
        // DB가 현재 사용하는 경로를 허용 목록으로 넘기므로 활성 사용자의 이미지는 삭제하지 않습니다.
        recoveryStorageService.deleteUnreferencedPublicFiles(
                userRepository.findAllReferencedProfileImages(),
                // DB 커밋 전 잠깐 미참조 상태인 새 파일을 지우지 않도록 생성 후 2시간을 기다립니다.
                Duration.ofHours(orphanGraceHours)
        );
    }
}

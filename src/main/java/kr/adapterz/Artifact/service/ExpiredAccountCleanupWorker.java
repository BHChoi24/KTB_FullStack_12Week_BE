package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.entity.User;
import kr.adapterz.Artifact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 만료 사용자 한 명을 잠근 뒤 복구 파일과 개인정보를 정리합니다.
 * 사용자별 트랜잭션으로 실행하여 한 계정의 실패가 다른 계정 정리를 막지 않게 합니다.
 */
@Service
@RequiredArgsConstructor
public class ExpiredAccountCleanupWorker {
    private final UserRepository userRepository;
    private final ProfileImageRecoveryStorageService recoveryStorageService;
    private final AccountRecoveryPolicy recoveryPolicy;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void cleanup(Long userId, LocalDateTime now) {
        // 복구 요청과 만료 처리가 겹치지 않도록 해당 사용자 행에 쓰기 잠금을 겁니다.
        User user = userRepository.findByIdForRecovery(userId).orElse(null);
        // 스케줄러 조회 이후 계정이 복구됐을 수 있으므로 잠금을 얻은 뒤 상태를 다시 검사합니다.
        if (user == null
                || !user.isDeleted()
                || user.isAnonymized()
                || recoveryPolicy.isRecoverable(user, now)) {
            return;
        }

        // 파일 삭제 실패 시 예외를 발생시켜 익명화를 하지 않고 다음 스케줄에서 다시 시도합니다.
        recoveryStorageService.deleteRecoveryStrict(
                user.getRecoveryProfileImage()
        );
        // 게시글·댓글의 user_id 관계는 유지하고 로그인 가능한 개인정보만 복구 불가능한 값으로 바꿉니다.
        user.anonymize(
                "deleted-" + user.getId() + "-" + UUID.randomUUID()
                        + "@invalid.local",
                // 익명화된 계정의 사용 불가능한 임의 비밀번호도 평문으로 남기지 않습니다.
                passwordEncoder.encode(UUID.randomUUID().toString())
        );
    }
}

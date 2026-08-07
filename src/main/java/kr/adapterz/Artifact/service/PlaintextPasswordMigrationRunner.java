package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.entity.User;
import kr.adapterz.Artifact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 기존 DB의 평문 비밀번호를 BCrypt로 일괄 변환하는 일회성 실행기입니다.
 * PASSWORD_MIGRATION_ENABLED=true인 경우에만 생성되며 이미 BCrypt인 값은 건너뜁니다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.password-migration",
        name = "enabled",
        havingValue = "true"
)
public class PlaintextPasswordMigrationRunner implements ApplicationRunner {
    private static final Logger log =
            LoggerFactory.getLogger(PlaintextPasswordMigrationRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<User> users = userRepository.findAll();
        long migratedCount = users.stream()
                .filter(user -> user.encodePasswordIfNeeded(passwordEncoder))
                .count();

        // 비밀번호 값은 절대 로그로 출력하지 않고 변환 개수만 기록합니다.
        log.info(
                "Plaintext password migration completed: migrated={}, skipped={}",
                migratedCount,
                users.size() - migratedCount
        );
    }
}

package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.entity.User;
import kr.adapterz.Artifact.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ExpiredAccountCleanupWorkerTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfileImageRecoveryStorageService recoveryStorageService;
    @Mock
    private AccountRecoveryPolicy recoveryPolicy;
    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @InjectMocks
    private ExpiredAccountCleanupWorker cleanupWorker;

    @Test
    void 복구기간이_끝난_사용자는_이미지를_삭제하고_개인정보를_익명화한다() {
        User user = new User(
                "user@test.com",
                passwordEncoder.encode("Password1!"),
                "테스터",
                "/uploads/profiles/profile.png"
        );
        ReflectionTestUtils.setField(user, "id", 1L);
        user.softDelete("recovery.png");
        LocalDateTime now = LocalDateTime.now();

        given(userRepository.findByIdForRecovery(1L))
                .willReturn(Optional.of(user));
        given(recoveryPolicy.isRecoverable(user, now)).willReturn(false);

        cleanupWorker.cleanup(1L, now);

        then(recoveryStorageService).should().deleteRecoveryStrict("recovery.png");
        assertTrue(user.isAnonymized());
        assertTrue(user.getEmail().startsWith("deleted-1-"));
    }
}

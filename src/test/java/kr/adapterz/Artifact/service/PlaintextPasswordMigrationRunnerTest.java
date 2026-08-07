package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.entity.User;
import kr.adapterz.Artifact.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PlaintextPasswordMigrationRunnerTest {
    @Test
    void 평문만_BCrypt로_변환하고_다시_실행해도_재해싱하지_않는다() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = spy(new BCryptPasswordEncoder());
        PlaintextPasswordMigrationRunner runner =
                new PlaintextPasswordMigrationRunner(userRepository, passwordEncoder);
        User user = new User("user@test.com", "Password1!", "테스터", null);
        given(userRepository.findAll()).willReturn(List.of(user));

        runner.run(new DefaultApplicationArguments(new String[0]));
        runner.run(new DefaultApplicationArguments(new String[0]));

        assertTrue(user.matchesPassword("Password1!", passwordEncoder));
        verify(passwordEncoder, times(1)).encode("Password1!");
    }
}

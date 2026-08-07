package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.dto.user.PasswordUpdateRequestDto;
import kr.adapterz.Artifact.dto.user.UserProfileUpdateRequestDto;
import kr.adapterz.Artifact.dto.user.UserRecoveryRequestDto;
import kr.adapterz.Artifact.dto.user.UserSignupRequestDto;
import kr.adapterz.Artifact.entity.User;
import kr.adapterz.Artifact.exception.AuthenticationException;
import kr.adapterz.Artifact.exception.InvalidInputException;
import kr.adapterz.Artifact.exception.NotFoundException;
import kr.adapterz.Artifact.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static kr.adapterz.Artifact.response.code.ErrorCode.EMAIL_PASSWORD_CHECK;
import static kr.adapterz.Artifact.response.code.ErrorCode.UNAUTHORIZED;
import static kr.adapterz.Artifact.response.code.ErrorCode.USER_NOT_FOUND;
import static kr.adapterz.Artifact.response.code.ValidationField.EMAIL;
import static kr.adapterz.Artifact.response.code.ValidationField.NICKNAME;
import static kr.adapterz.Artifact.response.code.ValidationField.PASSWORD_CHECK;
import static kr.adapterz.Artifact.response.code.ValidationField.PROFILE_IMAGE;
import static kr.adapterz.Artifact.response.code.ValidationMessage.EMAIL_DUPLICATE;
import static kr.adapterz.Artifact.response.code.ValidationMessage.NICKNAME_DUPLICATE;
import static kr.adapterz.Artifact.response.code.ValidationMessage.PASSWORD_CHECK_INVALID;
import static kr.adapterz.Artifact.response.code.ValidationMessage.PASSWORD_CHECK_NOT_SAME;
import static kr.adapterz.Artifact.response.code.ValidationMessage.PROFILE_IMAGE_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * UserService의 비즈니스 규칙만 확인하는 단위 테스트
 *
 * <p>실제 DB와 Spring 서버를 실행하지 않고, UserService가 의존하는
 * UserRepository만 Mock으로 교체합니다. 각 테스트는 다른 테스트 결과에
 * 의존하지 않도록 필요한 User와 Mock 응답을 매번 새로 준비합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    private static final String EMAIL_VALUE = "user@test.com";
    private static final String PASSWORD_VALUE = "Password1!";
    private static final String NICKNAME_VALUE = "테스터";

    // 중요: 실제 DB에 접근하지 않도록 Repository만 가짜 객체 생성
    @Mock
    private UserRepository userRepository;

    // 프로필 이미지 단위 테스트는 별도 클래스에서 수행하므로 여기서는 저장 서비스를 Mock 처리합니다.
    @Mock
    private ProfileImageStorageService profileImageStorageService;
    @Mock
    private ProfileImageRecoveryStorageService profileImageRecoveryStorageService;
    @Mock
    private AccountRecoveryPolicy accountRecoveryPolicy;
    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 중요: 테스트 대상 -> UserService는 실제 객체이며, 위 Mock이 생성자로 주입
    @InjectMocks
    private UserService userService;

    /*
     * ----------------------------------------------------------------------
     * 1. 회원가입 signup()
     * ----------------------------------------------------------------------
     */

    @Test
    void signup_중복과_비밀번호_문제가_없으면_사용자를_저장한다() {
        // Given 1: 현재 DTO에는 값 생성자가 없으므로 getter의 반환값을 Mock으로 설정
        UserSignupRequestDto request = signupRequest(
                EMAIL_VALUE,
                PASSWORD_VALUE,
                PASSWORD_VALUE,
                NICKNAME_VALUE
        );

        // Given 2: exists 메서드를 질문으로 읽으면 "이미 존재하는가?"
        // 둘 다 false이므로 이메일과 닉네임이 중복되지 않은 정상 상황.
        given(userRepository.existsByEmailIgnoreCase(EMAIL_VALUE)).willReturn(false);
        given(userRepository.existsByNickname(NICKNAME_VALUE)).willReturn(false);

        // Given 3: Mock은 실제 저장을 하지 않고 객체 반환 기본값이 null
        // -> 서비스가 save()에 전달한 첫 번째 User를 즉시 그대로 반환하게 설정함
        given(userRepository.saveAndFlush(any(User.class)))
                .willAnswer(invocation -> {
                    User userPassedToSave = invocation.getArgument(0);
                    return userPassedToSave;
                });

        // When: Mock 요청 DTO를 실제 UserService의 회원가입 메서드에 전달한다
        User result = userService.signup(request, null);

        // Then 1: 서비스가 요청값으로 User를 올바르게 생성했는지 확인
        assertEquals(EMAIL_VALUE, result.getEmail());
        assertEquals(NICKNAME_VALUE, result.getNickname());
        assertTrue(result.matchesPassword(PASSWORD_VALUE, passwordEncoder));
        assertEquals(null, result.getProfileImage());

        // Then 2: 정상 검사를 모두 통과한 뒤 저장 요청이 발생했는지 확인
        then(profileImageStorageService).should().store(null);
        // 실제 DB 저장과 ID 생성
        then(userRepository).should().saveAndFlush(any(User.class));
    }

    @Test
    void signup_프로필_이미지가_있으면_저장된_공개_경로를_사용자에게_저장한다() {
        UserSignupRequestDto request = signupRequest(
                EMAIL_VALUE,
                PASSWORD_VALUE,
                PASSWORD_VALUE,
                NICKNAME_VALUE
        );
        MultipartFile profileImage = mock(MultipartFile.class);
        String storedPath = "/uploads/profiles/test-profile.jpg";

        given(userRepository.existsByEmailIgnoreCase(EMAIL_VALUE)).willReturn(false);
        given(userRepository.existsByNickname(NICKNAME_VALUE)).willReturn(false);
        given(profileImageStorageService.store(profileImage)).willReturn(storedPath);
        given(userRepository.saveAndFlush(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        User result = userService.signup(request, profileImage);

        assertEquals(storedPath, result.getProfileImage());
        then(profileImageStorageService).should().store(profileImage);
        then(userRepository).should().saveAndFlush(any(User.class));
    }

    @Test
    void signup_DB_저장이_실패하면_먼저_저장한_프로필_이미지를_삭제한다() {
        UserSignupRequestDto request = signupRequest(
                EMAIL_VALUE,
                PASSWORD_VALUE,
                PASSWORD_VALUE,
                NICKNAME_VALUE
        );
        MultipartFile profileImage = mock(MultipartFile.class);
        String storedPath = "/uploads/profiles/orphan.jpg";

        given(userRepository.existsByEmailIgnoreCase(EMAIL_VALUE)).willReturn(false);
        given(userRepository.existsByNickname(NICKNAME_VALUE)).willReturn(false);
        given(profileImageStorageService.store(profileImage)).willReturn(storedPath);
        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(new IllegalStateException("DB 저장 실패"));

        assertThrows(
                IllegalStateException.class,
                () -> userService.signup(request, profileImage)
        );

        then(profileImageStorageService).should().delete(storedPath);
    }

    @Test
    void signup_이메일이_중복되면_예외가_발생하고_사용자를_저장하지_않는다() {
        // Given 1: 첫 이메일 검사에서 바로 중단돼야 하므로 email getter만 설정합니다.
        UserSignupRequestDto request = mock(UserSignupRequestDto.class);
        given(request.getEmail()).willReturn(EMAIL_VALUE);

        // Given 2: true는 같은 이메일이 이미 존재한다는 뜻입니다.
        given(userRepository.existsByEmailIgnoreCase(EMAIL_VALUE)).willReturn(true);

        // When + Then 1: 회원가입 실행 중 서비스 검증 예외가 발생해야 합니다.
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> userService.signup(request, null)
        );

        // Then 2: 예외 타입뿐 아니라 오류 필드와 원인까지 확인합니다.
        assertValidationError(exception, EMAIL, EMAIL_DUPLICATE);

        // Then 3: 이메일 검사에서 중단됐으므로 이후 검사와 저장은 실행되면 안 됩니다.
        then(userRepository).should(never()).existsByNickname(any(String.class));
        then(userRepository).should(never()).saveAndFlush(any(User.class));
    }

    @Test
    void signup_닉네임이_중복되면_예외가_발생하고_사용자를_저장하지_않는다() {
        // Given 1: 이메일 검사를 통과한 뒤 닉네임 검사까지 진행할 요청값입니다.
        UserSignupRequestDto request = mock(UserSignupRequestDto.class);
        given(request.getEmail()).willReturn(EMAIL_VALUE);
        given(request.getNickname()).willReturn(NICKNAME_VALUE);

        // Given 2: 이메일은 중복이 아니지만 닉네임은 이미 존재합니다.
        given(userRepository.existsByEmailIgnoreCase(EMAIL_VALUE)).willReturn(false);
        given(userRepository.existsByNickname(NICKNAME_VALUE)).willReturn(true);

        // When + Then 1: 닉네임 중복 예외를 받습니다.
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> userService.signup(request, null)
        );

        // Then 2: nickname 필드의 nickname_duplicate 오류인지 확인합니다.
        assertValidationError(exception, NICKNAME, NICKNAME_DUPLICATE);

        // Then 3: 닉네임 검사에서 중단됐으므로 저장은 실행되지 않아야 합니다.
        then(userRepository).should(never()).saveAndFlush(any(User.class));
    }

    @Test
    void signup_비밀번호_확인이_다르면_예외가_발생하고_사용자를_저장하지_않는다() {
        // Given 1: 중복 검사를 통과하지만 비밀번호 두 값이 다른 요청
        UserSignupRequestDto request = mock(UserSignupRequestDto.class);
        given(request.getEmail()).willReturn(EMAIL_VALUE);
        given(request.getNickname()).willReturn(NICKNAME_VALUE);
        given(request.getPassword()).willReturn(PASSWORD_VALUE);
        given(request.getPasswordCheck()).willReturn("Different1!");

        given(userRepository.existsByEmailIgnoreCase(EMAIL_VALUE)).willReturn(false);
        given(userRepository.existsByNickname(NICKNAME_VALUE)).willReturn(false);

        // When + Then 1: 비밀번호 확인 불일치 예외를 받습니다.
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> userService.signup(request, null)
        );

        // Then 2: password_check 필드의 정확한 오류 이유를 확인(메세지)
        assertValidationError(exception, PASSWORD_CHECK, PASSWORD_CHECK_INVALID);

        // Then 3: User를 만들고 저장하는 마지막 단계까지 도달하면 안됨
        then(userRepository).should(never()).saveAndFlush(any(User.class));
    }

    /*
     * ----------------------------------------------------------------------
     * 2. 로그인 login()
     * ----------------------------------------------------------------------
     */

    @Test
    void login_이메일과_비밀번호가_일치하는_정상_회원이면_사용자를_반환한다() {
        // Given: 이메일로 조회되는 정상 User를 실제 객체로 준비합니다.
        User user = activeUser();
        given(userRepository.findByEmailIgnoreCase(EMAIL_VALUE))
                .willReturn(Optional.of(user));

        // When: 입력 이메일과 비밀번호로 로그인합니다.
        User result = userService.login(EMAIL_VALUE, PASSWORD_VALUE);

        // Then: Repository가 준 동일한 User가 반환돼야 합니다.
        assertSame(user, result);
    }

    @Test
    void login_이메일에_해당하는_사용자가_없으면_인증_예외가_발생한다() {
        // Given: Optional.empty()는 이메일에 해당하는 User가 없다는 뜻입니다.
        given(userRepository.findByEmailIgnoreCase(EMAIL_VALUE))
                .willReturn(Optional.empty());

        // When + Then: 계정 존재 여부를 노출하지 않는 공통 로그인 오류를 확인합니다.
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> userService.login(EMAIL_VALUE, PASSWORD_VALUE)
        );

        assertSame(EMAIL_PASSWORD_CHECK, exception.getErrorCode());
    }

    @Test
    void login_비밀번호가_일치하지_않으면_인증_예외가_발생한다() {
        // Given: User는 존재하지만 입력 비밀번호가 실제 값과 다릅니다.
        User user = activeUser();
        given(userRepository.findByEmailIgnoreCase(EMAIL_VALUE))
                .willReturn(Optional.of(user));

        // When + Then: 비밀번호 오류도 동일한 로그인 오류 코드로 처리합니다.
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> userService.login(EMAIL_VALUE, "WrongPassword1!")
        );

        assertSame(EMAIL_PASSWORD_CHECK, exception.getErrorCode());
    }

    @Test
    void login_탈퇴한_회원이면_인증_예외가_발생한다() {
        // Given: 비밀번호는 일치하지만 softDelete()가 실행된 탈퇴 User입니다.
        User deletedUser = activeUser();
        deletedUser.softDelete();
        given(userRepository.findByEmailIgnoreCase(EMAIL_VALUE))
                .willReturn(Optional.of(deletedUser));

        // When + Then: 탈퇴 여부를 외부에 노출하지 않고 공통 로그인 오류를 반환합니다.
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> userService.login(EMAIL_VALUE, PASSWORD_VALUE)
        );

        assertSame(EMAIL_PASSWORD_CHECK, exception.getErrorCode());
    }

    /*
     * ----------------------------------------------------------------------
     * 3. 인증 사용자 확인 requireAuthenticated()
     * ----------------------------------------------------------------------
     */

    @Test
    void requireAuthenticated_정상_사용자이면_해당_사용자를_반환한다() {
        // Given: ID에 해당하는 정상 User가 존재합니다.
        User user = activeUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // When
        User result = userService.requireAuthenticated(1L);

        // Then
        assertSame(user, result);
    }

    @Test
    void requireAuthenticated_사용자_ID가_null이면_조회하지_않고_인증_예외가_발생한다() {
        // Given: 별도 Repository 응답이 필요 없습니다. userId 자체가 null입니다.

        // When + Then
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> userService.requireAuthenticated(null)
        );

        assertSame(UNAUTHORIZED, exception.getErrorCode());

        // null은 저장소에서 조회할 수 있는 사용자 번호가 아니므로 Repository를 호출하지 않습니다.
        then(userRepository).shouldHaveNoInteractions();
    }

    @Test
    void requireAuthenticated_사용자가_존재하지_않으면_인증_예외가_발생한다() {
        // Given: ID 값은 있지만 저장소에 User가 없습니다.
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // When + Then
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> userService.requireAuthenticated(1L)
        );

        assertSame(UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void requireAuthenticated_탈퇴한_사용자이면_인증_예외가_발생한다() {
        // Given: 저장소에는 존재하지만 탈퇴 상태인 User입니다.
        User deletedUser = activeUser();
        deletedUser.softDelete();
        given(userRepository.findById(1L)).willReturn(Optional.of(deletedUser));

        // When + Then
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> userService.requireAuthenticated(1L)
        );

        assertSame(UNAUTHORIZED, exception.getErrorCode());
    }

    /*
     * ----------------------------------------------------------------------
     * 4. 프로필 수정 updateProfile()
     * ----------------------------------------------------------------------
     */

    @Test
    void updateProfile_닉네임만_변경하고_기존_이미지는_유지한다() {
        // Given 1: 인증 확인에서 반환할 실제 User입니다.
        User user = activeUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // Given 2: 생성자가 없는 수정 DTO는 필요한 getter를 Mock으로 설정합니다.
        UserProfileUpdateRequestDto request = mock(UserProfileUpdateRequestDto.class);
        given(request.getNickname()).willReturn("새닉네임");

        // 현재 사용자를 제외한 다른 사용자가 새 닉네임을 사용하지 않는 상황입니다.
        given(userRepository.existsByNicknameAndIdNot("새닉네임", 1L))
                .willReturn(false);

        // When
        User result = userService.updateProfile(1L, request);

        // Then: Repository가 준 실제 User 객체의 상태가 변경됐는지 확인합니다.
        assertSame(user, result);
        assertEquals("새닉네임", user.getNickname());
        assertEquals("profile.png", user.getProfileImage());

        // JPA Dirty Checking을 전제로 하므로 서비스는 save()를 다시 호출하지 않습니다.
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    void updateProfile_닉네임이_중복되면_예외가_발생하고_기존_프로필을_유지한다() {
        // Given 1: 인증된 정상 User입니다.
        User user = activeUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // Given 2: 다른 사용자가 이미 사용 중인 닉네임을 요청합니다.
        UserProfileUpdateRequestDto request = mock(UserProfileUpdateRequestDto.class);
        given(request.getNickname()).willReturn("중복닉네임");
        given(userRepository.existsByNicknameAndIdNot("중복닉네임", 1L))
                .willReturn(true);

        // When + Then 1
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> userService.updateProfile(1L, request)
        );

        assertValidationError(exception, NICKNAME, NICKNAME_DUPLICATE);

        // Then 2: updateProfile()까지 도달하지 않아 기존 값이 유지돼야 합니다.
        assertEquals(NICKNAME_VALUE, user.getNickname());
        assertEquals("profile.png", user.getProfileImage());
    }

    @Test
    void updateProfile_새_이미지를_선택하면_경로를_교체하고_기존_파일을_삭제한다() {
        User user = activeUser();
        UserProfileUpdateRequestDto request = mock(UserProfileUpdateRequestDto.class);
        MultipartFile image = mock(MultipartFile.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(request.getNickname()).willReturn("새닉네임");
        given(userRepository.existsByNicknameAndIdNot("새닉네임", 1L))
                .willReturn(false);
        given(image.isEmpty()).willReturn(false);
        given(profileImageStorageService.store(image))
                .willReturn("/uploads/profiles/new.png");

        User result = userService.updateProfile(1L, request, image);

        assertEquals("/uploads/profiles/new.png", result.getProfileImage());
        then(profileImageStorageService).should().delete("profile.png");
    }

    @Test
    void updateProfile_삭제를_요청하면_경로를_null로_바꾸고_기존_파일을_삭제한다() {
        User user = activeUser();
        UserProfileUpdateRequestDto request = mock(UserProfileUpdateRequestDto.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(request.getNickname()).willReturn(NICKNAME_VALUE);
        given(request.isRemoveProfileImage()).willReturn(true);

        User result = userService.updateProfile(1L, request, null);

        assertNull(result.getProfileImage());
        then(profileImageStorageService).should().delete("profile.png");
        then(profileImageStorageService).should(never()).store(any());
    }

    @Test
    void updateProfile_새_파일과_삭제를_동시에_요청하면_저장_전에_거부한다() {
        User user = activeUser();
        UserProfileUpdateRequestDto request = mock(UserProfileUpdateRequestDto.class);
        MultipartFile image = mock(MultipartFile.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(request.getNickname()).willReturn(NICKNAME_VALUE);
        given(request.isRemoveProfileImage()).willReturn(true);
        given(image.isEmpty()).willReturn(false);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> userService.updateProfile(1L, request, image)
        );

        assertValidationError(
                exception,
                PROFILE_IMAGE,
                PROFILE_IMAGE_INVALID
        );
        then(profileImageStorageService).should(never()).store(any());
        assertEquals("profile.png", user.getProfileImage());
    }

    @Test
    void updateProfile_트랜잭션이_롤백되면_새_파일만_삭제한다() {
        User user = activeUser();
        UserProfileUpdateRequestDto request = mock(UserProfileUpdateRequestDto.class);
        MultipartFile image = mock(MultipartFile.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(request.getNickname()).willReturn(NICKNAME_VALUE);
        given(image.isEmpty()).willReturn(false);
        given(profileImageStorageService.store(image))
                .willReturn("/uploads/profiles/new.png");

        TransactionSynchronizationManager.initSynchronization();
        try {
            userService.updateProfile(1L, request, image);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization ->
                            synchronization.afterCompletion(
                                    TransactionSynchronization.STATUS_ROLLED_BACK
                            ));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        then(profileImageStorageService)
                .should()
                .delete("/uploads/profiles/new.png");
        then(profileImageStorageService)
                .should(never())
                .delete("profile.png");
    }

    /*
     * ----------------------------------------------------------------------
     * 5. 비밀번호 수정 updatePassword()
     * ----------------------------------------------------------------------
     */

    @Test
    void updatePassword_새_비밀번호와_확인값이_같으면_비밀번호를_변경한다() {
        // Given 1: 인증된 정상 User입니다.
        User user = activeUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // Given 2: 새 비밀번호와 확인값이 같은 요청입니다.
        PasswordUpdateRequestDto request = mock(PasswordUpdateRequestDto.class);
        given(request.getPassword()).willReturn("NewPassword1!");
        given(request.getPasswordCheck()).willReturn("NewPassword1!");

        // When: 반환값이 void이므로 메서드 실행 자체가 When입니다.
        userService.updatePassword(1L, request);

        // Then: 비밀번호 getter 대신 엔티티의 비교 메서드로 상태를 확인합니다.
        assertTrue(user.matchesPassword("NewPassword1!", passwordEncoder));
        assertFalse(user.matchesPassword(PASSWORD_VALUE, passwordEncoder));
    }

    @Test
    void updatePassword_새_비밀번호와_확인값이_다르면_예외가_발생하고_기존값을_유지한다() {
        // Given
        User user = activeUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        PasswordUpdateRequestDto request = mock(PasswordUpdateRequestDto.class);
        given(request.getPassword()).willReturn("NewPassword1!");
        given(request.getPasswordCheck()).willReturn("Different1!");

        // When + Then 1
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> userService.updatePassword(1L, request)
        );

        assertValidationError(exception, PASSWORD_CHECK, PASSWORD_CHECK_NOT_SAME);

        // Then 2: 예외가 발생했으므로 원래 비밀번호가 유지돼야 합니다.
        assertTrue(user.matchesPassword(PASSWORD_VALUE, passwordEncoder));
        assertFalse(user.matchesPassword("NewPassword1!", passwordEncoder));
    }

    /*
     * ----------------------------------------------------------------------
     * 6. 회원 탈퇴 delete()
     * ----------------------------------------------------------------------
     */

    @Test
    void delete_정상_사용자이면_row를_삭제하지_않고_탈퇴_상태로_변경한다() {
        // Given: 인증 확인에서 반환할 정상 User입니다.
        User user = activeUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // When
        userService.delete(1L);

        // Then 1: softDelete()가 deletedAt을 채웠으므로 true가 되어야 합니다.
        assertTrue(user.isDeleted());

        // Then 2: 소프트 삭제이므로 Repository의 물리 삭제는 호출하지 않습니다.
        then(userRepository).should(never()).delete(any(User.class));
        // 공개 프로필은 비공개 복구 파일로 복사한 뒤 트랜잭션 완료 시 제거합니다.
        then(profileImageRecoveryStorageService).should().quarantine("profile.png");
        then(profileImageRecoveryStorageService).should().deletePublic("profile.png");
    }

    @Test
    void recover_복구기간_안이고_비밀번호가_맞으면_새_프로필_경로로_복구한다() {
        User user = activeUser();
        user.softDelete("recovery-profile.png");
        UserRecoveryRequestDto request =
                new UserRecoveryRequestDto(EMAIL_VALUE, PASSWORD_VALUE);

        given(userRepository.findByEmailIgnoreCaseForRecovery(EMAIL_VALUE))
                .willReturn(Optional.of(user));
        given(accountRecoveryPolicy.isRecoverable(any(User.class), any()))
                .willReturn(true);
        given(profileImageRecoveryStorageService.restore("recovery-profile.png"))
                .willReturn("/uploads/profiles/restored.png");

        User result = userService.recover(request);

        assertFalse(result.isDeleted());
        assertEquals("/uploads/profiles/restored.png", result.getProfileImage());
        assertNull(result.getRecoveryProfileImage());
        assertEquals(2, result.getTokenVersion());
        then(profileImageRecoveryStorageService)
                .should()
                .deleteRecovery("recovery-profile.png");
    }

    @Test
    void recover_복구기간이_지났으면_같은_복구불가_예외를_반환한다() {
        User user = activeUser();
        user.softDelete("recovery-profile.png");
        UserRecoveryRequestDto request =
                new UserRecoveryRequestDto(EMAIL_VALUE, PASSWORD_VALUE);

        given(userRepository.findByEmailIgnoreCaseForRecovery(EMAIL_VALUE))
                .willReturn(Optional.of(user));
        given(accountRecoveryPolicy.isRecoverable(any(User.class), any()))
                .willReturn(false);

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> userService.recover(request)
        );

        assertSame(
                kr.adapterz.Artifact.response.code.ErrorCode.ACCOUNT_RECOVERY_UNAVAILABLE,
                exception.getErrorCode()
        );
        then(profileImageRecoveryStorageService).should(never()).restore(any());
    }

    @Test
    void delete_트랜잭션이_롤백되면_공개파일은_유지하고_복구복사본만_삭제한다() {
        User user = activeUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(profileImageRecoveryStorageService.quarantine("profile.png"))
                .willReturn("recovery.png");

        TransactionSynchronizationManager.initSynchronization();
        try {
            userService.delete(1L);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization ->
                            synchronization.afterCompletion(
                                    TransactionSynchronization.STATUS_ROLLED_BACK
                            ));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        then(profileImageRecoveryStorageService)
                .should()
                .deleteRecovery("recovery.png");
        then(profileImageRecoveryStorageService)
                .should(never())
                .deletePublic("profile.png");
    }

    /*
     * ----------------------------------------------------------------------
     * 7-1. 사용자 조회
     * ----------------------------------------------------------------------
     */

    @Test
    void findById_사용자가_존재하면_해당_사용자를_반환한다() {
        // Given: 1번 사용자 조회 결과를 Optional.of(user)로 설정합니다.
        User user = activeUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // When
        User result = userService.findById(1L);

        // Then: 서비스는 Repository가 준 동일한 User를 그대로 반환합니다.
        assertSame(user, result);
    }

    @Test
    void findById_사용자가_존재하지_않으면_USER_NOT_FOUND_예외가_발생한다() {
        // Given: Optional.empty()는 조회 결과가 없다는 뜻입니다.
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // When + Then
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userService.findById(1L)
        );

        assertSame(USER_NOT_FOUND, exception.getErrorCode());
    }

    /*
     * ----------------------------------------------------------------------
     * 7-2. 표시 닉네임
     * ----------------------------------------------------------------------
     */

    @Test
    void getDisplayInfo_정상_회원이면_닉네임과_프로필_경로를_반환한다() {
        // Given: 새 User의 deletedAt은 null이므로 정상 회원입니다.
        User user = activeUser();

        // When: 이 메서드는 Repository를 사용하지 않으므로 Stub도 필요 없습니다.
        var result = userService.getDisplayInfo(user);

        // Then
        assertEquals(NICKNAME_VALUE, result.getNickname());
        assertEquals("profile.png", result.getProfileImage());
    }

    @Test
    void getDisplayInfo_탈퇴_회원이면_닉네임을_치환하고_이미지를_숨긴다() {
        // Given: 실제 User를 탈퇴 상태로 변경합니다.
        User user = activeUser();
        user.softDelete();

        // When
        var result = userService.getDisplayInfo(user);

        // Then
        assertEquals(UserService.DELETED_USER_NICKNAME, result.getNickname());
        assertNull(result.getProfileImage());
    }

    @Test
    void findDisplayInfoMapByIds_작성자_정보를_한번에_반환한다() {
        // Given 1: 단위 테스트에는 DB가 없으므로 테스트용 ID를 직접 넣습니다.
        User activeUser = userWithId(1L, "활동회원");
        User deletedUser = userWithId(2L, "탈퇴회원");
        deletedUser.softDelete();

        Set<Long> userIds = Set.of(1L, 2L);
        given(userRepository.findAllById(userIds))
                .willReturn(List.of(activeUser, deletedUser));

        // When
        var result = userService.findDisplayInfoMapByIds(userIds);

        // Then: 정상 회원은 원래 이름, 탈퇴 회원은 "알 수 없음"으로 변환됩니다.
        assertEquals(2, result.size());
        assertEquals("활동회원", result.get(1L).getNickname());
        assertEquals("profile.png", result.get(1L).getProfileImage());
        assertEquals(UserService.DELETED_USER_NICKNAME, result.get(2L).getNickname());
        assertNull(result.get(2L).getProfileImage());
    }

    /*
     * ----------------------------------------------------------------------
     * 8. 테스트 데이터와 반복 검증을 위한 보조 메서드
     * ----------------------------------------------------------------------
     */

    /**
     * 회원가입 요청 DTO에 값 생성자가 없으므로, 각 getter가 테스트값을 반환하는
     * Mock DTO를 만들어 반복 코드를 줄입니다.
     */
    private UserSignupRequestDto signupRequest(
            String email,
            String password,
            String passwordCheck,
            String nickname
    ) {
        UserSignupRequestDto request = mock(UserSignupRequestDto.class);
        given(request.getEmail()).willReturn(email);
        given(request.getPassword()).willReturn(password);
        given(request.getPasswordCheck()).willReturn(passwordCheck);
        given(request.getNickname()).willReturn(nickname);
        return request;
    }

    /** 각 테스트가 독립적으로 사용할 정상 User를 새로 생성합니다. */
    private User activeUser() {
        return new User(
                EMAIL_VALUE,
                passwordEncoder.encode(PASSWORD_VALUE),
                NICKNAME_VALUE,
                "profile.png"
        );
    }

    /**
     * 실제 DB가 생성하는 ID가 필요한 Map 테스트를 위한 User입니다.
     * ReflectionTestUtils는 운영 코드가 아니라 테스트에서만 사용합니다.
     */
    private User userWithId(Long id, String nickname) {
        User user = new User(
                id + "@test.com",
                passwordEncoder.encode(PASSWORD_VALUE),
                nickname,
                "profile.png"
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    /** InvalidInputException의 공통 구조인 오류 1개, 필드, 이유를 함께 확인합니다. */
    private void assertValidationError(
            InvalidInputException exception,
            String expectedField,
            String expectedReason
    ) {
        assertEquals(1, exception.getErrors().size());
        assertEquals(expectedField, exception.getErrors().get(0).getField());
        assertEquals(expectedReason, exception.getErrors().get(0).getReason());
    }
}

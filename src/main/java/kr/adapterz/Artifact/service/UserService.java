package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.dto.user.PasswordUpdateRequestDto;
import kr.adapterz.Artifact.dto.user.UserProfileUpdateRequestDto;
import kr.adapterz.Artifact.dto.user.UserRecoveryRequestDto;
import kr.adapterz.Artifact.dto.user.UserDisplayInfoDto;
import kr.adapterz.Artifact.dto.user.UserSignupRequestDto;
import kr.adapterz.Artifact.entity.User;
import kr.adapterz.Artifact.exception.AuthenticationException;
import kr.adapterz.Artifact.exception.InvalidInputException;
import kr.adapterz.Artifact.exception.NotFoundException;
import kr.adapterz.Artifact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static kr.adapterz.Artifact.response.code.ErrorCode.*;
import static kr.adapterz.Artifact.response.code.ValidationField.*;
import static kr.adapterz.Artifact.response.code.ValidationMessage.*;

//@Transactional(readOnly = true)로 조회를 기본으로 두고 CURD 메소드부분만 @Transactional을 붙이는게 깔끔하다
//@RequiredArgsConstructor -> Lombok 라이브러리에서 제공하는 어노테이션, 필수인자를 가진 생성자를 자동을 생성
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    public static final String DELETED_USER_NICKNAME = "알 수 없음";

    //의존성 주입
    private final UserRepository userRepository;
    private final ProfileImageStorageService profileImageStorageService;
    private final ProfileImageRecoveryStorageService profileImageRecoveryStorageService;
    private final AccountRecoveryPolicy accountRecoveryPolicy;
    private final PasswordEncoder passwordEncoder;

    //회원가입
    @Transactional
    public User signup(UserSignupRequestDto request, MultipartFile profileImage) {
        // 1. 어노테이션만으로 확인하기 어려운 중복값을 검사
        //이메일 중복확인-> 참이 오면 중복임으로 에러 메세지
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new InvalidInputException(EMAIL, EMAIL_DUPLICATE);
        }
        //닉네임 중복
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new InvalidInputException(NICKNAME, NICKNAME_DUPLICATE);
        }
        // 2. 비밀번호와 비밀번호확인 검사
        if (!request.getPassword().equals(request.getPasswordCheck())) {
            throw new InvalidInputException(PASSWORD_CHECK, PASSWORD_CHECK_INVALID);
        }
        // 3. 입력값 검사를 모두 통과한 뒤에만 이미지 파일을 저장합니다.
        // 중복 회원 요청이 불필요한 이미지 파일을 만들지 않도록 검증보다 뒤에 둡니다.
        String profileImagePath = profileImageStorageService.store(profileImage);

        try {
            // DB에는 이미지 파일 자체가 아니라 브라우저가 접근할 공개 URL 경로만 저장합니다.
            // flush까지 수행해 DB 제약 조건 오류를 이 범위 안에서 확인할 수 있게 합니다.
            return userRepository.saveAndFlush(new User(
                    request.getEmail(),
                    // 중요: DB에는 사용자가 입력한 평문이 아니라 BCrypt 해시만 저장합니다.
                    passwordEncoder.encode(request.getPassword()),
                    request.getNickname(),
                    profileImagePath
            ));
        } catch (RuntimeException e) {
            // DB 저장이 실패하면 먼저 저장된 이미지도 제거하여 고아 파일이 남지 않게 합니다.
            profileImageStorageService.delete(profileImagePath);
            throw e;
        }
    }

    //유저찾기
    public User findById(Long userId) {
        //사용자ID로 찾고 NULL일때 (.orElseThrow) 예외
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
    }

    // 여러 작성자의 닉네임과 프로필 경로를 한 번에 조회하여 N+1 문제를 방지합니다.
    public Map<Long, UserDisplayInfoDto> findDisplayInfoMapByIds(Set<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);

        return users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        this::getDisplayInfo
                ));
    }

    // 탈퇴 회원은 원래 닉네임과 이미지를 노출하지 않고 공통 표시값으로 바꿉니다.
    public UserDisplayInfoDto getDisplayInfo(User user) {
        if (user.isDeleted()) {
            return new UserDisplayInfoDto(DELETED_USER_NICKNAME, null);
        }
        return new UserDisplayInfoDto(user.getNickname(), user.getProfileImage());
    }

    //인증 부분
    public User requireAuthenticated(Long userId) {
        // Controller가 JWT에서 꺼낸 사용자 ID를 전달하며, 사용자 상태도 다시 확인합니다.
        if (userId == null) {
            throw new AuthenticationException(UNAUTHORIZED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException(UNAUTHORIZED));

        if (user.isDeleted()) {
            throw new AuthenticationException(UNAUTHORIZED); //탈퇴한 유저 막기
        }

        return user;
    }

    //로그인
    public User login(String email, String password) {
        // 이메일과 비밀번호 중 하나라도 일치하지 않으면 같은 401 응답을 보냅니다.
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthenticationException(EMAIL_PASSWORD_CHECK));

        if (!user.matchesPassword(password, passwordEncoder)) {
            throw new AuthenticationException(EMAIL_PASSWORD_CHECK);
        }
        if (user.isDeleted()) {
            throw new AuthenticationException(EMAIL_PASSWORD_CHECK);
        }
        return user;
    }

    /**
     * 이메일과 기존 비밀번호로 30일 이내 탈퇴 계정을 복구합니다.
     * 존재 여부·비밀번호·만료 원인을 외부에 구분하지 않아 계정 탐색을 방지합니다.
     */
    @Transactional
    public User recover(UserRecoveryRequestDto request) {
        // 복구와 만료 익명화가 동시에 실행되지 않도록 이메일로 사용자 행을 잠가 조회합니다.
        User user = userRepository
                .findByEmailIgnoreCaseForRecovery(request.getEmail())
                .orElseThrow(() ->
                        new AuthenticationException(ACCOUNT_RECOVERY_UNAVAILABLE));

        // 실패 원인을 하나로 합쳐 이메일 존재 여부나 탈퇴 상태가 외부에 노출되지 않게 합니다.
        if (!user.matchesPassword(request.getPassword(), passwordEncoder)
                || !accountRecoveryPolicy.isRecoverable(user, LocalDateTime.now())) {
            throw new AuthenticationException(ACCOUNT_RECOVERY_UNAVAILABLE);
        }

        String recoveryKey = user.getRecoveryProfileImage();
        // 복구 파일이 실제로 없으면 null이 반환되며 계정은 기본 프로필로 정상 복구됩니다.
        String restoredPublicPath =
                profileImageRecoveryStorageService.restore(recoveryKey);

        // 엔티티 상태를 활성으로 바꾸고 tokenVersion을 올려 탈퇴 전 JWT를 무효화합니다.
        user.restore(restoredPublicPath);
        registerRecoveryCompletion(recoveryKey, restoredPublicPath);
        return user;
    }

    //프로필수정
    @Transactional
    public User updateProfile(Long userId, UserProfileUpdateRequestDto request) {
        // 기존 JSON 닉네임 수정 요청은 이미지 파일이 없는 멀티파트 규칙과 동일하게 처리합니다.
        return updateProfile(userId, request, null);
    }

    /**
     * 닉네임과 선택 프로필 이미지를 함께 수정합니다.
     * 새 파일 없음·새 파일 교체·기존 이미지 삭제의 세 상태를 명시적으로 구분합니다.
     */
    @Transactional
    public User updateProfile(
            Long userId,
            UserProfileUpdateRequestDto request,
            MultipartFile profileImage
    ) {
        // 1. 로그인한 사용자인지 확인합니다.
        User user = requireAuthenticated(userId);

        // 2. 다른 사용자가 이미 사용 중인 닉네임인지 확인합니다.
        if (userRepository.existsByNicknameAndIdNot(request.getNickname(), userId)) {
            throw new InvalidInputException(NICKNAME, NICKNAME_DUPLICATE);
        }

        boolean hasNewImage =
                profileImage != null && !profileImage.isEmpty();
        if (hasNewImage && request.isRemoveProfileImage()) {
            // 교체와 삭제를 동시에 요청하면 최종 상태가 모호하므로 파일 저장 전에 거부합니다.
            throw new InvalidInputException(
                    PROFILE_IMAGE,
                    PROFILE_IMAGE_INVALID
            );
        }

        String previousImagePath = user.getProfileImage();
        String newImagePath = null;

        try {
            // 중복 닉네임 검사를 통과한 뒤에만 새 파일을 저장해 불필요한 고아 파일을 줄입니다.
            if (hasNewImage) {
                newImagePath =
                        profileImageStorageService.store(profileImage);
            }

            user.updateNickname(request.getNickname());

            boolean imageChanged =
                    hasNewImage || request.isRemoveProfileImage();
            if (imageChanged) {
                user.updateProfileImage(
                        hasNewImage ? newImagePath : null
                );
                registerProfileUpdateCompletion(
                        previousImagePath,
                        newImagePath
                );
            }
            return user;
        } catch (RuntimeException e) {
            // 트랜잭션 완료 콜백 등록 전에 실패한 경우 이번 요청이 만든 파일을 즉시 제거합니다.
            profileImageStorageService.delete(newImagePath);
            throw e;
        }
    }

    //pw 변경
    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequestDto request) {
        // 1. 로그인 확인
        User user = requireAuthenticated(userId);

        // 2. 새 비밀번호와 확인 값 비교
        if (!request.getPassword().equals(request.getPasswordCheck())) {
            throw new InvalidInputException(PASSWORD_CHECK, PASSWORD_CHECK_NOT_SAME);
        }
        // 3. 비밀번호 변경
        // 중요: 비밀번호 변경에서도 평문이 DB에 들어가지 않도록 먼저 해시합니다.
        user.updatePassword(passwordEncoder.encode(request.getPassword()));
    }

    //사용자 삭제 -> 실제 row를 삭제하지 않고 탈퇴 시각만 기록하는 소프트 삭제 방식
    @Transactional
    public void delete(Long userId) {
        // 1. 로그인한 사용자
        User user = requireAuthenticated(userId);
        // 엔티티에서 경로를 null로 바꾸기 전에 커밋 후 삭제할 기존 URL을 보관합니다.
        String previousPublicPath = user.getProfileImage();

        // 2. 공개 파일을 먼저 비공개 폴더에 복사합니다. 실패하면 사용자는 활성 상태로 유지됩니다.
        String recoveryKey =
                profileImageRecoveryStorageService.quarantine(previousPublicPath);

        // 3. 사용자는 DB에 남기고 공개 이미지 연결과 기존 JWT를 비활성화합니다.
        user.softDelete(recoveryKey);
        registerDeleteCompletion(previousPublicPath, recoveryKey);

    }

    /**
     * 탈퇴 DB 커밋 성공 시 공개 파일을 제거하고, 롤백 시 만들어 둔 복구 파일을 제거합니다.
     */
    private void registerDeleteCompletion(
            String previousPublicPath,
            String recoveryKey
    ) {
        registerAfterCompletion(
                () -> profileImageRecoveryStorageService.deletePublic(previousPublicPath),
                () -> profileImageRecoveryStorageService.deleteRecovery(recoveryKey)
        );
    }

    /**
     * 복구 DB 커밋 성공 시 비공개 파일을 제거하고, 롤백 시 새 공개 파일을 제거합니다.
     */
    private void registerRecoveryCompletion(
            String recoveryKey,
            String restoredPublicPath
    ) {
        registerAfterCompletion(
                () -> profileImageRecoveryStorageService.deleteRecovery(recoveryKey),
                () -> profileImageRecoveryStorageService.deletePublic(restoredPublicPath)
        );
    }

    /**
     * 프로필 교체·삭제 커밋 시 이전 파일을 지우고, 롤백 시 이번 요청의 새 파일만 지웁니다.
     */
    private void registerProfileUpdateCompletion(
            String previousImagePath,
            String newImagePath
    ) {
        registerAfterCompletion(
                () -> profileImageStorageService.delete(previousImagePath),
                () -> profileImageStorageService.delete(newImagePath)
        );
    }

    private void registerAfterCompletion(
            Runnable afterCommit,
            Runnable afterRollback
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 순수 단위 테스트처럼 트랜잭션이 없으면 메서드가 끝난 시점을 커밋으로 간주합니다.
            afterCommit.run();
            return;
        }

        // 실제 파일 삭제는 DB 결과가 확정된 afterCompletion 시점까지 미룹니다.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) {
                            afterCommit.run();
                        } else {
                            afterRollback.run();
                        }
                    }
                }
        );
    }

}

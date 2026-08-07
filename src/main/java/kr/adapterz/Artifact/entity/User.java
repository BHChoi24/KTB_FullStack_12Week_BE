package kr.adapterz.Artifact.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

// 데이터베이스 테이블과 1:1로 매핑되어 데이터를 객체 형태로 표현하는 자바 클래스
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                // 중요: 동시 회원가입 요청에서도 같은 이메일이 중복 저장되지 않게 DB가 보장합니다.
                @UniqueConstraint(
                        name = "uk_users_email",
                        columnNames = "email"
                )
        },
        indexes = {
                @Index(
                        name = "idx_users_nickname",
                        columnList = "nickname"
                )
        }
)
@Getter // 자동으로 .get메서드
public class User extends BaseTimeEntity {
    private static final Pattern BCRYPT_PATTERN = Pattern.compile(
            "^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$"
    );
    //@Id를 사용하는 이유는 엔티티를 고유하게 구분해 JPA가 저장, 수정, 삭제와 같은 데이터 처리를 정확하게 할 수 있도록 하기 위해서
    //@GeneratedValue 기본 키 값을 자동으로 생성하도록 JPA에 지시하는 어노테이션
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)//자동으로 증가 -> 생정자에 id따로 넣을필요가 없어짐
    @Column(name = "user_id")
    private Long id;              // 회원의 고유 식별 번호 (저장소에서 자동 발급)
    @Column(nullable = false)
    private String email;         // 로그인 시 식별자로 사용하는 이메일 주소
    @Column(nullable = false)
    @Getter(AccessLevel.NONE)
    private String password;      // 인증을 위한 BCrypt 해시(평문 저장 금지)
    @Column(nullable = false, length = 10)
    private String nickname;
    @Column(name = "profile_image", length = 500)// 게시글이나 댓글 작성 시 노출되는 사용자 닉네임
    private String profileImage;  // 사용자 이미지
    @Column(name = "recovery_profile_image", length = 500)
    private String recoveryProfileImage; // 공개되지 않는 탈퇴 복구 폴더의 파일 키

    @Column(
            name = "token_version",
            nullable = false,
            columnDefinition = "INTEGER DEFAULT 0"
    )
    private int tokenVersion = 0; // 탈퇴·복구 전 JWT를 무효화하는 사용자별 버전

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER; // 회원가입시 기본으로 유저역할

    // 탈퇴한 회원 정보 표시
    @Column(name = "deleted_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime deletedAt;
    @Column(name = "anonymized_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime anonymizedAt;

    // @OneToMany을 통해 양방향, mappedBy로 읽기권한만 주기
    // 예제를 따라했는데 나중에 확인해보기
    // @OneToMany(mappedBy = "user") //양뱡향인데 좀 더 조사해보기
    // private List<Post> posts = new ArrayList<>();


    // JPA가 사용할 생성자. DB 조회 결과로 객체를 만들기 위해 필요한 생성자, User 기본값 오버로딩
    protected User(){
    }

    // 회원가입용 생성자
    // 기존에 id를 null로 받았는데 부여를 DB가 하니까 빼주기
    public User(String email, String encodedPassword, String nickname, String profileImage) {
        this.email = email;
        this.password = encodedPassword;
        this.nickname = nickname;
        this.profileImage = profileImage;
    }

    // 닉네임 수정 API가 프로필 이미지 경로까지 실수로 덮어쓰지 않도록 책임을 분리합니다.
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 서버가 검증·저장한 공개 경로만 프로필에 연결합니다.
     * null을 전달하면 사용자가 기본 프로필 이미지를 사용한다는 의미입니다.
     */
    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    //비밀번호 수정
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /** 입력한 평문과 DB의 BCrypt 해시가 같은 비밀번호인지 확인합니다. */
    public boolean matchesPassword(String rawPassword, PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(rawPassword, this.password);
    }

    /** 일괄 변환 시 평문만 해시하고 이미 변환된 값은 다시 해시하지 않습니다. */
    public boolean encodePasswordIfNeeded(PasswordEncoder passwordEncoder) {
        if (BCRYPT_PATTERN.matcher(this.password).matches()) {
            return false;
        }
        this.password = passwordEncoder.encode(this.password);
        return true;
    }

    /**
     * 회원 행과 게시글 관계는 유지하되 공개 프로필 연결을 제거하고 복구 상태로 전환합니다.
     */
    public void softDelete(String recoveryProfileImage) {
        this.deletedAt = LocalDateTime.now();
        this.profileImage = null;
        this.recoveryProfileImage = recoveryProfileImage;
        this.tokenVersion++;
    }

    /** 이미지가 없는 사용자와 기존 테스트가 사용할 수 있는 소프트 삭제 진입점입니다. */
    public void softDelete() {
        softDelete(null);
    }

    /** 30일 안에 계정을 복구하고 새 공개 프로필 경로를 연결합니다. */
    public void restore(String restoredProfileImage) {
        this.deletedAt = null;
        this.anonymizedAt = null;
        this.profileImage = restoredProfileImage;
        this.recoveryProfileImage = null;
        this.tokenVersion++;
    }

    /**
     * 복구 기간이 끝난 사용자의 개인정보를 제거하되 게시글·댓글 외래키는 유지합니다.
     */
    public void anonymize(String anonymousEmail, String unusablePassword) {
        this.email = anonymousEmail;
        this.password = unusablePassword;
        this.nickname = "탈퇴회원";
        this.profileImage = null;
        this.recoveryProfileImage = null;
        this.anonymizedAt = LocalDateTime.now();
        this.tokenVersion++;
    }

    //삭제 판단
    public boolean isDeleted() {
        return this.deletedAt != null;
        /*
         * deleted_at == null
         * -> 정상 회원
         * deleted_at != null
         * -> 탈퇴 회원
         */
    }

    /** 만료 정리가 끝난 계정은 더 이상 복구할 수 없습니다. */
    public boolean isAnonymized() {
        return this.anonymizedAt != null;
    }
}

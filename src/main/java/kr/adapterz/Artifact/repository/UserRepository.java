package kr.adapterz.Artifact.repository;


import kr.adapterz.Artifact.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Optional;



//JPA Repository 만들기 인터페이스로 만들고 상속<엔티티 타입, 키 타입>
//인터페이스가 됬으니까 기존에 있던 본문지우고 CURD메소드는 지원해주니까 삭제, public는 기본값이라 색략해도된다
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    //List<User> findAllById(Set<Long> userIds);

    //이메일 찾기findByEmail, 대소문자 구문없이 하기위해 IgnoreCase을 붙여서 JPA에게 알려줌
    //lower(u.email) = lower(:email) -> 이메일을 비교할 때 대소문자를 무시하려고 양쪽을 전부 소문자로 바꾼 뒤 비교
    Optional<User> findByEmailIgnoreCase(String email);

    //존재하는 이메일 확인, 대소문자 구문없이 하기위해 IgnoreCase을 붙여서 JPA에게 알려줨
    boolean existsByEmailIgnoreCase(String email);


    //존재하는 닉네임
    boolean existsByNickname(String nickname);


    //닉네임 중복확인
    //기존의 이름이 JPA이름 규칙에 안맞는다고함 existsByNicknameExceptUser -> existsByNicknameAndIdNot
    // u.id <> :userId -> 현재 로그인한 내 id는 제외하고 닉네임 중복을 검사
    boolean existsByNicknameAndIdNot(String nickname, Long userId);

    /**
     * 복구와 만료 정리가 동시에 같은 사용자를 변경하지 않도록 행 쓰기 잠금을 획득합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmailIgnoreCaseForRecovery(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :userId")
    Optional<User> findByIdForRecovery(@Param("userId") Long userId);

    /** 복구 기간이 끝났지만 아직 익명화되지 않은 계정을 오래된 순으로 최대 100개 조회합니다. */
    List<User> findTop100ByDeletedAtBeforeAndAnonymizedAtIsNullOrderByDeletedAtAsc(
            LocalDateTime cutoff
    );

    /** 고아 파일 정리 작업이 현재 DB에서 실제 사용 중인 공개 프로필 경로만 확인하게 합니다. */
    @Query("select u.profileImage from User u where u.profileImage is not null")
    Set<String> findAllReferencedProfileImages();
}

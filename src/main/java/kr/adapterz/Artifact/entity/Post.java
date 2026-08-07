package kr.adapterz.Artifact.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(
                        name = "idx_posts_created_at_post_id",
                        columnList = "created_at, post_id"
                ),
                @Index(
                        name = "idx_posts_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_posts_category_created_at_post_id",
                        columnList = "category, created_at, post_id"
                ),
                @Index(
                        name = "idx_posts_pinned_pinned_at",
                        columnList = "pinned, pinned_at"
                )
        }
)
@Getter
public class Post extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)// 자동증가
    @Column(name = "post_id")
    private Long id;             // 게시글의 고유 식별 번호

    //외래키 매핑 다대일
    //적으면서 복습->다대일 관계에서 외래키는 N쪽, 외래키 가진쪽이 연관관계 주인
    // 게시글을 작성한 회원의 고유 식별자 (작성자 검증 시 활용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    //이전 private Long userId;->객체지향적이지 않은 방식 ,수정하면서 아래 생성자도 수정하기

    @Column(nullable = false, length = 26)
    private String title;

    // ERD에서 타입 TEXT사용, columnDefinition = "TEXT"
    @Column(nullable = false, columnDefinition = "TEXT")   // 게시글 내용
    private String content;

    // EnumType.STRING은 enum 순서가 바뀌어도 DB 값의 의미가 변하지 않도록 이름 자체를 저장합니다.
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30,
            columnDefinition = "VARCHAR(30) DEFAULT 'DAILY_SHARE'"
    )
    private PostCategory category = PostCategory.DAILY_SHARE;

    @Column(
            nullable = false,
            columnDefinition = "BOOLEAN DEFAULT FALSE"
    )
    private boolean pinned = false;

    @Column(name = "pinned_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime pinnedAt;

    @Column( length = 500)
    private String imageUrl;    // 게시글 첨부 이미지 URL (선택 사항)

    // 중요: 집계값은 NULL을 허용하지 않고 DB 직접 입력에서도 0을 기본값으로 사용합니다.
    @Column(name = "likes_count", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long likesCount;

    @Column(name = "views_count", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long viewsCount;      // 게시글 조회수 카운터

    //게시글에 들어가야할 것들
    public Post(
            User user,
            String title,
            String content,
            String imageUrl,
            PostCategory category
    ) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.category = Objects.requireNonNull(category);
        this.likesCount = 0L;
        this.viewsCount = 0L; // 초기 생성 시 조회수는 0으로 기본 설정
    }

    /** 기존 테스트와 기존 코드 경로는 일상공유 글로 해석합니다. */
    public Post(User user, String title, String content, String imageUrl) {
        this(user, title, content, imageUrl, PostCategory.DAILY_SHARE);
    }
    //JPA 사용객체
    protected Post() {}

    //게시물 작성
    //Post.user.getId 짧게 만들기 -> 게시글의 유저id
    public Long getUserId() {
        return user.getId();
    }

    //게시글 조회할때마다 1증가
    public void increaseViewsCount() {
        this.viewsCount++;
    }

    // 좋아요가 성공적으로 저장됐을 때만 호출합니다.
    public void increaseLikesCount() {
        this.likesCount++;
    }

    // 좋아요 취소가 성공했을 때만 호출합니다.
    public void decreaseLikesCount() {
        if (this.likesCount > 0) {
            this.likesCount--;
        }
    }

    // 게시글 텍스트 수정: 이미지 변경과 분리하여 null의 의미가 모호해지지 않게 합니다.
    public void updateText(String title, String content) {
        // PATCH는 전달된 값만 변경, null인 필드는 기존 값을 유지하기
        if (title != null) this.title = title;
        if (content != null) this.content = content;
    }

    /**
     * 카테고리를 공지사항에서 일반 게시판으로 바꾸면 고정 상태도 함께 해제합니다.
     * 이를 엔티티에서 보장해야 어떤 Service 경로에서도 "일반 글이 고정됨" 상태가 남지 않습니다.
     */
    public void updateCategory(PostCategory category) {
        this.category = Objects.requireNonNull(category);
        if (category != PostCategory.NOTICE && pinned) {
            updatePinned(false);
        }
    }

    /** 공지사항만 상단에 고정할 수 있으며, 고정 시각은 표시 순서를 결정합니다. */
    public void updatePinned(boolean pinned) {
        if (pinned && category != PostCategory.NOTICE) {
            throw new IllegalStateException("Only notice posts can be pinned");
        }
        this.pinned = pinned;
        this.pinnedAt = pinned ? LocalDateTime.now() : null;
    }

    // 서버가 파일을 저장한 뒤 반환한 공개 경로만 반영합니다. null이면 이미지가 제거됩니다.
    public void updateImage(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}

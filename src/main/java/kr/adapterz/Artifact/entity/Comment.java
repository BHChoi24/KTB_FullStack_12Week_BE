package kr.adapterz.Artifact.entity;

import jakarta.persistence.*;
import lombok.Getter;

/**
 * [클래스 역할] 댓글(Comment)의 원본 데이터와 데이터 변경 규칙을 관리하는 도메인 모델입니다.
 */
@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(
                        name = "idx_comments_post_id",
                        columnList = "post_id"
                ),
                @Index(
                        name = "idx_comments_user_id",
                        columnList = "user_id"
                )
        }
)
@Getter
public class Comment extends BaseTimeEntity {

    // 댓글의 고유 식별 번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    //다대일, 외래키
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;      // 댓글이 작성된 대상 게시글의 고유 식별자 (외래키 역할)

    //다대일, 외래키
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;      // 댓글을 작성한 회원의 고유 식별자 (수정/삭제 권한 검증 시 활용)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;   // 댓글 내용 문자열

    // 댓글 생성자, 생성/수정 시각은 BaseTimeEntity가 자동으로 넣어줍니다.
    public Comment(Post post, User user, String content) {
        this.post = post;
        this.user = user;
        this.content = content;
    }
    //JPA가 사용할 생성자
    protected Comment(){}

    // 댓글수정
    public void update(String content) {
        this.content = content;
    }


    //댓글에서 유저,게시글의 id를 찾을때
    public Long getUserId() {
        return user.getId();
    }
    public Long getPostId() {
        return post.getId();
    }

}

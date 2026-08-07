package kr.adapterz.Artifact.entity;

import jakarta.persistence.*;
import lombok.Getter;


//게시글 좋아요 엔티티
@Entity
@Table(
        name = "post_likes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_likes_user_post",
                        columnNames = {"user_id", "post_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_post_likes_post_id",
                        columnList = "post_id"
                )
        }
)
// 좋아요 식별, 좋아요누른 유저, 좋아요 누른 게시글
@Getter
public class PostLike extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    //JPA 사용
    protected PostLike() {
    }
    public PostLike(User user, Post post) {
        this.user = user;
        this.post = post;
    }


}

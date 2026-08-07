package kr.adapterz.Artifact.dto.post;

import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.entity.PostCategory;

import java.time.LocalDateTime;

/** 목록 화면에 필요한 값만 모은 응답 DTO. */
public class PostListResponseDto {
    private final Long postId;
    private final String title;
    private final String nickname;
    // 게시글 목록 카드에서 작성자의 원형 프로필 이미지를 표시할 공개 경로입니다.
    private final String profileImage;
    private final LocalDateTime createdAt;
    private final Long likesCount;
    private final Long commentsCount;
    private final Long viewsCount;
    private final PostCategory category;
    private final boolean pinned;

    private PostListResponseDto(Post post, String nickname, String profileImage, Long commentsCount) {
        this.postId = post.getId();
        this.title = post.getTitle();
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.createdAt = post.getCreatedAt();
        this.likesCount = Long.valueOf(post.getLikesCount());
        this.commentsCount = commentsCount;
        this.viewsCount = Long.valueOf(post.getViewsCount());
        this.category = post.getCategory();
        this.pinned = post.isPinned();
    }

    public static PostListResponseDto from(
            Post post,
            String nickname,
            String profileImage,
            long commentsCount
    ) {
        // Post에는 사용자 번호만 있으므로 UserService에서 조회한 닉네임과 이미지 경로를 함께 받습니다.
        return new PostListResponseDto(post, nickname, profileImage, commentsCount);
    }

    public Long getPostId() { return postId; }
    public String getTitle() { return title; }
    public String getNickname() { return nickname; }
    public String getProfileImage() { return profileImage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getLikesCount() { return likesCount; }
    public Long getCommentsCount() { return commentsCount; }
    public Long getViewsCount() { return viewsCount; }
    public PostCategory getCategory() { return category; }
    public boolean isPinned() { return pinned; }
}

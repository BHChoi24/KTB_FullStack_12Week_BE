package kr.adapterz.Artifact.dto.post;
import kr.adapterz.Artifact.dto.comment.CommentResponseDto;
import kr.adapterz.Artifact.entity.PostCategory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/** 게시글 본문과 댓글을 한 번에 내려주는 상세 응답 DTO입니다. */
@Getter
public class PostDetailResponseDto {
    private final Long postId;
    private final Long userId;
    private final String title;
    private final String content;
    private final String nickname;
    // 게시글 본문 이미지(imageUrl)와 구분되는 "작성자 프로필 이미지" 경로입니다.
    private final String profileImage;
    private final LocalDateTime createdAt;
    private final String imageUrl;
    private final Long likesCount;
    private final Long commentsCount;
    private final Long viewsCount;
    private final boolean liked;
    private final PostCategory category;
    private final boolean pinned;
    private final List<CommentResponseDto> comments;

    @Builder
    private PostDetailResponseDto(Long postId, Long userId, String title, String content, String nickname, String profileImage, LocalDateTime createdAt, String imageUrl, Long likesCount, Long commentsCount, Long viewsCount, boolean liked, PostCategory category, boolean pinned, List<CommentResponseDto> comments) {
        // PostQueryService가 게시글, 작성자, 댓글, 좋아요 정보를 조립한 결과를 저장합니다.
        this.postId = postId;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.createdAt = createdAt;
        this.imageUrl = imageUrl;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.viewsCount = viewsCount;
        this.liked = liked;
        this.category = category;
        this.pinned = pinned;
        this.comments = comments;
    }

    // 기존에 있던거 혹시몰라서 남김
//    public PostDetailResponseDto(Post post, String nickname, List<CommentResponseDto> comments) {
//        this.postId = post.getId();
//        this.title = post.getTitle();
//        this.content = post.getContent();
//        //this.userId = post.getUserId();
//        this.nickname = nickname;
//        this.createdAt = post.getCreatedAt();
//        this.imageUrl = post.getImageUrl();
//        this.likesCount = Long.valueOf(post.getLikesCount());
//        this.commentsCount = comments.size();
//        this.viewsCount = Long.valueOf(post.getViewsCount());
//        this.comments = comments;
//    }

}

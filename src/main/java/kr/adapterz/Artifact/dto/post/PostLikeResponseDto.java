package kr.adapterz.Artifact.dto.post;

import kr.adapterz.Artifact.entity.Post;

public class PostLikeResponseDto {
    private final Long postId;
    private final Long likesCount;
    private final boolean liked;

    public PostLikeResponseDto(Post post, boolean liked) {
        this.postId = post.getId();
        this.likesCount = post.getLikesCount();
        this.liked = liked;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getLikesCount() {
        return likesCount;
    }

    public boolean isLiked() {
        return liked;
    }
}

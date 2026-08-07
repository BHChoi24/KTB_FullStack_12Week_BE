package kr.adapterz.Artifact.dto.post;

import kr.adapterz.Artifact.entity.Post;

/** 수정이 완료된 게시글의 ID를 반환합니다. */
public class PostUpdateResponseDto {
    private Long postId;

    public PostUpdateResponseDto(Post post) {
        this.postId = post.getId();
    }

    public Long getPostId() { return postId; }
}

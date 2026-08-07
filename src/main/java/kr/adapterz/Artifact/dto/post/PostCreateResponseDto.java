package kr.adapterz.Artifact.dto.post;


import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.entity.PostCategory;

/** 새로 작성된 게시글 정보를 반환합니다. */
public class PostCreateResponseDto {
    private Long postId;
    private String title;
    private String content;
    private String imageUrl;
    private PostCategory category;
    private boolean pinned;
    private java.time.LocalDateTime createdAt;

    public PostCreateResponseDto(Post post) {
        this.postId = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.imageUrl = post.getImageUrl();
        this.category = post.getCategory();
        this.pinned = post.isPinned();
        this.createdAt = post.getCreatedAt();
    }

    public Long getPostId() { return postId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getImageUrl() { return imageUrl; }
    public PostCategory getCategory() { return category; }
    public boolean isPinned() { return pinned; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
}

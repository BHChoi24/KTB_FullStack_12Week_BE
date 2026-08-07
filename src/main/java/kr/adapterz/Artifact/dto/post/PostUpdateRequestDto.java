package kr.adapterz.Artifact.dto.post;

import jakarta.validation.constraints.Size;
import kr.adapterz.Artifact.entity.PostCategory;

import static kr.adapterz.Artifact.response.code.ValidationMessage.TITLE_INVALID;

/** PATCH 요청이므로 null인 필드는 기존 값을 유지합니다. */
public class PostUpdateRequestDto {
    @Size(max = 26, message = TITLE_INVALID)
    private String title;
    private String content;
    // PATCH이므로 null은 기존 카테고리와 고정 상태를 유지한다는 의미입니다.
    private PostCategory category;
    private Boolean pinned;
    // true이면 새 파일이 없을 때 기존 게시글 이미지를 제거합니다.
    private boolean removeImage;

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public PostCategory getCategory() { return category; }
    public Boolean getPinned() { return pinned; }
    public boolean isRemoveImage() { return removeImage; }
}

package kr.adapterz.Artifact.dto.comment;

import jakarta.validation.constraints.NotBlank;

import static kr.adapterz.Artifact.response.code.ValidationMessage.CONTENT_EMPTY;

/** 댓글 작성과 수정 요청의 content를 검증합니다. */
public class CommentRequestDto {
    @NotBlank(message = CONTENT_EMPTY)
    private String content;

    public String getContent() { return content; }
}

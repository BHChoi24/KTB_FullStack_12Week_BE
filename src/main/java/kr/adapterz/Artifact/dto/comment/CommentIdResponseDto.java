package kr.adapterz.Artifact.dto.comment;

public class CommentIdResponseDto {
    private final Long commentId;

    public CommentIdResponseDto(Long commentId) {
        this.commentId = commentId;
    }

    public Long getCommentId() {
        return commentId;
    }
}

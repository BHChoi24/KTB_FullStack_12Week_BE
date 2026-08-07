package kr.adapterz.Artifact.dto.comment;


import kr.adapterz.Artifact.entity.Comment;

/** 댓글 도메인 객체를 API 응답 형식으로 변환합니다. */
public class CommentResponseDto {
    private Long commentId;  // 시트 명세에 명시된 하위 필드명 준수 ("comment_id" 매핑용)
    private Long userId;     // 댓글을 작성한 사람의 ID
    private String nickname;
    // 각 댓글 작성자의 프로필 이미지 경로이며, 없으면 프론트가 기본 이미지를 사용합니다.
    private String profileImage;
    private java.time.LocalDateTime createdAt;
    private String content;   // 댓글 내용

    private CommentResponseDto(Comment comment, String nickname, String profileImage) {
        this.commentId = comment.getId();
        this.userId = comment.getUserId();
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.createdAt = comment.getCreatedAt();
        this.content = comment.getContent();
    }

    public static CommentResponseDto from(
            Comment comment,
            String nickname,
            String profileImage
    ) {
        // 상세 조회에서는 사용자 일괄 조회 결과를 받아 댓글마다 추가 DB 조회가 발생하지 않게 합니다.
        return new CommentResponseDto(comment, nickname, profileImage);
    }

    /** 새 댓글 작성 직후 이미 영속 상태인 작성자 참조를 사용합니다. */
    public static CommentResponseDto from(Comment comment) {
        // 댓글 등록 직후에는 Comment가 이미 참조하는 User를 사용하므로 별도 사용자 조회가 필요 없습니다.
        return from(
                comment,
                comment.getUser().getNickname(),
                comment.getUser().getProfileImage()
        );
    }

    public Long getCommentId() { return commentId; }
    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getProfileImage() { return profileImage; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public String getContent() { return content; }
}

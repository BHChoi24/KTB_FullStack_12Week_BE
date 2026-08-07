package kr.adapterz.Artifact.controller;

import jakarta.validation.Valid;
import kr.adapterz.Artifact.dto.comment.CommentIdResponseDto;
import kr.adapterz.Artifact.dto.comment.CommentRequestDto;
import kr.adapterz.Artifact.dto.comment.CommentResponseDto;
import kr.adapterz.Artifact.entity.Comment;
import kr.adapterz.Artifact.response.ApiResponse;
import kr.adapterz.Artifact.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static kr.adapterz.Artifact.response.code.SuccessCode.*;

/**
 * 댓글 작성, 수정, 삭제 요청을 받는 Controller입니다.
 * 댓글의 존재 여부와 작성자 권한 같은 규칙은 CommentService가 담당합니다.
 */
@RestController
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 댓글 작성 API: POST /posts/{postId}/comments
     * 새 댓글이 생성되므로 201 Created를 반환합니다.
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommentResponseDto> createComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequestDto request
    ) {
        // 1. Service가 로그인 여부와 부모 게시글 존재 여부를 확인하고 댓글을 저장합니다.
        Comment comment = commentService.createComment(postId, userId, request);

        // 2. 저장된 댓글이 이미 참조하는 작성자 정보로 응답 DTO를 만듭니다.
        return ApiResponse.of(
                COMMENT_CREATE_SUCCESS,
                CommentResponseDto.from(comment)
        );
    }

    /** 댓글 수정 API: PATCH /posts/{postId}/comments/{commentId} */
    @PatchMapping("/posts/{postId}/comments/{commentId}")
    public ApiResponse<CommentIdResponseDto> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequestDto request
    ) {
        // Spring Security가 관리자 또는 작성자 권한과 댓글 경로 관계를 검사합니다.
        Comment comment = commentService.updateComment(postId, commentId, request);

        // 수정 응답은 명세에 따라 댓글 번호만 반환합니다.
        return ApiResponse.of(COMMENT_UPDATE_SUCCESS, new CommentIdResponseDto(comment.getId()));
    }

    /** 댓글 삭제 API: DELETE /posts/{postId}/comments/{commentId} */
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        // 관리자 또는 작성자 권한을 통과한 경우에만 삭제됩니다.
        commentService.deleteComment(postId, commentId);
        return ApiResponse.of(COMMENT_DELETE_SUCCESS, null);
    }
}

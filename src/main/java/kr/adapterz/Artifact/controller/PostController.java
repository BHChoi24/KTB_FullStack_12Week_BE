package kr.adapterz.Artifact.controller;


import jakarta.validation.Valid;
import kr.adapterz.Artifact.dto.post.*;
import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.entity.PostCategory;
import kr.adapterz.Artifact.entity.PostPeriod;
import kr.adapterz.Artifact.entity.PostSort;
import kr.adapterz.Artifact.response.ApiResponse;
import kr.adapterz.Artifact.service.PostLikeService;
import kr.adapterz.Artifact.service.PostQueryService;
import kr.adapterz.Artifact.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static kr.adapterz.Artifact.response.code.SuccessCode.*;

/**
 * 게시글관련 API의 Controller
 * 게시글의 생성/조회/수정/삭제 규칙은 PostService가 처리
 * 목록과 상세 응답 조립은 PostQueryService가 처리
 * +게시글 좋아요부분 추가
 */
@RestController
public class PostController {
    private final PostService postService;
    private final PostLikeService postLikeService;
    private final PostQueryService postQueryService;

    public PostController(PostService postService, PostLikeService postLikeService, PostQueryService postQueryService) {
        this.postService = postService;
        this.postLikeService = postLikeService;
        this.postQueryService = postQueryService;
    }

    // 게시글 작성 API: POST /posts
    // 사용자 번호는 검증된 JWT에서 받고, body에서는 제목/본문/이미지를 받습니다.
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(
            value = "/posts",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<PostCreateResponseDto> createPost(
            @AuthenticationPrincipal Long userId,
            // 제목과 본문은 application/json 형식의 request 파트로 받습니다.
            @Valid @RequestPart("request") PostCreateRequestDto request,
            // 파일을 선택하지 않아도 게시글을 작성할 수 있습니다.
            @RequestPart(value = "post_image", required = false)
            MultipartFile postImage
    ) {
        // Service가 로그인 여부를 확인하고 게시글을 저장합니다.
        Post createdPost = postService.createPost(userId, request, postImage);

        // 저장된 Post를 외부에 보여 줄 응답 DTO로 변환합니다.
        return ApiResponse.of(
                POST_ADD_SUCCESS,
                new PostCreateResponseDto(createdPost)
        );
    }


    //게시글 목록 API: GET /posts?page=1
    //@RequestParam은 URL 뒤의 page 값을 받으며, 생략하면 1을 사용합니다.
    @GetMapping("/posts")
    public ApiResponse<PostPageResponseDto> getPosts(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) PostCategory category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ALL") PostPeriod period,
            @RequestParam(defaultValue = "LATEST") PostSort sort
    ) {
        // 목록과 서버가 계산한 다음 페이지 존재 여부를 하나의 응답으로 전달합니다.
        return ApiResponse.of(
                POST_LIST_SUCCESS,
                postQueryService.getPosts(userId, page, category, keyword, period, sort)
        );
    }

    //게시글 상세 API: GET /posts/{postId}
    @GetMapping("/posts/{postId}")
    public ApiResponse<PostDetailResponseDto> getPost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    ) {
        return ApiResponse.of(POST_DETAIL_SUCCESS, postQueryService.getPost(userId, postId));
    }
    /** 여기서부터는 좋아요 기능 관련**/
    //게시글 좋아요 토글 API: 안 눌렀으면 좋아요, 이미 눌렀으면 좋아요 취소
    @PostMapping("/posts/{postId}/likes")
    public ApiResponse<PostLikeResponseDto> togglePostLike(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    ) {
        return ApiResponse.of(POST_LIKE_TOGGLE_SUCCESS, postLikeService.togglePostLike(userId, postId));
    }


    //게시글 수정 API: PATCH /posts/{postId} PATCH이므로 body에서 전달한 필드만 변경
    @PatchMapping(
            value = "/posts/{postId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<PostUpdateResponseDto> updatePost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @Valid @RequestPart("request") PostUpdateRequestDto request,
            @RequestPart(value = "post_image", required = false)
            MultipartFile postImage
    ) {
        // Service 메서드 실행 전에 Spring Security가 관리자 또는 작성자 권한을 검사합니다.
        Post post = postService.updatePost(userId, postId, request, postImage);
        return ApiResponse.of(POST_UPDATE_SUCCESS, new PostUpdateResponseDto(post));
    }



    //게시글 삭제 API: DELETE /posts/{postId} */
    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Void> deletePost(
            @PathVariable Long postId
    ) {
        // 관리자 또는 작성자 권한 확인 후 게시글과 그 아래 댓글을 함께 삭제합니다.
        postService.deletePost(postId);
        return ApiResponse.of(POST_DELETE_SUCCESS, null);
    }

}

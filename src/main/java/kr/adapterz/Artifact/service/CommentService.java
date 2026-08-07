package kr.adapterz.Artifact.service;


import kr.adapterz.Artifact.dto.comment.CommentRequestDto;
import kr.adapterz.Artifact.entity.Comment;
import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.entity.User;
import kr.adapterz.Artifact.exception.NotFoundException;
import kr.adapterz.Artifact.repository.CommentRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static kr.adapterz.Artifact.response.code.ErrorCode.COMMENT_NOT_FOUND;

//게스글 댓글
@Service
@Transactional(readOnly = true)
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostService postService;
    private final UserService userService;

    public CommentService(CommentRepository commentRepository, PostService postService, UserService userService) {
        this.commentRepository = commentRepository;
        this.postService = postService;
        this.userService = userService;
    }

    //댓글생성
    @Transactional
    public Comment createComment(Long postId, Long userId, CommentRequestDto request) {
        // 1. 로그인한 사용자인지 확인합니다.
        User user = userService.requireAuthenticated(userId);
        // 2. 댓글을 작성할 게시글이 실제로 존재하는지 확인합니다.
        Post post = postService.findById(postId);
        // 3. 댓글을 저장합니다.
        return commentRepository.save(new Comment(post, user, request.getContent()));
    }

    //댓글찾기
    public Comment findById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(COMMENT_NOT_FOUND));
    }

    //댓글 삭제
    @PreAuthorize("hasRole('ADMIN') or @commentAuthorization.isAuthor(#postId, #commentId, authentication.principal)")
    @Transactional
    public void deleteComment(Long postId, Long commentId) {
        Comment comment = findCommentInPost(postId, commentId);
        commentRepository.delete(comment);
    }

    //댓글 수정
    @PreAuthorize("hasRole('ADMIN') or @commentAuthorization.isAuthor(#postId, #commentId, authentication.principal)")
    @Transactional
    public Comment updateComment(
            Long postId,
            Long commentId,
            CommentRequestDto request
    ) {
        Comment comment = findCommentInPost(postId, commentId);
        comment.update(request.getContent());
        return comment;
    }


    // 댓글이 작성된 게시물 찾기
    public List<Comment> findByPostId(Long postId) {
        return commentRepository.findByPost_Id(postId);
    }

    //하나의 게시글에 댓글수 -> DB에서 개수만 가져오는걸로 변경함
    public long countByPostId(Long postId) {
        return commentRepository.countByPost_Id(postId);
    }

    //게시물별로 댓글이 몇개인지 Map으로 묶기, 게시글별로 댓글 볼때 사용
    public Map<Long, Long> countMapByPostIds(List<Long> postIds) {
        //게시글 없으면 빈 Map반환
        if (postIds.isEmpty()) {
            return Map.of();
        }
        // 댓글래포에서 postId별 댓글 수를 조회, 결과를 Map<게시글ID, 댓글수> 형태로 변환
        return commentRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    // 작성자와 관리자 모두 URL의 게시글과 실제 댓글의 관계를 확인합니다.
    private Comment findCommentInPost(Long postId, Long commentId) {
        postService.findById(postId);
        Comment comment = findById(commentId);
        if (!comment.getPostId().equals(postId)) {
            throw new NotFoundException(COMMENT_NOT_FOUND);
        }
        return comment;
    }

}

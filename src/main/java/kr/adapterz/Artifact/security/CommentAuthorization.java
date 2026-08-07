package kr.adapterz.Artifact.security;

import kr.adapterz.Artifact.entity.Comment;
import kr.adapterz.Artifact.exception.NotFoundException;
import kr.adapterz.Artifact.repository.CommentRepository;
import kr.adapterz.Artifact.repository.PostRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static kr.adapterz.Artifact.response.code.ErrorCode.COMMENT_NOT_FOUND;
import static kr.adapterz.Artifact.response.code.ErrorCode.POSTS_NOT_FOUND;

@Component("commentAuthorization")
@Transactional(readOnly = true)
public class CommentAuthorization {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentAuthorization(
            CommentRepository commentRepository,
            PostRepository postRepository
    ) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    public boolean isAuthor(Long postId, Long commentId, Long authenticatedUserId) {
        if (authenticatedUserId == null) {
            return false;
        }

        if (!postRepository.existsById(postId)) {
            throw new NotFoundException(POSTS_NOT_FOUND);
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(COMMENT_NOT_FOUND));

        if (!comment.getPostId().equals(postId)) {
            throw new NotFoundException(COMMENT_NOT_FOUND);
        }

        return comment.getUserId().equals(authenticatedUserId);
    }
}

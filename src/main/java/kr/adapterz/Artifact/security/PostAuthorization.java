package kr.adapterz.Artifact.security;

import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.exception.NotFoundException;
import kr.adapterz.Artifact.repository.PostRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static kr.adapterz.Artifact.response.code.ErrorCode.POSTS_NOT_FOUND;

@Component("postAuthorization")
@Transactional(readOnly = true)
public class PostAuthorization {
    private final PostRepository postRepository;

    public PostAuthorization(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public boolean isAuthor(Long postId, Long authenticatedUserId) {
        if (authenticatedUserId == null) {
            return false;
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(POSTS_NOT_FOUND));

        return post.getUserId().equals(authenticatedUserId);
    }
}

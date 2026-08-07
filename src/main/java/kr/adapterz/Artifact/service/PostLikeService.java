package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.dto.post.PostLikeResponseDto;
import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.entity.PostLike;
import kr.adapterz.Artifact.entity.User;
import kr.adapterz.Artifact.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {
    private final PostLikeRepository postLikeRepository;
    private final UserService userService;
    private final PostService postService;

    @Transactional
    public PostLikeResponseDto togglePostLike(Long userId, Long postId) {
        User user = userService.requireAuthenticated(userId);
        Post post = postService.findById(postId);

        return postLikeRepository.findByUser_IdAndPost_Id(userId, postId)
                .map(postLike -> unlikePost(post, postLike))
                .orElseGet(() -> likePost(user, post));
    }

    // 현재 사용자가 해당 게시글에 좋아요를 눌렀는지 확인합니다.
    public boolean isLiked(Long userId, Long postId) {
        return postLikeRepository.existsByUser_IdAndPost_Id(userId, postId);
    }

    private PostLikeResponseDto likePost(User user, Post post) {
        postLikeRepository.save(new PostLike(user, post));
        post.increaseLikesCount();

        return new PostLikeResponseDto(post, true);
    }

    private PostLikeResponseDto unlikePost(Post post, PostLike postLike) {
        postLikeRepository.delete(postLike);
        post.decreaseLikesCount();

        return new PostLikeResponseDto(post, false);
    }
}

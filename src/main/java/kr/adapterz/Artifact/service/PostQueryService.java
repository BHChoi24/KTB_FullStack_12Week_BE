package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.dto.comment.CommentResponseDto;
import kr.adapterz.Artifact.dto.post.PostDetailResponseDto;
import kr.adapterz.Artifact.dto.post.PostListResponseDto;
import kr.adapterz.Artifact.dto.post.PostPageResponseDto;
import kr.adapterz.Artifact.dto.user.UserDisplayInfoDto;
import kr.adapterz.Artifact.entity.Comment;
import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.entity.PostCategory;
import kr.adapterz.Artifact.entity.PostPeriod;
import kr.adapterz.Artifact.entity.PostSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 게시글 조회 결과에 작성자, 댓글, 좋아요 정보를 조립하는 조회 전용 서비스입니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {
    private static final int POST_PAGE_SIZE = 10;

    private final PostService postService;
    private final CommentService commentService;
    private final UserService userService;
    private final PostLikeService postLikeService;
    private final PostPeriodRangeCalculator postPeriodRangeCalculator;

    public PostPageResponseDto getPosts(
            Long userId,
            int page,
            PostCategory category,
            String keyword,
            PostPeriod period,
            PostSort sort
    ) {
        userService.requireAuthenticated(userId);

        PostPeriodRange range = postPeriodRangeCalculator.calculate(period);
        PostSearchCondition condition = PostSearchCondition.of(
                category,
                keyword,
                period,
                sort,
                range
        );
        Page<Post> postPage = postService.findPage(page, POST_PAGE_SIZE, condition);
        List<Post> posts = postPage.getContent();
        List<Post> pinnedNotices = postService.findPinnedNotices();
        List<Post> allVisiblePosts = java.util.stream.Stream
                .concat(pinnedNotices.stream(), posts.stream())
                .toList();
        Set<Long> userIds = allVisiblePosts.stream()
                .map(Post::getUserId)
                .collect(Collectors.toSet());
        // 사용자별 조회를 반복하지 않고 작성자 ID 전체를 한 번에 조회합니다.
        Map<Long, UserDisplayInfoDto> userInfoMap =
                userService.findDisplayInfoMapByIds(userIds);

        // 같은 고정 공지가 상단과 일반 목록에 동시에 있어도 집계 쿼리에는 ID를 한 번만 전달합니다.
        List<Long> postIds = allVisiblePosts.stream()
                .map(Post::getId)
                .distinct()
                .toList();
        Map<Long, Long> commentCountMap = commentService.countMapByPostIds(postIds);

        java.util.function.Function<Post, PostListResponseDto> toResponse = post -> {
            UserDisplayInfoDto author = userInfoMap.get(post.getUserId());
            return PostListResponseDto.from(
                    post,
                    author.getNickname(),
                    author.getProfileImage(),
                    commentCountMap.getOrDefault(post.getId(), 0L)
            );
        };

        List<PostListResponseDto> pinnedResponses = pinnedNotices.stream()
                .map(toResponse)
                .toList();
        List<PostListResponseDto> postResponses = posts.stream()
                .map(post -> {
                    // Map의 게시글 작성자 번호로 해당 닉네임과 프로필 경로를 찾습니다.
                    return toResponse.apply(post);
                })
                .toList();

        // 중요: 데이터 개수로 추측하지 않고 Spring Data가 계산한 다음 페이지 여부를 응답합니다.
        return new PostPageResponseDto(pinnedResponses, postResponses, postPage.hasNext());
    }

    @Transactional
    public PostDetailResponseDto getPost(Long userId, Long postId) {
        userService.requireAuthenticated(userId);

        Post post = postService.findByIdAndIncreaseView(postId);
        List<Comment> comments = commentService.findByPostId(postId);

        // 게시글 작성자와 댓글 작성자를 함께 조회하여 상세 조회에서도 사용자 쿼리를 한 번만 실행합니다.
        Set<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());
        userIds.add(post.getUserId());
        // 게시글 작성자와 댓글 작성자를 합친 Set이므로 중복 ID는 자동으로 한 번만 조회됩니다.
        Map<Long, UserDisplayInfoDto> userInfoMap =
                userService.findDisplayInfoMapByIds(userIds);

        List<CommentResponseDto> commentResponses = comments.stream()
                .map(comment -> {
                    UserDisplayInfoDto author = userInfoMap.get(comment.getUserId());
                    return CommentResponseDto.from(
                            comment,
                            author.getNickname(),
                            author.getProfileImage()
                    );
                })
                .toList();

        UserDisplayInfoDto postAuthor = userInfoMap.get(post.getUserId());
        // 게시글 작성자 이미지와 댓글 작성자 이미지가 각각 응답에 포함되도록 최종 DTO를 조립합니다.
        return PostDetailResponseDto.builder()
                .postId(post.getId())
                .userId(post.getUserId())
                .title(post.getTitle())
                .content(post.getContent())
                .nickname(postAuthor.getNickname())
                .profileImage(postAuthor.getProfileImage())
                .createdAt(post.getCreatedAt())
                .imageUrl(post.getImageUrl())
                .likesCount(post.getLikesCount())
                .commentsCount((long) commentResponses.size())
                .viewsCount(post.getViewsCount())
                .liked(postLikeService.isLiked(userId, postId))
                .category(post.getCategory())
                .pinned(post.isPinned())
                .comments(commentResponses)
                .build();
    }
}

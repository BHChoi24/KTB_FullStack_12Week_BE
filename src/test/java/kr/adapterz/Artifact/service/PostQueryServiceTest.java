package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.dto.post.PostPageResponseDto;
import kr.adapterz.Artifact.dto.user.UserDisplayInfoDto;
import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.entity.PostCategory;
import kr.adapterz.Artifact.entity.PostPeriod;
import kr.adapterz.Artifact.entity.PostSort;
import kr.adapterz.Artifact.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostQueryService 게시글 목록 테스트")
class PostQueryServiceTest {

    private static final Long LOGIN_USER_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long AUTHOR_ID = 2L;
    private static final int PAGE_SIZE = 10;

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    @Mock
    private UserService userService;

    @Mock
    private PostLikeService postLikeService;

    @Mock
    private PostPeriodRangeCalculator postPeriodRangeCalculator;

    @InjectMocks
    private PostQueryService postQueryService;

    @Test
    void getPosts_다음_페이지가_있으면_hasNext를_true로_반환한다() {
        // Given: 첫 페이지에 게시글이 있고 전체 개수가 페이지 크기보다 큰 상황
        Post post = post();
        Page<Post> postPage = new PageImpl<>(
                List.of(post),
                PageRequest.of(0, PAGE_SIZE),
                PAGE_SIZE + 1L
        );
        given(userService.requireAuthenticated(LOGIN_USER_ID)).willReturn(org.mockito.Mockito.mock(User.class));
        given(postPeriodRangeCalculator.calculate(PostPeriod.ALL)).willReturn(PostPeriodRange.all());
        given(postService.findPage(org.mockito.ArgumentMatchers.eq(1), org.mockito.ArgumentMatchers.eq(PAGE_SIZE), any(PostSearchCondition.class))).willReturn(postPage);
        given(postService.findPinnedNotices()).willReturn(List.of());
        given(userService.findDisplayInfoMapByIds(Set.of(AUTHOR_ID))).willReturn(
                Map.of(AUTHOR_ID, new UserDisplayInfoDto("작성자", "/uploads/profiles/author.jpg"))
        );
        given(commentService.countMapByPostIds(List.of(POST_ID))).willReturn(Map.of(POST_ID, 3L));

        // When
        PostPageResponseDto result = getPosts(null);

        // Then: 목록과 Page.hasNext() 결과가 응답 DTO에 함께 담깁니다.
        assertTrue(result.isHasNext());
        assertEquals(1, result.getPosts().size());
        assertEquals(POST_ID, result.getPosts().getFirst().getPostId());
        assertEquals(3L, result.getPosts().getFirst().getCommentsCount());
        assertEquals(
                "/uploads/profiles/author.jpg",
                result.getPosts().getFirst().getProfileImage()
        );
    }

    @Test
    void getPosts_마지막_페이지이면_hasNext를_false로_반환한다() {
        // Given: 현재 페이지가 마지막인 상황
        Post post = post();
        Page<Post> postPage = new PageImpl<>(
                List.of(post),
                PageRequest.of(0, PAGE_SIZE),
                1L
        );
        given(userService.requireAuthenticated(LOGIN_USER_ID)).willReturn(org.mockito.Mockito.mock(User.class));
        given(postPeriodRangeCalculator.calculate(PostPeriod.ALL)).willReturn(PostPeriodRange.all());
        given(postService.findPage(org.mockito.ArgumentMatchers.eq(1), org.mockito.ArgumentMatchers.eq(PAGE_SIZE), any(PostSearchCondition.class))).willReturn(postPage);
        given(postService.findPinnedNotices()).willReturn(List.of());
        given(userService.findDisplayInfoMapByIds(Set.of(AUTHOR_ID))).willReturn(
                Map.of(AUTHOR_ID, new UserDisplayInfoDto("작성자", null))
        );
        given(commentService.countMapByPostIds(List.of(POST_ID))).willReturn(Map.of());

        // When
        PostPageResponseDto result = getPosts(null);

        // Then
        assertFalse(result.isHasNext());
        assertEquals(1, result.getPosts().size());
    }

    @Test
    void getPosts_카테고리목록과_고정공지를_분리해_반환한다() {
        Post question = post();
        Post notice = org.mockito.Mockito.mock(Post.class);
        given(notice.getId()).willReturn(20L);
        given(notice.getUserId()).willReturn(3L);
        given(notice.getTitle()).willReturn("고정 공지");
        given(notice.getCategory()).willReturn(PostCategory.NOTICE);
        given(notice.isPinned()).willReturn(true);
        given(notice.getLikesCount()).willReturn(0L);
        given(notice.getViewsCount()).willReturn(0L);
        Page<Post> postPage = new PageImpl<>(List.of(question));

        given(userService.requireAuthenticated(LOGIN_USER_ID)).willReturn(org.mockito.Mockito.mock(User.class));
        given(postPeriodRangeCalculator.calculate(PostPeriod.ALL)).willReturn(PostPeriodRange.all());
        given(postService.findPage(org.mockito.ArgumentMatchers.eq(1), org.mockito.ArgumentMatchers.eq(PAGE_SIZE), any(PostSearchCondition.class))).willReturn(postPage);
        given(postService.findPinnedNotices()).willReturn(List.of(notice));
        given(userService.findDisplayInfoMapByIds(Set.of(AUTHOR_ID, 3L))).willReturn(Map.of(
                AUTHOR_ID, new UserDisplayInfoDto("작성자", null),
                3L, new UserDisplayInfoDto("운영자", null)
        ));
        given(commentService.countMapByPostIds(List.of(20L, POST_ID))).willReturn(Map.of());

        PostPageResponseDto result = getPosts(PostCategory.QUESTION);

        assertEquals(1, result.getPinnedNotices().size());
        assertEquals(PostCategory.NOTICE, result.getPinnedNotices().getFirst().getCategory());
        assertEquals(1, result.getPosts().size());
        verify(postService).findPage(org.mockito.ArgumentMatchers.eq(1), org.mockito.ArgumentMatchers.eq(PAGE_SIZE), any(PostSearchCondition.class));
    }

    @Test
    void getPosts_고정공지가_일반목록에도_있으면_댓글집계_ID는_중복제거한다() {
        Post notice = org.mockito.Mockito.mock(Post.class);
        given(notice.getId()).willReturn(20L);
        given(notice.getUserId()).willReturn(3L);
        given(notice.getTitle()).willReturn("고정 공지");
        given(notice.getCategory()).willReturn(PostCategory.NOTICE);
        given(notice.isPinned()).willReturn(true);
        given(notice.getLikesCount()).willReturn(0L);
        given(notice.getViewsCount()).willReturn(0L);

        given(userService.requireAuthenticated(LOGIN_USER_ID)).willReturn(org.mockito.Mockito.mock(User.class));
        given(postPeriodRangeCalculator.calculate(PostPeriod.ALL)).willReturn(PostPeriodRange.all());
        given(postService.findPage(org.mockito.ArgumentMatchers.eq(1), org.mockito.ArgumentMatchers.eq(PAGE_SIZE), any(PostSearchCondition.class)))
                .willReturn(new PageImpl<>(List.of(notice)));
        given(postService.findPinnedNotices()).willReturn(List.of(notice));
        given(userService.findDisplayInfoMapByIds(Set.of(3L))).willReturn(
                Map.of(3L, new UserDisplayInfoDto("운영자", null))
        );
        given(commentService.countMapByPostIds(List.of(20L))).willReturn(Map.of(20L, 2L));

        PostPageResponseDto result = getPosts(PostCategory.NOTICE);

        assertEquals(1, result.getPinnedNotices().size());
        assertEquals(1, result.getPosts().size());
        verify(commentService).countMapByPostIds(List.of(20L));
    }

    private PostPageResponseDto getPosts(PostCategory category) {
        return postQueryService.getPosts(
                LOGIN_USER_ID,
                1,
                category,
                null,
                PostPeriod.ALL,
                PostSort.LATEST
        );
    }

    private Post post() {
        Post post = org.mockito.Mockito.mock(Post.class);
        given(post.getId()).willReturn(POST_ID);
        given(post.getUserId()).willReturn(AUTHOR_ID);
        given(post.getTitle()).willReturn("게시글 제목");
        given(post.getLikesCount()).willReturn(0L);
        given(post.getViewsCount()).willReturn(0L);
        return post;
    }
}

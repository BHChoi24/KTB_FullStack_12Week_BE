package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.dto.post.PostCreateRequestDto;
import kr.adapterz.Artifact.dto.post.PostUpdateRequestDto;
import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.entity.PostCategory;
import kr.adapterz.Artifact.entity.User;
import kr.adapterz.Artifact.entity.UserRole;
import kr.adapterz.Artifact.exception.ForbiddenException;
import kr.adapterz.Artifact.exception.InvalidInputException;
import kr.adapterz.Artifact.repository.CommentRepository;
import kr.adapterz.Artifact.repository.PostLikeRepository;
import kr.adapterz.Artifact.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PostServiceCategoryTest {
    private static final Long USER_ID = 1L;
    private static final Long POST_ID = 10L;

    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private UserService userService;
    @Mock private PostImageStorageService postImageStorageService;
    @InjectMocks private PostService postService;

    @Test
    void 일반사용자는_공지사항을_작성할_수_없다() {
        User user = user(UserRole.USER);
        PostCreateRequestDto request = createRequest(PostCategory.NOTICE, false);
        given(userService.requireAuthenticated(USER_ID)).willReturn(user);

        assertThrows(
                ForbiddenException.class,
                () -> postService.createPost(USER_ID, request, null)
        );
        then(postImageStorageService).should(never()).store(any());
    }

    @Test
    void 관리자는_공지를_작성하면서_상단에_고정할_수_있다() {
        User admin = user(UserRole.ADMIN);
        PostCreateRequestDto request = createRequest(PostCategory.NOTICE, true);
        given(userService.requireAuthenticated(USER_ID)).willReturn(admin);
        given(postRepository.countByPinnedTrue()).willReturn(1L);
        given(postRepository.saveAndFlush(any(Post.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Post result = postService.createPost(USER_ID, request, null);

        assertEquals(PostCategory.NOTICE, result.getCategory());
        assertEquals(true, result.isPinned());
    }

    @Test
    void 세번째_고정공지는_저장을_거부한다() {
        User admin = user(UserRole.ADMIN);
        PostCreateRequestDto request = createRequest(PostCategory.NOTICE, true);
        given(userService.requireAuthenticated(USER_ID)).willReturn(admin);
        given(postRepository.countByPinnedTrue()).willReturn(2L);

        InvalidInputException error = assertThrows(
                InvalidInputException.class,
                () -> postService.createPost(USER_ID, request, null)
        );

        assertEquals("pinned_limit_exceeded", error.getErrors().getFirst().getReason());
        then(postImageStorageService).should(never()).store(any());
    }

    @Test
    void 공지가_아닌_게시글은_고정할_수_없다() {
        User admin = user(UserRole.ADMIN);
        PostCreateRequestDto request = createRequest(PostCategory.QUESTION, true);
        given(userService.requireAuthenticated(USER_ID)).willReturn(admin);

        InvalidInputException error = assertThrows(
                InvalidInputException.class,
                () -> postService.createPost(USER_ID, request, null)
        );

        assertEquals("pinned_notice_only", error.getErrors().getFirst().getReason());
    }

    @Test
    void 일반사용자는_자신의_글도_공지사항으로_변경할_수_없다() {
        User user = user(UserRole.USER);
        Post post = new Post(user, "질문", "내용", null, PostCategory.QUESTION);
        PostUpdateRequestDto request = mock(PostUpdateRequestDto.class);
        given(request.getCategory()).willReturn(PostCategory.NOTICE);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(userService.requireAuthenticated(USER_ID)).willReturn(user);

        assertThrows(
                ForbiddenException.class,
                () -> postService.updatePost(USER_ID, POST_ID, request, null)
        );
    }

    @Test
    void 관리자가_고정공지를_일반카테고리로_바꾸면_고정을_자동해제한다() {
        User admin = user(UserRole.ADMIN);
        Post post = new Post(admin, "공지", "내용", null, PostCategory.NOTICE);
        post.updatePinned(true);
        PostUpdateRequestDto request = mock(PostUpdateRequestDto.class);
        given(request.getCategory()).willReturn(PostCategory.QUESTION);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(userService.requireAuthenticated(USER_ID)).willReturn(admin);
        given(postRepository.saveAndFlush(post)).willReturn(post);

        Post result = postService.updatePost(USER_ID, POST_ID, request, null);

        assertEquals(PostCategory.QUESTION, result.getCategory());
        assertEquals(false, result.isPinned());
        assertEquals(null, result.getPinnedAt());
    }

    private PostCreateRequestDto createRequest(PostCategory category, boolean pinned) {
        PostCreateRequestDto request = mock(PostCreateRequestDto.class);
        lenient().when(request.getTitle()).thenReturn("제목");
        lenient().when(request.getContent()).thenReturn("내용");
        given(request.getCategory()).willReturn(category);
        given(request.isPinned()).willReturn(pinned);
        return request;
    }

    private User user(UserRole role) {
        User user = mock(User.class);
        lenient().when(user.getRole()).thenReturn(role);
        return user;
    }
}

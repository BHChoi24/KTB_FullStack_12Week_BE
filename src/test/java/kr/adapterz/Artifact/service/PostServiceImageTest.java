package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.dto.post.PostCreateRequestDto;
import kr.adapterz.Artifact.dto.post.PostUpdateRequestDto;
import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.entity.PostCategory;
import kr.adapterz.Artifact.entity.User;
import kr.adapterz.Artifact.exception.InvalidInputException;
import kr.adapterz.Artifact.repository.CommentRepository;
import kr.adapterz.Artifact.repository.PostLikeRepository;
import kr.adapterz.Artifact.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PostServiceImageTest {
    private static final Long USER_ID = 1L;
    private static final Long POST_ID = 10L;

    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private UserService userService;
    @Mock
    private PostImageStorageService postImageStorageService;
    @InjectMocks
    private PostService postService;

    @Test
    void 게시글_생성시_이미지_경로를_DB에_저장한다() {
        User user = user();
        PostCreateRequestDto request = createRequest();
        MultipartFile image = mock(MultipartFile.class);
        given(userService.requireAuthenticated(USER_ID)).willReturn(user);
        given(postImageStorageService.store(image))
                .willReturn("/uploads/posts/new.jpg");
        given(postRepository.saveAndFlush(any(Post.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Post result = postService.createPost(USER_ID, request, image);

        assertEquals("/uploads/posts/new.jpg", result.getImageUrl());
    }

    @Test
    void 게시글_DB_저장에_실패하면_새_이미지를_삭제한다() {
        User user = user();
        PostCreateRequestDto request = createRequest();
        MultipartFile image = mock(MultipartFile.class);
        given(userService.requireAuthenticated(USER_ID)).willReturn(user);
        given(postImageStorageService.store(image))
                .willReturn("/uploads/posts/new.jpg");
        given(postRepository.saveAndFlush(any(Post.class)))
                .willThrow(new IllegalStateException("db error"));

        assertThrows(
                IllegalStateException.class,
                () -> postService.createPost(USER_ID, request, image)
        );
        then(postImageStorageService).should().delete("/uploads/posts/new.jpg");
    }

    @Test
    void 수정시_새_파일이_없으면_기존_이미지를_유지한다() {
        Post post = post("/uploads/posts/old.jpg");
        PostUpdateRequestDto request = updateRequest(false);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(userService.requireAuthenticated(USER_ID)).willReturn(post.getUser());
        given(postRepository.saveAndFlush(post)).willReturn(post);

        Post result = postService.updatePost(USER_ID, POST_ID, request, null);

        assertEquals("/uploads/posts/old.jpg", result.getImageUrl());
        then(postImageStorageService).should(never()).store(any());
        then(postImageStorageService).should(never()).delete("/uploads/posts/old.jpg");
    }

    @Test
    void 수정시_removeImage가_true이면_기존_이미지를_제거한다() {
        Post post = post("/uploads/posts/old.jpg");
        PostUpdateRequestDto request = updateRequest(true);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(userService.requireAuthenticated(USER_ID)).willReturn(post.getUser());
        given(postRepository.saveAndFlush(post)).willReturn(post);

        Post result = postService.updatePost(USER_ID, POST_ID, request, null);

        assertNull(result.getImageUrl());
        // 단위 테스트에는 실제 트랜잭션이 없으므로 기존 파일 정리를 즉시 실행합니다.
        then(postImageStorageService).should().delete("/uploads/posts/old.jpg");
    }

    @Test
    void 새_이미지와_삭제를_동시에_요청하면_거부한다() {
        Post post = post("/uploads/posts/old.jpg");
        PostUpdateRequestDto request = updateRequest(true);
        MultipartFile image = mock(MultipartFile.class);
        given(image.isEmpty()).willReturn(false);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(userService.requireAuthenticated(USER_ID)).willReturn(post.getUser());

        assertThrows(
                InvalidInputException.class,
                () -> postService.updatePost(USER_ID, POST_ID, request, image)
        );
        then(postImageStorageService).should(never()).store(any());
    }

    private User user() {
        return new User("user@test.com", "Password1!", "사용자", null);
    }

    private Post post(String imageUrl) {
        return new Post(user(), "기존 제목", "기존 내용", imageUrl);
    }

    private PostCreateRequestDto createRequest() {
        PostCreateRequestDto request = mock(PostCreateRequestDto.class);
        given(request.getTitle()).willReturn("제목");
        given(request.getContent()).willReturn("내용");
        given(request.getCategory()).willReturn(PostCategory.DAILY_SHARE);
        return request;
    }

    private PostUpdateRequestDto updateRequest(boolean removeImage) {
        PostUpdateRequestDto request = mock(PostUpdateRequestDto.class);
        given(request.getTitle()).willReturn("수정 제목");
        given(request.getContent()).willReturn("수정 내용");
        given(request.isRemoveImage()).willReturn(removeImage);
        return request;
    }
}

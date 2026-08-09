package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.entity.PostPeriod;
import kr.adapterz.Artifact.entity.PostSort;
import kr.adapterz.Artifact.repository.CommentRepository;
import kr.adapterz.Artifact.repository.PostFullTextRepository;
import kr.adapterz.Artifact.repository.PostLikeRepository;
import kr.adapterz.Artifact.repository.PostRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PostServiceSearchTest {
    @Mock private PostRepository postRepository;
    @Mock private PostFullTextRepository postFullTextRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private UserService userService;
    @Mock private PostImageStorageService postImageStorageService;
    @InjectMocks private PostService postService;

    @ParameterizedTest
    @MethodSource("sortCases")
    void 선택한_정렬에_안정적인_보조정렬을_추가한다(
            PostSort postSort,
            List<String> expectedProperties,
            List<Sort.Direction> expectedDirections
    ) {
        given(postRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Post>>any(),
                any(Pageable.class)
        )).willAnswer(invocation -> Page.empty(invocation.getArgument(1)));
        PostSearchCondition condition = PostSearchCondition.of(
                null,
                null,
                PostPeriod.ALL,
                postSort,
                PostPeriodRange.all()
        );

        postService.findPage(1, 10, condition);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<Post>>any(),
                pageableCaptor.capture()
        );
        List<Sort.Order> orders = pageableCaptor.getValue().getSort().stream().toList();
        assertEquals(expectedProperties, orders.stream().map(Sort.Order::getProperty).toList());
        assertEquals(expectedDirections, orders.stream().map(Sort.Order::getDirection).toList());
    }

    @org.junit.jupiter.api.Test
    void 검색어가_있으면_FULLTEXT_저장소를_사용한다() {
        PostSearchCondition condition = PostSearchCondition.of(
                null,
                "검색 기능",
                PostPeriod.ALL,
                PostSort.LATEST,
                PostPeriodRange.all()
        );
        given(postFullTextRepository.search(any(PostSearchCondition.class), any(Pageable.class)))
                .willReturn(Page.empty());

        postService.findPage(1, 10, condition);

        verify(postFullTextRepository).search(org.mockito.ArgumentMatchers.eq(condition), any(Pageable.class));
        verify(postRepository, never()).findAll(
                org.mockito.ArgumentMatchers.<Specification<Post>>any(),
                any(Pageable.class)
        );
    }

    private static Stream<Arguments> sortCases() {
        return Stream.of(
                Arguments.of(
                        PostSort.LATEST,
                        List.of("createdAt", "id"),
                        List.of(Sort.Direction.DESC, Sort.Direction.DESC)
                ),
                Arguments.of(
                        PostSort.OLDEST,
                        List.of("createdAt", "id"),
                        List.of(Sort.Direction.ASC, Sort.Direction.ASC)
                ),
                Arguments.of(
                        PostSort.LIKES,
                        List.of("likesCount", "createdAt", "id"),
                        List.of(Sort.Direction.DESC, Sort.Direction.DESC, Sort.Direction.DESC)
                ),
                Arguments.of(
                        PostSort.VIEWS,
                        List.of("viewsCount", "createdAt", "id"),
                        List.of(Sort.Direction.DESC, Sort.Direction.DESC, Sort.Direction.DESC)
                )
        );
    }
}

package kr.adapterz.Artifact.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import kr.adapterz.Artifact.dto.post.PostCreateRequestDto;
import kr.adapterz.Artifact.dto.post.PostPageResponseDto;
import kr.adapterz.Artifact.dto.post.PostUpdateRequestDto;
import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.entity.PostCategory;
import kr.adapterz.Artifact.entity.PostPeriod;
import kr.adapterz.Artifact.entity.PostSort;
import kr.adapterz.Artifact.service.PostLikeService;
import kr.adapterz.Artifact.service.PostQueryService;
import kr.adapterz.Artifact.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostControllerMultipartTest {
    private PostService postService;
    private PostQueryService postQueryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        postService = mock(PostService.class);
        postQueryService = mock(PostQueryService.class);
        PostController controller = new PostController(
                postService,
                mock(PostLikeService.class),
                postQueryService
        );
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void 게시글_목록의_검색_기간_정렬_파라미터를_전달한다() throws Exception {
        given(postQueryService.getPosts(
                isNull(),
                org.mockito.ArgumentMatchers.eq(2),
                org.mockito.ArgumentMatchers.eq(PostCategory.NOTICE),
                org.mockito.ArgumentMatchers.eq("공지"),
                org.mockito.ArgumentMatchers.eq(PostPeriod.LAST_30_DAYS),
                org.mockito.ArgumentMatchers.eq(PostSort.LIKES)
        )).willReturn(new PostPageResponseDto(List.of(), List.of(), false));

        mockMvc.perform(get("/posts")
                        .param("page", "2")
                        .param("category", "NOTICE")
                        .param("keyword", "공지")
                        .param("period", "LAST_30_DAYS")
                        .param("sort", "LIKES"))
                .andExpect(status().isOk());

        verify(postQueryService).getPosts(
                null,
                2,
                PostCategory.NOTICE,
                "공지",
                PostPeriod.LAST_30_DAYS,
                PostSort.LIKES
        );
    }

    @Test
    void 게시글_목록의_기간과_정렬을_생략하면_기본값을_사용한다() throws Exception {
        given(postQueryService.getPosts(
                isNull(),
                org.mockito.ArgumentMatchers.eq(1),
                isNull(),
                isNull(),
                org.mockito.ArgumentMatchers.eq(PostPeriod.ALL),
                org.mockito.ArgumentMatchers.eq(PostSort.LATEST)
        )).willReturn(new PostPageResponseDto(List.of(), List.of(), false));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk());

        verify(postQueryService).getPosts(
                null,
                1,
                null,
                null,
                PostPeriod.ALL,
                PostSort.LATEST
        );
    }

    @Test
    void 게시글_작성_JSON과_이미지_파트를_각각_전달한다() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                """
                {
                  "title": "제목",
                  "content": "내용",
                  "category": "QUESTION",
                  "pinned": false
                }
                """.getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile imagePart = new MockMultipartFile(
                "post_image",
                "post.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );
        Post savedPost = mock(Post.class);
        given(postService.createPost(
                isNull(),
                any(PostCreateRequestDto.class),
                any(MultipartFile.class)
        )).willReturn(savedPost);

        mockMvc.perform(
                        multipart("/posts")
                                .file(requestPart)
                                .file(imagePart)
                )
                .andExpect(status().isCreated());

        ArgumentCaptor<PostCreateRequestDto> requestCaptor =
                ArgumentCaptor.forClass(PostCreateRequestDto.class);
        ArgumentCaptor<MultipartFile> imageCaptor =
                ArgumentCaptor.forClass(MultipartFile.class);
        verify(postService).createPost(
                isNull(),
                requestCaptor.capture(),
                imageCaptor.capture()
        );
        assertEquals("제목", requestCaptor.getValue().getTitle());
        assertEquals(PostCategory.QUESTION, requestCaptor.getValue().getCategory());
        assertEquals("post.png", imageCaptor.getValue().getOriginalFilename());
    }

    @Test
    void 게시글_수정_JSON에서_카테고리와_고정상태를_전달한다() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                """
                {
                  "title": "수정 제목",
                  "content": "수정 내용",
                  "category": "NOTICE",
                  "pinned": true,
                  "remove_image": false
                }
                """.getBytes(StandardCharsets.UTF_8)
        );
        Post savedPost = mock(Post.class);
        given(postService.updatePost(
                isNull(),
                org.mockito.ArgumentMatchers.eq(10L),
                any(PostUpdateRequestDto.class),
                isNull()
        )).willReturn(savedPost);

        mockMvc.perform(
                        multipart("/posts/{postId}", 10L)
                                .file(requestPart)
                                .with(request -> {
                                    request.setMethod("PATCH");
                                    return request;
                                })
                )
                .andExpect(status().isOk());

        ArgumentCaptor<PostUpdateRequestDto> requestCaptor =
                ArgumentCaptor.forClass(PostUpdateRequestDto.class);
        verify(postService).updatePost(
                isNull(),
                org.mockito.ArgumentMatchers.eq(10L),
                requestCaptor.capture(),
                isNull()
        );
        assertEquals(PostCategory.NOTICE, requestCaptor.getValue().getCategory());
        assertEquals(true, requestCaptor.getValue().getPinned());
    }
}

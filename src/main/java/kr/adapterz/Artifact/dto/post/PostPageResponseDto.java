package kr.adapterz.Artifact.dto.post;

import java.util.List;

/**
 * 게시글 목록과 다음 페이지 존재 여부를 함께 전달하는 응답 DTO입니다.
 *
 * <p>프론트가 게시글 개수로 다음 페이지를 추측하지 않고,
 * 서버가 계산한 페이징 정보를 그대로 사용할 수 있게 합니다.</p>
 */
public class PostPageResponseDto {
    private final List<PostListResponseDto> posts;
    private final List<PostListResponseDto> pinnedNotices;
    private final boolean hasNext;

    public PostPageResponseDto(
            List<PostListResponseDto> pinnedNotices,
            List<PostListResponseDto> posts,
            boolean hasNext
    ) {
        this.pinnedNotices = pinnedNotices;
        this.posts = posts;
        this.hasNext = hasNext;
    }

    public List<PostListResponseDto> getPosts() {
        return posts;
    }

    public List<PostListResponseDto> getPinnedNotices() {
        return pinnedNotices;
    }

    public boolean isHasNext() {
        return hasNext;
    }
}

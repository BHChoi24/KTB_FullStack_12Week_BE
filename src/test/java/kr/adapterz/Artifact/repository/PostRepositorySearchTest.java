package kr.adapterz.Artifact.repository;

import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.entity.PostCategory;
import kr.adapterz.Artifact.entity.PostPeriod;
import kr.adapterz.Artifact.entity.PostSort;
import kr.adapterz.Artifact.entity.User;
import kr.adapterz.Artifact.service.PostPeriodRange;
import kr.adapterz.Artifact.service.PostSearchCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 중요: Repository 검색 테스트는 MySQL 전환 중에도 독립된 H2 테스트 DB를 명시적으로 사용합니다.
@ActiveProfiles("h2")
@DataJpaTest
class PostRepositorySearchTest {
    @Autowired private PostRepository postRepository;
    @Autowired private UserRepository userRepository;

    private User author;

    @BeforeEach
    void setUp() {
        author = userRepository.save(new User(
                "author@example.com",
                "$2a$10$12345678901234567890123456789012345678901234567890123",
                "작성자",
                null
        ));
    }

    @Test
    void 전체와_공지사항_일반목록은_고정여부와_관계없이_조회한다() {
        Post pinnedNotice = post("고정 공지", PostCategory.NOTICE);
        pinnedNotice.updatePinned(true);
        Post normalNotice = post("일반 공지", PostCategory.NOTICE);
        Post daily = post("일상 글", PostCategory.DAILY_SHARE);
        postRepository.saveAllAndFlush(List.of(pinnedNotice, normalNotice, daily));

        List<Post> allPosts = search(condition(null, null));
        List<Post> notices = search(condition(PostCategory.NOTICE, null));

        assertEquals(3, allPosts.size());
        assertEquals(2, notices.size());
        assertTrue(notices.stream().anyMatch(Post::isPinned));
    }

    private Post post(String title, PostCategory category) {
        return new Post(author, title, "내용", null, category);
    }

    private PostSearchCondition condition(PostCategory category, String keyword) {
        return PostSearchCondition.of(
                category,
                keyword,
                PostPeriod.ALL,
                PostSort.LATEST,
                PostPeriodRange.all()
        );
    }

    private List<Post> search(PostSearchCondition condition) {
        return postRepository.findAll(
                PostSpecifications.matches(condition),
                PageRequest.of(0, 10)
        ).getContent();
    }
}

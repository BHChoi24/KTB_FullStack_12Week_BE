package kr.adapterz.Artifact.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostCategoryTest {

    @Test
    void 기존_생성자는_일상공유와_고정해제를_기본값으로_사용한다() {
        Post post = new Post(user(), "제목", "내용", null);

        assertEquals(PostCategory.DAILY_SHARE, post.getCategory());
        assertFalse(post.isPinned());
        assertNull(post.getPinnedAt());
    }

    @Test
    void 공지를_고정하면_고정시각을_기록하고_일반게시판으로_변경하면_해제한다() {
        Post post = new Post(user(), "공지", "내용", null, PostCategory.NOTICE);

        post.updatePinned(true);
        assertTrue(post.isPinned());
        assertNotNull(post.getPinnedAt());

        post.updateCategory(PostCategory.QUESTION);
        assertFalse(post.isPinned());
        assertNull(post.getPinnedAt());
    }

    @Test
    void 공지가_아닌_글은_고정할_수_없다() {
        Post post = new Post(user(), "질문", "내용", null, PostCategory.QUESTION);

        assertThrows(IllegalStateException.class, () -> post.updatePinned(true));
    }

    private User user() {
        return new User("user@test.com", "Password1!", "사용자", null);
    }
}

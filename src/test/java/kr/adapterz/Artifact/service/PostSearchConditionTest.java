package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.entity.PostPeriod;
import kr.adapterz.Artifact.entity.PostSort;
import kr.adapterz.Artifact.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostSearchConditionTest {

    @Test
    void 일반_검색어를_필수_단어_검색식으로_변환한다() {
        PostSearchCondition condition = condition("검색 기능");

        assertEquals("+검색 +기능", condition.fullTextQuery());
        assertTrue(condition.hasFullTextQuery());
    }

    @Test
    void Boolean_특수문자를_공백으로_바꾼다() {
        PostSearchCondition condition = condition("\"검색+기능\"");

        assertEquals("+검색 +기능", condition.fullTextQuery());
    }

    @Test
    void 한글자_단어는_제외하고_두글자_단어만_검색한다() {
        PostSearchCondition condition = condition("a 검색");

        assertEquals("+검색", condition.fullTextQuery());
    }

    @Test
    void 검색어가_없으면_FULLTEXT_조회로_분기하지_않는다() {
        PostSearchCondition condition = condition("   ");

        assertNull(condition.fullTextQuery());
        assertFalse(condition.hasFullTextQuery());
    }

    @Test
    void 유효한_두글자_단어가_없으면_입력_오류를_반환한다() {
        assertThrows(InvalidInputException.class, () -> condition("a b"));
    }

    private PostSearchCondition condition(String keyword) {
        return PostSearchCondition.of(
                null,
                keyword,
                PostPeriod.ALL,
                PostSort.LATEST,
                PostPeriodRange.all()
        );
    }
}

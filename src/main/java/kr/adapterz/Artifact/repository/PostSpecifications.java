package kr.adapterz.Artifact.repository;

import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.service.PostSearchCondition;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** 선택된 게시글 조회 조건만 조립하여 조건 조합별 Repository 메서드 증가를 막습니다. */
public final class PostSpecifications {
    private static final char LIKE_ESCAPE = '\\';

    private PostSpecifications() {
    }

    public static Specification<Post> matches(PostSearchCondition condition) {
        return hasCategory(condition)
                .and(titleStartsWith(condition))
                .and(createdInRange(condition));
    }

    private static Specification<Post> hasCategory(PostSearchCondition condition) {
        return (root, query, builder) -> condition.category() == null
                ? builder.conjunction()
                : builder.equal(root.get("category"), condition.category());
    }

    private static Specification<Post> titleStartsWith(PostSearchCondition condition) {
        return (root, query, builder) -> {
            if (condition.keyword() == null) return builder.conjunction();

            String pattern = escapeLike(condition.keyword().toLowerCase(Locale.ROOT)) + "%";
            return builder.like(builder.lower(root.get("title")), pattern, LIKE_ESCAPE);
        };
    }

    private static Specification<Post> createdInRange(PostSearchCondition condition) {
        return (root, query, builder) -> {
            if (condition.startAt() == null || condition.endAt() == null) {
                return builder.conjunction();
            }
            return builder.and(
                    builder.greaterThanOrEqualTo(root.get("createdAt"), condition.startAt()),
                    builder.lessThan(root.get("createdAt"), condition.endAt())
            );
        };
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}

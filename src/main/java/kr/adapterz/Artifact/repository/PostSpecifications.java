package kr.adapterz.Artifact.repository;

import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.service.PostSearchCondition;
import org.springframework.data.jpa.domain.Specification;

/** 카테고리와 기간 조건을 조합합니다. */
public final class PostSpecifications {
    private PostSpecifications() {
    }

    /** 검색어가 없는 목록의 카테고리와 기간 조건을 조합한다. */
    public static Specification<Post> matches(PostSearchCondition condition) {
        return hasCategory(condition)
                .and(createdInRange(condition));
    }

    /** 카테고리가 선택된 경우에만 일치 조건을 추가한다. */
    private static Specification<Post> hasCategory(PostSearchCondition condition) {
        return (root, query, builder) -> {
            if (condition.category() == null) {
                return builder.conjunction();
            }
            return builder.equal(root.get("category"), condition.category());
        };
    }

    /** 기간이 선택된 경우에만 시작·종료 시각 조건을 추가한다. */
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

}

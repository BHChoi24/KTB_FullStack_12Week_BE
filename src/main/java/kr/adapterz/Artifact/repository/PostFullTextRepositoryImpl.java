package kr.adapterz.Artifact.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.entity.PostSort;
import kr.adapterz.Artifact.service.PostSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/** MySQL ngram FULLTEXT 검색과 기존 목록 조건을 결합합니다. */
@Repository
public class PostFullTextRepositoryImpl implements PostFullTextRepository {
    @PersistenceContext
    private EntityManager entityManager;

    /** 본문 조회와 전체 개수 조회를 실행해 Page 결과를 만든다. */
    @Override
    public Page<Post> search(PostSearchCondition condition, Pageable pageable) {
        String whereClause = createWhereClause(condition);
        Query contentQuery = entityManager.createNativeQuery(
                "SELECT p.*" + whereClause + " ORDER BY " + sortClause(condition.sort()),
                Post.class
        );
        bindParameters(contentQuery, condition);
        contentQuery.setFirstResult(Math.toIntExact(pageable.getOffset()));
        contentQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Post> posts = contentQuery.getResultList();

        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(*)" + whereClause);
        bindParameters(countQuery, condition);
        long total = ((Number) countQuery.getSingleResult()).longValue();
        return new PageImpl<>(posts, pageable, total);
    }

    /** FULLTEXT, 카테고리, 기간 조건을 WHERE 절로 조합한다. */
    private String createWhereClause(PostSearchCondition condition) {
        StringBuilder where = new StringBuilder(
                " FROM posts p WHERE MATCH(p.title, p.content) AGAINST (:fullTextQuery IN BOOLEAN MODE)"
        );
        if (condition.category() != null) {
            where.append(" AND p.category = :category");
        }
        if (condition.startAt() != null && condition.endAt() != null) {
            where.append(" AND p.created_at >= :startAt AND p.created_at < :endAt");
        }
        return where.toString();
    }

    /** 본문 조회와 count 조회에 같은 조건 값을 바인딩한다. */
    private void bindParameters(Query query, PostSearchCondition condition) {
        query.setParameter("fullTextQuery", condition.fullTextQuery());
        if (condition.category() != null) {
            query.setParameter("category", condition.category().name());
        }
        if (condition.startAt() != null && condition.endAt() != null) {
            query.setParameter("startAt", condition.startAt());
            query.setParameter("endAt", condition.endAt());
        }
    }

    /** 기존 PostSort 규칙을 MySQL ORDER BY 절로 변환한다. */
    private String sortClause(PostSort sort) {
        // 정렬 문자열은 enum으로만 선택해 SQL 조합 범위를 제한한다.
        return switch (sort) {
            case LATEST -> "p.created_at DESC, p.post_id DESC";
            case OLDEST -> "p.created_at ASC, p.post_id ASC";
            case LIKES -> "p.likes_count DESC, p.created_at DESC, p.post_id DESC";
            case VIEWS -> "p.views_count DESC, p.created_at DESC, p.post_id DESC";
        };
    }
}

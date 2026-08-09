package kr.adapterz.Artifact.repository;

import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.service.PostSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** MySQL FULLTEXT 검색 전용 저장소입니다. */
public interface PostFullTextRepository {
    /** FULLTEXT 조건과 페이지 조건에 맞는 게시글을 조회한다. */
    Page<Post> search(PostSearchCondition condition, Pageable pageable);
}

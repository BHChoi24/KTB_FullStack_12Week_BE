package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.entity.PostCategory;
import kr.adapterz.Artifact.entity.PostPeriod;
import kr.adapterz.Artifact.entity.PostSort;

import java.time.LocalDateTime;

/** 카테고리·검색·기간·정렬을 한 요청 단위로 전달하는 조회 조건입니다. */
public record PostSearchCondition(
        PostCategory category,
        String keyword,
        PostPeriod period,
        PostSort sort,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
    public static PostSearchCondition of(
            PostCategory category,
            String keyword,
            PostPeriod period,
            PostSort sort,
            PostPeriodRange range
    ) {
        String normalizedKeyword = keyword == null || keyword.isBlank()
                ? null
                : keyword.trim();
        return new PostSearchCondition(
                category,
                normalizedKeyword,
                period,
                sort,
                range.startAt(),
                range.endAt()
        );
    }
}

package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.entity.PostCategory;
import kr.adapterz.Artifact.entity.PostPeriod;
import kr.adapterz.Artifact.entity.PostSort;
import kr.adapterz.Artifact.exception.InvalidInputException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.regex.Pattern;

import static kr.adapterz.Artifact.response.code.ValidationField.KEYWORD;
import static kr.adapterz.Artifact.response.code.ValidationMessage.KEYWORD_TOO_SHORT;

/** 카테고리·검색·기간·정렬을 한 요청 단위로 전달하는 조회 조건입니다. */
public record PostSearchCondition(
        PostCategory category,
        String fullTextQuery,
        PostPeriod period,
        PostSort sort,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
    private static final Pattern BOOLEAN_SPECIAL_CHARACTERS =
            Pattern.compile("[+\\-<>()~*\\\"@]");

    /** 요청 조건을 만들고 검색어를 Boolean 검색식으로 변환한다. */
    public static PostSearchCondition of(
            PostCategory category,
            String keyword,
            PostPeriod period,
            PostSort sort,
            PostPeriodRange range
    ) {
        String fullTextQuery = toFullTextQuery(keyword);
        return new PostSearchCondition(
                category,
                fullTextQuery,
                period,
                sort,
                range.startAt(),
                range.endAt()
        );
    }

    /** FULLTEXT 검색을 실행할 검색어가 있는지 확인한다. */
    public boolean hasFullTextQuery() {
        return fullTextQuery != null;
    }

    /** 사용자 검색어를 MySQL Boolean 필수 단어 검색식으로 변환한다. */
    private static String toFullTextQuery(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        // MySQL Boolean 연산자는 공백으로 바꿔 일반 단어로 처리한다.
        String normalized = BOOLEAN_SPECIAL_CHARACTERS.matcher(keyword).replaceAll(" ").trim();
        if (normalized.replaceAll("\\s", "").length() < 2) {
            throw new InvalidInputException(KEYWORD, KEYWORD_TOO_SHORT);
        }

        return Arrays.stream(normalized.split("\\s+"))
                // 2글자 미만 단어는 ngram 검색어에서 제외한다.
                .filter(token -> token.length() >= 2)
                .map(token -> "+" + token)
                .reduce((left, right) -> left + " " + right)
                .orElseThrow(() -> new InvalidInputException(KEYWORD, KEYWORD_TOO_SHORT));
    }
}

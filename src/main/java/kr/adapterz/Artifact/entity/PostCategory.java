package kr.adapterz.Artifact.entity;

/**
 * 게시글이 실제로 저장되는 네 가지 게시판 분류입니다.
 * "전체"는 여러 카테고리를 조회하는 화면 조건이므로 DB 값에는 포함하지 않습니다.
 */
public enum PostCategory {
    NOTICE,
    QUESTION,
    DAILY_SHARE,
    MUSIC_RECOMMENDATION
}

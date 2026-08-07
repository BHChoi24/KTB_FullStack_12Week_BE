package kr.adapterz.Artifact.service;

import java.time.LocalDateTime;

/** 시작 시각은 포함하고 종료 시각은 포함하지 않는 게시글 작성 기간입니다. */
public record PostPeriodRange(LocalDateTime startAt, LocalDateTime endAt) {
    public static PostPeriodRange all() {
        return new PostPeriodRange(null, null);
    }
}

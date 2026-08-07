package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.entity.PostPeriod;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PostPeriodRangeCalculatorTest {
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private final PostPeriodRangeCalculator calculator = new PostPeriodRangeCalculator(
            Clock.fixed(Instant.parse("2026-08-05T05:00:00Z"), KOREA)
    );

    @Test
    void 전체는_날짜범위가_없다() {
        PostPeriodRange range = calculator.calculate(PostPeriod.ALL);

        assertNull(range.startAt());
        assertNull(range.endAt());
    }

    @Test
    void 오늘과_최근7일과_최근30일은_오늘을_포함한다() {
        assertRange(PostPeriod.TODAY, "2026-08-05T00:00", "2026-08-06T00:00");
        assertRange(PostPeriod.LAST_7_DAYS, "2026-07-30T00:00", "2026-08-06T00:00");
        assertRange(PostPeriod.LAST_30_DAYS, "2026-07-07T00:00", "2026-08-06T00:00");
    }

    @Test
    void 월단위와_올해범위를_한국날짜로_계산한다() {
        assertRange(PostPeriod.LAST_3_MONTHS, "2026-05-05T00:00", "2026-08-06T00:00");
        assertRange(PostPeriod.LAST_6_MONTHS, "2026-02-05T00:00", "2026-08-06T00:00");
        assertRange(PostPeriod.THIS_YEAR, "2026-01-01T00:00", "2026-08-06T00:00");
    }

    private void assertRange(PostPeriod period, String startAt, String endAt) {
        PostPeriodRange range = calculator.calculate(period);
        assertEquals(LocalDateTime.parse(startAt), range.startAt());
        assertEquals(LocalDateTime.parse(endAt), range.endAt());
    }
}

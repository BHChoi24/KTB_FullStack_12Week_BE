package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.entity.PostPeriod;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

/** 한국 날짜 경계를 한 곳에서 계산하여 실행 날짜와 무관한 테스트를 가능하게 합니다. */
@Component
public class PostPeriodRangeCalculator {
    private final Clock clock;

    public PostPeriodRangeCalculator(Clock clock) {
        this.clock = clock;
    }

    public PostPeriodRange calculate(PostPeriod period) {
        if (period == PostPeriod.ALL) return PostPeriodRange.all();

        LocalDate today = LocalDate.now(clock);
        LocalDate startDate = switch (period) {
            case TODAY -> today;
            case LAST_7_DAYS -> today.minusDays(6);
            case LAST_30_DAYS -> today.minusDays(29);
            case LAST_3_MONTHS -> today.minusMonths(3);
            case LAST_6_MONTHS -> today.minusMonths(6);
            case THIS_YEAR -> today.withDayOfYear(1);
            case ALL -> throw new IllegalStateException("ALL period has no date range");
        };

        // 종료 경계를 내일 자정 미만으로 두면 시·분·초를 잘라내는 DB 함수를 사용하지 않아도 됩니다.
        return new PostPeriodRange(
                startDate.atStartOfDay(),
                today.plusDays(1).atTime(LocalTime.MIDNIGHT)
        );
    }
}

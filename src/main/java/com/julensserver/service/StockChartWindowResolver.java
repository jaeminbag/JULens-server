package com.julensserver.service;

import com.julensserver.dto.stock.StockPricePeriod;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 미국 동부시간의 장 운영 시간을 기준으로 차트 조회 범위를 계산한다.
 * 뉴욕 시간대를 사용하므로 미국 서머타임이 바뀌어도 한국 표시 시각이 자동 보정된다.
 */
@Component
public class StockChartWindowResolver {

    static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final LocalTime OVERNIGHT_OPEN = LocalTime.of(20, 0);
    private static final LocalTime REGULAR_OPEN = LocalTime.of(9, 30);
    private static final LocalTime SESSION_END = LocalTime.of(20, 0);

    public ChartWindow resolve(StockPricePeriod period, Instant now) {
        ZonedDateTime newYorkNow = now.atZone(NEW_YORK);
        return switch (period) {
            case REALTIME -> realtimeWindow(newYorkNow);
            case ONE_DAY -> completedSessionWindow(newYorkNow);
            case ONE_WEEK -> oneWeekWindow(newYorkNow);
            case THREE_MONTHS -> dailyWindow(newYorkNow, 3);
            case ONE_YEAR -> dailyWindow(newYorkNow, 12);
        };
    }

    private ChartWindow realtimeWindow(ZonedDateTime now) {
        LocalDate date = now.toLocalDate();
        if (isWeekend(date)) {
            return completedSessionWindow(now);
        }

        ZonedDateTime regularOpen = at(date, REGULAR_OPEN);
        ZonedDateTime sessionEnd = at(date, SESSION_END);
        if (!now.isBefore(regularOpen) && now.isBefore(sessionEnd)) {
            // 정규장 개장 순간 그래프를 초기화한다.
            return new ChartWindow(
                    regularOpen.toInstant(),
                    now.toInstant(),
                    "1Min",
                    false
            );
        }

        if (!now.isBefore(sessionEnd) && date.getDayOfWeek() != DayOfWeek.FRIDAY) {
            // 다음 거래일의 overnight 세션은 전날 20:00 ET부터 시작한다.
            return new ChartWindow(
                    sessionEnd.toInstant(),
                    now.toInstant(),
                    "1Min",
                    true
            );
        }

        if (now.isBefore(regularOpen)) {
            LocalDate previousCalendarDate = date.minusDays(1);
            return new ChartWindow(
                    at(previousCalendarDate, OVERNIGHT_OPEN).toInstant(),
                    now.toInstant(),
                    "1Min",
                    true
            );
        }

        // 금요일 장 종료 뒤와 주말에는 마지막 완결 세션을 유지한다.
        return completedSessionWindow(now);
    }

    private ChartWindow completedSessionWindow(ZonedDateTime now) {
        LocalDate completedDate = latestCompletedTradingDate(now);
        return new ChartWindow(
                at(completedDate, REGULAR_OPEN).toInstant(),
                at(completedDate, SESSION_END).toInstant(),
                "1Min",
                false
        );
    }

    private ChartWindow oneWeekWindow(ZonedDateTime now) {
        LocalDate endDate = latestCompletedTradingDate(now);
        LocalDate startDate = nextWeekday(endDate.minusWeeks(1));
        return new ChartWindow(
                at(startDate, REGULAR_OPEN).toInstant(),
                at(endDate, SESSION_END).toInstant(),
                "1Hour",
                true
        );
    }

    private ChartWindow dailyWindow(ZonedDateTime now, int months) {
        LocalDate endDate = latestCompletedTradingDate(now);
        LocalDate startDate = endDate.minusMonths(months);
        return new ChartWindow(
                startDate.atStartOfDay(NEW_YORK).toInstant(),
                endDate.plusDays(1).atStartOfDay(NEW_YORK)
                        .toInstant().minusNanos(1),
                "1Day",
                false
        );
    }

    private LocalDate latestCompletedTradingDate(ZonedDateTime now) {
        LocalDate candidate = now.toLocalDate();
        if (isWeekend(candidate)
                || now.toLocalTime().isBefore(SESSION_END)) {
            candidate = candidate.minusDays(1);
        }
        return previousOrSameWeekday(candidate);
    }

    private LocalDate previousOrSameWeekday(LocalDate date) {
        LocalDate result = date;
        while (isWeekend(result)) {
            result = result.minusDays(1);
        }
        return result;
    }

    private LocalDate nextWeekday(LocalDate date) {
        LocalDate result = date;
        while (isWeekend(result)) {
            result = result.plusDays(1);
        }
        return result;
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private ZonedDateTime at(LocalDate date, LocalTime time) {
        return date.atTime(time).atZone(NEW_YORK);
    }

    public record ChartWindow(
            Instant start,
            Instant end,
            String timeframe,
            boolean includeOvernight
    ) {
    }
}

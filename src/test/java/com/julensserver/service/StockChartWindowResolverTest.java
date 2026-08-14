package com.julensserver.service;

import com.julensserver.dto.stock.StockPricePeriod;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockChartWindowResolverTest {

    private final StockChartWindowResolver resolver =
            new StockChartWindowResolver();

    @Test
    void 정규장_전_실시간은_전날_오후_8시부터_시작한다() {
        StockChartWindowResolver.ChartWindow window = resolver.resolve(
                StockPricePeriod.REALTIME,
                Instant.parse("2026-08-14T06:00:00Z")
        );

        assertEquals(Instant.parse("2026-08-14T00:00:00Z"), window.start());
        assertTrue(window.includeOvernight());
    }

    @Test
    void 정규장_개장_뒤_실시간은_오전_9시30분부터_다시_시작한다() {
        StockChartWindowResolver.ChartWindow window = resolver.resolve(
                StockPricePeriod.REALTIME,
                Instant.parse("2026-08-14T14:00:00Z")
        );

        assertEquals(Instant.parse("2026-08-14T13:30:00Z"), window.start());
        assertFalse(window.includeOvernight());
    }

    @Test
    void 하루는_가장_최근에_끝난_정규장부터_애프터마켓까지다() {
        StockChartWindowResolver.ChartWindow window = resolver.resolve(
                StockPricePeriod.ONE_DAY,
                Instant.parse("2026-08-14T06:00:00Z")
        );

        assertEquals(Instant.parse("2026-08-13T13:30:00Z"), window.start());
        assertEquals(Instant.parse("2026-08-14T00:00:00Z"), window.end());
        assertEquals("1Min", window.timeframe());
    }

    @Test
    void 세달은_최근_완료_거래일로부터_세달_전부터_일봉이다() {
        StockChartWindowResolver.ChartWindow window = resolver.resolve(
                StockPricePeriod.THREE_MONTHS,
                Instant.parse("2026-08-14T06:00:00Z")
        );

        assertEquals(Instant.parse("2026-05-13T04:00:00Z"), window.start());
        assertEquals("1Day", window.timeframe());
    }
}

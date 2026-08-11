package com.julensserver.service;

import com.julensserver.domain.MarketSession;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsMarketSessionResolverTest {

    private static final ZoneId NEW_YORK =
            ZoneId.of("America/New_York");

    private final UsMarketSessionResolver resolver =
            new UsMarketSessionResolver();

    @Test
    void 프리마켓_시간을_판별한다() {
        assertEquals(
                MarketSession.PRE_MARKET,
                resolver.resolve(time(2026, 8, 10, 8, 0)).orElseThrow()
        );
    }

    @Test
    void 정규장_시간을_판별한다() {
        assertEquals(
                MarketSession.REGULAR_MARKET,
                resolver.resolve(time(2026, 8, 10, 10, 0)).orElseThrow()
        );
    }

    @Test
    void 애프터마켓_시간을_판별한다() {
        assertEquals(
                MarketSession.AFTER_MARKET,
                resolver.resolve(time(2026, 8, 10, 18, 0)).orElseThrow()
        );
    }

    @Test
    void 장외시간과_주말에는_분석하지_않는다() {
        assertTrue(resolver.resolve(time(2026, 8, 10, 2, 0)).isEmpty());
        assertTrue(resolver.resolve(time(2026, 8, 9, 10, 0)).isEmpty());
    }

    private ZonedDateTime time(
            int year,
            int month,
            int day,
            int hour,
            int minute
    ) {
        return ZonedDateTime.of(
                year,
                month,
                day,
                hour,
                minute,
                0,
                0,
                NEW_YORK
        );
    }
}

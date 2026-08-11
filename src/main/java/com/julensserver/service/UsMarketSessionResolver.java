package com.julensserver.service;

import com.julensserver.domain.MarketSession;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;

@Component
public class UsMarketSessionResolver {

    private static final LocalTime PRE_MARKET_OPEN = LocalTime.of(4, 0);
    private static final LocalTime REGULAR_MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime REGULAR_MARKET_CLOSE = LocalTime.of(16, 0);
    private static final LocalTime AFTER_MARKET_CLOSE = LocalTime.of(20, 0);

    public Optional<MarketSession> resolve(ZonedDateTime newYorkTime) {
        if (newYorkTime.getDayOfWeek() == DayOfWeek.SATURDAY
                || newYorkTime.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return Optional.empty();
        }

        LocalTime time = newYorkTime.toLocalTime();
        if (!time.isBefore(PRE_MARKET_OPEN)
                && time.isBefore(REGULAR_MARKET_OPEN)) {
            return Optional.of(MarketSession.PRE_MARKET);
        }
        if (!time.isBefore(REGULAR_MARKET_OPEN)
                && time.isBefore(REGULAR_MARKET_CLOSE)) {
            return Optional.of(MarketSession.REGULAR_MARKET);
        }
        if (!time.isBefore(REGULAR_MARKET_CLOSE)
                && time.isBefore(AFTER_MARKET_CLOSE)) {
            return Optional.of(MarketSession.AFTER_MARKET);
        }
        return Optional.empty();
    }
}

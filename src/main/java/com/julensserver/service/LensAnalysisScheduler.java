package com.julensserver.service;

import com.julensserver.domain.MarketSession;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class LensAnalysisScheduler {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    private final LensAnalysisService lensAnalysisService;

    @Scheduled(
            fixedRateString = "${lens.analysis.schedule-rate-ms:300000}",
            initialDelayString = "${lens.analysis.schedule-initial-delay-ms:300000}"
    )
    public void analyzeLatestStocks() {
        lensAnalysisService.runAnalysis(resolveMarketSession(Clock.system(NEW_YORK)));
    }

    MarketSession resolveMarketSession(Clock clock) {
        LocalTime now = LocalTime.now(clock);

        if (now.isBefore(LocalTime.of(9, 30))) {
            return MarketSession.PRE_MARKET;
        }
        if (now.isBefore(LocalTime.of(16, 0))) {
            return MarketSession.REGULAR_MARKET;
        }
        return MarketSession.AFTER_MARKET;
    }
}

package com.julensserver.service;

import com.julensserver.domain.MarketSession;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "lens.analysis.scheduling-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class LensAnalysisScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(LensAnalysisScheduler.class);
    private static final ZoneId NEW_YORK =
            ZoneId.of("America/New_York");

    private final LensAnalysisService lensAnalysisService;
    private final UsMarketSessionResolver marketSessionResolver;

    @Scheduled(
            cron = "${lens.analysis.schedule-cron:0 */5 * * * MON-FRI}",
            zone = "America/New_York"
    )
    public void runAnalysis() {
        Optional<MarketSession> marketSession = marketSessionResolver.resolve(
                ZonedDateTime.now(NEW_YORK)
        );
        if (marketSession.isEmpty()) {
            return;
        }

        try {
            lensAnalysisService.runAnalysis(marketSession.get());
        } catch (BusinessException exception) {
            if (exception.getErrorCode()
                    != ErrorCode.LENS_ANALYSIS_ALREADY_RUNNING) {
                logFailure(marketSession.get(), exception);
            }
        } catch (RuntimeException exception) {
            logFailure(marketSession.get(), exception);
        }
    }

    private void logFailure(
            MarketSession marketSession,
            RuntimeException exception
    ) {
        log.error(
                "Scheduled Lens analysis failed. marketSession={}",
                marketSession,
                exception
        );
    }
}

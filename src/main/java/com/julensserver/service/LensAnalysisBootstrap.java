package com.julensserver.service;

import com.julensserver.domain.MarketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 완료된 Lens 분석이 없거나 오래됐을 때 실제 분석을 한 번 생성한다.
 * 외부 API 호출이 애플리케이션 시작 완료를 막지 않도록 가상 스레드에서 실행한다.
 */
@Component
@ConditionalOnProperty(
        name = "lens.analysis.bootstrap-enabled",
        havingValue = "true"
)
public class LensAnalysisBootstrap {

    private static final Logger log =
            LoggerFactory.getLogger(LensAnalysisBootstrap.class);

    private final LensAnalysisService lensAnalysisService;
    private final MarketSession marketSession;
    private final long maxAgeHours;

    public LensAnalysisBootstrap(
            LensAnalysisService lensAnalysisService,
            @Value("${lens.analysis.bootstrap-market-session:REGULAR_MARKET}")
            MarketSession marketSession,
            @Value("${lens.analysis.bootstrap-max-age-hours:24}")
            long maxAgeHours
    ) {
        if (maxAgeHours < 1) {
            throw new IllegalArgumentException(
                    "Lens 분석 유효 시간은 1시간 이상이어야 합니다."
            );
        }
        this.lensAnalysisService = lensAnalysisService;
        this.marketSession = marketSession;
        this.maxAgeHours = maxAgeHours;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startBootstrap() {
        // Railway가 먼저 HTTP 요청을 받을 수 있도록 초기 분석은 비동기로 시작한다.
        Thread.startVirtualThread(this::runIfRequired);
    }

    void runIfRequired() {
        try {
            LocalDateTime completedAtOrAfter = LocalDateTime.now()
                    .minusHours(maxAgeHours);
            if (lensAnalysisService.hasRecentCompletedAnalysis(
                    completedAtOrAfter
            )) {
                log.info(
                        "Lens analysis bootstrap skipped. recent batch exists. completedAtOrAfter={}",
                        completedAtOrAfter
                );
                return;
            }

            log.info(
                    "Lens analysis bootstrap started. marketSession={}",
                    marketSession
            );
            lensAnalysisService.runAnalysis(marketSession);
            log.info(
                    "Lens analysis bootstrap completed. marketSession={}",
                    marketSession
            );
        } catch (RuntimeException exception) {
            // 초기 분석 실패가 서버 기동 자체를 중단시키지 않도록 로그만 남긴다.
            log.error(
                    "Lens analysis bootstrap failed. marketSession={}",
                    marketSession,
                    exception
            );
        }
    }
}

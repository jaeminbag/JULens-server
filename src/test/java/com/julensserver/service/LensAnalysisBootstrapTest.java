package com.julensserver.service;

import com.julensserver.domain.MarketSession;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LensAnalysisBootstrapTest {

    @Test
    void 최근_완료된_분석이_없으면_분석을_실행한다() {
        LensAnalysisService service = mock(LensAnalysisService.class);
        when(service.hasRecentCompletedAnalysis(any(LocalDateTime.class)))
                .thenReturn(false);
        LensAnalysisBootstrap bootstrap = new LensAnalysisBootstrap(
                service,
                MarketSession.REGULAR_MARKET,
                24
        );

        bootstrap.runIfRequired();

        verify(service).hasRecentCompletedAnalysis(any(LocalDateTime.class));
        verify(service).runAnalysis(MarketSession.REGULAR_MARKET);
    }

    @Test
    void 최근_완료된_분석이_있으면_중복_분석을_실행하지_않는다() {
        LensAnalysisService service = mock(LensAnalysisService.class);
        when(service.hasRecentCompletedAnalysis(any(LocalDateTime.class)))
                .thenReturn(true);
        LensAnalysisBootstrap bootstrap = new LensAnalysisBootstrap(
                service,
                MarketSession.REGULAR_MARKET,
                24
        );

        bootstrap.runIfRequired();

        verify(service, never()).runAnalysis(MarketSession.REGULAR_MARKET);
    }

    @Test
    void 시작_분석이_실패해도_예외를_전파하지_않는다() {
        LensAnalysisService service = mock(LensAnalysisService.class);
        when(service.hasRecentCompletedAnalysis(any(LocalDateTime.class)))
                .thenReturn(false);
        doThrow(new RuntimeException("provider failure"))
                .when(service)
                .runAnalysis(MarketSession.PRE_MARKET);
        LensAnalysisBootstrap bootstrap = new LensAnalysisBootstrap(
                service,
                MarketSession.PRE_MARKET,
                24
        );

        assertDoesNotThrow(bootstrap::runIfRequired);

        verify(service).runAnalysis(MarketSession.PRE_MARKET);
    }

    @Test
    void 분석_유효_시간은_한_시간_이상이어야_한다() {
        LensAnalysisService service = mock(LensAnalysisService.class);

        assertThrows(
                IllegalArgumentException.class,
                () -> new LensAnalysisBootstrap(
                        service,
                        MarketSession.REGULAR_MARKET,
                        0
                )
        );
    }
}

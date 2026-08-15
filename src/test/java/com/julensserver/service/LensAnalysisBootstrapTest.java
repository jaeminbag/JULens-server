package com.julensserver.service;

import com.julensserver.domain.MarketSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LensAnalysisBootstrapTest {

    @Test
    void 완료된_분석이_없으면_최초_분석을_실행한다() {
        LensAnalysisService service = mock(LensAnalysisService.class);
        when(service.hasCompletedAnalysis()).thenReturn(false);
        LensAnalysisBootstrap bootstrap = new LensAnalysisBootstrap(
                service,
                MarketSession.REGULAR_MARKET
        );

        bootstrap.runIfRequired();

        verify(service).runAnalysis(MarketSession.REGULAR_MARKET);
    }

    @Test
    void 완료된_분석이_있으면_중복_분석을_실행하지_않는다() {
        LensAnalysisService service = mock(LensAnalysisService.class);
        when(service.hasCompletedAnalysis()).thenReturn(true);
        LensAnalysisBootstrap bootstrap = new LensAnalysisBootstrap(
                service,
                MarketSession.REGULAR_MARKET
        );

        bootstrap.runIfRequired();

        verify(service, never()).runAnalysis(MarketSession.REGULAR_MARKET);
    }

    @Test
    void 최초_분석이_실패해도_예외를_전파하지_않는다() {
        LensAnalysisService service = mock(LensAnalysisService.class);
        when(service.hasCompletedAnalysis()).thenReturn(false);
        doThrow(new RuntimeException("provider failure"))
                .when(service)
                .runAnalysis(MarketSession.PRE_MARKET);
        LensAnalysisBootstrap bootstrap = new LensAnalysisBootstrap(
                service,
                MarketSession.PRE_MARKET
        );

        assertDoesNotThrow(bootstrap::runIfRequired);

        verify(service).runAnalysis(MarketSession.PRE_MARKET);
    }
}

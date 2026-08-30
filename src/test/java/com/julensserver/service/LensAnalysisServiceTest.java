package com.julensserver.service;

import com.julensserver.domain.Currency;
import com.julensserver.domain.Exchange;
import com.julensserver.domain.LensAnalysisBatch;
import com.julensserver.domain.LensBatchStatus;
import com.julensserver.domain.LensLabel;
import com.julensserver.domain.MarketSession;
import com.julensserver.domain.Stock;
import com.julensserver.dto.lens.LensAnalysisResult;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.LensAnalysisBatchRepository;
import com.julensserver.repository.LensAnalysisRepository;
import com.julensserver.repository.StockRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LensAnalysisServiceTest {

    @Test
    void 기준_시각_이후에_완료된_분석_배치가_있는지_확인한다() {
        TestFixture fixture = new TestFixture();
        LocalDateTime completedAtOrAfter =
                LocalDateTime.of(2026, 8, 29, 12, 0);
        when(fixture.batchRepository
                .existsByStatusAndCompletedAtGreaterThanEqual(
                        LensBatchStatus.COMPLETED,
                        completedAtOrAfter
                )).thenReturn(true);

        boolean recentCompletedAnalysisExists = fixture.service
                .hasRecentCompletedAnalysis(completedAtOrAfter);

        assertTrue(recentCompletedAnalysisExists);
    }

    @Test
    void 한_종목이_실패해도_성공한_종목으로_배치를_완료한다() {
        TestFixture fixture = new TestFixture();
        Stock failedStock = fixture.stock("FAIL");
        Stock succeededStock = fixture.stock("PASS");
        LensAnalysisCandidate candidate = new LensAnalysisCandidate(
                succeededStock,
                new LensAnalysisResult(
                        new BigDecimal("100.00"),
                        new BigDecimal("1.00"),
                        1_000_000L,
                        new BigDecimal("100000000.00"),
                        10,
                        10,
                        10,
                        0,
                        30,
                        LensLabel.WATCH
                ),
                List.of()
        );

        when(fixture.mostActiveStockProvider.getMostActiveTickers())
                .thenReturn(List.of("FAIL", "PASS"));
        when(fixture.stockRepository.findAllByTickerIn(
                List.of("FAIL", "PASS")
        ))
                .thenReturn(List.of(failedStock, succeededStock));
        when(fixture.lensStockAnalyzer.analyze(failedStock))
                .thenThrow(new RuntimeException("provider failure"));
        when(fixture.lensStockAnalyzer.analyze(succeededStock))
                .thenReturn(candidate);

        fixture.service.runAnalysis(MarketSession.REGULAR_MARKET);

        verify(fixture.persistenceService).completeBatch(
                1L,
                List.of(candidate)
        );
        verify(fixture.persistenceService, never()).failBatch(1L);
    }

    @Test
    void 모든_종목이_실패하면_배치를_실패처리한다() {
        TestFixture fixture = new TestFixture();
        Stock failedStock = fixture.stock("FAIL");
        when(fixture.mostActiveStockProvider.getMostActiveTickers())
                .thenReturn(List.of("FAIL"));
        when(fixture.stockRepository.findAllByTickerIn(
                List.of("FAIL")
        ))
                .thenReturn(List.of(failedStock));
        when(fixture.lensStockAnalyzer.analyze(failedStock))
                .thenThrow(new RuntimeException("provider failure"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.runAnalysis(
                        MarketSession.REGULAR_MARKET
                )
        );

        assertEquals(
                ErrorCode.EXTERNAL_DATA_PROVIDER_ERROR,
                exception.getErrorCode()
        );
        verify(fixture.persistenceService).failBatch(1L);
    }

    @Test
    void 거래량_상위종목이_DB에_없으면_빈_완료배치를_만들지_않는다() {
        TestFixture fixture = new TestFixture();
        when(fixture.mostActiveStockProvider.getMostActiveTickers())
                .thenReturn(List.of("MISSING"));
        when(fixture.stockRepository.findAllByTickerIn(
                List.of("MISSING")
        ))
                .thenReturn(List.of());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.runAnalysis(
                        MarketSession.REGULAR_MARKET
                )
        );

        assertEquals(
                ErrorCode.ACTIVE_STOCK_NOT_FOUND,
                exception.getErrorCode()
        );
        verify(fixture.persistenceService).failBatch(1L);
    }

    @Test
    void 거래량_상위종목_응답이_비어있으면_배치를_실패처리한다() {
        TestFixture fixture = new TestFixture();
        when(fixture.mostActiveStockProvider.getMostActiveTickers())
                .thenReturn(List.of());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.runAnalysis(
                        MarketSession.REGULAR_MARKET
                )
        );

        assertEquals(
                ErrorCode.EXTERNAL_DATA_PROVIDER_ERROR,
                exception.getErrorCode()
        );
        verify(fixture.persistenceService).failBatch(1L);
        verify(fixture.stockRepository, never())
                .findAllByTickerIn(List.of());
    }

    private static class TestFixture {
        private final LensAnalysisBatchRepository batchRepository =
                mock(LensAnalysisBatchRepository.class);
        private final LensAnalysisRepository analysisRepository =
                mock(LensAnalysisRepository.class);
        private final StockRepository stockRepository =
                mock(StockRepository.class);
        private final MostActiveStockProvider mostActiveStockProvider =
                mock(MostActiveStockProvider.class);
        private final LensStockAnalyzer lensStockAnalyzer =
                mock(LensStockAnalyzer.class);
        private final LensAnalysisPersistenceService persistenceService =
                mock(LensAnalysisPersistenceService.class);
        private final LensAnalysisService service;

        private TestFixture() {
            LensAnalysisBatch batch = mock(LensAnalysisBatch.class);
            when(batch.getId()).thenReturn(1L);
            when(batchRepository.existsByStatus(LensBatchStatus.RUNNING))
                    .thenReturn(false);
            when(persistenceService.startBatch(
                    MarketSession.REGULAR_MARKET
            )).thenReturn(batch);

            service = new LensAnalysisService(
                    batchRepository,
                    analysisRepository,
                    stockRepository,
                    mostActiveStockProvider,
                    lensStockAnalyzer,
                    persistenceService
            );
        }

        private Stock stock(String ticker) {
            Stock stock = new Stock(
                    ticker,
                    ticker + " Company",
                    ticker + " 회사",
                    Exchange.NASDAQ,
                    Currency.USD,
                    null
            );
            stock.activate();
            return stock;
        }
    }
}

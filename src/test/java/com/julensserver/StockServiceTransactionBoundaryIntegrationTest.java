package com.julensserver;

import com.julensserver.domain.Currency;
import com.julensserver.domain.Exchange;
import com.julensserver.domain.Stock;
import com.julensserver.dto.stock.StockPriceHistoryResponse;
import com.julensserver.dto.stock.StockPricePeriod;
import com.julensserver.repository.StockRepository;
import com.julensserver.service.RealtimeStockPriceFeed;
import com.julensserver.service.RealtimeStockPriceService;
import com.julensserver.service.StockPriceHistoryProvider;
import com.julensserver.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles({"test", "mock"})
class StockServiceTransactionBoundaryIntegrationTest {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockService stockService;

    @Autowired
    private RealtimeStockPriceService realtimeStockPriceService;

    @MockitoBean
    private StockPriceHistoryProvider stockPriceHistoryProvider;

    @MockitoBean
    private RealtimeStockPriceFeed realtimeStockPriceFeed;

    @Test
    void 가격이력_외부_API는_DB_트랜잭션_밖에서_호출한다() {
        Stock stock = new Stock(
                "TXIO",
                "Transaction Boundary Test",
                "트랜잭션 경계 테스트",
                Exchange.NASDAQ,
                Currency.USD,
                "Technology"
        );
        stock.activate();
        stockRepository.save(stock);

        when(stockPriceHistoryProvider.getPriceHistory(anyString(), any()))
                .thenAnswer(invocation -> {
                    assertFalse(TransactionSynchronizationManager
                            .isActualTransactionActive());
                    return new StockPriceHistoryResponse(
                            "TXIO",
                            StockPricePeriod.REALTIME,
                            null,
                            null,
                            null,
                            List.of()
                    );
                });
        when(stockPriceHistoryProvider.getPriceHistory(anyString()))
                .thenAnswer(invocation -> {
                    assertFalse(TransactionSynchronizationManager
                            .isActualTransactionActive());
                    return List.of();
                });

        stockService.getPriceHistories(
                List.of("TXIO"),
                StockPricePeriod.REALTIME
        );
        stockService.getStockDetail("TXIO");
    }

    @Test
    void 실시간_피드_구독은_DB_트랜잭션_밖에서_호출한다() {
        Stock stock = new Stock(
                "RTIO",
                "Realtime Boundary Test",
                "실시간 경계 테스트",
                Exchange.NASDAQ,
                Currency.USD,
                "Technology"
        );
        stock.activate();
        stockRepository.save(stock);

        doAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager
                    .isActualTransactionActive());
            return null;
        }).when(realtimeStockPriceFeed).subscribe(anySet());

        RealtimeStockPriceService.Subscription subscription =
                realtimeStockPriceService.subscribe(
                        List.of("RTIO"),
                        ignored -> { }
                );
        subscription.close();
    }
}

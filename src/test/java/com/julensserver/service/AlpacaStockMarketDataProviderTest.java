package com.julensserver.service;

import com.julensserver.domain.Currency;
import com.julensserver.domain.Exchange;
import com.julensserver.domain.Stock;
import com.julensserver.dto.lens.StockMarketData;
import com.julensserver.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlpacaStockMarketDataProviderTest {

    @Test
    void 오늘_분봉을_조회할_수_없으면_최근_거래일_분봉을_사용한다() {
        AlpacaClient client = mock(AlpacaClient.class);
        Stock stock = new Stock(
                "AAPL",
                "Apple Inc.",
                "애플",
                Exchange.NASDAQ,
                Currency.USD,
                null
        );
        AlpacaClient.AlpacaBarsResponse dailyResponse =
                new AlpacaClient.AlpacaBarsResponse(
                        List.of(
                                bar("100", "1000", "2026-08-13T04:00:00Z"),
                                bar("110", "3000", "2026-08-14T04:00:00Z")
                        ),
                        "AAPL",
                        null
                );
        AlpacaClient.AlpacaBarsResponse minuteResponse =
                new AlpacaClient.AlpacaBarsResponse(
                        List.of(
                                bar("105", "100", "2026-08-14T13:30:00Z"),
                                bar("110", "200", "2026-08-14T19:59:00Z")
                        ),
                        "AAPL",
                        null
                );
        when(client.getDailyBars("AAPL", 45)).thenReturn(dailyResponse);
        when(client.isTodayDelayedDataAvailable()).thenReturn(false);
        when(client.getMinuteBarsForTradingDate(
                "AAPL",
                LocalDate.of(2026, 8, 14)
        )).thenReturn(minuteResponse);
        AlpacaStockMarketDataProvider provider =
                new AlpacaStockMarketDataProvider(client);

        StockMarketData result = provider.getMarketData(stock);

        assertEquals(new BigDecimal("110"), result.currentPrice());
        assertEquals(new BigDecimal("10.0000"), result.changeRate());
        assertEquals(300L, result.volume());
        assertEquals(1_000L, result.averageVolume20d());
        verify(client).getMinuteBarsForTradingDate(
                "AAPL",
                LocalDate.of(2026, 8, 14)
        );
    }

    @Test
    void 전일종가와_오늘분봉으로_현재시세를_계산한다() {
        AlpacaClient.AlpacaBarsResponse dailyResponse =
                new AlpacaClient.AlpacaBarsResponse(
                        List.of(
                                bar("100", "1000", "2026-08-10T04:00:00Z"),
                                bar("105", "3000", "2026-08-11T04:00:00Z")
                        ),
                        "AAPL",
                        null
                );
        AlpacaClient.AlpacaBarsResponse minuteResponse =
                new AlpacaClient.AlpacaBarsResponse(
                        List.of(
                                bar("108", "2000", "2026-08-12T08:00:00Z"),
                                bar("110", "4000", "2026-08-12T08:01:00Z")
                        ),
                        "AAPL",
                        null
                );

        StockMarketData result =
                AlpacaStockMarketDataProvider.toMarketData(
                        dailyResponse,
                        minuteResponse
                );

        assertEquals(new BigDecimal("110"), result.currentPrice());
        assertEquals(new BigDecimal("4.7619"), result.changeRate());
        assertEquals(6_000L, result.volume());
        assertEquals(2_000L, result.averageVolume20d());
        assertEquals(new BigDecimal("660000.00"), result.tradingValue());
    }

    @Test
    void 데이터가_없으면_외부데이터_오류를_던진다() {
        AlpacaClient.AlpacaBarsResponse emptyResponse =
                new AlpacaClient.AlpacaBarsResponse(
                        List.of(),
                        "AAPL",
                        null
                );
        AlpacaClient.AlpacaBarsResponse minuteResponse =
                new AlpacaClient.AlpacaBarsResponse(
                        List.of(bar(
                                "110",
                                "1000",
                                "2026-08-12T08:00:00Z"
                        )),
                        "AAPL",
                        null
                );

        assertThrows(
                BusinessException.class,
                () -> AlpacaStockMarketDataProvider.toMarketData(
                        emptyResponse,
                        minuteResponse
                )
        );
    }

    @Test
    void 오늘_분봉이_없어도_외부데이터_오류를_던진다() {
        AlpacaClient.AlpacaBarsResponse dailyResponse =
                new AlpacaClient.AlpacaBarsResponse(
                        List.of(bar(
                                "105",
                                "3000",
                                "2026-08-11T04:00:00Z"
                        )),
                        "AAPL",
                        null
                );
        AlpacaClient.AlpacaBarsResponse emptyMinuteResponse =
                new AlpacaClient.AlpacaBarsResponse(
                        List.of(),
                        "AAPL",
                        null
                );

        assertThrows(
                BusinessException.class,
                () -> AlpacaStockMarketDataProvider.toMarketData(
                        dailyResponse,
                        emptyMinuteResponse
                )
        );
    }

    private AlpacaClient.AlpacaBar bar(
            String close,
            String volume,
            String timestamp
    ) {
        return new AlpacaClient.AlpacaBar(
                new BigDecimal(close),
                new BigDecimal(volume),
                Instant.parse(timestamp)
        );
    }
}

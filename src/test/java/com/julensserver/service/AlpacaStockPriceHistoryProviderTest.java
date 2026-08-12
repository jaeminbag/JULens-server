package com.julensserver.service;

import com.julensserver.domain.Currency;
import com.julensserver.domain.Exchange;
import com.julensserver.domain.Stock;
import com.julensserver.dto.stock.StockPricePointResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlpacaStockPriceHistoryProviderTest {

    @Test
    void 장중에는_분봉을_가격순서대로_변환한다() {
        AlpacaClient client = mock(AlpacaClient.class);
        when(client.getTodayMinuteBars("MU")).thenReturn(response(
                bar("2026-08-12T08:00:00Z", "120.10"),
                bar("2026-08-12T08:01:00Z", "121.20")
        ));
        AlpacaStockPriceHistoryProvider provider =
                new AlpacaStockPriceHistoryProvider(client);

        List<StockPricePointResponse> result =
                provider.getPriceHistory(stock());

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("120.10"), result.getFirst().price());
        assertEquals(new BigDecimal("121.20"), result.getLast().price());
    }

    @Test
    void 오늘_분봉이_없으면_최근_일봉을_사용한다() {
        AlpacaClient client = mock(AlpacaClient.class);
        when(client.getTodayMinuteBars("MU"))
                .thenReturn(response());
        when(client.getDailyBars("MU", 45)).thenReturn(response(
                bar("2026-08-10T04:00:00Z", "118.00"),
                bar("2026-08-11T04:00:00Z", "120.00")
        ));
        AlpacaStockPriceHistoryProvider provider =
                new AlpacaStockPriceHistoryProvider(client);

        List<StockPricePointResponse> result =
                provider.getPriceHistory(stock());

        assertEquals(2, result.size());
        verify(client).getDailyBars("MU", 45);
    }

    private Stock stock() {
        return new Stock(
                "MU",
                "Micron Technology",
                "마이크론 테크놀로지",
                Exchange.NASDAQ,
                Currency.USD,
                "Technology"
        );
    }

    private AlpacaClient.AlpacaBarsResponse response(
            AlpacaClient.AlpacaBar... bars
    ) {
        return new AlpacaClient.AlpacaBarsResponse(
                List.of(bars),
                "MU",
                null
        );
    }

    private AlpacaClient.AlpacaBar bar(String timestamp, String price) {
        return new AlpacaClient.AlpacaBar(
                new BigDecimal(price),
                BigDecimal.ONE,
                Instant.parse(timestamp)
        );
    }
}

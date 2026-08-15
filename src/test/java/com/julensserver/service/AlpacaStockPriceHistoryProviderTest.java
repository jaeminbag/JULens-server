package com.julensserver.service;

import com.julensserver.dto.stock.StockPriceHistoryResponse;
import com.julensserver.dto.stock.StockPricePeriod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlpacaStockPriceHistoryProviderTest {

    @Test
    void 실시간_구간은_SIP와_BOATS_가격을_시간순으로_합친다() {
        AlpacaClient client = mock(AlpacaClient.class);
        StockChartWindowResolver resolver = mock(
                StockChartWindowResolver.class
        );
        when(client.getDataDelayMinutes()).thenReturn(16);
        when(resolver.resolve(any(), any())).thenReturn(
                new StockChartWindowResolver.ChartWindow(
                        Instant.parse("2026-08-14T00:00:00Z"),
                        Instant.parse("2026-08-14T12:00:00Z"),
                        "1Min",
                        true
                )
        );
        when(client.getHistoricalBars(
                anyString(), anyString(), any(), any(),
                anyString(), anyInt()
        )).thenAnswer(invocation -> {
            String feed = invocation.getArgument(4);
            if ("boats".equals(feed)) {
                return response(
                        bar("2026-08-14T00:01:00Z", "120.10")
                );
            }
            return response(
                    bar("2026-08-14T08:01:00Z", "121.20")
            );
        });
        AlpacaStockPriceHistoryProvider provider =
                new AlpacaStockPriceHistoryProvider(client, resolver);

        StockPriceHistoryResponse result = provider.getPriceHistory(
                "MU",
                StockPricePeriod.REALTIME
        );

        assertEquals(2, result.points().size());
        assertEquals(
                new BigDecimal("120.10"),
                result.points().getFirst().price()
        );
        assertEquals(
                new BigDecimal("121.20"),
                result.points().getLast().price()
        );
    }

    @Test
    void 한_피드가_실패해도_다른_피드의_가격은_반환한다() {
        AlpacaClient client = mock(AlpacaClient.class);
        StockChartWindowResolver resolver = mock(
                StockChartWindowResolver.class
        );
        when(client.getDataDelayMinutes()).thenReturn(16);
        when(resolver.resolve(any(), any())).thenReturn(
                new StockChartWindowResolver.ChartWindow(
                        Instant.parse("2026-08-14T00:00:00Z"),
                        Instant.parse("2026-08-14T12:00:00Z"),
                        "1Min",
                        true
                )
        );
        when(client.getHistoricalBars(
                anyString(), anyString(), any(), any(),
                anyString(), anyInt()
        )).thenAnswer(invocation -> {
            if ("boats".equals(invocation.getArgument(4))) {
                throw new IllegalStateException("BOATS unavailable");
            }
            return response(bar("2026-08-14T08:01:00Z", "121.20"));
        });
        AlpacaStockPriceHistoryProvider provider =
                new AlpacaStockPriceHistoryProvider(client, resolver);

        StockPriceHistoryResponse result = provider.getPriceHistory(
                "MU",
                StockPricePeriod.REALTIME
        );

        assertEquals(1, result.points().size());
        assertEquals(
                new BigDecimal("121.20"),
                result.points().getFirst().price()
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

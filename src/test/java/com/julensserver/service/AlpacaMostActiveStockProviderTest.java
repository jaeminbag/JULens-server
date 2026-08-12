package com.julensserver.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlpacaMostActiveStockProviderTest {

    @Test
    void 거래량순서를_유지하며_티커를_정규화하고_중복을_제거한다() {
        AlpacaClient client = mock(AlpacaClient.class);
        when(client.getMostActiveStocks(100)).thenReturn(
                new AlpacaClient.AlpacaMostActivesResponse(
                        List.of(
                                activeStock(" chow "),
                                activeStock("OFA"),
                                activeStock("CHOW")
                        ),
                        Instant.parse("2026-08-12T12:00:00Z")
                )
        );
        AlpacaMostActiveStockProvider provider =
                new AlpacaMostActiveStockProvider(client, 100);

        List<String> result = provider.getMostActiveTickers();

        assertEquals(List.of("CHOW", "OFA"), result);
        verify(client).getMostActiveStocks(100);
    }

    private AlpacaClient.AlpacaActiveStock activeStock(String symbol) {
        return new AlpacaClient.AlpacaActiveStock(
                symbol,
                BigDecimal.ONE,
                BigDecimal.ONE
        );
    }
}

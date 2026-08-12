package com.julensserver.service;

import com.julensserver.dto.lens.StockMarketData;
import com.julensserver.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlpacaStockMarketDataProviderTest {

    @Test
    void 일봉에서_현재가_등락률_평균거래량을_계산한다() {
        AlpacaClient.AlpacaBarsResponse response =
                new AlpacaClient.AlpacaBarsResponse(
                        List.of(
                                bar("100", "1000", "2026-08-10T04:00:00Z"),
                                bar("105", "3000", "2026-08-11T04:00:00Z"),
                                bar("110", "6000", "2026-08-12T04:00:00Z")
                        ),
                        "AAPL",
                        null
                );

        StockMarketData result =
                AlpacaStockMarketDataProvider.toMarketData(response);

        assertEquals(new BigDecimal("110"), result.currentPrice());
        assertEquals(new BigDecimal("4.7619"), result.changeRate());
        assertEquals(6_000L, result.volume());
        assertEquals(2_000L, result.averageVolume20d());
        assertEquals(new BigDecimal("660000.00"), result.tradingValue());
    }

    @Test
    void 데이터가_없으면_외부데이터_오류를_던진다() {
        AlpacaClient.AlpacaBarsResponse response =
                new AlpacaClient.AlpacaBarsResponse(
                        List.of(),
                        "AAPL",
                        null
                );

        assertThrows(
                BusinessException.class,
                () -> AlpacaStockMarketDataProvider.toMarketData(response)
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

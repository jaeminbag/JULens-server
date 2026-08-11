package com.julensserver.service;

import com.julensserver.dto.lens.StockMarketData;
import com.julensserver.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinnhubStockMarketDataProviderTest {

    @Test
    void 일봉에서_현재가_등락률_평균거래량을_계산한다() {
        FinnhubClient.FinnhubCandleResponse response =
                new FinnhubClient.FinnhubCandleResponse(
                        List.of(
                                new BigDecimal("100"),
                                new BigDecimal("105"),
                                new BigDecimal("110")
                        ),
                        List.of(
                                new BigDecimal("1000"),
                                new BigDecimal("3000"),
                                new BigDecimal("6000")
                        ),
                        "ok",
                        null
                );

        StockMarketData result =
                FinnhubStockMarketDataProvider.toMarketData(response);

        assertEquals(new BigDecimal("110"), result.currentPrice());
        assertEquals(new BigDecimal("4.7619"), result.changeRate());
        assertEquals(6_000L, result.volume());
        assertEquals(2_000L, result.averageVolume20d());
        assertEquals(new BigDecimal("660000.00"), result.tradingValue());
    }

    @Test
    void 데이터가_없으면_외부데이터_오류를_던진다() {
        FinnhubClient.FinnhubCandleResponse response =
                new FinnhubClient.FinnhubCandleResponse(
                        List.of(),
                        List.of(),
                        "no_data",
                        null
                );

        assertThrows(
                BusinessException.class,
                () -> FinnhubStockMarketDataProvider.toMarketData(response)
        );
    }
}

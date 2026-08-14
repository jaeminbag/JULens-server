package com.julensserver.service;

import com.julensserver.dto.stock.StockPricePointResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
@Profile("mock")
public class MockStockPriceHistoryProvider
        implements StockPriceHistoryProvider {

    @Override
    public List<StockPricePointResponse> getPriceHistory(String ticker) {
        return List.of(
                point("2026-08-12T08:00:00Z", "24.10"),
                point("2026-08-12T10:00:00Z", "25.20"),
                point("2026-08-12T12:00:00Z", "25.50")
        );
    }

    private StockPricePointResponse point(String time, String price) {
        return new StockPricePointResponse(
                Instant.parse(time),
                new BigDecimal(price)
        );
    }
}

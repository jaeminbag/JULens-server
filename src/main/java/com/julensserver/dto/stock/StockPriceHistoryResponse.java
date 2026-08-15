package com.julensserver.dto.stock;

import java.time.Instant;
import java.util.List;

public record StockPriceHistoryResponse(
        String ticker,
        StockPricePeriod period,
        Instant windowStart,
        Instant windowEnd,
        String interval,
        List<StockPricePointResponse> points
) {
}

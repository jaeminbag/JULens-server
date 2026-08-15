package com.julensserver.dto.stock;

import com.julensserver.service.RealtimeStockPrice;

import java.math.BigDecimal;
import java.time.Instant;

public record RealtimeStockPriceResponse(
        String ticker,
        BigDecimal price,
        BigDecimal tradeSize,
        Instant timestamp,
        String feed
) {
    public static RealtimeStockPriceResponse from(
            RealtimeStockPrice stockPrice
    ) {
        return new RealtimeStockPriceResponse(
                stockPrice.ticker(),
                stockPrice.price(),
                stockPrice.tradeSize(),
                stockPrice.timestamp(),
                stockPrice.feed()
        );
    }
}

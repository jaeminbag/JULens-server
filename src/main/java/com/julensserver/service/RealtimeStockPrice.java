package com.julensserver.service;

import java.math.BigDecimal;
import java.time.Instant;

public record RealtimeStockPrice(
        String ticker,
        BigDecimal price,
        BigDecimal tradeSize,
        Instant timestamp
) {
}

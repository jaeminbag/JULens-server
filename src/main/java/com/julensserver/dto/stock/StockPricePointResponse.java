package com.julensserver.dto.stock;

import java.math.BigDecimal;
import java.time.Instant;

public record StockPricePointResponse(
        Instant timestamp,
        BigDecimal price
) {
}

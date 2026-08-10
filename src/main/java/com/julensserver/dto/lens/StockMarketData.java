package com.julensserver.dto.lens;

import java.math.BigDecimal;

public record StockMarketData(
        BigDecimal currentPrice,
        BigDecimal changeRate,
        Long volume,
        BigDecimal tradingValue
) {
}
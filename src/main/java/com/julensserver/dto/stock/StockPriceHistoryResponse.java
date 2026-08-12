package com.julensserver.dto.stock;

import java.util.List;

public record StockPriceHistoryResponse(
        String ticker,
        List<StockPricePointResponse> points
) {
}

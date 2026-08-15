package com.julensserver.service;

import com.julensserver.dto.stock.StockPriceHistoryResponse;
import com.julensserver.dto.stock.StockPricePointResponse;
import com.julensserver.dto.stock.StockPricePeriod;

import java.util.List;

public interface StockPriceHistoryProvider {

    StockPriceHistoryResponse getPriceHistory(
            String ticker,
            StockPricePeriod period
    );

    default List<StockPricePointResponse> getPriceHistory(String ticker) {
        return getPriceHistory(ticker, StockPricePeriod.REALTIME).points();
    }
}

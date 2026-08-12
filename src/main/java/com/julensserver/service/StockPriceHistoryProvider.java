package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.dto.stock.StockPricePointResponse;

import java.util.List;

public interface StockPriceHistoryProvider {

    List<StockPricePointResponse> getPriceHistory(Stock stock);
}

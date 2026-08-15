package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.dto.lens.StockMarketData;


public interface StockMarketDataProvider {


    StockMarketData getMarketData(Stock stock);
}
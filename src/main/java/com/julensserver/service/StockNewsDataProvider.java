package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.dto.lens.StockNewsData;

import java.util.List;


public interface StockNewsDataProvider {

    List<StockNewsData> getNews(Stock stock);
}
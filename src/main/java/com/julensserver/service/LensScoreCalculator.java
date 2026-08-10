package com.julensserver.service;

import com.julensserver.dto.lens.LensAnalysisResult;
import com.julensserver.dto.lens.StockMarketData;
import com.julensserver.dto.lens.StockNewsData;

import java.util.List;


public interface LensScoreCalculator {

    LensAnalysisResult calculate(
            StockMarketData marketData,
            List<StockNewsData> newsList
    );
}
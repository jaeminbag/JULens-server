package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.dto.lens.LensAnalysisResult;
import com.julensserver.dto.lens.StockMarketData;
import com.julensserver.dto.lens.StockNewsData;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class DefaultLensStockAnalyzer implements LensStockAnalyzer {

    private final StockMarketDataProvider stockMarketDataProvider;
    private final StockNewsDataProvider stockNewsDataProvider;
    private final LensScoreCalculator lensScoreCalculator;

    public DefaultLensStockAnalyzer(
            StockMarketDataProvider stockMarketDataProvider,
            StockNewsDataProvider stockNewsDataProvider,
            LensScoreCalculator lensScoreCalculator
    ) {
        this.stockMarketDataProvider = stockMarketDataProvider;
        this.stockNewsDataProvider = stockNewsDataProvider;
        this.lensScoreCalculator = lensScoreCalculator;
    }

    @Override
    public LensAnalysisCandidate analyze(Stock stock) {
        Objects.requireNonNull(
                stock,
                "분석할 종목은 null일 수 없습니다."
        );

        StockMarketData marketData =
                stockMarketDataProvider.getMarketData(stock);
        List<StockNewsData> news =
                stockNewsDataProvider.getNews(stock);
        LensAnalysisResult result =
                lensScoreCalculator.calculate(marketData, news);

        return new LensAnalysisCandidate(stock, result, news);
    }
}

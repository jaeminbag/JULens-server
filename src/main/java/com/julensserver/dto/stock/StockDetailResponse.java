package com.julensserver.dto.stock;

import com.julensserver.domain.LensAnalysis;
import com.julensserver.domain.Stock;
import com.julensserver.domain.StockNews;
import com.julensserver.dto.lens.LensAnalysisResponse;

import java.util.List;

public record StockDetailResponse(
        StockResponse stock,
        LensAnalysisResponse latestAnalysis,
        List<StockNewsResponse> news
) {
    public static StockDetailResponse from(
            Stock stock,
            LensAnalysis latestAnalysis,
            List<StockNews> news
    ) {
        return new StockDetailResponse(
                StockResponse.from(stock),
                latestAnalysis == null
                        ? null
                        : LensAnalysisResponse.from(latestAnalysis),
                news.stream()
                        .map(StockNewsResponse::from)
                        .toList()
        );
    }
}

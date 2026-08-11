package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.dto.lens.LensAnalysisResult;
import com.julensserver.dto.lens.StockNewsData;

import java.util.List;

public record LensAnalysisCandidate(
        Stock stock,
        LensAnalysisResult result,
        List<StockNewsData> news
) {
    public LensAnalysisCandidate {
        news = news == null ? List.of() : List.copyOf(news);
    }
}

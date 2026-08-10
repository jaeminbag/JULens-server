package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.dto.lens.LensAnalysisResult;

public interface LensStockAnalyzer {

    LensAnalysisResult analyze(Stock stock);
}
package com.julensserver.service;

import com.julensserver.domain.Stock;

public interface LensStockAnalyzer {

    LensAnalysisCandidate analyze(Stock stock);
}

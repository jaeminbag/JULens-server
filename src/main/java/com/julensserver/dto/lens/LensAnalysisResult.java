package com.julensserver.dto.lens;

import com.julensserver.domain.LensAnalysis;
import com.julensserver.domain.LensAnalysisBatch;
import com.julensserver.domain.LensLabel;
import com.julensserver.domain.Stock;

import java.math.BigDecimal;

public record LensAnalysisResult(
        BigDecimal currentPrice,
        BigDecimal changeRate,
        Long volume,
        BigDecimal tradingValue,
        Integer newsScore,
        Integer movementScore,
        Integer volumeScore,
        Integer riskScore,
        Integer totalScore,
        LensLabel label
) {

    public LensAnalysis toEntity(
            LensAnalysisBatch batch,
            Stock stock
    ) {
        return LensAnalysis.create(
                batch,
                stock,
                currentPrice,
                changeRate,
                volume,
                tradingValue,
                newsScore,
                movementScore,
                volumeScore,
                riskScore,
                totalScore,
                label
        );
    }
}
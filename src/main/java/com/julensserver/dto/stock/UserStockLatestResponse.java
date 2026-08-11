package com.julensserver.dto.stock;

import com.julensserver.domain.LensAnalysis;
import com.julensserver.domain.UserStock;
import com.julensserver.dto.lens.LensAnalysisResponse;

public record UserStockLatestResponse(
        Long userStockId,
        StockResponse stock,
        LensAnalysisResponse latestAnalysis
) {
    public static UserStockLatestResponse from(
            UserStock userStock,
            LensAnalysis latestAnalysis
    ) {
        return new UserStockLatestResponse(
                userStock.getId(),
                StockResponse.from(userStock.getStock()),
                latestAnalysis == null
                        ? null
                        : LensAnalysisResponse.from(latestAnalysis)
        );
    }
}

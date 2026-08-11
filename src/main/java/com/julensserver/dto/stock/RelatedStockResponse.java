package com.julensserver.dto.stock;

import com.julensserver.domain.Stock;

public record RelatedStockResponse(
        Long stockId,
        String ticker,
        String companyName,
        String companyNameKr
) {
    public static RelatedStockResponse from(Stock stock) {
        return new RelatedStockResponse(
                stock.getId(),
                stock.getTicker(),
                stock.getCompanyName(),
                stock.getCompanyNameKr()
        );
    }
}

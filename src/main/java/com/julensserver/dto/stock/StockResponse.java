package com.julensserver.dto.stock;

import com.julensserver.domain.Currency;
import com.julensserver.domain.Exchange;
import com.julensserver.domain.Stock;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StockResponse {
    private Long id;

    private String ticker;

    private String companyName;

    private String companyNameKr;

    private Exchange exchange;

    private Currency currency;

    private String sector;

    private boolean active;

    public static StockResponse from(Stock stock){
        return new StockResponse(
                stock.getId(),
                stock.getTicker(),
                stock.getCompanyName(),
                stock.getCompanyNameKr(),
                stock.getExchange(),
                stock.getCurrency(),
                stock.getSector(),
                stock.isActive()
        );
    }

}

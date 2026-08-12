package com.julensserver.controller;

import com.julensserver.dto.common.ApiResponse;
import com.julensserver.dto.stock.StockDetailResponse;
import com.julensserver.dto.stock.StockPriceHistoryResponse;
import com.julensserver.dto.stock.StockResponse;
import com.julensserver.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stocks")
public class StockController {
    private final StockService stockService;

    @GetMapping("/{ticker}")
    public ApiResponse<StockResponse> getStockByTicker(@PathVariable String ticker){
        StockResponse stockResponse = stockService.getStockByTicker(ticker);

        return ApiResponse.success("종목 조회에 성공했습니다.", stockResponse);
    }

    @GetMapping("/{ticker}/detail")
    public ApiResponse<StockDetailResponse> getStockDetail(
            @PathVariable String ticker
    ) {
        return ApiResponse.success(
                "종목 상세 조회에 성공했습니다.",
                stockService.getStockDetail(ticker)
        );
    }

    @GetMapping("/price-history")
    public ApiResponse<List<StockPriceHistoryResponse>> getPriceHistories(
            @RequestParam List<String> tickers
    ) {
        return ApiResponse.success(
                "종목 가격 이력 조회에 성공했습니다.",
                stockService.getPriceHistories(tickers)
        );
    }
}

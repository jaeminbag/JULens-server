package com.julensserver.controller;

import com.julensserver.dto.common.ApiResponse;
import com.julensserver.dto.common.PageResponse;
import com.julensserver.dto.stock.StockNewsResponse;
import com.julensserver.service.StockNewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stock-news")
public class StockNewsController {

    private final StockNewsService stockNewsService;

    @GetMapping
    public ApiResponse<PageResponse<StockNewsResponse>> getStockNews(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String ticker,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                "종목 뉴스 조회에 성공했습니다.",
                PageResponse.from(
                        stockNewsService.search(keyword, ticker, page, size)
                )
        );
    }
}

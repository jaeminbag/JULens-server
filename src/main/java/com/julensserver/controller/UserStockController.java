package com.julensserver.controller;

import com.julensserver.dto.common.ApiResponse;
import com.julensserver.dto.stock.StockResponse;
import com.julensserver.dto.stock.UserStockLatestResponse;
import com.julensserver.service.UserStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user-stocks")
public class UserStockController {
    private final UserStockService userStockService;

    @PutMapping("/{stockId}")
    public ApiResponse<StockResponse> addUserStock(@PathVariable Long stockId, @AuthenticationPrincipal Long userId){
        StockResponse stockResponse = userStockService.addUserStock(userId, stockId);


        return ApiResponse.success("관심 종목 추가에 성공했습니다.", stockResponse);
    }

    @GetMapping
    public ApiResponse<List<StockResponse>> getUserStocks(@AuthenticationPrincipal Long userId){
        List<StockResponse> stockResponses = userStockService.getUserStocks(userId);


        return ApiResponse.success("관심 종목 조회에 성공했습니다.", stockResponses);
    }

    @GetMapping("/latest")
    public ApiResponse<List<UserStockLatestResponse>> getUserStocksLatest(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                "관심 종목 최신 분석 조회에 성공했습니다.",
                userStockService.getUserStocksLatest(userId)
        );
    }

    @DeleteMapping("/{stockId}")
    public ApiResponse<Void> deleteUserStock(@PathVariable Long stockId, @AuthenticationPrincipal Long userId){
        userStockService.deleteUserStock(userId, stockId);

        return ApiResponse.success("관심 종목 제거에 성공했습니다.");
    }

}

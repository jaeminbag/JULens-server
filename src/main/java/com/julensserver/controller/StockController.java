package com.julensserver.controller;

import com.julensserver.dto.common.ApiResponse;
import com.julensserver.dto.stock.StockDetailResponse;
import com.julensserver.dto.stock.StockPriceHistoryResponse;
import com.julensserver.dto.stock.StockResponse;
import com.julensserver.dto.stock.RealtimeStockPriceResponse;
import com.julensserver.service.RealtimeStockPriceService;
import com.julensserver.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stocks")
public class StockController {
    private final StockService stockService;
    private final RealtimeStockPriceService realtimeStockPriceService;

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

    @GetMapping(
            value = "/realtime",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamRealtimePrices(
            @RequestParam List<String> tickers
    ) {
        SseEmitter emitter = new SseEmitter(0L);
        AtomicReference<RealtimeStockPriceService.Subscription> reference =
                new AtomicReference<>();

        RealtimeStockPriceService.Subscription subscription =
                realtimeStockPriceService.subscribe(tickers, price -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("price")
                                .id(price.ticker() + ":" + price.timestamp())
                                .data(price));
                    } catch (IOException | IllegalStateException exception) {
                        close(reference.get());
                        emitter.completeWithError(exception);
                    }
                });
        reference.set(subscription);

        Runnable cleanup = () -> close(reference.get());
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("ready")
                    .data("IEX realtime stream connected"));
        } catch (IOException exception) {
            cleanup.run();
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    @GetMapping("/realtime/latest")
    public ApiResponse<List<RealtimeStockPriceResponse>> getLatestRealtimePrices(
            @RequestParam List<String> tickers
    ) {
        return ApiResponse.success(
                "IEX 최신 실시간 가격 조회에 성공했습니다.",
                realtimeStockPriceService.getLatestPrices(tickers)
        );
    }

    private void close(RealtimeStockPriceService.Subscription subscription) {
        if (subscription != null) {
            subscription.close();
        }
    }
}

package com.julensserver.controller;

import com.julensserver.dto.common.ApiResponse;
import com.julensserver.dto.stock.StockDetailResponse;
import com.julensserver.dto.stock.StockPriceHistoryResponse;
import com.julensserver.dto.stock.StockResponse;
import com.julensserver.dto.stock.RealtimeStockPriceResponse;
import com.julensserver.dto.stock.ExchangeRateResponse;
import com.julensserver.dto.stock.StockPricePeriod;
import com.julensserver.service.ExchangeRateService;
import com.julensserver.service.RealtimeStockPriceService;
import com.julensserver.service.StockService;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stocks")
public class StockController {
    private static final long SSE_HEARTBEAT_SECONDS = 15L;

    private final StockService stockService;
    private final RealtimeStockPriceService realtimeStockPriceService;
    private final ExchangeRateService exchangeRateService;
    private final ScheduledExecutorService heartbeatExecutor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "stock-sse-heartbeat");
                thread.setDaemon(true);
                return thread;
            });

    @GetMapping
    public ApiResponse<Page<StockResponse>> getStocks(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @PageableDefault(size = 200, sort = "ticker") Pageable pageable
    ) {
        return ApiResponse.success(
                "종목 목록 조회에 성공했습니다.",
                stockService.getStocks(activeOnly, pageable)
        );
    }

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
            @RequestParam List<String> tickers,
            @RequestParam(defaultValue = "REALTIME") StockPricePeriod period
    ) {
        return ApiResponse.success(
                "종목 가격 이력 조회에 성공했습니다.",
                stockService.getPriceHistories(tickers, period)
        );
    }

    @GetMapping("/exchange-rate/usd-krw")
    public ApiResponse<ExchangeRateResponse> getUsdKrwExchangeRate() {
        return ApiResponse.success(
                "원/달러 기준환율 조회에 성공했습니다.",
                exchangeRateService.getUsdKrwRate()
        );
    }

    @GetMapping(
            value = "/realtime",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamRealtimePrices(
            @RequestParam List<String> tickers,
            HttpServletResponse response
    ) {
        // Nginx와 브라우저가 실시간 이벤트를 캐시하거나 모아서 보내지 않게 한다.
        response.setHeader("Cache-Control", "no-cache, no-store");
        response.setHeader("X-Accel-Buffering", "no");

        SseEmitter emitter = new SseEmitter(0L);
        AtomicReference<RealtimeStockPriceService.Subscription> reference =
                new AtomicReference<>();
        AtomicReference<ScheduledFuture<?>> heartbeatReference =
                new AtomicReference<>();

        RealtimeStockPriceService.Subscription subscription =
                realtimeStockPriceService.subscribe(tickers, price -> {
                    try {
                        sendPrice(emitter, price);
                    } catch (IOException | IllegalStateException exception) {
                        close(reference.get());
                        // 탭 이동·새로고침으로 끊긴 SSE는 서버 오류가 아니라 정상 종료다.
                        emitter.complete();
                    }
                });
        reference.set(subscription);

        Runnable cleanup = () -> {
            close(reference.get());
            ScheduledFuture<?> heartbeat = heartbeatReference.getAndSet(null);
            if (heartbeat != null) {
                heartbeat.cancel(false);
            }
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("ready")
                    .data("Realtime price stream connected"));

            // 새 탭은 다음 체결까지 기다리지 않고 서버가 가진 최신가를 즉시 받는다.
            for (RealtimeStockPriceResponse price
                    : realtimeStockPriceService.getLatestPrices(tickers)) {
                sendPrice(emitter, price);
            }

            // 가격 변화가 없어도 프록시가 연결을 끊지 않도록 주기적으로 전송한다.
            heartbeatReference.set(heartbeatExecutor.scheduleAtFixedRate(() -> {
                try {
                    emitter.send(SseEmitter.event().comment("keep-alive"));
                } catch (IOException | IllegalStateException exception) {
                    cleanup.run();
                    emitter.complete();
                }
            }, SSE_HEARTBEAT_SECONDS, SSE_HEARTBEAT_SECONDS, TimeUnit.SECONDS));
        } catch (IOException | IllegalStateException exception) {
            cleanup.run();
            emitter.complete();
        }
        return emitter;
    }

    @GetMapping("/realtime/latest")
    public ApiResponse<List<RealtimeStockPriceResponse>> getLatestRealtimePrices(
            @RequestParam List<String> tickers
    ) {
        return ApiResponse.success(
                "최신 실시간 가격 조회에 성공했습니다.",
                realtimeStockPriceService.getLatestPrices(tickers)
        );
    }

    private void close(RealtimeStockPriceService.Subscription subscription) {
        if (subscription != null) {
            subscription.close();
        }
    }

    private void sendPrice(
            SseEmitter emitter,
            RealtimeStockPriceResponse price
    ) throws IOException {
        emitter.send(SseEmitter.event()
                .name("price")
                .id(price.ticker() + ":" + price.timestamp())
                .data(price));
    }

    @PreDestroy
    void stopHeartbeatExecutor() {
        heartbeatExecutor.shutdownNow();
    }
}

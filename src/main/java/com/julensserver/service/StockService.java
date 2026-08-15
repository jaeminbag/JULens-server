package com.julensserver.service;

import com.julensserver.dto.stock.StockDetailResponse;
import com.julensserver.dto.stock.StockPriceHistoryResponse;
import com.julensserver.dto.stock.StockPricePointResponse;
import com.julensserver.dto.stock.StockPricePeriod;
import com.julensserver.dto.stock.StockResponse;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class StockService {

    private static final Logger log =
            LoggerFactory.getLogger(StockService.class);

    private final StockQueryService stockQueryService;
    private final StockPriceHistoryProvider stockPriceHistoryProvider;

    public StockService(
            StockQueryService stockQueryService,
            StockPriceHistoryProvider stockPriceHistoryProvider
    ) {
        this.stockQueryService = stockQueryService;
        this.stockPriceHistoryProvider = stockPriceHistoryProvider;
    }

    public StockResponse getStockByTicker(String ticker) {
        return stockQueryService.getStockByTicker(ticker);
    }

    public Page<StockResponse> getStocks(
            boolean activeOnly,
            Pageable pageable
    ) {
        return stockQueryService.getStocks(activeOnly, pageable);
    }

    public StockDetailResponse getStockDetail(String ticker) {
        String resolvedTicker = stockQueryService.findTicker(ticker);

        // Alpaca HTTP 호출은 DB 읽기 트랜잭션이 끝난 뒤 실행한다.
        List<StockPricePointResponse> priceHistory =
                getPriceHistory(resolvedTicker);
        return stockQueryService.getStockDetail(
                resolvedTicker,
                priceHistory
        );
    }

    public List<StockPriceHistoryResponse> getPriceHistories(
            List<String> tickers,
            StockPricePeriod period
    ) {
        if (tickers == null || tickers.isEmpty() || tickers.size() > 20) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<String> normalizedTickers = tickers.stream()
                .filter(ticker -> ticker != null && !ticker.isBlank())
                .map(ticker -> ticker.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        if (normalizedTickers.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Set<String> existingTickers = new LinkedHashSet<>(
                stockQueryService.findTickers(normalizedTickers)
        );

        return normalizedTickers.stream()
                .filter(existingTickers::contains)
                .map(ticker -> getPriceHistory(ticker, period))
                .toList();
    }

    private List<StockPricePointResponse> getPriceHistory(String ticker) {
        try {
            return stockPriceHistoryProvider.getPriceHistory(ticker);
        } catch (RuntimeException exception) {
            // 차트 조회 실패가 종목 상세 전체를 막지 않도록 빈 그래프로 응답한다.
            log.warn(
                    "Stock price history unavailable. ticker={}",
                    ticker,
                    exception
            );
            return List.of();
        }
    }

    private StockPriceHistoryResponse getPriceHistory(
            String ticker,
            StockPricePeriod period
    ) {
        try {
            return stockPriceHistoryProvider.getPriceHistory(ticker, period);
        } catch (RuntimeException exception) {
            log.warn(
                    "Stock price history unavailable. ticker={}, period={}",
                    ticker,
                    period,
                    exception
            );
            return new StockPriceHistoryResponse(
                    ticker,
                    period,
                    null,
                    null,
                    null,
                    List.of()
            );
        }
    }
}

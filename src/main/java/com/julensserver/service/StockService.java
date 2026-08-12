package com.julensserver.service;

import com.julensserver.domain.LensAnalysis;
import com.julensserver.domain.Stock;
import com.julensserver.dto.stock.StockDetailResponse;
import com.julensserver.dto.stock.StockPriceHistoryResponse;
import com.julensserver.dto.stock.StockPricePointResponse;
import com.julensserver.dto.stock.StockResponse;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.LensAnalysisRepository;
import com.julensserver.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private static final Logger log =
            LoggerFactory.getLogger(StockService.class);

    private final StockRepository stockRepository;
    private final LensAnalysisRepository lensAnalysisRepository;
    private final StockNewsService stockNewsService;
    private final StockPriceHistoryProvider stockPriceHistoryProvider;

    public StockResponse getStockByTicker(String ticker) {
        return StockResponse.from(findStock(ticker));
    }

    public StockDetailResponse getStockDetail(String ticker) {
        Stock stock = findStock(ticker);
        LensAnalysis latestAnalysis = lensAnalysisRepository
                .findFirstByStockOrderByAnalyzedAtDescIdDesc(stock)
                .orElse(null);
        List<StockPricePointResponse> priceHistory =
                getPriceHistory(stock);

        return StockDetailResponse.from(
                stock,
                latestAnalysis,
                priceHistory,
                stockNewsService.findLatestEntitiesByTicker(
                        stock.getTicker()
                )
        );
    }

    public List<StockPriceHistoryResponse> getPriceHistories(
            List<String> tickers
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

        Map<String, Stock> stocksByTicker = new LinkedHashMap<>();
        for (Stock stock : stockRepository.findAllByTickerIn(
                normalizedTickers
        )) {
            stocksByTicker.put(
                    stock.getTicker().toUpperCase(Locale.ROOT),
                    stock
            );
        }

        return normalizedTickers.stream()
                .filter(stocksByTicker::containsKey)
                .map(ticker -> new StockPriceHistoryResponse(
                        ticker,
                        getPriceHistory(stocksByTicker.get(ticker))
                ))
                .toList();
    }

    private List<StockPricePointResponse> getPriceHistory(Stock stock) {
        try {
            return stockPriceHistoryProvider.getPriceHistory(stock);
        } catch (RuntimeException exception) {
            // 차트 조회 실패가 종목 상세 전체를 막지 않도록 빈 그래프로 응답한다.
            log.warn(
                    "Stock price history unavailable. ticker={}",
                    stock.getTicker(),
                    exception
            );
            return List.of();
        }
    }

    private Stock findStock(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return stockRepository.findByTickerIgnoreCase(ticker.trim())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.STOCK_NOT_FOUND
                ));
    }
}

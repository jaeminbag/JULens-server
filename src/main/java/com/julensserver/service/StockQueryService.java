package com.julensserver.service;

import com.julensserver.domain.LensAnalysis;
import com.julensserver.domain.Stock;
import com.julensserver.dto.stock.StockDetailResponse;
import com.julensserver.dto.stock.StockPricePointResponse;
import com.julensserver.dto.stock.StockResponse;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.LensAnalysisRepository;
import com.julensserver.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * 주식 화면에 필요한 DB 조회만 짧은 읽기 트랜잭션으로 수행한다.
 * 외부 시세 API는 이 서비스 밖에서 호출해 커넥션을 점유하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockQueryService {

    private final StockRepository stockRepository;
    private final LensAnalysisRepository lensAnalysisRepository;
    private final StockNewsService stockNewsService;

    public StockResponse getStockByTicker(String ticker) {
        return StockResponse.from(findStock(ticker));
    }

    public String findTicker(String ticker) {
        return findStock(ticker).getTicker().toUpperCase(Locale.ROOT);
    }

    public List<String> findTickers(Collection<String> tickers) {
        return stockRepository.findAllByTickerIn(tickers).stream()
                .map(stock -> stock.getTicker().toUpperCase(
                        Locale.ROOT
                ))
                .toList();
    }

    public List<String> findActiveTickers(Collection<String> tickers) {
        return stockRepository.findAllByTickerIn(tickers).stream()
                .filter(Stock::isActive)
                .map(stock -> stock.getTicker().toUpperCase(
                        Locale.ROOT
                ))
                .toList();
    }

    public StockDetailResponse getStockDetail(
            String ticker,
            List<StockPricePointResponse> priceHistory
    ) {
        Stock stock = findStock(ticker);
        LensAnalysis latestAnalysis = lensAnalysisRepository
                .findFirstByStockOrderByAnalyzedAtDescIdDesc(stock)
                .orElse(null);

        // 관련 종목은 LAZY 관계이므로 트랜잭션 안에서 DTO로 변환한다.
        return StockDetailResponse.from(
                stock,
                latestAnalysis,
                priceHistory,
                stockNewsService.findLatestEntitiesByTicker(
                        stock.getTicker()
                )
        );
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

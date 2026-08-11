package com.julensserver.service;

import com.julensserver.domain.LensAnalysis;
import com.julensserver.domain.Stock;
import com.julensserver.dto.stock.StockDetailResponse;
import com.julensserver.dto.stock.StockResponse;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.LensAnalysisRepository;
import com.julensserver.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private final LensAnalysisRepository lensAnalysisRepository;
    private final StockNewsService stockNewsService;

    public StockResponse getStockByTicker(String ticker) {
        return StockResponse.from(findStock(ticker));
    }

    public StockDetailResponse getStockDetail(String ticker) {
        Stock stock = findStock(ticker);
        LensAnalysis latestAnalysis = lensAnalysisRepository
                .findFirstByStockOrderByAnalyzedAtDescIdDesc(stock)
                .orElse(null);

        return StockDetailResponse.from(
                stock,
                latestAnalysis,
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

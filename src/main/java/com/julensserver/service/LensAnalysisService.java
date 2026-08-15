package com.julensserver.service;

import com.julensserver.domain.LensAnalysisBatch;
import com.julensserver.domain.LensBatchStatus;
import com.julensserver.domain.MarketSession;
import com.julensserver.domain.Stock;
import com.julensserver.dto.lens.LensAnalysisResponse;
import com.julensserver.dto.lens.LensAnalysisSortBy;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.LensAnalysisBatchRepository;
import com.julensserver.repository.LensAnalysisRepository;
import com.julensserver.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LensAnalysisService {

    private static final Logger log =
            LoggerFactory.getLogger(LensAnalysisService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final LensAnalysisBatchRepository lensAnalysisBatchRepository;
    private final LensAnalysisRepository lensAnalysisRepository;
    private final StockRepository stockRepository;
    private final MostActiveStockProvider mostActiveStockProvider;
    private final LensStockAnalyzer lensStockAnalyzer;
    private final LensAnalysisPersistenceService persistenceService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public synchronized void runAnalysis(MarketSession marketSession) {
        persistenceService.failStaleBatches(
                LocalDateTime.now().minusMinutes(30)
        );

        if (isAnalysisRunning()) {
            throw new BusinessException(ErrorCode.LENS_ANALYSIS_ALREADY_RUNNING);
        }

        LensAnalysisBatch batch = persistenceService.startBatch(marketSession);

        try {
            List<String> mostActiveTickers =
                    mostActiveStockProvider.getMostActiveTickers();
            if (mostActiveTickers.isEmpty()) {
                throw new BusinessException(
                        ErrorCode.EXTERNAL_DATA_PROVIDER_ERROR,
                        "Alpaca 거래량 상위 종목 목록이 비어 있습니다."
                );
            }

            List<Stock> activeStocks = resolveMostActiveStocks(
                    mostActiveTickers
            );
            if (activeStocks.isEmpty()) {
                throw new BusinessException(
                        ErrorCode.ACTIVE_STOCK_NOT_FOUND
                );
            }

            List<LensAnalysisCandidate> candidates =
                    new ArrayList<>();
            for (Stock stock : activeStocks) {
                try {
                    candidates.add(lensStockAnalyzer.analyze(stock));
                } catch (RuntimeException exception) {
                    log.warn(
                            "Lens stock analysis skipped. ticker={}",
                            stock.getTicker(),
                            exception
                    );
                }
            }

            if (candidates.isEmpty()) {
                throw new BusinessException(
                        ErrorCode.EXTERNAL_DATA_PROVIDER_ERROR,
                        "활성 종목의 분석 데이터를 한 건도 가져오지 못했습니다."
                );
            }

            persistenceService.completeBatch(batch.getId(), candidates);
        } catch (RuntimeException exception) {
            persistenceService.failBatch(batch.getId());
            throw exception;
        }
    }

    public boolean isAnalysisRunning() {
        return lensAnalysisBatchRepository.existsByStatus(
                LensBatchStatus.RUNNING
        );
    }

    /**
     * 최초 배포처럼 비어 있는 DB에서 분석 초기화가 필요한지 확인한다.
     * 완료 배치가 하나라도 있으면 기존 결과와 정기 스케줄을 그대로 사용한다.
     */
    public boolean hasCompletedAnalysis() {
        return lensAnalysisBatchRepository.existsByStatus(
                LensBatchStatus.COMPLETED
        );
    }

    private List<Stock> resolveMostActiveStocks(List<String> tickers) {
        List<String> normalizedTickers = tickers.stream()
                .filter(ticker -> ticker != null && !ticker.isBlank())
                .map(ticker -> ticker.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        Map<String, Stock> stocksByTicker = new LinkedHashMap<>();
        for (Stock stock : stockRepository.findAllByTickerIn(
                normalizedTickers
        )) {
            stocksByTicker.put(
                    stock.getTicker().trim().toUpperCase(Locale.ROOT),
                    stock
            );
        }

        return normalizedTickers.stream()
                .map(stocksByTicker::get)
                .filter(stock -> stock != null && stock.isActive())
                .toList();
    }

    public Page<LensAnalysisResponse> getLatestAnalyses(
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            LensAnalysisSortBy sortBy,
            Sort.Direction direction,
            int page,
            int size
    ) {
        validateQuery(minPrice, maxPrice, page, size);

        LensAnalysisBatch latestBatch = lensAnalysisBatchRepository
                .findFirstByStatusOrderByCompletedAtDescIdDesc(
                        LensBatchStatus.COMPLETED
                )
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.LENS_ANALYSIS_NOT_FOUND
                ));

        LensAnalysisSortBy resolvedSort = sortBy == null
                ? LensAnalysisSortBy.TOTAL_SCORE
                : sortBy;
        Sort.Direction resolvedDirection = direction == null
                ? Sort.Direction.DESC
                : direction;
        Pageable pageable = PageRequest.of(
                page,
                Math.min(size, MAX_PAGE_SIZE),
                resolvedSort.toSort(resolvedDirection)
        );

        return lensAnalysisRepository.searchLatest(
                        latestBatch,
                        normalizeKeyword(keyword),
                        minPrice,
                        maxPrice,
                        pageable
                )
                .map(LensAnalysisResponse::from);
    }

    private void validateQuery(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size
    ) {
        if (page < 0 || size < 1
                || (minPrice != null && minPrice.signum() < 0)
                || (maxPrice != null && maxPrice.signum() < 0)
                || (minPrice != null && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank()
                ? null
                : keyword.trim();
    }
}

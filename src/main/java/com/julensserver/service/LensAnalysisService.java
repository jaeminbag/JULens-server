package com.julensserver.service;

import com.julensserver.domain.LensAnalysis;
import com.julensserver.domain.LensAnalysisBatch;
import com.julensserver.domain.LensBatchStatus;
import com.julensserver.domain.MarketSession;
import com.julensserver.domain.Stock;
import com.julensserver.dto.lens.LensAnalysisResponse;
import com.julensserver.dto.lens.LensAnalysisResult;
import com.julensserver.dto.lens.LensAnalysisSortBy;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.LensAnalysisBatchRepository;
import com.julensserver.repository.LensAnalysisRepository;
import com.julensserver.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LensAnalysisService {

    private final LensAnalysisBatchRepository lensAnalysisBatchRepository;
    private final LensAnalysisRepository lensAnalysisRepository;
    private final StockRepository stockRepository;
    private final LensStockAnalyzer lensStockAnalyzer;

    @Transactional
    public void runAnalysis(MarketSession marketSession) {
        LensAnalysisBatch batch =
                lensAnalysisBatchRepository.save(
                        LensAnalysisBatch.start(marketSession)
                );

        List<Stock> stocks =
                stockRepository.findAllByActiveTrue();

        List<LensAnalysis> analyses = stocks.stream()
                .map(stock -> {
                    LensAnalysisResult result =
                            lensStockAnalyzer.analyze(stock);

                    return result.toEntity(batch, stock);
                })
                .toList();

        lensAnalysisRepository.saveAll(analyses);

        batch.complete();
    }

    public List<LensAnalysisResponse> getLatestAnalyses() {
        return getLatestAnalyses(
                null,
                null,
                null,
                LensAnalysisSortBy.TOTAL_SCORE,
                Sort.Direction.DESC
        );
    }

    public List<LensAnalysisResponse> getLatestAnalyses(
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            LensAnalysisSortBy sortBy,
            Sort.Direction direction
    ) {
        if (minPrice != null && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "최소 가격은 최대 가격보다 클 수 없습니다."
            );
        }

        LensAnalysisBatch latestBatch =
                lensAnalysisBatchRepository
                        .findFirstByStatusOrderByCompletedAtDesc(
                                LensBatchStatus.COMPLETED
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.LENS_ANALYSIS_NOT_FOUND
                                )
                        );

        String normalizedKeyword = keyword == null || keyword.isBlank()
                ? null
                : keyword.trim();
        Sort sort = Sort.by(direction, sortBy.getProperty());

        return lensAnalysisRepository
                .searchLatest(
                        latestBatch,
                        normalizedKeyword,
                        minPrice,
                        maxPrice,
                        sort
                )
                .stream()
                .map(LensAnalysisResponse::from)
                .toList();
    }
}

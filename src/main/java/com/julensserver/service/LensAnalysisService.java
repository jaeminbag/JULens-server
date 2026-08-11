package com.julensserver.service;

import com.julensserver.domain.LensAnalysis;
import com.julensserver.domain.LensAnalysisBatch;
import com.julensserver.domain.LensBatchStatus;
import com.julensserver.domain.MarketSession;
import com.julensserver.domain.Stock;
import com.julensserver.dto.lens.LensAnalysisResponse;
import com.julensserver.dto.lens.LensAnalysisResult;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.LensAnalysisBatchRepository;
import com.julensserver.repository.LensAnalysisRepository;
import com.julensserver.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        return lensAnalysisRepository
                .findAllByBatchOrderByTotalScoreDesc(latestBatch)
                .stream()
                .map(LensAnalysisResponse::from)
                .toList();
    }
}
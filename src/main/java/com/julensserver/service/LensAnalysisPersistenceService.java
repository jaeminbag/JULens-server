package com.julensserver.service;

import com.julensserver.domain.LensAnalysis;
import com.julensserver.domain.LensAnalysisBatch;
import com.julensserver.domain.LensBatchStatus;
import com.julensserver.domain.MarketSession;
import com.julensserver.domain.Stock;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.LensAnalysisBatchRepository;
import com.julensserver.repository.LensAnalysisRepository;
import com.julensserver.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LensAnalysisPersistenceService {

    private final LensAnalysisBatchRepository lensAnalysisBatchRepository;
    private final LensAnalysisRepository lensAnalysisRepository;
    private final StockRepository stockRepository;
    private final StockNewsService stockNewsService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LensAnalysisBatch startBatch(MarketSession marketSession) {
        return lensAnalysisBatchRepository.save(
                LensAnalysisBatch.start(marketSession)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeBatch(
            Long batchId,
            List<LensAnalysisCandidate> candidates
    ) {
        LensAnalysisBatch batch = getBatch(batchId);
        List<LensAnalysis> analyses = new ArrayList<>();

        for (LensAnalysisCandidate candidate : candidates) {
            Stock managedStock = stockRepository.getReferenceById(
                    candidate.stock().getId()
            );
            analyses.add(candidate.result().toEntity(batch, managedStock));
            stockNewsService.saveForStock(managedStock, candidate.news());
        }

        lensAnalysisRepository.saveAll(analyses);
        batch.complete();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failBatch(Long batchId) {
        getBatch(batchId).fail();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failStaleBatches(LocalDateTime startedBefore) {
        lensAnalysisBatchRepository
                .findAllByStatusAndStartedAtBefore(
                        LensBatchStatus.RUNNING,
                        startedBefore
                )
                .forEach(LensAnalysisBatch::fail);
    }

    private LensAnalysisBatch getBatch(Long batchId) {
        return lensAnalysisBatchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.LENS_ANALYSIS_BATCH_NOT_FOUND
                ));
    }
}

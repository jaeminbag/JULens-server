package com.julensserver.service;

import com.julensserver.domain.LensAnalysisBatch;
import com.julensserver.domain.LensBatchStatus;
import com.julensserver.dto.lens.LensAnalysisResponse;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.LensAnalysisBatchRepository;
import com.julensserver.repository.LensAnalysisRepository;
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
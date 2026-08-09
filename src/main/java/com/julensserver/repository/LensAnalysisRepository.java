package com.julensserver.repository;

import com.julensserver.domain.LensAnalysis;
import com.julensserver.domain.LensAnalysisBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LensAnalysisRepository extends JpaRepository<LensAnalysis, Long> {
    List<LensAnalysis> findAllByBatchOrderByTotalScoreDesc(
            LensAnalysisBatch batch
    );
}

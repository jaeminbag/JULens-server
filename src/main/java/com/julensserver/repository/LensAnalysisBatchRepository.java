package com.julensserver.repository;

import com.julensserver.domain.LensAnalysisBatch;
import com.julensserver.domain.LensBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LensAnalysisBatchRepository
        extends JpaRepository<LensAnalysisBatch, Long> {
    Optional<LensAnalysisBatch>
    findFirstByStatusOrderByCompletedAtDesc(LensBatchStatus status);
}
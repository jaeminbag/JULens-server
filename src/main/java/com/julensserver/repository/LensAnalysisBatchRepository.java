package com.julensserver.repository;

import com.julensserver.domain.LensAnalysisBatch;
import com.julensserver.domain.LensBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LensAnalysisBatchRepository
        extends JpaRepository<LensAnalysisBatch, Long> {
    Optional<LensAnalysisBatch>
    findFirstByStatusOrderByCompletedAtDescIdDesc(LensBatchStatus status);

    boolean existsByStatus(LensBatchStatus status);

    List<LensAnalysisBatch> findAllByStatusAndStartedAtBefore(
            LensBatchStatus status,
            LocalDateTime startedAt
    );
}

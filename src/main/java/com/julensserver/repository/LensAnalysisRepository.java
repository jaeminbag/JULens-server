package com.julensserver.repository;

import com.julensserver.domain.LensAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LensAnalysisRepository extends JpaRepository<LensAnalysis, Long> {
}

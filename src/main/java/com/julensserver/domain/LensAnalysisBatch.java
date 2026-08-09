package com.julensserver.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "lens_analysis_batch")
public class LensAnalysisBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LensBatchStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_session", nullable = false, length = 20)
    private MarketSession marketSession;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    private LensAnalysisBatch(MarketSession marketSession){
        this.status=LensBatchStatus.RUNNING;
        this.marketSession=marketSession;
        this.startedAt=LocalDateTime.now();
    }

    public static LensAnalysisBatch start(MarketSession marketSession){
        return new LensAnalysisBatch(marketSession);
    }

    public void complete(){
        this.status=LensBatchStatus.COMPLETED;
        this.completedAt=LocalDateTime.now();
    }

    public void fail(){
        this.status=LensBatchStatus.FAILED;
    }

}

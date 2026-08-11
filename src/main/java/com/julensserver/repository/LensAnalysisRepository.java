package com.julensserver.repository;

import com.julensserver.domain.LensAnalysis;
import com.julensserver.domain.LensAnalysisBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface LensAnalysisRepository extends JpaRepository<LensAnalysis, Long> {
    @Query(
            value = """
            select analysis
            from LensAnalysis analysis
            join fetch analysis.stock stock
            where analysis.batch = :batch
              and (:keyword is null
                   or lower(stock.ticker) like lower(concat('%', :keyword, '%'))
                   or lower(stock.companyName) like lower(concat('%', :keyword, '%'))
                   or lower(stock.companyNameKr) like lower(concat('%', :keyword, '%')))
              and (:minPrice is null or analysis.currentPrice >= :minPrice)
              and (:maxPrice is null or analysis.currentPrice <= :maxPrice)
            """,
            countQuery = """
            select count(analysis)
            from LensAnalysis analysis
            join analysis.stock stock
            where analysis.batch = :batch
              and (:keyword is null
                   or lower(stock.ticker) like lower(concat('%', :keyword, '%'))
                   or lower(stock.companyName) like lower(concat('%', :keyword, '%'))
                   or lower(stock.companyNameKr) like lower(concat('%', :keyword, '%')))
              and (:minPrice is null or analysis.currentPrice >= :minPrice)
              and (:maxPrice is null or analysis.currentPrice <= :maxPrice)
            """
    )
    Page<LensAnalysis> searchLatest(
            @Param("batch") LensAnalysisBatch batch,
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    Optional<LensAnalysis> findFirstByStockOrderByAnalyzedAtDescIdDesc(
            com.julensserver.domain.Stock stock
    );

    List<LensAnalysis> findAllByBatchAndStock_IdIn(
            LensAnalysisBatch batch,
            List<Long> stockIds
    );
}

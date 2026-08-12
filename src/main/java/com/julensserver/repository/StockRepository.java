package com.julensserver.repository;

import com.julensserver.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByTicker(String ticker);

    Optional<Stock> findByTickerIgnoreCase(String ticker);

    boolean existsByTicker(String ticker);

    List<Stock> findAllByActiveTrueOrderByTickerAsc();
}

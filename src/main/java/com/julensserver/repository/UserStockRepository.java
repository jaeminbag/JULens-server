package com.julensserver.repository;

import com.julensserver.domain.UserStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserStockRepository extends JpaRepository<UserStock, Long> {
    boolean existsByUser_IdAndStock_Id(Long userId, Long stockId);

    List<UserStock> findAllByUser_Id(Long userId);

    Optional<UserStock> findByUser_IdAndStock_Id(Long userId, Long stockId);

}

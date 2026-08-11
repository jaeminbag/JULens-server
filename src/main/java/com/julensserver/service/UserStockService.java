package com.julensserver.service;

import com.julensserver.domain.LensAnalysis;
import com.julensserver.domain.LensAnalysisBatch;
import com.julensserver.domain.LensBatchStatus;
import com.julensserver.domain.Stock;
import com.julensserver.domain.User;
import com.julensserver.domain.UserStock;
import com.julensserver.dto.stock.StockResponse;
import com.julensserver.dto.stock.UserStockLatestResponse;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.LensAnalysisBatchRepository;
import com.julensserver.repository.LensAnalysisRepository;
import com.julensserver.repository.StockRepository;
import com.julensserver.repository.UserRepository;
import com.julensserver.repository.UserStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserStockService {

    private final UserStockRepository userStockRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final LensAnalysisBatchRepository lensAnalysisBatchRepository;
    private final LensAnalysisRepository lensAnalysisRepository;

    @Transactional
    public StockResponse addUserStock(Long userId, Long stockId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.USER_NOT_FOUND
                ));
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.STOCK_NOT_FOUND
                ));

        if (userStockRepository.existsByUser_IdAndStock_Id(userId, stockId)) {
            throw new BusinessException(ErrorCode.USER_STOCK_ALREADY_EXISTS);
        }

        userStockRepository.save(new UserStock(user, stock));
        return StockResponse.from(stock);
    }

    @Transactional(readOnly = true)
    public List<StockResponse> getUserStocks(Long userId) {
        validateUser(userId);
        return userStockRepository.findAllByUser_Id(userId).stream()
                .map(userStock -> StockResponse.from(userStock.getStock()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserStockLatestResponse> getUserStocksLatest(Long userId) {
        validateUser(userId);
        List<UserStock> userStocks =
                userStockRepository.findAllByUser_Id(userId);

        if (userStocks.isEmpty()) {
            return List.of();
        }

        LensAnalysisBatch latestBatch = lensAnalysisBatchRepository
                .findFirstByStatusOrderByCompletedAtDescIdDesc(
                        LensBatchStatus.COMPLETED
                )
                .orElse(null);
        Map<Long, LensAnalysis> analysisByStockId = new HashMap<>();

        if (latestBatch != null) {
            List<Long> stockIds = userStocks.stream()
                    .map(userStock -> userStock.getStock().getId())
                    .toList();
            lensAnalysisRepository
                    .findAllByBatchAndStock_IdIn(latestBatch, stockIds)
                    .forEach(analysis -> analysisByStockId.put(
                            analysis.getStock().getId(),
                            analysis
                    ));
        }

        return userStocks.stream()
                .map(userStock -> UserStockLatestResponse.from(
                        userStock,
                        analysisByStockId.get(userStock.getStock().getId())
                ))
                .toList();
    }

    @Transactional
    public void deleteUserStock(Long userId, Long stockId) {
        if (!stockRepository.existsById(stockId)) {
            throw new BusinessException(ErrorCode.STOCK_NOT_FOUND);
        }

        UserStock userStock = userStockRepository
                .findByUser_IdAndStock_Id(userId, stockId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.USER_STOCK_NOT_FOUND
                ));
        userStockRepository.delete(userStock);
    }

    private void validateUser(Long userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }
}

package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.domain.User;
import com.julensserver.domain.UserStock;
import com.julensserver.dto.stock.StockResponse;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.StockRepository;
import com.julensserver.repository.UserRepository;
import com.julensserver.repository.UserStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@RequiredArgsConstructor
@Service
public class UserStockService {
    private final UserStockRepository userStockRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;

    @Transactional
    public StockResponse addUserStock(Long userId, Long stockId){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(()-> new BusinessException(ErrorCode.STOCK_NOT_FOUND));

        if(userStockRepository.existsByUser_IdAndStock_Id(userId,stockId)){
            throw new BusinessException(ErrorCode.USER_STOCK_ALREADY_EXISTS);
        }

        UserStock userStock = new UserStock(user, stock);
        userStockRepository.save(userStock);

        return StockResponse.from(stock);
    }

    @Transactional(readOnly = true)
    public List<StockResponse> getUserStocks(Long userId){
        if(!userRepository.existsById(userId)){
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return userStockRepository.findAllByUser_Id(userId).stream()
                .map(userStock -> StockResponse.from(userStock.getStock()))
                .toList();
    }



    @Transactional
    public void deleteUserStock(Long userId, Long stockId){
        if(!stockRepository.existsById(stockId)){
            throw new BusinessException(ErrorCode.STOCK_NOT_FOUND);
        }

        UserStock userStock = userStockRepository.findByUser_IdAndStock_Id(userId, stockId)
                        .orElseThrow(()-> new BusinessException(ErrorCode.USER_STOCK_NOT_FOUND));

        userStockRepository.delete(userStock);
    }

}

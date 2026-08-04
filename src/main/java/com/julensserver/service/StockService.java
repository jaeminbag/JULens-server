package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.dto.stock.StockResponse;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {
    private final StockRepository stockRepository;


    public StockResponse getStockByTicker(String ticker){
        Stock stock = stockRepository.findByTicker(ticker)
                .orElseThrow(()-> new BusinessException(ErrorCode.STOCK_NOT_FOUND));

        return StockResponse.from(stock);
    }

}

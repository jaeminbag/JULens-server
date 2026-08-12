package com.julensserver.service;

import com.julensserver.domain.Currency;
import com.julensserver.domain.Exchange;
import com.julensserver.domain.Stock;
import com.julensserver.exception.BusinessException;
import com.julensserver.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockSyncServiceTest {

    @Test
    void 외부_전체종목을_갱신하고_목록에서_사라진_종목을_비활성화한다() {
        StockRepository repository = mock(StockRepository.class);
        StockSymbolProvider provider = mock(StockSymbolProvider.class);
        Stock apple = stock("AAPL", "Old Apple", "옛 애플", false);
        Stock legacy = stock("OLD", "Old Company", "기존 종목", true);

        when(repository.findAll()).thenReturn(List.of(apple, legacy));
        when(provider.getUsCommonStocks()).thenReturn(List.of(
                symbol("AAPL", "APPLE INC", Exchange.NASDAQ),
                symbol("MSFT", "MICROSOFT CORP", Exchange.NASDAQ)
        ));
        StockSyncService service = new StockSyncService(
                repository,
                provider,
                1
        );

        StockSyncResult result = service.synchronize();

        ArgumentCaptor<List<Stock>> captor = ArgumentCaptor.forClass(
                List.class
        );
        verify(repository).saveAll(captor.capture());
        Stock microsoft = captor.getValue().stream()
                .filter(stock -> stock.getTicker().equals("MSFT"))
                .findFirst()
                .orElseThrow();

        assertTrue(apple.isActive());
        assertEquals("APPLE INC", apple.getCompanyName());
        assertEquals("옛 애플", apple.getCompanyNameKr());
        assertTrue(microsoft.isActive());
        assertEquals("MICROSOFT CORP", microsoft.getCompanyNameKr());
        assertFalse(legacy.isActive());
        assertEquals(2, result.activated());
        assertEquals(1, result.created());
        assertEquals(1, result.updated());
        assertEquals(1, result.deactivated());
    }

    @Test
    void 외부_종목목록이_비어있으면_기존종목을_변경하지_않는다() {
        StockRepository repository = mock(StockRepository.class);
        StockSymbolProvider provider = mock(StockSymbolProvider.class);
        Stock existing = stock("AAPL", "Apple", "애플", true);
        when(provider.getUsCommonStocks()).thenReturn(List.of());

        StockSyncService service = new StockSyncService(
                repository,
                provider,
                1
        );

        assertThrows(BusinessException.class, service::synchronize);
        assertTrue(existing.isActive());
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void 외부_종목수가_기준보다_적으면_기존종목을_비활성화하지_않는다() {
        StockRepository repository = mock(StockRepository.class);
        StockSymbolProvider provider = mock(StockSymbolProvider.class);
        Stock existing = stock("MSFT", "Microsoft", "마이크로소프트", true);
        when(provider.getUsCommonStocks()).thenReturn(List.of(
                symbol("AAPL", "APPLE INC", Exchange.NASDAQ)
        ));

        StockSyncService service = new StockSyncService(
                repository,
                provider,
                2
        );

        assertThrows(BusinessException.class, service::synchronize);
        assertTrue(existing.isActive());
        verify(repository, never()).findAll();
        verify(repository, never()).saveAll(anyList());
    }

    private StockSymbolData symbol(
            String ticker,
            String companyName,
            Exchange exchange
    ) {
        return new StockSymbolData(
                ticker,
                companyName,
                exchange,
                Currency.USD
        );
    }

    private Stock stock(
            String ticker,
            String companyName,
            String companyNameKr,
            boolean active
    ) {
        Stock stock = new Stock(
                ticker,
                companyName,
                companyNameKr,
                Exchange.NASDAQ,
                Currency.USD,
                null
        );
        if (active) {
            stock.activate();
        }
        return stock;
    }
}

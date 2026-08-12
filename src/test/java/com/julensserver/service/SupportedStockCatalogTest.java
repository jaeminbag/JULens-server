package com.julensserver.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SupportedStockCatalogTest {

    @Test
    void 기본_분석_카탈로그는_한국어명을_가진_100개_종목이다() {
        SupportedStockCatalog catalog = new SupportedStockCatalog();

        assertEquals(100, catalog.stocks().size());
        assertEquals(
                100,
                catalog.stocks().stream()
                        .map(SupportedStockCatalog.SupportedStock::ticker)
                        .distinct()
                        .count()
        );
        assertFalse(catalog.stocks().stream().anyMatch(stock ->
                stock.companyNameKr().isBlank()
        ));
    }
}

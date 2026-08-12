package com.julensserver.service;

import java.util.List;

public interface StockSymbolProvider {
    List<StockSymbolData> getUsCommonStocks();
}

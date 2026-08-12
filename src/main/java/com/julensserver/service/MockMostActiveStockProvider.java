package com.julensserver.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("mock")
public class MockMostActiveStockProvider
        implements MostActiveStockProvider {

    @Override
    public List<String> getMostActiveTickers() {
        return List.of("AAPL", "MSFT", "NVDA");
    }
}

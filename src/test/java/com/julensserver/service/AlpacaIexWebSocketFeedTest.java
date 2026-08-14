package com.julensserver.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlpacaIexWebSocketFeedTest {

    @Test
    void Alpaca_체결_메시지를_실시간_가격으로_변환한다() {
        AlpacaIexWebSocketFeed feed = new AlpacaIexWebSocketFeed(
                new JsonMapper(),
                "wss://example.com/v2/iex",
                "test-key",
                "test-secret"
        );
        List<RealtimeStockPrice> received = new ArrayList<>();
        feed.setPriceConsumer(received::add);

        feed.acceptPayload("""
                [{
                  "T":"t",
                  "S":"CHOW",
                  "p":2.37,
                  "s":100,
                  "t":"2026-08-14T13:30:00Z"
                }]
                """);

        assertEquals(1, received.size());
        assertEquals("CHOW", received.getFirst().ticker());
        assertEquals("2.37", received.getFirst().price().toPlainString());
        assertEquals("100", received.getFirst().tradeSize().toPlainString());
    }
}

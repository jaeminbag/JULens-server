package com.julensserver.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlpacaOvernightQuoteFeedTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    @Test
    void 오버나이트_실시간_참고호가의_중간값을_전달한다() {
        AlpacaClient client = mock(AlpacaClient.class);
        when(client.getLatestOvernightQuotes(Set.of("MU"))).thenReturn(
                new AlpacaClient.AlpacaLatestQuotesResponse(Map.of(
                        "MU",
                        new AlpacaClient.AlpacaQuote(
                                new BigDecimal("123.40"),
                                new BigDecimal("123.60"),
                                Instant.parse("2026-08-14T06:00:00Z")
                        )
                ))
        );
        AlpacaOvernightQuoteFeed feed =
                new AlpacaOvernightQuoteFeed(client, 5000);
        List<RealtimeStockPrice> received = new ArrayList<>();
        feed.setPriceConsumer(received::add);
        feed.subscribe(Set.of("MU"));

        feed.pollOnce(ZonedDateTime.of(
                2026, 8, 14, 2, 0, 0, 0, NEW_YORK
        ));

        assertEquals(1, received.size());
        assertEquals("123.50", received.getFirst().price().toPlainString());
        assertEquals("OVERNIGHT", received.getFirst().feed());
    }

    @Test
    void 미국_오버나이트_운영시간만_조회한다() {
        assertTrue(AlpacaOvernightQuoteFeed.isOvernightSession(
                ZonedDateTime.of(2026, 8, 13, 20, 0, 0, 0, NEW_YORK)
        ));
        assertTrue(AlpacaOvernightQuoteFeed.isOvernightSession(
                ZonedDateTime.of(2026, 8, 14, 3, 59, 0, 0, NEW_YORK)
        ));
        assertFalse(AlpacaOvernightQuoteFeed.isOvernightSession(
                ZonedDateTime.of(2026, 8, 14, 4, 0, 0, 0, NEW_YORK)
        ));
        assertFalse(AlpacaOvernightQuoteFeed.isOvernightSession(
                ZonedDateTime.of(2026, 8, 14, 20, 0, 0, 0, NEW_YORK)
        ));
    }
}

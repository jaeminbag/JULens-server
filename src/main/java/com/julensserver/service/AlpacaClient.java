package com.julensserver.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Profile("real")
public class AlpacaClient {

    private static final ZoneId NEW_YORK =
            ZoneId.of("America/New_York");
    private static final LocalTime EXTENDED_MARKET_OPEN =
            LocalTime.of(4, 0);
    private static final LocalTime EXTENDED_MARKET_CLOSE =
            LocalTime.of(20, 0);

    private final RestClient restClient;
    private final RestClient screenerRestClient;
    private final String apiKeyId;
    private final String secretKey;
    private final int dataDelayMinutes;

    public AlpacaClient(
            RestClient.Builder restClientBuilder,
            @Value("${alpaca.base-url:https://data.alpaca.markets/v2}")
            String baseUrl,
            @Value("${alpaca.screener-base-url:"
                    + "https://data.alpaca.markets/v1beta1/screener}")
            String screenerBaseUrl,
            @Value("${alpaca.api-key-id}") String apiKeyId,
            @Value("${alpaca.secret-key}") String secretKey,
            @Value("${alpaca.data-delay-minutes:16}")
            int dataDelayMinutes
    ) {
        if (dataDelayMinutes < 15) {
            throw new IllegalArgumentException(
                    "Alpaca 무료 SIP 데이터 지연은 15분 이상이어야 합니다."
            );
        }

        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.screenerRestClient = restClientBuilder
                .baseUrl(screenerBaseUrl)
                .build();
        this.apiKeyId = apiKeyId;
        this.secretKey = secretKey;
        this.dataDelayMinutes = dataDelayMinutes;
    }

    public AlpacaBarsResponse getDailyBars(
            String ticker,
            int lookbackDays
    ) {
        validateTicker(ticker);

        LocalDate tradingDate = LocalDate.now(NEW_YORK);
        Instant start = tradingDate.minusDays(lookbackDays)
                .atStartOfDay(NEW_YORK)
                .toInstant();
        // 오늘 진행 중인 일봉은 제외하고 전일까지 완성된 일봉만 조회한다.
        Instant end = tradingDate.atStartOfDay(NEW_YORK)
                .toInstant()
                .minusNanos(1);

        return getHistoricalBars(
                ticker,
                "1Day",
                start,
                end,
                "sip",
                1000
        );
    }

    public AlpacaBarsResponse getTodayMinuteBars(String ticker) {
        validateTicker(ticker);

        ZonedDateTime now = ZonedDateTime.now(NEW_YORK);
        // 프리마켓 개장부터 무료 플랜이 허용하는 지연 시각까지 조회한다.
        Instant start = now.toLocalDate()
                .atTime(EXTENDED_MARKET_OPEN)
                .atZone(NEW_YORK)
                .toInstant();
        Instant end = now.toInstant()
                .minus(dataDelayMinutes, ChronoUnit.MINUTES);

        if (!end.isAfter(start)) {
            throw providerError(
                    "Alpaca delayed data is not available yet today"
            );
        }

        return getHistoricalBars(
                ticker,
                "1Min",
                start,
                end,
                "sip",
                10000
        );
    }

    /**
     * 무료 SIP 지연 분봉을 오늘 조회할 수 있는 시간대인지 확인한다.
     * 주말·휴장일 데이터 존재 여부는 실제 bars 응답에서 한 번 더 확인한다.
     */
    public boolean isTodayDelayedDataAvailable() {
        ZonedDateTime now = ZonedDateTime.now(NEW_YORK);
        Instant start = now.toLocalDate()
                .atTime(EXTENDED_MARKET_OPEN)
                .atZone(NEW_YORK)
                .toInstant();
        Instant end = now.toInstant()
                .minus(dataDelayMinutes, ChronoUnit.MINUTES);
        return end.isAfter(start);
    }

    /**
     * 주말·휴장일 초기 분석에서 최근 완료 거래일의 전체 연장장 분봉을 조회한다.
     */
    public AlpacaBarsResponse getMinuteBarsForTradingDate(
            String ticker,
            LocalDate tradingDate
    ) {
        validateTicker(ticker);
        if (tradingDate == null) {
            throw new IllegalArgumentException(
                    "분봉을 조회할 거래일은 null일 수 없습니다."
            );
        }

        Instant start = tradingDate
                .atTime(EXTENDED_MARKET_OPEN)
                .atZone(NEW_YORK)
                .toInstant();
        Instant end = tradingDate
                .atTime(EXTENDED_MARKET_CLOSE)
                .atZone(NEW_YORK)
                .toInstant();

        return getHistoricalBars(
                ticker,
                "1Min",
                start,
                end,
                "sip",
                10000
        );
    }

    /**
     * 차트가 계산한 구간을 지정된 Alpaca 피드로 조회한다.
     * 정규·연장장은 sip, 야간장은 boats를 사용한다.
     */
    public AlpacaBarsResponse getHistoricalBars(
            String ticker,
            String timeframe,
            Instant start,
            Instant end,
            String feed,
            int limit
    ) {
        validateTicker(ticker);
        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException(
                    "가격 이력 종료 시각은 시작 시각보다 뒤여야 합니다."
            );
        }
        if (feed == null || feed.isBlank()) {
            throw new IllegalArgumentException(
                    "가격 이력 피드는 비어 있을 수 없습니다."
            );
        }

        return getBars(
                ticker,
                timeframe,
                start,
                end,
                feed,
                Math.min(Math.max(limit, 1), 10000)
        );
    }

    public int getDataDelayMinutes() {
        return dataDelayMinutes;
    }

    public AlpacaMostActivesResponse getMostActiveStocks(int top) {
        if (top < 1 || top > 100) {
            throw new IllegalArgumentException(
                    "거래량 상위 종목 수는 1~100이어야 합니다."
            );
        }

        try {
            AlpacaMostActivesResponse response = screenerRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stocks/most-actives")
                            .queryParam("by", "volume")
                            .queryParam("top", top)
                            .build())
                    .header("APCA-API-KEY-ID", apiKeyId)
                    .header("APCA-API-SECRET-KEY", secretKey)
                    .retrieve()
                    .body(AlpacaMostActivesResponse.class);

            if (response == null) {
                throw providerError(
                        "Alpaca most-actives response is empty"
                );
            }
            return response;
        } catch (RestClientException exception) {
            throw providerError(
                    "Alpaca most-actives request failed",
                    exception
            );
        }
    }

    /**
     * 무료 플랜의 overnight 피드에서 종목별 최신 참고 호가를 한 번에 조회한다.
     * 체결가는 15분 지연이므로 실시간으로 제공되는 bid/ask 호가를 사용한다.
     */
    public AlpacaLatestQuotesResponse getLatestOvernightQuotes(
            Set<String> tickers
    ) {
        if (tickers == null || tickers.isEmpty()) {
            return new AlpacaLatestQuotesResponse(Map.of());
        }
        String symbols = tickers.stream()
                .sorted()
                .peek(this::validateTicker)
                .reduce((left, right) -> left + "," + right)
                .orElseThrow();

        try {
            AlpacaLatestQuotesResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stocks/quotes/latest")
                            .queryParam("symbols", symbols)
                            .queryParam("feed", "overnight")
                            .build())
                    .header("APCA-API-KEY-ID", apiKeyId)
                    .header("APCA-API-SECRET-KEY", secretKey)
                    .retrieve()
                    .body(AlpacaLatestQuotesResponse.class);

            if (response == null) {
                throw providerError(
                        "Alpaca overnight quotes response is empty"
                );
            }
            return response;
        } catch (RestClientException exception) {
            throw providerError(
                    "Alpaca overnight quotes request failed",
                    exception
            );
        }
    }

    private AlpacaBarsResponse getBars(
            String ticker,
            String timeframe,
            Instant start,
            Instant end,
            String feed,
            int limit
    ) {

        try {
            AlpacaBarsResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stocks/{symbol}/bars")
                            .queryParam("timeframe", timeframe)
                            .queryParam("start", start)
                            .queryParam("end", end)
                            .queryParam("feed", feed)
                            .queryParam("adjustment", "split")
                            .queryParam("sort", "asc")
                            .queryParam("limit", limit)
                            .build(ticker.trim().toUpperCase()))
                    .header("APCA-API-KEY-ID", apiKeyId)
                    .header("APCA-API-SECRET-KEY", secretKey)
                    .retrieve()
                    .body(AlpacaBarsResponse.class);

            if (response == null) {
                throw providerError("Alpaca bars response is empty");
            }
            return response;
        } catch (RestClientException exception) {
            throw providerError("Alpaca bars request failed", exception);
        }
    }

    private void validateTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException(
                    "조회할 종목 티커는 비어 있을 수 없습니다."
            );
        }
    }

    private BusinessException providerError(String detail) {
        return new BusinessException(
                ErrorCode.EXTERNAL_DATA_PROVIDER_ERROR,
                detail
        );
    }

    private BusinessException providerError(
            String detail,
            RuntimeException cause
    ) {
        BusinessException exception = providerError(detail);
        exception.initCause(cause);
        return exception;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AlpacaBarsResponse(
            List<AlpacaBar> bars,
            String symbol,
            @JsonProperty("next_page_token") String nextPageToken
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AlpacaBar(
            @JsonProperty("c") BigDecimal close,
            @JsonProperty("v") BigDecimal volume,
            @JsonProperty("t") Instant timestamp
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AlpacaMostActivesResponse(
            @JsonProperty("most_actives")
            List<AlpacaActiveStock> mostActives,
            @JsonProperty("last_updated") Instant lastUpdated
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AlpacaActiveStock(
            String symbol,
            BigDecimal volume,
            @JsonProperty("trade_count") BigDecimal tradeCount
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AlpacaLatestQuotesResponse(
            Map<String, AlpacaQuote> quotes
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AlpacaQuote(
            @JsonProperty("bp") BigDecimal bidPrice,
            @JsonProperty("ap") BigDecimal askPrice,
            @JsonProperty("t") Instant timestamp
    ) {
    }
}

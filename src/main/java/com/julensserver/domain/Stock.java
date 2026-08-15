package com.julensserver.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String ticker;

    @Column(nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, length = 200)
    private String companyNameKr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Exchange exchange;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency currency;

    @Column(length = 100)
    private String sector;

    @Column(nullable = false)
    private boolean active;

    public Stock(
            String ticker,
            String companyName,
            String companyNameKr,
            Exchange exchange,
            Currency currency,
            String sector
    ){
        this.ticker=ticker;
        this.companyName=companyName;
        this.companyNameKr=companyNameKr;
        this.exchange=exchange;
        this.currency=currency;
        this.sector=sector;
    }

    public void activate(){
        this.active=true;
    }

    public void deactivate(){
        this.active=false;
    }

    public void synchronizeMetadata(
            String companyName,
            String companyNameKr,
            Exchange exchange,
            Currency currency
    ) {
        if (companyName == null || companyName.isBlank()
                || companyNameKr == null || companyNameKr.isBlank()) {
            throw new IllegalArgumentException(
                    "종목명과 한국어 종목명은 비어 있을 수 없습니다."
            );
        }

        this.companyName = companyName.trim();
        this.companyNameKr = companyNameKr.trim();
        this.exchange = Objects.requireNonNull(exchange);
        this.currency = Objects.requireNonNull(currency);
    }
}

package com.julensserver.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
            Exchange exchange,
            Currency currency,
            String sector
    ){
        this.ticker=ticker;
        this.companyName=companyName;
        this.exchange=exchange;
        this.currency=currency;
        this.sector=sector;
    }
}

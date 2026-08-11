package com.julensserver.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Entity
@Table(
        name = "stock_news",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_news_url_hash",
                columnNames = "url_hash"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockNews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, length = 200)
    private String source;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(name = "url_hash", nullable = false, length = 64)
    private String urlHash;

    @Column(name = "published_at", nullable = false)
    private OffsetDateTime publishedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "stock_news_stocks",
            joinColumns = @JoinColumn(name = "news_id"),
            inverseJoinColumns = @JoinColumn(name = "stock_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_stock_news_stock",
                    columnNames = {"news_id", "stock_id"}
            )
    )
    private Set<Stock> relatedStocks = new LinkedHashSet<>();

    private StockNews(
            String title,
            String summary,
            String source,
            String url,
            String urlHash,
            OffsetDateTime publishedAt
    ) {
        this.title = title;
        this.summary = summary;
        this.source = source;
        this.url = url;
        this.urlHash = urlHash;
        this.publishedAt = publishedAt;
    }

    public static StockNews create(
            String title,
            String summary,
            String source,
            String url,
            String urlHash,
            OffsetDateTime publishedAt
    ) {
        return new StockNews(
                title,
                summary,
                source,
                url,
                urlHash,
                publishedAt
        );
    }

    public void updateArticle(
            String title,
            String summary,
            String source,
            String url,
            OffsetDateTime publishedAt
    ) {
        this.title = title;
        this.summary = summary;
        this.source = source;
        this.url = url;
        this.publishedAt = publishedAt;
    }

    public void linkStock(Stock stock) {
        relatedStocks.add(stock);
    }
}

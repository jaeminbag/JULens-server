package com.julensserver.repository;

import com.julensserver.domain.StockNews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockNewsRepository extends JpaRepository<StockNews, Long> {

    Optional<StockNews> findByUrlHash(String urlHash);

    @Query(
            value = """
                    select distinct news
                    from StockNews news
                    left join news.relatedStocks stock
                    where (:keyword is null
                           or lower(news.title) like lower(concat('%', :keyword, '%'))
                           or lower(news.source) like lower(concat('%', :keyword, '%'))
                           or lower(stock.ticker) like lower(concat('%', :keyword, '%'))
                           or lower(stock.companyName) like lower(concat('%', :keyword, '%'))
                           or lower(stock.companyNameKr) like lower(concat('%', :keyword, '%')))
                      and (:ticker is null or lower(stock.ticker) = lower(:ticker))
                    """,
            countQuery = """
                    select count(distinct news.id)
                    from StockNews news
                    left join news.relatedStocks stock
                    where (:keyword is null
                           or lower(news.title) like lower(concat('%', :keyword, '%'))
                           or lower(news.source) like lower(concat('%', :keyword, '%'))
                           or lower(stock.ticker) like lower(concat('%', :keyword, '%'))
                           or lower(stock.companyName) like lower(concat('%', :keyword, '%'))
                           or lower(stock.companyNameKr) like lower(concat('%', :keyword, '%')))
                      and (:ticker is null or lower(stock.ticker) = lower(:ticker))
                    """
    )
    Page<StockNews> search(
            @Param("keyword") String keyword,
            @Param("ticker") String ticker,
            Pageable pageable
    );

    @Query("""
            select distinct news
            from StockNews news
            left join fetch news.relatedStocks
            where news.id in :ids
            """)
    List<StockNews> findAllWithRelatedStocksByIdIn(
            @Param("ids") List<Long> ids
    );

    @Query("""
            select distinct news
            from StockNews news
            join news.relatedStocks matchedStock
            left join fetch news.relatedStocks
            where lower(matchedStock.ticker) = lower(:ticker)
            order by news.publishedAt desc, news.id desc
            """)
    List<StockNews> findLatestByTicker(
            @Param("ticker") String ticker,
            Pageable pageable
    );
}

package com.nexusexchange.stock.repository;

import com.nexusexchange.stock.entity.StockPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockPriceHistoryRepository extends JpaRepository<StockPriceHistory, Long> {
}

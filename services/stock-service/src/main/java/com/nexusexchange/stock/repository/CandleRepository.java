package com.nexusexchange.stock.repository;

import com.nexusexchange.stock.entity.Candle;
import com.nexusexchange.stock.entity.CandleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CandleRepository extends JpaRepository<Candle, CandleId> {
}

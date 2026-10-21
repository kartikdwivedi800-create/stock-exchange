package com.nexusexchange.portfolio.repository;

import com.nexusexchange.portfolio.entity.HoldingTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HoldingTransactionRepository extends JpaRepository<HoldingTransaction, Long> {
}

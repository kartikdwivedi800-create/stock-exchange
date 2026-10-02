package com.nexusexchange.wallet.repository;

import com.nexusexchange.wallet.entity.FundReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FundReservationRepository extends JpaRepository<FundReservation, Long> {

    Optional<FundReservation> findByOrderId(Long orderId);
}

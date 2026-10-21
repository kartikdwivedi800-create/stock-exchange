package com.nexusexchange.portfolio.repository;

import com.nexusexchange.portfolio.entity.ShareReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShareReservationRepository extends JpaRepository<ShareReservation, Long> {

    Optional<ShareReservation> findByOrderId(Long orderId);
}
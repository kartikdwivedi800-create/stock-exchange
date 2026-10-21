package com.nexusexchange.order.repository;

import com.nexusexchange.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByUserIdAndClientOrderId(Long userId, String clientOrderId);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

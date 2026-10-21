package com.nexusexchange.order.repository;

import com.nexusexchange.order.entity.OrderFill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderFillRepository extends JpaRepository<OrderFill, Long> {
}

package com.picard.shipping.repository;

import com.picard.shipping.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    boolean existsByExternalOrderNumber(String externalOrderNumber);
}

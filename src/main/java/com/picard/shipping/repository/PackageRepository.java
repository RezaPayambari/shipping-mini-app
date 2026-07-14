package com.picard.shipping.repository;

import com.picard.shipping.domain.ShipmentPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PackageRepository extends JpaRepository<ShipmentPackage, UUID> {
    boolean existsByTrackingCode(String trackingCode);
}

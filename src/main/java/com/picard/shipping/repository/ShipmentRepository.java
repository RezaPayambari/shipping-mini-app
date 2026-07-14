package com.picard.shipping.repository;

import com.picard.shipping.domain.Shipment;
import com.picard.shipping.domain.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    @Query("""
            select s from Shipment s
            left join fetch s.packages
            where (:status is null or s.status = :status)
            and (:carrier is null or exists (
                select 1 from ShipmentPackage p
                where p.shipment = s and p.carrier = :carrier
            ))
            """)
    List<Shipment> search(@Param("status") ShipmentStatus status, @Param("carrier") String carrier);
}

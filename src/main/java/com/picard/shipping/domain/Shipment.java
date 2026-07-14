package com.picard.shipping.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue
    private UUID id;

    // unique -> enforces "max one shipment per order" on the DB level
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status = ShipmentStatus.CREATED;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShipmentPackage> packages = new ArrayList<>();

    private Instant shippedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public void addPackage(ShipmentPackage pkg) {
        packages.add(pkg);
        pkg.setShipment(this);
    }

    public boolean allPackagesHaveTracking() {
        return !packages.isEmpty() && packages.stream()
                .allMatch(p -> p.getTrackingCode() != null && !p.getTrackingCode().isBlank());
    }
}

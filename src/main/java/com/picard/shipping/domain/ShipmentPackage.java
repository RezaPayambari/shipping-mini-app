package com.picard.shipping.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "packages", uniqueConstraints = @UniqueConstraint(columnNames = "tracking_code"))
@Getter
@Setter
@NoArgsConstructor
public class ShipmentPackage {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "tracking_code", unique = true)
    private String trackingCode;

    private String carrier;
}

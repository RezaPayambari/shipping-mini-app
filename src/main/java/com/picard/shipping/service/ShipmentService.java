package com.picard.shipping.service;

import com.picard.shipping.domain.*;
import com.picard.shipping.dto.*;
import com.picard.shipping.exception.ConflictException;
import com.picard.shipping.exception.InvalidStateException;
import com.picard.shipping.exception.NotFoundException;
import com.picard.shipping.repository.PackageRepository;
import com.picard.shipping.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final PackageRepository packageRepository;
    private final OrderService orderService;

    /** 2.2 Sendung aus Auftrag erzeugen: max one shipment per order, MVP = exactly one package. */
    public ShipmentResponse createShipmentForOrder(UUID orderId) {
        Order order = orderService.getOrderEntity(orderId);

        if (order.getShipment() != null) {
            throw new ConflictException("Order " + orderId + " already has a shipment");
        }

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setStatus(ShipmentStatus.CREATED);

        // MVP: exactly one package per shipment, no bin-packing/optimization.
        ShipmentPackage pkg = new ShipmentPackage();
        shipment.addPackage(pkg);

        order.setShipment(shipment);
        Shipment saved = shipmentRepository.save(shipment);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipment(UUID id) {
        return toResponse(getShipmentEntity(id));
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> search(ShipmentStatus status, String carrier) {
        return shipmentRepository.search(status, carrier).stream()
                .map(this::toResponse)
                .toList();
    }

    /** 2.3 Paket labeln: sets tracking code + carrier, enforces system-wide unique tracking code. */
    public ShipmentResponse labelPackage(UUID shipmentId, UUID packageId, LabelPackageRequest request) {
        Shipment shipment = getShipmentEntity(shipmentId);

        ShipmentPackage pkg = shipment.getPackages().stream()
                .filter(p -> p.getId().equals(packageId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Package " + packageId + " not found in shipment " + shipmentId));

        if (packageRepository.existsByTrackingCode(request.trackingCode())) {
            throw new ConflictException("Tracking code '" + request.trackingCode() + "' is already in use");
        }

        pkg.setTrackingCode(request.trackingCode());
        pkg.setCarrier(request.carrier());
        return toResponse(shipment);
    }

    /** 2.4 Statusübergang CREATED -> PACKED. */
    public ShipmentResponse markPacked(UUID shipmentId) {
        Shipment shipment = getShipmentEntity(shipmentId);

        if (shipment.getStatus() != ShipmentStatus.CREATED) {
            throw new InvalidStateException(
                    "Shipment must be in status CREATED to be packed, but was " + shipment.getStatus());
        }

        shipment.setStatus(ShipmentStatus.PACKED);
        return toResponse(shipment);
    }

    /**
     * 2.4 Versand bestätigen: PACKED -> SHIPPED.
     * Only allowed when the shipment is PACKED and all packages have a tracking code.
     */
    public ShipmentResponse confirmShipment(UUID shipmentId) {
        Shipment shipment = getShipmentEntity(shipmentId);

        if (shipment.getStatus() != ShipmentStatus.PACKED) {
            throw new InvalidStateException(
                    "Shipment must be in status PACKED to be shipped, but was " + shipment.getStatus());
        }
        if (!shipment.allPackagesHaveTracking()) {
            throw new InvalidStateException(
                    "All packages must have a tracking code before shipment can be confirmed");
        }

        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setShippedAt(Instant.now());
        return toResponse(shipment);
    }

    private Shipment getShipmentEntity(UUID id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Shipment not found: " + id));
    }

    private ShipmentResponse toResponse(Shipment shipment) {
        List<PackageDto> packages = shipment.getPackages().stream()
                .map(p -> new PackageDto(p.getId(), p.getTrackingCode(), p.getCarrier()))
                .toList();

        return new ShipmentResponse(
                shipment.getId(),
                shipment.getOrder().getId(),
                shipment.getOrder().getExternalOrderNumber(),
                shipment.getStatus(),
                shipment.getShippedAt(),
                packages
        );
    }
}

package com.picard.shipping.service;

import com.picard.shipping.domain.*;
import com.picard.shipping.dto.LabelPackageRequest;
import com.picard.shipping.dto.ShipmentResponse;
import com.picard.shipping.exception.ConflictException;
import com.picard.shipping.exception.InvalidStateException;
import com.picard.shipping.exception.NotFoundException;
import com.picard.shipping.repository.PackageRepository;
import com.picard.shipping.repository.ShipmentRepository;
import com.picard.shipping.service.OrderService;
import com.picard.shipping.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    ShipmentRepository shipmentRepository;
    @Mock
    PackageRepository packageRepository;
    @Mock
    OrderService orderService;

    @InjectMocks
    ShipmentService shipmentService;

    private Order orderWithoutShipment() {
        Order order = new Order();
        order.setExternalOrderNumber("ORD-1");
        order.setStatus(OrderStatus.CREATED);
        return order;
    }

    private Shipment shipmentInStatus(ShipmentStatus status) {
        Order order = orderWithoutShipment();
        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setStatus(status);
        shipment.setPackages(new ArrayList<>());
        return shipment;
    }

    @Test
    void createShipmentForOrder_orderAlreadyHasShipment_throwsConflict() {
        UUID orderId = UUID.randomUUID();
        Order order = orderWithoutShipment();
        Shipment existing = new Shipment();
        order.setShipment(existing);
        given(orderService.getOrderEntity(orderId)).willReturn(order);

        assertThatThrownBy(() -> shipmentService.createShipmentForOrder(orderId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createShipmentForOrder_happyPath_createsShipmentWithPackage() {
        UUID orderId = UUID.randomUUID();
        Order order = orderWithoutShipment();
        given(orderService.getOrderEntity(orderId)).willReturn(order);

        Shipment saved = new Shipment();
        saved.setOrder(order);
        saved.setStatus(ShipmentStatus.CREATED);
        ShipmentPackage pkg = new ShipmentPackage();
        saved.addPackage(pkg);
        given(shipmentRepository.save(any(Shipment.class))).willReturn(saved);

        ShipmentResponse response = shipmentService.createShipmentForOrder(orderId);
        assertThat(response.status()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(response.packages()).hasSize(1);
    }

    @Test
    void markPacked_notCreatedStatus_throwsInvalidState() {
        UUID id = UUID.randomUUID();
        Shipment shipment = shipmentInStatus(ShipmentStatus.PACKED);
        given(shipmentRepository.findById(id)).willReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.markPacked(id))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void markPacked_createdStatus_transitionsToPacked() {
        UUID id = UUID.randomUUID();
        Shipment shipment = shipmentInStatus(ShipmentStatus.CREATED);
        given(shipmentRepository.findById(id)).willReturn(Optional.of(shipment));

        ShipmentResponse response = shipmentService.markPacked(id);
        assertThat(response.status()).isEqualTo(ShipmentStatus.PACKED);
    }

    @Test
    void confirmShipment_notPackedStatus_throwsInvalidState() {
        UUID id = UUID.randomUUID();
        Shipment shipment = shipmentInStatus(ShipmentStatus.CREATED);
        given(shipmentRepository.findById(id)).willReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.confirmShipment(id))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void confirmShipment_packedMissingTracking_throwsInvalidState() {
        UUID id = UUID.randomUUID();
        Shipment shipment = shipmentInStatus(ShipmentStatus.PACKED);
        ShipmentPackage pkg = new ShipmentPackage();
        // no tracking code
        shipment.addPackage(pkg);
        given(shipmentRepository.findById(id)).willReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.confirmShipment(id))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void confirmShipment_packedAllTracked_transitionsToShipped() {
        UUID id = UUID.randomUUID();
        Shipment shipment = shipmentInStatus(ShipmentStatus.PACKED);
        ShipmentPackage pkg = new ShipmentPackage();
        pkg.setTrackingCode("TRACK-001");
        shipment.addPackage(pkg);
        given(shipmentRepository.findById(id)).willReturn(Optional.of(shipment));

        ShipmentResponse response = shipmentService.confirmShipment(id);
        assertThat(response.status()).isEqualTo(ShipmentStatus.SHIPPED);
        assertThat(response.shippedAt()).isNotNull();
    }

    @Test
    void labelPackage_duplicateTrackingCode_throwsConflict() {
        UUID shipmentId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        Shipment shipment = shipmentInStatus(ShipmentStatus.CREATED);
        ShipmentPackage pkg = new ShipmentPackage();
        pkg.setId(packageId);
        shipment.addPackage(pkg);
        given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(shipment));
        given(packageRepository.existsByTrackingCode("TRACK-DUP")).willReturn(true);

        assertThatThrownBy(() -> shipmentService.labelPackage(shipmentId, packageId,
                new LabelPackageRequest("TRACK-DUP", "DHL")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void labelPackage_packageNotInShipment_throwsNotFound() {
        UUID shipmentId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        Shipment shipment = shipmentInStatus(ShipmentStatus.CREATED);
        given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.labelPackage(shipmentId, packageId,
                new LabelPackageRequest("TRACK-1", "DHL")))
                .isInstanceOf(NotFoundException.class);
    }
}

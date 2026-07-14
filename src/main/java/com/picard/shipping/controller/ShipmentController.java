package com.picard.shipping.controller;

import com.picard.shipping.domain.ShipmentStatus;
import com.picard.shipping.dto.LabelPackageRequest;
import com.picard.shipping.dto.ShipmentResponse;
import com.picard.shipping.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping("/order/{orderId}")
    public ResponseEntity<ShipmentResponse> createShipmentForOrder(@PathVariable UUID orderId) {
        ShipmentResponse response = shipmentService.createShipmentForOrder(orderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ShipmentResponse getShipment(@PathVariable UUID id) {
        return shipmentService.getShipment(id);
    }

    @GetMapping
    public List<ShipmentResponse> search(
            @RequestParam(required = false) ShipmentStatus status,
            @RequestParam(required = false) String carrier) {
        return shipmentService.search(status, carrier);
    }

    @PatchMapping("/{shipmentId}/packages/{packageId}")
    public ShipmentResponse labelPackage(
            @PathVariable UUID shipmentId,
            @PathVariable UUID packageId,
            @Valid @RequestBody LabelPackageRequest request) {
        return shipmentService.labelPackage(shipmentId, packageId, request);
    }

    @PostMapping("/{id}/pack")
    public ShipmentResponse pack(@PathVariable UUID id) {
        return shipmentService.markPacked(id);
    }

    @PostMapping("/{id}/ship")
    public ShipmentResponse ship(@PathVariable UUID id) {
        return shipmentService.confirmShipment(id);
    }
}

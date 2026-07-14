package com.picard.shipping.exception;

/**
 * Used for all "already exists" / "duplicate" style conflicts,
 * e.g. duplicate externalOrderNumber, duplicate trackingCode,
 * or attempting to create a second shipment for an order.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

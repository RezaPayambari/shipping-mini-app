package com.picard.shipping.exception;

/**
 * Used when a requested state transition or business rule is violated,
 * e.g. confirming shipment before all packages have tracking codes.
 */
public class InvalidStateException extends RuntimeException {
    public InvalidStateException(String message) {
        super(message);
    }
}

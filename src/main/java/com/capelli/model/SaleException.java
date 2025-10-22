package com.capelli.model;

/**
 * ==========================================
 * EXCEPCIONES ESPECÍFICAS
 * ==========================================
 *
 * Excepción cuando hay problema con una venta
 */
public class SaleException extends Exception {
    public SaleException(String message) {
        super(message);
    }

    public SaleException(String message, Throwable cause) {
        super(message, cause);
    }
}

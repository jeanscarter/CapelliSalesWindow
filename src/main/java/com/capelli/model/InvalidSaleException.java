package com.capelli.model;

/**
 * Excepción cuando hay datos inválidos en la venta
 */
public class InvalidSaleException extends SaleException {
    public InvalidSaleException(String reason) {
        super("Venta inválida: " + reason);
    }
}

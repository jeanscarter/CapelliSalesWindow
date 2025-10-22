package com.capelli.model;

/**
 * Excepción cuando la venta no existe
 */
public class SaleNotFoundException extends SaleException {
    public SaleNotFoundException(long saleId) {
        super("Venta no encontrada: " + saleId);
    }
}

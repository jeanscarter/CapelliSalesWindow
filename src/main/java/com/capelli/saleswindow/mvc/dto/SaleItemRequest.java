package com.capelli.saleswindow.mvc.dto;

/**
 * ============================================
 * DTO - SaleItemRequest
 * ============================================
 * DTO que representa un único ítem (servicio) dentro de una solicitud de venta.
 */
public class SaleItemRequest {
    private final int serviceId;
    private final int employeeId;
    private final double price;
    
    public SaleItemRequest(int serviceId, int employeeId, double price) {
        this.serviceId = serviceId;
        this.employeeId = employeeId;
        this.price = price;
    }
    
    public int getServiceId() { return serviceId; }
    public int getEmployeeId() { return employeeId; }
    public double getPrice() { return price; }
}

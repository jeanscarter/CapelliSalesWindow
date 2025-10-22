package com.capelli.model;

import java.io.Serializable;

/**
 * ==========================================
 * CLASE RELACIONADA: SaleItem
 * ==========================================
 *
 * SaleItem - Representa un servicio en una venta
 *
 * Relación:
 * Sale 1 ----> * SaleItem
 *
 * Cada Sale puede tener múltiples SaleItem
 * Cada SaleItem pertenece a una Sale
 */
public class SaleItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID único del item (generado por BD)
     */
    private long saleItemId;

    /**
     * ID de la venta a la que pertenece
     * FOREIGN KEY → sales.sale_id
     */
    private long saleId;

    /**
     * ID del servicio
     * FOREIGN KEY → services.service_id
     */
    private int serviceId;

    /**
     * ID de la empleada que realizó el servicio
     * FOREIGN KEY → trabajadoras.id
     */
    private int employeeId;

    /**
     * Precio del servicio en el momento de la venta
     * Se guarda para histórico (en caso de cambios de precio)
     * Siempre en USD
     */
    private double priceAtSale;

    // ==========================================
    // CONSTRUCTORES
    // ==========================================

    public SaleItem() {
    }

    public SaleItem(long saleId, int serviceId, int employeeId, double priceAtSale) {
        this.saleId = saleId;
        this.serviceId = serviceId;
        this.employeeId = employeeId;
        this.priceAtSale = priceAtSale;
    }

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================

    public long getSaleItemId() {
        return saleItemId;
    }

    public void setSaleItemId(long saleItemId) {
        this.saleItemId = saleItemId;
    }

    public long getSaleId() {
        return saleId;
    }

    public void setSaleId(long saleId) {
        this.saleId = saleId;
    }

    public int getServiceId() {
        return serviceId;
    }

    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public double getPriceAtSale() {
        return priceAtSale;
    }

    public void setPriceAtSale(double priceAtSale) {
        this.priceAtSale = priceAtSale;
    }

    // ==========================================
    // MÉTODOS ÚTILES
    // ==========================================

    @Override
    public String toString() {
        return "SaleItem{" +
                "id=" + saleItemId +
                ", saleId=" + saleId +
                ", serviceId=" + serviceId +
                ", employeeId=" + employeeId +
                ", price=$" + String.format("%.2f", priceAtSale) +
                '}';
    }
}

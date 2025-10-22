package com.capelli.saleswindow.mvc.dto;

import java.util.List;

/**
 * ============================================
 * DTO - SaleRequest
 * ============================================
 * Data Transfer Object (DTO) para encapsular todos los datos necesarios
 * para crear una nueva venta. Se utiliza para pasar información de manera
 * estructurada desde el Controlador hacia la capa de Servicio.
 */
public class SaleRequest {
    private final int clientId;
    private final List<SaleItemRequest> items;
    private final double subtotal;
    private final double discount;
    private final String discountType;
    private final double tip;
    private final String tipRecipient;
    private final double total;
    private final double paidAmount;
    private final String paymentMethod;
    private final String paymentDestination;
    private final String currency;
    
    public SaleRequest(int clientId, List<SaleItemRequest> items, double subtotal,
                       double discount, String discountType, double tip,
                       String tipRecipient, double total, double paidAmount,
                       String paymentMethod, String paymentDestination, String currency) {
        this.clientId = clientId;
        this.items = items;
        this.subtotal = subtotal;
        this.discount = discount;
        this.discountType = discountType;
        this.tip = tip;
        this.tipRecipient = tipRecipient;
        this.total = total;
        this.paidAmount = paidAmount;
        this.paymentMethod = paymentMethod;
        this.paymentDestination = paymentDestination;
        this.currency = currency;
    }
    
    // Getters
    public int getClientId() { return clientId; }
    public List<SaleItemRequest> getItems() { return items; }
    public double getSubtotal() { return subtotal; }
    public double getDiscount() { return discount; }
    public String getDiscountType() { return discountType; }
    public double getTip() { return tip; }
    public String getTipRecipient() { return tipRecipient; }
    public double getTotal() { return total; }
    public double getPaidAmount() { return paidAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentDestination() { return paymentDestination; }
    public String getCurrency() { return currency; }
}

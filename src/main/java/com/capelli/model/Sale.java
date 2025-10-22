package com.capelli.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.logging.Logger;


public class Sale implements Serializable, Comparable<Sale> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(Sale.class.getName());

    // ==========================================
    // ATRIBUTOS - IDENTIFICACIÓN
    // ==========================================

    /**
     * ID único de la venta (generado por BD)
     * PRIMARY KEY
     */
    private long saleId;

    /**
     * ID del cliente que realizó la compra
     * FOREIGN KEY → clients.client_id
     * Puede ser null si es cliente anónimo
     */
    private Integer clientId;

    // ==========================================
    // ATRIBUTOS - FECHA Y HORA
    // ==========================================

    /**
     * Fecha y hora en que se realizó la venta
     * DEFAULT: CURRENT_TIMESTAMP en BD
     * Formato: 2025-10-21T18:30:45
     */
    private LocalDateTime saleDate;

    // ==========================================
    // ATRIBUTOS - MONTOS
    // ==========================================

    /**
     * Suma de todos los servicios sin descuento ni propina
     * Ejemplo: 2 servicios: $50 + $30 = $80
     * Siempre en USD
     */
    private double subtotal;

    /**
     * Tipo de descuento aplicado
     * Valores: "Ninguno", "Promoción", "Intercambio",
     * "Cuenta por pagar", "Cuenta por Cobrar"
     */
    private String discountType;

    /**
     * Monto de descuento aplicado
     * Ejemplo: si descuento es 20%, amount = $16
     * Siempre en USD
     */
    private double discountAmount;

    /**
     * Total a pagar (subtotal - descuento)
     * NO incluye propina
     * Siempre en USD
     *
     * Fórmula: total = subtotal - discountAmount
     */
    private double total;

    // ==========================================
    // ATRIBUTOS - PAGO
    // ==========================================

    /**
     * Método de pago utilizado
     * Valores: "TD" (Tarjeta Débito)
     * "TC" (Tarjeta Crédito)
     * "Pago Movil"
     * "Efectivo $"
     * "Efectivo Bs"
     * "Transferencia"
     */
    private String paymentMethod;

    /**
     * Moneda en que se realizó el pago
     * Valores: "$" (USD/Dólares) o "Bs" (Bolívares)
     */
    private String currency;

    /**
     * Destino del pago (para Pago Móvil)
     * Valores: "Capelli", "Rosa", o null
     * Solo se usa si paymentMethod == "Pago Movil"
     */
    private String paymentDestination;

    // ==========================================
    // CONSTRUCTORES
    // ==========================================

    /**
     * Constructor vacío (para BD/serialización)
     */
    public Sale() {
    }

    /**
     * Constructor básico con datos principales
     */
    public Sale(long saleId, Integer clientId, LocalDateTime saleDate,
                double subtotal, double total, String paymentMethod) {
        this.saleId = saleId;
        this.clientId = clientId;
        this.saleDate = saleDate;
        this.subtotal = subtotal;
        this.total = total;
        this.paymentMethod = paymentMethod;
        this.currency = "$"; // Por defecto USD
    }

    /**
     * Constructor completo con todos los datos
     */
    public Sale(
            long saleId,
            Integer clientId,
            LocalDateTime saleDate,
            double subtotal,
            String discountType,
            double discountAmount,
            double total,
            String paymentMethod,
            String currency,
            String paymentDestination) {

        this.saleId = saleId;
        this.clientId = clientId;
        this.saleDate = saleDate;
        this.subtotal = subtotal;
        this.discountType = discountType;
        this.discountAmount = discountAmount;
        this.total = total;
        this.paymentMethod = paymentMethod;
        this.currency = currency;
        this.paymentDestination = paymentDestination;
    }

    // ==========================================
    // GETTERS
    // ==========================================

    public long getSaleId() {
        return saleId;
    }

    public Integer getClientId() {
        return clientId;
    }

    public LocalDateTime getSaleDate() {
        return saleDate;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public String getDiscountType() {
        return discountType;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getTotal() {
        return total;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPaymentDestination() {
        return paymentDestination;
    }

    // ==========================================
    // SETTERS
    // ==========================================

    public void setSaleId(long saleId) {
        this.saleId = saleId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public void setSaleDate(LocalDateTime saleDate) {
        this.saleDate = saleDate;
    }

    public void setSubtotal(double subtotal) {
        if (subtotal < 0) {
            LOGGER.warning("Subtotal negativo establecido: " + subtotal);
        }
        this.subtotal = subtotal;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public void setDiscountAmount(double discountAmount) {
        if (discountAmount < 0) {
            LOGGER.warning("Descuento negativo establecido: " + discountAmount);
        }
        this.discountAmount = discountAmount;
    }

    public void setTotal(double total) {
        if (total < 0) {
            LOGGER.warning("Total negativo establecido: " + total);
        }
        this.total = total;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setPaymentDestination(String paymentDestination) {
        this.paymentDestination = paymentDestination;
    }

    // ==========================================
    // MÉTODOS DE UTILIDAD
    // ==========================================

    /**
     * Obtener información breve para logs/debugging
     */
    @Override
    public String toString() {
        return "Sale{" +
                "id=" + saleId +
                ", clientId=" + clientId +
                ", date=" + saleDate +
                ", total=$" + String.format("%.2f", total) +
                ", method='" + paymentMethod + '\'' +
                ", currency='" + currency + '\'' +
                '}';
    }

    /**
     * Comparar ventas por ID
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Sale sale = (Sale) o;
        return saleId == sale.saleId;
    }

    /**
     * Hash de la venta
     */
    @Override
    public int hashCode() {
        return Objects.hash(saleId);
    }

    /**
     * Comparar ventas por fecha (más recientes primero)
     * Usado para ordenar listados
     */
    @Override
    public int compareTo(Sale other) {
        if (this.saleDate == null || other.saleDate == null) {
            return 0;
        }
        // Más recientes primero (descendente)
        return other.saleDate.compareTo(this.saleDate);
    }

    /**
     * ¿Es una venta nueva? (sin ID)
     */
    public boolean isNew() {
        return saleId == 0;
    }

    /**
     * ¿Es venta en USD?
     */
    public boolean isUSD() {
        return "$".equals(currency);
    }

    /**
     * ¿Es venta en Bolívares?
     */
    public boolean isBolívares() {
        return "Bs".equals(currency);
    }

    /**
     * ¿Es cliente anónimo?
     */
    public boolean isAnonymousClient() {
        return clientId == null || clientId == 0;
    }

    /**
     * ¿Tiene descuento?
     */
    public boolean hasDiscount() {
        return discountAmount > 0;
    }

    /**
     * ¿Es Pago Móvil?
     */
    public boolean isPaymentMobile() {
        return "Pago Movil".equals(paymentMethod);
    }

    /**
     * ¿Es cuenta por cobrar?
     */
    public boolean isAccountsReceivable() {
        return "Cuenta por Cobrar".equals(discountType);
    }

    /**
     * Calcular descuento en porcentaje
     * Retorna: 0-100
     */
    public double getDiscountPercentage() {
        if (subtotal == 0) {
            return 0;
        }
        return (discountAmount / subtotal) * 100;
    }

    /**
     * Obtener descripción del tipo de descuento
     */
    public String getDiscountDescription() {
        if (discountType == null || "Ninguno".equals(discountType)) {
            return "Sin descuento";
        }
        return discountType + " (-$" + String.format("%.2f", discountAmount) + ")";
    }

    /**
     * Obtener descripción del método de pago
     */
    public String getPaymentMethodDescription() {
        StringBuilder desc = new StringBuilder(paymentMethod);

        if ("Pago Movil".equals(paymentMethod) && paymentDestination != null) {
            desc.append(" (").append(paymentDestination).append(")");
        }

        return desc.toString();
    }

    /**
     * Obtener descripción completa de la venta
     */
    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();

        sb.append("=== RESUMEN DE VENTA ===\n");
        sb.append("ID Venta: ").append(saleId).append("\n");
        sb.append("Fecha: ").append(saleDate).append("\n");

        if (!isAnonymousClient()) {
            sb.append("Cliente ID: ").append(clientId).append("\n");
        } else {
            sb.append("Cliente: Anónimo\n");
        }

        sb.append("\n--- MONTOS ---\n");
        sb.append("Subtotal: $").append(String.format("%.2f", subtotal)).append("\n");

        if (hasDiscount()) {
            sb.append("Descuento: ").append(getDiscountDescription()).append("\n");
        }

        sb.append("Total: $").append(String.format("%.2f", total));
        sb.append(" (").append(currency).append(")\n");

        sb.append("\n--- PAGO ---\n");
        sb.append("Método: ").append(getPaymentMethodDescription()).append("\n");

        return sb.toString();
    }

    /**
     * Validar que los datos sean consistentes
     */
    public boolean validateConsistency() {
        // El total debe ser subtotal - descuento
        double expectedTotal = subtotal - discountAmount;

        // Permite pequeña tolerancia para errores de punto flotante
        return Math.abs(total - expectedTotal) < 0.01;
    }

    /**
     * ¿Es una venta válida?
     */
    public boolean isValid() {
        return subtotal >= 0 &&
                discountAmount >= 0 &&
                total > 0 &&
                paymentMethod != null && !paymentMethod.isEmpty() &&
                currency != null && !currency.isEmpty() &&
                validateConsistency();
    }

    /**
     * Recalcular total basado en subtotal y descuento
     * Útil después de cambiar subtotal o descuento
     */
    public void recalculateTotal() {
        this.total = this.subtotal - this.discountAmount;

        if (this.total < 0) {
            LOGGER.warning("Total calculado negativo, estableciendo a 0");
            this.total = 0;
        }
    }

    /**
     * Aplicar descuento por porcentaje
     *
     * @param percentage Porcentaje (0-100)
     */
    public void applyDiscountPercentage(double percentage) {
        if (percentage < 0 || percentage > 100) {
            LOGGER.warning("Porcentaje de descuento inválido: " + percentage);
            return;
        }

        this.discountAmount = (subtotal * percentage) / 100;
        recalculateTotal();

        LOGGER.info(String.format("Descuento %f%% aplicado a venta %d",
                percentage, saleId));
    }

    /**
     * Obtener hora de la venta (formato: HH:mm:ss)
     */
    public String getSaleTime() {
        if (saleDate == null) {
            return "N/A";
        }
        return saleDate.toLocalTime().toString();
    }

    /**
     * Obtener fecha de la venta (formato: dd/MM/yyyy)
     */
    public String getSaleDateFormatted() {
        if (saleDate == null) {
            return "N/A";
        }
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return saleDate.format(formatter);
    }

    /**
     * ¿Fue hace menos de X horas?
     */
    public boolean wasWithinHours(int hours) {
        if (saleDate == null) {
            return false;
        }

        LocalDateTime threshold = LocalDateTime.now().minusHours(hours);
        return saleDate.isAfter(threshold);
    }

    /**
     * ¿Fue hace menos de X días?
     */
    public boolean wasWithinDays(int days) {
        if (saleDate == null) {
            return false;
        }

        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        return saleDate.isAfter(threshold);
    }
}

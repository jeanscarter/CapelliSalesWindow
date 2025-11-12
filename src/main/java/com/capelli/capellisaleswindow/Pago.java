package com.capelli.capellisaleswindow;

public record Pago(
    String metodo,
    String moneda,
    double montoMoneda,
    double montoUSD,
    double tasaBcv,
    String destino,
    String referencia
) {}
package com.capelli.validation;

import com.capelli.config.AppConfig; // Necesario para obtener el porcentaje de IVA
import java.util.List;
import java.util.Map;

/**
 * Validador para operaciones de venta.
 */
public class VentaValidator {
    
    /**
     * DTO simple para representar un servicio en la venta.
     */
    public static class ServicioVenta {
        private final String servicio;
        private final String trabajadora;
        private final double precio;
        
        public ServicioVenta(String servicio, String trabajadora, double precio) {
            this.servicio = servicio;
            this.trabajadora = trabajadora;
            this.precio = precio;
        }
        
        public String getServicio() { return servicio; }
        public String getTrabajadora() { return trabajadora; }
        public double getPrecio() { return precio; }
    }
    
    /**
     * Valida una venta completa antes de procesarla.
     * @param iva El monto de IVA calculado
     * @param totalPagadoEnDolares La suma de todos los pagos (ya convertidos a $)
     */
    public static ValidationResult validateVenta(
            List<ServicioVenta> servicios,
            double subtotal,
            double descuento,
            double iva, 
            double propina,
            double total, // Total en $
            double totalPagadoEnDolares, // NUEVO PARÁMETRO
            String tipoDescuento) {
        
        ValidationResult result = new ValidationResult();
        
        // 1. Validar que hay servicios
        CommonValidators.validateNotEmpty(servicios, "Servicios", result);
        if (servicios == null || servicios.isEmpty()) {
            // Si no hay servicios, no tiene sentido seguir validando
            return result;
        }
        
        // 2. Validar cada servicio
        for (int i = 0; i < servicios.size(); i++) {
            ServicioVenta servicio = servicios.get(i);
            ValidationResult servicioResult = validateServicio(servicio, i + 1);
            result.merge(servicioResult);
        }
        
        // 3. Validar montos
        CommonValidators.validateNonNegative(subtotal, "Subtotal", result);
        CommonValidators.validateNonNegative(descuento, "Descuento", result);
        CommonValidators.validateNonNegative(iva, "IVA", result);
        CommonValidators.validateNonNegative(propina, "Propina", result);
        CommonValidators.validateNonNegative(total, "Total", result);
        
        // 4. Validar que el subtotal sea coherente
        double subtotalCalculado = servicios.stream()
                .mapToDouble(ServicioVenta::getPrecio)
                .sum();
        
        if (Math.abs(subtotalCalculado - subtotal) > 0.01) {
            result.addError("Subtotal", 
                String.format("El subtotal no coincide con la suma de servicios (Esperado: %.2f, Actual: %.2f)", 
                    subtotalCalculado, subtotal));
        }

        // --- INICIO DE MODIFICACIÓN IVA/DESCUENTO ---
        // 5. Separar subtotales para validación de IVA y Descuento
        double subtotalGravable = 0.0;
        double subtotalNoGravable = 0.0;
        for (ServicioVenta vs : servicios) {
            // DEBE USAR EL MISMO NOMBRE QUE EN CapelliSalesWindow.java
            if ("Abono Manual Staff".equals(vs.getServicio())) {
                subtotalNoGravable += vs.getPrecio();
            } else {
                subtotalGravable += vs.getPrecio();
            }
        }

        // 6. Validar que el descuento no sea mayor que el subtotal (gravable)
        if (tipoDescuento.equals("Promoción") && descuento > subtotalGravable) {
            result.addError("Descuento", 
                "El descuento de promoción no puede ser mayor que el subtotal gravable");
        } else if (descuento > subtotal) {
            result.addError("Descuento", 
                "El descuento no puede ser mayor que el subtotal total");
        }
        
        // 7. Validar que el total sea correcto
        // El IVA se calcula sobre la parte gravable, después del descuento.
        // El subtotalNoGravable no tiene descuento ni IVA.
        double ivaCalculado = (subtotalGravable - descuento) * AppConfig.getVatPercentage();
        
        // Si el IVA que llegó de la ventana es 0 (porque usaron CTRL+I), 
        // nuestra validación debe respetarlo.
        if (iva == 0.0) {
            ivaCalculado = 0.0; // Se asume que el usuario excluyó el IVA manualmente
        }

        if (Math.abs(ivaCalculado - iva) > 0.01) {
             result.addError("IVA", 
                String.format("El IVA no coincide con el cálculo (Esperado: %.2f, Actual: %.2f)", 
                    ivaCalculado, iva));
        }

        double totalCalculado = (subtotalGravable - descuento) + subtotalNoGravable + iva + propina;
        if (Math.abs(totalCalculado - total) > 0.01) {
            result.addError("Total", 
                String.format("El total no es correcto (Esperado: %.2f, Actual: %.2f)", 
                    totalCalculado, total));
        }
        // --- FIN DE MODIFICACIÓN ---
        
        // 8. Validar tipo de descuento
        CommonValidators.validateNotEmpty(tipoDescuento, "Tipo de descuento", result);
        
        // 9. Validar monto pagado (excepto para cuentas por cobrar)
        double Epsilon = 0.01; // Tolerancia para punto flotante
        
        if (!"Cuenta por Cobrar".equals(tipoDescuento)) {
            // Comparamos el total pagado (suma de todos los pagos) con el total de la factura
            if (totalPagadoEnDolares < (total - Epsilon)) { 
                result.addError("Monto pagado", 
                    String.format("El monto total pagado (%.2f) es insuficiente. Total: %.2f", 
                        totalPagadoEnDolares, total));
            }
        } else {
            // Si es cuenta por cobrar, agregar advertencia
            result.addWarning("Método de pago", 
                "Esta venta quedará registrada como cuenta por cobrar");
        }
        
        // 10. Validar propina si existe
        if (propina > 0) {
            // Advertencia si la propina es muy alta (más del 30% del subtotal)
            if (propina > subtotal * 0.30) {
                result.addWarning("Propina", 
                    String.format("La propina (%.2f) es mayor al 30%% del subtotal", propina));
            }
        }
        
        return result;
    }
    
    /**
     * Valida un servicio individual.
     */
    public static ValidationResult validateServicio(ServicioVenta servicio, int numero) {
        ValidationResult result = new ValidationResult();
        String prefix = "Servicio " + numero;
        
        if (servicio == null) {
            result.addError(prefix, "El servicio no puede ser nulo");
            return result;
        }
        
        // Validar nombre del servicio
        CommonValidators.validateNotEmpty(servicio.getServicio(), prefix + " - Nombre", result);
        
        // Validar trabajadora
        CommonValidators.validateNotEmpty(servicio.getTrabajadora(), prefix + " - Trabajadora", result);
        
        // Validar precio
        // Permitimos 0 o positivo para el Abono Manual, pero positivo para el resto
        if (servicio.getServicio().equals("Abono Manual Staff")) {
            CommonValidators.validateNonNegative(servicio.getPrecio(), prefix + " - Precio", result);
        } else {
            CommonValidators.validatePositive(servicio.getPrecio(), prefix + " - Precio", result);
        }

        // Advertencia si el precio es muy bajo (pero mayor a 0)
        if (servicio.getPrecio() > 0 && servicio.getPrecio() < 1.0) {
            result.addWarning(prefix + " - Precio", 
                String.format("El precio (%.2f) es muy bajo, ¿es correcto?", servicio.getPrecio()));
        }
        
        // Advertencia si el precio es muy alto
        if (servicio.getPrecio() > 500.0) {
            result.addWarning(prefix + " - Precio", 
                String.format("El precio (%.2f) es muy alto, ¿es correcto?", servicio.getPrecio()));
        }
        
        return result;
    }
    
    /**
     * Valida el monto de propina.
     */
    public static ValidationResult validatePropina(double propina, String destinatario) {
        ValidationResult result = new ValidationResult();
        
        if (propina > 0) {
            CommonValidators.validatePositive(propina, "Propina", result);
            CommonValidators.validateNotEmpty(destinatario, "Destinatario de propina", result);
        }
        
        return result;
    }
    
    /**
     * Valida el método de pago según el tipo (al momento de agregarlo).
     */
    public static ValidationResult validateMetodoPago(String metodoPago, String moneda, 
                                                      String destinoPago) {
        ValidationResult result = new ValidationResult();
        
        CommonValidators.validateNotEmpty(metodoPago, "Método de pago", result);
        CommonValidators.validateNotEmpty(moneda, "Moneda", result);
        
        // Validar moneda
        if (CommonValidators.isNotEmpty(moneda)) {
            if (!"$".equals(moneda) && !"Bs".equals(moneda)) {
                result.addError("Moneda", "La moneda debe ser ' o 'Bs'");
            }
        }

        // Validar destino si es Pago Móvil en Bs
        if ("Pago Movil".equals(metodoPago) && "Bs".equals(moneda)) {
            CommonValidators.validateNotEmpty(destinoPago, "Destino de Pago Móvil", result);
            
            if (CommonValidators.isNotEmpty(destinoPago)) {
                if (!"Capelli".equals(destinoPago) && !"Rosa".equals(destinoPago)) {
                    result.addError("Destino de Pago Móvil", 
                        "El destino debe ser 'Capelli' o 'Rosa'");
                }
            }
        }

        // Validar destino si es Transferencia en $
        if ("Transferencia".equals(metodoPago) && "$".equals(moneda)) {
            CommonValidators.validateNotEmpty(destinoPago, "Destino de Transferencia", result);
            if (CommonValidators.isNotEmpty(destinoPago)) {
                if (!"@hotmail".equals(destinoPago) && !"@Gmail".equals(destinoPago) && !"Ingrid".equals(destinoPago)) {
                        result.addError("Destino de Transferencia", "Destino inválido");
                }
            }
        }
        
        return result;
    }
    
    /**
     * Valida que no haya duplicados de servicios (advertencia).
     */
    public static ValidationResult checkDuplicateServices(List<ServicioVenta> servicios) {
        ValidationResult result = new ValidationResult();
        
        Map<String, Long> servicioCount = new java.util.HashMap<>();
        
        for (ServicioVenta servicio : servicios) {
            // El abono manual SÍ se puede repetir (uno por trabajadora)
            if (servicio.getServicio().equals("Abono Manual Staff")) {
                continue;
            }
            
            String key = servicio.getServicio() + "-" + servicio.getTrabajadora();
            servicioCount.put(key, servicioCount.getOrDefault(key, 0L) + 1);
        }
        
        servicioCount.forEach((key, count) -> {
            if (count > 1) {
                String[] parts = key.split("-");
                result.addWarning("Servicios duplicados", 
                    String.format("El servicio '%s' con '%s' está repetido %d veces", 
                        parts[0], parts[1], count));
            }
        });
        
        return result;
    }
    
    /**
     * Valida límites de descuento según el tipo.
     */
    public static ValidationResult validateDescuento(String tipoDescuento, double descuento, 
                                                     double subtotalGravable) { // Modificado para recibir el subtotal gravable
        ValidationResult result = new ValidationResult();
        
        if (descuento == 0) {
            return result; // Sin descuento, todo OK
        }
        
        CommonValidators.validateNotEmpty(tipoDescuento, "Tipo de descuento", result);
        
        if ("Ninguno".equals(tipoDescuento) && descuento > 0) {
            result.addError("Descuento", 
                "No puede haber descuento si el tipo es 'Ninguno'");
        }
        
        if ("Promoción".equals(tipoDescuento)) {
            // Validar que el descuento sea aproximadamente 20% del subtotal GRAVABLE
            double descuentoEsperado = subtotalGravable * AppConfig.getPromoDiscountPercentage();
            if (Math.abs(descuento - descuentoEsperado) > 0.01) {
                result.addWarning("Descuento", 
                    String.format("El descuento de promoción (%.2f) no coincide con el %.2f%% del subtotal gravable (%.2f)", 
                        descuento, (AppConfig.getPromoDiscountPercentage() * 100), descuentoEsperado));
            }
        }
        
        // Validar que el descuento no sea mayor al 50% (sospechoso)
        if (descuento > subtotalGravable * 0.50) {
            result.addWarning("Descuento", 
                String.format("El descuento (%.2f) es mayor al 50%% del subtotal gravable, ¿es correcto?", 
                    descuento));
        }
        
        return result;
    }
}
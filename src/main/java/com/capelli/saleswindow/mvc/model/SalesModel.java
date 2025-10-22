package com.capelli.saleswindow.mvc.model;

import com.capelli.database.ServiceDAO;
import com.capelli.model.Cliente;
import com.capelli.model.Service;
import com.capelli.validation.VentaValidator;
import com.capelli.validation.ValidationResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ============================================
 * MODEL - SalesModel (Lógica de Datos y Cálculos)
 * ============================================
 * Contiene el estado de la venta actual (cliente, servicios agregados) y
 * la lógica para calcular totales, descuentos y realizar validaciones de datos.
 * No interactúa directamente con la Vista.
 */
public class SalesModel {
    private static final Logger LOGGER = Logger.getLogger(SalesModel.class.getName());
    
    // Estado del modelo
    private Cliente clienteActual;
    private List<VentaServicio> serviciosAgregados;
    private Map<String, Service> serviciosCache;
    private double tasaBcv;
    
    // Dependencias
    private final ServiceDAO serviceDAO;
    private final VentaValidator ventaValidator;
    
    // Constructor
    public SalesModel(ServiceDAO serviceDAO, VentaValidator ventaValidator) {
        this.serviceDAO = serviceDAO;
        this.ventaValidator = ventaValidator;
        this.serviciosAgregados = new ArrayList<>();
        this.serviciosCache = new HashMap<>();
        this.tasaBcv = 207.89; // Valor por defecto
    }
    
    // ===== GESTIÓN DE CLIENTE =====
    
    /**
     * Carga un cliente desde el repositorio usando su cédula.
     * @param cedula La cédula del cliente a buscar.
     * @return El objeto Cliente encontrado.
     * @throws ModelException si la cédula es inválida o hay un error en la búsqueda.
     */
    public Cliente loadClientByCedula(String cedula) throws ModelException {
        ValidationResult validation = validateCedula(cedula);
        if (!validation.isValid()) {
            throw new ModelException(validation.getErrorMessage());
        }
        
        try {
            // Suponiendo que existe un ClientRepository estático o inyectado
            // this.clienteActual = ClientRepository.findByCedula(cedula);
            // LOGGER.info("Cliente cargado: " + clienteActual.getNombre());
            // return clienteActual;
            return null; // Placeholder
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar cliente", e);
            throw new ModelException("Error al buscar cliente: " + e.getMessage());
        }
    }
    
    public void setCurrentClient(Cliente cliente) {
        this.clienteActual = cliente;
    }
    
    public Cliente getCurrentClient() {
        return clienteActual;
    }
    
    public void clearClient() {
        this.clienteActual = null;
    }
    
    // ===== GESTIÓN DE SERVICIOS =====
    
    /**
     * Carga todos los servicios, utilizando un caché para mejorar el rendimiento.
     * @return Una lista de todos los servicios disponibles.
     * @throws ModelException si ocurre un error al acceder a la base de datos.
     */
    public List<Service> loadServices() throws ModelException {
        if (!serviciosCache.isEmpty()) {
            LOGGER.fine("Servicios cargados desde caché");
            return new ArrayList<>(serviciosCache.values());
        }
        
        try {
            List<Service> servicios = serviceDAO.getAll();
            servicios.forEach(s -> serviciosCache.put(s.getName(), s));
            LOGGER.info("Servicios cargados desde BD: " + servicios.size());
            return servicios;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar servicios", e);
            throw new ModelException("Error al cargar servicios: " + e.getMessage());
        }
    }
    
    public Service getServiceByName(String nombre) throws ModelException {
        Service service = serviciosCache.get(nombre);
        if (service == null) {
            throw new ModelException("Servicio no encontrado: " + nombre);
        }
        return service;
    }
    
    public void invalidateServiceCache() {
        serviciosCache.clear();
        LOGGER.info("Caché de servicios invalidado");
    }
    
    // ===== GESTIÓN DE SERVICIOS AGREGADOS A LA VENTA =====
    
    /**
     * Agrega un servicio a la lista de la venta actual, calculando su precio.
     * @param nombreServicio El nombre del servicio a agregar.
     * @param trabajadora El nombre de la trabajadora que realizó el servicio.
     * @param precioOverride Un precio manual para sobreescribir el calculado.
     * @throws ModelException si el servicio no se encuentra o hay un error.
     */
    public void addServiceToSale(String nombreServicio, String trabajadora, double precioOverride) throws ModelException {
        try {
            Service service = getServiceByName(nombreServicio);
            double precioFinal = calcularPrecioServicio(service);
            
            if (precioOverride > 0) {
                precioFinal = precioOverride;
            }
            
            // VentaServicio es una clase interna o DTO para manejar los servicios en la venta
            // VentaServicio venta = new VentaServicio(nombreServicio, trabajadora, precioFinal);
            // serviciosAgregados.add(venta);
            
            LOGGER.info("Servicio agregado: " + nombreServicio + " - $" + precioFinal);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al agregar servicio", e);
            throw new ModelException("Error al agregar servicio: " + e.getMessage());
        }
    }
    
    private double calcularPrecioServicio(Service service) {
        // Implementación del cálculo de precio basado en el tipo de cabello
        return service.getPrice_corto(); // Lógica simplificada
    }
    
    public void removeService(int index) throws ModelException {
        if (index < 0 || index >= serviciosAgregados.size()) {
            throw new ModelException("Índice de servicio inválido");
        }
        serviciosAgregados.remove(index);
        LOGGER.info("Servicio eliminado en índice: " + index);
    }
    
    public void updateServicePrice(int index, double newPrice) throws ModelException {
        if (index < 0 || index >= serviciosAgregados.size()) {
            throw new ModelException("Índice de servicio inválido");
        }
        // serviciosAgregados.get(index).setPrecio(newPrice);
        LOGGER.info("Precio actualizado en índice " + index + ": $" + newPrice);
    }
    
    public List<VentaServicio> getAddedServices() {
        return new ArrayList<>(serviciosAgregados);
    }
    
    public void clearAddedServices() {
        serviciosAgregados.clear();
    }
    
    // ===== CÁLCULOS DE VENTA =====
    
    public double calculateSubtotal() {
        return serviciosAgregados.stream()
            // .mapToDouble(VentaServicio::getPrecio)
            .mapToDouble(vs -> 0.0) // Placeholder
            .sum();
    }
    
    public double calculateDiscount(String tipoDescuento, double subtotal) {
        if ("Promoción".equals(tipoDescuento)) {
            return subtotal * 0.20; // 20%
        }
        return 0.0;
    }
    
    public SalesCalculation calculateTotal(double subtotal, double descuento, double propina, boolean enBolívares) {
        double total = subtotal - descuento + propina;
        
        if (enBolívares) {
            total *= tasaBcv;
        }
        
        return new SalesCalculation(subtotal, descuento, propina, total, tasaBcv);
    }
    
    public double calculateChange(double total, double montoPagado) {
        return montoPagado - total;
    }
    
    // ===== VALIDACIONES =====
    
    private ValidationResult validateCedula(String cedula) {
        ValidationResult result = new ValidationResult();
        if (cedula == null || !cedula.matches("^[VEJGPvejgp]-?[0-9]{6,9}$")) {
            result.addError("Cédula inválida. Formato esperado: V-12345678");
        }
        return result;
    }
    
    public ValidationResult validateSale(double subtotal, double descuento, double propina, double total, double montoPagado, String metodoPago, String tipoDescuento) {
        // Lógica de validación de la venta completa
        return ventaValidator.validateVenta(
            null, // Lista de servicios
            subtotal, descuento, propina, total, montoPagado, metodoPago, tipoDescuento
        );
    }
    
    // ===== TASA BCV =====
    
    public void setBcvRate(double rate) {
        this.tasaBcv = rate;
        LOGGER.info("Tasa BCV actualizada: " + rate);
    }
    
    public double getBcvRate() {
        return tasaBcv;
    }
    
    // ===== CLASES INTERNAS Y EXCEPCIONES =====
    
    public static class SalesCalculation {
        public double subtotal, discount, tip, total, bcvRate;
        
        public SalesCalculation(double subtotal, double discount, double tip, double total, double bcvRate) {
            this.subtotal = subtotal;
            this.discount = discount;
            this.tip = tip;
            this.total = total;
            this.bcvRate = bcvRate;
        }
    }
    
    public static class ModelException extends Exception {
        public ModelException(String message) {
            super(message);
        }
        public ModelException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Placeholder para VentaServicio, reemplazar con la clase real
    private static class VentaServicio {
        // ...
    }
}

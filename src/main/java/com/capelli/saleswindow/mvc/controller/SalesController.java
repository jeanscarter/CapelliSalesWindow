package com.capelli.saleswindow.mvc.controller;

import com.capelli.saleswindow.mvc.SalesView;
import com.capelli.saleswindow.mvc.model.SalesModel;
import com.capelli.model.Cliente;
import com.capelli.validation.ValidationResult;
import javax.swing.SwingWorker;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ============================================
 * CONTROLLER - SalesController
 * ============================================
 * Orquesta la comunicación entre el Modelo y la Vista.
 * - Recibe eventos de la Vista (ej. clic de botón).
 * - Llama a los métodos del Modelo para procesar datos.
 * - Llama a los métodos de la Vista para actualizar la UI con los resultados.
 * - Maneja el flujo de la aplicación y la lógica de navegación.
 */
public class SalesController {
    private static final Logger LOGGER = Logger.getLogger(SalesController.class.getName());
    
    private final SalesView view;
    private final SalesModel model;
    
    public SalesController(SalesView view, SalesModel model) {
        this.view = view;
        this.model = model;
    }
    
    // ===== INICIALIZACIÓN =====
    
    /**
     * Inicializa el controlador, cargando datos necesarios como la tasa BCV y la lista de servicios.
     */
    public void initialize() {
        loadBcvRate();
        loadServices();
    }
    
    private void loadBcvRate() {
        new SwingWorker<Double, Void>() {
            @Override
            protected Double doInBackground() throws Exception {
                // return BCVService.getBCVRateSafe(); // Simulación
                return 36.5;
            }
            
            @Override
            protected void done() {
                try {
                    double rate = get();
                    if (rate > 0) {
                        model.setBcvRate(rate);
                        LOGGER.info("Tasa BCV cargada: " + rate);
                    } else {
                        view.displayWarning("No se pudo obtener tasa BCV actual, usando valor por defecto.");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error cargando tasa BCV", e);
                    view.displayWarning("Error al cargar tasa BCV, usando valor por defecto.");
                }
            }
        }.execute();
    }
    
    private void loadServices() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                view.setLoading(true);
                try {
                    view.displayServices(model.loadServices());
                    LOGGER.info("Servicios cargados y mostrados en la vista");
                } catch (SalesModel.ModelException e) {
                    view.displayError(e.getMessage());
                }
                return null;
            }
            
            @Override
            protected void done() {
                view.setLoading(false);
            }
        }.execute();
    }
    
    // ===== GESTIÓN DE CLIENTES =====
    
    public void searchClient(String cedula) {
        if (cedula == null || cedula.trim().isEmpty()) {
            view.displayWarning("Por favor, ingrese una cédula.");
            return;
        }
        view.setLoading(true);
        try {
            Cliente cliente = model.loadClientByCedula(cedula);
            if (cliente != null) {
                view.displayClient(cliente);
                view.displaySuccess("Cliente encontrado: " + cliente.getFullName());
            } else {
                view.displayWarning("Cliente no encontrado.");
                model.clearClient();
            }
        } catch (SalesModel.ModelException e) {
            LOGGER.log(Level.WARNING, "Error buscando cliente", e);
            view.displayError(e.getMessage());
        } finally {
            view.setLoading(false);
        }
    }
    
    public void setClient(Cliente cliente) {
        model.setCurrentClient(cliente);
        view.displayClient(cliente);
    }
    
    public void clearClient() {
        model.clearClient();
        view.clearForm();
    }
    
    // ===== GESTIÓN DE SERVICIOS EN LA VENTA =====
    
    public void addService(String serviceName, String workerName) {
        try {
            if (serviceName == null || serviceName.isEmpty()) {
                view.displayError("Debe seleccionar un servicio.");
                return;
            }
            if (workerName == null || workerName.isEmpty()) {
                view.displayError("Debe seleccionar una trabajadora.");
                return;
            }
            
            model.addServiceToSale(serviceName, workerName, 0);
            updateSaleTotals();
            view.displaySuccess("Servicio '" + serviceName + "' agregado.");
        } catch (SalesModel.ModelException e) {
            view.displayError(e.getMessage());
        }
    }
    
    public void removeService(int index) {
        try {
            model.removeService(index);
            updateSaleTotals();
            view.displaySuccess("Servicio eliminado correctamente.");
        } catch (SalesModel.ModelException e) {
            view.displayError(e.getMessage());
        }
    }
    
    public void updateServicePrice(int index, double newPrice) {
        if (newPrice < 0) {
            view.displayError("El precio no puede ser negativo.");
            return;
        }
        try {
            model.updateServicePrice(index, newPrice);
            updateSaleTotals();
            LOGGER.fine("Precio actualizado para el servicio en el índice " + index);
        } catch (SalesModel.ModelException e) {
            view.displayError(e.getMessage());
        }
    }
    
    // ===== CÁLCULOS Y ACTUALIZACIÓN DE TOTALES =====
    
    public void updateSaleTotals() {
        try {
            double subtotal = model.calculateSubtotal();
            double discount = model.calculateDiscount(view.getDiscountType(), subtotal);
            double tip = view.getTipAmount();
            boolean isBolivares = view.isSelectedBolívares();
            
            SalesModel.SalesCalculation calc = model.calculateTotal(subtotal, discount, tip, isBolivares);
            
            String moneda = isBolivares ? "Bs" : "$";
            view.displayTotal(calc.total, moneda);
            
            double montoPagado = view.getPaidAmount();
            double vuelto = model.calculateChange(calc.total, montoPagado);
            view.displayVuelto(vuelto);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al actualizar los totales de la venta", e);
            view.displayError("Ocurrió un error al calcular los totales.");
        }
    }
    
    // ===== PROCESAMIENTO DE VENTA =====
    
    public void generateInvoice() {
        try {
            view.setLoading(true);
            view.lockUI();
            
            // Recolectar datos y calcular
            double subtotal = model.calculateSubtotal();
            String discountType = view.getDiscountType();
            double discount = model.calculateDiscount(discountType, subtotal);
            double tip = view.getTipAmount();
            boolean isBolivares = view.isSelectedBolívares();
            double montoPagado = view.getPaidAmount();
            String metodoPago = view.getPaymentMethod();
            
            SalesModel.SalesCalculation calc = model.calculateTotal(subtotal, discount, tip, isBolivares);
            
            double totalEnDolares = isBolivares ? calc.total / calc.bcvRate : calc.total;
            double montoPagadoEnDolares = isBolivares ? montoPagado / calc.bcvRate : montoPagado;
            
            // Validar
            ValidationResult validationResult = model.validateSale(
                subtotal, discount, tip, totalEnDolares, montoPagadoEnDolares, metodoPago, discountType
            );
            
            if (!validationResult.isValid()) {
                view.displayError("Error de validación:\n" + validationResult.getFormattedMessage());
                return;
            }
            
            // TODO: Delegar a un SalesService para procesar la venta en la BD
            // SaleProcessResult result = salesService.processSale(...);
            
            // Simulación de éxito
            view.displaySuccess("✅ Factura generada exitosamente. ID: 12345");
            clearSaleForm();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error crítico al generar factura", e);
            view.displayError("Error inesperado al generar factura: " + e.getMessage());
        } finally {
            view.setLoading(false);
            view.unlockUI();
        }
    }
    
    private void clearSaleForm() {
        model.clearAddedServices();
        model.clearClient();
        view.clearForm();
        view.clearServicesTable();
        updateSaleTotals();
    }
}

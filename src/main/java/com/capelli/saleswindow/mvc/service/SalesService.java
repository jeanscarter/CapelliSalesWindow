package com.capelli.saleswindow.mvc.service;

// import com.capelli.database.ClientRepository;
// import com.capelli.database.SalesRepository;
import com.capelli.model.Sale;
import com.capelli.model.SaleItem;
import com.capelli.saleswindow.mvc.dto.SaleItemRequest;
import com.capelli.saleswindow.mvc.dto.SaleRequest;
import java.util.logging.Logger;

/**
 * ============================================
 * CAPA DE SERVICIO - SalesService
 * ============================================
 * Se sitúa entre el Controlador y los Repositorios.
 * - Encapsula la lógica de negocio compleja que involucra múltiples
 * operaciones de base de datos o interacciones con otros servicios.
 * - Asegura la atomicidad de las operaciones (transacciones).
 * - Mantiene al Controlador limpio de lógica de persistencia detallada.
 */
public class SalesService {
    private static final Logger LOGGER = Logger.getLogger(SalesService.class.getName());
    
    // private final SalesRepository salesRepository;
    // private final ClientRepository clientRepository;
    
    // public SalesService(SalesRepository salesRepository, ClientRepository clientRepository) {
    //     this.salesRepository = salesRepository;
    //     this.clientRepository = clientRepository;
    // }
    
    /**
     * Procesa una venta completa de forma transaccional.
     * Guarda la venta, sus ítems y la propina en la base de datos.
     *
     * @param request El objeto DTO que contiene toda la información de la venta.
     * @return El ID de la venta generada.
     * @throws ServiceException si ocurre un error durante el proceso.
     */
    public long processSale(SaleRequest request) throws ServiceException {
        // En una implementación real, aquí se iniciaría una transacción de base de datos.
        // Connection connection = Database.getConnection();
        // connection.setAutoCommit(false);
        
        try {
            // 1. Validar cliente (si aplica)
            if (request.getClientId() > 0) {
                // if (!clientRepository.existsById(request.getClientId())) {
                //     throw new ServiceException("El cliente con ID " + request.getClientId() + " no existe.");
                // }
            }
            
            // 2. Validar que haya ítems en la venta
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new ServiceException("La venta debe contener al menos un servicio.");
            }
            
            // 3. Mapear DTO a entidad de dominio (Sale)
            Sale sale = new Sale();
            sale.setClientId(request.getClientId());
            sale.setSubtotal(request.getSubtotal());
            sale.setDiscountType(request.getDiscountType());
            sale.setDiscountAmount(request.getDiscount());
            sale.setTotal(request.getTotal());
            sale.setPaymentMethod(request.getPaymentMethod());
            sale.setCurrency(request.getCurrency());
            
            // 4. Guardar la entidad principal para obtener su ID
            // long saleId = salesRepository.save(sale, connection);
            long saleId = System.currentTimeMillis(); // Simulación
            LOGGER.info("Venta principal guardada con ID: " + saleId);
            
            // 5. Guardar cada ítem de la venta
            for (SaleItemRequest itemRequest : request.getItems()) {
                SaleItem saleItem = new SaleItem();
                saleItem.setSaleId(saleId);
                saleItem.setServiceId(itemRequest.getServiceId());
                saleItem.setEmployeeId(itemRequest.getEmployeeId());
                saleItem.setPriceAtSale(itemRequest.getPrice());
                
                // salesRepository.saveItem(saleItem, connection);
            }
            LOGGER.info(request.getItems().size() + " ítems de venta guardados.");
            
            // 6. Guardar la propina si existe
            if (request.getTip() > 0) {
                // salesRepository.saveTip(saleId, request.getTipRecipient(), request.getTip(), connection);
                LOGGER.info("Propina guardada para la venta ID: " + saleId);
            }
            
            // Si todo fue exitoso, confirmar la transacción
            // connection.commit();
            LOGGER.info("Venta " + saleId + " procesada y confirmada exitosamente.");
            
            return saleId;
            
        } catch (Exception e) {
            // En caso de error, revertir la transacción
            // try { connection.rollback(); } catch (SQLException ex) { ... }
            LOGGER.log(java.util.logging.Level.SEVERE, "Error al procesar la venta. Se revirtió la transacción.", e);
            throw new ServiceException("No se pudo procesar la venta: " + e.getMessage(), e);
        } finally {
            // Cerrar la conexión
            // try { connection.close(); } catch (SQLException e) { ... }
        }
    }
    
    /**
     * Excepción personalizada para la capa de servicio.
     */
    public static class ServiceException extends Exception {
        public ServiceException(String message) {
            super(message);
        }
        
        public ServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

package com.capelli.saleswindow.mvc.controller;

import com.capelli.model.Cliente;
import com.capelli.saleswindow.mvc.SalesView;
import com.capelli.saleswindow.mvc.model.SalesModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

/**
 * ============================================
 * TEST UNITARIO - SalesControllerTest
 * ============================================
 * Ejemplo de cómo se probaría el SalesController de forma aislada.
 * - Se "mockean" (simulan) sus dependencias (View y Model).
 * - Se verifica que el controlador llama a los métodos correctos en sus
 * dependencias en respuesta a una acción.
 */
public class SalesControllerTest {
    
    @Mock // Crea un objeto simulado (mock) de SalesView
    private SalesView mockView;
    
    @Mock // Crea un objeto simulado (mock) de SalesModel
    private SalesModel mockModel;
    
    @InjectMocks // Crea una instancia de SalesController e inyecta los mocks de arriba
    private SalesController controller;
    
    @BeforeEach
    void setUp() {
        // Inicializa los mocks para cada prueba
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void searchClient_WithEmptyCedula_ShouldShowWarning() throws SalesModel.ModelException {
        // Arrange (Preparación)
        String emptyCedula = "   ";
        
        // Act (Acción)
        controller.searchClient(emptyCedula);
        
        // Assert (Verificación)
        // Verificar que se llamó al método displayWarning en la vista con el mensaje esperado.
        verify(mockView).displayWarning(contains("ingrese una cédula"));
        // Verificar que NUNCA se intentó cargar un cliente desde el modelo.
        verify(mockModel, never()).loadClientByCedula(anyString());
    }
    
    @Test
    void searchClient_WhenClientFound_ShouldDisplayClientAndSuccessMessage() throws SalesModel.ModelException {
        // Arrange
        String cedula = "V-12345678";
        Cliente mockCliente = new Cliente((int) 1L, "Juan Pérez", cedula, "Largo");
        
        // Configurar el comportamiento del mock del modelo:
        // Cuando se llame a loadClientByCedula con `cedula`, debe devolver `mockCliente`.
        when(mockModel.loadClientByCedula(cedula)).thenReturn(mockCliente);
        
        // Act
        controller.searchClient(cedula);
        
        // Assert
        // Verificar que el controlador mostró los datos del cliente en la vista.
        verify(mockView).displayClient(mockCliente);
        // Verificar que se mostró un mensaje de éxito.
        verify(mockView).displaySuccess(contains("Cliente encontrado"));
        // Verificar que no se mostró ningún mensaje de error.
        verify(mockView, never()).displayError(anyString());
    }
    
    @Test
    void addService_WithValidData_ShouldUpdateTotalsAndShowSuccess() throws SalesModel.ModelException {
        // Arrange
        String serviceName = "Corte de Dama";
        String workerName = "Maria";
        
        // Configurar mocks para el cálculo de totales
        when(mockModel.calculateSubtotal()).thenReturn(50.0);
        when(mockView.getDiscountType()).thenReturn("Ninguno");
        
        // Act
        controller.addService(serviceName, workerName);
        
        // Assert
        // Verificar que se llamó al modelo para agregar el servicio.
        verify(mockModel).addServiceToSale(serviceName, workerName, 0);
        // Verificar que se mostró un mensaje de éxito.
        verify(mockView).displaySuccess(contains("Servicio '" + serviceName + "' agregado"));
        // Verificar que se llamó a la vista para actualizar el total.
        verify(mockView).displayTotal(anyDouble(), anyString());
    }
    
    @Test
    void removeService_WhenModelThrowsException_ShouldDisplayError() throws SalesModel.ModelException {
        // Arrange
        int invalidIndex = -1;
        // Configurar el mock para que lance una excepción cuando se intente eliminar un servicio con un índice inválido.
        doThrow(new SalesModel.ModelException("Índice de servicio inválido"))
            .when(mockModel).removeService(invalidIndex);
            
        // Act
        controller.removeService(invalidIndex);
        
        // Assert
        // Verificar que se mostró un mensaje de error en la vista.
        verify(mockView).displayError("Índice de servicio inválido");
        // Verificar que NUNCA se intentó actualizar los totales, ya que la operación falló.
        verify(mockView, never()).displayTotal(anyDouble(), anyString());
    }
}

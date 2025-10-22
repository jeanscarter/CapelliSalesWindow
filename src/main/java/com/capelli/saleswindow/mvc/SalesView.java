package com.capelli.saleswindow.mvc;

import com.capelli.model.Cliente;
import com.capelli.model.Service;
import java.util.List;

public interface SalesView {
    // Mostrar datos
    void displayClient(Cliente cliente);
    void displayServices(List<Service> services);
    void displayTotal(double total, String moneda);
    void displayVuelto(double vuelto);
    void displayError(String message);
    void displaySuccess(String message);
    void displayWarning(String message);
    
    // Obtener datos del usuario
    String getClientCedula();
    String getSelectedService();
    String getSelectedWorker();
    String getDiscountType();
    double getTipAmount();
    double getPaidAmount();
    String getPaymentMethod();
    String getPaymentDestination();
    boolean isSelectedBolívares();
    
    // Limpiar UI
    void clearForm();
    void clearServicesTable();
    void enablePaymentFields();
    void disablePaymentFields();
    
    // Estado
    void setLoading(boolean loading);
    void lockUI();
    void unlockUI();
}

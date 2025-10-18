package com.capelli.validation;

/**
 * Validador para datos de clientes.
 */
public class ClienteValidator {
    
    /**
     * Valida los datos de un cliente antes de guardar.
     */
    public static ValidationResult validateCliente(String fullName, String cedula, 
                                                   String phone, String email) {
        ValidationResult result = new ValidationResult();
        
        // Validar nombre completo (obligatorio)
        CommonValidators.validateNotEmpty(fullName, "Nombre completo", result);
        if (CommonValidators.isNotEmpty(fullName)) {
            CommonValidators.validateMinLength(fullName, 3, "Nombre completo", result);
            CommonValidators.validateMaxLength(fullName, 100, "Nombre completo", result);
        }
        
        // Validar cédula (obligatoria)
        CommonValidators.validateCedula(cedula, "Cédula", result);
        
        // Validar teléfono (opcional pero si existe debe ser válido)
        if (CommonValidators.isNotEmpty(phone)) {
            CommonValidators.validatePhone(phone, "Teléfono", result);
        }
        
        // Validar email (opcional pero si existe debe ser válido)
        if (CommonValidators.isNotEmpty(email)) {
            CommonValidators.validateEmail(email, "Correo electrónico", result);
        }
        
        return result;
    }
    
    /**
     * Valida solo la cédula de un cliente.
     */
    public static ValidationResult validateCedula(String cedula) {
        ValidationResult result = new ValidationResult();
        CommonValidators.validateCedula(cedula, "Cédula", result);
        return result;
    }
    
    /**
     * Valida datos de dirección.
     */
    public static ValidationResult validateAddress(String address) {
        ValidationResult result = new ValidationResult();
        
        if (CommonValidators.isNotEmpty(address)) {
            CommonValidators.validateMaxLength(address, 200, "Dirección", result);
        }
        
        return result;
    }
}
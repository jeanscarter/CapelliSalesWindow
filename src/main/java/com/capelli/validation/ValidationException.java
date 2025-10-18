package com.capelli.validation;

/**
 * Excepción lanzada cuando falla una validación.
 * Esta excepción encapsula el resultado de validación completo,
 * permitiendo acceder a todos los errores y advertencias.
 */
public class ValidationException extends Exception {
    
    private final ValidationResult validationResult;
    
    /**
     * Constructor que recibe un resultado de validación.
     * 
     * @param validationResult El resultado de validación que contiene los errores
     */
    public ValidationException(ValidationResult validationResult) {
        super(validationResult.getErrorMessage());
        this.validationResult = validationResult;
    }
    
    /**
     * Constructor simple con mensaje de error.
     * Crea automáticamente un ValidationResult con un solo error.
     * 
     * @param message Mensaje de error
     */
    public ValidationException(String message) {
        super(message);
        ValidationResult result = new ValidationResult();
        result.addError(message);
        this.validationResult = result;
    }
    
    /**
     * Constructor con mensaje y causa.
     * 
     * @param message Mensaje de error
     * @param cause Causa de la excepción
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        ValidationResult result = new ValidationResult();
        result.addError(message);
        this.validationResult = result;
    }
    
    /**
     * Obtiene el resultado de validación que causó la excepción.
     * 
     * @return ValidationResult con todos los errores y advertencias
     */
    public ValidationResult getValidationResult() {
        return validationResult;
    }
    
    /**
     * Obtiene todos los errores formateados en un mensaje legible.
     * 
     * @return String con todos los errores formateados
     */
    public String getFormattedErrors() {
        return validationResult.getFormattedMessage();
    }
    
    /**
     * Verifica si hay advertencias además de errores.
     * 
     * @return true si hay advertencias
     */
    public boolean hasWarnings() {
        return validationResult.hasWarnings();
    }
    
    /**
     * Obtiene el número total de errores.
     * 
     * @return Cantidad de errores
     */
    public int getErrorCount() {
        return validationResult.getErrorCount();
    }
}
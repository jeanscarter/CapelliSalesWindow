package com.capelli.validation;

/**
 * Interface base para todos los validadores.
 * Define el contrato que deben seguir todos los validadores del sistema.
 * 
 * @param <T> El tipo de objeto a validar
 */
public interface Validator<T> {
    
    /**
     * Valida un objeto y retorna el resultado.
     * Este método debe implementarse para definir las reglas de validación específicas.
     * 
     * @param object El objeto a validar
     * @return ValidationResult con los errores y advertencias encontrados
     */
    ValidationResult validate(T object);
    
    /**
     * Valida un objeto y lanza excepción si no es válido.
     * Este método es útil cuando se quiere forzar la validación y detener
     * la ejecución si hay errores.
     * 
     * @param object El objeto a validar
     * @throws ValidationException si la validación falla (hay errores)
     */
    default void validateAndThrow(T object) throws ValidationException {
        ValidationResult result = validate(object);
        if (!result.isValid()) {
            throw new ValidationException(result);
        }
    }
    
    /**
     * Verifica si un objeto es válido.
     * Este método es una forma rápida de verificar validez sin procesar
     * los detalles de los errores.
     * 
     * @param object El objeto a validar
     * @return true si es válido (sin errores), false en caso contrario
     */
    default boolean isValid(T object) {
        return validate(object).isValid();
    }
}
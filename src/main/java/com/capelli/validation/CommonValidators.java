package com.capelli.validation;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Utilidades de validación comunes reutilizables.
 * Proporciona métodos estáticos para validar tipos de datos comunes.
 */
public class CommonValidators {
    
    // ===== PATRONES DE VALIDACIÓN =====
    
    /**
     * Patrón para validar emails.
     * Formato: usuario@dominio.extension
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    /**
     * Patrón para validar teléfonos venezolanos.
     * Formato: 10 u 11 dígitos (ej: 04121234567 o 2121234567)
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[0-9]{10,11}$"
    );
    
    /**
     * Patrón para validar cédulas venezolanas.
     * Formato: V-12345678, E-12345678, J-123456789, etc.
     * Acepta con o sin guion
     */
    private static final Pattern CEDULA_PATTERN = Pattern.compile(
        "^[VEJGPvejgp]-?[0-9]{6,9}$"
    );
    
    /**
     * Patrón para validar cuentas bancarias venezolanas.
     * Formato: 20 dígitos exactos
     */
    private static final Pattern CUENTA_BANCARIA_PATTERN = Pattern.compile(
        "^[0-9]{20}$"
    );
    
    // ===== VALIDACIONES DE CADENAS (STRING) =====
    
    /**
     * Valida que una cadena no sea nula ni vacía.
     * @param value Cadena a validar
     * @return true si no es nula ni vacía (después de trim)
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    /**
     * Valida que una cadena no sea nula ni vacía, agregando error al resultado.
     * @param value Cadena a validar
     * @param fieldName Nombre del campo (para el mensaje)
     * @param result Objeto donde agregar el error
     */
    public static void validateNotEmpty(String value, String fieldName, ValidationResult result) {
        if (!isNotEmpty(value)) {
            result.addError(fieldName, "El campo " + fieldName + " es obligatorio");
        }
    }
    
    /**
     * Valida longitud mínima de una cadena.
     * @param value Cadena a validar
     * @param minLength Longitud mínima requerida
     * @return true si cumple la longitud mínima
     */
    public static boolean hasMinLength(String value, int minLength) {
        return isNotEmpty(value) && value.trim().length() >= minLength;
    }
    
    /**
     * Valida longitud mínima, agregando error al resultado.
     * @param value Cadena a validar
     * @param minLength Longitud mínima
     * @param fieldName Nombre del campo
     * @param result Objeto donde agregar el error
     */
    public static void validateMinLength(String value, int minLength, String fieldName, 
                                        ValidationResult result) {
        if (isNotEmpty(value) && !hasMinLength(value, minLength)) {
            result.addError(fieldName, String.format(
                "El campo %s debe tener al menos %d caracteres", fieldName, minLength
            ));
        }
    }
    
    /**
     * Valida longitud máxima de una cadena.
     * @param value Cadena a validar
     * @param maxLength Longitud máxima permitida
     * @return true si cumple la longitud máxima
     */
    public static boolean hasMaxLength(String value, int maxLength) {
        return value == null || value.trim().length() <= maxLength;
    }
    
    /**
     * Valida longitud máxima, agregando error al resultado.
     * @param value Cadena a validar
     * @param maxLength Longitud máxima
     * @param fieldName Nombre del campo
     * @param result Objeto donde agregar el error
     */
    public static void validateMaxLength(String value, int maxLength, String fieldName, 
                                        ValidationResult result) {
        if (isNotEmpty(value) && !hasMaxLength(value, maxLength)) {
            result.addError(fieldName, String.format(
                "El campo %s no puede tener más de %d caracteres", fieldName, maxLength
            ));
        }
    }
    
    // ===== VALIDACIONES DE COLECCIONES =====
    
    /**
     * Valida que una colección no sea nula ni vacía.
     * @param collection Colección a validar
     * @return true si no es nula ni vacía
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }
    
    /**
     * Valida que una colección no sea nula ni vacía, agregando error al resultado.
     * @param collection Colección a validar
     * @param fieldName Nombre del campo
     * @param result Objeto donde agregar el error
     */
    public static void validateNotEmpty(Collection<?> collection, String fieldName, 
                                       ValidationResult result) {
        if (!isNotEmpty(collection)) {
            result.addError(fieldName, "Debe agregar al menos un elemento en " + fieldName);
        }
    }
    
    // ===== VALIDACIONES DE OBJETOS =====
    
    /**
     * Valida que un objeto no sea nulo.
     * @param value Objeto a validar
     * @return true si no es nulo
     */
    public static boolean isNotNull(Object value) {
        return value != null;
    }
    
    /**
     * Valida que un objeto no sea nulo, agregando error al resultado.
     * @param value Objeto a validar
     * @param fieldName Nombre del campo
     * @param result Objeto donde agregar el error
     */
    public static void validateNotNull(Object value, String fieldName, ValidationResult result) {
        if (!isNotNull(value)) {
            result.addError(fieldName, "El campo " + fieldName + " es obligatorio");
        }
    }
    
    // ===== VALIDACIONES NUMÉRICAS =====
    
    /**
     * Valida que un número sea positivo (mayor que cero).
     * @param value Número a validar
     * @return true si es mayor que cero
     */
    public static boolean isPositive(double value) {
        return value > 0;
    }
    
    /**
     * Valida que un número sea positivo, agregando error al resultado.
     * @param value Número a validar
     * @param fieldName Nombre del campo
     * @param result Objeto donde agregar el error
     */
    public static void validatePositive(double value, String fieldName, ValidationResult result) {
        if (!isPositive(value)) {
            result.addError(fieldName, "El campo " + fieldName + " debe ser un número positivo");
        }
    }
    
    /**
     * Valida que un número no sea negativo (puede ser cero).
     * @param value Número a validar
     * @return true si es mayor o igual a cero
     */
    public static boolean isNonNegative(double value) {
        return value >= 0;
    }
    
    /**
     * Valida que un número no sea negativo, agregando error al resultado.
     * @param value Número a validar
     * @param fieldName Nombre del campo
     * @param result Objeto donde agregar el error
     */
    public static void validateNonNegative(double value, String fieldName, ValidationResult result) {
        if (!isNonNegative(value)) {
            result.addError(fieldName, "El campo " + fieldName + " no puede ser negativo");
        }
    }
    
    /**
     * Valida que un número esté en un rango específico.
     * @param value Número a validar
     * @param min Valor mínimo (inclusivo)
     * @param max Valor máximo (inclusivo)
     * @return true si está dentro del rango
     */
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }
    
    /**
     * Valida que un número esté en un rango, agregando error al resultado.
     * @param value Número a validar
     * @param min Valor mínimo
     * @param max Valor máximo
     * @param fieldName Nombre del campo
     * @param result Objeto donde agregar el error
     */
    public static void validateInRange(double value, double min, double max, String fieldName, 
                                      ValidationResult result) {
        if (!isInRange(value, min, max)) {
            result.addError(fieldName, String.format(
                "El campo %s debe estar entre %.2f y %.2f", fieldName, min, max
            ));
        }
    }
    
    // ===== VALIDACIONES DE FORMATO ESPECÍFICO =====
    
    /**
     * Valida formato de email.
     * @param email Email a validar
     * @return true si el formato es válido
     */
    public static boolean isValidEmail(String email) {
        return isNotEmpty(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * Valida formato de email, agregando error al resultado.
     * @param email Email a validar
     * @param fieldName Nombre del campo
     * @param result Objeto donde agregar el error
     */
    public static void validateEmail(String email, String fieldName, ValidationResult result) {
        if (isNotEmpty(email) && !isValidEmail(email)) {
            result.addError(fieldName, "El formato del email no es válido");
        }
    }
    
    /**
     * Valida formato de teléfono venezolano.
     * Acepta: 04121234567 (11 dígitos) o 2121234567 (10 dígitos)
     * @param phone Teléfono a validar
     * @return true si el formato es válido
     */
    public static boolean isValidPhone(String phone) {
        if (!isNotEmpty(phone)) {
            return true; // Opcional, si está vacío es válido
        }
        // Limpiar caracteres especiales (espacios, guiones, paréntesis)
        String cleaned = phone.replaceAll("[\\s()\\-]", "");
        return PHONE_PATTERN.matcher(cleaned).matches();
    }
    
    /**
     * Valida formato de teléfono, agregando error al resultado.
     * @param phone Teléfono a validar
     * @param fieldName Nombre del campo
     * @param result Objeto donde agregar el error
     */
    public static void validatePhone(String phone, String fieldName, ValidationResult result) {
        if (isNotEmpty(phone) && !isValidPhone(phone)) {
            result.addError(fieldName, 
                "El formato del teléfono no es válido (debe tener 10-11 dígitos)");
        }
    }
    
    /**
     * Valida formato de cédula venezolana.
     * Acepta: V-12345678, E-12345678, J-123456789, G-12345678, P-12345678
     * También acepta sin guion: V12345678
     * @param cedula Cédula a validar
     * @return true si el formato es válido
     */
    public static boolean isValidCedula(String cedula) {
        if (!isNotEmpty(cedula)) {
            return false;
        }
        return CEDULA_PATTERN.matcher(cedula.trim()).matches();
    }
    
    /**
     * Valida formato de cédula, agregando error al resultado.
     * @param cedula Cédula a validar
     * @param fieldName Nombre del campo
     * @param result Objeto donde agregar el error
     */
    public static void validateCedula(String cedula, String fieldName, ValidationResult result) {
        if (!isNotEmpty(cedula)) {
            result.addError(fieldName, "La cédula es obligatoria");
        } else if (!isValidCedula(cedula)) {
            result.addError(fieldName, "El formato de la cédula no es válido (ej: V-12345678)");
        }
    }
    
    /**
     * Valida formato de cuenta bancaria venezolana.
     * Debe tener exactamente 20 dígitos.
     * @param cuenta Número de cuenta a validar
     * @return true si el formato es válido
     */
    public static boolean isValidCuentaBancaria(String cuenta) {
        if (!isNotEmpty(cuenta)) {
            return false;
        }
        // Limpiar espacios y guiones
        String cleaned = cuenta.replaceAll("[\\s\\-]", "");
        return CUENTA_BANCARIA_PATTERN.matcher(cleaned).matches();
    }
    
    /**
     * Valida formato de cuenta bancaria, agregando error al resultado.
     * @param cuenta Número de cuenta a validar
     * @param fieldName Nombre del campo
     * @param result Objeto donde agregar el error
     */
    public static void validateCuentaBancaria(String cuenta, String fieldName, 
                                             ValidationResult result) {
        if (!isNotEmpty(cuenta)) {
            result.addError(fieldName, "El número de cuenta es obligatorio");
        } else if (!isValidCuentaBancaria(cuenta)) {
            result.addError(fieldName, "El número de cuenta debe tener 20 dígitos");
        }
    }
    
    // ===== MÉTODOS DE UTILIDAD =====
    
    /**
     * Limpia una cédula removiendo guiones y espacios.
     * @param cedula Cédula a limpiar
     * @return Cédula limpia (ej: "V12345678")
     */
    public static String cleanCedula(String cedula) {
        if (cedula == null) {
            return "";
        }
        return cedula.replaceAll("[\\s\\-]", "").toUpperCase();
    }
    
    /**
     * Limpia un teléfono removiendo caracteres especiales.
     * @param phone Teléfono a limpiar
     * @return Teléfono limpio (solo dígitos)
     */
    public static String cleanPhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[^0-9]", "");
    }
    
    /**
     * Limpia un número de cuenta removiendo espacios y guiones.
     * @param cuenta Cuenta a limpiar
     * @return Cuenta limpia (solo dígitos)
     */
    public static String cleanCuentaBancaria(String cuenta) {
        if (cuenta == null) {
            return "";
        }
        return cuenta.replaceAll("[\\s\\-]", "");
    }
    
    /**
     * Formatea una cédula en el formato estándar (V-12345678).
     * @param cedula Cédula a formatear
     * @return Cédula formateada o cadena vacía si es inválida
     */
    public static String formatCedula(String cedula) {
        if (!isValidCedula(cedula)) {
            return "";
        }
        
        String cleaned = cleanCedula(cedula);
        if (cleaned.length() < 2) {
            return "";
        }
        
        // Separar tipo y número
        String tipo = cleaned.substring(0, 1).toUpperCase();
        String numero = cleaned.substring(1);
        
        return tipo + "-" + numero;
    }
}
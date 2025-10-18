package com.capelli.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Representa el resultado de una validación.
 * Almacena errores, advertencias e información adicional.
 */
public class ValidationResult {
    
    private final List<ValidationError> errors;
    private final List<ValidationWarning> warnings;
    private final List<ValidationInfo> infos;
    
    public ValidationResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.infos = new ArrayList<>();
    }
    
    /**
     * Agrega un error de validación.
     */
    public void addError(String field, String message) {
        errors.add(new ValidationError(field, message));
    }
    
    /**
     * Agrega un error de validación sin campo específico.
     */
    public void addError(String message) {
        errors.add(new ValidationError(null, message));
    }
    
    /**
     * Agrega una advertencia.
     */
    public void addWarning(String field, String message) {
        warnings.add(new ValidationWarning(field, message));
    }
    
    /**
     * Agrega información adicional.
     */
    public void addInfo(String field, String message) {
        infos.add(new ValidationInfo(field, message));
    }
    
    /**
     * Verifica si la validación fue exitosa (sin errores).
     */
    public boolean isValid() {
        return errors.isEmpty();
    }
    
    /**
     * Verifica si hay errores.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Verifica si hay advertencias.
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    /**
     * Obtiene todos los errores.
     */
    public List<ValidationError> getErrors() {
        return new ArrayList<>(errors);
    }
    
    /**
     * Obtiene todas las advertencias.
     */
    public List<ValidationWarning> getWarnings() {
        return new ArrayList<>(warnings);
    }
    
    /**
     * Obtiene toda la información.
     */
    public List<ValidationInfo> getInfos() {
        return new ArrayList<>(infos);
    }
    
    /**
     * Obtiene un mensaje con todos los errores.
     */
    public String getErrorMessage() {
        if (errors.isEmpty()) {
            return "";
        }
        
        return errors.stream()
                .map(ValidationError::toString)
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * Obtiene un mensaje con todas las advertencias.
     */
    public String getWarningMessage() {
        if (warnings.isEmpty()) {
            return "";
        }
        
        return warnings.stream()
                .map(ValidationWarning::toString)
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * Obtiene un mensaje formateado para mostrar al usuario.
     */
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        
        if (hasErrors()) {
            sb.append("❌ ERRORES:\n");
            sb.append(getErrorMessage());
        }
        
        if (hasWarnings()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("⚠️  ADVERTENCIAS:\n");
            sb.append(getWarningMessage());
        }
        
        return sb.toString();
    }
    
    /**
     * Combina este resultado con otro.
     */
    public void merge(ValidationResult other) {
        this.errors.addAll(other.errors);
        this.warnings.addAll(other.warnings);
        this.infos.addAll(other.infos);
    }
    
    /**
     * Obtiene el número de errores.
     */
    public int getErrorCount() {
        return errors.size();
    }
    
    /**
     * Obtiene el número de advertencias.
     */
    public int getWarningCount() {
        return warnings.size();
    }
    
    @Override
    public String toString() {
        return String.format("ValidationResult[errors=%d, warnings=%d, valid=%s]",
                errors.size(), warnings.size(), isValid());
    }
    
    // ===== CLASES INTERNAS =====
    
    /**
     * Representa un error de validación.
     */
    public static class ValidationError {
        private final String field;
        private final String message;
        
        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }
        
        public String getField() {
            return field;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            if (field != null && !field.isEmpty()) {
                return "• " + field + ": " + message;
            }
            return "• " + message;
        }
    }
    
    /**
     * Representa una advertencia de validación.
     */
    public static class ValidationWarning {
        private final String field;
        private final String message;
        
        public ValidationWarning(String field, String message) {
            this.field = field;
            this.message = message;
        }
        
        public String getField() {
            return field;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            if (field != null && !field.isEmpty()) {
                return "• " + field + ": " + message;
            }
            return "• " + message;
        }
    }
    
    /**
     * Representa información adicional.
     */
    public static class ValidationInfo {
        private final String field;
        private final String message;
        
        public ValidationInfo(String field, String message) {
            this.field = field;
            this.message = message;
        }
        
        public String getField() {
            return field;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            if (field != null && !field.isEmpty()) {
                return field + ": " + message;
            }
            return message;
        }
    }
}

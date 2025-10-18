package com.capelli.validation;

import com.capelli.model.CuentaBancaria;
import com.capelli.model.Trabajadora;
import java.util.List;

/**
 * Validador para trabajadoras.
 * Implementa todas las reglas de validación para el modelo Trabajadora.
 */
public class TrabajadoraValidator implements Validator<Trabajadora> {
    
    /**
     * Valida una trabajadora completa incluyendo sus cuentas bancarias.
     * 
     * @param trabajadora La trabajadora a validar
     * @return ValidationResult con los errores y advertencias encontrados
     */
    @Override
    public ValidationResult validate(Trabajadora trabajadora) {
        ValidationResult result = new ValidationResult();
        
        // Validar que el objeto no sea nulo
        if (trabajadora == null) {
            result.addError("La trabajadora no puede ser nula");
            return result;
        }
        
        // Validar nombres
        CommonValidators.validateNotEmpty(trabajadora.getNombres(), "Nombres", result);
        if (CommonValidators.isNotEmpty(trabajadora.getNombres())) {
            CommonValidators.validateMinLength(trabajadora.getNombres(), 2, "Nombres", result);
            CommonValidators.validateMaxLength(trabajadora.getNombres(), 50, "Nombres", result);
        }
        
        // Validar apellidos
        CommonValidators.validateNotEmpty(trabajadora.getApellidos(), "Apellidos", result);
        if (CommonValidators.isNotEmpty(trabajadora.getApellidos())) {
            CommonValidators.validateMinLength(trabajadora.getApellidos(), 2, "Apellidos", result);
            CommonValidators.validateMaxLength(trabajadora.getApellidos(), 50, "Apellidos", result);
        }
        
        // Validar cédula (concatenar tipo y número)
        if (trabajadora.getTipoCi() != null && trabajadora.getNumeroCi() != null) {
            String ciCompleta = trabajadora.getTipoCi() + "-" + trabajadora.getNumeroCi();
            CommonValidators.validateCedula(ciCompleta, "Cédula", result);
        } else {
            result.addError("Cédula", "Debe especificar tipo y número de cédula");
        }
        
        // Validar teléfono (opcional, pero si existe debe ser válido)
        if (CommonValidators.isNotEmpty(trabajadora.getTelefono())) {
            CommonValidators.validatePhone(trabajadora.getTelefono(), "Teléfono", result);
        }
        
        // Validar email (opcional, pero si existe debe ser válido)
        if (CommonValidators.isNotEmpty(trabajadora.getCorreoElectronico())) {
            CommonValidators.validateEmail(trabajadora.getCorreoElectronico(), "Correo", result);
        }
        
        // Validar cuentas bancarias
        ValidationResult cuentasResult = validateCuentasBancarias(trabajadora.getCuentas());
        result.merge(cuentasResult);
        
        return result;
    }
    
    /**
     * Valida las cuentas bancarias de una trabajadora.
     * Verifica que haya al menos una cuenta y exactamente una cuenta principal.
     * 
     * @param cuentas Lista de cuentas bancarias a validar
     * @return ValidationResult con los errores y advertencias encontrados
     */
    public static ValidationResult validateCuentasBancarias(List<CuentaBancaria> cuentas) {
        ValidationResult result = new ValidationResult();
        
        // Verificar si hay cuentas
        if (cuentas == null || cuentas.isEmpty()) {
            result.addWarning("Cuentas bancarias", 
                "No se han agregado cuentas bancarias para esta trabajadora");
            return result;
        }
        
        // Contar cuentas principales
        long cuentasPrincipales = cuentas.stream()
                .filter(CuentaBancaria::isEsPrincipal)
                .count();
        
        // Validar que haya exactamente una cuenta principal
        if (cuentasPrincipales == 0) {
            result.addError("Cuentas bancarias", 
                "Debe marcar al menos una cuenta como principal");
        } else if (cuentasPrincipales > 1) {
            result.addError("Cuentas bancarias", 
                "Solo puede haber una cuenta principal");
        }
        
        // Validar cada cuenta individual
        for (int i = 0; i < cuentas.size(); i++) {
            CuentaBancaria cuenta = cuentas.get(i);
            String prefix = "Cuenta " + (i + 1);
            
            // Validar banco
            CommonValidators.validateNotEmpty(cuenta.getBanco(), prefix + " - Banco", result);
            
            // Validar tipo de cuenta
            CommonValidators.validateNotEmpty(cuenta.getTipoDeCuenta(), prefix + " - Tipo", result);
            
            // Validar que el tipo sea válido
            if (CommonValidators.isNotEmpty(cuenta.getTipoDeCuenta())) {
                String tipo = cuenta.getTipoDeCuenta();
                if (!tipo.equals("Ahorro") && !tipo.equals("Corriente")) {
                    result.addError(prefix + " - Tipo", 
                        "El tipo de cuenta debe ser 'Ahorro' o 'Corriente'");
                }
            }
            
            // Validar número de cuenta
            CommonValidators.validateCuentaBancaria(cuenta.getNumeroDeCuenta(), 
                prefix + " - Número", result);
        }
        
        // Verificar cuentas duplicadas
        for (int i = 0; i < cuentas.size(); i++) {
            String numero1 = cuentas.get(i).getNumeroDeCuenta();
            if (!CommonValidators.isNotEmpty(numero1)) {
                continue;
            }
            
            for (int j = i + 1; j < cuentas.size(); j++) {
                String numero2 = cuentas.get(j).getNumeroDeCuenta();
                if (numero1.equals(numero2)) {
                    result.addError("Cuentas bancarias", 
                        "El número de cuenta " + numero1 + " está duplicado");
                }
            }
        }
        
        return result;
    }
    
    /**
     * Valida solo los datos básicos de una trabajadora (sin cuentas bancarias).
     * Útil para validaciones parciales durante la entrada de datos.
     * 
     * @param nombres Nombres de la trabajadora
     * @param apellidos Apellidos de la trabajadora
     * @param tipoCi Tipo de cédula (V, E, J, etc.)
     * @param numeroCi Número de cédula
     * @return ValidationResult con los errores encontrados
     */
    public static ValidationResult validateBasicInfo(String nombres, String apellidos, 
                                                     String tipoCi, String numeroCi) {
        ValidationResult result = new ValidationResult();
        
        // Validar nombres
        CommonValidators.validateNotEmpty(nombres, "Nombres", result);
        if (CommonValidators.isNotEmpty(nombres)) {
            CommonValidators.validateMinLength(nombres, 2, "Nombres", result);
            CommonValidators.validateMaxLength(nombres, 50, "Nombres", result);
        }
        
        // Validar apellidos
        CommonValidators.validateNotEmpty(apellidos, "Apellidos", result);
        if (CommonValidators.isNotEmpty(apellidos)) {
            CommonValidators.validateMinLength(apellidos, 2, "Apellidos", result);
            CommonValidators.validateMaxLength(apellidos, 50, "Apellidos", result);
        }
        
        // Validar cédula
        if (tipoCi != null && numeroCi != null) {
            String ciCompleta = tipoCi + "-" + numeroCi;
            CommonValidators.validateCedula(ciCompleta, "Cédula", result);
        } else {
            result.addError("Cédula", "Debe especificar tipo y número de cédula");
        }
        
        return result;
    }
    
    /**
     * Valida una cuenta bancaria individual.
     * 
     * @param cuenta Cuenta bancaria a validar
     * @return ValidationResult con los errores encontrados
     */
    public static ValidationResult validateCuentaBancaria(CuentaBancaria cuenta) {
        ValidationResult result = new ValidationResult();
        
        if (cuenta == null) {
            result.addError("La cuenta bancaria no puede ser nula");
            return result;
        }
        
        CommonValidators.validateNotEmpty(cuenta.getBanco(), "Banco", result);
        CommonValidators.validateNotEmpty(cuenta.getTipoDeCuenta(), "Tipo de cuenta", result);
        CommonValidators.validateCuentaBancaria(cuenta.getNumeroDeCuenta(), "Número de cuenta", result);
        
        // Validar que el tipo sea válido
        if (CommonValidators.isNotEmpty(cuenta.getTipoDeCuenta())) {
            String tipo = cuenta.getTipoDeCuenta();
            if (!tipo.equals("Ahorro") && !tipo.equals("Corriente")) {
                result.addError("Tipo de cuenta", 
                    "El tipo de cuenta debe ser 'Ahorro' o 'Corriente'");
            }
        }
        
        return result;
    }
    
    /**
     * Valida datos de contacto opcionales.
     * 
     * @param telefono Teléfono de la trabajadora
     * @param correo Correo electrónico de la trabajadora
     * @return ValidationResult con los errores encontrados
     */
    public static ValidationResult validateContactInfo(String telefono, String correo) {
        ValidationResult result = new ValidationResult();
        
        // Validar teléfono (opcional)
        if (CommonValidators.isNotEmpty(telefono)) {
            CommonValidators.validatePhone(telefono, "Teléfono", result);
        }
        
        // Validar email (opcional)
        if (CommonValidators.isNotEmpty(correo)) {
            CommonValidators.validateEmail(correo, "Correo electrónico", result);
        }
        
        return result;
    }
}
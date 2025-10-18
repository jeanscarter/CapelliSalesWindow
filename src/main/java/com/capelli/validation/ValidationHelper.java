package com.capelli.validation;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * Clase de ayuda para mostrar resultados de validación en la interfaz.
 */
public class ValidationHelper {
    
    private static final Logger LOGGER = Logger.getLogger(ValidationHelper.class.getName());
    
    /**
     * Muestra un resultado de validación en un diálogo.
     * @param parent Componente padre
     * @param result Resultado de validación
     * @param title Título del diálogo
     * @return true si la validación fue exitosa
     */
    public static boolean showValidationResult(Component parent, ValidationResult result, String title) {
        if (result.isValid()) {
            if (result.hasWarnings()) {
                // Solo advertencias, mostrar y permitir continuar
                int response = JOptionPane.showConfirmDialog(
                    parent,
                    result.getWarningMessage() + "\n\n¿Desea continuar?",
                    title + " - Advertencias",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                return response == JOptionPane.YES_OPTION;
            }
            return true; // Sin errores ni advertencias
        } else {
            // Hay errores
            JOptionPane.showMessageDialog(
                parent,
                result.getFormattedMessage(),
                title + " - Errores de Validación",
                JOptionPane.ERROR_MESSAGE
            );
            LOGGER.warning("Validación falló: " + result.getErrorMessage());
            return false;
        }
    }
    
    /**
     * Muestra solo los errores de validación.
     */
    public static void showErrors(Component parent, ValidationResult result) {
        if (result.hasErrors()) {
            JOptionPane.showMessageDialog(
                parent,
                result.getErrorMessage(),
                "Errores de Validación",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    /**
     * Muestra solo las advertencias de validación.
     */
    public static boolean showWarnings(Component parent, ValidationResult result) {
        if (result.hasWarnings()) {
            int response = JOptionPane.showConfirmDialog(
                parent,
                result.getWarningMessage() + "\n\n¿Desea continuar?",
                "Advertencias",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            return response == JOptionPane.YES_OPTION;
        }
        return true;
    }
    
    /**
     * Valida y muestra resultado. Si falla, retorna false.
     */
    public static boolean validateAndShow(Component parent, ValidationResult result, String title) {
        if (!result.isValid()) {
            showErrors(parent, result);
            return false;
        }
        
        if (result.hasWarnings()) {
            return showWarnings(parent, result);
        }
        
        return true;
    }
    
    /**
     * Crea un panel con un mensaje de error estilizado.
     */
    public static JPanel createErrorPanel(ValidationResult result) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(255, 235, 235));
        
        JLabel titleLabel = new JLabel("❌ Errores de Validación");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(200, 0, 0));
        panel.add(titleLabel);
        
        panel.add(Box.createVerticalStrut(10));
        
        for (ValidationResult.ValidationError error : result.getErrors()) {
            JLabel errorLabel = new JLabel(error.toString());
            errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            panel.add(errorLabel);
            panel.add(Box.createVerticalStrut(5));
        }
        
        return panel;
    }
    
    /**
     * Crea un panel con un mensaje de advertencia estilizado.
     */
    public static JPanel createWarningPanel(ValidationResult result) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(255, 250, 220));
        
        JLabel titleLabel = new JLabel("⚠️  Advertencias");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(180, 120, 0));
        panel.add(titleLabel);
        
        panel.add(Box.createVerticalStrut(10));
        
        for (ValidationResult.ValidationWarning warning : result.getWarnings()) {
            JLabel warningLabel = new JLabel(warning.toString());
            warningLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            panel.add(warningLabel);
            panel.add(Box.createVerticalStrut(5));
        }
        
        return panel;
    }
    
    /**
     * Marca visualmente un campo con error.
     */
    public static void markFieldAsError(JTextField field) {
        field.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
    }
    
    /**
     * Marca visualmente un campo con advertencia.
     */
    public static void markFieldAsWarning(JTextField field) {
        field.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2));
    }
    
    /**
     * Restaura el borde normal de un campo.
     */
    public static void resetFieldBorder(JTextField field) {
        field.setBorder(UIManager.getBorder("TextField.border"));
    }
    
    /**
     * Valida un campo de texto y muestra error si es necesario.
     */
    public static boolean validateTextField(JTextField field, String fieldName, 
                                            Component parent, boolean required) {
        String value = field.getText().trim();
        
        if (required && value.isEmpty()) {
            markFieldAsError(field);
            JOptionPane.showMessageDialog(
                parent,
                "El campo '" + fieldName + "' es obligatorio",
                "Campo Obligatorio",
                JOptionPane.ERROR_MESSAGE
            );
            field.requestFocus();
            return false;
        }
        
        resetFieldBorder(field);
        return true;
    }
    
    /**
     * Valida un campo numérico.
     */
    public static boolean validateNumericField(JTextField field, String fieldName, 
                                               Component parent, boolean allowNegative) {
        String value = field.getText().trim();
        
        if (value.isEmpty()) {
            return true; // Opcional
        }
        
        try {
            double number = Double.parseDouble(value.replace(",", "."));
            
            if (!allowNegative && number < 0) {
                markFieldAsError(field);
                JOptionPane.showMessageDialog(
                    parent,
                    "El campo '" + fieldName + "' no puede ser negativo",
                    "Valor Inválido",
                    JOptionPane.ERROR_MESSAGE
                );
                field.requestFocus();
                return false;
            }
            
            resetFieldBorder(field);
            return true;
            
        } catch (NumberFormatException e) {
            markFieldAsError(field);
            JOptionPane.showMessageDialog(
                parent,
                "El campo '" + fieldName + "' debe ser un número válido",
                "Formato Inválido",
                JOptionPane.ERROR_MESSAGE
            );
            field.requestFocus();
            return false;
        }
    }
    
    /**
     * Agrega un tooltip con mensaje de error a un campo.
     */
    public static void addErrorTooltip(JComponent field, String errorMessage) {
        field.setToolTipText("❌ " + errorMessage);
    }
    
    /**
     * Remueve el tooltip de error de un campo.
     */
    public static void removeErrorTooltip(JComponent field) {
        field.setToolTipText(null);
    }
}
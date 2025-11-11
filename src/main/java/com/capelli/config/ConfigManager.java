package com.capelli.config;

import com.capelli.database.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestiona la configuración dinámica almacenada en la base de datos,
 * como el número correlativo de facturas.
 */
public class ConfigManager {

    private static final Logger LOGGER = Logger.getLogger(ConfigManager.class.getName());
    public static final String KEY_CORRELATIVE = "correlative";

    /**
     * Obtiene una configuración específica de la base de datos.
     * @param key La clave de la configuración (ej: "correlative")
     * @return El valor guardado, o null si no se encuentra.
     */
    private static String getSetting(String key) {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("setting_value");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al leer configuración de BD: " + key, e);
        }
        return null;
    }

    /**
     * Guarda o actualiza una configuración en la base de datos.
     * @param key La clave de la configuración.
     * @param value El nuevo valor.
     */
    private static void setSetting(String key, String value) {
        // INSERT OR REPLACE (UPSERT)
        String sql = "INSERT OR REPLACE INTO app_settings (setting_key, setting_value) VALUES (?, ?)";
        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar configuración en BD: " + key, e);
        }
    }

    /**
     * Obtiene el número correlativo actual de la factura.
     * @return El número correlativo.
     */
    public static int getCurrentCorrelative() {
        try {
            String value = getSetting(KEY_CORRELATIVE);
            if (value != null) {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Correlativo en BD no es un número. Reseteando a 1.", e);
            setCorrelative(1); // Auto-corrección
        }
        return 1; // Valor por defecto
    }

    /**
     * Establece un nuevo número correlativo para la factura.
     * @param correlative El nuevo número.
     */
    public static void setCorrelative(int correlative) {
        setSetting(KEY_CORRELATIVE, String.valueOf(correlative));
    }
}
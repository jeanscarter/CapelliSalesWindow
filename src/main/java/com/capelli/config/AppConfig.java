// Archivo: src/main/java/com/capelli/config/AppConfig.java
package com.capelli.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sistema centralizado de configuración de la aplicación.
 * Lee configuraciones desde archivo properties y proporciona valores por defecto.
 */
public class AppConfig {
    
    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static final Properties props = new Properties();
    private static boolean initialized = false;
    
    // Configuración de Base de Datos
    public static final String DB_URL = "db.url";
    public static final String DB_INIT_ON_STARTUP = "db.init.on.startup";
    
    // Configuración de API BCV
    public static final String BCV_API_URL = "bcv.api.url";
    public static final String BCV_DEFAULT_RATE = "bcv.default.rate";
    public static final String BCV_UPDATE_INTERVAL_MINUTES = "bcv.update.interval.minutes";
    public static final String BCV_TIMEOUT_SECONDS = "bcv.timeout.seconds";
    
    // Configuración de UI
    public static final String APP_TITLE = "app.title";
    public static final String APP_ICON_PATH = "app.icon.path";
    public static final String APP_DEFAULT_THEME = "app.default.theme";
    public static final String APP_WINDOW_WIDTH = "app.window.width";
    public static final String APP_WINDOW_HEIGHT = "app.window.height";
    public static final String APP_MAXIMIZED = "app.maximized";
    
    // Configuración de Negocio
    public static final String PROMO_DISCOUNT_PERCENTAGE = "business.promo.discount.percentage";
    public static final String BUSINESS_VAT_PERCENTAGE = "business.vat.percentage"; // NUEVO
    public static final String MULTIPLE_WORKER_SERVICES = "business.multiple.worker.services";
    public static final String DISCOUNT_TYPES = "business.discount.types";
    public static final String PAYMENT_METHODS = "business.payment.methods";
    
    static {
        initialize();
    }
    
    /**
     * Inicializa el sistema de configuración cargando el archivo properties.
     * Utiliza EnvironmentLoader para soportar múltiples ambientes.
     */
    private static void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            EnvironmentLoader.printEnvironmentInfo();
            Properties loadedProps = EnvironmentLoader.loadEnvironmentProperties();
            
            if (loadedProps.isEmpty()) {
                LOGGER.warning("No se cargaron propiedades, usando valores por defecto");
                loadDefaults();
            } else {
                props.putAll(loadedProps);
                LOGGER.info("Configuración cargada exitosamente");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar configuración, usando valores por defecto", e);
            loadDefaults();
        }
        
        initialized = true;
    }
    
    /**
     * Carga valores por defecto en caso de que no exista el archivo de configuración.
     */
    private static void loadDefaults() {
        // Base de datos
        props.setProperty(DB_URL, "jdbc:sqlite:capelli_salon.db");
        props.setProperty(DB_INIT_ON_STARTUP, "true");
        
        // API BCV
        props.setProperty(BCV_API_URL, "https://api.exchangedyn.com/markets/quotes/usdves/bcv");
        props.setProperty(BCV_DEFAULT_RATE, "200.00");
        props.setProperty(BCV_UPDATE_INTERVAL_MINUTES, "30");
        props.setProperty(BCV_TIMEOUT_SECONDS, "10");
        
        // UI
        props.setProperty(APP_TITLE, "Ventana de Ventas - Salón de Belleza Capelli");
        props.setProperty(APP_ICON_PATH, "/icons/Logo.png");
        props.setProperty(APP_DEFAULT_THEME, "dark");
        props.setProperty(APP_WINDOW_WIDTH, "1200");
        props.setProperty(APP_WINDOW_HEIGHT, "800");
        props.setProperty(APP_MAXIMIZED, "true");
        
        // Negocio
        props.setProperty(PROMO_DISCOUNT_PERCENTAGE, "20");
        props.setProperty(BUSINESS_VAT_PERCENTAGE, "16"); // NUEVO
        props.setProperty(MULTIPLE_WORKER_SERVICES, "Mechas,Extensiones,Mantenimiento de Extensiones");
        props.setProperty(DISCOUNT_TYPES, "Ninguno,Promoción,Intercambio,Cuenta por pagar,Cuenta por Cobrar");
        props.setProperty(PAYMENT_METHODS, "TD,TC,Pago Movil,Efectivo $,Efectivo Bs,Transferencia");
    }
    
    // ===== MÉTODOS GETTER GENÉRICOS =====
    
    /**
     * Obtiene una propiedad como String.
     */
    public static String getString(String key) {
        return props.getProperty(key);
    }
    
    /**
     * Obtiene una propiedad como String con valor por defecto.
     */
    public static String getString(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }
    
    /**
     * Obtiene una propiedad como int.
     */
    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            LOGGER.warning("Error al parsear int para key: " + key + ", usando default: " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Obtiene una propiedad como double.
     */
    public static double getDouble(String key, double defaultValue) {
        try {
            // Reemplazar coma por punto para compatibilidad
            String value = props.getProperty(key, String.valueOf(defaultValue));
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException e) {
            LOGGER.warning("Error al parsear double para key: " + key + ", usando default: " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Obtiene una propiedad como boolean.
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
    
    /**
     * Obtiene una lista de valores separados por coma.
     */
    public static String[] getStringArray(String key) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return new String[0];
        }
        return value.split(",");
    }
    
    // ===== MÉTODOS GETTER ESPECÍFICOS =====
    
    // Base de datos
    public static String getDatabaseUrl() {
        return getString(DB_URL);
    }
    
    public static boolean shouldInitDatabaseOnStartup() {
        return getBoolean(DB_INIT_ON_STARTUP, true);
    }
    
    // API BCV
    public static String getBcvApiUrl() {
        return getString(BCV_API_URL);
    }
    
    public static double getDefaultBcvRate() {
        return getDouble(BCV_DEFAULT_RATE, 36.5);
    }
    
    public static int getBcvUpdateIntervalMinutes() {
        return getInt(BCV_UPDATE_INTERVAL_MINUTES, 30);
    }
    
    public static int getBcvTimeoutSeconds() {
        return getInt(BCV_TIMEOUT_SECONDS, 10);
    }
    
    // UI
    public static String getAppTitle() {
        return getString(APP_TITLE);
    }
    
    public static String getIconPath() {
        return getString(APP_ICON_PATH);
    }
    
    public static boolean isDarkModeDefault() {
        return "dark".equalsIgnoreCase(getString(APP_DEFAULT_THEME, "dark"));
    }
    
    public static int getDefaultWindowWidth() {
        return getInt(APP_WINDOW_WIDTH, 1200);
    }
    
    public static int getDefaultWindowHeight() {
        return getInt(APP_WINDOW_HEIGHT, 800);
    }
    
    public static boolean isMaximizedByDefault() {
        return getBoolean(APP_MAXIMIZED, true);
    }
    
    // Negocio
    public static double getPromoDiscountPercentage() {
        return getDouble(PROMO_DISCOUNT_PERCENTAGE, 20.0) / 100.0;
    }
    
    /**
     * Obtiene el porcentaje de IVA (ej: 0.16 para 16%).
     * @return El porcentaje de IVA.
     */
    public static double getVatPercentage() {
        return getDouble(BUSINESS_VAT_PERCENTAGE, 16.0) / 100.0;
    }
    
    public static String[] getMultipleWorkerServices() {
        return getStringArray(MULTIPLE_WORKER_SERVICES);
    }
    
    public static String[] getDiscountTypes() {
        return getStringArray(DISCOUNT_TYPES);
    }
    
    public static String[] getPaymentMethods() {
        return getStringArray(PAYMENT_METHODS);
    }
    
    // ===== MÉTODOS SETTER (para configuración en tiempo de ejecución) =====
    
    /**
     * Establece una propiedad (solo en memoria, no persiste).
     */
    public static void setProperty(String key, String value) {
        props.setProperty(key, value);
    }
    
    /**
     * Obtiene todas las propiedades (útil para debugging).
     */
    public static Properties getAllProperties() {
        return (Properties) props.clone();
    }
    
    /**
     * Muestra todas las configuraciones en consola (útil para debugging).
     */
    public static void printConfiguration() {
        LOGGER.info("=== CONFIGURACIÓN DE LA APLICACIÓN ===");
        props.forEach((key, value) -> {
            LOGGER.info(key + " = " + value);
        });
        LOGGER.info("=====================================");
    }
}
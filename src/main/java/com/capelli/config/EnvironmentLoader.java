package com.capelli.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Cargador de configuración por ambiente (desarrollo/producción).
 * Permite tener diferentes configuraciones según el ambiente.
 */
public class EnvironmentLoader {
    
    private static final Logger LOGGER = Logger.getLogger(EnvironmentLoader.class.getName());
    
    /**
     * Tipos de ambiente disponibles.
     */
    public enum Environment {
        DEVELOPMENT("dev"),
        PRODUCTION("prod"),
        DEFAULT("");
        
        private final String suffix;
        
        Environment(String suffix) {
            this.suffix = suffix;
        }
        
        public String getSuffix() {
            return suffix;
        }
        
        public String getFileName() {
            if (suffix.isEmpty()) {
                return "application.properties";
            }
            return "application-" + suffix + ".properties";
        }
    }
    
    /**
     * Obtiene el ambiente actual desde variable de sistema o por defecto.
     * @return El ambiente configurado
     */
    public static Environment getCurrentEnvironment() {
        String envProperty = System.getProperty("app.environment");
        
        if (envProperty == null) {
            envProperty = System.getenv("APP_ENVIRONMENT");
        }
        
        if ("dev".equalsIgnoreCase(envProperty) || "development".equalsIgnoreCase(envProperty)) {
            LOGGER.info("Ambiente detectado: DEVELOPMENT");
            return Environment.DEVELOPMENT;
        } else if ("prod".equalsIgnoreCase(envProperty) || "production".equalsIgnoreCase(envProperty)) {
            LOGGER.info("Ambiente detectado: PRODUCTION");
            return Environment.PRODUCTION;
        }
        
        LOGGER.info("Ambiente por defecto: DEFAULT");
        return Environment.DEFAULT;
    }
    
    /**
     * Carga las propiedades del ambiente actual.
     * Primero carga application.properties y luego sobrescribe con el específico del ambiente.
     * 
     * @return Properties con la configuración cargada
     */
    public static Properties loadEnvironmentProperties() {
        Properties props = new Properties();
        
        // 1. Cargar propiedades base
        loadPropertiesFile(Environment.DEFAULT.getFileName(), props, true);
        
        // 2. Cargar propiedades específicas del ambiente (sobrescriben las base)
        Environment currentEnv = getCurrentEnvironment();
        if (currentEnv != Environment.DEFAULT) {
            loadPropertiesFile(currentEnv.getFileName(), props, false);
        }
        
        return props;
    }
    
    /**
     * Carga un archivo de propiedades.
     * 
     * @param fileName Nombre del archivo
     * @param props Properties donde cargar
     * @param required Si es requerido (lanza error si no existe)
     */
    private static void loadPropertiesFile(String fileName, Properties props, boolean required) {
        try (InputStream inputStream = EnvironmentLoader.class.getClassLoader()
                .getResourceAsStream(fileName)) {
            
            if (inputStream != null) {
                props.load(inputStream);
                LOGGER.info("Archivo de configuración cargado: " + fileName);
            } else if (required) {
                LOGGER.warning("Archivo de configuración no encontrado: " + fileName);
            } else {
                LOGGER.info("Archivo de configuración opcional no encontrado: " + fileName);
            }
            
        } catch (IOException e) {
            if (required) {
                LOGGER.severe("Error al cargar configuración requerida: " + fileName);
            } else {
                LOGGER.warning("Error al cargar configuración opcional: " + fileName);
            }
        }
    }
    
    /**
     * Muestra información sobre el ambiente actual.
     */
    public static void printEnvironmentInfo() {
        Environment env = getCurrentEnvironment();
        LOGGER.info("====================================");
        LOGGER.info("AMBIENTE: " + env.name());
        LOGGER.info("ARCHIVO: " + env.getFileName());
        LOGGER.info("====================================");
    }
}
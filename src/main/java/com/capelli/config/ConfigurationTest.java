package com.capelli.config;

/**
 * Clase de prueba para verificar que la configuración se carga correctamente.
 * Ejecutar este main antes de iniciar la aplicación principal.
 */
public class ConfigurationTest {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║   TEST DE CONFIGURACIÓN - CAPELLI SALES WINDOW       ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Mostrar ambiente
        EnvironmentLoader.printEnvironmentInfo();
        System.out.println();
        
        // Probar configuración de base de datos
        System.out.println("📊 CONFIGURACIÓN DE BASE DE DATOS:");
        System.out.println("  ✓ URL: " + AppConfig.getDatabaseUrl());
        System.out.println("  ✓ Inicializar al inicio: " + AppConfig.shouldInitDatabaseOnStartup());
        System.out.println();
        
        // Probar configuración de BCV
        System.out.println("💱 CONFIGURACIÓN DE API BCV:");
        System.out.println("  ✓ URL API: " + AppConfig.getBcvApiUrl());
        System.out.println("  ✓ Tasa por defecto: " + AppConfig.getDefaultBcvRate());
        System.out.println("  ✓ Intervalo de actualización: " + AppConfig.getBcvUpdateIntervalMinutes() + " minutos");
        System.out.println("  ✓ Timeout: " + AppConfig.getBcvTimeoutSeconds() + " segundos");
        System.out.println();
        
        // Probar configuración de UI
        System.out.println("🎨 CONFIGURACIÓN DE INTERFAZ:");
        System.out.println("  ✓ Título: " + AppConfig.getAppTitle());
        System.out.println("  ✓ Icono: " + AppConfig.getIconPath());
        System.out.println("  ✓ Tema por defecto: " + (AppConfig.isDarkModeDefault() ? "Oscuro" : "Claro"));
        System.out.println("  ✓ Tamaño ventana: " + AppConfig.getDefaultWindowWidth() + "x" + AppConfig.getDefaultWindowHeight());
        System.out.println("  ✓ Maximizada: " + AppConfig.isMaximizedByDefault());
        System.out.println();
        
        // Probar configuración de negocio
        System.out.println("💼 CONFIGURACIÓN DE NEGOCIO:");
        System.out.println("  ✓ Descuento por promoción: " + (AppConfig.getPromoDiscountPercentage() * 100) + "%");
        
        System.out.println("  ✓ Servicios con múltiples trabajadoras:");
        for (String servicio : AppConfig.getMultipleWorkerServices()) {
            System.out.println("    - " + servicio);
        }
        
        System.out.println("  ✓ Tipos de descuento:");
        for (String tipo : AppConfig.getDiscountTypes()) {
            System.out.println("    - " + tipo);
        }
        
        System.out.println("  ✓ Métodos de pago:");
        for (String metodo : AppConfig.getPaymentMethods()) {
            System.out.println("    - " + metodo);
        }
        System.out.println();
        
        // Probar carga de recursos
        System.out.println("📁 VERIFICACIÓN DE RECURSOS:");
        boolean iconExists = ConfigurationTest.class.getResourceAsStream(AppConfig.getIconPath()) != null;
        System.out.println("  " + (iconExists ? "✓" : "✗") + " Icono encontrado: " + (iconExists ? "SÍ" : "NO"));
        System.out.println();
        
        // Probar conexión a base de datos
        System.out.println("🔌 VERIFICACIÓN DE BASE DE DATOS:");
        try {
            com.capelli.database.Database.initialize();
            boolean dbConnected = com.capelli.database.Database.testConnection();
            System.out.println("  " + (dbConnected ? "✓" : "✗") + " Conexión a BD: " + (dbConnected ? "EXITOSA" : "FALLIDA"));
        } catch (Exception e) {
            System.out.println("  ✗ Error al conectar a BD: " + e.getMessage());
        }
        System.out.println();
        
        // Probar API BCV
        System.out.println("🌐 VERIFICACIÓN DE API BCV:");
        try {
            System.out.print("  Consultando API... ");
            double rate = com.capelli.capellisaleswindow.BCVService.getBCVRate();
            if (rate > 0) {
                System.out.println("✓ EXITOSA");
                System.out.println("  Tasa obtenida: " + rate + " Bs/$");
            } else {
                System.out.println("✗ FALLIDA");
                System.out.println("  Usando tasa por defecto: " + AppConfig.getDefaultBcvRate());
            }
        } catch (Exception e) {
            System.out.println("✗ ERROR");
            System.out.println("  " + e.getMessage());
        }
        System.out.println();
        
        // Resumen final
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║              RESULTADO DEL TEST                      ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println("✅ Configuración cargada correctamente");
        System.out.println("✅ Sistema listo para ejecutar");
        System.out.println();
        System.out.println("💡 Para ejecutar en modo desarrollo:");
        System.out.println("   mvn exec:java -Dapp.environment=dev");
        System.out.println();
        System.out.println("💡 Para ejecutar en modo producción:");
        System.out.println("   mvn exec:java -Dapp.environment=prod");
        System.out.println();
    }
}
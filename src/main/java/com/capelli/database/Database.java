package com.capelli.database;

import com.capelli.config.AppConfig;
import com.capelli.config.ConfigManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Database {

    private static final Logger LOGGER = Logger.getLogger(Database.class.getName());

    /**
     * Obtiene la URL de la base de datos desde la configuración.
     */
    private static String getDatabaseUrl() {
        return AppConfig.getDatabaseUrl();
    }

    /**
     * Establece una conexión a la base de datos.
     *
     * @return Connection o null si hay error
     */
    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(getDatabaseUrl());

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                LOGGER.fine("PRAGMA foreign_keys = ON ejecutado.");
            }

            LOGGER.fine("Conexión a base de datos establecida: " + getDatabaseUrl());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al conectar a la base de datos", e);
        }
        return conn;
    }

    /**
     * Inserta o actualiza un servicio en la base de datos.
     * CORREGIDO: Usa la sintaxis UPSERT (ON CONFLICT DO UPDATE) para
     * evitar violaciones de Foreign Key.
     */
    private static void addOrUpdateService(Connection conn, String name, double pCorto, double pMedio, double pLargo, double pExt, boolean permiteCliente, double pCliente) throws SQLException {
        
        // CORRECCIÓN: Se añade 'is_active = 1' para asegurar que los servicios actualizados/creados estén activos
        String sql = "INSERT INTO services (name, price_corto, price_medio, price_largo, price_ext, permite_cliente_producto, price_cliente_producto, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, 1) " + // Añadido 'is_active' y valor '1'
                     "ON CONFLICT(name) DO UPDATE SET " +
                     "  price_corto = excluded.price_corto, " +
                     "  price_medio = excluded.price_medio, " +
                     "  price_largo = excluded.price_largo, " +
                     "  price_ext = excluded.price_ext, " +
                     "  permite_cliente_producto = excluded.permite_cliente_producto, " +
                     "  price_cliente_producto = excluded.price_cliente_producto, " +
                     "  is_active = 1"; // Añadido 'is_active = 1' en la actualización

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setDouble(2, pCorto);
            pstmt.setDouble(3, pMedio);
            pstmt.setDouble(4, pLargo);
            pstmt.setDouble(5, pExt);
            pstmt.setBoolean(6, permiteCliente);
            pstmt.setDouble(7, pCliente);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Inserta o actualiza una trabajadora.
     * CORREGIDO: Usa la sintaxis UPSERT (ON CONFLICT DO UPDATE) para
     * evitar violaciones de Foreign Key.
     */
    private static void addOrUpdateTrabajadora(Connection conn, String nombres, String apellidos, String tipo_ci, String numero_ci, String telefono) throws SQLException {
        
        String sql = "INSERT INTO trabajadoras (nombres, apellidos, tipo_ci, numero_ci, telefono, correo, foto) " +
                     "VALUES (?, ?, ?, ?, ?, NULL, NULL) " +
                     "ON CONFLICT(numero_ci) DO UPDATE SET " +
                     "  nombres = excluded.nombres, " +
                     "  apellidos = excluded.apellidos, " +
                     "  tipo_ci = excluded.tipo_ci, " +
                     "  telefono = excluded.telefono";
                     
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombres);
            pstmt.setString(2, apellidos);
            pstmt.setString(3, tipo_ci);
            pstmt.setString(4, numero_ci);
            pstmt.setString(5, telefono);
            pstmt.executeUpdate();
        }
    }

    /**
     * Inserta o actualiza una cuenta bancaria.
     */
    private static void addOrUpdateCuenta(Connection conn, String trabajadora_ci, String banco, String tipo_cuenta, String numero_cuenta, boolean es_principal) throws SQLException {
        int trabajadora_id = -1;
        String sqlGetId = "SELECT id FROM trabajadoras WHERE numero_ci = ?";
        try (PreparedStatement pstmtGetId = conn.prepareStatement(sqlGetId)) {
            pstmtGetId.setString(1, trabajadora_ci);
            ResultSet rs = pstmtGetId.executeQuery();
            if (rs.next()) {
                trabajadora_id = rs.getInt("id");
            } else {
                LOGGER.warning("No se encontró trabajadora con CI: " + trabajadora_ci + " para agregar cuenta. Saltando...");
                return; 
            }
        }
        
        String sqlCuenta = "INSERT INTO cuentas_bancarias (trabajadora_id, banco, tipo_cuenta, numero_cuenta, es_principal) " +
                           "VALUES (?, ?, ?, ?, ?) " +
                           "ON CONFLICT(numero_cuenta) DO UPDATE SET " +
                           "  trabajadora_id = excluded.trabajadora_id, " +
                           "  banco = excluded.banco, " +
                           "  tipo_cuenta = excluded.tipo_cuenta, " +
                           "  es_principal = excluded.es_principal";
                           
        try (PreparedStatement pstmt = conn.prepareStatement(sqlCuenta)) {
            pstmt.setInt(1, trabajadora_id);
            pstmt.setString(2, banco);
            pstmt.setString(3, tipo_cuenta);
            pstmt.setString(4, numero_cuenta);
            pstmt.setBoolean(5, es_principal);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Inserta o actualiza una regla de comisión.
     */
    private static void addOrUpdateCommissionRule(Connection conn, String trabajadora_ci, String service_category, double commission_rate) throws SQLException {
        int trabajadora_id = -1;
        String sqlGetId = "SELECT id FROM trabajadoras WHERE numero_ci = ?";
        try (PreparedStatement pstmtGetId = conn.prepareStatement(sqlGetId)) {
            pstmtGetId.setString(1, trabajadora_ci);
            ResultSet rs = pstmtGetId.executeQuery();
            if (rs.next()) {
                trabajadora_id = rs.getInt("id");
            } else {
                LOGGER.warning("No se encontró trabajadora con CI: " + trabajadora_ci + " para agregar regla de comisión. Saltando...");
                return; 
            }
        }

        String sqlRule = "INSERT INTO trabajadora_commission_rules (trabajadora_id, service_category, commission_rate) " +
                         "VALUES (?, ?, ?) " +
                         "ON CONFLICT(trabajadora_id, service_category) DO UPDATE SET " +
                         "  commission_rate = excluded.commission_rate";
                         
        try (PreparedStatement pstmt = conn.prepareStatement(sqlRule)) {
            pstmt.setInt(1, trabajadora_id);
            pstmt.setString(2, service_category);
            pstmt.setDouble(3, commission_rate);
            pstmt.executeUpdate();
        }
    }


    /**
     * Inicializa la base de datos creando las tablas necesarias.
     */
    public static void initialize() {
        if (!AppConfig.shouldInitDatabaseOnStartup()) {
            LOGGER.info("Inicialización de BD deshabilitada en configuración");
            return;
        }

        LOGGER.info("Inicializando base de datos...");

        String sqlClients = "CREATE TABLE IF NOT EXISTS clients (\n"
                + "    client_id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "    cedula TEXT NOT NULL UNIQUE,\n"
                + "    full_name TEXT NOT NULL,\n"
                + "    address TEXT,\n"
                + "    phone TEXT,\n"
                + "    hair_type TEXT,\n"
                + "    birth_date TEXT,\n"
                + "    last_dye_date TEXT,\n"
                + "    last_chemical_date TEXT,\n"
                + "    last_keratin_date TEXT,\n" 
                + "    extensions_type TEXT,\n"
                + "    last_extensions_maintenance_date TEXT\n"
                + ");";

        String sqlTrabajadoras = "CREATE TABLE IF NOT EXISTS trabajadoras (\n"
                + "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "    nombres TEXT NOT NULL,\n"
                + "    apellidos TEXT NOT NULL,\n"
                + "    tipo_ci TEXT,\n"
                + "    numero_ci TEXT UNIQUE,\n"
                + "    telefono TEXT,\n"
                + "    correo TEXT,\n"
                + "    foto BLOB\n"
                + ");";

        String sqlCuentas = "CREATE TABLE IF NOT EXISTS cuentas_bancarias (\n"
                + "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "    trabajadora_id INTEGER NOT NULL,\n"
                + "    banco TEXT NOT NULL,\n"
                + "    tipo_cuenta TEXT NOT NULL,\n"
                + "    numero_cuenta TEXT NOT NULL UNIQUE,\n"
                + "    es_principal BOOLEAN NOT NULL,\n"
                + "    FOREIGN KEY (trabajadora_id) REFERENCES trabajadoras (id) ON DELETE CASCADE\n"
                + ");";

        // ===== INICIO DE MODIFICACIÓN: Añadida columna 'is_active' =====
        String sqlServices = "CREATE TABLE IF NOT EXISTS services (\n"
                + "    service_id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "    name TEXT NOT NULL UNIQUE,\n"
                + "    price_corto REAL DEFAULT 0.0,\n"
                + "    price_medio REAL DEFAULT 0.0,\n"
                + "    price_largo REAL DEFAULT 0.0,\n"
                + "    price_ext REAL DEFAULT 0.0,\n"
                + "    permite_cliente_producto BOOLEAN DEFAULT 0,\n"
                + "    price_cliente_producto REAL DEFAULT 0.0,\n"
                + "    is_active BOOLEAN DEFAULT 1\n" // <-- NUEVA COLUMNA
                + ");";
        // ===== FIN DE MODIFICACIÓN =====

        String sqlSales = "CREATE TABLE IF NOT EXISTS sales (\n"
                + "    sale_id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "    client_id INTEGER,\n"
                + "    sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n"
                + "    subtotal REAL NOT NULL,\n"
                + "    discount_type TEXT,\n"
                + "    discount_amount REAL,\n"
                + "    total REAL NOT NULL,\n"
                + "    FOREIGN KEY (client_id) REFERENCES clients (client_id)\n"
                + ");";

        String sqlSalePayments = "CREATE TABLE IF NOT EXISTS sale_payments (\n"
                + "    payment_id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "    sale_id INTEGER NOT NULL,\n"
                + "    monto REAL NOT NULL,\n"
                + "    moneda TEXT NOT NULL, \n" // '$' o 'Bs'
                + "    metodo_pago TEXT NOT NULL,\n"
                + "    destino_pago TEXT,\n"
                + "    referencia_pago TEXT,\n"
                + "    tasa_bcv_al_pago REAL DEFAULT 0.0,\n"
                + "    FOREIGN KEY (sale_id) REFERENCES sales (sale_id) ON DELETE CASCADE\n"
                + ");";

        String sqlSaleItems = "CREATE TABLE IF NOT EXISTS sale_items (\n"
                + "    sale_item_id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "    sale_id INTEGER NOT NULL,\n"
                + "    service_id INTEGER NOT NULL,\n"
                + "    employee_id INTEGER NOT NULL,\n"
                + "    price_at_sale REAL NOT NULL,\n"
                + "    FOREIGN KEY (sale_id) REFERENCES sales (sale_id),\n"
                + "    FOREIGN KEY (service_id) REFERENCES services (service_id),\n" // <-- Esta es la restricción que falló
                + "    FOREIGN KEY (employee_id) REFERENCES trabajadoras (id)\n"
                + ");";

        String sqlTips = "CREATE TABLE IF NOT EXISTS tips (\n"
                + "    tip_id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "    sale_id INTEGER,\n"
                + "    recipient_name TEXT NOT NULL,\n"
                + "    amount REAL NOT NULL,\n"
                + "    FOREIGN KEY (sale_id) REFERENCES sales (sale_id)\n"
                + ");";

        String sqlSettings = "CREATE TABLE IF NOT EXISTS app_settings (\n"
                + "    setting_key TEXT PRIMARY KEY NOT NULL,\n"
                + "    setting_value TEXT NOT NULL\n"
                + ");";
        
        String sqlCommissionRules = "CREATE TABLE IF NOT EXISTS trabajadora_commission_rules (\n"
                + "    rule_id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "    trabajadora_id INTEGER NOT NULL,\n"
                + "    service_category TEXT NOT NULL,\n"
                + "    commission_rate REAL NOT NULL,\n"
                + "    UNIQUE(trabajadora_id, service_category),\n"
                + "    FOREIGN KEY (trabajadora_id) REFERENCES trabajadoras (id) ON DELETE CASCADE\n"
                + ");";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {

            stmt.execute(sqlClients);
            LOGGER.info("Tabla 'clients' verificada/creada");

            stmt.execute(sqlTrabajadoras);
            LOGGER.info("Tabla 'trabajadoras' verificada/creada");

            stmt.execute(sqlCuentas);
            LOGGER.info("Tabla 'cuentas_bancarias' verificada/creada");

            stmt.execute(sqlServices);
            LOGGER.info("Tabla 'services' verificada/creada");

            stmt.execute(sqlSales);
            LOGGER.info("Tabla 'sales' verificada/creada (actualizada sin campos de pago)");

            stmt.execute(sqlSalePayments);
            LOGGER.info("Tabla 'sale_payments' verificada/creada");

            stmt.execute(sqlSaleItems);
            LOGGER.info("Tabla 'sale_items' verificada/creada");

            stmt.execute(sqlTips);
            LOGGER.info("Tabla 'tips' verificada/creada");
            
            stmt.execute(sqlSettings);
            LOGGER.info("Tabla 'app_settings' verificada/creada");
            
            stmt.execute(sqlCommissionRules);
            LOGGER.info("Tabla 'trabajadora_commission_rules' verificada/creada");
            
            String sqlInitCorr = "INSERT OR IGNORE INTO app_settings (setting_key, setting_value) VALUES ('" + ConfigManager.KEY_CORRELATIVE + "', '1');";
            stmt.execute(sqlInitCorr);
            LOGGER.info("Correlativo inicial verificado/creado.");
            
            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN bcv_rate_at_sale REAL DEFAULT 0.0");
                LOGGER.info("Columna 'bcv_rate_at_sale' agregada a la tabla 'sales'.");
            } catch (SQLException e) {
                if (e.getMessage().contains("duplicate column name")) {
                    LOGGER.info("Columna 'bcv_rate_at_sale' ya existe en 'sales'.");
                } else {
                    LOGGER.log(Level.SEVERE, "Error al alterar la tabla 'sales'", e);
                }
            }
            
            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN vat_amount REAL DEFAULT 0.0");
                LOGGER.info("Columna 'vat_amount' agregada a la tabla 'sales'.");
            } catch (SQLException e) {
                if (e.getMessage().contains("duplicate column name")) {
                    LOGGER.info("Columna 'vat_amount' ya existe en 'sales'.");
                } else {
                    LOGGER.log(Level.SEVERE, "Error al alterar la tabla 'sales' para VAT", e);
                }
            }
            
            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN correlative_number TEXT");
                LOGGER.info("Columna 'correlative_number' agregada a la tabla 'sales'.");
            } catch (SQLException e) {
                if (e.getMessage().contains("duplicate column name")) {
                    LOGGER.info("Columna 'correlative_number' ya existe en 'sales'.");
                } else {
                    LOGGER.log(Level.SEVERE, "Error al alterar la tabla 'sales' para Correlativo", e);
                }
            }
            
            try {
                stmt.execute("ALTER TABLE services ADD COLUMN service_category TEXT");
                LOGGER.info("Columna 'service_category' agregada a la tabla 'services'.");
            } catch (SQLException e) {
                if (e.getMessage().contains("duplicate column name")) {
                    LOGGER.info("Columna 'service_category' ya existe en 'services'.");
                } else {
                    LOGGER.log(Level.SEVERE, "Error al alterar la tabla 'services' para service_category", e);
                }
            }
            
            try {
                stmt.execute("ALTER TABLE services ADD COLUMN is_active BOOLEAN DEFAULT 1");
                LOGGER.info("Columna 'is_active' agregada a la tabla 'services'.");
            } catch (SQLException e) {
                if (e.getMessage().contains("duplicate column name")) {
                    LOGGER.info("Columna 'is_active' ya existe en 'services'.");
                } else {
                    LOGGER.log(Level.SEVERE, "Error al alterar la tabla 'services' para is_active", e);
                }
            }

            LOGGER.info("Agregando/Actualizando lista de servicios...");
            
            addOrUpdateService(conn, "Lavado", 10.0, 0.0, 0.0, 0.0, true, 8.0); 
            addOrUpdateService(conn, "Secado", 10.0, 12.0, 15.0, 20.0, false, 0.0);
            addOrUpdateService(conn, "Ondas", 15.0, 20.0, 35.0, 40.0, false, 0.0);
            addOrUpdateService(conn, "Corte Puntas", 20.0, 0.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Corte Elaborado", 25.0, 0.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Maquillaje", 60.0, 70.0, 80.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Peinados", 40.0, 50.0, 60.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Color (Tinte)", 40.0, 50.0, 0.0, 0.0, true, 25.0); 
            addOrUpdateService(conn, "Mechas", 80.0, 100.0, 120.0, 150.0, false, 0.0);
            addOrUpdateService(conn, "Cejas", 5.0, 8.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Bozo", 5.0, 8.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Manicure Tradicional", 12.0, 0.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Pedicure Tradicional", 12.0, 0.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Keratina", 60.0, 120.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Hidratación solo", 15.0, 20.0, 25.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Hidratación Fusio-Dose", 35.0, 0.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Secado con extensiones", 25.0, 30.0, 45.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Planchado", 15.0, 20.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Pestañas", 10.0, 0.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Manicure Gelish", 15.0, 0.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Manicure Rubber", 18.0, 0.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Manicure Polygel", 20.0, 0.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Pedicure Gelish", 15.0, 0.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Hidratación M/P", 15.0, 0.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Extensiones (Medio Paquete)", 60.0, 0.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Extensiones (1 Paquete)", 120.0, 0.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Extensiones (2 Paquetes)", 140.0, 0.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Extensiones (3 Paquetes)", 160.0, 0.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Extensiones (4 Paquetes)", 180.0, 0.0, 0.0, 0.0, false, 0.0);
            addOrUpdateService(conn, "Productos", 35.0, 0.0, 0.0, 0.0, false, 0.0);

            LOGGER.info("Lista de servicios actualizada.");
            
            stmt.execute("UPDATE services SET service_category = 'Peluqueria' WHERE name IN ('Secado', 'Corte Puntas', 'Corte Elaborado', 'Peinados', 'Ondas', 'Planchado', 'Maquillaje', 'Pestañas', 'Cejas', 'Bozo', 'Secado con extensiones')");
            stmt.execute("UPDATE services SET service_category = 'Quimico' WHERE name IN ('Color (Tinte)', 'Mechas', 'Keratina', 'Hidratación solo', 'Hidratación Fusio-Dose')");
            stmt.execute("UPDATE services SET service_category = 'Manos/Pies' WHERE name IN ('Manicure Tradicional', 'Pedicure Tradicional', 'Manicure Gelish', 'Manicure Rubber', 'Manicure Polygel', 'Pedicure Gelish', 'Hidratación M/P')");
            stmt.execute("UPDATE services SET service_category = 'Lavado' WHERE name = 'Lavado'");
            stmt.execute("UPDATE services SET service_category = 'Extensiones' WHERE name IN ('Extensiones (Medio Paquete)', 'Extensiones (1 Paquete)', 'Extensiones (2 Paquetes)', 'Extensiones (3 Paquetes)', 'Extensiones (4 Paquetes)')");
            stmt.execute("UPDATE services SET service_category = 'Otros' WHERE name IN ('Productos')");
            LOGGER.info("Categorías de servicio por defecto re-asignadas.");
            
            try {
                stmt.execute("UPDATE services SET is_active = 0 WHERE name IN ('Hidratación + Secado', 'Aplicación de Tinte', 'Mantenimiento', 'Extensiones')");
                LOGGER.info("Servicios obsoletos desactivados (is_active = 0).");
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error al desactivar servicios obsoletos", e);
            }
        
            LOGGER.info("Agregando/Actualizando lista de trabajadoras y cuentas...");

            addOrUpdateTrabajadora(conn, "Dayana", "Govea", "V", "18522231", "04127915851");
            addOrUpdateCuenta(conn, "18522231", "Banco Provincial", "Corriente", "01080511200100296908", true);

            addOrUpdateTrabajadora(conn, "Maria Virginia", "Romero", "V", "31085005", "04143604499");
            addOrUpdateCuenta(conn, "31085005", "Banesco", "Corriente", "01340039330391056651", true);

            addOrUpdateTrabajadora(conn, "Pascualina", "Gutierrez", "V", "5562378", "04146638330");
            addOrUpdateCuenta(conn, "5562378", "Banco Provincial", "Ahorro", "01080086240200360744", true);

            addOrUpdateTrabajadora(conn, "Aurora Sofia", "Exposito", "V", "27683374", "04242092890");
            addOrUpdateCuenta(conn, "27683374", "Banesco", "Corriente", "01340039330391055077", true);

            addOrUpdateTrabajadora(conn, "Jeimy", "Añez", "V", "18921264", "04246695087");
            addOrUpdateCuenta(conn, "18921264", "Banco Provincial", "Corriente", "01080059550100393036", true);

            addOrUpdateTrabajadora(conn, "Belkis", "Gutierrez", "V", "9395233", "04146126300");
            addOrUpdateCuenta(conn, "9395233", "Banco Provincial", "Corriente", "01080086260100175613", true);

            addOrUpdateTrabajadora(conn, "Milagros", "Gutierrez", "V", "24342800", "04246194365");
            addOrUpdateCuenta(conn, "24342800", "Banco Provincial", "Ahorro", "01080302190200068019", true);

            addOrUpdateTrabajadora(conn, "Maria", "Diaz", "V", "7774946", "04246464683");
           
            addOrUpdateTrabajadora(conn, "Rosa Maria", "Gutierrez", "V", "9200133", "04246889337");
         
            addOrUpdateCuenta(conn, "9200133", "Banesco", "Corriente", "01340946380001307454", true); 
            addOrUpdateCuenta(conn, "9200133", "Banco Nacional de Crédito (BNC)", "Corriente", "01160148150014749505", false);
            addOrUpdateCuenta(conn, "9200133", "Bancamiga", "Corriente", "01720112381125322600", false);
            addOrUpdateCuenta(conn, "9200133", "Banesco", "Corriente", "01340077650773172568", false);

            addOrUpdateTrabajadora(conn, "Jaqueline", "Añez", "V", "24734839", "04246703185");
            addOrUpdateCuenta(conn, "24734839", "Banco Provincial", "Corriente", "01080059500100533199", true);

            LOGGER.info("Lista de trabajadoras y cuentas actualizada.");
            
            LOGGER.info("Agregando/Actualizando reglas de comisión...");
            
            // --- Reglas de Comisión Base (por categoría) ---
            
            // Maria Diaz (CI 7774946)
            addOrUpdateCommissionRule(conn, "7774946", "Manos/Pies", 0.70);
            
            // Jaqueline Añez (CI 24734839)
            addOrUpdateCommissionRule(conn, "24734839", "Peluqueria", 0.50);
            addOrUpdateCommissionRule(conn, "24734839", "Quimico", 0.40); // Regla "Hidratación 40%" se aplica a Quimicos
            
            // Dayana Govea (CI 18522231)
            addOrUpdateCommissionRule(conn, "18522231", "Peluqueria", 0.50);
            addOrUpdateCommissionRule(conn, "18522231", "Quimico", 0.40); // Regla "Hidratación 40%"
            
            // Maria Virginia Romero (CI 31085005)
            addOrUpdateCommissionRule(conn, "31085005", "Peluqueria", 0.50);
            addOrUpdateCommissionRule(conn, "31085005", "Quimico", 0.40); // Regla "Hidratación 40%"
            
            // Belkis Gutierrez (CI 9395233)
            addOrUpdateCommissionRule(conn, "9395233", "Peluqueria", 0.65);
            addOrUpdateCommissionRule(conn, "9395233", "Quimico", 0.50);
            
            // Aurora Sofia Exposito (CI 27683374)
            addOrUpdateCommissionRule(conn, "27683374", "Peluqueria", 0.60);
            addOrUpdateCommissionRule(conn, "27683374", "Quimico", 0.50);
            
            // Jeimy Añez (CI 18921264)
            addOrUpdateCommissionRule(conn, "18921264", "Peluqueria", 0.60);
            addOrUpdateCommissionRule(conn, "18921264", "Quimico", 0.50);
            
            // Pascualina Gutierrez (CI 5562378)
            addOrUpdateCommissionRule(conn, "5562378", "Peluqueria", 0.60);
            addOrUpdateCommissionRule(conn, "5562378", "Quimico", 0.50);
            
            // Milagros Gutierrez (CI 24342800)
            addOrUpdateCommissionRule(conn, "24342800", "Peluqueria", 0.60);
            addOrUpdateCommissionRule(conn, "24342800", "Quimico", 0.50);
            
            LOGGER.info("Reglas de comisión actualizadas.");


            LOGGER.info("Base de datos inicializada correctamente");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar la base de datos", e);
        }
    }

    /**
     * Verifica que la conexión a la base de datos esté funcionando.
     *
     * @return true si la conexión es exitosa
     */
    public static boolean testConnection() {
        try (Connection conn = connect()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Test de conexión falló", e);
            return false;
        }
    }
}
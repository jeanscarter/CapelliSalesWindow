package com.capelli.database;

import com.capelli.config.AppConfig;
import java.sql.Connection;
import java.sql.DriverManager;
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
            LOGGER.fine("Conexión a base de datos establecida: " + getDatabaseUrl());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al conectar a la base de datos", e);
        }
        return conn;
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

        String sqlServices = "CREATE TABLE IF NOT EXISTS services (\n"
                + "    service_id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "    name TEXT NOT NULL UNIQUE,\n"
                + "    price_corto REAL DEFAULT 0.0,\n"
                + "    price_medio REAL DEFAULT 0.0,\n"
                + "    price_largo REAL DEFAULT 0.0,\n"
                + "    price_ext REAL DEFAULT 0.0\n"
                + ");";

        String sqlSales = "CREATE TABLE IF NOT EXISTS sales (\n"
                + "    sale_id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "    client_id INTEGER,\n"
                + "    sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n"
                + "    subtotal REAL NOT NULL,\n"
                + "    discount_type TEXT,\n"
                + "    discount_amount REAL,\n"
                + "    total REAL NOT NULL,\n"
                + "    payment_method TEXT,\n"
                + "    currency TEXT,\n"
                + "    payment_destination TEXT,\n"
                + "    FOREIGN KEY (client_id) REFERENCES clients (client_id)\n"
                + ");";

        String sqlSaleItems = "CREATE TABLE IF NOT EXISTS sale_items (\n"
                + "    sale_item_id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "    sale_id INTEGER NOT NULL,\n"
                + "    service_id INTEGER NOT NULL,\n"
                + "    employee_id INTEGER NOT NULL,\n"
                + "    price_at_sale REAL NOT NULL,\n"
                + "    FOREIGN KEY (sale_id) REFERENCES sales (sale_id),\n"
                + "    FOREIGN KEY (service_id) REFERENCES services (service_id),\n"
                + "    FOREIGN KEY (employee_id) REFERENCES trabajadoras (id)\n"
                + ");";

        String sqlTips = "CREATE TABLE IF NOT EXISTS tips (\n"
                + "    tip_id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "    sale_id INTEGER,\n"
                + "    recipient_name TEXT NOT NULL,\n"
                + "    amount REAL NOT NULL,\n"
                + "    FOREIGN KEY (sale_id) REFERENCES sales (sale_id)\n"
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
            LOGGER.info("Tabla 'sales' verificada/creada");

            stmt.execute(sqlSaleItems);
            LOGGER.info("Tabla 'sale_items' verificada/creada");

            stmt.execute(sqlTips);
            LOGGER.info("Tabla 'tips' verificada/creada");

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

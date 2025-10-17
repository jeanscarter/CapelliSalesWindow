package com.capelli.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final String URL = "jdbc:sqlite:capelli_salon.db";

    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public static void initialize() {
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
                + "    price REAL NOT NULL\n"
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
                + "    FOREIGN KEY (employee_id) REFERENCES employees (employee_id)\n"
                + ");";

        String sqlTips = "CREATE TABLE IF NOT EXISTS tips (\n"
                + "    tip_id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "    sale_id INTEGER,\n"
                + "    recipient_name TEXT NOT NULL,\n"
                + "    amount REAL NOT NULL,\n"
                + "    FOREIGN KEY (sale_id) REFERENCES sales (sale_id)\n"
                + ");";


        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlClients);
            stmt.execute(sqlTrabajadoras); 
            stmt.execute(sqlCuentas);      
            stmt.execute(sqlServices);
            stmt.execute(sqlSales);
            stmt.execute(sqlSaleItems);
            stmt.execute(sqlTips);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
package com.capelli.database;

import com.capelli.model.Service;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceDAO {

    /**
     * Devuelve todos los servicios como un Map, usando el nombre como clave.
     */
    public Map<String, Service> getAllMap() throws SQLException {
        Map<String, Service> servicesMap = new HashMap<>();
        // Reutilizamos el código de getAll() para llenar el Map
        for (Service service : getAll()) {
            servicesMap.put(service.getName(), service);
        }
        return servicesMap;
    }

    /**
     * Devuelve todos los servicios como una Lista. (MÉTODO REQUERIDO)
     */
    public List<Service> getAll() throws SQLException {
        List<Service> servicesList = new ArrayList<>();
        // AÑADIDO: service_category
        String sql = "SELECT service_id, name, price_corto, price_medio, price_largo, price_ext, "
                 + "permite_cliente_producto, price_cliente_producto, service_category "
                 + "FROM services ORDER BY name";

        try (Connection conn = Database.connect(); 
             Statement stmt = conn.createStatement(); 
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Service service = new Service();
                service.setId(rs.getInt("service_id"));
                service.setName(rs.getString("name"));
                service.setPrice_corto(rs.getDouble("price_corto"));
                service.setPrice_medio(rs.getDouble("price_medio"));
                service.setPrice_largo(rs.getDouble("price_largo"));
                service.setPrice_ext(rs.getDouble("price_ext"));
                service.setPermiteClienteProducto(rs.getBoolean("permite_cliente_producto"));
                service.setPriceClienteProducto(rs.getDouble("price_cliente_producto"));
                service.setService_category(rs.getString("service_category")); // AÑADIDO
                servicesList.add(service);
            }
        }
        return servicesList;
    }

    public void save(Service service) throws SQLException {
        if (service.getId() == 0) {
       
            // AÑADIDO: service_category
            String sql = "INSERT INTO services(name, price_corto, price_medio, price_largo, price_ext, "
                       + "permite_cliente_producto, price_cliente_producto, service_category) "
                       + "VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, service.getName());
                pstmt.setDouble(2, service.getPrice_corto());
                pstmt.setDouble(3, service.getPrice_medio());
                pstmt.setDouble(4, service.getPrice_largo());
                pstmt.setDouble(5, service.getPrice_ext());
                pstmt.setBoolean(6, service.isPermiteClienteProducto()); 
                pstmt.setDouble(7, service.getPriceClienteProducto());  
                pstmt.setString(8, service.getService_category()); // AÑADIDO
                pstmt.executeUpdate();
            }
        } else {
       
            // AÑADIDO: service_category
            String sql = "UPDATE services SET name = ?, price_corto = ?, price_medio = ?, price_largo = ?, "
                       + "price_ext = ?, permite_cliente_producto = ?, price_cliente_producto = ?, "
                       + "service_category = ? "
                       + "WHERE service_id = ?";
            try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, service.getName());
                pstmt.setDouble(2, service.getPrice_corto());
                pstmt.setDouble(3, service.getPrice_medio());
                pstmt.setDouble(4, service.getPrice_largo());
                pstmt.setDouble(5, service.getPrice_ext());
                pstmt.setBoolean(6, service.isPermiteClienteProducto()); 
                pstmt.setDouble(7, service.getPriceClienteProducto());    
                pstmt.setString(8, service.getService_category()); // AÑADIDO
                pstmt.setInt(9, service.getId()); // CAMBIO DE ÍNDICE
                pstmt.executeUpdate();
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM services WHERE service_id = ?";
        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
}
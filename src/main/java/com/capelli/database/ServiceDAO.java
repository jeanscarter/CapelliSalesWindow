package com.capelli.database;

import com.capelli.model.Service;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceDAO {

    public List<Service> getAll() throws SQLException {
        List<Service> services = new ArrayList<>();
        // Consulta SQL actualizada para incluir las nuevas columnas de precio
        String sql = "SELECT service_id, name, price_corto, price_medio, price_largo, price_ext FROM services ORDER BY name";

        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Se asume que el modelo Service tiene setters para las nuevas columnas
                Service service = new Service();
                service.setId(rs.getInt("service_id"));
                service.setName(rs.getString("name"));
                service.setPrice_corto(rs.getDouble("price_corto"));
                service.setPrice_medio(rs.getDouble("price_medio"));
                service.setPrice_largo(rs.getDouble("price_largo"));
                service.setPrice_ext(rs.getDouble("price_ext"));
                services.add(service);
            }
        }
        return services;
    }

    public void save(Service service) throws SQLException {
        if (service.getId() == 0) {
            // Insertar nuevo servicio
            String sql = "INSERT INTO services(name, price_corto, price_medio, price_largo, price_ext) VALUES(?, ?, ?, ?, ?)";
            try (Connection conn = Database.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, service.getName());
                pstmt.setDouble(2, service.getPrice_corto());
                pstmt.setDouble(3, service.getPrice_medio());
                pstmt.setDouble(4, service.getPrice_largo());
                pstmt.setDouble(5, service.getPrice_ext());
                pstmt.executeUpdate();
            }
        } else {
            // Actualizar servicio existente
            String sql = "UPDATE services SET name = ?, price_corto = ?, price_medio = ?, price_largo = ?, price_ext = ? WHERE service_id = ?";
            try (Connection conn = Database.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, service.getName());
                pstmt.setDouble(2, service.getPrice_corto());
                pstmt.setDouble(3, service.getPrice_medio());
                pstmt.setDouble(4, service.getPrice_largo());
                pstmt.setDouble(5, service.getPrice_ext());
                pstmt.setInt(6, service.getId());
                pstmt.executeUpdate();
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM services WHERE service_id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
}
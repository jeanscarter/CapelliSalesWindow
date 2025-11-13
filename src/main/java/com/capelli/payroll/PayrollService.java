package com.capelli.payroll;

import com.capelli.database.Database;
import com.capelli.database.TrabajadoraDAO;
import com.capelli.model.CuentaBancaria;
import com.capelli.model.Trabajadora;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PayrollService {

    /**
     * Calcula la nómina basándose en reglas de comisión complejas y hardcodeadas.
     * Esta versión REEMPLAZA la lógica de la tabla 'trabajadora_commission_rules'.
     */
    public List<PayrollResult> calculatePayroll(LocalDate startDate, LocalDate endDate) throws SQLException, IOException {
        
        // 1. Obtener todas las trabajadoras
        TrabajadoraDAO trabajadoraDAO = new TrabajadoraDAO();
        Map<Integer, Trabajadora> trabajadorasMap = trabajadoraDAO.getAll().stream()
                .collect(Collectors.toMap(Trabajadora::getId, t -> t));
        
        // Mapa para acumular las comisiones en Java
        Map<Integer, Double> commissionTotals = new HashMap<>();
        for (Integer id : trabajadorasMap.keySet()) {
            commissionTotals.put(id, 0.0);
        }

        // 2. SQL para obtener TODOS los items de venta individuales
        String sql = "SELECT "
                + "    si.employee_id, "
                + "    si.price_at_sale, "
                + "    svc.name AS service_name, "
                + "    svc.service_category, "
                + "    (t.nombres || ' ' || t.apellidos) as trabajadora_name "
                + "FROM "
                + "    sale_items si "
                + "JOIN "
                + "    sales s ON si.sale_id = s.sale_id "
                + "JOIN "
                + "    services svc ON si.service_id = svc.service_id "
                + "JOIN "
                + "    trabajadoras t ON si.employee_id = t.id "
                + "WHERE "
                + "    DATE(s.sale_date, 'localtime') BETWEEN ? AND ? ";

        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startDate.toString());
            pstmt.setString(2, endDate.toString());

            ResultSet rs = pstmt.executeQuery();

            // 3. Iterar en Java y aplicar lógica de comisión
            while (rs.next()) {
                int employee_id = rs.getInt("employee_id");
                double price = rs.getDouble("price_at_sale");
                String serviceName = rs.getString("service_name");
                String serviceCategory = rs.getString("service_category");
                String trabajadoraName = rs.getString("trabajadora_name");
                
                double commissionForItem = calculateCommissionForItem(trabajadoraName, serviceName, serviceCategory, price);
                
                // Acumular la comisión
                commissionTotals.merge(employee_id, commissionForItem, Double::sum);
            }
        }

        // 4. Construir el resultado final
        List<PayrollResult> results = new ArrayList<>();
        for (Map.Entry<Integer, Trabajadora> entry : trabajadorasMap.entrySet()) {
            int id = entry.getKey();
            Trabajadora trabajadora = entry.getValue();
            double totalCommission = commissionTotals.getOrDefault(id, 0.0);
            
            Optional<CuentaBancaria> primaryAccount = trabajadora.getCuentaPrincipal();
            results.add(new PayrollResult(trabajadora, totalCommission, primaryAccount.orElse(null)));
        }

        return results;
    }

    /**
     * Lógica de comisión hardcodeada basada en las reglas del usuario.
     */
    private double calculateCommissionForItem(String tName, String sName, String sCat, double price) {
        
        // Definición de la categoría "Peluqueria" según el usuario
        boolean isUserPeluqueria = sName.equals("Secado") || sName.equals("Maquillaje") || sName.equals("Ondas");
        boolean isDepilacion = sName.equals("Cejas") || sName.equals("Bozo");

        // --- Reglas para Maria Diaz ---
        if (tName.equals("Maria Diaz")) {
            if (sCat.equals("Manos/Pies")) {
                return price * 0.70; // 70%
            }
        }

        // --- Reglas para Jaqueline Añez, Dayana Govea, Maria Virginia Romero ---
        else if (tName.equals("Jaqueline Añez") || 
                 tName.equals("Dayana Govea") || 
                 tName.equals("Maria Virginia Romero")) {

            // Regla específica de Maria Virginia
            if (tName.equals("Maria Virginia Romero")) {
                if (isDepilacion) {
                    return price * 0.50; // 50%
                }
            }
            
            // Peluqueria (Usuario)
            if (isUserPeluqueria) {
                return price * 0.50; // 50%
            }

            // Regla Lavado
            if (sCat.equals("Lavado")) {
                // Precios fijos para Lavado (usando tolerancia 0.01)
                if (Math.abs(price - 10.0) < 0.01) return 4.0;
                if (Math.abs(price - 8.0) < 0.01) return 3.0;
                if (Math.abs(price - 12.0) < 0.01) return 4.8;
                if (Math.abs(price - 15.0) < 0.01) return 6.0;
                // Si el precio del lavado no es ninguno de esos, se usa 40%
                return price * 0.40;
            }

            // Regla Hidratación
            if (sName.equals("Hidratación Fusio-Dose")) {
                return 8.0; // $8 monto fijo
            }
            // "Hidratación 40%" (asumimos que aplica a 'Hidratación solo' y otras no-Fusio)
            if (sCat.equals("Quimico") && sName.contains("Hidratación")) {
                 return price * 0.40; // 40%
            }

            // Regla Extensiones
            if (sName.equals("Extensiones (1 Paquete)")) return 10.0;
            if (sName.equals("Extensiones (2 Paquetes)")) return 20.0;
            if (sName.equals("Extensiones (3 Paquetes)")) return 15.0; 
            // (Medio paquete y 4 paquetes no tienen regla para este grupo)
        }

        // --- Reglas para Belkis Gutierrez ---
        else if (tName.equals("Belkis Gutierrez")) {
            if (sName.equals("Keratina")) {
                return price * 0.70; // 70% (Excepción sobre Químicos)
            }
            if (isDepilacion) {
                return price * 0.50; // 50% (Excepción sobre Peluqueria)
            }
            if (isUserPeluqueria) {
                return price * 0.65; // 65%
            }
            if (sCat.equals("Quimico")) {
                return price * 0.50; // 50%
            }
        }

        // --- Reglas para Aurora Sofia Exposito ("Sofia") ---
        else if (tName.equals("Aurora Sofia Exposito")) {
            if (isDepilacion) {
                return price * 0.50; // 50% (Excepción sobre Peluqueria)
            }
            if (isUserPeluqueria) {
                return price * 0.60; // 60%
            }
            if (sCat.equals("Quimico")) {
                return price * 0.50; // 50%
            }
        }
        
        // --- Reglas para Jeimy Añez ---
        else if (tName.equals("Jeimy Añez")) {
            // Extensiones (Excepción)
            if (sName.equals("Extensiones (1 Paquete)")) return 20.0;
            if (sName.equals("Extensiones (2 Paquetes)")) return 30.0;
            if (sName.equals("Extensiones (3 Paquetes)") || sName.equals("Extensiones (4 Paquetes)")) {
                return 40.0; // "3 paquetes o más"
            }
            // (Medio Paquete no especificado)
            
            if (isUserPeluqueria) {
                return price * 0.60; // 60%
            }
            if (sCat.equals("Quimico")) {
                return price * 0.50; // 50%
            }
        }

        // --- Reglas para Pascualina Gutierrez ("Pascuala") ---
        else if (tName.equals("Pascualina Gutierrez")) {
            if (isUserPeluqueria) {
                return price * 0.60; // 60%
            }
            if (sCat.equals("Quimico")) {
                return price * 0.50; // 50%
            }
        }

        // --- Reglas para Milagros Gutierrez ---
        else if (tName.equals("Milagros Gutierrez")) {
            if (isUserPeluqueria) {
                return price * 0.60; // 60%
            }
            if (sCat.equals("Quimico")) {
                return price * 0.50; // 50%
            }
        }

        // --- Fallback ---
        // Si no se define ninguna regla, la comisión es 0
        return 0.0;
    }
}
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PayrollService {

    public List<PayrollResult> calculatePayroll(LocalDate startDate, LocalDate endDate) throws SQLException, IOException {
        
        // 1. Obtener todas las trabajadoras y sus cuentas
        TrabajadoraDAO trabajadoraDAO = new TrabajadoraDAO();
        Map<Integer, Trabajadora> trabajadorasMap = trabajadoraDAO.getAll().stream()
                .collect(Collectors.toMap(Trabajadora::getId, t -> t));

        // 2. SQL para calcular las comisiones totales
        String sql = "SELECT "
                + "    si.employee_id, "
                + "    SUM(si.price_at_sale * COALESCE(cr.commission_rate, 0.0)) as total_commission "
                + "FROM "
                + "    sale_items si "
                + "JOIN "
                + "    sales s ON si.sale_id = s.sale_id "
                + "JOIN "
                + "    services svc ON si.service_id = svc.service_id "
                + "LEFT JOIN "
                + "    trabajadora_commission_rules cr ON si.employee_id = cr.trabajadora_id AND svc.service_category = cr.service_category "
                + "WHERE "
                + "    DATE(s.sale_date, 'localtime') BETWEEN ? AND ? "
                + "GROUP BY "
                + "    si.employee_id";

        List<PayrollResult> results = new ArrayList<>();

        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startDate.toString());
            pstmt.setString(2, endDate.toString());

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int employee_id = rs.getInt("employee_id");
                double total_commission = rs.getDouble("total_commission");

                Trabajadora trabajadora = trabajadorasMap.get(employee_id);
                if (trabajadora != null) {
                    // Marcarla como procesada para luego añadir las que no tuvieron ventas
                    trabajadorasMap.remove(employee_id); 
                    
                    Optional<CuentaBancaria> primaryAccount = trabajadora.getCuentaPrincipal();
                    results.add(new PayrollResult(trabajadora, total_commission, primaryAccount.orElse(null)));
                }
            }
        }

        // 3. Añadir trabajadoras restantes que no tuvieron ventas (comisión 0)
        for (Trabajadora trabajadora : trabajadorasMap.values()) {
            Optional<CuentaBancaria> primaryAccount = trabajadora.getCuentaPrincipal();
            results.add(new PayrollResult(trabajadora, 0.0, primaryAccount.orElse(null)));
        }

        return results;
    }
}
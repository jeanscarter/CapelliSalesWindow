package com.capelli.payroll;

import com.capelli.database.CommissionRuleDAO;
import com.capelli.database.Database;
import com.capelli.database.TrabajadoraDAO;
import com.capelli.model.CommissionRule;
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
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PayrollService {

    private static final Logger LOGGER = Logger.getLogger(PayrollService.class.getName());

    /**
     * Calcula la nómina basándose en:
     * 1. Ventas y reglas de comisión (DB).
     * 2. Pagos manuales (Abono Manual Staff).
     * 3. Propinas registradas.
     * 4. Bonos fijos semanales configurados en la trabajadora.
     */
    public List<PayrollResult> calculatePayroll(LocalDate startDate, LocalDate endDate) throws SQLException, IOException {
        
        // 1. Obtener todas las trabajadoras
        TrabajadoraDAO trabajadoraDAO = new TrabajadoraDAO();
        List<Trabajadora> todasLasTrabajadoras = trabajadoraDAO.getAll();
        
        // Mapa ID -> Trabajadora
        Map<Integer, Trabajadora> trabajadorasMap = todasLasTrabajadoras.stream()
                .collect(Collectors.toMap(Trabajadora::getId, t -> t));
        
        // 2. Cargar reglas de comisión
        CommissionRuleDAO commissionRuleDAO = new CommissionRuleDAO();
        List<CommissionRule> allRules = commissionRuleDAO.getAll();
        
        // Mapa: Key="trabajadora_id-service_category"
        Map<String, Double> ruleMap = allRules.stream()
                .collect(Collectors.toMap(
                        rule -> rule.getTrabajadora_id() + "-" + rule.getService_category(),
                        CommissionRule::getCommission_rate
                ));
        
        LOGGER.info("Reglas de comisión cargadas: " + ruleMap.size());
        
        // 3. Mapas acumuladores
        Map<Integer, Double> bankTotals = new HashMap<>();
        Map<Integer, Double> cashTotals = new HashMap<>();
        
        for (Integer id : trabajadorasMap.keySet()) {
            bankTotals.put(id, 0.0);
            cashTotals.put(id, 0.0);
        }

        // 4. Procesar VENTAS
        String sqlVentas = "SELECT "
                + "    si.employee_id, "
                + "    si.price_at_sale, "
                + "    svc.name AS service_name, "
                + "    COALESCE(svc.service_category, 'Sin Categoria') AS service_category, "
                + "    (t.nombres || ' ' || t.apellidos) as trabajadora_name, "
                + "    si.client_brought_product "
                + "FROM "
                + "    sale_items si "
                + "JOIN "
                + "    sales s ON si.sale_id = s.sale_id "
                + "JOIN "
                + "    services svc ON si.service_id = svc.service_id "
                + "JOIN "
                + "    trabajadoras t ON si.employee_id = t.id "
                + "WHERE "
                + "    DATE(s.sale_date) BETWEEN ? AND ? ";

        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sqlVentas)) {

            pstmt.setString(1, startDate.toString());
            pstmt.setString(2, endDate.toString());

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int employee_id = rs.getInt("employee_id");
                double price = rs.getDouble("price_at_sale");
                String serviceName = rs.getString("service_name");
                String serviceCategory = rs.getString("service_category");
                String trabajadoraName = rs.getString("trabajadora_name");
                boolean clientBroughtProduct = rs.getBoolean("client_brought_product");
                
                // A) Abono Manual
                if ("PAGO-MANUAL".equals(serviceCategory)) {
                    double amount = price;
                    if (trabajadoraName.equals("Rosa Maria Gutierrez") || 
                        trabajadoraName.equals("Jeimy Añez") || 
                        trabajadoraName.equals("Milagros Gutierrez")) {
                        cashTotals.merge(employee_id, amount, Double::sum);
                    } else {
                        bankTotals.merge(employee_id, amount, Double::sum);
                    }
                } 
                // B) Servicios Normales
                else {
                    double commissionForItem = calculateCommissionForItem(
                            trabajadoraName, 
                            employee_id, 
                            serviceName, 
                            serviceCategory, 
                            price, 
                            ruleMap,
                            clientBroughtProduct
                    );
                    bankTotals.merge(employee_id, commissionForItem, Double::sum);
                }
            }
        }
        
        // 5. Procesar PROPINAS
        String sqlPropinas = "SELECT "
                           + "    t.recipient_name, "
                           + "    SUM(t.amount) as total_tips "
                           + "FROM "
                           + "    tips t "
                           + "JOIN "
                           + "    sales s ON t.sale_id = s.sale_id "
                           + "WHERE "
                           + "    DATE(s.sale_date) BETWEEN ? AND ? "
                           + "GROUP BY t.recipient_name";
                           
        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sqlPropinas)) {
             
            pstmt.setString(1, startDate.toString());
            pstmt.setString(2, endDate.toString());
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String recipientName = rs.getString("recipient_name");
                double tipAmount = rs.getDouble("total_tips");
                
                if (recipientName == null) continue;

                boolean matched = false;
                for (Trabajadora t : todasLasTrabajadoras) {
                    if (t.getNombreCompleto().trim().equalsIgnoreCase(recipientName.trim())) {
                        bankTotals.merge(t.getId(), tipAmount, Double::sum);
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    LOGGER.warning("No se encontró trabajadora para la propina: " + recipientName);
                }
            }
        }

        // 6. Procesar BONOS FIJOS
        for (Trabajadora t : todasLasTrabajadoras) {
            if (t.isBonoActivo() && t.getMontoBono() > 0) {
                bankTotals.merge(t.getId(), t.getMontoBono(), Double::sum);
                LOGGER.info("Bono Fijo aplicado: $" + t.getMontoBono() + " para " + t.getNombreCompleto());
            }
        }

        // 7. Resultados Finales
        List<PayrollResult> results = new ArrayList<>();
        for (Map.Entry<Integer, Trabajadora> entry : trabajadorasMap.entrySet()) {
            int id = entry.getKey();
            Trabajadora trabajadora = entry.getValue();
            
            double totalBank = bankTotals.getOrDefault(id, 0.0);
            double totalCash = cashTotals.getOrDefault(id, 0.0);
            
            Optional<CuentaBancaria> primaryAccount = trabajadora.getCuentaPrincipal();
            results.add(new PayrollResult(trabajadora, totalBank, totalCash, primaryAccount.orElse(null)));
        }

        return results;
    }

    /**
     * Lógica de cálculo de comisiones.
     * ORDEN ACTUALIZADO: Excepciones Persona > Excepciones Servicio (Tinte) > Reglas BD.
     */
    private double calculateCommissionForItem(String tName, int tId, String sName, String sCat, double price, Map<String, Double> ruleMap, boolean clientBroughtProduct) {
        
        boolean isDepilacion = sName.equals("Cejas") || sName.equals("Bozo");

        // --- PRIORIDAD 1: Excepciones de Trabajadoras Específicas ---

        // GRUPO: Dayana, Jaqueline, Maria Virginia
        if (tName.equals("Jaqueline Añez") || 
            tName.equals("Dayana Govea") || 
            tName.equals("Maria Virginia Romero")) {

            if (tName.equals("Maria Virginia Romero") && isDepilacion) {
                return price * 0.50; 
            }
            
            // Lógica flexible para Lavado (Dayana y grupo)
            if (sCat.equals("Lavado")) {
                double comision = price * 0.40;
                if (Math.abs(price - 8.0) < 0.01) {
                    comision = 3.0;
                }
                return comision;
            }

            if (sName.equals("Hidratación Fusio-Dose")) return 8.0; 
            if (sName.equals("Extensiones (1 Paquete)")) return 10.0;
            if (sName.equals("Extensiones (2 Paquetes)")) return 20.0;
            if (sName.equals("Extensiones (3 Paquetes)")) return 15.0; 
        }

        // Reglas para Belkis Gutierrez
        else if (tName.equals("Belkis Gutierrez")) {
            if (sName.equals("Mechas")) return price * 0.36;
            if (sName.equals("Keratina")) return price * 0.70;
            if (isDepilacion) return price * 0.50;
        }

        // Reglas para Aurora Sofia Exposito
        else if (tName.equals("Aurora Sofia Exposito")) {
            if (isDepilacion) return price * 0.50;
        }
        
        // Reglas para Jeimy Añez
        else if (tName.equals("Jeimy Añez")) {
            if (sName.equals("Mechas")) return price * 0.36;
            if (sName.equals("Extensiones (1 Paquete)")) return 20.0;
            if (sName.equals("Extensiones (2 Paquetes)")) return 30.0;
            if (sName.equals("Extensiones (3 Paquetes)") || sName.equals("Extensiones (4 Paquetes)")) return 40.0;
        }
        
        // --- PRIORIDAD 2: Excepciones Globales por Servicio (MOVIDO AQUÍ) ---
        // Esto asegura que el Tinte se pague al 25% antes de mirar la categoría general de la trabajadora.
        if (sName.equals("Color (Tinte)")) {
            if (clientBroughtProduct) {
                return 12.5; 
            } else {
                return price * 0.25; // Regla del 25%
            }
        }

        // --- PRIORIDAD 3: Reglas Generales de Categoría (Base de Datos) ---
        String key = tId + "-" + sCat;
        Double rate = ruleMap.get(key);

        if (rate != null) {
            return price * rate;
        }

        // --- Fallback ---
        LOGGER.warning(String.format("No se encontró regla de comisión para: %s - %s ($%.2f). Comisión será 0.0", tName, sName, price));
        return 0.0;
    }
}
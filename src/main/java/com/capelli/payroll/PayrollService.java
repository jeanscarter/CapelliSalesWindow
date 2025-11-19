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
     * Calcula la nómina basándose en reglas de comisión de la base de datos,
     * pagos manuales y PROPINAS.
     */
    public List<PayrollResult> calculatePayroll(LocalDate startDate, LocalDate endDate) throws SQLException, IOException {
        
        // 1. Obtener todas las trabajadoras
        TrabajadoraDAO trabajadoraDAO = new TrabajadoraDAO();
        List<Trabajadora> todasLasTrabajadoras = trabajadoraDAO.getAll();
        
        // Mapa ID -> Trabajadora
        Map<Integer, Trabajadora> trabajadorasMap = todasLasTrabajadoras.stream()
                .collect(Collectors.toMap(Trabajadora::getId, t -> t));
        
        // 2. Cargar todas las reglas de comisión de la BD
        CommissionRuleDAO commissionRuleDAO = new CommissionRuleDAO();
        List<CommissionRule> allRules = commissionRuleDAO.getAll();
        
        // Convertir en un Mapa para búsqueda rápida: Key="trabajadora_id-service_category"
        Map<String, Double> ruleMap = allRules.stream()
                .collect(Collectors.toMap(
                        rule -> rule.getTrabajadora_id() + "-" + rule.getService_category(),
                        CommissionRule::getCommission_rate
                ));
        
        LOGGER.info("Reglas de comisión cargadas: " + ruleMap.size());
        
        // 3. Mapas para acumular comisiones (Banco y Efectivo)
        Map<Integer, Double> bankTotals = new HashMap<>();
        Map<Integer, Double> cashTotals = new HashMap<>();
        
        // Inicializar en 0.0 para todas
        for (Integer id : trabajadorasMap.keySet()) {
            bankTotals.put(id, 0.0);
            cashTotals.put(id, 0.0);
        }

        // 4. SQL para obtener TODOS los items de venta individuales
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

            // 5. Iterar items y aplicar lógica de comisión
            while (rs.next()) {
                int employee_id = rs.getInt("employee_id");
                double price = rs.getDouble("price_at_sale");
                String serviceName = rs.getString("service_name");
                String serviceCategory = rs.getString("service_category");
                String trabajadoraName = rs.getString("trabajadora_name");
                boolean clientBroughtProduct = rs.getBoolean("client_brought_product");
                
                // Lógica de Abono Manual
                if ("PAGO-MANUAL".equals(serviceCategory)) {
                    double amount = price; // El precio es el monto a pagar

                    // Excepción para Rosa, Jeimy y Milagros (Va a Efectivo $)
                    if (trabajadoraName.equals("Rosa Maria Gutierrez") || 
                        trabajadoraName.equals("Jeimy Añez") || 
                        trabajadoraName.equals("Milagros Gutierrez")) {
                        
                        cashTotals.merge(employee_id, amount, Double::sum);
                    } else {
                        // Resto va a Banco
                        bankTotals.merge(employee_id, amount, Double::sum);
                    }
                } else {
                    // Servicio normal -> calcular comisión
                    double commissionForItem = calculateCommissionForItem(
                            trabajadoraName, 
                            employee_id, 
                            serviceName, 
                            serviceCategory, 
                            price, 
                            ruleMap,
                            clientBroughtProduct
                    );
                    
                    // Comisiones normales -> Banco
                    bankTotals.merge(employee_id, commissionForItem, Double::sum);
                }
            }
        }
        
        // =================================================================================
        // CÁLCULO DE PROPINAS (CORREGIDO: Se agregan al BANCO)
        // =================================================================================
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
                
                // Buscar la trabajadora por nombre completo para obtener su ID
                for (Trabajadora t : todasLasTrabajadoras) {
                    if (t.getNombreCompleto().equalsIgnoreCase(recipientName)) {
                        // Encontrada: Sumar propina al total de BANCO
                        bankTotals.merge(t.getId(), tipAmount, Double::sum);
                        LOGGER.info("Propina sumada al Banco: " + tipAmount + " para " + recipientName);
                        break;
                    }
                }
            }
        }
        // =================================================================================

        // 6. Construir el resultado final
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
     * Lógica de comisión que prioriza excepciones hardcodeadas y
     * luego usa las reglas de la base de datos (pasadas en ruleMap).
     */
    private double calculateCommissionForItem(String tName, int tId, String sName, String sCat, double price, Map<String, Double> ruleMap, boolean clientBroughtProduct) {
        
        // Definición de grupos de servicios especiales
        boolean isDepilacion = sName.equals("Cejas") || sName.equals("Bozo");

        // --- 1. Reglas de Excepción (Prioridad Alta) ---

        // Lógica de "Color (Tinte)" (Aplica a TODOS)
        if (sName.equals("Color (Tinte)")) {
            if (clientBroughtProduct) {
                return 12.5; 
            } else {
                return price * 0.25;
            }
        }

        // Reglas para Jaqueline Añez, Dayana Govea, Maria Virginia Romero
        if (tName.equals("Jaqueline Añez") || 
                 tName.equals("Dayana Govea") || 
                 tName.equals("Maria Virginia Romero")) {

            if (tName.equals("Maria Virginia Romero")) {
                if (isDepilacion) {
                    return price * 0.50; 
                }
            }
            
            if (sCat.equals("Lavado")) {
                if (Math.abs(price - 10.0) < 0.01) return 4.0;
                if (Math.abs(price - 8.0) < 0.01) return 3.0;
                if (Math.abs(price - 12.0) < 0.01) return 4.8;
                if (Math.abs(price - 15.0) < 0.01) return 6.0;
            }

            if (sName.equals("Hidratación Fusio-Dose")) {
                return 8.0; 
            }

            if (sName.equals("Extensiones (1 Paquete)")) return 10.0;
            if (sName.equals("Extensiones (2 Paquetes)")) return 20.0;
            if (sName.equals("Extensiones (3 Paquetes)")) return 15.0; 
        }

        // Reglas para Belkis Gutierrez
        else if (tName.equals("Belkis Gutierrez")) {
            if (sName.equals("Mechas")) {
                return price * 0.36; // 36%
            }
            if (sName.equals("Keratina")) {
                return price * 0.70; 
            }
            if (isDepilacion) {
                return price * 0.50; 
            }
        }

        // Reglas para Aurora Sofia Exposito
        else if (tName.equals("Aurora Sofia Exposito")) {
            if (isDepilacion) {
                return price * 0.50; 
            }
        }
        
        // Reglas para Jeimy Añez
        else if (tName.equals("Jeimy Añez")) {
            if (sName.equals("Mechas")) {
                return price * 0.36; 
            }
            if (sName.equals("Extensiones (1 Paquete)")) return 20.0;
            if (sName.equals("Extensiones (2 Paquetes)")) return 30.0;
            if (sName.equals("Extensiones (3 Paquetes)") || sName.equals("Extensiones (4 Paquetes)")) {
                return 40.0; 
            }
        }
        
        // --- 2. Reglas Generales de Categoría (Usando el Mapa de BD) ---
        String key = tId + "-" + sCat;
        Double rate = ruleMap.get(key);

        if (rate != null) {
            return price * rate;
        }

        // --- 3. Fallback ---
        LOGGER.warning(String.format("No se encontró regla de comisión ni excepción para: [Trabajadora: %s, Servicio: %s, Categoría: %s]. Comisión será 0.0",
                tName, sName, sCat));
        return 0.0;
    }
}
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
     * Calcula la nómina basándose en reglas de comisión de la base de datos
     * y aplicando excepciones hardcodeadas para casos especiales.
     */
    public List<PayrollResult> calculatePayroll(LocalDate startDate, LocalDate endDate) throws SQLException, IOException {
        
        // 1. Obtener todas las trabajadoras
        TrabajadoraDAO trabajadoraDAO = new TrabajadoraDAO();
        Map<Integer, Trabajadora> trabajadorasMap = trabajadoraDAO.getAll().stream()
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
        for (Integer id : trabajadorasMap.keySet()) {
            bankTotals.put(id, 0.0);
            cashTotals.put(id, 0.0);
        }

        // 4. SQL para obtener TODOS los items de venta individuales
        String sql = "SELECT "
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
                + "    DATE(s.sale_date, 'localtime') BETWEEN ? AND ? ";

        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startDate.toString());
            pstmt.setString(2, endDate.toString());

            ResultSet rs = pstmt.executeQuery();

            // 5. Iterar en Java y aplicar lógica de comisión
            while (rs.next()) {
                int employee_id = rs.getInt("employee_id");
                double price = rs.getDouble("price_at_sale");
                String serviceName = rs.getString("service_name");
                String serviceCategory = rs.getString("service_category");
                String trabajadoraName = rs.getString("trabajadora_name");
                boolean clientBroughtProduct = rs.getBoolean("client_brought_product");
                
                // ===== INICIO DE MODIFICACIÓN: Lógica de Abono Manual =====
                
                // Si es "Abono Manual Staff" (Categoría PAGO-MANUAL)
                if ("PAGO-MANUAL".equals(serviceCategory)) {
                    double amount = price; // El precio es el monto a pagar

                    // Excepción para Rosa, Jeimy y Milagros
                    if (trabajadoraName.equals("Rosa Maria Gutierrez") || 
                        trabajadoraName.equals("Jeimy Añez") || 
                        trabajadoraName.equals("Milagros Gutierrez")) {
                        
                        // Va a la nómina de Efectivo $
                        cashTotals.merge(employee_id, amount, Double::sum);
                    } else {
                        // Va a la nómina normal (Banco)
                        bankTotals.merge(employee_id, amount, Double::sum);
                    }
                } else {
                    // Si es un servicio normal, calcular comisión
                    double commissionForItem = calculateCommissionForItem(
                            trabajadoraName, 
                            employee_id, 
                            serviceName, 
                            serviceCategory, 
                            price, 
                            ruleMap,
                            clientBroughtProduct
                    );
                    
                    // Las comisiones normales siempre van al banco
                    bankTotals.merge(employee_id, commissionForItem, Double::sum);
                }
                // ===== FIN DE MODIFICACIÓN =====
            }
        }

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
     * (Se eliminó la lógica de PAGO-MANUAL de aquí, ya que se maneja arriba)
     */
    private double calculateCommissionForItem(String tName, int tId, String sName, String sCat, double price, Map<String, Double> ruleMap, boolean clientBroughtProduct) {
        
        // Definición de grupos de servicios especiales
        boolean isDepilacion = sName.equals("Cejas") || sName.equals("Bozo");

        // --- 1. Reglas de Excepción (Prioridad Alta) ---

        // --- NUEVA REGLA: Lógica de "Color (Tinte)" (Aplica a TODOS) ---
        if (sName.equals("Color (Tinte)")) {
            if (clientBroughtProduct) {
                // Scenario 2: Cliente trae producto. Comisión es 50% de $25 (fijo $12.5)
                return 12.5; 
            } else {
                // Scenario 1: Salón pone producto.
                // 50% producto, 50% del restante (50%) para la trabajadora
                // (price * 0.50) * 0.50 = price * 0.25
                return price * 0.25;
            }
        }
        // --- FIN NUEVA REGLA ---

        // --- Reglas para Jaqueline Añez, Dayana Govea, Maria Virginia Romero ---
        if (tName.equals("Jaqueline Añez") || 
                 tName.equals("Dayana Govea") || 
                 tName.equals("Maria Virginia Romero")) {

            // Excepción Depilación (Solo Maria Virginia)
            if (tName.equals("Maria Virginia Romero")) {
                if (isDepilacion) {
                    return price * 0.50; // 50%
                }
            }
            
            // Excepción Lavado (Montos fijos)
            if (sCat.equals("Lavado")) {
                if (Math.abs(price - 10.0) < 0.01) return 4.0;
                if (Math.abs(price - 8.0) < 0.01) return 3.0;
                if (Math.abs(price - 12.0) < 0.01) return 4.8;
                if (Math.abs(price - 15.0) < 0.01) return 6.0;
                // Si no es un precio fijo, usará la regla de BD (si existe)
            }

            // Excepción Hidratación (Monto fijo)
            if (sName.equals("Hidratación Fusio-Dose")) {
                return 8.0; // $8 monto fijo
            }

            // Excepción Extensiones (Montos fijos)
            if (sName.equals("Extensiones (1 Paquete)")) return 10.0;
            if (sName.equals("Extensiones (2 Paquetes)")) return 20.0;
            if (sName.equals("Extensiones (3 Paquetes)")) return 15.0; 
        }

        // --- Reglas para Belkis Gutierrez ---
        else if (tName.equals("Belkis Gutierrez")) {
            
            // NUEVA REGLA PARA MECHAS (Belkis)
            if (sName.equals("Mechas")) {
                // 40% para producto, 60% para la trabajadora DE la base comisionable (que es el 60% del precio)
                // (price * 0.60) * 0.60 = price * 0.36
                return price * 0.36; // 36%
            }
            
            if (sName.equals("Keratina")) {
                return price * 0.70; // 70% (Excepción sobre Químicos)
            }
            if (isDepilacion) {
                return price * 0.50; // 50% (Excepción sobre Peluqueria)
            }
        }

        // --- Reglas para Aurora Sofia Exposito ("Sofia") ---
        else if (tName.equals("Aurora Sofia Exposito")) {
            if (isDepilacion) {
                return price * 0.50; // 50% (Excepción sobre Peluqueria)
            }
        }
        
        // --- Reglas para Jeimy Añez ---
        else if (tName.equals("Jeimy Añez")) {
            
            // NUEVA REGLA PARA MECHAS (Jeimy)
            if (sName.equals("Mechas")) {
                // 40% para producto, 60% para la trabajadora DE la base comisionable (que es el 60% del precio)
                // (price * 0.60) * 0.60 = price * 0.36
                return price * 0.36; // 36%
            }
            
            // Excepción Extensiones (Montos fijos)
            if (sName.equals("Extensiones (1 Paquete)")) return 20.0;
            if (sName.equals("Extensiones (2 Paquetes)")) return 30.0;
            if (sName.equals("Extensiones (3 Paquetes)") || sName.equals("Extensiones (4 Paquetes)")) {
                return 40.0; // "3 paquetes o más"
            }
        }
        
        // --- FIN DE EXCEPCIONES ---

        
        // --- 2. Reglas Generales de Categoría (Usando el Mapa de BD) ---
        // Si ninguna excepción se aplicó, buscar la regla de categoría.
        
        // CORRECCIÓN: Usar SIEMPRE la categoría del servicio (sCat)
        String key = tId + "-" + sCat;
        Double rate = ruleMap.get(key);

        if (rate != null) {
            // Se encontró una regla de BD (ej: Peluqueria -> 0.50)
            return price * rate;
        }

        // --- 3. Fallback ---
        // Si no se define ninguna regla (ni excepción ni categoría), la comisión es 0
        LOGGER.warning(String.format("No se encontró regla de comisión ni excepción para: [Trabajadora: %s, Servicio: %s, Categoría: %s]. Comisión será 0.0",
                tName, sName, sCat));
        return 0.0;
    }
}
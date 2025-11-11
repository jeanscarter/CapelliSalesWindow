package com.capelli.database;

import com.capelli.model.CommissionRule;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CommissionRuleDAO {

    public List<CommissionRule> getAll() throws SQLException {
        List<CommissionRule> rules = new ArrayList<>();
        String sql = "SELECT cr.rule_id, cr.trabajadora_id, cr.service_category, cr.commission_rate, "
                   + "(t.nombres || ' ' || t.apellidos) as trabajadora_name "
                   + "FROM trabajadora_commission_rules cr "
                   + "JOIN trabajadoras t ON cr.trabajadora_id = t.id "
                   + "ORDER BY trabajadora_name, cr.service_category";

        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                CommissionRule rule = new CommissionRule();
                rule.setRule_id(rs.getInt("rule_id"));
                rule.setTrabajadora_id(rs.getInt("trabajadora_id"));
                rule.setService_category(rs.getString("service_category"));
                rule.setCommission_rate(rs.getDouble("commission_rate"));
                rule.setTrabajadora_name(rs.getString("trabajadora_name"));
                rules.add(rule);
            }
        }
        return rules;
    }

    public void save(CommissionRule rule) throws SQLException {
        String sql;
        if (rule.getRule_id() == 0) {
            // Insertar nueva
            sql = "INSERT INTO trabajadora_commission_rules(trabajadora_id, service_category, commission_rate) VALUES(?, ?, ?)";
        } else {
            // Actualizar existente
            sql = "UPDATE trabajadora_commission_rules SET trabajadora_id = ?, service_category = ?, commission_rate = ? WHERE rule_id = ?";
        }

        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, rule.getTrabajadora_id());
            pstmt.setString(2, rule.getService_category());
            pstmt.setDouble(3, rule.getCommission_rate());
            if (rule.getRule_id() != 0) {
                pstmt.setInt(4, rule.getRule_id());
            }
            pstmt.executeUpdate();
        }
    }

    public void delete(int rule_id) throws SQLException {
        String sql = "DELETE FROM trabajadora_commission_rules WHERE rule_id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rule_id);
            pstmt.executeUpdate();
        }
    }
}
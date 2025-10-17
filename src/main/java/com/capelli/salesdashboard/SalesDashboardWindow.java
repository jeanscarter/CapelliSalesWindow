package com.capelli.salesdashboard;

import com.capelli.database.Database;
import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.BorderLayout;
import java.awt.Font;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

public class SalesDashboardWindow extends JFrame {

    private DefaultTableModel salesTableModel;
    private JTable salesTable;
    private JLabel topSellerLabel;

    public SalesDashboardWindow() {
        setTitle("Dashboard de Ventas - Capelli");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Importante para no cerrar la app principal
        setSize(1200, 800);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();

        loadSalesData();
        loadTopSeller();
    }

    private void initComponents() {
        String[] columnNames = {"ID Venta", "Fecha", "Cliente", "Servicio", "Trabajadora", "Precio", "Descuento", "Total Venta"};
        salesTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Hacer la tabla de solo lectura
            }
        };
        salesTable = new JTable(salesTableModel);
        salesTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        salesTable.setRowHeight(25);
        topSellerLabel = new JLabel("Cargando...", SwingConstants.CENTER);
        topSellerLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 15", "[grow]", "[][grow]"));

        JPanel topSellerPanel = new JPanel(new BorderLayout());
        topSellerPanel.setBorder(BorderFactory.createTitledBorder("⭐ Trabajadora con Más Servicios Realizados"));
        topSellerPanel.add(topSellerLabel, BorderLayout.CENTER);

        mainPanel.add(topSellerPanel, "north, h 80!");
        
        JPanel salesPanel = new JPanel(new BorderLayout());
        salesPanel.setBorder(BorderFactory.createTitledBorder("Historial de Ventas"));
        salesPanel.add(new JScrollPane(salesTable), BorderLayout.CENTER);

        mainPanel.add(salesPanel, "grow"); 

        add(mainPanel);
    }

    private void loadSalesData() {
        salesTableModel.setRowCount(0);
        
        String sql = "SELECT " +
                     "s.sale_id, " +
                     "strftime('%d/%m/%Y %H:%M', s.sale_date) as sale_date, " +
                     "COALESCE(c.full_name, 'Cliente Genérico') as client_name, " +
                     "ser.name as service_name, " +
                     "e.full_name as employee_name, " +
                     "si.price_at_sale, " +
                     "s.discount_amount, " +
                     "s.total " +
                     "FROM sales s " +
                     "JOIN sale_items si ON s.sale_id = si.sale_id " +
                     "JOIN services ser ON si.service_id = ser.service_id " +
                     "JOIN employees e ON si.employee_id = e.employee_id " +
                     "LEFT JOIN clients c ON s.client_id = c.client_id " +
                     "ORDER BY s.sale_id DESC";

        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                salesTableModel.addRow(new Object[]{
                    rs.getInt("sale_id"),
                    rs.getString("sale_date"),
                    rs.getString("client_name"),
                    rs.getString("service_name"),
                    rs.getString("employee_name"),
                    String.format("%.2f", rs.getDouble("price_at_sale")),
                    String.format("%.2f", rs.getDouble("discount_amount")),
                    String.format("%.2f", rs.getDouble("total"))
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar el historial de ventas: " + e.getMessage(), "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void loadTopSeller() {
        String sql = "SELECT e.full_name, COUNT(si.sale_item_id) as services_count " +
                     "FROM employees e " +
                     "JOIN sale_items si ON e.employee_id = si.employee_id " +
                     "GROUP BY e.full_name " +
                     "ORDER BY services_count DESC " +
                     "LIMIT 1";

        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                String topSellerName = rs.getString("full_name");
                int serviceCount = rs.getInt("services_count");
                topSellerLabel.setText(String.format("%s (%d servicios)", topSellerName, serviceCount));
            } else {
                topSellerLabel.setText("Aún no hay datos de ventas.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al calcular la trabajadora del mes: " + e.getMessage(), "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
        
        SwingUtilities.invokeLater(() -> {
            new SalesDashboardWindow().setVisible(true);
        });
    }
}
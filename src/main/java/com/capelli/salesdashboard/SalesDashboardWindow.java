package com.capelli.salesdashboard;

import com.capelli.database.Database;
import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.BorderLayout;
import java.awt.Font;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
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
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Ventana de Dashboard para visualizar ventas y estadísticas.
 * Muestra historial de ventas y la trabajadora con más servicios realizados.
 */
public class SalesDashboardWindow extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(SalesDashboardWindow.class.getName());
    
    private DefaultTableModel salesTableModel;
    private JTable salesTable;
    private JLabel topSellerLabel;
    private final DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");

    public SalesDashboardWindow() {
        setTitle("Dashboard de Ventas - Capelli");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();

        loadSalesData();
        loadTopSeller();
    }

    private void initComponents() {
        String[] columnNames = {
            "ID Venta", 
            "Fecha", 
            "Cliente", 
            "Servicio", 
            "Trabajadora", 
            "Precio", 
            "Descuento", 
            "Total Venta"
        };
        
        salesTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                // Columnas 0, 5, 6, 7 son numéricas
                if (columnIndex == 0) return Integer.class;
                if (columnIndex >= 5 && columnIndex <= 7) return String.class; // Formateadas como String
                return String.class;
            }
        };
        
        salesTable = new JTable(salesTableModel);
        salesTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        salesTable.setRowHeight(25);
        salesTable.setAutoCreateRowSorter(true); // Permite ordenar columnas
        
        topSellerLabel = new JLabel("Cargando estadísticas...", SwingConstants.CENTER);
        topSellerLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 15", "[grow]", "[][grow]"));

        // Panel de trabajadora destacada
        JPanel topSellerPanel = new JPanel(new BorderLayout());
        topSellerPanel.setBorder(BorderFactory.createTitledBorder("⭐ Trabajadora con Más Servicios Realizados"));
        topSellerPanel.add(topSellerLabel, BorderLayout.CENTER);

        mainPanel.add(topSellerPanel, "north, h 80!");
        
        // Panel de historial de ventas
        JPanel salesPanel = new JPanel(new BorderLayout());
        salesPanel.setBorder(BorderFactory.createTitledBorder("Historial de Ventas"));
        salesPanel.add(new JScrollPane(salesTable), BorderLayout.CENTER);

        mainPanel.add(salesPanel, "grow");

        add(mainPanel);
    }

    /**
     * Carga los datos de ventas desde la base de datos.
     * CORREGIDO: Ahora usa la tabla 'trabajadoras' en lugar de 'employees'.
     */
    private void loadSalesData() {
        LOGGER.info("Cargando datos de ventas...");
        salesTableModel.setRowCount(0);
        
        String sql = "SELECT " +
                     "    s.sale_id, " +
                     "    strftime('%d/%m/%Y %H:%M', s.sale_date, 'localtime') as sale_date, " +
                     "    COALESCE(c.full_name, 'Cliente Genérico') as client_name, " +
                     "    COALESCE(ser.name, 'SERVICIO BORRADO') as service_name, " + 
                     "    si.client_brought_product, " +
                     "    COALESCE((t.nombres || ' ' || t.apellidos), 'TRABAJADORA BORRADA') as employee_name, " + 
                     "    si.price_at_sale, " +
                     "    s.discount_amount, " +
                     "    s.total " +
                     "FROM sales s " +
                     "LEFT JOIN sale_items si ON s.sale_id = si.sale_id " + 
                     "LEFT JOIN services ser ON si.service_id = ser.service_id " + 
                     "LEFT JOIN trabajadoras t ON si.employee_id = t.id " + 
                     "LEFT JOIN clients c ON s.client_id = c.client_id " +
                     "ORDER BY s.sale_date DESC, s.sale_id DESC " +
                     "LIMIT 1000";

        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int rowCount = 0;
            while (rs.next()) {
                
                String serviceName = rs.getString("service_name");
                boolean broughtProduct = rs.getBoolean("client_brought_product");
                if (broughtProduct) {
                    serviceName += " (Cliente)";
                }
                
                salesTableModel.addRow(new Object[]{
                    rs.getInt("sale_id"),
                    rs.getString("sale_date"),
                    rs.getString("client_name"),
                    serviceName,
                    rs.getString("employee_name"),
                    currencyFormat.format(rs.getDouble("price_at_sale")),
                    currencyFormat.format(rs.getDouble("discount_amount")),
                    currencyFormat.format(rs.getDouble("total"))
                });
                rowCount++;
            }
            
            LOGGER.info("Cargadas " + rowCount + " ventas exitosamente");
            
            if (rowCount == 0) {
                LOGGER.warning("No se encontraron ventas en la base de datos");
                JOptionPane.showMessageDialog(this,
                    "No hay ventas registradas aún.",
                    "Sin Datos",
                    JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar el historial de ventas", e);
            JOptionPane.showMessageDialog(this,
                "Error al cargar el historial de ventas:\n" + e.getMessage(),
                "Error de Base de Datos",
                JOptionPane.ERROR_MESSAGE);
            topSellerLabel.setText("Error al cargar datos");
        }
    }

    /**
     * Carga la trabajadora con más servicios realizados.
     * CORREGIDO: Ahora usa la tabla 'trabajadoras' en lugar de 'employees'.
     */
    private void loadTopSeller() {
        LOGGER.info("Calculando trabajadora con más servicios...");
        
       String sql = "SELECT " +
                     "    (t.nombres || ' ' || t.apellidos) as full_name, " + 
                     "    COUNT(si.sale_item_id) as services_count " +
                     "FROM trabajadoras t " + 
                     "LEFT JOIN sale_items si ON t.id = si.employee_id " + 
                     "GROUP BY t.id, t.nombres, t.apellidos " + 
                     "ORDER BY services_count DESC " +
                     "LIMIT 1";

        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                String topSellerName = rs.getString("full_name");
                int serviceCount = rs.getInt("services_count");
                
                topSellerLabel.setText(String.format("🏆 %s (%d servicios)", 
                    topSellerName, serviceCount));
                
                LOGGER.info("Trabajadora destacada: " + topSellerName + 
                           " con " + serviceCount + " servicios");
            } else {
                topSellerLabel.setText("Aún no hay datos de ventas.");
                LOGGER.info("No hay datos suficientes para calcular trabajadora destacada");
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al calcular la trabajadora destacada", e);
            topSellerLabel.setText("Error al cargar estadísticas");
            JOptionPane.showMessageDialog(this,
                "Error al calcular la trabajadora del mes:\n" + e.getMessage(),
                "Error de Base de Datos",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Refresca todos los datos del dashboard.
     * Útil para actualizar la vista después de nuevas ventas.
     */
    public void refreshDashboard() {
        LOGGER.info("Refrescando dashboard...");
        loadSalesData();
        loadTopSeller();
    }

    // Método main para testing independiente
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to initialize LaF", ex);
        }
        
        SwingUtilities.invokeLater(() -> {
            Database.initialize();
            SalesDashboardWindow window = new SalesDashboardWindow();
            window.setVisible(true);
        });
    }
}
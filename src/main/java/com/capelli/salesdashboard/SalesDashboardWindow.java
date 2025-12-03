package com.capelli.salesdashboard;

import com.capelli.capellisaleswindow.CapelliSalesWindow;
import com.capelli.database.Database;
import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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

/**
 * Ventana de Dashboard para visualizar ventas y estadísticas.
 * Muestra historial de ventas y la trabajadora con más servicios realizados.
 */
public class SalesDashboardWindow extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(SalesDashboardWindow.class.getName());
    
    private DefaultTableModel salesTableModel;
    private JTable salesTable;
    private JLabel topSellerLabel;
    private JButton editSaleButton; 
    private JButton viewDetailsButton; // BOTÓN NUEVO
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
                // Columnas 0, 5, 6, 7 son numéricas o de moneda
                if (columnIndex == 0) return Object.class; 
                if (columnIndex >= 5 && columnIndex <= 7) return String.class;
                return String.class;
            }
        };
        
        salesTable = new JTable(salesTableModel);
        salesTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        salesTable.setRowHeight(25);
        salesTable.setAutoCreateRowSorter(true); 
        
        // Listener para doble clic -> Ver Detalles
        salesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    verDetallesVenta();
                }
            }
        });
        
        topSellerLabel = new JLabel("Cargando estadísticas...", SwingConstants.CENTER);
        topSellerLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));

        // Inicializar botones
        editSaleButton = new JButton("Editar Venta Seleccionada");
        editSaleButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        editSaleButton.addActionListener(e -> editarVentaSeleccionada());
        
        viewDetailsButton = new JButton("Ver Detalles de Factura");
        viewDetailsButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        viewDetailsButton.addActionListener(e -> verDetallesVenta());
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 15", "[grow]", "[][grow][]"));

        // Panel de trabajadora destacada
        JPanel topSellerPanel = new JPanel(new BorderLayout());
        topSellerPanel.setBorder(BorderFactory.createTitledBorder("⭐ Trabajadora con Más Servicios Realizados"));
        topSellerPanel.add(topSellerLabel, BorderLayout.CENTER);

        mainPanel.add(topSellerPanel, "north, h 80!");
        
        // Panel de historial de ventas
        JPanel salesPanel = new JPanel(new BorderLayout());
        salesPanel.setBorder(BorderFactory.createTitledBorder("Historial de Ventas (Doble clic para ver detalles)"));
        salesPanel.add(new JScrollPane(salesTable), BorderLayout.CENTER);

        mainPanel.add(salesPanel, "grow, wrap"); 

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(viewDetailsButton); // Añadido
        buttonPanel.add(editSaleButton);
        
        mainPanel.add(buttonPanel, "growx");

        add(mainPanel);
    }

    private void loadSalesData() {
        LOGGER.info("Cargando datos de ventas...");
        salesTableModel.setRowCount(0);
        
        String sql = "SELECT " +
                     "    s.sale_id, " +
                     "    s.correlative_number, " + 
                     "    s.sale_date, " +          
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
             
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat uiFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            int rowCount = 0;
            while (rs.next()) {
                
                String serviceName = rs.getString("service_name");
                boolean broughtProduct = rs.getBoolean("client_brought_product");
                if (broughtProduct) {
                    serviceName += " (Cliente)";
                }
                
                String correlativoStr = rs.getString("correlative_number");
                Object idVentaMostrar;
                if (correlativoStr != null && !correlativoStr.isEmpty()) {
                    try {
                        idVentaMostrar = Integer.parseInt(correlativoStr);
                    } catch (NumberFormatException e) {
                        idVentaMostrar = correlativoStr;
                    }
                } else {
                    idVentaMostrar = rs.getInt("sale_id");
                }
                
                String rawDate = rs.getString("sale_date");
                String formattedDate = rawDate;
                try {
                    if (rawDate != null) {
                        Date date = dbFormat.parse(rawDate);
                        formattedDate = uiFormat.format(date);
                    }
                } catch (Exception e) {
                    LOGGER.warning("No se pudo parsear la fecha: " + rawDate);
                }
                
                salesTableModel.addRow(new Object[]{
                    idVentaMostrar,
                    formattedDate,
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
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar el historial de ventas", e);
            JOptionPane.showMessageDialog(this, "Error al cargar datos: " + e.getMessage());
        }
    }

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
                topSellerLabel.setText(String.format("🏆 %s (%d servicios)", topSellerName, serviceCount));
            } else {
                topSellerLabel.setText("Aún no hay datos de ventas.");
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al calcular la trabajadora destacada", e);
            topSellerLabel.setText("Error al cargar estadísticas");
        }
    }
    
    private void editarVentaSeleccionada() {
        int selectedRow = salesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona una venta para editar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Object idObj = salesTableModel.getValueAt(salesTable.convertRowIndexToModel(selectedRow), 0);
        String idStr = String.valueOf(idObj);
        long saleId = getSaleIdFromCorrelative(idStr);
        
        if (saleId == -1) {
             JOptionPane.showMessageDialog(this, "No se pudo identificar el ID interno de la venta.", "Error", JOptionPane.ERROR_MESSAGE);
             return;
        }

        try {
            CapelliSalesWindow editWindow = new CapelliSalesWindow(saleId);
            editWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            editWindow.setVisible(true);
            editWindow.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    refreshDashboard();
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir ventana de edición", e);
        }
    }
    
    private void verDetallesVenta() {
        int selectedRow = salesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona una venta para ver detalles.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Object idObj = salesTableModel.getValueAt(salesTable.convertRowIndexToModel(selectedRow), 0);
        String idStr = String.valueOf(idObj);
        long saleId = getSaleIdFromCorrelative(idStr);
        
        if (saleId == -1) {
             JOptionPane.showMessageDialog(this, "No se pudo identificar el ID interno de la venta.", "Error", JOptionPane.ERROR_MESSAGE);
             return;
        }

        // Abrir la ventana de detalle
        try {
            InvoiceDetailWindow detailWindow = new InvoiceDetailWindow(saleId);
            detailWindow.setVisible(true);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir ventana de detalles", e);
            JOptionPane.showMessageDialog(this, "Error al abrir detalles: " + e.getMessage());
        }
    }

    private long getSaleIdFromCorrelative(String correlativeOrId) {
        String sql = "SELECT sale_id FROM sales WHERE correlative_number = ? OR sale_id = ? LIMIT 1";
        try (Connection conn = Database.connect(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, correlativeOrId);
            pstmt.setString(2, correlativeOrId); 
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getLong("sale_id");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error buscando sale_id para: " + correlativeOrId, e);
        }
        return -1;
    }

    public void refreshDashboard() {
        LOGGER.info("Refrescando dashboard...");
        loadSalesData();
        loadTopSeller();
    }

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
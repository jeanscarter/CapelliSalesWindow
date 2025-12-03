package com.capelli.salesdashboard;

import com.capelli.database.CommissionRuleDAO;
import com.capelli.database.Database;
import com.capelli.model.CommissionRule;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Ventana de Detalle de Factura.
 * Muestra servicios, comisiones calculadas, ganancia empresa, IVA y propinas.
 */
public class InvoiceDetailWindow extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(InvoiceDetailWindow.class.getName());
    private final long saleId;
    private final DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");
    private final DecimalFormat percentFormat = new DecimalFormat("#,##0.0%");

    private JLabel lblInvoiceNumber, lblDate, lblClient;
    private JTable servicesTable;
    private DefaultTableModel servicesModel;
    
    // Footer Labels
    private JLabel lblSubtotal, lblDiscount, lblIva, lblTotal, lblTipsTotal;
    
    // Profit Category Labels
    private JLabel lblProfitLavado, lblProfitFusio, lblProfitQuimicos, lblProfitProductos, lblProfitTotal;

    private Map<String, Double> ruleMap = new HashMap<>();

    public InvoiceDetailWindow(long saleId) {
        this.saleId = saleId;
        setTitle("Detalle de Factura #" + saleId);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        loadCommissionRules();
        initComponents();
        loadData();
    }

    private void loadCommissionRules() {
        try {
            CommissionRuleDAO dao = new CommissionRuleDAO();
            List<CommissionRule> rules = dao.getAll();
            // Mapa: Key="trabajadora_id-service_category"
            ruleMap = rules.stream()
                .collect(Collectors.toMap(
                        rule -> rule.getTrabajadora_id() + "-" + rule.getService_category(),
                        CommissionRule::getCommission_rate
                ));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error cargando reglas de comisión", e);
        }
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- HEADER ---
        JPanel headerPanel = new JPanel(new MigLayout("fillx", "[][grow][][grow]", "[]"));
        headerPanel.setBorder(BorderFactory.createTitledBorder("Información General"));
        
        lblInvoiceNumber = new JLabel();
        lblInvoiceNumber.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblInvoiceNumber.setForeground(Color.BLUE);
        
        lblDate = new JLabel();
        lblClient = new JLabel();
        
        headerPanel.add(new JLabel("Factura:"));
        headerPanel.add(lblInvoiceNumber);
        headerPanel.add(new JLabel("Fecha:"));
        headerPanel.add(lblDate, "wrap");
        headerPanel.add(new JLabel("Cliente:"));
        headerPanel.add(lblClient, "span 3");

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // --- CENTER (Services Table) ---
        String[] columns = {
            "Servicio", "Categoría", "Trabajadora", "Precio ($)", 
            "Comisión ($)", "% Est.", "Ganancia Empresa ($)"
        };
        
        servicesModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        servicesTable = new JTable(servicesModel);
        servicesTable.setRowHeight(25);
        servicesTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        mainPanel.add(new JScrollPane(servicesTable), BorderLayout.CENTER);

        // --- BOTTOM PANEL ---
        JPanel bottomPanel = new JPanel(new MigLayout("fillx", "[grow][grow]", "[top]"));
        
        // Left: Profit Analysis & Tips
        // CORREGIDO: Eliminado 'bold' que causaba el crash
        JPanel profitPanel = new JPanel(new MigLayout("wrap 2", "[right][left]")); 
        profitPanel.setBorder(BorderFactory.createTitledBorder("Ganancia Empresa por Categoría"));
        
        lblProfitLavado = new JLabel("$ 0.00"); lblProfitLavado.setForeground(new Color(0, 102, 204));
        lblProfitFusio = new JLabel("$ 0.00"); lblProfitFusio.setForeground(new Color(0, 153, 51));
        lblProfitQuimicos = new JLabel("$ 0.00"); lblProfitQuimicos.setForeground(new Color(204, 0, 0));
        lblProfitProductos = new JLabel("$ 0.00"); lblProfitProductos.setForeground(new Color(153, 0, 153));
        lblProfitTotal = new JLabel("$ 0.00");
        
        // Set Bold Font manually since MigLayout constraint caused issues
        Font boldFont = new Font("Segoe UI", Font.BOLD, 12);
        lblProfitLavado.setFont(boldFont);
        lblProfitFusio.setFont(boldFont);
        lblProfitQuimicos.setFont(boldFont);
        lblProfitProductos.setFont(boldFont);
        lblProfitTotal.setFont(boldFont);
        
        profitPanel.add(new JLabel("Lavado:")); profitPanel.add(lblProfitLavado);
        profitPanel.add(new JLabel("Fusio-Dose:")); profitPanel.add(lblProfitFusio);
        profitPanel.add(new JLabel("Químicos:")); profitPanel.add(lblProfitQuimicos);
        profitPanel.add(new JLabel("Productos/Otros:")); profitPanel.add(lblProfitProductos);
        profitPanel.add(new JLabel("----------------")); profitPanel.add(new JLabel("-------"));
        profitPanel.add(new JLabel("Total Operativo:")); profitPanel.add(lblProfitTotal);

        JPanel tipsPanel = new JPanel(new MigLayout("wrap 2"));
        tipsPanel.setBorder(BorderFactory.createTitledBorder("Propinas"));
        lblTipsTotal = new JLabel("Ninguna");
        tipsPanel.add(lblTipsTotal, "span 2");

        JPanel leftContainer = new JPanel(new BorderLayout());
        leftContainer.add(profitPanel, BorderLayout.CENTER);
        leftContainer.add(tipsPanel, BorderLayout.SOUTH);

        // Right: Invoice Totals
        // CORREGIDO: Eliminado 'bold' que causaba el crash
        JPanel totalsPanel = new JPanel(new MigLayout("wrap 2", "[right][right]")); 
        totalsPanel.setBorder(BorderFactory.createTitledBorder("Totales Factura"));
        
        lblSubtotal = new JLabel("$ 0.00");
        lblDiscount = new JLabel("$ 0.00");
        lblIva = new JLabel("$ 0.00");
        lblTotal = new JLabel("$ 0.00");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        // Apply bold to value labels
        lblSubtotal.setFont(boldFont);
        lblDiscount.setFont(boldFont);
        lblIva.setFont(boldFont);

        totalsPanel.add(new JLabel("Subtotal:")); totalsPanel.add(lblSubtotal);
        totalsPanel.add(new JLabel("Descuento:")); totalsPanel.add(lblDiscount);
        totalsPanel.add(new JLabel("IVA:")); totalsPanel.add(lblIva);
        totalsPanel.add(new JLabel("TOTAL A PAGAR:")); totalsPanel.add(lblTotal);

        bottomPanel.add(leftContainer, "growy, top");
        bottomPanel.add(totalsPanel, "growy, top, align right");

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        add(mainPanel);
    }

    private void loadData() {
        String sqlHeader = "SELECT s.*, COALESCE(c.full_name, 'Cliente Genérico') as client_name " +
                           "FROM sales s LEFT JOIN clients c ON s.client_id = c.client_id " +
                           "WHERE s.sale_id = ?";
        
        String sqlItems = "SELECT si.*, s.name as service_name, s.service_category, " +
                          "t.nombres, t.apellidos, t.id as worker_id " +
                          "FROM sale_items si " +
                          "JOIN services s ON si.service_id = s.service_id " +
                          "JOIN trabajadoras t ON si.employee_id = t.id " +
                          "WHERE si.sale_id = ?";
        
        String sqlTips = "SELECT recipient_name, amount FROM tips WHERE sale_id = ?";

        try (Connection conn = Database.connect()) {
            
            // 1. Load Header
            try (PreparedStatement pstmt = conn.prepareStatement(sqlHeader)) {
                pstmt.setLong(1, saleId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String corr = rs.getString("correlative_number");
                    lblInvoiceNumber.setText((corr != null ? corr : String.valueOf(saleId)));
                    lblDate.setText(rs.getString("sale_date"));
                    lblClient.setText(rs.getString("client_name"));
                    
                    lblSubtotal.setText(currencyFormat.format(rs.getDouble("subtotal")));
                    lblDiscount.setText(currencyFormat.format(rs.getDouble("discount_amount")));
                    lblIva.setText(currencyFormat.format(rs.getDouble("vat_amount")));
                    lblTotal.setText(currencyFormat.format(rs.getDouble("total")));
                }
            }

            // 2. Load Items & Calculate Logic
            double pLavado = 0, pFusio = 0, pQuimicos = 0, pProductos = 0;
            double totalOperatingProfit = 0;

            try (PreparedStatement pstmt = conn.prepareStatement(sqlItems)) {
                pstmt.setLong(1, saleId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    String sName = rs.getString("service_name");
                    String sCat = rs.getString("service_category");
                    String wName = rs.getString("nombres") + " " + rs.getString("apellidos");
                    int wId = rs.getInt("worker_id");
                    double price = rs.getDouble("price_at_sale");
                    boolean clientBrought = rs.getBoolean("client_brought_product");
                    
                    if (clientBrought) sName += " (Cliente)";

                    // Logic copied/adapted from PayrollService to calculate commission
                    double commission = calculateCommission(wName, wId, sName, sCat, price, clientBrought);
                    double profit = price - commission;
                    
                    String percentStr = "N/A";
                    if (price > 0) {
                        percentStr = percentFormat.format(commission / price);
                    }

                    servicesModel.addRow(new Object[]{
                        sName, 
                        sCat, 
                        wName, 
                        currencyFormat.format(price),
                        currencyFormat.format(commission),
                        percentStr,
                        currencyFormat.format(profit)
                    });

                    // Accumulate Category Profit
                    String lowerName = sName.toLowerCase();
                    String lowerCat = (sCat != null) ? sCat.toLowerCase() : "";

                    if ("lavado".equals(lowerCat)) pLavado += profit;
                    else if (lowerName.contains("fusio")) pFusio += profit;
                    else if ("quimico".equals(lowerCat)) pQuimicos += profit;
                    else if ("otros".equals(lowerCat) || lowerName.contains("producto")) pProductos += profit;
                    
                    if (!"PAGO-MANUAL".equals(sCat)) { // Exclude manual payments from profit
                        totalOperatingProfit += profit;
                    }
                }
            }
            
            lblProfitLavado.setText(currencyFormat.format(pLavado));
            lblProfitFusio.setText(currencyFormat.format(pFusio));
            lblProfitQuimicos.setText(currencyFormat.format(pQuimicos));
            lblProfitProductos.setText(currencyFormat.format(pProductos));
            lblProfitTotal.setText(currencyFormat.format(totalOperatingProfit));

            // 3. Load Tips
            StringBuilder tipsText = new StringBuilder("<html>");
            try (PreparedStatement pstmt = conn.prepareStatement(sqlTips)) {
                pstmt.setLong(1, saleId);
                ResultSet rs = pstmt.executeQuery();
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    tipsText.append(rs.getString("recipient_name"))
                            .append(": <b>")
                            .append(currencyFormat.format(rs.getDouble("amount")))
                            .append("</b><br>");
                }
                if (!any) tipsText.append("Sin propinas");
            }
            tipsText.append("</html>");
            lblTipsTotal.setText(tipsText.toString());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading invoice details", e);
        }
    }

    /**
     * Lógica replicada de PayrollService para consistencia en la visualización.
     */
    private double calculateCommission(String tName, int tId, String sName, String sCat, double price, boolean clientBroughtProduct) {
        if ("PAGO-MANUAL".equals(sCat)) return price; // Todo para la trabajadora

        boolean isDepilacion = sName.equals("Cejas") || sName.equals("Bozo");

        // GRUPO: Dayana, Jaqueline, Maria Virginia
        if (tName.contains("Jaqueline") || tName.contains("Dayana") || tName.contains("Maria Virginia")) {
            if (tName.contains("Maria Virginia") && isDepilacion) return price * 0.50; 
            
            if ("Lavado".equalsIgnoreCase(sCat)) {
                if (Math.abs(price - 8.0) < 0.01) return 3.0;
                return price * 0.40;
            }
            if (sName.contains("Fusio-Dose")) return 8.0; 
            if (sName.contains("Extensiones (1 Paquete)")) return 10.0;
            if (sName.contains("Extensiones (2 Paquetes)")) return 20.0;
            if (sName.contains("Extensiones (3 Paquetes)")) return 15.0; 
        }
        else if (tName.contains("Belkis")) {
            if (sName.equals("Mechas")) return price * 0.36;
            if (sName.equals("Keratina")) return price * 0.70;
            if (isDepilacion) return price * 0.50;
        }
        else if (tName.contains("Aurora")) {
            if (isDepilacion) return price * 0.50;
        }
        else if (tName.contains("Jeimy")) {
            if (sName.equals("Mechas")) return price * 0.36;
            if (sName.contains("Extensiones (1 Paquete)")) return 20.0;
            if (sName.contains("Extensiones (2 Paquetes)")) return 30.0;
            if (sName.contains("Extensiones (3 Paquetes)")) return 40.0;
        }
        
        if (sName.contains("Tinte")) {
            return clientBroughtProduct ? 12.5 : price * 0.25;
        }

        String key = tId + "-" + sCat;
        Double rate = ruleMap.get(key);
        if (rate != null) return price * rate;

        return 0.0;
    }
}
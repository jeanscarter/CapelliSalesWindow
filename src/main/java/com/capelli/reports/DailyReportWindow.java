package com.capelli.reports;

import com.capelli.database.Database;
import com.capelli.config.AppConfig;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.miginfocom.swing.MigLayout;

public class DailyReportWindow extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(DailyReportWindow.class.getName());
    
    // Componentes de Fecha
    private final JSpinner dateSpinner;
    
    // Etiquetas de montos
    private final JLabel rateUsedLabel;
    private final JLabel cashUsdLabel;
    private final JLabel posAndMobilePaymentBsLabel;
    private final JLabel zelleLabel;
    private final JLabel accountsReceivableLabel;
    private final JLabel personalAccountPaymentsLabel;
    private final JLabel othersLabel;
    private final JLabel totalIvaLabel;
    
    // Etiqueta de Total
    private final JLabel totalDayLabel;

    private final DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");

    private record DailyStats(
        double rateUsed,
        double cashUsd, 
        double totalBsCapelli, 
        double totalBsRosa, 
        double zelleUsd, 
        double receivableUsd, // Esto ahora será la deuda REAL (Total - Pagado)
        double otherUsd,      // Para otros métodos en $
        double totalIva
    ) {}

    public DailyReportWindow() {
        setTitle("Reporte Diario de Operaciones - Capelli");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(650, 600);
        setLocationRelativeTo(null);
        setResizable(false);
        
        // --- Inicialización de Componentes ---
        dateSpinner = new JSpinner(new SpinnerDateModel());
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));
        dateSpinner.setValue(new Date()); 

        JButton refreshButton = new JButton("Consultar Fecha");
        refreshButton.addActionListener(e -> loadReportData());

        // Etiquetas de resultados
        rateUsedLabel = new JLabel("Tasa: -");
        rateUsedLabel.setForeground(Color.GRAY);
        
        cashUsdLabel = new JLabel("Cargando...");
        posAndMobilePaymentBsLabel = new JLabel("Cargando...");
        zelleLabel = new JLabel("Cargando...");
        accountsReceivableLabel = new JLabel("Cargando...");
        personalAccountPaymentsLabel = new JLabel("Cargando...");
        othersLabel = new JLabel("Cargando..."); 
        totalIvaLabel = new JLabel("Cargando...");
        
        totalDayLabel = new JLabel("$ 0.00");
        totalDayLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        totalDayLabel.setForeground(new Color(0, 150, 0));

        // --- Diseño del Panel (Layout) ---
        JPanel mainPanel = new JPanel(new MigLayout("wrap 2, fillx, insets 15", "[right]15[grow, left]"));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Resumen del Día"));

        // Fila 1: Selección de Fecha
        mainPanel.add(new JLabel("Seleccione Fecha:"));
        JPanel datePanel = new JPanel(new MigLayout("insets 0", "[grow][][]"));
        datePanel.add(dateSpinner, "growx, w 150!");
        datePanel.add(refreshButton, "gapleft 10");
        datePanel.add(rateUsedLabel, "gapleft 15");
        mainPanel.add(datePanel, "growx");

        // Separador
        mainPanel.add(new javax.swing.JSeparator(), "span 2, growx, gapbottom 10");

        // Filas de Datos
        mainPanel.add(new JLabel("Efectivo ($):"));
        mainPanel.add(cashUsdLabel, "growx");

        mainPanel.add(new JLabel("Pto. Venta / P. Móvil Capelli:"));
        mainPanel.add(posAndMobilePaymentBsLabel, "growx");
        
        mainPanel.add(new JLabel("Pagos Cta. Personal:"));
        mainPanel.add(personalAccountPaymentsLabel, "growx");

        mainPanel.add(new JLabel("Zelle / Transferencia ($):"));
        mainPanel.add(zelleLabel, "growx");
        
        // Aquí mostramos la DEUDA real, no el total de la venta
        mainPanel.add(new JLabel("Cuentas por Cobrar (Deuda):"));
        mainPanel.add(accountsReceivableLabel, "growx");

        mainPanel.add(new JLabel("Otros (préstamos, etc.):"));
        mainPanel.add(othersLabel, "growx");

        // Separador pequeño antes del IVA
        mainPanel.add(new javax.swing.JSeparator(), "span 2, growx, gaptop 5, gapbottom 5");

        // Fila de IVA
        JLabel lblIvaTitle = new JLabel("Total IVA Recaudado:");
        lblIvaTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        mainPanel.add(lblIvaTitle);
        mainPanel.add(totalIvaLabel, "growx");
        
        // Separador Final
        mainPanel.add(new javax.swing.JSeparator(), "span 2, growx, gaptop 10, gapbottom 10");
        
        // Total General
        JLabel lblTotalTitle = new JLabel("TOTAL GENERAL ($):");
        lblTotalTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        mainPanel.add(lblTotalTitle);
        mainPanel.add(totalDayLabel, "growx");

        add(mainPanel);
        
        loadReportData();
    }

    private void loadReportData() {
        setLabelsToLoading();
        
        Date selectedDate = (Date) dateSpinner.getValue();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(selectedDate);
        
        SwingWorker<DailyStats, Void> worker = new SwingWorker<>() {
            @Override
            protected DailyStats doInBackground() throws Exception {
                double rateFound = 0.0;
                double cashUsd = 0;
                double totalBsCapelli = 0;
                double totalBsRosa = 0;
                double zelleUsd = 0; 
                double receivableUsd = 0; // Deuda real
                double otherUsd = 0;
                double totalIva = 0;

                try (Connection conn = Database.connect()) {
                    
                    // 1. Obtener la Tasa de referencia del día
                    String sqlRate = "SELECT bcv_rate_at_sale FROM sales WHERE date(sale_date) = ? ORDER BY sale_date ASC LIMIT 1";
                    try (PreparedStatement pstmt = conn.prepareStatement(sqlRate)) {
                        pstmt.setString(1, dateStr);
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            rateFound = rs.getDouble("bcv_rate_at_sale");
                        }
                    }
                    if (rateFound <= 0) rateFound = AppConfig.getDefaultBcvRate();

                    // 2. Calcular IVA Total
                    String sqlIva = "SELECT COALESCE(SUM(vat_amount), 0.0) FROM sales WHERE date(sale_date) = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(sqlIva)) {
                        pstmt.setString(1, dateStr);
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) totalIva = rs.getDouble(1);
                    }

                    // 3. Calcular Pagos realizados (Dinero real que entró)
                    String sqlPayments = "SELECT "
                                       + "    p.metodo_pago, p.moneda, p.monto, "
                                       + "    p.destino_pago, p.tasa_bcv_al_pago "
                                       + "FROM sale_payments p "
                                       + "JOIN sales s ON p.sale_id = s.sale_id "
                                       + "WHERE date(s.sale_date) = ?";

                    try (PreparedStatement pstmt = conn.prepareStatement(sqlPayments)) {
                        pstmt.setString(1, dateStr);
                        ResultSet rs = pstmt.executeQuery();
                        
                        while (rs.next()) {
                            String method = rs.getString("metodo_pago");
                            String currency = rs.getString("moneda");
                            double amount = rs.getDouble("monto");
                            String destination = rs.getString("destino_pago");
                            // Si la moneda es Bs, el 'monto' guardado es en Bs.
                            // Si es $, 'monto' es $.

                            if ("$".equals(currency)) {
                                if ("Efectivo $".equals(method)) cashUsd += amount;
                                else if ("Transferencia".equals(method)) zelleUsd += amount;
                                else otherUsd += amount;
                            } else if ("Bs".equals(currency)) {
                                if ("Pago Movil".equals(method)) {
                                    if ("Rosa".equals(destination)) totalBsRosa += amount;
                                    else totalBsCapelli += amount;
                                } else {
                                    totalBsCapelli += amount; // Puntos de venta, etc.
                                }
                            }
                        }
                    }

                    // 4. Calcular Cuentas por Cobrar (Deuda Real = Total Venta - Total Pagado en esa venta)
                    // Solo para ventas marcadas como 'Cuenta por Cobrar'
                    String sqlReceivable = 
                        "SELECT " +
                        "  s.total, " +
                        "  (SELECT COALESCE(SUM(CASE WHEN sp.moneda = 'Bs' THEN sp.monto / sp.tasa_bcv_al_pago ELSE sp.monto END), 0) " +
                        "   FROM sale_payments sp WHERE sp.sale_id = s.sale_id) as paid_usd " +
                        "FROM sales s " +
                        "WHERE date(s.sale_date) = ? AND s.discount_type = 'Cuenta por Cobrar'";

                    try (PreparedStatement pstmt = conn.prepareStatement(sqlReceivable)) {
                        pstmt.setString(1, dateStr);
                        ResultSet rs = pstmt.executeQuery();
                        while (rs.next()) {
                            double totalVenta = rs.getDouble("total");
                            double totalPagado = rs.getDouble("paid_usd");
                            // La deuda es la diferencia. Si pagó todo, es 0.
                            double deuda = totalVenta - totalPagado;
                            if (deuda > 0.01) { // Ignorar diferencias por redondeo ínfimo
                                receivableUsd += deuda;
                            }
                        }
                    }

                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error al cargar datos del reporte diario", e);
                }
                
                return new DailyStats(rateFound, cashUsd, totalBsCapelli, totalBsRosa, zelleUsd, receivableUsd, otherUsd, totalIva);
            }

            @Override
            protected void done() {
                try {
                    DailyStats stats = get();
                    
                    rateUsedLabel.setText("(Tasa usada: " + currencyFormat.format(stats.rateUsed) + " Bs/$)");
                    
                    double capelliInUsd = (stats.rateUsed > 0) ? (stats.totalBsCapelli / stats.rateUsed) : 0;
                    double rosaInUsd = (stats.rateUsed > 0) ? (stats.totalBsRosa / stats.rateUsed) : 0;
                    
                    cashUsdLabel.setText("$ " + currencyFormat.format(stats.cashUsd));
                    
                    posAndMobilePaymentBsLabel.setText("Bs " + currencyFormat.format(stats.totalBsCapelli) + 
                            "  ➤  ($ " + currencyFormat.format(capelliInUsd) + ")");
                            
                    personalAccountPaymentsLabel.setText("Bs " + currencyFormat.format(stats.totalBsRosa) + 
                            "  ➤  ($ " + currencyFormat.format(rosaInUsd) + ")");
                    
                    zelleLabel.setText("$ " + currencyFormat.format(stats.zelleUsd));
                    
                    // Mostrar Deuda Real
                    accountsReceivableLabel.setText("$ " + currencyFormat.format(stats.receivableUsd));
                    if (stats.receivableUsd > 0) accountsReceivableLabel.setForeground(Color.RED);
                    else accountsReceivableLabel.setForeground(UIManager.getColor("Label.foreground"));

                    othersLabel.setText("$ " + currencyFormat.format(stats.otherUsd));

                    // Mostrar IVA
                    double totalIvaBs = stats.totalIva * stats.rateUsed;
                    totalIvaLabel.setText("Bs " + currencyFormat.format(totalIvaBs) + " ($ " + currencyFormat.format(stats.totalIva) + ")");
                    
                    // Total General del Día = Todo lo cobrado + Lo que quedó debiendo
                    double grandTotal = stats.cashUsd + stats.zelleUsd + stats.otherUsd + capelliInUsd + rosaInUsd + stats.receivableUsd;
                    
                    totalDayLabel.setText("$ " + currencyFormat.format(grandTotal));

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error al mostrar resultados", e);
                }
            }
        };
        worker.execute();
    }
    
    private void setLabelsToLoading() {
        String loading = "Calculando...";
        rateUsedLabel.setText("Consultando...");
        cashUsdLabel.setText(loading);
        posAndMobilePaymentBsLabel.setText(loading);
        personalAccountPaymentsLabel.setText(loading);
        zelleLabel.setText(loading);
        accountsReceivableLabel.setText(loading);
        othersLabel.setText(loading);
        totalIvaLabel.setText(loading);
        totalDayLabel.setText("$ -");
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
        
        SwingUtilities.invokeLater(() -> {
            Database.initialize();
            new DailyReportWindow().setVisible(true);
        });
    }
}
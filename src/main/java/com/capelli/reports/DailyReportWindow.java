package com.capelli.reports;

import com.capelli.capellisaleswindow.BCVService;
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
    private final JLabel rateUsedLabel; // Etiqueta para mostrar la tasa usada
    private final JLabel cashUsdLabel;
    private final JLabel posAndMobilePaymentBsLabel;
    private final JLabel zelleLabel;
    private final JLabel accountsReceivableLabel;
    private final JLabel personalAccountPaymentsLabel;
    private final JLabel othersLabel;
    
    // Etiqueta de Total
    private final JLabel totalDayLabel;

    private final DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");

    // Record para transportar datos del worker a la UI
    private record DailyStats(
        double rateUsed,
        double cashUsd, 
        double totalBsCapelli, 
        double totalBsRosa, 
        double zelleUsd, 
        double receivableUsd
    ) {}

    public DailyReportWindow() {
        setTitle("Reporte Diario de Operaciones - Capelli");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(650, 550); 
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
        othersLabel = new JLabel("$ 0.00"); 
        
        totalDayLabel = new JLabel("$ 0.00");
        totalDayLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        totalDayLabel.setForeground(new Color(0, 150, 0)); // Verde oscuro

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
        
        mainPanel.add(new JLabel("Cuentas por Cobrar ($):"));
        mainPanel.add(accountsReceivableLabel, "growx");

        mainPanel.add(new JLabel("Otros (préstamos, etc.):"));
        mainPanel.add(othersLabel, "growx");
        
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
                double receivableUsd = 0;

                try (Connection conn = Database.connect()) {
                    
                    // 1. Obtener la Tasa de la PRIMERA venta del día
                    String sqlRate = "SELECT bcv_rate_at_sale FROM sales WHERE date(sale_date) = ? ORDER BY sale_date ASC LIMIT 1";
                    try (PreparedStatement pstmt = conn.prepareStatement(sqlRate)) {
                        pstmt.setString(1, dateStr);
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            rateFound = rs.getDouble("bcv_rate_at_sale");
                        }
                    }
                    
                    // Si no hubo ventas o la tasa es 0, usamos la tasa actual/configurada por seguridad
                    if (rateFound <= 0) {
                        rateFound = AppConfig.getDefaultBcvRate();
                    }

                    // 2. Calcular Cuentas por Cobrar
                    String sqlReceivable = "SELECT COALESCE(SUM(total), 0.0) FROM sales "
                                         + "WHERE date(sale_date) = ? "
                                         + "AND discount_type = 'Cuenta por Cobrar'";
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(sqlReceivable)) {
                        pstmt.setString(1, dateStr);
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            receivableUsd = rs.getDouble(1);
                        }
                    }

                    // 3. Calcular Pagos
                    String sqlPayments = "SELECT "
                                       + "    p.metodo_pago, p.moneda, p.monto, "
                                       + "    p.destino_pago "
                                       + "FROM sale_payments p "
                                       + "JOIN sales s ON p.sale_id = s.sale_id "
                                       + "WHERE date(s.sale_date) = ? "
                                       + "AND s.discount_type != 'Cuenta por Cobrar'";

                    try (PreparedStatement pstmt = conn.prepareStatement(sqlPayments)) {
                        pstmt.setString(1, dateStr);
                        ResultSet rs = pstmt.executeQuery();
                        
                        while (rs.next()) {
                            String method = rs.getString("metodo_pago");
                            String currency = rs.getString("moneda");
                            double amount = rs.getDouble("monto");
                            String destination = rs.getString("destino_pago");

                            if ("$".equals(currency)) {
                                if ("Efectivo $".equals(method)) {
                                    cashUsd += amount;
                                } else if ("Transferencia".equals(method)) {
                                    zelleUsd += amount;
                                }
                            } else if ("Bs".equals(currency)) {
                                if ("Pago Movil".equals(method)) {
                                    if ("Rosa".equals(destination)) {
                                        totalBsRosa += amount;
                                    } else {
                                        totalBsCapelli += amount;
                                    }
                                } else {
                                    // TD, TC, Efectivo Bs van a Capelli
                                    totalBsCapelli += amount;
                                }
                            }
                        }
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error al cargar datos del reporte diario", e);
                    // En caso de error retornamos ceros
                }
                
                return new DailyStats(rateFound, cashUsd, totalBsCapelli, totalBsRosa, zelleUsd, receivableUsd);
            }

            @Override
            protected void done() {
                try {
                    DailyStats stats = get();
                    
                    // Mostrar Tasa usada
                    rateUsedLabel.setText("(Tasa usada: " + currencyFormat.format(stats.rateUsed) + " Bs/$)");
                    
                    // Conversiones
                    double capelliInUsd = (stats.rateUsed > 0) ? (stats.totalBsCapelli / stats.rateUsed) : 0;
                    double rosaInUsd = (stats.rateUsed > 0) ? (stats.totalBsRosa / stats.rateUsed) : 0;
                    
                    // Set labels
                    cashUsdLabel.setText("$ " + currencyFormat.format(stats.cashUsd));
                    
                    posAndMobilePaymentBsLabel.setText("Bs " + currencyFormat.format(stats.totalBsCapelli) + 
                            "  ➤  ($ " + currencyFormat.format(capelliInUsd) + ")");
                            
                    personalAccountPaymentsLabel.setText("Bs " + currencyFormat.format(stats.totalBsRosa) + 
                            "  ➤  ($ " + currencyFormat.format(rosaInUsd) + ")");
                    
                    zelleLabel.setText("$ " + currencyFormat.format(stats.zelleUsd));
                    accountsReceivableLabel.setText("$ " + currencyFormat.format(stats.receivableUsd));
                    
                    // Calcular Total General en Dólares
                    // (Nota: othersLabel está hardcodeado a 0 en la inicialización, si hubiera lógica se suma aquí)
                    double grandTotal = stats.cashUsd + stats.zelleUsd + stats.receivableUsd + capelliInUsd + rosaInUsd;
                    
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
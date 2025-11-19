package com.capelli.reports;

import com.capelli.capellisaleswindow.BCVService;
import com.capelli.database.Database;
import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import net.miginfocom.swing.MigLayout;
import com.capelli.config.AppConfig;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DailyReportWindow extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(DailyReportWindow.class.getName());
    
    // Componentes de Fecha
    private final JSpinner dateSpinner;
    
    // Etiquetas de montos
    private final JLabel cashUsdLabel;
    private final JLabel posAndMobilePaymentBsLabel;
    private final JLabel zelleLabel;
    private final JLabel accountsReceivableLabel;
    private final JLabel personalAccountPaymentsLabel;
    private final JLabel othersLabel;

    private final DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");
    private double bcvRate = AppConfig.getDefaultBcvRate();

    public DailyReportWindow() {
        setTitle("Reporte Diario de Operaciones - Capelli");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 450); // Un poco más ancho para el selector
        setLocationRelativeTo(null);
        setResizable(false);
        
        // Carga inicial de tasa (aunque el reporte usa montos guardados, es bueno tenerla actualizada)
        loadBcvRate(); 

        // --- Inicialización de Componentes ---
        
        // Selector de Fecha
        dateSpinner = new JSpinner(new SpinnerDateModel());
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));
        dateSpinner.setValue(new Date()); // Por defecto hoy

        // Etiquetas de resultados
        cashUsdLabel = new JLabel("Cargando...");
        posAndMobilePaymentBsLabel = new JLabel("Cargando...");
        zelleLabel = new JLabel("Cargando...");
        accountsReceivableLabel = new JLabel("Cargando...");
        personalAccountPaymentsLabel = new JLabel("Cargando...");
        othersLabel = new JLabel("$ 0.00"); 

        JButton refreshButton = new JButton("Consultar Fecha");
        refreshButton.addActionListener(e -> loadReportData());

        // --- Diseño del Panel (Layout) ---
        JPanel mainPanel = new JPanel(new MigLayout("wrap 2, fillx, insets 15", "[right]15[grow, left]"));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Resumen del Día"));

        // Fila 1: Selección de Fecha
        mainPanel.add(new JLabel("Seleccione Fecha:"));
        JPanel datePanel = new JPanel(new MigLayout("insets 0", "[grow][]"));
        datePanel.add(dateSpinner, "growx, w 150!");
        datePanel.add(refreshButton, "gapleft 10");
        mainPanel.add(datePanel, "growx");

        // Separador (CORREGIDO: gapyb 10 -> gapbottom 10)
        mainPanel.add(new javax.swing.JSeparator(), "span 2, growx, gapbottom 10");

        // Filas de Datos
        mainPanel.add(new JLabel("Efectivo ($):"));
        mainPanel.add(cashUsdLabel, "growx");

        mainPanel.add(new JLabel("Pto. Venta / P. Móvil Capelli (Bs):"));
        mainPanel.add(posAndMobilePaymentBsLabel, "growx");
        
        mainPanel.add(new JLabel("Pagos Cta. Personal (Bs):"));
        mainPanel.add(personalAccountPaymentsLabel, "growx");

        mainPanel.add(new JLabel("Zelle / Transferencia ($):"));
        mainPanel.add(zelleLabel, "growx");
        
        mainPanel.add(new JLabel("Cuentas por Cobrar ($):"));
        mainPanel.add(accountsReceivableLabel, "growx");

        mainPanel.add(new JLabel("Otros (préstamos, etc.):"));
        mainPanel.add(othersLabel, "growx");

        add(mainPanel);
        
        // Cargar datos iniciales (fecha de hoy)
        loadReportData();
    }
    
    private void loadBcvRate() {
        SwingWorker<Double, Void> worker = new SwingWorker<Double, Void>() {
            @Override
            protected Double doInBackground() throws Exception {
                return BCVService.getBCVRateSafe();
            }

            @Override
            protected void done() {
                try {
                    double rate = get();
                    if (rate > 0) {
                        bcvRate = rate;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al cargar tasa BCV", e);
                }
            }
        };
        worker.execute();
    }

    private void loadReportData() {
        setLabelsToLoading();
        
        // Obtener la fecha seleccionada del Spinner
        Date selectedDate = (Date) dateSpinner.getValue();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(selectedDate);
        
        SwingWorker<double[], Void> worker = new SwingWorker<>() {
            @Override
            protected double[] doInBackground() throws Exception {
                double cashUsd = 0;
                double totalBsCapelli = 0;
                double totalBsRosa = 0;
                double zelleUsd = 0; 
                double receivableUsd = 0;

                try (Connection conn = Database.connect()) {
                    
                    // --- 1. Calcular Cuentas por Cobrar ---
                    // Se usa la fecha seleccionada (dateStr)
                    String sqlReceivable = "SELECT COALESCE(SUM(total), 0.0) FROM sales "
                                         + "WHERE date(sale_date) = ? " // Eliminado 'localtime' para precisión
                                         + "AND discount_type = 'Cuenta por Cobrar'";
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(sqlReceivable)) {
                        pstmt.setString(1, dateStr);
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            receivableUsd = rs.getDouble(1);
                        }
                    }

                    // --- 2. Calcular Pagos ---
                    // Se usa la fecha seleccionada (dateStr)
                    String sqlPayments = "SELECT "
                                       + "    p.metodo_pago, p.moneda, p.monto, "
                                       + "    p.destino_pago "
                                       + "FROM sale_payments p "
                                       + "JOIN sales s ON p.sale_id = s.sale_id "
                                       + "WHERE date(s.sale_date) = ? " // Eliminado 'localtime' para precisión
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
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DailyReportWindow.this,
                            "Error al cargar los datos: " + e.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE));
                }
                return new double[]{cashUsd, totalBsCapelli, totalBsRosa, zelleUsd, receivableUsd};
            }

            @Override
            protected void done() {
                try {
                    double[] results = get();
                    cashUsdLabel.setText("$ " + currencyFormat.format(results[0]));
                    posAndMobilePaymentBsLabel.setText("Bs " + currencyFormat.format(results[1]));
                    personalAccountPaymentsLabel.setText("Bs " + currencyFormat.format(results[2]));
                    zelleLabel.setText("$ " + currencyFormat.format(results[3]));
                    accountsReceivableLabel.setText("$ " + currencyFormat.format(results[4]));
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error al mostrar resultados", e);
                }
            }
        };
        worker.execute();
    }
    
    private void setLabelsToLoading() {
        String loading = "Calculando...";
        cashUsdLabel.setText(loading);
        posAndMobilePaymentBsLabel.setText(loading);
        personalAccountPaymentsLabel.setText(loading);
        zelleLabel.setText(loading);
        accountsReceivableLabel.setText(loading);
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
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
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import net.miginfocom.swing.MigLayout;
import com.capelli.config.AppConfig;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DailyReportWindow extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(DailyReportWindow.class.getName());
    private final JLabel reportDateLabel;
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
        setSize(550, 400);
        setLocationRelativeTo(null);
        setResizable(false);
        loadBcvRate();

        String today = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        reportDateLabel = new JLabel("Mostrando reporte para la fecha: " + today);
        reportDateLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        cashUsdLabel = new JLabel("Cargando...");
        posAndMobilePaymentBsLabel = new JLabel("Cargando...");
        zelleLabel = new JLabel("Cargando...");
        accountsReceivableLabel = new JLabel("Cargando...");
        personalAccountPaymentsLabel = new JLabel("Cargando...");
        othersLabel = new JLabel("$ 0.00"); 

        JButton refreshButton = new JButton("Actualizar Reporte");
        refreshButton.addActionListener(e -> loadReportData());

        JPanel mainPanel = new JPanel(new MigLayout("wrap 2, fillx, insets 15", "[right]15[grow, left]"));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Resumen del Día"));

        mainPanel.add(reportDateLabel, "span 2, center, gapbottom 15");

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
        
        mainPanel.add(refreshButton, "span 2, center, gaptop 20");

        add(mainPanel);
        
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
                    LOGGER.info("Tasa BCV actualizada en reporte diario: " + rate);
                    // Recargar datos con nueva tasa
                    loadReportData();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al cargar tasa BCV para reporte", e);
            }
        }
    };
    worker.execute();
}

    private void loadReportData() {
        setLabelsToLoading();
        
        SwingWorker<double[], Void> worker = new SwingWorker<>() {
            @Override
            protected double[] doInBackground() throws Exception {
                double cashUsd = 0;
                double totalBsCapelli = 0;
                double totalBsRosa = 0;
                double zelleUsd = 0;
                double receivableUsd = 0;

                String todayStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

                try (Connection conn = Database.connect()) {
                    String sql = "SELECT total, currency, payment_method, discount_type, payment_destination FROM sales WHERE date(sale_date, 'localtime') = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, todayStr);
                        ResultSet rs = pstmt.executeQuery();
                        while (rs.next()) {
                            double total = rs.getDouble("total");
                            String currency = rs.getString("currency");
                            String paymentMethod = rs.getString("payment_method");
                            String discountType = rs.getString("discount_type");
                            String paymentDestination = rs.getString("payment_destination");

                            double totalInUsd = "Bs".equals(currency) ? total / bcvRate : total;

                            if ("Cuenta por Cobrar".equals(discountType)) {
                                receivableUsd += totalInUsd;
                                continue; 
                            }

                            switch (paymentMethod) {
                                case "Efectivo $":
                                    cashUsd += total;
                                    break;
                                case "TD":
                                case "TC":
                                case "Efectivo Bs":
                                    totalBsCapelli += "Bs".equals(currency) ? total : total * bcvRate;
                                    break;
                                case "Transferencia":
                                    zelleUsd += totalInUsd;
                                    break;
                                case "Pago Movil":
                                    if ("Rosa".equals(paymentDestination)) {
                                        totalBsRosa += "Bs".equals(currency) ? total : total * bcvRate;
                                    } else {
                                        totalBsCapelli += "Bs".equals(currency) ? total : total * bcvRate;
                                    }
                                    break;
                            }
                        }
                    }
                } catch (SQLException e) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DailyReportWindow.this,
                            "Error al cargar los datos del reporte: " + e.getMessage(), "Error de Base de Datos", JOptionPane.ERROR_MESSAGE));
                    e.printStackTrace();
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
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void setLabelsToLoading() {
        cashUsdLabel.setText("Calculando...");
        posAndMobilePaymentBsLabel.setText("Calculando...");
        personalAccountPaymentsLabel.setText("Calculando...");
        zelleLabel.setText("Calculando...");
        accountsReceivableLabel.setText("Calculando...");
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
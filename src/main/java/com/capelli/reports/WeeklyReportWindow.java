package com.capelli.reports;

import com.capelli.config.AppConfig;
import com.capelli.database.Database;
import com.formdev.flatlaf.FlatDarkLaf;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WeeklyReportWindow extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(WeeklyReportWindow.class.getName());
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private final SimpleDateFormat sdfSql = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat sdfDisplay = new SimpleDateFormat("dd/MM/yyyy");

    private JSpinner startDateSpinner;
    private JSpinner endDateSpinner;
    private JTable reportTable;
    private DefaultTableModel tableModel;
    private JLabel totalPeriodoLabel;

    public WeeklyReportWindow() {
        setTitle("Reporte Semanal / Rango de Fechas - Capelli");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 600);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        // Configuración de fechas (Por defecto últimos 7 días)
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        Date sevenDaysAgo = cal.getTime();

        startDateSpinner = new JSpinner(new SpinnerDateModel());
        startDateSpinner.setEditor(new JSpinner.DateEditor(startDateSpinner, "dd/MM/yyyy"));
        startDateSpinner.setValue(sevenDaysAgo);

        endDateSpinner = new JSpinner(new SpinnerDateModel());
        endDateSpinner.setEditor(new JSpinner.DateEditor(endDateSpinner, "dd/MM/yyyy"));
        endDateSpinner.setValue(today);

        // Configuración de Tabla
        String[] columnNames = {
            "Fecha", 
            "Efectivo ($)", 
            "Pto Venta/PM Capelli ($)", 
            "Zelle ($)", 
            "Ctas por Cobrar ($)", 
            "Pago Cta Personal ($)", 
            "Otros ($)", 
            "TOTAL DÍA ($)"
        };

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? String.class : String.class;
            }
        };

        reportTable = new JTable(tableModel);
        reportTable.setRowHeight(25);
        reportTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        // Renderizar columna de totales en negrita
        DefaultTableCellRenderer boldRenderer = new DefaultTableCellRenderer();
        boldRenderer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        boldRenderer.setHorizontalAlignment(JLabel.RIGHT);
        reportTable.getColumnModel().getColumn(7).setCellRenderer(boldRenderer);
        
        // Alinear números a la derecha
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        for(int i=1; i<7; i++) {
            reportTable.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }

        totalPeriodoLabel = new JLabel("Total Periodo: $ 0.00");
        totalPeriodoLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        totalPeriodoLabel.setForeground(new Color(0, 150, 0));
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel de Control Superior
        JPanel controlsPanel = new JPanel(new MigLayout("fillx", "[right]10[grow]20[right]10[grow]20[]20[]"));
        controlsPanel.setBorder(new TitledBorder("Rango de Consulta"));
        
        JButton btnConsultar = new JButton("Generar Reporte");
        btnConsultar.setIcon(UIManager.getIcon("FileView.directoryIcon"));
        btnConsultar.addActionListener(e -> generarReporte());

        JButton btnExportar = new JButton("Exportar CSV");
        btnExportar.addActionListener(e -> exportarCSV());

        controlsPanel.add(new JLabel("Desde:"));
        controlsPanel.add(startDateSpinner);
        controlsPanel.add(new JLabel("Hasta:"));
        controlsPanel.add(endDateSpinner);
        controlsPanel.add(btnConsultar, "w 150!");
        controlsPanel.add(btnExportar, "w 120!");

        // Panel Central (Tabla)
        JScrollPane scrollPane = new JScrollPane(reportTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // Panel Inferior (Totales)
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
        bottomPanel.add(totalPeriodoLabel);

        mainPanel.add(controlsPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void generarReporte() {
        Date start = (Date) startDateSpinner.getValue();
        Date end = (Date) endDateSpinner.getValue();

        if (start.after(end)) {
            JOptionPane.showMessageDialog(this, "La fecha de inicio no puede ser mayor a la fecha fin.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Bloquear UI
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        tableModel.setRowCount(0);

        SwingWorker<List<DailyData>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<DailyData> doInBackground() throws Exception {
                List<DailyData> dataList = new ArrayList<>();
                
                LocalDate startLd = start.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate endLd = end.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                long daysBetween = ChronoUnit.DAYS.between(startLd, endLd);

                // Iterar día por día para asegurar que días sin ventas aparezcan (opcional, aquí solo buscaremos los que tengan datos si preferimos, 
                // pero para consistencia iteraremos el rango)
                
                Calendar cal = Calendar.getInstance();
                cal.setTime(start);

                while (!cal.getTime().after(end)) {
                    Date currentDate = cal.getTime();
                    DailyData dayData = calcularDatosDia(currentDate);
                    // Solo agregar si hay algún movimiento o si queremos mostrar ceros
                    if (dayData.getTotalDia() > 0 || true) { 
                        dataList.add(dayData);
                    }
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
                
                return dataList;
            }

            @Override
            protected void done() {
                try {
                    List<DailyData> results = get();
                    llenarTabla(results);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error al generar reporte semanal", e);
                    JOptionPane.showMessageDialog(WeeklyReportWindow.this, "Error al consultar base de datos: " + e.getMessage());
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

    private DailyData calcularDatosDia(Date date) {
        String dateStr = sdfSql.format(date);
        
        double rateUsed = 0.0;
        double efectivoUsd = 0;
        double ptoVentaBsCapelli = 0; // En Bs
        double ptoVentaBsRosa = 0;    // En Bs (Cta Personal)
        double zelleUsd = 0; 
        double cxcUsd = 0;
        double otros = 0;

        try (Connection conn = Database.connect()) {
            // 1. Obtener Tasa del día (Promedio o Primera venta)
            String sqlRate = "SELECT bcv_rate_at_sale FROM sales WHERE date(sale_date) = ? LIMIT 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlRate)) {
                pstmt.setString(1, dateStr);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    rateUsed = rs.getDouble("bcv_rate_at_sale");
                }
            }
            if (rateUsed <= 0) rateUsed = AppConfig.getDefaultBcvRate();

            // 2. Cuentas por Cobrar
            String sqlCxc = "SELECT COALESCE(SUM(total), 0.0) FROM sales WHERE date(sale_date) = ? AND discount_type = 'Cuenta por Cobrar'";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlCxc)) {
                pstmt.setString(1, dateStr);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) cxcUsd = rs.getDouble(1);
            }

            // 3. Pagos
            String sqlPayments = "SELECT p.metodo_pago, p.moneda, p.monto, p.destino_pago " +
                                 "FROM sale_payments p " +
                                 "JOIN sales s ON p.sale_id = s.sale_id " +
                                 "WHERE date(s.sale_date) = ? AND s.discount_type != 'Cuenta por Cobrar'";

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
                            efectivoUsd += amount;
                        } else if ("Transferencia".equals(method)) { // Zelle
                            zelleUsd += amount;
                        } else {
                            otros += amount;
                        }
                    } else if ("Bs".equals(currency)) {
                        if ("Pago Movil".equals(method) && "Rosa".equals(destination)) {
                            ptoVentaBsRosa += amount;
                        } else {
                            // Todo lo demás en Bs (Pto Venta, Pago Movil Capelli, Efectivo Bs)
                            ptoVentaBsCapelli += amount;
                        }
                    }
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculando día " + dateStr, e);
        }

        // Convertir Bs a USD
        double capelliConverted = (rateUsed > 0) ? ptoVentaBsCapelli / rateUsed : 0;
        double rosaConverted = (rateUsed > 0) ? ptoVentaBsRosa / rateUsed : 0;

        return new DailyData(date, efectivoUsd, capelliConverted, zelleUsd, cxcUsd, rosaConverted, otros);
    }

    private void llenarTabla(List<DailyData> data) {
        double sumEfectivo = 0;
        double sumCapelli = 0;
        double sumZelle = 0;
        double sumCxc = 0;
        double sumRosa = 0;
        double sumOtros = 0;
        double sumTotalGeneral = 0;

        for (DailyData d : data) {
            // Solo agregar filas si hay montos (opcional)
            if (d.getTotalDia() > 0.01) {
                tableModel.addRow(new Object[]{
                    sdfDisplay.format(d.fecha),
                    df.format(d.efectivo),
                    df.format(d.capelliBsConverted),
                    df.format(d.zelle),
                    df.format(d.cxc),
                    df.format(d.rosaBsConverted),
                    df.format(d.otros),
                    df.format(d.getTotalDia())
                });
            }

            sumEfectivo += d.efectivo;
            sumCapelli += d.capelliBsConverted;
            sumZelle += d.zelle;
            sumCxc += d.cxc;
            sumRosa += d.rosaBsConverted;
            sumOtros += d.otros;
            sumTotalGeneral += d.getTotalDia();
        }

        // Agregar fila de Totales del Rubro
        tableModel.addRow(new Object[]{"", "", "", "", "", "", "", ""}); // Espacio
        tableModel.addRow(new Object[]{
            "TOTALES RUBRO:",
            df.format(sumEfectivo),
            df.format(sumCapelli),
            df.format(sumZelle),
            df.format(sumCxc),
            df.format(sumRosa),
            df.format(sumOtros),
            df.format(sumTotalGeneral)
        });

        // Colorear la ultima fila si se usa un renderer custom, 
        // pero por ahora actualizamos el label grande
        totalPeriodoLabel.setText("Total Periodo: $ " + df.format(sumTotalGeneral));
    }

    private void exportarCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Reporte Semanal");
        fileChooser.setSelectedFile(new File("Reporte_Semanal_" + System.currentTimeMillis() + ".csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileWriter fw = new FileWriter(fileChooser.getSelectedFile())) {
                // Headers
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    fw.write(tableModel.getColumnName(i) + ((i == tableModel.getColumnCount() - 1) ? "" : ","));
                }
                fw.write("\n");

                // Rows
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        Object val = tableModel.getValueAt(i, j);
                        String str = (val == null) ? "" : val.toString().replace(",", "."); // Reemplazar coma decimal por punto para CSV estándar o mantener coma si es excel europeo
                        fw.write(str + ((j == tableModel.getColumnCount() - 1) ? "" : ","));
                    }
                    fw.write("\n");
                }
                JOptionPane.showMessageDialog(this, "Exportado exitosamente.");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al exportar: " + ex.getMessage());
            }
        }
    }

    // Clase interna para almacenar datos
    private record DailyData(
        Date fecha,
        double efectivo,
        double capelliBsConverted, // Pto Venta / Pago Movil Capelli convertido
        double zelle,
        double cxc,
        double rosaBsConverted,    // Pago Cta Personal convertido
        double otros
    ) {
        public double getTotalDia() {
            return efectivo + capelliBsConverted + zelle + cxc + rosaBsConverted + otros;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
        SwingUtilities.invokeLater(() -> new WeeklyReportWindow().setVisible(true));
    }
}
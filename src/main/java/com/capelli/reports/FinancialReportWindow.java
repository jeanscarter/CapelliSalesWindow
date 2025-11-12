// Archivo: src/main/java/com/capelli/reports/FinancialReportWindow.java
// (MODIFICADO para ser compatible con pagos múltiples)

package com.capelli.reports;

import com.capelli.database.Database;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Vector;

public class FinancialReportWindow extends JFrame {

    private final JSpinner startDateSpinner;
    private final JSpinner endDateSpinner;
    private final DefaultTableModel tableModel;
    private final JTable reportTable;

    public FinancialReportWindow() {
        super("Reporte Financiero Detallado");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel de Controles
        JPanel controlsPanel = new JPanel(new MigLayout("fillx", "[right]10[grow,fill]20[right]10[grow,fill]20[grow,fill]20[grow,fill]", ""));
        controlsPanel.setBorder(new TitledBorder("Seleccionar Rango de Fechas"));

        startDateSpinner = new JSpinner(new SpinnerDateModel());
        startDateSpinner.setEditor(new JSpinner.DateEditor(startDateSpinner, "dd/MM/yyyy"));

        endDateSpinner = new JSpinner(new SpinnerDateModel());
        endDateSpinner.setEditor(new JSpinner.DateEditor(endDateSpinner, "dd/MM/yyyy"));

        JButton generateButton = new JButton("Generar Reporte");
        JButton exportButton = new JButton("Exportar a CSV");

        controlsPanel.add(new JLabel("Fecha Inicio:"));
        controlsPanel.add(startDateSpinner, "sg date");
        controlsPanel.add(new JLabel("Fecha Fin:"));
        controlsPanel.add(endDateSpinner, "sg date");
        controlsPanel.add(generateButton, "sg button");
        controlsPanel.add(exportButton, "sg button");

        // Panel de Tabla
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        reportTable = new JTable(tableModel);
        reportTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        reportTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Importante para muchas columnas

        mainPanel.add(controlsPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(reportTable), BorderLayout.CENTER);

        add(mainPanel);

        // Listeners
        generateButton.addActionListener(e -> generateReport());
        exportButton.addActionListener(e -> exportToCSV());
    }

    private void generateReport() {
        Date startDate = (Date) startDateSpinner.getValue();
        Date endDate = (Date) endDateSpinner.getValue();
        LocalDate startLocalDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endLocalDate = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        // ===== INICIO DE MODIFICACIÓN (SQL) =====
        // La consulta ahora se une con 'sale_payments' (usando subconsultas con GROUP_CONCAT)
        // y 'tips' para obtener una vista completa de la transacción por cada item de servicio.
        String sql = "SELECT "
                + "    DATE(s.sale_date, 'localtime') AS Fecha, "
                + "    s.correlative_number AS Factura, "
                + "    COALESCE(c.full_name, 'N/A') AS Cliente, "
                + "    COALESCE(t.nombres || ' ' || t.apellidos, 'N/A') AS Trabajadora, "
                + "    COALESCE(svc.name, 'N/A') AS Servicio, "
                + "    COALESCE(svc.service_category, 'N/A') AS Categoria_Servicio, "
                + "    si.price_at_sale AS Precio_Item_USD, "
                + "    s.discount_type AS Tipo_Descuento, "
                + "    s.discount_amount AS Descuento_Venta_USD, "
                + "    s.vat_amount AS IVA_Venta_USD, "
                + "    (SELECT COALESCE(SUM(tip.amount), 0.0) FROM tips tip WHERE tip.sale_id = s.sale_id) AS Propina_Venta_USD, "
                + "    s.total AS Total_Venta_USD, "
                + "    s.bcv_rate_at_sale AS Tasa_BCV_Venta, "
                + "    ( "
                + "        SELECT GROUP_CONCAT( "
                + "            payment_method || ' (' || "
                + "            printf('%.2f', amount_currency) || ' ' || currency || "
                + "            CASE "
                + "                WHEN currency = 'Bs' THEN ' / @' || printf('%.2f', bcv_rate_at_payment) "
                + "                ELSE '' "
                + "            END || ' = $' || printf('%.2f', amount_usd) || "
                + "            COALESCE(' / Ref: ' || payment_reference, '') || "
                + "            COALESCE(' / Dest: ' || payment_destination, '') || "
                + "            ')', "
                + "            ' | ' "
                + "        ) "
                + "        FROM sale_payments sp "
                + "        WHERE sp.sale_id = s.sale_id "
                + "    ) AS Pagos_Detallados, "
                + "    ( "
                + "        SELECT COALESCE(SUM(sp.amount_usd), 0.0) "
                + "        FROM sale_payments sp "
                + "        WHERE sp.sale_id = s.sale_id "
                + "    ) AS Total_Pagado_USD "
                + "FROM "
                + "    sales s "
                + "LEFT JOIN "
                + "    sale_items si ON s.sale_id = si.sale_id "
                + "LEFT JOIN "
                + "    trabajadoras t ON si.employee_id = t.id "
                + "LEFT JOIN "
                + "    services svc ON si.service_id = svc.service_id "
                + "LEFT JOIN "
                + "    clients c ON s.client_id = c.client_id "
                + "WHERE "
                + "    DATE(s.sale_date, 'localtime') BETWEEN ? AND ? "
                + "ORDER BY "
                + "    s.sale_date, s.correlative_number, Trabajadora";
        // ===== FIN DE MODIFICACIÓN (SQL) =====


        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startLocalDate.toString());
            pstmt.setString(2, endLocalDate.toString());

            ResultSet rs = pstmt.executeQuery();
            
            DefaultTableModel newModel = buildTableModel(rs);
            
            Vector<String> columnNames = new Vector<>();
            for (int i = 0; i < newModel.getColumnCount(); i++) {
                columnNames.add(newModel.getColumnName(i));
            }
            
            tableModel.setDataVector(newModel.getDataVector(), columnNames);

            // Ajustar ancho de columnas
            reportTable.getColumnModel().getColumn(0).setPreferredWidth(100); // Fecha
            reportTable.getColumnModel().getColumn(1).setPreferredWidth(60);  // Factura
            reportTable.getColumnModel().getColumn(2).setPreferredWidth(180); // Cliente
            reportTable.getColumnModel().getColumn(3).setPreferredWidth(180); // Trabajadora
            reportTable.getColumnModel().getColumn(4).setPreferredWidth(180); // Servicio
            reportTable.getColumnModel().getColumn(12).setPreferredWidth(400); // Pagos_Detallados

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al generar el reporte: " + e.getMessage(), "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    // Utilidad para construir el TableModel desde un ResultSet
    public static DefaultTableModel buildTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        Vector<String> columnNames = new Vector<>();
        int columnCount = metaData.getColumnCount();
        for (int column = 1; column <= columnCount; column++) {
            columnNames.add(metaData.getColumnName(column));
        }
        Vector<Vector<Object>> data = new Vector<>();
        while (rs.next()) {
            Vector<Object> vector = new Vector<>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                vector.add(rs.getObject(columnIndex));
            }
            data.add(vector);
        }
        return new DefaultTableModel(data, columnNames);
    }

    private void exportToCSV() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para exportar. Genere un reporte primero.", "Reporte Vacío", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Reporte CSV");
        fileChooser.setSelectedFile(new File("reporte_financiero.csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (FileWriter fw = new FileWriter(fileToSave);
                 BufferedWriter bw = new BufferedWriter(fw)) {

                // Escribir cabeceras
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    bw.write("\"" + tableModel.getColumnName(i) + "\"");
                    if (i < tableModel.getColumnCount() - 1) {
                        bw.write(",");
                    }
                }
                bw.newLine();

                // Escribir datos
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        Object value = tableModel.getValueAt(i, j);
                        String cellValue = (value != null) ? value.toString() : "";
                        // Reemplazar saltos de línea (de GROUP_CONCAT) por un caracter legible en CSV
                        cellValue = cellValue.replace("\n", " | "); 
                        bw.write("\"" + cellValue.replace("\"", "\"\"") + "\"");
                        if (j < tableModel.getColumnCount() - 1) {
                            bw.write(",");
                        }
                    }
                    bw.newLine();
                }

                JOptionPane.showMessageDialog(this, "Reporte exportado exitosamente a:\n" + fileToSave.getAbsolutePath(), "Exportación Completa", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al exportar a CSV: " + e.getMessage(), "Error de Exportación", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
}
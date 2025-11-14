package com.capelli.payroll;

import com.capelli.model.CuentaBancaria;
import com.capelli.model.Trabajadora;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public class PayrollWindow extends JFrame {

    private final JSpinner startDateSpinner;
    private final JSpinner endDateSpinner;
    private final DefaultTableModel tableModel;
    private final JTable payrollTable;
    private final PayrollService payrollService;
    private final DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");

    public PayrollWindow() {
        super("Cálculo de Nómina y Comisiones");
        this.payrollService = new PayrollService();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel de Controles
        JPanel controlsPanel = new JPanel(new MigLayout("fillx", "[right]10[grow,fill]20[right]10[grow,fill]20[grow,fill]", ""));
        controlsPanel.setBorder(new TitledBorder("Seleccionar Rango de Fechas"));

        startDateSpinner = new JSpinner(new SpinnerDateModel());
        startDateSpinner.setEditor(new JSpinner.DateEditor(startDateSpinner, "dd/MM/yyyy"));
        
        endDateSpinner = new JSpinner(new SpinnerDateModel());
        endDateSpinner.setEditor(new JSpinner.DateEditor(endDateSpinner, "dd/MM/yyyy"));

        JButton calculateButton = new JButton("Calcular Nómina");
        
        controlsPanel.add(new JLabel("Fecha Inicio:"));
        controlsPanel.add(startDateSpinner, "sg date");
        controlsPanel.add(new JLabel("Fecha Fin:"));
        controlsPanel.add(endDateSpinner, "sg date");
        controlsPanel.add(calculateButton, "sg button");

        // Panel de Tabla
        // ===== INICIO DE MODIFICACIÓN: Añadida columna "Monto Efectivo $" =====
        String[] columnNames = {"Trabajadora", "Monto Pagar (Banco)", "Monto Efectivo $", "Banco Principal", "No. Cuenta", "C.I.", "Teléfono"};
        // ===== FIN DE MODIFICACIÓN =====
        
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        payrollTable = new JTable(tableModel);
        payrollTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        payrollTable.setRowHeight(24);

        mainPanel.add(controlsPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(payrollTable), BorderLayout.CENTER);

        add(mainPanel);

        // Listeners
        calculateButton.addActionListener(e -> calculatePayroll());
    }

    private void calculatePayroll() {
        Date startDate = (Date) startDateSpinner.getValue();
        Date endDate = (Date) endDateSpinner.getValue();

        LocalDate startLocalDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endLocalDate = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        if (startLocalDate.isAfter(endLocalDate)) {
            JOptionPane.showMessageDialog(this, "La fecha de inicio no puede ser posterior a la fecha de fin.", "Error de Fechas", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            List<PayrollResult> results = payrollService.calculatePayroll(startLocalDate, endLocalDate);
            
            tableModel.setRowCount(0); // Limpiar tabla

            for (PayrollResult result : results) {
                Trabajadora t = result.trabajadora();
                CuentaBancaria cb = result.primaryAccount();
                
                // ===== INICIO DE MODIFICACIÓN: Añadidos nuevos campos al agregar fila =====
                tableModel.addRow(new Object[]{
                    t.getNombreCompleto(),
                    currencyFormat.format(result.amountToPayBank()),
                    currencyFormat.format(result.amountToPayCash()),
                    (cb != null) ? cb.getBanco() : "N/A",
                    (cb != null) ? cb.getNumeroDeCuenta() : "N/A",
                    t.getCiCompleta(),
                    t.getTelefono()
                });
                // ===== FIN DE MODIFICACIÓN =====
            }

        } catch (SQLException | IOException e) {
            JOptionPane.showMessageDialog(this, "Error al calcular la nómina: " + e.getMessage(), "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
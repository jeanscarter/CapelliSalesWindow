package com.capelli.payroll;

import com.capelli.model.CuentaBancaria;
import com.capelli.model.Trabajadora;
import com.formdev.flatlaf.FlatLaf;
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
    
    // Labels para estadísticas
    private final JLabel lblTotalLavado;
    private final JLabel lblTotalFusio;
    private final JLabel lblTotalQuimicos;
    private final JLabel lblTotalProductos;

    public PayrollWindow() {
        super("Cálculo de Nómina y Comisiones");
        this.payrollService = new PayrollService();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 750);
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
        String[] columnNames = {"Trabajadora", "Monto Pagar (Banco)", "Monto Efectivo $", "Banco Principal", "No. Cuenta", "C.I.", "Teléfono"};
        
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        payrollTable = new JTable(tableModel);
        payrollTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        payrollTable.setRowHeight(24);

        // Panel Inferior para Totales Específicos
        JPanel statsPanel = new JPanel(new MigLayout("fillx, insets 10", "[center]20[center]20[center]20[center]", ""));
        // Cambio de título para reflejar que es Ganancia para la Empresa
        statsPanel.setBorder(new TitledBorder("Ganancia Neta Empresa (Precio Venta - Comisión)"));
        
        // --- LÓGICA DE COLORES SEGÚN MODO OSCURO ---
        boolean isDark = FlatLaf.isLafDark();

        Color colorLavado   = isDark ? new Color(100, 180, 255) : new Color(0, 102, 204); // Azul
        Color colorFusio    = isDark ? new Color(100, 255, 100) : new Color(0, 153, 51);  // Verde
        Color colorQuimicos = isDark ? new Color(255, 100, 100) : new Color(204, 0, 0);   // Rojo
        Color colorProduct  = isDark ? new Color(220, 130, 255) : new Color(153, 0, 153); // Lila/Morado

        lblTotalLavado = new JLabel("Lavado: $ 0.00");
        lblTotalLavado.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotalLavado.setForeground(colorLavado);
        
        lblTotalFusio = new JLabel("Fusio Dose: $ 0.00");
        lblTotalFusio.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotalFusio.setForeground(colorFusio);
        
        lblTotalQuimicos = new JLabel("Químicos: $ 0.00");
        lblTotalQuimicos.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotalQuimicos.setForeground(colorQuimicos);
        
        lblTotalProductos = new JLabel("Productos: $ 0.00");
        lblTotalProductos.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotalProductos.setForeground(colorProduct);
        
        statsPanel.add(lblTotalLavado);
        statsPanel.add(lblTotalFusio);
        statsPanel.add(lblTotalQuimicos);
        statsPanel.add(lblTotalProductos);

        mainPanel.add(controlsPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(payrollTable), BorderLayout.CENTER);
        mainPanel.add(statsPanel, BorderLayout.SOUTH);

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
            PayrollService.PayrollCalculation calc = payrollService.calculatePayroll(startLocalDate, endLocalDate);
            
            tableModel.setRowCount(0); 

            for (PayrollResult result : calc.results()) {
                Trabajadora t = result.trabajadora();
                CuentaBancaria cb = result.primaryAccount();
                
                tableModel.addRow(new Object[]{
                    t.getNombreCompleto(),
                    currencyFormat.format(result.amountToPayBank()),
                    currencyFormat.format(result.amountToPayCash()),
                    (cb != null) ? cb.getBanco() : "N/A",
                    (cb != null) ? cb.getNumeroDeCuenta() : "N/A",
                    t.getCiCompleta(),
                    t.getTelefono()
                });
            }
            
            // Actualizar etiquetas inferiores con las GANANCIAS
            lblTotalLavado.setText("Lavado: $ " + currencyFormat.format(calc.gananciaLavado()));
            lblTotalFusio.setText("Fusio Dose: $ " + currencyFormat.format(calc.gananciaFusio()));
            lblTotalQuimicos.setText("Químicos: $ " + currencyFormat.format(calc.gananciaQuimicos()));
            lblTotalProductos.setText("Productos: $ " + currencyFormat.format(calc.gananciaProductos()));

        } catch (SQLException | IOException e) {
            JOptionPane.showMessageDialog(this, "Error al calcular la nómina: " + e.getMessage(), "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
package com.capelli.capellisaleswindow;

import com.capelli.validation.ValidationHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class MultiPagoPanel extends JPanel {

    private final List<String> metodosPagoBs = new ArrayList<>(Arrays.asList("TD", "TC", "Pago Movil", "Efectivo Bs"));
    private final List<String> metodosPagoUsd = new ArrayList<>(Arrays.asList("Efectivo $", "Transferencia"));
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private final Supplier<Double> tasaBcvSupplier;

    private JComboBox<String> pagoComboBox;
    private JRadioButton monedaBs, monedaDolar;
    private JTextField montoField;
    private JButton agregarPagoBtn;
    private JButton eliminarPagoBtn;
    private JTable pagosTable;
    private DefaultTableModel pagosTableModel;
    private JLabel totalPagadoLabel;
    private JLabel totalFaltanteLabel;

    private JPanel pagoMovilPanel;
    private JRadioButton pagoMovilCapelliRadio, pagoMovilRosaRadio;
    private ButtonGroup pagoMovilDestinoGroup;

    private JPanel transferenciaUsdPanel;
    private JRadioButton transferenciaHotmailRadio, transferenciaGmailRadio, transferenciaIngridRadio;
    private ButtonGroup transferenciaUsdDestinoGroup;
    private JTextField referenciaUsdField;

    private final List<Pago> pagosAgregados = new ArrayList<>();
    private double totalVentaUSD = 0.0;

    public MultiPagoPanel(Supplier<Double> tasaBcvSupplier) {
        this.tasaBcvSupplier = tasaBcvSupplier;
        initComponents();
        layoutComponents();
        addListeners();
    }

    private void initComponents() {
        monedaBs = new JRadioButton("Bs", true);
        monedaDolar = new JRadioButton("$");
        ButtonGroup monedaGroup = new ButtonGroup();
        monedaGroup.add(monedaBs);
        monedaGroup.add(monedaDolar);

        pagoComboBox = new JComboBox<>(metodosPagoBs.toArray(new String[0]));
        montoField = new JTextField("0.00");
        ((AbstractDocument) montoField.getDocument()).setDocumentFilter(new NumericFilter());

        agregarPagoBtn = new JButton("Agregar Pago");
        eliminarPagoBtn = new JButton("Eliminar Pago");

        String[] columnNames = {"Método", "Monto", "Monto ($)"};
        pagosTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        pagosTable = new JTable(pagosTableModel);

        totalPagadoLabel = new JLabel("$ 0.00");
        totalFaltanteLabel = new JLabel("$ 0.00");
        totalFaltanteLabel.setForeground(Color.RED);

        // Paneles de destino
        pagoMovilPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pagoMovilCapelliRadio = new JRadioButton("Capelli", true);
        pagoMovilRosaRadio = new JRadioButton("Rosa");
        pagoMovilDestinoGroup = new ButtonGroup();
        pagoMovilDestinoGroup.add(pagoMovilCapelliRadio);
        pagoMovilDestinoGroup.add(pagoMovilRosaRadio);
        pagoMovilPanel.add(new JLabel("Destino:"));
        pagoMovilPanel.add(pagoMovilCapelliRadio);
        pagoMovilPanel.add(pagoMovilRosaRadio);

        transferenciaUsdPanel = new JPanel(new MigLayout("wrap 2, insets 0", "[right]10[grow,fill]"));
        transferenciaHotmailRadio = new JRadioButton("@hotmail", true);
        transferenciaGmailRadio = new JRadioButton("@Gmail");
        transferenciaIngridRadio = new JRadioButton("Ingrid");
        transferenciaUsdDestinoGroup = new ButtonGroup();
        transferenciaUsdDestinoGroup.add(transferenciaHotmailRadio);
        transferenciaUsdDestinoGroup.add(transferenciaGmailRadio);
        transferenciaUsdDestinoGroup.add(transferenciaIngridRadio);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        radioPanel.add(transferenciaHotmailRadio);
        radioPanel.add(transferenciaGmailRadio);
        radioPanel.add(transferenciaIngridRadio);
        transferenciaUsdPanel.add(new JLabel("Destino:"));
        transferenciaUsdPanel.add(radioPanel, "growx");

        referenciaUsdField = new JTextField();
        transferenciaUsdPanel.add(new JLabel("Ref:"));
        transferenciaUsdPanel.add(referenciaUsdField, "growx");
    }

    private void layoutComponents() {
        setLayout(new MigLayout("fillx, wrap 2", "[right]10[grow,fill]", ""));
        setBorder(new TitledBorder("Gestión de Pagos Múltiples"));

        // Fila 1: Moneda
        JPanel monedaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        monedaPanel.add(monedaDolar);
        monedaPanel.add(monedaBs);
        add(new JLabel("Moneda de Pago:"));
        add(monedaPanel);

        // Fila 2: Método
        add(new JLabel("Método:"));
        add(pagoComboBox);

        // Fila 3: Paneles Opcionales (Destino/Ref)
        add(pagoMovilPanel, "span 2, growx");
        add(transferenciaUsdPanel, "span 2, growx");
        pagoMovilPanel.setVisible(true);
        transferenciaUsdPanel.setVisible(false);

        // Fila 4: Monto
        add(new JLabel("Monto:"));
        add(montoField);

        // Fila 5: Botón Agregar
        add(agregarPagoBtn, "span 2, right, gaptop 10");

        // Fila 6: Tabla de Pagos
        add(new JScrollPane(pagosTable), "span 2, grow, h 100!, gaptop 10");

        // Fila 7: Botón Eliminar
        add(eliminarPagoBtn, "span 2, right");

        // Fila 8: Resumen
        JPanel resumenPanel = new JPanel(new MigLayout("fillx, wrap 2", "[right]10[grow,fill]"));
        resumenPanel.add(new JLabel("Total Pagado:"));
        resumenPanel.add(totalPagadoLabel, "growx");
        resumenPanel.add(new JLabel("Total Faltante:"));
        resumenPanel.add(totalFaltanteLabel, "growx");

        add(resumenPanel, "span 2, growx, gaptop 10");
    }

    private void addListeners() {
        monedaBs.addActionListener(e -> actualizarMetodosPago());
        monedaDolar.addActionListener(e -> actualizarMetodosPago());
        pagoComboBox.addActionListener(e -> actualizarPanelesOpcionales());
        agregarPagoBtn.addActionListener(e -> agregarPago());
        eliminarPagoBtn.addActionListener(e -> eliminarPago());

        // Listener para auto-completar monto faltante
        montoField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (Double.parseDouble(montoField.getText().replace(",", ".")) == 0.0) {
                    double faltante = totalVentaUSD - getTotalPagadoUSD();
                    if (faltante > 0) {
                        if (monedaBs.isSelected()) {
                            montoField.setText(df.format(faltante * getTasaBcv()).replace(",", "."));
                        } else {
                            montoField.setText(df.format(faltante).replace(",", "."));
                        }
                    }
                }
            }
        });
    }

    private void actualizarMetodosPago() {
        pagoComboBox.removeAllItems();
        if (monedaBs.isSelected()) {
            metodosPagoBs.forEach(pagoComboBox::addItem);
        } else {
            metodosPagoUsd.forEach(pagoComboBox::addItem);
        }
        actualizarPanelesOpcionales();
    }

    private void actualizarPanelesOpcionales() {
        String selectedMethod = (String) pagoComboBox.getSelectedItem();
        boolean esBs = monedaBs.isSelected();
        pagoMovilPanel.setVisible(esBs && "Pago Movil".equals(selectedMethod));
        transferenciaUsdPanel.setVisible(!esBs && "Transferencia".equals(selectedMethod));
    }

    private void agregarPago() {
        String metodo = (String) pagoComboBox.getSelectedItem();
        if (metodo == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un método de pago.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double monto;
        try {
            monto = Double.parseDouble(montoField.getText().replace(",", "."));
            if (monto <= 0) {
                JOptionPane.showMessageDialog(this, "El monto debe ser positivo.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Monto inválido.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String moneda = monedaDolar.isSelected() ? "$" : "Bs";
        double tasa = getTasaBcv();
        double montoUSD = moneda.equals("$") ? monto : monto / tasa;

        String destino = null;
        String ref = null;

        if (pagoMovilPanel.isVisible()) {
            destino = pagoMovilRosaRadio.isSelected() ? "Rosa" : "Capelli";
        } else if (transferenciaUsdPanel.isVisible()) {
            if (transferenciaHotmailRadio.isSelected()) destino = "@hotmail";
            else if (transferenciaGmailRadio.isSelected()) destino = "@Gmail";
            else if (transferenciaIngridRadio.isSelected()) destino = "Ingrid";
            ref = referenciaUsdField.getText().trim();
            if (ref.isEmpty()) {
                JOptionPane.showMessageDialog(this, "La referencia es obligatoria para Transferencia $.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        Pago nuevoPago = new Pago(metodo, moneda, monto, montoUSD, tasa, destino, ref);
        pagosAgregados.add(nuevoPago);
        
        String montoDisplay = df.format(monto) + " " + moneda;
        pagosTableModel.addRow(new Object[]{metodo, montoDisplay, df.format(montoUSD)});

        actualizarTotalesPagos();
        limpiarCamposPago();
    }

    private void eliminarPago() {
        int selectedRow = pagosTable.getSelectedRow();
        if (selectedRow >= 0) {
            pagosAgregados.remove(selectedRow);
            pagosTableModel.removeRow(selectedRow);
            actualizarTotalesPagos();
        } else {
            JOptionPane.showMessageDialog(this, "Seleccione un pago de la tabla para eliminar.", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void limpiarCamposPago() {
        montoField.setText("0.00");
        referenciaUsdField.setText("");
        pagoComboBox.requestFocusInWindow();
    }

    public void actualizarTotalesPagos() {
        double totalPagado = getTotalPagadoUSD();
        double faltante = totalVentaUSD - totalPagado;

        totalPagadoLabel.setText(df.format(totalPagado) + " $");
        totalFaltanteLabel.setText(df.format(faltante) + " $");

        if (faltante <= 0.01) { // Usar tolerancia
            totalFaltanteLabel.setForeground(Color.GREEN);
            totalFaltanteLabel.setText(df.format(faltante) + " $ (Vuelto: " + df.format(Math.abs(faltante)) + ")");
        } else {
            totalFaltanteLabel.setForeground(Color.RED);
        }
    }

    public double getTotalPagadoUSD() {
        return pagosAgregados.stream().mapToDouble(Pago::montoUSD).sum();
    }

    public void setTotalVentaUSD(double totalVentaUSD) {
        this.totalVentaUSD = totalVentaUSD;
        actualizarTotalesPagos();
    }

    public List<Pago> getPagos() {
        return new ArrayList<>(pagosAgregados);
    }

    public void limpiar() {
        pagosAgregados.clear();
        pagosTableModel.setRowCount(0);
        limpiarCamposPago();
        setTotalVentaUSD(0.0);
    }
    
    private double getTasaBcv() {
        return tasaBcvSupplier.get();
    }
}
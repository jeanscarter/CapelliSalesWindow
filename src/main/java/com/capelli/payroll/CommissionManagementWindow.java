package com.capelli.payroll;

import com.capelli.database.CommissionRuleDAO;
import com.capelli.database.TrabajadoraDAO;
import com.capelli.model.CommissionRule;
import com.capelli.model.Trabajadora;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CommissionManagementWindow extends JFrame {

    private final CommissionRuleDAO dao;
    private final DefaultTableModel tableModel;
    private final JTable rulesTable;
    private final JComboBox<TrabajadoraWrapper> trabajadoraComboBox;
    private final JTextField categoryField;
    private final JTextField rateField;
    
    private CommissionRule currentRule = null;
    private List<Trabajadora> trabajadorasList = new ArrayList<>();

    public CommissionManagementWindow() {
        super("Gestión de Reglas de Comisión");
        this.dao = new CommissionRuleDAO();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Formulario
        JPanel formPanel = new JPanel(new MigLayout("wrap 2, fillx", "[right]10[grow,fill]"));
        formPanel.setBorder(new TitledBorder("Datos de la Regla"));

        trabajadoraComboBox = new JComboBox<>();
        categoryField = new JTextField();
        rateField = new JTextField("0.0");

        formPanel.add(new JLabel("Trabajadora:"));
        formPanel.add(trabajadoraComboBox, "growx");

        formPanel.add(new JLabel("Categoría de Servicio:"));
        formPanel.add(categoryField, "growx");

        formPanel.add(new JLabel("Tasa (ej: 0.70 para 70%):"));
        formPanel.add(rateField, "growx");

        // Botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Agregar");
        JButton updateButton = new JButton("Actualizar");
        JButton deleteButton = new JButton("Eliminar");
        JButton clearButton = new JButton("Limpiar");
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(formPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Tabla
        String[] columnNames = {"ID", "Trabajadora", "Categoría", "Tasa"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        rulesTable = new JTable(tableModel);
        rulesTable.getColumnModel().getColumn(0).setMaxWidth(50); // ID column width

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(rulesTable), BorderLayout.CENTER);

        add(mainPanel);

        // Listeners
        addButton.addActionListener(e -> saveRule(true));
        updateButton.addActionListener(e -> saveRule(false));
        deleteButton.addActionListener(e -> deleteRule());
        clearButton.addActionListener(e -> clearFields());

        rulesTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int selectedRow = rulesTable.getSelectedRow();
                if (selectedRow >= 0) {
                    try {
                        int ruleId = Integer.parseInt(tableModel.getValueAt(selectedRow, 0).toString());
                        loadRuleIntoForm(ruleId);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(CommissionManagementWindow.this, "Error al seleccionar la regla.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        loadTrabajadoras();
        loadRules();
    }

    private void loadTrabajadoras() {
        try {
            TrabajadoraDAO trabajadoraDAO = new TrabajadoraDAO();
            trabajadorasList = trabajadoraDAO.getAll();
            trabajadoraComboBox.removeAllItems();
            for (Trabajadora t : trabajadorasList) {
                trabajadoraComboBox.addItem(new TrabajadoraWrapper(t));
            }
        } catch (SQLException | IOException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar trabajadoras: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadRules() {
        try {
            tableModel.setRowCount(0);
            List<CommissionRule> rules = dao.getAll();
            DecimalFormat df = new DecimalFormat("0.00%");
            for (CommissionRule rule : rules) {
                tableModel.addRow(new Object[]{
                    rule.getRule_id(),
                    rule.getTrabajadora_name(),
                    rule.getService_category(),
                    df.format(rule.getCommission_rate())
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar reglas: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadRuleIntoForm(int ruleId) {
         try {
            // Re-buscamos de la DB para asegurar datos frescos
            List<CommissionRule> rules = dao.getAll();
            currentRule = rules.stream().filter(r -> r.getRule_id() == ruleId).findFirst().orElse(null);

            if (currentRule != null) {
                // Seleccionar la trabajadora correcta en el ComboBox
                for (int i = 0; i < trabajadoraComboBox.getItemCount(); i++) {
                    if (trabajadoraComboBox.getItemAt(i).getId() == currentRule.getTrabajadora_id()) {
                        trabajadoraComboBox.setSelectedIndex(i);
                        break;
                    }
                }
                categoryField.setText(currentRule.getService_category());
                rateField.setText(String.valueOf(currentRule.getCommission_rate()));
            }
        } catch (SQLException e) {
             JOptionPane.showMessageDialog(this, "Error al cargar regla: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveRule(boolean isNew) {
        TrabajadoraWrapper selectedWrapper = (TrabajadoraWrapper) trabajadoraComboBox.getSelectedItem();
        String category = categoryField.getText().trim();
        String rateStr = rateField.getText().trim().replace(',', '.');

        if (selectedWrapper == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar una trabajadora.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (category.isEmpty()) {
            JOptionPane.showMessageDialog(this, "La categoría es obligatoria.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double rate;
        try {
            rate = Double.parseDouble(rateStr);
            if (rate < 0.0 || rate > 1.0) {
                JOptionPane.showMessageDialog(this, "La tasa debe ser un valor entre 0.0 y 1.0 (ej: 0.70).", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "La tasa es inválida. Use punto (.) para decimales.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (isNew) {
            currentRule = new CommissionRule(0, selectedWrapper.getId(), category, rate);
        } else {
            if (currentRule == null) {
                JOptionPane.showMessageDialog(this, "Seleccione una regla de la tabla para actualizar.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            currentRule.setTrabajadora_id(selectedWrapper.getId());
            currentRule.setService_category(category);
            currentRule.setCommission_rate(rate);
        }

        try {
            dao.save(currentRule);
            JOptionPane.showMessageDialog(this, "Regla guardada con éxito.");
            loadRules();
            clearFields();
        } catch (SQLException e) {
             if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                JOptionPane.showMessageDialog(this, "Error: Ya existe una regla para esa trabajadora y categoría.", "Error de Duplicado", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error al guardar la regla: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteRule() {
        if (currentRule == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una regla para eliminar.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int response = JOptionPane.showConfirmDialog(this, "¿Está seguro de que desea eliminar esta regla?", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            try {
                dao.delete(currentRule.getRule_id());
                JOptionPane.showMessageDialog(this, "Regla eliminada con éxito.");
                loadRules();
                clearFields();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al eliminar la regla: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearFields() {
        currentRule = null;
        trabajadoraComboBox.setSelectedIndex(0);
        categoryField.setText("");
        rateField.setText("0.0");
        rulesTable.clearSelection();
    }

    // Clase wrapper para mostrar Trabajadoras en JComboBox
    private static class TrabajadoraWrapper {
        private final Trabajadora trabajadora;

        public TrabajadoraWrapper(Trabajadora trabajadora) {
            this.trabajadora = trabajadora;
        }

        public int getId() {
            return trabajadora.getId();
        }

        @Override
        public String toString() {
            return trabajadora.getNombreCompleto();
        }
    }
}
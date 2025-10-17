package com.capelli.ui;

import com.capelli.database.TrabajadoraDAO;
import com.capelli.model.CuentaBancaria;
import com.capelli.model.Trabajadora;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final boolean isDarkMode;

    private final TrabajadoraDAO trabajadoraDAO;
    private List<Trabajadora> listaTrabajadoras; 

    public MainPanel(boolean isDarkMode) {
        this.isDarkMode = isDarkMode;
        this.trabajadoraDAO = new TrabajadoraDAO();
        this.listaTrabajadoras = new ArrayList<>();

        setLayout(new BorderLayout(0, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel headerPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow,fill]rel[]rel[]rel[]", "[]"));
        JTextField searchField = new JTextField();
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search...");
        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSVGIcon("icons/search.svg"));
        JButton btnCreate = new JButton("Registrar");
        JButton btnEdit = new JButton("Editar");
        JButton btnDelete = new JButton("Eliminar");
        headerPanel.add(searchField, "growx");
        headerPanel.add(btnCreate);
        headerPanel.add(btnEdit);
        headerPanel.add(btnDelete);

        String[] columnNames = {"ID", "FOTO", "NOMBRES", "APELLIDOS", "C.I.", "BANCO (PRINCIPAL)", "TIPO DE CUENTA", "# DE CUENTA", "TELÉFONO", "CORREO"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(50);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(1).setCellRenderer(new ImageRenderer());
        table.getColumnModel().getColumn(1).setMaxWidth(60);
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        searchField.addActionListener(e -> {
            String text = searchField.getText();
            sorter.setRowFilter(text.trim().isEmpty() ? null : RowFilter.regexFilter("(?i)" + text));
        });

        JScrollPane scrollPane = new JScrollPane(table);
        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        btnCreate.addActionListener(e -> createTrabajadora());
        btnEdit.addActionListener(e -> editTrabajadora());
        btnDelete.addActionListener(e -> deleteTrabajadora());
        refreshTableData();
    }

    private void refreshTableData() {
        try {
            tableModel.setRowCount(0);
            listaTrabajadoras = trabajadoraDAO.getAll();
            for (Trabajadora t : listaTrabajadoras) {
                addTrabajadoraToTable(t);
            }
        } catch (SQLException | IOException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar los datos de las trabajadoras: " + e.getMessage(), "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void createTrabajadora() {
        TrabajadoraDialog dialog = new TrabajadoraDialog((Frame) SwingUtilities.getWindowAncestor(this), null, isDarkMode);
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            Trabajadora nueva = dialog.getTrabajadora();
            try {
                trabajadoraDAO.save(nueva);
                JOptionPane.showMessageDialog(this, "Trabajadora registrada con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                refreshTableData();
            } catch (SQLException | IOException e) {
                JOptionPane.showMessageDialog(this, "Error al guardar la trabajadora: " + e.getMessage(), "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void editTrabajadora() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione una trabajadora para editar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        int trabajadoraId = (int) tableModel.getValueAt(modelRow, 0);
        Trabajadora trabajadoraAEditar = listaTrabajadoras.stream()
                .filter(t -> t.getId() == trabajadoraId)
                .findFirst()
                .orElse(null);

        if (trabajadoraAEditar != null) {
            TrabajadoraDialog dialog = new TrabajadoraDialog((Frame) SwingUtilities.getWindowAncestor(this), trabajadoraAEditar, isDarkMode);
            dialog.setVisible(true);

            if (dialog.isSaved()) {
                try {
                    trabajadoraDAO.save(trabajadoraAEditar);
                    JOptionPane.showMessageDialog(this, "Trabajadora actualizada con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    refreshTableData();
                } catch (SQLException | IOException e) {
                    JOptionPane.showMessageDialog(this, "Error al actualizar la trabajadora: " + e.getMessage(), "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
    }

    private void deleteTrabajadora() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione una trabajadora para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "¿Está seguro de que desea eliminar a esta trabajadora?\nEsta acción es irreversible.", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            int modelRow = table.convertRowIndexToModel(selectedRow);
            int trabajadoraId = (int) tableModel.getValueAt(modelRow, 0);
            
            try {
                trabajadoraDAO.delete(trabajadoraId);
                JOptionPane.showMessageDialog(this, "Trabajadora eliminada con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                refreshTableData();
            } catch (SQLException e) {
                 JOptionPane.showMessageDialog(this, "Error al eliminar la trabajadora: " + e.getMessage(), "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
                 e.printStackTrace();
            }
        }
    }

    private void addTrabajadoraToTable(Trabajadora t) {
        Optional<CuentaBancaria> cuentaPrincipal = t.getCuentaPrincipal();
        tableModel.addRow(new Object[]{
                t.getId(),
                t.getFoto(),
                t.getNombres(),
                t.getApellidos(),
                t.getCiCompleta(),
                cuentaPrincipal.map(CuentaBancaria::getBanco).orElse("N/A"),
                cuentaPrincipal.map(CuentaBancaria::getTipoDeCuenta).orElse("N/A"),
                cuentaPrincipal.map(CuentaBancaria::getNumeroDeCuenta).orElse("N/A"),
                t.getTelefono(),
                t.getCorreoElectronico()
        });
    }

    static class ImageRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = new JLabel();
            if (value instanceof ImageIcon) {
                ImageIcon icon = (ImageIcon) value;
                Image scaledImage = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                label.setIcon(new ImageIcon(scaledImage));
                label.setHorizontalAlignment(CENTER);
            }
            return label;
        }
    }
}
package com.capelli.clientmanagementwindow;

import com.capelli.database.Database;
import com.capelli.validation.ClienteValidator;
import com.capelli.validation.CommonValidators;
import com.capelli.validation.ValidationHelper;
import com.capelli.validation.ValidationResult;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import net.miginfocom.swing.MigLayout;

public class ClientManagementWindow extends JFrame {
    
    private static final Logger LOGGER = Logger.getLogger(ClientManagementWindow.class.getName());

    private JTextField clienteField, cedulaField, direccionField, telefonoField, tipoExtensionesField;
    private JFormattedTextField fechaCumpleañosField, fechaUltimoTinteField, fechaUltimoQuimicoField, fechaUltimaKeratinaField, fechaUltimoMantField;
    private JButton addButton, updateButton, deleteButton, clearButton;
    private JTable clientTable;
    private DefaultTableModel tableModel;
    
   
    private JRadioButton cortoRadioButton, medianoRadioButton, largoRadioButton;
    private ButtonGroup hairTypeGroup;
   

    public ClientManagementWindow() {
        setTitle("Módulo de Clientes - Capelli");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        try {
            setIconImage(ImageIO.read(getClass().getResource("/com/capelli/capellisaleswindow/image/Logo.png")));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Error al cargar el icono de la ventana: " + e.getMessage());
        }

        initComponents();
        layoutComponents();
        addListeners();
        setupRealtimeValidation();
        setupKeyBindings();
        loadClients();
    }

    public ClientManagementWindow(String cedula) {
        this();
        cedulaField.setText(cedula);
        cedulaField.setEditable(false);
        clienteField.requestFocusInWindow();
    }

    private void initComponents() {
        clienteField = new JTextField();
        cedulaField = new JTextField();
        direccionField = new JTextField();
        telefonoField = new JTextField();
        tipoExtensionesField = new JTextField();

        try {
            MaskFormatter dateFormatter = new MaskFormatter("##/##/####");
            dateFormatter.setPlaceholderCharacter('_');
            fechaCumpleañosField = new JFormattedTextField(dateFormatter);
            fechaUltimoTinteField = new JFormattedTextField(dateFormatter);
            fechaUltimoQuimicoField = new JFormattedTextField(dateFormatter);
            fechaUltimaKeratinaField = new JFormattedTextField(dateFormatter);
            fechaUltimoMantField = new JFormattedTextField(dateFormatter);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        addButton = new JButton("Agregar (F2)");
        updateButton = new JButton("Actualizar (F3)");
        deleteButton = new JButton("Eliminar (F4)");
        clearButton = new JButton("Limpiar (F5)");
        
     
        cortoRadioButton = new JRadioButton("Corto");
        medianoRadioButton = new JRadioButton("Mediano");
        largoRadioButton = new JRadioButton("Largo");
        hairTypeGroup = new ButtonGroup();
        hairTypeGroup.add(cortoRadioButton);
        hairTypeGroup.add(medianoRadioButton);
        hairTypeGroup.add(largoRadioButton);
   

        
        String[] columnNames = {"Cliente", "Cédula", "Dirección", "Teléfono", "Tipo Cabello", "Cumpleaños", "Últ. Tinte", "Últ. Químico", "Últ. Keratina", "Extensiones", "Últ. Mant."};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        clientTable = new JTable(tableModel);
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 15", "[grow]", "[][grow]"));
        JPanel formPanel = new JPanel(new MigLayout("wrap 4, fillx, insets 10", "[right]10[grow, fill]20[right]10[grow, fill]", "[]10[]10[]10[]10[]10[]"));
        
        formPanel.add(new JLabel("Cliente:"));
        formPanel.add(clienteField, "span 3, growx");

        formPanel.add(new JLabel("Cédula:"));
        formPanel.add(cedulaField, "growx");
        formPanel.add(new JLabel("Teléfono:"));
        formPanel.add(telefonoField, "growx");

        formPanel.add(new JLabel("Dirección:"));
        formPanel.add(direccionField, "span 3, growx");
        
      
        JPanel hairPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        hairPanel.setBorder(new TitledBorder("Tipo de Cabello"));
        hairPanel.add(cortoRadioButton);
        hairPanel.add(medianoRadioButton);
        hairPanel.add(largoRadioButton);
        formPanel.add(hairPanel, "span 4, growx");
       

        formPanel.add(new JLabel("Fecha Cumpleaños:"));
        formPanel.add(fechaCumpleañosField, "growx");
        formPanel.add(new JLabel("Fecha Último Tinte:"));
        formPanel.add(fechaUltimoTinteField, "growx");

        formPanel.add(new JLabel("Fecha Último Químico:"));
        formPanel.add(fechaUltimoQuimicoField, "growx");
        formPanel.add(new JLabel("Fecha Última Keratina:"));
        formPanel.add(fechaUltimaKeratinaField, "growx");

        formPanel.add(new JLabel("Tipo de Extensiones:"));
        formPanel.add(tipoExtensionesField, "growx");
        formPanel.add(new JLabel("Fecha Último Mant.:"));
        formPanel.add(fechaUltimoMantField, "growx");

        JPanel buttonPanel = new JPanel(new MigLayout("", "push[center]10[center]10[center]10[center]push"));
        buttonPanel.add(addButton, "sg btn");
        buttonPanel.add(updateButton, "sg btn");
        buttonPanel.add(deleteButton, "sg btn");
        buttonPanel.add(clearButton, "sg btn");
        formPanel.add(buttonPanel, "span 4, growx, gaptop 15");

        mainPanel.add(formPanel, "north");
        mainPanel.add(new JScrollPane(clientTable), "grow");

        add(mainPanel, BorderLayout.CENTER);
    }

    private void addListeners() {
        addButton.addActionListener(e -> addClient());
        updateButton.addActionListener(e -> updateClient());
        deleteButton.addActionListener(e -> deleteClient());
        clearButton.addActionListener(e -> clearFields());

        clientTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int selectedRow = clientTable.getSelectedRow();
                if (selectedRow >= 0) {
                    cedulaField.setEditable(false);
                    clienteField.setText(tableModel.getValueAt(selectedRow, 0).toString());
                    cedulaField.setText(tableModel.getValueAt(selectedRow, 1).toString());
                    direccionField.setText(tableModel.getValueAt(selectedRow, 2).toString());
                    telefonoField.setText(tableModel.getValueAt(selectedRow, 3).toString());
                    
                   
                    Object hairTypeObj = tableModel.getValueAt(selectedRow, 4);
                    if (hairTypeObj != null) {
                        String hairType = hairTypeObj.toString();
                        if (hairType.equals("Corto")) {
                            cortoRadioButton.setSelected(true);
                        } else if (hairType.equals("Mediano")) {
                            medianoRadioButton.setSelected(true);
                        } else if (hairType.equals("Largo")) {
                            largoRadioButton.setSelected(true);
                        } else {
                            hairTypeGroup.clearSelection();
                        }
                    } else {
                        hairTypeGroup.clearSelection();
                    }
                   

                    fechaCumpleañosField.setText(tableModel.getValueAt(selectedRow, 5).toString());
                    fechaUltimoTinteField.setText(tableModel.getValueAt(selectedRow, 6).toString());
                    fechaUltimoQuimicoField.setText(tableModel.getValueAt(selectedRow, 7).toString());
                    fechaUltimaKeratinaField.setText(tableModel.getValueAt(selectedRow, 8).toString());
                    tipoExtensionesField.setText(tableModel.getValueAt(selectedRow, 9).toString());
                    fechaUltimoMantField.setText(tableModel.getValueAt(selectedRow, 10).toString());
                }
            }
        });
    }
    
    private void setupKeyBindings() {
        JPanel contentPane = (JPanel) this.getContentPane();
        contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F2"), "add_action");
        contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F3"), "update_action");
        contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F4"), "delete_action");
        contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F5"), "clear_action");

        contentPane.getActionMap().put("add_action", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { addButton.doClick(); }
        });
        contentPane.getActionMap().put("update_action", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { updateButton.doClick(); }
        });
        contentPane.getActionMap().put("delete_action", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { deleteButton.doClick(); }
        });
        contentPane.getActionMap().put("clear_action", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { clearButton.doClick(); }
        });
    }

    private void loadClients() {
        tableModel.setRowCount(0);
        
        String sql = "SELECT full_name, cedula, address, phone, hair_type, birth_date, last_dye_date, last_chemical_date, last_keratin_date, extensions_type, last_extensions_maintenance_date FROM clients";
        try (Connection conn = Database.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getString("full_name"),
                    rs.getString("cedula"),
                    rs.getString("address"),
                    rs.getString("phone"),
                    rs.getString("hair_type"), 
                    rs.getString("birth_date"),
                    rs.getString("last_dye_date"),
                    rs.getString("last_chemical_date"),
                    rs.getString("last_keratin_date"),
                    rs.getString("extensions_type"),
                    rs.getString("last_extensions_maintenance_date")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar los clientes: " + e.getMessage(), "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
        }
    }
    
  
    private String getSelectedHairType() {
        if (cortoRadioButton.isSelected()) return "Corto";
        if (medianoRadioButton.isSelected()) return "Mediano";
        if (largoRadioButton.isSelected()) return "Largo";
        return null;
    }
   

    private void addClient() {
        LOGGER.info("Intentando agregar nuevo cliente");

        // Validar datos del cliente
        ValidationResult result = ClienteValidator.validateCliente(
            clienteField.getText(),
            cedulaField.getText(),
            telefonoField.getText(),
            null // email si lo agregas en el futuro
        );

        // Validar dirección
        ValidationResult addressResult = ClienteValidator.validateAddress(
            direccionField.getText()
        );
        result.merge(addressResult);

        // Mostrar resultado de validación
        if (!ValidationHelper.validateAndShow(this, result, "Validación de Cliente")) {
            LOGGER.warning("Validación de cliente falló");
            return;
        }

        String sql = "INSERT INTO clients(full_name, cedula, address, phone, hair_type, " +
                     "birth_date, last_dye_date, last_chemical_date, last_keratin_date, " +
                     "extensions_type, last_extensions_maintenance_date) VALUES(?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = Database.connect(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, clienteField.getText());
            pstmt.setString(2, cedulaField.getText());
            pstmt.setString(3, direccionField.getText());
            pstmt.setString(4, telefonoField.getText());
            pstmt.setString(5, getSelectedHairType());
            pstmt.setString(6, fechaCumpleañosField.getText());
            pstmt.setString(7, fechaUltimoTinteField.getText());
            pstmt.setString(8, fechaUltimoQuimicoField.getText());
            pstmt.setString(9, fechaUltimaKeratinaField.getText());
            pstmt.setString(10, tipoExtensionesField.getText());
            pstmt.setString(11, fechaUltimoMantField.getText());
            pstmt.executeUpdate();

            LOGGER.info("Cliente agregado exitosamente");
            JOptionPane.showMessageDialog(this, "Cliente agregado con éxito.");
            loadClients();
            clearFields();

        } catch (SQLException e) {
            if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                LOGGER.warning("Intento de agregar cédula duplicada: " + cedulaField.getText());
                JOptionPane.showMessageDialog(this, 
                    "Error: La cédula '" + cedulaField.getText() + "' ya existe.", 
                    "Error de Duplicado", 
                    JOptionPane.ERROR_MESSAGE);
            } else {
                LOGGER.log(Level.SEVERE, "Error al agregar cliente", e);
                JOptionPane.showMessageDialog(this, 
                    "Error al agregar el cliente: " + e.getMessage(), 
                    "Error de Base de Datos", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateClient() {
        int selectedRow = clientTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, 
                "Por favor, seleccione un cliente de la tabla para actualizar.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        LOGGER.info("Intentando actualizar cliente");

        // Validar datos del cliente
        ValidationResult result = ClienteValidator.validateCliente(
            clienteField.getText(),
            cedulaField.getText(),
            telefonoField.getText(),
            null
        );

        ValidationResult addressResult = ClienteValidator.validateAddress(
            direccionField.getText()
        );
        result.merge(addressResult);

        // Mostrar resultado de validación
        if (!ValidationHelper.validateAndShow(this, result, "Validación de Cliente")) {
            LOGGER.warning("Validación de cliente falló");
            return;
        }

        String sql = "UPDATE clients SET full_name = ?, address = ?, phone = ?, hair_type = ?, " +
                     "birth_date = ?, last_dye_date = ?, last_chemical_date = ?, " +
                     "last_keratin_date = ?, extensions_type = ?, " +
                     "last_extensions_maintenance_date = ? WHERE cedula = ?";

        try (Connection conn = Database.connect(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, clienteField.getText());
            pstmt.setString(2, direccionField.getText());
            pstmt.setString(3, telefonoField.getText());
            pstmt.setString(4, getSelectedHairType());
            pstmt.setString(5, fechaCumpleañosField.getText());
            pstmt.setString(6, fechaUltimoTinteField.getText());
            pstmt.setString(7, fechaUltimoQuimicoField.getText());
            pstmt.setString(8, fechaUltimaKeratinaField.getText());
            pstmt.setString(9, tipoExtensionesField.getText());
            pstmt.setString(10, fechaUltimoMantField.getText());
            pstmt.setString(11, cedulaField.getText());

            pstmt.executeUpdate();

            LOGGER.info("Cliente actualizado exitosamente");
            JOptionPane.showMessageDialog(this, "Cliente actualizado con éxito.");
            loadClients();
            clearFields();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar cliente", e);
            JOptionPane.showMessageDialog(this, 
                "Error al actualizar el cliente: " + e.getMessage(), 
                "Error de Base de Datos", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteClient() {
        int selectedRow = clientTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un cliente de la tabla para eliminar.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "¿Está seguro de que desea eliminar a este cliente?", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM clients WHERE cedula = ?";
            try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, cedulaField.getText());
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Cliente eliminado con éxito.");
                loadClients();
                clearFields();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al eliminar el cliente: " + e.getMessage(), "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void setupRealtimeValidation() {
        // Validación de cédula en tiempo real
        cedulaField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateCedulaField(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateCedulaField(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validateCedulaField(); }
        });

        // Validación de teléfono en tiempo real
        telefonoField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validatePhoneField(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validatePhoneField(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validatePhoneField(); }
        });

        // Validación de nombre en tiempo real
        clienteField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateNameField(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateNameField(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validateNameField(); }
        });
    }

    private void validateCedulaField() {
        String cedula = cedulaField.getText().trim();
        if (!cedula.isEmpty()) {
            if (CommonValidators.isValidCedula(cedula)) {
                ValidationHelper.resetFieldBorder(cedulaField);
                ValidationHelper.removeErrorTooltip(cedulaField);
            } else {
                ValidationHelper.markFieldAsWarning(cedulaField);
                ValidationHelper.addErrorTooltip(cedulaField, 
                    "Formato esperado: V-12345678");
            }
        } else {
            ValidationHelper.resetFieldBorder(cedulaField);
        }
    }

    private void validatePhoneField() {
        String phone = telefonoField.getText().trim();
        if (!phone.isEmpty()) {
            if (CommonValidators.isValidPhone(phone)) {
                ValidationHelper.resetFieldBorder(telefonoField);
                ValidationHelper.removeErrorTooltip(telefonoField);
            } else {
                ValidationHelper.markFieldAsWarning(telefonoField);
                ValidationHelper.addErrorTooltip(telefonoField, 
                    "Debe tener 10-11 dígitos");
            }
        } else {
            ValidationHelper.resetFieldBorder(telefonoField);
        }
    }

    private void validateNameField() {
        String name = clienteField.getText().trim();
        if (!name.isEmpty()) {
            if (name.length() >= 3 && name.length() <= 100) {
                ValidationHelper.resetFieldBorder(clienteField);
                ValidationHelper.removeErrorTooltip(clienteField);
            } else if (name.length() < 3) {
                ValidationHelper.markFieldAsWarning(clienteField);
                ValidationHelper.addErrorTooltip(clienteField, 
                    "El nombre debe tener al menos 3 caracteres");
            } else {
                ValidationHelper.markFieldAsWarning(clienteField);
                ValidationHelper.addErrorTooltip(clienteField, 
                    "El nombre no puede tener más de 100 caracteres");
            }
        } else {
            ValidationHelper.resetFieldBorder(clienteField);
        }
    }

    private void clearFields() {
        clienteField.setText("");
        if (cedulaField.isEditable()) {
            cedulaField.setText("");
        }
        direccionField.setText("");
        telefonoField.setText("");
        tipoExtensionesField.setText("");

        hairTypeGroup.clearSelection();

        String defaultDateValue = "__/__/____";
        fechaCumpleañosField.setText(defaultDateValue);
        fechaUltimoTinteField.setText(defaultDateValue);
        fechaUltimoQuimicoField.setText(defaultDateValue);
        fechaUltimaKeratinaField.setText(defaultDateValue);
        fechaUltimoMantField.setText(defaultDateValue);

        cedulaField.setEditable(true);
        clientTable.clearSelection();
    }

    public static void main(String[] args) {
        Database.initialize();
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        SwingUtilities.invokeLater(() -> {
            new ClientManagementWindow().setVisible(true);
        });
    }
}
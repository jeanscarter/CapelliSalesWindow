package com.capelli.ui;

import com.capelli.model.CuentaBancaria;
import com.capelli.model.Trabajadora;
import com.capelli.validation.CommonValidators;
import com.capelli.validation.TrabajadoraValidator;
import com.capelli.validation.ValidationHelper;
import com.capelli.validation.ValidationResult;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class TrabajadoraDialog extends JDialog {

    private static final Logger LOGGER = Logger.getLogger(TrabajadoraDialog.class.getName());

    private final Trabajadora trabajadora;
    private boolean saved = false;

    private final JTextField txtNombres;
    private final JTextField txtApellidos;
    private final JComboBox<String> cbTipoCi;
    private final JTextField txtNumeroCi;
    private final JTextField txtTelefono;
    private final JTextField txtCorreo;
    private final JLabel lblFoto;
    private ImageIcon fotoIcon;

    private final JComboBox<String> cbBanco;
    private final JComboBox<String> cbTipoCuenta;
    private final JTextField txtNumeroCuenta;
    private final JTable tblCuentas;
    private final DefaultTableModel cuentasTableModel;

    public TrabajadoraDialog(Frame owner, Trabajadora trabajadora, boolean isDarkMode) {
        super(owner, true);
        this.trabajadora = (trabajadora != null) ? trabajadora : new Trabajadora();
        if (trabajadora != null) {
            this.fotoIcon = trabajadora.getFoto();
        }

        setTitle(trabajadora == null ? "Crear Trabajadora" : "Editar Trabajadora");
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new MigLayout("wrap 2, fillx, insets 15", "[right]rel[grow,fill]", ""));
        JPanel cuentasPanel = new JPanel(new MigLayout("wrap 2, fill, insets 10", "[right]rel[grow,fill]", ""));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JPanel mainPanel = new JPanel(new BorderLayout(10,10));
        JPanel ciPanel = new JPanel(new MigLayout("insets 0, gap 0", "[][grow]", "[]"));

        if (isDarkMode) {
            Color darkBackground = UIManager.getColor("Panel.background");
            Color lightText = UIManager.getColor("Label.foreground");

            formPanel.setBackground(darkBackground);
            cuentasPanel.setBackground(darkBackground);
            ciPanel.setBackground(darkBackground);
            buttonPanel.setBackground(darkBackground);
            mainPanel.setBackground(darkBackground);

            TitledBorder border = new TitledBorder("Cuentas Bancarias");
            border.setTitleColor(lightText);
            cuentasPanel.setBorder(border);
        } else {
            Color lightBackground = Color.WHITE;
            formPanel.setBackground(lightBackground);
            cuentasPanel.setBackground(lightBackground);
            ciPanel.setBackground(lightBackground);
            buttonPanel.setBackground(lightBackground);
            mainPanel.setBackground(lightBackground);

            TitledBorder border = new TitledBorder("Cuentas Bancarias");
            border.setTitleColor(UIManager.getColor("TitledBorder.titleColor"));
            cuentasPanel.setBorder(border);
        }

        txtNombres = new JTextField();
        txtApellidos = new JTextField();
        cbTipoCi = new JComboBox<>(new String[]{"V", "E", "J", "G", "P"});
        txtNumeroCi = new JTextField();
        ((AbstractDocument) txtNumeroCi.getDocument()).setDocumentFilter(new NumericFilter());
        txtTelefono = new JTextField();
        txtCorreo = new JTextField();
        lblFoto = new JLabel();
        JButton btnCargarFoto = new JButton("Seleccionar Foto");
        
        ciPanel.add(cbTipoCi, "");
        ciPanel.add(txtNumeroCi, "growx");

        JPanel fotoPanel = new JPanel(new BorderLayout(5, 5));
        fotoPanel.add(lblFoto, BorderLayout.CENTER);
        fotoPanel.add(btnCargarFoto, BorderLayout.SOUTH);
        lblFoto.setHorizontalAlignment(SwingConstants.CENTER);
        lblFoto.setPreferredSize(new Dimension(100, 100));
        lblFoto.setBorder(BorderFactory.createEtchedBorder());

        formPanel.add(new JLabel("Nombres:"));
        formPanel.add(txtNombres, "span, growx");
        formPanel.add(new JLabel("Apellidos:"));
        formPanel.add(txtApellidos, "span, growx");
        formPanel.add(new JLabel("C.I.:"));
        formPanel.add(ciPanel, "span, growx");
        formPanel.add(new JLabel("Teléfono:"));
        formPanel.add(txtTelefono, "span, growx");
        formPanel.add(new JLabel("Correo Electrónico:"));
        formPanel.add(txtCorreo, "span, growx");
        formPanel.add(new JLabel("Foto:"), "top");
        formPanel.add(fotoPanel, "span, growx");


        cbBanco = new JComboBox<>(getBancosVenezuela());
        cbTipoCuenta = new JComboBox<>(new String[]{"Ahorro", "Corriente"});
        txtNumeroCuenta = new JTextField();
        ((AbstractDocument) txtNumeroCuenta.getDocument()).setDocumentFilter(new NumericFilter());

        JButton btnAgregarCuenta = new JButton("Agregar Cuenta");
        
        cuentasPanel.add(new JLabel("Banco:"));
        cuentasPanel.add(cbBanco, "growx");
        cuentasPanel.add(new JLabel("Tipo de Cuenta:"));
        cuentasPanel.add(cbTipoCuenta, "growx");
        cuentasPanel.add(new JLabel("# de Cuenta:"));
        cuentasPanel.add(txtNumeroCuenta, "growx");
        cuentasPanel.add(btnAgregarCuenta, "span 2, right");

        cuentasTableModel = new DefaultTableModel(new Object[]{"Principal", "Banco", "Tipo", "# Cuenta"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }
             @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };
        tblCuentas = new JTable(cuentasTableModel);
        tblCuentas.getColumnModel().getColumn(0).setMaxWidth(60);
        
        JScrollPane scrollCuentas = new JScrollPane(tblCuentas);
        scrollCuentas.setPreferredSize(new Dimension(400, 150));
        
        JButton btnEliminarCuenta = new JButton("Eliminar Seleccionada");
        
        cuentasPanel.add(scrollCuentas, "span 2, grow");
        cuentasPanel.add(btnEliminarCuenta, "span 2, right");

        JButton btnGuardar = new JButton("Yes");
        JButton btnCancelar = new JButton("No");
        buttonPanel.add(btnGuardar);
        buttonPanel.add(btnCancelar);

        mainPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(cuentasPanel, BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Listeners ---
        btnCargarFoto.addActionListener(this::cargarFoto);
        btnGuardar.addActionListener(this::guardar);
        btnCancelar.addActionListener(e -> dispose());
        btnAgregarCuenta.addActionListener(this::agregarCuenta);
        btnEliminarCuenta.addActionListener(this::eliminarCuenta);
        
        cuentasTableModel.addTableModelListener(e -> {
            if (e.getColumn() == 0) {
                int row = e.getFirstRow();
                boolean isPrincipal = (boolean) cuentasTableModel.getValueAt(row, 0);
                if (isPrincipal) {
                    for (int i = 0; i < cuentasTableModel.getRowCount(); i++) {
                        if (i != row) {
                            cuentasTableModel.setValueAt(false, i, 0);
                        }
                    }
                }
            }
        });


        if (trabajadora != null) {
            populateFields();
        }

        setupRealtimeValidation();
        pack();
        setLocationRelativeTo(owner);
    }

    private void populateFields() {
        txtNombres.setText(trabajadora.getNombres());
        txtApellidos.setText(trabajadora.getApellidos());
        cbTipoCi.setSelectedItem(trabajadora.getTipoCi());
        txtNumeroCi.setText(trabajadora.getNumeroCi());
        txtTelefono.setText(trabajadora.getTelefono());
        txtCorreo.setText(trabajadora.getCorreoElectronico());
        if (trabajadora.getFoto() != null) {
            Image scaledImage = trabajadora.getFoto().getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            lblFoto.setIcon(new ImageIcon(scaledImage));
        }
        for (CuentaBancaria cuenta : trabajadora.getCuentas()) {
            cuentasTableModel.addRow(new Object[]{cuenta.isEsPrincipal(), cuenta.getBanco(), cuenta.getTipoDeCuenta(), cuenta.getNumeroDeCuenta()});
        }
    }

    private void guardar(ActionEvent e) {
        LOGGER.info("Intentando guardar trabajadora");

        // Preparar trabajadora temporal para validación
        trabajadora.setNombres(txtNombres.getText());
        trabajadora.setApellidos(txtApellidos.getText());
        trabajadora.setTipoCi((String) cbTipoCi.getSelectedItem());
        trabajadora.setNumeroCi(txtNumeroCi.getText());
        trabajadora.setTelefono(txtTelefono.getText());
        trabajadora.setCorreoElectronico(txtCorreo.getText());
        trabajadora.setFoto(fotoIcon);

        // Recopilar cuentas bancarias
        List<CuentaBancaria> cuentas = new ArrayList<>();
        for (int i = 0; i < cuentasTableModel.getRowCount(); i++) {
            boolean isPrincipal = (boolean) cuentasTableModel.getValueAt(i, 0);
            String banco = (String) cuentasTableModel.getValueAt(i, 1);
            String tipo = (String) cuentasTableModel.getValueAt(i, 2);
            String numero = (String) cuentasTableModel.getValueAt(i, 3);
            cuentas.add(new CuentaBancaria(banco, tipo, numero, isPrincipal));
        }
        trabajadora.setCuentas(cuentas);

        // Validar trabajadora completa
        TrabajadoraValidator validator = new TrabajadoraValidator();
        ValidationResult result = validator.validate(trabajadora);

        // Mostrar resultado de validación
        if (!ValidationHelper.validateAndShow(this, result, "Validación de Trabajadora")) {
            LOGGER.warning("Validación de trabajadora falló");
            return;
        }

        LOGGER.info("Validación exitosa, guardando trabajadora");
        saved = true;
        dispose();
    }
    
    private void agregarCuenta(ActionEvent e) {
        String banco = Objects.requireNonNull(cbBanco.getSelectedItem()).toString();
        String tipo = Objects.requireNonNull(cbTipoCuenta.getSelectedItem()).toString();
        String numero = txtNumeroCuenta.getText();

        // Validar cuenta antes de agregar
        ValidationResult result = new ValidationResult();
        CommonValidators.validateNotEmpty(banco, "Banco", result);
        CommonValidators.validateNotEmpty(tipo, "Tipo de cuenta", result);
        CommonValidators.validateCuentaBancaria(numero, "Número de cuenta", result);

        if (!result.isValid()) {
            ValidationHelper.showErrors(this, result);
            return;
        }

        // Verificar si ya existe una cuenta con ese número
        for (int i = 0; i < cuentasTableModel.getRowCount(); i++) {
            String numeroExistente = (String) cuentasTableModel.getValueAt(i, 3);
            if (numeroExistente.equals(numero)) {
                JOptionPane.showMessageDialog(this,
                        "Ya existe una cuenta con ese número",
                        "Cuenta Duplicada",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        boolean isFirstAccount = cuentasTableModel.getRowCount() == 0;
        cuentasTableModel.addRow(new Object[]{isFirstAccount, banco, tipo, numero});
        txtNumeroCuenta.setText("");

        LOGGER.info("Cuenta bancaria agregada");
    }

    private void eliminarCuenta(ActionEvent e) {
        int selectedRow = tblCuentas.getSelectedRow();
        if (selectedRow >= 0) {
            cuentasTableModel.removeRow(selectedRow);
        } else {
            JOptionPane.showMessageDialog(this, "Seleccione una cuenta para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }


    private void cargarFoto(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Imágenes", "jpg", "png", "gif", "jpeg"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            fotoIcon = new ImageIcon(fileChooser.getSelectedFile().getAbsolutePath());
            Image scaledImage = fotoIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            lblFoto.setIcon(new ImageIcon(scaledImage));
        }
    }

    private void setupRealtimeValidation() {
        // Validación de nombres
        txtNombres.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateNombres(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateNombres(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validateNombres(); }
        });

        // Validación de apellidos
        txtApellidos.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateApellidos(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateApellidos(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validateApellidos(); }
        });

        // Validación de CI
        txtNumeroCi.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateCI(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateCI(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validateCI(); }
        });

        // Validación de teléfono
        txtTelefono.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateTelefono(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateTelefono(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validateTelefono(); }
        });

        // Validación de correo
        txtCorreo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateCorreo(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateCorreo(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validateCorreo(); }
        });

        // Validación de número de cuenta
        txtNumeroCuenta.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateNumeroCuenta(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateNumeroCuenta(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validateNumeroCuenta(); }
        });
    }

    private void validateNombres() {
        String nombres = txtNombres.getText().trim();
        if (!nombres.isEmpty()) {
            if (nombres.length() >= 2 && nombres.length() <= 50) {
                ValidationHelper.resetFieldBorder(txtNombres);
                ValidationHelper.removeErrorTooltip(txtNombres);
            } else {
                ValidationHelper.markFieldAsWarning(txtNombres);
                ValidationHelper.addErrorTooltip(txtNombres,
                        "Debe tener entre 2 y 50 caracteres");
            }
        } else {
            ValidationHelper.markFieldAsError(txtNombres);
            ValidationHelper.addErrorTooltip(txtNombres, "Campo obligatorio");
        }
    }

    private void validateApellidos() {
        String apellidos = txtApellidos.getText().trim();
        if (!apellidos.isEmpty()) {
            if (apellidos.length() >= 2 && apellidos.length() <= 50) {
                ValidationHelper.resetFieldBorder(txtApellidos);
                ValidationHelper.removeErrorTooltip(txtApellidos);
            } else {
                ValidationHelper.markFieldAsWarning(txtApellidos);
                ValidationHelper.addErrorTooltip(txtApellidos,
                        "Debe tener entre 2 y 50 caracteres");
            }
        } else {
            ValidationHelper.markFieldAsError(txtApellidos);
            ValidationHelper.addErrorTooltip(txtApellidos, "Campo obligatorio");
        }
    }

    private void validateCI() {
        String tipoCi = (String) cbTipoCi.getSelectedItem();
        String numeroCi = txtNumeroCi.getText().trim();

        if (!numeroCi.isEmpty()) {
            String ciCompleta = tipoCi + "-" + numeroCi;
            if (CommonValidators.isValidCedula(ciCompleta)) {
                ValidationHelper.resetFieldBorder(txtNumeroCi);
                ValidationHelper.removeErrorTooltip(txtNumeroCi);
            } else {
                ValidationHelper.markFieldAsWarning(txtNumeroCi);
                ValidationHelper.addErrorTooltip(txtNumeroCi,
                        "Debe contener entre 6 y 9 dígitos");
            }
        } else {
            ValidationHelper.markFieldAsError(txtNumeroCi);
            ValidationHelper.addErrorTooltip(txtNumeroCi, "Campo obligatorio");
        }
    }

    private void validateTelefono() {
        String telefono = txtTelefono.getText().trim();
        if (!telefono.isEmpty()) {
            if (CommonValidators.isValidPhone(telefono)) {
                ValidationHelper.resetFieldBorder(txtTelefono);
                ValidationHelper.removeErrorTooltip(txtTelefono);
            } else {
                ValidationHelper.markFieldAsWarning(txtTelefono);
                ValidationHelper.addErrorTooltip(txtTelefono,
                        "Debe tener 10-11 dígitos");
            }
        } else {
            ValidationHelper.resetFieldBorder(txtTelefono);
        }
    }

    private void validateCorreo() {
        String correo = txtCorreo.getText().trim();
        if (!correo.isEmpty()) {
            if (CommonValidators.isValidEmail(correo)) {
                ValidationHelper.resetFieldBorder(txtCorreo);
                ValidationHelper.removeErrorTooltip(txtCorreo);
            } else {
                ValidationHelper.markFieldAsWarning(txtCorreo);
                ValidationHelper.addErrorTooltip(txtCorreo,
                        "Formato de email inválido");
            }
        } else {
            ValidationHelper.resetFieldBorder(txtCorreo);
        }
    }

    private void validateNumeroCuenta() {
        String numero = txtNumeroCuenta.getText().trim();
        if (!numero.isEmpty()) {
            if (CommonValidators.isValidCuentaBancaria(numero)) {
                ValidationHelper.resetFieldBorder(txtNumeroCuenta);
                ValidationHelper.removeErrorTooltip(txtNumeroCuenta);
            } else {
                ValidationHelper.markFieldAsWarning(txtNumeroCuenta);
                ValidationHelper.addErrorTooltip(txtNumeroCuenta,
                        "Debe tener exactamente 20 dígitos");
            }
        } else {
            ValidationHelper.resetFieldBorder(txtNumeroCuenta);
        }
    }

    public Trabajadora getTrabajadora() {
        return trabajadora;
    }

    public boolean isSaved() {
        return saved;
    }

    private String[] getBancosVenezuela() {
        return new String[]{
            "100% Banco", "Bancamiga", "Banco Activo", "Banco Agrícola de Venezuela",
            "Banco Bicentenario del Pueblo", "Banco Caroní", "Banco de la Fuerza Armada Nacional Bolivariana (BANFANB)",
            "Banco de Venezuela", "Banco del Caribe (Bancaribe)", "Banco del Tesoro", "Banco Exterior",
            "Banco Mercantil", "Banco Nacional de Crédito (BNC)", "Banco Plaza", "Banco Provincial", "Banco Sofitasa",
            "Banco Venezolano de Crédito", "Bancrecer", "Bangente", "Banplus", "Banesco",
            "Citibank Venezuela", "DelSur Banco Universal", "Mi Banco"
        };
    }

    static class NumericFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string.matches("[0-9]+")) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text.matches("[0-9]+")) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }
}
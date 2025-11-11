package com.capelli.capellisaleswindow;

import com.capelli.clientmanagementwindow.ClientManagementWindow;
import com.capelli.database.Database;
import com.capelli.model.Trabajadora;
import com.capelli.reports.DailyReportWindow;
import com.capelli.salesdashboard.SalesDashboardWindow;
import com.capelli.ui.MainPanel;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent; 
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.event.TableModelEvent;
import com.capelli.config.AppConfig;
import com.capelli.config.ConfigManager; // IMPORTAR
import com.capelli.database.ServiceDAO;
import com.capelli.model.Service;
import com.capelli.servicemanagement.ServiceManagementWindow;
import java.io.InputStream;
import java.util.Date; 
import java.sql.Timestamp; 
import java.util.logging.Level;
import java.util.logging.Logger;
import com.capelli.validation.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class CapelliSalesWindow extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(CapelliSalesWindow.class.getName());
    private Map<String, Double> preciosServicios = new HashMap<>();
    private List<String> trabajadorasNombres = new ArrayList<>();
    private List<Trabajadora> trabajadorasList = new ArrayList<>();
    private final List<String> tiposDescuento = new ArrayList<>(Arrays.asList(AppConfig.getDiscountTypes()));
    
    private final List<String> metodosPagoBs = new ArrayList<>(Arrays.asList("TD", "TC", "Pago Movil", "Efectivo Bs"));
    private final List<String> metodosPagoUsd = new ArrayList<>(Arrays.asList("Efectivo $", "Transferencia"));
    
    private final List<String> serviciosConMultiplesTrabajadoras = Arrays.asList(AppConfig.getMultipleWorkerServices());
    private double tasaBcv = AppConfig.getDefaultBcvRate();

    private boolean isDarkMode = true;

    private final DefaultTableModel tableModel;
    private JTable serviciosTable;
    private JComboBox<String> cedulaTipoComboBox;
    private JTextField cedulaNumeroField;
    private JLabel nombreClienteLabel;
    private final JLabel tasaLabel = new JLabel("Tasa BCV: Cargando...");
    private JComboBox<String> serviciosComboBox;
    private JComboBox<String> trabajadorasComboBox;
    private JComboBox<String> descuentoComboBox;
    private JTextField propinaField;
    private JComboBox<String> propinaTrabajadoraComboBox;
    private JTextField montoPagadoField;
    private JLabel vueltoLabel;
    private JRadioButton monedaBs, monedaDolar;
    private JComboBox<String> pagoComboBox;
    
    private JLabel subtotalLabel;
    private JLabel descuentoLabel;
    private JLabel propinaLabelGUI;
    private JLabel ivaLabel;
    private JLabel totalLabel;

    private JRadioButton pagoMovilCapelliRadio;
    private JRadioButton pagoMovilRosaRadio;
    private ButtonGroup pagoMovilDestinoGroup;
    private JPanel pagoMovilPanel;
    
    private JSpinner dateSpinner;
    private JTextField manualBcvField;
    private JCheckBox historicalSaleCheck;
    
    private JLabel correlativeLabel;
    private int currentCorrelative = 1;

    private final List<VentaServicio> serviciosAgregados = new ArrayList<>();
    private ClienteActivo clienteActual = null;

    private JCheckBox clienteProductoCheck;
    private Map<String, Service> serviciosMap = new HashMap<>(); 

    public CapelliSalesWindow() {
        super(AppConfig.getAppTitle());

        try {
            InputStream iconStream = getClass().getResourceAsStream(AppConfig.getIconPath());
            if (iconStream != null) {
                setIconImage(ImageIO.read(iconStream));
                LOGGER.info("Icono de aplicación cargado correctamente");
            } else {
                LOGGER.warning("No se pudo cargar el icono de la aplicación");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error al cargar icono de aplicación", e);
        }
        runBcvWorker();

        tableModel = new DefaultTableModel(new String[]{"Servicio", "Trabajador(a)", "Precio ($)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }
        };

        cargarDatosDesdeDB();
        loadApplicationSettings();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(AppConfig.getDefaultWindowWidth(), AppConfig.getDefaultWindowHeight());
        if (AppConfig.isMaximizedByDefault()) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        mainPanel.add(crearPanelIzquierdo(), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        mainPanel.add(crearPanelTabla(), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        mainPanel.add(crearPanelDerechoInferior(), gbc);

        add(mainPanel);
        
        setupKeyBindings();

        propinaField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            ValidationHelper.resetFieldBorder(propinaField);
            try {
                double propina = Double.parseDouble(propinaField.getText().replace(",", "."));
                if (propina < 0) {
                    ValidationHelper.markFieldAsError(propinaField);
                    ValidationHelper.addErrorTooltip(propinaField, "La propina no puede ser negativa");
                } else {
                    ValidationHelper.removeErrorTooltip(propinaField);
                }
            } catch (NumberFormatException e) {
                if (!propinaField.getText().isEmpty()) {
                    ValidationHelper.markFieldAsError(propinaField);
                    ValidationHelper.addErrorTooltip(propinaField, "Debe ser un número válido");
                }
            }
            actualizarTotales();
        }));

        montoPagadoField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            ValidationHelper.resetFieldBorder(montoPagadoField);
            try {
                double monto = Double.parseDouble(montoPagadoField.getText().replace(",", "."));
                if (monto < 0) {
                    ValidationHelper.markFieldAsError(montoPagadoField);
                    ValidationHelper.addErrorTooltip(montoPagadoField, "El monto no puede ser negativo");
                } else {
                    ValidationHelper.removeErrorTooltip(montoPagadoField);
                }
            } catch (NumberFormatException e) {
                if (!montoPagadoField.getText().isEmpty()) {
                    ValidationHelper.markFieldAsError(montoPagadoField);
                    ValidationHelper.addErrorTooltip(montoPagadoField, "Debe ser un número válido");
                }
            }
            actualizarTotales();
        }));

        cedulaNumeroField.getDocument().addDocumentListener(new SimpleDocumentListener(this::validateCedulaInput));
        cedulaTipoComboBox.addActionListener(e -> validateCedulaInput());
    }
    
    private void loadApplicationSettings() {
        this.currentCorrelative = ConfigManager.getCurrentCorrelative();
        if (correlativeLabel != null) {
            correlativeLabel.setText("Factura N°: " + currentCorrelative);
        }
    }
    
    private void setupKeyBindings() {
        JPanel contentPane = (JPanel) this.getContentPane();
        InputMap im = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = contentPane.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, KeyEvent.CTRL_DOWN_MASK), "changeCorrelative");
        am.put("changeCorrelative", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                promptForCorrelativeChange();
            }
        });
    }

    private void promptForCorrelativeChange() {
        // 1. Pedir contraseña
        JPasswordField passwordField = new JPasswordField(20);
        int option = JOptionPane.showConfirmDialog(this, passwordField, "Ingrese Contraseña de Administrador", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        String password = new String(passwordField.getPassword());
        // Contraseña simple (cambiar por una más segura si se desea)
        if (!password.equals("capelli2024")) {
            JOptionPane.showMessageDialog(this, "Contraseña incorrecta.", "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Pedir nuevo número
        String newCorrStr = JOptionPane.showInputDialog(this, "Ingrese el NUEVO número correlativo:", currentCorrelative);
        if (newCorrStr == null || newCorrStr.trim().isEmpty()) {
            return;
        }

        try {
            int newCorrelative = Integer.parseInt(newCorrStr.trim());
            if (newCorrelative <= 0) {
                JOptionPane.showMessageDialog(this, "El número debe ser positivo.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // 3. Guardar y actualizar
            ConfigManager.setCorrelative(newCorrelative);
            loadApplicationSettings(); // Recargar el número en la UI
            
            JOptionPane.showMessageDialog(this, "Correlativo actualizado a: " + newCorrelative, "Éxito", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valor inválido. Debe ingresar solo números.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void runBcvWorker() {
        tasaLabel.setText("Tasa BCV: Cargando...");
        SwingWorker<Double, Void> worker = new SwingWorker<Double, Void>() {
            @Override
            protected Double doInBackground() throws Exception {
                return BCVService.getBCVRate();
            }

            @Override
            protected void done() {
                try {
                    // Solo actualizar si no estamos en modo histórico
                    if (historicalSaleCheck != null && !historicalSaleCheck.isSelected()) {
                        double rate = get();
                        if (rate > 0) {
                            tasaBcv = rate;
                            String formattedRate = String.format("%.2f", tasaBcv);
                            tasaLabel.setText("Tasa BCV: " + formattedRate + " Bs/$");
                        } else {
                            tasaLabel.setText("Tasa BCV: Error de carga");
                        }
                    }
                } catch (Exception e) {
                    if (historicalSaleCheck != null && !historicalSaleCheck.isSelected()) {
                        tasaLabel.setText("Tasa BCV: Error");
                    }
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void cargarDatosDesdeDB() {
        ServiceDAO serviceDAO = new ServiceDAO(); 

        try {
            serviciosMap = serviceDAO.getAllMap(); 
            preciosServicios.clear(); 

            if (serviciosComboBox != null) {
                serviciosComboBox.removeAllItems();
                serviciosMap.keySet().stream().sorted().forEach(serviciosComboBox::addItem);
                if (serviciosComboBox.getItemCount() > 0) {
                     serviciosComboBox.setSelectedIndex(0);
                 }
            }
             serviciosMap.forEach((name, service) -> preciosServicios.put(name, service.getPrice_corto()));


        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar servicios: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "Error al cargar servicios desde DB", e);
        }

         String sqlEmployees = "SELECT id, nombres, apellidos FROM trabajadoras ORDER BY nombres";
         try (Connection conn = Database.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlEmployees)) {
             trabajadorasList.clear();
             trabajadorasNombres.clear();
             if (trabajadorasComboBox != null) {
                 trabajadorasComboBox.removeAllItems();
             }
             if (propinaTrabajadoraComboBox != null) {
                 propinaTrabajadoraComboBox.removeAllItems(); 
             }

             List<String> propinaDestinatarios = new ArrayList<>();

             while (rs.next()) {
                 Trabajadora t = new Trabajadora();
                 t.setId(rs.getInt("id"));
                 t.setNombres(rs.getString("nombres"));
                 t.setApellidos(rs.getString("apellidos"));
                 trabajadorasList.add(t);
                 String nombreCompleto = t.getNombreCompleto();
                 trabajadorasNombres.add(nombreCompleto);
                 propinaDestinatarios.add(nombreCompleto); 

                 if (trabajadorasComboBox != null) {
                     trabajadorasComboBox.addItem(nombreCompleto);
                 }
             }

             propinaDestinatarios.add("Salón"); 
             if (propinaTrabajadoraComboBox != null) {
                  propinaDestinatarios.forEach(propinaTrabajadoraComboBox::addItem);
              }

         } catch (SQLException e) {
             JOptionPane.showMessageDialog(this, "Error al cargar trabajadoras: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
              LOGGER.log(Level.SEVERE, "Error al cargar trabajadoras desde DB", e);
         }
    }

    private JPanel crearPanelIzquierdo() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Información de Venta"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel clientePanel = new JPanel(new GridBagLayout());
        clientePanel.setBorder(new TitledBorder("Datos de la Cliente"));
        GridBagConstraints gbcCliente = new GridBagConstraints();
        gbcCliente.insets = new Insets(5, 5, 5, 5);
        gbcCliente.fill = GridBagConstraints.HORIZONTAL;

        gbcCliente.gridx = 0;
        gbcCliente.gridy = 0;
        clientePanel.add(new JLabel("Cédula:"), gbcCliente);
        
        cedulaTipoComboBox = new JComboBox<>(new String[]{"V", "J", "G", "P"});
        cedulaNumeroField = new JTextField(15);
        ((javax.swing.text.AbstractDocument) cedulaNumeroField.getDocument()).setDocumentFilter(new NumericFilter());

        JPanel cedulaPanel = new JPanel(new BorderLayout(5, 0));
        cedulaPanel.add(cedulaTipoComboBox, BorderLayout.WEST);
        cedulaPanel.add(cedulaNumeroField, BorderLayout.CENTER);

        gbcCliente.gridx = 1;
        gbcCliente.gridy = 0;
        gbcCliente.gridwidth = 2;
        gbcCliente.weightx = 1.0;
        clientePanel.add(cedulaPanel, gbcCliente);
        
        cedulaNumeroField.addActionListener(e -> buscarClienteEnDB());

        historicalSaleCheck = new JCheckBox("Registrar Venta Histórica");
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 5; 
        gbcCliente.gridwidth = 3;
        clientePanel.add(historicalSaleCheck, gbcCliente);
        
        dateSpinner = new JSpinner(new SpinnerDateModel());
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));
        dateSpinner.setEnabled(false);
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 6;
        gbcCliente.gridwidth = 1;
        clientePanel.add(new JLabel("Fecha Venta:"), gbcCliente);
        gbcCliente.gridx = 1;
        gbcCliente.gridy = 6;
        gbcCliente.gridwidth = 2;
        clientePanel.add(dateSpinner, gbcCliente);
        
        manualBcvField = new JTextField("0.00");
        ((javax.swing.text.AbstractDocument) manualBcvField.getDocument()).setDocumentFilter(new NumericFilter()); 
        manualBcvField.setEnabled(false);
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 7;
        gbcCliente.gridwidth = 1;
        clientePanel.add(new JLabel("Tasa Manual (Bs/$):"), gbcCliente);
        gbcCliente.gridx = 1;
        gbcCliente.gridy = 7;
        gbcCliente.gridwidth = 2;
        clientePanel.add(manualBcvField, gbcCliente);
        
        manualBcvField.getDocument().addDocumentListener(new SimpleDocumentListener(this::actualizarTasaManual));
        historicalSaleCheck.addActionListener(e -> toggleHistoricalMode());

        tasaLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 4; 
        gbcCliente.gridwidth = 3; 
        gbcCliente.anchor = GridBagConstraints.CENTER;
        clientePanel.add(tasaLabel, gbcCliente);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(clientePanel, gbc);

        gbcCliente.gridx = 1;
        gbcCliente.gridy = 0;
        gbcCliente.gridwidth = 2;
        gbcCliente.weightx = 1.0;

        JButton buscarClienteBtn = new JButton("Buscar Cliente");
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 1;
        gbcCliente.gridwidth = 1;
        gbcCliente.weightx = 0.5;
        clientePanel.add(buscarClienteBtn, gbcCliente);
        buscarClienteBtn.addActionListener(e -> buscarClienteEnDB());

        JButton gestionarClientesBtn = new JButton("Gestionar Clientes");
        gbcCliente.gridx = 1;
        gbcCliente.gridy = 1;
        gbcCliente.gridwidth = 1;
        gbcCliente.weightx = 0.5;
        clientePanel.add(gestionarClientesBtn, gbcCliente);
        gestionarClientesBtn.addActionListener(e -> new ClientManagementWindow().setVisible(true));

        JButton gestionarServiciosBtn = new JButton("Gestionar Servicios");
        gbcCliente.gridx = 2;
        gbcCliente.gridy = 2;
        gbcCliente.gridwidth = 1;
        clientePanel.add(gestionarServiciosBtn, gbcCliente);

        gestionarServiciosBtn.addActionListener(e -> {
            ServiceManagementWindow serviceWindow = new ServiceManagementWindow();
            serviceWindow.setVisible(true);
            serviceWindow.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    cargarDatosDesdeDB();
                }
            });
        });

        JButton gestionarTrabajadorasBtn = new JButton("Gestionar Trabajadoras");
        gbcCliente.gridx = 2;
        gbcCliente.gridy = 1;
        gbcCliente.gridwidth = 1;
        gbcCliente.weightx = 0.5;
        clientePanel.add(gestionarTrabajadorasBtn, gbcCliente);

        gestionarTrabajadorasBtn.addActionListener(e -> {
            JFrame frame = new JFrame("Gestión de Trabajadoras");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.getContentPane().add(new MainPanel(isDarkMode));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    cargarDatosDesdeDB();
                }
            });
        });

        nombreClienteLabel = new JLabel("Nombre: No cargado");
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 2;
        gbcCliente.gridwidth = 2;
        clientePanel.add(nombreClienteLabel, gbcCliente);

        JButton verReportesBtn = new JButton("Ver Reportes de Ventas");
        verReportesBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 3;
        gbcCliente.gridwidth = 2;
        gbcCliente.insets = new Insets(15, 5, 5, 5);
        clientePanel.add(verReportesBtn, gbcCliente);
        verReportesBtn.addActionListener(e -> {
            SalesDashboardWindow dashboard = new SalesDashboardWindow();
            dashboard.setVisible(true);
        });
        
        correlativeLabel = new JLabel("Factura N°: " + currentCorrelative);
        correlativeLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gbcCliente.gridx = 2;
        gbcCliente.gridy = 3;
        gbcCliente.gridwidth = 1;
        gbcCliente.anchor = GridBagConstraints.EAST;
        clientePanel.add(correlativeLabel, gbcCliente);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(clientePanel, gbc);

        gbc.insets = new Insets(5, 5, 5, 5);

        JPanel serviciosPanel = new JPanel(new GridBagLayout());
        serviciosPanel.setBorder(new TitledBorder("Servicios"));
        GridBagConstraints gbcServicios = new GridBagConstraints();
        gbcServicios.insets = new Insets(5, 5, 5, 5);
        gbcServicios.fill = GridBagConstraints.HORIZONTAL;

        gbcServicios.gridx = 0;
        gbcServicios.gridy = 0;
        serviciosPanel.add(new JLabel("Servicio:"), gbcServicios);
        serviciosComboBox = new JComboBox<>(preciosServicios.keySet().toArray(new String[0]));
        gbcServicios.gridx = 1;
        gbcServicios.gridy = 0;
        gbcServicios.weightx = 1.0;
        serviciosPanel.add(serviciosComboBox, gbcServicios);

        gbcServicios.gridx = 0;
        gbcServicios.gridy = 1;
        serviciosPanel.add(new JLabel("Trabajador(a):"), gbcServicios);
        trabajadorasComboBox = new JComboBox<>(trabajadorasNombres.toArray(new String[0]));
        gbcServicios.gridx = 1;
        gbcServicios.gridy = 1;
        gbcServicios.weightx = 1.0;
        serviciosPanel.add(trabajadorasComboBox, gbcServicios);

        clienteProductoCheck = new JCheckBox("Cliente trae producto");
        clienteProductoCheck.setVisible(false); 
        gbcServicios.gridx = 0;
        gbcServicios.gridy = 3; 
        gbcServicios.gridwidth = 2; 
        gbcServicios.anchor = GridBagConstraints.WEST; 
        serviciosPanel.add(clienteProductoCheck, gbcServicios);

        JButton agregarBtn = new JButton("Agregar Servicio");
        gbcServicios.gridx = 0;
        gbcServicios.gridy = 4; 
        gbcServicios.gridwidth = 1;
        gbcServicios.weightx = 0.5;
        gbcServicios.anchor = GridBagConstraints.CENTER; 
        serviciosPanel.add(agregarBtn, gbcServicios);
        agregarBtn.addActionListener(e -> agregarServicio());

        JButton eliminarBtn = new JButton("Eliminar Servicio");
        gbcServicios.gridx = 1;
        gbcServicios.gridy = 4; 
        gbcServicios.gridwidth = 1;
        gbcServicios.weightx = 0.5;
        serviciosPanel.add(eliminarBtn, gbcServicios);
        eliminarBtn.addActionListener(e -> eliminarServicioSeleccionado());

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(serviciosPanel, gbc);

        serviciosComboBox.addActionListener(e -> {
            String selectedServiceName = (String) serviciosComboBox.getSelectedItem();
            if (selectedServiceName != null) {
                Service selectedService = serviciosMap.get(selectedServiceName);
                if (selectedService != null && selectedService.isPermiteClienteProducto()) {
                    clienteProductoCheck.setVisible(true); 
                } else {
                    clienteProductoCheck.setVisible(false); 
                    clienteProductoCheck.setSelected(false); 
                }
            } else {
                clienteProductoCheck.setVisible(false);
                clienteProductoCheck.setSelected(false);
            }
        });

        clienteProductoCheck.addActionListener(e -> {
            LOGGER.fine("Checkbox 'Cliente trae producto' cambiado a: " + clienteProductoCheck.isSelected());
        });

        JButton verReporteDiarioBtn = new JButton("Ver Reporte del Día");
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 8; 
        gbcCliente.gridwidth = 3; 
        gbcCliente.anchor = GridBagConstraints.CENTER;
        clientePanel.add(verReporteDiarioBtn, gbcCliente);

        verReporteDiarioBtn.addActionListener(e -> {

            new DailyReportWindow().setVisible(true);
        });

        JToggleButton themeToggle = new JToggleButton("Cambiar a Modo Claro", true);
        themeToggle.addActionListener(e -> {
            try {
                if (themeToggle.isSelected()) {
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                    themeToggle.setText("Cambiar a Modo Claro");
                    isDarkMode = true;
                } else {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                    themeToggle.setText("Cambiar a Modo Oscuro");
                    isDarkMode = false;
                }
                SwingUtilities.updateComponentTreeUI(CapelliSalesWindow.this);
            } catch (UnsupportedLookAndFeelException ex) {
                ex.printStackTrace();
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(themeToggle, gbc);

        return panel;
    }

    private void toggleHistoricalMode() {
        boolean enabled = historicalSaleCheck.isSelected();
        dateSpinner.setEnabled(enabled);
        manualBcvField.setEnabled(enabled);
        tasaLabel.setEnabled(!enabled); 

        if (!enabled) {
            manualBcvField.setText("0.00");
            runBcvWorker(); 
        } else {
            actualizarTasaManual();
        }
    }
    
    private void actualizarTasaManual() {
        if (historicalSaleCheck.isSelected()) {
            try {
                tasaBcv = Double.parseDouble(manualBcvField.getText().replace(",", "."));
            } catch (NumberFormatException e) {
                tasaBcv = 0.0;
            }
            actualizarTotales(); 
        }
    }


    private void validateCedulaInput() {
        String tipoCi = (String) cedulaTipoComboBox.getSelectedItem();
        String numeroCi = cedulaNumeroField.getText().trim();

        if (!numeroCi.isEmpty()) {
            String cedula = tipoCi + "-" + numeroCi;
            if (CommonValidators.isValidCedula(cedula)) {
                ValidationHelper.resetFieldBorder(cedulaNumeroField);
                ValidationHelper.removeErrorTooltip(cedulaNumeroField);
            } else {
                ValidationHelper.markFieldAsWarning(cedulaNumeroField);
                ValidationHelper.addErrorTooltip(cedulaNumeroField,
                        "Cédula incompleta o inválida (ej: 6-9 dígitos)");
            }
        } else {
            ValidationHelper.resetFieldBorder(cedulaNumeroField);
            ValidationHelper.removeErrorTooltip(cedulaNumeroField);
        }
    }

    private void buscarClienteEnDB() {
        String tipoCi = (String) cedulaTipoComboBox.getSelectedItem();
        String numeroCi = cedulaNumeroField.getText().trim();
        
        if (numeroCi.isEmpty()) {
            nombreClienteLabel.setText("Nombre: No cargado");
            clienteActual = null;
            return;
        }

        String cedula = tipoCi + "-" + numeroCi;

        ValidationResult result = ClienteValidator.validateCedula(cedula);

        if (!result.isValid()) {
            ValidationHelper.showErrors(this, result);
            ValidationHelper.markFieldAsError(cedulaNumeroField);
            return;
        }

        ValidationHelper.resetFieldBorder(cedulaNumeroField);

        String sql = "SELECT client_id, full_name, hair_type FROM clients WHERE cedula = ?";
        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cedula);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                clienteActual = new ClienteActivo(
                        rs.getInt("client_id"),
                        cedula,
                        rs.getString("full_name"),
                        rs.getString("hair_type") 
                );

                String hairTypeInfo = clienteActual.getHairType();
                String infoCliente = "Nombre: " + clienteActual.getNombre()
                        + " (Cabello: " + (hairTypeInfo != null && !hairTypeInfo.isEmpty() ? hairTypeInfo : "No definido") + ")";
                nombreClienteLabel.setText(infoCliente);

                LOGGER.info("Cliente cargado: " + clienteActual.getNombre());
                JOptionPane.showMessageDialog(this,
                        "Cliente cargado: " + clienteActual.getNombre(),
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                clienteActual = null;
                nombreClienteLabel.setText("Nombre: No encontrado");

                int response = JOptionPane.showConfirmDialog(this,
                        "Cliente no encontrado. ¿Desea registrar un nuevo cliente?",
                        "Cliente No Encontrado",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                if (response == JOptionPane.YES_OPTION) {
                    ClientManagementWindow clientWindow = new ClientManagementWindow(cedula);
                    clientWindow.setVisible(true);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al buscar cliente", ex);
            JOptionPane.showMessageDialog(this,
                    "Error al buscar cliente: " + ex.getMessage(),
                    "Error DB",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarServicio() {
        String nombreServicio = (String) serviciosComboBox.getSelectedItem();
        String trabajadora = (String) trabajadorasComboBox.getSelectedItem();
        boolean clienteTraeProducto = clienteProductoCheck.isSelected() && clienteProductoCheck.isVisible();
    
        if (nombreServicio == null) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un servicio.", "Servicio no seleccionado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (trabajadora == null) {
             JOptionPane.showMessageDialog(this, "Por favor, seleccione una trabajadora.", "Trabajadora no seleccionada", JOptionPane.WARNING_MESSAGE);
             return;
        }
    
        Service servicioCompleto = serviciosMap.get(nombreServicio);
        if (servicioCompleto == null) {
            JOptionPane.showMessageDialog(this, "Error: no se pudo cargar la información del servicio.", "Error Interno", JOptionPane.ERROR_MESSAGE);
            LOGGER.severe("Servicio seleccionado '" + nombreServicio + "' no encontrado en serviciosMap.");
            return;
        }
    
        double precioFinal;
    
        if (clienteTraeProducto && servicioCompleto.isPermiteClienteProducto()) {
            precioFinal = servicioCompleto.getPriceClienteProducto();
            LOGGER.fine("Usando precio con producto del cliente para " + nombreServicio + ": $" + precioFinal);
        } else {
            precioFinal = servicioCompleto.getPrice_corto(); 
    
            if (clienteActual != null && clienteActual.getHairType() != null && !clienteActual.getHairType().isEmpty()) {
                String tipoCabello = clienteActual.getHairType();
                LOGGER.fine("Cliente tiene tipo de cabello: " + tipoCabello);
                switch (tipoCabello) {
                    case "Mediano":
                        if (servicioCompleto.getPrice_medio() > 0) {
                            precioFinal = servicioCompleto.getPrice_medio();
                            LOGGER.fine("Aplicando precio 'Mediano': $" + precioFinal);
                        } else {
                             LOGGER.fine("Servicio no tiene precio 'Mediano', usando 'Corto': $" + precioFinal);
                        }
                        break;
                    case "Largo":
                        if (servicioCompleto.getPrice_largo() > 0) {
                            precioFinal = servicioCompleto.getPrice_largo();
                            LOGGER.fine("Aplicando precio 'Largo': $" + precioFinal);
                        } else {
                             LOGGER.fine("Servicio no tiene precio 'Largo', usando 'Corto': $" + precioFinal);
                        }
                        break;
                    default: 
                         LOGGER.fine("Aplicando precio 'Corto' por defecto: $" + precioFinal);
                        break;
                }
            } else {
                LOGGER.fine("No hay cliente cargado o no tiene tipo de cabello, usando precio 'Corto': $" + precioFinal);
            }
    
            boolean esServicioExtensiones = nombreServicio.toLowerCase().contains("exten"); 
            if (esServicioExtensiones && servicioCompleto.getPrice_ext() > 0) {
                precioFinal = servicioCompleto.getPrice_ext();
                LOGGER.fine("Aplicando precio 'Extensiones': $" + precioFinal);
            }
        }
    
        String nombreEnTabla = nombreServicio;
        if (clienteTraeProducto) {
            nombreEnTabla += " (Cliente)"; 
        }
    
        serviciosAgregados.add(new VentaServicio(nombreEnTabla, trabajadora, precioFinal)); 
        tableModel.addRow(new Object[]{nombreEnTabla, trabajadora, new DecimalFormat("#,##0.00").format(precioFinal)}); 
        LOGGER.info("Servicio agregado a la venta: " + nombreEnTabla + ", Trabajadora: " + trabajadora + ", Precio: $" + precioFinal);
    
        actualizarTotales();
    
        clienteProductoCheck.setSelected(false);
        String nextSelectedService = (String) serviciosComboBox.getSelectedItem();
         if (nextSelectedService != null) {
             Service nextService = serviciosMap.get(nextSelectedService);
             if (nextService != null && !nextService.isPermiteClienteProducto()) {
                 clienteProductoCheck.setVisible(false);
             }
         } else {
             clienteProductoCheck.setVisible(false);
         }
    }

    private void eliminarServicioSeleccionado() {
        int selectedRow = serviciosTable.getSelectedRow();

        if (selectedRow >= 0) {
            serviciosAgregados.remove(selectedRow);
            tableModel.removeRow(selectedRow);
            actualizarTotales();
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un servicio de la tabla para eliminar.", "Ningún Servicio Seleccionado", JOptionPane.WARNING_MESSAGE);
        }
    }

    private JPanel crearPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Servicios a Facturar"));

        serviciosTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(serviciosTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 2) {
                int row = e.getFirstRow();
                try {
                    double nuevoPrecio = Double.parseDouble(tableModel.getValueAt(row, 2).toString().replace(",", "."));
                    serviciosAgregados.get(row).setPrecio(nuevoPrecio);
                    actualizarTotales();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Por favor, ingrese un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        return panel;
    }

    private JPanel crearPanelDerechoInferior() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Total y Pago"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;


        JPanel totalesPanel = new JPanel(new GridLayout(5, 2)); 
        totalesPanel.setBorder(new TitledBorder("Resumen"));
        
        subtotalLabel = new JLabel("Subtotal ($): 0.00");
        descuentoLabel = new JLabel("Descuento ($): 0.00");
        ivaLabel = new JLabel("IVA ($): 0.00"); 
        totalLabel = new JLabel("Total ($): 0.00");
        propinaLabelGUI = new JLabel("Propina ($): 0.00");
        
        totalesPanel.add(subtotalLabel);
        totalesPanel.add(descuentoLabel);
        totalesPanel.add(ivaLabel); 
        totalesPanel.add(propinaLabelGUI);
        totalesPanel.add(totalLabel);

        JPanel descuentoPanel = new JPanel(new BorderLayout());
        descuentoPanel.setBorder(new TitledBorder("Descuento"));
        descuentoComboBox = new JComboBox<>(tiposDescuento.toArray(new String[0]));
        descuentoPanel.add(descuentoComboBox, BorderLayout.CENTER);

        JPanel propinaPanel = new JPanel(new GridBagLayout());
        propinaPanel.setBorder(new TitledBorder("Propina"));
        GridBagConstraints gbcPropina = new GridBagConstraints();
        gbcPropina.insets = new Insets(5, 5, 5, 5);
        gbcPropina.fill = GridBagConstraints.HORIZONTAL;
        gbcPropina.gridx = 0;
        gbcPropina.gridy = 0;
        propinaPanel.add(new JLabel("Monto ($):"), gbcPropina);
        propinaField = new JTextField("0.00");
        gbcPropina.gridx = 1;
        gbcPropina.gridy = 0;
        gbcPropina.weightx = 1.0;
        propinaPanel.add(propinaField, gbcPropina);
        gbcPropina.gridx = 0;
        gbcPropina.gridy = 1;
        propinaPanel.add(new JLabel("Para:"), gbcPropina);
        List<String> propinaDestinatarios = new ArrayList<>(trabajadorasNombres);
        propinaDestinatarios.add("Salón");
        propinaTrabajadoraComboBox = new JComboBox<>(propinaDestinatarios.toArray(new String[0]));
        gbcPropina.gridx = 1;
        gbcPropina.gridy = 1;
        propinaPanel.add(propinaTrabajadoraComboBox, gbcPropina);

        JPanel pagoPanel = new JPanel(new GridBagLayout());
        pagoPanel.setBorder(new TitledBorder("Opciones de Pago"));
        GridBagConstraints gbcPago = new GridBagConstraints();
        gbcPago.insets = new Insets(5, 5, 5, 5);
        gbcPago.fill = GridBagConstraints.HORIZONTAL;
        gbcPago.gridx = 0;
        gbcPago.gridy = 0;
        pagoPanel.add(new JLabel("Moneda:"), gbcPago);
        
        monedaBs = new JRadioButton("Bs", true);
        monedaDolar = new JRadioButton("$");
        
        ButtonGroup monedaGroup = new ButtonGroup();
        monedaGroup.add(monedaBs);
        monedaGroup.add(monedaDolar);
        JPanel monedaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        monedaPanel.add(monedaDolar);
        monedaPanel.add(monedaBs);
        gbcPago.gridx = 1;
        gbcPago.gridy = 0;
        pagoPanel.add(monedaPanel, gbcPago);

        pagoPanel.setBorder(new TitledBorder("Opciones de Pago"));
        gbcPago.insets = new Insets(5, 5, 5, 5);
        gbcPago.fill = GridBagConstraints.HORIZONTAL;

        gbcPago.gridx = 0;
        gbcPago.gridy = 1;
        pagoPanel.add(new JLabel("Método:"), gbcPago);
        
        pagoComboBox = new JComboBox<>(metodosPagoBs.toArray(new String[0]));
        
        gbcPago.gridx = 1;
        gbcPago.gridy = 1;
        gbcPago.weightx = 1.0;
        pagoPanel.add(pagoComboBox, gbcPago);

        pagoMovilPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pagoMovilCapelliRadio = new JRadioButton("Capelli", true);
        pagoMovilRosaRadio = new JRadioButton("Rosa");
        pagoMovilDestinoGroup = new ButtonGroup();
        pagoMovilDestinoGroup.add(pagoMovilCapelliRadio);
        pagoMovilDestinoGroup.add(pagoMovilRosaRadio);
        pagoMovilPanel.add(new JLabel("Destino:"));
        pagoMovilPanel.add(pagoMovilCapelliRadio);
        pagoMovilPanel.add(pagoMovilRosaRadio);
        pagoMovilPanel.setVisible(true); 

        gbcPago.gridx = 1;
        gbcPago.gridy = 2;
        pagoPanel.add(pagoMovilPanel, gbcPago);

        gbcPago.gridx = 0;
        gbcPago.gridy = 3;
        pagoPanel.add(new JLabel("Monto Pagado:"), gbcPago);
        montoPagadoField = new JTextField("0.00");
        gbcPago.gridx = 1;
        gbcPago.gridy = 3;
        gbcPago.weightx = 1.0;
        pagoPanel.add(montoPagadoField, gbcPago);

        gbcPago.gridx = 0;
        gbcPago.gridy = 4;
        pagoPanel.add(new JLabel("Vuelto:"), gbcPago);
        vueltoLabel = new JLabel("0.00");
        gbcPago.gridx = 1;
        gbcPago.gridy = 4;
        gbcPago.weightx = 1.0;
        pagoPanel.add(vueltoLabel, gbcPago);

        ActionListener updateListener = e -> {
            actualizarMetodosPago();
            actualizarTotales();
        };
        descuentoComboBox.addActionListener(e -> actualizarTotales()); 
        monedaBs.addActionListener(updateListener);
        monedaDolar.addActionListener(updateListener);

        pagoComboBox.addActionListener(e -> {
            String selectedMethod = (String) pagoComboBox.getSelectedItem();
            pagoMovilPanel.setVisible("Pago Movil".equals(selectedMethod));
        });

        JButton facturarBtn = new JButton("Generar Factura");
        facturarBtn.setFont(new Font("Arial", Font.BOLD, 16));
        facturarBtn.addActionListener(e -> generarFactura());

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(descuentoPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(propinaPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(totalesPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(pagoPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(facturarBtn, gbc);

        return panel;
    }

    private void actualizarMetodosPago() {
        pagoComboBox.removeAllItems();
        if (monedaBs.isSelected()) {
            metodosPagoBs.forEach(pagoComboBox::addItem);
        } else {
            metodosPagoUsd.forEach(pagoComboBox::addItem);
        }
        String selectedMethod = (String) pagoComboBox.getSelectedItem();
        pagoMovilPanel.setVisible("Pago Movil".equals(selectedMethod));
    }


    private void actualizarTotales() {
        double subtotal = serviciosAgregados.stream().mapToDouble(VentaServicio::getPrecio).sum();
        
        double propina = 0.0;
        try {
            propina = Double.parseDouble(propinaField.getText().replace(",", "."));
        } catch (NumberFormatException ignored) {
        }

        double descuento = 0.0;
        String tipoDesc = Objects.requireNonNull(descuentoComboBox.getSelectedItem()).toString();

        if (tipoDesc.equals("Promoción")) {
            descuento = subtotal * AppConfig.getPromoDiscountPercentage();
            LOGGER.fine("Descuento por promoción aplicado: " + descuento);
        }
        
        double subtotalConDescuento = subtotal - descuento;
        double iva = subtotalConDescuento * AppConfig.getVatPercentage();
        double total = subtotalConDescuento + iva + propina;

        DecimalFormat df = new DecimalFormat("#,##0.00");
        double montoPagado = 0.0;
        try {
            montoPagado = Double.parseDouble(montoPagadoField.getText().replace(",", "."));
        } catch (NumberFormatException ignored) {
        }
        
        double tasa = tasaBcv; 

        if (monedaBs.isSelected()) {
            subtotalLabel.setText("Subtotal (Bs): " + df.format(subtotal * tasa));
            descuentoLabel.setText("Descuento (Bs): " + df.format(descuento * tasa));
            ivaLabel.setText("IVA (Bs): " + df.format(iva * tasa));
            propinaLabelGUI.setText("Propina (Bs): " + df.format(propina * tasa));
            totalLabel.setText("Total (Bs): " + df.format(total * tasa));
            vueltoLabel.setText(df.format(montoPagado - (total * tasa)));
        } else {
            subtotalLabel.setText("Subtotal ($): " + df.format(subtotal));
            descuentoLabel.setText("Descuento ($): " + df.format(descuento));
            ivaLabel.setText("IVA ($): " + df.format(iva));
            propinaLabelGUI.setText("Propina ($): " + df.format(propina));
            totalLabel.setText("Total ($): " + df.format(total));
            vueltoLabel.setText(df.format(montoPagado - total));
        }
    }

    private void generarFactura() {
        LOGGER.info("Iniciando validación de venta...");

        List<VentaValidator.ServicioVenta> serviciosParaValidar = new ArrayList<>();
        for (VentaServicio vs : serviciosAgregados) {
            serviciosParaValidar.add(new VentaValidator.ServicioVenta(
                    vs.getServicio(),
                    vs.getTrabajadora(),
                    vs.getPrecio()
            ));
        }

        double subtotal = serviciosAgregados.stream().mapToDouble(VentaServicio::getPrecio).sum();

        double propina = 0.0;
        try {
            propina = Double.parseDouble(propinaField.getText().replace(",", "."));
        } catch (NumberFormatException e) {
        }

        String tipoDesc = Objects.requireNonNull(descuentoComboBox.getSelectedItem()).toString();
        double descuento = tipoDesc.equals("Promoción") ? subtotal * AppConfig.getPromoDiscountPercentage() : 0.0;

        double subtotalConDescuento = subtotal - descuento;
        double iva = subtotalConDescuento * AppConfig.getVatPercentage();
        double total = subtotalConDescuento + iva + propina;

        double montoPagado = 0.0;
        try {
            montoPagado = Double.parseDouble(montoPagadoField.getText().replace(",", "."));
        } catch (NumberFormatException e) {
        }

        String metodoPago = pagoComboBox.getSelectedItem() != null
                ? pagoComboBox.getSelectedItem().toString()
                : "";

        String moneda = monedaDolar.isSelected() ? "$" : "Bs";

        double totalEnDolares = total; 
        double montoPagadoEnDolares = montoPagado; 

        if (moneda.equals("Bs")) {
            montoPagadoEnDolares = montoPagado / tasaBcv;
        }

        ValidationResult result = VentaValidator.validateVenta(
                serviciosParaValidar,
                subtotal,
                descuento,
                iva, 
                propina,
                totalEnDolares, 
                montoPagadoEnDolares, 
                metodoPago,
                tipoDesc
        );

        String destinoPagoMovil = null;
        if ("Pago Movil".equals(metodoPago)) {
            destinoPagoMovil = pagoMovilRosaRadio.isSelected() ? "Rosa" : "Capelli";
        }

        ValidationResult pagoResult = VentaValidator.validateMetodoPago(
                metodoPago, moneda, destinoPagoMovil
        );
        result.merge(pagoResult);

        if (propina > 0) {
            String destinatarioPropina = propinaTrabajadoraComboBox.getSelectedItem() != null
                    ? propinaTrabajadoraComboBox.getSelectedItem().toString()
                    : "";
            ValidationResult propinaResult = VentaValidator.validatePropina(
                    propina, destinatarioPropina
            );
            result.merge(propinaResult);
        }

        ValidationResult descuentoResult = VentaValidator.validateDescuento(
                tipoDesc, descuento, subtotal
        );
        result.merge(descuentoResult);

        ValidationResult duplicadosResult = VentaValidator.checkDuplicateServices(
                serviciosParaValidar
        );
        result.merge(duplicadosResult);

        if (!ValidationHelper.validateAndShow(this, result, "Validación de Venta")) {
            LOGGER.warning("Validación de venta falló o fue cancelada por el usuario");
            return;
        }

        LOGGER.info("Validación de venta exitosa, procediendo a guardar...");

        Connection conn = null;
        long saleId = -1;
        
        int correlativeToSave = ConfigManager.getCurrentCorrelative();

        try {
            conn = Database.connect();
            conn.setAutoCommit(false);

            java.util.Date saleDateUtil = historicalSaleCheck.isSelected() ? 
                (java.util.Date) dateSpinner.getValue() : 
                new java.util.Date();
            java.sql.Timestamp saleDateSql = new java.sql.Timestamp(saleDateUtil.getTime());

            String sqlSale = "INSERT INTO sales (client_id, sale_date, subtotal, discount_type, "
                    + "discount_amount, vat_amount, total, payment_method, currency, "
                    + "payment_destination, bcv_rate_at_sale, correlative_number) " 
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sqlSale)) {
                
                if (clienteActual != null) {
                    pstmt.setInt(1, clienteActual.getId());
                } else {
                    pstmt.setNull(1, java.sql.Types.INTEGER);
                }
                pstmt.setTimestamp(2, saleDateSql); 
                pstmt.setDouble(3, subtotal); 
                pstmt.setString(4, tipoDesc); 
                pstmt.setDouble(5, descuento); 
                pstmt.setDouble(6, iva); 
                pstmt.setDouble(7, totalEnDolares); 
                pstmt.setString(8, metodoPago); 
                pstmt.setString(9, moneda); 
                if (destinoPagoMovil != null) {
                    pstmt.setString(10, destinoPagoMovil); 
                } else {
                    pstmt.setNull(10, java.sql.Types.VARCHAR); 
                }
                pstmt.setDouble(11, tasaBcv); 
                pstmt.setString(12, String.valueOf(correlativeToSave));
                pstmt.executeUpdate();
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    saleId = rs.getLong(1);
                    LOGGER.info("Venta insertada con ID: " + saleId + ", Correlativo: " + correlativeToSave);
                } else {
                    throw new SQLException("Error al obtener el ID de la venta generada (last_insert_rowid falló)");
                }
            }

            String sqlItems = "INSERT INTO sale_items (sale_id, service_id, employee_id, price_at_sale) "
                    + "VALUES (?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sqlItems)) {
                for (VentaServicio vs : serviciosAgregados) {
                    int serviceId = getServiceId(vs.getServicio(), conn);
                    int employeeId = getEmployeeIdByName(vs.getTrabajadora(), conn);
                    pstmt.setLong(1, saleId);
                    pstmt.setInt(2, serviceId);
                    pstmt.setInt(3, employeeId);
                    pstmt.setDouble(4, vs.getPrecio());
                    pstmt.addBatch();
                }

                int[] batchResults = pstmt.executeBatch();
                LOGGER.info("Items de venta insertados: " + batchResults.length);
            }

            if (propina > 0) {
                String sqlTip = "INSERT INTO tips (sale_id, recipient_name, amount) VALUES (?, ?, ?)";

                try (PreparedStatement pstmt = conn.prepareStatement(sqlTip)) {
                    String destinatarioPropina = propinaTrabajadoraComboBox.getSelectedItem() != null
                            ? propinaTrabajadoraComboBox.getSelectedItem().toString()
                            : "Salón";
                    pstmt.setLong(1, saleId);
                    pstmt.setString(2, destinatarioPropina);
                    pstmt.setDouble(3, propina);
                    pstmt.executeUpdate();
                    LOGGER.info("Propina insertada: $" + propina + " para " + destinatarioPropina);
                }
            }
            conn.commit();

            LOGGER.info("Venta registrada exitosamente. ID: " + saleId);
            
            int nextCorrelative = correlativeToSave + 1;
            ConfigManager.setCorrelative(nextCorrelative);
            loadApplicationSettings();

            String mensajeExito = construirMensajeExito(saleId, totalEnDolares, moneda,
                    montoPagadoEnDolares, tasaBcv);
            JOptionPane.showMessageDialog(this,
                    mensajeExito,
                    "✅ Factura Generada Exitosamente",
                    JOptionPane.INFORMATION_MESSAGE);
            limpiarVentana();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al registrar la venta", e);
            try {
                if (conn != null) {
                    conn.rollback();
                    LOGGER.info("Rollback ejecutado exitosamente");
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error al hacer rollback", ex);
            }
            String mensajeError = "Error al registrar la venta en la base de datos:\n\n"
                    + e.getMessage() + "\n\n"
                    + "Por favor, verifique los datos e intente nuevamente.";
            JOptionPane.showMessageDialog(this,
                    mensajeError,
                    "❌ Error de Transacción",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                    LOGGER.fine("Conexión a BD cerrada");
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error al cerrar conexión", ex);
            }
        }
    }

    private int getServiceId(String serviceName, Connection conn) throws SQLException {
        String originalServiceName = serviceName.replace(" (Cliente)", "").trim();
        
        String sql = "SELECT service_id FROM services WHERE name = ?";
    
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, originalServiceName); 
            ResultSet rs = pstmt.executeQuery();
    
            if (rs.next()) {
                int serviceId = rs.getInt("service_id");
                LOGGER.fine("Service ID encontrado: " + serviceId + " para servicio: " + originalServiceName);
                return serviceId;
            } else {
                LOGGER.severe("Servicio no encontrado en BD: " + originalServiceName);
                throw new SQLException("Servicio no encontrado en la base de datos: " + originalServiceName);
            }
        }
    }

    private int getEmployeeIdByName(String nombreCompleto, Connection conn) throws SQLException {
        for (Trabajadora t : trabajadorasList) {
            if (t.getNombreCompleto().equals(nombreCompleto)) {
                return t.getId(); 
            }
        }

        throw new SQLException("Trabajadora no encontrada: " + nombreCompleto 
                + " (Error: no se encontró en la lista 'trabajadorasList' de la aplicación)");
    }
    
    private String construirMensajeExito(long saleId, double total, String moneda,
            double montoPagado, double tasaBcv) {
        DecimalFormat df = new DecimalFormat("#,##0.00");

        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Venta registrada exitosamente\n\n");
        mensaje.append("═══════════════════════════════════\n");
        mensaje.append("ID de Venta: ").append(saleId).append("\n");
        mensaje.append("N° Factura: ").append(currentCorrelative).append("\n"); // MODIFICADO
        mensaje.append("───────────────────────────────────\n");

        if (moneda.equals("$")) {
            mensaje.append("Total: $").append(df.format(total)).append("\n");
            mensaje.append("Pagado: $").append(df.format(montoPagado)).append("\n");

            double vuelto = montoPagado - total;
            if (vuelto > 0) {
                mensaje.append("Vuelto: $").append(df.format(vuelto)).append("\n");
            }
        } else {
            mensaje.append("Total: Bs ").append(df.format(total * tasaBcv)).append("\n");
            mensaje.append("Pagado: Bs ").append(df.format(montoPagado * tasaBcv)).append("\n");

            double vuelto = (montoPagado * tasaBcv) - (total * tasaBcv);
            if (vuelto > 0) {
                mensaje.append("Vuelto: Bs ").append(df.format(vuelto)).append("\n");
            }

            mensaje.append("\n(Equivalente en $: ").append(df.format(total)).append(")\n");
        }

        mensaje.append("═══════════════════════════════════\n");
        mensaje.append("\n¡Gracias por usar el sistema Capelli!");

        return mensaje.toString();
    }

    private Service obtenerServicioPorNombre(String nombre) {
        Service service = serviciosMap.get(nombre);
        if (service == null) {
            LOGGER.log(Level.SEVERE, "Servicio no encontrado en el Map: " + nombre);
            JOptionPane.showMessageDialog(this, "Error: Servicio '" + nombre + "' no encontrado en la caché local.", "Error Interno", JOptionPane.ERROR_MESSAGE);
        }
        return service;
    }

    private void limpiarVentana() {
        clienteActual = null;
        cedulaTipoComboBox.setSelectedItem("V");
        cedulaNumeroField.setText("");
        nombreClienteLabel.setText("Nombre: No cargado");
        
        historicalSaleCheck.setSelected(false);
        dateSpinner.setValue(new Date());
        manualBcvField.setText("0.00");
        toggleHistoricalMode();
        
        serviciosAgregados.clear();
        tableModel.setRowCount(0);
        propinaField.setText("0.00");
        montoPagadoField.setText("0.00");
        descuentoComboBox.setSelectedIndex(0);
        
        monedaBs.setSelected(true);
        actualizarMetodosPago();
        
        actualizarTotales();
    }

    public static void main(String[] args) throws SQLException {
        LOGGER.info("=== INICIANDO APLICACIÓN CAPELLI ===");
        AppConfig.printConfiguration();

        Database.initialize();

        try {
            if (AppConfig.isDarkModeDefault()) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                LOGGER.info("Tema oscuro aplicado");
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
                LOGGER.info("Tema claro aplicado");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error al inicializar Look and Feel", ex);
        }

        SwingUtilities.invokeLater(() -> {
            CapelliSalesWindow window = new CapelliSalesWindow();
            window.setVisible(true);
            LOGGER.info("Ventana principal mostrada");
        });
    }
}
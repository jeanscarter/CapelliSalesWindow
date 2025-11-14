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
import com.capelli.config.ConfigManager;
import com.capelli.database.ServiceDAO;
import com.capelli.model.Service;
import com.capelli.servicemanagement.ServiceManagementWindow;
import java.io.InputStream;
import java.util.Date; 
import java.sql.Timestamp; 
import java.util.logging.Level;
import java.util.logging.Logger;
import com.capelli.validation.*;
import com.capelli.payroll.CommissionManagementWindow;
import com.capelli.payroll.PayrollWindow;
import com.capelli.reports.FinancialReportWindow;

import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import net.miginfocom.swing.MigLayout; 

public class CapelliSalesWindow extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(CapelliSalesWindow.class.getName());
    private boolean ivaExcluido = false; 
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
    
    // --- CAMBIOS PROPINAS ---
    private JTextField propinaField; // Ahora se usa para ingresar la propina individual
    private JComboBox<String> propinaTrabajadoraComboBox;
    
    private JRadioButton monedaBs, monedaDolar;
    private JComboBox<String> pagoComboBox;
    
    private JLabel subtotalLabel;
    private JLabel descuentoLabel;
    private JLabel propinaLabelGUI; // Muestra el total de propinas
    private JLabel ivaLabel;
    private JLabel totalLabel;

    private JRadioButton pagoMovilCapelliRadio;
    private JRadioButton pagoMovilRosaRadio;
    private ButtonGroup pagoMovilDestinoGroup;
    private JPanel pagoMovilPanel;
    
    private JPanel transferenciaUsdPanel;
    private JRadioButton transferenciaHotmailRadio, transferenciaGmailRadio, transferenciaIngridRadio;
    private ButtonGroup transferenciaUsdDestinoGroup;
    private JTextField referenciaUsdField;
    
    private JSpinner dateSpinner;
    private JTextField manualBcvField;
    private JCheckBox historicalSaleCheck;
    
    private JLabel correlativeLabel;
    private int currentCorrelative = 1;

    private final List<VentaServicio> serviciosAgregados = new ArrayList<>();
    private ClienteActivo clienteActual = null;

    private JCheckBox clienteProductoCheck;
    private Map<String, Service> serviciosMap = new HashMap<>(); 

    // --- VARIABLES PARA PAGOS MÚLTIPLES ---
    private DefaultTableModel pagosTableModel;
    private JTable pagosTable;
    private final List<Pago> pagosAgregados = new ArrayList<>();
    private JLabel montoRestanteLabel;
    private JTextField montoPagoField;
    private final DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");

    private record Pago(double monto, String moneda, String metodo, String destino, String referencia, double tasaBcv) {}

    // --- NUEVAS VARIABLES PARA PROPINAS MÚLTIPLES ---
    /**
     * Record interno para almacenar temporalmente las propinas agregadas.
     */
    private record Tip(String recipientName, double amount) {}

    private DefaultTableModel propinasTableModel;
    private JTable propinasTable;
    private final List<Tip> propinasAgregados = new ArrayList<>();
    // ------------------------------------------------

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

        // Panel Izquierdo
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 2; 
        gbc.weightx = 1.0; 
        gbc.weighty = 1.0; 
        mainPanel.add(crearPanelIzquierdo(), gbc);

        // Tabla (Top-Right)
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1; 
        gbc.weightx = 1.0; 
        gbc.weighty = 0.6; // Reducido un poco para dar espacio abajo
        mainPanel.add(crearPanelTabla(), gbc);

        // Pago (Bottom-Right)
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1; 
        gbc.weightx = 1.0; 
        gbc.weighty = 0.4; // Aumentado para caber propinas y pagos
        mainPanel.add(crearPanelDerechoInferior(), gbc);

        add(mainPanel);
        
        setupKeyBindings();

        // Listener para el campo "Agregar Propina" (para validación visual inmediata)
        propinaField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            ValidationHelper.resetFieldBorder(propinaField);
            try {
                double propina = Double.parseDouble(propinaField.getText().replace(",", "."));
                if (propina < 0) {
                    ValidationHelper.markFieldAsError(propinaField);
                }
            } catch (NumberFormatException e) {
                if (!propinaField.getText().isEmpty()) {
                    ValidationHelper.markFieldAsError(propinaField);
                }
            }
        }));

        // Listener para el campo "Agregar Pago"
        montoPagoField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            ValidationHelper.resetFieldBorder(montoPagoField);
            try {
                double monto = Double.parseDouble(montoPagoField.getText().replace(",", "."));
                if (monto < 0) {
                    ValidationHelper.markFieldAsError(montoPagoField);
                }
            } catch (NumberFormatException e) {
                if (!montoPagoField.getText().isEmpty()) {
                    ValidationHelper.markFieldAsError(montoPagoField);
                }
            }
        }));

        cedulaNumeroField.getDocument().addDocumentListener(new SimpleDocumentListener(this::validateCedulaInput));
        cedulaTipoComboBox.addActionListener(e -> validateCedulaInput());
    }
    
    // ... (Métodos loadApplicationSettings, setupKeyBindings, promptForCorrelativeChange, runBcvWorker igual que antes) ...
    // [Para brevedad, asumo que el usuario copia los métodos estándar sin cambios si no los pongo, pero pidió la clase completa]
    // Incluiré los métodos estándar para que sea "completa".

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

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK), "toggleIVA");
        am.put("toggleIVA", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleIVA();
            }
        });
    }

    private void promptForCorrelativeChange() {
        JPasswordField passwordField = new JPasswordField(20);
        int option = JOptionPane.showConfirmDialog(this, passwordField, "Ingrese Contraseña de Administrador", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        String password = new String(passwordField.getPassword());
        if (!password.equals("capelli2024")) {
            JOptionPane.showMessageDialog(this, "Contraseña incorrecta.", "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
            return;
        }

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
            ConfigManager.setCorrelative(newCorrelative);
            loadApplicationSettings();
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

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(clientePanel, gbc);

        JButton buscarClienteBtn = new JButton("Buscar Cliente");
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 1;
        gbcCliente.gridwidth = 1;
        gbcCliente.weightx = 0.5;
        clientePanel.add(buscarClienteBtn, gbcCliente);
        buscarClienteBtn.addActionListener(e -> buscarClienteEnDB());

        JPanel managementPanel = new JPanel(new MigLayout("wrap 2, fillx, insets 0", "[grow,fill][grow,fill]"));
        managementPanel.setBorder(new TitledBorder("Módulos de Gestión"));

        JButton gestionarClientesBtn = new JButton("Gestionar Clientes");
        gestionarClientesBtn.addActionListener(e -> new ClientManagementWindow().setVisible(true));
        managementPanel.add(gestionarClientesBtn, "sg btn");

        JButton gestionarServiciosBtn = new JButton("Gestionar Servicios");
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
        managementPanel.add(gestionarServiciosBtn, "sg btn");

        JButton gestionarTrabajadorasBtn = new JButton("Gestionar Trabajadoras");
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
        managementPanel.add(gestionarTrabajadorasBtn, "sg btn");

        JButton gestionarComisionesBtn = new JButton("Gestionar Comisiones");
        gestionarComisionesBtn.addActionListener(e -> new CommissionManagementWindow().setVisible(true));
        managementPanel.add(gestionarComisionesBtn, "sg btn, wrap"); 

        JButton calcularNominaBtn = new JButton("Calcular Nómina");
        calcularNominaBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        calcularNominaBtn.addActionListener(e -> new PayrollWindow().setVisible(true));
        managementPanel.add(calcularNominaBtn, "sg btn");

        JButton reporteFinancieroBtn = new JButton("Reporte Financiero");
        reporteFinancieroBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        reporteFinancieroBtn.addActionListener(e -> new FinancialReportWindow().setVisible(true));
        managementPanel.add(reporteFinancieroBtn, "sg btn");

        gbcCliente.gridx = 0;
        gbcCliente.gridy = 2; 
        gbcCliente.gridwidth = 3;
        clientePanel.add(managementPanel, gbcCliente);

        nombreClienteLabel = new JLabel("Nombre: No cargado");
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 3; 
        gbcCliente.gridwidth = 2;
        clientePanel.add(nombreClienteLabel, gbcCliente);

        JButton verReportesBtn = new JButton("Ver Reportes de Ventas");
        verReportesBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 4; 
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
        gbcCliente.gridy = 4; 
        gbcCliente.gridwidth = 1;
        gbcCliente.anchor = GridBagConstraints.EAST;
        clientePanel.add(correlativeLabel, gbcCliente);

        tasaLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 5; 
        gbcCliente.gridwidth = 3; 
        gbcCliente.anchor = GridBagConstraints.CENTER;
        clientePanel.add(tasaLabel, gbcCliente);

        historicalSaleCheck = new JCheckBox("Registrar Venta Histórica");
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 6; 
        gbcCliente.gridwidth = 3;
        clientePanel.add(historicalSaleCheck, gbcCliente);
        
        dateSpinner = new JSpinner(new SpinnerDateModel());
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));
        dateSpinner.setEnabled(false);
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 7;
        gbcCliente.gridwidth = 1;
        clientePanel.add(new JLabel("Fecha Venta:"), gbcCliente);
        gbcCliente.gridx = 1;
        gbcCliente.gridy = 7;
        gbcCliente.gridwidth = 2;
        clientePanel.add(dateSpinner, gbcCliente);
        
        manualBcvField = new JTextField("0.00");
        ((javax.swing.text.AbstractDocument) manualBcvField.getDocument()).setDocumentFilter(new NumericFilter()); 
        manualBcvField.setEnabled(false);
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 8;
        gbcCliente.gridwidth = 1;
        clientePanel.add(new JLabel("Tasa Manual (Bs/$):"), gbcCliente);
        gbcCliente.gridx = 1;
        gbcCliente.gridy = 8;
        gbcCliente.gridwidth = 2;
        clientePanel.add(manualBcvField, gbcCliente);
        
        manualBcvField.getDocument().addDocumentListener(new SimpleDocumentListener(this::actualizarTasaManual));
        historicalSaleCheck.addActionListener(e -> toggleHistoricalMode());

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
        gbcCliente.gridy = 9;
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
                switch (tipoCabello) {
                    case "Mediano":
                        if (servicioCompleto.getPrice_medio() > 0) {
                            precioFinal = servicioCompleto.getPrice_medio();
                        }
                        break;
                    case "Largo":
                        if (servicioCompleto.getPrice_largo() > 0) {
                            precioFinal = servicioCompleto.getPrice_largo();
                        }
                        break;
                    default: 
                        break;
                }
            }
    
            boolean esServicioExtensiones = nombreServicio.toLowerCase().contains("exten"); 
            if (esServicioExtensiones && servicioCompleto.getPrice_ext() > 0) {
                precioFinal = servicioCompleto.getPrice_ext();
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
        
        SwingUtilities.invokeLater(() -> {
            int lastRow = serviciosTable.getRowCount() - 1;
            if (lastRow >= 0) {
                int viewRow = serviciosTable.convertRowIndexToView(lastRow);
                serviciosTable.scrollRectToVisible(serviciosTable.getCellRect(viewRow, 0, true));
                serviciosTable.setRowSelectionInterval(viewRow, viewRow);
            }
        });

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
            int modelRow = serviciosTable.convertRowIndexToModel(selectedRow);
            serviciosAgregados.remove(modelRow);
            tableModel.removeRow(modelRow);
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
                    int modelRow = serviciosTable.convertRowIndexToModel(row);
                    double nuevoPrecio = Double.parseDouble(tableModel.getValueAt(row, 2).toString().replace(",", "."));
                    serviciosAgregados.get(modelRow).setPrecio(nuevoPrecio);
                    actualizarTotales();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Por favor, ingrese un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (IndexOutOfBoundsException ioobe) {
                     LOGGER.log(Level.WARNING, "Error de sincronización de tabla al editar precio", ioobe);
                }
            }
        });

        return panel;
    }

    /**
     * Reconstruye el panel de pago para soportar múltiples pagos y múltiples propinas.
     */
    private JPanel crearPanelDerechoInferior() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Total y Pago"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;

        // --- 1. Panel de Descuento ---
        JPanel descuentoPanel = new JPanel(new BorderLayout());
        descuentoPanel.setBorder(new TitledBorder("Descuento"));
        descuentoComboBox = new JComboBox<>(tiposDescuento.toArray(new String[0]));
        descuentoComboBox.addActionListener(e -> actualizarTotales());
        descuentoPanel.add(descuentoComboBox, BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(descuentoPanel, gbc);

        // --- 2. Panel de Propina (MODIFICADO) ---
        JPanel propinaPanel = new JPanel(new MigLayout("wrap 2, fillx, insets 5", "[right]10[grow,fill]"));
        propinaPanel.setBorder(new TitledBorder("Gestión de Propinas"));

        // Fila 1: Destinatario
        propinaPanel.add(new JLabel("Para:"));
        List<String> propinaDestinatarios = new ArrayList<>(trabajadorasNombres);
        propinaDestinatarios.add("Salón");
        propinaTrabajadoraComboBox = new JComboBox<>(propinaDestinatarios.toArray(new String[0]));
        propinaPanel.add(propinaTrabajadoraComboBox, "growx");

        // Fila 2: Monto
        propinaPanel.add(new JLabel("Monto ($):"));
        propinaField = new JTextField("0.00");
        ((javax.swing.text.AbstractDocument) propinaField.getDocument()).setDocumentFilter(new NumericFilter());
        propinaPanel.add(propinaField, "growx");

        // Fila 3: Botón Agregar
        JButton agregarPropinaBtn = new JButton("Agregar Propina");
        agregarPropinaBtn.addActionListener(e -> agregarPropina());
        propinaPanel.add(agregarPropinaBtn, "span 2, growx");

        // Fila 4: Tabla de Propinas
        propinasTableModel = new DefaultTableModel(new String[]{"Destinatario", "Monto ($)"}, 0) {
             @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        propinasTable = new JTable(propinasTableModel);
        JScrollPane scrollPropinas = new JScrollPane(propinasTable);
        scrollPropinas.setPreferredSize(new Dimension(200, 80)); 
        propinaPanel.add(scrollPropinas, "span 2, grow, h 80!");

        // Fila 5: Botón Eliminar
        JButton eliminarPropinaBtn = new JButton("Eliminar Propina");
        eliminarPropinaBtn.addActionListener(e -> eliminarPropina());
        propinaPanel.add(eliminarPropinaBtn, "span 2, align right");

        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(propinaPanel, gbc);

        // --- 3. Panel de Resumen (Totales) ---
        JPanel totalesPanel = new JPanel(new GridLayout(6, 1)); 
        totalesPanel.setBorder(new TitledBorder("Resumen"));
        subtotalLabel = new JLabel("Subtotal ($): 0.00");
        descuentoLabel = new JLabel("Descuento ($): 0.00");
        ivaLabel = new JLabel("IVA ($): 0.00");
        propinaLabelGUI = new JLabel("Propina ($): 0.00");
        totalLabel = new JLabel("Total ($): 0.00");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        montoRestanteLabel = new JLabel("Restante por Pagar ($): 0.00");
        montoRestanteLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        montoRestanteLabel.setForeground(Color.RED);

        totalesPanel.add(subtotalLabel);
        totalesPanel.add(descuentoLabel);
        totalesPanel.add(ivaLabel);
        totalesPanel.add(propinaLabelGUI);
        totalesPanel.add(totalLabel);
        totalesPanel.add(montoRestanteLabel);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        panel.add(totalesPanel, gbc);

        // --- 4. Panel de AGREGAR PAGO ---
        JPanel pagoPanel = new JPanel(new MigLayout("wrap 4, fillx, insets 5", "[right]10[grow,fill]20[right]10[grow,fill]")); 
        pagoPanel.setBorder(new TitledBorder("Agregar Pago"));

        // Moneda
        pagoPanel.add(new JLabel("Moneda:"));
        monedaBs = new JRadioButton("Bs", true);
        monedaDolar = new JRadioButton("$");
        ButtonGroup monedaGroup = new ButtonGroup();
        monedaGroup.add(monedaBs);
        monedaGroup.add(monedaDolar);
        JPanel monedaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); 
        monedaPanel.add(monedaDolar);
        monedaPanel.add(monedaBs);
        pagoPanel.add(monedaPanel);

        // Monto
        pagoPanel.add(new JLabel("Monto:"));
        montoPagoField = new JTextField("0.00");
        ((javax.swing.text.AbstractDocument) montoPagoField.getDocument()).setDocumentFilter(new NumericFilter());
        pagoPanel.add(montoPagoField, "growx, wrap");

        // Método
        pagoPanel.add(new JLabel("Método:"));
        pagoComboBox = new JComboBox<>(metodosPagoBs.toArray(new String[0]));
        pagoPanel.add(pagoComboBox, "growx");

        // Botón Agregar
        JButton agregarPagoBtn = new JButton("Agregar Pago");
        agregarPagoBtn.addActionListener(e -> agregarPago());
        pagoPanel.add(agregarPagoBtn, "span 2, growx, wrap");

        // Paneles dinámicos (Pago Móvil y Transferencia $)
        pagoMovilPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pagoMovilCapelliRadio = new JRadioButton("Capelli", true);
        pagoMovilRosaRadio = new JRadioButton("Rosa");
        pagoMovilDestinoGroup = new ButtonGroup();
        pagoMovilDestinoGroup.add(pagoMovilCapelliRadio);
        pagoMovilDestinoGroup.add(pagoMovilRosaRadio);
        pagoMovilPanel.add(new JLabel("Destino:"));
        pagoMovilPanel.add(pagoMovilCapelliRadio);
        pagoMovilPanel.add(pagoMovilRosaRadio);
        pagoMovilPanel.setVisible(true);
        pagoPanel.add(pagoMovilPanel, "span 4, growx");

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
        transferenciaUsdPanel.setVisible(false);
        pagoPanel.add(transferenciaUsdPanel, "span 4, growx");

        // Listeners para el panel de pago
        ActionListener updateListener = e -> {
            actualizarMetodosPago();
            actualizarPanelesPago();
        };
        monedaBs.addActionListener(updateListener);
        monedaDolar.addActionListener(updateListener);
        pagoComboBox.addActionListener(e -> actualizarPanelesPago());

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(pagoPanel, gbc);


        // --- 5. Panel de TABLA DE PAGOS ---
        pagosTableModel = new DefaultTableModel(new String[]{"Monto", "Moneda", "Método"}, 0) {
             @Override
             public boolean isCellEditable(int row, int column) { return false; }
        };
        pagosTable = new JTable(pagosTableModel);
        JScrollPane scrollPagos = new JScrollPane(pagosTable);
        scrollPagos.setBorder(new TitledBorder("Pagos Registrados"));

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.weighty = 1.0; 
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scrollPagos, gbc);

        // --- 6. Botón de Facturar ---
        JButton facturarBtn = new JButton("Generar Factura");
        facturarBtn.setFont(new Font("Arial", Font.BOLD, 16));
        facturarBtn.addActionListener(e -> generarFactura());
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.weighty = 0.0; 
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
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
    }

    private void agregarPropina() {
        double monto;
        try {
            monto = Double.parseDouble(propinaField.getText().replace(",", "."));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El monto de la propina es inválido.", "Error de Monto", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String destinatario = (String) propinaTrabajadoraComboBox.getSelectedItem();
        
        ValidationResult result = VentaValidator.validatePropina(monto, destinatario);
        if (!ValidationHelper.validateAndShow(this, result, "Validación de Propina")) {
            return;
        }

        Tip nuevaPropina = new Tip(destinatario, monto);
        propinasAgregados.add(nuevaPropina);
        
        propinasTableModel.addRow(new Object[]{
            destinatario,
            currencyFormat.format(monto)
        });

        LOGGER.info("Propina agregada: $" + monto + " para " + destinatario);
        
        propinaField.setText("0.00");
        
        actualizarTotales();
        
        SwingUtilities.invokeLater(() -> {
            int lastRow = propinasTable.getRowCount() - 1;
            if (lastRow >= 0) {
                int viewRow = propinasTable.convertRowIndexToView(lastRow);
                propinasTable.scrollRectToVisible(propinasTable.getCellRect(viewRow, 0, true));
                propinasTable.setRowSelectionInterval(viewRow, viewRow);
            }
        });
    }

    private void eliminarPropina() {
        int selectedRow = propinasTable.getSelectedRow();

        if (selectedRow >= 0) {
            int modelRow = propinasTable.convertRowIndexToModel(selectedRow);
            propinasAgregados.remove(modelRow);
            propinasTableModel.removeRow(modelRow);
            actualizarTotales();
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione una propina de la tabla para eliminar.", "Ninguna Propina Seleccionada", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void agregarPago() {
        double monto;
        try {
            monto = Double.parseDouble(montoPagoField.getText().replace(",", "."));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El monto ingresado es inválido.", "Error de Monto", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (monto <= 0) {
            JOptionPane.showMessageDialog(this, "El monto debe ser positivo.", "Error de Monto", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String moneda = monedaDolar.isSelected() ? "$" : "Bs";
        String metodo = (String) pagoComboBox.getSelectedItem();
        if (metodo == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un método de pago.", "Error de Método", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String destino = null;
        String referencia = null;
        double tasa = this.tasaBcv; 

        if (moneda.equals("Bs") && "Pago Movil".equals(metodo)) {
            destino = pagoMovilRosaRadio.isSelected() ? "Rosa" : "Capelli";
        } else if (moneda.equals("$") && "Transferencia".equals(metodo)) {
            if (transferenciaHotmailRadio.isSelected()) destino = "@hotmail";
            else if (transferenciaGmailRadio.isSelected()) destino = "@Gmail";
            else if (transferenciaIngridRadio.isSelected()) destino = "Ingrid";
            
            referencia = referenciaUsdField.getText().trim();
            if (referencia.isEmpty()) {
                 JOptionPane.showMessageDialog(this, "Debe ingresar la referencia para la transferencia en $.", "Error de Referencia", JOptionPane.ERROR_MESSAGE);
                 return;
            }
        }
        
        double montoEnDolares = monto;
        String montoDisplay = "$ " + currencyFormat.format(monto);

        if (moneda.equals("Bs")) {
            if (tasa <= 0) {
                 JOptionPane.showMessageDialog(this, "No se puede agregar pago en Bs sin una tasa BCV válida.", "Error de Tasa", JOptionPane.ERROR_MESSAGE);
                 return;
            }
            montoEnDolares = monto / tasa;
            montoDisplay = "Bs " + currencyFormat.format(monto);
        }

        Pago nuevoPago = new Pago(montoEnDolares, moneda, metodo, destino, referencia, tasa);
        pagosAgregados.add(nuevoPago);
        
        pagosTableModel.addRow(new Object[]{
            montoDisplay,
            moneda,
            metodo + (destino != null ? " (" + destino + ")" : "")
        });

        LOGGER.info("Pago agregado: " + montoDisplay + " " + metodo);
        
        montoPagoField.setText("0.00");
        referenciaUsdField.setText("");
        
        actualizarTotales();
        
        SwingUtilities.invokeLater(() -> {
            int lastRow = pagosTable.getRowCount() - 1;
            if (lastRow >= 0) {
                int viewRow = pagosTable.convertRowIndexToView(lastRow);
                pagosTable.scrollRectToVisible(pagosTable.getCellRect(viewRow, 0, true));
                pagosTable.setRowSelectionInterval(viewRow, viewRow);
            }
        });
    }

    private void actualizarTotales() {
        
        double subtotalGravable = 0.0; 
        double subtotalNoGravable = 0.0; 

        for (VentaServicio vs : serviciosAgregados) {
            if (vs.getServicio().equals("Abono Manual Staff")) {
                subtotalNoGravable += vs.getPrecio();
            } else {
                subtotalGravable += vs.getPrecio();
            }
        }
        
        double subtotal = subtotalGravable + subtotalNoGravable;
        
        // Sumar propinas desde la lista
        double propina = propinasAgregados.stream()
            .mapToDouble(Tip::amount)
            .sum();

        double descuento = 0.0;
        String tipoDesc = Objects.requireNonNull(descuentoComboBox.getSelectedItem()).toString();

        if (tipoDesc.equals("Promoción")) {
            descuento = subtotalGravable * AppConfig.getPromoDiscountPercentage();
            LOGGER.fine("Descuento por promoción (sobre subtotal gravable) aplicado: " + descuento);
        }
        
        double subtotalConDescuento = (subtotalGravable - descuento) + subtotalNoGravable;
        
        double iva = ivaExcluido ? 0.0 : (subtotalGravable - descuento) * AppConfig.getVatPercentage();
        
        double total = subtotalConDescuento + iva + propina; 

        double totalPagadoEnDolares = pagosAgregados.stream()
            .mapToDouble(Pago::monto)
            .sum();
            
        double restantePorPagar = total - totalPagadoEnDolares;
        
        double tasa = this.tasaBcv; 

        subtotalLabel.setText("Subtotal ($): " + currencyFormat.format(subtotal));
        descuentoLabel.setText("Descuento ($): " + currencyFormat.format(descuento));
        ivaLabel.setText("IVA ($): " + currencyFormat.format(iva));
        propinaLabelGUI.setText("Propina ($): " + currencyFormat.format(propina));
        totalLabel.setText("Total ($): " + currencyFormat.format(total));
        
        montoRestanteLabel.setText("Restante ($): " + currencyFormat.format(restantePorPagar) + 
                                  "  (Bs " + currencyFormat.format(restantePorPagar * tasa) + ")");
        
        if (restantePorPagar <= 0.01) { 
            montoRestanteLabel.setForeground(new Color(0, 150, 0)); 
            montoRestanteLabel.setText("Total Cubierto. Vuelto ($): " + currencyFormat.format(restantePorPagar * -1));
        } else {
            montoRestanteLabel.setForeground(Color.RED);
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

        double subtotalGravable = 0.0;
        double subtotalNoGravable = 0.0;
        for (VentaServicio vs : serviciosAgregados) {
            if (vs.getServicio().equals("Abono Manual Staff")) {
                subtotalNoGravable += vs.getPrecio();
            } else {
                subtotalGravable += vs.getPrecio();
            }
        }
        double subtotal = subtotalGravable + subtotalNoGravable;

        double propina = propinasAgregados.stream()
            .mapToDouble(Tip::amount)
            .sum();

        String tipoDesc = Objects.requireNonNull(descuentoComboBox.getSelectedItem()).toString();
        double descuento = 0.0;
        if (tipoDesc.equals("Promoción")) {
            descuento = subtotalGravable * AppConfig.getPromoDiscountPercentage();
        }

        double subtotalConDescuentoGravable = subtotalGravable - descuento;
        double iva = ivaExcluido ? 0.0 : subtotalConDescuentoGravable * AppConfig.getVatPercentage();
        double totalEnDolares = subtotalConDescuentoGravable + subtotalNoGravable + iva + propina;
        
        double totalPagadoEnDolares = pagosAgregados.stream()
            .mapToDouble(Pago::monto) 
            .sum();

        ValidationResult result = VentaValidator.validateVenta(
                serviciosParaValidar,
                subtotal,
                descuento,
                iva, 
                propina,
                totalEnDolares, 
                totalPagadoEnDolares, 
                tipoDesc
        );
        
        if (pagosAgregados.isEmpty() && !"Cuenta por Cobrar".equals(tipoDesc)) {
            result.addError("Pagos", "Debe agregar al menos un método de pago.");
        }
        
        ValidationResult descuentoResult = VentaValidator.validateDescuento(
                tipoDesc, descuento, subtotalGravable 
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
        
        java.util.Date saleDateUtil;
        if (historicalSaleCheck.isSelected()) {
            try {
                dateSpinner.commitEdit(); 
                saleDateUtil = (java.util.Date) dateSpinner.getValue();
                LOGGER.info("Modo histórico: Usando fecha del spinner: " + saleDateUtil);
            } catch (java.text.ParseException e) {
                LOGGER.log(Level.WARNING, "Error al 'parsear' la fecha del spinner, usando fecha actual", e);
                saleDateUtil = new java.util.Date(); 
            }
        } else {
            saleDateUtil = new java.util.Date();
            LOGGER.info("Modo estándar: Usando fecha actual: " + saleDateUtil);
        }
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String saleDateSqlString = sdf.format(saleDateUtil);
        
        try {
            conn = Database.connect();
            conn.setAutoCommit(false);

            // 1. GUARDAR LA VENTA
            String sqlSale = "INSERT INTO sales (client_id, sale_date, subtotal, discount_type, "
                    + "discount_amount, vat_amount, total, bcv_rate_at_sale, correlative_number) " 
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sqlSale)) {
                
                if (clienteActual != null) {
                    pstmt.setInt(1, clienteActual.getId());
                } else {
                    pstmt.setNull(1, java.sql.Types.INTEGER);
                }
                
                pstmt.setString(2, saleDateSqlString); 
                pstmt.setDouble(3, subtotal); 
                pstmt.setString(4, tipoDesc); 
                pstmt.setDouble(5, descuento); 
                pstmt.setDouble(6, iva); 
                pstmt.setDouble(7, totalEnDolares); 
                pstmt.setDouble(8, tasaBcv); 
                pstmt.setString(9, String.valueOf(correlativeToSave));
                
                pstmt.executeUpdate();
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    saleId = rs.getLong(1);
                    LOGGER.info("Venta insertada con ID: " + saleId + ", Correlativo: " + correlativeToSave + ", Fecha: " + saleDateSqlString);
                } else {
                    throw new SQLException("Error al obtener el ID de la venta generada (last_insert_rowid falló)");
                }
            }

            // 2. GUARDAR LOS ITEMS DE LA VENTA
            String sqlItems = "INSERT INTO sale_items (sale_id, service_id, employee_id, price_at_sale, client_brought_product) "
                    + "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sqlItems)) {
                for (VentaServicio vs : serviciosAgregados) {
                    int serviceId = getServiceId(vs.getServicio(), conn);
                    int employeeId = getEmployeeIdByName(vs.getTrabajadora(), conn);
                    boolean clienteTrajo = vs.getServicio().contains(" (Cliente)");
                    
                    pstmt.setLong(1, saleId);
                    pstmt.setInt(2, serviceId);
                    pstmt.setInt(3, employeeId);
                    pstmt.setDouble(4, vs.getPrecio());
                    pstmt.setBoolean(5, clienteTrajo);
                    pstmt.addBatch();
                }

                int[] batchResults = pstmt.executeBatch();
                LOGGER.info("Items de venta insertados: " + batchResults.length);
            }

            // 3. GUARDAR LOS PAGOS
            String sqlPayments = "INSERT INTO sale_payments (sale_id, monto, moneda, metodo_pago, "
                    + "destino_pago, referencia_pago, tasa_bcv_al_pago) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlPayments)) {
                for (Pago p : pagosAgregados) {
                    pstmt.setLong(1, saleId);
                    
                    if(p.moneda().equals("Bs")) {
                        pstmt.setDouble(2, p.monto() * p.tasaBcv()); 
                    } else {
                        pstmt.setDouble(2, p.monto()); 
                    }
                    
                    pstmt.setString(3, p.moneda());
                    pstmt.setString(4, p.metodo());
                    pstmt.setString(5, p.destino());
                    pstmt.setString(6, p.referencia());
                    pstmt.setDouble(7, p.tasaBcv()); 
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                LOGGER.info("Pagos de venta insertados: " + pagosAgregados.size());
            }

            // 4. GUARDAR PROPINAS
            if (!propinasAgregados.isEmpty()) {
                String sqlTip = "INSERT INTO tips (sale_id, recipient_name, amount) VALUES (?, ?, ?)";

                try (PreparedStatement pstmt = conn.prepareStatement(sqlTip)) {
                    for (Tip tip : propinasAgregados) {
                        pstmt.setLong(1, saleId);
                        pstmt.setString(2, tip.recipientName());
                        pstmt.setDouble(3, tip.amount());
                        pstmt.addBatch();
                    }
                    int[] batchResults = pstmt.executeBatch();
                    LOGGER.info("Propinas insertadas: " + batchResults.length);
                }
            }
            
            conn.commit();

            LOGGER.info("Venta registrada exitosamente. ID: " + saleId);
            
            int nextCorrelative = correlativeToSave + 1;
            ConfigManager.setCorrelative(nextCorrelative);
            loadApplicationSettings();

            String mensajeExito = construirMensajeExito(saleId, totalEnDolares,
                    totalPagadoEnDolares);
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
    
    private String construirMensajeExito(long saleId, double total, double montoPagado) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Venta registrada exitosamente\n\n");
        mensaje.append("═══════════════════════════════════\n");
        mensaje.append("ID de Venta: ").append(saleId).append("\n");
        mensaje.append("N° Factura: ").append(currentCorrelative).append("\n");
        mensaje.append("───────────────────────────────────\n");

        mensaje.append("Total: $").append(currencyFormat.format(total)).append("\n");
        mensaje.append("Pagado: $").append(currencyFormat.format(montoPagado)).append("\n");

        double vuelto = montoPagado - total;
        if (vuelto > 0.01) { 
            mensaje.append("Vuelto: $").append(currencyFormat.format(vuelto)).append("\n");
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
        ivaExcluido = false; 
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

        pagosAgregados.clear();
        pagosTableModel.setRowCount(0);
        montoPagoField.setText("0.00"); 
        if (referenciaUsdField != null) {
            referenciaUsdField.setText("");
        }

        propinaField.setText("0.00");
        propinasAgregados.clear();
        if (propinasTableModel != null) {
            propinasTableModel.setRowCount(0);
        }
        if (propinaTrabajadoraComboBox != null) {
             propinaTrabajadoraComboBox.setSelectedIndex(0);
        }

        descuentoComboBox.setSelectedIndex(0);
        
        monedaBs.setSelected(true);
        actualizarMetodosPago();
        actualizarPanelesPago(); 
        
        actualizarTotales();
    }

    private void toggleIVA() {
        ivaExcluido = !ivaExcluido; 
        if (ivaExcluido) {
            JOptionPane.showMessageDialog(this, 
                "IVA (" + (AppConfig.getVatPercentage() * 100) + "%) Excluido para ESTA factura.", 
                "Modo Sin IVA", 
                JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, 
                "IVA (" + (AppConfig.getVatPercentage() * 100) + "%) Incluido para ESTA factura.", 
                "Modo Con IVA", 
                JOptionPane.INFORMATION_MESSAGE);
        }
        actualizarTotales(); 
    }

    private void actualizarPanelesPago() {
        String selectedMethod = (String) pagoComboBox.getSelectedItem();
        boolean esBs = monedaBs.isSelected();
        
        pagoMovilPanel.setVisible(esBs && "Pago Movil".equals(selectedMethod));
        transferenciaUsdPanel.setVisible(!esBs && "Transferencia".equals(selectedMethod));
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